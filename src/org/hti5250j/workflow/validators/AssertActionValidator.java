/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;
import java.util.Map;

public class AssertActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        String screen = step.getScreen();
        if (screen == null || screen.isBlank()) {
            screen = getFromFields(step, "screen");
        }

        String text = step.getText();
        if (text == null || text.isBlank()) {
            text = getFromFields(step, "text");
        }

        boolean hasScreen = screen != null && !screen.isBlank();
        boolean hasText = text != null && !text.isBlank();

        if (!hasScreen && !hasText) {
            result.addError(stepIndex, "assert", "ASSERT requires screen or text", "Add 'screen:' or 'text:' field");
        }

        return result;
    }

    private String getFromFields(StepDef step, String key) {
        Map<String, String> fields = step.getFields();
        return fields != null ? fields.get(key) : null;
    }
}
