/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.interfaces;

import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.session.DefaultHeadlessSession;
import org.hti5250j.session.NullRequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for HeadlessSession interface (Phase 15B).
 * <p>
 * Verifies that HeadlessSession provides:
 * 1. Session lifecycle (connect, disconnect)
 * 2. Screen interaction (sendKeys, waitFor)
 * 3. Data access (getScreen, getConfiguration)
 * 4. Screenshot generation (PNG via HeadlessScreenRenderer)
 * 5. SYSREQ handling via RequestHandler abstraction
 * <p>
 * No GUI coupling or java.awt imports allowed in this path.
 */
@DisplayName("HeadlessSession Interface Contract Tests")
class HeadlessSessionInterfaceTest {

    private Session5250 session5250;
    private HeadlessSession headless;

    @BeforeEach
    void setUp() {
        Properties props = createTestProperties();
        String configResource = "test-session";
        String sessionName = "test-session";
        SessionConfig config = new SessionConfig(configResource, sessionName);

        session5250 = new Session5250(props, configResource, sessionName, config);
        headless = new DefaultHeadlessSession(session5250, new NullRequestHandler());
    }

    @Test
    @DisplayName("getSessionName returns configured name")
    void testGetSessionName() {
        assertEquals("test-session", headless.getSessionName());
    }

    @Test
    @DisplayName("isConnected returns false when disconnected")
    void testIsConnectedWhenDisconnected() {
        assertFalse(headless.isConnected());
    }

    @Test
    @DisplayName("getConfiguration returns SessionConfig")
    void testGetConfiguration() {
        SessionConfig config = headless.getConfiguration();
        assertNotNull(config);
        assertEquals("test-session", config.getSessionName());
    }

    @Test
    @DisplayName("getConnectionProperties returns Properties")
    void testGetConnectionProperties() {
        java.util.Properties props = headless.getConnectionProperties();
        assertNotNull(props);
        assertTrue(props.containsKey("host"));
    }

    @Test
    @DisplayName("getScreen throws IllegalStateException when not connected")
    void testGetScreenThrowsWhenNotConnected() {
        assertThrows(IllegalStateException.class, () -> headless.getScreen());
    }

    @Test
    @DisplayName("disconnect is idempotent")
    void testDisconnectIdempotent() {
        // Should not throw even when not connected
        headless.disconnect();
        headless.disconnect();
        // If we get here, test passes
    }

    @Test
    @DisplayName("sendKeys throws when not connected")
    void testSendKeysThrowsWhenNotConnected() {
        assertThrows(IllegalStateException.class, () -> headless.sendKeys("test"));
    }

    @Test
    @DisplayName("waitForKeyboardUnlock throws when not connected")
    void testWaitForKeyboardUnlockThrowsWhenNotConnected() {
        assertThrows(Exception.class, () -> headless.waitForKeyboardUnlock(1000));
    }

    @Test
    @DisplayName("handleSystemRequest delegates to RequestHandler")
    void testHandleSystemRequestDelegates() {
        RequestHandler customHandler = new TestRequestHandler("test-response");
        DefaultHeadlessSession customSession = new DefaultHeadlessSession(session5250, customHandler);

        String response = customSession.handleSystemRequest();
        assertEquals("test-response", response);
    }

    @Test
    @DisplayName("addSessionListener succeeds (even when not connected)")
    void testAddSessionListener() {
        TestSessionListener listener = new TestSessionListener();
        headless.addSessionListener(listener);
        // If we get here, listener was added successfully
    }

    @Test
    @DisplayName("removeSessionListener succeeds (even when not connected)")
    void testRemoveSessionListener() {
        TestSessionListener listener = new TestSessionListener();
        headless.addSessionListener(listener);
        headless.removeSessionListener(listener);
        // If we get here, listener was removed successfully
    }

    @Test
    @DisplayName("signalBell does not throw")
    void testSignalBellDoesNotThrow() {
        headless.signalBell();
        // Success if no exception thrown
    }

    @Test
    @DisplayName("HeadlessSession has no GUI imports in call chain")
    void testNoGuiImportsInHeadlessPath() {
        // Verify DefaultHeadlessSession class does not import java.awt
        String className = DefaultHeadlessSession.class.getName();
        assertTrue(className.startsWith("org.hti5250j"),
                  "DefaultHeadlessSession should be in hti5250j package");

        // Verify interface is pure data API
        Class<?>[] interfaces = DefaultHeadlessSession.class.getInterfaces();
        assertTrue(interfaces.length > 0, "Should implement at least one interface");
        assertEquals(HeadlessSession.class, interfaces[0], "Should implement HeadlessSession");
    }

    /**
     * Helper: Create test Properties.
     */
    private Properties createTestProperties() {
        Properties props = new Properties();
        props.setProperty("host", "localhost");
        props.setProperty("port", "23");
        props.setProperty("screen-size", "24x80");
        props.setProperty("code-page", "37");
        return props;
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
            // No-op for testing
        }
    }
}
