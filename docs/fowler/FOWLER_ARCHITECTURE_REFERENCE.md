# Martin Fowler Patterns: HTI5250J Architecture Reference

**Quick reference:** Which Fowler patterns apply to which components in your system.

---

## System Architecture (Fowler Patterns Annotated)

```
┌─────────────────────────────────────────────────────────────────┐
│                        User/Agent                               │
│                    (Human or AI Phase 13+)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  WorkflowCLI (Main Entry)                       │
│  Phase 10: ValidationResult (Human Review in Slices ✓)         │
│  Phase 12E: WorkflowSimulator (Preview before execution ⏳)     │
│  Phase 12E: Approval Gate (Required human click)               │
└────────────────────────┬────────────────────────────────────────┘
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
    ┌─────────────────────┐  ┌────────────────────┐
    │  WorkflowValidator  │  │ WorkflowSimulator  │
    │  (Phase 10) ✓       │  │ (Phase 12E) ⏳     │
    │                     │  │                    │
    │ • Structural check  │  │ Against MockScreen │
    │ • Action-specific   │  │ Dry-run execution  │
    │ • Parameter binding │  │ Preview: "will it  │
    └─────────────────────┘  │ work?" → human     │
                             │ approval           │
                             └────────────────────┘
              │                     │
              └──────────┬──────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                 WorkflowExecutor (Orchestrator)                 │
│  Phase 12D: Sealed Actions (Lethal Trifecta ✓)                 │
│  Phase 12E: EvalScorer (Evals ⏳)                               │
│  Phase 12E: Tolerances (Nondeterminism Bounds ⏳)               │
│  Phase 12F: Event Emission (Bounded Contexts ⏳)                │
│  Phase 12G: AuditLog (Lethal Trifecta ✓ + hardening)           │
└────────────────────────┬────────────────────────────────────────┘
         ┌──────────────┼──────────────┬────────────┐
         ▼              ▼              ▼            ▼
    ┌────────┐   ┌──────────┐  ┌─────────┐  ┌────────────┐
    │ Events │   │ Metrics  │  │ Evals   │  │ AuditLog   │
    │ (12F)  │   │ (12E)    │  │ (12E)   │  │ (12G)      │
    │ ⏳     │   │ ⏳       │  │ ⏳      │  │ ⏳         │
    │        │   │          │  │         │  │            │
    │Bounded │   │Tolerance │  │ Correct │  │ Who did    │
    │Context │   │Compliance│  │ness,    │  │ what when  │
    │Memory  │   │Dashboard │  │Idempot. │  │ & why      │
    └────────┘   └──────────┘  └─────────┘  └────────────┘
         │              │              │            │
         └──────────────┼──────────────┴────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Action Handlers (Execution)                    │
│                                                                 │
│  ┌─────────────┐ ┌──────────────┐ ┌──────────┐ ┌────────────┐ │
│  │ LoginAction │ │NavigateAction│ │FillAction│ │SubmitAction│ │
│  │ (12D ✓)     │ │ (12D ✓)      │ │(12D ✓)   │ │ (12D ✓)    │ │
│  │             │ │              │ │          │ │            │ │
│  │Credentials  │ │Keystroke seq │ │Field map │ │ AID key    │ │
│  │validation ✓ │ │validation ✓  │ │validity ✓│ │ enums ✓    │ │
│  └─────────────┘ └──────────────┘ └──────────┘ └────────────┘ │
│                                                                 │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────┐              │
│  │AssertAction  │ │ CaptureAction│ │WaitAction│              │
│  │ (12D ✓)      │ │ (12D ✓)      │ │(12D ✓)   │              │
│  │              │ │              │ │          │              │
│  │Text expected │ │Artifact write│ │ Timeout  │              │
│  │ OR screen OK │ │ only ✓       │ │ bounds ✓ │              │
│  └──────────────┘ └──────────────┘ └──────────┘              │
│                                                                 │
│  Sealed Interface Prevents Injection (Lethal Trifecta ✓)      │
│  Each Action Pinned to Specific Task (Capability Isolation ✓) │
└────────────────────────┬────────────────────────────────────────┘
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
    ┌──────────────────────┐   ┌────────────────────┐
    │  SessionInterface    │   │   Screen5250       │
    │  (Mocked in tests)   │   │   (Real i5 APIs)   │
    │  ✓ Phase 1-11        │   │   ✓ Phase 1-11     │
    │                      │   │                    │
    │ • connect/disconnect │   │ • getScreenAsChars │
    │ • sendKeys           │   │ • getOIA (keyboard)│
    │ • listeners          │   │ • hasUnfilledField │
    └──────────────────────┘   └────────────────────┘
              │                     │
              └──────────┬──────────┘
                         ▼
            ┌────────────────────────┐
            │   IBM i 5250 System    │
            │                        │
            │ Real Data Changes State│
            └────────────────────────┘
```

---

## Pattern-to-Component Mapping

### Pattern 1: Evals (Systematic Assessment)

**Components:**
- Domain 1: Unit tests (Phase 1) ✓
- Domain 2: Continuous contracts (Phase 2) ✓
- Domain 3: Surface tests (Phase 6) ✓
- Domain 4: Scenario tests (Phase 8) ✓
- EvalScorer (Phase 12E) ⏳

**Trust it to:**
- Detect non-determinism (network latency, i5 timing)
- Measure execution quality (numeric scores 0.0-1.0)
- Prevent regressions (evals fail if behavior changes)

---

### Pattern 2: Bounded Contexts (Memory Architecture)

**Current (Partial):**
- Actions (Phase 12D) ✓ - Semantic boundaries per action type
- Handlers - Isolated behavior per action

**Missing:**
- WorkflowExecution aggregate root (Phase 12F) ⏳
- WorkflowDomainEvent hierarchy (Phase 12F) ⏳
- State projection/replay (Phase 12F) ⏳

**Once Complete (Phase 12F):**
- Agents see semantic history, not raw logs
- "Why are we in this state?" question answerable
- Errors recoverable via replay + branching

---

### Pattern 3: Lethal Trifecta Defense (Security)

**Element 1: Sensitive Data**
- ✓ Credentials isolated to LoginAction only
- ✓ No credentials logged to console
- ⏳ Phase 12G: Audit log records access

**Element 2: Untrusted Content**
- ✓ Sealed Actions prevent unknown instruction injection
- ✓ Input validation at record construction
- ⏳ Phase 12G: Allowlist constraints formalized

**Element 3: External Communication**
- ✓ CaptureAction only writes to ./artifacts/ (no network)
- ✓ No outbound API calls in handlers
- ⏳ Phase 12G: Network constraints documented

**Defense Layer:**
- ✓ Sealed type compiler enforcement (can't subclass Action)
- ✓ Record immutability (can't mutate after creation)
- 🟡 Audit trail exists (console), not structured (Phase 12G)

---

### Pattern 4: Human Review in Slices (Governance)

**Slice 1: Validation**
- ✓ Phase 10: WorkflowValidator blocks invalid YAML
- ✓ Phase 10: User fixes errors, revalidates
- ✓ CLI enforces: no execution if validation fails

**Slice 2: Preview** (Before Real Execution)
- ⏳ Phase 12E: WorkflowSimulator dry-runs against MockScreen
- ⏳ Phase 12E: Report: "Step 0 LOGIN → connect ✓"
- ⏳ Phase 12E: Approval gate: "Proceed?"

**Slice 3: Execution Review**
- ✓ Phase 11: Handlers execute with defined semantics
- ✓ Phase 12D: Sealed Actions prevent surprise behavior
- ⏳ Phase 12F: Event log enables "what actually happened?"

**Slice 4: Error Recovery Review**
- ⏳ Phase 12E: If step fails → simulation suggests fallback
- ⏳ Phase 12E: Human approves retry strategy
- ⏳ Phase 12F: Event replay shows "where it diverged"

---

### Pattern 5: Tolerances for Nondeterminism (Metrics)

**Currently Defined (Hard-coded):**
- Phase 11: `DEFAULT_KEYBOARD_UNLOCK_TIMEOUT = 30s`
- Phase 11: `DEFAULT_KEYBOARD_LOCK_CYCLE_TIMEOUT = 5s`
- Phase 11: `FIELD_FILL_TIMEOUT = 500ms`
- Phase 6: Field size limits (no truncation)
- Phase 8: Decimal precision (no rounding loss)

**Needed (Declarative):**
- Phase 12E: `WorkflowTolerance` record in YAML
  ```yaml
  tolerances:
    keyboardUnlockMaxLatency: 30s
    screenRefreshMaxLatency: 5s
    maxDecimalPlaces: 2
    maxRetryAttempts: 3
    maxTotalExecutionTime: 5m
  ```

**Metrics Collection (Phase 12E):**
- Track actual vs declared tolerances
- Report compliance: "Within tolerance: YES/NO"
- Alert on drift: "Keyboard unlock exceeded 35s"

---

### Pattern 6: Code Structure (Maintainability)

**Present (✓):**

| Pattern | Example | Benefit |
|---------|---------|---------|
| Sealed types | `sealed Action permits LoginAction, ...` | Compiler prevents missing handlers |
| Records | `record LoginAction(host, user, password)` | Immutability, no state bugs |
| Pattern matching | `switch(action) { case LoginAction la -> ... }` | No instanceof, type-safe |
| SRP | ArgumentParser, SessionFactory, TerminalAdapter | Each class: one reason to change |
| DI | `WorkflowRunner(SessionInterface, ScreenProvider)` | Testable, swappable components |

**Recommended (Phase 12E+):**

| Pattern | Example | Benefit |
|---------|---------|---------|
| Value objects | `Screen5250Content wraps char[]` | Semantic queries, readable |
| Strategy pattern | `RetryStrategy` interface | Configurable retry policies |
| Builder pattern | `WorkflowExecutorBuilder` | Clear intent, fewer parameter bugs |

---

## Integration Path: Phases 12E → 13

```
Phase 12E: Metrics & Tolerances
├─ WorkflowTolerance (YAML spec)
├─ WorkflowExecutionMetrics (actual vs declared)
├─ EvalScorer (correctness/idempotency/latency)
├─ WorkflowSimulator (dry-run preview)
└─ Result: Execution is measurable & reviewable

Phase 12F: Event Sourcing & Audit Trail
├─ WorkflowExecution aggregate root
├─ WorkflowDomainEvent hierarchy
├─ Event replay/projection logic
├─ PostMortemAnalyzer (where did it fail?)
└─ Result: Complete history, recovery possible

Phase 12G: Security Hardening
├─ Input validation constructors (all Actions)
├─ Structured AuditLog (JSON, not console)
├─ Capability matrix (per action constraints)
├─ Network allowlist (no surprise calls)
└─ Result: Lethal trifecta defense complete

Phase 13: Multi-Agent Coordination
├─ Agents read WorkflowExecution aggregate (semantic memory)
├─ Agents propose Actions (Sealed type safety)
├─ Agents use EvalScorer for self-assessment
├─ Agents submit to human review (approval gate)
└─ Result: Safe human-AI workflow orchestration
```

---

## Risk Mitigation: What Each Pattern Prevents

| Risk | Pattern | How HTI5250J Mitigates |
|------|---------|------------------------|
| **Silent data loss** | Evals + Bounded Contexts | Domain 3/4 tests catch truncation; event log shows where loss occurred |
| **Protocol mismatch** | Evals (Domain 2) | 24/7 contract monitors detect schema drift in real i5 |
| **Workflow injection** | Lethal Trifecta | Sealed Actions prevent unknown instruction types |
| **Unrecoverable failure** | Event Sourcing | Complete history enables replay from recovery point |
| **Untrustworthy execution** | Tolerances | Metrics prove execution stayed within SLA bounds |
| **Unmaintainable code** | Structure patterns | SRP, sealed types, records → agents can reason about it |

---

## Success Metrics (Fowler Standard)

### Phase 12E Completion
- [ ] WorkflowTolerance defined in ≥1 workflow YAML
- [ ] ≥3 EvalScorer implementations (correctness/idempotency/latency)
- [ ] WorkflowSimulator runs full workflow without i5 interaction
- [ ] All Domain 4 scenarios run through evals + pass
- [ ] Execution metrics captured + logged to artifacts

### Phase 12F Completion
- [ ] WorkflowExecution aggregate created + sealed
- [ ] ≥7 WorkflowDomainEvent types defined
- [ ] Event replay reconstructs state perfectly
- [ ] PostMortemAnalyzer identifies failure point
- [ ] ≥1 failed workflow successfully replayed

### Phase 12G Completion
- [ ] All 7 Actions have input validation constructors
- [ ] Structured AuditLog with JSON serialization
- [ ] Capability matrix documented (per action constraints)
- [ ] Network constraint tests pass (no surprise outbound)
- [ ] Security review passed (no lethal trifecta exposure)

---

## Conclusion

Your HTI5250J architecture **already implements 50% of Fowler's patterns** through Phases 1-12D. The next step is Phase 12E to add:

1. **Numeric quality metrics** (Evals) - Prove reliability
2. **Configurable tolerances** (Nondeterminism) - Define acceptable bounds
3. **Dry-run simulation** (Human Review) - Preview before commit
4. **Event sourcing** (Phase 12F) - Enable recovery + forensics

This unlocks Phase 13 (multi-agent coordination) where AI or human agents can safely orchestrate workflows with confidence in:
- **Safety:** Sealed Actions prevent injection
- **Reliability:** Metrics prove quality within bounds
- **Recoverability:** Complete history enables resume
- **Auditability:** Audit trail answers "what happened?"

Start with Phase 12E to establish the metrics foundation.
