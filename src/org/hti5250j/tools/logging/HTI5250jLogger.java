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
    int DEBUG = 1;    // most verbose
    int INFO = 2;
    int WARN = 4;  // medium verbose, should be choosen for deployment
    int ERROR = 8;
    int FATAL = 16;
    int OFF = 32;  // most silence

    /**
     * @param clazz
     */
    void initialize(String clazz);

    /**
     * @param message
     */
    void debug(Object message);

    /**
     * @param message
     * @param throwable
     */
    void debug(Object message, Throwable throwable);

    void info(Object message);

    /**
     * @param message
     * @param throwable
     */
    void info(Object message, Throwable throwable);

    /**
     * @param message
     */
    void warn(Object message);

    /**
     * @param message
     * @param throwable
     */
    void warn(Object message, Throwable throwable);

    /**
     * @param message
     */
    void error(Object message);

    /**
     * @param message
     * @param throwable
     */
    void error(Object message, Throwable throwable);

    /**
     * @param message
     */
    void fatal(Object message);

    /**
     * @param message
     * @param throwable
     */
    void fatal(Object message, Throwable throwable);

    /**
     * @return
     */
    boolean isDebugEnabled();

    /**
     * @return
     */
    boolean isInfoEnabled();

    /**
     * @return
     */
    boolean isWarnEnabled();

    /**
     * @return
     */
    boolean isErrorEnabled();

    /**
     * @return
     */
    boolean isFatalEnabled();

    /**
     * Sets a new log level.
     *
     * @param newLevel
     * @throws IllegalArgumentException If the new level is not allowed
     */
    void setLevel(int newLevel);

    /**
     * @return The current log level.
     */
    int getLevel();

}
