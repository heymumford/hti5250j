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

import org.hti5250j.tools.*;

public class JobTabPanel extends JPanel implements QueueFilterInterface {

    private static final long serialVersionUID = 1L;
    JRadioButton all;
    JRadioButton select;
    JTextField jobName;
    JTextField jobUser;
    JTextField jobNumber;

    public JobTabPanel() {
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

        select = new JRadioButton("Job Name");
        select.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                select_itemStateChanged(e);
            }
        });

        jobName = new JTextField("*CURRENT", 10);
        jobUser = new JTextField(10);
        jobNumber = new JTextField(10);
        jobName.setEnabled(false);
        jobUser.setEnabled(false);
        jobNumber.setEnabled(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(all);
        bg.add(select);

        add(all);
        add(new JLabel(""));
        add(select);
        add(jobName);
        add(new JLabel("Job User"));
        add(jobUser);
        add(new JLabel("Job Number"));
        add(jobNumber);

        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    }

    /**
     * Reset to default value(s)
     */
    public void reset() {

        jobName.setText("*CURRENT");
        jobUser.setText("");
        jobNumber.setText("");
        all.setSelected(true);

    }

    void select_itemStateChanged(ItemEvent e) {
        if (select.isSelected()) {
            jobName.setEnabled(true);
            jobUser.setEnabled(true);
            jobNumber.setEnabled(true);
        } else {
            jobName.setEnabled(false);
            jobUser.setEnabled(false);
            jobNumber.setEnabled(false);
        }
    }

    public String getJobName() {
        if (all.isSelected()) {
            return "%ALL%";
        } else {
            return jobName.getText().trim();
        }
    }

    public String getJobUser() {

        if (all.isSelected()) {
            return "%ALL%";
        } else {
            return jobUser.getText().trim();
        }

    }

    public String getJobNumber() {

        if (all.isSelected()) {
            return "%ALL%";
        } else {
            return jobNumber.getText().trim();
        }

    }
}
