# Technical Writing Standards

**Purpose**: Write technical documentation that communicates clearly, not impressively.

**Authority**: Synthesized from Zinsser, Strunk & White, Orwell, Google, Microsoft style guides.

**Scope**: All markdown documentation in this repository (README, architecture docs, plans, reports).

---

## Core Principles (The Non-Negotiables)

1. **Clarity over cleverness** - Readers should never guess your meaning
2. **Brevity over ceremony** - Cut ruthlessly, respect reader time
3. **Active over passive** - "The system processes data" not "Data is processed"
4. **Concrete over abstract** - "Reduced latency by 40ms" not "Improved performance significantly"
5. **Simple over complex** - Short words, short sentences, short paragraphs

---

## Writing Standards

### Voice and Tone

**✅ DO:**
- Write in active voice: "The parser validates input"
- Use second person for instructions: "Run the tests with `pytest`"
- Write conversationally: "This approach works because..."
- Be direct: "Delete the file" not "The file should be deleted"

**❌ DON'T:**
- Use passive constructions: "The input is validated by the parser"
- Use first person plural: "We then process the data..."
- Sound robotic: "Proceed to execute the subsequent operation"
- Hedge unnecessarily: "It might be advisable to perhaps consider..."

### Word Choice

**✅ DO:**
- Use short, common words: "use" not "utilize", "help" not "facilitate"
- Define jargon on first use: "WAL (Write-Ahead Log) ensures..."
- Use specific numbers: "12 performance cores" not "many cores"
- Choose concrete verbs: "The function returns..." not "The function provides..."

**❌ DON'T:**
- Use ceremony phrases: ~~"world's first"~~, ~~"revolutionary"~~, ~~"paradigm shift"~~
- Use jargon without definition: "Implements CQRS pattern" (define CQRS first)
- Use vague intensifiers: ~~"very important"~~, ~~"extremely critical"~~, ~~"highly sophisticated"~~
- Use long words for short concepts: ~~"utilize"~~ → use, ~~"commence"~~ → start

### Sentence Structure

**✅ DO:**
- Keep sentences under 25 words
- One idea per sentence
- Front-load important information: "To fix: run `git reset`" not "Run `git reset` to fix this"
- Use parallel structure in lists

**❌ DON'T:**
- Create run-on sentences with multiple clauses
- Bury the action: "In order to accomplish the task of..." → "To..."
- Start with weak phrases: ~~"It should be noted that"~~, ~~"There are many cases where"~~

### Document Structure

**✅ DO:**
- Put conclusions first (inverted pyramid)
- Use descriptive headings: "Configure Database Connections" not "Configuration"
- Break long procedures into subsections (max 10 steps per section)
- Use code blocks for commands, not inline formatting

**❌ DON'T:**
- Save important details for the end
- Create walls of text (max 4 sentences per paragraph)
- Number more than 12 steps in a single procedure
- Use heading styles for emphasis (use **bold** instead)

---

## Ceremony Elimination Rules

### Prohibited Phrases (Auto-Reject)

- ~~"world's first"~~ / ~~"industry-leading"~~ / ~~"best-in-class"~~
- ~~"revolutionary"~~ / ~~"groundbreaking"~~ / ~~"game-changing"~~
- ~~"paradigm shift"~~ / ~~"synergy"~~ / ~~"leverage"~~ (as verb)
- ~~"seamless"~~ / ~~"robust"~~ / ~~"enterprise-grade"~~ (without metrics)
- ~~"cutting-edge"~~ / ~~"state-of-the-art"~~ / ~~"next-generation"~~

### Replacement Strategy

| Instead of (Ceremony) | Use (Clear) |
|----------------------|-------------|
| "This revolutionary approach..." | "This approach..." |
| "Leverages cutting-edge AI" | "Uses machine learning to..." |
| "Provides a robust solution" | "Handles X req/sec with <1% error rate" |
| "Seamless integration" | "Integrates via REST API" |
| "Enterprise-grade performance" | "Processes 10K transactions/sec" |

---

## Quality Checklist

Before committing documentation, verify:

- [ ] **Flesch Reading Ease > 50** (8th grade level) - Test at [readabilityformulas.com](https://readabilityformulas.com/free-reading-ease-calculator.php)
- [ ] **Average sentence length < 25 words** - Run `wc -w` on sentences
- [ ] **No ceremony phrases** - Search for prohibited terms
- [ ] **Active voice ≥ 80%** - Use [Hemingway Editor](https://hemingwayapp.com/)
- [ ] **Jargon defined on first use** - Scan for unexplained acronyms
- [ ] **Code examples tested** - All commands must execute successfully
- [ ] **Global-friendly** - Avoid idioms, culture-specific references, humor

---

## Examples: Before → After

### Example 1: Ceremony Elimination

**❌ Before:**
> "This groundbreaking, world-class solution leverages cutting-edge distributed systems paradigms to deliver a robust, enterprise-grade platform that seamlessly integrates with existing infrastructure."

**✅ After:**
> "This system uses distributed consensus (Raft) to replicate data across 3+ nodes with <100ms failover."

**Improvements**: Removed 6 ceremony phrases, added concrete details (Raft, 3+ nodes, <100ms), reduced 28 words to 17.

---

### Example 2: Passive → Active

**❌ Before:**
> "The configuration file should be edited by the user, and the database connection string must be updated with the production credentials before the application can be started."

**✅ After:**
> "Edit the configuration file and update the database connection string with production credentials. Then start the application."

**Improvements**: Changed passive to active, split long sentence (38 words → 14 + 4), front-loaded action.

---

### Example 3: Jargon Definition

**❌ Before:**
> "The system implements CQRS with event sourcing for eventual consistency."

**✅ After:**
> "The system separates read and write operations (CQRS: Command Query Responsibility Segregation) and logs all state changes as events. This provides eventual consistency: reads may lag writes by <500ms."

**Improvements**: Defined CQRS on first use, explained event sourcing, quantified "eventual consistency".

---

### Example 4: Vague → Specific

**❌ Before:**
> "The new caching layer significantly improves performance and reduces database load substantially."

**✅ After:**
> "Redis caching reduced API response time from 450ms to 80ms (82% faster) and cut database queries by 73%."

**Improvements**: Named technology (Redis), provided exact metrics, removed vague modifiers.

---

## Enforcement

### Automated Checks (Pre-Commit Hook)

```bash
#!/bin/bash
# .git/hooks/pre-commit

CEREMONY_PATTERNS=(
  "world'?s (first|best|only|leading)"
  "revolutionary|groundbreaking|game-changing"
  "paradigm shift|synergy"
  "seamless|robust|enterprise-grade"
  "cutting-edge|state-of-the-art|next-generation"
)

CHANGED_MD=$(git diff --cached --name-only --diff-filter=ACM | grep '\.md$')

for file in $CHANGED_MD; do
  for pattern in "${CEREMONY_PATTERNS[@]}"; do
    if git diff --cached "$file" | grep -iE "$pattern"; then
      echo "❌ BLOCKED: $file contains ceremony phrase: $pattern"
      echo "   See TECHNICAL_WRITING_STANDARDS.md"
      exit 1
    fi
  done
done
```

### Manual Review Checklist

Reviewers should verify:
1. Scan for ceremony phrases (prohibited list)
2. Check for jargon without definitions
3. Verify code examples execute successfully
4. Confirm metrics are specific, not vague
5. Test reading ease score ≥ 50

---

## When to Break These Rules

From Orwell: "Break any of these rules sooner than say anything outright barbarous."

**Valid exceptions:**
- **Domain-specific jargon** - If writing for experts (e.g., "CAP theorem" in distributed systems docs)
- **Legal requirements** - Compliance language may require passive voice
- **Quoted material** - Preserve original phrasing in citations
- **Established terminology** - "Enterprise Service Bus" if that's the actual product name

**Process for exceptions:**
1. Document why the rule doesn't apply
2. Get review approval
3. Add comment in markdown: `<!-- Ceremony exception: product name -->`

---

## Code Integration

**These principles apply to code, not just documentation.**

See [CODING_STANDARDS.md](./CODING_STANDARDS.md) Principle 3 for how to apply writing clarity to:
- **Code comments**: Explain WHY (business logic), never HOW (implementation)
- **Function names**: Self-documenting (e.g., `waitForKeyboardUnlock()` needs no comment)
- **Variable names**: Reveal intent (e.g., `fieldAttribute` not `adj` with comment)
- **JavaDoc**: Document contracts and preconditions, not line-by-line logic

**Philosophy**: Code should tell its story through structure and naming. Comments add context, not explanations.

**Example Anti-Pattern (Comment Crutch)**:
```java
// Loop through buffer and increment counter
for (int i = 0; i < buffer.length; i++) {
  counter++;  // Increment counter
}
```

**Example Correct (Self-Documenting)**:
```java
int fieldCount = countFieldsInBuffer(buffer);
```

---

## Authority References

This guide synthesizes principles from:

**Foundations:**
- [William Zinsser - On Writing Well](https://www.shortform.com/summary/on-writing-well-summary-william-zinsser) - Clarity, simplicity, brevity, humanity
- [Strunk & White - Elements of Style](https://faculty.washington.edu/heagerty/Courses/b572/public/StrunkWhite.pdf) - Vigorous writing is concise
- [George Orwell - Politics and the English Language](https://www.orwellfoundation.com/the-orwell-foundation/orwell/essays-and-other-works/politics-and-the-english-language/) - Six rules for clear writing

**Industry Standards:**
- [Google Developer Documentation Style Guide](https://developers.google.com/style) - Conversational, accessible, global-friendly
- [Microsoft Writing Style Guide](https://learn.microsoft.com/en-us/style-guide/welcome/) - Warm, crisp, user-focused

**Anti-Patterns:**
- [How to Avoid Jargon in Technical Writing](https://www.vistaprojects.com/how-to-avoid-jargon/) - Simplify, define, get feedback
- [Avoid Jargon - Digital.gov](https://digital.gov/guides/plain-language/principles/avoid-jargon) - No more than 1 technical term per 10 words

---

## Measuring Success

**Quarterly metrics:**
- Average Flesch Reading Ease score across all .md files
- Percentage of documents passing automated ceremony check
- User feedback: "Was this document helpful?" (docs with examples)

**Target:**
- Reading Ease: ≥ 55 (10th grade level, accessible to global audience)
- Ceremony violations: 0
- User helpfulness rating: ≥ 80%

---

**Last Updated**: 2026-02-12
**Version**: 1.0
**Maintainer**: Repository contributors
**Review Cycle**: Quarterly or when major standards update
