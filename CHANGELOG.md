# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] — v1.1.0

### Added
- **Session pooling**: `HeadlessSessionPool` interface and `DefaultHeadlessSessionPool` implementation with configurable acquisition modes (IMMEDIATE, QUEUED, TIMEOUT_ON_FULL), validation strategies (ON_BORROW, ON_RETURN, PERIODIC), and eviction policies (IDLE_TIME, MAX_AGE)
- **SessionPoolConfig**: Builder-pattern configuration for pool size, timeouts, validation, and eviction
- **PoolExhaustedException**: Checked exception for pool exhaustion scenarios
- 22 unit tests (`DefaultHeadlessSessionPoolTest`) and 7 real-pool pairwise integration tests

### Fixed
- **CI reliability**: Reduced `testVeryLongRunningAllocationStability` workload from 20s to 10s (45s timeout now has 35s headroom on GitHub Actions runners)
- **Semgrep SARIF**: Replaced `returntocorp/semgrep-action@v1` with direct `semgrep ci --sarif` invocation in both `ci.yml` and `semgrep.yml` to fix broken SARIF upload

---

## [Unreleased] — v1.0.0 Release Candidate

### Added
- **HeadlessSession API**: `DefaultHeadlessSession` wrapper with `sendKeys()`, `getScreenAsText()`, `captureScreenshot()`, `waitForKeyboardUnlock()`
- **YAML Workflow Engine**: 6 action handlers (LOGIN, NAVIGATE, FILL, SUBMIT, ASSERT, CAPTURE) with CSV data-driven execution and virtual thread batch processing
- **Protocol contract tests**: `TnvtProtocolContractTest` covering `setCodePage`, `setProxy`, `disconnect`, `sendHeartBeat`, `sendAidKey`, and Screen5250 operations (13 tests)
- **V1 definition document**: `V1_DEFINITION.md` defining scope, quality gates, and gap analysis for v1.0 release
- **EBCDIC code pages**: 23+ built-in CCSIDs (37, 273, 277, 278, 280, 284, 285, 297, 424, 500, 870, 871, 875, 930, 1025, 1026, 1112, 1122, 1140, 1141, 1147, 1148) with round-trip encoding tests
- **HeadlessSession abstractions**: `HeadlessSession` interface, `RequestHandler` interface, `NullRequestHandler`, `HeadlessSessionFactory`
- **183 test classes** across 4 domains (unit, surface, scenario, reliability)
- Documentation index (`docs/INDEX.md`) and comprehensive 5250 protocol reference (`docs/5250_COMPLETE_REFERENCE.md`)

### Changed
- **README rewrite**: Replaced all fictional API classes (`TN5250Session`, `SessionPool`, `TN5250Protocol`) with real classes (`Session5250`, `HeadlessSession`, `ICodePage`); corrected architecture tree to actual `org.hti5250j` package structure
- **Dependency management**: Removed vendored JARs (`tn5250j.jar`, `itext.jar`, `jt400.jar`), replaced with Gradle dependency declarations
- **SSL/TLS security**: Deprecated SSLv2 and SSLv3 protocols; only TLSv1.2+ accepted
- **Empty catch remediation**: Fixed critical swallowed exceptions across protocol and transport code; all catch blocks now log full exception objects
- **Exception handling**: All CCSID converters throw `CharacterConversionException` instead of returning `?` for unmappable characters
- **Documentation consolidation**: Merged 5 separate 5250 reference documents into single comprehensive reference; archived 19 planning documents to `archive/`
- **Project positioning**: Removed fictional session pooling claims; focused on actual capabilities (headless API, workflow engine, EBCDIC encoding)

### Fixed
- **Build System**: Fixed 3 P0 compilation errors
- **Character Encoding**: Fixed `CharacterConversionException` handling in 6 CCSID codepages (37, 273, 280, 284, 297, 500)
- **Test Assertions**: Updated `CharsetConversionPairwiseTest` to expect proper exception types (29 tests fixed)
- **Javadoc**: Corrected `@throws` annotations across event classes
- **Copyright Compliance**: Removed all unlicensed JavaPro magazine references from 6 source files

### Removed
- **Dead GUI code**: Deleted `SessionsDataModel.java` (unused Swing TableModel), `CopyrightComplianceTest.java` (referenced non-existent files), `FontMetricsIntegrationTest.java` (tested fictional API)
- **Vendored JARs**: Removed `lib/tn5250j.jar`, `lib/itext.jar`, `lib/jt400.jar`
- Archived planning and strategy documents to `archive/` directory
- Removed temporary review artifacts (`review_findings/`, `review_sources/`)

---

## [0.13.0] - 2026-02-12

### Summary
Comprehensive testing infrastructure release with virtual thread batch processing and reliability testing.

### Highlights
- Virtual thread batch workflow processing
- GitHub Packages publishing configuration
- 27 TDD GREEN tests enabled across event classes
- Compilation error fixes and pre-existing TDD RED test isolation

---

## [0.12.0] - 2026-02-12

### Summary
Extracted GuiGraphicBuffer responsibilities, implemented headless architecture, and created comprehensive test coverage.

### Highlights
- CCSID Factory Pattern working correctly
- GuiGraphicBuffer extraction complete (5 classes)
- Headless architecture validated
- Exception handling improved (CharacterConversionException)

---

*For architecture decisions, see `ARCHITECTURE.md`*
*For testing strategy, see `TESTING.md`*
*For v1.0 scope and gates, see `V1_DEFINITION.md`*
