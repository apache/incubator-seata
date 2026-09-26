---
name: seata-integration-check
description: Check local Seata integration configuration against observed read-only connectivity and topology evidence.
---

# seata-integration-check

Use environment.inspect, environment connectivity check, topology reads and config.inspect/validate as evidence permits. Local inspection must remain local unless a separately scoped remote read is requested. Use help/schema for exact commands and explicit Profile selection.

Report checks as passed, failed, unknown or not_evaluated, with sources and missing evidence. Local project files and the CLI process environment do not establish the effective Seata Server/TC JVM configuration. Placeholders, precedence and unobserved versions remain unknown unless separately verified.

Compare vgroup and routing observations without silently changing configuration. Preserve selector-only routing and partial discovery limitations. Optional logs, metrics, database or config-center providers that were not collected cannot count as passing checks.

For evidence-reporting limits, read [references/evidence.md](references/evidence.md). A worked decision is in [examples/scenario.md](examples/scenario.md); the behavioral acceptance case is in [tests/acceptance.md](tests/acceptance.md).
