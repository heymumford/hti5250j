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


    void createFileInstance(String fileName) throws
            FileNotFoundException;

    void writeHeader(String fileName, String host,
                                     ArrayList ffd, char decSep);

    void writeFooter(ArrayList ffd);

    void parseFields(byte[] cByte, ArrayList ffd, StringBuffer rb);

    boolean isCustomizable();

    void setCustomProperties();
}
