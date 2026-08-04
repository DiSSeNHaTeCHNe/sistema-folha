# Fix Cycles

Maximum **3** fix→re-verify iterations covering Verifier gaps and code-review findings class **(a)** (violates existing AC).

## Trigger

- Verifier returns FAIL or surviving mutants
- Code review classifies finding as **(a)** — violates existing AC

Do NOT enter fix cycle for:
- Class **(b)** spec gaps → stop, ask user, amend spec if approved
- Class **(c)** improvements → "Questões abertas" only

## Cycle Flow

```
Cycle N (N = 1..3):
  1. Orchestrator lists gaps → minimal fix tasks
  2. Dispatch ONE worker with fix tasks
  3. Worker commits: fix(cycle-N): <description>
  4. Dispatch Verifier (append new execution section to validation.md)
  5. Dispatch code-review (branch diff since EXEC_START)
  6. New class (a) findings? → Cycle N+1 if N < 3
  7. PASS + no class (a)? → done
```

## Fix Task Rules

- Correct what exists — no new features, no opportunistic refactor
- Tests: strengthen assertions per Verifier/sensor gaps; do not delete/weaken
- One atomic commit per fix task (may batch related micro-fixes if single AC gap)
- Prefix: `fix(cycle-N):` regardless of revision commit_prefix
- Scope: only files implicated by the gap

## Worker Payload (fix cycle)

Same constraints as main batch worker, plus:

```
Fix cycle: N of 3
Gaps to fix:
1. [AC-003] Sensor survived: flip in FooService — strengthen test at path:line
2. [code-review (a)] Missing auth guard on POST /api/keys — SecurityConfig:42

Do NOT expand scope. Do NOT spawn sub-agents.
Return compact summary with commit hashes.
```

## Escalation (after cycle 3)

Stop and report:

```markdown
## Pendências após 3 ciclos

| # | Gap | Origem | Motivo pendente |
|---|-----|--------|-----------------|
| 1 | ... | Verifier sensor | Mutant survives after 3 assertion strengthenings |
| 2 | ... | Code review (a) | Requires schema change outside spec scope |
```

Do NOT dispatch cycle 4. User decides next steps.
