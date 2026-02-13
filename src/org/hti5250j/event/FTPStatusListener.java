/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.event;

import java.util.EventListener;

public interface FTPStatusListener extends EventListener {

    void statusReceived(FTPStatusEvent statusevent);

    void commandStatusReceived(FTPStatusEvent statusevent);

    void fileInfoReceived(FTPStatusEvent statusevent);

}
