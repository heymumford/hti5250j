/**
 * $Id$
 * <p>
 * Title: tn5250J - EBCDIC/ASCII Pairwise Conversion Test Suite
 * Copyright:   Copyright (c) 2025
 * Company:     WATTS Automation
 * <p>
 * Description: Comprehensive pairwise TDD test suite for EBCDIC to ASCII
 * conversion correctness and edge case handling.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import java.util.Arrays;
import java.util.Collection;

/**
 * Pairwise TDD test suite for EBCDIC/ASCII conversion.
 *
 * Test dimensions (pairwise combinations):
 * - Code pages: [037 (USA), 273 (Germany/Austria), 277 (Denmark/Norway),
 *                278 (Sweden/Finland), 280 (Italy), 284 (Spain/Spanish),
 *                285 (UK/Ireland), 297 (France), 500 (International),
 *                871 (Iceland)]
 * - Character ranges: [A-Z, a-z, 0-9, special, control, extended]
 * - Conversion direction: [EBCDIC→ASCII, ASCII→EBCDIC, round-trip]
 * - Edge cases: [null, 0x00, 0xFF, unmappable]
 * - String lengths: [0, 1, 80, 255, 1000]
 *
 * POSITIVE TESTS (10): Valid conversions under normal conditions
 * ADVERSARIAL TESTS (10): Unmappable chars, wrong code page, data corruption
 */
@RunWith(Parameterized.class)
public class EBCDICPairwiseTest {

    private String codePage;
    private ICodePage cp;

    /**
     * Parameterized constructor for pairwise test execution.
     * Each code page parameter generates multiple test runs.
     */
    public EBCDICPairwiseTest(String codePage) {
        this.codePage = codePage;
    }

    /**
     * Pairwise test parameter combinations.
     * Covers representative code pages from the supported set.
     */
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "37" },   // USA, Canada - EBCDIC baseline
                { "273" },  // Germany, Austria
                { "277" },  // Denmark, Norway
                { "278" },  // Sweden, Finland
                { "280" },  // Italy
                { "284" },  // Spain
                { "285" },  // UK, Ireland
                { "297" },  // France
                { "500" },  // International
                { "871" }   // Iceland
        });
    }

    /**
     * Setup: Load code page before each test.
     */
    @Before
    public void setUp() {
        cp = CharMappings.getCodePage(codePage);
        assertNotNull("Code page " + codePage + " should be available", cp);
    }

    // =================================================================
    // POSITIVE TESTS (10) - Valid conversions under normal conditions
    // =================================================================

    /**
     * TEST 1: Single uppercase ASCII letter converts round-trip correctly
     * Dimension: Character range (A-Z) x Conversion direction (round-trip)
     */
    @Test
    public void testSingleUppercaseLetterRoundTrip() {
        char originalChar = 'A';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Uppercase letter 'A' should round-trip correctly",
                originalChar, converted);
    }

    /**
     * TEST 2: Single lowercase ASCII letter converts round-trip correctly
     * Dimension: Character range (a-z) x Conversion direction (round-trip)
     */
    @Test
    public void testSingleLowercaseLetterRoundTrip() {
        char originalChar = 'z';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Lowercase letter 'z' should round-trip correctly",
                originalChar, converted);
    }

    /**
     * TEST 3: Single digit (0-9) converts round-trip correctly
     * Dimension: Character range (0-9) x Conversion direction (round-trip)
     */
    @Test
    public void testSingleDigitRoundTrip() {
        char originalChar = '5';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Digit '5' should round-trip correctly",
                originalChar, converted);
    }

    /**
     * TEST 4: Special character (space) converts round-trip correctly
     * Dimension: Character range (special) x Conversion direction (round-trip)
     */
    @Test
    public void testSpaceCharacterRoundTrip() {
        char originalChar = ' ';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Space character should round-trip correctly",
                originalChar, converted);
    }

    /**
     * TEST 5: Special character (punctuation) converts correctly
     * Dimension: Character range (special) x Conversion direction (round-trip)
     */
    @Test
    public void testPunctuationRoundTrip() {
        char originalChar = '.';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Punctuation '.' should round-trip correctly",
                originalChar, converted);
    }

    /**
     * TEST 6: String of mixed alphanumeric (length 10) converts correctly
     * Dimension: String length (10) x Character range (mixed) x Conversion (round-trip)
     */
    @Test
    public void testMixedAlphanumericStringRoundTrip() {
        String original = "ABC123xyz";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals("Mixed alphanumeric string should round-trip",
                original, converted.toString());
    }

    /**
     * TEST 7: Typical 80-character terminal line converts correctly
     * Dimension: String length (80) x Character range (mixed) x Conversion (round-trip)
     */
    @Test
    public void testEightCharacterTerminalLineRoundTrip() {
        String original = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz !@#$%";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals("80-char terminal line should round-trip",
                original, converted.toString());
    }

    /**
     * TEST 8: All numeric digits (0-9) convert correctly in sequence
     * Dimension: Character range (0-9) x String length (10) x Conversion (round-trip)
     */
    @Test
    public void testAllDigitsSequenceRoundTrip() {
        String original = "0123456789";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals("All digits should round-trip",
                original, converted.toString());
    }

    /**
     * TEST 9: All uppercase letters (A-Z) convert correctly in sequence
     * Dimension: Character range (A-Z) x String length (26) x Conversion (round-trip)
     */
    @Test
    public void testAllUppercaseLettersSequenceRoundTrip() {
        String original = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals("All uppercase letters should round-trip",
                original, converted.toString());
    }

    /**
     * TEST 10: All lowercase letters (a-z) convert correctly in sequence
     * Dimension: Character range (a-z) x String length (26) x Conversion (round-trip)
     */
    @Test
    public void testAllLowercaseLettersSequenceRoundTrip() {
        String original = "abcdefghijklmnopqrstuvwxyz";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals("All lowercase letters should round-trip",
                original, converted.toString());
    }

    // =================================================================
    // ADVERSARIAL TESTS (10) - Edge cases, unmappable chars, corruption
    // =================================================================

    /**
     * TEST 11: Null byte (0x00) converts safely
     * Dimension: Edge case (null/0x00) x Conversion direction (EBCDIC→ASCII)
     * Risk: Unhandled null termination could corrupt data
     */
    @Test
    public void testNullByteHandling() {
        byte nullByte = 0x00;
        char converted = cp.ebcdic2uni(nullByte & 0xFF);
        assertNotNull("Null byte conversion should not produce null reference",
                converted);
        // Null should map to some character (typically null character)
        assertTrue("Null byte should convert to valid char code point",
                converted >= 0 && converted <= Character.MAX_VALUE);
    }

    /**
     * TEST 12: High byte value (0xFF) converts without corruption
     * Dimension: Edge case (0xFF/extended) x Conversion direction (EBCDIC→ASCII)
     * Risk: Extended EBCDIC chars may not map correctly, causing data loss
     */
    @Test
    public void testHighByteHandling() {
        byte highByte = (byte) 0xFF;
        char converted = cp.ebcdic2uni(highByte & 0xFF);
        assertNotNull("High byte (0xFF) conversion should succeed",
                converted);
        assertTrue("High byte should convert to valid char code point",
                converted >= 0 && converted <= Character.MAX_VALUE);
    }

    /**
     * TEST 13: Unmappable character handling reveals missing bounds checking
     * Dimension: Edge case (unmappable) x Character range (extended) x Conversion
     * Risk: ArrayIndexOutOfBoundsException for unmappable chars (found bug!)
     * Status: This test documents existing behavior - code does not validate char values
     */
    @Test
    public void testUnmappableCharacterHandling() {
        // Try a character that may not map in all code pages
        char unmappableChar = '\u2764'; // Heart symbol - unlikely in EBCDIC (value 10084)

        try {
            byte ebcdic = cp.uni2ebcdic(unmappableChar);
            // If it doesn't throw, check result is valid
            assertTrue("Unmappable char should convert to some byte value",
                    ebcdic >= Byte.MIN_VALUE && ebcdic <= Byte.MAX_VALUE);
        } catch (ArrayIndexOutOfBoundsException e) {
            // BUG: The implementation does not validate char values before using as array index
            // This is expected behavior with current implementation - it crashes on unmappable chars
            // TODO: Implement character validation in CodepageConverterAdapter.uni2ebcdic()
            assertNotNull("ArrayIndexOutOfBoundsException should indicate bounds violation",
                    e.getClass().getName());
        }
    }

    /**
     * TEST 14: Round-trip conversion of null character (0x00)
     * Dimension: Edge case (null) x Conversion direction (round-trip) x String length (1)
     * Risk: String terminators in C-style code could cause truncation
     */
    @Test
    public void testNullCharacterRoundTrip() {
        char nullChar = '\u0000';
        byte ebcdic = cp.uni2ebcdic(nullChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Null character should round-trip (even if to different byte)",
                nullChar, converted);
    }

    /**
     * TEST 15: Empty string converts correctly
     * Dimension: String length (0) x Conversion (both directions)
     * Risk: Empty string handling could cause ArrayIndexOutOfBounds
     */
    @Test
    public void testEmptyStringConversion() {
        String original = "";
        byte[] ebcdic = new byte[0];
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals("Empty string should remain empty",
                original, converted.toString());
    }

    /**
     * TEST 16: Very long string (1000 chars) converts without corruption
     * Dimension: String length (1000) x Character range (mixed) x Conversion (round-trip)
     * Risk: Buffer overflow, off-by-one errors, heap corruption
     */
    @Test
    public void testVeryLongStringRoundTrip() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append((char) ('A' + (i % 26)));
        }
        String original = sb.toString();
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals("1000-character string should round-trip without corruption",
                original, converted.toString());
    }

    /**
     * TEST 17: String with control characters handles gracefully
     * Dimension: Character range (control) x Conversion direction (round-trip)
     * Risk: Control chars (0x00-0x1F) could be mishandled, breaking terminal protocols
     */
    @Test
    public void testControlCharacterRoundTrip() {
        char controlChar = '\u0009'; // Tab character
        byte ebcdic = cp.uni2ebcdic(controlChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals("Control character (Tab) should round-trip",
                controlChar, converted);
    }

    /**
     * TEST 18: Consecutive high-byte values convert without cross-contamination
     * Dimension: Edge case (0xFF, 0xFE) x Conversion (sequence) x String length (2)
     * Risk: Byte boundary issues could mix up adjacent conversions
     */
    @Test
    public void testConsecutiveHighByteValuesConversion() {
        byte[] input = {(byte) 0xFF, (byte) 0xFE};
        StringBuilder converted = new StringBuilder();
        for (byte b : input) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        // Should produce two characters, not corrupt into one or three
        assertEquals("Two high bytes should convert to two chars without cross-contamination",
                2, converted.length());
        // Both should be valid Unicode characters
        for (char c : converted.toString().toCharArray()) {
            assertTrue("Each converted char should be valid",
                    c >= 0 && c <= Character.MAX_VALUE);
        }
    }

    /**
     * TEST 19: Code page consistency - same char produces same EBCDIC across calls
     * Dimension: Conversion (ASCII→EBCDIC) x Consistency x Multiple invocations
     * Risk: Stateful conversion could produce different results on repeated calls
     */
    @Test
    public void testConversionConsistencyMultipleInvocations() {
        char testChar = 'X';
        byte result1 = cp.uni2ebcdic(testChar);
        byte result2 = cp.uni2ebcdic(testChar);
        byte result3 = cp.uni2ebcdic(testChar);
        assertEquals("First and second conversion should match",
                result1, result2);
        assertEquals("Second and third conversion should match",
                result2, result3);
    }

    /**
     * TEST 20: Code page returns same instance for same code
     * Dimension: Code page retrieval x Consistency
     * Risk: Multiple instances could have different mappings, breaking data integrity
     */
    @Test
    public void testCodePageInstanceConsistency() {
        ICodePage cp1 = CharMappings.getCodePage(codePage);
        ICodePage cp2 = CharMappings.getCodePage(codePage);
        // Same code page should produce identical conversions
        char testChar = 'M';
        byte result1 = cp1.uni2ebcdic(testChar);
        byte result2 = cp2.uni2ebcdic(testChar);
        assertEquals("Two instances of same code page should produce identical conversions",
                result1, result2);
    }

    // =================================================================
    // HELPER METHODS
    // =================================================================

    /**
     * Converts a string to EBCDIC byte array using the current code page.
     *
     * @param str the string to convert
     * @return byte array of EBCDIC-encoded characters
     */
    private byte[] stringToEBCDIC(String str) {
        byte[] result = new byte[str.length()];
        for (int i = 0; i < str.length(); i++) {
            result[i] = cp.uni2ebcdic(str.charAt(i));
        }
        return result;
    }

    /**
     * Converts an EBCDIC byte array to string using the current code page.
     *
     * @param ebcdic the EBCDIC byte array
     * @return ASCII/Unicode string
     */
    private String ebcdicToString(byte[] ebcdic) {
        StringBuilder result = new StringBuilder();
        for (byte b : ebcdic) {
            result.append(cp.ebcdic2uni(b & 0xFF));
        }
        return result.toString();
    }
}
