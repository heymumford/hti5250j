/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.headless;

import org.hti5250j.interfaces.IUIDispatcher;

/**
 * Headless implementation of IUIDispatcher.
 * Executes tasks directly on the calling thread since there's no GUI thread.
 */
public class HeadlessUIDispatcher implements IUIDispatcher {

    @Override
    public void invokeAndWait(Runnable task) throws Exception {
        // In headless mode, just run directly on the current thread
        task.run();
    }

    @Override
    public void invokeLater(Runnable task) {
        // In headless mode, just run directly on the current thread
        task.run();
    }
}
