# Bug Hunt Findings Catalog

## Summary
- Total: 18 findings | Actual Bugs: 6 | False Positives: 12
- CRITICAL (Real): 2 | HIGH (Real): 0 | MEDIUM (Real): 3 | LOW (Real): 1
- CRITICAL (False): 1 | HIGH (False): 4 | MEDIUM (False): 3 | LOW (False): 2

## Cycle 1: Discovery Findings

| ID | Severity | Domain | Location | Issue | Status |
|----|----------|--------|----------|-------|--------|
| 1 | CRITICAL | **VERIFIED BUG** | BatchMetrics.java:61-62 | Off-by-one error in percentile calculation (p50, p99 use `(size * pct) / 100` instead of `(size - 1) * pct / 100`) | fix-in-cycle-3 |
| 2 | CRITICAL | **VERIFIED BUG** | DatasetLoader.java:49 | Null dereference: entry.getValue() can be null, passed to String.replace() without null check | fix-in-cycle-3 |
| 3 | CRITICAL | FALSE POSITIVE | WorkflowRunner.java:212 | FileWriter IS correctly using try-with-resources; no bug detected | closed |
| 4 | HIGH | FALSE POSITIVE | DatasetLoader.java (overall) | Thread-safe; each loadCSV() call creates its own parser instance; no shared state | closed |
| 5 | HIGH | FALSE POSITIVE | WorkflowSimulator.java:103 | Null check IS present; fieldValue.length() protected by `if (fieldValue != null)` | closed |
| 6 | HIGH | FALSE POSITIVE | WorkflowCLI.java:70-114 | Exception context IS preserved; TerminalAdapter.printError() calls e.printStackTrace() | closed |
| 7 | HIGH | FALSE POSITIVE | WorkflowCLI.java:50 | Logic is correct; CSVFormat.withFirstRecordAsHeader() removes header from result; batch detection valid | closed |
| 8 | MEDIUM | **VERIFIED BUG** | StepOrderValidator.java:42 | SUBMIT as first step (i=0) skipped by `i > 0` condition; only WARNING, should be ERROR | fix-in-cycle-3 |
| 9 | MEDIUM | **VERIFIED BUG** | WorkflowSimulator.java:158-161 | predictedFields HashMap lacks validation for null entries; no contract enforcement | fix-in-cycle-3 |
| 10 | MEDIUM | FALSE POSITIVE | DatasetLoader.java:20-35 | CSVParser properly closed by try-with-resources; implements AutoCloseable | closed |
| 11 | MEDIUM | FALSE POSITIVE | CorrectnessScorer.java:52-54 | Null check on line 52 assigns empty string; all .contains() calls safe (never null) | closed |
| 12 | MEDIUM | **VERIFIED BUG** | BatchMetrics.java (no test file) | No test coverage; no BatchMetricsTest.java file exists; Bug #1 unmasks by tests | fix-in-cycle-3 |
| 13 | LOW | FALSE POSITIVE | WorkflowValidator.java:91-99 | Switch statement IS exhaustive; all 7 ActionType cases covered; compiler enforces | closed |
| 14 | LOW | FALSE POSITIVE | WorkflowSimulation.java:54-62 | Already uses StringBuilder; not string concatenation in loop; code is optimized | closed |
| 15 | LOW | DOCUMENTATION | WorkflowRunner.java | Parameter nullability documentation missing; code correct; enhancement only | docs-in-cycle-3 |
| 16 | LOW | **VERIFIED BUG** | NavigationException.java:30-33 | No truncation protection for huge screen dumps (10KB+ could cause memory overhead) | fix-in-cycle-3 |
| 17 | LOW | FALSE POSITIVE | WorkflowCLI.java (output) | System.out is line-buffered by default; single-threaded CLI has no concurrency issues | closed |
| 18 | LOW | FALSE POSITIVE | ParameterValidator.java:21 | PARAM_PATTERN is `static final`; compiled once at class load; not in loop; efficient | closed |

## Patterns Observed

### High-Risk Patterns
1. **Percentile Calculation Bug**: Off-by-one in array indexing (line 61-62 BatchMetrics.java)
2. **Null Handling**: Multiple instances of null dereference without checks (DatasetLoader, WorkflowSimulator)
3. **Resource Leaks**: Try-with-resources coverage incomplete (FileWriter not verified)
4. **Concurrency Safety**: Shared state in stateless utilities (CSV parser)

### Code Smell Patterns
1. **Magic Numbers**: Hardcoded step durations (500ms, 2000ms) in WorkflowSimulator
2. **String Matching**: Using string equality ("LOGIN", "FILL") instead of enum switch
3. **Error Swallowing**: Generic catch blocks without exception context

## Root Causes (Cycle 2 Analysis)
(To be filled after agent deep-dive)
