/*
 * SPDX-FileCopyrightText: Copyright (c) 2001 , 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.gui;

import javax.swing.JOptionPane;

import org.hti5250j.tools.LangTool;
import org.hti5250j.gui.GenericTn5250JFrame;

public class HTI5250jSecurityAccessDialog {

    // set so outsiders can not initialize the dialog.
    private HTI5250jSecurityAccessDialog() {

    }

    static public void showErrorMessage(SecurityException se) {

        GenericTn5250JFrame parent = new GenericTn5250JFrame();
        JOptionPane.showMessageDialog(parent, LangTool.getString("messages.SADMessage")
                        + se.getMessage()
                , LangTool.getString("messages.SADTitle"),
                JOptionPane.ERROR_MESSAGE);


    }
}
