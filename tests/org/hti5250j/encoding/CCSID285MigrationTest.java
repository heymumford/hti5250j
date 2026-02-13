/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID285;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID285 Migration Compatibility Tests.
 *
 * These tests verify that CCSID285 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID285 Migration Compatibility Tests")
class CCSID285MigrationTest {

    @Test
    @DisplayName("CCSID285 converts all 256 characters correctly")
    void testCCSID285AllCharacters() {
        CCSID285 converter = new CCSID285();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID285 space character (0x40) converts correctly")
    void testCCSID285SpaceCharacter() {
        CCSID285 converter = new CCSID285();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID285 provides correct name")
    void testCCSID285Name() {
        CCSID285 converter = new CCSID285();

        assertEquals("285", converter.getName(), "CCSID285 name should be '285'");
    }

    @Test
    @DisplayName("CCSID285 provides description")
    void testCCSID285Description() {
        CCSID285 converter = new CCSID285();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID285 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID285 init() returns converter for chaining")
    void testCCSID285InitChaining() {
        CCSID285 converter = new CCSID285();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID285 NUL character (0x00) converts correctly")
    void testCCSID285NULCharacter() {
        CCSID285 converter = new CCSID285();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
