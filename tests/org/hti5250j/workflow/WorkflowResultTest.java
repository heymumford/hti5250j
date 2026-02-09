/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkflowResult Tests")
class WorkflowResultTest {

    @Test
    @DisplayName("success() should create successful result")
    void testSuccessResult() {
        WorkflowResult result = WorkflowResult.success("row1", 5000, "/artifacts/run1");

        assertTrue(result.success());
        assertEquals("row1", result.rowKey());
        assertEquals(5000, result.latencyMs());
        assertEquals("/artifacts/run1", result.artifactPath());
        assertNull(result.error());
    }

    @Test
    @DisplayName("failure() should create failed result")
    void testFailureResult() {
        Exception error = new RuntimeException("Connection failed");
        WorkflowResult result = WorkflowResult.failure("row2", 2000, error);

        assertFalse(result.success());
        assertEquals("row2", result.rowKey());
        assertEquals(2000, result.latencyMs());
        assertNull(result.artifactPath());
        assertEquals(error, result.error());
    }

    @Test
    @DisplayName("timeout() should create timeout result")
    void testTimeoutResult() {
        WorkflowResult result = WorkflowResult.timeout("Workflow exceeded 30s limit");

        assertFalse(result.success());
        assertEquals("Workflow exceeded 30s limit", result.rowKey());
        assertNull(result.artifactPath());
        assertTrue(result.error() instanceof TimeoutException);
    }

    @Test
    @DisplayName("success() should throw when rowKey is null")
    void testSuccessNullRowKey() {
        assertThrows(IllegalArgumentException.class,
            () -> WorkflowResult.success(null, 1000, "/path"));
    }

    @Test
    @DisplayName("success() should throw when artifactPath is null")
    void testSuccessNullArtifactPath() {
        assertThrows(IllegalArgumentException.class,
            () -> WorkflowResult.success("row1", 1000, null));
    }

    @Test
    @DisplayName("success() should throw when latencyMs is negative")
    void testSuccessNegativeLatency() {
        assertThrows(IllegalArgumentException.class,
            () -> WorkflowResult.success("row1", -1, "/path"));
    }

    @Test
    @DisplayName("success() should accept zero latency")
    void testSuccessZeroLatency() {
        WorkflowResult result = WorkflowResult.success("row1", 0, "/path");

        assertEquals(0, result.latencyMs());
        assertTrue(result.success());
    }

    @Test
    @DisplayName("failure() should throw when rowKey is null")
    void testFailureNullRowKey() {
        assertThrows(IllegalArgumentException.class,
            () -> WorkflowResult.failure(null, 1000, new RuntimeException()));
    }

    @Test
    @DisplayName("failure() should throw when error is null")
    void testFailureNullError() {
        assertThrows(IllegalArgumentException.class,
            () -> WorkflowResult.failure("row1", 1000, null));
    }

    @Test
    @DisplayName("failure() should throw when latencyMs is negative")
    void testFailureNegativeLatency() {
        assertThrows(IllegalArgumentException.class,
            () -> WorkflowResult.failure("row1", -1, new RuntimeException()));
    }

    @Test
    @DisplayName("timeout() should throw when message is null")
    void testTimeoutNullMessage() {
        assertThrows(IllegalArgumentException.class,
            () -> WorkflowResult.timeout(null));
    }

    @Test
    @DisplayName("summary() should format successful result")
    void testSummarySucess() {
        WorkflowResult result = WorkflowResult.success("row1", 5000, "/artifacts/run1");
        String summary = result.summary();

        assertTrue(summary.contains("✓"));
        assertTrue(summary.contains("row1"));
        assertTrue(summary.contains("5000ms"));
        assertTrue(summary.contains("/artifacts/run1"));
    }

    @Test
    @DisplayName("summary() should format failed result")
    void testSummaryFailure() {
        Exception error = new IllegalArgumentException("Invalid field");
        WorkflowResult result = WorkflowResult.failure("row2", 1500, error);
        String summary = result.summary();

        assertTrue(summary.contains("✗"));
        assertTrue(summary.contains("row2"));
        assertTrue(summary.contains("1500ms"));
        assertTrue(summary.contains("IllegalArgumentException"));
    }

    @Test
    @DisplayName("summary() should format timeout result")
    void testSummaryTimeout() {
        WorkflowResult result = WorkflowResult.timeout("30 second timeout");
        String summary = result.summary();

        assertTrue(summary.contains("✗"));
        assertTrue(summary.contains("30 second timeout"));
        assertTrue(summary.contains("TimeoutException"));
    }

    @Test
    @DisplayName("Record should be immutable")
    void testImmutability() {
        WorkflowResult result = WorkflowResult.success("row1", 5000, "/path");

        assertEquals("row1", result.rowKey());
        assertEquals(5000, result.latencyMs());
        // Fields are final in records, no setters exist
    }

    @Test
    @DisplayName("equals() should compare records by value")
    void testEquality() {
        WorkflowResult result1 = WorkflowResult.success("row1", 5000, "/path");
        WorkflowResult result2 = WorkflowResult.success("row1", 5000, "/path");
        WorkflowResult result3 = WorkflowResult.success("row1", 6000, "/path");

        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
    }

    @Test
    @DisplayName("hashCode() should be consistent")
    void testHashCode() {
        WorkflowResult result1 = WorkflowResult.success("row1", 5000, "/path");
        WorkflowResult result2 = WorkflowResult.success("row1", 5000, "/path");

        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    @DisplayName("success() should accept large latency values")
    void testSuccessLargeLatency() {
        WorkflowResult result = WorkflowResult.success("row1", Long.MAX_VALUE, "/path");

        assertEquals(Long.MAX_VALUE, result.latencyMs());
    }

    @Test
    @DisplayName("Record should preserve error exception details")
    void testErrorDetails() {
        Exception originalError = new IllegalArgumentException("Field 'amount' is required");
        WorkflowResult result = WorkflowResult.failure("row1", 1000, originalError);

        assertEquals(originalError, result.error());
        assertEquals("Field 'amount' is required", result.error().getMessage());
    }
}
