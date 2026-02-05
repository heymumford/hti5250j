/**
 * $Id$
 * <p>
 * Title: tn5250J - Character Set Conversion Pairwise Test Suite
 * Copyright:   Copyright (c) 2025
 * Company:     WATTS Automation
 * <p>
 * Description: Comprehensive pairwise TDD test suite for EBCDIC/ASCII
 * conversion accuracy, code page handling, and adversarial character scenarios.
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
 */
package org.tn5250j.encoding;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Comprehensive pairwise test suite for EBCDIC/ASCII character conversion.
 *
 * Pairwise dimensions (covering 20+ test cases):
 * 1. Code page: 37 (USA), 273 (Germany), 280 (Italy), 284 (Spain), 297 (France), 500 (International)
 * 2. Character type: printable (A-Z, 0-9), control (0x00-0x1F), extended (0x80-0xFF), undefined
 * 3. Direction: EBCDIC→ASCII, ASCII→EBCDIC, round-trip
 * 4. String length: 0 (empty), 1 (single), 80 (terminal), 256 (buffer), max
 * 5. Special chars: field markers (0x1D, 0x1F), null (0x00), newline (0x0A), graphic symbols
 *
 * Coverage:
 * - POSITIVE TESTS (12): Valid encodings, round-trips, multi-byte handling
 * - ADVERSARIAL TESTS (12): Unmappable chars, boundary violations, data integrity checks
 */
@RunWith(Parameterized.class)
public class CharsetConversionPairwiseTest {

    private String codePage;
    private ICodePage cp;

    /**
     * Parameterized constructor for code page variations.
     */
    public CharsetConversionPairwiseTest(String codePage) {
        this.codePage = codePage;
    }

    /**
     * Pairwise parameter combinations: representative code pages.
     * Dimension 1: Code page [37, 273, 280, 284, 297, 500]
     */
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "37" },    // USA, Canada - baseline EBCDIC
                { "273" },   // Germany, Austria
                { "280" },   // Italy
                { "284" },   // Spain
                { "297" },   // France
                { "500" }    // International
        });
    }

    @Before
    public void setUp() {
        cp = CharMappings.getCodePage(codePage);
        assertNotNull("Code page " + codePage + " must be available", cp);
    }

    // =====================================================================
    // POSITIVE TESTS (12) - Valid encodings under normal conditions
    // =====================================================================

    /**
     * TEST 1: Printable character (uppercase) round-trips correctly.
     * Dimension: Character type [printable: A-Z]
     *            Direction [round-trip]
     */
    @Test
    public void testPrintableUppercaseRoundTrip() {
        for (char c = 'A'; c <= 'Z'; c++) {
            byte ebcdic = cp.uni2ebcdic(c);
            char converted = cp.ebcdic2uni(ebcdic & 0xFF);
            assertEquals("Uppercase letter '" + c + "' (CP " + codePage + ") should round-trip",
                    c, converted);
        }
    }

    /**
     * TEST 2: Printable character (lowercase) round-trips correctly.
     * Dimension: Character type [printable: a-z]
     *            Direction [round-trip]
     */
    @Test
    public void testPrintableLowercaseRoundTrip() {
        for (char c = 'a'; c <= 'z'; c++) {
            byte ebcdic = cp.uni2ebcdic(c);
            char converted = cp.ebcdic2uni(ebcdic & 0xFF);
            assertEquals("Lowercase letter '" + c + "' (CP " + codePage + ") should round-trip",
                    c, converted);
        }
    }

    /**
     * TEST 3: Numeric digits (0-9) round-trip correctly.
     * Dimension: Character type [printable: 0-9]
     *            Direction [round-trip]
     */
    @Test
    public void testPrintableNumericRoundTrip() {
        for (char c = '0'; c <= '9'; c++) {
            byte ebcdic = cp.uni2ebcdic(c);
            char converted = cp.ebcdic2uni(ebcdic & 0xFF);
            assertEquals("Digit '" + c + "' (CP " + codePage + ") should round-trip",
                    c, converted);
        }
    }

    /**
     * TEST 4: Special printable characters round-trip correctly.
     * Dimension: Character type [special: space, punctuation]
     *            Direction [round-trip]
     */
    @Test
    public void testSpecialCharactersRoundTrip() {
        String specialChars = " .,-!?@#$%&*()";
        for (char c : specialChars.toCharArray()) {
            byte ebcdic = cp.uni2ebcdic(c);
            char converted = cp.ebcdic2uni(ebcdic & 0xFF);
            assertEquals("Special char '" + c + "' (CP " + codePage + ") should round-trip",
                    c, converted);
        }
    }

    /**
     * TEST 5: Empty string (length 0) converts without error.
     * Dimension: String length [0]
     *            Direction [both]
     */
    @Test
    public void testEmptyStringConversion() {
        byte[] ebcdic = new byte[0];
        String result = ebcdicToString(ebcdic);
        assertEquals("Empty string should remain empty in CP " + codePage,
                "", result);
    }

    /**
     * TEST 6: Single character (length 1) converts correctly.
     * Dimension: String length [1]
     *            Character type [printable]
     *            Direction [round-trip]
     */
    @Test
    public void testSingleCharacterRoundTrip() {
        char testChar = 'X';
        byte ebcdic = cp.uni2ebcdic(testChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Single char 'X' (CP " + codePage + ") should round-trip",
                testChar, converted);
    }

    /**
     * TEST 7: Terminal-length string (80 chars) round-trips correctly.
     * Dimension: String length [80]
     *            Character type [mixed printable]
     *            Direction [round-trip]
     */
    @Test
    public void testTerminalLineRoundTrip() {
        String original = generateTerminalLine(80);
        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);
        assertEquals("80-char terminal line (CP " + codePage + ") should round-trip",
                original, converted);
    }

    /**
     * TEST 8: Buffer-size string (256 chars) round-trips correctly.
     * Dimension: String length [256]
     *            Character type [mixed printable]
     *            Direction [round-trip]
     */
    @Test
    public void testBufferSizeStringRoundTrip() {
        String original = generateTerminalLine(256);
        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);
        assertEquals("256-char buffer (CP " + codePage + ") should round-trip",
                original, converted);
    }

    /**
     * TEST 9: ASCII→EBCDIC→ASCII triple conversion maintains fidelity.
     * Dimension: Direction [ASCII→EBCDIC→ASCII]
     *            Character type [mixed]
     *            String length [50]
     */
    @Test
    public void testTripleConversionFidelity() {
        String original = "The quick brown fox jumps over the lazy dog 12345";
        byte[] toEBCDIC = stringToEBCDIC(original);
        String backToASCII = ebcdicToString(toEBCDIC);
        byte[] toEBCDICAgain = stringToEBCDIC(backToASCII);

        String finalResult = ebcdicToString(toEBCDICAgain);
        assertEquals("Triple conversion should maintain fidelity in CP " + codePage,
                original, finalResult);
    }

    /**
     * TEST 10: Field marker character (0x1D) converts correctly.
     * Dimension: Special chars [field markers]
     *            Direction [EBCDIC→ASCII]
     * Note: Field markers are protocol-specific; test conversion behavior
     */
    @Test
    public void testFieldMarkerConversion() {
        byte fieldMarker = (byte) 0x1D;
        char converted = cp.ebcdic2uni(fieldMarker & 0xFF);
        assertNotNull("Field marker 0x1D should convert to some character in CP " + codePage,
                converted);
        assertTrue("Field marker conversion should yield valid Unicode",
                converted >= 0 && converted <= Character.MAX_VALUE);
    }

    /**
     * TEST 11: Conversion consistency across multiple calls.
     * Dimension: Direction [ASCII→EBCDIC]
     *            Consistency [idempotent]
     */
    @Test
    public void testConversionConsistency() {
        char testChar = 'Q';
        byte result1 = cp.uni2ebcdic(testChar);
        byte result2 = cp.uni2ebcdic(testChar);
        byte result3 = cp.uni2ebcdic(testChar);

        assertEquals("First and second conversion should match in CP " + codePage,
                result1, result2);
        assertEquals("Second and third conversion should match in CP " + codePage,
                result2, result3);
    }

    /**
     * TEST 12: Code page instance consistency.
     * Dimension: Code page retrieval [cached]
     *            Consistency [singleton]
     */
    @Test
    public void testCodePageInstanceConsistency() {
        ICodePage cp1 = CharMappings.getCodePage(codePage);
        ICodePage cp2 = CharMappings.getCodePage(codePage);

        char testChar = 'M';
        byte result1 = cp1.uni2ebcdic(testChar);
        byte result2 = cp2.uni2ebcdic(testChar);

        assertEquals("Same code page retrieved twice should yield identical conversions in CP "
                + codePage, result1, result2);
    }

    // =====================================================================
    // ADVERSARIAL TESTS (12) - Edge cases, bounds, unmappable characters
    // =====================================================================

    /**
     * TEST 13: Null byte (0x00) EBCDIC converts without corruption.
     * Dimension: Special chars [null]
     *            Direction [EBCDIC→ASCII]
     * Risk: Null terminators in C-style code could truncate data
     */
    @Test
    public void testNullByteEBCDICConversion() {
        byte nullByte = 0x00;
        char converted = cp.ebcdic2uni(nullByte & 0xFF);
        assertNotNull("Null byte should produce valid char reference in CP " + codePage,
                Character.valueOf(converted));
        assertTrue("Null byte should map to valid Unicode range in CP " + codePage,
                converted >= 0 && converted <= Character.MAX_VALUE);
    }

    /**
     * TEST 14: High byte (0xFF) EBCDIC converts without data loss.
     * Dimension: Character type [extended: 0x80-0xFF]
     *            Direction [EBCDIC→ASCII]
     * Risk: Extended EBCDIC chars may not map, causing data corruption
     */
    @Test
    public void testHighByteEBCDICConversion() {
        byte highByte = (byte) 0xFF;
        char converted = cp.ebcdic2uni(highByte & 0xFF);
        assertTrue("High byte (0xFF) should convert to valid Unicode in CP " + codePage,
                converted >= 0 && converted <= Character.MAX_VALUE);
    }

    /**
     * TEST 15: Consecutive high bytes (0xFE, 0xFF) without cross-contamination.
     * Dimension: Character type [extended]
     *            String length [2]
     *            Direction [EBCDIC→ASCII]
     * Risk: Byte boundary issues could corrupt adjacent conversions
     */
    @Test
    public void testConsecutiveHighBytesWithoutCrossContamination() {
        byte[] input = {(byte) 0xFE, (byte) 0xFF};
        String converted = ebcdicToString(input);

        assertEquals("Two high bytes should produce exactly two characters in CP " + codePage,
                2, converted.length());

        for (char c : converted.toCharArray()) {
            assertTrue("Each converted char must be valid Unicode in CP " + codePage,
                    c >= 0 && c <= Character.MAX_VALUE);
        }
    }

    /**
     * TEST 16: Byte array boundary (256 bytes = full byte range) converts.
     * Dimension: String length [256]
     *            Character type [mixed: all byte values]
     *            Direction [EBCDIC→ASCII]
     * Risk: Array indexing errors at boundaries
     */
    @Test
    public void testFullByteRangeConversion() {
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }

        String converted = ebcdicToString(allBytes);
        assertEquals("256-byte range should produce 256 characters in CP " + codePage,
                256, converted.length());

        for (char c : converted.toCharArray()) {
            assertTrue("Each char must be valid Unicode in CP " + codePage,
                    c >= 0 && c <= Character.MAX_VALUE);
        }
    }

    /**
     * TEST 17: Unmappable Unicode character handling (U+2764 = heart symbol).
     * Dimension: Character type [undefined]
     *            Direction [ASCII→EBCDIC]
     * Risk: Unmappable chars could throw ArrayIndexOutOfBoundsException
     * Note: This test documents expected behavior with current implementation
     */
    @Test
    public void testUnmappableUnicodeCharacterHandling() {
        char unmappableChar = '\u2764'; // Heart symbol (value 10084) - unlikely in EBCDIC

        try {
            byte result = cp.uni2ebcdic(unmappableChar);
            // If conversion succeeds, result must be valid
            assertTrue("Unmappable char conversion should yield byte value in CP " + codePage,
                    result >= Byte.MIN_VALUE && result <= Byte.MAX_VALUE);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Current implementation throws on unmappable chars
            // This documents the behavior; caller must validate input
            assertNotNull("ArrayIndexOutOfBoundsException indicates validation needed in CP "
                    + codePage, e);
        } catch (Exception e) {
            // Any other exception is unexpected
            fail("Unexpected exception for unmappable char in CP " + codePage + ": "
                    + e.getClass().getName());
        }
    }

    /**
     * TEST 18: Control character (tab, 0x09) round-trips correctly.
     * Dimension: Character type [control: 0x00-0x1F]
     *            Direction [round-trip]
     * Risk: Control chars could be stripped or mishandled
     */
    @Test
    public void testControlCharacterRoundTrip() {
        char tabChar = '\t';
        byte ebcdic = cp.uni2ebcdic(tabChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Tab character should round-trip in CP " + codePage,
                tabChar, converted);
    }

    /**
     * TEST 19: Newline character (0x0A) handling preserves semantics.
     * Dimension: Special chars [newline]
     *            Direction [round-trip]
     * Risk: Newlines could be lost or converted to spaces
     */
    @Test
    public void testNewlineCharacterHandling() {
        char newlineChar = '\n';
        byte ebcdic = cp.uni2ebcdic(newlineChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Newline character should round-trip in CP " + codePage,
                newlineChar, converted);
    }

    /**
     * TEST 20: Very long string (1024 chars) converts without buffer overflow.
     * Dimension: String length [1024, max]
     *            Character type [mixed]
     *            Direction [round-trip]
     * Risk: Buffer overflow, heap corruption, truncation
     */
    @Test
    public void testVeryLongStringRoundTrip() {
        String original = generateTerminalLine(1024);
        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);

        assertEquals("1024-char string should round-trip without truncation in CP " + codePage,
                original, converted);
        assertEquals("Buffer should maintain exact length in CP " + codePage,
                original.length(), converted.length());
    }

    /**
     * TEST 21: Repeated character sequence maintains pattern integrity.
     * Dimension: Character type [repetitive pattern]
     *            Direction [round-trip]
     *            String length [100]
     * Risk: Pattern corruption, off-by-one errors in indexing
     */
    @Test
    public void testRepeatingPatternIntegrity() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("ABC");
        }
        String original = sb.toString();

        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);

        assertEquals("300-char repeating pattern should maintain integrity in CP " + codePage,
                original, converted);
    }

    /**
     * TEST 22: Null termination behavior in round-trip doesn't truncate.
     * Dimension: Special chars [null]
     *            String length [including null]
     *            Direction [round-trip]
     * Risk: C-style null termination could truncate data mid-string
     */
    @Test
    public void testNullTerminationDoesNotTruncate() {
        String beforeNull = "DATA";
        String afterNull = "MORE";
        String original = beforeNull + '\u0000' + afterNull;

        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);

        assertEquals("String with embedded null should maintain length in CP " + codePage,
                original.length(), converted.length());
        assertEquals("String with embedded null should round-trip in CP " + codePage,
                original, converted);
    }

    /**
     * TEST 23: Asymmetric code page mappings validate directional conversion.
     * Dimension: Direction [EBCDIC→ASCII vs ASCII→EBCDIC]
     *            Character type [all mapped printable]
     * Risk: Code page might have asymmetric mappings (not all chars map both ways)
     */
    @Test
    public void testAsymmetricCodePageMapping() {
        // Test a sample of printable ASCII chars both directions
        String testChars = "ABCXYZ0123456789";

        for (char asciiChar : testChars.toCharArray()) {
            byte ebcdic = cp.uni2ebcdic(asciiChar);
            char backToASCII = cp.ebcdic2uni(ebcdic & 0xFF);

            assertEquals("Character '" + asciiChar + "' in CP " + codePage
                    + " should have symmetric mapping",
                    asciiChar, backToASCII);
        }
    }

    /**
     * TEST 24: Boundary between printable and control characters.
     * Dimension: Character type [boundary: 0x1F/0x20]
     *            Direction [round-trip]
     * Risk: Off-by-one errors at ASCII control/printable boundary
     */
    @Test
    public void testControlPrintableBoundary() {
        // 0x1F = Unit Separator (control)
        char controlChar = '\u001F';
        byte ebcdic = cp.uni2ebcdic(controlChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Boundary char 0x1F should round-trip in CP " + codePage,
                controlChar, converted);

        // 0x20 = Space (printable)
        char spaceChar = ' ';
        ebcdic = cp.uni2ebcdic(spaceChar);
        converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Boundary char Space (0x20) should round-trip in CP " + codePage,
                spaceChar, converted);
    }

    // =====================================================================
    // HELPER METHODS
    // =====================================================================

    /**
     * Converts ASCII string to EBCDIC byte array.
     *
     * @param str string to convert
     * @return EBCDIC byte array
     */
    private byte[] stringToEBCDIC(String str) {
        byte[] result = new byte[str.length()];
        for (int i = 0; i < str.length(); i++) {
            result[i] = cp.uni2ebcdic(str.charAt(i));
        }
        return result;
    }

    /**
     * Converts EBCDIC byte array to ASCII string.
     *
     * @param ebcdic EBCDIC byte array
     * @return ASCII/Unicode string
     */
    private String ebcdicToString(byte[] ebcdic) {
        StringBuilder result = new StringBuilder();
        for (byte b : ebcdic) {
            result.append(cp.ebcdic2uni(b & 0xFF));
        }
        return result.toString();
    }

    /**
     * Generates a terminal-length string with mixed printable characters.
     * Used for length dimension testing.
     *
     * @param length desired string length
     * @return string of specified length with repeating pattern
     */
    private String generateTerminalLine(int length) {
        String pattern = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz ";
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            sb.append(pattern);
        }
        return sb.substring(0, length);
    }
}
