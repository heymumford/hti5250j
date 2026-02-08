/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Immutable validation warning (non-fatal issue).
 *
 * @param stepIndex Step index (-1 for workflow-level warnings)
 * @param fieldName Field that triggered warning
 * @param message Warning description
 */
public record ValidationWarning(
    int stepIndex,
    String fieldName,
    String message
) {}
