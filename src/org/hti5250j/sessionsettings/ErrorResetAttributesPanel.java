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

class ErrorResetAttributesPanel extends AttributesPanel {

    private static final long serialVersionUID = 1L;
    private JCheckBox resetRequired;
    private JCheckBox backspaceError;

    ErrorResetAttributesPanel(SessionConfig config) {
        super(config, "ErrorReset");
    }

    /**
     * Component initialization
     */
    public void initPanel() throws Exception {

        setLayout(new BorderLayout());
        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        add(contentPane, BorderLayout.NORTH);

        // define error reset
        JPanel reset = new JPanel();
        reset.setBorder(BorderFactory.createTitledBorder(
                LangTool.getString("sa.titleErrorReset")));

        resetRequired = new JCheckBox(LangTool.getString("sa.errorReset"));

        // check if reset required is set or not
        resetRequired.setSelected(getStringProperty("resetRequired").equals("Yes"));

        reset.add(resetRequired);

        // define backspace error
        JPanel backspace = new JPanel();
        backspace.setBorder(BorderFactory.createTitledBorder(
                LangTool.getString("sa.titleBackspace")));

        backspaceError = new JCheckBox(LangTool.getString("sa.errorBackspace"));

        // check if backspace error is set or not
        backspaceError.setSelected(getStringProperty("backspaceError", "Yes").equals("Yes"));

        backspace.add(backspaceError);

        contentPane.add(reset);
        contentPane.add(backspace);

    }

    public void applyAttributes() {

        if (resetRequired.isSelected()) {
            changes.firePropertyChange(this, "resetRequired",
                    getStringProperty("resetRequired"),
                    "Yes");
            setProperty("resetRequired", "Yes");
        } else {
            changes.firePropertyChange(this, "resetRequired",
                    getStringProperty("resetRequired"),
                    "No");
            setProperty("resetRequired", "No");
        }

        if (backspaceError.isSelected()) {
            changes.firePropertyChange(this, "backspaceError",
                    getStringProperty("backspaceError"),
                    "Yes");
            setProperty("backspaceError", "Yes");
        } else {
            changes.firePropertyChange(this, "backspaceError",
                    getStringProperty("backspaceError"),
                    "No");
            setProperty("backspaceError", "No");
        }

    }
}
