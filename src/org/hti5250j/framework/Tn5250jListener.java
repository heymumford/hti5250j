/*
 * SPDX-FileCopyrightText: Copyright (C) 2004 Seagull Software
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: bvansomeren (bvansomeren@seagull.nl)
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */




package org.hti5250j.framework;

import java.io.File;
import java.util.Properties;

public abstract class Tn5250jListener {
    public abstract void actionPerformed(Tn5250jEvent event);

    public abstract void init(File fileDir, Properties config);

    public abstract void run();

    public abstract void destroy();

    public abstract String getName();

    public abstract void setController(Tn5250jController control);

    public abstract void sessionCreated(Tn5250jSession session);
}
