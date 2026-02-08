/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Validates action-specific constraints.
 */
public interface ActionValidator {
    /**
     * Validate a step's action-specific requirements.
     *
     * @param step The step to validate
     * @param stepIndex Index in workflow
     * @return ValidationResult with errors if constraints violated
     */
    ValidationResult validate(StepDef step, int stepIndex);
}
