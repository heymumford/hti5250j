/**
 * Title: tn5250J
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Test suite for Stream5250 buffer boundary conditions
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
package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test cases for Stream5250 buffer boundary violations.
 *
 * CRITICAL BUGS UNDER TEST:
 * Bug 1: Off-by-one buffer boundary (line 81)
 *   Current: if (pos > buffer.length)
 *   Should be: if (pos >= buffer.length)
 *   Impact: getNextByte() can access buffer[pos] when pos == buffer.length (ArrayIndexOutOfBoundsException)
 *
 * Bug 2: Negative index access (lines 105-113)
 *   Current: getByteOffset(int off) doesn't validate negative resulting indices
 *   Impact: getByteOffset(-1) with pos=0 accesses buffer[-1]
 */
public class Stream5250Test {

    private static final byte[] MINIMAL_BUFFER = createMinimalBuffer();
    private Stream5250 stream;

    /**
     * Creates a minimal valid 5250 stream buffer.
     * Format: [length_high, length_low, ..., header_padding, header_len, ..., opcode, ...]
     */
    private static byte[] createMinimalBuffer() {
        byte[] buffer = new byte[20];
        // Stream size: 20 bytes (bytes 0-1)
        buffer[0] = 0x00;
        buffer[1] = 0x14;
        // Header length at offset 6
        buffer[6] = 0x03;  // 3 bytes of header after position 6
        // Opcode at position 9 (6 + 3)
        buffer[9] = 0x42;
        return buffer;
    }

    @Before
    public void setUp() {
        stream = new Stream5250(MINIMAL_BUFFER);
    }

    /**
     * TEST BUG 1: Off-by-one buffer boundary
     *
     * Bug: getNextByte() uses "pos > buffer.length" instead of "pos >= buffer.length"
     * This allows reading one byte past the end of the buffer.
     *
     * When pos equals buffer.length (9 in this buffer):
     * - Current behavior: No exception, tries to access buffer[9] causing ArrayIndexOutOfBoundsException
     * - Expected behavior: Should throw IllegalStateException immediately
     *
     * This test FAILS with the current implementation.
     */
    @Test(expected = IllegalStateException.class)
    public void testGetNextByte_ThrowsIllegalStateWhenPosEqualsBufferLength() {
        // Arrange: Position stream to the end (pos == buffer.length)
        stream.pos = MINIMAL_BUFFER.length;

        // Act: Try to read beyond buffer boundary
        // This should throw IllegalStateException, not ArrayIndexOutOfBoundsException
        byte result = stream.getNextByte();

        // Assert: Should never reach here
        fail("Expected IllegalStateException when pos equals buffer.length, but got byte: " + result);
    }

    /**
     * TEST BUG 1: Verify buffer boundary is enforced
     *
     * When pos is one less than buffer.length, getNextByte() should succeed.
     * This verifies we can read the last valid byte in the buffer.
     */
    @Test
    public void testGetNextByte_SucceedsWhenPosIsBeforeBufferLength() {
        // Arrange: Position stream to last valid index
        stream.pos = MINIMAL_BUFFER.length - 1;
        byte expectedValue = (byte) 0x99;
        MINIMAL_BUFFER[MINIMAL_BUFFER.length - 1] = expectedValue;

        // Act: Read the last valid byte
        byte result = stream.getNextByte();

        // Assert: Should succeed and return the byte
        assertEquals("Should read the last valid byte in buffer", expectedValue, result);
        assertEquals("Position should be incremented after read", MINIMAL_BUFFER.length, stream.pos);
    }

    /**
     * TEST BUG 1: Boundary condition - exactly at limit
     *
     * When pos is already at buffer.length (limit), attempting to read should fail.
     */
    @Test(expected = IllegalStateException.class)
    public void testGetNextByte_ThrowsWhenAtExactBoundary() {
        // Arrange: Set position exactly at buffer length
        stream.pos = MINIMAL_BUFFER.length;

        // Act: Attempt to read when already at boundary
        stream.getNextByte();

        // Assert: Should throw IllegalStateException
        fail("Expected IllegalStateException at exact buffer boundary");
    }

    /**
     * TEST BUG 2: Negative index access vulnerability
     *
     * Bug: getByteOffset(int off) doesn't validate that (pos + off) is non-negative.
     * This allows reading from negative array indices.
     *
     * With pos=0 and off=-1:
     * - Current behavior: Calculates (pos + off) = -1, tries to access buffer[-1] (ArrayIndexOutOfBoundsException)
     * - Expected behavior: Should throw Exception immediately
     *
     * This test FAILS with the current implementation.
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_ThrowsExceptionOnNegativeResultingIndex() throws Exception {
        // Arrange: Position at start of buffer
        stream.pos = 0;

        // Act: Try to read with negative offset that results in negative index
        // This would attempt buffer[-1], which is invalid
        byte result = stream.getByteOffset(-1);

        // Assert: Should never reach here
        fail("Expected Exception when resulting index is negative, but got byte: " + result);
    }

    /**
     * TEST BUG 2: Negative offset edge case
     *
     * When pos=5 and off=-6, the result would be index -1 (out of bounds on left side).
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_ThrowsOnLargeNegativeOffset() throws Exception {
        // Arrange: Position somewhere in the middle
        stream.pos = 5;

        // Act: Negative offset that would go before buffer start
        byte result = stream.getByteOffset(-6);

        // Assert: Should throw Exception
        fail("Expected Exception on large negative offset");
    }

    /**
     * TEST BUG 2: Boundary case with zero position
     *
     * When pos=0, any negative offset should be rejected.
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_ThrowsWithZeroPositionAndNegativeOffset() throws Exception {
        // Arrange: At the very start of buffer
        stream.pos = 0;

        // Act: Any negative offset from position 0 is invalid
        stream.getByteOffset(-1);

        // Assert: Should throw
        fail("Cannot use negative offset from position 0");
    }

    /**
     * TEST BUG 2: Verify positive offsets still work
     *
     * Positive offsets within bounds should still succeed.
     */
    @Test
    public void testGetByteOffset_SucceedsWithValidPositiveOffset() throws Exception {
        // Arrange: Position in middle of buffer with valid positive offset
        stream.pos = 5;
        byte expectedValue = (byte) 0xAB;
        MINIMAL_BUFFER[8] = expectedValue;  // pos(5) + off(3) = 8

        // Act: Read with valid positive offset
        byte result = stream.getByteOffset(3);

        // Assert: Should succeed
        assertEquals("Should read byte at valid positive offset", expectedValue, result);
    }

    /**
     * TEST BUG 2: Positive offset boundary (still within buffer)
     *
     * Reading up to the last byte with a positive offset should work.
     */
    @Test
    public void testGetByteOffset_SucceedsAtLastByteWithPositiveOffset() throws Exception {
        // Arrange: Position allows reaching last byte with offset
        stream.pos = MINIMAL_BUFFER.length - 1;
        byte expectedValue = (byte) 0xCD;
        MINIMAL_BUFFER[MINIMAL_BUFFER.length - 1] = expectedValue;

        // Act: Read with zero offset (last byte)
        byte result = stream.getByteOffset(0);

        // Assert: Should succeed
        assertEquals("Should read at exact position with zero offset", expectedValue, result);
    }

    /**
     * TEST BUG 2: Null buffer guard
     *
     * getByteOffset should check for null buffer before accessing.
     */
    @Test(expected = Exception.class)
    public void testGetByteOffset_ThrowsWhenBufferIsNull() throws Exception {
        // Arrange: Create stream with null buffer
        stream.buffer = null;
        stream.pos = 0;

        // Act: Try to read from null buffer
        stream.getByteOffset(0);

        // Assert: Should throw Exception
        fail("Expected Exception for null buffer");
    }

    /**
     * TEST BUG 1: Null buffer guard in getNextByte
     *
     * getNextByte should check for null buffer.
     */
    @Test(expected = IllegalStateException.class)
    public void testGetNextByte_ThrowsWhenBufferIsNull() {
        // Arrange: Create stream with null buffer
        stream.buffer = null;
        stream.pos = 0;

        // Act: Try to read from null buffer
        stream.getNextByte();

        // Assert: Should throw IllegalStateException
        fail("Expected IllegalStateException for null buffer");
    }

    /**
     * Comprehensive boundary test: Multiple sequential reads near end
     *
     * This tests the interaction of multiple getNextByte calls as position moves toward boundary.
     */
    @Test
    public void testGetNextByte_SequentialReadsNearBoundary() {
        // Arrange: Position near end, leaving 2 bytes to read
        stream.pos = MINIMAL_BUFFER.length - 2;
        byte byte1 = (byte) 0x11;
        byte byte2 = (byte) 0x22;
        MINIMAL_BUFFER[MINIMAL_BUFFER.length - 2] = byte1;
        MINIMAL_BUFFER[MINIMAL_BUFFER.length - 1] = byte2;

        // Act & Assert: Read the last two bytes successfully
        assertEquals("Read second-to-last byte", byte1, stream.getNextByte());
        assertEquals("Position should be MINIMAL_BUFFER.length - 1", MINIMAL_BUFFER.length - 1, stream.pos);

        assertEquals("Read last byte", byte2, stream.getNextByte());
        assertEquals("Position should be MINIMAL_BUFFER.length", MINIMAL_BUFFER.length, stream.pos);

        // Act & Assert: Next read should fail
        try {
            stream.getNextByte();
            fail("Expected IllegalStateException after reading all bytes");
        } catch (IllegalStateException e) {
            // Expected
            assertTrue("Exception message should mention buffer exceeded",
                    e.getMessage().contains("Buffer length exceeded"));
        }
    }

}
