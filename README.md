# TN5250J Headless Edition

Headless-first fork of [TN5250J](https://github.com/tn5250j/tn5250j), a 5250 terminal emulator for IBM i (AS/400). This fork removes all Swing/AWT dependencies and provides a pure Java library for headless execution, session management, test automation, and protocol extensions.

**Use cases**:
- Automated regression testing of 5250 applications
- Server-side terminal session pooling and reuse
- Integration testing of AS/400 business logic
- Protocol-level testing and validation
- Terminal session recording and playback

## Quick Start

### Prerequisites
- Java 21 or higher
- Gradle 8.x or higher
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
}

dependencies {
  implementation 'org.hti5250j:tn5250j-headless:0.13.0'
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

## Workflow Execution

YAML-based workflow automation with six execution handlers for end-to-end terminal operations.

### Execution Handlers

| Handler | Purpose |
|---------|---------|
| **LOGIN** | Connect and authenticate, wait for keyboard unlock |
| **NAVIGATE** | Send keystrokes, poll until target screen appears |
| **FILL** | Populate form fields from CSV parameters |
| **SUBMIT** | Send AID key, wait for screen refresh |
| **ASSERT** | Verify expected text on screen |
| **CAPTURE** | Save screen dump (80-column text) to artifacts |

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

### Artifacts

Execution produces a timestamped `ledger.txt` and screen dumps in `artifacts/screenshots/`:

```
artifacts/
├── ledger.txt            # Timestamped execution log
└── screenshots/
    ├── step_0_login.txt
    ├── step_1_navigate.txt
    └── step_5_capture.txt
```

### Error Handling

Execution stops on failure with context including the current screen state, expected state, and a screen dump saved to `artifacts/screenshots/`. Exception types: `NavigationException`, `AssertionException`, `TimeoutException`.

### Parameter Substitution

YAML fields support `${data.<column>}` placeholders, replaced at runtime with values from the CSV data file.

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

The test suite covers protocol correctness, encoding, workflow execution, and resilience.

### Running Tests

```bash
# Run all tests
./gradlew test

# Tests + coverage report
./gradlew test jacocoTestReport

# Run JMH benchmarks
./gradlew jmh

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Test Organization

~170 test classes across four domains:

| Domain | Purpose | Tool |
|--------|---------|------|
| Unit | Protocol, encoding, field handling | JUnit 5 |
| Surface | Boundary correctness, pairwise edge cases | JUnit 5 |
| Scenario | End-to-end workflows, stress, error recovery | JUnit 5 |
| Reliability | Property-based testing, chaos injection | jqwik, resilience4j |

See **[TESTING.md](./TESTING.md)** for the four-domain testing model and how to run each category.

### Quality Metrics
- **Coverage**: 21% baseline (legacy codebase, improving)
- **Compatibility**: Java 21+

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

### Project Documentation
- [ARCHITECTURE.md](./ARCHITECTURE.md) — System design and workflow pipeline
- [TESTING.md](./TESTING.md) — Four-domain testing model
- [CONTRIBUTING.md](./CONTRIBUTING.md) — Contributing guidelines
- [CHANGELOG.md](./CHANGELOG.md) — Release history
- [SECURITY.md](./SECURITY.md) — Security considerations

### Protocol & Technical Reference
- [docs/5250_COMPLETE_REFERENCE.md](./docs/5250_COMPLETE_REFERENCE.md) — **Comprehensive 5250 protocol reference** covering Telnet, GDS records, operations, commands, structured fields, attributes, and TN5250E extensions. Authoritative source based on RFC 1205, RFC 2877, RFC 4777, and IBM documentation.
- [docs/INDEX.md](./docs/INDEX.md) — Navigation index for all technical documentation

### Archived Documentation
Planning, strategy, and assessment documents have been archived in the [archive/](./archive/) directory to keep the root clean and focused on shipping code.
- [archive/planning/README.md](./archive/planning/README.md) — Index of archived planning documents

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

## Support

- **Issues**: [GitHub Issues](https://github.com/heymumford/hti5250j/issues)
- **Discussions**: [GitHub Discussions](https://github.com/heymumford/hti5250j/discussions)
- **Contributing**: See [CONTRIBUTING.md](./CONTRIBUTING.md)
