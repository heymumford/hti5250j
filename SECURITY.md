# Security Policy for HTI5250J

## Overview

HTI5250J implements comprehensive security scanning and vulnerability detection through GitHub Actions CI/CD pipelines. This document describes our security practices and scanning infrastructure.

## Security Scanning Infrastructure

### 1. CodeQL Analysis
**Tool**: GitHub's CodeQL (static analysis)
**Frequency**:
- On every push to main/master/feature/* branches
- On every pull request
- Daily scheduled scan at 2 AM UTC

**Coverage**: Java/Kotlin code
**Detects**:
- SQL injection vulnerabilities
- Cross-site scripting (XSS) patterns
- Command injection risks
- Unsafe serialization
- Null pointer dereferences
- Resource leaks
- Integer overflow issues

**Workflow**: `.github/workflows/codeql.yml`

### 2. Semgrep Security Audit
**Tool**: Semgrep (lightweight static analysis)
**Frequency**:
- On every push
- On every pull request
- Daily scheduled scan at 3 AM UTC

**Rule Sets**:
- `p/security-audit` — Security best practices
- `p/java` — Java language rules
- `p/owasp-top-ten` — OWASP Top 10 patterns

**Configuration**: `.semgrep.yml`

**Detects**:
- Hardcoded credentials/secrets
- Insecure cryptographic operations
- Unvalidated input handling
- Authentication bypass patterns
- Information disclosure risks
- Unsafe reflection/serialization

**Workflow**: `.github/workflows/semgrep.yml`

### 3. CI/CD Integration Security
**Workflow**: `.github/workflows/ci.yml`

Includes integrated Semgrep checks with every commit:
- Runs alongside unit tests
- Uploads results to GitHub Security tab
- Generates SARIF reports for all findings

## Vulnerability Reporting

### Private Security Disclosure
If you discover a security vulnerability, please email: **security@hti5250j.dev**

Do **NOT** open a public issue or PR for security vulnerabilities.

**Include**:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if available)

**Response Time**: 48 hours

### Supported Versions
Security updates are provided for:
- Current stable release
- Previous minor version (N-1)

## Security Best Practices

### Code-Level
- ✅ Use parameterized SQL queries (never string concatenation)
- ✅ Validate all external input (network, files, CLI args)
- ✅ Use strong cryptography (TLS 1.3+, AES-256)
- ✅ Handle sensitive data carefully (scrub logs, avoid String storage)
- ✅ Use security manager for Java applet contexts
- ✅ Review third-party dependencies monthly

### CI/CD-Level
- ✅ All workflows run with minimal permissions
- ✅ Dependency updates via Dependabot (automated security patches)
- ✅ Branch protection rules (require security scans before merge)
- ✅ SARIF artifact retention (90 days for audit trail)

### Dependency Management
HTI5250J uses Dependabot to automatically:
1. Detect vulnerable dependencies
2. Create PRs with security patches
3. Require security scan approval before merge

**Current Dependencies**: See `build.gradle` for exact versions

### Telnet Protocol Security
HTI5250J implements the TN5250 terminal emulation protocol. Security considerations:

- **Encryption**: Use TLS for all production deployments
- **Authentication**: Credentials validated against IBM i security mechanisms
- **Data Integrity**: Protocol includes checksums for data verification
- **Session Management**: Automatic timeout after inactivity

## Security Scanning Results

### Accessing Results
1. **GitHub Security Tab**: All SARIF results from CodeQL and Semgrep
2. **Pull Request Checks**: Security status required before merge
3. **Scheduled Reports**: Daily scan results available in Actions tab

### Interpreting Findings

**HIGH Severity**:
- Fix before merge
- May indicate exploitable vulnerability
- Escalate if in dependency

**MEDIUM Severity**:
- Address in current sprint
- Low exploitability but noteworthy risk
- Document if accepted risk

**LOW Severity**:
- Consider fixing
- May be false positive
- Safe to defer

## Compliance & Standards

- **OWASP Top 10**: Core scanning rule set
- **CWE Coverage**: Top 25 weaknesses detected
- **Java Security**: Following Oracle security guidelines
- **Telnet Protocol**: RFC 854/RFC 855 compliant

## Questions or Concerns?

Please refer to:
- **Security Issues**: security@hti5250j.dev
- **General Questions**: GitHub Discussions
- **Code Review**: GitHub Pull Request review process

---

**Last Updated**: February 8, 2026
**Maintained By**: HTI5250J Security Team
