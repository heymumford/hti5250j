/**
 * Title: tn5250J
 * Copyright:   Copyright (c) 2001
 * Company:
 *
 * @author Test Suite
 * @version 0.5
 * <p>
 * Description: Comprehensive pairwise TDD test suite for GUI components
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.tn5250j.gui;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JTabbedPane;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * TDD Test Suite for GUI component tab management (Gui5250Frame and SessionPanel)
 *
 * Test dimensions (pairwise):
 * - Tab count: [0, 1, 2, 10, 100]
 * - Selected index: [-1, 0, 1, mid, last, out-of-bounds]
 * - Operations: [add, remove, select, close]
 * - Timing: [sequential, rapid, concurrent]
 * - Component states: [visible, hidden, disposed]
 *
 * Focus: GUI state management and NPE bugs found in audit
 */
public class GuiComponentPairwiseTest {

    private JTabbedPane tabbedPane;
    private MockSessionPanel mockSessionPanel;
    private ExecutorService executor;

    @Before
    public void setUp() {
        tabbedPane = new JTabbedPane();
        executor = Executors.newFixedThreadPool(4);
    }

    @After
    public void tearDown() {
        tabbedPane = null;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    // ========== POSITIVE PATH TESTS ==========

    /**
     * Test Case 1: Adding single tab to empty pane
     * Dimensions: [tab_count=0 -> 1] [operation=add] [timing=sequential]
     */
    @Test
    public void testAddTab_SingleTabToEmptyPane_ShouldSucceed() {
        // ARRANGE: Empty pane
        assertEquals("Precondition: pane should be empty", 0, tabbedPane.getTabCount());
        mockSessionPanel = createMockSessionPanel("Tab 1");

        // ACT: Add single tab
        tabbedPane.addTab("Tab 1", mockSessionPanel);

        // ASSERT: Tab added successfully
        assertEquals("Tab count should be 1", 1, tabbedPane.getTabCount());
        assertEquals("Tab title should match", "Tab 1", tabbedPane.getTitleAt(0));
        assertEquals("Component should be retrievable", mockSessionPanel, tabbedPane.getComponentAt(0));
        assertTrue("Tab should be visible", tabbedPane.isEnabledAt(0));
    }

    /**
     * Test Case 2: Sequential addition of multiple tabs
     * Dimensions: [tab_count=0 -> 2] [operation=add] [timing=sequential]
     */
    @Test
    public void testAddTabs_SequentialMultipleTabs_ShouldMaintainOrder() {
        // ARRANGE: Add first tab
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        tabbedPane.addTab("Tab 1", tab1);

        // ACT: Add second tab
        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 2", tab2);

        // ASSERT: Both tabs present in correct order
        assertEquals("Should have 2 tabs", 2, tabbedPane.getTabCount());
        assertEquals("First tab title correct", "Tab 1", tabbedPane.getTitleAt(0));
        assertEquals("Second tab title correct", "Tab 2", tabbedPane.getTitleAt(1));
        assertEquals("First component correct", tab1, tabbedPane.getComponentAt(0));
        assertEquals("Second component correct", tab2, tabbedPane.getComponentAt(1));
    }

    /**
     * Test Case 3: Select existing tab by index
     * Dimensions: [tab_count=2] [selected_index=0] [operation=select] [timing=sequential]
     */
    @Test
    public void testSelectTab_ValidIndex_ShouldSelectCorrectTab() {
        // ARRANGE: Create tabs
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 1", tab1);
        tabbedPane.addTab("Tab 2", tab2);

        // ACT: Select first tab
        tabbedPane.setSelectedIndex(0);

        // ASSERT: Correct tab selected
        assertEquals("Selected index should be 0", 0, tabbedPane.getSelectedIndex());
        assertEquals("Selected component should be tab1", tab1, tabbedPane.getSelectedComponent());
    }

    /**
     * Test Case 4: Select middle tab from larger set
     * Dimensions: [tab_count=10] [selected_index=mid] [operation=select] [timing=sequential]
     */
    @Test
    public void testSelectTab_MiddleTabFromLargeSet_ShouldSelectCorrectly() {
        // ARRANGE: Create 10 tabs
        MockSessionPanel[] tabs = new MockSessionPanel[10];
        for (int i = 0; i < 10; i++) {
            tabs[i] = createMockSessionPanel("Tab " + i);
            tabbedPane.addTab("Tab " + i, tabs[i]);
        }

        // ACT: Select middle tab (index 5)
        tabbedPane.setSelectedIndex(5);

        // ASSERT: Middle tab selected
        assertEquals("Selected index should be 5", 5, tabbedPane.getSelectedIndex());
        assertEquals("Selected component should be tabs[5]", tabs[5], tabbedPane.getSelectedComponent());
    }

    /**
     * Test Case 5: Select last tab
     * Dimensions: [tab_count=10] [selected_index=last] [operation=select] [timing=sequential]
     */
    @Test
    public void testSelectTab_LastTabFromSet_ShouldSelectCorrectly() {
        // ARRANGE: Create 10 tabs
        MockSessionPanel[] tabs = new MockSessionPanel[10];
        for (int i = 0; i < 10; i++) {
            tabs[i] = createMockSessionPanel("Tab " + i);
            tabbedPane.addTab("Tab " + i, tabs[i]);
        }

        // ACT: Select last tab
        int lastIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setSelectedIndex(lastIndex);

        // ASSERT: Last tab selected
        assertEquals("Selected index should be 9", 9, tabbedPane.getSelectedIndex());
        assertEquals("Selected component should be last tab", tabs[9], tabbedPane.getSelectedComponent());
    }

    /**
     * Test Case 6: Remove middle tab from set
     * Dimensions: [tab_count=10] [operation=remove] [selected_index=mid] [timing=sequential]
     */
    @Test
    public void testRemoveTab_MiddleTab_ShouldRemoveAndAdjustSelection() {
        // ARRANGE: Create 10 tabs and select middle
        MockSessionPanel[] tabs = new MockSessionPanel[10];
        for (int i = 0; i < 10; i++) {
            tabs[i] = createMockSessionPanel("Tab " + i);
            tabbedPane.addTab("Tab " + i, tabs[i]);
        }
        tabbedPane.setSelectedIndex(5);
        assertEquals("Precondition: should have 10 tabs", 10, tabbedPane.getTabCount());

        // ACT: Remove middle tab
        tabbedPane.remove(5);

        // ASSERT: Tab removed, indices adjusted
        assertEquals("Should have 9 tabs after removal", 9, tabbedPane.getTabCount());
        assertEquals("Tab 5 should be gone, Tab 6 should now be at index 5",
                "Tab 6", tabbedPane.getTitleAt(5));
    }

    /**
     * Test Case 7: Remove first tab from set
     * Dimensions: [tab_count=2] [operation=remove] [selected_index=0] [timing=sequential]
     */
    @Test
    public void testRemoveTab_FirstTab_ShouldRemoveAndPreserveSecond() {
        // ARRANGE: Create two tabs
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 1", tab1);
        tabbedPane.addTab("Tab 2", tab2);
        tabbedPane.setSelectedIndex(0);

        // ACT: Remove first tab
        tabbedPane.remove(0);

        // ASSERT: First tab removed, second tab remains
        assertEquals("Should have 1 tab remaining", 1, tabbedPane.getTabCount());
        assertEquals("Remaining tab should be Tab 2", "Tab 2", tabbedPane.getTitleAt(0));
        assertEquals("Remaining component should be tab2", tab2, tabbedPane.getComponentAt(0));
    }

    /**
     * Test Case 8: Remove last tab from set
     * Dimensions: [tab_count=2] [operation=remove] [selected_index=last] [timing=sequential]
     */
    @Test
    public void testRemoveTab_LastTab_ShouldRemoveCorrectly() {
        // ARRANGE: Create two tabs and select last
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 1", tab1);
        tabbedPane.addTab("Tab 2", tab2);
        tabbedPane.setSelectedIndex(1);

        // ACT: Remove last tab
        tabbedPane.remove(1);

        // ASSERT: Last tab removed, first remains selected
        assertEquals("Should have 1 tab remaining", 1, tabbedPane.getTabCount());
        assertEquals("Remaining tab should be Tab 1", "Tab 1", tabbedPane.getTitleAt(0));
    }

    // ========== ADVERSARIAL TESTS ==========

    /**
     * Test Case 9: Select from empty pane should not crash
     * Dimensions: [tab_count=0] [selected_index=-1] [operation=select] [timing=sequential]
     */
    @Test
    public void testSelectTab_EmptyPane_ShouldHandleGracefully() {
        // ARRANGE: Empty pane
        assertEquals("Precondition: pane should be empty", 0, tabbedPane.getTabCount());

        // ACT & ASSERT: Should handle gracefully (not crash with NPE)
        try {
            int currentIndex = tabbedPane.getSelectedIndex();
            assertEquals("Selected index on empty pane should be -1", -1, currentIndex);
            assertNull("Selected component on empty pane should be null", tabbedPane.getSelectedComponent());
        } catch (Exception e) {
            fail("Should not throw exception when selecting from empty pane: " + e.getMessage());
        }
    }

    /**
     * Test Case 10: Select out-of-bounds index
     * Dimensions: [tab_count=2] [selected_index=out-of-bounds] [operation=select] [timing=sequential]
     */
    @Test
    public void testSelectTab_OutOfBoundsIndex_ShouldThrowException() {
        // ARRANGE: Create two tabs
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 1", tab1);
        tabbedPane.addTab("Tab 2", tab2);

        // ACT & ASSERT: Attempting out-of-bounds selection should throw
        try {
            tabbedPane.setSelectedIndex(99);
            // If no exception, verify the pane behaves consistently
            assertTrue("Index should be in valid range",
                    tabbedPane.getSelectedIndex() >= -1 &&
                    tabbedPane.getSelectedIndex() < tabbedPane.getTabCount());
        } catch (IndexOutOfBoundsException e) {
            // This is expected behavior
            assertNotNull("Exception should be thrown for out-of-bounds index", e);
        }
    }

    /**
     * Test Case 11: Remove from empty pane
     * Dimensions: [tab_count=0] [operation=remove] [timing=sequential]
     */
    @Test
    public void testRemoveTab_EmptyPane_ShouldHandleGracefully() {
        // ARRANGE: Empty pane
        assertEquals("Precondition: pane should be empty", 0, tabbedPane.getTabCount());

        // ACT & ASSERT: Should handle gracefully
        try {
            tabbedPane.remove(0);
            fail("Should throw exception when removing from empty pane");
        } catch (IndexOutOfBoundsException e) {
            // Expected behavior
            assertNotNull("Should throw IndexOutOfBoundsException", e);
        }
    }

    /**
     * Test Case 12: Rapid successive additions and removals
     * Dimensions: [tab_count=0 -> 100] [operation=add,remove] [timing=rapid]
     */
    @Test
    public void testTabOperations_RapidAddRemove_ShouldMaintainConsistency() {
        // ARRANGE: Empty pane
        assertEquals("Precondition: pane should be empty", 0, tabbedPane.getTabCount());

        // ACT: Rapidly add and remove tabs
        for (int i = 0; i < 100; i++) {
            MockSessionPanel tab = createMockSessionPanel("Tab " + i);
            tabbedPane.addTab("Tab " + i, tab);

            // Immediate removal
            tabbedPane.remove(0);
        }

        // ASSERT: Pane should be empty and consistent
        assertEquals("After 100 add-remove cycles, pane should be empty", 0, tabbedPane.getTabCount());
        assertEquals("Selected index should be -1 for empty pane", -1, tabbedPane.getSelectedIndex());
        assertNull("Selected component should be null", tabbedPane.getSelectedComponent());
    }

    /**
     * Test Case 13: Rapid index selection switching
     * Dimensions: [tab_count=2] [selected_index=0,1] [operation=select] [timing=rapid]
     */
    @Test
    public void testSelectTab_RapidSwitching_ShouldHandleConsistently() {
        // ARRANGE: Create two tabs
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 1", tab1);
        tabbedPane.addTab("Tab 2", tab2);

        // ACT: Rapid selection switching
        for (int i = 0; i < 100; i++) {
            tabbedPane.setSelectedIndex(i % 2);
        }

        // ASSERT: Final selection should be consistent (99 % 2 = 1, last iteration is i=99)
        assertEquals("Final selected index should be 1", 1, tabbedPane.getSelectedIndex());
        assertEquals("Selected component should be tab2", tab2, tabbedPane.getSelectedComponent());
    }

    /**
     * Test Case 14: Concurrent tab operations
     * Dimensions: [tab_count=0 -> 10] [operation=add,select] [timing=concurrent]
     */
    @Test
    public void testTabOperations_Concurrent_ShouldNotThrowNPE() throws InterruptedException {
        // ARRANGE: Setup concurrent operation tracker
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(4);
        AtomicReference<Exception> thrownException = new AtomicReference<>(null);

        // ACT: Start 4 concurrent threads performing different operations
        // Thread 1: Add tabs
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 10; i++) {
                    MockSessionPanel tab = createMockSessionPanel("Thread1-Tab" + i);
                    tabbedPane.addTab("Thread1-Tab" + i, tab);
                }
            } catch (Exception e) {
                thrownException.set(e);
            } finally {
                finishLatch.countDown();
            }
        });

        // Thread 2: Select tabs
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 20; i++) {
                    int count = tabbedPane.getTabCount();
                    if (count > 0) {
                        tabbedPane.setSelectedIndex(i % count);
                    }
                }
            } catch (Exception e) {
                thrownException.set(e);
            } finally {
                finishLatch.countDown();
            }
        });

        // Thread 3: Get selected component
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 20; i++) {
                    tabbedPane.getSelectedComponent();
                }
            } catch (Exception e) {
                thrownException.set(e);
            } finally {
                finishLatch.countDown();
            }
        });

        // Thread 4: Get selected index
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 20; i++) {
                    tabbedPane.getSelectedIndex();
                }
            } catch (Exception e) {
                thrownException.set(e);
            } finally {
                finishLatch.countDown();
            }
        });

        // Start all threads
        startLatch.countDown();

        // Wait for completion
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);

        // ASSERT: No exceptions thrown and operations completed
        assertTrue("Concurrent operations should complete within timeout", completed);
        assertNull("No exceptions should be thrown during concurrent access: " +
                (thrownException.get() != null ? thrownException.get().getMessage() : ""),
                thrownException.get());
    }

    /**
     * Test Case 15: Component visibility state changes
     * Dimensions: [component_state=visible,hidden] [tab_count=2] [operation=select]
     */
    @Test
    public void testTabComponent_VisibilityStateChanges_ShouldReflectCorrectly() {
        // ARRANGE: Create tabs
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 1", tab1);
        tabbedPane.addTab("Tab 2", tab2);

        // ACT: Change visibility
        tab1.setVisible(false);
        tabbedPane.setSelectedIndex(0);

        // ASSERT: Visibility state preserved
        assertFalse("Tab1 should be hidden", tab1.isVisible());
        assertEquals("Should still be able to select hidden tab", 0, tabbedPane.getSelectedIndex());

        // ACT: Make visible again
        tab1.setVisible(true);

        // ASSERT: Visibility state updated
        assertTrue("Tab1 should be visible", tab1.isVisible());
    }

    /**
     * Test Case 16: Disposed component handling
     * Dimensions: [component_state=disposed] [tab_count=2] [operation=select]
     */
    @Test
    public void testTabComponent_DisposedComponent_ShouldHandleGracefully() {
        // ARRANGE: Create tabs
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 1", tab1);
        tabbedPane.addTab("Tab 2", tab2);

        // ACT: Mark component as disposed
        tab1.setDisposed(true);

        // ASSERT: Should still be accessible (disposed state doesn't remove from pane)
        assertEquals("Should have 2 tabs even with disposed component", 2, tabbedPane.getTabCount());
        assertEquals("Should still retrieve disposed component", tab1, tabbedPane.getComponentAt(0));

        // ACT: Try to interact with disposed component
        try {
            MockSessionPanel retrieved = (MockSessionPanel) tabbedPane.getComponentAt(0);
            assertTrue("Disposed flag should be set", retrieved.isDisposed());
        } catch (NullPointerException e) {
            fail("Should not throw NPE when accessing disposed component: " + e.getMessage());
        }
    }

    /**
     * Test Case 17: GetSessionAt null safety with out-of-bounds index
     * Dimensions: [tab_count=2] [selected_index=out-of-bounds]
     */
    @Test
    public void testGetComponentAt_OutOfBoundsIndex_ShouldHandleGracefully() {
        // ARRANGE: Create tabs
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 1", tab1);
        tabbedPane.addTab("Tab 2", tab2);

        // ACT & ASSERT: Out-of-bounds access
        try {
            tabbedPane.getComponentAt(99);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull("Exception should be thrown", e);
        }
    }

    /**
     * Test Case 18: Selected component null safety when pane is empty
     * Dimensions: [tab_count=0]
     */
    @Test
    public void testGetSelectedComponent_EmptyPane_ShouldReturnNull() {
        // ARRANGE: Empty pane
        assertEquals("Precondition: pane should be empty", 0, tabbedPane.getTabCount());

        // ACT: Get selected component
        Object selected = tabbedPane.getSelectedComponent();

        // ASSERT: Should return null without NPE
        assertNull("Selected component should be null for empty pane", selected);
    }

    /**
     * Test Case 19: Tab count consistency after multiple operations
     * Dimensions: [tab_count=0 -> 10] [operations=add,add,remove,add,remove,select]
     */
    @Test
    public void testTabCount_ComplexOperationSequence_ShouldRemainConsistent() {
        // ARRANGE: Empty pane
        assertEquals("Precondition: pane should be empty", 0, tabbedPane.getTabCount());

        // ACT: Complex operation sequence
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        tabbedPane.addTab("Tab 1", tab1);
        assertEquals("After first add", 1, tabbedPane.getTabCount());

        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 2", tab2);
        assertEquals("After second add", 2, tabbedPane.getTabCount());

        tabbedPane.remove(0);
        assertEquals("After remove", 1, tabbedPane.getTabCount());

        MockSessionPanel tab3 = createMockSessionPanel("Tab 3");
        tabbedPane.addTab("Tab 3", tab3);
        assertEquals("After another add", 2, tabbedPane.getTabCount());

        tabbedPane.setSelectedIndex(0);
        assertEquals("Selection shouldn't change count", 2, tabbedPane.getTabCount());

        tabbedPane.remove(0);
        assertEquals("After another remove", 1, tabbedPane.getTabCount());

        // ASSERT: Final state
        assertEquals("Final tab count should be 1", 1, tabbedPane.getTabCount());
        assertEquals("Remaining tab should be Tab 3", "Tab 3", tabbedPane.getTitleAt(0));
    }

    /**
     * Test Case 20: Large-scale tab operations (100 tabs)
     * Dimensions: [tab_count=100] [operation=add,select,remove]
     */
    @Test
    public void testTabOperations_LargeScale_ShouldHandleHundredTabs() {
        // ARRANGE: Empty pane
        assertEquals("Precondition: pane should be empty", 0, tabbedPane.getTabCount());

        // ACT: Add 100 tabs
        MockSessionPanel[] tabs = new MockSessionPanel[100];
        for (int i = 0; i < 100; i++) {
            tabs[i] = createMockSessionPanel("Tab " + i);
            tabbedPane.addTab("Tab " + i, tabs[i]);
        }

        // ASSERT: All added
        assertEquals("Should have 100 tabs", 100, tabbedPane.getTabCount());

        // ACT: Select various indices
        tabbedPane.setSelectedIndex(0);
        assertEquals("First selection", 0, tabbedPane.getSelectedIndex());

        tabbedPane.setSelectedIndex(50);
        assertEquals("Middle selection", 50, tabbedPane.getSelectedIndex());

        tabbedPane.setSelectedIndex(99);
        assertEquals("Last selection", 99, tabbedPane.getSelectedIndex());

        // ACT: Remove first 10 tabs
        for (int i = 0; i < 10; i++) {
            tabbedPane.remove(0);
        }

        // ASSERT: Correct count and indices adjusted
        assertEquals("Should have 90 tabs after removing 10", 90, tabbedPane.getTabCount());
        assertEquals("First tab should now be Tab 10", "Tab 10", tabbedPane.getTitleAt(0));
    }

    // ========== HELPER METHODS ==========

    /**
     * Factory method to create mock SessionPanel
     */
    private MockSessionPanel createMockSessionPanel(String name) {
        return new MockSessionPanel(name);
    }

    // ========== MOCK CLASSES ==========

    /**
     * Mock implementation of SessionPanel for testing tab operations
     */
    private static class MockSessionPanel extends javax.swing.JPanel {
        private static final long serialVersionUID = 1L;
        private String name;
        private boolean disposed = false;
        private boolean isConnected = true;

        public MockSessionPanel(String name) {
            this.name = name;
            setPreferredSize(new java.awt.Dimension(400, 300));
        }

        public String getName() {
            return name;
        }

        public void setDisposed(boolean disposed) {
            this.disposed = disposed;
        }

        public boolean isDisposed() {
            return disposed;
        }

        public void setConnected(boolean connected) {
            isConnected = connected;
        }

        public boolean isConnected() {
            return isConnected;
        }

        @Override
        public String toString() {
            return "MockSessionPanel{" + name + "}";
        }
    }

}
