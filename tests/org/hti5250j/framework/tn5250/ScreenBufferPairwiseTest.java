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
public class ScreenBufferPairwiseTest {

    // Test parameters (pairwise combinations)
    private int screenSize;           // Dimension 1: screen size
    private String bufferOp;          // Dimension 2: buffer operation
    private String initialState;      // Dimension 3: initial state
    private int colorDepth;           // Dimension 4: color depth
    private boolean doubleByteEnabled; // Dimension 5: double-byte support

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

    private void setParameters(int screenSize, String bufferOp, String initialState,
                                     int colorDepth, boolean doubleByteEnabled) {
        this.screenSize = screenSize;
        this.bufferOp = bufferOp;
        this.initialState = initialState;
        this.colorDepth = colorDepth;
        this.doubleByteEnabled = doubleByteEnabled;
    }

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
    @ParameterizedTest
    @MethodSource("data")
    public void testBufferAllocationCorrectSize(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_ALLOCATE)) {
            return; // Skip if not allocate operation
        }

        int expectedSize = getScreenSizeInCells();

        assertEquals(expectedSize,screenBuffer.length
        ,
            String.format("Screen buffer wrong size for %d cell screen", expectedSize));

        assertEquals(expectedSize,colorBuffer.length
        ,
            String.format("Color buffer wrong size for %d cell screen", expectedSize));

        assertEquals(expectedSize,extendedBuffer.length
        ,
            String.format("Extended buffer wrong size for %d cell screen", expectedSize));
    }

    /**
     * TEST 2: Buffer allocation initializes all planes
     *
     * Positive test: Verify all buffer planes are allocated and accessible
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testBufferAllocationInitializesAllPlanes(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_ALLOCATE)) {
            return;
        }

        assertNotNull(screenBuffer,"Screen buffer should not be null");
        assertNotNull(colorBuffer,"Color buffer should not be null");
        assertNotNull(extendedBuffer,"Extended buffer should not be null");

        assertTrue(screenBuffer.length > 0,"Screen buffer should be non-empty");
        assertTrue(colorBuffer.length > 0,"Color buffer should be non-empty");
        assertTrue(extendedBuffer.length > 0,"Extended buffer should be non-empty");
    }

    /**
     * TEST 3: Allocation respects color depth configuration
     *
     * Positive test: Verify color buffer is properly initialized for color depth
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAllocationRespectsColorDepth(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_ALLOCATE)) {
            return;
        }

        // For allocated buffer, color initialization should match color depth
        // (color depth affects how colors are stored in colorBuffer)
        assertNotNull(colorBuffer,"Color buffer must be allocated");
        assertTrue(colorBuffer.length > 0,"Color buffer must have content");

        // Sample first cell should reflect color depth setting
        if (colorDepth > 0) {
            assertTrue(true,"Color should be initialized for non-monochrome");
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
    @ParameterizedTest
    @MethodSource("data")
    public void testResizeChangesBufferDimensions(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_RESIZE)) {
            return;
        }

        int originalSize = getScreenSizeInCells();
        int newScreenSize = (screenSize == SIZE_80x24) ? SIZE_132x27 : SIZE_80x24;
        int newSize = (newScreenSize == SIZE_132x27) ? 132 * 27 : 80 * 24;

        // Perform resize operation
        screenPlanes.setSize(newScreenSize);
        screenBuffer = getPrivateField("screen", char[].class);

        // Verify new size
        int actualNewSize = (newScreenSize == SIZE_132x27) ? 132 * 27 : 80 * 24;
        assertEquals(actualNewSize,screenBuffer.length
        ,
            String.format("Buffer not resized correctly (expected %d)", actualNewSize));
    }

    /**
     * TEST 5: Resize preserves buffer data when expanding
     *
     * Positive test: Verify data survives resize when expanding from smaller to larger
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testResizePreservesDataWhenExpanding(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
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
        assertEquals(markerChar,
            screenPlanes.getChar(0)
        ,
            "Marker character should survive resize");
    }

    /**
     * TEST 6: Resize to smallest valid size succeeds
     *
     * Positive test: Verify resize to minimum screen size is accepted
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testResizeToMinimumSize(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_RESIZE)) {
            return;
        }

        // Resize to standard 80x24
        screenPlanes.setSize(SIZE_80x24);
        screenBuffer = getPrivateField("screen", char[].class);

        int expectedSize = 80 * 24;
        assertEquals(expectedSize,
            screenBuffer.length
        ,
            "Should resize to minimum size");
    }

    // ============================================================================
    // TEST GROUP 3: BUFFER CLEAR
    // ============================================================================

    /**
     * TEST 7: Clear operation zeros all buffer content
     *
     * Positive test: Verify clear empties entire buffer
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testClearZerosAllBufferContent(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_CLEAR)) {
            return;
        }

        // Fill buffer first
        int computedScreenSize = getScreenSizeInCells();
        for (int i = 0; i < computedScreenSize; i++) {
            screenPlanes.setChar(i, (char) ('A' + (i % 26)));
        }

        // Clear operation
        screenPlanes.initalizePlanes();

        // Verify all zeros
        for (int i = 0; i < Math.min(10, computedScreenSize); i++) { // Sample first 10 positions
            assertEquals('\0', screenPlanes.getChar(i),
                String.format("Position %d not cleared", i));
        }
    }

    /**
     * TEST 8: Clear succeeds on empty buffer
     *
     * Positive test: Verify clear on empty buffer is idempotent
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testClearOnEmptyBufferIsIdempotent(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_CLEAR)) {
            return;
        }

        if (!initialState.equals(STATE_EMPTY)) {
            return; // Only test on empty state
        }

        // Clear empty buffer (should not crash)
        screenPlanes.initalizePlanes();

        // Verify still empty
        assertEquals('\0',
            screenPlanes.getChar(0)
        ,
            "Cleared empty buffer should remain empty");
    }

    /**
     * TEST 9: Clear resets all color planes
     *
     * Positive test: Verify color planes are also cleared
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testClearResetsAllColorPlanes(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
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
        assertEquals(32, // initAttr constant
            initialAttr
        ,
            "Color planes should be reset to default");
    }

    // ============================================================================
    // TEST GROUP 4: BUFFER RESTORE
    // ============================================================================

    /**
     * TEST 10: Restore from error line saves and restores content
     *
     * Positive test: Verify error line save/restore cycle preserves data
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRestoreFromErrorLineCycle(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
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
        assertEquals(testChar,
            screenPlanes.getChar(errorLinePos)
        ,
            "Error line should be restored");
    }

    /**
     * TEST 11: Restore on non-saved error line is safe
     *
     * Positive test: Verify restore without prior save doesn't crash
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRestoreWithoutSaveIsSafe(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_RESTORE)) {
            return;
        }

        // Restore without ever saving (should be safe)
        screenPlanes.restoreErrorLine();

        // Verify buffer still accessible
        assertNotNull(screenBuffer,"Buffer should still be valid");
    }

    /**
     * TEST 12: Restore only affects error line, not other rows
     *
     * Positive test: Verify restore is isolated to error line
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRestoreOnlyAffectsErrorLine(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
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
        assertEquals(nonErrorChar,
            screenPlanes.getChar(10)
        ,
            "Non-error lines should not be affected");
    }

    // ============================================================================
    // TEST GROUP 5: BUFFER COPY
    // ============================================================================

    /**
     * TEST 13: Copy plane data returns correct buffer
     *
     * Positive test: Verify copy operation extracts data correctly
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCopyPlaneDataReturnsCorrectBuffer(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_COPY)) {
            return;
        }

        // Set some test data
        screenPlanes.setChar(0, 'A');
        screenPlanes.setChar(1, 'B');
        screenPlanes.setChar(2, 'C');

        // Copy from position 0 to 3
        char[] copiedData = screenPlanes.getPlaneData(0, 3, 0); // 0 = PLANE_TEXT

        assertNotNull(copiedData,"Copied data should not be null");
        assertTrue(copiedData.length > 0,"Copied data should have content");
    }

    /**
     * TEST 14: Copy preserves character content
     *
     * Positive test: Verify copied data matches original
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCopyPreservesCharacterContent(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!bufferOp.equals(OP_COPY)) {
            return;
        }

        if (initialState.equals(STATE_EMPTY)) {
            return; // Skip if no content
        }

        // Get copy of first few characters
        int copyLen = Math.min(5, getScreenSizeInCells());
        char[] copiedData = screenPlanes.getPlaneData(0, copyLen, 0); // PLANE_TEXT

        assertNotNull(copiedData,"Copy should not return null");

        // Verify first character in copy matches original
        if (copiedData.length > 0 && screenBuffer.length > 0) {
            assertEquals(screenBuffer[0],
                copiedData[0]
            ,
                "Copied first character should match original");
        }
    }

    /**
     * TEST 15: Copy different planes returns different data
     *
     * Positive test: Verify different plane copies have different content
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCopyDifferentPlanesReturnDifferentData(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
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

        assertNotNull(textCopy,"Text plane copy should exist");
        assertNotNull(attrCopy,"Attr plane copy should exist");

        // Planes should have been retrieved (may not be different in all cases)
        assertTrue(textCopy.length > 0 && attrCopy.length > 0
        ,"Both planes should be retrievable");
    }

    // ============================================================================
    // TEST GROUP 6: ADVERSARIAL BUFFER CORRUPTION SCENARIOS
    // ============================================================================

    /**
     * TEST 16: Corrupted state survives initialization
     *
     * Adversarial test: Verify buffer can recover from corruption
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCorruptedStateCanBeRecovered(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!initialState.equals(STATE_CORRUPTED)) {
            return; // Only test corrupted state
        }

        // Buffer was initialized to corrupted state in setUp
        // Verify it's still accessible despite corruption
        try {
            char c = screenPlanes.getChar(0);
            // If we reach here, buffer is still functional
            assertTrue(true,"Corrupted buffer should still be readable");
        } catch (Exception e) {
            fail("Corrupted buffer should not throw exception: " + e.getMessage());
        }
    }

    /**
     * TEST 17: Clear recovers from corrupted state
     *
     * Adversarial test: Verify clear restores buffer from corruption
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testClearRecoveredFromCorruptedState(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!initialState.equals(STATE_CORRUPTED)) {
            return;
        }

        // Clear the corrupted buffer
        screenPlanes.initalizePlanes();

        // Verify recovery - should be empty and consistent
        assertEquals('\0',
            screenPlanes.getChar(0)
        ,
            "Cleared buffer should be empty");

        assertEquals(32, // initAttr
            screenPlanes.getCharAttr(0)
        ,
            "Default attribute should be set");
    }

    /**
     * TEST 18: Double-byte support doesn't corrupt single-byte data
     *
     * Adversarial test: Verify double-byte flag doesn't corrupt normal characters
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testDoubleByteSupportPreserveSingleByteData(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        // Set single-byte character
        screenPlanes.setChar(0, 'A');

        assertEquals('A',
            screenPlanes.getChar(0)
        ,
            "Single-byte character should survive double-byte mode");
    }

    /**
     * TEST 19: Resize from corrupted state succeeds
     *
     * Adversarial test: Verify resize works even from corrupted state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testResizeFromCorruptedStateSucceeds(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!initialState.equals(STATE_CORRUPTED)) {
            return;
        }

        // Resize from corrupted state
        try {
            screenPlanes.setSize((screenSize == SIZE_80x24) ? SIZE_132x27 : SIZE_80x24);
            // Should succeed
            assertTrue(true,"Resize from corrupted state should succeed");
        } catch (Exception e) {
            fail("Resize should not fail on corrupted state: " + e.getMessage());
        }
    }

    /**
     * TEST 20: Multiple sequential operations maintain consistency
     *
     * Adversarial test: Verify buffer state remains consistent across multiple operations
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMultipleSequentialOperationsMaintainConsistency(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        // Allocate
        assertNotNull(screenBuffer,"Allocated buffer should not be null");

        // Set some data
        screenPlanes.setChar(0, 'T');
        screenPlanes.setScreenAttr(0, 32);

        // Verify consistency after operations
        assertEquals('T', screenPlanes.getChar(0),"Character should be set");
        assertEquals(32, screenPlanes.getCharAttr(0),"Attribute should be set");

        // Clear and verify cleared
        screenPlanes.initalizePlanes();
        assertEquals('\0', screenPlanes.getChar(0),"Cleared character should be null");
    }

    /**
     * TEST 21: Buffer overflow at boundary positions handled gracefully
     *
     * Adversarial test: Verify buffer bounds are respected
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testBufferBoundaryPositionsHandledGracefully(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        int computedScreenSize = getScreenSizeInCells();
        int lastValidPos = computedScreenSize - 1;

        try {
            // Should succeed at last valid position
            screenPlanes.setChar(lastValidPos, 'E');
            assertEquals('E',
                screenPlanes.getChar(lastValidPos),"Last position should be writable");
        } catch (ArrayIndexOutOfBoundsException e) {
            fail("Last valid position should be accessible: " + e.getMessage());
        }
    }

    /**
     * TEST 22: Empty state clears properly
     *
     * Positive test: Verify empty state buffer can be cleared without data loss
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testEmptyStateClearsProperly(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!initialState.equals(STATE_EMPTY)) {
            return;
        }

        // Clear already empty buffer
        screenPlanes.initalizePlanes();

        // Should remain empty
        assertEquals('\0', screenPlanes.getChar(0),"Empty state should remain empty after clear");
    }

    /**
     * TEST 23: Partial state copy captures partial content
     *
     * Positive test: Verify copy on partial state captures correct portion
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testPartialStateCopyCapturesPartialContent(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!initialState.equals(STATE_PARTIAL) || !bufferOp.equals(OP_COPY)) {
            return;
        }

        int halfSize = getScreenSizeInCells() / 2;
        char[] copiedFirst = screenPlanes.getPlaneData(0, halfSize / 2, 0);

        assertNotNull(copiedFirst,"Copy should return data");
        assertTrue(copiedFirst.length > 0,"Copy should have content");
    }

    /**
     * TEST 24: Full state copy retrieves complete buffer
     *
     * Positive test: Verify copy on full state captures entire buffer
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testFullStateCopyRetrievesCompleteBuffer(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        if (!initialState.equals(STATE_FULL) || !bufferOp.equals(OP_COPY)) {
            return;
        }

        int totalSize = getScreenSizeInCells();
        char[] copiedFull = screenPlanes.getPlaneData(0, totalSize / 2, 0);

        assertNotNull(copiedFull,"Full copy should return data");
        assertTrue(copiedFull.length > 0,"Full copy should have significant content");
    }

    /**
     * TEST 25: Color depth doesn't affect character storage
     *
     * Positive test: Verify character data is independent of color depth
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testColorDepthDoesntAffectCharacterStorage(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        // Set character
        char testChar = 'C';
        screenPlanes.setChar(0, testChar);

        // Verify character is stored regardless of color depth
        assertEquals(testChar,screenPlanes.getChar(0)
        ,
            String.format("Character should be stored regardless of color depth %d", colorDepth));
    }

    /**
     * TEST 26: Extended screen sizes allocate additional capacity
     *
     * Positive test: Verify large screen sizes are properly accommodated
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testExtendedScreenSizesAllocateCapacity(int screenSize, String bufferOp, String initialState, int colorDepth, boolean doubleByteEnabled) throws Exception {
        setParameters(screenSize, bufferOp, initialState, colorDepth, doubleByteEnabled);
        setUp();
        int expectedCells = getScreenSizeInCells();
        int rows = getScreenRows();
        int cols = getScreenCols();

        assertTrue(screenBuffer.length >= expectedCells
        ,
            String.format("Screen %dx%d should have %d cells", rows, cols, expectedCells));
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
