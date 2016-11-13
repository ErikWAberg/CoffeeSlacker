#include <ESP8266HTTPClient.h>
#include <WiFiClientSecure.h>

#define HTTP_HOST "secret"
#define HTTP_PORT 8082

#define HTTP_ENDPOINT "/api/sensor/report"

const int SENSOR_ID = 11;

String id = String("&sensorId=") + SENSOR_ID;

int http_post(int lux) {

  HTTPClient client;

  client.begin(HTTP_HOST, HTTP_PORT, HTTP_ENDPOINT);
  client.addHeader("Content-Type", "application/x-www-form-urlencoded");
  String payload = String("sensorValue=") + lux + id;
  //Serial.println(payload);
  int httpCode = client.POST(payload);
  client.end();

  return httpCode == 200;

}
