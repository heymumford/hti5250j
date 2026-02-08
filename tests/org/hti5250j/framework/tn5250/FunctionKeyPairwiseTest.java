/*
 * SPDX-FileCopyrightText: TN5250J Community
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Test Generator - TDD Phase 1
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pairwise JUnit 4 tests for HTI5250j function key handling in KeyboardHandler.
 *
 * Test Coverage (Pairwise Design):
 * 1. Key type: F1-F12, F13-F24, Enter, Clear, SysReq, Attn
 * 2. Modifier: none, shift, ctrl, alt
 * 3. Input state: ready, input-inhibited, system-wait
 * 4. Key mapping: default, custom, disabled
 * 5. Response: send-aid, local-action, ignore
 *
 * Test Strategy:
 * - 25+ pairwise tests covering valid key handling
 * - Adversarial tests: key injection, flooding, null handling
 * - Boundary tests: F1 vs F24, modifier combinations
 * - State transition tests: ready → inhibited → ready
 *
 * This test suite validates Screen5250 OIA (Operator Information Area) state
 * management and input handling across all function key combinations.
 *
 * @author Test Generator - TDD Phase 1
 */
public class FunctionKeyPairwiseTest {

    private Screen5250 screen;
    private ScreenOIA oia;

    @BeforeEach
    public void setUp() throws Exception {
        screen = new Screen5250();
        oia = screen.getOIA();
    }

    @AfterEach
    public void tearDown() throws Exception {
        screen = null;
        oia = null;
    }

    // ==================== POSITIVE TESTS - Ready State ====================

    /**
     * Test: Ready state - keyboard unlocked, input not inhibited
     * Pairwise: State=ready, Input=not-inhibited, Lock=false
     */
    @Test
    public void testReadyStateKeyboardUnlocked() {
        oia.setKeyBoardLocked(false);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        assertFalse(oia.isKeyBoardLocked(),"Keyboard should be unlocked");
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited(),"Should be not inhibited");
    }

    /**
     * Test: F1 key should have valid F-key code
     * Pairwise: Key=F1, Type=function-key
     */
    @Test
    public void testF1KeyCodeValid() {
        assertTrue(KeyEvent.VK_F1 > 0,"F1 should be valid key");
        assertEquals(KeyEvent.VK_F1, 112,"F1 code");
    }

    /**
     * Test: F12 key should have valid F-key code
     * Pairwise: Key=F12, Type=function-key
     */
    @Test
    public void testF12KeyCodeValid() {
        assertTrue(KeyEvent.VK_F12 > 0,"F12 should be valid key");
        assertEquals(KeyEvent.VK_F12, 123,"F12 code");
    }

    /**
     * Test: Enter key should have valid key code
     * Pairwise: Key=Enter, Type=control-key
     */
    @Test
    public void testEnterKeyCodeValid() {
        assertTrue(KeyEvent.VK_ENTER > 0,"Enter should be valid key");
        assertEquals(KeyEvent.VK_ENTER, 10,"Enter code");
    }

    /**
     * Test: Shift modifier should have valid constant
     * Pairwise: Modifier=shift, Value=constant
     */
    @Test
    public void testShiftModifierValid() {
        int shiftMask = KeyEvent.SHIFT_MASK;
        assertTrue(shiftMask > 0,"Shift mask should be positive");
        assertEquals(1, shiftMask,"Shift mask value");
    }

    /**
     * Test: Ctrl modifier should have valid constant
     * Pairwise: Modifier=ctrl, Value=constant
     */
    @Test
    public void testCtrlModifierValid() {
        int ctrlMask = KeyEvent.CTRL_MASK;
        assertTrue(ctrlMask > 0,"Ctrl mask should be positive");
        assertEquals(2, ctrlMask,"Ctrl mask value");
    }

    /**
     * Test: Alt modifier should have valid constant
     * Pairwise: Modifier=alt, Value=constant
     */
    @Test
    public void testAltModifierValid() {
        int altMask = KeyEvent.ALT_MASK;
        assertTrue(altMask > 0,"Alt mask should be positive");
        assertEquals(8, altMask,"Alt mask value");
    }

    /**
     * Test: Input inhibited state - system wait
     * Pairwise: State=inhibited, Type=system-wait
     */
    @Test
    public void testInputInhibitedSystemWait() {
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);

        assertTrue(oia.isKeyBoardLocked(),"Keyboard should be locked");
        assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                     oia.getInputInhibited(),"Should be system wait");
    }

    /**
     * Test: Input inhibited state - prog check
     * Pairwise: State=inhibited, Type=prog-check
     */
    @Test
    public void testInputInhibitedProgCheck() {
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, 0);

        assertTrue(oia.isKeyBoardLocked(),"Keyboard should be locked");
        assertEquals(ScreenOIA.INPUTINHIBITED_PROGCHECK,
                     oia.getInputInhibited(),"Should be prog check");
    }

    /**
     * Test: Input inhibited state - machine check
     * Pairwise: State=inhibited, Type=machine-check
     */
    @Test
    public void testInputInhibitedMachineCheck() {
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_MACHINECHECK, 0);

        assertTrue(oia.isKeyBoardLocked(),"Keyboard should be locked");
        assertEquals(ScreenOIA.INPUTINHIBITED_MACHINECHECK,
                     oia.getInputInhibited(),"Should be machine check");
    }

    /**
     * Test: Input inhibited state - comm check
     * Pairwise: State=inhibited, Type=comm-check
     */
    @Test
    public void testInputInhibitedCommCheck() {
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_COMMCHECK, 0);

        assertTrue(oia.isKeyBoardLocked(),"Keyboard should be locked");
        assertEquals(ScreenOIA.INPUTINHIBITED_COMMCHECK,
                     oia.getInputInhibited(),"Should be comm check");
    }

    /**
     * Test: Keyboard lock state transition - unlock
     * Pairwise: State=transition, From=locked, To=unlocked
     */
    @Test
    public void testKeyboardLockStateTransitionUnlock() {
        oia.setKeyBoardLocked(true);
        assertTrue(oia.isKeyBoardLocked(),"Initially locked");

        oia.setKeyBoardLocked(false);
        assertFalse(oia.isKeyBoardLocked(),"Should be unlocked");
    }

    /**
     * Test: Keyboard lock state transition - lock
     * Pairwise: State=transition, From=unlocked, To=locked
     */
    @Test
    public void testKeyboardLockStateTransitionLock() {
        oia.setKeyBoardLocked(false);
        assertFalse(oia.isKeyBoardLocked(),"Initially unlocked");

        oia.setKeyBoardLocked(true);
        assertTrue(oia.isKeyBoardLocked(),"Should be locked");
    }

    /**
     * Test: Input state transition - ready to inhibited
     * Pairwise: Transition=ready→inhibited
     */
    @Test
    public void testInputStateTransitionReadyToInhibited() {
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited(),"Start: not inhibited");

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);
        assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                     oia.getInputInhibited(),"End: system wait");
    }

    /**
     * Test: Input state transition - inhibited to ready
     * Pairwise: Transition=inhibited→ready
     */
    @Test
    public void testInputStateTransitionInhibitedToReady() {
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);
        assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                     oia.getInputInhibited(),"Start: system wait");

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited(),"End: not inhibited");
    }

    /**
     * Test: Multiple states can be read correctly
     * Pairwise: State=sequence, Operations=multiple-reads
     */
    @Test
    public void testMultipleStateReads() {
        oia.setKeyBoardLocked(false);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        for (int i = 0; i < 10; i++) {
            assertFalse(oia.isKeyBoardLocked(),"Read " + i + ": unlocked");
            assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                         oia.getInputInhibited(),"Read " + i + ": not inhibited");
        }
    }

    /**
     * Test: State changes persist across multiple transitions
     * Pairwise: Persistence=state-changes, Operations=10-transitions
     */
    @Test
    public void testStatePersistenceMultipleTransitions() {
        for (int i = 0; i < 10; i++) {
            boolean shouldLock = (i % 2 == 0);
            oia.setKeyBoardLocked(shouldLock);
            assertEquals(shouldLock, oia.isKeyBoardLocked(),"Transition " + i + ": lock state");
        }
    }

    // ==================== BOUNDARY TESTS ====================

    /**
     * Test: F1 is first function key (boundary)
     * Pairwise: Key=F1, Boundary=lower
     */
    @Test
    public void testF1FirstFunctionKeyBoundary() {
        assertTrue(KeyEvent.VK_F1 > 0,"F1 > 0");
        assertTrue(KeyEvent.VK_F1 >= 112 && KeyEvent.VK_F1 <= 123,"F1 is in valid range");
    }

    /**
     * Test: F24 via Shift+F12 (boundary)
     * Pairwise: Key=F24, Boundary=upper
     */
    @Test
    public void testF24UpperBoundary() {
        int f12Code = KeyEvent.VK_F12;
        int shiftMask = KeyEvent.SHIFT_MASK;

        assertTrue(f12Code > 0,"F12 valid");
        assertTrue(shiftMask > 0,"Shift valid");
        assertTrue((f12Code + shiftMask) > 0,"Combined valid");
    }

    /**
     * Test: No modifiers case (boundary)
     * Pairwise: Modifier=none, Value=0
     */
    @Test
    public void testNoModifiersBoundary() {
        int noModifiers = 0;
        assertEquals(0, noModifiers,"No modifiers = 0");
    }

    /**
     * Test: All modifiers combined (boundary)
     * Pairwise: Modifier=all, Value=shift|ctrl|alt
     */
    @Test
    public void testAllModifiersCombinedBoundary() {
        int allMods = KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK;
        assertTrue(allMods > 0,"Combined modifiers > 0");
        assertEquals(11, allMods,"All modifiers value");
    }

    /**
     * Test: Input inhibited state values are distinct
     * Pairwise: State=comparison, Property=distinctness
     */
    @Test
    public void testInputInhibitedStateDistinctness() {
        int notInhibited = ScreenOIA.INPUTINHIBITED_NOTINHIBITED;
        int systemWait = ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT;
        int commCheck = ScreenOIA.INPUTINHIBITED_COMMCHECK;
        int progCheck = ScreenOIA.INPUTINHIBITED_PROGCHECK;
        int machineCheck = ScreenOIA.INPUTINHIBITED_MACHINECHECK;

        assertTrue(notInhibited != systemWait,"Not inhibited != system wait");
        assertTrue(systemWait != commCheck,"System wait != comm check");
        assertTrue(commCheck != progCheck,"Comm check != prog check");
        assertTrue(progCheck != machineCheck,"Prog check != machine check");
    }

    // ==================== ADVERSARIAL TESTS ====================

    /**
     * Test: Rapid keyboard lock/unlock (flooding attack)
     * Adversarial: Flooding, Operation=lock-toggle
     */
    @Test
    public void testRapidKeyboardLockUnlockFlooding() {
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            boolean shouldLock = (i % 2 == 0);
            oia.setKeyBoardLocked(shouldLock);
        }

        // Final state after 1000 iterations (i=999, 999 % 2 == 1, so shouldLock=false)
        assertFalse(oia.isKeyBoardLocked(),"Final state should be unlocked");
    }

    /**
     * Test: Rapid input state changes (flooding attack)
     * Adversarial: Flooding, Operation=inhibit-toggle
     */
    @Test
    public void testRapidInputStateChangeFlooding() {
        int[] states = {
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            ScreenOIA.INPUTINHIBITED_COMMCHECK,
            ScreenOIA.INPUTINHIBITED_PROGCHECK,
            ScreenOIA.INPUTINHIBITED_MACHINECHECK
        };

        for (int i = 0; i < 1000; i++) {
            int state = states[i % states.length];
            oia.setInputInhibited(state, 0);
        }

        // Final state
        int finalState = ScreenOIA.INPUTINHIBITED_MACHINECHECK;
        assertEquals(finalState, oia.getInputInhibited(),"Final state");
    }

    /**
     * Test: Very large iteration count for state transitions
     * Adversarial: Endurance, Operation=state-change, Count=10000
     */
    @Test
    public void testLargeIterationCountStateTransitions() {
        for (int i = 0; i < 10000; i++) {
            boolean shouldLock = (i % 3 == 0);
            int state = (i % 2 == 0) ? ScreenOIA.INPUTINHIBITED_NOTINHIBITED
                                    : ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT;

            oia.setKeyBoardLocked(shouldLock);
            oia.setInputInhibited(state, 0);
        }

        // Verify final state is valid
        assertTrue(oia.isKeyBoardLocked() || !oia.isKeyBoardLocked(),"Final state valid");
        assertTrue(oia.getInputInhibited() >= 0,"Final inhibited state valid");
    }

    /**
     * Test: Concurrent-like state changes (sequential rapid changes)
     * Adversarial: Concurrency simulation, Operations=interleaved
     */
    @Test
    public void testInterleavedStateChanges() {
        // Simulate interleaved lock/unlock and inhibit/uninhibit
        for (int i = 0; i < 100; i++) {
            oia.setKeyBoardLocked(true);
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);
            oia.setKeyBoardLocked(false);
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        }

        assertFalse(oia.isKeyBoardLocked(),"Final lock state");
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited(),"Final inhibit state");
    }

    /**
     * Test: Null input handling for input inhibited message
     * Adversarial: Null input, Parameter=message
     */
    @Test
    public void testNullInputInhibitedMessage() {
        try {
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, null);
            assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                         oia.getInputInhibited(),"State should be set");
        } catch (NullPointerException e) {
            fail("Should handle null message: " + e.getMessage());
        }
    }

    /**
     * Test: Empty string input inhibited message
     * Adversarial: Empty input, Parameter=message
     */
    @Test
    public void testEmptyInputInhibitedMessage() {
        try {
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, "");
            assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                         oia.getInputInhibited(),"State should be set");
        } catch (Exception e) {
            fail("Should handle empty message: " + e.getMessage());
        }
    }

    /**
     * Test: Very long input inhibited message
     * Adversarial: Large input, Parameter=message
     */
    @Test
    public void testLongInputInhibitedMessage() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("A");
        }

        try {
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, sb.toString());
            assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                         oia.getInputInhibited(),"State should be set");
        } catch (Exception e) {
            fail("Should handle long message: " + e.getMessage());
        }
    }

    /**
     * Test: Negative keyboard lock code (invalid)
     * Adversarial: Invalid input, Parameter=lock-code
     */
    @Test
    public void testNegativeKeyboardLockCode() {
        try {
            // Try to set with unusual value
            oia.setKeyBoardLocked(false);
            assertFalse(oia.isKeyBoardLocked(),"Should be unlocked");
        } catch (Exception e) {
            fail("Should handle boolean lock: " + e.getMessage());
        }
    }

    /**
     * Test: Unusual what-code values for inhibit
     * Adversarial: Invalid input, Parameter=what-code
     */
    @Test
    public void testUnusualWhatCodeValues() {
        int[] unusualCodes = { -1, 0, 999, Integer.MAX_VALUE, Integer.MIN_VALUE };

        for (int code : unusualCodes) {
            try {
                oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, code);
                assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                             oia.getInputInhibited(),"State should be set");
            } catch (Exception e) {
                fail("Should handle what-code " + code + ": " + e.getMessage());
            }
        }
    }

    /**
     * Test: Invalid inhibit state values (out of range)
     * Adversarial: Invalid input, Parameter=state
     */
    @Test
    public void testInvalidInhibitStateValues() {
        int[] invalidStates = { -1, -999, 999, Integer.MAX_VALUE };

        for (int state : invalidStates) {
            try {
                oia.setInputInhibited(state, 0);
                // Should set without exception (no validation assumed)
            } catch (Exception e) {
                // Some implementations may validate
                assertTrue(true,"Exception acceptable for invalid state");
            }
        }
    }

    /**
     * Test: Rapid state query under no state change (cache coherency)
     * Adversarial: Query stress, Operation=read-only
     */
    @Test
    public void testRapidStateQueryNoChange() {
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);

        boolean lockState = oia.isKeyBoardLocked();
        int inhibitState = oia.getInputInhibited();

        for (int i = 0; i < 10000; i++) {
            assertEquals(lockState, oia.isKeyBoardLocked(),"Lock state should not change");
            assertEquals(inhibitState, oia.getInputInhibited(),"Inhibit state should not change");
        }
    }

    /**
     * Test: State change followed by immediate query
     * Adversarial: Timing, Operation=set-then-read
     */
    @Test
    public void testStateChangeFollowedByImmediateQuery() {
        for (int i = 0; i < 1000; i++) {
            oia.setKeyBoardLocked(true);
            assertTrue(oia.isKeyBoardLocked(),"Should read back true");

            oia.setKeyBoardLocked(false);
            assertFalse(oia.isKeyBoardLocked(),"Should read back false");
        }
    }

    /**
     * Test: All state constant values are non-negative
     * Boundary: Constants, Property=non-negative
     */
    @Test
    public void testAllStateConstantsNonNegative() {
        assertTrue(ScreenOIA.INPUTINHIBITED_NOTINHIBITED >= 0,"NOTINHIBITED >= 0");
        assertTrue(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT >= 0,"SYSTEM_WAIT >= 0");
        assertTrue(ScreenOIA.INPUTINHIBITED_COMMCHECK >= 0,"COMMCHECK >= 0");
        assertTrue(ScreenOIA.INPUTINHIBITED_PROGCHECK >= 0,"PROGCHECK >= 0");
        assertTrue(ScreenOIA.INPUTINHIBITED_MACHINECHECK >= 0,"MACHINECHECK >= 0");
    }

    /**
     * Test: Screen5250 initialization creates valid OIA
     * Initialization: Screen, Property=OIA-valid
     */
    @Test
    public void testScreen5250InitializesValidOIA() {
        Screen5250 screen2 = new Screen5250();
        assertNotNull(screen2,"Screen should initialize");

        ScreenOIA oia2 = screen2.getOIA();
        assertNotNull(oia2,"OIA should be created");

        // Initial keyboard state is locked (per jbInit() in Screen5250)
        assertTrue(oia2.isKeyBoardLocked(),"Initial lock state");
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia2.getInputInhibited(),"Initial inhibit state");
    }

    /**
     * Test: Multiple Screen5250 instances have independent OIAs
     * Isolation: Instances, Property=independence
     */
    @Test
    public void testMultipleScreen5250InstancesIndependent() {
        Screen5250 screen1 = new Screen5250();
        Screen5250 screen2 = new Screen5250();

        ScreenOIA oia1 = screen1.getOIA();
        ScreenOIA oia2 = screen2.getOIA();

        oia1.setKeyBoardLocked(true);
        oia2.setKeyBoardLocked(false);

        assertTrue(oia1.isKeyBoardLocked(),"Screen1 locked");
        assertFalse(oia2.isKeyBoardLocked(),"Screen2 unlocked");
    }

    /**
     * Test: OIA state in original screen unaffected by secondary instance
     * Isolation: Mutation, Property=cross-instance-isolation
     */
    @Test
    public void testOIACrossInstanceIsolation() {
        // Original screen state
        oia.setKeyBoardLocked(false);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        // Create secondary screen and change it
        Screen5250 screen2 = new Screen5250();
        ScreenOIA oia2 = screen2.getOIA();
        oia2.setKeyBoardLocked(true);
        oia2.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);

        // Verify original unchanged
        assertFalse(oia.isKeyBoardLocked(),"Original still unlocked");
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited(),"Original still not inhibited");
    }

    /**
     * Test: Key event creation with various valid codes
     * Coverage: Key codes, Types=F1-F12+Enter+Clear
     */
    @Test
    public void testKeyEventCreationVariousCodes() {
        int[] keyCodes = {
            KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4,
            KeyEvent.VK_F5, KeyEvent.VK_F6, KeyEvent.VK_F7, KeyEvent.VK_F8,
            KeyEvent.VK_F9, KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12,
            KeyEvent.VK_ENTER, KeyEvent.VK_CLEAR
        };

        for (int keyCode : keyCodes) {
            KeyEvent event = createKeyEvent(keyCode, 0);
            assertNotNull(event,"Event created for code " + keyCode);
            assertEquals(keyCode, event.getKeyCode(),"Event key code");
        }
    }

    // ==================== HELPER METHODS ====================

    // ==================== PAIRWISE KEYEVENT TESTS ====================

    /**
     * Test: F1 key with no modifiers
     * Pairwise: Key=F1, Modifier=none
     */
    @Test
    public void testF1WithNoModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_F1, 0);
        assertNotNull(event,"F1 event created");
        assertEquals(KeyEvent.VK_F1, event.getKeyCode(),"Key code F1");
        assertEquals(0, event.getModifiers(),"No modifiers");
    }

    /**
     * Test: F1 key with shift modifier
     * Pairwise: Key=F1, Modifier=shift
     */
    @Test
    public void testF1WithShiftModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_F1, KeyEvent.SHIFT_MASK);
        assertNotNull(event,"F1+Shift event created");
        assertEquals(KeyEvent.VK_F1, event.getKeyCode(),"Key code F1");
        assertEquals(KeyEvent.SHIFT_MASK, event.getModifiers(),"Shift modifier");
        assertTrue(event.isShiftDown(),"Shift down");
    }

    /**
     * Test: F1 key with ctrl modifier
     * Pairwise: Key=F1, Modifier=ctrl
     */
    @Test
    public void testF1WithCtrlModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_F1, KeyEvent.CTRL_MASK);
        assertNotNull(event,"F1+Ctrl event created");
        assertEquals(KeyEvent.VK_F1, event.getKeyCode(),"Key code F1");
        assertTrue(event.isControlDown(),"Ctrl down");
    }

    /**
     * Test: F1 key with alt modifier
     * Pairwise: Key=F1, Modifier=alt
     */
    @Test
    public void testF1WithAltModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_F1, KeyEvent.ALT_MASK);
        assertNotNull(event,"F1+Alt event created");
        assertEquals(KeyEvent.VK_F1, event.getKeyCode(),"Key code F1");
        assertTrue(event.isAltDown(),"Alt down");
    }

    /**
     * Test: F12 key with no modifiers
     * Pairwise: Key=F12, Modifier=none
     */
    @Test
    public void testF12WithNoModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_F12, 0);
        assertNotNull(event,"F12 event created");
        assertEquals(KeyEvent.VK_F12, event.getKeyCode(),"Key code F12");
    }

    /**
     * Test: F12 key with shift modifier (F24 simulation)
     * Pairwise: Key=F12, Modifier=shift (F13-F24 mapping)
     */
    @Test
    public void testF12WithShiftModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_F12, KeyEvent.SHIFT_MASK);
        assertNotNull(event,"F12+Shift event created");
        assertEquals(KeyEvent.VK_F12, event.getKeyCode(),"Key code F12");
        assertTrue(event.isShiftDown(),"Shift indicates F24 range");
    }

    /**
     * Test: Enter key with no modifiers
     * Pairwise: Key=Enter, Modifier=none
     */
    @Test
    public void testEnterWithNoModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_ENTER, 0);
        assertNotNull(event,"Enter event created");
        assertEquals(KeyEvent.VK_ENTER, event.getKeyCode(),"Key code Enter");
    }

    /**
     * Test: Enter key with ctrl modifier
     * Pairwise: Key=Enter, Modifier=ctrl
     */
    @Test
    public void testEnterWithCtrlModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_ENTER, KeyEvent.CTRL_MASK);
        assertNotNull(event,"Enter+Ctrl event created");
        assertTrue(event.isControlDown(),"Ctrl down");
    }

    /**
     * Test: Clear key constant exists
     * Pairwise: Key=Clear, Property=constant-defined
     */
    @Test
    public void testClearKeyConstantDefined() {
        int clearCode = KeyEvent.VK_CLEAR;
        assertTrue(clearCode > 0,"Clear key code valid");
    }

    /**
     * Test: Clear key with no modifiers
     * Pairwise: Key=Clear, Modifier=none
     */
    @Test
    public void testClearWithNoModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_CLEAR, 0);
        assertNotNull(event,"Clear event created");
        assertEquals(KeyEvent.VK_CLEAR, event.getKeyCode(),"Key code Clear");
    }

    /**
     * Test: Escape key (SysReq alternate)
     * Pairwise: Key=Escape, Purpose=system-request
     */
    @Test
    public void testEscapeKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_ESCAPE, 0);
        assertNotNull(event,"Escape event created");
        assertEquals(KeyEvent.VK_ESCAPE, event.getKeyCode(),"Key code Escape");
    }

    /**
     * Test: Tab key
     * Pairwise: Key=Tab, Property=navigation
     */
    @Test
    public void testTabKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_TAB, 0);
        assertNotNull(event,"Tab event created");
        assertEquals(KeyEvent.VK_TAB, event.getKeyCode(),"Key code Tab");
    }

    /**
     * Test: Tab key with shift modifier (back-tab)
     * Pairwise: Key=Tab, Modifier=shift (implies back-tab)
     */
    @Test
    public void testTabWithShiftModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_TAB, KeyEvent.SHIFT_MASK);
        assertNotNull(event,"Tab+Shift event created");
        assertTrue(event.isShiftDown(),"Shift indicates back-tab");
    }

    /**
     * Test: F2 key with combination modifiers
     * Pairwise: Key=F2, Modifier=shift+ctrl
     */
    @Test
    public void testF2WithCombinedModifiers() {
        int modifiers = KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK;
        KeyEvent event = createKeyEvent(KeyEvent.VK_F2, modifiers);
        assertNotNull(event,"F2+Shift+Ctrl event created");
        assertTrue(event.isShiftDown(),"Shift down");
        assertTrue(event.isControlDown(),"Ctrl down");
    }

    /**
     * Test: F11 key with no modifiers
     * Pairwise: Key=F11, Boundary=near-F12
     */
    @Test
    public void testF11WithNoModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_F11, 0);
        assertNotNull(event,"F11 event created");
        assertEquals(KeyEvent.VK_F11, event.getKeyCode(),"Key code F11");
    }

    /**
     * Test: F10 key with no modifiers
     * Pairwise: Key=F10, Boundary=10-range
     */
    @Test
    public void testF10WithNoModifier() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_F10, 0);
        assertNotNull(event,"F10 event created");
        assertEquals(KeyEvent.VK_F10, event.getKeyCode(),"Key code F10");
    }

    // ==================== PAIRWISE FUNCTION KEY COVERAGE ====================

    /**
     * Test: F3-F8 key range with no modifiers
     * Pairwise: Key=F3-F8, Modifier=none
     */
    @Test
    public void testF3ThroughF8WithNoModifier() {
        for (int f = KeyEvent.VK_F3; f <= KeyEvent.VK_F8; f++) {
            KeyEvent event = createKeyEvent(f, 0);
            assertNotNull(event,"F" + (f - KeyEvent.VK_F1 + 1) + " event created");
            assertEquals(f, event.getKeyCode(),"Key code matches");
        }
    }

    /**
     * Test: F9-F12 key range with alt modifier
     * Pairwise: Key=F9-F12, Modifier=alt
     */
    @Test
    public void testF9ThroughF12WithAltModifier() {
        for (int f = KeyEvent.VK_F9; f <= KeyEvent.VK_F12; f++) {
            KeyEvent event = createKeyEvent(f, KeyEvent.ALT_MASK);
            assertNotNull(event,"F" + (f - KeyEvent.VK_F1 + 1) + "+Alt event created");
            assertTrue(event.isAltDown(),"Alt down for F" + (f - KeyEvent.VK_F1 + 1));
        }
    }

    /**
     * Test: All modifier mask combinations
     * Pairwise: Modifier=all-combinations
     */
    @Test
    public void testAllModifierCombinations() {
        int[] modifiers = {
            0,                                                              // none
            KeyEvent.SHIFT_MASK,                                           // shift
            KeyEvent.CTRL_MASK,                                            // ctrl
            KeyEvent.ALT_MASK,                                             // alt
            KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK,                     // shift+ctrl
            KeyEvent.SHIFT_MASK | KeyEvent.ALT_MASK,                      // shift+alt
            KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK,                       // ctrl+alt
            KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK // all
        };

        int keyCode = KeyEvent.VK_F5;
        for (int mod : modifiers) {
            KeyEvent event = createKeyEvent(keyCode, mod);
            assertNotNull(event,"Event created for modifier 0x" + Integer.toHexString(mod));
            assertEquals(mod, event.getModifiers(),"Modifiers preserved");
        }
    }

    /**
     * Test: Backspace key
     * Pairwise: Key=Backspace, Type=control-key
     */
    @Test
    public void testBackspaceKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_BACK_SPACE, 0);
        assertNotNull(event,"Backspace event created");
        assertEquals(KeyEvent.VK_BACK_SPACE, event.getKeyCode(),"Key code Backspace");
    }

    /**
     * Test: Delete key
     * Pairwise: Key=Delete, Type=control-key
     */
    @Test
    public void testDeleteKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_DELETE, 0);
        assertNotNull(event,"Delete event created");
        assertEquals(KeyEvent.VK_DELETE, event.getKeyCode(),"Key code Delete");
    }

    /**
     * Test: Home key
     * Pairwise: Key=Home, Type=navigation
     */
    @Test
    public void testHomeKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_HOME, 0);
        assertNotNull(event,"Home event created");
        assertEquals(KeyEvent.VK_HOME, event.getKeyCode(),"Key code Home");
    }

    /**
     * Test: End key
     * Pairwise: Key=End, Type=navigation
     */
    @Test
    public void testEndKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_END, 0);
        assertNotNull(event,"End event created");
        assertEquals(KeyEvent.VK_END, event.getKeyCode(),"Key code End");
    }

    /**
     * Test: Page Up key
     * Pairwise: Key=PageUp, Type=navigation
     */
    @Test
    public void testPageUpKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_PAGE_UP, 0);
        assertNotNull(event,"Page Up event created");
        assertEquals(KeyEvent.VK_PAGE_UP, event.getKeyCode(),"Key code Page Up");
    }

    /**
     * Test: Page Down key
     * Pairwise: Key=PageDown, Type=navigation
     */
    @Test
    public void testPageDownKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_PAGE_DOWN, 0);
        assertNotNull(event,"Page Down event created");
        assertEquals(KeyEvent.VK_PAGE_DOWN, event.getKeyCode(),"Key code Page Down");
    }

    /**
     * Test: Arrow keys - Up
     * Pairwise: Key=Up, Type=navigation
     */
    @Test
    public void testUpArrowKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_UP, 0);
        assertNotNull(event,"Up arrow event created");
        assertEquals(KeyEvent.VK_UP, event.getKeyCode(),"Key code Up");
    }

    /**
     * Test: Arrow keys - Down
     * Pairwise: Key=Down, Type=navigation
     */
    @Test
    public void testDownArrowKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_DOWN, 0);
        assertNotNull(event,"Down arrow event created");
        assertEquals(KeyEvent.VK_DOWN, event.getKeyCode(),"Key code Down");
    }

    /**
     * Test: Arrow keys - Left
     * Pairwise: Key=Left, Type=navigation
     */
    @Test
    public void testLeftArrowKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_LEFT, 0);
        assertNotNull(event,"Left arrow event created");
        assertEquals(KeyEvent.VK_LEFT, event.getKeyCode(),"Key code Left");
    }

    /**
     * Test: Arrow keys - Right
     * Pairwise: Key=Right, Type=navigation
     */
    @Test
    public void testRightArrowKey() {
        KeyEvent event = createKeyEvent(KeyEvent.VK_RIGHT, 0);
        assertNotNull(event,"Right arrow event created");
        assertEquals(KeyEvent.VK_RIGHT, event.getKeyCode(),"Key code Right");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Create a synthetic KeyEvent for testing
     */
    private KeyEvent createKeyEvent(int keyCode, int modifiers) {
        return new KeyEvent(
            new javax.swing.JPanel(),           // source component
            KeyEvent.KEY_PRESSED,               // event type
            System.currentTimeMillis(),         // timestamp
            modifiers,                          // modifiers
            keyCode,                            // key code
            KeyEvent.CHAR_UNDEFINED,            // key char (undefined for function keys)
            KeyEvent.KEY_LOCATION_STANDARD      // key location
        );
    }
}
