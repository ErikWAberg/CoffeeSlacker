package coffeeslacker.app;

import coffeeslacker.brewer.Brewer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.AbstractMap.SimpleEntry;
import static coffeeslacker.app.BrewState.*;

public class Brew implements BrewBountyListener {

    private static Brew mBrewInstance = new Brew();

    private long mExpectedBrewTime;
    private TemporalUnit mExpectedBrewTimeUnit;
    private long mBountyHuntDuration;
    private TimeUnit mBountyHuntDurationTimeUnit;
    private long mClaimableTime = 20;
    private TemporalUnit mClaimableTimeUnit = ChronoUnit.MINUTES;

    private Brew() {
        mStartTime = LocalDateTime.now();
    }

    public static Brew instance() {
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

    private LocalDateTime mStartTime;
    private BrewState mBrewState = WAITFORBREW;
    private Brewer mClaimer = null;
    private BrewBounty mBounty = null;
    private Map.Entry<LocalDate, Boolean> mHasBrewedToday = new SimpleEntry<>(LocalDate.now(), false);

    public void waitForBrew() {
        mBrewState = WAITFORBREW;
    }

    public void startBrew() {
        mBrewState = BREWING;
        mClaimer = null;
        mStartTime = LocalDateTime.now();
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
	if(mStartTime == null) return false;
        return LocalDateTime.now().isAfter(mStartTime.plus(mExpectedBrewTime, mExpectedBrewTimeUnit));
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
        mBounty.startBountyHuntTimer(mBountyHuntDuration, mBountyHuntDurationTimeUnit);
    }

    public BrewBounty getActiveBounty() {
        return mBounty;
    }

    @Override
    public void bountyExpired(final BrewBounty pBrewBounty) {
        if (mBounty.equals(pBrewBounty)) {
            mBounty.shutdown();
            mBounty = null;
        }
    }

}
