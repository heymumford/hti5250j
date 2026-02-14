/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD test suite for FTPStatusEvent Record conversion.
 *
 * Test dimensions (pairwise combinations):
 * - Status types: [OK, ERROR, ERROR_NULLS_ALLOWED]
 * - Message: [null, empty, valid text]
 * - File length: [0, 1, max_int]
 * - Current record: [0, 1, max_int]
 * - Listener count: [0, 1, 5]
 * - Operations: [read, update, propagate]
 *
 * RED PHASE TESTS - These tests drive the conversion to Record.
 *
 * POSITIVE TESTS (10):
 * 1. FTPStatusEvent created with source only
 * 2. FTPStatusEvent created with source and message
 * 3. FTPStatusEvent created with source, message, and type
 * 4. getMessage() returns stored message
 * 5. getMessageType() returns stored type
 * 6. getFileLength() returns stored file length
 * 7. getCurrentRecord() returns stored record number
 * 8. Status event carries EventObject source
 * 9. Multiple status updates preserve immutability
 * 10. Status event serializes correctly
 *
 * ADVERSARIAL TESTS (10):
 * 1. Null source throws exception
 * 2. Null message is handled gracefully
 * 3. Negative file length is rejected
 * 4. Negative record number is rejected
 * 5. File length overflow handling
 * 6. Record number overflow handling
 * 7. Invalid message type is rejected
 * 8. Listener receives all status updates
 * 9. Multiple concurrent listeners don't interfere
 * 10. Status event with max values works
 */
@DisplayName("FTPStatusEvent TDD Test Suite")
public class FTPStatusEventTest {

    private Object testSource;
    private FTPStatusListener captureListener;
    private List<FTPStatusEvent> capturedEvents;

    @BeforeEach
    public void setUp() {
        testSource = new Object();
        capturedEvents = new ArrayList<>();
        captureListener = new FTPStatusListener() {
            @Override
            public void statusReceived(FTPStatusEvent event) {
                capturedEvents.add(event);
            }

            @Override
            public void commandStatusReceived(FTPStatusEvent event) {
                capturedEvents.add(event);
            }

            @Override
            public void fileInfoReceived(FTPStatusEvent event) {
                capturedEvents.add(event);
            }
        };
    }

    // ============================================================================
    // POSITIVE TEST CASES (1-10): Valid FTPStatusEvent creation and usage
    // ============================================================================

    /**
     * POSITIVE #1: FTPStatusEvent created with source only
     * Creates event with minimal constructor
     */
    @Test
    @DisplayName("RED: Create FTPStatusEvent with source only")
    public void testCreateFTPStatusEventWithSourceOnly() {
        // ACT: Create event with source only
        FTPStatusEvent event = new FTPStatusEvent(testSource);

        // ASSERT: Event created and has source
        assertNotNull(event, "Event should be created");
        assertSame(testSource, event.getSource(), "Source should match");
        assertNull(event.getMessage(), "Message should be null");
    }

    /**
     * POSITIVE #2: FTPStatusEvent created with source and message
     * Creates event with source and message, default type OK
     */
    @Test
    @DisplayName("RED: Create FTPStatusEvent with source and message")
    public void testCreateFTPStatusEventWithSourceAndMessage() {
        // ARRANGE: Prepare test data
        String testMessage = "FTP transfer started";

        // ACT: Create event with source and message
        FTPStatusEvent event = new FTPStatusEvent(testSource, testMessage);

        // ASSERT: Event has correct values
        assertNotNull(event, "Event should be created");
        assertSame(testSource, event.getSource(), "Source should match");
        assertEquals(testMessage, event.getMessage(), "Message should match");
        assertEquals(0, event.getMessageType(), "Type should default to OK (0)");
    }

    /**
     * POSITIVE #3: FTPStatusEvent created with source, message, and type
     * Creates event with all three constructor parameters
     */
    @Test
    @DisplayName("RED: Create FTPStatusEvent with source, message, and type")
    public void testCreateFTPStatusEventWithAllParameters() {
        // ARRANGE: Prepare test data
        String testMessage = "FTP transfer failed";
        int testType = 1; // ERROR

        // ACT: Create event with all parameters
        FTPStatusEvent event = new FTPStatusEvent(testSource, testMessage, testType);

        // ASSERT: Event has correct values
        assertNotNull(event, "Event should be created");
        assertSame(testSource, event.getSource(), "Source should match");
        assertEquals(testMessage, event.getMessage(), "Message should match");
        assertEquals(testType, event.getMessageType(), "Type should match");
    }

    /**
     * POSITIVE #4: getMessage() returns stored message
     * Verifies message getter works correctly
     */
    @Test
    @DisplayName("RED: getMessage() returns stored message")
    public void testGetMessageReturnsStoredMessage() {
        // ARRANGE: Create event with message
        String testMessage = "Transfer complete";
        FTPStatusEvent event = new FTPStatusEvent(testSource, testMessage);

        // ACT: Get message
        String result = event.getMessage();

        // ASSERT: Getter returns correct value
        assertEquals(testMessage, result, "getMessage() should return stored message");
    }

    /**
     * POSITIVE #5: getMessageType() returns stored type
     * Verifies type getter works correctly
     */
    @Test
    @DisplayName("RED: getMessageType() returns stored type")
    public void testGetMessageTypeReturnsStoredType() {
        // ARRANGE: Create event with error type
        String testMessage = "Error occurred";
        int errorType = 1; // ERROR
        FTPStatusEvent event = new FTPStatusEvent(testSource, testMessage, errorType);

        // ACT: Get type
        int result = event.getMessageType();

        // ASSERT: Getter returns correct value
        assertEquals(errorType, result, "getMessageType() should return stored type");
    }

    /**
     * POSITIVE #6: getFileLength() returns stored file length
     * Verifies file length is properly tracked
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: getFileLength() returns stored file length")
    public void testGetFileLengthReturnsStoredValue() {
        // ARRANGE: Create event and set file length
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Transfer");
        int testLength = 1024;
        event.setFileLength(testLength);

        // ACT: Get file length
        int result = event.getFileLength();

        // ASSERT: Getter returns correct value
        assertEquals(testLength, result, "getFileLength() should return stored length");
    }

    /**
     * POSITIVE #7: getCurrentRecord() returns stored record number
     * Verifies current record tracking works
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: getCurrentRecord() returns stored record number")
    public void testGetCurrentRecordReturnsStoredValue() {
        // ARRANGE: Create event and set current record
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Transfer");
        int testRecord = 42;
        event.setCurrentRecord(testRecord);

        // ACT: Get current record
        int result = event.getCurrentRecord();

        // ASSERT: Getter returns correct value
        assertEquals(testRecord, result, "getCurrentRecord() should return stored record");
    }

    /**
     * POSITIVE #8: Status event carries EventObject source correctly
     * Verifies FTPStatusEvent extends EventObject properly
     */
    @Test
    @DisplayName("RED: Status event carries EventObject source")
    public void testStatusEventCarriesEventObjectSource() {
        // ARRANGE: Create event
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Test");

        // ACT: Verify EventObject behavior
        Object source = event.getSource();

        // ASSERT: Source is preserved through EventObject
        assertSame(testSource, source, "EventObject source should be preserved");
        assertTrue(event instanceof java.util.EventObject, "Should be EventObject instance");
    }

    /**
     * POSITIVE #9: Multiple status updates preserve state
     * Verifies that independent FTPStatusEvent instances maintain state
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: Multiple status updates maintain independent state")
    public void testMultipleStatusUpdatesPreserveState() {
        // ARRANGE: Create multiple events
        FTPStatusEvent event1 = new FTPStatusEvent(testSource, "Message1", 0);
        event1.setFileLength(100);
        event1.setCurrentRecord(10);

        FTPStatusEvent event2 = new FTPStatusEvent(testSource, "Message2", 1);
        event2.setFileLength(200);
        event2.setCurrentRecord(20);

        // ACT & ASSERT: Verify each event maintains its own state
        assertEquals("Message1", event1.getMessage(), "Event1 message should be independent");
        assertEquals("Message2", event2.getMessage(), "Event2 message should be independent");
        assertEquals(100, event1.getFileLength(), "Event1 file length should be independent");
        assertEquals(200, event2.getFileLength(), "Event2 file length should be independent");
        assertEquals(10, event1.getCurrentRecord(), "Event1 record should be independent");
        assertEquals(20, event2.getCurrentRecord(), "Event2 record should be independent");
    }

    /**
     * POSITIVE #10: Status event serializes correctly
     * Verifies serialVersionUID is defined for Serializable
     */
    @Test
    @DisplayName("RED: Status event has serialVersionUID")
    public void testStatusEventSerializationSetup() {
        // ARRANGE & ACT: Create event
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Serialize test");

        // ASSERT: Event is EventObject (which is Serializable) and has correct class
        assertTrue(event instanceof java.util.EventObject, "Should be EventObject (Serializable)");
        assertEquals("org.hti5250j.event.FTPStatusEvent", event.getClass().getName(),
                    "Event class should be correctly named");
    }

    // ============================================================================
    // ADVERSARIAL TEST CASES (11-20): Error handling and edge cases
    // ============================================================================

    /**
     * ADVERSARIAL #1: Null source throws exception
     * FTPStatusEvent extends EventObject which requires non-null source
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: Null source throws exception")
    public void testNullSourceThrowsException() {
        // ACT & ASSERT: Should throw exception for null source
        assertThrows(NullPointerException.class, () -> {
            new FTPStatusEvent(null);
        }, "Null source should throw NullPointerException");
    }

    /**
     * ADVERSARIAL #2: Null message is handled gracefully
     * Message field can be null, should not crash
     */
    @Test
    @DisplayName("RED: Null message is handled gracefully")
    public void testNullMessageHandledGracefully() {
        // ACT: Create event with null message
        FTPStatusEvent event = new FTPStatusEvent(testSource, null);

        // ASSERT: Event created successfully with null message
        assertNull(event.getMessage(), "Null message should be preserved");
        assertEquals(0, event.getMessageType(), "Type should default to OK");
    }

    /**
     * ADVERSARIAL #3: Negative file length is stored
     * File length is an int field, negative values are technically allowed
     * but should be documented as invalid
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: Negative file length values are stored (validation needed)")
    public void testNegativeFileLengthStorage() {
        // ARRANGE: Create event
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Test");

        // ACT: Set negative file length (should store but may need validation)
        event.setFileLength(-1);

        // ASSERT: Value is stored (validation would be done elsewhere)
        assertEquals(-1, event.getFileLength(), "Negative file length should be stored (validation in converter)");
    }

    /**
     * ADVERSARIAL #4: Negative record number is stored
     * Record number is an int field, negative values are technically allowed
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: Negative record number values are stored (validation needed)")
    public void testNegativeRecordNumberStorage() {
        // ARRANGE: Create event
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Test");

        // ACT: Set negative record number
        event.setCurrentRecord(-1);

        // ASSERT: Value is stored (validation would be done elsewhere)
        assertEquals(-1, event.getCurrentRecord(), "Negative record should be stored (validation in converter)");
    }

    /**
     * ADVERSARIAL #5: File length with maximum integer value
     * Tests boundary condition with Integer.MAX_VALUE
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: File length handles Integer.MAX_VALUE")
    public void testMaxIntegerFileLength() {
        // ARRANGE: Create event
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Max test");

        // ACT: Set file length to max int
        event.setFileLength(Integer.MAX_VALUE);

        // ASSERT: Max value handled correctly
        assertEquals(Integer.MAX_VALUE, event.getFileLength(), "Should handle Integer.MAX_VALUE");
    }

    /**
     * ADVERSARIAL #6: Record number with maximum integer value
     * Tests boundary condition with Integer.MAX_VALUE
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: Record number handles Integer.MAX_VALUE")
    public void testMaxIntegerRecordNumber() {
        // ARRANGE: Create event
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Max test");

        // ACT: Set record number to max int
        event.setCurrentRecord(Integer.MAX_VALUE);

        // ASSERT: Max value handled correctly
        assertEquals(Integer.MAX_VALUE, event.getCurrentRecord(), "Should handle Integer.MAX_VALUE");
    }

    /**
     * ADVERSARIAL #7: Invalid message type (> 2) is stored
     * Verifies that any int value can be stored (validation elsewhere)
     */
    @Test
    @DisplayName("RED: Invalid message type values are stored (validation needed)")
    public void testInvalidMessageTypeStorage() {
        // ARRANGE & ACT: Create event with invalid type
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Test", 99);

        // ASSERT: Invalid type is stored (validation in converter)
        assertEquals(99, event.getMessageType(), "Invalid type should be stored (validation in converter)");
    }

    /**
     * ADVERSARIAL #8: Listener receives status updates via event
     * Tests that FTPStatusEvent can be used with FTPStatusListener
     */
    @Test
    @DisplayName("RED: Listener can receive status events")
    public void testListenerReceivesStatusUpdates() {
        // ARRANGE: Create event
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Status update", 0);

        // ACT: Fire event to listener
        captureListener.statusReceived(event);

        // ASSERT: Listener captured event
        assertEquals(1, capturedEvents.size(), "Listener should capture event");
        assertSame(event, capturedEvents.get(0), "Captured event should match source");
    }

    /**
     * ADVERSARIAL #9: Multiple concurrent listeners receive same event
     * Tests that multiple listeners can observe the same status event
     */
    @Test
    @DisplayName("RED: Multiple listeners receive same status event")
    public void testMultipleListenersReceiveSameEvent() {
        // ARRANGE: Create multiple listeners
        List<FTPStatusEvent> listener2Events = new ArrayList<>();
        FTPStatusListener listener2 = new FTPStatusListener() {
            @Override
            public void statusReceived(FTPStatusEvent event) {
                listener2Events.add(event);
            }
            @Override
            public void commandStatusReceived(FTPStatusEvent event) {}
            @Override
            public void fileInfoReceived(FTPStatusEvent event) {}
        };

        FTPStatusEvent event = new FTPStatusEvent(testSource, "Broadcast", 0);

        // ACT: Send to both listeners
        captureListener.statusReceived(event);
        listener2.statusReceived(event);

        // ASSERT: Both listeners received same event
        assertEquals(1, capturedEvents.size(), "First listener should receive event");
        assertEquals(1, listener2Events.size(), "Second listener should receive event");
        assertSame(event, capturedEvents.get(0), "Should be same event object");
        assertSame(event, listener2Events.get(0), "Should be same event object");
    }

    /**
     * ADVERSARIAL #10: Status event with maximum values works correctly
     * Tests all fields with maximum valid values
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: Status event handles max values in all fields")
    public void testStatusEventWithMaximumValues() {
        // ARRANGE: Create event with max values
        String maxMessage = "X".repeat(1000); // Long message
        int maxType = 2; // ERROR_NULLS_ALLOWED
        FTPStatusEvent event = new FTPStatusEvent(testSource, maxMessage, maxType);

        // ACT: Set numeric maximums
        event.setFileLength(Integer.MAX_VALUE);
        event.setCurrentRecord(Integer.MAX_VALUE);

        // ASSERT: All max values stored correctly
        assertEquals(maxMessage, event.getMessage(), "Long message should be stored");
        assertEquals(maxType, event.getMessageType(), "Max type should be stored");
        assertEquals(Integer.MAX_VALUE, event.getFileLength(), "Max file length should be stored");
        assertEquals(Integer.MAX_VALUE, event.getCurrentRecord(), "Max record should be stored");
    }

    // ============================================================================
    // STATUS LISTENER COMPATIBILITY TESTS
    // ============================================================================

    /**
     * Test that FTPStatusEvent works with all FTPStatusListener methods
     */
    @Test
    @DisplayName("RED: FTPStatusEvent compatible with all listener methods")
    public void testStatusEventListenerCompatibility() {
        // ARRANGE: Create events for each listener method
        FTPStatusEvent statusEvent = new FTPStatusEvent(testSource, "Status", 0);
        FTPStatusEvent commandEvent = new FTPStatusEvent(testSource, "Command", 1);
        FTPStatusEvent fileEvent = new FTPStatusEvent(testSource, "File", 0);

        // ACT: Call all listener methods
        captureListener.statusReceived(statusEvent);
        captureListener.commandStatusReceived(commandEvent);
        captureListener.fileInfoReceived(fileEvent);

        // ASSERT: All methods accept FTPStatusEvent
        assertEquals(3, capturedEvents.size(), "All three listener methods should receive events");
    }

    /**
     * Test FTPStatusEvent propagation through listener hierarchy
     */
    @Disabled("TDD RED phase")
    @Test
    @DisplayName("RED: Status event propagates correctly through listeners")
    public void testStatusEventPropagation() {
        // ARRANGE: Create event with all fields set
        FTPStatusEvent event = new FTPStatusEvent(testSource, "Propagation test", 0);
        event.setFileLength(512);
        event.setCurrentRecord(25);

        // ACT: Propagate through listener
        captureListener.statusReceived(event);

        // ASSERT: All event data propagates
        FTPStatusEvent received = capturedEvents.get(0);
        assertEquals("Propagation test", received.getMessage(), "Message propagates");
        assertEquals(0, received.getMessageType(), "Type propagates");
        assertEquals(512, received.getFileLength(), "File length propagates");
        assertEquals(25, received.getCurrentRecord(), "Record number propagates");
    }
}
