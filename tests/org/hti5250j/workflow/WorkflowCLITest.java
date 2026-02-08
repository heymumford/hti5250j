package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

class WorkflowCLITest {

    @Test
    void testParseCommandLineArgs() {
        String[] args = {"run", "workflow.yaml", "--data", "data.csv"};
        WorkflowCLI.CommandLineArgs parsed = WorkflowCLI.parseArgs(args);

        assertThat(parsed.action()).isEqualTo("run");
        assertThat(parsed.workflowFile()).isEqualTo("workflow.yaml");
        assertThat(parsed.dataFile()).isEqualTo("data.csv");
    }

    @Test
    void testParseArgsWithEnvironment() {
        String[] args = {"run", "workflow.yaml", "--data", "data.csv", "--env", "prod"};
        WorkflowCLI.CommandLineArgs parsed = WorkflowCLI.parseArgs(args);

        assertThat(parsed.environment()).isEqualTo("prod");
    }

    @Test
    void testDefaultsWhenNoDataFile() {
        String[] args = {"run", "workflow.yaml"};
        WorkflowCLI.CommandLineArgs parsed = WorkflowCLI.parseArgs(args);

        assertThat(parsed.dataFile()).isNull();
    }

    @Test
    void testLoadWorkflowFromFile(@TempDir File tempDir) throws Exception {
        // Create sample workflow YAML
        File workflowFile = new File(tempDir, "test.yaml");
        try (FileWriter fw = new FileWriter(workflowFile)) {
            fw.write("name: Test Flow\n");
            fw.write("description: Sample workflow\n");
            fw.write("environment: test\n");
            fw.write("steps:\n");
            fw.write("  - action: LOGIN\n");
            fw.write("    host: localhost\n");
        }

        WorkflowSchema workflow = WorkflowCLI.loadWorkflow(workflowFile);

        assertThat(workflow.getName()).isEqualTo("Test Flow");
        assertThat(workflow.getSteps()).hasSize(1);
    }

    @Test
    void testHelpMessage() {
        String help = WorkflowCLI.helpMessage();

        assertThat(help).contains("Usage:")
                        .contains("run")
                        .contains("workflow")
                        .contains("--data");
    }
}
