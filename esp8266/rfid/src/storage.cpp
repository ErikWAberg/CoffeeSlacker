#include "Arduino.h"
#include "storage.h"
#include "FS.h"

static bool mounted = false;

bool storage_mount() {
  if (!mounted) {
    mounted = SPIFFS.begin();
  }
  return mounted;
}

void storage_unmount() {
  if(mounted) {
    SPIFFS.end();
  }
  mounted = false;
}

bool entry_equals(struct storage_entry* e1, struct storage_entry* e2) {

  if (sizeof(e1->sha512) != sizeof(e2->sha512)){
    return false;
  }

  for (size_t i = 0; i < sizeof(e1->sha512); i++) {
    if(e1->sha512[i] != e2->sha512[i]) {
      return false;
    }
  }

  return true;
}

void storage_print_entry(struct storage_entry* e) {
  for (size_t i = 0; i < sizeof(e->sha512); i++) {
    Serial.printf("0x%02X", e->sha512[i]);

    if(i == sizeof(e->sha512) - 1 || (i+1) % 8 == 0) {
      Serial.println("");
    } else {
      Serial.print(" ");
    }
  }
}

bool storage_search(struct storage_result* search) {

  storage_mount();

  if (!SPIFFS.exists("/keys")) {
    Serial.println("[S] Could not find file '/keys'");
    search->error_code = STORAGE_NE;
    return false;
  }

  File file = SPIFFS.open("/keys", "r");

  if (!file) {
    Serial.println("[S] Could not open file '/keys'");
    search->error_code = STORAGE_IF;
    return false;
  }

  size_t sizeByEntry = file.size() % sizeof(search->entry->sha512);
  if (sizeByEntry != 0) {
    Serial.printf("[S] Each entry should be of size %u, but file size: %u -> size % entry: %u\n",
              sizeof(search->entry->sha512), file.size(), sizeByEntry);
    //TODO not sure if this could happen.
  }

  uint32_t entry_offset = 0;
  size_t file_offset = 0;

  while (file.available()) {
    struct storage_entry read_entry = {0};

    size_t bytes_read = file.read(&read_entry.sha512[0], sizeof(read_entry.sha512));

    if(bytes_read != sizeof(read_entry.sha512)) {
      file.close();
      Serial.printf("[S] Bytes read: %u != %u, when reading entry: %u at file-offset: %u, quitting\n",
                    bytes_read, sizeof(read_entry.sha512), entry_offset, file_offset);

      //TODO tell somebody
      search->error_code = STORAGE_EOE;
      return false;
    }

    if (entry_equals(&read_entry, search->entry)) {
      file.close();
      search->file_offset = (uint32_t) file_offset;
      search->entry_offset = entry_offset;
      Serial.printf("[S] Found entry: %u at offset: %u\n", entry_offset, file_offset);
      return true;
    }

    file_offset = file.position();
    entry_offset++;
  }

  file.close();
  Serial.printf("[S] Entry not found!\n");
  search->error_code = STORAGE_NF;
  return false;
}

bool storage_insert(struct storage_result* insert) {

  storage_mount();
  if(storage_search(insert)) {
    Serial.println("[S] Entry already exists!");
    return true;
  }

  if (!SPIFFS.exists("/keys")) {
    Serial.println("[S] Could not find file '/keys'");
    insert->error_code = STORAGE_NE;
    return false;
  }

  File file = SPIFFS.open("/keys", "a");

  if (!file) {
    Serial.println("[S] Could not open file '/keys'");
    insert->error_code = STORAGE_IF;
    return false;
  }

  size_t bytes_written = file.write(&(insert->entry->sha512[0]), sizeof(insert->entry->sha512));

  if(bytes_written != sizeof(insert->entry->sha512)) {
    Serial.printf("[S] Wrote %u bytes out of %u, when writing at file offset: %u, entry offset: %u\n",
                  bytes_written, sizeof(insert->entry->sha512), insert->file_offset, insert->entry_offset);
    file.close();
    insert->error_code = STORAGE_WE;
    return false;
  }
  insert->file_offset = file.position() - bytes_written;
  insert->entry_offset = insert->file_offset / sizeof(insert->entry->sha512);

  Serial.printf("[S] Insert entry: %u of %u bytes at %u\n", insert->entry_offset, sizeof(insert->entry->sha512), insert->file_offset);
  file.flush();
  file.close();
  return true;
}

void storage_print(void) {
  storage_mount();

  Serial.println("[S] Printing storage:");
  if (!SPIFFS.exists("/keys")) {
    Serial.println("[S] Could not find file '/keys'");
    return;
  }

  File file = SPIFFS.open("/keys", "r");

  if (!file) {
    Serial.println("[S] Could not open file '/keys'");
    return;
  }
  int size = file.size();
  int entries = size / sizeof(struct storage_entry);

  Serial.printf("[S] Size: %d, entries: %d\n", size, entries);

  uint32_t entry_offset = 0;
  size_t file_offset = 0;

  while (file.available()) {
    struct storage_entry read_entry = {0};

    size_t bytes_read = file.read(&read_entry.sha512[0], sizeof(read_entry.sha512));

    if(bytes_read != sizeof(read_entry.sha512)) {
      file.close();
      Serial.printf("[S] Bytes read: %u != %u, when reading index %u, quitting\n",
                    bytes_read, sizeof(read_entry.sha512), entry_offset);
      return;
    }
    Serial.printf("[S] Entry: %u with size: %u at offset %u: \n", entry_offset, sizeof(read_entry.sha512), file_offset);
    storage_print_entry(&read_entry);
    file_offset = file.position();
    entry_offset++;
  }
  file.close();

}

void storage_info() {
  uint32_t realSize = ESP.getFlashChipRealSize();
  uint32_t ideSize = ESP.getFlashChipSize();
  FlashMode_t ideMode = ESP.getFlashChipMode();

  Serial.printf("Flash real id:   %08X\n", ESP.getFlashChipId());
  Serial.printf("Flash real size: %u\n\n", realSize);

  Serial.printf("Flash ide  size: %u\n", ideSize);
  Serial.printf("Flash ide speed: %u\n", ESP.getFlashChipSpeed());
  Serial.printf("Flash ide mode:  %s\n", (ideMode == FM_QIO ? "QIO" : ideMode == FM_QOUT ? "QOUT" : ideMode == FM_DIO ? "DIO" : ideMode == FM_DOUT ? "DOUT" : "UNKNOWN"));

  if(ideSize != realSize) {
      Serial.println("Flash Chip configuration wrong!\n");
  } else {
      Serial.println("Flash Chip configuration ok.\n");
  }

  FSInfo fs_info;
  SPIFFS.info(fs_info);
  Serial.printf("Total bytes: %d\n", fs_info.totalBytes);
  Serial.printf("Used bytes: %d\n", fs_info.usedBytes);
  Serial.printf("Block size: %d\n", fs_info.blockSize);
  Serial.printf("Page size: %d\n", fs_info.pageSize);
}
