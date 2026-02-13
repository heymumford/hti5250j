/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.interfaces.HeadlessSession;
import org.hti5250j.interfaces.HeadlessSessionFactory;
import org.hti5250j.interfaces.RequestHandler;

import java.util.Properties;

/**
 * Default factory for creating HeadlessSession instances.
 * <p>
 * Creates DefaultHeadlessSessionImpl wrapping Session5250.
 * Supports custom RequestHandler injection for Robot Framework integration.
 *
 * @since 0.12.0
 */
public class DefaultHeadlessSessionFactory implements HeadlessSessionFactory {

    private final RequestHandler requestHandler;

    /**
     * Create factory with default (null) request handler.
     */
    public DefaultHeadlessSessionFactory() {
        this(new NullRequestHandler());
    }

    /**
     * Create factory with custom request handler.
     *
     * @param requestHandler custom handler (e.g., for Robot Framework)
     */
    public DefaultHeadlessSessionFactory(RequestHandler requestHandler) {
        this.requestHandler = requestHandler != null ? requestHandler : new NullRequestHandler();
    }

    @Override
    public HeadlessSession createSession(String sessionName, String configResource, Properties connectionProps) {
        if (sessionName == null || sessionName.isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be null or empty");
        }
        if (connectionProps == null) {
            throw new IllegalArgumentException("Connection properties cannot be null");
        }

        SessionConfig config = new SessionConfig(configResource, sessionName);
        Session5250 session5250 = new Session5250(connectionProps, configResource, sessionName, config);
        return new DefaultHeadlessSession(session5250, requestHandler);
    }

}
