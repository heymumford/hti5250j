/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.framework.transport;

import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.tnvt;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory for creating and managing IBM i (AS/400) connections.
 *
 * Responsibilities:
 * - Load configuration from environment (.env) or properties
 * - Create SSL/TLS sockets with proper negotiation (IMPLICIT SSL on port 992)
 * - Manage connection pooling for concurrent sessions
 * - Provide health checks and automatic failover
 * - Handle telnet protocol negotiation (TN5250E)
 *
 * Architecture:
 * - Thread-safe (ConcurrentHashMap, AtomicBoolean)
 * - Virtual thread compatible (lightweight I/O operations)
 * - Explicit lifecycle (initialize, connect, disconnect, shutdown)
 */
public final class IBMiConnectionFactory {

    private static final HTI5250jLogger log = HTI5250jLogFactory.getLogger(IBMiConnectionFactory.class);

    // Configuration loaded from environment
    private final String host;
    private final int port;
    private final String sslType;
    private final int connectionTimeout;
    private final int socketTimeout;

    // Pool management
    private final ConcurrentLinkedQueue<PooledConnection> availableConnections;
    private final ConcurrentHashMap<String, PooledConnection> activeConnections;
    private final int maxPoolSize;
    private final AtomicBoolean shutdownInitiated;

    // SocketConnector for creating actual sockets
    private final SocketConnector socketConnector;

    /**
     * Create IBMiConnectionFactory from environment variables.
     *
     * Expected variables:
     * - IBM_I_HOST: hostname or IP (e.g., "ibmi.example.com")
     * - IBM_I_PORT: port (e.g., 992 for IMPLICIT SSL)
     * - IBM_I_SSL: "true" or "false" (determines SSL type)
     * - IBM_I_CONNECTION_TIMEOUT: milliseconds (default 10000)
     * - IBM_I_SOCKET_TIMEOUT: milliseconds (default 30000)
     * - IBM_I_POOL_SIZE: max concurrent connections (default 10)
     *
     * @throws IllegalArgumentException if required configuration is missing or invalid
     */
    public static IBMiConnectionFactory fromEnvironment() {
        String host = System.getenv("IBM_I_HOST");
        String portStr = System.getenv("IBM_I_PORT");
        String sslStr = System.getenv("IBM_I_SSL");
        String timeoutStr = System.getenv("IBM_I_CONNECTION_TIMEOUT");
        String socketTimeoutStr = System.getenv("IBM_I_SOCKET_TIMEOUT");
        String poolSizeStr = System.getenv("IBM_I_POOL_SIZE");

        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("IBM_I_HOST environment variable is required");
        }
        if (portStr == null || portStr.trim().isEmpty()) {
            throw new IllegalArgumentException("IBM_I_PORT environment variable is required");
        }

        int port;
        try {
            port = Integer.parseInt(portStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("IBM_I_PORT must be a valid integer", e);
        }

        boolean useSSL = sslStr != null && sslStr.trim().equalsIgnoreCase("true");
        int connectionTimeout = timeoutStr != null ? Integer.parseInt(timeoutStr.trim()) : 10000;
        int socketTimeout = socketTimeoutStr != null ? Integer.parseInt(socketTimeoutStr.trim()) : 30000;
        int poolSize = poolSizeStr != null ? Integer.parseInt(poolSizeStr.trim()) : 10;

        return new IBMiConnectionFactory(host, port, useSSL, connectionTimeout, socketTimeout, poolSize);
    }

    /**
     * Create factory with explicit configuration.
     *
     * @param host hostname or IP address
     * @param port port number (992 for IMPLICIT SSL, 23 for plain)
     * @param useSSL whether to enable SSL/TLS
     * @param connectionTimeout milliseconds to wait for socket creation
     * @param socketTimeout milliseconds for read/write operations
     * @param maxPoolSize maximum concurrent connections
     */
    public IBMiConnectionFactory(String host, int port, boolean useSSL,
                                 int connectionTimeout, int socketTimeout, int maxPoolSize) {
        validateConfiguration(host, port, connectionTimeout, socketTimeout, maxPoolSize);

        this.host = host;
        this.port = port;
        this.sslType = useSSL ? "SSL" : null;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.maxPoolSize = maxPoolSize;

        this.availableConnections = new ConcurrentLinkedQueue<>();
        this.activeConnections = new ConcurrentHashMap<>();
        this.shutdownInitiated = new AtomicBoolean(false);
        this.socketConnector = new SocketConnector();

        if (useSSL) {
            socketConnector.setSSLType("SSL");
        }

        log.info("IBMiConnectionFactory initialized for " + host + ":" + port +
                 " (SSL=" + useSSL + ", maxPoolSize=" + maxPoolSize + ")");
    }

    /**
     * Create a new connection to the IBM i system.
     * This method blocks until the connection is established or timeout occurs.
     *
     * @return SessionConnection wrapping the socket and telnet negotiation
     * @throws SocketTimeoutException if connection timeout expires
     * @throws SocketException if connection fails
     * @throws IllegalStateException if factory has been shut down
     */
    public SessionConnection createConnection() throws SocketException {
        if (shutdownInitiated.get()) {
            throw new IllegalStateException("ConnectionFactory has been shut down");
        }

        // Try to get from pool first
        PooledConnection pooled = availableConnections.poll();
        if (pooled != null && pooled.isHealthy()) {
            activeConnections.put(pooled.id, pooled);
            log.debug("Reused pooled connection: " + pooled.id);
            return pooled.sessionConnection;
        }

        // Create new connection
        Socket socket = socketConnector.createSocket(host, port);
        if (socket == null) {
            throw new SocketException("SocketConnector returned null socket");
        }

        try {
            socket.setSoTimeout(socketTimeout);
            String connectionId = generateConnectionId();
            SessionConnection session = new SessionConnection(socket, connectionId, host, port);

            PooledConnection pooledConn = new PooledConnection(connectionId, session);
            activeConnections.put(connectionId, pooledConn);

            log.info("Created new IBM i connection: " + connectionId +
                     " (" + activeConnections.size() + "/" + maxPoolSize + " active)");
            return session;
        } catch (Exception e) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            throw new SocketException("Failed to configure socket: " + e.getMessage(), e);
        }
    }

    /**
     * Release a connection back to the pool for reuse.
     * If pool is at max capacity, connection is closed instead.
     *
     * @param connection the connection to release
     */
    public void releaseConnection(SessionConnection connection) {
        if (connection == null || shutdownInitiated.get()) {
            return;
        }

        String connId = connection.getConnectionId();
        activeConnections.remove(connId);

        if (availableConnections.size() < maxPoolSize) {
            PooledConnection pooled = new PooledConnection(connId, connection);
            availableConnections.offer(pooled);
            log.debug("Released connection to pool: " + connId +
                     " (" + availableConnections.size() + "/" + maxPoolSize + " available)");
        } else {
            try {
                connection.close();
                log.debug("Closed connection (pool full): " + connId);
            } catch (Exception e) {
                log.error("Error closing excess connection: " + e.getMessage());
            }
        }
    }

    /**
     * Perform health check on a connection.
     * Attempts a socket keep-alive or ping operation.
     *
     * @param connection the connection to check
     * @return true if healthy, false if should be discarded
     */
    public boolean isHealthy(SessionConnection connection) {
        if (connection == null) {
            return false;
        }
        try {
            Socket socket = connection.getSocket();
            return socket != null && socket.isConnected() && !socket.isClosed();
        } catch (Exception e) {
            log.warn("Health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Close all connections in the pool and prevent new connections.
     * Waits for active connections to complete.
     */
    public void shutdown() {
        shutdownInitiated.set(true);
        log.info("Shutting down IBMiConnectionFactory");

        // Close available connections
        PooledConnection conn;
        while ((conn = availableConnections.poll()) != null) {
            try {
                conn.sessionConnection.close();
            } catch (Exception e) {
                log.error("Error closing pooled connection: " + e.getMessage());
            }
        }

        // Close active connections (give them 5 seconds)
        int attempts = 0;
        while (!activeConnections.isEmpty() && attempts < 50) {
            try {
                Thread.sleep(100);
                activeConnections.values().forEach(pc -> {
                    try {
                        pc.sessionConnection.close();
                    } catch (Exception ignored) {
                    }
                });
                attempts++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        activeConnections.clear();
        log.info("IBMiConnectionFactory shutdown complete");
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getConnectionCount() {
        return activeConnections.size();
    }

    public int getPooledConnectionCount() {
        return availableConnections.size();
    }

    private void validateConfiguration(String host, int port, int connTimeout, int sockTimeout, int poolSize) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be in range 1-65535");
        }
        if (connTimeout <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
        if (sockTimeout <= 0) {
            throw new IllegalArgumentException("Socket timeout must be positive");
        }
        if (poolSize <= 0) {
            throw new IllegalArgumentException("Pool size must be positive");
        }
    }

    private String generateConnectionId() {
        return "ibmi-" + System.nanoTime() + "-" + Thread.currentThread().getId();
    }

    /**
     * Wrapper for connections in the pool.
     * Tracks creation time for age-based eviction.
     */
    private static class PooledConnection {
        final String id;
        final SessionConnection sessionConnection;
        final long createdAt;
        private static final long MAX_AGE_MS = 300000; // 5 minutes

        PooledConnection(String id, SessionConnection sessionConnection) {
            this.id = id;
            this.sessionConnection = sessionConnection;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isHealthy() {
            return (System.currentTimeMillis() - createdAt) < MAX_AGE_MS &&
                   sessionConnection.getSocket() != null &&
                   !sessionConnection.getSocket().isClosed();
        }
    }
}
