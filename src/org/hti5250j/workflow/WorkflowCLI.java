package org.hti5250j.workflow;

import org.yaml.snakeyaml.Yaml;
import java.io.File;

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
     */
    public static CommandLineArgs parseArgs(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: i5250 run <workflow.yaml> [--data <data.csv>] [--env <environment>]");
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
            Usage: i5250 run <workflow.yaml> [options]

            Run HTI5250j workflow from YAML file.

            Options:
              --data <file>     CSV data file for parameter binding
              --env <env>       Environment name (dev/test/prod)
              --help            Show this help message

            Examples:
              i5250 run login.yaml
              i5250 run payment.yaml --data transactions.csv
              i5250 run settlement.yaml --data batch.csv --env prod
            """;
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

            if (!"run".equals(parsed.action())) {
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

            // TODO: Execute workflow with session
            // For now, just demonstrate loading succeeded
            System.out.println("Workflow contains " + workflow.getSteps().size() + " steps");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
