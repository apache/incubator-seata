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
import { mkdtempSync, writeFileSync, existsSync, symlinkSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
import { parseYaml } from "../dist/adapter/config/yaml.js";
import { validateOutput } from "../dist/command/registry.js";
import { parseArgs } from "../dist/cmdtree/parse.js";

const cli = new URL("../dist/cli.js", import.meta.url).pathname;
function fixture() {
  const dir = mkdtempSync(join(tmpdir(), "seata-config-cli-")), state = join(dir, "state");
  const file = (name, content) => { const path = join(dir, name); writeFileSync(path, content); return path; };
  const run = (command, input, env = {}, typed) => {
    const result = spawnSync(process.execPath, [cli, ...(typed ?? ["invoke"])], {
      input: typed ? undefined : JSON.stringify({ protocol_version: "0.1", contract_revision: "R9", command, context: {}, input, options: { timeout_ms: 10000 } }),
      cwd: dir, encoding: "utf8", timeout: 15000,
      env: { ...process.env, SEATA_AI_STATE_DIR: state, ...env },
    });
    assert.equal(existsSync(state), false, "local config inspection must not initialise credential/state storage");
    const envelope = JSON.parse(result.stdout || result.stderr);
    if (result.status === 0) validateOutput(command, envelope.data);
    return { ...result, envelope, data: envelope.data };
  };
  return { dir, file, run };
}

test("safe YAML supports nested maps, arrays, Unicode, large integers and separate documents", () => {
  const entries = parseYaml("seata:\n  enabled: true\n  id: 9007199254740993\n  labels: [中文, x]\n---\nseata:\n  enabled: false\n");
  assert.deepEqual(entries.map(e => [e.key, e.value, e.document]), [
    ["seata.enabled", "true", 0], ["seata.id", "9007199254740993", 0], ["seata.labels[0]", "中文", 0],
    ["seata.labels[1]", "x", 0], ["seata.enabled", "false", 1],
  ]);
  assert.equal(entries[0].line, 2);
  assert.equal(parseYaml("x: null")[0].value_kind, "null");
  assert.equal(parseYaml("x: []")[0].value_kind, "collection");
  assert.equal(parseYaml("x: {}")[0].value, "{}");
});

test("YAML rejects duplicate/flattened keys, cycles, unsafe tags and bounded alias amplification", () => {
  for (const source of ["x: 1\nx: 2", "a.b: 1\na:\n  b: 2"]) {
    assert.throws(() => parseYaml(source), e => e.subtype === "config_duplicate_key");
  }
  assert.throws(() => parseYaml("x: &x [*x]"), e => e.subtype === "config_alias_cycle");
  for (const source of ["x: !include https://SECRET.invalid", "x: !!js/function SECRET", "x: .inf", "[one, two]", "x: { <<: { y: 1 } }"]) {
    assert.throws(() => parseYaml(source), e => e.type === "config" && !e.message.includes("SECRET"));
  }
  assert.deepEqual(parseYaml("x: &v normal\ny: *v").map(e => e.value), ["normal", "normal"]);
  const amplified = "x: &x [a,a,a,a,a,a,a,a,a,a]\ny: &y [*x,*x,*x,*x,*x,*x,*x,*x,*x,*x]\nz: &z [*y,*y,*y,*y,*y,*y,*y,*y,*y,*y]\nw: [*z,*z,*z,*z,*z,*z,*z,*z,*z,*z]";
  assert.throws(() => parseYaml(amplified), e => e.subtype === "config_limit_exceeded");
  assert.throws(() => parseYaml("x: " + "[".repeat(1000) + "0" + "]".repeat(1000)), e => e.subtype === "config_limit_exceeded");
});

test("config inspect retains provenance, protects secrets and observes only the environment allowlist", () => {
  const f = fixture(), path = f.file("application.properties", "seata.enabled=true\nservice.vgroupMapping.orders=default\ndb.password=SECRET_PASSWORD\nanything=UNCLASSIFIED_SECRET\nseata.application-id=${APP:SECRET_DEFAULT}\n");
  const result = f.run("config.inspect", { sources: [{ path }] }, { SEATA_NAMESPACE: "public", UNRELATED_SECRET: "ENV_SECRET" });
  assert.equal(result.status, 0, result.stderr);
  assert.doesNotMatch(result.stdout, /SECRET_PASSWORD|UNCLASSIFIED_SECRET|SECRET_DEFAULT|ENV_SECRET|UNRELATED_SECRET/);
  const facts = result.data.observed;
  assert.equal(facts.find(f => f.key === "seata.enabled").value, "true");
  assert.equal(facts.find(f => f.key === "service.vgroupMapping.orders").location, `${path}:2`);
  assert.equal(facts.find(f => f.key === "SEATA_NAMESPACE").scope, "local_cli_process");
  assert.ok(facts.every(f => f.effective_status === "unknown" && f.precedence === null));
  assert.deepEqual(result.data.inferred_effective, []); assert.deepEqual(result.data.shadowed, []);
  assert.equal(result.data.unresolved[0].unresolved_reason, "expression_not_evaluated");
  const typed = f.run("config.inspect", {}, {}, ["config", "inspect", "--sources", JSON.stringify([{ path }]), "--no-include-environment"]);
  assert.equal(typed.status, 0, typed.stderr);
  assert.ok(typed.data.observed.every(f => f.scope === "local_project"));
});

test("boolean flags distinguish omitted, explicit false and true, and reject contradictory aliases", () => {
  assert.equal(parseArgs(["config", "inspect"]).value.input.include_environment, undefined);
  assert.equal(parseArgs(["config", "inspect", "--include-environment"]).value.input.include_environment, true);
  assert.equal(parseArgs(["config", "inspect", "--no-include-environment"]).value.input.include_environment, false);
  assert.throws(() => parseArgs(["config", "inspect", "--include-environment", "--no-include-environment"]), e => e.subtype === "duplicate_argument");
});

test("config validation limits conclusions to local evidence and rejects unknown rules before IO", () => {
  const f = fixture(), path = f.file("application.yml", "seata:\n  enabled: maybe\n  tx-service-group: orders\n");
  const result = f.run("config.validate", { sources: [{ path }] });
  assert.equal(result.status, 0, result.stderr);
  assert.ok(result.data.findings.some(f => f.rule_id === "config_boolean_literal" && f.status === "failed"));
  assert.ok(result.data.findings.some(f => f.rule_id === "remote_effective_configuration" && f.status === "not_evaluated"));
  const ids = new Set(result.data.evidence.map(e => e.id));
  assert.ok(result.data.findings.every(f => f.evidence_ids.every(id => ids.has(id))));
  const bad = f.run("config.validate", { sources: [{ path: "/absent" }], ruleset_id: "missing" });
  assert.equal(bad.envelope.error.subtype, "ruleset_not_found");
});

test("config diff compares observed values and provenance but never equates redacted or unknown effective values", () => {
  const f = fixture();
  const left = f.file("left.yml", "seata:\n  enabled: true\n  namespace: public\npassword: SAME_SECRET\n");
  const right = f.file("right.yml", "seata:\n  enabled: false\n  namespace: public\npassword: SAME_SECRET\n");
  const result = f.run("config.diff", { left: { path: left }, right: { path: right } });
  assert.equal(result.status, 0, result.stderr); assert.doesNotMatch(result.stdout, /SAME_SECRET/);
  const changed = result.data.changes.find(c => c.key === "seata.enabled");
  assert.equal(changed.value, "different"); assert.equal(changed.source, "different");
  assert.equal(changed.scope, "equal"); assert.equal(changed.version, "unknown");
  assert.equal(result.data.changes.find(c => c.key === "seata.namespace").value, "equal");
  assert.ok(result.data.changes.some(c => c.left.redacted && c.value === "unknown"));
  assert.ok(result.data.changes.every(c => c.effective_comparison === "unknown"));
});

test("public config rejects external includes, unsupported formats, invalid UTF-8 and symlink traversal", () => {
  const f = fixture();
  const include = f.file("include.yml", "spring:\n  config:\n    import: https://SECRET.invalid/config\n");
  const result = f.run("config.inspect", { sources: [{ path: include }], include_environment: false });
  assert.equal(result.envelope.error.subtype, "config_network_include_forbidden"); assert.doesNotMatch(result.stderr, /SECRET/);
  const bad = f.file("bad.properties", Buffer.from([0xff]));
  assert.equal(f.run("config.inspect", { sources: [{ path: bad }] }).envelope.error.subtype, "config_encoding_unsupported");
  const unknown = f.file("config.groovy", "SECRET");
  assert.equal(f.run("config.inspect", { sources: [{ path: unknown }] }).envelope.error.subtype, "format_unsupported");
  const link = join(f.dir, "link.properties"); symlinkSync(bad, link);
  assert.equal(f.run("config.inspect", { sources: [{ path: link }] }).envelope.error.subtype, "config_source_unavailable");
});

test("local vgroup checks identify observations without claiming a remote route or a precedence order", () => {
  const f = fixture(), path = f.file("application.properties", "seata.tx-service-group=orders\nservice.vgroupMapping.orders=default\n");
  const good = f.run("config.validate", { sources: [{ path }] });
  assert.equal(good.status, 0, good.stderr);
  const finding = good.data.findings.find(f => f.rule_id === "local_vgroup_mapping");
  assert.equal(finding.status, "passed"); assert.equal(finding.reason_code, "local_mapping_observed_remote_route_not_checked");
  const other = f.file("override.properties", "service.vgroupMapping.orders=other\n");
  const conflict = f.run("config.validate", { sources: [{ path }, { path: other }] });
  const ambiguous = conflict.data.findings.find(f => f.rule_id === "local_vgroup_mapping");
  assert.equal(ambiguous.status, "unknown"); assert.equal(ambiguous.reason_code, "conflicting_observations_precedence_unknown");
  const missing = f.file("missing.properties", "seata.tx-service-group=absent\n");
  assert.equal(f.run("config.validate", { sources: [{ path: missing }] }).data.findings.find(f => f.rule_id === "local_vgroup_mapping").status, "unknown");
});
