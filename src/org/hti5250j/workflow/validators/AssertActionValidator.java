/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;

public class AssertActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        boolean hasScreen = step.getScreen() != null && !step.getScreen().isBlank();
        boolean hasText = step.getText() != null && !step.getText().isBlank();

        if (!hasScreen && !hasText) {
            result.addError(stepIndex, "assert", "ASSERT requires screen or text", "Add 'screen:' or 'text:' field");
        }

        return result;
    }
}
