-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- Seata AI CLI R9 local metadata reference DDL.
-- Design aid, not a deployed migration. Requires an implementation-pinned SQLite engine.
-- Secrets, password, bearer tokens, login URL tokens MUST NOT enter these tables.
-- Set and verify these connection properties on EVERY application connection.
PRAGMA journal_mode = WAL;
PRAGMA synchronous = FULL;
PRAGMA foreign_keys = ON;
PRAGMA busy_timeout = 5000;

CREATE TABLE store_meta (
  singleton INTEGER PRIMARY KEY CHECK (singleton = 1),
  state_store_id TEXT NOT NULL UNIQUE,
  schema_version INTEGER NOT NULL CHECK (schema_version = 1),
  created_at TEXT NOT NULL,
  recovery_mode TEXT NOT NULL DEFAULT 'normal'
    CHECK (recovery_mode IN ('normal','read_only_recovery'))
);

CREATE TABLE profiles (
  profile_id TEXT PRIMARY KEY,
  name TEXT NOT NULL CHECK (length(name) BETWEEN 1 AND 128),
  revision INTEGER NOT NULL CHECK (revision > 0),
  auth_generation INTEGER NOT NULL CHECK (auth_generation >= 0),
  config_json TEXT NOT NULL CHECK (json_valid(config_json)),
  target_digest TEXT NOT NULL,
  trust_digest TEXT NOT NULL,
  active_credential_ref TEXT,
  deleted_at TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  FOREIGN KEY (active_credential_ref) REFERENCES credential_metadata(credential_ref)
    DEFERRABLE INITIALLY DEFERRED
);
CREATE UNIQUE INDEX profiles_live_name ON profiles(name) WHERE deleted_at IS NULL;

-- Secret lives in a provider. This is ownership/cleanup metadata, not a secret copy.
CREATE TABLE credential_metadata (
  credential_ref TEXT PRIMARY KEY,
  profile_id TEXT NOT NULL REFERENCES profiles(profile_id),
  auth_generation INTEGER NOT NULL CHECK (auth_generation >= 0),
  provider TEXT NOT NULL CHECK (provider IN ('keyring','protected_file')),
  status TEXT NOT NULL CHECK (status IN ('staged','active','retired','cleanup_pending')),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE auth_flows (
  flow_id TEXT PRIMARY KEY,
  profile_id TEXT NOT NULL REFERENCES profiles(profile_id),
  profile_label TEXT NOT NULL,
  auth_generation INTEGER NOT NULL CHECK (auth_generation >= 0),
  target_digest TEXT NOT NULL,
  trust_digest TEXT NOT NULL,
  helper_instance_id TEXT NOT NULL UNIQUE,
  state TEXT NOT NULL CHECK (state IN ('pending','submitting','authenticated','failed','expired','cancelled')),
  reason_json TEXT CHECK (reason_json IS NULL OR json_valid(reason_json)),
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  heartbeat_at TEXT,
  terminal_at TEXT,
  record_version INTEGER NOT NULL DEFAULT 1 CHECK (record_version > 0),
  CHECK ((state IN ('pending','submitting') AND terminal_at IS NULL)
      OR (state IN ('authenticated','failed','expired','cancelled') AND terminal_at IS NOT NULL))
);
CREATE UNIQUE INDEX auth_one_active_flow ON auth_flows(profile_id)
  WHERE state IN ('pending','submitting');

CREATE TABLE executions (
  execution_id TEXT PRIMARY KEY,
  client_request_id TEXT NOT NULL UNIQUE,
  command TEXT NOT NULL,
  intent_digest TEXT NOT NULL,
  intent_json TEXT NOT NULL CHECK (json_valid(intent_json)),
  target_digest TEXT NOT NULL,
  target_json TEXT NOT NULL CHECK (json_valid(target_json)),
  profile_id TEXT NOT NULL REFERENCES profiles(profile_id),
  plan_id TEXT REFERENCES plans(plan_id) DEFERRABLE INITIALLY DEFERRED,
  state TEXT NOT NULL CHECK (state IN ('CREATED','PREPARED','AWAITING_CONFIRMATION','SEND_INTENT','RESPONSE_OBSERVED','FINAL')),
  request_phase TEXT NOT NULL CHECK (request_phase IN ('not_started','request_maybe_sent','response_complete')),
  outcome TEXT NOT NULL CHECK (outcome IN ('not_started','unknown','not_applied','applied','partially_applied')),
  response_ack TEXT NOT NULL DEFAULT 'none'
    CHECK (response_ack IN ('none','accepted','rejected_before_effect','ambiguous')),
  observation TEXT NOT NULL DEFAULT 'unknown'
    CHECK (observation IN ('postcondition_observed','precondition_still_observed','partial_state_observed','divergent_state_observed','unknown')),
  business_completion TEXT NOT NULL DEFAULT 'not_claimed'
    CHECK (business_completion IN ('not_claimed','in_progress','succeeded','failed','unknown')),
  send_intent_at TEXT,
  response_json TEXT CHECK (response_json IS NULL OR json_valid(response_json)),
  evidence_json TEXT CHECK (evidence_json IS NULL OR json_valid(evidence_json)),
  zero_send_proof_json TEXT CHECK (zero_send_proof_json IS NULL OR json_valid(zero_send_proof_json)),
  terminal_at TEXT,
  record_version INTEGER NOT NULL DEFAULT 1 CHECK (record_version > 0),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  CHECK (state NOT IN ('CREATED','PREPARED','AWAITING_CONFIRMATION')
      OR (request_phase = 'not_started' AND outcome = 'not_started' AND send_intent_at IS NULL)),
  CHECK (state != 'SEND_INTENT'
      OR (request_phase = 'request_maybe_sent' AND outcome = 'unknown' AND send_intent_at IS NOT NULL AND plan_id IS NOT NULL)),
  CHECK (state != 'RESPONSE_OBSERVED'
      OR (request_phase = 'response_complete' AND send_intent_at IS NOT NULL AND plan_id IS NOT NULL)),
  CHECK ((state = 'FINAL') = (terminal_at IS NOT NULL)),
  CHECK (request_phase != 'not_started' OR outcome = 'not_started'),
  CHECK (send_intent_at IS NULL OR request_phase != 'not_started' OR zero_send_proof_json IS NOT NULL)
);

CREATE TABLE plans (
  plan_id TEXT PRIMARY KEY,
  plan_format_version INTEGER NOT NULL CHECK (plan_format_version = 1),
  command TEXT NOT NULL,
  plan_json TEXT NOT NULL CHECK (json_valid(plan_json)),
  target_digest TEXT NOT NULL,
  material_digest TEXT NOT NULL,
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  consumed_by TEXT UNIQUE REFERENCES executions(execution_id) DEFERRABLE INITIALLY DEFERRED,
  consumed_at TEXT,
  CHECK ((consumed_by IS NULL) = (consumed_at IS NULL))
);

CREATE TABLE execution_events (
  execution_id TEXT NOT NULL REFERENCES executions(execution_id),
  sequence INTEGER NOT NULL CHECK (sequence > 0),
  kind TEXT NOT NULL,
  event_json TEXT NOT NULL CHECK (json_valid(event_json)),
  event_digest TEXT NOT NULL,
  created_at TEXT NOT NULL,
  PRIMARY KEY (execution_id,sequence)
);

-- Minimal ID tombstones survive evidence GC. The owner field prevents reuse in an import.
CREATE TABLE request_tombstones (
  client_request_id TEXT PRIMARY KEY,
  execution_id TEXT NOT NULL UNIQUE,
  intent_digest TEXT NOT NULL,
  final_state TEXT NOT NULL,
  final_outcome TEXT NOT NULL,
  consumed_plan_id TEXT,
  created_at TEXT NOT NULL
);
CREATE TRIGGER no_claim_tombstoned_id BEFORE INSERT ON executions
WHEN EXISTS (SELECT 1 FROM request_tombstones t WHERE t.client_request_id=NEW.client_request_id)
BEGIN SELECT RAISE(ABORT,'client_request_id retained by tombstone'); END;

CREATE TRIGGER immutable_execution_identity BEFORE UPDATE ON executions
WHEN OLD.client_request_id != NEW.client_request_id OR OLD.command != NEW.command
  OR OLD.intent_digest != NEW.intent_digest OR OLD.intent_json != NEW.intent_json
  OR OLD.target_digest != NEW.target_digest OR OLD.target_json != NEW.target_json
  OR OLD.profile_id != NEW.profile_id OR OLD.created_at != NEW.created_at
  OR (OLD.plan_id IS NOT NULL AND OLD.plan_id IS NOT NEW.plan_id)
  OR (OLD.send_intent_at IS NOT NULL AND OLD.send_intent_at IS NOT NEW.send_intent_at)
BEGIN SELECT RAISE(ABORT,'immutable execution binding changed'); END;

CREATE TRIGGER monotonic_execution_state BEFORE UPDATE OF state ON executions
WHEN NOT (
  OLD.state=NEW.state
  OR (OLD.state='CREATED' AND NEW.state IN ('PREPARED','FINAL'))
  OR (OLD.state='PREPARED' AND NEW.state IN ('AWAITING_CONFIRMATION','SEND_INTENT','FINAL'))
  OR (OLD.state='AWAITING_CONFIRMATION' AND NEW.state IN ('SEND_INTENT','FINAL'))
  OR (OLD.state='SEND_INTENT' AND NEW.state IN ('RESPONSE_OBSERVED','FINAL'))
  OR (OLD.state='RESPONSE_OBSERVED' AND NEW.state='FINAL')
)
BEGIN SELECT RAISE(ABORT,'execution state cannot reopen or skip Prepare'); END;

CREATE TRIGGER immutable_plan BEFORE UPDATE ON plans
WHEN OLD.plan_id != NEW.plan_id OR OLD.command != NEW.command
  OR OLD.plan_json != NEW.plan_json OR OLD.target_digest != NEW.target_digest
  OR OLD.material_digest != NEW.material_digest OR OLD.created_at != NEW.created_at
  OR OLD.expires_at != NEW.expires_at OR OLD.plan_format_version != NEW.plan_format_version
  OR (OLD.consumed_by IS NOT NULL AND OLD.consumed_by IS NOT NEW.consumed_by)
  OR (OLD.consumed_at IS NOT NULL AND OLD.consumed_at IS NOT NEW.consumed_at)
BEGIN SELECT RAISE(ABORT,'plan immutable or already consumed'); END;

CREATE TRIGGER monotonic_auth_flow BEFORE UPDATE OF state ON auth_flows
WHEN NOT (
  OLD.state=NEW.state
  OR (OLD.state='pending' AND NEW.state IN ('submitting','failed','expired','cancelled'))
  OR (OLD.state='submitting' AND NEW.state IN ('authenticated','failed','expired','cancelled'))
)
BEGIN SELECT RAISE(ABORT,'auth flow cannot be reopened'); END;

-- APPLICATION TRANSACTION RESPONSIBILITIES (not proved by this DDL):
-- * Normalize UUIDs to lowercase before lookup/claim; canonicalize and compare intent.
-- * CAS record_version; atomically consume plan + SEND_INTENT + append event.
-- * Check Profile, credentials, generation, plan expiry/material, adapter certification.
-- * Publish staged credential reference and flow terminal in ONE transaction.
-- * No SendPermit across restart, and lifetime OS lock before Execute/Recover coordination.
-- * Check tombstones and live records together, including atomic evidence GC/import.
-- * Maintain evidence redaction/capacity and validate actual filesystem durability.
