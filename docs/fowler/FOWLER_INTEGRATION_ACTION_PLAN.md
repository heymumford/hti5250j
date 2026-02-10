# Martin Fowler AI Architecture Integration - Action Plan

**Date:** 2026-02-09
**Source:** Research agents + Martin Fowler's 2024-2026 GenAI publications
**Applied to:** HTI5250J Phase 12E-14 roadmap
**Status:** 📋 PLANNING

---

## Executive Summary

Martin Fowler's recent work on AI systems reveals that **enterprise-grade AI is not fundamentally about smarter algorithms—it's about operational discipline**. Five principles form the foundation:

1. **EVALS** — Non-determinism as a first-class problem (metrics, not mocks)
2. **BOUNDED CONTEXTS** — Semantic memory organization (events, not logs)
3. **LETHAL TRIFECTA DEFENSE** — Isolate credentials + untrusted input + network access
4. **HUMAN REVIEW IN SLICES** — Thin slices + dry-runs + approval gates
5. **TOLERANCES** — Declare bounds; measure compliance

**HTI5250J Current State:** 68% compliance (see FOWLER_RESEARCH_SUMMARY.md)

**Recommendation:** Execute Phase 12E (Metrics & Tolerances) immediately. This unblocks Phase 13 (multi-agent coordination) and Phase 14 (production validation).

---

## Phase 12E: Metrics & Tolerances (Q1 2026) — CRITICAL PATH

**Why Now?**
- Phase 13 (batch virtual threads) is complete but unmeasured
- Phase 14 (high-rigor tests) needs quality thresholds to verify against
- User selected "highest rigor" testing methodology (2026-02-08)

**Principle:** "Just as concrete bridges have weight limits, AI systems must declare their nondeterminism tolerances."

### Deliverables

#### 1. WorkflowTolerance Record (2 hours)
```java
public record WorkflowTolerance(
    String workflowName,
    long maxDurationMs,        // e.g., 300000 (5 minutes)
    double fieldPrecision,     // e.g., 0.01 (monetary precision)
    int maxRetries,           // e.g., 3
    boolean requiresApproval   // false for automated, true for human review
) {
    // YAML serialization: tolerances: { maxDurationMs: 300000, ... }
}
```

**Integration:** Updated in YAML workflow files:
```yaml
name: payment_processing
tolerances:
  maxDurationMs: 300000        # 5 min per workflow
  fieldPrecision: 0.01         # Monetary precision
  maxRetries: 3
  requiresApproval: true       # Always review payments
steps: [...]
```

**Testing:** WorkflowToleranceTest (6 tests)
- YAML deserialization
- Boundary validation
- Violation detection

#### 2. EvalScorer Interface + Implementations (4 hours)

```java
public interface EvalScorer {
    double evaluate(WorkflowResult result, WorkflowTolerance tolerance);
    // Returns 0.0 (failed) to 1.0 (perfect)
}

// Three critical scorers:

class CorrectnessScorer implements EvalScorer {
    // Did the workflow produce correct outputs?
    // Checks: field values match expected, no silent truncation
}

class IdempotencyScorer implements EvalScorer {
    // Can we run it twice and get the same result?
    // Checks: workflow state is deterministic end-to-end
}

class LatencyScorer implements EvalScorer {
    // Did it complete within tolerance?
    // Checks: duration < tolerance, no timeout violations
}
```

**Metrics Output:**
```
Workflow: payment_processing | 100 workflows
- Correctness: 99.8% (1 field mismatch out of 500)
- Idempotency: 100.0% (all retries matched)
- Latency: 98.2% (2 workflows exceeded 5min tolerance)
- Overall Quality: 0.993 (99.3%)
```

**Testing:** EvalScorerTest (12 tests)
- Scorer implementations
- Edge case handling (NaN, Infinity, boundary)
- Tolerance violation detection

#### 3. WorkflowSimulator (Dry-Run) Framework (4 hours)

**Purpose:** "Let humans preview what WOULD happen before executing on real i5"

```java
public class WorkflowSimulator {
    public WorkflowSimulation simulate(WorkflowDefinition workflow, Map<String, String> testData) {
        // Execute using MockScreen instead of real Screen5250
        // Return: predicted outcome without touching i5
    }
}

public record WorkflowSimulation(
    List<SimulatedStep> steps,
    String predictedOutcome,      // "success" | "timeout" | "validation_error"
    Map<String, String> predictedFields,  // What would be filled
    List<String> warnings         // "Field 'amount' would exceed 7 chars"
) {}
```

**Usage:**
```bash
# User workflow before approval
$ i5250 simulate examples/payment.yaml --data examples/test_payments.csv
Simulating: 1 workflow
  Step 1 (LOGIN): ✓ Predicts success
  Step 2 (NAVIGATE): ✓ Screen reached (expected)
  Step 3 (FILL): ⚠ Warning: 'amount' field would be truncated from 10 to 7 chars
  Step 4 (SUBMIT): ✓ Predicts success
Result: Would complete in 850ms (within 5min tolerance)
Approval: RECOMMEND → human review the amount truncation before deploying
```

**Testing:** WorkflowSimulatorTest (8 tests)
- Happy path simulation
- Predicted timeout detection
- Predicted validation errors
- Warning generation

#### 4. Integration with WorkflowValidator (1 hour)

Extend validation to include tolerance checks:

```java
public class WorkflowValidator {
    public ValidationResult validate(WorkflowSchema workflow, WorkflowTolerance tolerance) {
        // Check: steps don't exceed maxDurationMs in sum
        // Check: field precision matches tolerance
        // Check: retry count <= maxRetries
    }
}
```

#### 5. BatchMetrics Enhancement (1 hour)

Extend Phase 13's BatchMetrics to include eval scores:

```java
public record BatchMetrics(
    int totalWorkflows,
    int successCount,
    int failureCount,
    long p50LatencyMs,
    long p99LatencyMs,
    double throughputOpsPerSec,
    double correctnessScore,      // NEW: 0.0-1.0
    double idempotencyScore,      // NEW: 0.0-1.0
    double latencyScore,          // NEW: 0.0-1.0
    double overallQuality,        // NEW: geometric mean
    List<WorkflowResult> failures
) {}
```

**Reporting:**
```
Batch Execution Report
=====================
Throughput: 587,231 ops/sec (1000 workflows)
Quality Metrics:
  - Correctness: 99.8%
  - Idempotency: 100.0%
  - Latency: 98.2%
  - Overall: 0.993 (99.3% quality)

Tolerance Compliance:
  - maxDurationMs: 4250ms average (tolerance: 5000ms) ✓
  - fieldPrecision: 0.00 error (tolerance: 0.01) ✓
  - maxRetries: 0 used (tolerance: 3) ✓
```

### Files to Create (9)

| File | LOC | Purpose |
|------|-----|---------|
| WorkflowTolerance.java | 30 | Record definition + YAML binding |
| EvalScorer.java | 15 | Interface definition |
| CorrectnessScorer.java | 45 | Implementation |
| IdempotencyScorer.java | 40 | Implementation |
| LatencyScorer.java | 35 | Implementation |
| WorkflowSimulator.java | 95 | Dry-run execution |
| WorkflowSimulation.java | 25 | Record definition |
| WorkflowToleranceTest.java | 85 | Unit tests (6) |
| EvalScorerTest.java | 140 | Unit tests (12) |
| WorkflowSimulatorTest.java | 165 | Integration tests (8) |

### Files to Modify (3)

| File | Lines | Change |
|------|-------|--------|
| WorkflowValidator.java | +40 | Tolerance validation |
| WorkflowCLI.java | +25 | `simulate` command |
| BatchMetrics.java | +12 | Eval score fields |

### Effort Estimate

| Task | Hours | Effort |
|------|-------|--------|
| WorkflowTolerance design | 1 | 1×1 |
| EvalScorer implementations | 4 | 1×4 |
| WorkflowSimulator implementation | 4 | 1×4 |
| Integration + CLI | 2 | 1×2 |
| Testing (unit + integration) | 4 | 2×2 |
| Documentation | 1 | 1×1 |
| **Total** | **16** | **2 developers, 1 week** |

### Success Criteria

- ✅ All 26 new tests pass (WorkflowToleranceTest + EvalScorerTest + WorkflowSimulatorTest)
- ✅ `i5250 simulate` command runs without i5 connection
- ✅ Warnings prevent data loss (e.g., "amount would be truncated")
- ✅ BatchMetrics reports all 3 eval scores + overall quality
- ✅ YAML tolerances deserialized and validated
- ✅ Zero regressions (Phase 13 tests still pass)

---

## Phase 12F: Bounded Contexts & Event Sourcing (Q2 2026)

**Fowler Principle:** "Event Sourcing provides episodic memory for agents."

**Why Phase 12E first?** Event Sourcing requires knowing what "events" matter (Phase 12E defines tolerances; Phase 12F serializes them).

### Deliverables (Summary)

1. **WorkflowExecution Aggregate Root**
   - Encapsulates: workflow + all steps + state + complete event history
   - Purpose: "Where are we? How did we get here? Can we recover?"

2. **Sealed WorkflowDomainEvent Hierarchy**
   - Events: WorkflowStarted, StepStarted, StepCompleted, StepFailed, WorkflowCompleted
   - Immutable record definitions
   - Fully serializable to JSON for audit trail

3. **WorkflowEventStore Interface**
   - append(WorkflowExecution event)
   - retrieve(workflowId)
   - replay(workflowId, toStep) — for forensics
   - Implementation: FileEventStore (JSON-per-workflow)

4. **Error Recovery via Replay**
   - `i5250 resume <workflow-id> --from-step 3`
   - Replay steps 1-2 to restore state, continue from step 3

### Effort: 20 hours (Phase 12F detailed in FOWLER_AI_PATTERNS.md)

---

## Phase 12G: Lethal Trifecta Hardening (Q2 2026)

**Fowler Principle:** Sealed Actions defend against injection, but audit logs complete the defense.

### Deliverables (Summary)

1. **Structured Audit Log**
   - Action type + parameters + execution time + result
   - NO secret data (redact passwords, field values)
   - Signed/sealed to detect tampering

2. **Action Capability Matrix**
   - LoginAction: read credentials? (yes) | write i5? (no) | network? (no)
   - CaptureAction: read credentials? (no) | write disk? (yes, ./artifacts/) | network? (no)
   - Prevents unexpected combinations

3. **Attack Surface Documentation**
   - "What are all the inputs we accept?"
   - "Where could untrusted data enter?"
   - "What actions could expose secrets?"

### Effort: 12 hours (Phase 12G detailed in FOWLER_AI_PATTERNS.md)

---

## Phase 14: High-Rigor Testing (Parallel with 12E-G)

**User Decision (2026-02-08):** Adopt formal test ID methodology.

**Integration with Fowler Principles:**
- **Evals:** Phase 14 D1-D4 tests ARE your eval suite (Fowler confirms)
- **Tolerances:** Phase 12E declares them; Phase 14 tests verify compliance
- **Human Review:** Phase 12E simulator enables human approval gates
- **Bounded Contexts:** Phase 14 tests verify state transitions at context boundaries

### Parallel Execution

```
Timeline (2026-02-09 onward)

Week 1-2 (Phase 12E):
  - WorkflowTolerance + EvalScorer (8 hours)
  - WorkflowSimulator (4 hours)

Week 2-3 (Phase 14, parallel):
  - Create minimal D3-SCHEMA test (2 hours)
  - Expand D3 surface tests (4 hours)
  - Add D1 unit tests (2 hours)

Week 3-4 (Phase 12F):
  - Event Sourcing design + implementation (20 hours)

Week 4-5 (Phase 12G):
  - Audit log + capability matrix (12 hours)
```

**Critical Path:** Phase 12E unblocks everything else.

---

## Risk Assessment

| Risk | Mitigation | Owner |
|------|-----------|-------|
| Phase 12E complexity | Break into 3 independent components (Tolerance, Scorer, Simulator) | Dev lead |
| Test flakiness (non-determinism) | EvalScorer handles variation; tolerances define bounds | QA |
| Operational visibility | Phase 12G audit logs provide complete history | Ops |

---

## Compliance Matrix (Fowler Standards)

| Principle | Phase 12E | Phase 12F | Phase 12G | Phase 14 | Final |
|-----------|-----------|-----------|-----------|----------|-------|
| **Evals** | 95% | 95% | 95% | ✅ 100% | ✅ |
| **Bounded Contexts** | 40% | ✅ 95% | ✅ 95% | ✅ 95% | ✅ |
| **Lethal Trifecta** | 85% | 85% | ✅ 95% | ✅ 95% | ✅ |
| **Human Review** | ✅ 90% | ✅ 90% | ✅ 90% | ✅ 95% | ✅ |
| **Tolerances** | ✅ 95% | ✅ 95% | ✅ 95% | ✅ 98% | ✅ |

**Final Compliance:** >95% across all 5 Fowler principles by end of Q2 2026.

---

## AIWorks Integration Notes

ThoughtWorks' AIWorks framework (escaping-legacy-black-hole whitepaper) emphasizes:
1. **Legacy Assessment** — Understand what exists before modernizing
2. **Incremental Refactoring** — Small steps, continuous validation
3. **AI as Catalyst** — Use AI to accelerate legacy → modern transition

**HTI5250J Application:**
- ✅ Legacy Assessment: Phase 11-13 delivered working system
- ✅ Incremental: Phase 12E-G are thin vertical slices
- 🟡 AI as Catalyst: Phase 14 uses formal testing to guide improvements

**Action:** Treat Phase 12E-14 as AIWorks-compliant modernization path.

---

## Next Steps

**Immediate (Today):**
1. ✅ Read FOWLER_RESEARCH_SUMMARY.md (20 min)
2. Review this action plan (30 min)
3. Schedule Phase 12E kickoff (sync meeting)

**Phase 12E Kickoff (Week 1):**
1. Create task_plan.md for Phase 12E
2. Assign components: Tolerance (dev A), Scorer (dev B), Simulator (dev C)
3. Create daily standup schedule
4. Target: Ship WorkflowTolerance + EvalScorer by end of week 1

**Phase 14 Kickoff (Week 2, Parallel):**
1. Create minimal D3-SCHEMA test (read APIs first!)
2. Verify compilation
3. Expand systematically

---

## References

- FOWLER_RESEARCH_SUMMARY.md — 5-minute executive overview
- FOWLER_AI_PATTERNS.md — Full implementation recipes (30KB)
- FOWLER_INTEGRATION_CHECKLIST.md — Per-pattern compliance questions
- FOWLER_ARCHITECTURE_REFERENCE.md — System architecture with patterns

All documents available in repository root.

---

**Status:** Ready for Phase 12E execution.
