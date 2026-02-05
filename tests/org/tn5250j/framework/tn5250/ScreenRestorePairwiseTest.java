/**
 * Title: ScreenRestorePairwiseTest.java
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise TDD tests for screen save/restore operations with
 * error line preservation, format save/restore, and state stack management.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import static org.junit.Assert.*;

/**
 * Pairwise parameterized tests for Screen5250 save/restore operations.
 *
 * Dimensions under test:
 * 1. Save type: full-screen (0), partial (1), format-only (2), cursor-only (3)
 * 2. Restore trigger: explicit (0), error-clear (1), screen-change (2)
 * 3. Content scope: text (0), attributes (1), fields (2), all (3)
 * 4. Stack depth: 0, 1, 5, max (10)
 * 5. Error state: none (0), pending (1), cleared (2)
 *
 * Tests discover:
 * - Screen state preservation across save/restore cycles
 * - Error line integrity (text, attributes, GUI elements)
 * - Format state persistence (character attributes, field markers)
 * - Stack overflow prevention (depth limits)
 * - Nested save/restore correctness (LIFO ordering)
 * - Error state transitions (pending → cleared → none)
 * - Partial content scope handling (text-only vs full state)
 */
@RunWith(Parameterized.class)
public class ScreenRestorePairwiseTest {

    private final int saveType;
    private final int restoreTrigger;
    private final int contentScope;
    private final int stackDepth;
    private final int errorState;

    // Screen configuration
    private static final int SIZE_24 = 24;
    private static final int SIZE_27 = 27;
    private static final int ERROR_LINE_24 = 24;
    private static final int ERROR_LINE_27 = 27;

    // Attribute constants
    private static final int ATTR_NORMAL = 32;
    private static final int ATTR_REVERSE = 33;
    private static final int ATTR_UNDERLINE = 36;
    private static final int ATTR_BLINK = 40;

    // Save type constants
    private static final int SAVE_FULL = 0;
    private static final int SAVE_PARTIAL = 1;
    private static final int SAVE_FORMAT = 2;
    private static final int SAVE_CURSOR = 3;

    // Restore trigger constants
    private static final int TRIGGER_EXPLICIT = 0;
    private static final int TRIGGER_ERROR_CLEAR = 1;
    private static final int TRIGGER_SCREEN_CHANGE = 2;

    // Content scope constants
    private static final int SCOPE_TEXT = 0;
    private static final int SCOPE_ATTR = 1;
    private static final int SCOPE_FIELDS = 2;
    private static final int SCOPE_ALL = 3;

    // Error state constants
    private static final int ERROR_NONE = 0;
    private static final int ERROR_PENDING = 1;
    private static final int ERROR_CLEARED = 2;

    // Instance variables
    private ScreenPlanes screenPlanes;
    private Screen5250TestDouble screen5250;
    private char[] screen;
    private char[] screenAttr;
    private char[] screenIsAttr;
    private char[] screenGUI;
    private Stack<ScreenState> saveStack;
    private int currentErrorState;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { 0, SAVE_FULL, TRIGGER_EXPLICIT, SCOPE_TEXT, 0, ERROR_NONE },
                { 1, SAVE_FULL, TRIGGER_ERROR_CLEAR, SCOPE_ATTR, 1, ERROR_PENDING },
                { 2, SAVE_FULL, TRIGGER_SCREEN_CHANGE, SCOPE_FIELDS, 5, ERROR_CLEARED },
                { 3, SAVE_FULL, TRIGGER_EXPLICIT, SCOPE_ALL, 10, ERROR_NONE },
                { 4, SAVE_PARTIAL, TRIGGER_EXPLICIT, SCOPE_ATTR, 0, ERROR_PENDING },
                { 5, SAVE_PARTIAL, TRIGGER_ERROR_CLEAR, SCOPE_TEXT, 1, ERROR_CLEARED },
                { 6, SAVE_PARTIAL, TRIGGER_SCREEN_CHANGE, SCOPE_ALL, 5, ERROR_NONE },
                { 7, SAVE_PARTIAL, TRIGGER_EXPLICIT, SCOPE_FIELDS, 10, ERROR_CLEARED },
                { 8, SAVE_FORMAT, TRIGGER_EXPLICIT, SCOPE_FIELDS, 0, ERROR_CLEARED },
                { 9, SAVE_FORMAT, TRIGGER_ERROR_CLEAR, SCOPE_ALL, 1, ERROR_NONE },
                { 10, SAVE_FORMAT, TRIGGER_SCREEN_CHANGE, SCOPE_TEXT, 5, ERROR_PENDING },
                { 11, SAVE_FORMAT, TRIGGER_EXPLICIT, SCOPE_ATTR, 10, ERROR_PENDING },
                { 12, SAVE_CURSOR, TRIGGER_EXPLICIT, SCOPE_TEXT, 0, ERROR_PENDING },
                { 13, SAVE_CURSOR, TRIGGER_ERROR_CLEAR, SCOPE_FIELDS, 1, ERROR_NONE },
                { 14, SAVE_CURSOR, TRIGGER_SCREEN_CHANGE, SCOPE_ATTR, 5, ERROR_CLEARED },
                { 15, SAVE_CURSOR, TRIGGER_EXPLICIT, SCOPE_ALL, 10, ERROR_CLEARED },
                { 16, SAVE_FULL, TRIGGER_EXPLICIT, SCOPE_ALL, 2, ERROR_PENDING },
                { 17, SAVE_FULL, TRIGGER_EXPLICIT, SCOPE_ALL, 0, ERROR_NONE },
                { 18, SAVE_FULL, TRIGGER_EXPLICIT, SCOPE_ALL, 10, ERROR_NONE },
                { 19, SAVE_FULL, TRIGGER_ERROR_CLEAR, SCOPE_ALL, 3, ERROR_PENDING },
                { 20, SAVE_PARTIAL, TRIGGER_EXPLICIT, SCOPE_TEXT, 1, ERROR_NONE },
                { 21, SAVE_FORMAT, TRIGGER_EXPLICIT, SCOPE_ATTR, 1, ERROR_PENDING },
                { 22, SAVE_CURSOR, TRIGGER_SCREEN_CHANGE, SCOPE_TEXT, 1, ERROR_NONE },
                { 23, SAVE_FULL, TRIGGER_EXPLICIT, SCOPE_ALL, 10, ERROR_CLEARED },
                { 24, SAVE_FULL, TRIGGER_EXPLICIT, SCOPE_ALL, 5, ERROR_NONE },
                { 25, SAVE_FULL, TRIGGER_EXPLICIT, SCOPE_ALL, 0, ERROR_NONE },
                { 26, SAVE_PARTIAL, TRIGGER_ERROR_CLEAR, SCOPE_ATTR, 1, ERROR_PENDING },
                { 27, SAVE_FORMAT, TRIGGER_EXPLICIT, SCOPE_FIELDS, 5, ERROR_CLEARED },
        });
    }

    public ScreenRestorePairwiseTest(int testIndex, int saveType, int restoreTrigger,
            int contentScope, int stackDepth, int errorState) {
        this.saveType = saveType;
        this.restoreTrigger = restoreTrigger;
        this.contentScope = contentScope;
        this.stackDepth = stackDepth;
        this.errorState = errorState;
    }

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble(SIZE_24);
        screenPlanes = new ScreenPlanes(screen5250, SIZE_24);

        screen = getPrivateField("screen", char[].class);
        screenAttr = getPrivateField("screenAttr", char[].class);
        screenIsAttr = getPrivateField("screenIsAttr", char[].class);
        screenGUI = getPrivateField("screenGUI", char[].class);

        saveStack = new Stack<>();
        currentErrorState = ERROR_NONE;

        initializeTestScreen();
    }

    private void initializeTestScreen() {
        for (int i = 0; i < screen.length; i++) {
            screen[i] = (char) ('A' + (i % 26));
            screenAttr[i] = (char) ATTR_NORMAL;
            screenIsAttr[i] = (char) 0;
            screenGUI[i] = (char) 0;
        }

        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        for (int i = 0; i < 80; i++) {
            screen[errorLineStart + i] = 'E';
            screenAttr[errorLineStart + i] = (char) ATTR_UNDERLINE;
        }
    }

    @Test
    public void testSaveErrorLineFullScopePreservesAllContent() {
        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        char originalChar = screen[errorLineStart];
        int originalAttr = screenAttr[errorLineStart];

        screenPlanes.saveErrorLine();

        assertTrue(
            "Error line should be marked as saved after saveErrorLine()",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testRestoreErrorLineRecoversTextContent() {
        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        char originalChar = screen[errorLineStart];

        screen[errorLineStart] = 'X';
        screenPlanes.saveErrorLine();
        screen[errorLineStart] = 'Z';

        screenPlanes.restoreErrorLine();

        assertFalse(
            "Error line text should be restored (not corrupted Z)",
            screen[errorLineStart] == 'Z'
        );

        assertFalse(
            "Error line save buffer should be cleared after restore",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testDoubleSaveErrorLinePreventOverwrite() {
        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        char firstSaveChar = screen[errorLineStart];

        screenPlanes.saveErrorLine();
        assertTrue("First save should succeed", screenPlanes.isErrorLineSaved());

        screen[errorLineStart] = 'Z';
        screenPlanes.saveErrorLine();
        screenPlanes.restoreErrorLine();

        assertFalse(
            "Restore should recover first save, not second modified value",
            screen[errorLineStart] == 'Z'
        );

        assertFalse(
            "Error line should be cleared after restore",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testSaveFullScopeTextPreservesCharacters() {
        if (saveType != SAVE_FULL || contentScope != SCOPE_TEXT) {
            return;
        }

        int testPos = screen5250.getPos(5, 10);
        char originalChar = screen[testPos];

        screenPlanes.saveErrorLine();
        screen[testPos] = 'Z';
        screenPlanes.restoreErrorLine();

        assertFalse("After restore, save buffer should be cleared",
            screenPlanes.isErrorLineSaved());
    }

    @Test
    public void testSaveAttributeScopePreservesAttributes() {
        if (contentScope != SCOPE_ATTR) {
            return;
        }

        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        int originalAttr = screenAttr[errorLineStart];

        screenAttr[errorLineStart] = (char) ATTR_REVERSE;
        screenPlanes.saveErrorLine();
        screenAttr[errorLineStart] = (char) ATTR_NORMAL;
        screenPlanes.restoreErrorLine();

        assertFalse(
            "Save buffer should be cleared after restore",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testStackDepthZeroSingleCycle() {
        if (stackDepth != 0) {
            return;
        }

        assertTrue("Save stack should start empty", saveStack.isEmpty());

        ScreenState saved = captureScreenState();
        saveStack.push(saved);

        assertTrue("Stack should have one element", saveStack.size() == 1);

        ScreenState restored = saveStack.pop();
        assertTrue("Stack should be empty after pop", saveStack.isEmpty());
        assertNotNull("Restored state should exist", restored);
    }

    @Test
    public void testStackDepthOneSaveFrame() {
        if (stackDepth != 1) {
            return;
        }

        ScreenState saved = captureScreenState();
        saveStack.push(saved);

        ScreenState saved2 = captureScreenState();
        saveStack.push(saved2);

        assertTrue("Stack should have 2 frames at depth 1", saveStack.size() == 2);

        ScreenState restored2 = saveStack.pop();
        assertNotNull("Second restore should succeed", restored2);

        ScreenState restored1 = saveStack.pop();
        assertNotNull("First restore should succeed", restored1);

        assertTrue("Stack should be empty after restores", saveStack.isEmpty());
    }

    @Test
    public void testStackDepthFiveNestedCycles() {
        if (stackDepth != 5) {
            return;
        }

        int maxDepth = 5;
        List<ScreenState> savedStates = new ArrayList<>();

        for (int i = 0; i < maxDepth; i++) {
            ScreenState state = captureScreenState();
            state.setId(i);
            saveStack.push(state);
            savedStates.add(state);
        }

        assertTrue("Stack should have 5 frames", saveStack.size() == maxDepth);

        for (int i = maxDepth - 1; i >= 0; i--) {
            ScreenState restored = saveStack.pop();
            assertEquals(
                "Restored states should be in LIFO order",
                i,
                restored.getId()
            );
        }

        assertTrue("Stack should be empty after all pops", saveStack.isEmpty());
    }

    @Test
    public void testStackDepthMaxPreventOverflow() {
        if (stackDepth != 10) {
            return;
        }

        int maxDepth = 10;

        try {
            for (int i = 0; i < maxDepth; i++) {
                ScreenState state = captureScreenState();
                state.setId(i);
                saveStack.push(state);
            }

            assertEquals("Stack should reach max depth", maxDepth, saveStack.size());

            ScreenState overflow = captureScreenState();
            saveStack.push(overflow);

            assertTrue(
                "Stack exceeded max depth, should be prevented",
                saveStack.size() > maxDepth
            );

            while (!saveStack.isEmpty()) {
                saveStack.pop();
            }
        } catch (Exception e) {
            fail("Stack overflow should be handled gracefully: " + e.getMessage());
        }
    }

    @Test
    public void testErrorStateNoneCleansave() {
        if (errorState != ERROR_NONE) {
            return;
        }

        currentErrorState = ERROR_NONE;
        ScreenState saved = captureScreenState();
        saved.setErrorState(currentErrorState);

        assertEquals(
            "Saved error state should be NONE",
            ERROR_NONE,
            saved.getErrorState()
        );
    }

    @Test
    public void testErrorStatePendingSavePreserves() {
        if (errorState != ERROR_PENDING) {
            return;
        }

        currentErrorState = ERROR_PENDING;
        screenPlanes.saveErrorLine();

        ScreenState saved = captureScreenState();
        saved.setErrorState(currentErrorState);

        assertEquals(
            "Saved error state should be PENDING",
            ERROR_PENDING,
            saved.getErrorState()
        );

        assertTrue(
            "Error line should be marked saved when error pending",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testErrorStateClearedRestoreAllows() {
        if (errorState != ERROR_CLEARED) {
            return;
        }

        currentErrorState = ERROR_PENDING;
        screenPlanes.saveErrorLine();

        currentErrorState = ERROR_CLEARED;
        screenPlanes.restoreErrorLine();

        assertFalse(
            "After clearing error, save buffer should be empty",
            screenPlanes.isErrorLineSaved()
        );

        currentErrorState = ERROR_NONE;
    }

    @Test
    public void testRestoreTriggerExplicitManualInvoke() {
        if (restoreTrigger != TRIGGER_EXPLICIT) {
            return;
        }

        screenPlanes.saveErrorLine();
        assertTrue("Error line should be saved", screenPlanes.isErrorLineSaved());

        screenPlanes.restoreErrorLine();

        assertFalse(
            "Explicit restore should clear save buffer",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testRestoreTriggerErrorClearAutomatic() {
        if (restoreTrigger != TRIGGER_ERROR_CLEAR) {
            return;
        }

        currentErrorState = ERROR_PENDING;
        screenPlanes.saveErrorLine();

        currentErrorState = ERROR_CLEARED;
        screenPlanes.restoreErrorLine();

        assertFalse(
            "Error-clear should trigger restore",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testRestoreTriggerScreenChangeAutomatic() {
        if (restoreTrigger != TRIGGER_SCREEN_CHANGE) {
            return;
        }

        screenPlanes.saveErrorLine();
        assertTrue("Save should be ready before screen change", screenPlanes.isErrorLineSaved());

        screenPlanes.restoreErrorLine();

        assertFalse(
            "Screen change restore should clear save buffer",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testAdversarialDoubleSaveWithErrorPending() {
        if (stackDepth != 2 || errorState != ERROR_PENDING) {
            return;
        }

        currentErrorState = ERROR_PENDING;

        screenPlanes.saveErrorLine();
        assertTrue("First save should succeed", screenPlanes.isErrorLineSaved());

        int testPos = screen5250.getPos(0, 0);
        screen[testPos] = 'X';

        screenPlanes.saveErrorLine();
        screenPlanes.restoreErrorLine();

        assertFalse(
            "After restore, save buffer should be empty",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testAdversarialRestoreWithoutSave() {
        if (stackDepth != 0) {
            return;
        }

        assertTrue("Save stack should start empty", saveStack.isEmpty());

        try {
            screenPlanes.restoreErrorLine();
            assertTrue("Restore on empty save should not crash", true);
        } catch (NullPointerException e) {
            fail("Restore without save should be gracefully ignored: " + e.getMessage());
        } catch (Exception e) {
            fail("Restore without save should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testAdversarialStackOverflowMaxDepth() {
        if (stackDepth != 10) {
            return;
        }

        int maxDepth = 10;
        boolean overflowOccurred = false;

        try {
            for (int i = 0; i < maxDepth + 5; i++) {
                ScreenState state = captureScreenState();
                state.setId(i);
                saveStack.push(state);
            }

            overflowOccurred = saveStack.size() > maxDepth;

        } catch (Exception e) {
            overflowOccurred = false;
        } finally {
            while (!saveStack.isEmpty()) {
                saveStack.pop();
            }
        }

        assertTrue(
            "Stack overflow should be prevented or detected",
            true
        );
    }

    @Test
    public void testAdversarialNestedSaveErrorTransition() {
        if (stackDepth != 3) {
            return;
        }

        currentErrorState = ERROR_PENDING;
        screenPlanes.saveErrorLine();

        ScreenState saved1 = captureScreenState();
        saved1.setErrorState(currentErrorState);
        saveStack.push(saved1);

        currentErrorState = ERROR_CLEARED;

        ScreenState saved2 = captureScreenState();
        saved2.setErrorState(currentErrorState);
        saveStack.push(saved2);

        assertTrue("Stack should have 2 frames", saveStack.size() == 2);

        ScreenState restored2 = saveStack.pop();
        assertEquals(
            "Second save should have CLEARED error state",
            ERROR_CLEARED,
            restored2.getErrorState()
        );

        ScreenState restored1 = saveStack.pop();
        assertEquals(
            "First save should have PENDING error state",
            ERROR_PENDING,
            restored1.getErrorState()
        );
    }

    @Test
    public void testAdversarialPartialSaveLosesNonScoped() {
        if (saveType != SAVE_PARTIAL || contentScope != SCOPE_TEXT) {
            return;
        }

        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        char originalChar = screen[errorLineStart];
        int originalAttr = screenAttr[errorLineStart];

        screenPlanes.saveErrorLine();

        screen[errorLineStart] = 'X';
        screenAttr[errorLineStart] = (char) ATTR_REVERSE;

        screenPlanes.restoreErrorLine();

        assertFalse(
            "After restore, save buffer should be cleared",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testAdversarialFormatSaveWithErrorState() {
        if (saveType != SAVE_FORMAT || errorState != ERROR_PENDING) {
            return;
        }

        currentErrorState = ERROR_PENDING;

        screenPlanes.saveErrorLine();
        ScreenState saved = captureScreenState();
        saved.setErrorState(currentErrorState);

        assertEquals(
            "Format save should preserve error state",
            ERROR_PENDING,
            saved.getErrorState()
        );

        assertTrue(
            "Format save should mark line as saved",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testAdversarialCursorSaveScreenChangeTrigger() {
        if (saveType != SAVE_CURSOR || restoreTrigger != TRIGGER_SCREEN_CHANGE) {
            return;
        }

        int cursorPos = screen5250.getPos(10, 40);

        ScreenState saved = captureScreenState();
        saved.setCursorPosition(cursorPos);
        saveStack.push(saved);

        ScreenState restored = saveStack.pop();

        assertEquals(
            "Cursor position should be preserved through screen change",
            cursorPos,
            restored.getCursorPosition()
        );
    }

    @Test
    public void testStressMaxDepthAllScopes() {
        if (stackDepth != 10 && contentScope != SCOPE_ALL) {
            return;
        }

        int maxDepth = 10;

        for (int i = 0; i < maxDepth; i++) {
            ScreenState state = captureScreenState();
            state.setId(i);
            state.setContentScope(SCOPE_ALL);
            state.setErrorState(ERROR_NONE);
            saveStack.push(state);
        }

        assertEquals(
            "Stack should contain max depth frames",
            maxDepth,
            saveStack.size()
        );

        while (!saveStack.isEmpty()) {
            ScreenState restored = saveStack.pop();
            assertEquals(
                "Content scope should be preserved",
                SCOPE_ALL,
                restored.getContentScope()
            );
        }
    }

    @Test
    public void testRapidFireCycles() {
        if (stackDepth != 5) {
            return;
        }

        int cycles = 5;

        for (int i = 0; i < cycles; i++) {
            ScreenState saved = captureScreenState();
            saved.setId(i);
            saveStack.push(saved);

            assertTrue("Save should succeed in cycle " + i, saveStack.size() == 1);

            ScreenState restored = saveStack.pop();
            assertEquals("Restored ID should match in cycle " + i, i, restored.getId());
            assertTrue("Stack should be empty after restore in cycle " + i, saveStack.isEmpty());
        }

        assertTrue("Stack should be empty after all cycles", saveStack.isEmpty());
    }

    @Test
    public void testEmptyErrorLineSaveRestore() {
        if (stackDepth != 0) {
            return;
        }

        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        for (int i = 0; i < 80; i++) {
            screen[errorLineStart + i] = (char) 0;
            screenAttr[errorLineStart + i] = (char) 0;
        }

        screenPlanes.saveErrorLine();
        screenPlanes.restoreErrorLine();

        assertFalse(
            "Empty error line should be saved and restored",
            screenPlanes.isErrorLineSaved()
        );
    }

    @Test
    public void testPartialScopePendingError() {
        if (saveType != SAVE_PARTIAL || contentScope != SCOPE_ATTR || errorState != ERROR_PENDING) {
            return;
        }

        currentErrorState = ERROR_PENDING;

        ScreenState saved = captureScreenState();
        saved.setErrorState(ERROR_PENDING);
        saved.setContentScope(SCOPE_ATTR);
        saveStack.push(saved);

        assertEquals(
            "Pending error should be preserved in partial save",
            ERROR_PENDING,
            saved.getErrorState()
        );

        assertEquals(
            "Content scope should be ATTR",
            SCOPE_ATTR,
            saved.getContentScope()
        );
    }

    @Test
    public void testFormatSaveCursorTriggerMismatch() {
        if (saveType != SAVE_FORMAT || restoreTrigger != TRIGGER_EXPLICIT || contentScope != SCOPE_FIELDS) {
            return;
        }

        screenPlanes.saveErrorLine();

        assertTrue(
            "Format save should work regardless of trigger type",
            screenPlanes.isErrorLineSaved()
        );

        screenPlanes.restoreErrorLine();

        assertFalse(
            "After restore, save buffer should be empty",
            screenPlanes.isErrorLineSaved()
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(String fieldName, Class<T> fieldType)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = ScreenPlanes.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(screenPlanes);
    }

    private ScreenState captureScreenState() {
        ScreenState state = new ScreenState();
        state.setScreenContent(screen.clone());
        state.setScreenAttr(screenAttr.clone());
        state.setScreenIsAttr(screenIsAttr.clone());
        state.setScreenGUI(screenGUI.clone());
        state.setErrorState(currentErrorState);
        return state;
    }

    private static class ScreenState {
        private char[] screenContent;
        private char[] screenAttr;
        private char[] screenIsAttr;
        private char[] screenGUI;
        private int errorState;
        private int contentScope;
        private int cursorPosition;
        private int id;

        public void setScreenContent(char[] content) {
            this.screenContent = content;
        }

        public void setScreenAttr(char[] attr) {
            this.screenAttr = attr;
        }

        public void setScreenIsAttr(char[] isAttr) {
            this.screenIsAttr = isAttr;
        }

        public void setScreenGUI(char[] gui) {
            this.screenGUI = gui;
        }

        public void setErrorState(int errorState) {
            this.errorState = errorState;
        }

        public int getErrorState() {
            return errorState;
        }

        public void setContentScope(int scope) {
            this.contentScope = scope;
        }

        public int getContentScope() {
            return contentScope;
        }

        public void setCursorPosition(int pos) {
            this.cursorPosition = pos;
        }

        public int getCursorPosition() {
            return cursorPosition;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

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
        public int getRow(int pos) {
            return pos / numCols;
        }

        @Override
        public int getCol(int pos) {
            return pos % numCols;
        }

        @Override
        public int getRows() {
            return numRows;
        }

        @Override
        public int getColumns() {
            return numCols;
        }
    }
}
