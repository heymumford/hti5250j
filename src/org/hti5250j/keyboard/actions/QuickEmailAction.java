/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.keyboard.actions;

import org.hti5250j.SessionPanel;
import org.hti5250j.keyboard.KeyMapper;
import org.hti5250j.mailtools.SendEMailDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static org.hti5250j.keyboard.KeyMnemonic.QUICK_MAIL;

/**
 * Quick Email Action
 */
public class QuickEmailAction extends EmulatorAction {

    private static final long serialVersionUID = 1L;

    public QuickEmailAction(SessionPanel session, KeyMapper keyMap) {
        super(session,
                QUICK_MAIL.mnemonic,
                KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.ALT_MASK),
                keyMap);
    }

    public void actionPerformed(ActionEvent e) {
        Runnable emailIt = new Runnable() {
            public void run() {
                new SendEMailDialog((JFrame) SwingUtilities.getRoot(session),
                        session, false);
            }

        };
        SwingUtilities.invokeLater(emailIt);
    }
}
