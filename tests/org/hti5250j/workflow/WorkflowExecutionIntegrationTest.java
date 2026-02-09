package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.hti5250j.Session5250;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.ScreenOIA;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for complete workflow execution.
 */
class WorkflowExecutionIntegrationTest {

    /**
     * Test complete LOGIN workflow execution.
     */
    @Test
    void testCompleteLoginWorkflow(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Build workflow
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Login Test");
        workflow.setEnvironment("test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("test.example.com");
        loginStep.setUser("testuser");
        loginStep.setPassword("testpass");
        steps.add(loginStep);

        workflow.setSteps(steps);

        // Execute
        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeWorkflow(workflow, Map.of());

        // Verify
        verify(mockSession).connect();
    }

    /**
     * Test complete LOGIN -> FILL -> SUBMIT workflow.
     */
    @Test
    void testCompletePaymentWorkflow(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Build workflow
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Payment Test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("ibmi.example.com");
        loginStep.setUser("user1");
        loginStep.setPassword("pass1");
        steps.add(loginStep);

        StepDef fillStep = new StepDef();
        fillStep.setAction(ActionType.FILL);
        fillStep.setFields(Map.of("account", "${data.acc}", "amount", "${data.amt}"));
        steps.add(fillStep);

        StepDef submitStep = new StepDef();
        submitStep.setAction(ActionType.SUBMIT);
        submitStep.setKey("ENTER");
        steps.add(submitStep);

        workflow.setSteps(steps);

        // Execute
        Map<String, String> data = Map.of("acc", "ACC-123", "amt", "500.00");
        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeWorkflow(workflow, data);

        // Verify all steps executed
        verify(mockSession).connect();
        verify(mockScreen).sendKeys("[home]");
        verify(mockScreen).sendKeys("ACC-123");
        verify(mockScreen).sendKeys("[tab]");
        verify(mockScreen).sendKeys("500.00");
        verify(mockScreen).sendKeys("[tab]");
        verify(mockScreen).sendKeys("[enter]");
    }

    /**
     * Test workflow with NAVIGATE step.
     */
    @Test
    void testWorkflowWithNavigation(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Screen shows menu on first navigate
        when(mockScreen.getScreenAsChars())
            .thenReturn("Menu Screen Available".toCharArray());

        // Build workflow
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Navigation Test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("test.example.com");
        loginStep.setUser("user1");
        loginStep.setPassword("pass1");
        steps.add(loginStep);

        StepDef navStep = new StepDef();
        navStep.setAction(ActionType.NAVIGATE);
        navStep.setScreen("Menu Screen");
        navStep.setKeys("[pf1]");
        steps.add(navStep);

        workflow.setSteps(steps);

        // Execute
        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeWorkflow(workflow, Map.of());

        // Verify navigation
        verify(mockScreen).sendKeys("[pf1]");
    }

    /**
     * Test workflow with ASSERT verification.
     */
    @Test
    void testWorkflowWithAssertion(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Screen shows success message
        when(mockScreen.getScreenAsChars())
            .thenReturn("Payment Processed Successfully".toCharArray());

        // Build workflow
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Assertion Test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("test.example.com");
        loginStep.setUser("user1");
        loginStep.setPassword("pass1");
        steps.add(loginStep);

        StepDef assertStep = new StepDef();
        assertStep.setAction(ActionType.ASSERT);
        assertStep.setText("Successfully");
        steps.add(assertStep);

        workflow.setSteps(steps);

        // Execute
        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeWorkflow(workflow, Map.of());

        // Verify assertion passed (no exception thrown)
    }

    /**
     * Test workflow with CAPTURE step generates artifact.
     */
    @Test
    void testWorkflowWithCapture(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        char[] screenContent = "Screen to capture line 1\nLine 2".toCharArray();
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

        // Build workflow
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Capture Test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("test.example.com");
        loginStep.setUser("user1");
        loginStep.setPassword("pass1");
        steps.add(loginStep);

        StepDef captureStep = new StepDef();
        captureStep.setAction(ActionType.CAPTURE);
        captureStep.setName("test_capture");
        steps.add(captureStep);

        workflow.setSteps(steps);

        // Execute
        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeWorkflow(workflow, Map.of());

        // Verify artifact was created
        File artifactDir = new File("artifacts");
        File[] files = artifactDir.listFiles((dir, name) -> name.startsWith("test_capture"));
        assertThat(files).isNotNull().isNotEmpty();

        // Cleanup
        for (File f : files) {
            f.delete();
        }
        artifactDir.delete();
    }

    /**
     * Test workflow with WAIT step.
     */
    @Test
    void testWorkflowWithWait(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Build workflow
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Wait Test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("test.example.com");
        loginStep.setUser("user1");
        loginStep.setPassword("pass1");
        steps.add(loginStep);

        StepDef waitStep = new StepDef();
        waitStep.setAction(ActionType.WAIT);
        waitStep.setTimeout(50); // 50ms for test
        steps.add(waitStep);

        workflow.setSteps(steps);

        // Execute
        long startTime = System.currentTimeMillis();
        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeWorkflow(workflow, Map.of());
        long elapsed = System.currentTimeMillis() - startTime;

        // Verify wait happened
        assertThat(elapsed).isGreaterThanOrEqualTo(50);
    }

    /**
     * Test workflow error recovery - assertion failure halts workflow.
     */
    @Test
    void testWorkflowHaltsOnAssertionFailure(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Screen does NOT contain expected text
        when(mockScreen.getScreenAsChars())
            .thenReturn("Error: Payment Failed".toCharArray());

        // Build workflow
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Error Test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("test.example.com");
        loginStep.setUser("user1");
        loginStep.setPassword("pass1");
        steps.add(loginStep);

        StepDef failAssertStep = new StepDef();
        failAssertStep.setAction(ActionType.ASSERT);
        failAssertStep.setText("Successfully");
        steps.add(failAssertStep);

        StepDef postAssertStep = new StepDef();
        postAssertStep.setAction(ActionType.WAIT);
        postAssertStep.setTimeout(100);
        steps.add(postAssertStep);

        workflow.setSteps(steps);

        // Execute - should fail at assertion
        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        assertThatThrownBy(() -> runner.executeWorkflow(workflow, Map.of()))
            .isInstanceOf(AssertionException.class);

        // Verify second step not executed (no second verify calls beyond assertion)
    }

    /**
     * Test parameter substitution across all steps.
     */
    @Test
    void testParameterSubstitutionAcrossSteps(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        when(mockScreen.getScreenAsChars())
            .thenReturn("Order ABC-456 Processing".toCharArray());

        // Build workflow with parameters
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("Param Test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("test.example.com");
        loginStep.setUser("${data.user}");
        loginStep.setPassword("${data.pass}");
        steps.add(loginStep);

        StepDef fillStep = new StepDef();
        fillStep.setAction(ActionType.FILL);
        fillStep.setFields(Map.of("order_id", "${data.order}"));
        steps.add(fillStep);

        StepDef assertStep = new StepDef();
        assertStep.setAction(ActionType.ASSERT);
        assertStep.setText("${data.order}");
        steps.add(assertStep);

        workflow.setSteps(steps);

        // Execute with data
        Map<String, String> data = Map.of("user", "user1", "pass", "pass1", "order", "ABC-456");
        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeWorkflow(workflow, data);

        // Verify substitutions happened
        verify(mockScreen).sendKeys("[home]");
        verify(mockScreen).sendKeys("ABC-456");
    }
}
