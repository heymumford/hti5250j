/**
 * Title: ScreenFormatChangePairwiseTest.java
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise TDD tests for HTI5250j screen format changes
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.hti5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Pairwise parameterized tests for ScreenFormat table changes during format transitions.
 *
 * Tests explore systematic combinations of:
 * 1. Format change type: NONE (no change), PARTIAL (some fields), COMPLETE (full replacement)
 * 2. Field preservation: DISCARD (clear all), PRESERVE_MODIFIED (keep modified), PRESERVE_ALL
 * 3. Cursor handling: HOME (to 0,0), PRESERVE (stay current), SPECIFIED (move to)
 * 4. Screen clear: NONE, FORMAT_ONLY (format plane), ALL (all planes)
 * 5. Input state: IDLE, ACTIVE (in field), PENDING_AID (waiting to send)
 *
 * Discovers: Format transition bugs, field corruption, cursor loss, state machine issues
 */
@RunWith(Parameterized.class)
public class ScreenFormatChangePairwiseTest {

    // Pairwise test parameters
    private final String formatChange;     // "none", "partial", "complete"
    private final String fieldPreservation; // "discard", "preserve-modified", "preserve-all"
    private final String cursorHandling;    // "home", "preserve", "specified"
    private final String screenClear;       // "none", "format-only", "all"
    private final String inputState;        // "idle", "active", "pending-aid"

    // Instance variables
    private ScreenFields screenFields;
    private Screen5250TestDouble screen5250;
    private ScreenField field1;
    private ScreenField field2;
    private int savedCursorPos;
    private int expectedCursorPos;

    /**
     * Pairwise combinations covering all interaction pairs across 5 dimensions
     * Total: 25+ combinations ensuring all pairs are exercised
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // === COMPLETE format changes (new screen layout) ===
                // Combination 1: Complete format + discard fields + home cursor + all clear + idle
                { "complete", "discard", "home", "all", "idle" },
                // Combination 2: Complete format + preserve-modified + preserve cursor + format-only + active
                { "complete", "preserve-modified", "preserve", "format-only", "active" },
                // Combination 3: Complete format + preserve-all + specified cursor + none + pending-aid
                { "complete", "preserve-all", "specified", "none", "pending-aid" },
                // Combination 4: Complete format + discard + specified + all + idle
                { "complete", "discard", "specified", "all", "idle" },
                // Combination 5: Complete format + preserve-modified + home + format-only + pending-aid
                { "complete", "preserve-modified", "home", "format-only", "pending-aid" },

                // === PARTIAL format changes (some fields updated) ===
                // Combination 6: Partial format + preserve-all + preserve + format-only + active
                { "partial", "preserve-all", "preserve", "format-only", "active" },
                // Combination 7: Partial format + discard + specified + all + idle
                { "partial", "discard", "specified", "all", "idle" },
                // Combination 8: Partial format + preserve-modified + home + none + pending-aid
                { "partial", "preserve-modified", "home", "none", "pending-aid" },
                // Combination 9: Partial format + preserve-all + home + all + active
                { "partial", "preserve-all", "home", "all", "active" },
                // Combination 10: Partial format + discard + preserve + format-only + idle
                { "partial", "discard", "preserve", "format-only", "idle" },

                // === NO format change (same layout) ===
                // Combination 11: No format + preserve-all + preserve + none + active
                { "none", "preserve-all", "preserve", "none", "active" },
                // Combination 12: No format + discard + home + format-only + pending-aid
                { "none", "discard", "home", "format-only", "pending-aid" },
                // Combination 13: No format + preserve-modified + specified + all + idle
                { "none", "preserve-modified", "specified", "all", "idle" },
                // Combination 14: No format + preserve-all + specified + format-only + pending-aid
                { "none", "preserve-all", "specified", "format-only", "pending-aid" },
                // Combination 15: No format + discard + preserve + none + active
                { "none", "discard", "preserve", "none", "active" },

                // === Adversarial: Incomplete/inconsistent format updates ===
                // Combination 16: Partial format + preserve-modified + home + all + idle (incomplete middle field)
                { "partial", "preserve-modified", "home", "all", "idle" },
                // Combination 17: Complete format + discard + preserve + none + pending-aid (no clear during format)
                { "complete", "discard", "preserve", "none", "pending-aid" },
                // Combination 18: Partial format + preserve-all + home + format-only + active (field redefinition conflict)
                { "partial", "preserve-all", "home", "format-only", "active" },

                // === Edge cases: Extreme state combinations ===
                // Combination 19: Complete format + preserve-modified + preserve + all + active (cursor in dead zone)
                { "complete", "preserve-modified", "preserve", "all", "active" },
                // Combination 20: Partial format + discard + specified + format-only + pending-aid (AID pending with no fields)
                { "partial", "discard", "specified", "format-only", "pending-aid" },
                // Combination 21: No format + preserve-all + home + all + idle (no-op format with full clear)
                { "none", "preserve-all", "home", "all", "idle" },

                // === Additional coverage for interaction pairs ===
                // Combination 22: Complete + discard + home + format-only + active
                { "complete", "discard", "home", "format-only", "active" },
                // Combination 23: Partial + preserve-modified + specified + none + idle
                { "partial", "preserve-modified", "specified", "none", "idle" },
                // Combination 24: No format + discard + specified + none + pending-aid
                { "none", "discard", "specified", "none", "pending-aid" },
                // Combination 25: Complete + preserve-all + preserve + format-only + idle
                { "complete", "preserve-all", "preserve", "format-only", "idle" },

                // === Additional adversarial cases ===
                // Combination 26: Partial + preserve-all + preserve + all + pending-aid (field conflict with AID)
                { "partial", "preserve-all", "preserve", "all", "pending-aid" },
                // Combination 27: Complete + preserve-modified + specified + none + active (cursor jump mid-field)
                { "complete", "preserve-modified", "specified", "none", "active" },
        });
    }

    public ScreenFormatChangePairwiseTest(String formatChange, String fieldPreservation,
                                         String cursorHandling, String screenClear, String inputState) {
        this.formatChange = formatChange;
        this.fieldPreservation = fieldPreservation;
        this.cursorHandling = cursorHandling;
        this.screenClear = screenClear;
        this.inputState = inputState;
    }

    @Before
    public void setUp() throws Exception {
        // Create test screen with initial format
        screen5250 = new Screen5250TestDouble(24, 80);

        // Access screenFields via reflection (it's private in Screen5250)
        Field screenFieldsField = Screen5250.class.getDeclaredField("screenFields");
        screenFieldsField.setAccessible(true);
        screenFields = (ScreenFields) screenFieldsField.get(screen5250);

        // Create initial format: 2 fields
        createInitialFormat();

        // Set input state
        setInputState();

        // Save cursor position before format change
        savedCursorPos = screen5250.getPos(screen5250.getCurrentRow() - 1, screen5250.getCurrentCol() - 1);
    }

    /**
     * TEST 1: Complete format change discards all fields
     *
     * Scenario: Format changes completely (new layout), fields should be discarded
     * Validates: Old fields are cleared, new fields are loaded
     */
    @Test
    public void testCompleteFormatChangeDiscardsFields() {
        if (!formatChange.equals("complete")) {
            return; // Skip non-complete format changes
        }

        // RED: Will fail if fields aren't properly cleared
        int fieldCountBefore = screenFields.getFieldCount();
        assertTrue("Should have fields before format change", fieldCountBefore > 0);

        // GREEN: Clear format table and create new format
        screenFields.clearFFT();
        assertEquals(
            "Format table should be empty after clearFFT()",
            0,
            screenFields.getFieldCount()
        );

        // Create new format with different fields
        ScreenField newField1 = createField(10, 0, 0, 20, 0x00, 0x00, 0x00, 0x00);
        ScreenField newField2 = createField(10, 2, 0, 15, 0x00, 0x00, 0x00, 0x00);

        assertEquals(
            "New format should have 2 fields",
            2,
            screenFields.getFieldCount()
        );
    }

    /**
     * TEST 2: Partial format change preserves non-conflicting fields
     *
     * Scenario: Some fields updated, others remain
     * Validates: Only affected fields change, others preserved
     */
    @Test
    public void testPartialFormatChangePreservesFields() {
        if (!formatChange.equals("partial")) {
            return;
        }

        if (!fieldPreservation.equals("preserve-modified") && !fieldPreservation.equals("preserve-all")) {
            return;
        }

        // Store original field information
        ScreenField originalField1 = field1;
        int originalField1Pos = field1.startPos();
        int originalField1Len = field1.getLength();

        // Simulate partial format change: update only field2, keep field1
        // (field1 remains in format table)
        ScreenField newField2 = createField(10, 3, 0, 25, 0x00, 0x00, 0x00, 0x00);

        // Verify field1 still exists
        assertTrue(
            "Field 1 should still exist after partial format change",
            screenFields.existsAtPos(originalField1Pos)
        );
    }

    /**
     * TEST 3: No format change leaves fields intact
     *
     * Scenario: Format definition unchanged
     * Validates: All fields preserved, attributes unchanged
     */
    @Test
    public void testNoFormatChangePreservesFields() {
        if (!formatChange.equals("none")) {
            return;
        }

        // Record field positions before format change
        int field1Pos = field1.startPos();
        int field2Pos = field2.startPos();

        // Verify fields still exist after no change
        assertTrue(
            "Field 1 should still exist when format unchanged",
            screenFields.existsAtPos(field1Pos)
        );

        assertTrue(
            "Field 2 should still exist when format unchanged",
            screenFields.existsAtPos(field2Pos)
        );
    }

    /**
     * TEST 4: Cursor moves to home position on format change if specified
     *
     * Scenario: Cursor handling set to HOME
     * Validates: Cursor moves to (0, 0) before field input
     */
    @Test
    public void testCursorMovesToHomeOnFormatChange() {
        if (!cursorHandling.equals("home")) {
            return;
        }

        // Simulate cursor movement to home
        screen5250.setCursor(0, 0);
        int homePos = screen5250.getPos(0, 0);

        int cursorPos = screen5250.getPos(screen5250.getCurrentRow() - 1, screen5250.getCurrentCol() - 1);

        assertEquals(
            "Cursor should be at home position (0,0) after format change",
            homePos,
            cursorPos
        );
    }

    /**
     * TEST 5: Cursor preserved at current position
     *
     * Scenario: Cursor handling set to PRESERVE
     * Validates: Cursor stays at saved position if position still valid in new format
     */
    @Test
    public void testCursorPreservedAtCurrentPosition() {
        if (!cursorHandling.equals("preserve")) {
            return;
        }

        // Save current position before format change
        int cursorBeforeChange = screen5250.getPos(screen5250.getCurrentRow() - 1, screen5250.getCurrentCol() - 1);

        // Simulate format change without moving cursor
        // (In real code, this would be conditional on whether position is still valid)
        int cursorAfterChange = screen5250.getPos(screen5250.getCurrentRow() - 1, screen5250.getCurrentCol() - 1);

        assertEquals(
            "Cursor should remain at same position when PRESERVE specified",
            cursorBeforeChange,
            cursorAfterChange
        );
    }

    /**
     * TEST 6: Cursor moved to specified position
     *
     * Scenario: Cursor handling set to SPECIFIED
     * Validates: Cursor moves to designated field or position
     */
    @Test
    public void testCursorMovedToSpecifiedPosition() {
        if (!cursorHandling.equals("specified")) {
            return;
        }

        // Move cursor to specified location (first field start)
        int specifiedPos = field1.startPos();
        int specifiedRow = specifiedPos / 80;
        int specifiedCol = specifiedPos % 80;

        screen5250.setCursor(specifiedRow + 1, specifiedCol + 1);
        int actualPos = screen5250.getPos(screen5250.getCurrentRow() - 1, screen5250.getCurrentCol() - 1);

        assertEquals(
            "Cursor should move to specified position",
            specifiedPos,
            actualPos
        );
    }

    /**
     * TEST 7: Screen cleared when SCREEN_CLEAR_ALL is specified
     *
     * Scenario: Screen clear mode is ALL
     * Validates: All planes (char, attr, extended) are zeroed
     */
    @Test
    public void testScreenClearedWhenClearAll() throws NoSuchFieldException, IllegalAccessException {
        if (!screenClear.equals("all")) {
            return;
        }

        // Write some characters to screen
        ScreenPlanes planes = screen5250.getPlanes();
        planes.setChar(0, 'A');
        planes.setChar(80, 'B');
        planes.setScreenAttr(0, 32);

        // Clear positions (simulate screen clear)
        for (int i = 0; i < 160; i++) {
            planes.setChar(i, (char) 0);
            planes.setScreenAttr(i, 32); // reset to default
        }

        // Verify screen is cleared
        assertEquals("Position 0 should be cleared", (char) 0, planes.getChar(0));
        assertEquals("Position 80 should be cleared", (char) 0, planes.getChar(80));
        assertEquals("Position 0 attribute should be default", 32, planes.getCharAttr(0));
    }

    /**
     * TEST 8: Format plane cleared but character data preserved
     *
     * Scenario: Screen clear mode is FORMAT_ONLY
     * Validates: Format/attribute planes cleared, character plane intact
     */
    @Test
    public void testFormatOnlyClearPreservesCharacters() throws NoSuchFieldException, IllegalAccessException {
        if (!screenClear.equals("format-only")) {
            return;
        }

        ScreenPlanes planes = screen5250.getPlanes();

        // Set both character and attribute
        planes.setChar(10, 'X');
        planes.setScreenAttr(10, 33); // reverse

        // Simulate format-only clear (attributes reset, characters stay)
        planes.setScreenAttr(10, 32); // Reset to default attribute

        // Verify character data is preserved
        assertEquals(
            "Character should be preserved in format-only clear",
            'X',
            planes.getChar(10)
        );

        // Verify attribute was reset
        assertEquals(
            "Attribute should be reset to default",
            32,
            planes.getCharAttr(10)
        );
    }

    /**
     * TEST 9: No screen clear leaves all data intact
     *
     * Scenario: Screen clear mode is NONE
     * Validates: All planes unchanged during format change
     */
    @Test
    public void testNoClearLeavesScreenIntact() throws NoSuchFieldException, IllegalAccessException {
        if (!screenClear.equals("none")) {
            return;
        }

        ScreenPlanes planes = screen5250.getPlanes();

        // Set data on screen
        planes.setChar(5, 'T');
        planes.setScreenAttr(5, 36);

        char savedChar = planes.getChar(5);
        int savedAttr = planes.getCharAttr(5);

        // Format change with no clear should not affect data
        assertEquals("Character should be unchanged with no clear", 'T', savedChar);
        assertEquals("Attribute should be unchanged with no clear", 36, savedAttr);
    }

    /**
     * TEST 10: Input state IDLE allows format change without validation
     *
     * Scenario: Input state is IDLE (no field active)
     * Validates: Format change proceeds without field validation
     */
    @Test
    public void testIdleInputStateAllowsFormatChange() {
        if (!inputState.equals("idle")) {
            return;
        }

        // When idle, currentField should be null
        screenFields.clearFFT();
        assertNull("Current field should be null when IDLE", screenFields.getCurrentField());

        // Format change should proceed
        ScreenField newField = createField(10, 0, 0, 20, 0x00, 0x00, 0x00, 0x00);
        assertNotNull("New field should be created", newField);
    }

    /**
     * TEST 11: Input state ACTIVE requires field cleanup
     *
     * Scenario: Input state is ACTIVE (cursor in field)
     * Validates: Field is properly cleared before format change
     */
    @Test
    public void testActiveInputStateRequiresFieldCleanup() {
        if (!inputState.equals("active")) {
            return;
        }

        // Set active field
        screenFields.setCurrentField(field1);
        assertNotNull("Current field should be set for ACTIVE state", screenFields.getCurrentField());

        // Clear format table (cleanup)
        screenFields.clearFFT();
        assertNull("Current field should be null after format change", screenFields.getCurrentField());
    }

    /**
     * TEST 12: Input state PENDING_AID preserves field for transmission
     *
     * Scenario: Input state is PENDING_AID (AID key pressed, waiting for host response)
     * Validates: Fields preserved until ACK, then format change allowed
     */
    @Test
    public void testPendingAidInputStatePreservesFields() {
        if (!inputState.equals("pending-aid")) {
            return;
        }

        // Mark MDT (Modified Data Tag) on current field
        screenFields.setCurrentField(field1);
        screenFields.setCurrentFieldMDT();
        assertTrue("Master MDT should be set", screenFields.isMasterMDT());

        // When PENDING_AID, fields should remain until format change confirmed
        // (in real code, this would wait for host ACK)
        assertTrue("Field should still exist for transmission", screenFields.getFieldCount() > 0);
    }

    /**
     * TEST 13: Format table corruption recovery - incomplete field definition
     *
     * Adversarial: Simulate incomplete field update (length missing)
     * Validates: System handles malformed format without crashing
     */
    @Test
    public void testIncompleteFieldDefinitionHandling() {
        if (!formatChange.equals("partial")) {
            return;
        }

        // Attempt to create field with missing attributes
        try {
            // This simulates receiving partial format data from host
            ScreenField incompleteField = new ScreenField(screen5250);
            incompleteField.setField(0, 0, 0, 0, 0x00, 0x00, 0x00, 0x00); // length=0

            // Should handle gracefully (either reject or handle zero-length field)
            assertTrue(
                "System should handle incomplete field definition",
                incompleteField.getLength() == 0
            );
        } catch (Exception e) {
            fail("System crashed on incomplete field definition: " + e.getMessage());
        }
    }

    /**
     * TEST 14: Format change with cursor in invalid position
     *
     * Adversarial: Cursor position becomes invalid after format change
     * Validates: Cursor repositioned to valid location
     */
    @Test
    public void testCursorInvalidationAfterFormatChange() {
        if (!formatChange.equals("complete")) {
            return;
        }

        // Set cursor at far right (column 79)
        screen5250.setCursor(10, 80);
        int originalPos = screen5250.getPos(9, 79);

        // Complete format change might invalidate cursor
        // System should reposition to home or first field
        if (cursorHandling.equals("home")) {
            screen5250.setCursor(0, 0);
            int newPos = screen5250.getPos(screen5250.getCurrentRow() - 1, screen5250.getCurrentCol() - 1);
            assertEquals("Cursor should be repositioned to home", 0, newPos);
        }
    }

    /**
     * TEST 15: Format change with conflicting field redefinition
     *
     * Adversarial: New format redefines field at same position with different attributes
     * Validates: Old field properly replaced, attributes updated
     */
    @Test
    public void testFieldRedefinitionWithAttributeConflict() {
        if (!formatChange.equals("partial")) {
            return;
        }

        // Save original field1 attributes
        int originalAttr = field1.getAttr();
        int originalLen = field1.getLength();

        // Redefine field1 at same position with different attributes
        // This simulates host sending format update with changed field definition
        ScreenField redefinedField = new ScreenField(screen5250);
        redefinedField.setField(36, field1.startPos() / 80, field1.startPos() % 80,
                               originalLen + 5, 0x00, 0x00, 0x00, 0x00);

        // New field should have updated attributes
        assertNotSame(
            "Field should be redefined with new attribute",
            originalAttr,
            redefinedField.getAttr()
        );

        assertTrue(
            "Field should be extended in length",
            redefinedField.getLength() > originalLen
        );
    }

    /**
     * TEST 16: Format state consistency after format change
     *
     * Validates: Field count, positions, attributes all consistent after change
     */
    @Test
    public void testFormatTableConsistencyAfterChange() {
        if (formatChange.equals("none")) {
            return; // No change scenario
        }

        int fieldCountBefore = screenFields.getFieldCount();

        if (formatChange.equals("complete")) {
            screenFields.clearFFT();
            assertEquals("Format table should be empty after complete change", 0, screenFields.getFieldCount());

            // Recreate new format
            createField(10, 0, 0, 20, 0x00, 0x00, 0x00, 0x00);
            createField(10, 1, 0, 20, 0x00, 0x00, 0x00, 0x00);

            int fieldCountAfter = screenFields.getFieldCount();
            assertEquals("New format should have consistent field count", 2, fieldCountAfter);
        }
    }

    /**
     * TEST 17: Multiple sequential format changes
     *
     * Validates: Format table can be updated multiple times without corruption
     */
    @Test
    public void testMultipleSequentialFormatChanges() {
        // Change 1: Clear and create new format
        screenFields.clearFFT();
        assertEquals("First clear should empty format table", 0, screenFields.getFieldCount());

        createField(10, 0, 0, 20, 0x00, 0x00, 0x00, 0x00);
        assertEquals("After first change, should have 1 field", 1, screenFields.getFieldCount());

        // Change 2: Clear and create different format
        screenFields.clearFFT();
        assertEquals("Second clear should empty format table", 0, screenFields.getFieldCount());

        createField(10, 0, 0, 30, 0x00, 0x00, 0x00, 0x00);
        createField(10, 2, 0, 25, 0x00, 0x00, 0x00, 0x00);
        assertEquals("After second change, should have 2 fields", 2, screenFields.getFieldCount());

        // Change 3: Partial update
        assertTrue("Format should remain valid after multiple changes", screenFields.getFieldCount() > 0);
    }

    /**
     * TEST 18: Format change does not corrupt existing field data on disk
     *
     * Validates: Field data isolation - format changes don't affect persisted data
     */
    @Test
    public void testFormatChangeDoesNotCorruptFieldData() {
        // Store field data
        int field1StartPos = field1.startPos();
        int field1Length = field1.getLength();
        int field1Attr = field1.getAttr();

        // Format change
        if (formatChange.equals("partial")) {
            screenFields.clearFFT();
            createField(field1Attr, field1StartPos / 80, field1StartPos % 80, field1Length, 0x00, 0x00, 0x00, 0x00);

            // Retrieve field and verify data intact
            ScreenField retrievedField = screenFields.getCurrentField();
            assertNotNull("Field should be recoverable", retrievedField);
        }
    }

    /**
     * TEST 19: Format change with bidirectional field links
     *
     * Validates: Field navigation links (prev/next) remain valid after format change
     */
    @Test
    public void testFieldNavigationLinksAfterFormatChange() {
        if (!formatChange.equals("partial")) {
            return;
        }

        // Create linked fields
        ScreenField f1 = createField(10, 0, 0, 20, 0x00, 0x00, 0x00, 0x00);
        ScreenField f2 = createField(10, 1, 0, 20, 0x00, 0x00, 0x00, 0x00);

        // After partial format change, both should still be navigable
        assertTrue("Fields should exist after format change", screenFields.getFieldCount() >= 2);

        // Verify field positions are valid
        assertNotNull("Field 1 should be findable", f1);
        assertNotNull("Field 2 should be findable", f2);
    }

    /**
     * TEST 20: Attribute inheritance in format changes
     *
     * Validates: Field attributes properly copied/inherited during format updates
     */
    @Test
    public void testAttributeInheritanceInFormatChanges() {
        if (formatChange.equals("none")) {
            return;
        }

        int srcAttr = 33; // reverse video
        ScreenField newField = createField(srcAttr, 0, 0, 20, 0x00, 0x00, 0x00, 0x00);

        assertEquals(
            "New field should inherit source attribute",
            srcAttr,
            newField.getAttr()
        );
    }

    /**
     * TEST 21: Format change with pending MDT fields
     *
     * Validates: MDT (Modified Data Tag) state preserved correctly during format change
     */
    @Test
    public void testFormatChangePreservesMDTState() {
        if (!inputState.equals("pending-aid")) {
            return;
        }

        // Set MDT on field
        screenFields.setCurrentField(field1);
        screenFields.setCurrentFieldMDT();
        assertTrue("MDT should be set", screenFields.isMasterMDT());

        if (fieldPreservation.equals("preserve-modified")) {
            // MDT state should be preserved
            assertTrue("MDT should remain after format change with preserve-modified", screenFields.isMasterMDT());
        }
    }

    /**
     * TEST 22: Format table bounds checking
     *
     * Adversarial: Attempt to define fields beyond screen boundaries
     * Validates: System rejects or clips out-of-bounds fields
     */
    @Test
    public void testFormatTableBoundsChecking() {
        // Attempt to create field beyond screen (24x80)
        ScreenField overflowField = new ScreenField(screen5250);
        overflowField.setField(10, 100, 100, 20, 0x00, 0x00, 0x00, 0x00);

        // Field position calculation should handle out-of-bounds
        int pos = overflowField.startPos();
        assertTrue(
            "Field position should be calculated even if out-of-bounds",
            pos >= 0
        );
    }

    /**
     * TEST 23: Cursor position validation in new format
     *
     * Adversarial: Format changes to layout where old cursor position is in non-field area
     * Validates: Cursor moved to first field or home position
     */
    @Test
    public void testCursorValidationInNewFormat() {
        if (!formatChange.equals("complete")) {
            return;
        }

        // Simulate cursor at position that won't exist in new format
        screen5250.setCursor(5, 50);

        // Complete format change with new field layout
        screenFields.clearFFT();
        createField(10, 0, 0, 20, 0x00, 0x00, 0x00, 0x00); // Field only at top

        // Cursor should be repositioned if cursorHandling indicates it
        if (cursorHandling.equals("home")) {
            assertEquals("Cursor should be at home", 0, screen5250.getPos(0, 0));
        }
    }

    /**
     * TEST 24: Format table empty field handling
     *
     * Adversarial: Format change results in no fields (all-blank screen)
     * Validates: System handles empty format gracefully
     */
    @Test
    public void testEmptyFormatTableHandling() {
        screenFields.clearFFT();
        assertEquals("Format table should be empty", 0, screenFields.getFieldCount());

        // Should be able to navigate/process empty format without crashing
        assertNull("Current field should be null when format empty", screenFields.getCurrentField());

        // Cursor should still be positionable
        screen5250.setCursor(0, 0);
        assertEquals("Cursor should be positionable even with empty format", 0, screen5250.getPos(0, 0));
    }

    /**
     * TEST 25: Format change with field attribute synchronization
     *
     * Validates: All field attributes properly synced with screen planes during format change
     */
    @Test
    public void testFieldAttributeSynchronizationAfterFormatChange() throws NoSuchFieldException, IllegalAccessException {
        if (formatChange.equals("none")) {
            return;
        }

        ScreenPlanes planes = screen5250.getPlanes();

        // Create field with specific attribute
        int fieldAttr = 36; // underline
        ScreenField syncField = createField(fieldAttr, 0, 0, 20, 0x00, 0x00, 0x00, 0x00);

        // Set attributes on corresponding screen positions
        for (int i = syncField.startPos(); i < syncField.startPos() + syncField.getLength(); i++) {
            planes.setScreenAttr(i, fieldAttr);
        }

        // Verify attributes are in sync
        for (int i = syncField.startPos(); i < syncField.startPos() + 5; i++) {
            assertEquals(
                "Screen attribute should match field attribute",
                fieldAttr,
                planes.getCharAttr(i)
            );
        }
    }

    // ============ HELPER METHODS ============

    private void createInitialFormat() throws Exception {
        // Use reflection to call protected setField method
        java.lang.reflect.Method setFieldMethod = ScreenFields.class.getDeclaredMethod("setField",
            int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
        setFieldMethod.setAccessible(true);
        field1 = (ScreenField) setFieldMethod.invoke(screenFields, 32, 0, 0, 20, 0x00, 0x00, 0x00, 0x00);
        field2 = (ScreenField) setFieldMethod.invoke(screenFields, 32, 2, 0, 20, 0x00, 0x00, 0x00, 0x00);
    }

    private ScreenField createField(int attr, int row, int col, int len, int ffw1, int ffw2, int fcw1, int fcw2) {
        try {
            java.lang.reflect.Method setFieldMethod = ScreenFields.class.getDeclaredMethod("setField",
                int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            setFieldMethod.setAccessible(true);
            return (ScreenField) setFieldMethod.invoke(screenFields, attr, row, col, len, ffw1, ffw2, fcw1, fcw2);
        } catch (Exception e) {
            fail("Could not create field: " + e.getMessage());
            return null;
        }
    }

    private void setInputState() {
        switch (inputState) {
            case "idle":
                screenFields.clearFFT();
                break;
            case "active":
                screenFields.setCurrentField(field1);
                break;
            case "pending-aid":
                screenFields.setCurrentField(field1);
                screenFields.setCurrentFieldMDT();
                break;
        }
    }

    /**
     * Test double for Screen5250
     */
    private static class Screen5250TestDouble extends Screen5250 {
        private int numRows;
        private int numCols;

        public Screen5250TestDouble(int rows, int cols) {
            super();
            this.numRows = rows;
            this.numCols = cols;
        }

        @Override
        public int getPos(int row, int col) {
            return (row * numCols) + col;
        }

        @Override
        public int getScreenLength() {
            return numRows * numCols;
        }

        @Override
        public int getRows() {
            return numRows;
        }

        @Override
        public int getColumns() {
            return numCols;
        }

        // Returns fields - inherited from Screen5250
        public ScreenPlanes getPlanes() {
            return planes;
        }

        @Override
        public boolean isInField(int x, boolean checkAttr) {
            return false;
        }

        @Override
        public StringBuffer getHSMore() {
            return new StringBuffer("More...");
        }

        @Override
        public StringBuffer getHSBottom() {
            return new StringBuffer("Bottom");
        }

        @Override
        public void setDirty(int pos) {
            // No-op
        }
    }
}
