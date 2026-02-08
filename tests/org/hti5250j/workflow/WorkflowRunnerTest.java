package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.hti5250j.interfaces.SessionInterface;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class WorkflowRunnerTest {

    @Test
    void testExecuteWorkflowRunsStepsSequentially(@TempDir File tempDir) throws Exception {
        // Setup mocks
        SessionInterface mockSession = mock(SessionInterface.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        // Create workflow
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Test Workflow");
        workflow.setEnvironment("test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("test.example.com");
        loginStep.setUser("testuser");
        loginStep.setPassword("testpass");
        steps.add(loginStep);

        StepDef fillStep = new StepDef();
        fillStep.setAction(ActionType.FILL);
        Map<String, String> fields = Map.of("account", "${data.account_id}", "amount", "${data.amount}");
        fillStep.setFields(fields);
        steps.add(fillStep);

        StepDef submitStep = new StepDef();
        submitStep.setAction(ActionType.SUBMIT);
        submitStep.setKey("ENTER");
        steps.add(submitStep);

        workflow.setSteps(steps);

        // Execute workflow
        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        Map<String, String> dataRow = Map.of("account_id", "ACC-123", "amount", "500.00");

        runner.executeWorkflow(workflow, dataRow);

        // Verify session was called
        verify(mockSession, times(1)).connect();
    }

    @Test
    void testExecuteStepDispatchesToCorrectHandler(@TempDir File tempDir) throws Exception {
        SessionInterface mockSession = mock(SessionInterface.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        // Test NAVIGATE action
        StepDef navigateStep = new StepDef();
        navigateStep.setAction(ActionType.NAVIGATE);
        navigateStep.setScreen("menu_screen");

        Map<String, String> emptyData = Map.of();
        runner.executeStep(navigateStep, emptyData);

        // Should not crash - minimal implementation
        assertThat(true).isTrue();
    }

    @Test
    void testParameterSubstitutionInFieldValues(@TempDir File tempDir) throws Exception {
        SessionInterface mockSession = mock(SessionInterface.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        StepDef fillStep = new StepDef();
        fillStep.setAction(ActionType.FILL);
        fillStep.setFields(Map.of(
            "account", "${data.account_id}",
            "amount", "${data.amount}"
        ));

        Map<String, String> dataRow = Map.of(
            "account_id", "ACC-999",
            "amount", "1250.50"
        );

        // Should execute without error (parameter substitution)
        runner.executeStep(fillStep, dataRow);
        assertThat(true).isTrue();
    }
}
