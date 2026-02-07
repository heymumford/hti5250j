# TN5250J Headless Fork

## Fork Status

This is a **maintained fork** of the original [TN5250J](https://github.com/tn5250j/tn5250j) project, specialized for headless terminal emulation and session management.

## Original Project

**TN5250J** — A 5250 terminal emulator for IBM i (AS/400)
**Original Repository:** https://github.com/tn5250j/tn5250j
**Original Author:** TN5250J Community
**License:** GNU General Public License v2

## This Fork

**Purpose:** Headless mode support with enhanced session pooling, lifecycle management, and protocol extensions
**Maintainer:** Eric Mumford (@heymumford)
**Repository:** https://github.com/heymumford/tn5250j
**License:** GNU General Public License v2 (inherited from original)

## Key Differences

This fork extends the original with:
- Headless operation (no GUI requirement)
- Session pooling and lifecycle management
- Pairwise testing framework (25+ test suites)
- Plugin architecture for protocol extensions
- Enhanced diagnostics and logging
- Attribute plane operations testing
- Screen refresh and restore validation

## Compatibility

- **Upstream:** Tracks tn5250j/tn5250j master branch
- **Sync Frequency:** Pull upstream changes regularly
- **Breaking Changes:** Documented in CHANGELOG.md
- **Contribution Path:** PRs welcome to this fork; significant changes should consider upstream integration

## Attribution

This fork maintains perpetual credit to:
- **Original Authors:** TN5250J Community (original development)
- **This Fork:** Eric Mumford (headless extensions, testing, architecture)

## License Compliance

This project is distributed under GPL v2, which permits:
- Free and paid distribution (see Section 2.a and Section 1)
- Modification and derivative works
- Shareware models

All derivative work must:
- Include full GPL v2 license
- Maintain original copyright notices
- Document modifications
- Provide source code
- Use same (GPL v2) license

See [LICENSE](./LICENSE) for full terms.

## Contributions

Contributions are welcome! By contributing, you agree that your changes will be distributed under GPL v2 with attribution as described above.
