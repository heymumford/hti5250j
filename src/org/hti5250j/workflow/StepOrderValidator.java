package org.hti5250j.workflow;

import java.util.List;

/**
 * Validates step ordering constraints in workflows.
 * - LOGIN must be the first step
 * - SUBMIT must be preceded by data entry (FILL or other action)
 *
 * @since 0.12.0
 */
public class StepOrderValidator {

    /**
     * Validate step order in workflow.
     *
     * @param steps list of workflow steps
     * @return ValidationResult with errors if ordering constraints violated
     */
    public ValidationResult validate(List<StepDef> steps) {
        ValidationResult result = new ValidationResult();

        if (steps == null || steps.isEmpty()) {
            return result;
        }

        StepDef firstStep = steps.get(0);
        if (firstStep.getAction() != ActionType.LOGIN) {
            result.addError(
                0,
                "action",
                "Workflow must start with LOGIN step, found: " + firstStep.getAction(),
                "Move LOGIN step to position 0"
            );
        }

        for (int i = 0; i < steps.size(); i++) {
            StepDef step = steps.get(i);
            if (step.getAction() == ActionType.SUBMIT) {
                if (i == 0) {
                    result.addWarning(
                        i,
                        "action",
                        "SUBMIT at step 0 should typically be preceded by FILL or NAVIGATE action"
                    );
                } else {
                    StepDef prevStep = steps.get(i - 1);
                    if (prevStep.getAction() != ActionType.FILL && prevStep.getAction() != ActionType.NAVIGATE) {
                        result.addWarning(
                            i,
                            "action",
                            "SUBMIT should typically follow FILL or NAVIGATE action"
                        );
                    }
                }
            }
        }

        return result;
    }
}
