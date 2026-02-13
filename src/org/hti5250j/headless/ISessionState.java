/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.headless;

/**
 * Session state enumeration for headless operations.
 *
 * Represents the lifecycle state of a session, independent of
 * UI representation. Enables programmatic state-based decision making.
 *
 * State Machine:
 * ```
 * CREATED ──→ CONNECTED ──→ DISCONNECTED
 *    ↓                           ↑
 *    └─────→ ERROR ──────────────┘
 * ```
 *
 * Transitions:
 * - CREATED → CONNECTED: Call ISession.connect()
 * - CONNECTED → DISCONNECTED: Call ISession.disconnect()
 * - Any → ERROR: Connection failure, unexpected shutdown, etc.
 * - ERROR → DISCONNECTED: Manual cleanup
 *
 * @since Wave 3A Track 3
 */
public class ISessionState {

    private static final String CREATED_STR = "CREATED";
    private static final String CONNECTED_STR = "CONNECTED";
    private static final String DISCONNECTED_STR = "DISCONNECTED";
    private static final String ERROR_STR = "ERROR";

    public static final ISessionState CREATED = new ISessionState(CREATED_STR);
    public static final ISessionState CONNECTED = new ISessionState(CONNECTED_STR);
    public static final ISessionState DISCONNECTED = new ISessionState(DISCONNECTED_STR);
    public static final ISessionState ERROR = new ISessionState(ERROR_STR);

    private final String state;

    private ISessionState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return state;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ISessionState)) return false;
        return state.equals(((ISessionState) obj).state);
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }

}
