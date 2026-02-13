/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui.adapters;

import org.hti5250j.interfaces.IKeyEvent;
import java.awt.event.KeyEvent;

/**
 * Adapter that wraps java.awt.event.KeyEvent to implement IKeyEvent.
 * Used in GUI mode for backward compatibility.
 */
public class AwtKeyEventAdapter implements IKeyEvent {

    private final KeyEvent awtEvent;

    public AwtKeyEventAdapter(KeyEvent awtEvent) {
        this.awtEvent = awtEvent;
    }

    /**
     * Get the underlying AWT KeyEvent (for backward compatibility).
     * @return the wrapped KeyEvent
     */
    public KeyEvent getAwtEvent() {
        return awtEvent;
    }

    @Override
    public int getKeyCode() {
        return awtEvent.getKeyCode();
    }

    @Override
    public boolean isShiftDown() {
        return awtEvent.isShiftDown();
    }

    @Override
    public boolean isControlDown() {
        return awtEvent.isControlDown();
    }

    @Override
    public boolean isAltDown() {
        return awtEvent.isAltDown();
    }

    @Override
    public boolean isAltGraphDown() {
        return awtEvent.isAltGraphDown();
    }

    @Override
    public int getKeyLocation() {
        return awtEvent.getKeyLocation();
    }

    @Override
    public char getKeyChar() {
        return awtEvent.getKeyChar();
    }

    @Override
    public void consume() {
        awtEvent.consume();
    }

    @Override
    public boolean isConsumed() {
        return awtEvent.isConsumed();
    }
}
