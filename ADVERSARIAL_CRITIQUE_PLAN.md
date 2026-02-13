# Adversarial Code Critique Plan - HTI5250J

**Date**: 2026-02-12
**Java Version**: Temurin 21.0.10+7-LTS
**Build Status**: ✅ SUCCESSFUL (43s, 6 tasks)
**Critique Standards**: CODING_STANDARDS.md + WRITING_STYLE.md

---

## Execution Strategy

### Phase 1: Parallel Analysis (32 Agents)

**Chief Architect Role**: Coordinate 32 parallel code review agents
**Distribution**: ~3-4 files per agent (95+ Java files total)
**Critique Focus**:
1. **Naming violations** (Principle 1: abbreviations, unclear names)
2. **Comment anti-patterns** (Principle 3: WHAT/HOW comments instead of WHY)
3. **Java 21 adoption gaps** (missing Records, switch expressions, pattern matching)
4. **File length violations** (>400 lines without justification)
5. **Self-documenting code failures** (comment crutches)
6. **Thread safety issues** (pre-Java 21 patterns)
7. **Exception handling** (missing context, poor messages)

---

## File Inventory

**Total Java Files**: Counting...

**Package Structure**:
- `org.hti5250j.*` - Core application (19 files)
- `org.hti5250j.connectdialog.*` - Connection UI (6 files)
- `org.hti5250j.encoding.*` - EBCDIC codecs (25+ files)
- `org.hti5250j.event.*` - Event listeners (15 files)
- `org.hti5250j.framework.*` - Core protocol (20+ files)
- `org.hti5250j.framework.tn5250.*` - 5250 protocol (10+ files)
- `org.hti5250j.framework.transport.*` - Network transport (5 files)

---

## Critique Criteria (From CODING_STANDARDS.md)

### Priority 1: Critical Violations (Must Fix)

**C1. Comment Crutches (Principle 3)**
- Comments explaining WHAT code does (redundant)
- Comments explaining HOW code works (refactor needed)
- Comments compensating for cryptic names (rename variables)
- JavaDoc repeating implementation details (document contract instead)

**C2. Naming Anti-Patterns (Principle 1)**
- Abbreviations without justification (`adj`, `buf`, `attr`)
- Single-letter variables outside loops (`x`, `a`, `c` for non-trivial use)
- Boolean variables without `is/has/can/should` prefix
- Method names violating conventions (`getData()` doing I/O)

**C3. Java 21 Feature Gaps (Principle 2)**
- Data classes not using Records (boilerplate reduction)
- instanceof without pattern matching (explicit casts)
- Switch statements not using expressions (verbosity)
- Platform threads instead of virtual threads for I/O

### Priority 2: Code Smells (Should Fix)

**S1. File Length Violations**
- Files > 400 lines without architectural justification
- God classes handling multiple responsibilities
- Missing extraction opportunities (helper methods, classes)

**S2. Exception Design Gaps**
- Generic exceptions without context (`throw new Exception("failed")`)
- Missing contextual information (screen state, OIA status, timeout values)
- Catch blocks swallowing exceptions silently

**S3. Thread Safety Issues**
- Volatile variables with busy-wait loops (CPU waste)
- Missing AtomicReference for shared state
- Race conditions in keyboard state polling

### Priority 3: Opportunities (Nice to Have)

**O1. Documentation Gaps**
- Missing JavaDoc on public APIs
- Undocumented protocol assumptions (IBM i quirks)
- Missing precondition/postcondition contracts

**O2. Test Coverage Gaps**
- Boundary condition tests missing
- Error recovery tests missing
- Concurrent execution tests missing (some skipped)

---

## Agent Assignment Matrix (32 Agents)

### Batch 1: Core Application (Agents 1-6)
- **Agent 1**: BootStrapper.java, ExternalProgramConfig.java, GlobalConfigure.java
- **Agent 2**: Gui5250Frame.java, GuiGraphicBuffer.java (⚠️ GUI code - headless violation check)
- **Agent 3**: HTI5250jConstants.java, HeadlessScreenRenderer.java, KeypadPanel.java
- **Agent 4**: My5250.java, My5250Applet.java, OptionAccess.java
- **Agent 5**: PrinterThread.java, RubberBand.java, RubberBandCanvasIF.java
- **Agent 6**: Session5250.java, SessionConfig.java, SessionPanel.java

### Batch 2: Connect Dialog (Agents 7-8)
- **Agent 7**: ConnectDialog.java, Configure.java, CustomizedExternalProgram.java
- **Agent 8**: CustomizedTableModel.java, MultiSelectListComponent.java, SessionsDataModel.java, SessionsTableModel.java

### Batch 3: Encoding (Agents 9-16) - EBCDIC Codecs
- **Agent 9**: AbstractCodePage.java, BuiltInCodePageFactory.java, CharMappings.java
- **Agent 10**: ICodePage.java, JavaCodePageFactory.java, ToolboxCodePageFactory.java
- **Agent 11**: CCSID1025.java, CCSID1026.java, CCSID1112.java, CCSID1140.java
- **Agent 12**: CCSID1141.java, CCSID1147.java, CCSID1148.java, CCSID273.java
- **Agent 13**: CCSID277.java, CCSID278.java, CCSID280.java, CCSID284.java
- **Agent 14**: CCSID285.java, CCSID297.java, CCSID37.java, CCSID424.java
- **Agent 15**: CCSID500.java, CCSID870.java, CCSID871.java, CCSID875.java
- **Agent 16**: CCSID930.java, CodepageConverterAdapter.java, ICodepageConverter.java

### Batch 4: Events (Agents 17-20)
- **Agent 17**: BootEvent.java, BootListener.java, EmulatorActionEvent.java, EmulatorActionListener.java
- **Agent 18**: FTPStatusEvent.java, FTPStatusListener.java, KeyChangeListener.java, ScreenListener.java
- **Agent 19**: ScreenOIAListener.java, SessionChangeEvent.java, SessionConfigEvent.java, SessionConfigListener.java
- **Agent 20**: SessionJumpEvent.java, SessionJumpListener.java, SessionListener.java, TabClosedListener.java, ToggleDocumentListener.java, WizardEvent.java, WizardListener.java

### Batch 5: Framework Core (Agents 21-26)
- **Agent 21**: Tn5250jController.java, Tn5250jEvent.java, Tn5250jKeyEvents.java
- **Agent 22**: Tn5250jListener.java, Tn5250jSession.java, SessionManager.java
- **Agent 23**: Sessions.java, ByteExplainer.java, DataStreamDumper.java
- **Agent 24**: DataStreamProducer.java, KbdTypesCodePages.java, KeyStrokenizer.java
- **Agent 25**: Rect.java (⚠️ should be Record), Screen5250.java (🔥 CRITICAL)
- **Agent 26**: ScreenField.java, ScreenFields.java, ScreenOIA.java

### Batch 6: Protocol & Transport (Agents 27-32)
- **Agent 27**: ScreenPlanes.java, Stream5250.java, WTDSFParser.java
- **Agent 28**: tnvt.java (🔥 CRITICAL - telnet protocol, virtual threads candidate)
- **Agent 29**: IBMiConnectionFactory.java, SSLImplementation.java
- **Agent 30**: X509CertificateTrustManager.java, SSLInterface.java, SessionConnection.java
- **Agent 31**: **Test files** (if any exist in src/test)
- **Agent 32**: **Chief Architect** - Aggregate findings, prioritize refactorings

---

## Critical Files Identified (Pre-Analysis)

### 🔥 Tier 1: Must Review First
1. **tnvt.java** - Telnet protocol, likely >500 lines, virtual threads candidate
2. **Screen5250.java** - Core screen buffer management, likely complex
3. **Rect.java** - Should be Record (Principle 2, Java 16+)
4. **Session5250.java** - Session management, likely >400 lines

### ⚠️ Tier 2: Headless Violation Candidates
1. **Gui5250Frame.java** - GUI frame (should not be in core)
2. **GuiGraphicBuffer.java** - GUI rendering (check for core dependencies)
3. **SessionPanel.java** - GUI component (verify isolation)
4. **KeypadPanel.java** - GUI component (verify isolation)

---

## Deliverables (On Disk)

### 1. Individual Agent Reports
- `CRITIQUE_AGENT_01.md` - Findings from Agent 1
- `CRITIQUE_AGENT_02.md` - Findings from Agent 2
- ... (32 total)

### 2. Aggregated Findings
- `CRITIQUE_SUMMARY.md` - Chief Architect synthesis
- `REFACTORING_PRIORITIES.md` - Ranked list of fixes
- `QUICK_WINS.md` - Low-effort, high-impact changes

### 3. Refactoring Backlog
- `REFACTOR_P1_CRITICAL.md` - Must-fix violations
- `REFACTOR_P2_CODE_SMELLS.md` - Should-fix issues
- `REFACTOR_P3_OPPORTUNITIES.md` - Nice-to-have improvements

### 4. Metrics Dashboard
- `METRICS.md` - Before/after statistics:
  - Total comment lines vs code lines (target: ≤10%)
  - Files >400 lines (target: <10%)
  - Java 21 feature adoption rate (target: 80%)
  - Naming convention violations (target: 0)

---

## Success Criteria

**Definition of Done**:
- [ ] All 95+ Java files reviewed by assigned agent
- [ ] All findings documented in agent reports
- [ ] Chief Architect summary created
- [ ] Refactoring backlog prioritized (P1, P2, P3)
- [ ] Quick wins identified (≤4 hours each)
- [ ] Metrics baseline established

**Quality Gates**:
- [ ] Zero P1 (Critical) violations in core protocol files
- [ ] <20% comment-to-code ratio (excluding JavaDoc)
- [ ] 80%+ Java 21 feature adoption in new/refactored code
- [ ] All files ≤400 lines or architecturally justified

---

## Execution Timeline

**Phase 1 (Parallel Analysis)**: 32 agents × 3-4 files each = 90 minutes (estimated)
**Phase 2 (Aggregation)**: Chief Architect synthesis = 30 minutes
**Phase 3 (Plan Creation)**: Refactoring backlog = 30 minutes

**Total Estimated Time**: 2.5 hours

**Start Time**: 2026-02-12 21:20 PST
**Expected Completion**: 2026-02-12 23:50 PST

---

## Next Steps

1. **Launch 32 Parallel Agents**: `/snap` orchestration begins
2. **Monitor Progress**: Check agent completion status
3. **Aggregate Findings**: Chief Architect review
4. **Create Backlog**: Prioritized refactoring tasks
5. **Execute Quick Wins**: Immediate low-hanging fruit

---

**Status**: ⏳ PENDING - Awaiting agent launch
**Updated**: 2026-02-12 21:20 PST
