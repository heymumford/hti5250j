/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.sessionsettings;

/**
 * Types of separator line style
 *
 * @author master_jaf
 */
public enum ColumnSeparator {

    Hide, Dot, Line, ShortLine;

    /**
     * searches the enumeration for the given name, case insensitive
     *
     * @param name
     * @return the corresponding enum value OR default value, if name not matches
     */
    public static ColumnSeparator getFromName(String name) {
        ColumnSeparator result = DEFAULT;
        if (name == null) {
            return result;
        }
        for (ColumnSeparator sep : ColumnSeparator.values()) {
            if (name.equalsIgnoreCase(sep.toString())) {
                return sep;
            }
        }
        return result;
    }

    /**
     * default Line
     */
    public static ColumnSeparator DEFAULT = Hide;
}
