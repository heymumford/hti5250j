/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding.builtin;

import java.util.Arrays;
import org.hti5250j.encoding.CharacterConversionException;

/**
 * Adapter class for converters using 8bit codepages.
 *
 * @author master_jaf
 */
public abstract class CodepageConverterAdapter implements ICodepageConverter {

    private char[] codepage = null;
    private int[] reverse_codepage = null;

    /* (non-Javadoc)
     * @see org.hti5250j.cp.ICodepageConverter#init()
     */
    public ICodepageConverter init() {
        codepage = getCodePage();

        int size = 0;
        for (char c : codepage) {
            size = Math.max(size, c);
        }
        assert (size + 1) < 1024 * 1024; // some kind of maximum size limiter.
        reverse_codepage = new int[size + 1];
        Arrays.fill(reverse_codepage, '?');
        for (int i = 0; i < codepage.length; i++) {
            reverse_codepage[codepage[i]] = i;
        }
        return this;
    }

    /* (non-Javadoc)
     * @see org.hti5250j.cp.ICodepageConverter#uni2ebcdic(char)
     */
    public byte uni2ebcdic(char index) {
        if (index >= reverse_codepage.length) {
            throw new CharacterConversionException(
                formatUniToEbcdicError(index, reverse_codepage.length - 1)
            );
        }
        return (byte) reverse_codepage[index];
    }

    /* (non-Javadoc)
     * @see org.hti5250j.cp.ICodepageConverter#ebcdic2uni(int)
     */
    public char ebcdic2uni(int index) {
        index = index & 0xFF;
        if (index >= codepage.length) {
            throw new CharacterConversionException(
                formatEbcdicToUniError(index, codepage.length - 1)
            );
        }
        return codepage[index];
    }

    /**
     * Formats an error message for Unicode to EBCDIC conversion failures.
     *
     * @param codepoint the codepoint that failed conversion
     * @param maxValid the maximum valid codepoint for this converter
     * @return formatted error message with converter context
     */
    private String formatUniToEbcdicError(int codepoint, int maxValid) {
        return String.format(
            "[CCSID-%s] Unicode to EBCDIC conversion failed: character U+%04X (decimal %d) " +
            "cannot be mapped to this codepage (valid range: U+0000-U+%04X)",
            getName(),
            codepoint,
            codepoint,
            maxValid
        );
    }

    /**
     * Formats an error message for EBCDIC to Unicode conversion failures.
     *
     * @param codepoint the EBCDIC codepoint that failed conversion
     * @param maxValid the maximum valid EBCDIC codepoint for this converter
     * @return formatted error message with converter context
     */
    private String formatEbcdicToUniError(int codepoint, int maxValid) {
        return String.format(
            "[CCSID-%s] EBCDIC to Unicode conversion failed: byte 0x%02X (decimal %d) " +
            "is out of bounds (valid range: 0x00-0x%02X)",
            getName(),
            codepoint,
            codepoint,
            maxValid
        );
    }

    /**
     * @return The oringal 8bit codepage.
     */
    protected abstract char[] getCodePage();

    @Override
    public boolean isDoubleByteActive() {
        return false;
    }

    @Override
    public boolean secondByteNeeded() {
        return false;
    }
}
