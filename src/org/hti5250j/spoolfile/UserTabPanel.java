/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.spoolfile;

import java.awt.event.*;
import javax.swing.*;

import org.hti5250j.tools.AlignLayout;
import org.hti5250j.event.ToggleDocumentListener;
import org.hti5250j.gui.ToggleDocument;

public class UserTabPanel extends JPanel implements QueueFilterInterface,
        ToggleDocumentListener {

    private static final long serialVersionUID = 1L;
    JRadioButton all;
    JRadioButton select;
    JTextField user;
    ;

    public UserTabPanel() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {

        setLayout(new AlignLayout(2, 5, 5));

        all = new JRadioButton("All");

        all.setSelected(false);

        select = new JRadioButton("User");
        select.setSelected(true);
        select.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                select_itemStateChanged(e);
            }
        });

        user = new JTextField("*CURRENT", 15);
        ToggleDocument td = new ToggleDocument();
        td.addToggleDocumentListener(this);
        user.setDocument(td);
        user.setText("*CURRENT");

        ButtonGroup bg = new ButtonGroup();
        bg.add(all);
        bg.add(select);

        add(all);
        add(new JLabel(""));
        add(select);
        add(user);

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    }

    /**
     * Reset to default value(s)
     */
    public void reset() {

//      user.setEnabled(true);
        user.setText("*CURRENT");
        select.setSelected(true);

    }

    void select_itemStateChanged(ItemEvent e) {
//      if (select.isSelected())
//         user.setEnabled(true);
//      else
//         user.setEnabled(false);
    }

    public void toggleNotEmpty() {

        select.setSelected(true);

    }

    public void toggleEmpty() {

    }

    public String getUser() {
        if (all.isSelected())
            return "*ALL";
        else
            return user.getText().trim();
    }

    public void setUser(String filter) {

        user.setText(filter);
    }
}
