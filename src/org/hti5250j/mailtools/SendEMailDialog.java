/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.mailtools;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.hti5250j.SessionConfig;
import org.hti5250j.SessionPanel;
import org.hti5250j.HTI5250jConstants;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.gui.GenericTn5250JFrame;
import org.hti5250j.gui.HTI5250jFileChooser;
import org.hti5250j.tools.LangTool;
import org.hti5250j.tools.encoder.EncodeComponent;

/**
 * Send E-Mail dialog
 */
public class SendEMailDialog extends GenericTn5250JFrame implements Runnable {

    private static final long serialVersionUID = 1L;

    JComboBox toAddress;
    JTextField subject;
    JTextArea bodyText;
    JTextField attachmentName;
    SessionConfig config;
    SessionPanel session;
    String fileName;
    JRadioButton text;
    JRadioButton graphic;
    GridBagConstraints gbc;
    JRadioButton normal;
    JRadioButton screenshot;
    JButton browse;
    boolean sendScreen;
    SendEMail sendEMail;
    Thread myThread = new Thread(this);

    /**
     * Constructor to send the screen information
     *
     * @param parent
     * @param session
     * @param sendScreen
     */
    public SendEMailDialog(Frame parent, SessionPanel session) {
        this(parent, session, true);
    }

    /**
     * Constructor to send the screen information
     *
     * @param parent
     * @param session
     */
    public SendEMailDialog(Frame parent, SessionPanel session, boolean sendScreen) {
        super();
        if (!isEMailAvailable()) {

            JOptionPane.showMessageDialog(
                    parent,
                    LangTool.getString("messages.noEmailAPI"),
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    null);

        } else {

            this.session = session;
            Screen5250 screen = session.getScreen();
            this.sendScreen = sendScreen;

            Object[] message = new Object[1];
            message[0] = setupMailPanel("tn5250j.txt");

            String[] options = new String[3];

            int result = 0;
            while (result == 0 || result == 2) {

                setOptions(options);

                result = JOptionPane.showOptionDialog(parent,
                        message,
                        LangTool.getString("em.title"),
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                switch (result) {
                    case 0: // Send it
                        sendEMail = new SendEMail();
                        sendEMail.setConfigFile("SMTPProperties.cfg");
                        sendEMail.setTo((String) toAddress.getSelectedItem());
                        sendEMail.setSubject(subject.getText());
                        if (bodyText.getText().length() > 0) {
                            sendEMail.setMessage(bodyText.getText());
                        }

                        if (attachmentName.getText().length() > 0) {
                            if (!normal.isSelected()) {
                                sendEMail.setAttachmentName(attachmentName.getText());
                            } else {
                                sendEMail.setAttachmentName(fileName);
                            }
                        }

                        if (text.isSelected()) {

                            char[] screenTxt;
                            char[] screenExtendedAttr;
                            char[] screenAttrPlace;

                            int len = screen.getScreenLength();
                            screenTxt = new char[len];
                            screenExtendedAttr = new char[len];
                            screenAttrPlace = new char[len];
                            screen.GetScreen(screenTxt, len, HTI5250jConstants.PLANE_TEXT);
                            screen.GetScreen(screenExtendedAttr, len, HTI5250jConstants.PLANE_EXTENDED);
                            screen.GetScreen(screenAttrPlace, len, HTI5250jConstants.PLANE_IS_ATTR_PLACE);

                            StringBuffer sb = new StringBuffer();
                            int c = screen.getColumns();
                            int l = screen.getRows() * c;

                            int col = 0;
                            for (int x = 0; x < l; x++, col++) {

                                                if (screenTxt[x] >= ' ' && ((screenExtendedAttr[x] & HTI5250jConstants.EXTENDED_5250_NON_DSP) == 0)) {

                                    if (
                                            (screenExtendedAttr[x] & HTI5250jConstants.EXTENDED_5250_UNDERLINE) != 0 &&
                                                    screenAttrPlace[x] != 1) {
                                        sb.append('_');
                                    } else {
                                        sb.append(screenTxt[x]);

                                    }

                                } else {

                                    if (
                                            (screenExtendedAttr[x] & HTI5250jConstants.EXTENDED_5250_UNDERLINE) != 0 &&
                                                    screenAttrPlace[x] != 1) {
                                        sb.append('_');
                                    } else {
                                        sb.append(' ');
                                    }
                                }

                                if (col == c) {
                                    sb.append('\n');
                                    col = 0;
                                }
                            }

                            sendEMail.setAttachment(sb.toString());
                        } else if (graphic.isSelected()) {

                            File dir = new File(System.getProperty("user.dir"));
                            String tempFile = "tn5250jTemp";

                            try {
                                File f =
                                        File.createTempFile(tempFile, ".png", dir);
                                f.deleteOnExit();

                                EncodeComponent.encode(
                                        EncodeComponent.PNG,
                                        session,
                                        f);
                                sendEMail.setFileName(f.getName());
                            } catch (Exception ex) {
                                System.out.println(ex.getMessage());
                            }

                        } else if (attachmentName.getText().length() > 0) {
                            File f = new File(attachmentName.getText());
                            sendEMail.setFileName(f.toString());
                        }

                        sendIt(parent, sendEMail);

                        break;
                    case 1: // Cancel
                        break;
                    case 2: // Configure SMTP
                        configureSMTP(parent);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Constructor to send a file
     *
     * @param parent
     * @param session
     */
    public SendEMailDialog(Frame parent, SessionPanel session, String fileName) {

        if (!isEMailAvailable()) {

            JOptionPane.showMessageDialog(
                    parent,
                    LangTool.getString("messages.noEmailAPI"),
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    null);
        } else {

            this.session = session;

            Object[] message = new Object[1];
            message[0] = setupMailPanel(fileName);
            String[] options = new String[3];

            int result = 0;
            while (result == 0 || result == 2) {

                setOptions(options);
                result = JOptionPane.showOptionDialog(parent,
                        message,
                        LangTool.getString("em.titleFileTransfer"),
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                switch (result) {
                    case 0: // Send it

                        sendEMail = new SendEMail();

                        sendEMail.setConfigFile("SMTPProperties.cfg");
                        sendEMail.setTo((String) toAddress.getSelectedItem());
                        sendEMail.setSubject(subject.getText());
                        if (bodyText.getText().length() > 0) {
                            sendEMail.setMessage(bodyText.getText());
                        }

                        if (attachmentName.getText().length() > 0) {
                            sendEMail.setAttachmentName(attachmentName.getText());
                        }

                        if (fileName != null && fileName.length() > 0) {
                            sendEMail.setFileName(fileName);
                        }

                        sendIt(parent, sendEMail);

                        break;
                    case 1: // Cancel
                        break;
                    case 2: // Configure SMTP
                        configureSMTP(parent);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Send the e-mail on its way.
     * @param sem
     */
    private void sendIt(Frame parent, SendEMail sem) {
        myThread.start();
    }

    public void setSendEMail(SendEMail sem) {
        sendEMail = sem;
    }

    public void run() {
        try {
            if (sendEMail.send()) {
                sendEMail.release();
                sendEMail = null;

                JOptionPane.showMessageDialog(
                        null,
                        LangTool.getString("em.confirmationMessage")
                                + " "
                                + (String) toAddress.getSelectedItem(),
                        LangTool.getString("em.titleConfirmation"),
                        JOptionPane.INFORMATION_MESSAGE);

                if (session != null) {
                    config.setProperty(
                            "emailTo",
                            getToTokens(
                                    config.getStringProperty("emailTo"),
                                    toAddress));
                    config.saveSessionProps();
                    setToCombo(config.getStringProperty("emailTo"), toAddress);

                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Configure the SMTP server information
     *
     * @param parent
     */
    private void configureSMTP(Frame parent) {

        if (parent == null) {
            parent = new JFrame();
        }

        SMTPConfig smtp = new SMTPConfig(parent, "", true);
        smtp.setVisible(true);
        smtp.dispose();

    }

    /**
     * Create the main e-mail panel for display
     *
     * @param fileName
     * @return
     */
    private JPanel setupMailPanel(String fileName) {

        JPanel semp = new JPanel();
        semp.setLayout(new GridBagLayout());

        text = new JRadioButton(LangTool.getString("em.text"));
        graphic = new JRadioButton(LangTool.getString("em.graphic"));
        normal = new JRadioButton(LangTool.getString("em.normalmail"), true);
        screenshot = new JRadioButton(LangTool.getString("em.screenshot"));

        ButtonGroup tGroup = new ButtonGroup();
        tGroup.add(text);
        tGroup.add(graphic);
        ButtonGroup mGroup = new ButtonGroup();
        mGroup.add(normal);
        mGroup.add(screenshot);

        text.setSelected(false);
        text.setEnabled(false);
        graphic.setEnabled(false);

        JLabel screenDump = new JLabel(LangTool.getString("em.screendump"));
        JLabel tol = new JLabel(LangTool.getString("em.to"));
        JLabel subl = new JLabel(LangTool.getString("em.subject"));
        JLabel bodyl = new JLabel(LangTool.getString("em.body"));
        JLabel fnl = new JLabel(LangTool.getString("em.fileName"));
        JLabel tom = new JLabel(LangTool.getString("em.typeofmail"));

        browse = new JButton(LangTool.getString("em.choosefile"));
        browse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                browse_actionPerformed(e);
            }
        });

        toAddress = new JComboBox();
        toAddress.setPreferredSize(new Dimension(175, 25));
        toAddress.setEditable(true);

        subject = new JTextField(30);
        bodyText = new JTextArea(6, 30);
        JScrollPane bodyScrollPane = new JScrollPane(bodyText);
        bodyScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        bodyScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        attachmentName = new JTextField(fileName, 30);
        if (fileName != null && fileName.length() > 0) {
            attachmentName.setText(fileName);
        } else {
            attachmentName.setText("");
        }

        text.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent e) {
                setAttachmentName();
            }
        });
        normal.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent e) {
                setTypeOfMail();
            }
        });

        if (sendScreen) {
            screenshot.setSelected(true);
        } else {
            normal.setSelected(true);
        }

        config = null;

        if (session != null) {
            config = session.getSession().getConfiguration();

            if (config.isPropertyExists("emailTo")) {
                setToCombo(config.getStringProperty("emailTo"), toAddress);
            }
        }

        semp.setBorder(BorderFactory.createEtchedBorder());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 5);
        semp.add(tom, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 15, 5, 5);
        semp.add(normal, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 45, 5, 10);
        semp.add(screenshot, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 10, 5, 5);
        semp.add(screenDump, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 15, 5, 5);
        semp.add(text, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 45, 5, 10);
        semp.add(graphic, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 5);
        semp.add(tol, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 10);
        semp.add(toAddress, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 5);
        semp.add(subl, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 10);
        semp.add(subject, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridheight = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 5);
        semp.add(bodyl, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.gridheight = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 10);
        semp.add(bodyScrollPane, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 5);
        semp.add(fnl, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 10);
        semp.add(attachmentName, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 10, 10);
        semp.add(browse, gbc);

        return semp;

    }

    private void browse_actionPerformed(ActionEvent e) {
        String workingDir = System.getProperty("user.dir");
        HTI5250jFileChooser pcFileChooser = new HTI5250jFileChooser(workingDir);

        int ret = pcFileChooser.showOpenDialog(new JFrame());

        if (ret == HTI5250jFileChooser.APPROVE_OPTION) {
            File file = pcFileChooser.getSelectedFile();
            fileName = file.getName();
            attachmentName.setText(file.toString());
        }
    }

    private void setAttachmentName() {

        if (text.isSelected()) {
            attachmentName.setText("tn5250j.txt");

        } else if (normal.isSelected()) {
            attachmentName.setText("tn5250j.png");
        } else {
            attachmentName.setText("tn5250j.png");
        }
    }

    private void setTypeOfMail() {

        if (normal.isSelected()) {
            text.setEnabled(false);
            graphic.setEnabled(false);
            attachmentName.setText("");
            browse.setEnabled(true);
        } else {
            text.setEnabled(true);
            graphic.setEnabled(true);
            text.setSelected(true);
            setAttachmentName();
            browse.setEnabled(false);
        }
    }

    private void setOptions(String[] options) {

        options[0] = LangTool.getString("em.optSendLabel");
        options[1] = LangTool.getString("em.optCancelLabel");

        File smtp = new File("SMTPProperties.cfg");

        if (smtp.exists()) {
            options[2] = LangTool.getString("em.optEditLabel");
        } else {
            options[2] = LangTool.getString("em.optConfigureLabel");
        }

    }

    /**
     * Set the combo box items to the string token from to.
     * The separator is a '|' character.
     *
     * @param to
     * @param boxen
     */
    private void setToCombo(String to, JComboBox boxen) {

        StringTokenizer tokenizer = new StringTokenizer(to, "|");

        boxen.removeAllItems();

        while (tokenizer.hasMoreTokens()) {
            boxen.addItem(tokenizer.nextToken());
        }
    }

    /**
     * Creates string of tokens from the combobox items.
     * The separator is a '|' character.  It does not save duplicate items.
     *
     * @param to
     * @param boxen
     * @return
     */
    private String getToTokens(String to, JComboBox boxen) {

        StringBuffer sb = new StringBuffer();
        String selected = (String) boxen.getSelectedItem();

        sb.append(selected + '|');

        int c = boxen.getItemCount();

        for (int x = 0; x < c; x++) {
            if (!selected.equals(boxen.getItemAt(x))) {
                sb.append((String) boxen.getItemAt(x) + '|');
            }
        }
        return sb.toString();
    }

    /**
     * Checks to make sure that the e-mail api's are available
     *
     * @return whether or not the e-mail api's are available or not.
     */
    private boolean isEMailAvailable() {

        try {
            Class.forName("javax.mail.Message");
            return true;
        } catch (Exception ex) {
            System.out.println(" not there " + ex.getMessage());
            return false;
        }

    }

}
