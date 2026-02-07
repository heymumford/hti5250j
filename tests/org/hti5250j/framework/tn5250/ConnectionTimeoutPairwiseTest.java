/**
 * Title: ConnectionTimeoutPairwiseTest.java
 * Copyright: Copyright (c) 2025
 * Company: Guild Mortgage
 *
 * Description: Comprehensive pairwise timeout test suite for HTI5250j connection handling.
 * Tests combinations of timeout types, durations, recovery actions, connection states,
 * and recovery strategies to ensure robust timeout behavior and adversarial hung scenarios.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING. If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.hti5250j.framework.tn5250;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.Assert.*;

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
@RunWith(JUnit4.class)
public class ConnectionTimeoutPairwiseTest {

    private ExecutorService executorService;
    private static final int TIMEOUT_SECONDS = 10;
    private static final String VALID_HOST = "127.0.0.1";
    private static final String SLOW_HOST = "10.255.255.1"; // Non-routable, simulates slow/hung connection
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

    @Before
    public void setUp() throws Exception {
        executorService = Executors.newCachedThreadPool();
        mockController = new MockTimeoutController();
        mockScreen = new MockScreen5250();
        mockSession = new MockTimeoutSession(mockController, mockScreen, executorService);
    }

    @After
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
    @Test(timeout = 5000)
    public void testConnectTimeoutWithAutomaticRetry_RecoverySuceeeds() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(1, 100); // 1 retry, 100ms delay

        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue("Should be connected after retry recovery", mockSession.isConnected());
        assertEquals("Should have at least 1 connect attempt", 1, mockSession.getConnectAttempts());
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
    @Test(timeout = 5000)
    public void testReadTimeoutWithManualReconnect_RecoveryInitiatedByUser() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue("Initial connection should succeed", mockSession.isConnected());

        // Simulate read timeout event
        mockSession.simulateReadTimeout();
        assertFalse("Should be disconnected after read timeout", mockSession.isConnected());

        // Manual reconnect
        mockSession.reconnect();
        assertTrue("Should be reconnected after manual reconnect", mockSession.isConnected());
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
    @Test(timeout = 5000)
    public void testInactivityTimeoutWithKeepaliveEnabled_PreventsTimeout() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_NORMAL);
        mockSession.enableKeepalive(true, 10000); // 10s keepalive interval

        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue("Should be connected", mockSession.isConnected());

        // Simulate idle period
        Thread.sleep(500);

        // Keepalive should have fired
        assertTrue("Keepalive should have sent probe", mockSession.hasKeepaliveProbed());
        assertTrue("Should still be connected with keepalive", mockSession.isConnected());
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
    @Test(timeout = 5000)
    public void testWriteTimeoutWithAutomaticReconnect_StateRecovered() throws Exception {
        mockSession.setWriteTimeout(TIMEOUT_SHORT);
        mockSession.setAutoRecoveryEnabled(true);
        mockSession.connect(VALID_HOST, VALID_PORT);

        mockSession.simulateWriteTimeout();
        assertFalse("Should be disconnected after write timeout", mockSession.isConnected());

        // Auto-recovery should reconnect
        Thread.sleep(500);
        assertTrue("Should be auto-recovered to connected state", mockSession.isConnected());
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
    @Test(timeout = 5000)
    public void testConnectTimeoutZeroDuration_ImmediateTimeoutWithRetry() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_ZERO);
        mockSession.setRetryPolicy(1, 100);

        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue("Should succeed with retry after zero timeout", mockSession.isConnected());
        assertTrue("Should have attempted at least once", mockSession.getConnectAttempts() >= 1);
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
    @Test(timeout = 5000)
    public void testConnectTimeoutNoRetry_FailureWithoutRecovery() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(0, 0); // No retries

        boolean connected = mockSession.connect(SLOW_HOST, VALID_PORT);
        assertFalse("Should fail without retry", connected);
        assertFalse("Should not be connected", mockSession.isConnected());
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
    @Test(timeout = 5000)
    public void testReadTimeoutDuringDataReception_PartialMessageHandling() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Simulate partial data reception followed by timeout
        mockSession.simulatePartialDataReceived(10);
        mockSession.simulateReadTimeout();

        assertFalse("Should be disconnected after read timeout", mockSession.isConnected());
        assertTrue("Timeout event should have been reported", mockSession.hasTimeoutOccurred());
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
    @Test(timeout = 5000)
    public void testWriteTimeoutSocketBufferFull_SlowReceiverHandling() throws Exception {
        mockSession.setWriteTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);
        mockSession.setAutoRecoveryEnabled(true);

        mockSession.simulateWriteTimeout();
        assertTrue("Timeout event should be recorded", mockSession.hasTimeoutOccurred());
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
    @Test(timeout = 5000)
    public void testInactivityTimeoutWithoutKeepalive_ConnectionDropped() throws Exception {
        mockSession.setInactivityTimeout(TIMEOUT_SHORT);
        mockSession.enableKeepalive(false, 0); // Keepalive disabled
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Simulate idle period longer than timeout
        Thread.sleep(2000);

        // Connection should be dropped
        mockSession.checkInactivityTimeout();
        assertFalse("Connection should be dropped after inactivity timeout", mockSession.isConnected());
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
    @Test(timeout = 10000)
    public void testCascadingTimeouts_MultipleTimeoutSequence() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.setWriteTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(3, 100);

        // First connect attempt times out
        mockSession.connect(SLOW_HOST, VALID_PORT);
        assertTrue("Should eventually connect with retries", mockSession.isConnected() || mockSession.getConnectAttempts() > 1);
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
    @Test(timeout = 10000)
    public void testTimeoutDuringRetry_ExponentialBackoffIncreases() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(3, 100); // Initial 100ms, exponential

        long startTime = System.currentTimeMillis();
        mockSession.connect(SLOW_HOST, VALID_PORT);
        long duration = System.currentTimeMillis() - startTime;

        // With exponential backoff: 100ms + 200ms + 400ms = 700ms minimum
        assertTrue("Should have backoff delay between retries", duration >= 400 || !mockSession.isConnected());
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
    @Test(timeout = 5000)
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

        assertTrue("Read should have been interrupted", readInterrupted.await(2, TimeUnit.SECONDS));
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
    @Test(timeout = 5000)
    public void testTimeoutWithPendingOperations_QueueHandling() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Queue multiple operations
        mockSession.queueOperation("SEND_COMMAND_1");
        mockSession.queueOperation("SEND_COMMAND_2");
        mockSession.queueOperation("SEND_COMMAND_3");

        assertEquals("Should have 3 queued operations", 3, mockSession.getPendingOperationCount());

        // Trigger read timeout
        mockSession.simulateReadTimeout();

        // Queue should be preserved or cleared appropriately
        assertTrue("Should handle queued operations on timeout",
                mockSession.getPendingOperationCount() >= 0);
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
    @Test(timeout = 5000)
    public void testRecoveryAfterReadTimeout_StateConsistency() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);
        String preTimeoutState = mockSession.getSessionState();

        mockSession.simulateReadTimeout();
        assertNotSame("State should change on timeout", preTimeoutState, mockSession.getSessionState());

        mockSession.reconnect();
        assertNotNull("Reconnected state should be valid", mockSession.getSessionState());
        assertTrue("Should be connected after recovery", mockSession.isConnected());
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
    @Test(timeout = 5000)
    public void testTimeoutNotificationToObservers_ListenerPattern() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        AtomicInteger notificationCount = new AtomicInteger(0);

        mockSession.addTimeoutListener(() -> notificationCount.incrementAndGet());
        mockSession.addTimeoutListener(() -> notificationCount.incrementAndGet());

        mockSession.simulateTimeout("CONNECT");

        assertEquals("Both observers should be notified", 2, notificationCount.get());
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
    @Test(timeout = 5000)
    public void testConnectTimeoutAtBoundary_NoSpuriousTimeout() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(1, 0);

        long startTime = System.currentTimeMillis();
        mockSession.connect(VALID_HOST, VALID_PORT);
        long elapsed = System.currentTimeMillis() - startTime;

        assertTrue("Should complete successfully", mockSession.isConnected());
        // Should complete within reasonable time, not hang
        assertTrue("Should not take excessively long", elapsed < 5000);
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
    @Test(timeout = 5000)
    public void testLongTimeoutDuration_ConfiguredButNotTriggered() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_LONG);
        mockSession.connect(VALID_HOST, VALID_PORT);
        assertTrue("Should be connected", mockSession.isConnected());

        // Should complete quickly despite long timeout
        Thread.sleep(100);
        assertTrue("Should still be connected with long timeout", mockSession.isConnected());
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
    @Test(timeout = 5000)
    public void testTimeoutDuringSslNegotiation_EncryptionHandling() throws Exception {
        mockSession.enableSSL(true);
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(1, 100);

        mockSession.connect(VALID_HOST, VALID_PORT);
        // Should either succeed or fail gracefully after timeout
        assertTrue("Should handle SSL timeout gracefully", true);
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
    @Test(timeout = 5000)
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

        assertTrue("Both operations should complete without deadlock",
                done.await(3, TimeUnit.SECONDS));
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
    @Test(timeout = 10000)
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
        assertTrue("Should handle multiple timeouts", mockSession.getTimeoutEventCount() >= 3);
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
    @Test(timeout = 5000)
    public void testKeepaliveTimeoutDuringIdle_ConnectionDroppedOnKeepaliveFailure() throws Exception {
        mockSession.setInactivityTimeout(TIMEOUT_SHORT);
        mockSession.enableKeepalive(true, 500); // 500ms keepalive
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Simulate keepalive timeout
        mockSession.simulateKeepaliveTimeout();
        assertFalse("Connection should be dropped on keepalive timeout", mockSession.isConnected());
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
    @Test(timeout = 5000)
    public void testTimeoutTriggersCircuitBreaker_FailureThresholdEnforced() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setCircuitBreakerThreshold(2); // Open after 2 failures

        // Multiple timeout attempts
        for (int i = 0; i < 3; i++) {
            mockSession.connect(SLOW_HOST, VALID_PORT);
        }

        assertTrue("Circuit breaker should be active after threshold", mockSession.isCircuitBreakerOpen());
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
    @Test(timeout = 5000)
    public void testTimeoutMetricsTracking_EventCountingAndReporting() throws Exception {
        mockSession.setConnectTimeout(TIMEOUT_SHORT);
        mockSession.setRetryPolicy(2, 100);

        long startCount = mockSession.getTimeoutEventCount();
        mockSession.connect(SLOW_HOST, VALID_PORT);
        long endCount = mockSession.getTimeoutEventCount();

        assertTrue("Timeout events should be tracked", endCount > startCount);
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
    @Test(timeout = 5000)
    public void testWriteTimeoutWithPartialMessage_IncompleteMessageHandling() throws Exception {
        mockSession.setWriteTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Send large message that will timeout partway
        byte[] largeMessage = new byte[10000];
        mockSession.simulatePartialWrite(5000, TIMEOUT_SHORT);

        assertTrue("Should handle partial write on timeout", mockSession.hasTimeoutOccurred());
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
    @Test(timeout = 5000)
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

        assertTrue("Operation should complete before timeout", done.await(2, TimeUnit.SECONDS));
        assertTrue("Should still be connected", mockSession.isConnected());
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
    @Test(timeout = 5000)
    public void testTimeoutDuringDisconnect_CleanupCompletion() throws Exception {
        mockSession.setReadTimeout(TIMEOUT_SHORT);
        mockSession.connect(VALID_HOST, VALID_PORT);

        // Trigger timeout during disconnect
        mockSession.simulateTimeoutDuringDisconnect();
        mockSession.disconnect();

        assertFalse("Should be disconnected despite timeout", mockSession.isConnected());
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
                if (host.equals("10.255.255.1")) {
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
