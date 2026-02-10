# HTI5250J Component Dependency Diagram

## Layer Stack (Current Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│ External Integration Layer                                   │
│ ┌────────────┐  ┌──────────────┐  ┌────────────────┐        │
│ │ JUnit Test │  │ Robot        │  │ Custom Python  │        │
│ │ Frameworks │  │ Framework    │  │ Integration    │        │
│ └──────┬──────┘  └──────┬───────┘  └────────┬───────┘        │
└────────┼──────────────────┼──────────────────┼────────────────┘
         │                  │                  │
         └──────────────────┼──────────────────┘
                            ▼
       ┌────────────────────────────────────────────┐
       │ PUBLIC API GATEWAY (Session5250)           │
       │                                            │
       │ ✗ PROBLEM: GUI Coupling                   │
       │   - imports: java.awt.Toolkit             │
       │   - imports: SystemRequestDialog          │
       │   - field: SessionPanel guiComponent      │
       │   - public: Properties sesProps (mutable) │
       │                                            │
       │ ALL external tools inherit coupling!      │
       └──────────────────┬─────────────────────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
         ▼                ▼                ▼
    ┌─────────────┐  ┌──────────────┐  ┌──────────┐
    │ SessionPanel│  │ Screen5250   │  │ SessionMgr
    │ (GUI)       │  │ (Display Buf)│  │          │
    │             │  │ ✓ Clean      │  │ ✓ Clean  │
    │ ✗ Couples   │  │ No GUI       │  │ No GUI   │
    │ to AWT      │  │              │  │          │
    └─────────────┘  └──────┬───────┘  └──────────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │ScreenField  │  │ ScreenOIA    │  │ Event Sys    │
    │ ✓ Clean     │  │ ✓ Clean      │  │ ✓ Clean      │
    └──────────────┘  └──────────────┘  └──────────────┘
                            │
         ┌──────────────────┘
         │
         ▼
    ┌──────────────────────────────┐
    │ tnvt (TN5250E Protocol)       │
    │ ✓ CLEAN - No GUI coupling    │
    │ • Virtual thread I/O         │
    │ • Telnet negotiation         │
    │ • Data stream parsing        │
    └──────────┬───────────────────┘
               │
         ┌─────┴─────┐
         │           │
         ▼           ▼
    ┌──────────────┐  ┌─────────────────┐
    │DataStream    │  │EBCDICCodec      │
    │Producer      │  │✓ Clean          │
    │✓ Clean       │  │Encoding/Decoding
    └──────────────┘  └─────────────────┘
               │
               ▼
    ┌──────────────────────────────┐
    │ IBM i (Port 23/992)          │
    │ 5250 Terminal Emulation      │
    └──────────────────────────────┘
```

---

## Coupling Graph: Current Problem

```
JAVA.AWT (ROOT SYSTEM LIBRARY)
    │
    ├─ java.awt.Toolkit
    ├─ java.awt.Frame
    ├─ java.awt.Component
    └─ java.awt.Dimension
         │
         ▼
┌────────────────────────────────────────┐
│ GUI LAYER (20 files, pkg: gui/*)       │
├────────────────────────────────────────┤
│ • SystemRequestDialog (direct AWT)    │
│ • SessionPanel (extends JPanel)       │
│ • GuiGraphicBuffer                    │
│ • GuiComponent                        │
└────────────┬─────────────────────────────┘
             │
    ┌────────┴──────────────────────────┐
    │                                   │
    ▼ (dependency)                      ▼ (dependency)
┌──────────────────┐          ┌──────────────────────┐
│ Session5250      │          │ SessionPanel         │
│ (PUBLIC API)     │◄─────────┤ (GUI CONCRETE)      │
│                  │ has-a    │                      │
│ ✗ imports:       │          └──────────────────────┘
│  - SystemRequest │                    ▲
│  - SessionPanel  │                    │
│  - java.awt.*    │              (all 42 files)
└────────┬─────────┘                    │
         │                              │
    ┌────┴─────────────────────────────┴─────────┐
    │                                             │
    │  42 NON-GUI FILES IMPORTING GUI            │
    │  • keyboard/actions/* (30+ files)          │
    │  • tools/* (8+ files)                      │
    │  • scripting/* (3+ files)                  │
    │                                             │
    └─────────────────────────────────────────────┘
                     │
    ┌────────────────┘
    │
    ▼
ALL EXTERNAL TOOLS INHERIT GUI COUPLING
    │
    ├─ Robot Framework (Jython) ✗ BLOCKED
    ├─ Python Integration ✗ BLOCKED
    ├─ Docker Headless ✗ FAILS
    └─ CI/CD Agents ✗ FAILS (Display initialization)
```

---

## Desired Architecture (Post-Refactoring)

```
JAVA.AWT (OPTIONAL, LAZY-LOADED)
    │
    ▼
┌────────────────────────────────────────┐
│ GUI LAYER (optional, pkg: gui/*)       │
├────────────────────────────────────────┤
│ • SystemRequestDialog                  │
│ • SessionPanel                         │
│ • GuiRequestHandler                    │
│ (only loaded if GUI mode selected)     │
└────────┬─────────────────────────────────┘
         │
         ▼
┌────────────────────────┐
│ RequestHandler         │
│ (Interface)            │
│ ✓ ABSTRACTION POINT   │
├────────────────────────┤
│ • GuiRequestHandler    │
│ • NullRequestHandler   │
│ • RobotRequestHandler  │
└────────────────────────┘
         ▲
         │ injected
         │
         ▼
┌────────────────────────────────────────┐
│ HeadlessSession (Interface)             │
│ ✓ PURE DATA TRANSPORT - NEW GATEWAY    │
├────────────────────────────────────────┤
│ • DefaultHeadlessSession (no GUI)      │
│ • RobotHeadlessSession (custom)        │
│ • MockHeadlessSession (testing)        │
└────────────────┬──────────────────────────┘
                 │
    ┌────────────┼──────────────────────┐
    │            │                      │
    ▼            ▼                      ▼
┌──────────┐  ┌──────────────┐  ┌──────────────┐
│ Screen   │  │ Session5250  │  │ SessionMgr   │
│ 5250     │  │ (Adapter)    │  │ ✓ Clean      │
│ ✓ Clean  │  │ delegates →  │  │              │
│          │  │ Headless     │  │              │
└──────────┘  └──────────────┘  └──────────────┘
    │                  │              │
    └──────────────────┼──────────────┘
                       │
                       ▼
                 ┌──────────────┐
                 │ tnvt         │
                 │ ✓ CLEAN      │
                 │ Protocol     │
                 └──────────────┘
```

---

## Dependency Import Frequency

```
Package / Class              Files Importing    Status
────────────────────────────────────────────────────────
java.awt.*                       42              ✗ HIGH RISK
org.hti5250j.SessionPanel        42              ✗ BLOCKER
org.hti5250j.gui.*               42              ✗ COUPLES
org.hti5250j.Session5250        290              ✓ OK (facade)
org.hti5250j.Screen5250          15              ✓ OK
org.hti5250j.tnvt               4               ✓ OK
org.hti5250j.event.*             8               ✓ OK
```

---

## Risk Matrix: Coupling Impact

```
┌──────────────────┬──────────┬──────────┐
│ Component        │ Risk     │ Impact   │
├──────────────────┼──────────┼──────────┤
│ java.awt imports │ CRITICAL │ CRITICAL │
│ SystemRequestDlg │ CRITICAL │ CRITICAL │
│ SessionPanel     │ HIGH     │ CRITICAL │
│ sesProps mutabil │ MEDIUM   │ MEDIUM   │
│ ScreenListener   │ LOW      │ LOW      │
│ Plugin System    │ LOW      │ LOW      │
└──────────────────┴──────────┴──────────┘
```

---

## Call Flow: Robot Framework Integration Problem

### Current (FAILS)

```
Robot Framework
    │ (Jython bridge)
    ▼
hti5250j.Session5250.<init>()
    │
    ├─ load class org.hti5250j.Session5250
    │  │
    │  ├─ execute static initializers
    │  │  │
    │  │  └─ import java.awt.Toolkit
    │  │     │
    │  │     ▼
    │  │  java.awt.Toolkit.initDisplay()
    │  │     │
    │  │     ├─ try to initialize X11 / Wayland
    │  │     │
    │  │     └─ FAILS in Docker/headless
    │  │        (no DISPLAY env var)
    │  │
    │  └─ ✗ ClassNotFoundException or NPE
    │
    └─ ✗ Session cannot be created
```

### Desired (SUCCEEDS)

```
Robot Framework
    │ (Jython bridge)
    ▼
hti5250j.headless.HeadlessSession.<init>()
    │
    ├─ load class org.hti5250j.headless.HeadlessSession
    │  │
    │  ├─ execute static initializers
    │  │  │
    │  │  └─ NO java.awt imports
    │  │     │
    │  │     ✓ No display initialization
    │  │
    │  ├─ inject RequestHandler
    │  │  │
    │  │  └─ NullRequestHandler (headless)
    │  │
    │  └─ ✓ Session created successfully
    │
    ├─ session.connect("ibmi.example.com:23")
    │  │
    │  └─ ✓ works in Docker
    │
    └─ ✓ Robot Framework can proceed
```

---

## Abstraction Inversion: Coupling Direction

### Current (Inverted)

```
┌─────────────────────────────────┐
│ External Tools (Robot, Python)  │
│ (depend on public API)          │
└────────────┬────────────────────┘
             │
             ▼ (must use)
        ┌─────────────────┐
        │ Session5250     │
        │ (depends on GUI)│
        └────────┬────────┘
                 │
                 ▼
           ┌──────────────┐
           │ GUI System   │
           │ (optional)   │
           └──────────────┘

PROBLEM: Tools forced to depend on optional GUI layer!
```

### Desired (Inverted Back)

```
┌──────────────┐
│ GUI System   │
│ (optional)   │
│ (depends on) │ ◄────────┐
└──────────────┘          │
                    ┌─────┴────────────┐
                    │ RequestHandler   │
                    │ (interface)      │
                    └─────┬────────────┘
                          │
                    ┌─────▼────────────┐
                    │ HeadlessSession  │
                    │ (interface)      │
                    └─────▲────────────┘
                          │
                          │ (depends on)
        ┌─────────────────┴────────────┐
        │                              │
        │ External Tools               │
        │ (Robot, Python, JUnit)       │
        │                              │
        └──────────────────────────────┘

DESIRED: Tools depend on pure interface, GUI optional!
```

---

## Package Structure: Pre-Refactoring

```
org/hti5250j/
├── Session5250.java                    ← GATEWAY (COUPLING)
├── SessionPanel.java                   ← GUI CONCRETE
│
├── framework/
│   ├── tn5250/
│   │   ├── Screen5250.java            ✓ Clean
│   │   ├── ScreenField.java           ✓ Clean
│   │   ├── ScreenOIA.java             ✓ Clean
│   │   └── tnvt.java                  ✓ Clean
│   │
│   ├── common/
│   │   └── SessionManager.java        ✓ Clean
│   │
│   └── transport/
│       ├── SSL/
│       └── (TCP transport)            ✓ Clean
│
├── gui/                                ← GUI LAYER (20 files)
│   ├── SystemRequestDialog.java
│   ├── SessionPanel.java
│   └── ...
│
├── workflow/                           ✓ CLEAN
│   ├── WorkflowRunner.java
│   ├── WorkflowCLI.java
│   ├── ScreenProvider.java
│   └── ...
│
├── keyboard/                           ← KEYBOARD (30+ files, GUI-dependent)
│   ├── actions/
│   └── (all depend on SessionPanel)
│
└── tools/                              ← UTILITIES (mixed)
    ├── codec/
    │   └── EBCDICCodec.java           ✓ Clean
    ├── logging/
    │   └── (logging abstractions)     ✓ Clean
    └── (mostly depend on GUI)
```

---

## Package Structure: Post-Refactoring (Desired)

```
org/hti5250j/
├── Session5250.java                    ← ADAPTER (delegates to Headless)
│
├── headless/                           ← NEW: Pure data transport
│   ├── HeadlessSession.java            (interface)
│   ├── DefaultHeadlessSession.java      (impl, no GUI)
│   ├── RobotHeadlessSession.java        (custom)
│   └── MockHeadlessSession.java         (testing)
│
├── handlers/                           ← NEW: Abstraction for customization
│   ├── RequestHandler.java             (interface)
│   ├── NullRequestHandler.java         (headless impl)
│   ├── GuiRequestHandler.java          (GUI impl)
│   └── RobotRequestHandler.java        (custom)
│
├── factory/                            ← NEW: Polymorphic creation
│   ├── SessionFactory.java             (interface)
│   ├── DefaultSessionFactory.java      (standard)
│   └── RobotSessionFactory.java        (custom)
│
├── framework/
│   ├── tn5250/
│   │   ├── Screen5250.java            ✓ Unchanged
│   │   ├── tnvt.java                  ✓ Unchanged
│   │   └── ...
│   │
│   └── common/
│       └── SessionManager.java        ✓ Unchanged
│
├── gui/                                ← OPTIONAL GUI LAYER (lazy-loaded)
│   ├── SystemRequestDialog.java
│   └── SessionPanel.java
│
├── workflow/                           ✓ UNCHANGED
│   ├── WorkflowRunner.java
│   └── ...
│
└── keyboard/                           ← REFACTORED (no GUI import)
    ├── actions/
    └── (depends on interfaces, not SessionPanel)
```

---

## Integration Architecture: Post-Refactoring

```
Robot Framework Extension
    │ (Jython)
    │
    ├─ RobotSessionFactory
    │  │ (creates custom sessions)
    │  │
    │  └─ RobotHeadlessSession
    │     │ (custom I/O handling)
    │     │
    │     └─ RobotRequestHandler
    │        │ (custom SYSREQ handling)
    │        │
    │        └─ (calls back to Robot keywords)
    │
    └─ ScreenProvider interface
       │
       └─ DefaultHeadlessSession
          │ (pure I/O, no GUI)
          │
          ├─ Screen5250
          ├─ tnvt
          └─ IBM i

JUnit Tests
    │
    ├─ DefaultSessionFactory
    │  │
    │  └─ MockHeadlessSession (fake responses)
    │
    └─ ScreenProvider interface
       │
       └─ Mocked Screen5250 (no I/O)

GUI Application (Optional)
    │
    ├─ DefaultSessionFactory
    │  │
    │  └─ DefaultHeadlessSession
    │     │
    │     └─ GuiRequestHandler
    │        │
    │        └─ SystemRequestDialog
    │
    └─ SessionPanel (optional view)
```

---

## Summary: Coupling vs. Cohesion

| Metric | Current | Target | Improvement |
|--------|---------|--------|-------------|
| **Import Cohesion** | Low | High | 40% reduction |
| **GUI Coupling** | 42 files | 0 (core) | 100% elimination |
| **Abstraction Layers** | 1 (Session5250) | 4 (Headless, Handler, Factory, Session) | +3 layers |
| **Circular Dependencies** | 0 (good) | 0 (maintained) | No change |
| **Interface Count** | 2 (Session, ScreenProvider) | 5 (+ Headless, Handler, Factory) | +3 |
| **Extensibility** | Limited | High (custom RequestHandler) | 3x improvement |

---

**Generated:** February 9, 2026
