/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.framework.transport;


import java.net.Socket;

public interface SSLInterface {

    /**
     * Initialize the components required to create a new client socket
     * when createSSLSocket is called.
     *
     * @param type The ssl socket type (TLS recommended; SSLv2/SSLv3 are deprecated)
     * @see org.hti5250j.framework.transport.SSLConstants
     */
    void init(String sslType);

    /**
     * Create a new socket
     *
     * @param destination
     * @param port
     * @return new socket, or null if none could be created.
     */
    Socket createSSLSocket(String destination, int port);

}
