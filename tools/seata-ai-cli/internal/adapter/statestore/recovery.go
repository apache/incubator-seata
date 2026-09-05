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
	"errors"
)

// Recovery contains only durable local facts. A successful lookup never proves
// that a distributed transaction or a management request completed remotely.
type Recovery struct {
	ExecutionID     string `json:"execution_id"`
	ClientRequestID string `json:"client_request_id"`
	StateStoreID    string `json:"state_store_id"`
	State           string `json:"state"`
	Phase           string `json:"request_phase"`
	Outcome         string `json:"outcome"`
	RecordVersion   int    `json:"record_version"`
	Owner           string `json:"owner"`
	NextAction      string `json:"next_action"`
}

func (s *Store) RecoverExecution(ctx context.Context, id string) (Recovery, error) {
	if !executionIDPattern.MatchString(id) {
		return Recovery{}, ErrUnsafe
	}
	owner, err := s.AcquireExecution(ctx, id)
	active := errors.Is(err, ErrBusy)
	if err != nil && !active {
		return Recovery{}, err
	}
	if owner != nil {
		defer owner.Close()
	}
	var result Recovery
	// A busy owner gets a read-only snapshot without waiting for a write lock.
	if active {
		c, err := s.db.Conn(ctx)
		if err != nil {
			return result, err
		}
		defer c.Close()
		if err = checkConnection(ctx, c); err != nil {
			return result, err
		}
		r, err := scanRecord(c.QueryRowContext(ctx, "SELECT "+recordColumns+" FROM executions WHERE execution_id=?", id))
		if err != nil {
			return result, err
		}
		return s.recovery(r, "active"), nil
	}
	err = owner.withHeld(s, id, func() error {
		return s.transaction(ctx, func(c *sql.Conn) error {
			r, err := scanRecord(c.QueryRowContext(ctx, "SELECT "+recordColumns+" FROM executions WHERE execution_id=?", id))
			if err != nil {
				return err
			}
			if r.State == "CREATED" || r.State == "PREPARED" {
				now := stamp()
				res, err := c.ExecContext(ctx, `UPDATE executions SET state='FINAL',terminal_at=?,updated_at=?,record_version=record_version+1
WHERE execution_id=? AND record_version=? AND state IN ('CREATED','PREPARED') AND send_intent_at IS NULL AND request_phase='not_started'`, now, now, id, r.Version)
				if err != nil {
					return err
				}
				if n, err := res.RowsAffected(); err != nil || n != 1 {
					return ErrState
				}
				if err = appendEvent(ctx, c, id, "OWNER_EXITED_BEFORE_SEND", `{"outcome":"not_started"}`); err != nil {
					return err
				}
				r.State = "FINAL"
				r.Version++
			}
			result = s.recovery(r, "inactive")
			return nil
		})
	})
	return result, err
}
func (s *Store) recovery(r record, owner string) Recovery {
	next := "read_only_reconcile"
	switch {
	case r.State == "FINAL" && (r.Outcome == "not_started" || r.Outcome == "not_applied"):
		next = "new_execution_after_fix"
	case r.State == "FINAL" && r.Outcome == "applied":
		next = "none"
	case owner == "active":
		next = "read_only_poll"
	case r.State == "AWAITING_CONFIRMATION":
		next = "await_confirmation"
	}
	return Recovery{r.ID, r.RequestID, s.ID, r.State, r.Phase, r.Outcome, r.Version, owner, next}
}
