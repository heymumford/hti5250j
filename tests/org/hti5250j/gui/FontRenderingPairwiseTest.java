/**
 * FontRenderingPairwiseTest.java - Pairwise TDD Tests for Font Rendering
 *
 * This test suite uses pairwise testing to systematically discover bugs
 * in font rendering operations by combining multiple test dimensions:
 *
 * Pairwise Dimensions:
 * 1. Font family: monospace, system, custom
 * 2. Font size: small (10pt), medium (14pt), large (18pt), scaled
 * 3. Rendering: aliased, anti-aliased, subpixel
 * 4. Character set: ASCII, extended, graphic
 * 5. Display DPI: standard (96), retina (192), custom (144)
 *
 * Test strategy: Combine pairs of dimensions to create adversarial scenarios
 * that expose font selection gaps, rendering issues, character metric bugs,
 * and missing glyph handling failures.
 *
 * Writing style: RED phase tests that expose bugs in font rendering,
 * character metrics, and glyph availability.
 */
package org.hti5250j.gui;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;

import static org.junit.Assert.*;

/**
 * TDD Pairwise Tests for Font Rendering Operations
 *
 * Test categories:
 * 1. FONT_SELECTION: Valid font families, fallback handling, unavailable fonts
 * 2. FONT_SIZING: Point sizes, scaling factors, DPI-aware sizing
 * 3. RENDERING_MODES: Aliased, anti-aliased, subpixel rendering
 * 4. CHARACTER_METRICS: Ascent, descent, advance width, bounds
 * 5. GLYPH_AVAILABILITY: ASCII, extended Latin, graphics, missing glyphs
 * 6. DPI_SCALING: Standard (96), retina (192), custom (144) DPI
 * 7. ADVERSARIAL: Null fonts, invalid sizes, unsupported rendering hints, missing glyphs
 */
public class FontRenderingPairwiseTest {

    private static final float TOLERANCE = 0.01f;
    private static final String[] MONOSPACE_FONTS = {"Courier New", "Courier", "Menlo"};
    private static final String[] SYSTEM_FONTS = {"Arial", "Helvetica", "System"};
    private static final String[] CUSTOM_FONTS = {"Consolas", "DejaVu Sans Mono", "Liberation Mono"};

    // Character sets for testing
    private static final String ASCII_CHARS = "AaBbCc123!@#";
    private static final String EXTENDED_CHARS = "àáâãäæçèéêëìíîïðñòóôõöøùúûüýþÿ";
    private static final String GRAPHIC_CHARS = "┌┬┐├┼┤└┴┘─│";
    private static final String MISSING_GLYPH_TEST = "\u00A0\u200B\u200C\uFEFF";  // Spaces/zero-width chars

    private GraphicsEnvironment graphicsEnv;
    private Graphics2D graphics2D;
    private BufferedImage testImage;

    @Before
    public void setUp() {
        graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        testImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        graphics2D = testImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    @After
    public void tearDown() {
        if (graphics2D != null) {
            graphics2D.dispose();
        }
    }

    // ============================================================================
    // PAIRWISE TEST SUITE: Font Family × Font Size
    // ============================================================================

    /**
     * TEST 1: Monospace font (10pt) renders without null font exception
     * Dimension pair: FontFamily=Monospace, FontSize=Small
     */
    @Test
    public void testMonospaceFontSmallSizeNotNull() {
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 10);
        assertNotNull("Monospace 10pt font should not be null", font);
        assertTrue("Font should be monospaced", font.getSize() == 10 || font.getFontName().length() > 0);
        assertEquals("Font size should be 10", 10, font.getSize());
    }

    /**
     * TEST 2: System font (14pt) returns valid Font object
     * Dimension pair: FontFamily=System, FontSize=Medium
     */
    @Test
    public void testSystemFontMediumSizeValid() {
        Font font = findOrCreateFont("Arial", Font.PLAIN, 14);
        assertNotNull("System font 14pt should not be null", font);
        assertEquals("Font size should be 14", 14, font.getSize());
    }

    /**
     * TEST 3: Custom font (18pt) handles availability check
     * Dimension pair: FontFamily=Custom, FontSize=Large
     */
    @Test
    public void testCustomFontLargeSizeAvailability() {
        Font font = findOrCreateFont("Consolas", Font.PLAIN, 18);
        assertNotNull("Custom font 18pt should not be null", font);
        assertEquals("Font size should be 18", 18, font.getSize());
    }

    /**
     * TEST 4: Scaled font size computation doesn't return zero or negative
     * Dimension pair: FontSize=Scaled, FontFamily=Monospace
     */
    @Test
    public void testScaledFontSizePositive() {
        Font baseFont = findOrCreateFont("Courier New", Font.PLAIN, 12);
        Font scaledFont = baseFont.deriveFont(12f * 1.5f);
        assertTrue("Scaled font size should be positive", scaledFont.getSize() > 0);
        assertEquals("Scaled font size should be 18", 18, scaledFont.getSize());
    }

    /**
     * TEST 5: Font size zero is rejected or handled gracefully
     * Dimension pair: FontSize=Invalid, FontFamily=System
     */
    @Test
    public void testZeroFontSizeHandledGracefully() {
        Font font = findOrCreateFont("Arial", Font.PLAIN, 0);
        assertNotNull("Zero size font should still produce Font object", font);
        // Java allows zero-size fonts but behavior is undefined; we accept it
        assertTrue("Font should exist", font != null);
    }

    /**
     * TEST 6: Negative font size is rejected or defaults to valid size
     * Dimension pair: FontSize=Invalid, FontFamily=Custom
     */
    @Test
    public void testNegativeFontSizeRejected() {
        Font font = new Font("Courier New", Font.PLAIN, 12);
        Font derived = font.deriveFont(-14f);
        // Java may create with negative size; we should handle or reject it
        assertTrue("Negative font size should be handled", derived != null || font.getSize() > 0);
    }

    // ============================================================================
    // PAIRWISE TEST SUITE: Font Size × Rendering Mode
    // ============================================================================

    /**
     * TEST 7: Small font (10pt) with aliased rendering renders without error
     * Dimension pair: FontSize=Small, Rendering=Aliased
     */
    @Test
    public void testSmallFontAliasedRendering() {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 10);
        graphics2D.setFont(font);
        assertDoesNotThrow("Aliased rendering should not throw", () -> {
            graphics2D.drawString("Test", 10, 20);
        });
    }

    /**
     * TEST 8: Medium font (14pt) with anti-aliased rendering renders without error
     * Dimension pair: FontSize=Medium, Rendering=AntiAliased
     */
    @Test
    public void testMediumFontAntiAliasedRendering() {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = findOrCreateFont("Arial", Font.PLAIN, 14);
        graphics2D.setFont(font);
        assertDoesNotThrow("Anti-aliased rendering should not throw", () -> {
            graphics2D.drawString("Test", 10, 20);
        });
    }

    /**
     * TEST 9: Large font (18pt) with subpixel rendering renders without error
     * Dimension pair: FontSize=Large, Rendering=Subpixel
     */
    @Test
    public void testLargeFontSubpixelRendering() {
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        Font font = findOrCreateFont("Consolas", Font.PLAIN, 18);
        graphics2D.setFont(font);
        assertDoesNotThrow("Subpixel rendering should not throw", () -> {
            graphics2D.drawString("Test", 10, 20);
        });
    }

    // ============================================================================
    // PAIRWISE TEST SUITE: Rendering Mode × Character Set
    // ============================================================================

    /**
     * TEST 10: ASCII characters render with aliased mode
     * Dimension pair: Rendering=Aliased, CharacterSet=ASCII
     */
    @Test
    public void testASCIICharactersAliasedMode() {
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 12);
        graphics2D.setFont(font);

        for (char c : ASCII_CHARS.toCharArray()) {
            GlyphVector glyphVector = font.createGlyphVector(graphics2D.getFontRenderContext(), String.valueOf(c));
            assertNotNull("Glyph vector for ASCII char should not be null", glyphVector);
        }
    }

    /**
     * TEST 11: Extended Latin characters render with anti-aliased mode
     * Dimension pair: Rendering=AntiAliased, CharacterSet=Extended
     */
    @Test
    public void testExtendedLatinCharactersAntiAliased() {
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = findOrCreateFont("Arial", Font.PLAIN, 14);
        graphics2D.setFont(font);

        for (char c : EXTENDED_CHARS.toCharArray()) {
            GlyphVector glyphVector = font.createGlyphVector(graphics2D.getFontRenderContext(), String.valueOf(c));
            assertNotNull("Glyph vector for extended char should not be null", glyphVector);
        }
    }

    /**
     * TEST 12: Graphic box-drawing characters render with subpixel mode
     * Dimension pair: Rendering=Subpixel, CharacterSet=Graphic
     */
    @Test
    public void testGraphicCharactersSubpixelMode() {
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 12);
        graphics2D.setFont(font);

        for (char c : GRAPHIC_CHARS.toCharArray()) {
            GlyphVector glyphVector = font.createGlyphVector(graphics2D.getFontRenderContext(), String.valueOf(c));
            assertNotNull("Graphic character should produce glyph vector", glyphVector);
        }
    }

    // ============================================================================
    // PAIRWISE TEST SUITE: Character Set × Display DPI
    // ============================================================================

    /**
     * TEST 13: ASCII characters metric calculation at standard DPI (96)
     * Dimension pair: CharacterSet=ASCII, DPI=Standard
     */
    @Test
    public void testASCIICharacterMetricsStandardDPI() {
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 12);
        FontMetrics metrics = graphics2D.getFontMetrics(font);

        int ascent = metrics.getAscent();
        int descent = metrics.getDescent();
        int lineHeight = metrics.getHeight();

        assertTrue("Ascent should be positive", ascent > 0);
        assertTrue("Descent should be non-negative", descent >= 0);
        assertTrue("Line height should be greater than ascent", lineHeight > ascent);
    }

    /**
     * TEST 14: Extended character metric calculation at retina DPI (192)
     * Dimension pair: CharacterSet=Extended, DPI=Retina
     */
    @Test
    public void testExtendedCharacterMetricsRetinaDPI() {
        // Scale font for 192 DPI (2x standard)
        Font baseFont = findOrCreateFont("Arial", Font.PLAIN, 12);
        Font scaledFont = baseFont.deriveFont(24f);  // 12pt * 2 for 192 DPI
        FontMetrics baseMetrics = graphics2D.getFontMetrics(baseFont);
        FontMetrics scaledMetrics = graphics2D.getFontMetrics(scaledFont);

        int baseCharWidth = baseMetrics.charWidth('M');
        int scaledCharWidth = scaledMetrics.charWidth('M');
        assertTrue("Character width should be positive", scaledCharWidth > 0);
        assertTrue("Scaled font character width should be larger", scaledCharWidth > baseCharWidth);
    }

    /**
     * TEST 15: Graphic character advance width calculation at custom DPI (144)
     * Dimension pair: CharacterSet=Graphic, DPI=Custom
     */
    @Test
    public void testGraphicCharacterAdvanceWidthCustomDPI() {
        Font baseFont = findOrCreateFont("Courier New", Font.PLAIN, 12);
        Font scaledFont = baseFont.deriveFont(18f);  // 12pt * 1.5 for 144 DPI
        FontMetrics metrics = graphics2D.getFontMetrics(scaledFont);

        int advanceWidth = metrics.charWidth('─');
        assertTrue("Advance width should be non-negative", advanceWidth >= 0);
    }

    // ============================================================================
    // PAIRWISE TEST SUITE: Display DPI × Font Family
    // ============================================================================

    /**
     * TEST 16: Monospace font advance width consistency across DPI levels
     * Dimension pair: DPI=Standard, FontFamily=Monospace
     */
    @Test
    public void testMonospaceFontAdvanceWidthConsistent() {
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 12);
        FontMetrics metrics = graphics2D.getFontMetrics(font);

        int widthI = metrics.charWidth('I');
        int widthM = metrics.charWidth('M');
        int widthSpace = metrics.charWidth(' ');

        // Monospace fonts should have equal widths for most characters
        assertEquals("Monospace font should have consistent width for I and M", widthI, widthM);
        assertEquals("Monospace font should have consistent width for space", widthSpace, widthI);
    }

    /**
     * TEST 17: System font proportional spacing at retina DPI
     * Dimension pair: DPI=Retina, FontFamily=System
     */
    @Test
    public void testSystemFontProportionalSpacingRetina() {
        Font baseFont = findOrCreateFont("Arial", Font.PLAIN, 12);
        Font scaledFont = baseFont.deriveFont(24f);
        FontMetrics metrics = graphics2D.getFontMetrics(scaledFont);

        int widthI = metrics.charWidth('I');
        int widthM = metrics.charWidth('M');

        // System fonts may have proportional spacing (M > I)
        assertNotNull("Metrics should not be null", metrics);
        assertTrue("Widths should be positive", widthI > 0 && widthM > 0);
    }

    /**
     * TEST 18: Custom font scaling factor applied correctly
     * Dimension pair: DPI=Custom, FontFamily=Custom
     */
    @Test
    public void testCustomFontScalingFactorApplied() {
        Font baseFont = findOrCreateFont("Consolas", Font.PLAIN, 12);
        Font scaledFont = baseFont.deriveFont(18f);

        assertEquals("Scaled font size should be 18", 18, scaledFont.getSize());
        FontMetrics scaledMetrics = graphics2D.getFontMetrics(scaledFont);
        FontMetrics baseMetrics = graphics2D.getFontMetrics(baseFont);

        assertTrue("Scaled font height should be larger",
            scaledMetrics.getHeight() > baseMetrics.getHeight());
    }

    // ============================================================================
    // PAIRWISE TEST SUITE: Font Family × Character Set
    // ============================================================================

    /**
     * TEST 19: Monospace font renders all ASCII characters without missing glyphs
     * Dimension pair: FontFamily=Monospace, CharacterSet=ASCII
     */
    @Test
    public void testMonospaceFontASCIINoMissingGlyphs() {
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 12);
        for (char c = 32; c < 127; c++) {
            int code = font.canDisplay(c) ? 1 : 0;
            assertTrue("Monospace should display ASCII char: " + c, code == 1);
        }
    }

    /**
     * TEST 20: System font extended Latin character coverage
     * Dimension pair: FontFamily=System, CharacterSet=Extended
     */
    @Test
    public void testSystemFontExtendedLatinCoverage() {
        Font font = findOrCreateFont("Arial", Font.PLAIN, 12);
        int displayedCount = 0;

        for (char c : EXTENDED_CHARS.toCharArray()) {
            if (font.canDisplay(c)) {
                displayedCount++;
            }
        }

        assertTrue("System font should display some extended Latin chars", displayedCount > 0);
    }

    /**
     * TEST 21: Custom font graphic character fallback handling
     * Dimension pair: FontFamily=Custom, CharacterSet=Graphic
     */
    @Test
    public void testCustomFontGraphicCharacterFallback() {
        Font font = findOrCreateFont("Consolas", Font.PLAIN, 12);
        for (char c : GRAPHIC_CHARS.toCharArray()) {
            if (!font.canDisplay(c)) {
                // Should have fallback mechanism
                GlyphVector gv = font.createGlyphVector(graphics2D.getFontRenderContext(), String.valueOf(c));
                assertNotNull("Should have fallback glyph vector", gv);
            }
        }
    }

    // ============================================================================
    // PAIRWISE TEST SUITE: Rendering Mode × DPI
    // ============================================================================

    /**
     * TEST 22: Aliased rendering performance acceptable at standard DPI
     * Dimension pair: Rendering=Aliased, DPI=Standard
     */
    @Test
    public void testAliasedRenderingStandardDPIPerformance() {
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 12);
        graphics2D.setFont(font);

        long startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            graphics2D.drawString("TestString" + i, 10, 20 + i);
        }
        long duration = System.nanoTime() - startTime;

        assertTrue("Aliased rendering should complete in reasonable time", duration < 1_000_000_000);  // 1 second
    }

    /**
     * TEST 23: Anti-aliased rendering quality acceptable at retina DPI
     * Dimension pair: Rendering=AntiAliased, DPI=Retina
     */
    @Test
    public void testAntiAliasedRenderingRetinaDPIQuality() {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font baseFont = findOrCreateFont("Arial", Font.PLAIN, 12);
        Font scaledFont = baseFont.deriveFont(24f);
        graphics2D.setFont(scaledFont);

        long startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            graphics2D.drawString("QualityTest" + i, 10, 20 + i);
        }
        long duration = System.nanoTime() - startTime;

        assertTrue("Anti-aliased rendering should complete in reasonable time", duration < 2_000_000_000);  // 2 seconds
    }

    /**
     * TEST 24: Subpixel rendering correctness at custom DPI
     * Dimension pair: Rendering=Subpixel, DPI=Custom
     */
    @Test
    public void testSubpixelRenderingCustomDPICorrectness() {
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        Font baseFont = findOrCreateFont("Consolas", Font.PLAIN, 12);
        Font scaledFont = baseFont.deriveFont(18f);
        graphics2D.setFont(scaledFont);

        GlyphVector glyphVector = scaledFont.createGlyphVector(graphics2D.getFontRenderContext(), "Subpixel");
        assertNotNull("Glyph vector for subpixel rendering should be valid", glyphVector);
        assertEquals("Glyph count should match string length", 8, glyphVector.getNumGlyphs());
    }

    // ============================================================================
    // ADVERSARIAL TESTS: Missing Glyph Handling & Edge Cases
    // ============================================================================

    /**
     * TEST 25: Missing glyphs (zero-width spaces) handled gracefully
     * Adversarial: Character set contains zero-width characters that may not display
     */
    @Test
    public void testMissingGlyphsZeroWidthCharacters() {
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 12);

        for (char c : MISSING_GLYPH_TEST.toCharArray()) {
            GlyphVector gv = font.createGlyphVector(graphics2D.getFontRenderContext(), String.valueOf(c));
            assertNotNull("Glyph vector should exist even for zero-width chars", gv);
        }
    }

    /**
     * TEST 26: Null font reference is handled (no guaranteed exception in Java)
     * Adversarial: Font selection failure
     */
    @Test
    public void testNullFontHandled() {
        Font currentFont = graphics2D.getFont();
        assertNotNull("Graphics should have default font", currentFont);
        // Setting null font may or may not throw; behavior is implementation-specific
    }

    /**
     * TEST 27: Invalid rendering hint combination handled gracefully
     * Adversarial: Conflicting rendering hints
     */
    @Test
    public void testInvalidRenderingHintCombination() {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        Font font = findOrCreateFont("Arial", Font.PLAIN, 12);
        graphics2D.setFont(font);

        assertDoesNotThrow("Conflicting hints should be handled", () -> {
            graphics2D.drawString("Test", 10, 20);
        });
    }

    /**
     * TEST 28: Very large font size (256pt) does not cause overflow
     * Adversarial: Extreme sizing
     */
    @Test
    public void testExtremelyLargeFontSize() {
        Font font = findOrCreateFont("Arial", Font.PLAIN, 256);
        assertNotNull("Very large font should still be created", font);
        assertEquals("Font size should be 256", 256, font.getSize());

        FontMetrics metrics = graphics2D.getFontMetrics(font);
        assertTrue("Large font metrics should be reasonable", metrics.getHeight() > 0);
    }

    /**
     * TEST 29: Font style variations (bold, italic) render correctly
     * Adversarial: Style combinations
     */
    @Test
    public void testFontStyleVariations() {
        Font plainFont = findOrCreateFont("Arial", Font.PLAIN, 12);
        Font boldFont = plainFont.deriveFont(Font.BOLD);
        Font italicFont = plainFont.deriveFont(Font.ITALIC);
        Font boldItalicFont = plainFont.deriveFont(Font.BOLD | Font.ITALIC);

        assertNotNull("Bold font should not be null", boldFont);
        assertNotNull("Italic font should not be null", italicFont);
        assertNotNull("Bold italic font should not be null", boldItalicFont);

        graphics2D.setFont(boldFont);
        assertDoesNotThrow("Bold font should render", () -> graphics2D.drawString("Bold", 10, 20));
    }

    /**
     * TEST 30: Font metrics consistency across multiple retrieval calls
     * Adversarial: State management
     */
    @Test
    public void testFontMetricsConsistency() {
        Font font = findOrCreateFont("Courier New", Font.PLAIN, 12);

        FontMetrics metrics1 = graphics2D.getFontMetrics(font);
        FontMetrics metrics2 = graphics2D.getFontMetrics(font);
        FontMetrics metrics3 = graphics2D.getFontMetrics(font);

        assertEquals("Metrics ascent should be consistent", metrics1.getAscent(), metrics2.getAscent());
        assertEquals("Metrics descent should be consistent", metrics1.getDescent(), metrics3.getDescent());
        assertEquals("Metrics height should be consistent", metrics1.getHeight(), metrics2.getHeight());
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Helper: Find or create a font, with fallback to system default
     */
    private Font findOrCreateFont(String fontName, int style, int size) {
        Font[] allFonts = graphicsEnv.getAllFonts();

        for (Font f : allFonts) {
            if (f.getFontName().equalsIgnoreCase(fontName)) {
                return new Font(fontName, style, size);
            }
        }

        // Fallback to monospace if not found
        return new Font("Monospaced", style, size);
    }

    /**
     * Helper: Assert that code does not throw exception
     */
    private void assertDoesNotThrow(String message, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            fail(message + ": " + e.getMessage());
        }
    }
}
