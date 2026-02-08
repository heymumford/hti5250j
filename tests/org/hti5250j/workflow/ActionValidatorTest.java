/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.hti5250j.workflow.validators.*;
import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActionValidatorTest {

    @Test
    void testLoginActionValidation() {
        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        step.setHost("example.com");
        step.setUser("testuser");
        step.setPassword("testpass");

        ValidationResult result = new LoginActionValidator().validate(step, 0);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testLoginRequiresHost() {
        StepDef step = new StepDef();
        step.setUser("user");
        step.setPassword("pass");

        ValidationResult result = new LoginActionValidator().validate(step, 0);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(e ->
            assertThat(e.fieldName()).isEqualTo("host")
        );
    }

    @Test
    void testLoginRequiresUser() {
        StepDef step = new StepDef();
        step.setHost("example.com");
        step.setPassword("pass");

        ValidationResult result = new LoginActionValidator().validate(step, 0);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testLoginRequiresPassword() {
        StepDef step = new StepDef();
        step.setHost("example.com");
        step.setUser("user");

        ValidationResult result = new LoginActionValidator().validate(step, 0);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testNavigateActionValidation() {
        StepDef step = new StepDef();
        step.setScreen("MAIN_MENU");

        ValidationResult result = new NavigateActionValidator().validate(step, 0);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testNavigateRequiresScreen() {
        StepDef step = new StepDef();

        ValidationResult result = new NavigateActionValidator().validate(step, 0);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testFillActionValidation() {
        StepDef step = new StepDef();
        step.setFields(Map.of("username", "john", "password", "secret"));

        ValidationResult result = new FillActionValidator().validate(step, 0);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testFillRequiresFields() {
        StepDef step = new StepDef();

        ValidationResult result = new FillActionValidator().validate(step, 0);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testSubmitActionValidation() {
        StepDef step = new StepDef();
        step.setKey("ENTER");

        ValidationResult result = new SubmitActionValidator().validate(step, 0);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testSubmitRequiresKey() {
        StepDef step = new StepDef();

        ValidationResult result = new SubmitActionValidator().validate(step, 0);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testAssertActionWithScreen() {
        StepDef step = new StepDef();
        step.setScreen("confirmation");

        ValidationResult result = new AssertActionValidator().validate(step, 0);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testAssertActionWithText() {
        StepDef step = new StepDef();
        step.setText("Success");

        ValidationResult result = new AssertActionValidator().validate(step, 0);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testAssertRequiresScreenOrText() {
        StepDef step = new StepDef();

        ValidationResult result = new AssertActionValidator().validate(step, 0);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testWaitActionValidation() {
        StepDef step = new StepDef();
        step.setTimeout(5000);

        ValidationResult result = new WaitActionValidator().validate(step, 0);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testWaitRequiresPositiveTimeout() {
        StepDef step = new StepDef();
        step.setTimeout(0);

        ValidationResult result = new WaitActionValidator().validate(step, 0);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testCaptureActionValidation() {
        StepDef step = new StepDef();
        step.setName("screenshot");

        ValidationResult result = new CaptureActionValidator().validate(step, 0);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testCaptureWarnsIfNoName() {
        StepDef step = new StepDef();

        ValidationResult result = new CaptureActionValidator().validate(step, 0);
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).hasSize(1);
    }
}
