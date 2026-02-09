/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Simulation result for offline workflow dry-run.
 *
 * Contract: Predicts workflow outcome without real i5 connection.
 * Used by human approval gates to verify safety before execution.
 *
 * @param steps Simulated step results (sequence of predictions)
 * @param predictedOutcome Overall prediction: "success" | "timeout" | "validation_error"
 * @param predictedFields Expected output field values after workflow
 * @param warnings Warnings about potential data loss, truncation, precision loss
 */
public record WorkflowSimulation(
    List<SimulatedStep> steps,
    String predictedOutcome,
    Map<String, String> predictedFields,
    List<String> warnings
) {
    /**
     * Check if simulation predicts success.
     *
     * @return true if predictedOutcome == "success"
     */
    public boolean predictSuccess() {
        return "success".equals(predictedOutcome);
    }

    /**
     * Check if simulation has warnings.
     *
     * @return true if warnings list is not empty
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * Get simulation summary for logging.
     *
     * @return Human-readable summary (e.g., "success: 4 steps, 2 warnings")
     */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(predictedOutcome).append(": ");
        sb.append(steps.size()).append(" steps");

        if (hasWarnings()) {
            sb.append(", ").append(warnings.size()).append(" warnings");
        }

        return sb.toString();
    }
}

/**
 * Single simulated step result.
 *
 * @param stepIndex Index in workflow (0-based)
 * @param stepName Action type (LOGIN, NAVIGATE, FILL, SUBMIT, ASSERT, CAPTURE, WAIT)
 * @param prediction Outcome: "success" | "timeout" | "error"
 * @param warning Optional warning if step has risk (e.g., field truncation)
 */
record SimulatedStep(
    int stepIndex,
    String stepName,
    String prediction,
    Optional<String> warning
) {
    /**
     * Check if step prediction is success.
     *
     * @return true if prediction == "success"
     */
    boolean isSuccess() {
        return "success".equals(prediction);
    }

    /**
     * Check if step has warning.
     *
     * @return true if warning is present
     */
    boolean hasWarning() {
        return warning.isPresent();
    }
}
