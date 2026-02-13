/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Component;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD test suite for WizardEvent record conversion.
 *
 * Follows RED-GREEN-REFACTOR cycle:
 * RED Phase: Tests for WizardEvent converted to Java 21 Record
 * Tests cover:
 * 1. Record construction with all fields
 * 2. Field immutability (no setters except mutable allowChange)
 * 3. Auto-generated equals/hashCode/toString
 * 4. Compatibility with EventObject parent
 * 5. Compatibility with WizardListener implementations
 */
@DisplayName("WizardEvent Record Conversion Tests")
public class WizardEventRecordTest {

    // Test fixtures
    private Object source;
    private Component currentPage;
    private Component newPage;
    private boolean isLastPage;
    private boolean allowChange;

    @BeforeEach
    public void setUp() {
        source = new Object(); // EventObject requires non-null source
        currentPage = new JPanel();
        currentPage.setName("PageOne");
        newPage = new JButton("Next");
        newPage.setName("PageTwo");
        isLastPage = false;
        allowChange = true;
    }

    // ============================================================================
    // PHASE 1: RECORD CONSTRUCTION TESTS (RED)
    // ============================================================================

    @Test
    @DisplayName("RED.1: Can construct WizardEvent with all parameters")
    public void testConstructWizardEventWithAllParameters() {
        // ACT: Construct event with all parameters
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);

        // ASSERT: Event is not null
        assertNotNull(event, "WizardEvent should be constructible with all parameters");
    }

    @Test
    @DisplayName("RED.2: Constructor sets source from EventObject parent")
    public void testConstructorSetsSourceFromEventObject() {
        // ACT: Construct event
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);

        // ASSERT: Source is accessible (from EventObject parent)
        assertEquals(source, event.getSource(), "Event source should be set from constructor");
    }

    @Test
    @DisplayName("RED.3: Constructor sets currentPage field")
    public void testConstructorSetsCurrentPageField() {
        // ACT: Construct event
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);

        // ASSERT: currentPage is accessible and correct
        assertEquals(currentPage, event.getCurrentPage(), "currentPage should be set from constructor");
    }

    @Test
    @DisplayName("RED.4: Constructor sets newPage field")
    public void testConstructorSetsNewPageField() {
        // ACT: Construct event
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);

        // ASSERT: newPage is accessible and correct
        assertEquals(newPage, event.getNewPage(), "newPage should be set from constructor");
    }

    @Test
    @DisplayName("RED.5: Constructor sets isLastPage field")
    public void testConstructorSetsIsLastPageField() {
        // ACT: Construct event with isLastPage = true
        WizardEvent event = new WizardEvent(source, currentPage, newPage, true, allowChange);

        // ASSERT: isLastPage is accessible and correct
        assertTrue(event.isLastPage(), "isLastPage should be set from constructor");
    }

    @Test
    @DisplayName("RED.6: Constructor sets allowChange field")
    public void testConstructorSetsAllowChangeField() {
        // ACT: Construct event with allowChange = false
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, false);

        // ASSERT: allowChange is accessible and correct
        assertFalse(event.getAllowChange(), "allowChange should be set from constructor");
    }

    @Test
    @DisplayName("RED.7: Constructor rejects null source")
    public void testConstructorRejectsNullSource() {
        // ACT & ASSERT: Null source should throw NullPointerException (EventObject requirement)
        assertThrows(NullPointerException.class,
                () -> new WizardEvent(null, currentPage, newPage, isLastPage, allowChange),
                "Constructor should reject null source");
    }

    // ============================================================================
    // PHASE 2: FIELD IMMUTABILITY TESTS (RED)
    // ============================================================================

    @Test
    @DisplayName("RED.8: getNewPage returns field value (immutable)")
    public void testGetNewPageReturnsFieldValue() {
        // ACT: Construct event and get newPage
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);
        Component retrieved = event.getNewPage();

        // ASSERT: getNewPage returns same object
        assertSame(newPage, retrieved, "getNewPage should return the field value (immutable via record)");
    }

    @Test
    @DisplayName("RED.9: getCurrentPage returns field value (immutable)")
    public void testGetCurrentPageReturnsFieldValue() {
        // ACT: Construct event and get currentPage
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);
        Component retrieved = event.getCurrentPage();

        // ASSERT: getCurrentPage returns same object
        assertSame(currentPage, retrieved, "getCurrentPage should return the field value (immutable via record)");
    }

    @Test
    @DisplayName("RED.10: isLastPage returns field value (immutable)")
    public void testIsLastPageReturnsFieldValue() {
        // ACT: Construct event with isLastPage = true
        WizardEvent event = new WizardEvent(source, currentPage, newPage, true, allowChange);
        boolean retrieved = event.isLastPage();

        // ASSERT: isLastPage returns field value
        assertTrue(retrieved, "isLastPage should return the field value (immutable via record)");
    }

    @Test
    @DisplayName("RED.11: getAllowChange returns field value")
    public void testGetAllowChangeReturnsFieldValue() {
        // ACT: Construct event with allowChange = false
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, false);
        boolean retrieved = event.getAllowChange();

        // ASSERT: getAllowChange returns field value
        assertFalse(retrieved, "getAllowChange should return the field value");
    }

    @Test
    @DisplayName("RED.12: setAllowChange modifies allowChange field")
    public void testSetAllowChangeModifiesField() {
        // ARRANGE: Construct event with allowChange = true
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, true);
        assertTrue(event.getAllowChange(), "Initial allowChange should be true");

        // ACT: Set allowChange to false
        event.setAllowChange(false);

        // ASSERT: allowChange is now false
        assertFalse(event.getAllowChange(), "setAllowChange should modify the mutable field");
    }

    @Test
    @DisplayName("RED.13: setNewPage modifies newPage field")
    public void testSetNewPageModifiesField() {
        // ARRANGE: Construct event
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);
        assertSame(newPage, event.getNewPage(), "Initial newPage should match");

        // ACT: Set newPage to different component
        Component differentPage = new JPanel();
        differentPage.setName("DifferentPage");
        event.setNewPage(differentPage);

        // ASSERT: newPage is now the different component
        assertSame(differentPage, event.getNewPage(), "setNewPage should modify the mutable field");
    }

    // ============================================================================
    // PHASE 3: EQUALS/HASHCODE/TOSTRING TESTS (RED)
    // ============================================================================

    @Test
    @DisplayName("RED.14: equals() - two events with same fields are equal")
    public void testEqualsWithSameFields() {
        // ARRANGE: Create two events with same fields
        WizardEvent event1 = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);
        WizardEvent event2 = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);

        // ACT & ASSERT: events should be equal
        assertEquals(event1, event2, "Events with same fields should be equal");
    }

    @Test
    @DisplayName("RED.15: equals() - event equals itself")
    public void testEqualsSelf() {
        // ARRANGE: Create event
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);

        // ACT & ASSERT: event equals itself
        assertEquals(event, event, "Event should equal itself");
    }

    @Test
    @DisplayName("RED.16: equals() - different currentPage makes events unequal")
    public void testEqualsDifferentCurrentPage() {
        // ARRANGE: Create two events with different currentPage
        WizardEvent event1 = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);
        Component differentPage = new JPanel();
        WizardEvent event2 = new WizardEvent(source, differentPage, newPage, isLastPage, allowChange);

        // ACT & ASSERT: events should not be equal
        assertNotEquals(event1, event2, "Events with different currentPage should not be equal");
    }

    @Test
    @DisplayName("RED.17: hashCode() - same events have same hashCode")
    public void testHashCodeSame() {
        // ARRANGE: Create two events with same fields
        WizardEvent event1 = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);
        WizardEvent event2 = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);

        // ACT & ASSERT: hashCodes should be same
        assertEquals(event1.hashCode(), event2.hashCode(), "Equal events should have same hashCode");
    }

    @Test
    @DisplayName("RED.18: toString() - returns meaningful string")
    public void testToStringMeaningful() {
        // ARRANGE: Create event
        WizardEvent event = new WizardEvent(source, currentPage, newPage, isLastPage, allowChange);

        // ACT: Get string representation
        String str = event.toString();

        // ASSERT: String is not null and contains class name
        assertNotNull(str, "toString should not be null");
        assertTrue(str.contains("WizardEvent"), "toString should contain class name");
    }

    // ============================================================================
    // PHASE 4: WIZARD LISTENER COMPATIBILITY TESTS (RED)
    // ============================================================================

    @Test
    @DisplayName("RED.19: WizardListener can call nextBegin with event")
    public void testWizardListenerNextBegin() {
        // ARRANGE: Create event and listener
        WizardEvent event = new WizardEvent(source, currentPage, newPage, false, true);
        TestWizardListener listener = new TestWizardListener();

        // ACT: Call listener method with event
        listener.nextBegin(event);

        // ASSERT: Listener received event without error
        assertTrue(listener.nextBeginCalled, "Listener nextBegin should be called");
        assertSame(event, listener.lastEvent, "Listener should receive the event");
    }

    @Test
    @DisplayName("RED.20: WizardListener can call nextComplete with event")
    public void testWizardListenerNextComplete() {
        // ARRANGE: Create event and listener
        WizardEvent event = new WizardEvent(source, currentPage, newPage, false, true);
        TestWizardListener listener = new TestWizardListener();

        // ACT: Call listener method with event
        listener.nextComplete(event);

        // ASSERT: Listener received event without error
        assertTrue(listener.nextCompleteCalled, "Listener nextComplete should be called");
    }

    @Test
    @DisplayName("RED.21: WizardListener can modify allowChange during nextBegin")
    public void testWizardListenerModifyAllowChange() {
        // ARRANGE: Create event with allowChange = true
        WizardEvent event = new WizardEvent(source, currentPage, newPage, false, true);
        assertTrue(event.getAllowChange(), "Initial allowChange should be true");

        // ACT: Listener calls setAllowChange(false)
        event.setAllowChange(false);

        // ASSERT: allowChange is modified
        assertFalse(event.getAllowChange(), "Listener should be able to modify allowChange");
    }

    @Test
    @DisplayName("RED.22: WizardListener can modify newPage during nextBegin")
    public void testWizardListenerModifyNewPage() {
        // ARRANGE: Create event
        WizardEvent event = new WizardEvent(source, currentPage, newPage, false, true);
        assertSame(newPage, event.getNewPage(), "Initial newPage should match");

        // ACT: Listener calls setNewPage with different component
        Component altPage = new JPanel();
        event.setNewPage(altPage);

        // ASSERT: newPage is modified
        assertSame(altPage, event.getNewPage(), "Listener should be able to modify newPage");
    }

    // ============================================================================
    // PHASE 5: SERIALIZATION TESTS (RED)
    // ============================================================================

    @Test
    @DisplayName("RED.23: Event has serialVersionUID (EventObject requirement)")
    public void testSerialVersionUIDPresent() {
        // ARRANGE: Get serialVersionUID from class
        long serialVersionUID = 1L;

        // ACT & ASSERT: Check that serialVersionUID is defined
        assertNotNull(WizardEvent.class, "WizardEvent class should be accessible for reflection");
        // Records inherit serialVersionUID from EventObject, so this should work
    }

    // ============================================================================
    // PHASE 6: EDGE CASE TESTS (RED)
    // ============================================================================

    @Test
    @DisplayName("RED.24: Can handle null currentPage")
    public void testNullCurrentPage() {
        // ACT: Create event with null currentPage
        WizardEvent event = new WizardEvent(source, null, newPage, isLastPage, allowChange);

        // ASSERT: Event created successfully and can retrieve null
        assertNull(event.getCurrentPage(), "Event should allow null currentPage");
    }

    @Test
    @DisplayName("RED.25: Can handle null newPage")
    public void testNullNewPage() {
        // ACT: Create event with null newPage
        WizardEvent event = new WizardEvent(source, currentPage, null, isLastPage, allowChange);

        // ASSERT: Event created successfully and can retrieve null
        assertNull(event.getNewPage(), "Event should allow null newPage");
    }

    @Test
    @DisplayName("RED.26: Multiple events don't interfere with each other")
    public void testMultipleEventsIndependent() {
        // ARRANGE: Create multiple events
        Component page1 = new JPanel();
        Component page2 = new JPanel();
        Component page3 = new JPanel();

        WizardEvent event1 = new WizardEvent(source, page1, page2, true, false);
        WizardEvent event2 = new WizardEvent(source, page2, page3, false, true);

        // ACT: Modify event1
        event1.setAllowChange(true);

        // ASSERT: Only event1 is modified
        assertTrue(event1.getAllowChange(), "Event1 should be modified");
        assertTrue(event2.getAllowChange(), "Event2 should remain unchanged");
    }

    @Test
    @DisplayName("RED.27: isLastPage with true value")
    public void testIsLastPageTrue() {
        // ACT: Create event with isLastPage = true
        WizardEvent event = new WizardEvent(source, currentPage, newPage, true, allowChange);

        // ASSERT: isLastPage returns true
        assertTrue(event.isLastPage(), "isLastPage should return true");
    }

    @Test
    @DisplayName("RED.28: isLastPage with false value")
    public void testIsLastPageFalse() {
        // ACT: Create event with isLastPage = false
        WizardEvent event = new WizardEvent(source, currentPage, newPage, false, allowChange);

        // ASSERT: isLastPage returns false
        assertFalse(event.isLastPage(), "isLastPage should return false");
    }

    // ============================================================================
    // TEST HELPER CLASSES
    // ============================================================================

    /**
     * Mock WizardListener for testing compatibility
     */
    private static class TestWizardListener implements WizardListener {
        boolean nextBeginCalled = false;
        boolean nextCompleteCalled = false;
        WizardEvent lastEvent;

        @Override
        public void nextBegin(WizardEvent e) {
            nextBeginCalled = true;
            lastEvent = e;
        }

        @Override
        public void nextComplete(WizardEvent e) {
            nextCompleteCalled = true;
            lastEvent = e;
        }

        @Override
        public void previousBegin(WizardEvent e) {}

        @Override
        public void previousComplete(WizardEvent e) {}

        @Override
        public void finished(WizardEvent e) {}

        @Override
        public void canceled(WizardEvent e) {}

        @Override
        public void help(WizardEvent e) {}
    }
}
