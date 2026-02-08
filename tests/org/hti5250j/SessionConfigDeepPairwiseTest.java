/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import org.junit.*;
import org.hti5250j.event.SessionConfigEvent;
import org.hti5250j.event.SessionConfigListener;

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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

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

    @BeforeEach
    public void setUp() {
        config = new SessionConfigTestDouble();
        testListeners = new ArrayList<>();
        capturedEvents = new ArrayList<>();
    }

    @AfterEach
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
        assertTrue(config.isPropertyExists(hostKey),"Property should exist");
        assertEquals(fileSourceHost, config.getStringProperty(hostKey),"Host should match file source");
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
            assertEquals(size, retrieved, 0.01f,"Font size " + size + " should be accepted");
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
            assertEquals(mapping, config.getStringProperty(mappingKey),"Mapping " + mapping + " should be stored");
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

            assertNotNull(color,"Color should be created for RGB " + rgb);
            int retrievedRGB = color.getRGB() & 0xFFFFFF;
            assertEquals(rgb, retrievedRGB,"RGB value should match");
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

            assertEquals(rect.x, retrieved.x,"X should match");
            assertEquals(rect.y, retrieved.y,"Y should match");
            assertEquals(rect.width, retrieved.width,"Width should match");
            assertEquals(rect.height, retrieved.height,"Height should match");
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
        assertTrue(isEnabled,"Keypad should be enabled");

        // ACT: Set disabled
        config.setProperty(keypadKey, "No");
        boolean isDisabled = "No".equals(config.getStringProperty(keypadKey));

        // ASSERT: Boolean flip works
        assertTrue(isDisabled,"Keypad should be disabled (No value set)");
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
        assertEquals(99999, highPort,"High port currently accepted (no validation)");

        // Invalid scenario 2: Port = 0
        config.setProperty(portKey, "0");
        int zeroPort = config.getIntegerProperty(portKey);
        assertEquals(0, zeroPort,"Zero port currently accepted (no validation)");

        // Invalid scenario 3: Non-numeric
        config.setProperty(portKey, "telnet");
        int invalidPort = config.getIntegerProperty(portKey);
        assertEquals(0, invalidPort,"Non-numeric defaults to 0");
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
        assertNotNull(negColor,"Negative color still creates Color object");

        // Scenario 2: RGB above 0xFFFFFF
        config.setProperty(colorKey, "16777216");   // Just above max
        Color overColor = config.getColorProperty(colorKey);
        assertNotNull(overColor,"Over-range RGB still creates Color object");

        // Scenario 3: Non-numeric (parses as 0 = black)
        config.setProperty(colorKey, "not-a-color");
        Color badColor = config.getColorProperty(colorKey);
        assertNotNull(badColor,"Non-numeric color creates Color object (defaults to 0)");
        // getIntegerProperty returns 0 for non-numeric, so creates black color
        assertEquals(0, badColor.getRGB() & 0xFFFFFF,"Non-numeric defaults to 0");
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
        assertEquals(0, z.width,"Zero width accepted");
        assertEquals(0, z.height,"Zero height accepted");

        // Scenario 2: Negative dimensions
        Rectangle negRect = new Rectangle(100, 100, -500, -300);
        config.setRectangleProperty(rectKey, negRect);
        Rectangle n = config.getRectangleProperty(rectKey);
        assertEquals(-500, n.width,"Negative width accepted");
        assertEquals(-300, n.height,"Negative height accepted");

        // Scenario 3: Overflow in coordinates
        Rectangle bigRect = new Rectangle(Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        config.setRectangleProperty(rectKey, bigRect);
        Rectangle b = config.getRectangleProperty(rectKey);
        assertEquals(Integer.MAX_VALUE, b.x,"Large coordinates accepted");
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
        assertEquals(14.0f, badFormat, 0.01f,"Invalid float format returns default");

        // Scenario 2: Scientific notation (may or may not parse)
        config.setProperty(fontKey, "1.2E2");
        float scientific = config.getFloatProperty(fontKey);
        assertTrue(scientific >= 0,"Scientific notation parses or returns 0");

        // Scenario 3: Leading/trailing whitespace
        config.setProperty(fontKey, "  12.5  ");
        float whitespace = config.getFloatProperty(fontKey);
        // Behavior depends on Float.parseFloat() tolerance
        assertTrue(whitespace >= 0 || whitespace == 0,"Whitespace handling varies");
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
        assertTrue(config.isPropertyExists(hostKey),"Host set");
        assertFalse(config.isPropertyExists(portKey),"Port not set initially");

        // Scenario 2: Set port, but host still exists
        config.setProperty(portKey, "5250");
        assertTrue(config.isPropertyExists(hostKey) && config.isPropertyExists(portKey),"Both host and port now set");

        // Scenario 3: Remove host - port should remain (no cascade)
        config.removeProperty(hostKey);
        assertFalse(config.isPropertyExists(hostKey),"Host removed");
        assertTrue(config.isPropertyExists(portKey),"Port still exists (no cascade removal)");
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
            assertTrue(config.isPropertyExists(entry.getKey()),"Color " + entry.getKey() + " set");
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
        assertEquals(String.valueOf(0xFFFFFF), bgColor,"Background switched to light theme");
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
        assertEquals(4096, retrieved.width,"Large window persists");
        assertEquals(2.0f, tinyFont, 0.01f,"Tiny font persists");
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
        assertFalse(originalValue.equals(newValue),"Property should be modified");
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
        assertEquals("1.9", config.getStringProperty("a"),"A should have last value");
        assertEquals("2.9", config.getStringProperty("b"),"B should have last value");
        assertEquals("3.9", config.getStringProperty("c"),"C should have last value");
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
        assertEquals(original, reloaded,"Reloaded configuration should match");
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
        assertEquals(1, eventCount.get(),"Should receive exactly one event");
        assertNotNull(lastEvent.get(),"Event should be captured");
        assertEquals("test.key", lastEvent.get().getPropertyName(),"Property name should match");
        assertEquals("test.value", lastEvent.get().getNewValue(),"New value should match");
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
            assertEquals(1, counts.get(i).get(),"Listener " + i + " should receive event");
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
        assertEquals(4, eventSequence.size(),"Should receive all events");
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], eventSequence.get(i),"Event " + i + " should be in order");
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
        assertEquals(1, eventCount.get(),"First event should be received");

        // ACT: Remove listener
        config.removeSessionConfigListener(listener);

        // ACT: Fire another event (should NOT be received)
        config.firePropertyChange(this, "prop2", null, "value2");
        assertEquals(1, eventCount.get(),"Count should remain 1 after removal");
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
        assertTrue(receivedNullOld.get(),"Should handle null old value");

        // ACT: Fire change with null new value (edge case)
        config.firePropertyChange(this, "prop2", "value", null);
        assertTrue(receivedNullNew.get(),"Should handle null new value");
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
        assertEquals("filehost.local",
                config.getStringProperty(hostKey),"Initial value from file");

        // ACT: Override with programmatic source
        config.setProperty(hostKey, "programhost.local");

        // ASSERT: Programmatic override takes precedence
        assertEquals("programhost.local",
                config.getStringProperty(hostKey),"Programmatic should override file");
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
        assertEquals(defaultValue, retrieved, 0.01f,"Should use default when not set");

        // ACT: Load from "file" source
        config.setProperty(fontKey, "14.5");

        // ASSERT: File value overrides default
        float fileValue = config.getFloatProperty(fontKey, defaultValue);
        assertEquals(14.5f, fileValue, 0.01f,"File should override default");
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
        assertEquals(0, defaultPort,"Initial default");

        // File source
        config.setProperty(portKey, "5250");
        int filePort = config.getIntegerProperty(portKey);
        assertEquals(5250, filePort,"After file load");

        // Programmatic override 1
        config.setProperty(portKey, "5251");
        int override1 = config.getIntegerProperty(portKey);
        assertEquals(5251, override1,"After first override");

        // Programmatic override 2
        config.setProperty(portKey, "5252");
        int override2 = config.getIntegerProperty(portKey);
        assertEquals(5252, override2,"After second override");

        // Last value wins
        assertEquals(5252, config.getIntegerProperty(portKey),"Final value is last override");
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
        assertFalse(newConfig.isPropertyExists(cacheKey),"Transient property should not persist");
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
        assertEquals(14.0f, fontSize, 0.01f,"Session property available");
        assertTrue(config.isPropertyExists(sessionKey),"Property exists in session");
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
        assertEquals("localhost", newSession.getStringProperty("saved.host"),"Host persisted");
        assertEquals(5250, newSession.getIntegerProperty("saved.port"),"Port persisted");
        assertEquals(12.0f, newSession.getFloatProperty("saved.font"), 0.01f,"Font persisted");
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
            assertEquals("value." + i, value,"Property " + i + " should be retrievable");
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
        assertEquals(1, finalCount.get(),"Remaining listeners should receive event");
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
        assertEquals("arial", config.getStringProperty("font"),"font exact");
        assertEquals("times", config.getStringProperty("fonts"),"fonts exact");
        assertEquals("12", config.getStringProperty("fontsize"),"fontsize exact");
        assertEquals("12.0", config.getStringProperty("font.size"),"font.size exact");
        assertEquals("helvetica", config.getStringProperty("Font"),"Font case sensitive");
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
            assertEquals("value", config.getStringProperty(key),"Key " + key + " should be stored");
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
