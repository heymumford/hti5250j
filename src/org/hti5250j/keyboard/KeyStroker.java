/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.keyboard;

import java.awt.event.KeyEvent;
import org.hti5250j.interfaces.IKeyEvent;

/**
 * This class is basically a wrapper for KeyEvent that is used internally to the
 * project instead of KeyEvents. Uses getKeyLocation for 1.4 and greater.
 */
public class KeyStroker {

    protected int location;
    private int keyCode;
    private boolean isShiftDown;
    private boolean isControlDown;
    private boolean isAltDown;
    private boolean isAltGrDown;
    private int hashCode;

    public static final String altSuffix = ".alt2";


    // literals copied from KeyEvent of JDK Version 1.4.0

    /**
     * A constant indicating that the keyLocation is indeterminate
     * or not relevant.
     * KEY_TYPED events do not have a keyLocation; this value
     * is used instead.
     *
     * @since 1.4
     */
    public static final int KEY_LOCATION_UNKNOWN = KeyCodes.KEY_LOCATION_UNKNOWN;

    /**
     * A constant indicating that the key pressed or released
     * is not distinguished as the left or right version of a key,
     * and did not originate on the numeric keypad (or did not
     * originate with a virtual key corresponding to the numeric
     * keypad).
     *
     * @since 1.4
     */
    public static final int KEY_LOCATION_STANDARD = KeyCodes.KEY_LOCATION_STANDARD;

    /**
     * A constant indicating that the key pressed or released is in
     * the left key location (there is more than one possible location
     * for this key).  Example: the left shift key.
     *
     * @since 1.4
     */
    public static final int KEY_LOCATION_LEFT = KeyCodes.KEY_LOCATION_LEFT;

    /**
     * A constant indicating that the key pressed or released is in
     * the right key location (there is more than one possible location
     * for this key).  Example: the right shift key.
     *
     * @since 1.4
     */
    public static final int KEY_LOCATION_RIGHT = KeyCodes.KEY_LOCATION_RIGHT;

    /**
     * A constant indicating that the key event originated on the
     * numeric keypad or with a virtual key corresponding to the
     * numeric keypad.
     *
     * @since 1.4
     */
    public static final int KEY_LOCATION_NUMPAD = KeyCodes.KEY_LOCATION_NUMPAD;

    /**
     * Create a KeyStroker from an IKeyEvent (headless-compatible).
     * @param ke the key event
     */
    public KeyStroker(IKeyEvent ke) {
        this.keyCode = ke.getKeyCode();
        this.isShiftDown = ke.isShiftDown();
        this.isControlDown = ke.isControlDown();
        this.isAltDown = ke.isAltDown();
        this.isAltGrDown = ke.isAltGraphDown();
        this.location = ke.getKeyLocation();

        hashCode = computeHashCode();
    }

    public KeyStroker(KeyEvent ke) {


        this.keyCode = ke.getKeyCode();
        this.isShiftDown = ke.isShiftDown();
        this.isControlDown = ke.isControlDown();
        this.isAltDown = ke.isAltDown();
        this.isAltGrDown = ke.isAltGraphDown();
        this.location = ke.getKeyLocation();

        hashCode = computeHashCode();

    }

    public KeyStroker(KeyEvent ke, boolean isAltGrDown) {


        this.keyCode = ke.getKeyCode();
        this.isShiftDown = ke.isShiftDown();
        this.isControlDown = ke.isControlDown();
        this.isAltDown = ke.isAltDown();
        this.isAltGrDown = isAltGrDown;
        this.location = ke.getKeyLocation();

        hashCode = computeHashCode();

    }

    /**
     * Create a KeyStroker with explicit key attributes (headless-compatible).
     * @param keyCode the key code
     * @param isShiftDown whether shift is down
     * @param isControlDown whether control is down
     * @param isAltDown whether alt is down
     */
    public KeyStroker(int keyCode,
                      boolean isShiftDown,
                      boolean isControlDown,
                      boolean isAltDown) {

        this.keyCode = keyCode;
        this.isShiftDown = isShiftDown;
        this.isControlDown = isControlDown;
        this.isAltDown = isAltDown;
        this.isAltGrDown = false;
        this.location = KEY_LOCATION_UNKNOWN;

        hashCode = computeHashCode();
    }

    public KeyStroker(int keyCode,
                      boolean isShiftDown,
                      boolean isControlDown,
                      boolean isAltDown,
                      boolean isAltGrDown,
                      int location) {

        this.keyCode = keyCode;
        this.isShiftDown = isShiftDown;
        this.isControlDown = isControlDown;
        this.isAltDown = isAltDown;
        this.isAltGrDown = isAltGrDown;
        this.location = location;

        hashCode = computeHashCode();
    }

    /**
     * Set attributes from an IKeyEvent (headless-compatible).
     * @param ke the key event
     */
    public void setAttributes(IKeyEvent ke) {
        keyCode = ke.getKeyCode();
        isShiftDown = ke.isShiftDown();
        isControlDown = ke.isControlDown();
        isAltDown = ke.isAltDown();
        isAltGrDown = ke.isAltGraphDown();
        location = ke.getKeyLocation();

        hashCode = computeHashCode();
    }

    /**
     * Set attributes from an IKeyEvent (headless-compatible).
     * @param ke the key event
     * @param isAltGr whether AltGraph is down
     */
    public void setAttributes(IKeyEvent ke, boolean isAltGr) {
        keyCode = ke.getKeyCode();
        isShiftDown = ke.isShiftDown();
        isControlDown = ke.isControlDown();
        isAltDown = ke.isAltDown();
        isAltGrDown = isAltGr;
        location = ke.getKeyLocation();

        hashCode = computeHashCode();
    }

    public void setAttributes(KeyEvent ke, boolean isAltGr) {

        keyCode = ke.getKeyCode();
        isShiftDown = ke.isShiftDown();
        isControlDown = ke.isControlDown();
        isAltDown = ke.isAltDown();
        isAltGrDown = isAltGr;
        location = ke.getKeyLocation();

        hashCode = computeHashCode();
    }

    /**
     * Compute hash code using bit-shifting to avoid collisions.
     * Layout: keyCode (bits 0-15) | shift (bit 16) | control (bit 17) |
     *         alt (bit 18) | altGr (bit 19) | location (bits 20-23)
     * @return computed hash code
     */
    private int computeHashCode() {
        return keyCode
             | (isShiftDown ? (1 << 16) : 0)
             | (isControlDown ? (1 << 17) : 0)
             | (isAltDown ? (1 << 18) : 0)
             | (isAltGrDown ? (1 << 19) : 0)
             | ((location & 0xF) << 20);
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean isShiftDown() {

        return isShiftDown;
    }

    public boolean isControlDown() {

        return isControlDown;
    }

    public boolean isAltDown() {

        return isAltDown;
    }

    public boolean isAltGrDown() {

        return isAltGrDown;
    }

    public int getLocation() {
        return location;
    }

    /**
     * Check equality with an IKeyEvent (headless-compatible).
     * @param ke the key event to compare
     * @return true if equal
     */
    public boolean equals(IKeyEvent ke) {
        return (keyCode == ke.getKeyCode() &&
                isShiftDown == ke.isShiftDown() &&
                isControlDown == ke.isControlDown() &&
                isAltDown == ke.isAltDown() &&
                isAltGrDown == ke.isAltGraphDown() &&
                location == ke.getKeyLocation());
    }

    /**
     * Check equality with an IKeyEvent and explicit AltGraph state.
     * @param ke the key event to compare
     * @param altGrDown the AltGraph state to compare
     * @return true if equal
     */
    public boolean equals(IKeyEvent ke, boolean altGrDown) {
        return (keyCode == ke.getKeyCode() &&
                isShiftDown == ke.isShiftDown() &&
                isControlDown == ke.isControlDown() &&
                isAltDown == ke.isAltDown() &&
                isAltGrDown == altGrDown &&
                location == ke.getKeyLocation());
    }

    public boolean equals(KeyEvent ke) {

        return (keyCode == ke.getKeyCode() &&
                isShiftDown == ke.isShiftDown() &&
                isControlDown == ke.isControlDown() &&
                isAltDown == ke.isAltDown() &&
                isAltGrDown == ke.isAltGraphDown() &&
                location == ke.getKeyLocation());
    }

    public boolean equals(Object obj) {
        if (obj instanceof KeyStroker) {
            KeyStroker ks = (KeyStroker) obj;

            return ks.keyCode == keyCode &&
                    ks.isShiftDown == isShiftDown &&
                    ks.isControlDown == isControlDown &&
                    ks.isAltDown == isAltDown &&
                    ks.isAltGrDown == isAltGrDown &&
                    ks.location == location;
        }
        return false;
    }

    public boolean equals(KeyEvent ke, boolean altGrDown) {

        return (keyCode == ke.getKeyCode() &&
                isShiftDown == ke.isShiftDown() &&
                isControlDown == ke.isControlDown() &&
                isAltDown == ke.isAltDown() &&
                isAltGrDown == altGrDown &&
                location == ke.getKeyLocation());
    }

    public boolean equals(Object obj, boolean altGrDown) {
        KeyStroker ks = (KeyStroker) obj;

        return ks.keyCode == keyCode &&
                ks.isShiftDown == isShiftDown &&
                ks.isControlDown == isControlDown &&
                ks.isAltDown == isAltDown &&
                ks.isAltGrDown == altGrDown &&
                ks.location == location;
    }

    public String toString() {

        return new String(keyCode + "," +
                (isShiftDown ? "true" : "false") + "," +
                (isControlDown ? "true" : "false") + "," +
                (isAltDown ? "true" : "false") + "," +
                (isAltGrDown ? "true" : "false") + "," +
                location);
    }

    public String getKeyStrokeDesc() {

        return (isShiftDown ? "Shift + " : "") +
                (isControlDown ? "Ctrl + " : "") +
                (isAltDown ? "Alt + " : "") +
                (isAltGrDown ? "Alt-Gr + " : "") +
                KeyCodes.getKeyText(keyCode);
    }

    public int getKeyCode() {
        return keyCode;
    }

}
