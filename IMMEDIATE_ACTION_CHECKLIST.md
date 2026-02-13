# Immediate Action Checklist - Wave 1/2 Error Fixes

**Priority**: P0 - BLOCKER
**Estimated Time**: 9 hours
**Goal**: Fix 3 compilation errors and create integration tests before Wave 3

---

## Quick Status Check

```bash
# Run this to see current status
./gradlew clean build 2>&1 | tail -20

# Expected output right now:
# BUILD FAILED in 575ms
# 3 errors
# 33 warnings
```

---

## Task 1: Fix GuiGraphicBuffer.java Missing Method (1.5 hours)

### Error
```
error: GuiGraphicBuffer is not abstract and does not override abstract method
propertyChange(PropertyChangeEvent) in PropertyChangeListener
public class GuiGraphicBuffer implements ScreenOIAListener,
       ^
```

### Investigation Steps

1. **Check if method exists but has wrong signature**
   ```bash
   grep -n "propertyChange" src/org/hti5250j/GuiGraphicBuffer.java
   ```

2. **Check current method signature** (if found)
   ```java
   // If you find this:
   public void propertyChange(SessionConfigEvent event) { ... }

   // It should be:
   public void propertyChange(PropertyChangeEvent event) { ... }
   ```

3. **If method doesn't exist, add it**
   ```java
   // Add to GuiGraphicBuffer.java around line 487 (where pattern matching code is)

   @Override
   public void propertyChange(PropertyChangeEvent event) {
       // Handle PropertyChangeEvent (base interface requirement)
       if (event instanceof SessionConfigEvent sce) {
           // Delegate to existing SessionConfigListener method
           onSessionConfigChanged(sce);
       }
       // Add other event type handling if needed
   }
   ```

4. **Verify fix**
   ```bash
   ./gradlew compileJava
   # Should succeed without GuiGraphicBuffer error
   ```

### ✅ Success Criteria
- [ ] `./gradlew compileJava` succeeds for GuiGraphicBuffer
- [ ] Method signature matches `PropertyChangeListener` interface
- [ ] Existing event handling logic still works

---

## Task 2: Fix RectTest.java Type Error (30 minutes)

### Error
```
error: incompatible types: Object cannot be converted to String
String value = map.get(rect2);
               ^
```

### Fix

**File**: `/src/test/java/org/hti5250j/framework/common/RectTest.java`
**Line**: 162

**Change from**:
```java
String value = map.get(rect2);
```

**Change to**:
```java
Object value = map.get(rect2);
// Then cast when needed:
assertEquals("first", (String) value, "Equal Rect should retrieve same map value");
```

**Or better** (type-safe approach):
```java
// Change map declaration from:
var map = new java.util.HashMap<>();

// To:
var map = new java.util.HashMap<Rect, String>();

// Then line 162 works as-is:
String value = map.get(rect2);  // No error
```

### ✅ Success Criteria
- [ ] RectTest.java compiles without errors
- [ ] Test still validates HashMap usage with Rect keys
- [ ] Test passes when run

---

## Task 3: Fix SessionConfigEventTest.java Type Hierarchy Issue (2 hours)

### Error
```
error: incompatible types: SessionConfigEvent cannot be converted to PropertyChangeEvent
assertTrue(event instanceof PropertyChangeEvent);
           ^
```

### Analysis

**Root Cause**: SessionConfigEvent is now a record, no longer extends PropertyChangeEvent

**Current Code** (SessionConfigEvent.java):
```java
public record SessionConfigEvent(Object source, String propertyName, Object oldValue, Object newValue) {
    // No longer extends PropertyChangeEvent
}
```

**Test Expects** (SessionConfigEventTest.java:65):
```java
assertTrue(event instanceof PropertyChangeEvent);  // ❌ FAILS
```

### Decision Required: Choose ONE Option

#### Option A: Revert to Class-Based Design (SAFEST)
**Pros**: Maintains full backward compatibility
**Cons**: Loses record benefits, more boilerplate

```java
// SessionConfigEvent.java - Revert to class
public class SessionConfigEvent extends PropertyChangeEvent {
    public SessionConfigEvent(Object source, String propertyName, Object oldValue, Object newValue) {
        super(source, propertyName, oldValue, newValue);
    }
    // Keep backward-compatible getters
}
```

#### Option B: Update Test to Match New Reality (RISKY)
**Pros**: Keeps record design, modern Java 21
**Cons**: Breaking change, may affect production code

```java
// SessionConfigEventTest.java:65 - Update test
// Remove this:
assertTrue(event instanceof PropertyChangeEvent);  // ❌

// Replace with:
assertTrue(event instanceof SessionConfigEvent);   // ✅
assertNotNull(event.propertyName());               // ✅ Test record accessor
```

**THEN audit all code for PropertyChangeEvent assumptions**:
```bash
# Find all files that might assume inheritance
grep -r "PropertyChangeEvent.*SessionConfig" src/ --include="*.java"
grep -r "SessionConfig.*PropertyChange" src/ --include="*.java"
grep -r "instanceof PropertyChangeEvent" src/ --include="*.java" | grep -v Test
```

#### Option C: Create Adapter/Wrapper (COMPLEX)
**Pros**: Keeps record, maintains compatibility
**Cons**: More code, two classes to maintain

```java
// Keep record as-is:
public record SessionConfigEvent(Object source, String propertyName, Object oldValue, Object newValue) { }

// Create adapter:
public class SessionConfigEventAdapter extends PropertyChangeEvent {
    private final SessionConfigEvent event;

    public SessionConfigEventAdapter(SessionConfigEvent event) {
        super(event.source(), event.propertyName(), event.oldValue(), event.newValue());
        this.event = event;
    }

    public SessionConfigEvent getEvent() { return event; }
}

// Usage in listener code:
public void fireEvent(SessionConfigEvent event) {
    listeners.forEach(l -> l.propertyChange(new SessionConfigEventAdapter(event)));
}
```

### Recommendation: **Option A (Revert)** for now

**Rationale**:
1. Fastest path to green build (30 minutes)
2. Zero risk of breaking production code
3. Can revisit record design in Wave 4 with proper integration tests

**If you choose Option A**:
1. Revert SessionConfigEvent.java to class-based design
2. Keep the comprehensive tests from Agent 10
3. Document in Wave 4: "Convert SessionConfigEvent to record with proper adapter"

### ✅ Success Criteria
- [ ] SessionConfigEventTest.java compiles
- [ ] All SessionConfigEvent tests pass
- [ ] Decision documented (which option chosen and why)
- [ ] If Option B or C chosen: All consumers audited and updated

---

## Task 4: Create Integration Test Suite (3 hours)

**Purpose**: Validate that unit-level changes work together in real application

### Test 1: Application Startup (30 min)

**File**: Create `/src/test/java/org/hti5250j/integration/ApplicationStartupIntegrationTest.java`

```java
package org.hti5250j.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hti5250j.SessionConfig;
import org.hti5250j.GuiGraphicBuffer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration: Application Startup")
class ApplicationStartupIntegrationTest {

    @Test
    @DisplayName("Application initializes without errors")
    void testApplicationStartup() {
        // This test validates that all components can be instantiated
        // and basic initialization works

        // 1. Create session config
        SessionConfig config = new SessionConfig();
        assertNotNull(config);

        // 2. Create GUI buffer (uses config)
        GuiGraphicBuffer buffer = new GuiGraphicBuffer();
        assertNotNull(buffer);

        // 3. Verify buffer implements required interfaces
        assertTrue(buffer instanceof PropertyChangeListener);
        assertTrue(buffer instanceof SessionConfigListener);

        // SUCCESS: Components initialized without errors
    }
}
```

### Test 2: Event Propagation (1 hour)

**File**: `/src/test/java/org/hti5250j/integration/EventPropagationIntegrationTest.java`

```java
package org.hti5250j.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hti5250j.event.SessionConfigEvent;
import org.hti5250j.event.SessionConfigListener;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration: Event Propagation")
class EventPropagationIntegrationTest {

    private TestListener listener;

    @BeforeEach
    void setUp() {
        listener = new TestListener();
    }

    @Test
    @DisplayName("SessionConfigEvent fires to registered listeners")
    void testEventFiresToListeners() {
        // Arrange
        Object source = new Object();
        SessionConfigEvent event = new SessionConfigEvent(
            source, "testProperty", "oldValue", "newValue"
        );

        // Act
        listener.onSessionConfigChanged(event);

        // Assert
        assertTrue(listener.receivedEvent.get(), "Listener should receive event");
        assertEquals("testProperty", listener.lastPropertyName.get());
        assertEquals("newValue", listener.lastNewValue.get());
    }

    // Test helper
    static class TestListener implements SessionConfigListener {
        final AtomicBoolean receivedEvent = new AtomicBoolean(false);
        final AtomicReference<String> lastPropertyName = new AtomicReference<>();
        final AtomicReference<Object> lastNewValue = new AtomicReference<>();

        @Override
        public void onSessionConfigChanged(SessionConfigEvent event) {
            receivedEvent.set(true);
            lastPropertyName.set(event.getPropertyName());
            lastNewValue.set(event.getNewValue());
        }
    }
}
```

### Test 3: Rect Record in Real Usage (30 min)

**File**: `/src/test/java/org/hti5250j/integration/RectRecordIntegrationTest.java`

```java
package org.hti5250j.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hti5250j.framework.tn5250.Rect;

import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration: Rect Record Usage")
class RectRecordIntegrationTest {

    @Test
    @DisplayName("Rect works as HashMap key in real usage")
    void testRectAsMapKey() {
        // Simulate real usage: mapping screen regions to metadata
        var regionMetadata = new HashMap<Rect, String>();

        // Add regions
        regionMetadata.put(new Rect(0, 0, 80, 24), "main screen");
        regionMetadata.put(new Rect(0, 23, 80, 1), "status line");

        // Retrieve by equal Rect (tests equals/hashCode)
        String metadata = regionMetadata.get(new Rect(0, 0, 80, 24));
        assertEquals("main screen", metadata);
    }

    @Test
    @DisplayName("Rect works in HashSet for deduplication")
    void testRectDeduplication() {
        var uniqueRegions = new HashSet<Rect>();

        // Add same rect twice
        uniqueRegions.add(new Rect(10, 20, 30, 40));
        uniqueRegions.add(new Rect(10, 20, 30, 40));

        // Should only have one entry
        assertEquals(1, uniqueRegions.size());
    }

    @Test
    @DisplayName("Deprecated getters still work")
    @SuppressWarnings("deprecation")
    void testBackwardCompatibility() {
        Rect rect = new Rect(5, 10, 100, 200);

        // Old API still works
        assertEquals(5, rect.getX());
        assertEquals(10, rect.getY());
        assertEquals(100, rect.getWidth());
        assertEquals(200, rect.getHeight());

        // New API also works
        assertEquals(5, rect.x());
        assertEquals(10, rect.y());
        assertEquals(100, rect.width());
        assertEquals(200, rect.height());
    }
}
```

### Test 4: GuiGraphicBuffer with New Events (1 hour)

**File**: `/src/test/java/org/hti5250j/integration/GuiGraphicBufferIntegrationTest.java`

```java
package org.hti5250j.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hti5250j.GuiGraphicBuffer;
import org.hti5250j.event.SessionConfigEvent;

import java.beans.PropertyChangeEvent;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration: GuiGraphicBuffer Event Handling")
class GuiGraphicBufferIntegrationTest {

    @Test
    @DisplayName("GuiGraphicBuffer handles PropertyChangeEvent")
    void testPropertyChangeEventHandling() {
        // Arrange
        GuiGraphicBuffer buffer = new GuiGraphicBuffer();
        PropertyChangeEvent event = new PropertyChangeEvent(
            this, "testProperty", "old", "new"
        );

        // Act - should not throw
        assertDoesNotThrow(() -> buffer.propertyChange(event));
    }

    @Test
    @DisplayName("GuiGraphicBuffer handles SessionConfigEvent")
    void testSessionConfigEventHandling() {
        // Arrange
        GuiGraphicBuffer buffer = new GuiGraphicBuffer();
        SessionConfigEvent event = new SessionConfigEvent(
            this, "colorScheme", "light", "dark"
        );

        // Act - should not throw
        assertDoesNotThrow(() -> buffer.onSessionConfigChanged(event));
    }
}
```

### ✅ Success Criteria
- [ ] All 4 integration test files created
- [ ] All integration tests compile
- [ ] All integration tests pass
- [ ] Tests validate cross-component interactions

---

## Task 5: Final Verification (30 minutes)

### Verification Checklist

```bash
# 1. Clean build
./gradlew clean

# 2. Full build (must succeed)
./gradlew build

# Expected output:
# BUILD SUCCESSFUL
# 0 errors
# ≤33 warnings (should not increase)

# 3. Run all tests
./gradlew test

# Expected output:
# All tests passed
# 0 failures
# 0 skipped

# 4. Check warning count
./gradlew build 2>&1 | grep "warnings"

# Expected: "33 warnings" or fewer
```

### Regression Tests

Run these specific test suites to ensure nothing broke:

```bash
# Wave 1 fixes
./gradlew test --tests "*CCSID930Test"
./gradlew test --tests "*GuiGraphicBufferTest"

# Wave 2 conversions
./gradlew test --tests "*RectTest"
./gradlew test --tests "*SessionConfigEventTest"
./gradlew test --tests "*EmulatorActionEventTest"

# New integration tests
./gradlew test --tests "*IntegrationTest"
```

### ✅ Success Criteria
- [ ] `./gradlew clean build` succeeds (0 errors)
- [ ] `./gradlew test` succeeds (all pass)
- [ ] Warning count ≤ 33 (no new warnings)
- [ ] All Wave 1 tests still pass
- [ ] All Wave 2 tests still pass
- [ ] All integration tests pass

---

## Task 6: Documentation (30 minutes)

### Update Status Documents

1. **Create WAVE_1_2_COMPLETION_REPORT.md**
   ```markdown
   # Wave 1 & 2 Completion Report

   **Date**: 2026-02-[XX]
   **Status**: ✅ COMPLETE - All errors fixed, integration tests passing

   ## Fixes Applied
   1. GuiGraphicBuffer.java - Added missing propertyChange() method
   2. RectTest.java - Fixed type incompatibility
   3. SessionConfigEvent.java - [Document which option chosen]

   ## Integration Tests Created
   - ApplicationStartupIntegrationTest
   - EventPropagationIntegrationTest
   - RectRecordIntegrationTest
   - GuiGraphicBufferIntegrationTest

   ## Build Status
   - Compilation: ✅ 0 errors
   - Tests: ✅ All passing
   - Warnings: 33 (tracked for Wave 4)

   ## Readiness for Wave 3
   ✅ READY - All quality gates passed
   ```

2. **Update AGENT_ASSIGNMENTS.md**
   - Add note about error fixes
   - Document lessons learned
   - Add new quality gates for Wave 3+

### ✅ Success Criteria
- [ ] Completion report created
- [ ] Agent assignments updated
- [ ] Lessons learned documented

---

## Time Tracking

| Task | Estimated | Actual | Status |
|------|-----------|--------|--------|
| 1. Fix GuiGraphicBuffer | 1.5h | | ⬜ Not Started |
| 2. Fix RectTest | 0.5h | | ⬜ Not Started |
| 3. Fix SessionConfigEvent | 2.0h | | ⬜ Not Started |
| 4. Integration Tests | 3.0h | | ⬜ Not Started |
| 5. Final Verification | 0.5h | | ⬜ Not Started |
| 6. Documentation | 0.5h | | ⬜ Not Started |
| **TOTAL** | **8.0h** | | |

**Target Completion**: [Fill in target date/time]

---

## Success Definition

**Wave 1/2 is COMPLETE when**:
- ✅ 0 compilation errors
- ✅ All tests passing (unit + integration)
- ✅ Backward compatibility validated
- ✅ Integration tests demonstrate cross-component functionality
- ✅ Documentation updated

**Then and only then**: Proceed to Wave 3 (file splitting)

---

## Quick Reference Commands

```bash
# Check current status
./gradlew clean build

# Run specific test
./gradlew test --tests "*RectTest"

# Run all integration tests
./gradlew test --tests "*IntegrationTest"

# Full verification (clean slate)
./gradlew clean build test

# Check warning count
./gradlew build 2>&1 | grep -c "warning"
```

---

**Priority**: Start with Task 1 (GuiGraphicBuffer) - it's the blocker
**Escalate If**: Any task takes >2x estimated time
**Ask For Help If**: Unclear which option to choose for SessionConfigEvent fix
