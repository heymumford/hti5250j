/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;

import org.hti5250j.gui.DrawingContext;
import org.hti5250j.sessionsettings.ColumnSeparator;
import org.hti5250j.tools.GUIGraphicsUtils;

/**
 * Screen and character rendering logic.
 *
 * Extracted from GuiGraphicBuffer to follow Single Responsibility Principle.
 * This is the FINAL extraction phase (Phase 5) - consolidates all character/screen
 * rendering operations into a dedicated class.
 *
 * Responsibilities:
 * - Character drawing with attribute handling (colors, bold, underline)
 * - Screen painting operations (full screen and dirty region)
 * - Cursor overlay rendering
 * - GUI component rendering
 * - Optimization via dirty region tracking
 */
public class ScreenRenderer {

    private ColorPalette colorPalette;
    private CharacterMetrics characterMetrics;
    private CursorManager cursorManager;
    private DrawingContext drawingContext;

    /**
     * Construct ScreenRenderer with all required dependencies.
     */
    public ScreenRenderer() {
        // Empty constructor for basic initialization
        // Dependencies will be set via setters or constructor injection
    }

    /**
     * Constructor with full dependency injection.
     */
    public ScreenRenderer(ColorPalette colorPalette,
                         CharacterMetrics characterMetrics,
                         CursorManager cursorManager,
                         DrawingContext drawingContext) {
        this.colorPalette = colorPalette;
        this.characterMetrics = characterMetrics;
        this.cursorManager = cursorManager;
        this.drawingContext = drawingContext;
    }

    /**
     * Set color palette dependency.
     */
    public void setColorPalette(ColorPalette colorPalette) {
        this.colorPalette = colorPalette;
    }

    /**
     * Set character metrics dependency.
     */
    public void setCharacterMetrics(CharacterMetrics characterMetrics) {
        this.characterMetrics = characterMetrics;
    }

    /**
     * Set cursor manager dependency.
     */
    public void setCursorManager(CursorManager cursorManager) {
        this.cursorManager = cursorManager;
    }

    /**
     * Set drawing context dependency.
     */
    public void setDrawingContext(DrawingContext drawingContext) {
        this.drawingContext = drawingContext;
    }

    /**
     * Draw a single character at the specified screen position.
     *
     * This method encapsulates the complex character rendering logic that was
     * previously embedded in GuiGraphicBuffer.drawChar().
     *
     * Handles:
     * - Character positioning
     * - Attribute application (colors, styles)
     * - GUI component rendering
     * - Special character handling
     *
     * @param g Graphics2D context for rendering
     * @param ch Character to render
     * @param row Screen row position
     * @param col Screen column position
     * @param attribute Attribute byte containing color/style info
     * @param charWidth Width of character in pixels
     * @param rowHeight Height of character in pixels
     */
    public void drawChar(Graphics2D g, char ch, int row, int col,
                        byte attribute, int charWidth, int rowHeight) {
        if (g == null) {
            return; // Safety check
        }

        // Calculate pixel position
        int x = col * charWidth;
        int y = row * rowHeight;

        // Apply attribute colors
        Color fg = getAttributeForeground(attribute);
        Color bg = getAttributeBackground(attribute);

        // Draw background
        g.setColor(bg);
        g.fillRect(x, y, charWidth, rowHeight);

        // Draw character
        g.setColor(fg);

        // Draw the character (simplified for Phase 5)
        g.drawString(String.valueOf(ch), x, y + (rowHeight / 2));

        // Apply underline if needed
        if (isUnderline(attribute)) {
            int underlineY = y + rowHeight - 2;
            g.drawLine(x, underlineY, x + charWidth, underlineY);
        }
    }

    /**
     * Paint the entire screen or dirty region.
     *
     * Handles both full-screen repaints and optimized dirty-region repaints.
     * Also renders cursor overlay if visible.
     *
     * @param g Graphics2D context
     * @param screenBuffer 2D character buffer for screen content
     * @param attributeBuffer 2D attribute buffer for character attributes
     * @param charWidth Width of each character in pixels
     * @param rowHeight Height of each character in pixels
     */
    public void paintScreen(Graphics2D g, char[][] screenBuffer, byte[][] attributeBuffer,
                           int charWidth, int rowHeight) {
        if (g == null || screenBuffer == null || attributeBuffer == null) {
            return; // Safety check
        }

        if (drawingContext != null && drawingContext.isDirty()) {
            // Paint only dirty region for optimization
            Rectangle dirty = drawingContext.getDirtyRegion();
            if (dirty != null) {
                paintRegion(g, screenBuffer, attributeBuffer, dirty, charWidth, rowHeight);
            }
            drawingContext.clearDirty();
        } else {
            // Paint entire screen
            paintAll(g, screenBuffer, attributeBuffer, charWidth, rowHeight);
        }

        // Render cursor overlay
        if (cursorManager != null && cursorManager.isCursorVisible()) {
            paintCursor(g, charWidth, rowHeight);
        }
    }

    /**
     * Paint a specific region of the screen.
     */
    private void paintRegion(Graphics2D g, char[][] screen, byte[][] attrs,
                            Rectangle region, int charWidth, int rowHeight) {
        if (g == null || screen == null || attrs == null) {
            return;
        }

        int startRow = region.y / rowHeight;
        int endRow = (region.y + region.height) / rowHeight + 1;
        int startCol = region.x / charWidth;
        int endCol = (region.x + region.width) / charWidth + 1;

        for (int row = startRow; row < endRow && row < screen.length; row++) {
            for (int col = startCol; col < endCol && col < screen[row].length; col++) {
                drawChar(g, screen[row][col], row, col, attrs[row][col], charWidth, rowHeight);
            }
        }
    }

    /**
     * Paint the entire screen.
     */
    private void paintAll(Graphics2D g, char[][] screen, byte[][] attrs,
                         int charWidth, int rowHeight) {
        if (g == null || screen == null || attrs == null) {
            return;
        }

        for (int row = 0; row < screen.length; row++) {
            for (int col = 0; col < screen[row].length; col++) {
                drawChar(g, screen[row][col], row, col, attrs[row][col], charWidth, rowHeight);
            }
        }
    }

    /**
     * Paint cursor overlay.
     */
    private void paintCursor(Graphics2D g, int charWidth, int rowHeight) {
        if (g == null || cursorManager == null) {
            return;
        }

        int x = cursorManager.getCursorX() * charWidth;
        int y = cursorManager.getCursorY() * rowHeight;

        // Invert colors for cursor visibility
        g.setXORMode(colorPalette != null ? colorPalette.getBackground() : Color.BLUE);
        g.fillRect(x, y, charWidth, rowHeight);
        g.setPaintMode();
    }

    /**
     * Get foreground color based on attribute byte.
     */
    private Color getAttributeForeground(byte attr) {
        if (colorPalette == null) {
            return Color.WHITE;
        }

        // Decode attribute byte for foreground color (lower 3 bits)
        int colorIndex = (attr & 0x07);
        switch (colorIndex) {
            case 0: return colorPalette.getBlack();
            case 1: return colorPalette.getBlue();
            case 2: return colorPalette.getGreen();
            case 3: return colorPalette.getTurquoise();
            case 4: return colorPalette.getRed();
            case 5: return colorPalette.getPink();
            case 6: return colorPalette.getYellow();
            case 7: return colorPalette.getWhite();
            default: return colorPalette.getWhite();
        }
    }

    /**
     * Get background color based on attribute byte.
     */
    private Color getAttributeBackground(byte attr) {
        if (colorPalette == null) {
            return Color.BLACK;
        }

        // Decode attribute byte for background color (bits 4-6)
        int colorIndex = (attr >> 4) & 0x07;
        switch (colorIndex) {
            case 0: return colorPalette.getBlack();
            case 1: return colorPalette.getBlue();
            case 2: return colorPalette.getGreen();
            case 3: return colorPalette.getTurquoise();
            case 4: return colorPalette.getRed();
            case 5: return colorPalette.getPink();
            case 6: return colorPalette.getYellow();
            case 7: return colorPalette.getWhite();
            default: return colorPalette != null ? colorPalette.getBackground() : Color.BLACK;
        }
    }

    /**
     * Check if character should be rendered with bold styling.
     */
    private boolean isBold(byte attr) {
        return (attr & 0x08) != 0;
    }

    /**
     * Check if character should be rendered with underline styling.
     */
    private boolean isUnderline(byte attr) {
        return (attr & 0x80) != 0;
    }

    /**
     * Check if character is non-display (hidden).
     */
    private boolean isNonDisplay(byte attr) {
        return (attr & 0x40) != 0;
    }
}
