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
import { Worker } from "node:worker_threads";
import { homedir } from "node:os";
import { join } from "node:path";
import { type Obj, object, isObject } from "../../protocol/value.js";
import { CliError, exits, type ErrorType } from "../../protocol/error.js";

export function stateDirectory(): string {
  return process.env.SEATA_AI_STATE_DIR ?? join(homedir(), ".seata-ai");
}
export class StateStore {
  private readonly worker: Worker;
  private readonly pending = new Map<
    number,
    {
      resolve: (value: Obj) => void;
      reject: (error: Error) => void;
      cleanup: () => void;
    }
  >();
  private sequence = 0;
  private closed = false;
  private readonly deadline: number;
  constructor(directory = stateDirectory(), budgetMs = 30000) {
    this.deadline = performance.now() + budgetMs;
    this.worker = new Worker(new URL("./worker.js", import.meta.url), {
      workerData: { directory },
      execArgv: ["--disable-warning=ExperimentalWarning"],
      stdout: true,
      stderr: true,
    });
    // Any unexpected worker output is a fault, never forwarded into the public streams.
    this.worker.stdout.on("data", () =>
      this.fail(
        new CliError(
          "internal",
          "journal_failure",
          "Unexpected StateStore worker output.",
        ),
      ),
    );
    this.worker.stderr.on("data", () =>
      this.fail(
        new CliError(
          "internal",
          "journal_failure",
          "Unexpected StateStore worker warning.",
        ),
      ),
    );
    this.worker.on("message", (message: unknown) => {
      if (!isObject(message) || typeof message.id !== "number") {
        this.fail(
          new CliError(
            "internal",
            "journal_failure",
            "Invalid StateStore worker response.",
          ),
        );
        return;
      }
      const call = this.pending.get(message.id);
      if (!call) return;
      this.pending.delete(message.id);
      call.cleanup();
      if (isObject(message.error)) {
        const type = message.error.type;
        const errorType: ErrorType =
          typeof type === "string" && Object.hasOwn(exits, type)
            ? (type as ErrorType)
            : "internal";
        call.reject(
          new CliError(
            errorType,
            typeof message.error.subtype === "string"
              ? message.error.subtype
              : "journal_failure",
            typeof message.error.message === "string"
              ? message.error.message
              : "StateStore operation failed.",
          ),
        );
      } else {
        try {
          call.resolve(object(message.value));
        } catch {
          call.reject(
            new CliError(
              "internal",
              "journal_failure",
              "Malformed StateStore response.",
            ),
          );
        }
      }
    });
    this.worker.on("error", () =>
      this.fail(
        new CliError(
          "internal",
          "journal_failure",
          "StateStore worker failed; a submitted transaction may have committed.",
        ),
      ),
    );
    this.worker.on("exit", () =>
      this.fail(
        new CliError(
          "internal",
          "journal_failure",
          "StateStore worker exited; query durable state before retrying.",
        ),
      ),
    );
  }
  async call(op: string, args: Obj = {}, signal?: AbortSignal): Promise<Obj> {
    if (this.closed)
      throw new CliError(
        "internal",
        "journal_failure",
        "StateStore is closed.",
      );
    if (this.pending.size >= 32)
      throw new CliError("policy", "store_busy", "StateStore queue is full.");
    signal?.throwIfAborted();
    const id = ++this.sequence;
    return new Promise<Obj>((resolve, reject) => {
      const aborted = () => {
        const call = this.pending.get(id);
        if (!call) return;
        signal?.removeEventListener("abort", aborted);
        reject(
          new CliError(
            "cancelled",
            "context_cancelled",
            "Stopped waiting for StateStore; query durable state before retrying.",
          ),
        );
      };
      const timeout = setTimeout(() => {
        this.pending.delete(id);
        signal?.removeEventListener("abort", aborted);
        reject(
          new CliError(
            "internal",
            "journal_failure",
            "StateStore response deadline exceeded; commit outcome may be unknown.",
          ),
        );
      }, 10000);
      this.pending.set(id, {
        resolve,
        reject,
        cleanup: () => {
          clearTimeout(timeout);
          signal?.removeEventListener("abort", aborted);
        },
      });
      signal?.addEventListener("abort", aborted, { once: true });
      this.worker.postMessage({
        id,
        op,
        args,
        deadline: op === "close" ? performance.now() + 2000 : this.deadline,
      });
    });
  }
  private fail(error: Error): void {
    this.closed = true;
    for (const call of this.pending.values()) {
      call.cleanup();
      call.reject(error);
    }
    this.pending.clear();
    void this.worker.terminate();
  }
  async close(): Promise<void> {
    if (this.closed) {
      await this.worker.terminate();
      return;
    }
    try {
      await this.call("close");
    } finally {
      this.closed = true;
      await this.worker.terminate();
    }
  }
}
