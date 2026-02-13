# HTI5250J Open Source Strategy - Executive Summary

**Project:** GPL-2.0-or-later Headless 5250 Terminal Emulator for IBM i
**Maintainer:** Eric C. Mumford (@heymumford)
**Current Version:** v0.12.0
**Strategy Date:** February 2026

---

## The Opportunity

HTI5250J fills a critical gap in the Java ecosystem: **there is no actively maintained, headless 5250 terminal emulator library for IBM i integration and test automation**.

The original TN5250J is 20+ years old and GUI-focused. Modern companies automating IBM i systems must either:
1. Use expensive commercial terminal emulators ($500-2000/seat)
2. Write custom Telnet clients (time-consuming, fragile)
3. Use RPA tools (bloated, expensive, not integrated with CI/CD)

**HTI5250J solves this with:**
- ✓ Pure Java library (headless, containerizable)
- ✓ Modern architecture (99.55% test pass rate, session pooling)
- ✓ Open source (GPL-2.0, free to use)
- ✓ Active maintenance (monthly updates, responsive community)

---

## Strategic Goals (12 months)

| Goal | Target | Owner |
|------|--------|-------|
| **Distribution** | Publish to Maven Central, Docker Hub, GHCR | Eric C. Mumford |
| **Community** | Recruit 5+ active contributors, establish governance | Eric C. Mumford |
| **Adoption** | 500+ Maven downloads, 100+ GitHub stars, 1-2 conference talks | Eric C. Mumford + volunteers |
| **Sustainability** | Release 1-2 minor versions per quarter, sub-7-day PR review | Eric C. Mumford + core maintainers |

---

## The Strategy at a Glance

### Phase 1: Foundation (Weeks 1-4)
```
Setup Maven Central account, GitHub Discussions, governance model
↓ Deliverables: Account approved, publishing config ready, community infrastructure
```

### Phase 2: Maven Central Release (Weeks 5-8)
```
Publish v0.13.0 to Maven Central, verify discoverability
↓ Deliverables: Artifact published, Maven coordinates: org.hti5250j:hti5250j:0.13.0
```

### Phase 3: Docker & CI/CD (Weeks 9-12)
```
Publish Docker images to Docker Hub + GHCR, create k8s examples
↓ Deliverables: heymumford/hti5250j:latest, docker-compose, k8s StatefulSet examples
```

### Phase 4: Content & Community (Weeks 13-20)
```
Write blog posts, recruit first contributors, submit conference proposals
↓ Deliverables: 2-3 blog posts, 5+ good-first-issue items, 1-2 proposals submitted
```

### Phase 5: GraalVM Native (Weeks 21+, deferred to v1.0.0)
```
Native image support for lightweight deployments, Homebrew formula
↓ Deliverables: hti5250j binary (no JVM), brew install hti5250j
```

---

## Target Personas & Channels

### Persona #1: IBM i QA Engineers (Primary)
- **Pain:** Manual 5250 testing, regression burden, expensive tools
- **Solution:** YAML-based automation, session pooling, free and open
- **Reach:** COMMON Conference (3000+ attendees), WRKGRP forums
- **Year 1 Goal:** 50-100 active QA engineers using HTI5250J

### Persona #2: Enterprise Java Developers (Emerging)
- **Pain:** IBM i integration in modern microservices, no headless library
- **Solution:** Maven Central + Spring Boot integration, type-safe APIs
- **Reach:** r/java (400K+ subscribers), JavaOne, Java Weekly newsletter
- **Year 1 Goal:** 50-100 Maven Central downloads

### Persona #3: DevOps/SRE Teams (Growing)
- **Pain:** Legacy system integration in k8s, no health checks, manual sessions
- **Solution:** Docker images, k8s manifests, health checks, CI/CD integration
- **Reach:** DevOps Subreddit, Kubernetes community, DevOps Days
- **Year 1 Goal:** 200+ Docker Hub pulls, 5-10 documented deployments

### Persona #4: Open Source Contributors (Community)
- **Pain:** Fork maintenance burden, protocol knowledge gap, governance unclear
- **Solution:** Clear GOVERNANCE.md, good-first-issue tagging, architecture docs
- **Reach:** GitHub Trending, Hacker News, Java Weekly
- **Year 1 Goal:** 5-10 active contributors, 3+ core maintainers recruited

---

## Key Distribution Channels (Priority Order)

### Tier 1: Critical (Weeks 1-8)
1. **Maven Central Repository** - 80% of enterprise Java developers
2. **GitHub Discussions** - Community hub, issue triage, RFCs

### Tier 2: High-Impact (Weeks 9-12)
3. **Docker Hub + GHCR** - Containerized deployments, DevOps reach
4. **Conference Presentations** - COMMON, JavaOne, DevOps Days
5. **Blog Posts** - Medium, personal blog, syndication (DZone, Dev.to)

### Tier 3: Community Growth (Ongoing)
6. **GitHub Trending** - Visibility to developer community
7. **Social Media** - Twitter, LinkedIn, Reddit (r/java, r/ibm)
8. **Mailing List** - Newsletter for subscribers (optional, Year 1+)

### Tier 4: Future (Post-v1.0.0)
9. **SDKMAN** - Command-line tool version management
10. **GraalVM Native Image** - Lightweight binary distributions
11. **Dual-Licensing** - Commercial support option (if needed)

---

## License Strategy

### Current: Pure GPL-2.0-or-later
- ✓ Aligns with original TN5250J
- ✓ Protects open-source community
- ✓ Prevents proprietary forks without contribution

### Adoption Barriers & Solutions
| Barrier | Solution |
|---------|----------|
| "GPL = can't use in commercial software" | Document: Using as library = no GPL obligations |
| "GPL = must open-source my code" | Create FAQ explaining derivative work definition |
| "GPL = too restrictive" | Highlight 20+ year TN5250J precedent |

### Future Option: Dual-Licensing (Post-v1.0.0)
If enterprise adoption plateaus, consider:
- Contributor License Agreement (CLA) enabling dual-licensing
- GPL-2.0 for open-source projects (free)
- Commercial license for proprietary applications (€50-200/year)
- Expected impact: €50-200K/year revenue (optional, not required)

---

## Governance Model

### Maintainer (Now)
- Eric C. Mumford (@heymumford)
- 8-10 hours/week commitment
- Final decision authority (with consensus preference)

### Core Maintainers (Target: Q4 2026)
- 2-3 people (IBM i expert, Java async expert, testing lead)
- Code review, architecture guidance, release coordination
- Recruited from active contributors (5+ PRs, 50+ hours, 6+ months)

### Committers (Target: Q4 2026)
- 3-5 people (detailed code review, test coverage focus)
- No merge authority (but fast-track to core maintainer)
- Recruited from consistent contributors (3+ PRs, 30+ hours)

### Decision-Making
- **Trivial** (bug fixes, docs) → Maintainer decides
- **Small** (features, refactoring) → 1 review + approval
- **Medium** (API changes) → 2 reviews + consensus
- **Large** (breaking changes, license) → RFC + community input (2 weeks)

---

## Success Metrics (12-month targets)

### Distribution
- [ ] Maven Central: 500+ downloads
- [ ] Docker Hub: 200+ pulls
- [ ] GitHub: 100+ stars, 10+ forks

### Community
- [ ] GitHub Discussions: 50+ posts
- [ ] Active contributors: 5+
- [ ] Issues closed: 30+
- [ ] PRs merged: 15+

### Technical
- [ ] Test pass rate: 99%+
- [ ] Code coverage: 82%+
- [ ] Security findings: 0 critical
- [ ] Release cadence: 1-2 per quarter

### Engagement
- [ ] Blog readers: 1000+
- [ ] Conference talks: 1-2 accepted
- [ ] Mailing list: 100+ subscribers

---

## Budget & Resources

### No-Cost (Already Available)
- GitHub (free tier for public repos)
- GitHub Actions (free for public repos)
- Maven Central (free for open-source)
- Docker Hub (free tier)
- Medium (free blogging)

### Recommended Spend (Year 1)
```
Optional Budget:
├── Conference booth (COMMON): $2,000-3,000
├── Domain (hti5250j.dev): $15/year
├── Newsletter platform (Mailchimp): $20/month
└── Total: ~$2,500-5,500 (mostly optional)
```

**Recommendation:** Start with zero spend (Phases 1-3). Add conference booth for Phase 4 (if attending COMMON).

---

## Risk Mitigation

| Risk | Mitigation | Fallback |
|------|-----------|----------|
| Maven Central approval delays | Start Week 1 (earliest) | Use GitHub Packages interim |
| No community interest | Focus on IBM i community first | Pivot to test automation angle |
| Maintainer burnout | Recruit core maintainers by Q2 | Reduce release cadence to 1x/6mo |
| Fork sync complexity | Document upstream divergence | Maintain separate branch if needed |

---

## Implementation Timeline

```
Feb 2026  |████████████|  Phase 1: Foundation (4 weeks)
          └─ Maven Central account, governance, community setup

Mar 2026  |████████████|  Phase 2: Maven Central Release (4 weeks)
          └─ v0.13.0 published, verified, announced

Apr 2026  |████████████|  Phase 3: Docker & CI/CD (4 weeks)
          └─ Docker images, k8s examples, GitHub Actions workflows

May 2026  |████████████████|  Phase 4: Content & Community (8 weeks)
          |  Blog posts, contributor recruitment, conference proposals
          ├── Q2 milestone: First contributors recruited
          └─ Conferences: COMMON (if accepted)

Aug 2026  └─ Phase 4 complete: Community foundation established

Q3 2026   Feature releases (v0.14.0, etc.)
Q4 2026   Target: v1.0.0 release (if major features completed)
```

---

## Quick Start Checklist

**Week 1 Actions:**
- [ ] Request Maven Central account (Sonatype JIRA)
- [ ] Create GitHub Discussions categories
- [ ] Draft v0.13.0 release notes

**Week 2 Actions:**
- [ ] Generate GPG keys for signing
- [ ] Update build.gradle with publishing config
- [ ] Create GitHub Actions workflow for Maven Central

**Week 3 Actions:**
- [ ] Create docs/LICENSE_FAQ.md
- [ ] Create CODE_OF_CONDUCT.md
- [ ] Update CONTRIBUTING.md

**Week 4 Actions:**
- [ ] Tag v0.13.0 (triggers Maven Central publish)
- [ ] Announce on GitHub + social media
- [ ] Plan Docker image strategy

See **IMPLEMENTATION_CHECKLIST.md** for detailed, week-by-week breakdown with task IDs.

---

## Documents Created

This strategy is documented across 5 new files:

1. **OPEN_SOURCE_STRATEGY.md** (52 pages)
   - Comprehensive 12-month plan
   - Distribution channels, community building, license strategy, packaging options
   - Detailed implementation roadmap (5 phases)

2. **GOVERNANCE.md** (25 pages)
   - Decision-making framework (trivial → large decisions)
   - Contributor tiers (contributor → committer → core maintainer)
   - Conflict resolution and conduct standards
   - Release process checklist

3. **IMPLEMENTATION_CHECKLIST.md** (20 pages)
   - Week-by-week tasks (Phases 1-6)
   - Success metrics and KPIs
   - Risk mitigation
   - Quick reference links

4. **MARKET_PERSONAS.md** (18 pages)
   - 4 target personas with pain points & channels
   - Market size estimation (3,500-10,500 addressable individuals)
   - Competitive landscape analysis
   - Content calendar (Q1-Q4 2026)

5. **STRATEGY_SUMMARY.md** (This document)
   - Executive overview
   - Key sections at a glance
   - Quick start checklist
   - Timeline visualization

---

## Questions to Discuss

1. **Maven Central Naming:** Use `org.hti5250j` or `com.heymumford`?
   - Recommendation: `org.hti5250j` (standard for open-source)

2. **Conference Budget:** Is $2K-3K COMMON sponsorship feasible?
   - Recommendation: Defer to Phase 4 (Q2), decide based on progress

3. **Dual-Licensing Timeline:** Consider CLA now or wait until v1.0.0?
   - Recommendation: Wait until v1.0.0 (not needed for community growth)

4. **Core Maintainer Recruitment:** Start now or after Phase 2?
   - Recommendation: Identify candidates in Phase 3; recruit in Phase 4

5. **GraalVM Native Image:** Essential for Year 1, or nice-to-have?
   - Recommendation: Nice-to-have; defer to v1.0.0 (focus on core library)

---

## Success Looks Like (12 months from now)

**February 2027:**
- ✓ HTI5250J published to Maven Central (500+ downloads)
- ✓ Docker images available (200+ pulls)
- ✓ GitHub: 100+ stars, 10+ forks, 5+ active contributors
- ✓ Community: 50+ Discussions posts, 1-2 conference talks accepted
- ✓ Release: v0.13.0, v0.14.0, v1.0.0 (or v0.15.0+)
- ✓ Governance: 2-3 core maintainers, 3-5 committers established
- ✓ Adoption: First case studies + documented production deployments
- ✓ Impact: QA engineers automating 5250 tests, Java developers integrating IBM i

---

## What's Next

**Start:** Week of February 10, 2026

1. **This week:** Request Maven Central account, create Discussions categories
2. **Next week:** Configure Gradle publishing, generate GPG keys
3. **Week 3:** Write LICENSE FAQ, update contributing guide
4. **Week 4:** Tag v0.13.0, trigger Maven Central publish, announce release

**For ongoing:** Review IMPLEMENTATION_CHECKLIST.md weekly; update progress

---

## Contact & Support

- **GitHub Issues:** https://github.com/heymumford/hti5250j/issues
- **GitHub Discussions:** https://github.com/heymumford/hti5250j/discussions
- **Email:** ericmumford@outlook.com
- **Twitter:** @heymumford (project updates)

---

## Document Metadata

**Status:** Ready for implementation
**Version:** 1.0
**Date Created:** February 2026
**Last Updated:** February 2026
**Review Date:** May 2026 (post-Maven Central launch)

**How to use this document:**
- Share with stakeholders (e.g., community, employers, mentors)
- Reference when explaining project vision
- Link from README.md under "Community & Contributing"
- Update quarterly with progress metrics

---

**The strategy is solid. The plan is clear. The community is ready.**

Now: Execute Phase 1, publish to Maven Central, build momentum.

---

*For detailed implementation guidance, see:*
- *IMPLEMENTATION_CHECKLIST.md* (week-by-week tasks)
- *OPEN_SOURCE_STRATEGY.md* (comprehensive strategy)
- *GOVERNANCE.md* (decision-making framework)
- *MARKET_PERSONAS.md* (community growth strategy)
