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
import { type Invocation, validateOutput } from "../command/registry.js";
import { type Obj, object, str } from "../protocol/value.js";
import { PartialResult } from "../protocol/error.js";
import { ExportFile } from "../adapter/export/file.js";
import { encodeSnapshot } from "../adapter/export/archive.js";
import { snapshotCommand } from "./snapshot.js";

export async function exportSnapshot(inv: Invocation, signal: AbortSignal): Promise<Obj> {
  const target = new ExportFile(str(inv.input.root), str(inv.input.path), inv.input.overwrite === true, undefined, Number(inv.options.timeout_ms ?? 30000));
  try {
    // Path, ownership and local locking are checked before any network request.
    await target.prepare(signal);
    let collected: Obj, partial = false;
    try { collected = await snapshotCommand(inv, signal); }
    catch (error) {
      if (!(error instanceof PartialResult)) throw error;
      collected = error.data; partial = true;
    }
    validateOutput("diagnose.snapshot", collected);
    const snapshot = object(collected.snapshot), encoded = encodeSnapshot(snapshot, str(inv.input.format));
    const written = await target.write(encoded.bytes, signal);
    const result = { export_manifest: { ...encoded.manifest, artifact: { ...written, format: inv.input.format! } }, completeness: snapshot.completeness! };
    if (partial) throw new PartialResult(result, "snapshot_export_partial", "The export was written successfully but contains incomplete source evidence.");
    return result;
  } finally { await target.close(); }
}
