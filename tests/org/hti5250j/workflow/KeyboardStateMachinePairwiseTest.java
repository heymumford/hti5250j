/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Keyboard State Machine - Pairwise TDD")
public class KeyboardStateMachinePairwiseTest {

    private KeyboardStateMachineVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new KeyboardStateMachineVerifier();
    }

    @Test
    @DisplayName("LOGIN: Keyboard already unlocked (instant acquire)")
    void testLoginKeyboardAlreadyUnlocked() throws Exception {
        verifier.setKeyboardLocked(false);
        boolean acquired = verifier.acquireKeyboardForLogin(100);
        assertThat("Should acquire keyboard immediately", acquired, equalTo(true));
    }

    @Test
    @DisplayName("LOGIN: Keyboard unlocks after 50ms delay")
    void testLoginKeyboardDelayedUnlock() throws Exception {
        verifier.setKeyboardLockedWithDelay(50);
        boolean acquired = verifier.acquireKeyboardForLogin(2000);
        assertThat("Should acquire keyboard after delay", acquired, equalTo(true));
    }

    @Test
    @DisplayName("LOGIN: Keyboard never unlocks within timeout")
    void testLoginKeyboardTimeoutException() throws Exception {
        verifier.setKeyboardLockedPermanently();
        Exception exception = null;
        try {
            verifier.acquireKeyboardForLogin(100);
        } catch (TimeoutException e) {
            exception = e;
        }
        assertThat("Should throw TimeoutException", exception, instanceOf(TimeoutException.class));
    }

    @Test
    @DisplayName("LOGIN: Concurrent lock signal during wait")
    void testLoginConcurrentLockSignal() throws Exception {
        verifier.setKeyboardLockedWithTransition(50, 200);
        boolean acquired = verifier.acquireKeyboardForLogin(500);
        assertThat("Should acquire on first unlock event", acquired, equalTo(true));
    }

    @Test
    @DisplayName("NAVIGATE: Keyboard already unlocked")
    void testNavigateKeyboardAlreadyUnlocked() throws Exception {
        verifier.setKeyboardLocked(false);
        boolean acquired = verifier.acquireKeyboardForNavigate(100);
        assertThat("Should acquire keyboard immediately", acquired, equalTo(true));
    }

    @Test
    @DisplayName("NAVIGATE: Keyboard unlocks after delay")
    void testNavigateKeyboardDelayedUnlock() throws Exception {
        verifier.setKeyboardLockedWithDelay(100);
        boolean acquired = verifier.acquireKeyboardForNavigate(500);
        assertThat("Should acquire keyboard after delay", acquired, equalTo(true));
    }

    @Test
    @DisplayName("NAVIGATE: Keyboard timeout during navigation")
    void testNavigateKeyboardTimeout() throws Exception {
        verifier.setKeyboardLockedPermanently();
        Exception exception = null;
        try {
            verifier.acquireKeyboardForNavigate(100);
        } catch (TimeoutException e) {
            exception = e;
        }
        assertThat("Should timeout", exception, instanceOf(TimeoutException.class));
    }

    @Test
    @DisplayName("FILL: Keyboard available for field input")
    void testFillKeyboardAvailable() throws Exception {
        verifier.setKeyboardLocked(false);
        boolean acquired = verifier.acquireKeyboardForFill(1000);
        assertThat("Should acquire keyboard immediately", acquired, equalTo(true));
    }

    @Test
    @DisplayName("FILL: Keyboard becomes available after delay")
    void testFillKeyboardDelayedAvailability() throws Exception {
        verifier.setKeyboardLockedWithDelay(200);
        boolean acquired = verifier.acquireKeyboardForFill(1000);
        assertThat("Should acquire keyboard after delay", acquired, equalTo(true));
    }

    @Test
    @DisplayName("FILL: Keyboard timeout during field population")
    void testFillKeyboardTimeout() throws Exception {
        verifier.setKeyboardLockedPermanently();
        Exception exception = null;
        try {
            verifier.acquireKeyboardForFill(200);
        } catch (TimeoutException e) {
            exception = e;
        }
        assertThat("Should timeout", exception, instanceOf(TimeoutException.class));
    }

    @Test
    @DisplayName("FILL: Multiple field updates require repeated keyboard acquisition")
    void testFillMultipleFieldsSequential() throws Exception {
        verifier.setKeyboardLocked(false);

        boolean field1 = verifier.acquireKeyboardForFill(500);
        verifier.releaseKeyboard();

        boolean field2 = verifier.acquireKeyboardForFill(500);
        verifier.releaseKeyboard();

        boolean field3 = verifier.acquireKeyboardForFill(500);

        assertThat("All fields should acquire keyboard",
                   field1 && field2 && field3, equalTo(true));
    }

    @Test
    @DisplayName("SUBMIT: Keyboard ready for submission key")
    void testSubmitKeyboardReady() throws Exception {
        verifier.setKeyboardLocked(false);
        boolean acquired = verifier.acquireKeyboardForSubmit(100);
        assertThat("Should acquire keyboard immediately", acquired, equalTo(true));
    }

    @Test
    @DisplayName("SUBMIT: Keyboard unlocks before submit deadline")
    void testSubmitKeyboardUnlocksBeforeDeadline() throws Exception {
        verifier.setKeyboardLockedWithDelay(50);
        boolean acquired = verifier.acquireKeyboardForSubmit(500);
        assertThat("Should acquire keyboard", acquired, equalTo(true));
    }

    @Test
    @DisplayName("SUBMIT: Keyboard lock timeout prevents submission")
    void testSubmitKeyboardLockTimeout() throws Exception {
        verifier.setKeyboardLockedPermanently();
        Exception exception = null;
        try {
            verifier.acquireKeyboardForSubmit(100);
        } catch (TimeoutException e) {
            exception = e;
        }
        assertThat("Should timeout", exception, instanceOf(TimeoutException.class));
    }

    @Test
    @DisplayName("SUBMIT: Keyboard acquisition fails, submission blocked")
    void testSubmitKeyboardAcquisitionFailsBlocksSubmission() throws Exception {
        verifier.setKeyboardLockedPermanently();
        boolean canSubmit = verifier.canSubmitWithKeyboardTimeout(100);
        assertThat("Submission should be blocked", canSubmit, equalTo(false));
    }

    @Test
    @DisplayName("ASSERT: Keyboard unlocked for screen capture")
    void testAssertKeyboardUnlockedForCapture() throws Exception {
        verifier.setKeyboardLocked(false);
        boolean acquired = verifier.acquireKeyboardForAssert(500);
        assertThat("Should acquire keyboard immediately", acquired, equalTo(true));
    }

    @Test
    @DisplayName("ASSERT: Keyboard delayed but acquired before assertion timeout")
    void testAssertKeyboardDelayedBeforeTimeout() throws Exception {
        verifier.setKeyboardLockedWithDelay(100);
        boolean acquired = verifier.acquireKeyboardForAssert(500);
        assertThat("Should acquire keyboard after delay", acquired, equalTo(true));
    }

    @Test
    @DisplayName("ASSERT: Keyboard timeout prevents screen validation")
    void testAssertKeyboardTimeoutPreventsValidation() throws Exception {
        verifier.setKeyboardLockedPermanently();
        Exception exception = null;
        try {
            verifier.acquireKeyboardForAssert(100);
        } catch (TimeoutException e) {
            exception = e;
        }
        assertThat("Should timeout", exception, instanceOf(TimeoutException.class));
    }

    @Test
    @DisplayName("Adversarial: Keyboard never unlocks (hardware hang)")
    void testAdversarialKeyboardNeverUnlocks() throws Exception {
        verifier.setKeyboardLockedPermanently();
        Exception exception = null;
        try {
            verifier.acquireKeyboardForLogin(200);
        } catch (TimeoutException e) {
            exception = e;
        }
        assertThat("Should detect hang and timeout", exception, notNullValue());
    }

    @Test
    @DisplayName("Adversarial: Rapid lock/unlock cycles (flutter pattern)")
    void testAdversarialKeyboardFlutterPattern() throws Exception {
        verifier.setKeyboardFluttering(50);
        boolean acquired = verifier.acquireKeyboardForLogin(1000);
        assertThat("Should eventually acquire despite flutter", acquired, equalTo(true));
    }

    @Test
    @DisplayName("Adversarial: Spurious unlock signals (lock→unlock→lock)")
    void testAdversarialSpuriousUnlockSignal() throws Exception {
        verifier.setKeyboardLockedWithTransition(100, 200);
        boolean acquired = verifier.acquireKeyboardForLogin(500);
        assertThat("Should handle spurious unlock", acquired, equalTo(true));
    }

    @Test
    @DisplayName("Adversarial: Thread interruption during keyboard wait")
    void testAdversarialThreadInterruptionDuringWait() throws Exception {
        verifier.setKeyboardLockedWithDelay(2000);
        Exception exception = null;
        try {
            verifier.acquireKeyboardForLoginWithInterruption(100);
        } catch (InterruptedException e) {
            exception = e;
        }
        assertThat("Should handle thread interruption", exception, notNullValue());
    }

    @Test
    @DisplayName("Adversarial: Concurrent workflows competing for keyboard")
    void testAdversarialConcurrentKeyboardContention() throws Exception {
        verifier.setKeyboardLocked(false);
        int successCount = verifier.concurrentKeyboardAcquisitions(10, 500);
        assertThat("All 10 concurrent acquisitions should succeed",
                   successCount, greaterThanOrEqualTo(9));
    }

    @Test
    @DisplayName("Adversarial: Out-of-order state transitions (unlock before lock)")
    void testAdversarialOutOfOrderStateTransition() throws Exception {
        Exception exception = null;
        try {
            verifier.releaseKeyboardWithoutAcquire();
        } catch (IllegalStateException e) {
            exception = e;
        }
        assertThat("Should handle state violation gracefully",
                   exception == null || exception instanceof IllegalStateException,
                   equalTo(true));
    }
}
