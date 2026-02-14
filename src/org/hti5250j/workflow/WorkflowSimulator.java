/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Offline workflow simulator for dry-run testing.
 *
 * Contract: Predicts workflow outcome without real i5 connection.
 * Purpose: Enable human approval gates before real execution.
 *
 * Features:
 * - Happy path execution (all steps succeed)
 * - Timeout prediction (cumulative duration exceeds tolerance)
 * - Validation error prediction (field boundary violations)
 * - Warning generation (truncation, precision loss)
 * - No network access, no i5 connection required
 *
 * Usage:
 * ```java
 * WorkflowSimulator sim = new WorkflowSimulator();
 * WorkflowSimulation result = sim.simulate(workflow, testData, tolerance);
 * if (result.predictSuccess()) {
 *     System.out.println("Approved for execution");
 * } else if (result.hasWarnings()) {
 *     System.out.println("Warnings detected: " + result.warnings());
 * }
 * ```
 */
public class WorkflowSimulator {
    private static final long STEP_DURATION_MS = 500;  // Estimate per step
    private static final long LOGIN_DURATION_MS = 2000;  // Extra for connection

    /**
     * Simulate workflow execution against mock screen (no i5 required).
     *
     * @param workflow WorkflowSchema to simulate
     * @param testData Data row for parameter substitution
     * @param tolerance WorkflowTolerance with bounds
     * @return WorkflowSimulation with predicted outcome and warnings
     */
    public WorkflowSimulation simulate(
        WorkflowSchema workflow,
        Map<String, String> testData,
        WorkflowTolerance tolerance
    ) {
        List<SimulatedStep> simulatedSteps = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        long cumulativeDurationMs = 0;
        String predictedOutcome = "success";

        if (workflow == null || workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            warnings.add("Workflow is empty or invalid");
            return new WorkflowSimulation(
                simulatedSteps,
                "validation_error",
                new HashMap<>(),
                warnings
            );
        }

        for (int i = 0; i < workflow.getSteps().size(); i++) {
            StepDef step = workflow.getSteps().get(i);
            String stepName = step.getAction().toString();
            String prediction = "success";
            Optional<String> stepWarning = Optional.empty();

            long stepDuration = "LOGIN".equals(stepName) ? LOGIN_DURATION_MS : STEP_DURATION_MS;
            cumulativeDurationMs += stepDuration;

            if (cumulativeDurationMs > tolerance.maxDurationMs()) {
                prediction = "timeout";
                predictedOutcome = "timeout";
                stepWarning = Optional.of(
                    String.format(
                        "Step %d would exceed timeout (cumulative: %dms > %dms)",
                        i,
                        cumulativeDurationMs,
                        tolerance.maxDurationMs()
                    )
                );
            }

            if ("FILL".equals(stepName) && step.getFields() != null) {
                for (Map.Entry<String, String> field : step.getFields().entrySet()) {
                    String fieldValue = field.getValue();

                    if (fieldValue != null && fieldValue.length() > 255) {
                        warnings.add(
                            String.format(
                                "Step %d FILL: field '%s' value too long (%d chars, max 255)",
                                i,
                                field.getKey(),
                                fieldValue.length()
                            )
                        );
                    }

                    try {
                        double numValue = Double.parseDouble(fieldValue);
                        if (numValue != Math.round(numValue * 100.0) / 100.0) {
                            warnings.add(
                                String.format(
                                    "Step %d FILL: field '%s' has precision loss (precision: %.3f)",
                                    i,
                                    field.getKey(),
                                    tolerance.fieldPrecision()
                                )
                            );
                        }
                    } catch (NumberFormatException e) {
                        // Non-numeric field value; precision check not applicable
                    }
                }
            }

            if ("ASSERT".equals(stepName)) {
                if ((step.getScreen() == null || step.getScreen().isEmpty())
                    && (step.getText() == null || step.getText().isEmpty())) {
                    prediction = "error";
                    predictedOutcome = "validation_error";
                    stepWarning = Optional.of("Step " + i + " ASSERT: must specify 'screen' or 'text'");
                }
            }

            SimulatedStep simStep = new SimulatedStep(i, stepName, prediction, stepWarning);
            simulatedSteps.add(simStep);

            if (stepWarning.isPresent()) {
                warnings.add(stepWarning.get());
            }

            if ("timeout".equals(prediction)) {
                break;
            }
        }

        Map<String, String> predictedFields = new HashMap<>();
        if (testData != null) {
            for (Map.Entry<String, String> entry : testData.entrySet()) {
                if (entry.getValue() != null) {
                    predictedFields.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return new WorkflowSimulation(
            simulatedSteps,
            predictedOutcome,
            predictedFields,
            warnings
        );
    }
}
