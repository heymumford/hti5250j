/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * An implementation of the HTI5250jLogger to provide log4j logger instances.
 */
public final class Log4jLogger implements HTI5250jLogger {

    private Logger log = null;

    /*
     * Package level access only
     */
    Log4jLogger() {

    }

    public void initialize(final String clazz) {
        log = Logger.getLogger(clazz);
    }

    public void debug(Object message) {
        log.debug(message);
    }

    public void debug(Object message, Throwable throwable) {
        log.debug(message, throwable);
    }

    public void info(Object message) {
        log.info(message);
    }

    public void info(Object message, Throwable throwable) {
        log.info(message, throwable);
    }

    public void warn(Object message) {
        log.warn(message);
    }

    public void warn(Object message, Throwable throwable) {
        log.warn(message, throwable);
    }

    public void error(Object message) {
        log.error(message);
    }

    public void error(Object message, Throwable throwable) {
        log.error(message, throwable);
    }

    public void fatal(Object message) {
        log.fatal(message);
    }

    public void fatal(Object message, Throwable throwable) {
        log.fatal(message, throwable);
    }

    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return (Level.WARN.equals(log.getLevel()));
    }

    public boolean isFatalEnabled() {
        return (Level.FATAL.equals(log.getLevel()));
    }

    public boolean isErrorEnabled() {
        return (Level.ERROR.equals(log.getLevel()));
    }

    public void setLevel(int newLevel) {
        // Configure Log4j level based on internal logging constants
        // Map HTI5250j log levels (OFF, DEBUG, INFO, WARN, ERROR, FATAL) to Log4j Level objects
        switch (newLevel) {
            case OFF -> log.setLevel(Level.OFF);
            case DEBUG -> log.setLevel(Level.DEBUG);
            case INFO -> log.setLevel(Level.INFO);
            case WARN -> log.setLevel(Level.WARN);
            case ERROR -> log.setLevel(Level.ERROR);
            case FATAL -> log.setLevel(Level.FATAL);
            default -> { }
        }
    }

    public int getLevel() {

        switch (log.getLevel().toInt()) {

            case (org.apache.log4j.Level.DEBUG_INT):
                return DEBUG;

            case (org.apache.log4j.Level.INFO_INT):
                return INFO;

            case (org.apache.log4j.Level.WARN_INT):
                return WARN;

            case (org.apache.log4j.Level.ERROR_INT):
                return ERROR;

            case (org.apache.log4j.Level.FATAL_INT):
                return FATAL;
            default:
                return WARN;
        }

    }
}
