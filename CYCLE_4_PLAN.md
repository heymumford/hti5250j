# Cycle 4: Optimization and Systemic Refactoring

**Objective:** Address 8 systemic patterns discovered in bug hunt (Cycles 1-3) to improve maintainability, safety, and quality.

## Timeline: 19 hours

### Block 1: Null Safety (Hours 1-3)
**Pattern:** Ad-hoc null handling without consistent annotations

**Tasks:**
1. Add JSR-305 @Nullable/@Nonnull annotations to public methods
2. Create NullSafetyValidator.java helper class
3. Apply to: DatasetLoader, WorkflowSimulator, ValidationResult, Action classes
4. Test: NullSafetyValidatorTest.java

**Files affected:**
- DatasetLoader.java
- WorkflowSimulator.java
- ValidationResult.java
- Action.java (sealed interface)
- LoginAction.java, NavigateAction.java, etc.

---

### Block 2: Test Coverage Gaps (Hours 4-7)
**Pattern:** 25 public classes in workflow module lack test files

**Missing test files (priority order):**
1. TerminalAdapter (UI adapter - critical)
2. ArgumentParser (argument handling - critical)
3. WorkflowLoader (YAML loading - critical)
4. SessionFactory (session creation - high)
5. StepDef (step definition - high)
6. WorkflowResult (result DTO - medium)
7. Action.java, LoginAction, NavigateAction, etc. (records - medium)
8. Scorers: CorrectnessScorer, IdempotencyScorer, LatencyScorer (validation)

**Target:** 100% of public classes with test files

---

### Block 3: Validation Completeness (Hours 8-10)
**Pattern:** Validators skip edge cases

**Review validators:**
1. StepValidator - timeout bounds, null checks
2. ActionValidator interface and 7 implementations
3. ParameterValidator - ${data.x} references
4. StepOrderValidator - circular dependencies

**Enhancements:**
- Add missing edge case tests
- Verify all constraints in documentation
- Add negative tests for each constraint

**Files:**
- All validators in workflow/ and workflow/validators/
- *Test.java files for each

---

### Block 4: Error Handling Context (Hours 11-12)
**Pattern:** Generic catch-all exceptions lose cause chains

**Tasks:**
1. Review all catch blocks in WorkflowCLI, WorkflowLoader, DatasetLoader
2. Ensure IOException → WorkflowException(cause) patterns
3. Add meaningful context to messages
4. Add cause chain tests

**Files:**
- WorkflowCLI.java
- WorkflowLoader.java
- DatasetLoader.java
- Exception test files

---

### Block 5: Concurrency Safety (Hours 13-14)
**Pattern:** Single-threaded design in virtual thread context

**Review for virtual thread safety:**
1. WorkflowRunner (handler methods)
2. SessionInterface (screen state)
3. DataStreamProducer (queue operations)
4. ScreenField, Screen5250 (cursor state)

**Document:**
- Identify thread-local state
- Add @ThreadSafe / @NotThreadSafe annotations
- Verify no shared mutable state in virtual threads

**Files:**
- All files used by virtual threads
- Create THREADING_MODEL.md

---

### Block 6: Code Organization (Hours 15-16)
**Pattern:** Classes approaching/exceeding 300-line limit

**Identify candidates:**
1. Find all files > 300 lines
2. Consider extraction of helper classes
3. Apply SRP (single responsibility principle)
4. Verify cohesion

**Metrics:**
- Max class size
- Average class size
- Cohesion score

---

### Block 7: Documentation Sync (Hours 17-18)
**Pattern:** Comments become outdated vs implementation

**Tasks:**
1. Review all Javadoc for accuracy
2. Update example code in comments
3. Verify @param/@return documentation
4. Add missing Javadoc to public methods

**Files:**
- All public classes
- Focus: workflow module

---

### Block 8: Configuration Constants (Hour 19)
**Pattern:** Magic numbers scattered throughout code

**Create Constants.java:**
1. Keyboard timeouts (30000ms, 5000ms, etc.)
2. Limits (255-char field limit, 80-column screen width)
3. Thresholds (timeout bounds: 100-300000ms)
4. Buffer sizes

**Replace all hardcoded values**

---

## Deliverables

1. **optimization_results.md**
   - Baseline vs. after metrics
   - Lines of code per module
   - Class size distribution
   - Test coverage improvement %

2. **refactoring_summary.md**
   - Details of each 8 themes
   - Files modified/created
   - Benefits demonstrated

3. **final_verification.md**
   - Test suite results (expected ~13,000 tests)
   - Build metrics
   - Zero regression confirmation

4. **recommendations.md**
   - Ongoing quality practices
   - Annual audit schedule
   - Prevention strategies

---

## Execution

- Work in parallel on independent blocks (1, 2, 4, 5, 6, 7, 8)
- Sequential for shared work (block 3)
- Test-first for new test files
- Commit incrementally (one per theme)
- Zero regressions required

---

## Quality Gates

✓ All refactored code must compile
✓ Full test suite must pass (0 failures)
✓ Cyclomatic complexity must not increase
✓ Test coverage must increase or maintain
✓ No public API breaks (backward compatible)

---

## Status

**START TIME:** 2026-02-09 02:00 UTC
**CURRENT PHASE:** Planning
**NEXT:** Block 1 - Null Safety
