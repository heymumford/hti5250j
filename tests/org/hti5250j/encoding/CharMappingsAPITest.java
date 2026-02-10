/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D1-EBCDIC: CharMappings API Surface Tests
 *
 * Tests the public CharMappings API for codec availability and basic codec behavior.
 * This is a minimal working test for Phase 14 test ID traceability.
 *
 * Domain: D1 (Unit tests, isolation, pre-commit gate)
 * Category: EBCDIC (character set encoding)
 * Format: D1-EBCDIC-NNN (test ID)
 */
public class CharMappingsAPITest {

    /**
     * D1-EBCDIC-001: CharMappings.getCodePage() returns valid ICodePage for US English (CCSID 37)
     *
     * Verifies that the public API can retrieve a codec for standard CCSID 37 (USA/Canada).
     * This is the baseline test for codec availability.
     */
    @Test
    @DisplayName("D1-EBCDIC-001: getCodePage() returns valid ICodePage for CCSID 37")
    void testGetCodePageReturnsValidInstanceForCcsid37() {
        // GIVEN: US English code page (CCSID 37)
        String ccsid = "37";

        // WHEN: Request codec from public API
        ICodePage codec = CharMappings.getCodePage(ccsid);

        // THEN: Returns non-null codec
        assertNotNull(codec, "CharMappings.getCodePage('37') should return non-null ICodePage");
    }

    /**
     * D1-EBCDIC-002: Round-trip ASCII → EBCDIC → ASCII preserves content
     *
     * Verifies that converting ASCII to EBCDIC and back to ASCII yields the original string.
     * This tests bidirectional codec symmetry for common alphanumeric characters.
     */
    @Test
    @DisplayName("D1-EBCDIC-002: Round-trip ASCII → EBCDIC → ASCII preserves content")
    void testRoundTripPreservesContent() {
        // GIVEN: ASCII string with uppercase, lowercase, digits, and punctuation
        String input = "Hello123";
        ICodePage codec = CharMappings.getCodePage("37");

        // WHEN: Convert to EBCDIC and back
        byte[] ebcdic = new byte[input.length()];
        for (int i = 0; i < input.length(); i++) {
            ebcdic[i] = codec.uni2ebcdic(input.charAt(i));
        }

        StringBuilder output = new StringBuilder();
        for (byte b : ebcdic) {
            output.append(codec.ebcdic2uni(b & 0xFF));
        }

        // THEN: Output matches input
        assertEquals(input, output.toString(), "Round-trip conversion should preserve ASCII content");
    }

    /**
     * D1-EBCDIC-003: Numeric characters round-trip correctly
     *
     * Verifies that numeric characters (0-9) specifically round-trip through
     * EBCDIC conversion. This supports Phase 14 surface tests for numeric field extraction.
     */
    @Test
    @DisplayName("D1-EBCDIC-003: Numeric characters round-trip correctly")
    void testNumericCharactersRoundTrip() {
        // GIVEN: Numeric string
        String input = "0123456789";
        ICodePage codec = CharMappings.getCodePage("37");

        // WHEN: Convert to EBCDIC and back
        byte[] ebcdic = new byte[input.length()];
        for (int i = 0; i < input.length(); i++) {
            ebcdic[i] = codec.uni2ebcdic(input.charAt(i));
        }

        StringBuilder output = new StringBuilder();
        for (byte b : ebcdic) {
            output.append(codec.ebcdic2uni(b & 0xFF));
        }

        // THEN: Output matches input exactly
        assertEquals(input, output.toString(), "Numeric characters should round-trip without loss");
    }

    /**
     * D1-EBCDIC-004: Multiple code pages are available
     *
     * Verifies that the CharMappings API can provide multiple code pages,
     * demonstrating that the factory pattern works for different locales.
     */
    @Test
    @DisplayName("D1-EBCDIC-004: Multiple code pages are available")
    void testMultipleCodePagesAvailable() {
        // GIVEN: Multiple EBCDIC code page identifiers
        String[] codepages = {"37", "273", "278", "285", "297", "500"};

        // WHEN: Request each code page
        for (String ccsid : codepages) {
            ICodePage codec = CharMappings.getCodePage(ccsid);

            // THEN: Each returns a valid codec
            assertNotNull(codec, "CharMappings.getCodePage('" + ccsid + "') should return non-null ICodePage");
        }
    }

    /**
     * D1-EBCDIC-005: Invalid code page returns default (CCSID 37)
     *
     * Verifies fallback behavior: requesting an unsupported code page
     * returns the default (USA) code page rather than null.
     */
    @Test
    @DisplayName("D1-EBCDIC-005: Invalid code page returns default (CCSID 37)")
    void testInvalidCodePageReturnsDefault() {
        // GIVEN: An unsupported code page identifier
        String invalidCcsid = "99999";

        // WHEN: Request codec
        ICodePage codec = CharMappings.getCodePage(invalidCcsid);

        // THEN: Returns default codec (not null)
        assertNotNull(codec, "CharMappings.getCodePage() should return default codec for unsupported CCSID");

        // AND: Default codec works (test round-trip with default)
        String testChar = "A";
        byte ebcdic = codec.uni2ebcdic('A');
        char result = codec.ebcdic2uni(ebcdic & 0xFF);
        assertEquals('A', result, "Default codec should convert 'A' correctly");
    }
}
