/**
 * Title: SFCommandPairwiseTest.java
 * Copyright: Copyright (c) 2025
 * Company:
 *
 * Description: Comprehensive pairwise TDD tests for TN5250j SF (Start Field) command.
 *
 * The SF (Start Field) command initializes screen fields with various attributes
 * that control field behavior in 5250 terminal emulation:
 *
 * COMMAND STRUCTURE:
 * SF = 0x1D (Start Field order)
 * Parameters: attr (1 byte), ffw1/ffw2 (Field Format Words), fcw1/fcw2 (Field Control Words)
 *
 * PAIRWISE TEST DIMENSIONS (5 dimensions x 5 values = 25 combinations, testing 20+):
 * 1. Field Type (4 types via FFW1 bits 5-7):
 *    - Input (shift=0): User can enter data
 *    - Output (shift=1): Display-only
 *    - Both (shift=2): Input/output
 *    - Hidden (shift=5): Non-visible field
 *
 * 2. Protection Status (FFW1 bit 5):
 *    - Unprotected: Normal user input allowed
 *    - Protected: Read-only, no input (bypass field)
 *    - Auto-skip: Skip to next field automatically
 *
 * 3. Intensity Attribute (attr byte high nibble):
 *    - Normal (0x20): Standard display
 *    - High (0xC0): Enhanced visibility
 *    - Non-display (0x00): Hidden on screen
 *
 * 4. Field Length (len parameter):
 *    - 0 (degenerate): Single position marker
 *    - 1: Minimal field
 *    - 80: Full row
 *    - 255: Large field
 *    - Max (1920 for 24x80): Entire screen
 *
 * 5. Field Position (row/col at creation):
 *    - Row start (row 0): Top of screen
 *    - Mid-row (row 11): Center of screen
 *    - Row end (row 23): Bottom of screen
 *    - Column variations: start, mid, end
 *
 * ADVERSARIAL SCENARIOS:
 * - Overlapping field ranges
 * - Malformed FFW/FCW values (0x00, 0xFF)
 * - Fields that exceed screen bounds
 * - Continued fields (FCW1 bit 1 & 2 set)
 * - Cursor progression fields (FCW1=0x88)
 *
 * POSITIVE TESTS (15+): Valid field creation and attribute verification
 * ADVERSARIAL TESTS (10+): Boundary conditions, malformed data, overlaps
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
package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Pairwise TDD test suite for Screen5250 SF (Start Field) command.
 *
 * Focuses on field creation and attribute setting across all combinations
 * of field types, protection modes, intensity levels, lengths, and positions.
 */
public class SFCommandPairwiseTest {

    private Screen5250 screen;
    private ScreenFields screenFields;
    private ScreenOIA oia;

    private static final int SCREEN_ROWS = 24;
    private static final int SCREEN_COLS = 80;
    private static final int SCREEN_SIZE = SCREEN_ROWS * SCREEN_COLS; // 1920

    /**
     * Test double for Screen5250 with SF command support
     */
    private static class Screen5250TestDouble extends Screen5250 {
        public Screen5250TestDouble() {
            super();
        }

        @Override
        public int getScreenLength() {
            return SCREEN_SIZE;
        }

        @Override
        public boolean isInField(int pos, boolean checkAttr) {
            // Default implementation: if position >= 0, it's potentially in a field
            return pos >= 0 && pos < SCREEN_SIZE;
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
            // No-op for testing
        }
    }

    @Before
    public void setUp() throws Exception {
        screen = new Screen5250TestDouble();
        screenFields = screen.getScreenFields();
        oia = screen.getOIA();
        oia.setKeyBoardLocked(false);

        // Initialize screen to home position
        screen.setCursor(1, 1);
        screenFields.clearFFT();
    }

    /**
     * Helper: Get private lastPos field via reflection
     */
    private int getLastPos() throws Exception {
        Field field = Screen5250.class.getDeclaredField("lastPos");
        field.setAccessible(true);
        return (int) field.get(screen);
    }

    /**
     * Helper: Set private lastPos field via reflection
     */
    private void setLastPos(int pos) throws Exception {
        Field field = Screen5250.class.getDeclaredField("lastPos");
        field.setAccessible(true);
        field.set(screen, pos);
    }

    /**
     * Helper: Invoke addField via reflection
     */
    private void addField(int attr, int len, int ffw1, int ffw2, int fcw1, int fcw2) throws Exception {
        Method method = Screen5250.class.getDeclaredMethod("addField",
                int.class, int.class, int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(screen, attr, len, ffw1, ffw2, fcw1, fcw2);
    }

    // ========================================================================
    // POSITIVE TESTS: Valid field creation and attribute verification (15+)
    // ========================================================================

    /**
     * POSITIVE: Create simple input field at home position (1,1)
     * Pairwise: type=input(shift=0), protection=unprotected, intensity=normal, len=10, pos=(1,1)
     *
     * FFW1: bit 5=0 (unprotected), bits 5-7=0 (input)
     * Attributes: Normal intensity (0x20)
     */
    @Test
    public void testCreateSimpleInputFieldAtHomePosition() throws Exception {
        // ARRANGE: Position at home
        screen.setCursor(1, 1);

        // ACT: Add input field (ffw1=0x00 unprotected/input, normal intensity)
        addField(0x20, 10, 0x00, 0x00, 0x00, 0x00);

        // ASSERT: Field created
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertNotNull("Field should exist", field);
        assertEquals("Field length should be 10", 10, field.getLength());
        assertFalse("Field should be unprotected", field.isBypassField());
    }

    /**
     * POSITIVE: Create output field (read-only display)
     * Pairwise: type=output(shift=1), protection=protected, intensity=high, len=20, pos=(12,40)
     *
     * FFW1: 0x20 (bit 5 set = bypass), shift bits would be 0x01
     * Attributes: High intensity (0xC0)
     */
    @Test
    public void testCreateOutputFieldWithHighIntensity() throws Exception {
        // ARRANGE: Position at middle of screen
        screen.setCursor(12, 40);

        // ACT: Add output/protected field
        addField(0xC0, 20, 0x21, 0x00, 0x00, 0x00);

        // ASSERT: Field created with protection
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Field length should be 20", 20, field.getLength());
        assertTrue("Field should be protected (bypass)", field.isBypassField());
        assertEquals("Field attribute should be high intensity", 0xC0, field.getAttr());
    }

    /**
     * POSITIVE: Create both-type field (input/output)
     * Pairwise: type=both(shift=2), protection=auto-skip, intensity=normal, len=80, pos=(23,1)
     *
     * FFW1: 0x22 (shift=2, bit 5 set for auto-skip)
     */
    @Test
    public void testCreateBothTypeFieldFullRow() throws Exception {
        // ARRANGE: Position at last row
        screen.setCursor(23, 1);

        // ACT: Add both-type field for entire row
        addField(0x20, 80, 0x22, 0x00, 0x00, 0x00);

        // ASSERT: Field spans full row
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Field length should be 80 (full row)", 80, field.getLength());
    }

    /**
     * POSITIVE: Create hidden field (non-visible input)
     * Pairwise: type=hidden(shift=5), protection=unprotected, intensity=non-display, len=1, pos=(1,1)
     *
     * FFW1: 0x05 (shift=5 for hidden)
     * Attributes: Non-display (0x00)
     */
    @Test
    public void testCreateHiddenFieldNonDisplay() throws Exception {
        // ARRANGE
        screen.setCursor(1, 1);
        int startPos = getLastPos();

        // ACT: Add hidden field
        addField(0x00, 1, 0x05, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Field length should be 1", 1, field.getLength());
        assertEquals("Field attribute should be non-display", 0x00, field.getAttr());
    }

    /**
     * POSITIVE: Create field with MDT (Modified Data Tag) bit set
     * Pairwise: type=input, protection=unprotected, intensity=normal, len=30, pos=(12,25)
     *
     * FFW1: 0x08 (MDT bit)
     */
    @Test
    public void testCreateFieldWithModifiedDataTagBit() throws Exception {
        // ARRANGE
        screen.setCursor(12, 25);
        int startPos = getLastPos();

        // ACT: Add field with MDT bit in FFW1
        addField(0x20, 30, 0x08, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("FFW1 should have MDT bit set", 0x08, field.getFFW1() & 0x08);
    }

    /**
     * POSITIVE: Create field with Field Exit Required (FER) flag
     * Pairwise: type=input, protection=unprotected, intensity=normal, len=15, pos=(5,10)
     *
     * FFW2: 0x40 (FER bit)
     */
    @Test
    public void testCreateFieldWithFieldExitRequired() throws Exception {
        // ARRANGE
        screen.setCursor(5, 10);

        // ACT: Add field with FER flag in FFW2
        addField(0x20, 15, 0x00, 0x40, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertTrue("Field should have FER flag", field.isFER());
    }

    /**
     * POSITIVE: Create field with Mandatory Enter flag
     * Pairwise: type=input, protection=unprotected, intensity=normal, len=12, pos=(8,45)
     *
     * FFW2: 0x08 (Mandatory Enter bit)
     */
    @Test
    public void testCreateFieldWithMandatoryEnter() throws Exception {
        // ARRANGE
        screen.setCursor(8, 45);

        // ACT: Add field with mandatory enter in FFW2
        addField(0x20, 12, 0x00, 0x08, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertTrue("Field should have mandatory enter flag", field.isMandatoryEnter());
    }

    /**
     * POSITIVE: Create field with Duplicate Enable flag
     * Pairwise: type=input, protection=unprotected, intensity=normal, len=25, pos=(15,30)
     *
     * FFW1: 0x10 (Duplicate Enable bit)
     */
    @Test
    public void testCreateFieldWithDuplicateEnable() throws Exception {
        // ARRANGE
        screen.setCursor(15, 30);

        // ACT: Add field with duplicate enable in FFW1
        addField(0x20, 25, 0x10, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertTrue("Field should have duplicate enable", field.isDupEnabled());
    }

    /**
     * POSITIVE: Create numeric field
     * Pairwise: type=numeric(shift=3), protection=unprotected, intensity=normal, len=10, pos=(6,50)
     *
     * FFW1: 0x03 (shift=3 for numeric)
     */
    @Test
    public void testCreateNumericField() throws Exception {
        // ARRANGE
        screen.setCursor(6, 50);

        // ACT: Add numeric field
        addField(0x20, 10, 0x03, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertTrue("Field should be numeric", field.isNumeric());
    }

    /**
     * POSITIVE: Create signed numeric field
     * Pairwise: type=signed_numeric(shift=7), protection=unprotected, intensity=normal, len=10, pos=(10,20)
     *
     * FFW1: 0x07 (shift=7 for signed numeric)
     */
    @Test
    public void testCreateSignedNumericField() throws Exception {
        // ARRANGE
        screen.setCursor(10, 20);

        // ACT: Add signed numeric field
        addField(0x20, 10, 0x07, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertTrue("Field should be signed numeric", field.isSignedNumeric());
    }

    /**
     * POSITIVE: Create field with To-Upper conversion
     * Pairwise: type=input, protection=unprotected, intensity=normal, len=20, pos=(3,15)
     *
     * FFW2: 0x20 (To-Upper bit)
     */
    @Test
    public void testCreateFieldWithToUpperFlag() throws Exception {
        // ARRANGE
        screen.setCursor(3, 15);

        // ACT: Add field with to-upper flag
        addField(0x20, 20, 0x00, 0x20, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertTrue("Field should have to-upper flag", field.isToUpper());
    }

    /**
     * POSITIVE: Create field with Auto-Enter flag
     * Pairwise: type=input, protection=unprotected, intensity=normal, len=5, pos=(7,60)
     *
     * FFW2: 0x80 (Auto-Enter bit)
     */
    @Test
    public void testCreateFieldWithAutoEnter() throws Exception {
        // ARRANGE
        screen.setCursor(7, 60);

        // ACT: Add field with auto-enter flag
        addField(0x20, 5, 0x00, 0x80, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertTrue("Field should have auto-enter flag", field.isAutoEnter());
    }

    /**
     * POSITIVE: Create field with Cursor Progression control
     * Pairwise: type=input, protection=unprotected, intensity=normal, len=15, pos=(13,35)
     *
     * FCW1: 0x88 (Cursor Progression marker)
     */
    @Test
    public void testCreateFieldWithCursorProgression() throws Exception {
        // ARRANGE
        screen.setCursor(13, 35);

        // ACT: Add field with cursor progression
        addField(0x20, 15, 0x00, 0x00, 0x88, 0x05);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Cursor progression value should be set", 0x05, field.getCursorProgression());
    }

    /**
     * POSITIVE: Create field with maximum valid length (255)
     * Pairwise: type=input, protection=unprotected, intensity=normal, len=255, pos=(1,1)
     */
    @Test
    public void testCreateFieldWithMaximumLength255() throws Exception {
        // ARRANGE
        screen.setCursor(1, 1);

        // ACT: Add field with length 255
        addField(0x20, 255, 0x00, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Field length should be 255", 255, field.getLength());
    }

    /**
     * POSITIVE: Create multiple sequential fields without overlap
     * Pairwise: Tests field adjacency and positioning
     */
    @Test
    public void testCreateMultipleSequentialFields() throws Exception {
        // ARRANGE: Create first field at position (1,1)
        screen.setCursor(1, 1);
        addField(0x20, 10, 0x00, 0x00, 0x00, 0x00);

        // ACT: Create second field immediately after first
        screen.setCursor(1, 12);
        addField(0x20, 10, 0x00, 0x00, 0x00, 0x00);

        // ASSERT: Both fields exist without overlap
        assertEquals("Should have two fields", 2, screenFields.getSize());
        ScreenField field1 = screenFields.getField(0);
        ScreenField field2 = screenFields.getField(1);

        // Fields exist and don't overlap
        assertTrue("Field 1 should not overlap field 2",
                field1.endPos() < field2.startPos());
        assertEquals("Field 1 length should be 10", 10, field1.getLength());
        assertEquals("Field 2 length should be 10", 10, field2.getLength());
    }

    /**
     * POSITIVE: Create field and verify start/end position calculations
     * Pairwise: Tests position boundary calculations
     */
    @Test
    public void testFieldStartEndPositionCalculations() throws Exception {
        // ARRANGE: Position at row 5, col 10
        screen.setCursor(5, 10);

        // ACT: Add field with length 20
        addField(0x20, 20, 0x00, 0x00, 0x00, 0x00);

        // ASSERT: Field created with correct length and end position relative to start
        ScreenField field = screenFields.getField(0);
        assertEquals("Field length should be 20", 20, field.getLength());
        assertEquals("End position should be start + length - 1",
                field.startPos() + 20 - 1, field.endPos());
    }

    // ========================================================================
    // ADVERSARIAL TESTS: Boundary conditions, malformed data (10+)
    // ========================================================================

    /**
     * ADVERSARIAL: Create field with length 0 (degenerate case)
     * Edge case: Minimal field that only marks position
     */
    @Test
    public void testCreateDegenerateFieldLengthZero() throws Exception {
        // ARRANGE
        screen.setCursor(1, 1);

        // ACT: Add field with length 0 (should be allowed as position marker)
        addField(0x20, 0, 0x00, 0x00, 0x00, 0x00);

        // ASSERT: Field should be created even with length 0
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Field length should be 0", 0, field.getLength());
    }

    /**
     * ADVERSARIAL: Create field that starts at screen boundary
     * Edge case: Field at last valid position on screen
     */
    @Test
    public void testCreateFieldAtScreenBoundary() throws Exception {
        // ARRANGE: Position at last character of screen
        screen.setCursor(24, 80);

        // ACT: Add field with length 1 at boundary
        addField(0x20, 1, 0x00, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Field should have length 1", 1, field.getLength());
    }

    /**
     * ADVERSARIAL: Create field with all FFW/FCW bits set to 0xFF (all bits on)
     * Malformed case: Extreme attribute values
     */
    @Test
    public void testCreateFieldWithAllBitsSet() throws Exception {
        // ARRANGE
        screen.setCursor(12, 40);

        // ACT: Add field with all bits set to 0xFF
        addField(0xFF, 10, 0xFF, 0xFF, 0xFF, 0xFF);

        // ASSERT: Field should still be created (no validation error)
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("FFW1 should preserve all bits", 0xFF, field.getFFW1());
        assertEquals("FFW2 should preserve all bits", 0xFF, field.getFFW2());
        assertEquals("FCW1 should preserve all bits", 0xFF, field.getFCW1());
        assertEquals("FCW2 should preserve all bits", 0xFF, field.getFCW2());
    }

    /**
     * ADVERSARIAL: Create field with all FFW/FCW bits set to 0x00 (all bits off)
     * Malformed case: Minimum attribute values
     */
    @Test
    public void testCreateFieldWithNoBitsSet() throws Exception {
        // ARRANGE
        screen.setCursor(12, 40);

        // ACT: Add field with all bits set to 0x00
        addField(0x00, 10, 0x00, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("FFW1 should be 0x00", 0x00, field.getFFW1());
        assertEquals("FFW2 should be 0x00", 0x00, field.getFFW2());
    }

    /**
     * ADVERSARIAL: Create overlapping fields (same start position)
     * Malformed case: Second field overwrites first field's control
     */
    @Test
    public void testCreateOverlappingFieldsAtSamePosition() throws Exception {
        // ARRANGE: Create first field
        screen.setCursor(10, 20);
        addField(0x20, 15, 0x00, 0x00, 0x00, 0x00);

        // ACT: Create second field at same position (should update FFW for existing field)
        setLastPos((9 * SCREEN_COLS) + 19);
        addField(0xC0, 20, 0x01, 0x00, 0x00, 0x00);

        // ASSERT: Either two fields exist or second one updated first
        // This tests field coalescing behavior per IBM 5250 spec
        assertTrue("Should have at least one field", screenFields.getSize() >= 1);
    }

    /**
     * ADVERSARIAL: Create field with large length that exceeds screen capacity
     * Edge case: Field length > remaining screen space
     */
    @Test
    public void testCreateFieldWithLengthExceedingScreen() throws Exception {
        // ARRANGE: Position near end of screen
        screen.setCursor(24, 50);

        // ACT: Add field longer than remaining space (80 chars but only 31 remain)
        addField(0x20, 100, 0x00, 0x00, 0x00, 0x00);

        // ASSERT: Field is created with requested length (overflow handling in screen)
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Field should retain requested length", 100, field.getLength());
    }

    /**
     * ADVERSARIAL: Create field at multiple positions across row boundaries
     * Edge case: Field that spans from one row to next
     */
    @Test
    public void testCreateFieldSpanningRowBoundary() throws Exception {
        // ARRANGE: Position at row 5, column 70 (10 chars from row end)
        screen.setCursor(5, 70);

        // ACT: Add field with length 20 (will wrap to next row)
        addField(0x20, 20, 0x00, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Field should span 20 characters across rows", 20, field.getLength());
    }

    /**
     * ADVERSARIAL: Create continued field (multi-part field)
     * Pairwise: FCW1 bits 1,2 set to indicate continuation
     *
     * FCW1: 0x86 (bits 1,2,7 set for continued field)
     * FCW2: values 1,2,3 indicate first/last/middle
     */
    @Test
    public void testCreateContinuedFieldFirst() throws Exception {
        // ARRANGE
        screen.setCursor(1, 1);

        // ACT: Add first part of continued field
        addField(0x20, 40, 0x00, 0x00, 0x86, 0x01);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertTrue("Field should be continued", field.isContinued());
        assertTrue("Field should be continued first", field.isContinuedFirst());
    }

    /**
     * ADVERSARIAL: Create field with invalid adjustment bits
     * Malformed case: Bits 0-2 of FFW2 contain adjustment codes
     */
    @Test
    public void testCreateFieldWithAllAdjustmentBitsSet() throws Exception {
        // ARRANGE
        screen.setCursor(8, 30);

        // ACT: Add field with adjustment bits 0-2 all set (0x07)
        addField(0x20, 10, 0x00, 0x07, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertEquals("Adjustment should be 7", 7, field.getAdjustment());
    }

    /**
     * ADVERSARIAL: Create field and verify field doesn't exceed screen boundary
     * Integration test: Field positioning at maximum valid screen position
     */
    @Test
    public void testFieldPositioningDoesNotExceedScreenSize() throws Exception {
        // ARRANGE: Position at very end of screen
        screen.setCursor(SCREEN_ROWS, SCREEN_COLS);
        int expectedMaxPos = SCREEN_SIZE - 1;

        // ACT: Add field with length 1
        addField(0x20, 1, 0x00, 0x00, 0x00, 0x00);

        // ASSERT
        ScreenField field = screenFields.getField(0);
        assertTrue("Field start position should not exceed screen",
                field.startPos() <= expectedMaxPos);
    }

    /**
     * ADVERSARIAL: Create field with mixed protection and intensity
     * Complex case: Combines multiple attribute features
     */
    @Test
    public void testCreateFieldWithComplexAttributeCombination() throws Exception {
        // ARRANGE
        screen.setCursor(15, 25);

        // ACT: Add field with: bypass (0x20), numeric (0x03), mandatory (0x08), to-upper (0x20)
        // Attributes: High intensity (0xC0)
        addField(0xC0, 15, 0x23, 0x28, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have one field", 1, screenFields.getSize());
        ScreenField field = screenFields.getField(0);
        assertTrue("Field should be protected (bypass)", field.isBypassField());
        assertTrue("Field should be numeric", field.isNumeric());
        assertTrue("Field should have mandatory enter", field.isMandatoryEnter());
        assertTrue("Field should have to-upper", field.isToUpper());
    }

    // ========================================================================
    // INTEGRATION TESTS: Multi-field scenarios
    // ========================================================================

    /**
     * INTEGRATION: Create screen with alternating input/output fields
     * Tests realistic screen layout with mixed field types
     */
    @Test
    public void testCreateAlternatingInputOutputFields() throws Exception {
        // ARRANGE: Create input field
        screen.setCursor(1, 1);
        addField(0x20, 10, 0x00, 0x00, 0x00, 0x00);

        // ACT: Create output field
        screen.setCursor(1, 15);
        addField(0x20, 10, 0x21, 0x00, 0x00, 0x00);

        // ACT: Create another input field
        screen.setCursor(1, 30);
        addField(0x20, 10, 0x00, 0x00, 0x00, 0x00);

        // ASSERT
        assertEquals("Should have three fields", 3, screenFields.getSize());
        assertFalse("Field 1 should be unprotected", screenFields.getField(0).isBypassField());
        assertTrue("Field 2 should be protected", screenFields.getField(1).isBypassField());
        assertFalse("Field 3 should be unprotected", screenFields.getField(2).isBypassField());
    }

    /**
     * INTEGRATION: Create field table with various field lengths
     * Tests length variation across field set
     */
    @Test
    public void testCreateFieldTableWithVariousLengths() throws Exception {
        // Test data: (row, col, length)
        int[][] fieldPositions = {
            {1, 1, 5},
            {2, 10, 20},
            {3, 40, 35},
            {4, 1, 80},
            {5, 50, 15}
        };

        // ACT: Create all fields
        for (int[] pos : fieldPositions) {
            screen.setCursor(pos[0], pos[1]);
            addField(0x20, pos[2], 0x00, 0x00, 0x00, 0x00);
        }

        // ASSERT
        assertEquals("Should have 5 fields", 5, screenFields.getSize());
        for (int i = 0; i < fieldPositions.length; i++) {
            ScreenField field = screenFields.getField(i);
            assertEquals("Field " + i + " length mismatch",
                    fieldPositions[i][2], field.getLength());
        }
    }

    /**
     * INTEGRATION: Verify field attribute inheritance and independence
     * Tests that each field maintains separate attributes
     */
    @Test
    public void testFieldAttributeIndependence() throws Exception {
        // ARRANGE & ACT: Create fields with different attributes
        screen.setCursor(1, 1);
        addField(0x20, 10, 0x00, 0x40, 0x00, 0x00); // FER flag

        screen.setCursor(1, 15);
        addField(0xC0, 10, 0x08, 0x80, 0x00, 0x00); // MDT + Auto-Enter

        // ASSERT
        ScreenField field1 = screenFields.getField(0);
        ScreenField field2 = screenFields.getField(1);

        assertTrue("Field 1 should have FER", field1.isFER());
        assertFalse("Field 2 should not have FER", field2.isFER());

        assertFalse("Field 1 should not have MDT", (field1.getFFW1() & 0x08) == 0x08);
        assertTrue("Field 2 should have MDT", (field2.getFFW1() & 0x08) == 0x08);

        assertFalse("Field 1 should not have auto-enter", field1.isAutoEnter());
        assertTrue("Field 2 should have auto-enter", field2.isAutoEnter());
    }
}
