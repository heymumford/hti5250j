package org.hti5250j.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates step ordering constraints in workflows.
 * - LOGIN must be the first step
 * - SUBMIT must be preceded by data entry (FILL or other action)
 *
 * @since Phase 11
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

        // LOGIN must be first step
        StepDef firstStep = steps.get(0);
        if (firstStep.getAction() != ActionType.LOGIN) {
            result.addError(
                0,
                "action",
                "Workflow must start with LOGIN step, found: " + firstStep.getAction(),
                "Move LOGIN step to position 0"
            );
        }

        // Check for SUBMIT without preceding data actions
        for (int i = 0; i < steps.size(); i++) {
            StepDef step = steps.get(i);
            if (step.getAction() == ActionType.SUBMIT && i > 0) {
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

        return result;
    }
}
