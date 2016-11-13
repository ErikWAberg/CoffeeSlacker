package coffeeslacker.app;

import coffeeslacker.brewer.Brewer;
import coffeeslacker.brewer.BrewerService;
import coffeeslacker.sensor.Sensor;
import coffeeslacker.sensor.SensorService;
import coffeeslacker.sensor.SensorType;
import coffeeslacker.slack.SlackNotification;
import coffeeslacker.slack.SlackService;
import coffeeslacker.statistics.BrewStat;
import coffeeslacker.statistics.BrewStatService;
import in.ashwanthkumar.slack.webhook.SlackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class CoffeeSlacker {

    private static final Logger cLogger = LoggerFactory.getLogger(CoffeeSlacker.class);
    private static final String cMasterTitle = "Master Elite Bean Injector";
    private final long cBrewCompleteDelay = 75;
    private final long cBrewCompleteLeaderDelay = 55;
    private final int cBountyHuntDurationMinutes = 15;
    private final long cBrewInitDelay = 60;


    private BrewerService mBrewerService;
    private SensorService mSensorService;
    private SlackService mSlackService;
    private BrewStatService mBrewStatService;
    private boolean mBrewing = false;
    private boolean mTagScanned = false;

    private Brewer mPreviousBrewer = null;
    private Brewer mPreviousTopBrewer = null;
    private long mPreviousBrewTime;
    private BeanBounty mBeanBounty = null;

    private ScheduledExecutorService mScheduledExecutorService;

    @Autowired
    public CoffeeSlacker(BrewerService pBrewerService, SensorService pSensorService, SlackService pSlackService, BrewStatService pBrewStatService) {
        mBrewerService = pBrewerService;
        mSensorService = pSensorService;
        mSlackService = pSlackService;
        mBrewStatService = pBrewStatService;
        mPreviousBrewTime = System.currentTimeMillis();
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    private void brewCompleteNotifyLeaders(List<String> pSlackUsers, SlackMessage pSlackMessage) {
        mScheduledExecutorService.schedule(() -> pSlackUsers.forEach(pUser -> mSlackService.sendToUser(pUser, pSlackMessage)), cBrewCompleteLeaderDelay, TimeUnit.SECONDS);
    }

    private void brewCompleteNotifyChannel(SlackMessage pSlackMessage) {
        mScheduledExecutorService.schedule(() -> mSlackService.send(pSlackMessage), cBrewCompleteDelay, TimeUnit.SECONDS);
    }

    void toggleDebug() {
        mSlackService.toggleDebugMode();
    }


    @Async
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
                        onBrewClaim(mBrewerService.getBrewerByRfid(pValue));
                        break;
                    case POWER:
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

    public void onClaimRequest(SlackNotification pNotification) {
        onBrewClaim(mBrewerService.getBrewerBySlackUser(pNotification.getUser_name()));
    }

    private void onBrewClaim(Brewer pBrewer) {

        if (pBrewer != null) {
            if (mBrewing && !mTagScanned) {
                mTagScanned = true;
                mPreviousBrewer = pBrewer;

                if (mBeanBounty != null && mBeanBounty.canBeClaimed(cBountyHuntDurationMinutes)) {
                    Brewer tBountyStarter = mBeanBounty.getBountyStarter();

                    mBeanBounty.claimBounty(pBrewer);
                    mBeanBounty.cancelBountHuntTimer();
                    mBeanBounty = null;

                    if (tBountyStarter.equals(pBrewer)) {
                        mSlackService.send(new SlackMessage("Nice try " + pBrewer.getSlackUser() + "! But you still get 1 point..."));
                        mBrewerService.incrementBrews(pBrewer);
                    } else {
                        mSlackService.send(new SlackMessage("Bean bounty claimed by " + pBrewer.getSlackUser() + ", having " + pBrewer.getBrews() + "+2 brews!"
                                + " _" + tBountyStarter.getSlackUser() + " loses one point, having " + tBountyStarter.getBrews() + "-1 brews_"));
                        mBrewerService.incrementBrews(pBrewer);
                        mBrewerService.incrementBrews(pBrewer);
                        mBrewerService.decrementBrews(tBountyStarter);
                    }

                } else {
                    mSlackService.send(new SlackMessage("Brew claimed by " + pBrewer.getSlackUser() + ", having " + pBrewer.getBrews() + "+1 brews!"));
                    mBrewerService.incrementBrews(pBrewer);
                }


                final Brewer tTopBrewer = mBrewerService.getTopBrewer();

                if (tTopBrewer != null && !tTopBrewer.equals(pBrewer) && pBrewer.isBetterThan(tTopBrewer) || mPreviousTopBrewer == null) {
                    mSlackService.send(new SlackMessage("*" + pBrewer.getSlackUser() + " is now the " + cMasterTitle + "!*"));
                    mPreviousTopBrewer = tTopBrewer;
                }

            } /*else {
                slackSend(new SlackMessage("SYSTEM ALERT! " + tBrewer.getName() + " is trying to cheat!"));
            }*/

        } /*else {
            slackSend(new SlackMessage("Bleep, bloop! I mean, hello Brewer! I don't think we've met before? PM slackbot with '!register'"));
        }*/

    }

    private void onLuxScan(String pLux, Sensor pSensor) {
        try {
            int tLux = Integer.parseInt(pLux);
            if (tLux > (int) pSensor.getUpperThreshold() && !mBrewing && ((System.currentTimeMillis() - mPreviousBrewTime) > cBrewInitDelay * 1000)) {
                mBrewing = true;
                mPreviousBrewTime = System.currentTimeMillis();

                mSlackService.send(new SlackMessage("Beans injected, brew initialized!"));

            } else if (tLux < (int) pSensor.getLowerThreshold() && mBrewing) {
                List<String> tPrivilegedUsers = new LinkedList<>();
                Brewer tTopBrewer = mBrewerService.getTopBrewer();
                if (tTopBrewer != null) {
                    tPrivilegedUsers.add(tTopBrewer.getSlackUser());
                }

                if (mTagScanned && mPreviousBrewer != null) {
                    if (tTopBrewer == null || !tTopBrewer.equals(mPreviousBrewer)) {
                        tPrivilegedUsers.add(mPreviousBrewer.getSlackUser());
                    }
                    mPreviousBrewer = null;
                }

                brewCompleteNotifyLeaders(tPrivilegedUsers, new SlackMessage("Psst! Brew is klar, but don't berätta för anyone!"));
                brewCompleteNotifyChannel(new SlackMessage("*Brew complete*, först to kvarn!"));
                updateTodaysBrewCount();

                mBrewing = false;
                mTagScanned = false;
            }

        } catch (NumberFormatException nfe) {
            cLogger.error("onLuxScan Failed Integer.parse on: " + pLux);
        }
    }


    public String onBountyRequest(String pSlackUser) {
        Brewer tBountyStarter = mBrewerService.getBrewerBySlackUser(pSlackUser);

        if (tBountyStarter.getBrews() > 0) {
            if (mBeanBounty != null && !mBeanBounty.hasBeenClaimed()) {
                return "Bean bounty already in progress!";
            }
            mBeanBounty = new BeanBounty(tBountyStarter);
            mBeanBounty.startBountyHuntTimer(mSlackService, cBountyHuntDurationMinutes);
            return "*Bean bounty started by " + pSlackUser + "!*"
                    + " Claim a brew within " + cBountyHuntDurationMinutes + " minutes for extra points!";
        } else {
            return "You must claim a few brews before you can register a bounty!";
        }
    }

    public String onStatsRequest() {

        StringBuilder tStringBuilder = new StringBuilder("```");

        tStringBuilder.append("Brews today: ").append(getTodaysBrewCount()).append("\n");
        tStringBuilder.append("Brews yesterday: ").append(getYesterdaysBrewCount()).append("\n");
        tStringBuilder.append("Average brews/day this month: ").append(averageThisMonth()).append("  (today excluded)\n");
        tStringBuilder.append("Average brews/day previous month: ").append(averagePreviousMonth()).append("\n");
        tStringBuilder.append("Leader board:\n");
        tStringBuilder.append(String.format("%-10s %-30s %-5s\n", "Rating", "Brewer", "Brews"));
        tStringBuilder.append("------------------------------------------------\n");

        final int[] tScore = {1};

        mBrewerService.getBrewersSortedByBrewCount().forEach(pBrewer -> {
            if (tScore[0] == 1) {
                tStringBuilder.append(String.format("%-10s %-30s %-5s %-10s\n", "" + tScore[0] + ".", pBrewer.getSlackUser(), pBrewer.getBrews(), "<-- " + cMasterTitle + "!"));
            } else {
                tStringBuilder.append(String.format("%-10s %-30s %-5s\n", "" + tScore[0] + ".", pBrewer.getSlackUser(), pBrewer.getBrews()));
            }
            tScore[0]++;
        });

        tStringBuilder.append("```");
        return tStringBuilder.toString();
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
        return averageBrewPerMonth(tQueryDay.getYear(), tQueryDay.getMonth(), tQueryDay.getDayOfMonth());
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
}
