# HTI5250J Python/Robot Framework Integration Analysis — Document Index

**Date:** February 9, 2026
**Status:** Complete and ready for distribution

---

## Overview

This analysis assesses API accessibility of HTI5250J for external tool integration (Python, Robot Framework, other languages). Three companion documents provide complementary views of the same codebase.

### Research Methodology

- **Architecture study** → ARCHITECTURE.md review
- **Source code inspection** → 1100+ lines examined (Session5250, Screen5250, SessionPanel, workflow components)
- **Interface analysis** → Public API surface mapped (SessionInterface, Screen5250, ScreenOIA)
- **Coupling analysis** → GUI dependencies identified (Toolkit, Swing imports, SessionPanel inheritance)
- **Contract test review** → Expected behavioral guarantees documented
- **Reverse engineering assessment** → Effort estimates for accessing hidden functionality

### Key Findings Summary

**Integration Difficulty:** MEDIUM
**Public APIs:** 80% of use cases covered without reverse engineering
**Hidden Functionality:** 20% trapped in GUI layer (macro system, clipboard, rendering)
**Estimated Abstraction Work:** 15-30 hours to expose complete API

---

## Document Guide

### 1. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md (1032 lines)

**Comprehensive technical analysis — detailed reference document**

**Best for:** Architecture teams, integration architects, technical decision makers

**Contains:**
- Executive summary with difficulty ratings
- Public API inventory (Session5250, Screen5250, ScreenOIA, SessionConfig)
- Hidden functionality analysis (SessionPanel, macro system, keyboard handling)
- Integration barrier assessment with severity matrix
- Required abstraction changes (proposed refactoring)
- Four integration patterns (JPype, REST bridge, Robot Framework, direct workflow API)
- Reverse engineering roadmap (step-by-step approach)
- Risk assessment (breaking changes, licensing, performance)
- 13 sections + appendices

**Read sections:**
- Sections 1-5: Understand what's accessible and what's hidden
- Sections 7-8: Plan your integration approach
- Sections 11-12: Evaluate risk and effort
- Appendix A: File manifest for targeted research

**Page 1 starting points:**
- Need quick summary? → Section 0 (Executive Summary)
- Need to decide go/no-go? → Section 12 (Conclusion)
- Need technical details? → Sections 3-6 (API inventory and barriers)

---

### 2. INTEGRATION_BARRIER_QUICK_REFERENCE.md (581 lines)

**Executive summary with decision trees — quick-start guide**

**Best for:** Python developers, automation engineers, team leads

**Contains:**
- TL;DR decision table (easy vs hard use cases)
- 3 core barriers with workarounds
- Public APIs that work (don't reverse engineer)
- APIs that need reverse engineering
- 3 recommended integration paths with code examples
- Decision matrix (choose your path)
- Common pitfalls and solutions
- Testing strategy
- 8 sections

**Read sections:**
- Sections 1-4: Understand barriers and your options
- Section 5: See code examples for each path
- Section 6: Use decision matrix to choose approach
- Section 7: Avoid common pitfalls
- Section 8: Plan your testing

**Page 1 starting points:**
- Can I integrate Python? → Section TL;DR
- Why is integration hard? → Sections 2-4
- How do I get started? → Section 6 (decision matrix)
- What code do I write? → Section 5 (paths)

---

### 3. ARCHITECTURE.md (Existing, 790 lines)

**System design document — understand how HTI5250J works**

**Best for:** Understanding overall system design (read first if new to codebase)

**Contains:**
- C1 System context (client-HTI5250J-IBM i)
- C2 Container diagram (6 logical containers)
- C3 Component breakdown (per-container responsibilities)
- C4 Code-level detail (keyboard state machine)
- Phase 11 workflow execution pipeline
- Integration points
- Design decisions & rationale
- Headless-first philosophy

**Read sections:**
- C1-C2: Big picture (what talks to what?)
- C3 → Focus on Session5250, Screen5250 components
- C4 → Understand polling/state machine
- "Headless-First Philosophy" (line 767) → Confirmation HTI5250J is designed for headless use

---

## How to Use These Documents

### Scenario 1: "I want to automate 5250 screens from Python"

**Reading order:**
1. INTEGRATION_BARRIER_QUICK_REFERENCE.md → Sections 1-2 (understand barriers)
2. INTEGRATION_BARRIER_QUICK_REFERENCE.md → Section 6 (choose Path 1)
3. INTEGRATION_BARRIER_QUICK_REFERENCE.md → Section 5 (see code example)
4. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 4.1 (understand Session5250 API)
5. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 8, Pattern 1 (JPype integration)

**Effort estimate:** 4-6 hours to working Python adapter

---

### Scenario 2: "I need to integrate Robot Framework with HTI5250J"

**Reading order:**
1. INTEGRATION_BARRIER_QUICK_REFERENCE.md → Section TL;DR (confirm feasibility)
2. INTEGRATION_BARRIER_QUICK_REFERENCE.md → Section 5 (see code examples)
3. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 8, Pattern 3 (Robot Framework pattern)
4. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 10 (testing strategy)

**Effort estimate:** 8-12 hours for basic keyword library

---

### Scenario 3: "I need to execute YAML workflows from Python"

**Reading order:**
1. INTEGRATION_BARRIER_QUICK_REFERENCE.md → Section 2.2 (understand barrier)
2. INTEGRATION_BARRIER_QUICK_REFERENCE.md → Section 6 (choose Path 2 or 3)
3. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 5 (reverse engineering required?)
4. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 8 (integration patterns)
5. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 11.2 (file GitHub issue request)

**Effort estimate:** 2-3 hours (Path 2, CLI wrapper) OR 20-30 hours (Path 3, REST bridge)

---

### Scenario 4: "I need to understand integration barriers for architecture decisions"

**Reading order:**
1. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 0 (executive summary)
2. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 3 (barrier assessment)
3. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 7 (required abstraction changes)
4. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 11.2 (recommendations for HTI5250J maintainers)
5. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Appendix A (file manifest)

**Time estimate:** 2-3 hours for full understanding

---

### Scenario 5: "I'm implementing a Python wrapper around HTI5250J"

**Reading order:**
1. ARCHITECTURE.md → Read full document (understand system design)
2. INTEGRATION_BARRIER_QUICK_REFERENCE.md → Sections 1-4 (understand barriers you'll hit)
3. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Sections 1-6 (API inventory, barriers, examples)
4. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 9 (reverse engineering roadmap)
5. PYTHON_ROBOT_INTEGRATION_ANALYSIS.md → Section 10 (integration testing)

**Time estimate:** 8-12 hours reading + 15-40 hours implementation (depending on scope)

---

## Key Takeaways (All Scenarios)

### What's Accessible (Use Public APIs)

✓ Session5250 connection management
✓ Screen5250 buffer reading
✓ ScreenOIA polling (keyboard state)
✓ Key sending via Screen5250.sendKeys()
✓ Plugin system registration
✓ Virtual threads (transparent, 1000+ sessions)

**Code is clean, well-designed, no GUI coupling at this level.**

### What Requires Workarounds

⚠ Session5250 imports AWT/Swing (but works headless if you override GUI methods)
⚠ SessionConfig only supports Java Properties format (write a YAML converter)
⚠ SessionPanel required for GUI, but optional for headless use

**Workarounds are straightforward (1-3 hours each).**

### What Requires Reverse Engineering

❌ WorkflowRunner (no public execute API)
❌ ActionFactory (not intended for external use)
❌ Macro system (SessionPanel-only)
❌ Keyboard customization (no programmatic API)

**Reverse engineering is necessary to execute workflows programmatically.**

### Recommendation

**Start with Path 1 (Direct API)** — 80% of use cases are covered with clean public APIs:
1. Use JPype to call Session5250/Screen5250 directly
2. Create Robot Framework keyword wrappers
3. Build custom logic in Python (not reverse engineering)
4. File GitHub issue requesting WorkflowExecutor API
5. Iterate as HTI5250J evolves

**This approach avoids fragile reverse engineering and has low maintenance burden.**

---

## File Locations (Absolute Paths)

### Documents (This Analysis)

- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/PYTHON_ROBOT_INTEGRATION_ANALYSIS.md` (1032 lines)
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/INTEGRATION_BARRIER_QUICK_REFERENCE.md` (581 lines)
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/INTEGRATION_ANALYSIS_INDEX.md` (this file)

### Reference Documentation

- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/ARCHITECTURE.md` (790 lines, system design)
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/CONTRIBUTING.md` (79 lines, contribution guidelines)

### Key Source Files

- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/Session5250.java` (primary entry point)
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/framework/tn5250/Screen5250.java` (terminal buffer)
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/SessionPanel.java` (GUI layer, contains hidden functionality)
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/workflow/` (workflow execution, reverse-engineer required)

### Test References

- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/hti5250j/contracts/SessionInterfaceContractTest.java`
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/hti5250j/contracts/Screen5250ContractTest.java`
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/hti5250j/workflow/WorkflowExecutionIntegrationTest.java`

---

## Cross-References Between Documents

### INTEGRATION_BARRIER_QUICK_REFERENCE.md References

| Section | Links to |
|---------|----------|
| Barrier #1 (GUI Coupling) | PYTHON_ROBOT → Section 2.1 (Session5250 GUI coupling) |
| Barrier #2 (Workflow API) | PYTHON_ROBOT → Section 5.1 (workflow execution hidden) |
| Barrier #3 (SessionPanel) | PYTHON_ROBOT → Section 2.2 (SessionPanel analysis) |
| Public APIs That Work | PYTHON_ROBOT → Section 1 (API inventory) |
| Path 1 (Direct API) | PYTHON_ROBOT → Section 8, Pattern 1 |
| Path 2 (CLI Wrapper) | PYTHON_ROBOT → Section 8, Pattern 2 |
| Path 3 (REST Bridge) | PYTHON_ROBOT → Section 8, Pattern 3 |
| Reverse Engineering | PYTHON_ROBOT → Section 9 (roadmap) |
| Testing | PYTHON_ROBOT → Section 10 (testing strategy) |

### PYTHON_ROBOT_INTEGRATION_ANALYSIS.md References

| Section | Links to |
|---------|----------|
| Section 0 (Executive) | QUICK_REFERENCE → TL;DR |
| Section 3 (Barriers) | QUICK_REFERENCE → Barriers 1-3 |
| Section 4 (Accessibility) | QUICK_REFERENCE → Public APIs section |
| Section 5 (Hidden) | QUICK_REFERENCE → "APIs That Need Reverse Engineering" |
| Section 7 (Abstractions) | QUICK_REFERENCE → "Common Pitfalls" |
| Section 8 (Patterns) | QUICK_REFERENCE → "Recommended Integration Paths" |
| Section 9 (Reverse Eng) | QUICK_REFERENCE → "Getting Help" / "Reverse Engineering" |
| Section 11 (Recommendations) | QUICK_REFERENCE → "Decision Matrix" |
| Section 12 (Conclusion) | QUICK_REFERENCE → Section TL;DR |

---

## Maintenance & Updates

### When to Update This Analysis

- **HTI5250J updates:** If major refactoring occurs (Session5250, SessionPanel, workflow)
- **New features:** If public APIs are added (e.g., WorkflowExecutor exposed)
- **Policy changes:** If GUI-coupling strategy changes
- **Community feedback:** If integration attempts reveal new barriers

### How to Verify Analysis Accuracy

Run these commands to regenerate findings:

```bash
# Verify Session5250 still imports AWT/Swing
grep "import java.awt\|import javax.swing" \
  /Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/Session5250.java

# Verify SessionPanel still extends JPanel
grep "class SessionPanel extends" \
  /Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/SessionPanel.java

# Verify WorkflowCLI main() is only entry point
grep "public.*execute\|public.*workflow" \
  /Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/workflow/*.java

# Verify contract tests exist
ls /Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/hti5250j/contracts/*Test.java

# Run tests to verify expected behavior
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
./gradlew test --tests "SessionInterfaceContractTest"
```

---

## Support & Next Steps

### For Python Developers

1. **Start here:** Read INTEGRATION_BARRIER_QUICK_REFERENCE.md (20 minutes)
2. **Choose approach:** Use decision matrix (Section 6)
3. **See code:** Review Path examples (Section 5)
4. **Get hands-on:** Clone repo, try Path 1 with JPype (4-6 hours)
5. **Build:** Create Robot Framework keyword library
6. **Contribute:** File GitHub issue or submit PR if improvements found

### For HTI5250J Maintainers

1. **Review:** PYTHON_ROBOT_INTEGRATION_ANALYSIS.md Section 11.2 (recommendations)
2. **Prioritize:** Consider WorkflowExecutor API exposure (high demand, 6h effort)
3. **Communicate:** Update README/CONTRIBUTING with integration guidance
4. **Iterate:** Act on Python community feedback

### For Architects/Decision Makers

1. **Read:** PYTHON_ROBOT_INTEGRATION_ANALYSIS.md Section 12 (conclusion)
2. **Assess:** Review Section 3 (barrier severity matrix)
3. **Plan:** Section 7 (required abstraction changes) for multi-year roadmap
4. **Decide:** File GitHub issues to request priority changes

---

## Questions?

- **"How do I integrate?"** → INTEGRATION_BARRIER_QUICK_REFERENCE.md
- **"Why is it hard?"** → PYTHON_ROBOT_INTEGRATION_ANALYSIS.md Sections 3-6
- **"What code do I write?"** → PYTHON_ROBOT_INTEGRATION_ANALYSIS.md Section 8
- **"What's the roadmap?"** → PYTHON_ROBOT_INTEGRATION_ANALYSIS.md Section 9
- **"How do I test?"** → PYTHON_ROBOT_INTEGRATION_ANALYSIS.md Section 10

---

**Analysis Complete**
**Ready for Distribution**
**February 9, 2026**
