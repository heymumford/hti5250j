/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.interfaces;

/**
 * Interface for dispatching UI updates in both GUI and headless modes.
 * Abstracts SwingUtilities.invokeAndWait for headless operation.
 */
public interface IUIDispatcher {

    /**
     * Execute a runnable on the UI thread (GUI mode) or directly (headless mode).
     * Blocks until the task completes.
     * @param task the task to execute
     * @throws Exception if the task throws an exception
     */
    void invokeAndWait(Runnable task) throws Exception;

    /**
     * Execute a runnable on the UI thread (GUI mode) or directly (headless mode).
     * Does not wait for completion.
     * @param task the task to execute
     */
    void invokeLater(Runnable task);
}
