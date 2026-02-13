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

class OnConnectAttributesPanel extends AttributesPanel {

    private static final long serialVersionUID = 1L;
    private JTextField connectMacro;

    OnConnectAttributesPanel(SessionConfig config) {
        super(config, "OnConnect");
    }

    public void initPanel() throws Exception {

        setLayout(new BorderLayout());
        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        add(contentPane, BorderLayout.NORTH);

        JPanel ocMacrop = new JPanel();
        ocMacrop.setBorder(BorderFactory.createTitledBorder(LangTool.getString("sa.connectMacro")));

        connectMacro = new JTextField();
        connectMacro.setColumns(30);
        connectMacro.setText(getStringProperty("connectMacro"));

        ocMacrop.add(connectMacro);
        contentPane.add(ocMacrop);

    }

    public void applyAttributes() {

        changes.firePropertyChange(this, "connectMacro",
                getStringProperty("connectMacro"),
                connectMacro.getText());
        setProperty("connectMacro", connectMacro.getText());

    }
}
