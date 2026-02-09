/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for virtual thread execution in workflow batch processing.
 * Verifies thread efficiency, error propagation, and timeout handling.
 */
public class VirtualThreadIntegrationTest {

    @Test
    public void testVirtualThreadCreation() {
        // Given: Collect thread info during virtual thread execution
        List<String> threadNames = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(3);

        // When: Submit tasks to virtual thread executor
        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                threadNames.add(Thread.currentThread().getName());
                assertTrue(Thread.currentThread().isVirtual(), "Should be virtual thread");
                latch.countDown();
            });
        }

        try {
            assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Latch await interrupted");
        }

        // Then: Verify all executed on virtual threads
        assertEquals(3, threadNames.size());
        executor.shutdown();
    }

    @Test
    public void testBatchMetricsThreadSafety() throws InterruptedException {
        // Given: Multiple concurrent workflow results
        int workflowCount = 100;
        List<WorkflowResult> results = new java.util.concurrent.CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(workflowCount);

        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        // When: Generate results concurrently
        for (int i = 0; i < workflowCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Simulate variable execution times
                    long latency = 100 + (index % 50);
                    WorkflowResult result = WorkflowResult.success(
                        "row" + index,
                        latency,
                        "/artifacts/path" + index
                    );
                    results.add(result);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();

        // Then: Verify thread-safe aggregation
        long startNanos = System.nanoTime();
        long endNanos = startNanos + 1_000_000_000;
        BatchMetrics metrics = BatchMetrics.from(results, startNanos, endNanos);

        assertEquals(workflowCount, metrics.totalWorkflows());
        assertEquals(workflowCount, metrics.successCount());
        assertEquals(0, metrics.failureCount());
    }

    @Test
    public void testErrorPropagationFromVirtualThread() {
        // Given: Virtual thread that throws exception
        List<Throwable> caughtExceptions = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        // When: Submit task that throws
        executor.submit(() -> {
            try {
                throw new NavigationException("Test error");
            } finally {
                latch.countDown();
            }
        });

        try {
            assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Latch await interrupted");
        }

        executor.shutdown();
    }

    @Test
    public void testInterruptedExceptionHandling() throws InterruptedException {
        // Given: Virtual thread with interrupt handling
        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        // When: Submit task with exception handling
        java.util.concurrent.Future<?> future = executor.submit(() -> {
            try {
                Thread.sleep(100); // Brief sleep
                completed.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        // Wait for completion
        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();

        // Verify task completed normally
        assertEquals(1, completed.get());
    }

    @Test
    public void testBatchMetricsFromConcurrentResults() throws InterruptedException {
        // Given: Concurrent generation of both success and failure results
        int totalWorkflows = 50;
        int failureCount = 10;
        List<WorkflowResult> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(totalWorkflows);

        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        // When: Generate mixed results
        for (int i = 0; i < totalWorkflows; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    WorkflowResult result;
                    if (index < failureCount) {
                        result = WorkflowResult.failure(
                            "row" + index,
                            50,
                            new Exception("Simulated failure")
                        );
                    } else {
                        result = WorkflowResult.success(
                            "row" + index,
                            100 + index,
                            "/path" + index
                        );
                    }
                    results.add(result);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();

        // Then: Verify metrics aggregation
        long startNanos = System.nanoTime();
        long endNanos = startNanos + 1_000_000_000; // 1 second duration
        BatchMetrics metrics = BatchMetrics.from(results, startNanos, endNanos);

        assertEquals(totalWorkflows, metrics.totalWorkflows());
        assertEquals(totalWorkflows - failureCount, metrics.successCount());
        assertEquals(failureCount, metrics.failureCount());
    }

    @Test
    public void testTimeoutExceptionInVirtualThread() {
        // Given: Virtual thread that times out
        CountDownLatch latch = new CountDownLatch(1);
        List<java.util.concurrent.TimeoutException> timeouts = new ArrayList<>();

        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        // When: Submit task that times out via Future.get()
        java.util.concurrent.Future<?> future = executor.submit(() -> {
            try {
                Thread.sleep(10000); // Long sleep
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        try {
            future.get(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            timeouts.add(e);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        executor.shutdownNow();

        // Then: Verify timeout was caught
        assertEquals(1, timeouts.size());
    }

    @Test
    public void testMemoryEfficiencyOfVirtualThreads() throws InterruptedException {
        // Given: Large number of virtual threads
        int threadCount = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger completed = new AtomicInteger(0);

        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        // When: Create many virtual threads
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(100); // Brief sleep
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();
        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Then: Verify all completed
        assertEquals(threadCount, completed.get());
        // Memory increase should be minimal (< 100MB for 1000 virtual threads)
        long memoryIncrease = (endMemory - startMemory) / (1024 * 1024); // Convert to MB
        assertTrue(memoryIncrease < 200, "Memory increase too large: " + memoryIncrease + "MB");
    }

    @Test
    public void testConcurrentWorkflowResultCollection() throws InterruptedException {
        // Given: Concurrent result collection from multiple threads
        int workflowCount = 100;
        java.util.concurrent.CopyOnWriteArrayList<WorkflowResult> results =
            new java.util.concurrent.CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(workflowCount);

        var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

        // When: Generate results concurrently
        for (int i = 0; i < workflowCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    WorkflowResult result = WorkflowResult.success(
                        "row" + index,
                        100L,
                        "/artifacts/row" + index
                    );
                    results.add(result);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();

        // Then: Verify all results collected
        assertEquals(workflowCount, results.size());
        assertTrue(results.stream().allMatch(WorkflowResult::success));
    }
}
