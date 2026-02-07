/*
 * Host Terminal Interface 5250j - Contract Test Suite
 * Plugin Lifecycle State Machine Contract
 *
 * This test establishes immutable behavioral contracts for HTI5250jPlugin
 * BEFORE any refactoring to Temurin 21 features. Violations detected by these
 * tests indicate fundamental architectural issues requiring review.
 *
 * Contract: Plugin lifecycle follows strict state machine
 *   UNLOADED → LOADED → ACTIVATED → DEACTIVATED → UNLOADED
 *
 * Contract Requirements:
 * 1. getPluginId() must return non-empty string (unique plugin identifier)
 * 2. getName() must return non-empty, human-readable string
 * 3. getVersion() must return valid PluginVersion
 * 4. getApiVersionRequired() must return valid minimum version
 * 5. getDependencies() must return non-null array (may be empty)
 * 6. load()/activate()/deactivate()/unload() must handle PluginException
 * 7. isActive() must reflect plugin state
 * 8. getDescription() must return non-empty string
 *
 * Precondition: No test should modify plugin state
 * Post-condition: All contracts must pass before proceeding to Phase 2
 */
package org.hti5250j.contracts;

import org.hti5250j.plugin.HTI5250jPlugin;
import org.hti5250j.plugin.PluginException;
import org.hti5250j.plugin.PluginVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests for HTI5250jPlugin lifecycle state machine.
 *
 * Establishes behavioral guarantees for plugin loading, activation,
 * and deactivation that cannot change without architectural review.
 */
@DisplayName("HTI5250jPlugin Lifecycle Contract")
public class HTI5250jPluginContractTest {

    private HTI5250jPlugin plugin;

    @BeforeEach
    void setUp() {
        // Initialize a concrete plugin implementation
        plugin = new MockHTI5250jPlugin();
    }

    // ============================================================================
    // Contract 1: Plugin Metadata - Identification
    // ============================================================================

    @Test
    @DisplayName("Contract 1.1: Plugin must have non-empty unique ID")
    void contractPluginHasValidPluginId() {
        String pluginId = plugin.getPluginId();
        assertThat("Plugin ID must not be null", pluginId, notNullValue());
        assertThat("Plugin ID must not be empty", pluginId, not(emptyString()));
        assertThat("Plugin ID must follow namespace convention", pluginId,
            containsString("."));
    }

    @Test
    @DisplayName("Contract 1.2: Plugin must have human-readable name")
    void contractPluginHasValidName() {
        String name = plugin.getName();
        assertThat("Plugin name must not be null", name, notNullValue());
        assertThat("Plugin name must not be empty", name, not(emptyString()));
    }

    @Test
    @DisplayName("Contract 1.3: Plugin must have valid description")
    void contractPluginHasValidDescription() {
        String description = plugin.getDescription();
        assertThat("Plugin description must not be null", description, notNullValue());
        assertThat("Plugin description must not be empty", description, not(emptyString()));
    }

    // ============================================================================
    // Contract 2: Plugin Versioning
    // ============================================================================

    @Test
    @DisplayName("Contract 2.1: Plugin must return valid version")
    void contractPluginHasValidVersion() {
        PluginVersion version = plugin.getVersion();
        assertThat("Plugin version must not be null", version, notNullValue());
    }

    @Test
    @DisplayName("Contract 2.2: Plugin must declare minimum API version")
    void contractPluginDecluresApiVersionRequired() {
        PluginVersion apiVersion = plugin.getApiVersionRequired();
        assertThat("API version required must not be null", apiVersion, notNullValue());
    }

    // ============================================================================
    // Contract 3: Plugin Dependencies
    // ============================================================================

    @Test
    @DisplayName("Contract 3.1: Plugin must declare dependencies (array, may be empty)")
    void contractPluginDeclaresValidDependencies() {
        String[] dependencies = plugin.getDependencies();
        assertThat("Dependencies array must not be null", dependencies, notNullValue());
        // Dependencies may be empty array - that's OK
        for (String dep : dependencies) {
            assertThat("Each dependency must be non-empty string", dep, not(emptyString()));
        }
    }

    // ============================================================================
    // Contract 4: Plugin Lifecycle - Load/Activate/Deactivate/Unload
    // ============================================================================

    @Test
    @DisplayName("Contract 4.1: Plugin.load() must not throw unchecked exception")
    void contractPluginLoadDoesNotThrowUnchecked() throws PluginException {
        // Should complete without throwing RuntimeException or PluginException
        plugin.load();
        // No assertion needed - contract is "doesn't throw"
    }

    @Test
    @DisplayName("Contract 4.2: Plugin.activate() must not throw unchecked exception")
    void contractPluginActivateDoesNotThrowUnchecked() throws PluginException {
        plugin.load(); // Must load first
        plugin.activate();
        // No assertion needed - contract is "doesn't throw"
    }

    @Test
    @DisplayName("Contract 4.3: Plugin.deactivate() must not throw unchecked exception")
    void contractPluginDeactivateDoesNotThrowUnchecked() throws PluginException {
        plugin.load();
        plugin.activate();
        plugin.deactivate();
        // No assertion needed - contract is "doesn't throw"
    }

    @Test
    @DisplayName("Contract 4.4: Plugin.unload() must not throw unchecked exception")
    void contractPluginUnloadDoesNotThrowUnchecked() throws PluginException {
        plugin.load();
        plugin.activate();
        plugin.deactivate();
        plugin.unload();
        // No assertion needed - contract is "doesn't throw"
    }

    // ============================================================================
    // Contract 5: Plugin Activity State
    // ============================================================================

    @Test
    @DisplayName("Contract 5.1: Plugin.isActive() reflects activation state")
    void contractPluginIsActiveReflectsState() throws PluginException {
        assertThat("New plugin should not be active",
            plugin.isActive(), is(false));

        plugin.load();
        assertThat("Loaded plugin may or may not be active",
            plugin.isActive(), anyOf(is(true), is(false)));

        plugin.activate();
        assertThat("Activated plugin must be active",
            plugin.isActive(), is(true));

        plugin.deactivate();
        assertThat("Deactivated plugin must not be active",
            plugin.isActive(), is(false));
    }

    // ============================================================================
    // Mock Implementation for Testing
    // ============================================================================

    /**
     * Minimal concrete implementation of HTI5250jPlugin for contract testing.
     * This mock implements all required methods and passes all contracts.
     */
    static class MockHTI5250jPlugin implements HTI5250jPlugin {
        private boolean active = false;

        @Override
        public String getPluginId() {
            return "org.hti5250j.test.mock-plugin-v1";
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
        public void load() throws PluginException {
            // No-op for contract testing
        }

        @Override
        public void activate() throws PluginException {
            active = true;
        }

        @Override
        public void deactivate() throws PluginException {
            active = false;
        }

        @Override
        public void unload() throws PluginException {
            active = false;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public String getDescription() {
            return "Mock plugin for contract testing";
        }
    }
}
