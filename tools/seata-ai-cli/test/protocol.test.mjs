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

test("public invoke schema accepts every registered target and rejects delegation to itself", () => {
  const result = run(["schema", "system.invoke"]);
  assert.equal(result.status, 0, result.stderr);
  const schema = JSON.parse(result.stdout).data.invocation_schema;
  const accepts = ajv.compile(schema);
  const targets = registry.filter(spec => spec.id !== "system.invoke");
  assert.deepEqual(schema.oneOf.map(s => s.properties.command.const),
    targets.map(spec => spec.id));
  for (const spec of targets) {
    const example = json("test/contract/r11/examples/invocations/" + spec.id + ".json");
    assert.equal(accepts(example), true, spec.id);
    const child = run(["invoke"], JSON.stringify(example));
    assert.equal(child.status, 0, child.stderr);
    const envelope = JSON.parse(child.stdout);
    assert.equal(envelope.command, spec.id);
    assert.equal(envelope.meta.invoked_via, "system.invoke");
  }
  const example = json("test/contract/r11/examples/invocations/system.version.json");
  const wrapper = { ...example, command: "system.invoke", input: { invocation: example } };
  assert.equal(accepts(wrapper), false);
  const child = run(["invoke"], JSON.stringify(wrapper));
  assert.equal(child.status, 2);
  assert.equal(JSON.parse(child.stderr).error.subtype, "recursive_invoke");
});

test("registered command IDs, flags and independent invocation fixtures agree", () => {
  assert.equal(new Set(registry.map((s) => s.id)).size, registry.length);
  assert.equal(new Set(registry.map((s) => s.path.join(" "))).size, registry.length);
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
        flags.push("--" + key.replaceAll("_", "-"),
          typeof value === "string" ? value : JSON.stringify(value));
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
test("capabilities lists the five executable framework commands", () => {
  const result = run(["capabilities"]);
  assert.equal(result.status, 0, result.stderr);
  const data = JSON.parse(result.stdout).data;
  assert.deepEqual(data.commands.map(command => command.command_id).sort(), [
    "system.capabilities", "system.help", "system.invoke", "system.schema", "system.version",
  ]);
  assert.ok(data.commands.every(command => command.status === "available"));
  assert.ok(data.commands.every(command => command.backends === "local"));
  assert.equal(data.runtime.management_reachable, "not_evaluated");
  assert.equal(data.runtime.storage_safe, "unavailable");
  assert.equal(data.runtime.server_mutations, "disabled");
});
test("output DTOs reject incomplete successful payloads", () => {
  for (const id of [
    "system.version",
    "system.help",
    "system.capabilities",
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
  const parsed = parseArgs(["schema", "system.help"]);
  assert.equal(parsed.kind, "typed");
  const fixture = json("test/contract/r11/examples/invocations/system.schema.json");
  assert.equal(canonical(validate(parsed.value)),
    canonical(validate({ ...fixture, input: { command_id: "system.help" } })));
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
test("help budgets and framework/domain counts", () => {
  assert.ok(renderHelp([]).split("\n").length <= 45);
  const domains = new Set(
    registry.filter((s) => !s.id.startsWith("system.")).map((s) => s.path[0]),
  );
  for (const domain of domains)
    assert.ok(renderHelp([domain]).split("\n").length <= 80);
  for (const spec of registry)
    assert.ok(renderHelp(spec.path).split("\n").length <= 140);
});
test("invoke help describes its file option and field help requires a command", () => {
  const child = run(["invoke", "--help"]);
  assert.equal(child.status, 0, child.stderr);
  assert.match(child.stdout, /--file <path>/);
  assert.doesNotMatch(child.stdout, /--invocation|--profile|--timeout-ms/);
  const invalid = run(["help", "--field", "command_id"]);
  assert.equal(invalid.status, 2);
  assert.match(JSON.parse(invalid.stderr).error.message, /leaf command/);
});
test("options with missing values do not consume following flags", () => {
  const invalid = run(["version", "--profile", "--namespace=public"]);
  assert.equal(invalid.status, 2);
  assert.match(JSON.parse(invalid.stderr).error.message, /requires a value/);
  const explicit = run(["version", "--profile=--namespace=public"]);
  assert.equal(explicit.status, 0, explicit.stderr);
  assert.equal(JSON.parse(explicit.stdout).context.profile, "--namespace=public");
});
test("validated typed and JSON invocations return the caller's request ID", () => {
  const id = "ABCDEF01-1234-4234-9234-ABCDEF123456";
  const example = json("test/contract/r11/examples/invocations/system.version.json");
  for (const [child, status, stream] of [
    [run(["version", "--client-request-id", id]), 0, "stdout"],
    [run(["invoke"], JSON.stringify({ ...example, options: { client_request_id: id } })), 0, "stdout"],
    [run(["schema", "missing", "--client-request-id", id]), 2, "stderr"],
  ]) {
    assert.equal(child.status, status);
    const envelope = JSON.parse(child[stream]);
    assert.equal(validateEnvelope(envelope), true);
    assert.equal(envelope.meta.client_request_id, id.toLowerCase());
  }
});
test("real process produces exactly one envelope on the required stream", () => {
  for (const [args, exit, stream] of [
    [["version"], 0, "stdout"],
    [["version", "--bad"], 2, "stderr"],
    [["schema", "transaction.get"], 2, "stderr"],
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
