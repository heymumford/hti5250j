/*
 * SPDX-FileCopyrightText: Copyright (c) 2016
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.connectdialog;

/**
 * Simple data model representing rows within the {@link SessionsTableModel}.
 */
class SessionsDataModel {
    final String name;
    final String host;
    final Boolean deflt;

    SessionsDataModel(String name, String host, Boolean deflt) {
        this.name = name;
        this.host = host;
        this.deflt = deflt;
    }
}
