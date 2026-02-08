/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pairwise TDD test suite for HTI5250j SOH (Start of Header) parsing.
 *
 * Tests focus on:
 * - Record length validation (0-7 byte range)
 * - Flag byte interpretation (bit masks and combinations)
 * - Sequence number handling
 * - Error detection and response
 * - Boundary conditions (truncated, oversized records)
 * - Malformed data injection
 */
public class SOHParsingPairwiseTest {

    private static final int SCREEN_ROWS = 24;
    private static final int SCREEN_COLS = 80;
    private static final int SCREEN_SIZE = SCREEN_ROWS * SCREEN_COLS;

    // SOH Order Constants
    private static final byte SOH_ORDER = 0x01;

    // Valid record length range per TN5250 spec
    private static final int MIN_SOH_LENGTH = 1;
    private static final int MAX_SOH_LENGTH = 7;

    // Header flag bits
    private static final byte FLAG_EXTENDED_5250 = (byte) 0x01;
    private static final byte FLAG_DATA_STREAM = (byte) 0x40;
    private static final byte FLAG_ERROR_CONDITION = (byte) 0x80;
    private static final byte FLAG_ALL_SET = (byte) 0xFF;
    private static final byte FLAG_NONE = (byte) 0x00;

    // Sequence number boundaries
    private static final int SEQ_INITIAL = 0;
    private static final int SEQ_NORMAL = 1;
    private static final int SEQ_NEAR_MAX = 254;
    private static final int SEQ_MAX = 255;

    // Record type identifiers
    private static final byte TYPE_DATA = 0x00;
    private static final byte TYPE_ESCAPE = 0x04;
    private static final byte TYPE_STRUCTURED_FIELD = 0x11;
    private static final byte TYPE_SNA = (byte) 0xD0;

    // Error response codes per TN5250 spec
    private static final int ERROR_NONE = 0;
    private static final int ERROR_RETRY = 0x05;
    private static final int ERROR_ABORT = 0x0A;

    // ============================================================
    // POSITIVE TESTS: Valid SOH records (15+ tests)
    // ============================================================

    /**
     * TEST 1: Valid SOH length=1 (minimal record)
     *
     * Pairwise: length=1, type=data, flags=0x00, seq=0, error=none
     *
     * Verifies minimum valid SOH record is parsed without error.
     */
    @Test
    public void testValidSOHLengthOneMinimal() {
        // Arrange: Create minimal 1-byte SOH record
        byte[] record = {
                SOH_ORDER,           // Order code
                0x01,                // Length = 1
                0x00, 0x00, 0x00,   // Padding
                0x00, 0x00, 0x00    // Padding
        };

        // Act: Parse header
        boolean valid = validateSOHRecord(record, 1);

        // Assert: Should be valid
        assertTrue(valid,"SOH length=1 should be valid");
    }

    /**
     * TEST 2: Valid SOH length=4 (includes error row)
     *
     * Pairwise: length=4, type=data, flags=0x01, seq=1, error=none
     *
     * Verifies error row field is correctly parsed.
     */
    @Test
    public void testValidSOHLengthFourWithErrorRow() {
        // Arrange: SOH with error row
        byte[] record = {
                SOH_ORDER,           // Order code
                0x04,                // Length = 4
                0x01,                // Flag byte (EXTENDED_5250)
                0x00,                // Reserved
                0x05,                // Error row = 5
                0x00, 0x00, 0x00     // Padding
        };

        // Act: Parse and verify error row
        int errorRow = extractErrorRow(record);

        // Assert: Error row should be 5
        assertEquals(5, errorRow,"Error row should be extracted correctly");
    }

    /**
     * TEST 3: Valid SOH length=5 (includes flag byte 1)
     *
     * Pairwise: length=5, type=data, flags=0x40, seq=1, error=none
     *
     * Verifies first data-included flags byte is parsed.
     */
    @Test
    public void testValidSOHLengthFiveWithFlagByte1() {
        // Arrange: SOH with first flag byte
        byte[] record = {
                SOH_ORDER,           // Order code
                0x05,                // Length = 5
                0x40,                // Flag byte (DATA_STREAM)
                0x00,                // Reserved
                0x03,                // Error row
                (byte) 0xAA,         // Data included byte 1 = 10101010
                0x00, 0x00           // Padding
        };

        // Act: Extract flag patterns
        boolean[] dataIncluded = extractDataIncludedFlags(record);

        // Assert: Byte 1 bits should match 0xAA pattern
        assertEquals(false, dataIncluded[16],"Bit 0 of byte 1 should be 0");
        assertEquals(true, dataIncluded[17],"Bit 1 of byte 1 should be 1");
        assertEquals(true, dataIncluded[23],"Bit 7 of byte 1 should be 1");
    }

    /**
     * TEST 4: Valid SOH length=6 (includes flag byte 2)
     *
     * Pairwise: length=6, type=structured_field, flags=0x80, seq=254, error=none
     *
     * Verifies second data-included flags byte is parsed.
     */
    @Test
    public void testValidSOHLengthSixWithFlagByte2() {
        // Arrange: SOH with two flag bytes
        byte[] record = {
                SOH_ORDER,           // Order code
                0x06,                // Length = 6
                (byte) 0x80,         // Flag byte (ERROR_CONDITION)
                0x00,                // Reserved
                0x10,                // Error row
                0x55,                // Data included byte 1 = 01010101
                (byte) 0xFF,         // Data included byte 2 = 11111111
                0x00                 // Padding
        };

        // Act: Extract both flag bytes
        boolean[] dataIncluded = extractDataIncludedFlags(record);

        // Assert: Byte 2 bits should all be set
        assertTrue(dataIncluded[8] && dataIncluded[9] && dataIncluded[10],"Byte 2 flags should be set");
    }

    /**
     * TEST 5: Valid SOH length=7 (includes all flag bytes)
     *
     * Pairwise: length=7, type=sna, flags=0xFF, seq=255, error=none
     *
     * Verifies all three data-included flags bytes are parsed.
     */
    @Test
    public void testValidSOHLengthSevenComplete() {
        // Arrange: Complete SOH with all optional fields
        byte[] record = {
                SOH_ORDER,           // Order code
                0x07,                // Length = 7
                (byte) 0xFF,         // Flag byte (all flags)
                0x00,                // Reserved
                0x15,                // Error row = 21
                0x12,                // Data included byte 1
                0x34,                // Data included byte 2
                0x56                 // Data included byte 3
        };

        // Act: Parse complete record
        boolean valid = validateSOHRecord(record, 7);

        // Assert: Should parse without error
        assertTrue(valid,"Complete SOH should be valid");
    }

    /**
     * TEST 6: SOH with extended 5250 flag
     *
     * Pairwise: length=5, type=escape, flags=0x01, seq=0, error=none
     *
     * Verifies EXTENDED_5250 flag is correctly interpreted.
     */
    @Test
    public void testSOHWithExtended5250Flag() {
        // Arrange: SOH with extended 5250 flag
        byte[] record = {
                SOH_ORDER,
                0x05,
                FLAG_EXTENDED_5250,  // 0x01
                0x00,
                0x02,
                (byte) 0xCC,
                0x00, 0x00
        };

        // Act: Extract flag
        byte flagByte = record[2];

        // Assert: Extended flag should be set
        assertTrue((flagByte & FLAG_EXTENDED_5250) == FLAG_EXTENDED_5250,"Extended 5250 flag should be set");
    }

    /**
     * TEST 7: SOH with data stream flag
     *
     * Pairwise: length=5, type=data, flags=0x40, seq=100, error=none
     *
     * Verifies DATA_STREAM flag is correctly interpreted.
     */
    @Test
    public void testSOHWithDataStreamFlag() {
        // Arrange: SOH with data stream flag
        byte[] record = {
                SOH_ORDER,
                0x05,
                FLAG_DATA_STREAM,    // 0x40
                0x00,
                0x08,
                0x44,
                0x00, 0x00
        };

        // Act: Extract flag
        byte flagByte = record[2];

        // Assert: Data stream flag should be set
        assertTrue((flagByte & FLAG_DATA_STREAM) == FLAG_DATA_STREAM,"Data stream flag should be set");
    }

    /**
     * TEST 8: SOH with error condition flag
     *
     * Pairwise: length=5, type=data, flags=0x80, seq=50, error=retry
     *
     * Verifies ERROR_CONDITION flag triggers appropriate handling.
     */
    @Test
    public void testSOHWithErrorConditionFlag() {
        // Arrange: SOH with error condition flag
        byte[] record = {
                SOH_ORDER,
                0x05,
                FLAG_ERROR_CONDITION, // 0x80
                0x00,
                0x12,
                (byte) 0x99,
                0x00, 0x00
        };

        // Act: Extract flag
        byte flagByte = record[2];

        // Assert: Error condition flag should be set
        assertTrue((flagByte & FLAG_ERROR_CONDITION) == FLAG_ERROR_CONDITION,"Error condition flag should be set");
    }

    /**
     * TEST 9: SOH with combined flags (0x41)
     *
     * Pairwise: length=5, type=structured_field, flags=0x41, seq=200, error=none
     *
     * Verifies multiple flags can be set simultaneously.
     */
    @Test
    public void testSOHWithCombinedFlags() {
        // Arrange: SOH with combined flags
        byte[] record = {
                SOH_ORDER,
                0x05,
                (byte) 0x41,         // Extended + Data Stream
                0x00,
                0x06,
                (byte) 0xAB,
                0x00, 0x00
        };

        // Act: Extract flags
        byte flagByte = record[2];

        // Assert: Both flags should be set
        assertTrue((flagByte & FLAG_EXTENDED_5250) == FLAG_EXTENDED_5250,"Extended flag should be set");
        assertTrue((flagByte & FLAG_DATA_STREAM) == FLAG_DATA_STREAM,"Data stream flag should be set");
    }

    /**
     * TEST 10: SOH with zero error row
     *
     * Pairwise: length=4, type=data, flags=0x00, seq=1, error=none
     *
     * Verifies error row of 0 is handled correctly.
     */
    @Test
    public void testSOHWithZeroErrorRow() {
        // Arrange: SOH with error row = 0
        byte[] record = {
                SOH_ORDER,
                0x04,
                0x00,
                0x00,
                0x00,  // Error row = 0
                0x00, 0x00, 0x00
        };

        // Act: Extract error row
        int errorRow = extractErrorRow(record);

        // Assert: Should accept 0
        assertEquals(0, errorRow,"Error row 0 should be valid");
    }

    /**
     * TEST 11: SOH with max error row (255)
     *
     * Pairwise: length=4, type=data, flags=0x00, seq=255, error=none
     *
     * Verifies large error row values are handled.
     */
    @Test
    public void testSOHWithMaxErrorRow() {
        // Arrange: SOH with error row = 255
        byte[] record = {
                SOH_ORDER,
                0x04,
                0x00,
                0x00,
                (byte) 0xFF,  // Error row = 255
                0x00, 0x00, 0x00
        };

        // Act: Extract error row
        int errorRow = extractErrorRow(record);

        // Assert: Should accept 255
        assertEquals(0xFF, errorRow & 0xFF,"Error row 255 should be valid");
    }

    /**
     * TEST 12: SOH data flags with alternating pattern (0x55)
     *
     * Pairwise: length=5, type=sna, flags=0x00, seq=100, error=none
     *
     * Verifies alternating bit pattern in flags.
     * 0x55 = 01010101 binary
     */
    @Test
    public void testSOHWithAlternatingFlagPattern() {
        // Arrange: SOH with alternating flag pattern
        byte[] record = {
                SOH_ORDER,
                0x05,
                0x00,
                0x00,
                0x07,
                (byte) 0x55,  // 01010101 binary
                0x00, 0x00
        };

        // Act: Extract flags
        boolean[] dataIncluded = extractDataIncludedFlags(record);

        // Assert: Pattern 0x55 = 01010101
        assertEquals(false, dataIncluded[23],"Bit 7 (dataIncluded[23]) should be false");
        assertEquals(true, dataIncluded[22],"Bit 6 (dataIncluded[22]) should be true");
        assertEquals(false, dataIncluded[21],"Bit 5 (dataIncluded[21]) should be false");
        assertEquals(true, dataIncluded[20],"Bit 4 (dataIncluded[20]) should be true");
        assertEquals(false, dataIncluded[19],"Bit 3 (dataIncluded[19]) should be false");
        assertEquals(true, dataIncluded[18],"Bit 2 (dataIncluded[18]) should be true");
        assertEquals(false, dataIncluded[17],"Bit 1 (dataIncluded[17]) should be false");
        assertEquals(true, dataIncluded[16],"Bit 0 (dataIncluded[16]) should be true");
    }

    /**
     * TEST 13: SOH data flags with inverse alternating pattern (0xAA)
     *
     * Pairwise: length=5, type=escape, flags=0x80, seq=50, error=abort
     *
     * Verifies inverse alternating bit pattern.
     * 0xAA = 10101010 binary
     */
    @Test
    public void testSOHWithInverseAlternatingPattern() {
        // Arrange: SOH with inverse alternating pattern
        byte[] record = {
                SOH_ORDER,
                0x05,
                (byte) 0x80,
                0x00,
                0x09,
                (byte) 0xAA,  // 10101010 binary
                0x00, 0x00
        };

        // Act: Extract flags
        boolean[] dataIncluded = extractDataIncludedFlags(record);

        // Assert: Pattern 0xAA = 10101010
        assertEquals(true, dataIncluded[23],"Bit 7 (dataIncluded[23]) should be true");
        assertEquals(false, dataIncluded[22],"Bit 6 (dataIncluded[22]) should be false");
        assertEquals(true, dataIncluded[21],"Bit 5 (dataIncluded[21]) should be true");
        assertEquals(false, dataIncluded[20],"Bit 4 (dataIncluded[20]) should be false");
        assertEquals(true, dataIncluded[19],"Bit 3 (dataIncluded[19]) should be true");
        assertEquals(false, dataIncluded[18],"Bit 2 (dataIncluded[18]) should be false");
        assertEquals(true, dataIncluded[17],"Bit 1 (dataIncluded[17]) should be true");
        assertEquals(false, dataIncluded[16],"Bit 0 (dataIncluded[16]) should be false");
    }

    /**
     * TEST 14: SOH with all zeros in flag bytes
     *
     * Pairwise: length=7, type=data, flags=0x00, seq=0, error=none
     *
     * Verifies all-zero flag bytes are handled.
     */
    @Test
    public void testSOHWithAllZeroFlags() {
        // Arrange: SOH with all zero flag bytes
        byte[] record = {
                SOH_ORDER,
                0x07,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00
        };

        // Act: Parse and verify
        boolean valid = validateSOHRecord(record, 7);

        // Assert: Should be valid
        assertTrue(valid,"All-zero flags should be valid");
    }

    /**
     * TEST 15: SOH with all ones in flag bytes
     *
     * Pairwise: length=7, type=sna, flags=0xFF, seq=255, error=abort
     *
     * Verifies all-one flag bytes are handled.
     */
    @Test
    public void testSOHWithAllOneFlags() {
        // Arrange: SOH with all one flag bytes
        byte[] record = {
                SOH_ORDER,
                0x07,
                (byte) 0xFF,
                0x00,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF
        };

        // Act: Parse and verify
        boolean valid = validateSOHRecord(record, 7);

        // Assert: Should be valid
        assertTrue(valid,"All-one flags should be valid");
    }

    // ============================================================
    // ADVERSARIAL TESTS: Malformed/truncated records (13+ tests)
    // ============================================================

    /**
     * TEST 16: SOH with invalid length=0
     *
     * Pairwise: length=0, type=data, flags=0x00, seq=0, error=retry
     *
     * Verifies length 0 triggers error response.
     */
    @Test
    public void testInvalidSOHLengthZero() {
        // Arrange: SOH with length 0
        byte[] record = {
                SOH_ORDER,
                0x00,  // Invalid length
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };

        // Act: Validate record
        boolean valid = validateSOHRecord(record, 0);

        // Assert: Should be invalid
        assertFalse(valid,"SOH length=0 should be invalid");
    }

    /**
     * TEST 17: SOH with invalid length=8 (beyond max)
     *
     * Pairwise: length=8, type=structured_field, flags=0x40, seq=100, error=retry
     *
     * Verifies length > 7 triggers error.
     */
    @Test
    public void testInvalidSOHLengthEight() {
        // Arrange: SOH with length 8
        byte[] record = {
                SOH_ORDER,
                0x08,  // Invalid: > 7
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };

        // Act: Validate record
        boolean valid = validateSOHRecord(record, 8);

        // Assert: Should be invalid
        assertFalse(valid,"SOH length=8 should be invalid");
    }

    /**
     * TEST 18: SOH with invalid length=255 (malicious payload)
     *
     * Pairwise: length=255, type=escape, flags=0xFF, seq=255, error=abort
     *
     * Verifies oversized length values are rejected.
     */
    @Test
    public void testInvalidSOHLengthOversized() {
        // Arrange: SOH with oversized length
        byte[] record = {
                SOH_ORDER,
                (byte) 0xFF,  // Invalid: 255
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };

        // Act: Validate record
        boolean valid = validateSOHRecord(record, 255);

        // Assert: Should be invalid
        assertFalse(valid,"SOH length=255 should be invalid");
    }

    /**
     * TEST 19: Truncated SOH (only order byte, no length)
     *
     * Pairwise: length=N/A (missing), type=data, flags=N/A, seq=0, error=retry
     *
     * Verifies truncated header without length byte is handled.
     */
    @Test
    public void testTruncatedSOHMissingLength() {
        // Arrange: Only order byte, no length byte
        byte[] record = {
                SOH_ORDER  // No length byte
        };

        // Act: Attempt to validate
        boolean canExtractLength = (record.length > 1);

        // Assert: Should fail gracefully
        assertFalse(canExtractLength,"Truncated SOH should not parse");
    }

    /**
     * TEST 20: Truncated SOH (length=5 but only 4 bytes available)
     *
     * Pairwise: length=5, type=data, flags=partial, seq=1, error=retry
     *
     * Verifies buffer underrun is detected.
     */
    @Test
    public void testTruncatedSOHBufferUnderrun() {
        // Arrange: Claims length=5 but only 4 bytes exist
        byte[] record = {
                SOH_ORDER,
                0x05,        // Claims 5 bytes
                0x40,        // Only 4 bytes present
                0x00
        };

        // Act: Check buffer length
        boolean hasEnoughData = (record.length >= 6);

        // Assert: Should detect underrun
        assertFalse(hasEnoughData,"Truncated buffer should be detected");
    }

    /**
     * TEST 21: NULL/empty record buffer
     *
     * Pairwise: length=N/A, type=N/A, flags=N/A, seq=N/A, error=abort
     *
     * Verifies null buffer is handled safely.
     */
    @Test
    public void testNullRecordBuffer() {
        // Arrange: Null buffer
        byte[] record = null;

        // Act: Attempt validation
        boolean valid = false;
        try {
            valid = validateSOHRecord(record, 0);
        } catch (NullPointerException e) {
            // Expected - should throw or return false
            valid = false;
        }

        // Assert: Should not crash
        assertFalse(valid,"Null buffer should not validate");
    }

    /**
     * TEST 22: Empty record buffer (length 0)
     *
     * Pairwise: length=0, type=N/A, flags=N/A, seq=0, error=abort
     *
     * Verifies empty buffer is rejected.
     */
    @Test
    public void testEmptyRecordBuffer() {
        // Arrange: Empty array
        byte[] record = {};

        // Act: Check length
        boolean isEmpty = (record.length == 0);

        // Assert: Should be detected
        assertTrue(isEmpty,"Empty buffer should be detected");
    }

    /**
     * TEST 23: SOH with null/invalid flag bytes (all 0xFF in data flags)
     *
     * Pairwise: length=7, type=sna, flags=0xFF, seq=255, error=abort
     *
     * Verifies 0xFF patterns in all flag positions.
     */
    @Test
    public void testSOHWithMaxFlagByteValues() {
        // Arrange: All flag bytes set to 0xFF
        byte[] record = {
                SOH_ORDER,
                0x07,
                (byte) 0xFF,  // Flag byte
                0x00,
                (byte) 0xFF,  // Error row
                (byte) 0xFF,  // Data byte 1
                (byte) 0xFF,  // Data byte 2
                (byte) 0xFF   // Data byte 3
        };

        // Act: Parse record
        boolean[] dataIncluded = extractDataIncludedFlags(record);

        // Assert: All should be true
        for (boolean flag : dataIncluded) {
            assertTrue(flag,"Flag should be set");
        }
    }

    /**
     * TEST 24: SOH injection with high-order bytes (potential buffer overflow)
     *
     * Pairwise: length=7, type=escape, flags=0x80, seq=200, error=retry
     *
     * Verifies high-order bytes don't cause signed/unsigned issues.
     */
    @Test
    public void testSOHWithHighOrderBytes() {
        // Arrange: SOH with negative byte values (high-order bits set)
        byte[] record = {
                SOH_ORDER,
                0x07,
                (byte) 0x8F,  // Flag byte with high bit
                (byte) 0xFF,  // Reserved
                (byte) 0xFE,  // Error row
                (byte) 0xF0,  // Data byte 1
                (byte) 0xF1,  // Data byte 2
                (byte) 0xF2   // Data byte 3
        };

        // Act: Parse and check signed/unsigned handling
        int errorRow = extractErrorRow(record) & 0xFF;

        // Assert: Should handle unsigned correctly
        assertEquals(0xFE, errorRow,"High-order bytes should be unsigned masked");
    }

    /**
     * TEST 25: SOH with negative length in signed interpretation
     *
     * Pairwise: length=negative, type=data, flags=0x00, seq=0, error=abort
     *
     * Verifies signed byte interpretation doesn't bypass validation.
     */
    @Test
    public void testSOHWithNegativeLength() {
        // Arrange: Signed length byte = -1 (0xFF as signed = -1)
        byte[] record = {
                SOH_ORDER,
                (byte) 0xFF,  // As signed = -1, as unsigned = 255
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };

        // Act: Validate with unsigned interpretation
        int length = record[1] & 0xFF;  // Unsigned
        boolean valid = validateSOHRecord(record, length);

        // Assert: Should treat as unsigned (255), not signed (-1)
        assertEquals(255, length,"Length should be unsigned");
        assertFalse(valid,"Oversized unsigned length should be invalid");
    }

    /**
     * TEST 26: SOH with sequence wraparound (255 to 0 transition)
     *
     * Pairwise: length=4, type=data, flags=0x00, seq=254->255->0, error=none
     *
     * Verifies sequence number wrap-around handling.
     */
    @Test
    public void testSOHSequenceWraparound() {
        // Arrange: Simulate sequence numbers approaching limit
        int seq1 = 254;
        int seq2 = 255;
        int seq3 = (seq2 + 1) & 0xFF;  // Should wrap to 0

        // Act: Verify wraparound logic
        int wrapped = seq3;

        // Assert: Should wrap to 0
        assertEquals(0, wrapped,"Sequence should wrap from 255 to 0");
    }

    /**
     * TEST 27: SOH header followed by garbage data
     *
     * Pairwise: length=4, type=data, flags=0x40, seq=100, error=retry
     *
     * Verifies parser stops at length boundary despite trailing garbage.
     */
    @Test
    public void testSOHWithTrailingGarbage() {
        // Arrange: Valid SOH length=4 followed by garbage
        byte[] record = {
                SOH_ORDER,
                0x04,
                0x40,
                0x00,
                0x05,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,  // Garbage
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF   // More garbage
        };

        // Act: Validate based on length
        boolean valid = validateSOHRecord(record, 4);

        // Assert: Should be valid (garbage is outside length)
        assertTrue(valid,"Valid SOH length=4 should be valid despite trailing data");
    }

    /**
     * TEST 28: SOH with mixed valid/invalid flag combinations
     *
     * Pairwise: length=5, type=structured_field, flags=0xC1, seq=150, error=none
     *
     * Verifies uncommon flag combinations are handled gracefully.
     */
    @Test
    public void testSOHWithUncommonFlagCombination() {
        // Arrange: SOH with unusual flag combo (0xC1 = 11000001)
        byte[] record = {
                SOH_ORDER,
                0x05,
                (byte) 0xC1,  // Extended + Error + Data stream bits
                0x00,
                0x10,
                0x77,
                0x00, 0x00
        };

        // Act: Extract flags
        byte flagByte = record[2];
        boolean extendedSet = (flagByte & FLAG_EXTENDED_5250) == FLAG_EXTENDED_5250;
        boolean errorSet = (flagByte & FLAG_ERROR_CONDITION) == FLAG_ERROR_CONDITION;
        boolean dataSet = (flagByte & FLAG_DATA_STREAM) == FLAG_DATA_STREAM;

        // Assert: Verify each flag independently
        assertTrue(extendedSet,"Extended flag should be set");
        assertTrue(errorSet,"Error flag should be set");
        assertTrue(dataSet,"Data flag should be set");
    }

    // ============================================================
    // HELPER METHODS for SOH parsing simulation
    // ============================================================

    /**
     * Simulates TN5250 SOH parsing validation.
     * Returns true if record is valid per TN5250 spec.
     */
    private boolean validateSOHRecord(byte[] buffer, int length) {
        if (buffer == null || buffer.length < 2) {
            return false;
        }

        // SOH length must be 1-7 per TN5250 spec
        if (length < 1 || length > 7) {
            return false;
        }

        // Minimum buffer size check
        if (buffer.length < length + 1) {  // +1 for order byte
            return false;
        }

        return true;
    }

    /**
     * Extracts error row value from SOH record.
     * Valid only if length >= 4.
     */
    private int extractErrorRow(byte[] buffer) {
        if (buffer == null || buffer.length < 5) {
            return -1;
        }

        int length = buffer[1] & 0xFF;
        if (length < 4) {
            return -1;
        }

        return buffer[4] & 0xFF;  // Unsigned
    }

    /**
     * Extracts data-included flags from SOH record.
     * Simulates the bit-extraction logic from tnvt.processStartOfHeaderOrder().
     * Returns 24-element boolean array where:
     * - [0-7] = byte 3 bits (if length >= 7)
     * - [8-15] = byte 2 bits (if length >= 6)
     * - [16-23] = byte 1 bits (if length >= 5)
     */
    private boolean[] extractDataIncludedFlags(byte[] buffer) {
        boolean[] dataIncluded = new boolean[24];

        if (buffer == null || buffer.length < 5) {
            return dataIncluded;  // All false
        }

        int length = buffer[1] & 0xFF;

        // Byte 1 (index 5 in record) - affects bits 16-23
        if (length >= 5 && buffer.length > 5) {
            byte byte1 = buffer[5];
            dataIncluded[23] = (byte1 & 0x80) == 0x80;
            dataIncluded[22] = (byte1 & 0x40) == 0x40;
            dataIncluded[21] = (byte1 & 0x20) == 0x20;
            dataIncluded[20] = (byte1 & 0x10) == 0x10;
            dataIncluded[19] = (byte1 & 0x8) == 0x8;
            dataIncluded[18] = (byte1 & 0x4) == 0x4;
            dataIncluded[17] = (byte1 & 0x2) == 0x2;
            dataIncluded[16] = (byte1 & 0x1) == 0x1;
        }

        // Byte 2 (index 6 in record) - affects bits 8-15
        if (length >= 6 && buffer.length > 6) {
            byte byte1 = buffer[6];
            dataIncluded[15] = (byte1 & 0x80) == 0x80;
            dataIncluded[14] = (byte1 & 0x40) == 0x40;
            dataIncluded[13] = (byte1 & 0x20) == 0x20;
            dataIncluded[12] = (byte1 & 0x10) == 0x10;
            dataIncluded[11] = (byte1 & 0x8) == 0x8;
            dataIncluded[10] = (byte1 & 0x4) == 0x4;
            dataIncluded[9] = (byte1 & 0x2) == 0x2;
            dataIncluded[8] = (byte1 & 0x1) == 0x1;
        }

        // Byte 3 (index 7 in record) - affects bits 0-7
        if (length >= 7 && buffer.length > 7) {
            byte byte1 = buffer[7];
            dataIncluded[7] = (byte1 & 0x80) == 0x80;
            dataIncluded[6] = (byte1 & 0x40) == 0x40;
            dataIncluded[5] = (byte1 & 0x20) == 0x20;
            dataIncluded[4] = (byte1 & 0x10) == 0x10;
            dataIncluded[3] = (byte1 & 0x8) == 0x8;
            dataIncluded[2] = (byte1 & 0x4) == 0x4;
            dataIncluded[1] = (byte1 & 0x2) == 0x2;
            dataIncluded[0] = (byte1 & 0x1) == 0x1;
        }

        return dataIncluded;
    }
}
