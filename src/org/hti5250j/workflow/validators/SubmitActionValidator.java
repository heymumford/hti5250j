/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;
import java.util.Map;

public class SubmitActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        String key = step.getKey();
        if (key == null || key.isBlank()) {
            key = getFromFields(step, "key");
            if (key == null || key.isBlank()) {
                result.addError(stepIndex, "key", "SUBMIT requires key", "Add 'key:' field (F1, ENTER, etc)");
            }
        }

        return result;
    }

    private String getFromFields(StepDef step, String key) {
        Map<String, String> fields = step.getFields();
        return fields != null ? fields.get(key) : null;
    }
}
