#include <ESP8266WiFi.h>
#include "light_sensor.h"
#include "http.h"

#define LED_RED 0 //RED
#define LED_BLUE 2 //BLUE

#define SSID_NAME "secret"
#define SSID_PASS "secret"


void alert_on(void) {
  digitalWrite(LED_RED, LOW);
}

void alert_off(void) {
  digitalWrite(LED_RED, HIGH);
}

void wifi_init() {

  WiFi.begin(SSID_NAME, SSID_PASS);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.print("\nWiFi connected: ");
  Serial.println(WiFi.localIP());

}


void setup() {
  pinMode(LED_RED, OUTPUT);
  pinMode(LED_BLUE, OUTPUT);

  digitalWrite(LED_BLUE, HIGH);

  alert_on();
  Serial.begin(9600);
  delay(100);
  wifi_init();
  //init_rfid();
  light_sensor_init();
  alert_off();

}


void alert(bool alert) {
  if(alert) {
    alert_on();
  } else {
    alert_off();
  }
}

void loop() {

  delay(1000);

  if (WiFi.status() == WL_CONNECTED) {
    int lux = light_sensor_read();
    digitalWrite(LED_BLUE, LOW);
    alert(http_post(lux));
    digitalWrite(LED_BLUE, HIGH);
  } else {
    alert_on();
    wifi_init();
    alert_off();
  }

}
