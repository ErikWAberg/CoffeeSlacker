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
    private final String mDebugUser;

    private Slack mSlack;
    private boolean mDebugMode = false;

    public SlackService(final String pWebHook, final String pSlackToken, final String pSlackChannelName, final String pDisplayName, final String pIcon, final String pDebugUser) {
        cWebHook = pWebHook;
        cSlackToken = pSlackToken;
        cSlackChannelName = pSlackChannelName;
        cBotDisplayName = pDisplayName;
        cIcon = pIcon;
        mDebugUser = pDebugUser;

        mSlack = new Slack(pWebHook)
                .icon(pIcon)
                .sendToChannel(pSlackChannelName)
                .displayName(pDisplayName);
    }

    public void toggleDebugMode() {
        mDebugMode = !mDebugMode;
        sendToUser(mDebugUser, new SlackMessage("Bleep, bloop! Debug mode is " + (mDebugMode ? "on" : "off")));
    }


    public synchronized void sendToUser(String pUserId, SlackMessage pSlackMessage) {
        try {
            if(mDebugMode) {
                mSlack.sendToUser(mDebugUser).push(pSlackMessage);
            } else {
                mSlack.sendToUser(pUserId).push(pSlackMessage);
            }

        } catch(IOException e) {
            cLogger.error("Can't send message to slack-user" + pUserId + " message: " + pSlackMessage, e);
        }
    }

    public synchronized void send(SlackMessage pSlackMessage) {
        try {

            if(mDebugMode) {
                sendToUser(mDebugUser, pSlackMessage);
            } else {
                mSlack.sendToChannel(cSlackChannelName).push(pSlackMessage);
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
