/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

/**
 * Thrown when a session pool cannot provide a session because it is exhausted.
 *
 * @since 1.1.0
 */
public class PoolExhaustedException extends Exception {

    public PoolExhaustedException(String message) {
        super(message);
    }

    public PoolExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
