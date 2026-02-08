/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationResultTest {

    @Test
    void testValidationResultAccumulates() {
        ValidationResult result = new ValidationResult();

        assertThat(result.isValid()).isTrue();

        result.addError(-1, "host", "Host is required", "Add host field");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);

        result.addWarning(1, "timeout", "Timeout exceeds recommended max");
        assertThat(result.getWarnings()).hasSize(1);
    }

    @Test
    void testMergeResults() {
        ValidationResult result1 = new ValidationResult();
        result1.addError(0, "action", "Action is null", "Add action field");

        ValidationResult result2 = new ValidationResult();
        result2.addWarning(1, "timeout", "High timeout value");

        result1.merge(result2);

        assertThat(result1.getErrors()).hasSize(1);
        assertThat(result1.getWarnings()).hasSize(1);
        assertThat(result1.isValid()).isFalse();
    }

    @Test
    void testEmptyValidationResultIsValid() {
        ValidationResult result = new ValidationResult();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    void testMultipleErrorsAndWarnings() {
        ValidationResult result = new ValidationResult();

        result.addError(0, "field1", "Error 1", "Fix 1");
        result.addError(1, "field2", "Error 2", "Fix 2");
        result.addWarning(0, "field3", "Warning 1");
        result.addWarning(1, "field4", "Warning 2");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getWarnings()).hasSize(2);
    }
}
