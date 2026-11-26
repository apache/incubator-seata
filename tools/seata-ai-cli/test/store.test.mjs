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
import { mkdtemp, chmod, symlink, readFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomUUID } from "node:crypto";
import { StateStore } from "../dist/adapter/statestore/client.js";
import { profileConfig, endpoint } from "../dist/app/profile.js";
import { canonical, digest } from "../dist/protocol/value.js";
import {
  commitSendPermit,
  sendWithPermit,
  requestDigest,
} from "../dist/safety/send.js";
import http from "node:http";
import { once } from "node:events";
import { spawnSync } from "node:child_process";
import { DatabaseSync } from "node:sqlite";

const config = (name) =>
  profileConfig(
    {
      name,
      environment: "development",
      management: {
        console_endpoint: "http://127.0.0.1:8080/env-a",
        naming_endpoint: "http://127.0.0.1:8081/env-a",
        compatibility_id: "seata-src-41694e14",
      },
      target: { namespace: "public", cluster: "default" },
    },
    true,
  );
async function fixture(t) {
  const path = await mkdtemp(join(tmpdir(), "seata-r11-store-"));
  const a = new StateStore(path),
    b = new StateStore(path);
  t.after(async () => {
    await a.close();
    await b.close();
  });
  const result = await a.call("profile.create", config("p"));
  return { a, b, path, profile: result.profile };
}
async function credential(store) {
  const { flow } = await store.call("flow.create", { name: "p" });
  await store.call("flow.submit", { flow_id: flow.flow_id });
  const { credential_ref } = await store.call("flow.stage", {
    flow_id: flow.flow_id,
    provider: "keyring",
  });
  await store.call("flow.activate", { flow_id: flow.flow_id, credential_ref });
  return credential_ref;
}

test("expired and abandoned login flows cannot activate staged credentials or revive on heartbeat", async t => {
  const { a, path } = await fixture(t);
  const db = new DatabaseSync(join(path, "state.db"));
  t.after(() => db.close());
  for (const scenario of ["expired", "abandoned"]) {
    const { flow } = await a.call("flow.create", { name: "p" });
    await a.call("flow.submit", { flow_id: flow.flow_id });
    const staged = await a.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
    // Move persisted timestamps, not the production clock, to exercise recovery
    // after elapsed wall time without a five-minute sleep or a runtime test hook.
    const past = new Date(Date.now() - 60000).toISOString();
    if (scenario === "expired") db.prepare("UPDATE auth_flows SET expires_at=? WHERE flow_id=?").run(past, flow.flow_id);
    else db.prepare("UPDATE auth_flows SET heartbeat_at=? WHERE flow_id=?").run(past, flow.flow_id);
    const status = await a.call("flow.status", { name: "p", flow_id: flow.flow_id });
    assert.equal(status.flow_state, scenario === "expired" ? "expired" : "failed");
    assert.equal(status.reason.subtype, scenario === "expired" ? "auth_flow_expired" : "helper_unavailable");
    assert.ok(status.terminal_at);
    assert.equal(db.prepare("SELECT status FROM credential_metadata WHERE credential_ref=?").get(staged.credential_ref).status, "cleanup_pending");
    await assert.rejects(a.call("flow.activate", { flow_id: flow.flow_id, credential_ref: staged.credential_ref }), { subtype: "state_changed" });
    await assert.rejects(a.call("flow.submit", { flow_id: flow.flow_id }), { subtype: "state_changed" });
    assert.equal((await a.call("flow.heartbeat", { flow_id: flow.flow_id, helper_instance_id: flow.helper_instance_id })).active, false);
    assert.equal((await a.call("auth.binding", { name: "p" })).credential, null);
    assert.deepEqual(await a.call("flow.status", { name: "p", flow_id: flow.flow_id }), status);
  }
});

test("terminal flow history expires independently of the current authenticated credential", async t => {
  const { a, path } = await fixture(t);
  const { flow } = await a.call("flow.create", { name: "p" });
  await a.call("flow.cancel", { name: "p", flow_id: flow.flow_id });
  const active = await credential(a);
  const db = new DatabaseSync(join(path, "state.db"));
  t.after(() => db.close());
  db.prepare("UPDATE auth_flows SET terminal_at=? WHERE flow_id=?")
    .run(new Date(Date.now() - 23 * 3600000).toISOString(), flow.flow_id);
  assert.equal((await a.call("flow.status", { name: "p", flow_id: flow.flow_id })).flow_state, "cancelled");
  db.prepare("UPDATE auth_flows SET terminal_at=? WHERE flow_id=?")
    .run(new Date(Date.now() - 25 * 3600000).toISOString(), flow.flow_id);
  await assert.rejects(a.call("flow.status", { name: "p", flow_id: flow.flow_id }), { subtype: "auth_flow_not_found" });
  assert.equal((await a.call("auth.binding", { name: "p" })).credential.credential_ref, active);
});

test("a flow first observed after its 24-hour expiry retention is not revived by a late read", async t => {
  const { a, path } = await fixture(t);
  const { flow } = await a.call("flow.create", { name: "p" });
  const db = new DatabaseSync(join(path, "state.db"));
  t.after(() => db.close());
  const expiredAt = new Date(Date.now() - 25 * 3600000).toISOString();
  db.prepare("UPDATE auth_flows SET expires_at=?,heartbeat_at=? WHERE flow_id=?")
    .run(expiredAt, expiredAt, flow.flow_id);
  await assert.rejects(a.call("flow.status", { name: "p", flow_id: flow.flow_id }), { subtype: "auth_flow_not_found" });
  assert.equal(db.prepare("SELECT count(*) AS n FROM auth_flows WHERE flow_id=?").get(flow.flow_id).n, 0);
  await assert.rejects(a.call("flow.cancel", { name: "p", flow_id: flow.flow_id }), { subtype: "auth_flow_not_found" });
});

test("an abandoned helper's retention starts at the missed heartbeat deadline", async t => {
  const { a, path } = await fixture(t);
  const { flow } = await a.call("flow.create", { name: "p" });
  const db = new DatabaseSync(join(path, "state.db"));
  t.after(() => db.close());
  const heartbeatAt = new Date(Date.now() - 60000).toISOString();
  db.prepare("UPDATE auth_flows SET heartbeat_at=? WHERE flow_id=?").run(heartbeatAt, flow.flow_id);
  const status = await a.call("flow.status", { name: "p", flow_id: flow.flow_id });
  assert.equal(status.flow_state, "failed");
  assert.equal(status.reason.subtype, "helper_unavailable");
  assert.equal(status.terminal_at, new Date(Date.parse(heartbeatAt) + 10000).toISOString());
});

test("expired flow history stays hidden while its credential binding remains active", async t => {
  const { a, path } = await fixture(t);
  const reference = await credential(a);
  const db = new DatabaseSync(join(path, "state.db"));
  t.after(() => db.close());
  const flowId = db.prepare("SELECT flow_id FROM credential_metadata WHERE credential_ref=?").get(reference).flow_id;
  db.prepare("UPDATE auth_flows SET terminal_at=? WHERE flow_id=?")
    .run(new Date(Date.now() - 25 * 3600000).toISOString(), flowId);
  await assert.rejects(a.call("flow.status", { name: "p", flow_id: flowId }), { subtype: "auth_flow_not_found" });
  await assert.rejects(a.call("flow.cancel", { name: "p", flow_id: flowId }), { subtype: "auth_flow_not_found" });
  assert.equal(db.prepare("SELECT count(*) AS n FROM auth_flows WHERE flow_id=?").get(flowId).n, 1);
  assert.equal((await a.call("auth.binding", { name: "p" })).credential.credential_ref, reference);
});

test("a late helper failure cannot restart retention for an already expired flow", async t => {
  const { a, path } = await fixture(t);
  const { flow } = await a.call("flow.create", { name: "p" });
  const db = new DatabaseSync(join(path, "state.db"));
  t.after(() => db.close());
  const expiredAt = new Date(Date.now() - 25 * 3600000).toISOString();
  db.prepare("UPDATE auth_flows SET expires_at=?,heartbeat_at=? WHERE flow_id=?")
    .run(expiredAt, expiredAt, flow.flow_id);
  await a.call("flow.fail", { flow_id: flow.flow_id, helper_instance_id: flow.helper_instance_id, reason: "login_rejected" });
  await assert.rejects(a.call("flow.status", { name: "p", flow_id: flow.flow_id }), { subtype: "auth_flow_not_found" });
  assert.equal(db.prepare("SELECT count(*) AS n FROM auth_flows WHERE flow_id=?").get(flow.flow_id).n, 0);
});

test("a failed flow queues its staged credential for immediate cleanup", async t => {
  const { a, path } = await fixture(t);
  const { flow } = await a.call("flow.create", { name: "p" });
  await a.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await a.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" });
  await a.call("flow.fail", {
    flow_id: flow.flow_id,
    helper_instance_id: flow.helper_instance_id,
    reason: "credential_store_unavailable",
  });
  const db = new DatabaseSync(join(path, "state.db"));
  t.after(() => db.close());
  assert.equal(db.prepare("SELECT status FROM credential_metadata WHERE credential_ref=?")
    .get(staged.credential_ref).status, "cleanup_pending");
  assert.ok((await a.call("credential.cleanup.list")).credentials
    .some(item => item.credential_ref === staged.credential_ref));
});

test("expired flow cleanup commits even when a late submission is rejected", async t => {
  const { a, path } = await fixture(t);
  const { flow } = await a.call("flow.create", { name: "p" });
  const db = new DatabaseSync(join(path, "state.db"));
  t.after(() => db.close());
  const expiredAt = new Date(Date.now() - 25 * 3600000).toISOString();
  db.prepare("UPDATE auth_flows SET expires_at=?,heartbeat_at=? WHERE flow_id=?")
    .run(expiredAt, expiredAt, flow.flow_id);
  await assert.rejects(a.call("flow.submit", { flow_id: flow.flow_id }), { subtype: "state_changed" });
  assert.equal(db.prepare("SELECT count(*) AS n FROM auth_flows WHERE flow_id=?").get(flow.flow_id).n, 0);
});

test("staging rejects elapsed expiry or abandoned helper without a preceding status query", async t => {
  const { a, path } = await fixture(t);
  const db = new DatabaseSync(join(path, "state.db"));
  t.after(() => db.close());
  for (const scenario of ["expired", "abandoned"]) {
    const { flow } = await a.call("flow.create", { name: "p" });
    await a.call("flow.submit", { flow_id: flow.flow_id });
    const past = new Date(Date.now() - 60000).toISOString();
    if (scenario === "expired") db.prepare("UPDATE auth_flows SET expires_at=? WHERE flow_id=?").run(past, flow.flow_id);
    else db.prepare("UPDATE auth_flows SET heartbeat_at=? WHERE flow_id=?").run(past, flow.flow_id);
    await assert.rejects(a.call("flow.stage", { flow_id: flow.flow_id, provider: "protected_file" }), { subtype: "state_changed" });
    assert.equal(db.prepare("SELECT count(*) AS n FROM credential_metadata WHERE flow_id=?").get(flow.flow_id).n, 0);
    assert.equal((await a.call("flow.status", { name: "p", flow_id: flow.flow_id })).flow_state,
      scenario === "expired" ? "expired" : "failed");
  }
});
const claimArgs = (p) => ({
  client_request_id: randomUUID(),
  command: "transaction.retry.pause",
  intent: { xid: "opaque" },
  intent_digest: digest({ xid: "opaque" }),
  target: { profile_id: p.profile_id },
  target_digest: config("p").target_digest,
  profile_id: p.profile_id,
});
const planFor = (claim) => ({
  plan_id: randomUUID(),
  request_digest: digest({ fixture: "no-network" }),
  material_digest: digest({ status: 4 }),
  created_at: new Date().toISOString(),
  expires_at: new Date(Date.now() + 120000).toISOString(),
  target_digest: claim.target_digest,
});

test("WAL/FULL startup, profile CAS and stable state-store identity", async (t) => {
  const { a, b, profile } = await fixture(t);
  const [first, second] = await Promise.all([a.call("info"), b.call("info")]);
  assert.equal(first.state_store_id, second.state_store_id);
  assert.equal(first.journal_mode, "wal");
  assert.equal(first.synchronous, 2);
  const changes = { ...config("p"), name: "p", expected_revision: 1 };
  const results = await Promise.allSettled([
    a.call("profile.update", changes),
    b.call("profile.update", changes),
  ]);
  assert.equal(results.filter((r) => r.status === "fulfilled").length, 1);
  assert.equal(
    (await a.call("profile.get", { name: "p" })).profile.profile_id,
    profile.profile_id,
  );
});
test("duplicate IDs across workers, changed intent, OS lifetime exclusion", async (t) => {
  const { a, b, profile } = await fixture(t);
  const claim = claimArgs(profile);
  const results = await Promise.all([
    a.call("execution.claim", claim),
    b.call("execution.claim", {
      ...claim,
      client_request_id: claim.client_request_id.toUpperCase(),
    }),
  ]);
  assert.equal(results.filter((r) => r.created).length, 1);
  const id = results[0].execution.execution_id;
  await assert.rejects(
    b.call("execution.claim", {
      ...claim,
      intent_digest: digest({ xid: "other" }),
    }),
    (e) => e.subtype === "request_id_conflict",
  );
  await a.call("execution.lock", { execution_id: id });
  await assert.rejects(
    b.call("execution.recover", { execution_id: id }),
    (e) => e.subtype === "store_busy",
  );
  await a.call("execution.unlock", { execution_id: id });
  const recovered = await b.call("execution.recover", { execution_id: id });
  assert.equal(recovered.execution.state, "FINAL");
  assert.equal(recovered.execution.outcome, "not_started");
  assert.equal(recovered.send_permit, false);
});
test("SEND_INTENT consumes plan atomically; restart cannot recreate send permission", async (t) => {
  const { a, b, profile, path } = await fixture(t);
  const reference = await credential(a);
  const claim = claimArgs(profile);
  const e = (await a.call("execution.claim", claim)).execution;
  await a.call("execution.lock", { execution_id: e.execution_id });
  const plan = planFor(claim);
  const prepared = (
    await a.call("execution.prepare", { execution_id: e.execution_id, plan })
  ).execution;
  const send = {
    execution_id: e.execution_id,
    record_version: prepared.record_version,
    intent_digest: claim.intent_digest,
    material_digest: plan.material_digest,
    request_digest: plan.request_digest,
    auth_generation: 0,
    profile_revision: 1,
    trust_digest: config("p").trust_digest,
    credential_ref: reference,
  };
  await assert.rejects(
    a.call("execution.sendIntent", { ...send, auth_generation: 99 }),
  );
  assert.equal(
    (await a.call("execution.get", { execution_id: e.execution_id })).execution
      .state,
    "PREPARED",
  );
  const committed = await a.call("execution.sendIntent", send);
  assert.equal(committed.execution.state, "SEND_INTENT");
  assert.equal(committed.execution.outcome, "unknown");
  const observed = await b.call("execution.observe", {
    execution_id: e.execution_id,
    expected_revision: committed.execution.record_version,
    target_digest: claim.target_digest,
    observation: { observation: "postcondition_observed", causality: "unproven", replay_allowed: false,
      current_evidence: [{ source: "synthetic_read", status: "desired_condition_currently_observed" }] },
  });
  const afterObservation = await b.call("execution.get", { execution_id: e.execution_id });
  assert.equal(afterObservation.execution.record_version, observed.record_version);
  assert.equal(afterObservation.execution.state, "SEND_INTENT");
  assert.equal(afterObservation.execution.outcome, "unknown");
  assert.equal(afterObservation.execution.observation, committed.execution.observation);
  assert.equal(afterObservation.events.at(-1).kind, "READ_ONLY_OBSERVATION");
  assert.equal(afterObservation.events.at(-1).data.observation, "postcondition_observed");
  await assert.rejects(a.call("execution.sendIntent", send));
  await a.close();
  const recovered = await b.call("execution.recover", {
    execution_id: e.execution_id,
  });
  assert.equal(recovered.execution.outcome, "unknown");
  assert.equal(recovered.send_permit, false);
  const bytes = await readFile(join(path, "state.db"));
  assert.equal(bytes.includes(Buffer.from("Bearer ")), false);
});
test("read-only observations enforce revision and target binding without changing historical execution facts", async t => {
  const { a, b, profile } = await fixture(t);
  const claim = claimArgs(profile), e = (await a.call("execution.claim", claim)).execution;
  const args = { execution_id: e.execution_id, expected_revision: e.record_version, target_digest: claim.target_digest,
    observation: { observation: "unknown", causality: "unproven", replay_allowed: false, current_evidence: [] } };
  await assert.rejects(a.call("execution.observe", { ...args, target_digest: "different" }), e => e.subtype === "execution_target_mismatch");
  for (const change of [{ causality: "proven" }, { replay_allowed: true }, { observation: "applied" }, { payload: "x".repeat(8 * 1024 * 1024) }])
    await assert.rejects(a.call("execution.observe", { ...args, observation: { ...args.observation, ...change } }));
  const results = await Promise.allSettled([a.call("execution.observe", args), b.call("execution.observe", args)]);
  assert.equal(results.filter(r => r.status === "fulfilled").length, 1);
  const saved = await a.call("execution.get", { execution_id: e.execution_id });
  assert.equal(saved.events.length, 2);
  assert.equal(saved.execution.state, "CREATED");
  assert.equal(saved.execution.outcome, "not_started");
  assert.equal(saved.execution.request_phase, "not_started");
  assert.equal(saved.execution.send_intent_at, null);
  assert.equal(saved.recovery.replay_allowed, false);
});

test("read-only observation journal enforces an exact UTF-8 eight-MiB boundary", async t => {
  const { a, profile } = await fixture(t);
  const claim = claimArgs(profile), e = (await a.call("execution.claim", claim)).execution;
  const observation = { observation: "unknown", causality: "unproven", replay_allowed: false,
    current_evidence: [{ payload: "" }] };
  const remaining = 8 * 1024 * 1024 - Buffer.byteLength(canonical(observation));
  observation.current_evidence[0].payload = "é".repeat(Math.floor(remaining / 2)) + "x".repeat(remaining % 2);
  assert.equal(Buffer.byteLength(canonical(observation)), 8 * 1024 * 1024);
  const args = { execution_id: e.execution_id, expected_revision: e.record_version,
    target_digest: claim.target_digest, observation };
  await assert.rejects(a.call("execution.observe", { ...args,
    observation: { ...observation, current_evidence: [{ payload: observation.current_evidence[0].payload + "x" }] } }),
    { subtype: "invalid_observation" });
  assert.equal((await a.call("execution.get", { execution_id: e.execution_id })).events.length, 1);
  await a.call("execution.observe", args);
  const saved = await a.call("execution.get", { execution_id: e.execution_id });
  assert.equal(saved.events.length, 2);
  assert.deepEqual(saved.events.at(-1).data, observation);
  assert.equal(saved.execution.outcome, "not_started");
  assert.equal(saved.recovery.replay_allowed, false);
});

test("Skills installation journal holds a process lock and preserves incomplete stage identity across reopen", async t => {
  const { a, b } = await fixture(t);
  const record = { operation_id: randomUUID(), version: "0.1.0-r11.0", phase: "preparing",
    content_digest: digest({ bundle: "synthetic" }), installation_path: "/synthetic/user-skills", files: [] };
  await assert.rejects(a.call("skills.install.record", { expected_revision: 0, record }), e => e.subtype === "skills_install_not_owned");
  assert.equal((await a.call("skills.install.lock")).record, null);
  await assert.rejects(b.call("skills.install.lock"));
  let saved = (await a.call("skills.install.record", { expected_revision: 0, record })).record;
  await assert.rejects(a.call("skills.install.record", { expected_revision: saved.revision, record: { ...record, phase: "complete" } }), e => e.subtype === "skills_install_phase");
  await assert.rejects(a.call("skills.install.record", { expected_revision: saved.revision, record: { ...record, installation_path: "/other" } }), e => e.subtype === "skills_install_changed");
  await assert.rejects(a.call("skills.install.record", { expected_revision: saved.revision, record: { ...record, previous_files: [{}] } }), e => e.subtype === "skills_install_changed");
  await a.close();
  assert.equal((await b.call("skills.install.lock")).record.phase, "preparing");
  await assert.rejects(b.call("skills.install.record", { expected_revision: saved.revision, record: { ...record, operation_id: randomUUID() } }), e => e.subtype === "skills_install_unresolved");
  for (const phase of ["writing", "verifying", "complete"])
    saved = (await b.call("skills.install.record", { expected_revision: saved.revision, record: { ...record, phase } })).record;
  const final = await b.call("skills.install.get");
  assert.equal(final.record.phase, "complete"); assert.equal(final.record.operation_id, record.operation_id);
  assert.equal(final.record.installation_path, "/synthetic/user-skills");
  assert.equal(final.record.canonical_path, undefined); assert.equal(final.record.exposure_path, undefined);
  await assert.rejects(b.call("skills.install.record", { expected_revision: 0, record }), e => e.subtype === "skills_install_changed");
  await assert.rejects(b.call("skills.install.record", { expected_revision: saved.revision, record }), e => e.subtype === "skills_install_phase");
});

test("logout fences staged activation; auth flow remains queryable after cancel", async (t) => {
  const { a, b } = await fixture(t);
  const { flow } = await a.call("flow.create", { name: "p" });
  assert.equal((await b.call("flow.create", { name: "p" })).created, false);
  await a.call("flow.submit", { flow_id: flow.flow_id });
  await assert.rejects(b.call("flow.submit", { flow_id: flow.flow_id }));
  const staged = await a.call("flow.stage", {
    flow_id: flow.flow_id,
    provider: "keyring",
  });
  await b.call("auth.logout", { name: "p" });
  await assert.rejects(
    a.call("flow.activate", {
      flow_id: flow.flow_id,
      credential_ref: staged.credential_ref,
    }),
    (e) => e.subtype === "state_changed",
  );
  assert.equal(
    (await a.call("auth.status", { name: "p" })).authenticated,
    false,
  );
  assert.equal(
    (await a.call("flow.cancel", { name: "p", flow_id: flow.flow_id }))
      .flow_state,
    "cancelled",
  );
});
test("unsafe directory and symlink rejected without rewriting permissions", async () => {
  const path = await mkdtemp(join(tmpdir(), "seata-r11-unsafe-"));
  await chmod(path, 0o777);
  const a = new StateStore(path);
  try {
    await assert.rejects(a.call("info"), (e) => e.type === "policy");
    await assert.rejects(a.call("info"), (e) => e.type === "policy");
  } finally {
    await a.close();
  }
  const link = path + "-link";
  await symlink(path, link);
  const b = new StateStore(link);
  try {
    await assert.rejects(b.call("info"), (e) => e.type === "policy");
  } finally {
    await b.close();
  }
});
test("endpoint trust rejects redirects disguised as prefixes and production HTTP", () => {
  for (const url of [
    "https://u:p@host",
    "https://host/a/../b",
    "https://host/%2e%2e/a",
    "https://host/a%2fb",
    "https://host?q=1",
    "https://host/#x",
    "https://host\\a",
  ])
    assert.throws(() => endpoint(url, "production", true));
  assert.throws(() => endpoint("http://127.0.0.1:8080", "production", true));
  assert.equal(
    endpoint("HTTPS://EXAMPLE.COM:443/env-a/", "production", true),
    "https://example.com/env-a",
  );
});

test("profile rename preserves selected identity when the old name is reused", async (t) => {
  const { a, profile } = await fixture(t);
  await a.call("profile.use", { name: "p" });
  await a.call("profile.update", {
    ...config("new"),
    name: "p",
    expected_revision: 1,
  });
  await a.call("profile.create", config("p"));
  const list = await a.call("profile.list");
  assert.equal(list.current_profile, "new");
  assert.equal(
    list.profiles.find((p) => p.name === "new").profile_id,
    profile.profile_id,
  );
});
test("cancel pending and submitting flows rejects late stage/activation without logout", async (t) => {
  const { a } = await fixture(t);
  const pending = (await a.call("flow.create", { name: "p" })).flow;
  assert.equal(
    (await a.call("flow.cancel", { name: "p", flow_id: pending.flow_id }))
      .changed,
    true,
  );
  await assert.rejects(
    a.call("flow.submit", { flow_id: pending.flow_id }),
    (e) => e.subtype === "state_changed",
  );
  const flow = (await a.call("flow.create", { name: "p" })).flow;
  await a.call("flow.submit", { flow_id: flow.flow_id });
  const staged = await a.call("flow.stage", {
    flow_id: flow.flow_id,
    provider: "keyring",
  });
  assert.equal(
    (await a.call("flow.cancel", { name: "p", flow_id: flow.flow_id })).changed,
    true,
  );
  await assert.rejects(
    a.call("flow.activate", {
      flow_id: flow.flow_id,
      credential_ref: staged.credential_ref,
    }),
    (e) => e.subtype === "state_changed",
  );
  const next = (await a.call("flow.create", { name: "p" })).flow;
  await a.call("flow.submit", { flow_id: next.flow_id });
  await assert.rejects(
    a.call("flow.activate", {
      flow_id: next.flow_id,
      credential_ref: staged.credential_ref,
    }),
    (e) => e.subtype === "state_changed",
  );
  assert.equal(
    (await a.call("auth.status", { name: "p" })).authenticated,
    false,
  );
});
test("two request IDs competing for one plan yield one send permit and one HTTP write", async (t) => {
  const { a, b, profile } = await fixture(t);
  const reference = await credential(a);
  let count = 0;
  const server = http.createServer((_req, res) => {
    count++;
    res.end("{}");
  });
  server.listen(0, "127.0.0.1");
  await once(server, "listening");
  t.after(() => {
    server.closeAllConnections();
    server.close();
  });
  const request = {
    endpoint: "http://127.0.0.1:" + server.address().port,
    suffix: "/api/v1/test",
    method: "PUT",
  };
  const first = claimArgs(profile),
    second = { ...first, client_request_id: randomUUID() };
  const plan = { ...planFor(first), request_digest: requestDigest(request) };
  const prepared = [];
  for (const [store, claim] of [
    [a, first],
    [b, second],
  ]) {
    const e = (await store.call("execution.claim", claim)).execution;
    await store.call("execution.lock", { execution_id: e.execution_id });
    prepared.push(
      (
        await store.call("execution.prepare", {
          execution_id: e.execution_id,
          plan,
        })
      ).execution,
    );
  }
  const results = await Promise.allSettled(
    [a, b].map((store, i) =>
      commitSendPermit(
        store,
        {
          execution_id: prepared[i].execution_id,
          record_version: prepared[i].record_version,
          intent_digest: first.intent_digest,
          material_digest: plan.material_digest,
          request_digest: plan.request_digest,
          auth_generation: 0,
          profile_revision: 1,
          trust_digest: config("p").trust_digest,
          credential_ref: reference,
        },
        request,
        AbortSignal.timeout(5000),
      ),
    ),
  );
  assert.equal(results.filter((r) => r.status === "fulfilled").length, 1);
  assert.equal(
    results.find((r) => r.status === "rejected").reason.subtype,
    "plan_consumed",
  );
  await sendWithPermit(
    results.find((r) => r.status === "fulfilled").value,
    AbortSignal.timeout(1000),
  );
  assert.equal(count, 1);
});

function sql(path, source) {
  const result = spawnSync(
    process.execPath,
    [
      "--disable-warning=ExperimentalWarning",
      "--input-type=module",
      "-e",
      "import { DatabaseSync } from 'node:sqlite'; const db=new DatabaseSync(process.argv[1]); " +
        source +
        "; db.close();",
      join(path, "state.db"),
    ],
    { encoding: "utf8", timeout: 10000 },
  );
  assert.equal(result.status, 0, result.stderr);
  return result.stdout;
}

test("failure after plan consumption rolls back SEND_INTENT and returns no permit", async (t) => {
  const { a, profile, path } = await fixture(t);
  const reference = await credential(a),
    claim = claimArgs(profile);
  const e = (await a.call("execution.claim", claim)).execution;
  await a.call("execution.lock", { execution_id: e.execution_id });
  const request = {
    endpoint: "http://127.0.0.1:1",
    suffix: "/api/v1/test",
    method: "PUT",
  };
  const plan = { ...planFor(claim), request_digest: requestDigest(request) };
  const prepared = (
    await a.call("execution.prepare", { execution_id: e.execution_id, plan })
  ).execution;
  sql(
    path,
    `db.exec("CREATE TRIGGER fault_before_commit BEFORE INSERT ON execution_events WHEN NEW.kind='SEND_INTENT' BEGIN SELECT RAISE(ABORT,'injected event persistence fault'); END")`,
  );
  await assert.rejects(
    commitSendPermit(
      a,
      {
        execution_id: e.execution_id,
        record_version: prepared.record_version,
        intent_digest: claim.intent_digest,
        material_digest: plan.material_digest,
        request_digest: plan.request_digest,
        auth_generation: 0,
        profile_revision: 1,
        trust_digest: config("p").trust_digest,
        credential_ref: reference,
      },
      request,
      AbortSignal.timeout(5000),
    ),
    (error) => error.type === "internal",
  );
  const saved = (
    await a.call("execution.get", { execution_id: e.execution_id })
  ).execution;
  assert.equal(saved.state, "PREPARED");
  assert.equal(saved.send_intent_at, null);
  assert.equal(
    JSON.parse(
      sql(
        path,
        `console.log(JSON.stringify(db.prepare('SELECT consumed_by FROM plans').get()))`,
      ),
    ).consumed_by,
    null,
  );
});

test("missing uniqueness/transition schema is detected on reopening", async (t) => {
  const { a, path } = await fixture(t);
  await a.close();
  sql(path, `db.exec('DROP TRIGGER immutable_execution_identity')`);
  const store = new StateStore(path);
  t.after(() => store.close());
  await assert.rejects(
    store.call("info"),
    (error) => error.type === "policy" && error.subtype === "store_unsafe",
  );
});

test("unsupported future schema is refused without initializing a replacement store", async (t) => {
  const { a, path } = await fixture(t);
  await a.close();
  const before = JSON.parse(
    sql(
      path,
      `console.log(JSON.stringify(db.prepare('SELECT state_store_id FROM store_meta').get()))`,
    ),
  );
  sql(path, `db.exec('PRAGMA user_version=999')`);
  const store = new StateStore(path);
  t.after(() => store.close());
  await assert.rejects(
    store.call("info"),
    (error) => error.subtype === "store_unsafe",
  );
  assert.equal(
    JSON.parse(
      sql(
        path,
        `console.log(JSON.stringify(db.prepare('SELECT state_store_id FROM store_meta').get()))`,
      ),
    ).state_store_id,
    before.state_store_id,
  );
});
