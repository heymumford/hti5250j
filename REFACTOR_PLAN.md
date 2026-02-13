# HTI5250J Dead Code Extraction Plan

## Design Philosophy: Parallel Change (Expand, Migrate, Contract)

Following Martin Fowler's *Refactoring* principles, this plan uses the **Strangler Fig**
pattern applied to a legacy GUI codebase being narrowed to a headless automation core.
Each step is independently verifiable, maintains a green build, and can be reverted atomically.

The governing constraint: **never break the build between commits.**

---

## Current State

- **Branch**: `refactor/cleanup-cruft-and-docs` (0 commits, forked from main)
- **Pending work**: 260 staged files (cruft removal + lint), 147 unstaged (comment cleanup)
- **Build**: compileJava + compileTestJava both pass
- **Codebase**: 327 Java source files, 224 test files
- **Purpose**: Headless semantic orchestrator for IBM i workflow automation

## Target State

- All existing cleanup work committed
- Dead GUI/plugin/interactive code removed (~64 source files, ~19,000 LOC)
- Dead tests removed (11 files), affected tests updated (11 files)
- Build + test compilation green
- Net reduction: ~20,000+ lines

---

## Commit Strategy (3 atomic commits)

### Commit 1: "refactor: deep clean cruft, lint, and comments"
- All 260 staged + 147 unstaged files from prior cleanup work
- Cruft file removal, .gitignore rewrite, checkstyle/spotbugs config
- 2,268 lint violations fixed (down to 27 size-limit warnings)
- Code comment cleanup across all packages
- Documentation rewrite (README, ARCHITECTURE, TESTING, CHANGELOG, etc.)

### Commit 2: "refactor: remove dead GUI, plugin, and interactive code"
- Phase 1: Delete dead source files (~64 files)
- Phase 2: Delete dead test files (11 files)
- Phase 3: Update affected tests (11 files)
- Phase 4: Delete dead example/demo files

### Commit 3: "test: verify headless core builds and tests compile"
- Only if any fixups are needed after Commit 2
- Otherwise Commit 2 stands alone

---

## Phase 1: Dead Source File Deletion

### 1A. Pure Dead Packages (delete entire directories)

| Package | Files | LOC | Reason |
|---------|-------|-----|--------|
| `plugin/` | 8 | 491 | Zero implementations, never instantiated |
| `scripting/` | 4 | 424 | Jython legacy, replaced by YAML workflows |
| `sessionsettings/` | 15 | 2,929 | GUI settings panels |
| `spoolfile/` | 10 | 2,891 | GUI spool file viewer |
| `sql/` | 2 | 626 | GUI SQL wizard |
| `mailtools/` | 3 | 1,210 | GUI email tools |
| `keyboard/actions/` | 21 | 872 | GUI keyboard actions (guarded by getGUI()!=null) |
| `keyboard/configure/` | 3 | 1,296 | GUI keyboard config |

### 1B. Dead Individual Files

| File | LOC | Reason |
|------|-----|--------|
| `My5250.java` | 873 | Desktop main class |
| `My5250Applet.java` | 234 | Deprecated Java applet |
| `BootStrapper.java` | 116 | Multi-instance socket server |
| `Gui5250Frame.java` | 461 | JFrame tabbed UI |
| `SessionPanel.java` | ~2000 | Interactive terminal panel |
| `SessionPopup.java` | ~300 | Right-click context menu |
| `SessionScroller.java` | ~100 | Mouse wheel scrolling |
| `KeypadPanel.java` | ~200 | On-screen keypad |
| `RubberBand.java` | ~100 | Mouse selection |
| `RubberBandCanvasIF.java` | ~30 | Rubber band interface |
| `PrinterThread.java` | 287 | Physical printer output |
| `ExternalProgramConfig.java` | 272 | External program config |
| `GuiRequestHandler.java` | 47 | GUI SYSREQ dialog |
| `HeadlessKeyEvent.java` | 122 | Unused event abstraction |
| `HeadlessScheduler.java` | 82 | Unused scheduling |
| `HeadlessUIDispatcher.java` | 27 | No-op dispatcher |
| `ISessionState.java` | 70 | Unused state enum |

### 1C. Dead GUI Package Files

| File | LOC | Reason |
|------|-----|--------|
| `gui/ConfirmTabCloseDialog.java` | ~80 | Tab close dialog |
| `gui/SystemRequestDialog.java` | ~100 | SYSREQ popup |
| `gui/HexCharMapDialog.java` | ~200 | Hex char picker |
| `gui/HTI5250jSecurityAccessDialog.java` | ~150 | Security dialog |
| `gui/HTI5250jSplashScreen.java` | ~100 | Splash screen |
| `gui/Wizard.java` | ~200 | Wizard framework |
| `gui/WizardPage.java` | ~100 | Wizard page base |
| `gui/HTI5250jFileChooser.java` | ~50 | File picker |
| `gui/HTI5250jFileFilter.java` | ~50 | File filter |
| `gui/HTI5250jFontsSelection.java` | ~150 | Font picker |
| `gui/AppleApplicationTools.java` | ~50 | macOS integration |
| `gui/ButtonTabComponent.java` | ~100 | Tab close button |
| `gui/GenericTn5250JFrame.java` | ~80 | Base JFrame |
| `gui/ToggleDocument.java` | ~50 | Document toggle |
| `gui/JSortTable.java` | ~200 | Sortable JTable |
| `gui/ModernTableSorter.java` | ~150 | Table sorter |
| `gui/ColumnComparator.java` | ~50 | Column comparator |
| `gui/DefaultSortTableModel.java` | ~80 | Sort table model |
| `gui/SortTableModel.java` | ~30 | Sort interface |
| `gui/SortArrowIcon.java` | ~50 | Sort arrow icon |
| `gui/SortHeaderRenderer.java` | ~50 | Header renderer |

### 1D. Dead Tool Files

| File | LOC | Reason |
|------|-----|--------|
| `tools/XTFRFile.java` | 1,352 | GUI file transfer |
| `tools/SendScreenToFile.java` | 97 | GUI screen export |
| `tools/SendScreenImageToFile.java` | 87 | GUI image export |
| `tools/LoadMacroMenu.java` | 270 | GUI macro menu |
| `tools/Macronizer.java` | 270 | Macro engine (GUI-only instantiation) |
| `tools/DecimalField.java` | 46 | Swing numeric field |
| `tools/FormattedDocument.java` | 65 | Document filter |
| `tools/GUIGraphicsUtils.java` | 1,400 | GUI drawing utilities |
| `tools/FTP5250Prot.java` | 1,076 | FTP client |
| `tools/AlignLayout.java` | 166 | Swing layout |
| `tools/ENHGridLayout.java` | 224 | Swing layout |
| `tools/FixedCenterLayout.java` | 227 | Swing layout |
| `tools/filters/OutputFilterInterface.java` | 34 | Filter interface |
| `tools/filters/ExcelOutputFilter.java` | 296 | Excel export |
| `tools/filters/HTMLOutputFilter.java` | 162 | HTML export |
| `tools/filters/KSpreadOutputFilter.java` | 192 | KSpread export |
| `tools/filters/OpenOfficeOutputFilter.java` | 465 | OO Calc export |
| `tools/filters/DelimitedOutputFilter.java` | 210 | CSV export |
| `tools/filters/FixedWidthOutputFilter.java` | 183 | Fixed-width export |
| `tools/filters/XTFRFileFilter.java` | 329 | File chooser filter |
| `tools/encoder/PNGEncoder.java` | 308 | Custom PNG encoder |
| `tools/encoder/AbstractImageEncoder.java` | 137 | Encoder base |
| `tools/encoder/Encoder.java` | 27 | Encoder interface |
| `tools/encoder/EncodeComponent.java` | 141 | Encode component |
| `tools/encoder/EncoderException.java` | 30 | Encoder exception |

### 1E. Dead Connect Dialog Files

| File | LOC | Reason |
|------|-----|--------|
| `connectdialog/ConnectDialog.java` | 1,215 | GUI connection dialog |
| `connectdialog/Configure.java` | 700 | GUI config |
| `connectdialog/SessionsTableModel.java` | 181 | JTable model |
| `connectdialog/CustomizedTableModel.java` | 137 | Custom table |
| `connectdialog/CustomizedExternalProgram.java` | 66 | External program |
| `connectdialog/MultiSelectListComponent.java` | 904 | Multi-select list |

### 1F. Dead Interface/Framework Files

| File | LOC | Reason |
|------|-----|--------|
| `interfaces/GUIViewInterface.java` | ~58 | Extends JFrame |
| `interfaces/SessionManagerInterface.java` | ~30 | Desktop session mgmt |
| `interfaces/SessionsInterface.java` | ~40 | Multi-tab container |

---

## Phase 2: Dead Test Deletion (11 files)

| Test File | Reason |
|-----------|--------|
| `tests/org/hti5250j/gui/ColorThemePairwiseTest.java` | Tests dead GUI color themes |
| `tests/org/hti5250j/gui/FontRenderingPairwiseTest.java` | Tests dead GUI font rendering |
| `tests/components/ButtonTabComponent.java` | GUI component demo |
| `tests/components/TabComponentsDemo.java` | GUI demo |
| `tests/org/hti5250j/contracts/PluginManagerContractTest.java` | Tests dead plugin system |
| `tests/org/hti5250j/contracts/HTI5250jPluginContractTest.java` | Tests dead plugin interface |
| `tests/org/hti5250j/scripting/ScriptingPairwiseTest.java` | Tests dead scripting |
| `tests/org/hti5250j/scripting/MacroRecordingPairwiseTest.java` | Tests dead macro recording |
| `tests/headless/KeyboardHandlerHeadlessTest.java` | Tests dead keyboard actions |
| `tests/org/hti5250j/accessibility/AccessibilityCompliancePairwiseTest.java` | Tests dead GUI accessibility |
| `tests/org/hti5250j/clipboard/ClipboardIntegrationPairwiseTest.java` | Tests dead GUI clipboard |

---

## Phase 3: Affected Test Modifications (11 files)

Each affected test needs surgical edits to remove references to dead code
while preserving essential protocol/headless test coverage.

| Test File | Issue | Fix |
|-----------|-------|-----|
| `ExampleEmbeddedMinimalBootstrap.java` | Uses SessionBean + SessionPanel | Delete (GUI example, not a test) |
| `SessionBean.java` | Extends SessionPanel | Delete (GUI test utility) |
| `My5250Test.java` | Tests My5250.java entry point | Review: keep CLI tests if any, delete GUI tests |
| `GuiComponentPairwiseTest.java` | Tests JTabbedPane management | Delete (pure GUI) |
| `SessionsHeadlessTest.java` | Imports SessionPanel | Remove dead imports, keep headless tests |
| `SecurityVulnerabilityTest.java` | Includes GUI keyboard tests | Remove GUI sections, keep protocol security |
| `EventListenerPairwiseTest.java` | Uses GUI rendering context | Remove rendering, keep event dispatch tests |
| `ConfigurationPairwiseTest.java` | Includes GUI config (fonts, themes) | Remove GUI config tests, keep core config |
| `ResourceExhaustionPairwiseTest.java` | Includes GUI cleanup | Remove Swing disposal tests |
| `SessionPoolingBenchmark.java` | Measures GUI rendering | Remove rendering benchmarks |
| `ColorPaletteIntegrationTest.java` | Tests GUI color rendering | Review: keep Screen5250 color tests |

---

## Verification Strategy

After each phase:
1. `./gradlew compileJava` - Source compilation green
2. `./gradlew compileTestJava` - Test compilation green
3. `./gradlew test` - Tests pass (where possible)

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Compile break from missing import | HIGH | Fix incrementally after each package deletion |
| Test referencing deleted class | MEDIUM | Agent analysis mapped all references |
| Essential code accidentally deleted | LOW | Dead code analysis by 6 agents, cross-validated |
| Transitive dependency break | LOW | Session5250/Screen5250 are GUI-free |

---

## Essential Core Preserved (~80 files, ~25,000 LOC)

- `workflow/` (51 files) - Workflow engine, actions, validators, batch executor
- `headless/` (4 essential files) - HeadlessSession, ISession, manager
- `framework/tn5250/` (14 files) - Protocol, screen buffer, OIA
- `framework/transport/` (4 files) - Socket, SSL, connection factory
- `encoding/` (28 files) - EBCDIC code pages
- `interfaces/` (essential only) - SessionInterface, HeadlessSession, RequestHandler
- `keyboard/` (3 files) - KeyMnemonic, KeyMnemonicResolver, KeyMnemonicSerializer
- `session/` (2 files) - DefaultHeadlessSession, NullRequestHandler
- `tools/logging/` (4 files) - Logging infrastructure
- `tools/` (4 files) - LangTool, DESSHA1, As400Util, OperatingSystem
- Root package essentials - Session5250, SessionConfig, HeadlessScreenRenderer, etc.
