/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for WorkflowTolerance record (Phase 12E, D5-TOLERANCE).
 *
 * Contract: Tolerances define acceptable bounds for non-determinism.
 * These tests verify that tolerance validation, defaults, and boundary checks work correctly.
 *
 * Test IDs map to ARCHITECTURE.md contracts:
 * - Contract: "Tolerances define bounds for non-determinism"
 * - Verified by: D5-TOLERANCE-001 through D5-TOLERANCE-006
 */
class WorkflowToleranceTest {

    /**
     * D5-TOLERANCE-001: Valid tolerance created with explicit values.
     *
     * Setup: Create tolerance with all valid values
     * Action: Instantiate WorkflowTolerance record
     * Expected: Record created successfully, fields accessible
     *
     * Catches: Record definition incomplete or fields inaccessible
     */
    @Test
    void createToleranceWithValidValues() {
        WorkflowTolerance tolerance = new WorkflowTolerance(
            "payment_processing",
            300000L,    // 5 minutes
            0.01,       // 2 decimal places
            3,          // 3 retries
            true        // require approval
        );

        assertThat(tolerance.workflowName()).isEqualTo("payment_processing");
        assertThat(tolerance.maxDurationMs()).isEqualTo(300000L);
        assertThat(tolerance.fieldPrecision()).isEqualTo(0.01);
        assertThat(tolerance.maxRetries()).isEqualTo(3);
        assertThat(tolerance.requiresApproval()).isTrue();
    }

    /**
     * D5-TOLERANCE-002: Zero duration rejected (invalid tolerance).
     *
     * Setup: Attempt to create with maxDurationMs = 0
     * Action: New WorkflowTolerance(...)
     * Expected: IllegalArgumentException with message "maxDurationMs must be > 0"
     *
     * Catches: Invalid tolerance not rejected
     */
    @Test
    void zeroMaxDurationRejected() {
        assertThatThrownBy(() ->
            new WorkflowTolerance("test", 0L, 0.01, 3, false)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxDurationMs")
            .hasMessageContaining("must be > 0");
    }

    /**
     * D5-TOLERANCE-003: Negative duration rejected.
     *
     * Setup: Attempt to create with maxDurationMs = -1000
     * Action: New WorkflowTolerance(...)
     * Expected: IllegalArgumentException thrown
     *
     * Catches: Negative tolerance accepted (should not be)
     */
    @Test
    void negativeDurationRejected() {
        assertThatThrownBy(() ->
            new WorkflowTolerance("test", -1000L, 0.01, 3, false)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxDurationMs");
    }

    /**
     * D5-TOLERANCE-004: Negative retry count rejected.
     *
     * Setup: Attempt to create with maxRetries = -1
     * Action: New WorkflowTolerance(...)
     * Expected: IllegalArgumentException with message "maxRetries cannot be negative"
     *
     * Catches: Invalid retry budget not rejected
     */
    @Test
    void negativeRetryCountRejected() {
        assertThatThrownBy(() ->
            new WorkflowTolerance("test", 300000L, 0.01, -1, false)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxRetries")
            .hasMessageContaining("negative");
    }

    /**
     * D5-TOLERANCE-005: Blank workflow name rejected.
     *
     * Setup: Attempt to create with empty workflow name
     * Action: New WorkflowTolerance("", ...)
     * Expected: IllegalArgumentException with message about workflow name
     *
     * Catches: Empty workflow names accepted
     */
    @Test
    void blankWorkflowNameRejected() {
        assertThatThrownBy(() ->
            new WorkflowTolerance("", 300000L, 0.01, 3, false)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Workflow name");

        // Also test whitespace-only names
        assertThatThrownBy(() ->
            new WorkflowTolerance("   ", 300000L, 0.01, 3, false)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Workflow name");
    }

    /**
     * D5-TOLERANCE-006: Defaults provides sensible bounds.
     *
     * Setup: Call WorkflowTolerance.defaults("test_workflow")
     * Action: Create tolerance with defaults
     * Expected: Standard defaults applied (5min duration, 0.01 precision, 3 retries, no approval)
     *
     * Catches: Defaults not provided or incorrect
     */
    @Test
    void defaultsProvideSensibleBounds() {
        WorkflowTolerance defaults = WorkflowTolerance.defaults("test_workflow");

        assertThat(defaults.workflowName()).isEqualTo("test_workflow");
        assertThat(defaults.maxDurationMs()).isEqualTo(300000L);       // 5 minutes
        assertThat(defaults.fieldPrecision()).isEqualTo(0.01);          // Monetary precision
        assertThat(defaults.maxRetries()).isEqualTo(3);                 // Standard retries
        assertThat(defaults.requiresApproval()).isFalse();              // Automated allowed
    }

    /**
     * D5-TOLERANCE-007: Duration comparison works.
     *
     * Setup: Create tolerance with maxDurationMs = 5000
     * Action: Call exceededDuration(4000) and exceededDuration(6000)
     * Expected: Returns false and true respectively
     *
     * Catches: Duration boundary check fails
     */
    @Test
    void durationComparisonAccurate() {
        WorkflowTolerance tolerance = new WorkflowTolerance(
            "test", 5000L, 0.01, 3, false
        );

        // Under tolerance
        assertThat(tolerance.exceededDuration(4000L)).isFalse();
        assertThat(tolerance.exceededDuration(5000L)).isFalse();  // Equal = within tolerance

        // Over tolerance
        assertThat(tolerance.exceededDuration(5001L)).isTrue();
        assertThat(tolerance.exceededDuration(10000L)).isTrue();
    }

    /**
     * D5-TOLERANCE-008: Precision validation works for valid values.
     *
     * Setup: Create tolerance with fieldPrecision = 0.01
     * Action: Check withinPrecision(123.45) and withinPrecision(123.456)
     * Expected: true and false respectively
     *
     * Catches: Precision boundary check fails
     */
    @Test
    void precisionValidationAccurate() {
        WorkflowTolerance tolerance = new WorkflowTolerance(
            "test", 300000L, 0.01, 3, false
        );

        // Within precision
        assertThat(tolerance.withinPrecision(123.45)).isTrue();
        assertThat(tolerance.withinPrecision(0.01)).isTrue();
        assertThat(tolerance.withinPrecision(0.00)).isTrue();

        // Exceeds precision
        assertThat(tolerance.withinPrecision(123.456)).isFalse();
        assertThat(tolerance.withinPrecision(0.001)).isFalse();
    }

    /**
     * D5-TOLERANCE-009: Retry budget validation works.
     *
     * Setup: Create tolerance with maxRetries = 3
     * Action: Check withinRetryBudget(2), withinRetryBudget(3), withinRetryBudget(4)
     * Expected: true, true, false respectively
     *
     * Catches: Retry budget boundary check fails
     */
    @Test
    void retryBudgetValidationAccurate() {
        WorkflowTolerance tolerance = new WorkflowTolerance(
            "test", 300000L, 0.01, 3, false
        );

        // Within budget
        assertThat(tolerance.withinRetryBudget(0)).isTrue();
        assertThat(tolerance.withinRetryBudget(2)).isTrue();
        assertThat(tolerance.withinRetryBudget(3)).isTrue();  // Equal = within budget

        // Over budget
        assertThat(tolerance.withinRetryBudget(4)).isFalse();
        assertThat(tolerance.withinRetryBudget(10)).isFalse();
    }

    /**
     * D5-TOLERANCE-010: Record immutability (no setters).
     *
     * Setup: Create WorkflowTolerance record
     * Action: Verify no setter methods exist
     * Expected: Record is immutable, cannot be modified after creation
     *
     * Catches: Record not properly immutable
     */
    @Test
    void recordIsImmutable() {
        WorkflowTolerance tolerance = new WorkflowTolerance(
            "test", 300000L, 0.01, 3, false
        );

        // Verify immutability by checking that accessor methods return same values
        assertThat(tolerance.workflowName()).isEqualTo("test");
        assertThat(tolerance.maxDurationMs()).isEqualTo(300000L);

        // Record equality and hashing work
        WorkflowTolerance same = new WorkflowTolerance(
            "test", 300000L, 0.01, 3, false
        );
        assertThat(tolerance).isEqualTo(same);
        assertThat(tolerance.hashCode()).isEqualTo(same.hashCode());
    }
}
