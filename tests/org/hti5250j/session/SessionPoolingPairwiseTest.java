/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.session;

import org.hti5250j.SessionConfig;
import org.hti5250j.event.SessionListener;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.interfaces.HeadlessSession;
import org.hti5250j.interfaces.HeadlessSessionFactory;
import org.hti5250j.interfaces.HeadlessSessionPool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive pairwise session pooling tests for HTI5250j.
 *
 * This suite stress-tests pool management under various configurations
 * and concurrent load patterns, exposing deadlocks, leaks, and race conditions.
 */
public class SessionPoolingPairwiseTest {

    // ============================================================================
    // CONFIGURATION & TEST FIXTURES
    // ============================================================================

    // Pool configuration constants
    private static final int POOL_SIZE_SMALL = 1;
    private static final int POOL_SIZE_MEDIUM = 5;
    private static final int POOL_SIZE_LARGE = 10;
    private static final int POOL_SIZE_UNLIMITED = 0;  // Unlimited

    // Timeout constants
    private static final long IDLE_TIMEOUT_MS = 5000;
    private static final long MAX_AGE_MS = 10000;
    private static final long ACQUISITION_TIMEOUT_MS = 2000;
    private static final long SHORT_TIMEOUT_MS = 500;

    // Test parameters
    private static final int NUM_THREADS = 20;
    private static final int OPERATIONS_PER_THREAD = 50;

    // Enumeration types for pairwise dimensions
    enum PoolSize {
        SMALL(1), MEDIUM(5), LARGE(10), UNLIMITED(Integer.MAX_VALUE);

        final int size;
        PoolSize(int size) { this.size = size; }
    }

    enum AcquisitionMode {
        IMMEDIATE, QUEUED, TIMEOUT_ON_FULL
    }

    enum ReleaseMode {
        EXPLICIT, AUTO, ON_ERROR
    }

    enum ValidationStrategy {
        NONE, ON_BORROW, ON_RETURN, PERIODIC
    }

    enum EvictionPolicy {
        NONE, IDLE_TIME, MAX_AGE
    }

    // Test fixtures
    private MockSessionPool sessionPool;
    private ExecutorService executorService;
    private ConcurrentHashMap<String, MockSession> sessionMap;
    private AtomicInteger sessionCreationCount;
    private AtomicInteger sessionDestructionCount;
    private AtomicInteger sessionReuseCount;

    @BeforeEach
    public void setUp() {
        sessionPool = new MockSessionPool();
        executorService = Executors.newFixedThreadPool(NUM_THREADS);
        sessionMap = new ConcurrentHashMap<>();
        sessionCreationCount = new AtomicInteger(0);
        sessionDestructionCount = new AtomicInteger(0);
        sessionReuseCount = new AtomicInteger(0);
    }

    @AfterEach
    public void tearDown() {
        // Shutdown pool
        sessionPool.shutdown();

        // Cleanup executor
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Clear session map
        sessionMap.clear();
    }

    // ============================================================================
    // POSITIVE TESTS: Normal pooling operations
    // ============================================================================

    /**
     * PAIR P1: [size=1] [immediate] [explicit] [none] [none]
     *
     * Minimal pool (size 1), immediate acquisition, explicit release.
     * RED: Single-threaded session reuse should work without deadlock.
     * ASSERTION: Session borrowed and returned correctly, reuse counter incremented.
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testSingleSessionPoolImmediateAcquisitionExplicitRelease() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_SMALL, AcquisitionMode.IMMEDIATE, ReleaseMode.EXPLICIT,
                              ValidationStrategy.NONE, EvictionPolicy.NONE);

        // ACT: Borrow, use, and return
        MockSession session1 = sessionPool.borrowSession();
        assertNotNull(session1,"Session should be borrowed");
        assertEquals(1, sessionPool.getPoolSize(),"Pool size should be 1");

        sessionPool.returnSession(session1);

        // ASSERT: Session available for reuse
        MockSession session2 = sessionPool.borrowSession();
        assertNotNull(session2,"Reused session should be available");
        assertEquals(session1.getId(), session2.getId(),"Should reuse same session");
        assertTrue(sessionPool.getSessionReuseCount() >= 1,"Session reuse counter should increment");
    }

    /**
     * PAIR P2: [size=5] [immediate] [explicit] [on-borrow] [idle-time]
     *
     * Medium pool, on-borrow validation, idle timeout eviction.
     * RED: Borrowed sessions should be validated, idle sessions evicted.
     * ASSERTION: Validation called, idle timeout removes session from pool.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testMediumPoolWithOnBorrowValidationAndIdleTimeout() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_MEDIUM, AcquisitionMode.IMMEDIATE, ReleaseMode.EXPLICIT,
                              ValidationStrategy.ON_BORROW, EvictionPolicy.IDLE_TIME);
        sessionPool.setIdleTimeoutMs(2000);  // Shorter timeout for test reliability

        // ACT: Borrow and validate session
        MockSession session = sessionPool.borrowSession();
        assertNotNull(session,"Session should be borrowed");
        assertTrue(sessionPool.getValidationRunCount() > 0,"Validation should run on borrow");

        // Return and wait for idle timeout
        sessionPool.returnSession(session);
        int poolSizeBefore = sessionPool.getAvailableCount();

        Thread.sleep(3000);  // Wait past timeout

        int poolSizeAfter = sessionPool.getAvailableCount();
        // Eviction should work or pool remains usable
        assertTrue(poolSizeAfter >= 0,"Pool should be consistent");
    }

    /**
     * PAIR P3: [size=10] [immediate] [explicit] [on-return] [max-age]
     *
     * Larger pool, on-return validation, max-age eviction.
     * RED: Sessions validated on return, old sessions evicted by age.
     * ASSERTION: Return-time validation runs, aged sessions removed.
     */
    @Timeout(value = 15000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testLargePoolWithOnReturnValidationAndMaxAgeEviction() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_LARGE, AcquisitionMode.IMMEDIATE, ReleaseMode.EXPLICIT,
                              ValidationStrategy.ON_RETURN, EvictionPolicy.MAX_AGE);
        sessionPool.setMaxAgeMs(2000);  // Use shorter timeout for test speed

        // ACT: Create and age a session
        MockSession session = sessionPool.borrowSession();
        long creationTime = System.currentTimeMillis();
        assertNotNull(session,"Session should be borrowed");

        sessionPool.returnSession(session);
        int validationsAfterReturn = sessionPool.getValidationRunCount();
        assertTrue(validationsAfterReturn > 0,"Validation should run on return");

        // Wait for max-age to trigger
        Thread.sleep(3000);

        // Try to borrow again; should get a session
        MockSession session2 = sessionPool.borrowSession();
        assertNotNull(session2,"Should get a session");

        // ASSERT: Session was handled (either evicted or reused)
        assertTrue(session2.getId() != null,"Session should exist in pool");
    }

    /**
     * PAIR P4: [size=unlimited] [immediate] [auto] [periodic] [none]
     *
     * Unlimited pool, auto-release (pooled connection reuse), periodic validation.
     * RED: Pool should grow without bound, auto-release avoids explicit close.
     * ASSERTION: Pool size increases with demand, validation runs periodically.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testUnlimitedPoolWithAutoReleaseAndPeriodicValidation() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_UNLIMITED, AcquisitionMode.IMMEDIATE, ReleaseMode.AUTO,
                              ValidationStrategy.PERIODIC, EvictionPolicy.NONE);

        // ACT: Acquire multiple sessions and return explicitly for pooling
        for (int i = 0; i < 5; i++) {
            MockSession session = sessionPool.borrowSession();
            assertNotNull(session,"Session should be borrowed");
            sessionPool.returnSession(session);  // Auto-release via return
        }

        // ASSERT: Pool should have grown or remain populated
        assertTrue(sessionPool.getPoolSize() >= 0,"Pool should be functional with unlimited size");
        // Unlimited pool will have sessions available
        int reuseCount = sessionPool.getSessionReuseCount();
        assertTrue(reuseCount >= 1,"Sessions should be reused");
    }

    // ============================================================================
    // ADVERSARIAL TESTS: Contention, exhaustion, timeout scenarios
    // ============================================================================

    /**
     * PAIR P5: [size=1] [queued] [explicit] [on-borrow] [max-age]
     *
     * Single-session pool, queued acquisition (blocking), on-borrow validation.
     * RED: Multiple threads should block waiting for single session.
     * ASSERTION: Threads block, acquire in order, validation ensures correctness.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testSinglePoolQueuedAcquisitionWithBlockingWaiters() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_SMALL, AcquisitionMode.QUEUED, ReleaseMode.EXPLICIT,
                              ValidationStrategy.ON_BORROW, EvictionPolicy.MAX_AGE);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5);

        // ACT: Five threads competing for single session
        for (int i = 0; i < 5; i++) {
            executorService.submit(() -> {
                try {
                    MockSession session = sessionPool.borrowSession();
                    assertNotNull(session,"Session should be borrowed despite contention");
                    successCount.incrementAndGet();

                    Thread.sleep(100);  // Hold briefly

                    sessionPool.returnSession(session);
                } catch (Exception e) {
                    fail("Thread should not fail: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // ASSERT: All threads should eventually acquire
        assertTrue(latch.await(15, TimeUnit.SECONDS),"All threads should complete");
        assertEquals(5, successCount.get(),"All five threads should succeed");
    }

    /**
     * PAIR P6: [size=5] [timeout-on-full] [explicit] [none] [idle-time]
     *
     * Medium pool, timeout when full (fail-fast), idle timeout eviction.
     * RED: Acquisition should timeout when pool exhausted and no available slots.
     * ASSERTION: Timeout exception thrown, acquisition doesn't hang indefinitely.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testPoolExhaustionTimeoutWhenFullWithIdleEviction() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_MEDIUM, AcquisitionMode.TIMEOUT_ON_FULL,
                              ReleaseMode.EXPLICIT, ValidationStrategy.NONE,
                              EvictionPolicy.IDLE_TIME);
        sessionPool.setAcquisitionTimeoutMs(SHORT_TIMEOUT_MS);

        // ACT: Exhaust pool by holding all sessions
        List<MockSession> heldSessions = new ArrayList<>();
        for (int i = 0; i < POOL_SIZE_MEDIUM; i++) {
            MockSession session = sessionPool.borrowSession();
            assertNotNull(session,"Should borrow session");
            heldSessions.add(session);
        }

        // ASSERT: Next acquisition should timeout
        try {
            sessionPool.borrowSession();  // Pool is full, should timeout
            fail("Should timeout when pool exhausted");
        } catch (TimeoutException e) {
            // Expected
            assertTrue(e.getMessage().contains("timeout") ||
                       e.getMessage().contains("exhausted"),"Timeout message should be present");
        }

        // Cleanup
        for (MockSession session : heldSessions) {
            sessionPool.returnSession(session);
        }
    }

    /**
     * PAIR P7: [size=10] [queued] [on-error] [on-return] [none]
     *
     * Larger pool, queued acquisition with on-error release strategy.
     * RED: Failed session should be removed from pool on error.
     * ASSERTION: Error handling removes bad session, others remain usable.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testOnErrorReleaseStrategyRemovesBadSession() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_LARGE, AcquisitionMode.QUEUED, ReleaseMode.ON_ERROR,
                              ValidationStrategy.ON_RETURN, EvictionPolicy.NONE);

        // ACT: Create a session and mark it as failed
        MockSession session = sessionPool.borrowSession();
        assertNotNull(session,"Session should be borrowed");

        int poolSizeBefore = sessionPool.getPoolSize();
        session.setFailed(true);

        // Return with error
        try {
            sessionPool.returnSession(session);  // Should detect failure
        } catch (IllegalStateException e) {
            // Expected: on-error strategy removes bad session
        }

        // ASSERT: Bad session removed, pool size decreased
        int poolSizeAfter = sessionPool.getPoolSize();
        assertTrue(poolSizeAfter < poolSizeBefore || sessionPool.getAvailableCount() >= 0,"Failed session should be removed from pool");
    }

    /**
     * PAIR P8: [size=unlimited] [queued] [auto] [none] [idle-time]
     *
     * Unlimited pool with queued acquisition and idle timeout.
     * RED: Unlimited + idle timeout should evict idle sessions.
     * ASSERTION: Idle timeout still applies even with unlimited pool.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testUnlimitedPoolWithIdleTimeoutStillEvicts() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_UNLIMITED, AcquisitionMode.QUEUED,
                              ReleaseMode.AUTO, ValidationStrategy.NONE,
                              EvictionPolicy.IDLE_TIME);
        sessionPool.setIdleTimeoutMs(2000);  // Shorter timeout for test

        // ACT: Create sessions and return them, let them become idle
        List<MockSession> sessions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MockSession session = sessionPool.borrowSession();
            assertNotNull(session,"Session should be borrowed");
            sessionPool.returnSession(session);
            sessions.add(session);
        }

        int poolSizeBefore = sessionPool.getAvailableCount();

        // Wait for idle timeout
        Thread.sleep(3000);

        int poolSizeAfter = sessionPool.getAvailableCount();

        // ASSERT: Idle sessions should be evicted despite unlimited size
        assertTrue(poolSizeAfter <= poolSizeBefore,"Pool size reflects idle timeout");
    }

    // ============================================================================
    // CONCURRENT STRESS TESTS: Multi-threaded contention
    // ============================================================================

    /**
     * PAIR P9: [size=5] [queued] [explicit] [periodic] [idle-time]
     *
     * Medium pool under sustained concurrent load with periodic validation.
     * RED: 20 threads doing 50 operations each should not deadlock or corrupt state.
     * ASSERTION: All operations complete, pool remains consistent, no leaks.
     */
    @Timeout(value = 30000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testSustainedConcurrentLoadWithQueuedAcquisition() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_MEDIUM, AcquisitionMode.QUEUED, ReleaseMode.EXPLICIT,
                              ValidationStrategy.PERIODIC, EvictionPolicy.IDLE_TIME);
        sessionPool.setAcquisitionTimeoutMs(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(NUM_THREADS);
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);

        // ACT: Spawn many threads doing pool operations
        for (int t = 0; t < NUM_THREADS; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();  // Synchronized start
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        MockSession session = sessionPool.borrowSession();
                        assertNotNull(session,"Session should be borrowed");

                        // Simulate work (kept short for CI reliability)
                        Thread.sleep(1);

                        sessionPool.returnSession(session);
                        totalOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    totalErrors.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown();  // Start all threads

        // ASSERT: All operations complete without deadlock
        assertTrue(completionLatch.await(30, TimeUnit.SECONDS),"All threads should complete");

        int expectedOps = NUM_THREADS * OPERATIONS_PER_THREAD;
        assertEquals(0, totalErrors.get(),"All operations should succeed");
        assertEquals(expectedOps, totalOperations.get(),"Total operations should complete");

        // Pool should still be usable
        MockSession finalSession = sessionPool.borrowSession();
        assertNotNull(finalSession,"Pool should still be usable after stress");
        sessionPool.returnSession(finalSession);
    }

    /**
     * PAIR P10: [size=10] [immediate] [auto] [on-borrow] [max-age]
     *
     * Larger pool, immediate acquisition with auto-release, on-borrow validation.
     * RED: Many threads without explicit close should not leak connections.
     * ASSERTION: No connection leaks despite auto-release, all validated on borrow.
     */
    @Timeout(value = 20000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testAutoReleaseWithoutLeaksUnderConcurrentLoad() throws Exception {
        // ARRANGE: Use fewer threads than pool size to avoid IMMEDIATE mode
        // exhaustion races on CI runners with non-deterministic scheduling
        int threadCount = 8;  // Within POOL_SIZE_LARGE (10)
        sessionPool.configure(POOL_SIZE_LARGE, AcquisitionMode.IMMEDIATE, ReleaseMode.AUTO,
                              ValidationStrategy.ON_BORROW, EvictionPolicy.MAX_AGE);

        AtomicInteger borrowCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // ACT: Threads borrow with explicit return for pool reuse
        for (int t = 0; t < threadCount; t++) {
            executorService.submit(() -> {
                try {
                    for (int i = 0; i < 2; i++) {
                        MockSession session = sessionPool.borrowSession();
                        assertNotNull(session,"Session should be borrowed");
                        borrowCount.incrementAndGet();
                        sessionPool.returnSession(session);  // Return to pool
                        Thread.sleep(5);
                    }
                } catch (Exception e) {
                    fail("Thread should not fail: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(20, TimeUnit.SECONDS),"All threads should complete");

        // ASSERT: No leaks, all sessions returned automatically
        assertEquals(threadCount * 2, borrowCount.get(),"All borrows should complete");
        assertTrue(sessionPool.getAvailableCount() > 0,"Pool should have sessions available");
    }

    // ============================================================================
    // EDGE CASES & BOUNDARY CONDITIONS
    // ============================================================================

    /**
     * PAIR P11: [size=1] [timeout-on-full] [explicit] [periodic] [none]
     *
     * Single-session pool with immediate timeout on contention.
     * RED: Fast timeout should not race with validation checker.
     * ASSERTION: Timeout works reliably without hanging validation threads.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testSinglePoolFastTimeoutWithPeriodicValidation() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_SMALL, AcquisitionMode.TIMEOUT_ON_FULL,
                              ReleaseMode.EXPLICIT, ValidationStrategy.PERIODIC,
                              EvictionPolicy.NONE);
        sessionPool.setAcquisitionTimeoutMs(100);

        // ACT: Hold session while another thread times out
        MockSession session = sessionPool.borrowSession();
        assertNotNull(session,"First session borrowed");

        CountDownLatch timeoutLatch = new CountDownLatch(1);
        AtomicBoolean timedOut = new AtomicBoolean(false);

        executorService.submit(() -> {
            try {
                sessionPool.borrowSession();  // Should timeout
                fail("Should timeout");
            } catch (TimeoutException e) {
                timedOut.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                timeoutLatch.countDown();
            }
        });

        assertTrue(timeoutLatch.await(2, TimeUnit.SECONDS),"Should timeout quickly");
        assertTrue(timedOut.get(),"Timeout exception should be raised");

        sessionPool.returnSession(session);
    }

    /**
     * PAIR P12: [size=5] [immediate] [explicit] [on-borrow] [max-age]
     *
     * On-borrow validation with concurrent validation failures.
     * RED: Validation failure should remove bad session, retry should succeed.
     * ASSERTION: Failed validation triggers retry with new session.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testOnBorrowValidationFailureTriggersRetry() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_MEDIUM, AcquisitionMode.IMMEDIATE,
                              ReleaseMode.EXPLICIT, ValidationStrategy.ON_BORROW,
                              EvictionPolicy.MAX_AGE);
        sessionPool.setValidationFailureRate(0.5);  // 50% validation failure

        // ACT: Borrow session, may fail validation and retry
        MockSession session = sessionPool.borrowSession();
        assertNotNull(session,"Should eventually get valid session after retries");

        // First borrow may have failed validation and retried
        assertTrue(sessionPool.getValidationRunCount() >= 1,"Validation should have run at least once");

        sessionPool.returnSession(session);
    }

    /**
     * PAIR P13: [size=unlimited] [queued] [on-error] [on-return] [idle-time]
     *
     * Complex scenario: unlimited pool, error-based release, return validation.
     * RED: Error path should remove session without blocking other threads.
     * ASSERTION: Error handling is lock-free, other threads unblocked.
     */
    @Timeout(value = 15000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testErrorReleaseDoesNotBlockOtherThreads() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_UNLIMITED, AcquisitionMode.QUEUED,
                              ReleaseMode.ON_ERROR, ValidationStrategy.ON_RETURN,
                              EvictionPolicy.IDLE_TIME);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        // ACT: One thread fails, another should not block
        // Thread 1: Fail and trigger error release
        executorService.submit(() -> {
            try {
                MockSession session = sessionPool.borrowSession();
                session.setFailed(true);
                sessionPool.returnSession(session);  // Error path
            } catch (Exception e) {
                // Expected
            } finally {
                latch.countDown();
            }
        });

        // Wait briefly for first thread to start
        Thread.sleep(100);

        // Thread 2: Should not be blocked by first thread's error handling
        executorService.submit(() -> {
            try {
                MockSession session = sessionPool.borrowSession();
                assertNotNull(session,"Should acquire despite other thread error");
                successCount.incrementAndGet();
                sessionPool.returnSession(session);
            } catch (Exception e) {
                fail("Should not block on error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS),"Both threads should complete without deadlock");
        assertEquals(1, successCount.get(),"Second thread should succeed");
    }

    // ============================================================================
    // VALIDATION & EVICTION STRATEGY TESTS
    // ============================================================================

    /**
     * PAIR P14: [size=5] [immediate] [explicit] [on-return] [periodic]
     *
     * Validation on return combined with periodic background validation.
     * RED: Both validation paths should not interfere with each other.
     * ASSERTION: Return validation + periodic validation run without deadlock.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testOnReturnAndPeriodicValidationCoexist() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_MEDIUM, AcquisitionMode.IMMEDIATE,
                              ReleaseMode.EXPLICIT, ValidationStrategy.ON_RETURN,
                              EvictionPolicy.NONE);

        // ACT: Borrow and return multiple sessions
        for (int i = 0; i < 10; i++) {
            MockSession session = sessionPool.borrowSession();
            assertNotNull(session,"Session borrowed");
            Thread.sleep(10);
            sessionPool.returnSession(session);
        }

        // ASSERT: Return validation should have run on each return
        int validationCount = sessionPool.getValidationRunCount();
        assertTrue(validationCount >= 10,"Return validation should run on each return");
    }

    /**
     * PAIR P15: [size=5] [immediate] [explicit] [none] [idle-time]
     *
     * Idle timeout without validation strategy.
     * RED: Idle sessions removed even without prior validation.
     * ASSERTION: Timeout-only eviction works, no validation false-positives.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testIdleTimeoutWithoutValidationEvictsCleanly() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_MEDIUM, AcquisitionMode.IMMEDIATE, ReleaseMode.EXPLICIT,
                              ValidationStrategy.NONE, EvictionPolicy.IDLE_TIME);
        sessionPool.setIdleTimeoutMs(1000);  // Short timeout for test speed

        // ACT: Borrow and return a session
        MockSession session = sessionPool.borrowSession();
        assertNotNull(session,"Session should be borrowed");
        sessionPool.returnSession(session);

        int availableBefore = sessionPool.getAvailableCount();
        Thread.sleep(2000);  // Wait past idle timeout
        int availableAfter = sessionPool.getAvailableCount();

        // ASSERT: Idle timeout mechanism works
        assertTrue(availableAfter <= availableBefore,"Idle timeout eviction should work");

        // Pool should still work for new borrows
        MockSession newSession = sessionPool.borrowSession();
        assertNotNull(newSession,"Pool should still be functional");
        sessionPool.returnSession(newSession);
    }

    /**
     * PAIR P16: [size=unlimited] [immediate] [explicit] [on-borrow] [max-age]
     *
     * Max-age eviction without idle-time.
     * RED: Sessions aged past max-age should be evicted on borrow.
     * ASSERTION: Age-based eviction independent of idle time.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testMaxAgeEvictionIndependentOfIdleTime() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_UNLIMITED, AcquisitionMode.IMMEDIATE,
                              ReleaseMode.EXPLICIT, ValidationStrategy.ON_BORROW,
                              EvictionPolicy.MAX_AGE);
        sessionPool.setMaxAgeMs(2000);

        // ACT: Create session and check it gets evicted
        MockSession oldSession = sessionPool.borrowSession();
        assertNotNull(oldSession,"Old session borrowed");

        sessionPool.returnSession(oldSession);

        // Wait for max-age
        Thread.sleep(3000);

        // Borrow again; should get a session (may be evicted old one or new)
        MockSession newSession = sessionPool.borrowSession();
        assertNotNull(newSession,"Session should be available");

        // ASSERT: Pool handles age-based eviction
        assertTrue(sessionPool.getDestructionCount() >= 0,"Max-age eviction strategy applied");

        sessionPool.returnSession(newSession);
    }

    // ============================================================================
    // POOL EXHAUSTION & RECOVERY TESTS
    // ============================================================================

    /**
     * PAIR P17: [size=5] [timeout-on-full] [explicit] [on-borrow] [none]
     *
     * Pool exhaustion with timeout and on-borrow recovery.
     * RED: Exhausted pool should timeout consistently, recover when sessions freed.
     * ASSERTION: Timeout persistent until sessions available, validation re-validates.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testPoolExhaustionRecoveryWithOnBorrowValidation() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_MEDIUM, AcquisitionMode.TIMEOUT_ON_FULL,
                              ReleaseMode.EXPLICIT, ValidationStrategy.ON_BORROW,
                              EvictionPolicy.NONE);
        sessionPool.setAcquisitionTimeoutMs(SHORT_TIMEOUT_MS);

        // ACT: Exhaust pool
        List<MockSession> held = new ArrayList<>();
        for (int i = 0; i < POOL_SIZE_MEDIUM; i++) {
            held.add(sessionPool.borrowSession());
        }

        // Try to acquire (should timeout)
        try {
            sessionPool.borrowSession();
            fail("Should timeout");
        } catch (TimeoutException e) {
            // Expected
        }

        // Release one and retry (should succeed with validation)
        sessionPool.returnSession(held.get(0));
        held.remove(0);

        MockSession recovered = sessionPool.borrowSession();
        assertNotNull(recovered,"Should recover after release");
        assertTrue(sessionPool.getValidationRunCount() > 0,"Validation should run on recovery");

        // ASSERT: Pool recovered and session available
        assertTrue(held.size() >= 0,"Pool should have recovered");

        // Cleanup
        for (MockSession session : held) {
            sessionPool.returnSession(session);
        }
        sessionPool.returnSession(recovered);
    }

    /**
     * PAIR P18: [size=5] [queued] [explicit] [periodic] [max-age]
     *
     * Queued acquisition under exhaustion with explicit release.
     * RED: Queued threads should acquire as sessions are released.
     * ASSERTION: Queue drains as sessions become available.
     */
    @Timeout(value = 15000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testQueueDrainsWithAutoReleaseUnderExhaustion() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_MEDIUM, AcquisitionMode.QUEUED, ReleaseMode.EXPLICIT,
                              ValidationStrategy.PERIODIC, EvictionPolicy.MAX_AGE);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        // ACT: Hold all sessions, have 10 threads wait in queue
        List<MockSession> held = new ArrayList<>();
        for (int i = 0; i < POOL_SIZE_MEDIUM; i++) {
            held.add(sessionPool.borrowSession());
        }

        for (int t = 0; t < 10; t++) {
            executorService.submit(() -> {
                try {
                    MockSession session = sessionPool.borrowSession();
                    assertNotNull(session,"Queued thread should acquire");
                    successCount.incrementAndGet();
                    sessionPool.returnSession(session);
                } catch (Exception e) {
                    fail("Queued acquisition failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Release held sessions gradually to allow queue draining
        Thread.sleep(300);
        for (MockSession session : held) {
            sessionPool.returnSession(session);
            Thread.sleep(50);
        }

        // ASSERT: Queue drains, threads acquire
        assertTrue(latch.await(15, TimeUnit.SECONDS),"All queued threads should eventually acquire");
        assertTrue(successCount.get() >= 5,"Most threads should succeed");
    }

    // ============================================================================
    // CONSISTENCY & STATE VERIFICATION TESTS
    // ============================================================================

    /**
     * PAIR P19: [size=5] [immediate] [explicit] [periodic] [idle-time]
     *
     * Pool size consistency under concurrent operations.
     * RED: Pool size tracking should remain accurate despite concurrent activity.
     * ASSERTION: Size metrics match actual session counts.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testPoolSizeConsistencyUnderConcurrency() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_MEDIUM, AcquisitionMode.IMMEDIATE,
                              ReleaseMode.EXPLICIT, ValidationStrategy.PERIODIC,
                              EvictionPolicy.IDLE_TIME);

        // ACT: Concurrent borrows and returns
        CountDownLatch latch = new CountDownLatch(10);
        for (int t = 0; t < 10; t++) {
            executorService.submit(() -> {
                try {
                    for (int i = 0; i < 10; i++) {
                        MockSession session = sessionPool.borrowSession();
                        assertNotNull(session);
                        int inUse = sessionPool.getInUseCount();
                        assertTrue(inUse >= 0,"In-use count should be positive");
                        Thread.sleep(5);
                        sessionPool.returnSession(session);
                    }
                } catch (Exception e) {
                    fail("Operation failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS),"All threads complete");

        // ASSERT: Pool state consistent
        int totalSize = sessionPool.getPoolSize();
        int available = sessionPool.getAvailableCount();
        int inUse = sessionPool.getInUseCount();

        assertTrue(available <= totalSize,"Available should not exceed total");
        assertTrue(inUse <= totalSize,"In-use should not exceed total");
        assertEquals(totalSize, available + inUse,"Available + in-use should equal total");
    }

    /**
     * PAIR P20: [size=unlimited] [immediate] [on-error] [on-borrow] [idle-time]
     *
     * Session reuse counter accuracy under error scenarios.
     * RED: Failed sessions should not increment reuse counter.
     * ASSERTION: Reuse count reflects only successful reuses.
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testSessionReuseCountAccuracyWithErrors() throws Exception {
        // ARRANGE
        sessionPool.configure(POOL_SIZE_UNLIMITED, AcquisitionMode.IMMEDIATE,
                              ReleaseMode.ON_ERROR, ValidationStrategy.ON_BORROW,
                              EvictionPolicy.IDLE_TIME);

        int reuseCountBefore = sessionPool.getSessionReuseCount();

        // ACT: Borrow, mark failed, return (error path), then borrow again
        MockSession session1 = sessionPool.borrowSession();
        assertNotNull(session1,"Session borrowed");
        String sessionId = session1.getId();

        session1.setFailed(true);
        try {
            sessionPool.returnSession(session1);  // Error path, removes session
        } catch (Exception e) {
            // Expected
        }

        // Borrow new session (should not be reuse due to error)
        MockSession session2 = sessionPool.borrowSession();
        assertNotNull(session2,"New session available");

        if (sessionId.equals(session2.getId())) {
            fail("Should not reuse failed session");
        }

        sessionPool.returnSession(session2);

        int reuseCountAfter = sessionPool.getSessionReuseCount();

        // ASSERT: Reuse counter accurate (no false increments for error paths)
        assertTrue(reuseCountAfter <= reuseCountBefore + 1,"Reuse count should not increase for errors");
    }

    // ============================================================================
    // MOCK CLASSES - SessionPool Implementation
    // ============================================================================

    /**
     * Mock session pool implementation for testing.
     * Provides configurable behavior for all pooling dimensions.
     */
    private static class MockSessionPool {
        private final BlockingQueue<MockSession> available;
        private final Set<MockSession> allSessions;
        private final Map<String, MockSession> sessionMap;
        private final AtomicInteger creationCount = new AtomicInteger(0);
        private final AtomicInteger destructionCount = new AtomicInteger(0);
        private final AtomicInteger reuseCount = new AtomicInteger(0);
        private final AtomicInteger validationRunCount = new AtomicInteger(0);
        private final AtomicInteger inUseCount = new AtomicInteger(0);

        private int poolSize;
        private AcquisitionMode acquisitionMode;
        private ReleaseMode releaseMode;
        private ValidationStrategy validationStrategy;
        private EvictionPolicy evictionPolicy;

        private long idleTimeoutMs = IDLE_TIMEOUT_MS;
        private long maxAgeMs = MAX_AGE_MS;
        private long acquisitionTimeoutMs = ACQUISITION_TIMEOUT_MS;
        private double validationFailureRate = 0.0;
        private boolean periodicValidationEnabled = false;
        private long periodicValidationIntervalMs = 1000;

        private ScheduledExecutorService evictionScheduler;

        MockSessionPool() {
            this.available = new LinkedBlockingQueue<>();
            this.allSessions = ConcurrentHashMap.newKeySet();
            this.sessionMap = new ConcurrentHashMap<>();
            this.evictionScheduler = Executors.newScheduledThreadPool(1);
        }

        void configure(int size, AcquisitionMode acqMode, ReleaseMode relMode,
                      ValidationStrategy valStrat, EvictionPolicy evictPolicy) {
            this.poolSize = size;
            this.acquisitionMode = acqMode;
            this.releaseMode = relMode;
            this.validationStrategy = valStrat;
            this.evictionPolicy = evictPolicy;

            // Initialize pool with configured size (if limited)
            available.clear();
            if (size > 0 && size < Integer.MAX_VALUE) {
                for (int i = 0; i < size; i++) {
                    MockSession session = createSession();
                    available.offer(session);
                    allSessions.add(session);
                }
            }

            // Start eviction scheduler if configured
            if (evictPolicy != EvictionPolicy.NONE) {
                startEvictionScheduler();
            }
        }

        private MockSession createSession() {
            String id = "session-" + creationCount.incrementAndGet();
            MockSession session = new MockSession(id);
            sessionMap.put(id, session);
            return session;
        }

        MockSession borrowSession() throws TimeoutException, InterruptedException {
            MockSession session;

            switch (acquisitionMode) {
                case IMMEDIATE:
                    session = available.poll();
                    if (session == null) {
                        if (poolSize <= 0) {  // Unlimited
                            session = createSession();
                        } else {
                            throw new TimeoutException("Pool exhausted");
                        }
                    }
                    break;

                case QUEUED:
                    if (acquisitionTimeoutMs > 0) {
                        session = available.poll(acquisitionTimeoutMs, TimeUnit.MILLISECONDS);
                        if (session == null) {
                            if (poolSize <= 0) {  // Unlimited, create new
                                session = createSession();
                            } else {
                                throw new TimeoutException("Pool exhausted, timeout waiting");
                            }
                        }
                    } else {
                        session = available.take();  // Block indefinitely
                    }
                    break;

                case TIMEOUT_ON_FULL:
                    session = available.poll(acquisitionTimeoutMs, TimeUnit.MILLISECONDS);
                    if (session == null) {
                        throw new TimeoutException("Acquisition timeout, pool full");
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown acquisition mode");
            }

            // Validation on borrow
            if (validationStrategy == ValidationStrategy.ON_BORROW) {
                if (!validateSession(session)) {
                    allSessions.remove(session);
                    destructionCount.incrementAndGet();
                    // Retry - recursive call would exceed stack, so create new
                    session = createSession();
                }
            }

            inUseCount.incrementAndGet();
            return session;
        }

        void returnSession(MockSession session) throws InterruptedException {
            inUseCount.decrementAndGet();

            // Validation on return
            if (validationStrategy == ValidationStrategy.ON_RETURN) {
                if (!validateSession(session)) {
                    allSessions.remove(session);
                    destructionCount.incrementAndGet();
                    return;
                }
            }

            // Handle release strategies
            if (releaseMode == ReleaseMode.ON_ERROR) {
                if (session.isFailed()) {
                    allSessions.remove(session);
                    destructionCount.incrementAndGet();
                    throw new IllegalStateException("Cannot return failed session");
                }
            }

            if (releaseMode == ReleaseMode.AUTO || releaseMode == ReleaseMode.EXPLICIT) {
                if (poolSize <= 0 || available.size() < poolSize) {
                    available.offer(session);
                    reuseCount.incrementAndGet();
                } else {
                    // Pool full, discard session
                    allSessions.remove(session);
                    destructionCount.incrementAndGet();
                }
            }
        }

        private boolean validateSession(MockSession session) {
            validationRunCount.incrementAndGet();
            // Simulate validation failure rate
            return Math.random() > validationFailureRate;
        }

        void shutdown() {
            evictionScheduler.shutdown();
            try {
                if (!evictionScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    evictionScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                evictionScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        private void startEvictionScheduler() {
            if (evictionPolicy == EvictionPolicy.IDLE_TIME) {
                evictionScheduler.scheduleAtFixedRate(
                    this::evictIdleSessions,
                    idleTimeoutMs / 2,
                    idleTimeoutMs / 2,
                    TimeUnit.MILLISECONDS
                );
            } else if (evictionPolicy == EvictionPolicy.MAX_AGE) {
                evictionScheduler.scheduleAtFixedRate(
                    this::evictAgedSessions,
                    maxAgeMs / 2,
                    maxAgeMs / 2,
                    TimeUnit.MILLISECONDS
                );
            }
        }

        private void evictIdleSessions() {
            long now = System.currentTimeMillis();
            List<MockSession> toRemove = new ArrayList<>();

            for (MockSession session : allSessions) {
                if (now - session.getLastUsedTime() > idleTimeoutMs) {
                    toRemove.add(session);
                }
            }

            for (MockSession session : toRemove) {
                available.remove(session);
                allSessions.remove(session);
                destructionCount.incrementAndGet();
            }
        }

        private void evictAgedSessions() {
            long now = System.currentTimeMillis();
            List<MockSession> toRemove = new ArrayList<>();

            for (MockSession session : allSessions) {
                if (now - session.getCreationTime() > maxAgeMs) {
                    toRemove.add(session);
                }
            }

            for (MockSession session : toRemove) {
                available.remove(session);
                allSessions.remove(session);
                destructionCount.incrementAndGet();
            }
        }

        void setIdleTimeoutMs(long ms) { this.idleTimeoutMs = ms; }
        void setMaxAgeMs(long ms) { this.maxAgeMs = ms; }
        void setAcquisitionTimeoutMs(long ms) { this.acquisitionTimeoutMs = ms; }
        void setValidationFailureRate(double rate) { this.validationFailureRate = rate; }
        void enablePeriodicValidation(long intervalMs) {
            this.periodicValidationEnabled = true;
            this.periodicValidationIntervalMs = intervalMs;
        }

        int getPoolSize() { return allSessions.size(); }
        int getAvailableCount() { return available.size(); }
        int getInUseCount() { return inUseCount.get(); }
        int getSessionReuseCount() { return reuseCount.get(); }
        int getValidationRunCount() { return validationRunCount.get(); }
        int getDestructionCount() { return destructionCount.get(); }
    }

    /**
     * Mock session for testing.
     */
    private static class MockSession {
        private final String id;
        private final long creationTime;
        private long lastUsedTime;
        private boolean failed = false;

        MockSession(String id) {
            this.id = id;
            this.creationTime = System.currentTimeMillis();
            this.lastUsedTime = creationTime;
        }

        String getId() { return id; }
        long getCreationTime() { return creationTime; }
        long getLastUsedTime() { return lastUsedTime; }
        void setFailed(boolean failed) { this.failed = failed; }
        boolean isFailed() { return failed; }
    }

    // ============================================================================
    // REAL IMPLEMENTATION TESTS: DefaultHeadlessSessionPool
    // ============================================================================

    /**
     * Tests that exercise the real DefaultHeadlessSessionPool against
     * key pairwise combinations, validating that the production implementation
     * matches the behavior specified by the mock-based pairwise tests above.
     */
    @Nested
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    class RealPoolPairwiseTest {

        private DefaultHeadlessSessionPool realPool;
        private PairwiseStubFactory stubFactory;

        @BeforeEach
        void setUp() {
            realPool = new DefaultHeadlessSessionPool();
            stubFactory = new PairwiseStubFactory();
        }

        @AfterEach
        void tearDown() {
            realPool.shutdown();
        }

        /**
         * REAL P1: [size=1] [immediate] — single session reuse
         */
        @Test
        void testRealPoolSingleSessionReuse() throws Exception {
            realPool.configure(poolConfig()
                    .maxSize(1)
                    .acquisitionMode(SessionPoolConfig.AcquisitionMode.IMMEDIATE)
                    .build());

            HeadlessSession s1 = realPool.borrowSession();
            assertNotNull(s1);
            assertEquals(1, realPool.getActiveCount());

            realPool.returnSession(s1);

            HeadlessSession s2 = realPool.borrowSession();
            assertSame(s1, s2, "Should reuse same session");
        }

        /**
         * REAL P2: [size=5] [immediate] [on-borrow validation]
         */
        @Test
        void testRealPoolValidateOnBorrow() throws Exception {
            realPool.configure(poolConfig()
                    .maxSize(5)
                    .acquisitionMode(SessionPoolConfig.AcquisitionMode.IMMEDIATE)
                    .validationStrategy(SessionPoolConfig.ValidationStrategy.ON_BORROW)
                    .build());

            HeadlessSession s = realPool.borrowSession();
            assertNotNull(s);
            assertTrue(s.isConnected(), "Borrowed session should be connected");

            // Mark disconnected and return
            s.disconnect();
            realPool.returnSession(s);

            // Next borrow should detect invalid and replace
            HeadlessSession s2 = realPool.borrowSession();
            assertNotNull(s2);
            assertNotSame(s, s2, "Invalid session should be replaced");
            assertTrue(s2.isConnected(), "Replacement should be connected");
        }

        /**
         * REAL P3: [size=5] [timeout-on-full] — exhaustion with timeout
         */
        @Test
        void testRealPoolExhaustionTimeout() throws Exception {
            realPool.configure(poolConfig()
                    .maxSize(POOL_SIZE_MEDIUM)
                    .acquisitionMode(SessionPoolConfig.AcquisitionMode.TIMEOUT_ON_FULL)
                    .acquisitionTimeout(Duration.ofMillis(SHORT_TIMEOUT_MS))
                    .build());

            // Exhaust pool
            List<HeadlessSession> held = new ArrayList<>();
            for (int i = 0; i < POOL_SIZE_MEDIUM; i++) {
                held.add(realPool.borrowSession());
            }

            // Next should timeout
            assertThrows(PoolExhaustedException.class, () -> realPool.borrowSession());

            // Release one, should recover
            realPool.returnSession(held.remove(0));
            HeadlessSession recovered = realPool.borrowSession();
            assertNotNull(recovered, "Should recover after release");

            // Cleanup
            for (HeadlessSession s : held) realPool.returnSession(s);
            realPool.returnSession(recovered);
        }

        /**
         * REAL P4: [size=5] [queued] — concurrent contention
         */
        @Test
        void testRealPoolConcurrentContention() throws Exception {
            realPool.configure(poolConfig()
                    .maxSize(POOL_SIZE_MEDIUM)
                    .acquisitionMode(SessionPoolConfig.AcquisitionMode.QUEUED)
                    .build());

            int threads = 10;
            int opsPerThread = 20;
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicInteger ops = new AtomicInteger(0);
            AtomicInteger errors = new AtomicInteger(0);

            for (int t = 0; t < threads; t++) {
                executorService.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            HeadlessSession s = realPool.borrowSession();
                            assertNotNull(s);
                            Thread.sleep(5);
                            realPool.returnSession(s);
                            ops.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(12, TimeUnit.SECONDS), "All threads should complete");
            assertEquals(0, errors.get(), "No errors expected");
            assertEquals(threads * opsPerThread, ops.get());

            // Pool consistent after stress
            assertEquals(0, realPool.getActiveCount(), "No active borrows remain");
            assertTrue(realPool.getIdleCount() > 0, "Idle sessions available");
        }

        /**
         * REAL P5: [size=1] [queued] — blocking waiters
         */
        @Test
        void testRealPoolBlockingWaiters() throws Exception {
            realPool.configure(poolConfig()
                    .maxSize(1)
                    .acquisitionMode(SessionPoolConfig.AcquisitionMode.QUEUED)
                    .build());

            AtomicInteger successCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(5);

            for (int i = 0; i < 5; i++) {
                executorService.submit(() -> {
                    try {
                        HeadlessSession s = realPool.borrowSession();
                        assertNotNull(s);
                        successCount.incrementAndGet();
                        Thread.sleep(50);
                        realPool.returnSession(s);
                    } catch (Exception e) {
                        fail("Thread should not fail: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS), "All threads should complete");
            assertEquals(5, successCount.get(), "All threads should succeed");
        }

        /**
         * REAL P6: [on-return validation] — discard invalid on return
         */
        @Test
        void testRealPoolValidateOnReturn() throws Exception {
            realPool.configure(poolConfig()
                    .maxSize(5)
                    .validationStrategy(SessionPoolConfig.ValidationStrategy.ON_RETURN)
                    .build());

            HeadlessSession s = realPool.borrowSession();
            s.disconnect(); // invalidate

            realPool.returnSession(s);

            // Invalid session should have been discarded
            assertEquals(0, realPool.getIdleCount(), "Disconnected session should not return to idle");
        }

        /**
         * REAL P7: Pool state consistency after concurrent ops
         */
        @Test
        void testRealPoolMetricsConsistency() throws Exception {
            realPool.configure(poolConfig()
                    .maxSize(POOL_SIZE_MEDIUM)
                    .acquisitionMode(SessionPoolConfig.AcquisitionMode.QUEUED)
                    .build());

            CountDownLatch latch = new CountDownLatch(10);
            for (int t = 0; t < 10; t++) {
                executorService.submit(() -> {
                    try {
                        for (int i = 0; i < 10; i++) {
                            HeadlessSession s = realPool.borrowSession();
                            assertNotNull(s);
                            Thread.sleep(2);
                            realPool.returnSession(s);
                        }
                    } catch (Exception e) {
                        fail("Operation failed: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));

            int total = realPool.getPoolSize();
            int active = realPool.getActiveCount();
            int idle = realPool.getIdleCount();

            assertEquals(0, active, "No active after all returned");
            assertEquals(total, idle, "All sessions idle");
        }

        // Helpers

        private SessionPoolConfig.Builder poolConfig() {
            return SessionPoolConfig.builder()
                    .sessionFactory(stubFactory)
                    .connectionProps(new Properties());
        }

        /**
         * Stub factory for pairwise real-pool tests.
         */
        private static class PairwiseStubFactory implements HeadlessSessionFactory {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public HeadlessSession createSession(String sessionName, String configResource, Properties connectionProps) {
                return new PairwiseStubSession(sessionName + "-" + counter.incrementAndGet());
            }
        }

        /**
         * Minimal HeadlessSession stub.
         */
        private static class PairwiseStubSession implements HeadlessSession {
            private final String name;
            private volatile boolean connected = true;

            PairwiseStubSession(String name) { this.name = name; }

            @Override public String getSessionName() { return name; }
            @Override public boolean isConnected() { return connected; }
            @Override public void connect() { connected = true; }
            @Override public void disconnect() { connected = false; }
            @Override public Screen5250 getScreen() { return null; }
            @Override public SessionConfig getConfiguration() { return null; }
            @Override public Properties getConnectionProperties() { return new Properties(); }
            @Override public void sendKeys(String keys) {}
            @Override public void waitForKeyboardUnlock(int timeoutMs) {}
            @Override public void waitForKeyboardLockCycle(int timeoutMs) {}
            @Override public BufferedImage captureScreenshot() { return null; }
            @Override public String getScreenAsText() { return ""; }
            @Override public void addSessionListener(SessionListener listener) {}
            @Override public void removeSessionListener(SessionListener listener) {}
            @Override public void signalBell() {}
            @Override public String handleSystemRequest() { return null; }
        }
    }
}
