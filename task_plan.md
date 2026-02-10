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
**IN PROGRESS** - Phase 1: Setting up Iteration 1 agent dispatch
- Branch created: doc_cleanup ✓
- Planning files: task_plan.md (this file) ✓
- Next: Launch 12 parallel Iteration 1 agents

## Efficiency Metrics (to be filled)
- Parallel services: 24 agents × 2 iterations = {actual}
- Redundant calls: 0 (planned, verify after)
- Backtracking: Minimize via findings.md catalog
- Optimal path: Identify targets → Verify impact → Execute cleanup
