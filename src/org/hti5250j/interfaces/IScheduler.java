/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.interfaces;

/**
 * Interface for scheduling periodic tasks in both GUI and headless modes.
 * Provides abstraction over javax.swing.Timer (GUI) and ScheduledExecutorService (headless).
 */
public interface IScheduler {

    /**
     * Start the scheduler to execute periodic tasks.
     */
    void start();

    /**
     * Stop the scheduler and cancel all scheduled tasks.
     */
    void stop();

    /**
     * Check if the scheduler is currently running.
     * @return true if running, false otherwise
     */
    boolean isRunning();

    /**
     * Set the delay (in milliseconds) between task executions.
     * @param delayMillis delay in milliseconds
     */
    void setDelay(int delayMillis);

    /**
     * Get the current delay between task executions.
     * @return delay in milliseconds
     */
    int getDelay();
}
