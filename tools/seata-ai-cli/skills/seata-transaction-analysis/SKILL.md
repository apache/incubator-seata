---
name: seata-transaction-analysis
description: Investigate a Seata transaction, branches and locks using observed read-only relationships and timelines.
---

# seata-transaction-analysis

Start with transaction.get for the explicit target, then branch.list and lock.list; use transaction.trace to assemble available evidence. Consult help/schema for selectors; preserve identifiers as strings, including int64-scale branch IDs.

Separate observed transaction/branch/resource relationships from inferred causality. Unknown enum values must stay unknown. Include pagination limits, truncation, missing sources and selector-only route assurance with conclusions. A snapshot is best-effort, not an atomic cluster view.

A lock row or shared resource is not by itself a confirmed waiting edge or deadlock. lock.check reports owner_session_exists, not a general lock_exists assertion. Do not invoke retry, recovery, dispatch or deletion actions.

For evidence-reporting limits, read [references/evidence.md](references/evidence.md). A worked decision is in [examples/scenario.md](examples/scenario.md); the behavioral acceptance case is in [tests/acceptance.md](tests/acceptance.md).
