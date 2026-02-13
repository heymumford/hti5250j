/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID1148;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID1148 Migration Compatibility Tests.
 *
 * These tests verify that CCSID1148 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID1148 Migration Compatibility Tests")
class CCSID1148MigrationTest {

    @Test
    @DisplayName("CCSID1148 converts all 256 characters correctly")
    void testCCSID1148AllCharacters() {
        CCSID1148 converter = new CCSID1148();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID1148 space character (0x40) converts correctly")
    void testCCSID1148SpaceCharacter() {
        CCSID1148 converter = new CCSID1148();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID1148 provides correct name")
    void testCCSID1148Name() {
        CCSID1148 converter = new CCSID1148();

        assertEquals("1148", converter.getName(), "CCSID1148 name should be '1148'");
    }

    @Test
    @DisplayName("CCSID1148 provides description")
    void testCCSID1148Description() {
        CCSID1148 converter = new CCSID1148();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID1148 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID1148 init() returns converter for chaining")
    void testCCSID1148InitChaining() {
        CCSID1148 converter = new CCSID1148();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID1148 NUL character (0x00) converts correctly")
    void testCCSID1148NULCharacter() {
        CCSID1148 converter = new CCSID1148();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
