/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <Security/Security.h>
#include <sys/file.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

static const char service[] = "seata-ai-cli";

int main(int argc, char **argv) {
  if (argc == 3 && strcmp(argv[1], "lock") == 0) {
    int mode = strcmp(argv[2], "shared") == 0 ? LOCK_SH :
      strcmp(argv[2], "exclusive") == 0 ? LOCK_EX : 0;
    return mode && flock(3, mode | LOCK_NB) == 0 ? 0 : 1;
  }
  if (argc != 4 || (strcmp(argv[1], "put") && strcmp(argv[1], "get") && strcmp(argv[1], "delete"))) return 2;
  if (strlen(argv[2]) != 36 || strlen(argv[3]) != 36) return 2;
  char account[74];
  snprintf(account, sizeof(account), "%s:%s", argv[2], argv[3]);
  SecKeychainRef keychain = NULL;
  const char *path = getenv("SEATA_AI_TEST_KEYCHAIN_PATH");
  if (path && SecKeychainOpen(path, &keychain) != errSecSuccess) return 3;
  if (strcmp(argv[1], "put") == 0) {
    char secret[16385];
    size_t size = 0;
    while (size < sizeof(secret)) {
      ssize_t n = read(STDIN_FILENO, secret + size, sizeof(secret) - size);
      if (n < 0) { if (keychain) CFRelease(keychain); return 3; }
      if (n == 0) break;
      size += (size_t)n;
    }
    if (size == 0 || size > 16384) { if (keychain) CFRelease(keychain); return 3; }
    OSStatus status = SecKeychainAddGenericPassword(keychain, sizeof(service) - 1, service,
      (UInt32)strlen(account), account, (UInt32)size, secret, NULL);
    volatile char *wipe = secret;
    for (size_t i = 0; i < sizeof(secret); i++) wipe[i] = 0;
    if (keychain) CFRelease(keychain);
    return status == errSecSuccess ? 0 : 3;
  }
  void *secret = NULL;
  UInt32 size = 0;
  SecKeychainItemRef item = NULL;
  OSStatus status = SecKeychainFindGenericPassword(keychain, sizeof(service) - 1, service,
    (UInt32)strlen(account), account, strcmp(argv[1], "get") == 0 ? &size : NULL,
    strcmp(argv[1], "get") == 0 ? &secret : NULL, &item);
  if (status == errSecItemNotFound && strcmp(argv[1], "delete") == 0) {
    if (keychain) CFRelease(keychain);
    return 0;
  }
  if (status != errSecSuccess) { if (keychain) CFRelease(keychain); return 3; }
  if (strcmp(argv[1], "get") == 0) {
    if (size == 0 || size > 16384) status = errSecParam;
    else {
      size_t written = 0;
      while (written < size) {
        ssize_t n = write(STDOUT_FILENO, (char *)secret + written, size - written);
        if (n <= 0) { status = errSecIO; break; }
        written += (size_t)n;
      }
    }
    SecKeychainItemFreeContent(NULL, secret);
  } else status = SecKeychainItemDelete(item);
  CFRelease(item);
  if (keychain) CFRelease(keychain);
  return status == errSecSuccess ? 0 : 3;
}
