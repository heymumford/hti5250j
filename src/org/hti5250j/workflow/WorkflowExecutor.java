/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.hti5250j.interfaces.SessionInterface;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Orchestrates workflow execution against i5 systems.
 * Coordinates session creation, dataset loading, and workflow running.
 */
public class WorkflowExecutor {

    /**
     * Execute workflow with dataset and environment context.
     *
     * @param workflow the workflow to execute
     * @param dataFileArg path to CSV data file (optional)
     * @param environment environment name (dev/test/prod)
     * @throws Exception if execution fails
     */
    public static void execute(WorkflowSchema workflow, String dataFileArg, String environment) throws Exception {
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow cannot be null");
        }

        Map<String, String> dataRow = loadDataset(dataFileArg);

        StepDef loginStep = workflow.getSteps().stream()
            .filter(s -> s.getAction() == ActionType.LOGIN)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Workflow requires LOGIN step"));

        SessionInterface session = SessionFactory.createFromLoginStep(loginStep);

        String workflowNamePath = workflow.getName().replaceAll("\\s+", "_");
        File artifactDir = new File("artifacts/" + workflowNamePath);
        artifactDir.mkdirs();
        ArtifactCollector collector = new ArtifactCollector(artifactDir);

        DatasetLoader loader = new DatasetLoader();
        WorkflowRunner runner = new WorkflowRunner(session, loader, collector);

        try {
            runner.executeWorkflow(workflow, dataRow);
            TerminalAdapter.printExecutionSuccess(artifactDir.getAbsolutePath());
        } catch (Exception e) {
            TerminalAdapter.printExecutionFailure(e.getMessage());
            throw e;
        }
    }

    /**
     * Execute batch workflows with parallel processing using virtual threads.
     * Processes all CSV rows concurrently.
     *
     * @param workflow the workflow to execute
     * @param dataFileArg path to CSV data file
     * @param environment environment name (dev/test/prod)
     * @return batch metrics with aggregated results
     * @throws Exception if batch execution fails
     */
    public static BatchMetrics executeBatch(
            WorkflowSchema workflow,
            String dataFileArg,
            String environment) throws Exception {

        if (workflow == null) {
            throw new IllegalArgumentException("Workflow cannot be null");
        }

        if (dataFileArg == null) {
            throw new IllegalArgumentException("Data file required for batch execution");
        }

        DatasetLoader loader = new DatasetLoader();
        Map<String, Map<String, String>> allRows = loader.loadCSV(new File(dataFileArg));

        if (allRows.isEmpty()) {
            throw new IllegalArgumentException("CSV file contains no data rows");
        }

        return BatchExecutor.executeAll(workflow, allRows, environment);
    }

    /**
     * Load dataset from CSV file.
     *
     * @param dataFileArg path to CSV file (optional, can be null)
     * @return map of column names to values from first row
     */
    private static Map<String, String> loadDataset(String dataFileArg) {
        Map<String, String> result = new HashMap<>();

        if (dataFileArg == null) {
            return result;
        }

        try {
            DatasetLoader loader = new DatasetLoader();
            Map<String, Map<String, String>> csvData = loader.loadCSV(new File(dataFileArg));
            if (!csvData.isEmpty()) {
                result = csvData.values().iterator().next();
            }
        } catch (Exception e) {
            System.err.println("Error loading dataset: " + e.getMessage());
        }

        return result;
    }
}
