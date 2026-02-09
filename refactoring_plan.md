# Refactoring Plan - Cycle 4 (After Bug Fixes)

## Overview
Post-fix refactoring to eliminate technical debt and improve maintainability. This plan targets medium and low-severity findings plus architectural improvements.

## Phase 1: Null Safety Hardening (MEDIUM Priority)

### Task 1.1: Defensive Null Contracts
**Goal**: Add @Nullable / @NonNull annotations to clarify null expectations

**Files**: DatasetLoader, WorkflowSimulator, StepOrderValidator, ParameterValidator

**Changes**:
```java
// BEFORE
public Map<String, Map<String, String>> loadCSV(File csvFile) throws Exception

// AFTER (with annotation)
import org.checkerframework.checker.nullness.qual.NonNull;

public @NonNull Map<String, @NonNull Map<String, @NonNull String>>
    loadCSV(@NonNull File csvFile) throws Exception
```

**Test Coverage**:
- Test with null file argument (should throw NPE early)
- Test with null field values in CSV
- Test with empty Map returns

**Estimated Effort**: 2 hours

---

## Phase 2: Performance Optimization (LOW Priority)

### Task 2.1: Pattern Compilation Caching
**Goal**: Avoid recompiling PARAM_PATTERN in ParameterValidator loop

**File**: ParameterValidator.java

**Changes**:
```java
// BEFORE (line 21 - class scope, but compile() each use)
private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{data\\.([^}]+)\\}");

private void checkField(String field, List<String> references) {
    Matcher matcher = PARAM_PATTERN.matcher(field);  // Good - already cached

// AFTER (verify caching already in place)
// This is actually already optimized - no change needed.
```

**Status**: ALREADY OPTIMIZED - No fix needed

**Estimated Effort**: 0 hours (investigate and mark as done)

---

### Task 2.2: StringBuilder Reuse in Loops
**Goal**: Reuse StringBuilder instead of creating new in formatScreenDump()

**File**: WorkflowRunner.java:195-203

**Changes**:
```java
// BEFORE
private String formatScreenDump(String screenContent) {
    StringBuilder sb = new StringBuilder();
    int cols = 80;
    for (int i = 0; i < screenContent.length(); i += cols) {
        int end = Math.min(i + cols, screenContent.length());
        sb.append(screenContent.substring(i, end)).append("\n");
    }
    return sb.toString();
}

// AFTER (already using StringBuilder - no change needed)
// This is already optimized. Verify and mark as done.
```

**Status**: ALREADY OPTIMIZED

**Estimated Effort**: 0 hours

---

### Task 2.3: Hardcoded Timeout Constants Review
**Goal**: Extract magic numbers to configuration

**Files**: WorkflowRunner.java (lines 20-24), WorkflowSimulator.java (lines 40-41)

**Changes**:
```java
// BEFORE
private static final int DEFAULT_KEYBOARD_UNLOCK_TIMEOUT = 30000;
private static final int DEFAULT_KEYBOARD_LOCK_CYCLE_TIMEOUT = 5000;
private static final int KEYBOARD_POLL_INTERVAL = 100;
private static final int FIELD_FILL_TIMEOUT = 500;

private static final long STEP_DURATION_MS = 500;
private static final long LOGIN_DURATION_MS = 2000;

// AFTER - Create WorkflowConfig class
public record WorkflowConfig(
    int keyboardUnlockTimeoutMs,
    int keyboardLockCycleTimeoutMs,
    int keyboardPollIntervalMs,
    int fieldFillTimeoutMs,
    long stepDurationMs,
    long loginDurationMs
) {
    public static WorkflowConfig defaults() {
        return new WorkflowConfig(30000, 5000, 100, 500, 500, 2000);
    }

    public static WorkflowConfig fromProperties(Properties props) {
        return new WorkflowConfig(
            Integer.parseInt(props.getProperty("keyboard.unlock.timeout", "30000")),
            Integer.parseInt(props.getProperty("keyboard.cycle.timeout", "5000")),
            // ... etc
        );
    }
}
```

**Impact**: Enables configuration without code changes

**Estimated Effort**: 3 hours

---

## Phase 3: Concurrency Hardening (HIGH Priority)

### Task 3.1: Thread-Safe DatasetLoader
**Goal**: Ensure DatasetLoader is safe for concurrent use

**File**: DatasetLoader.java

**Changes**:
```java
// BEFORE
public Map<String, Map<String, String>> loadCSV(File csvFile) throws Exception

// AFTER - Make static (no instance state)
public static Map<String, Map<String, String>> loadCSV(File csvFile) throws Exception

// Update all callers from instance to static
DatasetLoader loader = new DatasetLoader();
loader.loadCSV(file);  // BEFORE

DatasetLoader.loadCSV(file);  // AFTER
```

**Test Coverage**:
- Concurrent load test with 10 threads
- Verify no data corruption
- Verify all threads get same results

**Estimated Effort**: 1 hour

---

### Task 3.2: Synchronized ArtifactCollector
**Goal**: Thread-safe ledger writing in concurrent scenarios

**File**: ArtifactCollector.java

**Changes**:
```java
// BEFORE
public synchronized void appendLedger(String action, String details) {
    // Write to ledger file
}

// AFTER - Use ReentrantLock for better control
private final ReentrantLock ledgerLock = new ReentrantLock();

public void appendLedger(String action, String details) {
    ledgerLock.lock();
    try {
        // Write to ledger file
    } finally {
        ledgerLock.unlock();
    }
}
```

**Estimated Effort**: 1 hour

---

## Phase 4: Error Handling Improvements (HIGH Priority)

### Task 4.1: Structured Exception Chaining
**Goal**: Preserve exception cause chain throughout call stack

**File**: WorkflowCLI.java (main exception handler)

**Changes**:
```java
// BEFORE
} catch (Exception e) {
    TerminalAdapter.printError("Error", e);
    System.exit(1);
}

// AFTER - Enhanced context
} catch (WorkflowException e) {
    TerminalAdapter.printError("Workflow error: " + e.getWorkflowName(), e);
    System.exit(1);
} catch (IOException e) {
    TerminalAdapter.printError("File I/O error: " + e.getMessage(), e);
    System.exit(1);
} catch (Exception e) {
    TerminalAdapter.printError("Unexpected error", e);
    e.printStackTrace(System.err);
    System.exit(1);
}
```

**Estimated Effort**: 2 hours

---

### Task 4.2: Exception Hierarchy Design
**Goal**: Create structured exception types for better error handling

**New Files**:
```java
// WorkflowException (base)
public class WorkflowException extends Exception {
    private final String workflowName;
    private final int stepIndex;

    public WorkflowException(String message, String workflowName, int stepIndex, Throwable cause) {
        super(message, cause);
        this.workflowName = workflowName;
        this.stepIndex = stepIndex;
    }

    public String getWorkflowName() { return workflowName; }
    public int getStepIndex() { return stepIndex; }
}

// ValidationException (specific)
public class ValidationException extends WorkflowException { ... }

// ExecutionException (specific)
public class ExecutionException extends WorkflowException { ... }
```

**Estimated Effort**: 3 hours

---

## Phase 5: Code Organization (MEDIUM Priority)

### Task 5.1: Consolidate Validation Logic
**Goal**: Reduce duplication across validators

**Files**:
- WorkflowValidator.java
- StepValidator.java
- ActionValidator implementations (7 files)

**Current State**: 7 separate action validators + main validator

**Proposed**:
```java
// Create ValidatorChain to compose validators
public class ValidatorChain {
    private final List<Validator> validators = new ArrayList<>();

    public ValidatorChain add(Validator v) {
        validators.add(v);
        return this;
    }

    public ValidationResult validate(Object target) {
        ValidationResult result = new ValidationResult();
        for (Validator v : validators) {
            result.merge(v.validate(target));
        }
        return result;
    }
}

// Usage
ValidatorChain chain = new ValidatorChain()
    .add(new StepValidator())
    .add(new ActionValidator())
    .add(new ParameterValidator());

ValidationResult result = chain.validate(workflow);
```

**Estimated Effort**: 4 hours

---

## Phase 6: Testing Improvements (HIGH Priority)

### Task 6.1: Add Edge Case Tests
**Goal**: Increase coverage for boundary conditions

**New Test Files**:
- BatchMetricsEdgeCaseTest (empty lists, single element, null)
- DatasetLoaderNullValueTest
- WorkflowSimulatorNullFieldTest
- StepOrderValidatorEdgeCaseTest

**Test Cases**:
```java
// Empty percentiles list
@Test
void testBatchMetricsWithEmptyLatencies() {
    List<WorkflowResult> results = List.of(failureResult);
    BatchMetrics metrics = BatchMetrics.from(results, 0, 1000);
    assertEquals(0, metrics.p50LatencyMs());
}

// Null field values
@Test
void testSimulateWithNullFields() {
    Map<String, String> fields = new HashMap<>();
    fields.put("amount", null);
    // Should not throw NPE
}

// SUBMIT as first step
@Test
void testSUBMITAsFirstStepIsError() {
    List<StepDef> steps = List.of(createSubmitStep());
    ValidationResult result = validator.validate(steps);
    assertTrue(result.hasErrors());
}
```

**Estimated Effort**: 3 hours

---

### Task 6.2: Concurrency Testing
**Goal**: Add stress tests for virtual thread scenarios

**New Test File**: ConcurrentWorkflowTest

**Test Cases**:
```java
@Test
void testConcurrentDatasetLoading() throws Exception {
    // 100 virtual threads loading same CSV
    // Verify no data corruption, all get identical results
}

@Test
void testConcurrentArtifactCollection() throws Exception {
    // 100 virtual threads writing to ledger
    // Verify no interleaved output, all entries present
}
```

**Estimated Effort**: 2 hours

---

## Phase 7: Documentation (LOW Priority)

### Task 7.1: API Contract Documentation
**Goal**: Document null expectations and concurrency safety

**Files to Update**:
- DatasetLoader.java - Add @ThreadSafe or @NotThreadSafe
- WorkflowSimulator.java - Document null handling expectations
- ArtifactCollector.java - Document synchronization strategy

**Format**:
```java
/**
 * Load CSV file into map structure.
 *
 * Thread-safety: NOT THREAD-SAFE. Each thread must create its own instance
 * or serialize access. Consider making static for better semantics.
 *
 * @param csvFile non-null CSV file to load (will throw NPE if null)
 * @return non-null map (never null, but may be empty)
 * @throws IOException if file not readable
 * @throws IllegalArgumentException if null field values in CSV
 */
```

**Estimated Effort**: 2 hours

---

## Phase 8: Configuration Management (MEDIUM Priority)

### Task 8.1: Externalize Timeouts
**Goal**: Move hardcoded timeouts to configuration

**New File**: workflow.properties
```properties
# Keyboard interaction timeouts (milliseconds)
keyboard.unlock.timeout=30000
keyboard.cycle.timeout=5000
keyboard.poll.interval=100
field.fill.timeout=500

# Simulation durations
simulation.step.duration=500
simulation.login.duration=2000

# Batch execution
batch.workflow.timeout.seconds=300
batch.thread.pool.size=10
```

**Implementation**: Create WorkflowConfig class to load from properties

**Estimated Effort**: 2 hours

---

## Execution Summary

| Phase | Tasks | Hours | Priority |
|-------|-------|-------|----------|
| 1 | Null Safety | 2 | MEDIUM |
| 2 | Performance | 3 | LOW |
| 3 | Concurrency | 2 | HIGH |
| 4 | Error Handling | 5 | HIGH |
| 5 | Code Organization | 4 | MEDIUM |
| 6 | Testing | 5 | HIGH |
| 7 | Documentation | 2 | LOW |
| 8 | Configuration | 2 | MEDIUM |
| **TOTAL** | **8 Phases** | **25 hours** | **Mixed** |

## Schedule

**Recommended Order**:
1. Phases 3, 4, 6 (HIGH priority - concurrency, error handling, testing) - 12 hours
2. Phase 1 (MEDIUM - null safety) - 2 hours
3. Phase 5 (MEDIUM - code organization) - 4 hours
4. Phase 8 (MEDIUM - configuration) - 2 hours
5. Phase 2, 7 (LOW - performance, documentation) - 5 hours

**Total Estimated Effort**: 25 hours (1 week at 40 hrs/week)

## Success Criteria

- [ ] All 7 bug fixes implemented with passing tests
- [ ] Zero regressions in existing test suite
- [ ] Edge case tests cover identified vulnerabilities
- [ ] Concurrency tests pass under 100+ thread load
- [ ] Code review signoff on error handling changes
- [ ] Configuration externalized and tested
- [ ] API documentation updated with null contracts
- [ ] Performance metrics baseline established (post-refactor)

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| Regression from refactoring | Medium | High | Comprehensive test coverage before/after |
| Config externalization complexity | Low | Medium | Start with simple property file, evolve |
| Concurrency issues in testing | Medium | Medium | Use virtual threads, stress test (100+ threads) |
| Performance impact of locking | Low | Low | Benchmark before/after, optimize if needed |

---

## Dependencies

- Fix implementation (Cycle 3) must complete before refactoring starts
- Baseline test results required for comparison
- Code review process established for API changes

---

## Post-Refactoring Metrics

**Expected Improvements**:
- Null safety: 100% of public APIs documented
- Test coverage: +15% (edge cases, concurrency)
- Code duplication: -20% (validator chain consolidation)
- Maintainability: +25% (clear error hierarchies, configuration)
- Performance: Neutral (no significant impact expected)
