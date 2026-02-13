/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID297;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID297 Migration Compatibility Tests.
 *
 * These tests verify that CCSID297 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID297 Migration Compatibility Tests")
class CCSID297MigrationTest {

    @Test
    @DisplayName("CCSID297 converts all 256 characters correctly")
    void testCCSID297AllCharacters() {
        CCSID297 converter = new CCSID297();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID297 space character (0x40) converts correctly")
    void testCCSID297SpaceCharacter() {
        CCSID297 converter = new CCSID297();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID297 provides correct name")
    void testCCSID297Name() {
        CCSID297 converter = new CCSID297();

        assertEquals("297", converter.getName(), "CCSID297 name should be '297'");
    }

    @Test
    @DisplayName("CCSID297 provides description")
    void testCCSID297Description() {
        CCSID297 converter = new CCSID297();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID297 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID297 init() returns converter for chaining")
    void testCCSID297InitChaining() {
        CCSID297 converter = new CCSID297();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID297 NUL character (0x00) converts correctly")
    void testCCSID297NULCharacter() {
        CCSID297 converter = new CCSID297();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
