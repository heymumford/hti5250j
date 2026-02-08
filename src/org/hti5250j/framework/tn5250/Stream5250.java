/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

public class Stream5250 {

    public static final int OPCODE_OFFSET = 9;

    public int streamSize;
    public int opCode;
    public int dataStart;
    public int pos;
    public byte buffer[];

    public Stream5250(byte abyte0[]) {
        buffer = abyte0;
        // size without end of record 0xFF 0xEF
        streamSize = (abyte0[0] & 0xff) << 8 | abyte0[1] & 0xff;
        opCode = abyte0[9];
        dataStart = 6 + abyte0[6];
        pos = dataStart;
    }

    public Stream5250() {
        buffer = null;
        streamSize = 0;
        opCode = 0;
        dataStart = 0;
        pos = dataStart;
    }

    /**
     * This method takes a byte array and initializes the object information
     *    to be used.
     *
     * @param abyte0
     */
    public void initialize(byte abyte0[]) {

        buffer = abyte0;
        // size without end of record 0xFF 0xEF
        streamSize = (abyte0[0] & 0xff) << 8 | abyte0[1] & 0xff;
        opCode = abyte0[OPCODE_OFFSET];
        dataStart = 6 + abyte0[6];
        pos = dataStart;

    }

    public final int getOpCode() {
        return opCode;
    }

    /**
     * @throws IllegalStateException
     * @return
     */
    public final byte getNextByte() {
        if (buffer == null) {
            throw new IllegalStateException("Buffer is null");
        }
        if (pos < 0 || pos >= buffer.length) {
            throw new IllegalStateException("Buffer length exceeded: " + pos);
        }
        return buffer[pos++];
    }

    public final void setPrevByte()
            throws Exception {
        if (pos == 0) {
            throw new Exception("Index equals zero.");
        } else {
            pos--;
            return;
        }
    }

    /**
     * Returns where we are in the buffer
     * @return position in the buffer
     */
    public final int getCurrentPos() {
        return pos;
    }

    public final byte getByteOffset(int off)
            throws Exception {

        if (buffer == null) {
            throw new Exception("Buffer is null");
        }
        int index = pos + off;
        if (index < 0) {
            throw new Exception("Buffer index underflow: " + index);
        }
        if (index >= buffer.length) {
            throw new Exception("Buffer length exceeded: " + index);
        }
        return buffer[index];

    }

    public final boolean size() {
        return pos >= streamSize;
    }


    /**
     * Determines if any more bytes are available in the buffer to be processed.
     * @return yes or no
     */
    public final boolean hasNext() {

//      return pos >= buffer.length;
        return pos < streamSize;
    }

    /**
     * This routine will retrieve a segment based on the first two bytes being
     * the length of the segment.
     *
     * @return a new byte array containing the bytes of the segment.
     * @throws Exception
     */
    public final byte[] getSegment() throws Exception {

        // The first two bytes contain the length of the segment.
        if (buffer == null) {
            throw new Exception("Buffer is null");
        }
        if (pos < 0 || pos + 1 >= buffer.length) {
            throw new Exception("Buffer length exceeded: start " + pos);
        }
        int length = ((buffer[pos] & 0xff) << 8 | (buffer[pos + 1] & 0xff));
        // allocate space for it.
        byte[] segment = new byte[length];

        getSegment(segment, length, true);

        return segment;
    }


    /**
     * This routine will retrieve a byte array based on the first two bytes being
     * the length of the segment.
     *
     * @param segment - byte array
     * @param length - length of segment to return
     * @param adjustPos - adjust the position of the buffer to the end of the seg
     *                      ment
     * @throws Exception
     */
    public final void getSegment(byte[] segment, int length, boolean adjustPos)
            throws Exception {

        // If the length is larger than what is available throw an exception
        if (buffer == null) {
            throw new Exception("Buffer is null");
        }
        if (pos < 0 || (pos + length) > buffer.length)
            throw new Exception("Buffer length exceeded: start " + pos
                    + " length " + length);
        // use the system array copy to move the bytes from the buffer
        //    to the allocated byte array
        System.arraycopy(buffer, pos, segment, 0, length);

        // update the offset to be after the segment so the next byte can be read
        if (adjustPos)
            pos += length;

    }

}
