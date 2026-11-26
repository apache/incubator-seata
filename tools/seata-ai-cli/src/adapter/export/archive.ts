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
import { createHash } from "node:crypto";
import { zipSync } from "fflate";
import { type Obj, canonical, object } from "../../protocol/value.js";
import { CliError } from "../../protocol/error.js";

export function bytesDigest(bytes: Uint8Array): string { return "sha256:" + createHash("sha256").update(bytes).digest("hex"); }
export function encodeSnapshot(snapshot: Obj, format: string): { bytes: Buffer; manifest: Obj } {
  if (!["json", "zip"].includes(format)) throw new CliError("validation", "format_unsupported", "Snapshot export supports JSON or ZIP.");
  const payload = Buffer.from(canonical(snapshot));
  if (payload.length > 12 * 1024 * 1024) throw new CliError("config", "export_limit_exceeded", "Snapshot exceeds its export budget.");
  const manifest: Obj = { version: "1", snapshot_id: snapshot.snapshot_id ?? null, redaction_version: object(snapshot.redaction).version ?? null,
    encoding: "utf-8", canonicalization: "sorted_object_keys_no_whitespace", entries: [
      { path: "snapshot.json", bytes: payload.length, digest: bytesDigest(payload), redaction_version: object(snapshot.redaction).version ?? null },
    ], completeness: snapshot.completeness ?? null,
    manifest_digest_scope: "payload_entries_only_outer_artifact_digest_returned_separately" };
  const bytes = format === "json" ? Buffer.from(canonical({ manifest, snapshot })) : Buffer.from(zipSync({
    // Entry names are constants, never derived from paths or server responses.
    "snapshot.json": [payload, { level: 0, os: 3, attrs: (0o100600 << 16) >>> 0 }],
    "manifest.json": [Buffer.from(canonical(manifest)), { level: 0, os: 3, attrs: (0o100600 << 16) >>> 0 }],
  }, { mtime: new Date("2000-01-01T00:00:00.000Z") }));
  if (bytes.length > 16 * 1024 * 1024) throw new CliError("config", "export_limit_exceeded", "Archive exceeds its export budget.");
  return { bytes, manifest };
}
