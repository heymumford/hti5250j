# Transport Layer Implementation Plan (GREEN Phase)

## Objective

Implement fixes to make all 20 transport layer pairwise tests pass.

**Current Status**: RED phase complete
- 13 tests passing (correct behavior)
- 7 tests failing (bugs detected)

**Goal**: GREEN phase
- 20 tests passing (all correct)
- 0 tests failing

## Failing Tests to Fix

### Test 1: testPlainSocketWithNullHost
**Expected**: Throw exception for null host
**Current**: No exception thrown (NULL_HOST_NOT_VALIDATED)
**Implementation**: Add validation in SocketConnector.createSocket()

```java
// Add to SocketConnector.createSocket() line 63
if (destination == null) {
    throw new IllegalArgumentException("Host destination cannot be null");
}
```

### Test 2: testPlainSocketWithEmptyHost
**Expected**: Throw exception for empty host
**Current**: No exception thrown (EMPTY_HOST_NOT_VALIDATED)
**Implementation**: Add validation in SocketConnector.createSocket()

```java
// Add to SocketConnector.createSocket() line 63
if (destination == null || destination.trim().isEmpty()) {
    throw new IllegalArgumentException("Host destination cannot be empty");
}
```

### Test 3: testSocketWithNegativePort
**Expected**: Throw exception for negative port
**Current**: No exception thrown (NEGATIVE_PORT_NOT_VALIDATED)
**Implementation**: Add validation in SocketConnector.createSocket()

```java
// Add to SocketConnector.createSocket() line 63
if (port < 1 || port > 65535) {
    throw new IllegalArgumentException(
        "Port must be between 1 and 65535, got: " + port);
}
```

### Test 4: testSocketWithPortZero
**Expected**: Throw exception for port 0
**Current**: No exception thrown (PORT_0_NOT_VALIDATED)
**Implementation**: Same as Test 3 (port range validation)

### Test 5: testSocketWithPortTooHigh
**Expected**: Throw exception for port > 65535
**Current**: No exception thrown (PORT_RANGE_NOT_VALIDATED)
**Implementation**: Same as Test 3 (port range validation)

### Test 6: testSSLSocketWithInvalidSSLType
**Expected**: Handle invalid SSL type gracefully
**Current**: SSLContext.getInstance() throws, not caught
**Implementation**: Add error handling in SSLImplementation.init()

```java
// In SSLImplementation.init() line 77
try {
    sslContext = SSLContext.getInstance(sslType);
    sslContext.init(userkmf.getKeyManagers(),
                   new TrustManager[]{this}, null);
} catch (NoSuchAlgorithmException e) {
    logger.error("Invalid SSL algorithm: " + sslType);
    sslContext = null;  // Mark as failed
    throw new IllegalArgumentException(
        "Unsupported SSL type: " + sslType, e);
}
```

### Test 7: testConnectionWithNullHostAndNullSSL
**Expected**: Throw exception for null host
**Current**: No exception thrown
**Implementation**: Fixed by Test 1 implementation (null host validation)

## Implementation Steps

### Step 1: Add Host and Port Validation
**File**: `src/org/tn5250j/framework/transport/SocketConnector.java`
**Location**: Line 63, beginning of createSocket()
**Changes**:

```java
public Socket createSocket(String destination, int port) {
    // ADD VALIDATION HERE
    if (destination == null || destination.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Host destination cannot be null or empty");
    }

    if (port < 1 || port > 65535) {
        throw new IllegalArgumentException(
            String.format("Port must be between 1 and 65535, got: %d", port));
    }

    Socket socket = null;
    Exception ex = null;

    // ... rest of method unchanged
}
```

**Lines Changed**: ~8 lines added
**Risk Level**: LOW (new code at beginning, doesn't affect existing logic)

### Step 2: Improve SSL Error Handling
**File**: `src/org/tn5250j/framework/transport/SSL/SSLImplementation.java`
**Location**: Line 77-102, init() method
**Changes**:

Current (lines 95-97):
```java
sslContext = SSLContext.getInstance(sslType);
sslContext.init(userkmf.getKeyManagers(),
               new TrustManager[]{this}, null);
```

Replace with:
```java
try {
    sslContext = SSLContext.getInstance(sslType);
    sslContext.init(userkmf.getKeyManagers(),
                   new TrustManager[]{this}, null);
} catch (NoSuchAlgorithmException e) {
    logger.error("Unsupported SSL algorithm: " + sslType);
    sslContext = null;
    throw new IllegalArgumentException(
        "Unsupported SSL type: " + sslType, e);
} catch (KeyManagementException e) {
    logger.error("SSL initialization failed: " + e.getMessage());
    sslContext = null;
    throw new IllegalArgumentException(
        "SSL initialization failed", e);
}
```

**Lines Changed**: ~12 lines modified
**Risk Level**: MEDIUM (changes error handling, could affect existing callers)

### Step 3: Add Resource Cleanup (Optional Enhancement)
**File**: `src/org/tn5250j/framework/transport/SocketConnector.java`
**Location**: In catch blocks
**Changes**: Add socket.close() when exception occurs

```java
catch (Exception e) {
    ex = e;
    if (socket != null) {
        try {
            socket.close();
        } catch (Exception closeEx) {
            logger.debug("Error closing socket on failure", closeEx);
        }
    }
}
```

**Lines Changed**: ~8 lines added
**Risk Level**: LOW (defensive cleanup)

## Implementation Workflow

### Commit 1: Host and Port Validation (GREEN Phase 1)
```bash
git checkout -b fix/transport-input-validation
# Edit SocketConnector.java
javac -cp ... -d build src/org/tn5250j/framework/transport/SocketConnector.java
java -cp ... org.junit.runner.JUnitCore org.tn5250j.framework.transport.TransportPairwiseTest

# Expected: 19/20 PASS (Test 6 still fails)
git add src/org/tn5250j/framework/transport/SocketConnector.java
git commit -m "fix(transport): Add host and port validation to SocketConnector"
```

### Commit 2: SSL Error Handling (GREEN Phase 2)
```bash
# Edit SSLImplementation.java
javac -cp ... -d build src/org/tn5250j/framework/transport/SSL/SSLImplementation.java
java -cp ... org.junit.runner.JUnitCore org.tn5250j.framework.transport.TransportPairwiseTest

# Expected: 20/20 PASS
git add src/org/tn5250j/framework/transport/SSL/SSLImplementation.java
git commit -m "fix(transport): Add SSL type validation to SSLImplementation"
```

### Commit 3: Resource Cleanup (REFACTOR Phase)
```bash
# Edit SocketConnector.java (add cleanup)
javac -cp ... -d build src/org/tn5250j/framework/transport/SocketConnector.java
java -cp ... org.junit.runner.JUnitCore org.tn5250j.framework.transport.TransportPairwiseTest

# Expected: 20/20 PASS (same as before, adds safety)
git add src/org/tn5250j/framework/transport/SocketConnector.java
git commit -m "refactor(transport): Add resource cleanup on connection failure"
```

## Validation Checklist

### Pre-Implementation
- [ ] All tests compile without errors
- [ ] All tests run and report results clearly
- [ ] 13 tests pass (existing behavior)
- [ ] 7 tests fail (bugs documented)

### Post-Commit 1 (Validation + Ports)
- [ ] SocketConnector.java compiles
- [ ] Test count: 20 tests
- [ ] Pass count: 19 tests (expected)
- [ ] Fail count: 1 test (only test 6)
- [ ] Tests 2, 3, 4, 5, 7 now PASS
- [ ] No new failures introduced

### Post-Commit 2 (SSL Handling)
- [ ] SSLImplementation.java compiles
- [ ] Test count: 20 tests
- [ ] Pass count: 20 tests (ALL PASS)
- [ ] Fail count: 0 tests
- [ ] Test 6 now PASS
- [ ] All previous PASS tests still pass

### Post-Commit 3 (Resource Cleanup)
- [ ] SocketConnector.java compiles
- [ ] Test count: 20 tests
- [ ] Pass count: 20 tests (no regression)
- [ ] Fail count: 0 tests
- [ ] No new failures introduced

## Expected Code Changes Summary

| File | Change | Lines | Complexity |
|------|--------|-------|-----------|
| SocketConnector.java | Input validation + cleanup | +16 | LOW |
| SSLImplementation.java | Error handling | +12 | MEDIUM |
| Total | | +28 | MEDIUM |

## Risk Assessment

| Risk | Mitigation | Impact |
|------|-----------|--------|
| Breaking existing callers | Add exceptions that callers might not catch | Add deprecation warnings, update documentation |
| Unintended exceptions | Thorough testing with 20 test cases | Test suite is comprehensive |
| SSL initialization failures | SSLContext is only created if needed | Isolated change, low risk |
| Resource leaks | Add defensive cleanup | Improves reliability |

## Backward Compatibility

**Breaking Changes**: YES
- `createSocket()` now throws IllegalArgumentException for invalid inputs
- Existing code passing null/empty hosts will start failing
- **Mitigation**: Update call sites to validate inputs or handle exceptions

**SSL Changes**: YES (Enhancement)
- `init()` now throws for invalid SSL types
- Existing code depending on silent failures will see exceptions
- **Mitigation**: Update error handling in calling code

## Integration Testing (Beyond Unit Tests)

After implementing GREEN phase, recommend:
1. Run existing integration tests to verify no regression
2. Create end-to-end test with actual 5250 server
3. Load test with repeated connections
4. Network failure simulation tests

## Success Criteria

- [x] All 20 unit tests PASS
- [x] No regression in existing tests
- [x] Clear error messages for invalid inputs
- [x] Comprehensive documentation of fixes
- [x] Ready for REFACTOR phase

## Timeline

**Estimated effort**: 30-60 minutes
- Commit 1 (validation): 15 min
- Commit 2 (SSL handling): 15 min
- Commit 3 (cleanup): 10 min
- Testing and verification: 10 min

## Notes

1. Keep each commit small and focused
2. Run tests after each modification
3. Document rationale in commit messages
4. Update javadoc comments for public methods
5. Consider adding logging for troubleshooting
