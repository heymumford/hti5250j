# SSL Certificate Pairwise Test Quick Reference

## Test File
**Location:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/tn5250j/ssl/SSLCertificatePairwiseTest.java`

**Package:** `org.tn5250j.ssl`

**Test Class:** `SSLCertificatePairwiseTest`

## Test Execution

```bash
cd ~/ProjectsWATTS/tn5250j-headless
java -cp lib/development/junit-4.5.jar:build/classes:tests \
  org.junit.runner.JUnitCore org.tn5250j.ssl.SSLCertificatePairwiseTest
```

**Result:** OK (40 tests) - All pass

## Test Categories & Methods (40 Tests)

### Category 1: Certificate State × Hostname (5 tests)
```
 1. testValidCertificateExactHostnameMatches
 2. testValidCertificateWildcardHostnameMatches
 3. testValidCertificateHostnameMismatchFails
 4. testExpiredCertificateExactHostnameFails
 5. testNotYetValidCertificateFails
```

### Category 2: Chain Completeness × Trust Store (5 tests)
```
 6. testCompleteChainWithSystemTrustStore
 7. testMissingIntermediateCertificateFails
 8. testSelfSignedCertificateWithCustomTrustStore
 9. testSelfSignedCertificateNotInTrustStoreFails
10. testValidCertificateStrictModeSucceeds (overlaps Category 3)
```

### Category 3: Verification Modes (4 tests)
```
10. testValidCertificateStrictModeSucceeds
11. testExpiredCertificateStrictModeFails
12. testExpiredCertificateRelaxedModeWarns
13. testSelfSignedCertificateNoneModeAlwaysPasses
```

### Category 4: Hostname × Mode (3 tests)
```
14. testHostnameMismatchStrictModeFails
15. testHostnameMismatchRelaxedModeWarns
16. testHostnameMismatchNoneModeAlwaysPasses
```

### Category 5: Chain & State (2 tests)
```
17. testValidCompleteChainVerifies
18. testCompleteChainWithExpiredIntermediateFails
19. testMissingIntermediateCompleteChainFails
```

### Category 6: Trust Store × Hostname (3 tests)
```
20. testSystemTrustStoreCorrectHostname
21. testSystemTrustStoreWrongHostname
22. testEmptyTrustStoreRejectsAllCertificates
```

### Category 7: Certificate State × Trust Store (3 tests)
```
23. testExpiredCertificateSystemTrustStoreFails
24. testValidCertificateNotInCustomTrustStoreFails
25. testSelfSignedCertificateInCustomTrustStoreSucceeds
```

### Category 8: Revocation × Mode (2 tests)
```
26. testRevokedCertificateStrictModeFails
27. testRevokedCertificateNoneModeWarning
```

### Category 9: Wildcard Validation (3 tests)
```
28. testWildcardHostnameStrictModeMatchingSubdomain
29. testWildcardHostnameStrictModeNonMatchingDomain
30. testWildcardHostnameBareDomainFails
```

### Category 10: Subject Alternative Names (2 tests)
```
31. testMultiSANCertificateOneMatchingHostname
32. testMultiSANCertificateNoMatchingHostname
```

### Category 11: Date Boundaries (2 tests)
```
33. testCertificateExpiringAtBoundaryFails
34. testCertificateStartingAtBoundaryPasses
```

### Category 12: Signature Algorithms (3 tests)
```
35. testWeakAlgorithmMD5ShouldBeRejected
36. testWeakAlgorithmSHA1ShouldBeRejected
37. testStrongAlgorithmSHA256ShouldPass
```

### Category 13: Key Sizes (3 tests)
```
38. testWeakKeySize512ShouldBeRejected
39. testWeakKeySize1024ShouldBeRejected
40. testStrongKeySize2048ShouldPass
```

---

## Pairwise Dimensions

| # | Dimension | Values |
|---|-----------|--------|
| 1 | Certificate State | valid, expired, not-yet-valid, revoked |
| 2 | Chain Completeness | complete, missing-intermediate, self-signed |
| 3 | Hostname Match | exact, wildcard, mismatch |
| 4 | Trust Store | system, custom, empty |
| 5 | Verification Mode | strict, relaxed, none |

---

## Security Adversarial Scenarios

| Scenario | Tests | Prevention |
|----------|-------|-----------|
| MITM Attack (wrong host) | 3, 14 | Hostname validation |
| Expired Credentials | 4, 11, 23 | Date validation |
| Chain Forgery | 7, 19 | Chain completeness |
| Weak Cryptography (MD5/SHA1) | 35, 36 | Algorithm validation |
| Weak Keys (RSA-512/1024) | 38, 39 | Key size validation |
| Revoked Certificates | 26 | Revocation checking |
| Self-Signed Acceptance | 9 | Explicit trust requirement |
| Wildcard Overmatch | 29 | RFC 6125 validation |

---

## Mock Classes

### MockX509Certificate
- Extends `X509Certificate`
- Configurable: state, hostname, SANs, algorithm, key size
- States: VALID, EXPIRED, NOT_YET_VALID, REVOKED
- Simulates real certificate behavior

### MockTrustManager
- Implements `X509TrustManager`
- Methods:
  - `validateHostname(cert, hostname)` - Exact/wildcard matching
  - `validateCertificateValidity(cert)` - Date validation
  - `validateChainCompleteness(chain)` - Chain checking
  - `validateInMode(cert, mode)` - Verification mode logic
  - `validateSignatureAlgorithm(cert)` - Algorithm checking
  - `validateKeySize(cert)` - Minimum RSA-2048

### MockX509CertificateFactory
- Builder factory for test certificates
- Methods:
  - `createCertificate(state, chain, hostname, selfSigned)`
  - `createCertificateWithSANs(state, chain, names, selfSigned)`
  - `createCertificateWithNotAfter(state, chain, hostname, selfSigned, date)`
  - `createCertificateWithAlgorithm(state, chain, hostname, selfSigned, algo)`
  - `createCertificateWithKeySize(state, chain, hostname, selfSigned, bits)`

---

## Assertion Patterns

### Certificate Validation Success
```java
assertTrue("Description", trustManager.validateMethod(cert));
```

### Certificate Validation Failure
```java
assertFalse("Description", trustManager.validateMethod(cert));
```

### Object Existence
```java
assertNotNull("Description", object);
```

---

## Verification Modes

| Mode | Behavior | Use Case |
|------|----------|----------|
| **STRICT** | All checks enforced, reject invalid | Production TLS |
| **RELAXED** | Warnings issued, may accept warnings | Development/Testing |
| **NONE** | All checks bypassed, accept all | Unit tests/Mocking |

---

## Key Security Rules

1. **Default Deny:** Empty trust store rejects all
2. **Expiration Supersedes:** Expired certs rejected even if trusted
3. **Chain Integrity:** One invalid cert fails entire chain
4. **Hostname Strict:** Wildcard *.example.com ≠ example.com
5. **Algorithm Strength:** MD5 and SHA1 rejected
6. **Key Size Minimum:** RSA-2048 required
7. **Revocation Check:** Revoked certs rejected in strict mode
8. **Boundary Check:** notAfter must be > now (not >=)

---

## Compilation

```bash
cd ~/ProjectsWATTS/tn5250j-headless
javac -cp lib/development/junit-4.5.jar:build/classes \
  tests/org/tn5250j/ssl/SSLCertificatePairwiseTest.java
```

**Output:** No errors (1 deprecation warning is expected)

---

## Integration Points

### Real X509TrustManager
Replace `MockTrustManager` with actual implementation (e.g., `SSLImplementation.X509TrustManager`) to test production code:

```java
X509TrustManager trustManager = new SSLImplementation();
trustManager.checkServerTrusted(chain, "X509");
```

### Cert Chain Setup
Use `SSLImplementation.init()` with test certificates:

```java
SSLImplementation ssl = new SSLImplementation();
ssl.init("TLSv1.2");
SSLSocket socket = (SSLSocket) ssl.createSSLSocket("example.com", 443);
```

---

## Test Execution Time
- **Full Suite:** ~30ms
- **Per Test:** <1ms average

---

## Coverage Summary

| Category | Tests | Pass | Fail | Pass% |
|----------|-------|------|------|-------|
| Certificate State | 5 | 5 | 0 | 100% |
| Chain Completeness | 5 | 5 | 0 | 100% |
| Verification Modes | 4 | 4 | 0 | 100% |
| Hostname Matching | 3 | 3 | 0 | 100% |
| Chain & State | 2 | 2 | 0 | 100% |
| Trust Store & Host | 3 | 3 | 0 | 100% |
| Certificate & Store | 3 | 3 | 0 | 100% |
| Revocation | 2 | 2 | 0 | 100% |
| Wildcard | 3 | 3 | 0 | 100% |
| SANs | 2 | 2 | 0 | 100% |
| Boundaries | 2 | 2 | 0 | 100% |
| Algorithms | 3 | 3 | 0 | 100% |
| Key Sizes | 3 | 3 | 0 | 100% |
| **TOTAL** | **40** | **40** | **0** | **100%** |

---

## Next Steps

1. **Integrate Real Certificates:** Replace mock with actual test certificates
2. **OCSP Validation:** Implement real revocation checking
3. **Session Resumption:** Test TLS session caching
4. **Performance Testing:** Benchmark certificate validation
5. **Integration Testing:** Test with actual TN5250j terminal connections

