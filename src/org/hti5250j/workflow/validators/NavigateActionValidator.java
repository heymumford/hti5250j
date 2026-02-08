/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;
import java.util.Map;

public class NavigateActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        String screen = step.getScreen();
        if (screen == null || screen.isBlank()) {
            screen = getFromFields(step, "screen");
            if (screen == null || screen.isBlank()) {
                result.addError(stepIndex, "screen", "NAVIGATE requires screen", "Add 'screen:' field");
            }
        }

        return result;
    }

    private String getFromFields(StepDef step, String key) {
        Map<String, String> fields = step.getFields();
        return fields != null ? fields.get(key) : null;
    }
}
