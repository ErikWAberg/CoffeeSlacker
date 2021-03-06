package coffeeslacker.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/debug")
public class DebugController {

    private CoffeeSlacker mCoffeeSlacker;

    @Autowired
    public DebugController(CoffeeSlacker pCoffeeSlacker) {
        mCoffeeSlacker = pCoffeeSlacker;
    }

    @RequestMapping(value = "/toggle", method = RequestMethod.GET)
    public void toggleDebug() {
        mCoffeeSlacker.toggleDebug();
    }

    @RequestMapping(value = "/editBrewer", method = RequestMethod.POST)
    public void editBrewer(@RequestParam("slackUser") String slackUser, @RequestParam("wins") int wins, @RequestParam("brews") int brews, @RequestParam("monthly") int monthly) {
        mCoffeeSlacker.editBrewer(slackUser, wins, brews, monthly);
    }


    @RequestMapping(value = "/blyat", method = RequestMethod.POST)
    public void deleteEverything(@RequestParam("asd") String asd) {
        mCoffeeSlacker.blyat(asd);
    }


}
