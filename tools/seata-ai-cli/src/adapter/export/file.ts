// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
import { closeSync, constants, fchmodSync, fstatSync, fsyncSync, ftruncateSync, lstatSync, openSync, readSync, writeSync } from "node:fs";
import { basename, dirname, isAbsolute, relative, resolve } from "node:path";
import { randomUUID } from "node:crypto";
import { PrivateDirectory } from "../../platform/linux.js";
import { StateStore, stateDirectory as defaultStateDirectory } from "../statestore/client.js";
import { CliError } from "../../protocol/error.js";
import { type Obj, digest, isObject, object } from "../../protocol/value.js";
import { bytesDigest } from "./archive.js";

const failure = (subtype: string) => new CliError("policy", subtype, "Export target is not a private, unchanged CLI-owned regular file.");
export class ExportFile {
  readonly absolutePath: string;
  private readonly directory: PrivateDirectory;
  private readonly name: string;
  private readonly key: string;
  private readonly store: StateStore;
  private fd: number | undefined;
  private record: Obj | undefined;
  private prepared = false;
  private changed = false;
  constructor(root: string, path: string, private readonly overwrite: boolean, stateDirectory?: string, budgetMs = 120000,
    private readonly sharedStore?: StateStore) {
    if (!root || /[\x00-\x1f\x7f]/.test(root) || !path || path.length > 4096 || isAbsolute(path) || /[\\:\x00-\x1f\x7f]/.test(path) ||
        path.split("/").some(part => !part || part === "." || part === ".."))
      throw new CliError("validation", "unsafe_export_path", "Export path must be a relative path without traversal or ambiguous separators.");
    this.absolutePath = resolve(root, path);
    const stateRelative = relative(resolve(stateDirectory ?? defaultStateDirectory()), this.absolutePath);
    if (/^state\.db(?:-wal|-shm)?$|^[^/]+\.lock$/.test(stateRelative) || stateRelative === "credentials" || stateRelative.startsWith("credentials/"))
      throw new CliError("policy", "export_reserved_target", "Exports cannot target internal state, lock or credential files.");
    const rootDirectory = new PrivateDirectory(root);
    try {
      this.directory = new PrivateDirectory(dirname(this.absolutePath));
    } finally { rootDirectory.close(); }
    this.name = basename(path); this.key = digest(this.absolutePath);
    this.store = sharedStore ?? new StateStore(stateDirectory, budgetMs);
  }
  private get path(): string { return this.directory.path + "/" + this.name; }
  private verifyFile(): ReturnType<typeof fstatSync> {
    if (this.fd === undefined) throw failure("export_not_open");
    const stat = fstatSync(this.fd), named = lstatSync(this.path);
    if (!stat.isFile() || stat.uid !== process.getuid?.() || stat.nlink !== 1 || (stat.mode & 0o777) !== 0o600 ||
        named.isSymbolicLink() || named.dev !== stat.dev || named.ino !== stat.ino || stat.size > 16 * 1024 * 1024)
      throw failure("export_target_changed");
    const current = new PrivateDirectory(dirname(this.absolutePath), false);
    try {
      const a = fstatSync(current.fd), b = fstatSync(this.directory.fd);
      if (a.dev !== b.dev || a.ino !== b.ino) throw failure("export_target_changed");
    } finally { current.close(); }
    return stat;
  }
  private verifyOwnership(record = this.record): void {
    const stat = this.verifyFile();
    if (!record || record.phase !== "complete" || record.device !== String(stat.dev) || record.inode !== String(stat.ino) || record.bytes !== stat.size)
      throw failure("export_not_owned");
    const bytes = Buffer.alloc(Number(stat.size));
    let offset = 0;
    while (offset < bytes.length) {
      const n = readSync(this.fd!, bytes, offset, bytes.length - offset, offset);
      if (n === 0) throw failure("export_target_changed");
      offset += n;
    }
    const after = this.verifyFile();
    if (after.mtimeMs !== stat.mtimeMs || after.ctimeMs !== stat.ctimeMs || bytesDigest(bytes) !== record.digest)
      throw failure("export_target_changed");
  }
  async prepare(signal: AbortSignal): Promise<void> {
    signal.throwIfAborted();
    const locked = await this.store.call("artifact.lock", { key: this.key }, signal);
    this.record = isObject(locked.record) ? locked.record : undefined;
    try { this.fd = openSync(this.path, constants.O_RDWR | constants.O_NOFOLLOW | constants.O_NONBLOCK); }
    catch (error) { if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw failure("unsafe_export_target"); }
    if (this.fd !== undefined) {
      if (!this.overwrite) throw new CliError("conflict", "export_exists", "Export already exists; no file was changed.");
      this.verifyOwnership();
    }
    this.prepared = true;
  }
  async write(bytes: Buffer, signal: AbortSignal): Promise<Obj> {
    if (!this.prepared || this.changed) throw failure("export_not_prepared");
    if (bytes.length > 16 * 1024 * 1024) throw failure("export_limit_exceeded");
    signal.throwIfAborted();
    const existing = this.fd !== undefined, previousRecord = this.record;
    if (this.fd !== undefined) this.verifyOwnership();
    else {
      try { this.fd = openSync(this.path, constants.O_RDWR | constants.O_CREAT | constants.O_EXCL | constants.O_NOFOLLOW, 0o600); }
      catch (error) {
        if ((error as NodeJS.ErrnoException).code === "EEXIST") throw new CliError("conflict", "export_exists", "Export appeared during collection; no file was changed.");
        throw failure("export_create_failed");
      }
      fchmodSync(this.fd, 0o600);
    }
    const stat = this.verifyFile(), operation = randomUUID();
    const writing: Obj = { phase: "writing", operation_id: operation, device: String(stat.dev), inode: String(stat.ino), bytes: bytes.length, digest: bytesDigest(bytes) };
    this.record = object((await this.store.call("artifact.record", { key: this.key, expected_revision: this.record?.revision ?? 0, record: writing }, signal)).record);
    signal.throwIfAborted();
    if (existing) this.verifyOwnership(previousRecord);
    else if (this.verifyFile().size !== 0) throw failure("export_target_changed");
    this.changed = true;
    try {
      // Write only the verified open inode; never truncate by pathname or rename
      // over a pathname that another process could replace after verification.
      ftruncateSync(this.fd!, 0);
      for (let offset = 0; offset < bytes.length;) {
        signal.throwIfAborted();
        const n = writeSync(this.fd!, bytes, offset, bytes.length - offset, offset);
        if (n === 0) throw failure("export_write_failed");
        offset += n;
      }
      fsyncSync(this.fd!); fsyncSync(this.directory.fd); this.verifyFile();
      this.record = object((await this.store.call("artifact.record", { key: this.key, expected_revision: this.record.revision!, record: { ...writing, phase: "complete" } }, signal)).record);
      this.verifyOwnership();
      return { path: this.absolutePath, bytes: bytes.length, digest: writing.digest!, permissions: "0600", ownership: "state_store_verified" };
    } catch {
      throw new CliError("config", "export_incomplete", "Export may contain incomplete data; its ownership record must not be treated as permission to retry or overwrite blindly.");
    }
  }
  async close(): Promise<void> {
    if (this.fd !== undefined) { closeSync(this.fd); this.fd = undefined; }
    this.directory.close(); if (!this.sharedStore) await this.store.close();
  }
}
