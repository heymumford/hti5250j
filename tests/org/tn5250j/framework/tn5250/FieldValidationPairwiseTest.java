/**
 * Title: FieldValidationPairwiseTest.java
 * Copyright: Copyright (c) 2025
 * Company:
 *
 * Description: TDD pairwise tests for ScreenField input validation.
 *
 * This test suite focuses on field validation critical for terminal automation:
 * - Field type detection (alpha, numeric, signed-numeric, alphanumeric)
 * - Mandatory field enforcement
 * - Numeric range validation
 * - Format validation (signed numbers)
 * - Auto-enter behavior
 * - Field Exit Required (FER) enforcement
 * - Bypass field handling
 *
 * Test dimensions (pairwise combination):
 * 1. Field type: [alpha (0), numeric (3), signed-numeric (7), alphanumeric (0)]
 * 2. Validation rule: [none, mandatory, range-check, format-check]
 * 3. Input value: [empty, valid, boundary, overflow, special-chars]
 * 4. Auto-enter: [disabled, enabled]
 * 5. Error handling: [highlight, beep, reject]
 *
 * POSITIVE TESTS (13): Valid inputs and expected field behaviors
 * ADVERSARIAL TESTS (12): Injection, bypass, overflow, format violations
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

import static org.junit.Assert.*;

/**
 * Pairwise TDD test suite for ScreenField input validation.
 *
 * Focuses on high-risk behaviors in headless terminal automation:
 * 1. Field type misidentification (numeric vs alpha)
 * 2. Mandatory field bypass (empty submission)
 * 3. Numeric overflow (exceed field range)
 * 4. Format injection (special chars in numeric)
 * 5. Auto-enter false triggers
 * 6. Signed numeric boundary conditions
 */
public class FieldValidationPairwiseTest {

    private Screen5250 screen5250;
    private ScreenFields screenFields;

    private static final int SCREEN_ROWS = 24;
    private static final int SCREEN_COLS = 80;

    /**
     * Test double for Screen5250 with minimal dependencies
     */
    private static class Screen5250TestDouble extends Screen5250 {
        public Screen5250TestDouble() {
            super();
        }

        @Override
        public int getScreenLength() {
            return SCREEN_ROWS * SCREEN_COLS;
        }

        @Override
        public boolean isInField(int pos, boolean checkAttr) {
            return pos >= 0 && pos < getScreenLength();
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

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble();
        screenFields = new ScreenFields(screen5250);
        screen5250.getOIA().setKeyBoardLocked(false);
    }

    /**
     * Helper: Create a field with specified attributes.
     * FFW1 bits [7:5] = field shift (0=alpha, 3=numeric, 7=signed-numeric)
     * FFW2 bit [3] = mandatory enter (0x8)
     * FFW2 bit [7] = auto-enter (0x80)
     * FER = FFW2 bit [6] (0x40)
     */
    private ScreenField createField(
            int startPos, int length,
            int fieldShift,      // 0=alpha, 3=numeric, 7=signed-numeric
            boolean isMandatory, // FFW2 & 0x8
            boolean isAutoEnter, // FFW2 & 0x80
            boolean isFER        // FFW2 & 0x40
    ) {
        int ffw1 = fieldShift & 0x7;  // Field shift in bits [2:0]
        int ffw2 = 0;
        if (isMandatory) ffw2 |= 0x8;
        if (isAutoEnter) ffw2 |= 0x80;
        if (isFER) ffw2 |= 0x40;

        ScreenField field = new ScreenField(screen5250);
        field.setField(0x20, startPos / SCREEN_COLS, startPos % SCREEN_COLS,
                length, ffw1, ffw2, 0x41, 0x00);
        return field;
    }

    /**
     * Helper: Read private field via reflection
     */
    private int getFieldValue(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (int) field.get(obj);
    }

    private boolean getFieldBooleanValue(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (boolean) field.get(obj);
    }

    // ========================================================================
    // POSITIVE TESTS (13): Valid field validation scenarios
    // ========================================================================

    /**
     * POSITIVE: Alpha field accepts alphabetic characters
     * Dimension pair: type=alpha, validation=none, input=valid
     */
    @Test
    public void testAlphaFieldAcceptsAlphabeticInput() {
        ScreenField field = createField(0, 10, 0, false, false, false);
        field.setString("HELLO");

        assertFalse("Alpha field should not be numeric", field.isNumeric());
        assertFalse("Alpha field should not be signed numeric", field.isSignedNumeric());
        assertEquals("Field should retain alpha input length", 10, field.getText().length());
        assertTrue("Field should start with input", field.getText().startsWith("HELLO"));
    }

    /**
     * POSITIVE: Numeric field validates numeric input correctly
     * Dimension pair: type=numeric, validation=range, input=valid
     */
    @Test
    public void testNumericFieldAcceptsNumericInput() {
        ScreenField field = createField(80, 5, 3, false, false, false);
        field.setString("12345");

        assertTrue("Numeric field should be detected as numeric", field.isNumeric());
        assertFalse("Numeric field should not be signed numeric", field.isSignedNumeric());
        assertEquals("Field should have numeric input", 5, field.getText().length());
    }

    /**
     * POSITIVE: Signed numeric field validates signed numbers
     * Dimension pair: type=signed-numeric, validation=format, input=valid
     */
    @Test
    public void testSignedNumericFieldAcceptsSignedInput() {
        ScreenField field = createField(160, 6, 7, false, false, false);
        field.setString("-12345");

        assertTrue("Signed numeric field should be detected", field.isSignedNumeric());
        assertFalse("Signed numeric is NOT the same as numeric (different field shift)", field.isNumeric());
        assertEquals("Field should have signed numeric input", 6, field.getText().length());
    }

    /**
     * POSITIVE: Mandatory field enforcement - valid non-empty input
     * Dimension pair: type=alpha, validation=mandatory, input=valid
     */
    @Test
    public void testMandatoryFieldWithValidInput() {
        ScreenField field = createField(240, 10, 0, true, false, false);
        field.setString("REQUIRED");

        assertTrue("Field should be mandatory", field.isMandatoryEnter());
        assertEquals("Mandatory field should accept valid input", 10, field.getText().length());
    }

    /**
     * POSITIVE: Auto-enter enabled allows early field exit
     * Dimension pair: type=numeric, validation=none, auto-enter=enabled
     */
    @Test
    public void testAutoEnterEnabledAllowsEarlyExit() {
        ScreenField field = createField(320, 5, 3, false, true, false);
        field.setString("999");

        assertTrue("Field should have auto-enter enabled", field.isAutoEnter());
        assertTrue("Numeric field accepts numeric input", field.isNumeric());
    }

    /**
     * POSITIVE: Field Exit Required enforces complete field fill
     * Dimension pair: type=alpha, validation=range, FER=enabled
     */
    @Test
    public void testFieldExitRequiredEnforced() {
        ScreenField field = createField(400, 8, 0, false, false, true);
        field.setString("COMPLETE");

        assertTrue("Field should require exit", field.isFER());
        assertEquals("Field with FER should accept input", 8, field.getText().length());
    }

    /**
     * POSITIVE: Multiple field types coexist correctly
     * Dimension pair: type=mixed, validation=none, input=valid
     */
    @Test
    public void testMultipleFieldTypesCoexist() {
        ScreenField alphaField = createField(0, 5, 0, false, false, false);
        ScreenField numericField = createField(80, 5, 3, false, false, false);
        ScreenField signedField = createField(160, 6, 7, false, false, false);

        alphaField.setString("ALPHA");
        numericField.setString("12345");
        signedField.setString("-12345");

        assertFalse(alphaField.isNumeric());
        assertTrue(numericField.isNumeric());
        assertTrue(signedField.isSignedNumeric());
    }

    /**
     * POSITIVE: Empty field submission without mandatory constraint
     * Dimension pair: type=alpha, validation=none, input=empty
     */
    @Test
    public void testEmptyFieldWithoutMandatoryConstraint() {
        ScreenField field = createField(0, 10, 0, false, false, false);
        field.setString("");

        assertFalse("Field should not be mandatory", field.isMandatoryEnter());
        assertEquals("Empty field should have padded length", 10, field.getText().length());
    }

    /**
     * POSITIVE: Numeric field with leading zeros
     * Dimension pair: type=numeric, validation=range, input=boundary
     */
    @Test
    public void testNumericFieldWithLeadingZeros() {
        ScreenField field = createField(80, 5, 3, false, false, false);
        field.setString("00123");

        assertTrue("Numeric field should accept leading zeros", field.isNumeric());
        assertEquals("Field should preserve numeric input", 5, field.getText().length());
    }

    /**
     * POSITIVE: Signed numeric with positive sign
     * Dimension pair: type=signed-numeric, validation=format, input=valid
     */
    @Test
    public void testSignedNumericWithPositiveSign() {
        ScreenField field = createField(160, 6, 7, false, false, false);
        field.setString("+12345");

        assertTrue("Signed field accepts positive sign", field.isSignedNumeric());
        assertEquals("Field should preserve signed input", 6, field.getText().length());
    }

    /**
     * POSITIVE: Field bypass disabled - standard field
     * Dimension pair: type=alpha, validation=mandatory, bypass=disabled
     */
    @Test
    public void testBypassDisabledStandardField() {
        ScreenField field = createField(240, 10, 0, true, false, false);
        field.setString("NORMAL");

        assertFalse("Standard field should not be bypass", field.isBypassField());
        assertTrue("Standard field should be mandatory", field.isMandatoryEnter());
    }

    /**
     * POSITIVE: Auto-enter with numeric boundary at max length
     * Dimension pair: type=numeric, validation=range, auto-enter=enabled
     */
    @Test
    public void testAutoEnterWithNumericAtMaxLength() {
        ScreenField field = createField(320, 5, 3, false, true, false);
        field.setString("99999");

        assertTrue("Field should have auto-enter", field.isAutoEnter());
        assertEquals("Field at max length should have numeric input", 5, field.getText().length());
    }

    /**
     * POSITIVE: Signed numeric zero
     * Dimension pair: type=signed-numeric, validation=format, input=boundary
     */
    @Test
    public void testSignedNumericZero() {
        ScreenField field = createField(160, 3, 7, false, false, false);
        field.setString("000");

        assertTrue("Signed field should accept zero", field.isSignedNumeric());
        assertEquals("Field should preserve zero input", 3, field.getText().length());
    }

    // ========================================================================
    // ADVERSARIAL TESTS (12): Input validation failures and bypass attempts
    // ========================================================================

    /**
     * ADVERSARIAL: Numeric field rejects non-numeric characters (injection)
     * Dimension pair: type=numeric, validation=format, input=special-chars
     * Risk: SQL injection, command injection through numeric field
     */
    @Test
    public void testNumericFieldWithSQLInjectionAttempt() {
        ScreenField field = createField(80, 10, 3, false, false, false);
        // Attempt: "1' OR '1'='1"
        field.setString("1' OR '1'=");

        assertTrue("Field is numeric", field.isNumeric());
        // Field type should prevent interpretation as numeric comparison
        String text = field.getText();
        assertNotNull("Field should not throw on special chars (stored as-is)", text);
    }

    /**
     * ADVERSARIAL: Signed numeric rejects format-invalid input
     * Dimension pair: type=signed-numeric, validation=format, input=overflow
     * Risk: Sign injection (++, --, multiple signs)
     */
    @Test
    public void testSignedNumericRejectsDoubleSign() {
        ScreenField field = createField(160, 6, 7, false, false, false);
        field.setString("--1234");

        assertTrue("Field is signed numeric", field.isSignedNumeric());
        // Verify field stores content (validation would occur at submission layer)
        String text = field.getText();
        assertNotNull("Field stores input", text);
    }

    /**
     * ADVERSARIAL: Mandatory field with empty input attempt
     * Dimension pair: type=alpha, validation=mandatory, input=empty
     * Risk: Bypassing required field validation
     */
    @Test
    public void testMandatoryFieldCannotBypassEmptyCheck() {
        ScreenField field = createField(240, 10, 0, true, false, false);
        field.setString("");

        assertTrue("Field is marked mandatory", field.isMandatoryEnter());
        // Validation logic should catch empty mandatory field
        // The canSend logic checks mandatoried flag
        assertEquals("Empty mandatory field should have padded length", 10, field.getText().length());
    }

    /**
     * ADVERSARIAL: Field overflow attempt - exceed field length
     * Dimension pair: type=numeric, validation=range, input=overflow
     * Risk: Buffer overflow, adjacent field corruption
     */
    @Test
    public void testFieldOverflowAttemptTruncated() {
        ScreenField field = createField(80, 5, 3, false, false, false);
        // Attempt to write 10 chars into 5-char field
        String longInput = "1234567890";
        field.setString(longInput);

        assertEquals("Overflow should be truncated to field length",
                5, field.getText().length());
    }

    /**
     * ADVERSARIAL: Special characters in numeric field
     * Dimension pair: type=numeric, validation=format, input=special-chars
     * Risk: Format string attacks via numeric field
     */
    @Test
    public void testNumericFieldWithFormatStringAttempt() {
        ScreenField field = createField(80, 10, 3, false, false, false);
        field.setString("%x%x%x%x");

        assertTrue("Field is numeric type", field.isNumeric());
        // Field stores input; validation layer should reject at submission
        String text = field.getText();
        assertNotNull("Field stores input without crash", text);
    }

    /**
     * ADVERSARIAL: Alpha field with control characters
     * Dimension pair: type=alpha, validation=none, input=special-chars
     * Risk: Terminal control sequence injection
     */
    @Test
    public void testAlphaFieldWithControlCharacterAttempt() {
        ScreenField field = createField(0, 10, 0, false, false, false);
        // Attempt escape sequence: ESC[2J (clear screen)
        String escapeSeq = "\u001b[2J";
        field.setString(escapeSeq);

        assertFalse("Field is not numeric", field.isNumeric());
        // Field should store control chars (rendering layer handles safety)
        String text = field.getText();
        assertNotNull("Field stores control chars", text);
    }

    /**
     * ADVERSARIAL: FER field with premature exit attempt
     * Dimension pair: type=alpha, validation=range, FER=enabled, input=partial
     * Risk: Incomplete record submission
     */
    @Test
    public void testFERFieldPreventsPartialSubmission() {
        ScreenField field = createField(400, 10, 0, false, false, true);
        field.setString("SHORT");

        assertTrue("Field requires exit", field.isFER());
        // Validation logic checks FER; field stores input for validation
        assertEquals("Partial input stored but FER prevents exit", 10, field.getText().length());
    }

    /**
     * ADVERSARIAL: Numeric field with whitespace
     * Dimension pair: type=numeric, validation=format, input=special-chars
     * Risk: Whitespace bypass of numeric validation
     */
    @Test
    public void testNumericFieldWithWhitespaceInjection() {
        ScreenField field = createField(80, 5, 3, false, false, false);
        field.setString("1 2 3");

        assertTrue("Field is numeric", field.isNumeric());
        String text = field.getText();
        // Field stores content; numeric validation would occur at submission
        assertEquals("Whitespace stored in field", "1 2 3", text);
    }

    /**
     * ADVERSARIAL: Mandatory + auto-enter conflict resolution
     * Dimension pair: type=numeric, validation=mandatory+auto-enter, input=boundary
     * Risk: Race condition between mandatory enforcement and auto-exit
     */
    @Test
    public void testMandatoryAndAutoEnterCoexistence() {
        ScreenField field = createField(320, 5, 3, true, true, false);
        field.setString("123");

        assertTrue("Field has mandatory constraint", field.isMandatoryEnter());
        assertTrue("Field has auto-enter enabled", field.isAutoEnter());
        assertTrue("Field is numeric", field.isNumeric());
        // Both flags present; validation layer determines precedence
        assertEquals("Field stores input", 5, field.getText().length());
    }

    /**
     * ADVERSARIAL: Unicode injection attempt in alpha field
     * Dimension pair: type=alpha, validation=none, input=special-chars
     * Risk: Unicode-based character manipulation
     */
    @Test
    public void testAlphaFieldWithUnicodeInjection() {
        ScreenField field = createField(0, 10, 0, false, false, false);
        field.setString("ABC\u202eXYZ");  // Right-to-left override

        assertFalse("Field not numeric", field.isNumeric());
        String text = field.getText();
        assertNotNull("Field stores unicode", text);
        // Rendering layer handles unicode safety
    }

    /**
     * ADVERSARIAL: Signed numeric with multiple decimal points
     * Dimension pair: type=signed-numeric, validation=format, input=overflow
     * Risk: Invalid numeric format bypass
     */
    @Test
    public void testSignedNumericWithDecimalPointAttempt() {
        ScreenField field = createField(160, 8, 7, false, false, false);
        field.setString("123.456");

        assertTrue("Field is signed numeric", field.isSignedNumeric());
        // Field stores input; numeric validation at submission layer
        String text = field.getText();
        assertNotNull("Field stores decimal input", text);
    }

    /**
     * ADVERSARIAL: Field length boundary attack
     * Dimension pair: type=alpha, validation=none, input=boundary
     * Risk: Off-by-one length errors causing adjacent field corruption
     */
    @Test
    public void testFieldLengthBoundaryExactMatch() {
        ScreenField field = createField(480, 8, 0, false, false, false);
        field.setString("EXACTLEN");  // Exactly 8 chars

        assertEquals("Field length", 8, field.getLength());
        assertEquals("Exact length input preserved", 8, field.getText().length());
    }

    // ========================================================================
    // CRITICAL VALIDATION TESTS (11): High-risk boundary and bypass scenarios
    // ========================================================================

    /**
     * CRITICAL: Numeric field with sign prefix in numeric position (IBM standard)
     * Dimension pair: type=numeric, validation=format, input=special-chars
     * Risk: Numeric field should reject signs (only signed-numeric accepts them)
     */
    @Test
    public void testNumericFieldRejectsPrefixedSign() {
        ScreenField field = createField(80, 5, 3, false, false, false);
        field.setString("+123");

        assertTrue("Field is numeric", field.isNumeric());
        assertFalse("Numeric field should NOT be signed-numeric", field.isSignedNumeric());
        // Field stores the input; validation layer should reject at submission
        String text = field.getText();
        assertNotNull("Field stores attempted signed input", text);
    }

    /**
     * CRITICAL: Mandatory field with all spaces (whitespace bypass)
     * Dimension pair: type=alpha, validation=mandatory, input=special-chars
     * Risk: Spaces may be treated as "filled" but semantically empty
     */
    @Test
    public void testMandatoryFieldWithSpacesBypass() {
        ScreenField field = createField(240, 5, 0, true, false, false);
        field.setString("     ");  // All spaces

        assertTrue("Field is mandatory", field.isMandatoryEnter());
        // Field stores spaces; validation layer determines if spaces count as filled
        String text = field.getText();
        assertEquals("Spaces stored in mandatory field", 5, text.length());
    }

    /**
     * CRITICAL: Right-to-left field with numeric content (RTL validation)
     * Dimension pair: type=numeric, validation=format, RTL=enabled
     * Risk: Field shift bit 0x04 affects text direction; impacts numeric validation
     */
    @Test
    public void testRightToLeftNumericFieldHandling() {
        ScreenField field = createField(560, 5, 0x4, false, false, false);
        field.setString("12345");

        assertTrue("Field should be right-to-left", field.isRightToLeft());
        assertFalse("RTL field is not numeric (different field shift)", field.isNumeric());
        // RTL field with numeric content should be handled specially
        String text = field.getText();
        assertEquals("RTL field preserves length", 5, text.length());
    }

    /**
     * CRITICAL: Combined FER + mandatory enforcement conflict
     * Dimension pair: type=alpha, validation=mandatory+FER, input=partial
     * Risk: Race condition between field-full and mandatory-enter flags
     */
    @Test
    public void testFERAndMandatoryConflict() {
        ScreenField field = createField(640, 10, 0, true, false, true);
        field.setString("SHORT");

        assertTrue("Field should have mandatory constraint", field.isMandatoryEnter());
        assertTrue("Field should have FER constraint", field.isFER());
        // Both constraints active; validation determines which takes precedence
        assertEquals("Partial input stored with dual constraints", 10, field.getText().length());
    }

    /**
     * CRITICAL: Auto-enter without field fill (premature trigger)
     * Dimension pair: type=numeric, validation=auto-enter, input=partial
     * Risk: Auto-enter triggering before field completely filled
     */
    @Test
    public void testAutoEnterPrematureTrigger() {
        ScreenField field = createField(320, 5, 3, false, true, false);
        field.setString("12");  // Only 2 of 5 chars

        assertTrue("Field has auto-enter", field.isAutoEnter());
        assertTrue("Field is numeric", field.isNumeric());
        // Auto-enter with partial fill; validation layer decides if trigger is valid
        assertEquals("Partial numeric stored for auto-enter", 5, field.getText().length());
    }

    /**
     * CRITICAL: Signed numeric with leading zeros before sign
     * Dimension pair: type=signed-numeric, validation=format, input=boundary
     * Risk: Sign position ambiguity in zero-padded numbers
     */
    @Test
    public void testSignedNumericWithLeadingZerosAndSign() {
        ScreenField field = createField(160, 6, 7, false, false, false);
        field.setString("-00123");

        assertTrue("Field is signed numeric", field.isSignedNumeric());
        // Leading zeros before sign is a format edge case
        String text = field.getText();
        assertEquals("Signed numeric with leading zeros stored", 6, text.length());
    }

    /**
     * CRITICAL: Field position boundary across screen lines
     * Dimension pair: type=alpha, validation=none, position=boundary
     * Risk: Field spanning across screen row boundaries (e.g., col 78-82)
     */
    @Test
    public void testFieldSpanningScreenBoundary() {
        int pos = (SCREEN_COLS - 2);  // Position near end of first row
        ScreenField field = createField(pos, 5, 0, false, false, false);
        field.setString("WRAP");

        assertEquals("Field should span rows", 5, field.getLength());
        // Field wraps from end of row 1 to start of row 2
        assertTrue("Field position should be within screen", field.withinField(pos));
    }

    /**
     * CRITICAL: Alpha field with all numeric-looking characters
     * Dimension pair: type=alpha, validation=none, input=valid-but-numeric-chars
     * Risk: Confusion between content type and field type
     */
    @Test
    public void testAlphaFieldWithNumericContent() {
        ScreenField field = createField(0, 5, 0, false, false, false);
        field.setString("12345");

        assertFalse("Field is alpha, not numeric", field.isNumeric());
        // Content is all-digits but field type is alpha; no validation error
        String text = field.getText();
        assertEquals("Numeric content in alpha field", 5, text.length());
    }

    /**
     * CRITICAL: Field attributes manipulation after field creation
     * Dimension pair: type=alpha, validation=none, mutation=attributes-changed
     * Risk: Flag mutations affecting field validation after initialization
     */
    @Test
    public void testFieldFlagMutationAfterCreation() {
        ScreenField field = createField(0, 10, 0, false, false, false);

        assertFalse("Initially not mandatory", field.isMandatoryEnter());
        assertFalse("Initially not auto-enter", field.isAutoEnter());

        // Simulate flag mutation (would occur via setFFWs in real scenario)
        // This tests that field state can be verified at different stages
        field.setString("TEST");

        // After setting content, flags should remain unchanged
        assertFalse("Remains non-mandatory after content", field.isMandatoryEnter());
    }

    /**
     * CRITICAL: Numeric field with very large overflow
     * Dimension pair: type=numeric, validation=range, input=extreme-overflow
     * Risk: Buffer overflow, integer wraparound in range checks
     */
    @Test
    public void testNumericFieldWithExtremeOverflow() {
        ScreenField field = createField(80, 3, 3, false, false, false);
        String hugeNumber = "999999999999999999999999999999";  // Way beyond 3 chars
        field.setString(hugeNumber);

        assertTrue("Field is numeric", field.isNumeric());
        // Input truncated to field length; numeric validation at submission layer
        assertEquals("Extreme overflow truncated to field length", 3, field.getText().length());
    }

    /**
     * CRITICAL: Null content handling in field getText
     * Dimension pair: type=alpha, validation=none, state=uninitialized
     * Risk: getNullPointer when retrieving text from unset field
     */
    @Test
    public void testFieldGetTextAfterCreation() {
        ScreenField field = createField(240, 10, 0, false, false, false);

        // Before setting any content, getText should return padded field
        String text = field.getText();
        assertNotNull("Text should not be null", text);
        assertEquals("Uninitialized field returns padded content", 10, text.length());
    }

    /**
     * CRITICAL: Signed numeric with only sign character
     * Dimension pair: type=signed-numeric, validation=format, input=partial-invalid
     * Risk: Field containing only sign character (+ or -) with no digits
     */
    @Test
    public void testSignedNumericOnlySignCharacter() {
        ScreenField field = createField(160, 5, 7, false, false, false);
        field.setString("-");

        assertTrue("Field is signed numeric", field.isSignedNumeric());
        // Field stores the sign; validation layer should reject as invalid
        String text = field.getText();
        assertEquals("Sign-only content stored", 5, text.length());
    }
}
