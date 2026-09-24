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
import {
  constants,
  openSync,
  closeSync,
  fstatSync,
  mkdirSync,
  statfsSync,
  existsSync,
} from "node:fs";
import { resolve, parse, sep } from "node:path";
import { spawnSync } from "node:child_process";
import { CliError } from "../protocol/error.js";

/** Linux-only descriptor-relative traversal. Unsupported platforms fail closed. */
export class PrivateDirectory {
  readonly fd: number;
  readonly path: string;
  constructor(input: string, create = true) {
    if (process.platform !== "linux" || !process.getuid)
      throw new CliError(
        "policy",
        "store_unsafe",
        "Private storage is currently implemented only for Linux.",
      );
    const path = resolve(input);
    const parts = path
      .slice(parse(path).root.length)
      .split(sep)
      .filter(Boolean);
    if (parts.length === 0)
      throw new CliError(
        "policy",
        "unsafe_path",
        "The filesystem root cannot be a state directory.",
      );
    let fd = openSync(
      "/",
      constants.O_RDONLY | constants.O_DIRECTORY | constants.O_NOFOLLOW,
    );
    try {
      for (let i = 0; i < parts.length; i++) {
        const next = "/proc/self/fd/" + fd + "/" + parts[i];
        if (create) {
          try {
            mkdirSync(next, { mode: 0o700 });
          } catch (e) {
            if (
              !(
                e &&
                typeof e === "object" &&
                "code" in e &&
                e.code === "EEXIST"
              )
            )
              throw e;
          }
        }
        const child = openSync(
          next,
          constants.O_RDONLY | constants.O_DIRECTORY | constants.O_NOFOLLOW,
        );
        closeSync(fd);
        fd = child;
        const stat = fstatSync(fd);
        const final = i === parts.length - 1;
        if (
          final
            ? stat.uid !== process.getuid() || (stat.mode & 0o077) !== 0
            : (stat.uid !== 0 && stat.uid !== process.getuid()) ||
              ((stat.mode & 0o022) !== 0 && (stat.mode & 0o1000) === 0)
        )
          throw new CliError(
            "policy",
            "store_unsafe",
            "State directory ownership or permissions are unsafe.",
          );
      }
      // ext, tmpfs, btrfs, xfs, overlay; no network or unrecognized filesystems.
      if (
        ![0xef53, 0x01021994, 0x9123683e, 0x58465342, 0x794c7630].includes(
          statfsSync("/proc/self/fd/" + fd).type,
        )
      )
        throw new CliError(
          "policy",
          "store_unsafe",
          "Filesystem has no enabled private-storage adapter.",
        );
      this.fd = fd;
      this.path = "/proc/self/fd/" + fd;
    } catch (error) {
      closeSync(fd);
      if (error instanceof CliError) throw error;
      throw new CliError(
        "policy",
        "unsafe_path",
        "Cannot securely open the state directory.",
      );
    }
  }
  file(name: string, create = true): number {
    if (!/^[a-zA-Z0-9_.-]+$/.test(name) || name === "." || name === "..")
      throw new CliError("policy", "unsafe_path", "Invalid internal filename.");
    let fd: number;
    try {
      fd = openSync(
        this.path + "/" + name,
        constants.O_RDWR |
          constants.O_NOFOLLOW |
          (create ? constants.O_CREAT : 0),
        0o600,
      );
    } catch {
      throw new CliError(
        "policy",
        "unsafe_path",
        "Cannot securely open state file.",
      );
    }
    const stat = fstatSync(fd);
    if (
      !stat.isFile() ||
      stat.uid !== process.getuid?.() ||
      (stat.mode & 0o077) !== 0 ||
      stat.nlink !== 1
    ) {
      closeSync(fd);
      throw new CliError(
        "policy",
        "store_unsafe",
        "State file ownership, links or permissions are unsafe.",
      );
    }
    return fd;
  }
  checkAuxiliary(): void {
    for (const name of ["state.db-wal", "state.db-shm"])
      if (existsSync(this.path + "/" + name)) closeSync(this.file(name, false));
  }
  lock(name: string, shared = false): number {
    const fd = this.file(name);
    // flock(2) locks the inherited open file description; parent retains it until close.
    const result = spawnSync("/usr/bin/flock", [shared ? "-sn" : "-xn", "3"], {
      stdio: ["ignore", "ignore", "ignore", fd],
      timeout: 5000,
      env: { PATH: "/usr/bin:/bin", LANG: "C" },
    });
    if (result.status !== 0) {
      closeSync(fd);
      throw new CliError(
        "conflict",
        "store_busy",
        "OS lifetime lock unavailable or held by another process.",
      );
    }
    return fd;
  }
  close(): void {
    closeSync(this.fd);
  }
}
