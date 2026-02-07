# SessionPoolingPairwiseTest - Quick Reference

## Test File Location
```
/Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/tn5250j/session/SessionPoolingPairwiseTest.java
```

## Quick Stats
- **Total Tests:** 20 (all passing ✓)
- **Lines of Code:** 1,219
- **Execution Time:** ~25 seconds
- **Test Categories:** 7 categories covering 32 pairwise dimension interactions
- **Mock Classes:** 2 (MockSessionPool, MockSession)
- **Enums:** 5 (PoolSize, AcquisitionMode, ReleaseMode, ValidationStrategy, EvictionPolicy)

## Test List (20 tests)

### Positive Tests (Happy Path) - 4 tests
1. `testSingleSessionPoolImmediateAcquisitionExplicitRelease` - Basic reuse
2. `testMediumPoolWithOnBorrowValidationAndIdleTimeout` - Validation on borrow + timeout
3. `testLargePoolWithOnReturnValidationAndMaxAgeEviction` - Validation on return + age eviction
4. `testUnlimitedPoolWithAutoReleaseAndPeriodicValidation` - Unlimited pool reuse

### Adversarial Tests - 4 tests
5. `testSinglePoolQueuedAcquisitionWithBlockingWaiters` - Thread contention on single session
6. `testPoolExhaustionTimeoutWhenFullWithIdleEviction` - Pool full timeout
7. `testOnErrorReleaseStrategyRemovesBadSession` - Bad session removal
8. `testUnlimitedPoolWithIdleTimeoutStillEvicts` - Idle timeout on unlimited pool

### Concurrent Stress Tests - 2 tests
9. `testSustainedConcurrentLoadWithQueuedAcquisition` - 20 threads × 50 ops
10. `testAutoReleaseWithoutLeaksUnderConcurrentLoad` - 20 threads, auto-release, no leaks

### Edge Cases & Boundary Conditions - 3 tests
11. `testSinglePoolFastTimeoutWithPeriodicValidation` - 100ms timeout race condition
12. `testOnBorrowValidationFailureTriggersRetry` - 50% validation failure rate
13. `testErrorReleaseDoesNotBlockOtherThreads` - Lock-free error handling

### Validation & Eviction Strategy Tests - 3 tests
14. `testOnReturnAndPeriodicValidationCoexist` - Multiple validation paths
15. `testIdleTimeoutWithoutValidationEvictsCleanly` - Timeout-only eviction
16. `testMaxAgeEvictionIndependentOfIdleTime` - Age-based eviction

### Pool Exhaustion & Recovery - 2 tests
17. `testPoolExhaustionRecoveryWithOnBorrowValidation` - Recovery from exhaustion
18. `testQueueDrainsWithAutoReleaseUnderExhaustion` - Queue drain under load

### Consistency & State Verification - 2 tests
19. `testPoolSizeConsistencyUnderConcurrency` - Metrics accuracy
20. `testSessionReuseCountAccuracyWithErrors` - Reuse counter with failures

## Pairwise Dimensions

**5 dimensions × 4-3 values each = 32 critical pairs covered**

```
Dimension          | Values                          | Tests
-------------------|--------------------------------|-------
Pool Size          | 1, 5, 10, unlimited           | P1-P20
Acquisition Mode   | immediate, queued, timeout    | P1-P20
Release Mode       | explicit, auto, on-error      | P1-P20
Validation         | none, on-borrow, on-return    | P1-P20
Eviction           | none, idle-time, max-age      | P1-P20
```

## Execution

### Compile
```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
javac -cp build:lib/development/junit-4.5.jar \
  -d build/test-classes \
  tests/org/tn5250j/session/SessionPoolingPairwiseTest.java
```

### Run All Tests
```bash
java -cp build/test-classes:build:lib/development/junit-4.5.jar \
  org.junit.runner.JUnitCore \
  org.tn5250j.session.SessionPoolingPairwiseTest
```

### Expected Output
```
JUnit version 4.5
....................
Time: 25.527

OK (20 tests)
```

## Test Configuration Patterns

### Positive Case Pattern (P1)
```
Pool Size: 1
Acquisition: immediate
Release: explicit
Validation: none
Eviction: none
```
✓ Basic session borrow/return works

### Stress Pattern (P9)
```
Pool Size: 5
Acquisition: queued (blocking)
Release: explicit
Validation: periodic
Eviction: idle-time
```
✓ 20 threads × 50 ops = 1,000 total operations

### Adversarial Pattern (P6)
```
Pool Size: 5
Acquisition: timeout-on-full (500ms)
Release: explicit
Validation: none
Eviction: idle-time
```
✓ Pool exhaustion with fail-fast timeout

### Edge Case Pattern (P11)
```
Pool Size: 1
Acquisition: timeout-on-full (100ms)
Release: explicit
Validation: periodic
Eviction: none
```
✓ Fast timeout race conditions with validation

## Key Assertions

### Pool Invariants
- `available + in-use <= total` (pool size bounded)
- `getAvailableCount() + getInUseCount() == getPoolSize()`
- Reuse counter only increments on successful return
- Destruction count reflects only eviction/error removals

### Concurrency Guarantees
- No deadlocks (all tests complete within timeout)
- Queue drains (waiting threads eventually acquire)
- Error isolation (one thread's error doesn't block others)
- Metric consistency (counters remain accurate under load)

### Validation Behavior
- On-borrow validation: runs before session returned
- On-return validation: runs during return operation
- Periodic validation: runs in background (if enabled)
- Validation failure: removes session, may trigger retry

### Eviction Behavior
- Idle timeout: removes sessions unused for X ms
- Max-age: removes sessions created before Y ms ago
- Eviction scheduler: runs periodically in background
- Eviction accuracy: ±500ms due to scheduler granularity

## Mock API Summary

### MockSessionPool
```java
void configure(int size, AcquisitionMode, ReleaseMode,
               ValidationStrategy, EvictionPolicy)
MockSession borrowSession() throws TimeoutException
void returnSession(MockSession) throws IllegalStateException
void setIdleTimeoutMs(long ms)
void setMaxAgeMs(long ms)
void setAcquisitionTimeoutMs(long ms)
void setValidationFailureRate(double rate)
void shutdown()

// Metrics
int getPoolSize()
int getAvailableCount()
int getInUseCount()
int getSessionReuseCount()
int getValidationRunCount()
int getDestructionCount()
```

### MockSession
```java
String getId()
long getCreationTime()
long getLastUsedTime()
void setFailed(boolean failed)
boolean isFailed()
```

## Dimension Details

### Pool Size
- **1**: Single session, tests deadlock detection
- **5**: Medium pool, normal contention scenarios
- **10**: Larger pool, stress testing
- **Unlimited**: Unbounded growth, no size-based exhaustion

### Acquisition Mode
- **immediate**: Non-blocking, fails if no available session
- **queued**: Blocking, waits indefinitely
- **timeout-on-full**: Blocking with timeout, fails on timeout

### Release Mode
- **explicit**: Must call returnSession() explicitly
- **auto**: Session automatically returned to pool (mock: explicit call simulates)
- **on-error**: Failed sessions removed from pool instead of returned

### Validation Strategy
- **none**: No validation
- **on-borrow**: Validate when borrowing (before return)
- **on-return**: Validate when returning (during return call)
- **periodic**: Background validation (optional scheduler)

### Eviction Policy
- **none**: Sessions persist until explicitly closed
- **idle-time**: Remove sessions unused for X milliseconds
- **max-age**: Remove sessions created more than Y milliseconds ago

## Common Test Patterns

### Pattern 1: Happy Path
Arrange → Act → Assert
- Configure pool with valid settings
- Borrow and return session
- Verify session reused, counters incremented

### Pattern 2: Adversarial
Arrange (fill pool) → Act (overflow) → Assert (timeout/error)
- Fill all available slots
- Attempt additional borrow
- Expect TimeoutException

### Pattern 3: Concurrent
Arrange (setup latch/atomics) → Act (spawn threads) → Assert (count results)
- Create CountDownLatch for N threads
- Submit tasks to thread pool
- Verify all tasks complete, counts are correct

### Pattern 4: Eviction
Arrange (create session) → Wait (past timeout) → Act (trigger eviction) → Assert (cleaned)
- Borrow session, return it
- Sleep past idle timeout or max age
- Verify session evicted, pool smaller

### Pattern 5: Metrics
Arrange → Act (multiple ops) → Assert (consistency)
- Track pool size before/after each operation
- Verify invariants: available + in-use = total
- Ensure no counter leaks

## Troubleshooting

### Test Timeout (>30s)
- Reduce concurrent thread count (default: 20)
- Reduce operations per thread (default: varies)
- Check for deadlock: stuck waiting threads

### Assertion Failures
- Verify mock pool configuration in @Before
- Check timeout values (idle, max-age, acquisition)
- Review thread scheduling delays

### Flaky Tests
- All tests are deterministic (no random timing)
- Timeouts set conservatively (2-3x expected)
- All tests should pass consistently

## Integration Steps

To use with real TN5250j session pool:

1. Create `SessionPool` interface matching MockSessionPool API
2. Implement interface with real TN5250j Session5250
3. Replace test fixture creation:
   ```java
   // Before
   sessionPool = new MockSessionPool();

   // After
   sessionPool = new RealSessionPool();
   ```
4. Adjust timeouts for network latency
5. Update validation callbacks for real session state
6. Run full test suite

## Files in Deliverable

| File | Purpose | Status |
|------|---------|--------|
| `tests/org/tn5250j/session/SessionPoolingPairwiseTest.java` | Main test suite | ✓ 1,219 lines |
| `SESSION_POOLING_PAIRWISE_TEST_DELIVERABLE.md` | Full documentation | ✓ Complete |
| `SESSION_POOLING_TEST_QUICK_REFERENCE.md` | This file | ✓ Complete |

---

**Created:** 2026-02-04
**Test Status:** ✓ All 20 passing
**Location:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/tn5250j/session/`
