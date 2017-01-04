package coffeeslacker.app;

import coffeeslacker.brewer.Brewer;
import coffeeslacker.brewer.BrewerService;
import coffeeslacker.sensor.Sensor;
import coffeeslacker.sensor.SensorService;
import coffeeslacker.sensor.SensorType;
import coffeeslacker.slack.SlackService;
import coffeeslacker.statistics.BrewStat;
import coffeeslacker.statistics.BrewStatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static coffeeslacker.app.Brew.BrewState.*;


@Service
public class CoffeeSlacker implements BrewBountyListener, DelayedExecutorService {

    private static final Logger cLogger = LoggerFactory.getLogger(DelayedExecutorService.class);
    private static final String cMasterTitle = "Master Elite Bean Injector";
    private static final String cBrewCompleteChannelMsg = "*Brew complete*, först to kvarn!";

    private long mChannelDelayAfterCompletedBrew;
    private TimeUnit mChannelDelayAfterCompletedBrewUnit;
    private long mMsgDelayAfterCompletedBrew;
    private TimeUnit mMsgDelayAfterCompletedBrewUnit;
    private int mBountyHuntDuration;
    private TimeUnit mBountyHuntDurationUnit;
    private long mBrewExpectedCompletionTime;
    private TemporalUnit mBrewExpectedCompletionUnit;


    private BrewerService mBrewerService;
    private SensorService mSensorService;
    private SlackService mSlackService;
    private BrewStatService mBrewStatService;
    private ScheduledExecutorService mScheduledExecutorService;

    private Brew mBrew;
    private boolean mDebugMode = false;

    @Autowired
    public CoffeeSlacker(BrewerService pBrewerService, SensorService pSensorService, SlackService pSlackService, BrewStatService pBrewStatService) {
        mBrewerService = pBrewerService;
        mSensorService = pSensorService;
        mSlackService = pSlackService;
        mBrewStatService = pBrewStatService;
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        mBrew = Brew.instance((DelayedExecutorService) this);
        mBrewStatService.deleteZeroBrewEntries();
        normalConfig();
    }


    @Override
    public void schedule(Runnable pRunnable, long pDelay, TimeUnit pDelayTimeUnit) {
        mScheduledExecutorService.schedule(pRunnable, pDelay, pDelayTimeUnit);
    }

    public String onBountyRequest(final String pSlackUser) {
        final String tBountyResp = veryBountyRequest(pSlackUser);
        if (mDebugMode) {
            mSlackService.send(tBountyResp);
            return "";
        }
        return tBountyResp;
    }

    public String onClaimRequest(String pSlackUser) {
        final String tClaimResp = veryBrewClaim(mBrewerService.getBrewer(pSlackUser));
        if (mDebugMode) {
            mSlackService.send(tClaimResp);
            return "";
        }
        return tClaimResp;
    }

    public String onStatsRequest(final String pSlackUser) {
        final String tStats = compileBrewStats(pSlackUser);
        if (mDebugMode) {
            mSlackService.send(tStats);
            return "";
        }
        return tStats;
    }

    public synchronized void onSensorReport(final String pValue, final int pSensorId) {
        final Sensor tSensor = mSensorService.getSensorById(pSensorId);
        if (tSensor != null) {
            SensorType tSensorType = SensorType.getSensorType(tSensor);
            if (tSensorType != null) {
                switch (tSensorType) {
                    case LIGHT:
                        onLuxScan(pValue, tSensor);
                        break;
                    case RFID:
                        cLogger.info("On RFID scan: RFID=" + pValue);

                        final Brewer tBrewerByRfid = mBrewerService.getBrewerByRfid(pValue);
                        if (tBrewerByRfid != null) {
                            veryBrewClaim(tBrewerByRfid);
                        } else {
                            cLogger.info("RFID not registered: " + pValue);
                            //need slack-bot
                            //mSlackService.send("Bleep, bloop! I mean, hello RFID-Brewer! I don't think we've met before? PM slackbot with '!register'");
                        }

                        break;
                    case POWER:
                        cLogger.info("On power scan: power=" + pValue);

                        onPowerScan(pValue, tSensor);
                        break;
                }
            }
        } else {
            cLogger.info("Couldn't find sensor with id: " + pSensorId);
        }
    }

    private void onPowerScan(final String pValue, final Sensor pSensor) {

    }

    private int mPrevLuxValue = -1;

    private void onLuxScan(String pLux, Sensor pSensor) {
        try {
            int tLux = Integer.parseInt(pLux);

            if (tLux > (int) pSensor.getUpperThreshold() && mBrew.inState(WAITFORBREW)) {

                if (mBrewStatService.shouldResetMonthlyStats()) {
                    mSlackService.send(compileBrewStats(null));
                }

                cLogger.info("Starting brew: lux=" + pLux);

                mBrew.startBrew();
                //checkNeverEndingBrew(mBrew.getStartTime(), cBrewCompleteChannelMsg);
                mSlackService.send("Beans injected, brew initialized!");
                mBrewStatService.incrementTodaysBrews();


            } else if (tLux < (int) pSensor.getLowerThreshold()) {

                if (mBrew.inState(BREWING) && mBrew.afterExpectedBrewTime()) {
                    cLogger.info("Brew complete, waiting for drip: lux=" + pLux);

                    mBrew.waitForDrip();
                    List<String> tPrivilegedUsers = new LinkedList<>();
                    Brewer tTopBrewer = mBrewerService.getBrewMaster();

                    if (tTopBrewer != null) {
                        tPrivilegedUsers.add(tTopBrewer.getSlackUser());
                    }

                    if (mBrew.hasBeenClaimed()) {
                        String tSlackBrewClaimer = mBrew.getClaimer().getSlackUser();
                        if (!tPrivilegedUsers.contains(tSlackBrewClaimer)) {
                            tPrivilegedUsers.add(tSlackBrewClaimer);
                        }
                    }
                    schedule(() -> {
                        mBrew.waitForBrew();
                        tPrivilegedUsers.forEach(
                                pUser -> mSlackService.sendToUser(pUser, "Psst! Brew is klar, but don't berätta för anyone!"));
                    }, mMsgDelayAfterCompletedBrew, mMsgDelayAfterCompletedBrewUnit);

                    schedule(() -> mSlackService.send(cBrewCompleteChannelMsg),
                            mChannelDelayAfterCompletedBrew, mChannelDelayAfterCompletedBrewUnit);

                }
            }
            if (tLux != mPrevLuxValue) {
                mPrevLuxValue = tLux;
                cLogger.info("Got lux: " + pLux + ", brewing: " + mBrew.inState(BREWING) + ", afterExpectedBrewTime: " + mBrew.afterExpectedBrewTime() + ", waitingForDrip: " + mBrew.inState(WAITFORDRIP) + ", waitingForBrew: " + mBrew.inState(WAITFORBREW));
            }

        } catch (NumberFormatException nfe) {
            cLogger.error("onLuxScan Failed Integer.parse on: " + pLux);
        }
    }


    private String veryBountyRequest(String pSlackUser) {

        if (mBrew.inState(BREWING) || mBrew.inState(WAITFORDRIP)) {
            return "Brew already in progress.";
        }

        if (mBrew.getActiveBounty() != null) {
            return "Bounty already in progress.";
        }

        Brewer tBountyStarter = mBrewerService.getBrewer(pSlackUser);

        if (tBountyStarter.getBrews() == 0) {
            return "You must claim a few brews before you can register a bounty!";
        }

        mBrew.startBounty(tBountyStarter, this);
        return "*Bean bounty started by " + pSlackUser + "!*"
                + " Claim a brew within " + mBountyHuntDuration + " minutes for extra points!";
    }


    private String veryBrewClaim(Brewer pClaimee) {
        String response = "";

        if (mBrew.hasBeenClaimed()) {
            return "Brew has already been claimed.";
        }

        if (!mBrew.hasBrewedToday()) {
            return "No brew has been started today";
        }

        if (!mBrew.withinClaimableTime()) {
            return "Brew must be claimed within 180 minutes";
        }
        mBrew.claim(pClaimee);

        final BrewBounty tActiveBounty = mBrew.getActiveBounty();

        if (tActiveBounty == null) {
            pClaimee.adjustBrews(1);
            response = "Brew claimed by " + pClaimee.getSlackUser() + ", now having " + pClaimee.getBrews() + " brews! (+1)";

        } else {
            if (tActiveBounty.hasBeenClaimed()) {
                return "Brew bounty has already been claimed.";
            }

            tActiveBounty.claim(pClaimee);

            if (tActiveBounty.startedBy().equals(pClaimee)) {
                pClaimee.adjustBrews(1);
                response = "Nice try " + pClaimee.getSlackUser() + "! But you still get 1 point...";

            } else {
                final Brewer tBountyStarter = mBrewerService.getBrewer(tActiveBounty.startedBy());
                pClaimee.adjustBrews(2);
                tBountyStarter.adjustBrews(-1);
                mBrewerService.save(tBountyStarter);

                response = "Brew bounty claimed by " + pClaimee.getSlackUser() + ", now having " + pClaimee.getBrews() + " brews! (+2)"
                        + "\n_Bounty starter " + tBountyStarter.getSlackUser() + " loses one point, having " + tBountyStarter.getBrews() + " brews. (-1)_";
            }
            mBrew.bountyExpired(tActiveBounty);
        }

        mBrewStatService.incrementTodaysClaimed();

        final Brewer tBrewMaster = mBrewerService.getBrewMaster();
        mBrewerService.save(pClaimee);
        final Brewer tNewBrewMaster = mBrewerService.getBrewMaster();

        if (tNewBrewMaster != null && tNewBrewMaster.equals(pClaimee) && !tNewBrewMaster.equals(tBrewMaster) || mBrewerService.getBrewerCount() == 1) {
            response += "\n*" + pClaimee.getSlackUser() + " is now " + cMasterTitle + "!*";
        }

        return response;
    }

/*
    private void checkNeverEndingBrew(LocalTime pBrewStartTime, String pSlackMessage) {
        mScheduledExecutorService.schedule(() -> {
            if (mBrew.isBrewing() && mBrew.getStartTime().equals(pBrewStartTime)) {
                mSlackService.send(pSlackMessage + "... or brew got canceled prematurely...");
                mBrew.reset();
                cLogger.info("Brew got stuck?");
            }
        }, 8, TimeUnit.MINUTES);
    }
*/


    @Override
    public void bountyExpired(final BrewBounty pBrewBounty) {
        final BrewBounty tActiveBounty = mBrew.getActiveBounty();
        if (tActiveBounty != null && tActiveBounty.equals(pBrewBounty)) {
            mSlackService.send("Bean bounty started by " + tActiveBounty.startedBy().getSlackUser() + " has expired :(");
        }
    }

    private String compileBrewStats(final String pSlackUser) {

        final List<Brewer> tTopBrewers = mBrewerService.getTopBrewers().stream().filter(pBrewer -> pBrewer.getMonthlyBrews() > 0).collect(Collectors.toList());
        final boolean tCompileEndOfMonthStats = mBrewStatService.shouldResetMonthlyStats() && tTopBrewers.size() > 0;
        final Brewer tStatBrewer = tCompileEndOfMonthStats ? null : mBrewerService.getBrewer(pSlackUser);
        final StringBuilder tStrBuilder = new StringBuilder();

        if(mBrewStatService.shouldResetMonthlyStats()) {
            mBrewStatService.monthlyStatsWereReset();
        }

        if (tCompileEndOfMonthStats) {
            mBrewStatService.monthlyStatsWereReset();
            tStrBuilder.append("The brewing challenge of this month has ended!\n");
            if (tTopBrewers.size() == 1) {
                tStrBuilder.append("The winner is:\n");
            } else {
                tStrBuilder.append("The winner(s) are:\n");
            }

            tTopBrewers.forEach(pTopBrewer -> {
                tStrBuilder.append("*" + pTopBrewer.getSlackUser() + "*\n");
                pTopBrewer.incrementWins();
                mBrewerService.save(pTopBrewer);
            });

            tStrBuilder.append("```");
        } else {
            BrewStat tStatsToday = getTodaysBrewStat();
            BrewStat tStatsYday = getYesterdaysBrewStat();
            tStrBuilder.append("```");
            tStrBuilder.append(String.format("Today: \t\t\t brews: %d, claimed: %d\n", tStatsToday.getBrews(), tStatsToday.getClaimed()));
            tStrBuilder.append(String.format("Yesterday: \t\t brews: %d, claimed: %d\n", tStatsYday.getBrews(), tStatsYday.getClaimed()));
        }

        tStrBuilder.append(String.format("This month: \t\t%s \n", averageThisMonth()));
        tStrBuilder.append(String.format("Previous month: \t%s\n", averagePreviousMonth()));
        tStrBuilder.append(String.format("%-6s %-20s %-5s %-7s %-5s\n", "Rating", "Brewer", "Wins", "Brews", "This Month"));
        tStrBuilder.append("------------------------------------------------\n");


        final List<Brewer> tBrewers = mBrewerService.getBrewersSortedByBrewCountThisMonth();

        int tStatLimit = Math.min(tBrewers.size(), 15);

        boolean outOfLimit = false;
        final int tBrewerIndex = tCompileEndOfMonthStats ? 0 : tBrewers.indexOf(tStatBrewer);
        if (!tCompileEndOfMonthStats) {
            if (tBrewerIndex > tStatLimit - 1 || tStatBrewer.getBrews() == 0) {
                outOfLimit = true;
            }
        }

        tBrewers.stream()
                .filter(brewer -> brewer.getBrews() > 0)
                .limit(tStatLimit - (outOfLimit ? 1 : 0))
                .forEach(pBrewer -> tStrBuilder.append(String.format("%-6s %-20s %-5s %-7s %-5s %-10s\n",
                        (tBrewers.indexOf(pBrewer) + 1) + ".", pBrewer.getSlackUser(), pBrewer.getWins(), pBrewer.getBrews(), pBrewer.getMonthlyBrews(),
                        tTopBrewers.contains(pBrewer) ? "<-- " + cMasterTitle + "!" : "")));

        if (outOfLimit) {
            tStrBuilder.append(String.format("...\n%-6s %-20s %-5s %-7s %-5s\n",
                    (tBrewerIndex + 1) + ".", tStatBrewer.getSlackUser(), tStatBrewer.getWins(), tStatBrewer.getBrews(), tStatBrewer.getMonthlyBrews()));
        }

        tStrBuilder.append("```");
        if (tCompileEndOfMonthStats) {
            mBrewerService.getAllBrewers().forEach(pBrewer -> {
                pBrewer.setMonthlyBrews(0);
                mBrewerService.save(pBrewer);
            });
        }
        return tStrBuilder.toString();
    }


    private BrewStat getYesterdaysBrewStat() {
        LocalDate tToday = LocalDate.now();
        LocalDate tQueryDay = tToday.minusDays(1);
        if (tToday.getDayOfWeek() == DayOfWeek.MONDAY) {
            tQueryDay = tQueryDay.minusDays(2);
        }
        return mBrewStatService.getBrewStatByDate(tQueryDay);
    }

    private BrewStat getTodaysBrewStat() {
        return mBrewStatService.getBrewStatByDate(LocalDate.now());
    }

    private String stringifyStats(DoubleSummaryStatistics tStats) {
        if (tStats.getCount() > 0) {
            return String.format("brews: %d, avg: %.1f, min: %d, max: %d", (int) tStats.getSum(), tStats.getAverage(), (int) tStats.getMin(), (int) tStats.getMax());
        }
        return String.format("brews: %d, avg: %.1f, min: %d, max: %d", 0, 0f, 0, 0);
    }

    private Map.Entry<LocalDate, BrewStat[]> mThisMonthStats = null;

    private String averageThisMonth() {
        LocalDate tDate = LocalDate.now();
        if (mThisMonthStats == null || !mThisMonthStats.getKey().equals(tDate.withDayOfMonth(1))) {
            mThisMonthStats = new AbstractMap.SimpleEntry<>(tDate.withDayOfMonth(1), new BrewStat[31]);
        }

        for (int tDay = 1; tDay <= tDate.getDayOfMonth(); tDay++) {
            if (mThisMonthStats.getValue()[tDay] == null) {
                mThisMonthStats.getValue()[tDay] = mBrewStatService.getBrewStatByDate(tDate.withDayOfMonth(tDay));
            }
        }
        DoubleSummaryStatistics tDoubleSummaryStatistics = Arrays.stream(mThisMonthStats.getValue())
                .filter(Objects::nonNull)
                .filter(stat -> stat.getBrews() > 0)
                .filter(stat -> stat.getDate().getDayOfMonth() != tDate.getDayOfMonth())
                .filter(stat -> stat.getDate().getMonth() == tDate.getMonth())
                .filter(stat -> stat.getDate().getYear() == tDate.getYear())
                .mapToDouble(BrewStat::getBrews).summaryStatistics();

        return stringifyStats(tDoubleSummaryStatistics);

    }

    private Map.Entry<LocalDate, String> mPreviousMonthStats = null;

    private String averagePreviousMonth() {
        LocalDate tDate = LocalDate.now().minusMonths(1).withDayOfMonth(1);

        if (mPreviousMonthStats != null && mPreviousMonthStats.getKey().equals(tDate)) {
            return mPreviousMonthStats.getValue();
        }

        DoubleSummaryStatistics tDoubleSummaryStatistics = mBrewStatService.getAllBrewStats()
                .stream()
                .filter(Objects::nonNull)
                .filter(stat -> stat.getBrews() > 0)
                .filter(stat -> stat.getDate().getMonth() == tDate.getMonth())
                .filter(stat -> stat.getDate().getYear() == tDate.getYear())
                .mapToDouble(BrewStat::getBrews).summaryStatistics();

        String tFormattedStats = stringifyStats(tDoubleSummaryStatistics);
        mPreviousMonthStats = new AbstractMap.SimpleEntry<>(tDate, tFormattedStats);
        return tFormattedStats;
    }


    // ====================== DEBUG STUFF ======================================
    private void debugConfig() {
        mChannelDelayAfterCompletedBrew = 8;
        mChannelDelayAfterCompletedBrewUnit = TimeUnit.SECONDS;
        mMsgDelayAfterCompletedBrew = 4;
        mMsgDelayAfterCompletedBrewUnit = TimeUnit.SECONDS;
        mBountyHuntDuration = 30;
        mBountyHuntDurationUnit = TimeUnit.SECONDS;
        mBrewExpectedCompletionTime = 30;
        mBrewExpectedCompletionUnit = ChronoUnit.SECONDS;
        mBrew.configDelayThreshold(mBrewExpectedCompletionTime, mBrewExpectedCompletionUnit);
        mBrew.configBountyDuration(mBountyHuntDuration, mBountyHuntDurationUnit);
    }

    private void normalConfig() {
        mChannelDelayAfterCompletedBrew = 70;
        mChannelDelayAfterCompletedBrewUnit = TimeUnit.SECONDS;
        mMsgDelayAfterCompletedBrew = 50;
        mMsgDelayAfterCompletedBrewUnit = TimeUnit.SECONDS;
        mBountyHuntDuration = 15;
        mBountyHuntDurationUnit = TimeUnit.MINUTES;
        mBrewExpectedCompletionTime = 2;
        mBrewExpectedCompletionUnit = ChronoUnit.MINUTES;
        mBrew.configDelayThreshold(mBrewExpectedCompletionTime, mBrewExpectedCompletionUnit);
        mBrew.configBountyDuration(mBountyHuntDuration, mBountyHuntDurationUnit);
    }

    void toggleDebug() {
        mDebugMode = !mDebugMode;
        mSlackService.toggleDebugMode();

        if (mDebugMode) {
            debugConfig();
            final Random tr = new Random();

            for (int i = 0; i < 20; i++) {
                mBrewerService.deleteBrewer("debug" + i);
                Brewer tBrewer = mBrewerService.getBrewer("debug" + i);
                int tBrewCount = tr.nextInt(10);
                tBrewer.setBrews(tBrewCount);
                /*tBrewCount = tBrewCount == 0 ? 1 : tBrewCount;
                for (int j = 0; j < tBrewCount; j++) {
                    tBrewer.adjustBrews(1);
                }*/
                mBrewerService.save(tBrewer);
            }

            List<BrewStat> tDebugStats = mBrewStatService.findByClaimed(1337);
            tDebugStats.forEach(stat -> mBrewStatService.delete(stat));

            LocalDate tLocalDate = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            for (int i = 0; i < 10; i++) {
                final int tBrewCnt = tr.nextInt(10);
                mBrewStatService.save(new BrewStat(tLocalDate, tBrewCnt, 1337));
                tLocalDate = tLocalDate.plusDays(1);
            }
            tLocalDate = LocalDate.now().withDayOfMonth(1);
            for (int i = 0; i < 10; i++) {
                final int tBrewCnt = tr.nextInt(10);
                mBrewStatService.save(new BrewStat(tLocalDate, tBrewCnt, 1337));
                tLocalDate = tLocalDate.plusDays(1);
            }
        } else {
            normalConfig();
            for (int i = 0; i < 20; i++) {
                mBrewerService.deleteBrewer("debug" + i);
            }
            List<BrewStat> tDebugStats = mBrewStatService.findByClaimed(1337);
            tDebugStats.forEach(stat -> mBrewStatService.delete(stat));
        }

    }

    void blyat(final String pAsd) {
        if (pAsd.equals("blaaat")) {
            mBrewStatService.deleteEverything();
            mBrewerService.deleteEverything();
            mSensorService.deleteEverything();
        }
    }

    void editBrewer(final String pSlackUser, final int pWins, final int pBrews, final int pBrewsPerMonth) {
        final Brewer tBrewer = mBrewerService.getBrewer(pSlackUser);
        tBrewer.setBrews(pBrews);
        tBrewer.setMonthlyBrews(pBrewsPerMonth);
        tBrewer.setWins(pWins);
        mBrewerService.save(tBrewer);
    }
}
