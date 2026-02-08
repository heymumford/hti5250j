# 5250 Automation Platform - Feature Set and Requirements

| Field | Value |
| --- | --- |
| Status | Draft - requirements baseline |
| Owner | Architecture |
| Last updated | 2026-02-07 |

## Summary

This document is the single source of truth for the headless 5250 automation platform feature set and requirements. The focus is deterministic, scalable, and auditable test automation for IBM i systems. While the initial application is mortgage and financial workflows, all interfaces and capabilities remain domain-agnostic and treat IBM i as a general system of record.

## Audience

- Product and platform architects
- Engineering leads and implementers
- Security and compliance reviewers
- QA and test automation owners

## Document Structure

- Part I: Overview and positioning
- Part II: Architecture and concepts
- Part III: Core automation requirements
- Part IV: Authoring and data requirements
- Part V: Execution and operations requirements
- Part VI: Extensibility and compatibility requirements
- Part VII: Governance and compliance requirements
- Part VIII: Use cases and benefits
- Part IX: Cross-cutting non-functional requirements
- Appendices: Glossary, traceability, standards, implementation mapping, MoSCoW checklist

## Conventions

- Requirement IDs use `R<Section>.<Number>` where Section matches the numbered heading.
- Requirements use `shall` for testable obligations.
- Priority uses `Must`, `Should`, `Could`.
- Applicability uses `All`, `Headless`, `EHLLAPI Adapter`, or `ACS Bridge`.
- `Current Implementation` cites file paths or tests and notes partial or missing coverage.
- References in Appendix E are informational unless marked normative.

## Scope

**In scope**
- Headless TN5250E sessions as the primary execution model
- EHLLAPI-compatible automation semantics for legacy interoperability
- Deterministic automation with synchronization, evidence, and data correlation
- Multi-language authoring with a canonical workflow IR
- CI/CD-ready runners with scale and standard artifacts

**Out of scope**
- GUI-only emulator dependency as a primary mode
- User-facing analytics or business dashboards
- General RPA features outside 5250 automation (OCR, browser automation)

## Assumptions

- IBM i version is not fixed; the platform targets currently supported releases and documents supported levels per release.
- Primary deployment is headless; GUI emulator dependency is optional and used only for legacy compatibility.
- ACS EHLLAPI bridge mode is optional and provided only for compatibility.

## Feature Set (Canonical)

- F1 Headless TN5250E Session Engine: Direct, headless 5250 connectivity and negotiation
- F2 Session Pooling and Isolation: Deterministic session lifecycle and per-test isolation
- F3 EHLLAPI Compatibility Adapter: EHLLAPI semantics over headless sessions
- F4 ACS EHLLAPI Bridge Adapter: Optional ACS bridge for legacy stacks
- F5 Deterministic Sync and Replay: Explicit synchronization and idempotent execution
- F6 Screen Intelligence and Field Semantics: Field-level operations and screen contracts
- F7 Workflow DSL + Canonical IR + SDKs: Multi-language authoring to a single IR
- F8 Test Data and State Control: Fixtures, data setup, and cross-layer correlation
- F9 CI/CD Runners and Scale: Repeatable execution at scale with standard artifacts
- F10 Observability and Evidence: Rich artifacts for debug and audit
- F11 Governance and Security: Security, compliance, and audit readiness
- F12 Extensibility APIs: Plugin and API extension points
- F13 Intelligent Authoring and Safe Discovery: Semantic path planning, screen graph, and safe spider mode

---

## Part I. Overview and Positioning

### 10) Platform Positioning and Standards Alignment

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R10.1 | The platform shall implement EHLLAPI semantics consistent with the presentation space model and function set. | F3 | Must | EHLLAPI Adapter | Not implemented (no EHLLAPI adapter in `src/`). |
| R10.2 | The platform shall implement synchronization that requires Wait plus Search Field or Search Presentation Space checks. | F5 | Must | All | TBD |
| R10.3 | The platform shall implement headless TN5250E negotiation consistent with telnet option and terminal type exchange. | F1 | Must | Headless | Partial: `src/org/hti5250j/framework/tn5250/tnvt.java`, `tests/org/hti5250j/TelnetNegotiationPairwiseTest.java`. |
| R10.4 | The platform shall align ACS compatibility to IBM documented EHLLAPI bridge requirements. | F4 | Should | ACS Bridge | Not implemented (no ACS bridge in `src/`). |

---

## Part II. Architecture and Concepts

### Concepts and Solutions
- Session is the unit of isolation and concurrency. Each session owns a presentation space and state.
- Presentation space is the authoritative screen buffer and the primary evidence surface.
- Screen Contracts define stable identifiers for screens using labels, attributes, and field structure.
- Workflow IR is the canonical representation of automation flows across all languages.
- Screen Graph captures observed screen transitions and action semantics for path planning.
- Safe Discovery (Spider Mode) traverses screens under explicit policies without data mutation.
- Evidence pipeline records screen snapshots, field maps, keystrokes, and host responses per step.
- Deterministic execution uses explicit synchronization and idempotent boundaries.
- System boundary, trust zones, and data flows are defined by Section 11 requirements.

### Platform Principles (Freedom, Portability, Verifiability)
1. **User Freedom Is First-Class**: Workflows must be inspectable and represented as readable text (DSL/IR). Artifacts must be exportable in standard formats (text, JSON, HTML). A run must be reproducible from code, data, and environment config without vendor dependency.
2. **Separation of Mechanism and Policy**: The engine provides mechanisms (TN5250 I/O, screen parsing, field semantics). Policy decisions (retry behavior, environment gating, data lifecycle rules, CI thresholds) are expressed via config, DSL constructs, or extension points. No hidden heuristics.
3. **Text Is the Universal Interface**: The canonical source of truth is text for workflows, screen contracts, and data fixtures. Any GUI is an optional view, editor, or visualizer, never the sole interface.
4. **No Hostage Interfaces**: Automation semantics must not bind to a single emulator, vendor object model, or language. EHLLAPI is a compatibility layer, not the conceptual core. Workflow IR enables multiple frontends (Python, Robot, YAML) and backends (headless TN5250, ACS bridge).
5. **Make Failure Visible**: No silent recovery and no best-guess screen matching. Errors are explicit, evidence is preserved, and failure modes are classified.
6. **Users Can Fork the Future**: Components such as the TN5250 engine, screen parser, and DSL frontend are replaceable without invalidating existing workflows. Stable contracts and versioned schemas are mandatory.
7. **Resist Convenience as Control**: Avoid opaque record-and-replay, auto-fix, or summarization that hides assumptions. Prefer raw screen data, field maps, and exact failing assertions.
8. **Freedom Includes Future You**: Preserve specs, diagrams, and text artifacts so the system is explainable and migratable without demos or proprietary tools.

---

## Part III. Core Automation Requirements

### 1) Connectivity and Session Lifecycle

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R1.1 | The platform shall enforce PSID values as single uppercase letters A-Z and return an explicit error for invalid or unavailable PSIDs. | F2, F3 | Must | EHLLAPI Adapter | Not implemented (no EHLLAPI adapter in `src/`). |
| R1.2 | The platform shall provide EHLLAPI-compatible connect, disconnect, and session discovery semantics with explicit status and metadata (host, port, device name, state). | F3 | Must | EHLLAPI Adapter | Not implemented (no EHLLAPI adapter in `src/`). |
| R1.3 | The platform shall provide EHLLAPI Reserve and Release semantics that are atomic and return explicit errors when already reserved. | F3 | Must | EHLLAPI Adapter | Not implemented (no EHLLAPI adapter in `src/`). |
| R1.4 | The platform shall implement headless TN5250E negotiation supporting terminal type, end-of-record, binary mode, and device name negotiation, and shall expose negotiated options. | F1 | Must | Headless | Partial: `src/org/hti5250j/framework/tn5250/tnvt.java`, `tests/org/hti5250j/TelnetNegotiationPairwiseTest.java`. |
| R1.5 | The platform shall provide optional ACS bridge mode with documented detection logic and PATH precedence rules. | F4 | Should | ACS Bridge | Not implemented (no ACS bridge in `src/`). |
| R1.6 | The platform shall support 64-bit callers through documented ACS bridge behavior and emit diagnostics when unsupported. | F4 | Should | ACS Bridge | Not implemented (no ACS bridge in `src/`). |
| R1.7 | The platform shall initialize ACS sessions via PCSAPI when ACS is not already running. | F4 | Should | ACS Bridge | Not implemented (no ACS bridge in `src/`). |
| R1.8 | The platform shall implement pooled session management with deterministic acquisition, configurable queue policy (default FIFO), and acquisition timeouts. | F2 | Must | All | TBD |

### 2) Interaction Model: Inputs, Synchronization, Determinism

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R2.1 | The platform shall implement Send Key semantics that accept literal text and bracketed mnemonics (for AID and function keys) with deterministic encoding. | F3, F5 | Must | All | Partial: `src/org/hti5250j/framework/tn5250/Screen5250.java` (`sendKeys`), `src/org/hti5250j/framework/tn5250/KeyStrokenizer.java`. |
| R2.2 | The platform shall reject or split keystroke sequences that contain multiple AID keys in a single step and record a validation error. | F5 | Must | All | Not implemented (no AID-sequence validation found). |
| R2.3 | The platform shall implement Wait semantics with explicit timeout parameters and return codes for XCLOCK and XSYSTEM conditions. | F5 | Must | All | TBD |
| R2.4 | The platform shall enforce completion checks that combine Wait with Search Field or Search Presentation Space checks. | F5 | Must | All | TBD |
| R2.5 | The platform shall gate input on OIA keyboard lock and input inhibited states, and shall queue or reject input with explicit status. | F5 | Must | All | Partial: `src/org/hti5250j/framework/tn5250/ScreenOIA.java`, `src/org/hti5250j/framework/tn5250/Screen5250.java` (`sendKeys`), `src/org/hti5250j/framework/tn5250/tnvt.java`. |
| R2.6 | The platform shall provide event-driven synchronization using host notifications and host update queries when supported. | F5, F10 | Should | All | TBD |
| R2.7 | The platform shall retry only at explicitly idempotent step boundaries and record each retry with policy details (max attempts, backoff). | F5, F10 | Must | All | TBD |

### 3) Screen Intelligence: Presentation Space + Field Semantics

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R3.1 | The platform shall capture the full presentation space and subregions as text buffers with row and column metadata. | F6, F10 | Must | All | Partial: `src/org/hti5250j/framework/tn5250/Screen5250.java` (`getScreenAsChars`, `getScreenAsAllChars`, `copyText`), `src/org/hti5250j/tools/SendScreenToFile.java`. |
| R3.2 | The platform shall provide field discovery that returns field start, length, and attribute metadata without relying on fixed coordinates. | F6 | Must | All | Partial: `src/org/hti5250j/framework/tn5250/ScreenFields.java`, `src/org/hti5250j/framework/tn5250/ScreenField.java`. |
| R3.3 | The platform shall provide field IO to copy field contents to strings and set field values with explicit truncation behavior. | F6 | Must | All | Partial: `src/org/hti5250j/framework/tn5250/Screen5250.java` (`copyTextField`, `pasteText`). |
| R3.4 | The platform shall expose field attributes including base attribute, field flags, and extended field attributes. | F6 | Must | All | Partial: `src/org/hti5250j/framework/tn5250/ScreenField.java`, `src/org/hti5250j/framework/tn5250/ScreenPlanes.java`. |
| R3.5 | The platform shall expose OIA state for diagnostics including input inhibited, message light, insert mode, and keyboard lock. | F6, F10 | Should | All | Partial: `src/org/hti5250j/framework/tn5250/ScreenOIA.java`. |
| R3.6 | The platform shall implement Screen Contracts as versioned schemas with validation that returns a structured diff. | F6 | Must | All | TBD |
| R3.7 | The platform shall detect and support multiple screen sizes, at minimum 24x80 and 27x132, and bind Screen Contracts to a specific size or size family. | F6 | Must | All | Partial: `src/org/hti5250j/framework/tn5250/Screen5250.java` uses rows/cols; size-aware contracts TBD. |
| R3.8 | The platform should allow a configuration to restrict execution to a declared screen size and fail fast on mismatch. | F6 | Should | All | TBD |

---

## Part IV. Authoring and Data Requirements

### 4) Scripting and Authoring Experience (DSL + SDK)

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R4.1 | The platform shall record interactions and emit editable scripts with deterministic replay ordering and explicit wait steps. | F7 | Must | All | Partial: `src/org/hti5250j/scripting/InterpreterDriver.java`, `src/org/hti5250j/scripting/ExecuteScriptAction.java` (GUI-coupled; no recorder). |
| R4.2 | The platform shall provide parameter tables with schema validation and explicit errors on missing parameters. | F7 | Must | All | TBD |
| R4.3 | The platform shall provide Scenario Outline style input matrices with deterministic expansion order. | F7 | Should | All | TBD |
| R4.4 | The platform shall provide control flow, reusable functions, and assertions for field values, attributes, and screen presence. | F7 | Must | All | TBD |
| R4.5 | The platform shall provide SDKs for Python, .NET, Java, and TypeScript plus Robot and pytest adapters. | F7 | Should | All | TBD |
| R4.6 | The platform shall compile all languages to a single canonical workflow IR. | F7 | Must | All | TBD |
| R4.7 | The platform shall version the workflow IR and publish backward-compatibility guarantees. | F7 | Must | All | TBD |
| R4.8 | The platform shall provide intent-driven authoring with two modes: (1) propose a candidate workflow path without writing code and (2) generate an editable workflow script from the proposed path. | F7, F13 | Must | All | TBD |
| R4.9 | The platform shall maintain a Screen Graph of observed screens and transitions with confidence scores and provenance. | F6, F13 | Must | All | TBD |
| R4.10 | The platform shall map user intent to screen predicates using label semantics and field-type inference, and shall explain the mapping and selected path. | F6, F7, F13 | Should | All | TBD |
| R4.11 | The platform shall capture and reuse prior successful paths, with explicit versioning and override controls. | F7, F13 | Should | All | TBD |

### 5) Test Data Management and System State Control

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R5.1 | The platform shall provide fixture creation and teardown with environment scoping and idempotent cleanup. | F8 | Must | All | TBD |
| R5.2 | The platform shall prefer direct system APIs or DB access for setup when permitted and use 5250 setup as a fallback. | F8 | Should | All | TBD |
| R5.3 | The platform shall provide DB regression assertions with baseline and diff outputs. | F8 | Should | All | TBD |
| R5.4 | The platform shall correlate each workflow step to UI evidence and data mutations using stable step identifiers. | F8, F10 | Must | All | TBD |
| R5.5 | The platform shall support correlation to code coverage events when available. | F8 | Should | All | TBD |

---

## Part V. Execution and Operations Requirements

### 6) Execution Modes: Headless, CI/CD, Scale

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R6.1 | The platform shall default to headless TN5250E execution without GUI dependencies, with GUI as an optional compatibility layer. | F1 | Must | Headless | Partial: `tests/org/hti5250j/headless/HeadlessSessionPairwiseTest.java`, `src/org/hti5250j/framework/tn5250/Screen5250.java`. |
| R6.2 | The platform shall produce JUnit XML and JSON artifacts with step timing and run metadata. | F9 | Must | All | TBD |
| R6.3 | The platform shall provide deterministic artifact naming that includes run ID and timestamp and enforce retention policies. | F9 | Must | All | TBD |
| R6.4 | The platform shall support horizontal scaling across sessions and runners with deterministic results. | F9 | Must | All | Partial: `src/org/hti5250j/framework/tn5250/tnvt.java` and `DataStreamProducer.java` use virtual threads; no runner pooling found. |
| R6.5 | The platform shall provide a Safe Discovery (Spider Mode) that traverses screens and captures contracts without mutating data by default. | F13 | Must | All | TBD |
| R6.6 | Safe Discovery shall be governed by user-configurable policy (allowed AID keys, allowed field writes, depth limits, and time limits) with a default read-only profile. | F13 | Must | All | TBD |
| R6.7 | Safe Discovery shall support a user-defined whitelist of inquiry-only workflows or menus and refuse traversal outside the whitelist when configured. | F13 | Should | All | TBD |
| R6.8 | The platform shall detect record/object lockouts and apply a configurable policy (wait, retry with backoff, back out, or skip) and record evidence. | F5, F13 | Must | All | TBD |

### 7) Observability, Debuggability, and Evidence

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R7.1 | The platform shall emit per-step screen snapshots in text and HTML formats with row and column metadata. | F10 | Must | All | Partial: `src/org/hti5250j/tools/SendScreenToFile.java`, `src/org/hti5250j/framework/tn5250/Screen5250.java`. |
| R7.2 | The platform shall emit field maps and attributes at assertion time. | F10 | Must | All | TBD |
| R7.3 | The platform shall emit a keystroke timeline and host response markers, with optional raw datastream capture. | F10 | Must | All | Partial: `src/org/hti5250j/framework/tn5250/DataStreamDumper.java`, `src/org/hti5250j/framework/tn5250/Screen5250.java` (`getKeys`). |
| R7.4 | The platform shall include OIA state in failure reports. | F10 | Should | All | Partial: `src/org/hti5250j/framework/tn5250/ScreenOIA.java` (no report integration found). |
| R7.5 | The platform shall provide contract diffs and root-cause classification (timeout, mismatch, auth, data). | F10 | Must | All | TBD |
| R7.6 | The platform shall render a visual screenshot artifact with optional highlight overlays for the failing field or rectangle and attach it to failure reports. | F10 | Must | All | TBD |

### 8) Operations, Commands, and Sizing

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R8.1 | The platform shall provide CLI or API operations to start, stop, list, and health-check runners and sessions. | F9, F12 | Must | All | TBD |
| R8.2 | The platform shall validate configuration and connectivity before test runs and return explicit diagnostic codes. | F9 | Must | All | TBD |
| R8.3 | The platform shall provide a documented migration path and compatibility matrix for EHLLAPI and ACS. | F3, F4 | Should | All | TBD |
| R8.4 | The platform shall publish sizing guidance with capacity formulas and validated baselines. | F9 | Must | All | TBD |
| R8.5 | The platform shall support rolling upgrades of runners without interrupting queued work. | F9 | Should | All | TBD |
| R8.6 | The platform shall version configuration schemas and provide backward-compatible defaults. | F9 | Must | All | TBD |

---

## Part VI. Extensibility and Compatibility Requirements

### 9) Extensibility and Compatibility

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R9.1 | The platform shall provide an EHLLAPI compatibility adapter for the key function set required for automation. | F3 | Must | EHLLAPI Adapter | Not implemented (no EHLLAPI adapter in `src/`). |
| R9.2 | The platform shall support ACS EHLLAPI bridge configuration with documented constraints. | F4 | Should | ACS Bridge | Not implemented (no ACS bridge in `src/`). |
| R9.3 | The platform shall provide gRPC or HTTP APIs for session control and workflow execution. | F12 | Should | All | TBD |
| R9.4 | The platform shall provide extension points for protocol filters, screen decorators, key handlers, and evidence exporters. | F12 | Must | All | Partial: `src/org/hti5250j/plugin/PluginManager.java`, `ProtocolFilterPlugin.java`, `ScreenDecoratorPlugin.java`, `KeyHandlerPlugin.java`. |
| R9.5 | The platform shall version extension interfaces and publish compatibility guarantees. | F12 | Must | All | Partial: `src/org/hti5250j/plugin/PluginVersion.java`. |

---

## Part VII. Governance and Compliance Requirements

### 11) System Boundary and Data Flows

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R11.1 | The platform shall define deployment models and system boundary, including components, trust zones, and external interfaces. | F11 | Must | All | TBD |
| R11.2 | The platform shall maintain versioned data flow diagrams for credentials, session traffic, test data, artifacts, and logs. | F11 | Must | All | TBD |
| R11.3 | The platform shall define where data is stored, processed, and transmitted for each deployment model. | F11 | Must | All | TBD |
| R11.4 | The platform shall declare multi-tenant vs single-tenant boundary assumptions and isolation guarantees. | F11 | Must | All | TBD |
| R11.5 | The platform shall document network paths and required connectivity to IBM i hosts and supporting services. | F11 | Must | All | TBD |

### 12) Governance, Security, and Compliance

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R12.1 | The platform shall apply PII or PHI redaction rules to all artifacts and logs. | F11 | Must | All | TBD |
| R12.2 | The platform shall integrate with secrets vaults and prohibit secrets in scripts. | F11 | Must | All | TBD |
| R12.3 | The platform shall provide non-repudiable run logs with who, what, when, and dataset identifiers. | F11 | Must | All | TBD |
| R12.4 | The platform shall define data classification levels and handling rules for each class. | F11 | Must | All | TBD |
| R12.5 | The platform shall enforce role-based access control with least privilege and separation of duties. | F11 | Must | All | TBD |
| R12.6 | The platform shall require strong authentication for privileged access and admin actions. | F11 | Must | All | TBD |
| R12.7 | The platform shall enforce session timeout and idle lock for consoles and APIs. | F11 | Must | All | TBD |
| R12.8 | The platform shall log required security events with synchronized timestamps and unique run identifiers. | F11 | Must | All | TBD |
| R12.9 | The platform shall provide tamper-evident or append-only log storage options. | F11 | Must | All | TBD |
| R12.10 | The platform shall encrypt data in transit using TLS 1.2+ and at rest using AES-256 or equivalent. | F11 | Must | All | Partial: `src/org/hti5250j/framework/transport/SocketConnector.java` supports SSL sockets; no TLS policy enforcement. |
| R12.11 | The platform shall support centralized key management and rotation with default rotation <= 90 days. | F11 | Must | All | TBD |
| R12.12 | The platform shall provide SBOM generation, dependency scanning, and CVE response SLAs (Critical 7d, High 30d, Medium 90d, Low 180d). | F11 | Must | All | TBD |
| R12.13 | The platform shall provide release signing and build provenance metadata. | F11 | Should | All | TBD |
| R12.14 | The platform shall support configuration baselines, change control, and drift detection. | F11 | Must | All | TBD |
| R12.15 | The platform shall define incident response procedures and evidence preservation requirements. | F11 | Must | All | TBD |
| R12.16 | The platform shall define backup scope, RTO/RPO targets, and restore testing cadence. | F11 | Must | All | TBD |
| R12.17 | The platform shall define network segmentation and egress control requirements for runners. | F11 | Must | All | TBD |
| R12.18 | The platform shall define data residency, retention, and secure disposal requirements. | F11 | Must | All | TBD |
| R12.19 | The platform shall define multi-tenant isolation controls for shared runners and artifact stores. | F11 | Must | All | TBD |
| R12.20 | The platform shall support FIPS-validated cryptography when required by deployment policy. | F11 | Should | All | TBD |

---

## Part VIII. Use Cases and Benefits

### Representative Use Cases
- Mortgage onboarding workflows with deterministic field validation and audit evidence.
- Batch processing validation with concurrency and idempotency guarantees.
- Regression suites aligned to release trains with repeatable data setups.
- Contract monitoring to detect schema or protocol drift before production impact.

### Expected Benefits
- Reduced flaky tests through explicit synchronization and contract validation.
- Faster failure diagnosis through rich evidence and root-cause classification.
- Stronger audit posture through reproducible runs and tamper-evident logs.
- Lower operational overhead through headless execution and predictable scaling.

---

## Part IX. Cross-Cutting Non-Functional Requirements

| ID | Requirement | Feature Mapping | Priority | Applicability | Current Implementation |
| --- | --- | --- | --- | --- | --- |
| R13.1 | The platform shall define and publish performance baselines per release (p50 and p95 latency, throughput, resource usage). | F9 | Must | All | TBD |
| R13.2 | The platform shall support deterministic replay given identical inputs and host state, and provide evidence diffs on deviation. | F5, F10 | Must | All | TBD |
| R13.3 | The platform shall provide configurable concurrency limits and enforce them consistently. | F2, F9 | Must | All | TBD |
| R13.4 | The platform shall support deployment on Linux containers and bare metal servers. | F9 | Must | All | TBD |
| R13.5 | The platform shall publish supported IBM i versions per release and target N and N-1. | F1 | Must | All | TBD |
| R13.6 | The platform shall provide a backward-compatibility policy for workflow IR and artifacts. | F7, F10 | Must | All | TBD |
| R13.7 | The platform shall publish SLOs for availability and execution success rates. | F9 | Should | All | TBD |
| R13.8 | The platform shall represent workflows, screen contracts, data fixtures, and policy configuration as readable text and provide canonical text export for recorded workflows. | F6, F7, F8 | Must | All | TBD |
| R13.9 | The platform shall export evidence artifacts in standard formats (text, JSON, HTML) and enable run reproduction from code, data, and environment configuration. | F5, F8, F10 | Must | All | TBD |
| R13.10 | The platform shall expose policy decisions (retry behavior, environment gating, data lifecycle rules, CI thresholds) via configuration, DSL constructs, or extension points, and document defaults. | F7, F11, F12 | Must | All | TBD |
| R13.11 | The platform shall treat GUIs, if any, as optional views/editors and not the sole interface for authoring or inspection. | F7 | Must | All | TBD |
| R13.12 | The platform shall keep automation semantics independent of any single emulator, vendor object model, or language; EHLLAPI shall be a compatibility layer rather than the conceptual core. | F3, F7, F12 | Must | All | TBD |
| R13.13 | The platform shall make failure modes explicit with preserved evidence and shall not silently recover or “best-guess” screen matches. | F5, F10 | Must | All | TBD |
| R13.14 | The platform shall support replaceable components (TN5250 engine, screen parser, DSL frontend) through stable contracts and versioned schemas without invalidating existing workflows. | F6, F7, F12 | Must | All | TBD |
| R13.15 | The platform shall preserve sufficient specs, diagrams, and text artifacts to enable explanation, replay, and migration without proprietary tooling. | F10, F11 | Should | All | TBD |
| R13.16 | The platform build shall use Gradle with explicit, configurable test parallelism controls and JUnit 5 as the primary test framework. | F9, F10 | Must | All | TBD |

---

## Appendices

### Appendix A. Glossary
- AID: Attention Identifier key that submits or triggers host actions (e.g., Enter, PF keys).
- EHLLAPI: Enhanced High Level Language Application Programming Interface.
- HLLAPI: High Level Language Application Programming Interface.
- OIA: Operator Information Area, the status line in 5250 emulators.
- PS or Presentation Space: The emulator screen buffer for a session.
- PSID: Presentation Space Identifier (single-character session ID, A-Z).
- TN5250E: Telnet extension protocol for 5250 sessions.

### Appendix B. Requirements Traceability Matrix
This section will map requirements to features, code artifacts, and tests.

### Appendix C. Standards Mapping
This section will map requirements to IBM EHLLAPI, TN5250E, and security control families.

### Appendix D. Implementation Mapping
The `Current Implementation` column is the authoritative place to map existing code features.

### Appendix E. External Standards and References (Informational)
- IBM EHLLAPI function set and presentation space model.
- IBM Wait and Search guidance for transaction completion.
- ACS EHLLAPI bridge setup, DLL naming, and 64-bit support constraints.
- TN5250E negotiation behavior and device name selection.

### Appendix F. 5250 Automation Platform Requirements Checklist (MoSCoW)

#### 1) Connectivity and Session Lifecycle

**MUST**
- Headless TN5250 session capability (no desktop emulator dependency) as a first-class execution mode.
- Session pooling with acquire/release and hard isolation between tests (no shared cursor/state).
- Deterministic teardown (guaranteed disconnect/reset even on failure).
- Multi-session inventory / status equivalent to EHLLAPI Query Sessions (10) and Query Session Status (22).

**SHOULD**
- Optional ACS EHLLAPI Bridge compatibility mode, including documented install/setup and PCSAPI initialization path.
- Support for 64-bit callers per IBM’s ACS bridge notes (bridge version constraints / DLL naming quirks).

**COULD**
- Session “profiles” (host, device name, library list setup macro, etc.) as declarative config.

#### 2) Input, AID Keys, and Synchronization (Anti-flake core)

**MUST**
- Send key/AID capability equivalent to EHLLAPI Send Key (3) (including ENTER and function keys).
- Wait / host synchronization equivalent to EHLLAPI Wait (4) and host readiness state checks (not just sleeps).
- Strict timeouts, step-level retry policy, and “retry only at idempotent boundaries.”

**SHOULD**
- Keyboard lock/unlock + host busy state exposed as first-class signals (not inferred).
- Built-in robust “transaction complete” patterns (wait + verify expected prompt/field).

**COULD**
- Adaptive backoff based on host responsiveness trends (per environment).

#### 3) Screen Intelligence: Presentation Space + Field Semantics

**MUST**
- Read full screen text equivalent to EHLLAPI: Copy Presentation Space (5) / Copy PS to String (8).
- Cursor location equivalent to Query Cursor Location (7).
- Field-level interaction (not coordinates) using EHLLAPI class primitives: Find Field Position (31) / Find Field Length (32) / Copy String to Field (33) / Copy Field to String (34).
- Query Field Attribute (14) and 5250 support for deeper attributes (e.g., Query Additional Field Attribute (45) listed as applicable).
- Screen Contracts: stable screen identification via signatures (constant labels + field topology + attributes) and validate “correct screen” before mutating fields.
- Support multiple screen sizes (at minimum 24x80 and 27x132) and bind Screen Contracts to size or size family.

**SHOULD**
- Field-map snapshot per step (values + attributes) to make diffs explainable.
- “Find-by-label” helpers that map human label → field, when feasible.
- Optional fail-fast configuration to restrict execution to a declared screen size.

**COULD**
- Contract versioning & compatibility scoring (“screen drift detected: safe/unsafe”).

#### 4) Authoring: Recorder + DSL + SDK (humans + coders)

**MUST**
- Record → script workflow capture (parity with record/replay products).
- Editable scripts with parameters (data-driven runs), loops/branching (minimum: while/for, if/else).
- First-class assertions: field value; field attributes (protected/unprotected, numeric-only, etc.); screen contract match.
- Intent-driven authoring with both modes: propose a candidate path without writing code and generate an editable workflow script.
- Screen Graph that captures observed screens and transitions with confidence and provenance.

**SHOULD**
- Multi-language client SDKs (Python + .NET at minimum) and a Robot Framework facade.
- A canonical Workflow IR (intermediate representation) so every language compiles to the same runtime plan.
- Semantic intent mapping with explainable path selection and reuse of prior successful paths.

**COULD**
- Visual “flow view” renderer for scripts (read-only) for stakeholders.

#### 5) Test Data Management and State Control

**MUST**
- Test data hooks: before/after steps that can create/reset/verify state outside the UI path.
- Dataset identity on every run (fixture id, environment id, user role, data version).

**SHOULD**
- DB regression expectation support (diff queries, invariants).
- Cross-layer correlation (UI step ↔ data mutation ↔ run id).

**COULD**
- Pluggable “data providers” (DB2, service APIs, flatfiles, vendor tools).

#### 6) Execution Modes: Headless CI/CD + Scale

**MUST**
- True headless execution compatible with CI runners.
- Outputs: JUnit XML (for pipeline gates), JSON (rich run details), artifacts (screens, traces).
- Safe Discovery (Spider Mode) with default read-only traversal and contract capture.
- Lockout detection and configurable policies (wait, retry, back out, skip) with evidence.

**SHOULD**
- Parallel execution with per-session isolation and resource governance.
- A remote runner service (API-driven) so CI agents don’t embed emulator dependencies.
- Whitelist of inquiry-only workflows or menus for safe discovery.

**COULD**
- Kubernetes-native autoscaling runners.

#### 7) Observability, Debuggability, Evidence

**MUST**
- Per-step screen snapshot and input timeline.
- Search and highlight “where it failed”: expected field missing; contract mismatch; timeout vs host busy vs auth fail.
- Visual screenshot artifacts with overlay highlights for failing fields or rectangles.

**SHOULD**
- Host/API traces when running via EHLLAPI/ACS where supported (IBM provides trace approaches for EHLLAPI/API events).

**COULD**
- Artifact redaction rules + secure storage policy (PII masking).

#### 8) Governance, Security, Compliance

**MUST**
- Secrets never stored in scripts; integrate with vault/CI secrets.
- Artifact redaction (configurable) for any field marked sensitive.
- Audit log: who ran what, when, with what dataset & environment.

**SHOULD**
- Role-based execution (privilege tiers) supported by design (aligns with “same test, different privileges”).

**COULD**
- Policy-as-code gates for which workflows can run in which envs.

#### 9) Compatibility and Extensibility

**MUST**
- Support “EHLLAPI mental model” interoperability (presentation space + function equivalence) because that’s the enterprise standard interface pattern.

**SHOULD**
- ACS bridge support as a compatibility mode (install/bridge behavior per IBM doc).

**COULD**
- Plug-in protocol backends (swap TN5250 engine, keep DSL/IR constant).
