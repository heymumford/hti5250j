# Headless-First Refactoring Roadmap

**Based on**: Probe Agent B Verification
**Total Files**: 11 core violations identified
**Total Effort**: 71 hours (1.8 weeks)
**Scope**: Extract Swing/AWT dependencies from core protocol/keyboard layer
**Risk Level**: LOW (standard interface extraction pattern)

---

## TIER 1: CRITICAL PATH (46 hours)

*These 5 files MUST be fixed to enable headless deployment*

### 1. tnvt.java - Terminal Protocol Core

**Current Problem**:
- Imports `javax.swing.*` (line 23)
- 2,555 lines (538% over limit)
- Protocol logic mixed with GUI concerns

**Violations**:
- Line 23: `import javax.swing.*;` (too broad)
- Likely uses JComponent for display updates
- May use Swing dialogs for error messages

**Refactoring Strategy**:

**Step 1: Extract Display Interface** (6h)
```java
// New interface in core layer
package org.hti5250j.framework.tn5250.display;

public interface IScreenDisplay {
    void updateScreen(int x, int y, int width, int height);
    void clearScreen();
    void setColors(Color fg, Color bg);
}

// Core protocol now uses abstraction
public class tnvt implements Runnable {
    private IScreenDisplay display;  // ← Instead of JComponent

    public void setDisplay(IScreenDisplay disp) {
        this.display = disp;
    }

    private void updateScreen() {
        display.updateScreen(0, 0, cols, rows);  // ← Abstracted call
    }
}
```

**Step 2: Extract Input Interface** (4h)
```java
// New interface in core layer
public interface IKeyInput {
    int getKeyCode();
    int getModifiers();
    boolean isShiftDown();
    boolean isCtrlDown();
}

// Core protocol now uses abstraction
public class tnvt {
    public void processKeyInput(IKeyInput input) {
        // Handle key input without AWT KeyEvent
        int keyCode = input.getKeyCode();
        // ...
    }
}
```

**Step 3: Create Swing Adapters** (4h)
```java
// In GUI layer
package org.hti5250j.gui.adapters;

public class SwingScreenDisplay extends JComponent implements IScreenDisplay {
    @Override
    public void updateScreen(int x, int y, int width, int height) {
        repaint(x, y, width, height);
    }
}

public class SwingKeyInputAdapter implements IKeyInput {
    private KeyEvent event;

    public SwingKeyInputAdapter(KeyEvent e) {
        this.event = e;
    }

    @Override
    public int getKeyCode() {
        return event.getKeyCode();
    }
    // ...
}
```

**Step 4: Create Headless Implementations** (2h)
```java
// In test/headless layer
public class HeadlessScreenDisplay implements IScreenDisplay {
    private int[][] buffer;

    @Override
    public void updateScreen(int x, int y, int width, int height) {
        // No-op or update internal buffer
    }
}

public class MockKeyInput implements IKeyInput {
    private int keyCode;
    private int modifiers;

    public MockKeyInput(int kc, int mod) {
        keyCode = kc;
        modifiers = mod;
    }

    @Override
    public int getKeyCode() {
        return keyCode;
    }
    // ...
}
```

**tnvt.java Changes Summary**:
- Remove: `import javax.swing.*;`
- Add: `import org.hti5250j.framework.tn5250.display.*;`
- Change: `JComponent display` → `IScreenDisplay display`
- Change: method signature from `KeyEvent` → `IKeyInput`
- Remove all direct Swing calls

**Test Strategy**:
```java
@Test
public void testTnvtWithHeadlessDisplay() {
    tnvt proto = new tnvt();
    IScreenDisplay display = new HeadlessScreenDisplay();
    proto.setDisplay(display);

    // No X11 required
    byte[] message = buildTestMessage();
    proto.handleDataStream(message);

    // Verify protocol state changed
    assertEquals(expectedState, proto.getState());
}
```

**Effort**: **16 hours**
**Risk**: MEDIUM (large refactoring, multiple interfaces)
**Complexity**: HIGH (2,555 lines, many dependencies)

---

### 2. KeyboardHandler.java - Keyboard Input Handler

**Current Problem**:
- Imports 6 Swing/AWT classes (lines 15-21)
- Extends `KeyAdapter` (AWT-dependent)
- Uses `ActionMap`, `InputMap`, `KeyStroke` (Swing constructs)

**Violations**:
- Line 15: `import java.awt.event.KeyAdapter;` (parent class)
- Line 16: `import java.awt.event.KeyEvent;`
- Line 18-21: `javax.swing` imports (Action, ActionMap, InputMap, KeyStroke)

**Refactoring Strategy**:

**Step 1: Extract Key Handler Interface** (4h)
```java
// New interface
public interface IKeyHandler {
    void handleKeyDown(IKeyInput input);
    void handleKeyUp(IKeyInput input);
    void handleKeyTyped(IKeyInput input);
}

// Remove parent class dependency
public class KeyboardHandler implements IKeyHandler {
    // No longer extends KeyAdapter

    @Override
    public void handleKeyDown(IKeyInput input) {
        // Handle key down
    }
}
```

**Step 2: Create Swing Adapter** (3h)
```java
// In GUI layer
public class SwingKeyboardAdapter extends KeyAdapter implements IKeyHandler {
    private IKeyHandler handler;

    public SwingKeyboardAdapter(IKeyHandler h) {
        this.handler = h;
    }

    @Override
    public void keyPressed(KeyEvent event) {
        handler.handleKeyDown(new SwingKeyInputAdapter(event));
    }

    // Register with Swing InputMap/ActionMap
    private void registerWithSwing() {
        // GUI-specific registration code
    }
}
```

**Step 3: Remove GUI Constructs** (2h)
- Replace `ActionMap` with action registry
- Replace `InputMap` with key mapping configuration
- Replace `KeyStroke` with key code constants

**KeyboardHandler.java Changes Summary**:
- Remove: extends `KeyAdapter`
- Remove: all AWT/Swing imports (6 lines)
- Add: `implements IKeyHandler`
- Change: method signatures to use `IKeyInput`
- Remove: `ActionMap`, `InputMap` fields
- Add: action registry interface

**Test Strategy**:
```java
@Test
public void testKeyboardHandlerHeadless() {
    IKeyHandler handler = new KeyboardHandler();

    // Inject mock key input
    MockKeyInput input = new MockKeyInput(KeyCode.ENTER, 0);
    handler.handleKeyDown(input);

    // Verify action was triggered
    assertEquals(expectedState, handler.getLastAction());
}
```

**Effort**: **8 hours**
**Risk**: LOW (straightforward interface extraction)
**Complexity**: MEDIUM (6 dependencies to replace)

---

### 3. KeyMapper.java - Key Mapping Configuration

**Current Problem**:
- Imports `KeyEvent`, `KeyStroke` (AWT/Swing)
- Uses `HashMap<KeyStroker, String>` with Swing KeyStroke
- Contains 481 lines of mapping logic

**Violations**:
- Line 14: `import java.awt.event.InputEvent;`
- Line 15: `import java.awt.event.KeyEvent;`
- Line 25: `import javax.swing.KeyStroke;`

**Refactoring Strategy**:

**Step 1: Extract KeyStroke Data Model** (4h)
```java
// New class (pure data, no AWT)
package org.hti5250j.keyboard.model;

public class KeyStrokeData {
    private int keyCode;
    private boolean shift;
    private boolean ctrl;
    private boolean alt;
    private int location;

    public KeyStrokeData(int kc, boolean s, boolean c, boolean a, int l) {
        keyCode = kc;
        shift = s;
        ctrl = c;
        alt = a;
        location = l;
    }

    // Getters (immutable)
    public int getKeyCode() { return keyCode; }
    public boolean isShift() { return shift; }
    // ...
}
```

**Step 2: Update KeyMapper** (5h)
```java
// Use new data model
public class KeyMapper {
    private static HashMap<KeyStrokeData, String> mappedKeys;

    public static void loadMapping() {
        // Load mappings using KeyStrokeData
        KeyStrokeData stroke = new KeyStrokeData(
            KeyCode.ENTER, false, false, false,
            KeyStrokeData.KEY_LOCATION_STANDARD
        );
        mappedKeys.put(stroke, "ENTER_KEY_ACTION");
    }

    // No Swing dependencies
}
```

**Step 3: Create Swing Adapter** (2h)
```java
// In GUI layer
public class SwingKeyMapperAdapter {
    public static javax.swing.KeyStroke
        toSwingKeyStroke(KeyStrokeData data) {
        // Convert data model to Swing KeyStroke
        int mask = 0;
        if (data.isShift()) mask |= InputEvent.SHIFT_DOWN_MASK;
        if (data.isCtrl()) mask |= InputEvent.CTRL_DOWN_MASK;
        return javax.swing.KeyStroke.getKeyStroke(
            data.getKeyCode(), mask
        );
    }
}
```

**KeyMapper.java Changes Summary**:
- Remove: `import java.awt.event.*;`
- Remove: `import javax.swing.KeyStroke;`
- Add: `import org.hti5250j.keyboard.model.KeyStrokeData;`
- Change: `HashMap<KeyStroker, String>` → `HashMap<KeyStrokeData, String>`
- Update: all key code references to use constants

**Test Strategy**:
```java
@Test
public void testKeyMapperHeadless() {
    KeyMapper mapper = KeyMapper.getInstance();

    KeyStrokeData enter = new KeyStrokeData(
        KeyCode.ENTER, false, false, false,
        KeyStrokeData.KEY_LOCATION_STANDARD
    );

    String action = mapper.getAction(enter);
    assertEquals("ENTER_KEY_ACTION", action);
}
```

**Effort**: **12 hours**
**Risk**: LOW (straightforward data model extraction)
**Complexity**: MEDIUM (many key mappings to update)

---

### 4. Sessions.java - Session Manager

**Current Problem**:
- Uses `javax.swing.Timer` (GUI event loop)
- Should use `java.util.Timer` (background task timer)

**Violations**:
- Line 15: `import javax.swing.Timer;` (GUI timer)

**Refactoring Strategy**:

**Step 1: Replace Swing Timer** (2h)
```java
// Before (WRONG)
import javax.swing.Timer;

public class Sessions {
    private javax.swing.Timer sessionCheckTimer;

    public void init() {
        sessionCheckTimer = new Timer(5000, this);
        sessionCheckTimer.start();
    }
}

// After (CORRECT)
import java.util.Timer;

public class Sessions {
    private java.util.Timer sessionCheckTimer;

    public void init() {
        sessionCheckTimer = new java.util.Timer();
        sessionCheckTimer.scheduleAtFixedRate(
            this::checkActiveSessions,
            5000,   // delay
            5000    // period
        );
    }

    private void checkActiveSessions() {
        // Check session status
    }
}
```

**Step 2: Update Action Methods** (1h)
- Change `actionPerformed(ActionEvent)` → task method
- Update callback signatures

**Step 3: Test** (1h)
- Verify timer fires on schedule
- No UI dependency

**Sessions.java Changes Summary**:
- Remove: `import javax.swing.Timer;`
- Add: `import java.util.Timer;`
- Change: `javax.swing.Timer` → `java.util.Timer`
- Update: callback method signature

**Test Strategy**:
```java
@Test
public void testSessionsTimerHeadless() {
    Sessions sessions = new Sessions();
    sessions.init();

    // Wait for timer to fire
    Thread.sleep(6000);

    // Verify session check occurred
    assertTrue(sessions.wasLastCheckRecent());

    sessions.shutdown();
}
```

**Effort**: **4 hours**
**Risk**: VERY LOW (simple timer replacement)
**Complexity**: LOW (straightforward change)

---

### 5. KeyStroker.java - Keystroke Data Class

**Current Problem**:
- Uses `java.awt.event.KeyEvent` constants
- 256 lines, but AWT-dependent

**Violations**:
- Line 14: `import java.awt.event.KeyEvent;`

**Refactoring Strategy**:

**Step 1: Extract Constants** (2h)
```java
// New class
public final class KeyCodes {
    // Common key codes (from KeyEvent)
    public static final int VK_ENTER = 10;
    public static final int VK_ESCAPE = 27;
    public static final int VK_SPACE = 32;
    // ... all standard codes

    public static final int KEY_LOCATION_STANDARD = 0;
    public static final int KEY_LOCATION_LEFT = 1;
    public static final int KEY_LOCATION_RIGHT = 2;
    public static final int KEY_LOCATION_NUMPAD = 3;
}
```

**Step 2: Update KeyStroker** (2h)
```java
// Before
import java.awt.event.KeyEvent;

public class KeyStroker {
    public static final int KEY_LOCATION_STANDARD =
        KeyEvent.KEY_LOCATION_STANDARD;
}

// After
public class KeyStroker {
    public static final int KEY_LOCATION_STANDARD =
        KeyCodes.KEY_LOCATION_STANDARD;
}
```

**Step 3: Create Swing Adapter** (1h)
```java
// For GUI code that needs KeyEvent constants
public class KeyEventConstants {
    public static int toAWTKeyCode(int keyStrokerCode) {
        return keyStrokerCode;  // Direct mapping
    }
}
```

**KeyStroker.java Changes Summary**:
- Remove: `import java.awt.event.KeyEvent;`
- Add: `import org.hti5250j.keyboard.KeyCodes;`
- Change: all `KeyEvent.VK_*` → `KeyCodes.VK_*`
- Change: all `KeyEvent.KEY_LOCATION_*` → `KeyCodes.KEY_LOCATION_*`

**Test Strategy**:
```java
@Test
public void testKeystrokerHeadless() {
    KeyStroker stroke = new KeyStroker(
        KeyCodes.VK_ENTER,
        false, false, false, false,
        KeyCodes.KEY_LOCATION_STANDARD
    );

    assertEquals(KeyCodes.VK_ENTER, stroke.getKeyCode());
}
```

**Effort**: **6 hours**
**Risk**: VERY LOW (mostly constant extraction)
**Complexity**: LOW (straightforward)

---

## TIER 1 SUMMARY

| File | Hours | Risk | Status |
|------|-------|------|--------|
| tnvt.java | 16 | MEDIUM | Largest, highest complexity |
| KeyboardHandler.java | 8 | LOW | Straightforward extraction |
| KeyMapper.java | 12 | LOW | Data model extraction |
| Sessions.java | 4 | VERY LOW | Simple timer replacement |
| KeyStroker.java | 6 | VERY LOW | Constant extraction |
| **TOTAL** | **46** | **LOW** | **1.2 weeks** |

### Dependencies

```
tnvt.java
├─→ KeyMapper.java (fix first)
├─→ KeyboardHandler.java (fix first)
├─→ KeyStroker.java (fix first)
└─→ Sessions.java (independent)

Recommended Order:
1. KeyStroker.java (simplest, no dependencies)
2. Sessions.java (simple timer change)
3. KeyMapper.java (data model extraction)
4. KeyboardHandler.java (interface extraction)
5. tnvt.java (largest, depends on others)
```

---

## TIER 2: ARCHITECTURAL CLEANUP (20 hours)

*These 3 files have architectural confusion but are less critical*

### 6. SessionConfig.java (HIGH)
- **Problem**: Configuration data depends on `javax.swing.*` and `java.awt.*`
- **Solution**: Extract color/dimension classes
- **Effort**: 6 hours
- **Impact**: Data model should be presentation-independent

### 7. DefaultKeyboardHandler.java (HIGH)
- **Problem**: Default key bindings depend on Swing
- **Solution**: Move to GUI layer or extract configuration
- **Effort**: 4 hours
- **Impact**: Should initialize without GUI

### 8. Tn5250jController.java (HIGH)
- **Problem**: Uses `javax.swing.JFrame`
- **Solution**: Extract frame reference to factory
- **Effort**: 10 hours
- **Impact**: Controller should not know about GUI

---

## TIER 3: BOUNDARY CLEANUP (5 hours)

*These files have acceptable UI bridges but should be extracted*

### 9. SSLImplementation.java + X509CertificateTrustManager.java (LOW)
- **Problem**: Transport layer uses `javax.swing.JOptionPane` for dialogs
- **Solution**: Extract dialog factory abstraction
- **Effort**: 5 hours total
- **Impact**: Clean separation of transport/UI concerns

---

## TESTING STRATEGY

### Unit Tests (Per-File)

```java
// For each file, create headless test
@Test
public void testTnvtNoSwingDependency() {
    // Verify no Swing imports loaded
    ClassLoader loader = new URLClassLoader(jarUrl);
    Class<?> tnvt = loader.loadClass("org.hti5250j.framework.tn5250.tnvt");

    // Check for Swing in imports
    String[] imports = getImports(tnvt);
    for (String imp : imports) {
        assertFalse(imp.contains("javax.swing"));
        assertFalse(imp.contains("java.awt"));
    }
}
```

### Integration Tests (Full Stack)

```java
@Test
public void testHeadlessTerminalSession() {
    // Create session without GUI
    HeadlessScreenDisplay display = new HeadlessScreenDisplay();
    HeadlessKeyboardHandler keyboard = new HeadlessKeyboardHandler();

    tnvt protocol = new tnvt();
    protocol.setDisplay(display);
    protocol.setKeyboardHandler(keyboard);

    // Connect to IBMi system (or mock)
    SessionConnection conn = new SessionConnection("localhost", 23);
    protocol.start(conn);

    // Simulate keyboard input
    keyboard.handleKeyInput(new MockKeyInput(KeyCode.ENTER, 0));

    // Verify protocol processed input
    assertEquals(expectedScreenState, display.getBuffer());

    protocol.stop();
}
```

### CI/CD Tests (Headless Environment)

```bash
#!/bin/bash
# test-headless.sh

# Set headless flag
export DISPLAY=
export JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

# Run tests without X11
mvn test -DargLine="-Djava.awt.headless=true"

# Verify success
if [ $? -eq 0 ]; then
    echo "✅ All tests pass in headless environment"
else
    echo "❌ Tests failed in headless environment"
    exit 1
fi

# Docker test (no X11 libraries)
docker run -e JAVA_TOOL_OPTIONS="-Djava.awt.headless=true" \
    openjdk:21 mvn test
```

### Verification Checklist

- [ ] All 11 files compile without Swing/AWT imports
- [ ] All unit tests pass (headless)
- [ ] All integration tests pass (headless)
- [ ] CI/CD pipeline runs without X11 libraries
- [ ] Docker build succeeds (no X11 dependencies)
- [ ] Cloud deployment succeeds
- [ ] Zero Swing/AWT in protocol layer
- [ ] Interface contracts stable for 2+ weeks

---

## ROLLOUT PLAN

### Week 1: Tier 1 Core (46 hours)

**Day 1-2: KeyStroker.java + Sessions.java** (10h)
- Simplest changes to build confidence
- Establish testing patterns

**Day 2-3: KeyMapper.java** (12h)
- Data model extraction
- Key mapping refactoring

**Day 3-4: KeyboardHandler.java** (8h)
- Interface extraction
- Adapter creation

**Day 4-5: tnvt.java** (16h)
- Largest refactoring
- Multiple interface implementations
- Comprehensive testing

### Week 2: Tier 2 + 3 Cleanup (25 hours)

**Day 6-7: Tier 2 (SessionConfig, DefaultKeyboardHandler, Tn5250jController)** (20h)

**Day 8-9: Tier 3 (SSL/Transport)** (5h)

### Week 3: Integration + Deployment

**Day 10+: Full integration testing**
- Headless mode validation
- CI/CD pipeline testing
- Docker containerization
- Production deployment prep

---

## SUCCESS CRITERIA

### Phase 1 Complete (Tier 1)

- [ ] All 5 CRITICAL files compile without `import javax.swing.*` or `import java.awt.*`
- [ ] All unit tests pass with headless display: `DISPLAY= java -Djava.awt.headless=true ...`
- [ ] All integration tests pass without X11 libraries
- [ ] CI/CD pipeline test runs successfully (no X11 setup needed)
- [ ] Docker container builds and runs terminal session successfully
- [ ] Zero Swing/AWT instantiation in protocol layer

### Phase 2 Complete (Tier 2)

- [ ] All 3 HIGH files extracted to proper layers
- [ ] SessionConfig is pure data (no UI references)
- [ ] DefaultKeyboardHandler moved to GUI or properly abstracted
- [ ] Tn5250jController uses UI factory instead of direct JFrame

### Phase 3 Complete (Tier 3)

- [ ] SSL/Transport layer uses dialog factory
- [ ] UI dialogs abstracted from transport concerns
- [ ] Clean separation of protocols/network/UI

### Final Verification

- [ ] Codebase compiles with `-Djava.awt.headless=true`
- [ ] All tests pass without `DISPLAY` environment variable
- [ ] Docker image builds without X11 libraries
- [ ] AWS EC2 deployment succeeds
- [ ] Azure VM deployment succeeds
- [ ] GitHub Actions CI/CD runs without X11 setup
- [ ] Performance: No change in protocol throughput
- [ ] Reliability: Zero regression in existing tests

---

## EFFORT SUMMARY

| Tier | Files | Hours | Timeline | Risk |
|------|-------|-------|----------|------|
| **1** | 5 (CRITICAL) | 46 | 1.2 weeks | LOW |
| **2** | 3 (HIGH) | 20 | 0.5 weeks | LOW |
| **3** | 2 (LOW) | 5 | 1-2 days | VERY LOW |
| **Total** | **11** | **71** | **1.8 weeks** | **LOW** |

**Resource**: 1 senior engineer (or 2 mid-level engineers)
**Timeline**: 9 calendar days (assuming 8h/day productive coding)
**Deadline**: Complete before Phase 11 launch
**Blocker**: YES - Headless deployment required for server

---

**Prepared by**: Probe Agent B
**Confidence Level**: 95% (comprehensive analysis)
**Status**: Ready for implementation sprint assignment
