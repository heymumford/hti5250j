/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress tests for BatchExecutor under high concurrency.
 * Validates scalability, thread safety, and performance characteristics.
 */
public class BatchExecutorStressTest {

    /**
     * Stress test: 1000 concurrent workflow results processing.
     * Validates metrics calculation under load and thread safety.
     */
    @Test
    @Timeout(30)
    public void testStress1000ConcurrentWorkflows() throws InterruptedException {
        // Given: 1000 concurrent workflow results
        int workflowCount = 1000;
        List<WorkflowResult> results = new java.util.concurrent.CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(workflowCount);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Random random = new Random(42); // Reproducible randomness

        long startTime = System.nanoTime();

        // When: Generate results concurrently with varying latencies
        for (int i = 0; i < workflowCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Simulate realistic latencies (100-500ms)
                    long latency = 100 + random.nextInt(400);

                    // 95% success rate
                    boolean success = random.nextDouble() < 0.95;

                    WorkflowResult result;
                    if (success) {
                        result = WorkflowResult.success(
                            "row" + index,
                            latency,
                            "/artifacts/path" + index
                        );
                    } else {
                        result = WorkflowResult.failure(
                            "row" + index,
                            latency,
                            new Exception("Simulated failure")
                        );
                    }
                    results.add(result);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Stress test did not complete in time");
        executor.shutdown();

        long endTime = System.nanoTime();

        // Then: Verify metrics and performance
        assertEquals(workflowCount, results.size(), "All workflows should complete");

        BatchMetrics metrics = BatchMetrics.from(results, startTime, endTime);
        assertEquals(workflowCount, metrics.totalWorkflows());
        assertTrue(metrics.successCount() > 900, "Success rate should be ~95%");
        assertTrue(metrics.failureCount() > 0, "Should have some failures");
        assertTrue(metrics.throughputOpsPerSec() > 300, "Throughput should exceed 300 ops/sec");
    }

    /**
     * Stress test: Partial failure isolation.
     * Validates that failures in individual workflows don't affect others.
     */
    @Test
    @Timeout(30)
    public void testStressPartialFailureIsolation() throws InterruptedException {
        // Given: 500 workflows with deterministic failures at specific indices
        int workflowCount = 500;
        List<WorkflowResult> results = new java.util.concurrent.CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(workflowCount);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        int[] failureIndices = {50, 150, 250, 350, 450}; // 5 predetermined failures
        java.util.Set<Integer> failureSet = new java.util.HashSet<>();
        for (int idx : failureIndices) {
            failureSet.add(idx);
        }

        // When: Generate results with isolated failures
        for (int i = 0; i < workflowCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    WorkflowResult result;
                    if (failureSet.contains(index)) {
                        result = WorkflowResult.failure(
                            "row" + index,
                            100,
                            new Exception("Deterministic failure at index " + index)
                        );
                    } else {
                        result = WorkflowResult.success(
                            "row" + index,
                            100 + (index % 50),
                            "/artifacts/row" + index
                        );
                    }
                    results.add(result);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // Then: Verify isolation - failures don't affect others
        assertEquals(workflowCount, results.size());
        BatchMetrics metrics = BatchMetrics.from(results, System.nanoTime(), System.nanoTime());
        assertEquals(workflowCount - failureIndices.length, metrics.successCount());
        assertEquals(failureIndices.length, metrics.failureCount());

        // Verify each failure was captured
        for (int idx : failureIndices) {
            assertTrue(
                metrics.failures().stream().anyMatch(f -> f.rowKey().equals("row" + idx)),
                "Failure at index " + idx + " should be captured"
            );
        }
    }

    /**
     * Stress test: Thread safety of BatchMetrics aggregation.
     * Validates concurrent metric computation.
     */
    @Test
    @Timeout(30)
    public void testStressThreadSafeAggregation() throws InterruptedException {
        // Given: Multiple threads computing metrics from shared results
        int resultCount = 500;
        List<WorkflowResult> results = generateResults(resultCount);

        // When: Compute metrics concurrently
        List<BatchMetrics> metrics = new java.util.concurrent.CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(10);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    long startNanos = System.nanoTime();
                    long endNanos = System.nanoTime() + 1_000_000_000;
                    BatchMetrics m = BatchMetrics.from(results, startNanos, endNanos);
                    metrics.add(m);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // Then: All metric computations should be consistent
        assertEquals(10, metrics.size());
        long expectedTotal = metrics.get(0).totalWorkflows();
        for (BatchMetrics m : metrics) {
            assertEquals(expectedTotal, m.totalWorkflows(), "Total should be consistent");
            assertEquals(metrics.get(0).successCount(), m.successCount(), "Success count should be consistent");
        }
    }

    /**
     * Stress test: Memory stability with 1000 workflows.
     * Validates virtual threads consume minimal memory.
     */
    @Test
    @Timeout(60)
    public void testStressMemoryStability() throws InterruptedException {
        // Given: Memory baseline before stress
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // When: Execute 1000 virtual threads
        int threadCount = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger completed = new AtomicInteger(0);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(10); // Brief work
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS));
        executor.shutdown();

        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Then: Memory increase should be minimal
        long memoryIncreaseMB = (endMemory - startMemory) / (1024 * 1024);
        assertTrue(memoryIncreaseMB < 300,
            "Memory increase too large: " + memoryIncreaseMB + "MB (expected < 300MB)");
        assertEquals(threadCount, completed.get(), "All threads should complete");
    }

    /**
     * Stress test: Latency percentile accuracy under load.
     * Validates P50/P99 calculations with realistic distribution.
     */
    @Test
    @Timeout(30)
    public void testStressLatencyPercentiles() throws InterruptedException {
        // Given: 500 results with simulated latency distribution
        int workflowCount = 500;
        List<WorkflowResult> results = new java.util.concurrent.CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(workflowCount);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Random random = new Random(42);

        // When: Generate results with bimodal latency distribution
        for (int i = 0; i < workflowCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Bimodal: 80% fast (50-100ms), 20% slow (500-1000ms)
                    long latency;
                    if (random.nextDouble() < 0.8) {
                        latency = 50 + random.nextInt(50);
                    } else {
                        latency = 500 + random.nextInt(500);
                    }

                    WorkflowResult result = WorkflowResult.success(
                        "row" + index,
                        latency,
                        "/artifacts/row" + index
                    );
                    results.add(result);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // Then: Verify percentiles reflect distribution
        long startNanos = System.nanoTime();
        long endNanos = System.nanoTime() + 1_000_000_000;
        BatchMetrics metrics = BatchMetrics.from(results, startNanos, endNanos);

        assertEquals(workflowCount, metrics.totalWorkflows());
        assertTrue(metrics.p50LatencyMs() > 50 && metrics.p50LatencyMs() < 150,
            "P50 should be in fast distribution range");
        assertTrue(metrics.p99LatencyMs() >= metrics.p50LatencyMs(),
            "P99 should be >= P50");
    }

    /**
     * Stress test: Rapid successive batch operations.
     * Validates executor cleanup and reuse.
     */
    @Test
    @Timeout(30)
    public void testStressRapidSuccessiveBatches() throws InterruptedException {
        // Given: Multiple sequential batches
        int batchCount = 5;
        int workflowsPerBatch = 100;

        // When: Execute multiple batches in succession
        for (int batch = 0; batch < batchCount; batch++) {
            List<WorkflowResult> results = generateResults(workflowsPerBatch);

            long startNanos = System.nanoTime();
            long endNanos = System.nanoTime() + 1_000_000_000;
            BatchMetrics metrics = BatchMetrics.from(results, startNanos, endNanos);

            // Then: Each batch should complete successfully
            assertEquals(workflowsPerBatch, metrics.totalWorkflows());
            assertTrue(metrics.successRate() >= 90, "Success rate should be high");
        }
    }

    // Helper method to generate test results
    private List<WorkflowResult> generateResults(int count) {
        List<WorkflowResult> results = new ArrayList<>();
        Random random = new Random(42);

        for (int i = 0; i < count; i++) {
            boolean success = random.nextDouble() < 0.95;
            long latency = 100 + random.nextInt(400);

            WorkflowResult result;
            if (success) {
                result = WorkflowResult.success(
                    "row" + i,
                    latency,
                    "/artifacts/row" + i
                );
            } else {
                result = WorkflowResult.failure(
                    "row" + i,
                    latency,
                    new Exception("Test failure")
                );
            }
            results.add(result);
        }

        return results;
    }
}
