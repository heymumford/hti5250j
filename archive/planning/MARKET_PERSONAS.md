# HTI5250J Market Analysis & Target Personas

**Purpose:** Understand user segments, pain points, and optimal messaging
**Date:** February 2026
**Status:** Research summary for marketing & community strategy

---

## 1. Persona Overview

HTI5250J targets four distinct user personas, each with different needs, pain points, and adoption barriers:

```
┌─────────────────────────────────────────────────────────────────┐
│         HTI5250J TARGET MARKET SEGMENTATION (Year 1)            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Persona #1         Persona #2        Persona #3    Persona #4 │
│  QA Engineers       Java Developers   DevOps/SRE   Contributors │
│  (Primary)          (Emerging)        (Growing)     (Community)  │
│                                                                 │
│  30% market          40% market        20% market    10% market │
│  2026 estimate       2026 estimate     2026 est.     2026 est.  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Persona #1: IBM i QA Engineers

### Profile
- **Age:** 45-65 (high mean age due to IBM i population)
- **Experience:** 15-30 years in IBM i systems
- **Education:** Associate degree or vocational training
- **Employment:** Fortune 500 / Mid-market manufacturing, finance, logistics
- **Technical depth:** Moderate (knows RPG, CL, 5250 protocols, but not modern dev)

### Pain Points
1. **Manual Testing** - 80% of 5250 testing is manual, copy-paste into spreadsheets
2. **Regression Burden** - Rerunning same test suite after each release (days of work)
3. **Test Documentation** - Keeping step-by-step instructions in sync with screens
4. **Legacy Tools** - Stuck with commercial terminal emulators (Rumba, IBM's iAccess)
5. **Skills Gap** - Team aging; younger hires unfamiliar with 5250 ecosystem
6. **Cost** - Commercial emulator licenses ($500-2000/seat) + support
7. **Reproducibility** - Hard to reproduce bugs from tester's manual notes

### What Success Looks Like
- Automated 50% of regression tests
- Cut test execution time from 3 days to 2 hours
- Version control test artifacts (YAML + CSV)
- Integrate with CI/CD pipeline
- Train next generation on modern automation tools

### Key Messages
- **"Automate 5250 testing like modern apps"** - Regression tests in YAML, executed in CI/CD
- **"Free tool, no licenses"** - Open-source, no per-seat costs
- **"Based on proven protocol"** - TN5250J has 20+ years of maturity
- **"Test reuse"** - Session pooling = faster test cycles

### Channels to Reach Them
1. **COMMON Conference** (Annual, 3000+ attendees)
   - Spring: April (Minneapolis)
   - Fall: October (Alternate city)
   - Cost: Booth ($2-3K) + speaking slot
   - ROI: Highest (direct access to target persona)

2. **IBM System i forums**
   - IBM i Community (forums.ibm.com)
   - WRKGRP (Work Group for IBM i)
   - iTech groups (regional user groups)

3. **Peer networks**
   - IBMi LinkedIn Group (5000+ members)
   - Reddit r/ibm (niche but active)

### Success Indicators (Year 1)
- [ ] 20+ GitHub stars from IBM i community members
- [ ] 1-2 COMMON conference presentations accepted
- [ ] 5+ documented case studies (blog posts with metrics)
- [ ] 50+ Maven Central downloads (this persona alone)
- [ ] 3-5 active contributors from QA background

---

## Persona #2: Enterprise Java Developers

### Profile
- **Age:** 30-45
- **Experience:** 8-15 years in Java (various frameworks)
- **Education:** Bachelor's degree (CS, Engineering)
- **Employment:** Fortune 500 tech teams, financial institutions
- **Technical depth:** High (Spring Boot, microservices, testing frameworks)

### Pain Points
1. **Legacy System Integration** - Modern microservices need to call IBM i
2. **No Native Support** - Spring Data doesn't support 5250 access
3. **Telnet Complexity** - Writing raw Telnet clients is error-prone
4. **Test Automation Gaps** - No framework for integration testing against AS/400
5. **Library Fatigue** - Too many niche libraries; hard to evaluate
6. **Version Mismatch** - Finding compatible versions (Java 11 vs 21, frameworks)

### What Success Looks Like
- Easy dependency injection (Spring Boot starter)
- Type-safe APIs (not raw byte strings)
- Full test coverage in unit/integration tests
- Clear error messages and debugging
- Active maintenance (bugs fixed in weeks, not months)

### Key Messages
- **"Modern Java for legacy systems"** - Spring Boot + Maven Central ready
- **"Type-safe 5250 library"** - Pojo beans, not raw bytes
- **"Headless = containerizable"** - Docker/k8s native
- **"Active maintenance"** - Active GitHub development, not abandoned

### Channels to Reach Them
1. **JavaOne / Oracle Code One** (if returning 2026)
   - Reach: 40,000+ Java developers
   - Format: 30-min talk or hands-on lab
   - ROI: High (persona is target audience)

2. **Developer Communities**
   - r/java (Reddit, 400K+ subscribers)
   - Stack Overflow (tag: telnet, 5250, IBM i)
   - DZone (Java publication, 1M+ readers)
   - InfoQ (enterprise Java mindset)

3. **Content Platforms**
   - Medium (Java publications syndicate)
   - Dev.to (developer community, 600K+ members)
   - Blog posts (Google search for "5250 Java library")

4. **Conference Talks**
   - Title: "Headless Terminal Emulation for Enterprise Integration"
   - Topics: Modernizing legacy system integration, test automation, containerization

### Success Indicators (Year 1)
- [ ] 100+ Maven Central downloads
- [ ] 40-50 GitHub stars from Java community
- [ ] 2-3 blog posts (Medium, Dev.to)
- [ ] 1-2 Java conference talk acceptances
- [ ] 5-10 Stack Overflow answers tagged [5250] + [java]

---

## Persona #3: DevOps/SRE Teams

### Profile
- **Age:** 28-40
- **Experience:** 5-10 years in DevOps/SRE
- **Education:** Bachelor's degree (CS, Engineering, or bootcamp)
- **Employment:** Tech companies, cloud-native shops, modern enterprises
- **Technical depth:** Very high (Kubernetes, CI/CD, infrastructure-as-code)

### Pain Points
1. **Legacy System Monitoring** - IBM i doesn't emit Prometheus metrics
2. **Health Checks** - No standardized way to check 5250 availability
3. **Incident Response** - Manual terminal session to debug production issue
4. **Integration Testing** - CI/CD pipelines can't easily test against IBM i
5. **Containerization** - Legacy systems resist Docker/k8s
6. **Observability** - No traces, logs, or metrics from 5250 sessions

### What Success Looks Like
- Container-native session pooling (DaemonSet on k8s)
- Health checks that integrate with Prometheus/monitoring stack
- CI/CD stages that test against real IBM i
- Session metrics (utilization, latency, errors)
- Incident response without manual terminal access

### Key Messages
- **"Containerize legacy integration"** - Docker/k8s deployment ready
- **"Infrastructure as code"** - Kubernetes manifests included
- **"Modern observability"** - Health checks, metrics, structured logging
- **"CI/CD native"** - GitHub Actions examples, works in Gitlab/Jenkins

### Channels to Reach Them
1. **DevOps Community**
   - DevOps Subreddit (r/devops, 150K+ members)
   - Kubernetes Slack community
   - CNCF (Cloud Native Computing Foundation) mailing list
   - DevOps Days local chapters (free, 500-1000 attendees)

2. **Container Registries**
   - Docker Hub (trending page)
   - GitHub Container Registry (GHCR)
   - Quay.io (Red Hat ecosystem)

3. **Content Platforms**
   - SRE community Slack groups
   - LinkedIn DevOps/SRE posts
   - Blog: "Containerizing Legacy System Integration" (Kubernetes-focused)

### Success Indicators (Year 1)
- [ ] 200+ Docker Hub pulls
- [ ] 30 GitHub stars from DevOps community
- [ ] 1-2 conference talks at DevOps Days
- [ ] Kubernetes example downloaded 50+ times
- [ ] 2-3 CI/CD pipeline examples adopted

---

## Persona #4: Open Source Contributors

### Profile
- **Age:** 25-45
- **Experience:** 3-10 years in software development (polyglot)
- **Education:** Bachelor's degree or self-taught
- **Employment:** Various (startups, FAANG, indie devs)
- **Technical depth:** High (code quality, testing, architecture matters)

### Pain Points
1. **Fork Maintenance Burden** - Code duplication between agent_a/b (known issue)
2. **Test Complexity** - 500+ tests to maintain; understanding architecture takes time
3. **Protocol Knowledge Gap** - TN5250E is niche; documentation is outdated
4. **Architecture Understanding** - How does headless differ from original TN5250J?
5. **Governance Clarity** - How are decisions made? What's the roadmap?

### What Success Looks Like
- **For code quality enthusiasts:**
  - Extract shared code (reduce 98% duplication)
  - Improve test suite organization
  - Add architecture documentation

- **For protocol nerds:**
  - Implement RFC 855 extensions
  - Add support for new CCSID charsets
  - Protocol compliance tests

- **For leadership learners:**
  - Help establish governance model
  - Mentor first-time contributors
  - Lead community discussions

### Key Messages
- **"Help modernize legacy protocol"** - TN5250E is 20+ years old; needs fresh eyes
- **"Learn test architecture"** - 500+ tests = great learning opportunity
- **"Lead a niche community"** - Become core maintainer, influence direction
- **"Solve real problems"** - IBM i automation matters to real companies

### Channels to Reach Them
1. **GitHub Trending**
   - Featured on https://github.com/trending/java
   - GitHub language weekly updates

2. **Hacker News**
   - "Show HN" post for major milestones (v1.0.0)
   - Reaches 2M+ developers

3. **Developer News**
   - Java Weekly newsletter (2K+ subscribers)
   - DZone Java Digest (50K+ subscribers)
   - Morning Brew Dev Edition (30K+ subscribers)

4. **Niche Communities**
   - Protocol/networking forums
   - Testing framework communities (JUnit 5, Mockito)
   - Open source mentorship programs (Google Summer of Code, Outreachy)

### Success Indicators (Year 1)
- [ ] 5-10 active contributors
- [ ] 3 core maintainers recruited
- [ ] 1 code duplication refactoring completed (50% reduction)
- [ ] 100+ GitHub stars from developer community
- [ ] Featured on Java Weekly (1-2 mentions)

---

## 2. Market Size Estimation

### Addressable Market (Year 1)

```
Total Potential Users (Worldwide):
├── Persona #1 (QA Engineers)
│   ├── IBM i installations: ~20,000 active
│   ├── Companies with 5+ QA staff: ~5,000
│   └── Estimated addressable: 2,000-3,000 individuals
│       (conservative: 50-100 likely to adopt)
│
├── Persona #2 (Java Developers)
│   ├── Java developers worldwide: ~10 million
│   ├── Working on enterprise integration: ~500,000
│   ├── Needing IBM i access: ~50,000
│   └── Estimated addressable: 1,000-5,000 individuals
│       (conservative: 50-100 likely to adopt)
│
├── Persona #3 (DevOps/SRE)
│   ├── DevOps/SRE professionals: ~200,000 worldwide
│   ├── Managing legacy system integration: ~20,000
│   ├── With IBM i infrastructure: ~5,000
│   └── Estimated addressable: 500-2,000 individuals
│       (conservative: 20-50 likely to adopt)
│
└── Persona #4 (Contributors)
    ├── Active Java open-source contributors: ~50,000
    ├── Interested in protocol/testing: ~5,000
    ├── Willing to mentor/lead: ~500
    └── Estimated addressable: 100-500 individuals
        (conservative: 5-15 likely to adopt)

Total Year 1 Addressable Market: ~3,500-10,500 individuals
Conservative Year 1 Adoption: 125-265 active users
```

### Market Growth Potential
- **Year 1:** 50-100 downloads (Maven Central)
- **Year 2:** 500-1000 downloads (assuming 5 conference talks, 10 contributors)
- **Year 3:** 2000+ downloads (established as de facto 5250 Java library)

### Revenue Model (Hypothetical, Year 3+)
- **Open source:** Free (GPL-2.0)
- **Consulting:** $150-200/hr for integration help (only if maintainer offers)
- **Commercial dual-license:** €50-200/year for proprietary use (potential, not planned Year 1)

---

## 3. Competitive Landscape

### Direct Competitors

| Competitor | Pros | Cons | HTI5250J Advantage |
|-----------|------|------|-------------------|
| **Commercial Emulators** (Rumba, iAccess) | Mature, UI polished, support contracts | $500-2000/seat, proprietary, no headless | Free, headless, open-source, no licenses |
| **TN5250J** (Original) | Mature, official, 20+ year history | GUI-only, no headless mode, inactive | Headless-optimized, modern Java 21, active |
| **IBM i SDK for Java** (official) | Runs on IBM i, integrated, supported | Not headless, complex setup, limited | Pure Java, runs anywhere, simple |
| **Custom Telnet Clients** | Total control | High maintenance burden, expertise required | Battle-tested protocol implementation |

### Indirect Competitors
- **API Gateway patterns** (use IBM i REST APIs instead of 5250) - Better long-term solution, but requires modernization
- **Robotic Process Automation** (UiPath, Automation Anywhere) - Heavier, more expensive, GUI-focused
- **Kubernetes + legacy adapters** - Emerging pattern, but no standard for 5250

### HTI5250J Positioning
```
Positioning Matrix:

           High Cost
              ↑
              │   Commercial Emulators
              │   (Rumba, iAccess)
              │
              │
        Low   │────────────────────→  High
      Openness   HTI5250J           Complexity
              │     (Free)              TN5250J
              │   (Open Source)      (Original, inactive)
              │
              ↓ Custom Telnet
           Low Cost  Clients
```

**HTI5250J sweet spot:** Free + Open + Headless + Modern

---

## 4. Marketing by Persona

### Persona #1: QA Engineers

**Recommended Content (Year 1):**
1. Blog: "Automating 5250 Regression Testing" (technical walkthrough)
2. Blog: "Case Study: From Manual to Automated Testing" (metrics-focused)
3. Conference talk: "5250 Test Automation in Modern CI/CD" (COMMON)
4. Example workflow: Payment processing, inventory lookup, user management

**Messaging focus:**
- ROI: "Cut testing time from 3 days to 2 hours"
- Cost: "Zero licensing; open-source"
- Ease: "YAML-based workflows, not programming"

**Success metric:** 5-10 active QA engineers in GitHub community by Q4 2026

---

### Persona #2: Java Developers

**Recommended Content (Year 1):**
1. Blog: "Integrating IBM i with Modern Java Microservices" (architecture)
2. Blog: "Testing 5250 Applications in CI/CD Pipelines" (spring-boot example)
3. Conference talk: "Headless Terminal Emulation for Enterprise Java" (JavaOne)
4. Example: Spring Boot starter (future)

**Messaging focus:**
- Integration: "Call IBM i from modern Java apps"
- Type-safety: "No raw byte strings; use POJOs"
- Ecosystem: "Maven Central, Docker, standard tooling"

**Success metric:** 50+ Maven Central downloads; 20+ GitHub stars from Java community

---

### Persona #3: DevOps/SRE

**Recommended Content (Year 1):**
1. Blog: "Containerizing Legacy System Integration" (Docker focus)
2. Example: Kubernetes StatefulSet + ConfigMap for session pooling
3. Example: GitHub Actions workflow (integration testing stage)
4. Conference talk: "Running Legacy Systems in Modern Infrastructure" (DevOps Days)

**Messaging focus:**
- Containerization: "Docker-native, k8s-ready"
- Observability: "Health checks, structured logging"
- Automation: "CI/CD integration, no manual steps"

**Success metric:** 200+ Docker Hub pulls; 5+ k8s deployments documented

---

### Persona #4: Contributors

**Recommended Content (Year 1):**
1. GitHub: Clear GOVERNANCE.md + contributor tiers
2. GitHub: 10+ good-first-issue tagged items
3. Doc: "Contributing to HTI5250J" walkthrough
4. Blog: "Refactoring Agent Code Duplication" (technical deep dive)

**Messaging focus:**
- Impact: "Help modernize legacy protocol"
- Learning: "Rich codebase (500+ tests, 20+ classes)"
- Community: "Shape project direction; become core maintainer"

**Success metric:** 5-10 active contributors; 3+ core maintainers by Q4 2026

---

## 5. Channel Prioritization Matrix

### Marketing Channels by Persona Reach

```
High Impact, High Effort:
├── COMMON Conference (Persona #1) → Speaker + booth
├── JavaOne (Persona #2) → Speaker slot or lab
└── DevOps Days (Persona #3) → Lightning talk or booth

High Impact, Medium Effort:
├── Java Weekly submission (Persona #2)
├── DZone publication (Persona #2)
├── Kubernetes Slack (Persona #3)
└── r/java + r/ibm (Personas #1, #2)

Medium Impact, Low Effort:
├── Twitter (Persona #2, #3, #4)
├── GitHub Trending (Persona #4)
├── Blog posts (Personas #1, #2, #3)
└── LinkedIn (Persona #1, #2)

Low Impact, Low Effort:
├── Medium (distribution)
├── Dev.to (reach)
└── Personal blog (authority)
```

---

## 6. Content Calendar by Persona (Year 1)

### Q1 2026 (Jan-Mar): Foundation
- **Target:** Personas #1, #2 (awareness)
- Blog #1: "Why Headless? 5250 Terminal Emulation Without the GUI"
- Maven Central launch + announcement
- GitHub Discussions launch (seed conversations)

### Q2 2026 (Apr-Jun): Momentum
- **Target:** Personas #1, #4 (engagement)
- COMMON Spring conference (if accepted)
- Blog #2: "Automating IBM i Regression Tests with YAML Workflows"
- Spring Boot integration example
- First contributor recruitment

### Q3 2026 (Jul-Sep): Growth
- **Target:** Personas #2, #3 (expansion)
- Blog #3: "Containerizing Legacy System Integration: 5250 in Docker"
- Kubernetes deployment guide
- GitHub Actions CI/CD examples
- Docker Hub launch

### Q4 2026 (Oct-Dec): Consolidation
- **Target:** All personas (stability)
- Blog #4: "Building Test Pools: Session Reuse & Performance"
- COMMON Fall conference (if accepted)
- v1.0.0 release (if ready)
- Year-in-review + 2027 roadmap

---

## 7. Key Adoption Barriers & Solutions

### Barrier #1: License Fear (GPL-2.0)
- **Perception:** "Free software = not production-ready"
- **Solution:**
  - Clear FAQ: Using as library = no GPL obligations
  - Highlight 20+ year pedigree (TN5250J)
  - Show enterprise adopters (use cases)

### Barrier #2: Integration Complexity
- **Perception:** "Terminal emulation = too hard"
- **Solution:**
  - Provide working examples (Spring Boot, Docker, k8s)
  - Write tutorials (step-by-step)
  - Offer pairing sessions for first-time setup

### Barrier #3: Unknown Project
- **Perception:** "Who maintains this? Will it be abandoned?"
- **Solution:**
  - Active releases (quarterly cadence)
  - Responsive GitHub issues (24-48 hour response)
  - Clear governance (decision-making public)
  - Conference visibility (talks, booths)

### Barrier #4: Protocol Knowledge Gap
- **Perception:** "TN5250E is too obscure to learn"
- **Solution:**
  - Publish protocol documentation
  - Create protocol testing examples
  - Build abstractions (YAML workflows hide complexity)

---

## 8. Conversion Funnel (Year 1 Targets)

```
GitHub Visitors
      ↓ (2% conversion)
Star the project (100 stars)
      ↓ (5% conversion)
Download / Maven Central (5+ downloads)
      ↓ (10% conversion)
Try Example Code (1-2 examples run)
      ↓ (20% conversion)
Open Issue / Ask Question (1-2 community interactions)
      ↓ (30% conversion)
Use in Project (integration, testing, automation)
      ↓ (10% conversion)
Contribute (submit PR)

Year 1 Target: 100-200 visitors → 10 downloads → 1-2 contributors
Year 2 Target: 1000+ visitors → 50-100 downloads → 5-10 contributors
```

---

## 9. Metrics Dashboard

**Track these monthly:**

| Metric | Source | Target (12mo) | Current |
|--------|--------|---------------|---------|
| GitHub stars | GitHub | 100+ | _ |
| Maven Central downloads | Sonatype | 500+ | _ |
| Docker Hub pulls | Docker Hub | 200+ | _ |
| Discussions posts | GitHub | 50+ | _ |
| Active contributors | GitHub | 5+ | _ |
| Blog readers | Medium analytics | 1000+ | _ |
| Conference talks accepted | Proposal tracker | 1-2 | _ |
| Test pass rate | CI logs | 99%+ | _ |

---

## Appendix: Persona Research Sources

1. **IBM i Community Research:**
   - COMMON annual conferences (attendance, speaker feedback)
   - WRKGRP forums (thread analysis, member surveys)
   - LinkedIn IBM i Group (member profiles)

2. **Java Developer Research:**
   - Stack Overflow 5250 tags (question patterns)
   - GitHub Java project analysis
   - Java Weekly newsletter (reader demographics)

3. **DevOps/SRE Research:**
   - CNCF survey (infrastructure preferences)
   - Kubernetes ecosystem study (adoption patterns)
   - SRE community Slack analysis

4. **Open Source Research:**
   - GitHub Trending analysis
   - Google Summer of Code (contributor motivation)
   - Open source mentorship programs (success metrics)

---

## Document Metadata

**Version:** 1.0
**Date:** February 2026
**Author:** Eric C. Mumford (@heymumford)
**Status:** Ready for marketing/community planning
**Next review:** Q2 2026 (after COMMON conference, adjust based on feedback)

---

**Use this document:**
1. **Before writing content** - Check which personas are target audience
2. **Before conference proposals** - Tailor pitch to conference attendees
3. **Before community outreach** - Use channels specific to persona
4. **Monthly metrics review** - Assess progress toward targets

**Questions?** See OPEN_SOURCE_STRATEGY.md for implementation details.
