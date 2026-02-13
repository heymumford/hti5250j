/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009,2021
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: nitram509
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding.builtin;

import com.ibm.as400.access.ConvTable;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hti5250j.framework.tn5250.ByteExplainer.*;

/**
 * @author nitram509
 */
public final class CCSID930 implements ICodepageConverter {

    public static final String NAME = "930";
    public static final String DESCR = "Japan Katakana (extended range), DBCS";

    private final AtomicBoolean doubleByteActive = new AtomicBoolean(false);
    private final AtomicBoolean secondByteNeeded = new AtomicBoolean(false);
    private final AtomicInteger lastByte = new AtomicInteger(0);
    private final ConvTable convTable;

    public CCSID930() {
        try {
            convTable = ConvTable.getTable("Cp930");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return NAME;
    }

    public String getDescription() {
        return DESCR;
    }

    @Override
    public ICodepageConverter init() {
        return null;
    }

    public String getEncoding() {
        return NAME;
    }

    @Override
    public byte uni2ebcdic(char index) {
        return 0;
    }

    @Override
    public char ebcdic2uni(int index) {
        if (isShiftIn(index)) {
            doubleByteActive.set(true);
            secondByteNeeded.set(false);
            return 0;
        }
        if (isShiftOut(index)) {
            doubleByteActive.set(false);
            secondByteNeeded.set(false);
            return 0;
        }
        if (isDoubleByteActive()) {
            if (!secondByteNeeded()) {
                lastByte.set(index);
                secondByteNeeded.set(true);
                return 0;
            } else {
                int i = lastByte.get() << 8 | (index & 0xff);
                secondByteNeeded.set(false);
                return convTable.byteArrayToString(new byte[]{SHIFT_IN, lastByte.byteValue(), (byte) (index & 0xff), SHIFT_OUT}, 0, 4).charAt(0);
            }
        }
        return convTable.byteArrayToString(new byte[]{(byte) (index & 0xff)}, 0, 1).charAt(0);
    }

    @Override
    public boolean isDoubleByteActive() {
        return doubleByteActive.get();
    }

    @Override
    public boolean secondByteNeeded() {
        return secondByteNeeded.get();
    }

    /**
     * Check if the given byte is a shift-in control character (0x0E).
     * Shift-in activates double-byte mode for processing DBCS characters.
     *
     * @param aByte the byte value to check
     * @return true if the byte is a shift-in character, false otherwise
     */
    public boolean isShiftIn(int aByte) {
        return (aByte & 0xff) == SHIFT_IN;
    }

    /**
     * Check if the given byte is a shift-out control character (0x0F).
     * Shift-out deactivates double-byte mode and returns to single-byte processing.
     *
     * @param aByte the byte value to check
     * @return true if the byte is a shift-out character, false otherwise
     */
    public boolean isShiftOut(int aByte) {
        return (aByte & 0xff) == SHIFT_OUT;
    }
}
