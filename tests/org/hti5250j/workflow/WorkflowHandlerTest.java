package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.hti5250j.Session5250;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.ScreenOIA;
import org.hti5250j.framework.tn5250.tnvt;
import org.hti5250j.interfaces.SessionInterface;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

class WorkflowHandlerTest {

    /**
     * Test handleLogin() connects session and waits for keyboard unlock.
     */
    @Test
    void testHandleLoginConnectsAndWaits(@TempDir File tempDir) throws Exception {
        // Setup mocks
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false); // Keyboard unlocked (ready)

        // Create and execute step
        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);
        step.setHost("test.example.com");
        step.setUser("testuser");
        step.setPassword("testpass");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeStep(step, Map.of());

        // Verify
        verify(mockSession).connect();
        verify(mockOIA).isKeyBoardLocked();
    }

    /**
     * Test handleAssert() verifies screen contains expected text.
     */
    @Test
    void testHandleAssertChecksScreenContent(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        char[] screenContent = "Welcome to IBM i System".toCharArray();
        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

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
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        char[] screenContent = "Welcome to IBM i System".toCharArray();
        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

        StepDef step = new StepDef();
        step.setAction(ActionType.ASSERT);
        step.setText("NonExistentText");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        assertThatThrownBy(() -> runner.executeStep(step, Map.of()))
            .isInstanceOf(AssertionException.class)
            .hasMessageContaining("Assertion failed");
    }

    /**
     * Test handleCapture() saves screen content to file.
     */
    @Test
    void testHandleCaptureCreatesScreenshotFile(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        char[] screenContent = "Screen Line 1\n".toCharArray();
        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

        StepDef step = new StepDef();
        step.setAction(ActionType.CAPTURE);
        step.setName("test_screenshot");

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeStep(step, Map.of());

        // Verify artifact was created
        File artifactDir = new File("artifacts");
        File[] files = artifactDir.listFiles((dir, name) -> name.startsWith("test_screenshot"));
        assertThat(files).isNotNull().isNotEmpty();

        // Cleanup
        for (File f : files) {
            f.delete();
        }
        artifactDir.delete();
    }

    /**
     * Test handleSubmit() sends key and waits for lock cycle.
     */
    @Test
    void testHandleSubmitSendsKeyAndWaits(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked())
            .thenReturn(false)  // First poll: not locked
            .thenReturn(true)   // Second poll: locked
            .thenReturn(false); // Third poll: unlocked (complete)

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
     */
    @Test
    void testHandleFillSendsHomeAndTabSequence(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        StepDef step = new StepDef();
        step.setAction(ActionType.FILL);
        step.setFields(Map.of("account", "12345", "amount", "100.00"));

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        runner.executeStep(step, Map.of());

        // Verify HOME key sent first
        verify(mockScreen).sendKeys("[home]");
        // Verify field values and tabs sent
        verify(mockScreen).sendKeys("12345");
        verify(mockScreen).sendKeys("[tab]");
        verify(mockScreen).sendKeys("100.00");
        verify(mockScreen).sendKeys("[tab]");
    }

    /**
     * Test handleFill() substitutes parameters from data row.
     */
    @Test
    void testHandleFillSubstitutesParameters(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        StepDef step = new StepDef();
        step.setAction(ActionType.FILL);
        step.setFields(Map.of("account", "${data.acc}", "amount", "${data.amt}"));

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);
        Map<String, String> data = Map.of("acc", "ACC-123", "amt", "500.00");
        runner.executeStep(step, data);

        // Verify substituted values sent
        verify(mockScreen).sendKeys("[home]");
        verify(mockScreen).sendKeys("ACC-123");
        verify(mockScreen).sendKeys("[tab]");
        verify(mockScreen).sendKeys("500.00");
        verify(mockScreen).sendKeys("[tab]");
    }

    /**
     * Test handleNavigate() requires keys parameter.
     */
    @Test
    void testHandleNavigateRequiresKeysParameter(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);

        StepDef step = new StepDef();
        step.setAction(ActionType.NAVIGATE);
        step.setScreen("menu");
        // No keys set

        WorkflowRunner runner = new WorkflowRunner(mockSession, loader, collector);

        assertThatThrownBy(() -> runner.executeStep(step, Map.of()))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("'keys' parameter");
    }

    /**
     * Test handleNavigate() verifies target screen reached.
     */
    @Test
    void testHandleNavigateVerifiesScreenReached(@TempDir File tempDir) throws Exception {
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Screen content contains target text
        char[] screenContent = "Payment Menu - Option Selection".toCharArray();
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

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
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(false);

        // Screen content does NOT contain target text
        char[] screenContent = "Wrong Screen".toCharArray();
        when(mockScreen.getScreenAsChars()).thenReturn(screenContent);

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
        Session5250 mockSession = mock(Session5250.class);
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
        Session5250 mockSession = mock(Session5250.class);
        Screen5250 mockScreen = mock(Screen5250.class);
        ScreenOIA mockOIA = mock(ScreenOIA.class);
        DatasetLoader loader = new DatasetLoader();
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        when(mockSession.getScreen()).thenReturn(mockScreen);
        when(mockScreen.getOIA()).thenReturn(mockOIA);
        when(mockOIA.isKeyBoardLocked()).thenReturn(true); // Always locked

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
