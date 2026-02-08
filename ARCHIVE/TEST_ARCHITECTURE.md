# Test Architecture

| Field | Value |
| --- | --- |
| Purpose | Define the four-domain test architecture and execution cadence |
| Applies to | All automated testing and verification |
| Last updated | 2026-02-07 |

## Summary

The test architecture is inverted from the traditional pyramid to protect against silent data loss at system boundaries. It prioritizes protocol, schema, and workflow verification against real IBM i behavior.

## Four-Domain Model

| Domain | Purpose | Environment | Typical Cadence |
| --- | --- | --- | --- |
| Domain 1: Unit | Validate translation-layer logic | Local | Pre-commit |
| Domain 2: Continuous Contracts | Detect schema, protocol, and execution drift | Real IBM i | 24/7 background |
| Domain 3: Surface | Verify boundary correctness and idempotency | Real IBM i | Per commit or scheduled |
| Domain 4: Scenario | Validate end-to-end business workflows | Real IBM i | Per commit or nightly |

## Execution Cadence

| Phase | Domains | Target Duration | Gate |
| --- | --- | --- | --- |
| Pre-commit | Domain 1 | < 5s | Local quality gate |
| Pre-push | Domains 1-3 (critical paths) | < 60s | Developer gate |
| Pre-merge | Domains 1-4 | 5-10 min | CI gate |
| Continuous | Domain 2 | 24/7 | Drift detection |

## Evidence and Artifacts

Each run should emit:
- JUnit XML and JSON summaries
- Screen snapshots and field maps
- Keystroke timeline with host response markers
- Contract diffs for schema or screen changes

## Risk Coverage

| Risk | Primary Domain |
| --- | --- |
| Logic error in translation layer | Domain 1 |
| Schema drift or protocol mismatch | Domain 2 |
| Boundary data loss and idempotency | Domain 3 |
| Workflow failure and SLA regression | Domain 4 |

## Phase References

- `PHASE_6_DOMAIN_3_SUMMARY.md`
- `PHASE_6_COMPLETION_REPORT.md`
- `PHASE_8_DOMAIN_4_SUMMARY.md`
- `PHASE_8_SPRINT_3_STRESS_SUMMARY.md`
