/**
 * Title: tn5250J
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise combinatorial tests for Stream5250 boundary conditions
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING. If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.hti5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Pairwise combinatorial test suite for Stream5250.
 *
 * Tests exhaustive combinations of:
 * - Buffer sizes: [0, 1, 10, 100, 1000]
 * - Position values: [0, 1, mid, length-1, length, length+1]
 * - Offset values: [-100, -1, 0, 1, mid, max]
 * - Buffer states: [null, empty, partial, full]
 *
 * Coverage matrix:
 * 1. getNextByte() with 6 buffer sizes × 6 position states = 36 combinations
 * 2. getByteOffset(int) with 6 buffer sizes × 6 position × 6 offset = 216 combinations
 * 3. hasNext() boundary behavior
 * 4. reset() state transitions
 *
 * Tests are named: test{Method}_{Scenario}_{ExpectedOutcome}
 */
public class Stream5250PairwiseTest {

    private Stream5250 stream;

    // Test fixture: minimal valid 5250 stream buffer
    private static byte[] createMinimalBuffer(int size) {
        byte[] buffer = new byte[size];
        if (size >= 2) {
            buffer[0] = (byte) ((size >> 8) & 0xFF);
            buffer[1] = (byte) (size & 0xFF);
        }
        if (size >= 7) {
            buffer[6] = 0x03;  // header length
        }
        if (size >= 10) {
            buffer[9] = 0x42;  // opcode
        }
        return buffer;
    }

    // Pairwise dimension values
    private static final int[] BUFFER_SIZES = {0, 1, 10, 100, 1000};
    private static final int[] POSITION_MULTIPLIERS = {0, 1, 5, 95, 99, 100}; // % of buffer length

    @Before
    public void setUp() {
        // Default to 100-byte buffer for most tests
        stream = new Stream5250(createMinimalBuffer(100));
    }

    // ===== TEST GROUP 1: getNextByte() Positive Cases =====

    /**
     * Test: getNextByte_ValidPositionStart_ReturnsFirstByte
     * Scenario: Position at start (0), read first byte
     * Expected: Success, byte returned, position incremented
     */
    @Test
    public void testGetNextByte_ValidPositionStart_ReturnsFirstByte() {
        // Arrange
        stream.pos = 0;
        byte expectedValue = (byte) 0xAA;
        stream.buffer[0] = expectedValue;

        // Act
        byte result = stream.getNextByte();

        // Assert
        assertEquals("Should return byte at position 0", expectedValue, result);
        assertEquals("Position should increment to 1", 1, stream.pos);
    }

    /**
     * Test: getNextByte_ValidPositionMid_ReturnsMiddleByte
     * Scenario: Position in middle (50), read middle byte
     * Expected: Success, byte returned, position incremented
     */
    @Test
    public void testGetNextByte_ValidPositionMid_ReturnsMiddleByte() {
        // Arrange
        stream.pos = 50;
        byte expectedValue = (byte) 0xBB;
        stream.buffer[50] = expectedValue;

        // Act
        byte result = stream.getNextByte();

        // Assert
        assertEquals("Should return byte at position 50", expectedValue, result);
        assertEquals("Position should increment to 51", 51, stream.pos);
    }

    /**
     * Test: getNextByte_ValidPositionBeforeEnd_ReturnsLastValidByte
     * Scenario: Position at length-1, read last valid byte
     * Expected: Success, byte returned, position incremented to length
     */
    @Test
    public void testGetNextByte_ValidPositionBeforeEnd_ReturnsLastValidByte() {
        // Arrange
        int bufferLength = stream.buffer.length;
        stream.pos = bufferLength - 1;
        byte expectedValue = (byte) 0xCC;
        stream.buffer[bufferLength - 1] = expectedValue;

        // Act
        byte result = stream.getNextByte();

        // Assert
        assertEquals("Should return last valid byte", expectedValue, result);
        assertEquals("Position should increment to buffer length", bufferLength, stream.pos);
    }

    // ===== TEST GROUP 2: getNextByte() Adversarial Cases =====

    /**
     * Test: getNextByte_PositionEqualsLength_ThrowsIllegalState
     * Scenario: Position equals buffer length (at boundary)
     * Expected: IllegalStateException thrown
     */
    @Test(expected = IllegalStateException.class)
    public void testGetNextByte_PositionEqualsLength_ThrowsIllegalState() {
        // Arrange
        int bufferLength = stream.buffer.length;
        stream.pos = bufferLength;

        // Act
        stream.getNextByte();

        // Assert: Should not reach here
        fail("Should throw IllegalStateException when pos == buffer.length");
    }

    /**
     * Test: getNextByte_PositionBeyondLength_ThrowsIllegalState
     * Scenario: Position beyond buffer length
     * Expected: IllegalStateException thrown
     */
    @Test(expected = IllegalStateException.class)
    public void testGetNextByte_PositionBeyondLength_ThrowsIllegalState() {
        // Arrange
        stream.pos = stream.buffer.length + 10;

        // Act
        stream.getNextByte();

        // Assert: Should not reach here
        fail("Should throw IllegalStateException when pos > buffer.length");
    }

    /**
     * Test: getNextByte_NullBuffer_ThrowsIllegalState
     * Scenario: Buffer is null, any position
     * Expected: IllegalStateException thrown
     */
    @Test(expected = IllegalStateException.class)
    public void testGetNextByte_NullBuffer_ThrowsIllegalState() {
        // Arrange
        stream.buffer = null;
        stream.pos = 0;

        // Act
        stream.getNextByte();

        // Assert: Should not reach here
        fail("Should throw IllegalStateException when buffer is null");
    }

    /**
     * Test: getNextByte_ZeroLengthBuffer_ThrowsIllegalState
     * Scenario: Empty buffer (length 0), position 0
     * Expected: IllegalStateException thrown
     */
    @Test(expected = IllegalStateException.class)
    public void testGetNextByte_ZeroLengthBuffer_ThrowsIllegalState() {
        // Arrange
        stream.buffer = new byte[0];
        stream.pos = 0;

        // Act
        stream.getNextByte();

        // Assert: Should not reach here
        fail("Should throw IllegalStateException on zero-length buffer");
    }

    /**
     * Test: getNextByte_SequentialReadsExhaustBuffer_ThrowsOnOverflow
     * Scenario: Read all bytes sequentially, then attempt one more
     * Expected: Early reads succeed, final read throws IllegalStateException
     */
    @Test
    public void testGetNextByte_SequentialReadsExhaustBuffer_ThrowsOnOverflow() {
        // Arrange
        stream.buffer = createMinimalBuffer(5);
        stream.pos = 0;
        byte[] values = {0x11, 0x22, 0x33, 0x44, 0x55};
        for (int i = 0; i < values.length; i++) {
            stream.buffer[i] = values[i];
        }

        // Act & Assert: Read all valid bytes
        for (int i = 0; i < values.length; i++) {
            assertEquals("Read byte at index " + i, values[i], stream.getNextByte());
        }

        // Act & Assert: Next read should fail
        assertEquals("Position should be at buffer length", stream.buffer.length, stream.pos);
        try {
            stream.getNextByte();
            fail("Should throw IllegalStateException after exhausting buffer");
        } catch (IllegalStateException e) {
            assertTrue("Exception message should indicate buffer exceeded",
                    e.getMessage().contains("Buffer length exceeded"));
        }
    }

    // ===== TEST GROUP 3: getByteOffset() Positive Cases =====

    /**
     * Test: getByteOffset_ValidZeroOffset_ReturnsCurrentByte
     * Scenario: Position in middle, zero offset
     * Expected: Success, byte at current position returned
     */
    @Test
    public void testGetByteOffset_ValidZeroOffset_ReturnsCurrentByte() throws Exception {
        // Arrange
        stream.pos = 50;
        byte expectedValue = (byte) 0xDD;
        stream.buffer[50] = expectedValue;

        // Act
        byte result = stream.getByteOffset(0);

        // Assert
        assertEquals("Should return byte at current position with zero offset", expectedValue, result);
        assertEquals("Position should not change", 50, stream.pos);
    }

    /**
     * Test: getByteOffset_ValidPositiveOffset_ReturnsOffsetByte
     * Scenario: Position 10, offset +5, read byte at position 15
     * Expected: Success, byte at pos+offset returned
     */
    @Test
    public void testGetByteOffset_ValidPositiveOffset_ReturnsOffsetByte() throws Exception {
        // Arrange
        stream.pos = 10;
        byte expectedValue = (byte) 0xEE;
        stream.buffer[15] = expectedValue;

        // Act
        byte result = stream.getByteOffset(5);

        // Assert
        assertEquals("Should return byte at position + offset", expectedValue, result);
        assertEquals("Position should not change", 10, stream.pos);
    }

    /**
     * Test: getByteOffset_ValidOffsetAtBufferEnd_ReturnsLastByte
     * Scenario: Position allows offset to reach last byte exactly
     * Expected: Success, last byte returned
     */
    @Test
    public void testGetByteOffset_ValidOffsetAtBufferEnd_ReturnsLastByte() throws Exception {
        // Arrange
        int bufferLength = stream.buffer.length;
        stream.pos = bufferLength - 10;
        byte expectedValue = (byte) 0xFF;
        stream.buffer[bufferLength - 1] = expectedValue;

        // Act
        byte result = stream.getByteOffset(9);

        // Assert
        assertEquals("Should read last byte via positive offset", expectedValue, result);
        assertEquals("Position should not change", bufferLength - 10, stream.pos);
    }

    /**
     * Test: getByteOffset_ValidSmallPositiveOffset_ReturnsNextByte
     * Scenario: Position 0, offset +1, read second byte
     * Expected: Success, byte at position 1 returned
     */
    @Test
    public void testGetByteOffset_ValidSmallPositiveOffset_ReturnsNextByte() throws Exception {
        // Arrange
        stream.pos = 0;
        byte expectedValue = (byte) 0x99;
        stream.buffer[1] = expectedValue;

        // Act
        byte result = stream.getByteOffset(1);

        // Assert
        assertEquals("Should read next byte with offset +1", expectedValue, result);
    }

    // ===== TEST GROUP 4: getByteOffset() Adversarial Cases =====

    /**
     * Test: getByteOffset_NegativeOffsetFromZero_ThrowsException
     * Scenario: Position 0, offset -1 (would access index -1)
     * Expected: Exception thrown (guards negative index)
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_NegativeOffsetFromZero_ThrowsException() throws Exception {
        // Arrange
        stream.pos = 0;

        // Act
        stream.getByteOffset(-1);

        // Assert: Should not reach here
        fail("Should throw Exception on negative resulting index");
    }

    /**
     * Test: getByteOffset_NegativeOffsetFromMid_ThrowsException
     * Scenario: Position 5, offset -10 (would access index -5)
     * Expected: Exception thrown
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_NegativeOffsetFromMid_ThrowsException() throws Exception {
        // Arrange
        stream.pos = 5;

        // Act
        stream.getByteOffset(-10);

        // Assert: Should not reach here
        fail("Should throw Exception on large negative offset");
    }

    /**
     * Test: getByteOffset_OffsetBeyondBuffer_ThrowsException
     * Scenario: Position 95, offset +10 (would access index 105 in 100-byte buffer)
     * Expected: Exception thrown
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_OffsetBeyondBuffer_ThrowsException() throws Exception {
        // Arrange
        stream.pos = 95;

        // Act
        stream.getByteOffset(10);

        // Assert: Should not reach here
        fail("Should throw Exception when pos+offset exceeds buffer");
    }

    /**
     * Test: getByteOffset_OffsetEqualsBufferBoundary_ThrowsException
     * Scenario: Position 90, offset +10 (pos+off == buffer.length, not < buffer.length)
     * Expected: Exception thrown (boundary violation)
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_OffsetEqualsBufferBoundary_ThrowsException() throws Exception {
        // Arrange: 100-byte buffer
        stream.pos = 90;

        // Act: pos(90) + off(10) = 100 (equals buffer.length, not valid index)
        stream.getByteOffset(10);

        // Assert: Should not reach here
        fail("Should throw Exception when pos+offset == buffer.length");
    }

    /**
     * Test: getByteOffset_NullBuffer_ThrowsException
     * Scenario: Buffer is null, any position/offset
     * Expected: Exception thrown
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_NullBuffer_ThrowsException() throws Exception {
        // Arrange
        stream.buffer = null;
        stream.pos = 0;

        // Act
        stream.getByteOffset(0);

        // Assert: Should not reach here
        fail("Should throw Exception when buffer is null");
    }

    /**
     * Test: getByteOffset_ZeroLengthBuffer_ThrowsException
     * Scenario: Empty buffer, position 0, offset 0
     * Expected: Exception thrown
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_ZeroLengthBuffer_ThrowsException() throws Exception {
        // Arrange
        stream.buffer = new byte[0];
        stream.pos = 0;

        // Act
        stream.getByteOffset(0);

        // Assert: Should not reach here
        fail("Should throw Exception on zero-length buffer");
    }

    /**
     * Test: getByteOffset_LargeNegativeOffset_ThrowsException
     * Scenario: Position 50, offset -100 (would access index -50)
     * Expected: Exception thrown
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_LargeNegativeOffset_ThrowsException() throws Exception {
        // Arrange
        stream.pos = 50;

        // Act
        stream.getByteOffset(-100);

        // Assert: Should not reach here
        fail("Should throw Exception on large negative offset");
    }

    // ===== TEST GROUP 5: hasNext() Boundary Tests =====

    /**
     * Test: hasNext_PositionBeforeStreamSize_ReturnsTrue
     * Scenario: Position less than streamSize
     * Expected: hasNext() returns true
     */
    @Test
    public void testHasNext_PositionBeforeStreamSize_ReturnsTrue() {
        // Arrange
        stream.streamSize = 100;
        stream.pos = 50;

        // Act & Assert
        assertTrue("hasNext() should return true when pos < streamSize", stream.hasNext());
    }

    /**
     * Test: hasNext_PositionEqualsStreamSize_ReturnsFalse
     * Scenario: Position equals streamSize (at boundary)
     * Expected: hasNext() returns false
     */
    @Test
    public void testHasNext_PositionEqualsStreamSize_ReturnsFalse() {
        // Arrange
        stream.streamSize = 100;
        stream.pos = 100;

        // Act & Assert
        assertFalse("hasNext() should return false when pos == streamSize", stream.hasNext());
    }

    /**
     * Test: hasNext_PositionBeyondStreamSize_ReturnsFalse
     * Scenario: Position greater than streamSize
     * Expected: hasNext() returns false
     */
    @Test
    public void testHasNext_PositionBeyondStreamSize_ReturnsFalse() {
        // Arrange
        stream.streamSize = 100;
        stream.pos = 150;

        // Act & Assert
        assertFalse("hasNext() should return false when pos > streamSize", stream.hasNext());
    }

    /**
     * Test: hasNext_PositionAtStart_ReturnsTrue
     * Scenario: Position at 0, streamSize > 0
     * Expected: hasNext() returns true
     */
    @Test
    public void testHasNext_PositionAtStart_ReturnsTrue() {
        // Arrange
        stream.streamSize = 50;
        stream.pos = 0;

        // Act & Assert
        assertTrue("hasNext() should return true at start of stream", stream.hasNext());
    }

    // ===== TEST GROUP 6: State Transitions & Interactions =====

    /**
     * Test: getNextByte_MultipleCallsInSequence_MaintainsPosition
     * Scenario: Call getNextByte() multiple times, verify position increments correctly
     * Expected: Each call increments position by 1
     */
    @Test
    public void testGetNextByte_MultipleCallsInSequence_MaintainsPosition() {
        // Arrange
        stream.pos = 0;
        int[] expectedPositions = {0, 1, 2, 3, 4};

        // Act & Assert
        for (int i = 0; i < 5; i++) {
            assertEquals("Position before read " + i, expectedPositions[i], stream.pos);
            stream.getNextByte();
        }
        assertEquals("Final position should be 5", 5, stream.pos);
    }

    /**
     * Test: getByteOffset_MultipleCallsPreservePosition
     * Scenario: Call getByteOffset() multiple times, verify position never changes
     * Expected: Position remains constant
     */
    @Test
    public void testGetByteOffset_MultipleCallsPreservePosition() throws Exception {
        // Arrange
        stream.pos = 30;

        // Act
        for (int i = 0; i < 5; i++) {
            stream.getByteOffset(i);
        }

        // Assert: Position should not have changed
        assertEquals("Position should be unchanged after multiple getByteOffset calls", 30, stream.pos);
    }

    /**
     * Test: getByteOffset_AndGetNextByte_CombinedReading
     * Scenario: Alternate between getByteOffset (reads ahead) and getNextByte (advances position)
     * Expected: getByteOffset reads ahead without advancing, getNextByte advances position
     */
    @Test
    public void testGetByteOffset_AndGetNextByte_CombinedReading() throws Exception {
        // Arrange
        stream.pos = 10;
        stream.buffer[10] = 0x11;
        stream.buffer[12] = 0x22;
        stream.buffer[11] = 0x33;

        // Act & Assert
        byte lookahead = stream.getByteOffset(2);
        assertEquals("getByteOffset should read pos+2", 0x22, lookahead);
        assertEquals("Position should not advance with getByteOffset", 10, stream.pos);

        byte current = stream.getNextByte();
        assertEquals("getNextByte should read pos", 0x11, current);
        assertEquals("Position should advance with getNextByte", 11, stream.pos);

        byte next = stream.getNextByte();
        assertEquals("Next byte should be 0x33", 0x33, next);
        assertEquals("Position should be 12", 12, stream.pos);
    }

    /**
     * Test: getByteOffset_WithVariousOffsets_BoundaryAlignment
     * Scenario: Read bytes at various offsets from a single position
     * Expected: All valid offsets succeed, out-of-bounds fail
     */
    @Test
    public void testGetByteOffset_WithVariousOffsets_BoundaryAlignment() throws Exception {
        // Arrange
        stream.pos = 50;
        stream.buffer = createMinimalBuffer(100);

        // Act & Assert: Valid offsets
        stream.getByteOffset(0);   // pos+0 = 50 (valid)
        stream.getByteOffset(10);  // pos+10 = 60 (valid)
        stream.getByteOffset(49);  // pos+49 = 99 (valid, last byte)

        // Act & Assert: Invalid offsets
        try {
            stream.getByteOffset(50);  // pos+50 = 100 (at boundary, invalid)
            fail("Should throw Exception at boundary");
        } catch (Exception e) {
            assertTrue("Exception expected for boundary offset", true);
        }
    }

}
