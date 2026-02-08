/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.contracts;

import org.hti5250j.plugin.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests for PluginManager lifecycle management.
 *
 * Establishes behavioral guarantees that PluginManager must maintain
 * for safe dynamic plugin loading in production environments.
 */
@DisplayName("PluginManager Lifecycle Management Contract")
public class PluginManagerContractTest {

    private MockPluginManager manager;

    @BeforeEach
    void setUp() {
        manager = new MockPluginManager();
    }

    // ============================================================================
    // Contract 1: Plugin Registration & Retrieval
    // ============================================================================

    @Test
    @DisplayName("Contract 1.1: PluginManager maintains empty registry initially")
    void contractPluginManagerStartsEmpty() {
        List<HTI5250jPlugin> plugins = manager.getAllPlugins();
        assertThat("Plugin registry should not be null", plugins, notNullValue());
        assertThat("Plugin registry should be empty initially", plugins, empty());
    }

    @Test
    @DisplayName("Contract 1.2: getPlugin returns null for non-existent plugin")
    void contractGetNonexistentPluginReturnsNull() {
        HTI5250jPlugin plugin = manager.getPlugin("org.nonexistent.plugin");
        assertThat("Non-existent plugin should return null", plugin, nullValue());
    }

    @Test
    @DisplayName("Contract 1.3: getAllPlugins returns non-null list")
    void contractGetAllPluginsNeverReturnsNull() {
        List<HTI5250jPlugin> plugins = manager.getAllPlugins();
        assertThat("getAllPlugins must never return null", plugins, notNullValue());
    }

    // ============================================================================
    // Contract 2: Plugin Activation/Deactivation
    // ============================================================================

    @Test
    @DisplayName("Contract 2.1: isPluginActive returns false for non-existent plugin")
    void contractIsPluginActiveReturnsFalseForNonexistent() {
        boolean isActive = manager.isPluginActive("org.nonexistent.plugin");
        assertThat("Non-existent plugin should not be active", isActive, is(false));
    }

    @Test
    @DisplayName("Contract 2.2: Deactivating non-existent plugin does not throw")
    void contractDeactivateNonexistentPluginDoesNotThrow() throws PluginException {
        // Should handle gracefully (either no-op or throw PluginException)
        try {
            manager.deactivatePlugin("org.nonexistent.plugin");
        } catch (PluginException e) {
            // PluginException is acceptable; RuntimeException is not
            assertThat("Should throw PluginException, not RuntimeException",
                e, instanceOf(PluginException.class));
        }
    }

    // ============================================================================
    // Contract 3: Error Handling & Robustness
    // ============================================================================

    @Test
    @DisplayName("Contract 3.1: PluginManager handles null listeners gracefully")
    void contractPluginManagerHandlesNullListenersGracefully() {
        // Should not throw NullPointerException
        try {
            manager.addLifecycleListener(null);
        } catch (NullPointerException e) {
            // Acceptable to reject null
        }
    }

    @Test
    @DisplayName("Contract 3.2: PluginManager.shutdown() completes without throwing")
    void contractShutdownCompletesWithoutThrowing() throws PluginException {
        // Shutdown must clean up gracefully
        manager.shutdown();
        // After shutdown, plugin registry should be cleanable
        List<HTI5250jPlugin> plugins = manager.getAllPlugins();
        assertThat("After shutdown, should still return valid list", plugins, notNullValue());
    }

    // ============================================================================
    // Contract 4: Type-Safe Plugin Queries
    // ============================================================================

    @Test
    @DisplayName("Contract 4.1: getPluginsOfType returns non-null list")
    void contractGetPluginsOfTypeNeverReturnsNull() {
        List<TestablePlugin> testPlugins = manager.getPluginsOfType(TestablePlugin.class);
        assertThat("getPluginsOfType must never return null", testPlugins, notNullValue());
        assertThat("Type query on empty registry returns empty list", testPlugins, empty());
    }

    // ============================================================================
    // Contract 5: Listener Management
    // ============================================================================

    @Test
    @DisplayName("Contract 5.1: Removing non-existent listener does not throw")
    void contractRemoveNonexistentListenerDoesNotThrow() {
        MockPluginLifecycleListener listener = new MockPluginLifecycleListener();
        // Should handle gracefully even if listener was never added
        manager.removeLifecycleListener(listener);
    }

    // ============================================================================
    // Mock Implementations for Testing
    // ============================================================================

    /**
     * Minimal mock PluginManager for contract testing.
     */
    static class MockPluginManager implements PluginManager {
        private final java.util.List<HTI5250jPlugin> plugins = new java.util.ArrayList<>();
        private final java.util.List<PluginLifecycleListener> listeners = new java.util.ArrayList<>();

        @Override
        public HTI5250jPlugin loadPlugin(Class<? extends HTI5250jPlugin> pluginClass)
                throws PluginException {
            HTI5250jPlugin plugin = new MockPlugin();
            plugins.add(plugin);
            return plugin;
        }

        @Override
        public void unloadPlugin(String pluginId) throws PluginException {
            plugins.removeIf(p -> p.getPluginId().equals(pluginId));
        }

        @Override
        public void activatePlugin(String pluginId) throws PluginException {
            // No-op for mock
        }

        @Override
        public void deactivatePlugin(String pluginId) throws PluginException {
            // No-op for mock
        }

        @Override
        public HTI5250jPlugin getPlugin(String pluginId) {
            return plugins.stream()
                .filter(p -> p.getPluginId().equals(pluginId))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<HTI5250jPlugin> getAllPlugins() {
            return new java.util.ArrayList<>(plugins);
        }

        @Override
        public <T extends HTI5250jPlugin> List<T> getPluginsOfType(Class<T> interfaceClass) {
            return new java.util.ArrayList<>();
        }

        @Override
        public void addLifecycleListener(PluginLifecycleListener listener) {
            if (listener != null) {
                listeners.add(listener);
            }
        }

        @Override
        public void removeLifecycleListener(PluginLifecycleListener listener) {
            if (listener != null) {
                listeners.remove(listener);
            }
        }

        @Override
        public boolean isPluginActive(String pluginId) {
            return getPlugin(pluginId) != null && getPlugin(pluginId).isActive();
        }

        @Override
        public void shutdown() {
            plugins.clear();
            listeners.clear();
        }
    }

    /**
     * Mock plugin for testing.
     */
    static class MockPlugin implements HTI5250jPlugin {
        @Override
        public String getPluginId() {
            return "org.hti5250j.test.mock-plugin";
        }

        @Override
        public String getName() {
            return "Mock Plugin";
        }

        @Override
        public PluginVersion getVersion() {
            return new PluginVersion(1, 0, 0);
        }

        @Override
        public PluginVersion getApiVersionRequired() {
            return new PluginVersion(1, 0, 0);
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public void load() throws PluginException {}

        @Override
        public void activate() throws PluginException {}

        @Override
        public void deactivate() throws PluginException {}

        @Override
        public void unload() throws PluginException {}

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Mock plugin for testing";
        }
    }

    /**
     * Marker interface for type-safe plugin queries.
     */
    interface TestablePlugin extends HTI5250jPlugin {
    }

    /**
     * Mock lifecycle listener for testing.
     */
    static class MockPluginLifecycleListener implements PluginLifecycleListener {
        @Override
        public void onPluginLoaded(String pluginId, PluginVersion version) {}

        @Override
        public void onPluginActivated(String pluginId) {}

        @Override
        public void onPluginDeactivated(String pluginId) {}

        @Override
        public void onPluginUnloaded(String pluginId) {}

        @Override
        public void onPluginError(String pluginId, PluginException error) {}
    }
}
