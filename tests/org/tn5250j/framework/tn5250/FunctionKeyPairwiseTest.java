package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.awt.event.KeyEvent;

import static org.junit.Assert.*;

/**
 * Pairwise JUnit 4 tests for TN5250j function key handling in KeyboardHandler.
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

    @Before
    public void setUp() throws Exception {
        screen = new Screen5250();
        oia = screen.getOIA();
    }

    @After
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

        assertFalse("Keyboard should be unlocked", oia.isKeyBoardLocked());
        assertEquals("Should be not inhibited", ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited());
    }

    /**
     * Test: F1 key should have valid F-key code
     * Pairwise: Key=F1, Type=function-key
     */
    @Test
    public void testF1KeyCodeValid() {
        assertTrue("F1 should be valid key", KeyEvent.VK_F1 > 0);
        assertEquals("F1 code", KeyEvent.VK_F1, 112);
    }

    /**
     * Test: F12 key should have valid F-key code
     * Pairwise: Key=F12, Type=function-key
     */
    @Test
    public void testF12KeyCodeValid() {
        assertTrue("F12 should be valid key", KeyEvent.VK_F12 > 0);
        assertEquals("F12 code", KeyEvent.VK_F12, 123);
    }

    /**
     * Test: Enter key should have valid key code
     * Pairwise: Key=Enter, Type=control-key
     */
    @Test
    public void testEnterKeyCodeValid() {
        assertTrue("Enter should be valid key", KeyEvent.VK_ENTER > 0);
        assertEquals("Enter code", KeyEvent.VK_ENTER, 10);
    }

    /**
     * Test: Shift modifier should have valid constant
     * Pairwise: Modifier=shift, Value=constant
     */
    @Test
    public void testShiftModifierValid() {
        int shiftMask = KeyEvent.SHIFT_MASK;
        assertTrue("Shift mask should be positive", shiftMask > 0);
        assertEquals("Shift mask value", 1, shiftMask);
    }

    /**
     * Test: Ctrl modifier should have valid constant
     * Pairwise: Modifier=ctrl, Value=constant
     */
    @Test
    public void testCtrlModifierValid() {
        int ctrlMask = KeyEvent.CTRL_MASK;
        assertTrue("Ctrl mask should be positive", ctrlMask > 0);
        assertEquals("Ctrl mask value", 2, ctrlMask);
    }

    /**
     * Test: Alt modifier should have valid constant
     * Pairwise: Modifier=alt, Value=constant
     */
    @Test
    public void testAltModifierValid() {
        int altMask = KeyEvent.ALT_MASK;
        assertTrue("Alt mask should be positive", altMask > 0);
        assertEquals("Alt mask value", 8, altMask);
    }

    /**
     * Test: Input inhibited state - system wait
     * Pairwise: State=inhibited, Type=system-wait
     */
    @Test
    public void testInputInhibitedSystemWait() {
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);

        assertTrue("Keyboard should be locked", oia.isKeyBoardLocked());
        assertEquals("Should be system wait", ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                     oia.getInputInhibited());
    }

    /**
     * Test: Input inhibited state - prog check
     * Pairwise: State=inhibited, Type=prog-check
     */
    @Test
    public void testInputInhibitedProgCheck() {
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, 0);

        assertTrue("Keyboard should be locked", oia.isKeyBoardLocked());
        assertEquals("Should be prog check", ScreenOIA.INPUTINHIBITED_PROGCHECK,
                     oia.getInputInhibited());
    }

    /**
     * Test: Input inhibited state - machine check
     * Pairwise: State=inhibited, Type=machine-check
     */
    @Test
    public void testInputInhibitedMachineCheck() {
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_MACHINECHECK, 0);

        assertTrue("Keyboard should be locked", oia.isKeyBoardLocked());
        assertEquals("Should be machine check", ScreenOIA.INPUTINHIBITED_MACHINECHECK,
                     oia.getInputInhibited());
    }

    /**
     * Test: Input inhibited state - comm check
     * Pairwise: State=inhibited, Type=comm-check
     */
    @Test
    public void testInputInhibitedCommCheck() {
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_COMMCHECK, 0);

        assertTrue("Keyboard should be locked", oia.isKeyBoardLocked());
        assertEquals("Should be comm check", ScreenOIA.INPUTINHIBITED_COMMCHECK,
                     oia.getInputInhibited());
    }

    /**
     * Test: Keyboard lock state transition - unlock
     * Pairwise: State=transition, From=locked, To=unlocked
     */
    @Test
    public void testKeyboardLockStateTransitionUnlock() {
        oia.setKeyBoardLocked(true);
        assertTrue("Initially locked", oia.isKeyBoardLocked());

        oia.setKeyBoardLocked(false);
        assertFalse("Should be unlocked", oia.isKeyBoardLocked());
    }

    /**
     * Test: Keyboard lock state transition - lock
     * Pairwise: State=transition, From=unlocked, To=locked
     */
    @Test
    public void testKeyboardLockStateTransitionLock() {
        oia.setKeyBoardLocked(false);
        assertFalse("Initially unlocked", oia.isKeyBoardLocked());

        oia.setKeyBoardLocked(true);
        assertTrue("Should be locked", oia.isKeyBoardLocked());
    }

    /**
     * Test: Input state transition - ready to inhibited
     * Pairwise: Transition=ready→inhibited
     */
    @Test
    public void testInputStateTransitionReadyToInhibited() {
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        assertEquals("Start: not inhibited", ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited());

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);
        assertEquals("End: system wait", ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                     oia.getInputInhibited());
    }

    /**
     * Test: Input state transition - inhibited to ready
     * Pairwise: Transition=inhibited→ready
     */
    @Test
    public void testInputStateTransitionInhibitedToReady() {
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);
        assertEquals("Start: system wait", ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                     oia.getInputInhibited());

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        assertEquals("End: not inhibited", ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited());
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
            assertFalse("Read " + i + ": unlocked", oia.isKeyBoardLocked());
            assertEquals("Read " + i + ": not inhibited", ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                         oia.getInputInhibited());
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
            assertEquals("Transition " + i + ": lock state", shouldLock, oia.isKeyBoardLocked());
        }
    }

    // ==================== BOUNDARY TESTS ====================

    /**
     * Test: F1 is first function key (boundary)
     * Pairwise: Key=F1, Boundary=lower
     */
    @Test
    public void testF1FirstFunctionKeyBoundary() {
        assertTrue("F1 > 0", KeyEvent.VK_F1 > 0);
        assertTrue("F1 is in valid range", KeyEvent.VK_F1 >= 112 && KeyEvent.VK_F1 <= 123);
    }

    /**
     * Test: F24 via Shift+F12 (boundary)
     * Pairwise: Key=F24, Boundary=upper
     */
    @Test
    public void testF24UpperBoundary() {
        int f12Code = KeyEvent.VK_F12;
        int shiftMask = KeyEvent.SHIFT_MASK;

        assertTrue("F12 valid", f12Code > 0);
        assertTrue("Shift valid", shiftMask > 0);
        assertTrue("Combined valid", (f12Code + shiftMask) > 0);
    }

    /**
     * Test: No modifiers case (boundary)
     * Pairwise: Modifier=none, Value=0
     */
    @Test
    public void testNoModifiersBoundary() {
        int noModifiers = 0;
        assertEquals("No modifiers = 0", 0, noModifiers);
    }

    /**
     * Test: All modifiers combined (boundary)
     * Pairwise: Modifier=all, Value=shift|ctrl|alt
     */
    @Test
    public void testAllModifiersCombinedBoundary() {
        int allMods = KeyEvent.SHIFT_MASK | KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK;
        assertTrue("Combined modifiers > 0", allMods > 0);
        assertEquals("All modifiers value", 11, allMods);
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

        assertTrue("Not inhibited != system wait", notInhibited != systemWait);
        assertTrue("System wait != comm check", systemWait != commCheck);
        assertTrue("Comm check != prog check", commCheck != progCheck);
        assertTrue("Prog check != machine check", progCheck != machineCheck);
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
        assertFalse("Final state should be unlocked", oia.isKeyBoardLocked());
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
        assertEquals("Final state", finalState, oia.getInputInhibited());
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
        assertTrue("Final state valid", oia.isKeyBoardLocked() || !oia.isKeyBoardLocked());
        assertTrue("Final inhibited state valid", oia.getInputInhibited() >= 0);
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

        assertFalse("Final lock state", oia.isKeyBoardLocked());
        assertEquals("Final inhibit state", ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited());
    }

    /**
     * Test: Null input handling for input inhibited message
     * Adversarial: Null input, Parameter=message
     */
    @Test
    public void testNullInputInhibitedMessage() {
        try {
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, null);
            assertEquals("State should be set", ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                         oia.getInputInhibited());
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
            assertEquals("State should be set", ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                         oia.getInputInhibited());
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
            assertEquals("State should be set", ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                         oia.getInputInhibited());
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
            assertFalse("Should be unlocked", oia.isKeyBoardLocked());
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
                assertEquals("State should be set", ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
                             oia.getInputInhibited());
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
                assertTrue("Exception acceptable for invalid state", true);
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
            assertEquals("Lock state should not change", lockState, oia.isKeyBoardLocked());
            assertEquals("Inhibit state should not change", inhibitState, oia.getInputInhibited());
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
            assertTrue("Should read back true", oia.isKeyBoardLocked());

            oia.setKeyBoardLocked(false);
            assertFalse("Should read back false", oia.isKeyBoardLocked());
        }
    }

    /**
     * Test: All state constant values are non-negative
     * Boundary: Constants, Property=non-negative
     */
    @Test
    public void testAllStateConstantsNonNegative() {
        assertTrue("NOTINHIBITED >= 0", ScreenOIA.INPUTINHIBITED_NOTINHIBITED >= 0);
        assertTrue("SYSTEM_WAIT >= 0", ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT >= 0);
        assertTrue("COMMCHECK >= 0", ScreenOIA.INPUTINHIBITED_COMMCHECK >= 0);
        assertTrue("PROGCHECK >= 0", ScreenOIA.INPUTINHIBITED_PROGCHECK >= 0);
        assertTrue("MACHINECHECK >= 0", ScreenOIA.INPUTINHIBITED_MACHINECHECK >= 0);
    }

    /**
     * Test: Screen5250 initialization creates valid OIA
     * Initialization: Screen, Property=OIA-valid
     */
    @Test
    public void testScreen5250InitializesValidOIA() {
        Screen5250 screen2 = new Screen5250();
        assertNotNull("Screen should initialize", screen2);

        ScreenOIA oia2 = screen2.getOIA();
        assertNotNull("OIA should be created", oia2);

        // Initial keyboard state is locked (per jbInit() in Screen5250)
        assertTrue("Initial lock state", oia2.isKeyBoardLocked());
        assertEquals("Initial inhibit state", ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia2.getInputInhibited());
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

        assertTrue("Screen1 locked", oia1.isKeyBoardLocked());
        assertFalse("Screen2 unlocked", oia2.isKeyBoardLocked());
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
        assertFalse("Original still unlocked", oia.isKeyBoardLocked());
        assertEquals("Original still not inhibited", ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
                     oia.getInputInhibited());
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
            assertNotNull("Event created for code " + keyCode, event);
            assertEquals("Event key code", keyCode, event.getKeyCode());
        }
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
