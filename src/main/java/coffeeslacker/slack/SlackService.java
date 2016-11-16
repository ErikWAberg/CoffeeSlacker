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

    public SlackService(final String pWebHook, final String pSlackToken, final String pSlackChannelName, final String pDisplayName, final String pIcon, SlackService pDebugService) {
        cWebHook = pWebHook;
        cSlackToken = pSlackToken;
        cSlackChannelName = pSlackChannelName;
        cBotDisplayName = pDisplayName;
        cIcon = pIcon;

        mSlack = new Slack(pWebHook)
                .icon(pIcon)
                .sendToChannel(pSlackChannelName)
                .displayName(pDisplayName);
        mDebugService = pDebugService;
    }

    public void toggleDebugMode() {
        if(mDebugService != null) {
            mDebugMode = !mDebugMode;
            if(mDebugMode) {
                sendToUser(mDebugUser, "Bleep, bloop! Debug mode is on");
            } else {
                sendToUser(mDebugUser, "Bleep, bloop! Debug mode is off");
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



}
