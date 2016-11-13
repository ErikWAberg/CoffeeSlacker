package coffeeslacker.sensor;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SensorRepository extends MongoRepository<Sensor, String> {

    public Sensor findBySensorId(int sensorId);
    public Sensor findByLocation(String location);

}
