# HTI5250J Open Source Strategy

**Project:** GPL-2.0-or-later Headless 5250 Terminal Emulator for IBM i
**Maintainer:** Eric C. Mumford (@heymumford)
**Repository:** https://github.com/heymumford/hti5250j
**Status:** v0.12.0 - Production-ready headless fork
**Date:** February 2026

---

## Executive Summary

HTI5250J is a well-architected, tested headless fork of TN5250J targeting a niche but underserved market: IBM i (AS/400) automation engineers and QA professionals who need serverless 5250 terminal emulation. This strategy outlines a path to:

1. **Maximize discoverability** in enterprise Java and IBM i ecosystems
2. **Build community momentum** around test automation and integration use cases
3. **Establish sustainable governance** as a maintained fork
4. **Navigate GPL-2.0 adoption barriers** in enterprise environments
5. **Create multiple distribution channels** to serve different user personas

**Timeline:** 6-month phased rollout with 12-month maturity target.

---

## 1. Distribution Strategy

### 1.1 Maven Central Repository (CRITICAL)

**Why it matters:** 60-70% of enterprise Java developers discover dependencies via Maven Central or Gradle's default repository chain. Absence = invisibility.

**Current state:** Not published to Maven Central.

**Action items:**

1. **Setup:**
   - Obtain JIRA account at Sonatype (https://issues.sonatype.org) - request "Create Jira account"
   - Request namespace: `org.hti5250j` (not `com.heymumford`)
     - Rationale: `org.` prefix for open-source projects; reverse domain hierarchy is standard
     - Alternative: Use `com.heymumford` if you own heymumford.com domain
   - Complete Sonatype OSS setup (PGP key generation, GPG signing)

2. **Gradle Configuration:**
   Update `build.gradle` with publishing plugin:
   ```gradle
   plugins {
       id 'java'
       id 'maven-publish'
       id 'signing'
   }

   publishing {
       publications {
           mavenJava(MavenPublication) {
               from components.java

               pom {
                   name = 'HTI5250J - Headless 5250 Terminal Emulator'
                   description = 'GPL-2.0-or-later headless fork of TN5250J for IBM i automation'
                   url = 'https://github.com/heymumford/hti5250j'

                   licenses {
                       license {
                           name = 'GNU General Public License, version 2 or later'
                           url = 'https://www.gnu.org/licenses/gpl-2.0.html'
                       }
                   }

                   developers {
                       developer {
                           id = 'heymumford'
                           name = 'Eric C. Mumford'
                           email = 'ericmumford@outlook.com'
                       }
                   }

                   scm {
                       url = 'https://github.com/heymumford/hti5250j'
                       connection = 'scm:git:git://github.com/heymumford/hti5250j.git'
                       developerConnection = 'scm:git:ssh://git@github.com/heymumford/hti5250j.git'
                   }
               }
           }
       }

       repositories {
           maven {
               url = 'https://oss.sonatype.org/service/local/staging/deploy/maven2'
               credentials {
                   username = project.findProperty('ossrhUsername') ?: System.getenv('OSSRH_USERNAME')
                   password = project.findProperty('ossrhPassword') ?: System.getenv('OSSRH_PASSWORD')
               }
           }
       }
   }

   signing {
       sign publishing.publications.mavenJava
   }
   ```

3. **GitHub Actions Workflow:**
   Create `.github/workflows/publish-maven-central.yml`:
   ```yaml
   name: Publish to Maven Central

   on:
     push:
       tags:
         - 'v*'

   permissions:
     contents: read

   jobs:
     publish:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - uses: actions/setup-java@v4
           with:
             java-version: '21'
             distribution: 'temurin'

         - name: Import GPG key
           run: |
             echo "${{ secrets.MAVEN_GPG_PRIVATE_KEY }}" | base64 -d > /tmp/private.key
             gpg --import /tmp/private.key

         - name: Publish to Maven Central
           run: |
             ./gradlew publish -PossrhUsername=${{ secrets.OSSRH_USERNAME }} \
               -PossrhPassword=${{ secrets.OSSRH_PASSWORD }} \
               -Psigning.keyId=${{ secrets.MAVEN_GPG_KEY_ID }} \
               -Psigning.password=${{ secrets.MAVEN_GPG_PASSWORD }}
   ```

4. **Release checklist:**
   - Update version in `gradle.properties` (SemVer)
   - Update CHANGELOG.md with release notes
   - Tag release: `git tag -a v0.13.0 -m "Release v0.13.0"`
   - Push tag: `git push --tags`
   - GitHub Actions publishes automatically

**Expected timeline:** 2-4 weeks (Sonatype approval + testing)

**Impact:** Immediate discoverability for 80%+ of enterprise Java developers

---

### 1.2 GitHub Packages

**Why it matters:** Drop-in alternative for organizations with Maven Central access restrictions; builds community momentum on GitHub.

**Setup:**
```gradle
repositories {
    maven {
        url = 'https://maven.pkg.github.com/heymumford/hti5250j'
        credentials {
            username = System.getenv('GITHUB_ACTOR')
            password = System.getenv('GITHUB_TOKEN')
        }
    }
}
```

**Usage (by consumers):**
```gradle
repositories {
    maven {
        url = 'https://maven.pkg.github.com/heymumford/hti5250j'
        credentials {
            username = github_username
            password = github_token  // PAT with 'read:packages' scope
        }
    }
}
```

**Automated publishing:**
Add to `publish-maven-central.yml` workflow:
```yaml
- name: Publish to GitHub Packages
  run: |
    ./gradlew publish \
      -Dgithub.actor=${{ github.actor }} \
      -Dgithub.token=${{ secrets.GITHUB_TOKEN }}
```

**Expected timeline:** 1 week (requires only GitHub Actions configuration)

---

### 1.3 Docker Hub & GitHub Container Registry

**Why it matters:** Enables serverless/containerized deployments; appeals to DevOps and infrastructure-as-code teams.

**Use cases:**
- Session pooling as a microservice: `docker run -p 5250:23 hti5250j:latest`
- CI/CD pipeline integration: pre-configured test harness
- Kubernetes sidecars: tn5250 service mesh

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine

# SPDX-FileCopyrightText: 2026 Eric C. Mumford
# SPDX-License-Identifier: GPL-2.0-or-later

WORKDIR /app

# Copy built artifacts
COPY build/libs/hti5250j-*.jar app.jar

# Expose telnet port
EXPOSE 23

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD java -cp app.jar com.heymumford.tn5250j.health.HealthCheck

# Default entrypoint: session pool server
ENTRYPOINT ["java", "-jar", "app.jar", "session-pool-server"]
```

**Build & publish workflow:** `.github/workflows/publish-docker.yml`
```yaml
name: Build and Publish Docker Image

on:
  push:
    tags:
      - 'v*'

jobs:
  docker:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build JAR
        run: ./gradlew build -x test

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: |
            heymumford/hti5250j
            ghcr.io/heymumford/hti5250j
          tags: |
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=ref,event=branch
            type=sha

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

**Expected timeline:** 2-3 weeks (requires Docker Hub account + token setup)

---

### 1.4 SDKMAN (Optional, Lower Priority)

**Why it matters:** Command-line tool version management for JVM ecosystem; popular with developers who use Java 21, Groovy, Kotlin, etc.

**Current maturity:** Pre-1.0 release; defer to v1.0.0 milestone

**When ready:**
1. Submit application to SDKMAN: https://sdkman.io/vendors
2. Provide build artifacts and release automation
3. Benefit: One-command install: `sdk install hti5250j`

---

## 2. Community Building Plan

### 2.1 Target Personas & Channels

**Persona 1: IBM i QA Engineers**
- **Pain point:** Manual 5250 testing; spreadsheet-based test matrices
- **Channel:** IBM i forums, WRKGRP (Work Group for IBM i), COMMON conference
- **Message:** "Automate 5250 regression tests with YAML workflows and version control"
- **Proof:** Session pooling + workflow examples

**Persona 2: Enterprise Java Developers**
- **Pain point:** Legacy system integration in modern architectures
- **Channel:** r/java, InfoQ, DZone, Java Reddit
- **Message:** "Headless 5250 library for modern Java 21 + testing frameworks"
- **Proof:** Maven Central publication, spring-boot-starter (future)

**Persona 3: DevOps/SRE Teams**
- **Pain point:** Health checks, monitoring, CI/CD integration with IBM i
- **Channel:** DevOps subreddits, Kubernetes Slack, CNCF mailing lists
- **Message:** "Containerized 5250 session pooling for k8s and cloud deployments"
- **Proof:** Docker images, health checks, observability examples

**Persona 4: Open Source Contributors**
- **Pain point:** Fork maintenance burden (code duplication, test complexity)
- **Channel:** GitHub trending, HackerNews, r/golang, r/rust (polyglots)
- **Message:** "Help modernize legacy 5250 protocol; improve test coverage"
- **Proof:** Clear contributing guide, REFACTOR_PLAN.md, code quality metrics

---

### 2.2 Community Touchpoints

#### GitHub Discussions (Already enabled)

**Organize categories:**
1. **Getting Started** - Quick start, common errors, library setup
2. **Workflows** - YAML workflow design, best practices, examples
3. **Protocol** - TN5250E, RFC 854, charset encoding, edge cases
4. **Integration** - Spring Boot, Gradle, Maven, Docker, Kubernetes
5. **Testing** - Unit tests, session mocks, CI/CD patterns

**Initial seed conversations:**
- "How to test 5250 applications in CI/CD"
- "Best practices for session pooling in production"
- "Migrating from manual testing to automation"

---

#### GitHub Issues (Already enabled)

**Label system:**
```
Labels:
  - bug/p0, bug/p1, bug/p2
  - feature/headless, feature/protocol, feature/testing
  - docs/api, docs/example, docs/faq
  - ecosystem/maven-central, ecosystem/docker, ecosystem/spring-boot
  - help-wanted
  - good-first-issue (for new contributors)
```

**Triage template:**
```
- Link to related discussion (if applicable)
- Reproduction steps (for bugs)
- Expected vs. actual behavior
- Environment (Java version, OS, IBM i target)
- Acceptance criteria (for features)
```

---

#### Conference Presence (Year 1)

**Target conferences:**

1. **COMMON (Cooperative Open IBM Users Multiple Environment)**
   - Timing: April and October (North America)
   - Audience: 3000+ IBM i professionals
   - Cost: $1500-3000 sponsorship + booth
   - Content: "Automating 5250 in the Cloud Era" (90-min session)
   - ROI: Direct access to target persona #1

2. **JavaOne / Oracle Code One** (if returning 2026)
   - Audience: 40,000+ Java developers
   - Content: "Headless Terminal Emulation for Enterprise Integration"
   - Format: 30-min talk OR lab session
   - ROI: Visibility to persona #2 (enterprise Java)

3. **AWS re:Invent** (if relevant to AWS integration)
   - AWS + IBM i modernization angle
   - Target: "Migration to cloud-native architectures"

4. **DevOps Days** (local chapters)
   - Low-cost community events
   - Message: "Containerizing Legacy System Integration"
   - Format: 20-min lightning talk

**Proposal template for 2026 conference season:**
```markdown
Title: Automating IBM i 5250 Terminal Testing with Headless HTI5250J

Abstract:
IBM i (AS/400) applications still power mission-critical financial,
manufacturing, and logistics systems worldwide. Yet testing remains
labor-intensive and manual. Learn how HTI5250J (headless fork of TN5250J)
brings 5250 terminal emulation into the modern testing ecosystem: YAML
workflows, containerization, CI/CD integration, and 80%+ test coverage.

Target Audience: QA engineers, automation specialists, enterprise Java
developers, DevOps teams modernizing IBM i

Learning Outcomes:
- Understand 5250 protocol (TN5250E) and headless emulation
- Automate 5250 tests with YAML workflows and session pooling
- Deploy as microservice (Docker, Kubernetes, AWS)
- Contribute to active open-source project
```

---

### 2.3 Content & Outreach

#### Blog Posts (3-4 per year)

**Year 1 topics:**
1. "Why Headless? 5250 Terminal Emulation Without the GUI" (Q1)
2. "Automating IBM i Regression Tests with YAML Workflows" (Q2)
3. "Containerizing Legacy System Integration: 5250 in Docker" (Q3)
4. "Building Test Pools: Session Reuse & Performance Benchmarks" (Q4)

**Platforms:** Medium (reach), Dev.to (community), personal blog (authority)

---

#### Documentation Examples (10-15 inline)

Current docs are strong. Add:
- `examples/spring-boot-integration/` - Spring Boot starter example
- `examples/docker-compose/` - Multi-container test environment
- `examples/kubernetes/` - K8s deployment (StatefulSet for sessions)
- `examples/ci-cd/` - GitHub Actions, GitLab CI, Jenkins pipelines
- `examples/performance-testing/` - JMH benchmarks, load testing

---

#### Newsletter & Mailing Lists

1. **Announce releases via:**
   - Java Weekly (https://javaweekly.net/) - submit each release
   - DZone - syndicate blog posts
   - r/java - post release notes with context

2. **Create low-traffic mailing list** (Mailchimp free tier or GitHub Discussions digest):
   - Monthly feature highlight
   - Quarterly state-of-project summary
   - User spotlights (who's using HTI5250J?)

---

### 2.4 First Contributors Program

**Goal:** Get 5-10 active contributors by end of Year 1

**"Good first issue" strategy:**
1. Tag simple issues (`good-first-issue`, `help-wanted`)
2. Provide acceptance criteria (not just "fix this")
3. Pair with quick 1:1 pairing session (30 min, async video)
4. Fast-track PRs (review within 24 hours)

**Examples:**
- Add 5 missing SPDX headers in test files
- Write JavaDoc for 3 public methods
- Add example YAML workflow for inventory inquiry
- Create CI/CD example for TestNG + HTI5250J

---

## 3. License Implications & Enterprise Adoption

### 3.1 GPL-2.0-or-later Impact

**Current license:** GPL-2.0-or-later (copyleft)

**Implications for enterprise adoption:**

| Scenario | Impact | Mitigation |
|----------|--------|-----------|
| **Using as library** | None (linking ≠ distribution) | Clearly document: "Use as library = no GPL obligations" |
| **Bundling in product** | HIGH (derivative work = GPL) | License compatibility guide; suggest dual-licensing |
| **Modifying source** | MEDIUM (internal use OK) | Contribute back; get PR merged for long-term support |
| **Selling modifications** | MEDIUM (allowed, but disclose) | Document GPL terms clearly |

**Real-world example:**
- Company X uses HTI5250J as library in internal CI/CD tool → **No GPL obligations**
- Company Y modifies HTI5250J and ships in appliance → **Must provide source on request**

---

### 3.2 Why GPL-2.0?

**Advantages:**
1. **Upstream compatibility** - Original TN5250J is GPL-2.0
2. **Community protection** - Prevents proprietary forks without contribution
3. **Fork harmony** - Forks must remain open-source; benefits original project

**Disadvantages:**
1. **Fortune 500 caution** - Legal teams sometimes avoid GPL (perception > reality)
2. **No dual-license** - Cannot sell proprietary versions (unlike MIT/Apache)
3. **Reciprocity** - Any bundled improvements must be shared back

---

### 3.3 Dual-Licensing Path (Future Option)

**If enterprise adoption plateaus after Year 2, consider:**

1. **Contributor License Agreement (CLA)**
   - Require contributors to assign copyright or grant license
   - Enables licensor to offer dual licensing

2. **Commercial license tier**
   - GPL-2.0 for open-source projects (free)
   - Commercial license for proprietary applications (€/year)
   - Example: Spring Framework (dual GPL + commercial)

3. **Cost-benefit analysis:**
   - Revenue upside: €50-200K/year (estimate for niche market)
   - Complexity cost: CLA mgmt, license enforcement, support tier
   - Community cost: Perception shift ("selling code")

**Recommendation for 2026:** Stick with pure GPL-2.0. Revisit dual-licensing after v1.0.0 (12-month maturity).

---

### 3.4 License Communication Strategy

**Create FAQ page: `docs/LICENSE_FAQ.md`**

```markdown
# GPL-2.0-or-later License FAQ

## Am I required to open-source my code if I use HTI5250J?

### No, not for library usage.

If you link HTI5250J as a library (Maven dependency):
- Your code can be proprietary
- No disclosure required
- No source code sharing

Example: Enterprise tool using HTI5250J for 5250 automation = not a derivative work.

### Yes, only if you distribute modifications.

If you modify HTI5250J AND distribute the modified version:
- You must provide source code
- You must license under GPL-2.0-or-later
- Suggested: Contribute back to this project

Example: Appliance vendor ships custom HTI5250J = must provide source.

## Can I use HTI5250J in a commercial product?

**Yes.** GPL-2.0 allows commercial use. You can:
- Sell applications using HTI5250J
- Charge for consulting/support
- License modifications (if you own copyright)

You cannot:
- Sell proprietary modifications without GPL disclosure
- Hide modifications from your customers

## What if my company has a "no GPL" policy?

1. Check your policy scope - does it prohibit library dependencies (no) or derivative works (yes)?
2. Consult your legal team - most "no GPL" policies allow library linking
3. Consider alternatives - but HTI5250J's GPL status reflects its heritage from TN5250J

## Can I contribute code to HTI5250J?

**Yes!** By contributing, you:
- Agree your code is licensed GPL-2.0-or-later
- Maintain copyright (you own what you write)
- Allow this project to use your code perpetually

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details.
```

---

## 4. Governance Model

### 4.1 Maintainer Role (Current State)

**Eric C. Mumford (@heymumford)**
- **Responsibilities:**
  - Release decisions (timing, scope)
  - Architecture reviews (new features)
  - Triage issues (priority, assignment)
  - Security vulnerability coordination
  - Community leadership

- **Workload estimate:** 8-10 hrs/week (part-time)

---

### 4.2 Contributor Hierarchy (Future)

**As community grows, establish:**

**Tier 1: Core Maintainers (2-3 people)**
- Merge PRs after review
- Release decisions
- Architecture guidance
- Examples: one other IBM i expert, one Java async expert

**Tier 2: Committers (3-5 people)**
- Review code
- Triage issues
- No merge rights (but fast-track to maintainer)
- Examples: active contributors with 5+ merged PRs

**Tier 3: Contributors (everyone with merged PR)**
- Submit PRs
- Review peers' work
- Participate in discussions
- Eligible for committer promotion after 3 PRs

---

### 4.3 Release Process

**Current:** Tag-based releases in GitHub Actions ✓

**Document formal process:**

```markdown
# Release Checklist

1. **Plan release** (GitHub Milestones)
   - Set version (SemVer)
   - List features/fixes
   - Target date

2. **Prepare code** (feature branch)
   - Merge all target PRs
   - Run full test suite
   - Update CHANGELOG.md
   - Update version in gradle.properties

3. **Pre-release** (candidate branch)
   - Tag: git tag -a v0.13.0-rc1 -m "Release Candidate 1"
   - Test Maven Central staging
   - Request community testing

4. **Release** (main branch)
   - Merge release candidate
   - Tag: git tag -a v0.13.0 -m "Release v0.13.0"
   - Push tags (GitHub Actions publishes automatically)

5. **Post-release** (communication)
   - Write release notes
   - Post to GitHub Discussions
   - Tweet from project account
   - Email mailing list
   - Submit to Java Weekly, DZone
```

---

### 4.4 Decision Making

**For small decisions** (bug fixes, doc improvements):
- Maintainer approves and merges

**For medium decisions** (new features, API changes):
- Propose in GitHub Issue
- Get 2+ approvals (maintainer + 1 committer)
- Discuss in GitHub Discussions if contentious

**For large decisions** (breaking changes, license changes, governance changes):
- RFC (Request for Comments) in GitHub Discussions
- Community input for 2 weeks
- Maintainer decides with consensus

---

## 5. Marketing Channels & Content Calendar

### 5.1 Channel Prioritization

**Tier 1 (Monthly effort):**
- GitHub Releases (automatic with version tags) ✓
- GitHub Discussions (seed conversations, respond to questions)
- README.md (keep up-to-date with examples)

**Tier 2 (Quarterly effort):**
- Blog posts (2-4 per year)
- Twitter/LinkedIn updates
- Conference proposals

**Tier 3 (Opportunistic):**
- Podcast interviews (if invited)
- Journal/magazine articles
- Community event sponsorships

---

### 5.2 Social Media Strategy

**Twitter (@heymumford or project account)**
- Weekly tips: "TN5250E tip: Attributes encode display behavior" (thread)
- Monthly milestones: "v0.13.0 released: 200+ new tests, session pooling improvements"
- Retweet relevant IBM i, Java, automation content

**LinkedIn (company page if created)**
- Quarterly state-of-project posts
- Thought leadership: "5250 Protocol Modernization"
- Team spotlights: "Meet HTI5250J contributors"

**Reddit**
- r/java: Announcement posts for releases
- r/ibm: Niche but high-quality audience for IBM i news
- r/devops: DevOps-focused discussions (containerization, CI/CD)

---

### 5.3 Content Calendar (Year 1)

```
Q1 (Jan-Mar):
- Blog: "Why Headless? 5250 Terminal Emulation Without the GUI"
- Release: v0.12.1 (bug fixes)
- Content: API documentation expansion
- Conference: Submit proposals to COMMON (April)

Q2 (Apr-Jun):
- Conference: COMMON presentation (if accepted)
- Blog: "Automating IBM i Regression Tests with YAML Workflows"
- Release: v0.13.0 (new features)
- Content: Spring Boot integration examples

Q3 (Jul-Sep):
- Blog: "Containerizing Legacy System Integration: 5250 in Docker"
- Release: v0.13.1 (Docker improvements)
- Content: Kubernetes examples, health checks
- Start recruiting first 5 core contributors

Q4 (Oct-Dec):
- Blog: "Building Test Pools: Session Reuse & Performance Benchmarks"
- Release: v0.14.0 or v1.0.0 (major milestone)
- Content: Year-in-review, roadmap for Year 2
- Conference: COMMON October (if applicable)
```

---

## 6. Packaging Recommendations

### 6.1 JAR Artifact Types

**Current build output:** JAR (library)

**Recommend providing:**

1. **Standard JAR** (already done)
   - Library consumers
   - Maven Central
   - Filename: `hti5250j-0.13.0.jar`

2. **Fat JAR** (all dependencies included)
   - CLI tool usage
   - Docker images
   - Filename: `hti5250j-0.13.0-all.jar`
   ```gradle
   task fatJar(type: Jar) {
       manifest.attributes 'Main-Class': 'com.heymumford.tn5250j.cli.Main'
       archiveBaseName = 'hti5250j'
       archiveClassifier = 'all'
       from {
           configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
       }
       with jar
   }
   ```

3. **Source JAR** (for IDE integration)
   - Already provided by Maven Central plugin ✓

4. **Javadoc JAR** (API documentation)
   - Already provided by Maven Central plugin ✓

---

### 6.2 Docker Image Variants

```dockerfile
# Multi-stage build for size optimization

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

# Stage 2: Runtime (minimal)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/hti5250j-*.jar app.jar

# Stage 3: Full image (with debugging tools)
FROM eclipse-temurin:21-jre AS debug
WORKDIR /app
RUN apt-get update && apt-get install -y curl jq
COPY --from=builder /app/build/libs/hti5250j-*.jar app.jar
```

**Image tagging strategy:**
```
Docker Hub / GitHub Container Registry:
- heymumford/hti5250j:0.13.0      (semver tag)
- heymumford/hti5250j:latest      (auto-updated)
- heymumford/hti5250j:0.13        (minor version)
- heymumford/hti5250j:0            (major version)
- heymumford/hti5250j:alpine      (minimal variant)
- heymumford/hti5250j:debug       (with tools)
```

---

### 6.3 GraalVM Native Image (Future Tier)

**Benefits:** Startup time <100ms, smaller footprint (50MB vs 500MB), no JVM overhead

**When to target:** Post-v1.0.0 (requires stability)

**Build configuration:**
```gradle
plugins {
    id 'org.graalvm.buildtools.native' version '0.9.28'
}

graalvmNative {
    binaries {
        main {
            mainClass = 'com.heymumford.tn5250j.cli.Main'
            runtimeArgs = ['--enable-preview']
        }
    }
}
```

**Command:**
```bash
./gradlew nativeCompile  # Produces: hti5250j binary (no .jar extension)
```

**Use case:** Lightweight k8s sidecar, fast CLI tool

---

## 7. Versioning Strategy

### 7.1 Semantic Versioning (SemVer)

**HTI5250J follows SemVer 2.0.0**

Format: `MAJOR.MINOR.PATCH[-PRERELEASE][+METADATA]`

**Increment rules:**

| Change | MAJOR | MINOR | PATCH |
|--------|-------|-------|-------|
| Breaking API changes | ✓ | | |
| New backward-compat features | | ✓ | |
| Bug fixes | | | ✓ |
| Pre-1.0 = unstable API | Can | Can | Can |

**Examples:**
- `0.12.0` - v0.12, release 0 (initial)
- `0.12.1` - Bug fix release
- `0.13.0` - New features (backward-compatible)
- `1.0.0` - Stable API, production-ready
- `1.1.0-alpha.1` - Pre-release (testing)
- `1.1.0-rc.1+upstream.0.8.0` - Release candidate with upstream link

---

### 7.2 Fork Versioning Policy

**Problem:** How to align version with upstream TN5250J (also at 0.x)?

**Solution: Upstream-anchored versioning**

1. **Base version = Upstream version**
   - If upstream is at 0.8.0, headless version starts at 0.8.0+
   - Allows easy tracking of fork delta

2. **Suffix notation (optional)**
   - `0.12.0-headless.1` if you want to distinguish
   - Or just `0.12.0` (simpler, assumes TN5250J is abandoned)

3. **Metadata for upstream tracking**
   - `0.12.0+upstream.0.8.0.abc123def` (full context)

**Recommendation for HTI5250J:**
- Use pure SemVer (`0.12.0`, `1.0.0`, etc.)
- Document in `CONTRIBUTING.md` that headless diverged from upstream
- Keep `UPSTREAM_SYNC.md` to track compat status

---

### 7.3 Release Cadence

**Target cadence:** 1-2 releases per quarter (3-4 per year)

| Cycle | Phase | Duration | Trigger |
|-------|-------|----------|---------|
| **Planning** | Milestone definition | 1 week | Maintainer proposal |
| **Development** | Feature/fix branches | 4-6 weeks | Community PRs |
| **Testing** | RC releases, feedback | 1-2 weeks | Release candidate |
| **Release** | Tag, publish, announce | 1-3 days | Merge to main |
| **Buffer** | Hotfixes, stabilization | 2-4 weeks | Reported issues |

**Current pace:** v0.12.0 (Feb 2026) → v0.13.0 (May 2026 estimated) → v1.0.0 (Oct 2026 target)

---

## 8. Implementation Roadmap

### Phase 1: Foundation (Weeks 1-4)

**Deliverables:**
- [ ] Maven Central account + namespace approval
- [ ] Update `build.gradle` with publishing config
- [ ] Create `.github/workflows/publish-maven-central.yml`
- [ ] Update `gradle.properties` to v0.13.0
- [ ] Write `docs/LICENSE_FAQ.md`
- [ ] Create GitHub Discussions categories

**Effort:** 15 hours
**Owner:** Eric C. Mumford

---

### Phase 2: Maven Central Release (Weeks 5-8)

**Deliverables:**
- [ ] Generate GPG keys, upload to Maven Central
- [ ] Publish v0.13.0 to Maven Central (first release)
- [ ] Test dependency resolution from consumer perspective
- [ ] Update README.md with Maven Central coordinates
- [ ] Announce release on GitHub Discussions + r/java

**Effort:** 8 hours
**Owner:** Eric C. Mumford

**Success metric:** Artifact discoverable at https://mvnrepository.com

---

### Phase 3: Docker & CI/CD (Weeks 9-12)

**Deliverables:**
- [ ] Create Dockerfile (2 variants: standard, debug)
- [ ] Create `.github/workflows/publish-docker.yml`
- [ ] Set up Docker Hub account + secrets
- [ ] Publish Docker images for v0.13.0
- [ ] Create `examples/docker-compose/` (test environment)
- [ ] Create `examples/ci-cd/` (GitHub Actions, GitLab CI examples)

**Effort:** 20 hours
**Owner:** Eric C. Mumford + 1 contributor (Docker expertise)

---

### Phase 4: Content & Community (Weeks 13-20)

**Deliverables:**
- [ ] Write 2 blog posts (Medium + personal blog)
- [ ] Create 5+ documentation examples
- [ ] Set up low-traffic mailing list (Mailchimp)
- [ ] Tag 10+ "good-first-issue" items
- [ ] Reach out to 5 potential first contributors
- [ ] Submit conference proposals (COMMON, JavaOne)

**Effort:** 25 hours
**Owner:** Eric C. Mumford + marketing volunteer (if available)

---

### Phase 5: GraalVM & Future Packaging (Weeks 21+)

**Deliverables:**
- [ ] Add GraalVM native-image support
- [ ] Create native binaries for macOS, Linux, Windows
- [ ] Document performance improvements
- [ ] Create Homebrew formula (macOS distribution)

**Effort:** 30 hours
**Owner:** Deferred to v1.0.0 milestone (Q4 2026)

---

## 9. Success Metrics & KPIs

### 9.1 Distribution Metrics

| Metric | Target (12 months) | How to measure |
|--------|-------------------|---|
| Maven Central downloads | 5,000+ | Sonatype dashboard, mvnrepository.com |
| Docker Hub pulls | 2,000+ | Docker Hub repository stats |
| GitHub stars | 150+ | GitHub repository page |
| GitHub forks | 25+ | GitHub repository page |

---

### 9.2 Community Metrics

| Metric | Target (12 months) | How to measure |
|--------|-------------------|---|
| GitHub Discussions posts | 100+ | GitHub Discussions stats |
| Issue response time | <48 hours | GitHub issue timestamps |
| Active contributors | 5+ | GitHub contributors page |
| Conference talk acceptances | 1-2 | Proposal tracker |

---

### 9.3 Technical Metrics

| Metric | Target | How to measure |
|--------|--------|---|
| Test coverage | 85%+ | Codecov / Gradle report |
| Security scan findings | 0 critical | CodeQL + Semgrep results |
| Release cadence | 1-2 per quarter | GitHub releases page |
| Average review time | <7 days | GitHub PR history |

---

### 9.4 Adoption Metrics (Estimated)

| Metric | Year 1 | Year 2 |
|--------|--------|--------|
| Estimated users | 50-100 | 200-500 |
| Production deployments | 5-10 | 30-50 |
| Reported bugs fixed | 15-20 | 30-40 |
| Community PRs merged | 5-10 | 20-30 |

---

## 10. Risks & Mitigation

### 10.1 License Friction

**Risk:** Enterprise adopters hesitate due to GPL-2.0

**Mitigation:**
- Create FAQ clarifying library use = no obligations
- Link to successful GPL projects (Linux, Apache Kafka uses libraries)
- Offer dual-licensing consultation (future option)
- Monitor adoption barriers via GitHub Discussions

---

### 10.2 Upstream Synchronization

**Risk:** TN5250J upstream releases conflicting changes

**Mitigation:**
- Document fork strategy in README
- Track upstream changes in `UPSTREAM_SYNC.md`
- Maintain separate branches if merging upstream features
- Communicate fork divergence clearly

---

### 10.3 Maintainer Burnout

**Risk:** Single maintainer + part-time commitment = bottleneck

**Mitigation:**
- Recruit core maintainers by end of Q2 2026
- Document decision-making process (RFC template)
- Use GitHub templates to reduce triage burden
- Scale via committer tier (review without merge rights)

---

### 10.4 Market Size Ceiling

**Risk:** IBM i ecosystem is aging; limited growth potential

**Mitigation:**
- Target emerging use cases (cloud modernization, API gateways)
- Position as test automation tool (broader appeal than just IBM i)
- Build CLI + containerization (lowers barrier to entry)
- Monitor adoption trends; pivot if needed

---

## 11. Budget & Resources

### 11.1 No-Cost Resources (Already Available)

- GitHub (free tier supports unlimited public repos) ✓
- GitHub Actions (free for public repos) ✓
- GitHub Discussions (built-in) ✓
- Maven Central (free for open-source) ✓
- Docker Hub (free tier) ✓
- Medium (free blogging platform)
- r/java, r/ibm (free communities)

---

### 11.2 Optional Paid Resources

| Resource | Cost | Benefit | Priority |
|----------|------|---------|----------|
| Mailchimp Pro | $20/month | Newsletter features | Low |
| Domain (hti5250j.dev) | $15/year | Branding | Low |
| Conference booth (COMMON) | $2,000-3,000 | Direct reach to 3000+ IBMi pros | High (Year 1) |
| Blog platform upgrade (Medium) | $12/month | Remove paywall, analytics | Low |
| Consulting (code review, architecture) | $0-2,000 | Speed up contributor onboarding | Medium (Year 1) |

**Estimated Year 1 budget:** $2,500-5,500 (mostly conference sponsorship)

---

## 12. Conclusion & Next Steps

### 12.1 Why This Strategy Works for HTI5250J

1. **Fills a real gap** - IBM i automation testing lacks modern tooling
2. **Clear differentiation** - Only headless 5250 fork for Java ecosystem
3. **Technical excellence** - 99.55% test pass rate, strong architecture
4. **License clarity** - GPL-2.0 aligns with TN5250J heritage
5. **Manageable scope** - Single maintainer feasible with community support
6. **Multi-channel distribution** - Maven Central, Docker, CLI, library

### 12.2 Immediate Next Steps (Next 30 days)

1. **Week 1:**
   - [ ] Request Maven Central account (Sonatype JIRA)
   - [ ] Create GitHub Discussions categories
   - [ ] Draft v0.13.0 release notes

2. **Week 2:**
   - [ ] Configure Gradle publishing + GitHub Actions workflow
   - [ ] Generate GPG keys for signing

3. **Week 3:**
   - [ ] Create `docs/LICENSE_FAQ.md`
   - [ ] Start blog post: "Why Headless?"

4. **Week 4:**
   - [ ] Tag & publish v0.13.0 to Maven Central
   - [ ] Announce on r/java, GitHub Discussions

### 12.3 Quarterly Milestones

**Q1 2026 (Jan-Mar):**
- Maven Central publication (v0.13.0)
- License FAQ + community touchpoints
- Blog post series initiated

**Q2 2026 (Apr-Jun):**
- Docker Hub integration
- Spring Boot examples
- First conference talk (COMMON if accepted)

**Q3 2026 (Jul-Sep):**
- 5+ active contributors recruited
- Kubernetes deployment guides
- v0.14.0 feature release

**Q4 2026 (Oct-Dec):**
- v1.0.0 stable release
- Potential COMMON October conference
- Year-in-review + roadmap

---

## Appendix A: Resources & References

### Maven Central Publishing
- Sonatype OSS Repository Hosting: https://central.sonatype.org/
- Maven Central Requirements: https://central.sonatype.org/publish/requirements/
- Gradle Publishing Plugin: https://docs.gradle.org/current/userguide/publishing-maven.html

### IBM i Community
- COMMON Annual Conference: https://www.common.org/
- IBM i Forums: https://www.ibm.com/community/z/
- Work Group for IBM i (WRKGRP): Community mailing list

### Docker & Container Distribution
- Docker Hub: https://hub.docker.com/
- GitHub Container Registry: https://ghcr.io/
- Best Practices: https://docs.docker.com/develop/dev-best-practices/

### Conference Proposals
- COMMON Call for Papers: https://www.common.org/speakers/cfp
- JavaOne (2024+): https://www.oracle.com/javaone/
- DevOps Days: https://devopsdays.org/find-a-devopsday

### Open Source Governance
- CNCF Governance Guidelines: https://www.cncf.io/blog/2020/08/31/governance-in-open-source/
- Fork Maintenance Guide: https://opensource.guide/best-practices/

### Dual-Licensing Examples
- Spring Framework: https://spring.io/projects/spring-framework (dual GPL/commercial)
- MySQL: Dual GPL/commercial licensing model
- CPAL (Common Public Attribution License): Alternative to dual-licensing

---

## Appendix B: Template Files to Create

These files should be created to support the strategy above:

1. **`docs/LICENSE_FAQ.md`** - Enterprise licensing concerns (shown in section 3.4)
2. **`.github/workflows/publish-maven-central.yml`** - Automatic Maven Central publication
3. **`.github/workflows/publish-docker.yml`** - Docker image builds
4. **`examples/docker-compose/`** - Multi-container test environment
5. **`examples/spring-boot-integration/`** - Spring Boot starter example
6. **`examples/kubernetes/`** - K8s StatefulSet + configurable session pooling
7. **`examples/ci-cd/`** - GitHub Actions, GitLab CI, Jenkins patterns
8. **`UPSTREAM_SYNC.md`** - Fork divergence tracking
9. **`GOVERNANCE.md`** - Decision-making, contributor tiers, release process
10. **`docs/CONTRIBUTOR_ONBOARDING.md`** - First-time contributor guide

---

## Document Metadata

**Version:** 1.0
**Date:** February 2026
**Author:** Eric C. Mumford (@heymumford)
**Status:** Approved for implementation
**Next review:** May 2026 (post-Maven Central launch)

---

**Questions?** Open discussion in GitHub Discussions or email ericmumford@outlook.com.
