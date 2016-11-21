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
                             @RequestParam(name="user_name") String user_name,
                             @RequestParam(name="text") String text,
                             @RequestParam(name="trigger_word", required = false) String trigger_word
                            ) throws ExecutionException, InterruptedException {


        cLogger.info("Slack sniffer: " + user_name + ", " + text);
        String tResponse = "";

        switch (text.toLowerCase()) {
            case "!claim":
                tResponse = mCoffeeSlacker.onClaimRequest(user_name);
                break;
            case "!stats":
                tResponse = mCoffeeSlacker.onStatsRequest(user_name);
                break;
            case "!bounty":
                tResponse = mCoffeeSlacker.onBountyRequest(user_name);
                break;
            case "!vaska":
                break;
            case "!help":
                tResponse = "```" +
                        "!claim a brew within 20minutes of an initialized brew to get points.\n" +
                        "!stats to view statistics & top 15 brewers.\n" +
                        "!bounty to give one of your points to the person that claims a brew within the next 15 minutes.\n" +
                        "!vaska -- not implemented yet --```";
                break;
        }

        return new SlackResponse(tResponse);

    }


}
