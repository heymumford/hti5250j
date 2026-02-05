/**
 * Title: OIAPairwiseTest.java
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise TDD tests for ScreenOIA (Operator Information Area) status line operations
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
import org.tn5250j.event.ScreenOIAListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import static org.junit.Assert.*;

/**
 * Pairwise parameterized tests for ScreenOIA (Operator Information Area).
 *
 * Tests explore automation-critical status queries systematically across:
 * - OIA states: clear, input-inhibited, system-wait, message-waiting, insert-mode
 * - Keyboard states: unlocked, locked-system, locked-error
 * - Message types: none, error, info, warning
 * - Connection states: connected, disconnecting, disconnected
 * - Indicators: insert, caps, num-lock
 *
 * Critical discovery area: OIA state transitions, listener notifications,
 * input inhibition race conditions, keyboard lock state consistency.
 */
@RunWith(Parameterized.class)
public class OIAPairwiseTest {

    // Test parameters
    private final String oiaState;
    private final String keyboardState;
    private final String messageType;
    private final int inhibitCode;
    private final boolean expectedLocked;

    // Instance variables
    private ScreenOIA oia;
    private Screen5250TestDouble screen5250;
    private TestOIAListener oiaListener;

    // OIA State constants
    private static final String OIA_CLEAR = "CLEAR";
    private static final String OIA_INPUT_INHIBITED = "INPUT_INHIBITED";
    private static final String OIA_SYSTEM_WAIT = "SYSTEM_WAIT";
    private static final String OIA_MESSAGE_WAITING = "MESSAGE_WAITING";
    private static final String OIA_INSERT_MODE = "INSERT_MODE";

    // Keyboard State constants
    private static final String KBD_UNLOCKED = "UNLOCKED";
    private static final String KBD_LOCKED_SYSTEM = "LOCKED_SYSTEM";
    private static final String KBD_LOCKED_ERROR = "LOCKED_ERROR";

    // Message Type constants
    private static final String MSG_NONE = "NONE";
    private static final String MSG_ERROR = "ERROR";
    private static final String MSG_INFO = "INFO";
    private static final String MSG_WARNING = "WARNING";

    /**
     * Pairwise parameter combinations covering:
     * - OIA states: 5 values (clear, input-inhibited, system-wait, message-waiting, insert-mode)
     * - Keyboard states: 3 values (unlocked, locked-system, locked-error)
     * - Message types: 4 values (none, error, info, warning)
     * - Input inhibit codes: varied (not inhibited, system wait, comm check, prog check)
     * - Expected locked state: varies with keyboard state
     *
     * Pairwise minimum: 20 combinations covering critical interaction pairs
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Positive tests: Valid combinations
                // Row 1: OIA clear + keyboard unlocked + no message
                { OIA_CLEAR, KBD_UNLOCKED, MSG_NONE,
                  ScreenOIA.INPUTINHIBITED_NOTINHIBITED, false },

                // Row 2: OIA input-inhibited + keyboard locked-system + error message
                { OIA_INPUT_INHIBITED, KBD_LOCKED_SYSTEM, MSG_ERROR,
                  ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, true },

                // Row 3: OIA system-wait + keyboard locked-error + info message
                { OIA_SYSTEM_WAIT, KBD_LOCKED_ERROR, MSG_INFO,
                  ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, true },

                // Row 4: OIA message-waiting + keyboard unlocked + warning message
                { OIA_MESSAGE_WAITING, KBD_UNLOCKED, MSG_WARNING,
                  ScreenOIA.INPUTINHIBITED_NOTINHIBITED, false },

                // Row 5: OIA insert-mode + keyboard unlocked + no message
                { OIA_INSERT_MODE, KBD_UNLOCKED, MSG_NONE,
                  ScreenOIA.INPUTINHIBITED_NOTINHIBITED, false },

                // Row 6: OIA clear + keyboard locked-system + info message
                { OIA_CLEAR, KBD_LOCKED_SYSTEM, MSG_INFO,
                  ScreenOIA.INPUTINHIBITED_COMMCHECK, true },

                // Row 7: OIA input-inhibited + keyboard unlocked + warning message
                { OIA_INPUT_INHIBITED, KBD_UNLOCKED, MSG_WARNING,
                  ScreenOIA.INPUTINHIBITED_MACHINECHECK, false },

                // Row 8: OIA system-wait + keyboard unlocked + error message
                { OIA_SYSTEM_WAIT, KBD_UNLOCKED, MSG_ERROR,
                  ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, false },

                // Row 9: OIA message-waiting + keyboard locked-system + no message
                { OIA_MESSAGE_WAITING, KBD_LOCKED_SYSTEM, MSG_NONE,
                  ScreenOIA.INPUTINHIBITED_NOTINHIBITED, true },

                // Row 10: OIA insert-mode + keyboard locked-error + info message
                { OIA_INSERT_MODE, KBD_LOCKED_ERROR, MSG_INFO,
                  ScreenOIA.INPUTINHIBITED_PROGCHECK, true },

                // Row 11: OIA clear + keyboard unlocked + error message
                { OIA_CLEAR, KBD_UNLOCKED, MSG_ERROR,
                  ScreenOIA.INPUTINHIBITED_NOTINHIBITED, false },

                // Row 12: OIA input-inhibited + keyboard locked-error + warning message
                { OIA_INPUT_INHIBITED, KBD_LOCKED_ERROR, MSG_WARNING,
                  ScreenOIA.INPUTINHIBITED_OTHER, true },

                // Row 13: OIA system-wait + keyboard locked-system + no message
                { OIA_SYSTEM_WAIT, KBD_LOCKED_SYSTEM, MSG_NONE,
                  ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, true },

                // Row 14: OIA message-waiting + keyboard unlocked + info message
                { OIA_MESSAGE_WAITING, KBD_UNLOCKED, MSG_INFO,
                  ScreenOIA.INPUTINHIBITED_NOTINHIBITED, false },

                // Row 15: OIA insert-mode + keyboard unlocked + warning message
                { OIA_INSERT_MODE, KBD_UNLOCKED, MSG_WARNING,
                  ScreenOIA.INPUTINHIBITED_NOTINHIBITED, false },

                // Adversarial tests: Edge cases and state transitions
                // Row 16: Rapid transition - keyboard lock while inhibited
                { OIA_INPUT_INHIBITED, KBD_LOCKED_SYSTEM, MSG_NONE,
                  ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, true },

                // Row 17: Rapid transition - insert mode with locked keyboard
                { OIA_INSERT_MODE, KBD_LOCKED_SYSTEM, MSG_ERROR,
                  ScreenOIA.INPUTINHIBITED_COMMCHECK, true },

                // Row 18: Inhibited with multiple message changes
                { OIA_INPUT_INHIBITED, KBD_UNLOCKED, MSG_ERROR,
                  ScreenOIA.INPUTINHIBITED_MACHINECHECK, false },

                // Row 19: Message light transitions - from none to warning
                { OIA_MESSAGE_WAITING, KBD_LOCKED_ERROR, MSG_WARNING,
                  ScreenOIA.INPUTINHIBITED_NOTINHIBITED, true },

                // Row 20: Complex state - all systems engaged
                { OIA_INPUT_INHIBITED, KBD_LOCKED_ERROR, MSG_ERROR,
                  ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, true },
        });
    }

    public OIAPairwiseTest(String oiaState, String keyboardState, String messageType,
                          int inhibitCode, boolean expectedLocked) {
        this.oiaState = oiaState;
        this.keyboardState = keyboardState;
        this.messageType = messageType;
        this.inhibitCode = inhibitCode;
        this.expectedLocked = expectedLocked;
    }

    @Before
    public void setUp() {
        screen5250 = new Screen5250TestDouble();
        oia = new ScreenOIA(screen5250);
        oiaListener = new TestOIAListener();
        oia.addOIAListener(oiaListener);
    }

    /**
     * TEST 1: Keyboard lock state initialization
     *
     * Positive test: Verify keyboard starts unlocked and query returns correct state
     */
    @Test
    public void testKeyboardLockStateInitial() {
        // RED: Will fail if isKeyBoardLocked() doesn't return initial state
        assertFalse(
            "Keyboard should start unlocked in new OIA instance",
            oia.isKeyBoardLocked()
        );
    }

    /**
     * TEST 2: Keyboard lock state transitions with listener notification
     *
     * Positive test: Verify keyboard lock/unlock triggers listener and state reflects change
     */
    @Test
    public void testKeyboardLockStateTransitions() {
        if (!keyboardState.equals(KBD_LOCKED_SYSTEM) && !keyboardState.equals(KBD_LOCKED_ERROR)) {
            return; // Only test locking transitions in this case
        }

        // Set locked state
        oia.setKeyBoardLocked(true);

        // GREEN: Verify locked state
        assertTrue(
            String.format("Keyboard should be locked when set to locked in state %s", keyboardState),
            oia.isKeyBoardLocked()
        );

        // Verify listener was notified
        assertTrue(
            "Listener should have been notified of keyboard lock change",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );
    }

    /**
     * TEST 3: Keyboard unlock from locked state
     *
     * Positive test: Verify keyboard can transition from locked to unlocked
     */
    @Test
    public void testKeyboardUnlockTransition() {
        // Lock the keyboard first
        oia.setKeyBoardLocked(true);
        assertTrue("Setup: Keyboard should be locked", oia.isKeyBoardLocked());

        // Unlock it
        oia.setKeyBoardLocked(false);

        // GREEN: Verify unlocked state
        assertFalse(
            "Keyboard should be unlocked after unlock transition",
            oia.isKeyBoardLocked()
        );

        // Verify listener notified of unlock
        assertTrue(
            "Listener should be notified when keyboard unlocks",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );
    }

    /**
     * TEST 4: Input inhibited state with system-wait code
     *
     * Positive test: Verify input inhibition can be set and queried with code
     */
    @Test
    public void testInputInhibitedSystemWait() {
        if (!oiaState.equals(OIA_INPUT_INHIBITED) && !oiaState.equals(OIA_SYSTEM_WAIT)) {
            return; // Focus on inhibited states
        }

        // Set inhibited state
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, inhibitCode);

        // GREEN: Verify inhibited state
        assertEquals(
            String.format("Input should be inhibited with SYSTEM_WAIT in state %s", oiaState),
            ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            oia.getInputInhibited()
        );

        // Verify listener notified
        assertTrue(
            "Listener should be notified of input inhibit change",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        );
    }

    /**
     * TEST 5: Input inhibit with message text
     *
     * Positive test: Verify inhibited state can carry optional message text
     */
    @Test
    public void testInputInhibitWithMessage() {
        String testMessage = "PROCESSING...";

        // Set inhibited with message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, testMessage);

        // GREEN: Verify message is stored
        assertEquals(
            "Inhibited text should be retrieved correctly",
            testMessage,
            oia.getInhibitedText()
        );
    }

    /**
     * TEST 6: Message light on/off state transitions
     *
     * Positive test: Verify message light can toggle on and off with notification
     */
    @Test
    public void testMessageLightTransitions() {
        if (!messageType.equals(MSG_ERROR) && !messageType.equals(MSG_WARNING) && !messageType.equals(MSG_INFO)) {
            return; // Only test message transitions when message expected
        }

        // Initially should be off
        assertFalse(
            "Message light should start off",
            oia.isMessageWait()
        );

        // Turn on message light
        oia.setMessageLightOn();

        // GREEN: Verify on state
        assertTrue(
            "Message light should be on after setMessageLightOn",
            oia.isMessageWait()
        );

        // Verify listener notified
        assertTrue(
            "Listener should be notified of message light on",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_MESSAGELIGHT)
        );

        // Turn off message light
        oia.setMessageLightOff();

        // GREEN: Verify off state
        assertFalse(
            "Message light should be off after setMessageLightOff",
            oia.isMessageWait()
        );
    }

    /**
     * TEST 7: Insert mode toggle state
     *
     * Positive test: Verify insert mode can toggle with listener notification
     */
    @Test
    public void testInsertModeToggle() {
        if (!oiaState.equals(OIA_INSERT_MODE)) {
            return; // Focus on insert mode tests
        }

        // Initially off
        assertFalse(
            "Insert mode should start off",
            oia.isInsertMode()
        );

        // Turn on
        oia.setInsertMode(true);

        // GREEN: Verify on
        assertTrue(
            "Insert mode should be on after setInsertMode(true)",
            oia.isInsertMode()
        );

        // Verify listener notified
        assertTrue(
            "Listener should be notified of insert mode change",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INSERT_MODE)
        );

        // Turn off
        oia.setInsertMode(false);

        // GREEN: Verify off
        assertFalse(
            "Insert mode should be off after setInsertMode(false)",
            oia.isInsertMode()
        );
    }

    /**
     * TEST 8: Keys buffered state management
     *
     * Positive test: Verify buffered keys flag can be set and monitored
     */
    @Test
    public void testKeysBufferedState() {
        // Initially false
        assertFalse(
            "Keys buffered should start false",
            oia.isKeysBuffered()
        );

        // Set true
        oia.setKeysBuffered(true);

        // GREEN: Verify true
        assertTrue(
            "Keys buffered should be true after setKeysBuffered(true)",
            oia.isKeysBuffered()
        );

        // Verify listener notified
        assertTrue(
            "Listener should be notified when keys buffered state changes",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYS_BUFFERED)
        );
    }

    /**
     * TEST 9: Script active state
     *
     * Positive test: Verify script execution state can be toggled
     */
    @Test
    public void testScriptActiveState() {
        // Initially false
        assertFalse(
            "Script should not be active initially",
            oia.isScriptActive()
        );

        // Activate script
        oia.setScriptActive(true);

        // GREEN: Verify active
        assertTrue(
            "Script should be active after setScriptActive(true)",
            oia.isScriptActive()
        );

        // Verify listener notified
        assertTrue(
            "Listener should be notified of script state change",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_SCRIPT)
        );

        // Deactivate
        oia.setScriptActive(false);

        // GREEN: Verify inactive
        assertFalse(
            "Script should be inactive after setScriptActive(false)",
            oia.isScriptActive()
        );
    }

    /**
     * TEST 10: Owner field getter/setter
     *
     * Positive test: Verify owner ID can be set and retrieved
     */
    @Test
    public void testOwnerFieldAccess() {
        int testOwner = 42;

        // Set owner
        oia.setOwner(testOwner);

        // GREEN: Verify retrieved
        assertEquals(
            "Owner ID should be retrievable after setting",
            testOwner,
            oia.getOwner()
        );
    }

    /**
     * TEST 11: Keyboard lock idempotency - double lock
     *
     * Adversarial test: Verify locking an already-locked keyboard is safe
     */
    @Test
    public void testKeyboardLockIdempotencyDoubleLock() {
        oia.setKeyBoardLocked(true);
        assertTrue("Setup: Should be locked", oia.isKeyBoardLocked());

        // Clear listener notifications from setup
        oiaListener.clear();

        // Lock again
        oia.setKeyBoardLocked(true);

        // GREEN: Should still be locked
        assertTrue(
            "Keyboard should remain locked after double lock",
            oia.isKeyBoardLocked()
        );

        // Verify no spurious notification (state didn't change)
        assertFalse(
            "Listener should NOT be notified when locking already-locked keyboard",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );
    }

    /**
     * TEST 12: Multiple listener registration and notification
     *
     * Positive test: Verify multiple listeners all receive notifications
     */
    @Test
    public void testMultipleListenerNotification() {
        TestOIAListener listener2 = new TestOIAListener();
        TestOIAListener listener3 = new TestOIAListener();

        oia.addOIAListener(listener2);
        oia.addOIAListener(listener3);

        // Trigger a change
        oia.setKeyBoardLocked(true);

        // GREEN: Verify all listeners notified
        assertTrue(
            "Original listener should be notified",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );
        assertTrue(
            "Second listener should be notified",
            listener2.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );
        assertTrue(
            "Third listener should be notified",
            listener3.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );
    }

    /**
     * TEST 13: Listener removal stops notifications
     *
     * Positive test: Verify removed listeners no longer receive notifications
     */
    @Test
    public void testListenerRemovalStopsNotifications() {
        TestOIAListener listener2 = new TestOIAListener();
        oia.addOIAListener(listener2);

        // Remove first listener
        oia.removeOIAListener(oiaListener);

        // Clear previous notifications
        listener2.clear();

        // Trigger change
        oia.setKeyBoardLocked(true);

        // GREEN: Verify removed listener not notified
        assertFalse(
            "Removed listener should NOT be notified",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );

        // But remaining listener should be
        assertTrue(
            "Remaining listener should still be notified",
            listener2.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );
    }

    /**
     * TEST 14: Audible bell notification
     *
     * Positive test: Verify bell event triggers listener notification
     */
    @Test
    public void testAudibleBellNotification() {
        oiaListener.clear();

        // Trigger bell
        oia.setAudibleBell();

        // GREEN: Verify listener notified
        assertTrue(
            "Listener should be notified of bell event",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_BELL)
        );
    }

    /**
     * TEST 15: Clear screen notification
     *
     * Positive test: Verify clear screen event triggers listener notification
     */
    @Test
    public void testClearScreenNotification() {
        oiaListener.clear();

        // Trigger clear
        oia.clearScreen();

        // GREEN: Verify listener notified
        assertTrue(
            "Listener should be notified of clear screen event",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_CLEAR_SCREEN)
        );
    }

    /**
     * TEST 16: Input inhibition code preservation - comm check
     *
     * Positive test: Verify comm check code is preserved across inhibit states
     */
    @Test
    public void testCommCheckCodePreservation() {
        int commCheckCode = 0x42;

        // Set comm check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_COMMCHECK, commCheckCode);

        // GREEN: Verify code preserved
        assertEquals(
            "Comm check code should be preserved",
            commCheckCode,
            oia.getCommCheckCode()
        );
    }

    /**
     * TEST 17: Machine check code preservation
     *
     * Positive test: Verify machine check code is stored and retrieved
     */
    @Test
    public void testMachineCheckCodePreservation() {
        int machineCheckCode = 0x55;

        // Set machine check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_MACHINECHECK, machineCheckCode);

        // GREEN: Verify code preserved
        assertEquals(
            "Machine check code should be preserved",
            machineCheckCode,
            oia.getMachineCheckCode()
        );
    }

    /**
     * TEST 18: State transitions - clear to inhibited
     *
     * Adversarial test: Verify complex state transition doesn't corrupt state
     */
    @Test
    public void testStateTransitionClearToInhibited() {
        // Start in clear state (not inhibited)
        assertEquals(
            "Setup: Should start clear",
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        );

        // Transition to inhibited
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, inhibitCode);

        // GREEN: Verify inhibited
        assertEquals(
            "Should transition to inhibited state",
            ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            oia.getInputInhibited()
        );

        // Transition back to clear
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        // GREEN: Verify clear
        assertEquals(
            "Should transition back to clear state",
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        );
    }

    /**
     * TEST 19: Level field tracks last operation
     *
     * Positive test: Verify level field reflects the last OIA-changing operation
     */
    @Test
    public void testLevelFieldReflectsLastOperation() {
        // Perform keyboard lock operation
        oia.setKeyBoardLocked(true);

        // GREEN: Level should reflect keyboard operation
        assertEquals(
            "Level should reflect last keyboard operation",
            ScreenOIA.OIA_LEVEL_KEYBOARD,
            oia.getLevel()
        );

        // Perform insert mode operation
        oia.setInsertMode(true);

        // GREEN: Level should now reflect insert mode operation
        assertEquals(
            "Level should reflect last insert mode operation",
            ScreenOIA.OIA_LEVEL_INSERT_MODE,
            oia.getLevel()
        );
    }

    /**
     * TEST 20: Message light transitions preserve other state
     *
     * Adversarial test: Verify message light changes don't affect keyboard or inhibit state
     */
    @Test
    public void testMessageLightPreservesOtherState() {
        // Set up complex state
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);

        boolean originalLocked = oia.isKeyBoardLocked();
        int originalInhibit = oia.getInputInhibited();

        // Toggle message light
        oia.setMessageLightOn();
        oia.setMessageLightOff();

        // GREEN: Verify other state unchanged
        assertEquals(
            "Keyboard lock state should not change when toggling message light",
            originalLocked,
            oia.isKeyBoardLocked()
        );

        assertEquals(
            "Input inhibit state should not change when toggling message light",
            originalInhibit,
            oia.getInputInhibited()
        );
    }

    /**
     * Test double for Screen5250 interface
     */
    private static class Screen5250TestDouble extends Screen5250 {
        public Screen5250TestDouble() {
            super();
        }

        @Override
        public int getPos(int row, int col) {
            return row * 80 + col;
        }

        @Override
        public int getScreenLength() {
            return 24 * 80;
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

        @Override
        public void sendKeys(String keys) {
            // No-op for test
        }
    }

    /**
     * Test listener that tracks OIA change notifications
     */
    private static class TestOIAListener implements ScreenOIAListener {
        private Vector<Integer> changeEvents = new Vector<>();

        @Override
        public void onOIAChanged(ScreenOIA oia, int change) {
            changeEvents.add(change);
        }

        public boolean wasNotifiedOfChange(int changeType) {
            return changeEvents.contains(changeType);
        }

        public void clear() {
            changeEvents.clear();
        }
    }
}
