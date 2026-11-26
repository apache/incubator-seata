// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
import test from "node:test";
import assert from "node:assert/strict";
import { chmodSync, closeSync, mkdirSync, mkdtempSync, renameSync, rmSync, symlinkSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { PrivateDirectory } from "../dist/platform/linux.js";

test("Darwin private directories reject links and replacement while OS locks retain ownership",
  { skip: process.platform !== "darwin" }, () => {
    const root = mkdtempSync(join(tmpdir(), "seata-darwin-path-"));
    const path = join(root, "含空格 state");
    const first = new PrivateDirectory(path);
    const second = new PrivateDirectory(path, false);
    const lock = first.lock("owner.lock");
    try {
      assert.throws(() => second.lock("owner.lock"), error => error.subtype === "store_busy");
      const linked = join(root, "linked");
      symlinkSync(path, linked);
      assert.throws(() => new PrivateDirectory(linked, false), error => error.subtype === "unsafe_path");
      chmodSync(path, 0o755);
      assert.throws(() => first.assertCurrent(), error => error.subtype === "store_unsafe");
      chmodSync(path, 0o700);
      renameSync(path, path + ".moved");
      mkdirSync(path, { mode: 0o700 });
      assert.throws(() => first.assertCurrent(), error => error.subtype === "store_unsafe");
    } finally { closeSync(lock); first.close(); second.close(); rmSync(root, { recursive: true, force: true }); }
  });
