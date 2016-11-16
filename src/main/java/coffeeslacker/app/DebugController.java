package coffeeslacker.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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


    @RequestMapping(value = "/blyat", method = RequestMethod.GET)
    public void deleteEverything() {
        mCoffeeSlacker.deleteEverything();
    }
}
