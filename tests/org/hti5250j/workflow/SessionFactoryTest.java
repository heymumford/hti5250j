/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.hti5250j.interfaces.SessionInterface;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SessionFactory Tests")
class SessionFactoryTest {

    @Test
    @DisplayName("createFromLoginStep() should create session from valid LOGIN step")
    void testCreateFromValidLoginStep() throws Exception {
        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("testhost.example.com");
        loginStep.setUser("TESTUSER");
        loginStep.setPassword("TESTPASS123");

        SessionInterface session = SessionFactory.createFromLoginStep(loginStep);

        assertNotNull(session);
    }

    @Test
    @DisplayName("createFromLoginStep() should throw when step is null")
    void testCreateFromNullStep() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> SessionFactory.createFromLoginStep(null));

        assertTrue(ex.getMessage().contains("cannot be null"));
    }

    @Test
    @DisplayName("createFromLoginStep() should throw when host is missing")
    void testCreateMissingHost() {
        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setUser("TESTUSER");
        loginStep.setPassword("TESTPASS");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> SessionFactory.createFromLoginStep(loginStep));

        assertTrue(ex.getMessage().contains("host"));
    }

    @Test
    @DisplayName("createFromLoginStep() should throw when user is missing")
    void testCreateMissingUser() {
        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("testhost");
        loginStep.setPassword("TESTPASS");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> SessionFactory.createFromLoginStep(loginStep));

        assertTrue(ex.getMessage().contains("user"));
    }

    @Test
    @DisplayName("createFromLoginStep() should throw when password is missing")
    void testCreateMissingPassword() {
        StepDef loginStep = new StepDef();
        loginStep.setAction(ActionType.LOGIN);
        loginStep.setHost("testhost");
        loginStep.setUser("TESTUSER");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> SessionFactory.createFromLoginStep(loginStep));

        assertTrue(ex.getMessage().contains("password"));
    }

    @Test
    @DisplayName("createFromLoginStep() should accept various host formats")
    void testCreateVariousHostFormats() throws Exception {
        String[] hosts = {
            "localhost",
            "192.168.1.1",
            "ibmi.example.com",
            "prod.ibmi.internal"
        };

        for (String host : hosts) {
            StepDef loginStep = new StepDef();
            loginStep.setHost(host);
            loginStep.setUser("USER");
            loginStep.setPassword("PASS");

            SessionInterface session = SessionFactory.createFromLoginStep(loginStep);
            assertNotNull(session);
        }
    }

    @Test
    @DisplayName("createFromLoginStep() should accept various credential formats")
    void testCreateVariousCredentials() throws Exception {
        String[][] credentials = {
            {"USER", "PASS"},
            {"user123", "p@ssw0rd"},
            {"ADMINISTRATOR", "MyP@ssw0rd123!"},
            {"test.user", "test.pass"}
        };

        for (String[] cred : credentials) {
            StepDef loginStep = new StepDef();
            loginStep.setHost("host");
            loginStep.setUser(cred[0]);
            loginStep.setPassword(cred[1]);

            SessionInterface session = SessionFactory.createFromLoginStep(loginStep);
            assertNotNull(session);
        }
    }

    @Test
    @DisplayName("createFromLoginStep() should create independent sessions")
    void testCreateIndependentSessions() throws Exception {
        StepDef loginStep1 = new StepDef();
        loginStep1.setHost("host1");
        loginStep1.setUser("USER1");
        loginStep1.setPassword("PASS1");

        StepDef loginStep2 = new StepDef();
        loginStep2.setHost("host2");
        loginStep2.setUser("USER2");
        loginStep2.setPassword("PASS2");

        SessionInterface session1 = SessionFactory.createFromLoginStep(loginStep1);
        SessionInterface session2 = SessionFactory.createFromLoginStep(loginStep2);

        assertNotSame(session1, session2);
    }

    @Test
    @DisplayName("createFromLoginStep() should handle empty string host")
    void testCreateEmptyHost() {
        StepDef loginStep = new StepDef();
        loginStep.setHost("");
        loginStep.setUser("USER");
        loginStep.setPassword("PASS");

        // Empty string is non-null, so should create session
        assertDoesNotThrow(() -> SessionFactory.createFromLoginStep(loginStep));
    }

    @Test
    @DisplayName("createFromLoginStep() should ignore non-LOGIN action type")
    void testCreateIgnoresActionType() throws Exception {
        StepDef step = new StepDef();
        step.setAction(ActionType.FILL);  // Wrong action type
        step.setHost("host");
        step.setUser("USER");
        step.setPassword("PASS");

        // Factory should not validate action type
        SessionInterface session = SessionFactory.createFromLoginStep(step);
        assertNotNull(session);
    }
}
