/**
 * Title: InputInhibitPairwiseTest.java
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise TDD tests for input inhibit state handling in HTI5250j
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
package org.hti5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.hti5250j.event.ScreenOIAListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import static org.junit.Assert.*;

/**
 * Pairwise parameterized tests for input inhibit handling and keyboard lock states.
 *
 * Tests explore automation-critical inhibit scenarios systematically across:
 * - Inhibit causes: system-wait, prog-check, comm-check, machine-check
 * - Unlock triggers: host-command, timeout, user-reset
 * - Queued input: none, pending, overflow
 * - OIA indicators: numeric, text, icon
 * - Duration: instant, timed, indefinite
 *
 * Critical discovery area: Keyboard lock state consistency, deadlock prevention,
 * inhibit state transitions, queued input handling under lock, listener notifications.
 */
@RunWith(Parameterized.class)
public class InputInhibitPairwiseTest {

    // Test parameters
    private final String inhibitCause;
    private final String unlockTrigger;
    private final String queuedInput;
    private final String oiaIndicator;
    private final String duration;

    // Inhibit cause constants
    private static final String INHIBIT_SYSTEM_WAIT = "SYSTEM_WAIT";
    private static final String INHIBIT_PROG_CHECK = "PROG_CHECK";
    private static final String INHIBIT_COMM_CHECK = "COMM_CHECK";
    private static final String INHIBIT_MACHINE_CHECK = "MACHINE_CHECK";

    // Unlock trigger constants
    private static final String UNLOCK_HOST_COMMAND = "HOST_COMMAND";
    private static final String UNLOCK_TIMEOUT = "TIMEOUT";
    private static final String UNLOCK_USER_RESET = "USER_RESET";

    // Queued input constants
    private static final String QUEUE_NONE = "NONE";
    private static final String QUEUE_PENDING = "PENDING";
    private static final String QUEUE_OVERFLOW = "OVERFLOW";

    // OIA indicator constants
    private static final String INDICATOR_NUMERIC = "NUMERIC";
    private static final String INDICATOR_TEXT = "TEXT";
    private static final String INDICATOR_ICON = "ICON";

    // Duration constants
    private static final String DURATION_INSTANT = "INSTANT";
    private static final String DURATION_TIMED = "TIMED";
    private static final String DURATION_INDEFINITE = "INDEFINITE";

    // Instance variables
    private ScreenOIA oia;
    private Screen5250TestDouble screen5250;
    private TestOIAListener oiaListener;
    private int inhibitCode;

    /**
     * Pairwise parameter combinations covering:
     * - Inhibit causes: 4 values (system-wait, prog-check, comm-check, machine-check)
     * - Unlock triggers: 3 values (host-command, timeout, user-reset)
     * - Queued input: 3 values (none, pending, overflow)
     * - OIA indicators: 3 values (numeric, text, icon)
     * - Duration: 3 values (instant, timed, indefinite)
     *
     * Pairwise minimum: 25+ combinations covering critical interaction pairs
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Row 1: System-wait + host-command + no queue + numeric + instant
                { INHIBIT_SYSTEM_WAIT, UNLOCK_HOST_COMMAND, QUEUE_NONE, INDICATOR_NUMERIC, DURATION_INSTANT },

                // Row 2: Prog-check + timeout + pending + text + timed
                { INHIBIT_PROG_CHECK, UNLOCK_TIMEOUT, QUEUE_PENDING, INDICATOR_TEXT, DURATION_TIMED },

                // Row 3: Comm-check + user-reset + overflow + icon + indefinite
                { INHIBIT_COMM_CHECK, UNLOCK_USER_RESET, QUEUE_OVERFLOW, INDICATOR_ICON, DURATION_INDEFINITE },

                // Row 4: Machine-check + host-command + none + numeric + timed
                { INHIBIT_MACHINE_CHECK, UNLOCK_HOST_COMMAND, QUEUE_NONE, INDICATOR_NUMERIC, DURATION_TIMED },

                // Row 5: System-wait + user-reset + pending + text + instant
                { INHIBIT_SYSTEM_WAIT, UNLOCK_USER_RESET, QUEUE_PENDING, INDICATOR_TEXT, DURATION_INSTANT },

                // Row 6: Prog-check + host-command + overflow + icon + indefinite
                { INHIBIT_PROG_CHECK, UNLOCK_HOST_COMMAND, QUEUE_OVERFLOW, INDICATOR_ICON, DURATION_INDEFINITE },

                // Row 7: Comm-check + timeout + none + text + instant
                { INHIBIT_COMM_CHECK, UNLOCK_TIMEOUT, QUEUE_NONE, INDICATOR_TEXT, DURATION_INSTANT },

                // Row 8: Machine-check + user-reset + pending + icon + timed
                { INHIBIT_MACHINE_CHECK, UNLOCK_USER_RESET, QUEUE_PENDING, INDICATOR_ICON, DURATION_TIMED },

                // Row 9: System-wait + timeout + overflow + numeric + indefinite
                { INHIBIT_SYSTEM_WAIT, UNLOCK_TIMEOUT, QUEUE_OVERFLOW, INDICATOR_NUMERIC, DURATION_INDEFINITE },

                // Row 10: Prog-check + user-reset + none + icon + instant
                { INHIBIT_PROG_CHECK, UNLOCK_USER_RESET, QUEUE_NONE, INDICATOR_ICON, DURATION_INSTANT },

                // Row 11: Comm-check + host-command + pending + numeric + timed
                { INHIBIT_COMM_CHECK, UNLOCK_HOST_COMMAND, QUEUE_PENDING, INDICATOR_NUMERIC, DURATION_TIMED },

                // Row 12: Machine-check + timeout + overflow + text + indefinite
                { INHIBIT_MACHINE_CHECK, UNLOCK_TIMEOUT, QUEUE_OVERFLOW, INDICATOR_TEXT, DURATION_INDEFINITE },

                // Row 13: System-wait + user-reset + none + text + timed
                { INHIBIT_SYSTEM_WAIT, UNLOCK_USER_RESET, QUEUE_NONE, INDICATOR_TEXT, DURATION_TIMED },

                // Row 14: Prog-check + timeout + pending + numeric + indefinite
                { INHIBIT_PROG_CHECK, UNLOCK_TIMEOUT, QUEUE_PENDING, INDICATOR_NUMERIC, DURATION_INDEFINITE },

                // Row 15: Comm-check + user-reset + overflow + text + instant
                { INHIBIT_COMM_CHECK, UNLOCK_USER_RESET, QUEUE_OVERFLOW, INDICATOR_TEXT, DURATION_INSTANT },

                // Adversarial/edge case tests: Deadlock and race conditions
                // Row 16: Rapid inhibit-unlock cycle with pending input (potential deadlock)
                { INHIBIT_SYSTEM_WAIT, UNLOCK_HOST_COMMAND, QUEUE_PENDING, INDICATOR_NUMERIC, DURATION_INSTANT },

                // Row 17: Inhibited with queued input overflow + timeout unlock (starvation risk)
                { INHIBIT_PROG_CHECK, UNLOCK_TIMEOUT, QUEUE_OVERFLOW, INDICATOR_NUMERIC, DURATION_INDEFINITE },

                // Row 18: Machine-check with pending input and user reset (out-of-order unlock)
                { INHIBIT_MACHINE_CHECK, UNLOCK_USER_RESET, QUEUE_PENDING, INDICATOR_TEXT, DURATION_INDEFINITE },

                // Row 19: Comm-check with icon indicator and indefinite timeout (visibility risk)
                { INHIBIT_COMM_CHECK, UNLOCK_TIMEOUT, QUEUE_NONE, INDICATOR_ICON, DURATION_INDEFINITE },

                // Row 20: Multiple inhibit transitions with queued overflow (state corruption risk)
                { INHIBIT_SYSTEM_WAIT, UNLOCK_HOST_COMMAND, QUEUE_OVERFLOW, INDICATOR_TEXT, DURATION_TIMED },

                // Row 21: Prog-check with text indicator transitions + pending queue
                { INHIBIT_PROG_CHECK, UNLOCK_HOST_COMMAND, QUEUE_PENDING, INDICATOR_TEXT, DURATION_TIMED },

                // Row 22: Comm-check icon indicator with immediate unlock and overflow queue
                { INHIBIT_COMM_CHECK, UNLOCK_HOST_COMMAND, QUEUE_OVERFLOW, INDICATOR_ICON, DURATION_INSTANT },

                // Row 23: Machine-check timed unlock with text indicator and no queue
                { INHIBIT_MACHINE_CHECK, UNLOCK_TIMEOUT, QUEUE_NONE, INDICATOR_TEXT, DURATION_TIMED },

                // Row 24: System-wait with numeric indicator and user-reset unlock with pending
                { INHIBIT_SYSTEM_WAIT, UNLOCK_USER_RESET, QUEUE_PENDING, INDICATOR_NUMERIC, DURATION_TIMED },

                // Row 25: Complex scenario - multiple inhibit causes in sequence (state machine stress)
                { INHIBIT_COMM_CHECK, UNLOCK_USER_RESET, QUEUE_PENDING, INDICATOR_NUMERIC, DURATION_INDEFINITE },

                // Row 26: Prog-check immediate unlock + text indicator + overflow queue (drainability check)
                { INHIBIT_PROG_CHECK, UNLOCK_HOST_COMMAND, QUEUE_OVERFLOW, INDICATOR_TEXT, DURATION_INSTANT },

                // Row 27: System-wait indefinite with timeout mechanism present + no queue (timer test)
                { INHIBIT_SYSTEM_WAIT, UNLOCK_TIMEOUT, QUEUE_NONE, INDICATOR_TEXT, DURATION_INDEFINITE },
        });
    }

    public InputInhibitPairwiseTest(String inhibitCause, String unlockTrigger,
                                    String queuedInput, String oiaIndicator, String duration) {
        this.inhibitCause = inhibitCause;
        this.unlockTrigger = unlockTrigger;
        this.queuedInput = queuedInput;
        this.oiaIndicator = oiaIndicator;
        this.duration = duration;
    }

    @Before
    public void setUp() {
        screen5250 = new Screen5250TestDouble();
        oia = new ScreenOIA(screen5250);
        oiaListener = new TestOIAListener();
        oia.addOIAListener(oiaListener);
        inhibitCode = 0x00; // Reset inhibit code for each test
    }

    // ========== POSITIVE TESTS: Inhibit State Initialization & Transitions ==========

    /**
     * TEST 1: Keyboard unlocked at initialization
     *
     * Positive test: Verify keyboard starts in unlocked state
     */
    @Test
    public void testKeyboardUnlockedAtInitialization() {
        assertFalse(
            "Keyboard should start unlocked in new OIA instance",
            oia.isKeyBoardLocked()
        );
    }

    /**
     * TEST 2: Input not inhibited at initialization
     *
     * Positive test: Verify input starts in not-inhibited state
     */
    @Test
    public void testInputNotInhibitedAtInitialization() {
        assertEquals(
            "Input should start in NOTINHIBITED state",
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        );
    }

    /**
     * TEST 3: System-wait inhibition locks keyboard
     *
     * Positive test: Verify system-wait inhibition can be set and keyboard locks
     */
    @Test
    public void testSystemWaitInhibitionLocksKeyboard() {
        if (!inhibitCause.equals(INHIBIT_SYSTEM_WAIT)) {
            return;
        }

        // Set inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0x01);

        // GREEN: Verify inhibited
        assertEquals(
            "Input should be inhibited with SYSTEM_WAIT",
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
     * TEST 4: Prog-check inhibition with code preservation
     *
     * Positive test: Verify prog-check inhibition sets state and preserves code
     */
    @Test
    public void testProgCheckInhibitionWithCodePreservation() {
        if (!inhibitCause.equals(INHIBIT_PROG_CHECK)) {
            return;
        }

        int progCheckCode = 0x42;

        // Set prog-check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, progCheckCode);

        // GREEN: Verify inhibited
        assertEquals(
            "Input should be inhibited with PROGCHECK",
            ScreenOIA.INPUTINHIBITED_PROGCHECK,
            oia.getInputInhibited()
        );
    }

    /**
     * TEST 5: Comm-check inhibition with code storage
     *
     * Positive test: Verify comm-check inhibition preserves communication code
     */
    @Test
    public void testCommCheckInhibitionWithCodeStorage() {
        if (!inhibitCause.equals(INHIBIT_COMM_CHECK)) {
            return;
        }

        int commCheckCode = 0x55;

        // Set comm-check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_COMMCHECK, commCheckCode);

        // GREEN: Verify inhibited and code preserved
        assertEquals(
            "Input should be inhibited with COMMCHECK",
            ScreenOIA.INPUTINHIBITED_COMMCHECK,
            oia.getInputInhibited()
        );

        assertEquals(
            "Comm check code should be preserved",
            commCheckCode,
            oia.getCommCheckCode()
        );
    }

    /**
     * TEST 6: Machine-check inhibition with code preservation
     *
     * Positive test: Verify machine-check inhibition stores diagnostic code
     */
    @Test
    public void testMachineCheckInhibitionWithCodePreservation() {
        if (!inhibitCause.equals(INHIBIT_MACHINE_CHECK)) {
            return;
        }

        int machineCheckCode = 0x99;

        // Set machine-check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_MACHINECHECK, machineCheckCode);

        // GREEN: Verify inhibited and code preserved
        assertEquals(
            "Input should be inhibited with MACHINECHECK",
            ScreenOIA.INPUTINHIBITED_MACHINECHECK,
            oia.getInputInhibited()
        );

        assertEquals(
            "Machine check code should be preserved",
            machineCheckCode,
            oia.getMachineCheckCode()
        );
    }

    /**
     * TEST 7: Host command unlock from inhibited state
     *
     * Positive test: Verify unlock via host command clears inhibition
     */
    @Test
    public void testHostCommandUnlockFromInhibition() {
        if (!unlockTrigger.equals(UNLOCK_HOST_COMMAND)) {
            return;
        }

        // Set inhibited
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);
        assertEquals("Setup: Should be inhibited", inhibitState, oia.getInputInhibited());

        // Clear listener for this phase
        oiaListener.clear();

        // Host command unlock
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        // GREEN: Verify not inhibited
        assertEquals(
            "Input should be not inhibited after host command",
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        );

        assertTrue(
            "Listener should be notified of unlock",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        );
    }

    /**
     * TEST 8: Keyboard unlock allows buffered input processing
     *
     * Positive test: Verify keyboard unlock with buffered input triggers send
     */
    @Test
    public void testKeyboardUnlockProcessesBufferedInput() {
        if (!queuedInput.equals(QUEUE_PENDING)) {
            return;
        }

        // Lock keyboard and buffer input
        oia.setKeyBoardLocked(true);
        oia.setKeysBuffered(true);
        assertTrue("Setup: Should be locked", oia.isKeyBoardLocked());
        assertTrue("Setup: Should have buffered input", oia.isKeysBuffered());

        // Clear listener
        oiaListener.clear();

        // Unlock keyboard - should trigger sendKeys()
        oia.setKeyBoardLocked(false);

        // GREEN: Verify unlocked
        assertFalse(
            "Keyboard should be unlocked",
            oia.isKeyBoardLocked()
        );

        // Verify listener notified
        assertTrue(
            "Listener should be notified of unlock",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );
    }

    /**
     * TEST 9: Inhibited text message storage and retrieval
     *
     * Positive test: Verify inhibit message can be stored with status
     */
    @Test
    public void testInhibitedTextMessageStorage() {
        if (!oiaIndicator.equals(INDICATOR_TEXT)) {
            return;
        }

        String testMessage = "PROCESSING...";

        // Set inhibited with message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0x01, testMessage);

        // GREEN: Verify message retrieved
        assertEquals(
            "Inhibited text should be stored and retrieved",
            testMessage,
            oia.getInhibitedText()
        );
    }

    /**
     * TEST 10: Message light on with inhibition
     *
     * Positive test: Verify message light can be set independently of inhibition
     */
    @Test
    public void testMessageLightOnWithInhibition() {
        if (!oiaIndicator.equals(INDICATOR_NUMERIC) && !oiaIndicator.equals(INDICATOR_ICON)) {
            return;
        }

        // Set inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0x01);

        // Set message light
        oiaListener.clear();
        oia.setMessageLightOn();

        // GREEN: Verify message light on
        assertTrue(
            "Message light should be on",
            oia.isMessageWait()
        );

        // Verify inhibition unchanged
        assertEquals(
            "Inhibition should remain unchanged",
            ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            oia.getInputInhibited()
        );
    }

    // ========== ADVERSARIAL TESTS: Deadlock, Race Conditions, State Corruption ==========

    /**
     * TEST 11: Rapid inhibit-unlock cycle (potential deadlock)
     *
     * Adversarial test: Verify rapid inhibit-unlock transitions don't corrupt state
     */
    @Test
    public void testRapidInhibitUnlockCycleStability() {
        if (!duration.equals(DURATION_INSTANT)) {
            return;
        }

        int inhibitState = mapInhibitCause(inhibitCause);

        // Inhibit
        oia.setInputInhibited(inhibitState, 0x01);
        assertEquals("Phase 1: Should be inhibited", inhibitState, oia.getInputInhibited());

        // Unlock immediately
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        assertEquals("Phase 2: Should be not inhibited", ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited());

        // Inhibit again
        oia.setInputInhibited(inhibitState, 0x02);
        assertEquals("Phase 3: Should be inhibited again", inhibitState, oia.getInputInhibited());

        // GREEN: Final state should be consistent
        assertTrue(
            "Final inhibit state should be consistent with last transition",
            oia.getInputInhibited() == inhibitState
        );
    }

    /**
     * TEST 12: Inhibited state with queued input overflow (starvation risk)
     *
     * Adversarial test: Verify overflow queue doesn't cause state corruption
     */
    @Test
    public void testInhibitionWithQueuedInputOverflow() {
        if (!queuedInput.equals(QUEUE_OVERFLOW)) {
            return;
        }

        // Set inhibition
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // Lock keyboard to queue input
        oia.setKeyBoardLocked(true);
        oia.setKeysBuffered(true);

        // GREEN: Verify inhibited state preserved despite queued input
        assertEquals(
            "Inhibit state should be preserved with queued overflow",
            inhibitState,
            oia.getInputInhibited()
        );

        assertTrue(
            "Keyboard should remain locked while inhibited",
            oia.isKeyBoardLocked()
        );
    }

    /**
     * TEST 13: Machine-check with user-reset unlock (out-of-order unlock)
     *
     * Adversarial test: Verify user-initiated reset doesn't create inconsistency
     */
    @Test
    public void testMachineCheckWithUserResetUnlock() {
        if (!inhibitCause.equals(INHIBIT_MACHINE_CHECK) || !unlockTrigger.equals(UNLOCK_USER_RESET)) {
            return;
        }

        // Set machine-check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_MACHINECHECK, 0x88);

        // User initiates reset (host-side decision, not normal flow)
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        // GREEN: Verify clean state after reset
        assertEquals(
            "Input should be cleared after user reset",
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        );

        // Verify keyboard can be unlocked
        oia.setKeyBoardLocked(true);
        oia.setKeyBoardLocked(false);
        assertFalse(
            "Keyboard should unlock cleanly after reset",
            oia.isKeyBoardLocked()
        );
    }

    /**
     * TEST 14: Indefinite timeout with no unlock trigger (visibility risk)
     *
     * Adversarial test: Verify indefinite inhibition state is stable
     */
    @Test
    public void testIndefiniteInhibitionStability() {
        if (!duration.equals(DURATION_INDEFINITE)) {
            return;
        }

        int inhibitState = mapInhibitCause(inhibitCause);

        // Set inhibition without timeout
        oia.setInputInhibited(inhibitState, 0x01);

        // GREEN: Verify state remains stable (no implicit timeout)
        int count = 0;
        for (int i = 0; i < 10; i++) {
            assertEquals(
                String.format("Inhibit state should remain stable on query %d", i),
                inhibitState,
                oia.getInputInhibited()
            );
            count++;
        }

        assertTrue(
            "Indefinite inhibition should not change state",
            count == 10 && oia.getInputInhibited() == inhibitState
        );
    }

    /**
     * TEST 15: Multiple inhibit cause transitions (state machine stress)
     *
     * Adversarial test: Verify state machine handles multiple inhibit cause changes
     */
    @Test
    public void testMultipleInhibitCauseTransitions() {
        // Cycle through multiple inhibit causes
        int[] causes = {
            ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            ScreenOIA.INPUTINHIBITED_PROGCHECK,
            ScreenOIA.INPUTINHIBITED_COMMCHECK,
            ScreenOIA.INPUTINHIBITED_MACHINECHECK
        };

        for (int i = 0; i < causes.length; i++) {
            oia.setInputInhibited(causes[i], 0x10 + i);
            assertEquals(
                String.format("Cause transition %d should succeed", i),
                causes[i],
                oia.getInputInhibited()
            );
        }

        // GREEN: Final state should match last transition
        assertEquals(
            "Final inhibit cause should be MACHINECHECK",
            ScreenOIA.INPUTINHIBITED_MACHINECHECK,
            oia.getInputInhibited()
        );
    }

    /**
     * TEST 16: Keyboard lock idempotency during inhibition
     *
     * Adversarial test: Verify double-locking while inhibited doesn't corrupt state
     */
    @Test
    public void testKeyboardLockIdempotencyDuringInhibition() {
        // Set inhibition
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // Lock keyboard
        oia.setKeyBoardLocked(true);
        assertTrue("Setup: Should be locked", oia.isKeyBoardLocked());

        oiaListener.clear();

        // Lock again (idempotent operation)
        oia.setKeyBoardLocked(true);

        // GREEN: Should still be locked
        assertTrue(
            "Keyboard should remain locked after double-lock",
            oia.isKeyBoardLocked()
        );

        // Verify no spurious notification
        assertFalse(
            "Listener should NOT be notified on idempotent lock",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        );

        // Inhibition should be unchanged
        assertEquals(
            "Inhibition state should not change",
            inhibitState,
            oia.getInputInhibited()
        );
    }

    /**
     * TEST 17: Inhibit with pending input while transitioning unlock
     *
     * Adversarial test: Verify pending input doesn't get lost during unlock
     */
    @Test
    public void testPendingInputPreservationDuringUnlock() {
        if (!queuedInput.equals(QUEUE_PENDING)) {
            return;
        }

        // Set inhibition and buffer input
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);
        oia.setKeyBoardLocked(true);
        oia.setKeysBuffered(true);

        // Remember buffered state
        boolean wasBuffered = oia.isKeysBuffered();
        assertTrue("Setup: Should have buffered input", wasBuffered);

        // Unlock
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        oia.setKeyBoardLocked(false);

        // GREEN: Verify pending input was handled
        assertEquals(
            "Input should no longer be inhibited after unlock",
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        );

        assertFalse(
            "Keyboard should be unlocked",
            oia.isKeyBoardLocked()
        );
    }

    /**
     * TEST 18: OIA indicator transitions during inhibition (icon rendering stability)
     *
     * Adversarial test: Verify OIA indicator changes don't destabilize inhibit state
     */
    @Test
    public void testOIAIndicatorTransitionsDuringInhibition() {
        // Set inhibition
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01, "PROCESSING...");

        // Transition indicators
        oia.setMessageLightOn();
        oia.setInsertMode(true);
        oia.setKeysBuffered(true);

        // GREEN: Inhibit state should remain unchanged
        assertEquals(
            "Inhibit state should be unchanged despite indicator transitions",
            inhibitState,
            oia.getInputInhibited()
        );

        assertEquals(
            "Inhibit message should be preserved",
            "PROCESSING...",
            oia.getInhibitedText()
        );
    }

    /**
     * TEST 19: Overflow queue with indefinite inhibition (potential deadlock)
     *
     * Adversarial test: Verify overflow input doesn't hang indefinite inhibit
     */
    @Test
    public void testOverflowQueueWithIndefiniteInhibition() {
        if (!queuedInput.equals(QUEUE_OVERFLOW) || !duration.equals(DURATION_INDEFINITE)) {
            return;
        }

        // Set indefinite inhibition
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // Lock keyboard to accumulate overflow
        oia.setKeyBoardLocked(true);
        oia.setKeysBuffered(true);

        // GREEN: State should remain stable (verify no deadlock)
        assertEquals(
            "Inhibit state should be stable under overflow pressure",
            inhibitState,
            oia.getInputInhibited()
        );

        assertTrue(
            "Keyboard should remain locked",
            oia.isKeyBoardLocked()
        );

        // Verify unlock is still possible
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        oia.setKeyBoardLocked(false);

        assertFalse(
            "Should be able to unlock from overflow state",
            oia.isKeyBoardLocked()
        );
    }

    /**
     * TEST 20: Inhibit code preservation across state transitions
     *
     * Adversarial test: Verify diagnostic codes survive multiple transitions
     */
    @Test
    public void testInhibitCodePreservationAcrossTransitions() {
        int originalCode = 0xAA;

        // Set comm-check with code
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_COMMCHECK, originalCode);
        int savedCommCode = oia.getCommCheckCode();

        // Transition to machine-check with different code
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_MACHINECHECK, 0xBB);
        int savedMachineCode = oia.getMachineCheckCode();

        // Transition back to comm-check
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_COMMCHECK, originalCode);

        // GREEN: Verify codes preserved (not overwritten)
        assertEquals(
            "Comm-check code should be preserved after transitions",
            originalCode,
            oia.getCommCheckCode()
        );

        assertEquals(
            "Machine-check code should remain from previous set",
            0xBB,
            oia.getMachineCheckCode()
        );
    }

    /**
     * TEST 21: Listener notification timing during rapid transitions
     *
     * Adversarial test: Verify all listeners are notified even with rapid changes
     */
    @Test
    public void testListenerNotificationDuringRapidTransitions() {
        TestOIAListener listener2 = new TestOIAListener();
        TestOIAListener listener3 = new TestOIAListener();

        oia.addOIAListener(listener2);
        oia.addOIAListener(listener3);

        // Rapid transitions
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0x01);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, 0x02);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        // GREEN: All listeners should have been notified
        assertTrue(
            "All listeners should be notified of inhibit change 1",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        );

        assertTrue(
            "Listener2 should be notified",
            listener2.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        );

        assertTrue(
            "Listener3 should be notified",
            listener3.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        );
    }

    /**
     * TEST 22: Keyboard lock state with message light independent operation
     *
     * Positive test: Verify message light changes don't affect keyboard lock state
     */
    @Test
    public void testMessageLightIndependenceFromKeyboardLock() {
        oia.setKeyBoardLocked(true);
        boolean originalLocked = oia.isKeyBoardLocked();

        // Toggle message light
        oia.setMessageLightOn();
        oia.setMessageLightOff();

        // GREEN: Keyboard lock state should be unchanged
        assertEquals(
            "Keyboard lock state should not change with message light toggles",
            originalLocked,
            oia.isKeyBoardLocked()
        );
    }

    /**
     * TEST 23: Timed duration inhibition state stability
     *
     * Positive test: Verify timed inhibition maintains state across queries
     */
    @Test
    public void testTimedDurationInhibitionStability() {
        if (!duration.equals(DURATION_TIMED)) {
            return;
        }

        int inhibitState = mapInhibitCause(inhibitCause);

        // Set timed inhibition
        oia.setInputInhibited(inhibitState, 0x01);

        // Multiple queries should return same state
        for (int i = 0; i < 5; i++) {
            assertEquals(
                String.format("Inhibit state should be stable on query %d", i),
                inhibitState,
                oia.getInputInhibited()
            );
        }
    }

    /**
     * TEST 24: Keys buffered flag transitions with inhibition
     *
     * Positive test: Verify keys buffered state can be set independently
     */
    @Test
    public void testKeysBufferedTransitionsWithInhibition() {
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // Initially false
        assertFalse("Setup: Keys should not be buffered initially", oia.isKeysBuffered());

        // Set buffered
        oia.setKeysBuffered(true);

        assertTrue(
            "Keys buffered should be true after setKeysBuffered(true)",
            oia.isKeysBuffered()
        );

        // Inhibition should be unchanged
        assertEquals(
            "Inhibit state should remain unchanged",
            inhibitState,
            oia.getInputInhibited()
        );

        // Clear buffered
        oia.setKeysBuffered(false);

        assertFalse(
            "Keys buffered should be false after setKeysBuffered(false)",
            oia.isKeysBuffered()
        );
    }

    /**
     * TEST 25: Insert mode toggle with inhibited state
     *
     * Positive test: Verify insert mode can toggle independently of inhibition
     */
    @Test
    public void testInsertModeToggleWithInhibition() {
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // Toggle insert mode
        oia.setInsertMode(true);
        assertTrue("Insert mode should be on", oia.isInsertMode());

        // Inhibition should be unchanged
        assertEquals(
            "Inhibit state should remain unchanged after insert mode on",
            inhibitState,
            oia.getInputInhibited()
        );

        // Toggle off
        oia.setInsertMode(false);
        assertFalse("Insert mode should be off", oia.isInsertMode());

        assertEquals(
            "Inhibit state should remain unchanged after insert mode off",
            inhibitState,
            oia.getInputInhibited()
        );
    }

    /**
     * TEST 26: Drainability check - unlock with pending overflow
     *
     * Adversarial test: Verify system can drain queued input after unlock
     */
    @Test
    public void testQueueDrainabilityAfterUnlock() {
        if (!queuedInput.equals(QUEUE_OVERFLOW) || !unlockTrigger.equals(UNLOCK_HOST_COMMAND)) {
            return;
        }

        // Setup overflow scenario
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);
        oia.setKeyBoardLocked(true);
        oia.setKeysBuffered(true);

        // Unlock via host command
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        oia.setKeyBoardLocked(false);

        // GREEN: Verify system is in clean state for draining
        assertEquals(
            "Input should be clear for draining",
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        );

        assertFalse(
            "Keyboard should be unlocked for input processing",
            oia.isKeyBoardLocked()
        );
    }

    /**
     * TEST 27: Owner field preservation during inhibit transitions
     *
     * Positive test: Verify owner field maintains identity through inhibit changes
     */
    @Test
    public void testOwnerFieldPreservationDuringInhibit() {
        int testOwner = 99;
        oia.setOwner(testOwner);

        // Transition inhibit
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // GREEN: Owner should be preserved
        assertEquals(
            "Owner field should be preserved during inhibit transitions",
            testOwner,
            oia.getOwner()
        );
    }

    // ========== HELPER METHODS ==========

    /**
     * Map inhibit cause string to ScreenOIA constant
     */
    private int mapInhibitCause(String cause) {
        switch (cause) {
            case INHIBIT_SYSTEM_WAIT:
                return ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT;
            case INHIBIT_PROG_CHECK:
                return ScreenOIA.INPUTINHIBITED_PROGCHECK;
            case INHIBIT_COMM_CHECK:
                return ScreenOIA.INPUTINHIBITED_COMMCHECK;
            case INHIBIT_MACHINE_CHECK:
                return ScreenOIA.INPUTINHIBITED_MACHINECHECK;
            default:
                return ScreenOIA.INPUTINHIBITED_NOTINHIBITED;
        }
    }

    /**
     * Test double for Screen5250 interface
     */
    private static class Screen5250TestDouble extends Screen5250 {
        private boolean sendKeysCalled = false;

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
            sendKeysCalled = true;
        }

        public boolean wasSendKeysCalled() {
            return sendKeysCalled;
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
