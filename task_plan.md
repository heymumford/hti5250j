# Task Plan: Phase 14 Test ID Traceability

## Goal
Implement test ID prefixes (D1-*, D3-*) to enable ARCHITECTURE.md → test traceability mapping and establish protocol verification test framework.

## Status: READY FOR PHASE 14 EXECUTION

### Prerequisites Complete ✅
- [x] Phase 1: Critical removals & data loss fixes (My5250Applet deleted, nonDisplay fixed, protocol docs added)
- [x] Phase 2: Architecture clarity (Fowler research archived, docs verified clean)
- [x] Phase 3: Protocol documentation (PROTOCOL_RESEARCH.md created, SessionConfig marked forRemoval)

**Build Status:** Clean (288 source files, 46 deprecation warnings, 0 errors)  
**Test Suite:** 12,920+ tests passing

---

## Phase 14 Tasks

### 14A: Minimal D1-EBCDIC-001 Test (2 hours)
- [ ] Create CharMappingsAPITest.java
- [ ] Implement D1-EBCDIC-001: getCodePage() returns valid ICodePage
- [ ] Implement D1-EBCDIC-002: Round-trip ASCII → EBCDIC → ASCII
- [ ] Verify tests pass

### 14B: D3 Surface Tests (8 hours)
- [ ] D3-SCHEMA-001: Screen5250 field boundary tests (4 tests)
- [ ] D3-PROTO-001: tnvt TN5250E negotiation (4 tests)
- [ ] D3-PROTO-002: Stream5250 structured field serialization (4 tests)
- [ ] D3-CONCUR-001: Session5250 concurrency (2 tests)

### 14C: Test Traceability Documentation (2 hours)
- [ ] Update ARCHITECTURE.md with test ID mapping section
- [ ] Create test ID registry in ARCHITECTURE.md

### 14D: Verification & Documentation (1 hour)
- [ ] Verify all 14 new tests pass
- [ ] Update REFACTORING_COMPLETE.md with Phase 14 summary
- [ ] Full test suite verification (should be 13,117+ tests)

---

## Critical Decisions
1. **Minimal working test first** — Create CharMappingsAPITest before surface tests
2. **Real protocol APIs** — Use Screen5250, tnvt, Stream5250 directly (not mocks)
3. **Test ID format** — `D{domain}-{category}-{number}.{subtest}` (e.g., D1-EBCDIC-001.1)

## Build Verification

```
✅ 288 source files compiled (0 errors)
✅ Build time: ~3 seconds
✅ Ready for Phase 14 test creation
```

## Execution Status
**IN PROGRESS** - Awaiting Phase 14 test implementation

**Next Step:** Create CharMappingsAPITest.java (Phase 14A)
