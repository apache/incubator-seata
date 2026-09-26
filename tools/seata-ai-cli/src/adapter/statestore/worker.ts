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
  isObject,
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
function expireFlows(): void {
  const time = now();
  run("UPDATE auth_flows SET state='expired',terminal_at=expires_at,reason_json=?,record_version=record_version+1 WHERE state IN ('pending','submitting') AND expires_at<=?",
    canonical({ subtype: "auth_flow_expired" }), time);
  run("UPDATE auth_flows SET state='failed',terminal_at=strftime('%Y-%m-%dT%H:%M:%fZ',heartbeat_at,'+10 seconds'),reason_json=?,record_version=record_version+1 WHERE state IN ('pending','submitting') AND heartbeat_at<?",
    canonical({ subtype: "helper_unavailable" }), new Date(Date.now() - 10000).toISOString());
  run("UPDATE credential_metadata SET status='cleanup_pending',updated_at=? WHERE status='staged' AND flow_id IN (SELECT flow_id FROM auth_flows WHERE state IN ('expired','failed','cancelled'))", time);
  run("DELETE FROM auth_flows WHERE terminal_at<? AND terminal_at=strftime('%Y-%m-%dT%H:%M:%fZ',terminal_at) AND NOT EXISTS (SELECT 1 FROM credential_metadata WHERE flow_id=auth_flows.flow_id)",
    new Date(Date.now() - 86400000).toISOString());
}
function publicFlow(args: Obj): Obj | null {
  const flow = row("SELECT * FROM auth_flows WHERE flow_id=? AND (profile_label=? OR profile_id IN (SELECT profile_id FROM profiles WHERE name=? AND deleted_at IS NULL))",
    str(args.flow_id), str(args.name), str(args.name));
  if (!flow || (flow.terminal_at !== null && str(flow.terminal_at) < new Date(Date.now() - 86400000).toISOString()))
    return null;
  return { flow_id: flow.flow_id ?? null, flow_state: flow.state === "submitting" ? "pending" : flow.state ?? null,
    phase: flow.state ?? null, reason: flow.reason_json === null ? null : decode(flow.reason_json),
    expires_at: flow.expires_at ?? null, terminal_at: flow.terminal_at ?? null };
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
  // A later validation failure must not roll back expiry or safe retention GC.
  // Each operation still rechecks the flow inside its own transaction.
  if (["flow.create", "flow.submit", "flow.stage", "flow.activate", "flow.helper", "flow.heartbeat"].includes(name))
    tx(() => expireFlows());
  switch (name) {
    case "skills.install.lock": {
      if (locks.has("skills-install")) throw new CliError("conflict", "skills_install_busy", "This worker already owns the installation lock.");
      if (!directory) throw new CliError("internal", "journal_failure", "No storage directory.");
      locks.set("skills-install", directory.lock("skills-install.lock"));
      const stored = row("SELECT value FROM local_settings WHERE key='skills.install'");
      return { record: stored ? parseJson(Buffer.from(str(stored.value))) : null };
    }
    case "skills.install.get": {
      const stored = row("SELECT value FROM local_settings WHERE key='skills.install'");
      return { record: stored ? parseJson(Buffer.from(str(stored.value))) : null };
    }
    case "skills.install.record":
      return tx(() => {
        if (!locks.has("skills-install")) throw new CliError("conflict", "skills_install_not_owned", "Installation lifetime lock is required.");
        const stored = row("SELECT value FROM local_settings WHERE key='skills.install'");
        const previous = stored ? object(parseJson(Buffer.from(str(stored.value)))) : undefined;
        if ((previous?.revision ?? 0) !== args.expected_revision)
          throw new CliError("conflict", "skills_install_changed", "Installation record changed.");
        const record = object(args.record), phase = str(record.phase);
        if (!["preparing", "writing", "verifying", "complete"].includes(phase) ||
            !/^[a-f0-9-]{36}$/.test(str(record.operation_id)) ||
            !/^sha256:[a-f0-9]{64}$/.test(str(record.content_digest)) ||
            typeof record.version !== "string" || record.version.length > 128 ||
            typeof record.installation_path !== "string" || !record.installation_path.startsWith("/") || record.installation_path.length > 4096 ||
            !Array.isArray(record.files) || record.files.length > 200 ||
            (record.previous_files !== undefined && (!Array.isArray(record.previous_files) || record.previous_files.length > 200)) ||
            Buffer.byteLength(canonical(record)) > 65536)
          throw new CliError("validation", "invalid_installation_record", "Installation metadata is invalid or exceeds limits.");
        const stages: Record<string, number> = { preparing: 0, writing: 1, verifying: 2, complete: 3 };
        if (!previous) {
          if (phase !== "preparing") throw new CliError("conflict", "skills_install_phase", "An installation must begin with a durable preparing record.");
        } else if (previous.operation_id === record.operation_id) {
          for (const key of ["version", "content_digest", "installation_path"])
            if (previous[key] !== record[key]) throw new CliError("conflict", "skills_install_changed", "Installation target is immutable within an operation.");
          if (canonical(previous.previous_files ?? []) !== canonical(record.previous_files ?? []))
            throw new CliError("conflict", "skills_install_changed", "Installation recovery baseline is immutable within an operation.");
          const before = stages[str(previous.phase)]!, after = stages[phase]!;
          if (before === undefined || after < before || after > before + 1 || previous.phase === "complete")
            throw new CliError("conflict", "skills_install_phase", "Installation stages cannot reopen or skip verification.");
        } else if (previous.phase !== "complete" || phase !== "preparing")
          throw new CliError("conflict", "skills_install_unresolved", "An incomplete installation cannot be replaced by a new operation.");
        const saved = { operation_id: record.operation_id!, version: record.version!, phase,
          content_digest: record.content_digest!, installation_path: record.installation_path!,
          files: record.files!, files_digest: digest(record.files!), previous_files: record.previous_files ?? [],
          revision: Number(previous?.revision ?? 0) + 1, updated_at: now() };
        run("INSERT INTO local_settings VALUES('skills.install',?) ON CONFLICT(key) DO UPDATE SET value=excluded.value", canonical(saved));
        return { record: saved };
      });
    case "artifact.lock": {
      const key = str(args.key);
      if (!/^sha256:[a-f0-9]{64}$/.test(key)) throw new CliError("validation", "invalid_argument", "Invalid artifact key.");
      const lockKey = "artifact-" + key.slice(7);
      if (locks.has(lockKey)) throw new CliError("conflict", "artifact_busy", "Artifact is already owned by this worker.");
      if (!directory) throw new CliError("internal", "journal_failure", "No storage directory.");
      locks.set(lockKey, directory.lock(lockKey + ".lock"));
      const stored = row("SELECT value FROM local_settings WHERE key=?", "artifact:" + key);
      return { record: stored ? parseJson(Buffer.from(str(stored.value))) : null };
    }
    case "artifact.record":
      return tx(() => {
        const key = str(args.key), lockKey = "artifact-" + key.slice(7);
        if (!/^sha256:[a-f0-9]{64}$/.test(key) || !locks.has(lockKey))
          throw new CliError("conflict", "artifact_not_owned", "Artifact lifetime lock is required.");
        const stored = row("SELECT value FROM local_settings WHERE key=?", "artifact:" + key);
        const previous = stored ? object(parseJson(Buffer.from(str(stored.value)))) : undefined;
        if ((previous?.revision ?? 0) !== args.expected_revision)
          throw new CliError("conflict", "artifact_revision_conflict", "Artifact ownership metadata changed.");
        const record = object(args.record);
        if (!["writing", "complete", "removing", "removed"].includes(String(record.phase)) ||
          !/^[a-f0-9-]{36}$/.test(str(record.operation_id)) || !/^sha256:[a-f0-9]{64}$/.test(str(record.digest)) ||
          !Number.isSafeInteger(record.bytes) || Number(record.bytes) < 0 || Number(record.bytes) > 16 * 1024 * 1024 ||
          !/^\d+$/.test(str(record.device)) || !/^\d+$/.test(str(record.inode)))
          throw new CliError("validation", "invalid_argument", "Invalid artifact ownership metadata.");
        if (record.phase === "complete" && (previous?.phase !== "writing" || previous.operation_id !== record.operation_id ||
            previous.device !== record.device || previous.inode !== record.inode || previous.digest !== record.digest || previous.bytes !== record.bytes))
          throw new CliError("conflict", "artifact_revision_conflict", "Artifact completion does not match its durable write record.");
        if (["removing", "removed"].includes(String(record.phase))) {
          const expectedPhase = record.phase === "removing" ? "complete" : "removing";
          if (previous?.phase !== expectedPhase || !/^\.seata-remove-[a-f0-9-]{36}$/.test(str(record.staging_name)) ||
              ["device", "inode", "digest", "bytes"].some(key => previous?.[key] !== record[key]) ||
              (record.phase === "removed" && (previous.operation_id !== record.operation_id || previous.staging_name !== record.staging_name)))
            throw new CliError("conflict", "artifact_revision_conflict", "Artifact removal does not match its owned durable record.");
        }
        const saved = { phase: str(record.phase), operation_id: str(record.operation_id), digest: str(record.digest), bytes: Number(record.bytes),
          device: str(record.device), inode: str(record.inode), revision: Number(previous?.revision ?? 0) + 1, updated_at: now(),
          ...(["removing", "removed"].includes(String(record.phase)) ? { staging_name: str(record.staging_name) } : {}) };
        run("INSERT INTO local_settings VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value", "artifact:" + key, canonical(saved));
        return { record: saved };
      });
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
    case "auth.binding": {
      const p = profile(str(args.name));
      const credential = p.active_credential_ref === null ? null : row(
        "SELECT credential_ref,provider,auth_generation FROM credential_metadata WHERE credential_ref=? AND profile_id=? AND status='active'",
        str(p.active_credential_ref), str(p.profile_id));
      if (p.active_credential_ref !== null && (!credential || credential.auth_generation !== p.auth_generation))
        throw new CliError("policy", "store_unsafe", "Active credential binding is inconsistent.");
      return { profile: publicProfile(p), credential: credential ?? null,
        target_digest: p.target_digest ?? null, trust_digest: p.trust_digest ?? null,
        state_store_id: row("SELECT state_store_id FROM store_meta")?.state_store_id ?? null };
    }
    case "credential.cleanup.list":
      return tx(() => {
        if (args.reap_flows === true) expireFlows();
        return { credentials: database().prepare("SELECT credential_ref,provider,flow_id FROM credential_metadata c WHERE status IN ('retired','cleanup_pending') AND NOT EXISTS (SELECT 1 FROM profiles p WHERE p.active_credential_ref=c.credential_ref) LIMIT 128").all().map(r => object(r)),
          state_store_id: row("SELECT state_store_id FROM store_meta")?.state_store_id ?? null };
      });
    case "credential.invalidate":
      return tx(() => {
        const p = profile(str(args.name));
        if (p.active_credential_ref !== args.credential_ref) return { changed: false };
        fence(p);
        return { changed: true };
      });
    case "credential.cleanup.done":
      return tx(() => {
        run("DELETE FROM credential_metadata WHERE credential_ref=? AND status IN ('retired','cleanup_pending') AND NOT EXISTS (SELECT 1 FROM profiles WHERE active_credential_ref=credential_metadata.credential_ref)", str(args.credential_ref));
        return {};
      });
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
    case "execution.readContext": {
      const e = execution(args);
      // Private IPC only: original selectors are not part of the public DTO.
      const p = e.plan_id ? row("SELECT * FROM plans WHERE plan_id=?", str(e.plan_id)) : undefined;
      const plan = p ? decode(p.plan_json) : null;
      const bound = p && isObject(plan) && p.command === e.command && p.target_digest === e.target_digest &&
        (p.consumed_by === null || p.consumed_by === e.execution_id) && plan.material_digest === p.material_digest;
      return { intent: decode(e.intent_json), target_digest: e.target_digest ?? null,
        plan, plan_binding_valid: Boolean(bound) };
    }
    case "execution.observe":
      return tx(() => {
        const e = execution(args), id = str(e.execution_id);
        if (e.record_version !== args.expected_revision)
          throw new CliError("conflict", "execution_changed", "Execution changed during read-only observation.");
        if (e.target_digest !== args.target_digest)
          throw new CliError("policy", "execution_target_mismatch", "Observation does not belong to the original execution target.");
        const data = object(args.observation);
        if (data.causality !== "unproven" || data.replay_allowed !== false ||
            !["postcondition_observed", "precondition_still_observed", "partial_state_observed", "divergent_state_observed", "unknown"].includes(str(data.observation)) ||
            // R11 permits up to 8 MiB of read evidence. A smaller journal
            // ceiling must not reject an otherwise bounded reconciliation.
            Buffer.byteLength(canonical(data)) > 8 * 1024 * 1024)
          throw new CliError("validation", "invalid_observation", "Read-only observation must be bounded and cannot authorize replay or establish causality.");
        // Observations are appended, not substituted for historical response
        // facts. In particular, SEND_INTENT/unknown remains unknown even when
        // a desired condition is now visible on the server.
        event(id, "READ_ONLY_OBSERVATION", data);
        run("UPDATE executions SET record_version=record_version+1,updated_at=? WHERE execution_id=?", now(), id);
        return { execution_id: id, record_version: Number(e.record_version) + 1, appended: true };
      });
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
        expireFlows();
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
        expireFlows();
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
        expireFlows();
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
        expireFlows();
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
    case "flow.status": {
      const result = tx(() => { expireFlows(); return publicFlow(args); });
      if (result === null)
        throw new CliError("not_found", "auth_flow_not_found", "Authentication flow does not exist or its retention period has ended.");
      return result;
    }
    case "flow.helper":
      return tx(() => {
        expireFlows();
        const flow = row("SELECT * FROM auth_flows WHERE flow_id=? AND helper_instance_id=? AND state IN ('pending','submitting')",
          str(args.flow_id), str(args.helper_instance_id));
        const p = flow ? row("SELECT * FROM profiles WHERE profile_id=? AND deleted_at IS NULL", str(flow.profile_id)) : undefined;
        if (!flow || !p || flow.auth_generation !== p.auth_generation || flow.target_digest !== p.target_digest || flow.trust_digest !== p.trust_digest)
          throw new CliError("authentication", "auth_flow_unavailable", "Login flow is no longer active for this target.");
        return { profile: publicProfile(p), flow, state_store_id: row("SELECT state_store_id FROM store_meta")?.state_store_id ?? null };
      });
    case "flow.heartbeat":
      return tx(() => {
        expireFlows();
        const flow = row("SELECT * FROM auth_flows WHERE flow_id=? AND helper_instance_id=?",
          str(args.flow_id), str(args.helper_instance_id));
        if (!flow) throw new CliError("not_found", "auth_flow_not_found", "Authentication helper binding does not exist.");
        const active = ["pending", "submitting"].includes(str(flow.state));
        if (active) run("UPDATE auth_flows SET heartbeat_at=? WHERE flow_id=?", now(), str(flow.flow_id));
        return { active, flow_state: flow.state ?? null };
      });
    case "flow.fail":
      return tx(() => {
        const reasons = ["helper_unavailable", "login_rejected", "login_transport_failed", "credential_store_unavailable", "invalid_credential", "ca_changed", "unsafe_ca_file"];
        const reason = str(args.reason);
        if (!reasons.includes(reason)) throw new CliError("internal", "contract_violation", "Invalid flow failure reason.");
        expireFlows();
        run("UPDATE auth_flows SET state='failed',terminal_at=?,reason_json=?,record_version=record_version+1 WHERE flow_id=? AND helper_instance_id=? AND state IN ('pending','submitting')",
          now(), canonical({ subtype: reason }), str(args.flow_id), str(args.helper_instance_id));
        run("UPDATE credential_metadata SET status='cleanup_pending',updated_at=? WHERE status='staged' AND flow_id IN (SELECT flow_id FROM auth_flows WHERE flow_id=? AND helper_instance_id=? AND state='failed')",
          now(), str(args.flow_id), str(args.helper_instance_id));
        return {};
      });
    case "flow.cancel": {
      const result = tx(() => {
        expireFlows();
        const f = row(
          "SELECT * FROM auth_flows WHERE flow_id=? AND profile_label=?",
          str(args.flow_id),
          str(args.name),
        );
        if (!f || (f.terminal_at !== null && str(f.terminal_at) < new Date(Date.now() - 86400000).toISOString()))
          return null;
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
      if (result === null)
        throw new CliError("not_found", "auth_flow_not_found", "Authentication flow does not exist or its retention period has ended.");
      return result;
    }
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
