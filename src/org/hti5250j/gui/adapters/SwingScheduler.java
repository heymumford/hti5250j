/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui.adapters;

import org.hti5250j.interfaces.IScheduler;

import javax.swing.Timer;
import java.awt.event.ActionListener;

/**
 * Swing-based implementation of IScheduler using javax.swing.Timer.
 * Used in GUI mode to maintain backward compatibility.
 */
public class SwingScheduler implements IScheduler {

    private final Timer timer;
    private int delayMillis;

    /**
     * Create a Swing scheduler.
     * @param listener The ActionListener to execute periodically
     * @param delayMillis Initial delay between executions in milliseconds
     */
    public SwingScheduler(ActionListener listener, int delayMillis) {
        this.delayMillis = delayMillis;
        this.timer = new Timer(delayMillis, listener);
    }

    @Override
    public void start() {
        timer.start();
    }

    @Override
    public void stop() {
        timer.stop();
    }

    @Override
    public boolean isRunning() {
        return timer.isRunning();
    }

    @Override
    public void setDelay(int delayMillis) {
        this.delayMillis = delayMillis;
        timer.setDelay(delayMillis);
    }

    @Override
    public int getDelay() {
        return delayMillis;
    }

    /**
     * Get the underlying Swing Timer (for backward compatibility).
     * @return the Timer instance
     */
    public Timer getTimer() {
        return timer;
    }
}
