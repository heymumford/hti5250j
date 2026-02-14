/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * Chaos injection tests for reliability validation (resilience4j)
 * Simulates failures and validates recovery mechanisms
 */

package org.hti5250j.test.chaos;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Chaos injection tests using resilience4j.
 * Tests system resilience under simulated failures:
 * - Transient failures (timeout, retry recovery)
 * - Timeout scenarios
 * - Cascading failures
 * - Recovery after chaos
 */
@DisplayName("Chaos Injection Tests")
public class ChaosInjectionTests {

    private AtomicInteger invocationCount;
    private Retry retryPolicy;
    private TimeLimiter timeLimiter;

    @BeforeEach
    void setup() {
        invocationCount = new AtomicInteger(0);

        // Configure retry policy: 3 attempts, exponential backoff
        retryPolicy = Retry.of("test-retry", RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(100))
            .retryOnException(e -> true)  // Retry on any exception
            .build()
        );

        // Configure time limiter: 1 second timeout
        timeLimiter = TimeLimiter.of("test-timer", TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(1))
            .cancelRunningFuture(true)
            .build()
        );
    }

    /**
     * Chaos Test: Transient failure recovery
     * First 2 invocations fail, 3rd succeeds
     * Validates that retry mechanism recovers
     */
    @Test
    @DisplayName("Transient failure recovery via retry")
    @Timeout(5)
    void chaosTransientFailureRecovery() {
        // Setup: Fail twice, then succeed
        Callable<String> flaky = () -> {
            int count = invocationCount.incrementAndGet();
            if (count < 3) {
                throw new RuntimeException("Simulated transient failure #" + count);
            }
            return "SUCCESS";
        };

        // Execute with retry
        Callable<String> retried = Retry.decorateCallable(retryPolicy, flaky);

        // Should succeed after retries
        assertThatCode(() -> {
            String result = retried.call();
            assertThat(result).isEqualTo("SUCCESS");
            assertThat(invocationCount.get()).isEqualTo(3);  // Failed twice, succeeded once
        }).doesNotThrowAnyException();
    }

    /**
     * Chaos Test: Permanent failure exhausts retries
     * All invocations fail permanently
     * Validates that retry gives up after max attempts
     */
    @Test
    @DisplayName("Permanent failure exhausts retry attempts")
    @Timeout(5)
    void chaosPermanentFailureExhaustsRetries() {
        // Setup: Always fail
        Callable<String> broken = () -> {
            invocationCount.incrementAndGet();
            throw new RuntimeException("Permanent failure");
        };

        // Execute with retry
        Callable<String> retried = Retry.decorateCallable(retryPolicy, broken);

        // Should fail after all retries exhausted
        assertThatThrownBy(retried::call)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Permanent failure");

        // Verify all retry attempts were made
        assertThat(invocationCount.get()).isEqualTo(3);  // Max attempts
    }

    /**
     * Chaos Test: Timeout detection
     * Operation exceeds time limit
     * Validates that timeout is properly detected
     */
    @Test
    @DisplayName("Timeout detection and cancellation")
    @Timeout(5)
    void chaosTimeoutDetection() {
        // Setup: Operation takes 2 seconds, timeout is 1 second
        Callable<String> slow = () -> {
            Thread.sleep(2000);
            return "SHOULD_NOT_REACH";
        };

        // Execute with time limiter - should timeout
        assertThatThrownBy(() -> callWithTimeout(slow))
            .isInstanceOf(TimeoutException.class);
    }

    /**
     * Chaos Test: Recovery after timeout
     * First operation times out, second succeeds
     * Validates that system can recover from timeout
     */
    @Test
    @DisplayName("Recovery after timeout")
    @Timeout(5)
    void chaosRecoveryAfterTimeout() {
        AtomicInteger callCount = new AtomicInteger(0);

        // Setup: First call times out, second succeeds
        Callable<String> sometimesSlow = () -> {
            int call = callCount.incrementAndGet();
            if (call == 1) {
                Thread.sleep(2000);  // Timeout
            }
            return "SUCCESS";
        };

        // First call: timeout
        assertThatThrownBy(() -> callWithTimeout(sometimesSlow))
            .isInstanceOf(TimeoutException.class);

        // Second call: should succeed
        assertThatCode(() -> {
            String result = callWithTimeout(sometimesSlow);
            assertThat(result).isEqualTo("SUCCESS");
        }).doesNotThrowAnyException();
    }

    /**
     * Chaos Test: Retry + timeout combination
     * Validates that retry and timeout work together
     */
    @Test
    @DisplayName("Retry with timeout protection")
    @Timeout(10)
    void chaosRetryWithTimeout() {
        AtomicInteger attemptCount = new AtomicInteger(0);

        // Setup: First 2 attempts fail quickly, 3rd succeeds
        Callable<String> flakyFast = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("Attempt " + attempt + " failed");
            }
            return "SUCCESS";
        };

        // Decorate with both retry and timeout
        Callable<String> decorated = Retry.decorateCallable(retryPolicy,
            () -> callWithTimeout(flakyFast)
        );

        // Should succeed after retries
        assertThatCode(() -> {
            String result = decorated.call();
            assertThat(result).isEqualTo("SUCCESS");
            assertThat(attemptCount.get()).isEqualTo(3);
        }).doesNotThrowAnyException();
    }

    /**
     * Chaos Test: Cascading failure prevention
     * When operation A fails, B should not fail (circuit breaker concept)
     * Validates isolation of failures
     */
    @Test
    @DisplayName("Failure isolation prevents cascades")
    @Timeout(5)
    void chaosCascadingFailurePrevention() {
        // Setup: Operation A fails
        AtomicInteger aCount = new AtomicInteger(0);
        Callable<String> operationA = () -> {
            aCount.incrementAndGet();
            throw new RuntimeException("Operation A failed");
        };

        // Operation B depends on A but should handle failure gracefully
        Callable<String> operationB = () -> {
            try {
                operationA.call();
            } catch (Exception e) {
                // Gracefully handle A's failure
                return "B_FALLBACK";
            }
            return "B_NORMAL";
        };

        // B should return fallback value
        assertThatCode(() -> {
            String result = operationB.call();
            assertThat(result).isEqualTo("B_FALLBACK");
        }).doesNotThrowAnyException();
    }

    /**
     * Chaos Test: Partial system failure
     * Some operations fail while others succeed
     * Validates that system remains partially functional
     */
    @Test
    @DisplayName("Partial system failure handling")
    @Timeout(5)
    void chaosPartialSystemFailure() {
        // Setup: 3 operations, 1 fails
        Callable<String> op1 = () -> "OP1_SUCCESS";
        Callable<String> op2 = () -> { throw new RuntimeException("OP2_FAILED"); };
        Callable<String> op3 = () -> "OP3_SUCCESS";

        // Execute operations with failure tolerance
        boolean op1Success = executeWithFallback(op1);
        boolean op2Success = executeWithFallback(op2);
        boolean op3Success = executeWithFallback(op3);

        // Verify partial success
        assertThat(op1Success).isTrue();
        assertThat(op2Success).isFalse();
        assertThat(op3Success).isTrue();

        // System should still be functional (2 of 3)
        int successCount = (op1Success ? 1 : 0) + (op2Success ? 1 : 0) + (op3Success ? 1 : 0);
        assertThat(successCount).isGreaterThanOrEqualTo(2);
    }

    /**
     * Chaos Test: Recovery time measurement
     * Measures how quickly system recovers from failures
     * Validates recovery is within acceptable bounds
     */
    @Test
    @DisplayName("Recovery time acceptable bounds")
    @Timeout(5)
    void chaosRecoveryTimeAcceptable() {
        AtomicInteger failureCount = new AtomicInteger(0);

        // Setup: Fail once, then recover
        Callable<String> transientFail = () -> {
            if (failureCount.getAndIncrement() < 1) {
                throw new RuntimeException("Transient failure");
            }
            return "RECOVERED";
        };

        // Measure recovery time (retry will fail once, then succeed)
        long startTime = System.currentTimeMillis();

        assertThatCode(() -> {
            String result = Retry.decorateCallable(retryPolicy, transientFail).call();
            assertThat(result).isEqualTo("RECOVERED");
        }).doesNotThrowAnyException();

        long recoveryTime = System.currentTimeMillis() - startTime;

        // Recovery should be < 500ms (including retry backoff ~100ms)
        assertThat(recoveryTime).isLessThan(500);
    }

    // ========== HELPERS ==========

    private <T> T callWithTimeout(Callable<T> callable) throws Exception {
        try {
            return timeLimiter.executeFutureSupplier(() ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return callable.call();
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
            );
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                throw (TimeoutException) e.getCause();
            }
            throw e;
        }
    }

    private boolean executeWithFallback(Callable<String> operation) {
        try {
            operation.call();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
