// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
export const exits = {
  api: 1,
  validation: 2,
  authentication: 3,
  authorization: 3,
  config: 3,
  network: 4,
  internal: 5,
  policy: 6,
  not_found: 7,
  conflict: 8,
  partial_failure: 9,
  cancelled: 11,
} as const;
export type ErrorType = keyof typeof exits;
import type { Obj } from "./value.js";
export class CliError extends Error {
  constructor(
    public readonly type: ErrorType,
    public readonly subtype: string,
    message: string,
    public readonly retry: "never" | "read_only" = "never",
  ) {
    super(message);
  }
  get exitCode(): number {
    return exits[this.type];
  }
}
export function safeError(error: unknown): CliError {
  return error instanceof CliError
    ? error
    : new CliError(
        "internal",
        "unexpected",
        "The operation could not be completed.",
      );
}
export class PartialResult extends CliError {
  constructor(readonly data: Obj, subtype: string, message: string) {
    super("partial_failure", subtype, message, "read_only");
  }
}
