/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.headless;

import org.hti5250j.interfaces.IScheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Headless implementation of IScheduler using Java's ScheduledExecutorService.
 * Suitable for server deployments and CI/CD environments without GUI.
 */
public class HeadlessScheduler implements IScheduler {

    private final ScheduledExecutorService executor;
    private final Runnable task;
    private ScheduledFuture<?> scheduledTask;
    private int delayMillis;
    private boolean running;

    /**
     * Create a headless scheduler.
     * @param task The task to execute periodically
     * @param delayMillis Initial delay between executions in milliseconds
     */
    public HeadlessScheduler(Runnable task, int delayMillis) {
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.task = task;
        this.delayMillis = delayMillis;
        this.running = false;
    }

    @Override
    public void start() {
        if (!running) {
            scheduledTask = executor.scheduleWithFixedDelay(
                task,
                delayMillis,
                delayMillis,
                TimeUnit.MILLISECONDS
            );
            running = true;
        }
    }

    @Override
    public void stop() {
        if (running && scheduledTask != null) {
            scheduledTask.cancel(false);
            running = false;
        }
        executor.shutdown();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setDelay(int delayMillis) {
        boolean wasRunning = running;
        if (wasRunning) {
            stop();
        }
        this.delayMillis = delayMillis;
        if (wasRunning) {
            start();
        }
    }

    @Override
    public int getDelay() {
        return delayMillis;
    }
}
