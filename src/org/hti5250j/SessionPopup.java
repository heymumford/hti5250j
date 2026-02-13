/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.tnvt;
import org.hti5250j.gui.HexCharMapDialog;
import org.hti5250j.interfaces.OptionAccessFactory;
import org.hti5250j.keyboard.configure.KeyConfigure;
import org.hti5250j.mailtools.SendEMailDialog;
import org.hti5250j.keyboard.KeyMnemonic;
import org.hti5250j.tools.*;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;

import static org.hti5250j.keyboard.KeyMnemonic.*;

/**
 * Custom
 */
public class SessionPopup {

    private final Screen5250 screen;
    private final SessionPanel sessiongui;
    private final tnvt vt;
    private final HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    public SessionPopup(SessionPanel sessionPanel, MouseEvent mouseEvent) {

        JMenuItem menuItem;
        Action action;
        JPopupMenu popup = new JPopupMenu();
        this.sessiongui = sessionPanel;
        vt = sessiongui.getSession().getVT();
        screen = sessiongui.getScreen();

        final int pos = sessiongui.getPosFromView(mouseEvent.getX(), mouseEvent.getY());

        if (!sessiongui.rubberband.isAreaSelected() && screen.isInField(pos, false)) {
            action = new AbstractAction(LangTool.getString("popup.copy")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String fcontent = screen.copyTextField(pos);
                    StringSelection contents = new StringSelection(fcontent);
                    Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                    cb.setContents(contents, null);
                    sessiongui.getFocusForMe();
                }
            };

            popup.add(createMenuItem(action, COPY));


            action = new AbstractAction(LangTool.getString("popup.paste")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    paste(false);
                }
            };
            popup.add(createMenuItem(action, PASTE));

            action = new AbstractAction(LangTool.getString("popup.pasteSpecial")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    paste(true);
                }
            };
            popup.add(action);

            popup.addSeparator(); // ------------------

            action = new AbstractAction(LangTool.getString("popup.hexMap")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    showHexMap();
                    sessiongui.getFocusForMe();
                }
            };
            popup.add(createMenuItem(action, ""));

            popup.addSeparator(); // ------------------
        } else {

            action = new AbstractAction(LangTool.getString("popup.copy")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    sessiongui.actionCopy();
                    sessiongui.getFocusForMe();
                }
            };

            popup.add(createMenuItem(action, COPY));

            action = new AbstractAction(LangTool.getString("popup.paste")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    paste(false);
                }
            };
            popup.add(createMenuItem(action, PASTE));

            action = new AbstractAction(LangTool.getString("popup.pasteSpecial")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    paste(true);
                }
            };
            popup.add(action);

            Rectangle workRect = new Rectangle();
            if (sessiongui.rubberband.isAreaSelected()) {

                // get the bounded area of the selection
                sessiongui.getBoundingArea(workRect);

                popup.addSeparator();

                menuItem = new JMenuItem(LangTool.getString("popup.selectedColumns")
                        + " " + workRect.width);
                menuItem.setArmed(false);
                popup.add(menuItem);

                menuItem = new JMenuItem(LangTool.getString("popup.selectedRows")
                        + " " + workRect.height);
                menuItem.setArmed(false);
                popup.add(menuItem);

                JMenu sumMenu = new JMenu(LangTool.getString("popup.calc"));
                popup.add(sumMenu);

                action = new AbstractAction(LangTool.getString("popup.calcGroupCD")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        sumArea(true);
                    }
                };
                sumMenu.add(action);

                action = new AbstractAction(LangTool.getString("popup.calcGroupDC")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        sumArea(false);
                    }
                };
                sumMenu.add(action);

            }

            popup.addSeparator();

            action = new AbstractAction(LangTool.getString("popup.printScreen")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    sessiongui.printMe();
                    sessiongui.getFocusForMe();
                }
            };
            popup.add(createMenuItem(action, PRINT_SCREEN));

            popup.addSeparator();

            JMenu kbMenu = new JMenu(LangTool.getString("popup.keyboard"));

            popup.add(kbMenu);

            action = new AbstractAction(LangTool.getString("popup.mapKeys")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {

                    mapMeKeys();
                }
            };

            kbMenu.add(action);

            kbMenu.addSeparator();

            createKeyboardItem(kbMenu, ATTN);

            createKeyboardItem(kbMenu, RESET);

            createKeyboardItem(kbMenu, SYSREQ);

            if (screen.getOIA().isMessageWait() &&
                    OptionAccessFactory.getInstance().isValidOption(DISP_MESSAGES.mnemonic)) {

                action = new AbstractAction(LangTool.getString("popup.displayMessages")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        vt.systemRequest('4');
                    }
                };

                kbMenu.add(createMenuItem(action, DISP_MESSAGES));
            }

            kbMenu.addSeparator();

            createKeyboardItem(kbMenu, DUP_FIELD);

            createKeyboardItem(kbMenu, HELP);

            createKeyboardItem(kbMenu, ERASE_EOF);

            createKeyboardItem(kbMenu, FIELD_PLUS);

            createKeyboardItem(kbMenu, FIELD_MINUS);

            createKeyboardItem(kbMenu, NEW_LINE);

            if (OptionAccessFactory.getInstance().isValidOption(PRINT.mnemonic)) {
                action = new AbstractAction(LangTool.getString("popup.hostPrint")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        vt.hostPrint(1);
                    }
                };
                kbMenu.add(createMenuItem(action, PRINT));
            }

            createShortCutItems(kbMenu);

            if (screen.getOIA().isMessageWait() &&
                    OptionAccessFactory.getInstance().isValidOption(DISP_MESSAGES.mnemonic)) {

                action = new AbstractAction(LangTool.getString("popup.displayMessages")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        vt.systemRequest('4');
                    }
                };
                popup.add(createMenuItem(action, DISP_MESSAGES));
            }

            popup.addSeparator();

            action = new AbstractAction(LangTool.getString("popup.hexMap")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    showHexMap();
                    sessiongui.getFocusForMe();
                }
            };
            popup.add(createMenuItem(action, ""));

            action = new AbstractAction(LangTool.getString("popup.mapKeys")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {

                    mapMeKeys();
                    sessiongui.getFocusForMe();
                }
            };
            popup.add(createMenuItem(action, ""));

            if (OptionAccessFactory.getInstance().isValidOption(DISP_ATTRIBUTES.mnemonic)) {

                action = new AbstractAction(LangTool.getString("popup.settings")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        sessiongui.actionAttributes();
                        sessiongui.getFocusForMe();
                    }
                };
                popup.add(createMenuItem(action, DISP_ATTRIBUTES));

            }

            popup.addSeparator();

            if (sessiongui.isMacroRunning()) {
                action = new AbstractAction(LangTool.getString("popup.stopScript")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        sessiongui.setStopMacroRequested();
                    }
                };
                popup.add(action);
            } else {

                JMenu macMenu = new JMenu(LangTool.getString("popup.macros"));

                if (sessiongui.isSessionRecording()) {
                    action = new AbstractAction(LangTool.getString("popup.stop")) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            sessiongui.stopRecordingMe();
                            sessiongui.getFocusForMe();
                        }
                    };

                } else {
                    action = new AbstractAction(LangTool.getString("popup.record")) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            sessiongui.startRecordingMe();
                            sessiongui.getFocusForMe();

                        }
                    };
                }
                macMenu.add(action);
                if (Macronizer.isMacrosExist()) {
                    // this will add a sorted list of the macros to the macro menu
                    addMacros(macMenu);
                }
                popup.add(macMenu);
            }

            popup.addSeparator();

            JMenu xtfrMenu = new JMenu(LangTool.getString("popup.export"));

            if (OptionAccessFactory.getInstance().isValidOption(FILE_TRANSFER.mnemonic)) {

                action = new AbstractAction(LangTool.getString("popup.xtfrFile")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        doMeTransfer();
                        sessiongui.getFocusForMe();
                    }
                };

                xtfrMenu.add(createMenuItem(action, FILE_TRANSFER));
            }

            if (OptionAccessFactory.getInstance().isValidOption(SPOOL_FILE.mnemonic)) {

                action = new AbstractAction(LangTool.getString("popup.xtfrSpool")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        doMeSpool();
                        sessiongui.getFocusForMe();
                    }
                };

                xtfrMenu.add(action);
            }

            popup.add(xtfrMenu);

            JMenu sendMenu = new JMenu(LangTool.getString("popup.send"));
            popup.add(sendMenu);

            if (OptionAccessFactory.getInstance().isValidOption(QUICK_MAIL.mnemonic)) {

                action = new AbstractAction(LangTool.getString("popup.quickmail")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        sendQuickEMail();
                        sessiongui.getFocusForMe();
                    }
                };
                sendMenu.add(createMenuItem(action, QUICK_MAIL));
            }

            if (OptionAccessFactory.getInstance().isValidOption(E_MAIL.mnemonic)) {

                action = new AbstractAction(LangTool.getString("popup.email")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        sendScreenEMail();
                        sessiongui.getFocusForMe();
                    }
                };

                sendMenu.add(createMenuItem(action, E_MAIL));
            }

            action = new AbstractAction(LangTool.getString("popup.file")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    sendMeToFile();
                }
            };

            sendMenu.add(action);

            action = new AbstractAction(LangTool.getString("popup.toImage")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    sendMeToImageFile();
                }
            };

            sendMenu.add(action);

            popup.addSeparator();

        }

        if (OptionAccessFactory.getInstance().isValidOption(OPEN_NEW.mnemonic)) {

            action = new AbstractAction(LangTool.getString("popup.connections")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    sessiongui.startNewSession();
                }
            };

            popup.add(createMenuItem(action, OPEN_NEW));
        }

        popup.addSeparator();

        if (OptionAccessFactory.getInstance().isValidOption(TOGGLE_CONNECTION.mnemonic)) {

            if (vt.isConnected()) {
                action = new AbstractAction(LangTool.getString("popup.disconnect")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        sessiongui.toggleConnection();
                        sessiongui.getFocusForMe();
                    }
                };
            } else {

                action = new AbstractAction(LangTool.getString("popup.connect")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        sessiongui.toggleConnection();
                        sessiongui.getFocusForMe();
                    }
                };


            }

            popup.add(createMenuItem(action, TOGGLE_CONNECTION));
        }

        if (OptionAccessFactory.getInstance().isValidOption(CLOSE.mnemonic)) {

            action = new AbstractAction(LangTool.getString("popup.close")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    sessiongui.confirmCloseSession(true);
                }
            };

            popup.add(createMenuItem(action, CLOSE));

        }

        GUIGraphicsUtils.positionPopup(mouseEvent.getComponent(), popup,
                mouseEvent.getX(), mouseEvent.getY());

    }

    private void createKeyboardItem(JMenu menu, KeyMnemonic keyMnemonic) {
        createKeyboardItem(menu, keyMnemonic.mnemonic);
    }

    private void createKeyboardItem(JMenu menu, String key) {

        if (OptionAccessFactory.getInstance().isValidOption(key)) {
            final String key2 = key;
            Action action = new AbstractAction(LangTool.getString("key." + key)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    screen.sendKeys(key2);
                }
            };

            menu.add(createMenuItem(action, key));
        }

    }

    private void addMacros(JMenu menu) {

        LoadMacroMenu.loadMacros(sessiongui, menu);
    }

    private JMenuItem createMenuItem(Action action, KeyMnemonic keyMnemonic) {
        return createMenuItem(action, keyMnemonic.mnemonic);
    }

    private JMenuItem createMenuItem(Action action, String accelKey) {
        JMenuItem menuItem = new JMenuItem();
        menuItem.setAction(action);
        if (sessiongui.keyHandler.isKeyStrokeDefined(accelKey)) {
            menuItem.setAccelerator(sessiongui.keyHandler.getKeyStroke(accelKey));
        } else {
            InputMap map = sessiongui.getInputMap();
            KeyStroke[] allKeys = map.allKeys();
            for (KeyStroke keyStroke : allKeys) {
                if (map.get(keyStroke).equals(accelKey)) {
                    menuItem.setAccelerator(keyStroke);
                    break;
                }
            }
        }
        return menuItem;
    }

    private void createShortCutItems(JMenu menu) {

        JMenu shortcutMenu = new JMenu(LangTool.getString("popup.shortCuts"));
        menu.addSeparator();
        menu.add(shortcutMenu);

        InputMap map = sessiongui.getInputMap();
        KeyStroke[] allKeys = map.allKeys();
        ActionMap aMap = sessiongui.getActionMap();

        for (KeyStroke allKey : allKeys) {
            Action menuAction = aMap.get(map.get(allKey));
            JMenuItem menuItem = new JMenuItem();
            menuItem.setAction(menuAction);
            menuItem.setText(LangTool.getString("key." + map.get(allKey)));
            menuItem.setAccelerator(allKey);
            shortcutMenu.add(menuItem);
        }
    }

    private void sumArea(boolean which) {


        List<Double> sumVector = sessiongui.sumThem(which);
        Iterator<Double> sumIterator = sumVector.iterator();
        double sum = 0.0;
        double currentValue;
        while (sumIterator.hasNext()) {

            currentValue = 0.0;
            try {
                currentValue = sumIterator.next();
            } catch (Exception exception) {
                log.warn(exception);
            }

            sum += currentValue;

        }
        if (log.isDebugEnabled()) {
            log.debug("Vector sum " + sum);
        }

        // obtain the decimal format for parsing
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();

        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();

        if (which) {
            dfs.setDecimalSeparator('.');
            dfs.setGroupingSeparator(',');
        } else {
            dfs.setDecimalSeparator(',');
            dfs.setGroupingSeparator('.');
        }

        df.setDecimalFormatSymbols(dfs);
        df.setMinimumFractionDigits(6);

        JOptionPane.showMessageDialog(null,
                df.format(sum),
                LangTool.getString("popup.calc"),
                JOptionPane.INFORMATION_MESSAGE);

    }

    private void showHexMap() {
        final HexCharMapDialog dlg = new HexCharMapDialog(sessiongui, vt.getCodePage());
        String key = dlg.showModal();
        if (key != null) {
            screen.sendKeys(key);
        }
    }

    private void mapMeKeys() {

        Frame parent = (Frame) SwingUtilities.getRoot(sessiongui);

        if (Macronizer.isMacrosExist()) {
            String[] macrosList = Macronizer.getMacroList();
            new KeyConfigure(parent, macrosList, vt.getCodePage());
        } else {
            new KeyConfigure(parent, null, vt.getCodePage());
        }

    }

    private void doMeTransfer() {

        new XTFRFile((Frame) SwingUtilities.getRoot(sessiongui), vt, sessiongui);

    }

    private void doMeSpool() {

        try {
            org.hti5250j.spoolfile.SpoolExporter spooler =
                    new org.hti5250j.spoolfile.SpoolExporter(vt, sessiongui);
            spooler.setVisible(true);
        } catch (NoClassDefFoundError ncdfe) {
            JOptionPane.showMessageDialog(sessiongui,
                    LangTool.getString("messages.noAS400Toolbox"),
                    "Error",
                    JOptionPane.ERROR_MESSAGE, null);
        }

    }

    private void sendScreenEMail() {

        new SendEMailDialog((Frame) SwingUtilities.getRoot(sessiongui), sessiongui);
    }

    private void sendQuickEMail() {

        new SendEMailDialog((Frame) SwingUtilities.getRoot(sessiongui), sessiongui, false);
    }

    private void sendMeToFile() {

        SendScreenToFile.showDialog(SwingUtilities.getRoot(sessiongui), screen);
    }

    private void sendMeToImageFile() {
        new SendScreenImageToFile((Frame) SwingUtilities.getRoot(sessiongui), sessiongui);
    }

    private void paste(boolean special) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            final Transferable transferable = clipboard.getContents(this);
            if (transferable != null) {
                final String content = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                screen.pasteText(content, special);
                sessiongui.getFocusForMe();
            }
        } catch (HeadlessException headlessException) {
            log.debug("HeadlessException", headlessException);
        } catch (UnsupportedFlavorException unsupportedFlavorException) {
            log.debug("the requested data flavor is not supported", unsupportedFlavorException);
        } catch (IOException ioException) {
            log.debug("data is no longer available in the requested flavor", ioException);
        }

    }

}
