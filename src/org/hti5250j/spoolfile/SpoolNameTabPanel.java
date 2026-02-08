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

public class SpoolNameTabPanel extends JPanel implements QueueFilterInterface,
        ToggleDocumentListener {

    private static final long serialVersionUID = 1L;
    JRadioButton all;
    JRadioButton select;
    JTextField spoolName;

    public SpoolNameTabPanel() {
        try {
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void jbInit() throws Exception {

        setLayout(new AlignLayout(2, 5, 5));

        all = new JRadioButton("All");

        all.setSelected(true);

        select = new JRadioButton("Spool Name");
        select.setSelected(false);
        select.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                select_itemStateChanged(e);
            }
        });

        spoolName = new JTextField(15);
        ToggleDocument td = new ToggleDocument();
        td.addToggleDocumentListener(this);
        spoolName.setDocument(td);

//      spoolName.setEnabled(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(all);
        bg.add(select);

        add(all);
        add(new JLabel(""));
        add(select);
        add(spoolName);

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    }

    /**
     * Reset to default value(s)
     */
    public void reset() {

//      spoolName.setEnabled(false);
        spoolName.setText("");
        all.setSelected(true);

    }

    void select_itemStateChanged(ItemEvent e) {
//      if (select.isSelected())
//         spoolName.setEnabled(true);
//      else
//         spoolName.setEnabled(false);
    }

    public void toggleNotEmpty() {

        select.setSelected(true);

    }

    public void toggleEmpty() {

    }

    public String getSpoolName() {
        if (all.isSelected())
            return "";
        else
            return spoolName.getText().trim();
    }

    public void setSpoolName(String filter) {

        spoolName.setText(filter);
    }
}
