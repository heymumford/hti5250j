/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.interfaces;


import org.hti5250j.event.SessionListener;

public interface SessionInterface {

    public abstract String getConfigurationResource();

    public abstract boolean isConnected();

    public abstract String getSessionName();

    public abstract int getSessionType();

    public abstract void connect();

    public abstract void disconnect();

    public abstract void addSessionListener(SessionListener listener);

    public abstract void removeSessionListener(SessionListener listener);

    /**
     * Popups a dialog to ask the user for entering a SysReq value.
     *
     * @return null if nothing to do, else a String containing the users input.
     */
    public abstract String showSystemRequest();

    /**
     * Signals the user a sound (or maybe a light flash).
     */
    public abstract void signalBell();

}
