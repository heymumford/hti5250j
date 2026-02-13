/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui.adapters;

import org.hti5250j.interfaces.IUIDispatcher;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

/**
 * Swing-based implementation of IUIDispatcher.
 * Delegates to SwingUtilities for proper EDT thread management.
 */
public class SwingUIDispatcher implements IUIDispatcher {

    @Override
    public void invokeAndWait(Runnable task) throws Exception {
        try {
            SwingUtilities.invokeAndWait(task);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new Exception("UI dispatch failed", e);
        }
    }

    @Override
    public void invokeLater(Runnable task) {
        SwingUtilities.invokeLater(task);
    }
}
