/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.tests.headless;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.hti5250j.keyboard.KeyMapper;
import org.hti5250j.keyboard.KeyStroker;
import org.hti5250j.interfaces.IKeyEvent;
import org.hti5250j.headless.HeadlessKeyEvent;

/**
 * TDD Phase 2 (GREEN): Tests for KeyMapper headless operation.
 *
 * These tests verify that KeyMapper can operate without Swing/AWT dependencies.
 */
@DisplayName("KeyMapper Headless Mode Tests")
class KeyMapperHeadlessTest {

    @BeforeAll
    static void setHeadlessMode() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() {
        KeyMapper.init();
    }

    @Test
    @DisplayName("KeyMapper initializes in headless mode without Swing")
    void testKeyMapperInitHeadless() {
        assertDoesNotThrow(() -> {
            KeyMapper.init();
        }, "KeyMapper.init() should work in headless mode");
    }

    @Test
    @DisplayName("KeyMapper can map headless key events (IKeyEvent)")
    void testMapHeadlessKeyEvent() {
        assertDoesNotThrow(() -> {
            IKeyEvent keyEvent = new HeadlessKeyEvent(10); // Enter key (keyCode 10)
            String mnemonic = KeyMapper.getKeyStrokeMnemonic(keyEvent);

            assertNotNull(mnemonic, "Should map headless key event to mnemonic");
            assertEquals("[enter]", mnemonic, "Enter key should map to [enter]");
        }, "KeyMapper should handle IKeyEvent in headless mode");
    }

    @Test
    @DisplayName("KeyMapper can check if headless key stroke is defined")
    void testIsKeyStrokeDefinedHeadless() {
        assertDoesNotThrow(() -> {
            IKeyEvent keyEvent = new HeadlessKeyEvent(10, false, false, false);
            boolean isDefined = KeyMapper.isKeyStrokeDefined(keyEvent);

            assertTrue(isDefined, "Enter key should be defined");
        }, "KeyMapper should check IKeyEvent key strokes");
    }

    @Test
    @DisplayName("KeyMapper can get keystroke text from headless event")
    void testGetKeyStrokeTextHeadless() {
        assertDoesNotThrow(() -> {
            IKeyEvent keyEvent = new HeadlessKeyEvent(9); // Tab key (keyCode 9)
            String text = KeyMapper.getKeyStrokeText(keyEvent);

            assertNotNull(text, "Should get text for Tab key");
            assertEquals("[tab]", text, "Tab key should map to [tab]");
        }, "KeyMapper should get text from IKeyEvent");
    }

    @Test
    @DisplayName("KeyMapper can set keystrokes using headless events")
    void testSetKeyStrokeHeadless() {
        assertDoesNotThrow(() -> {
            IKeyEvent keyEvent = new HeadlessKeyEvent(112); // F1 key
            KeyMapper.setKeyStroke("[custom]", keyEvent);

            // Verify it was set
            boolean isDefined = KeyMapper.isKeyStrokeDefined("[custom]");
            assertTrue(isDefined, "Custom keystroke should be defined");
        }, "KeyMapper should set keystrokes from IKeyEvent");
    }

    @Test
    @DisplayName("KeyMapper handles modifier keys in headless mode")
    void testModifierKeysHeadless() {
        assertDoesNotThrow(() -> {
            // Shift+F1 (PF13)
            IKeyEvent keyEvent = new HeadlessKeyEvent(112, true, false, false);
            String mnemonic = KeyMapper.getKeyStrokeMnemonic(keyEvent);

            assertNotNull(mnemonic, "Should handle modifier keys");
            assertEquals("[pf13]", mnemonic, "Shift+F1 should map to PF13");
        }, "KeyMapper should handle modifiers in headless mode");
    }

    @Test
    @DisplayName("KeyMapper handles key location in headless mode")
    void testKeyLocationHeadless() {
        assertDoesNotThrow(() -> {
            // Enter from numpad (keyCode 10, location NUMPAD)
            IKeyEvent keyEvent = new HeadlessKeyEvent(10, false, false, false, false, 4, (char)0);
            String mnemonic = KeyMapper.getKeyStrokeMnemonic(keyEvent);

            assertNotNull(mnemonic, "Should handle key location");
            // Note: The test might need adjustment based on actual mapping
            assertTrue(mnemonic.contains("enter"), "Numpad Enter should map to enter variant");
        }, "KeyMapper should handle key location in headless mode");
    }

    @Test
    @DisplayName("KeyStroker can be created from headless IKeyEvent")
    void testKeyStrokerFromIKeyEvent() {
        assertDoesNotThrow(() -> {
            IKeyEvent keyEvent = new HeadlessKeyEvent(10, false, false, false);
            KeyStroker stroker = new KeyStroker(keyEvent);

            assertEquals(10, stroker.getKeyCode(), "KeyStroker should store key code");
        }, "KeyStroker should be constructable from IKeyEvent");
    }

    @Test
    @DisplayName("KeyMapper equality check works with headless events")
    void testIsEqualLastHeadless() {
        assertDoesNotThrow(() -> {
            IKeyEvent keyEvent1 = new HeadlessKeyEvent(10);
            IKeyEvent keyEvent2 = new HeadlessKeyEvent(10);
            IKeyEvent keyEvent3 = new HeadlessKeyEvent(9);

            KeyMapper.getKeyStrokeMnemonic(keyEvent1); // Set the "last" keystroke

            boolean equal1 = KeyMapper.isEqualLast(keyEvent1);
            boolean equal2 = KeyMapper.isEqualLast(keyEvent2);
            boolean equal3 = KeyMapper.isEqualLast(keyEvent3);

            assertTrue(equal1, "Same event should be equal to last");
            assertTrue(equal2, "Identical event should be equal to last");
            assertFalse(equal3, "Different event should not be equal to last");
        }, "KeyMapper equality check should work with IKeyEvent");
    }
}
