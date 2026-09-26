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
import { closeSync, constants, fstatSync, fsyncSync, linkSync, lstatSync, mkdirSync, openSync,
  opendirSync, readSync, renameSync, rmdirSync, unlinkSync } from "node:fs";
import { basename, dirname, resolve } from "node:path";
import { randomUUID } from "node:crypto";
import { PrivateDirectory } from "../../platform/linux.js";
import { StateStore } from "../statestore/client.js";
import { CliError } from "../../protocol/error.js";
import { type Obj, digest, isObject, object, str } from "../../protocol/value.js";
import { bytesDigest } from "./archive.js";

const failure = () => new CliError("conflict", "skills_remove_conflict", "Obsolete resource or its removal staging area changed; preserved content requires inspection.");
function exists(path: string): boolean {
  try { lstatSync(path); return true; }
  catch (error) { if ((error as NodeJS.ErrnoException).code === "ENOENT") return false; throw error; }
}

/** Capture by rename, verify the captured inode, then unlink only inside the
 * private staging directory. Never unlink an independently replaceable source
 * pathname. The journal supports a crash before/after capture and deletion. */
export class OwnedRemoval {
  private readonly directory: PrivateDirectory;
  private readonly name: string;
  private readonly key: string;
  private record: Obj | undefined;
  constructor(readonly absolutePath: string, private readonly expected: Obj, private readonly store: StateStore) {
    this.absolutePath = resolve(absolutePath);
    this.directory = new PrivateDirectory(dirname(this.absolutePath), false);
    this.name = basename(this.absolutePath); this.key = digest(this.absolutePath);
  }
  get stagingName(): string | undefined { return typeof this.record?.staging_name === "string" ? this.record.staging_name : undefined; }
  private get source(): string { return this.directory.path + "/" + this.name; }
  private stage(): number | undefined {
    if (!this.stagingName) return undefined;
    let fd: number;
    try { fd = openSync(this.directory.path + "/" + this.stagingName, constants.O_RDONLY | constants.O_DIRECTORY | constants.O_NOFOLLOW); }
    catch (error) { if ((error as NodeJS.ErrnoException).code === "ENOENT") return undefined; throw failure(); }
    const stat = fstatSync(fd);
    if (stat.uid !== process.getuid?.() || (stat.mode & 0o777) !== 0o700) { closeSync(fd); throw failure(); }
    const dir = opendirSync(`/proc/self/fd/${fd}`);
    try {
      const first = dir.readSync();
      if ((first && first.name !== "payload") || dir.readSync() !== null) throw failure();
    } catch (error) { closeSync(fd); throw error; }
    finally { dir.closeSync(); }
    return fd;
  }
  private verify(path: string): void {
    const fd = openSync(path, constants.O_RDONLY | constants.O_NOFOLLOW | constants.O_NONBLOCK);
    try {
      const stat = fstatSync(fd), named = lstatSync(path);
      if (!stat.isFile() || stat.uid !== process.getuid?.() || stat.nlink !== 1 || (stat.mode & 0o777) !== 0o600 ||
          String(stat.dev) !== this.expected.device || String(stat.ino) !== this.expected.inode ||
          stat.size !== this.expected.bytes || stat.size > 1024 * 1024 || named.dev !== stat.dev || named.ino !== stat.ino) throw failure();
      const bytes = Buffer.alloc(stat.size);
      for (let offset = 0; offset < bytes.length;) {
        const n = readSync(fd, bytes, offset, bytes.length - offset, offset); if (n === 0) throw failure(); offset += n;
      }
      const after = fstatSync(fd);
      if (after.ctimeMs !== stat.ctimeMs || after.mtimeMs !== stat.mtimeMs || bytesDigest(bytes) !== this.expected.digest) throw failure();
    } finally { closeSync(fd); }
  }
  private inspect(): void {
    const fd = this.stage();
    try {
      const staged = fd !== undefined && exists(`/proc/self/fd/${fd}/payload`), original = exists(this.source);
      if (this.record!.phase === "removed") { if (staged || original) throw failure(); return; }
      if (staged) { if (original) throw failure(); this.verify(`/proc/self/fd/${fd}/payload`); }
      else if (original) this.verify(this.source);
      else if (this.record!.phase !== "removing") throw failure();
    } finally { if (fd !== undefined) closeSync(fd); }
  }
  async prepare(signal: AbortSignal): Promise<void> {
    const locked = await this.store.call("artifact.lock", { key: this.key }, signal);
    if (!isObject(locked.record)) throw failure();
    this.record = locked.record;
    for (const key of ["device", "inode", "digest", "bytes"]) if (this.record[key] !== this.expected[key]) throw failure();
    if (!["complete", "removing", "removed"].includes(str(this.record.phase)) ||
        (this.record.phase !== "complete" && !/^\.seata-remove-[a-f0-9-]{36}$/.test(this.stagingName ?? ""))) throw failure();
    this.inspect();
  }
  async remove(signal: AbortSignal): Promise<void> {
    signal.throwIfAborted();
    if (!this.record) throw failure();
    this.inspect();
    if (this.record.phase === "removed") return;
    if (this.record.phase === "complete") {
      const operation = randomUUID();
      this.record = object((await this.store.call("artifact.record", { key: this.key, expected_revision: this.record.revision!,
        record: { ...this.record, phase: "removing", operation_id: operation, staging_name: ".seata-remove-" + operation } }, signal)).record);
    }
    const stagePath = this.directory.path + "/" + this.stagingName!;
    if (!exists(stagePath)) { mkdirSync(stagePath, { mode: 0o700 }); fsyncSync(this.directory.fd); }
    const fd = this.stage(); if (fd === undefined) throw failure();
    try {
      const captured = `/proc/self/fd/${fd}/payload`;
      if (!exists(captured) && exists(this.source)) {
        this.verify(this.source); signal.throwIfAborted();
        renameSync(this.source, captured); fsyncSync(fd); fsyncSync(this.directory.fd);
        try { this.verify(captured); }
        catch (error) {
          // Restore a raced-in user file only through exclusive creation. If
          // the source was recreated, retain the captured file for inspection.
          try { linkSync(captured, this.source); unlinkSync(captured); fsyncSync(fd); fsyncSync(this.directory.fd); } catch { /* retain staging */ }
          throw error;
        }
      }
      if (exists(captured)) {
        if (exists(this.source)) throw failure();
        this.verify(captured); signal.throwIfAborted(); unlinkSync(captured); fsyncSync(fd);
      }
    } finally { closeSync(fd); }
    rmdirSync(stagePath); fsyncSync(this.directory.fd);
    this.record = object((await this.store.call("artifact.record", { key: this.key, expected_revision: this.record.revision!,
      record: { ...this.record, phase: "removed" } }, signal)).record);
  }
  close(): void { this.directory.close(); }
}
