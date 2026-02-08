/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;

public class NavigateActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        if (step.getScreen() == null || step.getScreen().isBlank()) {
            result.addError(stepIndex, "screen", "NAVIGATE requires screen", "Add 'screen:' field");
        }

        return result;
    }
}
