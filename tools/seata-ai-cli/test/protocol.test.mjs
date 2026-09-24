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
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import {
  registry,
  validate,
  ajv,
  validateEnvelope,
  validateOutput,
  outputSchemas,
} from "../dist/command/registry.js";
import { parseArgs } from "../dist/cmdtree/parse.js";
import { parseJson, canonical } from "../dist/protocol/value.js";
import { renderHelp } from "../dist/help/render.js";
const root = fileURLToPath(new URL("../", import.meta.url));
const json = (path) => JSON.parse(readFileSync(resolve(root, path), "utf8"));
const parse = (text) => parseJson(Buffer.from(text));
const run = (args, input) =>
  spawnSync(process.execPath, [resolve(root, "dist/cli.js"), ...args], {
    input,
    encoding: "utf8",
    cwd: "/tmp",
    timeout: 10000,
  });

test("all 57 IDs and 56 independently supplied invocation schemas match", () => {
  assert.equal(registry.length, 57);
  assert.equal(new Set(registry.map((s) => s.id)).size, 57);
  assert.equal(new Set(registry.map((s) => s.path.join(" "))).size, 57);
  for (const spec of registry.filter((s) => s.id !== "system.invoke")) {
    const oracle = json(
      "test/contract/r11/schemas/invocations/" + spec.id + ".schema.json",
    );
    assert.equal(canonical(spec.invocation), canonical(oracle), spec.id);
    const example = json(
      "test/contract/r11/examples/invocations/" + spec.id + ".json",
    );
    assert.equal(ajv.compile(oracle)(example), true, spec.id);
    assert.equal(validate(example).command, spec.id);
    const flags = [];
    for (const group of ["context", "input", "options"])
      for (const [key, value] of Object.entries(example[group])) {
        if (
          group === "options" &&
          key === "client_request_id" &&
          spec.id.startsWith("execution.")
        )
          continue;
        flags.push(
          "--" +
            (key === "confirm_high_risk" ? "yes" : key.replaceAll("_", "-")),
        );
        if (typeof value !== "boolean")
          flags.push(typeof value === "string" ? value : JSON.stringify(value));
        else if (value === false) flags.pop();
      }
    const parsed = parseArgs([...spec.path, ...flags]);
    assert.equal(parsed.kind, "typed");
    const typed = validate(parsed.value);
    for (const group of ["context", "input"])
      assert.equal(
        canonical(typed[group]),
        canonical(example[group]),
        spec.id + " " + group,
      );
    ajv.compile(outputSchemas[spec.id]);
  }
});
test("output DTOs reject incomplete successful payloads", () => {
  for (const id of [
    "system.version",
    "system.help",
    "system.capabilities",
    "profile.get",
    "execution.get",
  ])
    assert.throws(() => validateOutput(id, {}));
});
test("strict bytes, duplicate decoded keys, unicode, nesting, trailing values", () => {
  for (const s of [
    '{"a":1,"a":2}',
    '{"a":1,"\\u0061":2}',
    "null null",
    "[1,]",
    '{"x":NaN}',
    '"\\ud800"',
    "1e999",
    "9007199254740993",
    "\ufeff{}",
  ])
    assert.throws(() => parse(s), s);
  assert.throws(() => parseJson(Buffer.from([0xff])));
  assert.throws(() => parseJson(Buffer.alloc(1024 * 1024 + 1, 32)));
  assert.throws(() => parse("[".repeat(33) + "0" + "]".repeat(33)));
  assert.doesNotThrow(() => parse("[".repeat(32) + "0" + "]".repeat(32)));
  assert.equal(
    parseJson(Buffer.from("9223372036854775807"), true),
    "9223372036854775807",
  );
  assert.equal(
    canonical(parse('{"z":3,"a":{"x":2,"b":1}}')),
    '{"a":{"b":1,"x":2},"z":3}',
  );
  assert.equal(
    Object.getPrototypeOf(parse('{"__proto__":{"polluted":true}}')),
    null,
  );
  assert.equal({}.polluted, undefined);
});
test("typed and invoke normalization agree; ambiguous flags are refused", () => {
  const parsed = parseArgs([
    "branch",
    "get",
    "--profile",
    "p",
    "--xid",
    "opaque",
    "--branch-id",
    "9223372036854775807",
  ]);
  assert.equal(parsed.kind, "typed");
  const inv = validate(parsed.value);
  assert.equal(
    canonical(inv),
    canonical(
      validate(
        parse(
          '{"protocol_version":"0.1","contract_revision":"R9","command":"branch.get","context":{"profile":"p"},"input":{"xid":"opaque","branch_id":"9223372036854775807"},"options":{}}',
        ),
      ),
    ),
  );
  assert.throws(() =>
    validate({
      ...parsed.value,
      input: { xid: "x", branch_id: "9223372036854775808" },
    }),
  );
  for (const args of [
    ["version", "--timeout-ms", "1000", "--timeout-ms=2000"],
    ["schema", "system.version", "--command-id", "system.help"],
    ["invoke", "--profile", "p"],
    ["version", "--bad"],
    ["version", "--timeout-ms", "1000oops"],
  ])
    assert.throws(() => parseArgs(args));
  assert.throws(() => validate({ ...parsed.value, options: null }));
  assert.throws(() => validate({ ...parsed.value, command: "system.invoke" }));
});
test("write gates: UUID and direction required, dry-run exception, no write flags on reads", () => {
  const inv = {
    protocol_version: "0.1",
    contract_revision: "R9",
    command: "transaction.retry.pause",
    context: { profile: "p" },
    input: { xid: "x" },
    options: {},
  };
  assert.throws(() => validate(inv));
  assert.doesNotThrow(() =>
    validate({ ...inv, options: { dry_run: true, confirm_high_risk: true } }),
  );
  const full = validate({
    ...inv,
    input: { xid: "x", expected_direction: "rollback" },
    options: { client_request_id: "35BF7B3C-44D2-4CA8-8B6D-5B40C3E6D016" },
  });
  assert.equal(
    full.options.client_request_id,
    "35bf7b3c-44d2-4ca8-8b6d-5b40c3e6d016",
  );
  assert.throws(() =>
    validate({
      ...inv,
      command: "transaction.get",
      options: { dry_run: true },
    }),
  );
});
test("help budgets and framework/domain counts", () => {
  assert.ok(renderHelp([]).split("\n").length <= 45);
  const domains = new Set(
    registry.filter((s) => !s.id.startsWith("system.")).map((s) => s.path[0]),
  );
  assert.equal(domains.size, 12);
  for (const domain of domains)
    assert.ok(renderHelp([domain]).split("\n").length <= 80);
  for (const spec of registry)
    assert.ok(renderHelp(spec.path).split("\n").length <= 140);
});
test("real process produces exactly one envelope on the required stream", () => {
  for (const [args, exit, stream] of [
    [["version"], 0, "stdout"],
    [["version", "--bad"], 2, "stderr"],
    [["transaction", "get", "--profile", "p", "--xid", "x"], 6, "stderr"],
  ]) {
    const child = run(args);
    assert.equal(child.status, exit, child.stderr);
    const envelope = JSON.parse(child[stream]);
    assert.equal(validateEnvelope(envelope), true);
    assert.equal(child[stream === "stdout" ? "stderr" : "stdout"], "");
  }
  const child = run(
    ["invoke"],
    '{"protocol_version":"0.1","contract_revision":"R9","command":"system.version","context":{},"input":{},"options":{}}',
  );
  assert.equal(child.status, 0, child.stderr);
  assert.equal(child.stderr, "");
  assert.equal(JSON.parse(child.stdout).meta.invoked_via, "system.invoke");
});
