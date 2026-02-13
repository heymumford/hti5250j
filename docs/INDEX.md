# Documentation Index

**Welcome to the TN5250J Headless Edition technical reference library.** This index guides you to the right documentation for your task.

---

## 🎯 Start Here

### First Time Users
1. Read [../README.md](../README.md) - Overview and quick start
2. Review [5250_COMPLETE_REFERENCE.md](#5250-complete-reference) if you need protocol details
3. Check [../ARCHITECTURE.md](../ARCHITECTURE.md) for system design

### Developers
1. [../TESTING.md](../TESTING.md) - Testing strategy and test tiers
2. [../CONTRIBUTING.md](../CONTRIBUTING.md) - Development workflow
3. [5250_COMPLETE_REFERENCE.md](#5250-complete-reference) - When you need protocol details

### Protocol Engineers
Start directly with [5250_COMPLETE_REFERENCE.md](#5250-complete-reference) for comprehensive Telnet/5250 protocol information.

---

## 📚 Documentation Map

### 5250 Complete Reference
**File**: [`5250_COMPLETE_REFERENCE.md`](./5250_COMPLETE_REFERENCE.md)
**Size**: ~1095 lines | **Scope**: Comprehensive
**Audience**: Everyone - required reading for protocol work

**What's Inside**:
- **Telnet Transport Layer** - IAC sequences, option negotiation (TRANSMIT-BINARY, END-OF-RECORD, TERMINAL-TYPE)
- **5250 Record Format** - Logical Record Length (LRL), flags, opcodes, Record Terminator (IAC EOR)
- **Operation Codes** - INVITE, OUTPUT-ONLY, PUT/GET, SAVE/RESTORE, READ-IMMEDIATE, READ-SCREEN, CANCEL-INVITE
- **5250 Commands** - Row address, column address, erase/write, structured fields (WDSF/WSF)
- **5250 Orders** - SBA (Set Buffer Address), MC (Move Cursor), IC (Insert Cursor), PT (Pitch Table), FEA (Format/Erase All)
- **Field Format Word (FFW)** - Complete 8-bit breakdown with MSB-0 bit numbering (Bright, Protected, Autoskip, Mandatory Entry, etc.)
- **Field Control Word (FCW)** - 10 types of field controls with examples
- **Display Attributes** - Color codes (red, green, blue, pink, turquoise, yellow, white), blink, reverse video, underscore
- **AID Keys** - Attention Identifier codes (0x3C-0x3F, 0x61-0x79, 0x90-0x9B, 0xBA-0xBC)
- **Query/Query Reply** - 61-byte response format with device capabilities
- **Structured Fields** - WDSF (Write/Define Structured Field), WSF (Write Structured Field), field types
- **SCS Printer Data Stream** - For 3812 printer protocol
- **EBCDIC Code Pages** - ASCII ↔ EBCDIC mapping reference
- **TN5250E Enhancements** - RFC 2877, RFC 4777 extensions (NEW-ENVIRON, custom variables)
- **Error Detection** - Validation checklist for protocol violations
- **Complete Examples** - LRL calculation, Telnet negotiation sequences, message framing

**When to Use**:
- Implementing 5250 protocol operations
- Debugging protocol errors
- Validating message encoding/decoding
- Understanding field attributes and colors
- Terminal emulation or testing
- Protocol extension development

**Referenced Standards**:
- RFC 1205 (TN5250 initial definition)
- RFC 854, 856, 857, 858, 885, 1091 (Telnet options)
- RFC 2877 (TN5250E - NEW-ENVIRON)
- RFC 4777 (TN5250E - BIND, UNBIND)
- IBM 5250 and SCS documentation

---

## 🗂️ Project Documentation (Root)

Refer to files in the parent directory:

| Document | Purpose | Audience |
|----------|---------|----------|
| [../README.md](../README.md) | Project overview, quick start, features | Everyone |
| [../ARCHITECTURE.md](../ARCHITECTURE.md) | System design, C4 models, components | Architects, senior developers |
| [../TESTING.md](../TESTING.md) | Testing strategy, test tiers, running tests | QA, developers |
| [../CONTRIBUTING.md](../CONTRIBUTING.md) | Development workflow, coding standards | Contributors |
| [../CHANGELOG.md](../CHANGELOG.md) | Release history, version notes | Release managers |
| [../SECURITY.md](../SECURITY.md) | Security considerations, best practices | Security-conscious developers |

---

## 📦 Archived Documentation

**Location**: [`../archive/planning/`](../archive/planning/)

Planning, strategy, and assessment documents have been archived to keep the root directory clean and focused on shipping code. See [`../archive/planning/README.md`](../archive/planning/README.md) for details.

**What's Archived**:
- Strategy and roadmap documents
- API design assessments
- Release readiness checklists
- Governance documents
- Market persona research
- Refactoring plans
- Review artifacts from development process

**Access Archived Docs**:
```bash
# List archived planning documents
ls archive/planning/

# Search archived content
grep -r "keyword" archive/planning/

# Restore document if needed (copy from archive/ to root)
cp archive/planning/DOCUMENT.md ./
```

---

## 🔍 Quick Lookup

### Protocol Questions

**"How do I encode a 5250 message?"**
→ See [5250_COMPLETE_REFERENCE.md](./5250_COMPLETE_REFERENCE.md) § Record Format

**"What's the Telnet negotiation sequence?"**
→ See [5250_COMPLETE_REFERENCE.md](./5250_COMPLETE_REFERENCE.md) § Telnet Transport Layer

**"How do field attributes work?"**
→ See [5250_COMPLETE_REFERENCE.md](./5250_COMPLETE_REFERENCE.md) § Field Format Word (FFW)

**"What color codes are supported?"**
→ See [5250_COMPLETE_REFERENCE.md](./5250_COMPLETE_REFERENCE.md) § Display Attributes & Colors

**"How do I query terminal capabilities?"**
→ See [5250_COMPLETE_REFERENCE.md](./5250_COMPLETE_REFERENCE.md) § Query/Query Reply

### Project Questions

**"How do I build the project?"**
→ See [../README.md](../README.md) § Building

**"How do I run tests?"**
→ See [../README.md](../README.md) § Running Tests OR [../TESTING.md](../TESTING.md)

**"What's the system architecture?"**
→ See [../ARCHITECTURE.md](../ARCHITECTURE.md)

**"How do I contribute?"**
→ See [../CONTRIBUTING.md](../CONTRIBUTING.md)

**"What's the security model?"**
→ See [../SECURITY.md](../SECURITY.md)

---

## 📋 Document Statistics

| Document | Lines | Scope | Status |
|----------|-------|-------|--------|
| 5250_COMPLETE_REFERENCE.md | 1095 | Comprehensive 5250/Telnet/TN5250E | Current (2026-02-13) |
| ../ARCHITECTURE.md | 450+ | System models and components | Current |
| ../TESTING.md | 400+ | Test strategy and execution | Current |
| ../CONTRIBUTING.md | 250+ | Development guidelines | Current |
| ../README.md | 310+ | Project overview | Current |
| ../CHANGELOG.md | 200+ | Release history | Current |
| ../SECURITY.md | 150+ | Security guidelines | Current |

---

## 🔗 Cross-References

### Standards & RFCs
- [RFC 1205](https://www.rfc-editor.org/rfc/rfc1205) - TN5250 Protocol
- [RFC 2877](https://www.rfc-editor.org/rfc/rfc2877) - TN5250E: NEW-ENVIRON
- [RFC 4777](https://www.rfc-editor.org/rfc/rfc4777) - TN5250E: BIND/UNBIND
- [RFC 854](https://www.rfc-editor.org/rfc/rfc854) - Telnet Protocol Specification
- [RFC 856](https://www.rfc-editor.org/rfc/rfc856) - Telnet Binary Transmission
- [RFC 857](https://www.rfc-editor.org/rfc/rfc857) - Telnet Echo Option
- [RFC 858](https://www.rfc-editor.org/rfc/rfc858) - Telnet Suppress-Go-Ahead

### External Resources
- [IBM 5250 Terminal Specification](https://www.ibm.com/support) - Official IBM documentation
- [EBCDIC Code Page Reference](https://en.wikipedia.org/wiki/Extended_ASCII) - Character encoding
- [Telnet Protocol Tutorial](https://www.rfc-editor.org/rfc/rfc854) - RFC 854 detailed walkthrough

---

## 📝 Version & Maintenance

**Last Updated**: 2026-02-13
**Documentation Version**: 1.0
**Status**: Current and complete

### Contributing to Documentation
See [../CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines on updating documentation.

### Known Limitations
None. The 5250_COMPLETE_REFERENCE.md is authoritative and has undergone a 32-agent adversarial review (Feb 2026) covering 5 reference documents, 3 rounds of corrections, and verification against primary standards.

---

**Need help?** Check the "Quick Lookup" section above or file an issue on GitHub.
