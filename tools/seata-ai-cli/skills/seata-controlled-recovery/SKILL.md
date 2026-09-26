---
name: seata-controlled-recovery
description: Handle requests to execute Seata recovery by explaining the read-only boundary and checking historical execution evidence.
---

# seata-controlled-recovery

The name is retained for compatibility; this distribution does not execute controlled or high-risk server actions. State that restriction when asked to pause/resume retries, dispatch, recover status or delete transactions, branches or locks. Do not route around it with shell HTTP calls or an alternative request ID.

For historical executions preserve the original UUID and state_store_id. Use execution.get and available read-only observations to inspect the outcome. If a read-only reconciliation command is unavailable, report that limitation rather than claiming completion. Missing Envelope, partial or indeterminate state must not trigger automatic replay.

Provide a handoff containing observed target, intended direction, evidence and unresolved outcome. Any future mutating workflow requires separate specific authorization and a version that actually supports it; a recommendation here is not permission to execute.

For evidence-reporting limits, read [references/evidence.md](references/evidence.md). A worked decision is in [examples/scenario.md](examples/scenario.md); the behavioral acceptance case is in [tests/acceptance.md](tests/acceptance.md).
