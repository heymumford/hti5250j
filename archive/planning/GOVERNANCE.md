# HTI5250J Governance Model

**Effective:** February 2026
**Status:** Draft (community input welcome)
**Location:** GitHub Discussions #governance

---

## 1. Core Principles

1. **Transparency** - Decisions made publicly (GitHub Discussions or Issues)
2. **Meritocracy** - Influence earned through contributions, not affiliation
3. **Inclusivity** - Welcome contributors of all experience levels
4. **Sustainability** - Maintainer workload must be manageable long-term
5. **Community-first** - User/contributor needs drive priorities

---

## 2. Project Roles

### 2.1 Maintainer

**Role:** Eric C. Mumford (@heymumford)

**Responsibilities:**
- Release decisions (timing, scope, breaking changes)
- Security vulnerability response
- Architecture reviews for major features
- Triage high-priority issues
- Represent project in public forums (conferences, social media)
- Mentor core maintainers (future)

**Authority:**
- Final decision-maker on code (Linus Torvalds model with consensus preference)
- Can merge PRs without additional review if urgent
- Can revert changes that violate project values

**Workload:** 8-10 hours/week (sustainable pace)

---

### 2.2 Core Maintainers (Future Tier)

**Target:** 2-3 people by Q4 2026

**Responsibilities:**
- Code review (GitHub PRs)
- Issue triage (priority, assignment)
- Release coordination (changelog, testing)
- Architecture guidance (design discussions)

**Authority:**
- Approve and merge PRs in their domain
- Can close/re-open issues
- Can assign issues and PRs
- No unilateral decision-making on breaking changes

**Recruitment criteria:**
- 5+ merged PRs or 50+ hours contributed
- Deep expertise in one domain (protocol, session pooling, testing, etc.)
- Active for 6+ months
- Alignment with project values

---

### 2.3 Committers (Tier 2)

**Target:** 3-5 people by Q4 2026

**Responsibilities:**
- Code review (detail-focused)
- Test coverage for new features
- Documentation feedback
- Community engagement (Discussions, Issues)

**Authority:**
- Cannot merge PRs (requires core maintainer review)
- Can request changes on PRs
- Can close issues (if duplicate/off-topic)

**Recruitment:** Active contributors with consistent presence (3+ PRs, 30+ hours)

---

### 2.4 Contributors

**Everyone with merged PR(s)**

**Responsibilities:**
- Submit code via PR
- Participate in discussions
- Report bugs with reproduction steps
- Help answer questions (Discussions)

**Authority:**
- Push to feature branches
- Open issues and PRs
- Comment on all discussions

---

## 3. Decision-Making Framework

### 3.1 Decision Types

| Type | Authority | Process | Timeline |
|------|-----------|---------|----------|
| **Trivial** | Maintainer | Direct decision | Immediate |
| **Small** | Maintainer + 1 review | PR review + approval | 3-7 days |
| **Medium** | Maintainer + 2 reviews | Issue discussion + consensus | 7-14 days |
| **Large** | Community RFC + vote | GitHub Discussions (2 weeks) | 14-21 days |

### 3.2 Trivial Decisions

**Examples:**
- Bug fixes (non-breaking)
- Documentation typos
- Test improvements
- Dependency patches

**Process:**
```
1. Maintainer reviews PR (~1 day)
2. Maintainer approves + merges (or requests changes)
3. No additional review needed
```

---

### 3.3 Small Decisions

**Examples:**
- New backward-compatible feature
- Refactoring (internal only)
- Build/test infrastructure
- Low-risk API additions

**Process:**
```
1. Open issue with proposal
2. Discussion (comments, questions)
3. Submit PR (references issue)
4. Core maintainer reviews
5. If unclear: escalate to medium decision
6. Merge
```

---

### 3.4 Medium Decisions

**Examples:**
- Non-breaking API changes
- New subsystem (e.g., metrics, logging)
- Significant refactoring
- Major test additions

**Process:**
```
1. Open GitHub Issue with detailed proposal
2. Request feedback from relevant committers
3. Allow 7 days for discussion
4. Reach consensus (3/3 reviewers agree)
5. Implement via PR
6. Merge after PR review
```

---

### 3.5 Large Decisions (RFC Process)

**Examples:**
- Breaking API changes
- Major architectural shifts
- Dual-licensing or legal changes
- Governance changes
- Significant scope shifts (e.g., adding GUI)

**Process:**

**Step 1: RFC (Request for Comments)**
```markdown
# RFC: [Title]

## Summary
One-paragraph summary of decision.

## Motivation
Why is this needed? What problem does it solve?

## Proposal
Detailed technical proposal with examples.

## Alternatives Considered
What other approaches were ruled out?

## Impact Assessment
- Breaking changes? (Y/N)
- Performance implications?
- Maintenance burden?
- Community consensus needed?

## Timeline
When would this be implemented?

## Questions for Community
What feedback do you have?
```

**Step 2: Community Input (2 weeks)**
- Post in GitHub Discussions: #governance or #features
- Committers + maintainers provide feedback
- Open to all, but committer opinions weighted higher
- Maintainer may request additional research/prototyping

**Step 3: Maintainer Decision**
```
1. Collect feedback (2 weeks)
2. Synthesize community input
3. Maintainer decides:
   - Approved (move to implementation)
   - Approved with modifications (update RFC)
   - Deferred (not now, revisit)
   - Rejected (clear explanation)
4. Post decision in RFC thread
5. Archive approved RFCs in docs/rfcs/
```

**Example:** RFC for dual-licensing (future decision)

---

## 4. Conflict Resolution

### 4.1 Code Review Disagreements

**Scenario:** Committer requests changes; contributor disagrees

**Process:**
```
1. Committer + Contributor discuss in PR comments
2. If unresolved after 3 exchanges: escalate to core maintainer
3. Core maintainer makes final call
4. Decision documented in PR
```

**Principle:** "Disagree, but commit" - once decided, move forward

---

### 4.2 Scope Disputes

**Scenario:** Should feature X be in HTI5250J or a separate library?

**Process:**
```
1. Open GitHub Issue describing proposal
2. Discuss in Discussions #architecture
3. Core maintainers provide guidance (2-3 days)
4. Decision recorded
```

**Heuristic:**
- Protocol-level? (Yes → include)
- Session management? (Yes → include)
- Infrastructure? (Maybe → external library)
- Application-specific? (No → external library)

---

### 4.3 Contributor Disagreement with Maintainer

**Scenario:** Contributor believes maintainer decision is wrong

**Process:**
```
1. Respectfully voice concern (Discussions or PR comment)
2. Ask for clarification on decision rationale
3. If unsatisfied: propose discussion with committers (3-5 opinions)
4. If still unresolved: accept decision (majority rule)
5. Option: Fork and maintain separately (always permitted under GPL)
```

**Principle:** Maintainer is not infallible; decisions can be revisited with new evidence

---

## 5. Conduct & Values

### 5.1 Code of Conduct

HTI5250J adopts the **Contributor Covenant 2.1** (https://www.contributor-covenant.org/version/2/1/)

**Core commitments:**
- Be respectful of differing opinions
- Give and gracefully accept constructive feedback
- Focus on what is best for the community
- Show empathy towards other community members

**Unacceptable behavior:**
- Harassment (sexual, racist, exclusionary language)
- Personal attacks or insults
- Spam or off-topic discussions
- Doxxing or publishing private information

**Reporting:** GitHub Discussions (private message to @heymumford)

---

### 5.2 Project Values

1. **Reliability** - Code must be well-tested (80%+ coverage minimum)
2. **Simplicity** - APIs should be obvious; prefer fewer features over complexity
3. **Compatibility** - Backward compatibility unless breaking change is justified by RFC
4. **Attribution** - Preserve upstream TN5250J credit; give contributor credit
5. **Openness** - GPL-2.0 ensures derivative works remain free

---

## 6. Voting & Consensus

### 6.1 When Voting Is Used

- Large RFCs (major architectural changes)
- Governance changes (role creation, process changes)
- License changes (hypothetical dual-licensing decision)

**Not used for:** Regular PRs, small features, bug fixes

---

### 6.2 Voting Process

**Voting group:** Maintainer + core maintainers (+ committers if not consensus-seeking)

**Vote types:**
```
+1  = Approve (I agree and will help implement)
±0  = Neutral (Don't have strong opinion)
-1  = Veto (Don't agree, provide reasoning)
```

**Decision threshold:**
- Approve if: ≥2/3 are +1 and 0 vetos
- Veto is respectable; veto-er must suggest alternative
- If 1 veto: committers discuss; maintainer decides

**Example voting scenario:**
```
RFC: Dual-license HTI5250J under GPL-2.0 and commercial license

Votes:
- @heymumford:     +1 (helps long-term sustainability)
- @coredev1:        +1 (excited about commercial support)
- @coredev2:       -1 (worry: GPL purity message gets muddied)

Result: Approved (2 +1, 1 -1, but veto-er convinced)
Action: Implement with coredev2 as co-maintainer of commercial licensing
```

---

## 7. Release Process

### 7.1 Release Types

**Patch** (v0.12.1)
- Bug fixes only
- Backward-compatible
- Can release same week if urgent
- Minimal review needed

**Minor** (v0.13.0)
- New features + bug fixes
- Backward-compatible
- 4-6 week development cycle
- Requires core maintainer review

**Major** (v1.0.0)
- Breaking changes allowed
- Significant features
- 8-12 week development cycle
- RFC required for breaking changes

---

### 7.2 Release Checklist

```markdown
## Release v0.13.0 Checklist

### Planning Phase (Week 1)
- [ ] Define target features/fixes
- [ ] Create GitHub Milestone
- [ ] Announce target release date (Discussions)

### Development Phase (Weeks 2-6)
- [ ] Merge feature PRs (milestone filter)
- [ ] Run full test suite (99%+ pass rate required)
- [ ] Update documentation

### Stabilization Phase (Week 7)
- [ ] Create release-candidate branch: `release/0.13.0-rc1`
- [ ] Tag: git tag -a v0.13.0-rc1
- [ ] Request community testing (Discussions #releases)
- [ ] Fix reported issues

### Release Phase (Week 8)
- [ ] Merge RC fixes to main
- [ ] Final test run (./gradlew test)
- [ ] Update CHANGELOG.md
- [ ] Update gradle.properties to 0.13.0
- [ ] Commit: "Release v0.13.0"
- [ ] Tag: git tag -a v0.13.0 -m "Release v0.13.0"
- [ ] Push tags: git push --tags
- [ ] GitHub Actions builds + publishes (Maven Central, Docker)
- [ ] Create GitHub Release (auto-generated from tag)

### Announcement Phase
- [ ] Post release notes to GitHub Discussions
- [ ] Tweet from project account
- [ ] Email mailing list (if set up)
- [ ] Submit to Java Weekly, DZone
```

---

### 7.3 Hotfix Process

**For critical bugs only** (security, data loss, widespread breakage)

```markdown
## Hotfix v0.12.1 (from v0.12.0)

1. Create branch: git checkout -b hotfix/0.12.1
2. Fix bug with test
3. Update CHANGELOG.md
4. Bump patch version: 0.12.1
5. Tag: git tag -a v0.12.1
6. Push tag
7. GitHub Actions releases automatically
8. Post to Discussions (explain urgency)
```

---

## 8. Contributor Onboarding

### 8.1 Getting Involved

**Start here:**
1. Read README.md & CONTRIBUTING.md
2. Set up local environment (`./gradlew build`)
3. Run tests to verify (`./gradlew test`)
4. Browse GitHub Issues (filter by `good-first-issue`)

**Your first PR:**
- Start with small issue (1-5 tests, <50 lines code)
- Fork repo, create feature branch
- Run tests locally before pushing
- Submit PR with clear description
- Expect review feedback within 7 days

**After first PR:**
- You're now a contributor
- Eligible for committer promotion after 3 more PRs
- Ask questions in Discussions (no dumb questions)
- Help review others' PRs (earn trust)

---

### 8.2 Committer Promotion Criteria

Contributor becomes committer after:
- ✓ 5+ merged PRs
- ✓ 50+ hours contributed (estimated)
- ✓ Deep expertise in at least 1 domain (protocol, testing, session mgmt, etc.)
- ✓ Consistent participation for 6+ months
- ✓ Respectful interactions (no Code of Conduct violations)
- ✓ Nomination by maintainer or core maintainer
- ✓ Agreement from 2/3 of existing committers

**Promotion process:**
1. Maintainer drafts proposal (private discussion)
2. Existing committers vote (+1, ±0, -1)
3. If approved: public announcement (GitHub Discussions)
4. Grant GitHub permissions (write access to repo)

---

### 8.3 Core Maintainer Promotion

**Criteria (higher bar than committer):**
- ✓ 15+ merged PRs over 12+ months
- ✓ 200+ hours contributed
- ✓ Leadership in major feature or subsystem
- ✓ Architecture documentation contributions
- ✓ Mentored 1-2 new contributors
- ✓ Demonstrated judgment on major decisions
- ✓ Agreement from 3/3 existing core maintainers

---

## 9. Maintenance & Sustainability

### 9.1 Inactive Contributor Removal

**Policy:** After 12 months of inactivity, committer role lapses

```
Timeline:
- 3 months: Soft notice (email + Discussions mention)
- 6 months: Formal notice (GitHub issue, public)
- 12 months: Permission removal (with option to rejoin)
```

**Rationale:** Prevents stale permissions; committer roles stay active

---

### 9.2 Maintainer Succession

**Scenario:** Maintainer needs to step down

**Process:**
```
1. Announce intention 3 months in advance
2. Core maintainers discuss succession
3. Select 1 or 2 co-maintainers (consensus)
4. Transition period: shared decision-making (1 month)
5. Formal handoff: new maintainer takes lead
6. Previous maintainer available for consultation
```

**Principle:** Project > individual; smooth transitions ensure continuity

---

### 9.3 Maintainer Activity Expectations

**Minimum expected engagement:**
- Check GitHub at least 3x per week
- Review & respond to issues within 5 business days
- Merge PRs within 7 business days (acceptable 2-week turnaround if overwhelmed)
- Release 1-2 minor versions per quarter
- Attend to security vulnerabilities within 24 hours

**If unable to meet expectations:**
- Communicate in Discussions (personal circumstances)
- Recruit core maintainer to help
- Consider step-down if long-term

---

## 10. Governance Evolution

### 10.1 When to Revisit Governance

- After core maintainer recruitment (Q4 2026)
- If contributor count exceeds 15 active people
- If major conflicts emerge
- If significant scope changes (e.g., GUI addition)

---

### 10.2 Governance Changes

**Any governance change requires RFC** (see section 3.5)

**Examples:**
- Create committer tier
- Remove inactive maintainers
- Change voting thresholds
- Establish steering committee

---

## 11. Communication Channels

### 11.1 GitHub-Native (Primary)

- **Issues:** Bug reports, feature requests
- **Pull Requests:** Code review, discussion
- **Discussions:** Announcements, Q&A, RFC, community chat
- **Releases:** Version announcements

### 11.2 Secondary

- **Twitter:** Release announcements, tips
- **Blog:** Deep dives, quarterly updates
- **Mailing list:** For announcement subscribers (optional, Year 1+)

---

## 12. Document Management

**Governance-related docs:**
- This file: `GOVERNANCE.md` (source of truth)
- Contributing guide: `CONTRIBUTING.md` (contributor entry point)
- Code of Conduct: `CODE_OF_CONDUCT.md` (adoption of Contributor Covenant)
- RFCs: `docs/rfcs/` (archived major decisions)

**Updates to governance:** Open PR to `GOVERNANCE.md`, link to discussions

---

## 13. FAQ

### Q: How do I become a core maintainer?
**A:** Contribute actively (15+ PRs, 200+ hours, 12+ months), demonstrate expertise, and be nominated by existing maintainers. See section 8.3.

### Q: Can I use HTI5250J in commercial software?
**A:** Yes. See LICENSE_FAQ.md for details. GPL-2.0 only requires source disclosure if you modify + distribute.

### Q: What if I disagree with a maintainer decision?
**A:** Respectfully voice your concern. If unresolved, ask for a committer discussion. If still unresolved, you can fork (always permitted under GPL).

### Q: How often are releases made?
**A:** Target 1-2 minor releases per quarter + hotfixes as needed. See section 7.1.

### Q: Can I suggest a breaking change?
**A:** Yes, via RFC (GitHub Discussions #governance). Breaking changes require strong justification and community consensus.

---

## Document Metadata

**Version:** 1.0
**Effective:** February 2026
**Author:** Eric C. Mumford (@heymumford)
**Status:** Open for feedback (GitHub Discussions #governance)
**Next review:** October 2026

Questions? Open discussion in [GitHub Discussions](https://github.com/heymumford/hti5250j/discussions/new)
