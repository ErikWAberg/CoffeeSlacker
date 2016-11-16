package coffeeslacker.slack;

import coffeeslacker.app.CoffeeSlacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(value = "/api/slack")
public class SlackController {

    private static final Logger cLogger = LoggerFactory.getLogger(SlackController.class);

    private final CoffeeSlacker mCoffeeSlacker;

    @Autowired
    public SlackController(CoffeeSlacker pCoffeeSlacker) {
        mCoffeeSlacker = pCoffeeSlacker;
    }

    @RequestMapping(value = "/notification", method = RequestMethod.POST, produces = "application/json")
    public SlackResponse slackNotification(@RequestParam(name="team_id", required = false) String team_id,
                             @RequestParam(name="token", required = false) String token,
                             @RequestParam(name="team_domain", required = false) String team_domain,
                             @RequestParam(name="channel_id", required = false) String channel_id,
                             @RequestParam(name="channel_name", required = false) String channel_name,
                             @RequestParam(name="timestamp", required = false) String timestamp,
                             @RequestParam(name="user_id", required = false) String user_id,
                             @RequestParam(name="user_name", required = false) String user_name,
                             @RequestParam(name="text", required = false) String text,
                             @RequestParam(name="trigger_word", required = false) String trigger_word
                            ) throws ExecutionException, InterruptedException {

        //SlackNotification pNotification = new SlackNotification(team_id, token, team_domain, channel_id, channel_name, timestamp, user_id, user_name, text, trigger_word);

        cLogger.info("Slack sniffer: " + user_name + ", " + text);
        String tResponse = "";

        switch (text) {
            case "!claim":
                mCoffeeSlacker.onClaimRequest(user_name);
                break;
            case "!stats":
                tResponse = mCoffeeSlacker.onStatsRequest(user_name);
                break;
            case "!bounty":
                tResponse = mCoffeeSlacker.onBountyRequest(user_name);
                break;
            case "!help":
                tResponse = "```!claim to claim a brew.\n!stats to view statistics & top 15 brewers.\n!bounty to give one of your points to" +
                        "the person that claims a brew within the next 15 minutes.```";
                break;
        }

        return new SlackResponse(tResponse);

    }


}
