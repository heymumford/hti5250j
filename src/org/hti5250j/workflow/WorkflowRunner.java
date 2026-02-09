package org.hti5250j.workflow;

import org.hti5250j.Session5250;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.ScreenOIA;
import org.hti5250j.interfaces.SessionInterface;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
     * Performs parameter substitution before executing handler.
     */
    public void executeStep(StepDef step, Map<String, String> dataRow) throws Exception {
        switch (step.getAction()) {
            case LOGIN -> handleLogin(step);
            case NAVIGATE -> handleNavigate(step, dataRow);
            case FILL -> handleFill(step, dataRow);
            case SUBMIT -> handleSubmit(step, dataRow);
            case ASSERT -> handleAssert(step, dataRow);
            case WAIT -> handleWait(step, dataRow);
            case CAPTURE -> handleCapture(step, dataRow);
        }
    }

    private void handleLogin(StepDef step) throws Exception {
        if (!session.isConnected()) {
            session.connect();
        }

        Screen5250 screen = getScreen();
        waitForKeyboardUnlock(screen, DEFAULT_KEYBOARD_UNLOCK_TIMEOUT);

        artifactCollector.appendLedger("LOGIN", "Connected to " + step.getHost());
    }

    private void handleNavigate(StepDef step, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();
        String targetScreenName = datasetLoader.replaceParameters(step.getScreen(), dataRow);

        if (step.getKeys() == null) {
            throw new UnsupportedOperationException(
                "NAVIGATE requires 'keys' parameter for keystroke sequence. " +
                "Automatic menu navigation not yet implemented."
            );
        }

        screen.sendKeys(step.getKeys());
        waitForKeyboardUnlock(screen, DEFAULT_KEYBOARD_LOCK_CYCLE_TIMEOUT);

        if (!screenContainsText(screen, targetScreenName)) {
            String screenDump = formatScreenDump(getScreenContent(screen));
            throw NavigationException.withScreenDump("Failed to reach " + targetScreenName, screenDump);
        }

        artifactCollector.appendLedger("NAVIGATE", "Navigated to " + targetScreenName);
    }

    private void handleFill(StepDef step, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();

        screen.sendKeys("[home]");
        waitForKeyboardUnlock(screen, 1000);

        for (Map.Entry<String, String> field : step.getFields().entrySet()) {
            String fieldValue = datasetLoader.replaceParameters(field.getValue(), dataRow);
            fieldValue = fieldValue.trim();

            screen.sendKeys(fieldValue);
            waitForKeyboardUnlock(screen, FIELD_FILL_TIMEOUT);
            screen.sendKeys("[tab]");
            waitForKeyboardUnlock(screen, FIELD_FILL_TIMEOUT);
        }

        artifactCollector.appendLedger("FILL", "Fields populated: " + step.getFields().size());
    }

    private void handleSubmit(StepDef step, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();

        String keyName = step.getKey().toLowerCase();
        String mnemonic = mapKeyToMnemonic(keyName);

        screen.sendKeys(mnemonic);
        waitForKeyboardLockCycle(screen, DEFAULT_KEYBOARD_LOCK_CYCLE_TIMEOUT);

        artifactCollector.appendLedger("SUBMIT", "Submitted with " + keyName);
    }

    private void handleAssert(StepDef step, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();

        String expectedText = null;
        if (step.getText() != null) {
            expectedText = datasetLoader.replaceParameters(step.getText(), dataRow);
        }

        String expectedScreen = null;
        if (step.getScreen() != null) {
            expectedScreen = datasetLoader.replaceParameters(step.getScreen(), dataRow);
        }

        String screenContent = getScreenContent(screen);

        boolean passed = false;
        if (expectedText != null && expectedScreen != null) {
            passed = screenContent.contains(expectedScreen) && screenContent.contains(expectedText);
        } else if (expectedText != null) {
            passed = screenContent.contains(expectedText);
        } else if (expectedScreen != null) {
            passed = screenContent.contains(expectedScreen);
        } else {
            throw new IllegalArgumentException("ASSERT requires 'text' or 'screen' parameter");
        }

        if (!passed) {
            String screenDump = formatScreenDump(screenContent);
            throw AssertionException.withScreenDump("Assertion failed", screenDump);
        }

        artifactCollector.appendLedger("ASSERT", "Assertion passed");
    }

    private void handleWait(StepDef step, Map<String, String> dataRow) throws Exception {
        int timeout = step.getTimeout() != null ? step.getTimeout() : 5000;
        Thread.sleep(timeout);
        artifactCollector.appendLedger("WAIT", "Waited " + timeout + "ms");
    }

    private void handleCapture(StepDef step, Map<String, String> dataRow) throws Exception {
        Screen5250 screen = getScreen();

        String screenName = step.getName() != null ? step.getName() : "screenshot";
        screenName = datasetLoader.replaceParameters(screenName, dataRow);

        String screenContent = getScreenContent(screen);
        String formattedDump = formatScreenDump(screenContent);

        File captureFile = saveCaptureAsText(formattedDump, screenName);

        artifactCollector.appendLedger("CAPTURE", "Screenshot: " + captureFile.getName());
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
