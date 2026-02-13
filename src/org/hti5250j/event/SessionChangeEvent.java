/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.event;

import java.util.EventObject;

/**
 * Immutable event representing a session state change.
 * Provides record-like accessors and thread-safe distribution across listeners.
 */
public class SessionChangeEvent extends EventObject implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final String message;
    private final int state;

    /**
     * Constructs a SessionChangeEvent with just the source.
     *
     * This constructor creates an event with default values for message and state.
     *
     * @param source the object on which the Event initially occurred
     * @throws NullPointerException if source is null
     */
    public SessionChangeEvent(Object source) {
        this(source, null, 0);
    }

    /**
     * Constructs a SessionChangeEvent with source and message.
     *
     * @param source the object on which the Event initially occurred
     * @param message the message describing the change (may be null)
     * @throws NullPointerException if source is null
     */
    public SessionChangeEvent(Object source, String message) {
        this(source, message, 0);
    }

    /**
     * Canonical constructor with full parameters.
     *
     * This is the primary constructor that validates the source
     * is non-null per the EventObject contract. All fields are final
     * for immutability.
     *
     * @param source the object on which the Event initially occurred
     * @param message the message describing the change (may be null)
     * @param state the session state value
     * @throws NullPointerException if source is null
     */
    public SessionChangeEvent(Object source, String message, int state) {
        super(source);
        this.message = message;
        this.state = state;
    }

    /** @return the message, or null if not set */
    public String message() {
        return message;
    }

    /** @return the message, or null if not set */
    public String getMessage() {
        return message;
    }

    /** @return the state value */
    public int state() {
        return state;
    }

    /** @return the state value */
    public int getState() {
        return state;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SessionChangeEvent)) {
            return false;
        }

        SessionChangeEvent other = (SessionChangeEvent) obj;
        return state == other.state &&
               (message == null ? other.message == null : message.equals(other.message)) &&
               (getSource() == null ? other.getSource() == null : getSource().equals(other.getSource()));
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(state);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (getSource() != null ? getSource().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SessionChangeEvent{" +
                "source=" + getSource() +
                ", state=" + state +
                ", message='" + message + '\'' +
                '}';
    }
}
