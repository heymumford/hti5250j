/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end validation tests combining all validators.
 */
class WorkflowValidationIntegrationTest {

    @Test
    void testValidCompleteLoginWorkflow() {
        WorkflowSchema workflow = createLoginWorkflow("Test", "example.com", "user", "pass", 5000);

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testInvalidWorkflowMissingName() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setSteps(List.of(createLoginStep("example.com", "user", "pass")));

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(e ->
            assertThat(e.fieldName()).isEqualTo("name")
        );
    }

    @Test
    void testInvalidWorkflowMissingLoginHost() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Test");

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setUser("user");
        loginStep.setPassword("pass");

        workflow.setSteps(List.of(loginStep));

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void testWorkflowWithParameterValidation() {
        WorkflowSchema workflow = createLoginWorkflow(
            "Parameterized",
            "${data.hostname}",
            "${data.user}",
            "${data.pass}",
            5000
        );

        Map<String, Object> dataset = Map.of(
            "hostname", "example.com",
            "user", "john",
            "pass", "secret"
        );

        ParameterValidator paramValidator = new ParameterValidator();
        ValidationResult paramResult = paramValidator.validate(workflow, dataset);

        assertThat(paramResult.getWarnings()).isEmpty();
    }

    @Test
    void testWorkflowWithMissingDatasetColumns() {
        WorkflowSchema workflow = createLoginWorkflow(
            "Parameterized",
            "${data.hostname}",
            "${data.user}",
            "${data.pass}",
            5000
        );

        Map<String, Object> dataset = Map.of("hostname", "example.com");

        ParameterValidator paramValidator = new ParameterValidator();
        ValidationResult paramResult = paramValidator.validate(workflow, dataset);

        assertThat(paramResult.getWarnings()).hasSize(2);
    }

    @Test
    void testMultiStepWorkflow() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Multi-step");

        StepDef loginStep = createLoginStep("example.com", "user", "pass");

        StepDef navStep = new StepDef();
        navStep.setAction(ActionType.NAVIGATE);
        navStep.setScreen("MAIN");

        StepDef fillStep = new StepDef();
        fillStep.setAction(ActionType.FILL);
        fillStep.setFields(Map.of("username", "testuser", "password", "testpass"));

        workflow.setSteps(List.of(loginStep, navStep, fillStep));

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void testWorkflowWithTimeoutValidation() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Timeout Test");

        StepDef waitStep = new StepDef();
        waitStep.setAction(ActionType.WAIT);
        waitStep.setTimeout(50); // Invalid: too low

        workflow.setSteps(List.of(waitStep));

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isFalse();
    }

    // Helper methods

    private WorkflowSchema createLoginWorkflow(String name, String host, String user, String password, int timeout) {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName(name);
        workflow.setSteps(List.of(createLoginStep(host, user, password)));
        return workflow;
    }

    private StepDef createLoginStep(String host, String user, String password) {
        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        step.setHost(host);
        step.setUser(user);
        step.setPassword(password);
        return step;
    }
}
