/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Specifies acceptable bounds for workflow execution.
 *
 * Implements Martin Fowler principle: "Define acceptable bounds for non-determinism."
 * These tolerances answer: "Is execution reliable? How do we know?"
 *
 * Example YAML:
 * <pre>
 * tolerances:
 *   maxDurationMs: 300000      # 5 minutes per workflow
 *   fieldPrecision: 0.01       # Monetary precision (cents)
 *   maxRetries: 3              # Retry budget
 *   requiresApproval: true     # Human approval before executing on i5
 * </pre>
 */
public record WorkflowTolerance(
    String workflowName,
    long maxDurationMs,
    double fieldPrecision,
    int maxRetries,
    boolean requiresApproval
) {
    /**
     * Constructor with validation.
     *
     * @param workflowName workflow identifier
     * @param maxDurationMs maximum duration in milliseconds (must be > 0)
     * @param fieldPrecision minimum precision for decimal fields (e.g., 0.01 for cents)
     * @param maxRetries maximum retry count (must be >= 0)
     * @param requiresApproval true if human approval required before execution
     *
     * @throws IllegalArgumentException if any validation fails
     */
    public WorkflowTolerance {
        if (workflowName == null) {
            throw new IllegalArgumentException("Workflow name cannot be null");
        }
        if (workflowName.isBlank()) {
            throw new IllegalArgumentException("Workflow name cannot be blank");
        }
        if (maxDurationMs <= 0) {
            throw new IllegalArgumentException("maxDurationMs must be > 0, got: " + maxDurationMs);
        }
        if (fieldPrecision <= 0) {
            throw new IllegalArgumentException("fieldPrecision must be > 0, got: " + fieldPrecision);
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries cannot be negative, got: " + maxRetries);
        }
    }

    /**
     * Create tolerance with defaults for unspecified values.
     *
     * Defaults:
     * - maxDurationMs: 300000 (5 minutes)
     * - fieldPrecision: 0.01 (two decimal places)
     * - maxRetries: 3
     * - requiresApproval: false (automated execution allowed)
     *
     * @param workflowName workflow identifier
     * @return tolerance with sensible defaults
     */
    public static WorkflowTolerance defaults(String workflowName) {
        return new WorkflowTolerance(
            workflowName,
            300000L,    // 5 minutes
            0.01,       // 2 decimal places (monetary)
            3,          // 3 retries
            false       // no human approval required
        );
    }

    /**
     * Check if workflow exceeded this tolerance.
     *
     * @param actualDurationMs actual execution duration
     * @return true if duration exceeded tolerance
     */
    public boolean exceededDuration(long actualDurationMs) {
        return actualDurationMs > maxDurationMs;
    }

    /**
     * Check if field value precision matches tolerance.
     *
     * Example: 123.456 with precision 0.01 is invalid (3 decimals > 2 allowed)
     *
     * @param fieldValue numeric field value
     * @return true if precision is within tolerance
     */
    public boolean withinPrecision(double fieldValue) {
        // Round to precision, check if equal to original
        double rounded = Math.round(fieldValue / fieldPrecision) * fieldPrecision;
        return Math.abs(fieldValue - rounded) < 1e-9;
    }

    /**
     * Check if retry count within tolerance.
     *
     * @param retryCount number of retries used
     * @return true if retry count <= maxRetries
     */
    public boolean withinRetryBudget(int retryCount) {
        return retryCount <= maxRetries;
    }
}
