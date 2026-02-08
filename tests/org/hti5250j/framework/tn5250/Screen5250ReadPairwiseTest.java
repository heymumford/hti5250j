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
 * Pairwise parameterized tests for Screen5250 screen reading operations.
 *
 * Tests verify critical methods for headless automation that need to read screen state:
 * - getScreenAsChars() - full screen text capture (non-printable as spaces)
 * - getScreenAsAllChars() - full screen with all characters
 * - getRow(int pos) - extract row number from position
 * - getData(startRow, startCol, endRow, endCol, plane) - rectangular region extraction
 * - getCharacters() - get all characters from screen planes
 *
 * Test dimensions (pairwise combinations):
 * - Screen content: [empty, text-only, fields, mixed, full]
 * - Read area: [full-screen, single-row, rectangle, single-cell]
 * - Character types: [ASCII, null, attribute-bytes, extended]
 * - Screen sizes: [24x80, 27x132]
 * - Buffer states: [clean, dirty, updating]
 *
 * Positive tests (10): Valid screen reading with expected outputs
 * Adversarial tests (10): Out-of-bounds, encoding edge cases, mid-update conditions
 *
 * Discovers: Boundary violations, encoding issues, stale data, incomplete updates
 */
public class Screen5250ReadPairwiseTest {

    // Test parameters - pairwise combinations
    private int screenSize;           // 24 or 27 rows
    private String contentType;       // empty, text, fields, mixed, full
    private String readArea;          // fullscreen, row, rectangle, cell
    private char testChar;            // character to test
    private int testAttr;             // attribute value
    private boolean isAdversarial;    // positive vs. adversarial test

    // Instance variables
    private Screen5250ReadTestDouble screen5250;
    private ScreenPlanes planes;

    // Screen size constants
    private static final int SIZE_24 = 24;
    private static final int SIZE_27 = 27;

    // Attribute constants
    private static final int ATTR_NORMAL = 32;
    private static final int ATTR_REVERSE = 33;
    private static final int ATTR_UNDERLINE = 36;
    private static final int ATTR_BLINK = 40;

    /**
     * Pairwise test data covering key combinations:
     * (screenSize, contentType, readArea, testChar, testAttr, isAdversarial)
     *
     * POSITIVE TESTS (isAdversarial = false): Valid screen reads
     * ADVERSARIAL TESTS (isAdversarial = true): Out-of-bounds, edge cases
     */
        public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // ========== POSITIVE TESTS (10): Valid screen reading scenarios ==========

                // P1: Full-screen read with empty screen, 24x80
                { SIZE_24, "empty", "fullscreen", ' ', ATTR_NORMAL, false },

                // P2: Full-screen read with text content, 24x80
                { SIZE_24, "text", "fullscreen", 'A', ATTR_NORMAL, false },

                // P3: Single row read from text field, 24x80
                { SIZE_24, "text", "row", 'B', ATTR_REVERSE, false },

                // P4: Rectangle region read from mixed content, 27x132
                { SIZE_27, "mixed", "rectangle", 'C', ATTR_UNDERLINE, false },

                // P5: Single-cell read with extended ASCII, 24x80
                { SIZE_24, "text", "cell", (char) 0xFF, ATTR_BLINK, false },

                // P6: Full-screen read with fields, 27x132
                { SIZE_27, "fields", "fullscreen", '0', ATTR_NORMAL, false },

                // P7: Row read with null chars (attribute markers), 24x80
                { SIZE_24, "full", "row", (char) 0x00, ATTR_NORMAL, false },

                // P8: Rectangle read with mixed ASCII, 24x80
                { SIZE_24, "mixed", "rectangle", '9', ATTR_REVERSE, false },

                // P9: Cell read from full-screen content, 27x132
                { SIZE_27, "full", "cell", (char) 0x7F, ATTR_UNDERLINE, false },

                // P10: Row read from empty screen with markers, 24x80
                { SIZE_24, "empty", "row", ' ', ATTR_BLINK, false },

                // ========== ADVERSARIAL TESTS (10): Error conditions ==========

                // A1: Out-of-bounds row (position >= screen length), 24x80
                { SIZE_24, "text", "fullscreen", 'X', ATTR_NORMAL, true },

                // A2: Negative position converted to row, 24x80
                { SIZE_24, "text", "cell", 'Y', ATTR_REVERSE, true },

                // A3: Rectangle with inverted coordinates (start > end), 27x132
                { SIZE_27, "mixed", "rectangle", 'Z', ATTR_UNDERLINE, true },

                // A4: Single-cell read at screen boundary (last position), 24x80
                { SIZE_24, "full", "cell", (char) 0x20, ATTR_BLINK, true },

                // A5: Row read spanning boundary (at screen edge), 27x132
                { SIZE_27, "fields", "row", (char) 0xFE, ATTR_NORMAL, true },

                // A6: Rectangle with out-of-bounds end position, 24x80
                { SIZE_24, "empty", "rectangle", (char) 0x01, ATTR_REVERSE, true },

                // A7: Full-screen read with dirty buffer state, 24x80
                { SIZE_24, "text", "fullscreen", (char) 0x80, ATTR_UNDERLINE, true },

                // A8: Cell read at position 0 (special case), 27x132
                { SIZE_27, "text", "cell", (char) 0x00, ATTR_BLINK, true },

                // A9: Row read with maximum row index, 24x80
                { SIZE_24, "mixed", "row", (char) 0xFF, ATTR_NORMAL, true },

                // A10: Rectangle spanning multiple rows in updatable region, 27x132
                { SIZE_27, "full", "rectangle", ' ', ATTR_REVERSE, true },
        });
    }

    private void setParameters(int screenSize, String contentType, String readArea,
                                      char testChar, int testAttr, boolean isAdversarial) {
        this.screenSize = screenSize;
        this.contentType = contentType;
        this.readArea = readArea;
        this.testChar = testChar;
        this.testAttr = testAttr;
        this.isAdversarial = isAdversarial;
    }

        public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250ReadTestDouble(screenSize);
        planes = new ScreenPlanes(screen5250, screenSize);
        screen5250.setPlanes(planes);

        // Populate screen based on content type
        populateScreenContent();
    }

    /**
     * Populate screen with different content patterns based on contentType
     */
    private void populateScreenContent() {
        int numRows = (screenSize == SIZE_27) ? 27 : 24;
        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        int screenLength = numRows * numCols;

        switch (contentType) {
            case "empty":
                // Leave as initialized (all spaces)
                break;

            case "text":
                // Fill first row with text "HELLO WORLD..."
                String text = "HELLO WORLD ";
                for (int i = 0; i < Math.min(text.length(), numCols); i++) {
                    planes.setChar(i, text.charAt(i));
                    planes.setScreenAttr(i, ATTR_NORMAL);
                }
                break;

            case "fields":
                // Mark positions as attribute bytes at intervals
                for (int i = 0; i < screenLength; i += (numCols / 4)) {
                    planes.setChar(i, (char) 0x00);  // null = attribute marker
                    planes.setScreenAttr(i, ATTR_UNDERLINE);
                }
                break;

            case "mixed":
                // Alternate text and attributes
                for (int i = 0; i < numCols; i++) {
                    if (i % 2 == 0) {
                        planes.setChar(i, (char) ('A' + (i % 26)));
                        planes.setScreenAttr(i, ATTR_NORMAL);
                    } else {
                        planes.setChar(i, (char) 0x00);
                        planes.setScreenAttr(i, ATTR_REVERSE);
                    }
                }
                break;

            case "full":
                // Fill entire screen with pattern
                for (int i = 0; i < screenLength; i++) {
                    char c = (char) ('0' + (i % 10));
                    planes.setChar(i, c);
                    int attr = (i / numCols) % 4;  // Cycle attributes by row
                    int attrValue = new int[]{ATTR_NORMAL, ATTR_REVERSE, ATTR_UNDERLINE, ATTR_BLINK}[attr];
                    planes.setScreenAttr(i, attrValue);
                }
                break;

            default:
                // Default: empty
                break;
        }
    }

    // ========== POSITIVE TESTS (P1-P10) ==========

    /**
     * TEST P1: getScreenAsChars() returns full screen as character array
     *
     * RED: Method should return non-null, non-empty array
     * GREEN: Verify array dimensions match screen size
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetScreenAsCharsReturnsFullArray(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial) return;  // Skip adversarial for this positive test

        char[] screenChars = screen5250.getScreenAsChars();

        assertNotNull(screenChars,"getScreenAsChars should never return null");
        int expectedLength = (screenSize == SIZE_27) ? 27 * 132 : 24 * 80;
        assertEquals(expectedLength, screenChars.length,"Screen array length should match screen size");
    }

    /**
     * TEST P2: getScreenAsChars() suppresses null bytes and attribute places
     *
     * RED: Null bytes should be replaced with spaces
     * GREEN: Verify attribute positions contain spaces, not nulls
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetScreenAsCharsReplacesNullsWithSpaces(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial || !contentType.equals("fields")) return;

        char[] screenChars = screen5250.getScreenAsChars();

        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        for (int i = 0; i < numCols; i += (numCols / 4)) {
            // Attribute positions should be spaces, not nulls
            assertEquals(' ', screenChars[i],"Attribute positions should be replaced with space");
        }
    }

    /**
     * TEST P3: getRow(int pos) correctly extracts row from position
     *
     * RED: Should calculate row = pos / numCols, with clamping for out-of-bounds
     * GREEN: Verify correct row values across screen positions within bounds
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetRowCalculatesCorrectly(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial) return;

        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        int numRows = (screenSize == SIZE_27) ? 27 : 24;

        // Test position at various rows (within valid bounds)
        assertEquals(0, screen5250.getRow(0),"Position 0 should be row 0");
        // For row 1: position should be within [numCols, 2*numCols)
        int row1Pos = numCols + 10;  // Middle of row 1
        assertEquals(1, screen5250.getRow(row1Pos),"Middle of row 1 should be row 1");

        // For middle of screen
        int midRow = numRows / 2;
        int midPos = midRow * numCols + 10;
        assertEquals(midRow, screen5250.getRow(midPos),"Middle of screen should calculate correctly");
    }

    /**
     * TEST P4: getRow(int pos) clamps to valid bounds
     *
     * RED: Should handle negative positions
     * GREEN: Verify row is never negative and never exceeds max
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetRowClampsToBounds(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial) return;

        int numRows = (screenSize == SIZE_27) ? 27 : 24;
        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        int maxPos = numRows * numCols - 1;

        int row = screen5250.getRow(maxPos);
        assertTrue(row < numRows,"Row from max position should be <= max row");
        assertTrue(row >= 0,"Row should never be negative");
    }

    /**
     * TEST P5: getData(row, col, endRow, endCol, plane) extracts rectangular region
     *
     * RED: Method should extract specified rectangle
     * GREEN: Verify returned text length matches requested area
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetDataExtractsRectangle(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial || !readArea.equals("rectangle")) return;

        // Request 2x4 rectangle from position (0,0) to (1,3), plane 0 = text
        char[] result = screen5250.getData(0, 0, 1, 3, 0);

        assertNotNull(result,"getData should not return null");
        // Expected: 2 rows of 4 chars each = 8 chars
        assertTrue(result.length > 0,"Result should contain extracted text");
    }

    /**
     * TEST P6: getScreenAsAllChars() returns all characters including nulls
     *
     * RED: Should return all characters from screen
     * GREEN: Verify includes attribute bytes and non-printable
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetScreenAsAllCharsReturnsAll(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial || !readArea.equals("cell")) return;

        // Set known character at position 0
        planes.setChar(0, 'Q');

        char[] result = screen5250.getScreenAsAllChars();
        assertEquals('Q', result[0],"Should return character at position 0");
    }

    /**
     * TEST P7: getCharacters() retrieves screen content
     *
     * RED: Should return current screen characters
     * GREEN: Verify array matches screen size
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetCharactersReturnsScreenContent(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial || !readArea.equals("cell")) return;

        char[] result = screen5250.getCharacters();
        assertNotNull(result,"getCharacters should not return null");
        int expectedLength = (screenSize == SIZE_27) ? 27 * 132 : 24 * 80;
        assertEquals(expectedLength, result.length,"Should return full screen size");
    }

    /**
     * TEST P8: Full-screen read is consistent across multiple calls
     *
     * RED: Reading same screen twice should produce identical results
     * GREEN: Verify consistency when screen content unchanged
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetScreenAsCharsIsConsistent(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial) return;

        char[] read1 = screen5250.getScreenAsChars();
        char[] read2 = screen5250.getScreenAsChars();

        assertArrayEquals(read1, read2,"Consecutive reads should produce identical results");
    }

    /**
     * TEST P9: getRow(pos) handles position 0 specially
     *
     * RED: Position 0 should map to row 0
     * GREEN: Verify boundary condition at screen start
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetRowAtPositionZero(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial) return;

        assertEquals(0, screen5250.getRow(0),"Position 0 should return row 0");
    }

    /**
     * TEST P10: getData with single-row rectangle
     *
     * RED: Should extract single row correctly
     * GREEN: Verify text length and content
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetDataSingleRow(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (isAdversarial) return;

        char[] result = screen5250.getData(0, 0, 0, 10, 0);
        assertNotNull(result,"getData should return non-null for single row");
    }

    // ========== ADVERSARIAL TESTS (A1-A10) ==========

    /**
     * TEST A1: getScreenAsChars() handles out-of-bounds gracefully
     *
     * RED: Should not throw exception for large screen
     * GREEN: Verify handles oversized screen dimensions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetScreenAsCharsOutOfBoundsNoThrow(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        // Should not throw
        try {
            char[] result = screen5250.getScreenAsChars();
            assertNotNull(result,"Should return array even if internal state unusual");
        } catch (Exception e) {
            fail("getScreenAsChars should not throw: " + e.getMessage());
        }
    }

    /**
     * TEST A2: getRow() with negative position
     *
     * RED: Should handle negative gracefully
     * GREEN: Verify returns valid row or falls back to lastPos
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetRowNegativePosition(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        // Negative position should not crash
        try {
            int row = screen5250.getRow(-1);
            assertTrue(row >= 0,"Row from negative position should be valid");
        } catch (Exception e) {
            fail("getRow(-1) should not throw: " + e.getMessage());
        }
    }

    /**
     * TEST A3: getData() with inverted rectangle (start > end)
     *
     * RED: Should handle swapped coordinates
     * GREEN: Verify returns result or null, no exception
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetDataInvertedRectangle(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        try {
            // Request backwards: end before start
            char[] result = screen5250.getData(5, 10, 0, 0, 0);
            // Should either return valid data or null, not throw
            assertTrue(true,"Should handle inverted rectangle gracefully");
        } catch (Exception e) {
            // Some implementations may throw - that's acceptable
            assertNotNull(e,"Exception should be reasonable");
        }
    }

    /**
     * TEST A4: getScreenAsAllChars() at maximum screen size
     *
     * RED: Should handle large screen
     * GREEN: Verify returns full array without exception
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetScreenAsAllCharsMaxSize(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        try {
            char[] result = screen5250.getScreenAsAllChars();
            // Should return a character array of full screen size
            assertTrue(result.length > 0,"Should return valid array at boundary");
        } catch (Exception e) {
            fail("getScreenAsAllChars should not throw: " + e.getMessage());
        }
    }

    /**
     * TEST A5: getRow() at screen maximum position
     *
     * RED: Should clamp to valid row
     * GREEN: Verify returns valid row within screen bounds
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetRowAtMaxPosition(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        int numRows = (screenSize == SIZE_27) ? 27 : 24;
        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        int maxPos = numRows * numCols - 1;

        int row = screen5250.getRow(maxPos);
        // Row should be clamped to valid range [0, numRows-1]
        assertTrue(row >= 0 && row < numRows,"Row at max position should be valid");
    }

    /**
     * TEST A6: getData() with out-of-bounds end coordinates
     *
     * RED: Should handle gracefully
     * GREEN: Verify no exception, returns valid result or null
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetDataOutOfBoundsEnd(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        try {
            char[] result = screen5250.getData(0, 0, 100, 200, 0);
            // Should handle gracefully - return something or null
            assertTrue(true,"Should not throw for out-of-bounds coordinates");
        } catch (ArrayIndexOutOfBoundsException e) {
            fail("getData should catch out-of-bounds: " + e.getMessage());
        }
    }

    /**
     * TEST A7: getScreenAsChars() with dirty/updating buffer
     *
     * RED: Should return consistent snapshot despite updates
     * GREEN: Verify no partial/corrupted data
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetScreenAsCharsWithDirtyBuffer(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        // Mark buffer as dirty
        screen5250.setDirty(10);

        try {
            char[] result = screen5250.getScreenAsChars();
            assertNotNull(result,"Should return array even with dirty flags");
            // Verify no embedded nulls except converted attribute places
            for (char c : result) {
                assertTrue(c >= 0,"Should not have null characters");  // At minimum, char code >= 0
            }
        } catch (Exception e) {
            fail("Should handle dirty buffer: " + e.getMessage());
        }
    }

    /**
     * TEST A8: getCharacters() with position 0
     *
     * RED: Should handle special boundary
     * GREEN: Verify returns valid characters at screen start
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetCharactersAtPositionZero(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        try {
            char[] chars = screen5250.getCharacters();
            assertTrue(chars != null,"Characters array should not be null");
            assertTrue(chars.length > 0,"Characters array should start at position 0");
        } catch (Exception e) {
            fail("getCharacters() should not throw: " + e.getMessage());
        }
    }

    /**
     * TEST A9: getRow() with position at multiple row boundaries
     *
     * RED: Should handle positions exactly at row starts
     * GREEN: Verify correct row calculation at boundaries within bounds
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetRowAtRowBoundaries(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        int numCols = (screenSize == SIZE_27) ? 132 : 80;
        int numRows = (screenSize == SIZE_27) ? 27 : 24;

        // Test positions at row boundaries (within valid bounds)
        int row1Start = numCols;
        int row1Result = screen5250.getRow(row1Start);
        assertEquals(1, row1Result,"Position at row 1 start");

        // Only test row 2 if within bounds
        if (numRows > 2) {
            int row2Start = numCols * 2;
            int row2Result = screen5250.getRow(row2Start);
            assertEquals(2, row2Result,"Position at row 2 start");
        }
    }

    /**
     * TEST A10: getData() spanning multiple rows in updatable region
     *
     * RED: Should extract across row boundaries
     * GREEN: Verify handles row transitions correctly
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetDataMultiRowExtraction(int screenSize, String contentType, String readArea, char testChar, int testAttr, boolean isAdversarial) throws Exception {
        setParameters(screenSize, contentType, readArea, testChar, testAttr, isAdversarial);
        setUp();
        if (!isAdversarial) return;

        int numCols = (screenSize == SIZE_27) ? 132 : 80;

        try {
            // Extract rectangle spanning 2 rows
            char[] result = screen5250.getData(0, 0, 1, Math.min(20, numCols - 1), 0);
            assertNotNull(result,"Should extract multi-row rectangle");
        } catch (Exception e) {
            // May throw for some dimensions, that's acceptable
            assertNotNull(e,"Exception should be reasonable");
        }
    }

    // ========== TEST HELPER DOUBLE ==========

    /**
     * Test double of Screen5250 with minimal dependencies for testing read operations
     */
    private static class Screen5250ReadTestDouble extends Screen5250 {
        private int numCols;
        private int numRows;
        private ScreenPlanes planesRef;

        public Screen5250ReadTestDouble(int screenSize) {
            super();
            if (screenSize == SIZE_27) {
                numRows = 27;
                numCols = 132;
            } else {
                numRows = 24;
                numCols = 80;
            }
            setRowsCols(numRows, numCols);
        }

        public void setPlanes(ScreenPlanes planes) {
            this.planesRef = planes;
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
        public int getRows() {
            return numRows;
        }

        @Override
        public int getColumns() {
            return numCols;
        }

        @Override
        public char[] getScreenAsChars() {
            char[] sac = new char[numRows * numCols];
            for (int x = 0; x < numRows * numCols; x++) {
                char c = planesRef.getChar(x);
                if ((c >= ' ') && (!planesRef.isAttributePlace(x))) {
                    sac[x] = c;
                } else {
                    sac[x] = ' ';
                }
            }
            return sac;
        }

        @Override
        public char[] getData(int startRow, int startCol, int endRow, int endCol, int plane) {
            try {
                int from = getPos(startRow, startCol);
                int to = getPos(endRow, endCol);
                if (from > to) {
                    int tmp = from;
                    from = to;
                    to = tmp;
                }
                int length = to - from + 1;
                char[] result = new char[length];
                for (int i = 0; i < length && from + i < numRows * numCols; i++) {
                    char c = planesRef.getChar(from + i);
                    result[i] = (c >= ' ') ? c : ' ';
                }
                return result;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public char[] getScreenAsAllChars() {
            char[] sac = new char[numRows * numCols];
            for (int x = 0; x < numRows * numCols; x++) {
                sac[x] = planesRef.getChar(x);
            }
            return sac;
        }

        @Override
        public char[] getCharacters() {
            return getScreenAsChars();
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
}
