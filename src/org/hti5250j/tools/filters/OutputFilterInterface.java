/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.tools.filters;


import java.io.*;
import java.util.ArrayList;

public interface OutputFilterInterface {


    public void createFileInstance(String fileName) throws
            FileNotFoundException;

    public abstract void writeHeader(String fileName, String host,
                                     ArrayList ffd, char decSep);

    public abstract void writeFooter(ArrayList ffd);

    public abstract void parseFields(byte[] cByte, ArrayList ffd, StringBuffer rb);

    public abstract boolean isCustomizable();

    public abstract void setCustomProperties();
}
