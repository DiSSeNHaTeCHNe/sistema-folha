# Quick Task 001: Canonical specs cleanup

**Date:** 2026-06-20
**Status:** Done

## Description

Confirm `_docs/specs/` as the sole canonical location for TLC spec-driven artifacts and prevent regression to legacy `.specs/codebase/` layout.

## Files Changed

- `.gitignore` — ignore `.specs/` so deprecated layout is not re-committed
- `_docs/specs/STATE.md` — mark legacy migration todo done; record quick task
- `_docs/specs/quick/001-specs-canonical-cleanup/TASK.md` — this file
- `_docs/specs/quick/001-specs-canonical-cleanup/SUMMARY.md` — outcome record

## Verification

- [x] `_docs/specs/` contains PROJECT, ROADMAP, STATE, and 7 brownfield docs
- [x] No `.specs/` directory at repo root
- [x] `.gitignore` blocks future `.specs/` creation from being tracked

## Commit

Pending — user did not request commit.
