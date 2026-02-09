/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Immutable login action with host and credentials.
 * Constructor validates non-null, non-empty fields.
 */
public final record LoginAction(String host, String user, String password) implements Action {
    public LoginAction {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host required");
        }
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("user required");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password required");
        }
    }
}
