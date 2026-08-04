---
name: tlc-execute
description: >-
  Delegated Execute phase for tlc-spec-driven features. Always dispatches batch
  workers (~7 tasks each), Verifier, and code-review sub-agents — no inline
  implementation. Orchestrator packs phases, manages git branch feat/<feature-base>,
  runs fix cycles (max 3), and writes append-only validation.md. Use when the user
  invokes "/tlc-spec-driven Execute — <feature>" or asks to execute a feature whose
  spec.md, design.md and tasks.md already exist in _docs/specs/features/<feature>/.
disable-model-invocation: true
---

# TLC Execute — Delegated Feature Execution

Execute phase override for [tlc-spec-driven](../tlc-spec-driven/SKILL.md). Inherits the **execution contract** (tests from ACs, gate before done, atomic commits, Verifier author≠verifier). This skill replaces inline execution and offer-then-confirm with **always-delegate** orchestration.

**Trigger:** `/tlc-spec-driven Execute — <feature>` or "execute feature `<feature>`"

**Prerequisite:** `_docs/specs/features/<feature>/spec.md`, `design.md`, and `tasks.md` must exist.

---

## Critical Rules

1. **Orchestrator never implements** — no code, no tests, no gate runs. Only: pack batches, dispatch workers, update `tasks.md`, dispatch Verifier/code-review, route fix cycles.
2. **Always delegate** — ignore tlc-spec-driven steps 2 (T ≤ ~8 → inline) and offer-then-confirm. Pre-approved: dispatch immediately.
3. **Workers never spawn sub-agents** — they implement their batch only.
4. **Scope = spec** — no new features, no drift. Necessary divergence → `SPEC_DEVIATION` marker + "Questões abertas" + **stop and ask**.
5. **No push, no merge** — stay on `feat/<feature-base>`. User does squash merge after review.
6. **Knowledge Verification Chain** (workers): codebase → project docs → Context7 MCP → web. Consult Context7 before using external library APIs whose version is unconfirmed. Never invent APIs.

Load parent references from the active tlc-spec-driven skill directory when workers need them:
- [implement.md](../tlc-spec-driven/references/implement.md) — per-task cycle
- [validate.md](../tlc-spec-driven/references/validate.md) — Verifier checklist
- [coding-principles.md](../tlc-spec-driven/references/coding-principles.md)

---

## Phase 0: Resolve Feature & Git

### 0.1 Parse inputs

| Input | Derivation |
|-------|------------|
| `feature_slug` | Directory name under `_docs/specs/features/` (e.g. `auth-api-keys-fix2`) |
| `feature_base` | Strip revision suffix: `-fix\d+` (e.g. `auth-api-keys-fix2` → `auth-api-keys`) |
| `branch_name` | `feat/<feature_base>` |
| `is_revision` | `true` when slug ≠ base (has `-fixN` suffix) |
| `commit_prefix` | If revision: `{revision_tag}:` e.g. `fix2:` — else empty |

### 0.2 Git setup

```bash
# Record baseline BEFORE any checkout/create
EXEC_BASE=$(git rev-parse HEAD)
BRANCH_BASE=$(git merge-base HEAD origin/main 2>/dev/null || git merge-base HEAD main)

# Branch exists?
git show-ref --verify --quiet refs/heads/feat/<feature_base>
```

| Condition | Action |
|-----------|--------|
| Branch `feat/<feature_base>` **does not exist** | `git checkout -b feat/<feature_base>` from current HEAD |
| Branch **exists** | `git checkout feat/<feature_base>` — resume from last commit |
| Never | Create a new branch for `-fixN` revision specs |

Record `EXEC_START=$(git rev-parse HEAD)` immediately after checkout (first commit of *this* execution).

### 0.3 Load context

Read (orchestrator only — summarize for workers, do not load all into orchestrator context):
- `_docs/specs/features/<feature_slug>/spec.md`
- `_docs/specs/features/<feature_slug>/design.md`
- `_docs/specs/features/<feature_slug>/tasks.md`
- `_docs/specs/features/<feature_slug>/validation.md` (if exists — read for history, do not rewrite old sections)
- `_docs/specs/STATE.md` — Decisions section only
- `python3 scripts/lessons.py list --status confirmed` — pass confirmed lessons to workers

---

## Phase 1: Pack & Dispatch Workers

### 1.1 Batching (override — always delegate)

Use the batching algorithm from [sub-agents.md](../tlc-spec-driven/references/sub-agents.md) **steps 3–5 only** (skip steps 1–2):

1. Count total tasks `T` and list phases from `tasks.md`.
2. Walk phases **in order**, accumulate whole phases until ~7 tasks per batch.
3. **Never split a phase** across workers.
4. Final tail of 1–2 tasks → merge into previous batch.
5. Small feature (T ≤ 7) → **1 worker with all tasks** (not 1 worker per task).

### 1.2 Identify pending work

From `tasks.md`, find tasks not marked complete. If all complete → skip to Phase 3 (Verifier).

### 1.3 Dispatch loop (strictly sequential)

For each batch, dispatch **one Task sub-agent** (worker). Wait for compact summary before next batch.

**Orchestrator prompt to worker — include:**

```
Feature: <feature_slug>
Branch: feat/<feature_base> (already checked out)
Commit prefix: <commit_prefix or "none">
Execution start commit: <EXEC_START>
Revision spec: <yes/no>

Tasks in this batch: [list task IDs + full definitions from tasks.md]
Phases covered: [phase names/numbers]

Read and follow:
- tlc-spec-driven references/implement.md (full per-task cycle)
- tlc-spec-driven references/coding-principles.md
- spec.md, design.md (this feature only)
- Test Coverage Matrix + Gate Check Commands from tasks.md
- Confirmed lessons: [list]

Git rules:
- One atomic commit per task (Conventional Commits)
- If commit_prefix set: "<prefix> <conventional message>" e.g. "fix2: feat(auth): add key rotation"
- Fix-cycle commits (if fixing gaps mid-batch): "fix(cycle-N): <description>"
- Include ONLY files listed in each task
- Do NOT push, merge, or create branches

Scope:
- Nothing outside spec.md ACs
- SPEC_DEVIATION → marker + stop batch, report blocker

Constraints:
- Do NOT spawn sub-agents
- Do NOT skip gate checks
- Do NOT weaken/delete/skip tests

Return ONLY compact summary (no raw logs):
---
Batch (phases [N]–[M]) complete:
- Tasks done: [T1 @ <hash>, T2 @ <hash>, ...]
- Tasks skipped/blocked: [none | list with reason]
- Tests: [N passed, 0 failed] (per gate run)
- Deviations/blockers: [none | SPEC_DEVIATION details]
---
```

### 1.4 After each worker returns

1. Update `tasks.md` — mark completed tasks, note blockers.
2. If blocker/SPEC_DEVIATION → **stop**, present to user, do not dispatch next batch.
3. If batch incomplete → decide fix or escalate before next batch.
4. If all batches done → Phase 3.

**Orchestrator must NOT:** write implementation code, write tests, or run test/build commands.

---

## Phase 2: Task Confirmation (orchestrator → user)

After all workers complete, present task-by-task:

```markdown
## Execução — <feature_slug>

| Task | Commit | Gate | Testes |
|------|--------|------|--------|
| T1: [title] | `<hash>` | ✅ pass | N passed |
| T2: [title] | `<hash>` | ✅ pass | N passed |
```

---

## Phase 3: Verifier (always-on sub-agent)

Dispatch fresh Verifier sub-agent after last task committed. **Author ≠ verifier.**

**Verifier receives:** `spec.md`, git range `EXEC_START..HEAD`, test files in scope, [validate.md](../tlc-spec-driven/references/validate.md), [validation-format.md](references/validation-format.md).

**Verifier does NOT fix code.** Mutations run in scratch state only.

**Verifier writes** `_docs/specs/features/<feature_slug>/validation.md` following append-only rules in [validation-format.md](references/validation-format.md).

**Verifier returns compact verdict:**

```
## Validation: <feature> — [PASS ✅ | FAIL ❌]
Spec-anchored: [N/N ACs | M gaps]
Gate: [X passed, 0 failed]
Sensor: [N injected, N killed, N survived]
Report: _docs/specs/features/<feature>/validation.md
Ranked gaps: [list]
```

Present in chat: **Status atual** block + **this execution's section** (from validation.md).

If FAIL → Phase 5 (fix cycle). If PASS → Phase 4.

---

## Phase 4: Code Review (sub-agent)

After Verifier verdict, run the [code-review](../code-review/SKILL.md) skill as sub-agent orchestrator:

| Parameter | Value |
|-----------|--------|
| INPUT | `branch` — diff `EXEC_START..HEAD` on `feat/<feature_base>` |
| OUTPUT | `report` |
| Sonar/JaCoCo | **Always attempt** Subagent 7 when Sonar MCP ready (requirement d) |

Classify each finding before acting:

| Class | Action |
|-------|--------|
| **(a)** Violates existing AC | Create fix task → Phase 5 |
| **(b)** Spec gap (AC wrong/ambiguous/missing) | **Stop.** Present to user. If approved → run tlc-spec-driven Specify → Design → Tasks → Execute **amending** existing `spec.md` with new requirement IDs (no new feature folder) |
| **(c)** Improvement/refactor outside ACs | Record in spec.md or tasks.md **"Questões abertas"** — do NOT implement |
| **(d)** Sonar/JaCoCo | Include via Subagent 7 in code-review (mandatory when MCP available) |

---

## Phase 5: Fix Cycles (max 3)

**Covers:** Verifier gaps + code-review findings class **(a)**.

```
Cycle 1 → Verifier → Code Review → (gaps?) → Cycle 2 → ... → max 3
```

Each fix cycle:
1. Orchestrator creates fix tasks (minimal — correct existing code/tests only, no opportunistic refactor).
2. Dispatch **one worker** with fix tasks only.
3. Commits: `fix(cycle-N): <description>`.
4. Re-dispatch Verifier → Code Review.

After cycle 3 with remaining issues → **stop**, list pending with reasons.

Fix workers follow same git/scope rules as Phase 1 workers.

Details: [fix-cycle.md](references/fix-cycle.md).

---

## Phase 6: Final Handoff

Do NOT push. Do NOT merge. Stay on `feat/<feature_base>`.

Report to user:

```markdown
## Execução concluída — <feature_slug>

| | |
|---|---|
| **Branch** | `feat/<feature_base>` |
| **Commit base original** | `<BRANCH_BASE>` (merge-base with main) |
| **Commit inicial desta execução** | `<EXEC_START>` |
| **HEAD atual** | `<HEAD>` (`git rev-parse HEAD`) |
| **Veredito** | PASS ✅ / FAIL ❌ / PASS com ressalvas |
| **Ciclos fix** | 0 / 1 / 2 / 3 |

### Status atual (validation.md)
[top block content]

### Esta execução
[execution section content]

Squash merge fica com você após revisão.
```

Update `_docs/specs/STATE.md` Handoff section (section-scoped write only).

Distill lessons if Verifier recorded failures: `scripts/lessons.py` per [lessons.md](../tlc-spec-driven/references/lessons.md).

---

## Orchestrator Checklist

```
- [ ] 0. Resolve feature_slug, feature_base, branch, commit_prefix
- [ ] 0. Git: checkout/create feat/<feature_base>, record EXEC_START, BRANCH_BASE
- [ ] 1. Pack batches (~7 tasks, whole phases, tail merge)
- [ ] 1. Dispatch workers sequentially — update tasks.md after each
- [ ] 2. Present task-by-task confirmation
- [ ] 3. Dispatch Verifier → append validation.md
- [ ] 4. Run code-review (Sonar Subagent 7 when available)
- [ ] 5. Fix cycles (≤3) if needed
- [ ] 6. Final handoff (branch, commits, verdict) — no push/merge
```

---

## What This Skill Does NOT Do

- Specify, Design, or Tasks phases (must exist before Execute)
- Push, merge, or open PRs
- Inline implementation (even for 1 task)
- Ask user to confirm sub-agent delegation
- Rewrite historical validation.md sections
