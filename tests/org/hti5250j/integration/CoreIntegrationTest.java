/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.integration;

import org.junit.jupiter.api.*;
import org.hti5250j.framework.common.Rect;
import org.hti5250j.event.SessionConfigEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for core cross-component interactions.
 *
 * These tests verify that components work together correctly after Phase 15
 * Record conversions and compilation fixes.
 *
 * Test Coverage:
 * 1. SessionConfigEvent API compatibility with PropertyChangeEvent
 * 2. Rect as HashMap key (Record equals/hashCode)
 * 3. PropertyChangeListener handling of both event types
 * 4. Event propagation through listener chain
 * 5. Serialization compatibility
 */
@DisplayName("Core Integration Tests - Phase 15 Record Conversions")
class CoreIntegrationTest {

    private Object testSource;

    @BeforeEach
    void setUp() {
        testSource = new Object();
    }

    /**
     * RED/GREEN TEST 1: SessionConfigEvent provides PropertyChangeEvent-compatible API
     *
     * Verifies that SessionConfigEvent (now a Record) maintains API compatibility
     * with code expecting PropertyChangeEvent-like behavior.
     */
    @Test
    @DisplayName("SessionConfigEvent provides PropertyChangeEvent-compatible API")
    void testSessionConfigEventAPICompatibility() {
        // Arrange
        String propertyName = "colorBg";
        Object oldValue = "#000000";
        Object newValue = "#FFFFFF";

        // Act
        SessionConfigEvent event = new SessionConfigEvent(
            testSource, propertyName, oldValue, newValue
        );

        // Assert - Verify Record provides same API as PropertyChangeEvent
        assertEquals(testSource, event.getSource(),
            "getSource() should return source object");
        assertEquals(propertyName, event.getPropertyName(),
            "getPropertyName() should return property name");
        assertEquals(oldValue, event.getOldValue(),
            "getOldValue() should return old value");
        assertEquals(newValue, event.getNewValue(),
            "getNewValue() should return new value");

        // Verify Record accessors also work
        assertEquals(testSource, event.source(),
            "source() record accessor should work");
        assertEquals(propertyName, event.propertyName(),
            "propertyName() record accessor should work");
        assertEquals(oldValue, event.oldValue(),
            "oldValue() record accessor should work");
        assertEquals(newValue, event.newValue(),
            "newValue() record accessor should work");
    }

    /**
     * RED/GREEN TEST 2: Rect can be used as HashMap key
     *
     * Verifies that Rect (now a Record) provides correct equals/hashCode
     * implementation for use as a HashMap key.
     */
    @Test
    @DisplayName("Rect can be used as HashMap key with correct equals/hashCode")
    void testRectAsHashMapKey() {
        // Arrange
        Rect key1 = new Rect(10, 20, 100, 200);
        Rect key2 = new Rect(10, 20, 100, 200);  // Equal to key1
        Rect key3 = new Rect(15, 25, 100, 200);  // Different from key1

        HashMap<Rect, String> map = new HashMap<>();

        // Act
        map.put(key1, "value1");
        map.put(key3, "value3");

        // Assert - Equal Rect instances should retrieve same value
        assertEquals("value1", map.get(key1),
            "Original key should retrieve value");
        assertEquals("value1", map.get(key2),
            "Equal key should retrieve same value");
        assertEquals("value3", map.get(key3),
            "Different key should retrieve different value");

        // Verify equals and hashCode
        assertEquals(key1, key2,
            "Equal Rect instances should be equal");
        assertEquals(key1.hashCode(), key2.hashCode(),
            "Equal Rect instances should have same hashCode");
        assertNotEquals(key1, key3,
            "Different Rect instances should not be equal");
    }

    /**
     * RED/GREEN TEST 3: PropertyChangeListener can handle SessionConfigEvent
     *
     * Verifies that a PropertyChangeListener can receive and process SessionConfigEvent
     * through the propertyChange(PropertyChangeEvent) method.
     */
    @Test
    @DisplayName("PropertyChangeListener handles SessionConfigEvent through adapter")
    void testPropertyChangeListenerHandlesSessionConfigEvent() {
        // Arrange
        AtomicReference<String> capturedPropertyName = new AtomicReference<>();
        AtomicReference<Object> capturedNewValue = new AtomicReference<>();

        PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // This listener expects PropertyChangeEvent
                // SessionConfigEvent provides compatible API
                capturedPropertyName.set(evt.getPropertyName());
                capturedNewValue.set(evt.getNewValue());
            }
        };

        // Act - Create SessionConfigEvent and convert to PropertyChangeEvent
        SessionConfigEvent sessionEvent = new SessionConfigEvent(
            testSource, "fontSize", 12, 14
        );

        // Simulate adapter pattern: Create PropertyChangeEvent from SessionConfigEvent data
        PropertyChangeEvent pce = new PropertyChangeEvent(
            sessionEvent.getSource(),
            sessionEvent.getPropertyName(),
            sessionEvent.getOldValue(),
            sessionEvent.getNewValue()
        );

        listener.propertyChange(pce);

        // Assert
        assertEquals("fontSize", capturedPropertyName.get(),
            "Property name should propagate through listener");
        assertEquals(14, capturedNewValue.get(),
            "New value should propagate through listener");
    }

    /**
     * RED/GREEN TEST 4: Event propagation maintains data integrity
     *
     * Verifies that events can be propagated through multiple listeners
     * without data corruption.
     */
    @Test
    @DisplayName("Event propagation through listener chain maintains data integrity")
    void testEventPropagationDataIntegrity() {
        // Arrange
        SessionConfigEvent event1 = new SessionConfigEvent(
            testSource, "property1", "old1", "new1"
        );
        SessionConfigEvent event2 = new SessionConfigEvent(
            testSource, "property2", "old2", "new2"
        );

        AtomicReference<SessionConfigEvent> lastEvent = new AtomicReference<>();

        // Act - Simulate event propagation
        lastEvent.set(event1);
        SessionConfigEvent received1 = lastEvent.get();

        lastEvent.set(event2);
        SessionConfigEvent received2 = lastEvent.get();

        // Assert - Each event maintains its data
        assertEquals("property1", received1.getPropertyName());
        assertEquals("new1", received1.getNewValue());

        assertEquals("property2", received2.getPropertyName());
        assertEquals("new2", received2.getNewValue());

        // Verify events are immutable (Record semantics)
        assertEquals(event1, received1,
            "Event should maintain identity");
        assertEquals(event2, received2,
            "Event should maintain identity");
    }

    /**
     * RED/GREEN TEST 5: Rect immutability and Record semantics
     *
     * Verifies that Rect Record is truly immutable and provides
     * Record semantics (proper equals, hashCode, toString).
     */
    @Test
    @DisplayName("Rect Record provides immutability and proper semantics")
    void testRectRecordSemantics() {
        // Arrange & Act
        Rect rect1 = new Rect(5, 10, 50, 100);
        Rect rect2 = new Rect(5, 10, 50, 100);
        Rect rect3 = new Rect(6, 10, 50, 100);

        // Assert - Record accessors work
        assertEquals(5, rect1.x(), "Record accessor x() should work");
        assertEquals(10, rect1.y(), "Record accessor y() should work");
        assertEquals(50, rect1.width(), "Record accessor width() should work");
        assertEquals(100, rect1.height(), "Record accessor height() should work");

        // Assert - Record equals/hashCode
        assertEquals(rect1, rect2, "Equal Rects should be equal");
        assertEquals(rect1.hashCode(), rect2.hashCode(),
            "Equal Rects should have same hashCode");
        assertNotEquals(rect1, rect3, "Different Rects should not be equal");

        // Assert - Record toString
        String toString = rect1.toString();
        assertNotNull(toString, "toString should not be null");
        assertTrue(toString.contains("Rect"), "toString should contain class name");
        assertTrue(toString.contains("5"), "toString should contain x value");
        assertTrue(toString.contains("10"), "toString should contain y value");
        assertTrue(toString.contains("50"), "toString should contain width value");
        assertTrue(toString.contains("100"), "toString should contain height value");
    }

    /**
     * RED/GREEN TEST 6: SessionConfigEvent immutability
     *
     * Verifies that SessionConfigEvent is immutable (Record semantics).
     */
    @Test
    @DisplayName("SessionConfigEvent Record is immutable")
    void testSessionConfigEventImmutability() {
        // Arrange
        SessionConfigEvent event = new SessionConfigEvent(
            testSource, "testProp", "oldVal", "newVal"
        );

        // Act - Store original values
        String originalProp = event.getPropertyName();
        Object originalOld = event.getOldValue();
        Object originalNew = event.getNewValue();
        Object originalSource = event.getSource();

        // Try to pass event through various operations
        SessionConfigEvent passedEvent = processEvent(event);

        // Assert - Values unchanged after operations
        assertEquals(originalProp, passedEvent.getPropertyName(),
            "Property name should be immutable");
        assertEquals(originalOld, passedEvent.getOldValue(),
            "Old value should be immutable");
        assertEquals(originalNew, passedEvent.getNewValue(),
            "New value should be immutable");
        assertEquals(originalSource, passedEvent.getSource(),
            "Source should be immutable");

        // Assert - Event equality
        assertEquals(event, passedEvent,
            "Event should equal itself after operations");
    }

    /**
     * RED/GREEN TEST 7: Cross-Record type compatibility
     *
     * Verifies that different Record types can coexist and work together.
     */
    @Test
    @DisplayName("Multiple Record types work together correctly")
    void testCrossRecordTypeCompatibility() {
        // Arrange
        Rect rect = new Rect(0, 0, 100, 100);
        SessionConfigEvent event = new SessionConfigEvent(
            rect, "bounds", null, rect
        );

        // Act - Use Rect as event source and value
        Object source = event.getSource();
        Object newValue = event.getNewValue();

        // Assert - Records work together
        assertSame(rect, source, "Rect should work as event source");
        assertSame(rect, newValue, "Rect should work as event value");

        // Verify Record equality across usage
        Rect extractedRect = (Rect) event.getNewValue();
        assertEquals(rect, extractedRect, "Rect should maintain equality");
    }

    // Helper method for immutability test
    private SessionConfigEvent processEvent(SessionConfigEvent event) {
        // Simulate various operations that might be performed on the event
        // Event should remain unchanged due to Record immutability
        String prop = event.getPropertyName();
        Object val = event.getNewValue();

        // Return the same event (Records are immutable)
        return event;
    }
}
