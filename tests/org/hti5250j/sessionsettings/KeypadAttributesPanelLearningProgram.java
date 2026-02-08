/*
 * SPDX-FileCopyrightText: Copyright (c) 2016
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Martin W. Kirst
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.sessionsettings;

import org.hti5250j.SessionConfig;
import org.hti5250j.tools.LangTool;

import javax.swing.*;
import java.awt.*;

public class KeypadAttributesPanelLearningProgram {

    public static void main(String[] args) {
        LangTool.init();

        SessionConfig config = new SessionConfig("test-configuration-resource", "test-session");
        KeypadAttributesPanel keypadAttributesPanel = new KeypadAttributesPanel(config);

        showPanel(keypadAttributesPanel);
    }

    private static void showPanel(JPanel panel) {
        JFrame frame = new JFrame("FrameDemo");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }
}
