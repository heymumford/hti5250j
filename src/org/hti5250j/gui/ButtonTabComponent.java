/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicButtonUI;

import org.hti5250j.HTI5250jConstants;
import org.hti5250j.event.SessionChangeEvent;
import org.hti5250j.event.SessionListener;
import org.hti5250j.event.TabClosedListener;
import org.hti5250j.tools.LangTool;

/**
 * Component to be used as tabComponent; Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to.<br>
 * <br>
 * This class based on the ButtonTabComponent example from
 * Sun Microsystems, Inc. and was modified to use tool tips, other layout and stuff.
 */
public final class ButtonTabComponent extends JPanel implements SessionListener {

    private static final long serialVersionUID = 1L;

    private final JTabbedPane pane;
    private List<TabClosedListener> closeListeners;
    private final JLabel label;

    public ButtonTabComponent(final JTabbedPane pane) {
        super(new BorderLayout(0, 0));
        if (pane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.pane = pane;
        setOpaque(false);

        this.label = new TabLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        JButton button = new TabButton();
        button.setHorizontalAlignment(SwingConstants.TRAILING);
        add(button, BorderLayout.EAST);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        // Trigger repaint when tab title text changes so size is recalculated
        pane.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("indexForTitle".equals(evt.getPropertyName())) {
                    label.revalidate();
                    label.repaint();
                }
            }
        });
    }

    @Override
    public void onSessionChanged(SessionChangeEvent changeEvent) {
        if (changeEvent.getState() == HTI5250jConstants.STATE_CONNECTED) {
            this.label.setEnabled(true);
            this.label.setToolTipText(LangTool.getString("ss.state.connected"));
            this.setToolTipText(LangTool.getString("ss.state.connected"));
        } else {
            this.label.setEnabled(false);
            this.label.setToolTipText(LangTool.getString("ss.state.disconnected"));
            this.setToolTipText(LangTool.getString("ss.state.disconnected"));
        }
    }

    /**
     * Add a TabClosedListener to the listener list.
     *
     * @param listener The TabClosedListener to be added
     */
    public synchronized void addTabCloseListener(TabClosedListener listener) {
        if (closeListeners == null) {
            closeListeners = new ArrayList<TabClosedListener>(3);
        }
        closeListeners.add(listener);
    }

    /**
     * Remove a TabClosedListener from the listener list.
     *
     * @param listener The TabClosedListener to be removed
     */
    public synchronized void removeTabCloseListener(TabClosedListener listener) {
        if (closeListeners == null) {
            return;
        }
        closeListeners.remove(listener);
    }

    /**
     * Notify all the tab listeners that this specific tab was selected to close.
     *
     * @param tabToClose
     */
    protected void fireTabClosed(int tabToClose) {
        if (closeListeners != null) {
            int size = closeListeners.size();
            for (int i = 0; i < size; i++) {
                TabClosedListener target = closeListeners.get(i);
                target.onTabClosed(tabToClose);
            }
        }
    }

    /**
     * Label delegating icon and text to the corresponding tab.
     * Implementing MouseListener is a workaround, cause when applying
     * a tool tip to the JLabel, clicking the tabs doesn't work
     * (tested on Tn5250j0.6.2; WinXP+WinVista+Win7; JRE 1.6.0_20+).
     */
    private final class TabLabel extends JLabel implements MouseListener {
        private static final long serialVersionUID = 1L;

        TabLabel() {
            addMouseListener(this);
        }

        public String getText() {
            final int i = pane.indexOfTabComponent(ButtonTabComponent.this);
            if (i != -1) {
                return pane.getTitleAt(i);
            }
            return null;
        }

        public Icon getIcon() {
            final int i = pane.indexOfTabComponent(ButtonTabComponent.this);
            if (i != -1) {
                return pane.getIconAt(i);
            }
            return null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            actionSelectTab();
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            actionSelectTab();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        private void actionSelectTab() {
            final int i = pane.indexOfTabComponent(ButtonTabComponent.this);
            if (i != -1) {
                pane.setSelectedIndex(i);
            }
        }

    }

    /**
     * Special button displaying an X as close icon.
     */
    private final class TabButton extends JButton implements ActionListener {
        private static final long serialVersionUID = 1L;

        TabButton() {
            int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText(LangTool.getString("popup.close"));
            setUI(new BasicButtonUI());
            setContentAreaFilled(false);
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            int i = pane.indexOfTabComponent(ButtonTabComponent.this);
            if (i != -1) {
                fireTabClosed(i);
            }
        }

        public void updateUI() {

        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            }
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }

    private static final MouseListener buttonMouseListener = new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };

}
