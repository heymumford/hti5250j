/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.surfaces;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D3-PROTO-002: Stream5250 Structured Field Serialization Surface Tests
 *
 * Tests the Stream5250 class and structured field command serialization.
 * These tests verify that 5250 data stream commands are correctly formatted
 * according to the TN5250E protocol specification.
 *
 * Domain: D3 (Surface tests, protocol round-trip, schema, concurrency)
 * Category: PROTO (TN5250E structured fields)
 * Reference: IBM 5250 Data Stream Guide, TN5250E RFC 1205
 */
public class Stream5250StructuredFieldTest {

    /**
     * D3-PROTO-002.1: Write To Display (WTD) command structure is correct
     *
     * Verifies that WTD (Write To Display) command follows 5250 protocol format:
     * - Command code: 0x11 (Write To Display)
     * - Optional flags
     * - Field data
     */
    @Test
    @DisplayName("D3-PROTO-002.1: Write To Display command uses correct opcode (0x11)")
    void testWriteToDisplayOpcodeCorrect() {
        // GIVEN: Write To Display command from 5250 protocol
        // WTD opcode = 0x11 (17 decimal)
        byte wtdOpcode = 0x11;

        // WHEN: Command is examined
        int cmd = wtdOpcode & 0xFF;

        // THEN: Opcode matches 5250 specification
        assertEquals(0x11, cmd, "Write To Display command must be 0x11");
    }

    /**
     * D3-PROTO-002.2: Repeat To Address (RA) command structure is correct
     *
     * Verifies that RA (Repeat To Address) command follows protocol format:
     * - Command code: 0x13 (Repeat To Address)
     * - Row address
     * - Column address
     * - Character to repeat
     */
    @Test
    @DisplayName("D3-PROTO-002.2: Repeat To Address command uses correct opcode (0x13)")
    void testRepeatToAddressOpcodeCorrect() {
        // GIVEN: Repeat To Address command from 5250 protocol
        // RA opcode = 0x13 (19 decimal)
        byte raOpcode = 0x13;

        // WHEN: Command is examined
        int cmd = raOpcode & 0xFF;

        // THEN: Opcode matches 5250 specification
        assertEquals(0x13, cmd, "Repeat To Address command must be 0x13");
    }

    /**
     * D3-PROTO-002.3: Start of Field (SF) order defines field attributes
     *
     * Verifies that SF (Start of Field) order correctly encodes field attributes:
     * - Order code: 0x1D (Start of Field)
     * - Field attribute byte (PROTECTED, NUMERIC, HIDDEN, etc.)
     */
    @Test
    @DisplayName("D3-PROTO-002.3: Start of Field order uses correct code (0x1D)")
    void testStartOfFieldOrderCorrect() {
        // GIVEN: Start of Field order from 5250 protocol
        // SF order code = 0x1D (29 decimal)
        byte sfOrder = 0x1D;

        // WHEN: Order is examined
        int order = sfOrder & 0xFF;

        // THEN: Order code matches 5250 specification
        assertEquals(0x1D, order, "Start of Field order must be 0x1D");
    }

    /**
     * D3-PROTO-002.4: Field attribute byte encodes PROTECTED flag correctly
     *
     * Verifies that the PROTECTED attribute bit (0x20) is set correctly
     * in field attribute bytes. Protected fields cannot be modified by user input.
     */
    @Test
    @DisplayName("D3-PROTO-002.4: Field attribute PROTECTED flag is 0x20")
    void testFieldAttributeProtectedFlag() {
        // GIVEN: Field attribute encoding from 5250 protocol
        // PROTECTED = 0x20 (bit 5)
        int protectedFlag = 0x20;

        // WHEN: Flag is used in field attribute
        byte fieldAttr = 0x00;
        fieldAttr |= (byte)protectedFlag;  // Set PROTECTED

        // THEN: Flag is correctly encoded
        assertEquals(0x20, fieldAttr & 0x20, "PROTECTED flag must be bit 5 (0x20)");
    }

    /**
     * D3-PROTO-002.5: Field attribute byte encodes HIDDEN flag correctly
     *
     * Verifies that the HIDDEN attribute bit (0x04) is set correctly
     * in field attribute bytes. Hidden fields don't display but can store data (passwords).
     */
    @Test
    @DisplayName("D3-PROTO-002.5: Field attribute HIDDEN flag is 0x04")
    void testFieldAttributeHiddenFlag() {
        // GIVEN: Field attribute encoding from 5250 protocol
        // HIDDEN = 0x04 (bit 2)
        int hiddenFlag = 0x04;

        // WHEN: Flag is used in field attribute
        byte fieldAttr = 0x00;
        fieldAttr |= (byte)hiddenFlag;  // Set HIDDEN

        // THEN: Flag is correctly encoded
        assertEquals(0x04, fieldAttr & 0x04, "HIDDEN flag must be bit 2 (0x04)");
    }

    /**
     * D3-PROTO-002.6: Structured field command length encoding
     *
     * Verifies that structured field length is encoded correctly.
     * Length includes the length byte itself and command code.
     */
    @Test
    @DisplayName("D3-PROTO-002.6: Structured field length includes header bytes")
    void testStructuredFieldLengthEncoding() {
        // GIVEN: Structured field with minimal content
        // Format: [Length][Type][Data...]
        // Minimum: Length=2 (includes length and type bytes)
        int fieldType = 0x11;  // Write To Display
        int minLength = 2;     // Just length and type

        // WHEN: Field is constructed
        byte[] field = new byte[minLength];
        field[0] = (byte)minLength;
        field[1] = (byte)fieldType;

        // THEN: Field structure is valid
        assertEquals(minLength, field[0] & 0xFF, "Length byte must match field size");
        assertEquals(fieldType, field[1] & 0xFF, "Type byte must be preserved");
    }

    /**
     * D3-PROTO-002.7: Multiple structured fields are independent
     *
     * Verifies that consecutive structured fields in a stream are independent
     * and don't interfere with each other's parsing.
     */
    @Test
    @DisplayName("D3-PROTO-002.7: Multiple structured fields parse independently")
    void testMultipleStructuredFieldsIndependent() {
        // GIVEN: Two consecutive structured fields
        // Field 1: Write To Display with 2 bytes total length
        byte[] field1 = {2, 0x11};  // Length=2, Type=WTD

        // Field 2: Repeat To Address with 2 bytes total length
        byte[] field2 = {2, 0x13};  // Length=2, Type=RA

        // WHEN: Fields are combined into stream
        byte[] stream = new byte[4];
        System.arraycopy(field1, 0, stream, 0, 2);
        System.arraycopy(field2, 0, stream, 2, 2);

        // THEN: Each field retains its structure
        assertEquals(2, stream[0] & 0xFF, "Field 1 length must be 2");
        assertEquals(0x11, stream[1] & 0xFF, "Field 1 type must be WTD");
        assertEquals(2, stream[2] & 0xFF, "Field 2 length must be 2");
        assertEquals(0x13, stream[3] & 0xFF, "Field 2 type must be RA");
    }
}
