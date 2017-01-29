#include "Arduino.h"

struct storage_entry {
  uint8_t sha512[64];
};


// Storage error codes
#define STORAGE_NF -1  // Entry not found
#define STORAGE_EOF -2 // Premature EOF
#define STORAGE_IF -3  // Invalid file (file not found or other)
#define STORAGE_EOE -4 // Premature end of entry
#define STORAGE_NE -5  // File does not exist
#define STORAGE_WE -6  // Could not write all bytes of an entry
#define STORAGE_RE -7  // Could not read all bytes of an entry

struct storage_result {
  struct storage_entry* entry;  // Entry to find/insert
  int error_code;               // Error code, if applicable
  uint32_t file_offset;         // File-offset of entry 0, 64, 128, ...
  uint32_t entry_offset;        // Entry-offset of entry 0, 1, 2, ...
};


bool storage_mount();
void storage_unmount();
void storage_print();
bool storage_search(struct storage_result* search);
bool storage_insert(struct storage_result* insert);

void storage_info();
void storage_print_entry(struct storage_entry* e);
