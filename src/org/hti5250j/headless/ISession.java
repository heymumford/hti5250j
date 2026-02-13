/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.headless;

/**
 * Headless-compatible session interface.
 *
 * Represents a single session (connection) to a 5250 host without
 * Swing/AWT dependencies. Enables programmatic access to session
 * configuration and state.
 *
 * Lifecycle:
 * 1. Created via ISessionManager.createSession() - state: CREATED
 * 2. Optional: call connect() to establish connection - state: CONNECTED
 * 3. Use as needed
 * 4. Call disconnect() or ISessionManager.closeSession() - state: DISCONNECTED
 *
 * Configuration:
 * Session parameters (hostname, port) are immutable after creation.
 * For configuration changes, create new session.
 *
 * Thread Safety:
 * Implementations must be thread-safe for concurrent access.
 *
 * @see ISessionManager
 * @see ISessionState
 * @since Wave 3A Track 3
 */
public interface ISession {

    /**
     * Get the unique session identifier.
     *
     * Assigned by ISessionManager at creation time.
     * Immutable for session lifetime.
     *
     * @return Session ID (non-null, non-empty)
     */
    String getId();

    /**
     * Get the target host name or IP address.
     *
     * Configured at session creation time.
     * Immutable.
     *
     * @return Hostname (e.g., "mainframe.example.com" or "192.168.1.1")
     */
    String getHostname();

    /**
     * Get the target port number.
     *
     * Configured at session creation time.
     * Typically 23 for Telnet or 5250-based protocols.
     * Immutable.
     *
     * @return Port number (1-65535)
     */
    int getPort();

    /**
     * Check if session is currently connected to the host.
     *
     * @return true if connection is active, false otherwise
     */
    boolean isConnected();

    /**
     * Establish connection to the host.
     *
     * Transitions session state from CREATED to CONNECTED.
     * Safe to call multiple times (idempotent if already connected).
     *
     * @throws RuntimeException if connection fails (wrapped IOException)
     */
    void connect();

    /**
     * Close connection to the host.
     *
     * Transitions session state to DISCONNECTED.
     * Safe to call multiple times (idempotent).
     * Cleans up any resources (streams, sockets, etc.).
     */
    void disconnect();

}
