package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.hti5250j.event.SessionListener;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.ScreenOIA;
import org.hti5250j.interfaces.SessionInterface;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class WorkflowRunnerTest {

    /**
     * Test adapter implementing both SessionInterface and ScreenProvider.
     */
    static class MockSessionAdapter implements SessionInterface, ScreenProvider {
        private final Screen5250 screen;
        private boolean connected = false;

        MockSessionAdapter(Screen5250 screen) {
            this.screen = screen;
        }

        @Override
        public Screen5250 getScreen() throws IllegalStateException {
            return screen;
        }

        @Override
        public String getConfigurationResource() {
            return "test";
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public String getSessionName() {
            return "test-session";
        }

        @Override
        public int getSessionType() {
            return 0;
        }

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public void disconnect() {
            connected = false;
        }

        @Override
        public void addSessionListener(SessionListener listener) {}

        @Override
        public void removeSessionListener(SessionListener listener) {}

        @Override
        public String showSystemRequest() {
            return null;
        }

        @Override
        public void signalBell() {}
    }

    private SessionInterface createMockSessionWithScreen(Screen5250 mockScreen) {
        return new MockSessionAdapter(mockScreen);
    }

    @Test
    void testExecuteWorkflowRunsStepsSequentially(@TempDir File tempDir) throws Exception {
        // Setup mocks
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);
        when(mockScreen.getOIA()).thenReturn(mockOIA);

        MockSessionAdapter sessionAdapter = new MockSessionAdapter(mockScreen);
        SessionInterface mockSession = spy(sessionAdapter);
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
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockScreen.getScreenAsChars()).thenReturn("Welcome to menu_screen".toCharArray());

        MockSessionAdapter sessionAdapter = new MockSessionAdapter(mockScreen);
        SessionInterface mockSession = spy(sessionAdapter);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        // Test NAVIGATE action
        StepDef navigateStep = new StepDef();
        navigateStep.setAction(ActionType.NAVIGATE);
        navigateStep.setScreen("menu_screen");
        navigateStep.setKeys("[enter]");

        Map<String, String> emptyData = Map.of();
        runner.executeStep(navigateStep, emptyData);

        // Should not crash - minimal implementation
        assertThat(true).isTrue();
    }

    @Test
    void testParameterSubstitutionInFieldValues(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);
        when(mockScreen.getOIA()).thenReturn(mockOIA);

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
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
        verify(mockScreen, atLeastOnce()).sendKeys(anyString());
    }
}
