# Release Quick Start Guide

**For:** Release managers who need to execute a release in 30 minutes
**Contains:** Checklists, copy-paste commands, decision trees

---

## Is It Time to Release?

### Decision Tree

```
START
  │
  ├─ Run: ./gradlew test
  │   └─ Tests pass (≥99.5%)?
  │       ├─ YES → Continue to PRE-RELEASE
  │       └─ NO → FIX TESTS (see RELEASE_MANAGEMENT.md, Part 8)
  │
  ├─ Branch = main?
  │   ├─ YES → Continue
  │   └─ NO → Switch: git checkout main && git pull
  │
  ├─ Any uncommitted changes?
  │   ├─ NO → Continue
  │   └─ YES → Stash or commit: git stash
  │
  └─ Continue to: PRE-RELEASE PHASE
```

---

## Phase 1: Pre-Release (5 minutes)

### Checklist

- [ ] `./gradlew test` passes (≥99.5%)
- [ ] On main branch: `git status` shows "On branch main"
- [ ] No uncommitted changes: `git status` shows "clean"
- [ ] Version bumped in `gradle.properties`: `version=0.12.0` (no -SNAPSHOT)
- [ ] CHANGELOG.md has entry for this version
- [ ] README.md examples use correct version

### Copy-Paste Commands

```bash
# 1. Verify test pass rate
./gradlew test --no-daemon 2>&1 | grep "tests completed"
# Expected: "13270 tests completed, 0 failed" or acceptable failure count

# 2. Verify no uncommitted changes
git status
# Expected: "nothing to commit, working tree clean"

# 3. Verify version
grep "^version=" gradle.properties
# Expected: version=0.12.0 (no -SNAPSHOT)

# 4. Verify changelog entry
grep -A 5 "## \[0.12.0\]" CHANGELOG.md
# Expected: Found with date
```

### Go/No-Go Decision

✅ **GO:** All checks passed → **Continue to Phase 2**
❌ **NO-GO:** Any check failed → **Fix and retry Phase 1**

---

## Phase 2: Code Freeze & Tag (5 minutes)

### Checklist

- [ ] Main is up-to-date: `git pull origin main`
- [ ] Create annotated tag
- [ ] Push tag (triggers CI)
- [ ] Verify CI triggered

### Copy-Paste Commands

```bash
# 1. Ensure main is up-to-date
git checkout main
git pull origin main

# 2. Verify ready
./gradlew clean build -x test --no-daemon
# Expected: BUILD SUCCESSFUL

# 3. Create annotated tag
git tag -a v0.12.0 -m "Release version 0.12.0

Features:
- GuiGraphicBuffer extraction (5 classes)
- Headless architecture interfaces
- 99.55% test pass rate (13,270+ passing)
- Character encoding fixes (6 CCSID codepages)

See CHANGELOG.md for full details."

# 4. Verify tag created
git show v0.12.0 | head -10

# 5. Push tag (TRIGGERS CI)
git push origin v0.12.0
```

### Monitor CI

```bash
# Watch for workflow to start
# https://github.com/heymumford/hti5250j/actions

# Or use GitHub CLI
gh run list --workflow=release.yml --limit=1
```

---

## Phase 3: Artifact Build (CI/CD, ~10 minutes)

### What Happens Automatically

When you `git push origin v0.12.0`:
1. GitHub Actions detects `v*` tag
2. Runs `release.yml` workflow:
   - Checkout at tag
   - Setup Java 21
   - Build JAR: `./gradlew build -x test`
   - Generate release notes
   - Upload JAR to artifacts
3. Creates GitHub Release (auto-populated)

### Monitor

```bash
# Option 1: GitHub Web UI
# https://github.com/heymumford/hti5250j/actions
# → Click "Release" workflow → Monitor "build" job

# Option 2: GitHub CLI
gh run list --workflow=release.yml --limit=5

# Option 3: Manual verification
# Wait ~10 mins, then check:
gh release view v0.12.0
# Expected: Release exists with JAR attached
```

### If Build Fails

```bash
# 1. Check workflow logs
gh run list --workflow=release.yml | head -1
gh run view <run-id> --log

# 2. Common fixes
# - Version mismatch: Ensure gradle.properties = tag version
# - Compilation error: Run locally: ./gradlew build -x test
# - Artifact missing: Check build/libs/

# 3. If unfixable, delete tag and retry
git tag -d v0.12.0
git push origin :refs/tags/v0.12.0
# ... fix issue ...
git tag -a v0.12.0 -m "..."
git push origin v0.12.0
```

---

## Phase 4: Publish Phase (5-10 minutes)

### For 0.12.0: GitHub Releases Only

```bash
# Nothing to do - automatically created by release.yml
# Verify at: https://github.com/heymumford/hti5250j/releases

# Manual fallback (if auto-publish failed):
gh release create v0.12.0 \
  --title "Release v0.12.0" \
  --notes-from-changelog CHANGELOG.md \
  build/libs/hti5250j-0.12.0.jar
```

### For 0.13.0+: Maven Central (One-Time Setup)

See RELEASE_MANAGEMENT.md, Part 6 ("Maven Central Publishing")

### For 0.13.0+: Docker Hub (One-Time Setup)

See RELEASE_MANAGEMENT.md, Part 6 ("Docker Hub Publishing")

---

## Phase 5: Validation (10-15 minutes)

### Smoke Tests

```bash
# 1. Download artifact
wget https://github.com/heymumford/hti5250j/releases/download/v0.12.0/hti5250j-0.12.0.jar

# 2. Verify not corrupted (file size > 1MB)
ls -lh hti5250j-0.12.0.jar
# Expected: ~2.5 MB

# 3. Verify classes present
unzip -l hti5250j-0.12.0.jar | grep "\.class$" | head -5
# Expected: Multiple .class files found

# 4. Test as library (local install)
./gradlew publishToMavenLocal

# 5. Verify Maven Central (24-48 hours after publish)
# curl https://repo1.maven.org/maven2/org/hti5250j/hti5250j/0.12.0/
```

### Quick Health Check

```bash
# If CLI exists, test:
java -jar hti5250j-0.12.0.jar --version

# Verify no suspicious files
unzip -l hti5250j-0.12.0.jar | grep -E "(password|key|secret)"
# Expected: (no output = good)
```

---

## Phase 6: Announcement (Next Day)

### Quick Checklist

- [ ] Create GitHub Discussion post
- [ ] Update README.md version (if needed)
- [ ] Close GitHub milestone
- [ ] Create milestone for next release

### Copy-Paste

```bash
# 1. Create GitHub Discussion
gh discussion create \
  --title "Release: v0.12.0 Published" \
  --body "HTI5250J v0.12.0 is now available!

**Key Changes:**
- GuiGraphicBuffer extraction (5 classes)
- 99.55% test pass rate (13,270+ passing)
- Character encoding fixes

**Download:**
https://github.com/heymumford/hti5250j/releases/tag/v0.12.0

See CHANGELOG.md for full details.

Questions? Ask here or open an issue."

# 2. Update version in gradle.properties (next release prep)
sed -i.bak 's/version=0.12.0/version=0.13.0-SNAPSHOT/' gradle.properties
git add gradle.properties
git commit -m "build: bump to 0.13.0-SNAPSHOT"
git push origin main

# 3. Create milestone for next release (Web UI)
# https://github.com/heymumford/hti5250j/milestones/new
# Title: "0.13.0"
# Due Date: 2 months from now
```

---

## Troubleshooting

### Problem: Tests Failing, Can't Release

**Solution:**
```bash
# See which tests are failing
./gradlew test --no-daemon 2>&1 | grep "FAILED"

# Fix them (see RELEASE_MANAGEMENT.md, Part 8)
# Then retry Phase 1
```

### Problem: Build Artifact Missing After CI Completes

**Solution:**
```bash
# 1. Check workflow logs
gh run view <run-id> --log | tail -50

# 2. Build locally to verify
./gradlew build -x test --no-daemon
ls build/libs/hti5250j-0.12.0.jar

# 3. If local build works, re-run workflow
git push origin v0.12.0 --force  # NOT RECOMMENDED, but works if desperate
```

### Problem: GitHub Release Created But JAR Not Attached

**Solution:**
```bash
# 1. Verify artifact exists
ls build/libs/*.jar

# 2. Manually add to release
gh release upload v0.12.0 build/libs/hti5250j-0.12.0.jar
```

### Problem: Need to Rollback Release

**Solution:**
```bash
# 1. Delete tag
git tag -d v0.12.0
git push origin :refs/tags/v0.12.0

# 2. Delete GitHub Release (Web UI)
# https://github.com/heymumford/hti5250j/releases
# Click v0.12.0 → Delete

# 3. See RELEASE_MANAGEMENT.md, Part 8 for detailed rollback
```

---

## Emergency Contacts

| Issue | Action |
|-------|--------|
| Build failing in CI | Check workflow logs, fix locally, retry push |
| Artifact missing | Manually upload with `gh release upload` |
| Critical bug post-release | Create hotfix branch, release 0.12.1 |
| Security vulnerability | Create security advisory, release ASAP |

---

## Time Estimates

| Phase | Duration | Critical Path? |
|-------|----------|----------------|
| Phase 1 (Pre-Release) | 5 min | Yes |
| Phase 2 (Code Freeze) | 5 min | Yes |
| Phase 3 (Build) | 10 min | Yes |
| Phase 4 (Publish) | 5 min | Yes |
| Phase 5 (Validate) | 15 min | No (can skip) |
| Phase 6 (Announce) | 10 min | No (next day) |
| **Total** | **50 min** | **25 min critical** |

---

## Success Criteria

✅ Release is successful when:
1. Tag `v0.12.0` exists in git
2. GitHub Release shows JAR artifact
3. Artifact downloads without error
4. Unzipping JAR shows `.class` files
5. No errors in workflow logs

---

## Next Steps After Release

```bash
# 1. Bump version for next development
sed -i.bak 's/version=0.12.0/version=0.13.0-SNAPSHOT/' gradle.properties
git add gradle.properties
git commit -m "build: bump to 0.13.0-SNAPSHOT"
git push origin main

# 2. Review lessons learned (30 min debrief)
# - What took longer than expected?
# - What was confusing?
# - Update RELEASE_QUICK_START.md for next time

# 3. Plan 0.13.0 features
# - Maven Central publishing setup
# - Docker image automation
# - New feature branches (TBD)

# 4. Schedule 0.13.0 release (30 days out)
# - Due date: March 31, 2026 (estimated)
```

---

## One-Liner Release (Experienced Operators Only)

```bash
# Only run if all tests pass and version updated!
git checkout main && git pull && \
git tag -a v0.12.0 -m "Release v0.12.0" && \
git push origin v0.12.0 && \
echo "✓ Tag pushed, CI running. Monitor at: https://github.com/heymumford/hti5250j/actions"
```

---

**Last Updated:** February 13, 2026
**For Detailed Info:** See RELEASE_MANAGEMENT.md
**Questions?** GitHub Issues or Discussions

