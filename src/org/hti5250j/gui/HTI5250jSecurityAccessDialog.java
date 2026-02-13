/*
 * SPDX-FileCopyrightText: Copyright (c) 2001 , 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.gui;

import javax.swing.JOptionPane;

import org.hti5250j.tools.LangTool;

public class HTI5250jSecurityAccessDialog {

    private HTI5250jSecurityAccessDialog() {

    }

    public static void showErrorMessage(SecurityException se) {

        GenericTn5250JFrame parent = new GenericTn5250JFrame();
        JOptionPane.showMessageDialog(parent, LangTool.getString("messages.SADMessage")
                        + se.getMessage(),
                 LangTool.getString("messages.SADTitle"),
                JOptionPane.ERROR_MESSAGE);


    }
}
