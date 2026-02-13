/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.framework.transport;

import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Wrapper around a Socket that provides convenient stream access
 * and connection metadata.
 *
 * Responsibilities:
 * - Provide buffered input/output streams
 * - Track connection metadata (host, port, ID)
 * - Handle graceful close
 */
public final class SessionConnection implements AutoCloseable {

    private static final HTI5250jLogger log = HTI5250jLogFactory.getLogger(SessionConnection.class);

    private final Socket socket;
    private final String connectionId;
    private final String host;
    private final int port;
    private BufferedInputStream input;
    private BufferedOutputStream output;
    private volatile boolean closed = false;

    /**
     * Create a SessionConnection wrapping a socket.
     *
     * @param socket the connected socket
     * @param connectionId unique identifier for this connection
     * @param host hostname/IP for logging
     * @param port port number for logging
     * @throws IOException if stream creation fails
     */
    public SessionConnection(Socket socket, String connectionId, String host, int port) throws IOException {
        this.socket = socket;
        this.connectionId = connectionId;
        this.host = host;
        this.port = port;

        try {
            this.input = new BufferedInputStream(socket.getInputStream(), 4096);
            this.output = new BufferedOutputStream(socket.getOutputStream(), 4096);
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public BufferedInputStream getInput() {
        return input;
    }

    public BufferedOutputStream getOutput() {
        return output;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isOpen() {
        return !closed && socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        try {
            if (output != null) {
                output.flush();
                output.close();
            }
        } catch (IOException e) {
            log.error("Error closing output stream: " + e.getMessage());
        }

        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            log.error("Error closing input stream: " + e.getMessage());
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.error("Error closing socket: " + e.getMessage());
        }

        log.debug("Closed connection: " + connectionId);
    }

    @Override
    public String toString() {
        return "SessionConnection{" + connectionId + ", " + host + ":" + port +
               (isOpen() ? " (open)" : " (closed)") + "}";
    }
}
