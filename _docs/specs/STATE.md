# Project State

_Persistent memory across sessions. Updated as decisions are made, blockers surface, and lessons are learned._

**Last Updated:** 2026-07-26  
**Current Work:** Feature `ajuste-harness` — Execute T1–T13 done (uncommitted per user); awaiting Verifier + user commits

---

## Decisions

### AD-001: Layout spec-driven em `_docs/specs/` (2026-06-20)

**Decision:** Todos os artefatos TLC spec-driven ficam no layout flat `_docs/specs/` (sem `.specs/` ou subpastas `project/` / `codebase/`).  
**Reason:** Alinhamento com `AGENTS.md` e `.agents/references/specs-layout.md`.  
**Trade-off:** Layout antigo `.specs/codebase/` abandonado; `.specs/` ignorado no git.  
**Impact:** Novas features usam `_docs/specs/features/[feature]/`; brownfield docs na raiz de `_docs/specs/`. Quick task 001 confirmou cleanup (2026-06-20).  
**Status:** active (reforçado por AD-005)

### AD-002: Layout físico frontend / backend / diversos (2026-06-20)

**Decision:** Código de produção em `frontend/` e `backend/`; auxiliares em `diversos/`.  
**Reason:** Separação clara para multiagente (frentes frontend/backend) e raiz enxuta.  
**Trade-off:** `src/` e `pom.xml` saíram da raiz; relatórios operacionais em `diversos/relatorios/`.  
**Impact:** Dockerfile, README, `.gitignore` e specs usam `frontend/` e `backend/` (quick task 003 renomeou de `front/`/`back/`).  
**Status:** active

### AD-003: Núcleo do harness versionado no Git (2026-07-26)

**Decision:** Versionar `AGENTS.md` (raiz), `.agents/`, `_docs/specs/`, `.claude/CLAUDE.md` e `.cursor/rules/`.  
**Reason:** Harness deve sobreviver a clone/CI; não pode ser só local.  
**Trade-off:** Diffs de governança no Git; prefs locais de IDE fora do tracking.  
**Scope:** `.gitignore`, onboarding de agentes.  
**Status:** active (applied in Execute `ajuste-harness`)

### AD-004: Skills FE = TARGET até liberação no ROADMAP (2026-07-26)

**Decision:** `api-client`, `forms-validation`, `component-architecture`, `routing-perf`, `testing-a11y` são target; obrigação atual = brownfield (`CONVENTIONS`/`STRUCTURE`/`TESTING` + código).  
**Reason:** Skills aspiracionais não devem forçar refactors antes da feature de adequação.  
**Trade-off:** Dualidade current/target até a fase 2.  
**Scope:** Frontend agents / PRs.  
**Status:** active (applied in Execute `ajuste-harness`)

### AD-005: TLC paths canônicos em `_docs/specs/` (2026-07-26)

**Decision:** Patch cirúrgico na skill `tlc-spec-driven` para docs/memória/artefatos/lessons usarem `_docs/specs/` (não `.specs/`). Fluxo TLC intacto.  
**Reason:** Eliminar cisma com AD-001 sem symlink.  
**Trade-off:** Diff de strings na skill upstream-local.  
**Scope:** `.agents/skills/tlc-spec-driven/**`.  
**Status:** active (applied in Execute `ajuste-harness`)

### AD-006: AGENTS só na raiz; `_docs` sem governança permanente (2026-07-26)

**Decision:** Canônico = `AGENTS.md` na raiz. Apagar `_docs/AGENTS.md`. Em `_docs/`, só `_docs/specs/` (processo) e `_docs/temp/` (transitório, não versionado).  
**Reason:** Duplicatas divergem; usuário definiu AGENTS sob `_docs` como transitório.  
**Trade-off:** Ferramentas devem apontar para a raiz.  
**Scope:** Governança / IDE pointers.  
**Status:** active (applied in Execute `ajuste-harness`)

---

## Handoff

- **Feature**: `ajuste-harness` → `_docs/specs/features/ajuste-harness/`
- **Phase / Task**: Execute complete (T1–T13); Full harness gate PASS
- **Completed**: Specify, Design Approach A, Tasks, Execute T1–T13 (Batch 1 + Batch 2)
- **In-progress**: Awaiting independent Verifier; user commits (no auto-commit)
- **Next step**: Verifier → optional `validation.md`; user commits harness changes
- **Blockers**: Nenhum técnico
- **Uncommitted files**: harness + specs (produto backend/frontend fora do escopo desta feature)
- **Branch**: (working tree local)

---

## Blockers

_None currently._

---

## Todos

- [x] Inicializar projeto (`PROJECT.md`, `ROADMAP.md`, `STATE.md`)
- [x] Executar `map codebase` → brownfield docs
- [x] Specificar feature `ajuste-harness`
- [x] Aprovar Design `ajuste-harness` → Tasks → Execute (T1–T13 done, uncommitted)
- [ ] Verifier independente + commits do usuário
- [ ] Feature futura: adequação do código às skills FE target / gaps de segurança do relatório
- [x] Migrar ou descartar artefatos legados em `.specs/codebase/`

---

## Quick Tasks Completed

| #   | Description                         | Date       | Commit  | Status  |
| --- | ----------------------------------- | ---------- | ------- | ------- |
| 001 | Canonical specs cleanup (`.specs/`) | 2026-06-20 | pending | ✅ Done |
| 002 | Organizar pastas + diversos              | 2026-06-20 | pending | ✅ Done |
| 003 | Renomear front/back → frontend/backend   | 2026-06-20 | pending | ✅ Done |
| 004 | Fix TS build em Funcionarios/index.tsx     | 2026-06-20 | pending | ✅ Done |

---

## Lessons Learned

### L-001: Relatórios frontend desconectado (2026-06-20)

**Context:** Brownfield mapping compared frontend services to backend controllers.  
**Problem:** `relatorioService.ts` calls six `/relatorios/*` endpoints with no backend implementation.  
**Solution:** Document in `CONCERNS.md`; prioritize backend relatórios or remove/disable UI until implemented.  
**Prevents:** Agents assuming PDF reports work because README or UI exists.

---

## Deferred Ideas

- Portal do colaborador (holerite self-service)
- Motor legal completo de folha substituindo ADP
- Multi-tenant
- Notificações em tempo real
- Adequação do código ao harness (fase 2) — ROADMAP Deferred
- Mover relatório de conformidade para `diversos/relatorios/` se precisar versionar

---

## Preferences

- Governança multiagente via Linear (key única por produto) e regras em `.agents/rules/`
- Spec-driven: Specify + Execute sempre; Design/Tasks conforme auto-sizing da skill TLC
- Harness: Approach A (patch in-place); TLC path → `_docs/specs/`; AGENTS só na raiz
- Sem commits automáticos nesta feature — usuário controla commits
