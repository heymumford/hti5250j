/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Handles all terminal/CLI output for workflow operations.
 * Encapsulates formatting and user-facing messages.
 */
public class TerminalAdapter {

    /**
     * Print help message.
     */
    public static void printHelp() {
        System.out.println("""
            Usage: i5250 run|validate|simulate <workflow.yaml> [options]

            Run, validate, or simulate HTI5250j workflow from YAML file.

            Actions:
              run                Run the workflow against i5
              validate           Validate workflow structure without running
              simulate           Dry-run without i5 connection (approval gate)

            Options:
              --data <file>     CSV data file for parameter binding
              --env <env>       Environment name (dev/test/prod)
              --help            Show this help message

            Examples:
              i5250 validate login.yaml
              i5250 validate payment.yaml --data transactions.csv
              i5250 simulate payment.yaml --data transactions.csv
              i5250 run login.yaml
              i5250 run payment.yaml --data transactions.csv
              i5250 run settlement.yaml --data batch.csv --env prod
            """);
    }

    /**
     * Print workflow loaded message.
     */
    public static void printWorkflowLoaded(String workflowName, int stepCount) {
        System.out.println("Loaded workflow: " + workflowName);
        System.out.println("Workflow contains " + stepCount + " steps");
    }

    /**
     * Print validation success.
     */
    public static void printValidationSuccess() {
        System.out.println("✓ Workflow is valid");
    }

    /**
     * Print validation errors.
     */
    public static void printValidationErrors(ValidationResult result) {
        System.out.println("✗ Validation errors found:");
        for (ValidationError error : result.getErrors()) {
            System.err.printf("  [Step %d] %s: %s%n",
                error.stepIndex(), error.fieldName(), error.message());
            if (error.suggestedFix() != null) {
                System.err.printf("    Fix: %s%n", error.suggestedFix());
            }
        }
    }

    /**
     * Print validation warnings.
     */
    public static void printValidationWarnings(ValidationResult result) {
        if (!result.getWarnings().isEmpty()) {
            System.out.println("⚠ Warnings:");
            for (ValidationWarning warning : result.getWarnings()) {
                System.out.printf("  [Step %d] %s: %s%n",
                    warning.stepIndex(), warning.fieldName(), warning.message());
            }
        }
    }

    /**
     * Print workflow execution success.
     */
    public static void printExecutionSuccess(String artifactPath) {
        System.out.println("✓ Workflow executed successfully");
        System.out.println("Artifacts saved to: " + artifactPath);
    }

    /**
     * Print workflow execution failure.
     */
    public static void printExecutionFailure(String message) {
        System.err.println("✗ Workflow failed: " + message);
    }

    /**
     * Print batch mode activated message.
     */
    public static void printBatchMode(int rowCount) {
        System.out.println("Batch mode: " + rowCount + " workflows");
        System.out.println("Executing in parallel using virtual threads...");
    }

    /**
     * Print error with stack trace (for debugging).
     */
    public static void printError(String message, Exception e) {
        System.err.println("Error: " + message);
        e.printStackTrace();
    }

    /**
     * Print simulation started message with tolerance settings.
     */
    public static void printSimulationStarted(WorkflowTolerance tolerance) {
        System.out.println("Starting workflow simulation (dry-run, no i5 connection)...");
        System.out.printf("  Max duration: %dms%n", tolerance.maxDurationMs());
        System.out.printf("  Field precision: %.2f%n", tolerance.fieldPrecision());
    }

    /**
     * Print dataset loaded message.
     */
    public static void printDatasetLoaded(int rowCount) {
        System.out.println("Loaded dataset: " + rowCount + " rows");
        System.out.println("Running simulations for each row...");
    }
}
