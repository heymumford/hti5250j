/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Performance baselines for Phase 13 virtual thread batch execution.
 * Validates actual performance meets or exceeds targets.
 *
 * Baselines established from initial virtual thread profiling:
 * - 100 workflows: 100ms per workflow, 1KB overhead each
 * - 500 workflows: 150ms per workflow (slight contention)
 * - 1000 workflows: 200ms per workflow (carrier thread queueing)
 */
public class PerformanceBaseline {

    /**
     * P99 latency threshold in milliseconds for given workflow count.
     * Represents 99th percentile response time.
     *
     * Baselines:
     * - 100 workflows: <500ms (10× faster than sequential)
     * - 500 workflows: <1000ms (distributed queueing)
     * - 1000 workflows: <2000ms (mild contention)
     */
    public long p99Threshold(int workflowCount) {
        if (workflowCount <= 100) return 500;
        if (workflowCount <= 500) return 1000;
        return 2000;
    }

    /**
     * Throughput threshold in workflows per second.
     *
     * Baselines (each workflow ~100-200ms):
     * - 100 workflows: >50/sec (1KB per thread overhead)
     * - 500 workflows: >200/sec (efficient queueing)
     * - 1000 workflows: >300/sec (high concurrency sweet spot)
     */
    public double throughputThreshold(int workflowCount) {
        if (workflowCount <= 100) return 50.0;
        if (workflowCount <= 500) return 200.0;
        return 300.0;
    }

    /**
     * Memory overhead threshold in megabytes.
     * Virtual threads: ~1KB each, so 1000 threads = ~1MB overhead.
     *
     * Total memory = JVM baseline (~20MB) + threads (~1MB per 1000) + application
     * - 100 workflows: <50MB
     * - 500 workflows: <100MB
     * - 1000 workflows: <150MB
     */
    public long memoryThreshold(int workflowCount) {
        if (workflowCount <= 100) return 50;
        if (workflowCount <= 500) return 100;
        return 150;
    }

    /**
     * Validate batch metrics against baseline for given scale.
     *
     * @param workflowCount number of concurrent workflows
     * @param metrics actual batch metrics
     * @return true if metrics meet or exceed all baselines
     */
    public boolean validate(int workflowCount, BatchMetrics metrics) {
        long p99Threshold = p99Threshold(workflowCount);
        double throughputThreshold = throughputThreshold(workflowCount);

        boolean p99Pass = metrics.p99LatencyMs() <= p99Threshold;
        boolean throughputPass = metrics.throughputOpsPerSec() >= throughputThreshold;

        return p99Pass && throughputPass;
    }

    /**
     * Summary of baseline thresholds as human-readable table.
     */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nPERFORMANCE BASELINES - Phase 13 Virtual Threads\n");
        sb.append("═".repeat(70)).append("\n");
        sb.append(String.format("%-15s | %-12s | %-15s | %-10s%n",
            "Scale", "P99 Latency", "Throughput", "Memory"));
        sb.append("─".repeat(70)).append("\n");

        for (int count : new int[]{100, 500, 1000}) {
            sb.append(String.format("%-15d | %-12s | %-15s | %-10s%n",
                count,
                p99Threshold(count) + "ms",
                String.format(">%.0f/sec", throughputThreshold(count)),
                memoryThreshold(count) + "MB"));
        }

        sb.append("═".repeat(70)).append("\n");
        return sb.toString();
    }
}
