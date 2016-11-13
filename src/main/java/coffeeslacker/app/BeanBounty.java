package coffeeslacker.app;

import coffeeslacker.brewer.Brewer;
import coffeeslacker.slack.SlackService;
import in.ashwanthkumar.slack.webhook.SlackMessage;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BeanBounty {

    private final Brewer mBountyStarter;
    private final LocalDateTime mStartDateTime;
    private Brewer mBountyClaimee = null;
    private ScheduledExecutorService mScheduledExecutorService;

    public BeanBounty(final Brewer pBountyStarter) {
        mBountyStarter = pBountyStarter;
        mStartDateTime = LocalDateTime.now();
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void claimBounty(Brewer pBountyClaimee) {
        mBountyClaimee = pBountyClaimee;
    }

    public boolean hasBeenClaimed() {
        return mBountyClaimee != null;
    }

    public boolean canBeClaimed(int pMinutesLimit) {
        return !hasBeenClaimed() && LocalDateTime.now().isBefore(mStartDateTime.plusMinutes(pMinutesLimit));
    }

    public Brewer getBountyStarter() {
        return mBountyStarter;
    }

    public void startBountyHuntTimer(SlackService pSlackService, int durationInMinutes) {
        mScheduledExecutorService.schedule(() -> {
            if(!hasBeenClaimed()) {
                pSlackService.send(new SlackMessage("Bean bounty started by " + mBountyStarter.getSlackUser() + " has expired :("));
            }
        }, durationInMinutes, TimeUnit.MINUTES);
    }

    public void cancelBountHuntTimer() {
        mScheduledExecutorService.shutdownNow();
    }
}
