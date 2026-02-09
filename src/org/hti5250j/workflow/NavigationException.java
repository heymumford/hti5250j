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
     * Screen dumps larger than 80 lines are truncated to prevent memory overhead.
     * @param message Error message
     * @param screenDump Current screen content
     * @return NavigationException with context
     */
    public static NavigationException withScreenDump(String message, String screenDump) {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n\nScreen content:\n");

        if (screenDump == null) {
            sb.append("null");
        } else {
            // Truncate screen dump to max 80 lines (typical screen size)
            String[] lines = screenDump.split("\n", -1);
            int maxLines = Math.min(80, lines.length);

            for (int i = 0; i < maxLines; i++) {
                sb.append(lines[i]).append("\n");
            }

            // Add truncation indicator if content was cut off
            if (lines.length > maxLines) {
                sb.append("\n[... ").append(lines.length - maxLines)
                    .append(" additional lines truncated ...]\n");
            }
        }

        return new NavigationException(sb.toString());
    }
}
