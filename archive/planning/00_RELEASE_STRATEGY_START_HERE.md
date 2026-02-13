# HTI5250J Release Strategy & Readiness

**Audience:** Maintainers, release managers, stakeholders
**Date:** February 13, 2026
**Status:** Release Management Framework Complete (Implementation Pending Test Fixes)

---

## Executive Summary

HTI5250J is **ready for structured releases** but has a **BLOCKER preventing v0.12.0 publication:**
- **61 failing tests** (0.45% of 13,637 total) must be fixed
- Once fixed, v0.12.0 can be released to GitHub Releases in ~30 minutes
- Maven Central + Docker Hub publishing deferred to v0.13.0 (March 2026)
- Path to 1.0.0 stable release: Q3 2026

---

## Documents in This Release Strategy

### 1. **RELEASE_MANAGEMENT.md** (9,000 words)
**The authoritative guide.** Contains:
- Release readiness assessment (current: YELLOW)
- Release criteria for each milestone (0.12.0, 0.13.0, 1.0.0)
- End-to-end release process (6 phases, 50 minutes)
- Version numbering plan (SemVer 0.y.z → 1.0.0)
- Branching strategy (main, feature/*, release/*, hotfix/*)
- Artifact publishing checklist (GitHub Releases, Maven Central, Docker Hub)
- Rollback procedures (4 failure scenarios)
- 3 detailed checklists (pre-release, code freeze, post-release)

**Use when:** Planning a release, training new maintainers, validating process

### 2. **RELEASE_READINESS_SCORECARD.md** (2,000 words)
**Current state assessment.** Contains:
- 8-factor readiness matrix (Build, Tests, Docs, CI/CD, Security, Deps, Infra, Quality)
- Current scores: GREEN (4), YELLOW (3), RED (2)
- Detailed breakdown for each factor with action items
- Metrics dashboard (test pass rate, build time, security, memory)
- Trend analysis (last 7 days)
- Roadmap to 1.0.0 (0.12.0 Feb, 0.13.0 Mar, 1.0.0 Q3)

**Use when:** Assessing release readiness, tracking progress, reporting to stakeholders

### 3. **RELEASE_QUICK_START.md** (800 words)
**Copy-paste for busy operators.** Contains:
- Decision tree (is it time to release?)
- 6 phases with checklists + bash commands
- Troubleshooting section (common failures)
- Time estimates (50 min total, 25 min critical path)
- One-liner release (for experienced operators)

**Use when:** Actually executing a release, need quick reference

### 4. **00_RELEASE_STRATEGY_START_HERE.md** (This File)
**Navigation hub.** Contains:
- This summary
- Quick decision table
- Learning paths for different roles
- Next immediate actions

---

## Quick Status Summary

| Factor | Status | Issue | Action |
|--------|--------|-------|--------|
| **Can Release 0.12.0?** | 🔴 NO | 61 failing tests | Fix tests (2-4 hrs) |
| **Can Merge to Main?** | 🔴 NO | Branch uncommitted | Merge release/cleanup-cruft |
| **Test Pass Rate** | 🟡 99.55% | Needs ≥99.5% | Fix CCSID1122, WizardEvent |
| **Build Automation** | 🟢 GREEN | None | - |
| **Documentation** | 🟢 GREEN | None | - |
| **GitHub Releases** | 🟢 GREEN | None | - |
| **Maven Central** | 🔴 NO | Not configured | Plan for 0.13.0 |
| **Docker Publishing** | 🔴 NO | No Dockerfile | Plan for 0.13.0 |

---

## Immediate Actions (Next 4 Hours)

### 1. Fix Failing Tests (2-4 hours) - BLOCKER

```bash
# Identify failing tests
./gradlew test --no-daemon 2>&1 | grep -A 5 "FAILED"

# High-priority failures:
# 1. CCSID1122MigrationTest (6 failures)
#    → Likely broken by refactor commits c21c5a8 or a09ea7c
#    → Check CCSID1122Converter.init() return value
#
# 2. WizardEventRecordTest (1+ failures)
#    → Check exception type assertions
#    → May have changed during cleanup

# Fix and verify
./gradlew test --no-daemon
# Expected: ≤23 failures (99.5% pass rate)
```

### 2. Merge Refactor Branch (30 minutes)

```bash
# Create PR if not exists
git checkout refactor/cleanup-cruft-and-docs
git status  # Verify staged work

# Review + merge
git checkout main
git merge --no-ff refactor/cleanup-cruft-and-docs \
  -m "merge: refactor/cleanup-cruft-and-docs"
git push origin main

# Verify clean main branch
git status  # Should show "On branch main, nothing to commit"
```

### 3. Update Version (10 minutes)

```bash
# Edit gradle.properties
grep "^version=" gradle.properties  # Should show 0.12.0 (no -SNAPSHOT)

# Verify README examples
grep "implementation 'org" README.md | head -1
# Should show: 0.12.0

# Commit if needed
git commit -am "release: prepare 0.12.0"
```

### 4. Verify Release Readiness (30 minutes)

```bash
# Run full checklist
./gradlew test --no-daemon 2>&1 | tail -3  # Test pass rate
./gradlew clean build -x test --no-daemon  # Build success
git status  # No uncommitted changes
grep "## \[0.12.0\]" CHANGELOG.md  # Changelog entry
```

---

## Decision Tables

### "Should We Release Now?"

```
Q1: Are tests passing (≥99.5%)?
    ├─ NO → Fix tests first (2-4 hrs)
    └─ YES ↓

Q2: Are all features committed to main?
    ├─ NO → Finish development, create PR
    └─ YES ↓

Q3: Are docs up-to-date?
    ├─ NO → Update README, CHANGELOG, etc.
    └─ YES ↓

Q4: Has security scan passed?
    ├─ NO → Fix vulnerabilities
    └─ YES ↓

RESULT: ✅ Ready to release!
→ Proceed to RELEASE_QUICK_START.md, Phase 1
```

### "What Should We Release?"

```
Q1: Is it feature-complete for a milestone?
    ├─ NO → Add more features
    └─ YES ↓

Q2: Version number?
    ├─ New features (backward compat) → MINOR bump (0.12 → 0.13)
    ├─ Bug fixes only → PATCH bump (0.12.0 → 0.12.1)
    └─ Breaking changes → MAJOR bump (when ready for 1.0)

Q3: Can we commit to API stability until next release?
    ├─ NO → Plan 2+ releases for stability
    └─ YES ↓

RESULT: Determine version number and release criteria
→ See RELEASE_MANAGEMENT.md, Part 2, "Release Criteria"
```

### "Did We Break Something?"

```
Problem: Tests failing after merge
Solution: Rollback merge, fix in feature branch
Command: git reset --hard HEAD~1 && git push --force

Problem: Found security vulnerability in released version
Solution: Create 0.12.1 hotfix (see Part 8, Rollback)

Problem: Maven publish failed
Solution: Retry. If fails again, see Part 6, Maven Central setup

→ See RELEASE_MANAGEMENT.md, Part 8 for full rollback procedures
```

---

## Learning Paths by Role

### For Release Managers (First Time)

1. Read this file (10 min)
2. Read RELEASE_MANAGEMENT.md, Part 1-3 (30 min)
3. Read RELEASE_QUICK_START.md (15 min)
4. Run through a dry run (mock release, don't push): 30 min
5. Execute actual release with mentoring: 50 min

**Total onboarding:** 2-3 hours

### For Maintainers (Merging PRs)

1. Read CONTRIBUTING.md (existing, 5 min)
2. Read RELEASE_MANAGEMENT.md, Part 5 (Branching Strategy) (10 min)
3. Follow branch naming: `feature/*`, `bugfix/*`, `hotfix/*`
4. Require 1+ approval before merge
5. Use "Merge (no squash)" to preserve commit history

**Recurring:** 5 minutes per PR

### For Stakeholders (Checking Progress)

1. Read RELEASE_READINESS_SCORECARD.md (20 min)
2. Check back weekly for updated metrics
3. Watch GitHub Releases for published artifacts
4. Review CHANGELOG.md for breaking changes before upgrading

**Recurring:** 5 minutes per week

### For Operations (CI/CD Automation)

1. Read RELEASE_MANAGEMENT.md, Part 4 (Release Process Design) (20 min)
2. Review `.github/workflows/release.yml` (10 min)
3. Setup credentials for Maven Central (if needed): 30 min
4. Setup credentials for Docker Hub (if needed): 30 min
5. Test end-to-end publish (mock tag): 30 min

**Recurring:** 5 minutes per release to monitor workflow

---

## Version & Release Timeline

```
v0.12.0 (February 2026) ← CURRENT, BLOCKED ON TESTS
├─ Target: Feb 15
├─ Artifacts: GitHub Releases + JAR
├─ Status: Tests must reach ≥99.5%
└─ Blocker: 61 failing tests

v0.13.0 (March 2026)
├─ Target: Mar 31
├─ Features: Maven Central, Docker Hub
├─ Test requirement: ≥99.5%
└─ New capabilities: TBD

v1.0.0 (Q3 2026, July-September)
├─ Target: Jul 15
├─ Stability: 30+ days (no critical hotfixes)
├─ Test requirement: ≥99.7%
├─ API requirement: Freeze all public APIs
└─ Artifact requirement: Maven Central + Docker Hub stable
```

---

## Key Definitions

**Release Milestone:** Version number + date (e.g., v0.12.0 / Feb 15)

**Code Freeze:** No new PRs merged, only bug fixes. Lasts 1-2 hours.

**Tag:** Git tag marking release commit (e.g., `git tag v0.12.0`)

**Artifact:** Built output ready for distribution (JAR, Docker image, etc.)

**Publish:** Make artifact available to users (GitHub Releases, Maven Central, Docker Hub)

**Rollback:** Remove/replace a released artifact due to bugs or security issues

**Hotfix:** Patch release (0.y.z) for critical fixes post-release

**Breaking Change:** API change requiring consumer code updates (e.g., renamed method)

---

## Metrics to Watch

### Pre-Release (Weekly)

- [ ] Test pass rate (target: ≥99.5%)
- [ ] Build time (target: <30 seconds)
- [ ] Security scan status (target: 0 critical)
- [ ] Dependency audit (target: 0 unpatched CVEs)

### Post-Release (Daily for 1 week)

- [ ] Artifact downloads (GitHub Releases)
- [ ] User feedback (GitHub Issues)
- [ ] Critical bugs reported (escalate to hotfix if found)

### Monthly

- [ ] Code coverage (trend)
- [ ] Performance benchmark (regression detection)
- [ ] Upstream TN5250J compatibility (if still tracking)

---

## Frequently Asked Questions

**Q: Can I release 0.12.0 before fixing the 61 failing tests?**
A: No. Release criteria require ≥99.5% pass rate. Fix tests first (2-4 hours).

**Q: What if I find a critical bug in released v0.12.0?**
A: Create `hotfix/0.12.1` branch from tag `v0.12.0`, fix, tag `v0.12.1`, release. See RELEASE_MANAGEMENT.md, Part 8.

**Q: When can we publish to Maven Central?**
A: 0.13.0 (March 2026). Requires Sonatype OSSRH account setup (one-time, 30 min).

**Q: Why is test pass rate ≥99.5%, not 100%?**
A: Pre-1.0, some integration tests may be skipped (real i5 dependency). Allows for known limitations.

**Q: Do I need GPG signing for GitHub Releases?**
A: No (recommended but not required). Required for Maven Central (0.13.0).

**Q: What if release.yml workflow fails?**
A: Check logs, fix issue, delete tag, re-push. See RELEASE_QUICK_START.md, Troubleshooting.

**Q: How long does a release take?**
A: 25-50 minutes. Critical path: 25 min (test → tag → build). Validation + announcement: 25 min (optional).

**Q: Can we automate this further?**
A: Yes. Consider semantic-release or equivalent (future enhancement).

---

## Support & Contact

| Need | Resource |
|------|----------|
| Full process details | RELEASE_MANAGEMENT.md |
| Quick reference | RELEASE_QUICK_START.md |
| Current status | RELEASE_READINESS_SCORECARD.md |
| Questions | GitHub Discussions or Issues |
| Security issue | GitHub Security Advisories |

---

## Next Immediate Steps (Today)

1. **Fix failing tests** (BLOCKER) - 2-4 hours
   - CCSID1122MigrationTest (6 failures)
   - WizardEventRecordTest (1+ failures)
   - Verify: `./gradlew test` shows ≤23 failures

2. **Merge refactor branch** - 30 minutes
   - `git checkout main && git merge --no-ff refactor/cleanup-cruft-and-docs`
   - Push to origin

3. **Verify release readiness** - 30 minutes
   - Run full pre-release checklist (RELEASE_QUICK_START.md, Phase 1)
   - Confirm all criteria met

4. **Execute release** (once tests pass) - 50 minutes
   - Follow RELEASE_QUICK_START.md, Phases 1-6
   - Publish to GitHub Releases
   - Announce to community

**Estimated Timeline:** Test fixes (2-4 hrs) + Merge (30 min) + Release (50 min) = **3-5 hours total**

---

## Document Navigation Map

```
START HERE
    ↓
    ├─ Want quick checklist? → RELEASE_QUICK_START.md
    ├─ Want full details? → RELEASE_MANAGEMENT.md
    ├─ Want status dashboard? → RELEASE_READINESS_SCORECARD.md
    ├─ Want branching rules? → CONTRIBUTING.md (existing)
    ├─ Want architecture? → ARCHITECTURE.md (existing)
    └─ Want to merge a PR? → CONTRIBUTING.md + Part 5 of RELEASE_MANAGEMENT.md
```

---

## Sign-Off

**Release Management Framework:** COMPLETE
**Implementation Status:** BLOCKED (61 failing tests)
**Recommendation:** Fix tests, then proceed with 0.12.0 release per RELEASE_QUICK_START.md

**Framework Owner:** Release Manager (Eric C. Mumford)
**Last Updated:** February 13, 2026, 16:00 UTC
**Next Review:** February 15, 2026 (post-test fixes)

---

**You are here. Ready to release? Go to RELEASE_QUICK_START.md. Questions? Read RELEASE_MANAGEMENT.md. Want details? See RELEASE_READINESS_SCORECARD.md.**

