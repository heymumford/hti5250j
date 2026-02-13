/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID37;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID37 Migration Compatibility Tests.
 *
 * These tests verify that CCSID37 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID37 Migration Compatibility Tests")
class CCSID37MigrationTest {

    @Test
    @DisplayName("CCSID37 converts all 256 characters correctly")
    void testCCSID37AllCharacters() {
        CCSID37 converter = new CCSID37();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID37 space character (0x40) converts correctly")
    void testCCSID37SpaceCharacter() {
        CCSID37 converter = new CCSID37();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID37 'a' character (0x81) converts correctly")
    void testCCSID37LetterA() {
        CCSID37 converter = new CCSID37();
        converter.init();

        char lowerA = converter.ebcdic2uni(0x81);
        assertEquals('a', lowerA, "EBCDIC 0x81 should convert to lowercase 'a'");
    }

    @Test
    @DisplayName("CCSID37 provides correct name")
    void testCCSID37Name() {
        CCSID37 converter = new CCSID37();

        assertEquals("37", converter.getName(), "CCSID37 name should be '37'");
    }

    @Test
    @DisplayName("CCSID37 provides description")
    void testCCSID37Description() {
        CCSID37 converter = new CCSID37();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID37 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID37 init() returns converter for chaining")
    void testCCSID37InitChaining() {
        CCSID37 converter = new CCSID37();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID37 NUL character (0x00) converts correctly")
    void testCCSID37NULCharacter() {
        CCSID37 converter = new CCSID37();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }

    @Test
    @DisplayName("CCSID37 'A' character (0xC1) converts correctly")
    void testCCSID37CapitalLetterA() {
        CCSID37 converter = new CCSID37();
        converter.init();

        char capitalA = converter.ebcdic2uni(0xC1);
        assertEquals('A', capitalA, "EBCDIC 0xC1 should convert to uppercase 'A'");
    }

    @Test
    @DisplayName("CCSID37 '0' character (0xF0) converts correctly")
    void testCCSID37ZeroDigit() {
        CCSID37 converter = new CCSID37();
        converter.init();

        char zero = converter.ebcdic2uni(0xF0);
        assertEquals('0', zero, "EBCDIC 0xF0 should convert to digit '0'");
    }
}
