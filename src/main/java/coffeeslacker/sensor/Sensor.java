package coffeeslacker.sensor;

import org.springframework.data.annotation.Id;

public class Sensor {

    @Id
    public String id;

    private int sensorId;
    private double lowerThreshold;
    private double upperThreshold;
    private String sensorType;
    private String location;

    public Sensor(int sensorId, double lowerThreshold, double upperThreshold, String sensorType, String location) {
        this.sensorId = sensorId;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
        this.sensorType = sensorType;
        this.location = location;
    }

    public int getSensorId() {
        return sensorId;
    }

    public String getLocation() {
        return location;
    }

    public double getLowerThreshold() {
        return lowerThreshold;
    }

    public double getUpperThreshold() {
        return upperThreshold;
    }

    public String getSensorType() {
        return sensorType;
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "sensorId=" + sensorId +
                ", threshold=" + lowerThreshold +
                ", upperThreshold=" + upperThreshold +
                ", sensorType='" + sensorType + '\'' +
                ", location='" + location + '\'' +
                '}';
    }

    public void update(final double pLowerThreshold, final double pUpperThreshold, final String pSensorType, final String pLocation) {
        lowerThreshold = pLowerThreshold;
        upperThreshold = pUpperThreshold;
        sensorType = pSensorType;
        location = pLocation;
    }
}
