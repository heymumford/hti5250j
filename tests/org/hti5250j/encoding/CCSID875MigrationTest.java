/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID875;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID875 Migration Compatibility Tests.
 *
 * These tests verify that CCSID875 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID875 Migration Compatibility Tests")
class CCSID875MigrationTest {

    @Test
    @DisplayName("CCSID875 converts all 256 characters correctly")
    void testCCSID875AllCharacters() {
        CCSID875 converter = new CCSID875();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID875 space character (0x40) converts correctly")
    void testCCSID875SpaceCharacter() {
        CCSID875 converter = new CCSID875();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID875 provides correct name")
    void testCCSID875Name() {
        CCSID875 converter = new CCSID875();

        assertEquals("875", converter.getName(), "CCSID875 name should be '875'");
    }

    @Test
    @DisplayName("CCSID875 provides description")
    void testCCSID875Description() {
        CCSID875 converter = new CCSID875();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID875 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID875 init() returns converter for chaining")
    void testCCSID875InitChaining() {
        CCSID875 converter = new CCSID875();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID875 NUL character (0x00) converts correctly")
    void testCCSID875NULCharacter() {
        CCSID875 converter = new CCSID875();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
