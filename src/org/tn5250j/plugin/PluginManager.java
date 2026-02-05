package org.tn5250j.plugin;

import java.util.*;

/**
 * PluginManager - Core plugin lifecycle management.
 *
 * Responsibilities:
 * 1. Load/unload plugins dynamically
 * 2. Manage plugin state transitions (load -> activate -> deactivate -> unload)
 * 3. Resolve and validate dependencies
 * 4. Handle errors gracefully (malicious plugins don't crash terminal)
 * 5. Maintain plugin registry
 */
public interface PluginManager {

    /**
     * Load a plugin from class.
     * Triggers: load() -> dependency check -> activate() -> listeners
     *
     * @param pluginClass the plugin implementation class
     * @return the loaded plugin instance
     * @throws PluginException if loading fails
     */
    TN5250jPlugin loadPlugin(Class<? extends TN5250jPlugin> pluginClass)
            throws PluginException;

    /**
     * Unload a plugin by ID.
     * Triggers: deactivate() -> unload() -> listeners
     *
     * @param pluginId the plugin identifier
     * @throws PluginException if unloading fails
     */
    void unloadPlugin(String pluginId) throws PluginException;

    /**
     * Activate a plugin by ID.
     *
     * @param pluginId the plugin identifier
     * @throws PluginException if activation fails
     */
    void activatePlugin(String pluginId) throws PluginException;

    /**
     * Deactivate a plugin by ID.
     *
     * @param pluginId the plugin identifier
     * @throws PluginException if deactivation fails
     */
    void deactivatePlugin(String pluginId) throws PluginException;

    /**
     * Get loaded plugin by ID.
     *
     * @param pluginId the plugin identifier
     * @return the plugin or null if not found
     */
    TN5250jPlugin getPlugin(String pluginId);

    /**
     * Get all loaded plugins.
     *
     * @return list of loaded plugins
     */
    List<TN5250jPlugin> getAllPlugins();

    /**
     * Get all plugins of a specific type.
     *
     * @param interfaceClass the plugin interface type
     * @return list of plugins implementing the interface
     */
    <T extends TN5250jPlugin> List<T> getPluginsOfType(Class<T> interfaceClass);

    /**
     * Add lifecycle listener for events.
     *
     * @param listener the listener to add
     */
    void addLifecycleListener(PluginLifecycleListener listener);

    /**
     * Remove lifecycle listener.
     *
     * @param listener the listener to remove
     */
    void removeLifecycleListener(PluginLifecycleListener listener);

    /**
     * Check if a plugin is currently active.
     *
     * @param pluginId the plugin identifier
     * @return true if active, false otherwise
     */
    boolean isPluginActive(String pluginId);

    /**
     * Unload all plugins and shutdown the manager.
     */
    void shutdown();
}
