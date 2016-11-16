package coffeeslacker.app;

import coffeeslacker.brewer.Brewer;
import coffeeslacker.slack.SlackService;
import in.ashwanthkumar.slack.webhook.SlackMessage;

import java.time.LocalDateTime;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BrewBounty {

    private final Brewer mBountyStarter;
    private final LocalDateTime mStartDateTime;
    private Brewer mBountyClaimee = null;
    private ScheduledExecutorService mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private LinkedList<BrewBountyListener> mBrewBountyListeners;

    public BrewBounty(final Brewer pBountyStarter) {
        mBountyStarter = pBountyStarter;
        mStartDateTime = LocalDateTime.now();
    }

    public void claim(Brewer pBountyClaimee) {
        mBountyClaimee = pBountyClaimee;
        shutdown();
    }

    public void shutdown() {
        mScheduledExecutorService.shutdown();
    }

    public boolean hasBeenClaimed() {
        return mBountyClaimee != null;
    }


    @Override
    public boolean equals(final Object obj) {
        if(obj == null) return false;

        if(obj instanceof BrewBounty) {
            BrewBounty tOther = (BrewBounty) obj;
            return tOther.mStartDateTime.equals(mStartDateTime) && tOther.mBountyStarter.equals(mBountyStarter);
        }
        return false;
    }


    public void startBountyHuntTimer(final long pBountyHuntDuration, final TimeUnit pDurationTimeUnit) {
        mScheduledExecutorService.schedule(() -> {
            if(!hasBeenClaimed()) {
                mBrewBountyListeners.forEach(pBrewBountyListener -> pBrewBountyListener.bountyExpired(this));
            }
        }, pBountyHuntDuration, pDurationTimeUnit);
    }

    public void addBrewBountyListener(final BrewBountyListener pBrewBountyListener) {
        if(mBrewBountyListeners == null) {
            mBrewBountyListeners = new LinkedList<>();
        }
        mBrewBountyListeners.add(pBrewBountyListener);
    }

    public Brewer startedBy() {
        return mBountyStarter;
    }
}
