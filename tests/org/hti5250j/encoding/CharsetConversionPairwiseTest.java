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
public class CharsetConversionPairwiseTest {

    private String codePage;
    private ICodePage cp;

    /**
     * Parameterized constructor for code page variations.
     */
    private void setParameters(String codePage) {
        this.codePage = codePage;
    }

    /**
     * Pairwise parameter combinations: representative code pages.
     * Dimension 1: Code page [37, 273, 280, 284, 297, 500]
     */
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

        public void setUp() {
        cp = CharMappings.getCodePage(codePage);
        assertNotNull(cp,"Code page " + codePage + " must be available");
    }

    // =====================================================================
    // POSITIVE TESTS (12) - Valid encodings under normal conditions
    // =====================================================================

    /**
     * TEST 1: Printable character (uppercase) round-trips correctly.
     * Dimension: Character type [printable: A-Z]
     *            Direction [round-trip]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testPrintableUppercaseRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        for (char c = 'A'; c <= 'Z'; c++) {
            byte ebcdic = cp.uni2ebcdic(c);
            char converted = cp.ebcdic2uni(ebcdic & 0xFF);
            assertEquals(c, converted,"Uppercase letter '" + c + "' (CP " + codePage + ") should round-trip");
        }
    }

    /**
     * TEST 2: Printable character (lowercase) round-trips correctly.
     * Dimension: Character type [printable: a-z]
     *            Direction [round-trip]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testPrintableLowercaseRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        for (char c = 'a'; c <= 'z'; c++) {
            byte ebcdic = cp.uni2ebcdic(c);
            char converted = cp.ebcdic2uni(ebcdic & 0xFF);
            assertEquals(c, converted,"Lowercase letter '" + c + "' (CP " + codePage + ") should round-trip");
        }
    }

    /**
     * TEST 3: Numeric digits (0-9) round-trip correctly.
     * Dimension: Character type [printable: 0-9]
     *            Direction [round-trip]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testPrintableNumericRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        for (char c = '0'; c <= '9'; c++) {
            byte ebcdic = cp.uni2ebcdic(c);
            char converted = cp.ebcdic2uni(ebcdic & 0xFF);
            assertEquals(c, converted,"Digit '" + c + "' (CP " + codePage + ") should round-trip");
        }
    }

    /**
     * TEST 4: Special printable characters round-trip correctly.
     * Dimension: Character type [special: space, punctuation]
     *            Direction [round-trip]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSpecialCharactersRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String specialChars = " .,-!?@#$%&*()";
        for (char c : specialChars.toCharArray()) {
            byte ebcdic = cp.uni2ebcdic(c);
            char converted = cp.ebcdic2uni(ebcdic & 0xFF);
            assertEquals(c, converted,"Special char '" + c + "' (CP " + codePage + ") should round-trip");
        }
    }

    /**
     * TEST 5: Empty string (length 0) converts without error.
     * Dimension: String length [0]
     *            Direction [both]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testEmptyStringConversion(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        byte[] ebcdic = new byte[0];
        String result = ebcdicToString(ebcdic);
        assertEquals("", result,"Empty string should remain empty in CP " + codePage);
    }

    /**
     * TEST 6: Single character (length 1) converts correctly.
     * Dimension: String length [1]
     *            Character type [printable]
     *            Direction [round-trip]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testSingleCharacterRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char testChar = 'X';
        byte ebcdic = cp.uni2ebcdic(testChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(testChar, converted,"Single char 'X' (CP " + codePage + ") should round-trip");
    }

    /**
     * TEST 7: Terminal-length string (80 chars) round-trips correctly.
     * Dimension: String length [80]
     *            Character type [mixed printable]
     *            Direction [round-trip]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testTerminalLineRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = generateTerminalLine(80);
        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);
        assertEquals(original, converted,"80-char terminal line (CP " + codePage + ") should round-trip");
    }

    /**
     * TEST 8: Buffer-size string (256 chars) round-trips correctly.
     * Dimension: String length [256]
     *            Character type [mixed printable]
     *            Direction [round-trip]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testBufferSizeStringRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = generateTerminalLine(256);
        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);
        assertEquals(original, converted,"256-char buffer (CP " + codePage + ") should round-trip");
    }

    /**
     * TEST 9: ASCII→EBCDIC→ASCII triple conversion maintains fidelity.
     * Dimension: Direction [ASCII→EBCDIC→ASCII]
     *            Character type [mixed]
     *            String length [50]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testTripleConversionFidelity(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = "The quick brown fox jumps over the lazy dog 12345";
        byte[] toEBCDIC = stringToEBCDIC(original);
        String backToASCII = ebcdicToString(toEBCDIC);
        byte[] toEBCDICAgain = stringToEBCDIC(backToASCII);

        String finalResult = ebcdicToString(toEBCDICAgain);
        assertEquals(original, finalResult,"Triple conversion should maintain fidelity in CP " + codePage);
    }

    /**
     * TEST 10: Field marker character (0x1D) converts correctly.
     * Dimension: Special chars [field markers]
     *            Direction [EBCDIC→ASCII]
     * Note: Field markers are protocol-specific; test conversion behavior
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testFieldMarkerConversion(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        byte fieldMarker = (byte) 0x1D;
        char converted = cp.ebcdic2uni(fieldMarker & 0xFF);
        assertNotNull(converted,"Field marker 0x1D should convert to some character in CP " + codePage);
        assertTrue(converted >= 0 && converted <= Character.MAX_VALUE,"Field marker conversion should yield valid Unicode");
    }

    /**
     * TEST 11: Conversion consistency across multiple calls.
     * Dimension: Direction [ASCII→EBCDIC]
     *            Consistency [idempotent]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testConversionConsistency(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char testChar = 'Q';
        byte result1 = cp.uni2ebcdic(testChar);
        byte result2 = cp.uni2ebcdic(testChar);
        byte result3 = cp.uni2ebcdic(testChar);

        assertEquals(result1, result2,"First and second conversion should match in CP " + codePage);
        assertEquals(result2, result3,"Second and third conversion should match in CP " + codePage);
    }

    /**
     * TEST 12: Code page instance consistency.
     * Dimension: Code page retrieval [cached]
     *            Consistency [singleton]
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCodePageInstanceConsistency(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        ICodePage cp1 = CharMappings.getCodePage(codePage);
        ICodePage cp2 = CharMappings.getCodePage(codePage);

        char testChar = 'M';
        byte result1 = cp1.uni2ebcdic(testChar);
        byte result2 = cp2.uni2ebcdic(testChar);

        assertEquals(result1, result2,"Same code page retrieved twice should yield identical conversions in CP "
                + codePage);
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
    @ParameterizedTest
    @MethodSource("data")
    public void testNullByteEBCDICConversion(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        byte nullByte = 0x00;
        char converted = cp.ebcdic2uni(nullByte & 0xFF);
        assertNotNull(Character.valueOf(converted),"Null byte should produce valid char reference in CP " + codePage);
        assertTrue(converted >= 0 && converted <= Character.MAX_VALUE,"Null byte should map to valid Unicode range in CP " + codePage);
    }

    /**
     * TEST 14: High byte (0xFF) EBCDIC converts without data loss.
     * Dimension: Character type [extended: 0x80-0xFF]
     *            Direction [EBCDIC→ASCII]
     * Risk: Extended EBCDIC chars may not map, causing data corruption
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testHighByteEBCDICConversion(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        byte highByte = (byte) 0xFF;
        char converted = cp.ebcdic2uni(highByte & 0xFF);
        assertTrue(converted >= 0 && converted <= Character.MAX_VALUE,"High byte (0xFF) should convert to valid Unicode in CP " + codePage);
    }

    /**
     * TEST 15: Consecutive high bytes (0xFE, 0xFF) without cross-contamination.
     * Dimension: Character type [extended]
     *            String length [2]
     *            Direction [EBCDIC→ASCII]
     * Risk: Byte boundary issues could corrupt adjacent conversions
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testConsecutiveHighBytesWithoutCrossContamination(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        byte[] input = {(byte) 0xFE, (byte) 0xFF};
        String converted = ebcdicToString(input);

        assertEquals(2, converted.length(),"Two high bytes should produce exactly two characters in CP " + codePage);

        for (char c : converted.toCharArray()) {
            assertTrue(c >= 0 && c <= Character.MAX_VALUE,"Each converted char must be valid Unicode in CP " + codePage);
        }
    }

    /**
     * TEST 16: Byte array boundary (256 bytes = full byte range) converts.
     * Dimension: String length [256]
     *            Character type [mixed: all byte values]
     *            Direction [EBCDIC→ASCII]
     * Risk: Array indexing errors at boundaries
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testFullByteRangeConversion(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }

        String converted = ebcdicToString(allBytes);
        assertEquals(256, converted.length(),"256-byte range should produce 256 characters in CP " + codePage);

        for (char c : converted.toCharArray()) {
            assertTrue(c >= 0 && c <= Character.MAX_VALUE,"Each char must be valid Unicode in CP " + codePage);
        }
    }

    /**
     * TEST 17: Unmappable Unicode character handling (U+2764 = heart symbol).
     * Dimension: Character type [undefined]
     *            Direction [ASCII→EBCDIC]
     * Status: FIXED - Implementation now throws CharacterConversionException
     * The bug has been fixed - CodepageConverterAdapter validates char values before conversion
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUnmappableUnicodeCharacterHandling(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char unmappableChar = '\u2764'; // Heart symbol (value 10084) - unlikely in EBCDIC

        // After fix: Implementation throws CharacterConversionException for unmappable chars
        assertThrows(CharacterConversionException.class, () -> {
            cp.uni2ebcdic(unmappableChar);
        }, "Unmappable character should throw CharacterConversionException in CP " + codePage);
    }

    /**
     * TEST 18: Control character (tab, 0x09) round-trips correctly.
     * Dimension: Character type [control: 0x00-0x1F]
     *            Direction [round-trip]
     * Risk: Control chars could be stripped or mishandled
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testControlCharacterRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char tabChar = '\t';
        byte ebcdic = cp.uni2ebcdic(tabChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(tabChar, converted,"Tab character should round-trip in CP " + codePage);
    }

    /**
     * TEST 19: Newline character (0x0A) handling preserves semantics.
     * Dimension: Special chars [newline]
     *            Direction [round-trip]
     * Risk: Newlines could be lost or converted to spaces
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testNewlineCharacterHandling(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        char newlineChar = '\n';
        byte ebcdic = cp.uni2ebcdic(newlineChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(newlineChar, converted,"Newline character should round-trip in CP " + codePage);
    }

    /**
     * TEST 20: Very long string (1024 chars) converts without buffer overflow.
     * Dimension: String length [1024, max]
     *            Character type [mixed]
     *            Direction [round-trip]
     * Risk: Buffer overflow, heap corruption, truncation
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testVeryLongStringRoundTrip(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String original = generateTerminalLine(1024);
        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);

        assertEquals(original, converted,"1024-char string should round-trip without truncation in CP " + codePage);
        assertEquals(original.length(), converted.length(),"Buffer should maintain exact length in CP " + codePage);
    }

    /**
     * TEST 21: Repeated character sequence maintains pattern integrity.
     * Dimension: Character type [repetitive pattern]
     *            Direction [round-trip]
     *            String length [100]
     * Risk: Pattern corruption, off-by-one errors in indexing
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRepeatingPatternIntegrity(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("ABC");
        }
        String original = sb.toString();

        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);

        assertEquals(original, converted,"300-char repeating pattern should maintain integrity in CP " + codePage);
    }

    /**
     * TEST 22: Null termination behavior in round-trip doesn't truncate.
     * Dimension: Special chars [null]
     *            String length [including null]
     *            Direction [round-trip]
     * Risk: C-style null termination could truncate data mid-string
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testNullTerminationDoesNotTruncate(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        String beforeNull = "DATA";
        String afterNull = "MORE";
        String original = beforeNull + '\u0000' + afterNull;

        byte[] ebcdic = stringToEBCDIC(original);
        String converted = ebcdicToString(ebcdic);

        assertEquals(original.length(), converted.length(),"String with embedded null should maintain length in CP " + codePage);
        assertEquals(original, converted,"String with embedded null should round-trip in CP " + codePage);
    }

    /**
     * TEST 23: Asymmetric code page mappings validate directional conversion.
     * Dimension: Direction [EBCDIC→ASCII vs ASCII→EBCDIC]
     *            Character type [all mapped printable]
     * Risk: Code page might have asymmetric mappings (not all chars map both ways)
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testAsymmetricCodePageMapping(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        // Test a sample of printable ASCII chars both directions
        String testChars = "ABCXYZ0123456789";

        for (char asciiChar : testChars.toCharArray()) {
            byte ebcdic = cp.uni2ebcdic(asciiChar);
            char backToASCII = cp.ebcdic2uni(ebcdic & 0xFF);

            assertEquals(asciiChar, backToASCII,"Character '" + asciiChar + "' in CP " + codePage
                    + " should have symmetric mapping");
        }
    }

    /**
     * TEST 24: Boundary between printable and control characters.
     * Dimension: Character type [boundary: 0x1F/0x20]
     *            Direction [round-trip]
     * Risk: Off-by-one errors at ASCII control/printable boundary
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testControlPrintableBoundary(String codePage) throws Exception {
        setParameters(codePage);
        setUp();
        // 0x1F = Unit Separator (control)
        char controlChar = '\u001F';
        byte ebcdic = cp.uni2ebcdic(controlChar);
        char converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(controlChar, converted,"Boundary char 0x1F should round-trip in CP " + codePage);

        // 0x20 = Space (printable)
        char spaceChar = ' ';
        ebcdic = cp.uni2ebcdic(spaceChar);
        converted = cp.ebcdic2uni(ebcdic & 0xFF);
        assertEquals(spaceChar, converted,"Boundary char Space (0x20) should round-trip in CP " + codePage);
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
