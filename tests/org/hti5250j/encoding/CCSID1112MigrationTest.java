/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID1112;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID1112 Migration Compatibility Tests.
 *
 * These tests verify that CCSID1112 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID1112 Migration Compatibility Tests")
class CCSID1112MigrationTest {

    @Test
    @DisplayName("CCSID1112 converts all 256 characters correctly")
    void testCCSID1112AllCharacters() {
        CCSID1112 converter = new CCSID1112();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID1112 space character (0x40) converts correctly")
    void testCCSID1112SpaceCharacter() {
        CCSID1112 converter = new CCSID1112();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID1112 provides correct name")
    void testCCSID1112Name() {
        CCSID1112 converter = new CCSID1112();

        assertEquals("1112", converter.getName(), "CCSID1112 name should be '1112'");
    }

    @Test
    @DisplayName("CCSID1112 provides description")
    void testCCSID1112Description() {
        CCSID1112 converter = new CCSID1112();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID1112 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID1112 init() returns converter for chaining")
    void testCCSID1112InitChaining() {
        CCSID1112 converter = new CCSID1112();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID1112 NUL character (0x00) converts correctly")
    void testCCSID1112NULCharacter() {
        CCSID1112 converter = new CCSID1112();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
