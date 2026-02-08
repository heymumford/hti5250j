/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.gui;

import java.awt.*;
import javax.swing.*;

public class HTI5250jFontsSelection extends JComboBox {

    private static final long serialVersionUID = 1L;

    public HTI5250jFontsSelection() {
        super();
        // fonts
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

        for (int x = 0; x < fonts.length; x++) {
            if (fonts[x].getFontName().indexOf('.') < 0)
                addItem(fonts[x].getFontName());
        }

    }
}
