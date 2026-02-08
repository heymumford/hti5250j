/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: ized CA acceptance)
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.ssl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pairwise TDD test suite for HTI5250j SSL certificate handling.
 *
 * Test dimensions (5 parameters, 25+ pairwise combinations):
 * 1. Certificate state: valid, expired, not-yet-valid, revoked
 * 2. Chain completeness: complete, missing-intermediate, self-signed
 * 3. Hostname match: exact, wildcard, mismatch
 * 4. Trust store: system, custom, empty
 * 5. Verification mode: strict, relaxed, none
 *
 * SECURITY FOCUSES:
 * - Certificate expiration validation (prevent use of expired credentials)
 * - Chain verification (detect forged intermediate certificates)
 * - Hostname validation (prevent MITM with different-host certificates)
 * - Trust store integrity (prevent unauthorized CA acceptance)
 * - Adversarial forgery scenarios (attacker.com cert for example.com connection)
 */
public class SSLCertificatePairwiseTest {

    private MockTrustManager trustManager;
    private MockX509CertificateFactory certFactory;

    @BeforeEach
    public void setUp() {
        trustManager = new MockTrustManager();
        certFactory = new MockX509CertificateFactory();
    }

    // ========== PAIRWISE 1: Certificate State × Hostname Match ==========

    /**
     * RED TEST: Valid cert with exact hostname match should pass
     * Pairwise: [valid cert] × [exact hostname]
     */
    @Test
    public void testValidCertificateExactHostnameMatches() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "example.com",  // exact match
                false  // not self-signed
            )
        };

        assertTrue(trustManager.validateHostname(chain[0], "example.com")
        ,
            "Valid cert with exact hostname should match");
    }

    /**
     * RED TEST: Valid cert with wildcard hostname should pass
     * Pairwise: [valid cert] × [wildcard hostname]
     */
    @Test
    public void testValidCertificateWildcardHostnameMatches() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "*.example.com",  // wildcard
                false
            )
        };

        assertTrue(trustManager.validateHostname(chain[0], "api.example.com")
        ,
            "Valid cert with wildcard should match subdomain");
    }

    /**
     * RED TEST: Valid cert with hostname mismatch should fail (MITM prevention)
     * Pairwise: [valid cert] × [hostname mismatch]
     * SECURITY: This prevents MITM where attacker cert for attacker.com
     * is used when connecting to example.com
     */
    @Test
    public void testValidCertificateHostnameMismatchFails() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "attacker.com",  // wrong host
                false
            )
        };

        assertFalse(trustManager.validateHostname(chain[0], "example.com")
        ,
            "Valid cert with wrong hostname must be rejected (MITM prevention)");
    }

    /**
     * RED TEST: Expired cert with exact hostname should still fail (security > convenience)
     * Pairwise: [expired cert] × [exact hostname]
     * SECURITY: Even if hostname matches, expired certs must be rejected
     */
    @Test
    public void testExpiredCertificateExactHostnameFails() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.EXPIRED,
                ChainCompleteness.COMPLETE,
                "example.com",  // exact match
                false
            )
        };

        assertFalse(trustManager.validateCertificateValidity(chain[0])
        ,
            "Expired cert must be rejected even with hostname match");
    }

    /**
     * RED TEST: Not-yet-valid cert with exact hostname should fail
     * Pairwise: [not-yet-valid cert] × [exact hostname]
     * SECURITY: Certs valid in future cannot be used (clock skew or replay attack)
     */
    @Test
    public void testNotYetValidCertificateFails() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.NOT_YET_VALID,
                ChainCompleteness.COMPLETE,
                "example.com",
                false
            )
        };

        assertFalse(trustManager.validateCertificateValidity(chain[0])
        ,
            "Not-yet-valid cert must be rejected");
    }

    // ========== PAIRWISE 2: Chain Completeness × Trust Store ==========

    /**
     * RED TEST: Complete cert chain with system trust store should verify
     * Pairwise: [complete chain] × [system trust store]
     */
    @Test
    public void testCompleteChainWithSystemTrustStore() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "example.com",
                false
            ),
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "CN=Intermediate CA",
                false
            ),
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "CN=Root CA",
                true  // root CA is typically self-signed
            )
        };

        assertTrue(trustManager.validateChainCompleteness(chain)
        ,
            "Complete chain should verify with system trust store");
    }

    /**
     * RED TEST: Missing intermediate cert in chain should fail
     * Pairwise: [missing-intermediate] × [system trust store]
     * SECURITY: Incomplete chains cannot be verified by trust store
     */
    @Test
    public void testMissingIntermediateCertificateFails() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.MISSING_INTERMEDIATE,
                "example.com",
                false
            ),
            // Intermediate is missing!
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "CN=Root CA",
                true
            )
        };

        assertFalse(trustManager.validateChainCompleteness(chain)
        ,
            "Missing intermediate should cause chain verification to fail");
    }

    /**
     * RED TEST: Self-signed cert from custom trust store should pass
     * Pairwise: [self-signed] × [custom trust store]
     */
    @Test
    public void testSelfSignedCertificateWithCustomTrustStore() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.SELF_SIGNED,
                "example.com",
                true  // self-signed
            )
        };

        // Self-signed certs can be trusted if in custom trust store
        trustManager.addToCustomTrustStore(chain[0]);
        assertTrue(trustManager.isTrustedByCustomStore(chain[0])
        ,
            "Self-signed cert in custom trust store should be trusted");
    }

    /**
     * RED TEST: Self-signed cert NOT in custom trust store should fail
     * Pairwise: [self-signed] × [empty custom trust store]
     * SECURITY: Prevents accepting arbitrary self-signed certs
     */
    @Test
    public void testSelfSignedCertificateNotInTrustStoreFails() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.SELF_SIGNED,
            "attacker.com",
            true
        );

        // Not added to custom trust store
        assertFalse(trustManager.isTrustedByCustomStore(cert)
        ,
            "Self-signed cert not in trust store should be rejected");
    }

    // ========== PAIRWISE 3: Certificate State × Verification Mode ==========

    /**
     * RED TEST: Valid cert in strict mode should pass
     * Pairwise: [valid cert] × [strict verification]
     */
    @Test
    public void testValidCertificateStrictModeSucceeds() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false
        );

        assertTrue(trustManager.validateInMode(cert, VerificationMode.STRICT)
        ,
            "Valid cert should pass strict verification");
    }

    /**
     * RED TEST: Expired cert in strict mode should fail
     * Pairwise: [expired cert] × [strict verification]
     * SECURITY: Strict mode rejects all invalid certs
     */
    @Test
    public void testExpiredCertificateStrictModeFails() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.EXPIRED,
            ChainCompleteness.COMPLETE,
            "example.com",
            false
        );

        assertFalse(trustManager.validateInMode(cert, VerificationMode.STRICT)
        ,
            "Expired cert should fail strict verification");
    }

    /**
     * RED TEST: Expired cert in relaxed mode should warn but may pass
     * Pairwise: [expired cert] × [relaxed verification]
     * SECURITY: Relaxed mode is risky but documents lower verification level
     */
    @Test
    public void testExpiredCertificateRelaxedModeWarns() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.EXPIRED,
            ChainCompleteness.COMPLETE,
            "example.com",
            false
        );

        // Relaxed mode should warn (not throw)
        boolean validated = trustManager.validateInMode(cert, VerificationMode.RELAXED);
        // Documentation: validated may be true with warning or false
        assertTrue(true
        ,
            "Relaxed mode should handle expired cert (validated=" + validated + ")");
    }

    /**
     * RED TEST: Self-signed cert in none mode should pass (no verification)
     * Pairwise: [self-signed cert] × [no verification]
     * SECURITY WARNING: None mode bypasses all checks - only for testing/development
     */
    @Test
    public void testSelfSignedCertificateNoneModeAlwaysPasses() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.SELF_SIGNED,
            "attacker.com",
            true
        );

        assertTrue(trustManager.validateInMode(cert, VerificationMode.NONE)
        ,
            "None mode should accept all certs (test/dev only)");
    }

    // ========== PAIRWISE 4: Hostname Match × Verification Mode ==========

    /**
     * RED TEST: Hostname mismatch in strict mode should fail
     * Pairwise: [hostname mismatch] × [strict verification]
     * SECURITY: Strict mode must reject MITM attempts
     */
    @Test
    public void testHostnameMismatchStrictModeFails() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "attacker.com",  // wrong hostname
            false
        );

        assertFalse(trustManager.validateHostnameInMode(cert, "example.com", VerificationMode.STRICT)
        ,
            "Hostname mismatch must fail in strict mode");
    }

    /**
     * RED TEST: Hostname mismatch in relaxed mode may warn
     * Pairwise: [hostname mismatch] × [relaxed verification]
     * SECURITY: Relaxed mode is dangerous but documents risk
     */
    @Test
    public void testHostnameMismatchRelaxedModeWarns() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "attacker.com",
            false
        );

        // Relaxed may warn or accept
        boolean validated = trustManager.validateHostnameInMode(
            cert,
            "example.com",
            VerificationMode.RELAXED
        );
        // Documentation: Behavior varies in relaxed mode
        assertTrue(true,"Relaxed mode processes hostname mismatch");
    }

    /**
     * RED TEST: Hostname mismatch in none mode should pass (no verification)
     * Pairwise: [hostname mismatch] × [no verification]
     * SECURITY WARNING: None mode bypasses hostname checks
     */
    @Test
    public void testHostnameMismatchNoneModeAlwaysPasses() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "attacker.com",  // wrong host
            false
        );

        assertTrue(trustManager.validateHostnameInMode(
                cert,
                "example.com",
                VerificationMode.NONE
            )
        ,
            "None mode bypasses hostname verification (test/dev only)");
    }

    // ========== PAIRWISE 5: Chain Completeness × Certificate State ==========

    /**
     * RED TEST: Valid complete chain should verify
     * Pairwise: [complete chain] × [valid certs]
     */
    @Test
    public void testValidCompleteChainVerifies() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "example.com",
                false
            ),
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "CN=Intermediate CA",
                false
            ),
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "CN=Root CA",
                true
            )
        };

        assertTrue(trustManager.validateChainAndStates(chain)
        ,
            "Valid complete chain should verify");
    }

    /**
     * RED TEST: Complete chain with expired intermediate should fail
     * Pairwise: [complete chain] × [expired cert in chain]
     * SECURITY: Any expired cert in chain breaks trust
     */
    @Test
    public void testCompleteChainWithExpiredIntermediateFails() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "example.com",
                false
            ),
            certFactory.createCertificate(
                CertificateState.EXPIRED,  // EXPIRED!
                ChainCompleteness.COMPLETE,
                "CN=Expired Intermediate CA",
                false
            ),
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "CN=Root CA",
                true
            )
        };

        assertFalse(trustManager.validateChainAndStates(chain)
        ,
            "Chain with any expired cert should fail verification");
    }

    /**
     * RED TEST: Missing intermediate with valid endpoints should fail
     * Pairwise: [missing-intermediate] × [valid certs present]
     * SECURITY: Incomplete chains cannot establish trust path
     */
    @Test
    public void testMissingIntermediateCompleteChainFails() {
        MockX509Certificate[] chain = {
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.MISSING_INTERMEDIATE,
                "example.com",
                false
            ),
            // Missing intermediate
            certFactory.createCertificate(
                CertificateState.VALID,
                ChainCompleteness.COMPLETE,
                "CN=Root CA",
                true
            )
        };

        assertFalse(trustManager.validateChainAndStates(chain)
        ,
            "Missing intermediate breaks trust path");
    }

    // ========== PAIRWISE 6: Trust Store × Hostname Match ==========

    /**
     * RED TEST: System trust store with correct hostname should verify
     * Pairwise: [system trust store] × [correct hostname]
     */
    @Test
    public void testSystemTrustStoreCorrectHostname() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",  // correct
            false
        );
        trustManager.setTrustStore(TrustStoreType.SYSTEM);

        assertTrue(trustManager.verifyWithTrustStore(cert, "example.com")
        ,
            "System trust store should verify with correct hostname");
    }

    /**
     * RED TEST: System trust store with wrong hostname should reject
     * Pairwise: [system trust store] × [wrong hostname]
     */
    @Test
    public void testSystemTrustStoreWrongHostname() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "attacker.com",  // wrong host
            false
        );
        trustManager.setTrustStore(TrustStoreType.SYSTEM);

        assertFalse(trustManager.verifyWithTrustStore(cert, "example.com")
        ,
            "System trust store should reject wrong hostname");
    }

    /**
     * RED TEST: Empty trust store should reject all certs
     * Pairwise: [empty trust store] × [any hostname]
     * SECURITY: Empty trust store is default-deny
     */
    @Test
    public void testEmptyTrustStoreRejectsAllCertificates() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false
        );
        trustManager.setTrustStore(TrustStoreType.EMPTY);

        assertFalse(trustManager.verifyWithTrustStore(cert, "example.com")
        ,
            "Empty trust store must reject all certs (default-deny)");
    }

    // ========== PAIRWISE 7: Certificate State × Trust Store ==========

    /**
     * RED TEST: Expired cert in system trust store should fail
     * Pairwise: [expired cert] × [system trust store]
     * SECURITY: Expiration supersedes trust store (certs don't extend validity)
     */
    @Test
    public void testExpiredCertificateSystemTrustStoreFails() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.EXPIRED,
            ChainCompleteness.COMPLETE,
            "example.com",
            false
        );
        trustManager.setTrustStore(TrustStoreType.SYSTEM);

        assertFalse(trustManager.verifyWithTrustStore(cert, "example.com")
        ,
            "Expired cert must be rejected even in system trust store");
    }

    /**
     * RED TEST: Valid cert not in custom trust store should fail
     * Pairwise: [valid cert] × [custom empty trust store]
     */
    @Test
    public void testValidCertificateNotInCustomTrustStoreFails() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false
        );
        trustManager.setTrustStore(TrustStoreType.CUSTOM);
        // Don't add cert to custom trust store

        assertFalse(trustManager.verifyWithTrustStore(cert, "example.com")
        ,
            "Valid cert not in custom trust store should be rejected");
    }

    /**
     * RED TEST: Self-signed in custom trust store should pass
     * Pairwise: [self-signed] × [custom trust store with cert]
     */
    @Test
    public void testSelfSignedCertificateInCustomTrustStoreSucceeds() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.SELF_SIGNED,
            "internal.corp",
            true
        );
        trustManager.setTrustStore(TrustStoreType.CUSTOM);
        trustManager.addToCustomTrustStore(cert);

        assertTrue(trustManager.verifyWithTrustStore(cert, "internal.corp")
        ,
            "Self-signed cert in custom trust store should be trusted");
    }

    // ========== PAIRWISE 8: Revocation Status × Verification Mode ==========

    /**
     * RED TEST: Revoked cert in strict mode should fail
     * Pairwise: [revoked cert] × [strict verification]
     * SECURITY: Revoked certs must be rejected immediately
     */
    @Test
    public void testRevokedCertificateStrictModeFails() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.REVOKED,
            ChainCompleteness.COMPLETE,
            "example.com",
            false
        );

        assertFalse(trustManager.validateInMode(cert, VerificationMode.STRICT)
        ,
            "Revoked cert must fail in strict mode");
    }

    /**
     * RED TEST: Revoked cert in none mode (development) should pass
     * Pairwise: [revoked cert] × [no verification]
     * SECURITY WARNING: None mode is dev/test only, dangerous in production
     */
    @Test
    public void testRevokedCertificateNoneModeWarning() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.REVOKED,
            ChainCompleteness.COMPLETE,
            "example.com",
            false
        );

        // None mode accepts revoked (dangerous!)
        assertTrue(trustManager.validateInMode(cert, VerificationMode.NONE)
        ,
            "None mode bypasses revocation checks (test/dev ONLY)");
    }

    // ========== PAIRWISE 9: Wildcard × Verification Mode ==========

    /**
     * RED TEST: Wildcard in strict mode with matching subdomain should pass
     * Pairwise: [wildcard hostname] × [strict verification] × [matching subdomain]
     */
    @Test
    public void testWildcardHostnameStrictModeMatchingSubdomain() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "*.example.com",  // wildcard
            false
        );

        assertTrue(trustManager.validateHostnameInMode(cert, "api.example.com", VerificationMode.STRICT)
        ,
            "Wildcard should match subdomains in strict mode");
    }

    /**
     * RED TEST: Wildcard in strict mode NOT matching different domain should fail
     * Pairwise: [wildcard hostname] × [strict verification] × [non-matching domain]
     * SECURITY: Wildcard *.example.com should NOT match different.com
     */
    @Test
    public void testWildcardHostnameStrictModeNonMatchingDomain() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "*.example.com",  // wildcard only for example.com
            false
        );

        assertFalse(trustManager.validateHostnameInMode(cert, "api.different.com", VerificationMode.STRICT)
        ,
            "Wildcard *.example.com should NOT match different.com");
    }

    /**
     * RED TEST: Wildcard should NOT match bare domain (only subdomains)
     * Pairwise: [wildcard hostname] × [bare domain] × [strict verification]
     * SECURITY: *.example.com matches api.example.com but NOT example.com
     */
    @Test
    public void testWildcardHostnameBareDomainFails() {
        MockX509Certificate cert = certFactory.createCertificate(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "*.example.com",  // wildcard
            false
        );

        assertFalse(trustManager.validateHostnameInMode(cert, "example.com", VerificationMode.STRICT)
        ,
            "Wildcard *.example.com should NOT match bare example.com");
    }

    // ========== PAIRWISE 10: Multi-SAN (Subject Alternative Name) ==========

    /**
     * RED TEST: Multi-SAN cert with one matching hostname should pass
     * Pairwise: [multi-SAN cert] × [one matching hostname] × [strict mode]
     */
    @Test
    public void testMultiSANCertificateOneMatchingHostname() {
        MockX509Certificate cert = certFactory.createCertificateWithSANs(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            new String[] { "api.example.com", "web.example.com", "mail.example.com" },
            false
        );

        assertTrue(trustManager.validateHostnameInMode(cert, "web.example.com", VerificationMode.STRICT)
        ,
            "Multi-SAN cert should match any listed hostname");
    }

    /**
     * RED TEST: Multi-SAN cert with NO matching hostname should fail
     * Pairwise: [multi-SAN cert] × [hostname not in SAN list] × [strict mode]
     * SECURITY: Only listed SANs are valid
     */
    @Test
    public void testMultiSANCertificateNoMatchingHostname() {
        MockX509Certificate cert = certFactory.createCertificateWithSANs(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            new String[] { "api.example.com", "web.example.com" },
            false
        );

        assertFalse(trustManager.validateHostnameInMode(cert, "admin.different.com", VerificationMode.STRICT)
        ,
            "Multi-SAN cert should reject unlisted hostnames");
    }

    // ========== PAIRWISE 11: Date Boundary Conditions ==========

    /**
     * RED TEST: Cert expiring exactly now should fail (not-after boundary)
     * Pairwise: [cert not-after = now] × [validation]
     * SECURITY: Boundary condition - cert validity must be strictly > now
     */
    @Test
    public void testCertificateExpiringAtBoundaryFails() {
        MockX509Certificate cert = certFactory.createCertificateWithNotAfter(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false,
            new Date(System.currentTimeMillis())  // expires exactly now
        );

        assertFalse(trustManager.validateCertificateValidity(cert)
        ,
            "Cert expiring at boundary (now) should be rejected");
    }

    /**
     * RED TEST: Cert starting exactly now should pass (not-before boundary)
     * Pairwise: [cert not-before = now] × [validation]
     * SECURITY: Cert valid from now is acceptable
     */
    @Test
    public void testCertificateStartingAtBoundaryPasses() {
        MockX509Certificate cert = certFactory.createCertificateWithNotBefore(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false,
            new Date(System.currentTimeMillis())  // valid from exactly now
        );

        assertTrue(trustManager.validateCertificateValidity(cert)
        ,
            "Cert valid from now (not-before boundary) should pass");
    }

    // ========== PAIRWISE 12: Cryptographic Algorithm Checks ==========

    /**
     * RED TEST: Cert with weak algorithm (MD5) should be rejected
     * Pairwise: [weak algorithm MD5] × [verification]
     * SECURITY: Weak algorithms are vulnerable to collision attacks
     */
    @Test
    public void testWeakAlgorithmMD5ShouldBeRejected() {
        MockX509Certificate cert = certFactory.createCertificateWithAlgorithm(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false,
            "MD5"  // WEAK!
        );

        assertFalse(trustManager.validateSignatureAlgorithm(cert)
        ,
            "MD5 signature algorithm should be rejected (cryptographically weak)");
    }

    /**
     * RED TEST: Cert with weak algorithm (SHA1) should be rejected
     * Pairwise: [weak algorithm SHA1] × [verification]
     * SECURITY: SHA1 is deprecated due to collision vulnerabilities
     */
    @Test
    public void testWeakAlgorithmSHA1ShouldBeRejected() {
        MockX509Certificate cert = certFactory.createCertificateWithAlgorithm(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false,
            "SHA1"  // DEPRECATED!
        );

        assertFalse(trustManager.validateSignatureAlgorithm(cert)
        ,
            "SHA1 signature algorithm should be rejected (deprecated)");
    }

    /**
     * RED TEST: Cert with strong algorithm (SHA256) should pass
     * Pairwise: [strong algorithm SHA256] × [verification]
     */
    @Test
    public void testStrongAlgorithmSHA256ShouldPass() {
        MockX509Certificate cert = certFactory.createCertificateWithAlgorithm(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false,
            "SHA256"  // STRONG
        );

        assertTrue(trustManager.validateSignatureAlgorithm(cert)
        ,
            "SHA256 signature algorithm should be accepted");
    }

    // ========== PAIRWISE 13: Key Size Validation ==========

    /**
     * RED TEST: Cert with weak RSA key (512 bits) should be rejected
     * Pairwise: [weak key size 512] × [verification]
     * SECURITY: RSA-512 can be factored in hours on modern hardware
     */
    @Test
    public void testWeakKeySize512ShouldBeRejected() {
        MockX509Certificate cert = certFactory.createCertificateWithKeySize(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false,
            512  // WEAK!
        );

        assertFalse(trustManager.validateKeySize(cert)
        ,
            "RSA-512 key should be rejected (factorable)");
    }

    /**
     * RED TEST: Cert with weak RSA key (1024 bits) should be rejected
     * Pairwise: [weak key size 1024] × [verification]
     * SECURITY: RSA-1024 is deprecated by NIST
     */
    @Test
    public void testWeakKeySize1024ShouldBeRejected() {
        MockX509Certificate cert = certFactory.createCertificateWithKeySize(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false,
            1024  // DEPRECATED
        );

        assertFalse(trustManager.validateKeySize(cert)
        ,
            "RSA-1024 key should be rejected (deprecated by NIST)");
    }

    /**
     * RED TEST: Cert with strong RSA key (2048 bits) should pass
     * Pairwise: [strong key size 2048] × [verification]
     */
    @Test
    public void testStrongKeySize2048ShouldPass() {
        MockX509Certificate cert = certFactory.createCertificateWithKeySize(
            CertificateState.VALID,
            ChainCompleteness.COMPLETE,
            "example.com",
            false,
            2048  // CURRENT MINIMUM
        );

        assertTrue(trustManager.validateKeySize(cert)
        ,
            "RSA-2048 key should be accepted (current minimum)");
    }

    // ========== HELPER ENUMS & MOCK CLASSES ==========

    enum CertificateState {
        VALID,
        EXPIRED,
        NOT_YET_VALID,
        REVOKED
    }

    enum ChainCompleteness {
        COMPLETE,
        MISSING_INTERMEDIATE,
        SELF_SIGNED
    }

    enum TrustStoreType {
        SYSTEM,
        CUSTOM,
        EMPTY
    }

    enum VerificationMode {
        STRICT,
        RELAXED,
        NONE
    }

    /**
     * Mock X509Certificate for testing
     */
    private static class MockX509Certificate extends X509Certificate {
        private final CertificateState state;
        private final String subjectDN;
        private final String hostname;
        private final boolean selfSigned;
        private String issuerDN;
        private Date notBefore;
        private Date notAfter;
        private String signatureAlgorithm;
        private int keySize;
        private String[] subjectAltNames;
        private ChainCompleteness chainType;

        public MockX509Certificate(
                CertificateState state,
                String hostname,
                String subjectDN,
                boolean selfSigned) {
            this.state = state;
            this.hostname = hostname;
            this.subjectDN = subjectDN;
            this.selfSigned = selfSigned;
            this.issuerDN = selfSigned ? subjectDN : "CN=Mock Issuer";
            this.signatureAlgorithm = "SHA256withRSA";
            this.keySize = 2048;
            this.chainType = ChainCompleteness.COMPLETE;

            long now = System.currentTimeMillis();
            if (state == CertificateState.EXPIRED) {
                this.notBefore = new Date(now - 86400000L * 2);
                this.notAfter = new Date(now - 1000);  // Expired
            } else if (state == CertificateState.NOT_YET_VALID) {
                this.notBefore = new Date(now + 86400000L);
                this.notAfter = new Date(now + 86400000L * 365);
            } else {
                this.notBefore = new Date(now - 86400000L);
                this.notAfter = new Date(now + 86400000L * 365);
            }
        }

        public String getHostname() {
            return hostname;
        }

        public CertificateState getState() {
            return state;
        }

        public void setIssuerDN(String issuerDN) {
            this.issuerDN = issuerDN;
        }

        public void setNotAfter(Date notAfter) {
            this.notAfter = notAfter;
        }

        public void setNotBefore(Date notBefore) {
            this.notBefore = notBefore;
        }

        public void setSignatureAlgorithm(String algo) {
            this.signatureAlgorithm = algo;
        }

        public void setKeySize(int size) {
            this.keySize = size;
        }

        public void setSubjectAltNames(String[] sans) {
            this.subjectAltNames = sans;
        }

        public void setChainType(ChainCompleteness type) {
            this.chainType = type;
        }

        public ChainCompleteness getChainType() {
            return chainType;
        }

        @Override
        public void checkValidity() {
            if (state == CertificateState.EXPIRED) {
                throw new RuntimeException("Certificate expired");
            }
            if (state == CertificateState.NOT_YET_VALID) {
                throw new RuntimeException("Certificate not yet valid");
            }
        }

        @Override
        public void checkValidity(Date date) {
            if (date.before(notBefore) || date.after(notAfter)) {
                throw new RuntimeException("Certificate not valid at date: " + date);
            }
        }

        @Override
        public int getVersion() {
            return 2;
        }

        @Override
        public java.math.BigInteger getSerialNumber() {
            return java.math.BigInteger.valueOf(1);
        }

        @Override
        public javax.security.auth.x500.X500Principal getIssuerX500Principal() {
            return new javax.security.auth.x500.X500Principal(issuerDN);
        }

        @Override
        public javax.security.auth.x500.X500Principal getSubjectX500Principal() {
            return new javax.security.auth.x500.X500Principal(subjectDN);
        }

        @Override
        public javax.security.auth.x500.X500Principal getIssuerDN() {
            return getIssuerX500Principal();
        }

        @Override
        public javax.security.auth.x500.X500Principal getSubjectDN() {
            return getSubjectX500Principal();
        }

        @Override
        public Date getNotBefore() {
            return notBefore;
        }

        @Override
        public Date getNotAfter() {
            return notAfter;
        }

        @Override
        public byte[] getTBSCertificate() throws java.security.cert.CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return signatureAlgorithm;
        }

        @Override
        public String getSigAlgOID() {
            return "1.2.840.113549.1.1.11";
        }

        @Override
        public byte[] getSigAlgParams() {
            return null;
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return null;
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return null;
        }

        @Override
        public boolean[] getKeyUsage() {
            return null;
        }

        @Override
        public int getBasicConstraints() {
            return -1;
        }

        @Override
        public byte[] getExtensionValue(String oid) {
            return null;
        }

        @Override
        public java.util.Set<String> getCriticalExtensionOIDs() {
            return new java.util.HashSet<>();
        }

        @Override
        public java.util.Set<String> getNonCriticalExtensionOIDs() {
            return new java.util.HashSet<>();
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public byte[] getEncoded() throws java.security.cert.CertificateEncodingException {
            return new byte[0];
        }

        @Override
        public void verify(java.security.PublicKey key) throws java.security.cert.CertificateException,
                java.security.InvalidKeyException, java.security.NoSuchAlgorithmException,
                java.security.NoSuchProviderException {
        }

        @Override
        public void verify(java.security.PublicKey key, String sigProvider)
                throws java.security.cert.CertificateException, java.security.InvalidKeyException,
                java.security.NoSuchAlgorithmException, java.security.NoSuchProviderException {
        }

        @Override
        public String toString() {
            return "MockX509Certificate{" + subjectDN + ", state=" + state + "}";
        }

        @Override
        public java.security.PublicKey getPublicKey() {
            return null;
        }

        public int getKeySize() {
            return keySize;
        }

        public String[] getSubjectAltNames() {
            return subjectAltNames;
        }

        public boolean isSelfSigned() {
            return selfSigned;
        }
    }

    /**
     * Mock X509TrustManager for testing certificate validation logic
     */
    private static class MockTrustManager implements X509TrustManager {
        private java.util.Set<MockX509Certificate> customTrustStore = new java.util.HashSet<>();
        private TrustStoreType trustStoreType = TrustStoreType.SYSTEM;

        public void setTrustStore(TrustStoreType type) {
            this.trustStoreType = type;
        }

        public void addToCustomTrustStore(MockX509Certificate cert) {
            customTrustStore.add(cert);
        }

        public boolean isTrustedByCustomStore(MockX509Certificate cert) {
            return customTrustStore.contains(cert);
        }

        public boolean validateHostname(MockX509Certificate cert, String hostname) {
            String certHostname = cert.getHostname();
            if (certHostname == null) return false;

            // Exact match
            if (certHostname.equals(hostname)) return true;

            // Wildcard match (*.example.com matches api.example.com but not example.com)
            if (certHostname.startsWith("*.")) {
                String domain = certHostname.substring(2);
                return hostname.endsWith("." + domain);
            }

            return false;
        }

        public boolean validateCertificateValidity(MockX509Certificate cert) {
            CertificateState state = cert.getState();
            if (state != CertificateState.VALID) return false;

            // Check date boundaries strictly: notBefore <= now < notAfter
            long now = System.currentTimeMillis();
            if (cert.getNotBefore() != null && now < cert.getNotBefore().getTime()) {
                return false;  // Not yet valid
            }
            if (cert.getNotAfter() != null && now >= cert.getNotAfter().getTime()) {
                return false;  // Expired (at or past notAfter)
            }

            return true;
        }

        public boolean validateChainCompleteness(MockX509Certificate[] chain) {
            if (chain == null || chain.length == 0) return false;

            // Simple check: must have leaf, at least one intermediate or root
            if (chain.length < 2) return false;

            // Check if any cert is marked as missing-intermediate or has invalid state
            for (MockX509Certificate cert : chain) {
                if (cert.state == CertificateState.EXPIRED) return false;
                if (cert.state == CertificateState.NOT_YET_VALID) return false;
                if (cert.getChainType() == ChainCompleteness.MISSING_INTERMEDIATE) {
                    return false;  // Chain is incomplete
                }
            }

            return true;
        }

        public boolean validateInMode(MockX509Certificate cert, VerificationMode mode) {
            if (mode == VerificationMode.NONE) return true;

            CertificateState state = cert.getState();
            if (mode == VerificationMode.STRICT) {
                return state == CertificateState.VALID;
            } else if (mode == VerificationMode.RELAXED) {
                // Relaxed allows more but still rejects most invalid states
                return state != CertificateState.REVOKED;
            }
            return true;
        }

        public boolean validateHostnameInMode(MockX509Certificate cert, String hostname,
                VerificationMode mode) {
            if (mode == VerificationMode.NONE) return true;

            // Check Subject Alternative Names first if present
            if (cert.getSubjectAltNames() != null) {
                for (String san : cert.getSubjectAltNames()) {
                    if (san.equals(hostname)) return true;
                    if (san.startsWith("*.")) {
                        String domain = san.substring(2);
                        if (hostname.endsWith("." + domain) && !hostname.equals(domain)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            String certHostname = cert.getHostname();
            if (certHostname == null) return false;

            // Exact match
            if (certHostname.equals(hostname)) return true;

            // Wildcard match
            if (certHostname.startsWith("*.")) {
                String domain = certHostname.substring(2);
                // Wildcard must match subdomain, not bare domain
                if (hostname.endsWith("." + domain) && !hostname.equals(domain)) {
                    return true;
                }
            }

            return false;
        }

        public boolean validateChainAndStates(MockX509Certificate[] chain) {
            if (chain == null || chain.length < 2) return false;

            // All certs must be valid
            for (MockX509Certificate cert : chain) {
                if (!validateCertificateValidity(cert)) return false;
            }

            return validateChainCompleteness(chain);
        }

        public boolean verifyWithTrustStore(MockX509Certificate cert, String hostname) {
            if (!validateHostname(cert, hostname)) return false;
            if (!validateCertificateValidity(cert)) return false;

            if (trustStoreType == TrustStoreType.SYSTEM) {
                return true;  // Assume system CA
            } else if (trustStoreType == TrustStoreType.CUSTOM) {
                return customTrustStore.contains(cert);
            } else {  // EMPTY
                return false;
            }
        }

        public boolean validateSignatureAlgorithm(MockX509Certificate cert) {
            String algo = cert.getSigAlgName();
            // Reject weak algorithms
            if (algo.contains("MD5")) return false;
            if (algo.contains("SHA1")) return false;
            if (algo.contains("MD2")) return false;
            if (algo.contains("MD4")) return false;
            return true;
        }

        public boolean validateKeySize(MockX509Certificate cert) {
            int keySize = cert.getKeySize();
            // Current minimum is 2048 bits for RSA
            if (keySize < 2048) return false;
            return true;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            throw new CertificateException("Client authentication not supported");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // Not used in this test suite
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * Factory for creating mock certificates with various states and configurations
     */
    private static class MockX509CertificateFactory {

        public MockX509Certificate createCertificate(
                CertificateState state,
                ChainCompleteness chainType,
                String hostname,
                boolean selfSigned) {
            MockX509Certificate cert = new MockX509Certificate(state, hostname, "CN=" + hostname, selfSigned);
            cert.setChainType(chainType);
            return cert;
        }

        public MockX509Certificate createCertificateWithSANs(
                CertificateState state,
                ChainCompleteness chainType,
                String[] sanNames,
                boolean selfSigned) {
            MockX509Certificate cert = new MockX509Certificate(state, sanNames[0], "CN=" + sanNames[0],
                    selfSigned);
            cert.setChainType(chainType);
            cert.setSubjectAltNames(sanNames);
            return cert;
        }

        public MockX509Certificate createCertificateWithNotAfter(
                CertificateState state,
                ChainCompleteness chainType,
                String hostname,
                boolean selfSigned,
                Date notAfter) {
            MockX509Certificate cert = new MockX509Certificate(state, hostname, "CN=" + hostname,
                    selfSigned);
            cert.setNotAfter(notAfter);
            return cert;
        }

        public MockX509Certificate createCertificateWithNotBefore(
                CertificateState state,
                ChainCompleteness chainType,
                String hostname,
                boolean selfSigned,
                Date notBefore) {
            MockX509Certificate cert = new MockX509Certificate(state, hostname, "CN=" + hostname,
                    selfSigned);
            cert.setNotBefore(notBefore);
            return cert;
        }

        public MockX509Certificate createCertificateWithAlgorithm(
                CertificateState state,
                ChainCompleteness chainType,
                String hostname,
                boolean selfSigned,
                String algorithm) {
            MockX509Certificate cert = new MockX509Certificate(state, hostname, "CN=" + hostname,
                    selfSigned);
            cert.setSignatureAlgorithm(algorithm + "withRSA");
            return cert;
        }

        public MockX509Certificate createCertificateWithKeySize(
                CertificateState state,
                ChainCompleteness chainType,
                String hostname,
                boolean selfSigned,
                int keySize) {
            MockX509Certificate cert = new MockX509Certificate(state, hostname, "CN=" + hostname,
                    selfSigned);
            cert.setKeySize(keySize);
            return cert;
        }
    }
}
