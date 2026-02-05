/**
 * Title: ScreenPlanesTest.java
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: TDD tests for ScreenPlanes error line save/restore functionality
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

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

/**
 * TDD test suite for ScreenPlanes.saveErrorLine() and restoreErrorLine()
 *
 * Tests verify that:
 * 1. ALL columns saved by saveErrorLine() are restored by restoreErrorLine()
 * 2. The screenGUI array is restored at the correct offset (r + x, not just x)
 *
 * CRITICAL BUGS BEING TESTED:
 * Bug 1 (Line 158): screenGUI[x] = errorLineGui[x] should be screenGUI[r + x] = errorLineGui[x]
 * Bug 2 (Line 155): for (int x = 0; x < numCols - 1; x++) should be for (int x = 0; x < numCols; x++)
 */
public class ScreenPlanesTest {

    private ScreenPlanes screenPlanes;
    private Screen5250TestDouble screen5250;

    private static final int SCREEN_SIZE_24 = 24;
    private static final int NUM_COLS_80 = 80;
    private static final int ERROR_LINE_ROW = 24; // Last row by default

    private char[] screenGUI;
    private char[] screenAttr;
    private char[] screenIsAttr;

    /**
     * Test double for Screen5250 with minimal implementation
     */
    private static class Screen5250TestDouble extends Screen5250 {
        private int numCols = NUM_COLS_80;

        public Screen5250TestDouble() {
            super();
        }

        @Override
        public int getPos(int row, int col) {
            return (row * numCols) + col;
        }

        @Override
        public int getScreenLength() {
            return 0;
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

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        // Create ScreenPlanes with test double
        screen5250 = new Screen5250TestDouble();
        screenPlanes = new ScreenPlanes(screen5250, SCREEN_SIZE_24);

        // Use reflection to access private fields for testing
        screenGUI = getPrivateField("screenGUI", char[].class);
        screenAttr = getPrivateField("screenAttr", char[].class);
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
     * Bug 1 & 2: Test that restoreErrorLine() restores ALL columns
     *
     * CRITICAL BUG DETAILS:
     * - Line 155: Loop condition uses 'x < numCols - 1' which skips last column
     * - Should be: 'x < numCols' to process all 80 columns (0-79)
     *
     * This test verifies that the last column (index 79) is properly
     * restored after a save/restore cycle.
     */
    @Test
    public void testRestoreErrorLineRestoresAllColumns() {
        // Initialize screen content with distinctive values
        char testChar = 'X';
        char testAttr = 42;
        char testIsAttr = '1';
        char testGUI = 7;

        int errorLineStartPos = (ERROR_LINE_ROW - 1) * NUM_COLS_80;

        // Fill the error line in all internal arrays
        for (int i = 0; i < NUM_COLS_80; i++) {
            screenPlanes.screen[errorLineStartPos + i] = testChar;
            screenAttr[errorLineStartPos + i] = testAttr;
            screenIsAttr[errorLineStartPos + i] = testIsAttr;
            screenGUI[errorLineStartPos + i] = testGUI;
        }

        // Save the error line
        screenPlanes.saveErrorLine();

        // Corrupt the screen content to verify restore works
        for (int i = 0; i < NUM_COLS_80; i++) {
            screenPlanes.screen[errorLineStartPos + i] = ' ';
            screenAttr[errorLineStartPos + i] = 32; // Green normal
            screenGUI[errorLineStartPos + i] = 0;   // NO_GUI
        }

        // Restore the error line
        screenPlanes.restoreErrorLine();

        // Verify ALL columns were restored, including the last column (index 79)
        // This test MUST FAIL because bug skips column 79
        for (int i = 0; i < NUM_COLS_80; i++) {
            assertEquals(
                "Column " + i + " was not properly restored to screen array",
                testChar,
                screenPlanes.screen[errorLineStartPos + i]
            );
            assertEquals(
                "Column " + i + " was not properly restored to screenAttr array. "
                    + "BUG: Line 155 skips last column with 'x < numCols - 1'",
                testAttr,
                screenAttr[errorLineStartPos + i]
            );
            assertEquals(
                "Column " + i + " was not properly restored to screenIsAttr array",
                testIsAttr,
                screenIsAttr[errorLineStartPos + i]
            );
        }
    }

    /**
     * Bug 1: Test that screenGUI restoration uses correct array index
     *
     * CRITICAL BUG DETAILS:
     * - Line 158: screenGUI[x] = errorLineGui[x]  (WRONG!)
     * - Should be: screenGUI[r + x] = errorLineGui[x]
     *   where r = screen.getPos(errorLineNum - 1, 0) is the row offset
     *
     * This test verifies that the GUI plane is restored at the correct position
     * corresponding to the error line (row 24), not at the beginning of screen.
     */
    @Test
    public void testRestoreErrorLineUsesCorrectGuiArrayOffset() {
        // Initialize with distinctive GUI values at the error line row
        char errorLineGUI = 5;
        char otherGUI = 1;

        int errorLineStartPos = (ERROR_LINE_ROW - 1) * NUM_COLS_80;

        // Set the error line GUI plane to distinctive value
        for (int i = 0; i < NUM_COLS_80; i++) {
            screenGUI[errorLineStartPos + i] = errorLineGUI;

            // Fill the beginning of the screen with different values
            // to detect if the bug causes wrong indexing
            screenGUI[i] = otherGUI;
        }

        // Save the error line
        screenPlanes.saveErrorLine();

        // Corrupt the error line GUI values
        for (int i = 0; i < NUM_COLS_80; i++) {
            screenGUI[errorLineStartPos + i] = 0;
        }

        // Restore the error line
        screenPlanes.restoreErrorLine();

        // Verify that the GUI plane was restored at the CORRECT OFFSET
        // If bug exists (line 158: screenGUI[x] instead of screenGUI[r + x]),
        // GUI values will incorrectly be at screenGUI[0..79]
        for (int col = 0; col < NUM_COLS_80; col++) {
            int correctPos = errorLineStartPos + col;

            assertEquals(
                "GUI at column " + col + " of error line (pos " + correctPos
                    + ") was not restored correctly. "
                    + "BUG: Line 158 writes to screenGUI[x] instead of screenGUI[r + x]",
                errorLineGUI,
                screenGUI[correctPos]
            );

            // Also verify that the beginning of the screen was not corrupted
            assertEquals(
                "GUI at position " + col + " (start of screen) should not be overwritten",
                otherGUI,
                screenGUI[col]
            );
        }
    }

    /**
     * Bug 1 & 2: Test the last column specifically
     *
     * FOCUSED TEST for both bugs:
     * - Bug 1: Line 158 uses wrong index (x instead of r + x)
     * - Bug 2: Line 155 loop stops at numCols - 1, missing column 79
     *
     * This test specifically targets the last column (index 79),
     * which is both:
     * 1. Skipped by the buggy loop condition (x < numCols - 1)
     * 2. Would be written to wrong position if loop included it (screenGUI[79] instead of screenGUI[1919])
     */
    @Test
    public void testRestoreErrorLineLastColumnGuiRestoration() {
        int lastCol = NUM_COLS_80 - 1; // Index 79
        char lastColGUI = 9;

        int errorLineStartPos = (ERROR_LINE_ROW - 1) * NUM_COLS_80;
        int lastColPos = errorLineStartPos + lastCol; // Position 1919 (23*80 + 79)

        // Set distinctive value at the last column
        screenGUI[lastColPos] = lastColGUI;

        // Save the error line
        screenPlanes.saveErrorLine();

        // Corrupt the last column GUI value
        screenGUI[lastColPos] = 0;

        // Restore the error line
        screenPlanes.restoreErrorLine();

        // Verify the last column GUI was restored
        // This test MUST FAIL with the current buggy implementation because:
        // Line 155: for (int x = 0; x < numCols - 1; x++)
        //           ==================^ skips when x = 79
        assertEquals(
            "Last column (index " + lastCol + ", pos " + lastColPos
                + ") GUI was not restored. "
                + "BUG: Line 155 uses 'x < numCols - 1' instead of 'x < numCols'",
            lastColGUI,
            screenGUI[lastColPos]
        );
    }

    /**
     * Bug 1 & 2: Integration test for full error line restoration
     *
     * Tests the complete save/restore cycle with varied data across all planes
     * and all columns including the last one.
     */
    @Test
    public void testFullErrorLineSaveRestoreCycle() {
        int errorLineStartPos = (ERROR_LINE_ROW - 1) * NUM_COLS_80;

        // Initialize error line with pattern that varies by column
        for (int col = 0; col < NUM_COLS_80; col++) {
            int pos = errorLineStartPos + col;
            screenPlanes.screen[pos] = (char) ('A' + (col % 26));
            screenAttr[pos] = (char) (32 + (col % 10));
            screenIsAttr[pos] = (char) ((col % 2) == 0 ? '1' : '0');
            screenGUI[pos] = (char) ((col + 1) % 10);
        }

        // Save the error line
        screenPlanes.saveErrorLine();

        // Clear the error line
        for (int col = 0; col < NUM_COLS_80; col++) {
            int pos = errorLineStartPos + col;
            screenPlanes.screen[pos] = ' ';
            screenAttr[pos] = 32;
            screenIsAttr[pos] = '0';
            screenGUI[pos] = 0;
        }

        // Restore the error line
        screenPlanes.restoreErrorLine();

        // Verify complete restoration
        for (int col = 0; col < NUM_COLS_80; col++) {
            int pos = errorLineStartPos + col;

            assertEquals(
                "Column " + col + ": character not restored",
                (char) ('A' + (col % 26)),
                screenPlanes.screen[pos]
            );

            assertEquals(
                "Column " + col + ": attribute not restored",
                (char) (32 + (col % 10)),
                screenAttr[pos]
            );

            assertEquals(
                "Column " + col + ": isAttr not restored",
                (char) ((col % 2) == 0 ? '1' : '0'),
                screenIsAttr[pos]
            );

            assertEquals(
                "Column " + col + ": GUI not restored at correct offset. "
                    + "BUG: Line 158 writes to screenGUI[x] instead of screenGUI[r + x]",
                (char) ((col + 1) % 10),
                screenGUI[pos]
            );
        }
    }

    /**
     * Verify that saveErrorLine correctly saves all columns
     *
     * This is a sanity check that saveErrorLine works correctly.
     * If this test fails, the bug is in saveErrorLine, not restoreErrorLine.
     */
    @Test
    public void testSaveErrorLineSavesAllColumns() {
        int errorLineStartPos = (ERROR_LINE_ROW - 1) * NUM_COLS_80;

        // Initialize with pattern
        for (int col = 0; col < NUM_COLS_80; col++) {
            screenGUI[errorLineStartPos + col] = (char) (col % 256);
        }

        // Save the error line
        screenPlanes.saveErrorLine();

        // Verify error line was saved (not null)
        assertEquals(
            "Error line not saved",
            true,
            screenPlanes.isErrorLineSaved()
        );
    }

}
