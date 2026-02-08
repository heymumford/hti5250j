/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.sessionsettings;

import org.hti5250j.SessionConfig;
import org.hti5250j.tools.AlignLayout;
import org.hti5250j.tools.LangTool;

import javax.swing.*;
import java.awt.*;

class HotspotAttributesPanel extends AttributesPanel {

    private static final long serialVersionUID = 1L;
    private JCheckBox hsCheck;
    private JTextField hsMore;
    private JTextField hsBottom;

    HotspotAttributesPanel(SessionConfig config) {
        super(config, "HS");
    }

    /**
     * Component initialization
     */
    public void initPanel() throws Exception {

        setLayout(new BorderLayout());
        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        add(contentPane, BorderLayout.NORTH);

        // define hsPanel panel
        JPanel hsp = new JPanel();
        hsp.setBorder(BorderFactory.createTitledBorder(LangTool.getString("sa.hsp")));
        hsCheck = new JCheckBox(LangTool.getString("sa.hsCheck"));

        if (getStringProperty("hotspots").equals("Yes"))
            hsCheck.setSelected(true);

        hsp.add(hsCheck);

        // define assignment panel
        JPanel hsap = new JPanel();
        hsap.setBorder(BorderFactory.createTitledBorder(LangTool.getString("sa.hsap")));
        hsap.setLayout(new AlignLayout(2, 5, 5));

        JLabel moreLabel = new JLabel(LangTool.getString("sa.hsMore"));
        JLabel bottomLabel = new JLabel(LangTool.getString("sa.hsBottom"));
        hsMore = new JTextField(getStringProperty("hsMore"), 20);
        hsBottom = new JTextField(getStringProperty("hsBottom"), 20);

        hsap.add(moreLabel);
        hsap.add(hsMore);
        hsap.add(bottomLabel);
        hsap.add(hsBottom);

        contentPane.add(hsp);
        contentPane.add(hsap);

    }

    public void applyAttributes() {

        if (hsCheck.isSelected()) {
            changes.firePropertyChange(this, "hotspots",
                    getStringProperty("hotspots"),
                    "Yes");
            setProperty("hotspots", "Yes");
        } else {
            changes.firePropertyChange(this, "hotspots",
                    getStringProperty("hotspots"),
                    "No");
            setProperty("hotspots", "No");
        }

        changes.firePropertyChange(this, "hsMore",
                getStringProperty("hsMore"),
                hsMore.getText());
        setProperty("hsMore", hsMore.getText());

        changes.firePropertyChange(this, "hsBottom",
                getStringProperty("hsBottom"),
                hsBottom.getText());
        setProperty("hsBottom", hsBottom.getText());

    }
}
