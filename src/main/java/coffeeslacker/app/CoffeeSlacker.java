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

import static coffeeslacker.app.BrewState.*;


@Service
public class CoffeeSlacker implements BrewBountyListener {

    private static final Logger cLogger = LoggerFactory.getLogger(CoffeeSlacker.class);
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

    private Brew mBrew = Brew.instance();
    private boolean mDebugMode = false;

    @Autowired
    public CoffeeSlacker(BrewerService pBrewerService, SensorService pSensorService, SlackService pSlackService, BrewStatService pBrewStatService) {
        mBrewerService = pBrewerService;
        mSensorService = pSensorService;
        mSlackService = pSlackService;
        mBrewStatService = pBrewStatService;
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        normalConfig();
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

                    brewCompleteNotifyLeaders(tPrivilegedUsers, "Psst! Brew is klar, but don't berätta för anyone!");
                    brewCompleteNotifyChannel(cBrewCompleteChannelMsg);

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

        if (mBrew.inState(BREWING)) {
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
            return "Brew must be claimed within 20 minutes";
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
        }

        mBrewStatService.incrementTodaysClaimed();

        final Brewer tBrewMaster = mBrewerService.getBrewMaster();
        mBrewerService.save(pClaimee);
        final Brewer tNewBrewMaster = mBrewerService.getBrewMaster();

        if (tNewBrewMaster != null && tNewBrewMaster.equals(pClaimee) && !tNewBrewMaster.equals(tBrewMaster) || mBrewerService.getBrewerCount() == 1) {
            response += "\n*" + pClaimee.getSlackUser() + " is now the " + cMasterTitle + "!*";
        }

        return response;
    }


    private void brewCompleteNotifyLeaders(List<String> pSlackUsers, String pSlackMessage) {
        mScheduledExecutorService.schedule(() -> {
                    pSlackUsers.forEach(pUser -> mSlackService.sendToUser(pUser, pSlackMessage));
                    mBrew.waitForBrew();
                },
                mMsgDelayAfterCompletedBrew, mMsgDelayAfterCompletedBrewUnit);
    }

    private void brewCompleteNotifyChannel(String pSlackMessage) {
        mScheduledExecutorService.schedule(() -> mSlackService.send(pSlackMessage),
                mChannelDelayAfterCompletedBrew, mChannelDelayAfterCompletedBrewUnit);
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

        StringBuilder tStrBuilder = new StringBuilder("```");

        BrewStat tStatsToday = getTodaysBrewStat();
        BrewStat tStatsYday = getYesterdaysBrewStat();
        tStrBuilder.append(String.format("Today: brews=%d, claimed=%d\n", tStatsToday.getBrews(), tStatsToday.getClaimed()));
        tStrBuilder.append(String.format("Yesterday: brews=%d, claimed=%d\n", tStatsYday.getBrews(), tStatsYday.getClaimed()));
        tStrBuilder.append(String.format("Stats this month (today excluded): %s \n", averageThisMonth()));
        tStrBuilder.append(String.format("Stats previous month: %s\n", averagePreviousMonth()));
        tStrBuilder.append(String.format("%-10s %-20s %-5s\n", "Rating", "Brewer", "Brews"));
        tStrBuilder.append("------------------------------------------------\n");

        final Brewer tStatBrewer = mBrewerService.getBrewer(pSlackUser);
        List<Brewer> tBrewers = mBrewerService.getBrewersSortedByBrewCount();
        final Brewer tBrewMaster = mBrewerService.getBrewMaster();

        int tStatLimit = 15 > tBrewers.size() ? tBrewers.size() : 15;

        final int tBrewerIndex = tBrewers.indexOf(tStatBrewer);
        boolean outOfLimit = false;
        if (tBrewerIndex > tStatLimit - 1 || tStatBrewer.getBrews() == 0) {
            outOfLimit = true;
        }

        if (tBrewMaster != null && tBrewMaster.getBrews() > 0) {
            tStrBuilder.append(String.format("%-10s %-20s %-5s %-10s\n",
                    1 + ".", tBrewMaster.getSlackUser(), tBrewMaster.getBrews(), "<-- " + cMasterTitle + "!"));
        }

        tBrewers.stream()
                .filter(brewer -> brewer.getBrews() > 0)
                .filter(brewer -> tBrewMaster == null || !brewer.equals(tBrewMaster))
                .limit(tStatLimit - (outOfLimit ? 1 : 0))
                .forEach(brewer -> tStrBuilder.append(String.format("%-10s %-20s %-5s\n",
                        (tBrewers.indexOf(brewer) + 1) + ".", brewer.getSlackUser(), brewer.getBrews())));


        if (outOfLimit) {
            tStrBuilder.append(String.format("...\n%-10s %-20s %-5s\n",
                    (tBrewerIndex + 1) + ".", tStatBrewer.getSlackUser(), tStatBrewer.getBrews()));
        }

        tStrBuilder.append("```");
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
                .filter(stat -> stat != null)
                .filter(stat -> stat.getBrews() > 0)
                .filter(stat ->
                        stat.getDate().getDayOfMonth() != tDate.getDayOfMonth() &&
                                stat.getDate().getMonth() == tDate.getMonth() &&
                                stat.getDate().getYear() == tDate.getYear())
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
                .filter(stat ->
                        stat.getDate().getMonth() == tDate.getMonth() &&
                                stat.getDate().getYear() == tDate.getYear())
                .mapToDouble(BrewStat::getBrews).summaryStatistics();
        ;
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
    }

    void toggleDebug() {
        mDebugMode = !mDebugMode;
        mSlackService.toggleDebugMode();

        if (mDebugMode) {
            debugConfig();
            final Random tr = new Random();

            for (int i = 0; i < 16; i++) {
                mBrewerService.deleteBrewer("debug" + i);
                Brewer tBrewer = mBrewerService.getBrewer("debug" + i);
                int tBrewCount = tr.nextInt(10);
                tBrewCount = tBrewCount == 0 ? 1 : tBrewCount;
                tBrewer.adjustBrews(tBrewCount);
                mBrewerService.save(tBrewer);
            }

            LocalDate tLocalDate = LocalDate.of(2016, 10, 10);
            for (int i = 0; i < 10; i++) {
                final int tBrewCnt = tr.nextInt(10);
                mBrewStatService.save(new BrewStat(tLocalDate, tBrewCnt, 1337));
                tLocalDate = tLocalDate.plusDays(1);
            }
            tLocalDate = LocalDate.of(2016, 11, 10);
            for (int i = 0; i < 10; i++) {
                final int tBrewCnt = tr.nextInt(10);
                mBrewStatService.save(new BrewStat(tLocalDate, tBrewCnt, 1337));
                tLocalDate = tLocalDate.plusDays(1);
            }
        } else {
            normalConfig();
            for (int i = 0; i < 16; i++) {
                mBrewerService.deleteBrewer("debug" + i);
            }
            List<BrewStat> tDebugStats = mBrewStatService.findByClaimed(1337);
            tDebugStats.forEach(stat -> mBrewStatService.delete(stat));
        }
        mBrew.configDelayThreshold(mBrewExpectedCompletionTime, mBrewExpectedCompletionUnit);
        mBrew.configBountyDuration(mBountyHuntDuration, mBountyHuntDurationUnit);
    }

    void blyat(final String pAsd) {
        if (pAsd.equals("blaaat")) {
            mBrewStatService.deleteEverything();
            mBrewerService.deleteEverything();
            mSensorService.deleteEverything();
        }
    }

    void editBrewer(final String pSlackUser, final int pBrews) {
        final Brewer tBrewer = mBrewerService.getBrewer(pSlackUser);
        tBrewer.setBrews(pBrews);
        mBrewerService.save(tBrewer);
    }
}
