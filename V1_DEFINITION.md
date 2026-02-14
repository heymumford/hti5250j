# V1.0 Definition

What "version 1.0" means for TN5250J Headless Edition.

## What v1.0 Means

A stable, documented, headless-first TN5250E library suitable for production automation. All public APIs are tested, documented, and free of known correctness bugs. The README accurately describes the project.

## In Scope for v1.0

### HeadlessSession API
- `connect()`, `disconnect()`, `isConnected()` — connection lifecycle
- `sendKeys(String)` — mnemonic syntax (`[enter]`, `[tab]`, `[f1]`–`[f24]`)
- `getScreenAsText()` — EBCDIC-decoded screen content
- `captureScreenshot()` — PNG screenshot via `HeadlessScreenRenderer`
- `waitForKeyboardUnlock(int)` — synchronous wait for host response

### YAML Workflow Engine
- 6 action handlers: LOGIN, NAVIGATE, FILL, SUBMIT, ASSERT, CAPTURE
- CSV data-driven execution with `${data.<column>}` substitution
- Batch execution with virtual threads
- Artifact collection (ledger, screen dumps)

### EBCDIC Encoding
- 23+ built-in code pages (CCSIDs 37, 273, 277, 278, 280, 284, 285, 297, 424, 500, 870, 871, 875, 930, 1025, 1026, 1112, 1122, 1140, 1141, 1147, 1148)
- `ICodePage` interface with `ebcdic2uni()` / `uni2ebcdic()` round-trip
- Java Charset fallback via `JavaCodePageFactory`

### TN5250E Protocol
- Full telnet negotiation (IAC/DO/WILL/SB sequences)
- Screen save/restore (`saveScreen()` / `restoreScreen()`)
- AID key handling (PF1–PF24, Enter, Clear, Help, Print)
- Device name negotiation
- SSL/TLS socket support

## Out of Scope for v1.0

- **Session pooling** — no pool exists; single-session usage only
- **GUI / Swing components** — removed; headless-only
- **Plugin system** — no plugin architecture
- **Distributed sessions** — single-process, single-host
- **Double-byte character set (DBCS) production support** — CCSID 930 exists but is lightly tested
- **Windows-specific terminal emulation** — cross-platform Java only

## Quality Gates

All must pass before tagging v1.0:

| Gate | Requirement | Current |
|------|------------|---------|
| Build | `./gradlew clean build` succeeds | PASS |
| Tests | All tests pass | PASS |
| Coverage | ≥ 30% line coverage | PASS (31%) |
| README | No fictional classes, all examples use real API | PASS |
| Empty catches | No critical swallowed exceptions | PASS |
| Dead code | No dead GUI files in source tree | PASS |
| Protocol tests | tnvt contract tests cover key methods | PASS |

## Current State: v0.13.0 → v1.0

### Already Done
- Swing/AWT dependency removal (headless-first)
- HeadlessSession API with DefaultHeadlessSession wrapper
- Workflow engine with 6 handlers and batch execution
- 23+ EBCDIC code pages with round-trip testing
- ~183 test classes across 4 domains (unit, surface, scenario, reliability)
- Vendored JAR removal (Gradle dependency management)
- SSLv2/SSLv3 deprecation
- Empty catch remediation
- README rewrite with real API classes

### Remaining for v1.0
1. ~~**Test coverage to 30%**~~ — DONE: 31% instruction coverage (JaCoCo)
2. ~~**Protocol contract tests**~~ — DONE: `TnvtProtocolContractTest` with 13 tests
3. ~~**CHANGELOG update**~~ — DONE: documented all changes since v0.12.0
4. ~~**Final documentation pass**~~ — DONE: ARCHITECTURE.md and TESTING.md aligned
5. **Tag and release** — `git tag v1.0.0` after all gates pass

### Not Needed for v1.0
- Performance optimization (current latency is acceptable)
- Additional code page support beyond existing 23+
- CI/CD pipeline (can be added post-v1.0)
- API stability guarantees beyond HeadlessSession interface
