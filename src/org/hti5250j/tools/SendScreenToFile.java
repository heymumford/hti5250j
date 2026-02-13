/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JFileChooser;

import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.gui.HTI5250jFileChooser;
import org.hti5250j.gui.HTI5250jFileFilter;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

public class SendScreenToFile {

    private static final HTI5250jLogger LOG = HTI5250jLogFactory
            .getLogger(SendScreenToFile.class);

    /**
     * @param parent
     * @param screen
     */
    public static final void showDialog(Component parent, Screen5250 screen) {
        String workingDir = System.getProperty("user.dir");
        HTI5250jFileChooser fileChooser = new HTI5250jFileChooser(workingDir);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new HTI5250jFileFilter("txt", "Text files"));

        int ret = fileChooser.showSaveDialog(parent);

        // check to see if something was actually chosen
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            final String fname = file.getName();
            if (fname.lastIndexOf('.') < 0) {
                file = new File(file.toString() + ".txt");
            }

            StringBuffer sb = new StringBuffer();
            char[] s = screen.getScreenAsChars();
            int c = screen.getColumns();
            int l = screen.getRows() * c;
            int col = 0;
            for (int x = 0; x < l; x++, col++) {
                sb.append(s[x]);
                if (col == c) {
                    sb.append('\n');
                    col = 0;
                }
            }

            writeToFile(sb.toString(), file);

        }
    }

    private static void writeToFile(String sc, File file) {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(sc.getBytes());
            out.flush();
            out.close();

        } catch (FileNotFoundException fnfe) {
            LOG.warn("fnfe: " + fnfe.getMessage());
        } catch (IOException ioe) {
            LOG.warn("ioe: " + ioe.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException exc) {
                    LOG.warn("ioe finally: " + exc.getMessage());
                }
            }

        }

    }

}
