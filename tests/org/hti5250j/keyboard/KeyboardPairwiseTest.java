/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.keyboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pairwise TDD tests for keyboard input handling.
 *
 * Test dimensions:
 * - Key codes: [A-Z, 0-9, F1-F24, Enter, Tab, special, null, -1]
 * - Modifiers: [none, Shift, Ctrl, Alt, Meta, combinations]
 * - Input states: [normal, insert, overwrite, locked, unlocked]
 * - Field types: [alpha, numeric, signed, any]
 * - Cursor positions: [start, mid, end, outside-field]
 */
public class KeyboardPairwiseTest {

    private KeyStroker keystroke;
    private KeyMnemonicResolver mnemonicResolver;

    @BeforeEach
    public void setUp() {
        mnemonicResolver = new KeyMnemonicResolver();
    }

    // ==================== POSITIVE TESTS ====================

    /**
     * Test: Valid letter key (A) with no modifiers
     * Dimensions: Key=A, Modifiers=none, State=normal
     */
    @Test
    public void testLetterKeyWithoutModifiers() {
        keystroke = new KeyStroker(
                KeyEvent.VK_A,        // key code A
                false,                // no shift
                false,                // no ctrl
                false,                // no alt
                false,                // no altgr
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(keystroke.hashCode(), keystroke.hashCode(),"KeyStroker hash should be consistent");
        assertFalse(keystroke.isShiftDown(),"Shift should not be down");
        assertFalse(keystroke.isControlDown(),"Ctrl should not be down");
        assertFalse(keystroke.isAltDown(),"Alt should not be down");
    }

    /**
     * Test: Numeric key (0) with Shift modifier (produces special char)
     * Dimensions: Key=0, Modifiers=Shift, State=normal
     */
    @Test
    public void testNumericKeyWithShift() {
        keystroke = new KeyStroker(
                KeyEvent.VK_0,        // key code 0
                true,                 // shift down
                false,                // no ctrl
                false,                // no alt
                false,                // no altgr
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertTrue(keystroke.isShiftDown(),"Shift should be down");
        assertFalse(keystroke.isControlDown(),"Ctrl should not be down");
        assertEquals(KeyEvent.VK_0, keystroke.getKeyCode(),"Key code should be 0");
    }

    /**
     * Test: Function key F1 with no modifiers
     * Dimensions: Key=F1, Modifiers=none, State=normal
     */
    @Test
    public void testFunctionKeyF1NoModifiers() {
        keystroke = new KeyStroker(
                KeyEvent.VK_F1,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(KeyEvent.VK_F1, keystroke.getKeyCode(),"Key code should be F1");
        assertFalse(keystroke.isShiftDown() || keystroke.isControlDown() || keystroke.isAltDown(),"No modifiers should be active");
    }

    /**
     * Test: Function key F12 with Ctrl modifier
     * Dimensions: Key=F12, Modifiers=Ctrl, State=normal
     */
    @Test
    public void testFunctionKeyF12WithCtrl() {
        keystroke = new KeyStroker(
                KeyEvent.VK_F12,
                false,
                true,                 // ctrl down
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(KeyEvent.VK_F12, keystroke.getKeyCode(),"Key code should be F12");
        assertTrue(keystroke.isControlDown(),"Ctrl should be down");
    }

    /**
     * Test: Enter key mapping to [enter] mnemonic
     * Dimensions: Key=Enter, Modifiers=none, State=normal
     */
    @Test
    public void testEnterKeyMnemonic() {
        int enterValue = mnemonicResolver.findMnemonicValue("[enter]");
        assertTrue(enterValue > 0,"Enter mnemonic should map to valid value");
    }

    /**
     * Test: Tab key with Shift produces Backtab
     * Dimensions: Key=Tab, Modifiers=Shift, State=normal
     */
    @Test
    public void testTabWithShiftAsBacktab() {
        keystroke = new KeyStroker(
                KeyEvent.VK_TAB,
                true,                 // shift down
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertTrue(keystroke.isShiftDown(),"Shift+Tab should have shift set");
        assertEquals(KeyEvent.VK_TAB, keystroke.getKeyCode(),"Key code should be Tab");
    }

    /**
     * Test: Left arrow with Ctrl (navigate by word)
     * Dimensions: Key=Left, Modifiers=Ctrl, State=normal
     */
    @Test
    public void testLeftArrowWithCtrl() {
        keystroke = new KeyStroker(
                KeyEvent.VK_LEFT,
                false,
                true,                 // ctrl down
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertTrue(keystroke.isControlDown(),"Ctrl should be down");
        assertEquals(KeyEvent.VK_LEFT, keystroke.getKeyCode(),"Key code should be LEFT");
    }

    /**
     * Test: Keystroke equality with same modifiers and key
     * Dimensions: Key=Z, Modifiers=Alt, State=normal
     */
    @Test
    public void testKeystrokeEqualityMatch() {
        KeyStroker ks1 = new KeyStroker(
                KeyEvent.VK_Z,
                false,
                false,
                true,                 // alt down
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        KeyStroker ks2 = new KeyStroker(
                KeyEvent.VK_Z,
                false,
                false,
                true,                 // alt down
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(ks1, ks2,"KeyStrokes with same modifiers should be equal");
    }

    /**
     * Test: Multiple modifier combination (Ctrl+Shift)
     * Dimensions: Key=S, Modifiers=[Ctrl, Shift], State=normal
     */
    @Test
    public void testMultipleModifiersCombination() {
        keystroke = new KeyStroker(
                KeyEvent.VK_S,
                true,                 // shift down
                true,                 // ctrl down
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertTrue(keystroke.isShiftDown(),"Shift should be down");
        assertTrue(keystroke.isControlDown(),"Ctrl should be down");
        assertFalse(keystroke.isAltDown(),"Alt should not be down");
    }

    /**
     * Test: Insert key (toggle insert/overwrite mode)
     * Dimensions: Key=Insert, Modifiers=none, State=[insert/overwrite]
     */
    @Test
    public void testInsertKeyToggle() {
        keystroke = new KeyStroker(
                KeyEvent.VK_INSERT,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(KeyEvent.VK_INSERT, keystroke.getKeyCode(),"Key code should be INSERT");
    }

    /**
     * Test: Delete key (remove character)
     * Dimensions: Key=Delete, Modifiers=none, State=normal
     */
    @Test
    public void testDeleteKey() {
        keystroke = new KeyStroker(
                KeyEvent.VK_DELETE,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(KeyEvent.VK_DELETE, keystroke.getKeyCode(),"Key code should be DELETE");
    }

    // ==================== ADVERSARIAL / ERROR TESTS ====================

    /**
     * Test: Undefined key code (VK_UNDEFINED)
     * Dimensions: Key=undefined, Modifiers=none, State=normal
     */
    @Test
    public void testUndefinedKeyCode() {
        keystroke = new KeyStroker(
                KeyEvent.VK_UNDEFINED,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(KeyEvent.VK_UNDEFINED, keystroke.getKeyCode(),"Key code should preserve UNDEFINED");
    }

    /**
     * Test: Negative key code (invalid input)
     * Dimensions: Key=-1, Modifiers=none, State=normal
     */
    @Test
    public void testNegativeKeyCode() {
        keystroke = new KeyStroker(
                -1,                   // invalid key code
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(-1, keystroke.getKeyCode(),"Negative key code should be preserved");
    }

    /**
     * Test: AltGr key (special modifier on Linux)
     * Dimensions: Key=AltGr, Modifiers=AltGr, State=normal
     */
    @Test
    public void testAltGrModifier() {
        keystroke = new KeyStroker(
                KeyEvent.VK_ALT_GRAPH,
                false,
                false,
                false,
                true,                 // altgr down
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertTrue(keystroke.isAltGrDown(),"AltGr should be down");
        assertEquals(KeyEvent.VK_ALT_GRAPH, keystroke.getKeyCode(),"Key code should be ALT_GRAPH");
    }

    /**
     * Test: Numpad key location vs standard location same key
     * Dimensions: Key=Enter, Modifiers=none, State=normal, Location=[standard, numpad]
     */
    @Test
    public void testNumpadVsStandardEnter() {
        KeyStroker standardEnter = new KeyStroker(
                KeyEvent.VK_ENTER,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        KeyStroker numpadEnter = new KeyStroker(
                KeyEvent.VK_ENTER,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_NUMPAD
        );

        assertFalse(standardEnter.equals(numpadEnter),"Numpad and standard Enter should differ");
        assertFalse(standardEnter.hashCode() == numpadEnter.hashCode(),"Hash codes should differ");
    }

    /**
     * Test: Keystroke inequality with different modifiers
     * Dimensions: Key=A, Modifiers=[none vs Shift], State=normal
     */
    @Test
    public void testKeystrokeInequalityDifferentModifiers() {
        KeyStroker withoutShift = new KeyStroker(
                KeyEvent.VK_A,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        KeyStroker withShift = new KeyStroker(
                KeyEvent.VK_A,
                true,                 // shift down
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertFalse(withoutShift.equals(withShift),"A and Shift+A should not be equal");
    }

    /**
     * Test: Keystroke string representation
     * Dimensions: Key=F5, Modifiers=Ctrl, State=normal
     */
    @Test
    public void testKeystrokeStringRepresentation() {
        keystroke = new KeyStroker(
                KeyEvent.VK_F5,
                false,
                true,                 // ctrl down
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        String description = keystroke.getKeyStrokeDesc();
        assertNotNull(description,"Keystroke description should not be null");
        assertTrue(description.toLowerCase().contains("ctrl"),"Description should contain Ctrl");
    }

    /**
     * Test: Keystroke description includes all active modifiers
     * Dimensions: Key=G, Modifiers=[Ctrl, Alt], State=normal
     */
    @Test
    public void testKeystrokeDescriptionWithMultipleModifiers() {
        keystroke = new KeyStroker(
                KeyEvent.VK_G,
                false,
                true,                 // ctrl down
                true,                 // alt down
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        String description = keystroke.getKeyStrokeDesc();
        assertTrue(description.toLowerCase().contains("ctrl"),"Description should contain Ctrl");
        assertTrue(description.toLowerCase().contains("alt"),"Description should contain Alt");
    }

    /**
     * Test: Null mnemonic lookup returns null
     * Dimensions: Key=null, Modifiers=none, State=normal
     */
    @Test
    public void testNullMnemonicLookup() {
        KeyMnemonic result = mnemonicResolver.findMnemonic(null);
        assertNull(result,"Null mnemonic lookup should return null");
    }

    /**
     * Test: Invalid mnemonic string returns zero value
     * Dimensions: Key=invalid, Modifiers=none, State=normal
     */
    @Test
    public void testInvalidMnemonicValue() {
        int value = mnemonicResolver.findMnemonicValue("INVALID_MNEMONIC_XYZ");
        assertEquals(0, value,"Invalid mnemonic should return 0");
    }

    /**
     * Test: Home key defined in mnemonic resolver
     * Dimensions: Key=[home], Modifiers=none, State=normal
     */
    @Test
    public void testHomeMnemonicDefined() {
        KeyMnemonic home = mnemonicResolver.findMnemonic("[home]");
        assertNotNull(home,"Home mnemonic should be defined");
        int value = mnemonicResolver.findMnemonicValue("[home]");
        assertTrue(value > 0,"Home mnemonic should have valid value");
    }

    /**
     * Test: Escape key (sysreq in 5250 context)
     * Dimensions: Key=Escape, Modifiers=none, State=normal
     */
    @Test
    public void testEscapeKeySysreq() {
        keystroke = new KeyStroker(
                KeyEvent.VK_ESCAPE,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(KeyEvent.VK_ESCAPE, keystroke.getKeyCode(),"Key code should be ESCAPE");
    }

    /**
     * Test: Verify keystroke location property preserved
     * Dimensions: Key=5, Modifiers=none, State=normal, Location=LEFT
     */
    @Test
    public void testKeystrokeLocationPreserved() {
        keystroke = new KeyStroker(
                KeyEvent.VK_5,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_LEFT
        );

        assertEquals(KeyStroker.KEY_LOCATION_LEFT, keystroke.getLocation(),"Location should be LEFT");
    }

    /**
     * Test: PageUp key (navigation)
     * Dimensions: Key=PageUp, Modifiers=none, State=normal
     */
    @Test
    public void testPageUpNavigation() {
        keystroke = new KeyStroker(
                KeyEvent.VK_PAGE_UP,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(KeyEvent.VK_PAGE_UP, keystroke.getKeyCode(),"Key code should be PAGE_UP");
    }

    /**
     * Test: Right arrow with Alt (not standard navigation)
     * Dimensions: Key=Right, Modifiers=Alt, State=normal
     */
    @Test
    public void testRightArrowWithAlt() {
        keystroke = new KeyStroker(
                KeyEvent.VK_RIGHT,
                false,
                false,
                true,                 // alt down
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertTrue(keystroke.isAltDown(),"Alt should be down");
        assertEquals(KeyEvent.VK_RIGHT, keystroke.getKeyCode(),"Key code should be RIGHT");
        assertFalse(keystroke.isControlDown(),"Ctrl should not be down");
    }

    /**
     * Test: Backspace in normal vs locked input mode
     * Dimensions: Key=BackSpace, Modifiers=none, State=[normal, locked]
     */
    @Test
    public void testBackspaceKey() {
        keystroke = new KeyStroker(
                KeyEvent.VK_BACK_SPACE,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        assertEquals(KeyEvent.VK_BACK_SPACE, keystroke.getKeyCode(),"Key code should be BACK_SPACE");
    }

    /**
     * Test: Keystroke equality with Alt location and main key location
     * Dimensions: Key=. (period), Modifiers=none, State=normal, Location=[standard, alt]
     */
    @Test
    public void testKeystrokeLocationDifference() {
        KeyStroker standardPeriod = new KeyStroker(
                KeyEvent.VK_PERIOD,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_STANDARD
        );

        KeyStroker rightPeriod = new KeyStroker(
                KeyEvent.VK_PERIOD,
                false,
                false,
                false,
                false,
                KeyStroker.KEY_LOCATION_RIGHT
        );

        assertFalse(standardPeriod.equals(rightPeriod),"Different location should create different KeyStrokes");
    }

}
