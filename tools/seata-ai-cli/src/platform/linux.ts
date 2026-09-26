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
  lstatSync,
  statfsSync,
  existsSync,
} from "node:fs";
import { resolve, parse, sep } from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { CliError } from "../protocol/error.js";

export function darwinPath(input: string): string {
  const path = resolve(input);
  // macOS uses these fixed system links. Canonicalize only these roots; user
  // supplied links anywhere else are rejected during component traversal.
  for (const root of ["/var", "/tmp", "/etc"])
    if (path === root || path.startsWith(root + "/")) return "/private" + path;
  return path;
}

function localFilesystem(path: string): boolean {
  const type = statfsSync(path).type;
  return process.platform === "darwin" ? type === 26 :
    [0xef53, 0x01021994, 0x9123683e, 0x58465342, 0x794c7630].includes(type);
}

/** Private state directory with Linux descriptor traversal and verified APFS traversal. */
export class PrivateDirectory {
  readonly fd: number;
  readonly path: string;
  constructor(input: string, create = true) {
    if (!["linux", "darwin"].includes(process.platform) || !process.getuid)
      throw new CliError(
        "policy",
        "store_unsafe",
        "Private storage has no adapter for this platform.",
      );
    if (!input || input.length > 4096 || /[\x00-\x1f\x7f]/.test(input))
      throw new CliError("policy", "unsafe_path", "Invalid private directory path.");
    const path = process.platform === "darwin" ? darwinPath(input) : resolve(input);
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
        const next = process.platform === "darwin"
          ? resolve(parse(path).root, ...parts.slice(0, i + 1))
          : "/proc/self/fd/" + fd + "/" + parts[i];
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
        if (process.platform === "darwin" && lstatSync(next).isSymbolicLink())
          throw new CliError("policy", "unsafe_path", "Private directory contains a symbolic link.");
        const child = openSync(
          next,
          constants.O_RDONLY | constants.O_DIRECTORY | constants.O_NOFOLLOW,
        );
        closeSync(fd);
        fd = child;
        const stat = fstatSync(fd);
        if (process.platform === "darwin") {
          const named = lstatSync(next);
          if (named.dev !== stat.dev || named.ino !== stat.ino || !named.isDirectory())
            throw new CliError("policy", "unsafe_path", "Private directory changed during traversal.");
        }
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
      if (!localFilesystem(process.platform === "darwin" ? path : "/proc/self/fd/" + fd))
        throw new CliError(
          "policy",
          "store_unsafe",
          "Filesystem has no enabled private-storage adapter.",
        );
      this.fd = fd;
      this.path = process.platform === "darwin" ? path : "/proc/self/fd/" + fd;
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
  assertCurrent(): void {
    if (process.platform !== "darwin") return;
    let prefix = "/";
    const parts = this.path.split("/").filter(Boolean);
    for (const [index, part] of parts.entries()) {
      prefix = resolve(prefix, part);
      const named = lstatSync(prefix);
      const final = index === parts.length - 1;
      const unsafeMode = final ? (named.mode & 0o077) !== 0 :
        (named.mode & 0o022) !== 0 && (named.mode & 0o1000) === 0;
      const unsafeOwner = final ? named.uid !== process.getuid!() :
        named.uid !== 0 && named.uid !== process.getuid!();
      if (named.isSymbolicLink() || !named.isDirectory() || unsafeMode || unsafeOwner)
        throw new CliError("policy", "store_unsafe", "Private directory path changed.");
    }
    const named = lstatSync(this.path), opened = fstatSync(this.fd);
    if (named.dev !== opened.dev || named.ino !== opened.ino || !localFilesystem(this.path))
      throw new CliError("policy", "store_unsafe", "Private directory identity changed.");
  }
  file(name: string, create = true): number {
    if (!/^[a-zA-Z0-9_.-]+$/.test(name) || name === "." || name === "..")
      throw new CliError("policy", "unsafe_path", "Invalid internal filename.");
    let fd: number | undefined;
    try {
      this.assertCurrent();
      fd = openSync(
        this.path + "/" + name,
        constants.O_RDWR |
          constants.O_NOFOLLOW |
          constants.O_NONBLOCK |
          (create ? constants.O_CREAT : 0),
        0o600,
      );
      this.assertCurrent();
    } catch {
      if (fd !== undefined) closeSync(fd);
      throw new CliError(
        "policy",
        "unsafe_path",
        "Cannot securely open state file.",
      );
    }
    const stat = fstatSync(fd);
    if (process.platform === "darwin") {
      let named: ReturnType<typeof lstatSync>;
      try { named = lstatSync(this.path + "/" + name); }
      catch {
        closeSync(fd);
        throw new CliError("policy", "store_unsafe", "State file path disappeared.");
      }
      if (named.isSymbolicLink() || named.dev !== stat.dev || named.ino !== stat.ino) {
        closeSync(fd);
        throw new CliError("policy", "store_unsafe", "State file identity changed.");
      }
    }
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
    this.assertCurrent();
    for (const name of ["state.db-wal", "state.db-shm"])
      if (existsSync(this.path + "/" + name)) closeSync(this.file(name, false));
  }
  lock(name: string, shared = false): number {
    const fd = this.file(name);
    // flock(2) locks the inherited open file description; parent retains it until close.
    const darwin = process.platform === "darwin";
    const helper = fileURLToPath(new URL("../native/seata-darwin-helper", import.meta.url));
    const result = spawnSync(darwin ? helper : "/usr/bin/flock", darwin
      ? ["lock", shared ? "shared" : "exclusive"] : [shared ? "-sn" : "-xn", "3"], {
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
