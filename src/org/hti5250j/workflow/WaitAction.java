/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Immutable wait action with timeout in milliseconds.
 * Constructor validates positive timeout.
 */
public record WaitAction(Integer timeout) implements Action {
    public WaitAction {
        if (timeout == null || timeout <= 0) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
