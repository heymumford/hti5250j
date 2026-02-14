/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.interfaces;

/**
 * Factory for creating UI dispatchers based on runtime mode (GUI vs headless).
 */
public class UIDispatcherFactory {

    private static IUIDispatcher defaultDispatcher = null;
    private static boolean headlessMode = false;

    /**
     * Set headless mode. Must be called before any dispatcher is created.
     * @param headless true for headless mode, false for GUI mode
     */
    public static void setHeadlessMode(boolean headless) {
        headlessMode = headless;
        defaultDispatcher = null; // Reset cached dispatcher
    }

    /**
     * Get the default UI dispatcher based on the current mode.
     * @return UI dispatcher instance
     */
    public static IUIDispatcher getDefaultDispatcher() {
        if (defaultDispatcher == null) {
            if (headlessMode || isHeadlessEnvironment()) {
                defaultDispatcher = createHeadlessDispatcher();
            } else {
                defaultDispatcher = createSwingDispatcher();
            }
        }
        return defaultDispatcher;
    }

    /**
     * Check if running in a headless environment (java.awt.headless=true).
     * @return true if headless
     */
    private static boolean isHeadlessEnvironment() {
        return Boolean.getBoolean("java.awt.headless");
    }

    /**
     * Create a headless dispatcher that executes tasks directly on the calling thread.
     */
    private static IUIDispatcher createHeadlessDispatcher() {
        return new IUIDispatcher() {
            @Override
            public void invokeAndWait(Runnable task) throws Exception {
                task.run();
            }

            @Override
            public void invokeLater(Runnable task) {
                task.run();
            }
        };
    }

    /**
     * Create a Swing dispatcher (reflection to avoid loading Swing classes in headless mode).
     * Falls back to headless dispatcher if Swing classes are not available.
     */
    private static IUIDispatcher createSwingDispatcher() {
        try {
            Class<?> clazz = Class.forName("org.hti5250j.gui.adapters.SwingUIDispatcher");
            return (IUIDispatcher) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return createHeadlessDispatcher();
        }
    }
}
