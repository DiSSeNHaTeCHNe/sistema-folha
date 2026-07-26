# Ajuste do Harness — Validation

**Date**: 2026-07-26  
**Iteration**: 2 of max 3 (re-verify after Fix 1: gate `rg '\.specs'` broadened)  
**Spec**: `_docs/specs/features/ajuste-harness/spec.md`  
**Diff range**: uncommitted working tree (user instructed no commits) — harness paths scoped per Verifier brief  
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | `.gitignore` `_docs/*` + `!_docs/specs/**`; `.specs/` / `.env` ignored |
| T2 | ✅ Done | `git ls-files` lists núcleo; no `_docs/temp/` / `.env` in index |
| T3 | ✅ Done | `lessons.py` `STORE_REL`/`RENDER_REL` → `_docs/specs/...` (confirmed restored post-sensor) |
| T4 | ✅ Done | TLC `SKILL.md` layout `_docs/specs/`; flow intact |
| T5 | ✅ Done | Gate pattern `rg '\.specs'` (no slash-only) → 0 matches in TLC skill tree |
| T6 | ✅ Done | 5 FE skills with `Status: TARGET` |
| T7 | ✅ Done | STRUCTURE Current vs TARGET; CONVENTIONS/TESTING notes |
| T8 | ✅ Done | `_docs/AGENTS.md` absent |
| T9 | ✅ Done | Root AGENTS: tool pointers + P3 validation checklist |
| T10 | ✅ Done | `.cursorrules` no `_D2TLabs`; points to root AGENTS / `_docs/specs/` |
| T11 | ✅ Done | `.windsurf` absent; Claude → `../AGENTS.md` |
| T12 | ✅ Done | ROADMAP harness COMPLETE; FE target DEFERRED |
| T13 | ✅ Done | Full gate PASS; STATE/HANDOFF updated |

**Tasks.md status table**: T1–T13 all `Done`.  
**Fix 1 (iter 1 gap)**: Full/T3/T4/T5 gates + design/spec Independent Test use `rg '\.specs'` (not `\.specs/`) — confirmed in `tasks.md:26,52,176,201,224`, `design.md:135,220`, `spec.md:78`.

---

## Spec-Anchored Acceptance Criteria

### P1: Harness versionável no Git (HARN-01..02)

| Criterion (WHEN X THEN Y) | Spec-defined outcome | Evidence | Result |
| ------------------------- | -------------------- | -------- | ------ |
| Clone inclui núcleo versionado | `AGENTS.md`, `.agents/rules/`, `.agents/skills/` (TLC), `.agents/references/specs-layout.md`, `_docs/specs/` trackeáveis | `git ls-files` lists `AGENTS.md`, TLC `SKILL.md`, `_docs/specs/STATE.md`, `.claude/CLAUDE.md`, `.cursor/rules/.cursorrules`, `specs-layout.md`; Full gate: paths not ignored | ✅ PASS |
| `.gitignore` mantém temp/secrets/`.specs/` fora | `_docs/temp/`, `.env*`, `.specs/` ignored; specs not ignored | `git check-ignore -v _docs/temp/` → `.gitignore:113:_docs/*`; `.specs/` → `:110`; `.env` → `:102`; STATE not ignored | ✅ PASS |
| AGENTS aponta rules/skills/references sem `_docs/temp/` | Ponteiros válidos sob `.agents/` | `AGENTS.md` § fontes (`.agents/` + `_docs/specs/`); no temp dependency | ✅ PASS |

### P1: Paths TLC → `_docs/specs/` (HARN-03..04)

| Criterion | Spec-defined outcome | Evidence | Result |
| --------- | -------------------- | -------- | ------ |
| Artefatos usam `_docs/specs/` | writers/readers canônicos `_docs/specs/` | `lessons.py:35-36` `STORE_REL`/`RENDER_REL` = `os.path.join("_docs", "specs", …)`; feature tree under `_docs/specs/features/ajuste-harness/` | ✅ PASS |
| Patch só paths; fluxo intacto | Sem mudança Verifier/auto-sizing/contrato | TLC `SKILL.md` still documents Verifier/auto-sizing; `validate.md` Discrimination Sensor intact (spot-check prior + structure) | ✅ PASS |
| AGENTS + specs-layout coerentes | Uma fonte canônica `_docs/specs/` | `AGENTS.md` layout checklist; `.agents/references/specs-layout.md` | ✅ PASS |
| `.specs` não é destino oficial | Zero hits canônicos na skill TLC | Shell: `rg '\.specs' .agents/skills/tlc-spec-driven` → 0 matches (Full gate) | ✅ PASS |

### P1: Skills target vs brownfield (HARN-05..06)

| Criterion | Spec-defined outcome | Evidence | Result |
| --------- | -------------------- | -------- | ------ |
| 5 skills FE com aviso TARGET + ROADMAP | Banner explícito | `api-client/SKILL.md:6` (+ same pattern in 4 others); gate count=5 | ✅ PASS |
| Sem liberação ROADMAP → brownfield obrigatório | STRUCTURE/CONVENTIONS/TESTING current | `STRUCTURE.md:103-107` | ✅ PASS |
| ROADMAP/STATE: adequação código = feature posterior | Deferred / future | `ROADMAP.md:87-90` DEFERRED; `STATE.md` AD-004 | ✅ PASS |

### P1: AGENTS canônico na raiz (HARN-07..09)

| Criterion | Spec-defined outcome | Evidence | Result |
| --------- | -------------------- | -------- | ------ |
| Canônico só na raiz | `test -f AGENTS.md` | Present; Full gate PASS | ✅ PASS |
| `_docs/AGENTS.md` não existe | `test ! -f _docs/AGENTS.md` | Absent; Full gate + sensor C | ✅ PASS |
| Governança permanente sob `_docs/` rejeitada | Regra em AGENTS §11 | `AGENTS.md:231-234` tool pointers; P3 checklist `:60` | ✅ PASS |
| Claude → AGENTS raiz | `.claude/CLAUDE.md` aponta raiz | `.claude/CLAUDE.md:6` → `../AGENTS.md` | ✅ PASS |
| Cursor paths reais, sem `_D2TLabs` | Config aponta AGENTS + `.agents/` | `.cursor/rules/.cursorrules:5,12-15`; no `_D2TLabs` | ✅ PASS |
| Windsurf stub removido / não suportado | `.windsurf` ausente; documentado | `test ! -e .windsurf`; `AGENTS.md:234` | ✅ PASS |
| Antigravity → raiz | Ponteiro documentado | `AGENTS.md:233` | ✅ PASS |

### P1: Revisão pré-Execute (HARN-10)

| Criterion | Spec-defined outcome | Evidence | Result |
| --------- | -------------------- | -------- | ------ |
| Execute só após confirmação | Assumptions Confirmed?=y; Execute authorized | `spec.md:33-41` all `y`; `tasks.md:15` Approved — Execute T1–T13 complete | ✅ PASS |

### P2: Memória operacional (HARN-11)

| Criterion | Spec-defined outcome | Evidence | Result |
| --------- | -------------------- | -------- | ------ |
| STATE com ADs + Current Work | AD-003..006; Current Work | `STATE.md:6`, `:28-52` | ✅ PASS |
| HANDOFF com próximo passo | Next = Verifier / commits | `HANDOFF.md:3-11` | ✅ PASS |

### P3: Checklist Verified (HARN-12)

| Criterion | Spec-defined outcome | Evidence | Result |
| --------- | -------------------- | -------- | ------ |
| AGENTS exige `validation.md` sob `_docs/specs/features/...` | Path canônico | `AGENTS.md:60` | ✅ PASS |

**Status**: ✅ All HARN / P1–P3 ACs evidenced — 0 spec-precision gaps

---

## Discrimination Sensor

Scratch method: `cp` backup → mutate → run gate predicate → restore. Working tree restored; post-sensor Full smoke PASS. `lessons.py` STORE_REL confirmed `_docs/specs` after restore.

| Mutation | File | Description | Killed? |
| -------- | ---- | ----------- | ------- |
| A | `.agents/skills/tlc-spec-driven/scripts/lessons.py:35` | `STORE_REL = os.path.join(".specs", "lessons.json")` | ✅ **Killed** — `rg '\.specs'` matches join-style `.specs` (iter-1 survivor fixed) |
| B | `.agents/skills/api-client/SKILL.md:6` | `Status: TARGET` → `Status: REMOVED_FOR_SENSOR` | ✅ Killed — TARGET count became 4 |
| C | `_docs/AGENTS.md` (created empty) | Recreated duplicate AGENTS under `_docs/` | ✅ Killed — `test ! -f _docs/AGENTS.md` fails |

**Sensor depth**: lightweight (3 mutations)  
**Result**: 3/3 killed — **PASS ✅**

---

## Interactive UAT Results

N/A — harness/docs feature; no user-facing UI UAT required.

---

## Code Quality / Surgical Scope

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ Harness = docs/config/gitignore/skills |
| Surgical changes (feature Execute) | ✅ T1–T13 touch harness paths only |
| No scope creep in harness tasks | ✅ Adequação FE deferred |
| Concurrent WT product diffs | ⚠️ Observation: backend/frontend dirty in same WT (benefícios) — out of ajuste-harness Execute scope |
| Spec-anchored outcomes | ✅ Shell/file presence evidence |
| Documented guidelines | `tasks.md` Harness verify (`\.specs` broadened); Tests: none |

---

## Edge Cases

- [x] `_docs/temp/` continues ignored
- [x] Recreating `_docs/AGENTS.md` fails AGENTS gate (sensor C)
- [x] FE TARGET banner removal fails count gate (sensor B)
- [x] Join-style `.specs` STORE_REL fails TLC gate (sensor A)
- [x] Antigravity incomplete does not block P1
- [x] `lessons.py` restored to `_docs/specs` after sensor

---

## Gate Check

- **Gate command**: Harness verify (Full) from `tasks.md` — TLC check = `! rg -q '\.specs' .agents/skills/tlc-spec-driven`
- **Result**: **PASS** — all predicates green:
  - temp ignored; STATE/AGENTS/TLC skill not ignored
  - zero `rg '\.specs'` in TLC skill tree
  - `AGENTS.md` present, `_docs/AGENTS.md` absent
  - FE TARGET count = 5
  - no `_D2TLabs` in `.cursorrules`
  - `.windsurf` absent
- **Product tests**: N/A (explicitly not required)
- **Post-sensor smoke**: PASS; STORE_REL/RENDER_REL still `_docs/specs`
- **Commit status**: **uncommitted (user instructed no commits)**

---

## Fix Plans

None — iter-1 Fix 1 verified; no surviving mutants.

---

## Requirement Traceability Update

| Requirement | Previous Status (iter 1) | New Status |
| ----------- | ------------------------ | ---------- |
| HARN-01 | ✅ Verified (uncommitted) | ✅ Verified (uncommitted) |
| HARN-02 | ✅ Verified (uncommitted) | ✅ Verified (uncommitted) |
| HARN-03 | ⚠️ Verified with gate weakness | ✅ Verified (uncommitted) — gate kills join-style `.specs` |
| HARN-04 | ✅ Verified (path gate incomplete) | ✅ Verified (uncommitted) |
| HARN-05 | ✅ Verified (uncommitted) | ✅ Verified (uncommitted) |
| HARN-06 | ✅ Verified (uncommitted) | ✅ Verified (uncommitted) |
| HARN-07 | ✅ Verified (uncommitted) | ✅ Verified (uncommitted) |
| HARN-08 | ✅ Verified (uncommitted) | ✅ Verified (uncommitted) |
| HARN-09 | ✅ Verified (uncommitted) | ✅ Verified (uncommitted) |
| HARN-10 | ✅ Verified | ✅ Verified |
| HARN-11 | ✅ Verified (uncommitted) | ✅ Verified (uncommitted) |
| HARN-12 | ✅ Verified (uncommitted) | ✅ Verified (uncommitted) |

---

## Lessons

Clean PASS (no surviving mutants, no AC gaps, no SPEC_DEVIATION) → no lesson recorded.

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 12/12 HARN ACs evidenced; 0 spec-precision gaps  
**Sensor**: 3/3 mutations killed (incl. prior survivor A)  
**Gate**: Full harness PASS with broadened `\.specs` pattern  
**Commits**: none — uncommitted (user instructed no commits)

**What works**: Núcleo trackeável; ignore policy; TLC zero `.specs` under broadened gate; FE TARGET×5; AGENTS root-only; Cursor/Claude/Windsurf; ROADMAP/STATE/HANDOFF; Fix 1 closes join-style false-negative.

**Issues found**: none

**Next steps**: User commits of harness diffs (separate from concurrent product WT).
