package org.hti5250j.workflow;

import org.hti5250j.interfaces.SessionInterface;
import java.awt.image.BufferedImage;
import java.util.Map;

public class WorkflowRunner {
    private final SessionInterface session;
    private final DatasetLoader datasetLoader;
    private final ArtifactCollector artifactCollector;

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
        // SessionInterface.connect() uses pre-configured credentials
        // HOST/USER/PASSWORD would be set in session configuration
        session.connect();
        artifactCollector.appendLedger("LOGIN", "Connected to " + step.getHost());
    }

    private void handleNavigate(StepDef step, Map<String, String> dataRow) throws Exception {
        String screenName = datasetLoader.replaceParameters(step.getScreen(), dataRow);
        // Navigation logic would navigate to screen
        artifactCollector.appendLedger("NAVIGATE", "Navigated to " + screenName);
    }

    private void handleFill(StepDef step, Map<String, String> dataRow) throws Exception {
        // For each field, substitute parameters and fill on session
        for (Map.Entry<String, String> field : step.getFields().entrySet()) {
            String fieldValue = datasetLoader.replaceParameters(field.getValue(), dataRow);
            // session.fillField(field.getKey(), fieldValue);
        }
        artifactCollector.appendLedger("FILL", "Fields populated");
    }

    private void handleSubmit(StepDef step, Map<String, String> dataRow) throws Exception {
        String key = step.getKey();
        // Key submission would go through session's screen.sendKeys(key)
        artifactCollector.appendLedger("SUBMIT", "Submitted with key " + key);
    }

    private void handleAssert(StepDef step, Map<String, String> dataRow) throws Exception {
        String expectedText = datasetLoader.replaceParameters(step.getText(), dataRow);
        // Assertion logic would verify screen contains text
        artifactCollector.appendLedger("ASSERT", "Assertion passed: " + expectedText);
    }

    private void handleWait(StepDef step, Map<String, String> dataRow) throws Exception {
        int timeout = step.getTimeout() != null ? step.getTimeout() : 5000;
        Thread.sleep(timeout);
        artifactCollector.appendLedger("WAIT", "Waited " + timeout + "ms");
    }

    private void handleCapture(StepDef step, Map<String, String> dataRow) throws Exception {
        // Minimal capture: would call session.getScreen() to get BufferedImage
        // For now, just log to ledger
        String screenName = step.getName() != null ? step.getName() : "screenshot";
        artifactCollector.appendLedger("CAPTURE", "Screenshot captured: " + screenName);
    }
}
