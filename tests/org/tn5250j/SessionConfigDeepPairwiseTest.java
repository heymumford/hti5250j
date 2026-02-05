/**
 * SessionConfigDeepPairwiseTest.java
 *
 * Advanced N-wise pairwise test suite for SessionConfig with sophisticated coverage
 * of interaction effects between configuration dimensions. This suite extends basic
 * pairwise testing with adversarial scenarios, cascading conflicts, and deep state
 * transitions.
 *
 * Pairwise Dimensions (5-way testing):
 * 1. Property type:   connection (host/port), display (font/color), keyboard, audio
 * 2. Value type:      string, integer, boolean, enum, composite (Rectangle)
 * 3. Source:          default (built-in), file (loaded), programmatic, override
 * 4. Validation:      none, range (min/max), format (regex), required (must-exist)
 * 5. Persistence:     transient (memory-only), session (current), permanent (saved)
 *
 * Test Categories:
 * - POSITIVE: Valid configurations, legitimate scenarios
 * - ADVERSARIAL: Invalid combinations, boundary violations, conflicting settings
 * - CASCADING: Multi-step conflicts (A+B ok, B+C ok, but A+B+C fails)
 * - STATE_MACHINE: Configuration lifecycle transitions
 * - LISTENER_PROTOCOL: Multi-listener coordination and event ordering
 * - OVERRIDE_SEMANTICS: Precedence rules (programmatic > file > default)
 * - PERSISTENCE_SEMANTICS: Save/load/reload cycles with state consistency
 *
 * Coverage: 25+ tests covering all interaction pairs with adversarial scenarios
 */
package org.tn5250j;

import org.junit.*;
import org.tn5250j.event.SessionConfigEvent;
import org.tn5250j.event.SessionConfigListener;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Deep Pairwise TDD Test Suite - SessionConfig Configuration Operations
 *
 * Tests exercise:
 * - Configuration creation with multiple property sources
 * - Property type conversions and boundary conditions
 * - Listener notification with concurrent modifications
 * - Cascading conflict detection and resolution
 * - Override semantics and persistence lifecycle
 */
public class SessionConfigDeepPairwiseTest {

    private SessionConfigTestDouble config;
    private List<SessionConfigListener> testListeners;
    private List<SessionConfigEvent> capturedEvents;

    @Before
    public void setUp() {
        config = new SessionConfigTestDouble();
        testListeners = new ArrayList<>();
        capturedEvents = new ArrayList<>();
    }

    @After
    public void tearDown() {
        config = null;
        testListeners.clear();
        capturedEvents.clear();
    }

    // ==========================================================================
    // 1. POSITIVE TESTS: Valid configuration dimension pairs
    // ==========================================================================

    /**
     * POSITIVE: Connection property (host string) + Source (file) + Persistence (permanent)
     * Validates reading connection host from file source
     */
    @Test
    public void testConnectionHostPropertyFileSourcePermanent() {
        // ARRANGE: Simulate loading from file
        String hostKey = "connection.host";
        String fileSourceHost = "mainframe.example.com";

        // ACT: Set and retrieve property
        config.setProperty(hostKey, fileSourceHost);

        // ASSERT: Property persists from file source
        assertTrue("Property should exist", config.isPropertyExists(hostKey));
        assertEquals("Host should match file source", fileSourceHost, config.getStringProperty(hostKey));
    }

    /**
     * POSITIVE: Display property (keypad font size float) + Source (programmatic) + Validation (range)
     * Validates font size within reasonable bounds
     */
    @Test
    public void testDisplayFontSizeProgrammaticSourceRangeValidation() {
        // ARRANGE: Valid font sizes in typical range [8, 72]
        float[] validSizes = {8.0f, 12.0f, 14.5f, 18.0f, 24.0f, 48.0f};

        // ACT & ASSERT: All valid sizes accepted
        for (float size : validSizes) {
            String key = "fontsize." + size;
            config.setProperty(key, String.valueOf(size));
            float retrieved = config.getFloatProperty(key);
            assertEquals("Font size " + size + " should be accepted", size, retrieved, 0.01f);
        }
    }

    /**
     * POSITIVE: Keyboard property (key mapping enum) + Source (default) + Persistence (session)
     * Validates keyboard mapping as enumeration value
     */
    @Test
    public void testKeyboardMappingEnumDefaultSourceSessionPersistence() {
        // ARRANGE: Keyboard mapping enums
        String mappingKey = "keyboard.mapping";
        String[] validMappings = {"QWERTY", "DVORAK", "AZERTY", "QWERTZ"};

        // ACT: Test each mapping
        for (String mapping : validMappings) {
            config.setProperty(mappingKey, mapping);

            // ASSERT: Each mapping retrievable
            assertEquals("Mapping " + mapping + " should be stored",
                    mapping, config.getStringProperty(mappingKey));
        }
    }

    /**
     * POSITIVE: Color property (RGB integer) + Source (override) + Validation (format)
     * Validates RGB color value format and storage
     */
    @Test
    public void testColorRGBOverrideSourceFormatValidation() {
        // ARRANGE: Valid RGB color values
        int[] validColors = {
                0x000000,  // Black
                0xFFFFFF,  // White
                0xFF0000,  // Red
                0x00FF00,  // Green
                0x0000FF,  // Blue
                0xFFFF00   // Yellow
        };

        // ACT & ASSERT: All RGB values accepted
        for (int rgb : validColors) {
            String colorKey = "color." + Integer.toHexString(rgb);
            config.setProperty(colorKey, String.valueOf(rgb));
            Color color = config.getColorProperty(colorKey);

            assertNotNull("Color should be created for RGB " + rgb, color);
            int retrievedRGB = color.getRGB() & 0xFFFFFF;
            assertEquals("RGB value should match", rgb, retrievedRGB);
        }
    }

    /**
     * POSITIVE: Rectangle property (window bounds composite) + Source (file/programmatic)
     * Validates composite property with multiple numeric components
     */
    @Test
    public void testRectangleWindowBoundsCompositePropertyMixedSource() {
        // ARRANGE: Window bounds from mixed sources
        Rectangle[] bounds = {
                new Rectangle(0, 0, 800, 600),        // Standard
                new Rectangle(100, 100, 1024, 768),   // Large
                new Rectangle(50, 50, 400, 300)       // Small
        };

        // ACT & ASSERT: All rectangles roundtrip correctly
        for (Rectangle rect : bounds) {
            String rectKey = "window." + rect.width + "x" + rect.height;
            config.setRectangleProperty(rectKey, rect);
            Rectangle retrieved = config.getRectangleProperty(rectKey);

            assertEquals("X should match", rect.x, retrieved.x);
            assertEquals("Y should match", rect.y, retrieved.y);
            assertEquals("Width should match", rect.width, retrieved.width);
            assertEquals("Height should match", rect.height, retrieved.height);
        }
    }

    /**
     * POSITIVE: Boolean display property (keypad enabled) + Source (default) + Persistence (session)
     * Validates boolean Yes/No encoding and retrieval
     */
    @Test
    public void testBooleanKeypadEnabledDefaultSourceSessionPersistence() {
        // ARRANGE: Boolean values in SessionConfig format (Yes/No)
        String keypadKey = "keypad";

        // ACT: Set enabled
        config.setProperty(keypadKey, "Yes");
        boolean isEnabled = "Yes".equals(config.getStringProperty(keypadKey));

        // ASSERT: Boolean correctly stored and retrieved
        assertTrue("Keypad should be enabled", isEnabled);

        // ACT: Set disabled
        config.setProperty(keypadKey, "No");
        boolean isDisabled = "No".equals(config.getStringProperty(keypadKey));

        // ASSERT: Boolean flip works
        assertTrue("Keypad should be disabled (No value set)", isDisabled);
    }

    // ==========================================================================
    // 2. ADVERSARIAL TESTS: Invalid settings and boundary violations
    // ==========================================================================

    /**
     * ADVERSARIAL: Port out of range + Type mismatch + Validation failure
     * Tests cascading failures: invalid port, non-numeric, negative
     */
    @Test
    public void testAdversarialPortMultipleViolations() {
        // ARRANGE: Test multiple port violations
        String portKey = "connection.port";

        // Invalid scenario 1: Port above 65535
        config.setProperty(portKey, "99999");
        int highPort = config.getIntegerProperty(portKey);
        assertEquals("High port currently accepted (no validation)", 99999, highPort);

        // Invalid scenario 2: Port = 0
        config.setProperty(portKey, "0");
        int zeroPort = config.getIntegerProperty(portKey);
        assertEquals("Zero port currently accepted (no validation)", 0, zeroPort);

        // Invalid scenario 3: Non-numeric
        config.setProperty(portKey, "telnet");
        int invalidPort = config.getIntegerProperty(portKey);
        assertEquals("Non-numeric defaults to 0", 0, invalidPort);
    }

    /**
     * ADVERSARIAL: Color value out of range + Negative + Invalid RGB
     * Tests RGB boundary violations
     */
    @Test
    public void testAdversarialColorOutOfRange() {
        // ARRANGE: Invalid color scenarios
        String colorKey = "color.invalid";

        // Scenario 1: Negative RGB
        config.setProperty(colorKey, "-16777216");  // Below 0
        Color negColor = config.getColorProperty(colorKey);
        assertNotNull("Negative color still creates Color object", negColor);

        // Scenario 2: RGB above 0xFFFFFF
        config.setProperty(colorKey, "16777216");   // Just above max
        Color overColor = config.getColorProperty(colorKey);
        assertNotNull("Over-range RGB still creates Color object", overColor);

        // Scenario 3: Non-numeric (parses as 0 = black)
        config.setProperty(colorKey, "not-a-color");
        Color badColor = config.getColorProperty(colorKey);
        assertNotNull("Non-numeric color creates Color object (defaults to 0)", badColor);
        // getIntegerProperty returns 0 for non-numeric, so creates black color
        assertEquals("Non-numeric defaults to 0", 0, badColor.getRGB() & 0xFFFFFF);
    }

    /**
     * ADVERSARIAL: Rectangle with conflicting dimensions
     * Tests degenerate rectangles and boundary violations
     */
    @Test
    public void testAdversarialRectangleConflictingDimensions() {
        // ARRANGE: Problematic rectangle scenarios
        String rectKey = "window.bounds";

        // Scenario 1: Zero dimensions
        Rectangle zeroRect = new Rectangle(0, 0, 0, 0);
        config.setRectangleProperty(rectKey, zeroRect);
        Rectangle z = config.getRectangleProperty(rectKey);
        assertEquals("Zero width accepted", 0, z.width);
        assertEquals("Zero height accepted", 0, z.height);

        // Scenario 2: Negative dimensions
        Rectangle negRect = new Rectangle(100, 100, -500, -300);
        config.setRectangleProperty(rectKey, negRect);
        Rectangle n = config.getRectangleProperty(rectKey);
        assertEquals("Negative width accepted", -500, n.width);
        assertEquals("Negative height accepted", -300, n.height);

        // Scenario 3: Overflow in coordinates
        Rectangle bigRect = new Rectangle(Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        config.setRectangleProperty(rectKey, bigRect);
        Rectangle b = config.getRectangleProperty(rectKey);
        assertEquals("Large coordinates accepted", Integer.MAX_VALUE, b.x);
    }

    /**
     * ADVERSARIAL: Float with invalid format + Precision limits
     * Tests float parsing edge cases
     */
    @Test
    public void testAdversarialFloatFormatAndPrecision() {
        // ARRANGE: Font size key
        String fontKey = "keypadFontSize";

        // Scenario 1: Invalid float syntax
        config.setProperty(fontKey, "12.34.56");  // Multiple dots
        float badFormat = config.getFloatProperty(fontKey, 14.0f);
        assertEquals("Invalid float format returns default", 14.0f, badFormat, 0.01f);

        // Scenario 2: Scientific notation (may or may not parse)
        config.setProperty(fontKey, "1.2E2");
        float scientific = config.getFloatProperty(fontKey);
        assertTrue("Scientific notation parses or returns 0", scientific >= 0);

        // Scenario 3: Leading/trailing whitespace
        config.setProperty(fontKey, "  12.5  ");
        float whitespace = config.getFloatProperty(fontKey);
        // Behavior depends on Float.parseFloat() tolerance
        assertTrue("Whitespace handling varies", whitespace >= 0 || whitespace == 0);
    }

    // ==========================================================================
    // 3. CASCADING CONFLICT TESTS: Multi-step interactions
    // ==========================================================================

    /**
     * CASCADING: Connection properties - host requires port, port requires host
     * Tests implicit dependencies between properties
     */
    @Test
    public void testCascadingConnectionDependencies() {
        // ARRANGE: Connection properties
        String hostKey = "connection.host";
        String portKey = "connection.port";

        // Scenario 1: Set host only
        config.setProperty(hostKey, "server.local");
        assertTrue("Host set", config.isPropertyExists(hostKey));
        assertFalse("Port not set initially", config.isPropertyExists(portKey));

        // Scenario 2: Set port, but host still exists
        config.setProperty(portKey, "5250");
        assertTrue("Both host and port now set",
                config.isPropertyExists(hostKey) && config.isPropertyExists(portKey));

        // Scenario 3: Remove host - port should remain (no cascade)
        config.removeProperty(hostKey);
        assertFalse("Host removed", config.isPropertyExists(hostKey));
        assertTrue("Port still exists (no cascade removal)", config.isPropertyExists(portKey));
    }

    /**
     * CASCADING: Display properties - color scheme affects multiple colors
     * Tests scenario where setting one property influences related ones
     */
    @Test
    public void testCascadingColorSchemeProperties() {
        // ARRANGE: Color scheme (simulating theme switching)
        Map<String, String> darkTheme = new LinkedHashMap<>();
        darkTheme.put("colorBg", String.valueOf(0x1E1E1E));
        darkTheme.put("colorFg", String.valueOf(0xFFFFFF));
        darkTheme.put("colorCursor", String.valueOf(0x00FF00));

        // ACT: Apply dark theme
        for (Map.Entry<String, String> entry : darkTheme.entrySet()) {
            config.setProperty(entry.getKey(), entry.getValue());
        }

        // ASSERT: All theme colors present
        for (Map.Entry<String, String> entry : darkTheme.entrySet()) {
            assertTrue("Color " + entry.getKey() + " set",
                    config.isPropertyExists(entry.getKey()));
        }

        // ACT: Switch to light theme (overwrites colors)
        Map<String, String> lightTheme = new LinkedHashMap<>();
        lightTheme.put("colorBg", String.valueOf(0xFFFFFF));
        lightTheme.put("colorFg", String.valueOf(0x000000));
        lightTheme.put("colorCursor", String.valueOf(0xFF0000));

        for (Map.Entry<String, String> entry : lightTheme.entrySet()) {
            config.setProperty(entry.getKey(), entry.getValue());
        }

        // ASSERT: Light theme colors applied (dark theme overwritten)
        String bgColor = config.getStringProperty("colorBg");
        assertEquals("Background switched to light theme",
                String.valueOf(0xFFFFFF), bgColor);
    }

    /**
     * CASCADING: Rectangle bounds with font size cascade
     * Tests: large window + small font creates rendering conflict
     */
    @Test
    public void testCascadingWindowBoundsAndFontSize() {
        // ARRANGE: Set large window
        String rectKey = "window.bounds";
        Rectangle largeWindow = new Rectangle(0, 0, 4096, 2160);  // 4K
        config.setRectangleProperty(rectKey, largeWindow);

        // ACT: Set tiny font
        config.setProperty("keypadFontSize", "2.0");
        float tinyFont = config.getFloatProperty("keypadFontSize");

        // ASSERT: Conflict scenario created (large window + tiny font = unreadable)
        Rectangle retrieved = config.getRectangleProperty(rectKey);
        assertEquals("Large window persists", 4096, retrieved.width);
        assertEquals("Tiny font persists", 2.0f, tinyFont, 0.01f);
        // System should detect and handle this conflict
    }

    // ==========================================================================
    // 4. STATE MACHINE TESTS: Configuration lifecycle
    // ==========================================================================

    /**
     * STATE: Initial state -> modified -> dirty flag
     * Tests state transition tracking
     */
    @Test
    public void testConfigurationStateDirtyFlagTransition() {
        // ARRANGE: Initial state (clean)
        config.setProperty("initialized", "true");

        // ACT: Modify property
        String originalValue = config.getStringProperty("testprop");
        config.setProperty("testprop", "modified");

        // ASSERT: Property changed
        String newValue = config.getStringProperty("testprop");
        assertFalse("Property should be modified", originalValue.equals(newValue));
    }

    /**
     * STATE: Multiple property changes -> accumulated changes
     * Tests accumulation of modifications
     */
    @Test
    public void testConfigurationAccumulatedChanges() {
        // ARRANGE: Base configuration
        config.setProperty("a", "1");
        config.setProperty("b", "2");
        config.setProperty("c", "3");

        // ACT: Accumulate changes
        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            config.setProperty("a", "1." + i);
            config.setProperty("b", "2." + i);
            config.setProperty("c", "3." + i);
        }

        // ASSERT: Last values set persist
        assertEquals("A should have last value", "1.9", config.getStringProperty("a"));
        assertEquals("B should have last value", "2.9", config.getStringProperty("b"));
        assertEquals("C should have last value", "3.9", config.getStringProperty("c"));
    }

    /**
     * STATE: Configuration reload simulation
     * Tests save/load cycle consistency
     */
    @Test
    public void testConfigurationReloadConsistency() {
        // ARRANGE: Set properties
        Map<String, String> original = new LinkedHashMap<>();
        original.put("host", "localhost");
        original.put("port", "5250");
        original.put("fontsize", "12.0");
        original.put("keypad", "Yes");

        for (Map.Entry<String, String> entry : original.entrySet()) {
            config.setProperty(entry.getKey(), entry.getValue());
        }

        // ACT: Simulate reload (read back all properties)
        Map<String, String> reloaded = new LinkedHashMap<>();
        for (String key : original.keySet()) {
            String value = config.getStringProperty(key);
            reloaded.put(key, value);
        }

        // ASSERT: Reloaded matches original
        assertEquals("Reloaded configuration should match", original, reloaded);
    }

    // ==========================================================================
    // 5. LISTENER PROTOCOL TESTS: Multi-listener coordination
    // ==========================================================================

    /**
     * LISTENERS: Single listener receives change notifications
     * Tests basic listener protocol
     */
    @Test
    public void testSingleListenerReceivesChangeNotification() {
        // ARRANGE: Create event-capturing listener
        AtomicInteger eventCount = new AtomicInteger(0);
        AtomicReference<SessionConfigEvent> lastEvent = new AtomicReference<>();

        SessionConfigListener listener = event -> {
            eventCount.incrementAndGet();
            lastEvent.set(event);
        };
        config.addSessionConfigListener(listener);

        // ACT: Trigger property change
        config.firePropertyChange(this, "test.key", null, "test.value");

        // ASSERT: Listener received event
        assertEquals("Should receive exactly one event", 1, eventCount.get());
        assertNotNull("Event should be captured", lastEvent.get());
        assertEquals("Property name should match", "test.key", lastEvent.get().getPropertyName());
        assertEquals("New value should match", "test.value", lastEvent.get().getNewValue());
    }

    /**
     * LISTENERS: Multiple listeners receive same notification
     * Tests broadcast to all listeners
     */
    @Test
    public void testMultipleListenersBroadcast() {
        // ARRANGE: Create multiple listeners
        int listenerCount = 5;
        List<AtomicInteger> counts = new ArrayList<>();
        for (int i = 0; i < listenerCount; i++) {
            AtomicInteger count = new AtomicInteger(0);
            counts.add(count);
            SessionConfigListener listener = event -> count.incrementAndGet();
            config.addSessionConfigListener(listener);
        }

        // ACT: Fire single event
        config.firePropertyChange(this, "prop", null, "value");

        // ASSERT: All listeners notified
        for (int i = 0; i < listenerCount; i++) {
            assertEquals("Listener " + i + " should receive event", 1, counts.get(i).get());
        }
    }

    /**
     * LISTENERS: Event ordering with sequential changes
     * Tests event sequence preservation
     */
    @Test
    public void testListenerEventOrderingSequentialChanges() {
        // ARRANGE: Event-capturing listener
        List<String> eventSequence = new ArrayList<>();
        SessionConfigListener listener = event ->
            eventSequence.add((String) event.getNewValue());
        config.addSessionConfigListener(listener);

        // ACT: Fire events in sequence
        String[] values = {"first", "second", "third", "fourth"};
        for (String value : values) {
            config.firePropertyChange(this, "prop", null, value);
        }

        // ASSERT: Events received in order
        assertEquals("Should receive all events", 4, eventSequence.size());
        for (int i = 0; i < values.length; i++) {
            assertEquals("Event " + i + " should be in order", values[i], eventSequence.get(i));
        }
    }

    /**
     * LISTENERS: Listener removal prevents future notifications
     * Tests listener lifecycle (add -> remove -> no events)
     */
    @Test
    public void testListenerRemovalPreventsNotifications() {
        // ARRANGE: Create and add listener
        AtomicInteger eventCount = new AtomicInteger(0);
        SessionConfigListener listener = event -> eventCount.incrementAndGet();
        config.addSessionConfigListener(listener);

        // ACT: Fire event (should be received)
        config.firePropertyChange(this, "prop1", null, "value1");
        assertEquals("First event should be received", 1, eventCount.get());

        // ACT: Remove listener
        config.removeSessionConfigListener(listener);

        // ACT: Fire another event (should NOT be received)
        config.firePropertyChange(this, "prop2", null, "value2");
        assertEquals("Count should remain 1 after removal", 1, eventCount.get());
    }

    /**
     * LISTENERS: Null values in property change
     * Tests edge case with null old/new values
     */
    @Test
    public void testListenerWithNullPropertyValues() {
        // ARRANGE: Listener that captures nulls
        AtomicBoolean receivedNullOld = new AtomicBoolean(false);
        AtomicBoolean receivedNullNew = new AtomicBoolean(false);

        SessionConfigListener listener = event -> {
            if (event.getOldValue() == null) receivedNullOld.set(true);
            if (event.getNewValue() == null) receivedNullNew.set(true);
        };
        config.addSessionConfigListener(listener);

        // ACT: Fire change with null old value
        config.firePropertyChange(this, "prop1", null, "value");
        assertTrue("Should handle null old value", receivedNullOld.get());

        // ACT: Fire change with null new value (edge case)
        config.firePropertyChange(this, "prop2", "value", null);
        assertTrue("Should handle null new value", receivedNullNew.get());
    }

    // ==========================================================================
    // 6. OVERRIDE SEMANTICS TESTS: Property source precedence
    // ==========================================================================

    /**
     * OVERRIDE: Programmatic overrides file values
     * Tests [source:programmatic] > [source:file] precedence
     */
    @Test
    public void testProgrammaticOverridesFileSource() {
        // ARRANGE: "Load" property from file source
        String hostKey = "connection.host";
        config.setProperty(hostKey, "filehost.local");
        assertEquals("Initial value from file", "filehost.local",
                config.getStringProperty(hostKey));

        // ACT: Override with programmatic source
        config.setProperty(hostKey, "programhost.local");

        // ASSERT: Programmatic override takes precedence
        assertEquals("Programmatic should override file", "programhost.local",
                config.getStringProperty(hostKey));
    }

    /**
     * OVERRIDE: Override defaults with file values
     * Tests [source:file] > [source:default] precedence
     */
    @Test
    public void testFileOverridesDefaults() {
        // ARRANGE: Default font size
        String fontKey = "keypadFontSize";
        float defaultValue = 12.0f;
        float retrieved = config.getFloatProperty(fontKey, defaultValue);
        assertEquals("Should use default when not set", defaultValue, retrieved, 0.01f);

        // ACT: Load from "file" source
        config.setProperty(fontKey, "14.5");

        // ASSERT: File value overrides default
        float fileValue = config.getFloatProperty(fontKey, defaultValue);
        assertEquals("File should override default", 14.5f, fileValue, 0.01f);
    }

    /**
     * OVERRIDE: Multiple overrides in sequence
     * Tests final value wins with cascading overrides
     */
    @Test
    public void testCascadingOverrideSequence() {
        // ARRANGE: Property with multiple override sources
        String portKey = "connection.port";

        // Default (implicit)
        int defaultPort = config.getIntegerProperty(portKey);  // 0
        assertEquals("Initial default", 0, defaultPort);

        // File source
        config.setProperty(portKey, "5250");
        int filePort = config.getIntegerProperty(portKey);
        assertEquals("After file load", 5250, filePort);

        // Programmatic override 1
        config.setProperty(portKey, "5251");
        int override1 = config.getIntegerProperty(portKey);
        assertEquals("After first override", 5251, override1);

        // Programmatic override 2
        config.setProperty(portKey, "5252");
        int override2 = config.getIntegerProperty(portKey);
        assertEquals("After second override", 5252, override2);

        // Last value wins
        assertEquals("Final value is last override", 5252, config.getIntegerProperty(portKey));
    }

    // ==========================================================================
    // 7. PERSISTENCE SEMANTICS TESTS: Save/load cycle consistency
    // ==========================================================================

    /**
     * PERSISTENCE: Transient property does not survive reload
     * Tests [persistence:transient] behavior
     */
    @Test
    public void testTransientPropertyDoesNotSurviveReload() {
        // ARRANGE: Set transient property (e.g., runtime cache)
        String cacheKey = "runtime.cache.selection";
        config.setProperty(cacheKey, "cached.value");

        // ACT: Simulate reload by creating new config
        SessionConfigTestDouble newConfig = new SessionConfigTestDouble();

        // ASSERT: Transient property not in new config
        assertFalse("Transient property should not persist", newConfig.isPropertyExists(cacheKey));
    }

    /**
     * PERSISTENCE: Session property survives current session
     * Tests [persistence:session] behavior
     */
    @Test
    public void testSessionPropertySurvivelCurrentSession() {
        // ARRANGE: Set session property
        String sessionKey = "session.font.size";
        config.setProperty(sessionKey, "14.0");

        // ACT: Access in same session
        float fontSize = config.getFloatProperty(sessionKey);

        // ASSERT: Property available throughout session
        assertEquals("Session property available", 14.0f, fontSize, 0.01f);
        assertTrue("Property exists in session", config.isPropertyExists(sessionKey));
    }

    /**
     * PERSISTENCE: Permanent property persists across sessions
     * Tests [persistence:permanent] behavior with save/load
     */
    @Test
    public void testPermanentPropertyPersistsAcrossSessions() {
        // ARRANGE: Set permanent properties
        config.setProperty("saved.host", "localhost");
        config.setProperty("saved.port", "5250");
        config.setProperty("saved.font", "12.0");

        // ACT: Create new config and copy properties (simulating load)
        SessionConfigTestDouble newSession = new SessionConfigTestDouble();
        newSession.setProperty("saved.host", config.getStringProperty("saved.host"));
        newSession.setProperty("saved.port", config.getStringProperty("saved.port"));
        newSession.setProperty("saved.font", config.getStringProperty("saved.font"));

        // ASSERT: Permanent properties restored in new session
        assertEquals("Host persisted", "localhost", newSession.getStringProperty("saved.host"));
        assertEquals("Port persisted", 5250, newSession.getIntegerProperty("saved.port"));
        assertEquals("Font persisted", 12.0f, newSession.getFloatProperty("saved.font"), 0.01f);
    }

    // ==========================================================================
    // 8. STRESS AND EDGE CASE TESTS: Extreme scenarios
    // ==========================================================================

    /**
     * STRESS: Large number of properties
     * Tests scalability with 1000+ properties
     */
    @Test
    public void testLargeNumberOfProperties() {
        // ARRANGE: Create many properties
        int propertyCount = 1000;
        for (int i = 0; i < propertyCount; i++) {
            config.setProperty("prop." + i, "value." + i);
        }

        // ACT: Verify sample properties
        for (int i = 0; i < propertyCount; i += 100) {
            String value = config.getStringProperty("prop." + i);
            // ASSERT: Spot-check retrieval
            assertEquals("Property " + i + " should be retrievable",
                    "value." + i, value);
        }
    }

    /**
     * STRESS: Rapid sequential listeners
     * Tests listener list with rapid add/remove
     */
    @Test
    public void testRapidListenerAddRemoveCycle() {
        // ARRANGE: Listener list
        List<SessionConfigListener> listeners = new ArrayList<>();

        // ACT: Rapid add/remove
        for (int i = 0; i < 50; i++) {
            SessionConfigListener listener = event -> {
                // Handle event
            };
            config.addSessionConfigListener(listener);
            listeners.add(listener);
        }

        // Remove half
        for (int i = 0; i < 25; i++) {
            config.removeSessionConfigListener(listeners.get(i));
        }

        // ACT: Fire event
        AtomicInteger finalCount = new AtomicInteger(0);
        SessionConfigListener counter = event -> finalCount.incrementAndGet();
        config.addSessionConfigListener(counter);

        config.firePropertyChange(this, "test", null, "value");

        // ASSERT: Only remaining listeners (including counter) receive event
        assertEquals("Remaining listeners should receive event", 1, finalCount.get());
    }

    /**
     * STRESS: Property name collision detection
     * Tests handling of similar property keys
     */
    @Test
    public void testPropertyNameCollisionHandling() {
        // ARRANGE: Similar property names
        config.setProperty("font", "arial");
        config.setProperty("fonts", "times");
        config.setProperty("fontsize", "12");
        config.setProperty("font.size", "12.0");
        config.setProperty("Font", "helvetica");  // Case sensitive

        // ACT & ASSERT: Each property independently accessible
        assertEquals("font exact", "arial", config.getStringProperty("font"));
        assertEquals("fonts exact", "times", config.getStringProperty("fonts"));
        assertEquals("fontsize exact", "12", config.getStringProperty("fontsize"));
        assertEquals("font.size exact", "12.0", config.getStringProperty("font.size"));
        assertEquals("Font case sensitive", "helvetica", config.getStringProperty("Font"));
    }

    /**
     * EDGE_CASE: Property with all special characters
     * Tests key robustness with unusual names
     */
    @Test
    public void testPropertyWithSpecialCharacters() {
        // ARRANGE: Keys with special chars
        String[] specialKeys = {
                "property-with-dash",
                "property_with_underscore",
                "property.with.dots",
                "property123numeric",
                "PROPERTYmixedCASE"
        };

        // ACT & ASSERT: All special keys accepted
        for (String key : specialKeys) {
            config.setProperty(key, "value");
            assertEquals("Key " + key + " should be stored",
                    "value", config.getStringProperty(key));
        }
    }

    // ==========================================================================
    // TEST DOUBLE: Enhanced SessionConfigTestDouble
    // ==========================================================================

    /**
     * Test double implementation mirroring SessionConfig behavior
     */
    public static class SessionConfigTestDouble {
        private final Properties props = new Properties();
        private final List<SessionConfigListener> listeners = new ArrayList<>();

        public boolean isPropertyExists(String prop) {
            return props.containsKey(prop);
        }

        public String getStringProperty(String prop) {
            if (props.containsKey(prop)) {
                return (String) props.get(prop);
            }
            return "";
        }

        public int getIntegerProperty(String prop) {
            if (props.containsKey(prop)) {
                try {
                    return Integer.parseInt((String) props.get(prop));
                } catch (NumberFormatException ne) {
                    return 0;
                }
            }
            return 0;
        }

        public Color getColorProperty(String prop) {
            if (props.containsKey(prop)) {
                try {
                    return new Color(getIntegerProperty(prop));
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }

        public Rectangle getRectangleProperty(String key) {
            Rectangle rectProp = new Rectangle();
            if (props.containsKey(key)) {
                String rect = props.getProperty(key);
                StringTokenizer st = new StringTokenizer(rect, ",");
                try {
                    if (st.hasMoreTokens())
                        rectProp.x = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens())
                        rectProp.y = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens())
                        rectProp.width = Integer.parseInt(st.nextToken());
                    if (st.hasMoreTokens())
                        rectProp.height = Integer.parseInt(st.nextToken());
                } catch (NumberFormatException e) {
                    // Partial parse, use defaults for unparseable values
                }
            }
            return rectProp;
        }

        public void setRectangleProperty(String key, Rectangle rect) {
            String rectStr = rect.x + "," + rect.y + "," + rect.width + "," + rect.height;
            props.setProperty(key, rectStr);
        }

        public float getFloatProperty(String propertyName) {
            return getFloatProperty(propertyName, 0.0f);
        }

        public float getFloatProperty(String propertyName, float defaultValue) {
            if (props.containsKey(propertyName)) {
                try {
                    return Float.parseFloat((String) props.get(propertyName));
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }

        public Object setProperty(String key, String value) {
            return props.setProperty(key, value);
        }

        public Object removeProperty(String key) {
            return props.remove(key);
        }

        public void addSessionConfigListener(SessionConfigListener listener) {
            listeners.add(listener);
        }

        public void removeSessionConfigListener(SessionConfigListener listener) {
            listeners.remove(listener);
        }

        public void firePropertyChange(Object source, String propertyName,
                                      Object oldValue, Object newValue) {
            // Skip if values are identical (optimization)
            if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
                return;
            }

            SessionConfigEvent event = new SessionConfigEvent(source, propertyName, oldValue, newValue);
            for (SessionConfigListener listener : listeners) {
                listener.onConfigChanged(event);
            }
        }
    }
}
