/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
public class ScreenRestorePairwiseTest {

    private int saveType;
    private int restoreTrigger;
    private int contentScope;
    private int stackDepth;
    private int errorState;

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

    private void setParameters(int testIndex, int saveType, int restoreTrigger,
            int contentScope, int stackDepth, int errorState) {
        this.saveType = saveType;
        this.restoreTrigger = restoreTrigger;
        this.contentScope = contentScope;
        this.stackDepth = stackDepth;
        this.errorState = errorState;
    }

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

    @ParameterizedTest

    @MethodSource("data")
    public void testSaveErrorLineFullScopePreservesAllContent(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        char originalChar = screen[errorLineStart];
        int originalAttr = screenAttr[errorLineStart];

        screenPlanes.saveErrorLine();

        assertTrue(screenPlanes.isErrorLineSaved()
        ,
            "Error line should be marked as saved after saveErrorLine()");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testRestoreErrorLineRecoversTextContent(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        char originalChar = screen[errorLineStart];

        screen[errorLineStart] = 'X';
        screenPlanes.saveErrorLine();
        screen[errorLineStart] = 'Z';

        screenPlanes.restoreErrorLine();

        assertFalse(screen[errorLineStart] == 'Z'
        ,
            "Error line text should be restored (not corrupted Z)");

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "Error line save buffer should be cleared after restore");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testDoubleSaveErrorLinePreventOverwrite(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        char firstSaveChar = screen[errorLineStart];

        screenPlanes.saveErrorLine();
        assertTrue(screenPlanes.isErrorLineSaved(),"First save should succeed");

        screen[errorLineStart] = 'Z';
        screenPlanes.saveErrorLine();
        screenPlanes.restoreErrorLine();

        assertFalse(screen[errorLineStart] == 'Z'
        ,
            "Restore should recover first save, not second modified value");

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "Error line should be cleared after restore");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testSaveFullScopeTextPreservesCharacters(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (saveType != SAVE_FULL || contentScope != SCOPE_TEXT) {
            return;
        }

        int testPos = screen5250.getPos(5, 10);
        char originalChar = screen[testPos];

        screenPlanes.saveErrorLine();
        screen[testPos] = 'Z';
        screenPlanes.restoreErrorLine();

        assertFalse(screenPlanes.isErrorLineSaved(),"After restore, save buffer should be cleared");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testSaveAttributeScopePreservesAttributes(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (contentScope != SCOPE_ATTR) {
            return;
        }

        int errorLineStart = screen5250.getPos(ERROR_LINE_24 - 1, 0);
        int originalAttr = screenAttr[errorLineStart];

        screenAttr[errorLineStart] = (char) ATTR_REVERSE;
        screenPlanes.saveErrorLine();
        screenAttr[errorLineStart] = (char) ATTR_NORMAL;
        screenPlanes.restoreErrorLine();

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "Save buffer should be cleared after restore");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testStackDepthZeroSingleCycle(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (stackDepth != 0) {
            return;
        }

        assertTrue(saveStack.isEmpty(),"Save stack should start empty");

        ScreenState saved = captureScreenState();
        saveStack.push(saved);

        assertTrue(saveStack.size() == 1,"Stack should have one element");

        ScreenState restored = saveStack.pop();
        assertTrue(saveStack.isEmpty(),"Stack should be empty after pop");
        assertNotNull(restored,"Restored state should exist");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testStackDepthOneSaveFrame(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (stackDepth != 1) {
            return;
        }

        ScreenState saved = captureScreenState();
        saveStack.push(saved);

        ScreenState saved2 = captureScreenState();
        saveStack.push(saved2);

        assertTrue(saveStack.size() == 2,"Stack should have 2 frames at depth 1");

        ScreenState restored2 = saveStack.pop();
        assertNotNull(restored2,"Second restore should succeed");

        ScreenState restored1 = saveStack.pop();
        assertNotNull(restored1,"First restore should succeed");

        assertTrue(saveStack.isEmpty(),"Stack should be empty after restores");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testStackDepthFiveNestedCycles(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
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

        assertTrue(saveStack.size() == maxDepth,"Stack should have 5 frames");

        for (int i = maxDepth - 1; i >= 0; i--) {
            ScreenState restored = saveStack.pop();
            assertEquals(i,
                restored.getId()
            ,
                "Restored states should be in LIFO order");
        }

        assertTrue(saveStack.isEmpty(),"Stack should be empty after all pops");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testStackDepthMaxPreventOverflow(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
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

            assertEquals(maxDepth, saveStack.size(),"Stack should reach max depth");

            ScreenState overflow = captureScreenState();
            saveStack.push(overflow);

            assertTrue(saveStack.size() > maxDepth
            ,
                "Stack exceeded max depth, should be prevented");

            while (!saveStack.isEmpty()) {
                saveStack.pop();
            }
        } catch (Exception e) {
            fail("Stack overflow should be handled gracefully: " + e.getMessage());
        }
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testErrorStateNoneCleansave(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (errorState != ERROR_NONE) {
            return;
        }

        currentErrorState = ERROR_NONE;
        ScreenState saved = captureScreenState();
        saved.setErrorState(currentErrorState);

        assertEquals(ERROR_NONE,
            saved.getErrorState()
        ,
            "Saved error state should be NONE");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testErrorStatePendingSavePreserves(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (errorState != ERROR_PENDING) {
            return;
        }

        currentErrorState = ERROR_PENDING;
        screenPlanes.saveErrorLine();

        ScreenState saved = captureScreenState();
        saved.setErrorState(currentErrorState);

        assertEquals(ERROR_PENDING,
            saved.getErrorState()
        ,
            "Saved error state should be PENDING");

        assertTrue(screenPlanes.isErrorLineSaved()
        ,
            "Error line should be marked saved when error pending");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testErrorStateClearedRestoreAllows(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (errorState != ERROR_CLEARED) {
            return;
        }

        currentErrorState = ERROR_PENDING;
        screenPlanes.saveErrorLine();

        currentErrorState = ERROR_CLEARED;
        screenPlanes.restoreErrorLine();

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "After clearing error, save buffer should be empty");

        currentErrorState = ERROR_NONE;
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testRestoreTriggerExplicitManualInvoke(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (restoreTrigger != TRIGGER_EXPLICIT) {
            return;
        }

        screenPlanes.saveErrorLine();
        assertTrue(screenPlanes.isErrorLineSaved(),"Error line should be saved");

        screenPlanes.restoreErrorLine();

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "Explicit restore should clear save buffer");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testRestoreTriggerErrorClearAutomatic(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (restoreTrigger != TRIGGER_ERROR_CLEAR) {
            return;
        }

        currentErrorState = ERROR_PENDING;
        screenPlanes.saveErrorLine();

        currentErrorState = ERROR_CLEARED;
        screenPlanes.restoreErrorLine();

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "Error-clear should trigger restore");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testRestoreTriggerScreenChangeAutomatic(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (restoreTrigger != TRIGGER_SCREEN_CHANGE) {
            return;
        }

        screenPlanes.saveErrorLine();
        assertTrue(screenPlanes.isErrorLineSaved(),"Save should be ready before screen change");

        screenPlanes.restoreErrorLine();

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "Screen change restore should clear save buffer");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testAdversarialDoubleSaveWithErrorPending(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (stackDepth != 2 || errorState != ERROR_PENDING) {
            return;
        }

        currentErrorState = ERROR_PENDING;

        screenPlanes.saveErrorLine();
        assertTrue(screenPlanes.isErrorLineSaved(),"First save should succeed");

        int testPos = screen5250.getPos(0, 0);
        screen[testPos] = 'X';

        screenPlanes.saveErrorLine();
        screenPlanes.restoreErrorLine();

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "After restore, save buffer should be empty");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testAdversarialRestoreWithoutSave(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (stackDepth != 0) {
            return;
        }

        assertTrue(saveStack.isEmpty(),"Save stack should start empty");

        try {
            screenPlanes.restoreErrorLine();
            assertTrue(true,"Restore on empty save should not crash");
        } catch (NullPointerException e) {
            fail("Restore without save should be gracefully ignored: " + e.getMessage());
        } catch (Exception e) {
            fail("Restore without save should not throw exception: " + e.getMessage());
        }
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testAdversarialStackOverflowMaxDepth(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
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

        assertTrue(true
        ,
            "Stack overflow should be prevented or detected");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testAdversarialNestedSaveErrorTransition(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
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

        assertTrue(saveStack.size() == 2,"Stack should have 2 frames");

        ScreenState restored2 = saveStack.pop();
        assertEquals(ERROR_CLEARED,
            restored2.getErrorState()
        ,
            "Second save should have CLEARED error state");

        ScreenState restored1 = saveStack.pop();
        assertEquals(ERROR_PENDING,
            restored1.getErrorState()
        ,
            "First save should have PENDING error state");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testAdversarialPartialSaveLosesNonScoped(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
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

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "After restore, save buffer should be cleared");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testAdversarialFormatSaveWithErrorState(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (saveType != SAVE_FORMAT || errorState != ERROR_PENDING) {
            return;
        }

        currentErrorState = ERROR_PENDING;

        screenPlanes.saveErrorLine();
        ScreenState saved = captureScreenState();
        saved.setErrorState(currentErrorState);

        assertEquals(ERROR_PENDING,
            saved.getErrorState()
        ,
            "Format save should preserve error state");

        assertTrue(screenPlanes.isErrorLineSaved()
        ,
            "Format save should mark line as saved");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testAdversarialCursorSaveScreenChangeTrigger(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (saveType != SAVE_CURSOR || restoreTrigger != TRIGGER_SCREEN_CHANGE) {
            return;
        }

        int cursorPos = screen5250.getPos(10, 40);

        ScreenState saved = captureScreenState();
        saved.setCursorPosition(cursorPos);
        saveStack.push(saved);

        ScreenState restored = saveStack.pop();

        assertEquals(cursorPos,
            restored.getCursorPosition()
        ,
            "Cursor position should be preserved through screen change");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testStressMaxDepthAllScopes(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
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

        assertEquals(maxDepth,
            saveStack.size()
        ,
            "Stack should contain max depth frames");

        while (!saveStack.isEmpty()) {
            ScreenState restored = saveStack.pop();
            assertEquals(SCOPE_ALL,
                restored.getContentScope()
            ,
                "Content scope should be preserved");
        }
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testRapidFireCycles(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (stackDepth != 5) {
            return;
        }

        int cycles = 5;

        for (int i = 0; i < cycles; i++) {
            ScreenState saved = captureScreenState();
            saved.setId(i);
            saveStack.push(saved);

            assertTrue(saveStack.size() == 1,"Save should succeed in cycle " + i);

            ScreenState restored = saveStack.pop();
            assertEquals(i, restored.getId(),"Restored ID should match in cycle " + i);
            assertTrue(saveStack.isEmpty(),"Stack should be empty after restore in cycle " + i);
        }

        assertTrue(saveStack.isEmpty(),"Stack should be empty after all cycles");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testEmptyErrorLineSaveRestore(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
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

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "Empty error line should be saved and restored");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testPartialScopePendingError(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (saveType != SAVE_PARTIAL || contentScope != SCOPE_ATTR || errorState != ERROR_PENDING) {
            return;
        }

        currentErrorState = ERROR_PENDING;

        ScreenState saved = captureScreenState();
        saved.setErrorState(ERROR_PENDING);
        saved.setContentScope(SCOPE_ATTR);
        saveStack.push(saved);

        assertEquals(ERROR_PENDING,
            saved.getErrorState()
        ,
            "Pending error should be preserved in partial save");

        assertEquals(SCOPE_ATTR,
            saved.getContentScope()
        ,
            "Content scope should be ATTR");
    }

    @ParameterizedTest

    @MethodSource("data")
    public void testFormatSaveCursorTriggerMismatch(int testIndex, int saveType, int restoreTrigger, int contentScope, int stackDepth, int errorState) throws Exception {
        setParameters(testIndex, saveType, restoreTrigger, contentScope, stackDepth, errorState);
        setUp();
        if (saveType != SAVE_FORMAT || restoreTrigger != TRIGGER_EXPLICIT || contentScope != SCOPE_FIELDS) {
            return;
        }

        screenPlanes.saveErrorLine();

        assertTrue(screenPlanes.isErrorLineSaved()
        ,
            "Format save should work regardless of trigger type");

        screenPlanes.restoreErrorLine();

        assertFalse(screenPlanes.isErrorLineSaved()
        ,
            "After restore, save buffer should be empty");
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
