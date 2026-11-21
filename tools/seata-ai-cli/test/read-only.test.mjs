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
import { readFileSync, mkdtempSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
import { registry, validate, outputSchemas } from "../dist/command/registry.js";
import { deferredWrite } from "../dist/command/scope.js";
import { dispatch, implemented } from "../dist/app/dispatch.js";
import { renderHelp } from "../dist/help/render.js";

test("all 48 in-scope commands have implementation declarations and non-placeholder result schemas", () => {
  const enabled = registry.filter(spec => !deferredWrite(spec.id));
  assert.equal(enabled.length, 48);
  assert.deepEqual([...implemented].sort(), enabled.map(spec => spec.id).sort());
  for (const spec of enabled) {
    // invoke emits the delegated command's schema, not a recursive wrapper DTO.
    if (spec.id === "system.invoke") continue;
    assert.ok(outputSchemas[spec.id], spec.id);
    assert.equal(Object.hasOwn(outputSchemas[spec.id], "not"), false, spec.id);
    assert.equal(outputSchemas[spec.id].type, "object", spec.id);
    assert.ok(outputSchemas[spec.id].required.length > 0, spec.id);
  }
});

test("all nine server mutations are denied before profile or storage access, including dry-run", async () => {
  const writes = registry.filter(s => deferredWrite(s.id));
  assert.equal(writes.length, 9);
  assert.deepEqual(writes.map(s => s.id).sort(), registry.filter(s =>
    ["controlled_write", "high_risk_write"].includes(s.risk)).map(s => s.id).sort());
  for (const spec of writes) {
    const example = JSON.parse(readFileSync(new URL(
      `./contract/r11/examples/invocations/${spec.id}.json`, import.meta.url)));
    for (const dry_run of [true, false]) {
      example.options.dry_run = dry_run;
      await assert.rejects(dispatch(validate(example), new AbortController().signal),
        error => error.type === "policy" && error.subtype === "command_unavailable");
    }
    assert.match(renderHelp(spec.path), /deferred_read_only_scope/);
  }
  assert.equal(deferredWrite("profile.create"), false);
  assert.equal(deferredWrite("auth.logout"), false);
});

test("public invoke rejects writes without creating a state directory", () => {
  const directory = join(mkdtempSync(join(tmpdir(), "seata-read-only-")), "state");
  const example = readFileSync(new URL(
    "./contract/r11/examples/invocations/transaction.delete.json", import.meta.url));
  const result = spawnSync(process.execPath, [new URL("../dist/cli.js", import.meta.url).pathname, "invoke"], {
    input: example, encoding: "utf8", env: { ...process.env, SEATA_AI_STATE_DIR: directory }, timeout: 10000,
  });
  assert.equal(result.status, 6);
  assert.equal(result.stdout, "");
  assert.equal(JSON.parse(result.stderr).error.subtype, "command_unavailable");
  assert.equal(existsSync(directory), false);
});
