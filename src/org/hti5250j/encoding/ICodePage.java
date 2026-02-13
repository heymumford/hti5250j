/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.encoding;

public interface ICodePage {

    /**
     * Convert a single byte (or maybe more bytes which representing one character) to a Unicode character.
     *
     * @param index the EBCDIC codepoint to convert
     * @return the Unicode character representing the EBCDIC codepoint
     * @throws CharacterConversionException if the codepoint cannot be converted
     */
    char ebcdic2uni(int index);

    /**
     * Convert a Unicode character in it's byte representation.
     * Therefore, only 8bit codepages are supported.
     *
     * @param index the Unicode character to convert
     * @return the EBCDIC byte representation of the character
     * @throws CharacterConversionException if the character cannot be converted to EBCDIC
     */
    byte uni2ebcdic(char index);

    boolean isDoubleByteActive();

    boolean secondByteNeeded();
}
