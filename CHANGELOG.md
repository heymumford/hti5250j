# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Wave 3A: GuiGraphicBuffer extraction (5 classes)
- Wave 3A: Headless architecture interfaces and implementations
- 67 new tests for headless components (SessionsHeadlessTest, KeyMapperHeadlessTest, etc.)
- Integration tests for extracted components (ColorPaletteIntegrationTest, CursorManagerIntegrationTest, etc.)
- **CHANGELOG.md**: Created standardized changelog following Keep a Changelog format

### Fixed
- **Build System**: Fixed 3 P0 compilation errors in Wave 3A
- **Character Encoding**: Fixed CharacterConversionException handling in 6 CCSID codepages (37, 273, 280, 284, 297, 500)
- **Test Assertions**: Updated CharsetConversionPairwiseTest to expect proper exception types (29 tests fixed total)
- **CCSID870**: Fixed unmappable character handling in 4 tests with proper try-catch and success counting
- **Copyright Compliance**: Removed all unlicensed JavaPro magazine references from 6 source files
- **Shell Configuration**: Fixed `alias -- -` causing Claude Code shell snapshot parse errors permanently
  - Root cause: Shell snapshots replaying `alias -- -='cd -'` as `alias -- -- -='cd -'`
  - Fix: Added `unalias -- -` to `.bashrc`, removed corrupted snapshots
  - Alternative: Use `alias back='cd -'` (already in .bashrc line 146)

### Changed
- **Test Pass Rate**: Improved from 99.34% → 99.55% (+0.21%, 29 tests fixed)
- **Exception Handling**: All CCSID converters now throw CharacterConversionException instead of returning '?' for unmappable characters
- **Documentation Cleanup**: Removed 138 planning/analysis files, preserved 7 essential + 4 docs/
  - Removed: PHASE_*, CYCLE_*, CRITIQUE_AGENT_*, WAVE_*, BUG_HUNT_*, AGENT_*_REPORT.md, etc.
  - Preserved: README.md, CONTRIBUTING.md, SECURITY.md, TESTING.md, ARCHITECTURE.md, CHANGELOG.md, TEST_RESULTS_SUMMARY.md
  - docs/: ADR-015-Headless-Abstractions.md, MIGRATION_GUIDE_SESSION5250_TO_HEADLESS.md, ROBOT_FRAMEWORK_INTEGRATION.md, VIRTUAL_THREADS.md
  - Backed up to: `/Users/vorthruna/Projects/heymumford/hti5250j-archive/planning-docs-backup-20260213/`

### Technical Debt
- 61 test failures remain (0.45% of 13,637 tests)
- Categories: 1 timing test (flaky), 3 KeyStroker hash code tests, ~57 pre-existing failures

---

## [Wave 3A] - 2026-02-12

### Summary
Successfully completed Wave 3A refactoring with TDD approach. Extracted GuiGraphicBuffer responsibilities, implemented headless architecture, and created comprehensive test coverage.

**Branch**: `refactor/standards-critique-2026-02-12`
**Commits**: 6 clean commits (squashed from 31 original commits)
**Tests**: 99.55% pass rate (13,576 passing / 13,637 total)

### Milestones
- ✅ CCSID Factory Pattern working correctly
- ✅ GuiGraphicBuffer extraction complete (5 classes)
- ✅ Headless architecture validated
- ✅ Test-driven development maintained throughout
- ✅ Exception handling improved (CharacterConversionException)

---

*For detailed test results, see `TEST_RESULTS_SUMMARY.md`*
*For architecture decisions, see `ARCHITECTURE.md`*
