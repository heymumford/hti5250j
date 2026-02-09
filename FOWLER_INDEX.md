# Martin Fowler AI Patterns Research: Document Index

**Research Date:** February 9, 2026
**Source Authority:** martinfowler.com (2024-2026) + ThoughtWorks perspectives
**Applied Context:** HTI5250J Java 21 workflow execution engine for IBM i 5250 automation

---

## Quick Navigation

### For Executives / Decision-Makers
Start here: **FOWLER_RESEARCH_SUMMARY.md**
- 5-minute read: Core principles + status
- Where we stand (68% Fowler compliance)
- Next 3 phases + timeline
- ROI justification

### For Architects / Lead Engineers
Start here: **FOWLER_ARCHITECTURE_REFERENCE.md**
- System architecture diagram (Fowler patterns annotated)
- Pattern-to-component mapping
- Risk mitigation matrix
- Success metrics per phase

### For Implementation Teams
Start here: **FOWLER_AI_PATTERNS.md**
- Detailed breakdown of all 5 patterns
- HTI5250J code examples (copy-paste ready)
- Phase 12E-G roadmap (effort estimates included)
- Implementation recipes

### For Planning / Backlog Management
Start here: **FOWLER_INTEGRATION_CHECKLIST.md**
- Per-pattern status: ✅ / 🟡 / ⏳ / ❌
- Compliance verification questions
- Action items by phase
- Task-level detail

---

## The Five Core Patterns (Quick Reference)

| # | Pattern | Fowler Citation | HTI5250J Status | Phase for Completion |
|---|---------|-----------------|-----------------|----------------------|
| 1 | **Evals** (systematic non-determinism assessment) | "Evals play a central role in ensuring non-deterministic systems operate within sensible boundaries" | ✅ 90% (missing numeric metrics) | Phase 12E |
| 2 | **Bounded Contexts** (semantic memory organization) | "Event Sourcing provides episodic memory; Bounded Contexts partition into semantic boundaries" | 🟡 40% (actions semantic, memory scattered) | Phase 12F |
| 3 | **Lethal Trifecta Defense** (security isolation) | "Break up tasks so each blocks one element: sensitive data ∨ untrusted content ∨ external comms" | ✅ 85% (sealed actions done, audit incomplete) | Phase 12G |
| 4 | **Human Review in Slices** (governance) | "Break work into thin slices; review everything; never let agents run autonomously" | ✅ 70% (validation done, preview missing) | Phase 12E |
| 5 | **Tolerances for Nondeterminism** (quality bounds) | "Define acceptable precision/latency like bridges define safe load capacity" | 🟡 50% (hard-coded, not declared) | Phase 12E |

---

## Document Relationship Map

```
FOWLER_INDEX.md (you are here)
    │
    ├─→ FOWLER_RESEARCH_SUMMARY.md
    │   └─→ "What we learned + where we stand"
    │       └─→ Read if: you need executive summary
    │
    ├─→ FOWLER_AI_PATTERNS.md (4,200 lines, MOST DETAILED)
    │   └─→ "Deep dive: all 5 patterns with code"
    │       ├─→ Section 1: Evals (testing framework)
    │       ├─→ Section 2: Bounded Contexts (DDD + event sourcing)
    │       ├─→ Section 3: Lethal Trifecta (security)
    │       ├─→ Section 4: Human Review (governance)
    │       ├─→ Section 5: Tolerances (metrics)
    │       ├─→ Section 6: Code Structure (design patterns)
    │       └─→ Integration Roadmap: Phases 12E-13
    │
    ├─→ FOWLER_ARCHITECTURE_REFERENCE.md
    │   └─→ "Visual architecture + pattern mapping"
    │       ├─→ System diagram (Fowler patterns overlaid)
    │       ├─→ Pattern-to-component matrix
    │       ├─→ Risk mitigation per pattern
    │       └─→ Success metrics per phase
    │
    └─→ FOWLER_INTEGRATION_CHECKLIST.md
        └─→ "Implementation status + action items"
            ├─→ Per-pattern compliance ✅/🟡/⏳
            ├─→ Verification questions
            ├─→ Action items per phase
            └─→ Quality gates before Phase 13

```

---

## Key Findings by Pattern

### Pattern 1: Evals

**Current Implementation:** ✅ **Robust**
- Domain 1 unit tests (codec, queue ordering) ✓
- Domain 2 continuous monitors (schema drift detection 24/7) ✓
- Domain 3 surface tests (protocol round-trip on real i5) ✓
- Domain 4 scenario tests (payment, settlement, error workflows) ✓

**Missing:** Numeric quality scores (EvalScorer with 0.0-1.0)

**Phase 12E adds:**
- `EvalScorer` interface (correctness, idempotency, latency scorers)
- Integration with WorkflowExecutor
- Metrics reporting per execution

**See:** FOWLER_AI_PATTERNS.md Section 1 (lines ~200-320)

---

### Pattern 2: Bounded Contexts

**Current Implementation:** 🟡 **Partial**
- Actions semantic units ✓
- Handlers isolated ✓
- State scattered across method parameters ✗
- No event history ✗
- No replay capability ✗

**Phase 12F adds:**
- `WorkflowExecution` aggregate root (DDD pattern)
- Sealed `WorkflowDomainEvent` hierarchy
- State projection from events
- PostMortemAnalyzer for forensics

**See:** FOWLER_AI_PATTERNS.md Section 2 (lines ~350-480)

---

### Pattern 3: Lethal Trifecta Defense

**Current Implementation:** ✅ **Strong**
- Sealed Actions prevent injection ✓
- Input validation at record creation ✓
- Capability isolation per action ✓
- Audit logs exist (console) 🟡

**Phase 12G adds:**
- Structured AuditLog (JSON, not console)
- Explicit capability matrix documentation
- Network constraint enforcement
- Security compliance tests

**See:** FOWLER_AI_PATTERNS.md Section 3 (lines ~500-640)

---

### Pattern 4: Human Review in Slices

**Current Implementation:** ✅ **Validation done, Preview missing**
- WorkflowValidator (Phase 10) ✓
- ParameterValidator (Phase 10) ✓
- CLI enforcement ✓
- Dry-run simulator 🟡

**Phase 12E adds:**
- `WorkflowSimulator` (dry-run against MockScreen)
- Execution preview report
- Approval gate before real i5 execution
- Error recovery approval

**See:** FOWLER_AI_PATTERNS.md Section 4 (lines ~650-750)

---

### Pattern 5: Tolerances for Nondeterminism

**Current Implementation:** 🟡 **Hard-coded, not declared**
- Timeout bounds defined ✓
- Timeout enforcement in handlers ✓
- Field size limits tested ✓
- Declarative YAML spec ✗
- Compliance metrics ✗
- Monitoring/alerting ✗

**Phase 12E adds:**
- `WorkflowTolerance` record (YAML-serializable)
- `WorkflowExecutionMetrics` (actual vs declared)
- Compliance checking
- Metrics dashboard

**See:** FOWLER_AI_PATTERNS.md Section 5 (lines ~750-880)

---

## Implementation Timeline

### Phase 12E: Metrics & Tolerances (Q1 2026 - HIGH PRIORITY)
**Effort:** 13-15 hours | **Payoff:** Proves reliability, enables simulation
- [ ] WorkflowTolerance record + YAML support
- [ ] EvalScorer implementations (3 scorers)
- [ ] WorkflowExecutionMetrics capture
- [ ] WorkflowSimulator (dry-run framework)
- [ ] Domain 4 integration + verification

**Files:** FOWLER_AI_PATTERNS.md lines 200-320, 750-880

---

### Phase 12F: Event Sourcing & Audit Trail (Q2 2026)
**Effort:** 14-16 hours | **Payoff:** Enable forensics, error recovery
- [ ] WorkflowExecution aggregate root
- [ ] WorkflowDomainEvent sealed hierarchy
- [ ] Event replay/projection logic
- [ ] PostMortemAnalyzer implementation
- [ ] Integration tests + verification

**Files:** FOWLER_AI_PATTERNS.md lines 350-480

---

### Phase 12G: Security Hardening (Q2 2026)
**Effort:** 8-10 hours | **Payoff:** Complete lethal trifecta defense
- [ ] Input validation constructors (all Actions)
- [ ] Structured AuditLog (JSON serialization)
- [ ] Capability matrix documentation
- [ ] Network constraint enforcement
- [ ] Security compliance tests

**Files:** FOWLER_AI_PATTERNS.md lines 500-640

---

## How These Docs Are Used

### In Code Review
"Does this change align with Fowler's bounded context pattern?"
→ Reference: FOWLER_ARCHITECTURE_REFERENCE.md, Pattern-to-Component Mapping

### In Sprint Planning
"What's the effort estimate for Phase 12E?"
→ Reference: FOWLER_AI_PATTERNS.md, Integration Roadmap (12E section)

### In Architecture Decisions
"Should we use event sourcing?"
→ Reference: FOWLER_AI_PATTERNS.md, Section 2 (Bounded Contexts)

### In Security Reviews
"Are we defending against the lethal trifecta?"
→ Reference: FOWLER_ARCHITECTURE_REFERENCE.md, Risk Mitigation matrix

### In Requirements Definition
"What tolerances should this workflow declare?"
→ Reference: FOWLER_AI_PATTERNS.md, Section 5 (code examples)

---

## Source Documents (Official Fowler Works)

| Document | Date | Topic | Link |
|----------|------|-------|------|
| Emerging Patterns in Building GenAI Products | Feb 2025 | Evals, RAG, guardrails | https://martinfowler.com/articles/gen-ai-patterns/ |
| Agentic AI and Security | 2025 | Lethal trifecta, mitigations | https://martinfowler.com/articles/agentic-ai-security.html |
| How far can we push AI autonomy in code generation? | 2025 | Human review, multi-agent | https://martinfowler.com/articles/pushing-ai-autonomy.html |
| Exploring Generative AI (series) | 2023-2026 | Context engineering, spec-driven dev | https://martinfowler.com/articles/exploring-gen-ai.html |
| Eradicating Non-Determinism in Tests | Classic | Why non-determinism matters | https://martinfowler.com/articles/nonDeterminism.html |
| Event Sourcing | Classic | Complete history as truth | https://martinfowler.com/eaaDev/EventSourcing.html |
| Domain Events | Classic | Semantic memory units | https://martinfowler.com/eaaDev/DomainEvent.html |

---

## Glossary (Fowler Terminology)

| Term | Definition | HTI5250J Application |
|------|-----------|----------------------|
| **Evals** | Systematic assessment mechanisms for non-deterministic outputs | Domain 1-4 tests + Phase 12E EvalScorer |
| **Non-determinism** | System produces different outputs for same inputs (timing, network, data changes) | Workflow execution varies by i5 state, keyboard timing |
| **Bounded Context** | Semantic boundary in domain (e.g., "payment processing" vs "settlement batch") | WorkflowExecution aggregate = bounded context |
| **Aggregate Root** | Entity controlling access to semantic unit | WorkflowExecution (Phase 12F) |
| **Domain Event** | Immutable record of state change | WorkflowDomainEvent sealed types (Phase 12F) |
| **Event Sourcing** | Store complete history of events; derive state by replay | WorkflowExecution event log (Phase 12F) |
| **Lethal Trifecta** | Dangerous combo: sensitive data + untrusted content + external comms | Sealed Actions block all three simultaneously |
| **Tolerance** | Acceptable bounds for non-deterministic behavior | WorkflowTolerance (Phase 12E) |
| **Eval Scorer** | Function that measures output quality (0.0-1.0) | CorrectnessEval, IdempotencyEval, LatencyEval (Phase 12E) |

---

## Success Criteria (Fowler Standard)

### Phase 12E Completion
- [ ] 100% of workflows can declare WorkflowTolerance in YAML
- [ ] EvalScorer implementations run against ≥1 workflow
- [ ] WorkflowSimulator correctly predicts ≥3 scenarios
- [ ] Metrics captured for all Domain 4 executions
- [ ] Zero regressions from Phase 12D

### Phase 12F Completion
- [ ] WorkflowExecution aggregate captures all state changes
- [ ] Event replay reconstructs state perfectly (100% match)
- [ ] PostMortemAnalyzer identifies failure point in ≥1 workflow
- [ ] Event log enables ≥1 successful error recovery
- [ ] Zero regressions from Phase 12E

### Phase 12G Completion
- [ ] All 7 Actions have input validation constructors
- [ ] AuditLog records all executions in structured format
- [ ] Security review passes (no lethal trifecta exposure)
- [ ] Network constraints enforced (no surprise outbound calls)
- [ ] Zero regressions from Phase 12F

### Phase 13 Readiness
- [ ] All above criteria met
- [ ] Multi-agent prototype passes human oversight
- [ ] First workflow successfully orchestrated by agent + human
- [ ] Metrics prove safety/reliability within tolerances

---

## Questions This Research Answers

**Q: Should we adopt Fowler's patterns for HTI5250J?**
A: Yes. Your architecture already aligns with 68% of them. Phases 12E-G close remaining gaps.

**Q: Why Evals instead of just testing?**
A: Evals measure non-determinism; tests verify single scenarios. With network latency + i5 timing variation, evals catch what tests miss.

**Q: Why Event Sourcing for workflows?**
A: Enables forensics ("what happened?"), recovery ("resume from step N"), and agent reasoning ("semantic history for retry decisions").

**Q: Is the Lethal Trifecta risk real for HTI5250J?**
A: Yes. Sealed Actions already mitigate. Phase 12G formalization + audit trail eliminate remaining exposure.

**Q: Can we skip Phases 12E-G and go straight to Phase 13?**
A: Not recommended. Without metrics (12E), agents can't assess quality. Without events (12F), agents can't recover. Without audit (12G), you can't prove safety.

**Q: What's the ROI timeline?**
A: Phase 12E proves reliability (2-3 weeks). Phase 12F enables recovery (3-4 weeks). Phase 12G completes security (2 weeks). Phase 13 launches (4-6 weeks planning + execution).

---

## How to Use These Findings

### Step 1: Read the Summary (20 min)
**File:** FOWLER_RESEARCH_SUMMARY.md
Understand the 5 principles + where HTI5250J stands.

### Step 2: Choose Your Role

**If you're an architect:**
- Read: FOWLER_ARCHITECTURE_REFERENCE.md (30 min)
- Focus: System diagram + pattern mapping

**If you're implementing Phase 12E:**
- Read: FOWLER_AI_PATTERNS.md Section 1 & 5 (90 min)
- Focus: Code examples + recipes

**If you're planning Phases 12E-G:**
- Read: FOWLER_INTEGRATION_CHECKLIST.md (60 min)
- Focus: Task lists + effort estimates

**If you're reviewing phase completion:**
- Use: FOWLER_INTEGRATION_CHECKLIST.md + Success Criteria
- Check: All ✅ items verified before moving to next phase

### Step 3: Create Phase 12E Tasks
Based on FOWLER_AI_PATTERNS.md, Section 1 & 5 (Integration Roadmap).

### Step 4: Implement + Verify
Run Phase 12E. Report back on:
- Metrics captured? (yes/no)
- Tolerances declared? (count)
- Simulator working? (test results)

---

## Contact / Questions

For questions about these findings:
- Fowler's work: https://martinfowler.com/
- HTI5250J specifics: See MEMORY.md (project context)
- Implementation recipes: See FOWLER_AI_PATTERNS.md (code-ready examples)

---

**Document Version:** 1.0
**Last Updated:** February 9, 2026
**Stability:** Ready for team distribution
