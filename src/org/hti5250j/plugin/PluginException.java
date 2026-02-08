/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.plugin;

/**
 * PluginException - Root exception for plugin lifecycle and management errors.
 */
public class PluginException extends Exception {
    public static final int ERROR_LOAD_FAILED = 1;
    public static final int ERROR_VERSION_INCOMPATIBLE = 2;
    public static final int ERROR_DEPENDENCY_MISSING = 3;
    public static final int ERROR_DEPENDENCY_CONFLICT = 4;
    public static final int ERROR_ACTIVATION_FAILED = 5;
    public static final int ERROR_DEACTIVATION_FAILED = 6;
    public static final int ERROR_MALICIOUS_CODE = 7;
    public static final int ERROR_FATAL_EXCEPTION = 8;
    public static final int ERROR_RECOVERY_FAILED = 9;

    private final int errorCode;

    public PluginException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PluginException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
