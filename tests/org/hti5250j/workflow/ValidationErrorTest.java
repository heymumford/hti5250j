/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationErrorTest {

    @Test
    void testValidationErrorWithSuggestedFix() {
        ValidationError error = new ValidationError(
            0, "timeout", "Timeout must be integer, got: hello",
            "Change to numeric value (milliseconds)"
        );

        assertThat(error.stepIndex()).isEqualTo(0);
        assertThat(error.fieldName()).isEqualTo("timeout");
        assertThat(error.message()).contains("integer");
        assertThat(error.suggestedFix()).contains("numeric");
    }

    @Test
    void testValidationErrorAccessors() {
        ValidationError error = new ValidationError(5, "action", "Action required", "Add action");

        assertThat(error.stepIndex()).isEqualTo(5);
        assertThat(error.fieldName()).isEqualTo("action");
        assertThat(error.message()).isEqualTo("Action required");
        assertThat(error.suggestedFix()).isEqualTo("Add action");
    }

    @Test
    void testValidationWarningNoFix() {
        ValidationWarning warning = new ValidationWarning(
            1, "timeout", "Timeout 60000ms exceeds recommended 30000ms"
        );

        assertThat(warning.stepIndex()).isEqualTo(1);
        assertThat(warning.fieldName()).isEqualTo("timeout");
        assertThat(warning.message()).contains("60000ms");
    }

    @Test
    void testValidationWarningAccessors() {
        ValidationWarning warning = new ValidationWarning(3, "delay", "Long delay detected");

        assertThat(warning.stepIndex()).isEqualTo(3);
        assertThat(warning.fieldName()).isEqualTo("delay");
        assertThat(warning.message()).isEqualTo("Long delay detected");
    }
}
