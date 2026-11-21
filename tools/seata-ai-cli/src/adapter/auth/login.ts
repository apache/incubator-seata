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
import { requestOnce } from "../transport/http.js";
import { isObject } from "../../protocol/value.js";
import { CliError } from "../../protocol/error.js";
import { bearer } from "./credentials.js";

/** The sole password-send path: one POST, no retries or redirects. */
export async function login(endpoint: string, username: string, password: string, signal: AbortSignal, ca?: string, proxyEndpoint?: string): Promise<string> {
  if (!username || !password || username.length > 4096 || password.length > 4096)
    throw new CliError("authentication", "login_rejected", "Login fields are missing or too large.");
  const response = await requestOnce({ endpoint, suffix: "/api/v1/auth/login", method: "POST",
    ...(ca === undefined ? {} : { ca }),
    ...(proxyEndpoint === undefined ? {} : { proxyEndpoint }),
    body: JSON.stringify({ username, password }) }, AbortSignal.any([signal, AbortSignal.timeout(15000)]));
  if (response.status !== 200 || !isObject(response.body) || response.body.code !== "200")
    throw new CliError("authentication", "login_rejected", "Console did not accept the login.");
  if (typeof response.body.data !== "string" || !response.authorization || response.authorization !== response.body.data)
    throw new CliError("authentication", "invalid_credential", "Console credential headers and data do not match.");
  return bearer(response.authorization);
}
