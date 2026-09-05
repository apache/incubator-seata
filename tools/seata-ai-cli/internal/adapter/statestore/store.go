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
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	_ "embed"
	"encoding/hex"
	"errors"
	"fmt"
	"net/url"
	"os"
	"strings"
	"sync"
	"time"

	_ "modernc.org/sqlite"
)

//go:embed schema.sql
var schemaSQL string

var (
	ErrUnsafe   = errors.New("store_unsafe")
	ErrConflict = errors.New("request_id_conflict")
	ErrState    = errors.New("state_changed")
	ErrPlan     = errors.New("plan_consumed_or_changed")
	ErrNotFound = errors.New("record_not_found")
)

type Store struct {
	db  *sql.DB
	dir *os.File
	ID  string
}

func ID(prefix string) string {
	b := make([]byte, 16)
	if _, e := rand.Read(b); e != nil {
		panic(e)
	}
	return prefix + hex.EncodeToString(b)
}
func stamp() string          { return time.Now().UTC().Format(time.RFC3339Nano) }
func digest(b []byte) string { h := sha256.Sum256(b); return "sha256:" + hex.EncodeToString(h[:]) }

// Open initializes an empty store atomically; existing invalid databases are
// rejected, never removed or recreated. The caller supplies a local private path.
func Open(ctx context.Context, path string) (s *Store, err error) {
	dir, bound, fresh, err := openDirectory(path)
	if err != nil {
		return nil, err
	}
	dsn := (&url.URL{Scheme: "file", Path: bound}).String() + "?_pragma=foreign_keys(1)&_pragma=synchronous(2)&_pragma=busy_timeout(5000)"
	db, err := sql.Open("sqlite", dsn)
	if err != nil {
		dir.Close()
		return nil, err
	}
	db.SetMaxOpenConns(1)
	db.SetMaxIdleConns(1)
	s = &Store{db: db, dir: dir}
	opened := s
	defer func() {
		if err != nil {
			opened.Close()
		}
	}()
	c, err := db.Conn(ctx)
	if err != nil {
		return nil, err
	}
	defer c.Close()
	var journal string
	if err = c.QueryRowContext(ctx, "PRAGMA journal_mode=WAL").Scan(&journal); err != nil || journal != "wal" {
		return nil, ErrUnsafe
	}
	if err = checkConnection(ctx, c); err != nil {
		return nil, err
	}
	if _, err = c.ExecContext(ctx, "BEGIN IMMEDIATE"); err != nil {
		return nil, err
	}
	defer c.ExecContext(context.Background(), "ROLLBACK")
	var count int
	if err = c.QueryRowContext(ctx, "SELECT count(*) FROM sqlite_schema WHERE type='table' AND name NOT LIKE 'sqlite_%'").Scan(&count); err != nil {
		return nil, err
	}
	if count == 0 {
		if !fresh {
			return nil, ErrUnsafe
		}
		// Connection PRAGMAs precede the transactional DDL; journal_mode cannot be
		// switched inside an active transaction.
		ddl := schemaSQL[strings.Index(schemaSQL, "CREATE TABLE"):]
		if _, err = c.ExecContext(ctx, ddl); err != nil {
			return nil, err
		}
		if _, err = c.ExecContext(ctx, "INSERT INTO store_meta VALUES(1,?,1,?,'normal')", ID("store_"), stamp()); err != nil {
			return nil, err
		}
	}
	var version int
	var mode string
	if err = c.QueryRowContext(ctx, "SELECT state_store_id,schema_version,recovery_mode FROM store_meta WHERE singleton=1").Scan(&s.ID, &version, &mode); err != nil || version != 1 || mode != "normal" || s.ID == "" {
		return nil, ErrUnsafe
	}
	expected, e := expectedSchema()
	if e != nil {
		return nil, e
	}
	actual, e := schemaFingerprint(ctx, c)
	if e != nil || actual != expected {
		return nil, ErrUnsafe
	}
	var check string
	if err = c.QueryRowContext(ctx, "PRAGMA quick_check").Scan(&check); err != nil || check != "ok" {
		return nil, ErrUnsafe
	}
	rows, e := c.QueryContext(ctx, "PRAGMA foreign_key_check")
	if e != nil {
		return nil, e
	}
	bad := rows.Next()
	e = rows.Err()
	rows.Close()
	if bad || e != nil {
		return nil, ErrUnsafe
	}
	_, err = c.ExecContext(ctx, "COMMIT")
	if err != nil {
		return nil, err
	}
	return s, nil
}
func (s *Store) Close() error {
	err := s.db.Close()
	e := s.dir.Close()
	if err != nil {
		return err
	}
	return e
}
func checkConnection(ctx context.Context, c *sql.Conn) error {
	for p, want := range map[string]string{"journal_mode": "wal", "synchronous": "2", "foreign_keys": "1", "busy_timeout": "5000"} {
		var got string
		if err := c.QueryRowContext(ctx, "PRAGMA "+p).Scan(&got); err != nil || got != want {
			return ErrUnsafe
		}
	}
	return nil
}
func (s *Store) transaction(ctx context.Context, fn func(*sql.Conn) error) error {
	c, err := s.db.Conn(ctx)
	if err != nil {
		return err
	}
	defer c.Close()
	if err = checkConnection(ctx, c); err != nil {
		return err
	}
	if _, err = c.ExecContext(ctx, "BEGIN IMMEDIATE"); err != nil {
		return err
	}
	defer c.ExecContext(context.Background(), "ROLLBACK")
	if err = fn(c); err != nil {
		return err
	}
	_, err = c.ExecContext(ctx, "COMMIT")
	return err
}

var schemaOnce sync.Once
var expectedHash string
var expectedErr error

func expectedSchema() (string, error) {
	schemaOnce.Do(func() {
		db, e := sql.Open("sqlite", ":memory:")
		if e != nil {
			expectedErr = e
			return
		}
		defer db.Close()
		c, e := db.Conn(context.Background())
		if e != nil {
			expectedErr = e
			return
		}
		defer c.Close()
		if _, e = c.ExecContext(context.Background(), schemaSQL); e != nil {
			expectedErr = e
			return
		}
		expectedHash, expectedErr = schemaFingerprint(context.Background(), c)
	})
	return expectedHash, expectedErr
}
func schemaFingerprint(ctx context.Context, c *sql.Conn) (string, error) {
	rows, e := c.QueryContext(ctx, "SELECT type,name,tbl_name,sql FROM sqlite_schema WHERE name NOT LIKE 'sqlite_%' ORDER BY name")
	if e != nil {
		return "", e
	}
	defer rows.Close()
	var b strings.Builder
	for rows.Next() {
		var kind, name, table, sql string
		if e = rows.Scan(&kind, &name, &table, &sql); e != nil {
			return "", e
		}
		fmt.Fprintf(&b, "%s\x00%s\x00%s\x00%s\n", kind, name, table, sql)
	}
	return digest([]byte(b.String())), rows.Err()
}
