/*
 * SPDX-FileCopyrightText: Copyright (c) 2001 - 2004
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.SessionPanel;
import org.hti5250j.HTI5250jConstants;
import org.hti5250j.interfaces.SessionManagerInterface;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;


/**
 * The SessionManager is the central repository for access to all sessions.
 * The SessionManager contains a list of all Session objects available.
 */
public class SessionManager implements SessionManagerInterface {

    static private Sessions sessions;
    static private List<SessionConfig> configs;

    private HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());
    /**
     * A handle to the unique SessionManager class
     */
    static private SessionManager _instance;

    /**
     * The constructor is made protected to allow overriding.
     */
    protected SessionManager() {
        if (_instance == null) {
            // initialize the settings information
            initialize();
            // set our instance to this one.
            _instance = this;
        }
    }

    /**
     * @return The unique instance of this class.
     */
    static public SessionManager instance() {

        if (_instance == null) {
            _instance = new SessionManager();
        }
        return _instance;

    }

    private void initialize() {
        log.info("New session Manager initialized");
        sessions = new Sessions();
        configs = new ArrayList<SessionConfig>();

    }

    @Override
    public Sessions getSessions() {
        return sessions;
    }

    @Override
    public void closeSession(SessionPanel sesspanel) {

        sesspanel.closeDown();
        sessions.removeSession((sesspanel).getSession());

    }

    @Override
    public synchronized Session5250 openSession(Properties sesProps, String configurationResource
            , String sessionName) {

        if (sessionName == null)
            sesProps.put(HTI5250jConstants.SESSION_TERM_NAME, sesProps.getProperty(HTI5250jConstants.SESSION_HOST));
        else
            sesProps.put(HTI5250jConstants.SESSION_TERM_NAME, sessionName);

        if (configurationResource == null) configurationResource = "";

        sesProps.put(HTI5250jConstants.SESSION_CONFIG_RESOURCE, configurationResource);

        SessionConfig useConfig = null;
        for (SessionConfig conf : configs) {
            if (conf.getSessionName().equals(sessionName)) {
                useConfig = conf;
            }
        }

        if (useConfig == null) {

            useConfig = new SessionConfig(configurationResource, sessionName);
            configs.add(useConfig);
        }

        Session5250 newSession = new Session5250(sesProps, configurationResource,
                sessionName, useConfig);
        sessions.addSession(newSession);
        return newSession;

    }

}
