# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- GuiGraphicBuffer extraction (5 classes)
- Headless architecture interfaces and implementations
- 67 new tests for headless components (SessionsHeadlessTest, KeyMapperHeadlessTest, etc.)
- Integration tests for extracted components (ColorPaletteIntegrationTest, CursorManagerIntegrationTest, etc.)
- Comprehensive README with quick start and navigation
- Documentation index (`docs/INDEX.md`) for easy reference navigation

### Fixed
- **Build System**: Fixed 3 P0 compilation errors
- **Character Encoding**: Fixed CharacterConversionException handling in 6 CCSID codepages (37, 273, 280, 284, 297, 500)
- **Test Assertions**: Updated CharsetConversionPairwiseTest to expect proper exception types (29 tests fixed)
- **CCSID870**: Fixed unmappable character handling in 4 tests with proper try-catch and success counting
- **Copyright Compliance**: Removed all unlicensed JavaPro magazine references from 6 source files

### Changed
- **Test Pass Rate**: Improved from 99.34% to 99.55% (29 tests fixed)
- **Exception Handling**: All CCSID converters now throw CharacterConversionException instead of returning '?' for unmappable characters
- **Documentation consolidation**: Merged 5 separate 5250 reference documents into single comprehensive reference (`docs/5250_COMPLETE_REFERENCE.md`)
  - Eliminated redundancy while preserving all unique technical information
  - Improved navigation with comprehensive table of contents and internal cross-references
- **Repository cleanup**: Archived 19 planning and strategy documents to `archive/planning/`
  - Removed clutter from root directory while preserving historical planning artifacts
  - Created comprehensive documentation indices: `docs/INDEX.md`, `archive/README.md`, `archive/planning/README.md`
  - Updated main README.md with protocol documentation references
  - Result: Professional, clean repository structure focused on shipping code
  - Retained all technical review corrections and changelogs

### Removed
- Archived planning and strategy documents to `archive/` directory
- Removed temporary review artifacts (`review_findings/`, `review_sources/`)
- Deleted backup files from 2026-02-13 technical review
- Cleaned root directory to industry-standard files only

---

## [0.12.0] - 2026-02-12

### Summary
Extracted GuiGraphicBuffer responsibilities, implemented headless architecture, and created comprehensive test coverage.

### Highlights
- CCSID Factory Pattern working correctly
- GuiGraphicBuffer extraction complete (5 classes)
- Headless architecture validated
- Exception handling improved (CharacterConversionException)
- 99.55% test pass rate (13,576 passing / 13,637 total)

---

*For detailed test results, see `TEST_RESULTS_SUMMARY.md`*
*For architecture decisions, see `ARCHITECTURE.md`*
