package coffeeslacker.sensor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SensorService {

    private final SensorRepository mSensorRepository;

    @Autowired
    public SensorService(SensorRepository pSensorRepository) {
        mSensorRepository = pSensorRepository;
    }


    @PostConstruct
    public void postConstruct() {

    }

    public void deleteEverything() {
        mSensorRepository.deleteAll();
        registerSensor(11, 10.0, 70.0, "LIGHT", "Köket");
        registerSensor(12, 0.0, 0.0, "RFID", "Köket");
    }


    public Sensor registerSensor(int pSensorId, double pLowerThreshold, double pUpperThreshold, String pSensorType, String pLocation) {
        SensorType tSensorType = SensorType.getSensorType(pSensorType);
        if(tSensorType == null) {
            return null; //TODO
        }

        Sensor tSensor = mSensorRepository.findBySensorId(pSensorId);

        if(tSensor == null) {
            tSensor = new Sensor(pSensorId, pLowerThreshold, pUpperThreshold, pSensorType, pLocation);
        } else {
            tSensor.update(pLowerThreshold, pUpperThreshold, pSensorType, pLocation);
        }

        return mSensorRepository.save(tSensor);
    }

    public List<Sensor> getAllSensors() {
        return mSensorRepository.findAll();
    }

    public Sensor getSensorById(final int pSensorId) {
        return mSensorRepository.findBySensorId(pSensorId);
    }
}
