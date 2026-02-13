# HTI5250J Open Source Strategy - Implementation Checklist

**Timeline:** 6 months (February - August 2026)
**Owner:** Eric C. Mumford (@heymumford)
**Status:** Ready to implement
**Last updated:** February 2026

---

## PHASE 1: Foundation (Weeks 1-4)
**Goal:** Set up Maven Central publishing + community infrastructure
**Effort:** 15 hours

### Weeks 1-2: Maven Central Account Setup
- [ ] Register Sonatype JIRA account (https://issues.sonatype.org)
  - [ ] Link GitHub account
  - [ ] Verify email
- [ ] Request namespace `org.hti5250j`
  - [ ] Provide GitHub repo link
  - [ ] Provide project description
  - [ ] Wait for approval (typically 1-2 days)
- [ ] Generate GPG key pair (for signing artifacts)
  ```bash
  gpg --full-generate-key  # RSA, 4096-bit, no expiry
  gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
  ```
- [ ] Store GPG passphrase securely
- [ ] Document fingerprint: ________________

**Artifacts:**
- Sonatype account login credentials (store in 1Password/LastPass)
- GPG key fingerprint
- Passphrase (encrypted, separate location)

---

### Weeks 2-3: Gradle Configuration
- [ ] Update `build.gradle` with publishing config
  - [ ] Add `maven-publish` and `signing` plugins
  - [ ] Create `publishing { publications { } }` block
  - [ ] Add pom with license, developers, scm info
  - [ ] Add credentials (from env vars: `OSSRH_USERNAME`, `OSSRH_PASSWORD`)

  **Reference:** Section 1.1 of OPEN_SOURCE_STRATEGY.md

- [ ] Create `.github/secrets` (maintainer-only):
  - [ ] `OSSRH_USERNAME` = Sonatype JIRA username
  - [ ] `OSSRH_PASSWORD` = Sonatype token (not password!)
  - [ ] `MAVEN_GPG_KEY_ID` = GPG key ID (last 8 chars)
  - [ ] `MAVEN_GPG_PASSWORD` = GPG passphrase
  - [ ] `MAVEN_GPG_PRIVATE_KEY` = base64-encoded private key

  **How to export:**
  ```bash
  # Export private key (base64)
  gpg --armor --export-secret-key <KEY_ID> | base64 -w 0
  ```

- [ ] Create `.github/workflows/publish-maven-central.yml`
  - [ ] Copy template from OPEN_SOURCE_STRATEGY.md section 1.1
  - [ ] Test workflow syntax (validate in GitHub UI)

---

### Weeks 3-4: Community Infrastructure
- [ ] Create `docs/LICENSE_FAQ.md`
  - [ ] Answer "Do I need to open-source my code?" (NO for library use)
  - [ ] Link to official GPL-2.0 text
  - [ ] Provide contribution guidance

- [ ] Create GitHub Discussions categories
  - [ ] `Getting Started` (quick start, setup help)
  - [ ] `Workflows` (YAML automation examples)
  - [ ] `Protocol` (TN5250E technical questions)
  - [ ] `Integration` (Spring Boot, Docker, etc.)
  - [ ] `Announcements` (maintainer updates)
  - [ ] `Governance` (RFC, decision-making)

- [ ] Create `CODE_OF_CONDUCT.md`
  - [ ] Adopt Contributor Covenant 2.1
  - [ ] Link reporting mechanism

- [ ] Update `CONTRIBUTING.md`
  - [ ] Add "First time contributing?" section
  - [ ] Link to GOVERNANCE.md
  - [ ] Add "good-first-issue" filter link

---

### Weeks 4: Version Bump & Release Planning
- [ ] Update `gradle.properties` to v0.13.0
- [ ] Update `CHANGELOG.md` for v0.13.0
  - [ ] List features added since v0.12.0
  - [ ] List bugs fixed
  - [ ] Note backward-compatible API additions
- [ ] Create GitHub Milestone "v0.13.0"

**Deliverables ready:**
✓ Maven Central account + namespace
✓ Gradle publishing configuration
✓ GitHub Actions workflow (ready to publish)
✓ Community infrastructure (Discussions, CoC)
✓ v0.13.0 changelog

---

## PHASE 2: Maven Central Release (Weeks 5-8)
**Goal:** Publish v0.13.0 to Maven Central + verify
**Effort:** 8 hours

### Week 5: Pre-Release Testing
- [ ] Run full test suite locally
  ```bash
  ./gradlew clean test --info
  ```
  - [ ] Verify 99%+ pass rate
  - [ ] No security warnings (Semgrep, CodeQL)

- [ ] Build artifacts locally (test signing)
  ```bash
  ./gradlew build -x test
  ```
  - [ ] Verify JAR created: `build/libs/hti5250j-0.13.0.jar`
  - [ ] Check MANIFEST.MF has correct version

- [ ] Test Sonatype staging (dry run)
  ```bash
  # Use staging URL to test without publishing to prod
  ./gradlew publish \
    -PossrhUsername=<username> \
    -PossrhPassword=<token> \
    -Psigning.keyId=<KEY_ID> \
    -Psigning.password=<PASSPHRASE>
  ```

---

### Week 6: Create GitHub Release + Tag
- [ ] Commit version bump & changelog
  ```bash
  git add gradle.properties CHANGELOG.md
  git commit -m "Release v0.13.0"
  ```

- [ ] Create annotated tag
  ```bash
  git tag -a v0.13.0 -m "Release v0.13.0: [brief feature list]"
  ```

- [ ] Push commits + tags
  ```bash
  git push origin main
  git push origin v0.13.0
  ```
  - [ ] GitHub Actions triggered automatically
  - [ ] Watch build logs in Actions tab

---

### Week 7: Verify Maven Central Publication
- [ ] Wait 10-15 minutes for Sonatype sync
- [ ] Verify artifact on Sonatype:
  ```bash
  curl -I https://oss.sonatype.org/content/repositories/releases/org/hti5250j/hti5250j/0.13.0/hti5250j-0.13.0.jar
  # Should return HTTP 200
  ```

- [ ] Check Maven Central mirrors (allow 2-4 hours for sync)
  - [ ] Search mvnrepository.com for "hti5250j"
  - [ ] Verify coordinates: `org.hti5250j:hti5250j:0.13.0`
  - [ ] Verify POM displays correctly

- [ ] Test dependency resolution
  ```gradle
  // In consumer project build.gradle
  repositories {
      mavenCentral()
  }
  dependencies {
      implementation 'org.hti5250j:hti5250j:0.13.0'
  }
  ```
  - [ ] Run `./gradlew dependencies` and verify download

---

### Week 8: Announcement
- [ ] Create GitHub Release (auto-generated or manual)
  ```
  Title: Release v0.13.0
  Description: [Copy from CHANGELOG.md]
  Assets: [gradle-build artifacts from Actions]
  ```

- [ ] Post announcement in GitHub Discussions
  - [ ] `#announcements` category
  - [ ] Highlight key features
  - [ ] Link to Maven Central
  - [ ] Thank contributors

- [ ] Announce on social media
  - [ ] Twitter: "Excited to announce v0.13.0 of HTI5250J, now on Maven Central! ..."
  - [ ] r/java: "HTI5250J v0.13.0 released (headless 5250 terminal emulator)"
  - [ ] r/ibm: Similar announcement

- [ ] Submit to Java Weekly
  - [ ] https://javaweekly.net/
  - [ ] Subject: "HTI5250J v0.13.0 Released with Maven Central Support"

**Deliverables ready:**
✓ v0.13.0 published to Maven Central
✓ Artifact discoverable and resolvable
✓ Community announcements made
✓ First distribution milestone achieved

---

## PHASE 3: Docker & Container Distribution (Weeks 9-12)
**Goal:** Docker images published to Docker Hub + GitHub Container Registry
**Effort:** 20 hours

### Week 9: Docker Setup
- [ ] Create `Dockerfile`
  - [ ] Multi-stage build (builder + runtime)
  - [ ] BASE: eclipse-temurin:21-jre-alpine
  - [ ] COPY: JAR from build/ directory
  - [ ] HEALTHCHECK: Basic TCP check on port 23
  - [ ] ENTRYPOINT: Default to session pool server
  - [ ] Reference: Section 6.2 of OPEN_SOURCE_STRATEGY.md

- [ ] Create `.dockerignore`
  ```
  .git
  .github
  build/
  .gradle
  tests/
  *.md
  ```

- [ ] Create `docker-compose.yml` (example)
  ```yaml
  version: '3.9'
  services:
    hti5250j:
      image: heymumford/hti5250j:latest
      ports:
        - "23:23"  # Telnet
        - "8080:8080"  # (future) API port
      environment:
        IBMI_HOST: ibmi.example.com
        IBMI_PORT: 23
      healthcheck:
        test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
        interval: 30s
        timeout: 5s
  ```

---

### Week 10: GitHub Actions Docker Workflow
- [ ] Create `.github/workflows/publish-docker.yml`
  - [ ] Reference: Section 1.3 of OPEN_SOURCE_STRATEGY.md
  - [ ] Trigger: On version tags (v*)
  - [ ] Login to Docker Hub
  - [ ] Login to GHCR (GitHub Container Registry)
  - [ ] Build + push with metadata actions
  - [ ] Tag strategy:
    - [ ] `heymumford/hti5250j:0.13.0` (semver)
    - [ ] `heymumford/hti5250j:latest` (rolling)
    - [ ] `heymumford/hti5250j:0.13` (minor)
    - [ ] `heymumford/hti5250j:0` (major)

- [ ] Set up GitHub Actions secrets
  - [ ] `DOCKERHUB_USERNAME` = your Docker Hub username
  - [ ] `DOCKERHUB_TOKEN` = Docker Hub personal access token (PAT)
  - [ ] `GITHUB_TOKEN` = auto-available (GitHub provides)

---

### Week 11: Docker Hub Account & Testing
- [ ] Create Docker Hub account (if needed)
  - [ ] Verify email
  - [ ] Create personal access token (PAT)
    - [ ] Scope: Read, Write

- [ ] Create Docker Hub repository
  - [ ] Name: `hti5250j`
  - [ ] Visibility: Public
  - [ ] Description: "Headless 5250 terminal emulator for IBM i"
  - [ ] Link GitHub repo for automatic builds (optional)

- [ ] Test Docker build locally
  ```bash
  docker build -t heymumford/hti5250j:test .
  docker run --rm heymumford/hti5250j:test --help
  ```
  - [ ] Verify image starts without errors
  - [ ] Verify help output works
  - [ ] Check image size (should be <200MB)

- [ ] Test pulling from Docker Hub
  ```bash
  docker pull heymumford/hti5250j:0.13.0
  docker run --rm heymumford/hti5250j:0.13.0 --version
  ```

---

### Week 12: Documentation & Examples
- [ ] Create `examples/docker-compose/`
  - [ ] `docker-compose.yml` (basic example)
  - [ ] `README.md` with instructions
  - [ ] Environment setup (.env.example)

- [ ] Create `examples/ci-cd/`
  - [ ] `github-actions.yml` example (using Docker image)
  - [ ] `gitlab-ci.yml` example
  - [ ] `jenkins-pipeline.groovy` example

- [ ] Create `docs/DOCKER.md`
  - [ ] How to pull and run image
  - [ ] Environment variables
  - [ ] Port mapping (23 = Telnet, 8080+ = future APIs)
  - [ ] Health checks
  - [ ] Volume mounts (config, logs)

- [ ] Update `README.md`
  - [ ] Add Docker installation option
  - [ ] Link to `docs/DOCKER.md`

**Deliverables ready:**
✓ Docker images published to Docker Hub + GHCR
✓ Multiple tags available (version, latest, major, minor)
✓ Container examples (docker-compose)
✓ CI/CD integration examples
✓ Documentation complete

---

## PHASE 4: Content & Community Growth (Weeks 13-20)
**Goal:** Establish thought leadership, recruit contributors, build community
**Effort:** 25 hours

### Week 13-14: Blog Post #1
- [ ] Write: "Why Headless? 5250 Terminal Emulation Without the GUI"
  - [ ] Outline (30 min):
    - [ ] Problem: IBM i terminal automation is manual
    - [ ] Solution: Headless library
    - [ ] Use cases: Testing, pooling, integration
    - [ ] Code examples
  - [ ] Draft (2 hours)
  - [ ] Self-review + edit (1 hour)
  - [ ] Publish to Medium + personal blog (30 min)

- [ ] Share on social media
  - [ ] Twitter thread (3-5 tweets with thread)
  - [ ] LinkedIn post
  - [ ] r/java comment (if relevant discussion)

---

### Week 15-16: Blog Post #2 + Examples
- [ ] Write: "Automating IBM i Regression Tests with YAML Workflows"
  - [ ] Outline: Problem → Solution → Live demo
  - [ ] Code example: payment processing workflow
  - [ ] Results: Test execution log + artifacts

- [ ] Create `examples/spring-boot-integration/`
  - [ ] Spring Boot Starter example
  - [ ] pom.xml dependency declaration
  - [ ] Simple test class
  - [ ] README with instructions

- [ ] Create `examples/kubernetes/`
  - [ ] StatefulSet manifest (session pool)
  - [ ] ConfigMap (session configuration)
  - [ ] Service (expose 23/TCP)
  - [ ] README with k8s setup

---

### Week 17-18: Contributor Recruitment
- [ ] Identify "good-first-issue" candidates
  - [ ] Tag 10-15 issues with `good-first-issue`
  - [ ] Write clear acceptance criteria for each
  - [ ] Examples:
    - [ ] "Add SPDX headers to 5 test files"
    - [ ] "Write JavaDoc for 3 public methods"
    - [ ] "Add YAML workflow example for inventory inquiry"

- [ ] Reach out to potential contributors
  - [ ] Research 5 IBM i experts (GitHub, COMMON, forums)
  - [ ] Send personalized LinkedIn/email message
  - [ ] Suggest specific issue based on their expertise
  - [ ] Offer 30-min pair programming session

- [ ] Create first-contributor guide
  - [ ] Create `docs/CONTRIBUTOR_ONBOARDING.md`
  - [ ] Step-by-step: Clone → Setup → Test → First PR
  - [ ] Troubleshooting section
  - [ ] FAQ

---

### Week 19-20: Conference Proposals + Newsletter
- [ ] Submit proposals to COMMON (if CFP open)
  - [ ] Topic: "Automating 5250 in the Cloud Era"
  - [ ] Format: 90-min session or 30-min talk
  - [ ] Abstract: 300 words (use template from OPEN_SOURCE_STRATEGY.md)
  - [ ] Target: April 2026 (spring) or October 2026 (fall)

- [ ] Submit to JavaOne/Oracle Code One (if applicable)
  - [ ] Topic: "Headless Terminal Emulation for Enterprise Integration"
  - [ ] Track: Enterprise Java or Cloud

- [ ] Create low-traffic mailing list (optional)
  - [ ] Mailchimp free tier (up to 500 subscribers)
  - [ ] Monthly feature highlight + quarterly updates
  - [ ] Signup link in README + Discussions

- [ ] Create year-1 recap blog post
  - [ ] v0.12.0 → v0.13.0 → (future v1.0.0?)
  - [ ] Metrics: Downloads, GitHub stars, contributors
  - [ ] Roadmap for 2026 H2 + 2027

**Deliverables ready:**
✓ 2-3 blog posts published
✓ 4-5 code examples (Spring Boot, Kubernetes, CI/CD)
✓ First 5-10 contributors recruited + active
✓ Conference proposals submitted
✓ Community engagement metrics: 50+ Discussions posts, 5+ PRs merged

---

## PHASE 5: GraalVM Native Image (Weeks 21+, DEFERRED to v1.0.0)
**Goal:** Support lightweight deployments and fast CLI
**Effort:** 30 hours (post-v1.0.0)
**Status:** DEFERRED - start after v1.0.0 (Q4 2026)

- [ ] Add GraalVM buildtools plugin to build.gradle
- [ ] Configure native-image settings
- [ ] Test native binary compilation
- [ ] Create macOS Homebrew formula
- [ ] Publish binary releases (GitHub Releases)
- [ ] Document installation: `brew install hti5250j`

---

## PHASE 6: Parallel Ongoing (Throughout All Phases)

### Code Quality Maintenance (Every 2 weeks)
- [ ] Monitor CodeQL results (GitHub Security tab)
  - [ ] Fix P0/P1 findings immediately
  - [ ] Assess P2/P3 findings
- [ ] Monitor Semgrep results
  - [ ] Fix security findings first
- [ ] Run full test suite before release
  - [ ] Target: 99%+ pass rate (500+ tests)

### Community Triage (Weekly, 2-3 hours)
- [ ] Check GitHub Issues
  - [ ] Respond to all issues within 3-5 days
  - [ ] Label: bug, feature, documentation, good-first-issue
  - [ ] Close duplicates with reference to original
- [ ] Check GitHub Discussions
  - [ ] Welcome new discussions
  - [ ] Answer questions (or tag committer)
  - [ ] Move off-topic to Discussions #off-topic

### PR Review (Weekly, 3-5 hours)
- [ ] Review all open PRs within 7 days
  - [ ] Request changes for: bugs, incomplete tests, poor docs
  - [ ] Approve if: tests pass, 80%+ coverage, clear intent
  - [ ] Merge if approved + CI green
- [ ] Help PR authors (via comments)
  - [ ] Suggest improvements, not demands
  - [ ] Offer pairing if complex

---

## Success Metrics (Target End of August 2026)

### Distribution
- [ ] Maven Central: 500+ downloads
- [ ] Docker Hub: 200+ pulls
- [ ] GitHub: 100+ stars, 10+ forks

### Community
- [ ] GitHub Discussions: 50+ posts
- [ ] Active contributors: 5+
- [ ] Issues closed: 30+ (cumulative)
- [ ] PRs merged: 15+ (cumulative)

### Technical
- [ ] Test pass rate: 99%+
- [ ] Coverage: 82%+
- [ ] Security findings: 0 critical

### Engagement
- [ ] Blog readers: 1000+ (Medium analytics)
- [ ] Conference proposals: 1-2 accepted
- [ ] Twitter followers: 200+ (if starting project account)

---

## Risk Mitigation Checklist

### Risk: Maven Central Approval Delays
- **Mitigation:** [ ] Start Week 1 (earliest possible)
- **Fallback:** [ ] Use GitHub Packages as interim distribution
- **Owner:** Eric C. Mumford

### Risk: Docker Hub Quota/Limits
- **Mitigation:** [ ] Use GitHub Container Registry as primary
- **Fallback:** [ ] Self-host registry (OCI compatible)
- **Owner:** Eric C. Mumford + DevOps volunteer

### Risk: Contributor Burnout (Maintainer)
- **Mitigation:** [ ] Recruit core maintainers by end of Phase 4
- **Fallback:** [ ] Slow release cadence to 1x per 6 months
- **Owner:** Eric C. Mumford

### Risk: No Community Interest
- **Mitigation:** [ ] Focus on IBM i community (COMMON, forums)
- **Fallback:** [ ] Position as test automation library (broader appeal)
- **Owner:** Eric C. Mumford + marketing volunteer

---

## Quick Reference Links

| Task | Link | Notes |
|------|------|-------|
| Sonatype JIRA | https://issues.sonatype.org | Request namespace |
| Maven Central Search | https://mvnrepository.com | Verify publication |
| Docker Hub | https://hub.docker.com | Create repo |
| GHCR | https://ghcr.io | Auto-available |
| GitHub Discussions | github.com/heymumford/hti5250j/discussions | Create categories |
| Medium | https://medium.com/@heymumford | Blog platform |
| COMMON Conference | https://www.common.org/ | IBM i community |
| JavaOne | https://www.oracle.com/javaone/ | Java community |
| Java Weekly | https://javaweekly.net/ | Submission form |

---

## Document Metadata

**Version:** 1.0
**Date:** February 2026
**Owner:** Eric C. Mumford (@heymumford)
**Status:** Ready to implement
**Phase 1 Start Date:** February 10, 2026
**Phase 1 End Date:** March 9, 2026 (target)

**Print & Post:** Use this checklist during implementation. Update dates weekly.

**Questions?** See OPEN_SOURCE_STRATEGY.md or GOVERNANCE.md
