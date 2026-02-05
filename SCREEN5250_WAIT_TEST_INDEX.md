# Screen5250WaitPairwiseTest - Test Index

## Quick Reference

**File:** `tests/org/tn5250j/framework/tn5250/Screen5250WaitPairwiseTest.java`
**Status:** ALL PASSING (23/23 tests)
**Execution Time:** 1.913 seconds
**Last Run:** 2025-02-04

## Test Manifest

### POSITIVE TESTS (10)

| # | Test Name | Scenario | Validates |
|---|-----------|----------|-----------|
| 1 | `testWaitForText_ExactMatchImmediate_ReturnsTrue` | Text on screen, 1000ms timeout | Fast return on immediate match |
| 2 | `testWaitForText_ContainsMatchDelayed_ReturnsTrue` | Text appears after 50ms delay | Polling loop detects delayed text |
| 3 | `testWaitForCursor_PositionMatched_ReturnsTrue` | Cursor at target position (0ms) | Immediate position match |
| 4 | `testWaitForUnlock_KeyboardUnlocksDelayed_ReturnsTrue` | Lock released after 50ms | State transition detection |
| 5 | `testIsKeyboardLocked_LockedState_ReturnsTrue` | Query locked state | Accurate boolean reporting |
| 6 | `testWaitForOIAClear_NotInhibited_ReturnsTrue` | OIA not inhibited initially | Immediate success on clear |
| 7 | `testWaitForTextDisappear_TextRemovedDelayed_ReturnsTrue` | Text removed after 40ms | Disappearance detection |
| 8 | `testWaitForCursor_CursorMovesDelayed_ReturnsTrue` | Cursor moves after 60ms | Movement tracking |
| 9 | `testWaitForText_RapidConcurrentUpdates_ReturnsTrue` | Text appears amid 50 updates | Thread-safe eventual consistency |
| 10 | `testWaitForUnlock_WithOIAStateChange_ReturnsTrue` | OIA + keyboard state changes | Multi-state synchronization |

### ADVERSARIAL TESTS (10)

| # | Test Name | Scenario | Validates |
|---|-----------|----------|-----------|
| 1 | `testWaitForText_NeverMatches_TimeoutReturnsFalse` | Text never appears | Timeout expiration (100ms) |
| 2 | `testWaitForText_ZeroTimeout_ReturnsImmediately` | 0ms timeout, text absent | Immediate failure without polling |
| 3 | `testWaitForCursor_WrongPosition_TimeoutReturnsFalse` | Wait (20,40), cursor (5,10) | Position mismatch detection |
| 4 | `testWaitForUnlock_RemainsLocked_TimeoutReturnsFalse` | Keyboard stays locked | Timeout on persistent condition (100ms) |
| 5 | `testWaitForTextDisappear_PersistsOnScreen_TimeoutReturnsFalse` | Text doesn't disappear | Timeout on non-disappearance (100ms) |
| 6 | `testWaitForOIAClear_RemainsInhibited_TimeoutReturnsFalse` | OIA stays inhibited | Timeout on persistent inhibition (120ms) |
| 7 | `testWaitForText_CaseMismatch_TimeoutReturnsFalse` | HELLO vs hello | Strict matching semantics |
| 8 | `testWaitForText_RapidContentChanges_TimeoutReturnsFalse` | Screen changes every 10ms | Pattern matching under contention |
| 9 | `testWaitForCursor_RapidMovementMissesTarget_TimeoutReturnsFalse` | Cursor cycles, misses target | Strict position matching |
| 10 | `testWaitForUnlock_InterruptedException_ReturnsFalse` | Thread interrupted | Exception handling & interrupt restoration |

### BOUNDARY TESTS (3)

| # | Test Name | Scenario | Validates |
|---|-----------|----------|-----------|
| 1 | `testWaitForText_NegativeTimeout_TreatsAsZero` | Timeout = -1ms | Defensive timeout handling |
| 2 | `testWaitForText_LargeTimeout_ExitsEarly_OnMatch` | Timeout = 30000ms, match at 5ms | Early exit (no artificial delays) |
| 3 | `testWaitForCursor_BoundaryPositions_SuccessfulMatch` | Corners (0,0), (23,79), (12,40) | Position math at boundaries |

## Method Coverage Map

### waitForText(String text, int timeoutMs)
- Tests 1-2, 7-9, ADV 1-2, 7-8, BND 1-2 ✓
- Coverage: Immediate, delayed, concurrent, timeout, boundary cases

### waitForTextDisappear(String text, int timeoutMs)
- Tests 7, ADV 5 ✓
- Coverage: Delayed removal, timeout on persistence

### waitForCursor(int row, int col, int timeoutMs)
- Tests 3, 8, ADV 3, 9, BND 3 ✓
- Coverage: Position matching, movement, boundary positions

### waitForUnlock(int timeoutMs)
- Tests 4, 10, ADV 4, 10 ✓
- Coverage: Delayed unlock, persistent lock, interruption

### waitForOIAClear(int timeoutMs)
- Tests 6, ADV 6 ✓
- Coverage: Immediate clear, persistent inhibition

### isKeyboardLocked()
- Tests 5 ✓
- Coverage: State query accuracy

## Dimension Coverage

### Wait Conditions (5 types)
- ✓ Text appears
- ✓ Text disappears
- ✓ Cursor at position
- ✓ Keyboard unlocked
- ✓ OIA cleared

### Timeouts (5 values)
- ✓ 0ms (immediate)
- ✓ 100ms (short)
- ✓ 1000ms (moderate)
- ✓ 30000ms (long)
- ✓ -1 (edge case, treated as 0)

### Screen Changes (4 types)
- ✓ No change (timeout baseline)
- ✓ Immediate (0ms)
- ✓ Delayed (40-70ms)
- ✓ Rapid (every 10ms)

### Concurrency Patterns (3 types)
- ✓ None (baseline)
- ✓ Single-threaded delay
- ✓ Rapid multi-threaded

## Running Tests

### All Tests
```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
java -cp "build:lib/development/*" \
  org.junit.runner.JUnitCore \
  org.tn5250j.framework.tn5250.Screen5250WaitPairwiseTest
```

### Compile & Run
```bash
ant compile-tests
java -cp "build:lib/development/*" \
  org.junit.runner.JUnitCore \
  org.tn5250j.framework.tn5250.Screen5250WaitPairwiseTest
```

## Metrics

| Metric | Value |
|--------|-------|
| Total Tests | 23 |
| Positive | 10 |
| Adversarial | 10 |
| Boundary | 3 |
| Pass Rate | 100% |
| Execution Time | ~1.9 seconds |
| Lines of Code | 949 |
| Timeout Granularity | 10ms |
| Thread Safety | Synchronized blocks |
| Interrupt Safe | Yes (restores flag) |

## Key Findings

### Strengths
1. Comprehensive pairwise coverage
2. Fast execution (<2 seconds)
3. Thread-safe operations
4. Defensive timeout handling
5. Proper interrupt restoration

### Implementation Notes
1. Polling granularity: 10ms (balance of responsiveness vs efficiency)
2. Negative timeouts: treated as 0ms (defensive)
3. Deadline-based: immune to timer wraparound
4. Synchronized: protects TOCTOU races

### Critical for Production
- All wait methods must handle concurrent screen updates
- Race condition hotspots: text matches, cursor moves, OIA state changes
- Interrupt-safe: must restore interrupt flag on exception

## Dependencies

- JUnit 4.5+
- Java 8+
- No external dependencies beyond standard library

## Related Tests

- `ConcurrencyPairwiseTest.java` - General concurrency patterns
- `ScreenPlanesTest.java` - Screen rendering
- `ScreenFieldsTest.java` - Field navigation

## Validation Evidence

```
JUnit version 4.5
.......................
Time: 1.913

OK (23 tests)
```

Compiled successfully with Java 21 Temurin on macOS.

## Notes

- Test double implementations are self-contained (no dependencies on production code)
- All wait semantics are defined in test code (serves as specification)
- Ready for production implementation using same patterns
- Concurrency testing ensures thread-safe implementation is possible
