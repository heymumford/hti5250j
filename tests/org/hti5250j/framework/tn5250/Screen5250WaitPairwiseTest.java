/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * Pairwise combinatorial test suite for Screen5250 wait/synchronization methods.
 *
 * TEST DIMENSIONS (pairwise combinations):
 *   - Wait conditions: [text-appears, text-disappears, cursor-at, field-unlocked, OIA-clear]
 *   - Timeout values: [0, 100ms, 1000ms, 30000ms, -1 (infinite)]
 *   - Match patterns: [exact, contains, regex, case-insensitive]
 *   - Screen changes: [no-change, immediate, delayed, never]
 *   - Concurrent updates: [none, single-threaded, rapid multi-threaded]
 *
 * COVERAGE MATRIX (20 tests):
 *   - 10 POSITIVE: Successful waits with expected outcomes
 *   - 10 ADVERSARIAL: Timeouts, mismatches, concurrent changes that fail
 *
 * METHODS TESTED (to be implemented in Screen5250):
 *   - waitForText(String text, int timeout)
 *   - waitForTextDisappear(String text, int timeout)
 *   - waitForCursor(int row, int col, int timeout)
 *   - waitForUnlock(int timeout)
 *   - isKeyboardLocked()
 *   - getOIA() status methods
 *   - waitForOIAClear(int timeout)
 *
 * CRITICAL FOR AUTOMATION: These methods enable reliable headless testing without
 * polling loops. Proper synchronization prevents race conditions and flaky tests.
 */
public class Screen5250WaitPairwiseTest {

    private Screen5250WaitTestDouble screen;
    private ExecutorService executor;
    private static final int TIMEOUT_SECONDS = 5;

    /**
     * Test double for Screen5250 with mockable wait behavior.
     * Provides internal state control for synchronization testing.
     */
    private static class Screen5250WaitTestDouble extends Screen5250 {
        private char[] screenBuffer;
        private boolean keyboardLocked = false;
        private int cursorRow = 0;
        private int cursorCol = 0;
        private boolean oiaInputInhibited = false;
        private final Object screenLock = new Object();

        private int numRows = 24;
        private int numCols = 80;

        public Screen5250WaitTestDouble() {
            super();
            this.screenBuffer = new char[numRows * numCols];
            // Initialize with spaces
            for (int i = 0; i < screenBuffer.length; i++) {
                screenBuffer[i] = ' ';
            }
        }

        @Override
        public int getRows() {
            return numRows;
        }

        @Override
        public int getColumns() {
            return numCols;
        }

        @Override
        public int getPos(int row, int col) {
            return (row * numCols) + col;
        }

        @Override
        public int getScreenLength() {
            return screenBuffer.length;
        }

        @Override
        public int getCurrentRow() {
            return cursorRow;
        }

        @Override
        public int getCurrentCol() {
            return cursorCol;
        }

        @Override
        public int getCurrentPos() {
            return getPos(cursorRow, cursorCol);
        }

        @Override
        public boolean isInField(int pos, boolean checkAttr) {
            return false;
        }

        @Override
        public StringBuffer getHSMore() {
            return new StringBuffer("More...");
        }

        @Override
        public StringBuffer getHSBottom() {
            return new StringBuffer("Bottom");
        }

        @Override
        public void setDirty(int pos) {
            // No-op
        }

        // === WAIT/SYNC METHODS TO BE TESTED (placeholders) ===

        /**
         * Wait for text to appear on screen.
         * @param text Text to search for
         * @param timeoutMs Timeout in milliseconds (negative values treated as 0)
         * @return true if text appears before timeout, false if timeout
         */
        public boolean waitForText(String text, int timeoutMs) {
            // Treat negative timeout as 0 (immediate check only)
            if (timeoutMs < 0) {
                timeoutMs = 0;
            }
            long deadline = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < deadline) {
                String screenText = getScreenAsString();
                if (screenText.contains(text)) {
                    return true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        /**
         * Wait for text to disappear from screen.
         * @param text Text to search for
         * @param timeoutMs Timeout in milliseconds
         * @return true if text disappears before timeout
         */
        public boolean waitForTextDisappear(String text, int timeoutMs) {
            long deadline = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < deadline) {
                String screenText = getScreenAsString();
                if (!screenText.contains(text)) {
                    return true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        /**
         * Wait for cursor to reach specific position.
         * @param row Target row
         * @param col Target column
         * @param timeoutMs Timeout in milliseconds
         * @return true if cursor reaches position before timeout
         */
        public boolean waitForCursor(int row, int col, int timeoutMs) {
            long deadline = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < deadline) {
                if (cursorRow == row && cursorCol == col) {
                    return true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        /**
         * Wait for keyboard to unlock.
         * @param timeoutMs Timeout in milliseconds
         * @return true if keyboard unlocks before timeout
         */
        public boolean waitForUnlock(int timeoutMs) {
            long deadline = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < deadline) {
                if (!keyboardLocked) {
                    return true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        /**
         * Check if keyboard is currently locked.
         * @return true if keyboard is locked
         */
        public boolean isKeyboardLocked() {
            return keyboardLocked;
        }

        /**
         * Wait for OIA input inhibit to clear.
         * @param timeoutMs Timeout in milliseconds
         * @return true if OIA clears before timeout
         */
        public boolean waitForOIAClear(int timeoutMs) {
            long deadline = System.currentTimeMillis() + timeoutMs;

            while (System.currentTimeMillis() < deadline) {
                if (!oiaInputInhibited) {
                    return true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        }

        // === HELPER METHODS FOR TESTING ===

        public String getScreenAsString() {
            synchronized (screenLock) {
                return new String(screenBuffer);
            }
        }

        public void setScreenText(String text, int startPos) {
            synchronized (screenLock) {
                char[] chars = text.toCharArray();
                for (int i = 0; i < chars.length && (startPos + i) < screenBuffer.length; i++) {
                    screenBuffer[startPos + i] = chars[i];
                }
            }
        }

        public void clearScreen() {
            synchronized (screenLock) {
                for (int i = 0; i < screenBuffer.length; i++) {
                    screenBuffer[i] = ' ';
                }
            }
        }

        public void setCursorPosition(int row, int col) {
            this.cursorRow = row;
            this.cursorCol = col;
        }

        public void setKeyboardLocked(boolean locked) {
            this.keyboardLocked = locked;
        }

        public void setOIAInputInhibited(boolean inhibited) {
            this.oiaInputInhibited = inhibited;
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        screen = new Screen5250WaitTestDouble();
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void tearDown() throws Exception {
        executor.shutdownNow();
        if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
            fail("Executor did not terminate in time");
        }
    }

    // ============================================================================
    // POSITIVE TESTS: Successful wait operations (10 tests)
    // ============================================================================

    /**
     * POSITIVE TEST 1: waitForText with exact match, immediate success
     *
     * Scenario: Text appears immediately on screen
     * Timeout: 1000ms
     * Pattern: Exact match
     * Screen change: Immediate
     * Concurrency: None
     *
     * Expected: waitForText returns true
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForText_ExactMatchImmediate_ReturnsTrue() {
        // Arrange
        screen.setScreenText("WELCOME", 0);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForText("WELCOME", 1000);

        // Assert
        assertTrue(result,"Should find text immediately");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed < 500,"Should not wait full timeout");
    }

    /**
     * POSITIVE TEST 2: waitForText with contains match, delayed by single update
     *
     * Scenario: Text appears after short delay (50ms)
     * Timeout: 1000ms
     * Pattern: Substring match
     * Screen change: Delayed
     * Concurrency: Single-threaded delayed write
     *
     * Expected: waitForText returns true after text appears
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForText_ContainsMatchDelayed_ReturnsTrue() throws InterruptedException {
        // Arrange
        final String fullText = "System ready for input";
        executor.submit(() -> {
            try {
                Thread.sleep(50);
                screen.setScreenText(fullText, 100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Act
        boolean result = screen.waitForText("ready", 2000);

        // Assert
        assertTrue(result,"Should find substring after delay");
    }

    /**
     * POSITIVE TEST 3: waitForCursor at specific position, immediate
     *
     * Scenario: Cursor already at target position
     * Timeout: 100ms
     * Screen change: No change
     * Concurrency: None
     *
     * Expected: waitForCursor returns true immediately
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForCursor_PositionMatched_ReturnsTrue() {
        // Arrange
        screen.setCursorPosition(5, 10);

        // Act
        boolean result = screen.waitForCursor(5, 10, 100);

        // Assert
        assertTrue(result,"Should find cursor at position");
    }

    /**
     * POSITIVE TEST 4: waitForUnlock after keyboard becomes unlocked
     *
     * Scenario: Keyboard locked initially, then unlocked after 50ms
     * Timeout: 1000ms
     * Concurrency: Single threaded delayed state change
     *
     * Expected: waitForUnlock returns true when unlock occurs
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForUnlock_KeyboardUnlocksDelayed_ReturnsTrue() throws InterruptedException {
        // Arrange
        screen.setKeyboardLocked(true);
        executor.submit(() -> {
            try {
                Thread.sleep(50);
                screen.setKeyboardLocked(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Act
        boolean result = screen.waitForUnlock(1000);

        // Assert
        assertTrue(result,"Should return true when keyboard unlocks");
    }

    /**
     * POSITIVE TEST 5: isKeyboardLocked returns correct state
     *
     * Scenario: Keyboard locked state query
     * Concurrency: None
     *
     * Expected: isKeyboardLocked returns accurate boolean state
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testIsKeyboardLocked_LockedState_ReturnsTrue() {
        // Arrange
        screen.setKeyboardLocked(true);

        // Act
        boolean result = screen.isKeyboardLocked();

        // Assert
        assertTrue(result,"Should report keyboard locked");
    }

    /**
     * POSITIVE TEST 6: waitForOIAClear with immediate success
     *
     * Scenario: OIA input inhibit not active
     * Timeout: 100ms
     * Screen change: No change
     *
     * Expected: waitForOIAClear returns true immediately
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForOIAClear_NotInhibited_ReturnsTrue() {
        // Arrange
        screen.setOIAInputInhibited(false);

        // Act
        boolean result = screen.waitForOIAClear(100);

        // Assert
        assertTrue(result,"Should succeed when OIA already clear");
    }

    /**
     * POSITIVE TEST 7: waitForTextDisappear with initial presence, then removal
     *
     * Scenario: Text initially on screen, removed after 40ms
     * Timeout: 1000ms
     * Concurrency: Single-threaded delayed removal
     *
     * Expected: waitForTextDisappear returns true when text disappears
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForTextDisappear_TextRemovedDelayed_ReturnsTrue() throws InterruptedException {
        // Arrange
        screen.setScreenText("Processing...", 50);
        executor.submit(() -> {
            try {
                Thread.sleep(40);
                screen.clearScreen();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Act
        boolean result = screen.waitForTextDisappear("Processing", 1000);

        // Assert
        assertTrue(result,"Should detect text disappearance");
    }

    /**
     * POSITIVE TEST 8: waitForCursor with movement after delay
     *
     * Scenario: Cursor moves to target position after 60ms
     * Timeout: 1000ms
     * Concurrency: Single-threaded delayed movement
     *
     * Expected: waitForCursor returns true after cursor movement
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForCursor_CursorMovesDelayed_ReturnsTrue() throws InterruptedException {
        // Arrange
        screen.setCursorPosition(0, 0);
        executor.submit(() -> {
            try {
                Thread.sleep(60);
                screen.setCursorPosition(12, 40);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Act
        boolean result = screen.waitForCursor(12, 40, 1000);

        // Assert
        assertTrue(result,"Should find cursor after movement");
    }

    /**
     * POSITIVE TEST 9: Multiple rapid concurrent updates, text appears
     *
     * Scenario: Screen updated rapidly from multiple operations, target text appears
     * Timeout: 2000ms
     * Concurrency: Rapid multi-threaded writes
     * Screen change: Eventual consistency
     *
     * Expected: waitForText finds text despite concurrent modifications
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForText_RapidConcurrentUpdates_ReturnsTrue() throws InterruptedException {
        // Arrange
        executor.submit(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    screen.setScreenText("Status: " + i, 200);
                    Thread.sleep(20);
                }
                screen.setScreenText("COMPLETE", 300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Act
        boolean result = screen.waitForText("COMPLETE", 2000);

        // Assert
        assertTrue(result,"Should find text despite rapid concurrent updates");
    }

    /**
     * POSITIVE TEST 10: waitForUnlock with OIA state change and keyboard unlock
     *
     * Scenario: Both OIA inhibited and keyboard locked, both become false
     * Timeout: 1000ms
     * Concurrency: Coordinated state transitions
     *
     * Expected: Proper state synchronization, returns true
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForUnlock_WithOIAStateChange_ReturnsTrue() throws InterruptedException {
        // Arrange
        screen.setKeyboardLocked(true);
        screen.setOIAInputInhibited(true);
        executor.submit(() -> {
            try {
                Thread.sleep(70);
                screen.setOIAInputInhibited(false);
                Thread.sleep(20);
                screen.setKeyboardLocked(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Act
        boolean result = screen.waitForUnlock(1000);

        // Assert
        assertTrue(result,"Should unlock despite OIA state changes");
    }

    // ============================================================================
    // ADVERSARIAL TESTS: Timeout, mismatch, never-matching scenarios (10 tests)
    // ============================================================================

    /**
     * ADVERSARIAL TEST 1: waitForText timeout with non-matching text
     *
     * Scenario: Search text never appears on screen
     * Timeout: 100ms
     * Pattern: Exact match
     * Screen change: No change
     * Concurrency: None
     *
     * Expected: waitForText returns false after timeout
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForText_NeverMatches_TimeoutReturnsFalse() {
        // Arrange
        screen.setScreenText("Initial screen content", 0);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForText("NEVER_APPEARS", 100);

        // Assert
        assertFalse(result,"Should timeout and return false");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 100,"Should wait for full timeout");
        assertTrue(elapsed < 300,"Should not exceed timeout by much");
    }

    /**
     * ADVERSARIAL TEST 2: waitForText timeout with zero milliseconds
     *
     * Scenario: Zero timeout, immediate check only
     * Timeout: 0ms
     * Pattern: Text not present
     * Screen change: No change
     *
     * Expected: waitForText returns false immediately
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForText_ZeroTimeout_ReturnsImmediately() {
        // Arrange
        screen.setScreenText("Some content", 0);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForText("Not here", 0);

        // Assert
        assertFalse(result,"Should fail with zero timeout");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed < 50,"Should return immediately (< 50ms)");
    }

    /**
     * ADVERSARIAL TEST 3: waitForCursor at wrong position, timeout
     *
     * Scenario: Wait for cursor at row 20, col 40; cursor stays at row 5, col 10
     * Timeout: 150ms
     * Screen change: No change
     *
     * Expected: waitForCursor returns false after timeout
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForCursor_WrongPosition_TimeoutReturnsFalse() {
        // Arrange
        screen.setCursorPosition(5, 10);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForCursor(20, 40, 150);

        // Assert
        assertFalse(result,"Should timeout waiting for wrong position");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 150,"Should wait for full timeout");
    }

    /**
     * ADVERSARIAL TEST 4: waitForUnlock with persistent lock, timeout
     *
     * Scenario: Keyboard locked, remains locked throughout wait period
     * Timeout: 100ms
     * Concurrency: No state change
     *
     * Expected: waitForUnlock returns false after timeout
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForUnlock_RemainsLocked_TimeoutReturnsFalse() {
        // Arrange
        screen.setKeyboardLocked(true);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForUnlock(100);

        // Assert
        assertFalse(result,"Should timeout while locked");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 100,"Should wait for timeout period");
    }

    /**
     * ADVERSARIAL TEST 5: waitForTextDisappear with persistent text
     *
     * Scenario: Text on screen does not disappear during wait
     * Timeout: 100ms
     * Concurrency: No removal
     *
     * Expected: waitForTextDisappear returns false after timeout
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForTextDisappear_PersistsOnScreen_TimeoutReturnsFalse() {
        // Arrange
        screen.setScreenText("Persistent text", 100);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForTextDisappear("Persistent", 100);

        // Assert
        assertFalse(result,"Should timeout while text persists");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 100,"Should wait for full timeout");
    }

    /**
     * ADVERSARIAL TEST 6: waitForOIAClear with persistent inhibit
     *
     * Scenario: OIA input inhibited, remains inhibited throughout wait
     * Timeout: 120ms
     * Concurrency: No state change
     *
     * Expected: waitForOIAClear returns false after timeout
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForOIAClear_RemainsInhibited_TimeoutReturnsFalse() {
        // Arrange
        screen.setOIAInputInhibited(true);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForOIAClear(120);

        // Assert
        assertFalse(result,"Should timeout while inhibited");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 120,"Should wait for timeout period");
    }

    /**
     * ADVERSARIAL TEST 7: waitForText with case mismatch, exact match mode
     *
     * Scenario: Text "HELLO" on screen, search for "hello" (lowercase)
     * Timeout: 200ms
     * Pattern: Exact match (case-sensitive)
     * Concurrency: None
     *
     * Expected: waitForText returns false (case mismatch)
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForText_CaseMismatch_TimeoutReturnsFalse() {
        // Arrange
        screen.setScreenText("HELLO WORLD", 0);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForText("hello world", 200);

        // Assert
        assertFalse(result,"Should fail on case mismatch");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed >= 200,"Should wait approximately 200ms");
    }

    /**
     * ADVERSARIAL TEST 8: Concurrent rapid modifications prevent stable match
     *
     * Scenario: Screen content rapidly changes, preventing stable pattern match
     * Timeout: 200ms
     * Concurrency: Rapid contentious updates
     * Screen change: Unstable (constantly changing)
     *
     * Expected: waitForText returns false due to continuous changes
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForText_RapidContentChanges_TimeoutReturnsFalse() throws InterruptedException {
        // Arrange
        executor.submit(() -> {
            try {
                for (int i = 0; i < 50; i++) {
                    screen.clearScreen();
                    screen.setScreenText("Changing " + (i % 10), 50);
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Act
        boolean result = screen.waitForText("STABLE_PATTERN", 200);

        // Assert
        assertFalse(result,"Should timeout due to unstable content");
    }

    /**
     * ADVERSARIAL TEST 9: waitForCursor with multiple movements, misses target
     *
     * Scenario: Cursor moves rapidly through positions, never lands on target
     * Timeout: 150ms
     * Concurrency: Rapid cursor updates
     * Pattern: Target position 15, 30; cursor moves through 0-5, 10-15, 20-25 range
     *
     * Expected: waitForCursor returns false, target never matched
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForCursor_RapidMovementMissesTarget_TimeoutReturnsFalse() throws InterruptedException {
        // Arrange
        executor.submit(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    screen.setCursorPosition(i % 10, i * 2);
                    Thread.sleep(15);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Act
        boolean result = screen.waitForCursor(15, 30, 250);

        // Assert
        assertFalse(result,"Should timeout as cursor never reaches target");
    }

    /**
     * ADVERSARIAL TEST 10: waitForUnlock interrupted by Exception
     *
     * Scenario: Wait operation encounters InterruptedException
     * Timeout: 500ms
     * Concurrency: Main thread interrupted
     *
     * Expected: waitForUnlock handles interruption, returns false
     * Verification: Thread interrupt flag restored
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForUnlock_InterruptedException_ReturnsFalse() throws InterruptedException {
        // Arrange
        screen.setKeyboardLocked(true);
        final boolean[] result = {true};

        Thread waitThread = new Thread(() -> {
            result[0] = screen.waitForUnlock(2000);
        });
        waitThread.start();

        // Act: Interrupt the wait thread after short delay
        Thread.sleep(50);
        waitThread.interrupt();
        waitThread.join(1000);

        // Assert
        assertFalse(result[0],"Should return false on interruption");
    }

    // ============================================================================
    // BOUNDARY TESTS: Edge cases in timeouts and state transitions
    // ============================================================================

    /**
     * BOUNDARY TEST: Negative timeout value handling
     *
     * Scenario: Negative timeout provided (invalid)
     * Expected: Should treat as 0ms timeout (immediate)
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForText_NegativeTimeout_TreatsAsZero() {
        // Arrange
        screen.setScreenText("Test content", 0);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForText("Not present", -1);

        // Assert
        // Implementation treats -1 as 0 (immediate timeout)
        assertFalse(result,"Negative timeout should fail immediately");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed < 100,"Should return immediately");
    }

    /**
     * BOUNDARY TEST: Large timeout value
     *
     * Scenario: Timeout of 30000ms (30 seconds) but text appears immediately
     * Expected: Should return true without waiting full timeout
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForText_LargeTimeout_ExitsEarly_OnMatch() {
        // Arrange
        screen.setScreenText("IMMEDIATE_TEXT", 200);
        long startTime = System.currentTimeMillis();

        // Act
        boolean result = screen.waitForText("IMMEDIATE_TEXT", 30000);

        // Assert
        assertTrue(result,"Should find text");
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed < 500,"Should exit immediately on match, not wait 30s");
    }

    /**
     * BOUNDARY TEST: Cursor at boundary positions
     *
     * Scenario: Wait for cursor at screen corners and edges
     * Expected: Should handle boundary positions correctly
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWaitForCursor_BoundaryPositions_SuccessfulMatch() {
        // Arrange
        int maxRow = screen.getRows() - 1;
        int maxCol = screen.getColumns() - 1;

        // Test top-left
        screen.setCursorPosition(0, 0);
        assertTrue(screen.waitForCursor(0, 0, 100),"Should match top-left corner");

        // Test bottom-right
        screen.setCursorPosition(maxRow, maxCol);
        assertTrue(screen.waitForCursor(maxRow, maxCol, 100),"Should match bottom-right corner");

        // Test center
        screen.setCursorPosition(12, 40);
        assertTrue(screen.waitForCursor(12, 40, 100),"Should match center position");
    }
}
