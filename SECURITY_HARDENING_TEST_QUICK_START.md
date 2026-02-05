# SecurityHardeningPairwiseTest - Quick Start Guide

## Running the Tests

### Option 1: Direct JUnit Execution
```bash
cd ~/ProjectsWATTS/tn5250j-headless
java -cp "build:lib/development/junit-4.5.jar:lib/development/*" \
  org.junit.runner.JUnitCore \
  org.tn5250j.security.SecurityHardeningPairwiseTest
```

### Option 2: Ant Build
```bash
cd ~/ProjectsWATTS/tn5250j-headless
ant compile-tests run-tests
```

### Expected Output
```
JUnit version 4.5
..............................
Time: 0.144

OK (31 tests)
```

## Test File Location
**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/tn5250j/security/SecurityHardeningPairwiseTest.java`

## Test Summary
- **31 pairwise combination tests** covering 5 security dimensions
- **100% pass rate** (all 31 tests passing)
- **Execution time:** 0.144 seconds
- **CWE coverage:** 9 CWE categories (22, 89, 90, 276, 287, 295, 327, 611, 798)

## Pairwise Dimensions

### 1. Authentication Type (5 tests)
Tests auth methods: none, password, certificate, Kerberos
- ✓ Plaintext without auth fails
- ✓ Password without encryption fails
- ✓ Password + TLS 1.2 accepted
- ✓ Certificate + TLS 1.3 accepted
- ✓ Kerberos + TLS 1.3 accepted

### 2. Encryption Level (5 tests)
Tests TLS versions: none, 1.0, 1.1, 1.2, 1.3
- ✓ No encryption fails for credentials
- ✓ TLS 1.0 rejected (POODLE)
- ✓ TLS 1.1 rejected (BEAST)
- ✓ TLS 1.2 accepted
- ✓ TLS 1.3 accepted (strongest)

### 3. Credential Storage (3 tests)
Tests: memory, file, keystore
- ✓ Memory credentials cleared after use
- ✓ File permissions restricted
- ✓ Keystore password validation enforced

### 4. Certificate Validation (5 tests)
Tests: strict, relaxed, permissive
- ✓ Strict mode accepts valid certs
- ✓ Strict mode rejects expired certs
- ✓ Strict mode rejects wrong-host certs
- ✓ Relaxed mode accepts self-signed
- ✓ Permissive rejects null chains

### 5. Protocol Version (3 tests)
Tests: telnet, TN5250E, SSL
- ✓ Plain telnet rejected
- ✓ TN5250E requires encryption
- ✓ SSL + cert + TLS 1.3 accepted

## Adversarial Tests (10 tests)

### Injection Attacks (4 tests)
- SQL injection blocked
- Path traversal blocked
- XXE injection blocked
- LDAP injection blocked

### Downgrade Attacks (3 tests)
- TLS 1.3 → 1.0 downgrade blocked
- Auth removal negotiation blocked
- Encryption disabling blocked

### Credential Theft (3 tests)
- No password logging
- No credentials in session tokens
- Keystore encryption enforced

## Test Classes

### MockAuthenticationManager
Enforces security policies:
```java
validateSecurityPolicy(authType, encryption, protocol)
isEncryptionLevelAcceptable(tlsVersion)
isProtocolAccepted(protocol)
validateCertificate*(cert, host)
validateUsername(username)
validateMacroName(macro)
validateXMLConfiguration(xml)
validateLDAPFilter(filter)
negotiateProtocol(clientProto, serverProto)
```

### MockCredentialStore
Manages test credentials:
```java
storeCredential(key, credential, storageType)
loadKeystore(path, password)
useCredential(key)
```

### MockX509Certificate
Test certificate mocking:
```java
isExpired()
getSubjectDN()
getIssuerDN()
```

## Security CWEs Covered

| CWE | Title | Tests |
|-----|-------|-------|
| 22 | Path Traversal | 1 |
| 89 | SQL Injection | 1 |
| 90 | LDAP Injection | 1 |
| 276 | File Permission Issues | 1 |
| 287 | Authentication Bypass | 1 |
| 295 | Certificate Validation Bypass | 5 |
| 327 | Weak Cryptography | 10 |
| 611 | XXE/SSRF | 1 |
| 798 | Hardcoded Passwords | 7 |

## TDD Phases

### RED Phase
- Tests written to verify security requirements
- All 31 tests start as RED (failing on vulnerable code)
- Tests fail for the right reason - exposing actual vulnerabilities

### GREEN Phase
- Mock implementations provide minimal code to pass tests
- MockAuthenticationManager enforces security policies
- MockCredentialStore securely handles credentials

### REFACTOR Phase (Optional)
- Tests provide specifications for hardening SSLImplementation
- Use tests to guide actual security implementation
- Tests remain green throughout refactoring

## Implementation Recommendations

To use these tests for real security hardening:

1. **SSLImplementation Hardening**
   - Implement TLS 1.2+ only
   - Add certificate validation
   - Use secure credential storage

2. **Authentication Manager**
   - Enforce password encryption requirement
   - Implement Kerberos support
   - Add protocol negotiation security

3. **Input Validation**
   - Implement SQL parameterization
   - Validate macro/file paths
   - Parse XML safely

4. **Credential Handling**
   - Clear passwords from memory
   - Encrypt keystore files
   - Never log credentials

## Test Metrics

- **Lines of Code:** ~820 (including mocks)
- **Test Methods:** 31
- **Mock Classes:** 3
- **Assertions:** 50+
- **CWE Categories:** 9
- **Execution Time:** 144 ms

## Integration

Add to CI/CD pipeline:
```bash
# In build script
javac -cp "build:lib/development/junit-4.5.jar:lib/development/*" \
  -d build tests/org/tn5250j/security/SecurityHardeningPairwiseTest.java

java -cp "build:lib/development/junit-4.5.jar:lib/development/*" \
  org.junit.runner.JUnitCore \
  org.tn5250j.security.SecurityHardeningPairwiseTest
```

## Troubleshooting

### Issue: Keymap file not found
```
INFO [org.tn5250j.GlobalConfigure] Information Message: ~/.tn5250j/keymap ...
```
**Solution:** Normal initialization. Keymap will be created on first use.

### Issue: Compilation fails - imports not found
```
error: package org.junit does not exist
```
**Solution:** Ensure junit-4.5.jar is in classpath:
```bash
javac -cp "build:lib/development/junit-4.5.jar" ...
```

### Issue: Tests timeout
```
Current timeout is too short
```
**Solution:** Tests run in 144ms, increase timeout to 1000ms if needed.

## Further Reading

- **File:** SECURITY_HARDENING_TEST_REPORT.md - Detailed test report
- **File:** SSLSecurityPairwiseTest.java - Complementary SSL tests
- **File:** SecurityVulnerabilityTest.java - Known vulnerability tests

