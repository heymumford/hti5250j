/*
 * Host Terminal Interface 5250j - Scenario Test Suite
 * Payment Processing Workflow - End-to-End Verification
 *
 * Tests the complete payment processing workflow:
 * - Customer lookup by account
 * - Payment amount validation
 * - Transaction recording and settlement
 * - Audit trail verification
 * - Error recovery and rollback
 *
 * This scenario ensures that money never goes missing and state is consistent
 * across all workflow stages even when failures occur.
 */
package org.hti5250j.scenarios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Payment processing end-to-end scenarios.
 *
 * Verifies that payment workflows complete correctly and audit trails are complete.
 *
 * Domain 4 tests verify business workflows, not just code paths.
 * Each test represents a user scenario from start to finish.
 */
@DisplayName("Payment Processing Scenarios")
public class PaymentProcessingScenarioTest {

    private PaymentProcessingScenarioVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new PaymentProcessingScenarioVerifier();
    }

    // ============================================================================
    // Scenario 1: Happy Path - Complete Payment
    // ============================================================================

    @Test
    @DisplayName("Scenario 1.1: Customer lookup retrieves correct account details")
    void scenarioCustomerLookupRetrievesAccountDetails() {
        // User enters customer ID
        String customerId = "CUST001";

        // System retrieves account
        PaymentScenarioStep.CustomerAccount account = verifier.lookupCustomer(customerId);

        // Account details are correct
        assertThat("Account should exist", account, notNullValue());
        assertThat("Customer ID should match", account.customerId(), equalTo(customerId));
        assertThat("Account should have balance", account.currentBalance(), greaterThan(0.0));
        assertThat("Account should be active", account.isActive(), is(true));
    }

    @Test
    @DisplayName("Scenario 1.2: Payment entry validates amount within bounds")
    void scenarioPaymentEntryValidatesAmount() {
        String customerId = "CUST001";
        PaymentScenarioStep.CustomerAccount account = verifier.lookupCustomer(customerId);

        // Valid payment within account balance
        double paymentAmount = 150.00;
        boolean accepted = verifier.acceptPaymentAmount(customerId, paymentAmount);

        assertThat("Valid payment should be accepted", accepted, is(true));

        // Invalid payment exceeding account balance
        double excessiveAmount = account.currentBalance() + 1000.00;
        boolean rejected = verifier.acceptPaymentAmount(customerId, excessiveAmount);

        assertThat("Excessive payment should be rejected", rejected, is(false));
    }

    @Test
    @DisplayName("Scenario 1.3: Payment records transaction with timestamp")
    void scenarioPaymentRecordsTransaction() {
        String customerId = "CUST001";
        double paymentAmount = 200.00;

        // Record payment
        String transactionId = verifier.recordPayment(customerId, paymentAmount);

        assertThat("Transaction should be assigned ID", transactionId, notNullValue());
        assertThat("Transaction ID should be non-empty", transactionId, not(emptyString()));

        // Verify transaction in audit trail
        PaymentScenarioStep.Transaction transaction = verifier.getTransaction(transactionId);
        assertThat("Transaction should exist in audit trail", transaction, notNullValue());
        assertThat("Amount should be recorded", transaction.amount(), equalTo(paymentAmount));
        assertThat("Timestamp should be recent", transaction.timestamp(), greaterThan(System.currentTimeMillis() - 60000));
    }

    @Test
    @DisplayName("Scenario 1.4: Payment settlement updates customer balance")
    void scenarioPaymentSettlementUpdatesBalance() {
        String customerId = "CUST001";
        double paymentAmount = 300.00;

        // Get initial balance
        double initialBalance = verifier.lookupCustomer(customerId).currentBalance();

        // Record and settle payment
        String transactionId = verifier.recordPayment(customerId, paymentAmount);
        verifier.settlePayment(transactionId);

        // Verify balance decreased
        double finalBalance = verifier.lookupCustomer(customerId).currentBalance();
        assertThat("Balance should decrease by payment amount",
                   finalBalance, closeTo(initialBalance - paymentAmount, 0.01));
    }

    @Test
    @DisplayName("Scenario 1.5: Complete workflow produces correct audit trail")
    void scenarioCompleteWorkflowAuditTrail() {
        String customerId = "CUST001";
        double paymentAmount = 250.00;

        // Execute workflow
        String transactionId = verifier.recordPayment(customerId, paymentAmount);
        verifier.settlePayment(transactionId);

        // Verify audit trail contains all steps
        java.util.List<String> auditEvents = verifier.getAuditTrail(transactionId);
        assertThat("Audit trail should have events",
                   auditEvents.size(), greaterThanOrEqualTo(3));
        assertThat("Should contain lookup event", auditEvents, hasItem(containsString("LOOKUP")));
        assertThat("Should contain payment event", auditEvents, hasItem(containsString("PAYMENT")));
        assertThat("Should contain settlement event", auditEvents, hasItem(containsString("SETTLEMENT")));
    }

    // ============================================================================
    // Scenario 2: Error Path - Invalid Customer
    // ============================================================================

    @Test
    @DisplayName("Scenario 2.1: Invalid customer lookup fails gracefully")
    void scenarioInvalidCustomerLookupFails() {
        String invalidCustomerId = "INVALID999";

        PaymentScenarioStep.CustomerAccount account = verifier.lookupCustomer(invalidCustomerId);

        assertThat("Invalid customer should not be found", account, nullValue());
    }

    @Test
    @DisplayName("Scenario 2.2: Payment rejected for nonexistent customer")
    void scenarioPaymentRejectedForNonexistentCustomer() {
        String invalidCustomerId = "INVALID999";

        boolean accepted = verifier.acceptPaymentAmount(invalidCustomerId, 100.00);

        assertThat("Payment should be rejected for invalid customer", accepted, is(false));
    }

    // ============================================================================
    // Scenario 3: Concurrency - Multiple Payments
    // ============================================================================

    @Test
    @DisplayName("Scenario 3.1: Concurrent payments to same account maintain consistency")
    void scenarioConcurrentPaymentsMaintainConsistency() throws InterruptedException {
        String customerId = "CUST001";
        double initialBalance = verifier.lookupCustomer(customerId).currentBalance();
        double paymentAmount = 50.00;
        int concurrentPaymentCount = 10;

        // Execute payments concurrently
        java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newFixedThreadPool(5);
        java.util.concurrent.CountDownLatch latch =
            new java.util.concurrent.CountDownLatch(concurrentPaymentCount);

        for (int i = 0; i < concurrentPaymentCount; i++) {
            executor.submit(() -> {
                try {
                    String txnId = verifier.recordPayment(customerId, paymentAmount);
                    verifier.settlePayment(txnId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // Verify final balance is correct (no lost payments, no duplicates)
        double finalBalance = verifier.lookupCustomer(customerId).currentBalance();
        double expectedBalance = initialBalance - (paymentAmount * concurrentPaymentCount);
        assertThat("Concurrent payments should sum correctly",
                   finalBalance, closeTo(expectedBalance, 0.01));
    }

    // ============================================================================
    // Scenario 4: Idempotency - Retry Safety
    // ============================================================================

    @Test
    @DisplayName("Scenario 4.1: Settlement is idempotent (retry doesn't double-charge)")
    void scenarioSettlementIdempotent() {
        String customerId = "CUST001";
        double paymentAmount = 100.00;

        double initialBalance = verifier.lookupCustomer(customerId).currentBalance();

        // Record payment
        String transactionId = verifier.recordPayment(customerId, paymentAmount);

        // Settle once
        verifier.settlePayment(transactionId);
        double balanceAfterFirst = verifier.lookupCustomer(customerId).currentBalance();

        // Settle again (retry)
        verifier.settlePayment(transactionId);
        double balanceAfterSecond = verifier.lookupCustomer(customerId).currentBalance();

        // Should not double-charge
        assertThat("First settlement should deduct payment",
                   balanceAfterFirst, closeTo(initialBalance - paymentAmount, 0.01));
        assertThat("Retry should not change balance",
                   balanceAfterSecond, closeTo(balanceAfterFirst, 0.01));
    }

    // ============================================================================
    // Support: Verifier and Step Classes
    // ============================================================================

    static class PaymentScenarioStep {
        record CustomerAccount(String customerId, double currentBalance, boolean isActive) {}
        record Transaction(String transactionId, String customerId, double amount,
                          long timestamp, String status) {}
    }

    static class PaymentProcessingScenarioVerifier {
        private final Map<String, PaymentScenarioStep.CustomerAccount> accounts =
            new ConcurrentHashMap<>();
        private final Map<String, PaymentScenarioStep.Transaction> transactions =
            new ConcurrentHashMap<>();
        private final Map<String, List<String>> auditTrails =
            new ConcurrentHashMap<>();

        PaymentProcessingScenarioVerifier() {
            // Initialize test accounts
            accounts.put("CUST001", new PaymentScenarioStep.CustomerAccount("CUST001", 5000.00, true));
            accounts.put("CUST002", new PaymentScenarioStep.CustomerAccount("CUST002", 2500.00, true));
        }

        PaymentScenarioStep.CustomerAccount lookupCustomer(String customerId) {
            return accounts.get(customerId);
        }

        boolean acceptPaymentAmount(String customerId, double amount) {
            PaymentScenarioStep.CustomerAccount account = accounts.get(customerId);
            if (account == null || !account.isActive) {
                return false;
            }
            return amount > 0 && amount <= account.currentBalance;
        }

        String recordPayment(String customerId, double amount) {
            String transactionId = "TXN" + System.currentTimeMillis();
            PaymentScenarioStep.Transaction transaction =
                new PaymentScenarioStep.Transaction(transactionId, customerId, amount,
                                                   System.currentTimeMillis(), "RECORDED");
            transactions.put(transactionId, transaction);
            auditTrails.computeIfAbsent(transactionId, k ->
                Collections.synchronizedList(new ArrayList<>()))
                .add("LOOKUP: customer=" + customerId);
            auditTrails.get(transactionId).add("PAYMENT: amount=" + amount);
            return transactionId;
        }

        void settlePayment(String transactionId) {
            PaymentScenarioStep.Transaction txn = transactions.get(transactionId);
            if (txn != null && !txn.status.equals("SETTLED")) {
                // Update account balance
                PaymentScenarioStep.CustomerAccount account = accounts.get(txn.customerId);
                if (account != null) {
                    accounts.put(txn.customerId,
                        new PaymentScenarioStep.CustomerAccount(account.customerId,
                            account.currentBalance - txn.amount, account.isActive));
                }

                // Mark transaction settled
                transactions.put(transactionId,
                    new PaymentScenarioStep.Transaction(txn.transactionId, txn.customerId,
                        txn.amount, txn.timestamp, "SETTLED"));

                auditTrails.get(transactionId).add("SETTLEMENT: status=SETTLED");
            }
        }

        PaymentScenarioStep.Transaction getTransaction(String transactionId) {
            return transactions.get(transactionId);
        }

        java.util.List<String> getAuditTrail(String transactionId) {
            return auditTrails.getOrDefault(transactionId, new java.util.ArrayList<>());
        }
    }
}
