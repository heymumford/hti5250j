# Martin Fowler Patterns: HTI5250J Implementation Checklist

**Purpose:** Track which Fowler AI patterns are implemented in each project phase.

**Key:** ✅ = Already done | 🔄 = In progress | ⏳ = Planned | ❌ = Not recommended

---

## Pattern 1: Evals (Systematic Non-Determinism Assessment)

### Fowler Principle
"Evals play a central role in ensuring that non-deterministic systems are operating within sensible boundaries."

### HTI5250J Implementation Status

| Component | Phase | Status | Evidence |
|-----------|-------|--------|----------|
| **Unit test scorers** (automated) | Phase 1 | ✅ | Contract tests verify codec, queue ordering |
| **Continuous monitors** (LLM-judge equivalent) | Phase 2 | ✅ | Domain 2: 24/7 schema/protocol drift detection |
| **Surface test verifiers** | Phase 6 | ✅ | Domain 3: protocol round-trip, field bounds, concurrency |
| **Scenario verifiers** | Phase 8 | ✅ | Domain 4: payment/settlement/error workflows |
| **Eval metrics (correctness/idempotency/latency)** | Phase 12E | ⏳ | Recommended next |

### Compliance Verification

**Question:** "Can we measure if workflow execution quality degrades?"
- ✅ Yes - Domain 4 scenario tests verify outcomes
- ⏳ Not yet - No EvalScorer with numeric metrics (0.0-1.0)

### Action Items (Phase 12E)

- [ ] Create `EvalScorer` interface with three scoring methods
- [ ] Implement `CorrectnessEval` (transaction amounts preserved)
- [ ] Implement `IdempotencyEval` (run 2x, same result)
- [ ] Implement `LatencyEval` (execution time within SLA)
- [ ] Add eval runner to `WorkflowExecutor`
- [ ] Integrate evals into Domain 4 tests

---

## Pattern 2: Bounded Contexts & Event Sourcing (Memory Architecture)

### Fowler Principle
"Bounded Contexts partition memory into semantic boundaries. Aggregates function as cohesive memory clusters. Event Sourcing provides episodic memory."

### HTI5250J Implementation Status

| Component | Phase | Status | Evidence |
|-----------|-------|--------|----------|
| **Domain-driven design vocabulary** | Phase 10-12 | ✅ | StepDef, WorkflowSchema, Validators |
| **Action types as semantic units** | Phase 12D | ✅ | Sealed Actions: LoginAction, NavigateAction, etc. |
| **Aggregate root (WorkflowExecution)** | - | ❌ | Currently scattered state in method parameters |
| **Domain events (sealed hierarchy)** | - | ⏳ | Needed for replay/audit trail |
| **Event sourcing (complete history)** | - | ⏳ | Currently logs discarded after execution |
| **State projection from events** | - | ⏳ | Needed for replay/forensics |

### Compliance Verification

**Question:** "Can we replay a failed workflow to understand exactly what happened?"
- ❌ No - No event log captured, logs discarded
- ⏳ Yes (Phase 12F) - Once WorkflowExecution records events

**Question:** "Can agents (human or AI) review semantic history before retrying?"
- ❌ No - No structured history available
- ⏳ Yes (Phase 12F) - Once events become first-class

### Action Items (Phase 12F)

- [ ] Create `WorkflowExecution` aggregate root with sealed interface
- [ ] Define sealed `WorkflowDomainEvent` hierarchy
- [ ] Implement `WorkflowExecutionState` projection logic
- [ ] Refactor `WorkflowRunner` to emit events
- [ ] Add event replay capability for post-mortems
- [ ] Update Domain 4 tests to verify event sequences

---

## Pattern 3: Lethal Trifecta Defense (Security)

### Fowler Principle
"Break up tasks so each sub-task blocks at least one element of: (1) sensitive data, (2) untrusted content, (3) external communication. Run in containers with controlled access."

### HTI5250J Implementation Status

| Component | Phase | Status | Evidence |
|-----------|-------|--------|----------|
| **Sealed interface prevents unauthorized actions** | Phase 12D | ✅ | Action sealed type: only 7 permitted subtypes |
| **Input validation at record creation** | Phase 12D | ⏳ | LoginAction requires non-null host/user/password |
| **Capability isolation per action** | Phase 12D | 🟡 | Handlers enforce constraints, but not formalized |
| **Audit log of all executions** | Phase 11 | 🟡 | Logs printed to console, not structured |
| **Container isolation constraints** | - | ❌ | Not applicable (single JVM process) |
| **Network allowlist restrictions** | - | ⏳ | Phase 12G: formalize in capability matrix |

### Compliance Verification

**Question:** "Can an attacker inject a malicious step into a workflow?"
- ✅ No - Sealed Actions prevents unknown action types
- ✅ No - LoginAction rejects non-host parameters (narrowing)

**Question:** "Can we trace what actions executed and what data they accessed?"
- 🟡 Partial - Console logs exist, but not machine-parseable audit trail

### Action Items (Phase 12G)

- [ ] Add explicit input validation constructors to all Actions
- [ ] Create structured `AuditLog` (JSON or database-backed)
- [ ] Document capability matrix per Action type
- [ ] Formalize network constraints (no outbound except logging)
- [ ] Add security tests: "CaptureAction cannot write to /etc/passwd"

---

## Pattern 4: Human Review in Slices (Governance)

### Fowler Principle
"Break work into thin slices. Review everything closely, treating each slice as a PR from a dodgy collaborator. Never let agents run without human review between steps."

### HTI5250J Implementation Status

| Component | Phase | Status | Evidence |
|-----------|-------|--------|----------|
| **Workflow validation (structural + action-specific)** | Phase 10 | ✅ | ValidationResult with errors/warnings |
| **Parameter validation (dataset matching)** | Phase 10 | ✅ | ParameterValidator checks ${data.x} exists |
| **CLI prevents invalid workflows from running** | Phase 10 | ✅ | `i5250 validate` blocks execution on errors |
| **Simulation/dry-run before execution** | - | ⏳ | Recommended Phase 12E feature |
| **Approval gate (human clicks "run")** | Phase 11 | 🟡 | WorkflowCLI runs if valid, but no approval step |
| **Review step failures before retry** | - | ⏳ | Phase 12E: simulation + approval |

### Compliance Verification

**Question:** "Can a human review what a workflow will do before it executes?"
- 🟡 Partial - ValidationResult shows errors, but not execution flow
- ⏳ Yes (Phase 12E) - Simulator will show step-by-step execution

**Question:** "Are humans forced to review before risky actions?"
- 🟡 Partial - Only if validation fails
- ⏳ Yes (Phase 12E) - With simulation preview + approval

### Action Items (Phase 12E)

- [ ] Create `WorkflowSimulator` (execute against MockScreen)
- [ ] Generate simulation report: "Step 0: LOGIN → connect → ✓"
- [ ] Require human approval: "Proceed with real execution?"
- [ ] Log approval decision to audit trail
- [ ] Add error recovery approval: "Step 3 failed, retry with fallback?"

---

## Pattern 5: Tolerances for Nondeterminism (Metrics)

### Fowler Principle
"Define acceptable precision/latency bounds, just as bridges define safe load capacity. Come with metrics describing tolerance levels."

### HTI5250J Implementation Status

| Component | Phase | Status | Evidence |
|-----------|-------|--------|----------|
| **Timeout bounds defined** | Phase 11 | ✅ | keyboardUnlockMaxLatency = 30s, etc. |
| **Timeout enforcement in handlers** | Phase 11 | ✅ | SessionPanel.waitForKeyboardUnlock() enforces |
| **Decimal precision specs** | Phase 8 | ✅ | Domain 3 tests verify no truncation |
| **String field bounds** | Phase 6 | ✅ | SchemaContractVerifier checks field sizes |
| **Error recovery tolerance** | Phase 11 | 🟡 | Retry logic exists, max attempts not configurable |
| **Tolerance specification in YAML** | - | ⏳ | Phase 12E: WorkflowTolerance record |
| **Tolerance compliance metrics** | - | ⏳ | Phase 12E: WorkflowExecutionMetrics |
| **Dashboard/monitoring of tolerances** | - | ⏳ | Phase 12F: MetricsCollector |

### Compliance Verification

**Question:** "If a workflow executes slower than expected, do we alert?"
- ❌ No - No monitoring, logs discarded after completion

**Question:** "Can we declare 'this workflow must complete in < 5 minutes'?"
- ❌ No - No tolerance specification in YAML

### Action Items (Phase 12E)

- [ ] Create `WorkflowTolerance` record (latency, precision, retry bounds)
- [ ] Update `WorkflowSchema` to include optional tolerances
- [ ] Implement `WorkflowExecutionMetrics` to capture actual values
- [ ] Add compliance check: metrics vs declared tolerances
- [ ] Emit metrics to artifact ledger
- [ ] Document tolerance specs in CODING_STANDARDS.md

---

## Pattern 6: Code Structure (Design Patterns)

### Fowler Principle
"Well-constructed code with modular approaches makes it easier for agents (AI or human) to reason about and maintain."

### HTI5250J Implementation Status

| Pattern | Phase | Status | Evidence |
|---------|-------|--------|----------|
| **Sealed types** | Phase 12D | ✅ | Action sealed interface with 7 implementations |
| **Records (immutable data)** | Phase 3, 12D | ✅ | Rect, LoginAction, ValidationError records |
| **Pattern matching** | Phase 4 | ✅ | Switch expressions on ActionType |
| **Single Responsibility** | Phase 12A | ✅ | ArgumentParser, SessionFactory, TerminalAdapter |
| **Dependency injection** | Phase 11 | ✅ | WorkflowRunner(SessionInterface, ScreenProvider) |
| **Value objects** | Phase 12 | 🟡 | Screen5250Content = raw char[] (not wrapped) |
| **Strategy pattern** | - | ⏳ | RetryStrategy interface (Phase 12E) |
| **Builder pattern** | - | ⏳ | WorkflowExecutorBuilder (Phase 12E) |

### Compliance Verification

**Question:** "Is the codebase easy for agents (human or AI) to reason about?"
- ✅ Mostly - Clear separation of concerns, immutable data, sealed types
- 🟡 Minor gaps - Screen content not wrapped in value object

### Action Items (Phase 12E+)

- [ ] Create `Screen5250Content` value object wrapping char[] array
- [ ] Create `RetryStrategy` interface for configurable retry logic
- [ ] Create `WorkflowExecutorBuilder` to reduce parameter explosion
- [ ] Add semantic query methods to Screen5250Content

---

## Summary: Fowler Pattern Coverage by Phase

### ✅ Patterns Fully Implemented

| Pattern | Phase | Why It Works |
|---------|-------|------------|
| **Evals (unit/surface/scenario)** | 1, 6, 8 | 4-domain test architecture verifies non-determinism |
| **Lethal trifecta defense** | 12D | Sealed Actions prevent unauthorized operations |
| **Code structure** | 3, 4, 12A-D | SRP, records, pattern matching, sealed types |

### 🔄 Patterns Partially Implemented

| Pattern | Phase | Gap | Next Step |
|---------|-------|-----|-----------|
| **Human review** | 10, 11 | No simulation/dry-run | Phase 12E: Add simulator |
| **Tolerances** | 11 | No configurable bounds | Phase 12E: WorkflowTolerance record |
| **Bounded contexts** | 12D | Action types good, memory scattered | Phase 12F: Event sourcing |

### ⏳ Patterns Deferred

| Pattern | Planned | Why Deferred |
|---------|---------|---------------|
| **Event sourcing** | Phase 12F | Needed for replay/audit, not Phase 11 blocker |
| **Eval metrics** | Phase 12E | Nice-to-have for Phase 12E readiness |
| **Strategy pattern** | Phase 12E | Current static timeouts sufficient, make configurable later |

---

## Quality Metrics (Updated to Fowler Standard)

| Dimension | Current | Target | Phase |
|-----------|---------|--------|-------|
| **Test coverage** | 4 domains (254+ tests) | 5 domains (add evals) | 12E |
| **Security isolation** | Sealed types | Audit trail + allowlist | 12G |
| **Error visibility** | Console logs | Event log + PostMortemAnalyzer | 12F |
| **Tolerance definition** | Hardcoded timeouts | YAML + metrics | 12E |
| **Human oversight** | Validation only | Validation + simulation | 12E |

---

## Recommendation: Phase 12E Priority

Start with Phase 12E to implement Fowler's **"Tolerances for Nondeterminism"** and **"Evals over Assumptions"** patterns:

1. **WorkflowTolerance record** (2h) - "What are acceptable bounds?"
2. **EvalScorer implementations** (4h) - "Did we stay within bounds?"
3. **WorkflowSimulator** (4h) - "What WOULD happen without touching i5?"
4. **Metrics integration** (3h) - "Did actual execution match tolerances?"

**Expected outcome:** Demonstrate that workflow execution is reliable (metrics prove it), safe (bounded by declared tolerances), and reviewable (simulation before execution).

This unlocks Phase 13 (multi-agent coordination) by providing agents with: metrics, tolerances, and dry-run validation before committed execution.
