/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Evaluates workflow reliability against declared tolerances.
 *
 * Contract: Score represents confidence that workflow meets tolerance bounds.
 * - 1.0 = excellent (well within bounds)
 * - 0.5 = acceptable (borderline acceptable)
 * - 0.0 = failed (violation detected)
 *
 * Implementations:
 * - CorrectnessScorer: Verify workflow produced correct outputs
 * - IdempotencyScorer: Verify retries produce identical results
 * - LatencyScorer: Verify completion within time bounds
 *
 * Usage:
 * ```java
 * EvalScorer scorer = new CorrectnessScorer();
 * double score = scorer.evaluate(result, tolerance);
 * System.out.println(scorer.scorerName() + ": " + score);
 * ```
 */
public interface EvalScorer {
    /**
     * Evaluate workflow result against tolerance bounds.
     *
     * @param result WorkflowResult with outcome, latency, fields
     * @param tolerance WorkflowTolerance with bounds specification
     * @return Score 0.0-1.0 (1.0=excellent, 0.0=failed)
     */
    double evaluate(WorkflowResult result, WorkflowTolerance tolerance);

    /**
     * Human-readable scorer name.
     *
     * @return e.g., "Correctness", "Idempotency", "Latency"
     */
    String scorerName();
}
