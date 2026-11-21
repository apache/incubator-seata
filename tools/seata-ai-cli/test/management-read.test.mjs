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
import { mkdtemp } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { profileConfig } from "../dist/app/profile.js";
import { ManagementReader, transactionDTO, branchDTO } from "../dist/adapter/management/read.js";
import { validateOutput } from "../dist/command/registry.js";
import { CredentialStore } from "../dist/adapter/auth/credentials.js";

// Independent wire fixture, field names verified against GlobalSessionVO,
// BranchSessionVO and PageResult, not generated from the CLI registry/schema.
const xid = "10.0.0.1:8091:9223372036854775807";
const global = { xid, transactionId: "9223372036854775807", status: 5,
  applicationId: "orders", transactionName: "checkout", beginTime: 1700000000123,
  applicationData: "password=NEVER_EMIT", branchSessionVOs: [
    { xid, transactionId: "9223372036854775807", branchId: "9007199254740993",
      branchType: "AT", status: 9, resourceId: "jdbc:mysql://user:SECRET@db/orders",
      applicationData: "NEVER_EMIT", clientId: "opaque" },
  ] };
function page(data, num = 1, size = 20, total = data.length) {
  return { code: "200", message: "success", data, pageNum: num, pageSize: size, total, pages: Math.ceil(total / size) };
}
async function server(t, respond) {
  const requests = [];
  const app = http.createServer(async (req, res) => {
    requests.push({ method: req.method, url: req.url, headers: req.headers });
    const result = await respond(new URL(req.url, "http://localhost"), req);
    res.writeHead(result.status ?? 200, { "content-type": "application/json" });
    res.end(typeof result.body === "string" ? result.body : JSON.stringify(result.body));
  });
  app.listen(0, "127.0.0.1"); await once(app, "listening");
  t.after(() => new Promise(resolve => app.close(resolve)));
  return { endpoint: `http://127.0.0.1:${app.address().port}/env-a`, requests };
}
const reader = endpoint => new ManagementReader({ endpoint, namespace: "public", vgroup: "order_tx_group" }, AbortSignal.timeout(5000));

test("combined reads share a page and source-byte budget without sending after exhaustion", async t => {
  const { endpoint, requests } = await server(t, url => ({ body: page([global],
    Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize"))) }));
  const client = reader(endpoint), budget = { pages: 1, bytes: 8 * 1024 * 1024 };
  await client.collect("transaction", {}, false, () => true, budget);
  const stopped = await client.collect("lock", {}, false, () => true, budget);
  assert.equal(stopped.completeness.pages_read, 0);
  assert.equal(stopped.completeness.limit_reason, "page_limit");
  assert.equal(requests.length, 1);
  const byteBudget = { pages: 2, bytes: Buffer.byteLength(JSON.stringify(global)) };
  await client.collect("transaction", {}, false, () => true, byteBudget);
  const byteStopped = await client.collect("lock", {}, false, () => true, byteBudget);
  assert.equal(byteStopped.completeness.pages_read, 0);
  assert.equal(byteStopped.completeness.limit_reason, "evidence_byte_limit");
  assert.equal(requests.length, 2);
});
async function run(directory, args, input) {
  const child = spawn(process.execPath, [new URL("../dist/cli.js", import.meta.url).pathname, ...args], {
    env: { ...process.env, SEATA_AI_STATE_DIR: directory }, stdio: ["pipe", "pipe", "pipe"],
  });
  let stdout = "", stderr = "";
  child.stdout.on("data", chunk => stdout += chunk); child.stderr.on("data", chunk => stderr += chunk);
  child.stdin.end(input);
  const [code] = await once(child, "close");
  return { code, stdout, stderr };
}
test("real CLI reads pinned NamingServer paths, preserves IDs, redacts and agrees with invoke", async t => {
  const { endpoint, requests } = await server(t, url => {
    assert.equal(url.pathname, "/env-a/api/v1/console/globalSession/query");
    assert.equal(url.searchParams.get("vgroup"), "order_tx_group");
    assert.equal(url.searchParams.has("vGroup"), false);
    return { body: page([global], Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize"))) };
  });
  const directory = await mkdtemp(join(tmpdir(), "seata-read-http-"));
  const store = new StateStore(directory);
  await store.call("profile.create", profileConfig({ name: "p", environment: "development",
    management: { console_endpoint: endpoint, naming_endpoint: endpoint, compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", vgroup: "order_tx_group" }, policy: { storage_policy: "protected_file" } }, true));
  const { flow } = await store.call("flow.create", { name: "p" });
  await store.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  const binding = await store.call("auth.binding", { name: "p" });
  await new CredentialStore(directory, "protected_file", binding.state_store_id)
    .put(staged.credential_ref, "Bearer MANAGEMENT_TEST", AbortSignal.timeout(5000));
  await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref });
  await store.close();
  for (const [command, input, args] of [
    ["transaction.get", { xid }, ["transaction", "get", "--xid", xid]],
    ["branch.get", { xid, branch_id: "9007199254740993" }, ["branch", "get", "--xid", xid, "--branch-id", "9007199254740993"]],
    ["transaction.list", {}, ["transaction", "list"]],
    ["branch.list", { xid }, ["branch", "list", "--xid", xid]],
  ]) {
    const typed = await run(directory, [...args, "--profile", "p"]);
    const invoked = await run(directory, ["invoke"], JSON.stringify({ protocol_version: "0.1", contract_revision: "R9",
      command, context: { profile: "p" }, input, options: {} }));
    for (const result of [typed, invoked]) {
      assert.equal(result.code, 0, result.stderr); assert.equal(result.stderr, "");
      assert.doesNotMatch(result.stdout, /NEVER_EMIT|SECRET|jdbc:mysql/);
      const envelope = JSON.parse(result.stdout); validateOutput(command, envelope.data);
      const object = envelope.data.transaction ?? envelope.data.branch ?? envelope.data.transactions?.[0] ?? envelope.data.branches[0];
      assert.equal(object.transaction_id, "9223372036854775807");
      if (command.startsWith("branch")) assert.equal(object.branch_id, "9007199254740993");
    }
  }
  assert.equal(requests.length, 8);
  assert.ok(requests.every(r => r.method === "GET" && r.headers["x-seata-namespace"] === "public" &&
    !r.headers["x-seata-cluster"] && r.headers.authorization === "Bearer MANAGEMENT_TEST"));
});
test("a retried management page includes each redacted HTTP attempt in completeness", async t => {
  let attempts = 0;
  const { endpoint } = await server(t, url => {
    attempts++;
    return attempts === 1 ? { status: 503, body: {} }
      : { body: page([{ ...global, transactionName: "NAME_SECRET" }],
        Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize"))) };
  });
  const result = await reader(endpoint).page("transaction", { transaction_name: "NAME_SECRET", page: 1, page_size: 20 });
  assert.equal(attempts, 2);
  assert.deepEqual(result.completeness.read_attempts.map(item => item.http_status), [503, 200]);
  assert.equal(result.completeness.read_attempts[0].selector_digest,
    result.completeness.read_attempts[1].selector_digest);
  assert.doesNotMatch(JSON.stringify(result.completeness.read_attempts), /NAME_SECRET|Bearer/);
  validateOutput("transaction.list", { transactions: result.rows.map(row => transactionDTO(row, new Date().toISOString())),
    pagination: result.pagination, completeness: result.completeness });
});
test("bounded multi-page reads retain retries and each page's distinct selector", async t => {
  const rows = Array.from({ length: 101 }, (_, index) => ({ ...global,
    xid: `10.0.0.1:8091:${index + 1}`, transactionId: String(index + 1), branchSessionVOs: [] }));
  let requests = 0;
  const { endpoint } = await server(t, url => {
    requests++;
    if (requests === 1) return { status: 503, body: {} };
    const number = Number(url.searchParams.get("pageNum"));
    return { body: page(rows.slice((number - 1) * 100, number * 100), number, 100, rows.length) };
  });
  const result = await reader(endpoint).collect("transaction", { limit: 10000 });
  assert.equal(result.rows.length, 101);
  assert.equal(result.completeness.status, "complete");
  assert.equal(requests, 3);
  const attempts = result.completeness.read_attempts;
  assert.deepEqual(attempts.map(item => item.http_status), [503, 200, 200]);
  assert.equal(attempts[0].selector_digest, attempts[1].selector_digest);
  assert.notEqual(attempts[1].selector_digest, attempts[2].selector_digest);
});
test("optional read-attempt metadata is validated when present", () => {
  const observed = "2026-09-25T00:00:00.000Z";
  const output = { transactions: [], pagination: { page: 1, page_size: 20, returned: 0, total: 0, has_more: false },
    completeness: { scope: "single_page", status: "complete", consistency: "best_effort", pages_read: 1,
      limit_reason: null, observed_from: observed, observed_to: observed,
      read_attempts: [{ attempt: 0, path: "/api/v1/console/globalSession/query", selector_digest: "sha256:" + "0".repeat(64),
        started_at: observed, observed_at: observed, outcome: "http_response", http_status: 200 }] } };
  assert.throws(() => validateOutput("transaction.list", output), error => error.subtype === "contract_violation");
});
test("read-attempt metadata cannot add raw selectors or credentials", () => {
  const observed = "2026-09-25T00:00:00.000Z";
  const output = { transactions: [], pagination: { page: 1, page_size: 20, returned: 0, total: 0, has_more: false },
    completeness: { scope: "single_page", status: "complete", consistency: "best_effort", pages_read: 1,
      limit_reason: null, observed_from: observed, observed_to: observed,
      read_attempts: [{ attempt: 1, path: "/api/v1/console/globalSession/query", selector_digest: "sha256:" + "0".repeat(64),
        started_at: observed, observed_at: observed, outcome: "http_response", http_status: 200,
        raw_selector: "jdbc:mysql://user:SECRET@host/orders" }] } };
  assert.throws(() => validateOutput("transaction.list", output), error => error.subtype === "contract_violation");
});
test("discovery and lock check retain their distinct wire and business meanings", async t => {
  const { endpoint, requests } = await server(t, url => {
    if (url.pathname.endsWith("/namespace")) return { body: { code: "200", data: { public: { clusters: ["default"], vgroups: ["orders"] } } } };
    if (url.pathname.endsWith("/clusters")) {
      assert.equal(url.searchParams.get("namespace"), "public");
      return { body: [{ clusterName: "default", clusterType: "RAFT", vGroupMapping: ["orders"], unitData: [] }] };
    }
    assert.equal(url.pathname, "/env-a/api/v1/console/globalLock/check");
    assert.equal(url.searchParams.get("branchId"), "9007199254740993");
    return { body: { code: "200", data: false } };
  });
  const client = reader(endpoint);
  validateOutput("namespace.list", await client.namespaces());
  validateOutput("cluster.list", await client.clusters());
  const checked = await client.checkLock(xid, "9007199254740993");
  assert.equal(checked.owner_session_exists, false); assert.equal(checked.lock_exists, undefined);
  validateOutput("lock.check", checked);
  assert.ok(requests.every(r => r.method === "GET"));
});
test("unknown statuses remain unknown, raw numerical IDs decode losslessly", async t => {
  const { endpoint } = await server(t, url => ({ body: JSON.stringify(page([{ ...global, status: 222 }],
    Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize"))))
    .replace('"transactionId":"9223372036854775807"', '"transactionId":9223372036854775807') }));
  const result = await reader(endpoint).transaction(xid);
  const mapped = transactionDTO(result.row, new Date().toISOString());
  assert.equal(mapped.transaction_id, "9223372036854775807");
  assert.deepEqual(mapped.status, { code: 222, name: "unknown", known: false, phase: "unknown", terminal: null, abnormal: null });
});
test("Seata enum code zero remains semantically unknown for global and branch reads", () => {
  const observed = "2026-09-26T00:00:00.000Z";
  const transaction = transactionDTO({ ...global, status: 0 }, observed);
  const branch = branchDTO({ ...global.branchSessionVOs[0], status: 0 }, xid, observed);
  assert.deepEqual(transaction.status, { code: 0, name: "UnKnown", known: false,
    phase: "unknown", terminal: null, abnormal: null });
  assert.deepEqual(branch.status, { code: 0, name: "Unknown", known: false,
    phase: "unknown", terminal: null, abnormal: null });
});
test("File-style contains query cannot turn an exact transaction or branch read into a contract error", async t => {
  const wider = { ...global, xid: "prefix-" + xid, transactionId: "9007199254740995",
    branchSessionVOs: [{ ...global.branchSessionVOs[0], xid: "prefix-" + xid, branchId: "9007199254740996" }] };
  const { endpoint, requests } = await server(t, url => {
    const num = Number(url.searchParams.get("pageNum")), size = Number(url.searchParams.get("pageSize"));
    assert.equal(url.searchParams.get("xid"), xid);
    const rows = [wider, global];
    return { body: page(rows.slice((num - 1) * size, num * size), num, size, rows.length) };
  });
  const client = reader(endpoint);
  const exact = await client.transaction(xid, true);
  assert.equal(exact.row.xid, xid);
  assert.equal(exact.completeness.status, "complete");
  const branches = await client.branches(xid);
  assert.deepEqual(branches.rows.map(row => row.branchId), ["9007199254740993"]);
  const listed = await client.page("transaction", { xid, page: 1, page_size: 20 });
  assert.deepEqual(listed.rows.map(row => row.xid), [xid]);
  assert.equal(listed.pagination.total, 1);
  assert.ok(requests.every(request => request.method === "GET"));
});
test("Redis-style ignored transaction filters are applied exactly across pages before public pagination", async t => {
  const rows = Array.from({ length: 100 }, (_, index) => ({ ...global,
    xid: `10.0.0.1:8091:${index + 1}`, transactionId: String(index + 1), status: index % 2 ? 5 : 1,
    applicationId: "billing", transactionName: "checkout" }));
  rows.push({ ...global, xid: "10.0.0.1:8091:101", transactionId: "101", status: 5,
    applicationId: "orders", transactionName: "checkout" });
  const { endpoint, requests } = await server(t, url => {
    const num = Number(url.searchParams.get("pageNum")), size = Number(url.searchParams.get("pageSize"));
    return { body: page(rows.slice((num - 1) * size, num * size), num, size, rows.length) };
  });
  const result = await reader(endpoint).page("transaction", { application_id: "orders",
    transaction_name: "checkout", status: 5, page: 1, page_size: 20 });
  assert.deepEqual(result.rows.map(row => row.xid), ["10.0.0.1:8091:101"]);
  assert.deepEqual(result.pagination, { page: 1, page_size: 20, returned: 1, total: 1, has_more: false });
  assert.equal(result.completeness.status, "complete");
  assert.equal(requests.length, 2);
  assert.ok(requests.every(request => request.method === "GET"));
});
test("DB-style exact application filter can narrow a dataset larger than the bounded scan", async t => {
  const rows = Array.from({ length: 10000 }, (_, index) => ({ ...global,
    xid: `10.0.0.1:8091:${index + 1}`, transactionId: String(index + 1), applicationId: "billing" }));
  rows.push({ ...global, xid: "10.0.0.1:8091:10001", transactionId: "10001", applicationId: "orders" });
  const { endpoint, requests } = await server(t, url => {
    const selected = url.searchParams.get("applicationId") === "orders" ? rows.slice(-1) : rows;
    const num = Number(url.searchParams.get("pageNum")), size = Number(url.searchParams.get("pageSize"));
    return { body: page(selected.slice((num - 1) * size, num * size), num, size, selected.length) };
  });
  const result = await reader(endpoint).page("transaction", { application_id: "orders" });
  assert.deepEqual(result.rows.map(row => row.xid), ["10.0.0.1:8091:10001"]);
  assert.equal(result.pagination.total, 1);
  assert.equal(result.completeness.status, "complete");
  assert.ok(requests.every(request => request.method === "GET"));
});
test("combined XID and status do not trigger Redis's contradictory count path", async t => {
  const { endpoint, requests } = await server(t, url => {
    const num = Number(url.searchParams.get("pageNum")), size = Number(url.searchParams.get("pageSize"));
    return { body: page([{ ...global, status: 1 }], num, size,
      url.searchParams.has("xid") && url.searchParams.has("status") ? 0 : 1) };
  });
  const result = await reader(endpoint).page("transaction", { xid, status: 5 });
  assert.deepEqual(result.rows, []);
  assert.equal(result.pagination.total, 0);
  assert.equal(result.completeness.status, "complete");
  assert.ok(requests.every(request => {
    const query = new URL(request.url, "http://localhost").searchParams;
    return query.get("xid") === xid && !query.has("status");
  }));
});
test("missing transaction filter evidence cannot produce a complete empty answer", async t => {
  const { endpoint } = await server(t, url => ({ body: page([{ ...global, applicationId: null }],
    Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize"))) }));
  const result = await reader(endpoint).page("transaction", { application_id: "orders" });
  assert.deepEqual(result.rows, []);
  assert.equal(result.pagination.total, null);
  assert.equal(result.completeness.status, "truncated");
  assert.equal(result.completeness.limit_reason, "missing_filter_evidence");
});
test("mismatched identities, inconsistent counts and missing embedded branches never become successful exact lookups", async t => {
  for (const [rows, total] of [[[{ ...global, xid: "wrong" }], 1], [[], 3], [[global], 0], [[], 0]]) {
    const { endpoint } = await server(t, url => ({ body: page(rows,
      Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize")), total) }));
    await assert.rejects(reader(endpoint).transaction(xid), { subtype: "lookup_incomplete" });
  }
  const { endpoint } = await server(t, url => ({ body: page([{ ...global, branchSessionVOs: null }],
    Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize"))) }));
  await assert.rejects(reader(endpoint).branches(xid), { subtype: "response_contract_invalid" });
});
test("empty exact observations retain current-route uncertainty while count drift still fails", async t => {
  const empty = await server(t, url => ({ body: page([],
    Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize")), 0) }));
  const transaction = await reader(empty.endpoint).observeTransaction(xid);
  assert.equal(transaction.row, null);
  assert.equal(transaction.completeness.status, "unknown");
  assert.equal(transaction.completeness.limit_reason, "empty_exact_query_not_absence_proof");
  const branches = await reader(empty.endpoint).observeBranches(xid);
  assert.equal(branches.rows, null);
  assert.equal(branches.completeness.status, "unknown");
  await assert.rejects(reader(empty.endpoint).transaction(xid), { subtype: "lookup_incomplete" });
  await assert.rejects(reader(empty.endpoint).branches(xid), { subtype: "lookup_incomplete" });
  const drift = await server(t, url => ({ body: page([],
    Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize")), 3) }));
  await assert.rejects(reader(drift.endpoint).observeTransaction(xid), { subtype: "lookup_incomplete" });
  await assert.rejects(reader(drift.endpoint).observeBranches(xid), { subtype: "lookup_incomplete" });
});
test("HTTP auth and structured business errors are sanitized, non-retried failures", async t => {
  for (const [status, body, type] of [[401, "<html>SECRET</html>", "authentication"], [403, "", "authorization"], [200, { code: "500", message: "SECRET" }, "api"]]) {
    const { endpoint, requests } = await server(t, () => ({ status, body }));
    await assert.rejects(reader(endpoint).namespaces(), error => error.type === type && !error.message.includes("SECRET"));
    assert.equal(requests.length, 1);
  }
});
test("public management reads refuse to send any HTTP request without an active local credential", async t => {
  const { endpoint, requests } = await server(t, url => url.pathname.endsWith("/namespace")
    ? { body: { code: "200", data: { public: { clusters: ["default"], vgroups: [] } } } }
    : { body: page([global], 1, 2) });
  const directory = await mkdtemp(join(tmpdir(), "seata-read-login-required-"));
  const store = new StateStore(directory);
  try {
    await store.call("profile.create", profileConfig({ name: "p", environment: "development",
      management: { console_endpoint: endpoint, naming_endpoint: endpoint, compatibility_id: "seata-src-41694e14" },
      target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  } finally { await store.close(); }
  for (const args of [["topology", "namespace", "list"], ["transaction", "get", "--xid", xid]]) {
    const result = await run(directory, [...args, "--profile", "p"]);
    assert.equal(result.code, 3, result.stdout || result.stderr);
    assert.equal(result.stdout, "");
    assert.equal(JSON.parse(result.stderr).error.subtype, "authentication_required");
  }
  const connectivity = await run(directory, ["environment", "connectivity", "check", "--profile", "p"]);
  assert.equal(connectivity.code, 0, connectivity.stderr);
  assert.equal(JSON.parse(connectivity.stdout).data.checks[0].reason_code, "authentication_required");
  for (const args of [["diagnose", "run", "--scope", "environment"],
    ["diagnose", "run", "--scope", "transaction", "--xid", xid]]) {
    const diagnosis = await run(directory, [...args, "--profile", "p"]);
    assert.equal(diagnosis.code, 9, diagnosis.stderr);
    const data = JSON.parse(diagnosis.stdout).data;
    assert.equal(data.completeness.pages_read, 0);
    assert.equal(data.completeness.read_attempts, undefined);
    assert.equal(data.findings.find(f => f.rule_id === "management_connectivity")
      .observed_fact.checks[0].reason_code, "authentication_required");
  }
  assert.equal(requests.length, 0, "Anonymous server reads must not bypass local login");
});

test("nonempty short pages cannot prove a complete result or absence when total promises more rows", async t => {
  const { endpoint, requests } = await server(t, url => ({ body: page([global],
    Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize")), 2) }));
  const one = await reader(endpoint).page("transaction", { page: 1, page_size: 20 });
  assert.equal(one.rows.length, 1);
  assert.equal(one.completeness.status, "unknown"); assert.equal(one.pagination.has_more, null);
  const collected = await reader(endpoint).collect("transaction", {});
  assert.equal(collected.rows.length, 1);
  assert.equal(collected.completeness.status, "truncated");
  assert.equal(collected.completeness.limit_reason, "count_or_identity_drift");
  assert.equal(requests.length, 2, "Do not advance past a page known to have missing evidence");
});

test("time queries enforce millisecond [start,end) locally without assuming store-specific SQL bounds", async t => {
  const { endpoint, requests } = await server(t, url => {
    assert.equal(url.searchParams.has("timeStart"), false);
    assert.equal(url.searchParams.has("timeEnd"), false);
    return { body: page([122, 123, 124, 125].map((ms, i) => ({ ...global, xid: `opaque-${i}`, beginTime: 1700000000000 + ms })), 1, 100) };
  });
  const result = await reader(endpoint).page("transaction", { time_range: {
    start: "2023-11-14T22:13:20.123Z", end: "2023-11-14T22:13:20.125Z" }, page_size: 1 });
  assert.equal(result.rows[0].xid, "opaque-1");
  assert.equal(result.pagination.total, 2);
  assert.equal(result.pagination.has_more, true);
  assert.equal(result.completeness.consistency, "best_effort");
  assert.equal(requests.length, 1);
});

test("time-query count drift and missing timestamps cannot produce complete evidence", async t => {
  const { endpoint } = await server(t, () => ({ body: page([{ ...global, beginTime: null }], 1, 100) }));
  const result = await reader(endpoint).page("transaction", { time_range: {
    start: "2023-11-14T22:13:20Z", end: "2023-11-14T22:13:21Z" } });
  assert.equal(result.completeness.status, "truncated");
  assert.equal(result.completeness.limit_reason, "missing_time_evidence");
  assert.equal(result.pagination.total, null);
  assert.equal(result.pagination.has_more, null);
});

test("Redis-style lock queries forward an XID or complete row identity before local filtering", async t => {
  const lock = { xid: "x-1", transactionId: "1", branchId: "11",
    resourceId: "jdbc:mysql://example/orders", tableName: "orders", pk: "42", rowKey: "orders:42" };
  const { endpoint, requests } = await server(t, url => {
    const query = url.searchParams;
    if (query.get("xid") !== lock.xid && !(query.get("resourceId") === lock.resourceId &&
      query.get("tableName") === lock.tableName && query.get("pk") === lock.pk))
      return { body: { code: "0412", message: "scoped query required", data: null, total: 0 } };
    return { body: page([lock], Number(query.get("pageNum")), Number(query.get("pageSize")), 1) };
  });
  const byXid = await reader(endpoint).page("lock", { xid: lock.xid, branch_id: "11" });
  assert.deepEqual(byXid.rows.map(row => row.rowKey), [lock.rowKey]);
  assert.equal(byXid.completeness.status, "complete");
  const byRow = await reader(endpoint).page("lock", { resource_id: lock.resourceId,
    table_name: lock.tableName, pk: lock.pk });
  assert.deepEqual(byRow.rows.map(row => row.rowKey), [lock.rowKey]);
  assert.equal(byRow.completeness.status, "complete");
  assert.equal(requests.length, 2);
});

test("a scoped Redis lock response containing all rows can be paged locally", async t => {
  const rows = Array.from({ length: 120 }, (_, index) => ({ xid: "x-1", transactionId: "1",
    branchId: String(index + 1), resourceId: "jdbc:mysql://example/orders", tableName: "orders",
    pk: String(index + 1), rowKey: `orders:${index + 1}` }));
  const { endpoint, requests } = await server(t, url => {
    assert.equal(url.searchParams.get("xid"), "x-1");
    return { body: page(rows, Number(url.searchParams.get("pageNum")),
      Number(url.searchParams.get("pageSize")), rows.length) };
  });
  const result = await reader(endpoint).page("lock", { xid: "x-1", page: 2, page_size: 20 });
  assert.deepEqual(result.rows.map(row => row.branchId), Array.from({ length: 20 }, (_, index) => String(index + 21)));
  assert.deepEqual(result.pagination, { page: 2, page_size: 20, returned: 20, total: 120, has_more: true });
  assert.equal(result.completeness.status, "complete");
  assert.equal(requests.length, 1);
});

test("lock filters ignored by the DB API are applied across pages before public pagination", async t => {
  const locks = [
    { xid: "x-1", transactionId: "1", branchId: "11", resourceId: "other", tableName: "orders", pk: "42", rowKey: "other-1" },
    { xid: "x-2", transactionId: "2", branchId: "22", resourceId: "target", tableName: "orders", pk: "42", rowKey: "match-1" },
    { xid: "x-3", transactionId: "3", branchId: "33", resourceId: "target", tableName: "orders", pk: "43", rowKey: "other-2" },
    { xid: "x-4", transactionId: "4", branchId: "44", resourceId: "target", tableName: "orders", pk: "42", rowKey: "match-2" },
  ];
  const { endpoint, requests } = await server(t, url => ({ body: page(locks,
    Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize"))) }));
  const first = await reader(endpoint).page("lock", { resource_id: "target", pk: "42", page: 1, page_size: 1 });
  assert.deepEqual(first.rows.map(row => row.rowKey), ["match-1"]);
  assert.equal(first.pagination.total, 2);
  assert.equal(first.pagination.has_more, true);
  const second = await reader(endpoint).page("lock", { resource_id: "target", pk: "42", page: 2, page_size: 1 });
  assert.deepEqual(second.rows.map(row => row.rowKey), ["match-2"]);
  assert.equal(second.pagination.total, 2);
  assert.equal(second.pagination.has_more, false);
  assert.equal(second.completeness.status, "complete");
  assert.ok(requests.every(request => !request.url.includes("resourceId=") && !request.url.includes("pk=")));
});

test("server discovery keeps vGroup distinct, treats addresses only as data and omits metadata", async t => {
  const node = { control: { host: "192.0.2.1", port: 7091 }, transaction: { host: "192.0.2.1", port: 8091 },
    healthy: true, version: "2.5.0", role: "LEADER", metadata: { secret: "NEVER_EMIT" }, internal: { host: "SECRET" } };
  const unit = { unitName: "unit-1", namingInstanceList: [node] };
  const { endpoint, requests } = await server(t, url => {
    if (url.pathname.endsWith("/discovery")) {
      assert.equal(url.searchParams.get("vGroup"), "order_tx_group");
      assert.equal(url.searchParams.has("vgroup"), false);
      return { body: { term: 1, clusterList: [{ clusterName: "default", clusterType: "RAFT", unitData: [unit] }] } };
    }
    assert.equal(url.pathname, "/env-a/api/v1/naming/clusterData");
    assert.equal(url.searchParams.get("clusterName"), "default");
    return { body: { code: "200", data: { clusterName: "default", clusterType: "RAFT", unitData: { "unit-1": unit } } } };
  });
  for (const client of [reader(endpoint), new ManagementReader({ endpoint, namespace: "public", cluster: "default" }, AbortSignal.timeout(5000))]) {
    const result = await client.servers(); validateOutput("server.list", result);
    assert.equal(result.servers[0].control_endpoint.host, "192.0.2.1");
    assert.deepEqual(result.observed_versions, ["2.5.0"]);
    assert.doesNotMatch(JSON.stringify(result), /NEVER_EMIT|SECRET/);
  }
  assert.equal(requests.length, 2);
});

test("only the active private credential is sent and logout fences subsequent CLI reads", async t => {
  const token = "Bearer SYNTHETIC_TEST_TOKEN";
  const { endpoint, requests } = await server(t, (url, req) => req.headers.authorization === token
    ? { body: page([global], Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize"))) }
    : { status: 401, body: "" });
  const directory = await mkdtemp(join(tmpdir(), "seata-authenticated-read-"));
  const store = new StateStore(directory); t.after(() => store.close());
  await store.call("profile.create", profileConfig({ name: "p", environment: "development",
    management: { console_endpoint: endpoint, naming_endpoint: endpoint, compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  const { flow } = await store.call("flow.create", { name: "p" });
  await store.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  const binding = await store.call("auth.binding", { name: "p" });
  const secrets = new CredentialStore(directory, "protected_file", binding.state_store_id);
  await secrets.put(staged.credential_ref, token, AbortSignal.timeout(5000));
  const args = ["transaction", "get", "--profile", "p", "--xid", xid];
  assert.equal((await run(directory, args)).code, 3);
  await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref });
  const authenticated = await run(directory, args);
  assert.equal(authenticated.code, 0, authenticated.stderr);
  assert.doesNotMatch(authenticated.stdout, /SYNTHETIC_TEST_TOKEN|credential_ref/);
  const logout = await run(directory, ["auth", "logout", "--profile", "p"]);
  assert.equal(logout.code, 0, logout.stderr);
  assert.equal(JSON.parse(logout.stdout).data.credential_cleanup, "complete");
  await assert.rejects(secrets.get(staged.credential_ref, AbortSignal.timeout(5000)));
  assert.equal((await run(directory, args)).code, 3);
  assert.deepEqual(requests.map(r => r.headers.authorization), [token]);
});

test("logout during collection fences both subsequent reads and transport retries", async t => {
  for (const retry of [false, true]) {
    const directory = await mkdtemp(join(tmpdir(), "seata-read-fence-"));
    const store = new StateStore(directory); t.after(() => store.close());
    let calls = 0;
    const { endpoint, requests } = await server(t, async url => {
      calls++;
      if (calls === 1) await store.call("auth.logout", { name: "p" });
      return retry ? { status: 503, body: {} }
        : { body: page([global], Number(url.searchParams.get("pageNum")), Number(url.searchParams.get("pageSize"))) };
    });
    await store.call("profile.create", profileConfig({ name: "p", environment: "development",
      management: { console_endpoint: endpoint, naming_endpoint: endpoint, compatibility_id: "seata-src-41694e14" },
      target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
    const { flow } = await store.call("flow.create", { name: "p" });
    await store.call("flow.submit", { flow_id: flow.flow_id });
    const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
    const binding = await store.call("auth.binding", { name: "p" });
    await new CredentialStore(directory, "protected_file", binding.state_store_id)
      .put(staged.credential_ref, "Bearer FENCED_TEST", AbortSignal.timeout(5000));
    await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref });
    const result = await run(directory, ["transaction", retry ? "get" : "trace", "--profile", "p", "--xid", xid]);
    assert.equal(result.code, retry ? 3 : 9, result.stderr);
    assert.equal(requests.length, 1, "No subsequent request may send the cached token after observed logout");
    assert.equal(requests[0].headers.authorization, "Bearer FENCED_TEST");
    assert.match(result.stdout || result.stderr, /credential_changed/);
    assert.doesNotMatch(result.stdout + result.stderr, /FENCED_TEST/);
  }
});

test("auth status does not infer token validity from an anonymous-readable endpoint and invalidates a 401 credential", async t => {
  let rejected = false;
  const { endpoint, requests } = await server(t, url => {
    assert.equal(url.pathname, "/env-a/api/v1/naming/namespace");
    return rejected ? { status: 401, body: "" } : { body: { code: "200", data: { public: { clusters: [], vgroups: [] } } } };
  });
  const directory = await mkdtemp(join(tmpdir(), "seata-auth-verify-")), store = new StateStore(directory);
  t.after(() => store.close());
  await store.call("profile.create", profileConfig({ name: "p", environment: "development",
    management: { console_endpoint: endpoint, naming_endpoint: endpoint, compatibility_id: "seata-src-41694e14" },
    target: { namespace: "public", cluster: "default" }, policy: { storage_policy: "protected_file" } }, true));
  const { flow } = await store.call("flow.create", { name: "p" });
  await store.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await store.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  const binding = await store.call("auth.binding", { name: "p" });
  await new CredentialStore(directory, "protected_file", binding.state_store_id).put(staged.credential_ref, "Bearer VERIFY_TEST", AbortSignal.timeout(5000));
  await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref });
  const args = ["auth", "status", "--profile", "p", "--verify"];
  const valid = await run(directory, args);
  assert.equal(valid.code, 0, valid.stderr);
  assert.equal(JSON.parse(valid.stdout).data.verification, "unavailable");
  assert.equal(JSON.parse(valid.stdout).data.reason_code, "token_validity_unproven_by_read");
  assert.equal(JSON.parse(valid.stdout).data.probe_result, "read_succeeded");
  rejected = true;
  const invalid = await run(directory, args);
  assert.equal(invalid.code, 0, invalid.stderr);
  assert.equal(JSON.parse(invalid.stdout).data.verification, "invalid");
  assert.equal(JSON.parse(invalid.stdout).data.authenticated, false);
  assert.equal((await store.call("auth.binding", { name: "p" })).credential, null);
  const again = await run(directory, args);
  assert.equal(JSON.parse(again.stdout).data.verification, "not_authenticated");
  assert.equal(requests.length, 2);
});
