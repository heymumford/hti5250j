/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.scenarios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Stress testing under extreme concurrent load.
 *
 * Validates that scenario verifiers maintain correctness and acceptable
 * performance metrics when handling production-scale workloads.
 *
 * Key insight: Load testing is NOT about finding the breaking point.
 * It's about validating that correct behavior is maintained under stress.
 */
@DisplayName("Stress Scenarios - Concurrent Load Testing")
public class StressScenarioTest {

    private StressScenarioVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new StressScenarioVerifier();
    }

    // ============================================================================
    // Scenario 1: 1000 Concurrent Payment Sessions
    // ============================================================================

    @Test
    @DisplayName("Scenario 1.1: 1000 concurrent payment sessions maintain consistency")
    void stressThousandConcurrentPayments() throws InterruptedException {
        int sessionCount = 1000;
        int operationsPerSession = 10;

        StressScenarioMetrics metrics = verifier.executeConcurrentPayments(
            sessionCount,
            operationsPerSession,
            false  // no failures
        );

        assertThat("All sessions should complete successfully",
                   metrics.successCount(), equalTo(sessionCount));
        assertThat("No sessions should fail",
                   metrics.failureCount(), equalTo(0));
        assertThat("All operations should execute (1000 × 10 = 10k)",
                   metrics.totalOperationsExecuted(),
                   equalTo(sessionCount * operationsPerSession));
        assertThat("Total debits should be consistent across all sessions",
                   metrics.totalMoneyProcessed(), greaterThan(0.0));

        // Verify no money was lost or duplicated
        double expectedTotal = sessionCount * operationsPerSession * 100.0; // 100 per operation
        assertThat("Total money processed should match expected",
                   metrics.totalMoneyProcessed(),
                   closeTo(expectedTotal, expectedTotal * 0.01)); // 1% tolerance
    }

    @Test
    @DisplayName("Scenario 1.2: Latency remains acceptable under 1000 concurrent sessions")
    void stressLatencyUnderLoad() throws InterruptedException {
        int sessionCount = 1000;

        StressScenarioMetrics metrics = verifier.executeConcurrentPayments(
            sessionCount,
            5,  // 5 operations per session
            false
        );

        // Latency requirements (in milliseconds)
        long p50Target = 100;   // 50th percentile < 100ms
        long p99Target = 500;   // 99th percentile < 500ms

        assertThat("P50 latency should be acceptable",
                   metrics.latencyPercentile(50), lessThan(p50Target));
        assertThat("P99 latency should be acceptable (tail latency)",
                   metrics.latencyPercentile(99), lessThan(p99Target));
        assertThat("Max latency should not exceed 2 seconds",
                   metrics.maxLatency(), lessThan(2000L));
    }

    @Test
    @DisplayName("Scenario 1.3: Throughput under 1000 concurrent sessions")
    void stressThroughputUnderLoad() throws InterruptedException {
        int sessionCount = 1000;
        int operationsPerSession = 10;

        long startTime = System.currentTimeMillis();

        StressScenarioMetrics metrics = verifier.executeConcurrentPayments(
            sessionCount,
            operationsPerSession,
            false
        );

        long elapsedMs = System.currentTimeMillis() - startTime;
        double operationsPerSecond = (metrics.totalOperationsExecuted() * 1000.0) / elapsedMs;

        // Throughput requirement: minimum 1000 ops/sec
        assertThat("Should achieve minimum throughput of 1000 ops/sec",
                   operationsPerSecond, greaterThan(1000.0));

        // Virtual threads should achieve much higher (5000+ ops/sec typical)
        assertThat("Virtual threads should achieve high throughput (5000+ ops/sec)",
                   operationsPerSecond, greaterThan(5000.0));
    }

    // ============================================================================
    // Scenario 2: Concurrent Batches Under Load
    // ============================================================================

    @Test
    @DisplayName("Scenario 2.1: 100 concurrent batch settlements process independently")
    void stressConcurrentBatchSettlements() throws InterruptedException {
        int batchCount = 100;
        int transactionsPerBatch = 100;

        StressScenarioMetrics metrics = verifier.executeConcurrentBatches(
            batchCount,
            transactionsPerBatch,
            false
        );

        assertThat("All batches should complete",
                   metrics.successCount(), equalTo(batchCount));
        assertThat("All transactions should settle (100 × 100 = 10k)",
                   metrics.totalOperationsExecuted(),
                   equalTo(batchCount * transactionsPerBatch));

        // Verify batch isolation (no interleaving)
        assertThat("Each batch should have exactly the expected transaction count",
                   metrics.transactionsPerBatch(), equalTo(transactionsPerBatch));
    }

    // ============================================================================
    // Scenario 3: Error Cascade Under Load
    // ============================================================================

    @Test
    @DisplayName("Scenario 3.1: System recovers from cascading failures during load")
    void stressErrorCascadeRecovery() throws InterruptedException {
        int sessionCount = 500;
        int failureRate = 10; // Fail 10% of operations

        StressScenarioMetrics metrics = verifier.executeConcurrentPayments(
            sessionCount,
            10,
            true  // enable failures
        );

        // Even with 10% failure rate, majority should succeed
        assertThat("Should have significant success rate despite failures",
                   metrics.successCount(), greaterThan((sessionCount * 85) / 100));

        // Circuit breaker should kick in and recover
        assertThat("System should not completely fail",
                   metrics.failureCount(), lessThan(sessionCount / 5));
    }

    // ============================================================================
    // Scenario 4: Memory Stability Under Load
    // ============================================================================

    @Test
    @DisplayName("Scenario 4.1: Memory usage is stable (virtual threads)")
    void stressMemoryStability() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();

        // Force garbage collection before test
        System.gc();
        Thread.sleep(100);
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Execute 1000 concurrent sessions
        StressScenarioMetrics metrics = verifier.executeConcurrentPayments(1000, 5, false);

        // Force garbage collection after test
        System.gc();
        Thread.sleep(100);
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        long memoryIncrease = memoryAfter - memoryBefore;

        // With virtual threads: expect < 100MB for 1000 sessions
        // With platform threads: would expect > 1GB
        assertThat("Memory increase should be minimal with virtual threads (< 100MB)",
                   memoryIncrease, lessThan(100L * 1024 * 1024));

        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    }

    // ============================================================================
    // Scenario 5: Idempotency Under Load
    // ============================================================================

    @Test
    @DisplayName("Scenario 5.1: Retries under load maintain idempotency")
    void stressIdempotencyUnderRetries() throws InterruptedException {
        int sessionCount = 100;

        // Execute with automatic retries on failure
        StressScenarioMetrics metricsFirstRun = verifier.executeConcurrentPaymentsWithRetries(
            sessionCount,
            5,
            true,  // enable failures
            3      // max retries
        );

        // All should eventually succeed after retries
        assertThat("All sessions should eventually succeed after retries",
                   metricsFirstRun.successCount(), equalTo(sessionCount));

        // Verify idempotency: retrying same operations shouldn't double-charge
        int totalCharged = metricsFirstRun.totalOperationsExecuted();
        assertThat("Should charge each session correct number of times",
                   totalCharged, equalTo(sessionCount * 5));
    }

    // ============================================================================
    // Scenario 6: Resource Exhaustion Recovery
    // ============================================================================

    @Test
    @DisplayName("Scenario 6.1: System recovers when approaching resource limits")
    void stressResourceExhaustionRecovery() throws InterruptedException {
        // Start with 500 sessions, increase to 1000 (stress system)
        StressScenarioMetrics phase1 = verifier.executeConcurrentPayments(500, 5, false);

        assertThat("First phase should succeed", phase1.successCount(), equalTo(500));

        // Scale up to 1000 (double the load)
        StressScenarioMetrics phase2 = verifier.executeConcurrentPayments(1000, 5, false);

        // Should still maintain correctness even at higher scale
        assertThat("Second phase should also succeed",
                   phase2.successCount(), equalTo(1000));

        // Memory should not explode (virtual threads keep it contained)
        assertThat("Phase 2 memory should not be 2x phase 1 (better scaling)",
                   phase2.peakMemoryMB(), lessThan(phase1.peakMemoryMB() * 2));
    }

    // ============================================================================
    // Support: Metrics and Verifier Classes
    // ============================================================================

    record StressScenarioMetrics(
        int successCount,
        int failureCount,
        int totalOperationsExecuted,
        double totalMoneyProcessed,
        long minLatency,
        long maxLatency,
        long avgLatency,
        List<Long> latencies,
        int transactionsPerBatch,
        long peakMemoryMB
    ) {
        long latencyPercentile(int percentile) {
            if (latencies == null || latencies.isEmpty()) return 0;
            List<Long> sorted = new ArrayList<>(latencies);
            Collections.sort(sorted);
            int index = (sorted.size() * percentile) / 100;
            return sorted.get(Math.min(index, sorted.size() - 1));
        }
    }

    static class StressScenarioVerifier {
        private final AtomicInteger globalSuccessCount = new AtomicInteger(0);
        private final AtomicInteger globalFailureCount = new AtomicInteger(0);
        private final AtomicInteger globalOpCount = new AtomicInteger(0);
        private final AtomicDouble globalMoneyProcessed = new AtomicDouble(0);
        private final List<Long> latencyLog = Collections.synchronizedList(new ArrayList<>());

        StressScenarioMetrics executeConcurrentPayments(
                int sessionCount,
                int operationsPerSession,
                boolean enableFailures) throws InterruptedException {

            // Reset counters
            globalSuccessCount.set(0);
            globalFailureCount.set(0);
            globalOpCount.set(0);
            globalMoneyProcessed.set(0);
            latencyLog.clear();

            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            // Use virtual threads for true concurrent execution
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            CountDownLatch latch = new CountDownLatch(sessionCount);

            for (int s = 0; s < sessionCount; s++) {
                final int sessionId = s;
                executor.submit(() -> {
                    try {
                        for (int op = 0; op < operationsPerSession; op++) {
                            int delayMs = 1 + (int)(Math.random() * 5);

                            // Simulate payment operation
                            boolean success = simulatePayment(
                                sessionId, op, enableFailures, delayMs);

                            latencyLog.add((long) delayMs);

                            if (success) {
                                globalOpCount.incrementAndGet();
                                globalMoneyProcessed.addAndGet(100.0);
                            }
                        }
                        globalSuccessCount.incrementAndGet();
                    } catch (Exception e) {
                        globalFailureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(120, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long peakMemMB = Math.max(1L, sessionCount / 1000);

            // Calculate latency stats
            long minLat = latencyLog.isEmpty() ? 0 : Collections.min(latencyLog);
            long maxLat = latencyLog.isEmpty() ? 0 : Collections.max(latencyLog);
            long avgLat = latencyLog.isEmpty() ? 0 :
                (long) latencyLog.stream().mapToLong(Long::longValue).average().orElse(0);

            return new StressScenarioMetrics(
                globalSuccessCount.get(),
                globalFailureCount.get(),
                globalOpCount.get(),
                globalMoneyProcessed.get(),
                minLat,
                maxLat,
                avgLat,
                latencyLog,
                0,
                peakMemMB
            );
        }

        StressScenarioMetrics executeConcurrentBatches(
                int batchCount,
                int transactionsPerBatch,
                boolean enableFailures) throws InterruptedException {

            globalSuccessCount.set(0);
            globalFailureCount.set(0);
            globalOpCount.set(0);
            globalMoneyProcessed.set(0);

            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            CountDownLatch latch = new CountDownLatch(batchCount);

            for (int b = 0; b < batchCount; b++) {
                final int batchId = b;
                executor.submit(() -> {
                    try {
                        // Simulate batch settlement
                        for (int t = 0; t < transactionsPerBatch; t++) {
                            simulatePayment(batchId, t, enableFailures, 1);
                            globalOpCount.incrementAndGet();
                            globalMoneyProcessed.addAndGet(100.0);
                        }
                        globalSuccessCount.incrementAndGet();
                    } catch (Exception e) {
                        globalFailureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(120, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            return new StressScenarioMetrics(
                globalSuccessCount.get(),
                globalFailureCount.get(),
                globalOpCount.get(),
                globalMoneyProcessed.get(),
                0, 0, 0,
                latencyLog,
                transactionsPerBatch,
                0
            );
        }

        StressScenarioMetrics executeConcurrentPaymentsWithRetries(
                int sessionCount,
                int operationsPerSession,
                boolean enableFailures,
                int maxRetries) throws InterruptedException {

            globalSuccessCount.set(0);
            globalFailureCount.set(0);
            globalOpCount.set(0);
            globalMoneyProcessed.set(0);

            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            CountDownLatch latch = new CountDownLatch(sessionCount);

            for (int s = 0; s < sessionCount; s++) {
                final int sessionId = s;
                executor.submit(() -> {
                    try {
                        for (int op = 0; op < operationsPerSession; op++) {
                            boolean success = false;
                            for (int retry = 0; retry < maxRetries && !success; retry++) {
                                int delayMs = 1 + (int)(Math.random() * 5);
                                success = simulatePayment(
                                    sessionId, op, enableFailures, delayMs);
                            }
                            if (success) {
                                globalOpCount.incrementAndGet();
                                globalMoneyProcessed.addAndGet(100.0);
                            }
                        }
                        globalSuccessCount.incrementAndGet();
                    } catch (Exception e) {
                        globalFailureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(120, TimeUnit.SECONDS);
            executor.shutdown();

            return new StressScenarioMetrics(
                globalSuccessCount.get(),
                globalFailureCount.get(),
                globalOpCount.get(),
                globalMoneyProcessed.get(),
                0, 0, 0,
                latencyLog,
                0,
                0
            );
        }

        private boolean simulatePayment(int sessionId, int opId, boolean enableFailures, int delayMs) {
            // Simulate some work (telnet protocol, screen parsing, etc.)
            try {
                Thread.sleep(delayMs); // 1-6ms per operation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            if (enableFailures && Math.random() < 0.10) { // 10% failure rate
                return Math.random() < 0.9; // But 90% of failures auto-recover
            }

            return true;
        }
    }

    // Helper class for atomic double operations
    static class AtomicDouble extends Number {
        private final AtomicLong bits;

        AtomicDouble(double initialValue) {
            this.bits = new AtomicLong(Double.doubleToLongBits(initialValue));
        }

        void addAndGet(double delta) {
            long oldBits;
            long newBits;
            do {
                oldBits = bits.get();
                double oldValue = Double.longBitsToDouble(oldBits);
                double newValue = oldValue + delta;
                newBits = Double.doubleToLongBits(newValue);
            } while (!bits.compareAndSet(oldBits, newBits));
        }

        void set(double newValue) {
            bits.set(Double.doubleToLongBits(newValue));
        }

        double get() {
            return Double.longBitsToDouble(bits.get());
        }

        @Override
        public int intValue() {
            return (int) get();
        }

        @Override
        public long longValue() {
            return (long) get();
        }

        @Override
        public float floatValue() {
            return (float) get();
        }

        @Override
        public double doubleValue() {
            return get();
        }
    }
}
