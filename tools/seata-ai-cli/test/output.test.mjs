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
import { mkdtempSync, readFileSync, rmSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { execFile, spawnSync } from "node:child_process";
import { promisify } from "node:util";
import { fileURLToPath } from "node:url";
import { wireResult, write, serialize } from "../dist/output/emitter.js";
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
test("envelopes use the expected exit codes and output streams", () => {
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
  const text = serialize(wireResult(base));
  await write(sink, text);
  assert.equal(done, true);
  assert.equal(writes, 1);
  const broken = new Writable({
    write(_chunk, _encoding, callback) {
      callback(Object.assign(new Error("closed"), { code: "EPIPE" }));
    },
  });
  await assert.rejects(
    write(broken, text),
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
test("--file reads the request independently of stdin", async (t) => {
  const dir = mkdtempSync(join(tmpdir(), "seata-r11-wire-"));
  t.after(() => rmSync(dir, { recursive: true, force: true }));
  const file = join(dir, "invocation.json");
  writeFileSync(file, readFileSync(new URL("./contract/r11/examples/invocations/system.version.json", import.meta.url)));
  const { stdout } = await promisify(execFile)(process.execPath, [cli, "invoke", "--file", file], {
    timeout: 5000,
    killSignal: "SIGKILL",
  });
  assert.equal(JSON.parse(stdout).command, "system.version");
});
test("--file reports missing paths and rejects non-regular files without blocking", (t) => {
  const dir = mkdtempSync(join(tmpdir(), "seata-r11-wire-"));
  t.after(() => rmSync(dir, { recursive: true, force: true }));
  const link = join(dir, "link");
  const fifo = join(dir, "pipe");
  symlinkSync(dir, link);
  assert.equal(spawnSync("mkfifo", [fifo]).status, 0);
  for (const [path, status, subtype] of [
    [join(dir, "missing"), 2, "invalid_argument"],
    [dir, 6, "unsafe_path"],
    [link, 6, "unsafe_path"],
    [fifo, 6, "unsafe_path"],
  ]) {
    const child = spawnSync(process.execPath, [cli, "invoke", "--file", path], {
      encoding: "utf8",
      timeout: 5000,
      killSignal: "SIGKILL",
    });
    assert.ifError(child.error);
    assert.equal(child.status, status);
    assert.equal(child.stdout, "");
    assert.equal(JSON.parse(child.stderr).error.subtype, subtype);
  }
});
