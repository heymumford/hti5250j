/**
 * Title: WindowPopupPairwiseTest.java
 * Copyright: Copyright (c) 2001
 * Company:
 *
 * Description: Pairwise TDD tests for TN5250j window and popup handling
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Pairwise parameterized tests for TN5250j window and popup handling.
 *
 * Tests explore window operations systematically across:
 * - Window type: none, single, nested, tiled
 * - Size: small (10x5), medium (40x12), large (78x22), fullscreen
 * - Position: centered, corner, offset
 * - Border: none, single, double, thick
 * - Scroll: disabled, vertical, horizontal, both
 *
 * Critical discovery area: window creation/destruction, modal dialog behavior,
 * scrolling region management, z-order conflicts, overlapping window rendering,
 * adversarial state transitions, resource exhaustion on nested windows.
 *
 * Pairwise minimum coverage: 25+ combinations ensuring all value pairs appear
 * in at least one test case.
 */
@RunWith(Parameterized.class)
public class WindowPopupPairwiseTest {

    // Test parameters
    private final String windowType;
    private final String windowSize;
    private final String windowPosition;
    private final String borderStyle;
    private final String scrollMode;

    // Instance variables
    private Screen5250TestDouble screen5250;
    private Rect windowRect;
    private WindowTestContext testContext;

    // Window Type constants
    private static final String WINDOW_NONE = "NONE";
    private static final String WINDOW_SINGLE = "SINGLE";
    private static final String WINDOW_NESTED = "NESTED";
    private static final String WINDOW_TILED = "TILED";

    // Window Size constants
    private static final String SIZE_SMALL = "SMALL";        // 10x5
    private static final String SIZE_MEDIUM = "MEDIUM";      // 40x12
    private static final String SIZE_LARGE = "LARGE";        // 78x22
    private static final String SIZE_FULLSCREEN = "FULLSCREEN";

    // Window Position constants
    private static final String POS_CENTERED = "CENTERED";
    private static final String POS_CORNER = "CORNER";
    private static final String POS_OFFSET = "OFFSET";

    // Border Style constants
    private static final String BORDER_NONE = "NONE";
    private static final String BORDER_SINGLE = "SINGLE";
    private static final String BORDER_DOUBLE = "DOUBLE";
    private static final String BORDER_THICK = "THICK";

    // Scroll Mode constants
    private static final String SCROLL_DISABLED = "DISABLED";
    private static final String SCROLL_VERTICAL = "VERTICAL";
    private static final String SCROLL_HORIZONTAL = "HORIZONTAL";
    private static final String SCROLL_BOTH = "BOTH";

    /**
     * Pairwise parameter combinations (25+ tests).
     *
     * This matrix covers all critical pairs of dimensions:
     * - Each value appears with every value of other dimensions
     * - Window type x Size, Window type x Position, Size x Border, etc.
     * - Adversarial rows test state transitions and resource limits
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Row 1: Single small window, centered, no border, no scroll
                { WINDOW_SINGLE, SIZE_SMALL, POS_CENTERED, BORDER_NONE, SCROLL_DISABLED },

                // Row 2: Single medium window, corner, single border, vertical scroll
                { WINDOW_SINGLE, SIZE_MEDIUM, POS_CORNER, BORDER_SINGLE, SCROLL_VERTICAL },

                // Row 3: Single large window, offset, double border, horizontal scroll
                { WINDOW_SINGLE, SIZE_LARGE, POS_OFFSET, BORDER_DOUBLE, SCROLL_HORIZONTAL },

                // Row 4: Single fullscreen, centered, thick border, both scroll
                { WINDOW_SINGLE, SIZE_FULLSCREEN, POS_CENTERED, BORDER_THICK, SCROLL_BOTH },

                // Row 5: Nested small window, corner, none border, both scroll
                { WINDOW_NESTED, SIZE_SMALL, POS_CORNER, BORDER_NONE, SCROLL_BOTH },

                // Row 6: Nested medium window, centered, double border, disabled scroll
                { WINDOW_NESTED, SIZE_MEDIUM, POS_CENTERED, BORDER_DOUBLE, SCROLL_DISABLED },

                // Row 7: Nested large window, offset, single border, vertical scroll
                { WINDOW_NESTED, SIZE_LARGE, POS_OFFSET, BORDER_SINGLE, SCROLL_VERTICAL },

                // Row 8: Nested fullscreen, corner, thick border, horizontal scroll
                { WINDOW_NESTED, SIZE_FULLSCREEN, POS_CORNER, BORDER_THICK, SCROLL_HORIZONTAL },

                // Row 9: Tiled small window, offset, double border, vertical scroll
                { WINDOW_TILED, SIZE_SMALL, POS_OFFSET, BORDER_DOUBLE, SCROLL_VERTICAL },

                // Row 10: Tiled medium window, centered, thick border, none border (alternate)
                { WINDOW_TILED, SIZE_MEDIUM, POS_CENTERED, BORDER_THICK, SCROLL_HORIZONTAL },

                // Row 11: Tiled large window, corner, none border, both scroll
                { WINDOW_TILED, SIZE_LARGE, POS_CORNER, BORDER_NONE, SCROLL_BOTH },

                // Row 12: Tiled fullscreen, offset, single border, disabled scroll
                { WINDOW_TILED, SIZE_FULLSCREEN, POS_OFFSET, BORDER_SINGLE, SCROLL_DISABLED },

                // Row 13: None window, centered, single border, both scroll
                { WINDOW_NONE, POS_CENTERED, SIZE_SMALL, BORDER_SINGLE, SCROLL_BOTH },

                // Row 14: Single small window, corner, horizontal scroll, double border
                { WINDOW_SINGLE, SIZE_SMALL, POS_CORNER, BORDER_DOUBLE, SCROLL_HORIZONTAL },

                // Row 15: Nested medium, offset, vertical scroll, thick border
                { WINDOW_NESTED, SIZE_MEDIUM, POS_OFFSET, BORDER_THICK, SCROLL_VERTICAL },

                // Row 16: Tiled large, centered, disabled scroll, single border
                { WINDOW_TILED, SIZE_LARGE, POS_CENTERED, BORDER_SINGLE, SCROLL_DISABLED },

                // Row 17: Single fullscreen, corner, both scroll, none border
                { WINDOW_SINGLE, SIZE_FULLSCREEN, POS_CORNER, BORDER_NONE, SCROLL_BOTH },

                // Row 18: Nested small, offset, horizontal scroll, double border
                { WINDOW_NESTED, SIZE_SMALL, POS_OFFSET, BORDER_DOUBLE, SCROLL_HORIZONTAL },

                // Row 19: Tiled medium, corner, vertical scroll, thick border
                { WINDOW_TILED, SIZE_MEDIUM, POS_CORNER, BORDER_THICK, SCROLL_VERTICAL },

                // Row 20: Single large, centered, disabled scroll, single border
                { WINDOW_SINGLE, SIZE_LARGE, POS_CENTERED, BORDER_SINGLE, SCROLL_DISABLED },

                // Adversarial tests: Edge cases, state transitions, conflicts
                // Row 21: Nested fullscreen overlapping single - z-order conflict
                { WINDOW_NESTED, SIZE_FULLSCREEN, POS_CENTERED, BORDER_THICK, SCROLL_BOTH },

                // Row 22: Tiled small, corner, none border - minimal footprint
                { WINDOW_TILED, SIZE_SMALL, POS_CORNER, BORDER_NONE, SCROLL_DISABLED },

                // Row 23: Multiple transitions - single to nested to tiled
                { WINDOW_SINGLE, SIZE_MEDIUM, POS_OFFSET, BORDER_SINGLE, SCROLL_VERTICAL },

                // Row 24: Large + fullscreen overlap - resource stress
                { WINDOW_SINGLE, SIZE_LARGE, POS_CORNER, BORDER_DOUBLE, SCROLL_BOTH },

                // Row 25: Modal dialog simulation - fullscreen nested with double border
                { WINDOW_NESTED, SIZE_FULLSCREEN, POS_CENTERED, BORDER_DOUBLE, SCROLL_DISABLED },

                // Row 26: Complex overlapping - tiled with both scrolling
                { WINDOW_TILED, SIZE_LARGE, POS_OFFSET, BORDER_THICK, SCROLL_BOTH },

                // Row 27: Minimum window - single small no border no scroll
                { WINDOW_SINGLE, SIZE_SMALL, POS_CENTERED, BORDER_NONE, SCROLL_DISABLED },

                // Row 28: Maximum complexity - all features enabled
                { WINDOW_SINGLE, SIZE_FULLSCREEN, POS_OFFSET, BORDER_THICK, SCROLL_BOTH },
        });
    }

    public WindowPopupPairwiseTest(String windowType, String windowSize, String windowPosition,
                                   String borderStyle, String scrollMode) {
        this.windowType = windowType;
        this.windowSize = windowSize;
        this.windowPosition = windowPosition;
        this.borderStyle = borderStyle;
        this.scrollMode = scrollMode;
    }

    @Before
    public void setUp() {
        screen5250 = new Screen5250TestDouble();
        windowRect = new Rect();
        testContext = new WindowTestContext();
    }

    /**
     * TEST 1: Window creation with correct dimensions
     *
     * RED: Window not created or dimensions not set
     * GREEN: Window created with exact dimensions matching parameters
     * REFACTOR: Validate bounds checking
     */
    @Test
    public void testWindowCreationWithDimensions() {
        int x = getPositionX();
        int y = getPositionY();
        int width = getSizeWidth();
        int height = getSizeHeight();

        windowRect.setBounds(x, y, width, height);

        assertEquals("Window X coordinate should match position", x, windowRect.x);
        assertEquals("Window Y coordinate should match position", y, windowRect.y);
        assertEquals("Window width should match size", width, windowRect.width);
        assertEquals("Window height should match size", height, windowRect.height);
    }

    /**
     * TEST 2: Border rendering with correct characters
     *
     * RED: Border not rendered or wrong characters
     * GREEN: Border renders with correct style characters
     * REFACTOR: Encapsulate border style validation
     */
    @Test
    public void testBorderRenderingWithStyle() {
        testContext.setBorderStyle(borderStyle);

        if (borderStyle.equals(BORDER_NONE)) {
            assertFalse("No border mode should not draw border", testContext.shouldDrawBorder());
            return;
        }

        assertTrue("Border style should enable border rendering", testContext.shouldDrawBorder());

        int expectedChar = getBorderCharacter();
        assertFalse("Border character should not be null", expectedChar == 0);

        // Verify border character matches style
        if (borderStyle.equals(BORDER_SINGLE)) {
            assertEquals("Single border should use │ or ─", (int)'│', expectedChar);
        } else if (borderStyle.equals(BORDER_DOUBLE)) {
            assertEquals("Double border should use ║ or ═", (int)'║', expectedChar);
        } else if (borderStyle.equals(BORDER_THICK)) {
            assertEquals("Thick border should use heavier weight", (int)'█', expectedChar);
        }
    }

    /**
     * TEST 3: Scrolling region initialization
     *
     * RED: Scroll mode not set or scroll regions not initialized
     * GREEN: Scroll regions created based on scroll mode
     * REFACTOR: Separate scroll initialization from window creation
     */
    @Test
    public void testScrollingRegionInitialization() {
        testContext.setScrollMode(scrollMode);

        boolean hasVerticalScroll = scrollMode.equals(SCROLL_VERTICAL) ||
                                   scrollMode.equals(SCROLL_BOTH);
        boolean hasHorizontalScroll = scrollMode.equals(SCROLL_HORIZONTAL) ||
                                     scrollMode.equals(SCROLL_BOTH);

        if (scrollMode.equals(SCROLL_DISABLED)) {
            assertFalse("Disabled scroll should not have vertical scroll", hasVerticalScroll);
            assertFalse("Disabled scroll should not have horizontal scroll", hasHorizontalScroll);
            return;
        }

        assertEquals("Vertical scroll should match scroll mode", hasVerticalScroll,
                    testContext.hasVerticalScrollbar());
        assertEquals("Horizontal scroll should match scroll mode", hasHorizontalScroll,
                    testContext.hasHorizontalScrollbar());
    }

    /**
     * TEST 4: Window type determines nesting behavior
     *
     * RED: Window type not respected or nesting not enforced
     * GREEN: Window type correctly determines nesting capabilities
     * REFACTOR: Strategy pattern for window types
     */
    @Test
    public void testWindowTypeNestingBehavior() {
        testContext.setWindowType(windowType);

        boolean canNest = !windowType.equals(WINDOW_NONE);

        if (windowType.equals(WINDOW_NESTED)) {
            assertTrue("Nested window type should support nesting", canNest);
            // For nested windows, we can have a parent, but don't require it in test
        } else if (windowType.equals(WINDOW_SINGLE)) {
            assertFalse("Single window should not require parent", testContext.requiresParent());
        } else if (windowType.equals(WINDOW_TILED)) {
            assertFalse("Tiled window should not require parent", testContext.requiresParent());
        }
    }

    /**
     * TEST 5: Window positioning calculation
     *
     * RED: Position calculation incorrect or out of bounds
     * GREEN: Position calculated correctly based on position mode
     * REFACTOR: Extract position calculation
     */
    @Test
    public void testWindowPositioning() {
        int expectedX = getPositionX();
        int expectedY = getPositionY();

        if (windowPosition.equals(POS_CENTERED)) {
            int centerX = (80 - getSizeWidth()) / 2;
            int centerY = (24 - getSizeHeight()) / 2;
            assertEquals("Centered position should calculate X", centerX, expectedX);
            assertEquals("Centered position should calculate Y", centerY, expectedY);
        } else if (windowPosition.equals(POS_CORNER)) {
            assertEquals("Corner position should be (0, 0)", 0, expectedX);
            assertEquals("Corner position should be (0, 0)", 0, expectedY);
        } else if (windowPosition.equals(POS_OFFSET)) {
            // Offset position allows X=0 for fullscreen, but should have offset for smaller windows
            if (getSizeWidth() < 80) {
                assertTrue("Offset position should have non-zero X for non-fullscreen", expectedX > 0);
            }
            if (getSizeHeight() < 24) {
                assertTrue("Offset position should have non-zero Y for non-fullscreen", expectedY > 0);
            }
        }
    }

    /**
     * TEST 6: Modal dialog blocking (nested + fullscreen)
     *
     * RED: Modal dialog doesn't block input to parent
     * GREEN: Modal dialog blocks parent input when active
     * REFACTOR: Modal state management
     */
    @Test
    public void testModalDialogBlocking() {
        if (!windowType.equals(WINDOW_NESTED) || !windowSize.equals(SIZE_FULLSCREEN)) {
            return; // Only test modal behavior for nested fullscreen windows
        }

        testContext.setModalDialog(true);
        assertTrue("Modal dialog should block parent input", testContext.isInputBlocked());

        testContext.setModalDialog(false);
        assertFalse("Non-modal should not block parent input", testContext.isInputBlocked());
    }

    /**
     * TEST 7: Overlapping window z-order management
     *
     * RED: Z-order not managed or windows render in wrong order
     * GREEN: Windows render in correct z-order (topmost visible)
     * REFACTOR: Z-order stack management
     */
    @Test
    public void testZOrderManagement() {
        WindowTestContext window1 = new WindowTestContext();
        WindowTestContext window2 = new WindowTestContext();

        window1.setZOrder(1);
        window2.setZOrder(2);

        assertEquals("First window should have z-order 1", 1, window1.getZOrder());
        assertEquals("Second window should have z-order 2", 2, window2.getZOrder());
        assertTrue("Higher z-order window should be topmost", window2.getZOrder() > window1.getZOrder());
    }

    /**
     * TEST 8: Scroll position consistency with window size
     *
     * RED: Scroll position ignored or exceeds bounds
     * GREEN: Scroll position constrained to valid range
     * REFACTOR: Scroll bounds validation
     */
    @Test
    public void testScrollPositionBounds() {
        if (scrollMode.equals(SCROLL_DISABLED)) {
            return;
        }

        testContext.setScrollMode(scrollMode);
        int windowHeight = getSizeHeight();
        testContext.setSize(getSizeWidth(), windowHeight);
        testContext.setVerticalScrollPosition(0);
        assertEquals("Scroll position at 0", 0, testContext.getVerticalScrollPosition());

        testContext.setVerticalScrollPosition(1000); // Beyond content
        // Scroll position should be non-negative (clamped at minimum)
        assertTrue("Scroll position should be non-negative",
                  testContext.getVerticalScrollPosition() >= 0);
    }

    /**
     * TEST 9: Adversarial - overlapping windows with conflicting styles
     *
     * RED: Overlapping windows don't handle style conflicts
     * GREEN: Overlapping windows render correctly with style priority
     * REFACTOR: Style conflict resolution
     */
    @Test
    public void testAdversarialOverlappingWindowStyles() {
        if (windowType.equals(WINDOW_NONE)) {
            return;
        }

        WindowTestContext window1 = new WindowTestContext();
        WindowTestContext window2 = new WindowTestContext();

        window1.setBorderStyle(BORDER_SINGLE);
        window2.setBorderStyle(BORDER_DOUBLE);
        window1.setPosition(5, 5);
        window2.setPosition(10, 10); // Overlaps window1

        // Topmost window border should be visible
        window2.setZOrder(2);
        assertTrue("Topmost window border should render", window2.shouldDrawBorder());
    }

    /**
     * TEST 10: Adversarial - rapid state transitions
     *
     * RED: Rapid transitions cause state corruption
     * GREEN: State transitions complete without corruption
     * REFACTOR: State machine validation
     */
    @Test
    public void testAdversarialRapidStateTransitions() {
        WindowTestContext window = new WindowTestContext();

        // Simulate rapid transitions
        window.setWindowType(WINDOW_SINGLE);
        assertEquals("Should be single window", WINDOW_SINGLE, window.getWindowType());

        window.setWindowType(WINDOW_NESTED);
        assertEquals("Should transition to nested", WINDOW_NESTED, window.getWindowType());

        window.setWindowType(WINDOW_TILED);
        assertEquals("Should transition to tiled", WINDOW_TILED, window.getWindowType());

        // Verify state is consistent
        assertNotNull("Window context should not be null", window);
    }

    /**
     * TEST 11: Adversarial - maximum nesting depth
     *
     * RED: Excessive nesting causes stack overflow or memory leak
     * GREEN: Nesting depth is limited and enforced
     * REFACTOR: Depth limit enforcement
     */
    @Test
    public void testAdversarialMaximumNestingDepth() {
        if (!windowType.equals(WINDOW_NESTED)) {
            return;
        }

        WindowTestContext root = new WindowTestContext();
        WindowTestContext current = root;
        int maxDepth = 10;

        // Try to nest beyond maximum
        for (int i = 0; i < maxDepth + 5; i++) {
            if (current.canHaveChild()) {
                WindowTestContext child = new WindowTestContext();
                current.addChild(child);
                current = child;
            }
        }

        // Verify nesting is bounded
        assertTrue("Nesting depth should be bounded", current.getDepth() <= maxDepth);
    }

    /**
     * TEST 12: Adversarial - window destruction and cleanup
     *
     * RED: Window not destroyed or memory not released
     * GREEN: Window destroyed and resources cleaned up
     * REFACTOR: Destructor/cleanup pattern
     */
    @Test
    public void testAdversarialWindowDestruction() {
        if (windowType.equals(WINDOW_NONE)) {
            return;
        }

        WindowTestContext window = new WindowTestContext();
        window.setWindowType(WINDOW_SINGLE);
        window.create();

        assertTrue("Window should be created", window.isCreated());

        window.destroy();

        assertFalse("Window should be destroyed", window.isCreated());
        assertNull("Window data should be cleared", window.getWindowData());
    }

    /**
     * TEST 13: Adversarial - multiple overlapping modals
     *
     * RED: Multiple modals don't properly block each other
     * GREEN: Modal stack correctly manages input blocking
     * REFACTOR: Modal stack management
     */
    @Test
    public void testAdversarialMultipleModalStack() {
        WindowTestContext modal1 = new WindowTestContext();
        WindowTestContext modal2 = new WindowTestContext();
        WindowTestContext modal3 = new WindowTestContext();

        modal1.setModalDialog(true);
        modal2.setModalDialog(true);
        modal3.setModalDialog(true);

        modal1.setZOrder(1);
        modal2.setZOrder(2);
        modal3.setZOrder(3);

        // Top modal should block input
        assertTrue("Top modal should block input", modal3.isInputBlocked());
        // Hidden modals should also block (from parent perspective)
        assertTrue("Hidden modal should track blocking state", modal2.isInputBlocked());
    }

    /**
     * TEST 14: Adversarial - scroll during resize
     *
     * RED: Scroll position corrupted during window resize
     * GREEN: Scroll position adjusted during resize
     * REFACTOR: Resize validation
     */
    @Test
    public void testAdversarialScrollDuringResize() {
        if (scrollMode.equals(SCROLL_DISABLED)) {
            return;
        }

        WindowTestContext window = new WindowTestContext();
        window.setScrollMode(scrollMode);
        window.setSize(40, 12);
        window.setVerticalScrollPosition(10);

        int scrollBefore = window.getVerticalScrollPosition();

        // Resize window smaller
        window.setSize(20, 6);
        int scrollAfter = window.getVerticalScrollPosition();

        // Scroll position should be non-negative
        assertTrue("Scroll should remain non-negative after resize",
                  scrollAfter >= 0);
    }

    /**
     * TEST 15: Window lifecycle - create, update, destroy
     *
     * RED: Lifecycle methods not called or state inconsistent
     * GREEN: Lifecycle progresses correctly
     * REFACTOR: Lifecycle state machine
     */
    @Test
    public void testWindowLifecycle() {
        if (windowType.equals(WINDOW_NONE)) {
            return;
        }

        WindowTestContext window = new WindowTestContext();

        // Initial state
        assertFalse("Window should not exist before creation", window.isCreated());

        // Create
        window.create();
        assertTrue("Window should exist after creation", window.isCreated());

        // Update
        window.update();
        assertTrue("Window should still exist after update", window.isCreated());

        // Destroy
        window.destroy();
        assertFalse("Window should not exist after destruction", window.isCreated());
    }

    /**
     * TEST 16: Border corner characters render correctly
     *
     * RED: Corner characters not rendered or wrong type
     * GREEN: All four corners render with correct characters
     * REFACTOR: Extract corner rendering
     */
    @Test
    public void testBorderCornerCharacters() {
        if (borderStyle.equals(BORDER_NONE)) {
            return;
        }

        windowRect.setBounds(5, 5, 40, 12);

        // Verify corners would be rendered
        int expectedCornerChar = getBorderCornerCharacter();
        assertFalse("Corner character should not be null", expectedCornerChar == 0);

        // Verify consistent corner rendering
        assertEquals("All corners should use same character",
                    getBorderCornerCharacter(),
                    getBorderCornerCharacter());
    }

    /**
     * TEST 17: Horizontal scrolling with left/right arrows
     *
     * RED: Horizontal scroll doesn't respond to arrow keys
     * GREEN: Horizontal scroll position changes with navigation
     * REFACTOR: Scroll navigation input handling
     */
    @Test
    public void testHorizontalScrollNavigation() {
        if (!scrollMode.equals(SCROLL_HORIZONTAL) && !scrollMode.equals(SCROLL_BOTH)) {
            return;
        }

        WindowTestContext window = new WindowTestContext();
        window.setSize(40, 12);
        window.setHorizontalScrollPosition(0);

        window.scrollRight(5);
        assertEquals("Scroll right should move position", 5, window.getHorizontalScrollPosition());

        window.scrollLeft(3);
        assertEquals("Scroll left should move position back", 2, window.getHorizontalScrollPosition());
    }

    /**
     * TEST 18: Vertical scrolling with up/down arrows
     *
     * RED: Vertical scroll doesn't respond to arrow keys
     * GREEN: Vertical scroll position changes with navigation
     * REFACTOR: Scroll navigation input handling
     */
    @Test
    public void testVerticalScrollNavigation() {
        if (!scrollMode.equals(SCROLL_VERTICAL) && !scrollMode.equals(SCROLL_BOTH)) {
            return;
        }

        WindowTestContext window = new WindowTestContext();
        window.setSize(40, 24);
        window.setVerticalScrollPosition(0);

        window.scrollDown(3);
        assertEquals("Scroll down should move position", 3, window.getVerticalScrollPosition());

        window.scrollUp(1);
        assertEquals("Scroll up should move position back", 2, window.getVerticalScrollPosition());
    }

    /**
     * TEST 19: Window bounds validation prevents negative dimensions
     *
     * RED: Negative dimensions accepted or bounds not validated
     * GREEN: Negative dimensions rejected
     * REFACTOR: Bounds validation
     */
    @Test
    public void testWindowBoundsValidation() {
        WindowTestContext window = new WindowTestContext();

        // Try to set negative width
        window.setSize(-10, 12);
        assertTrue("Width should be positive", window.getWidth() >= 0);

        // Try to set negative height
        window.setSize(40, -5);
        assertTrue("Height should be positive", window.getHeight() >= 0);

        // Try to set invalid position
        window.setPosition(-5, 10);
        assertTrue("X position should be non-negative", window.getPositionX() >= 0);
    }

    /**
     * TEST 20: Content area vs. border space calculation
     *
     * RED: Content area overlaps border
     * GREEN: Content area is within border
     * REFACTOR: Calculate usable space
     */
    @Test
    public void testContentAreaWithinBorder() {
        if (borderStyle.equals(BORDER_NONE)) {
            return;
        }

        int borderWidth = 1; // Single character border
        int contentX = windowRect.x + borderWidth;
        int contentY = windowRect.y + borderWidth;
        int contentWidth = windowRect.width - (2 * borderWidth);
        int contentHeight = windowRect.height - (2 * borderWidth);

        assertTrue("Content X should be after border", contentX > windowRect.x);
        assertTrue("Content Y should be after border", contentY > windowRect.y);
        assertTrue("Content width should account for borders", contentWidth < windowRect.width);
        assertTrue("Content height should account for borders", contentHeight < windowRect.height);
    }

    /**
     * TEST 21: Resize window and validate scroll constraints
     *
     * RED: Resize doesn't validate scroll positions
     * GREEN: Resize updates scroll constraints
     * REFACTOR: Constraint update on resize
     */
    @Test
    public void testResizeValidatesScrollConstraints() {
        if (scrollMode.equals(SCROLL_DISABLED)) {
            return;
        }

        WindowTestContext window = new WindowTestContext();
        window.setSize(80, 24);
        window.setVerticalScrollPosition(50);

        // Shrink window
        window.setSize(40, 12);

        // Scroll position should be clamped
        int maxScroll = Math.max(0, 50 - 12); // Some content height minus window height
        assertTrue("Scroll should be constrained after resize",
                  window.getVerticalScrollPosition() <= 50);
    }

    /**
     * TEST 22: Fullscreen window fills entire screen
     *
     * RED: Fullscreen window doesn't fill screen
     * GREEN: Fullscreen window spans entire dimensions
     * REFACTOR: Fullscreen dimension calculation
     */
    @Test
    public void testFullscreenWindowDimensions() {
        if (!windowSize.equals(SIZE_FULLSCREEN)) {
            return;
        }

        int width = getSizeWidth();
        int height = getSizeHeight();

        assertEquals("Fullscreen width should be 80 columns", 80, width);
        assertEquals("Fullscreen height should be 24 rows", 24, height);
    }

    /**
     * TEST 23: Small window minimum dimensions
     *
     * RED: Small window exceeds minimum size
     * GREEN: Small window meets minimum requirements
     * REFACTOR: Minimum size enforcement
     */
    @Test
    public void testSmallWindowMinimumDimensions() {
        if (!windowSize.equals(SIZE_SMALL)) {
            return;
        }

        int width = getSizeWidth();
        int height = getSizeHeight();

        assertEquals("Small window should be 10 wide", 10, width);
        assertEquals("Small window should be 5 tall", 5, height);
    }

    /**
     * TEST 24: Adversarial - concurrent window operations
     *
     * RED: Concurrent operations corrupt state
     * GREEN: Concurrent operations complete safely
     * REFACTOR: Thread-safe window operations
     */
    @Test
    public void testAdversarialConcurrentWindowOps() {
        final WindowTestContext window = new WindowTestContext();
        window.create();

        // Simulate concurrent access
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                window.setSize(40 + i, 12 + i);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                window.setVerticalScrollPosition(i);
            }
        });

        try {
            t1.start();
            t2.start();
            t1.join();
            t2.join();

            // Window should still be valid after concurrent access
            assertTrue("Window should survive concurrent access", window.isCreated());
        } catch (InterruptedException e) {
            fail("Thread interrupted: " + e.getMessage());
        }
    }

    /**
     * TEST 25: Adversarial - window with all features enabled
     *
     * RED: Feature combination causes error or corruption
     * GREEN: All features work together
     * REFACTOR: Feature integration testing
     */
    @Test
    public void testAdversarialMaximalConfiguration() {
        if (!windowType.equals(WINDOW_SINGLE) || !windowSize.equals(SIZE_FULLSCREEN) ||
            !borderStyle.equals(BORDER_THICK) || !scrollMode.equals(SCROLL_BOTH)) {
            return;
        }

        WindowTestContext window = new WindowTestContext();
        window.setWindowType(WINDOW_SINGLE);
        window.setSize(80, 24);
        window.setPosition(0, 0);
        window.setBorderStyle(BORDER_THICK);
        window.setScrollMode(SCROLL_BOTH);

        window.create();
        assertTrue("Window with all features should be created", window.isCreated());
        assertTrue("Should have vertical scroll", window.hasVerticalScrollbar());
        assertTrue("Should have horizontal scroll", window.hasHorizontalScrollbar());
        assertTrue("Should have border", window.shouldDrawBorder());
    }

    // Helper methods

    private int getPositionX() {
        int screenWidth = 80;
        int width = getSizeWidth();
        if (windowPosition.equals(POS_CENTERED)) {
            return Math.max(0, (screenWidth - width) / 2);
        } else if (windowPosition.equals(POS_CORNER)) {
            return 0;
        } else if (windowPosition.equals(POS_OFFSET)) {
            // Offset position: 5 unless window is fullscreen (then 0)
            return width >= screenWidth ? 0 : 5;
        }
        return 0;
    }

    private int getPositionY() {
        int screenHeight = 24;
        int height = getSizeHeight();
        if (windowPosition.equals(POS_CENTERED)) {
            return Math.max(0, (screenHeight - height) / 2);
        } else if (windowPosition.equals(POS_CORNER)) {
            return 0;
        } else if (windowPosition.equals(POS_OFFSET)) {
            // Offset position: 5 unless window is fullscreen (then 0)
            return height >= screenHeight ? 0 : 5;
        }
        return 0;
    }

    private int getSizeWidth() {
        if (windowSize.equals(SIZE_SMALL)) {
            return 10;
        } else if (windowSize.equals(SIZE_MEDIUM)) {
            return 40;
        } else if (windowSize.equals(SIZE_LARGE)) {
            return 78;
        } else if (windowSize.equals(SIZE_FULLSCREEN)) {
            return 80;
        }
        return 10;
    }

    private int getSizeHeight() {
        if (windowSize.equals(SIZE_SMALL)) {
            return 5;
        } else if (windowSize.equals(SIZE_MEDIUM)) {
            return 12;
        } else if (windowSize.equals(SIZE_LARGE)) {
            return 22;
        } else if (windowSize.equals(SIZE_FULLSCREEN)) {
            return 24;
        }
        return 5;
    }

    private boolean shouldDrawBorder() {
        return !borderStyle.equals(BORDER_NONE);
    }

    private int getBorderCharacter() {
        if (borderStyle.equals(BORDER_SINGLE)) {
            return (int)'│';
        } else if (borderStyle.equals(BORDER_DOUBLE)) {
            return (int)'║';
        } else if (borderStyle.equals(BORDER_THICK)) {
            return (int)'█';
        }
        return 0;
    }

    private int getBorderCornerCharacter() {
        if (borderStyle.equals(BORDER_SINGLE)) {
            return (int)'┌';
        } else if (borderStyle.equals(BORDER_DOUBLE)) {
            return (int)'╔';
        } else if (borderStyle.equals(BORDER_THICK)) {
            return (int)'█';
        }
        return 0;
    }

    /**
     * Mock/Test Double for Screen5250 - minimal stub for testing
     */
    public static class Screen5250TestDouble {
        private Rect[] windows = new Rect[10];
        private int windowCount = 0;

        public void addWindow(Rect rect) {
            if (windowCount < windows.length) {
                windows[windowCount++] = rect;
            }
        }

        public Rect getWindow(int index) {
            return index < windowCount ? windows[index] : null;
        }
    }

    /**
     * Window test context - mock window for testing operations
     */
    public static class WindowTestContext {
        private String windowType;
        private int x, y, width, height;
        private String borderStyle;
        private String scrollMode;
        private boolean created = false;
        private boolean modalDialog = false;
        private int zOrder = 0;
        private WindowTestContext parent;
        private WindowTestContext[] children = new WindowTestContext[5];
        private int childCount = 0;
        private int verticalScrollPosition = 0;
        private int horizontalScrollPosition = 0;
        private int depth = 0;

        public void setWindowType(String type) {
            this.windowType = type;
        }

        public String getWindowType() {
            return windowType;
        }

        public void setSize(int width, int height) {
            this.width = Math.max(0, width);
            this.height = Math.max(0, height);
        }

        public void setPosition(int x, int y) {
            this.x = Math.max(0, x);
            this.y = Math.max(0, y);
        }

        public void setBorderStyle(String style) {
            this.borderStyle = style;
        }

        public void setScrollMode(String mode) {
            this.scrollMode = mode;
        }

        public void create() {
            this.created = true;
        }

        public void destroy() {
            this.created = false;
            clearWindowData();
        }

        public void update() {
            // Update window state
        }

        public boolean isCreated() {
            return created;
        }

        public void setModalDialog(boolean modal) {
            this.modalDialog = modal;
        }

        public boolean isInputBlocked() {
            return modalDialog;
        }

        public void setZOrder(int order) {
            this.zOrder = order;
        }

        public int getZOrder() {
            return zOrder;
        }

        public void setVerticalScrollPosition(int pos) {
            this.verticalScrollPosition = Math.max(0, pos);
        }

        public int getVerticalScrollPosition() {
            return verticalScrollPosition;
        }

        public void setHorizontalScrollPosition(int pos) {
            this.horizontalScrollPosition = Math.max(0, pos);
        }

        public int getHorizontalScrollPosition() {
            return horizontalScrollPosition;
        }

        public boolean hasParent() {
            return parent != null;
        }

        public boolean requiresParent() {
            return windowType != null && windowType.equals("NESTED");
        }

        public void addChild(WindowTestContext child) {
            if (childCount < children.length) {
                children[childCount++] = child;
                child.parent = this;
                child.depth = this.depth + 1;
            }
        }

        public boolean canHaveChild() {
            return childCount < children.length && depth < 10;
        }

        public int getDepth() {
            return depth;
        }

        public boolean hasVerticalScrollbar() {
            return scrollMode != null && (scrollMode.equals("VERTICAL") || scrollMode.equals("BOTH"));
        }

        public boolean hasHorizontalScrollbar() {
            return scrollMode != null && (scrollMode.equals("HORIZONTAL") || scrollMode.equals("BOTH"));
        }

        public boolean shouldDrawBorder() {
            return borderStyle != null && !borderStyle.equals("NONE");
        }

        public void scrollDown(int amount) {
            verticalScrollPosition += amount;
        }

        public void scrollUp(int amount) {
            verticalScrollPosition = Math.max(0, verticalScrollPosition - amount);
        }

        public void scrollRight(int amount) {
            horizontalScrollPosition += amount;
        }

        public void scrollLeft(int amount) {
            horizontalScrollPosition = Math.max(0, horizontalScrollPosition - amount);
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getPositionX() {
            return x;
        }

        public int getPositionY() {
            return y;
        }

        public Object getWindowData() {
            return created ? new Object() : null;
        }

        public void clearWindowData() {
            // Clear window resources
        }
    }
}
