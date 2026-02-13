/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import java.util.concurrent.TimeoutException;

/**
 * Immutable result of a single workflow execution.
 * Carries success/failure status, latency, and diagnostics.
 */
public record WorkflowResult(
    String rowKey,
    boolean success,
    long latencyMs,
    String artifactPath,
    Throwable error
) {
    /**
     * Create successful workflow result with latency and artifact path.
     */
    public static WorkflowResult success(String rowKey, long latencyMs, String artifactPath) {
        if (rowKey == null) {
            throw new IllegalArgumentException("rowKey cannot be null");
        }
        if (artifactPath == null) {
            throw new IllegalArgumentException("artifactPath cannot be null");
        }
        if (latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs cannot be negative");
        }
        return new WorkflowResult(rowKey, true, latencyMs, artifactPath, null);
    }

    /**
     * Create failed workflow result with exception.
     */
    public static WorkflowResult failure(String rowKey, long latencyMs, Throwable error) {
        if (rowKey == null) {
            throw new IllegalArgumentException("rowKey cannot be null");
        }
        if (error == null) {
            throw new IllegalArgumentException("error cannot be null");
        }
        if (latencyMs < 0) {
            throw new IllegalArgumentException("latencyMs cannot be negative");
        }
        return new WorkflowResult(rowKey, false, latencyMs, null, error);
    }

    /**
     * Create timeout result when workflow exceeds time limit.
     */
    public static WorkflowResult timeout(String message) {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        return new WorkflowResult(message, false, 0, null, new TimeoutException(message));
    }

    /**
     * User-readable summary of result.
     */
    public String summary() {
        if (success) {
            return String.format("✓ %s (%dms) → %s", rowKey, latencyMs, artifactPath);
        } else {
            String errorMsg = error != null ? error.getClass().getSimpleName() : "unknown";
            return String.format("✗ %s (%dms) — %s", rowKey, latencyMs, errorMsg);
        }
    }
}
