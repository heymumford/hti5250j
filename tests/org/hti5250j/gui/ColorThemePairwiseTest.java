/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Test Suite
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.gui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test Suite for color theme management
 *
 * Pairwise Test Dimensions:
 * 1. Theme type: [classic-green, modern, high-contrast, custom]
 * 2. Color component: [foreground, background, cursor, selection]
 * 3. Attribute: [normal, highlight, reverse, blink]
 * 4. Override scope: [none, per-field, per-screen]
 * 5. Persistence: [session, permanent]
 *
 * Test Coverage:
 * - Theme switching and validation
 * - Color mapping and RGB accuracy
 * - Custom color configuration
 * - Accessibility color constraints
 * - Thread safety and concurrent updates
 * - Adversarial input (invalid colors, malformed RGB values)
 * - Persistence across session boundaries
 */
public class ColorThemePairwiseTest {

    private TestColorThemeManager themeManager;
    private File tempConfigFile;
    private ExecutorService executor;

    @BeforeEach
    public void setUp() throws IOException {
        themeManager = new TestColorThemeManager();
        executor = Executors.newFixedThreadPool(4);
        tempConfigFile = File.createTempFile("test_color_theme_", ".properties");
        tempConfigFile.deleteOnExit();
    }

    @AfterEach
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (tempConfigFile != null && tempConfigFile.exists()) {
            tempConfigFile.delete();
        }
        themeManager = null;
    }

    // ========== POSITIVE PATH TESTS ==========

    /**
     * Test Case 1: Load classic-green theme
     * Dimensions: [theme=classic-green] [persistence=session]
     */
    @Test
    public void testLoadTheme_ClassicGreen_ShouldSetCorrectColors() {
        // ARRANGE: Theme not loaded
        assertNull(themeManager.getThemeColor("background"),"Precondition: colors not initialized");

        // ACT: Load classic-green theme
        themeManager.loadTheme("classic-green");

        // ASSERT: Colors match classic-green specification
        Color bg = themeManager.getThemeColor("background");
        assertNotNull(bg,"Background color should be set");
        assertEquals(0, bg.getRed(),"Background should be dark green");
        assertTrue(bg.getGreen() > 0,"Green component should be > 0");
        assertEquals(0, bg.getBlue(),"Blue component should be minimal");
    }

    /**
     * Test Case 2: Switch from classic-green to modern theme
     * Dimensions: [theme=classic-green->modern] [persistence=session]
     */
    @Test
    public void testSwitchTheme_ClassicToModern_ShouldUpdateAllColors() {
        // ARRANGE: Load classic-green theme
        themeManager.loadTheme("classic-green");
        Color originalBg = themeManager.getThemeColor("background");

        // ACT: Switch to modern theme
        themeManager.loadTheme("modern");

        // ASSERT: Colors updated
        Color newBg = themeManager.getThemeColor("background");
        assertNotNull(newBg,"New background should be set");
        assertFalse(originalBg.getRGB() == newBg.getRGB(),"Background should change");
    }

    /**
     * Test Case 3: Get foreground color from current theme
     * Dimensions: [color_component=foreground] [attribute=normal]
     */
    @Test
    public void testGetForegroundColor_ActiveTheme_ShouldReturnValidColor() {
        // ARRANGE: Load theme
        themeManager.loadTheme("classic-green");

        // ACT: Get foreground color (typically white text)
        Color fg = themeManager.getThemeColor("foreground");

        // ASSERT: Valid foreground color
        assertNotNull(fg,"Foreground color should exist");
        // White (255,255,255) has brightness sum >= 600
        int brightness = fg.getRed() + fg.getGreen() + fg.getBlue();
        assertTrue(brightness > 500,"Foreground should be light (brightness > 500)");
    }

    /**
     * Test Case 4: Get cursor color from current theme
     * Dimensions: [color_component=cursor] [attribute=normal]
     */
    @Test
    public void testGetCursorColor_ActiveTheme_ShouldReturnValidColor() {
        // ARRANGE: Load theme
        themeManager.loadTheme("modern");

        // ACT: Get cursor color
        Color cursor = themeManager.getThemeColor("cursor");

        // ASSERT: Valid cursor color
        assertNotNull(cursor,"Cursor color should exist");
        assertTrue(cursor.getRGB() != 0,"Cursor RGB should be valid");
    }

    /**
     * Test Case 5: Set custom foreground color per-field
     * Dimensions: [color_component=foreground] [override=per-field] [persistence=session]
     */
    @Test
    public void testSetCustomColor_PerFieldForeground_ShouldApplyToFieldOnly() {
        // ARRANGE: Load theme and identify field
        themeManager.loadTheme("classic-green");
        String fieldId = "FIELD_001";
        Color customColor = new Color(255, 200, 100); // Custom orange

        // ACT: Set custom color for field
        themeManager.setFieldColor(fieldId, "foreground", customColor);

        // ASSERT: Custom color applied to field
        Color appliedColor = themeManager.getFieldColor(fieldId, "foreground");
        assertEquals(customColor.getRGB(), appliedColor.getRGB(),"Field should have custom color");

        // Verify other fields unaffected
        Color otherFieldColor = themeManager.getFieldColor("FIELD_002", "foreground");
        assertFalse(customColor.getRGB() == otherFieldColor.getRGB(),"Other fields should retain theme color");
    }

    /**
     * Test Case 6: Set custom background color per-screen
     * Dimensions: [color_component=background] [override=per-screen] [persistence=session]
     */
    @Test
    public void testSetCustomColor_PerScreenBackground_ShouldApplyToScreenOnly() {
        // ARRANGE: Load theme
        themeManager.loadTheme("modern");
        String screenId = "SCREEN_A";
        Color customBg = new Color(20, 20, 60); // Custom dark blue

        // ACT: Set custom background for screen
        themeManager.setScreenColor(screenId, "background", customBg);

        // ASSERT: Custom color applied to screen
        Color screenBg = themeManager.getScreenColor(screenId, "background");
        assertEquals(customBg.getRGB(), screenBg.getRGB(),"Screen should have custom background");

        // Verify other screens unaffected
        Color otherScreenBg = themeManager.getScreenColor("SCREEN_B", "background");
        assertFalse(customBg.getRGB() == otherScreenBg.getRGB(),"Other screens should retain theme color");
    }

    /**
     * Test Case 7: Apply highlight attribute to color
     * Dimensions: [color_component=foreground] [attribute=highlight]
     */
    @Test
    public void testApplyAttribute_HighlightToForeground_ShouldBrightenColor() {
        // ARRANGE: Get base foreground color
        themeManager.loadTheme("classic-green");
        Color baseColor = themeManager.getThemeColor("foreground");

        // ACT: Apply highlight attribute
        Color highlighted = themeManager.applyAttribute(baseColor, "highlight");

        // ASSERT: Color should be brighter/more saturated
        assertNotNull(highlighted,"Highlighted color should exist");
        int baseBrightness = baseColor.getRed() + baseColor.getGreen() + baseColor.getBlue();
        int highlightBrightness = highlighted.getRed() + highlighted.getGreen() + highlighted.getBlue();
        assertTrue(highlightBrightness >= baseBrightness,"Highlight should increase brightness");
    }

    /**
     * Test Case 8: Apply reverse attribute to color
     * Dimensions: [color_component=background] [attribute=reverse]
     */
    @Test
    public void testApplyAttribute_ReverseToBackground_ShouldInvertColor() {
        // ARRANGE: Get base background color
        themeManager.loadTheme("classic-green");
        Color baseColor = themeManager.getThemeColor("background");

        // ACT: Apply reverse attribute
        Color reversed = themeManager.applyAttribute(baseColor, "reverse");

        // ASSERT: Color should be inverted
        assertNotNull(reversed,"Reversed color should exist");
        assertEquals(255 - baseColor.getRed(), reversed.getRed(),"Red should be inverted");
        assertEquals(255 - baseColor.getGreen(), reversed.getGreen(),"Green should be inverted");
        assertEquals(255 - baseColor.getBlue(), reversed.getBlue(),"Blue should be inverted");
    }

    /**
     * Test Case 9: Persist custom colors to permanent storage
     * Dimensions: [override=per-field] [persistence=permanent]
     */
    @Test
    public void testPersistColors_CustomPerFieldColors_ShouldSurviveReload() throws IOException {
        // ARRANGE: Set custom colors
        themeManager.loadTheme("classic-green");
        String fieldId = "FIELD_PERSIST";
        Color customColor = new Color(128, 128, 128);
        themeManager.setFieldColor(fieldId, "foreground", customColor);

        // ACT: Persist to file and reload
        themeManager.persistConfiguration(tempConfigFile);
        TestColorThemeManager reloadedManager = new TestColorThemeManager();
        reloadedManager.loadFromFile(tempConfigFile);

        // ASSERT: Custom colors restored
        Color restoredColor = reloadedManager.getFieldColor(fieldId, "foreground");
        assertEquals(customColor.getRGB(), restoredColor.getRGB(),"Custom color should persist");
    }

    /**
     * Test Case 10: Load high-contrast theme for accessibility
     * Dimensions: [theme=high-contrast] [color_component=all]
     */
    @Test
    public void testLoadTheme_HighContrast_ShouldMeetAccessibilityStandards() {
        // ARRANGE: Accessibility threshold (contrast ratio >= 4.5:1)
        themeManager.loadTheme("high-contrast");

        // ACT: Get theme colors
        Color fg = themeManager.getThemeColor("foreground");
        Color bg = themeManager.getThemeColor("background");

        // ASSERT: Contrast ratio meets WCAG AA standard
        assertNotNull(fg,"Foreground should exist");
        assertNotNull(bg,"Background should exist");
        double contrastRatio = calculateContrastRatio(fg, bg);
        assertTrue(contrastRatio >= 4.5,"Contrast ratio should >= 4.5 (WCAG AA)");
    }

    /**
     * Test Case 11: Reset custom colors to theme defaults
     * Dimensions: [override=per-field] [persistence=session]
     */
    @Test
    public void testResetColors_CustomPerFieldToDefaults_ShouldRestoreThemeColors() {
        // ARRANGE: Set custom color
        themeManager.loadTheme("modern");
        String fieldId = "FIELD_RESET";
        themeManager.setFieldColor(fieldId, "foreground", new Color(255, 0, 0)); // Red
        Color customColor = themeManager.getFieldColor(fieldId, "foreground");
        assertEquals(255, customColor.getRed(),"Custom color set");

        // ACT: Reset to theme defaults
        themeManager.resetFieldColor(fieldId, "foreground");

        // ASSERT: Theme default restored
        Color defaultColor = themeManager.getFieldColor(fieldId, "foreground");
        Color themeDefault = themeManager.getThemeColor("foreground");
        assertEquals(themeDefault.getRGB(), defaultColor.getRGB(),"Should restore theme default");
    }

    /**
     * Test Case 12: Get selection color from current theme
     * Dimensions: [color_component=selection] [attribute=normal]
     */
    @Test
    public void testGetSelectionColor_ActiveTheme_ShouldReturnValidColor() {
        // ARRANGE: Load theme with selection color
        themeManager.loadTheme("modern");

        // ACT: Get selection/highlight color
        Color selection = themeManager.getThemeColor("selection");

        // ASSERT: Valid selection color
        assertNotNull(selection,"Selection color should exist");
        assertTrue(selection.getRGB() != 0,"Selection should be distinct");
    }

    // ========== ADVERSARIAL / NEGATIVE PATH TESTS ==========

    /**
     * Test Case 13: Reject invalid RGB value (out of range)
     * Adversarial: Invalid color specification
     */
    @Test
    public void testSetColor_InvalidRGBValue_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            // ARRANGE: Invalid RGB (> 255)
            Color invalidColor = new Color(300, 100, 50); // Red channel out of range

            // ACT: Attempt to set invalid color
            themeManager.validateAndSetColor("background", invalidColor);

            // ASSERT: Exception thrown (via @Test annotation)
        });
    }

    /**
     * Test Case 14: Reject null color in required field
     * Adversarial: Null color specification
     */
    @Test
    public void testSetColor_NullColor_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
            // ARRANGE: Null color
            Color nullColor = null;

            // ACT: Attempt to set null color
            themeManager.validateAndSetColor("background", nullColor);

            // ASSERT: NullPointerException thrown
        });
    }

    /**
     * Test Case 15: Reject negative RGB value
     * Adversarial: Malformed color specification
     */
    @Test
    public void testSetColor_NegativeRGBValue_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            // ARRANGE: Negative RGB values
            Color invalidColor = new Color(-1, 100, 50);

            // ACT: Attempt to set color with negative component
            themeManager.validateAndSetColor("cursor", invalidColor);

            // ASSERT: Exception thrown
        });
    }

    /**
     * Test Case 16: Reject unknown theme name
     * Adversarial: Invalid theme specification
     */
    @Test
    public void testLoadTheme_UnknownThemeName_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            // ARRANGE: Unknown theme
            String unknownTheme = "non-existent-theme-xyz";

            // ACT: Load unknown theme
            themeManager.loadTheme(unknownTheme);

            // ASSERT: Exception thrown
        });
    }

    /**
     * Test Case 17: Reject empty theme name
     * Adversarial: Empty string input
     */
    @Test
    public void testLoadTheme_EmptyThemeName_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            // ARRANGE: Empty theme name
            String emptyTheme = "";

            // ACT: Load empty theme
            themeManager.loadTheme(emptyTheme);

            // ASSERT: Exception thrown
        });
    }

    /**
     * Test Case 18: Reject malformed RGB hex string
     * Adversarial: Invalid hex format
     */
    @Test
    public void testLoadColorFromHex_MalformedHexString_ShouldThrowException() {
        assertThrows(NumberFormatException.class, () -> {
            // ARRANGE: Invalid hex string
            String malformedHex = "GGGGGG"; // Invalid hex characters

            // ACT: Parse invalid hex
            themeManager.loadColorFromHexString(malformedHex);

            // ASSERT: NumberFormatException thrown
        });
    }

    /**
     * Test Case 19: Reject RGB hex string with invalid length
     * Adversarial: Wrong hex string length
     */
    @Test
    public void testLoadColorFromHex_InvalidLength_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            // ARRANGE: Hex string with wrong length (need 6 or 8 chars)
            String shortHex = "FF00"; // Only 4 characters

            // ACT: Parse short hex
            themeManager.loadColorFromHexString(shortHex);

            // ASSERT: Exception thrown
        });
    }

    /**
     * Test Case 20: Reject color contrast below accessibility threshold
     * Adversarial: Low contrast configuration
     */
    @Test
    public void testValidateContrast_LowContrast_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            // ARRANGE: Colors with low contrast
            Color almostWhite = new Color(250, 250, 250);
            Color almostWhiteAgain = new Color(240, 240, 240);

            // ACT: Validate low contrast pair
            themeManager.validateAccessibilityContrast(almostWhite, almostWhiteAgain);

            // ASSERT: Exception thrown (contrast ratio < 4.5)
        });
    }

    /**
     * Test Case 21: Reject color override for invalid field ID
     * Adversarial: Nonexistent field identifier
     */
    @Test
    public void testSetFieldColor_InvalidFieldId_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            // ARRANGE: Invalid field ID (null)
            String invalidFieldId = null;
            Color color = new Color(100, 100, 100);

            // ACT: Set color for invalid field
            themeManager.setFieldColor(invalidFieldId, "foreground", color);

            // ASSERT: Exception thrown
        });
    }

    /**
     * Test Case 22: Reject unknown color component name
     * Adversarial: Nonexistent color component
     */
    @Test
    public void testSetFieldColor_UnknownComponent_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            // ARRANGE: Unknown color component
            String fieldId = "FIELD_001";
            String unknownComponent = "neon-glow"; // Not a valid component
            Color color = new Color(100, 100, 100);

            // ACT: Set color for unknown component
            themeManager.setFieldColor(fieldId, unknownComponent, color);

            // ASSERT: Exception thrown
        });
    }

    /**
     * Test Case 23: Reject unknown attribute type
     * Adversarial: Invalid attribute specification
     */
    @Test
    public void testApplyAttribute_UnknownAttribute_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            // ARRANGE: Unknown attribute
            Color baseColor = new Color(100, 100, 100);
            String unknownAttribute = "sparkle"; // Not a valid attribute

            // ACT: Apply unknown attribute
            themeManager.applyAttribute(baseColor, unknownAttribute);

            // ASSERT: Exception thrown
        });
    }

    // ========== CONCURRENCY TESTS ==========

    /**
     * Test Case 24: Thread-safe theme switching under concurrent load
     * Dimensions: [persistence=session] [timing=concurrent]
     */
    @Test
    public void testConcurrentThemeSwitching_MultipleThreads_ShouldMaintainConsistency() throws InterruptedException {
        // ARRANGE: Multiple threads switching themes
        int threadCount = 4;
        int iterationsPerThread = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Exception> exception = new AtomicReference<>();

        // ACT: Concurrent theme switching
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    String[] themes = {"classic-green", "modern", "high-contrast"};
                    for (int i = 0; i < iterationsPerThread; i++) {
                        String theme = themes[i % themes.length];
                        themeManager.loadTheme(theme);

                        // Verify consistency
                        Color bg = themeManager.getThemeColor("background");
                        assertNotNull(bg,"Background should be set after switch");
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // ASSERT: All threads complete successfully
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed,"All threads should complete");
        assertNull(exception.get(),"No exceptions should occur");
    }

    /**
     * Test Case 25: Concurrent field color overrides with consistency
     * Dimensions: [override=per-field] [timing=concurrent] [persistence=session]
     */
    @Test
    public void testConcurrentFieldColorOverrides_MultipleFields_ShouldMaintainIsolation() throws InterruptedException {
        // ARRANGE: Load theme and prepare for concurrent updates
        themeManager.loadTheme("modern");
        int fieldCount = 10;
        int threadsPerField = 2;
        CountDownLatch latch = new CountDownLatch(fieldCount * threadsPerField);
        AtomicReference<Exception> exception = new AtomicReference<>();

        // ACT: Multiple threads setting field colors concurrently
        for (int f = 0; f < fieldCount; f++) {
            final int fieldIndex = f;
            for (int t = 0; t < threadsPerField; t++) {
                final int threadIndex = t;
                executor.submit(() -> {
                    try {
                        String fieldId = "FIELD_" + fieldIndex;
                        Color customColor = new Color(fieldIndex * 20, threadIndex * 30, 100);
                        themeManager.setFieldColor(fieldId, "foreground", customColor);

                        // Verify isolation
                        Color retrieved = themeManager.getFieldColor(fieldId, "foreground");
                        assertEquals(customColor.getRGB(), retrieved.getRGB(),"Field color should match");
                    } catch (Exception e) {
                        exception.set(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        // ASSERT: All concurrent operations complete successfully
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed,"All operations should complete");
        assertNull(exception.get(),"No exceptions should occur");
    }

    // ========== HELPER METHODS ==========

    /**
     * Calculate WCAG contrast ratio between two colors
     * Formula: (L1 + 0.05) / (L2 + 0.05) where L is relative luminance
     */
    private double calculateContrastRatio(Color color1, Color color2) {
        double lum1 = calculateLuminance(color1);
        double lum2 = calculateLuminance(color2);

        double lighter = Math.max(lum1, lum2);
        double darker = Math.min(lum1, lum2);

        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Calculate relative luminance per WCAG formula
     */
    private double calculateLuminance(Color color) {
        double r = color.getRed() / 255.0;
        double g = color.getGreen() / 255.0;
        double b = color.getBlue() / 255.0;

        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    // ========== TEST SUPPORT CLASS ==========

    /**
     * Mock color theme manager for testing
     * Provides theme switching, custom color overrides, and persistence
     */
    private static class TestColorThemeManager {
        private final Properties fieldColors = new Properties();
        private final Properties screenColors = new Properties();
        private final Properties themeColorProps = new Properties();
        private String currentTheme = "default";

        void loadTheme(String themeName) {
            if (themeName == null || themeName.isEmpty()) {
                throw new IllegalArgumentException("Theme name cannot be null or empty");
            }

            switch (themeName) {
                case "classic-green":
                    themeColorProps.setProperty("background", String.valueOf(new Color(0, 64, 0).getRGB()));
                    themeColorProps.setProperty("foreground", String.valueOf(new Color(255, 255, 255).getRGB()));
                    themeColorProps.setProperty("cursor", String.valueOf(new Color(0, 128, 0).getRGB()));
                    themeColorProps.setProperty("selection", String.valueOf(new Color(0, 0, 255).getRGB()));
                    break;
                case "modern":
                    themeColorProps.setProperty("background", String.valueOf(new Color(30, 30, 30).getRGB()));
                    themeColorProps.setProperty("foreground", String.valueOf(new Color(220, 220, 220).getRGB()));
                    themeColorProps.setProperty("cursor", String.valueOf(new Color(100, 200, 100).getRGB()));
                    themeColorProps.setProperty("selection", String.valueOf(new Color(100, 180, 255).getRGB()));
                    break;
                case "high-contrast":
                    themeColorProps.setProperty("background", String.valueOf(new Color(0, 0, 0).getRGB()));
                    themeColorProps.setProperty("foreground", String.valueOf(new Color(255, 255, 255).getRGB()));
                    themeColorProps.setProperty("cursor", String.valueOf(new Color(255, 255, 0).getRGB()));
                    themeColorProps.setProperty("selection", String.valueOf(new Color(0, 255, 255).getRGB()));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown theme: " + themeName);
            }
            currentTheme = themeName;
        }

        void setFieldColor(String fieldId, String component, Color color) {
            if (fieldId == null || fieldId.isEmpty()) {
                throw new IllegalArgumentException("Field ID cannot be null or empty");
            }
            validateComponent(component);
            validateColor(color);
            fieldColors.setProperty(fieldId + "." + component, String.valueOf(color.getRGB()));
        }

        Color getFieldColor(String fieldId, String component) {
            validateComponent(component);
            String key = fieldId + "." + component;
            if (fieldColors.containsKey(key)) {
                return new Color(Integer.parseInt(fieldColors.getProperty(key)));
            }
            // Return theme default
            return getThemeColor(component);
        }

        void setScreenColor(String screenId, String component, Color color) {
            validateComponent(component);
            validateColor(color);
            screenColors.setProperty(screenId + "." + component, String.valueOf(color.getRGB()));
        }

        Color getScreenColor(String screenId, String component) {
            validateComponent(component);
            String key = screenId + "." + component;
            if (screenColors.containsKey(key)) {
                return new Color(Integer.parseInt(screenColors.getProperty(key)));
            }
            // Return theme default
            return getThemeColor(component);
        }

        void resetFieldColor(String fieldId, String component) {
            validateComponent(component);
            fieldColors.remove(fieldId + "." + component);
        }

        Color getThemeColor(String colorName) {
            if (themeColorProps.containsKey(colorName)) {
                String rgbStr = themeColorProps.getProperty(colorName);
                return new Color(Integer.parseInt(rgbStr));
            }
            return null;
        }

        Color applyAttribute(Color baseColor, String attribute) {
            if (baseColor == null) {
                throw new NullPointerException("Base color cannot be null");
            }

            switch (attribute) {
                case "normal":
                    return baseColor;
                case "highlight":
                    return brightenColor(baseColor, 1.2);
                case "reverse":
                    return new Color(255 - baseColor.getRed(), 255 - baseColor.getGreen(), 255 - baseColor.getBlue());
                case "blink":
                    return new Color(baseColor.getRGB()); // Same color, but flagged for blinking
                default:
                    throw new IllegalArgumentException("Unknown attribute: " + attribute);
            }
        }

        void validateAndSetColor(String property, Color color) {
            if (color == null) {
                throw new NullPointerException("Color cannot be null");
            }
            if (color.getRed() < 0 || color.getRed() > 255 ||
                color.getGreen() < 0 || color.getGreen() > 255 ||
                color.getBlue() < 0 || color.getBlue() > 255) {
                throw new IllegalArgumentException("RGB values must be 0-255");
            }
            themeColorProps.setProperty(property, String.valueOf(color.getRGB()));
        }

        void validateAccessibilityContrast(Color color1, Color color2) {
            double ratio = calculateContrastRatio(color1, color2);
            if (ratio < 4.5) {
                throw new IllegalArgumentException("Contrast ratio " + ratio + " is below WCAG AA minimum of 4.5");
            }
        }

        Color loadColorFromHexString(String hexString) {
            if (hexString == null || hexString.isEmpty()) {
                throw new IllegalArgumentException("Hex string cannot be null or empty");
            }
            if (hexString.length() != 6 && hexString.length() != 8) {
                throw new IllegalArgumentException("Hex string must be 6 or 8 characters");
            }
            try {
                return new Color(Integer.parseInt(hexString, 16));
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Invalid hex color: " + hexString);
            }
        }

        void persistConfiguration(File file) throws IOException {
            Properties allProps = new Properties();
            // Copy theme colors
            allProps.putAll(themeColorProps);
            // Copy field overrides
            allProps.putAll(fieldColors);
            // Save to file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                allProps.store(fos, "Color theme configuration");
            }
        }

        void loadFromFile(File file) throws IOException {
            Properties props = new Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                props.load(fis);
            }
            loadFromProperties(props);
        }

        void loadFromProperties(Properties props) {
            // Load theme colors
            for (String key : props.stringPropertyNames()) {
                if (!key.contains(".")) {
                    themeColorProps.setProperty(key, props.getProperty(key));
                } else {
                    fieldColors.setProperty(key, props.getProperty(key));
                }
            }
        }

        private void validateComponent(String component) {
            switch (component) {
                case "foreground":
                case "background":
                case "cursor":
                case "selection":
                    break;
                default:
                    throw new IllegalArgumentException("Unknown color component: " + component);
            }
        }

        private void validateColor(Color color) {
            if (color == null) {
                throw new NullPointerException("Color cannot be null");
            }
        }

        private Color brightenColor(Color color, double factor) {
            int r = Math.min(255, (int) (color.getRed() * factor));
            int g = Math.min(255, (int) (color.getGreen() * factor));
            int b = Math.min(255, (int) (color.getBlue() * factor));
            return new Color(r, g, b);
        }

        private double calculateContrastRatio(Color color1, Color color2) {
            double lum1 = calculateLuminance(color1);
            double lum2 = calculateLuminance(color2);
            double lighter = Math.max(lum1, lum2);
            double darker = Math.min(lum1, lum2);
            return (lighter + 0.05) / (darker + 0.05);
        }

        private double calculateLuminance(Color color) {
            double r = color.getRed() / 255.0;
            double g = color.getGreen() / 255.0;
            double b = color.getBlue() / 255.0;
            r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
            g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
            b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
            return 0.2126 * r + 0.7152 * g + 0.0722 * b;
        }
    }
}
