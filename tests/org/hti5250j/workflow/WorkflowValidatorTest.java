/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.hti5250j.workflow.ActionType;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowValidatorTest {

    @Test
    void testValidateValidWorkflow() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Test Workflow");

        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        Map<String, String> fields = Map.of(
            "host", "example.com",
            "user", "testuser",
            "password", "testpass"
        );
        step.setFields(fields);

        workflow.setSteps(List.of(step));

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void testValidateDetectsNullWorkflowName() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setSteps(List.of());

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(e ->
            assertThat(e.message()).contains("name")
        );
    }

    @Test
    void testValidateDetectsBlankWorkflowName() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("   ");
        workflow.setSteps(List.of());

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(e ->
            assertThat(e.message()).contains("name")
        );
    }

    @Test
    void testValidateDetectsEmptySteps() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Test");
        workflow.setSteps(List.of());

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(e ->
            assertThat(e.message()).contains("step")
        );
    }

    @Test
    void testValidateDetectsNullAction() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Test");

        StepDef step = new StepDef();
        step.setAction((ActionType) null);
        workflow.setSteps(List.of(step));

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(e ->
            assertThat(e.fieldName()).isEqualTo("action")
        );
    }

    @Test
    void testValidateMultipleSteps() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Multi-step");

        StepDef step1 = new StepDef();
        step1.setAction(ActionType.LOGIN);
        step1.setFields(Map.of(
            "host", "example.com",
            "user", "user",
            "password", "pass"
        ));

        StepDef step2 = new StepDef();
        step2.setAction(ActionType.NAVIGATE);
        step2.setFields(Map.of("screen", "MAIN"));

        workflow.setSteps(List.of(step1, step2));

        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        assertThat(result.isValid()).isTrue();
    }
}
