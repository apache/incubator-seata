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
import { Writable } from "node:stream";
import { readFileSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { wireResult, emit, serialize } from "../dist/output/emitter.js";
const cli = fileURLToPath(new URL("../dist/cli.js", import.meta.url));
const base = {
  protocol_version: "0.1",
  contract_revision: "R9",
  ok: true,
  state: "completed",
  command: "system.version",
  request_id: "test",
  timestamp: new Date().toISOString(),
  context: {},
  meta: { duration_ms: 0, partial: false },
  warnings: [],
  data: {},
};
const error = (type) => ({
  type,
  subtype: "test",
  message: "test",
  retry: "never",
  details: {},
});
test("independent envelopes and complete failed error-to-exit mapping", () => {
  for (const [type, code] of Object.entries({
    api: 1,
    validation: 2,
    authentication: 3,
    authorization: 3,
    config: 3,
    network: 4,
    internal: 5,
    policy: 6,
    not_found: 7,
    conflict: 8,
    cancelled: 11,
  })) {
    const r = wireResult({
      ...base,
      ok: false,
      state: "failed",
      error: error(type),
    });
    assert.equal(r.exit, code);
    assert.equal(r.stream, "stderr");
  }
  for (const [name, exit, stream] of [
    ["confirmation", 10, "stderr"],
    ["indeterminate", 12, "stdout"],
  ]) {
    const fixture = JSON.parse(
      readFileSync(
        new URL(
          "./contract/r11/examples/envelopes/" + name + ".json",
          import.meta.url,
        ),
      ),
    );
    const r = wireResult(fixture);
    assert.equal(r.exit, exit);
    assert.equal(r.stream, stream);
  }
  const partial = wireResult({
    ...base,
    ok: false,
    state: "partial",
    meta: { duration_ms: 0, partial: true },
    error: error("partial_failure"),
  });
  assert.equal(partial.exit, 9);
  assert.equal(partial.stream, "stdout");
  for (const state of [
    "completed",
    "pending",
    "partial",
    "failed",
    "indeterminate",
  ])
    assert.throws(() =>
      wireResult({
        ...base,
        state,
        ok:
          state === "failed" ||
          state === "partial" ||
          state === "indeterminate",
      }),
    );
  assert.throws(() =>
    wireResult({
      ...base,
      state: "failed",
      ok: false,
      error: error("indeterminate"),
    }),
  );
});
test("backpressure waits for write completion; EPIPE rejects without another emission", async () => {
  let done = false,
    writes = 0;
  const sink = new Writable({
    highWaterMark: 1,
    write(_chunk, _encoding, callback) {
      writes++;
      setTimeout(() => {
        done = true;
        callback();
      }, 20);
    },
  });
  await emit(wireResult(base), sink, sink);
  assert.equal(done, true);
  assert.equal(writes, 1);
  const broken = new Writable({
    write(_chunk, _encoding, callback) {
      callback(Object.assign(new Error("closed"), { code: "EPIPE" }));
    },
  });
  await assert.rejects(
    emit(wireResult(base), broken, sink),
    (e) => e.code === "EPIPE",
  );
  assert.equal(writes, 1);
  assert.throws(
    () =>
      serialize(
        wireResult({ ...base, data: { text: "x".repeat(16 * 1024 * 1024) } }),
      ),
    (e) => e.subtype === "output_limit",
  );
});
test("--file plus piped stdin rejects, unsafe store is not advertised as available", () => {
  const dir = mkdtempSync(join(tmpdir(), "seata-r11-wire-"));
  const file = join(dir, "invocation.json");
  const invocation = JSON.stringify({
    protocol_version: "0.1",
    contract_revision: "R9",
    command: "system.version",
    context: {},
    input: {},
    options: {},
  });
  writeFileSync(file, invocation);
  const r = spawnSync(process.execPath, [cli, "invoke", "--file", file], {
    input: invocation,
    encoding: "utf8",
    timeout: 10000,
  });
  assert.equal(r.status, 2);
  assert.equal(r.stdout, "");
  assert.equal(JSON.parse(r.stderr).error.subtype, "invalid_argument");
  const cap = spawnSync(
    process.execPath,
    [cli, "capabilities", "profile.list"],
    {
      encoding: "utf8",
      env: { ...process.env, SEATA_AI_STATE_DIR: "/" },
      timeout: 10000,
    },
  );
  assert.equal(cap.status, 0, cap.stderr);
  assert.equal(JSON.parse(cap.stdout).data.commands[0].status, "unavailable");
});
