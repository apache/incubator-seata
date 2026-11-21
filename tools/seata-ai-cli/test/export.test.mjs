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
import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, statSync, symlinkSync, linkSync, renameSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";
import { randomUUID } from "node:crypto";
import { ExportFile } from "../dist/adapter/export/file.js";
import { encodeSnapshot, bytesDigest } from "../dist/adapter/export/archive.js";
import { canonical, digest } from "../dist/protocol/value.js";
import { StateStore } from "../dist/adapter/statestore/client.js";

const signal = new AbortController().signal;
test("batch exports retain per-file locks until the shared store closes", async () => {
  const f = fixture(), store = new StateStore(f.state), contender = new StateStore(f.state);
  const file = new ExportFile(f.root, "batch.json", false, f.state, 30000, store);
  try {
    await file.prepare(signal);
    await file.write(Buffer.from("batch"), signal);
    await file.close();
    await assert.rejects(contender.call("artifact.lock", { key: digest(join(f.root, "batch.json")) }),
      "Closing a file must not release the batch lifetime lock");
    await store.call("info");
    await store.close();
    assert.equal((await contender.call("artifact.lock", { key: digest(join(f.root, "batch.json")) })).record.phase, "complete");
  } finally { await store.close(); await contender.close(); }
});
function fixture() {
  const dir = mkdtempSync(join(tmpdir(), "seata-export-")), root = join(dir, "exports"), state = join(dir, "state");
  const use = async (path, overwrite, action) => {
    const file = new ExportFile(root, path, overwrite, state);
    try { await file.prepare(signal); return await action(file); }
    finally { await file.close(); }
  };
  return { dir, root, state, use };
}

test("JSON and ZIP manifests bind canonical UTF-8 payloads; Python independently verifies fixed regular-file entries", () => {
  const snapshot = { snapshot_id: "synthetic", redaction: { version: "seata-redaction-v1" }, completeness: { status: "partial" },
    sources: [{ id: "data", data: { label: "中文 /../../not-an-entry" } }] };
  const json = encodeSnapshot(snapshot, "json"), decoded = JSON.parse(json.bytes.toString());
  assert.equal(decoded.manifest.entries[0].digest, bytesDigest(Buffer.from(canonical(snapshot))));
  assert.equal(decoded.manifest.entries[0].bytes, Buffer.byteLength(canonical(snapshot)));
  const zip = encodeSnapshot(snapshot, "zip");
  const checked = spawnSync("python3", ["-c", `import io,json,sys,zipfile,hashlib,stat
z=zipfile.ZipFile(io.BytesIO(sys.stdin.buffer.read()))
assert z.namelist()==['snapshot.json','manifest.json']
assert z.testzip() is None
for item in z.infolist():
 assert item.create_system==3 and stat.S_ISREG(item.external_attr>>16)
 assert stat.S_IMODE(item.external_attr>>16)==0o600
 assert item.compress_type==zipfile.ZIP_STORED
payload=z.read('snapshot.json'); manifest=json.loads(z.read('manifest.json'))
assert manifest['entries'][0]['digest']=='sha256:'+hashlib.sha256(payload).hexdigest()
assert manifest['entries'][0]['bytes']==len(payload)
print(json.dumps(json.loads(payload),ensure_ascii=False))`], { input: zip.bytes, encoding: "utf8" });
  assert.equal(checked.status, 0, checked.stderr); assert.deepEqual(JSON.parse(checked.stdout), snapshot);
});

test("export creates private files exclusively and permits explicit verified overwrite across process-owned workers", async () => {
  const f = fixture(), payload = Buffer.from("first");
  const written = await f.use("nested/诊断.json", false, file => file.write(payload, signal));
  assert.equal(readFileSync(written.path, "utf8"), "first"); assert.equal(statSync(written.path).mode & 0o777, 0o600);
  assert.equal(statSync(f.root).mode & 0o777, 0o700);
  await assert.rejects(f.use("nested/诊断.json", false, async () => {}), e => e.subtype === "export_exists");
  await f.use("nested/诊断.json", true, file => file.write(Buffer.from("replacement"), signal));
  assert.equal(readFileSync(written.path, "utf8"), "replacement");
  writeFileSync(written.path, "user edit", { mode: 0o600 });
  await assert.rejects(f.use("nested/诊断.json", true, async () => {}), e => ["export_not_owned", "export_target_changed"].includes(e.subtype));
  assert.equal(readFileSync(written.path, "utf8"), "user edit");
});

test("export refuses unowned files, traversal, symlinks and hardlinks without touching their contents", async () => {
  const f = fixture(); mkdirSync(f.root, { mode: 0o700 });
  const foreign = join(f.root, "foreign.json"); writeFileSync(foreign, "sentinel", { mode: 0o600 });
  await assert.rejects(f.use("foreign.json", true, async () => {}), e => e.subtype === "export_not_owned");
  symlinkSync(foreign, join(f.root, "link.json"));
  await assert.rejects(f.use("link.json", true, async () => {}), e => e.subtype === "unsafe_export_target");
  linkSync(foreign, join(f.root, "hard.json"));
  await assert.rejects(f.use("hard.json", true, async () => {}), e => e.subtype === "export_target_changed");
  for (const path of ["../outside", "/absolute", "a/../outside", "a//b", "a\\b", "C:/file", "a/./b", "a/"]) {
    assert.throws(() => new ExportFile(f.root, path, true, f.state), e => e.subtype === "unsafe_export_path");
  }
  symlinkSync(f.dir, join(f.root, "escape"));
  await assert.rejects(f.use("escape/foreign.json", true, async () => {}), e => e.type === "policy");
  assert.equal(readFileSync(foreign, "utf8"), "sentinel");
});

test("export detects target replacement during collection and serializes competing writers", async () => {
  const f = fixture(); await f.use("out.json", false, file => file.write(Buffer.from("owned"), signal));
  const first = new ExportFile(f.root, "out.json", true, f.state), second = new ExportFile(f.root, "out.json", true, f.state);
  try {
    await first.prepare(signal);
    await assert.rejects(second.prepare(signal), e => e.subtype === "store_busy");
    renameSync(join(f.root, "out.json"), join(f.root, "previous.json"));
    writeFileSync(join(f.root, "out.json"), "foreign replacement", { mode: 0o600 });
    await assert.rejects(first.write(Buffer.from("must not overwrite"), signal), e => e.subtype === "export_target_changed");
    assert.equal(readFileSync(join(f.root, "out.json"), "utf8"), "foreign replacement");
    assert.equal(readFileSync(join(f.root, "previous.json"), "utf8"), "owned");
  } finally { await first.close(); await second.close(); }
});

test("exclusive creation catches a file appearing after preparation; aborted export never creates a payload", async () => {
  const f = fixture(), file = new ExportFile(f.root, "out.json", false, f.state);
  try {
    await file.prepare(signal); writeFileSync(join(f.root, "out.json"), "foreign", { mode: 0o600 });
    await assert.rejects(file.write(Buffer.from("overwrite"), signal), e => e.subtype === "export_exists");
    assert.equal(readFileSync(join(f.root, "out.json"), "utf8"), "foreign");
  } finally { await file.close(); }
  const abort = new AbortController(); abort.abort();
  const cancelled = new ExportFile(f.root, "cancelled.json", false, f.state);
  try { await assert.rejects(cancelled.prepare(abort.signal)); }
  finally { await cancelled.close(); }
  assert.equal(existsSync(join(f.root, "cancelled.json")), false);
});

test("incomplete ownership records and a different StateStore cannot authorize overwrite", async () => {
  const f = fixture(); await f.use("out.json", false, file => file.write(Buffer.from("completed"), signal));
  const foreign = new ExportFile(f.root, "out.json", true, join(f.dir, "other-state"));
  try { await assert.rejects(foreign.prepare(signal), e => e.subtype === "export_not_owned"); }
  finally { await foreign.close(); }
  const store = new StateStore(f.state), key = digest(join(f.root, "out.json"));
  try {
    const { record } = await store.call("artifact.lock", { key });
    const pending = { ...record, phase: "writing", operation_id: randomUUID() };
    await store.call("artifact.record", { key, expected_revision: record.revision, record: pending });
    await assert.rejects(store.call("artifact.record", { key, expected_revision: record.revision, record: pending }), e => e.subtype === "artifact_revision_conflict");
  } finally { await store.close(); }
  await assert.rejects(f.use("out.json", true, async () => {}), e => e.subtype === "export_not_owned");
  assert.equal(readFileSync(join(f.root, "out.json"), "utf8"), "completed");
});

test("export rejects private-state reserved paths even when the target does not exist", () => {
  const f = fixture();
  for (const path of ["state.db", "state.db-wal", "state.db-shm", "lifetime.lock", "credentials/owner/ref"]) {
    assert.throws(() => new ExportFile(f.state, path, false, f.state), e => e.subtype === "export_reserved_target");
  }
  assert.equal(existsSync(f.state), false);
});
