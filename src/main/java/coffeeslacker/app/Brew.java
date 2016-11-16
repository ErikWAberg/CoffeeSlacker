package coffeeslacker.app;

import coffeeslacker.brewer.Brewer;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

public class Brew implements BrewBountyListener {

    private static Brew mBrewInstance = new Brew();
    private LocalTime mStartTime;

    private long mDelayAmount;
    private TemporalUnit mDelayTimeUnit = ChronoUnit.MINUTES;

    private long mBountyHuntDuration;
    private TimeUnit mBountyHuntDurationTimeUnit;
    private boolean mWaitingForDrip;


    private Brew() {
        mStartTime = LocalTime.now();
    }

    public static Brew instance() {
        return mBrewInstance;
    }

    public void configDelayThreshold(long pDelayAmount, TemporalUnit pDelayTimeUnit) {
        mDelayAmount = pDelayAmount;
        mDelayTimeUnit = pDelayTimeUnit;
    }

    public void configBountyDuration(final long pCBountyHuntDuration, final TimeUnit pDurationTimeUnit) {
        mBountyHuntDuration = pCBountyHuntDuration;
        mBountyHuntDurationTimeUnit = pDurationTimeUnit;
    }

    private Brewer mClaimer = null;
    private boolean mBrewing = false;
    private BrewBounty mBounty = null;

    public void reset() {
        mClaimer = null;
        mBrewing = false;
        mBounty = null;
        mWaitingForDrip = false;
    }

    public void startBrew() {
        mBrewing = true;
        mStartTime = LocalTime.now();
    }

    public boolean isBrewing() {
        return mBrewing;
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
        if(mBounty.equals(pBrewBounty)) {
            mBounty.shutdown();
            mBounty = null;
        }
    }

    public boolean afterExpectedBrewTime() {
        return LocalTime.now().isAfter(mStartTime.plus(mDelayAmount, mDelayTimeUnit));
    }

    public void waitForDrip() {
        mWaitingForDrip = true;
    }

    public boolean isWaitingForDrip() {
        return mWaitingForDrip;
    }
}
