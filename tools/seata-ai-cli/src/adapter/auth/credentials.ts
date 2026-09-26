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
import { closeSync, constants, fstatSync, fsyncSync, openSync, readFileSync, unlinkSync, writeFileSync } from "node:fs";
import { spawn } from "node:child_process";
import { join } from "node:path";
import { PrivateDirectory } from "../../platform/linux.js";
import { CliError } from "../../protocol/error.js";

const unavailable = () => new CliError("authentication", "credential_store_unavailable", "The selected secure credential store is unavailable or locked; no fallback was attempted.");
export function bearer(value: string): string {
  if (!/^Bearer [A-Za-z0-9._~+/-]+={0,2}$/.test(value) || value.length > 16384)
    throw new CliError("authentication", "invalid_credential", "Credential does not match the pinned Bearer format.");
  return value;
}
function reference(value: string): string {
  if (!/^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/.test(value))
    throw new CliError("internal", "contract_violation", "Invalid private credential reference.");
  return value;
}

/** Secrets stay in this adapter's memory / OS store, never in StateStore messages. */
export class CredentialStore {
  constructor(private readonly root: string, readonly policy: string, private readonly storeId: string) {
    if (!["keyring_required", "protected_file"].includes(policy)) throw unavailable();
    reference(storeId);
    if (process.platform !== "linux") throw unavailable();
  }
  get assurance(): string { return this.policy === "protected_file" ? "local_user_file" : "os_keyring"; }
  private async secretTool(action: "store" | "lookup" | "clear", ref: string, signal: AbortSignal, secret?: string): Promise<string> {
    signal.throwIfAborted();
    const timeout = AbortSignal.timeout(5000), bounded = AbortSignal.any([signal, timeout]);
    const args = [action, ...(action === "store" ? ["--label=Seata AI CLI credential"] : []),
      "application", "seata-ai-cli", "state-store", this.storeId, "credential-ref", reference(ref)];
    return await new Promise((resolve, reject) => {
      const child = spawn("/usr/bin/secret-tool", args, { stdio: ["pipe", "pipe", "pipe"], signal: bounded });
      let output = "", bytes = 0, failed = false;
      // Wait for the owned subprocess to actually exit before reporting a
      // write failure; otherwise cleanup could race a late Keyring write.
      const fail = () => { failed = true; child.kill(); };
      child.on("error", fail);
      child.stdout.on("data", (chunk: Buffer) => {
        bytes += chunk.length;
        if (bytes > 16385) fail(); else output += chunk.toString("utf8");
      });
      child.stderr.on("data", (chunk: Buffer) => { bytes += chunk.length; if (bytes > 32768) fail(); });
      child.stdin.on("error", fail);
      child.on("close", code => {
        if (failed) { reject(unavailable()); return; }
        if (code !== 0) reject(unavailable());
        else resolve(output.replace(/\n$/, ""));
      });
      child.stdin.end(secret === undefined ? undefined : secret + "\n");
    });
  }
  private directory(): PrivateDirectory {
    // Include StateStore identity so restoring a different store cannot select
    // another store's files merely by reusing its filesystem directory.
    return new PrivateDirectory(join(this.root, "credentials", this.storeId));
  }
  async put(ref: string, token: string, signal: AbortSignal): Promise<void> {
    bearer(token); reference(ref); signal.throwIfAborted();
    if (this.policy === "keyring_required") { await this.secretTool("store", ref, signal, token); return; }
    const directory = this.directory(); let fd: number | undefined;
    try {
      fd = openSync(directory.path + "/" + ref, constants.O_WRONLY | constants.O_CREAT | constants.O_EXCL | constants.O_NOFOLLOW, 0o600);
      writeFileSync(fd, token, { encoding: "utf8" }); fsyncSync(fd); fsyncSync(directory.fd);
    } catch { throw unavailable(); }
    finally { if (fd !== undefined) closeSync(fd); directory.close(); }
  }
  async get(ref: string, signal: AbortSignal): Promise<string> {
    reference(ref); signal.throwIfAborted();
    if (this.policy === "keyring_required") return bearer(await this.secretTool("lookup", ref, signal));
    const directory = this.directory(); let fd: number | undefined;
    try {
      fd = directory.file(ref, false);
      if (fstatSync(fd).size > 16384) throw unavailable();
      return bearer(readFileSync(fd, "utf8"));
    } catch { throw unavailable(); }
    finally { if (fd !== undefined) closeSync(fd); directory.close(); }
  }
  async delete(ref: string, signal: AbortSignal): Promise<void> {
    reference(ref); signal.throwIfAborted();
    if (this.policy === "keyring_required") { await this.secretTool("clear", ref, signal); return; }
    const directory = this.directory(); let fd: number | undefined;
    try {
      try { fd = openSync(directory.path + "/" + ref, constants.O_RDONLY | constants.O_NOFOLLOW); }
      catch (error) { if ((error as NodeJS.ErrnoException).code === "ENOENT") return; throw error; }
      const stat = fstatSync(fd);
      if (!stat.isFile() || stat.uid !== process.getuid?.() || stat.nlink !== 1 || (stat.mode & 0o077) !== 0) throw unavailable();
      unlinkSync(directory.path + "/" + ref); fsyncSync(directory.fd);
    } catch { throw unavailable(); }
    finally { if (fd !== undefined) closeSync(fd); directory.close(); }
  }
}
