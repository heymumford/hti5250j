/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.headless;

import org.hti5250j.interfaces.IKeyEvent;
import org.hti5250j.keyboard.KeyCodes;

/**
 * Headless implementation of IKeyEvent for programmatic key injection.
 * Used in server deployments and automated testing without GUI.
 */
public class HeadlessKeyEvent implements IKeyEvent {

    private final int keyCode;
    private final boolean shiftDown;
    private final boolean controlDown;
    private final boolean altDown;
    private final boolean altGraphDown;
    private final int keyLocation;
    private final char keyChar;
    private boolean consumed;

    /**
     * Create a headless key event.
     * @param keyCode the key code
     */
    public HeadlessKeyEvent(int keyCode) {
        this(keyCode, false, false, false, false, KeyCodes.KEY_LOCATION_STANDARD, (char) 0);
    }

    /**
     * Create a headless key event with modifiers.
     * @param keyCode the key code
     * @param shiftDown true if Shift is down
     * @param controlDown true if Control is down
     * @param altDown true if Alt is down
     */
    public HeadlessKeyEvent(int keyCode, boolean shiftDown, boolean controlDown, boolean altDown) {
        this(keyCode, shiftDown, controlDown, altDown, false, KeyCodes.KEY_LOCATION_STANDARD, (char) 0);
    }

    /**
     * Create a headless key event with all parameters.
     * @param keyCode the key code
     * @param shiftDown true if Shift is down
     * @param controlDown true if Control is down
     * @param altDown true if Alt is down
     * @param altGraphDown true if AltGraph is down
     * @param keyLocation the key location
     * @param keyChar the character associated with the key
     */
    public HeadlessKeyEvent(int keyCode, boolean shiftDown, boolean controlDown,
                            boolean altDown, boolean altGraphDown, int keyLocation, char keyChar) {
        this.keyCode = keyCode;
        this.shiftDown = shiftDown;
        this.controlDown = controlDown;
        this.altDown = altDown;
        this.altGraphDown = altGraphDown;
        this.keyLocation = keyLocation;
        this.keyChar = keyChar;
        this.consumed = false;
    }

    @Override
    public int getKeyCode() {
        return keyCode;
    }

    @Override
    public boolean isShiftDown() {
        return shiftDown;
    }

    @Override
    public boolean isControlDown() {
        return controlDown;
    }

    @Override
    public boolean isAltDown() {
        return altDown;
    }

    @Override
    public boolean isAltGraphDown() {
        return altGraphDown;
    }

    @Override
    public int getKeyLocation() {
        return keyLocation;
    }

    @Override
    public char getKeyChar() {
        return keyChar;
    }

    @Override
    public void consume() {
        this.consumed = true;
    }

    @Override
    public boolean isConsumed() {
        return consumed;
    }

    @Override
    public String toString() {
        return "HeadlessKeyEvent[keyCode=" + keyCode +
                ", shift=" + shiftDown +
                ", ctrl=" + controlDown +
                ", alt=" + altDown +
                ", altGr=" + altGraphDown +
                ", location=" + keyLocation +
                ", char='" + keyChar + "']";
    }
}
