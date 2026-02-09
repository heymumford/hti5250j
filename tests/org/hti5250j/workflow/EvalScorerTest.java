/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for EvalScorer implementations (Phase 12E, D5-EVAL).
 *
 * Contract: Scorers measure workflow reliability against declared tolerances.
 * - CorrectnessScorer: Verify outputs are correct (no data loss)
 * - IdempotencyScorer: Verify retries produce same result
 * - LatencyScorer: Verify completion within time bounds
 *
 * Test IDs: D5-EVAL-001 through D5-EVAL-012 (4 per scorer)
 */
class EvalScorerTest {

    // ============================================================================
    // CorrectnessScorer Tests (4 tests)
    // ============================================================================

    /**
     * D5-EVAL-001: Correctness scorer scores successful workflow as 1.0.
     *
     * Setup: Workflow succeeds with correct outputs
     * Action: Score using CorrectnessScorer
     * Expected: Returns 1.0 (excellent confidence)
     */
    @Test
    void correctnessSuccessReturnsOne() {
        EvalScorer scorer = new CorrectnessScorer();
        WorkflowResult result = WorkflowResult.success("row1", 1000, "/artifact");
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(1.0);
        assertThat(scorer.scorerName()).isEqualTo("Correctness");
    }

    /**
     * D5-EVAL-002: Correctness scorer scores assertion failure as 0.5 (recoverable).
     *
     * Setup: Workflow fails due to assertion failure
     * Action: Score using CorrectnessScorer
     * Expected: Returns 0.5 (retry might succeed)
     */
    @Test
    void correctnessAssertionFailureReturnsHalf() {
        EvalScorer scorer = new CorrectnessScorer();
        WorkflowResult result = WorkflowResult.failure(
            "row1", 1000, new AssertionException("Expected 'OK' but got 'ERROR'", "SCREEN CONTENT")
        );
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(0.5);
    }

    /**
     * D5-EVAL-003: Correctness scorer scores truncation as 0.0 (critical failure).
     *
     * Setup: Workflow fails due to field truncation (data loss)
     * Action: Score using CorrectnessScorer
     * Expected: Returns 0.0 (cannot proceed, data lost)
     */
    @Test
    void correctnessTruncationReturnsZero() {
        EvalScorer scorer = new CorrectnessScorer();
        WorkflowResult result = WorkflowResult.failure(
            "row1", 1000, new RuntimeException("Field 'amount' truncated from 10 to 7 chars")
        );
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(0.0);
    }

    /**
     * D5-EVAL-004: Correctness scorer handles null result safely.
     *
     * Setup: Result is null (execution error)
     * Action: Score null result
     * Expected: Returns 0.0 (critical failure)
     */
    @Test
    void correctnessNullResultReturnsZero() {
        EvalScorer scorer = new CorrectnessScorer();
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        double score = scorer.evaluate(null, tolerance);

        assertThat(score).isEqualTo(0.0);
    }

    // ============================================================================
    // IdempotencyScorer Tests (4 tests)
    // ============================================================================

    /**
     * D5-EVAL-005: Idempotency scorer scores successful execution as 1.0.
     *
     * Setup: Workflow succeeds consistently
     * Action: Score using IdempotencyScorer
     * Expected: Returns 1.0 (idempotent, will retry same way)
     */
    @Test
    void idempotencySuccessReturnsOne() {
        EvalScorer scorer = new IdempotencyScorer();
        WorkflowResult result = WorkflowResult.success("row1", 1000, "/artifact");
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(1.0);
        assertThat(scorer.scorerName()).isEqualTo("Idempotency");
    }

    /**
     * D5-EVAL-006: Idempotency scorer scores timeout as 0.5 (potentially idempotent).
     *
     * Setup: Workflow fails due to timeout (timing-dependent)
     * Action: Score using IdempotencyScorer
     * Expected: Returns 0.5 (might succeed on retry with different timing)
     */
    @Test
    void idempotencyTimeoutReturnsHalf() {
        EvalScorer scorer = new IdempotencyScorer();
        WorkflowResult result = WorkflowResult.failure(
            "row1", 1000, new RuntimeException("timeout waiting for keyboard")
        );
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(0.5);
    }

    /**
     * D5-EVAL-007: Idempotency scorer scores non-deterministic failure as 0.0.
     *
     * Setup: Workflow fails due to random cursor variation
     * Action: Score using IdempotencyScorer
     * Expected: Returns 0.0 (non-idempotent, different each time)
     */
    @Test
    void idempotencyNonDeterministicReturnsZero() {
        EvalScorer scorer = new IdempotencyScorer();
        WorkflowResult result = WorkflowResult.failure(
            "row1", 1000, new RuntimeException("cursor position varies randomly")
        );
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(0.0);
    }

    /**
     * D5-EVAL-008: Idempotency scorer scores navigation failure as 1.0 (idempotent).
     *
     * Setup: Navigation fails consistently (deterministic failure)
     * Action: Score using IdempotencyScorer
     * Expected: Returns 1.0 (same failure each retry)
     */
    @Test
    void idempotencyNavigationFailureReturnsOne() {
        EvalScorer scorer = new IdempotencyScorer();
        WorkflowResult result = WorkflowResult.failure(
            "row1", 1000, new NavigationException("Screen 'MAIN' not reached")
        );
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(1.0);
    }

    // ============================================================================
    // LatencyScorer Tests (4 tests)
    // ============================================================================

    /**
     * D5-EVAL-009: Latency scorer scores under 80% of tolerance as 1.0 (excellent).
     *
     * Setup: Workflow completes at 50% of tolerance (well under)
     * Action: Score using LatencyScorer
     * Expected: Returns 1.0 (excellent latency)
     */
    @Test
    void latencyWellUnderToleranceReturnsOne() {
        EvalScorer scorer = new LatencyScorer();
        WorkflowResult result = WorkflowResult.success("row1", 2500, "/artifact");  // 50% of 5000ms
        WorkflowTolerance tolerance = new WorkflowTolerance("test", 5000L, 0.01, 3, false);

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(1.0);
    }

    /**
     * D5-EVAL-010: Latency scorer applies linear penalty approaching tolerance.
     *
     * Setup: Workflow completes at 90% of tolerance (approaching limit)
     * Action: Score using LatencyScorer
     * Expected: Returns ~0.5 (linear penalty: 1.0 - (10% / 100%) = 0.9)
     */
    @Test
    void latencyLinearPenaltyNearTolerance() {
        EvalScorer scorer = new LatencyScorer();
        WorkflowResult result = WorkflowResult.success("row1", 4500, "/artifact");  // 90% of 5000ms
        WorkflowTolerance tolerance = new WorkflowTolerance("test", 5000L, 0.01, 3, false);

        double score = scorer.evaluate(result, tolerance);

        // Linear penalty: 1.0 - ((5000 - 4500) / 5000) = 1.0 - 0.1 = 0.9
        assertThat(score).isCloseTo(0.9, within(0.01));
    }

    /**
     * D5-EVAL-011: Latency scorer scores at tolerance boundary as 0.0 (failed).
     *
     * Setup: Workflow completes exactly at tolerance limit
     * Action: Score using LatencyScorer
     * Expected: Returns 0.0 (at boundary, SLA violated)
     */
    @Test
    void latencyAtToleranceBoundaryReturnsZero() {
        EvalScorer scorer = new LatencyScorer();
        WorkflowResult result = WorkflowResult.success("row1", 5000, "/artifact");  // 100% of 5000ms
        WorkflowTolerance tolerance = new WorkflowTolerance("test", 5000L, 0.01, 3, false);

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(0.0);
    }

    /**
     * D5-EVAL-012: Latency scorer scores exceeded tolerance as 0.0 (failed).
     *
     * Setup: Workflow exceeds tolerance (6000ms > 5000ms limit)
     * Action: Score using LatencyScorer
     * Expected: Returns 0.0 (timeout, SLA violated)
     */
    @Test
    void latencyExceededToleranceReturnsZero() {
        EvalScorer scorer = new LatencyScorer();
        WorkflowResult result = WorkflowResult.success("row1", 6000, "/artifact");  // 120% of 5000ms
        WorkflowTolerance tolerance = new WorkflowTolerance("test", 5000L, 0.01, 3, false);

        double score = scorer.evaluate(result, tolerance);

        assertThat(score).isEqualTo(0.0);
    }
}
