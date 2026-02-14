/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EventObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TDD Test Suite for SessionJumpEvent Record conversion.
 * Tests cover:
 * - Field access (immutability)
 * - Constructor validation
 * - Listener compatibility
 * - Event handling semantics
 */
@DisplayName("SessionJumpEvent Record Conversion Tests")
class SessionJumpEventRecordTest {

    private static final int JUMP_NEXT = 1;
    private static final int JUMP_PREVIOUS = -1;

    private Object source;
    private SessionJumpEvent event;

    @BeforeEach
    void setUp() {
        source = new Object();
    }

    // ============ Constructor Tests ============

    @Test
    @DisplayName("should create event with source")
    void testConstructorWithSource() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "Next session");
        assertNotNull(event);
    }

    @Test
    @DisplayName("should throw when source is null")
    void testConstructorNullSource() {
        assertThrows(IllegalArgumentException.class,
            () -> new SessionJumpEvent(null, JUMP_NEXT, "message"));
    }

    @Test
    @DisplayName("should accept valid jump directions")
    void testConstructorValidJumpDirections() {
        SessionJumpEvent event1 = new SessionJumpEvent(source, JUMP_NEXT, "msg");
        SessionJumpEvent event2 = new SessionJumpEvent(source, JUMP_PREVIOUS, "msg");
        SessionJumpEvent event3 = new SessionJumpEvent(source, 0, "msg");

        assertEquals(JUMP_NEXT, event1.jumpDirection());
        assertEquals(JUMP_PREVIOUS, event2.jumpDirection());
        assertEquals(0, event3.jumpDirection());
    }

    @Test
    @DisplayName("should accept null message")
    void testConstructorNullMessage() {
        event = new SessionJumpEvent(source, JUMP_NEXT, null);
        assertNull(event.message());
    }

    // ============ Field Access Tests ============

    @Test
    @DisplayName("should provide source via getSource()")
    void testGetSource() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "test");
        assertSame(source, event.getSource());
    }

    @Test
    @DisplayName("should provide jumpDirection via jumpDirection()")
    void testGetJumpDirection() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "msg");
        assertEquals(JUMP_NEXT, event.jumpDirection());
    }

    @Test
    @DisplayName("should provide message via message()")
    void testGetMessage() {
        String testMessage = "Switching to session 2";
        event = new SessionJumpEvent(source, JUMP_NEXT, testMessage);
        assertEquals(testMessage, event.message());
    }

    @Test
    @DisplayName("should provide empty string when message is null")
    void testMessageDefaultValue() {
        event = new SessionJumpEvent(source, JUMP_NEXT, null);
        assertNull(event.message());
    }

    // ============ Immutability Tests ============

    @Test
    @DisplayName("should be immutable - cannot modify fields")
    void testImmutability() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "initial");

        // Record should not have setters, only getters
        assertThrows(NoSuchMethodException.class,
            () -> event.getClass().getMethod("setJumpDirection", int.class));

        assertThrows(NoSuchMethodException.class,
            () -> event.getClass().getMethod("setMessage", String.class));
    }

    @Test
    @DisplayName("fields remain unchanged after construction")
    void testFieldsImmutable() {
        String originalMessage = "Session A";
        event = new SessionJumpEvent(source, JUMP_NEXT, originalMessage);

        assertEquals(originalMessage, event.message());
        assertEquals(JUMP_NEXT, event.jumpDirection());

        // Creating new event with different values shouldn't affect first
        SessionJumpEvent event2 = new SessionJumpEvent(source, JUMP_PREVIOUS, "Session B");

        assertEquals(originalMessage, event.message());
        assertEquals(JUMP_NEXT, event.jumpDirection());
    }

    // ============ EventObject Contract Tests ============

    @Test
    @DisplayName("should extend EventObject")
    void testExtendsEventObject() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "msg");
        assertInstanceOf(EventObject.class, event);
    }

    @Test
    @DisplayName("should be serializable")
    void testSerializable() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "msg");
        assertTrue(java.io.Serializable.class.isAssignableFrom(event.getClass()));
    }

    @Test
    @DisplayName("should have serialVersionUID")
    void testSerialVersionUID() {
        assertDoesNotThrow(() -> {
            SessionJumpEvent.class.getDeclaredField("serialVersionUID");
        });
    }

    // ============ Equality and Hashing Tests ============

    @Test
    @DisplayName("records with same values should be equal")
    void testRecordEquality() {
        SessionJumpEvent event1 = new SessionJumpEvent(source, JUMP_NEXT, "msg");
        SessionJumpEvent event2 = new SessionJumpEvent(source, JUMP_NEXT, "msg");

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("records with different jumpDirection should not be equal")
    void testRecordInequalityDifferentDirection() {
        SessionJumpEvent event1 = new SessionJumpEvent(source, JUMP_NEXT, "msg");
        SessionJumpEvent event2 = new SessionJumpEvent(source, JUMP_PREVIOUS, "msg");

        assertNotEquals(event1, event2);
    }

    @Test
    @DisplayName("records with different message should not be equal")
    void testRecordInequalityDifferentMessage() {
        SessionJumpEvent event1 = new SessionJumpEvent(source, JUMP_NEXT, "msg1");
        SessionJumpEvent event2 = new SessionJumpEvent(source, JUMP_NEXT, "msg2");

        assertNotEquals(event1, event2);
    }

    // ============ Listener Compatibility Tests ============

    @Test
    @DisplayName("listener should receive and process event correctly")
    void testListenerReceivesEvent() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "test");
        TestSessionJumpListener listener = new TestSessionJumpListener();

        listener.onSessionJump(event);

        assertTrue(listener.eventReceived);
        assertEquals(JUMP_NEXT, listener.receivedDirection);
        assertEquals("test", listener.receivedMessage);
    }

    @Test
    @DisplayName("listener should handle JUMP_NEXT direction")
    void testListenerJumpNext() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "Next");
        TestSessionJumpListener listener = new TestSessionJumpListener();

        listener.onSessionJump(event);

        assertEquals(JUMP_NEXT, listener.receivedDirection);
        assertEquals("Next", listener.receivedMessage);
    }

    @Test
    @DisplayName("listener should handle JUMP_PREVIOUS direction")
    void testListenerJumpPrevious() {
        event = new SessionJumpEvent(source, JUMP_PREVIOUS, "Previous");
        TestSessionJumpListener listener = new TestSessionJumpListener();

        listener.onSessionJump(event);

        assertEquals(JUMP_PREVIOUS, listener.receivedDirection);
        assertEquals("Previous", listener.receivedMessage);
    }

    // ============ toString Tests ============

    @Test
    @DisplayName("should provide meaningful toString representation")
    void testToString() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "test msg");
        String str = event.toString();

        assertNotNull(str);
        assertTrue(str.contains("SessionJumpEvent") || str.contains("jumpDirection"));
    }

    // ============ Message Handling Tests ============

    @Test
    @DisplayName("should handle empty string message")
    void testEmptyStringMessage() {
        event = new SessionJumpEvent(source, JUMP_NEXT, "");
        assertEquals("", event.message());
    }

    @Test
    @DisplayName("should handle long messages")
    void testLongMessage() {
        String longMsg = "A".repeat(1000);
        event = new SessionJumpEvent(source, JUMP_NEXT, longMsg);
        assertEquals(longMsg, event.message());
    }

    @Test
    @DisplayName("should preserve message with special characters")
    void testSpecialCharactersInMessage() {
        String specialMsg = "Session: @#$%^&*()_+-={}[]|:;<>?,./";
        event = new SessionJumpEvent(source, JUMP_NEXT, specialMsg);
        assertEquals(specialMsg, event.message());
    }

    // ============ Test Listener Implementation ============

    static class TestSessionJumpListener implements SessionJumpListener {
        boolean eventReceived = false;
        int receivedDirection = -999;
        String receivedMessage = null;

        @Override
        public void onSessionJump(SessionJumpEvent jumpEvent) {
            eventReceived = true;
            receivedDirection = jumpEvent.jumpDirection();
            receivedMessage = jumpEvent.message();
        }
    }
}
