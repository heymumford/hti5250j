/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.interfaces.SessionInterface;
import java.util.Properties;

/**
 * Factory for creating and configuring Session5250 instances.
 * Encapsulates session creation logic and properties management.
 */
public class SessionFactory {

    /**
     * Create Session5250 from LOGIN step properties.
     *
     * @param loginStep the LOGIN step containing host/user/password
     * @return configured SessionInterface
     * @throws Exception if session creation fails
     */
    public static SessionInterface createFromLoginStep(StepDef loginStep) throws Exception {
        if (loginStep == null) {
            throw new IllegalArgumentException("LOGIN step cannot be null");
        }

        if (loginStep.getHost() == null) {
            throw new IllegalArgumentException("LOGIN step requires 'host' property");
        }
        if (loginStep.getUser() == null) {
            throw new IllegalArgumentException("LOGIN step requires 'user' property");
        }
        if (loginStep.getPassword() == null) {
            throw new IllegalArgumentException("LOGIN step requires 'password' property");
        }

        Properties props = new Properties();
        props.setProperty("SESSION_HOST", loginStep.getHost());
        props.setProperty("SESSION_USER", loginStep.getUser());
        props.setProperty("SESSION_PASSWORD", loginStep.getPassword());

        SessionConfig config = new SessionConfig("dummy", "dummy");

        return new Session5250(props, "workflow-session", "WorkflowSession", config);
    }
}
