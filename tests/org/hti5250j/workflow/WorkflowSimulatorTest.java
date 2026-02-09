/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Integration tests for WorkflowSimulator dry-run executor (Phase 12E, D5-SIM).
 *
 * Contract: Simulator predicts workflow outcome without i5 connection.
 * Purpose: Enable human approval gates before real execution.
 *
 * Test IDs: D5-SIM-001 through D5-SIM-008 (8 integration tests)
 */
class WorkflowSimulatorTest {

    /**
     * D5-SIM-001: Happy path simulation succeeds.
     */
    @Test
    void happyPathSimulationSucceeds() {
        WorkflowSimulator simulator = new WorkflowSimulator();

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
        fields.put("account", "ACC123");
        fillStep.setFields(fields);
        steps.add(fillStep);

        StepDef submitStep = new StepDef();
        submitStep.setAction("SUBMIT");
        submitStep.setKey("enter");
        steps.add(submitStep);

        workflow.setSteps(steps);

        Map<String, String> testData = new HashMap<>();
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        WorkflowSimulation result = simulator.simulate(workflow, testData, tolerance);

        assertThat(result.predictedOutcome()).isEqualTo("success");
        assertThat(result.steps()).hasSize(3);
        assertThat(result.steps().get(0).stepName()).isEqualTo("LOGIN");
        assertThat(result.warnings()).isEmpty();
    }

    /**
     * D5-SIM-002: Predicted timeout when duration exceeds tolerance.
     */
    @Test
    void timeoutPredictedWhenDurationExceedsLimit() {
        WorkflowSimulator simulator = new WorkflowSimulator();

        // Create workflow with many steps
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("slow_workflow");

        List<StepDef> steps = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            StepDef step = new StepDef();
            step.setAction("FILL");
            step.setFields(new HashMap<>());
            steps.add(step);
        }

        workflow.setSteps(steps);

        Map<String, String> testData = new HashMap<>();
        WorkflowTolerance tolerance = new WorkflowTolerance("test", 1000L, 0.01, 3, false);

        WorkflowSimulation result = simulator.simulate(workflow, testData, tolerance);

        assertThat(result.predictedOutcome()).isEqualTo("timeout");
        assertThat(result.warnings()).isNotEmpty();
    }

    /**
     * D5-SIM-003: Predicted validation error for invalid field.
     */
    @Test
    void validationErrorForMissingAssertField() {
        WorkflowSimulator simulator = new WorkflowSimulator();

        // Create workflow with invalid ASSERT
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("bad_workflow");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction("LOGIN");
        loginStep.setHost("host");
        loginStep.setUser("user");
        loginStep.setPassword("password");
        steps.add(loginStep);

        StepDef assertStep = new StepDef();
        assertStep.setAction("ASSERT");
        // No screen or text specified
        steps.add(assertStep);

        workflow.setSteps(steps);

        Map<String, String> testData = new HashMap<>();
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        WorkflowSimulation result = simulator.simulate(workflow, testData, tolerance);

        assertThat(result.predictedOutcome()).isEqualTo("validation_error");
        assertThat(result.warnings()).isNotEmpty();
    }

    /**
     * D5-SIM-004: Warning generated for field truncation.
     */
    @Test
    void warningGeneratedForFieldTruncation() {
        WorkflowSimulator simulator = new WorkflowSimulator();

        // Create workflow with oversized field
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("truncation_test");

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

        Map<String, String> testData = new HashMap<>();
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        WorkflowSimulation result = simulator.simulate(workflow, testData, tolerance);

        assertThat(result.predictedOutcome()).isEqualTo("success");
        assertThat(result.warnings()).isNotEmpty();
        assertThat(result.warnings().get(0)).contains("too long");
    }

    /**
     * D5-SIM-005: Warning generated for precision loss.
     */
    @Test
    void warningGeneratedForPrecisionLoss() {
        WorkflowSimulator simulator = new WorkflowSimulator();

        // Create workflow with precision loss
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("precision_test");

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
        fields.put("amount", "123.456");  // 3 decimals > 0.01 tolerance (2 decimals)
        fillStep.setFields(fields);
        steps.add(fillStep);

        workflow.setSteps(steps);

        Map<String, String> testData = new HashMap<>();
        WorkflowTolerance tolerance = new WorkflowTolerance("test", 300000L, 0.01, 3, false);

        WorkflowSimulation result = simulator.simulate(workflow, testData, tolerance);

        assertThat(result.predictedOutcome()).isEqualTo("success");
        assertThat(result.warnings()).isNotEmpty();
    }

    /**
     * D5-SIM-006: Multiple warnings accumulated across steps.
     */
    @Test
    void multipleWarningsAccumulated() {
        WorkflowSimulator simulator = new WorkflowSimulator();

        // Create workflow with multiple warnings
        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("multi_warning");

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
        fields.put("desc", "x".repeat(300));      // Truncation warning
        fields.put("amount", "123.456");           // Precision loss warning
        fillStep.setFields(fields);
        steps.add(fillStep);

        workflow.setSteps(steps);

        Map<String, String> testData = new HashMap<>();
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        WorkflowSimulation result = simulator.simulate(workflow, testData, tolerance);

        assertThat(result.warnings().size()).isGreaterThanOrEqualTo(2);
    }

    /**
     * D5-SIM-007: Simulation completes without i5 connection.
     */
    @Test
    void simulationCompletesWithoutI5Connection() {
        WorkflowSimulator simulator = new WorkflowSimulator();

        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("fast_test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction("LOGIN");
        loginStep.setHost("host");
        loginStep.setUser("user");
        loginStep.setPassword("password");
        steps.add(loginStep);

        StepDef fillStep = new StepDef();
        fillStep.setAction("FILL");
        fillStep.setFields(new HashMap<>());
        steps.add(fillStep);

        StepDef submitStep = new StepDef();
        submitStep.setAction("SUBMIT");
        submitStep.setKey("enter");
        steps.add(submitStep);

        workflow.setSteps(steps);

        Map<String, String> testData = new HashMap<>();
        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        long startNs = System.nanoTime();
        WorkflowSimulation result = simulator.simulate(workflow, testData, tolerance);
        long durationNs = System.nanoTime() - startNs;
        long durationMs = durationNs / 1_000_000;

        assertThat(result).isNotNull();
        assertThat(durationMs).isLessThan(100);  // Should be instant (no i5 connection)
    }

    /**
     * D5-SIM-008: Predicted fields match expected schema.
     */
    @Test
    void predictedFieldsMatchSchema() {
        WorkflowSimulator simulator = new WorkflowSimulator();

        WorkflowSchema workflow = new WorkflowSchema();
        workflow.setName("field_test");

        List<StepDef> steps = new ArrayList<>();

        StepDef loginStep = new StepDef();
        loginStep.setAction("LOGIN");
        loginStep.setHost("host");
        loginStep.setUser("user");
        loginStep.setPassword("password");
        steps.add(loginStep);

        StepDef fillStep = new StepDef();
        fillStep.setAction("FILL");
        fillStep.setFields(new HashMap<>());
        steps.add(fillStep);

        workflow.setSteps(steps);

        Map<String, String> testData = new HashMap<>();
        testData.put("account_number", "ACC123456");
        testData.put("amount", "1000.00");

        WorkflowTolerance tolerance = WorkflowTolerance.defaults("test");

        WorkflowSimulation result = simulator.simulate(workflow, testData, tolerance);

        assertThat(result.predictedFields()).containsEntry("account_number", "ACC123456");
        assertThat(result.predictedFields()).containsEntry("amount", "1000.00");
    }
}
