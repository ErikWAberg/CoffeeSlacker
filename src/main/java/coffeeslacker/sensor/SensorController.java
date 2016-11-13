package coffeeslacker.sensor;

import coffeeslacker.app.CoffeeSlacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/sensor")
public class SensorController {

    private static final Logger cLogger = LoggerFactory.getLogger(SensorController.class);

    private final CoffeeSlacker mCoffeeSlacker;
    private final SensorService mSensorService;

    @Autowired
    public SensorController(CoffeeSlacker pCoffeeSlacker, SensorService pSensorService) {
        mCoffeeSlacker = pCoffeeSlacker;
        mSensorService = pSensorService;
    }

    @RequestMapping(value = "/report", method = RequestMethod.POST)
    public void bySensorReport(@RequestParam("sensorValue") String pSensorValue, @RequestParam(name = "sensorId") int pSensorId) {
        //cLogger.info("Received sensorValue: " + pSensorValue + " sensorId: " + pSensorId);
        mCoffeeSlacker.onSensorReport(pSensorValue, pSensorId);
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public Sensor registerSensor(@RequestParam("sensorId") int pSensorId,
                                 @RequestParam("lowerThreshold") double pLowerThreshold,
                                 @RequestParam("upperThreshold") double pUpperThreshold,
                                 @RequestParam("sensorType") String pSensorType,
                                 @RequestParam("location") String pLocation) {
        cLogger.info("Register sensor: " + "sensorId: " +  pSensorId + " threshold: " + pLowerThreshold + " sensorType: " + pSensorType + " location: " + pLocation);
        return mSensorService.registerSensor(pSensorId, pLowerThreshold, pUpperThreshold, pSensorType, pLocation);
    }



    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public List<Sensor> getAllSensors()  {
        return mSensorService.getAllSensors();
    }

    @RequestMapping(value = "/find", method = RequestMethod.GET)
    public Sensor getSensorById(@RequestParam("sensorId") int pSensorId)  {
        cLogger.info("getSensorByName: sensorid= " + pSensorId + ": " + mSensorService.getSensorById(pSensorId));
        return mSensorService.getSensorById(pSensorId);
    }

}
