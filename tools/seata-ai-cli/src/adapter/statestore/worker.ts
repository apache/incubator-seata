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
import { parentPort, workerData } from "node:worker_threads";
import { DatabaseSync, type SQLInputValue } from "node:sqlite";
import { closeSync, fstatSync, statSync } from "node:fs";
import { randomUUID } from "node:crypto";
import { PrivateDirectory } from "../../platform/linux.js";
import { resource } from "../../resources.js";
import {
  type Obj,
  type Json,
  object,
  str,
  canonical,
  digest,
  parseJson,
} from "../../protocol/value.js";
import { CliError, safeError } from "../../protocol/error.js";

const port = parentPort;
if (!port) throw new Error("private worker");
let directory: PrivateDirectory | undefined;
let db: DatabaseSync | undefined;
let databaseFd: number | undefined;
let lifetime: number | undefined;
let startupFailure: CliError | undefined;
let operationDeadline = 0;
const locks = new Map<string, number>();
const now = () => new Date().toISOString();
function database(): DatabaseSync {
  if (!db)
    throw new CliError(
      "internal",
      "journal_failure",
      "StateStore is not open.",
    );
  return db;
}
function row(sql: string, ...args: SQLInputValue[]): Obj | undefined {
  const result = database()
    .prepare(sql)
    .get(...args);
  if (!result) return undefined;
  const json: Obj = {};
  for (const [k, v] of Object.entries(result)) {
    if (typeof v === "bigint" || v instanceof Uint8Array)
      throw new CliError(
        "internal",
        "journal_failure",
        "Unexpected stored value.",
      );
    json[k] = v;
  }
  return json;
}
function run(sql: string, ...args: SQLInputValue[]): void {
  database()
    .prepare(sql)
    .run(...args);
}
function tx<T>(body: () => T): T {
  budget();
  database().exec("BEGIN IMMEDIATE");
  try {
    const result = body();
    database().exec("COMMIT");
    return result;
  } catch (error) {
    try {
      database().exec("ROLLBACK");
    } catch {
      /* A failed COMMIT is never a send permit. */
    }
    throw error;
  }
}
function budget(): number {
  const remaining = Math.floor(operationDeadline - performance.now());
  if (remaining <= 0)
    throw new CliError(
      "cancelled",
      "context_cancelled",
      "StateStore operation deadline expired; inspect durable state.",
    );
  if (db) db.exec("PRAGMA busy_timeout=" + Math.min(5000, remaining));
  return Math.min(5000, remaining);
}
function schemaFingerprint(): string {
  return digest(
    database()
      .prepare(
        "SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name",
      )
      .all()
      .map(object),
  );
}
function openStore(): void {
  if (startupFailure) throw startupFailure;
  if (db) return;
  directory = new PrivateDirectory(str(object(workerData).directory));
  lifetime = directory.lock("lifetime.lock", true);
  databaseFd = directory.file("state.db");
  directory.checkAuxiliary();
  db = new DatabaseSync(directory.path + "/state.db", {
    enableForeignKeyConstraints: true,
    enableDoubleQuotedStringLiterals: false,
    allowExtension: false,
  });
  const before = fstatSync(databaseFd),
    after = statSync(directory.path + "/state.db");
  if (before.ino !== after.ino || before.dev !== after.dev)
    throw new CliError(
      "policy",
      "store_unsafe",
      "Database identity changed while opening.",
    );
  const version = row("PRAGMA user_version")?.user_version;
  if (version !== 0 && version !== 1)
    throw new CliError(
      "policy",
      "store_unsafe",
      "Unsupported StateStore schema; no automatic migration.",
    );
  db.exec(
    "PRAGMA busy_timeout=" +
      budget() +
      "; PRAGMA journal_mode=WAL; PRAGMA synchronous=FULL; PRAGMA foreign_keys=ON;",
  );
  if (
    row("PRAGMA journal_mode")?.journal_mode !== "wal" ||
    row("PRAGMA synchronous")?.synchronous !== 2 ||
    row("PRAGMA foreign_keys")?.foreign_keys !== 1
  )
    throw new CliError(
      "policy",
      "store_unsafe",
      "SQLite durability settings were not accepted.",
    );
  tx(() => {
    if (
      !row(
        "SELECT name FROM sqlite_master WHERE type='table' AND name='store_meta'",
      )
    ) {
      if (
        row(
          "SELECT name FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' LIMIT 1",
        )
      )
        throw new CliError(
          "policy",
          "store_unsafe",
          "Unrecognized existing StateStore.",
        );
      db?.exec(
        resource("api/store/schema.sql")
          .toString("utf8")
          .replace(/^PRAGMA[^;]+;/gm, ""),
      );
      run(
        "INSERT INTO store_meta(singleton,state_store_id,schema_version,created_at) VALUES(1,?,1,?)",
        randomUUID(),
        now(),
      );
      db?.exec("PRAGMA user_version=1;");
      run(
        "INSERT INTO local_settings VALUES(?,?)",
        "ddl_digest",
        digest(resource("api/store/schema.sql").toString("utf8")),
      );
      run(
        "INSERT INTO local_settings VALUES(?,?)",
        "schema_fingerprint",
        schemaFingerprint(),
      );
    }
  });
  if (
    row("PRAGMA user_version")?.user_version !== 1 ||
    row("SELECT schema_version FROM store_meta WHERE singleton=1")
      ?.schema_version !== 1
  )
    throw new CliError(
      "policy",
      "store_unsafe",
      "Unsupported StateStore schema; no automatic migration.",
    );
  if (
    row("PRAGMA quick_check")?.quick_check !== "ok" ||
    row("PRAGMA foreign_key_check")
  )
    throw new CliError(
      "policy",
      "store_unsafe",
      "StateStore integrity check failed.",
    );
  if (
    row("SELECT value FROM local_settings WHERE key='ddl_digest'")?.value !==
    digest(resource("api/store/schema.sql").toString("utf8"))
  )
    throw new CliError(
      "policy",
      "store_unsafe",
      "StateStore contract differs from this build.",
    );
  if (
    row("SELECT value FROM local_settings WHERE key='schema_fingerprint'")
      ?.value !== schemaFingerprint()
  )
    throw new CliError(
      "policy",
      "store_unsafe",
      "StateStore tables, indexes or triggers changed.",
    );
  directory.checkAuxiliary();
}
function profile(name: string): Obj {
  const result = row(
    "SELECT * FROM profiles WHERE name=? AND deleted_at IS NULL",
    name,
  );
  if (!result)
    throw new CliError(
      "config",
      "profile_not_found",
      "Profile does not exist.",
    );
  return result;
}
function decode(text: Json | undefined): Obj {
  return object(parseJson(Buffer.from(str(text)), false, 16 * 1024 * 1024));
}
function publicProfile(p: Obj): Obj {
  return {
    ...decode(p.config_json),
    profile_id: p.profile_id ?? null,
    revision: p.revision ?? null,
    auth_generation: p.auth_generation ?? null,
  };
}
function profileList(): Obj[] {
  const profiles: Obj[] = [];
  let bytes = 0;
  for (const p of database()
    .prepare("SELECT * FROM profiles WHERE deleted_at IS NULL ORDER BY name")
    .iterate()) {
    budget();
    const profile = publicProfile(object(p));
    bytes += Buffer.byteLength(canonical(profile));
    if (bytes > 8 * 1024 * 1024 || profiles.length >= 10000)
      throw new CliError(
        "internal",
        "output_limit",
        "Profile list exceeds its evidence budget; query individual profiles by name.",
      );
    profiles.push(profile);
  }
  return profiles;
}
function event(id: string, kind: string, data: Obj): void {
  const sequence = Number(
    row(
      "SELECT coalesce(max(sequence),0)+1 AS n FROM execution_events WHERE execution_id=?",
      id,
    )?.n,
  );
  const payload = canonical(data);
  run(
    "INSERT INTO execution_events VALUES(?,?,?,?,?,?)",
    id,
    sequence,
    kind,
    payload,
    digest({ sequence, kind, data }),
    now(),
  );
}
function execution(args: Obj): Obj {
  const byId = typeof args.execution_id === "string";
  const key = byId
    ? str(args.execution_id)
    : str(args.client_request_id).toLowerCase();
  const result = row(
    "SELECT * FROM executions WHERE " +
      (byId ? "execution_id" : "client_request_id") +
      "=?",
    key,
  );
  if (!result)
    throw new CliError(
      "not_found",
      "execution_not_found",
      "No execution exists in this state store for this ID.",
    );
  return result;
}
function fence(p: Obj): void {
  const time = now();
  const id = str(p.profile_id);
  run(
    "UPDATE credential_metadata SET status='cleanup_pending',updated_at=? WHERE profile_id=? AND status IN ('active','staged')",
    time,
    id,
  );
  run(
    "UPDATE profiles SET auth_generation=auth_generation+1,active_credential_ref=NULL,updated_at=? WHERE profile_id=?",
    time,
    id,
  );
  run(
    "UPDATE auth_flows SET state='cancelled',terminal_at=?,record_version=record_version+1 WHERE profile_id=? AND state IN ('pending','submitting')",
    time,
    id,
  );
}
function op(name: string, args: Obj): Obj {
  if (name === "close") {
    for (const fd of locks.values()) closeSync(fd);
    locks.clear();
    db?.close();
    db = undefined;
    if (databaseFd !== undefined) closeSync(databaseFd);
    if (lifetime !== undefined) closeSync(lifetime);
    directory?.close();
    return {};
  }
  try {
    openStore();
  } catch (error) {
    startupFailure = safeError(error);
    throw startupFailure;
  }
  switch (name) {
    case "info":
      return {
        ...row("SELECT * FROM store_meta"),
        sqlite_version:
          row("SELECT sqlite_version() AS version")?.version ?? null,
        journal_mode: row("PRAGMA journal_mode")?.journal_mode ?? null,
        synchronous: row("PRAGMA synchronous")?.synchronous ?? null,
      };
    case "profile.list":
      return {
        profiles: profileList(),
        current_profile:
          row(
            "SELECT name FROM profiles WHERE profile_id=(SELECT value FROM local_settings WHERE key='current_profile') AND deleted_at IS NULL",
          )?.name ?? null,
      };
    case "profile.get":
      return { profile: publicProfile(profile(str(args.name))) };
    case "profile.create":
      return tx(() => {
        const config = object(args.config),
          time = now(),
          id = randomUUID();
        if (
          row(
            "SELECT profile_id FROM profiles WHERE name=? AND deleted_at IS NULL",
            str(config.name),
          )
        )
          throw new CliError(
            "conflict",
            "profile_exists",
            "A live profile already uses this name.",
          );
        run(
          "INSERT INTO profiles(profile_id,name,revision,auth_generation,config_json,target_digest,trust_digest,created_at,updated_at) VALUES(?,?,1,0,?,?,?,?,?)",
          id,
          str(config.name),
          canonical(config),
          str(args.target_digest),
          str(args.trust_digest),
          time,
          time,
        );
        return {
          profile: publicProfile(profile(str(config.name))),
          trust: config.trust ?? {},
        };
      });
    case "profile.update":
      return tx(() => {
        const p = profile(str(args.name));
        if (p.revision !== args.expected_revision)
          throw new CliError(
            "conflict",
            "state_changed",
            "Profile revision changed.",
          );
        const config = object(args.config);
        fence(p);
        run(
          "UPDATE profiles SET name=?,revision=revision+1,config_json=?,target_digest=?,trust_digest=?,updated_at=? WHERE profile_id=?",
          str(config.name),
          canonical(config),
          str(args.target_digest),
          str(args.trust_digest),
          now(),
          str(p.profile_id),
        );
        return {
          profile: publicProfile(profile(str(config.name))),
          auth_invalidated: true,
          invalidated_plans: true,
        };
      });
    case "profile.use":
      return tx(() => {
        const p = profile(str(args.name));
        run(
          "INSERT INTO local_settings VALUES('current_profile',?) ON CONFLICT(key) DO UPDATE SET value=excluded.value",
          str(p.profile_id),
        );
        return { current_profile: p.name ?? null };
      });
    case "profile.delete":
      return tx(() => {
        const p = profile(str(args.name));
        if (p.revision !== args.expected_revision)
          throw new CliError(
            "conflict",
            "state_changed",
            "Profile revision changed.",
          );
        fence(p);
        run(
          "UPDATE profiles SET deleted_at=?,revision=revision+1 WHERE profile_id=?",
          now(),
          str(p.profile_id),
        );
        run(
          "DELETE FROM local_settings WHERE key='current_profile' AND value=?",
          str(p.profile_id),
        );
        return {
          deleted_profile_id: p.profile_id ?? null,
          credential_cleanup: "pending",
          retained_execution_count:
            row(
              "SELECT count(*) AS n FROM executions WHERE profile_id=?",
              str(p.profile_id),
            )?.n ?? 0,
        };
      });
    case "auth.logout":
      return tx(() => {
        const p = profile(str(args.name));
        fence(p);
        return {
          authenticated: false,
          auth_generation: Number(p.auth_generation) + 1,
          credential_cleanup: "pending",
        };
      });
    case "auth.status": {
      const p = profile(str(args.name));
      return {
        authenticated: p.active_credential_ref !== null,
        generation: p.auth_generation ?? null,
        storage:
          object(decode(p.config_json).policy).storage_policy ??
          "keyring_required",
        verification: "not_evaluated",
        expiry_hint: null,
      };
    }
    case "execution.get": {
      const e = execution(args),
        id = str(e.execution_id);
      const events = database()
        .prepare(
          "SELECT * FROM execution_events WHERE execution_id=? ORDER BY sequence",
        )
        .all(id)
        .map((r) => object(r));
      for (let i = 0; i < events.length; i++) {
        const r = events[i];
        if (
          !r ||
          r.sequence !== i + 1 ||
          r.event_digest !==
            digest({
              sequence: r.sequence,
              kind: r.kind ?? null,
              data: decode(r.event_json),
            })
        )
          throw new CliError(
            "policy",
            "store_unsafe",
            "Execution event integrity failed.",
          );
      }
      const stateStoreId =
        row("SELECT state_store_id FROM store_meta")?.state_store_id ?? null;
      const safe: Obj = {};
      for (const key of [
        "execution_id",
        "client_request_id",
        "command",
        "state",
        "request_phase",
        "outcome",
        "response_ack",
        "observation",
        "business_completion",
        "target_digest",
        "plan_id",
        "record_version",
        "send_intent_at",
        "created_at",
        "updated_at",
      ])
        safe[key] = e[key] ?? null;
      safe.state_store_id = stateStoreId;
      safe.causality = "unproven";
      safe.target = decode(e.target_json);
      return {
        execution: safe,
        events: events.map((r) => ({
          sequence: r.sequence ?? null,
          kind: r.kind ?? null,
          data: decode(r.event_json),
          created_at: r.created_at ?? null,
        })),
        state_store_id: stateStoreId,
        recovery: {
          next_action: e.send_intent_at
            ? "read_only_reconcile"
            : "inspect_owner",
          replay_allowed: false,
        },
      };
    }
    case "execution.claim":
      return tx(() => {
        const request = str(args.client_request_id).toLowerCase();
        if (
          !/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/.test(
            request,
          )
        )
          throw new CliError(
            "validation",
            "invalid_argument",
            "Invalid request UUID.",
          );
        const existing = row(
          "SELECT * FROM executions WHERE client_request_id=?",
          request,
        );
        if (existing) {
          if (existing.intent_digest !== args.intent_digest)
            throw new CliError(
              "conflict",
              "request_id_conflict",
              "Request ID belongs to another intent.",
            );
          return { execution: existing, created: false };
        }
        if (
          row(
            "SELECT client_request_id FROM request_tombstones WHERE client_request_id=?",
            request,
          )
        )
          throw new CliError(
            "conflict",
            "request_id_conflict",
            "Request ID is retained by a tombstone.",
          );
        const id = randomUUID(),
          time = now();
        run(
          "INSERT INTO executions(execution_id,client_request_id,command,intent_digest,intent_json,target_digest,target_json,profile_id,state,request_phase,outcome,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,'CREATED','not_started','not_started',?,?)",
          id,
          request,
          str(args.command),
          str(args.intent_digest),
          canonical(object(args.intent)),
          str(args.target_digest),
          canonical(object(args.target)),
          str(args.profile_id),
          time,
          time,
        );
        event(id, "CREATED", { intent_digest: args.intent_digest ?? null });
        return { execution: execution({ execution_id: id }), created: true };
      });
    case "execution.lock": {
      const id = str(execution(args).execution_id);
      if (locks.has(id))
        throw new CliError(
          "conflict",
          "execution_busy",
          "Execution is already owned by this worker.",
        );
      if (!directory)
        throw new CliError(
          "internal",
          "journal_failure",
          "No storage directory.",
        );
      locks.set(id, directory.lock("execution-" + id + ".lock"));
      return { acquired: true };
    }
    case "execution.unlock": {
      const id = str(args.execution_id),
        fd = locks.get(id);
      if (fd !== undefined) {
        closeSync(fd);
        locks.delete(id);
      }
      return {};
    }
    case "execution.prepare":
      return tx(() => {
        const e = execution(args),
          id = str(e.execution_id);
        if (!locks.has(id) || e.state !== "CREATED")
          throw new CliError(
            "conflict",
            "state_changed",
            "Execution is not owned and newly created.",
          );
        const plan = object(args.plan),
          planId = str(plan.plan_id);
        if (!row("SELECT plan_id FROM plans WHERE plan_id=?", planId))
          run(
            "INSERT INTO plans(plan_id,plan_format_version,command,plan_json,target_digest,material_digest,created_at,expires_at) VALUES(?,1,?,?,?,?,?,?)",
            planId,
            str(e.command),
            canonical(plan),
            str(e.target_digest),
            str(plan.material_digest),
            str(plan.created_at),
            str(plan.expires_at),
          );
        const stored = row("SELECT * FROM plans WHERE plan_id=?", planId);
        if (
          stored?.plan_json !== canonical(plan) ||
          stored?.target_digest !== e.target_digest ||
          stored?.command !== e.command
        )
          throw new CliError(
            "conflict",
            "plan_changed",
            "Plan binding differs.",
          );
        run(
          "UPDATE executions SET state='PREPARED',plan_id=?,record_version=record_version+1,updated_at=? WHERE execution_id=?",
          planId,
          now(),
          id,
        );
        event(id, "PREPARED", { plan_id: planId });
        if (args.confirmation === true) {
          run(
            "UPDATE executions SET state='AWAITING_CONFIRMATION',record_version=record_version+1 WHERE execution_id=?",
            id,
          );
          event(id, "AWAITING_CONFIRMATION", { plan_id: planId });
        }
        return { execution: execution(args) };
      });
    case "execution.sendIntent":
      return tx(() => {
        const e = execution(args),
          id = str(e.execution_id);
        if (
          !locks.has(id) ||
          !["PREPARED", "AWAITING_CONFIRMATION"].includes(str(e.state)) ||
          e.record_version !== args.record_version ||
          e.intent_digest !== args.intent_digest
        )
          throw new CliError(
            "conflict",
            "state_changed",
            "Execution send preconditions changed.",
          );
        const plan = row("SELECT * FROM plans WHERE plan_id=?", str(e.plan_id));
        const p = row(
          "SELECT * FROM profiles WHERE profile_id=? AND deleted_at IS NULL",
          str(e.profile_id),
        );
        if (
          !plan ||
          !p ||
          p.auth_generation !== args.auth_generation ||
          p.revision !== args.profile_revision ||
          p.trust_digest !== args.trust_digest ||
          p.active_credential_ref !== args.credential_ref ||
          !args.credential_ref
        )
          throw new CliError(
            "conflict",
            "state_changed",
            "Profile or authentication generation changed.",
          );
        if (plan.consumed_by !== null)
          throw new CliError(
            "conflict",
            "plan_consumed",
            "Plan was already consumed.",
          );
        if (
          !args.request_digest ||
          decode(plan.plan_json).request_digest !== args.request_digest
        )
          throw new CliError(
            "conflict",
            "plan_changed",
            "Transport request is not bound to the persisted plan.",
          );
        const time = now();
        if (
          time < str(plan.created_at) ||
          time >= str(plan.expires_at) ||
          plan.material_digest !== args.material_digest ||
          plan.target_digest !== e.target_digest
        )
          throw new CliError(
            "conflict",
            "plan_changed",
            "Plan expired or material changed.",
          );
        run(
          "UPDATE plans SET consumed_by=?,consumed_at=? WHERE plan_id=?",
          id,
          time,
          str(plan.plan_id),
        );
        run(
          "UPDATE executions SET state='SEND_INTENT',request_phase='request_maybe_sent',outcome='unknown',send_intent_at=?,record_version=record_version+1,updated_at=? WHERE execution_id=?",
          time,
          time,
          id,
        );
        event(id, "SEND_INTENT", { plan_id: plan.plan_id ?? null });
        return { execution: execution(args) };
      });
    case "execution.recover":
      return tx(() => {
        const e = execution(args),
          id = str(e.execution_id);
        if (!directory || locks.has(id))
          throw new CliError(
            "conflict",
            "execution_busy",
            "Execution still has an active owner.",
          );
        const fd = directory.lock("execution-" + id + ".lock");
        try {
          if (["CREATED", "PREPARED"].includes(str(e.state))) {
            run(
              "UPDATE executions SET state='FINAL',terminal_at=?,updated_at=?,record_version=record_version+1 WHERE execution_id=?",
              now(),
              now(),
              id,
            );
            event(id, "RECOVERED_NOT_STARTED", {});
          }
          return { execution: execution(args), send_permit: false };
        } finally {
          closeSync(fd);
        }
      });
    case "flow.create":
      return tx(() => {
        const p = profile(str(args.name));
        const existing = row(
          "SELECT * FROM auth_flows WHERE profile_id=? AND state IN ('pending','submitting')",
          str(p.profile_id),
        );
        if (existing) return { flow: existing, created: false };
        const id = randomUUID(),
          time = now();
        run(
          "INSERT INTO auth_flows(flow_id,profile_id,profile_label,auth_generation,target_digest,trust_digest,helper_instance_id,state,created_at,expires_at,heartbeat_at) VALUES(?,?,?,?,?,?,?,'pending',?,?,?)",
          id,
          str(p.profile_id),
          str(p.name),
          Number(p.auth_generation),
          str(p.target_digest),
          str(p.trust_digest),
          randomUUID(),
          time,
          new Date(Date.now() + 300000).toISOString(),
          time,
        );
        return {
          flow: row("SELECT * FROM auth_flows WHERE flow_id=?", id) ?? {},
          created: true,
        };
      });
    case "flow.submit":
      return tx(() => {
        const f = row(
          "SELECT * FROM auth_flows WHERE flow_id=?",
          str(args.flow_id),
        );
        if (!f || f.state !== "pending" || str(f.expires_at) <= now())
          throw new CliError(
            "conflict",
            "state_changed",
            "Flow cannot accept another submission.",
          );
        run(
          "UPDATE auth_flows SET state='submitting',record_version=record_version+1 WHERE flow_id=?",
          str(f.flow_id),
        );
        return {};
      });
    case "flow.stage":
      return tx(() => {
        const f = row(
          "SELECT * FROM auth_flows WHERE flow_id=?",
          str(args.flow_id),
        );
        if (!f || f.state !== "submitting")
          throw new CliError(
            "conflict",
            "state_changed",
            "Flow is not submitting.",
          );
        const reference = randomUUID();
        run(
          "INSERT INTO credential_metadata(credential_ref,flow_id,profile_id,auth_generation,provider,status,created_at,updated_at) VALUES(?,?,?,?,?,'staged',?,?)",
          reference,
          str(f.flow_id),
          str(f.profile_id),
          Number(f.auth_generation),
          str(args.provider),
          now(),
          now(),
        );
        return { credential_ref: reference };
      });
    case "flow.activate":
      return tx(() => {
        const f = row(
          "SELECT * FROM auth_flows WHERE flow_id=?",
          str(args.flow_id),
        );
        const p = f
          ? row(
              "SELECT * FROM profiles WHERE profile_id=? AND deleted_at IS NULL",
              str(f.profile_id),
            )
          : undefined;
        const c = row(
          "SELECT * FROM credential_metadata WHERE credential_ref=? AND status='staged'",
          str(args.credential_ref),
        );
        if (
          !f ||
          !p ||
          !c ||
          f.state !== "submitting" ||
          str(f.expires_at) <= now() ||
          f.auth_generation !== p.auth_generation ||
          f.target_digest !== p.target_digest ||
          f.trust_digest !== p.trust_digest ||
          c.profile_id !== p.profile_id ||
          c.auth_generation !== p.auth_generation ||
          c.flow_id !== f.flow_id
        )
          throw new CliError(
            "conflict",
            "state_changed",
            "Flow was cancelled, expired, or fenced.",
          );
        run(
          "UPDATE credential_metadata SET status='retired',updated_at=? WHERE profile_id=? AND status='active'",
          now(),
          str(p.profile_id),
        );
        run(
          "UPDATE credential_metadata SET status='active',updated_at=? WHERE credential_ref=?",
          now(),
          str(c.credential_ref),
        );
        run(
          "UPDATE profiles SET active_credential_ref=? WHERE profile_id=?",
          str(c.credential_ref),
          str(p.profile_id),
        );
        run(
          "UPDATE auth_flows SET state='authenticated',terminal_at=?,record_version=record_version+1 WHERE flow_id=?",
          now(),
          str(f.flow_id),
        );
        return { authenticated: true };
      });
    case "flow.cancel":
      return tx(() => {
        const f = row(
          "SELECT * FROM auth_flows WHERE flow_id=? AND profile_label=?",
          str(args.flow_id),
          str(args.name),
        );
        if (!f)
          throw new CliError(
            "not_found",
            "auth_flow_not_found",
            "Authentication flow does not exist.",
          );
        const changed = ["pending", "submitting"].includes(str(f.state));
        if (changed) {
          run(
            "UPDATE auth_flows SET state='cancelled',terminal_at=?,record_version=record_version+1 WHERE flow_id=?",
            now(),
            str(f.flow_id),
          );
          run(
            "UPDATE credential_metadata SET status='cleanup_pending',updated_at=? WHERE flow_id=? AND status='staged'",
            now(),
            str(f.flow_id),
          );
        }
        return {
          flow_id: f.flow_id ?? null,
          flow_state: changed ? "cancelled" : (f.state ?? null),
          changed,
        };
      });
    default:
      throw new CliError(
        "internal",
        "contract_violation",
        "Unknown private StateStore operation.",
      );
  }
}
port.on("message", (message: unknown) => {
  let id: Json = null;
  try {
    const m = object(message);
    id = m.id ?? null;
    operationDeadline = typeof m.deadline === "number" ? m.deadline : 0;
    if (m.op !== "close") budget();
    const value = op(str(m.op), object(m.args));
    port.postMessage({ id, value });
  } catch (error) {
    const failure = safeError(error);
    port.postMessage({
      id,
      error: {
        type: failure.type,
        subtype: failure.subtype,
        message: failure.message,
      },
    });
  }
});
