/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.tools;

import java.io.*;
import javax.swing.*;
import java.awt.Frame;

import org.hti5250j.tools.logging.*;
import org.hti5250j.SessionPanel;
import org.hti5250j.tools.encoder.EncodeComponent;
import org.hti5250j.tools.filters.XTFRFileFilter;
import org.hti5250j.gui.HTI5250jFileChooser;

public class SendScreenImageToFile {

    SessionPanel session;
    //  Change sent by Luc - LDC to pass a parent frame like the other dialogs
    Frame parent;
    private HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    public SendScreenImageToFile(Frame parent, SessionPanel ses) {

        session = ses;
        this.parent = parent;


        try {
            jbInit();
        } catch (Exception ex) {
            log.warn("Error in constructor: " + ex.getMessage());
        }
    }

    void jbInit() throws Exception {
        getPCFile();

    }

    /**
     * Get the local file from a file chooser
     */
    private void getPCFile() {

        String workingDir = System.getProperty("user.dir");
        HTI5250jFileChooser pcFileChooser = new HTI5250jFileChooser(workingDir);

        XTFRFileFilter pngFilter = new XTFRFileFilter("png", "Portable Network Graphics");

        pcFileChooser.setFileFilter(pngFilter);

        int ret = pcFileChooser.showSaveDialog(parent);

        // check to see if something was actually chosen
        if (ret == JFileChooser.APPROVE_OPTION) {

            File file;

            try {
                if (!pcFileChooser.getSelectedFile().getCanonicalPath().endsWith(".png"))
                    file = new File(pcFileChooser.getSelectedFile().getCanonicalPath()
                            + ".png");
                else
                    file = pcFileChooser.getSelectedFile();


                EncodeComponent.encode(EncodeComponent.PNG, session, file);
            } catch (Exception e) {
                log.warn("Error generating PNG exception caught: " + e.getMessage());

            }

        }

    }

}
