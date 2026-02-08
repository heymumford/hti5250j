/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;

public class SubmitActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        if (step.getKey() == null || step.getKey().isBlank()) {
            result.addError(stepIndex, "key", "SUBMIT requires key", "Add 'key:' field (F1, ENTER, etc)");
        }

        return result;
    }
}
