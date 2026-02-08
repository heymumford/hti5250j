/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.sessionsettings;

import org.hti5250j.SessionConfig;
import org.hti5250j.tools.LangTool;

import javax.swing.*;
import java.awt.*;

class MouseAttributesPanel extends AttributesPanel {

    private static final long serialVersionUID = 1L;
    private JCheckBox dceCheck;
    private JCheckBox mwCheck;

    MouseAttributesPanel(SessionConfig config) {
        super(config, "Mouse");
    }

    /**
     * Component initialization
     */
    public void initPanel() throws Exception {

        setLayout(new BorderLayout());
        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        add(contentPane, BorderLayout.NORTH);

        // define double click as enter
        JPanel dcep = new JPanel();
        dcep.setBorder(BorderFactory.createTitledBorder(LangTool.getString("sa.doubleClick")));

        dceCheck = new JCheckBox(LangTool.getString("sa.sendEnter"));

        // check if double click sends enter
        dceCheck.setSelected(getStringProperty("doubleClick").equals("Yes"));

        dcep.add(dceCheck);

        // define double click as enter
        JPanel mwp = new JPanel();
        mwp.setBorder(BorderFactory.createTitledBorder(LangTool.getString("sa.mouseWheel")));

        mwCheck = new JCheckBox(LangTool.getString("sa.activateMW"));

        // check if mouse wheel active
        mwCheck.setSelected(getStringProperty("mouseWheel").equals("Yes"));

        mwp.add(mwCheck);

        contentPane.add(dcep);
        contentPane.add(mwp);

    }

    public void applyAttributes() {

        //  double click enter
        if (dceCheck.isSelected()) {
            changes.firePropertyChange(this, "doubleClick",
                    getStringProperty("doubleClick"),
                    "Yes");
            setProperty("doubleClick", "Yes");
        } else {
            changes.firePropertyChange(this, "doubleClick",
                    getStringProperty("doubleClick"),
                    "No");
            setProperty("doubleClick", "No");
        }

        if (mwCheck.isSelected()) {
            changes.firePropertyChange(this, "mouseWheel",
                    getStringProperty("mouseWheel"),
                    "Yes");
            setProperty("mouseWheel", "Yes");
        } else {
            changes.firePropertyChange(this, "mouseWheel",
                    getStringProperty("mouseWheel"),
                    "No");
            setProperty("mouseWheel", "No");
        }

    }
}
