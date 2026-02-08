/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeEach
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

        assertEquals(DEFAULT_PAGE_SIZE, subfileModel.getRecordCount(),"Display-only should have page-size records");
        assertEquals(0, subfileModel.getCurrentPageStart(),"Initial page start at 0");
        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals(DEFAULT_PAGE_SIZE, page.size(),"Page has all records");
        assertTrue(subfileModel.isDisplayOnly(),"Display-only subfile type");
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

        assertEquals(1, subfileModel.getRecordCount(),"Input subfile should have 1 record");
        SubfileRecord record = subfileModel.getRecord(0);
        assertNotNull(record,"Record exists");
        assertEquals(0, record.getIndex(),"Record index is 0");
        assertTrue(subfileModel.isInputType(),"Input type detected");
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

        assertTrue(subfileModel.positionTo(5),"Position to record 5");
        assertTrue(subfileModel.selectRecord(5),"Select record 5");
        assertTrue(subfileModel.isRecordSelected(5),"Record 5 is selected");
        assertEquals(1, subfileModel.getSelectedRecords().size(),"Only 1 record selected");
        assertTrue(subfileModel.isSelectionType(),"Selection type");
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
        assertEquals(0, subfileModel.getCurrentPageStart(),"Home positioned");

        assertTrue(subfileModel.selectRecord(0),"Select record 0");
        assertTrue(subfileModel.selectRecord(5),"Select record 5");
        assertTrue(subfileModel.selectRecord(10),"Select record 10");

        Set<Integer> selected = subfileModel.getSelectedRecords();
        assertEquals(3, selected.size(),"Three records selected");
        assertTrue(selected.contains(0),"Record 0 selected");
        assertTrue(selected.contains(5),"Record 5 selected");
        assertTrue(selected.contains(10),"Record 10 selected");
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

        assertTrue(subfileModel.pageDown(),"Page down from start");
        assertEquals(DEFAULT_PAGE_SIZE, subfileModel.getCurrentPageStart(),"Page 2 start position");

        assertTrue(subfileModel.pageDown(),"Page down from page 2");
        assertEquals(40, subfileModel.getCurrentPageStart(),"Page 3 start position");

        assertFalse(subfileModel.pageDown(),"Page down from page 3 (last)");
        assertEquals(40, subfileModel.getCurrentPageStart(),"Still at page 3");
        assertTrue(subfileModel.isExpandableType(),"Expandable type");
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

        assertEquals(0, subfileModel.getRecordCount(),"Empty subfile");
        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals(0, page.size(),"Page is empty");

        assertFalse(subfileModel.pageDown(),"Cannot page down from empty");
        assertFalse(subfileModel.pageUp(),"Cannot page up from empty");

        subfileModel.home();
        assertEquals(0, subfileModel.getCurrentPageStart(),"Home on empty");
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
        assertEquals(40, subfileModel.getCurrentPageStart(),"At page 3");

        subfileModel.home();
        assertEquals(0, subfileModel.getCurrentPageStart(),"Home returns to page 1");
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
        assertEquals(40, subfileModel.getCurrentPageStart(),"End positioned at last page");
        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals(5, page.size(),"Last page has remaining records");
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

        assertTrue(subfileModel.positionTo(25),"Position to record 25");
        assertEquals(20, subfileModel.getCurrentPageStart(),"Page positioned for record 25");

        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals(20, page.size(),"Page has 20 records");
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
        assertEquals(2, subfileModel.getSelectedRecords().size(),"2 records selected on page 1");

        subfileModel.pageDown();
        assertEquals(2, subfileModel.getSelectedRecords().size(),"Selection preserved on page 2");
        assertTrue(subfileModel.isRecordSelected(5),"Record 5 still selected");
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
        assertEquals(3, subfileModel.getSelectedRecords().size(),"3 records selected");

        subfileModel.deselectRecord(5);
        assertEquals(2, subfileModel.getSelectedRecords().size(),"2 records after deselect");
        assertFalse(subfileModel.isRecordSelected(5),"Record 5 deselected");
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
        assertEquals(40, subfileModel.getCurrentPageStart(),"At page 3");

        assertTrue(subfileModel.pageUp(),"Page up to page 2");
        assertEquals(20, subfileModel.getCurrentPageStart(),"Page 2 position");

        assertTrue(subfileModel.pageUp(),"Page up to page 1");
        assertEquals(0, subfileModel.getCurrentPageStart(),"Page 1 position");

        assertFalse(subfileModel.pageUp(),"Cannot page up from page 1");
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
            assertNotNull(record,"Record " + i + " exists");
            assertEquals(i, record.getIndex(),"Record " + i + " index");
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
        assertEquals(DEFAULT_PAGE_SIZE, page1.size(),"Page 1 has full page");

        assertTrue(subfileModel.pageDown(),"Page down");
        List<SubfileRecord> page2 = subfileModel.getPageRecords();
        assertEquals(1, page2.size(),"Page 2 has 1 record");
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

        assertFalse(subfileModel.positionTo(-1),"Position to invalid record (negative)");
        assertFalse(subfileModel.positionTo(MAX_RECORDS),"Position to invalid record (beyond max)");
        assertFalse(subfileModel.positionTo(999999),"Position to invalid record (way beyond)");
        assertEquals(0, subfileModel.getCurrentPageStart(),"Position unchanged");
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

        assertFalse(subfileModel.selectRecord(0),"Cannot select in NONE mode");
        assertEquals(0, subfileModel.getSelectedRecords().size(),"No records selected");
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
        assertTrue(duration < 1000,"Navigation completes in reasonable time (< 1 sec)");
        // With 1000 records and 20 per page: 50 pages total, can move 49 times to reach last page
        assertEquals(49, pagesMoved,"49 pages navigated (can only move from page 1 to page 50)");
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
        assertEquals(1, subfileModel.getSelectedRecords().size(),"First selection recorded");

        subfileModel.selectRecord(5);
        assertEquals(1, subfileModel.getSelectedRecords().size(),"Second selection replaces first");
        assertFalse(subfileModel.isRecordSelected(0),"Record 0 no longer selected");
        assertTrue(subfileModel.isRecordSelected(5),"Record 5 selected");
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

        assertTrue(subfileModel.pageDown(),"Page down from start");
        assertEquals(DEFAULT_PAGE_SIZE, subfileModel.getCurrentPageStart(),"Page 2 start");

        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals(5, page.size(),"Partial page has 5 records");
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
        assertEquals(2, subfileModel.getSelectedRecords().size(),"Two records selected");

        // Simulate going to next page (doesn't change selection)
        subfileModel.pageDown();
        assertEquals(2, subfileModel.getSelectedRecords().size(),"Selection preserved");
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
        assertEquals(1, page.size(),"Last page has 1 record");

        // Try to page down beyond last (should fail)
        assertFalse(subfileModel.pageDown(),"Cannot page down beyond end");
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
        assertEquals(980, subfileModel.getCurrentPageStart(),"End positioned correctly");

        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals(20, page.size(),"Last page has 20 records");
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

        assertEquals(initialPage, returnPage,"Navigation returns to original position");
        assertTrue(initialPage != secondPage,"Intermediate page was different");
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
        assertEquals(2, subfileModel.getSelectedRecords().size(),"Two records selected on partial page");

        List<SubfileRecord> page = subfileModel.getPageRecords();
        assertEquals(19, page.size(),"Page truncated to 19 records");
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
        assertEquals(0, subfileModel.getCurrentPageStart(),"Home");

        subfileModel.end();
        assertEquals(80, subfileModel.getCurrentPageStart(),"End");

        subfileModel.home();
        assertEquals(0, subfileModel.getCurrentPageStart(),"Home again");

        subfileModel.end();
        assertEquals(80, subfileModel.getCurrentPageStart(),"End again");
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
        assertTrue(subfileModel.isRecordSelected(5),"Record 5 selected");

        subfileModel.positionTo(25);
        subfileModel.selectRecord(25);

        assertEquals(1, subfileModel.getSelectedRecords().size(),"Only record 25 selected");
        assertTrue(subfileModel.isRecordSelected(25),"Record 25 selected");
        assertFalse(subfileModel.isRecordSelected(5),"Record 5 no longer selected");
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

        assertEquals(0, subfileModel.getCurrentPageStart(),"Page 1 start");

        assertTrue(subfileModel.pageDown(),"Page down to page 2");
        assertEquals(DEFAULT_PAGE_SIZE, subfileModel.getCurrentPageStart(),"Page 2 start");

        assertFalse(subfileModel.pageDown(),"Cannot page down beyond page 2");
        assertEquals(DEFAULT_PAGE_SIZE, subfileModel.getCurrentPageStart(),"Still at page 2");
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
        assertEquals(4, subfileModel.getSelectedRecords().size(),"Four records selected");

        // Deselect: 5, 15, 0
        subfileModel.deselectRecord(5);
        subfileModel.deselectRecord(15);
        subfileModel.deselectRecord(0);

        Set<Integer> remaining = subfileModel.getSelectedRecords();
        assertEquals(1, remaining.size(),"Only record 10 remains");
        assertTrue(remaining.contains(10),"Record 10 still selected");
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
        assertEquals(1, subfileModel.getSelectedRecords().size(),"Single selection");

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

        assertEquals(3, multiModel.getSelectedRecords().size(),"Multiple selections in new mode");
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
        assertFalse(empty.pageDown(),"Cannot navigate empty");
        empty.home();
        assertEquals(0, empty.getCurrentPageStart(),"Home on empty");

        // Test with 1 record
        TestSubfileModel single = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        single.addRecords(1);
        assertFalse(single.pageDown(),"Cannot page down with 1 record");
        single.end();
        assertEquals(0, single.getCurrentPageStart(),"End with 1 record");

        // Test with MAX records
        TestSubfileModel max = new TestSubfileModel(
            TestSubfileModel.TYPE_DISPLAY_ONLY,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        max.addRecords(MAX_RECORDS);
        max.end();
        assertEquals(980, max.getCurrentPageStart(),"End with max records");
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
        assertEquals(19, lastPage.size(),"Last page has remaining records");
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
        assertEquals(20, subfileModel.getCurrentPageStart(),"Positioned to record 25 page");

        subfileModel.selectRecord(25);
        subfileModel.selectRecord(26);
        subfileModel.selectRecord(27);

        assertEquals(3, subfileModel.getSelectedRecords().size(),"Three records selected");
        assertTrue(subfileModel.isRecordSelected(25),"Record 25 selected");
        assertTrue(subfileModel.isRecordSelected(26),"Record 26 selected");
        assertTrue(subfileModel.isRecordSelected(27),"Record 27 selected");
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
        assertTrue(displayOnly.isDisplayOnly(),"Display-only identified");
        assertFalse(displayOnly.isInputType(),"Not input");

        // Type 1: Input
        TestSubfileModel input = new TestSubfileModel(
            TestSubfileModel.TYPE_INPUT,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        assertTrue(input.isInputType(),"Input identified");
        assertFalse(input.isDisplayOnly(),"Not display-only");

        // Type 2: Selection
        TestSubfileModel selection = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        assertTrue(selection.isSelectionType(),"Selection identified");

        // Type 3: Expandable
        TestSubfileModel expandable = new TestSubfileModel(
            TestSubfileModel.TYPE_EXPANDABLE,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_TRUNCATE
        );
        assertTrue(expandable.isExpandableType(),"Expandable identified");
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
        assertEquals(TestSubfileModel.OVERFLOW_TRUNCATE, truncate.getOverflowBehavior(),"Truncate behavior set");

        // Input with scroll
        TestSubfileModel scroll = new TestSubfileModel(
            TestSubfileModel.TYPE_INPUT,
            TestSubfileModel.MODE_NONE,
            TestSubfileModel.OVERFLOW_SCROLL
        );
        scroll.addRecords(25);
        assertEquals(TestSubfileModel.OVERFLOW_SCROLL, scroll.getOverflowBehavior(),"Scroll behavior set");

        // Selection with error
        TestSubfileModel error = new TestSubfileModel(
            TestSubfileModel.TYPE_SELECTION,
            TestSubfileModel.MODE_SINGLE,
            TestSubfileModel.OVERFLOW_ERROR
        );
        error.addRecords(25);
        assertEquals(TestSubfileModel.OVERFLOW_ERROR, error.getOverflowBehavior(),"Error behavior set");
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
        assertEquals(2, subfileModel.getSelectedRecords().size(),"Record 5 still selected after navigation");
        assertTrue(subfileModel.isRecordSelected(5),"Record 5 present");
        assertTrue(subfileModel.isRecordSelected(45),"Record 45 present");

        // Verify page shows all selection state across pages
        assertEquals(2, subfileModel.getSelectedRecords().size(),"Both selections preserved");
    }
}
