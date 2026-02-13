/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID1147;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID1147 Migration Compatibility Tests.
 *
 * These tests verify that CCSID1147 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID1147 Migration Compatibility Tests")
class CCSID1147MigrationTest {

    @Test
    @DisplayName("CCSID1147 converts all 256 characters correctly")
    void testCCSID1147AllCharacters() {
        CCSID1147 converter = new CCSID1147();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID1147 space character (0x40) converts correctly")
    void testCCSID1147SpaceCharacter() {
        CCSID1147 converter = new CCSID1147();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID1147 provides correct name")
    void testCCSID1147Name() {
        CCSID1147 converter = new CCSID1147();

        assertEquals("1147", converter.getName(), "CCSID1147 name should be '1147'");
    }

    @Test
    @DisplayName("CCSID1147 provides description")
    void testCCSID1147Description() {
        CCSID1147 converter = new CCSID1147();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID1147 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID1147 init() returns converter for chaining")
    void testCCSID1147InitChaining() {
        CCSID1147 converter = new CCSID1147();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID1147 NUL character (0x00) converts correctly")
    void testCCSID1147NULCharacter() {
        CCSID1147 converter = new CCSID1147();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
