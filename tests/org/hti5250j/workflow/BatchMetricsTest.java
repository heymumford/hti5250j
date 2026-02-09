/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for BatchMetrics percentile calculations.
 * Verifies P50, P99, and edge cases using nearest-rank method.
 */
public class BatchMetricsTest {

    @Test
    public void testPercentileP50EdgeCase() {
        // Test with sorted list: [10, 20, 30, 40, 50]
        // P50 (50th percentile) should return the middle value (30)
        // Using nearest-rank: index = ceil(0.5 * 5) - 1 = 2
        List<WorkflowResult> results = new ArrayList<>();
        results.add(createSuccessResult(100, 10));
        results.add(createSuccessResult(101, 20));
        results.add(createSuccessResult(102, 30));
        results.add(createSuccessResult(103, 40));
        results.add(createSuccessResult(104, 50));

        BatchMetrics metrics = BatchMetrics.from(results, 0L, 5000L);

        // P50 should be 30 (middle value)
        assertThat(metrics.p50LatencyMs()).isEqualTo(30L);
    }

    @Test
    public void testPercentileP99EdgeCase() {
        // Test with 100 items: 1, 2, 3, ..., 100
        // P99 should return approximately the 99th item
        // Using nearest-rank: index = ceil(0.99 * 100) - 1 = 98 (99th item)
        List<WorkflowResult> results = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            results.add(createSuccessResult(1000 + i, i));
        }

        BatchMetrics metrics = BatchMetrics.from(results, 0L, 100000L);

        // P99 should be 99 (or very close)
        assertThat(metrics.p99LatencyMs()).isGreaterThanOrEqualTo(98L);
    }

    @Test
    public void testPercentileP50WithSingleItem() {
        // Edge case: single item should return that item for all percentiles
        List<WorkflowResult> results = new ArrayList<>();
        results.add(createSuccessResult(100, 42));

        BatchMetrics metrics = BatchMetrics.from(results, 0L, 1000L);

        assertThat(metrics.p50LatencyMs()).isEqualTo(42L);
    }

    @Test
    public void testPercentileP50WithTwoItems() {
        // With [10, 20], P50 should return 10 (first item using nearest-rank)
        // index = ceil(0.5 * 2) - 1 = 0
        List<WorkflowResult> results = new ArrayList<>();
        results.add(createSuccessResult(100, 10));
        results.add(createSuccessResult(101, 20));

        BatchMetrics metrics = BatchMetrics.from(results, 0L, 2000L);

        assertThat(metrics.p50LatencyMs()).isEqualTo(10L);
    }

    @Test
    public void testPercentileP50WithThreeItems() {
        // With [10, 20, 30], P50 should return 20 (middle item)
        // index = ceil(0.5 * 3) - 1 = 1
        List<WorkflowResult> results = new ArrayList<>();
        results.add(createSuccessResult(100, 10));
        results.add(createSuccessResult(101, 20));
        results.add(createSuccessResult(102, 30));

        BatchMetrics metrics = BatchMetrics.from(results, 0L, 3000L);

        assertThat(metrics.p50LatencyMs()).isEqualTo(20L);
    }

    @Test
    public void testPercentileWithFailures() {
        // Only successful workflows should be included in latency percentiles
        List<WorkflowResult> results = new ArrayList<>();
        results.add(createSuccessResult(100, 10));
        results.add(createFailureResult(101, new Exception("Failed")));
        results.add(createSuccessResult(102, 30));
        results.add(createFailureResult(103, new Exception("Failed")));
        results.add(createSuccessResult(104, 50));

        BatchMetrics metrics = BatchMetrics.from(results, 0L, 5000L);

        // Only latencies [10, 30, 50] count
        // P50 should be 30
        assertThat(metrics.p50LatencyMs()).isEqualTo(30L);
        assertThat(metrics.successCount()).isEqualTo(3);
        assertThat(metrics.failureCount()).isEqualTo(2);
    }

    @Test
    public void testThroughputCalculation() {
        // 100 workflows in 10 seconds = 10 ops/sec
        List<WorkflowResult> results = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            results.add(createSuccessResult(100 + i, 50));
        }

        long startNanos = 0;
        long endNanos = 10_000_000_000L; // 10 seconds
        BatchMetrics metrics = BatchMetrics.from(results, startNanos, endNanos);

        assertThat(metrics.throughputOpsPerSec()).isCloseTo(10.0, within(0.1));
    }

    @Test
    public void testSuccessRateCalculation() {
        List<WorkflowResult> results = new ArrayList<>();
        results.add(createSuccessResult(100, 10));
        results.add(createSuccessResult(101, 20));
        results.add(createFailureResult(102, new Exception("Failed")));
        results.add(createFailureResult(103, new Exception("Failed")));

        BatchMetrics metrics = BatchMetrics.from(results, 0L, 4000L);

        // 2 success out of 4 = 50%
        assertThat(metrics.successRate()).isCloseTo(50.0, within(0.1));
        assertThat(metrics.failureRate()).isCloseTo(50.0, within(0.1));
    }

    @Test
    public void testEmptyLatencyListReturnsZero() {
        // All workflows failed, so no latencies to calculate
        List<WorkflowResult> results = new ArrayList<>();
        results.add(createFailureResult(100, new Exception("Failed")));
        results.add(createFailureResult(101, new Exception("Failed")));

        BatchMetrics metrics = BatchMetrics.from(results, 0L, 2000L);

        assertThat(metrics.p50LatencyMs()).isEqualTo(0L);
        assertThat(metrics.p99LatencyMs()).isEqualTo(0L);
    }

    @Test
    public void testNullResultsThrowsException() {
        assertThatThrownBy(() -> BatchMetrics.from(null, 0L, 1000L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Results cannot be empty");
    }

    @Test
    public void testEmptyResultsThrowsException() {
        assertThatThrownBy(() -> BatchMetrics.from(new ArrayList<>(), 0L, 1000L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Results cannot be empty");
    }

    /**
     * Helper to create successful workflow result.
     */
    private WorkflowResult createSuccessResult(long workflowId, long latencyMs) {
        return WorkflowResult.success(
            "row_" + workflowId,
            latencyMs,
            "/artifacts/result_" + workflowId
        );
    }

    /**
     * Helper to create failed workflow result.
     */
    private WorkflowResult createFailureResult(long workflowId, Exception error) {
        return WorkflowResult.failure(
            "row_" + workflowId,
            0L,
            error
        );
    }
}
