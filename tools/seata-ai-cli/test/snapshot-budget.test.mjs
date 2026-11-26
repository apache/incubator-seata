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
import { SnapshotCollector, snapshotLimits } from "../dist/app/snapshot.js";
import { CliError } from "../dist/protocol/error.js";

test("snapshot evidence/entity budgets are cumulative and do not turn omitted evidence into empty success", async () => {
  assert.deepEqual(snapshotLimits, { bytes: 8388608, entities: 10000, milliseconds: 60000 });
  const collector = new SnapshotCollector(new AbortController().signal, { bytes: 50, entities: 3, milliseconds: 100 });
  await collector.collect("first", async () => ({ data: { good: true }, entities: 1 }));
  await collector.collect("large", async () => ({ data: { value: "a".repeat(51) }, entities: 1 }));
  await collector.collect("entities", async () => ({ data: {}, entities: 3 }));
  await collector.collect("last", async () => ({ data: {}, entities: 2 }));
  let called = false;
  await collector.collect("skipped", async () => { called = true; return { data: {}, entities: 0 }; });
  assert.equal(called, false); assert.equal(collector.usage.entities, 3);
  assert.ok(collector.usage.bytes <= 50);
  assert.equal(collector.sources[1].status, "truncated"); assert.equal(collector.sources[1].bytes, 0); assert.equal(collector.sources[1].data, null);
  assert.equal(collector.sources[2].truncation.reason_code, "entity_limit");
  assert.equal(collector.sources[4].status, "not_collected");
});

test("snapshot retains upstream truncated data, sanitizes errors and distinguishes cancellation from deadlines", async () => {
  const controller = new AbortController(), collector = new SnapshotCollector(controller.signal);
  await collector.collect("upstream", async () => ({ data: { retained: true }, entities: 1, incomplete: true, truncationReason: "raw_row_limit" }));
  assert.equal(collector.sources[0].status, "truncated"); assert.deepEqual(collector.sources[0].data, { retained: true });
  await collector.collect("error", async () => { throw new Error("SECRET raw failure"); });
  assert.doesNotMatch(JSON.stringify(collector.sources), /SECRET/);
  controller.abort(new CliError("network", "timeout", "Deadline reached."));
  let called = false;
  await collector.collect("late", async () => { called = true; return { data: {}, entities: 0 }; });
  assert.equal(called, false); assert.equal(collector.sources[2].truncation.reason_code, "deadline_exceeded");
  const cancelled = new AbortController(); cancelled.abort(new CliError("cancelled", "context_cancelled", "Cancelled."));
  await assert.rejects(new SnapshotCollector(cancelled.signal).collect("cancelled", async () => ({ data: {}, entities: 0 })), e => e.type === "cancelled");
});

test("a deadline during collection preserves prior evidence and prevents subsequent source execution", async () => {
  const controller = new AbortController(), collector = new SnapshotCollector(controller.signal);
  const evidence = { transactions: [{ xid: "retained" }] };
  await collector.collect("transactions", async () => ({ data: evidence, entities: 1 }));
  const retained = structuredClone(collector.sources[0]), usage = collector.usage;
  await collector.collect("locks", async () => {
    const deadline = new CliError("network", "timeout", "Deadline reached.");
    controller.abort(deadline);
    throw deadline;
  });
  let called = false;
  await collector.collect("local_config", async () => { called = true; return { data: {}, entities: 0 }; });
  assert.deepEqual(collector.sources[0], retained);
  assert.deepEqual(collector.usage, usage);
  assert.equal(collector.sources[1].status, "unavailable");
  assert.equal(collector.sources[1].truncation.reason_code, "deadline_exceeded");
  assert.equal(collector.sources[1].data, null);
  assert.equal(collector.sources[2].status, "not_collected");
  assert.equal(collector.sources[2].truncation.reason_code, "deadline_exceeded");
  assert.equal(called, false);
});
