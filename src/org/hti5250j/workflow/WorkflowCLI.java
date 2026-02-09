package org.hti5250j.workflow;

import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.interfaces.SessionInterface;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
     * Load first row from CSV file using DatasetLoader.
     * Delegates to DatasetLoader for consistent CSV parsing.
     *
     * @param dataFile CSV file to load
     * @return Map of first row data (column name → value)
     */
    public static Map<String, Object> loadDataset(File dataFile) {
        Map<String, Object> result = new HashMap<>();
        try {
            DatasetLoader loader = new DatasetLoader();
            Map<String, Map<String, String>> allRows = loader.loadCSV(dataFile);
            if (!allRows.isEmpty()) {
                // Get first row from CSV
                Map<String, String> firstRow = allRows.values().iterator().next();
                // Convert Map<String, String> to Map<String, Object>
                result.putAll(firstRow);
            }
        } catch (Exception e) {
            System.err.println("Error loading dataset: " + e.getMessage());
        }
        return result;
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
                executeWorkflow(workflow, parsed.dataFile(), parsed.environment());
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Execute workflow with session integration.
     */
    private static void executeWorkflow(WorkflowSchema workflow, String dataFileArg, String environment) throws Exception {
        // Load dataset (if provided)
        Map<String, String> dataRow = new HashMap<>();
        if (dataFileArg != null) {
            DatasetLoader loader = new DatasetLoader();
            Map<String, Map<String, String>> csvData = loader.loadCSV(new File(dataFileArg));
            if (!csvData.isEmpty()) {
                // Get first row from CSV
                dataRow = csvData.values().iterator().next();
            }
        }

        // Extract LOGIN step to get host/user/password
        StepDef loginStep = workflow.getSteps().stream()
            .filter(s -> s.getAction() == ActionType.LOGIN)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Workflow requires LOGIN step"));

        // Create session from LOGIN step properties
        SessionInterface session = createSession(loginStep);

        // Setup artifact collection
        String workflowNamePath = workflow.getName().replaceAll("\\s+", "_");
        File artifactDir = new File("artifacts/" + workflowNamePath);
        artifactDir.mkdirs();
        ArtifactCollector collector = new ArtifactCollector(artifactDir);

        // Execute workflow
        DatasetLoader loader = new DatasetLoader();
        WorkflowRunner runner = new WorkflowRunner(session, loader, collector);

        try {
            runner.executeWorkflow(workflow, dataRow);
            System.out.println("✓ Workflow executed successfully");
            System.out.println("Artifacts saved to: " + artifactDir.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("✗ Workflow failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create Session5250 from LOGIN step properties.
     */
    private static SessionInterface createSession(StepDef loginStep) throws Exception {
        Properties props = new Properties();
        props.setProperty("SESSION_HOST", loginStep.getHost());
        props.setProperty("SESSION_USER", loginStep.getUser());
        props.setProperty("SESSION_PASSWORD", loginStep.getPassword());

        // Use default SessionConfig with dummy paths
        SessionConfig config = new SessionConfig("dummy", "dummy");

        return new Session5250(props, "workflow-session", "WorkflowSession", config);
    }
}
