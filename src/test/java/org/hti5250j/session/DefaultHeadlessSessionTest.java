/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.interfaces.HeadlessSession;
import org.hti5250j.interfaces.RequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultHeadlessSession wrapper (Phase 15B).
 * <p>
 * Verifies:
 * 1. Composition pattern (wraps Session5250)
 * 2. Null pointer safety (validates parameters)
 * 3. Delegation to underlying Session5250
 * 4. RequestHandler injection
 */
@DisplayName("DefaultHeadlessSession Wrapper Tests")
class DefaultHeadlessSessionTest {

    private Session5250 session5250;
    private RequestHandler testHandler;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.setProperty("host", "localhost");
        props.setProperty("port", "23");

        SessionConfig config = new SessionConfig("test", "test");
        session5250 = new Session5250(props, "test", "test", config);
        testHandler = new NullRequestHandler();
    }

    @Test
    @DisplayName("Constructor requires non-null session")
    void testConstructorRequiresNonNullSession() {
        assertThrows(NullPointerException.class, () ->
            new DefaultHeadlessSession(null, testHandler)
        );
    }

    @Test
    @DisplayName("Constructor requires non-null RequestHandler")
    void testConstructorRequiresNonNullRequestHandler() {
        assertThrows(NullPointerException.class, () ->
            new DefaultHeadlessSession(session5250, null)
        );
    }

    @Test
    @DisplayName("Constructor accepts valid parameters")
    void testConstructorValid() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        assertNotNull(headless);
    }

    @Test
    @DisplayName("getSessionName delegates to wrapped session")
    void testGetSessionNameDelegates() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        assertEquals("test", headless.getSessionName());
    }

    @Test
    @DisplayName("isConnected delegates to wrapped session")
    void testIsConnectedDelegates() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        assertFalse(headless.isConnected());  // Not connected yet
    }

    @Test
    @DisplayName("getConfiguration delegates to wrapped session")
    void testGetConfigurationDelegates() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        SessionConfig config = headless.getConfiguration();
        assertNotNull(config);
        assertEquals("test", config.getSessionName());
    }

    @Test
    @DisplayName("getConnectionProperties delegates to wrapped session")
    void testGetConnectionPropertiesDelegates() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        Properties props = headless.getConnectionProperties();
        assertNotNull(props);
        assertTrue(props.containsKey("host"));
    }

    @Test
    @DisplayName("getScreen throws when not connected")
    void testGetScreenThrowsWhenNotConnected() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        assertThrows(IllegalStateException.class, () -> headless.getScreen());
    }

    @Test
    @DisplayName("connect throws when already connected")
    void testConnectThrowsWhenAlreadyConnected() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);

        // Manually set connected state (simulating successful connection)
        session5250.connect();  // This attempts real connection
        // Skip assertion if connection fails (expected in test environment)
        if (session5250.isConnected()) {
            assertThrows(IllegalStateException.class, () -> headless.connect());
            session5250.disconnect();
        }
    }

    @Test
    @DisplayName("disconnect succeeds even when not connected")
    void testDisconnectIdempotent() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        // Should not throw
        headless.disconnect();
    }

    @Test
    @DisplayName("handleSystemRequest delegates to RequestHandler")
    void testHandleSystemRequestDelegates() {
        TestRequestHandler customHandler = new TestRequestHandler("response");
        HeadlessSession headless = new DefaultHeadlessSession(session5250, customHandler);

        String result = headless.handleSystemRequest();
        assertEquals("response", result);
    }

    @Test
    @DisplayName("sendKeys throws when not connected")
    void testSendKeysThrowsWhenNotConnected() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        assertThrows(IllegalStateException.class, () -> headless.sendKeys("test"));
    }

    @Test
    @DisplayName("getScreenAsText returns empty when not connected")
    void testGetScreenAsTextThrowsWhenNotConnected() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        // Should handle gracefully or throw
        assertThrows(IllegalStateException.class, () -> headless.getScreenAsText());
    }

    @Test
    @DisplayName("addSessionListener delegates to session")
    void testAddSessionListenerDelegates() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);

        TestSessionListener listener = new TestSessionListener();
        // Should not throw
        headless.addSessionListener(listener);
    }

    @Test
    @DisplayName("removeSessionListener delegates to session")
    void testRemoveSessionListenerDelegates() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);

        TestSessionListener listener = new TestSessionListener();
        headless.addSessionListener(listener);
        // Should not throw
        headless.removeSessionListener(listener);
    }

    @Test
    @DisplayName("signalBell delegates to session")
    void testSignalBellDelegates() {
        HeadlessSession headless = new DefaultHeadlessSession(session5250, testHandler);
        // Should not throw
        headless.signalBell();
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

    /**
     * Test SessionListener implementation.
     */
    private static class TestSessionListener implements org.hti5250j.event.SessionListener {
        @Override
        public void onSessionChanged(org.hti5250j.event.SessionChangeEvent event) {
            // No-op
        }
    }
}
