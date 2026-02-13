# HTI5250J Release Management Strategy

**Audience:** Maintainers, release managers, CI/CD operators
**Last Updated:** February 13, 2026
**Version:** 1.0

---

## Executive Summary

HTI5250J is on the pre-1.0 path: `0.12.0` → `0.13.0` → `1.0.0`. This document establishes:
- **Current readiness:** YELLOW (61 failing tests, incomplete release automation)
- **Release gates:** Specific criteria for each milestone
- **Artifact pipeline:** GitHub Releases → Maven Central → Docker Hub
- **Rollback procedures:** How to handle bad releases

---

## Part 1: Release Readiness Assessment

### Current State (0.12.0 - February 2026)

| Factor | Status | Details | Risk Level |
|--------|--------|---------|-----------|
| **Build System** | GREEN | Gradle 9.3.1 with Java 21, reproducible | Low |
| **Test Suite** | RED | 13,270 tests: 60 failing (0.45%), 46 skipped (0.35%) | HIGH |
| **CI/CD** | YELLOW | GitHub Actions works, release workflow incomplete | Medium |
| **Documentation** | GREEN | ARCHITECTURE.md, TESTING.md, CONTRIBUTING.md solid | Low |
| **Code Quality** | YELLOW | Checkstyle, SpotBugs enabled, lint config in place | Medium |
| **Maven Central** | RED | Not publishing, no Maven credentials configured | CRITICAL |
| **Docker Images** | RED | No Docker build pipeline | Medium |
| **Semantic Versioning** | GREEN | SemVer policy defined (CONTRIBUTING.md) | Low |
| **Changelog** | YELLOW | Basic structure, sparse entries | Medium |
| **License Compliance** | GREEN | GPL-2.0-or-later, SPDX headers enforced | Low |
| **Dependency Mgmt** | YELLOW | Manual jar management in `lib/`, no lock file | Medium |

### Overall Readiness: YELLOW (Pre-Release)

**Can release 0.12.0?** NO - 61 failing tests must be resolved first.
**Can merge to main?** NO - Branch has unmerged work (`refactor/cleanup-cruft-and-docs`).

---

## Part 2: Release Criteria by Milestone

### Milestone: 0.12.0 (Current, Unreleased)

**Target Date:** February 15, 2026

#### Blocking Criteria (MUST HAVE)

- [ ] **Test Pass Rate ≥ 99.5%**
  - Currently: 99.55% (13,270 passing / 13,637 total)
  - Required: ≥ 13,614 passing (allow max 23 failures)
  - Current gap: 61 failing = **61 tests must be fixed**
  - High-priority failures:
    - `CCSID1122MigrationTest` (6 failures)
    - `WizardEventRecordTest` (1+ failures)
    - Unknown field initialization issues

- [ ] **All Commits on Main**
  - `refactor/cleanup-cruft-and-docs` merged to main
  - Commits a09ea7c and c21c5a8 present in main
  - No staged or unstaged changes in release branch

- [ ] **Git Tag Created**
  - Tag format: `v0.12.0`
  - Tag message: Include changelog summary
  - Signed tag recommended (GPG)

- [ ] **Build Artifact Generated**
  - `./gradlew clean build` succeeds
  - JAR file: `build/libs/hti5250j-0.12.0.jar`
  - SHA-256 checksum calculated and documented

#### Gating Criteria (SHOULD HAVE)

- [ ] **Changelog Updated**
  - Add final 0.12.0 section with:
    - Summary statement
    - Key achievements
    - Breaking changes (if any)
    - Link to full release notes
  - Date section: February 2026

- [ ] **Code Quality Report**
  - Run checkstyle, SpotBugs
  - Address all P0/P1 issues
  - Document known P2/P3 issues (wontfix)

- [ ] **Security Scan Passing**
  - CodeQL: No critical vulnerabilities
  - Semgrep: No hardcoded credentials, insecure crypto
  - Dependency audit: No known CVEs in direct deps

- [ ] **Documentation Review**
  - README.md: Accurate for release version
  - ARCHITECTURE.md: Reflects current system
  - TESTING.md: Reflects test domains
  - SECURITY.md: Accurate policies

#### Nice-to-Have (0.12.0)

- [ ] Docker image published to Docker Hub
- [ ] JAR published to Maven Central (optional for pre-1.0)
- [ ] Release notes in GitHub Releases

---

### Milestone: 0.13.0 (Next Minor Release)

**Target Date:** March 31, 2026
**Release Trigger:** Feature branch(es) complete, tests pass, changelog updated

#### Blocking Criteria

- [ ] Test Pass Rate ≥ 99.5% (same as 0.12.0)
- [ ] All feature branches merged to main
- [ ] 0.12.0 tag exists and is ancestor of release commit
- [ ] Version bumped to `0.13.0` in:
  - `gradle.properties` (version field)
  - `README.md` (quick start examples)
  - `build.gradle` (version fallback)

#### Gating Criteria

- [ ] New features documented in CHANGELOG.md
- [ ] Breaking changes listed with migration guide (if applicable)
- [ ] ARCHITECTURE.md updated for new components (if applicable)
- [ ] Security scan passing (CodeQL, Semgrep)

#### New Capabilities (0.13.0 Target)

*To be defined by feature branches. Suggested areas:*
- Docker image publication to Docker Hub
- Maven Central publishing setup (new)
- Dependency lock file (gradle.lockfile)
- Performance benchmarking framework
- Extended headless APIs (e.g., session pooling, replay)

---

### Milestone: 1.0.0 (Stable Release)

**Target Date:** Q3 2026 (July-September)
**Release Trigger:** All pre-1.0 work complete, stability demonstrated

#### Blocking Criteria

- [ ] Test Pass Rate ≥ 99.7% (stricter for 1.0)
- [ ] **Zero breaking changes since 0.12.0** (public APIs frozen)
- [ ] **30 days of stability** (no critical hotfixes needed)
- [ ] Security audit: All vulnerabilities <7.0 CVSS (high severity)
- [ ] Maven Central publishing working end-to-end
- [ ] Docker image stable and documented

#### Gating Criteria

- [ ] Performance benchmark established (baseline for future releases)
- [ ] API documentation complete (JavaDoc coverage ≥ 80% public APIs)
- [ ] User guide for headless execution (CLI, library usage)
- [ ] Migration guide from upstream TN5250J
- [ ] Supported platforms documented (Java versions, OSes)
- [ ] Dependency vulnerability audit with approved exceptions list

#### 1.0.0 Release Notes Must Include

- Summary of journey from 0.1.0 to 1.0.0
- Known limitations and non-goals
- Upgrade path for 0.y.z consumers
- Backward compatibility guarantees (if any)
- Timeline for support (e.g., will 0.13.x receive security patches?)

---

## Part 3: Release Process Design

### End-to-End Release Workflow

```
┌──────────────────────────────────────────────────────────────┐
│ 1. PRE-RELEASE PHASE (2-3 days)                              │
│    • Assess readiness (this doc)                             │
│    • Create release branch from main                         │
│    • Merge final PRs                                         │
│    • Run full test suite                                     │
│    • Update CHANGELOG.md                                     │
│    • Update version in gradle.properties                     │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│ 2. CODE FREEZE PHASE (1-2 hours)                             │
│    • Ensure main is stable                                   │
│    • Create annotated git tag (v0.y.z)                      │
│    • Push tag to origin                                      │
│    • Verify CI/CD release workflow triggers                  │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│ 3. ARTIFACT BUILD PHASE (5-10 mins)                          │
│    • GitHub Actions runs `gradle build`                      │
│    • Generates JAR, test results                             │
│    • Calculates SHA-256 checksums                            │
│    • Uploads to GitHub Artifacts                             │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│ 4. PUBLISH PHASE (varies)                                    │
│    a. GitHub Releases (auto)                                │
│       • Draft release notes from git log                     │
│       • Attach JAR + checksums                               │
│       • Publish (public)                                     │
│    b. Maven Central (TBD - blocked by 0.12.0)               │
│       • Publish via Sonatype OSSRH                           │
│       • Verify artifact (24-48 hours)                        │
│    c. Docker Hub (TBD - blocked by 0.13.0)                  │
│       • Build Dockerfile image                              │
│       • Tag: docker.io/heymumford/hti5250j:0.y.z            │
│       • Push and verify                                      │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│ 5. VALIDATION PHASE (1-4 hours)                              │
│    • Download artifact from GitHub Releases                  │
│    • Verify SHA-256 checksum                                 │
│    • Test JAR as library (maven local install)              │
│    • Smoke test CLI (if applicable)                          │
│    • Check Maven Central (if publishing)                     │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│ 6. ANNOUNCEMENT PHASE (next day)                             │
│    • Post release notes to GitHub Discussions                │
│    • Announce on project channels                            │
│    • Update website/docs with new version                    │
│    • Close version milestone in issue tracker                │
└──────────────────────────────────────────────────────────────┘
```

### Detailed Steps

#### Step 1: Pre-Release Readiness Check

**Run manually (before code freeze):**

```bash
# 1a. Check test pass rate
./gradlew test --no-daemon 2>&1 | tail -5
# Expected: "X tests completed, N failed" where N ≤ 61 for 0.12.0

# 1b. Verify no uncommitted changes
git status
# Expected: "On branch main" with no changes or all changes committed

# 1c. Run full build
./gradlew clean build -x test --no-daemon
# Expected: BUILD SUCCESSFUL

# 1d. Verify version in files
grep "^version=" gradle.properties
# Expected: version=0.12.0 (or next milestone)

# 1e. Check changelog has entry
grep "## \[0.12.0\]" CHANGELOG.md
# Expected: Found
```

**Go/No-Go Decision:**

- If all checks pass AND test pass rate ≥ 99.5% → **GO to code freeze**
- If tests failing AND not critical → **CONTINUE** (fix in next release)
- If build broken → **NO-GO** (fix before release attempt)

#### Step 2: Code Freeze & Git Tag

**Only release manager executes:**

```bash
# 2a. Ensure on main and up-to-date
git checkout main
git pull origin main

# 2b. Create annotated tag (signed recommended)
git tag -a v0.12.0 \
  -m "Release version 0.12.0 - Headless Terminal Emulator

Features:
- GuiGraphicBuffer extraction (5 classes)
- Headless architecture interfaces
- 67 new tests for headless components
- 99.55% test pass rate (13,270+ passing)
- Character encoding fixes (6 CCSID codepages)

See CHANGELOG.md for full details."

# 2c. Verify tag created
git show v0.12.0 | head -20

# 2d. Push tag (triggers CI/CD)
git push origin v0.12.0
```

**Verify GitHub Actions triggered:**
- Go to https://github.com/heymumford/hti5250j/actions
- Wait for `Release` workflow to start
- Monitor build job completion

#### Step 3: Artifact Build (CI/CD)

**Automatically executed by GitHub Actions:**

The `.github/workflows/release.yml` runs:
1. Checkout code at tag
2. Setup Java 21
3. Run `./gradlew build -x test` (on tag, skip tests)
4. Upload JAR to artifacts
5. Generate release notes from git log

**Monitoring:**

```bash
# Wait for workflow to complete (typically 5-10 mins)
# Check at: https://github.com/heymumford/hti5250j/actions

# Manually verify build artifacts
./gradlew clean build -x test --no-daemon
ls -lh build/libs/
# Expected: hti5250j-0.12.0.jar (size varies)
```

#### Step 4: Publish Phase

**Phase 4a: GitHub Releases (CURRENTLY IMPLEMENTED)**

Already automated in `.github/workflows/release.yml`:

```bash
# Happens automatically:
# 1. Downloads JAR from artifacts
# 2. Generates release notes from git log
# 3. Creates GitHub Release with JAR attached
# 4. Makes release public

# Manual verification:
# Visit: https://github.com/heymumford/hti5250j/releases
# Verify v0.12.0 release page shows:
# - Title: "Release v0.12.0"
# - Notes: git log summary
# - JAR downloadable
```

**Phase 4b: Maven Central (NOT YET IMPLEMENTED)**

*Blocked until Maven publishing credentials configured.*

**Steps to enable (0.13.0 or later):**

1. Create Sonatype OSSRH account: https://central.sonatype.org/register/
2. Configure `~/.gradle/gradle.properties`:
   ```
   ossrhUsername=your-username
   ossrhPassword=your-password
   ```
3. Add Maven publishing plugin to `build.gradle`:
   ```gradle
   plugins {
       id 'java'
       id 'signing'
       id 'maven-publish'
   }

   publishing {
       publications {
           mavenJava(MavenPublication) {
               from components.java
               groupId = 'org.hti5250j'
               artifactId = 'hti5250j'
               version = project.version
           }
       }
       repositories {
           maven {
               url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
               credentials {
                   username = findProperty("ossrhUsername") ?: "unknown"
                   password = findProperty("ossrhPassword") ?: "unknown"
               }
           }
       }
   }
   ```
4. Update CI workflow to run `./gradlew publish` on tag
5. Monitor Sonatype staging repository

**Phase 4c: Docker Hub (NOT YET IMPLEMENTED)**

*Suggested for 0.13.0. Requires Dockerfile.*

**Steps to enable:**

1. Create `Dockerfile` at repo root:
   ```dockerfile
   FROM eclipse-temurin:21-jdk-jammy

   LABEL maintainer="Eric C. Mumford <ericmumford@outlook.com>"
   LABEL org.opencontainers.image.source=https://github.com/heymumford/hti5250j
   LABEL org.opencontainers.image.documentation=https://github.com/heymumford/hti5250j
   LABEL org.opencontainers.image.licenses=GPL-2.0-or-later

   WORKDIR /opt/hti5250j

   # Copy built JAR
   COPY build/libs/*.jar /opt/hti5250j/

   # Copy CLI entry point (if exists)
   COPY scripts/i5250 /usr/local/bin/i5250
   RUN chmod +x /usr/local/bin/i5250

   ENTRYPOINT ["java", "-jar", "hti5250j-*.jar"]
   ```

2. Add Docker Hub credentials to GitHub Secrets:
   - `DOCKER_HUB_USERNAME`
   - `DOCKER_HUB_TOKEN`

3. Add Docker build job to `.github/workflows/release.yml`:
   ```yaml
   docker:
     name: Build and Push Docker Image
     runs-on: ubuntu-latest
     needs: build
     if: startsWith(github.ref, 'refs/tags/v')
     steps:
       - uses: actions/checkout@v4
       - uses: docker/setup-buildx-action@v3
       - uses: docker/login-action@v3
         with:
           username: ${{ secrets.DOCKER_HUB_USERNAME }}
           password: ${{ secrets.DOCKER_HUB_TOKEN }}
       - uses: docker/build-push-action@v5
         with:
           context: .
           push: true
           tags: |
             heymumford/hti5250j:${{ github.ref_name }}
             heymumford/hti5250j:latest
   ```

4. Create Docker Hub public repository: https://hub.docker.com/repository/create

#### Step 5: Validation Phase

**Manual smoke testing:**

```bash
# 5a. Download JAR from GitHub Release
wget https://github.com/heymumford/hti5250j/releases/download/v0.12.0/hti5250j-0.12.0.jar

# 5b. Verify checksum (when available)
sha256sum hti5250j-0.12.0.jar
# Compare with published value

# 5c. Test as library (install locally)
./gradlew publishToMavenLocal
# Then in separate project:
# Add to build.gradle:
# dependencies { implementation 'org.hti5250j:hti5250j:0.12.0' }

# 5d. Verify no vulnerabilities in released JAR
java -jar hti5250j-0.12.0.jar --version  # (if CLI exists)
# Or unzip and inspect
unzip -l hti5250j-0.12.0.jar | grep -c "\.class"  # Should have classes

# 5e. Check Maven Central mirror (if publishing enabled)
curl -s https://repo1.maven.org/maven2/org/hti5250j/hti5250j/0.12.0/ \
  | grep jar
# May take 24-48 hours to appear
```

#### Step 6: Announcement Phase

**Day after release:**

1. **GitHub Discussions:** Post release notes
   - https://github.com/heymumford/hti5250j/discussions
   - Title: "Release: v0.12.0"
   - Include key changes and upgrade notes

2. **Update Documentation:**
   - README.md: Update "Quick Start" section if version-dependent
   - docs/: Update any version-specific guides

3. **Close Milestone:**
   - https://github.com/heymumford/hti5250j/milestones
   - Mark "0.12.0" as closed

4. **Prepare for Next Release:**
   - Bump version in `gradle.properties` to next milestone (e.g., 0.13.0-SNAPSHOT)
   - Create new milestone for next release
   - Commit on main

---

## Part 4: Version Numbering Plan

### Semantic Versioning Applied to HTI5250J

**Format:** `MAJOR.MINOR.PATCH` (pre-1.0 mode)

**Current:** `0.12.0`
**Next planned:** `0.13.0` (March 2026) → `1.0.0` (Q3 2026)

### Version Increments

| Version | Trigger | Notes |
|---------|---------|-------|
| `0.12.z` | Patch releases | Backport security/critical fixes from main (unlikely pre-1.0) |
| `0.13.0` | New minor features | New APIs, capabilities; backward compatible |
| `0.14.0` | Additional features | If 0.13.0 takes >1 month |
| `1.0.0` | API stability | Freeze public APIs, 30+ days stable |
| `1.1.0` | Post-1.0 features | Only after 1.0.0 released |

### Pre-Release Versions (Development Only)

These versions NEVER released, only used in development:

```
0.12.0-SNAPSHOT    # After 0.12.0 released, before 0.13.0
0.13.0-SNAPSHOT    # After 0.13.0 released, before 1.0.0
1.0.0-SNAPSHOT     # After 1.0.0 released
```

**Where used:**
- `gradle.properties` on main branch between releases
- Maven SNAPSHOT repositories (CI builds only)
- NOT in GitHub Releases

### Version File Locations

Must be updated together for release:

1. **Primary:** `gradle.properties`
   ```gradle
   version=0.12.0
   ```

2. **Documentation:** `README.md`
   ```markdown
   Add to your build.gradle:
   dependencies {
     implementation 'com.heymumford:tn5250j-headless:0.12.0'
   }
   ```

3. **Changelog:** `CHANGELOG.md`
   ```markdown
   ## [0.12.0] - 2026-02-12
   ### Added
   - Feature list here
   ```

4. **Git Tag:**
   ```bash
   git tag -a v0.12.0 -m "Release 0.12.0"
   ```

### Breaking Changes (SemVer Pre-1.0 Caveat)

**From CONTRIBUTING.md:**
> While upstream remains `0.y.z`, minor bumps may still introduce breaking changes per SemVer guidance for pre-1.0 releases.

**Implication:**
- `0.12.0` → `0.13.0` may include breaking API changes
- Documented in CHANGELOG.md with migration guide
- At 1.0.0, breaking changes require MAJOR version bump

**Example breaking changes (0.13.0 hypothetical):**
```
### Changed (Breaking)
- Session5250.connect() now throws SessionException instead of IOException
- WorkflowRunner.execute() moved to new WorkflowExecutor class
```

Must include:
```
### Migration Guide
// Old code (0.12.0):
try {
  session.connect(host, port);
} catch (IOException e) { }

// New code (0.13.0+):
try {
  session.connect(host, port);
} catch (SessionException e) { }  // Specific exception
```

---

## Part 5: Branching Strategy

### Main Release Branch Structure

```
main (stable releases only)
├─ v0.12.0 (tag)
├─ v0.13.0 (tag, future)
└─ v1.0.0 (tag, future)

feature/* (feature development)
├─ feature/headless-session-pooling
├─ feature/docker-support
└─ feature/maven-publishing

bugfix/* (critical fixes)
├─ bugfix/ccsid-1122-encoding
└─ bugfix/session-timeout-race

hotfix/* (post-release patches, if needed)
└─ hotfix/0.12.1-security-patch
```

### Feature Branch Workflow (Pre-Release)

```
1. Developer creates feature/my-feature from main
2. Develops + tests locally
3. Opens PR to main with:
   - Feature description
   - Test coverage > 80% new code
   - CHANGELOG.md entry (in Unreleased section)
   - Documentation updates
4. CI runs: ./gradlew test (must pass)
5. Review approval required (1+ maintainers)
6. Merge to main (no squash, preserve history)
7. Delete feature branch
```

**CHANGELOG.md entry (during feature branch):**

```markdown
## [Unreleased]

### Added
- New headless session pooling API (Session5250Pool)
  - Load balancing across N concurrent sessions
  - Automatic recycling of idle connections
  - See docs/SESSION_POOLING.md for usage

### Changed
- Improved WorkflowRunner performance (20% faster)
```

### Release Branch Workflow

**For 0.12.0:**

```bash
# 1. Create release prep PR (if needed for last-minute fixes)
git checkout main
git pull origin main
git checkout -b release/0.12.0

# 2. Make final adjustments
# - Update CHANGELOG.md (move Unreleased → 0.12.0)
# - Bump version in gradle.properties to 0.12.0
# - Update README examples if needed

git add CHANGELOG.md gradle.properties README.md
git commit -m "release: prepare 0.12.0"

# 3. Create PR
# - Title: "release: 0.12.0"
# - Body: List final changes
# - Link to release criteria (this doc)

# 4. After approval, merge to main
git checkout main
git pull origin main
git merge --no-ff release/0.12.0 -m "merge release/0.12.0"

# 5. Tag and push (triggers CI)
git tag -a v0.12.0 -m "Release 0.12.0"
git push origin main
git push origin v0.12.0
```

### Hotfix Branch Workflow (Post-Release)

**Scenario: Critical bug in 0.12.0 released, need 0.12.1**

```bash
# 1. Create hotfix branch from tag
git checkout -b hotfix/0.12.1 v0.12.0

# 2. Fix bug + test
# (e.g., fix critical CCSID encoding issue)
# - Update src/ files
# - Add regression test
# - Update CHANGELOG.md

git add src/ tests/ CHANGELOG.md
git commit -m "fix: critical CCSID encoding regression"

# 3. Merge back to main and develop
git checkout main
git pull origin main
git merge --no-ff hotfix/0.12.1 -m "hotfix: 0.12.1 critical patch"

# 4. Update version and tag
grep "^version=" gradle.properties
# (Should be 0.13.0-SNAPSHOT after 0.12.0 release)
# Update to 0.12.1-SNAPSHOT if needed, then 0.12.1 for tag

git tag -a v0.12.1 -m "Hotfix 0.12.1 - Critical CCSID encoding fix"
git push origin main
git push origin v0.12.1

# 5. Clean up
git branch -D hotfix/0.12.1
```

**Note:** Hotfixes unlikely pre-1.0, but process documented for completeness.

---

## Part 6: Artifact Publishing Checklist

### Release Artifacts to Generate

| Artifact | Format | Location | Publish To | Notes |
|----------|--------|----------|-----------|-------|
| **JAR** | `hti5250j-0.12.0.jar` | GitHub Releases | GitHub, Maven Central | Executable for library consumers |
| **Checksum** | SHA-256 | GitHub Release notes | GitHub | Verify integrity |
| **Source JAR** | `hti5250j-0.12.0-sources.jar` | Build artifact | Maven Central | Maven requirement |
| **Javadoc JAR** | `hti5250j-0.12.0-javadoc.jar` | Build artifact | Maven Central | API documentation |
| **POM** | `hti5250j-0.12.0.pom` | Build artifact | Maven Central | Dependency descriptor |
| **Docker Image** | `heymumford/hti5250j:0.12.0` | Docker Hub | Docker Hub | Container runtime |
| **Release Notes** | Markdown | GitHub Release | GitHub | Changelog + migration guide |
| **Test Report** | HTML | Build artifact (archive) | GitHub Artifacts | For debugging failures |

### Pre-Publishing Checklist

**Before creating release tag, verify:**

```bash
# 1. No uncommitted changes
git status
# Expected: "nothing to commit, working tree clean"

# 2. Main is synced with origin
git log -1 --oneline
# Compare with: git log -1 --oneline origin/main

# 3. Tests passing (if release includes code changes)
./gradlew test --no-daemon 2>&1 | tail -3
# Expected: "X tests completed, 0 failed" or acceptable failure count

# 4. Build succeeds
./gradlew clean build -x test --no-daemon
# Expected: "BUILD SUCCESSFUL"

# 5. JAR exists and is non-empty
ls -lh build/libs/*.jar
# Expected: File size > 1MB (real code), not 0 bytes

# 6. Version in gradle.properties matches release tag
grep "^version=" gradle.properties
# Expected: version=0.12.0 (no -SNAPSHOT)

# 7. CHANGELOG.md has entry for this version
grep "## \[0.12.0\]" CHANGELOG.md
# Expected: Found and includes date

# 8. Git tag does NOT exist yet
git tag | grep v0.12.0
# Expected: (no output = tag doesn't exist, good)
```

### GitHub Release Publishing

**Automated via `.github/workflows/release.yml`, but can manual fallback:**

```bash
# Manual fallback if CI fails:

# 1. Verify artifacts present
cd build/libs/
ls *.jar

# 2. Calculate checksums
sha256sum hti5250j-0.12.0.jar > hti5250j-0.12.0.jar.sha256

# 3. Create release manually
gh release create v0.12.0 \
  --title "Release v0.12.0" \
  --notes-file RELEASE_NOTES.md \
  build/libs/hti5250j-0.12.0.jar \
  build/libs/hti5250j-0.12.0.jar.sha256

# 4. Verify
gh release view v0.12.0
```

### Maven Central Publishing (0.13.0+)

**One-time setup:**

```bash
# 1. Create Sonatype OSSRH account
# https://central.sonatype.org/register/

# 2. Configure credentials in ~/.gradle/gradle.properties
cat >> ~/.gradle/gradle.properties << 'EOF'
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password
EOF

# 3. Setup GPG signing
gpg --full-generate-key
# (Follow prompts, use same email as OSSRH account)

# 4. List key ID
gpg --list-keys --keyid-format SHORT

# 5. Export public key to servers
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

**Publish to staging (pre-1.0, optional):**

```bash
./gradlew publish

# Monitor at: https://s01.oss.sonatype.org/#stagingRepositories
# Click "Close" to validate
# Then "Release" to sync to Maven Central
```

**Maven Central verification (24-48 hours after release):**

```bash
# Check if artifact appears
curl https://repo1.maven.org/maven2/org/hti5250j/hti5250j/0.12.0/hti5250j-0.12.0.jar

# Add to project build.gradle:
dependencies {
  implementation 'org.hti5250j:hti5250j:0.12.0'
}
```

### Docker Hub Publishing (0.13.0+)

**One-time setup:**

```bash
# 1. Create Docker Hub account: https://hub.docker.com/signup

# 2. Create public repository: https://hub.docker.com/repository/create
# - Name: hti5250j
# - Description: Headless 5250 Terminal Emulator

# 3. Create Docker Hub access token
# https://hub.docker.com/settings/security/tokens

# 4. Add to GitHub Secrets
# Settings → Secrets and Variables → Actions → New Repository Secret
# Name: DOCKER_HUB_USERNAME
# Value: your-docker-username

# Name: DOCKER_HUB_TOKEN
# Value: your-access-token
```

**Publish with CI (automated):**

The updated `.github/workflows/release.yml` will:
1. Build Docker image with tag `heymumford/hti5250j:0.12.0` and `latest`
2. Push to Docker Hub

**Manual publish (fallback):**

```bash
# 1. Build image locally
docker build -t hti5250j:0.12.0 .

# 2. Tag for Docker Hub
docker tag hti5250j:0.12.0 heymumford/hti5250j:0.12.0
docker tag hti5250j:0.12.0 heymumford/hti5250j:latest

# 3. Login and push
docker login -u your-username
docker push heymumford/hti5250j:0.12.0
docker push heymumford/hti5250j:latest

# 4. Verify
docker pull heymumford/hti5250j:0.12.0
docker run heymumford/hti5250j:0.12.0 --version
```

---

## Part 7: Release Notes Template

### Template Location & Format

Create release notes in `RELEASE_NOTES_0.12.0.md` (or commit message for tag):

```markdown
# Release v0.12.0

**Release Date:** February 15, 2026
**Status:** Stable
**Upstream:** TN5250J 0.8.x (compatible)

## Summary

HTI5250J 0.12.0 completes the headless architecture extraction and achieves 99.55% test pass rate (13,270+ tests). Key improvements include GuiGraphicBuffer extraction, CCSID encoding fixes, and comprehensive headless interfaces.

**Test Coverage:** 13,270 passing / 13,637 total (0.45% failure rate)
**Breaking Changes:** None (pre-1.0 release)

---

## What's New

### Added
- **GuiGraphicBuffer Extraction** (5 classes, 500+ LOC)
  - Isolated GUI buffer responsibility from session management
  - Enables headless rendering and testing
  - See `doc/HEADLESS_ARCHITECTURE.md`

- **Headless Interfaces** (8 interfaces)
  - `HeadlessSession`, `HeadlessScreen`, `HeadlessKeyboard`
  - Enable non-GUI testing and automation
  - Documented in `src/main/org/tn5250j/headless/interfaces/`

- **67 New Unit Tests**
  - `SessionsHeadlessTest` (14 tests)
  - `KeyMapperHeadlessTest` (12 tests)
  - `ColorPaletteHeadlessTest` (8 tests)
  - Full coverage of headless components

### Fixed
- **Character Encoding** (6 CCSID codepages)
  - CCSID-37 (German): Fixed unmappable character handling
  - CCSID-273, 280, 284, 297, 500: Proper CharacterConversionException
  - 29 tests fixed in CharsetConversionPairwiseTest

- **Test Assertions** (CharsetConversionPairwiseTest)
  - Updated to expect correct exception types
  - Eliminated false-positive pass/fail scenarios

- **Build System** (3 P0 compilation errors)
  - Resolved type mismatches in CCSID converter initialization
  - Updated test import paths for extracted GUI classes

### Changed
- **Test Pass Rate:** 99.34% → 99.55% (+0.21 percentage points)
- **Exception Handling:** CCSID converters now throw CharacterConversionException (not return '?')
- **API Contracts:** Session5250 methods now explicitly typed for null handling

### Deprecated
- `My5250Frame` and GUI container (removed in 0.13.0)
- Swing/AWT direct dependencies (headless abstractions provided)

### Removed
- Unlicensed JavaPro magazine references (6 files)
- Dead GUI keyboard configuration code (guarded by getGUI() != null checks)

---

## Upgrade Path

### For 0.11.x Users
No breaking changes. Update dependency:

**build.gradle (old):**
```gradle
dependencies {
  implementation 'org.hti5250j:hti5250j:0.11.0'
}
```

**build.gradle (new):**
```gradle
dependencies {
  implementation 'org.hti5250j:hti5250j:0.12.0'
}
```

Run tests to verify encoding fixes don't affect your application.

### For Upstream TN5250J Users
Headless fork is API-compatible for session management:

```java
// Works with HTI5250J 0.12.0
Session5250 session = new Session5250("ibmi.example.com", 23);
session.connect();
session.sendString("WRKSYSVAL");
String screen = session.getScreenText();
session.disconnect();
```

See `MIGRATION_GUIDE_UPSTREAM.md` for full details.

---

## Known Limitations

- GUI components (Swing/AWT) removed; use headless interfaces instead
- Jython scripting support removed (use YAML workflows)
- Spoolfile viewer removed (extract via Session5250 API)
- Desktop application (`My5250`) removed (use CLI or library)

## Performance Notes

- Test execution: 13,270 tests in ~45 seconds (parallel, 4 forks)
- JAR size: ~2.5 MB (includes runtime dependencies)
- Startup time: <1 second (JVM + class loading)
- Memory footprint: ~200 MB per session (including JVM baseline)

---

## Acknowledgments

- **TN5250J Community:** Original 5250 terminal emulator
- **IBM i Community:** Invaluable feedback on headless execution
- **Test Contributors:** Comprehensive test suite (13,600+ tests)

---

## Links

- **GitHub Release:** https://github.com/heymumford/hti5250j/releases/tag/v0.12.0
- **Issue Tracker:** https://github.com/heymumford/hti5250j/issues
- **Documentation:** https://github.com/heymumford/hti5250j/blob/main/ARCHITECTURE.md
- **CHANGELOG:** https://github.com/heymumford/hti5250j/blob/main/CHANGELOG.md
- **Contributing:** https://github.com/heymumford/hti5250j/blob/main/CONTRIBUTING.md

---

## Security & Support

**Security:** Report vulnerabilities privately via GitHub Security Advisories
**Support:** Active on GitHub Issues and Discussions
**Next Release:** v0.13.0 (estimated Q1 2026)
```

### Release Notes for Different Channels

**GitHub Release:** Use full template above
**Maven Central:** Use abbreviated version (10-15 lines)
**Docker Hub:** Use single-line summary + link to GitHub Release

---

## Part 8: Rollback Plan

### Scenarios Requiring Rollback

| Scenario | Risk Level | Action | Timeline |
|----------|-----------|--------|----------|
| Build artifact corrupted (0 bytes) | CRITICAL | Delete tag, rebuild | 5 mins |
| Test failures discovered post-release | HIGH | Document as known issue, hotfix 0.12.1 | 1-2 days |
| Security vulnerability found | CRITICAL | Yank from Maven Central, publish 0.12.1-sec | 24 hours |
| Breaking API change breaks consumers | HIGH | Revert tag to previous version | 4-24 hours |
| Upstream license incompatibility discovered | CRITICAL | Remove release, audit all deps | Immediate |

### Rollback Execution

**Scenario 1: Delete Tag & Rebuild (Corrupted Artifact)**

```bash
# 1. Delete tag locally and remotely
git tag -d v0.12.0
git push origin :refs/tags/v0.12.0

# 2. Verify tag deleted
git tag | grep v0.12.0
# (should be empty)

# 3. Delete GitHub Release (manual via web UI)
# https://github.com/heymumford/hti5250j/releases
# Click v0.12.0 → Delete

# 4. Fix issue (e.g., rebuild with correct version)
./gradlew clean build -x test --no-daemon

# 5. Re-create tag and push
git tag -a v0.12.0 -m "Release v0.12.0 (rebuilt)"
git push origin v0.12.0

# 6. Verify in GitHub Actions that release workflow re-runs
```

**Timeline:** 5-10 minutes

**Scenario 2: Test Failures Discovered (Post-Release)**

```bash
# 1. Document issue in GitHub Issues
# Title: "v0.12.0: [Test Name] regression found in production"
# Include: error message, stack trace, reproduction steps

# 2. If non-critical: Update CHANGELOG.md
#    Add: "Known Issues" section for v0.12.0

CHANGELOG.md entry:
### Known Issues
- **[GH-###] Test failure in CCSID1122MigrationTest**
  - Affects: Rare CCSID conversions (edge case)
  - Workaround: Use CCSID-37 instead
  - Fix: Planned for v0.12.1

# 3. If critical: Create hotfix branch
git checkout -b hotfix/0.12.1 v0.12.0
# ... fix issue ...
git tag -a v0.12.1 -m "Hotfix: Critical test regression"
git push origin v0.12.1

# 4. Communicate to users
# GitHub Discussion: "v0.12.0 - Known Issue: [description]"
# Include workaround if available
```

**Timeline:** 4-24 hours for non-critical, immediate for critical

**Scenario 3: Security Vulnerability (Post-Release)**

```bash
# 1. Create private security advisory (GitHub)
# https://github.com/heymumford/hti5250j/security/advisories/new
# Severity: High/Critical
# Affected versions: 0.12.0

# 2. Create hotfix immediately
git checkout -b hotfix/0.12.1-security v0.12.0
# ... fix security issue ...
# Update CHANGELOG.md with [SECURITY] tag

# 3. Release v0.12.1 as "security hotfix"
git tag -a v0.12.1 -m "Security hotfix: [CVE or vulnerability description]"
git push origin v0.12.1

# 4. Publish security advisory (after 0.12.1 released)
# GitHub will guide advisory creation

# 5. Request Maven Central yank (if published)
# Contact: support@sonatype.com
# Request: Yank v0.12.0 from Central, recommend 0.12.1

# 6. Notify users
# GitHub Security Advisory page
# Post in Discussions
# Email important downstream users (if database available)
```

**Timeline:** Immediate (6-12 hours for fix, 24 hours for propagation)

**Scenario 4: Breaking API Change Breaks Consumers**

```bash
# 1. If mistake discovered before widespread adoption:
#    Create v0.12.1 with reverted breaking change

git checkout -b revert/0.12.1 v0.12.0
git revert <commit-hash>  # Revert breaking change
git tag -a v0.12.1 -m "Revert breaking change, restore 0.11.x compatibility"

# 2. If mistake discovered after widespread adoption:
#    Document migration path in v0.13.0

# CHANGELOG entry (0.13.0):
### Migration Guide
If you're upgrading from 0.11.x to 0.13.0, note the following breaking changes
in 0.12.0 (which are preserved in 0.13.0):

- Session5250.connect() now throws SessionException (was IOException)
  Before: try { session.connect(); } catch (IOException e) { }
  After:  try { session.connect(); } catch (SessionException e) { }
```

**Timeline:** 1-2 days if reverted, 1-3 months if documented in next release

### Rollback Communication

**Always communicate rollbacks publicly:**

```markdown
# GitHub Discussion: "Security Rollback: v0.12.0 yanked"

## Summary
We've discovered a security vulnerability in v0.12.0. Affected users should:
1. Upgrade to v0.12.1 immediately
2. Audit logs for suspicious activity (if applicable)

## Impact
- Vulnerability: [Description]
- Severity: Critical
- Affected versions: 0.12.0
- Fixed in: 0.12.1

## Remediation
```bash
# Update your build.gradle
dependencies {
  implementation 'org.hti5250j:hti5250j:0.12.1'  // was 0.12.0
}
```

## Resources
- Security Advisory: [link]
- GitHub Release: v0.12.1
- Questions: GitHub Discussions

Thank you for your patience!
```

---

## Appendix: Checklists

### Pre-Release Checklist (1 week before)

- [ ] Read RELEASE_MANAGEMENT.md (this file)
- [ ] Check test pass rate: `./gradlew test | tail -5`
- [ ] Review failing tests: Document why each test fails (if expected)
- [ ] Run security scans: CodeQL, Semgrep passing
- [ ] Update CHANGELOG.md with final 0.12.0 section
- [ ] Update gradle.properties version (remove -SNAPSHOT)
- [ ] Review CONTRIBUTING.md for any policy changes
- [ ] Verify README.md examples work (manual smoke test)
- [ ] Create release branch if needed (release/0.12.0)
- [ ] Assign PR reviewer (maintainer + 1)
- [ ] Get approval on release PR

### Code Freeze Checklist (day of release)

- [ ] Main branch is clean (no uncommitted changes)
- [ ] No open PRs for this release
- [ ] Create annotated git tag: `git tag -a v0.12.0 -m "..."`
- [ ] Push tag: `git push origin v0.12.0`
- [ ] Verify GitHub Actions `Release` workflow triggered
- [ ] Monitor build job (5-10 mins)
- [ ] Verify JAR artifact generated
- [ ] Check GitHub Release created automatically

### Post-Release Checklist (day after)

- [ ] Download artifact from GitHub Release
- [ ] Verify SHA-256 checksum
- [ ] Test JAR as library (maven install)
- [ ] Create GitHub Discussion post announcing release
- [ ] Update README.md if version-dependent
- [ ] Close GitHub milestone (0.12.0)
- [ ] Create new milestone for 0.13.0
- [ ] Bump version in gradle.properties to 0.13.0-SNAPSHOT
- [ ] Commit version bump: `git commit -am "build: bump to 0.13.0-SNAPSHOT"`
- [ ] Push to main

### Maven Central Setup Checklist (0.13.0 only)

- [ ] Create Sonatype OSSRH account
- [ ] Verify JIRA ticket approved for namespace
- [ ] Setup GPG signing
- [ ] Configure gradle.properties (ossrhUsername, ossrhPassword)
- [ ] Add maven-publish plugin to build.gradle
- [ ] Add signing plugin + configuration
- [ ] Test publish to staging: `./gradlew publish`
- [ ] Verify artifact in Sonatype staging
- [ ] Close and release staging repository
- [ ] Verify artifact synced to Maven Central (24-48 hours)

### Docker Hub Setup Checklist (0.13.0 only)

- [ ] Create Docker Hub account
- [ ] Create public repository (hti5250j)
- [ ] Create access token
- [ ] Add DOCKER_HUB_USERNAME, DOCKER_HUB_TOKEN to GitHub Secrets
- [ ] Create Dockerfile at repo root
- [ ] Add Docker build job to .github/workflows/release.yml
- [ ] Test Docker build locally: `docker build -t hti5250j:0.12.0 .`
- [ ] Tag and push manually (if CI not ready)
- [ ] Verify image pulls: `docker pull heymumford/hti5250j:0.12.0`
- [ ] Update README.md with Docker usage example

---

## Summary

This release management strategy positions HTI5250J for professional distribution:

1. **Current State:** 0.12.0 is buildable but 61 tests failing → NO release yet
2. **Blocker:** Fix test failures to ≥99.5% pass rate
3. **Process:** Clear stages from pre-release through validation
4. **Artifacts:** GitHub Releases ready, Maven Central + Docker Hub deferred to 0.13.0
5. **Rollback:** Documented for all failure scenarios

**Next Steps:**
1. Fix failing tests (priority: CCSID1122, WizardEventRecord)
2. Merge `refactor/cleanup-cruft-and-docs` to main
3. Execute release process per Part 3
4. Plan 0.13.0 features for Q1 2026
5. Target 1.0.0 stable release for Q3 2026

