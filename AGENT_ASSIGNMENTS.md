# Agent File Assignments (32 Agents, 304 Files)

**Distribution**: ~9-10 files per agent
**Critique Standards**: CODING_STANDARDS.md Principle 1-3, Java 21 features

---

## Agent Task Template

Each agent must analyze assigned files for:

1. **Naming Violations** (CODING_STANDARDS.md Principle 1)
   - Abbreviations: `adj`, `buf`, `attr`, `pos`, `x`, `y` (non-loop)
   - Boolean methods without `is/has/can/should` prefix
   - Cryptic variable names requiring comments

2. **Comment Anti-Patterns** (CODING_STANDARDS.md Principle 3)
   - Comments explaining WHAT (redundant with code)
   - Comments explaining HOW (should refactor code instead)
   - Comment crutches (compensating for bad names)
   - JavaDoc repeating implementation (should document contract)

3. **Java 21 Feature Gaps** (CODING_STANDARDS.md Part 2)
   - Data classes not using Records
   - instanceof without pattern matching
   - Switch statements not using expressions
   - Platform threads instead of virtual threads

4. **File Length** (CODING_STANDARDS.md Part 3)
   - Files >400 lines without justification
   - Missing extraction opportunities

5. **Self-Documenting Code Failures** (WRITING_STYLE.md + CODING_STANDARDS.md 3.1-3.8)
   - Code requiring comments to understand
   - Magic numbers without constants
   - Complex logic without extracted methods

---

## Output Format (Per Agent)

```markdown
# Agent N Critique Report

## Files Analyzed
- file1.java (X lines, Y% comments)
- file2.java (X lines, Y% comments)

## Critical Violations (P1)
### File: filename.java:line_number
**Type**: Comment Crutch / Naming / Java21 / FileLength
**Evidence**: [code snippet]
**Impact**: Maintainability / Readability / Performance
**Fix**: [specific refactoring]

## Code Smells (P2)
[same format]

## Opportunities (P3)
[same format]

## Metrics
- Total lines of code: X
- Total comment lines: Y
- Comment-to-code ratio: Z%
- Files >400 lines: N
- Java 21 feature adoption: M%
```

---

## File Batches Generated

Batches created in `/tmp/agent_batch_*` (31+ files)
Each contains ~10 file paths for analysis.

---

**Status**: Ready for parallel execution
**Next**: Launch 32 Task agents with batch assignments
