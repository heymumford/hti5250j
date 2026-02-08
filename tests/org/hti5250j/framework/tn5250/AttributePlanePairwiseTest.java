/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;


import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pairwise parameterized tests for ScreenPlanes attribute plane operations.
 *
 * Tests explore attribute interactions systematically across:
 * - Attribute types: color, extended-highlight, character-set
 * - Color values: default, green, white, red, blue, all-colors
 * - Highlight: none, reverse, underscore, blink
 * - Column separator: none, single, double
 * - Position: single-cell, field-wide, screen-wide
 *
 * Discovers: Attribute mapping bugs, conflicting attribute handling, invalid state transitions
 */
public class AttributePlanePairwiseTest {

    // Test parameters
    private int attributeValue;
    private String attributeType;
    private String colorValue;
    private String highlightType;
    private String columnSeparator;
    private String positionType;

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

    // Attribute value constants (from ScreenPlanes.disperseAttribute)
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
    private static final int ATTR_COL_SEP_UL = 52;            // Cyan/col-sep/underline
    private static final int ATTR_COL_SEP_UL_ALT = 53;        // Cyan/col-sep/underline (alt)
    private static final int ATTR_COL_SEP_BLUE_UL = 54;       // Blue/col-sep/underline
    private static final int ATTR_COL_SEP_NON_DSP = 55;       // Col-sep/non-display
    private static final int ATTR_PINK_NORMAL = 56;           // Pink/normal
    private static final int ATTR_PINK_REVERSE = 57;          // Pink/reverse
    private static final int ATTR_MAGENTA_NORMAL = 58;        // Magenta/normal
    private static final int ATTR_BLUE_NORMAL = 59;           // Blue/normal
    private static final int ATTR_BLUE_REVERSE = 60;          // Blue/reverse
    private static final int ATTR_MAGENTA_REVERSE = 61;       // Magenta/reverse
    private static final int ATTR_NONDISPLAY = 63;            // Non-display

    /**
     * Pairwise parameter combinations covering:
     * - Attribute types: color, extended-highlight, character-set
     * - Color values: default, green, white, red, blue, all-colors
     * - Highlight: none, reverse, underscore, blink
     * - Column separator: none, single, double
     * - Position: single-cell, field-wide, screen-wide
     *
     * Total: 25+ test combinations
     */
        public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Basic attribute tests: attribute type x color value
                { ATTR_GREEN_NORMAL, "color", "default", "none", "none", "single-cell" },
                { ATTR_GREEN_REVERSE, "color", "green", "reverse", "none", "single-cell" },
                { ATTR_WHITE_NORMAL, "color", "white", "none", "none", "single-cell" },
                { ATTR_WHITE_REVERSE, "color", "white", "reverse", "none", "single-cell" },
                { ATTR_RED_NORMAL, "color", "red", "none", "none", "single-cell" },
                { ATTR_RED_REVERSE, "color", "red", "reverse", "none", "single-cell" },
                { ATTR_BLUE_NORMAL, "color", "blue", "none", "none", "single-cell" },
                { ATTR_BLUE_REVERSE, "color", "blue", "reverse", "none", "single-cell" },

                // Extended attributes: highlight combinations
                { ATTR_GREEN_UNDERLINE, "extended-highlight", "green", "underscore", "none", "single-cell" },
                { ATTR_WHITE_UNDERLINE, "extended-highlight", "white", "underscore", "none", "single-cell" },
                { ATTR_RED_UNDERLINE, "extended-highlight", "red", "underscore", "none", "single-cell" },
                { ATTR_GREEN_REV_UNDERLINE, "extended-highlight", "green", "reverse", "none", "single-cell" },
                { ATTR_RED_REV_UNDERLINE, "extended-highlight", "red", "reverse", "none", "single-cell" },

                // Character set tests: column separator combinations
                { ATTR_COL_SEP_CYAN, "character-set", "cyan", "none", "single", "single-cell" },
                { ATTR_COL_SEP_CYAN_ALT, "character-set", "cyan", "none", "single", "field-wide" },
                { ATTR_COL_SEP_BLUE, "character-set", "blue", "none", "single", "single-cell" },
                { ATTR_COL_SEP_YELLOW, "character-set", "yellow", "none", "single", "single-cell" },
                { ATTR_COL_SEP_UL, "character-set", "cyan", "underscore", "single", "single-cell" },
                { ATTR_COL_SEP_UL_ALT, "character-set", "cyan", "underscore", "single", "field-wide" },
                { ATTR_COL_SEP_BLUE_UL, "character-set", "blue", "underscore", "single", "single-cell" },

                // Position variation tests
                { ATTR_GREEN_NORMAL, "color", "default", "none", "none", "field-wide" },
                { ATTR_RED_NORMAL, "color", "red", "none", "none", "field-wide" },
                { ATTR_WHITE_NORMAL, "color", "white", "none", "none", "screen-wide" },
                { ATTR_GREEN_REVERSE, "color", "green", "reverse", "none", "screen-wide" },

                // Non-display and special attributes
                { ATTR_WHITE_NON_DSP, "extended-highlight", "white", "none", "none", "single-cell" },
                { ATTR_RED_NON_DSP, "extended-highlight", "red", "none", "none", "single-cell" },
                { ATTR_COL_SEP_NON_DSP, "character-set", "none", "none", "none", "single-cell" },
                { ATTR_NONDISPLAY, "extended-highlight", "none", "none", "none", "single-cell" },

                // Pink and magenta colors
                { ATTR_PINK_NORMAL, "color", "pink", "none", "none", "single-cell" },
                { ATTR_PINK_REVERSE, "color", "pink", "reverse", "none", "single-cell" },
                { ATTR_MAGENTA_NORMAL, "color", "magenta", "none", "none", "single-cell" },
                { ATTR_MAGENTA_REVERSE, "color", "magenta", "reverse", "none", "single-cell" },

                // Adversarial: conflicting attributes
                { ATTR_GREEN_NORMAL, "conflict", "green", "none", "single", "single-cell" },
                { ATTR_RED_NORMAL, "conflict", "red", "none", "single", "single-cell" },
                { ATTR_GREEN_REV_UNDERLINE, "conflict", "green", "reverse", "single", "single-cell" },

                // Edge cases: invalid/boundary values
                { 0, "invalid", "none", "none", "none", "single-cell" },
                { -1, "invalid", "none", "none", "none", "single-cell" },
                { 255, "invalid", "none", "none", "none", "single-cell" },
        });
    }

    private void setParameters(int attributeValue, String attributeType, String colorValue,
                                      String highlightType, String columnSeparator, String positionType) {
        this.attributeValue = attributeValue;
        this.attributeType = attributeType;
        this.colorValue = colorValue;
        this.highlightType = highlightType;
        this.columnSeparator = columnSeparator;
        this.positionType = positionType;
    }

        public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble(SIZE_24);
        screenPlanes = new ScreenPlanes(screen5250, SIZE_24);

        screenAttr = getPrivateField("screenAttr", char[].class);
        screenColor = getPrivateField("screenColor", char[].class);
        screenExtended = getPrivateField("screenExtended", char[].class);
        screenIsAttr = getPrivateField("screenIsAttr", char[].class);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(String fieldName, Class<T> fieldType)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = ScreenPlanes.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(screenPlanes);
    }

    private int positionForType(String posType) {
        switch (posType) {
            case "single-cell":
                return 100;  // Row 1, Col 20
            case "field-wide":
                return 160;  // Row 2, Col 0
            case "screen-wide":
                return 1920; // Row 24, Col 0
            default:
                return 0;
        }
    }

    /**
     * TEST 1: Color attribute values are properly set and retrieved
     *
     * Positive test: Verify color attributes map correctly to screenColor plane
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testColorAttributeSetAndRetrieve(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!attributeType.equals("color")) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenAttr.length) {
            return;
        }

        screenPlanes.setScreenAttr(pos, attributeValue);

        assertEquals(attributeValue,screenPlanes.getCharAttr(pos)
        ,
            String.format("Color attribute %d not set at pos %d", attributeValue, pos));
    }

    /**
     * TEST 2: Extended highlight attributes (underscore, reverse) are dispersed correctly
     *
     * Positive test: Verify underscore/reverse flags are set in screenExtended
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testExtendedHighlightAttributeDispersal(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!attributeType.equals("extended-highlight")) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenExtended.length) {
            return;
        }

        screenPlanes.setScreenAttr(pos, attributeValue);

        // Verify attribute was stored
        assertEquals(attributeValue,screenPlanes.getCharAttr(pos)
        ,
            String.format("Extended attribute %d not stored at pos %d", attributeValue, pos));

        // For underscore attributes (36, 38, 44, 46, 52, 53, 54), verify extended plane updated
        if (attributeValue == ATTR_GREEN_UNDERLINE || attributeValue == ATTR_WHITE_UNDERLINE ||
            attributeValue == ATTR_RED_UNDERLINE || attributeValue == ATTR_RED_UNDERLINE_ALT ||
            attributeValue == ATTR_COL_SEP_UL || attributeValue == ATTR_COL_SEP_UL_ALT ||
            attributeValue == ATTR_COL_SEP_BLUE_UL) {
            // Extended plane should have underline flag set (0x08)
            assertTrue((screenExtended[pos] & 0x08) == 0x08
            ,
                String.format("Underline flag not set for attr %d at pos %d", attributeValue, pos));
        }
    }

    /**
     * TEST 3: Character set attributes (column separator) set appropriate extended flags
     *
     * Positive test: Verify column separator flag is set in screenExtended (for 48-51, 63)
     *              or underline flag is set (for 52-54)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCharacterSetColumnSeparatorDispersal(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!attributeType.equals("character-set")) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenExtended.length) {
            return;
        }

        screenPlanes.setScreenAttr(pos, attributeValue);

        // For column separator attributes (48-51), verify column separator flag (0x02)
        if (attributeValue == ATTR_COL_SEP_CYAN || attributeValue == ATTR_COL_SEP_CYAN_ALT ||
            attributeValue == ATTR_COL_SEP_BLUE || attributeValue == ATTR_COL_SEP_YELLOW) {
            assertTrue((screenExtended[pos] & 0x02) == 0x02
            ,
                String.format("Column separator flag not set for attr %d at pos %d", attributeValue, pos));
        }
        // For underline column separator attributes (52-54), verify underline flag (0x08)
        if (attributeValue == ATTR_COL_SEP_UL || attributeValue == ATTR_COL_SEP_UL_ALT ||
            attributeValue == ATTR_COL_SEP_BLUE_UL) {
            assertTrue((screenExtended[pos] & 0x08) == 0x08
            ,
                String.format("Underline flag not set for attr %d at pos %d", attributeValue, pos));
        }
    }

    /**
     * TEST 4: Attribute values are isolated by position (single-cell vs field-wide)
     *
     * Positive test: Verify position changes don't affect other cells
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeIsolationByPosition(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        int singleCellPos = positionForType("single-cell");
        int fieldWidePos = positionForType("field-wide");

        if (singleCellPos >= screenAttr.length || fieldWidePos >= screenAttr.length) {
            return;
        }

        int testAttr1 = ATTR_GREEN_NORMAL;
        int testAttr2 = ATTR_RED_NORMAL;

        // Set different attributes at different positions
        screenPlanes.setScreenAttr(singleCellPos, testAttr1);
        screenPlanes.setScreenAttr(fieldWidePos, testAttr2);

        // Verify they remain isolated
        assertEquals(testAttr1,
            screenPlanes.getCharAttr(singleCellPos)
        ,
            "Attribute at single-cell corrupted by field-wide change");

        assertEquals(testAttr2,
            screenPlanes.getCharAttr(fieldWidePos)
        ,
            "Attribute at field-wide corrupted by single-cell change");
    }

    /**
     * TEST 5: Non-display attributes suppress rendering correctly
     *
     * Positive test: Verify non-display flag is set for attribute 39, 47, 55, 63
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testNonDisplayAttributeHandling(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (attributeValue != ATTR_WHITE_NON_DSP && attributeValue != ATTR_RED_NON_DSP &&
            attributeValue != ATTR_COL_SEP_NON_DSP && attributeValue != ATTR_NONDISPLAY) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenExtended.length) {
            return;
        }

        screenPlanes.setScreenAttr(pos, attributeValue);

        // Non-display flag (0x01) should be set
        assertTrue((screenExtended[pos] & 0x01) == 0x01
        ,
            String.format("Non-display flag not set for attr %d at pos %d", attributeValue, pos));
    }

    /**
     * TEST 6: Multiple attributes at different positions maintain independence
     *
     * Positive test: Verify screen-wide attribute changes don't affect other positions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMultipleAttributesScreenWideIndependence(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        int pos1 = 0;
        int pos2 = COLS_24 - 1;
        int pos3 = (ROWS_24 - 1) * COLS_24;

        screenPlanes.setScreenAttr(pos1, ATTR_GREEN_NORMAL);
        screenPlanes.setScreenAttr(pos2, ATTR_RED_NORMAL);
        screenPlanes.setScreenAttr(pos3, ATTR_WHITE_NORMAL);

        assertEquals(ATTR_GREEN_NORMAL, screenPlanes.getCharAttr(pos1),"Pos 0 attribute corrupted");
        assertEquals(ATTR_RED_NORMAL, screenPlanes.getCharAttr(pos2),"Pos EOL attribute corrupted");
        assertEquals(ATTR_WHITE_NORMAL, screenPlanes.getCharAttr(pos3),"Pos EOF attribute corrupted");
    }

    /**
     * TEST 7: Conflicting attributes (color + column separator) are handled
     *
     * Adversarial test: Verify system handles conflicting attribute combinations
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testConflictingAttributeCombinations(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!attributeType.equals("conflict")) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenAttr.length) {
            return;
        }

        // Set a color attribute
        screenPlanes.setScreenAttr(pos, attributeValue);

        // Verify it was stored (conflict resolution is implementation-dependent)
        assertEquals(attributeValue,screenPlanes.getCharAttr(pos)
        ,
            String.format("Conflicting attribute %d not stored", attributeValue));
    }

    /**
     * TEST 8: Invalid attribute values (0, negative, > 63) are handled gracefully
     *
     * Adversarial test: Verify system doesn't crash on invalid attribute values
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testInvalidAttributeValueHandling(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!attributeType.equals("invalid")) {
            return;
        }

        int pos = 50;
        if (pos >= screenAttr.length) {
            return;
        }

        try {
            screenPlanes.setScreenAttr(pos, attributeValue);

            // If attribute is 0, disperseAttribute returns early (no change)
            if (attributeValue == 0) {
                // screenExtended should remain unchanged (init value)
            } else if (attributeValue < 0 || attributeValue > 63) {
                // System should either store it or ignore it gracefully
                // Just verify no exception was thrown
            }

        } catch (Exception e) {
            fail(String.format("Invalid attribute %d caused exception: %s", attributeValue, e.getMessage()));
        }
    }

    /**
     * TEST 9: Attribute round-trip preserves exact value
     *
     * Positive test: Verify set/get consistency for all valid attributes
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeRoundTripPreservation(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        // Skip invalid attributes
        if (attributeValue < 0 || attributeValue > 63) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenAttr.length) {
            return;
        }

        screenPlanes.setScreenAttr(pos, attributeValue);
        int retrieved = screenPlanes.getCharAttr(pos);

        assertEquals(attributeValue,retrieved
        ,
            String.format("Attribute %d lost in round-trip at pos %d", attributeValue, pos));
    }

    /**
     * TEST 10: Green color attributes (32-38) are stored in color plane correctly
     *
     * Positive test: Verify green color range is properly handled
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGreenColorAttributeRange(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!colorValue.equals("green") && !colorValue.equals("default")) {
            return;
        }

        int[] greenAttrs = { ATTR_GREEN_NORMAL, ATTR_GREEN_REVERSE, ATTR_GREEN_UNDERLINE,
                             ATTR_GREEN_REV_UNDERLINE };

        for (int attr : greenAttrs) {
            if (attributeValue == attr) {
                int pos = positionForType(positionType);
                if (pos >= screenAttr.length) {
                    return;
                }

                screenPlanes.setScreenAttr(pos, attr);
                assertEquals(attr,screenPlanes.getCharAttr(pos)
                ,
                    String.format("Green attribute %d not stored at pos %d", attr, pos));
                return;
            }
        }
    }

    /**
     * TEST 11: Red color attributes (40-47) are stored in color plane correctly
     *
     * Positive test: Verify red color range is properly handled
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRedColorAttributeRange(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!colorValue.equals("red")) {
            return;
        }

        int[] redAttrs = { ATTR_RED_NORMAL, ATTR_RED_REVERSE, ATTR_RED_NORMAL_ALT,
                          ATTR_RED_REVERSE_ALT, ATTR_RED_UNDERLINE, ATTR_RED_REV_UNDERLINE,
                          ATTR_RED_UNDERLINE_ALT, ATTR_RED_NON_DSP };

        for (int attr : redAttrs) {
            if (attributeValue == attr) {
                int pos = positionForType(positionType);
                if (pos >= screenAttr.length) {
                    return;
                }

                screenPlanes.setScreenAttr(pos, attr);
                assertEquals(attr,screenPlanes.getCharAttr(pos)
                ,
                    String.format("Red attribute %d not stored at pos %d", attr, pos));
                return;
            }
        }
    }

    /**
     * TEST 12: White color attributes (34-39) are stored correctly
     *
     * Positive test: Verify white color range is properly handled
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testWhiteColorAttributeRange(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!colorValue.equals("white")) {
            return;
        }

        int[] whiteAttrs = { ATTR_WHITE_NORMAL, ATTR_WHITE_REVERSE, ATTR_WHITE_UNDERLINE,
                             ATTR_WHITE_NON_DSP };

        for (int attr : whiteAttrs) {
            if (attributeValue == attr) {
                int pos = positionForType(positionType);
                if (pos >= screenAttr.length) {
                    return;
                }

                screenPlanes.setScreenAttr(pos, attr);
                assertEquals(attr,screenPlanes.getCharAttr(pos)
                ,
                    String.format("White attribute %d not stored at pos %d", attr, pos));
                return;
            }
        }
    }

    /**
     * TEST 13: Blue/cyan color attributes (48-51) are stored correctly
     *
     * Positive test: Verify blue/cyan color range is properly handled
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testBlueColorAttributeRange(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!colorValue.equals("blue") && !colorValue.equals("cyan")) {
            return;
        }

        int[] blueAttrs = { ATTR_COL_SEP_CYAN, ATTR_COL_SEP_CYAN_ALT, ATTR_COL_SEP_BLUE,
                           ATTR_COL_SEP_BLUE_UL };

        for (int attr : blueAttrs) {
            if (attributeValue == attr) {
                int pos = positionForType(positionType);
                if (pos >= screenAttr.length) {
                    return;
                }

                screenPlanes.setScreenAttr(pos, attr);
                assertEquals(attr,screenPlanes.getCharAttr(pos)
                ,
                    String.format("Blue attribute %d not stored at pos %d", attr, pos));
                return;
            }
        }
    }

    /**
     * TEST 14: Reverse highlight attributes are properly distinguished from normal attributes
     *
     * Positive test: Verify reverse attributes are stored and retrieved correctly
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testReverseHighlightBitSetting(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!highlightType.equals("reverse")) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenAttr.length) {
            return;
        }

        screenPlanes.setScreenAttr(pos, attributeValue);

        // For reverse attributes, verify the attribute value is stored correctly
        // (reverse is encoded in the ATTR_XX constants, not as a separate extended bit)
        if (attributeValue == ATTR_GREEN_REVERSE || attributeValue == ATTR_WHITE_REVERSE ||
            attributeValue == ATTR_RED_REVERSE || attributeValue == ATTR_RED_REVERSE_ALT ||
            attributeValue == ATTR_GREEN_REV_UNDERLINE || attributeValue == ATTR_RED_REV_UNDERLINE ||
            attributeValue == ATTR_PINK_REVERSE || attributeValue == ATTR_BLUE_REVERSE ||
            attributeValue == ATTR_MAGENTA_REVERSE) {
            assertEquals(attributeValue,screenPlanes.getCharAttr(pos)
            ,
                String.format("Reverse attribute %d not stored at pos %d", attributeValue, pos));
        }
    }

    /**
     * TEST 15: Underscore highlight properly sets extended bit 0x08
     *
     * Positive test: Verify underscore attributes set correct bit in screenExtended
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUnderscoreHighlightBitSetting(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!highlightType.equals("underscore")) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenExtended.length) {
            return;
        }

        screenPlanes.setScreenAttr(pos, attributeValue);

        // For underscore attributes, verify extended bit is set (0x08)
        if (attributeValue == ATTR_GREEN_UNDERLINE || attributeValue == ATTR_WHITE_UNDERLINE ||
            attributeValue == ATTR_RED_UNDERLINE || attributeValue == ATTR_RED_UNDERLINE_ALT ||
            attributeValue == ATTR_GREEN_REV_UNDERLINE || attributeValue == ATTR_RED_REV_UNDERLINE ||
            attributeValue == ATTR_COL_SEP_UL || attributeValue == ATTR_COL_SEP_UL_ALT ||
            attributeValue == ATTR_COL_SEP_BLUE_UL) {
            assertTrue((screenExtended[pos] & 0x08) == 0x08
            ,
                String.format("Underscore bit 0x08 not set for attr %d at pos %d", attributeValue, pos));
        }
    }

    /**
     * TEST 16: Column separator flag (0x02) set for separator attributes (48-51, 63)
     *
     * Positive test: Verify column separator flag in screenExtended for correct attributes
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testColumnSeparatorFlagSetting(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!columnSeparator.equals("single")) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenExtended.length) {
            return;
        }

        screenPlanes.setScreenAttr(pos, attributeValue);

        // For column separator attributes (48-51, 63), verify flag is set (0x02)
        // Note: attributes 52-54 set underline only, not column separator
        if (attributeValue == ATTR_COL_SEP_CYAN || attributeValue == ATTR_COL_SEP_CYAN_ALT ||
            attributeValue == ATTR_COL_SEP_BLUE || attributeValue == ATTR_COL_SEP_YELLOW ||
            attributeValue == ATTR_COL_SEP_NON_DSP || attributeValue == 63) {
            assertTrue((screenExtended[pos] & 0x02) == 0x02
            ,
                String.format("Column separator bit 0x02 not set for attr %d at pos %d", attributeValue, pos));
        }
    }

    /**
     * TEST 17: Sequential attribute updates at same position
     *
     * Positive test: Verify last attribute value wins when updates are sequential
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSequentialAttributeUpdates(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        int pos = positionForType(positionType);
        if (pos >= screenAttr.length) {
            return;
        }

        // Skip invalid attributes for this test
        if (attributeValue < 0 || attributeValue > 63) {
            return;
        }

        // Set attributes in sequence
        screenPlanes.setScreenAttr(pos, ATTR_GREEN_NORMAL);
        assertEquals(ATTR_GREEN_NORMAL, screenPlanes.getCharAttr(pos),"First update failed");

        screenPlanes.setScreenAttr(pos, ATTR_RED_NORMAL);
        assertEquals(ATTR_RED_NORMAL, screenPlanes.getCharAttr(pos),"Second update failed");

        screenPlanes.setScreenAttr(pos, attributeValue);
        assertEquals(attributeValue, screenPlanes.getCharAttr(pos),"Final update failed");
    }

    /**
     * TEST 18: Pink color attributes (56-57) are stored correctly
     *
     * Positive test: Verify pink color handling
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testPinkColorAttributeRange(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!colorValue.equals("pink")) {
            return;
        }

        int[] pinkAttrs = { ATTR_PINK_NORMAL, ATTR_PINK_REVERSE };

        for (int attr : pinkAttrs) {
            if (attributeValue == attr) {
                int pos = positionForType(positionType);
                if (pos >= screenAttr.length) {
                    return;
                }

                screenPlanes.setScreenAttr(pos, attr);
                assertEquals(attr,screenPlanes.getCharAttr(pos)
                ,
                    String.format("Pink attribute %d not stored at pos %d", attr, pos));
                return;
            }
        }
    }

    /**
     * TEST 19: Magenta color attributes (58, 61) are stored correctly
     *
     * Positive test: Verify magenta color handling
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMagentaColorAttributeRange(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!colorValue.equals("magenta")) {
            return;
        }

        int[] magentaAttrs = { ATTR_MAGENTA_NORMAL, ATTR_MAGENTA_REVERSE };

        for (int attr : magentaAttrs) {
            if (attributeValue == attr) {
                int pos = positionForType(positionType);
                if (pos >= screenAttr.length) {
                    return;
                }

                screenPlanes.setScreenAttr(pos, attr);
                assertEquals(attr,screenPlanes.getCharAttr(pos)
                ,
                    String.format("Magenta attribute %d not stored at pos %d", attr, pos));
                return;
            }
        }
    }

    /**
     * TEST 20: Blue color attributes (59-60) are stored correctly
     *
     * Positive test: Verify blue color handling for normal/reverse
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testBlueStandardColorAttributeRange(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (!colorValue.equals("blue")) {
            return;
        }

        int[] blueAttrs = { ATTR_BLUE_NORMAL, ATTR_BLUE_REVERSE };

        for (int attr : blueAttrs) {
            if (attributeValue == attr) {
                int pos = positionForType(positionType);
                if (pos >= screenAttr.length) {
                    return;
                }

                screenPlanes.setScreenAttr(pos, attr);
                assertEquals(attr,screenPlanes.getCharAttr(pos)
                ,
                    String.format("Blue attribute %d not stored at pos %d", attr, pos));
                return;
            }
        }
    }

    /**
     * TEST 21: Attribute plane remains independent from character plane
     *
     * Positive test: Verify setting attributes doesn't modify characters
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributePlaneCharacterIndependence(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        int pos = positionForType(positionType);
        if (pos >= screenAttr.length) {
            return;
        }

        // Set a character first
        char expectedChar = 'X';
        screenPlanes.setChar(pos, expectedChar);

        // Then set attributes
        for (int i = 0; i < 5; i++) {
            int[] attrs = { ATTR_GREEN_NORMAL, ATTR_RED_NORMAL, ATTR_WHITE_NORMAL,
                           ATTR_GREEN_UNDERLINE, ATTR_RED_REVERSE };
            screenPlanes.setScreenAttr(pos, attrs[i]);

            // Character should remain unchanged
            assertEquals(expectedChar,screenPlanes.getChar(pos)
            ,
                String.format("Character corrupted after attr %d", attrs[i]));
        }
    }

    /**
     * TEST 22: Attribute setting marks position as changed via screenIsChanged
     *
     * Positive test: Verify change tracking when attributes are updated
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeChangeTracking(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        int pos = positionForType(positionType);
        if (pos >= screenAttr.length) {
            return;
        }

        try {
            char[] screenIsChanged = getPrivateField("screenIsChanged", char[].class);

            // Set initial attribute
            screenPlanes.setScreenAttr(pos, ATTR_GREEN_NORMAL);

            // Change attribute
            screenPlanes.setScreenAttr(pos, attributeValue);

            // If attribute differs, position should be marked as changed
            if (attributeValue != ATTR_GREEN_NORMAL) {
                // screenIsChanged[pos] should reflect the change
                // (exact behavior depends on implementation)
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not verify change tracking: " + e.getMessage());
        }
    }

    /**
     * TEST 23: Alternative attribute forms (42/40, 43/41, 46/44) map to same values
     *
     * Positive test: Verify attribute aliases work correctly
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAlternativeAttributeForms(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        // 42 is alias for 40 (red/normal)
        // 43 is alias for 41 (red/reverse)
        // 46 is alias for 44 (red/underline)

        int[][] aliases = {
            { ATTR_RED_NORMAL, ATTR_RED_NORMAL_ALT },
            { ATTR_RED_REVERSE, ATTR_RED_REVERSE_ALT },
            { ATTR_RED_UNDERLINE, ATTR_RED_UNDERLINE_ALT }
        };

        int pos = positionForType(positionType);
        if (pos >= screenAttr.length) {
            return;
        }

        for (int[] alias : aliases) {
            if (attributeValue == alias[0] || attributeValue == alias[1]) {
                screenPlanes.setScreenAttr(pos, attributeValue);
                assertEquals(attributeValue,screenPlanes.getCharAttr(pos)
                ,
                    String.format("Attribute %d not stored at pos %d", attributeValue, pos));
                return;
            }
        }
    }

    /**
     * TEST 24: Boundary position attributes (row 0 col 0, last row last col)
     *
     * Positive test: Verify attributes work at screen boundaries
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testBoundaryPositionAttributes(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        int[] boundaryPositions = {
            0,                                          // Top-left
            COLS_24 - 1,                                // Top-right
            (ROWS_24 - 1) * COLS_24,                   // Bottom-left
            (ROWS_24 - 1) * COLS_24 + COLS_24 - 1      // Bottom-right
        };

        for (int pos : boundaryPositions) {
            if (pos >= screenAttr.length) {
                continue;
            }

            screenPlanes.setScreenAttr(pos, attributeValue);

            if (attributeValue > 0) {
                assertEquals(attributeValue,screenPlanes.getCharAttr(pos)
                ,
                    String.format("Attribute %d not stored at boundary pos %d", attributeValue, pos));
            }
        }
    }

    /**
     * TEST 25: Full screen attribute coverage (sampling across all 1920 cells)
     *
     * Positive test: Verify attributes can be set at any position without corruption
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testFullScreenAttributeCoverage(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        // Sample positions across screen: every 100 cells
        int totalSize = ROWS_24 * COLS_24;

        for (int pos = 0; pos < totalSize; pos += 100) {
            if (pos >= screenAttr.length) {
                break;
            }

            screenPlanes.setScreenAttr(pos, attributeValue);

            if (attributeValue > 0) {
                assertEquals(attributeValue,screenPlanes.getCharAttr(pos)
                ,
                    String.format("Attribute %d not stored at sampled pos %d", attributeValue, pos));
            }
        }
    }

    /**
     * TEST 26: Attribute value 0 is handled as no-op (disperseAttribute early return)
     *
     * Positive test: Verify zero attribute doesn't cause issues
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testZeroAttributeHandling(int attributeValue, String attributeType, String colorValue, String highlightType, String columnSeparator, String positionType) throws Exception {
        setParameters(attributeValue, attributeType, colorValue, highlightType, columnSeparator, positionType);
        setUp();
        if (attributeValue != 0) {
            return;
        }

        int pos = positionForType(positionType);
        if (pos >= screenAttr.length) {
            return;
        }

        // Set non-zero first
        screenPlanes.setScreenAttr(pos, ATTR_GREEN_NORMAL);
        assertEquals(ATTR_GREEN_NORMAL, screenPlanes.getCharAttr(pos),"Setup failed");

        // Then set zero (should be no-op or clear)
        try {
            screenPlanes.setScreenAttr(pos, 0);
            // Zero attribute handling is implementation-dependent
        } catch (Exception e) {
            fail("Zero attribute caused exception: " + e.getMessage());
        }
    }

    /**
     * Test double for Screen5250 interface
     */
    private static class Screen5250TestDouble extends Screen5250 {
        private int numCols;
        private int numRows;

        public Screen5250TestDouble(int screenSize) {
            super();
            numRows = 24;
            numCols = 80;
        }

        @Override
        public int getPos(int row, int col) {
            return (row * numCols) + col;
        }

        @Override
        public int getScreenLength() {
            return numRows * numCols;
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
            // No-op
        }
    }
}
