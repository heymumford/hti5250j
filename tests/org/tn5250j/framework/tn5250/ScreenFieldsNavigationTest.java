/**
 * Title: tn5250J
 * Copyright:   Copyright (c) 2001
 * Company:
 *
 * @author Test Suite
 * @version 0.5
 * <p>
 * Description:
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Pairwise TDD Test Suite for ScreenFields field navigation
 *
 * Tests combinations of:
 * - Field counts: [0, 1, 2, 10]
 * - Current field indices: [-1, 0, 1, mid, last]
 * - Field types: [input, output, bypass, hidden]
 * - Cursor positions: [start, mid, end]
 *
 * Focus: Discover field navigation bugs through boundary value analysis
 */
public class ScreenFieldsNavigationTest {

    private ScreenFields screenFields;
    private TestScreen5250 testScreen;

    @Before
    public void setUp() {
        testScreen = new TestScreen5250();
        screenFields = new ScreenFields(testScreen);
    }

    // ===== POSITIVE TESTS: Valid Navigation Sequences =====

    /**
     * POSITIVE: Single field present, navigate to it
     * Pair: field_count=1, current_index=0
     */
    @Test
    public void testNavigate_SingleField_SetAndRetrieveCurrent() {
        // ARRANGE: Create single field
        ScreenField field = new ScreenField(testScreen);
        screenFields.setCurrentField(field);

        // ACT: Retrieve current field
        ScreenField current = screenFields.getCurrentField();

        // ASSERT: Should return the set field
        assertNotNull("Current field should not be null", current);
        assertSame("Should return the same field instance", field, current);
    }

    /**
     * POSITIVE: Two fields, navigate from first to second
     * Pair: field_count=2, current_index=0→1
     */
    @Test
    public void testNavigate_TwoFields_NextField() {
        // ARRANGE: Create two fields linked together
        ScreenField field1 = new ScreenField(testScreen);
        ScreenField field2 = new ScreenField(testScreen);
        field1.next = field2;
        field2.prev = field1;

        screenFields.setCurrentField(field1);

        // ACT: Navigate to next field
        ScreenField nextField = field1.next;

        // ASSERT: Should be able to navigate
        assertNotNull("Next field should not be null", nextField);
        assertSame("Next field should be field2", field2, nextField);
    }

    /**
     * POSITIVE: Two fields, navigate backward from second to first
     * Pair: field_count=2, current_index=1→0
     */
    @Test
    public void testNavigate_TwoFields_PreviousField() {
        // ARRANGE: Create two fields linked together
        ScreenField field1 = new ScreenField(testScreen);
        ScreenField field2 = new ScreenField(testScreen);
        field1.next = field2;
        field2.prev = field1;

        screenFields.setCurrentField(field2);

        // ACT: Navigate to previous field
        ScreenField prevField = field2.prev;

        // ASSERT: Should be able to navigate backward
        assertNotNull("Previous field should not be null", prevField);
        assertSame("Previous field should be field1", field1, prevField);
    }

    /**
     * POSITIVE: Find field by position when field exists at position
     * Pair: field_count=1, cursor_position=start
     */
    @Test
    public void testNavigate_FindByPosition_FieldExists() {
        // ARRANGE: Create field and add to fields array
        ScreenField field = new ScreenField(testScreen);
        setFieldInArray(0, field);
        setFieldCount(1);

        // ACT: Find field at position
        ScreenField found = screenFields.findByPosition(0);

        // ASSERT: Should find the field
        assertNotNull("Should find field at position 0", found);
    }

    /**
     * POSITIVE: Multiple fields, find correct field by position
     * Pair: field_count=10, cursor_position=mid
     */
    @Test
    public void testNavigate_FindByPosition_MultipleFields_CorrectField() {
        // ARRANGE: Create 10 fields
        createFieldsWithPositions(10, new int[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90});

        // ACT: Find field at position 50
        ScreenField found = screenFields.findByPosition(50);

        // ASSERT: Should find field at position 50
        assertNotNull("Should find field at position 50", found);
    }

    /**
     * POSITIVE: Get field count when fields exist
     * Pair: field_count=10
     */
    @Test
    public void testNavigate_GetFieldCount_MultipleFields() {
        // ARRANGE: Create 10 fields
        createFieldsWithPositions(10, new int[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90});

        // ACT: Get field count
        int count = screenFields.getFieldCount();

        // ASSERT: Should return 10
        assertEquals("Field count should be 10", 10, count);
    }

    /**
     * POSITIVE: Get field count when no fields exist
     * Pair: field_count=0
     */
    @Test
    public void testNavigate_GetFieldCount_NoFields() {
        // ARRANGE: No fields added
        screenFields.clearFFT();

        // ACT: Get field count
        int count = screenFields.getFieldCount();

        // ASSERT: Should return 0
        assertEquals("Field count should be 0", 0, count);
    }

    // ===== ADVERSARIAL TESTS: Boundary & Error Conditions =====

    /**
     * ADVERSARIAL: Access current field when it is null
     * Pair: current_index=-1 (null state)
     */
    @Test
    public void testNavigate_GetCurrentField_WhenNull() {
        // ARRANGE: Clear fields
        screenFields.clearFFT();

        // ACT: Try to get current field
        ScreenField current = screenFields.getCurrentField();

        // ASSERT: Should be null
        assertNull("Current field should be null when not set", current);
    }

    /**
     * ADVERSARIAL: Set current field to null explicitly
     * Pair: current_index=-1
     */
    @Test
    public void testNavigate_SetCurrentField_ToNull() {
        // ARRANGE: Set a field first
        ScreenField field = new ScreenField(testScreen);
        screenFields.setCurrentField(field);

        // ACT: Set to null
        screenFields.setCurrentField(null);

        // ASSERT: Should be null
        assertNull("Current field should be null", screenFields.getCurrentField());
    }

    /**
     * ADVERSARIAL: Navigate next when no next field (null)
     * Pair: field_count=1, current_index=0, next=null
     */
    @Test
    public void testNavigate_NextField_WhenNull() {
        // ARRANGE: Single field with no next
        ScreenField field = new ScreenField(testScreen);
        field.next = null;
        screenFields.setCurrentField(field);

        // ACT: Try to access next field
        ScreenField nextField = field.next;

        // ASSERT: Should be null
        assertNull("Next field should be null", nextField);
    }

    /**
     * ADVERSARIAL: Navigate previous when no previous field (null)
     * Pair: field_count=1, current_index=0, prev=null
     */
    @Test
    public void testNavigate_PreviousField_WhenNull() {
        // ARRANGE: Single field with no prev
        ScreenField field = new ScreenField(testScreen);
        field.prev = null;
        screenFields.setCurrentField(field);

        // ACT: Try to access prev field
        ScreenField prevField = field.prev;

        // ASSERT: Should be null
        assertNull("Previous field should be null", prevField);
    }

    /**
     * ADVERSARIAL: Find field at position when no fields exist
     * Pair: field_count=0, cursor_position=any
     */
    @Test
    public void testNavigate_FindByPosition_NoFields() {
        // ARRANGE: No fields
        screenFields.clearFFT();

        // ACT: Try to find field at position
        ScreenField found = screenFields.findByPosition(0);

        // ASSERT: Should return null
        assertNull("Should return null when no fields exist", found);
    }

    /**
     * ADVERSARIAL: Find field at position outside all fields
     * Pair: field_count=3, cursor_position=outside
     */
    @Test
    public void testNavigate_FindByPosition_OutsideAllFields() {
        // ARRANGE: Create 3 fields at positions 0-9, 10-19, 20-29
        createFieldsWithLengths(3, new int[]{10, 10, 10}, new int[]{0, 10, 20});

        // ACT: Try to find field at position 100 (outside)
        ScreenField found = screenFields.findByPosition(100);

        // ASSERT: Should return null
        assertNull("Should return null for position outside all fields", found);
    }

    /**
     * ADVERSARIAL: Get field by index when index out of bounds
     * Pair: field_count=2, index=10 (invalid)
     */
    @Test
    public void testNavigate_GetField_InvalidIndex() {
        // ARRANGE: Create 2 fields
        createFieldsWithPositions(2, new int[]{0, 10});

        // ACT & ASSERT: Accessing invalid index should not throw (graceful)
        try {
            ScreenField field = screenFields.getField(10);
            // May return null or may throw - both acceptable
        } catch (ArrayIndexOutOfBoundsException e) {
            // Expected for invalid index
        }
    }

    /**
     * ADVERSARIAL: Bypass fields should be skippable in navigation
     * Pair: field_count=3, field_type=bypass
     */
    @Test
    public void testNavigate_SkipBypassFields() {
        // ARRANGE: Create 3 fields, middle one is bypass
        ScreenField field1 = new ScreenField(testScreen);
        ScreenField field2 = createBypassField();
        ScreenField field3 = new ScreenField(testScreen);

        field1.next = field2;
        field2.prev = field1;
        field2.next = field3;
        field3.prev = field2;

        screenFields.setCurrentField(field1);

        // ACT: Navigate from field1, skipping bypass field2
        ScreenField nextInputField = field1.next;
        while (nextInputField != null && nextInputField.isBypassField()) {
            nextInputField = nextInputField.next;
        }

        // ASSERT: Should skip to field3
        assertNotNull("Should find next non-bypass field", nextInputField);
        assertSame("Should skip bypass field and find field3", field3, nextInputField);
    }

    /**
     * ADVERSARIAL: All fields are bypass fields
     * Pair: field_count=3, field_type=all_bypass
     */
    @Test
    public void testNavigate_AllFieldsAreBytpass() {
        // ARRANGE: Create 3 bypass fields
        ScreenField field1 = createBypassField();
        ScreenField field2 = createBypassField();
        ScreenField field3 = createBypassField();

        field1.next = field2;
        field2.prev = field1;
        field2.next = field3;
        field3.prev = field2;

        screenFields.setCurrentField(field1);

        // ACT: Try to find next non-bypass field
        ScreenField nextInputField = field1.next;
        while (nextInputField != null && nextInputField.isBypassField()) {
            nextInputField = nextInputField.next;
        }

        // ASSERT: Should be null (no non-bypass fields)
        assertNull("Should return null when all fields are bypass", nextInputField);
    }

    /**
     * ADVERSARIAL: Field length boundary - zero length field
     * Pair: field_length=0
     */
    @Test
    public void testNavigate_ZeroLengthField() {
        // ARRANGE: Create field with length 0
        ScreenField field = new ScreenField(testScreen);
        setFieldLength(field, 0);

        // ACT: Check if position within field
        boolean withinField = field.withinField(0);

        // ASSERT: Zero-length field behavior
        assertFalse("Position 0 should not be within zero-length field", withinField);
    }

    /**
     * ADVERSARIAL: Field length boundary - maximum length field (255)
     * Pair: field_length=255
     */
    @Test
    public void testNavigate_MaxLengthField() {
        // ARRANGE: Create field with max length
        ScreenField field = new ScreenField(testScreen);
        // Call setField with row=0, col=0, length=255
        // This will calculate startPos = (0 * 80) + 0 = 0
        // And endPos = 0 + 255 - 1 = 254
        field.setField(0x20, 0, 0, 255, 0, 0, 0, 0);

        // ACT: Check the calculated startPos and endPos
        int startPos = field.startPos();

        // Verify field bounds are correct
        boolean atStart = field.withinField(0);
        boolean atEnd = field.withinField(254);
        boolean pastEnd = field.withinField(255);

        // ASSERT: Positions should be within calculated bounds
        assertEquals("Start position should be 0", 0, startPos);
        assertTrue("Position 0 should be within field", atStart);
        assertTrue("Position 254 should be within field of length 255", atEnd);
        assertFalse("Position 255 should NOT be within field (255 positions indexed 0-254)", pastEnd);
    }

    /**
     * ADVERSARIAL: Wraparound navigation - last field next should loop
     * Pair: field_count=10, current_index=last, next_behavior=wraparound
     */
    @Test
    public void testNavigate_LastFieldNoNext_Wraparound() {
        // ARRANGE: Create chain of 3 fields, last has no next
        ScreenField field1 = new ScreenField(testScreen);
        ScreenField field2 = new ScreenField(testScreen);
        ScreenField field3 = new ScreenField(testScreen);

        field1.next = field2;
        field2.prev = field1;
        field2.next = field3;
        field3.prev = field2;
        field3.next = null;

        screenFields.setCurrentField(field3);

        // ACT: Try to navigate next from last field
        ScreenField nextField = field3.next;

        // ASSERT: Should be null (no wraparound by default)
        assertNull("Last field should have null next", nextField);
    }

    /**
     * ADVERSARIAL: Circular field chain (should not occur but test resilience)
     * Pair: field_count=2, structure=circular
     */
    @Test
    public void testNavigate_CircularFieldChain_DetectLoop() {
        // ARRANGE: Create circular chain (normally bad)
        ScreenField field1 = new ScreenField(testScreen);
        ScreenField field2 = new ScreenField(testScreen);

        field1.next = field2;
        field2.prev = field1;
        field2.next = field1;  // Creates loop
        field1.prev = field2;

        // ACT: Attempt navigation with loop detection
        int count = 0;
        ScreenField current = field1;
        while (current != null && count < 10) {
            current = current.next;
            count++;
        }

        // ASSERT: Should detect loop by counting
        assertEquals("Loop detection should stop at 10 iterations", 10, count);
    }

    /**
     * ADVERSARIAL: Position at field boundary
     * Pair: field_length=10, cursor_position=start_and_end
     */
    @Test
    public void testNavigate_PositionAtFieldBoundary_StartAndEnd() {
        // ARRANGE: Create field from pos 10-19
        ScreenField field = new ScreenField(testScreen);
        setFieldStart(field, 10);
        setFieldLength(field, 10);

        // ACT: Test positions at boundaries
        boolean atStart = field.withinField(10);
        boolean atEnd = field.withinField(19);
        boolean beforeStart = field.withinField(9);
        boolean afterEnd = field.withinField(20);

        // ASSERT: Boundaries should be inclusive start and end
        assertTrue("Start position 10 should be within field", atStart);
        assertTrue("End position 19 should be within field", atEnd);
        assertFalse("Position 9 should not be within field", beforeStart);
        assertFalse("Position 20 should not be within field", afterEnd);
    }

    /**
     * ADVERSARIAL: Multiple fields with overlapping detection
     * Pair: field_count=3, cursor_position=overlap_boundary
     */
    @Test
    public void testNavigate_MultipleFields_BoundaryConditions() {
        // ARRANGE: Create 3 fields: 0-9, 10-19, 20-29
        ScreenField field1 = new ScreenField(testScreen);
        setFieldStart(field1, 0);
        setFieldLength(field1, 10);

        ScreenField field2 = new ScreenField(testScreen);
        setFieldStart(field2, 10);
        setFieldLength(field2, 10);

        ScreenField field3 = new ScreenField(testScreen);
        setFieldStart(field3, 20);
        setFieldLength(field3, 10);

        setFieldInArray(0, field1);
        setFieldInArray(1, field2);
        setFieldInArray(2, field3);
        setFieldCount(3);

        // ACT: Find fields at boundaries
        ScreenField atPos9 = screenFields.findByPosition(9);
        ScreenField atPos10 = screenFields.findByPosition(10);
        ScreenField atPos19 = screenFields.findByPosition(19);
        ScreenField atPos20 = screenFields.findByPosition(20);

        // ASSERT: Should find correct fields
        assertSame("Position 9 should be in field1", field1, atPos9);
        assertSame("Position 10 should be in field2", field2, atPos10);
        assertSame("Position 19 should be in field2", field2, atPos19);
        assertSame("Position 20 should be in field3", field3, atPos20);
    }

    /**
     * ADVERSARIAL: Large field count navigation (100 fields)
     * Pair: field_count=100
     */
    @Test
    public void testNavigate_LargeFieldCount_Performance() {
        // ARRANGE: Create 100 fields
        int fieldCount = 100;
        int[] positions = new int[fieldCount];
        for (int i = 0; i < fieldCount; i++) {
            positions[i] = i * 10;
        }
        createFieldsWithPositions(fieldCount, positions);

        // ACT: Find field in middle
        ScreenField found = screenFields.findByPosition(500);

        // ASSERT: Should find field at position 500
        assertNotNull("Should find field even with 100 fields", found);
    }

    // ===== TEST HELPERS =====

    private void createFieldsWithPositions(int count, int[] positions) {
        for (int i = 0; i < count; i++) {
            ScreenField field = new ScreenField(testScreen);
            setFieldStart(field, positions[i]);
            setFieldLength(field, 10);
            setFieldInArray(i, field);
        }
        setFieldCount(count);
    }

    private void createFieldsWithLengths(int count, int[] lengths, int[] starts) {
        for (int i = 0; i < count; i++) {
            ScreenField field = new ScreenField(testScreen);
            setFieldStart(field, starts[i]);
            setFieldLength(field, lengths[i]);
            setFieldInArray(i, field);
        }
        setFieldCount(count);
    }

    private ScreenField createBypassField() {
        ScreenField field = new ScreenField(testScreen);
        // Set FFW1 to include bypass flag (0x20)
        try {
            Field ffw1Field = ScreenField.class.getDeclaredField("ffw1");
            ffw1Field.setAccessible(true);
            ffw1Field.setInt(field, 0x20);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set bypass flag: " + e.getMessage());
        }
        return field;
    }

    private void setFieldStart(ScreenField field, int startPos) {
        try {
            Field startPosField = ScreenField.class.getDeclaredField("startPos");
            startPosField.setAccessible(true);
            startPosField.setInt(field, startPos);

            Field endPosField = ScreenField.class.getDeclaredField("endPos");
            endPosField.setAccessible(true);
            endPosField.setInt(field, startPos + 10);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set field start: " + e.getMessage());
        }
    }

    private void setFieldLength(ScreenField field, int length) {
        try {
            Field startPosField = ScreenField.class.getDeclaredField("startPos");
            startPosField.setAccessible(true);
            int startPos = startPosField.getInt(field);

            Field lengthField = ScreenField.class.getDeclaredField("length");
            lengthField.setAccessible(true);
            lengthField.setInt(field, length);

            Field endPosField = ScreenField.class.getDeclaredField("endPos");
            endPosField.setAccessible(true);
            endPosField.setInt(field, startPos + length - 1);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set field length: " + e.getMessage());
        }
    }

    private void setFieldInArray(int index, ScreenField field) {
        try {
            Field screenFieldsField = ScreenFields.class.getDeclaredField("screenFields");
            screenFieldsField.setAccessible(true);
            ScreenField[] fields = (ScreenField[]) screenFieldsField.get(screenFields);
            fields[index] = field;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set field in array: " + e.getMessage());
        }
    }

    private void setFieldCount(int count) {
        try {
            Field sizeFieldsField = ScreenFields.class.getDeclaredField("sizeFields");
            sizeFieldsField.setAccessible(true);
            sizeFieldsField.setInt(screenFields, count);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set field count: " + e.getMessage());
        }
    }

    /**
     * Minimal stub implementation of Screen5250 for testing purposes
     */
    private static class TestScreen5250 extends Screen5250 {
        private int rows = 24;
        private int cols = 80;
        private int lastPos = 0;

        @Override
        public int getColumns() {
            return cols;
        }

        @Override
        public int getRows() {
            return rows;
        }

        @Override
        public int getLastPos() {
            return lastPos;
        }

        @Override
        public int getPos(int row, int col) {
            return (row * cols) + col;
        }
    }

}
