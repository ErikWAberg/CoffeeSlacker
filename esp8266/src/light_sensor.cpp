#include "light_sensor.h"
#include "TSL2561.h"
#include <stdint.h>


TSL2561 light_sensor(TSL2561_ADDR_FLOAT);

uint16_t SENSOR_GAIN = TSL2561_GAIN_0X;

int light_sensor_init(void)
{
  if (light_sensor.begin()) {
    light_sensor.setGain(TSL2561_GAIN_0X);       // set no gain (for bright situtations)
    //light_sensor.setGain(TSL2561_GAIN_16X);      // set 16x gain (for dim situations)
    light_sensor.setTiming(TSL2561_INTEGRATIONTIME_402MS);  // longest integration time (dim light)
    light_sensor.enable();
  } else {
    while (1);
  }
  return 1;
}


int light_sensor_read(void) {

  uint32_t lum = light_sensor.getFullLuminosity();
  // (visible + infrared)
  uint16_t full = (lum & 0xFFFF);

  // (infrared)
  uint16_t ir = (lum >> 16);

  uint32_t lux = light_sensor.calculateLux(full, ir);
  uint16_t lux_low = (uint16_t)(lux & 0xFFFF);

  return (int)lux_low;
}
