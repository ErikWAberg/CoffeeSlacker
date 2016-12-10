package coffeeslacker.app;

import coffeeslacker.brewer.Brewer;
import coffeeslacker.slack.SlackService;
import in.ashwanthkumar.slack.webhook.SlackMessage;

import java.time.LocalDateTime;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BrewBounty implements Runnable {

    private final Brewer mBountyStarter;
    private final LocalDateTime mStartDateTime;
    private Brewer mBountyClaimee = null;

    private LinkedList<BrewBountyListener> mBrewBountyListeners;

    public BrewBounty(final Brewer pBountyStarter) {
        mBountyStarter = pBountyStarter;
        mStartDateTime = LocalDateTime.now();
    }

    public void claim(Brewer pBountyClaimee) {
        mBountyClaimee = pBountyClaimee;
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

    public void addBrewBountyListener(final BrewBountyListener pBrewBountyListener) {
        if(mBrewBountyListeners == null) {
            mBrewBountyListeners = new LinkedList<>();
        }
        mBrewBountyListeners.add(pBrewBountyListener);
    }

    public Brewer startedBy() {
        return mBountyStarter;
    }

    @Override
    public void run() {
        if(!hasBeenClaimed()) {
            mBrewBountyListeners.forEach(pBrewBountyListener -> pBrewBountyListener.bountyExpired(this));
        }
    }
}
