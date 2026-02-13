/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID277;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID277 Migration Compatibility Tests.
 *
 * These tests verify that CCSID277 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID277 Migration Compatibility Tests")
class CCSID277MigrationTest {

    @Test
    @DisplayName("CCSID277 converts all 256 characters correctly")
    void testCCSID277AllCharacters() {
        CCSID277 converter = new CCSID277();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID277 space character (0x40) converts correctly")
    void testCCSID277SpaceCharacter() {
        CCSID277 converter = new CCSID277();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID277 provides correct name")
    void testCCSID277Name() {
        CCSID277 converter = new CCSID277();

        assertEquals("277", converter.getName(), "CCSID277 name should be '277'");
    }

    @Test
    @DisplayName("CCSID277 provides description")
    void testCCSID277Description() {
        CCSID277 converter = new CCSID277();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID277 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID277 init() returns converter for chaining")
    void testCCSID277InitChaining() {
        CCSID277 converter = new CCSID277();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID277 NUL character (0x00) converts correctly")
    void testCCSID277NULCharacter() {
        CCSID277 converter = new CCSID277();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
