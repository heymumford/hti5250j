/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.event;

import java.util.EventListener;

public interface FTPStatusListener extends EventListener {

    public abstract void statusReceived(FTPStatusEvent statusevent);

    public abstract void commandStatusReceived(FTPStatusEvent statusevent);

    public abstract void fileInfoReceived(FTPStatusEvent statusevent);

}
