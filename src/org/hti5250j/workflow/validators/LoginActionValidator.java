/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;

public class LoginActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        if (step.getHost() == null || step.getHost().isBlank()) {
            result.addError(stepIndex, "host", "LOGIN requires host", "Add 'host:' field");
        }
        if (step.getUser() == null || step.getUser().isBlank()) {
            result.addError(stepIndex, "user", "LOGIN requires user", "Add 'user:' field");
        }
        if (step.getPassword() == null || step.getPassword().isBlank()) {
            result.addError(stepIndex, "password", "LOGIN requires password", "Add 'password:' field");
        }

        return result;
    }
}
