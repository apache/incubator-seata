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
import http from "node:http";
import { once } from "node:events";
import { spawn } from "node:child_process";
import { mkdtemp, writeFile, readdir, readFile, stat } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { CredentialStore } from "../dist/adapter/auth/credentials.js";
import { profileConfig } from "../dist/app/profile.js";
import { validateOutput } from "../dist/command/registry.js";
import { canonical, digest } from "../dist/protocol/value.js";
import { bytesDigest } from "../dist/adapter/export/archive.js";
import { reconciliationAdapterDigest, verifyReconciliationTarget } from "../dist/app/reconcile.js";
import { randomUUID } from "node:crypto";
import { DatabaseSync } from "node:sqlite";
import { compareVgroupFacts } from "../dist/app/diagnose.js";

const transaction = { xid: "opaque-1", transactionId: "9223372036854775807", status: 5, applicationId: "orders",
  transactionName: "checkout", beginTime: 1700000000000, branchSessionVOs: [
    { xid: "opaque-1", transactionId: "9223372036854775807", branchId: "9007199254740993", status: 9, branchType: "AT", resourceId: "jdbc:mysql://db/test" },
  ] };
const lock = { xid: "opaque-1", branchId: "9007199254740993", transactionId: "9223372036854775807",
  resourceId: "jdbc:mysql://db/test", tableName: "orders", pk: "42", rowKey: "row-1", gmtCreate: 1700000000000 };
function page(url, rows) {
  const pageNum = Number(url.searchParams.get("pageNum")), pageSize = Number(url.searchParams.get("pageSize"));
  return { code: "200", data: rows.slice((pageNum - 1) * pageSize, pageNum * pageSize), total: rows.length, pageNum, pageSize };
}
async function fixture(t) {
  const requests = [], mode = { lockFailure: false, namingFailure: false, emptyRoute: false, manyTransactions: false, hangLocks: false, duplicateBranches: false, transactionMissing: false, transactionStatus: transaction.status, branchCount: null, lockCount: null, lockRetry: false };
  const server = http.createServer((req, res) => {
    requests.push({ method: req.method, path: req.url, authorization: req.headers.authorization });
    const url = new URL(req.url, "http://localhost");
    let body;
    switch (url.pathname) {
      case "/base/api/v1/overview/getData": body = { code: "200", data: [] }; break;
      case "/base/api/v1/naming/namespace":
        if (mode.namingMalformed) { res.end("{"); return; }
        if (mode.namingFailure) { res.writeHead(401); res.end("SECRET raw page"); return; }
        body = { code: "200", data: { public: { clusters: ["default"], vgroups: [] } } }; break;
      case "/base/api/v1/naming/clusters":
        if (mode.clusterFailureAfterProbe && requests.filter(r => r.path.includes("naming/clusters")).length > 1) {
          res.writeHead(403); res.end("SECRET permission page"); return;
        }
        body = [{ clusterName: "default", clusterType: "DB", vGroupMapping: ["orders"], unitData: [] }]; break;
      case "/base/api/v1/naming/clusterData": body = { code: "200", data: { clusterName: "default", clusterType: "DB",
        unitData: mode.emptyRoute ? {} : { unit: { unitName: "unit", namingInstanceList: [
          { healthy: true, version: "source-fixture", role: "MEMBER", control: { host: "192.0.2.1", port: 7091 }, transaction: null },
        ] } } } }; break;
      case "/base/api/v1/console/globalSession/query":
        if (mode.transactionRetryFailure && url.searchParams.has("xid")) { res.writeHead(503); res.end("SECRET"); return; }
        if (mode.transactionFailure) { res.writeHead(403); res.end("SECRET permission page"); return; }
        body = page(url, mode.manyTransactions && !url.searchParams.has("xid") ? [
          { ...transaction, xid: "normal", status: 1 }, transaction,
          { ...transaction, xid: "future-status", status: 222 }, { ...transaction, xid: "failed", status: 12 },
        ] : mode.transactionMissing ? [] : [mode.duplicateBranches ? { ...transaction, branchSessionVOs: [...transaction.branchSessionVOs, ...transaction.branchSessionVOs] } : { ...transaction, status: mode.transactionStatus }]); break;
      case "/base/api/v1/console/globalLock/query":
        if (mode.lockReset) { res.destroy(); return; }
        if (mode.hangLocks) return;
        if (mode.lockRetry) { mode.lockRetry = false; res.writeHead(503); res.end("{}"); return; }
        if (mode.lockFailure) { res.writeHead(mode.lockStatus ?? 403); res.end("SECRET permission page"); return; }
        body = page(url, mode.lockCount !== null ? Array.from({ length: mode.lockCount }, (_, i) =>
          ({ ...lock, pk: String(i), rowKey: `row-${i}`,
            resourceId: mode.lockResourceLength ? "r".repeat(mode.lockResourceLength) : lock.resourceId })) : url.searchParams.has("xid") ? [lock] : [lock, { ...lock, xid: "other", branchId: "9007199254740994", rowKey: "row-2" }]); break;
      default: res.writeHead(404); res.end("{}"); return;
    }
    if (url.pathname.endsWith("globalSession/query") && mode.branchCount !== null)
      body.data[0].branchSessionVOs = Array.from({ length: mode.branchCount }, (_, i) =>
        ({ ...transaction.branchSessionVOs[0], branchId: String(i + 1),
          resourceId: mode.branchResourceLength ? "r".repeat(mode.branchResourceLength) : transaction.branchSessionVOs[0].resourceId }));
    if (url.pathname.endsWith("globalSession/query") && !url.searchParams.has("xid") && mode.afterProbe) {
      const hook = mode.afterProbe; mode.afterProbe = null;
      hook().then(() => res.end(JSON.stringify(body)), error => { mode.hookError = error; res.destroy(); });
      return;
    }
    res.end(JSON.stringify(body));
  });
  server.listen(0, "127.0.0.1"); await once(server, "listening");
  const root = await mkdtemp(join(tmpdir(), "seata-analysis-")), store = new StateStore(root);
  const endpoint = `http://127.0.0.1:${server.address().port}/base`;
  await store.call("profile.create", profileConfig({ name: "p", environment: "development",
    management: { console_endpoint: endpoint, naming_endpoint: endpoint, compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  const { flow } = await store.call("flow.create", { name: "p" });
  await store.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  const binding = await store.call("auth.binding", { name: "p" });
  await new CredentialStore(root, "protected_file", binding.state_store_id)
    .put(staged.credential_ref, "Bearer ANALYSIS_TEST", AbortSignal.timeout(5000));
  await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref });
  await store.close();
  t.after(() => new Promise(resolve => server.close(resolve)));
  const run = async (args, withProfile = true) => {
    const child = spawn(process.execPath, [new URL("../dist/cli.js", import.meta.url).pathname, ...args, ...(withProfile ? ["--profile", "p"] : [])], {
      env: { ...process.env, SEATA_AI_STATE_DIR: root }, cwd: root, stdio: ["ignore", "pipe", "pipe"],
    });
    let stdout = "", stderr = "";
    child.stdout.on("data", c => stdout += c); child.stderr.on("data", c => stderr += c);
    const [code] = await once(child, "close");
    return { code, stdout, stderr, envelope: JSON.parse(stdout || stderr) };
  };
  return { requests, mode, run, root };
}

for (const scope of ["transaction", "environment"]) test(`diagnosis keeps its original target binding across ${scope} phases`, async t => {
  const f = await fixture(t);
  f.mode.afterProbe = async () => {
    const store = new StateStore(f.root);
    try {
      const binding = await store.call("auth.binding", { name: "p" }), p = binding.profile;
      await store.call("profile.update", { name: "p", expected_revision: p.revision,
        ...profileConfig({ name: "p", environment: "development", management: p.management,
          target: { namespace: "public", cluster: "other" }, policy: { storage_policy: "protected_file" } }, true) });
      const { flow } = await store.call("flow.create", { name: "p" });
      await store.call("flow.submit", { flow_id: flow.flow_id });
      const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
      await new CredentialStore(f.root, "protected_file", binding.state_store_id)
        .put(staged.credential_ref, "Bearer UPDATED_TEST", AbortSignal.timeout(5000));
      await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref });
    } finally { await store.close(); }
  };
  const result = await f.run(["diagnose", "run", "--scope", scope, ...(scope === "transaction" ? ["--xid", "opaque-1"] : [])]);
  assert.equal(f.mode.hookError, undefined);
  assert.equal(result.code, 9, result.stdout);
  assert.ok(result.envelope.data.not_evaluated.some(item => item.reason_code === "credential_changed"));
  assert.equal(f.requests.length, 5, "No request may silently switch to the replacement Profile binding");
  assert.equal(result.envelope.data.completeness.read_attempts.length, 5, "Probe attempts must not be duplicated");
});

test("reconciliation binding checks every management descriptor field but allows an explicitly chosen equivalent Profile", () => {
  const target = { console_base_endpoint: "https://example.test/console", naming_base_endpoint: "https://example.test/naming",
    namespace: "public", cluster: "default", trust_digest: "trust", route_assurance: "selector_only",
    adapter_id: "seata-src-41694e14", adapter_digest: reconciliationAdapterDigest, command_schema_version: "0.1" };
  const saved = { target, target_digest: "target" };
  const binding = { target_digest: "target", trust_digest: "trust", profile: { profile_id: "replacement", revision: 9,
    management: { console_endpoint: target.console_base_endpoint, naming_endpoint: target.naming_base_endpoint,
      compatibility_id: target.adapter_id }, target: { namespace: "public", cluster: "default" } } };
  assert.doesNotThrow(() => verifyReconciliationTarget(saved, binding, {}));
  for (const key of Object.keys(target))
    assert.throws(() => verifyReconciliationTarget({ ...saved, target: { ...target, [key]: "changed" } }, binding, {}),
      e => e.subtype === "execution_target_mismatch", key);
  assert.throws(() => verifyReconciliationTarget(saved, { ...binding, target_digest: "changed" }, {}));
  assert.throws(() => verifyReconciliationTarget(saved, binding, { vgroup: "different" }));
});

test("public reconciliation returns evidence without persisting history and rejects target overrides before HTTP", async t => {
  const f = await fixture(t), store = new StateStore(f.root);
  let request;
  try {
    const binding = await store.call("auth.binding", { name: "p" }), p = binding.profile;
    request = randomUUID();
    await store.call("execution.claim", { client_request_id: request, command: "transaction.retry.pause",
      intent: { xid: "opaque-1" }, intent_digest: digest({ xid: "opaque-1" }), profile_id: p.profile_id,
      target_digest: binding.target_digest, target: { profile_id: p.profile_id, profile_revision: p.revision,
        console_base_endpoint: p.management.console_endpoint, naming_base_endpoint: p.management.naming_endpoint,
        namespace: "public", cluster: "default", trust_digest: binding.trust_digest, route_assurance: "selector_only",
        adapter_id: "seata-src-41694e14", adapter_digest: reconciliationAdapterDigest, command_schema_version: "0.1" } });
  } finally { await store.close(); }
  const noProfile = await f.run(["execution", "reconcile", "--client-request-id", request], false);
  assert.equal(noProfile.code, 2); assert.equal(f.requests.length, 0);
  const mismatch = await f.run(["execution", "reconcile", "--client-request-id", request, "--cluster", "other"]);
  assert.equal(mismatch.code, 6); assert.match(mismatch.stderr, /execution_target_mismatch/);
  assert.equal(f.requests.length, 0);
  const before = await f.run(["execution", "get", "--client-request-id", request], false);
  const result = await f.run(["execution", "reconcile", "--client-request-id", request]);
  assert.equal(result.code, 0, result.stderr); validateOutput("execution.reconcile", result.envelope.data);
  const data = result.envelope.data;
  assert.equal(data.execution.outcome, "not_started"); assert.equal(data.observation, "unknown");
  assert.equal(data.causality, "unproven"); assert.equal(data.replay_allowed, false);
  assert.equal(data.evidence[0].facts.transaction.xid, "opaque-1");
  assert.ok(f.requests.every(r => r.method === "GET"));
  const get = await f.run(["execution", "get", "--client-request-id", request], false);
  assert.equal(get.code, 0, get.stderr);
  assert.deepEqual(get.envelope.data, before.envelope.data);
  assert.equal(get.envelope.data.execution.request_phase, "not_started");
  f.mode.transactionStatus = 19;
  const condition = await f.run(["execution", "reconcile", "--client-request-id", request, "--export", "reconciliation.json"]);
  assert.equal(condition.code, 0, condition.stderr);
  validateOutput("execution.reconcile", condition.envelope.data);
  assert.equal(condition.envelope.data.observation, "postcondition_observed");
  assert.equal(condition.envelope.data.desired_condition_currently_observed.observed, true);
  assert.equal(condition.envelope.data.execution.outcome, "not_started");
  assert.equal(condition.envelope.data.execution.business_completion, "not_claimed");
  assert.equal(condition.envelope.data.replay_allowed, false);
  assert.equal(await readFile(join(f.root, "reconciliation.json"), "utf8"), condition.stdout);
  const afterExport = await f.run(["execution", "get", "--client-request-id", request], false);
  assert.deepEqual(afterExport.envelope.data, before.envelope.data);
});

test("reconciliation records an empty current-route transaction observation without proving historical success", async t => {
  const f = await fixture(t), store = new StateStore(f.root), request = randomUUID();
  try {
    const binding = await store.call("auth.binding", { name: "p" }), p = binding.profile;
    await store.call("execution.claim", { client_request_id: request, command: "transaction.retry.pause",
      intent: { xid: "opaque-1" }, intent_digest: digest({ xid: "opaque-1" }), profile_id: p.profile_id,
      target_digest: binding.target_digest, target: { profile_id: p.profile_id, profile_revision: p.revision,
        console_base_endpoint: p.management.console_endpoint, naming_base_endpoint: p.management.naming_endpoint,
        namespace: "public", cluster: "default", trust_digest: binding.trust_digest, route_assurance: "selector_only",
        adapter_id: "seata-src-41694e14", adapter_digest: reconciliationAdapterDigest, command_schema_version: "0.1" } });
  } finally { await store.close(); }
  f.mode.transactionMissing = true;
  const result = await f.run(["execution", "reconcile", "--client-request-id", request]);
  assert.equal(result.code, 0, result.stderr);
  validateOutput("execution.reconcile", result.envelope.data);
  assert.equal(result.envelope.data.observation, "unknown");
  assert.equal(result.envelope.data.execution.outcome, "not_started");
  assert.equal(result.envelope.data.desired_condition_currently_observed.observed, null);
  assert.equal(result.envelope.data.completeness.status, "unknown");
  assert.equal(result.envelope.data.current_evidence[0].facts.transaction, null);
  assert.equal(result.envelope.data.current_evidence[0].facts.absence_meaning, "not_observed_in_current_route");
  assert.equal(result.envelope.data.replay_allowed, false);
  const history = await f.run(["execution", "get", "--client-request-id", request], false);
  assert.equal(history.envelope.data.events.some(e => e.kind === "READ_ONLY_OBSERVATION"), false);
  const exact = await f.run(["transaction", "get", "--xid", "opaque-1"]);
  assert.equal(exact.code, 1);
  assert.equal(exact.envelope.error.subtype, "lookup_incomplete");
  assert.ok(f.requests.every(row => row.method === "GET"));
});

test("branch reconciliation records an unobserved parent without treating it as a deleted branch", async t => {
  const f = await fixture(t), store = new StateStore(f.root), request = randomUUID();
  try {
    const binding = await store.call("auth.binding", { name: "p" }), p = binding.profile;
    const intent = { xid: "opaque-1", branch_id: "9007199254740993" };
    await store.call("execution.claim", { client_request_id: request, command: "branch.retry.pause",
      intent, intent_digest: digest(intent), profile_id: p.profile_id,
      target_digest: binding.target_digest, target: { profile_id: p.profile_id, profile_revision: p.revision,
        console_base_endpoint: p.management.console_endpoint, naming_base_endpoint: p.management.naming_endpoint,
        namespace: "public", cluster: "default", trust_digest: binding.trust_digest, route_assurance: "selector_only",
        adapter_id: "seata-src-41694e14", adapter_digest: reconciliationAdapterDigest, command_schema_version: "0.1" } });
  } finally { await store.close(); }
  f.mode.transactionMissing = true;
  const result = await f.run(["execution", "reconcile", "--client-request-id", request]);
  assert.equal(result.code, 0, result.stderr);
  validateOutput("execution.reconcile", result.envelope.data);
  assert.equal(result.envelope.data.observation, "unknown");
  assert.equal(result.envelope.data.completeness.status, "unknown");
  assert.deepEqual(result.envelope.data.current_evidence[0].facts.branches, []);
  assert.equal(result.envelope.data.current_evidence[0].facts.absence_meaning, "not_observed_in_current_route");
  assert.equal(result.envelope.data.execution.outcome, "not_started");
  assert.equal(result.envelope.data.replay_allowed, false);
  assert.ok(f.requests.every(row => row.method === "GET"));
});

test("lock reconciliation returns large evidence without persisting it", async t => {
  const f = await fixture(t), store = new StateStore(f.root), request = randomUUID();
  f.mode.lockCount = 2000;
  try {
    const binding = await store.call("auth.binding", { name: "p" }), p = binding.profile;
    const intent = { xid: "opaque-1", branch_id: "9007199254740993",
      resource_id: lock.resourceId, table_name: "orders", pk: "42" };
    await store.call("execution.claim", { client_request_id: request, command: "lock.delete",
      intent, intent_digest: digest(intent), profile_id: p.profile_id, target_digest: binding.target_digest,
      target: { profile_id: p.profile_id, profile_revision: p.revision,
        console_base_endpoint: p.management.console_endpoint, naming_base_endpoint: p.management.naming_endpoint,
        namespace: "public", cluster: "default", trust_digest: binding.trust_digest, route_assurance: "selector_only",
        adapter_id: "seata-src-41694e14", adapter_digest: reconciliationAdapterDigest, command_schema_version: "0.1" } });
  } finally { await store.close(); }
  const result = await f.run(["execution", "reconcile", "--client-request-id", request]);
  assert.equal(result.code, 0, result.stderr);
  const data = result.envelope.data;
  validateOutput("execution.reconcile", data);
  assert.equal(data.current_evidence[0].facts.locks.length, 2000);
  assert.ok(Buffer.byteLength(canonical(data.current_evidence)) > 256 * 1024);
  assert.equal(data.completeness.status, "complete");
  assert.equal(data.observation, "unknown");
  assert.equal(data.replay_allowed, false);
  const history = await f.run(["execution", "get", "--client-request-id", request], false);
  assert.equal(history.code, 0, history.stderr);
  assert.equal(history.envelope.data.events.some(e => e.kind === "READ_ONLY_OBSERVATION"), false);
  assert.equal(history.envelope.data.execution.outcome, "not_started");
  assert.ok(f.requests.every(r => r.method === "GET"));
});

test("connectivity and profile capabilities probe read paths without certifying the deployment", async t => {
  const f = await fixture(t);
  const staticCapabilities = await f.run(["capabilities"], false);
  assert.equal(staticCapabilities.code, 0); assert.equal(f.requests.length, 0);
  assert.equal(staticCapabilities.envelope.data.commands.find(c => c.command_id === "transaction.trace").status, "not_evaluated");
  const checked = await f.run(["environment", "connectivity", "check"]);
  assert.equal(checked.code, 0, checked.stderr); validateOutput("connectivity.check", checked.envelope.data);
  assert.equal(checked.envelope.data.overall, "available");
  assert.equal(checked.envelope.data.assurance.authentication, "credential_attached_read_succeeded");
  assert.ok(checked.envelope.data.assurance.limitations.includes("token_validity_not_proven_by_read"));
  for (const check of checked.envelope.data.checks)
    assert.equal(check.completeness.read_attempts.length, 1, check.id);
  assert.doesNotMatch(checked.stdout, /Bearer ANALYSIS_TEST/);
  assert.ok(f.requests.every(request => request.authorization === "Bearer ANALYSIS_TEST"));
  const capabilities = await f.run(["capabilities", "transaction.get"]);
  assert.equal(capabilities.envelope.data.commands[0].status, "degraded");
  assert.equal(capabilities.envelope.data.commands[0].scope, "profile");
  assert.equal(capabilities.envelope.data.runtime.compatibility_evidence, "source_inspected");
  f.mode.emptyRoute = true;
  const missing = await f.run(["capabilities", "transaction.get"]);
  assert.equal(missing.envelope.data.commands[0].reason_code, "route_not_found");
  f.mode.namingFailure = true;
  const denied = await f.run(["environment", "connectivity", "check"]);
  assert.equal(denied.envelope.data.checks.find(c => c.id === "naming_namespace").error_type, "authentication");
  const deniedCompleteness = denied.envelope.data.checks.find(c => c.id === "naming_namespace").completeness;
  assert.equal(deniedCompleteness.status, "partial");
  assert.deepEqual(deniedCompleteness.read_attempts.map(a => a.http_status), [401]);
  f.mode.namingFailure = false; f.mode.namingMalformed = true;
  const namespaceReads = f.requests.filter(r => r.path.includes("naming/namespace")).length;
  const malformed = await f.run(["environment", "connectivity", "check"]);
  const malformedCheck = malformed.envelope.data.checks.find(c => c.id === "naming_namespace");
  assert.equal(malformedCheck.status, "unavailable");
  assert.equal(malformedCheck.completeness.pages_read, 1);
  assert.deepEqual(malformedCheck.completeness.read_attempts.map(a => a.http_status), [200]);
  assert.equal(f.requests.filter(r => r.path.includes("naming/namespace")).length, namespaceReads + 1);
  assert.doesNotMatch(denied.stdout, /SECRET/);
  assert.ok(f.requests.every(r => r.method === "GET" && r.path.startsWith("/base/api/")));
});

test("connectivity capability reflects the scoped probe instead of handler presence", async t => {
  const f = await fixture(t);
  const staticResult = await f.run(["capabilities", "connectivity.check"], false);
  assert.equal(staticResult.code, 0, staticResult.stderr);
  assert.equal(staticResult.envelope.data.commands[0].status, "not_evaluated");
  assert.equal(f.requests.length, 0);
  const healthy = await f.run(["capabilities", "connectivity.check"]);
  assert.equal(healthy.code, 0, healthy.stderr);
  assert.equal(healthy.envelope.data.commands[0].status, "degraded");
  assert.equal(healthy.envelope.data.commands[0].scope, "profile");
  f.mode.emptyRoute = true;
  const partial = await f.run(["capabilities", "connectivity.check"]);
  assert.equal(partial.code, 0, partial.stderr);
  assert.equal(partial.envelope.data.commands[0].status, "degraded");
  assert.equal(partial.envelope.data.commands[0].reason_code, "some_read_paths_unavailable");
});

for (const durableIntent of [false, true]) test(`reconciliation preserves historical outcome with durable intent: ${durableIntent}`, async t => {
  const f = await fixture(t), store = new StateStore(f.root), request = randomUUID();
  try {
    const binding = await store.call("auth.binding", { name: "p" }), p = binding.profile;
    const target = { profile_id: p.profile_id, profile_revision: p.revision,
      console_base_endpoint: p.management.console_endpoint, naming_base_endpoint: p.management.naming_endpoint,
      namespace: "public", cluster: "default", trust_digest: binding.trust_digest, route_assurance: "selector_only",
      adapter_id: "seata-src-41694e14", adapter_digest: reconciliationAdapterDigest, command_schema_version: "0.1" };
    const input = { xid: "opaque-1", expected_direction: "rollback" };
    const claimed = await store.call("execution.claim", { client_request_id: request, command: "transaction.retry.pause",
      intent: input, intent_digest: digest(input), profile_id: p.profile_id, target_digest: binding.target_digest, target });
    const id = claimed.execution.execution_id;
    await store.call("execution.lock", { execution_id: id });
    const plan = {
      plan_id: randomUUID(), plan_format_version: 1, command: "transaction.retry.pause", target, normalized_input: input,
      before: { global_status: "RollbackRetrying", unused_secret: "HISTORICAL_SECRET" },
      preconditions: [{ global_status: "RollbackRetrying" }], material_digest: digest({ status: 5 }),
      request_digest: digest({ test_only: "no_service_write" }),
      created_at: new Date().toISOString(), expires_at: new Date(Date.now() + 120000).toISOString() };
    const prepared = await store.call("execution.prepare", { execution_id: id, plan });
    if (durableIntent) {
      // Persist only the CLI journal boundary; never construct a transport or send a server write.
      const committed = await store.call("execution.sendIntent", { execution_id: id,
        record_version: prepared.execution.record_version, intent_digest: digest(input),
        material_digest: plan.material_digest, request_digest: plan.request_digest,
        auth_generation: p.auth_generation, profile_revision: p.revision,
        trust_digest: binding.trust_digest, credential_ref: binding.credential.credential_ref });
      assert.equal(committed.execution.state, "SEND_INTENT");
      assert.equal(committed.execution.outcome, "unknown");
    }
  } finally { await store.close(); }
  const same = await f.run(["execution", "reconcile", "--client-request-id", request]);
  assert.equal(same.code, 0, same.stderr); validateOutput("execution.reconcile", same.envelope.data);
  assert.equal(same.envelope.data.comparison_to_before.status, "unchanged");
  assert.equal(same.envelope.data.observation, "precondition_still_observed");
  assert.equal(same.envelope.data.execution.outcome, durableIntent ? "unknown" : "not_started");
  f.mode.transactionStatus = 20;
  const changed = await f.run(["execution", "reconcile", "--client-request-id", request]);
  assert.equal(changed.code, 0, changed.stderr); validateOutput("execution.reconcile", changed.envelope.data);
  assert.equal(changed.envelope.data.comparison_to_before.status, "changed");
  assert.equal(changed.envelope.data.comparison_to_before.before_status.code, 5);
  assert.equal(changed.envelope.data.comparison_to_before.current_status.code, 20);
  assert.equal(changed.envelope.data.observation, "postcondition_observed");
  assert.equal(changed.envelope.data.execution.outcome, durableIntent ? "unknown" : "not_started");
  assert.equal(changed.envelope.data.causality, "unproven");
  assert.equal(changed.envelope.data.replay_allowed, false);
  assert.doesNotMatch(same.stdout + changed.stdout, /HISTORICAL_SECRET/);
  const beforeFailure = await f.run(["execution", "get", "--client-request-id", request], false);
  f.mode.transactionFailure = true;
  const failed = await f.run(["execution", "reconcile", "--client-request-id", request]);
  assert.equal(failed.code, 3, failed.stderr);
  assert.doesNotMatch(failed.stderr, /SECRET/);
  const afterFailure = await f.run(["execution", "get", "--client-request-id", request], false);
  assert.deepEqual(afterFailure.envelope.data, beforeFailure.envelope.data);
  assert.equal(afterFailure.envelope.data.recovery.replay_allowed, false);
  if (durableIntent) {
    assert.equal(afterFailure.envelope.data.execution.state, "SEND_INTENT");
    assert.equal(afterFailure.envelope.data.execution.request_phase, "request_maybe_sent");
    assert.equal(afterFailure.envelope.data.execution.response_ack, "none");
    assert.deepEqual(afterFailure.envelope.data.events.filter(e => e.kind === "READ_ONLY_OBSERVATION")
      .map(e => e.data.observation), []);
  }
  assert.ok(f.requests.every(request => request.method === "GET"));
});

test("explicit JSON export preserves the public result and refuses overwrite before HTTP", async t => {
  const f = await fixture(t), path = join(f.root, "query.json");
  const result = await f.run(["transaction", "get", "--xid", "opaque-1", "--export", path]);
  assert.equal(result.code, 0, result.stderr);
  assert.equal(await readFile(path, "utf8"), result.stdout);
  assert.equal((await stat(path)).mode & 0o777, 0o600);
  assert.doesNotMatch(result.stdout, /Bearer ANALYSIS_TEST/);
  const count = f.requests.length;
  const conflict = await f.run(["transaction", "get", "--xid", "opaque-1", "--export", path]);
  assert.equal(conflict.envelope.error.subtype, "export_exists");
  assert.equal(f.requests.length, count);
  assert.equal(await readFile(path, "utf8"), result.stdout);
  f.mode.lockFailure = true;
  const partial = await f.run(["transaction", "trace", "--xid", "opaque-1", "--export", "partial.json"]);
  assert.equal(partial.code, 9, partial.stderr);
  assert.equal(await readFile(join(f.root, "partial.json"), "utf8"), partial.stdout);
  f.mode.transactionFailure = true;
  const failed = await f.run(["transaction", "get", "--xid", "opaque-1", "--export", "failed.json"]);
  assert.equal(failed.code, 3);
  await assert.rejects(stat(join(f.root, "failed.json")), { code: "ENOENT" });
});

test("ordinary queries and diagnosis leave execution history empty", async t => {
  const f = await fixture(t);
  for (const args of [["transaction", "get", "--xid", "opaque-1"],
    ["diagnose", "run", "--scope", "transaction", "--xid", "opaque-1"]]) {
    const result = await f.run(args);
    assert.equal(result.code, 0, result.stderr);
  }
  const db = new DatabaseSync(join(f.root, "state.db"), { readOnly: true });
  try {
    assert.equal(db.prepare("SELECT count(*) AS n FROM executions").get().n, 0);
    assert.equal(db.prepare("SELECT count(*) AS n FROM execution_events").get().n, 0);
  } finally { db.close(); }
  assert.equal((await readdir(f.root)).some(name => name.endsWith(".json")), false);
});

test("invoke supports explicit export in options", async t => {
  const f = await fixture(t), path = join(f.root, "invocation.json");
  await writeFile(path, JSON.stringify({ protocol_version: "0.1", contract_revision: "R9",
    command: "transaction.get", context: { profile: "p" }, input: { xid: "opaque-1" },
    options: { export: "invoked.json" } }));
  const result = await f.run(["invoke", "--file", path], false);
  assert.equal(result.code, 0, result.stderr);
  assert.equal(await readFile(join(f.root, "invoked.json"), "utf8"), result.stdout);
});

test("abnormal transaction analysis filters across the observed set and includes unknown status without inventing its outcome", async t => {
  const f = await fixture(t); f.mode.manyTransactions = true;
  const result = await f.run(["transaction", "abnormal", "--page-size", "1", "--page", "2"]);
  assert.equal(result.code, 0, result.stderr); validateOutput("transaction.abnormal", result.envelope.data);
  assert.equal(result.envelope.data.pagination.total, 3);
  assert.equal(result.envelope.data.transactions[0].xid, "future-status");
  assert.equal(result.envelope.data.findings[0].confidence, "unknown");
  assert.deepEqual(result.envelope.data.findings[0].evidence_ids, [result.envelope.data.evidence[0].id]);
  const limited = await f.run(["transaction", "abnormal", "--limit", "1"]);
  assert.equal(limited.envelope.data.completeness.status, "truncated");
  assert.equal(limited.envelope.data.completeness.limit_reason, "entity_limit");
  const before = f.requests.length;
  const bad = await f.run(["transaction", "abnormal", "--ruleset-id", "unknown"]);
  assert.equal(bad.code, 2); assert.equal(f.requests.length, before);
});

test("enum code zero is investigated with unknown confidence in analysis and diagnosis", async t => {
  const f = await fixture(t); f.mode.transactionStatus = 0;
  const abnormal = await f.run(["transaction", "abnormal"]);
  assert.equal(abnormal.code, 0, abnormal.stderr);
  assert.equal(abnormal.envelope.data.transactions[0].status.code, 0);
  assert.equal(abnormal.envelope.data.findings[0].confidence, "unknown");
  const diagnosis = await f.run(["diagnose", "run", "--scope", "transaction", "--xid", "opaque-1"]);
  assert.equal(diagnosis.code, 0, diagnosis.stderr);
  const finding = diagnosis.envelope.data.findings.find(f => f.rule_id === "transaction_abnormal_status");
  assert.equal(finding.confidence, "unknown");
  assert.match(finding.inference, /unknown|ambiguous/i);
});

test("trace retains observed facts and returns partial stdout/exit 9 when lock evidence is unavailable", async t => {
  const f = await fixture(t); f.mode.lockRetry = true;
  const complete = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(complete.code, 0, complete.stderr); validateOutput("transaction.trace", complete.envelope.data);
  assert.equal(complete.envelope.data.branches[0].branch_id, "9007199254740993");
  assert.equal(complete.envelope.data.locks.length, 1);
  assert.equal(complete.envelope.data.completeness.pages_read, 2);
  assert.deepEqual(complete.envelope.data.completeness.read_attempts.map(a => a.path),
    ["/api/v1/console/globalSession/query", "/api/v1/console/globalLock/query", "/api/v1/console/globalLock/query"]);
  assert.deepEqual(complete.envelope.data.completeness.read_attempts.map(a => a.http_status), [200, 503, 200]);
  assert.ok(complete.envelope.data.evidence.every(e =>
    e.completeness.observed_to <= complete.envelope.data.completeness.observed_to));
  assert.deepEqual(complete.envelope.data.timeline.map(e => e.event), ["transaction_begin", "management_observation"]);
  f.mode.lockFailure = true;
  const partial = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(partial.code, 9, partial.stderr); assert.equal(partial.stderr, "");
  assert.equal(partial.envelope.state, "partial");
  assert.equal(partial.envelope.data.transaction.xid, "opaque-1");
  assert.ok(partial.envelope.data.missing_sources.some(s => s.source === "locks"));
  assert.equal(partial.envelope.data.completeness.pages_read, 2);
  assert.deepEqual(partial.envelope.data.completeness.read_attempts.map(a => a.http_status), [200, 403]);
  assert.doesNotMatch(partial.stdout, /SECRET/);
  f.mode.lockStatus = 600;
  const nonstandard = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(nonstandard.code, 9, nonstandard.stderr);
  validateOutput("transaction.trace", nonstandard.envelope.data);
  assert.deepEqual(nonstandard.envelope.data.completeness.read_attempts.map(a => a.http_status), [200, null]);
  assert.ok(nonstandard.envelope.data.missing_sources.some(s => s.reason_code === "management_http_error"));
});

test("trace retains all failed connection attempts without exposing raw network errors", async t => {
  const f = await fixture(t); f.mode.lockReset = true;
  const result = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(result.code, 9, result.stderr);
  validateOutput("transaction.trace", result.envelope.data);
  const complete = result.envelope.data.completeness;
  assert.equal(complete.pages_read, 2);
  assert.equal(complete.read_attempts.length, 4);
  assert.deepEqual(complete.read_attempts.slice(1).map(a => [a.attempt, a.outcome, a.http_status]),
    [[1, "connection_error", null], [2, "connection_error", null], [3, "connection_error", null]]);
  assert.equal(f.requests.filter(r => r.path.includes("globalLock")).length, 3);
  assert.doesNotMatch(result.stdout, /ECONNRESET|Bearer|ANALYSIS_TEST/);
});

test("trace shares its entity budget between the transaction, branches and locks", async t => {
  const f = await fixture(t); f.mode.branchCount = 10000;
  const result = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(result.code, 9, result.stderr);
  const data = result.envelope.data;
  validateOutput("transaction.trace", data);
  assert.equal(data.branches.length, 9999);
  assert.deepEqual(data.locks, []);
  assert.equal(data.completeness.status, "partial");
  assert.equal(data.completeness.limit_reason, "entity_limit");
  assert.ok(data.missing_sources.some(s => s.source === "branches" && s.reason_code === "entity_limit"));
  assert.ok(data.missing_sources.some(s => s.source === "locks" && s.reason_code === "entity_limit"));
  assert.ok(f.requests.every(r => !r.path.includes("globalLock")));
  f.mode.branchCount = 9998; f.mode.lockCount = 2;
  const limited = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(limited.code, 9, limited.stderr);
  assert.equal(limited.envelope.data.branches.length, 9998);
  assert.equal(limited.envelope.data.locks.length, 1);
  assert.equal(limited.envelope.data.completeness.limit_reason, "entity_limit");
  f.mode.lockCount = 1;
  const exact = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(exact.code, 0, exact.stderr);
  assert.equal(exact.envelope.data.completeness.status, "complete");
  assert.equal(1 + exact.envelope.data.branches.length + exact.envelope.data.locks.length, 10000);
});

test("trace stops at a shared hundred-page limit including the transaction lookup", async t => {
  const f = await fixture(t); f.mode.lockCount = 10001;
  const result = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(result.code, 9, result.stderr);
  const data = result.envelope.data;
  validateOutput("transaction.trace", data);
  assert.equal(data.completeness.pages_read, 100);
  assert.equal(data.completeness.limit_reason, "page_limit");
  assert.equal(data.locks.length, 9900);
  assert.equal(f.requests.filter(r => r.path.includes("globalSession")).length, 1);
  assert.equal(f.requests.filter(r => r.path.includes("globalLock")).length, 99);
  assert.equal(data.completeness.read_attempts.length, 100);
});

test("trace bounds retained evidence after DTO expansion", async t => {
  const f = await fixture(t); f.mode.branchCount = 4000; f.mode.branchResourceLength = 640;
  f.mode.lockCount = 5999; f.mode.lockResourceLength = 550;
  const result = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(result.code, 9, result.stderr);
  const data = result.envelope.data;
  validateOutput("transaction.trace", data);
  assert.equal(data.completeness.limit_reason, "evidence_byte_limit");
  assert.equal(data.completeness.pages_read, 61);
  assert.equal(data.branches.length, 4000);
  assert.ok(data.locks.length > 0 && data.locks.length < 5999);
  assert.ok(Buffer.byteLength(canonical(data.evidence)) <= 8 * 1024 * 1024);
  assert.ok(Buffer.byteLength(canonical({ transaction: data.transaction, branches: data.branches,
    locks: data.locks, evidence: data.evidence })) <= 8 * 1024 * 1024);
  assert.deepEqual(data.evidence[0].facts.branches, data.branches);
  assert.deepEqual(data.evidence[1].facts.locks, data.locks);
  assert.equal(data.evidence[1].completeness.status, "truncated");
  for (const record of data.evidence)
    assert.equal(record.id, digest({ command: record.source.command, subject: record.subject,
      facts: record.facts, complete: record.completeness }));
  assert.ok(data.missing_sources.some(s => s.source === "locks" && s.reason_code === "evidence_byte_limit"));
  f.mode.branchResourceLength = 900; f.mode.lockCount = 1;
  const branchLimited = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(branchLimited.code, 9, branchLimited.stderr);
  const trimmed = branchLimited.envelope.data;
  validateOutput("transaction.trace", trimmed);
  assert.ok(trimmed.branches.length > 0 && trimmed.branches.length < 4000);
  assert.equal(trimmed.locks.length, 0);
  assert.equal(trimmed.evidence[0].completeness.limit_reason, "evidence_byte_limit");
  assert.deepEqual(trimmed.evidence[0].facts.branches, trimmed.branches);
  assert.ok(Buffer.byteLength(canonical({ transaction: trimmed.transaction, branches: trimmed.branches,
    locks: trimmed.locks, evidence: trimmed.evidence })) <= 8 * 1024 * 1024);
});

test("trace rejects duplicate branch identities before collecting lock evidence", async t => {
  const f = await fixture(t); f.mode.duplicateBranches = true;
  const result = await f.run(["transaction", "trace", "--xid", "opaque-1"]);
  assert.equal(result.code, 1, result.stderr);
  assert.equal(result.stdout, "");
  assert.equal(result.envelope.ok, false);
  assert.match(result.stderr, /response_contract_invalid/);
  assert.ok(f.requests.every(r => !r.path.includes("globalLock")));
});

test("lock conflict analysis distinguishes multiple observed holders from unobserved waiters", async t => {
  const f = await fixture(t), result = await f.run(["lock", "conflicts"]);
  assert.equal(result.code, 0, result.stderr); validateOutput("lock.conflicts", result.envelope.data);
  assert.equal(result.envelope.data.holders.length, 2);
  assert.equal(result.envelope.data.potential_conflicts.length, 1);
  assert.equal(result.envelope.data.potential_conflicts[0].confirmed_waiter, false);
  assert.deepEqual(result.envelope.data.waiter_evidence, []);
  assert.ok(f.requests.every(r => r.method === "GET"));
});

test("vgroup comparison preserves ambiguity and never equates file observations with effective runtime configuration", () => {
  const group = { id: "group", key: "seata.tx-service-group", value: "orders", source: "file", document: null, redacted: false, unresolved_reason: null };
  const mapping = { ...group, id: "map", key: "seata.service.vgroupMapping.orders", value: "default" };
  const clusters = [{ name: "default", vgroups: ["orders"] }];
  const compare = facts => compareVgroupFacts(facts, clusters)[0];
  assert.equal(compare([group, mapping]).status, "passed");
  assert.equal(compare([group, { ...mapping, value: "other" }]).status, "failed");
  for (const facts of [[group], [group, mapping, { ...mapping, value: "other" }],
    [group, { ...mapping, source: "other" }], [group, { ...mapping, unresolved_reason: "expression_not_evaluated" }]])
    assert.equal(compare(facts).status, "unknown");
  assert.equal(compare([group, mapping]).remote_effective_configuration, "unknown");
  assert.equal(compareVgroupFacts([group, mapping], [])[0].status, "unknown");
});

test("environment diagnosis compares local vgroup mappings against discovered clusters", async t => {
  const f = await fixture(t);
  await writeFile(join(f.root, "application.properties"), "seata.tx-service-group=orders\nseata.service.vgroupMapping.orders=default\n");
  const result = await f.run(["diagnose", "run", "--scope", "environment"]);
  assert.equal(result.code, 0, result.stderr); validateOutput("diagnose.run", result.envelope.data);
  assert.equal(result.envelope.data.completeness.pages_read, 6);
  assert.equal(result.envelope.data.completeness.read_attempts.length, 6);
  const finding = result.envelope.data.findings.find(f => f.rule_id === "vgroup_config_consistency");
  assert.equal(finding.observed_fact.comparisons[0].status, "passed");
  const ids = new Set(result.envelope.data.evidence.map(e => e.id));
  assert.ok(finding.evidence_ids.every(id => ids.has(id)));
  assert.ok(f.requests.every(r => r.method === "GET"));
});

test("diagnostic rule discovery is local and diagnosis separates evidence from unevaluated providers", async t => {
  const f = await fixture(t);
  const listed = await f.run(["diagnose", "rules", "list"], false);
  assert.equal(listed.code, 0, listed.stderr); validateOutput("diagnose.rules.list", listed.envelope.data);
  assert.equal(f.requests.length, 0);
  const rule = await f.run(["diagnose", "rules", "get", "--rule-id", "transaction_abnormal_status"], false);
  assert.equal(rule.code, 0, rule.stderr); assert.equal(f.requests.length, 0);
  const environment = await f.run(["diagnose", "run", "--scope", "environment"]);
  assert.equal(environment.code, 0, environment.stderr); validateOutput("diagnose.run", environment.envelope.data);
  assert.ok(environment.envelope.data.not_evaluated.some(r => r.rule_id === "database_availability" && r.reason_code === "provider_not_implemented"));
  const transaction = await f.run(["diagnose", "run", "--scope", "transaction", "--xid", "opaque-1"]);
  assert.equal(transaction.code, 0, transaction.stderr);
  const data = transaction.envelope.data, ids = new Set(data.evidence.map(e => e.id));
  assert.ok(data.findings.some(f => f.rule_id === "branch_abnormal_status"));
  assert.ok(data.findings.every(f => f.evidence_ids.every(id => ids.has(id))));
  f.mode.lockFailure = true;
  const partial = await f.run(["diagnose", "run", "--scope", "transaction", "--xid", "opaque-1"]);
  assert.equal(partial.code, 9); assert.equal(partial.stderr, "");
  assert.equal(partial.envelope.state, "partial");
  assert.ok(partial.envelope.data.findings.some(f => f.rule_id === "transaction_abnormal_status"));
  assert.ok(f.requests.every(r => r.method === "GET"));
});

test("transaction diagnosis shares the probe and trace page budget and aggregates read attempts", async t => {
  const f = await fixture(t); f.mode.lockCount = 10001;
  f.mode.lockRetry = true;
  const result = await f.run(["diagnose", "run", "--scope", "transaction", "--xid", "opaque-1"]);
  assert.equal(result.code, 9, result.stderr);
  const data = result.envelope.data;
  validateOutput("diagnose.run", data);
  assert.equal(data.completeness.pages_read, 100);
  assert.equal(data.completeness.read_attempts.length, 101);
  assert.equal(data.completeness.read_attempts.filter(a => a.http_status === 503).length, 1);
  assert.equal(f.requests.length, 101);
  assert.equal(f.requests.filter(r => r.path.includes("globalLock")).length, 95);
  const locks = data.evidence.find(e => e.source.command === "lock.list");
  assert.equal(locks.facts.locks.length, 9400);
  assert.equal(locks.completeness.limit_reason, "page_limit");
  assert.equal(data.completeness.status, "partial");
});

test("environment diagnosis retains attempts when the extra cluster read fails", async t => {
  const f = await fixture(t); f.mode.clusterFailureAfterProbe = true;
  const result = await f.run(["diagnose", "run", "--scope", "environment"]);
  assert.equal(result.code, 9, result.stderr);
  validateOutput("diagnose.run", result.envelope.data);
  const data = result.envelope.data;
  assert.equal(data.completeness.pages_read, 6);
  assert.equal(data.completeness.read_attempts.length, 6);
  assert.equal(data.completeness.read_attempts.at(-1).http_status, 403);
  assert.ok(data.not_evaluated.some(r => r.rule_id === "vgroup_config_consistency" && r.reason_code === "permission_denied"));
  assert.equal(f.requests.length, 6);
  assert.doesNotMatch(result.stdout, /SECRET|Bearer/);
});

for (const failure of ["missing", "denied", "retry_exhausted"])
test(`transaction diagnosis names every unevaluated rule and retains attempts: ${failure}`, async t => {
  const f = await fixture(t);
  f.mode.transactionMissing = failure === "missing";
  f.mode.transactionFailure = failure === "denied";
  f.mode.transactionRetryFailure = failure === "retry_exhausted";
  const result = await f.run(["diagnose", "run", "--scope", "transaction", "--xid", "not-observed"]);
  assert.equal(result.code, 9, result.stderr);
  const data = result.envelope.data;
  const missing = new Map(data.not_evaluated.map(rule => [rule.rule_id, rule]));
  const reason = failure === "missing" ? "lookup_incomplete" : failure === "denied" ? "permission_denied" : "management_http_error";
  for (const id of ["transaction_abnormal_status", "branch_abnormal_status", "lock_potential_conflict"])
    assert.equal(missing.get(id)?.reason_code, reason, `${id} must not disappear from the diagnosis`);
  assert.equal(data.completeness.status, "partial");
  assert.equal(data.completeness.pages_read, 6);
  assert.equal(data.completeness.read_attempts.length, failure === "retry_exhausted" ? 8 : 6);
  assert.equal(data.completeness.read_attempts.length, f.requests.length);
  assert.doesNotMatch(result.stdout, /SECRET|Bearer/);
});

test("snapshot binds read sources, reports redaction/accounting and does not create an export", async t => {
  const f = await fixture(t);
  await writeFile(join(f.root, "application.properties"), "seata.enabled=true\ndb.password=SNAPSHOT_SECRET\n");
  const before = await readdir(f.root);
  const result = await f.run(["diagnose", "snapshot", "--xid", "opaque-1", "--include-config"]);
  assert.equal(result.code, 0, result.stderr); validateOutput("diagnose.snapshot", result.envelope.data);
  const snapshot = result.envelope.data.snapshot;
  assert.equal(snapshot.completeness.status, "complete"); assert.equal(snapshot.redaction.applied, true);
  assert.equal(snapshot.sources.find(s => s.id === "console").data.completeness.read_attempts.length, 1);
  assert.equal(snapshot.sources.find(s => s.id === "transactions").data.transactions[0].transaction_id, "9223372036854775807");
  assert.equal(snapshot.sources.find(s => s.id === "transactions").data.branches[0].branch_id, "9007199254740993");
  assert.equal(snapshot.sources.find(s => s.id === "local_config").status, "observed");
  assert.equal(snapshot.sources.find(s => s.id === "logs").status, "not_implemented");
  assert.equal(snapshot.completeness.usage.bytes, snapshot.sources.reduce((sum, s) => sum + s.bytes, 0));
  assert.equal(snapshot.completeness.usage.entities, snapshot.sources.reduce((sum, s) => sum + s.entities, 0));
  for (const source of snapshot.sources.filter(s => s.data !== null)) {
    assert.equal(source.bytes, Buffer.byteLength(canonical(source.data))); assert.equal(source.digest, digest(source.data));
  }
  const ids = new Set(snapshot.sources.map(s => s.id));
  assert.ok(snapshot.findings.every(f => f.source_ids.every(id => ids.has(id))));
  assert.doesNotMatch(result.stdout, /SNAPSHOT_SECRET/);
  assert.ok(f.requests.every(r => r.method === "GET"));
  assert.deepEqual((await readdir(f.root)).filter(n => !n.startsWith("state.db")), before.filter(n => !n.startsWith("state.db")));
});

test("snapshot source failures and deadlines retain earlier evidence and mark uncollected sources honestly", async t => {
  const f = await fixture(t); f.mode.lockFailure = true;
  const failed = await f.run(["diagnose", "snapshot", "--xid", "opaque-1"]);
  assert.equal(failed.code, 9); assert.equal(failed.stderr, "");
  const snapshot = failed.envelope.data.snapshot;
  assert.equal(snapshot.sources.find(s => s.id === "locks").status, "unavailable");
  assert.equal(snapshot.sources.find(s => s.id === "transactions").status, "observed");
  assert.equal(snapshot.sources.find(s => s.id === "local_config").status, "not_requested");
  assert.equal(snapshot.completeness.read_attempts.length, 6);
  assert.equal(snapshot.completeness.read_attempts.at(-1).http_status, 403);
  assert.doesNotMatch(failed.stdout, /SECRET/);
  f.mode.lockFailure = false; f.mode.hangLocks = true;
  const timed = await f.run(["diagnose", "snapshot", "--xid", "opaque-1", "--include-config", "--timeout-ms", "1000"]);
  assert.equal(timed.code, 9, timed.stderr); assert.equal(timed.stderr, "");
  const sources = timed.envelope.data.snapshot.sources;
  // The deadline covers startup/binding and every source, not only the hanging
  // lock request. A loaded host may exhaust it before transactions finish.
  // Deterministic mid-source retention is covered in snapshot-budget.test.mjs.
  const requested = sources.filter(s => !["not_implemented", "not_requested"].includes(s.status));
  const firstExpired = requested.findIndex(s => s.truncation.reason_code === "deadline_exceeded");
  assert.ok(firstExpired >= 0, timed.stdout);
  for (const source of requested.slice(0, firstExpired)) assert.equal(source.status, "observed", timed.stdout);
  assert.ok(["unavailable", "not_collected"].includes(requested[firstExpired].status), timed.stdout);
  for (const source of requested.slice(firstExpired + 1)) {
    assert.equal(source.status, "not_collected", timed.stdout);
    assert.equal(source.truncation.reason_code, "deadline_exceeded", timed.stdout);
  }
  assert.equal(sources.find(s => s.id === "local_config").status, "not_collected");
});

test("route snapshots collect bounded global/lock observations without claiming unrequested branch evidence", async t => {
  const f = await fixture(t); f.mode.manyTransactions = true;
  const result = await f.run(["diagnose", "snapshot"]);
  assert.equal(result.code, 0, result.stderr); validateOutput("diagnose.snapshot", result.envelope.data);
  const snapshot = result.envelope.data.snapshot;
  assert.equal(snapshot.requested_xid, null); assert.equal(snapshot.completeness.scope, "selected_route_snapshot");
  const global = snapshot.sources.find(s => s.id === "transactions");
  assert.equal(global.data.transactions.length, 4); assert.deepEqual(global.data.branches, []);
  assert.deepEqual(global.data.missing_evidence, ["branches_not_requested_without_xid"]);
  assert.equal(snapshot.sources.find(s => s.id === "locks").data.locks.length, 2);
  assert.equal(snapshot.completeness.usage.entities, 9);
  f.mode.emptyRoute = true;
  const missing = await f.run(["diagnose", "snapshot"]);
  assert.equal(missing.code, 9); assert.equal(missing.envelope.data.snapshot.sources.find(s => s.id === "servers").status, "partial");
});

for (const [scoped, failedDiscovery] of [[false, false], [true, false], [false, true]])
test(`snapshot shares its page budget, scoped: ${scoped}, failed discovery: ${failedDiscovery}`, async t => {
  const f = await fixture(t); f.mode.lockCount = 10001;
  f.mode.namingFailure = failedDiscovery;
  const result = await f.run(["diagnose", "snapshot", ...(scoped ? ["--xid", "opaque-1"] : [])]);
  assert.equal(result.code, 9, result.stderr);
  validateOutput("diagnose.snapshot", result.envelope.data);
  const snapshot = result.envelope.data.snapshot;
  const locks = snapshot.sources.find(s => s.id === "locks");
  assert.equal(locks.status, "truncated");
  assert.equal(locks.truncation.reason_code, "page_limit");
  assert.equal(locks.data.locks.length, 9500);
  assert.equal(locks.data.completeness.pages_read, 95);
  assert.equal(f.requests.length, 100);
  assert.equal(f.requests.filter(r => r.path.includes("globalSession")).length, 1);
  assert.equal(f.requests.filter(r => r.path.includes("globalLock")).length, 95);
  assert.equal(snapshot.sources.filter(s => s.data !== null)
    .reduce((sum, s) => sum + (s.data.completeness?.pages_read ?? 0), 0), failedDiscovery ? 99 : 100);
  assert.equal(snapshot.sources.find(s => s.id === "namespaces").status, failedDiscovery ? "unavailable" : "observed");
  assert.equal(snapshot.completeness.status, "partial");
  assert.ok(f.requests.every(r => r.method === "GET"));
});

test("public snapshot export writes verified JSON/ZIP artifacts, rejects overwrite before network and preserves partial evidence", async t => {
  const f = await fixture(t), root = join(f.root, "exports");
  const args = ["diagnose", "snapshot", "export", "--root", root, "--path", "snapshot.json", "--format", "json", "--xid", "opaque-1"];
  const first = await f.run(args);
  assert.equal(first.code, 0, first.stderr); validateOutput("diagnose.snapshot.export", first.envelope.data);
  const artifact = first.envelope.data.export_manifest.artifact, bytes = await readFile(artifact.path);
  assert.equal(artifact.digest, bytesDigest(bytes)); assert.equal(artifact.bytes, bytes.length);
  assert.equal((await stat(artifact.path)).mode & 0o777, 0o600);
  const payload = JSON.parse(bytes.toString());
  assert.equal(payload.manifest.entries[0].digest, bytesDigest(Buffer.from(canonical(payload.snapshot))));
  const count = f.requests.length;
  const duplicate = await f.run(args);
  assert.equal(duplicate.code, 8); assert.equal(f.requests.length, count);
  const unsafe = await f.run(["diagnose", "snapshot", "export", "--root", root, "--path", "../outside.json", "--format", "json"]);
  assert.equal(unsafe.code, 2); assert.equal(f.requests.length, count);
  const overwritten = await f.run([...args, "--overwrite"]);
  assert.equal(overwritten.code, 0, overwritten.stderr);
  assert.notEqual(overwritten.envelope.data.export_manifest.artifact.digest, artifact.digest);
  f.mode.lockFailure = true;
  const partial = await f.run(["diagnose", "snapshot", "export", "--root", root, "--path", "partial.zip", "--format", "zip", "--xid", "opaque-1"]);
  assert.equal(partial.code, 9, partial.stderr); assert.equal(partial.stderr, "");
  validateOutput("diagnose.snapshot.export", partial.envelope.data);
  assert.equal(partial.envelope.data.completeness.status, "partial");
  const zipped = await readFile(partial.envelope.data.export_manifest.artifact.path);
  assert.equal(zipped.readUInt32LE(0), 0x04034b50);
  assert.equal(bytesDigest(zipped), partial.envelope.data.export_manifest.artifact.digest);
  assert.doesNotMatch(zipped.toString(), /SECRET/);
  assert.ok(f.requests.every(r => r.method === "GET"));
});
