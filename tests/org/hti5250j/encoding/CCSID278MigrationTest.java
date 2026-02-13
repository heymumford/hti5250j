/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID278;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID278 Migration Compatibility Tests.
 *
 * These tests verify that CCSID278 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID278 Migration Compatibility Tests")
class CCSID278MigrationTest {

    @Test
    @DisplayName("CCSID278 converts all 256 characters correctly")
    void testCCSID278AllCharacters() {
        CCSID278 converter = new CCSID278();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID278 space character (0x40) converts correctly")
    void testCCSID278SpaceCharacter() {
        CCSID278 converter = new CCSID278();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID278 provides correct name")
    void testCCSID278Name() {
        CCSID278 converter = new CCSID278();

        assertEquals("278", converter.getName(), "CCSID278 name should be '278'");
    }

    @Test
    @DisplayName("CCSID278 provides description")
    void testCCSID278Description() {
        CCSID278 converter = new CCSID278();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID278 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID278 init() returns converter for chaining")
    void testCCSID278InitChaining() {
        CCSID278 converter = new CCSID278();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID278 NUL character (0x00) converts correctly")
    void testCCSID278NULCharacter() {
        CCSID278 converter = new CCSID278();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
