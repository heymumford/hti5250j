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
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import static org.hti5250j.keyboard.KeyMnemonic.PASTE;

/**
 * Paste from the clipboard
 */
public class PasteAction extends EmulatorAction {

    private static final long serialVersionUID = 1L;

    private final HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    public PasteAction(SessionPanel session, KeyMapper keyMap) {
        super(session,
                PASTE.mnemonic,
                KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.ALT_MASK),
                keyMap);
    }

    public void actionPerformed(ActionEvent event) {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            final Transferable transferable = cb.getContents(this);
            if (transferable != null) {
                final String content = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                session.getScreen().pasteText(content, false);
            }
        } catch (HeadlessException e1) {
            log.debug("HeadlessException", e1);
        } catch (UnsupportedFlavorException e1) {
            log.debug("the requested data flavor is not supported", e1);
        } catch (IOException e1) {
            log.debug("data is no longer available in the requested flavor", e1);
        }
    }

}
