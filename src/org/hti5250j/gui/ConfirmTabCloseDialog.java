/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: duncanc
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.hti5250j.tools.LangTool;

/**
 * Small dialog asking the user to confirm the close tab request
 *
 * @author duncanc
 */
public class ConfirmTabCloseDialog {

    private static final String[] OPTIONS = new String[]{LangTool.getString("key.labelClose"), LangTool.getString("ss.optCancel")};

    private final Component parent;

    private JDialog dialog;
    private JOptionPane pane;


    /**
     * @param parent
     */
    public ConfirmTabCloseDialog(Component parent) {
        super();
        this.parent = parent;
        initLayout();
    }

    private void initLayout() {
        Object[] messages = new Object[1];
            JPanel srp = new JPanel();
            srp.setLayout(new BorderLayout());
            JLabel jl = new JLabel("Are you sure you want to close this tab?");
            srp.add(jl, BorderLayout.NORTH);
            messages[0] = srp;

        pane = new JOptionPane(messages,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                OPTIONS,
                OPTIONS[0]);

        dialog = pane.createDialog(parent, LangTool.getString("sa.confirmTabClose"));

    }

    /**
     * Shows the dialog and returns the true if the close was confirmed
     * or false if the operation was canceled.
     *
     * @return
     */
    public boolean show() {
        dialog.setVisible(true);
        if (OPTIONS[0].equals(pane.getValue())) {
            return true;
        }
        return false;
    }

}
