# HTI5250J Integration Roadmap 2026

**Status:** Q1 2026 Planning
**Owner:** Integration Architects
**Audience:** Product, Engineering, Community

---

## Phase 1: Foundation (Q1 2026, Weeks 1-4)

### Goals
- Establish Maven Central distribution
- Implement REST API for cross-language clients
- Publish Docker image
- Create GitHub Actions plugin
- Build Python SDK (basic)

### Deliverables

#### 1.1 Maven Publishing (2 days)

**What:** Publish JAR to Maven Central via Sonatype OSSRH

**Acceptance Criteria:**
- [ ] `org.hti5250j:hti5250j:0.12.0` available on mvnrepository.com
- [ ] `build.gradle` includes maven-publish + signing plugins
- [ ] Release notes published on GitHub
- [ ] README.md updated with Maven dependency snippet

**Tasks:**
1. Create Sonatype OSSRH account (if not exists)
2. Add to `build.gradle`:
```gradle
plugins {
    id 'maven-publish'
    id 'signing'
}

publishing {
    repositories {
        maven {
            name = "OSSRH"
            url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
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
                // ... license, developers, etc.
            }
        }
    }
}

signing {
    sign publishing.publications.mavenJava
}
```
3. Create GitHub Actions release workflow
4. Tag v0.12.0 and push
5. Verify JAR on Maven Central

**Owner:** DevOps / Release Manager
**Timeline:** Week 1

---

#### 1.2 Docker Image (3 days)

**What:** Container image running REST API server

**Acceptance Criteria:**
- [ ] Image available at `ghcr.io/heymumford/hti5250j:latest`
- [ ] Docker Compose file for multi-session cluster
- [ ] README.md has Docker quick-start
- [ ] Image builds in under 2 minutes

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy JAR from Maven publishing step
COPY dist/hti5250j-rest-0.12.0.jar app.jar

# REST API server runs on port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health

ENTRYPOINT ["java", "-jar", "app.jar", \
    "-Dspring.profiles.active=docker", \
    "-Dmanagement.endpoints.web.exposure.include=health,metrics,prometheus"]
```

**docker-compose.yml:**
```yaml
version: '3'
services:
  hti5250j:
    image: ghcr.io/heymumford/hti5250j:latest
    ports:
      - "8080:8080"
    environment:
      JAVA_OPTS: "-Xmx512m"
    networks:
      - hti5250j

  prometheus:
    image: prom/prometheus:latest
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
    networks:
      - hti5250j

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    networks:
      - hti5250j

networks:
  hti5250j:
```

**Owner:** DevOps
**Timeline:** Week 1

---

#### 1.3 REST API (2 weeks)

**What:** Spring Boot HTTP server for session/workflow management

**Acceptance Criteria:**
- [ ] All endpoints in Section 4.2 (INTEGRATION_ARCHITECTURE_REVIEW.md) implemented
- [ ] OpenAPI/Swagger docs at `/api/docs`
- [ ] Postman collection published
- [ ] Basic auth or JWT token support
- [ ] Unit tests for all endpoints (JUnit 5)
- [ ] Integration tests with mock IBM i
- [ ] 200+ requests/sec throughput (load test)

**Module Structure:**
```
hti5250j-rest/
├── src/main/java/org/hti5250j/rest/
│   ├── SessionController.java
│   ├── WorkflowController.java
│   ├── BatchController.java
│   ├── HealthController.java
│   ├── dto/
│   │   ├── SessionRequest.java
│   │   ├── SessionResponse.java
│   │   ├── SendKeysRequest.java
│   │   ├── ScreenResponse.java
│   │   └── ... (other DTOs)
│   ├── config/
│   │   ├── RestConfiguration.java
│   │   ├── SwaggerConfiguration.java
│   │   └── SecurityConfiguration.java
│   └── service/
│       ├── SessionService.java
│       ├── WorkflowService.java
│       └── ScreenService.java
├── src/test/java/org/hti5250j/rest/
│   ├── SessionControllerTest.java
│   ├── WorkflowControllerTest.java
│   └── IntegrationTest.java
└── build.gradle
```

**Key Implementation:**

```java
// SessionController.java
@RestController
@RequestMapping("/api/v1/sessions")
@Slf4j
public class SessionController {

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
        @RequestBody SessionRequest request
    ) {
        // Validate request
        // Create Session5250 with request params
        // Store in sessionStore (ConcurrentHashMap)
        // Return 201 CREATED with sessionId
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(
        @PathVariable String sessionId
    ) {
        // Retrieve session from store
        // Return current state (connected, screen, OIA)
    }

    @PostMapping("/{sessionId}/keys")
    public ResponseEntity<ScreenResponse> sendKeys(
        @PathVariable String sessionId,
        @RequestBody SendKeysRequest request
    ) {
        // Send keys to session
        // Wait for keyboard availability
        // Return screen text
    }

    @PostMapping("/{sessionId}/disconnect")
    public ResponseEntity<Void> disconnect(
        @PathVariable String sessionId
    ) {
        // Disconnect and cleanup
        // Return 204 NO_CONTENT
    }
}
```

**Tests:**
```java
@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @MockBean
    private SessionService sessionService;

    @Test
    void testCreateSessionReturns201Created() {
        // Given
        SessionRequest request = new SessionRequest("ibmi.example.com", 23);
        SessionResponse response = new SessionResponse("sess-123", "CREATED");
        when(sessionService.createSession(request))
            .thenReturn(response);

        // When
        mockMvc.perform(post("/api/v1/sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").value("sess-123"));

        // Then
        verify(sessionService).createSession(request);
    }
}
```

**Owner:** Backend Engineer
**Timeline:** Weeks 2-3

---

#### 1.4 GitHub Actions Plugin (1 week)

**What:** Reusable action for running HTI5250J workflows

**Acceptance Criteria:**
- [ ] Action published at `heymumford/hti5250j-action@v1`
- [ ] Supports `run` and `validate` modes
- [ ] Inputs: workflow-file, data-file, ibmi-host, ibmi-user, ibmi-password
- [ ] Outputs: artifacts-path, success (boolean)
- [ ] Works in Docker-based and macos/ubuntu runners
- [ ] 5+ example workflows in .github/workflows/

**action.yml:**
```yaml
name: HTI5250J Workflow Runner
description: Execute HTI5250J workflows against IBM i

inputs:
  action:
    description: 'Action: run, validate, or simulate'
    required: true
    default: 'validate'
  workflow-file:
    description: 'Path to YAML workflow definition'
    required: true
  data-file:
    description: 'Path to CSV data file (optional)'
    required: false
  ibmi-host:
    description: 'IBM i hostname or IP'
    required: true
  ibmi-user:
    description: 'IBM i user (plain text or secret)'
    required: true
  ibmi-password:
    description: 'IBM i password (use secrets.IBMI_PASSWORD)'
    required: true
  timeout:
    description: 'Timeout in seconds'
    required: false
    default: '300'

outputs:
  artifacts-path:
    description: 'Path to artifacts/ directory'
    value: ${{ steps.run.outputs.artifacts }}
  success:
    description: 'Workflow execution successful'
    value: ${{ steps.run.outputs.success }}
  ledger:
    description: 'Execution ledger (stdout)'
    value: ${{ steps.run.outputs.ledger }}

runs:
  using: docker
  image: docker://ghcr.io/heymumford/hti5250j:latest
  args:
    - ${{ inputs.action }}
    - ${{ inputs.workflow-file }}
    - --data
    - ${{ inputs.data-file }}
```

**Example Workflow:**
```yaml
# .github/workflows/test-payment-workflow.yml
name: Payment Workflow Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Validate Workflow
        uses: heymumford/hti5250j-action@v1
        with:
          action: validate
          workflow-file: examples/payment.yaml
          data-file: examples/payment_data.csv
          ibmi-host: ${{ secrets.IBMI_HOST }}
          ibmi-user: ${{ secrets.IBMI_USER }}
          ibmi-password: ${{ secrets.IBMI_PASSWORD }}

      - name: Run Workflow
        uses: heymumford/hti5250j-action@v1
        with:
          action: run
          workflow-file: examples/payment.yaml
          data-file: examples/payment_data.csv
          ibmi-host: ${{ secrets.IBMI_HOST }}
          ibmi-user: ${{ secrets.IBMI_USER }}
          ibmi-password: ${{ secrets.IBMI_PASSWORD }}

      - name: Upload Artifacts
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: workflow-artifacts
          path: artifacts/
```

**Owner:** DevOps
**Timeline:** Week 4

---

#### 1.5 Python SDK (1 week)

**What:** Native Python bindings via JPype (no Jython required)

**Acceptance Criteria:**
- [ ] `pip install hti5250j` works
- [ ] Full API coverage (Session, Screen, exceptions)
- [ ] Type hints for IDE autocomplete
- [ ] 10+ example scripts in examples/
- [ ] Documentation in docs/PYTHON_SDK.md
- [ ] Unit tests for wrapper layer

**Package Structure:**
```
hti5250j-py/
├── setup.py
├── pyproject.toml
├── README.md
├── hti5250j/
│   ├── __init__.py
│   ├── session.py
│   ├── screen.py
│   ├── exceptions.py
│   ├── workflow.py
│   └── types.py
├── tests/
│   ├── test_session.py
│   ├── test_screen.py
│   └── test_integration.py
└── examples/
    ├── basic_session.py
    ├── workflow_execution.py
    ├── batch_processing.py
    └── custom_request_handler.py
```

**Key Implementation:**

```python
# hti5250j/session.py
from typing import Optional
import jpype
from jpype.types import JObject

class Session:
    """Python wrapper for HeadlessSession"""

    def __init__(self, host: str, port: int = 23, code_page: int = 37):
        if not jpype.isJVMStarted():
            jpype.startJVM()

        self._host = host
        self._port = port

        # Import Java classes
        SessionFactory = jpype.JClass(
            'org.hti5250j.interfaces.HeadlessSessionFactory'
        )

        # Create session
        self._session = SessionFactory.createSession(
            f"{host}-{port}",
            {"host": host, "port": str(port), "code_page": str(code_page)}
        )

    def connect(self):
        """Connect to IBM i system"""
        self._session.connect()

    def disconnect(self):
        """Disconnect from IBM i system"""
        self._session.disconnect()

    def send_keys(self, keys: str):
        """Send keys to terminal"""
        self._session.sendKeys(keys)

    def get_screen_text(self) -> str:
        """Get current screen as text"""
        return self._session.getScreenAsText()

    def wait_for_keyboard_unlock(self, timeout_ms: int = 5000):
        """Wait for keyboard to become available"""
        try:
            self._session.waitForKeyboardUnlock(timeout_ms)
        except jpype.JException as e:
            raise TimeoutError(f"Keyboard unlock timeout: {timeout_ms}ms")

    def is_connected(self) -> bool:
        """Check if connected to IBM i"""
        return self._session.isConnected()

# Usage example
if __name__ == "__main__":
    session = Session("ibmi.example.com")
    session.connect()
    session.wait_for_keyboard_unlock(30000)

    session.send_keys("CALL PGM(MYAPP)[enter]")
    screen = session.get_screen_text()
    print(screen)

    session.disconnect()
```

**Tests:**
```python
# tests/test_session.py
import pytest
from hti5250j import Session

@pytest.fixture
def session():
    """Create session fixture"""
    s = Session("mock-ibmi", code_page=37)
    yield s
    s.disconnect()

def test_connect_succeeds(session):
    """Test session connection"""
    session.connect()
    assert session.is_connected()

def test_send_keys_succeeds(session):
    """Test sending keys"""
    session.connect()
    session.send_keys("CALL PGM(TEST)[enter]")
    # Assertion would depend on mock

def test_get_screen_text_succeeds(session):
    """Test getting screen text"""
    session.connect()
    screen = session.get_screen_text()
    assert isinstance(screen, str)
    assert len(screen) > 0
```

**Owner:** Python Developer
**Timeline:** Week 4

---

#### 1.6 Prometheus Metrics (1 week)

**What:** Micrometer metrics for observability

**Acceptance Criteria:**
- [ ] Metrics exposed at `/actuator/prometheus`
- [ ] 10+ counters/gauges/timers (see INTEGRATION_ARCHITECTURE_REVIEW.md section 5.1)
- [ ] Latency percentiles (p50, p95, p99)
- [ ] Success/failure rate tracking
- [ ] Prometheus scrape config works
- [ ] Grafana dashboard template provided

**Implementation:**

```java
// src/main/java/org/hti5250j/metrics/HTI5250jMetrics.java
@Component
@Slf4j
public class HTI5250jMetrics {
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeSessions;
    private final Counter sessionCreatedCounter;
    private final Counter sessionErrorCounter;
    private final Timer workflowDurationTimer;
    private final DistributionSummary screenTextLengthSummary;

    public HTI5250jMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.activeSessions = meterRegistry.gauge(
            "hti5250j.sessions.active",
            new AtomicInteger(0)
        );

        this.sessionCreatedCounter = Counter.builder("hti5250j.sessions.created")
            .description("Total sessions created")
            .register(meterRegistry);

        this.sessionErrorCounter = Counter.builder("hti5250j.sessions.errors")
            .description("Session connection errors")
            .register(meterRegistry);

        this.workflowDurationTimer = Timer.builder("hti5250j.workflow.duration")
            .description("Workflow execution time")
            .publishPercentiles(0.50, 0.95, 0.99)
            .register(meterRegistry);

        this.screenTextLengthSummary = DistributionSummary
            .builder("hti5250j.screen.text.length")
            .description("Characters in screen text")
            .register(meterRegistry);
    }

    public void recordSessionCreated() {
        sessionCreatedCounter.increment();
        activeSessions.incrementAndGet();
    }

    public void recordSessionError() {
        sessionErrorCounter.increment();
    }

    public void recordSessionDisconnected() {
        activeSessions.decrementAndGet();
    }

    public void recordWorkflowDuration(long durationMs) {
        workflowDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordScreenText(String text) {
        screenTextLengthSummary.record(text.length());
    }
}
```

**Prometheus Config:**
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'hti5250j'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

**Grafana Dashboard (JSON):**
```json
{
  "dashboard": {
    "title": "HTI5250J Metrics",
    "panels": [
      {
        "title": "Active Sessions",
        "targets": [{ "expr": "hti5250j_sessions_active" }]
      },
      {
        "title": "Workflow P50 Latency",
        "targets": [{ "expr": "hti5250j_workflow_duration_seconds{quantile=\"0.50\"}" }]
      },
      {
        "title": "Session Errors",
        "targets": [{ "expr": "increase(hti5250j_sessions_errors_total[5m])" }]
      }
    ]
  }
}
```

**Owner:** Backend Engineer
**Timeline:** Week 3

---

### Phase 1 Summary

| Item | Owner | Timeline | Deliverables |
|------|-------|----------|--------------|
| Maven publishing | DevOps | Week 1 | JAR on Maven Central |
| Docker image | DevOps | Week 1 | ghcr.io image + compose |
| REST API | Backend | Weeks 2-3 | /api/v1 endpoints + Swagger |
| GitHub Actions | DevOps | Week 4 | Action published + examples |
| Python SDK | Python Dev | Week 4 | PyPI package + docs |
| Prometheus | Backend | Week 3 | /actuator/prometheus + dashboard |

**Phase 1 Completion Criteria:**
- ✅ All 6 items above completed
- ✅ 10+ example workflows for each integration
- ✅ Documentation complete (README, docs/*.md)
- ✅ 95%+ test coverage for REST API + Python SDK
- ✅ Load test passes (REST API: 200+ req/sec, Docker: 1000+ concurrent sessions)
- ✅ GitHub release tag created (v0.12.0)

---

## Phase 2: CI/CD Expansion (Q2 2026, Weeks 5-8)

### Goals
- Support GitLab CI, Jenkins, Azure DevOps
- Test reporting (Allure, TestRail)
- Extend Robot Framework (30+ keywords)

### Deliverables

#### 2.1 GitLab CI Template (3 days)

**Deliverable:** `.gitlab-ci.yml` template + documentation

```yaml
# .gitlab-ci.yml
stages:
  - validate
  - test
  - report

validate_workflow:
  stage: validate
  image: ghcr.io/heymumford/hti5250j:latest
  script:
    - i5250 validate examples/payment.yaml --data examples/payment_data.csv
  only:
    - merge_requests

run_workflow:
  stage: test
  image: ghcr.io/heymumford/hti5250j:latest
  script:
    - i5250 run examples/payment.yaml --data examples/payment_data.csv
  artifacts:
    paths:
      - artifacts/**
    reports:
      junit: artifacts/junit.xml
  environment:
    name: ibmi-prod
  only:
    - main
```

**Owner:** DevOps
**Timeline:** Week 5

#### 2.2 Jenkins Plugin (2 weeks)

**Deliverable:** Jenkins plugin published to Jenkins marketplace

**Plugin Structure:**
```
jenkins-plugin-hti5250j/
├── src/main/java/
│   ├── HTI5250jBuilder.java
│   ├── HTI5250jGlobalConfiguration.java
│   └── HTI5250jBuildAction.java
├── src/main/resources/
│   ├── HTI5250jBuilder/
│   │   ├── config.jelly
│   │   └── help-xxx.html
├── pom.xml
└── README.md
```

**Owner:** Backend Engineer
**Timeline:** Weeks 5-7

#### 2.3 Azure DevOps Task (1 week)

**Deliverable:** Task published to Azure DevOps Marketplace

**Owner:** DevOps
**Timeline:** Week 8

#### 2.4 Allure Report Adapter (5 days)

**Deliverable:** `hti5250j-allure` module with test report integration

**Owner:** Backend Engineer
**Timeline:** Week 5-6

#### 2.5 TestRail Integration (1 week)

**Deliverable:** `hti5250j-testrail` module for test result sync

**Owner:** Backend Engineer
**Timeline:** Week 7-8

#### 2.6 Robot Framework Enhancements (1 week)

**Deliverable:** 30+ keywords (from current 11)

New keywords:
- `Fill Field By Label`
- `Assert Field Value`
- `Capture Screen Diff`
- `Wait For Field Value`
- `Navigate To Screen By Menu`
- `Execute Function Key`
- `Get Field Attributes`
- `Set Screen Size`
- `Get OIA State`
- ... (20+ more)

**Owner:** Robot Framework Expert
**Timeline:** Week 6-7

---

## Phase 3: Observability & Reporting (Q3 2026, Weeks 9-12)

### Goals
- Datadog APM
- Splunk logging
- ExtentReports
- Jira/Xray integration

### Timeline
- Week 9: Datadog APM
- Week 10: Splunk logging
- Week 11: ExtentReports + Jira/Xray
- Week 12: Buffer/refinement

---

## Phase 4: Advanced Integration (Q3-Q4 2026)

### Goals
- Cucumber/BDD
- Go SDK
- Node.js SDK
- Gradle plugin

### Timeline
- Q3: Cucumber, Go SDK, Node.js SDK
- Q4: Gradle plugin + polish

---

## Success Metrics

### Phase 1
- [ ] Maven downloads: 100+
- [ ] Docker image pulls: 50+
- [ ] GitHub Actions usage: 5+ workflows
- [ ] Python SDK usage: 10+ installs
- [ ] Prometheus dashboard created: 5+ instances

### Phase 2
- [ ] Jenkins plugin: 20+ installs
- [ ] GitLab CI workflows: 10+
- [ ] TestRail syncs: 5+ projects
- [ ] Robot Framework workflows: 20+

### Phase 3
- [ ] Datadog integrations: 5+
- [ ] Splunk indexes: 10+
- [ ] Production monitoring: 50+ workflows

### Phase 4
- [ ] Go SDK usage: 50+
- [ ] Node.js SDK usage: 50+
- [ ] Gradle plugin: 10+

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| **REST API performance degradation** | Load test weekly; set SLO of 200 req/sec |
| **Plugin compatibility breaks** | Maintain 2-version compatibility; test against v0.10.0+ |
| **Security vulnerabilities in dependencies** | Dependabot enabled; monthly dependency updates |
| **Team capacity constraints** | Prioritize Phase 1 items; defer Phase 4 if needed |
| **IBM i environment issues** | Use mocks for CI/CD; real i5 for staging only |

---

## Budget & Resource Planning

### Phase 1 (4 weeks, 1 FTE)
- 1 Backend Engineer (REST API, metrics)
- 1 DevOps (Maven, Docker, GitHub Actions)
- 1 Python Developer (SDK)
- Effort: 4 weeks

### Phase 2 (4 weeks, 1 FTE)
- 1 Backend Engineer (Jenkins, Allure, TestRail)
- 1 DevOps (GitLab, Azure DevOps)
- 1 Robot Framework Expert (enhancements)
- Effort: 4 weeks

### Phase 3 (4 weeks, 1 FTE)
- 1 Backend Engineer (Datadog, Splunk, ExtentReports)
- 1 DevOps (monitoring setup)
- Effort: 4 weeks

### Phase 4 (8 weeks, 1.5 FTE)
- 2 Backend Engineers (Go SDK, Node.js SDK, Gradle plugin)
- Effort: 8 weeks

**Total:** 20 weeks, ~5 FTE, Q1-Q4 2026

---

## Communication Plan

| Stakeholder | Frequency | Channel |
|-------------|-----------|---------|
| Community | Monthly | GitHub discussions, releases |
| Enterprise users | Weekly | Email + Slack updates |
| Integration partners | Bi-weekly | Partner calls |
| Internal team | Daily | Standups + Slack |

---

**Document Version:** 1.0
**Last Updated:** February 2026
**Status:** Ready for Implementation
