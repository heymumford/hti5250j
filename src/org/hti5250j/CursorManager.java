/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j;

import java.awt.geom.Rectangle2D;
import javax.swing.Timer;

/**
 * Cursor position, visibility, and blink state management.
 *
 * Extracted from GuiGraphicBuffer to follow Single Responsibility Principle.
 * Encapsulates all cursor-related state and operations including position tracking,
 * visibility control, blink state management, and cursor size settings.
 *
 */
public class CursorManager {

    private int cursorX = 0;
    private int cursorY = 0;
    private boolean cursorVisible = true;
    private boolean blinkState = false;
    private int cursorSize = 0; // 0=Line, 1=Half, 2=Full
    private int cursorBottOffset = 0;
    private Rectangle2D cursor = new Rectangle2D.Float();
    private Timer blinker;

    /**
     * Sets the cursor position (column and row).
     *
     * @param x the column position
     * @param y the row position
     */
    public void setCursorPosition(int x, int y) {
        this.cursorX = x;
        this.cursorY = y;
    }

    /**
     * Gets the cursor X position (column).
     *
     * @return the column position
     */
    public int getCursorX() {
        return cursorX;
    }

    /**
     * Gets the cursor Y position (row).
     *
     * @return the row position
     */
    public int getCursorY() {
        return cursorY;
    }

    /**
     * Sets the cursor visibility state.
     *
     * @param visible true to show cursor, false to hide
     */
    public void setCursorVisible(boolean visible) {
        this.cursorVisible = visible;
    }

    /**
     * Gets the cursor visibility state.
     *
     * @return true if cursor is visible, false otherwise
     */
    public boolean isCursorVisible() {
        return cursorVisible;
    }

    /**
     * Toggles the cursor blink state.
     */
    public void toggleBlink() {
        blinkState = !blinkState;
    }

    /**
     * Gets the current blink state.
     *
     * @return true if blinking, false otherwise
     */
    public boolean getBlinkState() {
        return blinkState;
    }

    /**
     * Sets the cursor blink state explicitly.
     *
     * @param blinking true to enable blinking, false to disable
     */
    public void setBlinkState(boolean blinking) {
        this.blinkState = blinking;
    }

    /**
     * Sets the cursor size.
     *
     * @param size 0 for line, 1 for half, 2 for full
     */
    public void setCursorSize(int size) {
        if (size >= 0 && size <= 2) {
            this.cursorSize = size;
        }
    }

    /**
     * Gets the cursor size setting.
     *
     * @return 0 for line, 1 for half, 2 for full
     */
    public int getCursorSize() {
        return cursorSize;
    }

    /**
     * Sets the cursor bottom offset (vertical adjustment).
     *
     * @param offset the offset in pixels
     */
    public void setCursorBottOffset(int offset) {
        this.cursorBottOffset = offset;
    }

    /**
     * Gets the cursor bottom offset.
     *
     * @return the offset in pixels
     */
    public int getCursorBottOffset() {
        return cursorBottOffset;
    }

    /**
     * Gets the cursor shape rectangle.
     *
     * @return the Rectangle2D representing cursor bounds
     */
    public Rectangle2D getCursorBounds() {
        return cursor;
    }

    /**
     * Updates the cursor shape rectangle.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width
     * @param height the height
     */
    public void setCursorBounds(double x, double y, double width, double height) {
        cursor.setRect(x, y, width, height);
    }

    /**
     * Sets the blink timer.
     *
     * @param timer the Timer for cursor blinking, or null to disable
     */
    public void setBlinker(Timer timer) {
        this.blinker = timer;
    }

    /**
     * Gets the blink timer.
     *
     * @return the Timer, or null if blinking is disabled
     */
    public Timer getBlinker() {
        return blinker;
    }

    /**
     * Checks if blinking is enabled.
     *
     * @return true if blinker is active, false otherwise
     */
    public boolean isBlinkEnabled() {
        return blinker != null;
    }

    /**
     * Clears all cursor state and stops blinking.
     */
    public void reset() {
        cursorX = 0;
        cursorY = 0;
        cursorVisible = true;
        blinkState = false;
        cursorSize = 0;
        cursorBottOffset = 0;
        if (blinker != null) {
            blinker.stop();
            blinker = null;
        }
        cursor.setRect(0, 0, 0, 0);
    }
}
