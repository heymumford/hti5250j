/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.interfaces;

import java.util.Properties;

import org.hti5250j.Session5250;
import org.hti5250j.SessionPanel;
import org.hti5250j.framework.common.Sessions;

public interface SessionManagerInterface {

    /**
     * @return
     */
    Sessions getSessions();

    /**
     * @param sessionObject
     */
    void closeSession(SessionPanel sessionObject);

    /**
     * @param props
     * @param configurationResource
     * @param sessionName
     * @return
     */
    Session5250 openSession(Properties props, String configurationResource, String sessionName);

}
