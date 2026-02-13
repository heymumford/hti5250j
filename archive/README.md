# Archived Documentation

This directory contains planning, strategy, and assessment documents that have been archived to keep the repository clean and focused on shipping code.

## Organization

### `planning/` - Planning & Strategy Documents
These documents represent the planning and strategy work from development phases. They are preserved for historical reference but are not part of the active codebase.

**Contents**:
- `00_RELEASE_STRATEGY_START_HERE.md` - Release strategy framework
- `00_STRATEGY_START_HERE.md` - Overall strategy overview
- `5250_ADVERSARIAL_REVIEW_PLAN.md` - Plan for 32-agent protocol review (Feb 2026)
- `5250_ADVERSARIAL_REVIEW_SUMMARY.md` - Summary of adversarial review findings
- `REFACTOR_PLAN.md` - Refactoring roadmap and prioritization
- `API_1_0_READINESS_CHECKLIST.md` - v1.0.0 release checklist
- `API_ERGONOMICS_DX_ASSESSMENT.md` - Developer experience assessment
- `API_NAMING_CONVENTIONS.md` - API naming guidelines
- `DX_ASSESSMENT_INDEX.md` - Developer experience assessment index
- `GOVERNANCE.md` - Project governance structure
- `IMPLEMENTATION_CHECKLIST.md` - Implementation task checklist
- `INTEGRATION_EXECUTIVE_SUMMARY.md` - Integration architecture summary
- `MARKET_PERSONAS.md` - Target market personas
- `OPEN_SOURCE_STRATEGY.md` - Open source strategy
- `RELEASE_MANAGEMENT.md` - Release management procedures
- `RELEASE_QUICK_START.md` - Quick start for release process
- `RELEASE_READINESS_SCORECARD.md` - Readiness scoring framework
- `STRATEGY_QUICK_REFERENCE.txt` - Quick reference for strategy
- `STRATEGY_SUMMARY.md` - Executive summary of strategy

## Why Archive?

These documents served important roles during planning and review phases but are not referenced by the active codebase or end-user documentation. Archiving them:

1. **Reduces visual clutter** in the root directory - Users see only production-relevant files
2. **Preserves history** - Documents remain accessible but are not confusing for new contributors
3. **Improves discoverability** - Root directory focuses on CONTRIBUTING.md, README.md, and other shipping concerns
4. **Follows best practices** - Professional repositories keep build artifacts, plans, and administrative docs separate from source

## Access Archived Documents

### View a specific document
```bash
cat archive/planning/DOCUMENT.md
```

### Search archived content
```bash
grep -r "keyword" archive/planning/
```

### Restore document to root (if needed)
```bash
cp archive/planning/DOCUMENT.md ./
git add DOCUMENT.md
git commit -m "Restore DOCUMENT.md from archive"
```

## When to Restore

Restore documents from archive if:
- You need to reference historical strategy or design decisions
- A document needs to be revised and reactivated
- Planning resumes for a major release or refactoring phase

## What's NOT Archived

**Active documentation** remains in the root directory:
- [README.md](../README.md) - Project overview
- [ARCHITECTURE.md](../ARCHITECTURE.md) - System design
- [TESTING.md](../TESTING.md) - Testing strategy
- [CONTRIBUTING.md](../CONTRIBUTING.md) - Development guidelines
- [CHANGELOG.md](../CHANGELOG.md) - Release history
- [SECURITY.md](../SECURITY.md) - Security guidelines

**Protocol reference** is in `docs/`:
- [docs/5250_COMPLETE_REFERENCE.md](../docs/5250_COMPLETE_REFERENCE.md) - Comprehensive 5250/TN5250E protocol reference
- [docs/INDEX.md](../docs/INDEX.md) - Documentation navigation index

---

**Last Updated**: 2026-02-13
**Archive Version**: 1.0
**Total Archived Documents**: 19
