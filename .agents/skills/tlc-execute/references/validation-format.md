# validation.md — Append-Only Format

The Verifier MUST read the existing file before writing. Never rewrite, correct, or delete sections from previous executions — they are historical evidence.

## File Structure

```markdown
# Validation — <feature_slug>

## Status atual

| Campo | Valor |
|-------|-------|
| **Veredito** | PASS ✅ / FAIL ❌ / PASS com ressalvas |
| **Spec vigente** | `<feature_slug>` |
| **HEAD** | `<commit hash>` |
| **Gaps abertos** | [none | numbered list] |
| **Última execução** | `<feature_slug>` — <date> |

[One-paragraph summary of current state]

---

## Execução: <feature_slug> — <YYYY-MM-DD>

**Commit range:** `<EXEC_START>..<HEAD>`
**Veredito:** PASS ✅ / FAIL ❌

### Spec-anchored check

| AC / Criterion | Spec-defined outcome | Evidence (`file:line` + assertion) | Result |
|----------------|---------------------|-----------------------------------|--------|
| AC-001: ... | [expected from spec] | `path/test.ts:42` — `expect(x).toBe(y)` | ✅ / ❌ / ⚠️ gap |

### Gate

- Command: `[from tasks.md]`
- Result: N passed, 0 failed

### Discrimination sensor

| Mutation | Target | Killed? |
|----------|--------|---------|
| Flip condition in `FooService.bar` | `FooService.java:87` | ✅ / ❌ survived |

**Summary:** N injected, N killed, N survived

### Gaps encontrados

1. [Gap] — [AC] — [file:line or "no evidence"]

---

## Execução: <previous-slug> — <previous-date>

[Historical section — NEVER modify after written]

...
```

## Write Rules

1. **Status atual** — ONLY section rewritten each round. Replace entire block between `## Status atual` and the next `---`.
2. **Execution sections** — APPEND new section at the bottom (after last `---`), in chronological order.
3. **Historical file:line references** — leave unchanged even if code moved later. They record what was verified at that time.
4. **Header format:** `## Execução: <slug> — <YYYY-MM-DD>` with commit range on next line.
5. If file does not exist, create with Status atual + first execution section.

## Verifier Presentation (chat)

Orchestrator presents two blocks to user:

1. **Status atual** — copied from top of validation.md
2. **Esta execução** — the section just appended

Confirm task-by-task what was implemented and tested (from worker summaries + AC evidence table).
