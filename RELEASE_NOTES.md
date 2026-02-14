# Release Notes - v0.13.0

**Release Date**: February 13, 2026
**Branch**: main

---

## Summary

Testing infrastructure and CI pipeline for the headless TN5250J fork. Adds JaCoCo coverage, JMH benchmarks, property-based testing with jqwik, and chaos injection testing with resilience4j.

## Changes

### Testing Infrastructure
- JaCoCo branch-level code coverage tracking (21% baseline)
- JMH micro-benchmark framework configured
- jqwik property-based testing (8 properties with generative inputs)
- resilience4j chaos injection testing (8 failure scenarios)
- k6 load test configuration (5 scenarios)

### CI/CD
- GitHub Actions workflow with test, performance, and security jobs
- Semgrep security scanning
- JaCoCo coverage report generation and artifact upload
- Branch protection with required status checks

### Documentation
- Comprehensive 5250 protocol reference
- Testing framework documentation (four-domain model)
- Workflow execution examples with YAML DSL

### Cleanup
- Removed archived planning documents from root
- Removed internal project references
- Updated documentation to public standards

## Test Suite

- ~170 test classes across unit, integration, scenario, and pairwise tests
- ~2,600 test methods
- 8 jqwik property-based tests with generative inputs
- 8 chaos injection scenarios (retry, timeout, cascading failure)
- JMH benchmark suites for encoding and protocol operations

## Known Limitations

- Code coverage at 21% baseline (legacy codebase under modernization)
- Checkstyle and SpotBugs configured but not yet enforced as build gates
- k6 load tests require a running server (not executed in CI)
- Some vendored JARs in lib/ predate Maven Central migration

## Getting Started

```bash
./gradlew test                           # Run all tests
./gradlew test jacocoTestReport          # Tests + coverage
./gradlew jmh                            # Run benchmarks
open build/reports/jacoco/test/html/index.html  # View coverage
```

---

**License**: GPL-2.0-or-later
