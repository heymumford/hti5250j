/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.interfaces;

import org.hti5250j.session.PoolExhaustedException;
import org.hti5250j.session.SessionPoolConfig;

import java.util.concurrent.TimeUnit;

/**
 * Pool contract for managing reusable HeadlessSession instances.
 * <p>
 * Supports configurable acquisition modes, validation strategies,
 * and eviction policies for concurrent session management.
 *
 * @since 1.1.0
 */
public interface HeadlessSessionPool extends AutoCloseable {

    /**
     * Configure the pool with the given settings.
     *
     * @param config pool configuration
     */
    void configure(SessionPoolConfig config);

    /**
     * Borrow a session from the pool (blocking or immediate depending on config).
     *
     * @return a validated HeadlessSession
     * @throws PoolExhaustedException if pool is exhausted and acquisition mode is IMMEDIATE
     * @throws InterruptedException if interrupted while waiting
     */
    HeadlessSession borrowSession() throws PoolExhaustedException, InterruptedException;

    /**
     * Borrow a session with an explicit timeout.
     *
     * @param timeout maximum time to wait
     * @param unit time unit
     * @return a validated HeadlessSession
     * @throws PoolExhaustedException if timeout expires without acquiring a session
     * @throws InterruptedException if interrupted while waiting
     */
    HeadlessSession borrowSession(long timeout, TimeUnit unit)
            throws PoolExhaustedException, InterruptedException;

    /**
     * Return a session to the pool.
     * <p>
     * If validation-on-return is configured and the session fails validation,
     * it will be discarded rather than returned to the idle queue.
     *
     * @param session the session to return
     */
    void returnSession(HeadlessSession session);

    /**
     * Shut down the pool, disconnecting all sessions and rejecting new borrows.
     */
    void shutdown();

    /** Number of sessions currently borrowed. */
    int getActiveCount();

    /** Number of sessions idle in the pool. */
    int getIdleCount();

    /** Total sessions managed (active + idle). */
    int getPoolSize();

    /** Whether shutdown has been called. */
    boolean isShutdown();

    /**
     * AutoCloseable support — delegates to {@link #shutdown()}.
     */
    @Override
    default void close() {
        shutdown();
    }
}
