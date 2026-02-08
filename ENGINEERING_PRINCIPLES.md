# Engineering Principles

| Field | Value |
| --- | --- |
| Scope | Cross-cutting engineering principles for HTI5250J |
| Applies to | Code, tests, and documentation |
| Last updated | 2026-02-07 |

## Principles

### Code Is Evidence

Code must make falsifiable claims about system behavior. A reader must be able to answer:

1. What does this code claim to do?
2. How would we know if that claim is false?
3. Which IBM i behaviors could invalidate the claim?

### Prefer Explicitness

Implicit behavior creates knowledge debt. Make intent explicit in names, contracts, and comments. Avoid hidden conventions and undocumented heuristics.

### Three-Way Contract

HTI5250J sits between client expectations and IBM i reality:

```
┌──────────────────┐
│   Client Code    │  ← What test automation expects
└────────┬─────────┘
         │ "What are you promising?"
┌────────▼──────────────────────┐
│ HTI5250J (Translation Layer)  │  ← Bridge implementation
└────────┬──────────────────────┘
         │ "Is that what i5 actually does?"
┌────────▼──────────────────────┐
│  IBM i (PMTENT/LNINQ/etc.)    │  ← Concrete runtime behavior
└───────────────────────────────┘
```

Responsibilities:
- Client-facing code is semantic and intent-focused.
- Bridge code is explicit about assumptions and transformations.
- IBM i-facing code is auditable against protocol behavior.

### Determinism and Evidence

Determinism is a requirement, not a preference. Every run should be reproducible from code, data, and environment configuration. Failures must preserve evidence, not discard it.

### Maintainability

Prefer small, cohesive units that are verifiable in a single sitting. One file should have one reason to change. When a file grows past 400 lines, either justify the exception or split it.

## Headless-First

The platform targets headless execution as the default mode. GUI components are optional and must not be required for core behaviors.

## Testing Philosophy

Tests are executable specifications and must reflect IBM i reality rather than mocks. The canonical test architecture lives in `TEST_ARCHITECTURE.md`.

## Covenant

We will:
- Build for clarity over cleverness.
- Make assumptions explicit and testable.
- Fail loudly with evidence.
- Preserve reproducibility and migration paths.

We will not:
- Hide behavior behind framework magic.
- Depend on opaque tooling for core workflows.
- Trade short-term convenience for long-term control.

## Related Documents

- `CODING_STANDARDS.md`
- `TEST_ARCHITECTURE.md`
- `REQUIREMENTS.md`
