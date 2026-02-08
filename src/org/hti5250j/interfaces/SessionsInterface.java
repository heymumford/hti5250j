/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.interfaces;


import org.hti5250j.*;

public interface SessionsInterface {

    public abstract int getCount();

    public abstract Session5250 item(int index);

    public abstract Session5250 item(String sessionName);

    public abstract void refresh();

}
