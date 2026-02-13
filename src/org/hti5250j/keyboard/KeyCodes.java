/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.keyboard;

/**
 * Platform-independent key code constants extracted from java.awt.event.KeyEvent.
 * This enum allows headless operation without importing AWT/Swing classes.
 */
public final class KeyCodes {

    // Prevent instantiation
    private KeyCodes() {}

    /**
     * A constant indicating that the keyLocation is indeterminate or not relevant.
     * KEY_TYPED events do not have a keyLocation; this value is used instead.
     */
    public static final int KEY_LOCATION_UNKNOWN = 0;

    /**
     * A constant indicating that the key pressed or released is not distinguished
     * as the left or right version of a key, and did not originate on the numeric keypad.
     */
    public static final int KEY_LOCATION_STANDARD = 1;

    /**
     * A constant indicating that the key pressed or released is in the left key location
     * (there is more than one possible location for this key). Example: the left shift key.
     */
    public static final int KEY_LOCATION_LEFT = 2;

    /**
     * A constant indicating that the key pressed or released is in the right key location
     * (there is more than one possible location for this key). Example: the right shift key.
     */
    public static final int KEY_LOCATION_RIGHT = 3;

    /**
     * A constant indicating that the key event originated on the numeric keypad
     * or with a virtual key corresponding to the numeric keypad.
     */
    public static final int KEY_LOCATION_NUMPAD = 4;

    /**
     * Get the text description of a key code.
     * This is a fallback implementation that returns the numeric code as a string.
     * @param keyCode the key code
     * @return text description
     */
    public static String getKeyText(int keyCode) {
        // Simple implementation - can be enhanced with actual key name mappings
        return "KeyCode_" + keyCode;
    }
}
