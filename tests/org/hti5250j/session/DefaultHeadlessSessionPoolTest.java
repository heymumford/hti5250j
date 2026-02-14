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
 * Uses a stub HeadlessSessionFactory that creates lightweight mock sessions
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
        AtomicInteger errors = new AtomicInteger(0);

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
                    errors.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(12, TimeUnit.SECONDS), "All threads should complete");
        assertEquals(0, errors.get(), "No errors during concurrent operations");

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
                } catch (Exception e) {
                    // ignore
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
    // Stub implementations
    // ========================================================================

    private SessionPoolConfig.Builder baseConfig() {
        return SessionPoolConfig.builder()
                .sessionFactory(factory)
                .connectionProps(new Properties());
    }

    /**
     * Stub factory that creates lightweight mock sessions (no real TN5250 connections).
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
     * Tracks connected state; all other methods are no-ops.
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
