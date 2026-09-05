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
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

const testRequest = "35bf7b3c-44d2-4ca8-8b6d-5b40c3e6d016"

func openTest(t *testing.T, path string) *Store {
	t.Helper()
	s, e := Open(context.Background(), path)
	if e != nil {
		t.Fatal(e)
	}
	t.Cleanup(func() { s.Close() })
	return s
}
func seedProfile(t *testing.T, s *Store) {
	t.Helper()
	now := stamp()
	_, e := s.db.Exec("INSERT INTO profiles(profile_id,name,revision,auth_generation,config_json,target_digest,trust_digest,created_at,updated_at) VALUES('profile','test',1,0,'{}','target','trust',?,?)", now, now)
	if e != nil {
		t.Fatal(e)
	}
}
func setupPrepared(t *testing.T, s *Store, request, planID string) record {
	t.Helper()
	ctx := context.Background()
	r, _, e := s.claim(ctx, request, "transaction.retry.pause", "profile", `{"xid":"x"}`, `{"environment":"test"}`)
	if e != nil {
		t.Fatal(e)
	}
	p := modelPlan{planID, "transaction.retry.pause", `{"model":true}`, r.Target, "material", time.Now().UTC(), time.Now().UTC().Add(time.Minute)}
	if e = s.savePlan(ctx, p); e != nil {
		t.Fatal(e)
	}
	if e = s.prepare(ctx, r, p.ID, false); e != nil {
		t.Fatal(e)
	}
	r, e = s.get(ctx, request)
	if e != nil {
		t.Fatal(e)
	}
	return r
}
func testFence() fence {
	return fence{Revision: 1, Generation: 0, Target: "target", Trust: "trust", Material: "material", Now: time.Now().UTC()}
}
func TestEngineConstraintsAndReopen(t *testing.T) {
	path := privateTempDir(t)
	s := openTest(t, path)
	seedProfile(t, s)
	id := s.ID
	var engine string
	if e := s.db.QueryRow("SELECT sqlite_version()").Scan(&engine); e != nil {
		t.Fatal(e)
	}
	t.Log("SQLite", engine)
	r := setupPrepared(t, s, testRequest, "plan")
	if e := s.sendIntent(context.Background(), r, testFence()); e != nil {
		t.Fatal(e)
	}
	s2 := openTest(t, path)
	if s2.ID != id {
		t.Fatal("store identity changed")
	}
	got, e := s2.get(context.Background(), testRequest)
	if e != nil || got.State != "SEND_INTENT" || got.Outcome != "unknown" {
		t.Fatal(got, e)
	}
	if e = s2.sendIntent(context.Background(), got, testFence()); e == nil {
		t.Fatal("reissued send eligibility")
	}
	if _, created, e := s2.claim(context.Background(), strings.ToUpper(testRequest), "transaction.retry.pause", "profile", `{"xid":"x"}`, `{"environment":"test"}`); e != nil || created {
		t.Fatal("duplicate claim", created, e)
	}
	for _, name := range []string{"state.db", "state.db-wal", "state.db-shm"} {
		info, e := os.Stat(filepath.Join(path, name))
		if e != nil {
			t.Fatal(e)
		}
		if info.Mode().Perm() != 0600 {
			t.Fatalf("%s permissions: %o", name, info.Mode().Perm())
		}
	}
}
func TestConcurrentClaimAndConflict(t *testing.T) {
	path := privateTempDir(t)
	first := openTest(t, path)
	seedProfile(t, first)
	stores := []*Store{first, openTest(t, path), openTest(t, path), openTest(t, path)}
	var count atomic.Int32
	var wg sync.WaitGroup
	for _, s := range stores {
		wg.Add(1)
		go func() {
			defer wg.Done()
			_, created, e := s.claim(context.Background(), strings.ToUpper(testRequest), "transaction.delete", "profile", `{"xid":"x"}`, `{"target":"same"}`)
			if e != nil {
				t.Error(e)
			}
			if created {
				count.Add(1)
			}
		}()
	}
	wg.Wait()
	if count.Load() != 1 {
		t.Fatal(count.Load())
	}
	if _, _, e := first.claim(context.Background(), testRequest, "transaction.delete", "profile", `{"xid":"y"}`, `{"target":"same"}`); e != ErrConflict {
		t.Fatal(e)
	}
}
func TestSendIntentRollbackAndFences(t *testing.T) {
	s := openTest(t, privateTempDir(t))
	seedProfile(t, s)
	r := setupPrepared(t, s, testRequest, "plan")
	f := testFence()
	f.Generation = 1
	if e := s.sendIntent(context.Background(), r, f); e != ErrState {
		t.Fatal(e)
	}
	f = testFence()
	f.Now = f.Now.Add(-time.Hour)
	if e := s.sendIntent(context.Background(), r, f); e != ErrPlan {
		t.Fatal(e)
	}
	_, e := s.db.Exec("CREATE TRIGGER injected_failure BEFORE INSERT ON execution_events BEGIN SELECT RAISE(ABORT,'injected failure'); END")
	if e != nil {
		t.Fatal(e)
	}
	if e = s.sendIntent(context.Background(), r, testFence()); e == nil {
		t.Fatal("injection missed")
	}
	var consumed sql.NullString
	if e = s.db.QueryRow("SELECT consumed_by FROM plans WHERE plan_id='plan'").Scan(&consumed); e != nil || consumed.Valid {
		t.Fatal("partial plan consumption", consumed, e)
	}
	got, e := s.get(context.Background(), testRequest)
	if e != nil || got.State != "PREPARED" {
		t.Fatal(got, e)
	}
	if _, e = s.db.Exec("DROP TRIGGER injected_failure"); e != nil {
		t.Fatal(e)
	}
	if e = s.sendIntent(context.Background(), r, testFence()); e != nil {
		t.Fatal(e)
	}
}
func TestPlanCannotBeConsumedByTwoExecutions(t *testing.T) {
	s := openTest(t, privateTempDir(t))
	seedProfile(t, s)
	r := setupPrepared(t, s, testRequest, "shared")
	otherID := "8b17e861-2635-49aa-a33b-f5256155da27"
	other, _, e := s.claim(context.Background(), otherID, "transaction.retry.pause", "profile", `{"xid":"x"}`, `{"environment":"test"}`)
	if e != nil {
		t.Fatal(e)
	}
	if e = s.prepare(context.Background(), other, "shared", false); e != nil {
		t.Fatal(e)
	}
	other, e = s.get(context.Background(), otherID)
	if e != nil {
		t.Fatal(e)
	}
	var wg sync.WaitGroup
	var winners atomic.Int32
	for _, rec := range []record{r, other} {
		wg.Add(1)
		go func() {
			defer wg.Done()
			if e := s.sendIntent(context.Background(), rec, testFence()); e == nil {
				winners.Add(1)
			}
		}()
	}
	wg.Wait()
	if winners.Load() != 1 {
		t.Fatal(winners.Load())
	}
}
func TestUnsafeOrCorruptedStoreFailsClosed(t *testing.T) {
	t.Run("permissions", func(t *testing.T) {
		p := privateTempDir(t)
		os.Chmod(p, 0755)
		if _, e := Open(context.Background(), p); e != ErrUnsafe {
			t.Fatal(e)
		}
	})
	t.Run("symlink", func(t *testing.T) {
		p := privateTempDir(t)
		os.Symlink(privateTempDir(t), filepath.Join(p, "alias"))
		if _, e := Open(context.Background(), filepath.Join(p, "alias")); e != ErrUnsafe {
			t.Fatal(e)
		}
	})
	t.Run("db_symlink", func(t *testing.T) {
		p := privateTempDir(t)
		os.Symlink(filepath.Join(privateTempDir(t), "victim"), filepath.Join(p, "state.db"))
		if _, e := Open(context.Background(), p); e != ErrUnsafe {
			t.Fatal(e)
		}
	})
	t.Run("schema_tamper", func(t *testing.T) {
		p := privateTempDir(t)
		s := openTest(t, p)
		if _, e := s.db.Exec("DROP TRIGGER immutable_plan"); e != nil {
			t.Fatal(e)
		}
		if _, e := Open(context.Background(), p); e != ErrUnsafe {
			t.Fatal(e)
		}
	})
	t.Run("corrupt_bytes", func(t *testing.T) {
		p := privateTempDir(t)
		bad := []byte("not a SQLite database")
		os.WriteFile(filepath.Join(p, "state.db"), bad, 0600)
		if _, e := Open(context.Background(), p); e == nil {
			t.Fatal("accepted corrupt database")
		}
		b, _ := os.ReadFile(filepath.Join(p, "state.db"))
		if string(b) != string(bad) {
			t.Fatal("corrupt database recreated")
		}
	})
}
func TestAuthGenerationFencesLateActivation(t *testing.T) {
	s := openTest(t, privateTempDir(t))
	seedProfile(t, s)
	now := stamp()
	expiry := time.Now().Add(time.Minute).UTC().Format(time.RFC3339Nano)
	_, e := s.db.Exec("INSERT INTO credential_metadata VALUES('credential','profile',0,'keyring','staged',?,?)", now, now)
	if e != nil {
		t.Fatal(e)
	}
	_, e = s.db.Exec("INSERT INTO auth_flows(flow_id,profile_id,profile_label,auth_generation,target_digest,trust_digest,helper_instance_id,state,created_at,expires_at) VALUES('flow','profile','test',0,'target','trust','helper','submitting',?,?)", now, expiry)
	if e != nil {
		t.Fatal(e)
	}
	if e = s.logout(context.Background(), "profile"); e != nil {
		t.Fatal(e)
	}
	if e = s.activate(context.Background(), "flow", "credential", 0); e != ErrState {
		t.Fatal(e)
	}
	var ref sql.NullString
	var generation int
	if e = s.db.QueryRow("SELECT active_credential_ref,auth_generation FROM profiles").Scan(&ref, &generation); e != nil || ref.Valid || generation != 1 {
		t.Fatal(ref, generation, e)
	}
}

func TestCrashChild(t *testing.T) {
	if os.Getenv("SEATA_AI_CRASH_CHILD") == "" {
		t.Skip("subprocess only")
	}
	s, e := Open(context.Background(), os.Getenv("SEATA_AI_STORE_PATH"))
	if e != nil {
		t.Fatal(e)
	}
	r, e := s.get(context.Background(), testRequest)
	if e != nil {
		t.Fatal(e)
	}
	if os.Getenv("SEATA_AI_CRASH_CHILD") == "after" {
		if e = s.sendIntent(context.Background(), r, testFence()); e != nil {
			t.Fatal(e)
		}
	} else {
		c, e := s.db.Conn(context.Background())
		if e != nil {
			t.Fatal(e)
		}
		if _, e = c.ExecContext(context.Background(), "BEGIN IMMEDIATE"); e != nil {
			t.Fatal(e)
		}
		if _, e = c.ExecContext(context.Background(), "UPDATE plans SET consumed_by=?,consumed_at=? WHERE plan_id='plan'", r.ID, stamp()); e != nil {
			t.Fatal(e)
		}
		if _, e = c.ExecContext(context.Background(), "UPDATE executions SET state='SEND_INTENT',request_phase='request_maybe_sent',outcome='unknown',send_intent_at=? WHERE execution_id=?", stamp(), r.ID); e != nil {
			t.Fatal(e)
		}
	}
	fmt.Println("READY")
	time.Sleep(time.Hour) // Parent waits for READY, then sends SIGKILL immediately.
}
func TestActualProcessCrashAroundCommit(t *testing.T) {
	for _, where := range []string{"before", "after"} {
		t.Run(where, func(t *testing.T) {
			p := privateTempDir(t)
			s := openTest(t, p)
			seedProfile(t, s)
			setupPrepared(t, s, testRequest, "plan")
			ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
			defer cancel()
			cmd := exec.CommandContext(ctx, os.Args[0], "-test.run=^TestCrashChild$")
			cmd.Env = append(os.Environ(), "SEATA_AI_CRASH_CHILD="+where, "SEATA_AI_STORE_PATH="+p)
			pipe, e := cmd.StdoutPipe()
			if e != nil {
				t.Fatal(e)
			}
			cmd.Stderr = os.Stderr
			if e = cmd.Start(); e != nil {
				t.Fatal(e)
			}
			defer cmd.Process.Kill()
			scan := bufio.NewScanner(pipe)
			if !scan.Scan() || scan.Text() != "READY" {
				t.Fatal("child not ready")
			}
			if e = cmd.Process.Kill(); e != nil {
				t.Fatal(e)
			}
			_ = cmd.Wait()
			reopened := openTest(t, p)
			r, e := reopened.get(context.Background(), testRequest)
			if e != nil {
				t.Fatal(e)
			}
			want := "PREPARED"
			if where == "after" {
				want = "SEND_INTENT"
			}
			if r.State != want {
				t.Fatal(r)
			}
			if where == "after" {
				if r.Outcome != "unknown" {
					t.Fatal(r)
				}
				if e = reopened.sendIntent(context.Background(), r, testFence()); e == nil {
					t.Fatal("recovered a send permit")
				}
			}
		})
	}
}

func privateTempDir(t *testing.T) string {
	t.Helper()
	p := t.TempDir()
	if e := os.Chmod(p, 0700); e != nil {
		t.Fatal(e)
	}
	return p
}

func TestExistingEmptyStoreIsNotReinitialized(t *testing.T) {
	p := privateTempDir(t)
	if e := os.WriteFile(filepath.Join(p, "state.db"), nil, 0600); e != nil {
		t.Fatal(e)
	}
	if s, e := Open(context.Background(), p); e != ErrUnsafe {
		if s != nil {
			s.Close()
		}
		t.Fatal(e)
	}
}
func TestClaimChild(t *testing.T) {
	if os.Getenv("SEATA_AI_CLAIM_CHILD") == "" {
		t.Skip("subprocess only")
	}
	s, e := Open(context.Background(), os.Getenv("SEATA_AI_STORE_PATH"))
	if e != nil {
		t.Fatal(e)
	}
	defer s.Close()
	_, created, e := s.claim(context.Background(), strings.ToUpper(testRequest), "transaction.delete", "profile", `{"xid":"x"}`, `{"target":"same"}`)
	if e != nil {
		t.Fatal(e)
	}
	fmt.Printf("CREATED=%t\n", created)
}
func TestTwoProcessClaim(t *testing.T) {
	path := privateTempDir(t)
	s := openTest(t, path)
	seedProfile(t, s)
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()
	results := make(chan string, 2)
	for i := 0; i < 2; i++ {
		go func() {
			c := exec.CommandContext(ctx, os.Args[0], "-test.run=^TestClaimChild$")
			c.Env = append(os.Environ(), "SEATA_AI_CLAIM_CHILD=1", "SEATA_AI_STORE_PATH="+path)
			out, e := c.CombinedOutput()
			if e != nil {
				t.Errorf("claim child: %v %s", e, out)
			}
			results <- string(out)
		}()
	}
	winners := 0
	for i := 0; i < 2; i++ {
		if strings.Contains(<-results, "CREATED=true") {
			winners++
		}
	}
	if winners != 1 {
		t.Fatal(winners)
	}
}
func TestCredentialActivationAtomicity(t *testing.T) {
	s := openTest(t, privateTempDir(t))
	seedProfile(t, s)
	now := stamp()
	expiry := time.Now().Add(time.Minute).UTC().Format(time.RFC3339Nano)
	_, e := s.db.Exec("INSERT INTO credential_metadata VALUES('credential','profile',0,'keyring','staged',?,?)", now, now)
	if e != nil {
		t.Fatal(e)
	}
	_, e = s.db.Exec("INSERT INTO auth_flows(flow_id,profile_id,profile_label,auth_generation,target_digest,trust_digest,helper_instance_id,state,created_at,expires_at) VALUES('flow','profile','test',0,'target','trust','helper','submitting',?,?)", now, expiry)
	if e != nil {
		t.Fatal(e)
	}
	_, e = s.db.Exec("CREATE TRIGGER injected_auth_failure BEFORE UPDATE OF state ON auth_flows WHEN NEW.state='authenticated' BEGIN SELECT RAISE(ABORT,'injected failure'); END")
	if e != nil {
		t.Fatal(e)
	}
	if e = s.activate(context.Background(), "flow", "credential", 0); e == nil {
		t.Fatal("injection missed")
	}
	var ref sql.NullString
	var status string
	if e = s.db.QueryRow("SELECT active_credential_ref FROM profiles").Scan(&ref); e != nil || ref.Valid {
		t.Fatal(ref, e)
	}
	if e = s.db.QueryRow("SELECT status FROM credential_metadata").Scan(&status); e != nil || status != "staged" {
		t.Fatal(status, e)
	}
	if _, e = s.db.Exec("DROP TRIGGER injected_auth_failure"); e != nil {
		t.Fatal(e)
	}
	if e = s.activate(context.Background(), "flow", "credential", 0); e != nil {
		t.Fatal(e)
	}
	if e = s.db.QueryRow("SELECT active_credential_ref FROM profiles").Scan(&ref); e != nil || !ref.Valid || ref.String != "credential" {
		t.Fatal(ref, e)
	}
	if e = s.logout(context.Background(), "profile"); e != nil {
		t.Fatal(e)
	}
	if e = s.db.QueryRow("SELECT active_credential_ref FROM profiles").Scan(&ref); e != nil || ref.Valid {
		t.Fatal(ref, e)
	}
}
