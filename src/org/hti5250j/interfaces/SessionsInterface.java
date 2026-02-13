/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.interfaces;


import org.hti5250j.*;

public interface SessionsInterface {

    int getCount();

    Session5250 item(int index);

    Session5250 item(String sessionName);

    void refresh();

}
