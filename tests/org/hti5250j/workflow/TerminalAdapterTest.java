/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TerminalAdapter Tests")
class TerminalAdapterTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @BeforeEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private String getOutput() {
        return outputStream.toString();
    }

    private String getErrorOutput() {
        return errorStream.toString();
    }

    @Test
    @DisplayName("printHelp() should output usage information")
    void testPrintHelp() {
        TerminalAdapter.printHelp();
        String output = getOutput();

        assertTrue(output.contains("Usage:"));
        assertTrue(output.contains("i5250"));
        assertTrue(output.contains("run|validate|simulate"));
        assertTrue(output.contains("Actions:"));
        assertTrue(output.contains("Options:"));
        assertTrue(output.contains("Examples:"));
    }

    @Test
    @DisplayName("printHelp() should include all action descriptions")
    void testPrintHelpActionsDescribed() {
        TerminalAdapter.printHelp();
        String output = getOutput();

        assertTrue(output.contains("Run the workflow against i5"));
        assertTrue(output.contains("Validate workflow structure"));
        assertTrue(output.contains("Dry-run without i5 connection"));
    }

    @Test
    @DisplayName("printWorkflowLoaded() should show workflow name and step count")
    void testPrintWorkflowLoaded() {
        TerminalAdapter.printWorkflowLoaded("payment.yaml", 5);
        String output = getOutput();

        assertTrue(output.contains("Loaded workflow: payment.yaml"));
        assertTrue(output.contains("5 steps"));
    }

    @Test
    @DisplayName("printWorkflowLoaded() should handle single step")
    void testPrintWorkflowLoadedSingleStep() {
        TerminalAdapter.printWorkflowLoaded("simple.yaml", 1);
        String output = getOutput();

        assertTrue(output.contains("simple.yaml"));
        assertTrue(output.contains("1 steps"));
    }

    @Test
    @DisplayName("printValidationSuccess() should output success message")
    void testPrintValidationSuccess() {
        TerminalAdapter.printValidationSuccess();
        String output = getOutput();

        assertTrue(output.contains("✓ Workflow is valid"));
    }

    @Test
    @DisplayName("printValidationErrors() should list all errors")
    void testPrintValidationErrors() {
        ValidationResult result = new ValidationResult();
        result.addError(0, "host", "Host field required", "Add 'host:' to step");
        result.addError(1, "action", "Unknown action type", "Use LOGIN, FILL, SUBMIT, ASSERT, or CAPTURE");

        TerminalAdapter.printValidationErrors(result);
        String errorOutput = getErrorOutput();

        assertTrue(errorOutput.contains("Validation errors found:"));
        assertTrue(errorOutput.contains("[Step 0]"));
        assertTrue(errorOutput.contains("host"));
        assertTrue(errorOutput.contains("Host field required"));
        assertTrue(errorOutput.contains("Add 'host:' to step"));
    }

    @Test
    @DisplayName("printValidationErrors() should show step index in brackets")
    void testPrintValidationErrorsStepIndex() {
        ValidationResult result = new ValidationResult();
        result.addError(5, "field", "Error message", "Fix it");

        TerminalAdapter.printValidationErrors(result);
        String errorOutput = getErrorOutput();

        assertTrue(errorOutput.contains("[Step 5]"));
    }

    @Test
    @DisplayName("printValidationErrors() should handle missing suggested fix")
    void testPrintValidationErrorsNoSuggestedFix() {
        ValidationResult result = new ValidationResult();
        result.addError(0, "field", "Error message", null);

        TerminalAdapter.printValidationErrors(result);
        String errorOutput = getErrorOutput();

        assertTrue(errorOutput.contains("Error message"));
        // Should not print "Fix: null"
        assertFalse(errorOutput.contains("Fix: null"));
    }

    @Test
    @DisplayName("printValidationWarnings() should show all warnings")
    void testPrintValidationWarnings() {
        ValidationResult result = new ValidationResult();
        result.addWarning(2, "timeout", "Consider using longer timeout");

        TerminalAdapter.printValidationWarnings(result);
        String output = getOutput();

        assertTrue(output.contains("⚠ Warnings:"));
        assertTrue(output.contains("[Step 2]"));
        assertTrue(output.contains("timeout"));
        assertTrue(output.contains("Consider using longer timeout"));
    }

    @Test
    @DisplayName("printValidationWarnings() should do nothing if no warnings")
    void testPrintValidationWarningsEmpty() {
        ValidationResult result = new ValidationResult();

        TerminalAdapter.printValidationWarnings(result);
        String output = getOutput();

        // Should not print warnings header if empty
        assertTrue(!output.contains("⚠ Warnings:") || output.isEmpty());
    }

    @Test
    @DisplayName("printExecutionSuccess() should show artifact path")
    void testPrintExecutionSuccess() {
        TerminalAdapter.printExecutionSuccess("/tmp/artifacts");
        String output = getOutput();

        assertTrue(output.contains("✓ Workflow executed successfully"));
        assertTrue(output.contains("Artifacts saved to: /tmp/artifacts"));
    }

    @Test
    @DisplayName("printExecutionFailure() should show failure message")
    void testPrintExecutionFailure() {
        TerminalAdapter.printExecutionFailure("Connection timeout");
        String errorOutput = getErrorOutput();

        assertTrue(errorOutput.contains("✗ Workflow failed:"));
        assertTrue(errorOutput.contains("Connection timeout"));
    }

    @Test
    @DisplayName("printBatchMode() should show row count and parallel execution info")
    void testPrintBatchMode() {
        TerminalAdapter.printBatchMode(100);
        String output = getOutput();

        assertTrue(output.contains("Batch mode: 100 workflows"));
        assertTrue(output.contains("Executing in parallel using virtual threads"));
    }

    @Test
    @DisplayName("printError() should output error message and stack trace")
    void testPrintError() {
        Exception e = new RuntimeException("Test error");
        TerminalAdapter.printError("Operation failed", e);
        String errorOutput = getErrorOutput();

        assertTrue(errorOutput.contains("Error: Operation failed"));
        assertTrue(errorOutput.contains("java.lang.RuntimeException"));
    }

    @Test
    @DisplayName("printSimulationStarted() should show tolerance settings")
    void testPrintSimulationStarted() {
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");
        TerminalAdapter.printSimulationStarted(tolerance);
        String output = getOutput();

        assertTrue(output.contains("Starting workflow simulation"));
        assertTrue(output.contains("Max duration:"));
        assertTrue(output.contains("Field precision:"));
    }

    @Test
    @DisplayName("printDatasetLoaded() should show row count")
    void testPrintDatasetLoaded() {
        TerminalAdapter.printDatasetLoaded(50);
        String output = getOutput();

        assertTrue(output.contains("Loaded dataset: 50 rows"));
        assertTrue(output.contains("Running simulations for each row"));
    }
}
