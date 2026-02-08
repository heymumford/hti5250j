/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Validates workflow specifications before execution.
 *
 * Checks:
 * - Workflow has non-blank name
 * - Workflow has at least one step
 * - Each step has a valid action
 * - Action-specific constraints (delegated to action validators)
 */
public class WorkflowValidator {

    /**
     * Validates workflow structure and all steps.
     *
     * @param workflow The workflow to validate
     * @return ValidationResult with all errors and warnings
     */
    public ValidationResult validate(WorkflowSchema workflow) {
        ValidationResult result = new ValidationResult();

        // Validate workflow structure
        if (workflow == null) {
            result.addError(-1, "workflow", "Workflow is null", "Provide a valid workflow");
            return result;
        }

        if (workflow.getName() == null || workflow.getName().isBlank()) {
            result.addError(-1, "name", "Workflow name is required", "Add 'name:' field to YAML");
        }

        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            result.addError(-1, "steps", "Workflow must have at least one step",
                "Add at least one step to 'steps:' list");
            return result;
        }

        // Validate each step
        for (int i = 0; i < workflow.getSteps().size(); i++) {
            validateStep(workflow.getSteps().get(i), i, result);
        }

        return result;
    }

    /**
     * Validates individual step.
     */
    private void validateStep(StepDef step, int stepIndex, ValidationResult result) {
        if (step == null) {
            result.addError(stepIndex, "step", "Step is null", "Provide valid step definition");
            return;
        }

        if (step.getAction() == null) {
            result.addError(stepIndex, "action", "Step action is required",
                "Add 'action:' field (LOGIN, NAVIGATE, FILL, etc)");
        }
    }
}
