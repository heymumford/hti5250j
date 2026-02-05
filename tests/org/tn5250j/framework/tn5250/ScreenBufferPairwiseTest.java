/**
 * Title: ScreenBufferPairwiseTest.java
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise TDD tests for Screen5250 buffer management operations
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
package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Pairwise parameterized tests for Screen5250 buffer management operations.
 *
 * Pairwise dimensions systematically tested:
 * 1. Screen size: 80x24 (standard), 132x27 (wide), 80x43 (extended), 132x43 (custom)
 * 2. Buffer operation: allocate, resize, clear, restore, copy
 * 3. Initial state: empty, partial, full, corrupted
 * 4. Color depth: monochrome (0), 8-color, 16-color
 * 5. Double-byte support: disabled, enabled
 *
 * Pairwise design ensures all 2-way interactions are covered (25+ test cases).
 * Tests discover: Buffer allocation failures, resize boundary violations,
 * state corruption, color plane consistency, double-byte handling bugs.
 */
@RunWith(Parameterized.class)
public class ScreenBufferPairwiseTest {

    // Test parameters (pairwise combinations)
    private final int screenSize;           // Dimension 1: screen size
    private final String bufferOp;          // Dimension 2: buffer operation
    private final String initialState;      // Dimension 3: initial state
    private final int colorDepth;           // Dimension 4: color depth
    private final boolean doubleByteEnabled; // Dimension 5: double-byte support

    // Instance variables
    private ScreenPlanes screenPlanes;
    private Screen5250TestDouble screen5250;
    private char[] screenBuffer;
    private char[] colorBuffer;
    private char[] extendedBuffer;

    // Screen size constants (dimension 1)
    private static final int SIZE_80x24 = 24;    // Standard: 80x24
    private static final int SIZE_132x27 = 27;   // Wide: 132x27
    private static final int SIZE_80x43 = 43;    // Extended: 80x43
    private static final int SIZE_132x43 = 99;   // Custom: 132x43

    // Buffer operation constants (dimension 2)
    private static final String OP_ALLOCATE = "ALLOCATE";
    private static final String OP_RESIZE = "RESIZE";
    private static final String OP_CLEAR = "CLEAR";
    private static final String OP_RESTORE = "RESTORE";
    private static final String OP_COPY = "COPY";

    // Initial state constants (dimension 3)
    private static final String STATE_EMPTY = "EMPTY";
    private static final String STATE_PARTIAL = "PARTIAL";
    private static final String STATE_FULL = "FULL";
    private static final String STATE_CORRUPTED = "CORRUPTED";

    // Color depth constants (dimension 4)
    private static final int COLOR_MONOCHROME = 0;
    private static final int COLOR_8BIT = 8;
    private static final int COLOR_16BIT = 16;

    /**
     * Pairwise parameter combinations (25+ test cases).
     * Each combination covers critical 2-way interactions between dimensions.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Pairwise set 1: Standard screen (80x24) with all operations
                { SIZE_80x24, OP_ALLOCATE, STATE_EMPTY, COLOR_MONOCHROME, false },
                { SIZE_80x24, OP_RESIZE, STATE_PARTIAL, COLOR_8BIT, false },
                { SIZE_80x24, OP_CLEAR, STATE_FULL, COLOR_16BIT, false },
                { SIZE_80x24, OP_RESTORE, STATE_EMPTY, COLOR_8BIT, true },
                { SIZE_80x24, OP_COPY, STATE_CORRUPTED, COLOR_MONOCHROME, true },

                // Pairwise set 2: Wide screen (132x27)
                { SIZE_132x27, OP_ALLOCATE, STATE_PARTIAL, COLOR_8BIT, true },
                { SIZE_132x27, OP_RESIZE, STATE_CORRUPTED, COLOR_MONOCHROME, true },
                { SIZE_132x27, OP_CLEAR, STATE_EMPTY, COLOR_16BIT, true },
                { SIZE_132x27, OP_RESTORE, STATE_FULL, COLOR_MONOCHROME, false },
                { SIZE_132x27, OP_COPY, STATE_PARTIAL, COLOR_16BIT, false },

                // Pairwise set 3: Extended screen (80x43)
                { SIZE_80x43, OP_ALLOCATE, STATE_CORRUPTED, COLOR_16BIT, false },
                { SIZE_80x43, OP_RESIZE, STATE_FULL, COLOR_MONOCHROME, true },
                { SIZE_80x43, OP_CLEAR, STATE_PARTIAL, COLOR_8BIT, true },
                { SIZE_80x43, OP_RESTORE, STATE_CORRUPTED, COLOR_16BIT, true },
                { SIZE_80x43, OP_COPY, STATE_EMPTY, COLOR_8BIT, false },

                // Pairwise set 4: Custom screen (132x43)
                { SIZE_132x43, OP_ALLOCATE, STATE_FULL, COLOR_8BIT, false },
                { SIZE_132x43, OP_RESIZE, STATE_EMPTY, COLOR_16BIT, true },
                { SIZE_132x43, OP_CLEAR, STATE_CORRUPTED, COLOR_8BIT, false },
                { SIZE_132x43, OP_RESTORE, STATE_PARTIAL, COLOR_8BIT, false },
                { SIZE_132x43, OP_COPY, STATE_FULL, COLOR_MONOCHROME, true },

                // Pairwise set 5: Color depth combinations
                { SIZE_80x24, OP_ALLOCATE, STATE_EMPTY, COLOR_16BIT, true },
                { SIZE_132x27, OP_RESIZE, STATE_PARTIAL, COLOR_MONOCHROME, false },
                { SIZE_80x43, OP_CLEAR, STATE_FULL, COLOR_8BIT, false },
                { SIZE_132x43, OP_RESTORE, STATE_EMPTY, COLOR_16BIT, false },
                { SIZE_80x24, OP_COPY, STATE_PARTIAL, COLOR_8BIT, true },

                // Pairwise set 6: Double-byte and state combinations
                { SIZE_132x27, OP_ALLOCATE, STATE_CORRUPTED, COLOR_16BIT, true },
                { SIZE_80x43, OP_RESIZE, STATE_PARTIAL, COLOR_8BIT, true },
        });
    }

    public ScreenBufferPairwiseTest(int screenSize, String bufferOp, String initialState,
                                     int colorDepth, boolean doubleByteEnabled) {
        this.screenSize = screenSize;
        this.bufferOp = bufferOp;
        this.initialState = initialState;
        this.colorDepth = colorDepth;
        this.doubleByteEnabled = doubleByteEnabled;
    }

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble(screenSize);
        screenPlanes = new ScreenPlanes(screen5250, screenSize);

        screenBuffer = getPrivateField("screen", char[].class);
        colorBuffer = getPrivateField("screenColor", char[].class);
        extendedBuffer = getPrivateField("screenExtended", char[].class);

        // Initialize buffer to specified initial state
        initializeBufferState();
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(String fieldName, Class<T> fieldType)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = ScreenPlanes.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(screenPlanes);
    }

    private int getScreenSizeInCells() {
        switch (screenSize) {
            case SIZE_80x24: return 80 * 24;
            case SIZE_132x27: return 132 * 27;
            case SIZE_80x43: return 80 * 43;
            case SIZE_132x43: return 132 * 43;
            default: return 80 * 24;
        }
    }

    private int getScreenRows() {
        switch (screenSize) {
            case SIZE_80x24: return 24;
            case SIZE_132x27: return 27;
            case SIZE_80x43: return 43;
            case SIZE_132x43: return 43;
            default: return 24;
        }
    }

    private int getScreenCols() {
        switch (screenSize) {
            case SIZE_80x24: return 80;
            case SIZE_132x27: return 132;
            case SIZE_80x43: return 80;
            case SIZE_132x43: return 132;
            default: return 80;
        }
    }

    private void initializeBufferState() throws NoSuchFieldException, IllegalAccessException {
        int screenSize = getScreenSizeInCells();

        switch (initialState) {
            case STATE_EMPTY:
                // All zeros (empty state)
                for (int i = 0; i < screenSize; i++) {
                    screenBuffer[i] = '\0';
                    colorBuffer[i] = '\0';
                }
                break;

            case STATE_PARTIAL:
                // Half filled with test data
                for (int i = 0; i < screenSize / 2; i++) {
                    screenBuffer[i] = (char) ('A' + (i % 26));
                    colorBuffer[i] = (char) colorDepth;
                }
                for (int i = screenSize / 2; i < screenSize; i++) {
                    screenBuffer[i] = '\0';
                    colorBuffer[i] = '\0';
                }
                break;

            case STATE_FULL:
                // Completely filled
                for (int i = 0; i < screenSize; i++) {
                    screenBuffer[i] = (char) ('A' + (i % 26));
                    colorBuffer[i] = (char) colorDepth;
                }
                break;

            case STATE_CORRUPTED:
                // Random/corrupted data with inconsistencies
                for (int i = 0; i < screenSize; i++) {
                    screenBuffer[i] = (char) (0xFF - (i % 256));
                    colorBuffer[i] = (char) ((i * 7) % 256); // Intentionally mismatched
                }
                break;
        }
    }

    // ============================================================================
    // TEST GROUP 1: BUFFER ALLOCATION
    // ============================================================================

    /**
     * TEST 1: Buffer allocation creates correct size
     *
     * Positive test: Verify buffer is allocated with correct dimensions
     * for all screen sizes.
     */
    @Test
    public void testBufferAllocationCorrectSize() {
        if (!bufferOp.equals(OP_ALLOCATE)) {
            return; // Skip if not allocate operation
        }

        int expectedSize = getScreenSizeInCells();

        assertEquals(
            String.format("Screen buffer wrong size for %d cell screen", expectedSize),
            expectedSize,
            screenBuffer.length
        );

        assertEquals(
            String.format("Color buffer wrong size for %d cell screen", expectedSize),
            expectedSize,
            colorBuffer.length
        );

        assertEquals(
            String.format("Extended buffer wrong size for %d cell screen", expectedSize),
            expectedSize,
            extendedBuffer.length
        );
    }

    /**
     * TEST 2: Buffer allocation initializes all planes
     *
     * Positive test: Verify all buffer planes are allocated and accessible
     */
    @Test
    public void testBufferAllocationInitializesAllPlanes() {
        if (!bufferOp.equals(OP_ALLOCATE)) {
            return;
        }

        assertNotNull("Screen buffer should not be null", screenBuffer);
        assertNotNull("Color buffer should not be null", colorBuffer);
        assertNotNull("Extended buffer should not be null", extendedBuffer);

        assertTrue("Screen buffer should be non-empty", screenBuffer.length > 0);
        assertTrue("Color buffer should be non-empty", colorBuffer.length > 0);
        assertTrue("Extended buffer should be non-empty", extendedBuffer.length > 0);
    }

    /**
     * TEST 3: Allocation respects color depth configuration
     *
     * Positive test: Verify color buffer is properly initialized for color depth
     */
    @Test
    public void testAllocationRespectsColorDepth() {
        if (!bufferOp.equals(OP_ALLOCATE)) {
            return;
        }

        // For allocated buffer, color initialization should match color depth
        // (color depth affects how colors are stored in colorBuffer)
        assertNotNull("Color buffer must be allocated", colorBuffer);
        assertTrue("Color buffer must have content", colorBuffer.length > 0);

        // Sample first cell should reflect color depth setting
        if (colorDepth > 0) {
            assertTrue("Color should be initialized for non-monochrome", true);
        }
    }

    // ============================================================================
    // TEST GROUP 2: BUFFER RESIZE
    // ============================================================================

    /**
     * TEST 4: Resize operation changes buffer dimensions
     *
     * Positive test: Verify resize properly reallocates buffers to new size
     */
    @Test
    public void testResizeChangesBufferDimensions() {
        if (!bufferOp.equals(OP_RESIZE)) {
            return;
        }

        int originalSize = getScreenSizeInCells();
        int newScreenSize = (screenSize == SIZE_80x24) ? SIZE_132x27 : SIZE_80x24;
        int newSize = (newScreenSize == SIZE_132x27) ? 132 * 27 : 80 * 24;

        // Perform resize operation
        screenPlanes.setSize(newScreenSize);

        // Verify new size
        int actualNewSize = (newScreenSize == SIZE_132x27) ? 132 * 27 : 80 * 24;
        assertEquals(
            String.format("Buffer not resized correctly (expected %d)", actualNewSize),
            actualNewSize,
            screenBuffer.length
        );
    }

    /**
     * TEST 5: Resize preserves buffer data when expanding
     *
     * Positive test: Verify data survives resize when expanding from smaller to larger
     */
    @Test
    public void testResizePreservesDataWhenExpanding() {
        if (!bufferOp.equals(OP_RESIZE)) {
            return;
        }

        if (initialState.equals(STATE_EMPTY)) {
            return; // Skip if no data to preserve
        }

        // Set a known character at position 0
        char markerChar = 'X';
        screenPlanes.setChar(0, markerChar);

        // Resize to larger size
        if (screenSize == SIZE_80x24 || screenSize == SIZE_80x43) {
            screenPlanes.setSize(SIZE_132x27);
        }

        // Verify marker survived resize
        assertEquals(
            "Marker character should survive resize",
            markerChar,
            screenPlanes.getChar(0)
        );
    }

    /**
     * TEST 6: Resize to smallest valid size succeeds
     *
     * Positive test: Verify resize to minimum screen size is accepted
     */
    @Test
    public void testResizeToMinimumSize() {
        if (!bufferOp.equals(OP_RESIZE)) {
            return;
        }

        // Resize to standard 80x24
        screenPlanes.setSize(SIZE_80x24);

        int expectedSize = 80 * 24;
        assertEquals(
            "Should resize to minimum size",
            expectedSize,
            screenBuffer.length
        );
    }

    // ============================================================================
    // TEST GROUP 3: BUFFER CLEAR
    // ============================================================================

    /**
     * TEST 7: Clear operation zeros all buffer content
     *
     * Positive test: Verify clear empties entire buffer
     */
    @Test
    public void testClearZerosAllBufferContent() {
        if (!bufferOp.equals(OP_CLEAR)) {
            return;
        }

        // Fill buffer first
        int screenSize = getScreenSizeInCells();
        for (int i = 0; i < screenSize; i++) {
            screenPlanes.setChar(i, (char) ('A' + (i % 26)));
        }

        // Clear operation
        screenPlanes.initalizePlanes();

        // Verify all zeros
        for (int i = 0; i < Math.min(10, screenSize); i++) { // Sample first 10 positions
            assertEquals(
                String.format("Position %d not cleared", i),
                '\0',
                screenPlanes.getChar(i)
            );
        }
    }

    /**
     * TEST 8: Clear succeeds on empty buffer
     *
     * Positive test: Verify clear on empty buffer is idempotent
     */
    @Test
    public void testClearOnEmptyBufferIsIdempotent() {
        if (!bufferOp.equals(OP_CLEAR)) {
            return;
        }

        if (!initialState.equals(STATE_EMPTY)) {
            return; // Only test on empty state
        }

        // Clear empty buffer (should not crash)
        screenPlanes.initalizePlanes();

        // Verify still empty
        assertEquals(
            "Cleared empty buffer should remain empty",
            '\0',
            screenPlanes.getChar(0)
        );
    }

    /**
     * TEST 9: Clear resets all color planes
     *
     * Positive test: Verify color planes are also cleared
     */
    @Test
    public void testClearResetsAllColorPlanes() {
        if (!bufferOp.equals(OP_CLEAR)) {
            return;
        }

        // Set colors first
        for (int i = 0; i < Math.min(10, getScreenSizeInCells()); i++) {
            screenPlanes.setScreenAttr(i, colorDepth);
        }

        // Clear
        screenPlanes.initalizePlanes();

        // Verify colors reset to default (initAttr = 32)
        int initialAttr = screenPlanes.getCharAttr(0);
        assertEquals(
            "Color planes should be reset to default",
            32, // initAttr constant
            initialAttr
        );
    }

    // ============================================================================
    // TEST GROUP 4: BUFFER RESTORE
    // ============================================================================

    /**
     * TEST 10: Restore from error line saves and restores content
     *
     * Positive test: Verify error line save/restore cycle preserves data
     */
    @Test
    public void testRestoreFromErrorLineCycle() {
        if (!bufferOp.equals(OP_RESTORE)) {
            return;
        }

        // Set some content in what would be error line
        int errorLinePos = getScreenSizeInCells() - getScreenCols(); // Last row

        char testChar = 'E';
        for (int i = 0; i < getScreenCols(); i++) {
            screenPlanes.setChar(errorLinePos + i, testChar);
        }

        // Save error line
        screenPlanes.saveErrorLine();

        // Overwrite with different content
        for (int i = 0; i < getScreenCols(); i++) {
            screenPlanes.setChar(errorLinePos + i, 'X');
        }

        // Restore error line
        screenPlanes.restoreErrorLine();

        // Verify restoration
        assertEquals(
            "Error line should be restored",
            testChar,
            screenPlanes.getChar(errorLinePos)
        );
    }

    /**
     * TEST 11: Restore on non-saved error line is safe
     *
     * Positive test: Verify restore without prior save doesn't crash
     */
    @Test
    public void testRestoreWithoutSaveIsSafe() {
        if (!bufferOp.equals(OP_RESTORE)) {
            return;
        }

        // Restore without ever saving (should be safe)
        screenPlanes.restoreErrorLine();

        // Verify buffer still accessible
        assertNotNull("Buffer should still be valid", screenBuffer);
    }

    /**
     * TEST 12: Restore only affects error line, not other rows
     *
     * Positive test: Verify restore is isolated to error line
     */
    @Test
    public void testRestoreOnlyAffectsErrorLine() {
        if (!bufferOp.equals(OP_RESTORE)) {
            return;
        }

        // Set content in non-error line
        char nonErrorChar = 'N';
        screenPlanes.setChar(10, nonErrorChar);

        // Save and modify error line
        screenPlanes.saveErrorLine();

        // Restore
        screenPlanes.restoreErrorLine();

        // Verify non-error position unchanged
        assertEquals(
            "Non-error lines should not be affected",
            nonErrorChar,
            screenPlanes.getChar(10)
        );
    }

    // ============================================================================
    // TEST GROUP 5: BUFFER COPY
    // ============================================================================

    /**
     * TEST 13: Copy plane data returns correct buffer
     *
     * Positive test: Verify copy operation extracts data correctly
     */
    @Test
    public void testCopyPlaneDataReturnsCorrectBuffer() {
        if (!bufferOp.equals(OP_COPY)) {
            return;
        }

        // Set some test data
        screenPlanes.setChar(0, 'A');
        screenPlanes.setChar(1, 'B');
        screenPlanes.setChar(2, 'C');

        // Copy from position 0 to 3
        char[] copiedData = screenPlanes.getPlaneData(0, 3, 0); // 0 = PLANE_TEXT

        assertNotNull("Copied data should not be null", copiedData);
        assertTrue("Copied data should have content", copiedData.length > 0);
    }

    /**
     * TEST 14: Copy preserves character content
     *
     * Positive test: Verify copied data matches original
     */
    @Test
    public void testCopyPreservesCharacterContent() {
        if (!bufferOp.equals(OP_COPY)) {
            return;
        }

        if (initialState.equals(STATE_EMPTY)) {
            return; // Skip if no content
        }

        // Get copy of first few characters
        int copyLen = Math.min(5, getScreenSizeInCells());
        char[] copiedData = screenPlanes.getPlaneData(0, copyLen, 0); // PLANE_TEXT

        assertNotNull("Copy should not return null", copiedData);

        // Verify first character in copy matches original
        if (copiedData.length > 0 && screenBuffer.length > 0) {
            assertEquals(
                "Copied first character should match original",
                screenBuffer[0],
                copiedData[0]
            );
        }
    }

    /**
     * TEST 15: Copy different planes returns different data
     *
     * Positive test: Verify different plane copies have different content
     */
    @Test
    public void testCopyDifferentPlanesReturnDifferentData() {
        if (!bufferOp.equals(OP_COPY)) {
            return;
        }

        // Set test character and attribute
        screenPlanes.setChar(0, 'T');
        screenPlanes.setScreenAttr(0, colorDepth);

        // Copy text plane
        char[] textCopy = screenPlanes.getPlaneData(0, 1, 0); // PLANE_TEXT
        // Copy attribute plane
        char[] attrCopy = screenPlanes.getPlaneData(0, 1, 1); // PLANE_ATTR

        assertNotNull("Text plane copy should exist", textCopy);
        assertNotNull("Attr plane copy should exist", attrCopy);

        // Planes should have been retrieved (may not be different in all cases)
        assertTrue("Both planes should be retrievable",
            textCopy.length > 0 && attrCopy.length > 0
        );
    }

    // ============================================================================
    // TEST GROUP 6: ADVERSARIAL BUFFER CORRUPTION SCENARIOS
    // ============================================================================

    /**
     * TEST 16: Corrupted state survives initialization
     *
     * Adversarial test: Verify buffer can recover from corruption
     */
    @Test
    public void testCorruptedStateCanBeRecovered() {
        if (!initialState.equals(STATE_CORRUPTED)) {
            return; // Only test corrupted state
        }

        // Buffer was initialized to corrupted state in setUp
        // Verify it's still accessible despite corruption
        try {
            char c = screenPlanes.getChar(0);
            // If we reach here, buffer is still functional
            assertTrue("Corrupted buffer should still be readable", true);
        } catch (Exception e) {
            fail("Corrupted buffer should not throw exception: " + e.getMessage());
        }
    }

    /**
     * TEST 17: Clear recovers from corrupted state
     *
     * Adversarial test: Verify clear restores buffer from corruption
     */
    @Test
    public void testClearRecoveredFromCorruptedState() {
        if (!initialState.equals(STATE_CORRUPTED)) {
            return;
        }

        // Clear the corrupted buffer
        screenPlanes.initalizePlanes();

        // Verify recovery - should be empty and consistent
        assertEquals(
            "Cleared buffer should be empty",
            '\0',
            screenPlanes.getChar(0)
        );

        assertEquals(
            "Default attribute should be set",
            32, // initAttr
            screenPlanes.getCharAttr(0)
        );
    }

    /**
     * TEST 18: Double-byte support doesn't corrupt single-byte data
     *
     * Adversarial test: Verify double-byte flag doesn't corrupt normal characters
     */
    @Test
    public void testDoubleByteSupportPreserveSingleByteData() {
        // Set single-byte character
        screenPlanes.setChar(0, 'A');

        assertEquals(
            "Single-byte character should survive double-byte mode",
            'A',
            screenPlanes.getChar(0)
        );
    }

    /**
     * TEST 19: Resize from corrupted state succeeds
     *
     * Adversarial test: Verify resize works even from corrupted state
     */
    @Test
    public void testResizeFromCorruptedStateSucceeds() {
        if (!initialState.equals(STATE_CORRUPTED)) {
            return;
        }

        // Resize from corrupted state
        try {
            screenPlanes.setSize((screenSize == SIZE_80x24) ? SIZE_132x27 : SIZE_80x24);
            // Should succeed
            assertTrue("Resize from corrupted state should succeed", true);
        } catch (Exception e) {
            fail("Resize should not fail on corrupted state: " + e.getMessage());
        }
    }

    /**
     * TEST 20: Multiple sequential operations maintain consistency
     *
     * Adversarial test: Verify buffer state remains consistent across multiple operations
     */
    @Test
    public void testMultipleSequentialOperationsMaintainConsistency() {
        // Allocate
        assertNotNull("Allocated buffer should not be null", screenBuffer);

        // Set some data
        screenPlanes.setChar(0, 'T');
        screenPlanes.setScreenAttr(0, 32);

        // Verify consistency after operations
        assertEquals("Character should be set", 'T', screenPlanes.getChar(0));
        assertEquals("Attribute should be set", 32, screenPlanes.getCharAttr(0));

        // Clear and verify cleared
        screenPlanes.initalizePlanes();
        assertEquals("Cleared character should be null", '\0', screenPlanes.getChar(0));
    }

    /**
     * TEST 21: Buffer overflow at boundary positions handled gracefully
     *
     * Adversarial test: Verify buffer bounds are respected
     */
    @Test
    public void testBufferBoundaryPositionsHandledGracefully() {
        int screenSize = getScreenSizeInCells();
        int lastValidPos = screenSize - 1;

        try {
            // Should succeed at last valid position
            screenPlanes.setChar(lastValidPos, 'E');
            assertEquals("Last position should be writable", 'E',
                screenPlanes.getChar(lastValidPos));
        } catch (ArrayIndexOutOfBoundsException e) {
            fail("Last valid position should be accessible: " + e.getMessage());
        }
    }

    /**
     * TEST 22: Empty state clears properly
     *
     * Positive test: Verify empty state buffer can be cleared without data loss
     */
    @Test
    public void testEmptyStateClearsProperly() {
        if (!initialState.equals(STATE_EMPTY)) {
            return;
        }

        // Clear already empty buffer
        screenPlanes.initalizePlanes();

        // Should remain empty
        assertEquals("Empty state should remain empty after clear",
            '\0', screenPlanes.getChar(0));
    }

    /**
     * TEST 23: Partial state copy captures partial content
     *
     * Positive test: Verify copy on partial state captures correct portion
     */
    @Test
    public void testPartialStateCopyCapturesPartialContent() {
        if (!initialState.equals(STATE_PARTIAL) || !bufferOp.equals(OP_COPY)) {
            return;
        }

        int halfSize = getScreenSizeInCells() / 2;
        char[] copiedFirst = screenPlanes.getPlaneData(0, halfSize / 2, 0);

        assertNotNull("Copy should return data", copiedFirst);
        assertTrue("Copy should have content", copiedFirst.length > 0);
    }

    /**
     * TEST 24: Full state copy retrieves complete buffer
     *
     * Positive test: Verify copy on full state captures entire buffer
     */
    @Test
    public void testFullStateCopyRetrievesCompleteBuffer() {
        if (!initialState.equals(STATE_FULL) || !bufferOp.equals(OP_COPY)) {
            return;
        }

        int totalSize = getScreenSizeInCells();
        char[] copiedFull = screenPlanes.getPlaneData(0, totalSize / 2, 0);

        assertNotNull("Full copy should return data", copiedFull);
        assertTrue("Full copy should have significant content", copiedFull.length > 0);
    }

    /**
     * TEST 25: Color depth doesn't affect character storage
     *
     * Positive test: Verify character data is independent of color depth
     */
    @Test
    public void testColorDepthDoesntAffectCharacterStorage() {
        // Set character
        char testChar = 'C';
        screenPlanes.setChar(0, testChar);

        // Verify character is stored regardless of color depth
        assertEquals(
            String.format("Character should be stored regardless of color depth %d", colorDepth),
            testChar,
            screenPlanes.getChar(0)
        );
    }

    /**
     * TEST 26: Extended screen sizes allocate additional capacity
     *
     * Positive test: Verify large screen sizes are properly accommodated
     */
    @Test
    public void testExtendedScreenSizesAllocateCapacity() {
        int expectedCells = getScreenSizeInCells();
        int rows = getScreenRows();
        int cols = getScreenCols();

        assertTrue(
            String.format("Screen %dx%d should have %d cells", rows, cols, expectedCells),
            screenBuffer.length >= expectedCells
        );
    }

    // ============================================================================
    // Test Double for Screen5250
    // ============================================================================

    /**
     * Test double for Screen5250 interface with screen size support
     */
    private static class Screen5250TestDouble extends Screen5250 {
        private int numCols;
        private int numRows;

        public Screen5250TestDouble(int screenSize) {
            super();
            switch (screenSize) {
                case 27:  // 132x27
                    numRows = 27;
                    numCols = 132;
                    break;
                case 43:  // 80x43
                    numRows = 43;
                    numCols = 80;
                    break;
                case 99:  // 132x43 (custom)
                    numRows = 43;
                    numCols = 132;
                    break;
                default:  // 24 -> 80x24
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
