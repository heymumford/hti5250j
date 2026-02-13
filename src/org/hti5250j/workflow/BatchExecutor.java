/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.hti5250j.interfaces.SessionInterface;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Parallel batch workflow executor using Java 21 virtual threads.
 * Processes multiple CSV rows concurrently with minimal memory overhead.
 * Each row executes as independent workflow on separate virtual thread.
 * Preserves sequential step execution within each workflow.
 */
public class BatchExecutor {

    private static final long WORKFLOW_TIMEOUT_SECONDS = 300; // 5 minutes per workflow

    /**
     * Execute all workflows from CSV data in parallel using virtual threads.
     *
     * @param workflow the workflow schema to execute
     * @param csvRows key-value pairs from CSV file (one per row)
     * @param environment environment name (dev/test/prod)
     * @return aggregated batch metrics
     * @throws InterruptedException if batch execution is interrupted
     */
    public static BatchMetrics executeAll(
            WorkflowSchema workflow,
            Map<String, Map<String, String>> csvRows,
            String environment) throws InterruptedException {

        long batchStartNanos = System.nanoTime();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<WorkflowResult>> futures = new ArrayList<>();

        for (Map.Entry<String, Map<String, String>> entry : csvRows.entrySet()) {
            String rowKey = entry.getKey();
            Map<String, String> dataRow = entry.getValue();

            Future<WorkflowResult> future = executor.submit(() ->
                executeWorkflowWithMetrics(workflow, rowKey, dataRow, environment)
            );
            futures.add(future);
        }

        List<WorkflowResult> results = new ArrayList<>();
        for (Future<WorkflowResult> future : futures) {
            try {
                WorkflowResult result = future.get(WORKFLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                results.add(WorkflowResult.timeout("Workflow exceeded " + WORKFLOW_TIMEOUT_SECONDS + "s timeout"));
            } catch (ExecutionException e) {
                // Unwrap ExecutionException to preserve original exception type
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                String rowKey = "unknown";
                for (WorkflowResult r : results) {
                    if (!r.success()) {
                        rowKey = r.rowKey();
                        break;
                    }
                }
                results.add(WorkflowResult.failure(rowKey, 0, cause));
            }
        }

        executor.shutdown();

        long batchEndNanos = System.nanoTime();
        return BatchMetrics.from(results, batchStartNanos, batchEndNanos);
    }

    /**
     * Execute single workflow and record latency metrics.
     * Creates independent session + artifact collector per workflow.
     *
     * @param workflow the workflow to execute
     * @param rowKey identifier for this CSV row (for logging)
     * @param dataRow parameter values for workflow substitution
     * @param environment environment name
     * @return result with success/failure status and latency
     */
    private static WorkflowResult executeWorkflowWithMetrics(
            WorkflowSchema workflow,
            String rowKey,
            Map<String, String> dataRow,
            String environment) {

        long startNanos = System.nanoTime();

        try {
            StepDef loginStep = workflow.getSteps().stream()
                .filter(s -> s.getAction() == ActionType.LOGIN)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workflow requires LOGIN step"));

            SessionInterface session = SessionFactory.createFromLoginStep(loginStep);

            String workflowNamePath = workflow.getName().replaceAll("\\s+", "_");
            String uniquePath = workflowNamePath + "_" + rowKey;
            File artifactDir = new File("artifacts/" + uniquePath);
            artifactDir.mkdirs();
            ArtifactCollector collector = new ArtifactCollector(artifactDir);

            DatasetLoader loader = new DatasetLoader();
            WorkflowRunner runner = new WorkflowRunner(session, loader, collector);
            runner.executeWorkflow(workflow, dataRow);

            try {
                session.disconnect();
            } catch (Exception e) {
                // Suppress disconnect errors (session may already be closed)
            }

            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            return WorkflowResult.success(rowKey, latencyMs, artifactDir.getAbsolutePath());

        } catch (Exception e) {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            return WorkflowResult.failure(rowKey, latencyMs, e);
        }
    }
}
