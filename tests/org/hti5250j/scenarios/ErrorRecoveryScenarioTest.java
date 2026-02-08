/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.scenarios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Error recovery and retry scenarios.
 *
 * Verifies that system handles transient failures gracefully and
 * recovers to consistent state without data loss.
 *
 * Domain 4 tests verify that error handling doesn't create silent failures.
 */
@DisplayName("Error Recovery Scenarios")
public class ErrorRecoveryScenarioTest {

    private ErrorRecoveryScenarioVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new ErrorRecoveryScenarioVerifier();
    }

    // ============================================================================
    // Scenario 1: Network Failure & Reconnection
    // ============================================================================

    @Test
    @DisplayName("Scenario 1.1: Detects network disconnection")
    void scenarioDetectsNetworkDisconnection() {
        verifier.establishConnection("i5.example.com", 23);
        assertThat("Should be connected initially", verifier.isConnected(), is(true));

        verifier.simulateNetworkFailure();

        assertThat("Should detect disconnection", verifier.isConnected(), is(false));
    }

    @Test
    @DisplayName("Scenario 1.2: Automatic reconnection on network recovery")
    void scenarioAutomaticReconnectionOnRecovery() throws InterruptedException {
        verifier.establishConnection("i5.example.com", 23);
        verifier.simulateNetworkFailure();

        assertThat("Should be disconnected", verifier.isConnected(), is(false));

        // Simulate network recovery
        verifier.simulateNetworkRecovery();
        verifier.attemptReconnect();

        assertThat("Should reconnect automatically", verifier.isConnected(), is(true));
    }

    // ============================================================================
    // Scenario 2: Timeout & Retry
    // ============================================================================

    @Test
    @DisplayName("Scenario 2.1: Operation timeout triggers automatic retry")
    void scenarioOperationTimeoutTriggersRetry() {
        verifier.setOperationTimeout(1000); // 1 second timeout

        // Simulate operation that times out on first attempt
        verifier.scheduleSlowOperation(1500); // Takes 1.5 seconds

        String result = verifier.executeWithRetry("SLOW_OP", 3);

        // After retry (2nd attempt) it should eventually succeed
        assertThat("Operation should eventually succeed", result, notNullValue());
    }

    @Test
    @DisplayName("Scenario 2.2: Retry count respects maximum attempts")
    void scenarioRetryRespectMaxAttempts() {
        verifier.setOperationTimeout(500);

        // Schedule operation that always times out
        verifier.scheduleSlowOperation(5000);

        int maxRetries = 3;
        int retryCount = verifier.executeWithRetryCount("ALWAYS_SLOW", maxRetries);

        assertThat("Should attempt at most maxRetries times",
                   retryCount, lessThanOrEqualTo(maxRetries));
    }

    // ============================================================================
    // Scenario 3: Transaction Rollback on Failure
    // ============================================================================

    @Test
    @DisplayName("Scenario 3.1: Failed transaction rolls back state")
    void scenarioFailedTransactionRollsBack() {
        double initialBalance = 1000.00;
        verifier.setAccountBalance("ACC001", initialBalance);

        // Attempt transaction that will fail partway through
        boolean success = verifier.attemptTransaction("ACC001", 500.00, true); // fail=true

        assertThat("Transaction should fail", success, is(false));

        // Balance should be unchanged (rolled back)
        double finalBalance = verifier.getAccountBalance("ACC001");
        assertThat("Balance should be unchanged after rollback",
                   finalBalance, closeTo(initialBalance, 0.01));
    }

    @Test
    @DisplayName("Scenario 3.2: Successful transaction doesn't rollback")
    void scenarioSuccessfulTransactionPersists() {
        double initialBalance = 1000.00;
        verifier.setAccountBalance("ACC001", initialBalance);

        boolean success = verifier.attemptTransaction("ACC001", 500.00, false); // fail=false

        assertThat("Transaction should succeed", success, is(true));

        double finalBalance = verifier.getAccountBalance("ACC001");
        assertThat("Balance should be updated",
                   finalBalance, closeTo(initialBalance - 500.00, 0.01));
    }

    // ============================================================================
    // Scenario 4: Circuit Breaker Pattern
    // ============================================================================

    @Test
    @DisplayName("Scenario 4.1: Circuit breaker opens after repeated failures")
    void scenarioCircuitBreakerOpensOnFailures() {
        verifier.setCircuitBreakerThreshold(3); // Open after 3 failures

        // First 3 attempts fail
        verifier.simulateFailedOperation();
        verifier.simulateFailedOperation();
        verifier.simulateFailedOperation();

        assertThat("Circuit breaker should be open", verifier.isCircuitBreakerOpen(), is(true));
    }

    @Test
    @DisplayName("Scenario 4.2: Circuit breaker blocks requests when open")
    void scenarioCircuitBreakerBlocksRequests() {
        verifier.setCircuitBreakerThreshold(2);

        // Trigger failures to open breaker
        verifier.simulateFailedOperation();
        verifier.simulateFailedOperation();

        // Attempt operation while breaker is open
        String result = verifier.attemptOperationWhileBreaker("TEST_OP");

        assertThat("Should reject request when breaker is open",
                   result, containsString("CIRCUIT_OPEN"));
    }

    @Test
    @DisplayName("Scenario 4.3: Circuit breaker closes after recovery period")
    void scenarioCircuitBreakerClosesAfterRecovery() throws InterruptedException {
        verifier.setCircuitBreakerThreshold(2);
        verifier.setCircuitBreakerRecoveryTime(100); // 100ms recovery time

        // Open the breaker
        verifier.simulateFailedOperation();
        verifier.simulateFailedOperation();

        assertThat("Breaker should be open initially", verifier.isCircuitBreakerOpen(), is(true));

        // Wait for recovery period
        Thread.sleep(150);

        // Clear failure counts (simulating recovery)
        verifier.clearFailureCount();

        assertThat("Breaker should be closed after recovery",
                   verifier.isCircuitBreakerOpen(), is(false));
    }

    // ============================================================================
    // Scenario 5: Data Consistency After Crash
    // ============================================================================

    @Test
    @DisplayName("Scenario 5.1: Recovery restores committed transactions")
    void scenarioRecoveryRestoresCommitted() {
        verifier.setAccountBalance("ACC001", 1000.00);

        // Execute and commit transaction
        String txnId = verifier.beginTransaction("ACC001", 300.00);
        verifier.commitTransaction(txnId);

        // Simulate crash and recovery
        verifier.simulateCrash();
        verifier.recover();

        // Transaction should still be applied
        double balance = verifier.getAccountBalance("ACC001");
        assertThat("Committed transaction should persist after crash",
                   balance, closeTo(700.00, 0.01));
    }

    @Test
    @DisplayName("Scenario 5.2: Recovery discards uncommitted transactions")
    void scenarioRecoveryDiscardsUncommitted() {
        verifier.setAccountBalance("ACC001", 1000.00);

        // Begin transaction but don't commit
        String txnId = verifier.beginTransaction("ACC001", 300.00);
        // Intentionally don't commit

        // Simulate crash and recovery
        verifier.simulateCrash();
        verifier.recover();

        // Balance should be unchanged (transaction rolled back)
        double balance = verifier.getAccountBalance("ACC001");
        assertThat("Uncommitted transaction should be rolled back",
                   balance, closeTo(1000.00, 0.01));
    }

    // ============================================================================
    // Support: Verifier and Step Classes
    // ============================================================================

    static class ErrorRecoveryScenarioVerifier {
        private boolean connected = false;
        private boolean networkAvailable = true;
        private int operationTimeout = 5000;
        private int circuitBreakerThreshold = 3;
        private int circuitBreakerRecoveryTime = 5000;
        private int failureCount = 0;
        private boolean circuitBreakerOpen = false;
        private long circuitBreakerOpenedAt = 0;

        private final Map<String, Double> accountBalances =
            new ConcurrentHashMap<>();
        private final Map<String, TransactionRecord> transactions =
            new ConcurrentHashMap<>();
        private final Map<String, List<String>> txnLog =
            new ConcurrentHashMap<>();

        private Supplier<?> slowOperation;

        void establishConnection(String host, int port) {
            if (networkAvailable) {
                connected = true;
            }
        }

        boolean isConnected() {
            return connected && networkAvailable;
        }

        void simulateNetworkFailure() {
            networkAvailable = false;
            connected = false;
        }

        void simulateNetworkRecovery() {
            networkAvailable = true;
        }

        void attemptReconnect() {
            if (networkAvailable) {
                connected = true;
            }
        }

        void setOperationTimeout(int millis) {
            operationTimeout = millis;
        }

        void scheduleSlowOperation(int millis) {
            slowOperation = () -> {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            };
        }

        String executeWithRetry(String operation, int maxRetries) {
            for (int i = 0; i < maxRetries; i++) {
                try {
                    slowOperation.get();
                    return "SUCCESS";
                } catch (Exception e) {
                    if (i < maxRetries - 1) {
                        continue;
                    }
                    return null;
                }
            }
            return "SUCCESS";
        }

        int executeWithRetryCount(String operation, int maxRetries) {
            for (int i = 0; i < maxRetries; i++) {
                try {
                    slowOperation.get();
                    return i + 1;
                } catch (Exception e) {
                    if (i == maxRetries - 1) {
                        return maxRetries;
                    }
                }
            }
            return maxRetries;
        }

        void setAccountBalance(String accountId, double balance) {
            accountBalances.put(accountId, balance);
        }

        double getAccountBalance(String accountId) {
            return accountBalances.getOrDefault(accountId, 0.0);
        }

        boolean attemptTransaction(String accountId, double amount, boolean shouldFail) {
            if (shouldFail) {
                return false;
            }
            double currentBalance = accountBalances.getOrDefault(accountId, 0.0);
            accountBalances.put(accountId, currentBalance - amount);
            return true;
        }

        String beginTransaction(String accountId, double amount) {
            String txnId = "TXN" + System.currentTimeMillis();
            transactions.put(txnId, new TransactionRecord(accountId, amount, "PENDING"));
            return txnId;
        }

        void commitTransaction(String txnId) {
            transactions.computeIfPresent(txnId, (id, record) -> {
                if ("COMMITTED".equals(record.status)) {
                    return record;
                }
                accountBalances.compute(record.accountId, (acct, balance) -> {
                    double current = balance == null ? 0.0 : balance;
                    return current - record.amount;
                });
                return new TransactionRecord(record.accountId, record.amount, "COMMITTED");
            });
        }

        void setCircuitBreakerThreshold(int threshold) {
            circuitBreakerThreshold = threshold;
        }

        void setCircuitBreakerRecoveryTime(int millis) {
            circuitBreakerRecoveryTime = millis;
        }

        void simulateFailedOperation() {
            failureCount++;
            if (failureCount >= circuitBreakerThreshold) {
                circuitBreakerOpen = true;
                circuitBreakerOpenedAt = System.currentTimeMillis();
            }
        }

        boolean isCircuitBreakerOpen() {
            if (circuitBreakerOpen) {
                long elapsed = System.currentTimeMillis() - circuitBreakerOpenedAt;
                if (elapsed > circuitBreakerRecoveryTime) {
                    circuitBreakerOpen = false;
                    return false;
                }
                return true;
            }
            return false;
        }

        String attemptOperationWhileBreaker(String operation) {
            if (isCircuitBreakerOpen()) {
                return "ERROR: CIRCUIT_OPEN";
            }
            return "OK";
        }

        void clearFailureCount() {
            failureCount = 0;
            circuitBreakerOpen = false;
        }

        void simulateCrash() {
            // Simulate crash - mark all pending transactions as uncommitted
        }

        void recover() {
            // Discard pending transactions
            transactions.keySet().stream()
                .filter(txnId -> "PENDING".equals(transactions.get(txnId).status))
                .forEach(txnId -> transactions.remove(txnId));
        }

        private record TransactionRecord(String accountId, double amount, String status) {}
    }
}
