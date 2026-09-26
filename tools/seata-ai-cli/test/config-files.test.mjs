// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, writeFileSync, symlinkSync, linkSync, statSync, readdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
import { ConfigFiles } from "../dist/adapter/config/files.js";
const signal = new AbortController().signal;

test("config source reads are bounded, read-only and share a total inspection budget", () => {
  const dir = mkdtempSync(join(tmpdir(), "seata-config-read-")), path = join(dir, "application.properties");
  writeFileSync(path, "key=value", { mode: 0o640 });
  const files = new ConfigFiles(), before = statSync(path);
  assert.equal(files.read(path, signal).bytes.toString(), "key=value");
  assert.deepEqual(files.usage, { files: 1, bytes: 9 });
  assert.equal(statSync(path).mode, before.mode);
  assert.equal(statSync(path).mtimeMs, before.mtimeMs);
  const large = join(dir, "large.properties"); writeFileSync(large, Buffer.alloc(1024 * 1024));
  const total = new ConfigFiles();
  for (let i = 0; i < 10; i++) assert.equal(total.read(large, signal).bytes.length, 1024 * 1024);
  assert.throws(() => total.read(path, signal), e => e.subtype === "config_limit_exceeded");
  writeFileSync(large, Buffer.alloc(1024 * 1024 + 1));
  assert.throws(() => files.read(large, signal), e => e.subtype === "config_limit_exceeded");
});

test("config reads reject ancestor/leaf symlinks, hardlinks, directories and FIFOs without blocking", () => {
  const dir = mkdtempSync(join(tmpdir(), "seata-config-links-")), path = join(dir, "private.properties");
  writeFileSync(path, "password=SECRET");
  symlinkSync(path, join(dir, "leaf")); symlinkSync(dir, join(dir, "ancestor"));
  const files = new ConfigFiles(), fds = readdirSync("/proc/self/fd").length;
  for (const unsafe of [join(dir, "leaf"), join(dir, "ancestor/private.properties"), dir]) {
    assert.throws(() => files.read(unsafe, signal), e => e.type === "config" && !e.message.includes("SECRET"));
  }
  linkSync(path, join(dir, "hardlink"));
  assert.throws(() => files.read(path, signal), e => e.subtype === "unsafe_config_path");
  const fifo = join(dir, "fifo");
  assert.equal(spawnSync("mkfifo", [fifo]).status, 0);
  assert.throws(() => files.read(fifo, signal), e => e.subtype === "unsafe_config_path");
  assert.equal(readdirSync("/proc/self/fd").length, fds);
});

test("config read file-count and cancellation gates also apply to empty files", () => {
  const dir = mkdtempSync(join(tmpdir(), "seata-config-budget-")), path = join(dir, "empty.properties");
  writeFileSync(path, "");
  const files = new ConfigFiles();
  for (let i = 0; i < 200; i++) files.read(path, signal);
  assert.throws(() => files.read(path, signal), e => e.subtype === "config_limit_exceeded");
  const abort = new AbortController(); abort.abort();
  assert.throws(() => new ConfigFiles().read(path, abort.signal), e => e.name === "AbortError");
});
