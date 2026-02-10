/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

import org.hti5250j.interfaces.HeadlessSession;
import org.hti5250j.interfaces.HeadlessSessionFactory;
import org.hti5250j.interfaces.RequestHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultHeadlessSessionFactory (Phase 15B).
 * <p>
 * Verifies factory pattern:
 * 1. Default constructor creates sessions with NullRequestHandler
 * 2. Custom RequestHandler constructor enables injection
 * 3. createSession validates inputs
 * 4. Returns properly initialized HeadlessSession instances
 */
@DisplayName("DefaultHeadlessSessionFactory Tests")
class DefaultHeadlessSessionFactoryTest {

    @Test
    @DisplayName("Factory can be created without arguments")
    void testFactoryDefaultConstructor() {
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();
        assertNotNull(factory);
    }

    @Test
    @DisplayName("Factory can be created with custom RequestHandler")
    void testFactoryCustomHandlerConstructor() {
        RequestHandler handler = new NullRequestHandler("1");
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory(handler);
        assertNotNull(factory);
    }

    @Test
    @DisplayName("Factory with null handler defaults to NullRequestHandler")
    void testFactoryNullHandlerDefaultsToNull() {
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory(null);
        assertNotNull(factory);

        // Creating session should succeed
        HeadlessSession session = factory.createSession("test", "test", new Properties());
        assertNotNull(session);
    }

    @Test
    @DisplayName("createSession requires non-null sessionName")
    void testCreateSessionRequiresSessionName() {
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();
        Properties props = new Properties();
        props.setProperty("host", "localhost");

        assertThrows(IllegalArgumentException.class, () ->
            factory.createSession(null, "config", props)
        );
    }

    @Test
    @DisplayName("createSession requires non-empty sessionName")
    void testCreateSessionRequiresNonEmptySessionName() {
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();
        Properties props = new Properties();
        props.setProperty("host", "localhost");

        assertThrows(IllegalArgumentException.class, () ->
            factory.createSession("", "config", props)
        );
    }

    @Test
    @DisplayName("createSession requires non-null properties")
    void testCreateSessionRequiresProperties() {
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();

        assertThrows(IllegalArgumentException.class, () ->
            factory.createSession("test", "config", null)
        );
    }

    @Test
    @DisplayName("createSession returns HeadlessSession instance")
    void testCreateSessionReturnsHeadlessSession() {
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();
        Properties props = new Properties();
        props.setProperty("host", "localhost");

        HeadlessSession session = factory.createSession("test-session", "config", props);
        assertNotNull(session);
        assertTrue(session instanceof HeadlessSession);
    }

    @Test
    @DisplayName("createSession sets session name correctly")
    void testCreateSessionSetsName() {
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();
        Properties props = new Properties();
        props.setProperty("host", "localhost");

        HeadlessSession session = factory.createSession("my-session", "config", props);
        assertEquals("my-session", session.getSessionName());
    }

    @Test
    @DisplayName("createSession passes properties to session")
    void testCreateSessionPassesProperties() {
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();
        Properties props = new Properties();
        props.setProperty("host", "ibm-i.example.com");
        props.setProperty("port", "23");

        HeadlessSession session = factory.createSession("test", "config", props);
        java.util.Properties sessionProps = session.getConnectionProperties();
        assertEquals("ibm-i.example.com", sessionProps.getProperty("host"));
    }

    @Test
    @DisplayName("createSession injects custom RequestHandler")
    void testCreateSessionInjectsHandler() {
        RequestHandler handler = new TestRequestHandler("custom-response");
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory(handler);

        Properties props = new Properties();
        props.setProperty("host", "localhost");

        HeadlessSession session = factory.createSession("test", "config", props);
        String response = session.handleSystemRequest();
        assertEquals("custom-response", response);
    }

    @Test
    @DisplayName("Factory can create multiple independent sessions")
    void testFactoryCreatesMultipleSessions() {
        HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();
        Properties props = new Properties();
        props.setProperty("host", "localhost");

        HeadlessSession session1 = factory.createSession("session1", "config", props);
        HeadlessSession session2 = factory.createSession("session2", "config", props);

        assertNotEquals(session1.getSessionName(), session2.getSessionName());
        assertEquals("session1", session1.getSessionName());
        assertEquals("session2", session2.getSessionName());
    }

    /**
     * Test RequestHandler implementation.
     */
    private static class TestRequestHandler implements RequestHandler {
        private final String response;

        TestRequestHandler(String response) {
            this.response = response;
        }

        @Override
        public String handleSystemRequest(String screenContent) {
            return response;
        }
    }
}
