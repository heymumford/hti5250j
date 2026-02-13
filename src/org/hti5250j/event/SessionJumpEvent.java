/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.event;

import java.util.EventObject;

/**
 * Immutable event representing navigation between session tabs.
 */
public class SessionJumpEvent extends EventObject implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final int jumpDirection;
    private final String message;

    /**
     * Constructor with all parameters.
     *
     * @param source the object on which the Event initially occurred
     * @param jumpDirection the direction to jump (typically JUMP_NEXT or JUMP_PREVIOUS)
     * @param message optional message associated with the jump event (may be null)
     * @throws NullPointerException if source is null
     */
    public SessionJumpEvent(Object source, int jumpDirection, String message) {
        super(source);
        this.jumpDirection = jumpDirection;
        this.message = message;
    }

    /** @return the jump direction value */
    public int jumpDirection() {
        return jumpDirection;
    }

    /** @return the jump direction value */
    public int getJumpDirection() {
        return jumpDirection;
    }

    /** @return the message, or null if not set */
    public String message() {
        return message;
    }

    /** @return the message, or null if not set */
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SessionJumpEvent)) {
            return false;
        }

        SessionJumpEvent other = (SessionJumpEvent) obj;
        return jumpDirection == other.jumpDirection &&
               (message == null ? other.message == null : message.equals(other.message)) &&
               (getSource() == null ? other.getSource() == null : getSource().equals(other.getSource()));
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(jumpDirection);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (getSource() != null ? getSource().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SessionJumpEvent{" +
                "source=" + getSource() +
                ", jumpDirection=" + jumpDirection +
                ", message='" + message + '\'' +
                '}';
    }
}
