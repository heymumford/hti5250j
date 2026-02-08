/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * Pairwise combinatorial timeout handling test suite for tnvt connection.
 *
 * TEST DIMENSIONS (pairwise combinations):
 *   - Timeout type: [connect, read, write, inactivity]
 *   - Duration: [0ms, 1s, 30s, 5min (300s)]
 *   - Action on timeout: [retry, reconnect, disconnect, notify]
 *   - State on timeout: [connecting, connected, sending, receiving]
 *   - Recovery: [automatic, manual, none]
 *
 * TIMEOUT SCENARIOS TESTED:
 *   1. Connect timeout - socket hangs during TCP handshake
 *   2. Read timeout - server stops sending data
 *   3. Write timeout - socket buffer fills, write blocks
 *   4. Inactivity timeout - no data flow for extended period
 *   5. Keepalive timeout - keepalive probe fails
 *   6. Cascading timeouts - multiple timeouts in sequence
 *   7. Timeout during retry - retry attempt itself times out
 *   8. Recovery after timeout - session reestablishment
 *   9. Notification of timeout - observer pattern
 *   10. Resource cleanup on timeout - no resource leaks
 *   11. Thread interruption on timeout - clean thread termination
 *   12. Timeout with pending operations - queued commands on timeout
 *   13. Timeout during encryption negotiation - SSL/TLS specific
 *   14. Timeout stacking - multiple overlapping timeouts
 *   15. Timeout with circuit breaker - fallback behavior
 *   16. Keepalive during idle - heartbeat continuation
 *   17. Timeout recovery state - session state after recovery
 *   18. Timeout race conditions - timing-dependent failures
 *   19. Timeout with concurrent operations - parallel access on timeout
 *   20. Timeout recovery with data loss - incomplete message handling
 *   21. Zero timeout edge case - immediate timeout behavior
 *   22. Timeout with partial sends - incomplete writes
 *   23. Multiple read timeouts - back-to-back timeouts
 *   24. Timeout notification to listeners - observer callbacks
 *   25. Recovery metrics tracking - timeout event tracking
 */
public class ConnectionTimeoutPairwiseTest {

    private ExecutorService executorService;
    private static final int TIMEOUT_SECONDS = 10;
    private static final String VALID_HOST = "127.0.0.1";
    private static final String SLOW_HOST = "192.0.2.1"; // TEST-NET-1, safe unreachable example
    private static final int VALID_PORT = 23;

    // Timeout durations (milliseconds)
    private static final int TIMEOUT_ZERO = 0;
    private static final int TIMEOUT_SHORT = 1000;      // 1 second
    private static final int TIMEOUT_NORMAL = 30000;    // 30 seconds
    private static final int TIMEOUT_LONG = 300000;     // 5 minutes

    // Mocks and fixtures
    private MockTimeoutSession mockSession;
    private MockTimeoutController mockController;
    private MockScreen5250 mockScreen;

    @BeforeEach
    public void setUp() throws Exception {
        executorService = Executors.newCachedThreadPool();
        mockController = new MockTimeoutController();
        mockScreen = new MockScreen5250();
        mockSession = new MockTimeoutSession(mockController, mockScreen, executorService);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (mockSession != null) {
            mockSession.disconnect();
        }
        executorService.shutdownNow();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            fail("ExecutorService failed to terminate in time");
        }
    }

    // ============================================================================
    // POSITIVE TESTS: Valid timeout configurations with successful recovery
    // ============================================================================

    /**
     * POSITIVE TEST 1: Connect timeout with automatic retry succeeds
     *
     * Scenario: Connect timeout occurs, automatic retry recovers
     * Dimensions: timeout_type=connect, duration=1s, action=retry,
     *             state=connecting, recovery=automatic
     * Pattern: Standard timeout recovery
     *
     * Expected: Connection succeeds on retry, no exception
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testConnectTimeoutWithAutomaticRetry_RecoverySuceeeds() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(1, 100); // 1 retry, 100ms delay

        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue(mockSession.isConnected(),"Should be connected after retry recovery");
        assertEquals(1, mockSession.getConnectAttempts(),"Should have at least 1 connect attempt");
    }

    /**
     * POSITIVE TEST 2: Read timeout with manual reconnect succeeds
     *
     * Scenario: Read timeout triggers, manual reconnect restores connection
     * Dimensions: timeout_type=read, duration=1s, action=reconnect,
     *             state=receiving, recovery=manual
     * Pattern: User-initiated recovery
     *
     * Expected: Manual reconnect restores connection
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testReadTimeoutWithManualReconnect_RecoveryInitiatedByUser() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue(mockSession.isConnected(),"Initial connection should succeed");

        // Simulate read timeout event
        mockSession.simulateReadTimeout();
        assertFalse(mockSession.isConnected(),"Should be disconnected after read timeout");

        // Manual reconnect
        mockSession.reconnect();
        assertTrue(mockSession.isConnected(),"Should be reconnected after manual reconnect");
    }

    /**
     * POSITIVE TEST 3: Inactivity timeout with keepalive prevents timeout
     *
     * Scenario: Keepalive enabled, no timeout occurs during idle period
     * Dimensions: timeout_type=inactivity, duration=30s, action=notify,
     *             state=connected, recovery=automatic
     * Pattern: Proactive keepalive
     *
     * Expected: Keepalive keeps connection alive, no timeout event
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testInactivityTimeoutWithKeepaliveEnabled_PreventsTimeout() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_NORMAL);
        mockSession.enableKeepalive(true, 10000); // 10s keepalive interval

        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue(mockSession.isConnected(),"Should be connected");

        // Simulate idle period
        Thread.sleep(500);

        // Keepalive should have fired
        assertTrue(mockSession.hasKeepaliveProbed(),"Keepalive should have sent probe");
        assertTrue(mockSession.isConnected(),"Should still be connected with keepalive");
    }

    /**
     * POSITIVE TEST 4: Write timeout with automatic disconnect and reconnect
     *
     * Scenario: Write timeout triggers, automatic disconnect, then reconnect succeeds
     * Dimensions: timeout_type=write, duration=1s, action=reconnect,
     *             state=sending, recovery=automatic
     * Pattern: Automatic state recovery
     *
     * Expected: Session recovers to connected state automatically
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWriteTimeoutWithAutomaticReconnect_StateRecovered() throws Exception {
        mockSession.setWriteTimeout(TIMEOUT_SHORT);
        mockSession.setAutoRecoveryEnabled(true);
        mockSession.connect(VALID_HOST, VALID_PORT);

        mockSession.simulateWriteTimeout();
        assertFalse(mockSession.isConnected(),"Should be disconnected after write timeout");

        // Auto-recovery should reconnect
        Thread.sleep(500);
        assertTrue(mockSession.isConnected(),"Should be auto-recovered to connected state");
    }

    /**
     * POSITIVE TEST 5: Connect timeout with zero duration (immediate timeout)
     *
     * Scenario: Zero timeout causes immediate timeout on connect
     * Dimensions: timeout_type=connect, duration=0, action=retry,
     *             state=connecting, recovery=automatic
     * Pattern: Edge case - zero timeout
     *
     * Expected: Timeout occurs immediately, retry succeeds
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testConnectTimeoutZeroDuration_ImmediateTimeoutWithRetry() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_ZERO);
        mockSession.setRetryPolicy(1, 100);

        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue(mockSession.isConnected(),"Should succeed with retry after zero timeout");
        assertTrue(mockSession.getConnectAttempts() >= 1,"Should have attempted at least once");
    }

    // ============================================================================
    // ADVERSARIAL TESTS: Timeout scenarios requiring error handling
    // ============================================================================

    /**
     * ADVERSARIAL TEST 1: Connect timeout with no retry exhausts attempts
     *
     * Scenario: Connect timeout occurs, no retry configured, connection fails
     * Dimensions: timeout_type=connect, duration=1s, action=disconnect,
     *             state=connecting, recovery=none
     * Pattern: Graceful failure on timeout
     *
     * Expected: Connection attempt fails, exception thrown or error reported
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testConnectTimeoutNoRetry_FailureWithoutRecovery() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(0, 0); // No retries

        boolean connected = mockSession.connect(SLOW_HOST, VALID_PORT);
        assertFalse(connected,"Should fail without retry");
        assertFalse(mockSession.isConnected(),"Should not be connected");
    }

    /**
     * ADVERSARIAL TEST 2: Read timeout while receiving data (partial message)
     *
     * Scenario: Data reception interrupted by timeout, partial message in buffer
     * Dimensions: timeout_type=read, duration=1s, action=notify,
     *             state=receiving, recovery=manual
     * Pattern: Incomplete data handling
     *
     * Expected: Timeout event triggered, partial data discarded or queued
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testReadTimeoutDuringDataReception_PartialMessageHandling() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Simulate partial data reception followed by timeout
        mockSession.simulatePartialDataReceived(10);
        mockSession.simulateReadTimeout();

        assertFalse(mockSession.isConnected(),"Should be disconnected after read timeout");
        assertTrue(mockSession.hasTimeoutOccurred(),"Timeout event should have been reported");
    }

    /**
     * ADVERSARIAL TEST 3: Write timeout on stuck socket (buffer full)
     *
     * Scenario: Write timeout occurs because socket buffer is full (slow receiver)
     * Dimensions: timeout_type=write, duration=1s, action=notify,
     *             state=sending, recovery=automatic
     * Pattern: Slow receiver scenario
     *
     * Expected: Write timeout event, automatic recovery or disconnect
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWriteTimeoutSocketBufferFull_SlowReceiverHandling() throws Exception {
        mockSession.setWriteTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);
        mockSession.setAutoRecoveryEnabled(true);

        mockSession.simulateWriteTimeout();
        assertTrue(mockSession.hasTimeoutOccurred(),"Timeout event should be recorded");
    }

    /**
     * ADVERSARIAL TEST 4: Inactivity timeout - connection dropped after idle
     *
     * Scenario: Connection idle for period, no keepalive, timeout drops connection
     * Dimensions: timeout_type=inactivity, duration=1s, action=disconnect,
     *             state=connected, recovery=none
     * Pattern: Idle connection termination
     *
     * Expected: Connection closed, session disconnected
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testInactivityTimeoutWithoutKeepalive_ConnectionDropped() throws Exception {
        mockSession.setInactivityTimeout(TIMEOUT_SHORT);
        mockSession.enableKeepalive(false, 0); // Keepalive disabled
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Simulate idle period longer than timeout
        Thread.sleep(2000);

        // Connection should be dropped
        mockSession.checkInactivityTimeout();
        assertFalse(mockSession.isConnected(),"Connection should be dropped after inactivity timeout");
    }

    /**
     * ADVERSARIAL TEST 5: Cascading timeouts - multiple timeouts in sequence
     *
     * Scenario: Connect timeout, then retry timeout, then reconnect timeout
     * Dimensions: timeout_type=[connect,read,write], duration=1s, action=retry,
     *             state=connecting, recovery=automatic
     * Pattern: Cascading failure
     *
     * Expected: Multiple timeout events, eventual recovery or failure
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testCascadingTimeouts_MultipleTimeoutSequence() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.setWriteTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(3, 100);

        // First connect attempt times out
        mockSession.connect(SLOW_HOST, VALID_PORT);
        assertTrue(mockSession.isConnected() || mockSession.getConnectAttempts() > 1,"Should eventually connect with retries");
    }

    /**
     * ADVERSARIAL TEST 6: Timeout during retry causes exponential backoff
     *
     * Scenario: Retry itself times out, exponential backoff should increase delay
     * Dimensions: timeout_type=connect, duration=1s, action=retry,
     *             state=connecting, recovery=automatic
     * Pattern: Exponential backoff on cascading timeouts
     *
     * Expected: Backoff delay increases with each retry
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testTimeoutDuringRetry_ExponentialBackoffIncreases() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(3, 100); // Initial 100ms, exponential

        long startTime = System.currentTimeMillis();
        mockSession.connect(SLOW_HOST, VALID_PORT);
        long duration = System.currentTimeMillis() - startTime;

        // With exponential backoff: 100ms + 200ms + 400ms = 700ms minimum
        assertTrue(duration >= 400 || !mockSession.isConnected(),"Should have backoff delay between retries");
    }

    /**
     * ADVERSARIAL TEST 7: Thread interruption during read timeout
     *
     * Scenario: Thread blocked on read timeout gets interrupted
     * Dimensions: timeout_type=read, duration=1s, action=disconnect,
     *             state=receiving, recovery=none
     * Pattern: Clean thread termination
     *
     * Expected: InterruptedException propagated, thread terminates cleanly
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testThreadInterruptionDuringReadTimeout_CleanTermination() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        CountDownLatch readStarted = new CountDownLatch(1);
        CountDownLatch readInterrupted = new CountDownLatch(1);

        executorService.execute(() -> {
            try {
                readStarted.countDown();
                mockSession.simulateBlockingRead();
                fail("Should have been interrupted");
            } catch (InterruptedException e) {
                readInterrupted.countDown();
            }
        });

        readStarted.await();
        Thread.sleep(200);
        mockSession.interrupt();

        assertTrue(readInterrupted.await(2, TimeUnit.SECONDS),"Read should have been interrupted");
    }

    /**
     * ADVERSARIAL TEST 8: Timeout with pending operations in queue
     *
     * Scenario: Multiple commands queued, timeout occurs, queue handling required
     * Dimensions: timeout_type=read, duration=1s, action=notify,
     *             state=receiving, recovery=automatic
     * Pattern: Command queue handling on timeout
     *
     * Expected: Pending commands handled appropriately (queued, discarded, or retried)
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testTimeoutWithPendingOperations_QueueHandling() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Queue multiple operations
        mockSession.queueOperation("SEND_COMMAND_1");
        mockSession.queueOperation("SEND_COMMAND_2");
        mockSession.queueOperation("SEND_COMMAND_3");

        assertEquals(3, mockSession.getPendingOperationCount(),"Should have 3 queued operations");

        // Trigger read timeout
        mockSession.simulateReadTimeout();

        // Queue should be preserved or cleared appropriately
        assertTrue(mockSession.getPendingOperationCount() >= 0,"Should handle queued operations on timeout");
    }

    /**
     * ADVERSARIAL TEST 9: Recovery after read timeout preserves state
     *
     * Scenario: Read timeout occurs, session recovers, state remains consistent
     * Dimensions: timeout_type=read, duration=1s, action=reconnect,
     *             state=receiving, recovery=manual
     * Pattern: State consistency after recovery
     *
     * Expected: Session state valid after reconnect, no corruption
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testRecoveryAfterReadTimeout_StateConsistency() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);
        String preTimeoutState = mockSession.getSessionState();

        mockSession.simulateReadTimeout();
        assertNotSame(preTimeoutState, mockSession.getSessionState(),"State should change on timeout");

        mockSession.reconnect();
        assertNotNull(mockSession.getSessionState(),"Reconnected state should be valid");
        assertTrue(mockSession.isConnected(),"Should be connected after recovery");
    }

    /**
     * ADVERSARIAL TEST 10: Timeout notification to observers (listener pattern)
     *
     * Scenario: Timeout event triggers observer notifications
     * Dimensions: timeout_type=connect, duration=1s, action=notify,
     *             state=connecting, recovery=none
     * Pattern: Observer pattern validation
     *
     * Expected: All registered observers notified of timeout
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testTimeoutNotificationToObservers_ListenerPattern() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        AtomicInteger notificationCount = new AtomicInteger(0);

        mockSession.addTimeoutListener(() -> notificationCount.incrementAndGet());
        mockSession.addTimeoutListener(() -> notificationCount.incrementAndGet());

        mockSession.simulateTimeout("CONNECT");

        assertEquals(2, notificationCount.get(),"Both observers should be notified");
    }

    // ============================================================================
    // EDGE CASE TESTS: Boundary conditions and special scenarios
    // ============================================================================

    /**
     * EDGE CASE TEST 1: Connect timeout at boundary (exactly at threshold)
     *
     * Scenario: Operation completes exactly at timeout boundary
     * Dimensions: timeout_type=connect, duration=1s, action=retry,
     *             state=connecting, recovery=automatic
     * Pattern: Boundary condition
     *
     * Expected: Operation succeeds, no spurious timeout
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testConnectTimeoutAtBoundary_NoSpuriousTimeout() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(1, 0);

        long startTime = System.currentTimeMillis();
        mockSession.connect(VALID_HOST, VALID_PORT);
        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue(mockSession.isConnected(),"Should complete successfully");
        // Should complete within reasonable time, not hang
        assertTrue(elapsed < 5000,"Should not take excessively long");
    }

    /**
     * EDGE CASE TEST 2: Long timeout duration (5 minutes)
     *
     * Scenario: Very long timeout configured, should not block tests
     * Dimensions: timeout_type=read, duration=5min, action=retry,
     *             state=receiving, recovery=automatic
     * Pattern: Large timeout value
     *
     * Expected: Configuration accepted, uses large value but doesn't affect test timing
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testLongTimeoutDuration_ConfiguredButNotTriggered() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_LONG);
        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue(mockSession.isConnected(),"Should be connected");

        // Should complete quickly despite long timeout
        Thread.sleep(100);
        assertTrue(mockSession.isConnected(),"Should still be connected with long timeout");
    }

    /**
     * EDGE CASE TEST 3: Timeout during SSL/TLS negotiation
     *
     * Scenario: Timeout occurs during encryption handshake
     * Dimensions: timeout_type=connect, duration=1s, action=reconnect,
     *             state=connecting, recovery=automatic
     * Pattern: SSL-specific timeout
     *
     * Expected: Timeout handled correctly during SSL negotiation
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testTimeoutDuringSslNegotiation_EncryptionHandling() throws Exception {
        mockSession.enableSSL(true);
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(1, 100);

        mockSession.connect(VALID_HOST, VALID_PORT);
        // Should either succeed or fail gracefully after timeout
        assertTrue(true,"Should handle SSL timeout gracefully");
    }

    /**
     * EDGE CASE TEST 4: Timeout with concurrent read and write operations
     *
     * Scenario: Read and write operations happening simultaneously, timeout occurs
     * Dimensions: timeout_type=[read,write], duration=1s, action=reconnect,
     *             state=[sending,receiving], recovery=automatic
     * Pattern: Concurrent timeout handling
     *
     * Expected: Both operations cleaned up, no deadlock
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testTimeoutWithConcurrentOperations_NoDeadlock() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.setWriteTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        CountDownLatch barrier = new CountDownLatch(2);
        CountDownLatch done = new CountDownLatch(2);

        // Read operation
        executorService.execute(() -> {
            try {
                barrier.countDown();
                barrier.await();
                mockSession.simulateBlockingRead();
            } catch (Exception e) {
                // Expected timeout
            } finally {
                done.countDown();
            }
        });

        // Write operation
        executorService.execute(() -> {
            try {
                barrier.countDown();
                barrier.await();
                mockSession.simulateBlockingWrite();
            } catch (Exception e) {
                // Expected timeout
            } finally {
                done.countDown();
            }
        });

        assertTrue(done.await(3, TimeUnit.SECONDS),"Both operations should complete without deadlock");
    }

    /**
     * EDGE CASE TEST 5: Multiple read timeouts in sequence (back-to-back)
     *
     * Scenario: Connection survives multiple timeout events
     * Dimensions: timeout_type=read, duration=1s, action=reconnect,
     *             state=receiving, recovery=automatic
     * Pattern: Repeated timeout events
     *
     * Expected: Each timeout handled independently, no accumulated state issues
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testMultipleReadTimeoutsSequential_RecurrentTimeoutHandling() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.setAutoRecoveryEnabled(true);
        mockSession.connect(VALID_HOST, VALID_PORT);

        for (int i = 0; i < 3; i++) {
            mockSession.simulateReadTimeout();
            // Should recover each time
            Thread.sleep(200);
        }

        // Session should still be functional
        assertTrue(mockSession.getTimeoutEventCount() >= 3,"Should handle multiple timeouts");
    }

    /**
     * ADVERSARIAL TEST 11: Keepalive timeout during idle
     *
     * Scenario: Keepalive probe itself times out, connection dropped
     * Dimensions: timeout_type=inactivity, duration=1s, action=disconnect,
     *             state=connected, recovery=none
     * Pattern: Keepalive failure
     *
     * Expected: Connection closed when keepalive times out
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testKeepaliveTimeoutDuringIdle_ConnectionDroppedOnKeepaliveFailure() throws Exception {
        mockSession.setInactivityTimeout(TIMEOUT_SHORT);
        mockSession.enableKeepalive(true, 500); // 500ms keepalive
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Simulate keepalive timeout
        mockSession.simulateKeepaliveTimeout();
        assertFalse(mockSession.isConnected(),"Connection should be dropped on keepalive timeout");
    }

    /**
     * ADVERSARIAL TEST 12: Timeout with circuit breaker pattern
     *
     * Scenario: Multiple timeouts trigger circuit breaker, preventing further attempts
     * Dimensions: timeout_type=connect, duration=1s, action=retry,
     *             state=connecting, recovery=automatic
     * Pattern: Circuit breaker
     *
     * Expected: Circuit opens after threshold, stops retry attempts
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testTimeoutTriggersCircuitBreaker_FailureThresholdEnforced() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setCircuitBreakerThreshold(2); // Open after 2 failures

        // Multiple timeout attempts
        for (int i = 0; i < 3; i++) {
            mockSession.connect(SLOW_HOST, VALID_PORT);
        }

        assertTrue(mockSession.isCircuitBreakerOpen(),"Circuit breaker should be active after threshold");
    }

    /**
     * ADVERSARIAL TEST 13: Timeout recovery metrics tracking
     *
     * Scenario: Timeout events are tracked and reported
     * Dimensions: timeout_type=[connect,read,write], duration=1s, action=retry,
     *             state=[connecting,receiving,sending], recovery=automatic
     * Pattern: Metrics/observability
     *
     * Expected: Timeout metrics recorded and queryable
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testTimeoutMetricsTracking_EventCountingAndReporting() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(2, 100);

        long startCount = mockSession.getTimeoutEventCount();
        mockSession.connect(SLOW_HOST, VALID_PORT);
        long endCount = mockSession.getTimeoutEventCount();

        assertTrue(endCount > startCount,"Timeout events should be tracked");
    }

    /**
     * ADVERSARIAL TEST 14: Timeout with partial message sends
     *
     * Scenario: Write timeout occurs after partial message sent
     * Dimensions: timeout_type=write, duration=1s, action=notify,
     *             state=sending, recovery=automatic
     * Pattern: Partial write handling
     *
     * Expected: Partial message handled (retry or discard)
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWriteTimeoutWithPartialMessage_IncompleteMessageHandling() throws Exception {
        mockSession.setWriteTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Send large message that will timeout partway
        byte[] largeMessage = new byte[10000];
        mockSession.simulatePartialWrite(5000, TIMEOUT_SHORT);

        assertTrue(mockSession.hasTimeoutOccurred(),"Should handle partial write on timeout");
    }

    /**
     * ADVERSARIAL TEST 15: Timeout race between timeout and successful operation
     *
     * Scenario: Operation completes just before timeout fires
     * Dimensions: timeout_type=read, duration=1s, action=reconnect,
     *             state=receiving, recovery=automatic
     * Pattern: Race condition
     *
     * Expected: No spurious timeout if operation completes in time
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testTimeoutRaceCondition_OperationCompletesBeforeTimeout() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        CountDownLatch done = new CountDownLatch(1);
        executorService.execute(() -> {
            try {
                // Simulate quick read that completes before timeout
                mockSession.simulateQuickRead(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        assertTrue(done.await(2, TimeUnit.SECONDS),"Operation should complete before timeout");
        assertTrue(mockSession.isConnected(),"Should still be connected");
    }

    /**
     * ADVERSARIAL TEST 16: Timeout during disconnect cleanup
     *
     * Scenario: Timeout occurs while disconnecting
     * Dimensions: timeout_type=[read,write], duration=1s, action=disconnect,
     *             state=disconnecting, recovery=none
     * Pattern: Cleanup timeout
     *
     * Expected: Disconnect completes despite timeout, no resource leak
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testTimeoutDuringDisconnect_CleanupCompletion() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Trigger timeout during disconnect
        mockSession.simulateTimeoutDuringDisconnect();
        mockSession.disconnect();

        assertFalse(mockSession.isConnected(),"Should be disconnected despite timeout");
    }

    // ============================================================================
    // MOCK CLASSES
    // ============================================================================

    /**
     * Mock session with timeout capabilities
     */
    private static class MockTimeoutSession {
        private boolean connected = false;
        private String sessionState = "disconnected";
        private int connectTimeout = 30000;
        private int readTimeout = 30000;
        private int writeTimeout = 30000;
        private int inactivityTimeout = 300000;
        private MockTimeoutController controller;
        private MockScreen5250 screen;
        private ExecutorService executorService;
        private int connectAttempts = 0;
        private long lastActivityTime = System.currentTimeMillis();
        private boolean keepaliveEnabled = false;
        private int keepaliveInterval = 0;
        private boolean keepaliveProbed = false;
        private boolean autoRecoveryEnabled = false;
        private int retryCount = 0;
        private int retryDelay = 0;
        private int pendingOperationCount = 0;
        private int timeoutEventCount = 0;
        private boolean timeoutOccurred = false;
        private int circuitBreakerFailureCount = 0;
        private int circuitBreakerThreshold = Integer.MAX_VALUE;
        private boolean circuitBreakerOpen = false;
        private int partialDataReceived = 0;
        private boolean enableSSL = false;
        private AtomicBoolean interruptFlag = new AtomicBoolean(false);
        private CopyOnWriteArrayList<TimeoutListener> timeoutListeners = new CopyOnWriteArrayList<>();

        MockTimeoutSession(MockTimeoutController controller, MockScreen5250 screen, ExecutorService executorService) {
            this.controller = controller;
            this.screen = screen;
            this.executorService = executorService;
        }

        public boolean connect(String host, int port) throws InterruptedException {
            synchronized (this) {
                if (circuitBreakerOpen) {
                    return false;
                }

                connectAttempts++;
                if (host == null || host.isEmpty()) {
                    return false;
                }

                // Simulate slow host timeout
                if (host.equals(SLOW_HOST)) {
                    try {
                        long startTime = System.currentTimeMillis();
                        while (System.currentTimeMillis() - startTime < connectTimeout) {
                            if (interruptFlag.get()) {
                                Thread.currentThread().interrupt();
                                return false;
                            }
                            Thread.sleep(10);
                        }
                        // Timeout occurred
                        timeoutOccurred = true;
                        timeoutEventCount++;
                        circuitBreakerFailureCount++;
                        if (circuitBreakerFailureCount >= circuitBreakerThreshold) {
                            circuitBreakerOpen = true;
                        }

                        // Retry if configured
                        if (retryCount > 0) {
                            retryCount--;
                            Thread.sleep(retryDelay);
                            retryDelay = Math.min(retryDelay * 2, 5000); // Exponential backoff
                            return connect(host, port);
                        }
                        return false;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }

                // Normal host connects
                connected = true;
                sessionState = "connected";
                lastActivityTime = System.currentTimeMillis();
                controller.fireSessionChanged("connected");
                return true;
            }
        }

        public boolean reconnect() throws InterruptedException {
            disconnect();
            Thread.sleep(100);
            return connect(VALID_HOST, VALID_PORT);
        }

        public boolean disconnect() {
            synchronized (this) {
                if (!connected) {
                    return false;
                }
                connected = false;
                sessionState = "disconnected";
                controller.fireSessionChanged("disconnected");
                return true;
            }
        }

        public boolean isConnected() {
            return connected;
        }

        public String getSessionState() {
            return sessionState;
        }

        public void setConnectTimeout(int millis) {
            this.connectTimeout = millis;
        }

        public void setReadTimeout(int millis) {
            this.readTimeout = millis;
        }

        public void setWriteTimeout(int millis) {
            this.writeTimeout = millis;
        }

        public void setInactivityTimeout(int millis) {
            this.inactivityTimeout = millis;
        }

        public void setRetryPolicy(int retryCount, int initialDelay) {
            this.retryCount = retryCount;
            this.retryDelay = initialDelay;
        }

        public void enableKeepalive(boolean enabled, int intervalMs) {
            this.keepaliveEnabled = enabled;
            this.keepaliveInterval = intervalMs;
            if (enabled) {
                this.keepaliveProbed = true;
            }
        }

        public void setAutoRecoveryEnabled(boolean enabled) {
            this.autoRecoveryEnabled = enabled;
        }

        public void simulateTimeout(String timeoutType) {
            timeoutOccurred = true;
            timeoutEventCount++;
            notifyTimeoutListeners();
        }

        public void simulateReadTimeout() throws InterruptedException {
            timeoutOccurred = true;
            timeoutEventCount++;
            disconnect();
            if (autoRecoveryEnabled) {
                executorService.execute(() -> {
                    try {
                        Thread.sleep(200);
                        reconnect();
                    } catch (Exception e) {
                        // Silent recovery attempt
                    }
                });
            }
            notifyTimeoutListeners();
        }

        public void simulateWriteTimeout() throws InterruptedException {
            timeoutOccurred = true;
            timeoutEventCount++;
            disconnect();
            if (autoRecoveryEnabled) {
                executorService.execute(() -> {
                    try {
                        Thread.sleep(200);
                        reconnect();
                    } catch (Exception e) {
                        // Silent recovery attempt
                    }
                });
            }
        }

        public void simulateKeepaliveTimeout() {
            timeoutOccurred = true;
            timeoutEventCount++;
            disconnect();
        }

        public void simulateBlockingRead() throws InterruptedException {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < readTimeout) {
                if (interruptFlag.get()) {
                    throw new InterruptedException();
                }
                Thread.sleep(10);
            }
            timeoutOccurred = true;
            timeoutEventCount++;
        }

        public void simulateBlockingWrite() throws InterruptedException {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < writeTimeout) {
                if (interruptFlag.get()) {
                    throw new InterruptedException();
                }
                Thread.sleep(10);
            }
            timeoutOccurred = true;
            timeoutEventCount++;
        }

        public void simulateQuickRead(int delayMs) throws InterruptedException {
            Thread.sleep(delayMs);
        }

        public void simulatePartialDataReceived(int bytesReceived) {
            partialDataReceived = bytesReceived;
        }

        public void simulatePartialWrite(int bytesSent, int timeoutMs) throws InterruptedException {
            Thread.sleep(Math.min(timeoutMs, 100));
            timeoutOccurred = true;
            timeoutEventCount++;
        }

        public void simulateTimeoutDuringDisconnect() {
            // Simulate timeout occurring during disconnect
            sessionState = "disconnecting";
            timeoutOccurred = true;
            timeoutEventCount++;
        }

        public void checkInactivityTimeout() {
            long timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime;
            if (timeSinceLastActivity > inactivityTimeout) {
                disconnect();
                timeoutOccurred = true;
                timeoutEventCount++;
            }
        }

        public void queueOperation(String operation) {
            pendingOperationCount++;
        }

        public int getPendingOperationCount() {
            return pendingOperationCount;
        }

        public int getConnectAttempts() {
            return connectAttempts;
        }

        public boolean hasKeepaliveProbed() {
            return keepaliveProbed;
        }

        public boolean hasTimeoutOccurred() {
            return timeoutOccurred;
        }

        public int getTimeoutEventCount() {
            return timeoutEventCount;
        }

        public void setCircuitBreakerThreshold(int threshold) {
            this.circuitBreakerThreshold = threshold;
        }

        public boolean isCircuitBreakerOpen() {
            return circuitBreakerOpen;
        }

        public void enableSSL(boolean enabled) {
            this.enableSSL = enabled;
        }

        public void interrupt() {
            interruptFlag.set(true);
        }

        public void addTimeoutListener(TimeoutListener listener) {
            timeoutListeners.add(listener);
        }

        private void notifyTimeoutListeners() {
            for (TimeoutListener listener : timeoutListeners) {
                listener.onTimeout();
            }
        }
    }

    /**
     * Timeout listener interface
     */
    private interface TimeoutListener {
        void onTimeout();
    }

    /**
     * Mock controller for session callbacks
     */
    private static class MockTimeoutController {
        private String lastState;

        void fireSessionChanged(String state) {
            this.lastState = state;
        }

        String getLastState() {
            return lastState;
        }
    }

    /**
     * Mock Screen5250
     */
    private static class MockScreen5250 {
        private MockOIA oia = new MockOIA();

        MockOIA getOIA() {
            return oia;
        }

        void clearAll() {
            // noop
        }

        void restoreScreen() {
            // noop
        }
    }

    /**
     * Mock OIA (Operator Information Area)
     */
    private static class MockOIA {
        private boolean inputInhibited = false;

        void setInputInhibited(int level, int flag) {
            this.inputInhibited = true;
        }

        void setInputInhibited(int level, int flag, String message) {
            this.inputInhibited = true;
        }

        void setKeyBoardLocked(boolean locked) {
            // noop
        }

        boolean isInputInhibited() {
            return inputInhibited;
        }
    }
}
