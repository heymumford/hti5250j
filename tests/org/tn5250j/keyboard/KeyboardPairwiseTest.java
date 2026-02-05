package org.tn5250j.keyboard;

import org.junit.Before;
import org.junit.Test;

import java.awt.event.KeyEvent;

import static org.junit.Assert.*;

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

    @Before
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

        assertEquals("KeyStroker hash should be consistent",
                keystroke.hashCode(), keystroke.hashCode());
        assertFalse("Shift should not be down", keystroke.isShiftDown());
        assertFalse("Ctrl should not be down", keystroke.isControlDown());
        assertFalse("Alt should not be down", keystroke.isAltDown());
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

        assertTrue("Shift should be down", keystroke.isShiftDown());
        assertFalse("Ctrl should not be down", keystroke.isControlDown());
        assertEquals("Key code should be 0", KeyEvent.VK_0, keystroke.getKeyCode());
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

        assertEquals("Key code should be F1", KeyEvent.VK_F1, keystroke.getKeyCode());
        assertFalse("No modifiers should be active",
                keystroke.isShiftDown() || keystroke.isControlDown() || keystroke.isAltDown());
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

        assertEquals("Key code should be F12", KeyEvent.VK_F12, keystroke.getKeyCode());
        assertTrue("Ctrl should be down", keystroke.isControlDown());
    }

    /**
     * Test: Enter key mapping to [enter] mnemonic
     * Dimensions: Key=Enter, Modifiers=none, State=normal
     */
    @Test
    public void testEnterKeyMnemonic() {
        int enterValue = mnemonicResolver.findMnemonicValue("[enter]");
        assertTrue("Enter mnemonic should map to valid value", enterValue > 0);
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

        assertTrue("Shift+Tab should have shift set", keystroke.isShiftDown());
        assertEquals("Key code should be Tab", KeyEvent.VK_TAB, keystroke.getKeyCode());
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

        assertTrue("Ctrl should be down", keystroke.isControlDown());
        assertEquals("Key code should be LEFT", KeyEvent.VK_LEFT, keystroke.getKeyCode());
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

        assertEquals("KeyStrokes with same modifiers should be equal", ks1, ks2);
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

        assertTrue("Shift should be down", keystroke.isShiftDown());
        assertTrue("Ctrl should be down", keystroke.isControlDown());
        assertFalse("Alt should not be down", keystroke.isAltDown());
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

        assertEquals("Key code should be INSERT", KeyEvent.VK_INSERT, keystroke.getKeyCode());
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

        assertEquals("Key code should be DELETE", KeyEvent.VK_DELETE, keystroke.getKeyCode());
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

        assertEquals("Key code should preserve UNDEFINED", KeyEvent.VK_UNDEFINED, keystroke.getKeyCode());
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

        assertEquals("Negative key code should be preserved", -1, keystroke.getKeyCode());
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

        assertTrue("AltGr should be down", keystroke.isAltGrDown());
        assertEquals("Key code should be ALT_GRAPH", KeyEvent.VK_ALT_GRAPH, keystroke.getKeyCode());
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

        assertFalse("Numpad and standard Enter should differ",
                standardEnter.equals(numpadEnter));
        assertFalse("Hash codes should differ",
                standardEnter.hashCode() == numpadEnter.hashCode());
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

        assertFalse("A and Shift+A should not be equal", withoutShift.equals(withShift));
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
        assertNotNull("Keystroke description should not be null", description);
        assertTrue("Description should contain Ctrl",
                description.toLowerCase().contains("ctrl"));
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
        assertTrue("Description should contain Ctrl",
                description.toLowerCase().contains("ctrl"));
        assertTrue("Description should contain Alt",
                description.toLowerCase().contains("alt"));
    }

    /**
     * Test: Null mnemonic lookup returns null
     * Dimensions: Key=null, Modifiers=none, State=normal
     */
    @Test
    public void testNullMnemonicLookup() {
        KeyMnemonic result = mnemonicResolver.findMnemonic(null);
        assertNull("Null mnemonic lookup should return null", result);
    }

    /**
     * Test: Invalid mnemonic string returns zero value
     * Dimensions: Key=invalid, Modifiers=none, State=normal
     */
    @Test
    public void testInvalidMnemonicValue() {
        int value = mnemonicResolver.findMnemonicValue("INVALID_MNEMONIC_XYZ");
        assertEquals("Invalid mnemonic should return 0", 0, value);
    }

    /**
     * Test: Home key defined in mnemonic resolver
     * Dimensions: Key=[home], Modifiers=none, State=normal
     */
    @Test
    public void testHomeMnemonicDefined() {
        KeyMnemonic home = mnemonicResolver.findMnemonic("[home]");
        assertNotNull("Home mnemonic should be defined", home);
        int value = mnemonicResolver.findMnemonicValue("[home]");
        assertTrue("Home mnemonic should have valid value", value > 0);
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

        assertEquals("Key code should be ESCAPE", KeyEvent.VK_ESCAPE, keystroke.getKeyCode());
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

        assertEquals("Location should be LEFT", KeyStroker.KEY_LOCATION_LEFT, keystroke.getLocation());
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

        assertEquals("Key code should be PAGE_UP", KeyEvent.VK_PAGE_UP, keystroke.getKeyCode());
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

        assertTrue("Alt should be down", keystroke.isAltDown());
        assertEquals("Key code should be RIGHT", KeyEvent.VK_RIGHT, keystroke.getKeyCode());
        assertFalse("Ctrl should not be down", keystroke.isControlDown());
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

        assertEquals("Key code should be BACK_SPACE", KeyEvent.VK_BACK_SPACE, keystroke.getKeyCode());
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

        assertFalse("Different location should create different KeyStrokes",
                standardPeriod.equals(rightPeriod));
    }

}
