#include "Arduino.h"
#include <SoftwareSerial.h>
#include "rfid.h"
#include "storage.h"

#define ESP_RX 4
#define ESP_TX 5
SoftwareSerial RFID(GPIO_ID_PIN(ESP_RX), GPIO_ID_PIN(ESP_TX));

static const int RFID_STX = 2; // Start of rfid
static const int RFID_ETX = 3; // End of rfid

static const uint8_t BUFFSIZE = 6;
static const uint8_t IDX_VERSION = 0;
static const uint8_t IDX_CHECKSUM = 5;

static char number[2]; // We receive every byte as a pair of chars (in base 16)
static uint8_t byteCounter = 0;
static uint8_t key_data[BUFFSIZE] = {0}; // [VERSION, K1, K2, K3, K4, CHECKSUM]
static uint8_t checksum = 0;


void rfid_setup() {
  RFID.begin(9600);
  Serial.println("[R] RFID Ready !");
}

void rfid_extract(struct rfid_key* key) {
  memcpy(&key->version, &key_data[0], sizeof(uint8_t) * 1);
  memcpy(&key->id[0], &key_data[1], sizeof(uint8_t) * 4);
  memcpy(&key->checksum, &key_data[IDX_CHECKSUM], sizeof(uint8_t) * 1);
  key->id32 = (((uint32_t) key->id[0]) << 24 |
               ((uint32_t) key->id[1]) << 16 |
               ((uint32_t) key->id[2]) <<  8 |
               ((uint32_t) key->id[3]) <<  0);
}

bool rfid_equals(struct rfid_key* k1, struct rfid_key* k2) {
  return k1->id32 == k2->id32;
}

void rfid_discard() {
  while(RFID.read() > 0);
}

bool rfid_available() {
  while (RFID.available() > 0 && RFID.peek() != RFID_STX) {
      RFID.read();
  }
  return RFID.available()  >= 14;
}

bool rfid_read(struct rfid_key* key) {

  if (RFID.available() < 14 || RFID.peek() != RFID_STX) {
    return false;
  }

  memset(&key_data[0], 0, BUFFSIZE * sizeof(uint8_t));
  checksum = 0;
  byteCounter = 0;

  for(int i = 0; i < 2 * BUFFSIZE + 2; i++) {
      int input = RFID.read();

      switch (input) {
        case RFID_STX: break;
        case RFID_ETX:
        if (checksum != key_data[IDX_CHECKSUM]) {
          return false;
        }
        rfid_extract(key);
        return true;

        break;
        default:
          number[byteCounter % 2] = (char) input;
          if(byteCounter % 2 != 0) {
            uint8_t byte = (uint8_t) strtol(number, NULL, 16);
            uint8_t byte_index = byteCounter / 2;
            key_data[byte_index] = byte;
            if(byte_index != IDX_CHECKSUM) checksum ^= byte;
            number[0] = number[1] = 0;
          }
          byteCounter++;
          break;
      }
    }
  return false;
}

void rfid_print(struct rfid_key *key) {
  Serial.printf("[R] Key version: 0x%02X\n", key->version);
  Serial.printf("[R] Key ID: ");
  for (int i = 0; i < 4; i++) {
    Serial.printf("0x%02X ", key->id[i]);
  }
  Serial.printf(" | ");
  for (int i = 0; i < 4; i++) {
    Serial.printf("%u ", key->id[i]);
  }
  Serial.printf(" | %u\n", key->id32);
  Serial.printf("[R] Key checksum: 0x%02X\n\n", key->checksum);
}
