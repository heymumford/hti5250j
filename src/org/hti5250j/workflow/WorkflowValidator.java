/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.hti5250j.workflow.validators.*;

/**
 * Validates workflow specifications before execution.
 *
 * Delegates to:
 * - StepValidator for timeout bounds
 * - ActionValidators for action-specific constraints
 */
public class WorkflowValidator {
    private final StepValidator stepValidator = new StepValidator();
    private final StepOrderValidator stepOrderValidator = new StepOrderValidator();

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

        // Validate step ordering constraints
        ValidationResult orderResult = stepOrderValidator.validate(workflow.getSteps());
        result.merge(orderResult);

        // Validate each step
        for (int i = 0; i < workflow.getSteps().size(); i++) {
            ValidationResult stepResult = validateStep(workflow.getSteps().get(i), i);
            result.merge(stepResult);
        }

        return result;
    }

    /**
     * Validates individual step using step and action validators.
     * Also verifies that StepDef can be converted to typed Action (early validation).
     */
    private ValidationResult validateStep(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        if (step == null) {
            result.addError(stepIndex, "step", "Step is null", "Provide valid step definition");
            return result;
        }

        if (step.getAction() == null) {
            result.addError(stepIndex, "action", "Step action is required",
                "Add 'action:' field (LOGIN, NAVIGATE, FILL, etc)");
            return result;
        }

        // Validate timeout bounds
        result.merge(stepValidator.validate(step, stepIndex));

        // Verify StepDef can convert to typed Action (catches schema mismatches early)
        try {
            ActionFactory.from(step);
        } catch (IllegalArgumentException e) {
            result.addError(stepIndex, "action", "Step cannot be converted to action: " + e.getMessage(),
                "Verify all required fields for " + step.getAction() + " action are present");
            return result; // Stop validation if conversion fails
        }

        // Validate action-specific constraints
        ActionValidator actionValidator = getActionValidator(step.getAction());
        if (actionValidator != null) {
            result.merge(actionValidator.validate(step, stepIndex));
        }

        return result;
    }

    /**
     * Get the appropriate action validator for the given action type.
     */
    private ActionValidator getActionValidator(ActionType action) {
        return switch (action) {
            case LOGIN -> new LoginActionValidator();
            case NAVIGATE -> new NavigateActionValidator();
            case FILL -> new FillActionValidator();
            case SUBMIT -> new SubmitActionValidator();
            case ASSERT -> new AssertActionValidator();
            case WAIT -> new WaitActionValidator();
            case CAPTURE -> new CaptureActionValidator();
        };
    }
}
