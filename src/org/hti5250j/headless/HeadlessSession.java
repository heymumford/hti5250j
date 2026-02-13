/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.headless;

/**
 * Headless session implementation.
 *
 * Represents a single 5250 session without Swing/AWT dependencies.
 * Maintains session configuration (hostname, port) and connection state.
 *
 * Lifecycle:
 * 1. Created in CREATED state - configuration set, no connection
 * 2. Call connect() to establish connection - transitions to CONNECTED
 * 3. Use session as needed
 * 4. Call disconnect() - transitions to DISCONNECTED
 * 5. Can reconnect() if needed (idempotent)
 *
 * Configuration:
 * - hostname and port are immutable after creation
 * - Set during construction, not changeable
 * - To change host/port, create new session
 *
 * Connection State:
 * - Initially disconnected (isConnected() = false)
 * - After connect(): isConnected() = true
 * - After disconnect(): isConnected() = false
 * - connect() on already-connected session is safe (idempotent)
 * - disconnect() on already-disconnected session is safe (idempotent)
 *
 * Thread Safety:
 * Uses simple volatile field for connection state.
 * Safe for concurrent calls (worst case: duplicate connection attempts).
 *
 * @see ISession
 * @see ISessionManager
 * @since Wave 3A Track 3
 */
public class HeadlessSession implements ISession {

    /** Unique session identifier (assigned by manager) */
    private final String id;

    /** Target host name or IP address (immutable) */
    private final String hostname;

    /** Target port number (immutable) */
    private final int port;

    /** Connection state (volatile for visibility) */
    private volatile boolean connected;

    /**
     * Create a new headless session.
     *
     * Initializes in CREATED state (disconnected).
     * Typically called by HeadlessSessionManager.createSession().
     *
     * @param id Unique session identifier (UUID format recommended)
     * @param hostname Target host (e.g., "mainframe" or "192.168.1.1")
     * @param port Target port (typically 23)
     * @throws IllegalArgumentException if any parameter is null
     */
    public HeadlessSession(String id, String hostname, int port) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (hostname == null || hostname.isEmpty()) {
            throw new IllegalArgumentException("Hostname cannot be null or empty");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }

        this.id = id;
        this.hostname = hostname;
        this.port = port;
        this.connected = false;  // Initially disconnected
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void connect() {
        // In headless mode, "connection" is just state tracking.
        // Real TCP connection happens in TN5250 layer (tnvt.java).
        // This method marks the session as "active" for the headless manager.
        this.connected = true;
    }

    @Override
    public void disconnect() {
        // In headless mode, "disconnection" is just state tracking.
        // Real TCP cleanup happens in TN5250 layer.
        // This method marks the session as "inactive".
        this.connected = false;
    }

    @Override
    public String toString() {
        return String.format(
                "HeadlessSession{id=%s, hostname=%s, port=%d, connected=%b}",
                id, hostname, port, connected);
    }

}
