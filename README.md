# TN5250J Headless Edition

Headless-first fork of TN5250J, a 5250 terminal emulator for IBM i (AS/400). This fork prioritizes headless execution, deterministic session management, test automation, and protocol extensions.

Note: This is a maintained fork of [TN5250J](https://github.com/tn5250j/tn5250j). See [FORK.md](./FORK.md) for attribution and differences.

## Overview

TN5250J Headless is a Java library and toolkit for communicating with IBM i (AS/400) systems via the 5250 terminal protocol. Unlike the upstream GUI-based project, this fork removes all Swing/AWT dependencies and provides a pure library for server-side automation, test automation, and scripted terminal operations.

**Use cases**:
- Automated regression testing of 5250 applications
- Server-side terminal session pooling and reuse
- Integration testing of legacy AS/400 business logic
- Protocol-level testing and validation
- Terminal session recording and playback

## Quick Start

### Prerequisites
- Java 11 or higher
- Gradle 7.x or higher
- Linux, macOS, or Windows

### Building

```bash
git clone https://github.com/heymumford/hti5250j.git
cd hti5250j
./gradlew clean build
```

### Running Tests

```bash
# All tests
./gradlew test

# Specific test suite
./gradlew test --tests "*SessionPoolTest"

# With verbose output
./gradlew test --info
```

### Using as a Library

Add to your `build.gradle`:
```gradle
repositories {
  mavenLocal()
  // Or add Maven Central once published
}

dependencies {
  implementation 'com.heymumford:tn5250j-headless:1.0.0'
}
```

Basic usage:
```java
TN5250Session session = new TN5250Session("192.168.1.100", 23);
session.connect();
session.sendKeys("USER");
session.sendKeys("PASSWORD");
String screen = session.getScreenText();
session.disconnect();
```

## Workflow Execution (Phase 11)

HTI5250J supports YAML-based workflow automation for terminal operations. Workflows combine workflow validation with six execution handlers (LOGIN, NAVIGATE, FILL, SUBMIT, ASSERT, CAPTURE) for complete end-to-end automation.

### Execution Handlers

Six handlers execute terminal operations in sequence:

| Handler | Purpose | Example |
|---------|---------|---------|
| **LOGIN** | Connect to IBM i + authenticate | Host connection, keyboard unlock wait |
| **NAVIGATE** | Keystroke-based screen transitions | Send menu selection (e.g., "WRKSYSVAL<ENTER>") |
| **FILL** | Form field population with CSV parameters | Enter data using Tab-based navigation |
| **SUBMIT** | AID key submission + await screen refresh | Send ENTER, wait for lock→unlock cycle |
| **ASSERT** | Content verification with exceptions | Verify expected text on screen |
| **CAPTURE** | Headless screenshots (text dumps, 80-column) | Save screen state for artifacts |

### Example: Payment Processing Workflow

```bash
# Validate workflow before running
i5250 validate examples/payment.yaml --data examples/payment_data.csv

# Execute workflow against IBM i
i5250 run examples/payment.yaml --data examples/payment_data.csv
```

**Workflow (payment.yaml):**
```yaml
name: Payment Processing Workflow
steps:
  - action: LOGIN
    host: "ibmi.example.com"
    user: "testuser"
    password: "${env.IBMI_PASSWORD}"
    timeout: 30000
  - action: NAVIGATE
    keystroke: "CALL PGM(PMTENT)<ENTER>"
    screen: "Payment Entry"
    timeout: 5000
  - action: FILL
    fields:
      account: "${data.account_id}"
      amount: "${data.amount}"
    timeout: 5000
  - action: SUBMIT
    key: "ENTER"
    timeout: 5000
  - action: ASSERT
    text: "Transaction accepted"
    timeout: 5000
  - action: CAPTURE
    name: "confirmation"
```

**Data (payment_data.csv):**
```
account_id,amount,description
ACC001,150.00,Invoice-2026-001
ACC002,275.50,Invoice-2026-002
```

### Keyboard State Machine

Handlers implement a polling-based state machine:

```
LOGIN
  → Wait for keyboard unlock (OIA polling, 30s timeout, 100ms intervals)

NAVIGATE
  → Send keystroke
  → Poll screen until changed
  → Verify target screen content

FILL
  → For each field: HOME + type value + TAB
  → Wait for keyboard availability

SUBMIT
  → Send AID key (e.g., ENTER)
  → Wait for keyboard lock→unlock cycle (screen refresh)

ASSERT
  → Get screen text
  → Verify contains expected text (throw exception + dump if missing)

CAPTURE
  → Format screen dump (80-column text)
  → Write to artifacts/screenshots/
```

### Artifacts

Successful execution produces:

```
artifacts/
├── ledger.txt (execution timeline)
│   └─ Timestamped log of each step
└── screenshots/
    ├── step_0_login.txt
    ├── step_1_navigate.txt
    └── step_5_capture.txt
```

**Example ledger.txt:**
```
2026-02-08 14:30:15.123 [LOGIN] Connecting to ibmi.example.com:23
2026-02-08 14:30:15.456 [LOGIN] Keyboard unlocked, ready for input
2026-02-08 14:30:15.478 [NAVIGATE] Sending: CALL PGM(PMTENT)<ENTER>
2026-02-08 14:30:16.234 [NAVIGATE] Screen verified: Payment Entry
2026-02-08 14:30:16.245 [FILL] HOME + Account + Tab
2026-02-08 14:30:16.456 [SUBMIT] Sending: [ENTER]
2026-02-08 14:30:17.123 [SUBMIT] Keyboard lock→unlock detected, screen refreshed
2026-02-08 14:30:17.145 [ASSERT] Verified: "Transaction accepted"
2026-02-08 14:30:17.234 [CAPTURE] Screenshot saved: step_5_capture.txt
```

### Error Handling

If a step fails, execution stops with error context:

```
✗ Step 2: NAVIGATE - Failed to reach target screen
  Current screen: MAIN MENU
  Expected: PAYMENT ENTRY
  Timeout: 10000ms

Artifacts for debugging:
  - screenshots/step_2_failure.txt (includes full screen dump)
  - ledger.txt (all completed steps)
```

Exceptions provide debugging context:
- `NavigationException`: Could not navigate to target screen
- `AssertionException`: Content verification failed (includes screen dump)
- `TimeoutException`: Keyboard or screen operation timed out

### Parameter Substitution

YAML workflows support parameter binding from CSV:

```yaml
- action: FILL
  fields:
    account: "${data.account_id}"      # ← Replaced with CSV value
    amount: "${data.amount}"           # ← Replaced with CSV value
```

At runtime, `${data.account_id}` is replaced with the actual column value from payment_data.csv.

## Features

### Core Capabilities
- **Headless execution**: No GUI dependencies, pure library
- **Session pooling**: Reusable session management with lifecycle validation
- **Protocol compliance**: Full TN5250E and attribute-plane operation support
- **Plugin architecture**: Extensible handlers for custom protocol extensions
- **Structured logging**: Diagnostic output for debugging and monitoring
- **Pairwise protocol tests**: 25+ test suites covering protocol edge cases

### Key Differences from Upstream

| Feature | Upstream | This Fork |
|---------|----------|-----------|
| GUI | Swing/AWT | Removed |
| Target | Desktop users | Automation & testing |
| Dependencies | Heavy (Swing) | Minimal (Java runtime) |
| API | Desktop controls | Programmable session object |
| Testing | Manual | Comprehensive unit tests |
| Session management | Single-user | Pool & lifecycle |

See [FORK.md](./FORK.md) for full comparison and migration notes.

## Usage Examples

### Example 1: Simple Authentication

```java
TN5250Session session = new TN5250Session("ibmi.example.com", 23);
session.connect();

// Type credentials
session.sendString("USER");
session.sendTab();
session.sendString("PASSWORD");
session.sendEnter();

// Wait for prompt
session.waitForField(5000);

// Read screen
String welcome = session.getScreenText();
System.out.println(welcome);

session.disconnect();
```

### Example 2: Session Pooling

```java
SessionPool pool = new SessionPool("ibmi.example.com", 23);
pool.setMinSize(5);
pool.setMaxSize(20);
pool.start();

// Borrow session from pool
TN5250Session session = pool.borrowSession();

// Use session
session.sendString("WRKSYSVAL");
session.sendEnter();

// Return to pool
pool.returnSession(session);

// Cleanup
pool.shutdown();
```

### Example 3: Protocol Testing

```java
TN5250Protocol protocol = new TN5250Protocol();

// Test attribute handling
byte[] message = protocol.encodeAttributeUpdate(
  FieldAttribute.BRIGHT | FieldAttribute.PROTECTED
);

assertTrue(protocol.validateMessage(message));
```

## Architecture

See [TEST_ARCHITECTURE.md](./TEST_ARCHITECTURE.md) for detailed test model and coverage strategy.

```
src/main/java/com/heymumford/tn5250j/
├── core/           # Core 5250 protocol handling
├── session/        # Session and pooling management
├── protocol/       # TN5250E protocol extensions
├── handlers/       # Field and attribute handlers
└── util/           # Utilities and helpers

src/test/java/
├── core/           # Protocol tests
├── session/        # Session and pool tests
├── integration/    # Integration with real AS/400 (optional)
└── fixtures/       # Test data and mocks
```

## Testing Strategy

### Test Tiers
1. **Unit tests** — Protocol encoding/decoding, field parsing
2. **Session tests** — Connection, session lifecycle, pooling
3. **Integration tests** — Real AS/400 system (optional, requires `IBMI_HOST` env var)
4. **Pairwise tests** — 25+ protocol edge cases

Run specific tier:
```bash
./gradlew test --tests "*SessionTest"  # Session tier
./gradlew test --tests "*ProtocolTest" # Protocol tier
```

### Quality Metrics
- **Coverage**: 80%+ for core protocol
- **Test count**: 499 tests across all tiers
- **Performance**: All unit tests complete in <30 seconds
- **Compatibility**: Java 11-21, GraalVM native-image compatible

## Configuration

### Environment Variables

| Variable | Purpose | Example |
|----------|---------|---------|
| `IBMI_HOST` | AS/400 host for integration tests | `192.168.1.100` |
| `IBMI_PORT` | TN5250E port (default: 23) | `992` |
| `IBMI_USER` | Test user (if running integration tests) | `QSYS` |
| `IBMI_PASS` | Test password | (set via CI secrets) |

### Session Options

```java
TN5250Session session = new TN5250Session("ibmi.example.com", 23);
session.setConnectionTimeout(10000);       // 10 seconds
session.setReadTimeout(5000);              // 5 seconds
session.setCharacterEncoding("UTF-8");     // Character set
session.setSSL(true, trustStore);          // TLS encryption
session.connect();
```

## Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) — C1-C4 system models, containers, components, workflow pipeline
- [TESTING.md](./TESTING.md) — Four-domain test framework (Unit, Continuous Contracts, Surface, Scenario)
- [CODING_STANDARDS.md](./CODING_STANDARDS.md) — Development conventions, Java 21 features, Phase 11 patterns
- [FORK.md](./FORK.md) — Fork differences and migration guide
- [CONTRIBUTING.md](./CONTRIBUTING.md) — Contributing guidelines

## Performance Characteristics

| Operation | Latency | Notes |
|-----------|---------|-------|
| Connect | 500-1000ms | Network-dependent |
| Field input | 50-100ms | Rendering on AS/400 side |
| Session borrow | <1ms | From pool (cached) |
| Session create | 500-1000ms | First-time creation |
| Read screen (10KB) | 1-2ms | In-memory operation |
| Message encode | <1ms | Protocol operation |

## License

GPL-2.0-or-later (GPL v2 or later). See [LICENSE](./LICENSE).

## Attribution

Original TN5250J community; headless extensions by Eric C. Mumford (@heymumford).

## Upstream History

TN5250J was created to provide a Linux 5250 emulator with advanced features such as edit-field continuation, GUI windows, and cursor progression. It was open-sourced for cross-platform use and community adoption. The project originated on SourceForge and migrated to GitHub in 2016.

## Support & Contribution

- **Issues**: [GitHub Issues](https://github.com/heymumford/hti5250j/issues)
- **Discussions**: [GitHub Discussions](https://github.com/heymumford/hti5250j/discussions)
- **Contributing**: See [CONTRIBUTING.md](./CONTRIBUTING.md)

## Roadmap

**v1.0.0** (Stable)
- Core session and pooling API
- Full 5250E protocol support
- Comprehensive test suite

**v1.1.0** (Planned)
- GraalVM native-image support
- Async API (CompletableFuture)
- Record/playback sessions

**v2.0.0** (Future)
- WebSocket transport
- Metrics and observability
- Session replication for HA
