/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j;

import org.hti5250j.keyboard.KeyMnemonic;
import org.hti5250j.keyboard.KeyMnemonicResolver;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BoxLayout.Y_AXIS;
import static javax.swing.SwingUtilities.layoutCompoundLabel;
import static org.hti5250j.tools.LangTool.getString;

class KeypadPanel extends JPanel {

    private static final long serialVersionUID = -7460283401326716314L;
    private static final int MIN_FONT_SIZE = 3;
    private static final int NO_OF_BUTTONS_PER_ROW = 15;

    private final KeyMnemonicResolver keyMnemonicResolver = new KeyMnemonicResolver();
    private final SessionConfig.SessionConfiguration configuration;

    private JButton[] buttons;

    KeypadPanel(SessionConfig.SessionConfiguration sessionConfiguration) {
        this.configuration = sessionConfiguration;
        setBorder(createEmptyBorder());
        setLayout(new BoxLayout(this, Y_AXIS));
        addComponentListener(new KeypadPanelComponentListener());
        reInitializeButtons(configuration.getKeypadMnemonics());
    }

    void reInitializeButtons(KeyMnemonic[] keyMnemonics) {
        removeAll();
        final Insets noMargin = new Insets(0, 0, 0, 0);
        final CompoundBorder minimalBorder = createCompoundBorder(createEmptyBorder(), createEmptyBorder(2, 3, 3, 3));
        buttons = new JButton[keyMnemonics.length];
        JPanel buttonPanel = null;
        for (int i = 0; i < buttons.length; i++) {
            final KeyMnemonic mnemonic = keyMnemonics[i];
            buttons[i] = createButton(mnemonic, noMargin, minimalBorder);
            if (buttonPanel == null || i % NO_OF_BUTTONS_PER_ROW == 0) {
                buttonPanel = new JPanel(new GridLayout(1, NO_OF_BUTTONS_PER_ROW, 0, 0));
                add(buttonPanel);
            }
            buttonPanel.add(buttons[i]);
        }
        addInvisibleButtonsToPreventLayout(buttonPanel);
    }

    void updateButtonFontSize(float fontSize) {
        if (0 == buttons.length) return;

        final JButton referenceButton = buttons[0];
        Font buttonFont = referenceButton.getFont();
        buttonFont = buttonFont.deriveFont(fontSize);

        FontMetrics fm = referenceButton.getFontMetrics(buttonFont);
        Rectangle viewRect = referenceButton.getVisibleRect();

        Insets insets = referenceButton.getInsets();

        // we now subtract the insets which include the border insets as well
        viewRect.x = insets.left;
        viewRect.y = insets.top;
        viewRect.width = referenceButton.getWidth() - (insets.right + viewRect.x);
        viewRect.height = referenceButton.getHeight() - (insets.bottom + viewRect.y);

        Rectangle textRect = new Rectangle();
        Rectangle iconRect = new Rectangle();

        // now compute the text that will be displayed until we run do not get
        //    elipses or we go passes the minimum of our text size that we want
        final int textIconGap = 0;
        final Icon icon = null;
        String largestText = findLargestText();
        while (layoutCompoundLabel(fm, largestText, icon,
                referenceButton.getVerticalAlignment(),
                referenceButton.getHorizontalAlignment(),
                referenceButton.getVerticalTextPosition(),
                referenceButton.getHorizontalTextPosition(),
                viewRect,
                iconRect,
                textRect,
                textIconGap).endsWith("...")
                && fontSize > (MIN_FONT_SIZE - 1)) {
            buttonFont = buttonFont.deriveFont(--fontSize);
            fm = referenceButton.getFontMetrics(buttonFont);
        }

        if (fontSize >= MIN_FONT_SIZE) {
            for (JButton button : buttons) {
                button.setFont(buttonFont);
            }
        }
    }

    private JButton createButton(KeyMnemonic mnemonic, Insets noMargin, CompoundBorder minimalBorder) {
        JButton button = new JButton();
        button.setMargin(noMargin);
        button.setBorder(minimalBorder);
        button.setText(getString("KP_" + mnemonic.name(), keyMnemonicResolver.getDescription(mnemonic)));
        button.setActionCommand(mnemonic.mnemonic);
        return button;
    }

    private void addInvisibleButtonsToPreventLayout(JPanel bottomPanel) {
        if (buttons.length > NO_OF_BUTTONS_PER_ROW && buttons.length % NO_OF_BUTTONS_PER_ROW > 0) {
            for (int i = buttons.length % NO_OF_BUTTONS_PER_ROW; i < NO_OF_BUTTONS_PER_ROW; i++) {
                JButton button = new JButton();
                button.setVisible(false);
                bottomPanel.add(button);
            }
        }
    }

    void addActionListener(ActionListener actionlistener) {
        for (JButton button : buttons) {
            button.addActionListener(actionlistener);
        }
    }

    private void maximizeButtonSize() {
        updateButtonFontSize(configuration.getKeypadFontSize());
    }

    private String findLargestText() {
        String text = "";
        for (JButton button : buttons) {
            if (button.getText().length() > text.length()) {
                text = button.getText();
            }
        }
        return text;
    }

    private class KeypadPanelComponentListener extends ComponentAdapter {
        @Override
        public void componentShown(ComponentEvent componentEvent) {
            maximizeButtonSize();
        }

        @Override
        public void componentResized(ComponentEvent componentEvent) {
            maximizeButtonSize();
        }
    }
}
