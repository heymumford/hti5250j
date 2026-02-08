/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;

public class WaitActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        if (step.getTimeout() != null && step.getTimeout() <= 0) {
            result.addError(stepIndex, "timeout", "WAIT timeout must be > 0", "Set timeout to positive milliseconds");
        }

        return result;
    }
}
