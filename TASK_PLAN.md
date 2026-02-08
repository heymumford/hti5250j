# Planning and Refactor Tracker

| Field | Value |
| --- | --- |
| Goal | Establish tn5250j-headless on main with clean docs, verified tests, and a requirements-aligned roadmap |
| Branch | feature/hti5250j-temurin21-upgrade |
| Commits | TBD |
| Test status | Full suite passed; 20 structured-format tests skipped by design in AidKeyResponse. Code style refactors since last run are unverified. |
| Tech stack | Temurin Java 21, Gradle 9.3.1, JUnit 5, Log4j 2.23.1 |
| Last updated | 2026-02-08 |

## Reality Check

### Refactor
- Requirements structure: keep REDBOOKS-inspired structure and align checklist items to requirement IDs.
- Documentation drift: remove duplicate or inconsistent statements across phase summaries.
- Coding standards: trim duplicative sections and tighten examples where repetitive.

### Replace
- Placeholder planning language with concrete next steps tied to requirements sections.
- Any remaining legacy licensing blocks with SPDX-only headers if found later.

### Remove
- Redundant executive summary paragraphs or duplicated sections in phase summaries.
- Stale roadmap assumptions that conflict with current headless-first direction.

### Recover
- Preserve original authorship and attribution context when reformatting or trimming content.
- Keep historical phase summaries, but normalize structure to match current doc standard.

### Reissue
- Reissue updated requirements checklist as authoritative MoSCoW items in REQUIREMENTS.md.

### Reattribution
- Ensure SPDX headers remain consistent across all code and doc artifacts.

## Phases

- [x] Phase 1: Documentation audit and cleanup
- [x] Phase 2: Attribution verification and SPDX normalization
- [x] Phase 3: Requirements alignment (map checklist to requirements IDs)
- [x] Phase 4: Version verification (Java, dependencies)
- [~] Phase 5: Test suite execution and evidence capture (targeted buckets complete; full suite pending)
- [x] Phase 6: Precision pass (tighten language, remove ceremony)
- [ ] Phase 7: Final quality gate
- [ ] Phase 8: Create PR to main

## Action Plan (Post-Test Review)

- Full test suite completed; triage failures by category and update evidence log with fix status.
- Prioritize fixes: finalize quality gate and prep PR.
- Resource leak + cursor bounds fixes implemented and verified with targeted tests.
- Encoding/charset mapping fix implemented and verified with targeted tests.

## Test Triage Summary (Full Suite)

| Bucket | Failures | Notes |
| --- | --- | --- |
| Print/Spool | 38 | `HostPrintPassthroughPairwiseTest`, `SpoolFilePairwiseTest`, `PrintSpoolDeepPairwiseTest` |
| Encoding/Charset | 16 | `EBCDICPairwiseTest`, `CharsetConversionPairwiseTest` |
| Cursor Bounds | 8 | `Screen5250CursorPairwiseTest`, `CursorMovementDeepPairwiseTest` |
| Security/SSL | 7 | `SSLSecurityPairwiseTest`, `SecurityHardeningPairwiseTest`, `SecurityVulnerabilityTest` |
| Logging/Diagnostics | 5 | `LoggingDiagnosticsPairwiseTest` |
| Concurrency/Stress | 5 | `ConcurrencyPairwiseTest`, `ThreadSafetyTest`, `StressScenarioTest` |
| Resource Leak | 5 | `ResourceLeakTest` |
| Protocol/Schema/Decimal | 2 | `ProtocolRoundTripSurfaceTest`, `SchemaContractSurfaceTest` |
| Scenario | 2 | `PaymentProcessingScenarioTest`, `ErrorRecoveryScenarioTest` |
| Other | 6 | `ScreenPlanesTest`, `ConnectionTimeoutPairwiseTest`, `FileTransferPairwiseTest` |
- Confirm whether AidKeyResponse structured-format skips are acceptable or should be narrowed.
- Update quality gate checklist with full-suite status and any remaining blockers.

## Requirements Checklist (MoSCoW)

Authoritative checklist lives in `REQUIREMENTS.md` Appendix F.

## Evidence Log

- 2026-02-07: Targeted test run (8 classes) passed; 3718 tests run, 20 structured-format cases skipped by design in AidKeyResponse.
- 2026-02-07: Test buckets executed: AttributePlaneOpsPairwiseTest, TransactionBoundaryPairwiseTest, AidKeyResponsePairwiseTest, ScreenFormatChangePairwiseTest, SBACommandPairwiseTest, Screen5250ReadPairwiseTest, ScreenFieldsTest, TransportPairwiseTest.
- 2026-02-07: Full suite run: 12,795 tests completed; 94 failed; 20 skipped. Report: `build/reports/tests/test/index.html`.
- 2026-02-07: Failure buckets identified (print/spool 38, encoding/charset 16, cursor bounds 8, security/ssl 7, logging 5, concurrency/stress 5, resource leak 5, protocol/schema 2, scenarios 2, other 6).
- 2026-02-07: Targeted fixes verified: ResourceLeakTest + Screen5250CursorPairwiseTest + CursorMovementDeepPairwiseTest pass.
- 2026-02-07: Targeted fixes verified: EBCDICPairwiseTest + CharsetConversionPairwiseTest pass.
- 2026-02-07: Targeted fixes verified: HostPrintPassthroughPairwiseTest + SpoolFilePairwiseTest + PrintSpoolDeepPairwiseTest pass.
- 2026-02-07: Targeted fixes verified: SSLSecurityPairwiseTest + SecurityHardeningPairwiseTest + SecurityVulnerabilityTest pass.
- 2026-02-07: Targeted fixes verified: LoggingDiagnosticsPairwiseTest pass.
- 2026-02-07: Targeted fixes verified: ConcurrencyPairwiseTest + ThreadSafetyTest + StressScenarioTest pass.
- 2026-02-07: Targeted fixes verified: ProtocolRoundTripSurfaceTest + SchemaContractSurfaceTest + PaymentProcessingScenarioTest + ErrorRecoveryScenarioTest pass.
- 2026-02-07: Full suite run: 12,795 tests completed; 0 failed; 20 skipped. Report: `build/reports/tests/test/index.html`.
- 2026-02-08: Coding standards refactor pass (single-letter variables and naming clarity) across core and tools packages. Tests not rerun after these refactors.

## Blockers

(None yet.)
