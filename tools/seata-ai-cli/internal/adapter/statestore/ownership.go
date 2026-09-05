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
	"errors"
	"os"
	"regexp"
	"sync"
)

var ErrBusy = errors.New("execution_owner_active")
var executionIDPattern = regexp.MustCompile(`^exec_[a-zA-Z0-9_-]{1,120}$`)

// ExecutionOwner is a lifetime OS lock, not a lease. No elapsed time permits
// another process to steal it. The file is never unlinked while the store lives.
type ExecutionOwner struct {
	mu    sync.Mutex
	store *Store
	id    string
	file  *os.File
}

func (o *ExecutionOwner) Close() error {
	if o == nil {
		return nil
	}
	o.mu.Lock()
	defer o.mu.Unlock()
	if o.file == nil {
		return nil
	}
	err := o.file.Close()
	o.file = nil
	return err
}
func (o *ExecutionOwner) withHeld(s *Store, id string, fn func() error) error {
	if o == nil {
		return ErrState
	}
	o.mu.Lock()
	defer o.mu.Unlock()
	if o.file == nil || o.store != s || o.id != id {
		return ErrState
	}
	return fn()
}
func (s *Store) AcquireExecution(ctx context.Context, id string) (*ExecutionOwner, error) {
	if !executionIDPattern.MatchString(id) {
		return nil, ErrUnsafe
	}
	c, err := s.db.Conn(ctx)
	if err != nil {
		return nil, err
	}
	if err = checkConnection(ctx, c); err != nil {
		c.Close()
		return nil, err
	}
	_, err = scanRecord(c.QueryRowContext(ctx, "SELECT "+recordColumns+" FROM executions WHERE execution_id=?", id))
	c.Close()
	if err != nil {
		return nil, err
	}
	if err = ctx.Err(); err != nil {
		return nil, err
	}
	f, err := lockExecutionFile(s.dir, id)
	if err != nil {
		return nil, err
	}
	return &ExecutionOwner{store: s, id: id, file: f}, nil
}
