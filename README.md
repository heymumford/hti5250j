# TN5250J Headless Edition

Headless-first fork of [TN5250J](https://github.com/tn5250j/tn5250j), a 5250 terminal emulator for IBM i (AS/400). This fork removes all Swing/AWT dependencies and provides a pure Java library for headless 5250 automation, workflow execution, and protocol testing.

**Use cases**:
- Automated regression testing of 5250 applications
- Integration testing of AS/400 business logic
- Protocol-level testing and validation
- YAML-driven workflow automation with data-driven execution
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
./gradlew test --tests "*WorkflowRunnerTest"

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
// Configure session
Properties props = new Properties();
props.setProperty("host", "ibmi.example.com");
props.setProperty("port", "23");
props.setProperty("code-page", "37");
SessionConfig config = new SessionConfig("session.properties", "my-session");

// Connect and interact via HeadlessSession API
Session5250 session = new Session5250(props, "session.properties", "my-session", config);
session.connect();
HeadlessSession headless = session.asHeadlessSession();
headless.sendKeys("USER[tab]PASSWORD[enter]");
headless.waitForKeyboardUnlock(5000);
String screen = headless.getScreenAsText();
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
- **HeadlessSession API**: Programmatic screen access, keystroke sending, screenshot capture
- **YAML workflow engine**: 6 action handlers for end-to-end terminal automation
- **EBCDIC encoding**: 23+ built-in code pages (CCSIDs 37, 273, 277, 278, 280, 284, 285, 297, 500, 870, 871, 875, 930, and Euro variants)
- **Protocol compliance**: Full TN5250E and attribute-plane operation support
- **Structured logging**: Diagnostic output for debugging and monitoring

### Key Differences from Upstream

| Feature | Upstream | This Fork |
|---------|----------|-----------|
| GUI | Swing/AWT | Removed |
| Target | Desktop users | Automation & testing |
| Dependencies | Heavy (Swing) | Minimal (Java runtime) |
| API | Desktop controls | HeadlessSession + workflow engine |
| Testing | Manual | ~194 test classes across 4 domains |

## Usage Examples

### Example 1: Headless Authentication

```java
// Configure connection
Properties props = new Properties();
props.setProperty("host", "ibmi.example.com");
props.setProperty("port", "23");
props.setProperty("code-page", "37");
SessionConfig config = new SessionConfig("session.properties", "auth-session");
Session5250 session = new Session5250(props, "session.properties", "auth-session", config);

try {
    session.connect();
    HeadlessSession headless = session.asHeadlessSession();

    // Type credentials using mnemonic syntax
    headless.sendKeys("MYUSER[tab]MYPASSWORD[enter]");

    // Wait for main menu
    headless.waitForKeyboardUnlock(5000);

    // Read screen content
    String welcome = headless.getScreenAsText();
    System.out.println(welcome);

    // Capture screenshot as PNG
    BufferedImage screenshot = headless.captureScreenshot();
    ImageIO.write(screenshot, "PNG", new File("artifacts/login.png"));
} finally {
    session.disconnect();
}
```

### Example 2: Workflow Automation

```yaml
# invoice_entry.yaml - Data-driven invoice entry
name: Invoice Entry Workflow
steps:
  - action: LOGIN
    host: "${env.IBMI_HOST}"
    user: "${env.IBMI_USER}"
    password: "${env.IBMI_PASS}"
    timeout: 30000
  - action: NAVIGATE
    keystroke: "CALL INVENTRY[enter]"
    screen: "Invoice Entry"
    timeout: 5000
  - action: FILL
    fields:
      vendor: "${data.vendor_id}"
      amount: "${data.amount}"
      description: "${data.description}"
    timeout: 5000
  - action: SUBMIT
    key: "ENTER"
    timeout: 5000
  - action: ASSERT
    text: "Invoice recorded"
    timeout: 5000
  - action: CAPTURE
    name: "confirmation"
```

```bash
i5250 run invoice_entry.yaml --data invoices.csv
```

### Example 3: EBCDIC Encoding Test

```java
// Verify EBCDIC round-trip encoding
ICodePage cp = CharMappings.getCodePage("37");
byte ebcdic = cp.uni2ebcdic('A');
char unicode = cp.ebcdic2uni(ebcdic & 0xFF);
assertEquals('A', unicode);

// Verify code page 500 (International)
ICodePage cp500 = CharMappings.getCodePage("500");
assertNotNull(cp500);
```

## Architecture

```
src/org/hti5250j/
├── framework/tn5250/  # Protocol: Screen5250, tnvt, data stream, ScreenFields
├── interfaces/        # HeadlessSession, SessionInterface, ConfigureFactory
├── session/           # DefaultHeadlessSession, NullRequestHandler
├── workflow/          # YAML workflow engine (6 handlers, validators, batch executor)
├── encoding/          # EBCDIC code pages (23+ CCSIDs via ICodePage)
│   └── builtin/       # Built-in CCSID implementations (37, 273, 277, ...)
├── keyboard/          # Key mnemonics, keyboard remapping
├── framework/transport/ # Socket connector, SSL/TLS support
├── event/             # Session, screen, keyboard event listeners
├── tools/             # Logging, LangTool utilities
├── Session5250.java   # Main session class
├── SessionConfig.java # Configuration
└── HeadlessScreenRenderer.java  # PNG screenshot generation
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

~194 test classes across four domains:

| Domain | Purpose | Tool |
|--------|---------|------|
| Unit | Protocol, encoding, field handling | JUnit 5 |
| Surface | Boundary correctness, pairwise edge cases | JUnit 5 |
| Scenario | End-to-end workflows, stress, error recovery | JUnit 5 |
| Reliability | Property-based testing, chaos injection | jqwik, resilience4j |

See **[TESTING.md](./TESTING.md)** for the four-domain testing model and how to run each category.

### Quality Metrics
- **Coverage**: 31% (legacy codebase, improving)
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
Properties props = new Properties();
props.setProperty("host", "ibmi.example.com");
props.setProperty("port", "23");
props.setProperty("code-page", "37");
props.setProperty("screen-size", "24x80");
SessionConfig config = new SessionConfig("session.properties", "my-session");
Session5250 session = new Session5250(props, "session.properties", "my-session", config);

// SSL/TLS configuration via tnvt
session.getVT().setSSLType("TLS");
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
