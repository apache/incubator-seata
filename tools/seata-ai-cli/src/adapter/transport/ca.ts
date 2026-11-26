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
import { constants, openSync, closeSync, fstatSync, readSync } from "node:fs";
import { basename, dirname, isAbsolute } from "node:path";
import { X509Certificate } from "node:crypto";
import { PrivateDirectory } from "../../platform/linux.js";
import { CliError } from "../../protocol/error.js";

export function loadCa(reference: string, signal: AbortSignal): string | undefined {
  signal.throwIfAborted();
  if (reference === "system") return undefined;
  const fail = () => new CliError("policy", "unsafe_ca_file", "Custom CA must be a protected absolute regular PEM certificate file.");
  if (!isAbsolute(reference) || /[\x00-\x1f\x7f]/.test(reference)) throw fail();
  const directory = new PrivateDirectory(dirname(reference), false);
  let fd: number | undefined;
  try {
    fd = openSync(directory.path + "/" + basename(reference), constants.O_RDONLY | constants.O_NOFOLLOW | constants.O_NONBLOCK);
    const before = fstatSync(fd, { bigint: true });
    if (!before.isFile() || before.uid !== BigInt(process.getuid!()) || before.nlink !== 1n ||
        (before.mode & 0o077n) !== 0n || before.size < 1n || before.size > 262144n) throw fail();
    const bytes = Buffer.alloc(Number(before.size) + 1);
    let size = 0;
    while (size < bytes.length) {
      signal.throwIfAborted();
      const n = readSync(fd, bytes, size, bytes.length - size, null);
      if (!n) break;
      size += n;
    }
    const after = fstatSync(fd, { bigint: true });
    if (BigInt(size) !== before.size || after.size !== before.size || after.mtimeNs !== before.mtimeNs ||
        after.ctimeNs !== before.ctimeNs || after.nlink !== before.nlink) throw fail();
    const pem = new TextDecoder("utf-8", { fatal: true }).decode(bytes.subarray(0, size));
    const certificates = pem.match(/-----BEGIN CERTIFICATE-----[\s\S]*?-----END CERTIFICATE-----/g) ?? [];
    if (!certificates.length || certificates.length > 16 ||
        pem.replace(/-----BEGIN CERTIFICATE-----[\s\S]*?-----END CERTIFICATE-----/g, "").trim()) throw fail();
    for (const certificate of certificates) if (!new X509Certificate(certificate).ca) throw fail();
    return pem;
  } catch {
    signal.throwIfAborted(); throw fail();
  } finally { if (fd !== undefined) closeSync(fd); directory.close(); }
}
