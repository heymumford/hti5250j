# Security Policy for HTI5250J

## Overview

HTI5250J uses GitHub Actions CI/CD pipelines for security scanning and vulnerability detection.

## Security Scanning

### CodeQL Analysis
- **Frequency**: On push, on pull request, daily scheduled scan
- **Coverage**: Java/Kotlin code
- **Detects**: SQL injection, XSS, command injection, unsafe serialization, resource leaks, integer overflow
- **Workflow**: `.github/workflows/codeql.yml`

### Semgrep Security Audit
- **Frequency**: On push, on pull request, daily scheduled scan
- **Rule Sets**: `p/security-audit`, `p/java`, `p/owasp-top-ten`
- **Detects**: Hardcoded credentials, insecure crypto, unvalidated input, auth bypass, information disclosure
- **Configuration**: `.semgrep.yml`
- **Workflow**: `.github/workflows/semgrep.yml`

### CI Integration
- Semgrep runs alongside unit tests in `.github/workflows/ci.yml`
- Results uploaded to GitHub Security tab as SARIF reports

## Vulnerability Reporting

If you discover a security vulnerability, please report it through [GitHub's private vulnerability reporting](https://github.com/heymumford/hti5250j/security/advisories/new).

Do **NOT** open a public issue or PR for security vulnerabilities.

Please include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if available)

### Supported Versions
Security updates are provided for the current stable release and previous minor version (N-1).

## Security Best Practices

### Code-Level
- Use parameterized SQL queries (never string concatenation)
- Validate all external input (network, files, CLI args)
- Use strong cryptography (TLS 1.3+, AES-256)
- Handle sensitive data carefully (scrub logs, avoid String storage)
- Review third-party dependencies regularly

### CI/CD-Level
- All workflows run with minimal permissions
- Dependency updates via Dependabot (automated security patches)
- Branch protection rules require security scans before merge

### Telnet Protocol Security
HTI5250J implements the TN5250 terminal emulation protocol. Security considerations:

- **Encryption**: Use TLS for all production deployments
- **Authentication**: Credentials validated against IBM i security mechanisms
- **Data Integrity**: Protocol includes checksums for data verification
- **Session Management**: Automatic timeout after inactivity

## Compliance

- **OWASP Top 10**: Core scanning rule set
- **CWE Coverage**: Top 25 weaknesses detected
- **Java Security**: Following Oracle security guidelines
- **Telnet Protocol**: RFC 854/RFC 855 compliant

## Questions or Concerns?

- **Security Issues**: Use [GitHub Security Advisories](https://github.com/heymumford/hti5250j/security/advisories/new)
- **General Questions**: [GitHub Discussions](https://github.com/heymumford/hti5250j/discussions)
- **Code Review**: GitHub Pull Request review process
