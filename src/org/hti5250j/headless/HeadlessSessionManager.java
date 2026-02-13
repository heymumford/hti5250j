/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.headless;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Headless-first session manager implementation.
 *
 * Manages multiple sessions without Swing/AWT dependencies, enabling:
 * - Programmatic session control
 * - Server-side operation without display
 * - Thread-safe concurrent session access
 * - Easy integration with CI/CD and automation
 *
 * Design Principles:
 * - Zero Swing imports: No javax.swing.*, java.awt.* dependencies
 * - Thread-safe: ConcurrentHashMap for concurrent access
 * - Stateless operations: No side effects beyond session collection
 * - Testable: Simple constructor, injectable if needed
 *
 * Implementation Details:
 * - Session IDs: UUID v4 (random, globally unique)
 * - Storage: ConcurrentHashMap for lock-free reads
 * - State: CREATED on instantiation (must call connect() to activate)
 * - Error handling: Null-safe, validation on all inputs
 *
 * Performance:
 * - Create: O(1) - UUID generation + map insert
 * - Get: O(1) - map lookup
 * - List: O(n) - snapshot of keys
 * - Close: O(1) - map removal
 *
 * Memory:
 * - Per session: ~200 bytes (HeadlessSession object + map entry)
 * - Manager overhead: ~500 bytes
 *
 * Example Usage:
 * ```java
 * ISessionManager manager = new HeadlessSessionManager();
 *
 * // Create session
 * String sessionId = manager.createSession("mainframe", 23);
 *
 * // Get session
 * ISession session = manager.getSession(sessionId);
 * session.connect();
 *
 * // Track state
 * if (manager.getSessionState(sessionId) == ISessionState.CONNECTED) {
 *     // Send commands...
 * }
 *
 * // Clean up
 * manager.closeSession(sessionId);
 * ```
 *
 * @see ISessionManager
 * @see ISession
 * @see HeadlessSession
 * @since 0.12.0
 */
public class HeadlessSessionManager implements ISessionManager {

    private final Map<String, ISession> sessions = new ConcurrentHashMap<>();

    public HeadlessSessionManager() {
    }

    @Override
    public String createSession(String hostname, int port) {
        validateHostname(hostname);
        validatePort(port);

        String sessionId = UUID.randomUUID().toString();
        ISession session = new HeadlessSession(sessionId, hostname, port);
        sessions.put(sessionId, session);

        return sessionId;
    }

    @Override
    public ISession getSession(String sessionId) {
        validateSessionId(sessionId);
        return sessions.get(sessionId);
    }

    @Override
    public boolean closeSession(String sessionId) {
        validateSessionId(sessionId);

        ISession session = sessions.remove(sessionId);
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
            return true;
        }
        return false;
    }

    @Override
    public int getSessionCount() {
        return sessions.size();
    }

    @Override
    public String[] listSessions() {
        return sessions.keySet().toArray(new String[0]);
    }

    @Override
    public ISessionState getSessionState(String sessionId) {
        validateSessionId(sessionId);

        ISession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        if (session.isConnected()) {
            return ISessionState.CONNECTED;
        } else {
            return ISessionState.CREATED;
        }
    }

    private void validateHostname(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new IllegalArgumentException("Hostname must not be null or empty");
        }
    }

    private void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(
                    "Port must be between 1 and 65535, got: " + port);
        }
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID must not be null or empty");
        }
    }

}
