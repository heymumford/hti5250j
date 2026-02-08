/*
 * SPDX-FileCopyrightText: Copyright (c) 2015
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Martin W. Kirst
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.hti5250j.encoding.ICodePage;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class DataStreamDumper {

    private AtomicInteger counter = new AtomicInteger(0);

    private FileOutputStream fw;
    private BufferedOutputStream dw;
    private boolean dumpActive = false;
    private ICodePage codePage;

    private HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());


    public void toggleDebug(ICodePage cp) {

        if (codePage == null)
            codePage = cp;

        dumpActive = !dumpActive;
        if (dumpActive) {

            try {
                if (fw == null) {
                    fw = new FileOutputStream("log.txt");
                    dw = new BufferedOutputStream(fw);
                }
            } catch (FileNotFoundException fnfe) {
                log.warn(fnfe.getMessage());
            }

        } else {

            try {

                if (dw != null)
                    dw.close();
                if (fw != null)
                    fw.close();
                dw = null;
                fw = null;
                codePage = null;
            } catch (IOException ioe) {

                log.warn(ioe.getMessage());
            }
        }

        log.info("Data Stream output is now " + dumpActive);
    }

    public void dump(byte[] abyte0) {
        if (!dumpActive) {
            return;
        }

        try {

            log.info("\n Buffer Dump of data from AS400: ");
            dw.write("\r\n Buffer Dump of data from AS400: ".getBytes());

            StringBuilder h = new StringBuilder();
            for (int x = 0; x < abyte0.length; x++) {
                if (x % 16 == 0) {
                    System.out.println("  " + h.toString());
                    dw.write(("  " + h.toString() + "\r\n").getBytes());

                    h.setLength(0);
                    h.append("+0000");
                    h.setLength(5 - Integer.toHexString(x).length());
                    h.append(Integer.toHexString(x).toUpperCase());

                    System.out.print(h.toString());
                    dw.write(h.toString().getBytes());

                    h.setLength(0);
                }
                char ac = codePage.ebcdic2uni(abyte0[x]);
                if (ac < ' ')
                    h.append('.');
                else
                    h.append(ac);
                if (x % 4 == 0) {
                    System.out.print(" ");
                    dw.write((" ").getBytes());

                }

                if (Integer.toHexString(abyte0[x] & 0xff).length() == 1) {
                    System.out.print("0" + Integer.toHexString(abyte0[x] & 0xff).toUpperCase());
                    dw.write(("0" + Integer.toHexString(abyte0[x] & 0xff).toUpperCase()).getBytes());

                } else {
                    System.out.print(Integer.toHexString(abyte0[x] & 0xff).toUpperCase());
                    dw.write((Integer.toHexString(abyte0[x] & 0xff).toUpperCase()).getBytes());
                }

            }
            System.out.println();
            dw.write("\r\n".getBytes());

            dw.flush();
        } catch (IOException e) {
            log.warn("Cannot dump from host! Message=" + e.getMessage());
        }

    }

    void dumpRaw(byte[] buffer) {
        try {
            String fname = "dump_" + counter.get() + ".data";
            log.debug("Dumping file: " + fname);
            FileOutputStream fos = new FileOutputStream(fname);
            fos.write(buffer);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
