# TN5250J Headless Edition

Headless-first fork of TN5250J, a 5250 terminal emulator for IBM i (AS/400). This fork prioritizes headless execution, deterministic session management, test automation, and protocol extensions.

Note: This is a maintained fork of [TN5250J](https://github.com/tn5250j/tn5250j). See [FORK.md](./FORK.md) for attribution and differences.

## Quick Start

```bash
git clone https://github.com/heymumford/tn5250j-headless.git
cd tn5250j-headless
./gradlew clean test
```

## Features

- Headless execution (no GUI dependency)
- Session pooling and lifecycle validation
- Pairwise protocol tests (25+ suites)
- Plugin architecture for protocol handlers
- Diagnostics and structured logging
- TN5250E and attribute-plane operations

Upstream feature list: [tn5250j.github.io](https://tn5250j.github.io/)

## Documentation

- [REQUIREMENTS.md](./REQUIREMENTS.md) — canonical feature set and requirements
- [TEST_ARCHITECTURE.md](./TEST_ARCHITECTURE.md) — four-domain test model and cadence
- [ENGINEERING_PRINCIPLES.md](./ENGINEERING_PRINCIPLES.md) — engineering philosophy and design tenets
- [FORK.md](./FORK.md) — fork declaration and attribution
- [CONTRIBUTING.md](./CONTRIBUTING.md) — contribution guidelines

## License

GPL-2.0-or-later (GPL v2 or later). See [LICENSE](./LICENSE).

## Attribution

Original TN5250J community; headless extensions by Eric C. Mumford (@heymumford).

## Upstream History

TN5250J was created to provide a Linux 5250 emulator with advanced features such as edit-field continuation, GUI windows, and cursor progression. It was open-sourced for cross-platform use and community adoption. The project originated on SourceForge and migrated to GitHub in 2016.
