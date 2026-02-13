/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID280;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID280 Migration Compatibility Tests.
 *
 * These tests verify that CCSID280 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID280 Migration Compatibility Tests")
class CCSID280MigrationTest {

    @Test
    @DisplayName("CCSID280 converts all 256 characters correctly")
    void testCCSID280AllCharacters() {
        CCSID280 converter = new CCSID280();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID280 space character (0x40) converts correctly")
    void testCCSID280SpaceCharacter() {
        CCSID280 converter = new CCSID280();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID280 provides correct name")
    void testCCSID280Name() {
        CCSID280 converter = new CCSID280();

        assertEquals("280", converter.getName(), "CCSID280 name should be '280'");
    }

    @Test
    @DisplayName("CCSID280 provides description")
    void testCCSID280Description() {
        CCSID280 converter = new CCSID280();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID280 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID280 init() returns converter for chaining")
    void testCCSID280InitChaining() {
        CCSID280 converter = new CCSID280();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID280 NUL character (0x00) converts correctly")
    void testCCSID280NULCharacter() {
        CCSID280 converter = new CCSID280();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
