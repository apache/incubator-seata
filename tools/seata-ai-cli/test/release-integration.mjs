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
import { spawn } from "node:child_process";
import { randomUUID } from "node:crypto";
import { once } from "node:events";
import { mkdtemp, readFile, stat } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, isAbsolute } from "node:path";
import { pathToFileURL } from "node:url";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { CredentialStore } from "../dist/adapter/auth/credentials.js";
import { reconciliationAdapterDigest } from "../dist/app/reconcile.js";
import { digest } from "../dist/protocol/value.js";

for (const name of ["SEATA_TEST_ENDPOINT", "SEATA_TEST_USER", "SEATA_TEST_PASSWORD", "SEATA_TEST_PLAYWRIGHT"])
  assert.ok(process.env[name], `Set ${name} for the isolated release integration run`);
assert.ok(process.env.SEATA_TEST_ROUTE === undefined || ["cluster", "vgroup"].includes(process.env.SEATA_TEST_ROUTE));
assert.ok(process.env.SEATA_TEST_STORAGE_POLICY === undefined ||
  ["protected_file", "keyring_required"].includes(process.env.SEATA_TEST_STORAGE_POLICY));
assert.ok(process.env.SEATA_TEST_INJECT_FAILURE_AFTER_AUTH === undefined ||
  process.env.SEATA_TEST_INJECT_FAILURE_AFTER_AUTH === "1");
if (process.env.SEATA_TEST_COMPLETED_XID)
  assert.ok(process.env.SEATA_TEST_COMPLETED_BRANCH_ID, "Set the completed transaction's branch ID");
if (process.env.SEATA_TEST_ACTIVE_XID)
  assert.ok(process.env.SEATA_TEST_ACTIVE_BRANCH_ID, "Set the active transaction's branch ID from independent SQL");
const activeLockCount = Number(process.env.SEATA_TEST_ACTIVE_LOCK_COUNT ?? 1);
assert.ok(Number.isSafeInteger(activeLockCount) && activeLockCount >= 1 && activeLockCount <= 120,
  "SEATA_TEST_ACTIVE_LOCK_COUNT must be 1..120");
const { chromium } = await import(pathToFileURL(process.env.SEATA_TEST_PLAYWRIGHT).href);
const cliPath = process.env.SEATA_TEST_CLI_PATH ?? new URL("../dist/cli.js", import.meta.url).pathname;
assert.ok(isAbsolute(cliPath), "SEATA_TEST_CLI_PATH must be an absolute installed CLI entrypoint");

async function cli(root, args, allowPartial = false) {
  const child = spawn(process.execPath, [cliPath, ...args], {
    env: { ...process.env, SEATA_AI_STATE_DIR: root, SEATA_AI_LOCAL_AUTH: "shared_loopback" },
    stdio: ["ignore", "pipe", "pipe"],
  });
  let stdout = "", stderr = "";
  child.stdout.setEncoding("utf8"); child.stderr.setEncoding("utf8");
  child.stdout.on("data", chunk => stdout += chunk);
  child.stderr.on("data", chunk => stderr += chunk);
  const [code] = await once(child, "close");
  assert.equal(stderr, "", `CLI stderr for ${args.slice(0, 2).join(" ")}`);
  const envelope = JSON.parse(stdout);
  assert.ok(allowPartial ? code === 0 || code === 9 : code === 0,
    JSON.stringify({ code, error: envelope.error ?? null }));
  if (code === 9) {
    assert.equal(envelope.ok, false);
    assert.equal(envelope.state, "partial");
    assert.equal(envelope.error.type, "partial_failure");
  } else assert.equal(envelope.ok, true, JSON.stringify(envelope.error ?? {}));
  assert.ok(!stdout.includes(process.env.SEATA_TEST_PASSWORD));
  const exportIndex = args.indexOf("--export");
  if (exportIndex !== -1) {
    const path = args[exportIndex + 1];
    assert.equal(await readFile(path, "utf8"), stdout);
    assert.equal((await stat(path)).mode & 0o777, 0o600);
  }
  return envelope.data;
}

async function cliFailure(root, args, expectedCode, expectedSubtype) {
  const child = spawn(process.execPath, [cliPath, ...args], {
    env: { ...process.env, SEATA_AI_STATE_DIR: root, SEATA_AI_LOCAL_AUTH: "shared_loopback" },
    stdio: ["ignore", "pipe", "pipe"],
  });
  let stdout = "", stderr = "";
  child.stdout.setEncoding("utf8"); child.stderr.setEncoding("utf8");
  child.stdout.on("data", chunk => stdout += chunk);
  child.stderr.on("data", chunk => stderr += chunk);
  const [code] = await once(child, "close");
  assert.equal(code, expectedCode);
  assert.equal(stdout, "");
  const envelope = JSON.parse(stderr);
  assert.equal(envelope.ok, false);
  assert.equal(envelope.error.subtype, expectedSubtype);
  assert.ok(!stderr.includes(process.env.SEATA_TEST_PASSWORD));
  return envelope;
}

function registerCredentialCleanup(t, root, name) {
  t.after(async () => {
    const store = new StateStore(root);
    let binding;
    try { binding = await store.call("auth.binding", { name }); }
    finally { await store.close(); }
    const result = await cli(root, ["auth", "logout", "--profile", name]);
    assert.equal(result.authenticated, false);
    assert.equal(result.credential_cleanup, "complete");
    if (binding.credential !== null) {
      const policy = binding.credential.provider === "keyring" ? "keyring_required" : "protected_file";
      const secrets = new CredentialStore(root, policy, binding.state_store_id);
      await assert.rejects(secrets.get(binding.credential.credential_ref, AbortSignal.timeout(5000)),
        { subtype: "credential_store_unavailable" });
    }
    t.diagnostic("Local test credential cleanup completed");
  });
}

test("released Console business rejection does not activate a credential", { timeout: 45000 }, async t => {
  const root = await mkdtemp(join(tmpdir(), "seata-r11-release-rejected-"));
  const name = "release-rejected-login";
  const wrongPassword = "SEATA_R11_INCORRECT_TEST_PASSWORD";
  await cli(root, ["profile", "create", "--config", JSON.stringify({
    name, environment: "development",
    management: { console_endpoint: process.env.SEATA_TEST_ENDPOINT,
      naming_endpoint: process.env.SEATA_TEST_ENDPOINT, compatibility_id: "seata-src-41694e14",
      ...(process.env.SEATA_TEST_CA_REF ? { ca_ref: process.env.SEATA_TEST_CA_REF } : {}) },
    target: { namespace: "public", cluster: "default" },
    policy: { storage_policy: process.env.SEATA_TEST_STORAGE_POLICY ?? "protected_file" },
  }), "--accept-target"]);
  registerCredentialCleanup(t, root, name);
  const pending = await cli(root, ["auth", "login", "--profile", name]);
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage();
    assert.equal((await page.goto(pending.verification_url)).status(), 200);
    await page.locator('input[name="username"]').fill(process.env.SEATA_TEST_USER);
    await page.locator('input[name="password"]').fill(wrongPassword);
    const [response] = await Promise.all([
      page.waitForResponse(r => r.request().method() === "POST" && r.url().endsWith("/submit")),
      page.getByRole("button", { name: "Sign in" }).click(),
    ]);
    assert.equal(response.status(), 400);
    assert.ok(!(await response.text()).includes(wrongPassword));
  } finally { await browser.close(); }
  const flow = await cli(root, ["auth", "login", "status", "--profile", name, "--flow-id", pending.flow_id]);
  assert.equal(flow.flow_state, "failed");
  assert.equal(flow.reason.subtype, "login_rejected");
  const store = new StateStore(root);
  try { assert.equal((await store.call("auth.binding", { name })).credential, null); }
  finally { await store.close(); }
  await cliFailure(root, ["transaction", "list", "--profile", name], 3, "authentication_required");
  t.diagnostic("Published Console rejected wrong password with HTTP 200/business code 401; no credential activated");
});

test("released Seata accepts browser login and serves real read commands", { timeout: 90000 }, async t => {
  const root = await mkdtemp(join(tmpdir(), "seata-r11-release-"));
  const name = "release-integration";
  t.diagnostic(`StateStore: ${root}`);
  t.diagnostic(`CLI entrypoint: ${cliPath}`);
  const profile = {
    name, environment: "development",
    management: { console_endpoint: process.env.SEATA_TEST_ENDPOINT,
      naming_endpoint: process.env.SEATA_TEST_ENDPOINT, compatibility_id: "seata-src-41694e14",
      ...(process.env.SEATA_TEST_CA_REF ? { ca_ref: process.env.SEATA_TEST_CA_REF } : {}) },
    target: process.env.SEATA_TEST_ROUTE === "vgroup"
      ? { namespace: "public", vgroup: "default_tx_group" }
      : { namespace: "public", cluster: process.env.SEATA_TEST_CLUSTER ?? "default" },
    policy: { storage_policy: process.env.SEATA_TEST_STORAGE_POLICY ?? "protected_file" },
  };
  await cli(root, ["profile", "create", "--config", JSON.stringify(profile), "--accept-target"]);
  registerCredentialCleanup(t, root, name);
  const pending = await cli(root, ["auth", "login", "--profile", name]);
  assert.equal(pending.flow_state, "pending");
  const browser = await chromium.launch({ headless: true });
  try {
    const page = await browser.newPage();
    assert.equal((await page.goto(pending.verification_url)).status(), 200);
    await page.locator('input[name="username"]').fill(process.env.SEATA_TEST_USER);
    await page.locator('input[name="password"]').fill(process.env.SEATA_TEST_PASSWORD);
    const [response] = await Promise.all([
      page.waitForResponse(r => r.request().method() === "POST" && r.url().endsWith("/submit")),
      page.getByRole("button", { name: "Sign in" }).click(),
    ]);
    assert.equal(response.status(), 200, await response.text());
    assert.ok(!JSON.stringify(await response.allHeaders()).includes(process.env.SEATA_TEST_PASSWORD));
  } finally { await browser.close(); }
  const auth = await cli(root, ["auth", "status", "--profile", name, "--verify"]);
  assert.equal(auth.authenticated, true);
  assert.equal(auth.verification, "unavailable");
  assert.equal(auth.probe_result, "read_succeeded");
  const authStore = new StateStore(root);
  try {
    const binding = await authStore.call("auth.binding", { name });
    assert.equal(binding.credential?.provider,
      process.env.SEATA_TEST_STORAGE_POLICY === "keyring_required" ? "keyring" : "protected_file");
  } finally { await authStore.close(); }
  if (process.env.SEATA_TEST_INJECT_FAILURE_AFTER_AUTH === "1")
    throw new Error("injected_after_auth");
  const results = {};
  for (const [key, args] of Object.entries({
    namespaces: ["topology", "namespace", "list"],
    clusters: ["topology", "cluster", "list"],
    servers: ["topology", "server", "list"],
    transactions: ["transaction", "list"],
    locks: process.env.SEATA_TEST_STORE_MODE === "redis"
      ? ["lock", "list", "--xid", process.env.SEATA_TEST_ACTIVE_XID ??
        process.env.SEATA_TEST_COMPLETED_XID ?? "unobserved-test-xid"]
      : ["lock", "list"],
  })) results[key] = await cli(root, [...args, "--profile", name]);
  assert.ok(Array.isArray(results.namespaces.namespaces));
  assert.ok(Array.isArray(results.clusters.clusters));
  assert.ok(Array.isArray(results.servers.servers));
  assert.ok(results.servers.servers.length > 0);
  assert.ok(results.servers.servers.every(row => row.cluster === (process.env.SEATA_TEST_CLUSTER ?? "default")));
  assert.ok(Array.isArray(results.transactions.transactions));
  assert.ok(Array.isArray(results.locks.locks));
  for (const result of Object.values(results)) {
    assert.ok(Array.isArray(result.completeness.read_attempts));
    assert.ok(result.completeness.read_attempts.every(item => item.outcome === "http_response" && item.http_status === 200));
    assert.ok(!JSON.stringify(result.completeness.read_attempts).includes(process.env.SEATA_TEST_PASSWORD));
  }
  const connectivity = await cli(root, ["environment", "connectivity", "check", "--profile", name], true);
  assert.ok(Array.isArray(connectivity.checks));
  assert.ok(connectivity.checks.filter(check => check.status === "available")
    .every(check => Array.isArray(check.completeness.read_attempts)));
  if (process.env.SEATA_TEST_STORE_MODE !== "redis") {
    const conflicts = await cli(root, ["lock", "conflicts", "--profile", name], true);
    assert.ok(Array.isArray(conflicts.holders));
  }
  if (process.env.SEATA_TEST_SEEDED === "1") {
    const xid = "10.21.76.114:18091:9007199254740993";
    const branchId = "9007199254740994";
    assert.ok(results.transactions.transactions.some(row => row.xid === xid));
    assert.ok(results.locks.locks.some(row => row.row_key === "seata-r11-e2e-order-42"));
    const selectedTransactions = await cli(root, ["transaction", "list", "--profile", name,
      "--application-id", "seata-r11-e2e", "--transaction-name", "release-e2e", "--status", "1"]);
    assert.deepEqual(selectedTransactions.transactions.map(row => row.xid), [xid]);
    assert.equal(selectedTransactions.pagination.total, 1);
    assert.equal(selectedTransactions.completeness.status, "complete");
    const missingTransactions = await cli(root, ["transaction", "list", "--profile", name,
      "--application-id", "not-the-application"]);
    assert.deepEqual(missingTransactions.transactions, []);
    assert.equal(missingTransactions.pagination.total, 0);
    assert.equal(missingTransactions.completeness.status, "complete");
    const missingLock = await cli(root, ["lock", "list", "--profile", name,
      "--resource-id", "jdbc:mysql://not-present.invalid/missing", "--pk", "42"]);
    assert.deepEqual(missingLock.locks, []);
    assert.equal(missingLock.pagination.total, 0);
    assert.equal(missingLock.completeness.status, "complete");
    const selectedLock = await cli(root, ["lock", "list", "--profile", name,
      "--resource-id", "jdbc:mysql://127.0.0.1:13306/seata", "--pk", "42"]);
    assert.deepEqual(selectedLock.locks.map(row => row.row_key), ["seata-r11-e2e-order-42"]);
    assert.equal(selectedLock.pagination.total, 1);
    const abnormal = await cli(root, ["transaction", "abnormal", "--profile", name]);
    assert.deepEqual(abnormal.transactions, []);
    assert.equal(abnormal.completeness.status, "complete");
    const exact = await cli(root, ["transaction", "get", "--profile", name, "--xid", xid,
      "--export", join(root, "transaction-evidence.json")]);
    assert.equal(exact.transaction.xid, xid);
    assert.equal(exact.transaction.transaction_id, "9007199254740993");
    assert.equal(exact.transaction.status.code, 1);
    const branches = await cli(root, ["branch", "list", "--profile", name, "--xid", xid]);
    assert.equal(branches.branches.length, 1);
    const branch = await cli(root, ["branch", "get", "--profile", name, "--xid", xid, "--branch-id", branchId]);
    assert.equal(branch.branch.branch_id, branchId);
    assert.equal(branch.branch.branch_type, "AT");
    assert.equal(branch.branch.status.code, 1);
    const owner = await cli(root, ["lock", "check", "--profile", name, "--xid", xid, "--branch-id", branchId]);
    assert.equal(owner.owner_session_exists, true);
    const trace = await cli(root, ["transaction", "trace", "--profile", name, "--xid", xid], true);
    assert.equal(trace.transaction.xid, xid);
    const snapshot = await cli(root, ["diagnose", "snapshot", "--profile", name, "--xid", xid], true);
    assert.ok(snapshot.snapshot);
    assert.ok(!JSON.stringify({ results, exact, branches, branch }).includes("SYNTHETIC_DB_SECRET_DO_NOT_EMIT"));
    assert.ok(!JSON.stringify({ results, exact, branches, branch }).includes("SYNTHETIC_BRANCH_SECRET_DO_NOT_EMIT"));
  }
  if (process.env.SEATA_TEST_ACTIVE_XID) {
    const xid = process.env.SEATA_TEST_ACTIVE_XID;
    const branchId = process.env.SEATA_TEST_ACTIVE_BRANCH_ID;
    assert.ok(results.transactions.transactions.some(row => row.xid === xid));
    assert.ok(results.locks.locks.some(row => row.xid === xid));
    const exact = await cli(root, ["transaction", "get", "--profile", name, "--xid", xid]);
    assert.equal(exact.transaction.xid, xid);
    assert.equal(exact.transaction.status.code, 1);
    if (process.env.SEATA_TEST_STORE_MODE === "file") {
      for (const [flag, partial] of [["--application-id", "seata-r11"],
        ["--transaction-name", "r11-real"], ["--xid", xid.slice(0, -1)]]) {
        const filtered = await cli(root, ["transaction", "list", "--profile", name, flag, partial]);
        assert.deepEqual(filtered.transactions, [], `File-mode contains match must not satisfy ${flag}`);
        assert.equal(filtered.pagination.total, 0);
        assert.equal(filtered.completeness.status, "complete");
      }
      const selected = await cli(root, ["transaction", "list", "--profile", name,
        "--application-id", "seata-r11-e2e", "--transaction-name", "r11-real-at"]);
      assert.deepEqual(selected.transactions.map(row => row.xid), [xid]);
      assert.equal(selected.pagination.total, 1);
      assert.equal(selected.completeness.status, "complete");
    }
    if (process.env.SEATA_TEST_STORE_MODE === "redis") {
      for (const [flag, value] of [["--application-id", "not-the-application"],
        ["--transaction-name", "not-the-transaction"]]) {
        const filtered = await cli(root, ["transaction", "list", "--profile", name, flag, value]);
        assert.deepEqual(filtered.transactions, []);
        assert.equal(filtered.pagination.total, 0);
        assert.equal(filtered.completeness.status, "complete");
      }
      const mismatch = await cli(root, ["transaction", "list", "--profile", name,
        "--xid", xid, "--status", "5"]);
      assert.deepEqual(mismatch.transactions, []);
      assert.equal(mismatch.pagination.total, 0);
      assert.equal(mismatch.completeness.status, "complete");
      const selected = await cli(root, ["transaction", "list", "--profile", name,
        "--application-id", "seata-r11-e2e", "--transaction-name", "r11-real-at"]);
      assert.deepEqual(selected.transactions.map(row => row.xid), [xid]);
      assert.equal(selected.pagination.total, 1);
    }
    const branches = await cli(root, ["branch", "list", "--profile", name, "--xid", xid]);
    assert.ok(branches.branches.some(row => row.branch_id === branchId));
    const branch = await cli(root, ["branch", "get", "--profile", name, "--xid", xid, "--branch-id", branchId]);
    assert.equal(branch.branch.branch_id, branchId);
    assert.equal(branch.branch.branch_type, "AT");
    assert.equal(branch.branch.status.code, 1);
    const locks = await cli(root, ["lock", "list", "--profile", name, "--xid", xid, "--branch-id", branchId]);
    assert.equal(locks.completeness.status, "complete");
    assert.equal(locks.pagination.total, activeLockCount);
    assert.equal(locks.locks.length, Math.min(20, activeLockCount));
    assert.ok(locks.locks.every(row => row.xid === xid && row.branch_id === branchId));
    if (activeLockCount > 20) {
      assert.equal(locks.pagination.has_more, true);
      const lastPage = await cli(root, ["lock", "list", "--profile", name, "--xid", xid,
        "--branch-id", branchId, "--page", String(Math.ceil(activeLockCount / 20))]);
      assert.equal(lastPage.completeness.status, "complete");
      assert.equal(lastPage.pagination.total, activeLockCount);
      assert.equal(lastPage.pagination.has_more, false);
      assert.equal(lastPage.locks.length, activeLockCount % 20 || 20);
      assert.ok(lastPage.locks.every(row => row.xid === xid && row.branch_id === branchId));
    }
    if (process.env.SEATA_TEST_STORE_MODE === "redis") {
      const rowSelector = ["--resource-id", "jdbc:mysql://127.0.0.1:13306/seata_r11_business",
        "--table-name", "orders"];
      const matchingRow = await cli(root, ["lock", "list", "--profile", name, ...rowSelector,
        "--pk", activeLockCount === 1 ? "1001" : "2001"]);
      assert.deepEqual(matchingRow.locks.map(row => row.xid), [xid]);
      assert.equal(matchingRow.completeness.status, "complete");
      const missingRow = await cli(root, ["lock", "list", "--profile", name,
        ...rowSelector, "--pk", "not-present"]);
      assert.deepEqual(missingRow.locks, []);
      assert.equal(missingRow.pagination.total, 0);
      assert.equal(missingRow.completeness.status, "complete");
    }
    const owner = await cli(root, ["lock", "check", "--profile", name, "--xid", xid, "--branch-id", branchId]);
    assert.equal(owner.owner_session_exists, true);
  }
  if (process.env.SEATA_TEST_COMPLETED_XID) {
    const xid = process.env.SEATA_TEST_COMPLETED_XID;
    assert.ok(!results.transactions.transactions.some(row => row.xid === xid));
    assert.ok(!results.locks.locks.some(row => row.xid === xid));
    await cliFailure(root, ["transaction", "get", "--profile", name, "--xid", xid], 1, "lookup_incomplete");
    const diagnosis = await cli(root, ["diagnose", "run", "--profile", name,
      "--scope", "transaction", "--xid", xid], true);
    assert.equal(diagnosis.completeness.status, "partial");
    for (const rule of ["transaction_abnormal_status", "branch_abnormal_status", "lock_potential_conflict"])
      assert.ok(diagnosis.not_evaluated.some(item => item.rule_id === rule && item.reason_code === "lookup_incomplete"));
    const owner = await cli(root, ["lock", "check", "--profile", name, "--xid", xid,
      "--branch-id", process.env.SEATA_TEST_COMPLETED_BRANCH_ID]);
    assert.equal(owner.owner_session_exists, false);
    assert.equal(owner.lock_exists, undefined);
    const store = new StateStore(root), request = randomUUID();
    try {
      const binding = await store.call("auth.binding", { name }), p = binding.profile;
      const intent = { xid };
      await store.call("execution.claim", { client_request_id: request, command: "transaction.retry.pause",
        intent, intent_digest: digest(intent), profile_id: p.profile_id,
        target_digest: binding.target_digest, target: { profile_id: p.profile_id, profile_revision: p.revision,
          console_base_endpoint: p.management.console_endpoint, naming_base_endpoint: p.management.naming_endpoint,
          namespace: "public", ...profile.target.vgroup ? { vgroup: profile.target.vgroup } : { cluster: profile.target.cluster },
          trust_digest: binding.trust_digest, route_assurance: "selector_only",
          adapter_id: "seata-src-41694e14", adapter_digest: reconciliationAdapterDigest, command_schema_version: "0.1" } });
    } finally { await store.close(); }
    const historyBefore = await cli(root, ["execution", "get", "--client-request-id", request]);
    const reconciled = await cli(root, ["execution", "reconcile", "--profile", name, "--client-request-id", request,
      "--export", join(root, "reconciliation-evidence.json")]);
    assert.equal(reconciled.observation, "unknown");
    assert.equal(reconciled.completeness.status, "unknown");
    assert.equal(reconciled.current_evidence[0].facts.transaction, null);
    assert.equal(reconciled.current_evidence[0].facts.absence_meaning, "not_observed_in_current_route");
    assert.equal(reconciled.execution.outcome, "not_started");
    assert.equal(reconciled.replay_allowed, false);
    const history = await cli(root, ["execution", "get", "--client-request-id", request]);
    assert.deepEqual(history, historyBefore);
    assert.equal(history.events.some(event => event.kind === "READ_ONLY_OBSERVATION"), false);
  }
  t.diagnostic(JSON.stringify({ namespace_count: results.namespaces.namespaces.length,
    cluster_count: results.clusters.clusters.length, server_count: results.servers.servers.length,
    transaction_count: results.transactions.transactions.length, lock_count: results.locks.locks.length,
    observed_versions: results.servers.observed_versions }));
});
