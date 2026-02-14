/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hti5250j.event.SessionConfigEvent;
import org.hti5250j.event.SessionConfigListener;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
    public void setUp() {
        // Create test double with Properties backing store
        config = new SessionConfigTestDouble();
    }

    @AfterEach
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
        assertTrue(config.isPropertyExists(hostKey),"Property should exist");
        assertEquals(hostValue, config.getStringProperty(hostKey),"Host should match");
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
        assertEquals(5250, port,"Port should parse to integer");
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
        assertTrue(config.isPropertyExists(keypadKey),"Property should exist");
        assertEquals("Yes", config.getStringProperty(keypadKey),"Keypad value should be Yes");
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
        assertEquals(14.5f, fontSize, 0.01f,"Font size should match");
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
        assertNotNull(color,"Color should not be null");
        assertEquals(colorValue, color.getRGB() & 0xFFFFFF,"Color RGB should match");
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
        assertEquals(100, retrieved.x,"X should match");
        assertEquals(50, retrieved.y,"Y should match");
        assertEquals(800, retrieved.width,"Width should match");
        assertEquals(600, retrieved.height,"Height should match");
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
        assertEquals("localhost", host,"Host should be localhost");
        assertEquals(5250, port,"Port should be 5250");
        assertTrue(enabled,"Enabled should be true");
        assertEquals(12.0f, fontSize, 0.01f,"Font size should be 12.0");
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
        assertTrue(config.isPropertyExists(complexKey),"Complex key should exist");
        assertEquals(value, config.getStringProperty(complexKey),"Value should match");
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
        assertEquals(longValue.length(), retrieved.length(),"Long value should be retrieved completely");
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
        assertEquals(defaultValue, result, 0.01f,"Should return default value");
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

        // ACT + ASSERT: Validation rejects port 0
        assertThrows(IllegalArgumentException.class,
                () -> config.setProperty(portKey, invalidPort),
                "Should reject port below 1");
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

        // ACT + ASSERT: Validation rejects port above 65535
        assertThrows(IllegalArgumentException.class,
                () -> config.setProperty(portKey, invalidPort),
                "Should reject port above 65535");
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

        // ACT + ASSERT: Validation rejects non-numeric port
        assertThrows(IllegalArgumentException.class,
                () -> config.setProperty(portKey, notANumber),
                "Should reject non-numeric port value");
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
        assertTrue(config.isPropertyExists(key),"Empty property should exist");
        assertEquals("", config.getStringProperty(key),"Should return empty string");
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

        // ACT + ASSERT: Validation rejects negative dimensions
        assertThrows(IllegalArgumentException.class,
                () -> config.setRectangleProperty(rectKey, negativeRect),
                "Should reject rectangle with negative dimensions");
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
        assertNotNull(color,"Color object created even for negative value");
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
        assertTrue(fontSize > 0,"Float should be parsed");
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

        // ACT + ASSERT: Cross-property validation detects missing port
        assertThrows(IllegalStateException.class,
                () -> config.validateConfiguration(),
                "Should require port when host is set");
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

        // ACT + ASSERT: Validation rejects font size above 72.0
        assertThrows(IllegalArgumentException.class,
                () -> config.setProperty(fontKey, extremeSize),
                "Should reject font size above 72.0");
    }

    /**
     * ADVERSARIAL: Font size extremely small or zero
     * Dimension pair: [property:display] x [valuetype:float] x [validation:range]
     */
    @Test
    public void testFontSizeZeroOrNegative() {
        // ARRANGE: Zero font size (below minimum 8.0)
        String fontKey = "keypadFontSize";
        String zeroSize = "0.0";

        // ACT + ASSERT: Validation rejects font size below 8.0
        assertThrows(IllegalArgumentException.class,
                () -> config.setProperty(fontKey, zeroSize),
                "Should reject font size below 8.0");
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
        assertTrue(config.isPropertyExists(malformedKey),"Key with equals stored in memory");
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
        assertEquals(0, retrieved.width,"Width is zero");
        assertEquals(0, retrieved.height,"Height is zero");
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
        assertEquals(1, eventCount.get(),"Listener should receive one event");
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
        assertEquals(1, listener1Count.get(),"Listener 1 should be notified");
        assertEquals(1, listener2Count.get(),"Listener 2 should be notified");
        assertEquals(1, listener3Count.get(),"Listener 3 should be notified");
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
        assertEquals(0, eventCount.get(),"Removed listener should not receive event");
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
        assertEquals(0, eventCount.get(),"No notification when values identical");
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
            assertEquals("old", event.getOldValue(),"Old value should be 'old'");
            assertEquals("new", event.getNewValue(),"New value should be 'new'");
        };
        config.addSessionConfigListener(listener);

        // ACT: Fire property change with different values
        config.firePropertyChange(this, "prop", "old", "new");

        // ASSERT: Event fired with correct values
        assertTrue(eventFired.get(),"Event should be fired");
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
        assertTrue(eventFired.get(),"Event should fire with null old value");
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
        assertEquals("modified1", config.getStringProperty("prop1"),"Prop1 modified");
        assertEquals("modified2", config.getStringProperty("prop2"),"Prop2 modified");
        assertEquals("modified3", config.getStringProperty("prop3"),"Prop3 modified");
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
        assertTrue(config.isPropertyExists(key),"Property should exist");

        // ACT: Remove property
        config.removeProperty(key);

        // ASSERT: Property no longer exists
        assertFalse(config.isPropertyExists(key),"Property should be removed");
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
        assertEquals(Integer.MAX_VALUE, result,"Should preserve MAX_VALUE");
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
        assertEquals(-5250, result,"Should preserve negative value");
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
        assertTrue(result > 3.1f && result < 3.2f,"Should be approximately pi");
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
        assertNotNull(color,"Color should be created");
        int retrieved = color.getRGB() & 0xFFFFFF;
        assertEquals(0xFF0000, retrieved,"Red color should match");
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
        assertEquals(10, rect.x,"X coordinate");
        assertEquals(20, rect.y,"Y coordinate");
        assertEquals(300, rect.width,"Width");
        assertEquals(400, rect.height,"Height");
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
        assertEquals(100, rect.x,"X should be 100");
        assertEquals(200, rect.y,"Y should be 200");
        assertEquals(0, rect.width,"Width defaults to 0");
        assertEquals(0, rect.height,"Height defaults to 0");
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
        assertFalse(exists,"Non-existent property should return false");
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
        assertEquals("", result,"Should return empty string for missing property");
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
        assertEquals(0, result,"Should return 0 for missing integer");
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
        assertNull(result,"Should return null for missing color");
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
        assertEquals(0, result.x,"X should be 0");
        assertEquals(0, result.y,"Y should be 0");
        assertEquals(0, result.width,"Width should be 0");
        assertEquals(0, result.height,"Height should be 0");
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
        assertEquals(99.9f, result, 0.01f,"Should return default on parse error");
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
        assertEquals(0, result,"Should return 0 on overflow");
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
        assertEquals(100, result.x,"X should be 100");
        assertEquals(0, result.y,"Y should default to 0");
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
        assertEquals("value3",
                config.getStringProperty(key),"Last value should be stored");
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
        assertTrue(config.isPropertyExists(longKey.toString()),"Long key should be stored");
        assertEquals(value,
                config.getStringProperty(longKey.toString()),"Value should be retrievable");
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
            if (rect.width < 0 || rect.height < 0) {
                throw new IllegalArgumentException(
                        "Rectangle dimensions must be non-negative, got width=" + rect.width + " height=" + rect.height);
            }
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
            validatePropertyValue(key, value);
            return props.setProperty(key, value);
        }

        private void validatePropertyValue(String key, String value) {
            if ("connection.port".equals(key)) {
                try {
                    int port = Integer.parseInt(value);
                    if (port < 1 || port > 65535) {
                        throw new IllegalArgumentException(
                                "Port must be between 1 and 65535, got: " + port);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Port must be a valid integer, got: " + value);
                }
            } else if ("keypadFontSize".equals(key)) {
                try {
                    float size = Float.parseFloat(value);
                    if (size < 8.0f || size > 72.0f) {
                        throw new IllegalArgumentException(
                                "Font size must be between 8.0 and 72.0, got: " + size);
                    }
                } catch (NumberFormatException e) {
                    // Allow non-numeric values through
                }
            }
        }

        public Object removeProperty(String key) {
            return props.remove(key);
        }

        public void validateConfiguration() {
            if (isPropertyExists("connection.host") && !isPropertyExists("connection.port")) {
                throw new IllegalStateException(
                        "connection.port is required when connection.host is set");
            }
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
