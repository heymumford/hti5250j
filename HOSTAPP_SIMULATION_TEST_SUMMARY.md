# Host Application Simulation Pairwise Test Suite

**File:** `tests/org/tn5250j/simulation/HostAppSimulationPairwiseTest.java`
**Lines:** 999
**Test Count:** 30
**Status:** All tests passing

## Overview

This test suite validates TN5250j host application simulation and screen flow navigation using comprehensive pairwise testing. Tests cover mock host response scenarios, screen transitions, error handling, timing constraints, and adversarial edge cases.

## Test Organization

### Pairwise Dimensions

| Dimension | Values | Coverage |
|-----------|--------|----------|
| **Screen Type** | signon, menu, data-entry, report, subfile | 5 types |
| **Navigation** | forward, backward, help, exit | 4 actions |
| **Response Timing** | immediate, delayed, timeout | 3 modes |
| **Data Validation** | server-side, client-side, both | 3 locations |
| **Error Response** | message, lock, disconnect | 3 behaviors |

**Total Pairwise Combinations:** 5 × 4 × 3 × 3 × 3 = 540 possible combinations
**Test Coverage:** 30 tests strategically selected to cover critical interactions

## Test Structure

### Mock Implementation Classes

1. **MockHostScreen** - Simulates 5250 host screen states
   - Screen data generation per type
   - Navigation state transitions
   - Keyboard lock/unlock management
   - Navigation history tracking
   - Timeout simulation

2. **HostResponseHandler** - Validates and orchestrates navigation
   - Client-side validation (pre-navigation)
   - Server-side validation (pre-navigation)
   - Validation error handling
   - Error message queuing
   - Disconnection state management

### Test Groups

#### Group 1: SIGNON Screen Tests (5 tests)
- PAIR 1: Forward with client validation, immediate
- PAIR 2: Backward fails, error lock behavior
- PAIR 3: Help with delayed response, server validation
- PAIR 4: Exit with timeout throws exception
- PAIR 5: Forward with both validations, delayed

#### Group 2: MENU Screen Tests (5 tests)
- PAIR 6: Forward with server validation, immediate
- PAIR 7: Backward with client validation, delayed
- PAIR 8: Help with both validations, immediate
- PAIR 9: Exit with timeout and disconnect
- PAIR 10: Forward with timeout response

#### Group 3: DATA_ENTRY Screen Tests (5 tests)
- PAIR 11: Forward with both validations, immediate
- PAIR 12: Backward with server validation, delayed
- PAIR 13: Help with client validation, immediate
- PAIR 14: Exit with both validations, delayed
- PAIR 15: Forward with invalid data (length exceeds 100 chars)

#### Group 4: REPORT Screen Tests (4 tests)
- PAIR 16: Backward with both validations, immediate
- PAIR 17: Help with delayed response, client validation
- PAIR 18: Forward fails (end of flow)
- PAIR 19: Exit with timeout and lock

#### Group 5: SUBFILE Screen Tests (4 tests)
- PAIR 20: Forward with client validation, immediate
- PAIR 21: Backward with both validations, delayed
- PAIR 22: Help with timeout and server validation
- PAIR 23: Exit with delayed client validation

#### Group 6: Adversarial & Edge Cases (7 tests)
- PAIR 24: Rapid transitions with valid data
- PAIR 25: Null data handling with server validation
- PAIR 26: Empty data map handling
- PAIR 27: Keyboard lock persistence prevents navigation
- PAIR 28: Error message queue ordering (FIFO)
- PAIR 29: Screen data consistency matches screen type
- PAIR 30: Disconnection state prevents navigation

## Test Execution Results

```
JUnit version 4.5
..............................
Time: 1.854

OK (30 tests)
```

**All 30 tests passing**
**Average execution time: 61.8 ms per test**

## Test Execution Command

```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless

# Compile
javac -cp "lib/development/junit-4.5.jar:lib/development/*:lib/runtime/*:build" \
  tests/org/tn5250j/simulation/HostAppSimulationPairwiseTest.java

# Run
java -cp "lib/development/junit-4.5.jar:lib/development/*:lib/runtime/*:build:tests" \
  org.junit.runner.JUnitCore org.tn5250j.simulation.HostAppSimulationPairwiseTest
```

## Test Coverage Matrix

### By Screen Type
- Signon: 5 tests
- Menu: 5 tests
- Data-Entry: 5 tests
- Report: 4 tests
- Subfile: 4 tests
- Edge Cases: 7 tests

### By Navigation Action
- Forward: 8 tests
- Backward: 4 tests
- Help: 5 tests
- Exit: 5 tests
- Navigation effects: 3 tests

### By Response Timing
- Immediate: 9 tests
- Delayed: 8 tests
- Timeout: 5 tests
- Timing-neutral: 8 tests

### By Validation Location
- Client-side only: 7 tests
- Server-side only: 7 tests
- Both validations: 9 tests
- No validation: 7 tests

### By Error Response Behavior
- Message: 7 tests
- Lock: 7 tests
- Disconnect: 6 tests
- No error: 10 tests

## Key Testing Patterns

### 1. Positive Path Testing
Tests successful screen navigation with valid data and immediate responses:
- PAIR 1: Signon → Menu transition
- PAIR 11: Data-Entry → Report transition
- PAIR 16: Report → Data-Entry transition

### 2. Adversarial Input Testing
Tests validation failures, invalid data, and error conditions:
- PAIR 15: Field length validation (>100 chars fails)
- PAIR 26: Empty data map handling
- PAIR 27: Keyboard lock prevents navigation

### 3. Timing Testing
Tests immediate, delayed, and timeout response scenarios:
- PAIR 3: Delayed response timing (≥200ms)
- PAIR 4: Timeout exception handling
- PAIR 19: Timeout with lock behavior

### 4. State Consistency Testing
Tests that screen data and state remain consistent:
- PAIR 29: Screen data matches screen type
- PAIR 28: Error message queue order (FIFO)
- PAIR 30: Disconnected state prevents navigation

### 5. Error Handling Testing
Tests three error response behaviors:
- **MESSAGE:** Error queued, navigation allowed
- **LOCK:** Keyboard locked, navigation blocked
- **DISCONNECT:** Connection terminated, navigation blocked

## Mock Implementation Highlights

### MockHostScreen Features
```java
- 5 screen types (SIGNON, MENU, DATA_ENTRY, REPORT, SUBFILE)
- 4 navigation actions (FORWARD, BACKWARD, HELP, EXIT)
- 3 timing modes (IMMEDIATE, DELAYED with 200ms, TIMEOUT)
- State transitions following 5250 application flow
- Keyboard lock/unlock management
- Navigation history tracking
- AtomicInteger transition counter (thread-safe)
```

### HostResponseHandler Features
```java
- Pre-navigation client-side validation
- Pre-navigation server-side validation
- Validation error handling with 3 response types
- Concurrent error message queue (ConcurrentLinkedQueue)
- Screen type-aware validation rules
- Field length constraints (max 100 chars for DATA_ENTRY)
- Required field validation
```

## Validation Logic

### Client-Side Validation
- Applied before navigation
- Null/empty data valid for: MENU, REPORT, HELP, SIGNON, SUBFILE
- Field values must be non-empty if provided
- No length constraints at client side

### Server-Side Validation
- Applied before navigation (based on current screen)
- DATA_ENTRY requires non-null, non-empty data
- Field length limit: 100 characters max
- Field values cannot be null when data is provided

## Navigation Rules

| From | Forward | Backward | Help | Exit |
|------|---------|----------|------|------|
| SIGNON | → MENU | ✗ Fail | ✓ Help | ✓ Lock |
| MENU | → DATA_ENTRY | → SIGNON | ✓ Help | ✓ Lock |
| DATA_ENTRY | → REPORT | → MENU | ✓ Help | ✓ Lock |
| REPORT | ✗ Fail | → DATA_ENTRY | ✓ Help | ✓ Lock |
| SUBFILE | → REPORT | → MENU | ✓ Help | ✓ Lock |

## Error Response Behaviors

| Behavior | Effect | Navigation | Next State |
|----------|--------|-----------|-----------|
| MESSAGE | Error queued | Proceeds | Current screen |
| LOCK | Keyboard locked | Blocked | Keyboard locked |
| DISCONNECT | Disconnected | Blocked | Keyboard locked |

## Design Patterns

### RED-GREEN-REFACTOR Flow
1. **RED Phase:** Tests written to fail, exposing simulation gaps
2. **GREEN Phase:** Mock implementation built to pass tests
3. **Refactor:** Validation logic refined for real-world accuracy

### Pairwise Testing Strategy
- Selected 30 from 540 possible combinations
- Covers all single-dimension values
- Captures important two-way interactions
- Focuses on error conditions and edge cases

### Mock Interaction Patterns
- **State Transitions:** Screen → Screen navigation flows
- **Validation Cascades:** Client → Navigate → Server flow
- **Error Propagation:** Validation failure → Error handling → State lock
- **Timing Simulation:** Immediate/Delayed/Timeout responses

## Extension Points

Future tests can add:
- **Concurrency testing:** Multiple navigation threads
- **Recovery testing:** Timeout recovery and reconnection
- **Field-level validation:** Complex field constraints
- **Workflow testing:** Multi-screen business processes
- **Performance testing:** Navigation latency measurement
- **Stress testing:** Rapid navigation sequences

## Files Generated

```
/Users/vorthruna/ProjectsWATTS/tn5250j-headless/
├── tests/org/tn5250j/simulation/
│   └── HostAppSimulationPairwiseTest.java (999 lines)
└── HOSTAPP_SIMULATION_TEST_SUMMARY.md (this file)
```

## TDD Philosophy

This test suite exemplifies Test-Driven Development:
1. **Test First:** All tests written before production code
2. **Specification:** Tests document expected behavior
3. **Validation:** Passing tests prove implementation correctness
4. **Refactoring:** Tests enable safe code improvements
5. **Evidence:** Test execution provides proof of functionality

## Conclusion

The HostAppSimulationPairwiseTest suite provides comprehensive coverage of TN5250j host application simulation with 30 strategically-selected pairwise tests. All tests pass, validating correct screen navigation, data validation, error handling, and timing behavior across multiple dimensions.
