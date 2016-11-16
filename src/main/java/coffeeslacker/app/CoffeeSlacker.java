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
import java.time.LocalDateTime;
import java.time.Month;
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
        toggleDebug();
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

        mBrewerService.save(pClaimee);

        final Brewer tBrewMaster = mBrewerService.getBrewMaster();

        if (tBrewMaster == null || (!pClaimee.equals(tBrewMaster) && pClaimee.isBetterThan(tBrewMaster))) {
            response += "\n*" + pClaimee.getSlackUser() + " is now the " + cMasterTitle + "!*";
        }
        return response;
    }


    private void brewCompleteNotifyLeaders(List<String> pSlackUsers, String pSlackMessage) {
        mScheduledExecutorService.schedule(() -> {
            if(mBrew.isWaitingForDrip()) {
                pSlackUsers.forEach(pUser -> mSlackService.sendToUser(pUser, pSlackMessage));
            }

        }, mMsgDelayAfterCompletedBrew, mMsgDelayAfterCompletedBrewUnit);
    }

    private void brewCompleteNotifyChannel(String pSlackMessage) {
        mScheduledExecutorService.schedule(() -> {
            if(mBrew.isWaitingForDrip()) {
                mSlackService.send(pSlackMessage);
                mBrew.reset();
            }
        }, mChannelDelayAfterCompletedBrew, mChannelDelayAfterCompletedBrewUnit);
    }

    private void onLuxScan(String pLux, Sensor pSensor) {
        try {
            int tLux = Integer.parseInt(pLux);

            if (tLux > (int) pSensor.getUpperThreshold() && !mBrew.isBrewing()) {
                mBrew.startBrew();
                mSlackService.send("Beans injected, brew initialized!");

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
                    brewCompleteNotifyChannel("*Brew complete*, först to kvarn!");
                    updateTodaysBrewCount();

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
            mSlackService.send("Bean bounty started by " + tActiveBounty.startedBy() + " has expired :(");
        }
    }


    public String onStatsRequest(final String pSlackUser) {

        StringBuilder tStrBuilder = new StringBuilder("```");

        tStrBuilder.append(String.format("Brews today: %d, yesterday: %d\n", getTodaysBrewCount(), getYesterdaysBrewCount()));
        tStrBuilder.append(String.format("Average brews/day this month: %f (today excluded)\n", averageThisMonth()));
        tStrBuilder.append(String.format("Average brews/day previous month: %f\n", averagePreviousMonth()));
        tStrBuilder.append(String.format("%-10s %-20s %-5s\n", "Rating", "Brewer", "Brews"));
        tStrBuilder.append("------------------------------------------------\n");

        final Brewer tStatBrewer = mBrewerService.getBrewer(pSlackUser);
        List<Brewer> tBrewers = mBrewerService.getBrewersSortedByBrewCount();
        Brewer tBrewMaster = mBrewerService.getBrewMaster();

        int tStatLimit = 15;

        final int tBrewerIndex = tBrewers.indexOf(tStatBrewer);
        boolean outOfLimit = false;
        if (tBrewerIndex > tStatLimit - 1) {
            outOfLimit = true;
        }
        int i = 0;
        if(tBrewMaster != null) {
            tStrBuilder.append(String.format("%-10s %-20s %-5s %-10s\n",
                    (i + 1) + ".", tBrewMaster.getSlackUser(), tBrewMaster.getBrews(), "<-- " + cMasterTitle + "!" ));
            i++;
        }

        for(; i < tStatLimit - (outOfLimit ? 1 : 0); i++) {
            Brewer tBrewer = tBrewers.get(i);
            tStrBuilder.append(String.format("%-10s %-20s %-5s\n",
                    (i + 1) + ".", tBrewer.getSlackUser(), tBrewer.getBrews()));
        }

        if(outOfLimit) {
            tStrBuilder.append(String.format("...\n%-10s %-20s %-5s\n",
                    (tBrewerIndex + 1) + ".", tStatBrewer.getSlackUser(), tStatBrewer.getBrews()));
        }

        tStrBuilder.append("```");
        return tStrBuilder.toString();
    }

    private void updateTodaysBrewCount() {
        LocalDate tToday = LocalDate.now();
        BrewStat tBrewStat = mBrewStatService.getBrewStatByDate(tToday);
        mBrewStatService.incrementBrews(tBrewStat);
    }

    private int getYesterdaysBrewCount() {
        LocalDate tToday = LocalDate.now();
        LocalDate tQueryDay = tToday.minusDays(1);
        if (tToday.getDayOfWeek() == DayOfWeek.MONDAY) {
            tQueryDay = tQueryDay.minusDays(2);
        }
        BrewStat tBrewStat = mBrewStatService.getBrewStatByDate(tQueryDay);
        return tBrewStat.getBrews();
    }

    private int getTodaysBrewCount() {
        LocalDate tToday = LocalDate.now();
        BrewStat tBrewStat = mBrewStatService.getBrewStatByDate(tToday);
        return tBrewStat.getBrews();
    }

    private double averageThisMonth() {
        LocalDate tToday = LocalDate.now();
        return averageBrewPerMonth(tToday.getYear(), tToday.getMonth(), tToday.getDayOfMonth());
    }

    private double averagePreviousMonth() {
        LocalDate tQueryDay = LocalDate.now().minusMonths(1);
        return averageBrewPerMonth(tQueryDay.getYear(), tQueryDay.getMonth(), -1);
    }


    private double averageBrewPerMonth(int pYear, Month pMonth, int day) {
        final LocalDate tDate = LocalDate.of(pYear, pMonth, day == -1 ? 1 : day);
        final List<BrewStat> tAllBrewStats = mBrewStatService.getAllBrewStats();

        final Stream<BrewStat> tStatStreamForMonth = tAllBrewStats.stream()
                .filter(stat -> ((day == -1 || stat.getDate().getDayOfMonth() != tDate.getDayOfMonth()) &&
                        stat.getDate().getMonth() == tDate.getMonth() &&
                        stat.getDate().getYear() == tDate.getYear()));

        OptionalDouble tBrewAverageForMonth = tStatStreamForMonth
                .mapToInt(BrewStat::getBrews).average();
        return tBrewAverageForMonth.isPresent() ? tBrewAverageForMonth.getAsDouble() : 0;
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
        } else {
            normalConfig();
        }
        mBrew.configDelayThreshold(mBrewExpectedCompletionTime, mBrewExpectedCompletionUnit);
        mBrew.configBountyDuration(mBountyHuntDuration, mBountyHuntDurationUnit);
    }
}
