/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools.logging;

/**
 * An implementation of the HTI5250jLogger to provide logger instances to the
 * console - System.out or System.err.
 */
public final class ConsoleLogger implements HTI5250jLogger {

    private int logLevel = HTI5250jLogger.WARN;

    private String clazz = null;

    /*
     * Package level access only
     */
    ConsoleLogger() {

    }

    public void initialize(final String clazz) {
        this.clazz = clazz;
    }

    public void debug(Object message) {
        if (isDebugEnabled())
            System.out.println("DEBUG [" + clazz + "] " + ((message != null) ? message.toString() : ""));
    }

    public void debug(Object message, Throwable throwable) {
        if (isDebugEnabled())
            System.out.println("DEBUG [" + clazz + "] "
                    + ((message != null) ? message.toString() : "")
                    + ((throwable != null) ? throwable.getMessage() : ""));
    }

    public void info(Object message) {
        if (isInfoEnabled())
            System.out.println("INFO [" + clazz + "] " + ((message != null) ? message.toString() : ""));
    }

    public void info(Object message, Throwable throwable) {
        if (isInfoEnabled())
            System.out.println("INFO [" + clazz + "] "
                    + ((message != null) ? message.toString() : "")
                    + ((throwable != null) ? throwable.getMessage() : ""));
    }

    public void warn(Object message) {
        if (isWarnEnabled())
            System.err.println("WARN [" + clazz + "] " + ((message != null) ? message.toString() : ""));
    }

    public void warn(Object message, Throwable throwable) {
        if (isWarnEnabled())
            System.err.println("WARN [" + clazz + "] "
                    + ((message != null) ? message.toString() : "")
                    + ((throwable != null) ? throwable.getMessage() : ""));
    }

    public void error(Object message) {
        if (isErrorEnabled())
            System.err.println("ERROR [" + clazz + "] " + ((message != null) ? message.toString() : ""));
    }

    public void error(Object message, Throwable throwable) {
        if (isErrorEnabled())
            System.err.println("ERROR [" + clazz + "] "
                    + ((message != null) ? message.toString() : "")
                    + ((throwable != null) ? throwable.getMessage() : ""));
    }

    public void fatal(Object message) {
        if (isFatalEnabled())
            System.err.println("FATAL [" + clazz + "] " + ((message != null) ? message.toString() : ""));
    }

    public void fatal(Object message, Throwable throwable) {
        if (isFatalEnabled())
            System.err.println("FATAL [" + clazz + "] "
                    + ((message != null) ? message.toString() : "")
                    + ((throwable != null) ? throwable.getMessage() : ""));
    }

    public boolean isDebugEnabled() {
        return (logLevel <= DEBUG); // 1
    }

    public boolean isInfoEnabled() {
        return (logLevel <= INFO);  // 2
    }

    public boolean isWarnEnabled() {
        return (logLevel <= WARN);  // 4
    }

    public boolean isErrorEnabled() {
        return (logLevel <= ERROR); // 8
    }

    public boolean isFatalEnabled() {
        return (logLevel <= FATAL); // 16
    }

    public int getLevel() {
        return logLevel;
    }

    public void setLevel(int newLevel) {
        logLevel = newLevel;
    }

}
