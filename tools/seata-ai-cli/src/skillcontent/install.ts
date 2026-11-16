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
import { homedir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { lstatSync, closeSync } from "node:fs";
import { randomUUID } from "node:crypto";
import { type Invocation } from "../command/registry.js";
import { type Obj, object, str, digest, isObject } from "../protocol/value.js";
import { CliError } from "../protocol/error.js";
import { StateStore, stateDirectory } from "../adapter/statestore/client.js";
import { PrivateDirectory } from "../platform/linux.js";
import { ExportFile } from "../adapter/export/file.js";
import { OwnedRemoval } from "../adapter/export/remove.js";
import { bytesDigest } from "../adapter/export/archive.js";
import { ConfigDirectory, ConfigFiles } from "../adapter/config/files.js";
import { resource } from "../resources.js";
import { skillCatalog } from "./catalog.js";
import { skillsStatus, skillVersion } from "./status.js";

export async function installSkills(inv: Invocation, signal: AbortSignal): Promise<Obj> {
  if (inv.input.host_id !== undefined && inv.input.host_id !== "generic_agent_skills")
    throw new CliError("policy", "host_adapter_unavailable", "Only generic_agent_skills is supported.");
  const path = resolve(homedir(), ".agents", "skills"), state = stateDirectory();
  const directory = new PrivateDirectory(path), budget = Number(inv.options.timeout_ms ?? 30000);
  const store = new StateStore(state, budget), targets: ExportFile[] = [], removals: OwnedRemoval[] = [];
  let lock: number | undefined;
  const catalog = skillCatalog(), contentDigest = digest(catalog);
  const payloads = catalog.flatMap(skill => (skill.files as Obj[]).map(file => ({
    relative: str(skill.skill_id) + "/" + str(file.path),
    bytes: resource("skills/" + str(skill.skill_id) + "/" + str(file.path)),
  })));
  const managedPath = (value: string): boolean => {
    const parts = value.split("/");
    return catalog.some(skill => skill.skill_id === parts[0]) && !parts.some(part => part === "." || part === "..") &&
      ((parts.length === 2 && parts[1] === "SKILL.md") || (parts.length === 3 &&
        ["references", "examples", "tests"].includes(parts[1]!) && /^[a-zA-Z0-9_.-]+$/.test(parts[2]!)));
  };
  const conflict = () => new CliError("conflict", "skills_install_conflict", "Existing Skill content is unowned, modified, or belongs to an unresolved installation.");
  const incomplete = () => new CliError("conflict", "skills_install_incomplete", "Interrupted installation contains an unowned, modified, or uncertain file; no payloads were written during recovery.");
  try {
    // The directory lock coordinates different StateStores targeting one host.
    lock = directory.lock(".seata-ai-install.lock");
    const locked = await store.call("skills.install.lock", {}, signal);
    let record = isObject(locked.record) ? locked.record : undefined;
    if (record?.files_digest !== undefined && record.files_digest !== digest(record.files ?? null)) throw conflict();
    const resuming = record !== undefined && ["preparing", "writing"].includes(String(record.phase)) &&
      record.content_digest === contentDigest;
    if (resuming && (record!.installation_path !== path || record!.version !== skillVersion ||
        !Array.isArray(record!.files) || record!.files.length !== 0)) throw conflict();
    let upgrading = false;
    if (record && !resuming) {
      upgrading = record.content_digest !== contentDigest;
      const resumeVerification = record.phase === "verifying" && !upgrading;
      if ((!resumeVerification && record.phase !== "complete") || record.installation_path !== path ||
          typeof record.content_digest !== "string" || !/^sha256:[a-f0-9]{64}$/.test(record.content_digest) ||
          typeof record.version !== "string" || !record.version || (!upgrading && record.version !== skillVersion) ||
          !Array.isArray(record.files) || record.files.length === 0 ||
          ((!upgrading || record.files_digest === undefined) && record.files.length !== payloads.length))
        throw conflict();
      // Content equality does not establish ownership. Validate unique entries
      // against the bundled paths before reading any saved path. An upgrade
      // may add resources; an unchanged install must match the whole inventory.
      const expected = new Map(payloads.map(payload => [payload.relative, payload]));
      const seen = new Set<string>();
      for (const entry of record.files) {
        if (!isObject(entry) || typeof entry.relative_path !== "string" || !managedPath(entry.relative_path) || seen.has(entry.relative_path)) throw conflict();
        seen.add(entry.relative_path);
        const payload = expected.get(entry.relative_path);
        if ((!payload && !upgrading) || entry.path !== join(path, entry.relative_path) || !Number.isSafeInteger(entry.bytes) ||
            Number(entry.bytes) < 0 || Number(entry.bytes) > 1024 * 1024 ||
            typeof entry.digest !== "string" || !/^sha256:[a-f0-9]{64}$/.test(entry.digest) ||
            (!upgrading && (entry.bytes !== payload!.bytes.length || entry.digest !== bytesDigest(payload!.bytes))) || entry.permissions !== "0600" ||
            entry.ownership !== "state_store_verified" || typeof entry.device !== "string" ||
            !/^\d+$/.test(entry.device) || typeof entry.inode !== "string" || !/^\d+$/.test(entry.inode)) throw conflict();
        expected.delete(entry.relative_path);
      }
      if (!upgrading && expected.size !== 0) throw conflict();
      const reader = new ConfigFiles();
      for (const file of (record.files as Obj[])) {
        const stat = lstatSync(join(path, str(file.relative_path)));
        if (!stat.isFile() || stat.isSymbolicLink() || stat.nlink !== 1 || stat.uid !== process.getuid?.() ||
            String(stat.dev) !== file.device || String(stat.ino) !== file.inode || (stat.mode & 0o777) !== 0o600) throw conflict();
        const bytes = reader.read(join(path, str(file.relative_path)), signal).bytes;
        if (bytes.length !== file.bytes || bytesDigest(bytes) !== file.digest) throw conflict();
      }
      if (!upgrading) {
        const status = skillsStatus(inv.input, signal);
        if (status.content_installed !== true) throw conflict();
        if (resumeVerification) {
          record = object((await store.call("skills.install.record", { expected_revision: record.revision!,
            record: { ...record, phase: "complete" } }, signal)).record);
          return { manifest: record, installation_state: "complete", host_status: status };
        }
        return { manifest: record, installation_state: "unchanged", host_status: status };
      }
    }
    const previousFiles = upgrading ? record!.files! : resuming ? record!.previous_files ?? [] : [];
    if (!Array.isArray(previousFiles)) throw conflict();
    const baseline = new Map<string, Obj>();
    for (const file of previousFiles) {
      if (!isObject(file) || typeof file.relative_path !== "string" ||
          !managedPath(file.relative_path) || baseline.has(file.relative_path) ||
          !Number.isSafeInteger(file.bytes) || Number(file.bytes) < 0 || typeof file.digest !== "string" ||
          !/^sha256:[a-f0-9]{64}$/.test(file.digest) || typeof file.device !== "string" || typeof file.inode !== "string") throw conflict();
      baseline.set(file.relative_path, file);
      if (!payloads.some(payload => payload.relative === file.relative_path)) {
        const removal = new OwnedRemoval(join(path, file.relative_path), file, store);
        removals.push(removal); await removal.prepare(signal);
      }
    }
    if (upgrading || resuming) {
      // Reject extra user content before updating any payload, not only during
      // post-write verification. A preparing operation may have no directories.
      for (const skill of catalog) for (const section of ["", "references", "examples", "tests"]) {
        let dir: ConfigDirectory;
        try { dir = new ConfigDirectory(join(path, str(skill.skill_id), section), signal); }
        catch (error) {
          if ((section || resuming) && error instanceof CliError && error.subtype === "config_directory_missing") continue;
          throw error;
        }
        try {
          const listed = dir.list(signal);
          const allowed = section === "" ? ["SKILL.md", "references", "examples", "tests"] :
            (skill.files as Obj[]).filter(f => str(f.path).startsWith(section + "/")).map(f => str(f.path).slice(section.length + 1));
          const prefix = str(skill.skill_id) + "/" + section + "/";
          for (const oldPath of baseline.keys()) if (oldPath.startsWith(prefix)) allowed.push(oldPath.slice(prefix.length));
          for (const removal of removals) if (dirname(removal.absolutePath) === join(path, str(skill.skill_id), section) && removal.stagingName)
            allowed.push(removal.stagingName);
          if (listed.truncated || listed.names.some(name => !allowed.includes(name))) throw conflict();
        } finally { dir.close(); }
      }
    }
    // Do not adopt even byte-identical user-created Skills without ownership.
    if (!upgrading && !resuming) for (const skill of catalog) {
      try { lstatSync(directory.path + "/" + str(skill.skill_id)); }
      catch (error) { if ((error as NodeJS.ErrnoException).code === "ENOENT") continue; throw error; }
      throw conflict();
    }
    const operation: Obj = { operation_id: resuming ? record!.operation_id! : randomUUID(), version: skillVersion, content_digest: contentDigest,
      installation_path: path, files: [], previous_files: previousFiles, phase: "preparing" };
    const save = async (phase: string, files: Obj[] = []): Promise<void> => {
      record = object((await store.call("skills.install.record", { expected_revision: record?.revision ?? 0,
        record: { ...operation, phase, files } }, signal)).record);
    };
    if (!upgrading && !resuming) await save("preparing");
    const resumeReader = new ConfigFiles();
    for (const payload of payloads) {
      const target = new ExportFile(path, payload.relative, (upgrading && baseline.has(payload.relative)) || resuming, state, budget, store);
      targets.push(target);
      try {
        await target.prepare(signal);
        if (resuming) {
          let stat;
          try { stat = lstatSync(target.absolutePath); }
          catch (error) {
            if ((error as NodeJS.ErrnoException).code === "ENOENT" && !baseline.has(payload.relative)) continue;
            throw incomplete();
          }
          const bytes = resumeReader.read(target.absolutePath, signal).bytes, hash = bytesDigest(bytes), old = baseline.get(payload.relative);
          const isNew = hash === bytesDigest(payload.bytes) && bytes.length === payload.bytes.length;
          const isOld = old && hash === old.digest && bytes.length === old.bytes && String(stat.dev) === old.device && String(stat.ino) === old.inode;
          if (!isNew && !isOld) throw incomplete();
        }
      }
      catch (error) {
        signal.throwIfAborted();
        if (resuming && error instanceof CliError && ["policy", "conflict"].includes(error.type))
          throw incomplete();
        throw error;
      }
    }
    // A refused upgrade must retain the last complete journal. All old-file
    // ownership checks finish before recording the new operation or writing.
    if (upgrading) await save("preparing");
    await save("writing");
    const files: Obj[] = [];
    for (let i = 0; i < payloads.length; i++) {
      const payload = payloads[i]!, written = await targets[i]!.write(payload.bytes, signal);
      const stat = lstatSync(join(path, payload.relative));
      files.push({ ...written, relative_path: payload.relative, device: String(stat.dev), inode: String(stat.ino) });
    }
    for (const removal of removals) await removal.remove(signal);
    await save("verifying", files);
    const status = skillsStatus(inv.input, signal);
    if (status.content_installed !== true) throw new CliError("config", "skills_verification_failed", "Installed content did not pass verification; installation remains incomplete.");
    await save("complete", files);
    return { manifest: record!, installation_state: "complete", host_status: status };
  } finally {
    await Promise.allSettled(targets.map(target => target.close()));
    for (const removal of removals) removal.close();
    await store.close();
    if (lock !== undefined) closeSync(lock);
    directory.close();
  }
}
