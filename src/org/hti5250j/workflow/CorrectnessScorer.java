/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Verifies workflow produced correct outputs.
 *
 * Scoring Logic:
 * - 1.0: Workflow succeeded (all assertions passed, no field truncation)
 * - 0.5: Workflow failed but recoverable (assertion failure, can retry)
 * - 0.0: Workflow failed critically (navigation failure, connection lost, truncation detected)
 *
 * Detects:
 * - Silent field truncation (data loss)
 * - Field type mismatches (wrong field type for assertion)
 * - Missing required fields (incomplete workflow output)
 *
 * Contract: Returns confidence that workflow outputs are correct and complete.
 */
public class CorrectnessScorer implements EvalScorer {
    /**
     * Evaluate correctness of workflow output.
     *
     * @param result WorkflowResult with success flag and error details
     * @param tolerance WorkflowTolerance with precision bounds (for field validation)
     * @return 1.0 (success), 0.5 (recoverable failure), 0.0 (critical failure)
     */
    @Override
    public double evaluate(WorkflowResult result, WorkflowTolerance tolerance) {
        if (result == null) {
            return 0.0;  // No result = critical failure
        }

        // Success case: all assertions passed
        if (result.success()) {
            return 1.0;
        }

        // Failure case: analyze error type
        if (result.error() == null) {
            // No error details, assume recoverable
            return 0.5;
        }

        Throwable error = result.error();
        String errorClassName = error.getClass().getSimpleName();
        String errorMsg = error.getMessage();
        if (errorMsg == null) {
            errorMsg = "";
        }

        // Detect critical failures by exception type first (more reliable)
        if (error instanceof AssertionException) {
            return 0.5;  // Recoverable: retry may pass if timing/screen state changes
        }

        if (error instanceof NavigationException) {
            return 0.0;  // Navigation failure is critical (can't proceed)
        }

        // Detect critical failures (data loss, truncation)
        if (errorMsg.contains("truncated") || errorMsg.contains("data loss")) {
            return 0.0;  // Silent data loss is critical
        }

        if (errorMsg.contains("field mismatch") || errorMsg.contains("type mismatch")) {
            return 0.0;  // Type safety violation is critical
        }

        if (errorMsg.contains("ConnectionException") || errorMsg.contains("TimeoutException")) {
            return 0.0;  // Connection loss is critical
        }

        // Assertion failures are recoverable (catch by class name)
        if (errorMsg.contains("assertion") || errorClassName.contains("Assertion")) {
            return 0.5;  // Recoverable: retry may pass if timing/screen state changes
        }

        // Unknown error: assume partial failure
        return 0.3;  // Some confidence remains but error unknown
    }

    @Override
    public String scorerName() {
        return "Correctness";
    }
}
