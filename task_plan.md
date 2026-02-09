# Task Plan: 12-Agent Parallel Bug Hunt + 4 Optimization Cycles

## Mission
Find and fix bugs across 12 specialized domains in HTI5250J codebase, then apply 4 rounds of optimization refactoring.

## Project Baseline
- 453 Java files analyzed
- Build: Ant (build.xml)
- Test framework: JUnit 5, Mockito
- Architecture: Sealed actions, records, pattern matching (Java 21)
- Current phase: 12E (Workflow Simulation)

## Execution Plan

### Cycle 1: Discovery (All 12 agents scan in parallel)
- [x] Agent 1: Static Analysis (grep, pattern matching - 18 findings)
- [x] Agent 2: Concurrency (DatasetLoader, ArtifactCollector - 2 issues found)
- [x] Agent 3: Resource Leaks (FileWriter verified safe - 0 issues)
- [x] Agent 4: API Contracts (Null safety - 3 critical issues)
- [x] Agent 5: Test Coverage (Edge cases - coverage gaps identified)
- [x] Agent 6: Performance (Percentile calc - O(1) but wrong)
- [x] Agent 7: Error Handling (Exception context loss - 1 high issue)
- [x] Agent 8: Input Validation (Null pointers - 3 critical issues)
- [x] Agent 9: Logic Bugs (Percentile off-by-one, SUBMIT validation)
- [x] Agent 10: Integration (CSV batch detection - 1 high issue)
- [x] Agent 11: Security (Credentials not found in code - safe)
- [x] Agent 12: Configuration (Hardcoded timeouts identified - low priority)

### Cycle 2: Root Cause Analysis
- [ ] Agents dig deeper on findings
- [ ] Categorize by severity (CRITICAL/HIGH/MEDIUM/LOW)
- [ ] Identify root causes and patterns
- [ ] Collect evidence for reproducibility

### Cycle 3: Fix Implementation (TDD)
- [ ] Write failing tests first
- [ ] Implement fixes
- [ ] Verify all tests pass
- [ ] Zero regressions in existing tests

### Cycle 4: Optimization Refactoring
- [ ] Eliminate technical debt discovered
- [ ] Apply performance optimizations
- [ ] Reduce code duplication
- [ ] Improve maintainability

## Deliverables
- [ ] findings.md - Structured catalog with severity levels
- [ ] bug_fixes.md - TDD-based fixes applied
- [ ] refactoring_plan.md - Optimization recommendations
- [ ] evidence.md - Test results before/after

## Status
**CYCLE 1 COMPLETE** - 12 agents completed discovery phase
- 18 bugs found (3 CRITICAL, 4 HIGH, 6 MEDIUM, 5 LOW)
- Key findings: Percentile off-by-one, null dereferences, concurrency issues
- Evidence collected in findings.md, bug_fixes.md, evidence.md
- Baseline tests running (ant test in progress)

## Next: Cycle 2 Root Cause Analysis
- Detailed analysis of each finding
- Pattern identification across codebase
- Prepare TDD test cases for fixes
