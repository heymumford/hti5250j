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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static org.hti5250j.keyboard.KeyMnemonic.OPEN_SAME;

/**
 * Open Same Session emulator action to open a duplicate session
 */
public class OpenSameAction extends EmulatorAction {

    private static final long serialVersionUID = 1L;

    public OpenSameAction(SessionPanel session, KeyMapper keyMap) {
        super(session, OPEN_SAME.mnemonic,
                KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.ALT_MASK),
                keyMap);
    }

    public void actionPerformed(ActionEvent e) {
        session.startDuplicateSession();
    }
}
