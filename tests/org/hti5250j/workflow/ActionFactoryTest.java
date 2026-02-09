/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Map;

/**
 * Tests for ActionFactory conversion from StepDef to typed Action objects.
 */
class ActionFactoryTest {

    /**
     * Test LOGIN action conversion.
     */
    @Test
    void testLoginActionConversion() {
        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        step.setHost("test.example.com");
        step.setUser("testuser");
        step.setPassword("testpass");

        Action action = ActionFactory.from(step);

        assertThat(action).isInstanceOf(LoginAction.class);
        LoginAction login = (LoginAction) action;
        assertThat(login.host()).isEqualTo("test.example.com");
        assertThat(login.user()).isEqualTo("testuser");
        assertThat(login.password()).isEqualTo("testpass");
    }

    /**
     * Test NAVIGATE action conversion.
     */
    @Test
    void testNavigateActionConversion() {
        StepDef step = new StepDef();
        step.setAction(ActionType.NAVIGATE);
        step.setScreen("Main Menu");
        step.setKeys("[pf3]");

        Action action = ActionFactory.from(step);

        assertThat(action).isInstanceOf(NavigateAction.class);
        NavigateAction nav = (NavigateAction) action;
        assertThat(nav.screen()).isEqualTo("Main Menu");
        assertThat(nav.keys()).isEqualTo("[pf3]");
    }

    /**
     * Test FILL action conversion.
     */
    @Test
    void testFillActionConversion() {
        StepDef step = new StepDef();
        step.setAction(ActionType.FILL);
        Map<String, String> fields = Map.of("account", "123456", "amount", "1000");
        step.setFields(fields);

        Action action = ActionFactory.from(step);

        assertThat(action).isInstanceOf(FillAction.class);
        FillAction fill = (FillAction) action;
        assertThat(fill.fields()).isEqualTo(fields);
    }

    /**
     * Test SUBMIT action conversion.
     */
    @Test
    void testSubmitActionConversion() {
        StepDef step = new StepDef();
        step.setAction(ActionType.SUBMIT);
        step.setKey("enter");

        Action action = ActionFactory.from(step);

        assertThat(action).isInstanceOf(SubmitAction.class);
        SubmitAction submit = (SubmitAction) action;
        assertThat(submit.key()).isEqualTo("enter");
    }

    /**
     * Test ASSERT action conversion with text.
     */
    @Test
    void testAssertActionConversionWithText() {
        StepDef step = new StepDef();
        step.setAction(ActionType.ASSERT);
        step.setText("Transaction successful");

        Action action = ActionFactory.from(step);

        assertThat(action).isInstanceOf(AssertAction.class);
        AssertAction assert_ = (AssertAction) action;
        assertThat(assert_.text()).isEqualTo("Transaction successful");
    }

    /**
     * Test ASSERT action conversion with screen.
     */
    @Test
    void testAssertActionConversionWithScreen() {
        StepDef step = new StepDef();
        step.setAction(ActionType.ASSERT);
        step.setScreen("Confirmation");

        Action action = ActionFactory.from(step);

        assertThat(action).isInstanceOf(AssertAction.class);
        AssertAction assert_ = (AssertAction) action;
        assertThat(assert_.screen()).isEqualTo("Confirmation");
    }

    /**
     * Test WAIT action conversion.
     */
    @Test
    void testWaitActionConversion() {
        StepDef step = new StepDef();
        step.setAction(ActionType.WAIT);
        step.setTimeout(5000);

        Action action = ActionFactory.from(step);

        assertThat(action).isInstanceOf(WaitAction.class);
        WaitAction wait = (WaitAction) action;
        assertThat(wait.timeout()).isEqualTo(5000);
    }

    /**
     * Test CAPTURE action conversion.
     */
    @Test
    void testCaptureActionConversion() {
        StepDef step = new StepDef();
        step.setAction(ActionType.CAPTURE);
        step.setName("confirmation_screen");

        Action action = ActionFactory.from(step);

        assertThat(action).isInstanceOf(CaptureAction.class);
        CaptureAction capture = (CaptureAction) action;
        assertThat(capture.name()).isEqualTo("confirmation_screen");
    }

    /**
     * Test all 7 ActionType values are handled by factory.
     */
    @Test
    void testAllActionTypesHandled() {
        // This test ensures compiler exhaustiveness checking works
        // If a new ActionType is added, this test should still compile
        // and ActionFactory.from() should handle it

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("h");
        loginStep.setUser("u");
        loginStep.setPassword("p");

        StepDef navStep = new StepDef();
        navStep.setAction(ActionType.NAVIGATE);
        navStep.setScreen("s");
        navStep.setKeys("k");

        StepDef fillStep = new StepDef();
        fillStep.setAction(ActionType.FILL);
        fillStep.setFields(Map.of("f", "v"));

        StepDef submitStep = new StepDef();
        submitStep.setAction(ActionType.SUBMIT);
        submitStep.setKey("enter");

        StepDef assertStep = new StepDef();
        assertStep.setAction(ActionType.ASSERT);
        assertStep.setText("text");

        StepDef waitStep = new StepDef();
        waitStep.setAction(ActionType.WAIT);
        waitStep.setTimeout(1000);

        StepDef captureStep = new StepDef();
        captureStep.setAction(ActionType.CAPTURE);
        captureStep.setName("name");

        // All should convert without error
        assertThat(ActionFactory.from(loginStep)).isInstanceOf(LoginAction.class);
        assertThat(ActionFactory.from(navStep)).isInstanceOf(NavigateAction.class);
        assertThat(ActionFactory.from(fillStep)).isInstanceOf(FillAction.class);
        assertThat(ActionFactory.from(submitStep)).isInstanceOf(SubmitAction.class);
        assertThat(ActionFactory.from(assertStep)).isInstanceOf(AssertAction.class);
        assertThat(ActionFactory.from(waitStep)).isInstanceOf(WaitAction.class);
        assertThat(ActionFactory.from(captureStep)).isInstanceOf(CaptureAction.class);
    }

    /**
     * Test null StepDef throws NullPointerException.
     */
    @Test
    void testNullStepDefThrowsNPE() {
        assertThatThrownBy(() -> ActionFactory.from(null))
            .isInstanceOf(NullPointerException.class);
    }

    /**
     * Test LoginAction rejects null host.
     */
    @Test
    void testLoginActionValidation_NullHost() {
        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        step.setHost(null);
        step.setUser("u");
        step.setPassword("p");

        assertThatThrownBy(() -> ActionFactory.from(step))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("host");
    }

    /**
     * Test NavigateAction rejects null screen.
     */
    @Test
    void testNavigateActionValidation_NullScreen() {
        StepDef step = new StepDef();
        step.setAction(ActionType.NAVIGATE);
        step.setScreen(null);
        step.setKeys("k");

        assertThatThrownBy(() -> ActionFactory.from(step))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("screen");
    }

    /**
     * Test FillAction rejects null fields.
     */
    @Test
    void testFillActionValidation_NullFields() {
        StepDef step = new StepDef();
        step.setAction(ActionType.FILL);
        step.setFields(null);

        assertThatThrownBy(() -> ActionFactory.from(step))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fields");
    }

    /**
     * Test AssertAction rejects both null text and screen.
     */
    @Test
    void testAssertActionValidation_BothNull() {
        StepDef step = new StepDef();
        step.setAction(ActionType.ASSERT);
        step.setText(null);
        step.setScreen(null);

        assertThatThrownBy(() -> ActionFactory.from(step))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("text or screen");
    }

    /**
     * Test WaitAction rejects null timeout.
     */
    @Test
    void testWaitActionValidation_NullTimeout() {
        StepDef step = new StepDef();
        step.setAction(ActionType.WAIT);
        step.setTimeout(null);

        assertThatThrownBy(() -> ActionFactory.from(step))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");
    }
}
