/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepValidatorTest {

    @Test
    void testValidateStepWithValidTimeout() {
        StepDef step = new StepDef();
        step.setAction(ActionType.WAIT);
        step.setTimeout(5000);

        StepValidator validator = new StepValidator();
        ValidationResult result = validator.validate(step, 0);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateDetectsTimeoutTooLow() {
        StepDef step = new StepDef();
        step.setAction(ActionType.WAIT);
        step.setTimeout(50);

        StepValidator validator = new StepValidator();
        ValidationResult result = validator.validate(step, 0);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(e ->
            assertThat(e.fieldName()).isEqualTo("timeout")
        );
    }

    @Test
    void testValidateDetectsTimeoutTooHigh() {
        StepDef step = new StepDef();
        step.setAction(ActionType.WAIT);
        step.setTimeout(600000);

        StepValidator validator = new StepValidator();
        ValidationResult result = validator.validate(step, 0);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testValidateAllowsNullTimeout() {
        StepDef step = new StepDef();
        step.setAction(ActionType.NAVIGATE);
        // no timeout

        StepValidator validator = new StepValidator();
        ValidationResult result = validator.validate(step, 0);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testValidateTimeoutBoundaries() {
        StepValidator validator = new StepValidator();

        // Minimum valid: 100ms
        StepDef step1 = new StepDef();
        step1.setAction(ActionType.WAIT);
        step1.setTimeout(100);
        assertThat(validator.validate(step1, 0).isValid()).isTrue();

        // Maximum valid: 300000ms
        StepDef step2 = new StepDef();
        step2.setAction(ActionType.WAIT);
        step2.setTimeout(300000);
        assertThat(validator.validate(step2, 0).isValid()).isTrue();
    }
}
