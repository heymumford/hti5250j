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
import org.hti5250j.tools.XTFRFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static org.hti5250j.keyboard.KeyMnemonic.FILE_TRANSFER;

/**
 * Display session attributes
 */
public class TransferAction extends EmulatorAction {

    private static final long serialVersionUID = 1L;

    public TransferAction(SessionPanel session, KeyMapper keyMap) {
        super(session,
                FILE_TRANSFER.mnemonic,
                KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.ALT_MASK),
                keyMap);
    }

    public void actionPerformed(ActionEvent e) {
        new XTFRFile((Frame) SwingUtilities.getRoot(session),
                session.getVT(), session);
    }
}
