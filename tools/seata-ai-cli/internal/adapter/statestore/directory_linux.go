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
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"syscall"
)

// The retained directory descriptor binds SQLite and its sidecars to the same
// directory even if an ancestor is renamed. Every component rejects symlinks.
func openDirectory(path string) (*os.File, string, bool, error) {
	abs, err := filepath.Abs(path)
	if err != nil {
		return nil, "", false, err
	}
	parts := strings.Split(strings.TrimPrefix(abs, "/"), "/")
	fd, err := syscall.Open("/", syscall.O_RDONLY|syscall.O_DIRECTORY|syscall.O_CLOEXEC, 0)
	if err != nil {
		return nil, "", false, err
	}
	for i, part := range parts {
		if part == "" || part == "." || part == ".." {
			syscall.Close(fd)
			return nil, "", false, ErrUnsafe
		}
		next, e := syscall.Openat(fd, part, syscall.O_RDONLY|syscall.O_DIRECTORY|syscall.O_NOFOLLOW|syscall.O_CLOEXEC, 0)
		if e == syscall.ENOENT && i == len(parts)-1 {
			if e = syscall.Mkdirat(fd, part, 0700); e == nil || e == syscall.EEXIST {
				next, e = syscall.Openat(fd, part, syscall.O_RDONLY|syscall.O_DIRECTORY|syscall.O_NOFOLLOW|syscall.O_CLOEXEC, 0)
			}
		}
		syscall.Close(fd)
		if e != nil {
			return nil, "", false, ErrUnsafe
		}
		fd = next
	}
	dir := os.NewFile(uintptr(fd), abs)
	fail := func() (*os.File, string, bool, error) { dir.Close(); return nil, "", false, ErrUnsafe }
	st, err := dir.Stat()
	if err != nil {
		return fail()
	}
	stat, ok := st.Sys().(*syscall.Stat_t)
	if !ok || stat.Uid != uint32(os.Geteuid()) || st.Mode().Perm() != 0700 {
		return fail()
	}
	var fs syscall.Statfs_t
	if syscall.Fstatfs(fd, &fs) != nil {
		return fail()
	}
	// Only local filesystems exercised by this Linux implementation are admitted
	// as experimental. This is not a certification of disk flush behavior.
	switch uint64(fs.Type) {
	case 0xef53: // The ext-family filesystem used by the Linux fault tests.
	default:
		return fail()
	}
	bound := fmt.Sprintf("/proc/self/fd/%d", fd)
	for _, name := range []string{"state.db", "state.db-wal", "state.db-shm"} {
		p := bound + "/" + name
		info, e := os.Lstat(p)
		if os.IsNotExist(e) {
			continue
		}
		if e != nil {
			return fail()
		}
		raw, ok := info.Sys().(*syscall.Stat_t)
		if !ok || !info.Mode().IsRegular() || info.Mode().Perm() != 0600 || raw.Uid != uint32(os.Geteuid()) || raw.Nlink != 1 {
			return fail()
		}
	}
	file, e := syscall.Openat(fd, "state.db", syscall.O_RDWR|syscall.O_CREAT|syscall.O_EXCL|syscall.O_NOFOLLOW|syscall.O_CLOEXEC, 0600)
	fresh := e == nil
	if e == nil {
		syscall.Close(file)
	} else if e != syscall.EEXIST {
		return fail()
	}
	return dir, bound + "/state.db", fresh, nil
}
