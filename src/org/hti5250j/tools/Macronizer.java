/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.hti5250j.SessionPanel;
import org.hti5250j.interfaces.ConfigureFactory;
import org.hti5250j.scripting.InterpreterDriverManager;

public class Macronizer {

    private static Properties macros;
    private static boolean macrosExist;

    public static void init() {

        if (macros != null) {
            return;
        }

        macrosExist = loadMacros();

    }

    private static boolean loadMacros() {

        macros = ConfigureFactory.getInstance().getProperties(ConfigureFactory.MACROS);
        if (macros != null && macros.size() > 0) {
            return true;
        }

        return checkScripts();
    }

    private static void saveMacros() {

        ConfigureFactory.getInstance().saveSettings(
                ConfigureFactory.MACROS, "------ Macros --------");
    }

    public static final boolean isMacrosExist() {
        return macrosExist;
    }

    public static final int getNumOfMacros() {

        return macros.size();

    }

    public static final String[] getMacroList() {

        String[] macroList = new String[macros.size()];
        Set<Object> macroSet = macros.keySet();
        Iterator<Object> macroIterator = macroSet.iterator();
        String byName = null;
        int index = 0;
        while (macroIterator.hasNext()) {
            byName = (String) macroIterator.next();
            int period = byName.indexOf(".");
            macroList[index++] = byName.substring(period + 1);
        }

        return macroList;
    }

    public static final String getMacroByNumber(int num) {
        String mac = "macro" + num + ".";

        Set<Object> macroSet = macros.keySet();
        Iterator<Object> macroIterator = macroSet.iterator();
        String byNum = null;
        while (macroIterator.hasNext()) {
            byNum = (String) macroIterator.next();
            if (byNum.startsWith(mac)) {
                return (String) macros.get(byNum);
            }
        }
        return null;
    }

    public static final String getMacroByName(String name) {

        Set<Object> macroSet = macros.keySet();
        Iterator<Object> macroIterator = macroSet.iterator();
        String byName = null;
        while (macroIterator.hasNext()) {
            byName = (String) macroIterator.next();
            if (byName.endsWith(name)) {
                return (String) macros.get(byName);
            }
        }
        return null;
    }

    public static final void removeMacroByName(String name) {

        Set<Object> macroSet = macros.keySet();
        Iterator<Object> macroIterator = macroSet.iterator();
        String byName = null;
        while (macroIterator.hasNext()) {
            byName = (String) macroIterator.next();
            if (byName.endsWith(name)) {
                macros.remove(byName);
                saveMacros();
                return;
            }
        }
    }

    /**
     * Add the macro keystrokes to the macros list.
     *
     * This method is a destructive where if the macro already exists it will be
     *   overwritten.
     *
     * @param name
     * @param keyStrokes
     */
    public static final void setMacro(String name, String keyStrokes) {

        int index = 0;

        // first let's go through all the macros and replace the macro entry if it
        //   already exists.
        if (macrosExist && getMacroByName(name) != null) {
            Set<Object> macroSet = macros.keySet();
            Iterator<Object> macroIterator = macroSet.iterator();
            String byName = null;
            String prefix = null;
            while (macroIterator.hasNext()) {
                byName = (String) macroIterator.next();
                if (byName.endsWith(name)) {
                    //  we need to obtain the prefix so that we can replace
                    //   the slot with the new keystrokes.  If not the keymapping
                    //   will not work correctly.
                    prefix = byName.substring(0, byName.indexOf(name));
                    macros.put(prefix + name, keyStrokes);
                }
            }
        } else {
            // If it did not exist and get replaced then we need to find the next
            //  available slot to place the macro in.
            while (getMacroByNumber(++index) != null) {
                // no-op
            }

            macros.put("macro" + index + "." + name, keyStrokes);
            macrosExist = true;
        }
        saveMacros();

    }

    public static void showRunScriptDialog(SessionPanel session) {

        JPanel rsp = new JPanel();
        rsp.setLayout(new BorderLayout());
        JLabel jl = new JLabel("Enter script to run");
        final JTextField rst = new JTextField();
        rsp.add(jl, BorderLayout.NORTH);
        rsp.add(rst, BorderLayout.CENTER);
        Object[] message = new Object[1];
        message[0] = rsp;
        String[] options = {"Run", "Cancel"};

        final JOptionPane pane = new JOptionPane(
                message,                           // the dialog message array
                JOptionPane.QUESTION_MESSAGE,      // message type
                JOptionPane.DEFAULT_OPTION,        // option type
                null,                              // optional icon, use null to use the default icon
                options,                           // options string array, will be made into buttons//
                options[0]);                       // option that should be made into a default button


        final JDialog dialog = pane.createDialog(session, // parent frame
                "Run Script"  // dialog title
        );

        dialog.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent windowEvent) {
                super.windowOpened(windowEvent);

                // now we're setting the focus to the desired component
                // it's not the best solution as it depends on internals
                // of the OptionPane class, but you can use it temporarily
                // until the bug gets fixed
                // also you might want to iterate here thru the set of
                // the buttons and pick one to call requestFocus() for it

                rst.requestFocus();
            }
        });
        dialog.setVisible(true);

        Object myValue = pane.getValue();
        // If Integer, the user most likely hit escape
        if (!(myValue instanceof Integer)) {
            String value = (String) myValue;

            if (value.equals(options[0])) {
                if (rst.getText().length() > 0) {
                    invoke(rst.getText(), session);
                }
            }
        }


    }

    public static final void invoke(String macro, SessionPanel session) {

        String keys = getMacroByName(macro);
        if (keys != null) {
            session.getScreen().sendKeys(keys);
        } else {
            try {
                if (!macro.endsWith(".py")) {
                    macro = macro + ".py";
                }
                InterpreterDriverManager.executeScriptFile(session, "scripts" +
                        File.separatorChar + macro);
            } catch (Exception ex) {
                System.err.println(ex);
            }
        }
    }

    private static boolean checkScripts() {

        File directory = new File("scripts");

        File directory2 = new File(ConfigureFactory.getInstance().getProperty(
                "emulator.settingsDirectory") +
                "scripts");


        return directory.isDirectory() || directory2.isDirectory();

    }

}
