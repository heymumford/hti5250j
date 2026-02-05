package org.tn5250j.plugin;

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
