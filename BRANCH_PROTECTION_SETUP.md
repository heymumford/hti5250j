# Branch Protection Setup Guide

## Overview

Branch protection rules enforce quality standards on all code merged to `main`. This document provides step-by-step instructions for enabling the testing framework's automated gates.

---

## Required Status Checks (CI/CD Gates)

The following GitHub Actions jobs **must pass** before merging to main:

### Test Category Jobs (6 parallel)
- ✅ `test (encoding)` - Encoding/codec layer tests
- ✅ `test (framework-tn5250)` - TN5250 protocol layer tests
- ✅ `test (workflow)` - Workflow and scenario tests
- ✅ `test (transport-security)` - Transport and security tests
- ✅ `test (framework-core)` - Core framework tests
- ✅ `test (gui-tools)` - GUI and tools tests

### Quality Jobs
- ✅ `performance` - Performance SLA enforcement (JMH + k6)
- ✅ `security` - Security scanning (Semgrep)

---

## Setup Instructions

### Via GitHub Web Interface

1. **Navigate to Settings**:
   - Go to: `https://github.com/heymumford/hti5250j/settings/branches`
   - Or: Settings → Branches → Add rule

2. **Create Branch Protection Rule**:
   - Branch pattern: `main`
   - Click "Create"

3. **Enable Required Status Checks**:
   ✓ Check: "Require status checks to pass before merging"
   ✓ Check: "Require branches to be up to date before merging"

   **Add status checks** (search and select each):
   ```
   test (encoding)
   test (framework-tn5250)
   test (workflow)
   test (transport-security)
   test (framework-core)
   test (gui-tools)
   performance
   security
   ```

4. **Enable Pull Request Reviews**:
   ✓ Check: "Require pull request reviews before merging"
   - Required approving reviews: `1`
   ✓ Check: "Dismiss stale pull request approvals when new commits are pushed"

5. **Enforce Rules for Admins**:
   ✓ Check: "Include administrators"

6. **Restrict Force Pushes & Deletions**:
   ✓ Check: "Restrict who can push to matching branches"
   ✓ Check: "Allow force pushes" → Select "No one"
   ✓ Check: "Allow deletions" → Select "No one"

7. **Save**:
   - Click "Save changes"

---

## Via GitHub CLI

### One-Line Setup

```bash
gh api \
  -X PUT \
  /repos/heymumford/hti5250j/branches/main/protection \
  -f "required_status_checks={\"strict\":true,\"contexts\":[\"test (encoding)\",\"test (framework-tn5250)\",\"test (workflow)\",\"test (transport-security)\",\"test (framework-core)\",\"test (gui-tools)\",\"performance\",\"security\"]}" \
  -f "required_pull_request_reviews={\"required_approving_review_count\":1,\"dismiss_stale_reviews\":true}" \
  -f "enforce_admins=true" \
  -f "allow_force_pushes=false" \
  -f "allow_deletions=false" \
  -f "restrictions=null"
```

### Verify Configuration

```bash
gh api repos/heymumford/hti5250j/branches/main/protection
```

---

## How Branch Protection Works

### PR Creation & Testing

```
1. Developer creates PR against main
   ↓
2. GitHub Actions runs all jobs (6 test + 2 quality)
   ↓
3. All status checks must pass:
   ├─ All 6 test category jobs pass ✓
   ├─ Performance SLA gates pass ✓
   └─ Security scanning passes ✓
   ↓
4. PR requires 1 approval ✓
   ↓
5. Merge to main allowed (branch protection satisfied)
```

### Merge Blocked If

- ❌ Any test category fails
- ❌ Coverage drops below targets
- ❌ Performance regresses >10%
- ❌ Security issues found
- ❌ No approvals

---

## Testing Before Merge

### Local Pre-Submission Check

Run this locally **before creating/updating PR**:

```bash
# Run all quality gates
./gradlew qualityGate

# View coverage report
open build/reports/jacoco/test/html/index.html

# If failures, check runbooks in TESTING.md
```

### CI Pipeline (Automated)

Once pushed, GitHub Actions runs automatically:
- 6 test jobs in parallel (~2 minutes total)
- Performance validation
- Security scanning
- Results visible in PR checks section

---

## Troubleshooting

### Status Check Shows "Cannot be Created"

**Cause**: Job name doesn't match exactly (case-sensitive)

**Fix**:
1. Run CI workflow successfully once
2. Go to PR and check exact job names
3. Add status checks with exact names from CI output

### "Required status check missing"

**Cause**: Added checks before CI workflow created them

**Fix**:
1. Run complete CI pipeline (push code)
2. Let GitHub Actions jobs complete
3. Add status checks from completed jobs

### Temporarily Bypass Protection (Admin Only)

Not recommended, but possible if needed:
- Settings → Branches → Include administrators (uncheck)
- Make changes
- Re-enable protection

---

## Maintenance

### Weekly Review

```bash
# Check protection status
gh api repos/heymumford/hti5250j/branches/main/protection

# View recent CI results
gh run list --repo heymumford/hti5250j --limit 10
```

### Update Status Checks

If adding new CI jobs:

1. Run workflow successfully
2. Go to branch protection settings
3. Search for new job name
4. Add to "Required status checks"
5. Save

---

## Appendix: Status Check Reference

| Check | Tool | Failure Condition |
|-------|------|-------------------|
| test (encoding) | JUnit 5 + JaCoCo | <90% branch coverage OR test failure |
| test (framework-tn5250) | JUnit 5 + JaCoCo | <80% branch coverage OR test failure |
| test (workflow) | JUnit 5 + JaCoCo | Test failure OR timeout |
| test (transport-security) | JUnit 5 + JaCoCo | <95% branch coverage OR test failure |
| test (framework-core) | JUnit 5 + JaCoCo | Test failure |
| test (gui-tools) | JUnit 5 + JaCoCo | Test failure |
| performance | JMH + k6 | >10% latency regression OR SLA violation |
| security | Semgrep | Critical/High severity issues found |

---

## Status: ✅ ENABLED

Branch protection rules are currently active on the `main` branch as of 2026-02-13.

Verification:
```bash
gh api repos/heymumford/hti5250j/branches/main/protection
```

---

**Last Updated**: 2026-02-13
**Version**: 1.0.0
**Status**: ✅ ENABLED AND VERIFIED
**For Questions**: See TESTING.md for complete testing strategy
