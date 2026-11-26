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
import { CliError } from "../protocol/error.js";

// Keep the R9 wire catalog discoverable, but never enable these commands in
// this read-only release, even if a handler is accidentally registered later.
const deferred = new Set([
  "transaction.retry.pause", "transaction.retry.resume",
  "branch.retry.pause", "branch.retry.resume", "transaction.dispatch",
  "transaction.status.recover", "transaction.delete", "branch.delete", "lock.delete",
]);
export function deferredWrite(command: string): boolean {
  return deferred.has(command);
}
export function enforceReadOnly(command: string): void {
  if (deferredWrite(command))
    throw new CliError("policy", "command_unavailable",
      "Server mutations (including their dry-run plans) are deferred in this read-only release.");
}
