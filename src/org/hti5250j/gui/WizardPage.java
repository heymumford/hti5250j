/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.BoxLayout;
import javax.swing.Box;

import org.hti5250j.tools.LangTool;

/**
 * Class to create and manage a Wizard style framework for you.
 */
public class WizardPage extends JPanel {

    private static final long serialVersionUID = 1L;
    public static final int NO_BUTTONS = 0x00;
    public static final int PREVIOUS = 0x01;
    public static final int NEXT = 0x02;
    public static final int FINISH = 0x04;
    public static final int CANCEL = 0x08;
    public static final int HELP = 0x10;
    public static final int ALL = PREVIOUS | NEXT | FINISH | CANCEL | HELP;

    protected JButton previousButton;
    protected JButton nextButton;
    protected JButton finishButton;
    protected JButton cancelButton;
    protected JButton helpButton;

    private Action nextAction;
    private Action previousAction;
    private Action finishAction;
    private Action cancelAction;
    private Action helpAction;

    protected static final int GROUP_SPACING = 10;
    protected static final int MARGIN = 10;
    protected static final int BUTTON_SPACING = 5;

    protected JPanel buttonPanel;
    protected JSeparator separator;
    protected Container contentPane;

    public WizardPage() {
        this(ALL);
    }

    public WizardPage(int button_flags) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        contentPane = new JPanel();

        add(contentPane);
        add(Box.createGlue());

        JSeparator js = new JSeparator();
        js.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        add(js);
        add(Box.createRigidArea(new Dimension(10, 10)));

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setName("buttonPanel");
        buttonPanel.add(Box.createHorizontalGlue());
        add(buttonPanel);

        if (button_flags == 0) {
            return;
        }

        if ((button_flags & PREVIOUS) != 0) {
            previousAction = new AbstractAction(LangTool.getString("wiz.previous")) {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                }
            };

            previousButton = new JButton(previousAction);
            buttonPanel.add(Box.createRigidArea(new Dimension(GROUP_SPACING, 0)));
            buttonPanel.add(previousButton);
        }

        if ((button_flags & NEXT) != 0) {
            nextAction = new AbstractAction(LangTool.getString("wiz.next")) {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                }
            };

            nextButton = new JButton(nextAction);
            buttonPanel.add(Box.createRigidArea(new Dimension(BUTTON_SPACING, 0)));
            buttonPanel.add(nextButton);
        }

        if ((button_flags & FINISH) != 0) {
            finishAction = new AbstractAction(LangTool.getString("wiz.finish")) {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                }
            };
            finishButton = new JButton(finishAction);
            buttonPanel.add(Box.createRigidArea(new Dimension(BUTTON_SPACING, 0)));
            buttonPanel.add(finishButton);
        }

        if ((button_flags & CANCEL) != 0) {
            cancelAction = new AbstractAction(LangTool.getString("wiz.cancel")) {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                }
            };

            cancelButton = new JButton(cancelAction);
            buttonPanel.add(Box.createRigidArea(new Dimension(GROUP_SPACING, 0)));
            buttonPanel.add(cancelButton);
            buttonPanel.add(Box.createRigidArea(new Dimension(MARGIN, 0)));
        }
        if ((button_flags & HELP) != 0) {
            helpAction = new AbstractAction(LangTool.getString("wiz.help")) {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
                }
            };

            helpButton = new JButton(helpAction);
        }
    }

    public JButton getNextButton() {
        return nextButton;
    }

    public JButton getPreviousButton() {
        return previousButton;
    }

    public JButton getFinishButton() {
        return finishButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getHelpButton() {
        return helpButton;
    }

    public void setContentPane(Container new_pane) {
        if (new_pane == null) {
            throw new NullPointerException("content pane must be non-null");
        }
        removeAll();
        contentPane = new_pane;
        add(contentPane);
        add(new JSeparator());
        add(buttonPanel);
    }

    /**
     * Overrides normal getContentPane to provide specially
     * managed area
     */
    public Container getContentPane() {
        return contentPane;
    }

}
