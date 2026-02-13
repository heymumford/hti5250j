/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Immutable submit action with AID key (Attention Identifier).
 * Constructor validates non-null, non-empty key.
 */
public record SubmitAction(String key) implements Action {
    public SubmitAction {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key required");
        }
    }
}
