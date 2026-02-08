# Contributing to TN5250J Headless

This repository is a maintained fork of [TN5250J](https://github.com/tn5250j/tn5250j). Contributions must comply with GPL-2.0-or-later and preserve upstream attribution.

## Project and License Summary

**Upstream (TN5250J):**
- Repository: https://github.com/tn5250j/tn5250j
- Authors: TN5250J Community
- License: GPL-2.0-or-later

**This Fork (TN5250J Headless):**
- Repository: https://github.com/heymumford/tn5250j
- Maintainer: Eric C. Mumford (@heymumford)
- License: GPL-2.0-or-later

## Contribution Guidelines

1. Fork from this repository (not the upstream repo).
2. Create a feature branch: `feature/<short-description>`.
3. Preserve existing attribution headers and add your own where appropriate.
4. Run tests and add coverage for new behavior (`./gradlew test`).
5. Write commits that explain the intent, not just the diff.

## Versioning Policy (Fork SemVer)

This fork follows SemVer with an upstream‑anchored pre‑release suffix:

- Base version tracks upstream: `UPSTREAM_VERSION`
- Fork releases add a suffix: `-headless.N`
- Optional build metadata links upstream: `+upstream.<tag>.<sha>`

Examples:

- `0.8.0-headless.1`
- `0.8.0-headless.2+upstream.0.8.0.abcdef`

Increment rules:

- MAJOR: breaking changes to the fork’s public APIs or workflows.
- MINOR: backward‑compatible features.
- PATCH: backward‑compatible fixes.

While upstream remains `0.y.z`, minor bumps may still introduce breaking changes per SemVer guidance for pre‑1.0 releases.

## SPDX Header Example

Use SPDX headers for new files and when updating legacy headers:

```java
/*
 * SPDX-FileCopyrightText: 2001-2004 TN5250J Community
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Your Name <you@example.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
```

## Pull Request Checklist

- Clear problem statement and rationale
- Tests added or updated, with results reported
- Attribution preserved (SPDX headers)
- License compatibility confirmed (GPL-2.0-or-later)

## Questions

- Upstream issues: https://github.com/tn5250j/tn5250j/issues
- Fork issues: https://github.com/heymumford/tn5250j/issues
- License text: [LICENSE](./LICENSE)

## Legal

By contributing, you confirm:
- You have the right to contribute the code.
- Your contributions are licensed under GPL-2.0-or-later.
- You accept that attribution will be preserved.
