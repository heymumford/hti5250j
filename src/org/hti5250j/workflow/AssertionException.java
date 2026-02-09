/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Exception thrown when workflow assertion fails.
 * Indicates that screen content did not match expected values.
 */
public class AssertionException extends Exception {

    private final String screenDump;

    public AssertionException(String message, String screenDump) {
        super(message);
        this.screenDump = screenDump;
    }

    /**
     * @return Screen content at time of assertion failure
     */
    public String getScreenDump() {
        return screenDump;
    }

    /**
     * Create exception with formatted screen dump.
     * @param message Error message
     * @param screenDump Current screen content
     * @return AssertionException with context
     */
    public static AssertionException withScreenDump(String message, String screenDump) {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n\nScreen content:\n").append(screenDump);
        return new AssertionException(sb.toString(), screenDump);
    }
}
