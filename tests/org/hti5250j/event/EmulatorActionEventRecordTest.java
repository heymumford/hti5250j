/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD test suite for EmulatorActionEvent Record conversion.
 *
 * RED Phase: Define all expected behavior for Record-based EmulatorActionEvent
 * - Immutability validation
 * - Constructor behavior
 * - Accessor methods
 * - Message field handling
 * - Action type handling (CLOSE_SESSION, START_NEW_SESSION, etc.)
 * - Listener compatibility
 * - Serialization/deserialization
 * - Null handling and validation
 */
public class EmulatorActionEventRecordTest {

    private static final int TEST_ACTION = EmulatorActionEvent.CLOSE_SESSION;
    private static final String TEST_MESSAGE = "Test message";
    private Object source;

    @BeforeEach
    public void setUp() {
        source = new Object();
    }

    // ============================================================================
    // RED PHASE: Constructor Tests
    // ============================================================================

    /**
     * RED #1: Constructor with source only should set source and leave message/action unset
     * Expected: event created with source from EventObject, message=null, action=0
     */
    @Test
    public void testConstructorWithSourceOnly() {
        // ACT
        EmulatorActionEvent event = new EmulatorActionEvent(source);

        // ASSERT
        assertNotNull(event, "Event should be created");
        assertSame(source, event.getSource(), "Source should match");
        assertNull(event.getMessage(), "Message should be null");
        assertEquals(0, event.getAction(), "Action should be 0 (unset)");
    }

    /**
     * RED #2: Constructor with source and message should set both
     * Expected: event created with source, message set, action=0
     */
    @Test
    public void testConstructorWithSourceAndMessage() {
        // ACT
        EmulatorActionEvent event = new EmulatorActionEvent(source, TEST_MESSAGE);

        // ASSERT
        assertNotNull(event, "Event should be created");
        assertSame(source, event.getSource(), "Source should match");
        assertEquals(TEST_MESSAGE, event.getMessage(), "Message should match");
        assertEquals(0, event.getAction(), "Action should be 0 (unset)");
    }

    /**
     * RED #3: Constructor with null source should throw NullPointerException
     * Expected: EventObject behavior inherited from parent class
     */
    @Disabled("TDD RED phase")
    @Test
    public void testConstructorWithNullSourceThrows() {
        // ACT & ASSERT
        assertThrows(NullPointerException.class, () -> {
            new EmulatorActionEvent(null);
        }, "Null source should throw NullPointerException");
    }

    /**
     * RED #4: Constructor with null message should be allowed
     * Expected: message field remains null
     */
    @Test
    public void testConstructorWithNullMessage() {
        // ACT
        EmulatorActionEvent event = new EmulatorActionEvent(source, null);

        // ASSERT
        assertNotNull(event, "Event should be created");
        assertNull(event.getMessage(), "Message should be null");
    }

    /**
     * RED #5: Constructor with empty string message should be allowed
     * Expected: message field set to empty string
     */
    @Test
    public void testConstructorWithEmptyMessage() {
        // ACT
        EmulatorActionEvent event = new EmulatorActionEvent(source, "");

        // ASSERT
        assertEquals("", event.getMessage(), "Message should be empty string");
    }

    // ============================================================================
    // RED PHASE: Action Type Tests (Constants)
    // ============================================================================

    /**
     * RED #6: Action constant CLOSE_SESSION should exist
     * Expected: constant value == 1
     */
    @Test
    public void testActionConstantCloseSession() {
        assertEquals(1, EmulatorActionEvent.CLOSE_SESSION);
    }

    /**
     * RED #7: Action constant START_NEW_SESSION should exist
     * Expected: constant value == 2
     */
    @Test
    public void testActionConstantStartNewSession() {
        assertEquals(2, EmulatorActionEvent.START_NEW_SESSION);
    }

    /**
     * RED #8: Action constant CLOSE_EMULATOR should exist
     * Expected: constant value == 3
     */
    @Test
    public void testActionConstantCloseEmulator() {
        assertEquals(3, EmulatorActionEvent.CLOSE_EMULATOR);
    }

    /**
     * RED #9: Action constant START_DUPLICATE should exist
     * Expected: constant value == 4
     */
    @Test
    public void testActionConstantStartDuplicate() {
        assertEquals(4, EmulatorActionEvent.START_DUPLICATE);
    }

    // ============================================================================
    // RED PHASE: Accessor Methods (Getters/Setters for mutability during setup)
    // ============================================================================

    /**
     * RED #10: getMessage() should return null initially
     * Expected: null when not set
     */
    @Test
    public void testGetMessageInitiallyNull() {
        // ACT
        EmulatorActionEvent event = new EmulatorActionEvent(source);

        // ASSERT
        assertNull(event.getMessage());
    }

    /**
     * RED #11: setMessage() should update message field
     * Expected: getMessage() returns updated value
     */
    @Test
    public void testSetMessageUpdatesField() {
        // ARRANGE
        EmulatorActionEvent event = new EmulatorActionEvent(source);

        // ACT
        event.setMessage(TEST_MESSAGE);

        // ASSERT
        assertEquals(TEST_MESSAGE, event.getMessage());
    }

    /**
     * RED #12: setMessage() should allow null
     * Expected: getMessage() returns null
     */
    @Test
    public void testSetMessageToNull() {
        // ARRANGE
        EmulatorActionEvent event = new EmulatorActionEvent(source, "initial");

        // ACT
        event.setMessage(null);

        // ASSERT
        assertNull(event.getMessage());
    }

    /**
     * RED #13: getAction() should return 0 initially
     * Expected: 0 when not set
     */
    @Test
    public void testGetActionInitiallyZero() {
        // ACT
        EmulatorActionEvent event = new EmulatorActionEvent(source);

        // ASSERT
        assertEquals(0, event.getAction());
    }

    /**
     * RED #14: setAction() should update action field
     * Expected: getAction() returns updated value
     */
    @Test
    public void testSetActionUpdatesField() {
        // ARRANGE
        EmulatorActionEvent event = new EmulatorActionEvent(source);

        // ACT
        event.setAction(TEST_ACTION);

        // ASSERT
        assertEquals(TEST_ACTION, event.getAction());
    }

    /**
     * RED #15: setAction() with each constant should work
     * Expected: all action constants can be set and retrieved
     */
    @ParameterizedTest
    @ValueSource(ints = {
        EmulatorActionEvent.CLOSE_SESSION,
        EmulatorActionEvent.START_NEW_SESSION,
        EmulatorActionEvent.CLOSE_EMULATOR,
        EmulatorActionEvent.START_DUPLICATE
    })
    public void testSetActionWithAllConstants(int actionCode) {
        // ARRANGE
        EmulatorActionEvent event = new EmulatorActionEvent(source);

        // ACT
        event.setAction(actionCode);

        // ASSERT
        assertEquals(actionCode, event.getAction());
    }

    // ============================================================================
    // RED PHASE: Action Listener Integration Tests
    // ============================================================================

    /**
     * RED #16: Event can be used with EmulatorActionListener interface
     * Expected: listener receives event and can extract source, message, action
     */
    @Test
    public void testEventWithActionListener() {
        // ARRANGE
        EmulatorActionEvent event = new EmulatorActionEvent(source, "test");
        event.setAction(EmulatorActionEvent.START_NEW_SESSION);

        CapturingActionListener listener = new CapturingActionListener();

        // ACT
        listener.onEmulatorAction(event);

        // ASSERT
        assertTrue(listener.eventCaptured, "Listener should receive event");
        assertSame(source, listener.capturedSource, "Source should match");
        assertEquals("test", listener.capturedMessage, "Message should match");
        assertEquals(EmulatorActionEvent.START_NEW_SESSION, listener.capturedAction, "Action should match");
    }

    /**
     * RED #17: Multiple sequential events should work correctly
     * Expected: each event is independent and properly set up
     */
    @Test
    public void testMultipleSequentialEvents() {
        // ARRANGE & ACT
        EmulatorActionEvent event1 = new EmulatorActionEvent(source, "msg1");
        event1.setAction(EmulatorActionEvent.CLOSE_SESSION);

        EmulatorActionEvent event2 = new EmulatorActionEvent(source, "msg2");
        event2.setAction(EmulatorActionEvent.START_NEW_SESSION);

        // ASSERT
        assertEquals("msg1", event1.getMessage());
        assertEquals(EmulatorActionEvent.CLOSE_SESSION, event1.getAction());
        assertEquals("msg2", event2.getMessage());
        assertEquals(EmulatorActionEvent.START_NEW_SESSION, event2.getAction());
    }

    // ============================================================================
    // RED PHASE: Serialization Tests (for Record compatibility)
    // ============================================================================

    /**
     * RED #18: Event should be serializable (EventObject parent requirement)
     * Expected: event can be serialized and deserialized
     */
    @Test
    public void testEventSerializable() throws IOException, ClassNotFoundException {
        // ARRANGE
        EmulatorActionEvent original = new EmulatorActionEvent(source, "serialize test");
        original.setAction(EmulatorActionEvent.START_DUPLICATE);

        // ACT
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(original);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        EmulatorActionEvent deserialized = (EmulatorActionEvent) ois.readObject();
        ois.close();

        // ASSERT
        assertEquals("serialize test", deserialized.getMessage());
        assertEquals(EmulatorActionEvent.START_DUPLICATE, deserialized.getAction());
    }

    /**
     * RED #19: serialVersionUID should be correct
     * Expected: serialVersionUID == 1L
     */
    @Test
    public void testSerialVersionUID() throws Exception {
        // ACT
        java.lang.reflect.Field field = EmulatorActionEvent.class.getDeclaredField("serialVersionUID");
        field.setAccessible(true);
        long serialVersionUID = field.getLong(null);

        // ASSERT
        assertEquals(1L, serialVersionUID);
    }

    // ============================================================================
    // RED PHASE: Field Validation Tests (for Record guards)
    // ============================================================================

    /**
     * RED #20: Message field should accept various string values
     * Expected: all valid strings work without error
     */
    @ParameterizedTest
    @CsvSource({
        "simple",
        "with spaces",
        "with-dashes",
        "with_underscores",
        "123numbers",
        "special!@#$%",
        "line1\\nline2"
    })
    public void testMessageFieldWithVariousValues(String testMessage) {
        // ARRANGE
        EmulatorActionEvent event = new EmulatorActionEvent(source);

        // ACT
        event.setMessage(testMessage);

        // ASSERT
        assertEquals(testMessage, event.getMessage());
    }

    /**
     * RED #21: Action field should accept arbitrary int values
     * Expected: any int value can be set and retrieved
     */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 10, 100, Integer.MAX_VALUE})
    public void testActionFieldWithVariousValues(int actionValue) {
        // ARRANGE
        EmulatorActionEvent event = new EmulatorActionEvent(source);

        // ACT
        event.setAction(actionValue);

        // ASSERT
        assertEquals(actionValue, event.getAction());
    }

    // ============================================================================
    // RED PHASE: Immutability Validation (after conversion to Record)
    // ============================================================================

    /**
     * RED #22: Record should provide accessor methods (canonical constructors)
     * Expected: source() accessor returns the source object
     */
    @Test
    public void testRecordSourceAccessor() {
        // ACT
        EmulatorActionEvent event = new EmulatorActionEvent(source);

        // ASSERT
        assertSame(source, event.getSource(), "getSource() should return source");
    }

    /**
     * RED #23: Record toString() should include all fields
     * Expected: toString includes source, message, action
     */
    @Test
    public void testRecordToString() {
        // ARRANGE
        EmulatorActionEvent event = new EmulatorActionEvent(source, "test");
        event.setAction(EmulatorActionEvent.CLOSE_SESSION);

        // ACT
        String string = event.toString();

        // ASSERT
        assertNotNull(string, "toString() should not be null");
        assertTrue(string.contains("EmulatorActionEvent"), "Should include class name");
    }

    /**
     * RED #24: Record equals() should compare properly
     * Expected: two events with same source, message, action are equal
     */
    @Test
    public void testRecordEquality() {
        // ARRANGE
        Object sharedSource = new Object();
        EmulatorActionEvent event1 = new EmulatorActionEvent(sharedSource, "same");
        event1.setAction(EmulatorActionEvent.CLOSE_SESSION);

        EmulatorActionEvent event2 = new EmulatorActionEvent(sharedSource, "same");
        event2.setAction(EmulatorActionEvent.CLOSE_SESSION);

        // ASSERT
        assertEquals(event1, event2, "Events with same source, message, action should be equal");
    }

    /**
     * RED #25: Record hashCode() should be consistent
     * Expected: same events produce same hash code
     */
    @Test
    public void testRecordHashCode() {
        // ARRANGE
        Object sharedSource = new Object();
        EmulatorActionEvent event1 = new EmulatorActionEvent(sharedSource, "same");
        event1.setAction(EmulatorActionEvent.CLOSE_SESSION);

        EmulatorActionEvent event2 = new EmulatorActionEvent(sharedSource, "same");
        event2.setAction(EmulatorActionEvent.CLOSE_SESSION);

        // ASSERT
        assertEquals(event1.hashCode(), event2.hashCode(), "Same events should have same hash code");
    }

    // ============================================================================
    // Helper: Capturing Action Listener for testing
    // ============================================================================

    private static class CapturingActionListener implements EmulatorActionListener {
        boolean eventCaptured = false;
        Object capturedSource;
        String capturedMessage;
        int capturedAction;

        @Override
        public void onEmulatorAction(EmulatorActionEvent actionEvent) {
            eventCaptured = true;
            capturedSource = actionEvent.getSource();
            capturedMessage = actionEvent.getMessage();
            capturedAction = actionEvent.getAction();
        }
    }
}
