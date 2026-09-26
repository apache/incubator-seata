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
import { closeSync, constants, fstatSync, openSync, readSync, statfsSync, opendirSync } from "node:fs";
import { resolve } from "node:path";
import { CliError } from "../../protocol/error.js";

const FILE_BYTES = 1024 * 1024, TOTAL_BYTES = 10 * FILE_BYTES, FILE_COUNT = 200;
function failure(subtype: string): CliError {
  return new CliError("config", subtype, "Configuration source could not be read safely within its inspection limits.");
}

/** A retained directory descriptor anchors bounded discovery to the opened
 * directory. Only names are returned; every later file read revalidates its path.
 */
export class ConfigDirectory {
  readonly path: string;
  private fd: number;
  constructor(input: string, signal: AbortSignal) {
    signal.throwIfAborted();
    if (process.platform !== "linux") throw failure("filesystem_adapter_unavailable");
    if (!input || input.length > 4096 || /[\x00-\x1f\x7f]/.test(input)) throw failure("unsafe_config_path");
    this.path = resolve(input);
    let fd: number | undefined;
    try {
      fd = openSync("/", constants.O_RDONLY | constants.O_DIRECTORY | constants.O_NOFOLLOW);
      for (const part of this.path.split("/").filter(Boolean)) {
        signal.throwIfAborted();
        const child = openSync(`/proc/self/fd/${fd}/${part}`, constants.O_RDONLY | constants.O_DIRECTORY | constants.O_NOFOLLOW);
        closeSync(fd); fd = child;
      }
      if (![0xef53, 0x01021994, 0x9123683e, 0x58465342, 0x794c7630].includes(statfsSync(`/proc/self/fd/${fd}`).type))
        throw failure("filesystem_adapter_unavailable");
      this.fd = fd;
    } catch (error) {
      if (fd !== undefined) closeSync(fd);
      if (signal.aborted) signal.throwIfAborted();
      if (error instanceof CliError) throw error;
      throw failure((error as NodeJS.ErrnoException).code === "ENOENT" ? "config_directory_missing" : "unsafe_config_path");
    }
  }
  list(signal: AbortSignal): { names: string[]; truncated: boolean } {
    signal.throwIfAborted();
    const directory = opendirSync(`/proc/self/fd/${this.fd}`), names: string[] = [];
    try {
      for (let count = 0; count <= 2048; count++) {
        signal.throwIfAborted();
        const entry = directory.readSync();
        if (!entry) return { names: names.sort(), truncated: false };
        if (count === 2048) return { names: names.sort(), truncated: true };
        // Symlink and special-file names are included so the reader can report
        // their refusal, rather than quietly hiding a relevant unsafe source.
        names.push(entry.name);
      }
      return { names: names.sort(), truncated: true };
    } finally { directory.closeSync(); }
  }
  close(): void { if (this.fd >= 0) { closeSync(this.fd); this.fd = -1; } }
}

/** One budget per inspection, including failed opens and bytes already read. */
export class ConfigFiles {
  private count = 0;
  private bytes = 0;
  get usage(): { files: number; bytes: number } { return { files: this.count, bytes: this.bytes }; }

  /** Descriptor-relative traversal prevents both ancestor and leaf symlink races.
   * No writes, subprocesses, network filesystems, or unbounded readFile calls.
   * Bytes remain private until parsed and converted to redacted ConfigFacts.
   */
  read(input: string, signal: AbortSignal): { path: string; bytes: Buffer } {
    signal.throwIfAborted();
    if (process.platform !== "linux") throw failure("filesystem_adapter_unavailable");
    if (typeof input !== "string" || !input || input.length > 4096 || /[\x00-\x1f\x7f]/.test(input)) throw failure("unsafe_config_path");
    if (++this.count > FILE_COUNT) throw failure("config_limit_exceeded");
    const path = resolve(input), parts = path.split("/").filter(Boolean);
    if (!parts.length) throw failure("unsafe_config_path");
    let fd: number | undefined;
    try {
      fd = openSync("/", constants.O_RDONLY | constants.O_DIRECTORY | constants.O_NOFOLLOW);
      for (let i = 0; i < parts.length; i++) {
        signal.throwIfAborted();
        const child = openSync(`/proc/self/fd/${fd}/${parts[i]}`, constants.O_RDONLY | constants.O_NOFOLLOW |
          (i === parts.length - 1 ? constants.O_NONBLOCK : constants.O_DIRECTORY));
        closeSync(fd); fd = child;
      }
      const before = fstatSync(fd, { bigint: true });
      if (!before.isFile() || before.nlink !== 1n) throw failure("unsafe_config_path");
      if (![0xef53, 0x01021994, 0x9123683e, 0x58465342, 0x794c7630].includes(statfsSync(`/proc/self/fd/${fd}`).type))
        throw failure("filesystem_adapter_unavailable");
      const remaining = Math.min(FILE_BYTES, TOTAL_BYTES - this.bytes);
      if (before.size > BigInt(remaining)) throw failure("config_limit_exceeded");
      const buffer = Buffer.alloc(remaining + 1);
      let size = 0;
      while (size < buffer.length) {
        signal.throwIfAborted();
        const n = readSync(fd, buffer, size, buffer.length - size, null);
        if (n === 0) break;
        size += n; this.bytes += n;
        if (size > remaining) throw failure("config_limit_exceeded");
      }
      const after = fstatSync(fd, { bigint: true });
      if (before.size !== BigInt(size) || before.size !== after.size || before.mtimeNs !== after.mtimeNs ||
          before.ctimeNs !== after.ctimeNs || before.nlink !== after.nlink)
        throw failure("config_source_changed");
      return { path, bytes: buffer.subarray(0, size) };
    } catch (error) {
      if (signal.aborted) signal.throwIfAborted();
      if (error instanceof CliError) throw error;
      throw failure("config_source_unavailable");
    } finally { if (fd !== undefined) closeSync(fd); }
  }
}
