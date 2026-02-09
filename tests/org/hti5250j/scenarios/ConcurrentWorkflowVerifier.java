/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.scenarios;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ConcurrentWorkflowVerifier {

    private static class WorkflowMetrics {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        AtomicInteger operationCount = new AtomicInteger(0);
        AtomicInteger datasetReadCount = new AtomicInteger(0);
        AtomicInteger sessionConnectionCount = new AtomicInteger(0);
        AtomicInteger ledgerEntryCount = new AtomicInteger(0);
        AtomicLong maxKeyboardLockWaitTime = new AtomicLong(0);

        CopyOnWriteArrayList<Long> latencies = new CopyOnWriteArrayList<>();
        StringBuilder ledger = new StringBuilder();
        Object ledgerLock = new Object();

        void recordLatency(long latencyMs) {
            latencies.add(latencyMs);
        }

        void appendLedger(String entry) {
            synchronized (ledgerLock) {
                ledger.append(entry).append("\n");
            }
        }

        int getLedgerEntryCount() {
            synchronized (ledgerLock) {
                return (int) ledger.toString().split("\n").length;
            }
        }
    }

    private final WorkflowMetrics metrics = new WorkflowMetrics();
    private final Object keyboardLock = new Object();
    private boolean keyboardLocked = false;

    public ConcurrentWorkflowMetrics executeConcurrentLogins(int workflowCount) throws Exception {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(workflowCount);

        for (int i = 0; i < workflowCount; i++) {
            final int workflowId = i;
            executor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    simulateLogin(workflowId);
                    long latency = System.currentTimeMillis() - startTime;
                    metrics.recordLatency(latency);
                    metrics.successCount.incrementAndGet();
                    metrics.ledgerEntryCount.incrementAndGet();
                } catch (Exception e) {
                    metrics.failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();
        return new ConcurrentWorkflowMetrics(metrics);
    }

    public ConcurrentWorkflowMetrics executeConcurrentPaymentWorkflows(
        int workflowCount,
        int iterationsPerWorkflow) throws Exception {

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(workflowCount);

        for (int i = 0; i < workflowCount; i++) {
            final int workflowId = i;
            executor.submit(() -> {
                try {
                    for (int iter = 0; iter < iterationsPerWorkflow; iter++) {
                        long startTime = System.currentTimeMillis();
                        simulatePaymentWorkflow(workflowId, iter);
                        long latency = System.currentTimeMillis() - startTime;
                        metrics.recordLatency(latency);
                        metrics.operationCount.incrementAndGet();
                    }
                    metrics.successCount.incrementAndGet();
                } catch (Exception e) {
                    metrics.failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();
        return new ConcurrentWorkflowMetrics(metrics);
    }

    public ConcurrentWorkflowMetrics executeConcurrentPaymentWorkflowsWithTimeout(
        int workflowCount,
        int iterationsPerWorkflow,
        int workflowIdToTimeout,
        long timeoutAfterMs) throws Exception {

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(workflowCount);

        for (int i = 0; i < workflowCount; i++) {
            final int workflowId = i;
            executor.submit(() -> {
                try {
                    if (workflowId == workflowIdToTimeout) {
                        Thread.sleep(timeoutAfterMs);
                        throw new TimeoutException("Workflow " + workflowId + " timed out");
                    }
                    for (int iter = 0; iter < iterationsPerWorkflow; iter++) {
                        simulatePaymentWorkflow(workflowId, iter);
                    }
                    metrics.successCount.incrementAndGet();
                } catch (TimeoutException e) {
                    metrics.timeoutCount.incrementAndGet();
                } catch (Exception e) {
                    metrics.failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();
        return new ConcurrentWorkflowMetrics(metrics);
    }

    public ConcurrentWorkflowMetrics executeConcurrentPaymentWorkflowsWithException(
        int workflowCount,
        int iterationsPerWorkflow,
        int workflowIdToThrow) throws Exception {

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(workflowCount);

        for (int i = 0; i < workflowCount; i++) {
            final int workflowId = i;
            executor.submit(() -> {
                try {
                    if (workflowId == workflowIdToThrow) {
                        throw new RuntimeException("Simulated failure in workflow " + workflowId);
                    }
                    for (int iter = 0; iter < iterationsPerWorkflow; iter++) {
                        simulatePaymentWorkflow(workflowId, iter);
                    }
                    metrics.successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    metrics.exceptionCount.incrementAndGet();
                } catch (Exception e) {
                    metrics.failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();
        return new ConcurrentWorkflowMetrics(metrics);
    }

    public ConcurrentWorkflowMetrics executeConcurrentPaymentWorkflowsWithFailureRate(
        int workflowCount,
        int iterationsPerWorkflow,
        double failureRate) throws Exception {

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(workflowCount);
        Random random = new Random();

        for (int i = 0; i < workflowCount; i++) {
            final int workflowId = i;
            executor.submit(() -> {
                try {
                    for (int iter = 0; iter < iterationsPerWorkflow; iter++) {
                        if (random.nextDouble() < failureRate) {
                            throw new RuntimeException("Simulated random failure");
                        }
                        simulatePaymentWorkflow(workflowId, iter);
                    }
                    metrics.successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    metrics.exceptionCount.incrementAndGet();
                } catch (Exception e) {
                    metrics.failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();
        return new ConcurrentWorkflowMetrics(metrics);
    }

    public ConcurrentWorkflowMetrics executeConcurrentPaymentWorkflowsImmediate(
        int workflowCount,
        int iterationsPerWorkflow) throws Exception {

        return executeConcurrentPaymentWorkflows(workflowCount, iterationsPerWorkflow);
    }

    private void simulateLogin(int workflowId) throws Exception {
        acquireKeyboard(500);
        metrics.appendLedger("LOGIN:" + workflowId);
        Thread.sleep(10);
        releaseKeyboard();
        metrics.sessionConnectionCount.incrementAndGet();
    }

    private void simulatePaymentWorkflow(int workflowId, int iteration) throws Exception {
        acquireKeyboard(500);
        metrics.appendLedger("LOGIN:" + workflowId);
        Thread.sleep(5);
        releaseKeyboard();
        metrics.sessionConnectionCount.incrementAndGet();

        acquireKeyboard(300);
        metrics.appendLedger("NAVIGATE:" + workflowId);
        Thread.sleep(3);
        releaseKeyboard();

        acquireKeyboard(1000);
        metrics.appendLedger("FILL:" + workflowId);
        metrics.datasetReadCount.incrementAndGet();
        Thread.sleep(10);
        releaseKeyboard();

        acquireKeyboard(200);
        metrics.appendLedger("SUBMIT:" + workflowId);
        Thread.sleep(5);
        releaseKeyboard();

        acquireKeyboard(300);
        metrics.appendLedger("ASSERT:" + workflowId);
        Thread.sleep(2);
        releaseKeyboard();
    }

    private void acquireKeyboard(long timeoutMs) throws TimeoutException {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeoutMs;

        while (keyboardLocked && System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long waitTime = System.currentTimeMillis() - startTime;
        if (waitTime > metrics.maxKeyboardLockWaitTime.get()) {
            metrics.maxKeyboardLockWaitTime.set(waitTime);
        }

        keyboardLocked = true;
    }

    private void releaseKeyboard() {
        keyboardLocked = false;
    }

    public long getMemoryUsedMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
    }

    public boolean validateLedgerIntegrity() {
        synchronized (metrics.ledgerLock) {
            String ledgerContent = metrics.ledger.toString();
            for (String line : ledgerContent.split("\n")) {
                if (!line.isEmpty() && !line.contains(":")) {
                    return false;
                }
            }
            return true;
        }
    }

    public int countIncompleteEntries() {
        synchronized (metrics.ledgerLock) {
            String ledgerContent = metrics.ledger.toString();
            int incompleteCount = 0;
            for (String line : ledgerContent.split("\n")) {
                if (!line.isEmpty() && !line.contains(":")) {
                    incompleteCount++;
                }
            }
            return incompleteCount;
        }
    }

    public static class ConcurrentWorkflowMetrics {
        private final WorkflowMetrics internal;

        ConcurrentWorkflowMetrics(WorkflowMetrics metrics) {
            this.internal = metrics;
        }

        public int successCount() {
            return internal.successCount.get();
        }

        public int failureCount() {
            return internal.failureCount.get();
        }

        public int timeoutCount() {
            return internal.timeoutCount.get();
        }

        public int exceptionCount() {
            return internal.exceptionCount.get();
        }

        public int operationCount() {
            return internal.operationCount.get();
        }

        public int datasetReadCount() {
            return internal.datasetReadCount.get();
        }

        public int sessionConnectionCount() {
            return internal.sessionConnectionCount.get();
        }

        public int ledgerEntryCount() {
            return internal.getLedgerEntryCount();
        }

        public long maxKeyboardLockWaitTime() {
            return internal.maxKeyboardLockWaitTime.get();
        }

        public double successRate() {
            int total = successCount() + failureCount() + timeoutCount() + exceptionCount();
            return total > 0 ? (double) successCount() / total : 0.0;
        }

        public long latencyPercentile(int percentile) {
            if (internal.latencies.isEmpty()) return 0;
            List<Long> sorted = new ArrayList<>(internal.latencies);
            Collections.sort(sorted);
            int index = (int) ((percentile / 100.0) * sorted.size());
            return sorted.get(Math.min(index, sorted.size() - 1));
        }
    }
}
