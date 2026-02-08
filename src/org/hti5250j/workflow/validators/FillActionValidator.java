/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;

public class FillActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        if (step.getFields() == null || step.getFields().isEmpty()) {
            result.addError(stepIndex, "fields", "FILL requires fields", "Add 'fields:' Map with field names and values");
        }

        return result;
    }
}
