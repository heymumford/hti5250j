/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkflowLoader Tests")
class WorkflowLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("load() should parse valid workflow YAML file")
    void testLoadValidWorkflow() throws Exception {
        // Create a minimal valid workflow YAML
        File workflowFile = tempDir.resolve("workflow.yaml").toFile();
        Files.write(workflowFile.toPath(), """
            name: TestWorkflow
            steps:
              - action: LOGIN
                host: testhost
                user: testuser
                password: testpass
            """.getBytes());

        WorkflowSchema workflow = WorkflowLoader.load(workflowFile);

        assertNotNull(workflow);
        assertEquals("TestWorkflow", workflow.getName());
        assertNotNull(workflow.getSteps());
    }

    @Test
    @DisplayName("load() should work with file path string")
    void testLoadWithString() throws Exception {
        File workflowFile = tempDir.resolve("workflow.yaml").toFile();
        Files.write(workflowFile.toPath(), """
            name: TestWorkflow
            steps: []
            """.getBytes());

        WorkflowSchema workflow = WorkflowLoader.load(workflowFile.getAbsolutePath());

        assertNotNull(workflow);
        assertEquals("TestWorkflow", workflow.getName());
    }

    @Test
    @DisplayName("load() should throw when file is null")
    void testLoadNullFile() {
        assertThrows(IllegalArgumentException.class, () -> WorkflowLoader.load((File) null));
    }

    @Test
    @DisplayName("load() should throw when file does not exist")
    void testLoadNonexistentFile() {
        File nonexistent = new File("/tmp/nonexistent-workflow-" + System.nanoTime() + ".yaml");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> WorkflowLoader.load(nonexistent));

        assertTrue(ex.getMessage().contains("not found"));
        assertTrue(ex.getMessage().contains(nonexistent.getAbsolutePath()));
    }

    @Test
    @DisplayName("load() should throw when path is directory, not file")
    void testLoadDirectory() {
        File dir = tempDir.toFile();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> WorkflowLoader.load(dir));

        assertTrue(ex.getMessage().contains("not a file"));
    }

    @Test
    @DisplayName("load() should throw for empty YAML file")
    void testLoadEmptyFile() throws Exception {
        File workflowFile = tempDir.resolve("empty.yaml").toFile();
        Files.write(workflowFile.toPath(), new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> WorkflowLoader.load(workflowFile));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("load() should handle workflow with multiple steps")
    void testLoadMultipleSteps() throws Exception {
        File workflowFile = tempDir.resolve("workflow.yaml").toFile();
        Files.write(workflowFile.toPath(), """
            name: MultiStepWorkflow
            steps:
              - action: LOGIN
                host: testhost
                user: user1
                password: pass1
              - action: NAVIGATE
                screen: MenuScreen
                keys: "[pf3]"
              - action: FILL
                fields:
                  AccountNumber: "123456"
              - action: SUBMIT
                key: enter
            """.getBytes());

        WorkflowSchema workflow = WorkflowLoader.load(workflowFile);

        assertNotNull(workflow);
        assertEquals("MultiStepWorkflow", workflow.getName());
        assertNotNull(workflow.getSteps());
    }

    @Test
    @DisplayName("load() should preserve workflow metadata")
    void testLoadWorkflowMetadata() throws Exception {
        File workflowFile = tempDir.resolve("workflow.yaml").toFile();
        Files.write(workflowFile.toPath(), """
            name: PaymentWorkflow
            description: Process payment transactions
            steps: []
            """.getBytes());

        WorkflowSchema workflow = WorkflowLoader.load(workflowFile);

        assertEquals("PaymentWorkflow", workflow.getName());
        assertEquals("Process payment transactions", workflow.getDescription());
    }

    @Test
    @DisplayName("load() should handle file with special characters in name")
    void testLoadFileWithSpecialChars() throws Exception {
        File workflowFile = tempDir.resolve("workflow-test_v1.2.yaml").toFile();
        Files.write(workflowFile.toPath(), """
            name: TestWorkflow
            steps: []
            """.getBytes());

        WorkflowSchema workflow = WorkflowLoader.load(workflowFile);

        assertNotNull(workflow);
        assertEquals("TestWorkflow", workflow.getName());
    }

    @Test
    @DisplayName("load() should fail gracefully for malformed YAML")
    void testLoadMalformedYAML() throws Exception {
        File workflowFile = tempDir.resolve("malformed.yaml").toFile();
        Files.write(workflowFile.toPath(), """
            name: Test
            steps: [
              invalid yaml syntax here
            """.getBytes());

        // Depending on YAML parser behavior, this may throw various exceptions
        // but should not silently return null or incorrect data
        Exception ex = assertThrows(Exception.class, () -> WorkflowLoader.load(workflowFile));
        assertNotNull(ex.getMessage());
    }

    @Test
    @DisplayName("load() overload works correctly")
    void testLoadOverloads() throws Exception {
        File workflowFile = tempDir.resolve("workflow.yaml").toFile();
        Files.write(workflowFile.toPath(), """
            name: TestWorkflow
            steps: []
            """.getBytes());

        // Both overloads should produce same result
        WorkflowSchema workflow1 = WorkflowLoader.load(workflowFile);
        WorkflowSchema workflow2 = WorkflowLoader.load(workflowFile.getAbsolutePath());

        assertEquals(workflow1.getName(), workflow2.getName());
    }

    @Test
    @DisplayName("load() should handle YAML with null fields")
    void testLoadYAMLWithNullFields() throws Exception {
        File workflowFile = tempDir.resolve("workflow.yaml").toFile();
        Files.write(workflowFile.toPath(), """
            name: TestWorkflow
            description:
            steps: []
            """.getBytes());

        WorkflowSchema workflow = WorkflowLoader.load(workflowFile);

        assertNotNull(workflow);
        assertEquals("TestWorkflow", workflow.getName());
    }

    @Test
    @DisplayName("load() should handle large workflow files")
    void testLoadLargeWorkflow() throws Exception {
        StringBuilder yaml = new StringBuilder("name: LargeWorkflow\nsteps:\n");
        for (int i = 0; i < 100; i++) {
            yaml.append("  - action: WAIT\n    timeout: 1000\n");
        }

        File workflowFile = tempDir.resolve("large.yaml").toFile();
        Files.write(workflowFile.toPath(), yaml.toString().getBytes());

        WorkflowSchema workflow = WorkflowLoader.load(workflowFile);

        assertNotNull(workflow);
        assertEquals("LargeWorkflow", workflow.getName());
    }
}
