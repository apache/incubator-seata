<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# R11 implementation checkpoint, 2026-09-24

The first development checkpoint is runnable and tested. Full R11 remains
incomplete. This checkpoint implements protocol discovery, local Profile
operations and the SQLite execution/authentication consistency model. It does
not enable server management handlers, actual browser login or production writes.

Implementation commit: `0ed10afe5`; baseline: `96e84e52f1013c484ef524afefdc81a2fb981d18`.
The implementation and test changes are isolated to `tools/seata-ai-cli`.
The follow-up workflow is independent of the Java build.

| R11 area | Implemented behavior | Remaining work |
| --- | --- | --- |
| Registry and wire protocol | 57 IDs, 56 strict Invocation schemas, typed/invoke binding, help, output DTO validation, stream/exit mapping | Additional handler-specific nested DTOs with their implementations |
| Local consistency | SQLite worker, WAL/FULL, unique claim, immutable plan, atomic SEND_INTENT, Linux lifetime locks, bounded queue/waits, schema integrity checks | Complete response/final/reconcile pipeline, capacity/retention and full disk/power-loss matrix |
| Profiles and authentication | Profile CRUD/CAS, stable identity/default selection, trust checks, staged credential metadata, flow ownership and epoch fencing model | Real CredentialStore, browser Helper, flow TTL/retention, authenticated management |
| Transport | Fresh private HTTP/1.1 agents, no login/write redirect or retry, bounded read retries, lossless large IDs, one-use permit model | Verified Console/Naming adapters and all command-specific preflight/verify rules |
| npm packaging | Compiled ESM, exact dependencies/shrinkwrap, integrity manifest, real tarball install test | Official package ownership, product Skills, platform certification, source release approval/signatures |

## Verification

Verified on Linux x64, Node 24.19.0, npm 11.17.0, TypeScript 5.9.3,
Commander 15.0.0, Ajv 8.20.0 and Python jsonschema 4.26.0.

| Check | Result | Evidence boundary |
| --- | --- | --- |
| `npm test` | Passed | 28 tests, 0 skipped; compilation plus protocol, subprocess, SQLite, HTTP and SIGKILL scenarios |
| Python `scripts/verify-contract.py` | Passed | 56 generated Invocation schemas and supplied independent examples |
| `npm run test:install` | Passed | Exact tarball installed into a clean prefix with spaces/non-ASCII; bin, resources, 7 runtime dependencies and Profile persistence checked |
| Clean indexed-source checkout | Passed | No pre-existing dist, node_modules or ignored fixtures needed |
| `npm audit --omit=dev --json` | Passed | No reported runtime dependency advisories at execution time; not a security certification |
| GitHub workflow | Not run | Workflow added; local commands verified, no remote run claimed |
| Real Seata/Keyring/browser/platform certification | Not run | No certified compatibility rows added |

## Review verdict

**Ready to merge: No, as a complete R11 feature.** The development checkpoint
is suitable for continued implementation; no unresolved defect was found in the
bounded paths reviewed after corrections. Remote functionality and release
acceptance remain incomplete.

Review mode was full for the checkpoint because it introduces public protocol,
persistence and dependency boundaries and exceeds ten non-generated files.
The accepted contract is the supplied R11 design, including unchanged R9 machine
semantics and staged M1–M6 delivery. This is not a claim that all M1 acceptance
criteria or later milestones have been met.

### Findings and corrections

Independent Pass A reviewed scope and public claims. It found a stale selected
profile after rename and availability claims independent of storage readiness.
Selection now follows `profile_id`; capabilities evaluates storage or reports
`not_evaluated`. Both corrections were independently rechecked.

Independent Pass B reviewed test integrity. Existing cancellation coverage was
only exercising logout, and plan reuse coverage only repeated one execution.
Direct pending/submitting cancellation, late credential activation and two
different executions competing for one Plan now have regression tests. Staged
credentials are bound to their originating flow. The second pass re-ran all
12 storage tests and confirmed the corrections.

The primary review also corrected ambiguous execution lookup flag binding,
stdin/file conflicts, duplicate output on write failure, resource tracking and
bounded output/list construction. Relevant tests were rerun after changes.

| Review domain | Coverage | Basis |
| --- | --- | --- |
| Requirements/scope | Deeply reviewed | R11 phase mapping, handler catalog, explicit unavailable capabilities |
| Protocol/dependencies | Deeply reviewed | Independent schemas, exact installed versions, subprocess and tarball tests |
| Security/input/files | Deeply reviewed | Strict JSON, endpoint validation, Linux descriptor-relative private storage; remote authentication excluded |
| Transactions/concurrency | Deeply reviewed | Actual SQLite workers, competing claims/plans, rollback injection, OS locks and process termination |
| Resources/cancellation | Deeply reviewed | Input/output limits, response budgets, request aborts, worker uncertainty and bounded waits |
| Server/platform compatibility | Unverified | No real Seata deployment, browser, Keyring or non-Linux platform tests |

Later changes require fresh checks for affected paths. Continue with the
remaining M1 consistency/fault cases and R11 M2's pinned real read-only Seata
combination. Keep production writes unavailable until the required evidence
exists.
