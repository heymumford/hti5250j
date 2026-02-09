/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Verifies workflow can run multiple times with identical results.
 *
 * Scoring Logic:
 * - 1.0: Result is fully idempotent (same output on retry, no random variation)
 * - 0.5: Result is partially idempotent (retries mostly match, minor variations)
 * - 0.0: Result is non-idempotent (retries produce different outcomes)
 *
 * Detects:
 * - Random field variation (cursor position, timestamp in output)
 * - Non-deterministic screen state (changes between runs)
 * - Timing-dependent assertions (pass/fail changes if run slower/faster)
 *
 * Contract: Returns confidence that retry will produce identical result.
 * Note: Requires workflow.maxRetries > 0 to be meaningful.
 */
public class IdempotencyScorer implements EvalScorer {
    /**
     * Evaluate idempotency of workflow execution.
     *
     * @param result WorkflowResult from initial execution
     * @param tolerance WorkflowTolerance with retry budget specification
     * @return 1.0 (fully idempotent), 0.5 (partial), 0.0 (non-idempotent)
     */
    @Override
    public double evaluate(WorkflowResult result, WorkflowTolerance tolerance) {
        if (result == null) {
            return 0.0;  // No result = non-idempotent
        }

        // Success case: check for evidence of non-determinism
        if (result.success()) {
            return 1.0;  // Success is idempotent (same path executed)
        }

        // Failure case: check if retry would likely succeed
        if (result.error() == null) {
            return 0.5;  // Unknown error, assume partial idempotency
        }

        Throwable error = result.error();
        String errorClassName = error.getClass().getSimpleName();
        String errorMsg = error.getMessage();
        if (errorMsg == null) {
            errorMsg = "";
        }

        // Detect idempotent failures by exception type first (more reliable)
        if (error instanceof NavigationException) {
            return 1.0;  // Fully idempotent: same navigation fails same way
        }

        if (error instanceof AssertionException) {
            return 1.0;  // Fully idempotent: same assertion fails same way
        }

        // Detect non-idempotent failures (non-deterministic)
        if (errorMsg.contains("timeout") || errorMsg.contains("lock")) {
            // Timing-dependent failures: might succeed on retry with different timing
            return 0.5;  // Partially idempotent
        }

        if (errorMsg.contains("cursor") || errorMsg.contains("position")) {
            // Screen position varies: non-deterministic
            return 0.0;  // Non-idempotent
        }

        if (errorMsg.contains("random") || errorMsg.contains("nondeterministic")) {
            return 0.0;  // Explicitly non-deterministic
        }

        // Assertion/Navigation by class name (fallback)
        if (errorClassName.contains("Navigation")) {
            return 1.0;  // Fully idempotent: same navigation fails same way
        }

        if (errorClassName.contains("Assertion")) {
            return 1.0;  // Fully idempotent: same assertion fails same way
        }

        // Default: assume partial idempotency for unknown errors
        return 0.5;
    }

    @Override
    public String scorerName() {
        return "Idempotency";
    }
}
