# Phase 9: Workflow Runner + DSL

## Goal

Build a declarative workflow execution engine that allows users to:
- Write reusable YAML-based automation scripts
- Parameterize test data (JSON/CSV)
- Execute against i5 and produce CI-friendly artifacts (JUnit + screenshots)

## Context

**Phases 0-8:** Complete. Core HTI5250j engine stable with 254+ contract tests, virtual threads optimized, all tests passing.

**Scope:** 2 weeks, 3-4 deliverables

## Key Questions

1. **DSL format:** YAML (simple, familiar) vs Groovy (powerful, unfamiliar)?
2. **Step actions:** What's the minimal viable action set?
   - Options: login, navigate, fill, submit, assert, wait, capture
3. **Data binding:** CSV vs JSON vs YAML inline?
4. **Artifact storage:** Local filesystem only, or S3-ready?
5. **Execution model:** Sequential only (v1) or parallel steps (v2)?

## Design Phase Decision Points

### DSL Format

**Recommendation:** YAML (high confidence)

| Criterion | YAML | Groovy |
|-----------|------|--------|
| Learnability | Excellent (non-devs can read) | Good (devs only) |
| Extensibility | Limited (data-driven only) | Excellent (arbitrary code) |
| Maintenance | Simple versioning | Complex semantics |
| CI integration | Natural (config file) | Requires build tool |
| Time to MVP | 2 weeks | 4-6 weeks |

**Trade-off:** YAML is data-driven (not Turing-complete). If custom logic needed later, move to Groovy. For now, data-driven = sufficient.

### Step Actions (Minimal Set)

```yaml
- action: login
  host: &env.host
  user: &env.user
  password: &env.password

- action: navigate
  screen: menu_screen

- action: fill
  fields:
    account: &data.account
    amount: &data.amount

- action: submit
  key: ENTER

- action: assert
  screen: confirmation_screen
  text: "Transaction approved"

- action: wait
  screen: ready_screen
  timeout: 30

- action: capture
  name: final_state
```

**Trade-off:** Extensible (add more actions) but doesn't try to handle every case. Complex scenarios (conditional logic, retries) → Phase 10+.

### Data Binding

**Recommendation:** CSV (+ JSON as fallback)

```csv
account,amount,description
1001,1000.00,Test 1
1002,2000.00,Test 2
```

Bindings: `&data.account` → CSV row column reference.

### Artifact Storage

**v1:** Local filesystem only (`./artifacts/<run-id>/`)
- Screenshots: `./artifacts/<run-id>/screens/`
- Ledger: `./artifacts/<run-id>/execution.json`
- Report: `./artifacts/<run-id>/report.html`

**v2+:** S3/artifact repo integration (out of scope).

### Execution Model

**v1:** Sequential only (step N must complete before N+1).

**v2+:** Parallel steps with dependencies (out of scope).

## Decisions Made

| Decision | Rationale | Date |
|----------|-----------|------|
| YAML DSL | Data-driven, non-dev readable, fast MVP | 2026-02-08 |
| CSV data binding | Familiar to QA, easy to parameterize | 2026-02-08 |
| Local artifacts only | Simplify scope, S3 is connector pattern (later) | 2026-02-08 |
| Sequential execution | Deterministic, testable, single-threaded (v1) | 2026-02-08 |
| 7 core actions | Cover 80% of workflow (login/nav/fill/submit/assert/wait/capture) | 2026-02-08 |

## Implementation Plan Location

Once design is approved, plan will be in: `docs/plans/2026-02-08-phase-9-workflow-runner.md`

## Status

**APPROVED** - Design phase complete. All 5 decisions validated by user (2026-02-08).

**Next:** `/build plan` (create implementation roadmap with bite-sized tasks)
