/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2018
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: SaschaS93
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools.logging;

/**
 *
 * Just a simple HTI5250jLogger implementation to test customLogger functionality
 *
 * @author SaschaS93
 */
public class CustomHTI5250jLoggerMock implements HTI5250jLogger {

    /*
     * Package level access only
     */
    public CustomHTI5250jLoggerMock() {

    }

    @Override
    public void initialize(String clazz) {

    }

    @Override
    public void debug(Object message) {

    }

    @Override
    public void debug(Object message, Throwable throwable) {

    }

    @Override
    public void info(Object message) {

    }

    @Override
    public void info(Object message, Throwable throwable) {

    }

    @Override
    public void warn(Object message) {

    }

    @Override
    public void warn(Object message, Throwable throwable) {

    }

    @Override
    public void error(Object message) {

    }

    @Override
    public void error(Object message, Throwable throwable) {

    }

    @Override
    public void fatal(Object message) {

    }

    @Override
    public void fatal(Object message, Throwable throwable) {

    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public boolean isFatalEnabled() {
        return false;
    }

    @Override
    public void setLevel(int newLevel) {

    }

    @Override
    public int getLevel() {
        return 0;
    }
}
