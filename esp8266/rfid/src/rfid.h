#include "Arduino.h"

struct rfid_key {
  uint8_t  version;
  uint8_t  id[4];
  uint8_t  checksum;
  uint32_t id32;
};

void rfid_setup();

bool rfid_available();

bool rfid_read(struct rfid_key *key);

bool rfid_equals(struct rfid_key *k1, struct rfid_key *k2);

void rfid_print(struct rfid_key *key);

void rfid_extract(struct rfid_key *key);

void rfid_discard();
