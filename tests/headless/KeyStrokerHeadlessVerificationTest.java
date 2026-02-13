/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.tests.headless;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.hti5250j.keyboard.KeyStroker;
import org.hti5250j.interfaces.IKeyEvent;
import org.hti5250j.headless.HeadlessKeyEvent;

/**
 * Verification tests for KeyStroker headless operation.
 *
 * These tests verify that KeyStroker operates correctly in headless mode
 * after the hashCode fix (commit ae3676d) and IKeyEvent overload additions.
 *
 * TDD VERIFICATION phase - Wave 3A Track 1
 */
@DisplayName("KeyStroker Headless Verification Tests")
class KeyStrokerHeadlessVerificationTest {

    @BeforeAll
    static void setHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * Test 1: Verify headless constructor works correctly
     *
     * This test verifies that KeyStroker can be constructed from an IKeyEvent
     * without any Swing/AWT dependencies.
     */
    @Test
    @DisplayName("Headless constructor creates KeyStroker from IKeyEvent")
    void testHeadlessConstructor() {
        IKeyEvent event = new HeadlessKeyEvent(65, false, false, false); // 'A'
        KeyStroker stroker = new KeyStroker(event);

        assertEquals(65, stroker.getKeyCode(), "KeyCode should be 65 ('A')");
        assertFalse(stroker.isShiftDown(), "Shift should not be down");
        assertFalse(stroker.isControlDown(), "Control should not be down");
        assertFalse(stroker.isAltDown(), "Alt should not be down");
    }

    /**
     * Test 2: Verify hashCode uniqueness
     *
     * This test verifies that the bit-shifting hashCode implementation
     * produces unique hashes for different key combinations.
     * This is critical for HashMap lookups in KeyMapper.
     */
    @Test
    @DisplayName("Hash codes are unique for different key combinations")
    void testHashCodeUniqueness() {
        // Case 1: Adjacent key codes with different modifiers
        KeyStroker k1 = new KeyStroker(new HeadlessKeyEvent(10, true, false, false));
        KeyStroker k2 = new KeyStroker(new HeadlessKeyEvent(11, false, false, false));

        assertNotEquals(k1.hashCode(), k2.hashCode(),
            "Different keys should have different hash codes");

        // Case 2: Same key with different modifiers
        KeyStroker k3 = new KeyStroker(new HeadlessKeyEvent(112, false, false, false)); // F1
        KeyStroker k4 = new KeyStroker(new HeadlessKeyEvent(112, true, false, false));  // Shift+F1

        assertNotEquals(k3.hashCode(), k4.hashCode(),
            "Same key with different modifiers should have different hash codes");

        // Case 3: Same key with different locations
        KeyStroker k5 = new KeyStroker(new HeadlessKeyEvent(10, false, false, false, false, 1, (char)0)); // Standard Enter
        KeyStroker k6 = new KeyStroker(new HeadlessKeyEvent(10, false, false, false, false, 4, (char)0)); // Numpad Enter

        assertNotEquals(k5.hashCode(), k6.hashCode(),
            "Same key with different locations should have different hash codes");
    }

    /**
     * Test 3: Verify hashCode consistency
     *
     * This test verifies that hashCode is consistent across multiple calls
     * for the same KeyStroker instance. This is required by the Java contract
     * for hashCode().
     */
    @Test
    @DisplayName("Hash code is consistent across multiple calls")
    void testHashCodeConsistency() {
        IKeyEvent event = new HeadlessKeyEvent(65, true, false, false);
        KeyStroker stroker = new KeyStroker(event);

        int hash1 = stroker.hashCode();
        int hash2 = stroker.hashCode();
        int hash3 = stroker.hashCode();

        assertEquals(hash1, hash2, "First and second hash should be equal");
        assertEquals(hash2, hash3, "Second and third hash should be equal");
    }

    /**
     * Test 4: Verify setAttributes works with IKeyEvent
     *
     * This test verifies that KeyStroker can update its state from an IKeyEvent
     * and that the hashCode is recalculated correctly.
     */
    @Test
    @DisplayName("setAttributes updates KeyStroker from IKeyEvent")
    void testSetAttributesHeadless() {
        KeyStroker stroker = new KeyStroker(0, false, false, false);
        int originalHash = stroker.hashCode();

        IKeyEvent event = new HeadlessKeyEvent(65, true, false, false);
        stroker.setAttributes(event);

        assertEquals(65, stroker.getKeyCode(), "KeyCode should be updated");
        assertTrue(stroker.isShiftDown(), "Shift should be updated");
        assertNotEquals(originalHash, stroker.hashCode(),
            "Hash should be recalculated after setAttributes");
    }

    /**
     * Test 5: Verify equals works with IKeyEvent
     *
     * This test verifies that KeyStroker can correctly compare itself
     * with an IKeyEvent and determine equality.
     */
    @Test
    @DisplayName("equals method works with IKeyEvent")
    void testIsEqualHeadless() {
        IKeyEvent event1 = new HeadlessKeyEvent(65, true, false, false);
        IKeyEvent event2 = new HeadlessKeyEvent(65, true, false, false);
        IKeyEvent event3 = new HeadlessKeyEvent(66, true, false, false);

        KeyStroker stroker = new KeyStroker(event1);

        assertTrue(stroker.equals(event2),
            "Identical key events should be equal");
        assertFalse(stroker.equals(event3),
            "Different key events should not be equal");
    }

    /**
     * Test 6: Verify hash code bit-shifting layout
     *
     * This test demonstrates the bit-shifting layout used in the fix.
     * It verifies that different bit positions are used for different attributes.
     */
    @Test
    @DisplayName("Hash code uses distinct bit positions for attributes")
    void testHashCodeBitLayout() {
        // Test the hash code values to ensure bit positions are distinct

        // Base: keyCode=10, no modifiers, standard location
        KeyStroker base = new KeyStroker(new HeadlessKeyEvent(10, false, false, false, false, 1, (char)0));
        int baseHash = base.hashCode();
        assertEquals(10, baseHash, "Base hash should be 10 (the keyCode)");

        // With shift (bit 16)
        KeyStroker shift = new KeyStroker(new HeadlessKeyEvent(10, true, false, false, false, 1, (char)0));
        int shiftHash = shift.hashCode();
        assertTrue(shiftHash > baseHash, "Adding shift modifier should increase hash");

        // With control (bit 17)
        KeyStroker ctrl = new KeyStroker(new HeadlessKeyEvent(10, false, true, false, false, 1, (char)0));
        int ctrlHash = ctrl.hashCode();
        assertTrue(ctrlHash > baseHash, "Adding control modifier should increase hash");
        assertTrue(ctrlHash != shiftHash, "Shift and Control should produce different hashes");

        // With numpad location (bits 20-23)
        KeyStroker numpad = new KeyStroker(new HeadlessKeyEvent(10, false, false, false, false, 4, (char)0));
        int numpadHash = numpad.hashCode();
        assertTrue(numpadHash > baseHash, "Numpad location should increase hash");
        assertTrue(numpadHash > shiftHash, "Numpad location change should be distinguishable from modifiers");
    }

    /**
     * Test 7: Verify HashMap compatibility
     *
     * This test demonstrates that the fixed hashCode allows correct
     * HashMap operations by storing and retrieving KeyStroker objects.
     */
    @Test
    @DisplayName("KeyStroker works correctly in HashMap")
    void testHashMapCompatibility() {
        java.util.HashMap<KeyStroker, String> map = new java.util.HashMap<>();

        // Create unique key strokes
        KeyStroker k1 = new KeyStroker(new HeadlessKeyEvent(10, false, false, false)); // Enter
        KeyStroker k2 = new KeyStroker(new HeadlessKeyEvent(112, false, false, false)); // F1
        KeyStroker k3 = new KeyStroker(new HeadlessKeyEvent(112, true, false, false));  // Shift+F1

        // Store in HashMap
        map.put(k1, "Enter");
        map.put(k2, "F1");
        map.put(k3, "Shift+F1");

        // Verify retrieval
        assertEquals("Enter", map.get(k1), "Should retrieve Enter");
        assertEquals("F1", map.get(k2), "Should retrieve F1");
        assertEquals("Shift+F1", map.get(k3), "Should retrieve Shift+F1");

        // Verify size
        assertEquals(3, map.size(), "HashMap should contain 3 entries");
    }

    /**
     * Test 8: Verify Enter key mapping (critical test)
     *
     * This is the key test case from the original failure analysis.
     * The hashCode fix was specifically needed to make Enter key (keyCode=10)
     * work correctly in HashMap lookups.
     */
    @Test
    @DisplayName("Enter key (keyCode=10) has unique hash code")
    void testEnterKeyHashCodeUniqueness() {
        KeyStroker enter = new KeyStroker(new HeadlessKeyEvent(10));
        KeyStroker eleven = new KeyStroker(new HeadlessKeyEvent(11));

        // The original bug was that these had the same hash (11):
        // enter: hash = 10 + 1 (shift=true) + 0 + 0 + 0 + 0 = 11
        // eleven: hash = 11 + 0 + 0 + 0 + 0 + 0 = 11

        assertNotEquals(enter.hashCode(), eleven.hashCode(),
            "Enter key and key 11 must have different hash codes");
        assertEquals(10, enter.hashCode(),
            "Enter key (no modifiers, standard location) should hash to 10");
        assertEquals(11, eleven.hashCode(),
            "Key 11 (no modifiers, standard location) should hash to 11");
    }

    /**
     * Test 9: Verify all 4 IKeyEvent overloads work
     *
     * This test verifies that all 4 IKeyEvent-based methods added in the
     * Green phase work correctly.
     */
    @Test
    @DisplayName("All IKeyEvent overloads are functional")
    void testAllIKeyEventOverloads() {
        IKeyEvent event1 = new HeadlessKeyEvent(65);
        IKeyEvent event2 = new HeadlessKeyEvent(65, true, false, false);
        IKeyEvent event3 = new HeadlessKeyEvent(65, true, false, false, false, 1, 'A');

        // Overload 1: Constructor(IKeyEvent)
        KeyStroker stroker1 = new KeyStroker(event1);
        assertEquals(65, stroker1.getKeyCode());
        assertFalse(stroker1.isShiftDown());

        // Overload 2: Constructor would need full signature
        // Already covered in previous tests

        // Overload 3: setAttributes(IKeyEvent)
        stroker1.setAttributes(event2);
        assertEquals(65, stroker1.getKeyCode());
        assertTrue(stroker1.isShiftDown());

        // Overload 4: setAttributes(IKeyEvent, boolean) for AltGr
        stroker1.setAttributes(event1, true);
        assertTrue(stroker1.isAltGrDown(), "AltGraph should be set");

        // Overload 5: equals(IKeyEvent)
        KeyStroker stroker2 = new KeyStroker(event1);
        assertTrue(stroker1.equals(event1), "Should equal the event");

        // Overload 6: equals(IKeyEvent, boolean)
        assertTrue(stroker1.equals(event2, true), "Should equal with explicit AltGr");
    }

    /**
     * Test 10: Verify headless operation independence
     *
     * This test demonstrates that headless KeyStroker operation
     * is completely independent of Swing/AWT.
     */
    @Test
    @DisplayName("KeyStroker headless operation is Swing/AWT independent")
    void testHeadlessIndependence() {
        // This should not throw any exceptions related to Swing/AWT
        assertDoesNotThrow(() -> {
            for (int keyCode = 0; keyCode < 256; keyCode++) {
                IKeyEvent event = new HeadlessKeyEvent(keyCode);
                KeyStroker stroker = new KeyStroker(event);
                stroker.hashCode();
                stroker.toString();
            }
        }, "Creating 256 KeyStrokers should not require Swing/AWT");
    }
}
