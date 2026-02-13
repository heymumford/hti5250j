/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import java.util.Map;

/**
 * Immutable fill action with form fields and optional timeout.
 * Constructor validates non-null fields map.
 */
public record FillAction(Map<String, String> fields, Integer timeout) implements Action {
    public FillAction {
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("fields required");
        }
    }
}
