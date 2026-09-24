<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Seata AI CLI — R11 development foundation

This independent npm module implements the first R11 checkpoint: command
discovery, strict protocol input/output, local Profiles, and a transactional
execution/authentication model. It preserves Protocol `0.1`, contract revision
`R9`, all 57 command IDs and their CLI paths.

The package is a private development snapshot. `seata-ai-cli-development` is a
local package identifier, not an approved Apache npm package name. Server read
and write handlers, browser login, Keyring, diagnostic providers and the seven
product Skills are not enabled. `capabilities` reports actual handler/storage
availability. The command catalog alone does not imply implemented behavior.

## Run from source

Use Node.js `>=24.19.0 <25`; the checked environment uses Node 24.19.0,
npm 11.17.0 and TypeScript 5.9.3. Linux StateStore operations additionally require
`/usr/bin/flock` from util-linux and a private local directory. Other platforms
can use discovery commands; private-state operations fail closed.

```sh
cd tools/seata-ai-cli
npm ci --ignore-scripts
npm run build
node dist/cli.js --help
node dist/cli.js version
node dist/cli.js schema transaction.retry.pause
node dist/cli.js capabilities
```

`invoke` reads one Canonical Invocation from stdin or `--file`. Typed flags and
JSON invocation use the same Registry, schema and semantic validator. Input is
limited to 1 MiB and 32 nesting levels. Duplicate decoded keys, invalid UTF-8,
unsafe numbers, unknown fields and ambiguous repeated flags are rejected.

State is stored under `~/.seata-ai`; `SEATA_AI_STATE_DIR` selects an explicit
alternative private state directory. Use a fresh directory for this development
build. No migration from the discarded Go implementation is performed.
Profile mutations require explicit commands; management endpoint trust remains
separate from authenticated/compatible server support.

## Validation

```sh
npm test
python3 -m venv .venv
.venv/bin/pip install -r test/requirements.txt
.venv/bin/python scripts/verify-contract.py
npm run test:install
```

Tests exercise the parser and actual CLI subprocess streams, compare generated
schemas against independently supplied R11 fixtures, compete through real
SQLite workers and OS lifetime locks, and count HTTP requests across redirects
and SIGKILL recovery. The installer test packs the module, installs that exact
tarball into a clean path containing spaces and non-ASCII characters, and
verifies dependencies, resources, command shims and local Profile persistence.
Temporary test directories contain only synthetic data.

Internal auth tests model staged credential references and epoch fencing;
they do not establish real Keyring or browser integration. Internal write-model
tests establish local invariants, not Seata compatibility or a public write
executor. There are no certified server combinations and production writes
remain unavailable. Windows ACLs, macOS storage/locks, browser Helper, actual
power-loss/disk-full recovery and real Seata E2E remain unverified.

## Packaging and next checkpoints

`npm run build` generates protocol resources and their digests before compiling
ESM JavaScript. Input contracts live in `src/command/contracts.json`; supplied
fixtures under `test/contract/r11` remain independent. Generated files are not
edited. `package-lock.json` and `npm-shrinkwrap.json` pin identical dependency
trees; update both when dependencies change. No install lifecycle hook deploys
Skills, opens a browser or changes the user's StateStore.

Continue with R11 M2's pinned real Seata read-only combination and captured
fixtures, M3 browser/credential integration, M4 diagnostics/Skills, and M5/M6
the command-specific write executor and certification gates. Publishing and
official package ownership require the project's release process.
