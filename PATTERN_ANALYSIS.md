# HTI5250J Code Pattern Analysis Report

**Date:** February 9, 2026
**Scope:** Full codebase (290 source files, 174 test files)
**Focus:** Design patterns, anti-patterns, and architectural barriers to Robot Framework/Python integration

---

## Executive Summary

The HTI5250J codebase demonstrates **mature architectural separation** with strong contract-based testing and modern Java 21 patterns. However, **critical barriers exist for external tool integration**:

1. **Monolithic core classes** (Screen5250: 3,411 lines; tnvt: 2,555 lines) create interface complexity
2. **Implicit GUI coupling** in core protocols (tnvt.java imports javax.swing)
3. **No dedicated adapter layer** for programmatic access (only TerminalAdapter for CLI)
4. **SessionConfig property exposure** bypasses abstraction (Phase 3 migration incomplete)
5. **Limited streaming/functional API** adoption (6 stream() calls in 290 files)

**Recommendation:** Build a dedicated **PythonBridge Adapter** layer that exports sealed interface hierarchy via reflection-free factories before exposing to Robot Framework.

---

## Part 1: Design Pattern Inventory

### Good Patterns (Implemented Correctly)

| Pattern | Location | Assessment | Impact |
|---------|----------|-----------|--------|
| **Factory (Sealed)** | `ActionFactory.java` | Excellent - sealed interface + switch expression | ✅ Type-safe, compile-time exhaustive |
| **Factory (Codec)** | `ToolboxCodePageFactory.java` `BuiltInCodePageFactory.java` | Dual-factory with strategy selection | ✅ Extensible charset handling |
| **Observer/Listener** | `SessionListener`, `ScreenListener` interfaces | Thread-safe with ReadWriteLock in SessionConfig | ✅ Proper concurrency (63 listeners) |
| **Sealed Types** | `Action` interface (7 implementations) | Perfect compile-time safety | ✅ Pattern matching in handlers |
| **Virtual Threads** | `BatchExecutor`, `WorkflowRunner` | Phase 13 complete: 300× throughput | ✅ I/O scalability verified |
| **Singleton (Controlled)** | `HTI5250jLogFactory`, `OperatingSystem` | Private constructors, thread-safe getInstance | ✅ Minimal exposure (30 instances) |
| **Adapter** | `TerminalAdapter` (CLI output), `CodepageConverterAdapter` | Functional encapsulation | ✅ Good separation of concerns |
| **Contract Testing** | `IBMiConnectionFactoryContractTest`, `Screen5250ContractTest` | 254 contract tests (Phases 1-4) | ✅ Drift detection vs real i5 |

### Problematic Patterns (Blocking External Integration)

| Pattern | Location | Issue | Severity | Impact |
|---------|----------|-------|----------|--------|
| **God Object** | `Screen5250.java` (3,411 lines, 122 methods) | Handles rendering, state, I/O, event dispatch | **CRITICAL** | Python caller must understand entire screen model |
| **God Object** | `tnvt.java` (2,555 lines, unclear method count) | Protocol parsing + javax.swing coupling | **CRITICAL** | GUI dependencies prevent headless library use |
| **GUI Coupling** | `tnvt.java:238,290` calls `SwingUtilities.invokeAndWait()` | Core protocol logic bound to Swing thread | **CRITICAL** | Robot Framework can't run without GUI event loop |
| **Property Exposure** | `SessionConfig.sesProps` (private field, public getter) | Direct property access bypasses validation | **HIGH** | Phase 3 migration not complete; callers use `.getSesProps().getProperty()` |
| **Reflection Hell** | `ActionFactory.from()`, validators use `getAction()` | Type coercion via string method names | **MEDIUM** | Dynamic dispatch is fragile; sealed classes partially address |
| **Static Initialization** | `JPythonInterpreterDriver` (static {}, Singleton pattern) | Jython cache & singleton creation in class init | **MEDIUM** | Can't reload; blocks parallel test execution |
| **Missing Layer** | No `PythonBridge` or `RobotFrameworkAdapter` | External tools must couple directly to core | **CRITICAL** | Python/Robot Framework forced to import Java deeply |

---

## Part 2: Anti-Pattern Locations

### Technical Debt (TODO/FIXME/HACK Comments)

| Severity | Count | Top Issues |
|----------|-------|-----------|
| **TODO** | 17 | See Table Below |
| **XXX/HACK** | 2 | tnvt.java:598 (clear dataqueue check), XTFRFile.java:978 (field descriptor state) |
| **FIXME** | 0 | |

#### TODO Details (Blocking Automation)

| File | Line | Issue | Impact on Robot Framework |
|------|------|-------|---------------------------|
| `SessionConfig.java` | 424 | "TODO: refactor all former usages which access properties directly" | **Blocker** - Confirms sesProps exposure problem |
| `Screen5250.java` | 364 | "TODO: implement nonDisplay check" | Display attribute handling incomplete |
| `Screen5250.java` | 2040, 2065 | "TODO: implement underline check" (x2) | Attribute rendering gaps |
| `SessionConfigPairwiseTest.java` | 276, 295, 351, 408, 426 | Validation gaps (port bounds, host/port pairing, font size) | Weak input validation for programmatic API |
| `tnvt.java` | 598 | "XXX: Not sure if this is sufficient check for 'clear dataqueue'" | Unclear protocol logic |
| `TN5250EProtocolPairwiseTest.java` | 327 | Device name protocol limit (10 chars) | Edge case in external data binding |
| `EBCDICPairwiseTest.java` | 317 | "TODO: Implement character validation in CodepageConverterAdapter.uni2ebcdic()" | Charset conversion may be incomplete |
| `XTFRFile.java` | 978 | "TODO: save FieldDesc state as single property" | File transfer state serialization incomplete |

### Exception Handling Anti-Patterns

```
Total broad throws Exception: 384 instances
```

**Problem:** Many methods throw bare `Exception` rather than specific types.

**Impact:** Python/Robot Framework can't distinguish recoverable errors from fatal ones.

**Example:** `Screen5250.java` methods throw generic Exception; callers can't determine:
- Connection timeout vs field validation failure
- Transient i5 unavailability vs bad workflow definition

### Missing Null Checks (592 instances)

**Assessment:** Healthy number of defensive checks, but no consistent pattern:
- Some use `== null` (old style)
- Others use `Objects.requireNonNull()` (modern)
- No use of Optional<T> or `@Nullable` annotations

**Impact for Python:** Python callers will receive bare NullPointerExceptions with poor diagnostics.

### Insufficient Streaming API (6 stream() calls, 290 files)

**Finding:** Almost no functional/stream-based collection processing.

**Examples:**
- Manual for loops dominate (`ScreenFields.java`, `WTDSFParser.java`)
- No use of `.filter()`, `.map()`, or `.collect()`
- Array iteration 50+ times with index-based access

**Impact:** Python libraries that expect iterator protocols will hit impedance mismatches.

---

## Part 3: Architectural Boundary Violations

### Layer Diagram (Current)

```
┌─────────────────────────────────────────┐
│  GUI Layer (My5250, SessionPanel)       │  ← Swing/AWT coupling
├─────────────────────────────────────────┤
│  Framework Layer (Screen5250, tnvt)     │  ← VIOLATION: javax.swing imports
├─────────────────────────────────────────┤
│  Protocol Layer (DataStream, WTD)       │
├─────────────────────────────────────────┤
│  Transport Layer (IBMiConnectionFactory)│
├─────────────────────────────────────────┤
│  Encoding Layer (CharMappings, CCSID*)  │
└─────────────────────────────────────────┘
          ↕ EXTERNAL COUPLING
   Robot Framework (Python)
```

### Cross-Layer Dependencies Found

| Boundary | Violation | Files | Risk |
|----------|-----------|-------|------|
| **Framework → GUI** | `tnvt.java` imports `javax.swing.*` | 1 | **CRITICAL** - Core protocol layer assumes GUI thread |
| **Framework → GUI** | `tnvt.java:238,290` calls `SwingUtilities.invokeAndWait()` | 1 | **CRITICAL** - Blocks on EDT, fails in headless |
| **Workflow → AWT** | `ArtifactCollector.java` imports `java.awt.image.BufferedImage` | 1 | LOW - For screenshot capture only |
| **Workflow → AWT** | `WorkflowRunner.java` imports `java.awt.image.BufferedImage` | 1 | LOW - Same reason |
| **SessionConfig → Business Logic** | Callers access `sesProps` directly (Phase 3 incomplete) | 40+ | HIGH - Property validation bypassed |

### Missing Abstraction Layers

| Missing Layer | Purpose | Why Needed | Impact |
|---------------|---------|-----------|--------|
| **PythonBridge** | Serialize sealed Actions, Screen state to JSON | No Python-friendly interface | Python must parse Java byte streams |
| **RobotFrameworkAdapter** | Keyword library exporting HTI5250J concepts | Robot Framework needs @keyword methods | Manual wrapper code required |
| **REST/gRPC Gateway** | Out-of-process access via HTTP | IPC isolation and language neutrality | Embedded Java-only access |
| **Configuration Validation Service** | SessionConfig properties → typed builder | Phase 3 incomplete | Weak input validation for programmatic API |

---

## Part 4: Interface Complexity Analysis

### Session5250 Interface (Public API Surface)

**Public Methods:** 35+ (from grep)

**Access Patterns:**
```
getScreen()           → Screen5250 (3,411 lines, 122 methods)
getVT()               → tnvt (2,555 lines, unclear methods)
getSessionManager()   → SessionManager (partial facade)
getConfiguration()    → SessionConfig (incomplete validation)
```

**Problem:** Session5250 is a thin wrapper that exposes two God Objects (Screen5250, tnvt).

### Screen5250 Interface (Internal but Exposed)

**Public Methods:** 122
**LOC:** 3,411 (9.6× larger than recommended 350 LOC)

**Method Categories (estimated):**
- Rendering: 25+ methods
- State management: 30+ methods
- I/O handling: 20+ methods
- Event dispatch: 15+ methods
- Protocol parsing: 15+ methods
- Utility/internal: 17+ methods

**For Python:**
- No method naming consistency (camelCase mixed with underscores in data)
- No documented preconditions/postconditions
- Many methods have side effects (state mutations)
- No clear "read-only" vs "mutating" separation

### Sealed Action Hierarchy (Well-Designed)

**Positive:**
```
Action (sealed interface)
  ├─ LoginAction(host, user, password)
  ├─ NavigateAction(screen, keys)
  ├─ FillAction(fields, timeout)
  ├─ SubmitAction(key)
  ├─ AssertAction(text, screen)
  ├─ WaitAction(timeout)
  └─ CaptureAction(name)
```

**Pattern Matching in Handlers:**
```java
return switch (action) {
    case LoginAction a -> handler.handle(a);
    case NavigateAction a -> handler.handle(a);
    // ... exhaustive
};
```

**This IS exportable to Python** — just needs JSON serialization layer.

---

## Part 5: Code Duplication Patterns

### Search Strategy Results

**Output Filter Duplication:** 5 files with similar patterns
- `HTMLOutputFilter.java` (~200 LOC)
- `ExcelOutputFilter.java` (~250 LOC)
- `DelimitedOutputFilter.java` (~180 LOC)
- `FixedWidthOutputFilter.java` (~150 LOC)
- `OpenOfficeOutputFilter.java` (465 LOC - largest!)

**Common Loop Pattern (10+ files):**
```java
for (int x = 0; x < ffd.size(); x++) {
    f = (FileFieldDef) ffd.get(x);  // Index-based access, manual cast
    // ... process field
}
```

**Better as:**
```java
for (FileFieldDef f : ffd) {
    // ... process field
}
```

**CCSID Codec Duplication:** 22 codepage files (CCSID*.java)
- Pattern: static final byte[] CHARMAP with 256+ entries each
- Potential: Factored into data-driven factory (Phase 2 addressed partially)

### Duplication Severity

| Type | Count | LOC Impact | Priority |
|------|-------|-----------|----------|
| Output Filter Loop Pattern | 5 files | ~80 LOC | MEDIUM - low risk |
| CCSID Charmap (data) | 22 files | ~5000 LOC | LOW - metadata, not logic |
| Field Descriptor Access | 10+ files | ~50 LOC | LOW - business logic differs |

**For Robot Framework:** Duplication doesn't directly block integration but indicates code smell that reduces maintainability.

---

## Part 6: Naming Consistency Analysis

### Consistent Naming (Good)

| Category | Pattern | Example |
|----------|---------|---------|
| Action classes | `*Action` | `LoginAction`, `SubmitAction` |
| Factory classes | `*Factory` | `ActionFactory`, `IBMiConnectionFactory` |
| Validator classes | `*Validator` | `StepValidator`, `ParameterValidator` |
| Listener interfaces | `*Listener` | `SessionListener`, `ScreenListener` |
| Test classes | `*Test` or `*PairwiseTest` | `SessionConfigPairwiseTest` |
| CCSID codepages | `CCSID[number]` | `CCSID277`, `CCSID930` |

### Inconsistent Naming (Issues)

| Category | Inconsistency | Impact |
|----------|---------------|--------|
| **Properties** | sesProps, screenProps, configProps, guiProps | Inconsistent getter names: `getSesProps()` vs `getSessionConfig()` |
| **Method verbs** | `get*`, `fetch*`, `load*`, `read*` | No semantic distinction between cached vs fresh |
| **Boolean methods** | `isConnected()`, `isSslSocket()`, `isScanningEnabled()` | Mostly consistent, but some use `should*` |
| **Listener callbacks** | `fireSessionChanged()`, `fireScanned()` (verbs vary) | Inconsistent naming of event dispatch methods |
| **Constants** | `YES`, `NO`, `CONFIG_KEYPAD_ENABLED` | Mix of magic strings and constants |

### Acronyms (Unclear for External Users)

| Acronym | Meaning | Files | Clarity |
|---------|---------|-------|---------|
| **WTD** | Write To Display (protocol) | WTDSFParser.java | Obscure for Python users |
| **SF** | Structured Field | SFCommand, WTDSFParser | Protocol domain knowledge required |
| **OIA** | Operator Information Area | ScreenOIA.java | Standards knowledge required |
| **EBCDIC** | Extended Binary Coded Decimal | encoding/ | Standard but complex for Python |
| **tnvt** | TN5250 Virtual Terminal | tnvt.java | Historical; could be Tn5250VirtualTerminal |

---

## Part 7: Coupling Analysis

### High Coupling Points

| Coupling Type | Location | Risk | Impact on Integration |
|---------------|----------|------|----------------------|
| **SessionConfig ↔ Properties** | `SessionConfig.sesProps` exposed via public getter | HIGH | Phase 3 migration incomplete; direct property access is leaky abstraction |
| **Screen5250 ↔ tnvt** | Bidirectional dependency | MEDIUM | Both are 2000+ LOC; hard to test in isolation |
| **tnvt ↔ GUI** | `SwingUtilities.invokeAndWait()` calls | **CRITICAL** | Incompatible with headless Robot Framework environment |
| **WorkflowExecutor ↔ Session5250** | Direct method calls, no abstraction | MEDIUM | Difficult for external orchestration tools |
| **Keyboard ↔ GUI** | `KeyboardHandler` depends on Swing components | MEDIUM | Can't reuse keyboard logic in headless tests |

### Low Coupling (Positive)

| Layer | Coupling | Assessment |
|-------|----------|-----------|
| **Transport** | `IBMiConnectionFactory` | Good abstraction; decouples socket details |
| **Encoding** | Factory pattern for charsets | Extensible; no hardcoding in core |
| **Workflow Actions** | Sealed interface + pattern matching | Type-safe, minimal coupling |
| **Event System** | Listener pattern with ReadWriteLock | Thread-safe, decoupled updates |

---

## Part 8: Severity Assessment for Robot Framework Integration

### Critical Blockers (MUST FIX)

| Issue | Location | Fix Effort | Recommendation |
|-------|----------|-----------|-----------------|
| **tnvt.java GUI coupling** | Lines 238, 290 | **HIGH (3 days)** | Extract protocol logic to new `Tn5250Protocol` class; keep GUI dispatch in separate handler |
| **Screen5250 God Object** | Full class (3,411 lines) | **CRITICAL (2 weeks)** | Refactor into `ScreenRenderEngine` (render), `ScreenBuffer` (state), `ScreenEventBroker` (events) |
| **SessionConfig property exposure** | Phase 3 incomplete | **MEDIUM (2 days)** | Complete Phase 3 migration; add `SessionConfigBuilder` with validation |
| **No PythonBridge adapter** | Missing entirely | **MEDIUM (3 days)** | Create sealed interface serializers (JSON/MessagePack) |

### High Priority (Should Fix)

| Issue | Location | Fix Effort | Recommendation |
|-------|----------|-----------|-----------------|
| **Exception handling** | 384 throws Exception | **MEDIUM (1 week)** | Create `WorkflowException`, `ProtocolException`, `ConnectionException` hierarchy |
| **Interface documentation** | Screen5250, tnvt | **LOW (3 days)** | Add Javadoc with preconditions, postconditions, side effects |
| **Missing REST/gRPC** | Entire layer | **HIGH (2 weeks)** | Build gRPC service wrapping Workflow API; unblock multiple test frameworks |

### Medium Priority (Nice to Have)

| Issue | Location | Fix Effort | Recommendation |
|------|----------|-----------|-----------------|
| Stream API adoption | Throughout | LOW (5 days) | Adopt .stream() for collection operations; improves readability |
| CCSID factorization | encoding/ | LOW (2 days) | Move charmaps to properties/data files; reduce duplication |
| Builder pattern for ActionFactory inputs | workflow/ | LOW (1 day) | Better ergonomics for workflow YAML parsing |

---

## Part 9: Design Pattern Recommendations

### Recommended Pattern Shifts for External Integration

#### 1. Extract Adapter Layer (PythonBridge)

**Current:**
```
Robot Framework → (direct Java coupling) → Session5250 → Screen5250
```

**Recommended:**
```
Robot Framework → PythonBridge (JSON) → WorkflowAPI → Session5250
```

**Implementation:**
```java
public sealed interface WorkflowAPIEvent permits
    SessionConnectedEvent,
    ScreenRenderedEvent,
    WorkflowCompletedEvent { }

public class WorkflowAPIAdapter {
    public static String serialize(WorkflowAPIEvent event) {
        return switch (event) {
            case SessionConnectedEvent e -> toJson(e);
            case ScreenRenderedEvent e -> toJson(e);
            case WorkflowCompletedEvent e -> toJson(e);
        };
    }
}
```

#### 2. Decouple tnvt from GUI (Extract Protocol Handler)

**Current:**
```java
// In tnvt.java
SwingUtilities.invokeAndWait(() -> { /* render */ });
```

**Recommended:**
```java
// In Tn5250Protocol.java (headless)
protocol.parse(bytes) → ProtocolEvent

// In Tn5250GUIHandler.java (GUI-specific)
event -> SwingUtilities.invokeAndWait(() -> render());
```

#### 3. Introduce Configuration Builder (Complete Phase 3)

**Current:**
```java
SessionConfig config = new SessionConfig("default.props", "session1");
config.getSesProps().getProperty("host");  // Leaky!
```

**Recommended:**
```java
SessionConfig config = SessionConfigBuilder
    .withDefaults()
    .host("192.168.1.1")
    .port(23)
    .sslEnabled(true)
    .validateAndBuild();  // Throws checked exception

// No .sesProps exposure
String host = config.getHost();
```

#### 4. Introduce gRPC Service Layer

**Benefits for Robot Framework:**
- Language-neutral RPC interface
- Connection pooling + load balancing
- Streaming support for long-running workflows
- Built-in authentication/TLS

```protobuf
service HTI5250jWorkflow {
    rpc Connect(SessionRequest) returns (SessionResponse);
    rpc SendKeys(KeyRequest) returns (ScreenResponse);
    rpc RunWorkflow(WorkflowRequest) returns (stream WorkflowEvent);
}
```

#### 5. Establish Clear Exception Hierarchy

**Current:** 384 methods throw bare `Exception`

**Recommended:**
```java
public sealed class WorkflowException extends Exception
    permits ConnectionException,
            ProtocolException,
            ValidationException,
            TimeoutException { }

public class ConnectionException extends WorkflowException {
    private final int retryCount;
    private final Duration nextRetryIn;
}
```

---

## Part 10: Pattern Metrics Summary

| Metric | Current | Target | Status |
|--------|---------|--------|--------|
| **God Objects** (>1000 LOC) | 4 (Screen5250, tnvt, GuiGraphicBuffer, GUIGraphicsUtils) | 0 | ❌ Needs refactoring |
| **Circular Dependencies** | 0 detected | 0 | ✅ Good |
| **Exception Specificity** | Generic `Exception` (384×) | Sealed hierarchy | ❌ Needs hierarchy |
| **Stream API Adoption** | 6 instances / 290 files (2%) | 50%+ | ❌ Low functional style |
| **GUI Coupling** | tnvt imports javax.swing | 0 | ❌ Critical blocker |
| **Singleton Count** | 30 | <10 | ⚠️ Acceptable but high |
| **Factory Pattern Usage** | 27 files | Maintained | ✅ Well-distributed |
| **Interface Segregation** | Mixed (large interfaces like Screen5250) | Smaller cohesive interfaces | ⚠️ Needs refactoring |
| **Sealed Types** | 1 (Action) | 5+ (Action, Exception, Event, etc.) | ❌ Underutilized |
| **Contract Testing** | 254 tests (Phases 1-4) | 100% coverage on public APIs | ⚠️ Good but incomplete |

---

## Part 11: Robot Framework Integration Roadmap

### Phase A: Blocking Fixes (Weeks 1-2)

1. **Extract Tn5250Protocol from tnvt.java**
   - Move byte parsing logic to protocol-only class
   - Keep SwingUtilities coupling in separate GUI handler
   - Enable headless testing

2. **Complete SessionConfig Phase 3 Migration**
   - Add SessionConfigBuilder with validation
   - Mark sesProps as deprecated (private next release)
   - Add property-specific getters (getHost(), getPort(), etc.)

3. **Create Exception Hierarchy**
   - Sealed `WorkflowException` with subtypes
   - Proper error codes and diagnostics for Python

### Phase B: Adapter Layer (Weeks 3-4)

4. **Build PythonBridge**
   - Sealed interface JSON serializers
   - Action serialization (YAML→Java→JSON)
   - Screen state snapshots

5. **Create WorkflowAPIService**
   - Session management facade
   - Unified exception handling
   - Event streaming interface

### Phase C: External Integration (Weeks 5-6)

6. **Develop gRPC Service**
   - Multi-process isolation
   - Language-neutral interface
   - Connection pooling

7. **Robot Framework Keyword Library**
   - Connect, SendKeys, WaitForScreen
   - RunWorkflow, Capture, Assert
   - Data-driven test support

### Phase D: Polish (Week 7)

8. **Documentation + Examples**
   - Robot Framework keyword reference
   - Python client examples
   - Integration testing patterns

---

## Conclusions

### Strengths
- ✅ Mature sealed-type usage (Action hierarchy)
- ✅ Strong contract testing foundation (254 tests)
- ✅ Excellent factory pattern consistency
- ✅ Thread-safe listener patterns with locks
- ✅ Virtual thread integration (Phase 13)

### Critical Weaknesses
- ❌ **tnvt.java imports javax.swing (core protocol layer)**
- ❌ **Screen5250 is 3,411 lines (9.6× recommended max)**
- ❌ **SessionConfig.sesProps leaked to callers (Phase 3 incomplete)**
- ❌ **No dedicated adapter layer for external tools**
- ❌ **Exception handling is too generic (384 throws Exception)**

### Adversarial Question Answered

**"What patterns prevent Robot Framework and Python adapter integration?"**

1. **GUI coupling in core protocol** (tnvt.java) — SwingUtilities.invokeAndWait() blocks headless execution
2. **Leaky abstractions** (SessionConfig.sesProps) — Property access bypasses validation
3. **No serialization layer** — No JSON/gRPC bridge for out-of-process RPC
4. **God objects** (Screen5250, tnvt) — 3400+ lines each; hard to understand for Python callers
5. **Generic exceptions** — Python can't distinguish transient vs fatal errors
6. **Stateful singletons** — Jython interpreter locked to static initializer; can't reset between test runs

### Recommendation

**Build a three-layer strategy:**

1. **Separation:** Extract headless protocol logic from GUI (2 weeks)
2. **Bridging:** Create sealed-type serializers + PythonBridge (3 days)
3. **Access:** Expose via gRPC service for language-neutral RPC (2 weeks)

This preserves existing code while creating a clean boundary for Robot Framework integration.

