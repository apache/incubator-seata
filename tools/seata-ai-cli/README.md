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
local package identifier, not an approved Apache npm package name. Read-only
management handlers, browser login, diagnostics and the seven managed Skills
are implemented; server writes remain disabled. `capabilities` reports actual
handler/storage availability. The command catalog alone does not imply
implemented behavior.

## Run from source

Use Node.js `>=24.19.0 <25`. Linux StateStore operations require `/usr/bin/flock`
from util-linux and a private local directory. On macOS, `npm run build` needs
Apple Command Line Tools to compile the bundled universal Keychain/lock helper.
The macOS adapter accepts local APFS volumes and checks directory ownership,
permissions, symbolic links and inode identity. macOS 26.7 arm64 has been
validated; macOS x86_64 has a universal helper build but has not been run on
native hardware. Windows private-state operations still fail closed.

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

Python contract validation requires Python 3.10 or newer. Browser and live
release tests are separate opt-in commands; they require an external Playwright
installation and an isolated test deployment. See `test/browser-login.mjs` and
`test/release-integration.mjs` for their inputs. Keep test credentials out of
Profiles and command arguments.

Tests exercise the parser and actual CLI subprocess streams, compare generated
schemas against independently supplied R11 fixtures, compete through real
SQLite workers and OS lifetime locks, and count HTTP requests across redirects
and SIGKILL recovery. The installer test packs the module, installs that exact
tarball into a clean path containing spaces and non-ASCII characters, and
verifies dependencies, resources, command shims and local Profile persistence.
Local regression tests use synthetic data. Opt-in release tests use isolated
test credentials supplied by the operator.

Internal auth tests model staged credential references and epoch fencing.
macOS arm64 has additionally passed isolated Keychain, real Chromium Helper and
DB/File/Redis read-only integration against a Seata 2.6.0 test deployment.
These results do not establish production compatibility, power-loss or disk-full
recovery, or a public write executor. Windows ACLs and native macOS x86_64
remain unverified.

## Packaging and next checkpoints

`npm run build` generates protocol resources and their digests before compiling
ESM JavaScript. On macOS it also compiles a universal native helper for
BSD locks and Keychain access. A tarball built on Linux does not contain this
helper; create the macOS development tarball on macOS. Input contracts live in
`src/command/contracts.json`; supplied fixtures under `test/contract/r11`
remain independent. Generated files are not
edited. `package-lock.json` and `npm-shrinkwrap.json` pin identical dependency
trees; update both when dependencies change. No install lifecycle hook deploys
Skills, opens a browser or changes the user's StateStore.

Further gates include native macOS x86_64 and Windows validation, failure
recovery under power loss or disk exhaustion, and certified server combinations.
The command-specific write executor remains outside the read-only scope.
Publishing and official package ownership require the project's release process.
