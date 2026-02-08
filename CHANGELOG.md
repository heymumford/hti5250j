# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning.

## Unreleased

### Added
- `CHANGELOG.md` for release history tracking.
- `ENGINEERING_PRINCIPLES.md` for cross-cutting engineering principles.
- `TEST_ARCHITECTURE.md` for the four-domain test model and cadence.
- Appendix F MoSCoW requirements checklist in `REQUIREMENTS.md`.
- Cross-cutting requirements R13.8–R13.15 in `REQUIREMENTS.md`.
- Gradle build files (`build.gradle`, `settings.gradle`, `gradle.properties`) with JUnit 5 and parallel test controls.
- Gradle wrapper scripts and `gradle/wrapper` configuration for consistent builds.

### Changed
- Standardized documentation headers and metadata tables across primary docs.
- `TASK_PLAN.md` now links to the authoritative MoSCoW checklist in `REQUIREMENTS.md`.
- `REQUIREMENTS.md` summary now states domain-agnostic IBM i scope.
- Phase summaries now reference `TEST_ARCHITECTURE.md` instead of duplicating the architecture narrative.
- `CODING_STANDARDS.md` narrowed to rules and examples; philosophy moved to `ENGINEERING_PRINCIPLES.md`.
- Upgraded build targets to Temurin Java 21.
- Version set to `0.8.0-headless.0.rtr` to reflect an unstable refactor cycle.
- Migrated JUnit4 tests to JUnit 5, including parameterized suites, timeouts, and exception assertions.

### Fixed
- Normalized SPDX headers and cleaned malformed SPDX lines across code and scripts.
- Added missing SPDX headers to resource, installer, and script files, including the Apache-2.0 header for `lib/development/izpack/bin/start.sh`.

### Removed
- Duplicated four-domain architecture sections from phase summaries.
