/*
 * SPDX-FileCopyrightText: 2026 Test Agent
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding.builtin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for CCSID930 codec.
 * Tests the shift-in and shift-out byte handling for Japanese DBCS support.
 */
public class CCSID930Test {

    private CCSID930 codec;

    @BeforeEach
    public void setUp() {
        codec = new CCSID930();
    }

    /**
     * RED Phase Test: Verify that shift-in byte (0x0E) correctly activates double-byte mode.
     */
    @Test
    public void testShiftInByte() {
        // Arrange
        int shiftInByte = 0x0E;
        assertFalse(codec.isDoubleByteActive(), "Double-byte mode should be inactive before shift-in");

        // Act
        char result = codec.ebcdic2uni(shiftInByte);

        // Assert
        assertTrue(codec.isDoubleByteActive(), "Double-byte mode should be active after shift-in");
        assertEquals(0, result, "Shift-in should return 0 (control character)");
        assertFalse(codec.secondByteNeeded(), "secondByteNeeded should be false after shift-in");
    }

    /**
     * RED Phase Test: Verify that shift-out byte (0x0F) correctly deactivates double-byte mode.
     */
    @Test
    public void testShiftOutByte() {
        // Arrange - first activate double-byte mode
        codec.ebcdic2uni(0x0E); // shift-in
        assertTrue(codec.isDoubleByteActive(), "Double-byte mode should be active");

        // Act
        int shiftOutByte = 0x0F;
        char result = codec.ebcdic2uni(shiftOutByte);

        // Assert
        assertFalse(codec.isDoubleByteActive(), "Double-byte mode should be inactive after shift-out");
        assertEquals(0, result, "Shift-out should return 0 (control character)");
        assertFalse(codec.secondByteNeeded(), "secondByteNeeded should be false after shift-out");
    }

    /**
     * RED Phase Test: Verify that shift-in followed by double-byte sequence works correctly.
     */
    @Test
    public void testDoubleByteSequence() {
        // Arrange
        codec.ebcdic2uni(0x0E); // shift-in
        assertTrue(codec.isDoubleByteActive(), "Double-byte mode should be active");

        // Act - first byte of double-byte sequence
        char firstResult = codec.ebcdic2uni(0x50); // arbitrary first byte
        assertTrue(codec.secondByteNeeded(), "secondByteNeeded should be true after first byte");
        assertEquals(0, firstResult, "First byte should return 0");

        // Act - second byte of double-byte sequence
        char secondResult = codec.ebcdic2uni(0x60); // arbitrary second byte
        assertFalse(codec.secondByteNeeded(), "secondByteNeeded should be false after second byte");
        assertNotEquals(0, secondResult, "Second byte should produce a character");
    }

    /**
     * RED Phase Test: Verify codec metadata.
     */
    @Test
    public void testCodecMetadata() {
        assertEquals("930", codec.getName());
        assertEquals("Japan Katakana (extended range), DBCS", codec.getDescription());
        assertEquals("930", codec.getEncoding());
    }
}
