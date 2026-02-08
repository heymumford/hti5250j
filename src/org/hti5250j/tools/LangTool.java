/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.tools;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class LangTool {

    private static Locale locale;
    private static ResourceBundle labels = null;

    public static void init() {
        if (labels != null)
            return;

        locale = Locale.getDefault();
        init("tn5250jMsgs");
    }

    public static void init(Locale l) {
        if (labels != null)
            return;

        locale = l;
        init("tn5250jMsgs");
    }

    public static void init(String initMsgFile) {
        if (labels != null)
            return;

        try {
            labels = ResourceBundle.getBundle(initMsgFile, locale);
        } catch (MissingResourceException mre) {
            System.out.println(mre.getLocalizedMessage());
        }
    }

    public static String getString(String key) {
        try {
            return labels.getString(key);
        } catch (MissingResourceException mre) {
            System.out.println(mre.getLocalizedMessage());
            return key;
        }
    }

    public static String getString(String key, String defaultString) {
        try {
            return labels.getString(key);
        } catch (MissingResourceException mre) {
            return defaultString;
        }
    }

    public static String messageFormat(String key, Object[] args) {
        return MessageFormat.format(getString(key), args);
    }

}
