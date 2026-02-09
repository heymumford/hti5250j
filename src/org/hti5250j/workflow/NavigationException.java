/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Exception thrown when workflow navigation fails.
 * Indicates that the expected screen was not reached after navigation action.
 */
public class NavigationException extends Exception {

    public NavigationException(String message) {
        super(message);
    }

    public NavigationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create exception with screen dump context.
     * @param message Error message
     * @param screenDump Current screen content
     * @return NavigationException with context
     */
    public static NavigationException withScreenDump(String message, String screenDump) {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n\nScreen content:\n").append(screenDump);
        return new NavigationException(sb.toString());
    }
}
