/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.plugin;

/**
 * HTI5250jPlugin - Core plugin interface defining lifecycle and metadata.
 *
 * Plugin implementations must:
 * 1. Define unique plugin ID and version
 * 2. Declare API version compatibility
 * 3. Declare all required dependencies
 * 4. Implement clean load/activate/deactivate/unload sequence
 * 5. Handle errors gracefully without crashing the terminal
 */
public interface HTI5250jPlugin {

    /**
     * Get unique plugin identifier (e.g., "org.hti5250j.plugin.screen-decorator-v1")
     */
    String getPluginId();

    /**
     * Get human-readable plugin name (e.g., "Screen Decorator Plugin")
     */
    String getName();

    /**
     * Get plugin version (e.g., 1.0.0)
     */
    PluginVersion getVersion();

    /**
     * Get minimum API version required by this plugin (e.g., 1.0.0)
     */
    PluginVersion getApiVersionRequired();

    /**
     * Get array of plugin IDs this plugin depends on (may be empty)
     */
    String[] getDependencies();

    /**
     * Load the plugin - initialize resources, register listeners.
     * Must not throw uncaught exceptions; use PluginException instead.
     *
     * @throws PluginException if loading fails
     */
    void load() throws PluginException;

    /**
     * Activate the plugin - make it operational.
     * Called after successful load() and all dependencies are active.
     * Must not throw uncaught exceptions.
     *
     * @throws PluginException if activation fails
     */
    void activate() throws PluginException;

    /**
     * Deactivate the plugin - stop all operations gracefully.
     * Must not throw uncaught exceptions.
     *
     * @throws PluginException if deactivation fails
     */
    void deactivate() throws PluginException;

    /**
     * Unload the plugin - release all resources.
     * Called after deactivate(). Must clean up completely.
     * Must not throw uncaught exceptions.
     *
     * @throws PluginException if unload fails
     */
    void unload() throws PluginException;

    /**
     * Check if plugin is currently active
     */
    boolean isActive();

    /**
     * Get human-readable description of plugin purpose
     */
    String getDescription();
}
