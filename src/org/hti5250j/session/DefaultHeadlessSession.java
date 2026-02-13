/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

import org.hti5250j.HeadlessScreenRenderer;
import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.interfaces.HeadlessSession;
import org.hti5250j.interfaces.RequestHandler;
import org.hti5250j.event.SessionListener;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import java.awt.image.BufferedImage;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation of HeadlessSession interface.
 * <p>
 * This class uses composition over inheritance to wrap Session5250/Screen5250
 * while providing a clean headless interface. It:
 * <p>
 * 1. Holds a reference to the underlying Session5250 (created externally)
 * 2. Wraps core data access methods (getScreen, getConfiguration, etc.)
 * 3. Delegates I/O operations (connect, disconnect, sendKeys) to Session5250
 * 4. Injects RequestHandler for SYSREQ handling (enablement for Robot Framework)
 * 5. Uses HeadlessScreenRenderer for screenshot generation (no persistent GUI)
 * <p>
 * NOTE: This implementation depends on Session5250 as an internal detail.
 * Future refactoring can eliminate this dependency by directly implementing
 * the TN5250E protocol (tnvt layer), but current composition approach
 * provides backward compatibility and lower risk.
 *
 * @since 0.12.0
 */
public class DefaultHeadlessSession implements HeadlessSession {

    private static final HTI5250jLogger log = HTI5250jLogFactory.getLogger(DefaultHeadlessSession.class);

    private final Session5250 session;
    private final RequestHandler requestHandler;

    /**
     * Create headless session with default (null) request handler.
     * <p>
     * SYSREQ requests will return null (return to menu).
     *
     * @param session Session5250 instance to wrap (must be initialized)
     * @throws NullPointerException if session is null
     */
    public DefaultHeadlessSession(Session5250 session) {
        this(session, new NullRequestHandler());
    }

    /**
     * Create headless session with custom request handler.
     * <p>
     * Enables Robot Framework integration, workflow automation, and custom
     * SYSREQ handling logic.
     *
     * @param session Session5250 instance to wrap (must be initialized)
     * @param requestHandler custom RequestHandler implementation
     * @throws NullPointerException if either parameter is null
     */
    public DefaultHeadlessSession(Session5250 session, RequestHandler requestHandler) {
        if (session == null) {
            throw new NullPointerException("Session5250 cannot be null");
        }
        if (requestHandler == null) {
            throw new NullPointerException("RequestHandler cannot be null");
        }
        this.session = session;
        this.requestHandler = requestHandler;
    }

    @Override
    public String getSessionName() {
        return session.getSessionName();
    }

    @Override
    public Screen5250 getScreen() throws IllegalStateException {
        if (!session.isConnected()) {
            throw new IllegalStateException("Session not connected");
        }
        return session.getScreen();
    }

    @Override
    public SessionConfig getConfiguration() {
        return session.getConfiguration();
    }

    @Override
    public Properties getConnectionProperties() {
        return session.getConnectionProperties();
    }

    @Override
    public boolean isConnected() {
        return session.isConnected();
    }

    @Override
    public void connect() {
        if (isConnected()) {
            throw new IllegalStateException("Session already connected");
        }
        session.connect();
    }

    @Override
    public void disconnect() {
        try {
            session.disconnect();
        } catch (Exception e) {
            log.warn("Exception during disconnect: " + e.getMessage());
        }
    }

    @Override
    public void sendKeys(String keys) {
        if (!isConnected()) {
            throw new IllegalStateException("Session not connected");
        }
        try {
            Screen5250 screen = getScreen();
            screen.sendKeys(keys);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send keys: " + e.getMessage(), e);
        }
    }

    @Override
    public void waitForKeyboardUnlock(int timeoutMs) throws Exception {
        Screen5250 screen = getScreen();
        long start = System.currentTimeMillis();

        while (screen.getOIA().isKeyBoardLocked()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new TimeoutException("Keyboard locked after " + timeoutMs + "ms");
            }
            Thread.sleep(100);
        }
    }

    @Override
    public void waitForKeyboardLockCycle(int timeoutMs) throws Exception {
        Screen5250 screen = getScreen();

        // Wait for lock (submission accepted) — short timeout
        long start = System.currentTimeMillis();
        while (!screen.getOIA().isKeyBoardLocked()) {
            if (System.currentTimeMillis() - start > 1000) {
                return;  // Completed instantly
            }
            Thread.sleep(50);
        }

        // Wait for unlock (screen refreshed)
        start = System.currentTimeMillis();
        while (screen.getOIA().isKeyBoardLocked()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new TimeoutException("Screen not refreshed after " + timeoutMs + "ms");
            }
            Thread.sleep(100);
        }
    }

    @Override
    public BufferedImage captureScreenshot() {
        SessionConfig config = getConfiguration();
        if (config == null) {
            throw new IllegalStateException("SessionConfig missing");
        }

        try {
            Screen5250 screen = getScreen();
            return HeadlessScreenRenderer.renderScreen(screen, config);
        } catch (Exception e) {
            log.error("Screenshot generation failed: " + e.getMessage());
            throw new RuntimeException("Screenshot failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getScreenAsText() {
        Screen5250 screen = getScreen();
        char[] screenChars = screen.getScreenAsChars();
        return new String(screenChars);
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        session.addSessionListener(listener);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        session.removeSessionListener(listener);
    }

    @Override
    public void signalBell() {
        session.signalBell();
    }

    @Override
    public String handleSystemRequest() {
        String screenContent = getScreenAsText();
        return requestHandler.handleSystemRequest(screenContent);
    }

}
