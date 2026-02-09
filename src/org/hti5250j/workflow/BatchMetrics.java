/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated metrics for batch workflow execution.
 * Tracks throughput, latency percentiles, and failure analysis.
 */
public record BatchMetrics(
    int totalWorkflows,
    int successCount,
    int failureCount,
    long p50LatencyMs,
    long p99LatencyMs,
    double throughputOpsPerSec,
    List<WorkflowResult> failures
) {
    /**
     * Compute batch metrics from individual workflow results.
     * Calculates latency percentiles and throughput.
     *
     * @param results all workflow execution results
     * @param startNanos batch start time (nanoseconds)
     * @param endNanos batch end time (nanoseconds)
     * @return batch metrics summary
     */
    public static BatchMetrics from(List<WorkflowResult> results, long startNanos, long endNanos) {
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Results cannot be empty");
        }

        // Count successes and failures
        int successCount = (int) results.stream().filter(WorkflowResult::success).count();
        int failureCount = results.size() - successCount;

        // Collect failures for diagnostics
        List<WorkflowResult> failures = new ArrayList<>();
        for (WorkflowResult result : results) {
            if (!result.success()) {
                failures.add(result);
            }
        }

        // Extract latencies and compute percentiles
        List<Long> latencies = new ArrayList<>();
        for (WorkflowResult result : results) {
            if (result.success()) {
                latencies.add(result.latencyMs());
            }
        }
        Collections.sort(latencies);

        // Use nearest-rank method: index = ceil(percentile * size) - 1
        // P50 on [10,20,30,40,50]: index = ceil(0.5*5) - 1 = 2 → 30 (correct middle)
        long p50 = latencies.isEmpty() ? 0 : latencies.get((int)Math.ceil(latencies.size() * 0.50) - 1);
        long p99 = latencies.isEmpty() ? 0 : latencies.get((int)Math.ceil(latencies.size() * 0.99) - 1);

        // Calculate throughput (workflows per second)
        long durationNanos = endNanos - startNanos;
        long durationMs = durationNanos / 1_000_000;
        double throughput = durationMs > 0 ? (results.size() * 1000.0) / durationMs : 0;

        return new BatchMetrics(results.size(), successCount, failureCount, p50, p99, throughput, failures);
    }

    /**
     * Print batch metrics to console with formatting.
     */
    public void print() {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("  BATCH EXECUTION METRICS");
        System.out.println("═".repeat(70));
        System.out.printf("  Total workflows:   %d%n", totalWorkflows);
        System.out.printf("  Success:           %d (%.1f%%)%n", successCount, (successCount * 100.0) / totalWorkflows);
        System.out.printf("  Failures:          %d (%.1f%%)%n", failureCount, (failureCount * 100.0) / totalWorkflows);
        System.out.println("─".repeat(70));
        System.out.printf("  P50 latency:       %dms%n", p50LatencyMs);
        System.out.printf("  P99 latency:       %dms%n", p99LatencyMs);
        System.out.printf("  Throughput:        %.1f workflows/sec%n", throughputOpsPerSec);
        System.out.println("═".repeat(70));

        // Print failures if any
        if (!failures.isEmpty()) {
            System.out.println("\n  FAILURES:");
            for (WorkflowResult failure : failures) {
                System.out.println("    " + failure.summary());
                if (failure.error() != null) {
                    System.out.println("      " + failure.error().getMessage());
                }
            }
            System.out.println();
        }
    }

    /**
     * Get failure rate as percentage.
     */
    public double failureRate() {
        return totalWorkflows > 0 ? (failureCount * 100.0) / totalWorkflows : 0;
    }

    /**
     * Get success rate as percentage.
     */
    public double successRate() {
        return 100.0 - failureRate();
    }
}
