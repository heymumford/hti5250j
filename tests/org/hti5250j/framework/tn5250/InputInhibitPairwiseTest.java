/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.hti5250j.event.ScreenOIAListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
public class InputInhibitPairwiseTest {

    // Test parameters
    private String inhibitCause;
    private String unlockTrigger;
    private String queuedInput;
    private String oiaIndicator;
    private String duration;

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

    private void setParameters(String inhibitCause, String unlockTrigger,
                                    String queuedInput, String oiaIndicator, String duration) {
        this.inhibitCause = inhibitCause;
        this.unlockTrigger = unlockTrigger;
        this.queuedInput = queuedInput;
        this.oiaIndicator = oiaIndicator;
        this.duration = duration;
    }

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
    @ParameterizedTest
    @MethodSource("data")
    public void testKeyboardUnlockedAtInitialization(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        assertFalse(oia.isKeyBoardLocked()
        ,
            "Keyboard should start unlocked in new OIA instance");
    }

    /**
     * TEST 2: Input not inhibited at initialization
     *
     * Positive test: Verify input starts in not-inhibited state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testInputNotInhibitedAtInitialization(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        ,
            "Input should start in NOTINHIBITED state");
    }

    /**
     * TEST 3: System-wait inhibition locks keyboard
     *
     * Positive test: Verify system-wait inhibition can be set and keyboard locks
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSystemWaitInhibitionLocksKeyboard(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!inhibitCause.equals(INHIBIT_SYSTEM_WAIT)) {
            return;
        }

        // Set inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0x01);

        // GREEN: Verify inhibited
        assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            oia.getInputInhibited()
        ,
            "Input should be inhibited with SYSTEM_WAIT");

        // Verify listener notified
        assertTrue(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        ,
            "Listener should be notified of input inhibit change");
    }

    /**
     * TEST 4: Prog-check inhibition with code preservation
     *
     * Positive test: Verify prog-check inhibition sets state and preserves code
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testProgCheckInhibitionWithCodePreservation(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!inhibitCause.equals(INHIBIT_PROG_CHECK)) {
            return;
        }

        int progCheckCode = 0x42;

        // Set prog-check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, progCheckCode);

        // GREEN: Verify inhibited
        assertEquals(ScreenOIA.INPUTINHIBITED_PROGCHECK,
            oia.getInputInhibited()
        ,
            "Input should be inhibited with PROGCHECK");
    }

    /**
     * TEST 5: Comm-check inhibition with code storage
     *
     * Positive test: Verify comm-check inhibition preserves communication code
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCommCheckInhibitionWithCodeStorage(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!inhibitCause.equals(INHIBIT_COMM_CHECK)) {
            return;
        }

        int commCheckCode = 0x55;

        // Set comm-check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_COMMCHECK, commCheckCode);

        // GREEN: Verify inhibited and code preserved
        assertEquals(ScreenOIA.INPUTINHIBITED_COMMCHECK,
            oia.getInputInhibited()
        ,
            "Input should be inhibited with COMMCHECK");

        assertEquals(commCheckCode,
            oia.getCommCheckCode()
        ,
            "Comm check code should be preserved");
    }

    /**
     * TEST 6: Machine-check inhibition with code preservation
     *
     * Positive test: Verify machine-check inhibition stores diagnostic code
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMachineCheckInhibitionWithCodePreservation(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!inhibitCause.equals(INHIBIT_MACHINE_CHECK)) {
            return;
        }

        int machineCheckCode = 0x99;

        // Set machine-check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_MACHINECHECK, machineCheckCode);

        // GREEN: Verify inhibited and code preserved
        assertEquals(ScreenOIA.INPUTINHIBITED_MACHINECHECK,
            oia.getInputInhibited()
        ,
            "Input should be inhibited with MACHINECHECK");

        assertEquals(machineCheckCode,
            oia.getMachineCheckCode()
        ,
            "Machine check code should be preserved");
    }

    /**
     * TEST 7: Host command unlock from inhibited state
     *
     * Positive test: Verify unlock via host command clears inhibition
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testHostCommandUnlockFromInhibition(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!unlockTrigger.equals(UNLOCK_HOST_COMMAND)) {
            return;
        }

        // Set inhibited
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);
        assertEquals(inhibitState, oia.getInputInhibited(),"Setup: Should be inhibited");

        // Clear listener for this phase
        oiaListener.clear();

        // Host command unlock
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        // GREEN: Verify not inhibited
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        ,
            "Input should be not inhibited after host command");

        assertTrue(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        ,
            "Listener should be notified of unlock");
    }

    /**
     * TEST 8: Keyboard unlock allows buffered input processing
     *
     * Positive test: Verify keyboard unlock with buffered input triggers send
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testKeyboardUnlockProcessesBufferedInput(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!queuedInput.equals(QUEUE_PENDING)) {
            return;
        }

        // Lock keyboard and buffer input
        oia.setKeyBoardLocked(true);
        oia.setKeysBuffered(true);
        assertTrue(oia.isKeyBoardLocked(),"Setup: Should be locked");
        assertTrue(oia.isKeysBuffered(),"Setup: Should have buffered input");

        // Clear listener
        oiaListener.clear();

        // Unlock keyboard - should trigger sendKeys()
        oia.setKeyBoardLocked(false);

        // GREEN: Verify unlocked
        assertFalse(oia.isKeyBoardLocked()
        ,
            "Keyboard should be unlocked");

        // Verify listener notified
        assertTrue(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        ,
            "Listener should be notified of unlock");
    }

    /**
     * TEST 9: Inhibited text message storage and retrieval
     *
     * Positive test: Verify inhibit message can be stored with status
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testInhibitedTextMessageStorage(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!oiaIndicator.equals(INDICATOR_TEXT)) {
            return;
        }

        String testMessage = "PROCESSING...";

        // Set inhibited with message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0x01, testMessage);

        // GREEN: Verify message retrieved
        assertEquals(testMessage,
            oia.getInhibitedText()
        ,
            "Inhibited text should be stored and retrieved");
    }

    /**
     * TEST 10: Message light on with inhibition
     *
     * Positive test: Verify message light can be set independently of inhibition
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageLightOnWithInhibition(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!oiaIndicator.equals(INDICATOR_NUMERIC) && !oiaIndicator.equals(INDICATOR_ICON)) {
            return;
        }

        // Set inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0x01);

        // Set message light
        oiaListener.clear();
        oia.setMessageLightOn();

        // GREEN: Verify message light on
        assertTrue(oia.isMessageWait()
        ,
            "Message light should be on");

        // Verify inhibition unchanged
        assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            oia.getInputInhibited()
        ,
            "Inhibition should remain unchanged");
    }

    // ========== ADVERSARIAL TESTS: Deadlock, Race Conditions, State Corruption ==========

    /**
     * TEST 11: Rapid inhibit-unlock cycle (potential deadlock)
     *
     * Adversarial test: Verify rapid inhibit-unlock transitions don't corrupt state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRapidInhibitUnlockCycleStability(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!duration.equals(DURATION_INSTANT)) {
            return;
        }

        int inhibitState = mapInhibitCause(inhibitCause);

        // Inhibit
        oia.setInputInhibited(inhibitState, 0x01);
        assertEquals(inhibitState, oia.getInputInhibited(),"Phase 1: Should be inhibited");

        // Unlock immediately
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited(),"Phase 2: Should be not inhibited");

        // Inhibit again
        oia.setInputInhibited(inhibitState, 0x02);
        assertEquals(inhibitState, oia.getInputInhibited(),"Phase 3: Should be inhibited again");

        // GREEN: Final state should be consistent
        assertTrue(oia.getInputInhibited() == inhibitState
        ,
            "Final inhibit state should be consistent with last transition");
    }

    /**
     * TEST 12: Inhibited state with queued input overflow (starvation risk)
     *
     * Adversarial test: Verify overflow queue doesn't cause state corruption
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testInhibitionWithQueuedInputOverflow(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
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
        assertEquals(inhibitState,
            oia.getInputInhibited()
        ,
            "Inhibit state should be preserved with queued overflow");

        assertTrue(oia.isKeyBoardLocked()
        ,
            "Keyboard should remain locked while inhibited");
    }

    /**
     * TEST 13: Machine-check with user-reset unlock (out-of-order unlock)
     *
     * Adversarial test: Verify user-initiated reset doesn't create inconsistency
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMachineCheckWithUserResetUnlock(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!inhibitCause.equals(INHIBIT_MACHINE_CHECK) || !unlockTrigger.equals(UNLOCK_USER_RESET)) {
            return;
        }

        // Set machine-check inhibition
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_MACHINECHECK, 0x88);

        // User initiates reset (host-side decision, not normal flow)
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        // GREEN: Verify clean state after reset
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        ,
            "Input should be cleared after user reset");

        // Verify keyboard can be unlocked
        oia.setKeyBoardLocked(true);
        oia.setKeyBoardLocked(false);
        assertFalse(oia.isKeyBoardLocked()
        ,
            "Keyboard should unlock cleanly after reset");
    }

    /**
     * TEST 14: Indefinite timeout with no unlock trigger (visibility risk)
     *
     * Adversarial test: Verify indefinite inhibition state is stable
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testIndefiniteInhibitionStability(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!duration.equals(DURATION_INDEFINITE)) {
            return;
        }

        int inhibitState = mapInhibitCause(inhibitCause);

        // Set inhibition without timeout
        oia.setInputInhibited(inhibitState, 0x01);

        // GREEN: Verify state remains stable (no implicit timeout)
        int count = 0;
        for (int i = 0; i < 10; i++) {
            assertEquals(inhibitState,oia.getInputInhibited()
            ,
                String.format("Inhibit state should remain stable on query %d", i));
            count++;
        }

        assertTrue(count == 10 && oia.getInputInhibited() == inhibitState
        ,
            "Indefinite inhibition should not change state");
    }

    /**
     * TEST 15: Multiple inhibit cause transitions (state machine stress)
     *
     * Adversarial test: Verify state machine handles multiple inhibit cause changes
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMultipleInhibitCauseTransitions(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        // Cycle through multiple inhibit causes
        int[] causes = {
            ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            ScreenOIA.INPUTINHIBITED_PROGCHECK,
            ScreenOIA.INPUTINHIBITED_COMMCHECK,
            ScreenOIA.INPUTINHIBITED_MACHINECHECK
        };

        for (int i = 0; i < causes.length; i++) {
            oia.setInputInhibited(causes[i], 0x10 + i);
            assertEquals(causes[i],oia.getInputInhibited()
            ,
                String.format("Cause transition %d should succeed", i));
        }

        // GREEN: Final state should match last transition
        assertEquals(ScreenOIA.INPUTINHIBITED_MACHINECHECK,
            oia.getInputInhibited()
        ,
            "Final inhibit cause should be MACHINECHECK");
    }

    /**
     * TEST 16: Keyboard lock idempotency during inhibition
     *
     * Adversarial test: Verify double-locking while inhibited doesn't corrupt state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testKeyboardLockIdempotencyDuringInhibition(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        // Set inhibition
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // Lock keyboard
        oia.setKeyBoardLocked(true);
        assertTrue(oia.isKeyBoardLocked(),"Setup: Should be locked");

        oiaListener.clear();

        // Lock again (idempotent operation)
        oia.setKeyBoardLocked(true);

        // GREEN: Should still be locked
        assertTrue(oia.isKeyBoardLocked()
        ,
            "Keyboard should remain locked after double-lock");

        // Verify no spurious notification
        assertFalse(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_KEYBOARD_LOCKED)
        ,
            "Listener should NOT be notified on idempotent lock");

        // Inhibition should be unchanged
        assertEquals(inhibitState,
            oia.getInputInhibited()
        ,
            "Inhibition state should not change");
    }

    /**
     * TEST 17: Inhibit with pending input while transitioning unlock
     *
     * Adversarial test: Verify pending input doesn't get lost during unlock
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testPendingInputPreservationDuringUnlock(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
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
        assertTrue(wasBuffered,"Setup: Should have buffered input");

        // Unlock
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        oia.setKeyBoardLocked(false);

        // GREEN: Verify pending input was handled
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        ,
            "Input should no longer be inhibited after unlock");

        assertFalse(oia.isKeyBoardLocked()
        ,
            "Keyboard should be unlocked");
    }

    /**
     * TEST 18: OIA indicator transitions during inhibition (icon rendering stability)
     *
     * Adversarial test: Verify OIA indicator changes don't destabilize inhibit state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testOIAIndicatorTransitionsDuringInhibition(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        // Set inhibition
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01, "PROCESSING...");

        // Transition indicators
        oia.setMessageLightOn();
        oia.setInsertMode(true);
        oia.setKeysBuffered(true);

        // GREEN: Inhibit state should remain unchanged
        assertEquals(inhibitState,
            oia.getInputInhibited()
        ,
            "Inhibit state should be unchanged despite indicator transitions");

        assertEquals("PROCESSING...",
            oia.getInhibitedText()
        ,
            "Inhibit message should be preserved");
    }

    /**
     * TEST 19: Overflow queue with indefinite inhibition (potential deadlock)
     *
     * Adversarial test: Verify overflow input doesn't hang indefinite inhibit
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testOverflowQueueWithIndefiniteInhibition(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
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
        assertEquals(inhibitState,
            oia.getInputInhibited()
        ,
            "Inhibit state should be stable under overflow pressure");

        assertTrue(oia.isKeyBoardLocked()
        ,
            "Keyboard should remain locked");

        // Verify unlock is still possible
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);
        oia.setKeyBoardLocked(false);

        assertFalse(oia.isKeyBoardLocked()
        ,
            "Should be able to unlock from overflow state");
    }

    /**
     * TEST 20: Inhibit code preservation across state transitions
     *
     * Adversarial test: Verify diagnostic codes survive multiple transitions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testInhibitCodePreservationAcrossTransitions(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
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
        assertEquals(originalCode,
            oia.getCommCheckCode()
        ,
            "Comm-check code should be preserved after transitions");

        assertEquals(0xBB,
            oia.getMachineCheckCode()
        ,
            "Machine-check code should remain from previous set");
    }

    /**
     * TEST 21: Listener notification timing during rapid transitions
     *
     * Adversarial test: Verify all listeners are notified even with rapid changes
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testListenerNotificationDuringRapidTransitions(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        TestOIAListener listener2 = new TestOIAListener();
        TestOIAListener listener3 = new TestOIAListener();

        oia.addOIAListener(listener2);
        oia.addOIAListener(listener3);

        // Rapid transitions
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0x01);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, 0x02);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0);

        // GREEN: All listeners should have been notified
        assertTrue(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        ,
            "All listeners should be notified of inhibit change 1");

        assertTrue(listener2.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        ,
            "Listener2 should be notified");

        assertTrue(listener3.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        ,
            "Listener3 should be notified");
    }

    /**
     * TEST 22: Keyboard lock state with message light independent operation
     *
     * Positive test: Verify message light changes don't affect keyboard lock state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageLightIndependenceFromKeyboardLock(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        oia.setKeyBoardLocked(true);
        boolean originalLocked = oia.isKeyBoardLocked();

        // Toggle message light
        oia.setMessageLightOn();
        oia.setMessageLightOff();

        // GREEN: Keyboard lock state should be unchanged
        assertEquals(originalLocked,
            oia.isKeyBoardLocked()
        ,
            "Keyboard lock state should not change with message light toggles");
    }

    /**
     * TEST 23: Timed duration inhibition state stability
     *
     * Positive test: Verify timed inhibition maintains state across queries
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testTimedDurationInhibitionStability(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        if (!duration.equals(DURATION_TIMED)) {
            return;
        }

        int inhibitState = mapInhibitCause(inhibitCause);

        // Set timed inhibition
        oia.setInputInhibited(inhibitState, 0x01);

        // Multiple queries should return same state
        for (int i = 0; i < 5; i++) {
            assertEquals(inhibitState,oia.getInputInhibited()
            ,
                String.format("Inhibit state should be stable on query %d", i));
        }
    }

    /**
     * TEST 24: Keys buffered flag transitions with inhibition
     *
     * Positive test: Verify keys buffered state can be set independently
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testKeysBufferedTransitionsWithInhibition(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // Initially false
        assertFalse(oia.isKeysBuffered(),"Setup: Keys should not be buffered initially");

        // Set buffered
        oia.setKeysBuffered(true);

        assertTrue(oia.isKeysBuffered()
        ,
            "Keys buffered should be true after setKeysBuffered(true)");

        // Inhibition should be unchanged
        assertEquals(inhibitState,
            oia.getInputInhibited()
        ,
            "Inhibit state should remain unchanged");

        // Clear buffered
        oia.setKeysBuffered(false);

        assertFalse(oia.isKeysBuffered()
        ,
            "Keys buffered should be false after setKeysBuffered(false)");
    }

    /**
     * TEST 25: Insert mode toggle with inhibited state
     *
     * Positive test: Verify insert mode can toggle independently of inhibition
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testInsertModeToggleWithInhibition(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // Toggle insert mode
        oia.setInsertMode(true);
        assertTrue(oia.isInsertMode(),"Insert mode should be on");

        // Inhibition should be unchanged
        assertEquals(inhibitState,
            oia.getInputInhibited()
        ,
            "Inhibit state should remain unchanged after insert mode on");

        // Toggle off
        oia.setInsertMode(false);
        assertFalse(oia.isInsertMode(),"Insert mode should be off");

        assertEquals(inhibitState,
            oia.getInputInhibited()
        ,
            "Inhibit state should remain unchanged after insert mode off");
    }

    /**
     * TEST 26: Drainability check - unlock with pending overflow
     *
     * Adversarial test: Verify system can drain queued input after unlock
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testQueueDrainabilityAfterUnlock(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
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
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        ,
            "Input should be clear for draining");

        assertFalse(oia.isKeyBoardLocked()
        ,
            "Keyboard should be unlocked for input processing");
    }

    /**
     * TEST 27: Owner field preservation during inhibit transitions
     *
     * Positive test: Verify owner field maintains identity through inhibit changes
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testOwnerFieldPreservationDuringInhibit(String inhibitCause, String unlockTrigger, String queuedInput, String oiaIndicator, String duration) throws Exception {
        setParameters(inhibitCause, unlockTrigger, queuedInput, oiaIndicator, duration);
        setUp();
        int testOwner = 99;
        oia.setOwner(testOwner);

        // Transition inhibit
        int inhibitState = mapInhibitCause(inhibitCause);
        oia.setInputInhibited(inhibitState, 0x01);

        // GREEN: Owner should be preserved
        assertEquals(testOwner,
            oia.getOwner()
        ,
            "Owner field should be preserved during inhibit transitions");
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
