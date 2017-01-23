#include "Arduino.h"
#include <SoftwareSerial.h>

SoftwareSerial RFID(GPIO_ID_PIN(4), GPIO_ID_PIN(5)); // RX and TX, respectively

static const char CSTX = 2;
static const char CETX = 3;

static const uint8_t BUFFSIZE = 6;
static const uint8_t IDX_VERSION = 0;
static const uint8_t IDX_CHECKSUM = 5;

uint8_t byteCounter = 0;
uint8_t key_data[BUFFSIZE] = {0};
char number[2];

typedef struct {
  uint8_t  version;
  uint8_t  id[4];
  uint8_t  checksum;
  uint32_t id32;
} key;

void setup() {
  Serial.begin(9600);
  Serial.println("[*] Serial Ready !");
  RFID.begin(9600);
  Serial.println("[*] RFID Ready !");
}

bool checksumOk() {
  long int checksum = 0;
  for( int i = 0; i < IDX_CHECKSUM; i++) {
    checksum = checksum xor key_data[i];
  }
  return checksum == key_data[IDX_CHECKSUM];
}

void printKey(key *key) {
  Serial.printf("[*] Key version: 0x%02X\n", key->version);
  Serial.printf("[*] Key ID: ");
  for (int i = 0; i < 4; i++) {
    Serial.printf("0x%02X ", key->id[i]);
  }
  Serial.printf(" | ");
  for (int i = 0; i < 4; i++) {
    Serial.printf("%u ", key->id[i]);
  }
  Serial.printf(" | %u\n", key->id32);
  Serial.printf("[*] Key checksum: 0x%02X\n\n", key->checksum);
}

void processData(key* key) {
  if(!checksumOk()) {
    Serial.println("[*] Bad checksum!");
  }

  memcpy(&key->version, &key_data[0], sizeof(long int) * 1);
  memcpy(&key->id[0], &key_data[1], sizeof(long int) * 4);
  memcpy(&key->checksum, &key_data[IDX_CHECKSUM], sizeof(long int) * 1);
  key->id32 = (((uint32_t) key->id[0]) << 24 |
               ((uint32_t) key->id[1]) << 16 |
               ((uint32_t) key->id[2]) <<  8 |
               ((uint32_t) key->id[3]) <<  0);
}


void loop() {

  if (RFID.available()) {

    char input = RFID.read();

    if (input == CSTX) {
      memset(&key_data[0], 0, BUFFSIZE * sizeof(long int));
      byteCounter = 0;

    } else if (input == CETX) {
      key key;
      processData(&key);
      printKey(&key);
      delay(200);

    } else {
      number[byteCounter % 2] = input;

      if(byteCounter % 2 != 0) {
        key_data[byteCounter / 2] = (uint8_t) strtol(number, NULL, 16);
        number[0] = number[1] = 0;
      }

      byteCounter++;

    }
  }
}
