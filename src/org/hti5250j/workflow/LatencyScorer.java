/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Verifies workflow completed within declared time bounds.
 *
 * Scoring Logic:
 * - 1.0: Completed significantly under tolerance (< 80% of maxDurationMs)
 * - 0.5-0.9: Linear penalty as approaches limit
 * - 0.0: Exceeded tolerance (> maxDurationMs) OR timeout detected
 *
 * Penalty Calculation:
 * ```
 * if (actual > max) return 0.0;              // Failed: exceeded limit
 * if (actual < max * 0.8) return 1.0;        // Excellent: well under
 * return 1.0 - ((max - actual) / max);       // Linear penalty: scale [0.8, 1.0] → [0.2, 1.0]
 * ```
 *
 * Examples:
 * - max=5000ms, actual=3000ms (60%) → 1.0 (excellent)
 * - max=5000ms, actual=4000ms (80%) → 0.8 (good)
 * - max=5000ms, actual=5000ms (100%) → 0.0 (at limit)
 * - max=5000ms, actual=6000ms (120%) → 0.0 (exceeded)
 *
 * Contract: Returns confidence workflow will stay within SLA.
 */
public class LatencyScorer implements EvalScorer {
    /**
     * Evaluate latency of workflow execution.
     *
     * @param result WorkflowResult with latencyMs duration
     * @param tolerance WorkflowTolerance with maxDurationMs bound
     * @return 1.0 (excellent), 0.0-0.9 (penalty), 0.0 (failed/timeout)
     */
    @Override
    public double evaluate(WorkflowResult result, WorkflowTolerance tolerance) {
        if (result == null || tolerance == null) {
            return 0.0;  // Missing data = failed evaluation
        }

        long actual = result.latencyMs();
        long max = tolerance.maxDurationMs();

        // Timeout detection: latency is unreasonably high (negative or zero suggests error)
        if (actual < 0) {
            return 0.0;  // Negative latency = error
        }

        // Failed: at or exceeded tolerance (SLA violated at boundary)
        if (actual >= max) {
            return 0.0;  // At or exceeded limit
        }

        // Excellent: well under tolerance (80% buffer)
        if (actual < max * 0.8) {
            return 1.0;  // Excellent performance
        }

        // Linear penalty as approaches limit
        // At 80% of max: score = 1.0
        // At 100% of max: score = 0.0
        // Formula: 1.0 - ((max - actual) / max)
        double penalty = (double) (max - actual) / max;
        return 1.0 - penalty;
    }

    @Override
    public String scorerName() {
        return "Latency";
    }
}
