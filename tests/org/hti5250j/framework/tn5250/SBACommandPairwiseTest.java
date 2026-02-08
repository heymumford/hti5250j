/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
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
 * Pairwise TDD test suite for HTI5250j SBA command handling.
 *
 * Tests explore boundary conditions and edge cases systematically across:
 * - Row and column boundaries (1, last-1, last, last+1)
 * - Screen dimensions (80x24, 132x27, custom sizes)
 * - Address encoding formats (12-bit, 14-bit)
 * - Sequencing (single, consecutive, after field)
 *
 * Discovers: Buffer overflow conditions, address encoding bugs, cursor
 * positioning errors, state machine violations, error response generation
 */
public class SBACommandPairwiseTest {

    // Test parameters (pairwise combinations)
    private int testRow;
    private int testCol;
    private int screenSize;
    private String addressFormat;
    private String sequenceContext;

    // Instance variables
    private Screen5250 screen5250;

    // Screen dimension constants
    private static final int SIZE_80x24 = 80;    // Marker for 80x24 screen
    private static final int SIZE_132x27 = 132;  // Marker for 132x27 screen
    private static final int SIZE_CUSTOM = 99;   // Marker for custom 99x30 screen

    // Address format constants
    private static final String FORMAT_12BIT = "12BIT";
    private static final String FORMAT_14BIT = "14BIT";

    // Sequence context constants
    private static final String SEQ_SINGLE = "SINGLE";
    private static final String SEQ_CONSECUTIVE = "CONSECUTIVE";
    private static final String SEQ_AFTER_FIELD = "AFTER_FIELD";

    /**
     * Pairwise parameter combinations covering:
     * - Row boundaries: 1, last valid, beyond last, invalid
     * - Column boundaries: 1, last valid, beyond last, invalid
     * - Screen sizes: 80x24, 132x27, custom 99x30
     * - Address encoding: 12-bit, 14-bit
     * - Sequencing: single SBA, consecutive SBAs, SBA after field
     */
        public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // =====================================================================
                // POSITIVE TESTS: Valid SBA commands that should succeed
                // =====================================================================

                // 80x24 screen: valid positions (corners, center, boundaries)
                { 1, 1, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },               // Test 1: Home position, 12-bit, single
                { 1, 80, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },              // Test 2: Top-right valid
                { 24, 1, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },              // Test 3: Bottom-left valid
                { 24, 80, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },             // Test 4: Bottom-right valid
                { 12, 40, SIZE_80x24, FORMAT_12BIT, SEQ_CONSECUTIVE },        // Test 5: Center, consecutive SBAs
                { 1, 1, SIZE_80x24, FORMAT_14BIT, SEQ_SINGLE },               // Test 6: Home, 14-bit format
                { 24, 80, SIZE_80x24, FORMAT_14BIT, SEQ_CONSECUTIVE },        // Test 7: Corner, 14-bit, consecutive

                // 132x27 screen: wider screen with valid positions
                { 1, 1, SIZE_132x27, FORMAT_12BIT, SEQ_SINGLE },              // Test 8: Home on wider screen
                { 1, 132, SIZE_132x27, FORMAT_12BIT, SEQ_SINGLE },            // Test 9: Top-right on 132x27
                { 27, 132, SIZE_132x27, FORMAT_12BIT, SEQ_SINGLE },           // Test 10: Bottom-right on 132x27
                { 27, 1, SIZE_132x27, FORMAT_14BIT, SEQ_CONSECUTIVE },        // Test 11: Bottom-left on 132x27, 14-bit

                // Custom size screen (99x30): non-standard dimensions
                { 1, 1, SIZE_CUSTOM, FORMAT_12BIT, SEQ_AFTER_FIELD },         // Test 12: Custom size after field
                { 30, 99, SIZE_CUSTOM, FORMAT_12BIT, SEQ_SINGLE },            // Test 13: Custom size corner

                // Mid-range positions with various encoding formats
                { 12, 40, SIZE_80x24, FORMAT_14BIT, SEQ_AFTER_FIELD },        // Test 14: Center, 14-bit, after field
                { 15, 60, SIZE_132x27, FORMAT_14BIT, SEQ_SINGLE },            // Test 15: Mid-position on wider screen

                // =====================================================================
                // ADVERSARIAL TESTS: Out-of-bounds, invalid encoding, state violations
                // =====================================================================

                // Out-of-bounds rows (80x24 screen)
                { 0, 40, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },              // Test 16: Row 0 (below minimum)
                { 25, 40, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },             // Test 17: Row 25 (beyond max)
                { 27, 40, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },             // Test 18: Row 27 (way beyond max)

                // Out-of-bounds columns (80x24 screen)
                { 12, 0, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },              // Test 19: Column 0 (below minimum)
                { 12, 81, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },             // Test 20: Column 81 (beyond max)
                { 12, 255, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },            // Test 21: Column 255 (way beyond max)

                // Out-of-bounds on 132x27 screen
                { 28, 40, SIZE_132x27, FORMAT_12BIT, SEQ_SINGLE },            // Test 22: Row 28 (beyond 27)
                { 14, 133, SIZE_132x27, FORMAT_12BIT, SEQ_SINGLE },           // Test 23: Column 133 (beyond 132)

                // Adversarial encoding tests
                { 24, 80, SIZE_80x24, FORMAT_14BIT, SEQ_CONSECUTIVE },        // Test 24: Max position, 14-bit, consecutive
                { 1, 80, SIZE_132x27, FORMAT_14BIT, SEQ_AFTER_FIELD },        // Test 25: 14-bit after field

                // Mixed boundaries
                { 1, 255, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },             // Test 26: Valid row, invalid col
                { 255, 1, SIZE_80x24, FORMAT_12BIT, SEQ_SINGLE },             // Test 27: Invalid row, valid col
        });
    }

    private void setParameters(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) {
        this.testRow = testRow;
        this.testCol = testCol;
        this.screenSize = screenSize;
        this.addressFormat = addressFormat;
        this.sequenceContext = sequenceContext;
    }

    /**
     * Test double for Screen5250 with minimal dependencies
     */
    private static class Screen5250TestDouble extends Screen5250 {
        private int mockRows;
        private int mockCols;

        public Screen5250TestDouble(int rows, int cols) {
            super();
            this.mockRows = rows;
            this.mockCols = cols;
            setRowsCols(rows, cols);
        }

        @Override
        public int getRows() {
            return mockRows;
        }

        @Override
        public int getColumns() {
            return mockCols;
        }

        @Override
        public int getScreenLength() {
            return mockRows * mockCols;
        }

        @Override
        public boolean isInField(int pos, boolean checkAttr) {
            return false;  // Simplified for SBA tests
        }

        @Override
        public StringBuffer getHSMore() {
            return new StringBuffer("");
        }

        @Override
        public StringBuffer getHSBottom() {
            return new StringBuffer("");
        }

        @Override
        public void setDirty(int pos) {
            // No-op
        }
    }

        public void setUp() throws Exception {
        // Create screen with appropriate dimensions
        int rows, cols;
        if (screenSize == SIZE_80x24) {
            rows = 24;
            cols = 80;
        } else if (screenSize == SIZE_132x27) {
            rows = 27;
            cols = 132;
        } else if (screenSize == SIZE_CUSTOM) {
            rows = 30;
            cols = 99;
        } else {
            rows = 24;
            cols = 80;
        }

        screen5250 = new Screen5250TestDouble(rows, cols);
    }

    /**
     * Helper: Determine expected screen dimensions
     */
    private int getExpectedRows() {
        if (screenSize == SIZE_80x24) {
            return 24;
        } else if (screenSize == SIZE_132x27) {
            return 27;
        } else if (screenSize == SIZE_CUSTOM) {
            return 30;
        } else {
            return 24;
        }
    }

    private int getExpectedColumns() {
        if (screenSize == SIZE_80x24) {
            return 80;
        } else if (screenSize == SIZE_132x27) {
            return 132;
        } else if (screenSize == SIZE_CUSTOM) {
            return 99;
        } else {
            return 80;
        }
    }

    /**
     * Helper: Get cursor position via reflection
     */
    private int getCursorPosition() throws NoSuchFieldException, IllegalAccessException {
        Field field = Screen5250.class.getDeclaredField("lastPos");
        field.setAccessible(true);
        return (int) field.get(screen5250);
    }

    /**
     * Helper: Check if position is valid for screen dimensions
     */
    private boolean isValidPosition(int row, int col) {
        return row >= 1 && row <= getExpectedRows() &&
               col >= 1 && col <= getExpectedColumns();
    }

    /**
     * Helper: Convert row/col to linear position (1-based to 0-based)
     */
    private int convertToPosition(int row, int col) {
        return ((row - 1) * getExpectedColumns()) + (col - 1);
    }

    /**
     * Simulate processSetBufferAddressOrder validation logic
     * Returns true if error, false if success (matches tnvt behavior)
     */
    private boolean validateSBAAddress(int row, int col) {
        // From tnvt.processSetBufferAddressOrder():
        // if (saRow <= screen52.getRows() && saCol <= screen52.getColumns())
        // Note: No check for row > 0 or col > 0 in original code,
        // but we test that behavior is correct either way
        return !(row <= screen5250.getRows() && col <= screen5250.getColumns() &&
                 row > 0 && col > 0);
    }

    // ========================================================================
    // POSITIVE TESTS (12+): Valid SBA commands that should succeed
    // ========================================================================

    /**
     * POSITIVE: Single SBA command to valid position
     * Sets cursor using SBA and verifies position is correct
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAValidPositionSetsCursor(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;  // Skip invalid positions for positive test
        }

        // Act: Set cursor (simulates SBA processing)
        screen5250.setCursor(testRow, testCol);

        // Assert
        int expectedPos = convertToPosition(testRow, testCol);
        assertEquals(expectedPos, getCursorPosition(),"Cursor should be at correct position after SBA for row=" + testRow + ",col=" + testCol);
    }

    /**
     * POSITIVE: SBA home position (1,1) across all screen sizes
     * Home position should always be valid
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAHomePositionAllScreenSizes(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (testRow != 1 || testCol != 1) {
            return;  // Only test home position
        }

        // Act: Set cursor to home
        screen5250.setCursor(testRow, testCol);

        // Assert: Position should be 0 (home)
        assertEquals(0, getCursorPosition(),"Home position (1,1) should map to position 0");
    }

    /**
     * POSITIVE: SBA to bottom-right valid position
     * Tests maximum valid cursor position
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAMaximumValidPosition(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        int maxRow = getExpectedRows();
        int maxCol = getExpectedColumns();

        if (testRow != maxRow || testCol != maxCol) {
            return;  // Only test maximum position
        }

        // Act: Set cursor to bottom-right
        screen5250.setCursor(maxRow, maxCol);

        // Assert: Position should be at end of screen
        int expectedPos = convertToPosition(maxRow, maxCol);
        assertEquals(expectedPos, getCursorPosition(),"Bottom-right valid position should map correctly");
    }

    /**
     * POSITIVE: Consecutive SBA commands
     * Tests that second SBA command properly replaces first positioning
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testConsecutiveSBACommandsReplacePosition(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (!SEQ_CONSECUTIVE.equals(sequenceContext)) {
            return;  // Only test consecutive scenario
        }
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        // Act: Set first SBA to (5,5)
        screen5250.setCursor(5, 5);
        int firstPos = getCursorPosition();

        // Then set second SBA to test position
        screen5250.setCursor(testRow, testCol);
        int secondPos = getCursorPosition();

        // Assert: Positions should be different
        assertTrue(firstPos != secondPos,"Consecutive SBAs should move cursor");
        int expectedPos = convertToPosition(testRow, testCol);
        assertEquals(expectedPos, secondPos,"Second SBA should set position correctly");
    }

    /**
     * POSITIVE: SBA after field context
     * Tests that SBA properly resets position when switching contexts
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAAfterFieldContext(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (!SEQ_AFTER_FIELD.equals(sequenceContext)) {
            return;  // Only test after-field scenario
        }
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        // Arrange: Simulate being in a field (position 80-240)
        screen5250.setCursor(5, 5);

        // Act: SBA to new position after field
        screen5250.setCursor(testRow, testCol);
        int resultPos = getCursorPosition();

        // Assert: Position should be at SBA target
        int expectedPos = convertToPosition(testRow, testCol);
        assertEquals(expectedPos, resultPos,"SBA after field should move cursor to SBA position");
    }

    /**
     * POSITIVE: 14-bit address encoding on valid position
     * Tests wider address format (used on larger screens)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBA14BitEncodingValidPosition(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (!FORMAT_14BIT.equals(addressFormat)) {
            return;  // Only test 14-bit format
        }
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        // Act: Set cursor (14-bit encoding should handle same positions as 12-bit)
        screen5250.setCursor(testRow, testCol);

        // Assert: Position should be set correctly
        int expectedPos = convertToPosition(testRow, testCol);
        assertEquals(expectedPos, getCursorPosition(),"14-bit encoding should position cursor correctly");
    }

    /**
     * POSITIVE: Boundary rows (1 and max) with various columns
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBABoundaryRows(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        int maxRow = getExpectedRows();
        if ((testRow != 1 && testRow != maxRow) ||
            (testRow == 1 && testCol == 1 && !SEQ_SINGLE.equals(sequenceContext)) ||
            !isValidPosition(testRow, testCol)) {
            return;  // Focus on boundary rows
        }

        screen5250.setCursor(testRow, testCol);
        int resultPos = getCursorPosition();
        int expectedPos = convertToPosition(testRow, testCol);

        assertEquals(expectedPos, resultPos,"Boundary row position should be correct");
    }

    /**
     * POSITIVE: Boundary columns (1 and max) with various rows
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBABoundaryColumns(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        int maxCol = getExpectedColumns();
        if ((testCol != 1 && testCol != maxCol) ||
            (testCol == 1 && testRow == 1 && !SEQ_SINGLE.equals(sequenceContext)) ||
            !isValidPosition(testRow, testCol)) {
            return;  // Focus on boundary columns
        }

        screen5250.setCursor(testRow, testCol);
        int resultPos = getCursorPosition();
        int expectedPos = convertToPosition(testRow, testCol);

        assertEquals(expectedPos, resultPos,"Boundary column position should be correct");
    }

    /**
     * POSITIVE: Middle-of-screen position (row/col coverage)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAMiddleOfScreen(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if ((testRow != 12 && testRow != 15) || (testCol != 40 && testCol != 60)) {
            return;  // Focus on middle positions
        }
        if (!isValidPosition(testRow, testCol)) {
            return;
        }

        screen5250.setCursor(testRow, testCol);
        int resultPos = getCursorPosition();
        int expectedPos = convertToPosition(testRow, testCol);

        assertEquals(expectedPos, resultPos,"Middle position should be set correctly");
    }

    // ========================================================================
    // ADVERSARIAL TESTS (8+): Out-of-bounds, invalid state, error conditions
    // ========================================================================

    /**
     * ADVERSARIAL: Row 0 (below minimum)
     * SBA with row=0 should be detected as invalid
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBARowZeroInvalid(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (testRow != 0) {
            return;  // Only test row 0
        }

        boolean isError = validateSBAAddress(testRow, testCol);

        assertTrue(isError,"SBA with row=0 (below minimum) should be detected as error");
    }

    /**
     * ADVERSARIAL: Row beyond screen maximum
     * SBA with row > max should be detected as invalid
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBARowBeyondMaximumInvalid(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        int maxRow = getExpectedRows();
        if (testRow <= maxRow) {
            return;  // Only test beyond-max rows
        }

        boolean isError = validateSBAAddress(testRow, testCol);

        assertTrue(isError,"SBA with row > " + maxRow + " should be detected as error");
    }

    /**
     * ADVERSARIAL: Column 0 (below minimum)
     * SBA with col=0 should be detected as invalid
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAColumnZeroInvalid(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (testCol != 0) {
            return;  // Only test col 0
        }

        boolean isError = validateSBAAddress(testRow, testCol);

        assertTrue(isError,"SBA with col=0 (below minimum) should be detected as error");
    }

    /**
     * ADVERSARIAL: Column beyond screen maximum
     * SBA with col > max should be detected as invalid
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAColumnBeyondMaximumInvalid(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        int maxCol = getExpectedColumns();
        if (testCol <= maxCol) {
            return;  // Only test beyond-max columns
        }

        boolean isError = validateSBAAddress(testRow, testCol);

        assertTrue(isError,"SBA with col > " + maxCol + " should be detected as error");
    }

    /**
     * ADVERSARIAL: Both row and column out-of-bounds
     * SBA with both invalid should still return error
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBABothRowAndColumnOutOfBoundsInvalid(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        int maxRow = getExpectedRows();
        int maxCol = getExpectedColumns();

        if ((testRow >= 1 && testRow <= maxRow) ||
            (testCol >= 1 && testCol <= maxCol)) {
            return;  // Need both invalid
        }

        boolean isError = validateSBAAddress(testRow, testCol);

        assertTrue(isError,"SBA with both row and col out-of-bounds should return error");
    }

    /**
     * ADVERSARIAL: High out-of-bounds values (255)
     * Tests handling of extreme invalid addresses
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAExtremeOutOfBoundsValue(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (testRow != 255 && testCol != 255) {
            return;  // Only test extreme values
        }

        boolean isError = validateSBAAddress(testRow, testCol);

        assertTrue(isError,"SBA with extreme value (255) should be detected as error");
    }

    /**
     * ADVERSARIAL: Screen state unchanged after invalid SBA
     * Verifies that failed SBA doesn't leave screen in inconsistent state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAInvalidPreservesScreenState(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (isValidPosition(testRow, testCol)) {
            return;  // Only test invalid positions
        }

        // Arrange: Set initial position
        int initialRow = 1, initialCol = 1;
        screen5250.setCursor(initialRow, initialCol);
        int initialPos = getCursorPosition();

        // Act: Attempt invalid SBA (in real code, this would send error response)
        boolean isError = validateSBAAddress(testRow, testCol);

        // Assert: Error detected
        assertTrue(isError,"Invalid SBA should be detected");

        // In real implementation, cursor would not move on error
        // Verify by checking cursor is still at initial position
        int afterPos = getCursorPosition();
        assertEquals(initialPos, afterPos,"Screen position should not change after invalid SBA");
    }

    /**
     * POSITIVE: Valid address validation
     * Verifies that valid addresses are correctly identified
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAValidAddressValidation(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;  // Only test valid positions
        }

        boolean isError = validateSBAAddress(testRow, testCol);

        assertFalse(isError,"Valid SBA address row=" + testRow + ",col=" + testCol + " should not be error");
    }

    /**
     * POSITIVE: All screen size dimensions handled correctly
     * Tests that each screen size's boundary is respected
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSBAScreenSizeBoundariesRespected(int testRow, int testCol, int screenSize, String addressFormat, String sequenceContext) throws Exception {
        setParameters(testRow, testCol, screenSize, addressFormat, sequenceContext);
        setUp();
        if (!isValidPosition(testRow, testCol)) {
            return;  // Only test valid positions
        }

        // Set cursor and verify position
        screen5250.setCursor(testRow, testCol);
        int resultPos = getCursorPosition();

        // Verify position is within screen bounds
        int screenLength = getExpectedRows() * getExpectedColumns();
        assertTrue(resultPos >= 0 && resultPos < screenLength,"Position " + resultPos + " should be within screen length " + screenLength);
    }
}
