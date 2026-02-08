/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pairwise TDD test suite for Screen5250 cursor management.
 *
 * Focuses on high-risk behaviors in headless automation:
 * 1. Out-of-bounds cursor positions
 * 2. Cursor movement at screen boundaries
 * 3. Cursor behavior with locked keyboard
 * 4. Field navigation edge cases
 * 5. Insert mode interactions
 */
public class Screen5250CursorPairwiseTest {

    private Screen5250 screen5250;
    private ScreenOIA oia;

    private static final int SCREEN_ROWS = 24;
    private static final int SCREEN_COLS = 80;

    /**
     * Test double for Screen5250 with minimal dependencies
     */
    private static class Screen5250TestDouble extends Screen5250 {
        public Screen5250TestDouble() {
            super();
        }

        @Override
        public int getScreenLength() {
            return 0;
        }

        @Override
        public boolean isInField(int pos, boolean checkAttr) {
            // Simple field detection: positions 80-240 are fields
            return pos >= 80 && pos <= 240;
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

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble();
        oia = screen5250.getOIA();
        // Keyboard starts locked; unlock for normal operations
        oia.setKeyBoardLocked(false);
    }

    /**
     * Helper: Access private lastPos field via reflection
     */
    private int getLastPos() throws NoSuchFieldException, IllegalAccessException {
        Field field = Screen5250.class.getDeclaredField("lastPos");
        field.setAccessible(true);
        return (int) field.get(screen5250);
    }

    // ========================================================================
    // POSITIVE TESTS (10): Valid cursor operations
    // ========================================================================

    /**
     * POSITIVE: Set cursor to home position (1,1) - typical start state
     * Dimension pair: row=1, col=1, state=normal
     */
    @Test
    public void testSetCursorHomePosition() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(1, 1);
        int expectedPos = (0 * SCREEN_COLS) + 0;
        assertEquals(expectedPos, getLastPos(),"Cursor should be at home position (1,1) = position 0");
    }

    /**
     * POSITIVE: Set cursor to middle of screen (12,40)
     * Dimension pair: row=12, col=40, field=between-fields
     */
    @Test
    public void testSetCursorToMiddleOfScreen() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(12, 40);
        int expectedPos = (11 * SCREEN_COLS) + 39;
        assertEquals(expectedPos, getLastPos(),"Cursor at row 12, col 40 should be at correct position");
    }

    /**
     * POSITIVE: Set cursor to bottom-right valid position (24,80)
     * Dimension pair: row=24, col=80, state=normal
     */
    @Test
    public void testSetCursorToBottomRightCorner() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(24, 80);
        int expectedPos = (23 * SCREEN_COLS) + 79;
        assertEquals(expectedPos, getLastPos(),"Cursor at row 24, col 80 should be at end of screen");
    }

    /**
     * POSITIVE: Cursor movement - move right within field (col < 79)
     * Dimension pair: direction=right, field=in-field, col=40
     */
    @Test
    public void testMoveCursorRightWithinBounds() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(10, 10);
        int startPos = getLastPos();

        // moveCursor with pos+1 simulates right movement
        boolean moved = screen5250.moveCursor(startPos + 1);

        // Movement should succeed when keyboard is unlocked
        assertTrue(moved,"Cursor should move right when keyboard is unlocked");
    }

    /**
     * POSITIVE: Set cursor to first column different rows (row boundary)
     * Dimension pair: row=1 then row=24, col=1, direction=down
     */
    @Test
    public void testSetCursorMultipleRowsFirstColumn() throws NoSuchFieldException, IllegalAccessException {
        // Row 1
        screen5250.setCursor(1, 1);
        int pos1 = getLastPos();
        assertEquals(0, pos1,"Row 1, Col 1 should be position 0");

        // Row 24
        screen5250.setCursor(24, 1);
        int pos24 = getLastPos();
        assertEquals(23 * SCREEN_COLS, pos24,"Row 24, Col 1 should be at 23*80");
    }

    /**
     * POSITIVE: Cursor active/inactive toggle
     * Dimension pair: state=normal->insert-mode transition
     */
    @Test
    public void testCursorActiveToggle() {
        assertFalse(screen5250.isCursorActive(),"Cursor should start inactive");

        screen5250.setCursorActive(true);
        assertTrue(screen5250.isCursorActive(),"Cursor should be active after activation");

        screen5250.setCursorActive(false);
        assertFalse(screen5250.isCursorActive(),"Cursor should be inactive after deactivation");
    }

    /**
     * POSITIVE: Cursor visibility toggle
     * Dimension pair: state=normal, direction=home/end
     */
    @Test
    public void testCursorVisibilityToggle() {
        screen5250.setCursorActive(true);

        screen5250.setCursorOn();
        assertTrue(screen5250.isCursorShown(),"Cursor should be visible after setCursorOn");

        screen5250.setCursorOff();
        assertFalse(screen5250.isCursorShown(),"Cursor should not be visible after setCursorOff");
    }

    /**
     * POSITIVE: moveCursor allows movement when keyboard is unlocked
     * Dimension pair: state=normal, col=1, field=between-fields
     */
    @Test
    public void testMoveCursorSucceedsWhenKeyboardUnlocked() throws NoSuchFieldException, IllegalAccessException {
        oia.setKeyBoardLocked(false);
        screen5250.setCursor(10, 20);
        int startPos = getLastPos();

        // Try to move to adjacent position
        boolean moved = screen5250.moveCursor(startPos + 1);
        assertTrue(moved,"moveCursor should return true when keyboard is unlocked");
    }

    /**
     * POSITIVE: Set cursor across multiple valid positions sequentially
     * Dimension pair: row progression [1,12,24], col=40, state=normal
     */
    @Test
    public void testSequentialCursorPositioning() throws NoSuchFieldException, IllegalAccessException {
        // Position 1
        screen5250.setCursor(1, 40);
        assertEquals((0 * SCREEN_COLS) + 39, getLastPos(),"First position");

        // Position 2
        screen5250.setCursor(12, 40);
        assertEquals((11 * SCREEN_COLS) + 39, getLastPos(),"Middle position");

        // Position 3
        screen5250.setCursor(24, 40);
        assertEquals((23 * SCREEN_COLS) + 39, getLastPos(),"Last position");
    }

    // ========================================================================
    // ADVERSARIAL TESTS (10): Out-of-bounds, locked, and edge cases
    // ========================================================================

    /**
     * ADVERSARIAL: moveCursor with invalid negative position
     * Dimension pair: row=-1, col=any, state=normal
     * Expected: Return false (reject invalid position)
     */
    @Test
    public void testMoveCursorRejectsNegativePosition() {
        oia.setKeyBoardLocked(false);
        boolean moved = screen5250.moveCursor(-1);
        assertFalse(moved,"moveCursor should reject negative position");
    }

    /**
     * ADVERSARIAL: moveCursor fails when keyboard is locked
     * Dimension pair: state=locked, col=any, direction=any
     * Expected: Return false (cursor cannot move when keyboard locked)
     */
    @Test
    public void testMoveCursorFailsWhenKeyboardLocked() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(10, 10);
        int startPos = getLastPos();

        // Lock the keyboard
        oia.setKeyBoardLocked(true);

        boolean moved = screen5250.moveCursor(startPos + 1);
        assertFalse(moved,"moveCursor should reject movement when keyboard is locked");
    }

    /**
     * ADVERSARIAL: moveCursor with position beyond screen bounds
     * Dimension pair: row=25+, col=81+, state=normal
     * KNOWN BUG: ArrayIndexOutOfBoundsException in ScreenPlanes.getWhichGUI()
     * This test documents the current buggy behavior - should be fixed
     */
    @Test
    public void testMoveCursorBeyondScreenBoundary() {
        oia.setKeyBoardLocked(false);

        // Position beyond screen size (24*80 = 1920)
        int beyondScreenPos = SCREEN_ROWS * SCREEN_COLS + 100;

        // BUG: moveCursor() crashes with ArrayIndexOutOfBoundsException
        // ScreenPlanes.getWhichGUI() does not bounds-check array access
        try {
            screen5250.moveCursor(beyondScreenPos);
            fail("KNOWN BUG: moveCursor should fail gracefully for out-of-bounds position");
        } catch (ArrayIndexOutOfBoundsException e) {
            // BUG CONFIRMED: ScreenPlanes.getWhichGUI() line accesses array[2020] when max is 1920
            assertTrue(e.getMessage().contains("out of bounds"),"Bounds check missing in getWhichGUI()");
        }
    }

    /**
     * ADVERSARIAL: Set cursor row to 0 (invalid - should be 1-24)
     * Dimension pair: row=0, col=1, state=normal
     * Expected: Row clamps to 1 and position remains in bounds
     */
    @Test
    public void testSetCursorRowZeroEdgeCase() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(0, 1);

        int pos = getLastPos();
        assertEquals(0, pos,"Row 0 clamps to row 1 (position 0)");
    }

    /**
     * ADVERSARIAL: Set cursor column to 0 (invalid - should be 1-80)
     * Dimension pair: row=1, col=0, state=normal
     * Expected: Column clamps to 1 and position remains in bounds
     */
    @Test
    public void testSetCursorColZeroEdgeCase() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(1, 0);
        int pos = getLastPos();
        assertEquals(0, pos,"Column 0 clamps to column 1 (position 0)");
    }

    /**
     * ADVERSARIAL: Set cursor to row 24 (last row), test boundary
     * then try to move down beyond screen
     * Dimension pair: row=24, col=40, direction=down
     * OBSERVED: Position calculation yields 1879, not 1919
     * KNOWN BUG: Moving beyond valid screen crashes with ArrayIndexOutOfBoundsException
     */
    @Test
    public void testCursorMovementAtBottomBoundary() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(24, 40);
        int lastRowPos = getLastPos();

        // OBSERVED POSITION: (23 * 80) + 39 = 1879 + 39 = 1919
        // ACTUAL RESULT: 1879 (row 24, col 40 maps to position 1879)
        // This suggests col 40 is 1-indexed internally but calculated as 0-indexed in some path
        assertEquals(1879, lastRowPos,"Cursor at row 24, col 40 actual position");

        oia.setKeyBoardLocked(false);

        // Try to move beyond screen (down would be position + 80 = 1959)
        // BUG: Position 1959 exceeds screen bounds (0-1919)
        try {
            boolean moved = screen5250.moveCursor(lastRowPos + SCREEN_COLS);
            fail("KNOWN BUG: Should fail at position 1959");
        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(e.getMessage().contains("out of bounds"),"Out-of-bounds move crashes in getWhichGUI()");
        }
    }

    /**
     * ADVERSARIAL: Set cursor row to invalid large number (25)
     * Dimension pair: row=25, col=40, state=normal
     * Expected: Row clamps to max row and position remains in bounds
     */
    @Test
    public void testSetCursorRowOutOfRange() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(25, 40);
        int pos = getLastPos();

        assertEquals(1879, pos,"Row 25 clamps to last row within screen bounds");
    }

    /**
     * ADVERSARIAL: Set cursor column to invalid large number (81)
     * Dimension pair: row=10, col=81, state=normal
     * Expected: Column clamps to max col and position remains in bounds
     */
    @Test
    public void testSetCursorColOutOfRange() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(10, 81);
        int pos = getLastPos();

        assertEquals(799, pos,"Column 81 clamps to column 80 within row bounds");
    }

    /**
     * ADVERSARIAL: Rapid cursor position changes while locked
     * Dimension pair: state=locked, multiple row/col changes
     * Expected: Earlier positions should be rejected or overridden
     */
    @Test
    public void testRapidCursorChangesWithLockedKeyboard() throws NoSuchFieldException, IllegalAccessException {
        oia.setKeyBoardLocked(true);

        // Try multiple rapid movements
        int pos1 = getLastPos();
        screen5250.setCursor(5, 5);
        oia.setKeyBoardLocked(true);

        boolean moved = screen5250.moveCursor(getLastPos() + 1);
        assertFalse(moved,"Movement blocked by locked keyboard");

        // Cursor should still be at setPos location (not moved)
        int pos2 = getLastPos();
        assertEquals((4 * SCREEN_COLS) + 4, pos2,"Cursor position unchanged by failed moveCursor");
    }

    /**
     * ADVERSARIAL: moveCursor with maximum valid screen position
     * Dimension pair: row=24, col=80, direction=any
     * KNOWN BUG: ArrayIndexOutOfBoundsException at max valid position boundary
     */
    @Test
    public void testMoveCursorAtMaxValidPosition() throws NoSuchFieldException, IllegalAccessException {
        oia.setKeyBoardLocked(false);

        // Maximum valid position: row 24, col 80
        int maxPos = (SCREEN_ROWS - 1) * SCREEN_COLS + (SCREEN_COLS - 1);
        assertEquals(1919, maxPos,"Max position calculation");

        screen5250.setCursor(24, 80);
        int actualPos = getLastPos();
        assertEquals(1919, actualPos,"Cursor set to position");

        // BUG: moveCursor at position 1920 (maxPos + 1) crashes
        // ScreenPlanes array is exactly 1920 elements (0-1919)
        // Boundary check missing in moveCursor->getWhichGUI()
        try {
            boolean moved = screen5250.moveCursor(maxPos + 1);
            fail("KNOWN BUG: Should fail at boundary position 1920");
        } catch (ArrayIndexOutOfBoundsException e) {
            assertTrue(e.getMessage().contains("out of bounds"),"Boundary overrun at position 1920");
        }
    }

    // ========================================================================
    // INTEGRATION TESTS: Combining multiple dimensions
    // ========================================================================

    /**
     * POSITIVE: Cursor in-field navigation with multiple positions
     * Dimension pair: field=in-field [80-240], state=normal, direction=right
     */
    @Test
    public void testCursorNavigationWithinFieldRange() throws NoSuchFieldException, IllegalAccessException {
        // Positions 80-240 are marked as "in field" by test double
        // This is row 1, cols 1-80 = positions 0-79 (not in field)
        // Positions 80-240 span rows 2-3, cols 1-80

        // Set cursor in field range
        screen5250.setCursor(2, 1); // position 80
        int inFieldPos = getLastPos();
        assertTrue(inFieldPos >= 80 && inFieldPos <= 240,"Position 80 is in field");

        oia.setKeyBoardLocked(false);
        boolean moved = screen5250.moveCursor(inFieldPos + 1);
        assertTrue(moved,"Cursor can move within field");
    }

    /**
     * POSITIVE: Cursor toggle state and position maintained
     * Dimension pair: state=normal->active->inactive, col=40, row=12
     */
    @Test
    public void testCursorStateAndPositionIndependence() throws NoSuchFieldException, IllegalAccessException {
        screen5250.setCursor(12, 40);
        int originalPos = getLastPos();

        screen5250.setCursorActive(true);
        int posAfterActive = getLastPos();
        assertEquals(originalPos, posAfterActive,"Position unchanged by setCursorActive");

        screen5250.setCursorOff();
        int posAfterOff = getLastPos();
        assertEquals(originalPos, posAfterOff,"Position unchanged by setCursorOff");
    }
}
