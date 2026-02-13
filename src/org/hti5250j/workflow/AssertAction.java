/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Immutable assert action with optional text and screen name.
 * Constructor validates that at least one of text or screen is provided.
 */
public record AssertAction(String text, String screen) implements Action {
    public AssertAction {
        if ((text == null || text.isEmpty()) && (screen == null || screen.isEmpty())) {
            throw new IllegalArgumentException("text or screen required");
        }
    }
}
