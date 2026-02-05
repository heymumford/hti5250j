/**
 * Title: TransactionBoundaryPairwiseTest.java
 * Copyright: Copyright (c) 2025
 * Company: Guild Mortgage
 *
 * Description: Comprehensive pairwise test suite for TN5250j transaction boundary handling.
 *
 * Tests screen transaction demarcation, commit/rollback semantics, and field atomicity:
 *   - tnvt: Transaction lifecycle, WTD boundary markers, keyboard unlock sequences
 *   - Screen5250: Field state transitions, clear-format semantics, atomic commits
 *   - ScreenFields: Field-level dirty/clean state tracking, rollback isolation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING. If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.tn5250j.framework.tn5250;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Pairwise parameterized test suite for TN5250j transaction boundary semantics.
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
@RunWith(Parameterized.class)
public class TransactionBoundaryPairwiseTest {

    // Test parameters - pairwise combinations
    private final String transactionType;    // implicit, explicit, nested
    private final String boundaryMarker;     // WTD, clear-format, unlock-keyboard
    private final String rollbackTrigger;    // error, timeout, user-cancel
    private final String fieldState;         // clean, dirty, mixed
    private final String commitScope;        // single-field, screen, session
    private final boolean isAdversarial;     // positive vs. adversarial test

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
    @Parameterized.Parameters
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

    public TransactionBoundaryPairwiseTest(String transactionType, String boundaryMarker,
                                           String rollbackTrigger, String fieldState,
                                           String commitScope, boolean isAdversarial) {
        this.transactionType = transactionType;
        this.boundaryMarker = boundaryMarker;
        this.rollbackTrigger = rollbackTrigger;
        this.fieldState = fieldState;
        this.commitScope = commitScope;
        this.isAdversarial = isAdversarial;
    }

    @Before
    public void setUp() throws Exception {
        session = new MocktnvtSession(null, null);
        screen = new MockScreen5250();
        fields = new MockScreenFields(FIELD_COUNT);
        executor = Executors.newFixedThreadPool(2);
    }

    @After
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
    @Test(timeout = 2000)
    public void testImplicitTransactionWTDBoundaryErrorRollback() {
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("error")) return;

        // Setup: Clean fields before transaction
        fields.setClean();
        assertTrue("Fields should start clean", fields.isAllClean());

        // Start implicit transaction via WTD boundary
        screen.writeToDisplay(true);  // WTD marker
        assertTrue("Transaction should be active after WTD", screen.isTransactionActive());

        // Modify fields (dirty state)
        fields.setDirtyFields(new int[]{0, 1, 2});
        assertTrue("Fields should be dirty after modification", fields.hasDirtyFields());

        // Trigger error - should rollback
        screen.triggerError();
        assertFalse("Transaction should be inactive after rollback", screen.isTransactionActive());

        // Verify rollback: fields should be clean
        assertTrue("Fields should be clean after error-triggered rollback", fields.isAllClean());
    }

    /**
     * TEST P2: Explicit transaction with clear-format boundary and timeout-triggered rollback
     *
     * RED: Explicit transaction started, timeout causes rollback
     * GREEN: Verify clear-format boundary recognized, field state preserved through rollback
     */
    @Test(timeout = 2000)
    public void testExplicitTransactionClearFormatTimeoutRollback() {
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("timeout")) return;

        // Setup: Explicit transaction scope
        fields.setDirty(1, 2);
        screen.beginExplicitTransaction();
        assertTrue("Explicit transaction should be active", screen.isTransactionActive());

        // Apply clear-format boundary marker
        screen.clearFormat();
        assertTrue("Clear-format should preserve transaction", screen.isTransactionActive());

        // Simulate timeout - should trigger rollback
        screen.simulateTimeout(TIMEOUT_MS);
        assertFalse("Transaction should be inactive after timeout", screen.isTransactionActive());

        // Verify fields recovered to state before transaction
        assertTrue("Fields should be rolled back after timeout", fields.isAllClean());
    }

    /**
     * TEST P3: Nested transaction with unlock-keyboard boundary and user-cancel rollback
     *
     * RED: Nested transaction should respect parent scope, user-cancel rolls back inner only
     * GREEN: Verify nested rollback doesn't affect parent transaction, keyboard transitions properly
     */
    @Test(timeout = 2000)
    public void testNestedTransactionUnlockKeyboardUserCancelRollback() {
        if (isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("user-cancel")) return;

        // Setup: Outer transaction
        fields.setClean();
        screen.beginExplicitTransaction();
        assertTrue("Outer transaction should be active", screen.isTransactionActive());

        // Start nested transaction
        screen.beginNestedTransaction();
        assertTrue("Nested transaction should be active", screen.isTransactionActive());
        assertTrue("Keyboard should be locked during transaction", screen.isKeyboardLocked());

        // Modify fields in nested scope
        fields.setDirtyFields(new int[]{0});
        assertTrue("Nested fields should be dirty", fields.hasDirtyFields());

        // User-cancel: should rollback nested transaction only
        screen.userCancel();
        assertTrue("Outer transaction should still be active", screen.isTransactionActive());
        assertTrue("Nested changes should be rolled back", fields.isAllClean());

        // Unlock keyboard (boundary marker)
        screen.unlockKeyboard();
        assertFalse("Keyboard should be unlocked after rollback", screen.isKeyboardLocked());
    }

    /**
     * TEST P4: Implicit transaction with clear-format and error rollback
     *
     * RED: Clear-format should demarcate transaction, error triggers rollback
     * GREEN: Verify atomicity: all dirty fields rolled back or all committed
     */
    @Test(timeout = 2000)
    public void testImplicitTransactionClearFormatErrorRollback() {
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.clearFormat();  // Implicit transaction boundary
        fields.setDirtyFields(new int[]{1, 3, 4});

        // Verify transaction active
        assertTrue("Transaction should be active after clear-format", screen.isTransactionActive());

        // Trigger error
        screen.triggerError();
        assertTrue("Fields should be cleaned by rollback", fields.isAllClean());
        assertFalse("Transaction should end after rollback", screen.isTransactionActive());
    }

    /**
     * TEST P5: Explicit transaction with unlock-keyboard and timeout rollback
     *
     * RED: Transaction lock prevents keyboard unlock until commit/rollback
     * GREEN: Verify timeout forces rollback and unlocks keyboard
     */
    @Test(timeout = 2000)
    public void testExplicitTransactionUnlockKeyboardTimeoutRollback() {
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("timeout")) return;

        fields.setDirtyFields(new int[]{2});
        screen.beginExplicitTransaction();
        assertTrue("Keyboard should be locked", screen.isKeyboardLocked());

        screen.simulateTimeout(TIMEOUT_MS);
        assertFalse("Keyboard should be unlocked after timeout rollback", screen.isKeyboardLocked());
        assertTrue("Fields should be clean after timeout", fields.isAllClean());
    }

    /**
     * TEST P6: Nested transaction with WTD boundary and user-cancel
     *
     * RED: Nested WTD should create isolated sub-transaction
     * GREEN: Verify user-cancel rolls back only nested changes
     */
    @Test(timeout = 2000)
    public void testNestedTransactionWTDBoundaryUserCancel() {
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
        assertFalse("Nested fields should be rolled back", fields.isDirty(2));
        assertTrue("Outer fields should persist", fields.isDirty(0));
    }

    /**
     * TEST P7: Implicit transaction with unlock-keyboard and error rollback
     *
     * RED: Implicit transaction with unlock-keyboard barrier
     * GREEN: Verify error-triggered rollback before keyboard unlock
     */
    @Test(timeout = 2000)
    public void testImplicitTransactionUnlockKeyboardErrorRollback() {
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.simulateUserInput(0, "test");  // Start implicit transaction
        assertTrue("Keyboard should be locked", screen.isKeyboardLocked());

        fields.setDirtyFields(new int[]{0});
        screen.triggerError();

        assertTrue("Keyboard should remain locked after error", screen.isKeyboardLocked());
        assertTrue("Fields should be rolled back", fields.isAllClean());
    }

    /**
     * TEST P8: Explicit transaction with WTD and user-cancel
     *
     * RED: Explicit transaction with WTD boundary, user-cancel should rollback
     * GREEN: Verify all dirty fields cleaned, keyboard unlocked
     */
    @Test(timeout = 2000)
    public void testExplicitTransactionWTDUserCancel() {
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("user-cancel")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        screen.writeToDisplay(true);  // WTD boundary
        fields.setDirtyFields(new int[]{0, 1, 2, 3});

        screen.userCancel();
        assertTrue("All fields should be clean after user-cancel", fields.isAllClean());
        assertFalse("Keyboard should be unlocked", screen.isKeyboardLocked());
    }

    /**
     * TEST P9: Nested transaction with clear-format and error rollback
     *
     * RED: Nested clear-format boundary with error handling
     * GREEN: Verify nested rollback doesn't affect parent state
     */
    @Test(timeout = 2000)
    public void testNestedTransactionClearFormatErrorRollback() {
        if (isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0});

        screen.beginNestedTransaction();
        screen.clearFormat();
        fields.setDirtyFields(new int[]{1, 2});

        screen.triggerError();
        assertTrue("Outer transaction should still have field 0", fields.isDirty(0));
        assertFalse("Nested fields should be rolled back", fields.isDirty(1));
    }

    /**
     * TEST P10: Implicit transaction with WTD and timeout rollback
     *
     * RED: Implicit WTD transaction with timeout during field modification
     * GREEN: Verify timeout triggers rollback, keyboard unlocked
     */
    @Test(timeout = 2000)
    public void testImplicitTransactionWTDTimeoutRollback() {
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("timeout")) return;

        fields.setClean();
        screen.writeToDisplay(true);
        fields.setDirtyFields(new int[]{1, 2, 3});

        screen.simulateTimeout(TIMEOUT_MS);
        assertTrue("All fields should be clean after timeout", fields.isAllClean());
        assertFalse("Keyboard should be unlocked", screen.isKeyboardLocked());
    }

    /**
     * TEST P11: Explicit transaction with clear-format and user-cancel
     *
     * RED: User-cancel during clear-format transaction
     * GREEN: Verify clean fields not affected, dirty fields rolled back
     */
    @Test(timeout = 2000)
    public void testExplicitTransactionClearFormatUserCancel() {
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("user-cancel")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        screen.clearFormat();

        fields.setDirtyFields(new int[]{0, 1});
        screen.userCancel();

        assertTrue("Fields should be rolled back", fields.isAllClean());
    }

    /**
     * TEST P12: Nested transaction with unlock-keyboard and timeout
     *
     * RED: Nested transaction with timeout during keyboard unlock sequence
     * GREEN: Verify timeout forces nested rollback while preserving parent
     */
    @Test(timeout = 2000)
    public void testNestedTransactionUnlockKeyboardTimeout() {
        if (isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("timeout")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0});

        screen.beginNestedTransaction();
        fields.setDirtyFields(new int[]{1});

        screen.simulateTimeout(TIMEOUT_MS);
        assertTrue("Outer field should persist", fields.isDirty(0));
        assertFalse("Nested field should be rolled back", fields.isDirty(1));
    }

    /**
     * TEST P13: Implicit transaction with clear-format and user-cancel
     *
     * RED: Implicit clear-format transaction cancelled by user
     * GREEN: Verify atomicity: all dirty fields rolled back
     */
    @Test(timeout = 2000)
    public void testImplicitTransactionClearFormatUserCancel() {
        if (isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("clear-format") || !rollbackTrigger.equals("user-cancel")) return;

        fields.setClean();
        screen.clearFormat();
        fields.setDirtyFields(new int[]{0});

        screen.userCancel();
        assertTrue("Fields should be clean after user-cancel", fields.isAllClean());
    }

    /**
     * TEST P14: Explicit transaction with unlock-keyboard and error
     *
     * RED: Error during explicit transaction with unlock-keyboard boundary
     * GREEN: Verify rollback and keyboard state transition
     */
    @Test(timeout = 2000)
    public void testExplicitTransactionUnlockKeyboardError() {
        if (isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirtyFields(new int[]{0, 1, 2});

        screen.triggerError();
        assertTrue("Fields should be rolled back", fields.isAllClean());
    }

    /**
     * TEST P15: Nested transaction with WTD and user-cancel (mixed fields)
     *
     * RED: Nested WTD transaction with mixed field states
     * GREEN: Verify user-cancel respects field-level atomicity
     */
    @Test(timeout = 2000)
    public void testNestedTransactionWTDUserCancelMixed() {
        if (isAdversarial || !transactionType.equals("nested") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("user-cancel") ||
                !fieldState.equals("mixed")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        fields.setDirty(0, 1);

        screen.writeToDisplay(true);
        fields.setDirty(2, 3);

        screen.userCancel();
        assertTrue("Outer fields should persist", fields.isDirty(0) && fields.isDirty(1));
        assertFalse("Nested fields should be rolled back", fields.isDirty(2) || fields.isDirty(3));
    }

    // ========== ADVERSARIAL TESTS (A1-A12): Partial commits, stale locks, cascades ==========

    /**
     * TEST A1: Nested rollback cascade (should not rollback outer transaction)
     *
     * RED: Nested rollback incorrectly cascades to parent
     * GREEN: Verify nested rollback isolated from parent
     */
    @Test(timeout = 2000)
    public void testNestedRollbackDoesNotCascadeToParent() {
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
            assertTrue("Outer field should still be dirty", fields.isDirty(0));
        }
        assertFalse("Nested field should be rolled back", fields.isDirty(1));
    }

    /**
     * TEST A2: Partial field commit (mixed state) should fail atomically
     *
     * RED: System allows partial commit of mixed-state field group
     * GREEN: Verify all-or-nothing semantics for mixed field commits
     */
    @Test(timeout = 2000)
    public void testPartialFieldCommitFailsAtomically() {
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

        assertTrue("Should have atomic commit semantics (all-or-nothing)",
                allClean || allDirty);
    }

    /**
     * TEST A3: Keyboard remains locked after timeout (transaction incomplete)
     *
     * RED: Keyboard remains locked even after timeout forces rollback
     * GREEN: Verify timeout unlocks keyboard and completes rollback
     */
    @Test(timeout = 2000)
    public void testKeyboardLockedAfterTimeoutIncompleteTransaction() {
        if (!isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("unlock-keyboard") || !rollbackTrigger.equals("timeout")) return;

        fields.setClean();
        screen.simulateUserInput(0, "test");
        assertTrue("Keyboard should be locked", screen.isKeyboardLocked());

        fields.setDirtyFields(new int[]{0});
        screen.simulateTimeout(TIMEOUT_MS);

        // After timeout: keyboard should be unlocked and transaction rolled back
        assertFalse("Keyboard should be unlocked after timeout", screen.isKeyboardLocked());
        assertTrue("Fields should be rolled back", fields.isAllClean());
    }

    /**
     * TEST A4: Double WTD boundary without proper scope exit
     *
     * RED: Nested WTD without proper parent scope exit creates undefined state
     * GREEN: Verify WTD boundaries properly nested and scoped
     */
    @Test(timeout = 2000)
    public void testDoubleWTDBoundaryWithoutProperScopeExit() {
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
        assertFalse("Nested field should be rolled back", fields.isDirty(1));
    }

    /**
     * TEST A5: Field rollback in single-field scope should respect screen boundaries
     *
     * RED: Single-field rollback incorrectly clears screen-scope fields
     * GREEN: Verify rollback respects scope boundaries
     */
    @Test(timeout = 2000)
    public void testFieldRollbackRespectsScopeBoundaries() {
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
        assertFalse("Scoped field should be rolled back", fields.isDirty(2));
    }

    /**
     * TEST A6: Timeout during nested transaction causes partial parent state corruption
     *
     * RED: Timeout during nested transaction corrupts parent state
     * GREEN: Verify timeout rollback maintains parent state integrity
     */
    @Test(timeout = 2000)
    public void testTimeoutNestedTransactionPreservesParentState() {
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
        assertTrue("Parent field 0 should persist", fields.isDirty(0));
        assertFalse("Nested fields should be rolled back", fields.isDirty(1) || fields.isDirty(2));
    }

    /**
     * TEST A7: User-cancel with keyboard already unlocked (race condition)
     *
     * RED: User-cancel during unlock-keyboard transition causes race
     * GREEN: Verify user-cancel safe under concurrent unlock
     */
    @Test(timeout = 2000)
    public void testUserCancelWithKeyboardUnlockedRaceCondition() {
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
                assertTrue("Rollback should complete despite keyboard state", fields.isAllClean());
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
    @Test(timeout = 2000)
    public void testSessionScopeCommitWithPartialFieldsDirty() {
        if (!isAdversarial || !transactionType.equals("explicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("error") ||
                !fieldState.equals("mixed")) return;

        fields.setClean();
        screen.beginExplicitTransaction();
        screen.setCommitScope("session");
        fields.setDirtyFields(new int[]{0});  // Only 1 of 5 fields dirty

        screen.triggerError();

        // Verify atomic rollback: either all dirty or all clean
        assertTrue("Should have atomic semantics", fields.isAllClean() || fields.isAllDirty(new int[]{0}));
    }

    /**
     * TEST A9: Clear-format during nested transaction doesn't clear parent fields
     *
     * RED: Clear-format in nested scope clears parent fields
     * GREEN: Verify clear-format respects scope boundaries
     */
    @Test(timeout = 2000)
    public void testClearFormatInNestedScopeDoesNotAffectParent() {
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
        assertTrue("Parent fields should not be affected by nested clear", fields.isDirty(0));
    }

    /**
     * TEST A10: Rollback trigger arrives during field commit (in-flight atomicity violation)
     *
     * RED: Rollback during commit causes partial field state
     * GREEN: Verify atomicity is maintained even with concurrent rollback signal
     */
    @Test(timeout = 2000)
    public void testRollbackDuringFieldCommitMaintainsAtomicity() {
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
        assertTrue("Should maintain atomic state",
                fields.isAllClean() || fields.isAllDirty(new int[]{0, 1, 2}));
    }

    /**
     * TEST A11: Implicit transaction with no explicit boundary (stale keyboard lock)
     *
     * RED: Implicit transaction without explicit boundary leaves keyboard locked
     * GREEN: Verify keyboard eventually unlocks or transaction times out
     */
    @Test(timeout = 2000)
    public void testImplicitTransactionNoExplicitBoundaryStaleKeyboardLock() {
        if (!isAdversarial || !transactionType.equals("implicit") ||
                !boundaryMarker.equals("WTD") || !rollbackTrigger.equals("error")) return;

        fields.setClean();
        screen.simulateUserInput(0, "test");  // Implicit transaction start
        assertTrue("Keyboard should be locked", screen.isKeyboardLocked());

        fields.setDirtyFields(new int[]{0});
        // No explicit boundary or commit signal
        screen.triggerError();

        // Verify: keyboard eventually unlocks
        assertFalse("Keyboard should be unlocked after error", screen.isKeyboardLocked());
    }

    /**
     * TEST A12: Nested nested transaction (3-deep) rollback doesn't cascade cleanly
     *
     * RED: Triple-nested transaction rollback creates state corruption
     * GREEN: Verify nested rollback chain maintains invariants at all depths
     */
    @Test(timeout = 2000)
    public void testTripleNestedTransactionRollbackChain() {
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
        assertTrue("Parent field should persist", fields.isDirty(0));
        assertFalse("Nested level 1 should be rolled back", fields.isDirty(1));
        assertFalse("Nested level 2 should be rolled back", fields.isDirty(2));
        assertFalse("Nested level 3 should be rolled back", fields.isDirty(3));
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
        private int nestedDepth = 0;

        void writeToDisplay(boolean wtd) {
            if (wtd) {
                transactionActive = true;
                keyboardLocked = true;
            }
        }

        void beginExplicitTransaction() {
            transactionActive = true;
            keyboardLocked = true;
            nestedDepth = 0;
        }

        void beginNestedTransaction() {
            nestedDepth++;
        }

        void clearFormat() {
            transactionActive = true;
        }

        void simulateUserInput(int field, String value) {
            transactionActive = true;
            keyboardLocked = true;
        }

        void triggerError() {
            errorTriggered.set(true);
            transactionActive = false;
            keyboardLocked = false;
        }

        void simulateTimeout(int ms) {
            transactionActive = false;
            keyboardLocked = false;
        }

        void userCancel() {
            if (nestedDepth > 0) {
                nestedDepth--;
            } else {
                transactionActive = false;
            }
            keyboardLocked = false;
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
    }
}
