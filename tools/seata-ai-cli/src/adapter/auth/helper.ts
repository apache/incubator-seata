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
import { startBridge } from "./bridge.js";
import { isObject, str } from "../../protocol/value.js";

// This entry point is private, not a bin/Commander command. It accepts the
// capability only over its inherited IPC pipe and emits no protocol output.
let bridge: Awaited<ReturnType<typeof startBridge>> | undefined;
let initialized = false, acknowledged = false;
const stop = () => { void bridge?.close().finally(() => process.exit(0)); if (!bridge) process.exit(1); };
const timer = setTimeout(stop, 5000);
process.once("SIGTERM", stop); process.once("SIGINT", stop);
process.once("disconnect", () => { if (!acknowledged) stop(); });
if (!process.send) stop();
process.on("message", (value: unknown) => {
  if (!isObject(value)) { stop(); return; }
  if (value.kind === "ack" && initialized && bridge) { acknowledged = true; clearTimeout(timer); return; }
  if (initialized || value.kind !== "init") { stop(); return; }
  initialized = true;
  void (async () => {
    bridge = await startBridge(str(value.root), str(value.flow_id), str(value.helper_instance_id), str(value.token));
    process.send?.({ kind: "ready", port: bridge.port }, error => { if (error) stop(); });
  })().catch(stop);
});
