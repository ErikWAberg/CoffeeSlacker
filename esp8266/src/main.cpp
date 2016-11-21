#include <ESP8266WiFi.h>
#include "light_sensor.h"
#include "http.h"
#include "secret.h"

#define LED_RED 0 //RED
#define LED_BLUE 2 //BLUE




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

bool waitForHigh = true;
int THRESH = 50;

void loop() {

  delay(2000);

  if (WiFi.status() == WL_CONNECTED) {
    digitalWrite(LED_BLUE, LOW);
    int lux = light_sensor_read();
    Serial.println(lux);
    if(http_post(lux) == 200) {
      alert_off();
    } else {
      alert_on();
    }
    digitalWrite(LED_BLUE, HIGH);
    /*
    if(waitForHigh) {
      if(lux > THRESH) {
        digitalWrite(LED_BLUE, LOW);
        if(http_post(lux) == 200) {
          waitForHigh = false;
          alert_off();
        } else {
          alert_on();
        }
        digitalWrite(LED_BLUE, HIGH);
      }
    } else {
      if(lux < THRESH) {
        digitalWrite(LED_BLUE, LOW);
        if(http_post(lux) == 200) {
          waitForHigh = true;
          alert_off();
        } else {
          alert_on();
        }
        digitalWrite(LED_BLUE, HIGH);
      }
    }
*/
  } else {
    alert_on();
    wifi_init();
    alert_off();
  }

}
