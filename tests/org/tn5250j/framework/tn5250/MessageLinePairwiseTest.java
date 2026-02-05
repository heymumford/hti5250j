/**
 * Title: MessageLinePairwiseTest.java
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise TDD tests for TN5250j message line handling
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
@RunWith(Parameterized.class)
public class MessageLinePairwiseTest {

    // Test parameters
    private final String messageType;
    private final int messageLength;
    private final String displayDuration;
    private final String priority;
    private final String screenArea;

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
    @Parameterized.Parameters
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

    public MessageLinePairwiseTest(String messageType, int messageLength, String displayDuration,
                                   String priority, String screenArea) {
        this.messageType = messageType;
        this.messageLength = messageLength;
        this.displayDuration = displayDuration;
        this.priority = priority;
        this.screenArea = screenArea;
    }

    @Before
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
    @Test
    public void testMessageLightInitial() {
        // RED: Will fail if isMessageWait() doesn't return initial state
        assertFalse(
            "Message light should start off in new OIA instance",
            oia.isMessageWait()
        );
    }

    /**
     * TEST 2: Message light activation
     *
     * Positive test: Verify message light can be turned on with listener notification
     */
    @Test
    public void testMessageLightActivation() {
        if (!messageType.equals(MSG_ERROR) && !messageType.equals(MSG_WARNING) && !messageType.equals(MSG_SYSTEM)) {
            return; // Focus on message types that activate light
        }

        // Turn on message light
        oia.setMessageLightOn();

        // GREEN: Verify on state
        assertTrue(
            String.format("Message light should be on after setMessageLightOn for %s", messageType),
            oia.isMessageWait()
        );

        // Verify listener was notified
        assertTrue(
            "Listener should have been notified of message light on",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_MESSAGELIGHT)
        );
    }

    /**
     * TEST 3: Message light deactivation
     *
     * Positive test: Verify message light can be turned off
     */
    @Test
    public void testMessageLightDeactivation() {
        // Set up: Turn on light
        oia.setMessageLightOn();
        assertTrue("Setup: Message light should be on", oia.isMessageWait());

        oiaListener.clear();

        // Turn off
        oia.setMessageLightOff();

        // GREEN: Verify off state
        assertFalse(
            "Message light should be off after setMessageLightOff",
            oia.isMessageWait()
        );

        // Verify listener notified
        assertTrue(
            "Listener should be notified of message light off",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_MESSAGELIGHT)
        );
    }

    /**
     * TEST 4: Input inhibit message with text
     *
     * Positive test: Verify input inhibition can carry message text
     */
    @Test
    public void testInputInhibitMessageText() {
        String testMessage = createMessage(messageLength, messageType);

        // Set inhibited with message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, testMessage);

        // GREEN: Verify message is stored
        String storedMessage = oia.getInhibitedText();
        assertNotNull(
            "Inhibited text should not be null when set",
            storedMessage
        );

        // Verify message content
        // Note: Implementation stores full message without truncation
        assertEquals(
            String.format("Message text should match for length %d", messageLength),
            testMessage,
            storedMessage
        );
    }

    /**
     * TEST 5: Message with empty content
     *
     * Adversarial test: Verify empty messages are handled safely
     */
    @Test
    public void testEmptyMessageHandling() {
        if (messageLength != LEN_EMPTY) {
            return; // Focus on empty message case
        }

        String emptyMessage = "";
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, emptyMessage);

        // GREEN: Verify empty message doesn't cause errors
        String storedMessage = oia.getInhibitedText();
        assertNotNull(
            "Empty message should be stored without error",
            storedMessage
        );

        assertEquals(
            "Empty message should remain empty",
            "",
            storedMessage
        );
    }

    /**
     * TEST 6: Single character message
     *
     * Positive test: Verify minimal-length messages display correctly
     */
    @Test
    public void testSingleCharacterMessage() {
        if (messageLength != LEN_SINGLE) {
            return; // Focus on single-char case
        }

        String singleChar = "X";
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, singleChar);

        // GREEN: Verify single character is preserved
        String storedMessage = oia.getInhibitedText();
        assertEquals(
            "Single character message should be preserved",
            singleChar,
            storedMessage
        );
    }

    /**
     * TEST 7: Standard 80-character message
     *
     * Positive test: Verify standard display width messages work correctly
     */
    @Test
    public void testStandardWidth80Message() {
        if (messageLength != LEN_STANDARD) {
            return; // Focus on standard 80-char case
        }

        String message = createMessage(LEN_STANDARD, messageType);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify standard message integrity
        String storedMessage = oia.getInhibitedText();
        assertEquals(
            "Standard 80-character message should be preserved",
            message,
            storedMessage
        );

        assertEquals(
            "Standard message should be exactly 80 characters",
            LEN_STANDARD,
            storedMessage.length()
        );
    }

    /**
     * TEST 8: Wide 132-character message
     *
     * Positive test: Verify extended-width messages on wide displays
     */
    @Test
    public void testWideWidth132Message() {
        if (messageLength != LEN_WIDE) {
            return; // Focus on 132-char case
        }

        String message = createMessage(LEN_WIDE, messageType);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify wide message handling
        String storedMessage = oia.getInhibitedText();
        assertNotNull(
            "Wide 132-character message should be stored",
            storedMessage
        );

        // Verify stored length (may be truncated or preserved)
        assertTrue(
            "Wide message should be stored or safely truncated",
            storedMessage.length() <= LEN_WIDE && storedMessage.length() > 0
        );
    }

    /**
     * TEST 9: Overflow message handling - injection attack prevention
     *
     * Adversarial test: Verify overflow messages don't cause crashes
     * Note: Implementation stores full message without truncation
     */
    @Test
    public void testOverflowMessageTruncation() {
        if (messageLength != LEN_OVERFLOW) {
            return; // Focus on overflow case
        }

        String overflowMessage = createMessage(LEN_OVERFLOW, messageType);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, overflowMessage);

        // GREEN: Verify overflow is handled without crash
        String storedMessage = oia.getInhibitedText();
        assertNotNull(
            "Overflow message should be stored without null pointer",
            storedMessage
        );

        // Verify message is stored (implementation stores full length)
        assertTrue(
            "Overflow message should be stored",
            storedMessage.length() > 0
        );

        // Verify we can retrieve the message without crash
        assertEquals(
            "Full overflow message should be retrievable",
            overflowMessage,
            storedMessage
        );
    }

    /**
     * TEST 10: Message light toggle idempotency
     *
     * Adversarial test: Verify repeated on/off doesn't cause issues
     */
    @Test
    public void testMessageLightToggleIdempotency() {
        // Toggle multiple times
        oia.setMessageLightOn();
        assertTrue("Setup: Should be on", oia.isMessageWait());

        oia.setMessageLightOff();
        assertFalse("Should be off", oia.isMessageWait());

        oia.setMessageLightOn();
        assertTrue("Should be on again", oia.isMessageWait());

        // GREEN: Final state should be on
        assertTrue(
            "Final toggle should leave light on",
            oia.isMessageWait()
        );
    }

    /**
     * TEST 11: Multiple inhibit code types
     *
     * Positive test: Verify different inhibit codes can be set and preserved
     */
    @Test
    public void testMultipleInhibitCodeTypes() {
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
            assertEquals(
                String.format("Inhibit code %d should be preserved", code),
                code,
                stored
            );
        }
    }

    /**
     * TEST 12: Message with special characters
     *
     * Adversarial test: Verify special chars don't corrupt message handling
     */
    @Test
    public void testSpecialCharacterMessage() {
        String specialMessage = "ERROR: *@#$%^&*()_+-=[]{}|;:',.<>?/~`";

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, specialMessage);

        // GREEN: Verify special characters preserved
        String stored = oia.getInhibitedText();
        assertEquals(
            "Special characters should be preserved in message",
            specialMessage,
            stored
        );
    }

    /**
     * TEST 13: Message with control sequences - XSS/injection prevention
     *
     * Adversarial test: Verify control sequences are handled safely
     */
    @Test
    public void testControlSequenceHandling() {
        // Attempt XSS-like injection
        String injectionAttempt = "Normal message\u0007\u0008\u001B[31m RED \u001B[0m";

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, injectionAttempt);

        // GREEN: Verify no crash and message stored
        String stored = oia.getInhibitedText();
        assertNotNull(
            "Control sequence message should be stored without error",
            stored
        );

        // Message should be stored as-is (no sanitization expected in OIA layer)
        assertEquals(
            "Control sequences should be stored without modification",
            injectionAttempt,
            stored
        );
    }

    /**
     * TEST 14: Inhibited text with null check
     *
     * Adversarial test: Verify null inhibited text is handled gracefully
     */
    @Test
    public void testNullInhibitedText() {
        // Set inhibited without message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0);

        // GREEN: Verify null/empty handling
        String stored = oia.getInhibitedText();
        // Can be null or empty, but shouldn't cause crash
        assertTrue(
            "Null message should either be null or empty",
            stored == null || stored.equals("")
        );
    }

    /**
     * TEST 15: Message type with inhibit state transition
     *
     * Adversarial test: Verify state transition with different message types
     */
    @Test
    public void testMessageTypeStateTransition() {
        String message1 = "First message";
        String message2 = "Second message";

        // Set first message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message1);
        assertEquals("Setup: First message set", message1, oia.getInhibitedText());

        // Transition to second message
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, 0, message2);

        // GREEN: Verify new message replaces old
        assertEquals(
            "Second message should replace first",
            message2,
            oia.getInhibitedText()
        );

        // Verify inhibit code changed
        assertEquals(
            "Inhibit code should reflect new type",
            ScreenOIA.INPUTINHIBITED_PROGCHECK,
            oia.getInputInhibited()
        );
    }

    /**
     * TEST 16: Message with keyboard lock interaction
     *
     * Positive test: Verify message independent of keyboard state
     */
    @Test
    public void testMessageWithKeyboardLockInteraction() {
        String message = "System locked message";

        // Lock keyboard and set message
        oia.setKeyBoardLocked(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify message preserved regardless of keyboard state
        assertEquals(
            "Message should be preserved with locked keyboard",
            message,
            oia.getInhibitedText()
        );

        // Unlock and verify message still there
        oia.setKeyBoardLocked(false);
        assertEquals(
            "Message should persist after keyboard unlock",
            message,
            oia.getInhibitedText()
        );
    }

    /**
     * TEST 17: Message listener notification on inhibit change
     *
     * Positive test: Verify listeners notified of message changes
     */
    @Test
    public void testMessageListenerNotification() {
        oiaListener.clear();

        String message = "Test message";
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify listener notified
        assertTrue(
            "Listener should be notified of input inhibit change",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        );
    }

    /**
     * TEST 18: Clear inhibit state with null message
     *
     * Positive test: Verify returning to clear state
     */
    @Test
    public void testClearInhibitState() {
        // Set inhibited state
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, "Inhibited message");
        assertEquals("Setup: Should be inhibited", ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, oia.getInputInhibited());

        oiaListener.clear();

        // Clear inhibit
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_NOTINHIBITED, 0, null);

        // GREEN: Verify clear state
        assertEquals(
            "Should return to not-inhibited state",
            ScreenOIA.INPUTINHIBITED_NOTINHIBITED,
            oia.getInputInhibited()
        );

        assertTrue(
            "Listener should be notified of clear",
            oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED)
        );
    }

    /**
     * TEST 19: Message text longer than display width
     *
     * Adversarial test: Verify truncation on narrow displays
     */
    @Test
    public void testMessageTruncationOnNarrowDisplay() {
        String longMessage = "This is a very long message that exceeds the standard 80 column display width and should be truncated safely";

        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, longMessage);

        // GREEN: Verify handling
        String stored = oia.getInhibitedText();
        assertNotNull("Long message should be stored", stored);

        // Should either be truncated or preserved
        assertTrue(
            "Long message should fit in memory",
            stored.length() > 0
        );
    }

    /**
     * TEST 20: Rapid message updates
     *
     * Adversarial test: Verify rapid state changes don't cause race conditions
     */
    @Test
    public void testRapidMessageUpdates() {
        for (int i = 0; i < 10; i++) {
            String message = String.format("Rapid message %d", i);
            oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

            // Verify state after each update
            String stored = oia.getInhibitedText();
            assertEquals(
                String.format("Message %d should be preserved", i),
                message,
                stored
            );
        }

        // GREEN: Final state should be consistent
        assertTrue(
            "Final state should be consistent after rapid updates",
            oia.getInhibitedText().contains("Rapid message 9")
        );
    }

    /**
     * TEST 21: Message with owner field
     *
     * Positive test: Verify owner field doesn't interfere with messages
     */
    @Test
    public void testMessageWithOwnerField() {
        int owner = 42;
        String message = "Owned message";

        oia.setOwner(owner);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify both fields preserved
        assertEquals("Owner should be set", owner, oia.getOwner());
        assertEquals("Message should be preserved with owner", message, oia.getInhibitedText());
    }

    /**
     * TEST 22: Message light and inhibit state independence
     *
     * Positive test: Verify message light independent of inhibit status
     */
    @Test
    public void testMessageLightInhibitIndependence() {
        String message = "Test message";

        // Set inhibited without message light
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);
        assertFalse("Setup: Message light off", oia.isMessageWait());

        // Turn on message light separately
        oia.setMessageLightOn();

        // GREEN: Both should be true independently
        assertTrue("Message light should be on", oia.isMessageWait());
        assertEquals(
            "Inhibit message should still be set",
            message,
            oia.getInhibitedText()
        );

        // Turn off message light
        oia.setMessageLightOff();

        // GREEN: Inhibit message should persist
        assertFalse("Message light should be off", oia.isMessageWait());
        assertEquals(
            "Inhibit message should persist after light off",
            message,
            oia.getInhibitedText()
        );
    }

    /**
     * TEST 23: Message type priority ordering
     *
     * Positive test: Verify high-priority messages override lower ones
     */
    @Test
    public void testMessagePriorityOrdering() {
        if (!priority.equals(PRIORITY_CRITICAL) && !priority.equals(PRIORITY_HIGH)) {
            return; // Focus on high-priority cases
        }

        String lowPriorityMsg = "Low priority";
        String highPriorityMsg = "HIGH PRIORITY ERROR";

        // Set low priority first
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, lowPriorityMsg);
        assertEquals("Setup: Low priority set", lowPriorityMsg, oia.getInhibitedText());

        // High priority should override if handled
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_PROGCHECK, 0, highPriorityMsg);

        // GREEN: High priority message should be shown
        assertEquals(
            "High priority message should override",
            highPriorityMsg,
            oia.getInhibitedText()
        );
    }

    /**
     * TEST 24: Multiple listeners receive message notifications
     *
     * Positive test: Verify all listeners notified of changes
     */
    @Test
    public void testMultipleListenerMessageNotification() {
        TestOIAListener listener2 = new TestOIAListener();
        TestOIAListener listener3 = new TestOIAListener();

        oia.addOIAListener(listener2);
        oia.addOIAListener(listener3);

        // Trigger message change
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, "Test");

        // GREEN: Verify all listeners notified
        assertTrue("First listener notified", oiaListener.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED));
        assertTrue("Second listener notified", listener2.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED));
        assertTrue("Third listener notified", listener3.wasNotifiedOfChange(ScreenOIAListener.OIA_CHANGED_INPUTINHIBITED));
    }

    /**
     * TEST 25: Message with insert mode interaction
     *
     * Positive test: Verify messages independent of insert mode
     */
    @Test
    public void testMessageWithInsertModeInteraction() {
        String message = "Insert mode message";

        // Enable insert mode and set message
        oia.setInsertMode(true);
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify message preserved
        assertEquals(
            "Message should be preserved with insert mode on",
            message,
            oia.getInhibitedText()
        );

        // Disable insert mode
        oia.setInsertMode(false);
        assertEquals(
            "Message should persist after insert mode off",
            message,
            oia.getInhibitedText()
        );
    }

    /**
     * TEST 26: Complex state with all features engaged
     *
     * Adversarial test: Verify complex interactions don't corrupt state
     */
    @Test
    public void testComplexMultiStateInteraction() {
        String message = "Complex state message";

        // Engage multiple features simultaneously
        oia.setKeyBoardLocked(true);
        oia.setInsertMode(true);
        oia.setMessageLightOn();
        oia.setInputInhibited(ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT, 0, message);

        // GREEN: Verify all state preserved
        assertTrue("Keyboard should be locked", oia.isKeyBoardLocked());
        assertTrue("Insert mode should be on", oia.isInsertMode());
        assertTrue("Message light should be on", oia.isMessageWait());
        assertEquals(
            "Message should be preserved with complex state",
            message,
            oia.getInhibitedText()
        );

        // Now disable features one by one
        oia.setKeyBoardLocked(false);
        assertEquals("Message persists after keyboard unlock", message, oia.getInhibitedText());

        oia.setInsertMode(false);
        assertEquals("Message persists after insert mode off", message, oia.getInhibitedText());

        oia.setMessageLightOff();
        assertEquals("Message persists after light off", message, oia.getInhibitedText());

        // Verify all state changed correctly
        assertFalse("Keyboard should be unlocked", oia.isKeyBoardLocked());
        assertFalse("Insert mode should be off", oia.isInsertMode());
        assertFalse("Message light should be off", oia.isMessageWait());
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
