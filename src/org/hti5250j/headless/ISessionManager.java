/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.headless;

/**
 * Headless-compatible session manager interface.
 *
 * Provides session lifecycle management without Swing/AWT dependencies,
 * enabling:
 * - Server operation without X11 display
 * - Programmatic session control
 * - Integration with CI/CD and automation tools
 *
 * Design Principles:
 * - Platform-independent: No java.awt.* or javax.swing.* dependencies
 * - Stateless: Operations produce deterministic results
 * - Safe: Graceful handling of invalid operations
 * - Observable: Session state accessible without UI coupling
 *
 * Comparison to Swing Approach:
 * - Old: Session5250 + SessionPanel (UI-bound)
 * - New: ISessionManager + ISession (headless-first)
 *
 * Thread Safety:
 * Implementations must be thread-safe for concurrent session access.
 *
 * @see ISession
 * @see HeadlessSessionManager
 * @since 0.12.0
 */
public interface ISessionManager {

    /**
     * Create a new session for the given host.
     *
     * Initializes session in CREATED state without attempting connection.
     * Connection must be explicitly initiated by caller.
     *
     * @param hostname The target host (e.g., "mainframe.example.com" or "192.168.1.1")
     * @param port The target port (typically 23 for Telnet/5250)
     * @return Unique session identifier (UUID format recommended)
     * @throws IllegalArgumentException if hostname is null or empty
     * @throws IllegalArgumentException if port is out of valid range (1-65535)
     */
    String createSession(String hostname, int port);

    /**
     * Retrieve an existing session by ID.
     *
     * @param sessionId The session identifier from createSession()
     * @return The session, or null if not found
     * @throws IllegalArgumentException if sessionId is null or empty
     */
    ISession getSession(String sessionId);

    /**
     * Close and remove a session.
     *
     * Disconnects active connections and removes session from manager.
     * Safe to call multiple times (idempotent).
     *
     * @param sessionId The session identifier from createSession()
     * @return true if session was closed, false if session did not exist
     * @throws IllegalArgumentException if sessionId is null or empty
     */
    boolean closeSession(String sessionId);

    /**
     * Get the number of active sessions.
     *
     * Useful for monitoring and resource management.
     *
     * @return Number of sessions currently managed (0 to n)
     */
    int getSessionCount();

    /**
     * List all active session identifiers.
     *
     * Returns a snapshot of session IDs at time of call.
     * Returned array is copy-safe (modifications don't affect manager state).
     *
     * @return Array of session IDs (never null, may be empty)
     */
    String[] listSessions();

    /**
     * Get the current state of a session.
     *
     * @param sessionId The session identifier from createSession()
     * @return Current state (CREATED, CONNECTED, DISCONNECTED, ERROR, etc.)
     * @see ISessionState
     * @throws IllegalArgumentException if sessionId is null or empty
     * @throws IllegalArgumentException if session not found
     */
    ISessionState getSessionState(String sessionId);

}
