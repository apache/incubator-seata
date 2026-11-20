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

# Seata AI CLI

The Seata AI CLI development package provides command discovery, validated JSON
invocation, and structured output for scripts and agents. This foundation ships
`version`, `help`, `schema`, `capabilities`, and `invoke`. Seata server queries
and local profiles will be added with their implementations.

Use Node.js 22.13 or later in the 22 series, or 24.19 or later in the 24 series.

```sh
npm ci --ignore-scripts
npm run build
node dist/cli.js --help
node dist/cli.js schema system.version
node dist/cli.js capabilities
```

Use `invoke` with a JSON request from stdin or `--file`. The same command
validation applies to typed arguments and JSON input.

```sh
printf '%s\n' '{"protocol_version":"0.1","contract_revision":"R9","command":"system.version","context":{},"input":{},"options":{}}' |
  node dist/cli.js invoke
```

The build generates the registry and packaged schemas from
`src/command/contracts.json`. The public `system.invoke` schema is composed from
the registered commands. The per-command fixtures in `test/contract/r11` check
the generated contract independently. The generated `api/` and `schemas/`
directories are excluded from Git.

Run `npm test` for the protocol tests. Install the Python dependencies in
`test/requirements.txt` and run `npm run check` to include independent contract
validation and installation from the packed tarball.
