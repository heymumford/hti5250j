/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Immutable validation error with suggested fix.
 *
 * @param stepIndex Step index (-1 for workflow-level errors)
 * @param fieldName Field that failed validation
 * @param message Error description
 * @param suggestedFix Guidance on how to fix
 */
public record ValidationError(
    int stepIndex,
    String fieldName,
    String message,
    String suggestedFix
) {}
