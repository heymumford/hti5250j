/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,202,2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools.encoder;

/**
 * This class is an exception that is raised by Encode or one of it's
 * subclasses.  It may also be subclassed for exceptions thrown by subclasses
 * of Encode. It represents any problem encountered while encoding an image.
 * The message is used to state the type of error.
 */
public class EncoderException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with the given message.
     */
    public EncoderException(String msg) {
        super(msg);
    }

}
