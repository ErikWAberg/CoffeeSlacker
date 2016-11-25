package coffeeslacker.slack;

import in.ashwanthkumar.slack.webhook.Slack;
import in.ashwanthkumar.slack.webhook.SlackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SlackService {

    private static final Logger cLogger = LoggerFactory.getLogger(SlackService.class);

    private final String cWebHook;
    private final String cSlackToken;
    private final String cSlackChannelName;
    private final String cBotDisplayName;
    private final String cIcon;

    private Slack mSlack;
    private SlackService mDebugService;
    private String mDebugUser;
    private boolean mDebugMode = false;

    public SlackService(final String pWebHook, final String pSlackToken, final String pSlackChannelName, final String pDisplayName, final String pIcon) {
        cWebHook = pWebHook;
        cSlackToken = pSlackToken;
        cSlackChannelName = pSlackChannelName;
        cBotDisplayName = pDisplayName;
        cIcon = pIcon;

        mSlack = new Slack(pWebHook)
                .icon(pIcon)
                .sendToChannel(pSlackChannelName)
                .displayName(pDisplayName);
    }

    public void toggleDebugMode() {
        if(mDebugService != null && mDebugUser != null) {
            mDebugMode = !mDebugMode;
            if(mDebugMode) {
                try {
                    mSlack.sendToUser(mDebugUser).push(new SlackMessage("Bleep, bloop! Debug mode is on"));
                } catch (IOException pE) {
                }
            } else {
                try {
                    mSlack.sendToUser(mDebugUser).push(new SlackMessage("Bleep, bloop! Debug mode is off"));
                } catch (IOException pE) {
                }
            }
        }
    }

    public synchronized void sendToUser(String pUserId, String pSlackMessage) {
        try {
            if(mDebugMode) {
                mDebugService.sendToUser(mDebugUser, pSlackMessage + " (debug: was intended for " + pUserId + ")");
            } else {
                mSlack.sendToUser(pUserId).push(new SlackMessage(pSlackMessage));
            }

        } catch(IOException e) {
            cLogger.error("Can't send message to slack-user" + pUserId + " message: " + pSlackMessage, e);
        }
    }

    public synchronized void send(String pSlackMessage) {
        try {

            if(mDebugMode) {
                mDebugService.send(pSlackMessage);
            } else {
                mSlack.sendToChannel(cSlackChannelName).push(new SlackMessage(pSlackMessage));
            }
        } catch(IOException e) {
            cLogger.error("Can't send message to slack! " + pSlackMessage, e);

        }


    }

    public boolean tokenMatches(String pSlackToken) {
        return cSlackToken.equals(pSlackToken);
    }

    public boolean channelMatches(String pSlackChannelName) {
        return cSlackChannelName.equals(pSlackChannelName);
    }

    public void setupDebugServices(final SlackService pDebugService, final String pSlackDebugUser) {
        mDebugService = pDebugService;
        mDebugUser = pSlackDebugUser;
    }
}
