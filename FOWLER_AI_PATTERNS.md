# Martin Fowler's AI Architecture Patterns Applied to HTI5250J

**Research Date:** February 2026
**Source Research:** martinfowler.com GenAI articles + ThoughtWorks Perspectives
**Researcher:** Claude Code (Haiku 4.5)
**Context:** Java 21 workflow execution engine for IBM i 5250 automation

---

## Executive Summary

Martin Fowler's recent work on GenAI production patterns (Feb 2025) establishes five core principles directly applicable to HTI5250J's workflow execution architecture:

| Principle | Fowler Foundation | HTI5250J Application | Status |
|-----------|------------------|----------------------|--------|
| **Evals over Assumptions** | Systematic assessment of non-determinism | Contract tests (Phase 1) + Domain 4 scenario verification | ✅ Implemented |
| **Bounded Contexts for Memory** | DDD aggregates as semantic units | WorkflowExecutor + SessionContext isolation | 🔄 Phase 12E |
| **Lethal Trifecta Defense** | Container + isolation + task decomposition | Sealed Actions + handler isolation | ✅ Phase 12D |
| **Human Review in Slices** | Break work into reviewable units | Workflow validation before execution | ✅ Phase 10 |
| **Tolerances for Nondeterminism** | Define acceptable precision/latency | Timeout bounds + error recovery specs | ✅ Phase 11 |

---

## 1. EVALS: Assessing Non-Determinism Through Systematic Testing

### Fowler's Core Principle

From **"Emerging Patterns in Building GenAI Products"** (Feb 2025):

> "As software products using generative AI move from proof-of-concepts into production systems, common patterns are being uncovered. **Evals play a central role in ensuring that non-deterministic systems are operating within sensible boundaries.**"

Evals use three assessment mechanisms:
- **LLM-as-judge**: Ask another LLM to evaluate outputs
- **Human evaluation**: Domain experts verify correctness
- **Automated scorers**: Rule-based verification (deterministic checks)

### HTI5250J Context: Why Evals Matter

HTI5250J's workflow execution is inherently non-deterministic:
- **IBM i responses vary** by data, time, system load, code page
- **Network timing** introduces latency variation (100ms-5000ms)
- **Keyboard lock/unlock** has variable duration
- **Screen parsing** depends on field ordering in i5 response

### Current HTI5250J Implementation

Your **4-domain test architecture** (Phase 1-8) IS your eval system:

| Domain | Mechanism | Eval Type | Frequency |
|--------|-----------|-----------|-----------|
| **Domain 1** | Unit tests (EBCDIC codec, queue order) | Automated scorer | Pre-commit |
| **Domain 2** | Continuous contracts (24/7 monitoring) | LLM-as-judge equivalent | Every 5-30 min |
| **Domain 3** | Surface tests (protocol round-trip, field bounds) | Human + automated | Per-commit |
| **Domain 4** | Scenario tests (payment workflow, error recovery) | Automated verifier | Per-commit + nightly |

### Recommended Extension: Eval Scorers for Phase 12+

**Goal:** Quantify "workflow execution quality" before production deployment.

**Three Eval Metrics:**

1. **Correctness Scorer** (automated)
   ```java
   // Example: Verify payment processing didn't lose decimal places
   class PaymentProcessingEval implements EvalScorer {
       public double score(WorkflowExecution execution) {
           List<Payment> transactions = execution.getCapturedTransactions();
           long totalCents = transactions.stream()
               .mapToLong(p -> p.amountInCents())
               .sum();

           // Expect: no truncation, exact match to input
           return (totalCents == expectedTotalCents) ? 1.0 : 0.0;
       }
   }
   ```

2. **Idempotency Scorer** (automated)
   ```java
   // Verify: same workflow twice produces identical result
   class IdempotencyEval implements EvalScorer {
       public double score(WorkflowExecution exec1, WorkflowExecution exec2) {
           String screen1 = exec1.getScreenDump();
           String screen2 = exec2.getScreenDump();
           return screen1.equals(screen2) ? 1.0 : 0.5;  // 0.5 if minor differences
       }
   }
   ```

3. **Latency Scorer** (deterministic bounds)
   ```java
   // Verify: execution stays within SLA bounds
   class LatencyEval implements EvalScorer {
       public double score(WorkflowExecution execution) {
           Duration duration = execution.getTotalDuration();
           if (duration.toMillis() < 5000) return 1.0;      // Excellent
           if (duration.toMillis() < 10000) return 0.8;     // Good
           if (duration.toMillis() < 30000) return 0.5;     // Acceptable
           return 0.0;                                       // SLA breach
       }
   }
   ```

### Implementation Path (Phase 12E Candidate)

1. Create `EvalScorer` interface with three implementations
2. Add eval runner to WorkflowExecutor:
   ```java
   public record EvalResult(
       String workflowName,
       double correctnessScore,      // 0.0-1.0
       double idempotencyScore,      // 0.0-1.0
       double latencyScore,          // 0.0-1.0
       double overallScore           // weighted avg
   ) {}
   ```
3. Integrate into Domain 4 scenario tests (measure, don't just verify)

---

## 2. BOUNDED CONTEXTS FOR AGENT MEMORY: DDD Applied to Workflow State

### Fowler's Core Principle

From **search synthesis** across Fowler's DDD works:

> "Event Sourcing provides **episodic memory** for AI agents. Bounded Contexts from Domain-Driven Design partition memory into **semantic boundaries**, with Aggregates functioning as cohesive memory clusters for reliable agent behavior."

**Key Insight:** Agents need structured memory organized by business domain, not technical layers.

**DDD Components:**
- **Bounded Context**: Semantic boundary (e.g., "PaymentProcessing" vs "SettlementBatch")
- **Aggregate Root**: Cohesive entity that controls access (e.g., Order aggregate contains OrderItems)
- **Domain Events**: Memory of state changes (e.g., PaymentProcessed, TransactionFailed)
- **Event Sourcing**: Complete history replayed to reconstruct state

### HTI5250J Context: Current State Management

Your Phase 11 implementation uses **scattered state**:

```java
// Current: State scattered across method parameters and context
public void handleFill(FillAction action, SessionInterface session, ...) {
    // State: action fields, session screens, keyboard lock status
    // Problem: No unified memory of "why we're in this state"
}
```

**Problem:** When a fill fails, you don't have the semantic history:
- Was this field required or optional?
- Did we attempt it before (and fail)?
- What was the screen state when failure occurred?

### Recommended Design: Bounded Context for Workflows

**Goal:** Organize workflow memory by semantic boundaries, not layers.

**Structure:**

```java
// ===== BOUNDED CONTEXT: Workflow Execution =====

// Aggregate Root (controls all access to workflow state)
public record WorkflowExecution(
    String workflowId,                    // Unique identifier
    List<DomainEvent> events,             // Complete history (Event Sourcing)
    WorkflowExecutionState currentState,  // Derived from replaying events
    Instant startTime,
    Instant? completionTime
) {

    // Only way to change state: through domain events
    public void recordStepStarted(StepIndex index, StepDef step) {
        events.add(new StepStartedEvent(index, step, Instant.now()));
    }

    public void recordStepCompleted(StepIndex index, ScreenContent screen) {
        events.add(new StepCompletedEvent(index, screen, Instant.now()));
    }

    public void recordStepFailed(StepIndex index, Exception error) {
        events.add(new StepFailedEvent(index, error.getMessage(), Instant.now()));
    }
}

// Domain Events (immutable record of what happened)
sealed interface WorkflowDomainEvent permits
    StepStartedEvent,
    StepCompletedEvent,
    StepFailedEvent,
    KeyboardStateChangedEvent,
    ScreenCapturedEvent
{
    Instant occurredAt();
}

record StepStartedEvent(
    int stepIndex,
    StepDef step,
    Instant occurredAt
) implements WorkflowDomainEvent {}

record StepCompletedEvent(
    int stepIndex,
    ScreenContent screen,
    Instant occurredAt
) implements WorkflowDomainEvent {}

record StepFailedEvent(
    int stepIndex,
    String errorMessage,
    Instant occurredAt
) implements WorkflowDomainEvent {}

// State derived from events (projection)
public record WorkflowExecutionState(
    int currentStepIndex,
    StepStatus lastStepStatus,           // STARTED, COMPLETED, FAILED
    ScreenContent lastScreenContent,
    Map<Integer, StepOutcome> stepOutcomes,  // History of all steps
    List<String> errorLog                // All errors encountered
) {
    public static WorkflowExecutionState fromEvents(List<WorkflowDomainEvent> events) {
        // Replay all events to rebuild state
        int stepIndex = -1;
        Map<Integer, StepOutcome> outcomes = new HashMap<>();
        List<String> errors = new ArrayList<>();

        for (var event : events) {
            if (event instanceof StepStartedEvent ste) {
                stepIndex = ste.stepIndex();
                outcomes.put(stepIndex, new StepOutcome(StepStatus.STARTED, null, null));
            } else if (event instanceof StepCompletedEvent sce) {
                outcomes.put(sce.stepIndex(),
                    new StepOutcome(StepStatus.COMPLETED, sce.screen(), null));
            } else if (event instanceof StepFailedEvent sfe) {
                outcomes.put(sfe.stepIndex(),
                    new StepOutcome(StepStatus.FAILED, null, sfe.errorMessage()));
                errors.add(sfe.errorMessage());
            }
        }

        return new WorkflowExecutionState(
            stepIndex,
            outcomes.get(stepIndex).status(),
            outcomes.get(stepIndex).screen(),
            outcomes,
            errors
        );
    }
}

enum StepStatus { STARTED, COMPLETED, FAILED, TIMEOUT }

record StepOutcome(
    StepStatus status,
    ScreenContent screen,
    String? errorMessage
) {}
```

### Benefits for HTI5250J

| Benefit | How It Helps | Phase Impact |
|---------|-------------|--------------|
| **Replay-ability** | Reconstruct state at any point in workflow for debugging | Phase 12F: Post-mortem analysis |
| **Auditability** | Complete history for compliance + forensics | Phase 13: Enterprise features |
| **Error Recovery** | Know exactly where failure occurred, resume from there | Phase 12E: Advanced retry logic |
| **Testing** | Inject events to test error paths without i5 interaction | Phase 12G: Extended test coverage |
| **Memory for Agents** | When agents (Phase 13+) retry workflows, they see semantic history | Phase 13: Multi-agent coordination |

### Implementation Roadmap

**Phase 12E (Q1 2026):**
- [ ] Create WorkflowExecution aggregate root
- [ ] Define sealed WorkflowDomainEvent hierarchy
- [ ] Implement WorkflowExecutionState projection logic
- [ ] Refactor WorkflowRunner to record events instead of printing logs

**Phase 12F (Q2 2026):**
- [ ] Add event replay for post-mortem analysis
- [ ] Integrate with Domain 4 scenario tests (verify event sequences)

---

## 3. LETHAL TRIFECTA DEFENSE: Sealing Agent Capabilities

### Fowler's Core Principle

From **"Agentic AI and Security"** (2025):

> "The fundamental problem is that LLMs cannot reliably distinguish between data and instructions... The **Lethal Trifecta** consists of:
>
> 1. **Access to sensitive data** (credentials, tokens, source code)
> 2. **Exposure to untrusted content** (public issues, web pages, user input)
> 3. **Ability to externally communicate** (APIs, web requests, posts)
>
> When all three exist simultaneously, attackers can craft hidden instructions to extract and exfiltrate sensitive information."

**Mitigation:** "Run LLMs inside controlled containers and **break up tasks so that each sub-task blocks at least one element of the trifecta**. Above all do small steps that can be controlled and reviewed by humans."

### HTI5250J Context: Current Vulnerabilities

Your Phase 12D sealed Action types address this:

**Current state (pre-Phase 12D):**
```java
// OLD: StepDef contains everything (trifecta exposed)
record StepDef(
    String type,
    Map<String, String> params   // ← Could contain injected instructions
) {}

// Problem: Single handler with sprawling conditionals
if ("login".equals(type)) {
    // User could inject arbitrary code in params
}
```

**After Phase 12D (sealed Actions):**
```java
sealed interface Action permits LoginAction, NavigateAction, FillAction, ... {}

record LoginAction(
    String host,           // ← Pinned to login domain only
    String username,
    String password
) implements Action {}

// Compiler enforces: LoginAction can ONLY do login tasks
// Cannot inject "capture screenshot" or "send email"
```

### Security Properties Achieved

| Trifecta Element | How Sealed Actions Defend | Mechanism |
|------------------|--------------------------|-----------|
| **Sensitive Data** | Credentials isolated to LoginAction only | Sealed interface + record immutability |
| **Untrusted Content** | Each action white-lists its inputs | LoginAction rejects non-host parameters |
| **External Communication** | Only specific actions can communicate | CaptureAction (artifact write only), no outbound network |

### Recommended Hardening (Phase 12E+)

**1. Input Validation at Record Creation** (already done)
```java
record LoginAction(String host, String user, String password) implements Action {
    public LoginAction {
        Objects.requireNonNull(host, "host required");
        Objects.requireNonNull(user, "user required");
        Objects.requireNonNull(password, "password required");
        if (!isValidHostname(host))
            throw new IllegalArgumentException("Invalid host: " + host);
    }
}
```

**2. Capability Isolation** (implement per-action)
```java
// Example: CaptureAction can only write to artifacts directory
record CaptureAction(String name) implements Action {
    public CaptureAction {
        Objects.requireNonNull(name, "name required");
        // Validate name doesn't contain path traversal
        if (name.contains("..") || name.contains("/") || name.contains("\\"))
            throw new IllegalArgumentException("Invalid artifact name: " + name);
    }

    // Handler knows CaptureAction can ONLY write to ./artifacts/
    // No write to /etc/passwd, no network calls, no code execution
}
```

**3. Audit Trail** (record what actions executed)
```java
// WorkflowExecutor records all actions + results
public class AuditLog {
    void logActionExecution(
        Action action,           // WHAT was executed
        Duration duration,       // HOW LONG it took
        ScreenContent result,    // WHAT happened
        Optional<Exception> error // IF it failed
    ) {
        // Persist to artifacts directory
        // Enables forensics: "was this action supposed to run?"
    }
}
```

---

## 4. HUMAN REVIEW IN SLICES: Validation Before Execution

### Fowler's Core Principle

From **"How far can we push AI autonomy in code generation?"** (2025):

> "For a relatively simple application... AI is not ready to create and maintain a maintainable business software codebase without human oversight. The non-deterministic nature of LLMs means oversight remains essential for production software.
>
> **The key insight:** Break work into thin slices and review everything closely, treating every slice as a pull request from a dodgy collaborator who's very productive in the LOC sense."

**Concrete recommendation:** Never let agents run without human review between steps.

### HTI5250J Implementation: Phase 10 Validator

Your Phase 10 work is EXACTLY this pattern:

```java
// Phase 10: Validate workflow BEFORE execution
ValidationResult validate(WorkflowSchema workflow) {
    // Returns: errors + warnings, PREVENTS execution if errors exist

    // Checks all 7 action types for constraint violations
    // Checks parameter references match dataset
    // Checks timeout bounds (100-300000ms)

    // Example error: "Step 0 LOGIN missing host field"
    // Human reads error, fixes YAML, re-validates
}

// Then: Only after validation passes, execute
if (validationResult.isValid()) {
    executeWorkflow(workflow);  // Safe to proceed
}
```

### Quality of Review: Three-Tier Model

Your Phase 10 validator implements **three review tiers**:

| Tier | Check | Example | User Action |
|------|-------|---------|------------|
| **Structural** | Workflow well-formed | name present, steps non-empty | Auto-fix or accept |
| **Action-Specific** | Actions have required fields | LOGIN has host/user/password | Fix YAML manually |
| **Parameter** | References exist in dataset | ${data.accountId} exists in CSV | Supply data or skip |

### Recommendation: Add Simulation Tier (Phase 12E Candidate)

**Goal:** Preview workflow execution without touching i5.

```java
// New: Execute against mock i5 to catch logic errors
public class WorkflowSimulator {
    public SimulationResult simulate(
        WorkflowSchema workflow,
        DataProvider data,
        MockScreen mockI5
    ) {
        // Dry-run: execute all steps against mock
        // Returns: what WOULD happen if we ran this

        // Example output:
        // Step 0: LOGIN → connects to mock i5 ✓
        // Step 1: FILL → fills 3 fields → keyboard unlock ✓
        // Step 2: SUBMIT → submits request → waits for screen refresh ✓
        // Step 3: ASSERT → checks for "Payment confirmed" ✓
        // Result: Would succeed with mock data
    }
}

// Human review: "Does simulation output match intention?"
// If yes → approve → execute on real i5
// If no → fix workflow → re-simulate → re-review
```

---

## 5. TOLERANCES FOR NONDETERMINISM: Quality Metrics

### Fowler's Core Principle

From **Nondeterministic Computing** (2025):

> "LLMs are a form of nondeterministic computing... **What are the tolerances of nondeterminism that we have to deal with?**
>
> Just as we know how much weight a concrete bridge can take, so too should LLMs come with metrics describing the levels of precision they can support."

**Analogy:** A concrete bridge has:
- Safe load capacity: 10,000 tons ± 5% (acceptable variation)
- Max deflection: 2cm (failure threshold)
- If load > 10,500 tons or deflection > 2cm → alert

**For AI systems:** Define acceptable variation bounds.

### HTI5250J Context: Current Tolerance Definitions

Your Phase 11 implementation DOES set tolerances:

```java
// Phase 11: Timeout tolerances defined per action
public class WorkflowRunner {
    // Timeouts (tolerable variation in response time)
    static final Duration DEFAULT_KEYBOARD_UNLOCK_TIMEOUT = Duration.ofSeconds(30);
    static final Duration DEFAULT_KEYBOARD_LOCK_CYCLE_TIMEOUT = Duration.ofSeconds(5);
    static final Duration FIELD_FILL_TIMEOUT = Duration.ofMillis(500);

    // If i5 takes > 30s to unlock keyboard → fail (timeout exceeded tolerance)
}
```

### Recommended: Comprehensive Tolerance Specification (Phase 12E)

**Goal:** Document all quality thresholds explicitly.

```java
// New: Tolerance specification (machine-readable, human-verifiable)
public record WorkflowTolerance(
    // Latency tolerances
    Duration keyboardUnlockMaxLatency,    // default: 30s (LOGIN waits for connection)
    Duration screenRefreshMaxLatency,     // default: 5s (SUBMIT waits for screen update)
    Duration fieldFillInterval,           // default: 500ms (wait between fields)

    // Data tolerances
    int maxDecimalPlaces,                 // e.g., 2 for currency (no rounding loss)
    int maxStringTruncation,              // e.g., 0 for required fields, 10 for optional
    boolean allowPartialResults,          // e.g., false (all-or-nothing semantics)

    // Execution tolerances
    int maxRetryAttempts,                 // e.g., 3 (give up after 3 failures)
    Duration maxTotalExecutionTime,       // e.g., 5 minutes (SLA boundary)

    // Failure handling
    boolean requireHumanReviewOnFailure,  // e.g., true (don't auto-retry without review)
    List<String> allowedFallbacks         // e.g., ["skip_to_next_step", "capture_and_halt"]
) {

    // Validation: Ensure tolerances are consistent
    public void validate() {
        if (keyboardUnlockMaxLatency.toMillis() < 100)
            throw new IllegalArgumentException("keyboardUnlock timeout too short");
        if (maxRetryAttempts < 0)
            throw new IllegalArgumentException("maxRetries cannot be negative");
    }
}

// Usage: Each workflow declares its tolerances
public record WorkflowSchema(
    String name,
    List<StepDef> steps,
    WorkflowTolerance tolerances  // ← New field
) {}

// Example YAML:
/*
name: payment-processing
tolerances:
  keyboardUnlockMaxLatency: 30s
  screenRefreshMaxLatency: 5s
  maxDecimalPlaces: 2
  allowPartialResults: false
steps:
  - type: LOGIN
    ...
*/
```

### Quality Metrics Dashboard (Phase 12F Candidate)

**Goal:** Monitor if real execution stays within declared tolerances.

```java
public record WorkflowExecutionMetrics(
    String workflowId,
    Instant startTime,
    Instant endTime,

    // Latency metrics
    Duration actualKeyboardUnlockLatency,  // vs tolerance: 30s
    Duration actualScreenRefreshLatency,   // vs tolerance: 5s
    double actualLatencyPercentile99,      // vs tolerance: max total = 5min

    // Data metrics
    int actualMaxDecimalPlaces,            // vs tolerance: 2
    int actualMaxStringTruncation,         // vs tolerance: 0
    long bytesProcessed,

    // Reliability metrics
    int retryAttempts,                     // vs tolerance: 3
    boolean requiresHumanReview,           // vs tolerance: true
    Optional<Exception> finalError
) {
    // Compliance check
    public boolean withinTolerance(WorkflowTolerance tolerance) {
        return actualKeyboardUnlockLatency.compareTo(tolerance.keyboardUnlockMaxLatency()) <= 0
            && actualScreenRefreshLatency.compareTo(tolerance.screenRefreshMaxLatency()) <= 0
            && actualMaxDecimalPlaces <= tolerance.maxDecimalPlaces()
            && retryAttempts <= tolerance.maxRetryAttempts();
    }
}
```

---

## 6. CODE STRUCTURE: Design Patterns for AI-Integrated Systems

### Fowler's Recommendations

From **multiple sources**:

> "All signs so far show that if you have **well-constructed code, modular approaches**, and all the other good practices we tend to emphasize, that makes it much easier for the AI to work with and minimizes issues down the line."

**Key insight:** The better your code structure, the more reliably agents (AI or human) can reason about it.

### HTI5250J Alignment (Already Strong)

Your codebase ALREADY follows these patterns:

| Pattern | HTI5250J Implementation | Benefit |
|---------|------------------------|---------|
| **Sealed types** | Phase 12D: sealed Action interface | Compiler prevents missing handlers |
| **Records (immutable data)** | Phase 3: Rect record, Phase 12D: LoginAction record | No state mutation bugs |
| **Single Responsibility** | Phase 12A: ArgumentParser, SessionFactory, TerminalAdapter | Each class has ONE reason to change |
| **Dependency Injection** | WorkflowRunner(SessionInterface, Screen5250) | Testable, swappable components |
| **Explicit contracts** | Phase 1-8: Contract tests define API boundaries | No implicit behavior surprises |

### Recommended Additions (Phase 12E+)

**1. Strategy Pattern for Retry Logic** (currently missing)

```java
// Define retry strategies
@FunctionalInterface
public interface RetryStrategy {
    Duration delayBeforeRetry(int attemptNumber, Exception lastError);

    // Built-in strategies
    static RetryStrategy exponentialBackoff(Duration initialDelay, int maxAttempts) {
        return (attempt, error) -> {
            if (attempt >= maxAttempts) return Duration.ZERO;  // Stop retrying
            long delayMs = initialDelay.toMillis() * (1L << attempt);  // 2^attempt
            return Duration.ofMillis(delayMs);
        };
    }
}

// Apply to workflow execution
public class WorkflowRunner {
    private final RetryStrategy retryStrategy;

    public void executeStep(Action action, RetryStrategy strategy) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                execute(action);
                return;  // Success
            } catch (TransientException e) {
                Duration delay = strategy.delayBeforeRetry(attempt, e);
                if (delay.isZero()) throw e;  // Stop retrying
                Thread.sleep(delay.toMillis());
            }
        }
    }
}
```

**2. Value Object for Screen Content** (improves testability)

```java
// Current: char[] arrays passed around
void handleNavigate(char[] screenContent) { ... }

// Recommended: Value object with semantic methods
public record Screen5250Content(
    char[] rawContent,      // Original 80x24 matrix
    int cursorRow,
    int cursorCol,
    int keyboardStatus      // 0=unlocked, 1=locked, 2=system request
) {
    // Semantic queries (much easier for agents/humans to understand)
    public boolean containsText(String text) { ... }
    public boolean showsField(String fieldName) { ... }
    public boolean isKeyboardLocked() { ... }

    public String asFormattedText() {  // 80-column text for logging
        // Format rawContent as human-readable string
    }
}

// Now handlers are clearer
void handleNavigate(Screen5250Content screen) {
    if (screen.containsText("Enter Selection")) {
        // Semantic, not "char at position 42"
    }
}
```

**3. Builder Pattern for WorkflowExecutor** (reduces parameter explosions)

```java
// Before: Too many parameters
WorkflowExecutor executor = new WorkflowExecutor(
    sessionFactory, screenProvider, loggers, retryStrategy,
    timeoutConfig, dataProvider, artifactDirectory, ...
);

// After: Builder with clear intent
WorkflowExecutor executor = new WorkflowExecutorBuilder()
    .withSessionFactory(sessionFactory)
    .withScreenProvider(screenProvider)
    .withRetryStrategy(exponentialBackoff())
    .withTimeoutConfig(timeoutConfig)
    .withArtifactDirectory("./artifacts")
    .build();
```

---

## Integration Roadmap: Phases 12E-13

### Phase 12E: Metrics & Tolerances (Q1 2026)

**Goal:** Quantify "are we executing reliably?"

| Task | Fowler Pattern | Files | Effort |
|------|-----------------|-------|--------|
| Define WorkflowTolerance record | Tolerances for nondeterminism | 1 new | 2h |
| Add EvalScorer implementations | Evals over assumptions | 3 new | 4h |
| Refactor WorkflowRunner to record events | Bounded contexts + event sourcing | 1 modified | 3h |
| Add WorkflowSimulator (dry-run) | Human review in slices | 1 new | 4h |
| Document tolerance specs in CODING_STANDARDS.md | Quality metrics | 1 modified | 2h |

**Acceptance Criteria:**
- [ ] WorkflowExecutionMetrics captures all tolerances
- [ ] At least one workflow declares tolerances in YAML
- [ ] Domain 4 scenario tests verify tolerance compliance
- [ ] Simulator can dry-run a full workflow without i5 interaction

### Phase 12F: Event Sourcing & Audit Trail (Q2 2026)

**Goal:** "Where did we go wrong?" post-mortems enabled by complete history

| Task | Fowler Pattern | Files | Effort |
|------|-----------------|-------|--------|
| Create WorkflowExecution aggregate root | Bounded contexts + DDD | 1 new | 2h |
| Define sealed WorkflowDomainEvent hierarchy | Sealed types | 1 new | 3h |
| Implement event projection (replay logic) | Event sourcing | 1 new | 4h |
| Refactor WorkflowRunner to emit events | Event sourcing | 1 modified | 3h |
| Add PostMortemAnalyzer (replay events, find root cause) | DDD + audit trail | 1 new | 4h |

**Acceptance Criteria:**
- [ ] Complete workflow execution produces event log
- [ ] Replaying events from the beginning reconstructs final state exactly
- [ ] PostMortemAnalyzer can identify "where execution diverged from normal"
- [ ] Domain 4 tests verify event sequences

### Phase 12G: Advanced Security & Input Validation (Q2 2026)

**Goal:** Tighten lethal trifecta defenses

| Task | Fowler Pattern | Files | Effort |
|------|-----------------|-------|--------|
| Add input validation to all Action records | Lethal trifecta defense | 7 modified | 2h |
| Create AuditLog for action execution tracking | Audit trail | 1 new | 2h |
| Document capability matrix per Action type | Security by design | 1 new | 1h |
| Add container constraints for WorkflowRunner | Container isolation | 1 modified | 1h |

**Acceptance Criteria:**
- [ ] All Actions validate inputs at record construction
- [ ] AuditLog captures every action execution + result
- [ ] No unplanned information leakage possible (design review + tests)

### Phase 13: Multi-Agent Coordination (Q3 2026+)

**Goal:** Agents (AI or human) can safely orchestrate workflows

| Recommendation | Fowler Foundation | HTI5250J Application |
|---|---|---|
| Agents operate on Event Sourced state | Bounded contexts + event sourcing | Agents see complete history, not snapshots |
| Agents submit Actions for human review | Human review in slices | Review queue before execution |
| Each agent bounded to one context | Bounded contexts | Agent A = payment processing, Agent B = settlement |
| Agents use Evals to self-assess quality | Evals over assumptions | Agent runs EvalScorer after each step |

---

## Conclusion: Fowler's Five Principles in Practice

| Principle | How You're Doing | Recommended Next |
|-----------|-----------------|------------------|
| **Evals** | ✅ 4-domain test architecture | 🔄 Phase 12E: Add EvalScorer metrics |
| **Bounded Contexts** | 🟡 Partial (handler isolation good, memory scattered) | 🔄 Phase 12F: Event Sourcing + WorkflowExecution aggregate |
| **Lethal Trifecta** | ✅ Sealed Actions phase 12D | 🟡 Phase 12G: Harden input validation + audit trail |
| **Human Review** | ✅ Phase 10 validator | 🔄 Phase 12E: Add simulator for dry-run review |
| **Tolerances** | ✅ Timeout bounds defined | 🔄 Phase 12E: Comprehensive tolerance specification |

**Strategic recommendation:** Implement Phase 12E (Metrics & Tolerances) next. This bridges Fowler's quality framework to your production deployment readiness.

---

## Sources

- [Emerging Patterns in Building GenAI Products](https://martinfowler.com/articles/gen-ai-patterns/)
- [Agentic AI and Security](https://martinfowler.com/articles/agentic-ai-security.html)
- [How far can we push AI autonomy in code generation?](https://martinfowler.com/articles/pushing-ai-autonomy.html)
- [Exploring Generative AI (series)](https://martinfowler.com/articles/exploring-gen-ai.html)
- [Eradicating Non-Determinism in Tests](https://martinfowler.com/articles/nonDeterminism.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Domain Events](https://martinfowler.com/eaaDev/DomainEvent.html)
- Martin Fowler on Nondeterministic Computing ([The New Stack](https://thenewstack.io/martin-fowler-on-preparing-for-ais-nondeterministic-computing/))
