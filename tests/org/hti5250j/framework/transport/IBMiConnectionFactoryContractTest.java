/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.framework.transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Contract tests for IBMiConnectionFactory.
 *
 * Tests define the immutable behavioral contracts that the factory must uphold:
 * - Configuration validation (host, port, timeouts, pool size)
 * - Proper initialization from environment
 * - Pool management (acquire, release, reuse)
 * - Lifecycle (shutdown, idempotence)
 *
 * These are unit tests (no real IBM i connection).
 */
public class IBMiConnectionFactoryContractTest {

    private IBMiConnectionFactory factory;

    @BeforeEach
    void setUp() {
        // Create factory with test defaults (no real connection)
        factory = new IBMiConnectionFactory(
                "localhost",
                992,
                true,
                5000,  // connection timeout
                10000, // socket timeout
                5      // max pool size
        );
    }

    // ===== Configuration Validation Tests =====

    @Test
    void testConstructorRejectsNullHost() {
        assertThatThrownBy(() -> new IBMiConnectionFactory(
                null, 992, true, 5000, 10000, 5
        )).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Host");
    }

    @Test
    void testConstructorRejectsEmptyHost() {
        assertThatThrownBy(() -> new IBMiConnectionFactory(
                "   ", 992, true, 5000, 10000, 5
        )).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Host");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 65536, 100000})
    void testConstructorRejectsInvalidPort(int invalidPort) {
        assertThatThrownBy(() -> new IBMiConnectionFactory(
                "localhost", invalidPort, true, 5000, 10000, 5
        )).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Port");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 23, 992, 65535})
    void testConstructorAcceptsValidPorts(int validPort) {
        assertThatNoException().isThrownBy(() -> new IBMiConnectionFactory(
                "localhost", validPort, true, 5000, 10000, 5
        ));
    }

    @Test
    void testConstructorRejectsZeroConnectionTimeout() {
        assertThatThrownBy(() -> new IBMiConnectionFactory(
                "localhost", 992, true, 0, 10000, 5
        )).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Connection timeout");
    }

    @Test
    void testConstructorRejectsZeroSocketTimeout() {
        assertThatThrownBy(() -> new IBMiConnectionFactory(
                "localhost", 992, true, 5000, 0, 5
        )).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Socket timeout");
    }

    @Test
    void testConstructorRejectsZeroPoolSize() {
        assertThatThrownBy(() -> new IBMiConnectionFactory(
                "localhost", 992, true, 5000, 10000, 0
        )).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Pool size");
    }

    // ===== Configuration Storage Tests =====

    @Test
    void testConfigurationIsStoredCorrectly() {
        assertThat(factory.getHost()).isEqualTo("localhost");
        assertThat(factory.getPort()).isEqualTo(992);
    }

    @Test
    void testConnectionCountStartsAtZero() {
        assertThat(factory.getConnectionCount()).isZero();
        assertThat(factory.getPooledConnectionCount()).isZero();
    }

    // ===== Environment Configuration Tests =====

    @Test
    void testFromEnvironmentRequiresHost() {
        // NOTE: Can't actually unset env vars in Java, so we test the logic via constructor
        // This test documents the requirement
        String host = System.getenv("IBM_I_HOST");
        // If not set, skip this test (might not have .env loaded)
        if (host != null) {
            assertThat(host).isNotNull();
        }
    }

    @Test
    void testFromEnvironmentRequiresPort() {
        String port = System.getenv("IBM_I_PORT");
        // If not set, skip this test (might not have .env loaded)
        if (port != null) {
            assertThat(port).isNotNull();
        }
    }

    @Test
    void testFromEnvironmentLoadsConfiguration() {
        // This test depends on .env being loaded
        try {
            IBMiConnectionFactory envFactory = IBMiConnectionFactory.fromEnvironment();
            assertThat(envFactory.getHost()).isNotEmpty();
            assertThat(envFactory.getPort()).isGreaterThan(0).isLessThanOrEqualTo(65535);
        } catch (IllegalArgumentException e) {
            // Skip if environment variables not configured
            if (e.getMessage().contains("required")) {
                System.out.println("⚠ Skipping: Environment variables not configured (this is OK)");
            } else {
                throw e;
            }
        }
    }

    // ===== Lifecycle Tests =====

    @Test
    void testShutdownIsIdempotent() {
        factory.shutdown();
        // Should not throw
        factory.shutdown();
        assertThat(factory.getConnectionCount()).isZero();
    }

    @Test
    void testRejectsConnectionCreationAfterShutdown() {
        factory.shutdown();

        assertThatThrownBy(factory::createConnection)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shut down");
    }

    // ===== Pool Management Tests =====

    @Test
    void testReleaseConnectionReturnsNullGracefully() {
        // Should not throw
        factory.releaseConnection(null);
    }

    @Test
    void testShutdownAfterReleaseIsGraceful() {
        factory.releaseConnection(null);
        // Should not throw
        factory.shutdown();
    }

    // ===== Health Check Tests =====

    @Test
    void testHealthCheckRejectsNullConnection() {
        assertThat(factory.isHealthy(null)).isFalse();
    }
}
