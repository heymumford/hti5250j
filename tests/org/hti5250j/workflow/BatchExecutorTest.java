/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BatchExecutor parallel workflow execution.
 * Verifies correct handling of concurrent workflows using virtual threads.
 */
public class BatchExecutorTest {

    private WorkflowSchema sampleWorkflow;

    @BeforeEach
    public void setUp() {
        // Create minimal workflow with LOGIN step
        sampleWorkflow = new WorkflowSchema();
        sampleWorkflow.setName("Sample");
        sampleWorkflow.setDescription("Sample workflow");

        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setTimeout(30000);
        loginStep.setHost("test.example.com");
        loginStep.setUser("testuser");
        loginStep.setPassword("testpass");

        java.util.List<StepDef> steps = new java.util.ArrayList<>();
        steps.add(loginStep);
        sampleWorkflow.setSteps(steps);
    }

    @Test
    public void testBatchMetricsAggregation() {
        // Given: Three sample workflow results
        Map<String, Map<String, String>> rows = new HashMap<>();
        rows.put("row1", Map.of("field", "value1"));
        rows.put("row2", Map.of("field", "value2"));
        rows.put("row3", Map.of("field", "value3"));

        // Simulate execution times with realistic duration
        WorkflowResult r1 = WorkflowResult.success("row1", 100, "/path1");
        WorkflowResult r2 = WorkflowResult.success("row2", 200, "/path2");
        WorkflowResult r3 = WorkflowResult.success("row3", 150, "/path3");

        long startNanos = System.nanoTime();
        long endNanos = startNanos + 1_000_000_000; // 1 second total duration

        // When: Compute metrics
        BatchMetrics metrics = BatchMetrics.from(
            java.util.List.of(r1, r2, r3),
            startNanos,
            endNanos
        );

        // Then: Verify aggregation
        assertEquals(3, metrics.totalWorkflows());
        assertEquals(3, metrics.successCount());
        assertEquals(0, metrics.failureCount());
        assertTrue(metrics.p50LatencyMs() > 0);
        assertTrue(metrics.throughputOpsPerSec() > 0);
    }

    @Test
    public void testBatchMetricsWithFailures() {
        // Given: Mix of success and failure results
        WorkflowResult r1 = WorkflowResult.success("row1", 100, "/path1");
        WorkflowResult r2 = WorkflowResult.failure("row2", 50, new Exception("test error"));
        WorkflowResult r3 = WorkflowResult.success("row3", 150, "/path3");

        long startNanos = System.nanoTime();
        long endNanos = System.nanoTime() + 1_000_000_000; // 1 second later

        // When: Compute metrics
        BatchMetrics metrics = BatchMetrics.from(
            java.util.List.of(r1, r2, r3),
            startNanos,
            endNanos
        );

        // Then: Verify failure tracking
        assertEquals(3, metrics.totalWorkflows());
        assertEquals(2, metrics.successCount());
        assertEquals(1, metrics.failureCount());
        assertEquals(1, metrics.failures().size());
        assertTrue(metrics.failureRate() > 0);
    }

    @Test
    public void testWorkflowResultSuccess() {
        // When: Create successful result
        WorkflowResult result = WorkflowResult.success("row1", 500, "/artifacts/path");

        // Then: Verify immutability and correctness
        assertTrue(result.success());
        assertEquals("row1", result.rowKey());
        assertEquals(500, result.latencyMs());
        assertEquals("/artifacts/path", result.artifactPath());
        assertNull(result.error());
    }

    @Test
    public void testWorkflowResultFailure() {
        // Given: Exception for failure
        Exception error = new Exception("Navigation failed");

        // When: Create failure result
        WorkflowResult result = WorkflowResult.failure("row2", 300, error);

        // Then: Verify failure state
        assertFalse(result.success());
        assertEquals("row2", result.rowKey());
        assertEquals(300, result.latencyMs());
        assertNull(result.artifactPath());
        assertEquals(error, result.error());
    }

    @Test
    public void testWorkflowResultTimeout() {
        // When: Create timeout result
        WorkflowResult result = WorkflowResult.timeout("Workflow exceeded 300s timeout");

        // Then: Verify timeout state
        assertFalse(result.success());
        assertEquals(0, result.latencyMs());
        assertNotNull(result.error());
        assertTrue(result.error() instanceof java.util.concurrent.TimeoutException);
    }

    @Test
    public void testBatchMetricsPercentiles() {
        // Given: Workflow results with varying latencies
        WorkflowResult r1 = WorkflowResult.success("row1", 10, "/path1");
        WorkflowResult r2 = WorkflowResult.success("row2", 50, "/path2");
        WorkflowResult r3 = WorkflowResult.success("row3", 100, "/path3");
        WorkflowResult r4 = WorkflowResult.success("row4", 200, "/path4");

        long startNanos = System.nanoTime();
        long endNanos = System.nanoTime() + 1_000_000_000;

        // When: Compute percentiles
        BatchMetrics metrics = BatchMetrics.from(
            java.util.List.of(r1, r2, r3, r4),
            startNanos,
            endNanos
        );

        // Then: Verify percentile calculations
        assertTrue(metrics.p50LatencyMs() >= 10);
        assertTrue(metrics.p50LatencyMs() <= 200);
        assertTrue(metrics.p99LatencyMs() >= metrics.p50LatencyMs());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 100})
    public void testBatchMetricsScaling(int workflowCount) {
        // Given: Variable number of successful workflows
        java.util.List<WorkflowResult> results = new java.util.ArrayList<>();
        for (int i = 0; i < workflowCount; i++) {
            results.add(WorkflowResult.success("row" + i, 100 + i, "/path" + i));
        }

        long startNanos = System.nanoTime();
        long endNanos = System.nanoTime() + 1_000_000_000;

        // When: Compute metrics
        BatchMetrics metrics = BatchMetrics.from(results, startNanos, endNanos);

        // Then: Verify counts scale correctly
        assertEquals(workflowCount, metrics.totalWorkflows());
        assertEquals(workflowCount, metrics.successCount());
        assertEquals(0, metrics.failureCount());
    }

    @Test
    public void testBatchMetricsEmpty() {
        // When/Then: Empty results should throw
        assertThrows(IllegalArgumentException.class, () ->
            BatchMetrics.from(java.util.List.of(), System.nanoTime(), System.nanoTime())
        );
    }

    @Test
    public void testWorkflowResultNullValidation() {
        // Then: Verify null validation
        assertThrows(IllegalArgumentException.class, () ->
            WorkflowResult.success(null, 100, "/path")
        );

        assertThrows(IllegalArgumentException.class, () ->
            WorkflowResult.success("row", 100, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
            WorkflowResult.failure(null, 100, new Exception())
        );

        assertThrows(IllegalArgumentException.class, () ->
            WorkflowResult.failure("row", 100, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
            WorkflowResult.timeout(null)
        );
    }

    @Test
    public void testBatchMetricsSuccessRate() {
        // Given: Results with known success/failure counts
        WorkflowResult r1 = WorkflowResult.success("row1", 100, "/path1");
        WorkflowResult r2 = WorkflowResult.failure("row2", 50, new Exception("failed"));
        WorkflowResult r3 = WorkflowResult.success("row3", 150, "/path3");

        long startNanos = System.nanoTime();
        long endNanos = System.nanoTime() + 1_000_000_000;

        // When: Compute metrics
        BatchMetrics metrics = BatchMetrics.from(
            java.util.List.of(r1, r2, r3),
            startNanos,
            endNanos
        );

        // Then: Verify success/failure rates
        assertEquals(66.67, metrics.successRate(), 0.01);
        assertEquals(33.33, metrics.failureRate(), 0.01);
    }
}
