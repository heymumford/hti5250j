/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.awt.Color;

/**
 * TDD Phase 1 - RED: Tests for ColorPalette class extraction.
 *
 * These tests define the API for ColorPalette before implementation exists.
 * Expected to FAIL until ColorPalette class is created.
 */
@Disabled("TDD RED phase - ColorPalette extraction not yet implemented")
@DisplayName("ColorPalette Tests - Phase 1 Extraction")
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
    @DisplayName("ColorPalette provides default pink/magenta color")
    void testGetPinkColor() {
        Color pink = palette.getPink();
        assertNotNull(pink, "Pink color should not be null");
        assertEquals(Color.magenta, pink, "Pink should match Color.magenta");
    }

    @Test
    @DisplayName("ColorPalette provides default turquoise color")
    void testGetTurquoiseColor() {
        Color turq = palette.getTurquoise();
        assertNotNull(turq, "Turquoise color should not be null");
        assertEquals(new Color(0, 240, 255), turq, "Turquoise should match RGB(0,240,255)");
    }

    @Test
    @DisplayName("ColorPalette provides default background color (black for non-GUI)")
    void testGetBackgroundColor() {
        Color bg = palette.getBackground();
        assertNotNull(bg, "Background color should not be null");
        assertEquals(Color.black, bg, "Default background should be black");
    }

    @Test
    @DisplayName("ColorPalette provides GUI background color (light gray)")
    void testGetBackgroundColorForGuiMode() {
        palette.setGuiInterface(true);
        Color bg = palette.getBackground();
        assertEquals(Color.lightGray, bg, "GUI mode background should be light gray");
    }

    @Test
    @DisplayName("ColorPalette provides default cursor color")
    void testGetCursorColor() {
        Color cursor = palette.getCursor();
        assertNotNull(cursor, "Cursor color should not be null");
        assertEquals(Color.white, cursor, "Cursor should default to white");
    }

    @Test
    @DisplayName("ColorPalette provides default GUI field color")
    void testGetGuiFieldColor() {
        Color guiField = palette.getGuiField();
        assertNotNull(guiField, "GUI field color should not be null");
        assertEquals(Color.white, guiField, "GUI field should default to white");
    }

    @Test
    @DisplayName("ColorPalette provides default separator color")
    void testGetSeparatorColor() {
        Color sep = palette.getSeparator();
        assertNotNull(sep, "Separator color should not be null");
        assertEquals(Color.white, sep, "Separator should default to white");
    }

    @Test
    @DisplayName("ColorPalette provides default hex attribute color")
    void testGetHexAttrColor() {
        Color hexAttr = palette.getHexAttr();
        assertNotNull(hexAttr, "Hex attr color should not be null");
        assertEquals(Color.white, hexAttr, "Hex attr should default to white");
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
    @DisplayName("ColorPalette returns correct background color by constant")
    void testGetBackgroundByConstant() {
        // Background color encoding is in upper byte
        char colorValue = (char) ((HTI5250jConstants.COLOR_FG_BLUE << 8) | HTI5250jConstants.COLOR_FG_WHITE);
        Color bg = palette.getBackgroundColor(colorValue);
        assertEquals(palette.getBlue(), bg, "Should extract background from upper byte");
    }

    @Test
    @DisplayName("ColorPalette handles unknown color constant gracefully")
    void testGetUnknownColor() {
        Color unknown = palette.getForegroundColor(999);
        assertEquals(Color.orange, unknown, "Unknown color should return orange");
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
