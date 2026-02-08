/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Validates individual step constraints.
 *
 * Checks:
 * - Timeout is within safe bounds (100ms - 300000ms / 5 minutes)
 */
public class StepValidator {
    private static final int MIN_TIMEOUT = 100;
    private static final int MAX_TIMEOUT = 300000; // 5 minutes

    /**
     * Validate step constraints.
     *
     * @param step Step to validate
     * @param stepIndex Index in workflow
     * @return ValidationResult with errors if any
     */
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        if (step == null) {
            result.addError(stepIndex, "step", "Step is null", "Provide valid step");
            return result;
        }

        // Validate timeout if present
        if (step.getTimeout() != null) {
            if (step.getTimeout() < MIN_TIMEOUT) {
                result.addError(stepIndex, "timeout",
                    "Timeout must be >= " + MIN_TIMEOUT + "ms",
                    "Increase timeout to at least " + MIN_TIMEOUT + "ms");
            } else if (step.getTimeout() > MAX_TIMEOUT) {
                result.addError(stepIndex, "timeout",
                    "Timeout must be <= " + MAX_TIMEOUT + "ms (5 minutes)",
                    "Reduce timeout or split into multiple steps");
            }
        }

        return result;
    }
}
