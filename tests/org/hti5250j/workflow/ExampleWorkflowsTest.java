package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.io.File;

class ExampleWorkflowsTest {

    @Test
    void testLoginWorkflowStructure() throws Exception {
        File loginYaml = new File("examples/login.yaml");
        assertThat(loginYaml).exists().isFile();

        WorkflowSchema workflow = WorkflowCLI.loadWorkflow(loginYaml);

        assertThat(workflow.getName()).isNotBlank();
        assertThat(workflow.getSteps()).isNotEmpty();
        assertThat(workflow.getSteps().get(0).getAction()).isEqualTo(ActionType.LOGIN);
    }

    @Test
    void testPaymentWorkflowStructure() throws Exception {
        File paymentYaml = new File("examples/payment.yaml");
        assertThat(paymentYaml).exists().isFile();

        WorkflowSchema workflow = WorkflowCLI.loadWorkflow(paymentYaml);

        assertThat(workflow.getName()).isNotBlank();
        assertThat(workflow.getSteps()).hasSizeGreaterThanOrEqualTo(3);

        // Verify workflow has standard steps: LOGIN, FILL, SUBMIT
        assertThat(workflow.getSteps().get(0).getAction()).isEqualTo(ActionType.LOGIN);
        assertThat(workflow.getSteps().stream().map(StepDef::getAction))
            .contains(ActionType.FILL, ActionType.SUBMIT);
    }

    @Test
    void testLoginWorkflowHasValidFields() throws Exception {
        File loginYaml = new File("examples/login.yaml");
        WorkflowSchema workflow = WorkflowCLI.loadWorkflow(loginYaml);

        StepDef loginStep = workflow.getSteps().get(0);
        assertThat(loginStep.getHost()).isNotBlank();
        assertThat(loginStep.getUser()).isNotBlank();
        assertThat(loginStep.getPassword()).isNotBlank();
    }

    @Test
    void testPaymentWorkflowHasDataSubstitution() throws Exception {
        File paymentYaml = new File("examples/payment.yaml");
        WorkflowSchema workflow = WorkflowCLI.loadWorkflow(paymentYaml);

        // Find FILL step
        StepDef fillStep = workflow.getSteps().stream()
            .filter(s -> s.getAction() == ActionType.FILL)
            .findFirst()
            .orElseThrow();

        assertThat(fillStep.getFields()).isNotEmpty();

        // Check for parameter substitution patterns
        boolean hasParameterSubstitution = fillStep.getFields().values().stream()
            .anyMatch(v -> v.contains("${data."));

        assertThat(hasParameterSubstitution).isTrue();
    }
}
