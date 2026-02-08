/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.gui;

import javax.swing.JFileChooser;
import javax.swing.UIManager;

import org.hti5250j.tools.LangTool;

import java.awt.Dimension;

/**
 * Custom JFileChooser class to work around bug 4416982 on some versions of the
 * JDK/JRE
 */
public class HTI5250jFileChooser extends JFileChooser {

    private static final long serialVersionUID = 1L;

    static {
        doTranslation();
    }

    public HTI5250jFileChooser(String dir) {
        super(dir);
    }

    /**
     * This is to fix
     * Bug Id - 4416982
     * Synopsis JFileChooser does not use its resources to size itself initially
     **/

    public Dimension getPreferredSize() {
        return getLayout().preferredLayoutSize(this);
    }

    /* This method is included because Sun does not supports translations
     * for various languages at this time, for example dutch and russian
     * are not included yet. So until Sun fixes this we need to use this
     * self-made method (doTranslation) to translate the JFileChoosers.
     */

    static void doTranslation() {
        UIManager.put("FileChooser.lookInLabelText",
                LangTool.getString("jfc.Lookin") + ":");
        UIManager.put("FileChooser.upFolderToolTipText",
                LangTool.getString("jfc.UpOneLevel"));
        UIManager.put("FileChooser.newFolderToolTipText",
                LangTool.getString("jfc.CreateNewFolder"));
        UIManager.put("FileChooser.listViewButtonToolTipText",
                LangTool.getString("jfc.List"));
        UIManager.put("FileChooser.detailsViewButtonToolTipText",
                LangTool.getString("jfc.Details"));
        UIManager.put("FileChooser.fileNameLabelText",
                LangTool.getString("jfc.FileName") + ":");
        UIManager.put("FileChooser.filesOfTypeLabelText",
                LangTool.getString("jfc.FilesOfType") + ":");
        UIManager.put("FileChooser.openButtonText",
                LangTool.getString("jfc.Open"));
        UIManager.put("FileChooser.openButtonToolTipText",
                LangTool.getString("jfc.OpenSelectedFile"));
        UIManager.put("FileChooser.cancelButtonText",
                LangTool.getString("jfc.Cancel"));
        UIManager.put("FileChooser.cancelButtonToolTipText",
                LangTool.getString("jfc.Cancel"));
        UIManager.put("FileChooser.saveInLabelText",
                LangTool.getString("jfc.Savein") + ":");
        UIManager.put("FileChooser.saveButtonText",
                LangTool.getString("jfc.Save"));
        UIManager.put("FileChooser.saveButtonToolTipText",
                LangTool.getString("jfc.SaveSelectedFile"));
    }
}
