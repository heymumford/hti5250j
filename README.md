# TN5250J Headless Edition

A 5250 terminal emulator for the IBM i (AS/400) written in Java — **specialized for headless operation, session management, and protocol extensions**.

> **Note:** This is a **maintained fork** of [TN5250J](https://github.com/tn5250j/tn5250j). See [FORK.md](./FORK.md) for attribution and differences.

## Quick Start

```bash
git clone https://github.com/heymumford/tn5250j.git
cd tn5250j
ant build
```

## Features

This fork adds:
- **Headless Mode** — Full operation without GUI (server/daemon use)
- **Session Pooling** — Concurrent session management with lifecycle validation
- **Enhanced Testing** — 25+ pairwise test suites for protocol reliability
- **Plugin Architecture** — Extensible protocol handlers
- **Advanced Diagnostics** — Comprehensive logging and error reporting
- **Protocol Extensions** — TN5250E and attribute plane operations

For base TN5250J features, see [tn5250j.github.io](https://tn5250j.github.io/)

## Documentation

- [FORK.md](./FORK.md) — Fork declaration and attribution
- [CONTRIBUTING.md](./CONTRIBUTING.md) — Contribution guidelines
- **Original Project:** [tn5250j/tn5250j](https://github.com/tn5250j/tn5250j)

## License

GNU General Public License v2 — See [LICENSE](./LICENSE)

**Attribution:** Original TN5250J community. Headless extensions by Eric Mumford.

## History (Original Project)

Created to fill the gap for a Linux terminal emulator with advanced features (continued edit fields, GUI windows, cursor progression). Open-sourced to benefit the open-source community. Written in Java for cross-platform compatibility.

**Original Project Hosting:** Previously at [sourceforge.net](https://sourceforge.net/projects/tn5250j/); migrated to GitHub in 2016.
