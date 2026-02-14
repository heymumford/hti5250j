/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.awt.Color;

/**
 * GREEN Phase - Tests for ColorPalette class.
 *
 * Verifies the ColorPalette API: default colors, setters, foreground/background
 * color lookups by constant, and GUI interface mode flag.
 */
@DisplayName("ColorPalette Tests")
public class ColorPaletteTest {

    private ColorPalette palette;

    @BeforeEach
    void setUp() {
        palette = new ColorPalette();
    }

    @Test
    @DisplayName("ColorPalette provides default blue color")
    void testGetBlueColor() {
        Color blue = palette.getBlue();
        assertNotNull(blue, "Blue color should not be null");
        assertEquals(new Color(140, 120, 255), blue, "Blue should match default RGB");
    }

    @Test
    @DisplayName("ColorPalette provides default white color")
    void testGetWhiteColor() {
        Color white = palette.getWhite();
        assertNotNull(white, "White color should not be null");
        assertEquals(Color.white, white, "White should match Color.white");
    }

    @Test
    @DisplayName("ColorPalette provides default red color")
    void testGetRedColor() {
        Color red = palette.getRed();
        assertNotNull(red, "Red color should not be null");
        assertEquals(Color.red, red, "Red should match Color.red");
    }

    @Test
    @DisplayName("ColorPalette provides default green color")
    void testGetGreenColor() {
        Color green = palette.getGreen();
        assertNotNull(green, "Green color should not be null");
        assertEquals(Color.green, green, "Green should match Color.green");
    }

    @Test
    @DisplayName("ColorPalette provides default yellow color")
    void testGetYellowColor() {
        Color yellow = palette.getYellow();
        assertNotNull(yellow, "Yellow color should not be null");
        assertEquals(Color.yellow, yellow, "Yellow should match Color.yellow");
    }

    @Test
    @DisplayName("ColorPalette provides default pink color")
    void testGetPinkColor() {
        Color pink = palette.getPink();
        assertNotNull(pink, "Pink color should not be null");
        assertEquals(new Color(255, 192, 203), pink, "Pink should match RGB(255,192,203)");
    }

    @Test
    @DisplayName("ColorPalette provides default turquoise color")
    void testGetTurquoiseColor() {
        Color turq = palette.getTurquoise();
        assertNotNull(turq, "Turquoise color should not be null");
        assertEquals(new Color(0, 255, 255), turq, "Turquoise should match RGB(0,255,255)");
    }

    @Test
    @DisplayName("ColorPalette provides default background color (dark blue)")
    void testGetBackgroundColor() {
        Color bg = palette.getBackground();
        assertNotNull(bg, "Background color should not be null");
        assertEquals(new Color(0, 0, 128), bg, "Default background should be dark blue");
    }

    @Test
    @DisplayName("setGuiInterface sets the GUI mode flag")
    void testSetGuiInterfaceFlag() {
        assertFalse(palette.isGuiInterface(), "GUI interface should default to false");
        palette.setGuiInterface(true);
        assertTrue(palette.isGuiInterface(), "GUI interface should be true after setting");
    }

    @Test
    @DisplayName("ColorPalette provides default cursor color (yellow)")
    void testGetCursorColor() {
        Color cursor = palette.getCursor();
        assertNotNull(cursor, "Cursor color should not be null");
        assertEquals(new Color(255, 255, 0), cursor, "Cursor should default to yellow");
    }

    @Test
    @DisplayName("ColorPalette provides default GUI field color (dark blue)")
    void testGetGuiFieldColor() {
        Color guiField = palette.getGuiField();
        assertNotNull(guiField, "GUI field color should not be null");
        assertEquals(new Color(0, 0, 128), guiField, "GUI field should default to dark blue");
    }

    @Test
    @DisplayName("ColorPalette provides default separator color (gray)")
    void testGetSeparatorColor() {
        Color sep = palette.getSeparator();
        assertNotNull(sep, "Separator color should not be null");
        assertEquals(new Color(128, 128, 128), sep, "Separator should default to gray");
    }

    @Test
    @DisplayName("ColorPalette provides default hex attribute color (light gray)")
    void testGetHexAttrColor() {
        Color hexAttr = palette.getHexAttr();
        assertNotNull(hexAttr, "Hex attr color should not be null");
        assertEquals(new Color(200, 200, 200), hexAttr, "Hex attr should default to light gray");
    }

    @Test
    @DisplayName("ColorPalette allows updating blue color")
    void testSetBlueColor() {
        Color newBlue = new Color(100, 100, 200);
        palette.setBlue(newBlue);
        assertEquals(newBlue, palette.getBlue(), "Blue color should update");
    }

    @Test
    @DisplayName("ColorPalette allows updating background color")
    void testSetBackgroundColor() {
        Color newBg = new Color(50, 50, 50);
        palette.setBackground(newBg);
        assertEquals(newBg, palette.getBackground(), "Background color should update");
    }

    @Test
    @DisplayName("ColorPalette returns correct foreground color by constant")
    void testGetForegroundByConstant() {
        Color fg = palette.getForegroundColor(HTI5250jConstants.COLOR_FG_GREEN);
        assertEquals(Color.green, fg, "Should return green for COLOR_FG_GREEN");
    }

    @Test
    @DisplayName("getForegroundColor maps all COLOR_FG constants correctly")
    void testGetForegroundAllConstants() {
        assertEquals(palette.getBlack(), palette.getForegroundColor(HTI5250jConstants.COLOR_FG_BLACK),
                "COLOR_FG_BLACK should map to black");
        assertEquals(palette.getBlue(), palette.getForegroundColor(HTI5250jConstants.COLOR_FG_BLUE),
                "COLOR_FG_BLUE should map to blue");
        assertEquals(palette.getGreen(), palette.getForegroundColor(HTI5250jConstants.COLOR_FG_GREEN),
                "COLOR_FG_GREEN should map to green");
        assertEquals(palette.getTurquoise(), palette.getForegroundColor(HTI5250jConstants.COLOR_FG_CYAN),
                "COLOR_FG_CYAN should map to turquoise");
        assertEquals(palette.getRed(), palette.getForegroundColor(HTI5250jConstants.COLOR_FG_RED),
                "COLOR_FG_RED should map to red");
        assertEquals(palette.getPink(), palette.getForegroundColor(HTI5250jConstants.COLOR_FG_MAGENTA),
                "COLOR_FG_MAGENTA should map to pink");
        assertEquals(palette.getYellow(), palette.getForegroundColor(HTI5250jConstants.COLOR_FG_YELLOW),
                "COLOR_FG_YELLOW should map to yellow");
        assertEquals(palette.getWhite(), palette.getForegroundColor(HTI5250jConstants.COLOR_FG_WHITE),
                "COLOR_FG_WHITE should map to white");
    }

    @Test
    @DisplayName("ColorPalette extracts background color from upper byte")
    void testGetBackgroundByConstant() {
        char colorValue = (char) ((HTI5250jConstants.COLOR_FG_BLUE << 8) | HTI5250jConstants.COLOR_FG_WHITE);
        Color bg = palette.getBackgroundColor(colorValue);
        assertEquals(palette.getBlue(), bg, "COLOR_FG_BLUE in upper byte should resolve to blue");
    }

    @Test
    @DisplayName("ColorPalette handles unknown color constant gracefully")
    void testGetUnknownColor() {
        Color unknown = palette.getForegroundColor(999);
        assertEquals(new Color(255, 165, 0), unknown, "Unknown color should return orange fallback");
    }

    @Test
    @DisplayName("ColorPalette constructor initializes all colors")
    void testConstructorInitializesAllColors() {
        assertNotNull(palette.getBlue(), "Blue should be initialized");
        assertNotNull(palette.getWhite(), "White should be initialized");
        assertNotNull(palette.getRed(), "Red should be initialized");
        assertNotNull(palette.getGreen(), "Green should be initialized");
        assertNotNull(palette.getYellow(), "Yellow should be initialized");
        assertNotNull(palette.getPink(), "Pink should be initialized");
        assertNotNull(palette.getTurquoise(), "Turquoise should be initialized");
        assertNotNull(palette.getBackground(), "Background should be initialized");
        assertNotNull(palette.getCursor(), "Cursor should be initialized");
        assertNotNull(palette.getGuiField(), "GUI field should be initialized");
        assertNotNull(palette.getSeparator(), "Separator should be initialized");
        assertNotNull(palette.getHexAttr(), "Hex attr should be initialized");
    }
}
