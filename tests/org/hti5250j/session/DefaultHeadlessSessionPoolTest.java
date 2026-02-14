/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultHeadlessSessionPool}.
 *
 * Uses a stub HeadlessSessionFactory that creates lightweight stub sessions
 * (no real TN5250 connections) to test pool lifecycle, exhaustion, validation,
 * eviction, concurrency, and metrics.
 */
@Timeout(value = 15, unit = TimeUnit.SECONDS)
public class DefaultHeadlessSessionPoolTest {

    private DefaultHeadlessSessionPool pool;
    private StubSessionFactory factory;

    @BeforeEach
    void setUp() {
        pool = new DefaultHeadlessSessionPool();
        factory = new StubSessionFactory();
    }

    @AfterEach
    void tearDown() {
        pool.shutdown();
    }

    // ========================================================================
    // Lifecycle
    // ========================================================================

    @Test
    void testBorrowAndReturnLifecycle() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());

        HeadlessSession session = pool.borrowSession();
        assertNotNull(session);
        assertEquals(1, pool.getActiveCount());
        assertEquals(0, pool.getIdleCount());

        pool.returnSession(session);
        assertEquals(0, pool.getActiveCount());
        assertEquals(1, pool.getIdleCount());
    }

    @Test
    void testBorrowReusesReturnedSession() throws Exception {
        pool.configure(baseConfig().maxSize(1).build());

        HeadlessSession first = pool.borrowSession();
        pool.returnSession(first);

        HeadlessSession second = pool.borrowSession();
        assertSame(first, second, "Should reuse the same session object");
    }

    @Test
    void testPoolSizeGrowsOnDemand() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());

        List<HeadlessSession> sessions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            sessions.add(pool.borrowSession());
        }

        assertEquals(5, pool.getPoolSize());
        assertEquals(5, pool.getActiveCount());
        assertEquals(0, pool.getIdleCount());

        for (HeadlessSession s : sessions) {
            pool.returnSession(s);
        }
        assertEquals(0, pool.getActiveCount());
        assertEquals(5, pool.getIdleCount());
    }

    // ========================================================================
    // Pool Exhaustion
    // ========================================================================

    @Test
    void testImmediateModeThrowsOnExhaustion() throws Exception {
        pool.configure(baseConfig()
                .maxSize(2)
                .acquisitionMode(SessionPoolConfig.AcquisitionMode.IMMEDIATE)
                .build());

        pool.borrowSession();
        pool.borrowSession();

        assertThrows(PoolExhaustedException.class, () -> pool.borrowSession());
    }

    @Test
    void testTimeoutModeThrowsAfterTimeout() throws Exception {
        pool.configure(baseConfig()
                .maxSize(1)
                .acquisitionMode(SessionPoolConfig.AcquisitionMode.TIMEOUT_ON_FULL)
                .acquisitionTimeout(Duration.ofMillis(200))
                .build());

        pool.borrowSession(); // exhaust

        long start = System.currentTimeMillis();
        assertThrows(PoolExhaustedException.class, () -> pool.borrowSession());
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 150, "Should have waited ~200ms, was " + elapsed + "ms");
    }

    @Test
    void testExplicitTimeoutBorrow() throws Exception {
        pool.configure(baseConfig()
                .maxSize(1)
                .acquisitionMode(SessionPoolConfig.AcquisitionMode.IMMEDIATE)
                .build());

        pool.borrowSession(); // exhaust

        assertThrows(PoolExhaustedException.class,
                () -> pool.borrowSession(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void testQueuedModeBlocksUntilAvailable() throws Exception {
        pool.configure(baseConfig()
                .maxSize(1)
                .acquisitionMode(SessionPoolConfig.AcquisitionMode.QUEUED)
                .build());

        HeadlessSession held = pool.borrowSession();

        // Return the session after a short delay from another thread
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(200);
                pool.returnSession(held);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // This should block then succeed once the session is returned
        HeadlessSession borrowed = pool.borrowSession();
        assertNotNull(borrowed);
    }

    // ========================================================================
    // Validation
    // ========================================================================

    @Test
    void testValidateOnBorrowDiscardsDisconnectedSession() throws Exception {
        pool.configure(baseConfig()
                .maxSize(5)
                .validationStrategy(SessionPoolConfig.ValidationStrategy.ON_BORROW)
                .build());

        HeadlessSession session = pool.borrowSession();
        ((StubSession) session).setConnected(false); // mark as invalid
        pool.returnSession(session);

        // Next borrow should detect invalid session and create a new one
        HeadlessSession next = pool.borrowSession();
        assertNotNull(next);
        assertNotSame(session, next, "Invalid session should be replaced");
    }

    @Test
    void testValidateOnReturnDiscardsDisconnectedSession() throws Exception {
        pool.configure(baseConfig()
                .maxSize(5)
                .validationStrategy(SessionPoolConfig.ValidationStrategy.ON_RETURN)
                .build());

        HeadlessSession session = pool.borrowSession();
        ((StubSession) session).setConnected(false);
        pool.returnSession(session);

        // Disconnected session should not be in idle queue
        assertEquals(0, pool.getIdleCount(), "Invalid session should be discarded on return");
    }

    // ========================================================================
    // Eviction
    // ========================================================================

    @Test
    void testIdleTimeEvictionRemovesStaleSessions() throws Exception {
        pool.configure(baseConfig()
                .maxSize(5)
                .evictionPolicy(SessionPoolConfig.EvictionPolicy.IDLE_TIME)
                .maxIdleTime(Duration.ofMillis(500))
                .build());

        HeadlessSession session = pool.borrowSession();
        pool.returnSession(session);
        assertEquals(1, pool.getIdleCount());

        // Wait for eviction to run (interval = maxIdleTime/2 = 250ms, + margin)
        Thread.sleep(1200);

        assertEquals(0, pool.getIdleCount(), "Session should be evicted after idle timeout");
    }

    @Test
    void testMaxAgeEvictionRemovesOldSessions() throws Exception {
        pool.configure(baseConfig()
                .maxSize(5)
                .evictionPolicy(SessionPoolConfig.EvictionPolicy.MAX_AGE)
                .maxAge(Duration.ofMillis(500))
                .build());

        HeadlessSession session = pool.borrowSession();
        pool.returnSession(session);
        assertEquals(1, pool.getIdleCount());

        // Wait for age-based eviction
        Thread.sleep(1200);

        assertEquals(0, pool.getIdleCount(), "Old session should be evicted by max age");
    }

    // ========================================================================
    // Shutdown
    // ========================================================================

    @Test
    void testShutdownRejectsNewBorrows() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());
        pool.borrowSession();

        pool.shutdown();
        assertTrue(pool.isShutdown());

        assertThrows(PoolExhaustedException.class, () -> pool.borrowSession());
    }

    @Test
    void testShutdownDisconnectsAllSessions() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());

        List<HeadlessSession> sessions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            sessions.add(pool.borrowSession());
        }
        // Return two to idle
        pool.returnSession(sessions.get(0));
        pool.returnSession(sessions.get(1));

        pool.shutdown();

        // All stub sessions should have been disconnected
        for (HeadlessSession s : sessions) {
            assertFalse(((StubSession) s).isConnected(),
                    "Session should be disconnected after shutdown");
        }
    }

    @Test
    void testShutdownIdempotent() throws Exception {
        pool.configure(baseConfig().maxSize(1).build());
        pool.shutdown();
        pool.shutdown(); // should not throw
        assertTrue(pool.isShutdown());
    }

    @Test
    void testReturnSessionAfterShutdownDisconnects() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());
        HeadlessSession session = pool.borrowSession();

        pool.shutdown();
        pool.returnSession(session); // should not throw, just disconnect

        assertFalse(((StubSession) session).isConnected());
    }

    // ========================================================================
    // Concurrency
    // ========================================================================

    @Test
    void testConcurrentBorrowReturn() throws Exception {
        pool.configure(baseConfig()
                .maxSize(5)
                .acquisitionMode(SessionPoolConfig.AcquisitionMode.QUEUED)
                .build());

        int threadCount = 10;
        int opsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Exception> errors = new ConcurrentLinkedQueue<>();

        for (int t = 0; t < threadCount; t++) {
            Thread.ofVirtual().start(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        HeadlessSession s = pool.borrowSession();
                        Thread.sleep(1);
                        pool.returnSession(s);
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(12, TimeUnit.SECONDS), "All threads should complete");
        if (!errors.isEmpty()) {
            fail("Concurrent operations produced " + errors.size() + " error(s); first: " + errors.peek());
        }

        // Pool should be consistent
        assertEquals(0, pool.getActiveCount(), "No sessions should be borrowed");
        assertTrue(pool.getIdleCount() > 0, "Pool should have idle sessions");
        assertEquals(pool.getActiveCount() + pool.getIdleCount(), pool.getPoolSize());
    }

    // ========================================================================
    // Metrics
    // ========================================================================

    @Test
    void testBorrowAndReturnMetrics() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());

        HeadlessSession s1 = pool.borrowSession();
        HeadlessSession s2 = pool.borrowSession();
        pool.returnSession(s1);
        pool.returnSession(s2);
        pool.borrowSession();

        assertEquals(3, pool.getBorrowCount());
        assertEquals(2, pool.getReturnCount());
    }

    @Test
    void testPoolSizeMetrics() throws Exception {
        pool.configure(baseConfig().maxSize(10).build());

        assertEquals(0, pool.getPoolSize());
        assertEquals(0, pool.getActiveCount());
        assertEquals(0, pool.getIdleCount());

        HeadlessSession s = pool.borrowSession();
        assertEquals(1, pool.getPoolSize());
        assertEquals(1, pool.getActiveCount());
        assertEquals(0, pool.getIdleCount());

        pool.returnSession(s);
        assertEquals(1, pool.getPoolSize());
        assertEquals(0, pool.getActiveCount());
        assertEquals(1, pool.getIdleCount());
    }

    // ========================================================================
    // Configuration validation
    // ========================================================================

    @Test
    void testConfigureRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> pool.configure(null));
    }

    @Test
    void testReconfigureAfterShutdown() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());
        pool.borrowSession();
        pool.shutdown();
        assertTrue(pool.isShutdown());

        // Reconfigure should reset the pool to usable state
        pool.configure(baseConfig().maxSize(5).build());
        assertFalse(pool.isShutdown());

        HeadlessSession session = pool.borrowSession();
        assertNotNull(session, "Pool should work after reconfigure");
        pool.returnSession(session);
    }

    @Test
    void testReturnForeignSessionIsIgnored() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());

        // Create a session outside the pool
        HeadlessSession foreign = new StubSession("foreign-session");

        // Returning a foreign session should be silently ignored
        pool.returnSession(foreign);
        assertEquals(0, pool.getIdleCount(), "Foreign session should not enter idle queue");
        assertEquals(0, pool.getReturnCount(), "Foreign session should not count as a return");
    }

    @Test
    void testNullDurationRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                baseConfig().maxIdleTime(null));
        assertThrows(IllegalArgumentException.class, () ->
                baseConfig().maxAge(null));
        assertThrows(IllegalArgumentException.class, () ->
                baseConfig().validationInterval(null));
        assertThrows(IllegalArgumentException.class, () ->
                baseConfig().acquisitionTimeout(null));
    }

    @Test
    void testMaxSizeRespectedUnderConcurrency() throws Exception {
        int maxSize = 3;
        pool.configure(baseConfig()
                .maxSize(maxSize)
                .acquisitionMode(SessionPoolConfig.AcquisitionMode.IMMEDIATE)
                .build());

        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exhaustedCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            Thread.ofVirtual().start(() -> {
                try {
                    startLatch.await();
                    HeadlessSession s = pool.borrowSession();
                    successCount.incrementAndGet();
                    // Hold briefly
                    Thread.sleep(50);
                    pool.returnSession(s);
                } catch (PoolExhaustedException e) {
                    exhaustedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    exhaustedCount.incrementAndGet(); // count unexpected errors
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        // Pool size should never exceed maxSize
        assertTrue(pool.getPoolSize() <= maxSize,
                "Pool size " + pool.getPoolSize() + " should never exceed maxSize " + maxSize);
    }

    @Test
    void testMinIdlePreCreation() throws Exception {
        pool.configure(baseConfig()
                .maxSize(5)
                .minIdle(3)
                .build());

        assertEquals(3, pool.getIdleCount(), "Should pre-create minIdle sessions");
        assertEquals(3, pool.getPoolSize());
    }

    @Test
    void testUnlimitedPoolGrowsUnbounded() throws Exception {
        pool.configure(baseConfig()
                .maxSize(0) // unlimited
                .build());

        List<HeadlessSession> sessions = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            sessions.add(pool.borrowSession());
        }
        assertEquals(50, pool.getActiveCount());

        for (HeadlessSession s : sessions) {
            pool.returnSession(s);
        }
    }

    @Test
    void testBorrowBeforeConfigureThrows() {
        DefaultHeadlessSessionPool unconfigured = new DefaultHeadlessSessionPool();
        assertThrows(IllegalStateException.class, () -> unconfigured.borrowSession());
    }

    @Test
    void testBuilderRejectsNullFactory() {
        assertThrows(IllegalStateException.class, () ->
                SessionPoolConfig.builder().build());
    }

    @Test
    void testBuilderRejectsNegativeMaxSize() {
        assertThrows(IllegalArgumentException.class, () ->
                SessionPoolConfig.builder().sessionFactory(factory).maxSize(-1).build());
    }

    @Test
    void testBuilderRejectsMinIdleGreaterThanMaxSize() {
        assertThrows(IllegalArgumentException.class, () ->
                SessionPoolConfig.builder().sessionFactory(factory).maxSize(5).minIdle(6).build());
    }

    @Test
    void testValidateOnBorrowReplacesAllInvalidWithFresh() throws Exception {
        pool.configure(baseConfig()
                .maxSize(2)
                .validationStrategy(SessionPoolConfig.ValidationStrategy.ON_BORROW)
                .build());

        // Borrow both, mark invalid, return both
        HeadlessSession s1 = pool.borrowSession();
        HeadlessSession s2 = pool.borrowSession();
        ((StubSession) s1).setConnected(false);
        ((StubSession) s2).setConnected(false);
        pool.returnSession(s1);
        pool.returnSession(s2);

        // Pool should evict invalid sessions and create fresh replacements
        HeadlessSession fresh = pool.borrowSession();
        assertNotNull(fresh);
        assertNotSame(s1, fresh);
        assertNotSame(s2, fresh);
        assertTrue(fresh.isConnected(), "Fresh session should be connected");
        assertTrue(pool.getEvictionCount() >= 1, "Should have evicted at least one invalid session");
    }

    @Test
    void testReturnNullIsNoOp() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());
        HeadlessSession session = pool.borrowSession();

        pool.returnSession(null); // should not throw or change state

        assertEquals(1, pool.getActiveCount());
        assertEquals(0, pool.getIdleCount());
        assertEquals(1, pool.getPoolSize());

        pool.returnSession(session);
    }

    @Test
    void testEvictionCountMetricIncremented() throws Exception {
        pool.configure(baseConfig()
                .maxSize(5)
                .evictionPolicy(SessionPoolConfig.EvictionPolicy.IDLE_TIME)
                .maxIdleTime(Duration.ofMillis(500))
                .build());

        HeadlessSession session = pool.borrowSession();
        pool.returnSession(session);

        Thread.sleep(1200);

        assertTrue(pool.getEvictionCount() >= 1,
                "Eviction count should be at least 1 after idle eviction");
    }

    @Test
    void testQueuedBorrowInterrupted() throws Exception {
        pool.configure(baseConfig()
                .maxSize(1)
                .acquisitionMode(SessionPoolConfig.AcquisitionMode.QUEUED)
                .build());

        pool.borrowSession(); // exhaust pool

        Thread borrowThread = Thread.ofVirtual().start(() -> {
            assertThrows(InterruptedException.class, () -> pool.borrowSession());
        });

        Thread.sleep(100); // let the thread block
        borrowThread.interrupt();
        borrowThread.join(2000);
        assertFalse(borrowThread.isAlive(), "Thread should have completed after interrupt");
    }

    @Test
    void testAutoCloseableShutdown() throws Exception {
        DefaultHeadlessSessionPool autoPool = new DefaultHeadlessSessionPool();
        autoPool.configure(baseConfig().maxSize(1).build());
        autoPool.borrowSession();

        autoPool.close(); // should call shutdown
        assertTrue(autoPool.isShutdown());
    }

    // ========================================================================
    // Factory failure resilience
    // ========================================================================

    @Test
    void testFactoryExceptionLeavesPoolConsistent() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        HeadlessSessionFactory failingFactory = (name, configResource, props) -> {
            if (callCount.incrementAndGet() == 2) {
                throw new RuntimeException("simulated factory failure");
            }
            return new StubSession(name);
        };

        pool.configure(SessionPoolConfig.builder()
                .sessionFactory(failingFactory)
                .maxSize(5)
                .build());

        // First borrow succeeds
        HeadlessSession s1 = pool.borrowSession();
        assertNotNull(s1);
        pool.returnSession(s1);

        // Reuse s1 from idle, then trigger factory failure on the second create
        HeadlessSession reused = pool.borrowSession(); // reuses s1 from idle
        assertNotNull(reused);
        assertThrows(RuntimeException.class, () -> pool.borrowSession()); // factory call #2 throws
        pool.returnSession(reused); // return the reused session

        // Pool should still be usable — factory recovers on call #3
        HeadlessSession s3 = pool.borrowSession();
        assertNotNull(s3, "Pool should recover after factory failure");
    }

    // ========================================================================
    // Double-return safety
    // ========================================================================

    @Test
    void testDoubleReturnDoesNotCorruptPool() throws Exception {
        pool.configure(baseConfig().maxSize(1).build());

        HeadlessSession session = pool.borrowSession();
        pool.returnSession(session);
        pool.returnSession(session); // second return should be treated as foreign

        assertEquals(1, pool.getIdleCount(), "Idle count should be 1, not 2");
        assertEquals(1, pool.getPoolSize(), "Pool size should remain 1");

        // Borrow the session and verify pool is exhausted after one borrow
        HeadlessSession borrowed = pool.borrowSession();
        assertNotNull(borrowed);
        assertThrows(PoolExhaustedException.class, () -> pool.borrowSession());
    }

    // ========================================================================
    // Validation exhaustion at capacity
    // ========================================================================

    @Test
    void testValidateOnBorrowExhaustsWhenAllInvalidAndAtCapacity() throws Exception {
        pool.configure(baseConfig()
                .maxSize(2)
                .validationStrategy(SessionPoolConfig.ValidationStrategy.ON_BORROW)
                .build());

        // Borrow both, hold one, mark other invalid and return
        HeadlessSession held = pool.borrowSession();
        HeadlessSession toInvalidate = pool.borrowSession();
        ((StubSession) toInvalidate).setConnected(false);
        pool.returnSession(toInvalidate);

        // Pool has maxSize=2: 1 active (held) + 1 idle (invalid).
        // Borrow should evict invalid, then create a replacement (capacity allows 1 more)
        HeadlessSession replacement = pool.borrowSession();
        assertNotNull(replacement);
        assertNotSame(toInvalidate, replacement);

        // Now pool is at capacity (2 active, 0 idle). Next borrow should fail.
        assertThrows(PoolExhaustedException.class, () -> pool.borrowSession());

        pool.returnSession(held);
        pool.returnSession(replacement);
    }

    // ========================================================================
    // Reconfigure while sessions borrowed
    // ========================================================================

    @Test
    void testReconfigureWhileSessionsBorrowed() throws Exception {
        pool.configure(baseConfig().maxSize(5).build());

        HeadlessSession borrowed = pool.borrowSession();
        assertTrue(borrowed.isConnected());

        // Reconfigure disconnects all sessions (idle and borrowed)
        pool.configure(baseConfig().maxSize(5).build());

        assertFalse(((StubSession) borrowed).isConnected(),
                "Borrowed session should be disconnected after reconfigure");

        // Returning the disconnected session should be treated as foreign
        pool.returnSession(borrowed);
        assertEquals(0, pool.getIdleCount(), "Disconnected session should not re-enter pool");

        // Pool should be fully functional with new config
        HeadlessSession fresh = pool.borrowSession();
        assertNotNull(fresh);
        pool.returnSession(fresh);
    }

    // ========================================================================
    // Shutdown unblocks QUEUED borrowers
    // ========================================================================

    @Test
    void testShutdownUnblocksQueuedBorrower() throws Exception {
        pool.configure(baseConfig()
                .maxSize(1)
                .acquisitionMode(SessionPoolConfig.AcquisitionMode.QUEUED)
                .build());

        pool.borrowSession(); // exhaust

        AtomicReference<Exception> caught = new AtomicReference<>();
        Thread borrower = Thread.ofVirtual().start(() -> {
            try {
                pool.borrowSession(); // will block
            } catch (Exception e) {
                caught.set(e);
            }
        });

        Thread.sleep(200); // let borrower block
        pool.shutdown();
        borrower.join(5000);

        assertFalse(borrower.isAlive(), "Borrower thread should have been unblocked by shutdown");
        assertInstanceOf(PoolExhaustedException.class, caught.get(),
                "Should throw PoolExhaustedException on shutdown");
    }

    // ========================================================================
    // Builder null guard tests
    // ========================================================================

    @Test
    void testBuilderRejectsNullEnums() {
        assertThrows(IllegalArgumentException.class, () ->
                baseConfig().acquisitionMode(null));
        assertThrows(IllegalArgumentException.class, () ->
                baseConfig().validationStrategy(null));
        assertThrows(IllegalArgumentException.class, () ->
                baseConfig().evictionPolicy(null));
    }

    @Test
    void testBuilderRejectsNullConnectionProps() {
        assertThrows(IllegalArgumentException.class, () ->
                baseConfig().connectionProps(null));
    }

    @Test
    void testBuilderRejectsNullConfigResource() {
        assertThrows(IllegalArgumentException.class, () ->
                baseConfig().configResource(null));
    }

    // ========================================================================
    // Unconfigured pool edge case
    // ========================================================================

    @Test
    void testReturnSessionOnUnconfiguredPoolDisconnectsSession() {
        DefaultHeadlessSessionPool unconfigured = new DefaultHeadlessSessionPool();
        StubSession orphan = new StubSession("orphan");
        assertTrue(orphan.isConnected());

        unconfigured.returnSession(orphan);
        assertFalse(orphan.isConnected(), "Session should be disconnected when returned to unconfigured pool");
    }

    // ========================================================================
    // Factory failure during pre-creation
    // ========================================================================

    @Test
    void testFactoryFailureDuringPreCreationDoesNotPreventPoolStartup() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        HeadlessSessionFactory partialFactory = (name, configResource, props) -> {
            if (callCount.incrementAndGet() == 2) {
                throw new RuntimeException("simulated pre-creation failure");
            }
            return new StubSession(name);
        };

        pool.configure(SessionPoolConfig.builder()
                .sessionFactory(partialFactory)
                .maxSize(5)
                .minIdle(3) // requests 3, but #2 will fail
                .build());

        // Should have pre-created 2 of 3 (1 succeeded, 1 failed, 1 succeeded)
        assertEquals(2, pool.getIdleCount(), "Should have 2 pre-created sessions (1 failure)");

        // Pool should still be functional for on-demand borrows
        HeadlessSession s = pool.borrowSession();
        assertNotNull(s, "Pool should work despite partial pre-creation failure");
        pool.returnSession(s);
    }

    // ========================================================================
    // Builder null guard for sessionFactory
    // ========================================================================

    @Test
    void testBuilderRejectsNullSessionFactory() {
        assertThrows(IllegalArgumentException.class,
                () -> SessionPoolConfig.builder().sessionFactory(null));
    }

    // ========================================================================
    // Stub implementations
    // ========================================================================

    private SessionPoolConfig.Builder baseConfig() {
        return SessionPoolConfig.builder()
                .sessionFactory(factory)
                .connectionProps(new Properties());
    }

    /**
     * Stub factory that creates lightweight stub sessions (no real TN5250 connections).
     */
    private static class StubSessionFactory implements HeadlessSessionFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public HeadlessSession createSession(String sessionName, String configResource, Properties connectionProps) {
            return new StubSession(sessionName + "-" + counter.incrementAndGet());
        }
    }

    /**
     * Minimal HeadlessSession stub for pool testing.
     * Tracks connected state; all other methods return defaults (null, empty, or no-op).
     */
    private static class StubSession implements HeadlessSession {
        private final String name;
        private volatile boolean connected = true;

        StubSession(String name) {
            this.name = name;
        }

        void setConnected(boolean connected) {
            this.connected = connected;
        }

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
