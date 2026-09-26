---
name: seata-recovery-plan
description: Prepare human-reviewed Seata recovery recommendations from read-only evidence without executing mutations.
---

# seata-recovery-plan

This release permits investigation and recommendations only. Even mutation dry-run commands are deferred. Gather transaction, branch, lock, topology and diagnosis evidence through available reads; do not call recovery, dispatch, retry or delete endpoints.

For each proposed manual action identify the target, intended direction, prerequisite state, impact scope, possible partial side effects, evidence age and validation limits. Mark unverified server capabilities explicitly. Do not invent an executable OperationPlan or TTL guaranteed by the server; describe any recommended re-check window as advisory.

Offer alternatives only when supported by evidence. Do not promise automatic rollback or claim a plan authorizes execution. State which facts an operator must freshly verify through their separately approved operational workflow.

For evidence-reporting limits, read [references/evidence.md](references/evidence.md). A worked decision is in [examples/scenario.md](examples/scenario.md); the behavioral acceptance case is in [tests/acceptance.md](tests/acceptance.md).
