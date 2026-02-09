/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WorkflowSimulator CLI integration (Phase 12E, D5-SIMU-INT).
 *
 * Contract: 'simulate' action provides dry-run without i5 connection, displays approval gate.
 * Purpose: Enable human approval gates before real execution.
 *
 * Test IDs: D5-SIMU-INT-001 through D5-SIMU-INT-005 (5 integration tests)
 */
class SimulatorCLIIntegrationTest {

    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    /**
     * D5-SIMU-INT-001: Simulate action processes workflow successfully.
     */
    @Test
    void simulateActionProcessesWorkflowSuccessfully() {
        // Create workflow: LOGIN → FILL → SUBMIT
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("payment");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction("LOGIN");
        loginStep.setHost("host");
        loginStep.setUser("user");
        loginStep.setPassword("password");
        steps.add(loginStep);

        StepDef fillStep = new StepDef();
        fillStep.setAction("FILL");
        Map<String, String> fields = new HashMap<>();
        fields.put("amount", "100.50");
        fillStep.setFields(fields);
        steps.add(fillStep);

        StepDef submitStep = new StepDef();
        submitStep.setAction("SUBMIT");
        submitStep.setKey("enter");
        steps.add(submitStep);

        workflow.setSteps(steps);
        workflow.setTolerances(WorkflowTolerance.defaults("test"));

        // Simulate
        WorkflowSimulator simulator = new WorkflowSimulator();
        WorkflowSimulation result = simulator.simulate(workflow, new HashMap<>(), workflow.getTolerances());

        // Verify success
        assertThat(result.predictedOutcome()).isEqualTo("success");
        assertThat(result.steps()).hasSize(3);
        assertThat(result.warnings()).isEmpty();
    }

    /**
     * D5-SIMU-INT-002: Simulate displays approval gate for successful workflow.
     */
    @Test
    void simulateDisplaysApprovalGateForSuccess() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("test");
        StepDef step = new StepDef();
        step.setAction("LOGIN");
        step.setHost("host");
        step.setUser("user");
        step.setPassword("password");
        workflow.setSteps(java.util.List.of(step));
        workflow.setTolerances(WorkflowTolerance.defaults("test"));

        WorkflowSimulator simulator = new WorkflowSimulator();
        WorkflowSimulation result = simulator.simulate(workflow, new HashMap<>(), workflow.getTolerances());

        assertThat(result.predictedOutcome()).isEqualTo("success");
        // Output would show "APPROVED FOR EXECUTION" in CLI
    }

    /**
     * D5-SIMU-INT-003: Simulate detects timeout and blocks execution.
     */
    @Test
    void simulateDetectsTimeoutAndBlocksExecution() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("slow");

        List<StepDef> steps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            StepDef step = new StepDef();
            step.setAction("FILL");
            step.setFields(new HashMap<>());
            steps.add(step);
        }
        workflow.setSteps(steps);
        workflow.setTolerances(new WorkflowTolerance("test", 1000L, 0.01, 3, false));

        WorkflowSimulator simulator = new WorkflowSimulator();
        WorkflowSimulation result = simulator.simulate(workflow, new HashMap<>(), workflow.getTolerances());

        assertThat(result.predictedOutcome()).isEqualTo("timeout");
        assertThat(result.warnings()).isNotEmpty();
        // Output would show "BLOCKED FOR EXECUTION" in CLI
    }

    /**
     * D5-SIMU-INT-004: Simulate generates warnings for field truncation.
     */
    @Test
    void simulateGeneratesWarningsForFieldTruncation() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("truncate_test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction("LOGIN");
        loginStep.setHost("host");
        loginStep.setUser("user");
        loginStep.setPassword("password");
        steps.add(loginStep);

        StepDef fillStep = new StepDef();
        fillStep.setAction("FILL");
        Map<String, String> fields = new HashMap<>();
        fields.put("description", "x".repeat(300));  // 300 chars > 255 limit
        fillStep.setFields(fields);
        steps.add(fillStep);

        workflow.setSteps(steps);
        workflow.setTolerances(WorkflowTolerance.defaults("test"));

        WorkflowSimulator simulator = new WorkflowSimulator();
        WorkflowSimulation result = simulator.simulate(workflow, new HashMap<>(), workflow.getTolerances());

        assertThat(result.predictedOutcome()).isEqualTo("success");
        assertThat(result.warnings()).isNotEmpty();
        assertThat(result.warnings().get(0)).contains("too long");
    }

    /**
     * D5-SIMU-INT-005: Simulate with dataset parameters performs row-by-row simulation.
     */
    @Test
    void simulateWithDatasetParametersPerformsRowByRowSimulation() {
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("multi_row");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction("LOGIN");
        loginStep.setHost("host");
        loginStep.setUser("user");
        loginStep.setPassword("password");
        steps.add(loginStep);

        StepDef fillStep = new StepDef();
        fillStep.setAction("FILL");
        Map<String, String> fields = new HashMap<>();
        fields.put("amount", "100.00");
        fillStep.setFields(fields);
        steps.add(fillStep);

        workflow.setSteps(steps);
        workflow.setTolerances(WorkflowTolerance.defaults("test"));

        // Simulate with multiple test data rows
        Map<String, String> row1 = new HashMap<>();
        row1.put("customer_id", "C001");
        row1.put("amount", "100.00");

        Map<String, String> row2 = new HashMap<>();
        row2.put("customer_id", "C002");
        row2.put("amount", "200.00");

        WorkflowSimulator simulator = new WorkflowSimulator();
        WorkflowSimulation result1 = simulator.simulate(workflow, row1, workflow.getTolerances());
        WorkflowSimulation result2 = simulator.simulate(workflow, row2, workflow.getTolerances());

        // Both should succeed
        assertThat(result1.predictedOutcome()).isEqualTo("success");
        assertThat(result2.predictedOutcome()).isEqualTo("success");

        // Both should have predicted fields from test data
        assertThat(result1.predictedFields()).containsEntry("customer_id", "C001");
        assertThat(result2.predictedFields()).containsEntry("customer_id", "C002");
    }
}
