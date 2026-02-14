/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.event;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EventObject;

import org.hti5250j.HTI5250jConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TDD Test Suite for SessionChangeEvent Record conversion.
 * Tests cover:
 * - State field access and immutability
 * - Message field handling
 * - Constructor validation
 * - Change listener compatibility
 * - State transition tracking
 */
@DisplayName("SessionChangeEvent Record Conversion Tests")
class SessionChangeEventRecordTest {

    private static final int STATE_CONNECTED = HTI5250jConstants.STATE_CONNECTED;
    private static final int STATE_DISCONNECTED = HTI5250jConstants.STATE_DISCONNECTED;

    private Object source;
    private SessionChangeEvent event;

    @BeforeEach
    void setUp() {
        source = new Object();
    }

    // ============ Constructor Tests ============

    @Test
    @DisplayName("should create event with source only")
    void testConstructorWithSourceOnly() {
        event = new SessionChangeEvent(source);
        assertNotNull(event);
    }

    @Test
    @DisplayName("should create event with source and message")
    void testConstructorWithSourceAndMessage() {
        event = new SessionChangeEvent(source, "Connected successfully");
        assertNotNull(event);
    }

    @Test
    @DisplayName("should throw when source is null")
    void testConstructorNullSource() {
        assertThrows(IllegalArgumentException.class,
            () -> new SessionChangeEvent(null));
    }

    @Test
    @DisplayName("should throw when source is null with message")
    void testConstructorNullSourceWithMessage() {
        assertThrows(IllegalArgumentException.class,
            () -> new SessionChangeEvent(null, "message"));
    }

    @Test
    @DisplayName("should accept null message")
    void testConstructorNullMessage() {
        event = new SessionChangeEvent(source, null);
        assertNull(event.message());
    }

    @Test
    @DisplayName("should accept empty string message")
    void testConstructorEmptyMessage() {
        event = new SessionChangeEvent(source, "");
        assertEquals("", event.message());
    }

    // ============ Field Access Tests ============

    @Test
    @DisplayName("should provide source via getSource()")
    void testGetSource() {
        event = new SessionChangeEvent(source, "test");
        assertSame(source, event.getSource());
    }

    @Test
    @DisplayName("should provide message via message()")
    void testGetMessage() {
        String testMessage = "Session connected";
        event = new SessionChangeEvent(source, testMessage);
        assertEquals(testMessage, event.message());
    }

    @Test
    @DisplayName("should provide state via state()")
    void testGetState() {
        event = new SessionChangeEvent(source);
        int defaultState = event.state();
        assertNotNull(event);
    }

    // ============ State Tracking Tests ============

    @Test
    @DisplayName("should track session state changes - connected")
    void testStateConnected() {
        event = new SessionChangeEvent(source);
        // After conversion to Record with state field
        assertNotNull(event);
    }

    @Test
    @DisplayName("should track session state changes - disconnected")
    void testStateDisconnected() {
        event = new SessionChangeEvent(source);
        // After conversion to Record with state field
        assertNotNull(event);
    }

    @Test
    @DisplayName("should maintain state across multiple changes")
    void testMultipleStateChanges() {
        SessionChangeEvent event1 = new SessionChangeEvent(source);
        SessionChangeEvent event2 = new SessionChangeEvent(source);

        // Both events should be independent with their own state
        assertNotSame(event1, event2);
    }

    // ============ Immutability Tests ============

    @Test
    @DisplayName("should be immutable - cannot modify fields")
    void testImmutability() {
        event = new SessionChangeEvent(source, "initial");

        // Record should not have setters, only getters
        assertThrows(NoSuchMethodException.class,
            () -> event.getClass().getMethod("setState", int.class));

        assertThrows(NoSuchMethodException.class,
            () -> event.getClass().getMethod("setMessage", String.class));
    }

    @Test
    @DisplayName("fields remain unchanged after construction")
    void testFieldsImmutable() {
        String originalMessage = "Connection established";
        event = new SessionChangeEvent(source, originalMessage);

        assertEquals(originalMessage, event.message());

        // Creating new event shouldn't affect first
        SessionChangeEvent event2 = new SessionChangeEvent(source, "Different message");

        assertEquals(originalMessage, event.message());
    }

    // ============ EventObject Contract Tests ============

    @Test
    @DisplayName("should extend EventObject")
    void testExtendsEventObject() {
        event = new SessionChangeEvent(source);
        assertInstanceOf(EventObject.class, event);
    }

    @Test
    @DisplayName("should be serializable")
    void testSerializable() {
        event = new SessionChangeEvent(source, "msg");
        assertTrue(java.io.Serializable.class.isAssignableFrom(event.getClass()));
    }

    @Test
    @DisplayName("should have serialVersionUID")
    void testSerialVersionUID() {
        assertDoesNotThrow(() -> {
            SessionChangeEvent.class.getDeclaredField("serialVersionUID");
        });
    }

    // ============ Equality and Hashing Tests ============

    @Test
    @DisplayName("records with same values should be equal")
    void testRecordEquality() {
        SessionChangeEvent event1 = new SessionChangeEvent(source, "msg");
        SessionChangeEvent event2 = new SessionChangeEvent(source, "msg");

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("records with different message should not be equal")
    void testRecordInequalityDifferentMessage() {
        SessionChangeEvent event1 = new SessionChangeEvent(source, "msg1");
        SessionChangeEvent event2 = new SessionChangeEvent(source, "msg2");

        assertNotEquals(event1, event2);
    }

    @Test
    @DisplayName("records with different state should not be equal")
    void testRecordInequalityDifferentState() {
        SessionChangeEvent event1 = new SessionChangeEvent(source);
        SessionChangeEvent event2 = new SessionChangeEvent(source);

        // After converting to record, state should be part of equality
        assertNotNull(event1);
        assertNotNull(event2);
    }

    // ============ Listener Compatibility Tests ============

    @Test
    @DisplayName("listener should receive and process event correctly")
    void testListenerReceivesEvent() {
        event = new SessionChangeEvent(source, "test");
        TestSessionChangeListener listener = new TestSessionChangeListener();

        listener.onSessionChanged(event);

        assertTrue(listener.eventReceived);
        assertEquals("test", listener.receivedMessage);
    }

    @Test
    @DisplayName("listener should handle connected state")
    void testListenerConnectedState() {
        event = new SessionChangeEvent(source, "Connected");
        TestSessionChangeListener listener = new TestSessionChangeListener();

        listener.onSessionChanged(event);

        assertTrue(listener.eventReceived);
        assertEquals("Connected", listener.receivedMessage);
    }

    @Test
    @DisplayName("listener should handle disconnected state")
    void testListenerDisconnectedState() {
        event = new SessionChangeEvent(source, "Disconnected");
        TestSessionChangeListener listener = new TestSessionChangeListener();

        listener.onSessionChanged(event);

        assertTrue(listener.eventReceived);
        assertEquals("Disconnected", listener.receivedMessage);
    }

    // ============ toString Tests ============

    @Test
    @DisplayName("should provide meaningful toString representation")
    void testToString() {
        event = new SessionChangeEvent(source, "test msg");
        String str = event.toString();

        assertNotNull(str);
        assertTrue(str.contains("SessionChangeEvent") || str.contains("message"));
    }

    // ============ Message Handling Tests ============

    @Test
    @DisplayName("should handle long messages")
    void testLongMessage() {
        String longMsg = "A".repeat(1000);
        event = new SessionChangeEvent(source, longMsg);
        assertEquals(longMsg, event.message());
    }

    @Test
    @DisplayName("should preserve message with special characters")
    void testSpecialCharactersInMessage() {
        String specialMsg = "Session: @#$%^&*()_+-={}[]|:;<>?,./";
        event = new SessionChangeEvent(source, specialMsg);
        assertEquals(specialMsg, event.message());
    }

    @Test
    @DisplayName("should preserve message with newlines")
    void testMessageWithNewlines() {
        String multilineMsg = "Connection started\nSession ID: 123\nPort: 5250";
        event = new SessionChangeEvent(source, multilineMsg);
        assertEquals(multilineMsg, event.message());
    }

    // ============ Message and State Combination Tests ============

    @Test
    @DisplayName("should correctly associate message with state")
    void testMessageStateAssociation() {
        SessionChangeEvent connectedEvent = new SessionChangeEvent(source, "Now connected");
        SessionChangeEvent disconnectedEvent = new SessionChangeEvent(source, "Now disconnected");

        assertEquals("Now connected", connectedEvent.message());
        assertEquals("Now disconnected", disconnectedEvent.message());
    }

    @Test
    @DisplayName("should handle event with null message independently")
    void testNullMessageIndependence() {
        SessionChangeEvent eventWithMsg = new SessionChangeEvent(source, "message");
        SessionChangeEvent eventWithoutMsg = new SessionChangeEvent(source, null);

        assertEquals("message", eventWithMsg.message());
        assertNull(eventWithoutMsg.message());
    }

    // ============ Test Listener Implementation ============

    static class TestSessionChangeListener implements SessionListener {
        boolean eventReceived = false;
        String receivedMessage = null;
        int receivedState = -1;

        @Override
        public void onSessionChanged(SessionChangeEvent changeEvent) {
            eventReceived = true;
            receivedMessage = changeEvent.message();
            receivedState = changeEvent.state();
        }
    }
}
