# HTI5250J Integration Architecture Review

**Date:** February 2026
**Audience:** Integration architects, platform engineers, third-party consumers
**Scope:** Current and future integration surfaces, extensibility assessment, SDK/CLI/API roadmap

---

## Executive Summary

HTI5250J is architected as a **headless-first semantic orchestrator** with well-defined integration boundaries. The Java API is clean and library-friendly, currently supporting:

- **Test Framework Integration:** JUnit 5, TestNG, Robot Framework (Jython bridge)
- **Workflow Automation:** YAML-based workflows with CSV parameter binding
- **Extensibility:** Injectable RequestHandler interface, plugin system (legacy), virtual thread concurrency
- **Observability:** Batch metrics, execution ledger, screenshot artifacts

**Critical Finding:** The API is mature for library consumption but lacks:
1. **Published distribution** (Maven Central, Gradle Plugin Registry)
2. **REST API** for cross-language clients
3. **CLI tool** for direct command-line use
4. **CI/CD tool plugins** (Jenkins, GitHub Actions, GitLab CI)
5. **Monitoring integrations** (Prometheus, Datadog, Splunk)
6. **SDK wrappers** (Python, Go, Node.js)

**Recommendation:** Prioritize (1) Maven publishing, (2) REST API via Spring Boot, (3) GitHub Actions plugin — these unlock 80% of integration value with 20% of effort.

---

## 1. Integration Point Inventory

### 1.1 Current Integration Surfaces

| Layer | Entry Point | Status | Maturity |
|-------|------------|--------|----------|
| **Java Library API** | Session5250, HeadlessSession | ✅ Ready | Production |
| **Workflow Execution** | WorkflowRunner, YAML/CSV | ✅ Ready | Production |
| **CLI Tool** | WorkflowCLI (main) | ⚠️ Partial | Beta (validate, run, simulate only) |
| **Robot Framework** | HTI5250J.py (Jython bridge) | ⚠️ Partial | Alpha (11 keywords) |
| **Test Framework** | JUnit 5 integration | ✅ Ready | Production |
| **Request Handling** | RequestHandler interface | ✅ Ready | Production |
| **Plugin System** | PluginManager, HTI5250jPlugin | ❌ Dead | Deprecated (no implementations) |
| **Session Pooling** | Concurrent virtual threads | ✅ Ready | Production |

### 1.2 Planned Integration Surfaces

| Entry Point | Purpose | Priority | Estimated Effort |
|-------------|---------|----------|------------------|
| **Maven Central** | Dependency distribution | P0 | 2 days |
| **REST API** | Language-agnostic HTTP interface | P0 | 2-3 weeks |
| **GitHub Actions** | CI/CD workflows | P1 | 1 week |
| **Python SDK** | Native Python bindings (JPype) | P1 | 1-2 weeks |
| **Prometheus Metrics** | Observability integration | P1 | 1 week |
| **Jenkins Plugin** | Enterprise CI/CD | P2 | 2-3 weeks |
| **Datadog APM** | Application performance monitoring | P2 | 1-2 weeks |
| **TestRail / Jira Xray** | Test result reporting | P2 | 1-2 weeks |
| **Docker Image** | Container-ready distribution | P1 | 3 days |
| **Gradle Plugin** | Build-time workflow integration | P2 | 1 week |

---

## 2. Testing Framework Integrations

### 2.1 JUnit 5 (Current — Production Ready)

**Status:** ✅ Full integration
**Evidence:** 224 test files using JUnit 5, Domain 1-4 test architecture

**Surface:**
```java
// Direct integration via SessionInterface
SessionInterface session = new Session5250(props, resource, name, config);
session.connect();
String screen = session.getScreenText();
session.disconnect();
```

**Assessment:**
- **Strengths:**
  - Zero coupling to GUI; works headless
  - AssertionException + failure artifacts (screenshots, ledger)
  - Batching metrics (p50/p99 latency, throughput)
  - Virtual thread concurrency (1000+ sessions without resource pressure)

- **Gaps:**
  - No built-in JUnit 5 assertions (must use AssertJ)
  - No @ParameterizedTest support for CSV datasets
  - No @Nested test organization helpers

**Recommendation:** Create `hti5250j-junit5` module:
```java
// New module: src/test-support/junit5/
public class ScreenAssertions {
    public static void assertScreenContains(SessionInterface session, String expected) { ... }
    public static void assertScreenMatches(SessionInterface session, Pattern pattern) { ... }
    public static void assertKeyboardUnlocked(SessionInterface session) { ... }
}

public class SessionFactory {
    // Singleton session for test suite with auto-cleanup
    @RegisterExtension
    static SessionLifecycleExtension session = new SessionLifecycleExtension(config);
}
```

---

### 2.2 TestNG (Planned — P2)

**Status:** ⚠️ Untested but compatible
**Effort:** 3 days (create test support module, verify parallelization)

**Motivation:**
- Enterprise teams use TestNG (better parallelization, dependency injection)
- TestNG provides `@BeforeGroups`, `@AfterGroups` for session pooling
- HTI5250J's virtual threads align well with TestNG's thread pool

**Design:**
```java
// New module: src/test-support/testng/
public class HTI5250JTestListener implements ITestListener {
    // Auto-pool sessions, report metrics, cleanup on suite exit
    void onStart(ISuite suite) { ... }
    void onTestSuccess(ITestResult result) { ... }
    void onFinish(ISuite suite) { ... }
}

public class SessionPool {
    @DataProvider(name = "ibmi-sessions", parallel = true)
    Object[][] sessionDataProvider() {
        return pool.borrowSessions(5);
    }
}
```

---

### 2.3 Robot Framework (Current — Alpha)

**Status:** ⚠️ Partial (11 keywords, Jython bridge)
**Evidence:** `examples/HTI5250J.py` (14KB), `docs/ROBOT_FRAMEWORK_INTEGRATION.md`

**Current Keywords:**
- `Connect To IBM i` (host, port, screen_size, code_page)
- `Disconnect From IBM i`
- `Send Keys To Screen` (mnemonic syntax)
- `Wait For Keyboard Unlock`
- `Wait For Keyboard Lock Cycle`
- `Screen Should Contain` (text assertion)
- `Screen Should Match` (regex)
- `Capture Screen` (text dump)
- `Capture Screenshot` (PNG)
- `Get Screen As Text`
- `Handle System Request` (custom RequestHandler)

**Assessment:**
- **Strengths:**
  - Works in Docker/headless (no X11 required)
  - RequestHandler injection enables custom SYSREQ handling
  - ~500KB memory per session

- **Gaps:**
  - Only 11 keywords vs typical automation libraries (50+)
  - No field-level manipulation (e.g., `Fill Field By Label`)
  - No screen diff/OCR comparison
  - No parallel execution coordination
  - No test report integration (ResultsLibrary missing)

**Recommendation:** Create `hti5250j-robot-bridge` module (Java side):
```java
// New module: src/main/java/org/hti5250j/robot/
public interface RobotKeywordProvider {
    List<RobotKeyword> getKeywords();
    Object invoke(String keyword, Object[] args) throws Exception;
}

public class StandardRobotKeywords implements RobotKeywordProvider {
    // ~30 keywords: Fill Screen By Field Map, Assert Field Value, etc.
}

public class RobotAdapter {
    // Auto-discovery via service loader
    // Enables third parties to register custom keywords
}
```

Corresponding Python enhancements:
```python
# examples/HTI5250J.py (expand to 40+ keywords)
def fill_field_by_label(self, label, value):
    """Fill input field by visible label text."""
    # Find label position
    # Calculate offset to input field
    # Send keys to field
    pass

def assert_field_value(self, label, expected):
    """Assert input field contains expected value."""
    pass

def capture_screen_diff(self, baseline_screenshot, tolerance_percent=5):
    """Compare current screen to baseline image (with tolerance)."""
    pass
```

---

### 2.4 Cucumber/BDD (Planned — P2)

**Status:** ❌ Not implemented
**Effort:** 1-2 weeks (Glue code + step definitions)

**Motivation:**
- Enterprise teams use BDD for business-readable tests
- HTI5250J workflows are already declarative (YAML)
- Gherkin syntax maps cleanly to workflow actions

**Design:**
```gherkin
# features/payment_processing.feature
Feature: Payment Processing
  Scenario: Process single payment successfully
    Given I am connected to IBM i system "prod.ibmi.com"
    When I navigate to "Payment Entry" screen
    And I fill the form with:
      | Account ID | Amount | Currency |
      | ACC-001    | 150.00 | USD      |
    And I submit the form
    Then the screen should contain "Payment Processed"
    And I capture the confirmation screen
```

```java
// src/test/java/org/hti5250j/cucumber/PaymentSteps.java
@Given("I am connected to IBM i system {string}")
public void connectToIBMi(String hostname) { ... }

@When("I navigate to {string} screen")
public void navigateToScreen(String screenName) { ... }

@And("I fill the form with:")
public void fillFormWithTable(DataTable table) {
    // Maps table rows to CSV dataset format
    // Reuses WorkflowRunner.handleFill()
}
```

---

### 2.5 Karate API Testing (Planned — P3)

**Status:** ❌ Not applicable (Karate is HTTP-focused)
**Alternative:** Use REST API (see Section 4) instead

---

## 3. CI/CD Tool Integrations

### 3.1 GitHub Actions (Planned — P1)

**Status:** ⚠️ Untested (potential via Docker, no native plugin)
**Effort:** 1 week

**Current Workaround:**
```yaml
name: HTI5250J Workflows
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: ./gradlew build
      - run: |
          export IBMI_HOST=${{ secrets.IBMI_HOST }}
          export IBMI_USER=${{ secrets.IBMI_USER }}
          export IBMI_PASS=${{ secrets.IBMI_PASS }}
          ./gradlew test
```

**Recommendation:** Create GitHub Action:
```yaml
# actions/hti5250j-run/action.yml
name: 'HTI5250J Workflow Runner'
description: 'Run HTI5250J workflow against IBM i'
inputs:
  workflow-file:
    description: 'Path to workflow.yaml'
    required: true
  data-file:
    description: 'Path to data.csv (optional)'
    required: false
  ibmi-host:
    description: 'IBM i hostname'
    required: true
  ibmi-user:
    description: 'IBM i user'
    required: true
  ibmi-password:
    description: 'IBM i password'
    required: true
outputs:
  artifacts-path:
    description: 'Path to artifacts/ directory (screenshots, ledger)'
runs:
  using: 'docker'
  image: 'docker://ghcr.io/heymumford/hti5250j:latest'
  args:
    - 'run'
    - '${{ inputs.workflow-file }}'
    - '--data'
    - '${{ inputs.data-file }}'
```

Implementation:
- Publish Docker image to ghcr.io
- Create action wrapper that invokes Docker
- Test with sample GitHub repo

---

### 3.2 GitLab CI (Planned — P1)

**Status:** ⚠️ Untested (Docker image should work)
**Effort:** 3 days

**Template:**
```yaml
# .gitlab-ci.yml
hti5250j_workflow:
  image: ghcr.io/heymumford/hti5250j:latest
  stage: test
  script:
    - i5250 run examples/payment.yaml --data examples/payment_data.csv
  artifacts:
    paths:
      - artifacts/**
    reports:
      junit: artifacts/junit.xml
  environment:
    name: ibmi-prod
  variables:
    IBMI_HOST: $IBMI_HOST_PROD
    IBMI_USER: $IBMI_USER
    IBMI_PASS: $IBMI_PASS
```

---

### 3.3 Jenkins (Planned — P2)

**Status:** ⚠️ Untested
**Effort:** 2-3 weeks (plugin development)

**Approach 1: Declarative Pipeline (Simple)**
```groovy
pipeline {
    agent any
    stages {
        stage('HTI5250J Workflow') {
            steps {
                sh './gradlew test'
                sh 'i5250 run workflow.yaml --data data.csv'
                junit 'artifacts/junit.xml'
                archiveArtifacts 'artifacts/**'
            }
        }
    }
    post {
        always {
            publishHTML([
                reportDir: 'artifacts/screenshots',
                reportFiles: 'index.html',
                reportName: 'Screen Captures'
            ])
        }
    }
}
```

**Approach 2: Jenkins Plugin (Advanced)**
- Custom step: `hti5250j: { workflowFile: '...', dataFile: '...', ... }`
- Auto-pool sessions per build
- Integrated test result parsing
- Artifact collection

---

### 3.4 Azure DevOps (Planned — P2)

**Status:** ⚠️ Untested
**Effort:** 1-2 weeks

**Template:**
```yaml
trigger:
  - main

pool:
  vmImage: 'ubuntu-latest'

steps:
  - task: JavaToolInstaller@0
    inputs:
      versionSpec: '21'
  - task: Gradle@3
    inputs:
      gradleFile: 'build.gradle'
      tasks: 'build test'
  - script: |
      export IBMI_HOST=$(IBMI_HOST)
      ./gradlew hti5250j:run --workflow=$(workflow_file)
    displayName: 'Run HTI5250J Workflow'
  - task: PublishTestResults@2
    inputs:
      testResultsFiles: 'artifacts/junit.xml'
  - task: PublishBuildArtifacts@1
    inputs:
      pathToPublish: 'artifacts'
      artifactName: 'workflow-artifacts'
```

---

## 4. Reporting Integrations

### 4.1 Allure Report (Planned — P1)

**Status:** ❌ Not integrated
**Effort:** 5 days

**Design:**
```java
// New module: src/test-support/allure/
public class AllureReportAdapter {
    private final WorkflowRunner runner;
    private final AllureLifecycle lifecycle;

    public void onStepExecuted(String stepName, long durationMs, boolean success, Throwable error) {
        // Emit Allure step event
        lifecycle.startStep(new StepResult()
            .setName(stepName)
            .setStatus(success ? Status.PASSED : Status.FAILED)
        );
        lifecycle.stopStep();
    }

    public void onArtifactCollected(String path) {
        // Attach screenshot to Allure report
        lifecycle.addAttachment("screenshot", "image/png", path);
    }
}
```

**Integration Points:**
1. Domain 4 (Scenario) tests → Allure test cases
2. Artifacts (screenshots, ledger) → Allure attachments
3. Batch metrics (latency, throughput) → Allure timeline
4. Failures → Allure failure analysis

**Result:** Beautiful test reports with execution timeline, screenshots, metrics.

---

### 4.2 ExtentReports (Planned — P2)

**Status:** ❌ Not integrated
**Effort:** 5 days (similar to Allure)

**Advantages over Allure:**
- Single HTML file (no server required)
- Better for email distribution
- More customizable styling

**Design:** Similar to Allure adapter; emit ExtentTest events instead.

---

### 4.3 TestRail Integration (Planned — P2)

**Status:** ❌ Not integrated
**Effort:** 1 week

**Use Case:** Map test results to TestRail test cases, auto-update run status

```java
public class TestRailReporter {
    private final TestRailAPI api;

    public void reportWorkflowResult(WorkflowResult result, int testRunId) {
        api.updateTestResult(
            testRunId,
            result.testCaseId(),
            result.success() ? Status.PASSED : Status.FAILED,
            result.durationMs(),
            result.artifacts()
        );
    }
}
```

**Configuration:**
```properties
testrail.url=https://testrail.example.com
testrail.user=api@example.com
testrail.api-key=${TESTRAIL_API_KEY}
testrail.project-id=5
testrail.run-id=${TEST_RUN_ID}
```

---

### 4.4 Jira/Xray Integration (Planned — P2)

**Status:** ❌ Not integrated
**Effort:** 1-2 weeks

**Use Case:** Map HTI5250J workflows to Jira test cases, auto-update test execution status

```java
public class JiraXrayReporter {
    private final XrayAPI api;

    public void reportExecution(WorkflowResult result, String testExecKey) {
        api.createTestEvidence(testExecKey, XrayEvidence.builder()
            .status(result.success() ? "PASSED" : "FAILED")
            .duration(result.durationMs())
            .evidence(result.artifacts())
            .build()
        );
    }
}
```

---

## 5. Monitoring Integrations

### 5.1 Prometheus Metrics (Planned — P1)

**Status:** ❌ Not integrated
**Effort:** 1 week

**Design:**
```java
// New module: src/main/java/org/hti5250j/metrics/
public class HTI5250jMetrics {
    private final MeterRegistry registry;

    // Counters
    Counter.builder("hti5250j.sessions.created")
        .description("Total sessions created")
        .register(registry);

    // Gauges
    AtomicInteger.builder("hti5250j.sessions.active")
        .description("Currently active sessions")
        .register(registry);

    // Timers
    Timer.builder("hti5250j.workflow.duration")
        .description("Workflow execution time")
        .publishPercentiles(0.50, 0.95, 0.99)
        .register(registry);

    // Histograms
    DistributionSummary.builder("hti5250j.screen.text.length")
        .description("Characters in screen text")
        .register(registry);
}
```

**Metrics to Expose:**
| Metric | Type | Description |
|--------|------|-------------|
| `hti5250j.sessions.created` | Counter | Total sessions created since startup |
| `hti5250j.sessions.active` | Gauge | Currently active sessions |
| `hti5250j.sessions.errors` | Counter | Session connection failures |
| `hti5250j.workflow.duration` | Timer | Workflow execution time (p50, p95, p99) |
| `hti5250j.workflow.success_rate` | Gauge | Percentage of successful workflows |
| `hti5250j.keyboard.wait_cycles` | Counter | Keyboard lock-unlock cycles |
| `hti5250j.keyboard.timeout_errors` | Counter | Keyboard unlock timeouts |
| `hti5250j.field.fill_duration` | Timer | Time to fill form fields |
| `hti5250j.assertion.failures` | Counter | Assertion failures |
| `hti5250j.screenshot.captures` | Counter | Screenshots captured |

**Configuration:**
```properties
# application.yaml
management:
  endpoints.web.exposure.include: prometheus
  metrics.export.prometheus.enabled: true
```

**Scrape Config:**
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'hti5250j'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

---

### 5.2 Grafana Dashboards (Planned — P1)

**Status:** ⚠️ Can consume Prometheus metrics
**Effort:** 2 days (dashboard JSON)

**Panels:**
1. **Session Lifecycle:** Active sessions, creation rate, errors
2. **Workflow Performance:** P50/P95/P99 latency, success rate, throughput
3. **Keyboard Wait Time:** Average lock-unlock cycle, timeouts
4. **Form Filling:** Fields per workflow, average fill time
5. **Error Analysis:** Top 10 assertion failures, error types

---

### 5.3 Datadog APM (Planned — P2)

**Status:** ❌ Not integrated
**Effort:** 1-2 weeks

**Integration:**
```java
// Add Datadog tracer to WorkflowRunner
public class WorkflowRunner {
    private final Tracer tracer = GlobalTracer.get();

    public void executeWorkflow(WorkflowSchema workflow, Map<String, String> dataRow) {
        Span span = tracer.buildSpan("workflow")
            .withTag("workflow.name", workflow.getName())
            .withTag("data.rows", dataRow.size())
            .start();

        try {
            for (StepDef step : workflow.getSteps()) {
                executeStep(step, dataRow);  // Step spans auto-created
            }
        } finally {
            span.finish();
        }
    }
}
```

**Datadog Provides:**
- Distributed tracing (workflow → session → screen)
- APM dashboards
- Service dependency mapping
- Error tracking

---

### 5.4 Splunk Logging (Planned — P2)

**Status:** ❌ Not integrated
**Effort:** 3 days

**Design:**
```properties
# logback.xml
<appender name="SPLUNK" class="com.splunk.logging.HttpEventCollectorLogbackAppender">
    <token>${SPLUNK_HEC_TOKEN}</splunk>
    <url>https://splunk.example.com:8088</url>
</appender>

<logger name="org.hti5250j" level="INFO" additivity="false">
    <appender-ref ref="SPLUNK"/>
</logger>
```

**Log Events:**
- Workflow start/completion
- Session connect/disconnect
- Assertion failures (with screen dump)
- Keyboard timeouts
- Field fill operations

---

### 5.5 CloudWatch (Planned — P3)

**Status:** ❌ Not integrated
**Effort:** 1 week

**Integration:** Similar to Datadog; uses AWS SDK for metrics/logs publishing.

---

## 6. API Design Assessment

### 6.1 Java API Quality

**Scope:** `Session5250`, `HeadlessSession`, `WorkflowRunner`, supporting interfaces

#### 6.1.1 Strengths ✅

| Criterion | Assessment | Evidence |
|-----------|------------|----------|
| **Headless-first** | Excellent | No AWT/Swing in core APIs; optional GUI via dependency injection |
| **Interface segregation** | Excellent | HeadlessSession (6 methods), RequestHandler (1 method), clear contracts |
| **Concurrency ready** | Excellent | Virtual threads, immutable screen buffer, no shared mutable state |
| **Error handling** | Good | Custom exceptions (AssertionException, NavigationException), artifact collection |
| **Extensibility** | Good | RequestHandler injection, plugin system (legacy but structure is sound) |
| **Documentation** | Excellent | ARCHITECTURE.md, TESTING.md, migration guides, examples |
| **Testing** | Excellent | 224 test files, four-domain model, high coverage |

#### 6.1.2 Gaps ⚠️

| Criterion | Gap | Severity | Fix |
|-----------|-----|----------|-----|
| **Maven publishing** | Not on Maven Central | P0 (blocking) | Add `maven-publish` plugin, GPG signing, Sonatype OSSRH |
| **Semantic versioning** | Inconsistent (0.12.0 vs various docs) | P1 | Adopt SemVer strictly, tag releases |
| **API stability guarantees** | No deprecation policy | P1 | Define 2-version deprecation window |
| **Builder patterns** | Constructors are verbose | P2 | Add `Session5250Builder`, `HeadlessSessionBuilder` |
| **Factory patterns** | Multiple factory interfaces | P2 | Consolidate to single `HeadlessSessionFactory` |
| **REST/JSON support** | None | P0 (roadmap) | See Section 4 (REST API) |
| **Async APIs** | All blocking | P2 | Add CompletableFuture variants for client flexibility |

#### 6.1.3 Recommendations

**Immediate (P0):**
1. Add Maven publishing to `build.gradle`:
```gradle
plugins {
    id 'maven-publish'
    id 'signing'
}

publishing {
    repositories {
        maven {
            url "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            credentials(PasswordCredentials)
        }
    }
    publications {
        mavenJava(MavenPublication) {
            from components.java
            pom {
                name = 'HTI5250J'
                description = 'Headless TN5250E terminal emulator for IBM i'
                url = 'https://github.com/heymumford/hti5250j'
                // License, developers, SCM, etc.
            }
        }
    }
}
```

2. Add `@Deprecated` annotations with removal version:
```java
@Deprecated(since = "0.12.0", forRemoval = true)
public void legacyMethod() { }
```

**Short-term (P1):**
1. Create builder pattern:
```java
public class Session5250Builder {
    public Session5250Builder host(String host) { ... }
    public Session5250Builder port(int port) { ... }
    public Session5250Builder codePage(int codePage) { ... }
    public Session5250 build() { ... }
}

// Usage
Session5250 session = new Session5250Builder()
    .host("ibmi.example.com")
    .port(23)
    .codePage(37)
    .build();
```

2. Define API stability guarantees:
   - **Public API:** All classes in `org.hti5250j.interfaces` and `org.hti5250j` packages
   - **Internal API:** Classes in `org.hti5250j.framework.*, org.hti5250j.tools.*`
   - **Deprecation:** 2-version window before removal
   - **Version bumping:** MAJOR.MINOR.PATCH (semver)

**Medium-term (P2):**
1. Add async variants:
```java
public interface AsyncHeadlessSession extends HeadlessSession {
    CompletableFuture<Void> connectAsync();
    CompletableFuture<String> getScreenAsTextAsync();
    CompletableFuture<Void> sendKeysAsync(String keys);
}
```

---

### 6.2 REST API Design (Planned)

**Status:** ⚠️ Not implemented
**Effort:** 2-3 weeks
**Technology:** Spring Boot 3.x + embedded Undertow

#### 6.2.1 API Structure

**Base URL:** `http://localhost:8080/api/v1`

**Endpoints:**

```
POST   /sessions
       → Create new session (host, port, config)
       ← { sessionId, status }

GET    /sessions/{sessionId}
       → Get session details
       ← { sessionId, status, connected, screenText, oiaState }

POST   /sessions/{sessionId}/connect
       → Establish connection
       ← { sessionId, status }

POST   /sessions/{sessionId}/disconnect
       → Close connection
       ← { sessionId, status }

POST   /sessions/{sessionId}/keys
       → Send keys to session
       Body: { keys: "CALL PGM(MYAPP)[enter]" }
       ← { status, screenText }

GET    /sessions/{sessionId}/screen
       → Get current screen as text
       ← { text: "..." (80×24), oiaState }

GET    /sessions/{sessionId}/screenshot
       → Get PNG screenshot
       ← { data: "...", format: "image/png" }

POST   /sessions/{sessionId}/wait-keyboard-unlock
       → Wait for keyboard availability
       Body: { timeoutMs: 5000 }
       ← { available: true }

POST   /workflows
       → Create workflow execution
       Body: { workflowYaml: "...", dataRow: {...} }
       ← { executionId, status }

GET    /workflows/{executionId}
       → Get workflow execution status
       ← { executionId, status, steps: [...], artifacts: {...} }

GET    /workflows/{executionId}/artifacts
       → List execution artifacts
       ← { screenshots: [...], ledger: "..." }

POST   /batches
       → Execute batch workflow
       Body: { workflowYaml: "...", csvData: "..." }
       ← { batchId, status }

GET    /batches/{batchId}
       → Get batch metrics
       ← { totalWorkflows, success, failures, p50LatencyMs, p99LatencyMs, throughputOpsPerSec }
```

#### 6.2.2 Implementation

```java
// src/main/java/org/hti5250j/rest/
@RestController
@RequestMapping("/api/v1")
public class SessionController {

    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createSession(@RequestBody SessionRequest req) {
        SessionInterface session = HeadlessSessionFactory.create(
            req.host(),
            req.port(),
            req.config()
        );
        String sessionId = UUID.randomUUID().toString();
        sessionStore.put(sessionId, session);
        return ResponseEntity.created(...).body(new SessionResponse(sessionId, "CREATED"));
    }

    @PostMapping("/sessions/{sessionId}/keys")
    public ResponseEntity<ScreenResponse> sendKeys(
        @PathVariable String sessionId,
        @RequestBody SendKeysRequest req
    ) throws Exception {
        SessionInterface session = sessionStore.get(sessionId);
        session.sendString(req.keys());
        return ResponseEntity.ok(new ScreenResponse(
            session.getScreenText(),
            session.getOIA()
        ));
    }

    @GetMapping("/workflows/{executionId}")
    public ResponseEntity<WorkflowExecutionResponse> getExecution(
        @PathVariable String executionId
    ) {
        WorkflowExecution exec = executionStore.get(executionId);
        return ResponseEntity.ok(WorkflowExecutionResponse.from(exec));
    }
}
```

#### 6.2.3 Client Libraries

**Python (requests):**
```python
import requests

client = requests.Session()
client.base_url = "http://localhost:8080/api/v1"

# Create session
resp = client.post("/sessions", json={
    "host": "ibmi.example.com",
    "port": 23,
    "code_page": 37
})
session_id = resp.json()["sessionId"]

# Send keys
resp = client.post(f"/sessions/{session_id}/keys", json={
    "keys": "CALL PGM(MYAPP)[enter]"
})
screen_text = resp.json()["text"]

# Get screenshot
resp = client.get(f"/sessions/{session_id}/screenshot")
with open("screenshot.png", "wb") as f:
    f.write(resp.content)
```

**Go (net/http):**
```go
import "net/http"

client := &http.Client{}

// Create session
req, _ := http.NewRequest("POST", "http://localhost:8080/api/v1/sessions", ...)
resp, _ := client.Do(req)

// Send keys
req, _ = http.NewRequest("POST",
    fmt.Sprintf("http://localhost:8080/api/v1/sessions/%s/keys", sessionID), ...)
resp, _ = client.Do(req)
```

**Node.js (axios):**
```javascript
const axios = require('axios');

const client = axios.create({
    baseURL: 'http://localhost:8080/api/v1'
});

// Create session
const { data: { sessionId } } = await client.post('/sessions', {
    host: 'ibmi.example.com',
    port: 23
});

// Send keys
const { data: { text } } = await client.post(`/sessions/${sessionId}/keys`, {
    keys: 'CALL PGM(MYAPP)[enter]'
});
```

---

## 7. SDK/Client Library Feasibility

### 7.1 Python Wrapper (JPype — Planned P1)

**Status:** ⚠️ Untested
**Effort:** 1-2 weeks
**Approach:** JPype for direct Java interop (no Jython required)

**Design:**
```python
# pip install hti5250j
from hti5250j import HeadlessSessionFactory, RequestHandler

class MyRequestHandler(RequestHandler):
    def handle_system_request(self, screen_content):
        # Custom SYSREQ handling
        return "OK"

# Create session
session = HeadlessSessionFactory.create_session(
    host="ibmi.example.com",
    port=23,
    code_page=37,
    request_handler=MyRequestHandler()
)

# Connect
session.connect()
session.wait_for_keyboard_unlock(30000)

# Interact
session.send_keys("CALL PGM(MYAPP)[enter]")
screen = session.get_screen_as_text()

# Take screenshot
image = session.capture_screenshot()
image.save("screenshot.png")

# Disconnect
session.disconnect()
```

**Implementation:**
```python
# pyproject.toml
[project]
name = "hti5250j"
version = "0.12.0"
dependencies = [
    "JPype1>=1.4.1",
    "Pillow>=9.0.0",  # For screenshot handling
]

[tool.setuptools]
packages = ["hti5250j"]
```

```python
# hti5250j/__init__.py
from jpype import JPackage
import jpype

if not jpype.isJVMStarted():
    jpype.startJVM(classpath=['./dist/hti5250j-0.12.0.jar'])

# Import Java classes
HeadlessSessionFactory = JPackage('org.hti5250j.interfaces').HeadlessSessionFactory
Session5250 = JPackage('org.hti5250j').Session5250
RequestHandler = JPackage('org.hti5250j.interfaces').RequestHandler
```

### 7.2 Go Client Library (REST API — Planned P1)

**Status:** ❌ Not implemented
**Effort:** 1 week
**Approach:** Go http client + JSON serialization

```go
// go.mod
module github.com/heymumford/hti5250j-go

// hti5250j/client.go
package hti5250j

import (
    "encoding/json"
    "fmt"
    "io"
    "net/http"
)

type Client struct {
    baseURL string
    client  *http.Client
}

type SessionResponse struct {
    SessionID string `json:"sessionId"`
    Status    string `json:"status"`
}

func (c *Client) CreateSession(host string, port int) (*SessionResponse, error) {
    body := map[string]interface{}{
        "host": host,
        "port": port,
    }

    resp, err := c.post("/sessions", body)
    if err != nil {
        return nil, err
    }

    var result SessionResponse
    json.NewDecoder(resp.Body).Decode(&result)
    return &result, nil
}

func (c *Client) SendKeys(sessionID, keys string) (string, error) {
    body := map[string]string{"keys": keys}
    resp, _ := c.post(fmt.Sprintf("/sessions/%s/keys", sessionID), body)

    var result map[string]string
    json.NewDecoder(resp.Body).Decode(&result)
    return result["text"], nil
}

// Usage
client := NewClient("http://localhost:8080")
session, _ := client.CreateSession("ibmi.example.com", 23)
screen, _ := client.SendKeys(session.SessionID, "CALL PGM(MYAPP)[enter]")
fmt.Println(screen)
```

### 7.3 Node.js/TypeScript Client (REST API — Planned P1)

**Status:** ❌ Not implemented
**Effort:** 1 week

```typescript
// package.json
{
  "name": "hti5250j",
  "version": "0.12.0",
  "dependencies": {
    "axios": "^1.4.0"
  }
}

// src/index.ts
import axios, { AxiosInstance } from 'axios';

interface SessionResponse {
    sessionId: string;
    status: string;
}

export class HTI5250jClient {
    private http: AxiosInstance;

    constructor(baseURL = 'http://localhost:8080/api/v1') {
        this.http = axios.create({ baseURL });
    }

    async createSession(host: string, port: number): Promise<string> {
        const { data } = await this.http.post<SessionResponse>('/sessions', {
            host, port
        });
        return data.sessionId;
    }

    async sendKeys(sessionId: string, keys: string): Promise<string> {
        const { data } = await this.http.post(`/sessions/${sessionId}/keys`, {
            keys
        });
        return data.text;
    }

    async captureScreenshot(sessionId: string): Promise<Buffer> {
        const { data } = await this.http.get(
            `/sessions/${sessionId}/screenshot`,
            { responseType: 'arraybuffer' }
        );
        return data;
    }
}

// Usage
const client = new HTI5250jClient();
const sessionId = await client.createSession('ibmi.example.com', 23);
const screen = await client.sendKeys(sessionId, 'CALL PGM(MYAPP)[enter]');
const screenshot = await client.captureScreenshot(sessionId);
```

---

## 8. Priority Integration Roadmap

### Phase 1: Foundation (Weeks 1-4, Q1 2026)

**Goal:** Establish distribution, REST API, and basic CI/CD

| Item | Effort | Status | Deliverable |
|------|--------|--------|-------------|
| Maven publishing (Maven Central) | 2 days | 📋 Planned | `org.hti5250j:hti5250j:0.12.0` on OSSRH |
| Docker image | 3 days | 📋 Planned | `ghcr.io/heymumford/hti5250j:latest` |
| REST API (Spring Boot) | 2 weeks | 📋 Planned | `/api/v1/sessions`, `/api/v1/workflows` |
| GitHub Actions plugin | 1 week | 📋 Planned | `heymumford/hti5250j-action@v1` |
| Python SDK (JPype) | 1 week | 📋 Planned | `pip install hti5250j` |
| Prometheus metrics | 1 week | 📋 Planned | `/actuator/prometheus` endpoint |

**Success Criteria:**
- ✅ JAR on Maven Central
- ✅ Docker image runs REST API
- ✅ GitHub Action executes workflows
- ✅ Python SDK (basic) works
- ✅ Prometheus scrape config works

---

### Phase 2: CI/CD Expansion (Weeks 5-8, Q1-Q2 2026)

**Goal:** Enterprise CI/CD plugins

| Item | Effort | Status |
|------|--------|--------|
| GitLab CI template | 3 days | 📋 Planned |
| Jenkins plugin | 2 weeks | 📋 Planned |
| Azure DevOps task | 1 week | 📋 Planned |
| Allure report adapter | 5 days | 📋 Planned |
| TestRail integration | 1 week | 📋 Planned |

---

### Phase 3: Observability & Reporting (Weeks 9-12, Q2 2026)

**Goal:** Production-grade monitoring and reporting

| Item | Effort | Status |
|------|--------|--------|
| Datadog APM | 1 week | 📋 Planned |
| Splunk integration | 3 days | 📋 Planned |
| ExtentReports adapter | 5 days | 📋 Planned |
| Jira/Xray integration | 1 week | 📋 Planned |
| Grafana dashboards | 2 days | 📋 Planned |

---

### Phase 4: Advanced Integration (Q2-Q3 2026)

**Goal:** Specialized frameworks and platforms

| Item | Effort | Status |
|------|--------|--------|
| Cucumber/BDD step definitions | 2 weeks | 📋 Planned |
| TestNG support module | 3 days | 📋 Planned |
| Robot Framework enhancements (30+ keywords) | 1 week | 📋 Planned |
| Go SDK | 1 week | 📋 Planned |
| Node.js/TypeScript SDK | 1 week | 📋 Planned |
| Gradle plugin for workflow tasks | 1 week | 📋 Planned |

---

## 9. Integration Implementation Checklist

### Phase 1 Checklist

**Maven Publishing:**
- [ ] Add `maven-publish` and `signing` plugins to build.gradle
- [ ] Create Sonatype OSSRH account
- [ ] Configure GPG signing
- [ ] Tag release as v0.12.0
- [ ] Verify artifact on Maven Central (https://central.sonatype.com)

**Docker Image:**
- [ ] Create Dockerfile (FROM eclipse-temurin:21-jdk-alpine)
- [ ] Build image: `ghcr.io/heymumford/hti5250j:latest`
- [ ] Document image in README.md
- [ ] Push to ghcr.io

**REST API:**
- [ ] Create Spring Boot module (`hti5250j-server`)
- [ ] Implement SessionController (POST /sessions, GET /sessions/{id}, POST /sessions/{id}/keys, etc.)
- [ ] Implement WorkflowController (POST /workflows, GET /workflows/{id})
- [ ] Add OpenAPI/Swagger documentation
- [ ] Test with Postman collection
- [ ] Document in docs/REST_API.md

**GitHub Actions:**
- [ ] Create `actions/hti5250j-run/action.yml`
- [ ] Create `actions/hti5250j-validate/action.yml`
- [ ] Add example workflow in `.github/workflows/example.yml`
- [ ] Document in docs/GITHUB_ACTIONS.md

**Python SDK:**
- [ ] Create `hti5250j-py` directory with `setup.py`, `pyproject.toml`
- [ ] Implement JPype wrappers (Session, Screen, exceptions)
- [ ] Add unit tests
- [ ] Publish to PyPI
- [ ] Document in docs/PYTHON_SDK.md

**Prometheus Metrics:**
- [ ] Add Micrometer dependency to build.gradle
- [ ] Implement HTI5250jMetrics class with counters/gauges/timers
- [ ] Add /actuator/prometheus endpoint (Spring Boot)
- [ ] Document in docs/PROMETHEUS_METRICS.md

---

### Phase 2 Checklist

**GitLab CI:**
- [ ] Create `.gitlab-ci.yml` template
- [ ] Document in docs/GITLAB_CI.md
- [ ] Test with sample repo

**Jenkins Plugin:**
- [ ] Create `jenkins-plugin-hti5250j` directory
- [ ] Implement HTI5250jBuilder extends Builder
- [ ] Add HTI5250jGlobalConfiguration
- [ ] Create Jelly views for UI
- [ ] Publish to Jenkins plugin repository
- [ ] Document in docs/JENKINS_PLUGIN.md

**Azure DevOps:**
- [ ] Create `azure-devops-task-hti5250j` directory
- [ ] Implement task.json + task.ts
- [ ] Package and publish to Azure DevOps Marketplace
- [ ] Document in docs/AZURE_DEVOPS.md

**Allure Report:**
- [ ] Create `hti5250j-allure` module
- [ ] Implement AllureReportAdapter
- [ ] Add test examples
- [ ] Document in docs/ALLURE_REPORTS.md

**TestRail:**
- [ ] Create `hti5250j-testrail` module
- [ ] Implement TestRailReporter
- [ ] Add configuration examples
- [ ] Document in docs/TESTRAIL_INTEGRATION.md

---

## 10. Architectural Decisions & Rationale

### Decision 1: REST API for Cross-Language Support

**Issue:** Java library is powerful but locks clients into JVM. Teams using Python, Go, Node.js, .NET can't consume HTI5250J directly.

**Decision:** Implement REST API via Spring Boot with embedded server.

**Rationale:**
- ✅ Language-agnostic (HTTP is universal)
- ✅ Stateless design (enables load balancing)
- ✅ Proven OpenAPI/Swagger standards
- ✅ Easy Docker deployment

**Alternative Considered:** gRPC
- ❌ Requires client library generation per language
- ❌ Binary protocol harder to debug
- ❌ Not familiar to most teams

**Trade-offs:**
- REST adds HTTP latency (1-5ms vs 0ms in-process)
- Mitigated by connection pooling, batch workflows
- For typical IBM i workflows (1-10 second operations), <1% overhead

---

### Decision 2: Maven Central Publishing

**Issue:** JAR only available locally; can't be consumed as dependency by other projects.

**Decision:** Publish to Maven Central via Sonatype OSSRH.

**Rationale:**
- ✅ Standard JVM dependency distribution
- ✅ Enables `build.gradle: implementation 'org.hti5250j:hti5250j:0.12.0'`
- ✅ Builds trust (public project, reproducible builds)

**Alternative Considered:** Gradle Plugin Portal
- ✅ For build-time integrations (e.g., `apply plugin: 'org.hti5250j.workflow'`)
- ❌ Not suitable for library distribution
- ✅ Recommend as Phase 4 (Gradle plugin for workflow tasks)

---

### Decision 3: Prometheus Metrics Over Custom Monitoring

**Issue:** No observability into production workflows; can't detect performance degradation, connection failures.

**Decision:** Implement Micrometer metrics via standard Prometheus format.

**Rationale:**
- ✅ Standard format (works with Prometheus, Grafana, Datadog, New Relic, etc.)
- ✅ No vendor lock-in
- ✅ Low-overhead counters/gauges/timers
- ✅ Existing Grafana dashboards easy to build

**Alternative Considered:** Custom logging
- ❌ Unstructured, hard to aggregate
- ❌ Requires custom parsing per logging system

---

### Decision 4: Spring Boot for REST API

**Issue:** HTTPServer vs Spring Boot vs Quarkus vs Micronaut?

**Decision:** Spring Boot (spring-boot-starter-webflux with Undertow).

**Rationale:**
- ✅ Most familiar to Java teams
- ✅ Comprehensive ecosystem (Spring Security, Spring Data, etc.)
- ✅ Production-grade (Netflix, Uber, etc.)
- ✅ Easy integration with Prometheus, logging, etc.

**Alternative Considered:** Quarkus
- ✅ Smaller memory footprint (80MB vs 200MB), faster startup
- ❌ Less mature for typical REST APIs
- ❌ Overkill if Docker image is baseline overhead

**Alternative Considered:** Micronaut
- ✅ Better performance than Spring Boot
- ❌ Smaller community, less job market fit

**Trade-off:** Spring Boot adds ~150MB JAR + 200MB at runtime. Acceptable because REST API runs as separate service (not every client needs it).

---

## 11. Security Considerations

### 11.1 REST API Security

| Concern | Mitigation |
|---------|-----------|
| **Credential leakage in logs** | Use Spring Security to mask sensitive parameters |
| **HTTPS not enforced** | Default to TLS 1.3 in Spring Boot config; warn in docs |
| **Session token expiry** | Implement JWT tokens with 1-hour TTL |
| **IBMI password exposure** | Never log credentials; use Spring Vault for secret management |
| **Cross-origin attacks** | Configure CORS with allowlist (e.g., localhost:3000 for dev) |
| **SQL injection** | N/A (no database queries in core) |

**Recommended Config:**
```yaml
# application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    protocol: TLSv1.3

security:
  cors:
    allowed-origins: http://localhost:3000
  jwt:
    secret: ${JWT_SECRET}
    expiration: 3600000  # 1 hour
```

---

### 11.2 Plugin Security

Plugin system is deprecated but if revived:
- ✅ Classloader isolation (each plugin in separate URLClassLoader)
- ✅ Permission restrictions (plugin can't access filesystem directly)
- ⚠️ No code signing (accept unsigned plugins at runtime risk)

---

## 12. Conclusion & Recommendations

### 12.1 Top-5 Priorities (Impact × Effort)

| Priority | Item | Impact | Effort | Ratio |
|----------|------|--------|--------|-------|
| 1 | Maven publishing | 🟢 High (enables all downstream) | 🟡 2 days | 10x |
| 2 | REST API | 🟢 High (cross-language support) | 🔴 2 weeks | 5x |
| 3 | GitHub Actions | 🟢 High (90% of teams use GH) | 🟡 1 week | 5x |
| 4 | Docker image | 🟢 High (containerization standard) | 🟢 3 days | 8x |
| 5 | Python SDK | 🟡 Medium (Python adoption growing) | 🟡 1 week | 3x |

**Recommendation:** Complete Phase 1 (Weeks 1-4) to unlock downstream integrations.

---

### 12.2 Quick-Start Integrations (Minimal Effort)

If short on time, these are "free wins":

1. **Robot Framework enhancements** (add 20+ keywords)
   - Effort: 3 days
   - Unlocks: BDD workflows, enterprise test teams

2. **JUnit 5 assertion helpers**
   - Effort: 2 days
   - Unlocks: Better test ergonomics

3. **GitHub Actions template**
   - Effort: 2 days (reuse Docker image)
   - Unlocks: 90% of CI/CD use cases

---

### 12.3 Long-Term Vision

HTI5250J should become the **standard 5250 terminal emulator** for:
- ✅ JVM automation (Groovy, Kotlin, Scala)
- ✅ Python teams (via JPype + REST API)
- ✅ Go teams (via REST API)
- ✅ DevOps/SRE (Docker, Kubernetes, GitOps)
- ✅ Enterprise testing (TestRail, Jira, ALM)
- ✅ Observability (Prometheus, Datadog, Splunk)

**5-Year Roadmap:**
- Year 1 (2026): Foundation + CI/CD (Maven, REST, GitHub Actions)
- Year 2 (2027): Enterprise plugins (Jenkins, Azure DevOps, TestRail)
- Year 3 (2028): Observability (Datadog, Splunk, Grafana)
- Year 4 (2029): Advanced SDKs (Python, Go, Node.js, .NET)
- Year 5 (2030): Market leadership (1000+ GitHub stars, 10K+ Maven downloads/month)

---

**Document Version:** 1.0
**Last Updated:** February 2026
**Next Review:** May 2026 (post-Phase 1)
