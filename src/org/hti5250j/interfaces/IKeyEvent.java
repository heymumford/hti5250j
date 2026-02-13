/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.interfaces;

/**
 * Platform-independent key event interface.
 * Abstracts java.awt.event.KeyEvent for headless operation.
 */
public interface IKeyEvent {

    /**
     * Get the key code associated with the key in this event.
     * @return key code
     */
    int getKeyCode();

    /**
     * Check if the Shift modifier is down on this event.
     * @return true if Shift is down
     */
    boolean isShiftDown();

    /**
     * Check if the Control modifier is down on this event.
     * @return true if Control is down
     */
    boolean isControlDown();

    /**
     * Check if the Alt modifier is down on this event.
     * @return true if Alt is down
     */
    boolean isAltDown();

    /**
     * Check if the AltGraph modifier is down on this event.
     * @return true if AltGraph is down
     */
    boolean isAltGraphDown();

    /**
     * Get the location of the key during the event.
     * @return key location (STANDARD, LEFT, RIGHT, NUMPAD, or UNKNOWN)
     */
    int getKeyLocation();

    /**
     * Get the character associated with the key in this event.
     * @return key character
     */
    char getKeyChar();

    /**
     * Consume this event so that it will not be processed in the default manner.
     */
    void consume();

    /**
     * Check if this event has been consumed.
     * @return true if consumed
     */
    boolean isConsumed();
}
