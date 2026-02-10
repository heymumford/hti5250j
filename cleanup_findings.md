# Documentation Cleanup: Findings Catalog

## Summary
- Total files to analyze: TBD
- Industry-standard docs to preserve: README.md, ARCHITECTURE.md, CONTRIBUTING.md, API docs
- Candidates for deletion: MEMORY.md (1081 lines of session notes), PHASE_*.md files
- Candidates for refactoring: CODING_STANDARDS.md, TESTING_EPISTEMOLOGY.md

## Findings Table

| ID | File | Type | Severity | Issue | Action | Status | Notes |
|----|------|------|----------|-------|--------|--------|-------|
| F001 | MEMORY.md | Session artifact | HIGH | 1081 lines of AI-generated session notes and phase tracking | DELETE or MIGRATE to CLAUDE.md | pending | Preserve: Phase 0-13 completion status only |
| F002 | PHASE_*.md | Planning artifact | MEDIUM | Numbered phase files (12C, 12D, 13, 14) | CONSOLIDATE to ARCHITECTURE.md | pending | Historical value, move to /docs/archive/ |
| F003 | CODING_STANDARDS.md | Standards | LOW | Well-written but > 478 lines | EVALUATE for refactoring | pending | Keep if actively used in PR reviews |
| F004 | TESTING_EPISTEMOLOGY.md | Design | KEEP | Domain 3/4 surface testing philosophy | PRESERVE | complete | High-value architectural documentation |
| F005 | README.md | Standard | KEEP | Project overview | VERIFY currency | pending | Check if reflects current Java 21 + sealed classes |
| F006 | ARCHITECTURE.md | Standard | KEEP | System design | VERIFY completeness | pending | Consolidate Phase docs into this |
| F007 | build.xml | Config | KEEP | Ant build configuration | VERIFY no AI comments | pending | Check for generated prose in comments |
| F008 | .claude/CLAUDE.md | Config | KEEP | Project instructions | VERIFY | complete | User-written, good reference doc |

## Categories

### A. Delete (High-confidence)
- MEMORY.md (99% AI session notes)
- PHASE_13_COMPLETION_REPORT.md (if exists)
- Task tracking artifacts in task_plan.md (keep structure only)

### B. Migrate/Consolidate
- PHASE_*.md → /docs/archive/ or consolidate to ARCHITECTURE.md
- Key decision records from PHASE files → ADR format in ARCHITECTURE.md

### C. Refactor
- CODING_STANDARDS.md: Extract to code comments, reduce prose
- TESTING_EPISTEMOLOGY.md: Keep, but link from ARCHITECTURE.md

### D. Preserve As-Is
- CLAUDE.md (user instructions)
- TESTING_EPISTEMOLOGY.md (architectural value)
- README.md, CONTRIBUTING.md, API docs

## Iteration 1 Results
(To be populated by parallel agents)

## Iteration 2 Results
(To be populated by verification agents)

## Final Recommendations
(To be generated from combined findings)
