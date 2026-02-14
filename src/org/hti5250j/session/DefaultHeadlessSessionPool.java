/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

import org.hti5250j.interfaces.HeadlessSession;
import org.hti5250j.interfaces.HeadlessSessionPool;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of {@link HeadlessSessionPool}.
 * <p>
 * Uses a {@link BlockingQueue} for idle sessions, a {@link ConcurrentHashMap}
 * to track which sessions are currently borrowed, and a
 * {@link ScheduledExecutorService} for periodic validation and eviction.
 * <p>
 * All public methods are thread-safe. {@link #configure(SessionPoolConfig)}
 * must not be called concurrently with borrow or return operations.
 *
 * @since 1.1.0
 */
public class DefaultHeadlessSessionPool implements HeadlessSessionPool {

    private static final Logger LOG = Logger.getLogger(DefaultHeadlessSessionPool.class.getName());

    private volatile SessionPoolConfig config;

    // Idle sessions ready to be borrowed
    private final BlockingQueue<HeadlessSession> idleQueue = new LinkedBlockingQueue<>();

    // Currently borrowed sessions (membership set; values unused)
    private final ConcurrentHashMap<HeadlessSession, Instant> borrowedSessions = new ConcurrentHashMap<>();

    // All sessions → creation instant (idle + borrowed)
    private final ConcurrentHashMap<HeadlessSession, Instant> allSessions = new ConcurrentHashMap<>();

    // Idle sessions → time they became idle (via return or initial creation; for idle-time eviction)
    private final ConcurrentHashMap<HeadlessSession, Instant> lastReturnedTime = new ConcurrentHashMap<>();

    // Metrics
    private final AtomicInteger borrowCount = new AtomicInteger(0);
    private final AtomicInteger returnCount = new AtomicInteger(0);
    private final AtomicInteger evictionCount = new AtomicInteger(0);

    // State
    private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);
    private final ReentrantLock poolLock = new ReentrantLock();

    // Background maintenance
    private ScheduledExecutorService scheduler;

    // Session naming counter
    private final AtomicInteger sessionCounter = new AtomicInteger(0);

    public DefaultHeadlessSessionPool() {
    }

    @Override
    public void configure(SessionPoolConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;

        // Reset shutdown state so a pool can be reconfigured after shutdown
        shutdownFlag.set(false);

        // Shut down any existing scheduler and await termination
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    LOG.log(Level.WARNING,
                            "Previous scheduler did not terminate within 2s during reconfiguration");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Disconnect and clear existing sessions before reconfiguration
        if (!idleQueue.isEmpty() || !borrowedSessions.isEmpty()) {
            LOG.log(Level.WARNING, "Reconfiguring pool with {0} idle and {1} borrowed sessions; all will be disconnected",
                    new Object[]{idleQueue.size(), borrowedSessions.size()});
        }
        HeadlessSession existing;
        while ((existing = idleQueue.poll()) != null) {
            disconnectQuietly(existing);
        }
        for (HeadlessSession borrowed : borrowedSessions.keySet()) {
            disconnectQuietly(borrowed);
        }
        borrowedSessions.clear();
        allSessions.clear();
        lastReturnedTime.clear();

        // Pre-create minIdle sessions (failures are logged but do not prevent scheduler startup)
        int preCreate = config.getMinIdle();
        if (config.getMaxSize() > 0 && preCreate > config.getMaxSize()) {
            preCreate = config.getMaxSize();
        }
        for (int i = 0; i < preCreate; i++) {
            try {
                HeadlessSession session = createNewSession();
                lastReturnedTime.put(session, Instant.now());
                idleQueue.offer(session);
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE,
                        "Failed to pre-create session " + (i + 1) + " of " + preCreate
                                + " during pool configuration", e);
            }
        }

        // Start background maintenance
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-pool-maintenance");
            t.setDaemon(true);
            return t;
        });

        if (config.getEvictionPolicy() == SessionPoolConfig.EvictionPolicy.IDLE_TIME) {
            long intervalMs = config.getMaxIdleTime().toMillis() / 2;
            if (intervalMs < 100) intervalMs = 100;
            scheduler.scheduleAtFixedRate(this::evictIdleSessions, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        } else if (config.getEvictionPolicy() == SessionPoolConfig.EvictionPolicy.MAX_AGE) {
            long intervalMs = config.getMaxAge().toMillis() / 2;
            if (intervalMs < 100) intervalMs = 100;
            scheduler.scheduleAtFixedRate(this::evictAgedSessions, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        }

        if (config.getValidationStrategy() == SessionPoolConfig.ValidationStrategy.PERIODIC) {
            long intervalMs = config.getValidationInterval().toMillis();
            if (intervalMs < 100) intervalMs = 100;
            scheduler.scheduleAtFixedRate(this::validateIdleSessions, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public HeadlessSession borrowSession() throws PoolExhaustedException, InterruptedException {
        checkNotShutdown();

        switch (config.getAcquisitionMode()) {
            case IMMEDIATE:
                return borrowImmediate();
            case QUEUED:
                return borrowQueued();
            case TIMEOUT_ON_FULL:
                return borrowWithTimeout(config.getAcquisitionTimeout().toMillis(), TimeUnit.MILLISECONDS);
            default:
                throw new IllegalStateException("Unknown acquisition mode: " + config.getAcquisitionMode());
        }
    }

    @Override
    public HeadlessSession borrowSession(long timeout, TimeUnit unit)
            throws PoolExhaustedException, InterruptedException {
        checkNotShutdown();
        return borrowWithTimeout(timeout, unit);
    }

    @Override
    public void returnSession(HeadlessSession session) {
        if (session == null) return;
        if (config == null) {
            disconnectQuietly(session);
            return;
        }

        // Guard: only accept sessions that were borrowed from this pool
        if (borrowedSessions.remove(session) == null) {
            LOG.log(Level.WARNING, "Attempted to return a session not borrowed from this pool: {0}",
                    session.getSessionName());
            return;
        }

        if (shutdownFlag.get()) {
            disconnectQuietly(session);
            allSessions.remove(session);
            lastReturnedTime.remove(session);
            return;
        }

        // Validate on return
        if (config.getValidationStrategy() == SessionPoolConfig.ValidationStrategy.ON_RETURN) {
            if (!isSessionValid(session)) {
                allSessions.remove(session);
                lastReturnedTime.remove(session);
                disconnectQuietly(session);
                evictionCount.incrementAndGet();
                returnCount.incrementAndGet();
                return;
            }
        }

        lastReturnedTime.put(session, Instant.now());
        idleQueue.offer(session);
        returnCount.incrementAndGet();
    }

    @Override
    public void shutdown() {
        if (!shutdownFlag.compareAndSet(false, true)) {
            return; // already shut down
        }

        if (scheduler != null) {
            scheduler.shutdownNow();
        }

        // Disconnect all idle sessions
        HeadlessSession session;
        while ((session = idleQueue.poll()) != null) {
            disconnectQuietly(session);
            allSessions.remove(session);
            lastReturnedTime.remove(session);
        }

        // Disconnect all borrowed sessions
        for (HeadlessSession borrowed : borrowedSessions.keySet()) {
            disconnectQuietly(borrowed);
            allSessions.remove(borrowed);
        }
        borrowedSessions.clear();
        lastReturnedTime.clear();
    }

    @Override
    public int getActiveCount() {
        return borrowedSessions.size();
    }

    @Override
    public int getIdleCount() {
        return idleQueue.size();
    }

    @Override
    public int getPoolSize() {
        return allSessions.size();
    }

    @Override
    public boolean isShutdown() {
        return shutdownFlag.get();
    }

    public int getBorrowCount() { return borrowCount.get(); }
    public int getReturnCount() { return returnCount.get(); }
    public int getEvictionCount() { return evictionCount.get(); }

    // ========================================================================
    // Internal borrow strategies
    // ========================================================================

    private HeadlessSession borrowImmediate() throws PoolExhaustedException {
        HeadlessSession session = idleQueue.poll();
        if (session != null) {
            return finalizeBorrow(session);
        }

        // Atomically check capacity and create under lock
        session = tryCreateNewSession();
        if (session != null) {
            return finalizeBorrow(session);
        }

        throw new PoolExhaustedException(
                "Pool exhausted (max=" + config.getMaxSize() + ", active=" + getActiveCount() + ")");
    }

    private HeadlessSession borrowQueued() throws PoolExhaustedException, InterruptedException {
        // Try non-blocking first
        HeadlessSession session = idleQueue.poll();
        if (session != null) {
            return finalizeBorrow(session);
        }

        // Atomically check capacity and create under lock
        session = tryCreateNewSession();
        if (session != null) {
            return finalizeBorrow(session);
        }

        // Poll in a loop so we can detect shutdown rather than blocking forever
        while (!shutdownFlag.get()) {
            session = idleQueue.poll(1, TimeUnit.SECONDS);
            if (session != null) {
                return finalizeBorrow(session);
            }
        }
        throw new PoolExhaustedException("Pool has been shut down");
    }

    private HeadlessSession borrowWithTimeout(long timeout, TimeUnit unit)
            throws PoolExhaustedException, InterruptedException {
        HeadlessSession session = idleQueue.poll();
        if (session != null) {
            return finalizeBorrow(session);
        }

        session = tryCreateNewSession();
        if (session != null) {
            return finalizeBorrow(session);
        }

        session = idleQueue.poll(timeout, unit);
        if (session == null) {
            throw new PoolExhaustedException(
                    "Acquisition timeout after " + unit.toMillis(timeout) + "ms, pool full");
        }
        return finalizeBorrow(session);
    }

    private HeadlessSession finalizeBorrow(HeadlessSession session) throws PoolExhaustedException {
        // Iterative validation-on-borrow: evict invalid sessions until a valid one is found
        while (config.getValidationStrategy() == SessionPoolConfig.ValidationStrategy.ON_BORROW
                && !isSessionValid(session)) {
            allSessions.remove(session);
            lastReturnedTime.remove(session);
            disconnectQuietly(session);
            evictionCount.incrementAndGet();

            // Try to get or create a replacement
            session = idleQueue.poll();
            if (session != null) {
                continue;
            }
            session = tryCreateNewSession();
            if (session != null) {
                continue;
            }
            throw new PoolExhaustedException("No valid sessions available after validation failure");
        }

        lastReturnedTime.remove(session); // no longer idle
        borrowedSessions.put(session, allSessions.getOrDefault(session, Instant.now()));
        borrowCount.incrementAndGet();
        return session;
    }

    // ========================================================================
    // Session lifecycle
    // ========================================================================

    /**
     * Atomically checks capacity and creates a new session under the pool lock.
     * Returns null if the pool is at capacity.
     */
    private HeadlessSession tryCreateNewSession() {
        poolLock.lock();
        try {
            int max = config.getMaxSize();
            if (max > 0 && allSessions.size() >= max) {
                return null; // at capacity
            }
            return createSessionUnderLock();
        } finally {
            poolLock.unlock();
        }
    }

    /**
     * Creates a new session unconditionally (for minIdle pre-creation during configure).
     */
    private HeadlessSession createNewSession() {
        poolLock.lock();
        try {
            return createSessionUnderLock();
        } finally {
            poolLock.unlock();
        }
    }

    /** Must be called while holding poolLock. */
    private HeadlessSession createSessionUnderLock() {
        String name = "pool-session-" + sessionCounter.incrementAndGet();
        HeadlessSession session;
        try {
            session = config.getSessionFactory()
                    .createSession(name, config.getConfigResource(), config.getConnectionProps());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "SessionFactory failed to create session '" + name + "'", e);
            throw e;
        }
        if (session == null) {
            throw new IllegalStateException(
                    "SessionFactory.createSession() returned null for '" + name + "'");
        }
        allSessions.put(session, Instant.now());
        return session;
    }

    private boolean isSessionValid(HeadlessSession session) {
        try {
            return session.isConnected();
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "Session validation failed for '" + session.getSessionName() + "'; treating as invalid", e);
            return false;
        }
    }

    private void disconnectQuietly(HeadlessSession session) {
        try {
            session.disconnect();
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                    "Failed to disconnect session '" + session.getSessionName() + "'; resource may not be released", e);
        }
    }

    // ========================================================================
    // Background maintenance
    // ========================================================================

    private void evictIdleSessions() {
        try {
            if (shutdownFlag.get()) return;

            long maxIdleMs = config.getMaxIdleTime().toMillis();
            Instant cutoff = Instant.now().minusMillis(maxIdleMs);

            for (HeadlessSession session : idleQueue.toArray(new HeadlessSession[0])) {
                Instant returnedAt = lastReturnedTime.get(session);
                if (returnedAt != null && returnedAt.isBefore(cutoff)) {
                    if (idleQueue.remove(session)) {
                        allSessions.remove(session);
                        lastReturnedTime.remove(session);
                        disconnectQuietly(session);
                        evictionCount.incrementAndGet();
                    }
                }
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Uncaught exception in idle eviction task", t);
        }
    }

    private void evictAgedSessions() {
        try {
            if (shutdownFlag.get()) return;

            long maxAgeMs = config.getMaxAge().toMillis();
            Instant cutoff = Instant.now().minusMillis(maxAgeMs);

            for (HeadlessSession session : idleQueue.toArray(new HeadlessSession[0])) {
                Instant created = allSessions.get(session);
                if (created != null && created.isBefore(cutoff)) {
                    if (idleQueue.remove(session)) {
                        allSessions.remove(session);
                        lastReturnedTime.remove(session);
                        disconnectQuietly(session);
                        evictionCount.incrementAndGet();
                    }
                }
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Uncaught exception in age eviction task", t);
        }
    }

    private void validateIdleSessions() {
        try {
            if (shutdownFlag.get()) return;

            for (HeadlessSession session : idleQueue.toArray(new HeadlessSession[0])) {
                if (!isSessionValid(session)) {
                    if (idleQueue.remove(session)) {
                        allSessions.remove(session);
                        lastReturnedTime.remove(session);
                        disconnectQuietly(session);
                        evictionCount.incrementAndGet();
                    }
                }
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Uncaught exception in periodic validation task", t);
        }
    }

    private void checkNotShutdown() throws PoolExhaustedException {
        if (config == null) {
            throw new IllegalStateException("Pool not configured — call configure() before borrowing");
        }
        if (shutdownFlag.get()) {
            throw new PoolExhaustedException("Pool has been shut down");
        }
    }
}
