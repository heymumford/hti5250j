/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Stephen M. Kennedy
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.transport;

import java.net.Socket;

import org.hti5250j.HTI5250jConstants;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

public class SocketConnector {

    String sslType = null;

    HTI5250jLogger logger;

    /**
     * Creates a new instance that creates a plain socket by default.
     */
    public SocketConnector() {
        logger = HTI5250jLogFactory.getLogger(getClass());
    }

    /**
     * Set the type of SSL connection to use.  Specify null or an empty string
     * to use a plain socket.
     * @param type The SSL connection type
     * @see org.hti5250j.framework.transport.SSLConstants
     */
    public void setSSLType(String type) {
        sslType = type;
    }

    /**
     * Create a new client Socket to the given destination and port.  If an SSL
     * socket type has not been specified <i>(by setSSLType(String))</i>, then
     * a plain socket will be created.  Otherwise, a new SSL socket of the
     * specified type will be created.
     * @param destination
     * @param port
     * @return a new client socket, or null if
     */
    public Socket createSocket(String destination, int port) {

        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination host must be provided");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be in range 1-65535");
        }

        Socket socket = null;
        Exception ex = null;

        if (sslType == null || sslType.trim().length() == 0 ||
                sslType.toUpperCase().equals(HTI5250jConstants.SSL_TYPE_NONE)) {
            logger.info("Creating Plain Socket");
            try {
                // Use Socket Constructor!!! SocketFactory for jdk 1.4
                socket = new Socket(destination, port);
            } catch (Exception e) {
                ex = e;
            }
        } else {  //SSL SOCKET

            logger.info("Creating SSL [" + sslType + "] Socket");

            SSLInterface sslIf = null;

            String sslImplClassName =
                    "org.hti5250j.framework.transport.SSL.SSLImplementation";
            try {
                Class<?> c = Class.forName(sslImplClassName);
                sslIf = (SSLInterface) c.newInstance();
            } catch (Exception e) {
                ex = new Exception("Failed to create SSLInterface Instance. " +
                        "Message is [" + e.getMessage() + "]");
            }

            if (sslIf != null) {
                try {
                    sslIf.init(sslType);
                    socket = sslIf.createSSLSocket(destination, port);
                } catch (Exception e) {
                    ex = e;
                }
            }
        }

        if (ex != null) {
            logger.error(ex);
        }
        if (socket == null) {
            logger.warn("No socket was created");
        }
        return socket;
    }


}
