package org.hti5250j.workflow;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

/**
 * Command-line interface for HTI5250j workflow execution and validation.
 * Routes CLI invocations to specialized adapters and executors.
 */
public class WorkflowCLI {

    /**
     * Entry point for command-line execution.
     * Handles argument parsing, workflow loading, and delegating to validate/run operations.
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
                TerminalAdapter.printHelp();
                return;
            }

            ArgumentParser parsed = ArgumentParser.parse(args);
            parsed.validate();

            // Load workflow
            WorkflowSchema workflow = WorkflowLoader.load(parsed.workflowFile());
            TerminalAdapter.printWorkflowLoaded(workflow.getName(), workflow.getSteps().size());

            // Handle validate action
            if ("validate".equals(parsed.action())) {
                validateWorkflow(workflow, parsed.dataFile());
                return;
            }

            // Handle run action
            if ("run".equals(parsed.action())) {
                if (parsed.dataFile() != null) {
                    DatasetLoader loader = new DatasetLoader();
                    Map<String, Map<String, String>> allRows = loader.loadCSV(new File(parsed.dataFile()));

                    if (allRows.size() > 1) {
                        // Batch mode: multiple rows, execute in parallel with virtual threads
                        TerminalAdapter.printBatchMode(allRows.size());
                        BatchMetrics metrics = WorkflowExecutor.executeBatch(workflow, parsed.dataFile(), parsed.environment());
                        metrics.print();

                        if (metrics.failureCount() > 0) {
                            System.exit(1);
                        }
                    } else {
                        // Single-row mode: existing sequential execution
                        WorkflowExecutor.execute(workflow, parsed.dataFile(), parsed.environment());
                    }
                } else {
                    // No data file: existing execution
                    WorkflowExecutor.execute(workflow, null, parsed.environment());
                }
            }

        } catch (Exception e) {
            TerminalAdapter.printError("Error", e);
            System.exit(1);
        }
    }

    /**
     * Validate workflow against schema and dataset (if provided).
     */
    private static void validateWorkflow(WorkflowSchema workflow, String dataFile) {
        WorkflowValidator validator = new WorkflowValidator();
        ValidationResult result = validator.validate(workflow);

        // Validate parameters if dataset provided
        if (dataFile != null) {
            Map<String, Object> dataset = loadDataset(dataFile);
            ParameterValidator paramValidator = new ParameterValidator();
            ValidationResult paramResult = paramValidator.validate(workflow, dataset);
            result.merge(paramResult);
        }

        // Print validation results
        if (result.isValid()) {
            TerminalAdapter.printValidationSuccess();
        } else {
            TerminalAdapter.printValidationErrors(result);
        }

        TerminalAdapter.printValidationWarnings(result);
    }

    /**
     * Load first row from CSV file.
     * Helper for validation to load dataset.
     */
    private static Map<String, Object> loadDataset(String dataFile) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            DatasetLoader loader = new DatasetLoader();
            Map<String, Map<String, String>> allRows = loader.loadCSV(new File(dataFile));
            if (!allRows.isEmpty()) {
                // Get first row from CSV and convert to Object map
                Map<String, String> firstRow = allRows.values().iterator().next();
                result.putAll(firstRow);
            }
        } catch (Exception e) {
            System.err.println("Error loading dataset: " + e.getMessage());
        }
        return result;
    }
}
