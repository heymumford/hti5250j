/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

import org.hti5250j.interfaces.HeadlessSessionFactory;

import java.time.Duration;
import java.util.Properties;

/**
 * Configuration for {@link DefaultHeadlessSessionPool}.
 * <p>
 * Use the {@link Builder} for construction:
 * <pre>
 * SessionPoolConfig config = SessionPoolConfig.builder()
 *     .maxSize(10)
 *     .validationStrategy(ValidationStrategy.ON_BORROW)
 *     .evictionPolicy(EvictionPolicy.IDLE_TIME)
 *     .maxIdleTime(Duration.ofMinutes(5))
 *     .sessionFactory(factory)
 *     .connectionProps(props)
 *     .build();
 * </pre>
 *
 * @since 1.1.0
 */
public final class SessionPoolConfig {

    public enum AcquisitionMode {
        /** Fail immediately if no session available. */
        IMMEDIATE,
        /** Block indefinitely until a session is available. */
        QUEUED,
        /** Block up to a configured timeout, then fail. */
        TIMEOUT_ON_FULL
    }

    public enum ValidationStrategy {
        NONE,
        ON_BORROW,
        ON_RETURN,
        PERIODIC
    }

    public enum EvictionPolicy {
        NONE,
        IDLE_TIME,
        MAX_AGE
    }

    private final int maxSize;
    private final int minIdle;
    private final AcquisitionMode acquisitionMode;
    private final ValidationStrategy validationStrategy;
    private final EvictionPolicy evictionPolicy;
    private final Duration maxIdleTime;
    private final Duration maxAge;
    private final Duration validationInterval;
    private final Duration acquisitionTimeout;
    private final HeadlessSessionFactory sessionFactory;
    private final Properties connectionProps;
    private final String configResource;

    private SessionPoolConfig(Builder builder) {
        this.maxSize = builder.maxSize;
        this.minIdle = builder.minIdle;
        this.acquisitionMode = builder.acquisitionMode;
        this.validationStrategy = builder.validationStrategy;
        this.evictionPolicy = builder.evictionPolicy;
        this.maxIdleTime = builder.maxIdleTime;
        this.maxAge = builder.maxAge;
        this.validationInterval = builder.validationInterval;
        this.acquisitionTimeout = builder.acquisitionTimeout;
        this.sessionFactory = builder.sessionFactory;
        this.connectionProps = builder.connectionProps;
        this.configResource = builder.configResource;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getMaxSize() { return maxSize; }
    public int getMinIdle() { return minIdle; }
    public AcquisitionMode getAcquisitionMode() { return acquisitionMode; }
    public ValidationStrategy getValidationStrategy() { return validationStrategy; }
    public EvictionPolicy getEvictionPolicy() { return evictionPolicy; }
    public Duration getMaxIdleTime() { return maxIdleTime; }
    public Duration getMaxAge() { return maxAge; }
    public Duration getValidationInterval() { return validationInterval; }
    public Duration getAcquisitionTimeout() { return acquisitionTimeout; }
    public HeadlessSessionFactory getSessionFactory() { return sessionFactory; }
    public Properties getConnectionProps() { return connectionProps; }
    public String getConfigResource() { return configResource; }

    public static final class Builder {
        private int maxSize = 10;
        private int minIdle = 0;
        private AcquisitionMode acquisitionMode = AcquisitionMode.IMMEDIATE;
        private ValidationStrategy validationStrategy = ValidationStrategy.NONE;
        private EvictionPolicy evictionPolicy = EvictionPolicy.NONE;
        private Duration maxIdleTime = Duration.ofMinutes(5);
        private Duration maxAge = Duration.ofMinutes(30);
        private Duration validationInterval = Duration.ofMinutes(1);
        private Duration acquisitionTimeout = Duration.ofSeconds(5);
        private HeadlessSessionFactory sessionFactory;
        private Properties connectionProps = new Properties();
        private String configResource = "TN5250JDefaults.props";

        private Builder() {}

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder minIdle(int minIdle) {
            this.minIdle = minIdle;
            return this;
        }

        public Builder acquisitionMode(AcquisitionMode acquisitionMode) {
            this.acquisitionMode = acquisitionMode;
            return this;
        }

        public Builder validationStrategy(ValidationStrategy validationStrategy) {
            this.validationStrategy = validationStrategy;
            return this;
        }

        public Builder evictionPolicy(EvictionPolicy evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
            return this;
        }

        public Builder maxIdleTime(Duration maxIdleTime) {
            if (maxIdleTime == null) throw new IllegalArgumentException("maxIdleTime must not be null");
            this.maxIdleTime = maxIdleTime;
            return this;
        }

        public Builder maxAge(Duration maxAge) {
            if (maxAge == null) throw new IllegalArgumentException("maxAge must not be null");
            this.maxAge = maxAge;
            return this;
        }

        public Builder validationInterval(Duration validationInterval) {
            if (validationInterval == null) throw new IllegalArgumentException("validationInterval must not be null");
            this.validationInterval = validationInterval;
            return this;
        }

        public Builder acquisitionTimeout(Duration acquisitionTimeout) {
            if (acquisitionTimeout == null) throw new IllegalArgumentException("acquisitionTimeout must not be null");
            this.acquisitionTimeout = acquisitionTimeout;
            return this;
        }

        public Builder sessionFactory(HeadlessSessionFactory sessionFactory) {
            this.sessionFactory = sessionFactory;
            return this;
        }

        public Builder connectionProps(Properties connectionProps) {
            this.connectionProps = connectionProps;
            return this;
        }

        public Builder configResource(String configResource) {
            this.configResource = configResource;
            return this;
        }

        public SessionPoolConfig build() {
            if (sessionFactory == null) {
                throw new IllegalStateException("sessionFactory is required");
            }
            if (maxSize < 0) {
                throw new IllegalArgumentException("maxSize must be >= 0 (0 = unlimited)");
            }
            if (minIdle < 0 || (maxSize > 0 && minIdle > maxSize)) {
                throw new IllegalArgumentException("minIdle must be >= 0 and <= maxSize");
            }
            return new SessionPoolConfig(this);
        }
    }
}
