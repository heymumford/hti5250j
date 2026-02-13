package org.hti5250j.workflow;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

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

            WorkflowSchema workflow = WorkflowLoader.load(parsed.workflowFile());
            TerminalAdapter.printWorkflowLoaded(workflow.getName(), workflow.getSteps().size());

            if ("validate".equals(parsed.action())) {
                validateWorkflow(workflow, parsed.dataFile());
                return;
            }

            if ("simulate".equals(parsed.action())) {
                simulateWorkflow(workflow, parsed.dataFile());
                return;
            }

            if ("run".equals(parsed.action())) {
                if (parsed.dataFile() != null) {
                    DatasetLoader loader = new DatasetLoader();
                    Map<String, Map<String, String>> allRows = loader.loadCSV(new File(parsed.dataFile()));

                    if (allRows.size() > 1) {
                        TerminalAdapter.printBatchMode(allRows.size());
                        BatchMetrics metrics = WorkflowExecutor.executeBatch(workflow, parsed.dataFile(), parsed.environment());
                        metrics.print();

                        if (metrics.failureCount() > 0) {
                            System.exit(1);
                        }
                    } else {
                        WorkflowExecutor.execute(workflow, parsed.dataFile(), parsed.environment());
                    }
                } else {
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

        if (dataFile != null) {
            Map<String, Object> dataset = loadDataset(dataFile);
            ParameterValidator paramValidator = new ParameterValidator();
            ValidationResult paramResult = paramValidator.validate(workflow, dataset);
            result.merge(paramResult);
        }

        if (result.isValid()) {
            TerminalAdapter.printValidationSuccess();
        } else {
            TerminalAdapter.printValidationErrors(result);
        }

        TerminalAdapter.printValidationWarnings(result);
    }

    /**
     * Simulate workflow execution without real i5 connection.
     * Predicts outcome, timeouts, and warnings.
     * Provides approval gate recommendation before real execution.
     */
    private static void simulateWorkflow(WorkflowSchema workflow, String dataFile) {
        WorkflowTolerance tolerance = workflow.getTolerances() != null ?
            workflow.getTolerances() :
            WorkflowTolerance.defaults(workflow.getName());

        TerminalAdapter.printSimulationStarted(tolerance);

        if (dataFile == null) {
            WorkflowSimulator simulator = new WorkflowSimulator();
            WorkflowSimulation result = simulator.simulate(workflow, new HashMap<>(), tolerance);
            printSimulationResult(result);
            return;
        }

        try {
            DatasetLoader loader = new DatasetLoader();
            Map<String, Map<String, String>> allRows = loader.loadCSV(new File(dataFile));
            TerminalAdapter.printDatasetLoaded(allRows.size());

            WorkflowSimulator simulator = new WorkflowSimulator();
            List<WorkflowSimulation> simulations = new java.util.ArrayList<>();
            int successCount = 0;
            int timeoutCount = 0;
            int errorCount = 0;

            for (Map.Entry<String, Map<String, String>> entry : allRows.entrySet()) {
                WorkflowSimulation result = simulator.simulate(workflow, entry.getValue(), tolerance);
                simulations.add(result);

                if ("success".equals(result.predictedOutcome())) {
                    successCount++;
                } else if ("timeout".equals(result.predictedOutcome())) {
                    timeoutCount++;
                } else {
                    errorCount++;
                }
            }

            printBatchSimulationSummary(successCount, timeoutCount, errorCount, allRows.size());

            for (int i = 0; i < simulations.size(); i++) {
                System.out.printf("Row %d: %s%n", i + 1, simulations.get(i).predictedOutcome());
                if (!simulations.get(i).warnings().isEmpty()) {
                    for (String warning : simulations.get(i).warnings()) {
                        System.out.println("  ⚠ " + warning);
                    }
                }
            }

            if (errorCount > 0 || timeoutCount > 0) {
                System.out.println("\n⛔ SIMULATION BLOCKED FOR EXECUTION");
                System.out.println("  Fix issues before running: eliminate timeouts and validation errors");
            } else {
                System.out.println("\n✅ APPROVED FOR EXECUTION");
                System.out.println("  All simulations passed. Ready to run on real i5.");
            }

        } catch (Exception e) {
            TerminalAdapter.printError("Simulation failed", e);
            System.exit(1);
        }
    }

    /**
     * Print single simulation result with details.
     */
    private static void printSimulationResult(WorkflowSimulation result) {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("  SIMULATION RESULT");
        System.out.println("═".repeat(70));
        System.out.printf("  Outcome: %s%n", result.predictedOutcome());
        System.out.printf("  Steps completed: %d%n", result.steps().size());

        if (!result.warnings().isEmpty()) {
            System.out.println("  Warnings:");
            for (String warning : result.warnings()) {
                System.out.println("    ⚠ " + warning);
            }
        }

        System.out.println("═".repeat(70));

        if ("success".equals(result.predictedOutcome())) {
            System.out.println("\n✅ APPROVED FOR EXECUTION");
            System.out.println("  Simulation successful. Ready to run on real i5.");
        } else {
            System.out.println("\n⛔ BLOCKED FOR EXECUTION");
            System.out.println("  Fix the " + result.predictedOutcome() + " before running.");
        }
    }

    /**
     * Print batch simulation summary statistics.
     */
    private static void printBatchSimulationSummary(int success, int timeout, int error, int total) {
        System.out.println("\n" + "═".repeat(70));
        System.out.println("  BATCH SIMULATION SUMMARY");
        System.out.println("═".repeat(70));
        System.out.printf("  Total simulations: %d%n", total);
        System.out.printf("  Success: %d (%.1f%%)%n", success, (success * 100.0) / total);
        System.out.printf("  Timeout: %d (%.1f%%)%n", timeout, (timeout * 100.0) / total);
        System.out.printf("  Error: %d (%.1f%%)%n", error, (error * 100.0) / total);
        System.out.println("═".repeat(70));
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
                Map<String, String> firstRow = allRows.values().iterator().next();
                result.putAll(firstRow);
            }
        } catch (Exception e) {
            System.err.println("Error loading dataset: " + e.getMessage());
        }
        return result;
    }
}
