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
import http from "node:http";
import https from "node:https";
import { setTimeout as delay } from "node:timers/promises";
import { type Json, parseJson } from "../../protocol/value.js";
import { CliError } from "../../protocol/error.js";

export interface HttpRequest {
  endpoint: string;
  suffix: string;
  method: "GET" | "POST" | "PUT" | "DELETE";
  query?: Readonly<Record<string, string>>;
  headers?: Readonly<Record<string, string>>;
  body?: string;
}
export interface HttpResponse {
  status: number;
  body: Json;
}
function urlFor(request: HttpRequest): URL {
  const url = new URL(request.endpoint);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.search ||
    url.hash ||
    /[\\?#]|%(?:2f|5c|2e|25)/i.test(request.endpoint) ||
    /(?:^|\/)\.{1,2}(?:\/|$)/.test(request.endpoint)
  )
    throw new CliError(
      "policy",
      "trust_required",
      "Invalid trusted base endpoint.",
    );
  if (!/^\/api\/v1\/[A-Za-z0-9/_-]+$/.test(request.suffix))
    throw new CliError(
      "internal",
      "contract_violation",
      "Invalid pinned API suffix.",
    );
  url.pathname = url.pathname.replace(/\/$/, "") + request.suffix;
  for (const [key, value] of Object.entries(request.query ?? {}))
    url.searchParams.set(key, value);
  return url;
}
/** One HTTP/1.1 attempt on a fresh private Agent, with no redirect/replay middleware. */
export async function requestOnce(
  request: HttpRequest,
  signal: AbortSignal,
): Promise<HttpResponse> {
  signal.throwIfAborted();
  const url = urlFor(request),
    secure = url.protocol === "https:";
  const agent = secure
    ? new https.Agent({
        keepAlive: false,
        maxSockets: 1,
        ALPNProtocols: ["http/1.1"],
      })
    : new http.Agent({ keepAlive: false, maxSockets: 1 });
  try {
    return await new Promise<HttpResponse>((resolve, reject) => {
      const send = secure ? https.request : http.request;
      const req = send(
        url,
        {
          method: request.method,
          agent,
          signal,
          maxHeaderSize: 16384,
          headers: {
            ...request.headers,
            "accept-encoding": "identity",
            connection: "close",
            ...(request.body === undefined
              ? {}
              : {
                  "content-type": "application/json",
                  "content-length": Buffer.byteLength(request.body),
                }),
          },
        },
        (res) => {
          const fail = (error: CliError) => {
            res.destroy();
            reject(error);
          };
          if ((res.statusCode ?? 0) >= 300 && (res.statusCode ?? 0) < 400) {
            fail(
              new CliError(
                "network",
                "redirect_denied",
                "Management redirects are not followed.",
              ),
            );
            return;
          }
          if (
            res.headers["content-encoding"] &&
            res.headers["content-encoding"] !== "identity"
          ) {
            fail(
              new CliError(
                "network",
                "protocol",
                "Compressed response was not negotiated.",
              ),
            );
            return;
          }
          const chunks: Buffer[] = [];
          let size = 0;
          res.on("data", (chunk: Buffer) => {
            size += chunk.length;
            if (size > 4 * 1024 * 1024)
              fail(
                new CliError(
                  "network",
                  "response_limit",
                  "Response exceeds 4 MiB.",
                ),
              );
            else chunks.push(chunk);
          });
          res.once("error", () =>
            reject(
              new CliError("network", "protocol", "Response was interrupted."),
            ),
          );
          res.once("end", () => {
            if (!res.complete) {
              reject(
                new CliError(
                  "network",
                  "protocol",
                  "Incomplete HTTP response.",
                ),
              );
              return;
            }
            try {
              resolve({
                status: res.statusCode ?? 0,
                body: parseJson(Buffer.concat(chunks), true, 4 * 1024 * 1024),
              });
            } catch {
              reject(
                new CliError(
                  "network",
                  "protocol",
                  "Response is not valid bounded JSON.",
                ),
              );
            }
          });
        },
      );
      req.once("error", () =>
        reject(
          signal.aborted
            ? new CliError(
                "cancelled",
                "context_cancelled",
                "HTTP request was cancelled.",
              )
            : new CliError("network", "connection", "HTTP connection failed."),
        ),
      );
      req.once("upgrade", (_res, socket) => {
        socket.destroy();
        reject(
          new CliError("network", "protocol", "HTTP upgrade is forbidden."),
        );
      });
      req.end(request.body);
    });
  } finally {
    agent.destroy();
  }
}
export async function readRequest(
  request: HttpRequest,
  signal: AbortSignal,
): Promise<HttpResponse> {
  if (request.method !== "GET")
    throw new CliError(
      "internal",
      "contract_violation",
      "Read transport accepts only pinned read requests.",
    );
  for (let attempt = 0; ; attempt++) {
    try {
      const result = await requestOnce(request, signal);
      if (![429, 502, 503, 504].includes(result.status) || attempt >= 2)
        return result;
    } catch (error) {
      if (
        !(error instanceof CliError) ||
        error.subtype !== "connection" ||
        attempt >= 2 ||
        signal.aborted
      )
        throw error;
    }
    await delay(
      (attempt === 0 ? 100 : 300) + Math.floor(Math.random() * 30),
      undefined,
      { signal },
    );
  }
}
