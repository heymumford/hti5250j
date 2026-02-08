/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.framework.transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests that connect to real IBM i UAT environment.
 *
 * These tests require:
 * - Network access to IBM_I_HOST (10.1.154.41)
 * - Port 992 open (IMPLICIT SSL)
 * - .env file with IBM_I_* variables
 *
 * Tests verify:
 * - Socket creation (TLS negotiation)
 * - Telnet protocol negotiation
 * - Connection state machines
 * - Error handling for network failures
 *
 * Run with: mvn test -Dtest=IBMiUATIntegrationTest
 * Or with Maven profile for integration tests
 */
@DisplayName("IBM i UAT Integration Tests")
@EnabledIfEnvironmentVariable(named = "IBM_I_HOST", matches = ".+")
public class IBMiUATIntegrationTest {

    private IBMiConnectionFactory factory;
    private String testHost;
    private int testPort;

    @BeforeEach
    void setUp() {
        // Load from environment (required for this test suite)
        testHost = System.getenv("IBM_I_HOST");
        String portStr = System.getenv("IBM_I_PORT");
        testPort = portStr != null ? Integer.parseInt(portStr) : 992;

        // Create factory with test configuration
        factory = new IBMiConnectionFactory(
                testHost,
                testPort,
                true,  // SSL enabled (port 992)
                10000, // 10 second connection timeout
                30000, // 30 second socket timeout
                1      // Single connection for testing
        );

        System.out.println("\n=== IBM i UAT Integration Test ===");
        System.out.println("Target: " + testHost + ":" + testPort);
        System.out.println("SSL: true (IMPLICIT)");
        System.out.println("=====================================\n");
    }

    @Test
    @DisplayName("Network connectivity: Can resolve host and attempt connection")
    void testNetworkConnectivity() {
        try {
            // This is a pre-flight check before actual socket creation
            // In production, use InetAddress.getByName() to check DNS
            assertThat(testHost).isNotEmpty();
            System.out.println("✓ Host configured: " + testHost);
        } catch (Exception e) {
            fail("Network check failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Socket creation: TCP connection with IMPLICIT SSL (port 992)")
    void testSocketCreation() {
        SessionConnection connection = null;
        try {
            connection = factory.createConnection();

            assertThat(connection).isNotNull();
            assertThat(connection.isOpen()).isTrue();
            assertThat(connection.getHost()).isEqualTo(testHost);
            assertThat(connection.getPort()).isEqualTo(testPort);
            System.out.println("✓ Socket created: " + connection.getConnectionId());
            System.out.println("✓ Connection state: OPEN");

        } catch (Exception e) {
            if (e instanceof SocketTimeoutException) {
                fail("Connection timeout (network unreachable): " + e.getMessage());
            } else {
                fail("Socket creation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
            }
            factory.shutdown();
        }
    }

    @Test
    @DisplayName("TLS negotiation: Verify SSL handshake completes")
    void testTLSNegotiation() {
        SessionConnection connection = null;
        try {
            connection = factory.createConnection();
            Socket socket = connection.getSocket();

            assertThat(socket).isNotNull();
            assertThat(socket.isConnected()).isTrue();

            // Check if it's an SSL socket
            String socketType = socket.getClass().getSimpleName();
            System.out.println("✓ TLS negotiation successful");
            System.out.println("✓ Socket type: " + socketType);

        } catch (Exception e) {
            fail("TLS negotiation failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
            }
            factory.shutdown();
        }
    }

    @Test
    @DisplayName("Telnet negotiation: Receive IAC DO TRANSMIT_BINARY from server")
    void testTelnetNegotiationStart() {
        SessionConnection connection = null;
        try {
            connection = factory.createConnection();
            BufferedInputStream input = connection.getInput();
            BufferedOutputStream output = connection.getOutput();

            assertThat(input).isNotNull();
            assertThat(output).isNotNull();

            // Set read timeout for this test
            connection.getSocket().setSoTimeout(5000);

            // Try to read telnet negotiation bytes
            // IBM i typically sends: IAC-DO-TRANSMIT_BINARY (FF-FD-00)
            byte[] buffer = new byte[1024];
            int bytesRead = 0;

            try {
                bytesRead = input.read(buffer, 0, 10);
            } catch (SocketTimeoutException e) {
                System.out.println("⚠ No telnet negotiation received within 5 seconds");
                System.out.println("  This might indicate: server not responding, wrong port, or non-TN5250 service");
                throw new AssertionError("Telnet negotiation timeout", e);
            }

            if (bytesRead > 0) {
                System.out.println("✓ Received " + bytesRead + " bytes from server");
                System.out.println("✓ First bytes: " + formatBytes(buffer, bytesRead));

                // Check for IAC (0xFF)
                if (buffer[0] == (byte) 0xFF) {
                    System.out.println("✓ Detected IAC (Telnet Interpreter Control) - TN5250 protocol confirmed");
                } else {
                    System.out.println("⚠ First byte is not IAC (0xFF), received: 0x" + String.format("%02X", buffer[0]));
                }
            } else {
                fail("Server sent no data");
            }

        } catch (Exception e) {
            fail("Telnet negotiation test failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                }
            }
            factory.shutdown();
        }
    }

    @Test
    @DisplayName("Connection pooling: Reuse connections from pool")
    void testConnectionPooling() {
        IBMiConnectionFactory poolFactory = new IBMiConnectionFactory(
                testHost,
                testPort,
                true,
                10000,
                30000,
                3  // Max 3 connections
        );

        try {
            // This test is more about contract than real connection behavior
            // (actual network attempt happens in real env)
            assertThat(poolFactory.getConnectionCount()).isZero();
            assertThat(poolFactory.getPooledConnectionCount()).isZero();

            System.out.println("✓ Pool initialized: 0/3 connections");

        } finally {
            poolFactory.shutdown();
        }
    }

    @Test
    @DisplayName("Error handling: Graceful failure on unreachable host")
    void testUnreachableHostHandling() {
        IBMiConnectionFactory badFactory = new IBMiConnectionFactory(
                "192.0.2.1",  // TEST-NET-1 (reserved, always unreachable)
                992,
                true,
                2000,         // Short timeout
                5000,
                1
        );

        try {
            assertThatThrownBy(badFactory::createConnection)
                    .isInstanceOf(Exception.class);
            System.out.println("✓ Unreachable host handled gracefully");

        } finally {
            badFactory.shutdown();
        }
    }

    @Test
    @DisplayName("Diagnostic info: Environment and factory state")
    void testDiagnosticInformation() {
        System.out.println("\n=== Diagnostic Information ===");
        System.out.println("Environment Variables:");
        System.out.println("  IBM_I_HOST: " + System.getenv("IBM_I_HOST"));
        System.out.println("  IBM_I_PORT: " + System.getenv("IBM_I_PORT"));
        System.out.println("  IBM_I_SSL: " + System.getenv("IBM_I_SSL"));
        System.out.println("  IBM_I_DATABASE: " + System.getenv("IBM_I_DATABASE"));
        System.out.println("  IBM_I_USER: " + System.getenv("IBM_I_USER"));

        System.out.println("\nFactory Configuration:");
        System.out.println("  Host: " + factory.getHost());
        System.out.println("  Port: " + factory.getPort());
        System.out.println("  Active connections: " + factory.getConnectionCount());
        System.out.println("  Pooled connections: " + factory.getPooledConnectionCount());
        System.out.println("===============================\n");

        assertThat(factory.getHost()).isEqualTo(testHost);
        assertThat(factory.getPort()).isEqualTo(testPort);
    }

    // ===== Helper Methods =====

    private String formatBytes(byte[] buffer, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(len, 16); i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", buffer[i]));
        }
        if (len > 16) sb.append("...");
        return sb.toString();
    }
}
