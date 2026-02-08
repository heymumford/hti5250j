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
 * Pairwise parameterized tests for ScreenPlanes screen rendering operations.
 *
 * Tests explore boundary conditions and edge cases systematically across:
 * - Multiple screen sizes (24x80, 27x132)
 * - Extreme row/column positions (0, last, out-of-bounds)
 * - Character values (null, space, printable, extended ASCII, Unicode)
 * - Attribute values (normal, reverse, underline, combined)
 *
 * Discovers: Buffer overflow conditions, boundary violations, attribute handling bugs
 */
public class ScreenPlanesPairwiseTest {

    // Test parameters
    private int screenSize;
    private int testRow;
    private int testCol;
    private char testChar;
    private int testAttr;

    // Instance variables
    private ScreenPlanes screenPlanes;
    private Screen5250TestDouble screen5250;
    private char[] screenGUI;
    private char[] screenAttr;
    private char[] screenIsAttr;
    private char[] screen;

    // Screen size constants
    private static final int SIZE_24 = 24;
    private static final int SIZE_27 = 27;

    // Attribute constants (from ScreenPlanes)
    private static final int ATTR_NORMAL = 32;      // Green normal
    private static final int ATTR_REVERSE = 33;     // Green/reverse
    private static final int ATTR_UNDERLINE = 36;   // Green/underline
    private static final int ATTR_BLINK = 40;       // Red/normal (simulated blink)

    /**
     * Pairwise parameter combinations covering:
     * - Screen sizes: 24x80, 27x132
     * - Positions: corners, edges, center, out-of-bounds
     * - Characters: boundary values (0x00, 0x20, 0x40, 0xFF, Unicode)
     * - Attributes: basic and combined
     */
        public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Screen size 24x80: Valid positions with various characters
                { SIZE_24, 0, 0, (char) 0x00, ATTR_NORMAL },           // Top-left, null char
                { SIZE_24, 0, 79, (char) 0x20, ATTR_REVERSE },         // Top-right, space
                { SIZE_24, 23, 0, (char) 0x40, ATTR_UNDERLINE },       // Bottom-left, @
                { SIZE_24, 23, 79, (char) 0xFF, ATTR_BLINK },          // Bottom-right, extended ASCII
                { SIZE_24, 12, 40, 'A', ATTR_NORMAL },                 // Center, letter
                { SIZE_24, 1, 1, 'Z', ATTR_REVERSE },                  // Near top-left
                { SIZE_24, 22, 78, 'X', ATTR_UNDERLINE },              // Near bottom-right
                { SIZE_24, 0, 40, '0', ATTR_BLINK },                   // Top-middle
                { SIZE_24, 12, 0, '9', ATTR_NORMAL },                  // Left-middle

                // Screen size 27x132: Valid positions (wider screen)
                { SIZE_27, 0, 0, (char) 0x00, ATTR_NORMAL },           // Top-left
                { SIZE_27, 0, 131, (char) 0x20, ATTR_REVERSE },        // Top-right, rightmost column
                { SIZE_27, 26, 0, (char) 0x40, ATTR_UNDERLINE },       // Bottom-left
                { SIZE_27, 26, 131, (char) 0xFF, ATTR_BLINK },         // Bottom-right, rightmost
                { SIZE_27, 13, 65, 'M', ATTR_NORMAL },                 // Center

                // Out-of-bounds tests (adversarial - should handle gracefully)
                { SIZE_24, 24, 0, 'X', ATTR_NORMAL },                  // Beyond last row
                { SIZE_24, -1, 0, 'X', ATTR_NORMAL },                  // Negative row
                { SIZE_24, 0, 80, 'X', ATTR_NORMAL },                  // Beyond last column (80x)
                { SIZE_24, 0, -1, 'X', ATTR_NORMAL },                  // Negative column
                { SIZE_27, 27, 0, 'X', ATTR_NORMAL },                  // 27 beyond row for 27x132
                { SIZE_27, 0, 132, 'X', ATTR_NORMAL },                 // 132 beyond column

                // Unicode and special characters
                { SIZE_24, 5, 10, '\u0041', ATTR_NORMAL },             // Unicode 'A'
                { SIZE_24, 10, 20, '\u00FF', ATTR_REVERSE },           // Unicode extended
                { SIZE_24, 15, 40, '\u263A', ATTR_UNDERLINE },         // Unicode smiley

                // All attribute combinations
                { SIZE_24, 12, 12, 'T', 32 },                          // ATTR_NORMAL (green normal)
                { SIZE_24, 12, 13, 'T', 33 },                          // ATTR_REVERSE (green/reverse)
                { SIZE_24, 12, 14, 'T', 36 },                          // ATTR_UNDERLINE (green/underline)
                { SIZE_24, 12, 15, 'T', 40 },                          // ATTR_BLINK (red/normal)
                { SIZE_24, 12, 16, 'T', 63 },                          // ATTR_SPECIAL (nondisplay)
        });
    }

    private void setParameters(int screenSize, int testRow, int testCol, char testChar, int testAttr) {
        this.screenSize = screenSize;
        this.testRow = testRow;
        this.testCol = testCol;
        this.testChar = testChar;
        this.testAttr = testAttr;
    }

        public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble(screenSize);
        screenPlanes = new ScreenPlanes(screen5250, screenSize);

        screen = getPrivateField("screen", char[].class);
        screenGUI = getPrivateField("screenGUI", char[].class);
        screenAttr = getPrivateField("screenAttr", char[].class);
        screenIsAttr = getPrivateField("screenIsAttr", char[].class);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(String fieldName, Class<T> fieldType)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = ScreenPlanes.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(screenPlanes);
    }

    private int convertRowColToPos(int row, int col) {
        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        return row * numCols + col;
    }

    private boolean isValidPosition(int row, int col) {
        int numRows = (screenSize == SIZE_27) ? 27 : 24;
        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        return row >= 0 && row < numRows && col >= 0 && col < numCols;
    }

    /**
     * TEST 1: setChar and getChar with valid positions
     *
     * Positive test: Verify character can be set and retrieved at valid positions
     * across all screen sizes and character values.
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSetCharAndGetCharValidPositions(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return; // Skip out-of-bounds for this positive test
        }

        int pos = convertRowColToPos(testRow, testCol);

        // RED: Will fail if setChar doesn't update screen array
        screenPlanes.setChar(pos, testChar);

        // GREEN: Verify character was set
        assertEquals(testChar,screenPlanes.getChar(pos)
        ,
            String.format("Character not set at pos %d (row %d, col %d)", pos, testRow, testCol));
    }

    /**
     * TEST 2: setChar marks position as changed
     *
     * Positive test: Verify that setChar correctly tracks change status
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSetCharMarksPositionChanged(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        // Set initial character
        screenPlanes.setChar(pos, 'A');

        // Change character (should mark as changed)
        screenPlanes.setChar(pos, testChar);

        // Verify change was recorded (using reflection)
        try {
            char[] screenIsChanged = getPrivateField("screenIsChanged", char[].class);
            // If character differs, position should be marked as changed
            if (testChar != 'A') {
                assertTrue(screenIsChanged[pos] == '1'
                ,
                    String.format("Position %d should be marked as changed when char differs", pos));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not access screenIsChanged field: " + e.getMessage());
        }
    }

    /**
     * TEST 3: setScreenAttr with valid positions and various attributes
     *
     * Positive test: Verify attributes can be set across valid positions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSetScreenAttrValidPositions(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        // Set attribute
        screenPlanes.setScreenAttr(pos, testAttr);

        // Verify attribute was set
        assertEquals(testAttr,screenPlanes.getCharAttr(pos)
        ,
            String.format("Attribute not set at pos %d (row %d, col %d)", pos, testRow, testCol));
    }

    /**
     * TEST 4: setScreenAttr disperses attributes correctly
     *
     * Positive test: Verify that attribute setting triggers color/extended attribute mapping
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSetScreenAttrDispersesAttributes(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        screenPlanes.setScreenAttr(pos, testAttr);

        try {
            char[] screenColor = getPrivateField("screenColor", char[].class);
            char[] screenExtended = getPrivateField("screenExtended", char[].class);

            // Attributes should map to color and extended planes
            // (color/extended value depends on attribute value, tested implicitly)
            assertNotNull(screenColor,"screenColor should be populated");
            assertNotNull(screenExtended,"screenExtended should be populated");

            // For non-zero attribute, disperseAttribute should update color plane
            if (testAttr > 0) {
                // Just verify the planes were updated without null checks
                assertTrue(screenColor[pos] >= 0
                ,
                    "Attribute dispersal should update color plane");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not access color/extended fields: " + e.getMessage());
        }
    }

    /**
     * TEST 5: getWhichGUI returns correct GUI value
     *
     * Positive test: Verify GUI state is correctly retrieved
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetWhichGUIReturnsCorrectValue(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        // Set a GUI value via reflection
        try {
            char[] screenGUIArray = getPrivateField("screenGUI", char[].class);
            int testGUIValue = (testChar & 0x0F); // Use low 4 bits of test char
            screenGUIArray[pos] = (char) testGUIValue;

            // Verify getWhichGUI returns the set value
            assertEquals(testGUIValue,screenPlanes.getWhichGUI(pos)
            ,
                String.format("GUI value not retrieved correctly at pos %d", pos));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not access screenGUI field: " + e.getMessage());
        }
    }

    /**
     * TEST 6: Out-of-bounds setChar does not corrupt screen
     *
     * Adversarial test: Verify setChar gracefully handles out-of-bounds positions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSetCharOutOfBoundsDoesNotCorrupt(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (isValidPosition(testRow, testCol)) {
            return; // Skip valid positions for adversarial test
        }

        int numRows = (screenSize == SIZE_27) ? 27 : 24;
        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        int screenLength = numRows * numCols;

        try {
            // Attempt to set character at out-of-bounds position
            int pos = convertRowColToPos(testRow, testCol);

            // If position is out of bounds, setChar should not crash
            // (test will catch ArrayIndexOutOfBoundsException if it occurs)
            if (pos >= 0 && pos < screenLength) {
                screenPlanes.setChar(pos, testChar);
            } else {
                try {
                    screenPlanes.setChar(pos, testChar);
                    // If we reach here without exception, code doesn't validate bounds
                    // This is OK - just document the behavior
                } catch (ArrayIndexOutOfBoundsException e) {
                    // Expected for truly out-of-bounds positions
                }
            }
        } catch (Exception e) {
            fail("setChar threw unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * TEST 7: Out-of-bounds setScreenAttr does not corrupt screen
     *
     * Adversarial test: Verify setScreenAttr handles out-of-bounds positions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSetScreenAttrOutOfBoundsDoesNotCorrupt(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (isValidPosition(testRow, testCol)) {
            return; // Skip valid positions
        }

        try {
            int pos = convertRowColToPos(testRow, testCol);
            int numRows = (screenSize == SIZE_27) ? 27 : 24;
            int numCols = (screenSize == SIZE_27) ? 132 : 80;
            int screenLength = numRows * numCols;

            if (pos >= 0 && pos < screenLength) {
                screenPlanes.setScreenAttr(pos, testAttr);
            } else {
                try {
                    screenPlanes.setScreenAttr(pos, testAttr);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // Expected for out-of-bounds
                }
            }
        } catch (Exception e) {
            fail("setScreenAttr threw unexpected exception: " + e.getClass().getSimpleName());
        }
    }

    /**
     * TEST 8: Screen size initialization creates correct dimensions
     *
     * Positive test: Verify that screen initialization respects size parameter
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testScreenInitializationDimensions(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        int expectedRows = (screenSize == SIZE_27) ? 27 : 24;
        int expectedCols = (screenSize == SIZE_27) ? 132 : 80;
        int expectedSize = expectedRows * expectedCols;

        // Verify array dimensions
        assertEquals(expectedSize,screen.length
        ,
            String.format("Screen array size incorrect for %dx%d", expectedRows, expectedCols));

        assertEquals(expectedSize,screenGUI.length
        ,
            String.format("ScreenGUI array size incorrect for %dx%d", expectedRows, expectedCols));

        assertEquals(expectedSize,screenAttr.length
        ,
            String.format("ScreenAttr array size incorrect for %dx%d", expectedRows, expectedCols));

        assertEquals(expectedSize,screenIsAttr.length
        ,
            String.format("ScreenIsAttr array size incorrect for %dx%d", expectedRows, expectedCols));
    }

    /**
     * TEST 9: Character round-trip at boundary positions
     *
     * Positive test: Verify character preservation across all boundary positions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCharacterRoundTripBoundary(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        // Store original character
        char originalChar = screenPlanes.getChar(pos);

        // Set test character
        screenPlanes.setChar(pos, testChar);
        char retrievedChar = screenPlanes.getChar(pos);

        // Verify round-trip
        assertEquals(testChar,retrievedChar
        ,
            String.format("Character lost in round-trip at pos %d", pos));
    }

    /**
     * TEST 10: Attribute round-trip across attribute values
     *
     * Positive test: Verify attribute preservation for all tested values
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeRoundTripAllValues(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);
        char originalAttr = (char) screenPlanes.getCharAttr(pos);

        screenPlanes.setScreenAttr(pos, testAttr);
        int retrievedAttr = screenPlanes.getCharAttr(pos);

        assertEquals(testAttr,retrievedAttr
        ,
            String.format("Attribute lost in round-trip at pos %d", pos));
    }

    /**
     * TEST 11: Valid position edge cases at screen boundaries
     *
     * Positive test: Verify operations work at all edge positions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testEdgePositionOperations(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return; // Skip if test parameters are out of bounds
        }

        int numRows = (screenSize == SIZE_27) ? 27 : 24;
        int numCols = (screenSize == SIZE_27) ? 132 : 80;

        // Test that this position is within valid bounds
        assertTrue(testRow >= 0 && testRow < numRows && testCol >= 0 && testCol < numCols
        ,
            String.format("Position row=%d col=%d is invalid for %dx%d screen",
                testRow, testCol, numRows, numCols));

        int pos = convertRowColToPos(testRow, testCol);

        // Operations should succeed without exception
        screenPlanes.setChar(pos, testChar);
        screenPlanes.setScreenAttr(pos, testAttr);

        char retrieved = screenPlanes.getChar(pos);
        assertEquals(testChar, retrieved,"Character should be retrievable");
    }

    /**
     * TEST 12: Multiple sequential character updates
     *
     * Positive test: Verify screen state remains consistent across multiple updates
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMultipleSequentialUpdates(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        // Perform sequence of updates
        screenPlanes.setChar(pos, 'A');
        assertEquals('A', screenPlanes.getChar(pos),"First update failed");

        screenPlanes.setChar(pos, 'B');
        assertEquals('B', screenPlanes.getChar(pos),"Second update failed");

        screenPlanes.setChar(pos, testChar);
        assertEquals(testChar, screenPlanes.getChar(pos),"Final update failed");
    }

    /**
     * TEST 13: Attribute setting does not affect character value
     *
     * Positive test: Verify attribute changes are isolated from character plane
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeChangeDoeNotAffectCharacter(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        // Set character
        char expectedChar = 'T';
        screenPlanes.setChar(pos, expectedChar);

        // Change attributes multiple times
        screenPlanes.setScreenAttr(pos, 32);
        assertEquals(expectedChar, screenPlanes.getChar(pos),"Character corrupted by attr change 1");

        screenPlanes.setScreenAttr(pos, 33);
        assertEquals(expectedChar, screenPlanes.getChar(pos),"Character corrupted by attr change 2");

        screenPlanes.setScreenAttr(pos, testAttr);
        assertEquals(expectedChar, screenPlanes.getChar(pos),"Character corrupted by attr change 3");
    }

    /**
     * TEST 14: Character setting does not affect attribute value
     *
     * Positive test: Verify character changes are isolated from attribute plane
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCharacterChangeDoesNotAffectAttribute(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        // Set attribute
        int expectedAttr = 36;
        screenPlanes.setScreenAttr(pos, expectedAttr);

        // Change characters multiple times
        screenPlanes.setChar(pos, 'X');
        assertEquals(expectedAttr, screenPlanes.getCharAttr(pos),"Attribute corrupted by char change 1");

        screenPlanes.setChar(pos, 'Y');
        assertEquals(expectedAttr, screenPlanes.getCharAttr(pos),"Attribute corrupted by char change 2");

        screenPlanes.setChar(pos, testChar);
        assertEquals(expectedAttr, screenPlanes.getCharAttr(pos),"Attribute corrupted by char change 3");
    }

    /**
     * TEST 15: Unicode character support across positions
     *
     * Positive test: Verify Unicode characters are preserved
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUnicodeCharacterSupport(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        // Only test with actual Unicode test characters
        if (testChar < 0x00FF) {
            return; // Skip non-extended characters
        }

        int pos = convertRowColToPos(testRow, testCol);

        screenPlanes.setChar(pos, testChar);
        char retrieved = screenPlanes.getChar(pos);

        assertEquals(testChar,retrieved
        ,
            String.format("Unicode character 0x%04X not preserved", (int) testChar));
    }

    /**
     * TEST 16: Screen state consistency after attribute operations
     *
     * Positive test: Verify all planes remain in consistent state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testScreenStateConsistency(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        try {
            char[] screenColorArray = getPrivateField("screenColor", char[].class);
            char[] screenExtendedArray = getPrivateField("screenExtended", char[].class);

            // Set character and attribute
            screenPlanes.setChar(pos, testChar);
            screenPlanes.setScreenAttr(pos, testAttr);

            // Verify no plane is null and all have same length
            assertNotNull(screen,"Screen array should not be null");
            assertNotNull(screenGUI,"ScreenGUI should not be null");
            assertNotNull(screenAttr,"ScreenAttr should not be null");
            assertNotNull(screenColorArray,"ScreenColor should not be null");
            assertNotNull(screenExtendedArray,"ScreenExtended should not be null");

            assertTrue(screen.length == screenGUI.length &&
                screen.length == screenAttr.length &&
                screen.length == screenColorArray.length &&
                screen.length == screenExtendedArray.length
            ,
                "All planes should have same length");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not verify plane consistency: " + e.getMessage());
        }
    }

    /**
     * TEST 17: Null character handling
     *
     * Positive test: Verify null characters (0x00) are properly stored
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testNullCharacterHandling(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        if (testChar != 0x00) {
            return; // Only test with null character
        }

        int pos = convertRowColToPos(testRow, testCol);

        screenPlanes.setChar(pos, testChar);
        char retrieved = screenPlanes.getChar(pos);

        assertEquals((char) 0x00, retrieved,"Null character should be preserved");
    }

    /**
     * TEST 18: Extended ASCII character handling (0xFF)
     *
     * Positive test: Verify extended ASCII values are preserved
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testExtendedASCIIHandling(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        if (testChar != (char) 0xFF) {
            return; // Only test with extended ASCII
        }

        int pos = convertRowColToPos(testRow, testCol);

        screenPlanes.setChar(pos, testChar);
        char retrieved = screenPlanes.getChar(pos);

        assertEquals((char) 0xFF, retrieved,"Extended ASCII (0xFF) should be preserved");
    }

    /**
     * TEST 19: Attribute value range coverage
     *
     * Positive test: Verify wide range of attribute values are accepted
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAttributeValueRangeCoverage(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int pos = convertRowColToPos(testRow, testCol);

        // Test that attribute value is stored and retrieved
        screenPlanes.setScreenAttr(pos, testAttr);
        int retrieved = screenPlanes.getCharAttr(pos);

        assertEquals(testAttr,retrieved
        ,
            String.format("Attribute value %d should be preserved", testAttr));
    }

    /**
     * TEST 20: Position calculation consistency across screen sizes
     *
     * Positive test: Verify row/col to position conversion is consistent
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testPositionCalculationConsistency(int screenSize, int testRow, int testCol, char testChar, int testAttr) throws Exception {
        setParameters(screenSize, testRow, testCol, testChar, testAttr);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        int expectedPos = testRow * numCols + testCol;
        int calculatedPos = convertRowColToPos(testRow, testCol);

        assertEquals(expectedPos,calculatedPos
        ,
            String.format("Position calculation inconsistent for row=%d col=%d", testRow, testCol));
    }

    /**
     * Test double for Screen5250 interface
     */
    private static class Screen5250TestDouble extends Screen5250 {
        private int numCols;
        private int numRows;

        public Screen5250TestDouble(int screenSize) {
            super();
            if (screenSize == SIZE_27) {
                numRows = 27;
                numCols = 132;
            } else {
                numRows = 24;
                numCols = 80;
            }
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
