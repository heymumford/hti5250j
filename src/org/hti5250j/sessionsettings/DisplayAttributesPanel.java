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

import static org.hti5250j.sessionsettings.ColumnSeparator.*;

class DisplayAttributesPanel extends AttributesPanel {

    private static final String NO = "No";
    private static final String YES = "Yes";

    private static final long serialVersionUID = 1L;
    private JRadioButton csHide;
    private JRadioButton csLine;
    private JRadioButton csShortLine;
    private JRadioButton saNormal;
    private JCheckBox guiCheck;
    private JCheckBox guiShowUnderline;

    DisplayAttributesPanel(SessionConfig config) {
        super(config, "Display");
    }

    @Override
    public void initPanel() throws Exception {

        setLayout(new BorderLayout());
        contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        add(contentPane, BorderLayout.NORTH);

        // define column separator panel
        JPanel csp = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        TitledBorder tb =
                BorderFactory.createTitledBorder(LangTool.getString("sa.cs"));
        csp.setBorder(tb);

        csHide = new JRadioButton(LangTool.getString("sa.csHide"));
        csHide.setActionCommand("Hide");
        csLine = new JRadioButton(LangTool.getString("sa.csLine"));
        csLine.setActionCommand("Line");
        JRadioButton csDot = new JRadioButton(LangTool.getString("sa.csDot"));
        csDot.setActionCommand("Dot");
        csShortLine = new JRadioButton(LangTool.getString("sa.csShortLine"));
        csShortLine.setActionCommand("ShortLine");

        // Group the radio buttons.
        ButtonGroup csGroup = new ButtonGroup();
        csGroup.add(csHide);
        csGroup.add(csLine);
        csGroup.add(csDot);
        csGroup.add(csShortLine);

        csLine.setSelected(Line == ColumnSeparator.getFromName(getStringProperty("colSeparator")));
        csDot.setSelected(Dot == ColumnSeparator.getFromName(getStringProperty("colSeparator")));
        csShortLine.setSelected(ShortLine == ColumnSeparator.getFromName(getStringProperty("colSeparator")));
        csHide.setSelected(Hide == ColumnSeparator.getFromName(getStringProperty("colSeparator")));

        csp.add(csHide);
        csp.add(csLine);
        csp.add(csDot);
        csp.add(csShortLine);

        // define show attributs panel
        JPanel sap = new JPanel();
        sap.setBorder(
                BorderFactory.createTitledBorder(
                        LangTool.getString("sa.showAttr")));

        saNormal = new JRadioButton(LangTool.getString("sa.showNormal"));
        saNormal.setActionCommand("Normal");
        JRadioButton saHex = new JRadioButton(LangTool.getString("sa.showHex"));
        saHex.setActionCommand("Hex");

        // Group the radio buttons.
        ButtonGroup saGroup = new ButtonGroup();
        saGroup.add(saNormal);
        saGroup.add(saHex);

        if (getStringProperty("showAttr").equals("Hex")) {
            saHex.setSelected(true);
        } else {
            saNormal.setSelected(true);
        }

        sap.add(saNormal);
        sap.add(saHex);

        // define gui panel
        JPanel cgp = new JPanel();
        cgp.setBorder(BorderFactory.createTitledBorder(LangTool.getString("sa.cgp")));
        cgp.setLayout(new AlignLayout(1, 5, 5));

        guiCheck = new JCheckBox(LangTool.getString("sa.guiCheck"));
        guiCheck.setSelected(YES.equals(getStringProperty("guiInterface")));
        cgp.add(guiCheck);

        guiShowUnderline = new JCheckBox(LangTool.getString("sa.guiShowUnderline"));
        guiShowUnderline.setSelected(YES.equals(getStringProperty("guiShowUnderline")));
        cgp.add(guiShowUnderline);

        contentPane.add(csp);
        contentPane.add(sap);
        contentPane.add(cgp);

    }

    @Override
    public void applyAttributes() {

        if (csHide.isSelected()) {
            changes.firePropertyChange(this, "colSeparator",
                    getStringProperty("colSeparator"), "Hide");
            setProperty("colSeparator", "Hide");
        } else if (csLine.isSelected()) {
            changes.firePropertyChange(this, "colSeparator",
                    getStringProperty("colSeparator"), "Line");
            setProperty("colSeparator", "Line");
        } else if (csShortLine.isSelected()) {
            changes.firePropertyChange(this, "colSeparator",
                    getStringProperty("colSeparator"), "ShortLine");
            setProperty("colSeparator", "ShortLine");
        } else {
            changes.firePropertyChange(this, "colSeparator",
                    getStringProperty("colSeparator"), "Dot");
            setProperty("colSeparator", "Dot");
        }

        if (saNormal.isSelected()) {
            changes.firePropertyChange(
                    this,
                    "showAttr",
                    getStringProperty("showAttr"),
                    "Normal");
            setProperty("showAttr", "Normal");
        } else {
            changes.firePropertyChange(
                    this,
                    "showAttr",
                    getStringProperty("showAttr"),
                    "Hex");
            setProperty("showAttr", "Hex");

        }

        if (guiCheck.isSelected()) {
            changes.firePropertyChange(
                    this,
                    "guiInterface",
                    getStringProperty("guiInterface"),
                    YES);
            setProperty("guiInterface", YES);
        } else {
            changes.firePropertyChange(
                    this,
                    "guiInterface",
                    getStringProperty("guiInterface"),
                    NO);
            setProperty("guiInterface", NO);
        }

        if (guiShowUnderline.isSelected()) {
            changes.firePropertyChange(
                    this, "guiShowUnderline",
                    getStringProperty("guiShowUnderline"), YES);
            setProperty("guiShowUnderline", YES);
        } else {
            changes.firePropertyChange(
                    this, "guiShowUnderline",
                    getStringProperty("guiShowUnderline"), NO);
            setProperty("guiShowUnderline", NO);
        }

    }
}
