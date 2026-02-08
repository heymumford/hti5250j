/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding;

/**
 *
 * This class controls the translation from EBCDIC to ASCII and ASCII to EBCDIC
 *
 */
public abstract class AbstractCodePage implements ICodePage {

    protected AbstractCodePage(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    protected String encoding;
}
