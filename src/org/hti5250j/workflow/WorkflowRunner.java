package org.hti5250j.workflow;

import org.hti5250j.Session5250;
import org.hti5250j.HeadlessScreenRenderer;
import org.hti5250j.SessionConfig;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.ScreenOIA;
import org.hti5250j.interfaces.RequestHandler;
import org.hti5250j.interfaces.SessionInterface;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

public class WorkflowRunner {
    private final SessionInterface session;
    private final DatasetLoader datasetLoader;
    private final ArtifactCollector artifactCollector;

    // Screen interaction timeouts (milliseconds)
    private static final int DEFAULT_KEYBOARD_UNLOCK_TIMEOUT = 30000;
    private static final int DEFAULT_KEYBOARD_LOCK_CYCLE_TIMEOUT = 5000;
    private static final int KEYBOARD_POLL_INTERVAL = 100;
    private static final int FIELD_FILL_TIMEOUT = 500;

    public WorkflowRunner(SessionInterface session, DatasetLoader datasetLoader, ArtifactCollector artifactCollector) {
        this.session = session;
        this.datasetLoader = datasetLoader;
        this.artifactCollector = artifactCollector;

        // Development-mode validation: warn if running on platform thread
        if (!Thread.currentThread().isVirtual()) {
            System.err.println("WARNING: WorkflowRunner on platform thread. " +
                             "Expected virtual thread for efficient blocking I/O.");
        }
    }

    /**
     * Set custom RequestHandler for SYSREQ (F3 key) handling.
     * <p>
     * Phase 15B: Enables Robot Framework, Jython adapters, and workflow-specific logic
     * to intercept and handle system request dialogs programmatically.
     * <p>
     * Example: Custom handler that responds to SYSREQ with workflow-specific logic
     * (e.g., selecting menu options, handling confirmations automatically).
     * <p>
     * This method has no effect if the session does not support RequestHandler
     * (only Session5250 and its derivatives support this feature).
     *
     * @param requestHandler custom RequestHandler implementation
     * @throws NullPointerException if requestHandler is null
     * @since Phase 15B
     */
    public void setRequestHandler(RequestHandler requestHandler) {
        if (requestHandler == null) {
            throw new NullPointerException("RequestHandler cannot be null");
        }

        // Inject handler into underlying Session5250 if available
        if (session instanceof Session5250) {
            ((Session5250) session).setRequestHandler(requestHandler);
        }
    }

    /**
     * Get the underlying session as Session5250 if available.
     * <p>
     * Useful for accessing extended APIs not available through SessionInterface,
     * such as HeadlessSession interface, custom RequestHandler injection, etc.
     * <p>
     * Phase 15B: Enables advanced usage patterns (Robot Framework adapters, etc.)
     *
     * @return Session5250 instance, or null if session is not a Session5250
     * @since Phase 15B
     */
    public Session5250 getSession5250() {
        return (session instanceof Session5250) ? (Session5250) session : null;
    }

    /**
     * Execute workflow step by step sequentially.
     * Each step receives the same data row for parameter substitution.
     */
    public void executeWorkflow(WorkflowSchema workflow, Map<String, String> dataRow) throws Exception {
        for (StepDef step : workflow.getSteps()) {
            executeStep(step, dataRow);
        }
    }

    /**
     * Execute single step based on action type.
     * Converts StepDef to typed Action via ActionFactory, then dispatches
     * to handler via exhaustive pattern matching (compiler enforces completeness).
     */
    public void executeStep(StepDef stepDef, Map<String, String> dataRow) throws Exception {
        Action action = ActionFactory.from(stepDef);

        switch (action) {
            case LoginAction login -> handleLogin(login);
            case NavigateAction nav -> handleNavigate(nav, dataRow);
            case FillAction fill -> handleFill(fill, dataRow);
            case SubmitAction submit -> handleSubmit(submit, dataRow);
            case AssertAction assert_ -> handleAssert(assert_, dataRow);
            case WaitAction wait -> handleWait(wait, dataRow);
            case CaptureAction capture -> handleCapture(capture, dataRow);
        }
    }

    private void handleLogin(LoginAction login) throws Exception {
        if (!session.isConnected()) {
            session.connect();
        }

        Screen5250 screen = getScreen();
        waitForKeyboardUnlock(screen, DEFAULT_KEYBOARD_UNLOCK_TIMEOUT);

        artifactCollector.appendLedger("LOGIN", "Connected to " + login.host());
    }

    private void handleNavigate(NavigateAction nav, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();
        String targetScreenName = datasetLoader.replaceParameters(nav.screen(), dataRow);

        screen.sendKeys(nav.keys());
        waitForKeyboardUnlock(screen, DEFAULT_KEYBOARD_LOCK_CYCLE_TIMEOUT);

        if (!screenContainsText(screen, targetScreenName)) {
            String screenDump = formatScreenDump(getScreenContent(screen));
            throw NavigationException.withScreenDump("Failed to reach " + targetScreenName, screenDump);
        }

        artifactCollector.appendLedger("NAVIGATE", "Navigated to " + targetScreenName);
    }

    private void handleFill(FillAction fill, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();

        screen.sendKeys("[home]");
        waitForKeyboardUnlock(screen, 1000);

        for (Map.Entry<String, String> field : fill.fields().entrySet()) {
            String fieldValue = datasetLoader.replaceParameters(field.getValue(), dataRow);
            fieldValue = fieldValue.trim();

            screen.sendKeys(fieldValue);
            waitForKeyboardUnlock(screen, FIELD_FILL_TIMEOUT);
            screen.sendKeys("[tab]");
            waitForKeyboardUnlock(screen, FIELD_FILL_TIMEOUT);
        }

        artifactCollector.appendLedger("FILL", "Fields populated: " + fill.fields().size());
    }

    private void handleSubmit(SubmitAction submit, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();

        String keyName = submit.key().toLowerCase();
        String mnemonic = mapKeyToMnemonic(keyName);

        screen.sendKeys(mnemonic);
        waitForKeyboardLockCycle(screen, DEFAULT_KEYBOARD_LOCK_CYCLE_TIMEOUT);

        artifactCollector.appendLedger("SUBMIT", "Submitted with " + keyName);
    }

    private void handleAssert(AssertAction assert_, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();

        String expectedText = null;
        if (assert_.text() != null && !assert_.text().isEmpty()) {
            expectedText = datasetLoader.replaceParameters(assert_.text(), dataRow);
        }

        String expectedScreen = null;
        if (assert_.screen() != null && !assert_.screen().isEmpty()) {
            expectedScreen = datasetLoader.replaceParameters(assert_.screen(), dataRow);
        }

        String screenContent = getScreenContent(screen);

        boolean passed = false;
        if (expectedText != null && expectedScreen != null) {
            passed = screenContent.contains(expectedScreen) && screenContent.contains(expectedText);
        } else if (expectedText != null) {
            passed = screenContent.contains(expectedText);
        } else if (expectedScreen != null) {
            passed = screenContent.contains(expectedScreen);
        }

        if (!passed) {
            String screenDump = formatScreenDump(screenContent);
            throw AssertionException.withScreenDump("Assertion failed", screenDump);
        }

        artifactCollector.appendLedger("ASSERT", "Assertion passed");
    }

    private void handleWait(WaitAction wait, Map<String, String> dataRow) throws Exception {
        int timeout = wait.timeout();
        Thread.sleep(timeout);
        artifactCollector.appendLedger("WAIT", "Waited " + timeout + "ms");
    }

    private void handleCapture(CaptureAction capture, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();

        String screenName = capture.name() != null ? capture.name() : "screenshot";
        screenName = datasetLoader.replaceParameters(screenName, dataRow);

        try {
            // Generate PNG screenshot using HeadlessScreenRenderer (no GUI required)
            BufferedImage screenshot = generateScreenshot(screen);

            // Save PNG
            File pngFile = saveCapturePng(screenshot, screenName);
            artifactCollector.appendLedger("CAPTURE", "PNG: " + pngFile.getName());

            // Also save text representation for accessibility
            String screenContent = getScreenContent(screen);
            String formattedDump = formatScreenDump(screenContent);
            File textFile = saveCaptureAsText(formattedDump, screenName + ".txt");
            artifactCollector.appendLedger("CAPTURE", "Text: " + textFile.getName());

        } catch (Exception e) {
            // Fallback to text-only capture
            String screenContent = getScreenContent(screen);
            String formattedDump = formatScreenDump(screenContent);
            File captureFile = saveCaptureAsText(formattedDump, screenName);
            artifactCollector.appendLedger("CAPTURE", "Text (fallback): " + captureFile.getName());
        }
    }

    /**
     * Generate BufferedImage screenshot without requiring persistent GUI components.
     * Uses HeadlessScreenRenderer for pure headless mode support.
     */
    private BufferedImage generateScreenshot(Screen5250 screen) {
        // Get session configuration if available
        SessionConfig config = null;
        if (session instanceof Session5250) {
            config = ((Session5250) session).getConfiguration();
        }

        if (config == null) {
            throw new IllegalStateException("Cannot determine session configuration for screenshot generation");
        }

        // Use HeadlessScreenRenderer for stateless rendering (no GUI required)
        return HeadlessScreenRenderer.renderScreen(screen, config);
    }

    /**
     * Save BufferedImage as PNG file in artifacts directory.
     */
    private File saveCapturePng(BufferedImage image, String baseName) throws IOException {
        File artifactDir = new File("artifacts");
        if (!artifactDir.exists()) {
            artifactDir.mkdirs();
        }

        File pngFile = new File(artifactDir, baseName + ".png");
        ImageIO.write(image, "PNG", pngFile);
        return pngFile;
    }

    // Helper methods

    private Screen5250 getScreen() throws IllegalStateException {
        if (!(session instanceof ScreenProvider)) {
            throw new IllegalStateException("Session must implement ScreenProvider interface");
        }
        return ((ScreenProvider) session).getScreen();
    }

    private String getScreenContent(Screen5250 screen) {
        char[] screenChars = screen.getScreenAsChars();
        return new String(screenChars);
    }

    private boolean screenContainsText(Screen5250 screen, String text) {
        String screenContent = getScreenContent(screen);
        return screenContent.contains(text);
    }

    private String formatScreenDump(String screenContent) {
        StringBuilder sb = new StringBuilder();
        int cols = 80;
        for (int i = 0; i < screenContent.length(); i += cols) {
            int end = Math.min(i + cols, screenContent.length());
            sb.append(screenContent.substring(i, end)).append("\n");
        }
        return sb.toString();
    }

    private File saveCaptureAsText(String screenContent, String screenName) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String filename = screenName + "_" + timestamp + ".txt";

        File artifactDir = artifactCollector.getArtifactDir();
        File captureFile = new File(artifactDir, filename);

        try (FileWriter writer = new FileWriter(captureFile)) {
            writer.write(screenContent);
        }

        return captureFile;
    }

    private String mapKeyToMnemonic(String keyName) {
        return "[" + keyName.toLowerCase() + "]";
    }

    private void waitForKeyboardUnlock(Screen5250 screen, int timeoutMs) throws TimeoutException {
        long start = System.currentTimeMillis();
        ScreenOIA oia = screen.getOIA();

        while (oia.isKeyBoardLocked()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new TimeoutException("Keyboard locked after " + timeoutMs + "ms");
            }
            try {
                Thread.sleep(KEYBOARD_POLL_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TimeoutException("Interrupted while waiting for keyboard unlock");
            }
        }
    }

    private void waitForKeyboardLockCycle(Screen5250 screen, int timeoutMs) throws TimeoutException {
        ScreenOIA oia = screen.getOIA();

        // Wait for lock (submission accepted) - short timeout, may complete instantly
        long start = System.currentTimeMillis();
        while (!oia.isKeyBoardLocked()) {
            if (System.currentTimeMillis() - start > 1000) {
                return; // Completed instantly or no lock needed
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Wait for unlock (screen refreshed)
        start = System.currentTimeMillis();
        while (oia.isKeyBoardLocked()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new TimeoutException("Screen not refreshed after " + timeoutMs + "ms");
            }
            try {
                Thread.sleep(KEYBOARD_POLL_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TimeoutException("Interrupted while waiting for screen refresh");
            }
        }
    }
}
