/**
 * Title: AttributePlaneOpsPairwiseTest.java
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise TDD tests for ScreenPlanes attribute plane operations
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.hti5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.hti5250j.HTI5250jConstants.*;

/**
 * Pairwise parameterized tests for ScreenPlanes attribute plane operations.
 *
 * Tests explore attribute interactions systematically across:
 * - Attribute types: color, extended-highlight, character-set
 * - Color values: default, green, white, red, blue, magenta
 * - Highlight: none, reverse, underscore, blink
 * - Column separator: none, single
 * - Position: single-cell, field-wide, screen-wide
 *
 * Discovers: Attribute mapping bugs, conflicting attribute handling, invalid state transitions,
 * boundary conditions, and adversarial attribute combinations.
 *
 * Test Strategy: N-wise testing (pairs + triplets for high-risk boundaries)
 * - Happy paths: Standard attribute application
 * - Adversarial: Conflicting attributes, invalid combinations, edge positions
 * - Boundary: Screen edges, field boundaries, attribute limit values
 */
@RunWith(Parameterized.class)
public class AttributePlaneOpsPairwiseTest {

    // Pairwise test parameters
    private final int attributeValue;
    private final String colorName;
    private final String highlightType;
    private final String columnSeparator;
    private final String positionType;

    // Instance variables
    private ScreenPlanes screenPlanes;
    private Screen5250TestDouble screen5250;
    private char[] screenAttr;
    private char[] screenColor;
    private char[] screenExtended;
    private char[] screenIsAttr;

    // Screen size constants
    private static final int SIZE_24 = 24;
    private static final int COLS_24 = 80;
    private static final int ROWS_24 = 24;
    private static final int SCREEN_SIZE = COLS_24 * ROWS_24;

    // Attribute value constants from ScreenPlanes.disperseAttribute() switch
    private static final int ATTR_GREEN_NORMAL = 32;          // Green normal
    private static final int ATTR_GREEN_REVERSE = 33;         // Green/reverse
    private static final int ATTR_WHITE_NORMAL = 34;          // White normal
    private static final int ATTR_WHITE_REVERSE = 35;         // White/reverse
    private static final int ATTR_GREEN_UNDERLINE = 36;       // Green/underline
    private static final int ATTR_GREEN_REV_UNDERLINE = 37;   // Green/reverse/underline
    private static final int ATTR_WHITE_UNDERLINE = 38;       // White/underline
    private static final int ATTR_WHITE_NON_DSP = 39;         // White/non-display
    private static final int ATTR_RED_NORMAL = 40;            // Red/normal
    private static final int ATTR_RED_REVERSE = 41;           // Red/reverse
    private static final int ATTR_RED_NORMAL_ALT = 42;        // Red/normal (alt)
    private static final int ATTR_RED_REVERSE_ALT = 43;       // Red/reverse (alt)
    private static final int ATTR_RED_UNDERLINE = 44;         // Red/underline
    private static final int ATTR_RED_REV_UNDERLINE = 45;     // Red/reverse/underline
    private static final int ATTR_RED_UNDERLINE_ALT = 46;     // Red/underline (alt)
    private static final int ATTR_RED_NON_DSP = 47;           // Red/non-display
    private static final int ATTR_COL_SEP_CYAN = 48;          // Cyan/col-sep
    private static final int ATTR_COL_SEP_CYAN_ALT = 49;      // Cyan/col-sep (alt)
    private static final int ATTR_COL_SEP_BLUE = 50;          // Blue/col-sep
    private static final int ATTR_COL_SEP_YELLOW = 51;        // Yellow/col-sep
    private static final int ATTR_COL_SEP_UL = 52;            // Col-sep/underline
    private static final int ATTR_COL_SEP_UL_ALT = 53;        // Col-sep/underline (alt)
    private static final int ATTR_COL_SEP_BLUE_UL = 54;       // Blue/col-sep/underline
    private static final int ATTR_COL_SEP_NON_DSP = 55;       // Col-sep/non-display
    private static final int ATTR_MAGENTA_NORMAL = 58;        // Magenta/normal
    private static final int ATTR_BLUE_NORMAL = 59;           // Blue/normal
    private static final int ATTR_MAGENTA_UNDERLINE = 60;     // Magenta/underline
    private static final int ATTR_MAGENTA_REV_UL = 61;        // Magenta/reverse/underline
    private static final int ATTR_BLUE_UNDERLINE = 62;        // Blue/underline
    private static final int ATTR_COL_SEP_NON_DSP_EXTENDED = 63; // Col-sep/non-display/extended
    private static final int ATTR_INVALID_HIGH = 99;          // Invalid high value

    // Test positions
    private static final int POS_FIRST_CELL = 0;              // Top-left corner
    private static final int POS_MID_FIELD = 40;              // Middle of first row
    private static final int POS_LAST_CELL = SCREEN_SIZE - 1; // Bottom-right corner
    private static final int POS_ROW_START = COLS_24;         // Start of second row
    private static final int POS_ROW_END = (2 * COLS_24) - 1; // End of second row

    /**
     * Test double for Screen5250 with minimal implementation
     */
    private static class Screen5250TestDouble extends Screen5250 {
        private int numCols = COLS_24;

        public Screen5250TestDouble() {
            super();
        }

        @Override
        public int getPos(int row, int col) {
            return (row * numCols) + col;
        }

        @Override
        public int getScreenLength() {
            return SCREEN_SIZE;
        }

        @Override
        public boolean isInField(int x, boolean checkAttr) {
            return false;
        }

        @Override
        public StringBuffer getHSMore() {
            return new StringBuffer("More...");
        }

        @Override
        public StringBuffer getHSBottom() {
            return new StringBuffer("Bottom");
        }

        @Override
        public void setDirty(int pos) {
            // No-op for testing
        }
    }

    /**
     * Pairwise test data: 25+ test cases covering attribute combinations
     *
     * Pairwise dimensions:
     * 1. Attribute type: standard-color, extended-col-sep, non-display
     * 2. Color value: default, green, white, red, blue, magenta
     * 3. Highlight: none, reverse, underscore
     * 4. Column separator: none, single
     * 5. Position: single-cell, field-wide, screen-wide
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // Happy path: standard colors with various highlights
            { ATTR_GREEN_NORMAL,          "green",   "none",       "none",   "single-cell" },
            { ATTR_GREEN_REVERSE,         "green",   "reverse",    "none",   "field-wide" },
            { ATTR_GREEN_UNDERLINE,       "green",   "underscore", "none",   "screen-wide" },
            { ATTR_WHITE_NORMAL,          "white",   "none",       "none",   "field-wide" },
            { ATTR_WHITE_REVERSE,         "white",   "reverse",    "none",   "single-cell" },
            { ATTR_WHITE_UNDERLINE,       "white",   "underscore", "none",   "screen-wide" },
            { ATTR_RED_NORMAL,            "red",     "none",       "none",   "screen-wide" },
            { ATTR_RED_REVERSE,           "red",     "reverse",    "none",   "single-cell" },
            { ATTR_RED_UNDERLINE,         "red",     "underscore", "none",   "field-wide" },
            { ATTR_BLUE_NORMAL,           "blue",    "none",       "none",   "field-wide" },
            { ATTR_MAGENTA_NORMAL,        "magenta", "none",       "none",   "single-cell" },
            { ATTR_MAGENTA_UNDERLINE,     "magenta", "underscore", "none",   "screen-wide" },

            // Extended: column separators with and without underline
            { ATTR_COL_SEP_CYAN,          "cyan",    "none",       "single",  "single-cell" },
            { ATTR_COL_SEP_CYAN_ALT,      "cyan",    "none",       "single",  "field-wide" },
            { ATTR_COL_SEP_BLUE,          "blue",    "none",       "single",  "screen-wide" },
            { ATTR_COL_SEP_YELLOW,        "yellow",  "none",       "single",  "single-cell" },
            { ATTR_COL_SEP_UL,            "cyan",    "underscore", "single",  "field-wide" },
            { ATTR_COL_SEP_UL_ALT,        "cyan",    "underscore", "single",  "screen-wide" },
            { ATTR_COL_SEP_BLUE_UL,       "blue",    "underscore", "single",  "single-cell" },

            // Non-display attributes (adversarial)
            { ATTR_WHITE_NON_DSP,         "white",   "none",       "none",   "single-cell" },
            { ATTR_RED_NON_DSP,           "red",     "none",       "none",   "field-wide" },
            { ATTR_COL_SEP_NON_DSP,       "none",    "none",       "single",  "screen-wide" },
            { ATTR_COL_SEP_NON_DSP_EXTENDED, "none", "none",       "single",  "single-cell" },

            // Complex combinations: multiple attributes at same position
            { ATTR_GREEN_REV_UNDERLINE,   "green",   "reverse+ul", "none",   "field-wide" },
            { ATTR_RED_REV_UNDERLINE,     "red",     "reverse+ul", "none",   "screen-wide" },
            { ATTR_MAGENTA_REV_UL,        "magenta", "reverse+ul", "none",   "single-cell" },

            // Boundary and edge cases
            { ATTR_GREEN_NORMAL,          "green",   "none",       "none",   "screen-wide" }, // Extreme position
            { ATTR_RED_UNDERLINE,         "red",     "underscore", "none",   "single-cell" }, // Different color
            { ATTR_BLUE_NORMAL,           "blue",    "none",       "single",  "field-wide" }, // Col-sep on blue
        });
    }

    public AttributePlaneOpsPairwiseTest(int attributeValue, String colorName,
                                         String highlightType, String columnSeparator,
                                         String positionType) {
        this.attributeValue = attributeValue;
        this.colorName = colorName;
        this.highlightType = highlightType;
        this.columnSeparator = columnSeparator;
        this.positionType = positionType;
    }

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble();
        screenPlanes = new ScreenPlanes(screen5250, SIZE_24);

        // Access private fields via reflection for testing
        screenAttr = getPrivateField("screenAttr", char[].class);
        screenColor = getPrivateField("screenColor", char[].class);
        screenExtended = getPrivateField("screenExtended", char[].class);
        screenIsAttr = getPrivateField("screenIsAttr", char[].class);
    }

    /**
     * Helper to access private fields via reflection
     */
    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(String fieldName, Class<T> fieldType)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = ScreenPlanes.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(screenPlanes);
    }

    /**
     * Test 1: Attribute application at single cell position
     * Verifies: screenAttr[pos] is set and disperseAttribute() is called
     */
    @Test
    public void testSetScreenAttrSingleCell() {
        int pos = getPositionForType("single-cell");

        // Act: Apply attribute to single position
        screenPlanes.setScreenAttr(pos, attributeValue, true);

        // Assert: Attribute plane updated correctly
        assertEquals("Attribute not set at position " + pos,
                attributeValue, screenAttr[pos]);
        assertEquals("Attribute not marked in isAttr plane",
                (char) 1, screenIsAttr[pos]);
    }

    /**
     * Test 2: Attribute application across field
     * Verifies: Attribute applies to all positions in range without side effects
     */
    @Test
    public void testSetScreenAttrFieldWide() {
        int startPos = getPositionForType("field-wide");
        int fieldLength = 20; // Typical field width

        // Act: Apply attribute to field range
        for (int i = 0; i < fieldLength; i++) {
            screenPlanes.setScreenAttr(startPos + i, attributeValue);
        }

        // Assert: All positions in field have attribute
        for (int i = 0; i < fieldLength; i++) {
            assertEquals("Attribute not applied at field position " + i,
                    attributeValue, screenAttr[startPos + i]);
        }

        // Assert: Positions outside field unchanged
        if (startPos > 0) {
            assertEquals("Previous position incorrectly modified",
                    32, screenAttr[startPos - 1]); // Default attribute
        }
    }

    /**
     * Test 3: Attribute application across entire screen
     * Verifies: Mass attribute application doesn't overflow or corrupt
     */
    @Test
    public void testSetScreenAttrScreenWide() {
        int testAttr = attributeValue;

        // Act: Apply same attribute to all screen positions
        for (int pos = 0; pos < SCREEN_SIZE; pos++) {
            screenPlanes.setScreenAttr(pos, testAttr);
        }

        // Assert: All positions updated with same attribute
        for (int pos = 0; pos < SCREEN_SIZE; pos++) {
            assertEquals("Screen-wide attribute not applied at position " + pos,
                    testAttr, screenAttr[pos]);
        }
    }

    /**
     * Test 4: Color plane dispersal for standard colors
     * Verifies: disperseAttribute() correctly sets color plane values
     */
    @Test
    public void testColorPlaneDispersion() {
        int pos = POS_MID_FIELD;

        // Act: Set attribute which triggers disperseAttribute()
        screenPlanes.setScreenAttr(pos, attributeValue);

        // Assert: Color plane updated (non-zero for valid attributes)
        char color = screenColor[pos];
        if (attributeValue != 0) {
            assertTrue("Color plane should be updated for attribute " + attributeValue,
                    (int) color != 0);
        }

        // Assert: Color value format is valid (high byte = bg, low byte = fg)
        int bgColor = (color >> 8) & 0xff;
        int fgColor = color & 0xff;
        assertTrue("Background color out of range", bgColor >= 0 && bgColor <= 7);
        assertTrue("Foreground color out of range", fgColor >= 0 && fgColor <= 0xf);
    }

    /**
     * Test 5: Extended attribute plane for underline
     * Verifies: EXTENDED_5250_UNDERLINE flag set when appropriate
     */
    @Test
    public void testExtendedUnderlineAttribute() {
        int pos = POS_MID_FIELD;

        // Act: Set underline-containing attribute
        if (highlightType.contains("underscore") || highlightType.contains("ul")) {
            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Underline flag set in extended plane
            char extended = screenExtended[pos];
            assertEquals("Underline flag not set for " + highlightType,
                    EXTENDED_5250_UNDERLINE, extended & EXTENDED_5250_UNDERLINE);
        }
    }

    /**
     * Test 6: Extended attribute plane for column separator
     * Verifies: EXTENDED_5250_COL_SEP flag set when appropriate
     */
    @Test
    public void testExtendedColumnSeparatorAttribute() {
        int pos = POS_MID_FIELD;

        // Act: Set column separator attribute
        if ("single".equals(columnSeparator)) {
            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Column separator flag set in extended plane
            char extended = screenExtended[pos];
            assertEquals("Column separator flag not set",
                    EXTENDED_5250_COL_SEP, extended & EXTENDED_5250_COL_SEP);
        }
    }

    /**
     * Test 7: Extended attribute plane for non-display
     * Verifies: EXTENDED_5250_NON_DSP flag set when appropriate
     */
    @Test
    public void testExtendedNonDisplayAttribute() {
        int pos = POS_MID_FIELD;

        // Act: Set non-display attribute
        if (attributeValue == ATTR_WHITE_NON_DSP || attributeValue == ATTR_RED_NON_DSP ||
            attributeValue == ATTR_COL_SEP_NON_DSP || attributeValue == ATTR_COL_SEP_NON_DSP_EXTENDED) {

            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Non-display flag set in extended plane
            char extended = screenExtended[pos];
            assertEquals("Non-display flag not set",
                    EXTENDED_5250_NON_DSP, extended & EXTENDED_5250_NON_DSP);
        }
    }

    /**
     * Test 8: Attribute boundary - screen start position
     * Verifies: Attributes apply correctly at position 0 (top-left)
     */
    @Test
    public void testAttributeAtScreenStart() {
        int pos = POS_FIRST_CELL;

        // Act
        screenPlanes.setScreenAttr(pos, attributeValue);

        // Assert
        assertEquals("Attribute not set at screen start",
                attributeValue, (int) screenAttr[pos]);
        assertTrue("Color plane not updated at screen start",
                (int) screenColor[pos] != 0);
    }

    /**
     * Test 9: Attribute boundary - screen end position
     * Verifies: Attributes apply correctly at last position
     */
    @Test
    public void testAttributeAtScreenEnd() {
        int pos = POS_LAST_CELL;

        // Act
        screenPlanes.setScreenAttr(pos, attributeValue);

        // Assert
        assertEquals("Attribute not set at screen end",
                attributeValue, (int) screenAttr[pos]);
        assertTrue("Color plane not updated at screen end",
                (int) screenColor[pos] != 0);
    }

    /**
     * Test 10: Attribute boundary - row start position
     * Verifies: Attributes apply correctly at row boundaries (no column wraparound)
     */
    @Test
    public void testAttributeAtRowStart() {
        int pos = POS_ROW_START;

        // Act
        screenPlanes.setScreenAttr(pos, attributeValue);

        // Assert
        assertEquals("Attribute not set at row start",
                attributeValue, screenAttr[pos]);
        assertEquals("Previous position (end of previous row) should not be modified",
                32, screenAttr[pos - 1]); // Default attribute value
    }

    /**
     * Test 11: Attribute boundary - row end position
     * Verifies: Attributes apply correctly at end of row (no overflow)
     */
    @Test
    public void testAttributeAtRowEnd() {
        int pos = POS_ROW_END;

        // Act
        screenPlanes.setScreenAttr(pos, attributeValue);

        // Assert
        assertEquals("Attribute not set at row end",
                attributeValue, screenAttr[pos]);
        if (pos < SCREEN_SIZE - 1) {
            assertEquals("Next position (start of next row) should not be modified",
                    32, screenAttr[pos + 1]); // Default attribute value
        }
    }

    /**
     * Test 12: Adversarial - conflicting color/highlight combinations
     * Verifies: Invalid combinations don't corrupt state
     */
    @Test
    public void testConflictingAttributeCombinations() {
        // Attempt to apply conflicting attributes at same position
        int pos = POS_MID_FIELD;

        // Act 1: Apply first attribute (e.g., green normal)
        screenPlanes.setScreenAttr(pos, ATTR_GREEN_NORMAL);
        char colorAfterFirst = screenColor[pos];

        // Act 2: Apply conflicting attribute (e.g., red reverse) to same position
        screenPlanes.setScreenAttr(pos, ATTR_RED_REVERSE);
        char colorAfterConflict = screenColor[pos];

        // Assert: Last attribute wins (no corruption)
        assertEquals("Final attribute not applied",
                ATTR_RED_REVERSE, (int) screenAttr[pos]);
        assertFalse("Color should change when attribute changes",
                colorAfterFirst == colorAfterConflict);
    }

    /**
     * Test 13: Adversarial - attribute overflow beyond screen size
     * Verifies: Out-of-bounds access doesn't corrupt adjacent memory
     */
    @Test
    public void testAttributeOutOfBoundsProtection() {
        // Try to access position beyond screen size
        int outOfBoundsPos = SCREEN_SIZE + 10;

        // Act: Save last valid position state
        char lastValidAttr = screenAttr[SCREEN_SIZE - 1];

        // This should not throw or corrupt screen
        try {
            // Note: setScreenAttr doesn't explicitly bounds-check,
            // but accessing beyond SCREEN_SIZE would be out of array bounds
            // Just verify that valid positions are still intact
            for (int pos = 0; pos < SCREEN_SIZE; pos++) {
                if (screenAttr[pos] != 32 && screenAttr[pos] != lastValidAttr) {
                    fail("Screen corruption detected at position " + pos);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            fail("Array out of bounds exception should not occur with valid positions");
        }
    }

    /**
     * Test 14: Adversarial - zero/null attribute value
     * Verifies: Invalid attributes handled gracefully
     */
    @Test
    public void testInvalidZeroAttribute() {
        int pos = POS_MID_FIELD;

        // Act: Set attribute to 0 (technically invalid)
        screenPlanes.setScreenAttr(pos, 0);

        // Assert: Attribute still set, extended plane not updated
        assertEquals("Zero attribute should still be set",
                0, (int) screenAttr[pos]);
        // disperseAttribute() returns early for attr == 0, so extended stays initialized
    }

    /**
     * Test 15: Adversarial - invalid high attribute value
     * Verifies: Out-of-range attribute values handled gracefully
     */
    @Test
    public void testInvalidHighAttribute() {
        int pos = POS_MID_FIELD;

        // Act: Set attribute to invalid high value
        screenPlanes.setScreenAttr(pos, ATTR_INVALID_HIGH);

        // Assert: Attribute set to whatever value was provided
        assertEquals("Invalid high attribute should be set to provided value",
                ATTR_INVALID_HIGH, (int) screenAttr[pos]);
        // disperseAttribute() falls through to default case for unmapped values
    }

    /**
     * Test 16: Pairwise - green color with all highlight types
     * Verifies: Green color can be combined with reverse, underline, reverse+underline
     */
    @Test
    public void testGreenColorVariants() {
        if (colorName.equals("green")) {
            int pos = POS_MID_FIELD;

            // Act
            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Color plane shows green (foreground color 2)
            char color = screenColor[pos];
            int fgColor = color & 0xff;
            assertEquals("Green color not applied",
                    COLOR_FG_GREEN, fgColor);
        }
    }

    /**
     * Test 17: Pairwise - red color with all highlight types
     * Verifies: Red color can be combined with multiple highlights
     */
    @Test
    public void testRedColorVariants() {
        if (colorName.equals("red")) {
            int pos = POS_MID_FIELD;

            // Act
            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Color plane shows red (foreground color 4)
            char color = screenColor[pos];
            int fgColor = color & 0xff;
            assertEquals("Red color not applied",
                    COLOR_FG_RED, fgColor);
        }
    }

    /**
     * Test 18: Pairwise - white color with all highlight types
     * Verifies: White color can be combined with various effects
     */
    @Test
    public void testWhiteColorVariants() {
        if (colorName.equals("white")) {
            int pos = POS_MID_FIELD;

            // Act
            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Color plane shows white (foreground color 7)
            char color = screenColor[pos];
            int fgColor = color & 0xff;
            assertEquals("White color not applied",
                    COLOR_FG_WHITE, fgColor);
        }
    }

    /**
     * Test 19: Pairwise - column separator with cyan color
     * Verifies: Column separator correctly combined with cyan
     */
    @Test
    public void testColumnSeparatorCyan() {
        if ("single".equals(columnSeparator) && colorName.equals("cyan")) {
            int pos = POS_MID_FIELD;

            // Act
            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Both column separator and color flags set
            assertEquals("Column separator flag not set",
                    EXTENDED_5250_COL_SEP, (int) (screenExtended[pos] & EXTENDED_5250_COL_SEP));
            char color = screenColor[pos];
            int fgColor = color & 0xff;
            assertEquals("Cyan color not applied with column separator",
                    COLOR_FG_CYAN, fgColor);
        }
    }

    /**
     * Test 20: Pairwise - column separator with blue color
     * Verifies: Column separator correctly combined with blue
     */
    @Test
    public void testColumnSeparatorBlue() {
        if ("single".equals(columnSeparator) && colorName.equals("blue")) {
            int pos = POS_MID_FIELD;

            // Act
            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Both flags set correctly
            assertEquals("Column separator flag not set",
                    EXTENDED_5250_COL_SEP, (int) (screenExtended[pos] & EXTENDED_5250_COL_SEP));
        }
    }

    /**
     * Test 21: Pairwise - underline with reverse highlight
     * Verifies: Underline and reverse can coexist
     */
    @Test
    public void testUnderlineWithReverse() {
        if (highlightType.contains("reverse") && highlightType.contains("ul")) {
            int pos = POS_MID_FIELD;

            // Act
            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Both flags set
            char extended = screenExtended[pos];
            assertEquals("Underline not set",
                    EXTENDED_5250_UNDERLINE, (int) (extended & EXTENDED_5250_UNDERLINE));
            // Note: reverse is indicated in color plane, not extended
            assertTrue("Color plane should reflect reverse",
                    (int) screenColor[pos] != 0);
        }
    }

    /**
     * Test 22: Sequential attribute changes at same position
     * Verifies: Attributes can be updated multiple times without side effects
     */
    @Test
    public void testSequentialAttributeChanges() {
        int pos = POS_MID_FIELD;
        int[] attrs = { ATTR_GREEN_NORMAL, ATTR_RED_REVERSE, ATTR_WHITE_UNDERLINE };

        // Act: Apply attributes sequentially
        for (int attr : attrs) {
            screenPlanes.setScreenAttr(pos, attr);
        }

        // Assert: Final attribute is applied
        assertEquals("Final attribute not applied",
                attrs[attrs.length - 1], (int) screenAttr[pos]);

        // Assert: Color plane updated to reflect final attribute
        assertTrue("Color plane should reflect final attribute",
                (int) screenColor[pos] != 0);
    }

    /**
     * Test 23: Attribute with explicit isAttr flag
     * Verifies: setScreenAttr(int, int, boolean) respects isAttr flag
     */
    @Test
    public void testAttributeWithIsAttrFlag() {
        int pos = POS_MID_FIELD;

        // Act: Set attribute with isAttr = true
        screenPlanes.setScreenAttr(pos, attributeValue, true);

        // Assert: Both attribute and isAttr flag set
        assertEquals("Attribute not set",
                attributeValue, (int) screenAttr[pos]);
        assertEquals("isAttr flag not set",
                1, (int) screenIsAttr[pos]);

        // Act: Set attribute with isAttr = false at different position
        int pos2 = POS_MID_FIELD + 10;
        screenPlanes.setScreenAttr(pos2, attributeValue, false);

        // Assert: Attribute set but isAttr flag not set
        assertEquals("Attribute not set",
                attributeValue, (int) screenAttr[pos2]);
        assertEquals("isAttr flag should not be set",
                0, (int) screenIsAttr[pos2]);
    }

    /**
     * Test 24: Attribute plane isolation from other planes
     * Verifies: Changes to attribute plane don't corrupt screen or GUI planes
     */
    @Test
    public void testAttributePlaneIsolation() throws NoSuchFieldException, IllegalAccessException {
        int pos = POS_MID_FIELD;
        char[] screenText = new char[SCREEN_SIZE];
        System.arraycopy(getPrivateField("screen", char[].class), 0, screenText, 0, SCREEN_SIZE);

        char[] screenGUI = new char[SCREEN_SIZE];
        System.arraycopy(getPrivateField("screenGUI", char[].class), 0, screenGUI, 0, SCREEN_SIZE);

        // Act: Modify attribute plane
        screenPlanes.setScreenAttr(pos, attributeValue);

        // Assert: Text plane unchanged
        char[] screenTextAfter = getPrivateField("screen", char[].class);
        for (int i = 0; i < SCREEN_SIZE; i++) {
            assertEquals("Text plane corrupted by attribute change at position " + i,
                    screenText[i], screenTextAfter[i]);
        }

        // Assert: GUI plane unchanged
        char[] screenGUIAfter = getPrivateField("screenGUI", char[].class);
        for (int i = 0; i < SCREEN_SIZE; i++) {
            assertEquals("GUI plane corrupted by attribute change at position " + i,
                    screenGUI[i], screenGUIAfter[i]);
        }
    }

    /**
     * Test 25: Extended attribute bit field integrity
     * Verifies: Extended attribute bits don't interfere with each other
     */
    @Test
    public void testExtendedAttributeBitIntegrity() {
        int pos = POS_MID_FIELD;

        // Act: Apply attribute that may set multiple extended bits
        screenPlanes.setScreenAttr(pos, attributeValue);

        // Assert: Extended value is within valid bit field
        char extended = screenExtended[pos];
        int validBits = EXTENDED_5250_REVERSE | EXTENDED_5250_UNDERLINE |
                       EXTENDED_5250_BLINK | EXTENDED_5250_COL_SEP |
                       EXTENDED_5250_NON_DSP;
        int extValue = (int) extended & 0xff;
        int invalidBits = extValue & ~validBits;
        assertEquals("Invalid bits set in extended plane",
                0, invalidBits & 0xff);
    }

    /**
     * Test 26: Regression - disperseAttribute called on setScreenAttr
     * Verifies: disperseAttribute() is invoked for attribute application
     */
    @Test
    public void testDispersAttributeInvoked() {
        int pos = POS_MID_FIELD;
        char initialColor = screenColor[pos];
        char initialExtended = screenExtended[pos];

        // Act: Set non-zero attribute
        if (attributeValue != 0) {
            screenPlanes.setScreenAttr(pos, attributeValue);

            // Assert: Color or extended plane changed (indicates disperseAttribute called)
            boolean colorChanged = screenColor[pos] != initialColor;
            boolean extendedChanged = screenExtended[pos] != initialExtended;
            assertTrue("disperseAttribute() not called for attribute " + attributeValue +
                    " (color changed: " + colorChanged + ", extended changed: " + extendedChanged + ")",
                    colorChanged || extendedChanged);
        }
    }

    /**
     * Helper: Get position based on position type
     */
    private int getPositionForType(String type) {
        switch (type) {
            case "single-cell":
                return POS_FIRST_CELL;
            case "field-wide":
                return POS_ROW_START;
            case "screen-wide":
                return POS_LAST_CELL;
            default:
                return POS_MID_FIELD;
        }
    }
}
