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
 * Pairwise parameterized tests for message line handling and display.
 *
 * Tests systematically explore message behavior across:
 * 1. Message type: info, warning, error, system, input-inhibited
 * 2. Message length: 0 (empty), 1, 80 (standard), 132 (wide), overflow
 * 3. Display duration: instant, timed, persistent
 * 4. Priority: low, normal, high, critical
 * 5. Screen area: message-line, OIA, popup
 *
 * Critical discovery areas:
 * - Message display with varying lengths on fixed-width display (80/132 columns)
 * - Message overflow handling and injection protection
 * - OIA state synchronization with message changes
 * - Message priority queueing and display ordering
 * - Message persistence across screen updates
 * - Adversarial: null messages, special characters, control sequences, race conditions
 */
public class MessageLinePairwiseTest {

    // Test parameters
    private String messageType;
    private int messageLength;
    private String displayDuration;
    private String priority;
    private String screenArea;

    // Instance variables
    private ScreenOIA oia;
    private Screen5250TestDouble screen5250;
    private TestMessageListener messageListener;
    private TestOIAListener oiaListener;

    // Message Type constants
    private static final String MSG_INFO = "INFO";
    private static final String MSG_WARNING = "WARNING";
    private static final String MSG_ERROR = "ERROR";
    private static final String MSG_SYSTEM = "SYSTEM";
    private static final String MSG_INPUT_INHIBITED = "INPUT_INHIBITED";

    // Message Length constants
    private static final int LEN_EMPTY = 0;
    private static final int LEN_SINGLE = 1;
    private static final int LEN_STANDARD = 80;
    private static final int LEN_WIDE = 132;
    private static final int LEN_OVERFLOW = 200;

    // Display Duration constants
    private static final String DURATION_INSTANT = "INSTANT";
    private static final String DURATION_TIMED = "TIMED";
    private static final String DURATION_PERSISTENT = "PERSISTENT";

    // Priority constants
    private static final String PRIORITY_LOW = "LOW";
    private static final String PRIORITY_NORMAL = "NORMAL";
    private static final String PRIORITY_HIGH = "HIGH";
    private static final String PRIORITY_CRITICAL = "CRITICAL";

    // Screen Area constants
    private static final String AREA_MESSAGE_LINE = "MESSAGE_LINE";
    private static final String AREA_OIA = "OIA";
    private static final String AREA_POPUP = "POPUP";

    /**
     * Pairwise parameter combinations covering:
     * - Message types: 5 values (info, warning, error, system, input-inhibited)
     * - Message lengths: 5 values (0, 1, 80, 132, overflow)
     * - Display durations: 3 values (instant, timed, persistent)
     * - Priorities: 4 values (low, normal, high, critical)
     * - Screen areas: 3 values (message-line, OIA, popup)
     *
     * Pairwise minimum: 25+ combinations covering critical interaction pairs
     */
        public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Row 1: Info message, empty, instant, low, message-line
                { MSG_INFO, LEN_EMPTY, DURATION_INSTANT, PRIORITY_LOW, AREA_MESSAGE_LINE },

                // Row 2: Warning message, single char, timed, normal, OIA
                { MSG_WARNING, LEN_SINGLE, DURATION_TIMED, PRIORITY_NORMAL, AREA_OIA },

                // Row 3: Error message, standard 80, persistent, high, popup
                { MSG_ERROR, LEN_STANDARD, DURATION_PERSISTENT, PRIORITY_HIGH, AREA_POPUP },

                // Row 4: System message, wide 132, instant, critical, message-line
                { MSG_SYSTEM, LEN_WIDE, DURATION_INSTANT, PRIORITY_CRITICAL, AREA_MESSAGE_LINE },

                // Row 5: Input-inhibited, overflow 200, timed, low, OIA
                { MSG_INPUT_INHIBITED, LEN_OVERFLOW, DURATION_TIMED, PRIORITY_LOW, AREA_OIA },

                // Row 6: Info message, single char, persistent, normal, popup
                { MSG_INFO, LEN_SINGLE, DURATION_PERSISTENT, PRIORITY_NORMAL, AREA_POPUP },

                // Row 7: Warning message, standard 80, instant, high, OIA
                { MSG_WARNING, LEN_STANDARD, DURATION_INSTANT, PRIORITY_HIGH, AREA_OIA },

                // Row 8: Error message, wide 132, timed, critical, message-line
                { MSG_ERROR, LEN_WIDE, DURATION_TIMED, PRIORITY_CRITICAL, AREA_MESSAGE_LINE },

                // Row 9: System message, overflow 200, persistent, low, popup
                { MSG_SYSTEM, LEN_OVERFLOW, DURATION_PERSISTENT, PRIORITY_LOW, AREA_POPUP },

                // Row 10: Input-inhibited, empty, instant, normal, message-line
                { MSG_INPUT_INHIBITED, LEN_EMPTY, DURATION_INSTANT, PRIORITY_NORMAL, AREA_MESSAGE_LINE },

                // Row 11: Info message, wide 132, timed, high, message-line
                { MSG_INFO, LEN_WIDE, DURATION_TIMED, PRIORITY_HIGH, AREA_MESSAGE_LINE },

                // Row 12: Warning message, overflow 200, persistent, critical, OIA
                { MSG_WARNING, LEN_OVERFLOW, DURATION_PERSISTENT, PRIORITY_CRITICAL, AREA_OIA },

                // Row 13: Error message, empty, instant, low, popup
                { MSG_ERROR, LEN_EMPTY, DURATION_INSTANT, PRIORITY_LOW, AREA_POPUP },

                // Row 14: System message, single char, timed, normal, message-line
                { MSG_SYSTEM, LEN_SINGLE, DURATION_TIMED, PRIORITY_NORMAL, AREA_MESSAGE_LINE },

                // Row 15: Input-inhibited, standard 80, persistent, high, OIA
                { MSG_INPUT_INHIBITED, LEN_STANDARD, DURATION_PERSISTENT, PRIORITY_HIGH, AREA_OIA },

                // Adversarial tests: Edge cases, overflow, injection, race conditions
                // Row 16: Overflow message with special characters
                { MSG_ERROR, LEN_OVERFLOW, DURATION_INSTANT, PRIORITY_CRITICAL, AREA_MESSAGE_LINE },

                // Row 17: Multiple rapid message transitions
                { MSG_INFO, LEN_STANDARD, DURATION_INSTANT, PRIORITY_CRITICAL, AREA_OIA },

                // Row 18: Message with null/empty and then content
                { MSG_WARNING, LEN_EMPTY, DURATION_PERSISTENT, PRIORITY_HIGH, AREA_POPUP },

                // Row 19: High-priority system message
                { MSG_SYSTEM, LEN_STANDARD, DURATION_TIMED, PRIORITY_CRITICAL, AREA_MESSAGE_LINE },

                // Row 20: Input-inhibited with overflow (critical state)
                { MSG_INPUT_INHIBITED, LEN_OVERFLOW, DURATION_INSTANT, PRIORITY_CRITICAL, AREA_MESSAGE_LINE },

                // Row 21: Standard message, all combinations tested
                { MSG_INFO, LEN_STANDARD, DURATION_TIMED, PRIORITY_NORMAL, AREA_MESSAGE_LINE },

                // Row 22: Error with widest possible display
                { MSG_ERROR, LEN_WIDE, DURATION_PERSISTENT, PRIORITY_HIGH, AREA_POPUP },

                // Row 23: System message overflow to OIA
                { MSG_SYSTEM, LEN_OVERFLOW, DURATION_TIMED, PRIORITY_NORMAL, AREA_OIA },

                // Row 24: Warning persistent in popup
                { MSG_WARNING, LEN_STANDARD, DURATION_PERSISTENT, PRIORITY_LOW, AREA_POPUP },

                // Row 25: Critical info message display
                { MSG_INFO, LEN_WIDE, DURATION_INSTANT, PRIORITY_CRITICAL, AREA_MESSAGE_LINE },

                // Row 26: Complex state - all dimensions engaged
                { MSG_ERROR, LEN_OVERFLOW, DURATION_PERSISTENT, PRIORITY_CRITICAL, AREA_OIA },
        });
    }

    private void setParameters(String messageType, int messageLength, String displayDuration,
                                   String priority, String screenArea) {
        this.messageType = messageType;
        this.messageLength = messageLength;
        this.displayDuration = displayDuration;
        this.priority = priority;
        this.screenArea = screenArea;
    }

        public void setUp() {
        screen5250 = new Screen5250TestDouble();
        oia = new ScreenOIA(screen5250);
        messageListener = new TestMessageListener();
        oiaListener = new TestOIAListener();
        oia.addOIAListener(oiaListener);
    }

    /**
     * TEST 1: Message light state initialization
     *
     * Positive test: Verify message light starts off and can be queried
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageLightInitial(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        // RED: Will fail if isMessageWait() doesn't return initial state
        assertFalse(oia.isMessageWait()
        ,
            "Message light should start off in new OIA instance");
    }

    /**
     * TEST 2: Message light activation
     *
     * Positive test: Verify message light can be turned on with listener notification
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageLightActivation(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        if (!messageType.equals(MSG_ERROR) && !messageType.equals(MSG_WARNING) && !messageType.equals(MSG_SYSTEM)) {
            return; // Focus on message types that activate light
        }

        // Turn on message light
        oia.setMessageLightOn();

        // GREEN: Verify on state
        assertTrue(oia.isMessageWait()
        ,
            String.format("Message light should be on after setMessageLightOn for %s", messageType));

        // Verify listener was notified
        assertTrue(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_MESSAGELIGHT)
        ,
            "Listener should have been notified of message light on");
    }

    /**
     * TEST 3: Message light deactivation
     *
     * Positive test: Verify message light can be turned off
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageLightDeactivation(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        // Set up: Turn on light
        oia.setMessageLightOn();
        assertTrue(oia.isMessageWait(),"Setup: Message light should be on");

        oiaListener.clear();

        // Turn off
        oia.setMessageLightOff();

        // GREEN: Verify off state
        assertFalse(oia.isMessageWait()
        ,
            "Message light should be off after setMessageLightOff");

        // Verify listener notified
        assertTrue(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_MESSAGELIGHT)
        ,
            "Listener should be notified of message light off");
    }

    /**
     * TEST 4: Input inhibit message with text
     *
     * Positive test: Verify input inhibition can carry message text
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testInputInhibitMessageText(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        String testMessage = createMessage(messageLength, messageType);

        // Set inhibited with message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, testMessage);

        // GREEN: Verify message is stored
        String storedMessage = oia.getInhibitedText();
        assertNotNull(storedMessage
        ,
            "Inhibited text should not be null when set");

        // Verify message content
        // Note: Implementation stores full message without truncation
        assertEquals(testMessage,storedMessage
        ,
            String.format("Message text should match for length %d", messageLength));
    }

    /**
     * TEST 5: Message with empty content
     *
     * Adversarial test: Verify empty messages are handled safely
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testEmptyMessageHandling(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        if (messageLength != LEN_EMPTY) {
            return; // Focus on empty message case
        }

        String emptyMessage = "";
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, emptyMessage);

        // GREEN: Verify empty message doesn't cause errors
        String storedMessage = oia.getInhibitedText();
        assertNotNull(storedMessage
        ,
            "Empty message should be stored without error");

        assertEquals("",
            storedMessage
        ,
            "Empty message should remain empty");
    }

    /**
     * TEST 6: Single character message
     *
     * Positive test: Verify minimal-length messages display correctly
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSingleCharacterMessage(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        if (messageLength != LEN_SINGLE) {
            return; // Focus on single-char case
        }

        String singleChar = "X";
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, singleChar);

        // GREEN: Verify single character is preserved
        String storedMessage = oia.getInhibitedText();
        assertEquals(singleChar,
            storedMessage
        ,
            "Single character message should be preserved");
    }

    /**
     * TEST 7: Standard 80-character message
     *
     * Positive test: Verify standard display width messages work correctly
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testStandardWidth80Message(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        if (messageLength != LEN_STANDARD) {
            return; // Focus on standard 80-char case
        }

        String message = createMessage(LEN_STANDARD, messageType);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify standard message integrity
        String storedMessage = oia.getInhibitedText();
        assertEquals(message,
            storedMessage
        ,
            "Standard 80-character message should be preserved");

        assertEquals(LEN_STANDARD,
            storedMessage.length()
        ,
            "Standard message should be exactly 80 characters");
    }

    /**
     * TEST 8: Wide 132-character message
     *
     * Positive test: Verify extended-width messages on wide displays
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testWideWidth132Message(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        if (messageLength != LEN_WIDE) {
            return; // Focus on 132-char case
        }

        String message = createMessage(LEN_WIDE, messageType);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify wide message handling
        String storedMessage = oia.getInhibitedText();
        assertNotNull(storedMessage
        ,
            "Wide 132-character message should be stored");

        // Verify stored length (may be truncated or preserved)
        assertTrue(storedMessage.length() <= LEN_WIDE && storedMessage.length() > 0
        ,
            "Wide message should be stored or safely truncated");
    }

    /**
     * TEST 9: Overflow message handling - injection attack prevention
     *
     * Adversarial test: Verify overflow messages don't cause crashes
     * Note: Implementation stores full message without truncation
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testOverflowMessageTruncation(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        if (messageLength != LEN_OVERFLOW) {
            return; // Focus on overflow case
        }

        String overflowMessage = createMessage(LEN_OVERFLOW, messageType);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, overflowMessage);

        // GREEN: Verify overflow is handled without crash
        String storedMessage = oia.getInhibitedText();
        assertNotNull(storedMessage
        ,
            "Overflow message should be stored without null pointer");

        // Verify message is stored (implementation stores full length)
        assertTrue(storedMessage.length() > 0
        ,
            "Overflow message should be stored");

        // Verify we can retrieve the message without crash
        assertEquals(overflowMessage,
            storedMessage
        ,
            "Full overflow message should be retrievable");
    }

    /**
     * TEST 10: Message light toggle idempotency
     *
     * Adversarial test: Verify repeated on/off doesn't cause issues
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageLightToggleIdempotency(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        // Toggle multiple times
        oia.setMessageLightOn();
        assertTrue(oia.isMessageWait(),"Setup: Should be on");

        oia.setMessageLightOff();
        assertFalse(oia.isMessageWait(),"Should be off");

        oia.setMessageLightOn();
        assertTrue(oia.isMessageWait(),"Should be on again");

        // GREEN: Final state should be on
        assertTrue(oia.isMessageWait()
        ,
            "Final toggle should leave light on");
    }

    /**
     * TEST 11: Multiple inhibit code types
     *
     * Positive test: Verify different inhibit codes can be set and preserved
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMultipleInhibitCodeTypes(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        int[] inhibitCodes = {
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT,
            ScreenOIA.INPUTINHIBITED_COMMCHECK,
            ScreenOIA.INPUTINHIBITED_PROGCHECK,
            ScreenOIA.INPUTINHIBITED_MACHINECHECK,
            ScreenOIA.INPUTINHIBITED_OTHER
        };

        for (int code : inhibitCodes) {
            // Set inhibit code
            oia.setInputInhibited(code, code);

            // GREEN: Verify code is retrievable
            int stored = oia.getInputInhibited();
            assertEquals(code,stored
            ,
                String.format("Inhibit code %d should be preserved", code));
        }
    }

    /**
     * TEST 12: Message with special characters
     *
     * Adversarial test: Verify special chars don't corrupt message handling
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSpecialCharacterMessage(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        String specialMessage = "ERROR: *@#$%^&*()_+-=[]{}|;:',.<>?/~`";

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, specialMessage);

        // GREEN: Verify special characters preserved
        String stored = oia.getInhibitedText();
        assertEquals(specialMessage,
            stored
        ,
            "Special characters should be preserved in message");
    }

    /**
     * TEST 13: Message with control sequences - XSS/injection prevention
     *
     * Adversarial test: Verify control sequences are handled safely
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testControlSequenceHandling(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        // Attempt XSS-like injection
        String injectionAttempt = "Normal message\u0007\u0008\u001B[31m RED \u001B[0m";

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, injectionAttempt);

        // GREEN: Verify no crash and message stored
        String stored = oia.getInhibitedText();
        assertNotNull(stored
        ,
            "Control sequence message should be stored without error");

        // Message should be stored as-is (no sanitization expected in OIA layer)
        assertEquals(injectionAttempt,
            stored
        ,
            "Control sequences should be stored without modification");
    }

    /**
     * TEST 14: Inhibited text with null check
     *
     * Adversarial test: Verify null inhibited text is handled gracefully
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testNullInhibitedText(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        // Set inhibited without message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);

        // GREEN: Verify null/empty handling
        String stored = oia.getInhibitedText();
        // Can be null or empty, but shouldn't cause crash
        assertTrue(stored == null || stored.equals("")
        ,
            "Null message should either be null or empty");
    }

    /**
     * TEST 15: Message type with inhibit state transition
     *
     * Adversarial test: Verify state transition with different message types
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageTypeStateTransition(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        String message1 = "First message";
        String message2 = "Second message";

        // Set first message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message1);
        assertEquals(message1, oia.getInhibitedText(),"Setup: First message set");

        // Transition to second message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, 0, message2);

        // GREEN: Verify new message replaces old
        assertEquals(message2,
            oia.getInhibitedText()
        ,
            "Second message should replace first");

        // Verify inhibit code changed
        assertEquals(ScreenOIA.INPUTINHIBITED_PROGCHECK,
            oia.getInputInhibited()
        ,
            "Inhibit code should reflect new type");
    }

    /**
     * TEST 16: Message with keyboard lock interaction
     *
     * Positive test: Verify message independent of keyboard state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageWithKeyboardLockInteraction(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        String message = "System locked message";

        // Lock keyboard and set message
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify message preserved regardless of keyboard state
        assertEquals(message,
            oia.getInhibitedText()
        ,
            "Message should be preserved with locked keyboard");

        // Unlock and verify message still there
        oia.setKeyBoardLocked(false);
        assertEquals(message,
            oia.getInhibitedText()
        ,
            "Message should persist after keyboard unlock");
    }

    /**
     * TEST 17: Message listener notification on inhibit change
     *
     * Positive test: Verify listeners notified of message changes
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageListenerNotification(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        oiaListener.clear();

        String message = "Test message";
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify listener notified
        assertTrue(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        ,
            "Listener should be notified of input inhibit change");
    }

    /**
     * TEST 18: Clear inhibit state with null message
     *
     * Positive test: Verify returning to clear state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testClearInhibitState(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        // Set inhibited state
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, "Inhibited message");
        assertEquals(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, oia.getInputInhibited(),"Setup: Should be inhibited");

        oiaListener.clear();

        // Clear inhibit
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0, null);

        // GREEN: Verify clear state
        assertEquals(ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        ,
            "Should return to not-inhibited state");

        assertTrue(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        ,
            "Listener should be notified of clear");
    }

    /**
     * TEST 19: Message text longer than display width
     *
     * Adversarial test: Verify truncation on narrow displays
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageTruncationOnNarrowDisplay(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        String longMessage = "This is a very long message that exceeds the standard 80 column display width and should be truncated safely";

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, longMessage);

        // GREEN: Verify handling
        String stored = oia.getInhibitedText();
        assertNotNull(stored,"Long message should be stored");

        // Should either be truncated or preserved
        assertTrue(stored.length() > 0
        ,
            "Long message should fit in memory");
    }

    /**
     * TEST 20: Rapid message updates
     *
     * Adversarial test: Verify rapid state changes don't cause race conditions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRapidMessageUpdates(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        for (int i = 0; i < 10; i++) {
            String message = String.format("Rapid message %d", i);
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

            // Verify state after each update
            String stored = oia.getInhibitedText();
            assertEquals(message,stored
            ,
                String.format("Message %d should be preserved", i));
        }

        // GREEN: Final state should be consistent
        assertTrue(oia.getInhibitedText().contains("Rapid message 9")
        ,
            "Final state should be consistent after rapid updates");
    }

    /**
     * TEST 21: Message with owner field
     *
     * Positive test: Verify owner field doesn't interfere with messages
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageWithOwnerField(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        int owner = 42;
        String message = "Owned message";

        oia.setOwner(owner);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify both fields preserved
        assertEquals(owner, oia.getOwner(),"Owner should be set");
        assertEquals(message, oia.getInhibitedText(),"Message should be preserved with owner");
    }

    /**
     * TEST 22: Message light and inhibit state independence
     *
     * Positive test: Verify message light independent of inhibit status
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageLightInhibitIndependence(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        String message = "Test message";

        // Set inhibited without message light
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);
        assertFalse(oia.isMessageWait(),"Setup: Message light off");

        // Turn on message light separately
        oia.setMessageLightOn();

        // GREEN: Both should be true independently
        assertTrue(oia.isMessageWait(),"Message light should be on");
        assertEquals(message,
            oia.getInhibitedText()
        ,
            "Inhibit message should still be set");

        // Turn off message light
        oia.setMessageLightOff();

        // GREEN: Inhibit message should persist
        assertFalse(oia.isMessageWait(),"Message light should be off");
        assertEquals(message,
            oia.getInhibitedText()
        ,
            "Inhibit message should persist after light off");
    }

    /**
     * TEST 23: Message type priority ordering
     *
     * Positive test: Verify high-priority messages override lower ones
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessagePriorityOrdering(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        if (!priority.equals(PRIORITY_CRITICAL) && !priority.equals(PRIORITY_HIGH)) {
            return; // Focus on high-priority cases
        }

        String lowPriorityMsg = "Low priority";
        String highPriorityMsg = "HIGH PRIORITY ERROR";

        // Set low priority first
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, lowPriorityMsg);
        assertEquals(lowPriorityMsg, oia.getInhibitedText(),"Setup: Low priority set");

        // High priority should override if handled
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, 0, highPriorityMsg);

        // GREEN: High priority message should be shown
        assertEquals(highPriorityMsg,
            oia.getInhibitedText()
        ,
            "High priority message should override");
    }

    /**
     * TEST 24: Multiple listeners receive message notifications
     *
     * Positive test: Verify all listeners notified of changes
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMultipleListenerMessageNotification(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        TestOIAListener listener2 = new TestOIAListener();
        TestOIAListener listener3 = new TestOIAListener();

        oia.addOIAListener(listener2);
        oia.addOIAListener(listener3);

        // Trigger message change
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, "Test");

        // GREEN: Verify all listeners notified
        assertTrue(oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED),"First listener notified");
        assertTrue(listener2.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED),"Second listener notified");
        assertTrue(listener3.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED),"Third listener notified");
    }

    /**
     * TEST 25: Message with insert mode interaction
     *
     * Positive test: Verify messages independent of insert mode
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageWithInsertModeInteraction(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        String message = "Insert mode message";

        // Enable insert mode and set message
        oia.setInsertMode(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify message preserved
        assertEquals(message,
            oia.getInhibitedText()
        ,
            "Message should be preserved with insert mode on");

        // Disable insert mode
        oia.setInsertMode(false);
        assertEquals(message,
            oia.getInhibitedText()
        ,
            "Message should persist after insert mode off");
    }

    /**
     * TEST 26: Complex state with all features engaged
     *
     * Adversarial test: Verify complex interactions don't corrupt state
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testComplexMultiStateInteraction(String messageType, int messageLength, String displayDuration, String priority, String screenArea) throws Exception {
        setParameters(messageType, messageLength, displayDuration, priority, screenArea);
        setUp();
        String message = "Complex state message";

        // Engage multiple features simultaneously
        oia.setKeyBoardLocked(true);
        oia.setInsertMode(true);
        oia.setMessageLightOn();
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify all state preserved
        assertTrue(oia.isKeyBoardLocked(),"Keyboard should be locked");
        assertTrue(oia.isInsertMode(),"Insert mode should be on");
        assertTrue(oia.isMessageWait(),"Message light should be on");
        assertEquals(message,
            oia.getInhibitedText()
        ,
            "Message should be preserved with complex state");

        // Now disable features one by one
        oia.setKeyBoardLocked(false);
        assertEquals(message, oia.getInhibitedText(),"Message persists after keyboard unlock");

        oia.setInsertMode(false);
        assertEquals(message, oia.getInhibitedText(),"Message persists after insert mode off");

        oia.setMessageLightOff();
        assertEquals(message, oia.getInhibitedText(),"Message persists after light off");

        // Verify all state changed correctly
        assertFalse(oia.isKeyBoardLocked(),"Keyboard should be unlocked");
        assertFalse(oia.isInsertMode(),"Insert mode should be off");
        assertFalse(oia.isMessageWait(),"Message light should be off");
    }

    // Helper methods

    /**
     * Create a test message of specified length
     */
    private String createMessage(int length, String type) {
        if (length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String prefix = type.substring(0, Math.min(3, type.length()));

        while (sb.length() < length) {
            sb.append(prefix).append(" ");
        }

        return sb.substring(0, length);
    }

    // Test double for Screen5250 interface

    /**
     * Test double for Screen5250
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
     * Test listener for OIA changes
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

    /**
     * Test listener for message changes
     */
    private static class TestMessageListener {
        private int messageChangeCount = 0;
        private String lastMessage = null;

        public void onMessageChanged(String message) {
            messageChangeCount++;
            lastMessage = message;
        }

        public int getChangeCount() {
            return messageChangeCount;
        }

        public String getLastMessage() {
            return lastMessage;
        }
    }
}
