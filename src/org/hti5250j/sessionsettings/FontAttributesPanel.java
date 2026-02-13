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
import javax.swing.border.TitledBorder;
import java.awt.*;

class FontAttributesPanel extends AttributesPanel {

    private static final long serialVersionUID = 1L;
    private JComboBox fontsList;
    private JCheckBox useAntialias;
    private JTextField verticalScale;
    private JTextField horizontalScale;
    private JTextField pointSize;

    FontAttributesPanel(SessionConfig config) {
        super(config, "Fonts");
    }

    public void initPanel() throws Exception {

        setLayout(new BorderLayout());
        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        add(contentPane, BorderLayout.NORTH);

        // fonts
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

        JPanel flp = new JPanel();
        TitledBorder tb = BorderFactory.createTitledBorder(LangTool.getString("sa.font"));
        flp.setBorder(tb);


        fontsList = new JComboBox();

        String font = getStringProperty("font");

        for (int x = 0; x < fonts.length; x++) {
            if (fonts[x].getFontName().indexOf('.') < 0) {
                fontsList.addItem(fonts[x].getFontName());
            }
        }

        fontsList.setSelectedItem(font);

        useAntialias = new JCheckBox(LangTool.getString("sa.useAntialias"));
        useAntialias.setSelected(getBooleanProperty("useAntialias", true));

        flp.add(fontsList);
        flp.add(useAntialias);

        JPanel fsp = new JPanel();
        fsp.setLayout(new AlignLayout(2, 5, 5));
        tb = BorderFactory.createTitledBorder(LangTool.getString("sa.scaleLabel"));
        fsp.setBorder(tb);


        verticalScale = new JTextField("1.2", 5);
        horizontalScale = new JTextField("1.0", 5);
        pointSize = new JTextField("0", 5);
        if (getStringProperty("fontScaleWidth").length() != 0) {
            horizontalScale.setText(getStringProperty("fontScaleWidth"));
        }
        if (getStringProperty("fontScaleHeight").length() != 0) {
            verticalScale.setText(getStringProperty("fontScaleHeight"));
        }
        if (getStringProperty("fontPointSize").length() != 0) {
            pointSize.setText(getStringProperty("fontPointSize"));
        }
        fsp.add(new JLabel(LangTool.getString("sa.fixedPointSize")));
        fsp.add(pointSize);
        fsp.add(new JLabel(LangTool.getString("sa.horScaleLabel")));
        fsp.add(horizontalScale);
        fsp.add(new JLabel(LangTool.getString("sa.vertScaleLabel")));
        fsp.add(verticalScale);

        contentPane.add(flp);
        contentPane.add(fsp);

    }

    public void applyAttributes() {

        if (!getStringProperty("font").equals(
                fontsList.getSelectedItem())
        ) {
            changes.firePropertyChange(this, "font",
                    getStringProperty("font"),
                    fontsList.getSelectedItem());

            setProperty("font", (String) fontsList.getSelectedItem());
        }

        if (useAntialias.isSelected()) {
            changes.firePropertyChange(this, "useAntialias",
                    getStringProperty("useAntialias"),
                    "Yes");
            setProperty("useAntialias", "Yes");
        } else {

            changes.firePropertyChange(this, "useAntialias",
                    getStringProperty("useAntialias"),
                    "No");
            setProperty("useAntialias", "No");

        }

        changes.firePropertyChange(this, "fontScaleHeight",
                getStringProperty("fontScaleHeight"),
                verticalScale.getText());
        setProperty("fontScaleHeight", verticalScale.getText());

        changes.firePropertyChange(this, "fontScaleWidth",
                getStringProperty("fontScaleWidth"),
                horizontalScale.getText());
        setProperty("fontScaleWidth", horizontalScale.getText());

        changes.firePropertyChange(this, "fontPointSize",
                getStringProperty("fontPointSize"),
                pointSize.getText());
        setProperty("fontPointSize", pointSize.getText());
    }
}
