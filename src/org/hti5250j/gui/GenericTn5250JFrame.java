/*
 * SPDX-FileCopyrightText: Copyright (c) 2001 , 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.gui;

import java.awt.*;

import javax.swing.JFrame;

import org.hti5250j.tools.GUIGraphicsUtils;

/**
 * Convenient base class for all HTI5250j windows/frames.
 * Supports the standard application icon and a {@link #centerFrame()} method.
 * <br><br>
 * Direct known subclasses:
 * <ul>
 * <li>{@link org.hti5250j.interfaces.GUIViewInterface}</li>
 * <li>{@link org.hti5250j.mailtools.SendEMailDialog}</li>
 * <li>{@link org.hti5250j.spoolfile.SpoolExporter}</li>
 * <li>{@link org.hti5250j.spoolfile.SpoolExportWizard}</li>
 * <li>{@link org.hti5250j.tools.XTFRFile}</li>
 * </ul>
 */
public class GenericTn5250JFrame extends JFrame {

    private static final long serialVersionUID = 7349671770294342782L;

    protected boolean packFrame = false;

    public GenericTn5250JFrame() {
        super();
        java.util.List<Image> icons = GUIGraphicsUtils.getApplicationIcons();
        setIconImages(icons);
        new AppleApplicationTools().tryToSetDockIconImages(icons);
    }

    public void centerFrame() {

        if (packFrame) {
            pack();
        } else {
            validate();
        }

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }

        setLocation((screenSize.width - frameSize.width) / 2,
                (screenSize.height - frameSize.height) / 2);


    }

}
