package coffeeslacker.brewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/brewer")
public class BrewerController {

    private static final Logger cLogger = LoggerFactory.getLogger(BrewerController.class);

    private final BrewerService mBrewerService;

    @Autowired
    public BrewerController(BrewerService pBrewerService) {
        mBrewerService = pBrewerService;
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public Brewer registerBrewer(@RequestParam("slackUser") String pSlackUser,
                                 @RequestParam("rfid") String pRfid) {
        cLogger.info("Register brewer: " + " slackUser: " + pSlackUser + " rfid: " + pRfid);
        return mBrewerService.registerRfid(pSlackUser, pRfid);
    }


    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public List<Brewer> getAllBrewers()  {
        return mBrewerService.getBrewersSortedByBrewCountThisMonth();
    }

    @RequestMapping(value = "/find", method = RequestMethod.GET)
    public Brewer getBrewerByRfid(@RequestParam(name = "rfid") String pRfid)  {
        cLogger.info("getBrewerByRfid: rfid= " + pRfid + ": " + mBrewerService.getBrewerByRfid(pRfid));
        return mBrewerService.getBrewerByRfid(pRfid);
    }
}
