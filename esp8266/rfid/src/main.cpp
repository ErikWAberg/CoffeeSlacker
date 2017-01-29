#include "Arduino.h"
#include "storage.h"
#include "rfid.h"
#include "mbedtls/sha512.h"

void setup() {
  Serial.begin(115200);
  Serial.println("[*] Serial Ready !");
  rfid_setup();

  storage_info();
  storage_print();


/*
  struct storage_entry entry = {0};

  for(int i = 0; i < 25; i++) {
    for(int j = 0; j < 255; j++) {
        entry.sha512[i * 255 + j]++;
        struct storage_result insert = {0};
        storage_insert(&insert);
    }
  storage_info();
  }*/
}

static unsigned long prev_recv_millis;
static const unsigned long time_out_ms = 5000;  // time to wait until next read
static rfid_key rfid_key = {0};

bool got_rfid_key() {
  if(rfid_available()) {

    if((millis() - prev_recv_millis) < time_out_ms) {
      rfid_discard();
      return false;
    }

    bool ok = rfid_read(&rfid_key);
    if (!ok) {
      Serial.println("[*] Bad checksum!");
      //TODO notify user somehow.. buzzbuzzw
      return false;
    }

    rfid_print(&rfid_key);
    prev_recv_millis = millis();
    return true;
  }
  return false;
}


void loop() {

  if(got_rfid_key()) {
    struct storage_entry entry = {0};
    mbedtls_sha512(&rfid_key.id[0], sizeof(rfid_key.id), entry.sha512, 0);
    memset(&rfid_key, 0, sizeof(struct rfid_key));

    Serial.println("SHA512 of key id:");
    storage_print_entry(&entry);
    struct storage_result insert = {0};
    insert.entry = &entry;
    bool ok = storage_insert(&insert);
    if(ok) {
      Serial.println("Lets go");
    }
  }


}
