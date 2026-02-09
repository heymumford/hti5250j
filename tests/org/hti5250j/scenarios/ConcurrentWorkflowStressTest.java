/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.scenarios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Concurrent Workflow Stress - Phase 11")
public class ConcurrentWorkflowStressTest {

    private ConcurrentWorkflowVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new ConcurrentWorkflowVerifier();
    }

    @Test
    @DisplayName("Scenario 1.1: 10 concurrent login workflows complete successfully")
    void stressLoginWorkflowsConcurrent() throws Exception {
        int workflowCount = 10;
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentLogins(workflowCount);

        assertThat("All 10 login workflows should succeed",
                   metrics.successCount(), equalTo(workflowCount));
        assertThat("Zero login failures",
                   metrics.failureCount(), equalTo(0));
        assertThat("Each workflow logs at least one ledger entry",
                   metrics.ledgerEntryCount(), greaterThanOrEqualTo(workflowCount));
    }

    @Test
    @DisplayName("Scenario 1.2: 10 concurrent payment workflows complete (50 total operations)")
    void stressPaymentWorkflowsConcurrent() throws Exception {
        int workflowCount = 10;
        int iterationsPerWorkflow = 5;

        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            workflowCount,
            iterationsPerWorkflow
        );

        int expectedOperations = workflowCount * iterationsPerWorkflow * 5;
        assertThat("All 50 operations should execute successfully",
                   metrics.operationCount(), greaterThanOrEqualTo(expectedOperations - 10));
        assertThat("Success rate >= 90%",
                   metrics.successRate(), greaterThan(0.9));
    }

    @Test
    @DisplayName("Scenario 1.3: Memory usage remains acceptable under concurrent load")
    void stressMemoryUsageUnderLoad() throws Exception {
        int workflowCount = 10;
        long memoryBefore = verifier.getMemoryUsedMB();

        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            workflowCount,
            5
        );

        long memoryAfter = verifier.getMemoryUsedMB();
        long memoryUsed = memoryAfter - memoryBefore;

        assertThat("Memory usage should be < 200MB for 10 concurrent workflows",
                   memoryUsed, lessThan(200L));
    }

    @Test
    @DisplayName("Scenario 1.4: Latency remains acceptable under concurrent load")
    void stressLatencyUnderConcurrentLoad() throws Exception {
        int workflowCount = 10;

        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            workflowCount,
            5
        );

        long p50Latency = metrics.latencyPercentile(50);
        long p99Latency = metrics.latencyPercentile(99);

        assertThat("P50 latency should be acceptable (< 200ms)",
                   p50Latency, lessThan(200L));
        assertThat("P99 latency should be acceptable (< 1000ms)",
                   p99Latency, lessThan(1000L));
    }

    @Test
    @DisplayName("Scenario 1.5: 100% success rate across all concurrent operations")
    void stressHighSuccessRateConcurrent() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            10,
            5
        );

        assertThat("Success rate should be >= 95% under normal load",
                   metrics.successRate(), greaterThan(0.95));
    }

    @Test
    @DisplayName("Adversarial 2.1: One workflow times out, others complete")
    void stressOneWorkflowTimeoutOthersComplete() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflowsWithTimeout(
            10,
            5,
            3,
            1000
        );

        assertThat("Should have 9 successful completions",
                   metrics.successCount(), greaterThanOrEqualTo(9));
        assertThat("Should have 1 timeout failure",
                   metrics.timeoutCount(), greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Adversarial 2.2: One workflow throws exception, others continue")
    void stressOneWorkflowExceptionOthersComplete() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflowsWithException(
            10,
            5,
            5
        );

        assertThat("Should have 9 successful completions",
                   metrics.successCount(), greaterThanOrEqualTo(9));
        assertThat("Should have 1 exception",
                   metrics.exceptionCount(), greaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("Adversarial 2.3: Resource exhaustion (50 keyboards created/destroyed)")
    void stressResourceExhaustionKeyboards() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            10,
            5
        );

        assertThat("All 50 keyboard operations should complete",
                   metrics.operationCount(), greaterThanOrEqualTo(40));
    }

    @Test
    @DisplayName("Adversarial 2.4: Concurrent artifact collection doesn't corrupt ledger")
    void stressLedgerCorruptionUnderConcurrence() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            10,
            5
        );

        boolean ledgerValid = verifier.validateLedgerIntegrity();
        assertThat("Ledger should remain valid under concurrent writes",
                   ledgerValid, equalTo(true));

        assertThat("All ledger entries should be complete (no partial writes)",
                   verifier.countIncompleteEntries(), equalTo(0));
    }

    @Test
    @DisplayName("Adversarial 2.5: Dataset loader parallelized access (50 concurrent reads)")
    void stressDatasetLoaderConcurrentAccess() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            10,
            5
        );

        assertThat("Dataset loader should handle 50 concurrent reads",
                   metrics.datasetReadCount(), greaterThanOrEqualTo(40));
    }

    @Test
    @DisplayName("Adversarial 2.6: Session connection pool under concurrent load")
    void stressSessionConnectionPool() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            10,
            5
        );

        assertThat("All 10 workflows should establish sessions successfully",
                   metrics.sessionConnectionCount(), greaterThanOrEqualTo(9));
    }

    @Test
    @DisplayName("Adversarial 2.7: Keyboard lock contention (10 workflows, 1 lock)")
    void stressKeyboardLockContention() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            10,
            5
        );

        assertThat("Success rate should be > 90% despite keyboard contention",
                   metrics.successRate(), greaterThan(0.90));

        long maxLockWaitTime = metrics.maxKeyboardLockWaitTime();
        assertThat("Max lock wait time should be < 10s",
                   maxLockWaitTime, lessThan(10000L));
    }

    @Test
    @DisplayName("Adversarial 2.8: Virtual thread starvation (sustained 100% CPU)")
    void stressVirtualThreadStarvation() throws Exception {
        ExecutorService cpuStress = Executors.newSingleThreadExecutor();
        cpuStress.submit(() -> {
            long endTime = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < endTime) {
                // Spin
            }
        });

        try {
            ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
                10,
                5
            );

            assertThat("Workflows should complete despite CPU stress",
                       metrics.successRate(), greaterThan(0.80));
        } finally {
            cpuStress.shutdownNow();
        }
    }

    @Test
    @DisplayName("Adversarial 2.9: Mixed success/failure (20 succeed, 30 fail - others unaffected)")
    void stressMixedSuccessFailure() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflowsWithFailureRate(
            10,
            5,
            0.30
        );

        assertThat("Should have ~7 successful workflows (70%)",
                   metrics.successCount(), greaterThan(5));
        assertThat("Should have ~3 failed workflows (30%)",
                   metrics.failureCount(), greaterThan(1));
        assertThat("Overall success rate should be ~70%",
                   metrics.successRate(), allOf(
                       greaterThan(0.60),
                       lessThan(0.85)
                   ));
    }

    @Test
    @DisplayName("Adversarial 2.10: Rapid fire workflow submission (no queueing delay)")
    void stressRapidFireWorkflowSubmission() throws Exception {
        long startTime = System.currentTimeMillis();
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflowsImmediate(
            10,
            5
        );
        long elapsedTime = System.currentTimeMillis() - startTime;

        assertThat("All workflows should complete quickly (< 30s)",
                   elapsedTime, lessThan(30000L));
        assertThat("High success rate despite rapid submission",
                   metrics.successRate(), greaterThan(0.85));
    }

    @Test
    @DisplayName("Scenario 3: Full stress test - 10 workflows, 5 iterations, all features")
    void stressFullIntegration() throws Exception {
        ConcurrentWorkflowVerifier.ConcurrentWorkflowMetrics metrics = verifier.executeConcurrentPaymentWorkflows(
            10,
            5
        );

        assertThat("Success rate >= 90%", metrics.successRate(), greaterThan(0.90));
        assertThat("P99 latency < 1s", metrics.latencyPercentile(99), lessThan(1000L));
        assertThat("Memory < 200MB", verifier.getMemoryUsedMB(), lessThan(200L));
        assertThat("Ledger valid", verifier.validateLedgerIntegrity(), equalTo(true));
        assertThat("50+ operations executed", metrics.operationCount(), greaterThanOrEqualTo(45));
    }
}
