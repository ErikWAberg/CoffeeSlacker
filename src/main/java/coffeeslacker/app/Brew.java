package coffeeslacker.app;

import coffeeslacker.brewer.Brewer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static coffeeslacker.app.Brew.BrewState.BREWING;
import static coffeeslacker.app.Brew.BrewState.WAITFORBREW;
import static coffeeslacker.app.Brew.BrewState.WAITFORDRIP;
import static java.util.AbstractMap.SimpleEntry;

public class Brew implements BrewBountyListener {

    private static DelayedExecutorService mDelayedExecutorServiceService;

    public enum BrewState {
        WAITFORBREW, BREWING, WAITFORDRIP

    }

    private static Brew mBrewInstance = new Brew();

    private long mExpectedBrewTime;
    private TemporalUnit mExpectedBrewTimeUnit;
    private long mBountyHuntDuration;
    private TimeUnit mBountyHuntDurationTimeUnit;
    private long mClaimableTime = 180;
    private TemporalUnit mClaimableTimeUnit = ChronoUnit.MINUTES;

    private Brew() {
    }

    public static Brew instance(final DelayedExecutorService pDelayedScheduler) {
        mDelayedExecutorServiceService = pDelayedScheduler;
        return mBrewInstance;
    }

    void configDelayThreshold(long pExpectedBrewTime, TemporalUnit pExpectedBrewTimeUnit) {
        mExpectedBrewTime = pExpectedBrewTime;
        mExpectedBrewTimeUnit = pExpectedBrewTimeUnit;
    }

    void configBountyDuration(final long pCBountyHuntDuration, final TimeUnit pDurationTimeUnit) {
        mBountyHuntDuration = pCBountyHuntDuration;
        mBountyHuntDurationTimeUnit = pDurationTimeUnit;
    }

    private static LocalDateTime mStartTime = LocalDateTime.now();
    private BrewState mBrewState = WAITFORBREW;
    private Brewer mClaimer = null;
    private BrewBounty mBounty = null;
    private String mAssociatedRfid = null;
    private Map.Entry<LocalDate, Boolean> mHasBrewedToday = new SimpleEntry<>(LocalDate.now(), false);

    public void waitForBrew() {
        mBrewState = WAITFORBREW;
    }

    public void startBrew() {
        mBrewState = BREWING;
        mClaimer = null;
        mStartTime = LocalDateTime.now();
        mAssociatedRfid = null;
        if (!mHasBrewedToday.getKey().equals(LocalDate.now()) || !mHasBrewedToday.getValue()) {
            mHasBrewedToday = new SimpleEntry<>(LocalDate.now(), true);
        }
    }

    public void waitForDrip() {
        mBrewState = WAITFORDRIP;
    }

    public boolean hasBrewedToday() {
        return mHasBrewedToday.getKey().equals(LocalDate.now()) && mHasBrewedToday.getValue();
    }

    public boolean withinClaimableTime() {
        return mStartTime.plus(mClaimableTime, mClaimableTimeUnit).isAfter(LocalDateTime.now());
    }

    public boolean inState(BrewState pBrewState) {
        return mBrewState == pBrewState;
    }

    public boolean afterExpectedBrewTime() {
        return LocalDateTime.now().isAfter(mStartTime.plus(mExpectedBrewTime, mExpectedBrewTimeUnit));
    }

    public void associateWithRfid(String rfid) {
        mAssociatedRfid = rfid;
    }

    public String getAssociatedRfid() {
        return mAssociatedRfid;
    }

    public boolean hasBeenClaimed() {
        return mClaimer != null;
    }

    public void claim(Brewer pClaimer) {
        mClaimer = pClaimer;
    }

    public Brewer getClaimer() {
        return mClaimer;
    }

    public void startBounty(Brewer pBountyStarter, BrewBountyListener pBrewBountyListener) {
        mBounty = new BrewBounty(pBountyStarter);
        mBounty.addBrewBountyListener(pBrewBountyListener);
        mBounty.addBrewBountyListener(this);
        mDelayedExecutorServiceService.schedule(mBounty, mBountyHuntDuration, mBountyHuntDurationTimeUnit);
    }

    public BrewBounty getActiveBounty() {
        return mBounty;
    }

    @Override
    public void bountyExpired(final BrewBounty pBrewBounty) {
        if (mBounty.equals(pBrewBounty)) {
            mBounty = null;
        }
    }

}
