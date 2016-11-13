package coffeeslacker.sensor;

public enum SensorType {
    LIGHT,
    RFID,
    POWER;

    public  static SensorType getSensorType(String pSensorType) {
        try {
            SensorType tSensorType = SensorType.valueOf(pSensorType);
            return tSensorType;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static SensorType getSensorType(Sensor pSensor) {
        try {
            SensorType tSensorType = SensorType.valueOf(pSensor.getSensorType());
            return  tSensorType;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
