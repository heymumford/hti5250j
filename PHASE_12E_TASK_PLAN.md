# Phase 12E: Metrics & Tolerances - Task Plan

**Start Date:** 2026-02-09
**Target Completion:** 2026-02-16 (1 week)
**Effort:** 16 hours, 2 developers
**Principle:** Martin Fowler - "Define acceptable bounds for non-determinism"

---

## Goal

Answer "Is execution reliable? How do we know?" by adding measurable quality metrics to Phase 13's batch execution. Enable human approval gates via dry-run simulation.

---

## Phases

- [x] **Phase 1: WorkflowTolerance Component** (2 hours) ✅ COMPLETE
  - Create record definition
  - YAML deserialization support
  - Field validation
  - Unit tests (6 tests)

- [ ] **Phase 2: EvalScorer Framework** (4 hours)
  - Interface definition
  - CorrectnessScorer implementation
  - IdempotencyScorer implementation
  - LatencyScorer implementation
  - Unit tests (12 tests)

- [ ] **Phase 3: WorkflowSimulator (Dry-Run)** (4 hours)
  - Simulator engine (execute on MockScreen)
  - WorkflowSimulation record
  - Warning generation
  - Integration tests (8 tests)

- [ ] **Phase 4: Integration & CLI** (2 hours)
  - WorkflowValidator enhancement
  - WorkflowCLI `simulate` command
  - BatchMetrics eval score fields
  - Documentation

- [ ] **Phase 5: Verification** (2 hours)
  - Run full test suite
  - Verify no regressions
  - Performance baseline
  - Documentation review

- [ ] **Phase 6: Documentation & Commit** (2 hours)
  - README update
  - User guide for tolerances
  - Commit with detailed message

---

## Detailed Tasks

### PHASE 1: WorkflowTolerance Component

#### Task 1.1: Create WorkflowTolerance Record
**Owner:** Dev A
**Effort:** 30 min
**Status:** ✅ COMPLETE (2026-02-09)

```java
public record WorkflowTolerance(
    String workflowName,
    long maxDurationMs,
    double fieldPrecision,
    int maxRetries,
    boolean requiresApproval
) {
    public WorkflowTolerance {
        // Validation: maxDurationMs > 0, etc.
    }
}
```

**Files:**
- NEW: src/org/hti5250j/workflow/WorkflowTolerance.java (30 LOC)

**Acceptance Criteria:**
- ✓ Record compiles
- ✓ Fields are immutable
- ✓ toString() works
- ✓ equals/hashCode generated

#### Task 1.2: YAML Deserialization Support
**Owner:** Dev A
**Effort:** 30 min
**Status:** ✅ COMPLETE (2026-02-09)

Extend StepDef's YAML loader to handle tolerances:

```yaml
name: payment_processing
tolerances:
  maxDurationMs: 300000
  fieldPrecision: 0.01
  maxRetries: 3
  requiresApproval: true
steps: [...]
```

**Files:**
- MODIFY: src/org/hti5250j/workflow/WorkflowSchema.java (+15 LOC)
- NEW: tests/org/hti5250j/workflow/WorkflowToleranceTest.java (85 LOC, 6 tests)

**Tests:**
- T1.2.1: YAML loads tolerances correctly
- T1.2.2: Missing tolerances use defaults
- T1.2.3: Invalid tolerance values rejected
- T1.2.4: Boundary values validated
- T1.2.5: Zero duration rejected
- T1.2.6: Negative precision rejected

**Acceptance Criteria:**
- ✓ YAML parses without errors
- ✓ Defaults applied when not specified
- ✓ Invalid values throw with message
- ✓ 6 tests pass

---

### PHASE 2: EvalScorer Framework

#### Task 2.1: Define EvalScorer Interface
**Owner:** Dev B
**Effort:** 30 min
**Status:** 📋 PENDING

```java
public interface EvalScorer {
    /**
     * Evaluate workflow result against tolerance.
     * @return 0.0 (failed) to 1.0 (perfect)
     */
    double evaluate(WorkflowResult result, WorkflowTolerance tolerance);

    String scorerName();
}
```

**Files:**
- NEW: src/org/hti5250j/workflow/EvalScorer.java (15 LOC)

#### Task 2.2: Implement CorrectnessScorer
**Owner:** Dev B
**Effort:** 1 hour
**Status:** 📋 PENDING

Verify: Did workflow produce correct outputs?

```java
public class CorrectnessScorer implements EvalScorer {
    @Override
    public double evaluate(WorkflowResult result, WorkflowTolerance tolerance) {
        if (!result.success()) return 0.0;  // Failed = 0.0

        // Check field values match expected (to precision)
        // Check no silent truncation occurred
        // Return confidence 0.0-1.0
    }
}
```

**Files:**
- NEW: src/org/hti5250j/workflow/CorrectnessScorer.java (45 LOC)

**Tests (4):**
- T2.2.1: Failed workflow scores 0.0
- T2.2.2: Successful workflow with correct fields scores 1.0
- T2.2.3: Truncated field reduces score
- T2.2.4: Precision boundary respected

#### Task 2.3: Implement IdempotencyScorer
**Owner:** Dev B
**Effort:** 1 hour
**Status:** 📋 PENDING

Verify: Can we run it twice and get the same result?

```java
public class IdempotencyScorer implements EvalScorer {
    @Override
    public double evaluate(WorkflowResult result, WorkflowTolerance tolerance) {
        // Check: if retried, results match
        // Check: no non-deterministic output
        // Return 0.0-1.0
    }
}
```

**Files:**
- NEW: src/org/hti5250j/workflow/IdempotencyScorer.java (40 LOC)

**Tests (4):**
- T2.3.1: No retries = perfect idempotency (1.0)
- T2.3.2: Retry matches original = 1.0
- T2.3.3: Retry differs = 0.0
- T2.3.4: Partial retry diff = 0.5

#### Task 2.4: Implement LatencyScorer
**Owner:** Dev B
**Effort:** 1 hour
**Status:** 📋 PENDING

Verify: Did it complete within tolerance?

```java
public class LatencyScorer implements EvalScorer {
    @Override
    public double evaluate(WorkflowResult result, WorkflowTolerance tolerance) {
        long actual = result.latencyMs();
        long max = tolerance.maxDurationMs();

        if (actual > max) return 0.0;              // Failed
        if (actual < max * 0.8) return 1.0;        // Well under
        return 1.0 - ((max - actual) / max);        // Linear penalty
    }
}
```

**Files:**
- NEW: src/org/hti5250j/workflow/LatencyScorer.java (35 LOC)

**Tests (4):**
- T2.4.1: Under tolerance = 1.0
- T2.4.2: At tolerance = 0.0
- T2.4.3: Over tolerance = 0.0
- T2.4.4: Linear penalty applies correctly

#### Task 2.5: EvalScorer Tests
**Owner:** Dev B
**Effort:** 30 min
**Status:** 📋 PENDING

Create comprehensive test suite:

**Files:**
- NEW: tests/org/hti5250j/workflow/EvalScorerTest.java (140 LOC, 12 tests)

**Acceptance Criteria:**
- ✓ All 12 tests pass
- ✓ Edge cases handled (NaN, Infinity, boundary)
- ✓ Tolerance violations detected

---

### PHASE 3: WorkflowSimulator (Dry-Run)

#### Task 3.1: Create WorkflowSimulation Record
**Owner:** Dev C
**Effort:** 30 min
**Status:** 📋 PENDING

```java
public record WorkflowSimulation(
    List<SimulatedStep> steps,
    String predictedOutcome,      // "success" | "timeout" | "validation_error"
    Map<String, String> predictedFields,
    List<String> warnings
) {}

public record SimulatedStep(
    int stepIndex,
    String stepName,
    String prediction,            // "success" | "timeout" | "error"
    Optional<String> warning
) {}
```

**Files:**
- NEW: src/org/hti5250j/workflow/WorkflowSimulation.java (25 LOC)

#### Task 3.2: Implement WorkflowSimulator
**Owner:** Dev C
**Effort:** 2 hours
**Status:** 📋 PENDING

Execute workflow against MockScreen instead of real Screen5250:

```java
public class WorkflowSimulator {
    public WorkflowSimulation simulate(
        WorkflowSchema workflow,
        Map<String, String> testData,
        WorkflowTolerance tolerance
    ) {
        // Create MockScreen (no i5 connection)
        // Execute each step, predict outcome
        // Generate warnings if violations predicted
        // Return simulation results
    }
}
```

**Files:**
- NEW: src/org/hti5250j/workflow/WorkflowSimulator.java (95 LOC)

**Features:**
- Happy path execution (all steps succeed)
- Timeout prediction (cumulative duration > tolerance)
- Validation error prediction (field boundary violations)
- Warning generation for truncation, precision loss
- No network access, no i5 connection

#### Task 3.3: Integration Tests
**Owner:** Dev C
**Effort:** 1 hour
**Status:** 📋 PENDING

Create integration test suite:

**Files:**
- NEW: tests/org/hti5250j/workflow/WorkflowSimulatorTest.java (165 LOC, 8 tests)

**Tests:**
- T3.3.1: Happy path simulation succeeds
- T3.3.2: Predicted timeout when duration exceeds tolerance
- T3.3.3: Predicted validation error for invalid field
- T3.3.4: Warning generated for field truncation
- T3.3.5: Warning generated for precision loss
- T3.3.6: Multiple warnings accumulated
- T3.3.7: Simulation completes without i5 connection
- T3.3.8: Predicted fields match expected schema

**Acceptance Criteria:**
- ✓ All 8 tests pass
- ✓ Simulator runs offline
- ✓ Warnings prevent silent data loss
- ✓ Timeout prediction accurate

---

### PHASE 4: Integration & CLI

#### Task 4.1: Enhance WorkflowValidator
**Owner:** Dev A
**Effort:** 30 min
**Status:** 📋 PENDING

Add tolerance validation:

```java
public class WorkflowValidator {
    public ValidationResult validate(WorkflowSchema workflow) {
        // Existing validation

        if (workflow.tolerances() != null) {
            // Validate: steps don't exceed maxDurationMs in sum
            // Validate: field precision matches tolerance
            // Validate: retry count <= maxRetries
        }
    }
}
```

**Files:**
- MODIFY: src/org/hti5250j/workflow/WorkflowValidator.java (+40 LOC)

#### Task 4.2: Add CLI `simulate` Command
**Owner:** Dev A
**Effort:** 1 hour
**Status:** 📋 PENDING

Enable: `i5250 simulate <workflow.yaml> --data <data.csv>`

```java
public class WorkflowCLI {
    private void simulateAction(String workflowFile, String dataFile) {
        WorkflowSchema workflow = loadWorkflow(workflowFile);
        WorkflowTolerance tolerance = workflow.tolerances();
        Map<String, String> testData = loadCSV(dataFile);

        WorkflowSimulation sim = simulator.simulate(workflow, testData, tolerance);
        printSimulationReport(sim);
    }
}
```

**Files:**
- MODIFY: src/org/hti5250j/workflow/WorkflowCLI.java (+25 LOC)

**Output Format:**
```
Simulating: payment_processing (1 workflow)
  Step 1 (LOGIN): ✓ Predicts success
  Step 2 (NAVIGATE): ✓ Screen reached
  Step 3 (FILL): ⚠ Warning: 'amount' would be truncated from 10 to 7 chars
  Step 4 (SUBMIT): ✓ Predicts success
Result: Would complete in 850ms (tolerance: 5000ms) ✓
Approval Status: RECOMMEND → review truncation warning
```

#### Task 4.3: Enhance BatchMetrics
**Owner:** Dev A
**Effort:** 30 min
**Status:** 📋 PENDING

Add eval scores to Phase 13's BatchMetrics:

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
    double overallQuality,        // NEW: avg of above 3
    List<WorkflowResult> failures
) {
    public static BatchMetrics from(List<WorkflowResult> results, ...) {
        // Compute eval scores for all results
        // Return metrics with scores
    }
}
```

**Files:**
- MODIFY: src/org/hti5250j/workflow/BatchMetrics.java (+12 LOC)

**Output Example:**
```
Batch Execution Report
======================
Throughput: 587,231 ops/sec (1000 workflows)
Quality Metrics:
  - Correctness: 99.8% (1 field mismatch)
  - Idempotency: 100.0% (all retries matched)
  - Latency: 98.2% (2 workflows exceeded tolerance)
  - Overall Quality: 0.993 (99.3%)

Tolerance Compliance:
  - maxDurationMs: 4250ms avg (tolerance: 5000ms) ✓
  - fieldPrecision: 0.00 error (tolerance: 0.01) ✓
  - maxRetries: 0 used (tolerance: 3) ✓
```

---

### PHASE 5: Verification

#### Task 5.1: Full Test Suite
**Owner:** Dev Lead
**Effort:** 1 hour
**Status:** 📋 PENDING

Run comprehensive test suite:

```bash
./gradlew test --tests "WorkflowToleranceTest"
./gradlew test --tests "EvalScorerTest"
./gradlew test --tests "WorkflowSimulatorTest"
./gradlew test  # Full suite
```

**Acceptance Criteria:**
- ✓ 26 new tests pass (6 + 12 + 8)
- ✓ 0 regressions (Phase 13 tests still pass)
- ✓ Code coverage >90%

#### Task 5.2: Performance Baseline
**Owner:** Dev Lead
**Effort:** 30 min
**Status:** 📋 PENDING

Verify Phase 13 performance unchanged:

```bash
# Run 1000 concurrent workflows
./gradlew test --tests "BatchExecutorStressTest"
# Expected: >300K ops/sec, <150MB memory
```

**Acceptance Criteria:**
- ✓ >300K ops/sec maintained
- ✓ <150MB memory maintained
- ✓ No latency regression

#### Task 5.3: Manual Testing
**Owner:** QA/Dev Lead
**Effort:** 30 min
**Status:** 📋 PENDING

Manual verification:

```bash
# Create test workflow with tolerances
cat > examples/test_workflow.yaml << EOF
name: test
tolerances:
  maxDurationMs: 1000
  fieldPrecision: 0.01
  maxRetries: 2
  requiresApproval: true
steps: [...]
EOF

# Simulate
./i5250 simulate examples/test_workflow.yaml --data examples/test_data.csv
# Expect: Simulation output with warnings

# Run batch
./i5250 run examples/test_workflow.yaml --data examples/test_data.csv
# Expect: Metrics report with eval scores
```

**Acceptance Criteria:**
- ✓ `simulate` command runs without i5
- ✓ Warnings prevent data loss
- ✓ Metrics report all scores
- ✓ Overall Quality metric calculated

---

### PHASE 6: Documentation & Commit

#### Task 6.1: Update README
**Owner:** Dev Lead
**Effort:** 30 min
**Status:** 📋 PENDING

Document new features:

```markdown
## Tolerances & Metrics

Define acceptable bounds for workflow execution:

```yaml
name: payment_processing
tolerances:
  maxDurationMs: 300000        # 5 minutes
  fieldPrecision: 0.01         # Monetary precision
  maxRetries: 3                # Retry budget
  requiresApproval: true       # Human approval gate
```

Run simulation before deployment:

```bash
i5250 simulate examples/payment.yaml --data examples/test_data.csv
```

Batch execution reports quality metrics:

```
Quality Metrics:
  - Correctness: 99.8%
  - Idempotency: 100.0%
  - Latency: 98.2%
  - Overall: 0.993 (99.3%)
```
```

**Files:**
- MODIFY: README.md (+50 LOC)
- NEW: docs/TOLERANCES.md (user guide, ~150 LOC)

#### Task 6.2: Final Commit
**Owner:** Dev Lead
**Effort:** 30 min
**Status:** 📋 PENDING

Commit all Phase 12E work:

```bash
git add -A
git commit -m "feat(phase-12e): metrics and tolerances framework

Implements Martin Fowler principle: 'Define acceptable bounds for non-determinism'

New Components:
- WorkflowTolerance: YAML-serializable tolerance specification
- EvalScorer: Interface + 3 implementations (Correctness, Idempotency, Latency)
- WorkflowSimulator: Dry-run executor for human approval gates

Integration:
- Enhanced WorkflowValidator with tolerance validation
- Added 'simulate' CLI command
- Enhanced BatchMetrics with eval scores + overall quality metric

Testing:
- 26 new tests (WorkflowToleranceTest, EvalScorerTest, WorkflowSimulatorTest)
- All Phase 13 tests pass (zero regressions)
- Performance baseline maintained (>300K ops/sec, <150MB memory)

Documentation:
- README updated with tolerance usage
- TOLERANCES.md user guide
- PHASE_12E_TASK_PLAN.md planning document

Success Metrics:
- Quality reporting: Correctness 99.8%, Idempotency 100%, Latency 98.2%
- Human approval gates via simulation (no i5 required)
- Fowler compliance: 95% Evals, 90% Human Review, 95% Tolerances

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>"
```

---

## Status Tracking

| Phase | Task | Owner | Status | Effort | Start | End |
|-------|------|-------|--------|--------|-------|-----|
| 1 | WorkflowTolerance | Dev A | ✅ COMPLETE | 1h | 2026-02-09 | 2026-02-09 |
| 2 | EvalScorer Framework | Dev B | 📋 PENDING | 4h | | |
| 3 | WorkflowSimulator | Dev C | 📋 PENDING | 4h | | |
| 4 | Integration & CLI | Dev A | 📋 PENDING | 2h | | |
| 5 | Verification | Lead | 📋 PENDING | 2h | | |
| 6 | Documentation | Lead | 📋 PENDING | 2h | | |

---

## Risk Mitigation

| Risk | Mitigation | Owner |
|------|-----------|-------|
| Components aren't independent | Agree on interfaces first (EvalScorer) before implementation | Dev Lead |
| Test failures on integration | Start with unit tests, then integration | Dev B/C |
| Performance regression | Baseline Phase 13 early, re-verify after 12E | Dev Lead |
| Workflow complexity | Use existing payment/settlement examples | Dev C |

---

## Definition of Done

Phase 12E is complete when:

- ✅ All 26 tests pass
- ✅ Zero regressions (Phase 13 tests still pass)
- ✅ CLI `simulate` command works offline
- ✅ BatchMetrics reports eval scores
- ✅ README updated with tolerance usage
- ✅ Committed to main with detailed message
- ✅ Fowler compliance: 95% across all 5 principles

---

## Execution Efficiency Metrics (To Fill After Completion)

| Metric | Target | Actual |
|--------|--------|--------|
| Parallel services | 3 devs (A, B, C) | |
| Redundant calls | 0 | |
| Backtracking loops | 0 | |
| Optimal path efficiency | >90% | |

---

**Phase 12E Ready for Execution**
