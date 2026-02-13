/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

/**
 * Exception thrown when a character conversion fails due to an invalid codepoint
 * or other conversion error.
 *
 * This exception replaces the silent error handling pattern where conversion
 * failures would silently return a space character ' ' or '?'.
 *
 * @author Eric C. Mumford
 */
public class CharacterConversionException extends RuntimeException {

    /**
     * Constructs a new CharacterConversionException with the specified detail message.
     *
     * @param message the detail message describing the conversion error
     */
    public CharacterConversionException(String message) {
        super(message);
    }

    /**
     * Constructs a new CharacterConversionException with the specified detail message
     * and cause.
     *
     * @param message the detail message describing the conversion error
     * @param cause the cause of the conversion failure
     */
    public CharacterConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
