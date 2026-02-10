# Task Plan: Documentation Cleanup & Rationalization (doc_cleanup)

## Mission
Perform sweeping cleanup of documentation files in HTI5250J. Eliminate AI-generated content ("AI slop"), MEMORY.md artifacts, and redundant meta-commentary. Leave only industry-standard documentation.

## Baseline
- Current branch: refactor/docs-cleanup (created 2026-02-09)
- Scope: All .md files in repo root + docs/ directory
- Key files identified for review:
  - MEMORY.md (1081 lines - 80% AI-generated session notes)
  - Various PHASE_*.md files (planning artifacts)
  - TESTING_EPISTEMOLOGY.md (high-value, keep)
  - CLAUDE.md (project instructions, keep)
  - Standard: README.md, ARCHITECTURE.md, CONTRIBUTING.md

## Execution Plan

### Iteration 1: Analysis & Cataloging (12 parallel agents)
- [x] Agent 1: repo-research-analyst - Document structure audit
- [x] Agent 2: best-practices-researcher - Industry doc standards
- [x] Agent 3: code-simplicity-reviewer - Identify redundancy
- [x] Agent 4: pattern-recognition-specialist - Detect AI prose patterns
- [ ] Agent 5: Specific: MEMORY.md content analysis
- [ ] Agent 6: Specific: Phase documentation (numbered files)
- [ ] Agent 7: Specific: Design docs (Architecture-related)
- [ ] Agent 8: Specific: Test/Contract docs
- [ ] Agent 9: Specific: Workflow/Process docs
- [ ] Agent 10: Specific: Standards docs (CODING_STANDARDS.md, etc.)
- [ ] Agent 11: Meta: Cross-file duplication analysis
- [ ] Agent 12: Evidence: Lines to delete, keep, refactor

### Iteration 2: Verification & Recommendations (12 parallel agents)
- [ ] Agent 1: kieran-rails-reviewer - Code quality standards
- [ ] Agent 2: data-integrity-guardian - Preserve critical info
- [ ] Agent 3: deployment-verification-agent - Go/no-go for deletions
- [ ] Agent 4: security-sentinel - Check for exposed data
- [ ] Agent 5-12: Specialized verification per doc domain

## Deliverables
- [ ] cleanup_findings.md - Structured catalog (file, severity, action)
- [ ] cleanup_recommendations.md - What to delete/keep/refactor
- [ ] impact_analysis.md - Cross-references (what depends on what)
- [ ] execution_plan.md - Ordered deletion/refactoring steps

## Status
**ITERATION 2 COMPLETE** (Branch: refactor/docs-cleanup-verify)
- ✓ Iteration 1: Analysis (56 files cataloged, 4 audit reports)
- ✓ README corrections: Java 21 Temurin, Gradle, Session5250 names
- ✓ Iteration 2: Architectural verification (architecture-strategist agent)
- ✓ Verification results: ITERATION_2_VERIFICATION_REPORT.md (comprehensive Go/No-Go assessment)
- Result: **85% confidence to proceed with 3 mandatory blockers**
- Next: Resolve blockers, then execute Phases 1-5 cleanup

## Mandatory Blockers (Before Phase 2+)

**Blocker 1: ADR Extraction (CRITICAL)**
- [ ] Extract ADR-012C-001 (Records deferral via Fowler's YAGNI) → ARCHITECTURE.md
- [ ] Extract ADR-012D-001 (Sealed classes type safety) → CODING_STANDARDS.md
- Files: PHASE_12C_CLOSURE.md, PHASE_12D_PLAN.md
- Impact: ADRs not documented elsewhere; must preserve before archiving PHASE files

**Blocker 2: Broken Link Fixes**
- [ ] README.md line 5: FORK.md → /ARCHIVE/FORK.md
- [ ] Remove dead references to TESTING_EPISTEMOLOGY.md
- [ ] Update DOCUMENTATION_INVENTORY.md after archival

**Blocker 3: MEMORY.md Purpose Clarification**
- [ ] DO NOT migrate MEMORY.md → CLAUDE.md (history ≠ instructions)
- [ ] Archive to /docs/archive/ (safer than delete)
- [ ] Extract critical learnings → ARCHITECTURE.md "Lessons Learned" section

## Efficiency Metrics
- Iteration 1: 1 agent (repo-research-analyst) generated 4 comprehensive reports
- Iteration 2: 1 agent (architecture-strategist, sonnet tier) completed full verification
- Total cost: haiku (Iteration 1) + sonnet (Iteration 2 specialist) = estimated $2-3
- Blockers identified: 3 (all resolvable before execution phase)
