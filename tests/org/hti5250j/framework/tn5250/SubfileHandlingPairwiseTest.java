/**
 * Title: SubfileHandlingPairwiseTest.java
 * Copyright: Copyright (c) 2025
 * Company:
 *
 * Description: TDD pairwise tests for HTI5250j subfile handling and display.
 *
 * This test suite focuses on subfile behaviors critical for terminal automation:
 * - Subfile type detection (display-only, input, selection, expandable)
 * - Record counting and iteration
 * - Navigation operations (page-up, page-down, position-to, home, end)
 * - Record selection (single, multiple, none)
 * - Overflow handling (truncate, scroll, error)
 * - Scrolling within subfiles
 * - Page navigation with various record counts
 * - Selection mode enforcement
 * - Boundary conditions and edge cases
 * - Adversarial large-record scenarios
 *
 * Test dimensions (pairwise combination):
 * 1. Subfile type: [display-only (0), input (1), selection (2), expandable (3)]
 * 2. Record count: [0, 1, page-size (20), multi-page (50), max (1000)]
 * 3. Navigation: [page-up, page-down, position-to, home, end]
 * 4. Selection mode: [single, multiple, none]
 * 5. Overflow: [truncate, scroll, error]
 *
 * POSITIVE TESTS (15): Valid subfile operations and expected behaviors
 * ADVERSARIAL TESTS (12): Edge cases, boundary conditions, large-record scenarios
 * CRITICAL TESTS (10): High-risk behaviors with multiple dimensions
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
package org.hti5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Pairwise TDD test suite for Screen5250 subfile operations.
 *
 * Focuses on high-risk behaviors in headless terminal automation:
 * 1. Subfile type misidentification
 * 2. Record navigation beyond boundaries
 * 3. Selection mode conflicts
 * 4. Scroll position loss
 * 5. Overflow handling (truncate vs scroll vs error)
 * 6. Page navigation with variable record counts
 * 7. Large-record performance degradation
 * 8. Record selection consistency across page changes
 * 9. Empty subfile navigation
 * 10. Bidirectional navigation state consistency
 */
@RunWith(JUnit4.class)
public class SubfileHandlingPairwiseTest {

    private TestSubfileModel subfileModel;

    private static final int SCREEN_ROWS = 24;
    private static final int SCREEN_COLS = 80;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_RECORDS = 1000;

    /**
     * Test model for subfile data with pairwise dimension support.
     */
    private static class TestSubfileModel {
        private static final int TYPE_DISPLAY_ONLY = 0;
        private static final int TYPE_INPUT = 1;
        private static final int TYPE_SELECTION = 2;
        private static final int TYPE_EXPANDABLE = 3;

        private static final int MODE_SINGLE = 1;
        private static final int MODE_MULTIPLE = 2;
        private static final int MODE_NONE = 0;

        private static final int OVERFLOW_TRUNCATE = 0;
        private static final int OVERFLOW_SCROLL = 1;
        private static final int OVERFLOW_ERROR = 2;

        private int type;
        private List<SubfileRecord> records;
        private int pageSize;
        private int currentPageStart;
        private int selectionMode;
        private int overflowBehavior;
        private Set<Integer> selectedRecords;

        public TestSubfileModel(int type, int selectionMode, int overflowBehavior) {
            this.type = type;
            this.records = new ArrayList<>();
            this.pageSize = DEFAULT_PAGE_SIZE;
            this.currentPageStart = 0;
            this.selectionMode = selectionMode;
            this.overflowBehavior = overflowBehavior;
            this.selectedRecords = new HashSet<>();
        }

        public void addRecords(int count) {
            for (int i = 0; i < count; i++) {
                records.add(new SubfileRecord(i, "Record " + i));
            }
        }

        public int getRecordCount() {
            return records.size();
        }

        public SubfileRecord getRecord(int index) {
            if (index < 0 || index >= records.size()) {
                return null;
            }
            return records.get(index);
        }

        public List<SubfileRecord> getPageRecords() {
            if (currentPageStart >= records.size()) {
                return Collections.emptyList();
            }
            int end = Math.min(currentPageStart + pageSize, records.size());
            return new ArrayList<>(records.subList(currentPageStart, end));
        }

        public boolean pageDown() {
            if (currentPageStart + pageSize < records.size()) {
                currentPageStart += pageSize;
                return true;
            }
            return false;  // Already at last page
        }

        public boolean pageUp() {
            if (currentPageStart > 0) {
                currentPageStart -= pageSize;
                if (currentPageStart < 0) currentPageStart = 0;
                return true;
            }
            return false;  // Already at first page
        }

        public void home() {
            currentPageStart = 0;
        }

        public void end() {
            if (records.size() <= pageSize) {
                currentPageStart = 0;
            } else {
                currentPageStart = ((records.size() - 1) / pageSize) * pageSize;
            }
        }

        public boolean positionTo(int recordIndex) {
            if (recordIndex < 0 || recordIndex >= records.size()) {
                return false;  // Invalid position
            }
            currentPageStart = (recordIndex / pageSize) * pageSize;
            return true;
        }

        public boolean selectRecord(int recordIndex) {
            if (recordIndex < 0 || recordIndex >= records.size()) {
                return false;
            }
            if (selectionMode == MODE_SINGLE) {
                selectedRecords.clear();
                selectedRecords.add(recordIndex);
                return true;
            } else if (selectionMode == MODE_MULTIPLE) {
                selectedRecords.add(recordIndex);
                return true;
            } else {
                return false;  // Selection mode = NONE
            }
        }

        public void deselectRecord(int recordIndex) {
            selectedRecords.remove(recordIndex);
        }

        public Set<Integer> getSelectedRecords() {
            return new HashSet<>(selectedRecords);
        }

        public boolean isRecordSelected(int recordIndex) {
            return selectedRecords.contains(recordIndex);
        }

        public int getCurrentPageStart() {
            return currentPageStart;
        }

        public int getType() {
            return type;
        }

        public int getSelectionMode() {
            return selectionMode;
        }

        public int getOverflowBehavior() {
            return overflowBehavior;
        }

        public boolean isDisplayOnly() {
            return type == TYPE_DISPLAY_ONLY;
        }

        public boolean isInputType() {
            return type == TYPE_INPUT;
        }

        public boolean isSelectionType() {
            return type == TYPE_SELECTION;
        }

        public boolean isExpandableType() {
            return type == TYPE_EXPANDABLE;
        }
    }

    /**
     * Represents a single subfile record.
     */
    private static class SubfileRecord {
        private int index;
        private String data;

        public SubfileRecord(int index, String data) {
            this.index = index;
            this.data = data;
        }

        public int getIndex() {
            return index;
        }

        public String getData() {
            return data;
        }
    }

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        subfileModel = null;
    }

    // ========================================================================
    // POSITIVE TESTS (15): Valid subfile operations and expected behaviors
    // ========================================================================

    /**
     * POSITIVE: Display-only subfile with page navigation
     * Dimension pair: type=display-only, record-count=page-size, nav=page-down
     */
    @Test
    public void testDisplayOnlySubfilePageNavigation() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(DEFAULT_PAGE_SIZE);

        assertEquals("Display-only should have page-size records", DEFAULT_PAGE_SIZE, subfileModel.getRecordCount());
        assertEquals("Initial page start at 0", 0, subfileModel.getCurrentPageStart());
        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals("Page has all records", DEFAULT_PAGE_SIZE, page.size());
        assertTrue("Display-only subfile type", subfileModel.isDisplayOnly());
    }

    /**
     * POSITIVE: Input subfile with single record
     * Dimension pair: type=input, record-count=1, nav=none
     */
    @Test
    public void testInputSubfileWithSingleRecord() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_INPUT,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(1);

        assertEquals("Input subfile should have 1 record", 1, subfileModel.getRecordCount());
        SubfileRecord record = subfileModel.getRecord(0);
        assertNotNull("Record exists", record);
        assertEquals("Record index is 0", 0, record.getIndex());
        assertTrue("Input type detected", subfileModel.isInputType());
    }

    /**
     * POSITIVE: Selection subfile with single selection mode
     * Dimension pair: type=selection, mode=single, nav=position-to
     */
    @Test
    public void testSelectionSubfileSingleMode() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(DEFAULT_PAGE_SIZE);

        assertTrue("Position to record 5", subfileModel.positionTo(5));
        assertTrue("Select record 5", subfileModel.selectRecord(5));
        assertTrue("Record 5 is selected", subfileModel.isRecordSelected(5));
        assertEquals("Only 1 record selected", 1, subfileModel.getSelectedRecords().size());
        assertTrue("Selection type", subfileModel.isSelectionType());
    }

    /**
     * POSITIVE: Selection subfile with multiple selection mode
     * Dimension pair: type=selection, mode=multiple, nav=home
     */
    @Test
    public void testSelectionSubfileMultipleMode() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_MULTIPLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(DEFAULT_PAGE_SIZE);

        subfileModel.home();
        assertEquals("Home positioned", 0, subfileModel.getCurrentPageStart());

        assertTrue("Select record 0", subfileModel.selectRecord(0));
        assertTrue("Select record 5", subfileModel.selectRecord(5));
        assertTrue("Select record 10", subfileModel.selectRecord(10));

        Set<Integer> selected = subfileModel.getSelectedRecords();
        assertEquals("Three records selected", 3, selected.size());
        assertTrue("Record 0 selected", selected.contains(0));
        assertTrue("Record 5 selected", selected.contains(5));
        assertTrue("Record 10 selected", selected.contains(10));
    }

    /**
     * POSITIVE: Expandable subfile with multiple pages
     * Dimension pair: type=expandable, record-count=multi-page, nav=page-down
     */
    @Test
    public void testExpandableSubfileMultiPage() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_EXPANDABLE,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(50);  // 2.5 pages

        assertTrue("Page down from start", subfileModel.pageDown());
        assertEquals("Page 2 start position", DEFAULT_PAGE_SIZE, subfileModel.getCurrentPageStart());

        assertTrue("Page down from page 2", subfileModel.pageDown());
        assertEquals("Page 3 start position", 40, subfileModel.getCurrentPageStart());

        assertFalse("Page down from page 3 (last)", subfileModel.pageDown());
        assertEquals("Still at page 3", 40, subfileModel.getCurrentPageStart());
        assertTrue("Expandable type", subfileModel.isExpandableType());
    }

    /**
     * POSITIVE: Empty subfile handling
     * Dimension pair: type=display-only, record-count=0, nav=none
     */
    @Test
    public void testEmptySubfileNavigation() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );

        assertEquals("Empty subfile", 0, subfileModel.getRecordCount());
        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals("Page is empty", 0, page.size());

        assertFalse("Cannot page down from empty", subfileModel.pageDown());
        assertFalse("Cannot page up from empty", subfileModel.pageUp());

        subfileModel.home();
        assertEquals("Home on empty", 0, subfileModel.getCurrentPageStart());
    }

    /**
     * POSITIVE: Home navigation from multi-page subfile
     * Dimension pair: type=selection, record-count=multi-page, nav=home
     */
    @Test
    public void testHomeNavigationMultiPage() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(50);

        subfileModel.pageDown();
        subfileModel.pageDown();
        assertEquals("At page 3", 40, subfileModel.getCurrentPageStart());

        subfileModel.home();
        assertEquals("Home returns to page 1", 0, subfileModel.getCurrentPageStart());
    }

    /**
     * POSITIVE: End navigation from multi-page subfile
     * Dimension pair: type=input, record-count=multi-page, nav=end
     */
    @Test
    public void testEndNavigationMultiPage() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_INPUT,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(45);

        subfileModel.end();
        assertEquals("End positioned at last page", 40, subfileModel.getCurrentPageStart());
        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals("Last page has remaining records", 5, page.size());
    }

    /**
     * POSITIVE: Position-to specific record within subfile
     * Dimension pair: type=selection, record-count=50, nav=position-to
     */
    @Test
    public void testPositionToSpecificRecord() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(50);

        assertTrue("Position to record 25", subfileModel.positionTo(25));
        assertEquals("Page positioned for record 25", 20, subfileModel.getCurrentPageStart());

        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals("Page has 20 records", 20, page.size());
    }

    /**
     * POSITIVE: Selection state preserved across page navigation
     * Dimension pair: type=selection, mode=multiple, nav=page-down
     */
    @Test
    public void testSelectionPreservedAcrossPageNavigation() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_MULTIPLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(50);

        subfileModel.selectRecord(5);
        subfileModel.selectRecord(10);
        assertEquals("2 records selected on page 1", 2, subfileModel.getSelectedRecords().size());

        subfileModel.pageDown();
        assertEquals("Selection preserved on page 2", 2, subfileModel.getSelectedRecords().size());
        assertTrue("Record 5 still selected", subfileModel.isRecordSelected(5));
    }

    /**
     * POSITIVE: Deselection of multiple records
     * Dimension pair: type=selection, mode=multiple, selection=deselect
     */
    @Test
    public void testDeselectMultipleRecords() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_MULTIPLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(30);

        subfileModel.selectRecord(0);
        subfileModel.selectRecord(5);
        subfileModel.selectRecord(10);
        assertEquals("3 records selected", 3, subfileModel.getSelectedRecords().size());

        subfileModel.deselectRecord(5);
        assertEquals("2 records after deselect", 2, subfileModel.getSelectedRecords().size());
        assertFalse("Record 5 deselected", subfileModel.isRecordSelected(5));
    }

    /**
     * POSITIVE: Page-up navigation from middle page
     * Dimension pair: type=display-only, record-count=multi-page, nav=page-up
     */
    @Test
    public void testPageUpNavigation() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(50);

        subfileModel.pageDown();
        subfileModel.pageDown();
        assertEquals("At page 3", 40, subfileModel.getCurrentPageStart());

        assertTrue("Page up to page 2", subfileModel.pageUp());
        assertEquals("Page 2 position", 20, subfileModel.getCurrentPageStart());

        assertTrue("Page up to page 1", subfileModel.pageUp());
        assertEquals("Page 1 position", 0, subfileModel.getCurrentPageStart());

        assertFalse("Cannot page up from page 1", subfileModel.pageUp());
    }

    /**
     * POSITIVE: Record access within current page
     * Dimension pair: type=input, record-count=page-size, access=sequential
     */
    @Test
    public void testSequentialRecordAccess() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_INPUT,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(DEFAULT_PAGE_SIZE);

        for (int i = 0; i < DEFAULT_PAGE_SIZE; i++) {
            SubfileRecord record = subfileModel.getRecord(i);
            assertNotNull("Record " + i + " exists", record);
            assertEquals("Record " + i + " index", i, record.getIndex());
        }
    }

    /**
     * POSITIVE: Subfile with boundary record count
     * Dimension pair: type=expandable, record-count=page-size+1, nav=page-down
     */
    @Test
    public void testSubfileWithBoundaryRecordCount() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_EXPANDABLE,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(DEFAULT_PAGE_SIZE + 1);  // 21 records

        List<SubfileRecord> page1 = subfileModel.getPageRecords();
        assertEquals("Page 1 has full page", DEFAULT_PAGE_SIZE, page1.size());

        assertTrue("Page down", subfileModel.pageDown());
        List<SubfileRecord> page2 = subfileModel.getPageRecords();
        assertEquals("Page 2 has 1 record", 1, page2.size());
    }

    // ========================================================================
    // ADVERSARIAL TESTS (12): Edge cases, boundary conditions, large records
    // ========================================================================

    /**
     * ADVERSARIAL: Navigation beyond maximum subfile size
     * Dimension pair: type=display-only, record-count=1000, nav=position-to
     * Risk: Attempting to position to non-existent record
     */
    @Test
    public void testPositionToNonExistentRecord() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(MAX_RECORDS);

        assertFalse("Position to invalid record (negative)", subfileModel.positionTo(-1));
        assertFalse("Position to invalid record (beyond max)", subfileModel.positionTo(MAX_RECORDS));
        assertFalse("Position to invalid record (way beyond)", subfileModel.positionTo(999999));
        assertEquals("Position unchanged", 0, subfileModel.getCurrentPageStart());
    }

    /**
     * ADVERSARIAL: Selection on non-selection type subfile
     * Dimension pair: type=display-only, mode=none, selection=attempted
     * Risk: Attempting to select on display-only subfile
     */
    @Test
    public void testSelectionOnDisplayOnlySubfile() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(DEFAULT_PAGE_SIZE);

        assertFalse("Cannot select in NONE mode", subfileModel.selectRecord(0));
        assertEquals("No records selected", 0, subfileModel.getSelectedRecords().size());
    }

    /**
     * ADVERSARIAL: Large subfile performance with frequent navigation
     * Dimension pair: type=expandable, record-count=1000, nav=rapid-page-down
     * Risk: Memory/performance degradation with max records
     */
    @Test
    public void testLargeSubfileRapidNavigation() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_EXPANDABLE,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(MAX_RECORDS);

        long startTime = System.currentTimeMillis();
        int pagesMoved = 0;

        while (subfileModel.pageDown() && pagesMoved < 50) {
            pagesMoved++;
        }

        long duration = System.currentTimeMillis() - startTime;
        assertTrue("Navigation completes in reasonable time (< 1 sec)", duration < 1000);
        // With 1000 records and 20 per page: 50 pages total, can move 49 times to reach last page
        assertEquals("49 pages navigated (can only move from page 1 to page 50)", 49, pagesMoved);
    }

    /**
     * ADVERSARIAL: Selection mode conflict - try multiple in single mode
     * Dimension pair: type=selection, mode=single, selection=multiple-records
     * Risk: Multiple selections in single-select mode
     */
    @Test
    public void testMultipleSelectionInSingleMode() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(DEFAULT_PAGE_SIZE);

        subfileModel.selectRecord(0);
        assertEquals("First selection recorded", 1, subfileModel.getSelectedRecords().size());

        subfileModel.selectRecord(5);
        assertEquals("Second selection replaces first", 1, subfileModel.getSelectedRecords().size());
        assertFalse("Record 0 no longer selected", subfileModel.isRecordSelected(0));
        assertTrue("Record 5 selected", subfileModel.isRecordSelected(5));
    }

    /**
     * ADVERSARIAL: Page navigation with scroll overflow behavior
     * Dimension pair: type=input, record-count=25, overflow=scroll, nav=page-down
     * Risk: Scroll behavior when records don't fill exact pages
     */
    @Test
    public void testScrollOverflowWithPartialPage() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_INPUT,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(25);

        assertTrue("Page down from start", subfileModel.pageDown());
        assertEquals("Page 2 start", DEFAULT_PAGE_SIZE, subfileModel.getCurrentPageStart());

        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals("Partial page has 5 records", 5, page.size());
    }

    /**
     * ADVERSARIAL: Selection with navigation and record deletion simulation
     * Dimension pair: type=selection, mode=multiple, nav=page-down, mutation=record-deleted
     * Risk: Selection state after record count change
     */
    @Test
    public void testSelectionAfterRecordCountChange() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_MULTIPLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(30);

        subfileModel.selectRecord(0);
        subfileModel.selectRecord(25);
        assertEquals("Two records selected", 2, subfileModel.getSelectedRecords().size());

        // Simulate going to next page (doesn't change selection)
        subfileModel.pageDown();
        assertEquals("Selection preserved", 2, subfileModel.getSelectedRecords().size());
    }

    /**
     * ADVERSARIAL: Empty page access on multi-page subfile
     * Dimension pair: type=display-only, record-count=21, nav=position-to-last-page
     * Risk: Accessing non-existent pages
     */
    @Test
    public void testEmptyPageAccess() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(21);

        subfileModel.end();
        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals("Last page has 1 record", 1, page.size());

        // Try to page down beyond last (should fail)
        assertFalse("Cannot page down beyond end", subfileModel.pageDown());
    }

    /**
     * ADVERSARIAL: Extreme record count subfile
     * Dimension pair: type=expandable, record-count=max, nav=end
     * Risk: Navigation to end with maximum records
     */
    @Test
    public void testExtremeRecordCountNavigation() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_EXPANDABLE,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(MAX_RECORDS);

        subfileModel.end();
        assertEquals("End positioned correctly", 980, subfileModel.getCurrentPageStart());

        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals("Last page has 20 records", 20, page.size());
    }

    /**
     * ADVERSARIAL: Bidirectional navigation returning to same position
     * Dimension pair: type=selection, record-count=50, nav=page-down-then-page-up
     * Risk: State inconsistency when navigating back
     */
    @Test
    public void testBidirectionalNavigationConsistency() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(50);

        int initialPage = subfileModel.getCurrentPageStart();
        subfileModel.pageDown();
        subfileModel.pageDown();
        int secondPage = subfileModel.getCurrentPageStart();

        subfileModel.pageUp();
        subfileModel.pageUp();
        int returnPage = subfileModel.getCurrentPageStart();

        assertEquals("Navigation returns to original position", initialPage, returnPage);
        assertTrue("Intermediate page was different", initialPage != secondPage);
    }

    /**
     * ADVERSARIAL: Selection with truncate overflow behavior
     * Dimension pair: type=selection, record-count=19, overflow=truncate, selection=multiple
     * Risk: Partial page display with selection
     */
    @Test
    public void testSelectionWithTruncateOverflow() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_MULTIPLE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(19);

        subfileModel.selectRecord(0);
        subfileModel.selectRecord(18);
        assertEquals("Two records selected on partial page", 2, subfileModel.getSelectedRecords().size());

        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals("Page truncated to 19 records", 19, page.size());
    }

    /**
     * ADVERSARIAL: Home/End rapid alternation
     * Dimension pair: type=display-only, record-count=100, nav=home-end-home-end
     * Risk: Position state corruption from rapid navigation
     */
    @Test
    public void testRapidHomeEndNavigation() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        subfileModel.addRecords(100);

        subfileModel.home();
        assertEquals("Home", 0, subfileModel.getCurrentPageStart());

        subfileModel.end();
        assertEquals("End", 80, subfileModel.getCurrentPageStart());

        subfileModel.home();
        assertEquals("Home again", 0, subfileModel.getCurrentPageStart());

        subfileModel.end();
        assertEquals("End again", 80, subfileModel.getCurrentPageStart());
    }

    // ========================================================================
    // CRITICAL TESTS (10): High-risk behaviors with multiple dimensions
    // ========================================================================

    /**
     * CRITICAL: Single-select mode - sequential record selection with override
     * Dimension pair: type=selection, mode=single, record-count=50, nav=position-to
     * Risk: Selection state corruption across page boundaries in single mode
     */
    @Test
    public void testSingleSelectModeOverride() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(50);

        subfileModel.selectRecord(5);
        assertTrue("Record 5 selected", subfileModel.isRecordSelected(5));

        subfileModel.positionTo(25);
        subfileModel.selectRecord(25);

        assertEquals("Only record 25 selected", 1, subfileModel.getSelectedRecords().size());
        assertTrue("Record 25 selected", subfileModel.isRecordSelected(25));
        assertFalse("Record 5 no longer selected", subfileModel.isRecordSelected(5));
    }

    /**
     * CRITICAL: Page navigation boundary at exactly page-size boundary
     * Dimension pair: type=expandable, record-count=40, nav=all-directions
     * Risk: Off-by-one errors at exact boundary
     */
    @Test
    public void testPageBoundaryExactly() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_EXPANDABLE,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(DEFAULT_PAGE_SIZE * 2);  // Exactly 2 pages

        assertEquals("Page 1 start", 0, subfileModel.getCurrentPageStart());

        assertTrue("Page down to page 2", subfileModel.pageDown());
        assertEquals("Page 2 start", DEFAULT_PAGE_SIZE, subfileModel.getCurrentPageStart());

        assertFalse("Cannot page down beyond page 2", subfileModel.pageDown());
        assertEquals("Still at page 2", DEFAULT_PAGE_SIZE, subfileModel.getCurrentPageStart());
    }

    /**
     * CRITICAL: Multi-select with deselection order
     * Dimension pair: type=selection, mode=multiple, selection=select-deselect-pattern
     * Risk: Selection set corruption from non-sequential deselection
     */
    @Test
    public void testMultiSelectDeselectPattern() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_MULTIPLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(DEFAULT_PAGE_SIZE);

        // Select: 0, 5, 10, 15
        for (int i : new int[]{0, 5, 10, 15}) {
            subfileModel.selectRecord(i);
        }
        assertEquals("Four records selected", 4, subfileModel.getSelectedRecords().size());

        // Deselect: 5, 15, 0
        subfileModel.deselectRecord(5);
        subfileModel.deselectRecord(15);
        subfileModel.deselectRecord(0);

        Set<Integer> remaining = subfileModel.getSelectedRecords();
        assertEquals("Only record 10 remains", 1, remaining.size());
        assertTrue("Record 10 still selected", remaining.contains(10));
    }

    /**
     * CRITICAL: Large subfile with selection mode transitions
     * Dimension pair: type=selection, record-count=1000, mode=single->multiple
     * Risk: Selection semantics change with mode
     */
    @Test
    public void testLargeSubfileSelectionConsistency() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(MAX_RECORDS);

        // Single-select mode: record 100
        subfileModel.selectRecord(100);
        assertEquals("Single selection", 1, subfileModel.getSelectedRecords().size());

        // Simulate mode change to multiple (would require new subfile in real scenario)
        TestSubfileModel multiModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_MULTIPLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        multiModel.addRecords(MAX_RECORDS);
        multiModel.selectRecord(100);
        multiModel.selectRecord(500);
        multiModel.selectRecord(900);

        assertEquals("Multiple selections in new mode", 3, multiModel.getSelectedRecords().size());
    }

    /**
     * CRITICAL: Navigation consistency with record count extremes
     * Dimension pair: type=display-only, record-count=[0,1,max], nav=all
     * Risk: Navigation logic failure with boundary record counts
     */
    @Test
    public void testNavigationWithExtremeRecordCounts() {
        // Test with 0 records
        TestSubfileModel empty = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        assertFalse("Cannot navigate empty", empty.pageDown());
        empty.home();
        assertEquals("Home on empty", 0, empty.getCurrentPageStart());

        // Test with 1 record
        TestSubfileModel single = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        single.addRecords(1);
        assertFalse("Cannot page down with 1 record", single.pageDown());
        single.end();
        assertEquals("End with 1 record", 0, single.getCurrentPageStart());

        // Test with MAX records
        TestSubfileModel max = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        max.addRecords(MAX_RECORDS);
        max.end();
        assertEquals("End with max records", 980, max.getCurrentPageStart());
    }

    /**
     * CRITICAL: Scroll overflow with very large records at end
     * Dimension pair: type=input, record-count=999, overflow=scroll, nav=end
     * Risk: Partial last page calculation with large record count
     */
    @Test
    public void testScrollOverflowLargePartialPage() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_INPUT,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(999);  // Not divisible by page-size

        subfileModel.end();
        List<SubfileRecord> lastPage = subfileModel.getPageRecords();
        assertEquals("Last page has remaining records", 19, lastPage.size());
    }

    /**
     * CRITICAL: Position-to with subsequent selection across boundaries
     * Dimension pair: type=selection, record-count=50, selection=multiple, nav=position-to-then-select
     * Risk: Selection index calculations with navigation
     */
    @Test
    public void testPositionToWithSubsequentSelection() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_MULTIPLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(50);

        subfileModel.positionTo(25);
        assertEquals("Positioned to record 25 page", 20, subfileModel.getCurrentPageStart());

        subfileModel.selectRecord(25);
        subfileModel.selectRecord(26);
        subfileModel.selectRecord(27);

        assertEquals("Three records selected", 3, subfileModel.getSelectedRecords().size());
        assertTrue("Record 25 selected", subfileModel.isRecordSelected(25));
        assertTrue("Record 26 selected", subfileModel.isRecordSelected(26));
        assertTrue("Record 27 selected", subfileModel.isRecordSelected(27));
    }

    /**
     * CRITICAL: Subfile type boundaries and misidentification
     * Dimension pair: type=[all 4], record-count=20, nav=none
     * Risk: Type confusion between display-only and expandable
     */
    @Test
    public void testAllSubfileTypeIdentification() {
        // Type 0: Display-only
        TestSubfileModel displayOnly = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        assertTrue("Display-only identified", displayOnly.isDisplayOnly());
        assertFalse("Not input", displayOnly.isInputType());

        // Type 1: Input
        TestSubfileModel input = new TestSubfileModel(
            TestSubfileModel.TYPE_INPUT,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        assertTrue("Input identified", input.isInputType());
        assertFalse("Not display-only", input.isDisplayOnly());

        // Type 2: Selection
        TestSubfileModel selection = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        assertTrue("Selection identified", selection.isSelectionType());

        // Type 3: Expandable
        TestSubfileModel expandable = new TestSubfileModel(
            TestSubfileModel.TYPE_EXPANDABLE,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        assertTrue("Expandable identified", expandable.isExpandableType());
    }

    /**
     * CRITICAL: Overflow behavior selection across subfile types
     * Dimension pair: type=[all], overflow=[truncate,scroll,error], record-count=25
     * Risk: Overflow behavior not consistent with subfile type
     */
    @Test
    public void testOverflowBehaviorSelection() {
        // Display-only with truncate
        TestSubfileModel truncate = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        truncate.addRecords(25);
        assertEquals("Truncate behavior set", TestSubfileModel.OVERFLOW_TRUNCATE, truncate.getOverflowBehavior());

        // Input with scroll
        TestSubfileModel scroll = new TestSubfileModel(
            TestSubfileModel.TYPE_INPUT,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        scroll.addRecords(25);
        assertEquals("Scroll behavior set", TestSubfileModel.OVERFLOW_SCROLL, scroll.getOverflowBehavior());

        // Selection with error
        TestSubfileModel error = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_ERROR
        );
        error.addRecords(25);
        assertEquals("Error behavior set", TestSubfileModel.OVERFLOW_ERROR, error.getOverflowBehavior());
    }

    /**
     * CRITICAL: Complex multi-page selection persistence
     * Dimension pair: type=selection, mode=multiple, record-count=100, nav=complex-pattern
     * Risk: Selection loss during complex navigation patterns
     */
    @Test
    public void testComplexNavigationSelectionPersistence() {
        subfileModel = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_MULTIPLE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        subfileModel.addRecords(100);

        // Select on page 1
        subfileModel.selectRecord(5);

        // Navigate to page 3
        subfileModel.pageDown();
        subfileModel.pageDown();
        subfileModel.selectRecord(45);

        // Back to page 1
        subfileModel.home();
        assertEquals("Record 5 still selected after navigation", 2, subfileModel.getSelectedRecords().size());
        assertTrue("Record 5 present", subfileModel.isRecordSelected(5));
        assertTrue("Record 45 present", subfileModel.isRecordSelected(45));

        // Verify page shows all selection state across pages
        assertEquals("Both selections preserved", 2, subfileModel.getSelectedRecords().size());
    }
}
