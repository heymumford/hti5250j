/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

/**
 * Character and font metrics calculations.
 *
 * Extracted from GuiGraphicBuffer to follow Single Responsibility Principle.
 * Encapsulates all font-related measurements including character width and height,
 * font scaling, and line metrics caching.
 *
 * (Named CharacterMetrics to avoid collision with java.awt.FontMetrics)
 */
public class CharacterMetrics {

    private Font currentFont;
    private int charWidth;
    private int charHeight;
    private java.awt.FontMetrics awtMetrics;
    private LineMetrics lineMetrics;
    private FontRenderContext renderContext;

    /**
     * Sets the font and recalculates all metrics.
     *
     * @param font the Font to measure
     */
    public void setFont(Font font) {
        this.currentFont = font;
        if (font != null) {
            calculateMetrics();
        }
    }

    /**
     * Calculates and caches all font metrics for the current font.
     * Uses FontRenderContext to get accurate measurements with antialiasing hints.
     */
    private void calculateMetrics() {
        if (currentFont == null) {
            return;
        }

        renderContext = new FontRenderContext(currentFont.getTransform(), true, true);
        lineMetrics = currentFont.getLineMetrics("Wy", renderContext);
        charWidth = (int) currentFont.getStringBounds("W", renderContext).getWidth() + 1;
        charHeight = (int) (currentFont.getStringBounds("g", renderContext).getHeight()
                + lineMetrics.getDescent() + lineMetrics.getLeading());
    }

    /**
     * Gets the calculated character width in pixels.
     *
     * @return the width of a single character in pixels
     */
    public int getCharWidth() {
        return charWidth;
    }

    /**
     * Gets the calculated character height in pixels.
     *
     * @return the height of a single character in pixels
     */
    public int getCharHeight() {
        return charHeight;
    }

    /**
     * Gets the current Font being measured.
     *
     * @return the Font object, or null if not set
     */
    public Font getFont() {
        return currentFont;
    }

    /**
     * Gets the cached line metrics for the current font.
     *
     * @return the LineMetrics object, or null if not set
     */
    public LineMetrics getLineMetrics() {
        return lineMetrics;
    }

    /**
     * Gets the leading (vertical space above the text baseline).
     *
     * @return the leading in pixels
     */
    public float getLeading() {
        if (lineMetrics != null) {
            return lineMetrics.getLeading();
        }
        return 0;
    }

    /**
     * Gets the descent (vertical space below the baseline).
     *
     * @return the descent in pixels
     */
    public float getDescent() {
        if (lineMetrics != null) {
            return lineMetrics.getDescent();
        }
        return 0;
    }

    /**
     * Gets the ascent (vertical space above the baseline).
     *
     * @return the ascent in pixels
     */
    public float getAscent() {
        if (lineMetrics != null) {
            return lineMetrics.getAscent();
        }
        return 0;
    }
}
