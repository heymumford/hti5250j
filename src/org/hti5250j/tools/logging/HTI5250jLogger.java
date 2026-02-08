/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools.logging;

/**
 * An interface defining generic loggers.
 */
public interface HTI5250jLogger {

    // debug levels - The levels work from lower to higher. The lower levels
    // will be activated by turning on a higher level
    public static final int DEBUG = 1;    // most verbose
    public static final int INFO = 2;
    public static final int WARN = 4;  // medium verbose, should be choosen for deployment
    public static final int ERROR = 8;
    public static final int FATAL = 16;
    public static final int OFF = 32;  // most silence

    /**
     * @param clazz
     */
    abstract public void initialize(final String clazz);

    /**
     * @param message
     */
    abstract public void debug(Object message);

    /**
     * @param message
     * @param throwable
     */
    abstract public void debug(Object message, Throwable throwable);

    abstract public void info(Object message);

    /**
     * @param message
     * @param throwable
     */
    abstract public void info(Object message, Throwable throwable);

    /**
     * @param message
     */
    abstract public void warn(Object message);

    /**
     * @param message
     * @param throwable
     */
    abstract public void warn(Object message, Throwable throwable);

    /**
     * @param message
     */
    abstract public void error(Object message);

    /**
     * @param message
     * @param throwable
     */
    abstract public void error(Object message, Throwable throwable);

    /**
     * @param message
     */
    abstract public void fatal(Object message);

    /**
     * @param message
     * @param throwable
     */
    abstract public void fatal(Object message, Throwable throwable);

    /**
     * @return
     */
    abstract public boolean isDebugEnabled();

    /**
     * @return
     */
    abstract public boolean isInfoEnabled();

    /**
     * @return
     */
    abstract public boolean isWarnEnabled();

    /**
     * @return
     */
    abstract public boolean isErrorEnabled();

    /**
     * @return
     */
    abstract public boolean isFatalEnabled();

    /**
     * Sets a new log level.
     *
     * @param newLevel
     * @throws IllegalArgumentException If the new level is not allowed
     */
    abstract public void setLevel(int newLevel);

    /**
     * @return The current log level.
     */
    abstract public int getLevel();

}
