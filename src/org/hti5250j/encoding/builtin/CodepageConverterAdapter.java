/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding.builtin;

import java.util.Arrays;

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
            return (byte) '?';
        }
        return (byte) reverse_codepage[index];
    }

    /* (non-Javadoc)
     * @see org.hti5250j.cp.ICodepageConverter#ebcdic2uni(int)
     */
    public char ebcdic2uni(int index) {
        index = index & 0xFF;
        if (index >= codepage.length) {
            return '?';
        }
        return codepage[index];
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
