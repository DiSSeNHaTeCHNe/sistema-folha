# Quick Task 001 — Summary

**Completed:** 2026-06-20

## What was done

- Verified legacy `.specs/codebase/` is absent from the workspace (superseded by brownfield docs in `_docs/specs/` from `map codebase`).
- Added `.specs/` to `.gitignore` to prevent accidental reintroduction of the deprecated TLC layout.
- Updated `_docs/specs/STATE.md`: closed migration todo, refreshed AD-001, logged this quick task.

## Canonical layout (reference)

```text
_docs/specs/
├── PROJECT.md, ROADMAP.md, STATE.md, HANDOFF.md
├── STACK.md, ARCHITECTURE.md, CONVENTIONS.md, STRUCTURE.md
├── TESTING.md, INTEGRATIONS.md, CONCERNS.md
├── features/[feature]/{spec,context,design,tasks}.md
└── quick/NNN-slug/{TASK,SUMMARY}.md
```

Do **not** use `.specs/`, `project/`, or `codebase/` subfolders. See `.agents/references/specs-layout.md`.

## Files touched

| File | Change |
|------|--------|
| `.gitignore` | Added `.specs/` ignore rule |
| `_docs/specs/STATE.md` | Todo + AD-001 + quick tasks table |
