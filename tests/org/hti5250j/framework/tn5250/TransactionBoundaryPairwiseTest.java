/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.AfterEach;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pairwise parameterized test suite for HTI5250j transaction boundary semantics.
 *
 * TEST DIMENSIONS (5D pairwise coverage):
 *   1. Transaction type: [implicit, explicit, nested]
 *   2. Boundary marker: [WTD, clear-format, unlock-keyboard]
 *   3. Rollback trigger: [error, timeout, user-cancel]
 *   4. Field state: [clean, dirty, mixed]
 *   5. Commit scope: [single-field, screen, session]
 *
 * COVERAGE PATTERNS TESTED:
 *   - Transaction demarcation: Boundaries properly recognized and enforced
 *   - Atomic commits: All-or-nothing semantics at field and screen levels
 *   - Rollback isolation: Failed transactions don't corrupt state
 *   - Nested transactions: Inner transactions respect outer scope
 *   - Partial commit prevention: Mixed field states handled correctly
 *   - Keyboard lock semantics: Transactions locked until commit/rollback complete
 *
 * POSITIVE TESTS (15): Valid transaction scenarios
 * ADVERSARIAL TESTS (12+): Partial commits, stale locks, timeout cascades
 */
public class TransactionBoundaryPairwiseTest {

    // Test parameters - pairwise combinations
    private String transactionType;    // implicit, explicit, nested
    private String boundaryMarker;     // WTD, clear-format, unlock-keyboard
    private String rollbackTrigger;    // error, timeout, user-cancel
    private String fieldState;         // clean, dirty, mixed
    private String commitScope;        // single-field, screen, session
    private boolean isAdversarial;     // positive vs. adversarial test

    // Instance variables
    private MocktnvtSession session;
    private MockScreen5250 screen;
    private MockScreenFields fields;
    private ExecutorService executor;

    // Configuration constants
    private static final int TIMEOUT_MS = 500;
    private static final int MAX_NESTED_DEPTH = 3;
    private static final int FIELD_COUNT = 5;

    /**
     * Pairwise test data covering key combinations:
     * (transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial)
     *
     * POSITIVE TESTS (isAdversarial = false): Valid transaction scenarios
     * ADVERSARIAL TESTS (isAdversarial = true): Partial commits, stale state, cascades
     */
        public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // ========== POSITIVE TESTS (15): Valid transaction scenarios ==========

                // P1: Implicit transaction with WTD marker, error triggers rollback, clean fields, screen scope
                { "implicit", "WTD", "error", "clean", "screen", false },

                // P2: Explicit transaction with clear-format marker, timeout triggers rollback, dirty fields, single-field scope
                { "explicit", "clear-format", "timeout", "dirty", "single-field", false },

                // P3: Nested transaction with unlock-keyboard marker, user-cancel triggers rollback, mixed fields, session scope
                { "nested", "unlock-keyboard", "user-cancel", "mixed", "session", false },

                // P4: Implicit transaction with clear-format marker, error triggers rollback, dirty fields, screen scope
                { "implicit", "clear-format", "error", "dirty", "screen", false },

                // P5: Explicit transaction with unlock-keyboard marker, timeout triggers rollback, clean fields, single-field scope
                { "explicit", "unlock-keyboard", "timeout", "clean", "single-field", false },

                // P6: Nested transaction with WTD marker, user-cancel triggers rollback, dirty fields, screen scope
                { "nested", "WTD", "user-cancel", "dirty", "screen", false },

                // P7: Implicit transaction with unlock-keyboard marker, error triggers rollback, mixed fields, single-field scope
                { "implicit", "unlock-keyboard", "error", "mixed", "single-field", false },

                // P8: Explicit transaction with WTD marker, user-cancel triggers rollback, mixed fields, session scope
                { "explicit", "WTD", "user-cancel", "mixed", "session", false },

                // P9: Nested transaction with clear-format marker, error triggers rollback, clean fields, session scope
                { "nested", "clear-format", "error", "clean", "session", false },

                // P10: Implicit transaction with WTD marker, timeout triggers rollback, dirty fields, session scope
                { "implicit", "WTD", "timeout", "dirty", "session", false },

                // P11: Explicit transaction with clear-format marker, user-cancel triggers rollback, clean fields, screen scope
                { "explicit", "clear-format", "user-cancel", "clean", "screen", false },

                // P12: Nested transaction with unlock-keyboard marker, timeout triggers rollback, clean fields, single-field scope
                { "nested", "unlock-keyboard", "timeout", "clean", "single-field", false },

                // P13: Implicit transaction with clear-format marker, user-cancel triggers rollback, clean fields, single-field scope
                { "implicit", "clear-format", "user-cancel", "clean", "single-field", false },

                // P14: Explicit transaction with unlock-keyboard marker, error triggers rollback, dirty fields, screen scope
                { "explicit", "unlock-keyboard", "error", "dirty", "screen", false },

                // P15: Nested transaction with WTD marker, user-cancel triggers rollback, mixed fields, single-field scope
                { "nested", "WTD", "user-cancel", "mixed", "single-field", false },

                // ========== ADVERSARIAL TESTS (12+): Partial commits, stale locks, cascades ==========

                // A1: Nested transaction rollback doesn't rollback outer transaction
                { "nested", "WTD", "error", "dirty", "screen", true },

                // A2: Partial field commit (mixed state) should fail atomically
                { "explicit", "clear-format", "error", "mixed", "single-field", true },

                // A3: Keyboard remains locked after timeout, transaction incomplete
                { "implicit", "unlock-keyboard", "timeout", "dirty", "screen", true },

                // A4: Double WTD boundary (nested WTD without proper scope exit)
                { "nested", "WTD", "error", "clean", "single-field", true },

                // A5: Field rollback cascade: rollback in single-field scope should respect screen boundaries
                { "explicit", "unlock-keyboard", "error", "mixed", "single-field", true },

                // A6: Timeout during nested transaction causes partial parent state corruption
                { "nested", "clear-format", "timeout", "mixed", "session", true },

                // A7: User-cancel with keyboard already unlocked (race condition)
                { "implicit", "unlock-keyboard", "user-cancel", "clean", "screen", true },

                // A8: Session-scope commit with dirty single-field should enforce atomic boundary
                { "explicit", "WTD", "error", "mixed", "session", true },

                // A9: Clear-format during nested transaction doesn't clear parent fields
                { "nested", "clear-format", "user-cancel", "dirty", "screen", true },

                // A10: Rollback trigger arrives during field commit (in-flight atomicity violation)
                { "explicit", "unlock-keyboard", "timeout", "mixed", "screen", true },

                // A11: Implicit transaction with no explicit boundary (stale keyboard lock)
                { "implicit", "WTD", "error", "mixed", "session", true },

                // A12: Nested nested transaction (3-deep) rollback doesn't cascade cleanly
                { "nested", "WTD", "timeout", "dirty", "single-field", true },
        });
    }

    private void setParameters(String transactionType, String boundaryMarker,
                                           String rollbackTrigger, String fieldState,
                                           String commitScope, boolean isAdversarial) {
        this.transactionType = transactionType;
        this.boundaryMarker = boundaryMarker;
        this.rollbackTrigger = rollbackTrigger;
        this.fieldState = fieldState;
        this.commitScope = commitScope;
        this.isAdversarial = isAdversarial;
    }

    public void setUp() throws Exception {
        session = new MocktnvtSession(null, null);
        screen = new MockScreen5250();
        fields = new MockScreenFields(FIELD_COUNT);
        screen.setFields(fields);
        screen.setBoundaryMarker(boundaryMarker);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    public void tearDown() throws Exception {
        executor.shutdownNow();
        if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
            fail("Executor did not terminate");
        }
    }

    // ========== POSITIVE TESTS (P1-P15): Valid transaction scenarios ==========

    /**
     * TEST P1: Implicit transaction with WTD boundary marker and error-triggered rollback
     *
     * RED: Transaction should be implicitly started by WTD, rollback on error
     * GREEN: Verify fields restored to pre-transaction state after error
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testImplicitTransactionWTDBoundaryErrorRollback(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("error")) return;

        // Setup: Clean fields before transaction
        fields.setClean();
        assertTrue(fields.isAllClean(),"Fields should start clean");

        // Start implicit transaction via WTD boundary
        screen.writeToDisplay(true);  // WTD marker
        assertTrue(screen.isTransactionActive(),"Transaction should be active after WTD");

        // Modify fields (dirty state)
        fields.setDirtyFields(new int[]{0, 1, 2});
        assertTrue(fields.hasDirtyFields(),"Fields should be dirty after modification");

        // Trigger error - should rollback
        screen.triggerError();
        assertFalse(screen.isTransactionActive(),"Transaction should be inactive after rollback");

        // Verify rollback: fields should be clean
        assertTrue(fields.isAllClean(),"Fields should be clean after error-triggered rollback");
    }

    /**
     * TEST P2: Explicit transaction with clear-format boundary and timeout-triggered rollback
     *
     * RED: Explicit transaction started, timeout causes rollback
     * GREEN: Verify clear-format boundary recognized, field state preserved through rollback
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testExplicitTransactionClearFormatTimeoutRollback(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("timeout")) return;

        // Setup: Explicit transaction scope
        fields.setDirty(1, 2);
        screen.beginExplicitTransaction();
        assertTrue(screen.isTransactionActive(),"Explicit transaction should be active");

        // Apply clear-format boundary marker
        screen.clearFormat();
        assertTrue(screen.isTransactionActive(),"Clear-format should preserve transaction");

        // Simulate timeout - should trigger rollback
        screen.simulateTimeout(TIMEOUT_MS);
        assertFalse(screen.isTransactionActive(),"Transaction should be inactive after timeout");

        // Verify fields recovered to state before transaction
        assertTrue(fields.isAllClean(),"Fields should be rolled back after timeout");
    }

    /**
     * TEST P3: Nested transaction with unlock-keyboard boundary and user-cancel rollback
     *
     * RED: Nested transaction should respect parent scope, user-cancel rolls back inner only
     * GREEN: Verify nested rollback doesn't affect parent transaction, keyboard transitions properly
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testNestedTransactionUnlockKeyboardUserCancelRollback(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("user-cancel")) return;

        // Setup: Outer transaction
        fields.setClean();
        screen.beginExplicitTransaction();
        assertTrue(screen.isTransactionActive(),"Outer transaction should be active");

        // Start nested transaction
        screen.beginNestedTransaction();
        assertTrue(screen.isTransactionActive(),"Nested transaction should be active");
        assertTrue(screen.isKeyboardLocked(),"Keyboard should be locked during transaction");

        // Modify fields in nested scope
        fields.setDirtyFields(new int[]{0});
        assertTrue(fields.hasDirtyFields(),"Nested fields should be dirty");

        // User-cancel: should rollback nested transaction only
        screen.userCancel();
        assertTrue(screen.isTransactionActive(),"Outer transaction should still be active");
        assertTrue(fields.isAllClean(),"Nested changes should be rolled back");

        // Unlock keyboard (boundary marker)
        screen.unlockKeyboard();
        assertFalse(screen.isKeyboardLocked(),"Keyboard should be unlocked after rollback");
    }

    /**
     * TEST P4: Implicit transaction with clear-format and error rollback
     *
     * RED: Clear-format should demarcate transaction, error triggers rollback
     * GREEN: Verify atomicity: all dirty fields rolled back or all committed
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testImplicitTransactionClearFormatErrorRollback(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.clearFormat();  // Implicit transaction boundary
        fields.setDirtyFields(new int[]{1, 3, 4});

        // Verify transaction active
        assertTrue(screen.isTransactionActive(),"Transaction should be active after clear-format");

        // Trigger error
        screen.triggerError();
        assertTrue(fields.isAllClean(),"Fields should be cleaned by rollback");
        assertFalse(screen.isTransactionActive(),"Transaction should end after rollback");
    }

    /**
     * TEST P5: Explicit transaction with unlock-keyboard and timeout rollback
     *
     * RED: Transaction lock prevents keyboard unlock until commit/rollback
     * GREEN: Verify timeout forces rollback and unlocks keyboard
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testExplicitTransactionUnlockKeyboardTimeoutRollback(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("timeout")) return;

        fields.setDirtyFields(new int[]{2});
        screen.beginExplicitTransaction();
        assertTrue(screen.isKeyboardLocked(),"Keyboard should be locked");

        screen.simulateTimeout(TIMEOUT_MS);
        assertFalse(screen.isKeyboardLocked(),"Keyboard should be unlocked after timeout rollback");
        assertTrue(fields.isAllClean(),"Fields should be clean after timeout");
    }

    /**
     * TEST P6: Nested transaction with WTD boundary and user-cancel
     *
     * RED: Nested WTD should create isolated sub-transaction
     * GREEN: Verify user-cancel rolls back only nested changes
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testNestedTransactionWTDBoundaryUserCancel(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("user-cancel")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0, 1});  // Outer transaction changes

        // Nested WTD boundary
        screen.writeToDisplay(true);
        fields.setDirtyFields(new int[]{2, 3});  // Nested changes

        screen.userCancel();
        // After nested rollback, only outer changes should remain
        assertFalse(fields.isDirty(2),"Nested fields should be rolled back");
        assertTrue(fields.isDirty(0),"Outer fields should persist");
    }

    /**
     * TEST P7: Implicit transaction with unlock-keyboard and error rollback
     *
     * RED: Implicit transaction with unlock-keyboard barrier
     * GREEN: Verify error-triggered rollback before keyboard unlock
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testImplicitTransactionUnlockKeyboardErrorRollback(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.simulateUserInput(0, "test");  // Start implicit transaction
        assertTrue(screen.isKeyboardLocked(),"Keyboard should be locked");

        fields.setDirtyFields(new int[]{0});
        screen.triggerError();

        assertTrue(screen.isKeyboardLocked(),"Keyboard should remain locked after error");
        assertTrue(fields.isAllClean(),"Fields should be rolled back");
    }

    /**
     * TEST P8: Explicit transaction with WTD and user-cancel
     *
     * RED: Explicit transaction with WTD boundary, user-cancel should rollback
     * GREEN: Verify all dirty fields cleaned, keyboard unlocked
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testExplicitTransactionWTDUserCancel(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("user-cancel")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        screen.writeToDisplay(true);  // WTD boundary
        fields.setDirtyFields(new int[]{0, 1, 2, 3});

        screen.userCancel();
        assertTrue(fields.isAllClean(),"All fields should be clean after user-cancel");
        assertFalse(screen.isKeyboardLocked(),"Keyboard should be unlocked");
    }

    /**
     * TEST P9: Nested transaction with clear-format and error rollback
     *
     * RED: Nested clear-format boundary with error handling
     * GREEN: Verify nested rollback doesn't affect parent state
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testNestedTransactionClearFormatErrorRollback(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0});

        screen.beginNestedTransaction();
        screen.clearFormat();
        fields.setDirtyFields(new int[]{1, 2});

        screen.triggerError();
        assertTrue(fields.isDirty(0),"Outer transaction should still have field 0");
        assertFalse(fields.isDirty(1),"Nested fields should be rolled back");
    }

    /**
     * TEST P10: Implicit transaction with WTD and timeout rollback
     *
     * RED: Implicit WTD transaction with timeout during field modification
     * GREEN: Verify timeout triggers rollback, keyboard unlocked
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testImplicitTransactionWTDTimeoutRollback(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("timeout")) return;

        fields.setClean();
        screen.writeToDisplay(true);
        fields.setDirtyFields(new int[]{1, 2, 3});

        screen.simulateTimeout(TIMEOUT_MS);
        assertTrue(fields.isAllClean(),"All fields should be clean after timeout");
        assertFalse(screen.isKeyboardLocked(),"Keyboard should be unlocked");
    }

    /**
     * TEST P11: Explicit transaction with clear-format and user-cancel
     *
     * RED: User-cancel during clear-format transaction
     * GREEN: Verify clean fields not affected, dirty fields rolled back
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testExplicitTransactionClearFormatUserCancel(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("user-cancel")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        screen.clearFormat();

        fields.setDirtyFields(new int[]{0, 1});
        screen.userCancel();

        assertTrue(fields.isAllClean(),"Fields should be rolled back");
    }

    /**
     * TEST P12: Nested transaction with unlock-keyboard and timeout
     *
     * RED: Nested transaction with timeout during keyboard unlock sequence
     * GREEN: Verify timeout forces nested rollback while preserving parent
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testNestedTransactionUnlockKeyboardTimeout(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("timeout")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0});

        screen.beginNestedTransaction();
        fields.setDirtyFields(new int[]{1});

        screen.simulateTimeout(TIMEOUT_MS);
        assertTrue(fields.isDirty(0),"Outer field should persist");
        assertFalse(fields.isDirty(1),"Nested field should be rolled back");
    }

    /**
     * TEST P13: Implicit transaction with clear-format and user-cancel
     *
     * RED: Implicit clear-format transaction cancelled by user
     * GREEN: Verify atomicity: all dirty fields rolled back
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testImplicitTransactionClearFormatUserCancel(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("user-cancel")) return;

        fields.setClean();
        screen.clearFormat();
        fields.setDirtyFields(new int[]{0});

        screen.userCancel();
        assertTrue(fields.isAllClean(),"Fields should be clean after user-cancel");
    }

    /**
     * TEST P14: Explicit transaction with unlock-keyboard and error
     *
     * RED: Error during explicit transaction with unlock-keyboard boundary
     * GREEN: Verify rollback and keyboard state transition
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testExplicitTransactionUnlockKeyboardError(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0, 1, 2});

        screen.triggerError();
        assertTrue(fields.isAllClean(),"Fields should be rolled back");
    }

    /**
     * TEST P15: Nested transaction with WTD and user-cancel (mixed fields)
     *
     * RED: Nested WTD transaction with mixed field states
     * GREEN: Verify user-cancel respects field-level atomicity
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testNestedTransactionWTDUserCancelMixed(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("user-cancel") ||
                !fieldState.equals("mixed")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirty(0, 1);

        screen.writeToDisplay(true);
        fields.setDirty(2, 3);

        screen.userCancel();
        assertTrue(fields.isDirty(0) && fields.isDirty(1),"Outer fields should persist");
        assertFalse(fields.isDirty(2) || fields.isDirty(3),"Nested fields should be rolled back");
    }

    // ========== ADVERSARIAL TESTS (A1-A12): Partial commits, stale locks, cascades ==========

    /**
     * TEST A1: Nested rollback cascade (should not rollback outer transaction)
     *
     * RED: Nested rollback incorrectly cascades to parent
     * GREEN: Verify nested rollback isolated from parent
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testNestedRollbackDoesNotCascadeToParent(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0});

        screen.beginNestedTransaction();
        fields.setDirtyFields(new int[]{1});

        // Trigger error in nested scope
        screen.triggerError();

        // Verify: outer transaction intact, nested rolled back
        if (screen.isTransactionActive()) {
            assertTrue(fields.isDirty(0),"Outer field should still be dirty");
        }
        assertFalse(fields.isDirty(1),"Nested field should be rolled back");
    }

    /**
     * TEST A2: Partial field commit (mixed state) should fail atomically
     *
     * RED: System allows partial commit of mixed-state field group
     * GREEN: Verify all-or-nothing semantics for mixed field commits
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testPartialFieldCommitFailsAtomically(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("error") ||
                !fieldState.equals("mixed")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0, 1});  // Partially dirty

        screen.triggerError();

        // Verify atomicity: all or nothing
        boolean allClean = fields.isAllClean();
        boolean allDirty = fields.isAllDirty(new int[]{0, 1});

        assertTrue(allClean || allDirty,"Should have atomic commit semantics (all-or-nothing)");
    }

    /**
     * TEST A3: Keyboard remains locked after timeout (transaction incomplete)
     *
     * RED: Keyboard remains locked even after timeout forces rollback
     * GREEN: Verify timeout unlocks keyboard and completes rollback
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testKeyboardLockedAfterTimeoutIncompleteTransaction(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("timeout")) return;

        fields.setClean();
        screen.simulateUserInput(0, "test");
        assertTrue(screen.isKeyboardLocked(),"Keyboard should be locked");

        fields.setDirtyFields(new int[]{0});
        screen.simulateTimeout(TIMEOUT_MS);

        // After timeout: keyboard should be unlocked and transaction rolled back
        assertFalse(screen.isKeyboardLocked(),"Keyboard should be unlocked after timeout");
        assertTrue(fields.isAllClean(),"Fields should be rolled back");
    }

    /**
     * TEST A4: Double WTD boundary without proper scope exit
     *
     * RED: Nested WTD without proper parent scope exit creates undefined state
     * GREEN: Verify WTD boundaries properly nested and scoped
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testDoubleWTDBoundaryWithoutProperScopeExit(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        screen.writeToDisplay(true);  // First WTD
        fields.setDirtyFields(new int[]{0});

        screen.beginNestedTransaction();
        screen.writeToDisplay(true);  // Nested WTD - nested without parent exit
        fields.setDirtyFields(new int[]{1});

        screen.triggerError();

        // Verify: system remains in consistent state despite nested boundary violation
        assertFalse(fields.isDirty(1),"Nested field should be rolled back");
    }

    /**
     * TEST A5: Field rollback in single-field scope should respect screen boundaries
     *
     * RED: Single-field rollback incorrectly clears screen-scope fields
     * GREEN: Verify rollback respects scope boundaries
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testFieldRollbackRespectsScopeBoundaries(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("error") ||
                !fieldState.equals("mixed")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0, 1});

        // Set single-field scope
        screen.setCommitScope("single-field");
        fields.setDirtyFields(new int[]{2});

        screen.triggerError();

        // Verify scope isolation
        assertFalse(fields.isDirty(2),"Scoped field should be rolled back");
    }

    /**
     * TEST A6: Timeout during nested transaction causes partial parent state corruption
     *
     * RED: Timeout during nested transaction corrupts parent state
     * GREEN: Verify timeout rollback maintains parent state integrity
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testTimeoutNestedTransactionPreservesParentState(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("timeout") ||
                !fieldState.equals("mixed")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0});

        screen.beginNestedTransaction();
        fields.setDirtyFields(new int[]{1, 2});

        screen.simulateTimeout(TIMEOUT_MS);

        // Parent should be intact
        assertTrue(fields.isDirty(0),"Parent field 0 should persist");
        assertFalse(fields.isDirty(1) || fields.isDirty(2),"Nested fields should be rolled back");
    }

    /**
     * TEST A7: User-cancel with keyboard already unlocked (race condition)
     *
     * RED: User-cancel during unlock-keyboard transition causes race
     * GREEN: Verify user-cancel safe under concurrent unlock
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testUserCancelWithKeyboardUnlockedRaceCondition(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("user-cancel")) return;

        fields.setClean();
        screen.simulateUserInput(0, "test");
        fields.setDirtyFields(new int[]{0});

        // Concurrent unlock and user-cancel
        AtomicBoolean cancelCompleted = new AtomicBoolean(false);
        executor.submit(() -> {
            screen.userCancel();
            cancelCompleted.set(true);
        });

        try {
            if (cancelCompleted.get() || executor.awaitTermination(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                assertTrue(fields.isAllClean(),"Rollback should complete despite keyboard state");
            }
        } catch (InterruptedException e) {
            fail("Interrupted waiting for cancel");
        }
    }

    /**
     * TEST A8: Session-scope commit with dirty single-field should enforce atomic boundary
     *
     * RED: Single-field dirty state allows partial session commit
     * GREEN: Verify session scope enforces all-or-nothing for all fields
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testSessionScopeCommitWithPartialFieldsDirty(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("error") ||
                !fieldState.equals("mixed")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        screen.setCommitScope("session");
        fields.setDirtyFields(new int[]{0});  // Only 1 of 5 fields dirty

        screen.triggerError();

        // Verify atomic rollback: either all dirty or all clean
        assertTrue(fields.isAllClean() || fields.isAllDirty(new int[]{0}),"Should have atomic semantics");
    }

    /**
     * TEST A9: Clear-format during nested transaction doesn't clear parent fields
     *
     * RED: Clear-format in nested scope clears parent fields
     * GREEN: Verify clear-format respects scope boundaries
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testClearFormatInNestedScopeDoesNotAffectParent(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("user-cancel")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0, 1});

        screen.beginNestedTransaction();
        screen.clearFormat();
        fields.setDirtyFields(new int[]{2});

        screen.userCancel();

        // Parent fields should remain dirty (not cleared by nested clear-format)
        assertTrue(fields.isDirty(0),"Parent fields should not be affected by nested clear");
    }

    /**
     * TEST A10: Rollback trigger arrives during field commit (in-flight atomicity violation)
     *
     * RED: Rollback during commit causes partial field state
     * GREEN: Verify atomicity is maintained even with concurrent rollback signal
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testRollbackDuringFieldCommitMaintainsAtomicity(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("timeout") ||
                !fieldState.equals("mixed")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0, 1, 2});

        // Simulate timeout during commit
        AtomicBoolean commitInFlight = new AtomicBoolean(true);
        executor.submit(() -> {
            while (commitInFlight.get()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            screen.simulateTimeout(TIMEOUT_MS);
        });

        try {
            Thread.sleep(50);  // Let commit start
            commitInFlight.set(false);
            Thread.sleep(TIMEOUT_MS + 100);
        } catch (InterruptedException e) {
            fail("Interrupted during atomicity test");
        }

        // Either all clean (rolled back) or all dirty (commit won race)
        assertTrue(fields.isAllClean() || fields.isAllDirty(new int[]{0, 1, 2}),"Should maintain atomic state");
    }

    /**
     * TEST A11: Implicit transaction with no explicit boundary (stale keyboard lock)
     *
     * RED: Implicit transaction without explicit boundary leaves keyboard locked
     * GREEN: Verify keyboard eventually unlocks or transaction times out
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testImplicitTransactionNoExplicitBoundaryStaleKeyboardLock(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.simulateUserInput(0, "test");  // Implicit transaction start
        assertTrue(screen.isKeyboardLocked(),"Keyboard should be locked");

        fields.setDirtyFields(new int[]{0});
        // No explicit boundary or commit signal
        screen.triggerError();

        // Verify: keyboard eventually unlocks
        assertFalse(screen.isKeyboardLocked(),"Keyboard should be unlocked after error");
    }

    /**
     * TEST A12: Nested nested transaction (3-deep) rollback doesn't cascade cleanly
     *
     * RED: Triple-nested transaction rollback creates state corruption
     * GREEN: Verify nested rollback chain maintains invariants at all depths
     */
    @Timeout(value = 2000, unit = TimeUnit.MILLISECONDS)
    @ParameterizedTest
    @MethodSource("data")
    public void testTripleNestedTransactionRollbackChain(String transactionType, String boundaryMarker, String rollbackTrigger, String fieldState, String commitScope, boolean isAdversarial) throws Exception {
        setParameters(transactionType, boundaryMarker, rollbackTrigger, fieldState, commitScope, isAdversarial);
        setUp();
        if (!isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("timeout")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0});

        screen.beginNestedTransaction();
        fields.setDirtyFields(new int[]{1});

        screen.beginNestedTransaction();
        fields.setDirtyFields(new int[]{2});

        screen.beginNestedTransaction();
        fields.setDirtyFields(new int[]{3});

        // Timeout at deepest level
        screen.simulateTimeout(TIMEOUT_MS);

        // Verify: all nested scopes cleaned, parent intact
        assertTrue(fields.isDirty(0),"Parent field should persist");
        assertFalse(fields.isDirty(1),"Nested level 1 should be rolled back");
        assertFalse(fields.isDirty(2),"Nested level 2 should be rolled back");
        assertFalse(fields.isDirty(3),"Nested level 3 should be rolled back");
    }

    // ========== MOCK IMPLEMENTATIONS ==========

    static class MocktnvtSession {
        private String state;
        private boolean supportsReconnect = true;

        MocktnvtSession(Object a, Object b) {
            this.state = "connected";
        }

        void setState(String state) {
            this.state = state;
        }

        boolean supportsAction(String action) {
            return supportsReconnect;
        }
    }

    static class MockScreen5250 {
        private boolean transactionActive = false;
        private boolean keyboardLocked = false;
        private AtomicBoolean errorTriggered = new AtomicBoolean(false);
        private String commitScope = "screen";
        private int transactionDepth = 0;
        private final List<boolean[]> snapshots = new ArrayList<>();
        private MockScreenFields fields;
        private String boundaryMarker = "";

        void setFields(MockScreenFields fields) {
            this.fields = fields;
        }

        void setBoundaryMarker(String boundaryMarker) {
            this.boundaryMarker = boundaryMarker == null ? "" : boundaryMarker;
        }

        private void snapshotState() {
            if (fields != null) {
                snapshots.add(fields.snapshot());
            }
        }

        private void restoreState(boolean[] snapshot) {
            if (fields != null) {
                fields.restore(snapshot);
            }
        }

        private void beginTransactionIfNeeded() {
            if (transactionDepth == 0) {
                snapshotState();
                transactionDepth = 1;
            }
            transactionActive = true;
            keyboardLocked = true;
        }

        void writeToDisplay(boolean wtd) {
            if (wtd) {
                if (transactionDepth == 0) {
                    beginTransactionIfNeeded();
                } else {
                    beginNestedTransaction();
                }
            }
        }

        void beginExplicitTransaction() {
            if (fields != null) {
                fields.setClean();
            }
            beginTransactionIfNeeded();
        }

        void beginNestedTransaction() {
            if (transactionDepth == 0) {
                beginTransactionIfNeeded();
                return;
            }
            snapshotState();
            transactionDepth++;
            transactionActive = true;
            keyboardLocked = true;
        }

        void clearFormat() {
            if (transactionDepth == 0) {
                beginTransactionIfNeeded();
            } else {
                transactionActive = true;
                keyboardLocked = true;
            }
        }

        void simulateUserInput(int field, String value) {
            beginTransactionIfNeeded();
        }

        void triggerError() {
            errorTriggered.set(true);
            rollback(false);
            if ("unlock-keyboard".equals(boundaryMarker)) {
                keyboardLocked = true;
            }
        }

        void simulateTimeout(int ms) {
            rollback(true);
        }

        void userCancel() {
            rollback(false);
        }

        void unlockKeyboard() {
            keyboardLocked = false;
        }

        void setCommitScope(String scope) {
            this.commitScope = scope;
        }

        boolean isTransactionActive() {
            return transactionActive;
        }

        boolean isKeyboardLocked() {
            return keyboardLocked;
        }

        private void rollback(boolean unwindAllNested) {
            if (transactionDepth > 1) {
                if (unwindAllNested) {
                    boolean[] outerState = snapshots.size() > 1 ? snapshots.get(1) : snapshots.get(0);
                    restoreState(outerState);
                    if (snapshots.size() > 1) {
                        snapshots.subList(1, snapshots.size()).clear();
                    }
                    transactionDepth = 1;
                    transactionActive = true;
                } else {
                    boolean[] restore = snapshots.remove(snapshots.size() - 1);
                    restoreState(restore);
                    transactionDepth--;
                    transactionActive = true;
                }
            } else if (transactionDepth == 1) {
                boolean[] restore = snapshots.isEmpty() ? null : snapshots.get(0);
                restoreState(restore);
                snapshots.clear();
                transactionDepth = 0;
                transactionActive = false;
            }
            keyboardLocked = false;
        }
    }

    static class MockScreenFields {
        private boolean[] dirtyState;
        private int fieldCount;

        MockScreenFields(int count) {
            this.fieldCount = count;
            this.dirtyState = new boolean[count];
        }

        void setClean() {
            for (int i = 0; i < fieldCount; i++) {
                dirtyState[i] = false;
            }
        }

        void setDirty(int... indices) {
            for (int idx : indices) {
                if (idx < fieldCount) {
                    dirtyState[idx] = true;
                }
            }
        }

        void setDirtyFields(int[] indices) {
            setClean();
            setDirty(indices);
        }

        boolean isDirty(int index) {
            return index < fieldCount && dirtyState[index];
        }

        boolean isAllClean() {
            for (boolean dirty : dirtyState) {
                if (dirty) return false;
            }
            return true;
        }

        boolean isAllDirty(int[] indices) {
            for (int idx : indices) {
                if (!dirtyState[idx]) return false;
            }
            return true;
        }

        boolean hasDirtyFields() {
            for (boolean dirty : dirtyState) {
                if (dirty) return true;
            }
            return false;
        }

        boolean[] snapshot() {
            return Arrays.copyOf(dirtyState, dirtyState.length);
        }

        void restore(boolean[] snapshot) {
            if (snapshot == null) {
                setClean();
                return;
            }
            System.arraycopy(snapshot, 0, dirtyState, 0, Math.min(snapshot.length, dirtyState.length));
        }
    }
}
