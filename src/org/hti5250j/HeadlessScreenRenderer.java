/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;

import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

/**
 * Stateless renderer for generating BufferedImage screenshots of Screen5250
 * without requiring persistent GUI components.
 *
 * This class extracts rendering logic from GuiGraphicBuffer to enable
 * screenshot generation in pure headless mode with minimal memory overhead.
 *
 * Usage:
 * <pre>
 *   BufferedImage screenshot = HeadlessScreenRenderer.renderScreen(
 *       session.getScreen(),
 *       session.getConfiguration()
 *   );
 *   ImageIO.write(screenshot, "PNG", new File("output.png"));
 * </pre>
 */
public class HeadlessScreenRenderer {

    private static final HTI5250jLogger log = HTI5250jLogFactory.getLogger("RENDERER");

    // Color palette for 5250 protocol
    private static final class ColorPalette {
        Color colorBlue;
        Color colorWhite;
        Color colorRed;
        Color colorGreen;
        Color colorPink;
        Color colorYellow;
        Color colorTurq;
        Color colorBg;
        Color colorHexAttr;

        ColorPalette(SessionConfig config) {
            // Default colors matching GuiGraphicBuffer.loadColors()
            colorBlue = new Color(140, 120, 255);
            colorTurq = new Color(0, 240, 255);
            colorRed = Color.red;
            colorWhite = Color.white;
            colorYellow = Color.yellow;
            colorGreen = Color.green;
            colorPink = Color.magenta;
            colorHexAttr = Color.white;
            colorBg = Color.black;

            // Override with config if present
            if (config != null) {
                if (config.isPropertyExists("colorBg")) {
                    colorBg = config.getColorProperty("colorBg");
                }
                if (config.isPropertyExists("colorBlue")) {
                    colorBlue = config.getColorProperty("colorBlue");
                }
                if (config.isPropertyExists("colorTurq")) {
                    colorTurq = config.getColorProperty("colorTurq");
                }
                if (config.isPropertyExists("colorRed")) {
                    colorRed = config.getColorProperty("colorRed");
                }
                if (config.isPropertyExists("colorWhite")) {
                    colorWhite = config.getColorProperty("colorWhite");
                }
                if (config.isPropertyExists("colorYellow")) {
                    colorYellow = config.getColorProperty("colorYellow");
                }
                if (config.isPropertyExists("colorGreen")) {
                    colorGreen = config.getColorProperty("colorGreen");
                }
                if (config.isPropertyExists("colorPink")) {
                    colorPink = config.getColorProperty("colorPink");
                }
                if (config.isPropertyExists("colorHexAttr")) {
                    colorHexAttr = config.getColorProperty("colorHexAttr");
                }
            }
        }
    }

    /**
     * Render a Screen5250 to a BufferedImage without requiring persistent GUI components.
     *
     * @param screen The screen data model to render
     * @param config Session configuration containing fonts and colors
     * @return BufferedImage containing the rendered screen
     * @throws IllegalArgumentException if screen or config is null
     */
    public static BufferedImage renderScreen(Screen5250 screen, SessionConfig config) {
        if (screen == null) {
            throw new IllegalArgumentException("Screen5250 cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("SessionConfig cannot be null");
        }

        try {
            // Initialize font from config
            Font font = initializeFont(config);
            FontRenderContext frc = new FontRenderContext(font.getTransform(), true, true);
            LineMetrics lm = font.getLineMetrics("Wy", frc);

            // Calculate character dimensions
            int columnWidth = (int) font.getStringBounds("W", frc).getWidth() + 1;
            int rowHeight = (int) (font.getStringBounds("g", frc).getHeight()
                    + lm.getDescent() + lm.getLeading());

            // Initialize color palette
            ColorPalette colors = new ColorPalette(config);

            // Create BufferedImage with proper dimensions
            int imageWidth = columnWidth * screen.getColumns();
            int imageHeight = rowHeight * (screen.getRows() + 2); // +2 for status area
            BufferedImage bi = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

            // Render screen content
            renderScreenContent(bi, screen, config, font, lm, columnWidth, rowHeight, colors);

            return bi;
        } catch (Exception e) {
            log.error("Screen rendering failed: " + e.getMessage());
            throw new RuntimeException("Failed to render screen: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize font from session configuration.
     * Extracts logic from GuiGraphicBuffer.java lines 139-164
     */
    private static Font initializeFont(SessionConfig config) {
        String fontName = "Monospaced";

        // Try to load font from config
        if (config.isPropertyExists("font")) {
            String configFont = config.getStringProperty("font");
            if (configFont != null && !configFont.isEmpty()) {
                fontName = configFont;
            }
        }

        return new Font(fontName, Font.PLAIN, 14);
    }

    /**
     * Render screen characters and attributes to BufferedImage.
     * Core rendering logic extracted from GuiGraphicBuffer.drawOIA() and drawChar()
     */
    private static void renderScreenContent(BufferedImage bi, Screen5250 screen,
            SessionConfig config, Font font, LineMetrics lm,
            int columnWidth, int rowHeight, ColorPalette colors) {

        Graphics2D g2d = bi.createGraphics();

        try {
            // Set rendering hints for quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setFont(font);

            // Fill background
            g2d.setColor(colors.colorBg);
            g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());

            // Get screen data for rendering
            int screenLength = screen.getScreenLength();
            char[] textChars = new char[screenLength];
            char[] attrChars = new char[screenLength];
            char[] colorChars = new char[screenLength];
            char[] extendedChars = new char[screenLength];

            screen.GetScreen(textChars, screenLength, HTI5250jConstants.PLANE_TEXT);
            screen.GetScreen(attrChars, screenLength, HTI5250jConstants.PLANE_ATTR);
            screen.GetScreen(colorChars, screenLength, HTI5250jConstants.PLANE_COLOR);
            screen.GetScreen(extendedChars, screenLength, HTI5250jConstants.PLANE_EXTENDED);

            // Render each character
            for (int pos = 0; pos < screenLength; pos++) {
                int row = screen.getRow(pos);
                int col = screen.getCol(pos);

                renderCharacter(g2d, pos, row, col, columnWidth, rowHeight, lm,
                        textChars, attrChars, colorChars, extendedChars, colors);
            }
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Render a single character position.
     * Simplified version of GuiGraphicBuffer.drawChar()
     */
    private static void renderCharacter(Graphics2D graphics, int pos, int row, int col,
            int columnWidth, int rowHeight, LineMetrics lm,
            char[] textChars, char[] attrChars, char[] colorChars, char[] extendedChars,
            ColorPalette colors) {

        if (pos >= textChars.length) {
            return;
        }

        char charToRender = textChars[pos];
        char colorAttr = colorChars[pos];
        char extAttr = extendedChars[pos];

        // Calculate position
        int x = col * columnWidth;
        int y = row * rowHeight;
        int cy = (int) (y + rowHeight - (lm.getDescent() + lm.getLeading()));

        // Get foreground and background colors
        Color fg = getColorFromAttribute(colorAttr, false, colors);
        Color bg = getColorFromAttribute(colorAttr, true, colors);

        // Render background
        graphics.setColor(bg);
        graphics.fillRect(x, y, columnWidth, rowHeight);

        // Check for non-display
        boolean isAttrPlace = (attrChars[pos] & 0xFF) != 0;
        boolean nonDisplay = (extAttr & HTI5250jConstants.EXTENDED_5250_NON_DSP) != 0;

        // Render character (if not attribute place and not non-display)
        if (!nonDisplay && !isAttrPlace && charToRender != 0) {
            graphics.setColor(fg);
            try {
                graphics.drawChars(new char[]{charToRender}, 0, 1, x, cy - 2);
            } catch (Exception e) {
                log.debug("Character render skipped: " + e.getMessage());
            }
        }

        // Render underline if present
        boolean underline = (extAttr & HTI5250jConstants.EXTENDED_5250_UNDERLINE) != 0;
        if (underline && !isAttrPlace && !nonDisplay) {
            graphics.setColor(fg);
            graphics.drawLine(x, (int) (y + (rowHeight - (lm.getLeading() + lm.getDescent()))),
                    (x + columnWidth), (int) (y + (rowHeight - (lm.getLeading() + lm.getDescent()))));
        }
    }

    /**
     * Map 5250 color attribute byte to Color object.
     * Extracted from GuiGraphicBuffer.getColor()
     */
    private static Color getColorFromAttribute(char colorAttr, boolean background, ColorPalette colors) {
        int colorValue;
        if (background) {
            colorValue = (colorAttr & 0xff00) >> 8;
        } else {
            colorValue = colorAttr & 0x00ff;
        }

        return switch (colorValue) {
            case HTI5250jConstants.COLOR_FG_BLACK -> colors.colorBg;
            case HTI5250jConstants.COLOR_FG_GREEN -> colors.colorGreen;
            case HTI5250jConstants.COLOR_FG_BLUE -> colors.colorBlue;
            case HTI5250jConstants.COLOR_FG_RED -> colors.colorRed;
            case HTI5250jConstants.COLOR_FG_YELLOW -> colors.colorYellow;
            case HTI5250jConstants.COLOR_FG_CYAN -> colors.colorTurq;
            case HTI5250jConstants.COLOR_FG_WHITE -> colors.colorWhite;
            case HTI5250jConstants.COLOR_FG_MAGENTA -> colors.colorPink;
            default -> Color.orange;
        };
    }
}
