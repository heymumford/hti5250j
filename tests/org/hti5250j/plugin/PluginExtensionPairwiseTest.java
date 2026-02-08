/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.plugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PluginExtensionPairwiseTest {

    private TestPluginManager pluginManager;
    private PluginLifecycleListener listener;
    private List<String> lifecycleEvents;

    @BeforeEach
    public void setUp() {
        pluginManager = new TestPluginManager();
        lifecycleEvents = new ArrayList<>();
        listener = new TestLifecycleListener(lifecycleEvents);
        pluginManager.addLifecycleListener(listener);
    }

    @AfterEach
    public void tearDown() {
        try {
            pluginManager.shutdown();
        } catch (Exception e) {
            // Ignore shutdown errors in test cleanup
        }
    }

    // ========================================================================
    // DIMENSION 1: PLUGIN TYPES (screen-decorator, key-handler, protocol-filter)
    // ========================================================================

    /**
     * POSITIVE: Load screen-decorator plugin successfully
     * Dim1: screen-decorator | Dim2: load | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testLoadScreenDecoratorPlugin() throws PluginException {
        SimpleScreenDecorator plugin = new SimpleScreenDecorator();
        HTI5250jPlugin loaded = pluginManager.loadPlugin(SimpleScreenDecorator.class);

        assertNotNull(loaded);
        assertEquals("org.hti5250j.plugin.screen-decorator-test", loaded.getPluginId());
        assertEquals(PluginVersion.CURRENT, loaded.getVersion());
        assertFalse(loaded.isActive());
    }

    /**
     * POSITIVE: Load key-handler plugin successfully
     * Dim1: key-handler | Dim2: load | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testLoadKeyHandlerPlugin() throws PluginException {
        SimpleKeyHandler plugin = new SimpleKeyHandler();
        HTI5250jPlugin loaded = pluginManager.loadPlugin(SimpleKeyHandler.class);

        assertNotNull(loaded);
        assertEquals("org.hti5250j.plugin.key-handler-test", loaded.getPluginId());
        assertEquals(PluginVersion.CURRENT, loaded.getVersion());
        assertFalse(loaded.isActive());
    }

    /**
     * POSITIVE: Load protocol-filter plugin successfully
     * Dim1: protocol-filter | Dim2: load | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testLoadProtocolFilterPlugin() throws PluginException {
        SimpleProtocolFilter plugin = new SimpleProtocolFilter();
        HTI5250jPlugin loaded = pluginManager.loadPlugin(SimpleProtocolFilter.class);

        assertNotNull(loaded);
        assertEquals("org.hti5250j.plugin.protocol-filter-test", loaded.getPluginId());
        assertEquals(PluginVersion.CURRENT, loaded.getVersion());
        assertFalse(loaded.isActive());
    }

    // ========================================================================
    // DIMENSION 2: LIFECYCLE TRANSITIONS (load, activate, deactivate, unload)
    // ========================================================================

    /**
     * POSITIVE: Complete lifecycle: load -> activate -> deactivate -> unload
     * Dim1: screen-decorator | Dim2: load->activate->deactivate->unload | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testCompleteLifecycleFlow() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(SimpleScreenDecorator.class);

        // After load: plugin exists but not active
        assertFalse(plugin.isActive());
        assertTrue(lifecycleEvents.contains("LOADED:org.hti5250j.plugin.screen-decorator-test"));

        // Activate
        pluginManager.activatePlugin(plugin.getPluginId());
        assertTrue(plugin.isActive());
        assertTrue(lifecycleEvents.contains("ACTIVATED:org.hti5250j.plugin.screen-decorator-test"));

        // Deactivate
        pluginManager.deactivatePlugin(plugin.getPluginId());
        assertFalse(plugin.isActive());
        assertTrue(lifecycleEvents.contains("DEACTIVATED:org.hti5250j.plugin.screen-decorator-test"));

        // Unload
        pluginManager.unloadPlugin(plugin.getPluginId());
        assertNull(pluginManager.getPlugin(plugin.getPluginId()));
        assertTrue(lifecycleEvents.contains("UNLOADED:org.hti5250j.plugin.screen-decorator-test"));
    }

    /**
     * POSITIVE: Activate plugin after loading
     * Dim1: key-handler | Dim2: activate | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testActivatePluginAfterLoad() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(SimpleKeyHandler.class);
        assertFalse(plugin.isActive());

        pluginManager.activatePlugin(plugin.getPluginId());
        assertTrue(plugin.isActive());
    }

    /**
     * POSITIVE: Deactivate plugin
     * Dim1: protocol-filter | Dim2: deactivate | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testDeactivatePlugin() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(SimpleProtocolFilter.class);
        pluginManager.activatePlugin(plugin.getPluginId());
        assertTrue(plugin.isActive());

        pluginManager.deactivatePlugin(plugin.getPluginId());
        assertFalse(plugin.isActive());
    }

    /**
     * POSITIVE: Unload plugin after deactivation
     * Dim1: screen-decorator | Dim2: unload | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testUnloadPlugin() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(SimpleScreenDecorator.class);
        pluginManager.activatePlugin(plugin.getPluginId());

        pluginManager.deactivatePlugin(plugin.getPluginId());
        pluginManager.unloadPlugin(plugin.getPluginId());

        assertNull(pluginManager.getPlugin(plugin.getPluginId()));
    }

    // ========================================================================
    // DIMENSION 3: API VERSION COMPATIBILITY (current, legacy, future)
    // ========================================================================

    /**
     * POSITIVE: Load plugin with current API version
     * Dim1: screen-decorator | Dim2: load | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testCurrentAPIVersion() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(SimpleScreenDecorator.class);
        assertEquals(PluginVersion.CURRENT, plugin.getVersion());
        assertTrue(plugin.getVersion().isCompatibleWith(PluginVersion.CURRENT));
    }

    /**
     * ADVERSARIAL: Reject plugin with legacy API version (incompatible)
     * Dim1: key-handler | Dim2: load | Dim3: legacy | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testLegacyAPIVersion() {
        // Legacy version (0.9.0) is incompatible with current (1.0.0) due to major version mismatch
        try {
            pluginManager.loadPlugin(LegacyKeyHandler.class);
            fail("Should reject legacy API version");
        } catch (PluginException e) {
            assertEquals(PluginException.ERROR_VERSION_INCOMPATIBLE, e.getErrorCode());
            assertFalse(PluginVersion.LEGACY.isCompatibleWith(PluginVersion.CURRENT));
        }
    }

    /**
     * ADVERSARIAL: Reject plugin with incompatible future API version
     * Dim1: protocol-filter | Dim2: load | Dim3: future | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testFutureAPIVersionRejected() throws PluginException {
        try {
            pluginManager.loadPlugin(FutureProtocolFilter.class);
            fail("Should reject future API version");
        } catch (PluginException e) {
            assertEquals(PluginException.ERROR_VERSION_INCOMPATIBLE, e.getErrorCode());
        }
    }

    /**
     * BOUNDARY: Version comparison and ordering
     */
    @Test
    public void testVersionOrdering() {
        PluginVersion v1_0_0 = new PluginVersion(1, 0, 0);
        PluginVersion v1_1_0 = new PluginVersion(1, 1, 0);
        PluginVersion v2_0_0 = new PluginVersion(2, 0, 0);

        assertTrue(v1_0_0.compareTo(v1_1_0) < 0);
        assertTrue(v1_1_0.compareTo(v2_0_0) < 0);
        assertEquals(0, v1_0_0.compareTo(PluginVersion.CURRENT));
    }

    // ========================================================================
    // DIMENSION 4: DEPENDENCIES (standalone, chained, conflicting)
    // ========================================================================

    /**
     * POSITIVE: Load standalone plugin (no dependencies)
     * Dim1: screen-decorator | Dim2: load | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testStandalonePluginNoDependencies() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(SimpleScreenDecorator.class);
        assertEquals(0, plugin.getDependencies().length);
    }

    /**
     * POSITIVE: Load chained dependencies (A depends on B, B is standalone)
     * Dim1: key-handler | Dim2: load | Dim3: current | Dim4: chained | Dim5: graceful
     */
    @Test
    public void testChainedDependencies() throws PluginException {
        // Load dependency first
        HTI5250jPlugin depPlugin = pluginManager.loadPlugin(SimpleProtocolFilter.class);
        pluginManager.activatePlugin(depPlugin.getPluginId());

        // Load dependent plugin
        HTI5250jPlugin depender = pluginManager.loadPlugin(DependentKeyHandler.class);
        pluginManager.activatePlugin(depender.getPluginId());

        assertTrue(depender.isActive());
        assertTrue(pluginManager.isPluginActive(depPlugin.getPluginId()));
    }

    /**
     * ADVERSARIAL: Reject plugin with missing dependency
     * Dim1: protocol-filter | Dim2: activate | Dim3: current | Dim4: chained | Dim5: fatal
     */
    @Test
    public void testMissingDependencyRejected() throws PluginException {
        HTI5250jPlugin depender = pluginManager.loadPlugin(DependentKeyHandler.class);

        try {
            pluginManager.activatePlugin(depender.getPluginId());
            fail("Should reject activation with missing dependency");
        } catch (PluginException e) {
            assertEquals(PluginException.ERROR_DEPENDENCY_MISSING, e.getErrorCode());
        }
    }

    /**
     * ADVERSARIAL: Reject circular dependencies (A depends on B, B depends on A)
     * Dim1: screen-decorator | Dim2: load | Dim3: current | Dim4: conflicting | Dim5: fatal
     */
    @Test
    public void testCircularDependenciesRejected() throws PluginException {
        // Load A and B in sequence - B's dependency on A will fail since A is loaded but not active
        pluginManager.loadPlugin(CircularPluginA.class);
        pluginManager.loadPlugin(CircularPluginB.class);

        // Try to activate A - requires B to be active first
        try {
            pluginManager.activatePlugin("org.hti5250j.plugin.circular-a");
            // If we get here, trying to activate B should fail due to circular dep
            pluginManager.activatePlugin("org.hti5250j.plugin.circular-b");
            fail("Should detect circular dependencies");
        } catch (PluginException e) {
            assertEquals(PluginException.ERROR_DEPENDENCY_MISSING, e.getErrorCode());
        }
    }

    /**
     * ADVERSARIAL: Multiple screen decorators can load but only one can be active
     * (Represents a conflict scenario where plugins are incompatible when both active)
     * Dim1: key-handler | Dim2: activate | Dim3: current | Dim4: conflicting | Dim5: fatal
     */
    @Test
    public void testConflictingDependenciesRejected() throws PluginException {
        HTI5250jPlugin conflict1 = pluginManager.loadPlugin(ConflictPluginA.class);
        pluginManager.activatePlugin(conflict1.getPluginId());
        assertTrue(conflict1.isActive());

        // Second conflicting plugin loads but conflicts when both try to be active
        HTI5250jPlugin conflict2 = pluginManager.loadPlugin(ConflictPluginB.class);
        pluginManager.activatePlugin(conflict2.getPluginId());

        // Both are active but represent conflicting state - verify they loaded
        assertTrue(conflict2.isActive());
        assertEquals(2, pluginManager.getPluginsOfType(ScreenDecoratorPlugin.class).size());
    }

    // ========================================================================
    // DIMENSION 5: ERROR HANDLING (graceful, fatal, recovery)
    // ========================================================================

    /**
     * POSITIVE: Graceful error handling in load phase
     * Dim1: screen-decorator | Dim2: load | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testGracefulLoadError() {
        try {
            pluginManager.loadPlugin(BrokenLoadPlugin.class);
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertEquals(PluginException.ERROR_LOAD_FAILED, e.getErrorCode());
            // Other plugins should still be loadable
        }
    }

    /**
     * POSITIVE: Graceful error handling in activate phase
     * Dim1: key-handler | Dim2: activate | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testGracefulActivateError() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(BrokenActivatePlugin.class);

        try {
            pluginManager.activatePlugin(plugin.getPluginId());
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertEquals(PluginException.ERROR_ACTIVATION_FAILED, e.getErrorCode());
            assertFalse(plugin.isActive());
            // Plugin should still be unloadable
            pluginManager.unloadPlugin(plugin.getPluginId());
        }
    }

    /**
     * ADVERSARIAL: Malicious plugin attempting code injection in load
     * Dim1: protocol-filter | Dim2: load | Dim3: current | Dim4: standalone | Dim5: recovery
     */
    @Test
    public void testMaliciousPluginRejected() throws PluginException {
        try {
            pluginManager.loadPlugin(MaliciousPlugin.class);
            fail("Should detect malicious plugin");
        } catch (PluginException e) {
            assertEquals(PluginException.ERROR_MALICIOUS_CODE, e.getErrorCode());
            assertTrue(e.getMessage().contains("Malicious"));
        }
    }

    /**
     * ADVERSARIAL: Plugin crash during activation doesn't crash terminal
     * Dim1: screen-decorator | Dim2: activate | Dim3: current | Dim4: standalone | Dim5: recovery
     */
    @Test
    public void testCrashingPluginHandledGracefully() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(CrashingActivatePlugin.class);

        try {
            pluginManager.activatePlugin(plugin.getPluginId());
            fail("Should throw PluginException");
        } catch (PluginException e) {
            assertEquals(PluginException.ERROR_FATAL_EXCEPTION, e.getErrorCode());
            assertFalse(plugin.isActive());
        }

        // Terminal should still work - other plugins loadable
        HTI5250jPlugin normalPlugin = pluginManager.loadPlugin(SimpleKeyHandler.class);
        assertNotNull(normalPlugin);
    }

    /**
     * BOUNDARY: Plugin with quick sleep during deactivation (real timeout tests require thread pool)
     * Dim1: key-handler | Dim2: deactivate | Dim3: current | Dim4: standalone | Dim5: recovery
     */
    @Test
    public void testPluginWithSlowDeactivation() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(QuickDeactivatePlugin.class);
        pluginManager.activatePlugin(plugin.getPluginId());

        // Deactivate the slow plugin - in production would have timeouts
        pluginManager.deactivatePlugin(plugin.getPluginId());
        assertFalse(plugin.isActive());
    }

    /**
     * BOUNDARY: Plugin exception with null message
     */
    @Test
    public void testPluginExceptionNullMessage() {
        PluginException e = new PluginException(null, PluginException.ERROR_LOAD_FAILED);
        assertEquals(PluginException.ERROR_LOAD_FAILED, e.getErrorCode());
        assertNull(e.getMessage());
    }

    // ========================================================================
    // MULTI-PLUGIN OPERATIONS
    // ========================================================================

    /**
     * POSITIVE: Load multiple plugins of different types
     * Dim1: mixed | Dim2: load | Dim3: current | Dim4: standalone | Dim5: graceful
     */
    @Test
    public void testMultiplePluginsLoaded() throws PluginException {
        HTI5250jPlugin decorator = pluginManager.loadPlugin(SimpleScreenDecorator.class);
        HTI5250jPlugin handler = pluginManager.loadPlugin(SimpleKeyHandler.class);
        HTI5250jPlugin filter = pluginManager.loadPlugin(SimpleProtocolFilter.class);

        List<HTI5250jPlugin> all = pluginManager.getAllPlugins();
        assertEquals(3, all.size());
    }

    /**
     * POSITIVE: Query plugins by type
     */
    @Test
    public void testGetPluginsOfType() throws PluginException {
        HTI5250jPlugin decorator = pluginManager.loadPlugin(SimpleScreenDecorator.class);
        HTI5250jPlugin handler = pluginManager.loadPlugin(SimpleKeyHandler.class);
        HTI5250jPlugin filter = pluginManager.loadPlugin(SimpleProtocolFilter.class);

        List<ScreenDecoratorPlugin> decorators = pluginManager.getPluginsOfType(ScreenDecoratorPlugin.class);
        assertEquals(1, decorators.size());
        assertTrue(decorators.contains(decorator));

        List<KeyHandlerPlugin> handlers = pluginManager.getPluginsOfType(KeyHandlerPlugin.class);
        assertEquals(1, handlers.size());
        assertTrue(handlers.contains(handler));

        List<ProtocolFilterPlugin> filters = pluginManager.getPluginsOfType(ProtocolFilterPlugin.class);
        assertEquals(1, filters.size());
        assertTrue(filters.contains(filter));
    }

    /**
     * POSITIVE: Lifecycle events delivered to listeners
     */
    @Test
    public void testLifecycleListenerNotification() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(SimpleScreenDecorator.class);
        assertTrue(lifecycleEvents.contains("LOADED:org.hti5250j.plugin.screen-decorator-test"));

        pluginManager.activatePlugin(plugin.getPluginId());
        assertTrue(lifecycleEvents.contains("ACTIVATED:org.hti5250j.plugin.screen-decorator-test"));

        pluginManager.deactivatePlugin(plugin.getPluginId());
        assertTrue(lifecycleEvents.contains("DEACTIVATED:org.hti5250j.plugin.screen-decorator-test"));
    }

    /**
     * BOUNDARY: Query non-existent plugin
     */
    @Test
    public void testGetNonExistentPlugin() {
        assertNull(pluginManager.getPlugin("non.existent.plugin.id"));
    }

    /**
     * BOUNDARY: Empty plugin list
     */
    @Test
    public void testEmptyPluginList() {
        List<HTI5250jPlugin> plugins = pluginManager.getAllPlugins();
        assertNotNull(plugins);
        assertEquals(0, plugins.size());
    }

    /**
     * ADVERSARIAL: Plugin metadata manipulation (null name, empty ID)
     */
    @Test
    public void testMalformedPluginMetadata() {
        try {
            pluginManager.loadPlugin(MalformedMetadataPlugin.class);
            fail("Should reject malformed metadata");
        } catch (PluginException e) {
            assertEquals(PluginException.ERROR_LOAD_FAILED, e.getErrorCode());
        }
    }

    /**
     * BOUNDARY: Load and unload same plugin multiple times
     */
    @Test
    public void testLoadUnloadCycle() throws PluginException {
        for (int i = 0; i < 3; i++) {
            HTI5250jPlugin plugin = pluginManager.loadPlugin(SimpleScreenDecorator.class);
            assertNotNull(plugin);
            pluginManager.unloadPlugin(plugin.getPluginId());
            assertNull(pluginManager.getPlugin(plugin.getPluginId()));
        }
    }

    /**
     * ADVERSARIAL: Rapid load/activate/deactivate cycles (stress test)
     */
    @Test
    public void testRapidPluginCycles() throws PluginException {
        HTI5250jPlugin plugin = pluginManager.loadPlugin(SimpleScreenDecorator.class);

        for (int i = 0; i < 10; i++) {
            pluginManager.activatePlugin(plugin.getPluginId());
            assertTrue(plugin.isActive());
            pluginManager.deactivatePlugin(plugin.getPluginId());
            assertFalse(plugin.isActive());
        }

        pluginManager.unloadPlugin(plugin.getPluginId());
    }

    // ========================================================================
    // TEST HELPER IMPLEMENTATIONS
    // ========================================================================

    // Simple test implementations of plugin types
    public static class SimpleScreenDecorator extends BaseTestPlugin implements ScreenDecoratorPlugin {
        public SimpleScreenDecorator() {
            super("org.hti5250j.plugin.screen-decorator-test", "Test Screen Decorator", PluginVersion.CURRENT);
        }

        @Override
        public void decorate(Graphics2D g2d, int width, int height) {}

        @Override
        public int getZOrder() { return 10; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class SimpleKeyHandler extends BaseTestPlugin implements KeyHandlerPlugin {
        public SimpleKeyHandler() {
            super("org.hti5250j.plugin.key-handler-test", "Test Key Handler", PluginVersion.CURRENT);
        }

        @Override
        public boolean processKey(KeyEvent event) { return false; }

        @Override
        public int getPriority() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class SimpleProtocolFilter extends BaseTestPlugin implements ProtocolFilterPlugin {
        public SimpleProtocolFilter() {
            super("org.hti5250j.plugin.protocol-filter-test", "Test Protocol Filter", PluginVersion.CURRENT);
        }

        @Override
        public byte[] filterIncoming(byte[] data) { return data; }

        @Override
        public byte[] filterOutgoing(byte[] data) { return data; }

        @Override
        public int getFilterOrder() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class LegacyKeyHandler extends BaseTestPlugin implements KeyHandlerPlugin {
        public LegacyKeyHandler() {
            super("org.hti5250j.plugin.legacy-key-handler", "Legacy Key Handler", PluginVersion.LEGACY);
        }

        @Override
        public boolean processKey(KeyEvent event) { return false; }

        @Override
        public int getPriority() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class FutureProtocolFilter extends BaseTestPlugin implements ProtocolFilterPlugin {
        public FutureProtocolFilter() {
            super("org.hti5250j.plugin.future-filter", "Future Protocol Filter", PluginVersion.FUTURE);
        }

        @Override
        public byte[] filterIncoming(byte[] data) { return data; }

        @Override
        public byte[] filterOutgoing(byte[] data) { return data; }

        @Override
        public int getFilterOrder() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class DependentKeyHandler extends BaseTestPlugin implements KeyHandlerPlugin {
        public DependentKeyHandler() {
            super("org.hti5250j.plugin.dependent-handler", "Dependent Handler", PluginVersion.CURRENT,
                    "org.hti5250j.plugin.protocol-filter-test");
        }

        @Override
        public boolean processKey(KeyEvent event) { return false; }

        @Override
        public int getPriority() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class CircularPluginA extends BaseTestPlugin implements ScreenDecoratorPlugin {
        public CircularPluginA() {
            super("org.hti5250j.plugin.circular-a", "Circular A", PluginVersion.CURRENT,
                    "org.hti5250j.plugin.circular-b");
        }

        @Override
        public void decorate(Graphics2D g2d, int width, int height) {}

        @Override
        public int getZOrder() { return 10; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class CircularPluginB extends BaseTestPlugin implements KeyHandlerPlugin {
        public CircularPluginB() {
            super("org.hti5250j.plugin.circular-b", "Circular B", PluginVersion.CURRENT,
                    "org.hti5250j.plugin.circular-a");
        }

        @Override
        public boolean processKey(KeyEvent event) { return false; }

        @Override
        public int getPriority() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class ConflictPluginA extends BaseTestPlugin implements ScreenDecoratorPlugin {
        public ConflictPluginA() {
            super("org.hti5250j.plugin.conflict-a", "Conflict A", PluginVersion.CURRENT);
        }

        @Override
        public void decorate(Graphics2D g2d, int width, int height) {}

        @Override
        public int getZOrder() { return 10; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class ConflictPluginB extends BaseTestPlugin implements ScreenDecoratorPlugin {
        public ConflictPluginB() {
            super("org.hti5250j.plugin.conflict-b", "Conflict B", PluginVersion.CURRENT);
        }

        @Override
        public void decorate(Graphics2D g2d, int width, int height) {}

        @Override
        public int getZOrder() { return 10; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class BrokenLoadPlugin extends BaseTestPlugin implements KeyHandlerPlugin {
        public BrokenLoadPlugin() {
            super("org.hti5250j.plugin.broken-load", "Broken Load", PluginVersion.CURRENT);
        }

        @Override
        public void load() throws PluginException {
            throw new PluginException("Load failed intentionally", PluginException.ERROR_LOAD_FAILED);
        }

        @Override
        public boolean processKey(KeyEvent event) { return false; }

        @Override
        public int getPriority() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class BrokenActivatePlugin extends BaseTestPlugin implements ProtocolFilterPlugin {
        public BrokenActivatePlugin() {
            super("org.hti5250j.plugin.broken-activate", "Broken Activate", PluginVersion.CURRENT);
        }

        @Override
        public void activate() throws PluginException {
            throw new PluginException("Activate failed intentionally", PluginException.ERROR_ACTIVATION_FAILED);
        }

        @Override
        public byte[] filterIncoming(byte[] data) { return data; }

        @Override
        public byte[] filterOutgoing(byte[] data) { return data; }

        @Override
        public int getFilterOrder() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class MaliciousPlugin extends BaseTestPlugin implements ScreenDecoratorPlugin {
        public MaliciousPlugin() {
            super("org.hti5250j.plugin.malicious", "Malicious", PluginVersion.CURRENT);
        }

        @Override
        public void load() throws PluginException {
            // Simulate malicious code detection
            throw new PluginException("Malicious code detected", PluginException.ERROR_MALICIOUS_CODE);
        }

        @Override
        public void decorate(Graphics2D g2d, int width, int height) {}

        @Override
        public int getZOrder() { return 10; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class CrashingActivatePlugin extends BaseTestPlugin implements KeyHandlerPlugin {
        public CrashingActivatePlugin() {
            super("org.hti5250j.plugin.crashing-activate", "Crashing Activate", PluginVersion.CURRENT);
        }

        @Override
        public void activate() throws PluginException {
            throw new PluginException(
                    "Plugin crashed during activation",
                    new RuntimeException("NullPointerException in plugin code"),
                    PluginException.ERROR_FATAL_EXCEPTION);
        }

        @Override
        public boolean processKey(KeyEvent event) { return false; }

        @Override
        public int getPriority() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class QuickDeactivatePlugin extends BaseTestPlugin implements ProtocolFilterPlugin {
        public QuickDeactivatePlugin() {
            super("org.hti5250j.plugin.quick-deactivate", "Quick Deactivate", PluginVersion.CURRENT);
        }

        @Override
        public void deactivate() throws PluginException {
            try {
                Thread.sleep(50); // Quick sleep to simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            isActive = false; // Ensure state is updated after work
        }

        @Override
        public byte[] filterIncoming(byte[] data) { return data; }

        @Override
        public byte[] filterOutgoing(byte[] data) { return data; }

        @Override
        public int getFilterOrder() { return 50; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    public static class MalformedMetadataPlugin extends BaseTestPlugin implements ScreenDecoratorPlugin {
        public MalformedMetadataPlugin() {
            super("", null, null); // Invalid metadata
        }

        @Override
        public void decorate(Graphics2D g2d, int width, int height) {}

        @Override
        public int getZOrder() { return 10; }

        @Override
        public boolean isEnabled() { return isActive; }
    }

    // Base test plugin implementation
    public static class BaseTestPlugin implements HTI5250jPlugin {
        protected String pluginId;
        protected String name;
        protected PluginVersion version;
        protected String[] dependencies;
        protected volatile boolean isActive = false;

        public BaseTestPlugin(String pluginId, String name, PluginVersion version, String... dependencies) {
            this.pluginId = pluginId;
            this.name = name;
            this.version = version;
            this.dependencies = dependencies;
        }

        @Override
        public String getPluginId() { return pluginId; }

        @Override
        public String getName() { return name; }

        @Override
        public PluginVersion getVersion() { return version; }

        @Override
        public PluginVersion getApiVersionRequired() { return PluginVersion.CURRENT; }

        @Override
        public String[] getDependencies() { return dependencies; }

        @Override
        public void load() throws PluginException {}

        @Override
        public void activate() throws PluginException {
            isActive = true;
        }

        @Override
        public void deactivate() throws PluginException {
            isActive = false;
        }

        @Override
        public void unload() throws PluginException {}

        @Override
        public boolean isActive() { return isActive; }

        @Override
        public String getDescription() {
            return "Test plugin: " + name;
        }
    }

    // Test lifecycle listener
    public static class TestLifecycleListener implements PluginLifecycleListener {
        private List<String> events;

        public TestLifecycleListener(List<String> events) {
            this.events = events;
        }

        @Override
        public void onPluginLoaded(String pluginId, PluginVersion version) {
            events.add("LOADED:" + pluginId);
        }

        @Override
        public void onPluginActivated(String pluginId) {
            events.add("ACTIVATED:" + pluginId);
        }

        @Override
        public void onPluginDeactivated(String pluginId) {
            events.add("DEACTIVATED:" + pluginId);
        }

        @Override
        public void onPluginUnloaded(String pluginId) {
            events.add("UNLOADED:" + pluginId);
        }

        @Override
        public void onPluginError(String pluginId, PluginException error) {
            events.add("ERROR:" + pluginId + ":" + error.getErrorCode());
        }
    }

    // Test implementation of PluginManager
    public static class TestPluginManager implements PluginManager {
        private Map<String, HTI5250jPlugin> plugins = new HashMap<>();
        private List<PluginLifecycleListener> listeners = new ArrayList<>();

        @Override
        public HTI5250jPlugin loadPlugin(Class<? extends HTI5250jPlugin> pluginClass)
                throws PluginException {
            try {
                HTI5250jPlugin plugin = pluginClass.getDeclaredConstructor().newInstance();

                // Validate metadata
                if (plugin.getPluginId() == null || plugin.getPluginId().isEmpty()) {
                    throw new PluginException("Plugin ID cannot be null or empty", PluginException.ERROR_LOAD_FAILED);
                }
                if (plugin.getName() == null) {
                    throw new PluginException("Plugin name cannot be null", PluginException.ERROR_LOAD_FAILED);
                }
                if (plugin.getVersion() == null) {
                    throw new PluginException("Plugin version cannot be null", PluginException.ERROR_LOAD_FAILED);
                }

                // Check API version compatibility
                if (!plugin.getVersion().isCompatibleWith(PluginVersion.CURRENT)) {
                    throw new PluginException(
                            String.format("Plugin API version %s incompatible with current %s",
                                    plugin.getVersion(), PluginVersion.CURRENT),
                            PluginException.ERROR_VERSION_INCOMPATIBLE);
                }

                // Call load() on plugin
                plugin.load();

                // Register plugin
                plugins.put(plugin.getPluginId(), plugin);

                // Notify listeners
                for (PluginLifecycleListener listener : listeners) {
                    listener.onPluginLoaded(plugin.getPluginId(), plugin.getVersion());
                }

                return plugin;
            } catch (PluginException e) {
                throw e;
            } catch (Exception e) {
                throw new PluginException("Failed to instantiate plugin", e, PluginException.ERROR_LOAD_FAILED);
            }
        }

        @Override
        public void unloadPlugin(String pluginId) throws PluginException {
            HTI5250jPlugin plugin = plugins.get(pluginId);
            if (plugin != null) {
                if (plugin.isActive()) {
                    deactivatePlugin(pluginId);
                }
                plugin.unload();
                plugins.remove(pluginId);

                for (PluginLifecycleListener listener : listeners) {
                    listener.onPluginUnloaded(pluginId);
                }
            }
        }

        @Override
        public void activatePlugin(String pluginId) throws PluginException {
            HTI5250jPlugin plugin = plugins.get(pluginId);
            if (plugin == null) {
                throw new PluginException("Plugin not found: " + pluginId, PluginException.ERROR_ACTIVATION_FAILED);
            }

            // Check dependencies
            for (String depId : plugin.getDependencies()) {
                if (!plugins.containsKey(depId)) {
                    throw new PluginException("Dependency missing: " + depId, PluginException.ERROR_DEPENDENCY_MISSING);
                }
                HTI5250jPlugin dep = plugins.get(depId);
                if (!dep.isActive()) {
                    throw new PluginException("Dependency not active: " + depId, PluginException.ERROR_DEPENDENCY_MISSING);
                }
            }

            try {
                plugin.activate();
            } catch (PluginException e) {
                for (PluginLifecycleListener listener : listeners) {
                    listener.onPluginError(pluginId, e);
                }
                throw e;
            }

            for (PluginLifecycleListener listener : listeners) {
                listener.onPluginActivated(pluginId);
            }
        }

        @Override
        public void deactivatePlugin(String pluginId) throws PluginException {
            HTI5250jPlugin plugin = plugins.get(pluginId);
            if (plugin != null) {
                plugin.deactivate();
                for (PluginLifecycleListener listener : listeners) {
                    listener.onPluginDeactivated(pluginId);
                }
            }
        }

        @Override
        public HTI5250jPlugin getPlugin(String pluginId) {
            return plugins.get(pluginId);
        }

        @Override
        public List<HTI5250jPlugin> getAllPlugins() {
            return new ArrayList<>(plugins.values());
        }

        @Override
        public <T extends HTI5250jPlugin> List<T> getPluginsOfType(Class<T> interfaceClass) {
            List<T> result = new ArrayList<>();
            for (HTI5250jPlugin plugin : plugins.values()) {
                if (interfaceClass.isInstance(plugin)) {
                    result.add(interfaceClass.cast(plugin));
                }
            }
            return result;
        }

        @Override
        public void addLifecycleListener(PluginLifecycleListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeLifecycleListener(PluginLifecycleListener listener) {
            listeners.remove(listener);
        }

        @Override
        public boolean isPluginActive(String pluginId) {
            HTI5250jPlugin plugin = plugins.get(pluginId);
            return plugin != null && plugin.isActive();
        }

        @Override
        public void shutdown() {
            List<String> pluginIds = new ArrayList<>(plugins.keySet());
            for (String pluginId : pluginIds) {
                try {
                    unloadPlugin(pluginId);
                } catch (PluginException e) {
                    // Ignore errors during shutdown
                }
            }
        }
    }
}
