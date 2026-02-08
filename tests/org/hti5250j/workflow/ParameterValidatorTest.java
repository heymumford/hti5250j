/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterValidatorTest {

    @Test
    void testValidateParametersWithMatchingDataset() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Test");

        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        step.setHost("${data.hostname}");
        step.setUser("${data.username}");
        step.setPassword("${data.password}");

        workflow.setSteps(List.of(step));

        Map<String, Object> dataset = Map.of(
            "hostname", "example.com",
            "username", "user",
            "password", "pass"
        );

        ParameterValidator validator = new ParameterValidator();
        ValidationResult result = validator.validate(workflow, dataset);

        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    void testValidateDetectsMissingDatasetColumn() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Test");

        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        step.setHost("${data.hostname}");
        step.setUser("${data.missing_user}");

        workflow.setSteps(List.of(step));

        Map<String, Object> dataset = Map.of(
            "hostname", "example.com"
        );

        ParameterValidator validator = new ParameterValidator();
        ValidationResult result = validator.validate(workflow, dataset);

        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0).message()).contains("missing_user");
    }

    @Test
    void testValidateEmptyWorkflow() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setSteps(List.of());

        Map<String, Object> dataset = Map.of("col1", "val1");

        ParameterValidator validator = new ParameterValidator();
        ValidationResult result = validator.validate(workflow, dataset);

        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    void testValidateParametersInFieldsMap() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Test");

        StepDef step = new StepDef();
        step.setAction(ActionType.FILL);
        step.setFields(Map.of(
            "field1", "${data.column1}",
            "field2", "${data.column2}"
        ));

        workflow.setSteps(List.of(step));

        Map<String, Object> dataset = Map.of(
            "column1", "value1"
        );

        ParameterValidator validator = new ParameterValidator();
        ValidationResult result = validator.validate(workflow, dataset);

        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0).message()).contains("column2");
    }
}
