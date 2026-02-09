/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Immutable navigate action with target screen and keystroke sequence.
 * Constructor validates non-null, non-empty fields.
 */
public final record NavigateAction(String screen, String keys) implements Action {
    public NavigateAction {
        if (screen == null || screen.isEmpty()) {
            throw new IllegalArgumentException("screen required");
        }
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("keys required");
        }
    }
}
