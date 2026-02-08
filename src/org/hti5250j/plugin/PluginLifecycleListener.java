/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.plugin;

/**
 * PluginLifecycleListener - Observer for plugin lifecycle events.
 * Supports tracking of load, activate, deactivate, unload phases.
 */
public interface PluginLifecycleListener {
    void onPluginLoaded(String pluginId, PluginVersion version);
    void onPluginActivated(String pluginId);
    void onPluginDeactivated(String pluginId);
    void onPluginUnloaded(String pluginId);
    void onPluginError(String pluginId, PluginException error);
}
