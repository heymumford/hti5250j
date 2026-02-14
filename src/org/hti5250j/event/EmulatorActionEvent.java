/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.event;

import java.util.EventObject;

/**
 * Emulator action event representing user actions on the emulator UI.
 *
 * <p>Since Java Records cannot extend classes, this is a regular class designed with
 * record-like qualities: immutable data representation with clear field separation
 * between EventObject's source and additional event-specific fields (message, action).
 *
 * <p>Action types:
 * <ul>
 *   <li>{@code CLOSE_SESSION} - Close current session
 *   <li>{@code START_NEW_SESSION} - Start new session
 *   <li>{@code CLOSE_EMULATOR} - Close emulator application
 *   <li>{@code START_DUPLICATE} - Duplicate current session
 * </ul>
 */
public final class EmulatorActionEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public static final int CLOSE_SESSION = 1;
    public static final int START_NEW_SESSION = 2;
    public static final int CLOSE_EMULATOR = 3;
    public static final int START_DUPLICATE = 4;

    private String message;
    private int action;

    /**
     * Constructs an EmulatorActionEvent with source only.
     *
     * @param source the object on which the event initially occurred (typically a SessionPanel)
     * @throws IllegalArgumentException if source is null
     */
    public EmulatorActionEvent(Object source) {
        this(source, null, 0);
    }

    /**
     * Constructs an EmulatorActionEvent with source and message.
     *
     * @param source the object on which the event initially occurred (typically a SessionPanel)
     * @param message optional message associated with the event (may be null)
     * @throws IllegalArgumentException if source is null
     */
    public EmulatorActionEvent(Object source, String message) {
        this(source, message, 0);
    }

    /**
     * Canonical constructor: EmulatorActionEvent with source, message, and action.
     * This is the master constructor; all other constructors delegate here.
     *
     * @param source the object on which the event initially occurred (typically a SessionPanel)
     * @param message optional message associated with the event (may be null)
     * @param action the action code (e.g., CLOSE_SESSION, START_NEW_SESSION)
     * @throws IllegalArgumentException if source is null (from EventObject)
     */
    public EmulatorActionEvent(Object source, String message, int action) {
        super(source);
        this.message = message;
        this.action = action;
    }

    /**
     * Returns the message associated with this event.
     *
     * @return the message, or null if not set
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message field.
     * Note: This maintains backward compatibility with existing code in SessionPanel.
     * For new code, consider using the constructor instead.
     *
     * @param message the message to set (may be null)
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the action code for this event.
     *
     * @return the action code (e.g., CLOSE_SESSION, START_NEW_SESSION, 0 if not set)
     */
    public int getAction() {
        return action;
    }

    /**
     * Sets the action field.
     * Note: This maintains backward compatibility with existing code in SessionPanel.
     * For new code, consider using the constructor instead.
     *
     * @param action the action code to set
     */
    public void setAction(int action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return String.format(
            "%s[source=%s, message=%s, action=%d]",
            getClass().getSimpleName(),
            getSource(),
            message,
            action
        );
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getSource(), message, action);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EmulatorActionEvent other = (EmulatorActionEvent) obj;
        return java.util.Objects.equals(getSource(), other.getSource())
            && java.util.Objects.equals(message, other.message)
            && action == other.action;
    }
}
