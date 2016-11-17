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

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class CoffeeSlacker implements BrewBountyListener {

    private static final Logger cLogger = LoggerFactory.getLogger(CoffeeSlacker.class);
    private static final String cMasterTitle = "Master Elite Bean Injector";
    private static final String cBrewCompleteChannelMsg = "*Brew complete*, först to kvarn!";

    private long mChannelDelayAfterCompletedBrew = 75;
    private TimeUnit mChannelDelayAfterCompletedBrewUnit = TimeUnit.SECONDS;
    private long mMsgDelayAfterCompletedBrew = 55;
    private TimeUnit mMsgDelayAfterCompletedBrewUnit = TimeUnit.SECONDS;
    private int mBountyHuntDuration = 15;
    private TimeUnit mBountyHuntDurationUnit = TimeUnit.MINUTES;
    private long mBrewExpectedCompletionTime = 4;
    private TemporalUnit mBrewExpectedCompletionUnit = ChronoUnit.MINUTES;


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


    public synchronized void onSensorReport(final String pValue, final int pSensorId) {
        final Sensor tSensor = mSensorService.getSensorById(pSensorId);
        if (tSensor != null) {
            SensorType tSensorType = SensorType.getSensorType(tSensor);
            if (tSensorType != null) {
                switch (tSensorType) {
                    case LIGHT:
                        cLogger.info("On lux update: lux=" + pValue);
                        onLuxScan(pValue, tSensor);
                        break;
                    case RFID:
                        cLogger.info("On RFID scan: RFID=" + pValue);

                        final Brewer tBrewerByRfid = mBrewerService.getBrewerByRfid(pValue);
                        if (tBrewerByRfid != null) {
                            onBrewClaim(tBrewerByRfid);
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

    public void onClaimRequest(String pSlackUser) {
        mSlackService.send(onBrewClaim(mBrewerService.getBrewer(pSlackUser)));
    }

    private String onBrewClaim(Brewer pClaimee) {
        String response = "";

        if (!mBrew.isBrewing()) {
            return "Aint no brew to claim yo.";
        }

        if (mBrew.hasBeenClaimed()) {
            return "Brew has already been claimed.";
        }
        mBrew.claim(pClaimee);

        final BrewBounty tActiveBounty = mBrew.getActiveBounty();
        if (tActiveBounty != null) {

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

        } else {
            pClaimee.adjustBrews(1);
            response = "Brew claimed by " + pClaimee.getSlackUser() + ", now having " + pClaimee.getBrews() + " brews! (+1)";
        }

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
            if (mBrew.isWaitingForDrip()) {
                pSlackUsers.forEach(pUser -> mSlackService.sendToUser(pUser, pSlackMessage));
            }

        }, mMsgDelayAfterCompletedBrew, mMsgDelayAfterCompletedBrewUnit);
    }

    private void brewCompleteNotifyChannel(String pSlackMessage) {
        mScheduledExecutorService.schedule(() -> {
            if (mBrew.isWaitingForDrip()) {
                mSlackService.send(pSlackMessage);
                mBrew.reset();
            }
        }, mChannelDelayAfterCompletedBrew, mChannelDelayAfterCompletedBrewUnit);
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

    private void onLuxScan(String pLux, Sensor pSensor) {
        try {
            int tLux = Integer.parseInt(pLux);

            if (tLux > (int) pSensor.getUpperThreshold() && !mBrew.isBrewing()) {
                mBrew.startBrew();
                //checkNeverEndingBrew(mBrew.getStartTime(), cBrewCompleteChannelMsg);
                mSlackService.send("Beans injected, brew initialized!");
                mBrewStatService.incrementTodaysBrews();

            } else if (tLux < (int) pSensor.getLowerThreshold() && mBrew.isBrewing()) {

                if (mBrew.afterExpectedBrewTime() && !mBrew.isWaitingForDrip()) {
                    mBrew.waitForDrip();
                    List<String> tPrivilegedUsers = new LinkedList<>();
                    Brewer tTopBrewer = mBrewerService.getBrewMaster();

                    if (tTopBrewer != null) {
                        tPrivilegedUsers.add(tTopBrewer.getSlackUser());
                    }

                    if (mBrew.hasBeenClaimed()) {
                        if (!tPrivilegedUsers.contains(mBrew.getClaimer().getSlackUser())) {
                            tPrivilegedUsers.add(mBrew.getClaimer().getSlackUser());
                        }
                    }

                    brewCompleteNotifyLeaders(tPrivilegedUsers, "Psst! Brew is klar, but don't berätta för anyone!");
                    brewCompleteNotifyChannel(cBrewCompleteChannelMsg);

                } else {
                    cLogger.info("Got finished brew, but afterExpectedBrewTime: " + mBrew.afterExpectedBrewTime() + " waitingForDrip: " + mBrew.isWaitingForDrip());
                }

            }

        } catch (NumberFormatException nfe) {
            cLogger.error("onLuxScan Failed Integer.parse on: " + pLux);
        }
    }


    public String onBountyRequest(String pSlackUser) {

        if (mBrew.isBrewing()) {
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


    @Override
    public void bountyExpired(final BrewBounty pBrewBounty) {
        final BrewBounty tActiveBounty = mBrew.getActiveBounty();
        if (tActiveBounty != null && tActiveBounty.equals(pBrewBounty)) {
            mSlackService.send("Bean bounty started by " + tActiveBounty.startedBy().getSlackUser() + " has expired :(");
        }
    }


    public String onStatsRequest(final String pSlackUser) {

        StringBuilder tStrBuilder = new StringBuilder("```");

        tStrBuilder.append(String.format("Brews today: %d, yesterday: %d\n", getTodaysBrewCount(), getYesterdaysBrewCount()));
        tStrBuilder.append(String.format("Stats this month (today excluded): %s \n", averageThisMonth()));
        tStrBuilder.append(String.format("Stats previous month: %s\n", averagePreviousMonth()));
        tStrBuilder.append(String.format("%-10s %-20s %-5s\n", "Rating", "Brewer", "Brews"));
        tStrBuilder.append("------------------------------------------------\n");

        final Brewer tStatBrewer = mBrewerService.getBrewer(pSlackUser);
        List<Brewer> tBrewers = mBrewerService.getBrewersSortedByBrewCount();
        Brewer tBrewMaster = mBrewerService.getBrewMaster();

        int tStatLimit = 15 > tBrewers.size() ? tBrewers.size() : 15;

        final int tBrewerIndex = tBrewers.indexOf(tStatBrewer);
        boolean outOfLimit = false;
        if (tBrewerIndex > tStatLimit - 1) {
            outOfLimit = true;
        }
        int i = 0;
        if (tBrewMaster != null && tBrewMaster.getBrews() > 0) {
            tStrBuilder.append(String.format("%-10s %-20s %-5s %-10s\n",
                    (i + 1) + ".", tBrewMaster.getSlackUser(), tBrewMaster.getBrews(), "<-- " + cMasterTitle + "!"));
            i++;
        }

        for (; i < tStatLimit - (outOfLimit ? 1 : 0); i++) {
            Brewer tBrewer = tBrewers.get(i);
            tStrBuilder.append(String.format("%-10s %-20s %-5s\n",
                    (i + 1) + ".", tBrewer.getSlackUser(), tBrewer.getBrews()));
        }

        if (outOfLimit) {
            tStrBuilder.append(String.format("...\n%-10s %-20s %-5s\n",
                    (tBrewerIndex + 1) + ".", tStatBrewer.getSlackUser(), tStatBrewer.getBrews()));
        }

        tStrBuilder.append("```");
        return tStrBuilder.toString();
    }


    private int getYesterdaysBrewCount() {
        LocalDate tToday = LocalDate.now();
        LocalDate tQueryDay = tToday.minusDays(1);
        if (tToday.getDayOfWeek() == DayOfWeek.MONDAY) {
            tQueryDay = tQueryDay.minusDays(2);
        }
        return mBrewStatService.getBrewStatByDate(tQueryDay).getBrews();
    }

    private int getTodaysBrewCount() {
        return mBrewStatService.getBrewStatByDate(LocalDate.now()).getBrews();
    }

    private String stringifyStats(DoubleSummaryStatistics tStats) {
        if(tStats.getCount() > 0) {
            return String.format("brews: %d, brews/day: %.1f, min: %d, max: %d", (int)tStats.getSum(), tStats.getAverage(), (int)tStats.getMin(), (int)tStats.getMax());
        }
        return String.format("brews: %d, brews/day: %.1f, min: %d, max: %d", 0, 0f, 0, 0);
    }

    private Map.Entry<LocalDate, BrewStat[]> mThisMonthStats = null;
    private String averageThisMonth() {
        LocalDate tDate = LocalDate.now();
        if(mThisMonthStats == null || !mThisMonthStats.getKey().equals(tDate.withDayOfMonth(1))) {
             mThisMonthStats = new AbstractMap.SimpleEntry<>(tDate.withDayOfMonth(1), new BrewStat[31]);
        }

        for(int tDay = 1; tDay <= tDate.getDayOfMonth(); tDay++) {
            if(mThisMonthStats.getValue()[tDay] == null) {
                mThisMonthStats.getValue()[tDay] = mBrewStatService.getBrewStatByDate(tDate.withDayOfMonth(tDay));
            }
        }
        DoubleSummaryStatistics tDoubleSummaryStatistics = Arrays.stream(mThisMonthStats.getValue())
                .filter(stat -> stat != null)
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

        if(mPreviousMonthStats != null && mPreviousMonthStats.getKey().equals(tDate)) {
            return mPreviousMonthStats.getValue();
        }

        DoubleSummaryStatistics tDoubleSummaryStatistics = mBrewStatService.getAllBrewStats()
                .stream()
                .filter(stat ->
                                stat.getDate().getMonth() == tDate.getMonth() &&
                                stat.getDate().getYear() == tDate.getYear())
                .mapToDouble(BrewStat::getBrews).summaryStatistics();;
        String tFormattedStats = stringifyStats(tDoubleSummaryStatistics);
        mPreviousMonthStats = new AbstractMap.SimpleEntry<>(tDate, tFormattedStats);
        return tFormattedStats;
    }



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
          /*  for(int i = 0; i < 10; i++) {
                mBrewerService.deleteBrewer("debug" + i);
                mBrewerService.getBrewer("debug" + i);
            }
            final Random tr = new Random();
            mBrewStatService.deleteEverything();
            LocalDate tLocalDate = LocalDate.of(2016,10,1);
            for(int i = 0; i < 10; i++) {
                mBrewStatService.save(new BrewStat(tLocalDate, tr.nextInt(10)));
                tLocalDate = tLocalDate.plusDays(1);
            }
            tLocalDate = LocalDate.of(2016,11,8);
            for(int i = 0; i < 10; i++) {
                mBrewStatService.save(new BrewStat(tLocalDate, tr.nextInt(10)));
                tLocalDate = tLocalDate.plusDays(1);
            } */
        } else {
            normalConfig();
            for(int i = 0; i < 10; i++) {
                mBrewerService.deleteBrewer("debug" + i);
            }
        }
        mBrew.configDelayThreshold(mBrewExpectedCompletionTime, mBrewExpectedCompletionUnit);
        mBrew.configBountyDuration(mBountyHuntDuration, mBountyHuntDurationUnit);
    }

    public void blyat(final String pAsd) {
        if(pAsd.equals("blaaat")) {
            mBrewStatService.deleteEverything();
            mBrewerService.deleteEverything();
            mSensorService.deleteEverything();
        }
    }
}
