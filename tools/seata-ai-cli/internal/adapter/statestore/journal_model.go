/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package statestore

import (
	"context"
	"database/sql"
	"encoding/json"
	"regexp"
	"strings"
	"time"

	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
)

// This package-private model exercises the persistence barrier in M1. It does
// not issue a SendPermit or make a network request. M5 must add the complete
// executor, redaction, capacity and compatibility gates
// before connecting these transitions to any management handler.
var requestIDPattern = regexp.MustCompile(`^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`)

type record struct {
	ID, RequestID, Intent, Target, State, Phase, Outcome, PlanID string
	Version                                                      int
}

func scanRecord(row *sql.Row) (r record, err error) {
	err = row.Scan(&r.ID, &r.RequestID, &r.Intent, &r.Target, &r.State, &r.Phase, &r.Outcome, &r.PlanID, &r.Version)
	if err == sql.ErrNoRows {
		err = ErrNotFound
	}
	return
}

const recordColumns = "execution_id,client_request_id,intent_digest,target_digest,state,request_phase,outcome,coalesce(plan_id,''),record_version"

func canonical(raw string) (string, error) {
	v, e := protocol.Decode(strings.NewReader(raw))
	if e != nil {
		return "", e
	}
	if _, ok := v.(map[string]any); !ok {
		return "", ErrState
	}
	b, e := json.Marshal(v)
	return string(b), e
}
func (s *Store) claim(ctx context.Context, req, command, profile, intent, target string) (r record, created bool, err error) {
	req = strings.ToLower(req)
	if !requestIDPattern.MatchString(req) {
		return r, false, ErrState
	}
	intent, err = canonical(intent)
	if err != nil {
		return
	}
	target, err = canonical(target)
	if err != nil {
		return
	}
	ih, th := digest([]byte(intent)), digest([]byte(target))
	err = s.transaction(ctx, func(c *sql.Conn) error {
		existing, e := scanRecord(c.QueryRowContext(ctx, "SELECT "+recordColumns+" FROM executions WHERE client_request_id=?", req))
		if e == nil {
			if existing.Intent != ih || existing.Target != th {
				return ErrConflict
			}
			var cmd, p string
			if e = c.QueryRowContext(ctx, "SELECT command,profile_id FROM executions WHERE execution_id=?", existing.ID).Scan(&cmd, &p); e != nil {
				return e
			}
			if cmd != command || p != profile {
				return ErrConflict
			}
			r = existing
			return nil
		}
		if e != ErrNotFound {
			return e
		}
		var tombstone int
		if e = c.QueryRowContext(ctx, "SELECT count(*) FROM request_tombstones WHERE client_request_id=?", req).Scan(&tombstone); e != nil {
			return e
		}
		if tombstone != 0 {
			return ErrConflict
		}
		now := stamp()
		id := ID("exec_")
		_, e = c.ExecContext(ctx, "INSERT INTO executions(execution_id,client_request_id,command,intent_digest,intent_json,target_digest,target_json,profile_id,state,request_phase,outcome,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,'CREATED','not_started','not_started',?,?)", id, req, command, ih, intent, th, target, profile, now, now)
		if e != nil {
			return e
		}
		r = record{id, req, ih, th, "CREATED", "not_started", "not_started", "", 1}
		created = true
		return nil
	})
	if err != nil {
		created = false
	}
	return
}
func (s *Store) get(ctx context.Context, req string) (r record, err error) {
	req = strings.ToLower(req)
	if !requestIDPattern.MatchString(req) {
		return r, ErrState
	}
	c, e := s.db.Conn(ctx)
	if e != nil {
		return r, e
	}
	defer c.Close()
	if e = checkConnection(ctx, c); e != nil {
		return r, e
	}
	return scanRecord(c.QueryRowContext(ctx, "SELECT "+recordColumns+" FROM executions WHERE client_request_id=?", req))
}

type modelPlan struct {
	ID, Command, JSON, Target, Material string
	Created, Expires                    time.Time
}

func (s *Store) savePlan(ctx context.Context, p modelPlan) error {
	if p.ID == "" || p.Target == "" || p.Material == "" || !p.Expires.After(p.Created) || p.Expires.Sub(p.Created) > 120*time.Second {
		return ErrPlan
	}
	j, e := canonical(p.JSON)
	if e != nil {
		return e
	}
	return s.transaction(ctx, func(c *sql.Conn) error {
		_, e := c.ExecContext(ctx, "INSERT INTO plans VALUES(?,1,?,?,?,?,?,?,NULL,NULL)", p.ID, p.Command, j, p.Target, p.Material, p.Created.UTC().Format(time.RFC3339Nano), p.Expires.UTC().Format(time.RFC3339Nano))
		return e
	})
}
func (s *Store) prepare(ctx context.Context, r record, plan string, confirmation bool) error {
	return s.transaction(ctx, func(c *sql.Conn) error {
		var target, cmd, execCmd string
		if e := c.QueryRowContext(ctx, "SELECT target_digest,command FROM plans WHERE plan_id=? AND consumed_by IS NULL", plan).Scan(&target, &cmd); e != nil {
			return ErrPlan
		}
		if e := c.QueryRowContext(ctx, "SELECT command FROM executions WHERE execution_id=?", r.ID).Scan(&execCmd); e != nil {
			return e
		}
		if target != r.Target || cmd != execCmd {
			return ErrPlan
		}
		res, e := c.ExecContext(ctx, "UPDATE executions SET state='PREPARED',plan_id=?,record_version=record_version+1,updated_at=? WHERE execution_id=? AND record_version=? AND state='CREATED'", plan, stamp(), r.ID, r.Version)
		if e != nil {
			return e
		}
		if n, _ := res.RowsAffected(); n != 1 {
			return ErrState
		}
		if confirmation {
			_, e = c.ExecContext(ctx, "UPDATE executions SET state='AWAITING_CONFIRMATION',record_version=record_version+1 WHERE execution_id=?", r.ID)
		}
		return e
	})
}

type fence struct {
	Revision, Generation    int
	Target, Trust, Material string
	Now                     time.Time
	Confirmed               bool
}

func (s *Store) sendIntent(ctx context.Context, r record, f fence) error {
	owner, err := s.AcquireExecution(ctx, r.ID)
	if err != nil {
		return err
	}
	defer owner.Close()
	return s.sendIntentOwned(ctx, owner, r, f)
}
func (s *Store) sendIntentOwned(ctx context.Context, owner *ExecutionOwner, r record, f fence) error {
	return owner.withHeld(s, r.ID, func() error {
		return s.transaction(ctx, func(c *sql.Conn) error {
			var version, revision, generation int
			var state, plan, created, expires, material, target, trust, profileTarget, profile string
			e := c.QueryRowContext(ctx, `SELECT e.state,e.record_version,e.plan_id,p.created_at,p.expires_at,p.material_digest,p.target_digest,pr.revision,pr.auth_generation,pr.target_digest,pr.trust_digest,pr.profile_id
FROM executions e JOIN plans p ON e.plan_id=p.plan_id JOIN profiles pr ON e.profile_id=pr.profile_id
WHERE e.execution_id=? AND pr.deleted_at IS NULL AND p.consumed_by IS NULL`, r.ID).Scan(&state, &version, &plan, &created, &expires, &material, &target, &revision, &generation, &profileTarget, &trust, &profile)
			if e != nil {
				return ErrPlan
			}
			if (state != "PREPARED" && state != "AWAITING_CONFIRMATION") || version != r.Version {
				return ErrState
			}
			if state == "AWAITING_CONFIRMATION" && !f.Confirmed {
				return ErrState
			}
			a, ea := time.Parse(time.RFC3339Nano, created)
			b, eb := time.Parse(time.RFC3339Nano, expires)
			if ea != nil || eb != nil || f.Now.Before(a) || !f.Now.Before(b) || material != f.Material || target != r.Target {
				return ErrPlan
			}
			if revision != f.Revision || generation != f.Generation || profileTarget != f.Target || trust != f.Trust {
				return ErrState
			}
			now := stamp()
			res, e := c.ExecContext(ctx, "UPDATE plans SET consumed_by=?,consumed_at=? WHERE plan_id=? AND consumed_by IS NULL", r.ID, now, plan)
			if e != nil {
				return e
			}
			if n, _ := res.RowsAffected(); n != 1 {
				return ErrPlan
			}
			res, e = c.ExecContext(ctx, "UPDATE executions SET state='SEND_INTENT',request_phase='request_maybe_sent',outcome='unknown',send_intent_at=?,record_version=record_version+1,updated_at=? WHERE execution_id=? AND record_version=?", now, now, r.ID, r.Version)
			if e != nil {
				return e
			}
			if n, _ := res.RowsAffected(); n != 1 {
				return ErrState
			}
			return appendEvent(ctx, c, r.ID, "SEND_INTENT", `{"outcome":"unknown"}`)
		})
	})
}
func appendEvent(ctx context.Context, c *sql.Conn, id, kind, payload string) error {
	var seq int
	if e := c.QueryRowContext(ctx, "SELECT coalesce(max(sequence),0)+1 FROM execution_events WHERE execution_id=?", id).Scan(&seq); e != nil {
		return e
	}
	_, e := c.ExecContext(ctx, "INSERT INTO execution_events VALUES(?,?,?,?,?,?)", id, seq, kind, payload, digest([]byte(payload)), stamp())
	return e
}

// Staged credential activation is atomic with the flow's terminal transition.
// Secret material never enters this model; Keyring writes are an M3 concern.
func (s *Store) activate(ctx context.Context, flow, credential string, generation int) error {
	return s.transaction(ctx, func(c *sql.Conn) error {
		var profile string
		e := c.QueryRowContext(ctx, `SELECT f.profile_id FROM auth_flows f JOIN profiles p ON f.profile_id=p.profile_id
JOIN credential_metadata m ON m.profile_id=p.profile_id AND m.credential_ref=?
WHERE f.flow_id=? AND f.state='submitting' AND f.auth_generation=? AND p.auth_generation=? AND m.auth_generation=?
AND m.status='staged' AND p.deleted_at IS NULL AND p.target_digest=f.target_digest AND p.trust_digest=f.trust_digest
AND julianday(f.expires_at)>julianday(?)`, credential, flow, generation, generation, generation, stamp()).Scan(&profile)
		if e != nil {
			return ErrState
		}
		now := stamp()
		if _, e = c.ExecContext(ctx, "UPDATE credential_metadata SET status='retired',updated_at=? WHERE credential_ref=(SELECT active_credential_ref FROM profiles WHERE profile_id=?)", now, profile); e != nil {
			return e
		}
		if _, e = c.ExecContext(ctx, "UPDATE credential_metadata SET status='active',updated_at=? WHERE credential_ref=?", now, credential); e != nil {
			return e
		}
		if _, e = c.ExecContext(ctx, "UPDATE profiles SET active_credential_ref=?,updated_at=? WHERE profile_id=?", credential, now, profile); e != nil {
			return e
		}
		_, e = c.ExecContext(ctx, "UPDATE auth_flows SET state='authenticated',terminal_at=?,record_version=record_version+1 WHERE flow_id=?", now, flow)
		return e
	})
}
func (s *Store) logout(ctx context.Context, profile string) error {
	return s.transaction(ctx, func(c *sql.Conn) error {
		now := stamp()
		if _, e := c.ExecContext(ctx, "UPDATE credential_metadata SET status='cleanup_pending',updated_at=? WHERE profile_id=? AND status IN ('active','staged')", now, profile); e != nil {
			return e
		}
		res, e := c.ExecContext(ctx, "UPDATE profiles SET auth_generation=auth_generation+1,active_credential_ref=NULL,updated_at=? WHERE profile_id=? AND deleted_at IS NULL", now, profile)
		if e != nil {
			return e
		}
		if n, _ := res.RowsAffected(); n != 1 {
			return ErrNotFound
		}
		_, e = c.ExecContext(ctx, "UPDATE auth_flows SET state='cancelled',terminal_at=?,record_version=record_version+1 WHERE profile_id=? AND state IN ('pending','submitting')", now, profile)
		return e
	})
}
