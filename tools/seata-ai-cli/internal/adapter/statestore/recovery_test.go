//go:build linux

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
	"bufio"
	"context"
	"database/sql"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"testing"
	"time"
)

func TestRecoveryDoesNotStealActiveExecution(t *testing.T) {
	s := openTest(t, privateTempDir(t))
	seedProfile(t, s)
	r := setupPrepared(t, s, testRequest, "plan")
	owner, err := s.AcquireExecution(context.Background(), r.ID)
	if err != nil {
		t.Fatal(err)
	}
	defer owner.Close()
	got, err := s.RecoverExecution(context.Background(), r.ID)
	if err != nil || got.Owner != "active" || got.State != "PREPARED" || got.Outcome != "not_started" {
		t.Fatal(got, err)
	}
	if _, err = s.AcquireExecution(context.Background(), r.ID); !errors.Is(err, ErrBusy) {
		t.Fatal("second owner admitted", err)
	}
	if err = owner.Close(); err != nil {
		t.Fatal(err)
	}
	got, err = s.RecoverExecution(context.Background(), r.ID)
	if err != nil || got.State != "FINAL" || got.Phase != "not_started" || got.NextAction != "new_execution_after_fix" {
		t.Fatal(got, err)
	}
	if err = s.sendIntent(context.Background(), r, testFence()); err == nil {
		t.Fatal("abandoned execution regained sending eligibility")
	}
}
func TestRecoveryPreservesConfirmationAndUnknown(t *testing.T) {
	for _, state := range []string{"AWAITING_CONFIRMATION", "SEND_INTENT"} {
		t.Run(state, func(t *testing.T) {
			s := openTest(t, privateTempDir(t))
			seedProfile(t, s)
			r := setupPrepared(t, s, testRequest, "plan")
			if state == "AWAITING_CONFIRMATION" {
				if _, err := s.db.Exec("UPDATE executions SET state='AWAITING_CONFIRMATION' WHERE execution_id=?", r.ID); err != nil {
					t.Fatal(err)
				}
			} else {
				if err := s.sendIntent(context.Background(), r, testFence()); err != nil {
					t.Fatal(err)
				}
			}
			got, err := s.RecoverExecution(context.Background(), r.ID)
			if err != nil || got.State != state {
				t.Fatal(got, err)
			}
			if state == "SEND_INTENT" && (got.Outcome != "unknown" || got.NextAction != "read_only_reconcile") {
				t.Fatal(got)
			}
			if state == "AWAITING_CONFIRMATION" && got.NextAction != "await_confirmation" {
				t.Fatal(got)
			}
			var count int
			if err = s.db.QueryRow("SELECT count(*) FROM execution_events WHERE kind='OWNER_EXITED_BEFORE_SEND'").Scan(&count); err != nil || count != 0 {
				t.Fatal(count, err)
			}
		})
	}
}
func TestRecoveryTransitionIsIdempotent(t *testing.T) {
	s := openTest(t, privateTempDir(t))
	seedProfile(t, s)
	r := setupPrepared(t, s, testRequest, "plan")
	for i := 0; i < 2; i++ {
		got, err := s.RecoverExecution(context.Background(), r.ID)
		if err != nil || got.State != "FINAL" {
			t.Fatal(got, err)
		}
	}
	var count int
	if err := s.db.QueryRow("SELECT count(*) FROM execution_events WHERE kind='OWNER_EXITED_BEFORE_SEND'").Scan(&count); err != nil || count != 1 {
		t.Fatal(count, err)
	}
}

func TestDatabaseLockWaitRespectsInvocationBudget(t *testing.T) {
	path := privateTempDir(t)
	s := openTest(t, path)
	other := openTest(t, path)
	conn, err := s.db.Conn(context.Background())
	if err != nil {
		t.Fatal(err)
	}
	defer conn.Close()
	if _, err = conn.ExecContext(context.Background(), "BEGIN IMMEDIATE"); err != nil {
		t.Fatal(err)
	}
	defer conn.ExecContext(context.Background(), "ROLLBACK")
	ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancel()
	start := time.Now()
	err = other.transaction(ctx, func(*sql.Conn) error { return nil })
	if err == nil {
		t.Fatal("unexpected lock acquisition")
	}
	if elapsed := time.Since(start); elapsed > time.Second {
		t.Fatalf("100 ms request waited %s", elapsed)
	}
}

func TestOwnerProcess(t *testing.T) {
	if os.Getenv("SEATA_AI_OWNER_CHILD") == "" {
		t.Skip("subprocess only")
	}
	s, err := Open(context.Background(), os.Getenv("SEATA_AI_STORE_PATH"))
	if err != nil {
		t.Fatal(err)
	}
	r, err := s.get(context.Background(), testRequest)
	if err != nil {
		t.Fatal(err)
	}
	owner, err := s.AcquireExecution(context.Background(), r.ID)
	if err != nil {
		t.Fatal(err)
	}
	defer owner.Close()
	fmt.Println("READY")
	time.Sleep(time.Hour) // The parent kills this process immediately after READY.
}
func TestOwnerSIGKILLReleasesOnlyLocalLock(t *testing.T) {
	path := privateTempDir(t)
	s := openTest(t, path)
	seedProfile(t, s)
	r := setupPrepared(t, s, testRequest, "plan")
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()
	cmd := exec.CommandContext(ctx, os.Args[0], "-test.run=^TestOwnerProcess$")
	cmd.Env = append(os.Environ(), "SEATA_AI_OWNER_CHILD=1", "SEATA_AI_STORE_PATH="+path)
	pipe, err := cmd.StdoutPipe()
	if err != nil {
		t.Fatal(err)
	}
	cmd.Stderr = os.Stderr
	if err = cmd.Start(); err != nil {
		t.Fatal(err)
	}
	defer cmd.Process.Kill()
	scan := bufio.NewScanner(pipe)
	if !scan.Scan() || scan.Text() != "READY" {
		t.Fatal("owner not ready")
	}
	before, err := s.RecoverExecution(context.Background(), r.ID)
	if err != nil || before.Owner != "active" || before.State != "PREPARED" {
		t.Fatal(before, err)
	}
	if err = cmd.Process.Kill(); err != nil {
		t.Fatal(err)
	}
	_ = cmd.Wait()
	after, err := s.RecoverExecution(context.Background(), r.ID)
	if err != nil || after.State != "FINAL" || after.Outcome != "not_started" {
		t.Fatal(after, err)
	}
	if err = s.sendIntent(context.Background(), r, testFence()); err == nil {
		t.Fatal("dead owner's operation was replayed")
	}
}
func TestOwnerLockCannotEscapeOrChangeInode(t *testing.T) {
	path := privateTempDir(t)
	s := openTest(t, path)
	seedProfile(t, s)
	r := setupPrepared(t, s, testRequest, "plan")
	if _, err := s.AcquireExecution(context.Background(), "../../outside"); !errors.Is(err, ErrUnsafe) {
		t.Fatal(err)
	}
	lockPath := filepath.Join(path, "owner-"+r.ID+".lock")
	if err := os.Symlink(filepath.Join(path, "victim"), lockPath); err != nil {
		t.Fatal(err)
	}
	if _, err := s.AcquireExecution(context.Background(), r.ID); !errors.Is(err, ErrUnsafe) {
		t.Fatal(err)
	}
	if _, err := os.Stat(filepath.Join(path, "victim")); !os.IsNotExist(err) {
		t.Fatal("followed lock symlink")
	}
}
