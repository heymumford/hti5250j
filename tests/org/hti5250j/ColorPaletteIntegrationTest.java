/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.hti5250j.framework.tn5250.Screen5250;

import java.awt.Color;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 TDD Tests: ColorPalette integration with GuiGraphicBuffer
 *
 * RED Phase: These tests will fail if old color fields still exist in GuiGraphicBuffer
 * GREEN Phase: Tests pass after removing old color fields
 * REFACTOR Phase: Verify ColorPalette delegation works correctly
 */
@DisplayName("ColorPalette Integration Tests (Phase 1)")
class ColorPaletteIntegrationTest {

    @Mock
    private Screen5250 mockScreen;

    @Mock
    private SessionPanel mockGui;

    @Mock
    private SessionConfig mockConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("GuiGraphicBuffer should NOT have old color fields (colorBlue, colorRed, etc.)")
    void testOldColorFieldsRemoved() throws Exception {
        // RED phase: This test will FAIL if old fields exist
        Field[] fields = GuiGraphicBuffer.class.getDeclaredFields();

        String[] oldColorFieldNames = {
            "colorBlue", "colorRed", "colorGreen", "colorYellow",
            "colorTurq", "colorWhite", "colorPink", "colorBlack",
            "colorBg", "colorCursor", "colorGuiField", "colorGUIField",
            "colorSeparator", "colorSep", "colorHexAttr"
        };

        for (Field field : fields) {
            for (String oldFieldName : oldColorFieldNames) {
                assertNotEquals(oldFieldName, field.getName(),
                    "GuiGraphicBuffer should NOT have field: " + oldFieldName +
                    " (should use ColorPalette instead)");
            }
        }
    }

    @Test
    @DisplayName("GuiGraphicBuffer should have colorPalette field")
    void testColorPaletteFieldExists() throws Exception {
        Field colorPaletteField = null;
        try {
            colorPaletteField = GuiGraphicBuffer.class.getDeclaredField("colorPalette");
        } catch (NoSuchFieldException e) {
            fail("GuiGraphicBuffer should have a 'colorPalette' field");
        }

        assertNotNull(colorPaletteField);
        assertEquals(ColorPalette.class, colorPaletteField.getType());
    }

    @Test
    @DisplayName("ColorPalette should provide all required color methods")
    void testColorPaletteHasRequiredMethods() throws Exception {
        ColorPalette palette = new ColorPalette();

        // Verify all getter methods exist and return Colors
        assertNotNull(palette.getBlue());
        assertNotNull(palette.getRed());
        assertNotNull(palette.getGreen());
        assertNotNull(palette.getYellow());
        assertNotNull(palette.getTurquoise());
        assertNotNull(palette.getWhite());
        assertNotNull(palette.getPink());
        assertNotNull(palette.getBlack());
        assertNotNull(palette.getBg());
        assertNotNull(palette.getGuiField());
        assertNotNull(palette.getCursor());
        assertNotNull(palette.getSeparator());
        assertNotNull(palette.getHexAttr());
        assertNotNull(palette.getBackground());
        assertNotNull(palette.getText());

        assertTrue(palette.getBlue() instanceof Color);
        assertTrue(palette.getRed() instanceof Color);
    }

    @Test
    @DisplayName("ColorPalette setter methods should update colors")
    void testColorPaletteSetters() {
        ColorPalette palette = new ColorPalette();

        Color testColor = new Color(123, 45, 67);
        palette.setBlue(testColor);
        assertEquals(testColor, palette.getBlue());

        palette.setRed(testColor);
        assertEquals(testColor, palette.getRed());

        palette.setBackground(testColor);
        assertEquals(testColor, palette.getBackground());
    }

    @Test
    @DisplayName("ColorPalette should support color lookup by constant")
    void testColorLookupByConstant() {
        ColorPalette palette = new ColorPalette();

        // Test foreground color lookup (0-7)
        assertNotNull(palette.getForegroundColor((char)0)); // black
        assertNotNull(palette.getForegroundColor((char)1)); // red
        assertNotNull(palette.getForegroundColor((char)4)); // blue
        assertNotNull(palette.getForegroundColor((char)7)); // pink

        // Test unknown color returns orange
        Color unknownColor = palette.getForegroundColor((char)99);
        assertEquals(new Color(255, 165, 0), unknownColor); // orange
    }
}
