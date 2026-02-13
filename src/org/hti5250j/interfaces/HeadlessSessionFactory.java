/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.interfaces;


import java.util.Properties;

/**
 * Factory interface for creating HeadlessSession instances.
 * <p>
 * Enables polymorphic session creation for testing, mocking, and custom
 * session implementations (e.g., remote sessions, session pools).
 * <p>
 * Implementations:
 * - DefaultHeadlessSessionFactory: Creates DefaultHeadlessSession wrapping Session5250
 * - MockHeadlessSessionFactory: Creates mock sessions for testing
 * - RemoteHeadlessSessionFactory: Creates sessions backed by REST API
 *
 */
public interface HeadlessSessionFactory {

    /**
     * Create a new headless session.
     *
     * @param sessionName unique session identifier (e.g., "production", "uat")
     * @param configResource configuration file name (e.g., "session.props")
     * @param connectionProps connection parameters (host, port, user, etc.)
     * @return new HeadlessSession instance
     * @throws IllegalArgumentException if parameters invalid
     */
    HeadlessSession createSession(
        String sessionName,
        String configResource,
        Properties connectionProps
    );

    /**
     * Create session with default configuration.
     *
     * @param sessionName unique session identifier
     * @param connectionProps connection parameters
     * @return new HeadlessSession instance
     */
    default HeadlessSession createSession(String sessionName, Properties connectionProps) {
        return createSession(sessionName, "TN5250JDefaults.props", connectionProps);
    }

}
