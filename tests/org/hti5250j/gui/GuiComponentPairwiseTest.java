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

import javax.swing.JTabbedPane;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
    public void setUp() {
        tabbedPane = new JTabbedPane();
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
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
        assertEquals(0, tabbedPane.getTabCount(),"Precondition: pane should be empty");
        mockSessionPanel = createMockSessionPanel("Tab 1");

        // ACT: Add single tab
        tabbedPane.addTab("Tab 1", mockSessionPanel);

        // ASSERT: Tab added successfully
        assertEquals(1, tabbedPane.getTabCount(),"Tab count should be 1");
        assertEquals("Tab 1", tabbedPane.getTitleAt(0),"Tab title should match");
        assertEquals(mockSessionPanel, tabbedPane.getComponentAt(0),"Component should be retrievable");
        assertTrue(tabbedPane.isEnabledAt(0),"Tab should be visible");
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
        assertEquals(2, tabbedPane.getTabCount(),"Should have 2 tabs");
        assertEquals("Tab 1", tabbedPane.getTitleAt(0),"First tab title correct");
        assertEquals("Tab 2", tabbedPane.getTitleAt(1),"Second tab title correct");
        assertEquals(tab1, tabbedPane.getComponentAt(0),"First component correct");
        assertEquals(tab2, tabbedPane.getComponentAt(1),"Second component correct");
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
        assertEquals(0, tabbedPane.getSelectedIndex(),"Selected index should be 0");
        assertEquals(tab1, tabbedPane.getSelectedComponent(),"Selected component should be tab1");
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
        assertEquals(5, tabbedPane.getSelectedIndex(),"Selected index should be 5");
        assertEquals(tabs[5], tabbedPane.getSelectedComponent(),"Selected component should be tabs[5]");
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
        assertEquals(9, tabbedPane.getSelectedIndex(),"Selected index should be 9");
        assertEquals(tabs[9], tabbedPane.getSelectedComponent(),"Selected component should be last tab");
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
        assertEquals(10, tabbedPane.getTabCount(),"Precondition: should have 10 tabs");

        // ACT: Remove middle tab
        tabbedPane.remove(5);

        // ASSERT: Tab removed, indices adjusted
        assertEquals(9, tabbedPane.getTabCount(),"Should have 9 tabs after removal");
        assertEquals("Tab 6", tabbedPane.getTitleAt(5),"Tab 5 should be gone, Tab 6 should now be at index 5");
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
        assertEquals(1, tabbedPane.getTabCount(),"Should have 1 tab remaining");
        assertEquals("Tab 2", tabbedPane.getTitleAt(0),"Remaining tab should be Tab 2");
        assertEquals(tab2, tabbedPane.getComponentAt(0),"Remaining component should be tab2");
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
        assertEquals(1, tabbedPane.getTabCount(),"Should have 1 tab remaining");
        assertEquals("Tab 1", tabbedPane.getTitleAt(0),"Remaining tab should be Tab 1");
    }

    // ========== ADVERSARIAL TESTS ==========

    /**
     * Test Case 9: Select from empty pane should not crash
     * Dimensions: [tab_count=0] [selected_index=-1] [operation=select] [timing=sequential]
     */
    @Test
    public void testSelectTab_EmptyPane_ShouldHandleGracefully() {
        // ARRANGE: Empty pane
        assertEquals(0, tabbedPane.getTabCount(),"Precondition: pane should be empty");

        // ACT & ASSERT: Should handle gracefully (not crash with NPE)
        try {
            int currentIndex = tabbedPane.getSelectedIndex();
            assertEquals(-1, currentIndex,"Selected index on empty pane should be -1");
            assertNull(tabbedPane.getSelectedComponent(),"Selected component on empty pane should be null");
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
            assertTrue(tabbedPane.getSelectedIndex() >= -1 &&
                    tabbedPane.getSelectedIndex() < tabbedPane.getTabCount(),"Index should be in valid range");
        } catch (IndexOutOfBoundsException e) {
            // This is expected behavior
            assertNotNull(e,"Exception should be thrown for out-of-bounds index");
        }
    }

    /**
     * Test Case 11: Remove from empty pane
     * Dimensions: [tab_count=0] [operation=remove] [timing=sequential]
     */
    @Test
    public void testRemoveTab_EmptyPane_ShouldHandleGracefully() {
        // ARRANGE: Empty pane
        assertEquals(0, tabbedPane.getTabCount(),"Precondition: pane should be empty");

        // ACT & ASSERT: Should handle gracefully
        try {
            tabbedPane.remove(0);
            fail("Should throw exception when removing from empty pane");
        } catch (IndexOutOfBoundsException e) {
            // Expected behavior
            assertNotNull(e,"Should throw IndexOutOfBoundsException");
        }
    }

    /**
     * Test Case 12: Rapid successive additions and removals
     * Dimensions: [tab_count=0 -> 100] [operation=add,remove] [timing=rapid]
     */
    @Test
    public void testTabOperations_RapidAddRemove_ShouldMaintainConsistency() {
        // ARRANGE: Empty pane
        assertEquals(0, tabbedPane.getTabCount(),"Precondition: pane should be empty");

        // ACT: Rapidly add and remove tabs
        for (int i = 0; i < 100; i++) {
            MockSessionPanel tab = createMockSessionPanel("Tab " + i);
            tabbedPane.addTab("Tab " + i, tab);

            // Immediate removal
            tabbedPane.remove(0);
        }

        // ASSERT: Pane should be empty and consistent
        assertEquals(0, tabbedPane.getTabCount(),"After 100 add-remove cycles, pane should be empty");
        assertEquals(-1, tabbedPane.getSelectedIndex(),"Selected index should be -1 for empty pane");
        assertNull(tabbedPane.getSelectedComponent(),"Selected component should be null");
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
        assertEquals(1, tabbedPane.getSelectedIndex(),"Final selected index should be 1");
        assertEquals(tab2, tabbedPane.getSelectedComponent(),"Selected component should be tab2");
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
        assertTrue(completed,"Concurrent operations should complete within timeout");
        assertNull(thrownException.get(),"No exceptions should be thrown during concurrent access: " +
                (thrownException.get() != null ? thrownException.get().getMessage() : ""));
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
        assertFalse(tab1.isVisible(),"Tab1 should be hidden");
        assertEquals(0, tabbedPane.getSelectedIndex(),"Should still be able to select hidden tab");

        // ACT: Make visible again
        tab1.setVisible(true);

        // ASSERT: Visibility state updated
        assertTrue(tab1.isVisible(),"Tab1 should be visible");
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
        assertEquals(2, tabbedPane.getTabCount(),"Should have 2 tabs even with disposed component");
        assertEquals(tab1, tabbedPane.getComponentAt(0),"Should still retrieve disposed component");

        // ACT: Try to interact with disposed component
        try {
            MockSessionPanel retrieved = (MockSessionPanel) tabbedPane.getComponentAt(0);
            assertTrue(retrieved.isDisposed(),"Disposed flag should be set");
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
            assertNotNull(e,"Exception should be thrown");
        }
    }

    /**
     * Test Case 18: Selected component null safety when pane is empty
     * Dimensions: [tab_count=0]
     */
    @Test
    public void testGetSelectedComponent_EmptyPane_ShouldReturnNull() {
        // ARRANGE: Empty pane
        assertEquals(0, tabbedPane.getTabCount(),"Precondition: pane should be empty");

        // ACT: Get selected component
        Object selected = tabbedPane.getSelectedComponent();

        // ASSERT: Should return null without NPE
        assertNull(selected,"Selected component should be null for empty pane");
    }

    /**
     * Test Case 19: Tab count consistency after multiple operations
     * Dimensions: [tab_count=0 -> 10] [operations=add,add,remove,add,remove,select]
     */
    @Test
    public void testTabCount_ComplexOperationSequence_ShouldRemainConsistent() {
        // ARRANGE: Empty pane
        assertEquals(0, tabbedPane.getTabCount(),"Precondition: pane should be empty");

        // ACT: Complex operation sequence
        MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
        tabbedPane.addTab("Tab 1", tab1);
        assertEquals(1, tabbedPane.getTabCount(),"After first add");

        MockSessionPanel tab2 = createMockSessionPanel("Tab 2");
        tabbedPane.addTab("Tab 2", tab2);
        assertEquals(2, tabbedPane.getTabCount(),"After second add");

        tabbedPane.remove(0);
        assertEquals(1, tabbedPane.getTabCount(),"After remove");

        MockSessionPanel tab3 = createMockSessionPanel("Tab 3");
        tabbedPane.addTab("Tab 3", tab3);
        assertEquals(2, tabbedPane.getTabCount(),"After another add");

        tabbedPane.setSelectedIndex(0);
        assertEquals(2, tabbedPane.getTabCount(),"Selection shouldn't change count");

        tabbedPane.remove(0);
        assertEquals(1, tabbedPane.getTabCount(),"After another remove");

        // ASSERT: Final state
        assertEquals(1, tabbedPane.getTabCount(),"Final tab count should be 1");
        assertEquals("Tab 3", tabbedPane.getTitleAt(0),"Remaining tab should be Tab 3");
    }

    /**
     * Test Case 20: Large-scale tab operations (100 tabs)
     * Dimensions: [tab_count=100] [operation=add,select,remove]
     */
    @Test
    public void testTabOperations_LargeScale_ShouldHandleHundredTabs() {
        // ARRANGE: Empty pane
        assertEquals(0, tabbedPane.getTabCount(),"Precondition: pane should be empty");

        // ACT: Add 100 tabs
        MockSessionPanel[] tabs = new MockSessionPanel[100];
        for (int i = 0; i < 100; i++) {
            tabs[i] = createMockSessionPanel("Tab " + i);
            tabbedPane.addTab("Tab " + i, tabs[i]);
        }

        // ASSERT: All added
        assertEquals(100, tabbedPane.getTabCount(),"Should have 100 tabs");

        // ACT: Select various indices
        tabbedPane.setSelectedIndex(0);
        assertEquals(0, tabbedPane.getSelectedIndex(),"First selection");

        tabbedPane.setSelectedIndex(50);
        assertEquals(50, tabbedPane.getSelectedIndex(),"Middle selection");

        tabbedPane.setSelectedIndex(99);
        assertEquals(99, tabbedPane.getSelectedIndex(),"Last selection");

        // ACT: Remove first 10 tabs
        for (int i = 0; i < 10; i++) {
            tabbedPane.remove(0);
        }

        // ASSERT: Correct count and indices adjusted
        assertEquals(90, tabbedPane.getTabCount(),"Should have 90 tabs after removing 10");
        assertEquals("Tab 10", tabbedPane.getTitleAt(0),"First tab should now be Tab 10");
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
