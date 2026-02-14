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
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD test suite for BootEvent class.
 *
 * Conversion target: Java 21 Record
 * Current state: Traditional class with mutability
 * Goal state: Immutable Record with validated construction
 *
 * Test dimensions:
 * - Event construction: [no-args, source-only, source+options, source+message]
 * - Data access: [getter, record component]
 * - Listener integration: [single, multiple, listener removal, async fire]
 * - Serialization: [compatibility, integrity]
 * - Edge cases: [null values, empty strings, special characters]
 *
 * POSITIVE TESTS (15):
 * 1. Create BootEvent with source only
 * 2. Create BootEvent with source and boot options
 * 3. Create BootEvent with source and message
 * 4. Get boot options via getter
 * 5. Get message via getter
 * 6. Single listener receives boot event with options
 * 7. Multiple listeners all notified
 * 8. Listener receives event with both message and options
 * 9. Event source preserved in listener callback
 * 10. Event data accessible after construction
 * 11. Empty boot options string valid
 * 12. Empty message string valid
 * 13. Null boot options valid (if intended)
 * 14. Event carries source object
 * 15. Bootstrap sequence complete (listener fires)
 *
 * ADVERSARIAL TESTS (10):
 * 1. Null source throws exception in constructor
 * 2. Setting message after construction (mutability)
 * 3. Setting boot options after construction (mutability)
 * 4. Two events with same data are equal (immutability)
 * 5. Event with special characters in options
 * 6. Event with special characters in message
 * 7. Listener exception during fire
 * 8. Very long boot options string
 * 9. Very long message string
 * 10. Concurrent listener registration during fire
 */
@DisplayName("BootEvent Test Suite (RED-GREEN-REFACTOR)")
public class BootEventTest {

    private Object eventSource;
    private BootEvent bootEvent;

    @BeforeEach
    public void setUp() {
        eventSource = new Object(); // Generic event source
    }

    // ============================================================================
    // RED PHASE TESTS: Baseline behavior before Record conversion
    // ============================================================================

    @Nested
    @DisplayName("RED Phase: Current Implementation Tests")
    class RedPhaseTests {

        /**
         * POSITIVE #1: Create BootEvent with source only
         */
        @Test
        @DisplayName("BootEvent can be created with source only")
        public void testCreateBootEventWithSourceOnly() {
            // ARRANGE & ACT
            BootEvent event = new BootEvent(eventSource);

            // ASSERT
            assertNotNull(event, "Event should be created");
            assertNotNull(event.getSource(), "Event source should be accessible");
            assertEquals(eventSource, event.getSource(), "Source should match");
        }

        /**
         * POSITIVE #2: Create BootEvent with source and boot options
         */
        @Test
        @DisplayName("BootEvent can be created with source and boot options")
        public void testCreateBootEventWithSourceAndBootOptions() {
            // ARRANGE
            String bootOptions = "session=S001 mode=3270";

            // ACT
            BootEvent event = new BootEvent(eventSource, bootOptions);

            // ASSERT
            assertNotNull(event, "Event should be created");
            assertEquals(eventSource, event.getSource(), "Source should match");
            assertEquals(bootOptions, event.getNewSessionOptions(), "Boot options should be set");
        }

        /**
         * POSITIVE #3: Create BootEvent with source and message
         */
        @Test
        @DisplayName("BootEvent message can be set via constructor")
        public void testBootEventMessageCanBeSet() {
            // ARRANGE
            String message = "Bootstrap initiated";

            // ACT
            BootEvent event = new BootEvent(eventSource, "", message);

            // ASSERT
            assertEquals(message, event.getMessage(), "Message should be retrievable");
        }

        /**
         * POSITIVE #4: Get boot options via getter
         */
        @Test
        @DisplayName("getNewSessionOptions returns configured options")
        public void testGetNewSessionOptionsReturnsConfiguredOptions() {
            // ARRANGE
            String options = "host=HOSTSERVER port=23";
            BootEvent event = new BootEvent(eventSource, options);

            // ACT
            String retrieved = event.getNewSessionOptions();

            // ASSERT
            assertEquals(options, retrieved, "Options should be retrievable");
            assertNotNull(retrieved, "Options should not be null");
        }

        /**
         * POSITIVE #5: Get message via getter
         */
        @Test
        @DisplayName("getMessage returns configured message")
        public void testGetMessageReturnsConfiguredMessage() {
            // ARRANGE
            String message = "Connected";
            BootEvent event = new BootEvent(eventSource, "", message);

            // ACT
            String retrieved = event.getMessage();

            // ASSERT
            assertEquals(message, retrieved, "Message should be retrievable");
        }

        /**
         * POSITIVE #6: Single listener receives boot event
         */
        @Test
        @DisplayName("Single BootListener receives boot event")
        public void testSingleBootListenerReceivesEvent() {
            // ARRANGE
            CapturingBootListener listener = new CapturingBootListener();
            TestBootEventManager manager = new TestBootEventManager();
            manager.addBootListener(listener);
            String options = "test_options";

            // ACT
            BootEvent event = new BootEvent(eventSource, options);
            manager.fireBootEvent(event);

            // ASSERT
            assertEquals(1, listener.eventCount, "Listener should receive one event");
            assertNotNull(listener.lastEvent, "Event should be captured");
            assertEquals(options, listener.lastEvent.getNewSessionOptions(), "Options should be preserved");
        }

        /**
         * POSITIVE #7: Multiple listeners all notified
         */
        @Test
        @DisplayName("Multiple BootListeners all receive boot event")
        public void testMultipleBootListenersAllNotified() {
            // ARRANGE
            List<CapturingBootListener> listeners = new ArrayList<>();
            TestBootEventManager manager = new TestBootEventManager();
            for (int i = 0; i < 3; i++) {
                CapturingBootListener listener = new CapturingBootListener();
                listeners.add(listener);
                manager.addBootListener(listener);
            }
            String options = "broadcast_test";

            // ACT
            BootEvent event = new BootEvent(eventSource, options);
            manager.fireBootEvent(event);

            // ASSERT
            for (CapturingBootListener listener : listeners) {
                assertEquals(1, listener.eventCount, "All listeners should receive event");
                assertEquals(options, listener.lastEvent.getNewSessionOptions(), "All should see same options");
            }
        }

        /**
         * POSITIVE #8: Listener receives event with both message and options
         */
        @Test
        @DisplayName("BootListener receives event with message and options")
        public void testBootListenerReceivesEventWithMessageAndOptions() {
            // ARRANGE
            CapturingBootListener listener = new CapturingBootListener();
            TestBootEventManager manager = new TestBootEventManager();
            manager.addBootListener(listener);
            String message = "Bootstrap started";
            String options = "mode=5250";

            // ACT
            BootEvent event = new BootEvent(eventSource, options, message);
            manager.fireBootEvent(event);

            // ASSERT
            assertEquals(message, listener.lastEvent.getMessage(), "Message should be delivered");
            assertEquals(options, listener.lastEvent.getNewSessionOptions(), "Options should be delivered");
        }

        /**
         * POSITIVE #9: Event source preserved in listener callback
         */
        @Test
        @DisplayName("Event source is preserved and accessible in listener")
        public void testEventSourcePreservedInListener() {
            // ARRANGE
            CapturingBootListener listener = new CapturingBootListener();
            TestBootEventManager manager = new TestBootEventManager();
            manager.addBootListener(listener);

            // ACT
            BootEvent event = new BootEvent(eventSource, "options");
            manager.fireBootEvent(event);

            // ASSERT
            assertNotNull(listener.lastEvent.getSource(), "Source should be accessible");
            assertEquals(eventSource, listener.lastEvent.getSource(), "Source should match original");
        }

        /**
         * POSITIVE #10: Event data accessible after construction (immutable)
         */
        @Test
        @DisplayName("Event data remains accessible after construction")
        public void testEventDataAccessibleAfterConstruction() {
            // ARRANGE
            String options = "initial_options";
            String message = "initial_message";

            // ACT
            BootEvent event = new BootEvent(eventSource, options, message);

            // Deprecated setters are no-ops on immutable class
            event.setMessage("ignored");
            event.setNewSessionOptions("ignored");

            // ASSERT - original values preserved (immutable)
            assertEquals(message, event.getMessage(), "Message should remain unchanged (immutable)");
            assertEquals(options, event.getNewSessionOptions(), "Options should remain unchanged (immutable)");
        }

        /**
         * POSITIVE #11: Empty boot options string valid
         */
        @Test
        @DisplayName("BootEvent accepts empty boot options string")
        public void testBootEventAcceptsEmptyBootOptions() {
            // ARRANGE & ACT
            BootEvent event = new BootEvent(eventSource, "");

            // ASSERT
            assertEquals("", event.getNewSessionOptions(), "Empty options should be accepted");
            assertNotNull(event.getNewSessionOptions(), "Empty string should not be null");
        }

        /**
         * POSITIVE #12: Empty message string valid
         */
        @Test
        @DisplayName("BootEvent accepts empty message string")
        public void testBootEventAcceptsEmptyMessage() {
            // ARRANGE
            BootEvent event = new BootEvent(eventSource);
            event.setMessage("");

            // ACT & ASSERT
            assertEquals("", event.getMessage(), "Empty message should be accepted");
            assertNotNull(event.getMessage(), "Empty string should not be null");
        }

        /**
         * POSITIVE #13: Null boot options normalized to empty string
         */
        @Test
        @DisplayName("BootEvent normalizes null boot options to empty string")
        public void testBootEventAllowsNullBootOptions() {
            // ARRANGE & ACT - constructor normalizes null to ""
            BootEvent event = new BootEvent(eventSource, null);

            // ASSERT
            assertEquals("", event.getNewSessionOptions(), "Null options should be normalized to empty string");
        }

        /**
         * POSITIVE #14: Event carries source object
         */
        @Test
        @DisplayName("BootEvent carries EventObject source")
        public void testBootEventCarriesSource() {
            // ARRANGE & ACT
            BootEvent event = new BootEvent(eventSource);

            // ASSERT
            assertTrue(event instanceof java.util.EventObject, "Should be EventObject subclass");
            assertSame(eventSource, event.getSource(), "Source reference should match");
        }

        /**
         * POSITIVE #15: Bootstrap sequence complete
         */
        @Test
        @DisplayName("Complete bootstrap sequence: create event, notify listeners")
        public void testCompleteBootstrapSequence() {
            // ARRANGE
            TestBootEventManager manager = new TestBootEventManager();
            CapturingBootListener listener = new CapturingBootListener();
            manager.addBootListener(listener);

            String hostName = "HOSTSERVER";
            String port = "23";
            String bootOptions = "host=" + hostName + " port=" + port;
            String message = "Connecting to " + hostName;

            // ACT: Simulate BootStrapper flow (immutable - set all at construction)
            BootEvent event = new BootEvent(manager, bootOptions, message);
            manager.fireBootEvent(event);

            // ASSERT
            assertEquals(1, listener.eventCount, "Listener should be notified");
            assertEquals(bootOptions, listener.lastEvent.getNewSessionOptions(), "Options preserved");
            assertEquals(message, listener.lastEvent.getMessage(), "Message preserved");
        }
    }

    // ============================================================================
    // ADVERSARIAL TESTS: Edge cases and error conditions
    // ============================================================================

    @Nested
    @DisplayName("Adversarial Tests: Edge Cases and Errors")
    class AdversarialTests {

        /**
         * ADVERSARIAL #1: Null source throws exception
         */
        @Test
        @DisplayName("Null source in constructor throws exception")
        public void testNullSourceThrowsException() {
            assertThrows(Exception.class, () -> {
                new BootEvent(null);
            }, "Null source should throw exception (EventObject behavior)");
        }

        /**
         * ADVERSARIAL #2: Immutability - setMessage is a no-op
         */
        @Test
        @DisplayName("setMessage is a no-op on immutable BootEvent")
        public void testMessageMutabilityAfterConstruction() {
            // ARRANGE
            BootEvent event = new BootEvent(eventSource, "", "original");

            // ACT - deprecated setter is a no-op
            event.setMessage("modified");

            // ASSERT - original value preserved
            assertEquals("original", event.getMessage(), "Message should be immutable (setter is no-op)");
        }

        /**
         * ADVERSARIAL #3: Immutability - setNewSessionOptions is a no-op
         */
        @Test
        @DisplayName("setNewSessionOptions is a no-op on immutable BootEvent")
        public void testBootOptionsMutabilityAfterConstruction() {
            // ARRANGE
            BootEvent event = new BootEvent(eventSource, "original_options");

            // ACT - deprecated setter is a no-op
            event.setNewSessionOptions("modified_options");

            // ASSERT - original value preserved
            assertEquals("original_options", event.getNewSessionOptions(), "Options should be immutable (setter is no-op)");
        }

        /**
         * ADVERSARIAL #4: Events with same data are equal (value-based equality)
         */
        @Test
        @DisplayName("Two events with same data are equal (value-based equality)")
        public void testEventReferenceEquality() {
            // ARRANGE
            BootEvent event1 = new BootEvent(eventSource, "same_options");
            BootEvent event2 = new BootEvent(eventSource, "same_options");

            // ACT & ASSERT - BootEvent has value-based equals()
            assertEquals(event1, event2, "Events with same source and options should be equal");
        }

        /**
         * ADVERSARIAL #4b: Different bootOptions makes events unequal
         */
        @Test
        @DisplayName("Events with different bootOptions are not equal")
        public void testEventInequalityDifferentOptions() {
            // ARRANGE
            BootEvent event1 = new BootEvent(eventSource, "options_a");
            BootEvent event2 = new BootEvent(eventSource, "options_b");

            // ACT & ASSERT
            assertNotEquals(event1, event2, "Events with different bootOptions should not be equal");
        }

        /**
         * ADVERSARIAL #4c: Different message makes events unequal
         */
        @Test
        @DisplayName("Events with different message are not equal")
        public void testEventInequalityDifferentMessage() {
            // ARRANGE
            BootEvent event1 = new BootEvent(eventSource, "same", "message_a");
            BootEvent event2 = new BootEvent(eventSource, "same", "message_b");

            // ACT & ASSERT
            assertNotEquals(event1, event2, "Events with different message should not be equal");
        }

        /**
         * ADVERSARIAL #5: Event with special characters in options
         */
        @Test
        @DisplayName("BootEvent handles special characters in boot options")
        public void testSpecialCharactersInBootOptions() {
            // ARRANGE
            String specialOptions = "key=value&other=test|special=chars!@#$%";

            // ACT
            BootEvent event = new BootEvent(eventSource, specialOptions);

            // ASSERT
            assertEquals(specialOptions, event.getNewSessionOptions(), "Special characters should be preserved");
        }

        /**
         * ADVERSARIAL #6: Event with special characters in message
         */
        @Test
        @DisplayName("BootEvent handles special characters in message")
        public void testSpecialCharactersInMessage() {
            // ARRANGE
            String specialMessage = "Error: Connection failed! @ 192.168.1.1:23 (timeout)";

            // ACT
            BootEvent event = new BootEvent(eventSource, "", specialMessage);

            // ASSERT
            assertEquals(specialMessage, event.getMessage(), "Special characters should be preserved");
        }

        /**
         * ADVERSARIAL #7: Listener exception during fire
         */
        @Test
        @DisplayName("Exception in listener during fire handling")
        public void testListenerExceptionDuringFire() {
            // ARRANGE
            TestBootEventManager manager = new TestBootEventManager();
            ThrowingBootListener throwingListener = new ThrowingBootListener();
            CapturingBootListener normalListener = new CapturingBootListener();

            manager.addBootListener(throwingListener);
            manager.addBootListener(normalListener);

            // ACT & ASSERT
            BootEvent event = new BootEvent(eventSource, "test");
            assertThrows(RuntimeException.class, () -> {
                manager.fireBootEvent(event);
            }, "Exception in listener should be thrown");
        }

        /**
         * ADVERSARIAL #8: Very long boot options string
         */
        @Test
        @DisplayName("BootEvent handles very long boot options string")
        public void testVeryLongBootOptionsString() {
            // ARRANGE
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("option").append(i).append("=value").append(i).append("&");
            }
            String longOptions = sb.toString();

            // ACT
            BootEvent event = new BootEvent(eventSource, longOptions);

            // ASSERT
            assertEquals(longOptions, event.getNewSessionOptions(), "Long options should be preserved");
            assertTrue(event.getNewSessionOptions().length() > 5000, "String length should be substantial");
        }

        /**
         * ADVERSARIAL #9: Very long message string
         */
        @Test
        @DisplayName("BootEvent handles very long message string")
        public void testVeryLongMessageString() {
            // ARRANGE
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                sb.append("This is line ").append(i).append(" of the bootstrap log.\n");
            }
            String longMessage = sb.toString();

            // ACT
            BootEvent event = new BootEvent(eventSource, "", longMessage);

            // ASSERT
            assertEquals(longMessage, event.getMessage(), "Long message should be preserved");
            assertTrue(event.getMessage().length() > 10000, "Message length should be substantial");
        }

        /**
         * ADVERSARIAL #10: Concurrent listener registration during fire
         */
        @Test
        @DisplayName("Thread-safe listener registration during fire")
        public void testConcurrentListenerRegistrationDuringFire() throws InterruptedException {
            // ARRANGE
            TestBootEventManager manager = new TestBootEventManager();
            AtomicInteger fireCount = new AtomicInteger(0);

            // Self-registering listener (registers new listener during callback)
            BootListener selfRegisteringListener = event -> {
                fireCount.incrementAndGet();
                if (fireCount.get() == 1) {
                    // First fire: register another listener
                    manager.addBootListener(event1 -> fireCount.incrementAndGet());
                }
            };

            manager.addBootListener(selfRegisteringListener);

            // ACT
            BootEvent event = new BootEvent(eventSource, "test");
            manager.fireBootEvent(event);

            // ASSERT
            assertTrue(fireCount.get() >= 1, "Self-registering listener should have fired");
        }
    }

    // ============================================================================
    // GREEN PHASE: Tests that verify Record conversion correctness
    // ============================================================================

    @Nested
    @DisplayName("GREEN Phase: Record Conversion Validation")
    class GreenPhaseTests {

        /**
         * Verify Record components are accessible
         * (These tests validate the Record conversion)
         */
        @Test
        @DisplayName("Record: source component accessible")
        public void testRecordSourceComponent() {
            // ARRANGE & ACT
            BootEvent event = new BootEvent(eventSource, "options", "message");

            // ASSERT
            assertEquals(eventSource, event.source(), "source() should return the source");
        }

        /**
         * Verify Record immutability
         */
        @Test
        @DisplayName("Record: immutable after construction")
        public void testRecordImmutability() {
            // ARRANGE
            BootEvent event = new BootEvent(eventSource, "options", "message");
            Object originalSource = event.source();

            // ACT
            // Cannot do event.source("new"); - Record prevents this

            // ASSERT
            assertEquals(originalSource, event.source(), "Source should remain unchanged");
        }

        /**
         * Verify Record canonical constructor
         */
        @Test
        @DisplayName("Record: canonical constructor validation")
        public void testRecordCanonicalConstructor() {
            // ARRANGE & ACT
            BootEvent event = new BootEvent(eventSource, "test_options", "test_message");

            // ASSERT
            assertEquals(eventSource, event.source(), "Canonical constructor should set source");
            assertEquals("test_options", event.bootOptions(), "Canonical constructor should set bootOptions");
            assertEquals("test_message", event.message(), "Canonical constructor should set message");
        }
    }

    // ============================================================================
    // REFACTOR PHASE: Tests for enhanced Record with validation
    // ============================================================================

    @Nested
    @DisplayName("REFACTOR Phase: Enhanced Record with Validation")
    class RefactorPhaseTests {

        /**
         * Verify compact constructor validation
         */
        @Test
        @DisplayName("Record: compact constructor validates non-null source")
        public void testRecordCompactConstructorValidatesSource() {
            assertThrows(IllegalArgumentException.class, () -> {
                new BootEvent(null, "options", "message");
            }, "Constructor should reject null source per EventObject contract");
        }

        /**
         * Verify compact constructor normalizes empty strings
         */
        @Test
        @DisplayName("Record: compact constructor normalizes empty values")
        public void testRecordCompactConstructorNormalizesEmpty() {
            // ARRANGE & ACT
            BootEvent event = new BootEvent(eventSource, "", "");

            // ASSERT
            assertEquals("", event.bootOptions(), "Empty options should be normalized");
            assertEquals("", event.message(), "Empty message should be normalized");
        }

        /**
         * Verify compact constructor normalizes null to empty
         */
        @Test
        @DisplayName("Record: compact constructor converts null to empty string")
        public void testRecordCompactConstructorConvertsNullToEmpty() {
            // ARRANGE & ACT
            BootEvent event = new BootEvent(eventSource, null, null);

            // ASSERT
            assertNotNull(event.bootOptions(), "null options should be converted to empty string");
            assertNotNull(event.message(), "null message should be converted to empty string");
            assertEquals("", event.bootOptions(), "Null should become empty string");
            assertEquals("", event.message(), "Null should become empty string");
        }

        /**
         * Verify Record equals based on value
         */
        @Test
        @DisplayName("Record: value-based equality")
        public void testRecordValueBasedEquality() {
            // ARRANGE
            BootEvent event1 = new BootEvent(eventSource, "options", "message");
            BootEvent event2 = new BootEvent(eventSource, "options", "message");

            // ACT & ASSERT
            assertEquals(event1, event2, "Records with same values should be equal");
        }

        /**
         * Verify Record hashCode consistency
         */
        @Test
        @DisplayName("Record: consistent hashCode for equal values")
        public void testRecordHashCodeConsistency() {
            // ARRANGE
            BootEvent event1 = new BootEvent(eventSource, "options", "message");
            BootEvent event2 = new BootEvent(eventSource, "options", "message");

            // ACT & ASSERT
            assertEquals(event1.hashCode(), event2.hashCode(), "Equal records should have same hashCode");
        }

        /**
         * Verify Record toString
         */
        @Test
        @DisplayName("Record: useful toString representation")
        public void testRecordToStringRepresentation() {
            // ARRANGE
            BootEvent event = new BootEvent(eventSource, "options", "message");

            // ACT
            String str = event.toString();

            // ASSERT
            assertTrue(str.contains("BootEvent"), "toString should contain class name");
            assertTrue(str.contains("options") || str.contains("message"), "toString should contain field values");
        }
    }

    // ============================================================================
    // INTEGRATION TESTS: Listener integration with Record
    // ============================================================================

    @Nested
    @DisplayName("Integration Tests: Listener Integration with Record")
    class IntegrationTests {

        /**
         * Verify listeners work with Record immutability
         */
        @Test
        @DisplayName("BootListener receives immutable Record event")
        public void testBootListenerReceivesImmutableRecord() {
            // ARRANGE
            TestBootEventManager manager = new TestBootEventManager();
            CapturingBootListener listener = new CapturingBootListener();
            manager.addBootListener(listener);

            // ACT
            BootEvent event = new BootEvent(eventSource, "immutable_options", "immutable_message");
            manager.fireBootEvent(event);

            // ASSERT
            assertNotNull(listener.lastEvent, "Listener should receive event");
            assertEquals("immutable_options", listener.lastEvent.bootOptions(), "Options should be accessible via record component");
            assertEquals("immutable_message", listener.lastEvent.message(), "Message should be accessible via record component");
        }

        /**
         * Verify backward compatibility with getter methods
         */
        @Test
        @DisplayName("Record: backward compatible with existing getter methods")
        public void testBackwardCompatibilityWithGetters() {
            // ARRANGE & ACT
            BootEvent event = new BootEvent(eventSource, "test_options", "test_message");

            // ASSERT
            // Should still support old getter methods for backward compatibility
            // (if implemented as compact constructor record)
            assertEquals("test_options", event.getNewSessionOptions(), "getNewSessionOptions() should work");
            assertEquals("test_message", event.getMessage(), "getMessage() should work");
        }
    }

    // ============================================================================
    // TEST HELPERS
    // ============================================================================

    private static class TestBootEventManager {
        private final List<BootListener> listeners = new CopyOnWriteArrayList<>();

        void addBootListener(BootListener listener) {
            if (listener == null) throw new NullPointerException("Listener cannot be null");
            listeners.add(listener);
        }

        void fireBootEvent(BootEvent event) {
            for (BootListener listener : listeners) {
                listener.bootOptionsReceived(event);
            }
        }
    }

    private static class CapturingBootListener implements BootListener {
        int eventCount = 0;
        BootEvent lastEvent;

        @Override
        public void bootOptionsReceived(BootEvent bootEvent) {
            eventCount++;
            this.lastEvent = bootEvent;
        }
    }

    private static class ThrowingBootListener implements BootListener {
        @Override
        public void bootOptionsReceived(BootEvent bootEvent) {
            throw new RuntimeException("Intentional exception for testing");
        }
    }
}
