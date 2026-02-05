# Host Application Simulation Test - Quick Start Guide

## Location
**Test File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/tn5250j/simulation/HostAppSimulationPairwiseTest.java`

## Quick Compilation & Execution

### Compile Only
```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
javac -cp "lib/development/junit-4.5.jar:lib/development/*:lib/runtime/*:build" \
  tests/org/tn5250j/simulation/HostAppSimulationPairwiseTest.java
```

### Run All Tests
```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
java -cp "lib/development/junit-4.5.jar:lib/development/*:lib/runtime/*:build:tests" \
  org.junit.runner.JUnitCore org.tn5250j.simulation.HostAppSimulationPairwiseTest
```

### Expected Output
```
JUnit version 4.5
..............................
Time: 1.854

OK (30 tests)
```

## Test Statistics

| Metric | Value |
|--------|-------|
| Total Tests | 30 |
| Passing | 30 |
| Failing | 0 |
| Execution Time | ~1.85 seconds |
| Average per test | 61.8 ms |

## What's Being Tested

### 5 Screen Types
1. **SIGNON** - Login/authentication screen
2. **MENU** - Main menu with selections
3. **DATA_ENTRY** - Form data input screen
4. **REPORT** - Display report output
5. **SUBFILE** - Tabular/list data display

### 4 Navigation Actions
1. **FORWARD** - Proceed to next screen
2. **BACKWARD** - Return to previous screen
3. **HELP** - Display context help
4. **EXIT** - Disconnect from host

### 3 Response Timings
1. **IMMEDIATE** - No delay
2. **DELAYED** - 200ms simulated network lag
3. **TIMEOUT** - Connection timeout exception

### 3 Validation Locations
1. **CLIENT_SIDE** - Pre-navigation validation
2. **SERVER_SIDE** - Pre-navigation validation  
3. **BOTH** - Both client and server validations

### 3 Error Responses
1. **MESSAGE** - Error queued, continue
2. **LOCK** - Keyboard locked, blocked
3. **DISCONNECT** - Connection terminated

## Key Classes

### MockHostScreen
Simulates 5250 host screen states, transitions, and keyboard state.

```java
new MockHostScreen(ScreenType.SIGNON, ResponseTiming.IMMEDIATE)
```

### HostResponseHandler
Validates data and orchestrates screen navigation with error handling.

```java
new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.MESSAGE)
```

## Navigation Examples

### Simple Forward Navigation
```java
// Start on SIGNON, go to MENU
hostScreen = new MockHostScreen(ScreenType.SIGNON, ResponseTiming.IMMEDIATE);
responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.MESSAGE);

Map<String, String> loginData = new HashMap<>();
loginData.put("username", "user");
loginData.put("password", "pass");

boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, loginData);
assertEquals(ScreenType.MENU, hostScreen.getCurrentScreenType());
```

### Error Handling
```java
// Test invalid data on DATA_ENTRY screen
hostScreen = new MockHostScreen(ScreenType.DATA_ENTRY, ResponseTiming.IMMEDIATE);
responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.SERVER_SIDE, ErrorResponse.MESSAGE);

Map<String, String> badData = new HashMap<>();
badData.put("field", "x".repeat(101)); // > 100 chars

boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, badData);
assertFalse(result); // Should fail validation
assertTrue(!responseHandler.getErrorMessages().isEmpty()); // Error queued
```

### Timeout Handling
```java
// Test timeout response
hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.TIMEOUT);
responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.DISCONNECT);

try {
    responseHandler.validateAndNavigate(NavigationAction.EXIT, null);
    fail("Should throw TimeoutException");
} catch (TimeoutException e) {
    // Expected behavior
}
```

## Validation Rules

### Client-Side (Before Navigation)
- Null/empty data OK for: MENU, REPORT, HELP, SIGNON, SUBFILE
- Null/empty data INVALID for: DATA_ENTRY
- Non-empty fields must not be blank

### Server-Side (Before Navigation)
- DATA_ENTRY requires non-null, non-empty data map
- All field values must be ≤ 100 characters
- Field values cannot be null if data map is provided

## Navigation Flow

```
SIGNON
  ├─ FORWARD → MENU (login success)
  ├─ BACKWARD → ✗ (fail)
  ├─ HELP → Help overlay
  └─ EXIT → Keyboard locked

MENU
  ├─ FORWARD → DATA_ENTRY (selection 1-3)
  ├─ BACKWARD → SIGNON
  ├─ HELP → Help overlay
  └─ EXIT → Keyboard locked

DATA_ENTRY
  ├─ FORWARD → REPORT (submit form)
  ├─ BACKWARD → MENU
  ├─ HELP → Help overlay
  └─ EXIT → Keyboard locked

REPORT
  ├─ FORWARD → ✗ (fail, end of flow)
  ├─ BACKWARD → DATA_ENTRY
  ├─ HELP → Help overlay
  └─ EXIT → Keyboard locked

SUBFILE
  ├─ FORWARD → REPORT
  ├─ BACKWARD → MENU
  ├─ HELP → Help overlay
  └─ EXIT → Keyboard locked
```

## Test Categories

### Group 1: SIGNON Tests (5)
Basic authentication flow and error handling

### Group 2: MENU Tests (5)
Selection and menu navigation

### Group 3: DATA_ENTRY Tests (5)
Form submission and validation

### Group 4: REPORT Tests (4)
Report viewing and navigation limits

### Group 5: SUBFILE Tests (4)
List/table navigation

### Group 6: Edge Cases (7)
Error messages, state consistency, concurrency

## Common Patterns

### Positive Path Testing
Test successful navigation with valid data
→ Result: `assertTrue(result)`
→ Screen changed to expected type

### Error Path Testing
Test validation failures or timeout
→ Result: `assertFalse(result)` or `throws TimeoutException`
→ Error message queued or keyboard locked

### State Consistency Testing
Verify screen data and state align
→ Check: `hostScreen.getScreenData().contains("expected")`
→ Verify: Screen type matches expected value

## Extending the Tests

Add new test pair by:

1. Choose screen type, navigation, timing, validation, error response
2. Arrange: Create hostScreen and responseHandler
3. Act: Call `validateAndNavigate()`
4. Assert: Verify result, screen type, error state

Example:
```java
@Test
public void testCustomScenario() throws TimeoutException {
    // ARRANGE
    hostScreen = new MockHostScreen(ScreenType.SIGNON, ResponseTiming.IMMEDIATE);
    responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.LOCK);
    
    // ACT
    boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, null);
    
    // ASSERT
    assertFalse(result);
}
```

## Troubleshooting

### "Cannot find symbol: class Timeout"
→ Using JUnit 4.5, which doesn't have @Rule. Use try-catch instead.

### Tests fail with "Help should succeed"
→ Ensure validateAndNavigate is called on correct screen type.

### Timeout test doesn't timeout
→ Verify ResponseTiming.TIMEOUT is set on MockHostScreen.

## Documentation Files

- `HOSTAPP_SIMULATION_TEST_SUMMARY.md` - Comprehensive documentation
- `HOSTAPP_SIMULATION_QUICK_START.md` - This file
- `HostAppSimulationPairwiseTest.java` - Source code with inline comments

## Next Steps

Run the tests and observe:
1. All 30 tests pass
2. Mock host correctly simulates 5250 screens
3. Validation logic works as documented
4. Error handling behaves as specified

Then integrate test patterns into your own host simulation code!
