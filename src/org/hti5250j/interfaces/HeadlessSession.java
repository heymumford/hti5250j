/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.interfaces;

import org.hti5250j.SessionConfig;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.event.SessionListener;

import java.awt.image.BufferedImage;
import java.util.Properties;

/**
 * Pure headless session interface — no GUI coupling, no java.awt initialization required.
 * <p>
 * This interface enables programmatic automation without GUI dependencies:
 * - Robot Framework integration via Jython
 * - Python adapters via JPype
 * - Docker containers and CI/CD pipelines
 * - Distributed session pools with virtual threads
 * <p>
 * HeadlessSession is a data transport contract that delegates GUI interaction
 * to injectable RequestHandler implementations. This follows Dependency Inversion:
 * the session depends on an abstraction (RequestHandler), not concrete GUI code.
 *
 * @since Phase 15B
 */
public interface HeadlessSession {

    /**
     * Get the session name (e.g., "production", "uat", "dev").
     * Typically set during factory creation.
     *
     * @return session name
     */
    String getSessionName();

    /**
     * Get the screen buffer (pure data, no GUI).
     * <p>
     * The screen contains:
     * - Character plane (EBCDIC, code page 37 by default)
     * - Attribute plane (field protection, reverse video, etc.)
     * - Operator Information Area (keyboard state, message line)
     *
     * @return Screen5250 instance
     * @throws IllegalStateException if screen not initialized
     */
    Screen5250 getScreen() throws IllegalStateException;

    /**
     * Get session configuration (fonts, colors, code page, timeouts).
     * <p>
     * Used by HeadlessScreenRenderer to generate PNG screenshots without
     * persistent GUI components.
     *
     * @return SessionConfig instance
     */
    SessionConfig getConfiguration();

    /**
     * Get connection properties (host, port, user, device name, etc.).
     * <p>
     * Note: Properties object is mutable. Avoid direct modification;
     * use connection parameters instead.
     *
     * @return Properties object (immutable copy recommended)
     */
    Properties getConnectionProperties();

    /**
     * Check if currently connected to IBM i system.
     *
     * @return true if socket is active and authenticated
     */
    boolean isConnected();

    /**
     * Initiate connection to IBM i system.
     * <p>
     * Connection occurs asynchronously in a daemon thread.
     * Use addSessionListener() to be notified of connection completion.
     * <p>
     * After connect() returns:
     * - Socket connects in background
     * - TLS negotiation (if configured)
     * - EBCDIC/screen format negotiation
     * - First screen buffer populated
     *
     * @throws IllegalStateException if already connected
     */
    void connect();

    /**
     * Close connection to IBM i system and clean up resources.
     * <p>
     * Safe to call multiple times (idempotent).
     * After disconnect():
     * - Socket closed
     * - All pending keyboard input flushed
     * - Screen buffer frozen (no updates)
     */
    void disconnect();

    /**
     * Send keys to the host (synchronous operation).
     * <p>
     * Mnemonic syntax:
     * - [enter] — ENTER key
     * - [tab] — TAB key
     * - [f1] through [f24] — Function keys
     * - [pf1] through [pf24] — PF1-PF24 keys
     * - [home] — HOME key
     * - [pageup], [pagedown] — Page Up/Down
     * - [escape] — ESCAPE key
     * - Text — Literal characters (EBCDIC encoded automatically)
     * <p>
     * Example: "sendKeys("CALL MYPGM[enter]")" sends literal text + ENTER
     *
     * @param keys mnemonic key sequence
     * @throws IllegalStateException if not connected
     * @throws IllegalArgumentException if mnemonic syntax invalid
     */
    void sendKeys(String keys);

    /**
     * Wait for keyboard lock to clear (screen update complete).
     * <p>
     * IBM i uses keyboard lock to signal "processing request". After sending
     * keys, the host locks the keyboard, processes the command, then unlocks
     * it with a new screen.
     *
     * @param timeoutMs maximum wait in milliseconds
     * @throws java.util.concurrent.TimeoutException if timeout exceeded
     * @throws InterruptedException if current thread interrupted
     */
    void waitForKeyboardUnlock(int timeoutMs) throws Exception;

    /**
     * Wait for a keyboard lock-unlock cycle (submit and screen refresh).
     * <p>
     * Used after submitting form data or commands:
     * 1. Wait for keyboard to lock (submission accepted)
     * 2. Wait for keyboard to unlock (new screen ready)
     * <p>
     * Handles edge case where command completes instantly (no lock phase).
     *
     * @param timeoutMs maximum total wait in milliseconds
     * @throws java.util.concurrent.TimeoutException if timeout exceeded
     * @throws InterruptedException if current thread interrupted
     */
    void waitForKeyboardLockCycle(int timeoutMs) throws Exception;

    /**
     * Generate screenshot of current screen (PNG BufferedImage).
     * <p>
     * Uses HeadlessScreenRenderer — requires NO persistent GUI components.
     * Rendered character-by-character with field attributes (colors, underline, etc.)
     * respecting the SessionConfig color palette.
     * <p>
     * Safe to call in Docker, headless environments, or with null SessionPanel.
     *
     * @return BufferedImage (TYPE_INT_RGB)
     * @throws IllegalStateException if SessionConfig missing
     * @throws Exception if rendering fails (fallback to text capture)
     */
    BufferedImage captureScreenshot();

    /**
     * Get current screen content as text (80 chars × N rows).
     * <p>
     * Returns EBCDIC characters as Java String (auto-decoded using code page).
     * Useful for assertions, field extraction, and text-based validation.
     *
     * @return screen content as string
     */
    String getScreenAsText();

    /**
     * Add listener for session state changes (connect, disconnect, etc.).
     * <p>
     * Listeners are notified on:
     * - Connection established
     * - Connection lost
     * - Keyboard locked/unlocked
     * - Screen updated
     *
     * @param listener to add
     */
    void addSessionListener(SessionListener listener);

    /**
     * Remove listener from session state notifications.
     *
     * @param listener to remove
     */
    void removeSessionListener(SessionListener listener);

    /**
     * Signal audio feedback (beep).
     * <p>
     * In headless mode, typically a no-op. In GUI mode, produces system beep
     * via Toolkit.getDefaultToolkit().beep().
     */
    void signalBell();

    /**
     * Handle system request (SYSREQ key pressed).
     * <p>
     * Delegates to injected RequestHandler implementation:
     * - GuiRequestHandler: Pop dialog for user input
     * - NullRequestHandler: Return fixed response
     * - Custom handlers: Robot Framework, workflow automation
     * <p>
     * This is the key extension point for Robot Framework integration.
     *
     * @return response string, or null if request handling not needed
     */
    String handleSystemRequest();

}
