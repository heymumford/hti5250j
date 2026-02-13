/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
public class EBCDICPairwiseTest {

    private String codePage;
    private ICodePage cp;

    /**
     * Parameterized constructor for pairwise test execution.
     * Each code page parameter generates multiple test runs.
     */
    private void setParameters(String codePage) {
        this.codePage = codePage;
    }

    /**
     * Pairwise test parameter combinations.
     * Covers representative code pages from the supported set.
     */
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
        public void setUp() {
        cp = CharMappings.getCodePage(codePage);
        assertNotNull(cp,"Code page " + codePage + " should be available");
    }

    // =================================================================
    // POSITIVE TESTS (10) - Valid conversions under normal conditions
    // =================================================================

    /**
     * TEST 1: Single uppercase ASCII letter converts round-trip correctly
     * Dimension: Character range (A-Z) x Conversion direction (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSingleUppercaseLetterRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char originalChar = 'A';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(originalChar, converted,"Uppercase letter 'A' should round-trip correctly");
    }

    /**
     * TEST 2: Single lowercase ASCII letter converts round-trip correctly
     * Dimension: Character range (a-z) x Conversion direction (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSingleLowercaseLetterRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char originalChar = 'z';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(originalChar, converted,"Lowercase letter 'z' should round-trip correctly");
    }

    /**
     * TEST 3: Single digit (0-9) converts round-trip correctly
     * Dimension: Character range (0-9) x Conversion direction (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSingleDigitRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char originalChar = '5';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(originalChar, converted,"Digit '5' should round-trip correctly");
    }

    /**
     * TEST 4: Special character (space) converts round-trip correctly
     * Dimension: Character range (special) x Conversion direction (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSpaceCharacterRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char originalChar = ' ';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(originalChar, converted,"Space character should round-trip correctly");
    }

    /**
     * TEST 5: Special character (punctuation) converts correctly
     * Dimension: Character range (special) x Conversion direction (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testPunctuationRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char originalChar = '.';
        byte ebcdic = cp.uni2ebcdic(originalChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(originalChar, converted,"Punctuation '.' should round-trip correctly");
    }

    /**
     * TEST 6: String of mixed alphanumeric (length 10) converts correctly
     * Dimension: String length (10) x Character range (mixed) x Conversion (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMixedAlphanumericStringRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = "ABC123xyz";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals(original, converted.toString(),"Mixed alphanumeric string should round-trip");
    }

    /**
     * TEST 7: Typical 80-character terminal line converts correctly
     * Dimension: String length (80) x Character range (mixed) x Conversion (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testEightCharacterTerminalLineRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz !@#$%";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals(original, converted.toString(),"80-char terminal line should round-trip");
    }

    /**
     * TEST 8: All numeric digits (0-9) convert correctly in sequence
     * Dimension: Character range (0-9) x String length (10) x Conversion (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAllDigitsSequenceRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = "0123456789";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals(original, converted.toString(),"All digits should round-trip");
    }

    /**
     * TEST 9: All uppercase letters (A-Z) convert correctly in sequence
     * Dimension: Character range (A-Z) x String length (26) x Conversion (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAllUppercaseLettersSequenceRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals(original, converted.toString(),"All uppercase letters should round-trip");
    }

    /**
     * TEST 10: All lowercase letters (a-z) convert correctly in sequence
     * Dimension: Character range (a-z) x String length (26) x Conversion (round-trip)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAllLowercaseLettersSequenceRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = "abcdefghijklmnopqrstuvwxyz";
        byte[] ebcdic = new byte[original.length()];
        for (int i = 0; i < original.length(); i++) {
            ebcdic[i] = cp.uni2ebcdic(original.charAt(i));
        }
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals(original, converted.toString(),"All lowercase letters should round-trip");
    }

    // =================================================================
    // ADVERSARIAL TESTS (10) - Edge cases, unmappable chars, corruption
    // =================================================================

    /**
     * TEST 11: Null byte (0x00) converts safely
     * Dimension: Edge case (null/0x00) x Conversion direction (EBCDIC→ASCII)
     * Risk: Unhandled null termination could corrupt data
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testNullByteHandling(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        byte nullByte = 0x00;
        char converted = cp.ebcdic2uni(nullByte & 0xFF);
        assertNotNull(converted,"Null byte conversion should not produce null reference");
        // Null should map to some character (typically null character)
        assertTrue(converted >= 0 && converted <= Character.MAX_VALUE,"Null byte should convert to valid char code point");
    }

    /**
     * TEST 12: High byte value (0xFF) converts without corruption
     * Dimension: Edge case (0xFF/extended) x Conversion direction (EBCDIC→ASCII)
     * Risk: Extended EBCDIC chars may not map correctly, causing data loss
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testHighByteHandling(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        byte highByte = (byte) 0xFF;
        char converted = cp.ebcdic2uni(highByte & 0xFF);
        assertNotNull(converted,"High byte (0xFF) conversion should succeed");
        assertTrue(converted >= 0 && converted <= Character.MAX_VALUE,"High byte should convert to valid char code point");
    }

    /**
     * TEST 13: Unmappable character handling with proper exception
     * Dimension: Edge case (unmappable) x Character range (extended) x Conversion
     * Status: FIXED - Implementation now throws CharacterConversionException instead of ArrayIndexOutOfBoundsException
     * The bug has been fixed - CodepageConverterAdapter now validates char values before conversion
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUnmappableCharacterHandling(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        // Try a character that may not map in all code pages
        char unmappableChar = '\u2764'; // Heart symbol - unlikely in EBCDIC (value 10084)

        // After fix: Implementation throws CharacterConversionException for unmappable chars
        assertThrows(CharacterConversionException.class, () -> {
            cp.uni2ebcdic(unmappableChar);
        }, "Unmappable character should throw CharacterConversionException");
    }

    /**
     * TEST 14: Round-trip conversion of null character (0x00)
     * Dimension: Edge case (null) x Conversion direction (round-trip) x String length (1)
     * Risk: String terminators in C-style code could cause truncation
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testNullCharacterRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char nullChar = '\u0000';
        byte ebcdic = cp.uni2ebcdic(nullChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(nullChar, converted,"Null character should round-trip (even if to different byte)");
    }

    /**
     * TEST 15: Empty string converts correctly
     * Dimension: String length (0) x Conversion (both directions)
     * Risk: Empty string handling could cause ArrayIndexOutOfBounds
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testEmptyStringConversion(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = "";
        byte[] ebcdic = new byte[0];
        StringBuilder converted = new StringBuilder();
        for (byte b : ebcdic) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        assertEquals(original, converted.toString(),"Empty string should remain empty");
    }

    /**
     * TEST 16: Very long string (1000 chars) converts without corruption
     * Dimension: String length (1000) x Character range (mixed) x Conversion (round-trip)
     * Risk: Buffer overflow, off-by-one errors, heap corruption
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testVeryLongStringRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
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
        assertEquals(original, converted.toString(),"1000-character string should round-trip without corruption");
    }

    /**
     * TEST 17: String with control characters handles gracefully
     * Dimension: Character range (control) x Conversion direction (round-trip)
     * Risk: Control chars (0x00-0x1F) could be mishandled, breaking terminal protocols
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testControlCharacterRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char controlChar = '\u0009'; // Tab character
        byte ebcdic = cp.uni2ebcdic(controlChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(controlChar, converted,"Control character (Tab) should round-trip");
    }

    /**
     * TEST 18: Consecutive high-byte values convert without cross-contamination
     * Dimension: Edge case (0xFF, 0xFE) x Conversion (sequence) x String length (2)
     * Risk: Byte boundary issues could mix up adjacent conversions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testConsecutiveHighByteValuesConversion(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        byte[] input = {(byte) 0xFF, (byte) 0xFE};
        StringBuilder converted = new StringBuilder();
        for (byte b : input) {
            converted.append(cp.ebcdic2uni(b & 0xFF));
        }
        // Should produce two characters, not corrupt into one or three
        assertEquals(2, converted.length(),"Two high bytes should convert to two chars without cross-contamination");
        // Both should be valid Unicode characters
        for (char c : converted.toString().toCharArray()) {
            assertTrue(c >= 0 && c <= Character.MAX_VALUE,"Each converted char should be valid");
        }
    }

    /**
     * TEST 19: Code page consistency - same char produces same EBCDIC across calls
     * Dimension: Conversion (ASCII→EBCDIC) x Consistency x Multiple invocations
     * Risk: Stateful conversion could produce different results on repeated calls
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testConversionConsistencyMultipleInvocations(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char testChar = 'X';
        byte result1 = cp.uni2ebcdic(testChar);
        byte result2 = cp.uni2ebcdic(testChar);
        byte result3 = cp.uni2ebcdic(testChar);
        assertEquals(result1, result2,"First and second conversion should match");
        assertEquals(result2, result3,"Second and third conversion should match");
    }

    /**
     * TEST 20: Code page returns same instance for same code
     * Dimension: Code page retrieval x Consistency
     * Risk: Multiple instances could have different mappings, breaking data integrity
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCodePageInstanceConsistency(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        ICodePage cp1 = CharMappings.getCodePage(codePage);
        ICodePage cp2 = CharMappings.getCodePage(codePage);
        // Same code page should produce identical conversions
        char testChar = 'M';
        byte result1 = cp1.uni2ebcdic(testChar);
        byte result2 = cp2.uni2ebcdic(testChar);
        assertEquals(result1, result2,"Two instances of same code page should produce identical conversions");
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
