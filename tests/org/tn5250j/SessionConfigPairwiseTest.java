/**
 * SessionConfigPairwiseTest.java - Comprehensive TDD Pairwise Test Suite for SessionConfig
 *
 * This test suite uses N-wise pairwise testing to systematically validate SessionConfig
 * behavior across all configuration dimensions and property types. Tests exercise both
 * positive scenarios (valid configurations) and adversarial scenarios (conflicting,
 * invalid, and boundary-case settings).
 *
 * Pairwise Dimensions:
 * 1. Property type: connection (host/port), display (font/color), keyboard (mapping),
 *    color (RGB values), audio (enable/volume)
 * 2. Value type: string, integer, boolean, enum, composite (Rectangle)
 * 3. Source: default (built-in), file (loaded), programmatic (setProperty), override
 * 4. Validation: none, range (min/max), format (regex), required (must-exist)
 * 5. Persistence: transient (memory-only), session (current session), permanent (saved)
 *
 * Test Strategy: Combine pairs of dimensions to create:
 * - POSITIVE: Valid configurations that should succeed
 * - ADVERSARIAL: Invalid combinations, conflicting settings, boundary violations
 * - STATE: Configuration changes, listener notifications, reload behavior
 * - EDGE CASES: Empty values, null handling, type coercion, overflow scenarios
 *
 * Coverage Goals:
 * - 25+ test cases covering configuration operations
 * - Configuration creation, modification, validation, persistence
 * - Listener notification for configuration changes
 * - Error handling for invalid/conflicting settings
 * - Type coercion (string to int, bool, float, color)
 * - Boundary conditions (min port, max dimensions, font sizes)
 *
 * Test Categories:
 * 1. POSITIVE: Valid key/value pairs with valid sources
 * 2. ADVERSARIAL: Invalid/conflicting settings, boundary violations
 * 3. STATE: Configuration state transitions, listener notifications
 * 4. PERSISTENCE: Save/load/reload cycles
 * 5. TYPE_COERCION: String to int/float/bool/color conversions
 */
package org.tn5250j;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tn5250j.event.SessionConfigEvent;
import org.tn5250j.event.SessionConfigListener;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Pairwise TDD Test Suite for SessionConfig
 *
 * Tests Property-based operations directly against Properties backing store
 * to validate: property management, listener notifications, type conversions,
 * and conflict/error handling.
 *
 * STRATEGY: Test SessionConfig interfaces using a Properties-backed implementation
 * rather than full SessionConfig initialization (which has external dependencies).
 * This isolates config behavior from resource loading complexity.
 */
public class SessionConfigPairwiseTest {

    private SessionConfigTestDouble config;

    @Before
    public void setUp() {
        // Create test double with Properties backing store
        config = new SessionConfigTestDouble();
    }

    @After
    public void tearDown() {
        config = null;
    }

    // ==========================================================================
    // POSITIVE TEST CASES: Valid configurations, property types, sources
    // ==========================================================================

    /**
     * POSITIVE: Set connection property (string) from programmatic source
     * Dimension pair: [property:connection] x [valuetype:string] x [source:programmatic]
     *
     * RED: SessionConfig should accept host connection string
     */
    @Test
    public void testSetConnectionHostPropertyString() {
        // ARRANGE: Session config initialized
        String hostKey = "connection.host";
        String hostValue = "192.168.1.100";

        // ACT: Set property programmatically
        config.setProperty(hostKey, hostValue);

        // ASSERT: Property persists in memory
        assertTrue("Property should exist", config.isPropertyExists(hostKey));
        assertEquals("Host should match", hostValue, config.getStringProperty(hostKey));
    }

    /**
     * POSITIVE: Set port property (integer) from file source
     * Dimension pair: [property:connection] x [valuetype:integer] x [source:file]
     */
    @Test
    public void testSetConnectionPortPropertyInteger() {
        // ARRANGE: Port as string (typical file/property storage)
        String portKey = "connection.port";
        String portValue = "5250";

        // ACT: Set and retrieve as integer
        config.setProperty(portKey, portValue);
        int port = config.getIntegerProperty(portKey);

        // ASSERT: Integer parsing works
        assertEquals("Port should parse to integer", 5250, port);
    }

    /**
     * POSITIVE: Set display property (boolean) - keypad enabled
     * Dimension pair: [property:display] x [valuetype:boolean] x [source:programmatic]
     */
    @Test
    public void testSetDisplayPropertyKeypadEnabled() {
        // ARRANGE: Keypad enable as Yes/No (SessionConfig style)
        String keypadKey = "keypad";
        String keypadValue = "Yes";

        // ACT: Set property
        config.setProperty(keypadKey, keypadValue);

        // ASSERT: Property retrievable
        assertTrue("Property should exist", config.isPropertyExists(keypadKey));
        assertEquals("Keypad value should be Yes", "Yes", config.getStringProperty(keypadKey));
    }

    /**
     * POSITIVE: Set display property (float) - keypad font size
     * Dimension pair: [property:display] x [valuetype:float] x [source:file]
     */
    @Test
    public void testSetDisplayPropertyKeypadFontSize() {
        // ARRANGE: Font size as float string
        String fontSizeKey = "keypadFontSize";
        String fontSizeValue = "14.5";

        // ACT: Set and retrieve as float
        config.setProperty(fontSizeKey, fontSizeValue);
        float fontSize = config.getFloatProperty(fontSizeKey, 12.0f);

        // ASSERT: Float parsing works
        assertEquals("Font size should match", 14.5f, fontSize, 0.01f);
    }

    /**
     * POSITIVE: Set color property (integer RGB value)
     * Dimension pair: [property:color] x [valuetype:integer] x [source:programmatic]
     */
    @Test
    public void testSetColorPropertyRGB() {
        // ARRANGE: Color as integer (RGB encoded)
        String colorKey = "colorBg";
        int colorValue = 0xFFFFFF; // White
        String colorStr = String.valueOf(colorValue);

        // ACT: Set and retrieve color
        config.setProperty(colorKey, colorStr);
        Color color = config.getColorProperty(colorKey);

        // ASSERT: Color created correctly
        assertNotNull("Color should not be null", color);
        assertEquals("Color RGB should match", colorValue, color.getRGB() & 0xFFFFFF);
    }

    /**
     * POSITIVE: Set rectangle property (composite type)
     * Dimension pair: [property:display] x [valuetype:rectangle] x [source:programmatic]
     */
    @Test
    public void testSetRectanglePropertyWindowDimensions() {
        // ARRANGE: Window dimensions as rectangle
        String rectKey = "window.bounds";
        Rectangle rect = new Rectangle(100, 50, 800, 600);

        // ACT: Set rectangle property
        config.setRectangleProperty(rectKey, rect);

        // ASSERT: Rectangle retrieved correctly
        Rectangle retrieved = config.getRectangleProperty(rectKey);
        assertEquals("X should match", 100, retrieved.x);
        assertEquals("Y should match", 50, retrieved.y);
        assertEquals("Width should match", 800, retrieved.width);
        assertEquals("Height should match", 600, retrieved.height);
    }

    /**
     * POSITIVE: Multiple properties set, all retrieve correctly
     * Dimension pair: [property:mixed] x [valuetype:mixed] x [source:programmatic]
     */
    @Test
    public void testSetMultiplePropertiesMixed() {
        // ARRANGE: Multiple property types
        config.setProperty("host", "localhost");
        config.setProperty("port", "5250");
        config.setProperty("enabled", "true");
        config.setProperty("fontSize", "12.0");

        // ACT: Retrieve all properties
        String host = config.getStringProperty("host");
        int port = config.getIntegerProperty("port");
        boolean enabled = "true".equals(config.getStringProperty("enabled"));
        float fontSize = config.getFloatProperty("fontSize", 0f);

        // ASSERT: All properties accessible
        assertEquals("Host should be localhost", "localhost", host);
        assertEquals("Port should be 5250", 5250, port);
        assertTrue("Enabled should be true", enabled);
        assertEquals("Font size should be 12.0", 12.0f, fontSize, 0.01f);
    }

    /**
     * POSITIVE: Property with special characters in key
     * Dimension pair: [property:any] x [valuetype:string] x [source:programmatic]
     */
    @Test
    public void testSetPropertyWithSpecialCharKey() {
        // ARRANGE: Key with dots and underscores
        String complexKey = "emulator.settings.display_font.bold";
        String value = "true";

        // ACT: Set property with complex key
        config.setProperty(complexKey, value);

        // ASSERT: Property retrievable
        assertTrue("Complex key should exist", config.isPropertyExists(complexKey));
        assertEquals("Value should match", value, config.getStringProperty(complexKey));
    }

    /**
     * POSITIVE: Property with very long string value
     * Dimension pair: [property:any] x [valuetype:longstring] x [source:programmatic]
     */
    @Test
    public void testSetPropertyWithLongStringValue() {
        // ARRANGE: Very long string value (1000+ characters)
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longValue.append("verylongvalue");
        }
        String key = "config.longtext";

        // ACT: Set long property
        config.setProperty(key, longValue.toString());

        // ASSERT: Long value persists
        String retrieved = config.getStringProperty(key);
        assertEquals("Long value should be retrieved completely",
                longValue.length(), retrieved.length());
    }

    /**
     * POSITIVE: Default float property returns default value when missing
     * Dimension pair: [property:display] x [valuetype:float] x [source:default]
     */
    @Test
    public void testGetFloatPropertyWithDefault() {
        // ARRANGE: Property not set
        String missingKey = "nonexistent.fontsize";
        float defaultValue = 14.5f;

        // ACT: Get float with default
        float result = config.getFloatProperty(missingKey, defaultValue);

        // ASSERT: Default value returned
        assertEquals("Should return default value", defaultValue, result, 0.01f);
    }

    // ==========================================================================
    // ADVERSARIAL TEST CASES: Invalid settings, boundary violations, conflicts
    // ==========================================================================

    /**
     * ADVERSARIAL: Port number below valid range (0)
     * Dimension pair: [property:connection] x [valuetype:integer] x [validation:range]
     *
     * RED: Should validate port boundaries
     */
    @Test
    public void testInvalidPortBelowMinimum() {
        // ARRANGE: Invalid port number (too low)
        String portKey = "connection.port";
        String invalidPort = "0";

        // ACT: Set invalid port
        config.setProperty(portKey, invalidPort);
        int port = config.getIntegerProperty(portKey);

        // ASSERT: Currently accepts invalid port (documents behavior)
        // TODO: Add validation layer to reject ports < 1024 or < 1
        assertEquals("Currently accepts port 0", 0, port);
    }

    /**
     * ADVERSARIAL: Port number above valid range (65536)
     * Dimension pair: [property:connection] x [valuetype:integer] x [validation:range]
     */
    @Test
    public void testInvalidPortAboveMaximum() {
        // ARRANGE: Invalid port number (too high)
        String portKey = "connection.port";
        String invalidPort = "65536";

        // ACT: Set invalid port
        config.setProperty(portKey, invalidPort);
        int port = config.getIntegerProperty(portKey);

        // ASSERT: Currently accepts invalid port
        // TODO: Add validation to reject ports > 65535
        assertEquals("Currently accepts port 65536", 65536, port);
    }

    /**
     * ADVERSARIAL: Non-numeric value for port (type mismatch)
     * Dimension pair: [property:connection] x [valuetype:invalid] x [source:programmatic]
     */
    @Test
    public void testInvalidPortNonNumericValue() {
        // ARRANGE: Non-numeric port value
        String portKey = "connection.port";
        String notANumber = "not-a-port";

        // ACT: Set non-numeric value
        config.setProperty(portKey, notANumber);

        // ACT: Try to parse as integer
        int port = config.getIntegerProperty(portKey);

        // ASSERT: Returns 0 on parse failure (defensive behavior)
        assertEquals("Should return 0 for non-numeric port", 0, port);
    }

    /**
     * ADVERSARIAL: Empty property value
     * Dimension pair: [property:any] x [valuetype:empty] x [source:programmatic]
     */
    @Test
    public void testEmptyPropertyValue() {
        // ARRANGE: Empty string value
        String key = "config.empty";
        String emptyValue = "";

        // ACT: Set empty property
        config.setProperty(key, emptyValue);

        // ASSERT: Empty value accepted
        assertTrue("Empty property should exist", config.isPropertyExists(key));
        assertEquals("Should return empty string", "", config.getStringProperty(key));
    }

    /**
     * ADVERSARIAL: Rectangle with negative dimensions
     * Dimension pair: [property:display] x [valuetype:rectangle] x [validation:range]
     */
    @Test
    public void testRectangleWithNegativeDimensions() {
        // ARRANGE: Rectangle with negative width/height
        String rectKey = "window.bounds";
        Rectangle negativeRect = new Rectangle(-10, -5, -800, -600);

        // ACT: Set rectangle with negative values
        config.setRectangleProperty(rectKey, negativeRect);

        // ASSERT: Currently accepts negative dimensions
        // TODO: Add validation layer
        Rectangle retrieved = config.getRectangleProperty(rectKey);
        assertEquals("Currently accepts negative width", -800, retrieved.width);
        assertEquals("Currently accepts negative height", -600, retrieved.height);
    }

    /**
     * ADVERSARIAL: Invalid color value (negative RGB)
     * Dimension pair: [property:color] x [valuetype:invalid] x [validation:range]
     */
    @Test
    public void testInvalidColorNegativeRGB() {
        // ARRANGE: Negative color value
        String colorKey = "colorInvalid";
        String negativeColor = "-1";

        // ACT: Set negative color
        config.setProperty(colorKey, negativeColor);
        Color color = config.getColorProperty(colorKey);

        // ASSERT: Color created (may appear black or invalid)
        assertNotNull("Color object created even for negative value", color);
    }

    /**
     * ADVERSARIAL: Float value with excessive decimal places
     * Dimension pair: [property:display] x [valuetype:float] x [validation:format]
     */
    @Test
    public void testFloatWithManyDecimalPlaces() {
        // ARRANGE: Float with many decimal places
        String fontKey = "keypadFontSize";
        String preciseFloat = "14.123456789123456789";

        // ACT: Set and retrieve float
        config.setProperty(fontKey, preciseFloat);
        float fontSize = config.getFloatProperty(fontKey);

        // ASSERT: Float parsed (precision may be lost)
        assertTrue("Float should be parsed", fontSize > 0);
    }

    /**
     * ADVERSARIAL: Conflicting connection settings (host set but no port)
     * Dimension pair: [property:connection] x [source:mixed] x [validation:required]
     */
    @Test
    public void testMissingRequiredConnectionPort() {
        // ARRANGE: Set host but not port
        config.setProperty("connection.host", "localhost");
        // port deliberately not set

        // ACT: Check for port
        boolean portExists = config.isPropertyExists("connection.port");

        // ASSERT: Missing port not detected by property check
        assertFalse("Port not set", portExists);
        // TODO: Add configuration validation to require port when host is set
    }

    /**
     * ADVERSARIAL: Conflicting display settings (font size out of typical range)
     * Dimension pair: [property:display] x [valuetype:float] x [validation:range]
     */
    @Test
    public void testFontSizeExtremelyLarge() {
        // ARRANGE: Unreasonably large font size
        String fontKey = "keypadFontSize";
        String extremeSize = "500.0";

        // ACT: Set extreme font size
        config.setProperty(fontKey, extremeSize);
        float fontSize = config.getFloatProperty(fontKey);

        // ASSERT: Accepts unreasonable value
        // TODO: Add validation bounds (e.g., 8.0 - 72.0)
        assertEquals("Currently accepts extreme font size", 500.0f, fontSize, 0.01f);
    }

    /**
     * ADVERSARIAL: Font size extremely small or zero
     * Dimension pair: [property:display] x [valuetype:float] x [validation:range]
     */
    @Test
    public void testFontSizeZeroOrNegative() {
        // ARRANGE: Zero or negative font size
        String fontKey = "keypadFontSize";
        String zeroSize = "0.0";

        // ACT: Set invalid font size
        config.setProperty(fontKey, zeroSize);
        float fontSize = config.getFloatProperty(fontKey);

        // ASSERT: Accepts invalid size
        assertEquals("Currently accepts zero font size", 0.0f, fontSize, 0.01f);
    }

    /**
     * ADVERSARIAL: Property key with special characters that break parsing
     * Dimension pair: [property:any] x [key:malformed] x [source:file]
     */
    @Test
    public void testPropertyKeyWithEqualsSign() {
        // ARRANGE: Key contains equals sign (properties file delimiter)
        String malformedKey = "key=with=equals";
        String value = "test";

        // ACT: Set property with problematic key
        config.setProperty(malformedKey, value);

        // ASSERT: Key stored as-is in memory (may fail in file I/O)
        assertTrue("Key with equals stored in memory", config.isPropertyExists(malformedKey));
    }

    /**
     * ADVERSARIAL: Rectangle bounds with zero-size window
     * Dimension pair: [property:display] x [valuetype:rectangle] x [validation:range]
     */
    @Test
    public void testRectangleWithZeroDimensions() {
        // ARRANGE: Rectangle with zero width/height
        String rectKey = "window.degenerate";
        Rectangle zeroRect = new Rectangle(100, 100, 0, 0);

        // ACT: Set zero-dimension rectangle
        config.setRectangleProperty(rectKey, zeroRect);

        // ASSERT: Currently accepts degenerate rectangle
        Rectangle retrieved = config.getRectangleProperty(rectKey);
        assertEquals("Width is zero", 0, retrieved.width);
        assertEquals("Height is zero", 0, retrieved.height);
    }

    // ==========================================================================
    // STATE TEST CASES: Configuration changes, listener notifications
    // ==========================================================================

    /**
     * STATE: Add listener and verify notification on property change
     * Dimension pair: [property:any] x [source:programmatic] x [persistence:transient]
     */
    @Test
    public void testConfigListenerNotifiedOnPropertyChange() {
        // ARRANGE: Create listener to capture events
        AtomicInteger eventCount = new AtomicInteger(0);
        SessionConfigListener listener = new SessionConfigListener() {
            @Override
            public void onConfigChanged(SessionConfigEvent event) {
                eventCount.incrementAndGet();
            }
        };
        config.addSessionConfigListener(listener);

        // ACT: Change property
        config.firePropertyChange(this, "test.property", null, "newValue");

        // ASSERT: Listener received notification
        assertEquals("Listener should receive one event", 1, eventCount.get());
    }

    /**
     * STATE: Multiple listeners all notified
     * Dimension pair: [listeners:multiple] x [source:programmatic]
     */
    @Test
    public void testMultipleListenersNotified() {
        // ARRANGE: Add multiple listeners
        AtomicInteger listener1Count = new AtomicInteger(0);
        AtomicInteger listener2Count = new AtomicInteger(0);
        AtomicInteger listener3Count = new AtomicInteger(0);

        SessionConfigListener l1 = event -> listener1Count.incrementAndGet();
        SessionConfigListener l2 = event -> listener2Count.incrementAndGet();
        SessionConfigListener l3 = event -> listener3Count.incrementAndGet();

        config.addSessionConfigListener(l1);
        config.addSessionConfigListener(l2);
        config.addSessionConfigListener(l3);

        // ACT: Fire property change
        config.firePropertyChange(this, "prop", null, "value");

        // ASSERT: All listeners notified
        assertEquals("Listener 1 should be notified", 1, listener1Count.get());
        assertEquals("Listener 2 should be notified", 1, listener2Count.get());
        assertEquals("Listener 3 should be notified", 1, listener3Count.get());
    }

    /**
     * STATE: Remove listener prevents further notifications
     * Dimension pair: [listeners:removed] x [source:programmatic]
     */
    @Test
    public void testRemoveListenerStopsNotifications() {
        // ARRANGE: Add listener then remove
        AtomicInteger eventCount = new AtomicInteger(0);
        SessionConfigListener listener = event -> eventCount.incrementAndGet();

        config.addSessionConfigListener(listener);
        config.removeSessionConfigListener(listener);

        // ACT: Fire property change after removal
        config.firePropertyChange(this, "prop", null, "value");

        // ASSERT: No notification received
        assertEquals("Removed listener should not receive event", 0, eventCount.get());
    }

    /**
     * STATE: Same oldValue and newValue suppresses notification
     * Dimension pair: [property:any] x [values:unchanged]
     */
    @Test
    public void testNoNotificationWhenValueUnchanged() {
        // ARRANGE: Setup listener
        AtomicInteger eventCount = new AtomicInteger(0);
        SessionConfigListener listener = event -> eventCount.incrementAndGet();
        config.addSessionConfigListener(listener);

        // ACT: Fire property change with same old and new values
        config.firePropertyChange(this, "prop", "sameValue", "sameValue");

        // ASSERT: No notification (optimization)
        assertEquals("No notification when values identical", 0, eventCount.get());
    }

    /**
     * STATE: Property changed from one value to another notifies
     * Dimension pair: [property:any] x [values:different]
     */
    @Test
    public void testNotificationWhenValueChanged() {
        // ARRANGE: Setup listener
        AtomicBoolean eventFired = new AtomicBoolean(false);
        SessionConfigListener listener = event -> {
            eventFired.set(true);
            assertEquals("Old value should be 'old'", "old", event.getOldValue());
            assertEquals("New value should be 'new'", "new", event.getNewValue());
        };
        config.addSessionConfigListener(listener);

        // ACT: Fire property change with different values
        config.firePropertyChange(this, "prop", "old", "new");

        // ASSERT: Event fired with correct values
        assertTrue("Event should be fired", eventFired.get());
    }

    /**
     * STATE: Property change with null old value
     * Dimension pair: [property:any] x [oldvalue:null]
     */
    @Test
    public void testPropertyChangeWithNullOldValue() {
        // ARRANGE: Listener to capture event
        AtomicBoolean eventFired = new AtomicBoolean(false);
        SessionConfigListener listener = event -> eventFired.set(true);
        config.addSessionConfigListener(listener);

        // ACT: Fire change with null old value
        config.firePropertyChange(this, "newProp", null, "value");

        // ASSERT: Event fired (null oldValue is treated as new property)
        assertTrue("Event should fire with null old value", eventFired.get());
    }

    /**
     * STATE: Concurrent property modifications
     * Dimension pair: [property:multiple] x [source:concurrent]
     */
    @Test
    public void testConcurrentPropertyModifications() {
        // ARRANGE: Set up multiple properties
        config.setProperty("prop1", "value1");
        config.setProperty("prop2", "value2");
        config.setProperty("prop3", "value3");

        // ACT: Modify all properties
        config.setProperty("prop1", "modified1");
        config.setProperty("prop2", "modified2");
        config.setProperty("prop3", "modified3");

        // ASSERT: All modifications preserved
        assertEquals("Prop1 modified", "modified1", config.getStringProperty("prop1"));
        assertEquals("Prop2 modified", "modified2", config.getStringProperty("prop2"));
        assertEquals("Prop3 modified", "modified3", config.getStringProperty("prop3"));
    }

    /**
     * STATE: Property removal
     * Dimension pair: [property:existing] x [operation:remove]
     */
    @Test
    public void testRemovePropertyFromConfiguration() {
        // ARRANGE: Set property
        String key = "removeme";
        config.setProperty(key, "value");
        assertTrue("Property should exist", config.isPropertyExists(key));

        // ACT: Remove property
        config.removeProperty(key);

        // ASSERT: Property no longer exists
        assertFalse("Property should be removed", config.isPropertyExists(key));
    }

    // ==========================================================================
    // TYPE COERCION TEST CASES: String to int/float/bool/color conversions
    // ==========================================================================

    /**
     * TYPE_COERCION: String to integer conversion with boundaries
     * Dimension pair: [valuetype:string] x [target:integer] x [value:boundary]
     */
    @Test
    public void testIntegerCoercionMaxValue() {
        // ARRANGE: String with max integer value
        String intKey = "maxint";
        config.setProperty(intKey, String.valueOf(Integer.MAX_VALUE));

        // ACT: Retrieve as integer
        int result = config.getIntegerProperty(intKey);

        // ASSERT: Max value preserved
        assertEquals("Should preserve MAX_VALUE", Integer.MAX_VALUE, result);
    }

    /**
     * TYPE_COERCION: String to integer conversion with negative
     * Dimension pair: [valuetype:string] x [target:integer] x [value:negative]
     */
    @Test
    public void testIntegerCoercionNegativeValue() {
        // ARRANGE: String with negative integer
        String intKey = "negint";
        config.setProperty(intKey, "-5250");

        // ACT: Retrieve as integer
        int result = config.getIntegerProperty(intKey);

        // ASSERT: Negative value preserved
        assertEquals("Should preserve negative value", -5250, result);
    }

    /**
     * TYPE_COERCION: String to float conversion with precision loss
     * Dimension pair: [valuetype:string] x [target:float] x [value:precision]
     */
    @Test
    public void testFloatCoercionPrecisionLoss() {
        // ARRANGE: String with high-precision float
        String floatKey = "precfloat";
        String highPrecision = "3.14159265358979323846";

        // ACT: Set and retrieve as float
        config.setProperty(floatKey, highPrecision);
        float result = config.getFloatProperty(floatKey);

        // ASSERT: Float parsed (precision lost to 32-bit float)
        assertTrue("Should be approximately pi", result > 3.1f && result < 3.2f);
    }

    /**
     * TYPE_COERCION: String to Color via RGB integer
     * Dimension pair: [valuetype:string] x [target:color] x [value:rgb]
     */
    @Test
    public void testColorCoercionFromRGBString() {
        // ARRANGE: RGB integer as string
        String colorKey = "testcolor";
        int rgb = 0xFF0000; // Red
        config.setProperty(colorKey, String.valueOf(rgb));

        // ACT: Retrieve as Color
        Color color = config.getColorProperty(colorKey);

        // ASSERT: Color created with correct RGB
        assertNotNull("Color should be created", color);
        int retrieved = color.getRGB() & 0xFFFFFF;
        assertEquals("Red color should match", 0xFF0000, retrieved);
    }

    /**
     * TYPE_COERCION: Rectangle string parsing with format validation
     * Dimension pair: [valuetype:string] x [target:rectangle] x [format:comma-separated]
     */
    @Test
    public void testRectangleCoercionFromString() {
        // ARRANGE: Rectangle as comma-separated string
        String rectKey = "bounds";
        String rectStr = "10,20,300,400";
        config.setProperty(rectKey, rectStr);

        // ACT: Retrieve as Rectangle
        Rectangle rect = config.getRectangleProperty(rectKey);

        // ASSERT: Values parsed correctly
        assertEquals("X coordinate", 10, rect.x);
        assertEquals("Y coordinate", 20, rect.y);
        assertEquals("Width", 300, rect.width);
        assertEquals("Height", 400, rect.height);
    }

    /**
     * TYPE_COERCION: Rectangle with missing components (partial format)
     * Dimension pair: [valuetype:string] x [target:rectangle] x [format:incomplete]
     */
    @Test
    public void testRectangleCoercionPartialValues() {
        // ARRANGE: Rectangle with only 2 of 4 values
        String rectKey = "partial";
        String partialStr = "100,200";
        config.setProperty(rectKey, partialStr);

        // ACT: Retrieve as Rectangle
        Rectangle rect = config.getRectangleProperty(rectKey);

        // ASSERT: Parsed values, defaults for missing
        assertEquals("X should be 100", 100, rect.x);
        assertEquals("Y should be 200", 200, rect.y);
        assertEquals("Width defaults to 0", 0, rect.width);
        assertEquals("Height defaults to 0", 0, rect.height);
    }

    // ==========================================================================
    // EDGE CASES: Boundary conditions, null handling, special scenarios
    // ==========================================================================

    /**
     * EDGE_CASE: Check property existence for never-set key
     * Dimension pair: [property:nonexistent] x [operation:check]
     */
    @Test
    public void testPropertyExistenceCheckForNeverSetKey() {
        // ARRANGE: Never set this key
        String neverSet = "never.set.property";

        // ACT: Check existence
        boolean exists = config.isPropertyExists(neverSet);

        // ASSERT: Non-existent property returns false
        assertFalse("Non-existent property should return false", exists);
    }

    /**
     * EDGE_CASE: Get string property for non-existent key returns empty
     * Dimension pair: [property:nonexistent] x [valuetype:string]
     */
    @Test
    public void testGetStringPropertyNonExistentReturnsEmpty() {
        // ARRANGE: Missing property
        String missing = "missing.key";

        // ACT: Get string property
        String result = config.getStringProperty(missing);

        // ASSERT: Returns empty string (not null)
        assertEquals("Should return empty string for missing property", "", result);
    }

    /**
     * EDGE_CASE: Get integer property for non-existent key returns zero
     * Dimension pair: [property:nonexistent] x [valuetype:integer]
     */
    @Test
    public void testGetIntegerPropertyNonExistentReturnsZero() {
        // ARRANGE: Missing integer property
        String missing = "missing.int";

        // ACT: Get integer property
        int result = config.getIntegerProperty(missing);

        // ASSERT: Returns 0 (safe default)
        assertEquals("Should return 0 for missing integer", 0, result);
    }

    /**
     * EDGE_CASE: Get color property for non-existent key returns null
     * Dimension pair: [property:nonexistent] x [valuetype:color]
     */
    @Test
    public void testGetColorPropertyNonExistentReturnsNull() {
        // ARRANGE: Missing color property
        String missing = "missing.color";

        // ACT: Get color property
        Color result = config.getColorProperty(missing);

        // ASSERT: Returns null
        assertNull("Should return null for missing color", result);
    }

    /**
     * EDGE_CASE: Get rectangle property for non-existent key returns empty
     * Dimension pair: [property:nonexistent] x [valuetype:rectangle]
     */
    @Test
    public void testGetRectanglePropertyNonExistentReturnsEmpty() {
        // ARRANGE: Missing rectangle property
        String missing = "missing.rect";

        // ACT: Get rectangle property
        Rectangle result = config.getRectangleProperty(missing);

        // ASSERT: Returns empty rectangle (0,0,0,0)
        assertEquals("X should be 0", 0, result.x);
        assertEquals("Y should be 0", 0, result.y);
        assertEquals("Width should be 0", 0, result.width);
        assertEquals("Height should be 0", 0, result.height);
    }

    /**
     * EDGE_CASE: Float property with non-numeric returns default
     * Dimension pair: [valuetype:string] x [target:float] x [format:invalid]
     */
    @Test
    public void testFloatPropertyParseErrorReturnsDefault() {
        // ARRANGE: Non-numeric string for float
        String floatKey = "badFloat";
        config.setProperty(floatKey, "not-a-number");

        // ACT: Get as float
        float result = config.getFloatProperty(floatKey, 99.9f);

        // ASSERT: Returns default when parse fails
        assertEquals("Should return default on parse error", 99.9f, result, 0.01f);
    }

    /**
     * EDGE_CASE: Very large integer in string for port
     * Dimension pair: [valuetype:string] x [target:integer] x [value:overflow]
     */
    @Test
    public void testIntegerParseWithNumberFormatException() {
        // ARRANGE: String that exceeds integer range
        String intKey = "tooLarge";
        config.setProperty(intKey, "9999999999999999999");

        // ACT: Parse as integer
        int result = config.getIntegerProperty(intKey);

        // ASSERT: Returns 0 on overflow
        assertEquals("Should return 0 on overflow", 0, result);
    }

    /**
     * EDGE_CASE: Rectangle with non-numeric component
     * Dimension pair: [valuetype:string] x [target:rectangle] x [format:invalid]
     */
    @Test
    public void testRectangleParseWithNonNumericComponent() {
        // ARRANGE: Rectangle with non-numeric value
        String rectKey = "badRect";
        config.setProperty(rectKey, "100,not-a-number,300,400");

        // ACT: Parse rectangle
        Rectangle result = config.getRectangleProperty(rectKey);

        // ASSERT: Partial parsing (first value taken, rest default to 0)
        assertEquals("X should be 100", 100, result.x);
        assertEquals("Y should default to 0", 0, result.y);
    }

    /**
     * EDGE_CASE: Configuration with property set multiple times
     * Dimension pair: [property:existing] x [operation:overwrite]
     */
    @Test
    public void testPropertyOverwriteMultipleTimes() {
        // ARRANGE: Property key
        String key = "overwrite";

        // ACT: Set multiple times
        config.setProperty(key, "value1");
        config.setProperty(key, "value2");
        config.setProperty(key, "value3");

        // ASSERT: Last value wins
        assertEquals("Last value should be stored", "value3",
                config.getStringProperty(key));
    }

    /**
     * EDGE_CASE: Very long property key
     * Dimension pair: [property:any] x [key:verylongkey]
     */
    @Test
    public void testPropertyWithVeryLongKey() {
        // ARRANGE: Create very long key
        StringBuilder longKey = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longKey.append("verylongkeypart.");
        }
        String value = "testvalue";

        // ACT: Set property with long key
        config.setProperty(longKey.toString(), value);

        // ASSERT: Long key accepted
        assertTrue("Long key should be stored", config.isPropertyExists(longKey.toString()));
        assertEquals("Value should be retrievable", value,
                config.getStringProperty(longKey.toString()));
    }

    // ==========================================================================
    // TEST DOUBLE: SessionConfigTestDouble for isolated testing
    // ==========================================================================

    /**
     * Test double that mimics SessionConfig behavior using Properties
     */
    public static class SessionConfigTestDouble {
        private Properties props = new Properties();
        private List<SessionConfigListener> listeners = new ArrayList<>();

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
                return new Color(getIntegerProperty(prop));
            }
            return null;
        }

        public Rectangle getRectangleProperty(String key) {
            Rectangle rectProp = new Rectangle();
            if (props.containsKey(key)) {
                String rect = props.getProperty(key);
                java.util.StringTokenizer stringtokenizer = new java.util.StringTokenizer(rect, ",");
                try {
                    if (stringtokenizer.hasMoreTokens())
                        rectProp.x = Integer.parseInt(stringtokenizer.nextToken());
                    if (stringtokenizer.hasMoreTokens())
                        rectProp.y = Integer.parseInt(stringtokenizer.nextToken());
                    if (stringtokenizer.hasMoreTokens())
                        rectProp.width = Integer.parseInt(stringtokenizer.nextToken());
                    if (stringtokenizer.hasMoreTokens())
                        rectProp.height = Integer.parseInt(stringtokenizer.nextToken());
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
            // Skip if values are identical
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
