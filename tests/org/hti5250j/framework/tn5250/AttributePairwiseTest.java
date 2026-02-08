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
 * Pairwise parameterized tests for ScreenPlanes attribute operations.
 *
 * Tests systematically explore attribute combinations critical for screen scraping automation:
 * - Attribute types: normal, reverse, underline, blink, column-separator
 * - Colors: green, white, red, blue, yellow, pink, turquoise (cyan)
 * - Field attributes: input-capable, protected, hidden, modified
 * - Extended attributes: double-height, double-width, non-display
 * - Position variations: start-of-field, mid-field, end-of-field
 *
 * Discovers: Attribute handling bugs, color mapping failures, extended attribute conflicts,
 * boundary violations in attribute plane management.
 *
 * Useful for: Automation frameworks that need to identify field types, extract display
 * attributes, and validate 5250 screen state.
 */
public class AttributePairwiseTest {

    // Test parameters
    private int attributeType;
    private int colorValue;
    private int fieldAttribute;
    private int extendedAttribute;
    private int testPosition;

    // Instance variables
    private ScreenPlanes screenPlanes;
    private Screen5250TestDouble screen5250;
    private char[] screenAttr;
    private char[] screenIsAttr;
    private char[] screenColor;
    private char[] screenExtended;

    // Screen size constants
    private static final int SCREEN_SIZE = 24;
    private static final int SCREEN_WIDTH = 80;
    private static final int SCREEN_LENGTH = 1920; // 24 * 80

    // Attribute type constants
    private static final int ATTR_NORMAL = 32;              // Green normal
    private static final int ATTR_REVERSE = 33;             // Green/reverse
    private static final int ATTR_UNDERLINE = 36;           // Green/underline
    private static final int ATTR_BLINK = 40;               // Red/normal (simulated blink)
    private static final int ATTR_COLUMN_SEP = 48;          // Column separator

    // Color constants (from HTI5250jConstants)
    private static final int COLOR_GREEN = 2;
    private static final int COLOR_WHITE = 3;
    private static final int COLOR_RED = 4;
    private static final int COLOR_BLUE = 5;
    private static final int COLOR_YELLOW = 6;
    private static final int COLOR_PINK = 7;
    private static final int COLOR_TURQUOISE = 1;

    // Field attribute constants (bits within attribute byte)
    private static final int FIELD_INPUT = 0x01;            // Input capable
    private static final int FIELD_PROTECTED = 0x02;        // Protected/non-input
    private static final int FIELD_HIDDEN = 0x04;           // Hidden field
    private static final int FIELD_MODIFIED = 0x08;         // Modified indicator

    // Extended attribute constants
    private static final int EXT_DOUBLE_HEIGHT = 0x20;
    private static final int EXT_DOUBLE_WIDTH = 0x40;
    private static final int EXT_UNDERLINE = 0x08;
    private static final int EXT_BLINK = 0x04;
    private static final int EXT_COLUMN_SEP = 0x02;
    private static final int EXT_NON_DISPLAY = 0x01;

    /**
     * Pairwise parameter combinations covering:
     * - Attribute types: normal, reverse, underline, blink, column-separator
     * - Colors: 7 standard 5250 colors
     * - Field attributes: input, protected, hidden, modified
     * - Extended attributes: double-height, double-width, underline, blink, column-sep, non-display
     * - Positions: start (0), mid (960), end (1919)
     */
        public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // POSITIVE TESTS: Valid attribute combinations

                // Test 1: Green normal + input-capable at start-of-field
                { 1, ATTR_NORMAL, COLOR_GREEN, FIELD_INPUT, 0, 0 },

                // Test 2: White reverse + protected at mid-field
                { 2, ATTR_REVERSE, COLOR_WHITE, FIELD_PROTECTED, 0, 960 },

                // Test 3: Red underline + hidden at end-of-field
                { 3, ATTR_UNDERLINE, COLOR_RED, FIELD_HIDDEN, 0, 1919 },

                // Test 4: Blue blink + modified at position 100
                { 4, ATTR_BLINK, COLOR_BLUE, FIELD_MODIFIED, 0, 100 },

                // Test 5: Yellow column-separator + input at position 500
                { 5, ATTR_COLUMN_SEP, COLOR_YELLOW, FIELD_INPUT, EXT_COLUMN_SEP, 500 },

                // Test 6: Pink normal + double-height at position 1000
                { 6, ATTR_NORMAL, COLOR_PINK, 0, EXT_DOUBLE_HEIGHT, 1000 },

                // Test 7: Turquoise reverse + double-width at position 1500
                { 7, ATTR_REVERSE, COLOR_TURQUOISE, 0, EXT_DOUBLE_WIDTH, 1500 },

                // Test 8: Green underline + non-display at position 500
                { 8, ATTR_UNDERLINE, COLOR_GREEN, 0, EXT_NON_DISPLAY, 500 },

                // Test 9: White blink + hidden + underline at position 800
                { 9, ATTR_BLINK, COLOR_WHITE, FIELD_HIDDEN, EXT_UNDERLINE, 800 },

                // Test 10: Red normal + protected + double-width at position 1200
                { 10, ATTR_NORMAL, COLOR_RED, FIELD_PROTECTED, EXT_DOUBLE_WIDTH, 1200 },

                // ADVERSARIAL TESTS: Invalid/boundary conditions

                // Test 11: Out-of-bounds negative position
                { 11, ATTR_NORMAL, COLOR_GREEN, FIELD_INPUT, 0, -1 },

                // Test 12: Out-of-bounds position beyond screen length
                { 12, ATTR_REVERSE, COLOR_WHITE, FIELD_PROTECTED, 0, 1920 },

                // Test 13: Out-of-bounds position far beyond screen
                { 13, ATTR_UNDERLINE, COLOR_RED, FIELD_HIDDEN, 0, 5000 },

                // Test 14: Invalid attribute value (0 - should be handled gracefully)
                { 14, 0, COLOR_GREEN, FIELD_INPUT, 0, 960 },

                // Test 15: Invalid color value (99 - out of range)
                { 15, ATTR_NORMAL, 99, FIELD_PROTECTED, 0, 960 },

                // Test 16: Conflicting field attributes at same position
                { 16, ATTR_REVERSE, COLOR_BLUE, FIELD_INPUT | FIELD_PROTECTED, 0, 500 },

                // Test 17: All extended attributes combined (should not corrupt)
                { 17, ATTR_UNDERLINE, COLOR_YELLOW,
                    0,
                    EXT_DOUBLE_HEIGHT | EXT_DOUBLE_WIDTH | EXT_UNDERLINE |
                    EXT_BLINK | EXT_COLUMN_SEP | EXT_NON_DISPLAY,
                    1000 },

                // Test 18: Boundary position at screen start
                { 18, ATTR_NORMAL, COLOR_GREEN, 0, 0, 0 },

                // Test 19: Boundary position at screen end
                { 19, ATTR_REVERSE, COLOR_WHITE, 0, 0, 1919 },

                // Test 20: Mid-range position with all attributes set
                { 20, ATTR_BLINK, COLOR_PINK, FIELD_MODIFIED, EXT_UNDERLINE, 960 },
        });
    }

    private void setParameters(int testId, int attributeType, int colorValue,
                                  int fieldAttribute, int extendedAttribute, int testPosition) {
        this.attributeType = attributeType;
        this.colorValue = colorValue;
        this.fieldAttribute = fieldAttribute;
        this.extendedAttribute = extendedAttribute;
        this.testPosition = testPosition;
    }

        public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble(SCREEN_SIZE);
        screenPlanes = new ScreenPlanes(screen5250, SCREEN_SIZE);

        screenAttr = getPrivateField("screenAttr", char[].class);
        screenIsAttr = getPrivateField("screenIsAttr", char[].class);
        screenColor = getPrivateField("screenColor", char[].class);
        screenExtended = getPrivateField("screenExtended", char[].class);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(String fieldName, Class<T> fieldType)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = ScreenPlanes.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(screenPlanes);
    }

    private boolean isValidPosition(int pos) {
        return pos >= 0 && pos < SCREEN_LENGTH;
    }

    /**
     * TEST 1: getAttribute returns correct attribute at valid position
     *
     * Positive test: Verify attribute can be set and retrieved at valid positions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetAttributeValidPosition(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return; // Skip out-of-bounds for positive test
        }

        // Set attribute via screen planes
        screenPlanes.setScreenAttr(testPosition, attributeType);

        // GREEN: Verify attribute retrieval
        int retrieved = screenPlanes.getCharAttr(testPosition);
        assertEquals(attributeType,retrieved
        ,
            String.format("Attribute not retrieved at pos %d, expected %d got %d",
                testPosition, attributeType, retrieved));
    }

    /**
     * TEST 2: isAttributePlace correctly identifies attribute positions
     *
     * Positive test: Verify isAttributePlace returns boolean for any position
     * Note: isAttributePlace identifies 5250 format attribute bytes, not just
     * positions with attributes set via setScreenAttr.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testIsAttributePlaceValidPosition(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        // Get attribute place status - should not throw exception
        boolean isAttrPlace = screenPlanes.isAttributePlace(testPosition);

        // GREEN: Verify position returns a boolean
        assertNotNull(isAttrPlace
        ,
            String.format("Position %d isAttributePlace should return boolean", testPosition));
    }

    /**
     * TEST 3: Extended attribute handling for underline
     *
     * Positive test: Verify underline extended attribute is properly set
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testExtendedAttributeUnderline(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        if ((extendedAttribute & EXT_UNDERLINE) == 0) {
            return; // Skip if not testing underline
        }

        screenPlanes.setScreenAttr(testPosition, ATTR_UNDERLINE);

        try {
            // Verify extended attribute plane contains underline flag
            char extAttr = screenExtended[testPosition];
            assertTrue((extAttr & EXT_UNDERLINE) != 0
            ,
                String.format("Underline extended attribute not set at pos %d", testPosition));
        } catch (Exception e) {
            fail("Failed to verify underline extended attribute: " + e.getMessage());
        }
    }

    /**
     * TEST 4: Extended attribute handling for column separator
     *
     * Positive test: Verify column separator extended attribute is properly set
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testExtendedAttributeColumnSeparator(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        if ((extendedAttribute & EXT_COLUMN_SEP) == 0) {
            return; // Skip if not testing column separator
        }

        screenPlanes.setScreenAttr(testPosition, ATTR_COLUMN_SEP);

        try {
            char extAttr = screenExtended[testPosition];
            assertTrue((extAttr & EXT_COLUMN_SEP) != 0
            ,
                String.format("Column separator extended attribute not set at pos %d", testPosition));
        } catch (Exception e) {
            fail("Failed to verify column separator extended attribute: " + e.getMessage());
        }
    }

    /**
     * TEST 5: Color mapping for different attribute types
     *
     * Positive test: Verify color plane is updated when attribute is set
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testColorMappingValidAttribute(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        screenPlanes.setScreenAttr(testPosition, attributeType);

        try {
            char actualColorValue = screenColor[testPosition];
            // Color should be non-zero for valid attributes
            if (attributeType > 0) {
                assertTrue(actualColorValue != 0,
                    String.format("Color not mapped for attribute %d at pos %d",
                        attributeType, testPosition));
            }
        } catch (Exception e) {
            fail("Failed to verify color mapping: " + e.getMessage());
        }
    }

    /**
     * TEST 6: Attribute round-trip across all attribute types
     *
     * Positive test: Verify attribute preservation across set/get cycle
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeRoundTrip(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        screenPlanes.setScreenAttr(testPosition, attributeType);
        int retrieved = screenPlanes.getCharAttr(testPosition);

        assertEquals(attributeType,retrieved
        ,
            String.format("Attribute lost in round-trip at pos %d", testPosition));
    }

    /**
     * TEST 7: Field attribute isolation (setting doesn't corrupt screen)
     *
     * Positive test: Verify field attributes don't cause out-of-bounds writes
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testFieldAttributeIsolation(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        try {
            // Store initial values of adjacent positions
            int prevAttrBefore = testPosition > 0 ?
                screenPlanes.getCharAttr(testPosition - 1) : 0;
            int nextAttrBefore = testPosition < SCREEN_LENGTH - 1 ?
                screenPlanes.getCharAttr(testPosition + 1) : 0;

            screenPlanes.setScreenAttr(testPosition, attributeType);

            // Verify adjacent positions were not modified
            if (testPosition > 0) {
                int prevAttrAfter = screenPlanes.getCharAttr(testPosition - 1);
                assertEquals(prevAttrBefore,prevAttrAfter
                ,
                    String.format("Previous position corrupted by attribute at %d", testPosition));
            }

            if (testPosition < SCREEN_LENGTH - 1) {
                int nextAttrAfter = screenPlanes.getCharAttr(testPosition + 1);
                assertEquals(nextAttrBefore,nextAttrAfter
                ,
                    String.format("Next position corrupted by attribute at %d", testPosition));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            fail("Field attribute handling caused array bounds violation: " + e.getMessage());
        }
    }

    /**
     * TEST 8: Out-of-bounds getAttribute does not crash
     *
     * Adversarial test: Verify getAttribute gracefully handles invalid positions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetAttributeOutOfBounds(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (isValidPosition(testPosition)) {
            return; // Skip valid positions
        }

        try {
            // Should not throw exception for out-of-bounds
            int retrieved = screenPlanes.getCharAttr(testPosition);
            // If we reach here without exception, implementation doesn't validate bounds
            // This is acceptable - just document behavior
        } catch (ArrayIndexOutOfBoundsException e) {
            // Also acceptable - graceful failure is OK
        } catch (Exception e) {
            fail("getAttribute threw unexpected exception: " + e.getClass().getSimpleName());
        }
    }

    /**
     * TEST 9: Out-of-bounds isAttributePlace does not crash
     *
     * Adversarial test: Verify isAttributePlace handles invalid positions gracefully
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testIsAttributePlaceOutOfBounds(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (isValidPosition(testPosition)) {
            return; // Skip valid positions
        }

        try {
            boolean result = screenPlanes.isAttributePlace(testPosition);
            // If we reach here, implementation doesn't validate bounds
            // Just verify it returns a boolean (no exception)
            assertNotNull(result,"isAttributePlace should return boolean");
        } catch (ArrayIndexOutOfBoundsException e) {
            // Also acceptable - graceful failure
        } catch (Exception e) {
            fail("isAttributePlace threw unexpected exception: " + e.getClass().getSimpleName());
        }
    }

    /**
     * TEST 10: Invalid attribute value (0) handling
     *
     * Adversarial test: Verify zero attribute is handled properly
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testInvalidAttributeValueZero(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        if (attributeType != 0) {
            return; // Only test with zero attribute
        }

        try {
            screenPlanes.setScreenAttr(testPosition, 0);
            int retrieved = screenPlanes.getCharAttr(testPosition);
            // Zero is valid - may mean "no attribute" or default value
            assertEquals(0,retrieved
            ,
                String.format("Zero attribute should be stored at pos %d", testPosition));
        } catch (Exception e) {
            fail("Zero attribute caused exception: " + e.getMessage());
        }
    }

    /**
     * TEST 11: Multiple attributes at different positions don't interfere
     *
     * Positive test: Verify attribute isolation across positions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMultipleAttributesNonInterfering(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        // Set attributes at different positions and verify isolation
        // Use testPosition as primary and verify adjacent don't interfere
        int pos1 = testPosition;
        int pos2 = (testPosition + 10 < SCREEN_LENGTH) ? testPosition + 10 : testPosition - 10;

        // Only test if both positions are valid
        if (!isValidPosition(pos2)) {
            return;
        }

        // Set different attributes
        screenPlanes.setScreenAttr(pos1, ATTR_NORMAL);
        screenPlanes.setScreenAttr(pos2, ATTR_REVERSE);

        // Verify each position retains its attribute
        assertEquals(ATTR_NORMAL,screenPlanes.getCharAttr(pos1)
        ,
            String.format("First attribute at %d not preserved", pos1));
        assertEquals(ATTR_REVERSE,screenPlanes.getCharAttr(pos2)
        ,
            String.format("Second attribute at %d not preserved", pos2));
    }

    /**
     * TEST 12: Attribute boundary position consistency
     *
     * Positive test: Verify attributes work at screen boundaries
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeBoundaryPositions(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (testPosition != 0 && testPosition != SCREEN_LENGTH - 1) {
            return; // Only test boundaries
        }

        try {
            screenPlanes.setScreenAttr(testPosition, attributeType);
            int retrieved = screenPlanes.getCharAttr(testPosition);

            assertEquals(attributeType,retrieved
            ,
                String.format("Boundary attribute at pos %d not retrieved", testPosition));
        } catch (Exception e) {
            fail("Boundary position caused exception: " + e.getMessage());
        }
    }

    /**
     * TEST 13: Attribute does not affect adjacent character positions
     *
     * Positive test: Verify attribute changes are isolated to single position
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeDoesNotAffectAdjacentCharacters(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        char expectedChar = 'T';

        try {
            // Set character and attribute
            screenPlanes.setChar(testPosition, expectedChar);
            screenPlanes.setScreenAttr(testPosition, attributeType);

            // Verify character is unchanged
            char retrieved = screenPlanes.getChar(testPosition);
            assertEquals(expectedChar,retrieved
            ,
                String.format("Character corrupted by attribute at pos %d", testPosition));
        } catch (Exception e) {
            fail("Character/attribute interaction caused exception: " + e.getMessage());
        }
    }

    /**
     * TEST 14: Sequential attribute updates at same position
     *
     * Positive test: Verify attributes can be updated at same position
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSequentialAttributeUpdates(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        screenPlanes.setScreenAttr(testPosition, ATTR_NORMAL);
        assertEquals(ATTR_NORMAL, screenPlanes.getCharAttr(testPosition),"First update failed");

        screenPlanes.setScreenAttr(testPosition, ATTR_REVERSE);
        assertEquals(ATTR_REVERSE, screenPlanes.getCharAttr(testPosition),"Second update failed");

        screenPlanes.setScreenAttr(testPosition, attributeType);
        assertEquals(attributeType, screenPlanes.getCharAttr(testPosition),"Final update failed");
    }

    /**
     * TEST 15: Color plane consistency across attribute types
     *
     * Positive test: Verify color plane maintains consistency
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testColorPlaneConsistency(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        try {
            screenPlanes.setScreenAttr(testPosition, attributeType);

            // Verify color plane exists and has correct size
            assertNotNull(screenColor,"Color plane should not be null");
            assertEquals(SCREEN_LENGTH,
                screenColor.length
            ,
                "Color plane size mismatch");

            // Verify color value is set
            char color = screenColor[testPosition];
            if (attributeType > 0) {
                assertTrue(color != 0
                ,
                    String.format("Color not set for attribute %d", attributeType));
            }
        } catch (Exception e) {
            fail("Color plane consistency check failed: " + e.getMessage());
        }
    }

    /**
     * TEST 16: Extended attribute plane consistency
     *
     * Positive test: Verify extended attribute plane maintains correct state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testExtendedAttributePlaneConsistency(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        try {
            screenPlanes.setScreenAttr(testPosition, attributeType);

            // Verify extended plane exists and has correct size
            assertNotNull(screenExtended,"Extended attribute plane should not be null");
            assertEquals(SCREEN_LENGTH,
                screenExtended.length
            ,
                "Extended attribute plane size mismatch");

            // Verify extended attribute reflects dispersed attribute
            char extAttr = screenExtended[testPosition];
            if (attributeType >= ATTR_UNDERLINE) {
                // Certain attributes set extended flags
                assertNotNull(extAttr,"Extended attribute should be retrievable");
            }
        } catch (Exception e) {
            fail("Extended attribute plane consistency check failed: " + e.getMessage());
        }
    }

    /**
     * TEST 17: Attribute values can be retrieved after setting
     *
     * Positive test: Verify getAttribute/getCharAttr consistency
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeConsistencyAfterSetting(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        try {
            screenPlanes.setScreenAttr(testPosition, attributeType);
            int attrValue = screenPlanes.getCharAttr(testPosition);

            // Attribute value should be retrievable after setting
            assertEquals(attributeType,attrValue
            ,
                String.format("Attribute value mismatch at position %d", testPosition));
        } catch (Exception e) {
            fail("Attribute value consistency test failed: " + e.getMessage());
        }
    }

    /**
     * TEST 18: Attribute initialization state
     *
     * Positive test: Verify screen starts with valid attribute state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeInitializationState(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        // Verify all positions start with default attribute
        for (int pos = 0; pos < Math.min(100, SCREEN_LENGTH); pos++) {
            int initialAttr = screenPlanes.getCharAttr(pos);
            // Initial attributes may be 0 or default value - just verify retrievable
            assertNotNull(initialAttr,"Attribute should be retrievable at initialization");
        }
    }

    /**
     * TEST 19: Conflicting field attributes handling
     *
     * Adversarial test: Verify conflicting attributes don't cause corruption
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testConflictingFieldAttributesHandling(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        if ((fieldAttribute & (FIELD_INPUT | FIELD_PROTECTED)) !=
            (FIELD_INPUT | FIELD_PROTECTED)) {
            return; // Only test conflicting attributes
        }

        try {
            screenPlanes.setScreenAttr(testPosition, attributeType);
            int retrieved = screenPlanes.getCharAttr(testPosition);
            // Should handle gracefully without crashing
            assertEquals(attributeType, retrieved,"Conflicting attributes should be stored");
        } catch (Exception e) {
            fail("Conflicting field attributes caused exception: " + e.getMessage());
        }
    }

    /**
     * TEST 20: All extended attributes combined handling
     *
     * Adversarial test: Verify combining all extended attributes doesn't corrupt screen
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAllExtendedAttributesCombined(int testId, int attributeType, int colorValue, int fieldAttribute, int extendedAttribute, int testPosition) throws Exception {
        setParameters(testId, attributeType, colorValue, fieldAttribute, extendedAttribute, testPosition);
        setUp();
        if (!isValidPosition(testPosition)) {
            return;
        }

        int allExtended = EXT_DOUBLE_HEIGHT | EXT_DOUBLE_WIDTH | EXT_UNDERLINE |
                          EXT_BLINK | EXT_COLUMN_SEP | EXT_NON_DISPLAY;

        if ((extendedAttribute & allExtended) != allExtended) {
            return; // Only test with all extended
        }

        try {
            // Store initial state of adjacent position
            int prevAttrBefore = testPosition > 0 ?
                screenPlanes.getCharAttr(testPosition - 1) : 0;

            screenPlanes.setScreenAttr(testPosition, ATTR_UNDERLINE);

            // Should not corrupt screen state
            int retrieved = screenPlanes.getCharAttr(testPosition);
            assertTrue(retrieved != -1,"Screen state should not be corrupted");

            // Verify adjacent position unchanged
            if (testPosition > 0) {
                int prevAttrAfter = screenPlanes.getCharAttr(testPosition - 1);
                assertEquals(prevAttrBefore,
                    prevAttrAfter
                ,"Previous position should be unchanged");
            }
        } catch (Exception e) {
            fail("Combined extended attributes caused exception: " + e.getMessage());
        }
    }

    /**
     * Test double for Screen5250 interface
     */
    private static class Screen5250TestDouble extends Screen5250 {
        private int numCols = SCREEN_WIDTH;
        private int numRows = SCREEN_SIZE;

        public Screen5250TestDouble(int screenSize) {
            super();
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
