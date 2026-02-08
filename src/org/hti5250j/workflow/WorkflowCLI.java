package org.hti5250j.workflow;

import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

public class WorkflowCLI {

    /**
     * Immutable command-line arguments record.
     */
    public record CommandLineArgs(
        String action,
        String workflowFile,
        String dataFile,
        String environment
    ) {}

    /**
     * Parse command-line arguments.
     * Usage: i5250 run <workflow.yaml> [--data <data.csv>] [--env <environment>]
     *        i5250 validate <workflow.yaml> [--data <data.csv>]
     */
    public static CommandLineArgs parseArgs(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: i5250 run|validate <workflow.yaml> [--data <data.csv>] [--env <environment>]");
        }

        String action = args[0];
        String workflowFile = args[1];
        String dataFile = null;
        String environment = null;

        // Parse optional arguments
        for (int i = 2; i < args.length; i++) {
            if ("--data".equals(args[i]) && i + 1 < args.length) {
                dataFile = args[++i];
            } else if ("--env".equals(args[i]) && i + 1 < args.length) {
                environment = args[++i];
            }
        }

        return new CommandLineArgs(action, workflowFile, dataFile, environment);
    }

    /**
     * Load workflow from YAML file.
     */
    public static WorkflowSchema loadWorkflow(File workflowFile) throws Exception {
        Yaml yaml = WorkflowYAML.getInstance();
        return yaml.loadAs(new java.io.FileReader(workflowFile), WorkflowSchema.class);
    }

    /**
     * Print help message.
     */
    public static String helpMessage() {
        return """
            Usage: i5250 run|validate <workflow.yaml> [options]

            Run or validate HTI5250j workflow from YAML file.

            Actions:
              run                Run the workflow against i5
              validate           Validate workflow structure without running

            Options:
              --data <file>     CSV data file for parameter binding
              --env <env>       Environment name (dev/test/prod)
              --help            Show this help message

            Examples:
              i5250 validate login.yaml
              i5250 validate payment.yaml --data transactions.csv
              i5250 run login.yaml
              i5250 run payment.yaml --data transactions.csv
              i5250 run settlement.yaml --data batch.csv --env prod
            """;
    }

    /**
     * Validate workflow against schema and dataset (if provided).
     */
    public static void validateWorkflow(WorkflowSchema workflow, File dataFile) {
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
            System.out.println("✓ Workflow is valid");
        } else {
            System.out.println("✗ Validation errors found:");
            for (ValidationError error : result.getErrors()) {
                System.err.printf("  [Step %d] %s: %s%n",
                    error.stepIndex(), error.fieldName(), error.message());
                if (error.suggestedFix() != null) {
                    System.err.printf("    Fix: %s%n", error.suggestedFix());
                }
            }
        }

        if (!result.getWarnings().isEmpty()) {
            System.out.println("⚠ Warnings:");
            for (ValidationWarning warning : result.getWarnings()) {
                System.out.printf("  [Step %d] %s: %s%n",
                    warning.stepIndex(), warning.fieldName(), warning.message());
            }
        }
    }

    /**
     * Load dataset from CSV file (simplified - first row is headers).
     */
    public static Map<String, Object> loadDataset(File dataFile) {
        Map<String, Object> dataset = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(dataFile.getAbsolutePath()))) {
            String headerLine = reader.readLine();
            if (headerLine != null) {
                String[] headers = headerLine.split(",");
                String dataLine = reader.readLine();
                if (dataLine != null) {
                    String[] values = dataLine.split(",");
                    for (int i = 0; i < headers.length && i < values.length; i++) {
                        dataset.put(headers[i].trim(), values[i].trim());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading dataset: " + e.getMessage());
        }
        return dataset;
    }

    /**
     * Entry point for command-line execution.
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
                System.out.println(helpMessage());
                return;
            }

            CommandLineArgs parsed = parseArgs(args);

            if (!"run".equals(parsed.action()) && !"validate".equals(parsed.action())) {
                System.err.println("Unknown action: " + parsed.action());
                System.out.println(helpMessage());
                System.exit(1);
            }

            // Load workflow
            File workflowFile = new File(parsed.workflowFile());
            if (!workflowFile.exists()) {
                System.err.println("Workflow file not found: " + parsed.workflowFile());
                System.exit(1);
            }

            WorkflowSchema workflow = loadWorkflow(workflowFile);
            System.out.println("Loaded workflow: " + workflow.getName());
            System.out.println("Workflow contains " + workflow.getSteps().size() + " steps");

            // Handle validate action
            if ("validate".equals(parsed.action())) {
                File dataFile = parsed.dataFile() != null ? new File(parsed.dataFile()) : null;
                validateWorkflow(workflow, dataFile);
                return;
            }

            // Handle run action
            if ("run".equals(parsed.action())) {
                // TODO: Execute workflow with session
                System.out.println("Ready to execute (session integration pending)");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
