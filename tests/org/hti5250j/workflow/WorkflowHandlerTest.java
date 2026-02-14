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
import java.util.Map;
import java.util.concurrent.TimeoutException;

class WorkflowHandlerTest {

    /**
     * Test adapter implementing both SessionInterface and ScreenProvider.
     */
    static class MockSessionAdapter implements SessionInterface, ScreenProvider {
        private final Screen5250 screen;

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
            return true;
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
        public void connect() {}

        @Override
        public void disconnect() {}

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

    /**
     * Test handleLogin() executes without error with mocked screen.
     */
    @Test
    void testHandleLoginConnectsAndWaits(@TempDir File tempDir) throws Exception {
        // Setup mocks
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false); // Keyboard unlocked (ready)

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        // Create and execute step
        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        step.setHost("test.example.com");
        step.setUser("testuser");
        step.setPassword("testpass");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        // Execute and verify no exception thrown
        runner.executeStep(step, Map.of());
        // Test passes if no exception thrown
    }

    /**
     * Test handleAssert() verifies screen contains expected text.
     */
    @Test
    void testHandleAssertChecksScreenContent(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        char[] screenContent = "Welcome to IBM i System".toCharArray();
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.ASSERT);
        step.setText("Welcome");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeStep(step, Map.of());

        // Should not throw
        verify(mockScreen).getScreenAsChars();
    }

    /**
     * Test handleAssert() throws AssertionException when text not found.
     */
    @Test
    void testHandleAssertFailsWhenTextNotFound(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        char[] screenContent = "Welcome to IBM i System".toCharArray();
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.ASSERT);
        step.setText("NonExistentText");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        assertThatThrownBy(() -> runner.executeStep(step, Map.of()))
            .isInstanceOf(AssertionException.class)
            .hasMessageContaining("Assertion failed");
    }

    /**
     * Test handleCapture() saves screen content to file via text fallback.
     * PNG generation requires Session5250 config; MockSessionAdapter triggers
     * fallback to text-only capture in the artifact directory.
     */
    @Test
    void testHandleCaptureCreatesScreenshotFile(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        char[] screenContent = "Screen Line 1\n".toCharArray();
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.CAPTURE);
        step.setName("test_screenshot");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeStep(step, Map.of());

        // Verify text artifact was created in tempDir (text fallback, not PNG)
        File[] files = tempDir.listFiles((dir, name) -> name.startsWith("test_screenshot"));
        assertThat(files).isNotNull().isNotEmpty();
    }

    /**
     * Test handleSubmit() sends key and waits for lock cycle.
     */
    @Test
    void testHandleSubmitSendsKeyAndWaits(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked())
            .thenReturn(false)  // First poll: not locked
            .thenReturn(true)   // Second poll: locked
            .thenReturn(false); // Third poll: unlocked (complete)

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.SUBMIT);
        step.setKey("ENTER");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeStep(step, Map.of());

        // Verify
        verify(mockScreen).sendKeys("[enter]");
    }

    /**
     * Test handleFill() populates fields with HOME + Tab pattern.
     * Verifies on the Mockito mock Screen5250, not the session adapter.
     */
    @Test
    void testHandleFillSendsHomeAndTabSequence(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.FILL);
        step.setFields(Map.of("account", "12345", "amount", "100.00"));

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeStep(step, Map.of());

        // Verify HOME key sent first
        verify(mockScreen).sendKeys("[home]");
        // Verify both field values sent
        verify(mockScreen).sendKeys("12345");
        verify(mockScreen).sendKeys("100.00");
        // Verify tab sent once per field (2 fields = 2 tabs)
        verify(mockScreen, times(2)).sendKeys("[tab]");
    }

    /**
     * Test handleFill() substitutes parameters from data row.
     * Verifies ${data.xxx} placeholders are replaced with actual values.
     */
    @Test
    void testHandleFillSubstitutesParameters(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.FILL);
        step.setFields(Map.of("account", "${data.acc}", "amount", "${data.amt}"));

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        Map<String, String> data = Map.of("acc", "ACC-123", "amt", "500.00");
        runner.executeStep(step, data);

        // Verify substituted values sent
        verify(mockScreen).sendKeys("[home]");
        verify(mockScreen).sendKeys("ACC-123");
        verify(mockScreen).sendKeys("500.00");
        // Verify tab sent once per field (2 fields = 2 tabs)
        verify(mockScreen, times(2)).sendKeys("[tab]");
    }

    /**
     * Test that NAVIGATE action requires keys parameter.
     * ValidationException thrown at ActionFactory level (early validation).
     */
    @Test
    void testHandleNavigateRequiresKeysParameter(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.NAVIGATE);
        step.setScreen("menu");
        // No keys set - validation now happens at ActionFactory level

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        // ValidationAction record constructor validates non-null, non-empty keys
        assertThatThrownBy(() -> runner.executeStep(step, Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("keys");
    }

    /**
     * Test handleNavigate() verifies target screen reached.
     */
    @Test
    void testHandleNavigateVerifiesScreenReached(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Screen content contains target text
        char[] screenContent = "Payment Menu - Option Selection".toCharArray();
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.NAVIGATE);
        step.setScreen("Payment Menu");
        step.setKeys("[pf3]");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeStep(step, Map.of());

        // Verify keys sent
        verify(mockScreen).sendKeys("[pf3]");
    }

    /**
     * Test handleNavigate() throws when target not reached.
     */
    @Test
    void testHandleNavigateFailsWhenScreenNotFound(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Screen content does NOT contain target text
        char[] screenContent = "Wrong Screen".toCharArray();
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.NAVIGATE);
        step.setScreen("Payment Menu");
        step.setKeys("[pf3]");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        assertThatThrownBy(() -> runner.executeStep(step, Map.of()))
            .isInstanceOf(NavigationException.class)
            .hasMessageContaining("Failed to reach");
    }

    /**
     * Test handleWait() sleeps for specified duration.
     */
    @Test
    void testHandleWaitSleepsForTimeout(@TempDir File tempDir) throws Exception {
        SessionInterface mockSession = mock(SessionInterface.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.WAIT);
        step.setTimeout(100); // 100ms

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        long startTime = System.currentTimeMillis();
        runner.executeStep(step, Map.of());
        long elapsed = System.currentTimeMillis() - startTime;

        // Verify at least 100ms elapsed
        assertThat(elapsed).isGreaterThanOrEqualTo(100);
    }

    /**
     * Test keyboard unlock timeout throws exception.
     */
    @Test
    void testKeyboardUnlockTimeoutThrowsException(@TempDir File tempDir) throws Exception {
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(true); // Always locked

        SessionInterface mockSession = createMockSessionWithScreen(mockScreen);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        step.setHost("test.example.com");
        step.setUser("testuser");
        step.setPassword("testpass");
        step.setTimeout(100); // 100ms timeout for test speed

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        assertThatThrownBy(() -> runner.executeStep(step, Map.of()))
            .isInstanceOf(TimeoutException.class)
            .hasMessageContaining("Keyboard locked");
    }
}
