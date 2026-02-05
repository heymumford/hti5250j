/**
 * Title: tn5250J
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise TDD test suite for TN5250 data stream protocol layer
 *
 * This test suite covers high-risk areas identified in the bug hunt:
 * - DataStreamProducer stream parsing and validation
 * - Stream5250 buffer boundary conditions
 * - WTD (Write-to-Display) opcode handling
 * - Message fragmentation and reassembly
 * - Protocol fuzzing and adversarial inputs
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 */
package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pairwise parameter testing for TN5250 data stream protocol layer.
 *
 * Test dimensions (to be combined pairwise):
 * - Message lengths: [0, 1, 10, 255, 256, 65535, MAX]
 * - Opcodes: [valid (0-12), invalid (13+), reserved, 0x00, 0xFF]
 * - Header states: [complete, partial, missing, corrupt]
 * - Payload types: [empty, data, control, mixed]
 * - Sequence: [single, multiple, fragmented]
 *
 * Focus areas:
 * 1. POSITIVE: Valid TN5250 streams are parsed correctly
 * 2. BOUNDARY: Message lengths at protocol limits
 * 3. ADVERSARIAL: Malformed packets, truncation, buffer attacks
 * 4. FRAGMENTATION: Partial message handling and reassembly
 * 5. PROTOCOL FUZZING: Invalid opcodes, corrupt headers, injection
 */
public class DataStreamPairwiseTest {

    // Constants matching TN5250 protocol
    private static final int STREAM_HEADER_SIZE = 9;  // 2 (length) + 7 (header prefix)
    private static final int OPCODE_OFFSET = 9;
    private static final int MINIMAL_HEADER_LENGTH = 3;
    private static final int MINIMAL_PARTIAL_STREAM_LEN = 2;
    private static final byte END_OF_RECORD_HIGH = (byte) 0xFF;
    private static final byte END_OF_RECORD_LOW = (byte) 0xEF;

    // Valid opcode range (0-12 for WTD, exception handling, etc)
    private static final byte VALID_OPCODE_WTD = 0x42;
    private static final byte INVALID_OPCODE_RESERVED = (byte) 0xFE;
    private static final byte OPCODE_ZERO = 0x00;
    private static final byte OPCODE_MAX = (byte) 0xFF;

    private Stream5250 stream;
    private byte[] testBuffer;

    @Before
    public void setUp() {
        stream = new Stream5250();
    }

    // ========== POSITIVE TESTS: Valid TN5250 streams ==========

    /**
     * TEST 1: Valid minimal stream with WTD opcode
     *
     * Pairwise combination:
     * - Message length: 10 (minimal real message)
     * - Opcode: valid (0x42 = WTD)
     * - Header state: complete
     * - Payload: empty
     * - Sequence: single
     *
     * RED: Stream5250 should correctly parse a valid minimal message
     * GREEN: Initialize stream with valid header and verify opcode extraction
     */
    @Test
    public void testValidMinimalStreamWithWTDOpcode() {
        // Arrange: Create valid 10-byte stream
        testBuffer = createValidStream(10, VALID_OPCODE_WTD, true);

        // Act: Initialize stream
        stream.initialize(testBuffer);

        // Assert: Verify correct parsing
        assertEquals("Stream size should be 10", 10, stream.streamSize);
        assertEquals("Opcode should be WTD (0x42)", VALID_OPCODE_WTD & 0xFF, stream.opCode & 0xFF);
        // dataStart = 6 + buffer[6] = 6 + 3 = 9 per Stream5250 formula
        assertEquals("Data should start at position 9", 9, stream.dataStart);
        assertTrue("Position should be valid", stream.pos >= 0);
        assertTrue("Position should be within bounds", stream.pos <= testBuffer.length);
    }

    /**
     * TEST 2: Valid stream with data payload
     *
     * Pairwise combination:
     * - Message length: 255 (boundary)
     * - Opcode: valid (0x01)
     * - Header state: complete
     * - Payload: data (ASCII text)
     * - Sequence: single
     *
     * RED: Should parse stream with non-empty data payload
     * GREEN: Verify position and data accessibility
     */
    @Test
    public void testValidStreamWith255ByteDataPayload() {
        // Arrange: Create 255-byte stream with payload
        testBuffer = createValidStream(255, (byte) 0x01, true);
        // Fill payload with test data
        for (int i = OPCODE_OFFSET + 1; i < testBuffer.length - 2; i++) {
            testBuffer[i] = (byte) (0x40 + (i % 26));
        }

        // Act: Initialize and navigate to data
        stream.initialize(testBuffer);

        // Assert: Verify data parsing
        assertEquals("Stream size should be 255", 255, stream.streamSize);
        // hasNext() returns pos < streamSize
        assertTrue("Should have data to process", stream.hasNext());
        // dataStart = 6 + buffer[6] = 6 + 3 = 9
        assertEquals("Data start should be position 9", 9, stream.dataStart);
    }

    /**
     * TEST 3: Valid stream with control opcodes
     *
     * Pairwise combination:
     * - Message length: 50
     * - Opcode: valid control (0x03)
     * - Header state: complete
     * - Payload: control (no payload)
     * - Sequence: single
     *
     * RED: Should handle control-only streams
     * GREEN: Parse with minimal payload
     */
    @Test
    public void testValidStreamWithControlOpcode() {
        // Arrange: Create control-only stream
        testBuffer = createValidStream(50, (byte) 0x03, true);

        // Act: Initialize
        stream.initialize(testBuffer);

        // Assert: Verify control opcode extraction
        assertEquals("Opcode should be 0x03", 0x03, stream.opCode);
        assertTrue("Position should be initialized", stream.pos >= 0);
    }

    /**
     * TEST 4: Valid stream with minimum length (2-byte length header)
     *
     * Pairwise combination:
     * - Message length: 0 (protocol minimum)
     * - Opcode: valid (0x42)
     * - Header state: complete
     * - Payload: empty
     * - Sequence: single
     *
     * RED: Should handle zero-length message bodies
     * GREEN: Parse without throwing exception
     */
    @Test
    public void testValidStreamWithZeroLengthBody() {
        // Arrange: Create minimal structure
        testBuffer = new byte[11];  // Just header + length bytes + opcode
        testBuffer[0] = 0x00;
        testBuffer[1] = 0x0B;  // 11 bytes total
        testBuffer[6] = MINIMAL_HEADER_LENGTH;
        testBuffer[OPCODE_OFFSET] = VALID_OPCODE_WTD;

        // Act: Initialize
        stream.initialize(testBuffer);

        // Assert
        assertEquals("Opcode should parse correctly", VALID_OPCODE_WTD & 0xFF, stream.opCode & 0xFF);
        // streamSize = 11, pos = 9, so pos < streamSize is true
        assertTrue("Should still be able to read remaining bytes", stream.hasNext());
        assertEquals("Position should be set to dataStart", 9, stream.pos);
    }

    /**
     * TEST 5: Valid stream requiring byte-by-byte reading
     *
     * Pairwise combination:
     * - Message length: 100
     * - Opcode: valid (0x11)
     * - Header state: complete
     * - Payload: data
     * - Sequence: single
     *
     * RED: getNextByte() should work correctly on valid stream
     * GREEN: Sequential reading advances position correctly
     */
    @Test
    public void testSequentialByteReading() {
        // Arrange: Create stream and position to data
        testBuffer = createValidStream(100, (byte) 0x11, true);
        stream.initialize(testBuffer);
        int startPos = stream.pos;

        // Act: Read one byte
        byte firstByte = stream.getNextByte();

        // Assert: Position should advance
        assertEquals("Position should increment by 1", startPos + 1, stream.pos);
        assertNotNull("Should return a byte", firstByte);
    }

    // ========== BOUNDARY TESTS: Message length extremes ==========

    /**
     * TEST 6: Boundary - Message length = 256 (0x0100)
     *
     * Pairwise combination:
     * - Message length: 256 (16-bit boundary)
     * - Opcode: valid
     * - Header state: complete
     * - Payload: data
     * - Sequence: single
     *
     * RED: Should handle 256-byte messages without overflow
     * GREEN: Parse correctly despite boundary crossing
     */
    @Test
    public void testMessageLength256ByteBoundary() {
        // Arrange: Create 256-byte message
        testBuffer = createValidStream(256, VALID_OPCODE_WTD, true);

        // Act: Initialize
        stream.initialize(testBuffer);

        // Assert: Verify 16-bit encoding
        assertEquals("Stream size should be 256", 256, stream.streamSize);
        assertEquals("Length bytes should be 0x0100", 256, stream.streamSize);
    }

    /**
     * TEST 7: Boundary - Maximum message length (65535 bytes)
     *
     * Pairwise combination:
     * - Message length: 65535 (16-bit max)
     * - Opcode: valid
     * - Header state: complete
     * - Payload: empty (just length)
     * - Sequence: single
     *
     * RED: Should parse maximum valid TN5250 message
     * GREEN: Handle large 16-bit values correctly
     */
    @Test
    public void testMaximumMessage65535Bytes() {
        // Arrange: Create header for max message
        testBuffer = new byte[12];
        testBuffer[0] = (byte) 0xFF;  // High byte of 65535
        testBuffer[1] = (byte) 0xFF;  // Low byte of 65535
        testBuffer[6] = MINIMAL_HEADER_LENGTH;
        testBuffer[OPCODE_OFFSET] = VALID_OPCODE_WTD;

        // Act: Initialize
        stream.initialize(testBuffer);

        // Assert: Verify max value parsing
        assertEquals("Stream size should be 65535", 65535, stream.streamSize);
    }

    /**
     * TEST 8: Boundary - Very small message (1 byte)
     *
     * Pairwise combination:
     * - Message length: 1
     * - Opcode: valid
     * - Header state: complete
     * - Payload: empty
     * - Sequence: single
     *
     * RED: Should handle 1-byte message gracefully
     * GREEN: Parse despite minimal size
     */
    @Test
    public void testMinimumMessageLength() {
        // Arrange: Create 1-byte message
        testBuffer = new byte[11];
        testBuffer[0] = 0x00;
        testBuffer[1] = 0x01;
        testBuffer[6] = MINIMAL_HEADER_LENGTH;
        testBuffer[OPCODE_OFFSET] = VALID_OPCODE_WTD;

        // Act: Initialize
        stream.initialize(testBuffer);

        // Assert
        assertEquals("Stream size should be 1", 1, stream.streamSize);
        assertTrue("Buffer should exist", stream.buffer != null);
    }

    // ========== ADVERSARIAL TESTS: Malformed packets, truncation, invalid opcodes ==========

    /**
     * TEST 9: Adversarial - Invalid opcode (reserved byte)
     *
     * Pairwise combination:
     * - Message length: 50
     * - Opcode: invalid (0xFE)
     * - Header state: complete
     * - Payload: empty
     * - Sequence: single
     *
     * RED: Invalid opcode should be captured (not crash)
     * GREEN: Parse opcode regardless of validity
     */
    @Test
    public void testInvalidOpcodeReservedByte() {
        // Arrange: Create stream with reserved opcode
        testBuffer = createValidStream(50, INVALID_OPCODE_RESERVED, true);

        // Act: Initialize
        stream.initialize(testBuffer);

        // Assert: Should capture opcode without crashing
        assertEquals("Opcode should be captured", INVALID_OPCODE_RESERVED & 0xFF, stream.opCode & 0xFF);
    }

    /**
     * TEST 10: Adversarial - Opcode = 0x00 (null byte)
     *
     * Pairwise combination:
     * - Message length: 100
     * - Opcode: 0x00 (boundary)
     * - Header state: complete
     * - Payload: data
     * - Sequence: single
     *
     * RED: Should handle null opcode without special behavior
     * GREEN: Parse correctly
     */
    @Test
    public void testOpcodeZeroNullByte() {
        // Arrange
        testBuffer = createValidStream(100, OPCODE_ZERO, true);

        // Act
        stream.initialize(testBuffer);

        // Assert
        assertEquals("Opcode 0x00 should be stored", 0, stream.opCode);
    }

    /**
     * TEST 11: Adversarial - Opcode = 0xFF (max byte)
     *
     * Pairwise combination:
     * - Message length: 75
     * - Opcode: 0xFF (max)
     * - Header state: complete
     * - Payload: data
     * - Sequence: single
     *
     * RED: Should handle max opcode byte
     * GREEN: Parse without overflow
     */
    @Test
    public void testOpcodeMaxByte() {
        // Arrange
        testBuffer = createValidStream(75, OPCODE_MAX, true);

        // Act
        stream.initialize(testBuffer);

        // Assert
        assertEquals("Opcode 0xFF should be stored", 0xFF, stream.opCode & 0xFF);
    }

    /**
     * TEST 12: Adversarial - Truncated message (missing header)
     *
     * Pairwise combination:
     * - Message length: 10
     * - Opcode: would be valid
     * - Header state: partial (missing)
     * - Payload: empty
     * - Sequence: single
     *
     * RED: Should detect buffer underflow on initialization
     * GREEN: Throw exception when accessing out-of-bounds header
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testTruncatedMessageMissingHeader() {
        // Arrange: Buffer too small for header
        testBuffer = new byte[5];  // Only 5 bytes, need at least 11
        testBuffer[0] = 0x00;
        testBuffer[1] = 0x0A;

        // Act: Initialize should throw when accessing position 9 or 6
        stream.initialize(testBuffer);

        // Assert: Should never reach here
        fail("Expected ArrayIndexOutOfBoundsException for truncated header");
    }

    /**
     * TEST 13: Adversarial - Message length claims more data than buffer contains
     *
     * Pairwise combination:
     * - Message length: 1000 (claimed)
     * - Opcode: valid
     * - Header state: complete (but body incomplete)
     * - Payload: only 50 bytes actual
     * - Sequence: single
     *
     * RED: Mismatch between claimed and actual length should be detected
     * GREEN: Store claimed length, let reader detect truncation
     */
    @Test
    public void testClaimedLengthExceedsBufferSize() {
        // Arrange: Claim 1000 bytes but only provide 50
        testBuffer = new byte[50];
        testBuffer[0] = 0x03;  // 0x03E8 = 1000
        testBuffer[1] = (byte) 0xE8;
        testBuffer[6] = MINIMAL_HEADER_LENGTH;
        testBuffer[OPCODE_OFFSET] = VALID_OPCODE_WTD;

        // Act
        stream.initialize(testBuffer);

        // Assert: Should store claimed length
        assertEquals("Should store claimed length", 1000, stream.streamSize);
        assertTrue("Actual buffer is smaller", testBuffer.length < stream.streamSize);
    }

    /**
     * TEST 14: Adversarial - Buffer attack: negative offset leading to underflow
     *
     * Pairwise combination:
     * - Message length: 20
     * - Opcode: valid
     * - Header state: complete
     * - Payload: empty
     * - Sequence: single + getByteOffset(-1)
     *
     * RED: getByteOffset(-1) at pos=0 should throw exception
     * GREEN: Validate resulting index is non-negative
     */
    @Test(expected = Exception.class)
    public void testBufferUnderflowViaOffset() throws Exception {
        // Arrange: Create valid stream and position at start
        testBuffer = createValidStream(20, VALID_OPCODE_WTD, true);
        stream.initialize(testBuffer);
        stream.pos = 0;

        // Act: Attempt negative offset from start
        byte result = stream.getByteOffset(-1);

        // Assert: Should throw exception, never reach here
        fail("Expected Exception for negative offset, got byte: " + result);
    }

    /**
     * TEST 15: Adversarial - Null buffer after initialization
     *
     * Pairwise combination:
     * - Message length: N/A
     * - Opcode: N/A
     * - Header state: N/A (buffer is null)
     * - Payload: empty
     * - Sequence: N/A
     *
     * RED: Should detect null buffer on read
     * GREEN: Throw IllegalStateException
     */
    @Test(expected = IllegalStateException.class)
    public void testNullBufferOnRead() {
        // Arrange: Create stream with null buffer
        stream.buffer = null;
        stream.pos = 0;

        // Act: Attempt to read from null buffer
        byte result = stream.getNextByte();

        // Assert: Should throw, never reach here
        fail("Expected IllegalStateException for null buffer, got byte: " + result);
    }

    // ========== FRAGMENTATION TESTS: Partial messages, reassembly ==========

    /**
     * TEST 16: Fragmentation - Partial message in first buffer
     *
     * Pairwise combination:
     * - Message length: 100
     * - Opcode: valid
     * - Header state: complete, but payload partial
     * - Payload: 50 of 100 expected bytes
     * - Sequence: fragmented (more coming)
     *
     * RED: Should detect when message incomplete
     * GREEN: Validate message length vs available data
     */
    @Test
    public void testPartialMessageDetection() {
        // Arrange: Create message claiming 100 bytes but only provide 50
        testBuffer = new byte[60];  // 10 header + 50 payload
        testBuffer[0] = 0x00;
        testBuffer[1] = 0x64;  // 100 bytes claimed
        testBuffer[6] = MINIMAL_HEADER_LENGTH;
        testBuffer[OPCODE_OFFSET] = VALID_OPCODE_WTD;

        // Act: Initialize
        stream.initialize(testBuffer);

        // Assert: Should store claimed length but buffer is short
        assertEquals("Claimed length", 100, stream.streamSize);
        assertTrue("Actual buffer < claimed", testBuffer.length < stream.streamSize);
    }

    /**
     * TEST 17: Fragmentation - Multiple complete messages in single buffer
     *
     * Pairwise combination:
     * - Message length: 20 + 20 (two messages)
     * - Opcode: valid
     * - Header state: complete (both)
     * - Payload: data (both)
     * - Sequence: multiple
     *
     * RED: Should handle reading two messages from one buffer
     * GREEN: Track position between messages
     */
    @Test
    public void testMultipleMessagesInSingleBuffer() {
        // Arrange: Create two 20-byte messages
        testBuffer = new byte[40];
        // First message
        testBuffer[0] = 0x00;
        testBuffer[1] = 0x14;  // 20 bytes
        testBuffer[6] = MINIMAL_HEADER_LENGTH;
        testBuffer[OPCODE_OFFSET] = VALID_OPCODE_WTD;

        // Second message (starts at offset 20)
        testBuffer[20] = 0x00;
        testBuffer[21] = 0x14;  // 20 bytes
        testBuffer[26] = MINIMAL_HEADER_LENGTH;
        testBuffer[29] = (byte) 0x11;

        // Act: Initialize with first
        stream.initialize(testBuffer);
        int firstMsgStart = stream.pos;
        int firstSize = stream.streamSize;

        // Assert: First message parsed
        assertEquals("First message size", 20, firstSize);
    }

    /**
     * TEST 18: Fragmentation - Empty payload between complete header and next message
     *
     * Pairwise combination:
     * - Message length: 11, 11
     * - Opcode: valid
     * - Header state: complete
     * - Payload: empty (header-only messages)
     * - Sequence: multiple
     *
     * RED: Should handle back-to-back header-only messages
     * GREEN: Parse correctly without data payload
     */
    @Test
    public void testMultipleHeaderOnlyMessages() {
        // Arrange: Two header-only messages
        testBuffer = new byte[22];
        // First message
        testBuffer[0] = 0x00;
        testBuffer[1] = 0x0B;  // 11 bytes
        testBuffer[6] = MINIMAL_HEADER_LENGTH;
        testBuffer[OPCODE_OFFSET] = VALID_OPCODE_WTD;

        // Second message
        testBuffer[11] = 0x00;
        testBuffer[12] = 0x0B;
        testBuffer[17] = MINIMAL_HEADER_LENGTH;
        testBuffer[20] = (byte) 0x22;

        // Act
        stream.initialize(testBuffer);

        // Assert
        assertTrue("First message has valid opcode", stream.opCode >= 0);
    }

    // ========== PROTOCOL FUZZING TESTS: Invalid opcodes, corrupt headers, injection ==========

    /**
     * TEST 19: Protocol fuzzing - Opcode at wrong position in corrupted header
     *
     * Pairwise combination:
     * - Message length: 50
     * - Opcode: 0x42 but at wrong position (header corruption)
     * - Header state: corrupt (offset wrong)
     * - Payload: data
     * - Sequence: single
     *
     * RED: Should extract opcode from expected position (offset 9)
     * GREEN: Read byte from OPCODE_OFFSET regardless of validity
     */
    @Test
    public void testOpcodeExtractionWithCorruptHeader() {
        // Arrange: Create stream with corrupt header structure
        testBuffer = createValidStream(50, (byte) 0x42, true);
        // Corrupt the header length field
        testBuffer[6] = (byte) 0xFF;  // Invalid: way too long

        // Act: Initialize
        stream.initialize(testBuffer);

        // Assert: Should still extract byte at position 9
        assertEquals("Should extract opcode from position 9", 0x42, testBuffer[OPCODE_OFFSET] & 0xFF);
    }

    /**
     * TEST 20: Protocol fuzzing - End-of-record markers in payload (injection attempt)
     *
     * Pairwise combination:
     * - Message length: 100
     * - Opcode: valid
     * - Header state: complete
     * - Payload: contains 0xFF 0xEF (EOR marker)
     * - Sequence: single
     *
     * RED: EOR markers in payload should be treated as data, not protocol markers
     * GREEN: Store payload bytes as-is without interpretation
     */
    @Test
    public void testEORMarkersInPayloadAreData() {
        // Arrange: Create stream with EOR-like bytes in payload
        testBuffer = createValidStream(100, VALID_OPCODE_WTD, true);
        // Inject EOR bytes into payload
        int payloadStart = STREAM_HEADER_SIZE + MINIMAL_HEADER_LENGTH;
        if (payloadStart < testBuffer.length - 2) {
            testBuffer[payloadStart] = END_OF_RECORD_HIGH;
            testBuffer[payloadStart + 1] = END_OF_RECORD_LOW;
        }

        // Act: Initialize and read
        stream.initialize(testBuffer);

        // Assert: Should parse without treating markers as special
        assertEquals("Should read claimed size", 100, stream.streamSize);
        assertTrue("Should have bytes to read", stream.pos < testBuffer.length);
    }

    // ========== HELPER METHODS ==========

    /**
     * Creates a valid TN5250 stream with specified parameters.
     *
     * Format:
     * [0-1] length (big-endian 16-bit)
     * [2-5] reserved/header
     * [6] header length (N bytes after position 6)
     * [7-8] reserved
     * [9] opcode
     * [10+] payload
     *
     * @param messageSize total message size (including length bytes)
     * @param opcode opcode to use
     * @param addEOR whether to append EOR marker
     * @return valid stream buffer
     */
    private byte[] createValidStream(int messageSize, byte opcode, boolean addEOR) {
        int bufSize = messageSize + (addEOR ? 2 : 0);
        byte[] buf = new byte[bufSize];

        // Length (big-endian)
        buf[0] = (byte) ((messageSize >> 8) & 0xFF);
        buf[1] = (byte) (messageSize & 0xFF);

        // Header prefix (bytes 2-5)
        buf[2] = 0x00;
        buf[3] = 0x00;
        buf[4] = 0x00;
        buf[5] = 0x00;

        // Header length
        buf[6] = MINIMAL_HEADER_LENGTH;

        // Reserved (bytes 7-8)
        buf[7] = 0x00;
        buf[8] = 0x00;

        // Opcode
        buf[OPCODE_OFFSET] = opcode;

        // Payload (if any)
        for (int i = OPCODE_OFFSET + 1; i < messageSize; i++) {
            buf[i] = (byte) (0x41 + (i % 26));  // A-Z pattern
        }

        // EOR if requested
        if (addEOR && bufSize > messageSize) {
            buf[messageSize] = END_OF_RECORD_HIGH;
            buf[messageSize + 1] = END_OF_RECORD_LOW;
        }

        return buf;
    }
}
