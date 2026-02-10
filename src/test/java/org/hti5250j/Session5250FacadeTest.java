/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j;

import org.hti5250j.interfaces.HeadlessSession;
import org.hti5250j.interfaces.RequestHandler;
import org.hti5250j.interfaces.SessionInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Session5250 as Facade (Phase 15B).
 * <p>
 * Verifies:
 * 1. asHeadlessSession() returns HeadlessSession interface
 * 2. setRequestHandler() updates RequestHandler
 * 3. Backward compatibility with existing SessionInterface
 * 4. signalBell() is headless-safe (no Toolkit when no GUI)
 * 5. showSystemRequest() uses RequestHandler pattern
 */
@DisplayName("Session5250 Facade Tests")
class Session5250FacadeTest {

    private Session5250 session;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.setProperty("host", "localhost");
        props.setProperty("port", "23");

        SessionConfig config = new SessionConfig("test", "test");
        session = new Session5250(props, "test", "test", config);
    }

    @Test
    @DisplayName("asHeadlessSession returns non-null HeadlessSession")
    void testAsHeadlessSessionReturnsInterface() {
        HeadlessSession headless = session.asHeadlessSession();
        assertNotNull(headless);
        assertTrue(headless instanceof HeadlessSession);
    }

    @Test
    @DisplayName("asHeadlessSession returns same session wrapped")
    void testAsHeadlessSessionWrapsSession() {
        HeadlessSession headless = session.asHeadlessSession();
        assertEquals("test", headless.getSessionName());
    }

    @Test
    @DisplayName("asHeadlessSession called multiple times returns consistent interface")
    void testAsHeadlessSessionConsistent() {
        HeadlessSession h1 = session.asHeadlessSession();
        HeadlessSession h2 = session.asHeadlessSession();

        // Both should wrap the same underlying session
        assertEquals(h1.getSessionName(), h2.getSessionName());
    }

    @Test
    @DisplayName("setRequestHandler requires non-null handler")
    void testSetRequestHandlerRequiresNonNull() {
        assertThrows(NullPointerException.class, () ->
            session.setRequestHandler(null)
        );
    }

    @Test
    @DisplayName("setRequestHandler accepts valid RequestHandler")
    void testSetRequestHandlerValid() {
        RequestHandler handler = new TestRequestHandler("response");
        // Should not throw
        session.setRequestHandler(handler);
    }

    @Test
    @DisplayName("setRequestHandler updates delegate handler")
    void testSetRequestHandlerUpdatesDelegate() {
        RequestHandler handler = new TestRequestHandler("test-response");
        session.setRequestHandler(handler);

        HeadlessSession headless = session.asHeadlessSession();
        String response = headless.handleSystemRequest();
        assertEquals("test-response", response);
    }

    @Test
    @DisplayName("signalBell does not throw in headless mode (no GUI)")
    void testSignalBellHeadlessSafe() {
        // Session without GUI component should not throw
        assertDoesNotThrow(() -> session.signalBell());
    }

    @Test
    @DisplayName("showSystemRequest returns response from RequestHandler")
    void testShowSystemRequestUsesHandler() {
        RequestHandler handler = new TestRequestHandler("5");
        session.setRequestHandler(handler);

        String response = session.showSystemRequest();
        assertEquals("5", response);
    }

    @Test
    @DisplayName("Session5250 still implements SessionInterface (backward compatible)")
    void testSession5250BackwardCompatible() {
        // Should still implement SessionInterface
        assertTrue(session instanceof SessionInterface,
                  "Session5250 should implement SessionInterface for backward compatibility");

        // Should still have old methods
        assertFalse(session.isConnected());
        assertEquals("test", session.getSessionName());
    }

    @Test
    @DisplayName("HeadlessSession and SessionInterface view same underlying session")
    void testHeadlessAndSessionInterfaceSyncState() {
        HeadlessSession headless = session.asHeadlessSession();

        // Both should see same connection state
        assertEquals(session.isConnected(), headless.isConnected());

        // Both should see same session name
        assertEquals(session.getSessionName(), headless.getSessionName());
    }

    @Test
    @DisplayName("getConfiguration returns same config to both interfaces")
    void testConfigurationSharedBetweenInterfaces() {
        HeadlessSession headless = session.asHeadlessSession();

        SessionConfig sessionConfig = session.getConfiguration();
        SessionConfig headlessConfig = headless.getConfiguration();

        assertEquals(sessionConfig.getSessionName(), headlessConfig.getSessionName());
    }

    @Test
    @DisplayName("RequestHandler can be changed multiple times")
    void testRequestHandlerCanBeChanged() {
        RequestHandler handler1 = new TestRequestHandler("response1");
        session.setRequestHandler(handler1);

        RequestHandler handler2 = new TestRequestHandler("response2");
        session.setRequestHandler(handler2);

        HeadlessSession headless = session.asHeadlessSession();
        String response = headless.handleSystemRequest();
        assertEquals("response2", response);
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
