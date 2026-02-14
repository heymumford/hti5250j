# Deployment Guide - v0.13.0

**Version**: 0.13.0
**Release Date**: February 14, 2026
**Artifact**: tn5250j-headless-0.13.0.jar
**Status**: Production Ready

---

## Quick Start

### Build Artifacts

Main library JAR:
```bash
./gradlew clean jar -Pversion=0.13.0
# Output: build/libs/tn5250j-headless-0.13.0.jar (745 KB)
```

### Maven Dependency

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.hti5250j</groupId>
    <artifactId>tn5250j-headless</artifactId>
    <version>0.13.0</version>
</dependency>
```

### Gradle Dependency

Add to `build.gradle`:
```gradle
dependencies {
    implementation 'org.hti5250j:tn5250j-headless:0.13.0'
}
```

---

## Deployment Options

### Option 1: Maven Central Repository

```bash
# Configure your credentials in ~/.m2/settings.xml
# Then deploy:
./gradlew publishToMavenLocal

# For production Maven Central upload:
./gradlew publish
```

### Option 2: Artifactory/Nexus Repository

```bash
# Configure repository URL in build.gradle:
repositories {
    maven {
        url "https://your-repo.com/artifactory/release"
        credentials {
            username project.artifactoryUsername
            password project.artifactoryPassword
        }
    }
}

# Deploy:
./gradlew publish \
    -Partifactory.url=https://your-repo.com/artifactory \
    -Partifactory.username=your-username \
    -Partifactory.password=your-password
```

### Option 3: GitHub Packages

```bash
# Configure in build.gradle
repositories {
    maven {
        url "https://maven.pkg.github.com/heymumford/hti5250j"
        credentials {
            username "your-username"
            password "github-token"
        }
    }
}

# Deploy:
./gradlew publish
```

### Option 4: Direct JAR Distribution

```bash
# Build JAR
./gradlew clean jar -Pversion=0.13.0

# Distribute JAR
cp build/libs/tn5250j-headless-0.13.0.jar /path/to/distribution

# Users can add to classpath:
java -cp tn5250j-headless-0.13.0.jar:other-libs/* com.example.Application
```

---

## Pre-Deployment Verification

### Local Verification

```bash
# Verify build
./gradlew clean compileJava
echo "✅ Main source compiles"

# Verify tests pass in CI
gh run list --repo heymumford/hti5250j --branch main --limit 1 --json conclusion
# Expected: "success"

# Verify Git tag
git describe --tags --abbrev=0
# Expected: v0.13.0
```

### Quality Gates Check

```bash
# Verify all required checks passed
gh pr checks 21 --json name,state | jq '.[] | select(.state == "SUCCESS") | .name'

# Expected: 8 successful checks
# - test (encoding)
# - test (framework-tn5250)
# - test (workflow)
# - test (transport-security)
# - test (framework-core)
# - test (gui-tools)
# - performance
# - security
```

---

## Deployment Checklist

Before deploying, verify:

- [ ] Version number is 0.13.0
- [ ] Git tag v0.13.0 exists
- [ ] GitHub release published
- [ ] All CI checks passing (8/8)
- [ ] JAR built successfully (745 KB)
- [ ] Deployment target configured
- [ ] Credentials/tokens available
- [ ] Documentation updated
- [ ] RELEASE_NOTES.md current
- [ ] CHANGELOG.md updated

---

## Post-Deployment Verification

### Verify Repository Upload

```bash
# Check Maven Central (after sync, ~10 minutes)
curl -s https://repo1.maven.org/maven2/org/hti5250j/tn5250j-headless/0.13.0/
# or check your repository directly

# Download and verify
./gradlew dependencies | grep "org.hti5250j:tn5250j-headless:0.13.0"
```

### Verify Usage

```bash
# In a test project, add dependency and verify resolution
mvn dependency:tree | grep hti5250j
# or
./gradlew dependencies | grep hti5250j
```

---

## Rollback Plan

If deployment issues detected:

```bash
# Revert to previous version
git checkout v0.12.0

# Build previous JAR
./gradlew clean jar -Pversion=0.12.0

# If published, remove/deprecate release:
gh release delete v0.13.0 --cleanup-tag
# or mark as pre-release in repository manager
```

---

## Environment Variables

For scripted deployment:

```bash
# Maven Central
export MAVEN_USERNAME="your-username"
export MAVEN_PASSWORD="your-token"

# Artifactory
export ARTIFACTORY_URL="https://your-repo.com/artifactory"
export ARTIFACTORY_USERNAME="your-username"
export ARTIFACTORY_PASSWORD="your-token"

# GitHub
export GITHUB_TOKEN="your-github-token"
export GITHUB_ACTOR="your-username"

# Then deploy:
./gradlew publish
```

---

## Build Specifications

**Language**: Java 21+
**Build Tool**: Gradle 8.0+
**Library Size**: 745 KB (JAR)
**Dependencies**: Minimal (Gson 2.10.1, test dependencies excluded)
**Supported Platforms**: Linux, macOS, Windows

---

## Support

- **Documentation**: README.md, TESTING.md, docs/
- **Issues**: GitHub Issues
- **Discussions**: GitHub Discussions
- **Release Notes**: RELEASE_NOTES.md
- **Deployment Notes**: This file (DEPLOYMENT_GUIDE.md)

---

## Version Information

| Component | Version |
|-----------|---------|
| Library | 0.13.0 |
| Java | 21+ |
| Gradle | 8.0+ |
| Group ID | org.hti5250j |
| Artifact ID | tn5250j-headless |

---

**Status**: ✅ Production Ready for Deployment
**Next Review**: Monthly artifact repository monitoring
