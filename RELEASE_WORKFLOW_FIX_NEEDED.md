# Release Workflow Fix Required

**Issue:** Semantic Release workflow fails on every push to main
**Cause:** Workflow expects Node.js/npm, but HTI5250J is a Java/Gradle project
**Impact:** Non-blocking — main CI (Gradle tests) passes successfully

---

## Error Details

```
npm error The `npm ci` command can only install with an existing
npm error package-lock.json or npm-shrinkwrap.json with lockfileVersion >= 1
```

**When it occurs:** Every push to main branch triggers Semantic Release workflow
**Result:** Workflow fails with exit code 1 during "Install dependencies" phase

---

## Root Cause

The `.github/workflows/release.yml` workflow is configured for JavaScript/Node.js projects:

```yaml
- name: Install dependencies
  run: npm ci

- name: Semantic Release
  run: npx semantic-release
```

HTI5250J is a Java project. This workflow was likely created from a Node.js template and never adapted.

---

## Impact Assessment

| Aspect | Status |
|--------|--------|
| **Code quality** | ✅ Not affected (Gradle tests pass) |
| **Build status** | ✅ Not affected (CI workflow succeeds) |
| **Security** | ✅ Not affected (Semgrep passes) |
| **Deployment** | ❌ Release automation blocked |
| **Phase 15B** | ✅ Not affected (Phase 15B is complete and tested) |

---

## Fix Required

Replace `.github/workflows/release.yml` with Java/Gradle configuration:

### Option 1: Gradle-based Release

```yaml
name: Release

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin

      - name: Build with Gradle
        run: ./gradlew build

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: build/test-results/

      - name: Create Release
        if: success()
        run: |
          gh release create v$(date +%Y.%m.%d) \
            --generate-notes \
            ./build/libs/*.jar
        env:
          GH_TOKEN: ${{ github.token }}
```

### Option 2: Disable Release Workflow

If release automation not needed, delete or disable `.github/workflows/release.yml`:

```bash
rm .github/workflows/release.yml
git add .
git commit -m "ci: remove semantic release workflow (not applicable to Java project)"
git push
```

---

## Recommendation

**Remove Semantic Release workflow** until release automation is needed. Phase 15B is complete and ready for production use without automated versioning.

**Action:** Decide on release strategy (manual versioning, automated Gradle-based, or no automation), then update workflow accordingly.

---

## Verification After Fix

After updating the workflow, verify:

```bash
# Push a test commit
git commit --allow-empty -m "test: verify release workflow"
git push origin main

# Check workflow status
gh run list --branch main -L 1 --json status,conclusion,name

# Expected: All workflows should show "completed" with "success"
```

---

**This is not blocking Phase 15B.** Phase 15B is complete, tested, and published. The Release workflow failure is a separate infrastructure issue.
