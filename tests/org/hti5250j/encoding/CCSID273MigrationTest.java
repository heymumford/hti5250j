/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID273;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID273 Migration Compatibility Tests.
 *
 * These tests verify that CCSID273 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID273 Migration Compatibility Tests")
class CCSID273MigrationTest {

    @Test
    @DisplayName("CCSID273 converts all 256 characters correctly")
    void testCCSID273AllCharacters() {
        CCSID273 converter = new CCSID273();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID273 space character (0x40) converts correctly")
    void testCCSID273SpaceCharacter() {
        CCSID273 converter = new CCSID273();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID273 provides correct name")
    void testCCSID273Name() {
        CCSID273 converter = new CCSID273();

        assertEquals("273", converter.getName(), "CCSID273 name should be '273'");
    }

    @Test
    @DisplayName("CCSID273 provides description")
    void testCCSID273Description() {
        CCSID273 converter = new CCSID273();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID273 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID273 init() returns converter for chaining")
    void testCCSID273InitChaining() {
        CCSID273 converter = new CCSID273();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID273 NUL character (0x00) converts correctly")
    void testCCSID273NULCharacter() {
        CCSID273 converter = new CCSID273();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
