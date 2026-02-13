/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.interfaces;


import org.hti5250j.event.SessionListener;

public interface SessionInterface {

    String getConfigurationResource();

    boolean isConnected();

    String getSessionName();

    int getSessionType();

    void connect();

    void disconnect();

    void addSessionListener(SessionListener listener);

    void removeSessionListener(SessionListener listener);

    /**
     * Popups a dialog to ask the user for entering a SysReq value.
     *
     * @return null if nothing to do, else a String containing the users input.
     */
    String showSystemRequest();

    /**
     * Signals the user a sound (or maybe a light flash).
     */
    void signalBell();

}
