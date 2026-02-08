/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow.validators;

import org.hti5250j.workflow.ActionValidator;
import org.hti5250j.workflow.StepDef;
import org.hti5250j.workflow.ValidationResult;
import java.util.Map;

public class LoginActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        String host = step.getHost();
        if (host == null || host.isBlank()) {
            host = getFromFields(step, "host");
            if (host == null || host.isBlank()) {
                result.addError(stepIndex, "host", "LOGIN requires host", "Add 'host:' field");
            }
        }

        String user = step.getUser();
        if (user == null || user.isBlank()) {
            user = getFromFields(step, "user");
            if (user == null || user.isBlank()) {
                result.addError(stepIndex, "user", "LOGIN requires user", "Add 'user:' field");
            }
        }

        String password = step.getPassword();
        if (password == null || password.isBlank()) {
            password = getFromFields(step, "password");
            if (password == null || password.isBlank()) {
                result.addError(stepIndex, "password", "LOGIN requires password", "Add 'password:' field");
            }
        }

        return result;
    }

    private String getFromFields(StepDef step, String key) {
        Map<String, String> fields = step.getFields();
        return fields != null ? fields.get(key) : null;
    }
}
