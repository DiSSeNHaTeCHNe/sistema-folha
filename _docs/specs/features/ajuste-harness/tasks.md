# Ajuste do Harness — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

**Pré-Execute (HARN-10):** só iniciar Execute após confirmação explícita do usuário (“aprovado para Execute” / equivalente).

---

**Design**: `_docs/specs/features/ajuste-harness/design.md`  
**Spec**: `_docs/specs/features/ajuste-harness/spec.md`  
**Status**: Approved — Execute T1–T13 complete (awaiting Verifier)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `AGENTS.md`, `backend/AGENTS.md` (unit Mockito for services). **Esta feature não altera backend/frontend de produto** — camada = docs/config/harness.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| `.gitignore` / Git tracking policy | none | Shell gates: paths trackeáveis vs ignorados | `.gitignore` | Gate harness (abaixo) |
| TLC skill paths (`lessons.py`, SKILL, references) | none | Zero writers/readers canônicos em `.specs/`; fluxo intacto | `.agents/skills/tlc-spec-driven/**` | `rg -q '\.specs' .agents/skills/tlc-spec-driven` → exit 1 (0 matches) |
| FE skill TARGET banners | none | 5 skills com banner TARGET | `.agents/skills/{api-client,forms-validation,component-architecture,routing-perf,testing-a11y}/SKILL.md` | `rg -l 'Status: TARGET' .agents/skills/*/SKILL.md` → 5 files |
| Brownfield STRUCTURE/CONVENTIONS/TESTING | none | Nota current vs target | `_docs/specs/STRUCTURE.md` (+ opcional) | File contains “TARGET” or “current vs target” |
| `AGENTS.md` + tool pointers | none | Canônico raiz; sem `_docs/AGENTS.md`; Cursor sem `_D2TLabs` | `AGENTS.md`, `.cursor/rules/`, `.claude/` | `test ! -f _docs/AGENTS.md && test -f AGENTS.md` |
| Backend services / Frontend app | N/A | Fora de escopo desta feature | — | Não rodar como gate desta feature |

## Gate Check Commands

> Confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após cada task de docs/config | Checks específicos no `Done when` da task |
| Full (harness) | Fim de fase / feature | Ver bloco **Harness verify** abaixo |
| Build (produto) | N/A nesta feature | Não exigir `mvn test` / `npm run build` para fechar tasks de harness |

**Harness verify (Full):**

```bash
# Tracking / ignore
git check-ignore -v _docs/temp/ 2>/dev/null | head -1   # deve indicar ignored
git check-ignore -v _docs/specs/STATE.md                # deve FALHAR (não ignorado) — exit ≠ 0
git check-ignore -v AGENTS.md                           # deve FALHAR (não ignorado)
git check-ignore -v .agents/skills/tlc-spec-driven/SKILL.md  # deve FALHAR

# TLC paths
! rg -q '\.specs' .agents/skills/tlc-spec-driven || { echo "FAIL: .specs still referenced"; exit 1; }

# AGENTS
test -f AGENTS.md && test ! -f _docs/AGENTS.md

# FE TARGET
test "$(rg -l 'Status: TARGET' .agents/skills/api-client/SKILL.md .agents/skills/forms-validation/SKILL.md .agents/skills/component-architecture/SKILL.md .agents/skills/routing-perf/SKILL.md .agents/skills/testing-a11y/SKILL.md | wc -l | tr -d ' ')" = "5"

# Cursor
! rg -q '_D2TLabs' .cursor/rules/.cursorrules

# Windsurf gone
test ! -e .windsurf
```

---

## Execution Plan

Phases sequenciais; tasks em ordem dentro da fase. Fases ≤ ~7 tasks.

### Phase 1: Git foundation

```
T1 → T2
```

### Phase 2: TLC path alignment

```
T3 → T4 → T5
```

### Phase 3: Skills FE target + brownfield

```
T6 → T7
```

### Phase 4: AGENTS canônico + tool pointers

```
T8 → T9 → T10 → T11
```

### Phase 5: Memória + gate final

```
T12 → T13
```

---

## Task Breakdown

### T1: Atualizar política `.gitignore` do harness

**What**: Substituir ignores que bloqueiam o núcleo; aplicar negation `_docs/*` + `!_docs/specs/**`; remover ignores de `.agents/`, `.claude/`, `.cursor/`, `**/AGENTS.md`, `_docs/` blanket.  
**Where**: `.gitignore`  
**Depends on**: None  
**Reuses**: Comentários “Spec-driven deprecated”; design C1  
**Requirement**: HARN-01, HARN-02

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven` (Execute)

**Done when**:

- [ ] `.agents/`, `AGENTS.md`, `_docs/specs/**`, `.claude/`, `.cursor/rules/` não são mais ignorados pelo padrão antigo
- [ ] `_docs/temp/` (via `_docs/*`) permanece ignorado
- [ ] `.specs/` e `_old/` continuam ignorados
- [ ] Gate quick: `git check-ignore -v _docs/specs/STATE.md` falha (não ignorado); `git check-ignore -v _docs/temp/` indica ignored

**Tests**: none  
**Gate**: quick  
**Commit**: `chore(harness): version agent specs via gitignore policy`

---

### T2: Registrar núcleo do harness no índice Git

**What**: `git add` dos paths do núcleo agora trackeáveis (sem `_docs/temp`, sem secrets).  
**Where**: working tree / index (`AGENTS.md`, `.agents/`, `_docs/specs/`, `.claude/CLAUDE.md`, `.cursor/rules/`)  
**Depends on**: T1  
**Reuses**: Lista de track do design C1  
**Requirement**: HARN-01

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] `git ls-files AGENTS.md .agents/skills/tlc-spec-driven/SKILL.md _docs/specs/STATE.md .claude/CLAUDE.md .cursor/rules/.cursorrules` lista os arquivos (após add)
- [ ] Nenhum path sob `_docs/temp/` no index
- [ ] Nenhum `.env` no index

**Tests**: none  
**Gate**: quick  
**Commit**: `chore(harness): track AGENTS, .agents, and _docs/specs`

---

### T3: Patch `lessons.py` para `_docs/specs`

**What**: Alterar `STORE_REL` / `RENDER_REL` / criação de dirs / help para `_docs/specs/lessons.json` e `LESSONS.md`.  
**Where**: `.agents/skills/tlc-spec-driven/scripts/lessons.py`  
**Depends on**: T2  
**Reuses**: Lógica existente do script  
**Requirement**: HARN-03, HARN-04

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] Nenhuma string canônica `.specs/lessons` no arquivo
- [ ] Paths apontam para `_docs/specs/...`
- [ ] Sem mudança nas regras candidate/confirmed/quarantine além do path
- [ ] Gate: `rg -q '\.specs' .agents/skills/tlc-spec-driven/scripts/lessons.py` → exit 1 (0 matches)

**Tests**: none  
**Gate**: quick  
**Commit**: `fix(tlc): point lessons store to _docs/specs`

---

### T4: Patch `SKILL.md` TLC para `_docs/specs`

**What**: Substituir árvore/estrutura e referências `.specs/` → `_docs/specs/` no SKILL.md da TLC.  
**Where**: `.agents/skills/tlc-spec-driven/SKILL.md`  
**Depends on**: T3  
**Reuses**: Conteúdo de fluxo intacto  
**Requirement**: HARN-03, HARN-04

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] Layout documentado usa `_docs/specs/`
- [ ] Fluxo Specify→Execute / Verifier / auto-sizing inalterados em substância
- [ ] Gate: `rg -q '\.specs' .agents/skills/tlc-spec-driven/SKILL.md` → exit 1 (0 matches)

**Tests**: none  
**Gate**: quick  
**Commit**: `docs(tlc): document canonical _docs/specs layout`

---

### T5: Patch references TLC + gate zero `.specs`

**What**: Substituir `.specs/` → `_docs/specs/` em todos os `references/*.md` da skill TLC; confirmar `rg` zero na pasta da skill.  
**Where**: `.agents/skills/tlc-spec-driven/references/{memory,design,discuss,implement,validate,lessons,sub-agents,specify,tasks,code-analysis,context-limits,coding-principles}.md` (todos que referenciem `.specs/`)  
**Depends on**: T4  
**Reuses**: specs-layout do projeto (já canônico)  
**Requirement**: HARN-03, HARN-04

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] `rg -q '\.specs' .agents/skills/tlc-spec-driven` → exit 1 (0 matches)
- [ ] Nenhuma alteração de lógica de Verifier/sub-agents além de path strings
- [ ] `.agents/references/specs-layout.md` permanece coerente (sem mudança obrigatória se já correto)

**Tests**: none  
**Gate**: quick  
**Commit**: `fix(tlc): align skill references to _docs/specs`

---

### T6: Banners TARGET nas 5 skills FE

**What**: Inserir bloco padrão “Status: TARGET…” após frontmatter em cada skill FE listada.  
**Where**:  
`.agents/skills/api-client/SKILL.md`  
`.agents/skills/forms-validation/SKILL.md`  
`.agents/skills/component-architecture/SKILL.md`  
`.agents/skills/routing-perf/SKILL.md`  
`.agents/skills/testing-a11y/SKILL.md`  
**Depends on**: T5  
**Reuses**: Texto do design C3  
**Requirement**: HARN-05, HARN-06

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] As 5 skills contêm `Status: TARGET`
- [ ] Corpo das skills (regras) não reescrito além do banner (+ opcional prefixo TARGET na description YAML)
- [ ] Gate: contagem TARGET = 5 nos paths acima

**Tests**: none  
**Gate**: quick  
**Commit**: `docs(skills): mark frontend skills as TARGET until ROADMAP`

---

### T7: Nota current vs target no brownfield

**What**: Adicionar seção curta em `STRUCTURE.md` (e uma linha em `TESTING.md` e/ou `CONVENTIONS.md` se couber) apontando brownfield como obrigação atual e skills FE como target.  
**Where**: `_docs/specs/STRUCTURE.md` (+ `TESTING.md` / `CONVENTIONS.md` se necessário)  
**Depends on**: T6  
**Reuses**: Árvore `pages/`/`services/` já documentada  
**Requirement**: HARN-06

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] STRUCTURE explica dualidade current/target e aponta as 5 skills
- [ ] Menciona que adequação de código é feature futura / ROADMAP

**Tests**: none  
**Gate**: quick  
**Commit**: `docs(specs): document current vs TARGET frontend skills`

---

### T8: Apagar `_docs/AGENTS.md`

**What**: Remover a duplicata de governança sob `_docs/`.  
**Where**: `_docs/AGENTS.md` (delete)  
**Depends on**: T7  
**Reuses**: N/A  
**Requirement**: HARN-07

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] `test ! -f _docs/AGENTS.md`
- [ ] `test -f AGENTS.md` (raiz intacta)

**Tests**: none  
**Gate**: quick  
**Commit**: `chore(docs): remove duplicate AGENTS.md under _docs`

---

### T9: Atualizar `AGENTS.md` raiz (unificação + checklist)

**What**: Atualizar § unificação (Cursor path real, Claude, Antigravity→raiz, Windsurf não suportado); regra de não colocar AGENTS permanente em `_docs/`; checklist P3 `validation.md` sob `_docs/specs/features/...`.  
**Where**: `AGENTS.md`  
**Depends on**: T8  
**Reuses**: Seções existentes  
**Requirement**: HARN-07, HARN-08, HARN-12

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] Path Cursor documentado como `.cursor/rules/` (não `.cursorrules` raiz inexistente, salvo se criar symlink — design: rules path)
- [ ] Checklist Verified → `_docs/specs/features/[feature]/validation.md`
- [ ] Regra: sem `AGENTS.md` permanente em `_docs/`
- [ ] Sem referência a `_docs/AGENTS.md` como canônico

**Tests**: none  
**Gate**: quick  
**Commit**: `docs(agents): unify tool pointers and verification checklist`

---

### T10: Corrigir `.cursorrules`

**What**: Remover `_D2TLabs`; apontar estrutura real `.agents/`; skills via diretório (não só 2 skills).  
**Where**: `.cursor/rules/.cursorrules`  
**Depends on**: T9  
**Reuses**: Seção Regras de Ouro existente  
**Requirement**: HARN-08

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] `! rg -q '_D2TLabs' .cursor/rules/.cursorrules`
- [ ] Menciona `AGENTS.md` raiz e `_docs/specs/`
- [ ] Lista ou aponta `.agents/skills/` completo

**Tests**: none  
**Gate**: quick  
**Commit**: `docs(cursor): fix agents paths and skill inventory`

---

### T11: Remover Windsurf stub; confirmar Claude

**What**: Remover `.windsurf/`; confirmar `.claude/CLAUDE.md` aponta para `../AGENTS.md`.  
**Where**: `.windsurf/` (delete), `.claude/CLAUDE.md` (verify/minimal fix)  
**Depends on**: T10  
**Reuses**: CLAUDE.md existente  
**Requirement**: HARN-08, HARN-09

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] `test ! -e .windsurf`
- [ ] Claude aponta para AGENTS na raiz
- [ ] Nenhum `_docs/AGENTS.md` recriado

**Tests**: none  
**Gate**: quick  
**Commit**: `chore(tooling): drop windsurf stub; keep Claude → root AGENTS`

---

### T12: Atualizar ROADMAP (harness + adequação futura)

**What**: Registrar item de harness (IN PROGRESS/COMPLETE ao final do Execute) e item futuro “Adequação código ↔ skills FE target” como Planned/Deferred.  
**Where**: `_docs/specs/ROADMAP.md`  
**Depends on**: T11  
**Reuses**: Milestone 2/3 structure  
**Requirement**: HARN-06, HARN-11

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] ROADMAP menciona ajuste do harness
- [ ] Adequação FE target listada como feature futura (não misturada com P1 harness)
- [ ] Sem expandir escopo para implementar código de produto

**Tests**: none  
**Gate**: quick  
**Commit**: `docs(roadmap): track harness work and deferred FE target adoption`

---

### T13: Gate harness full + STATE/HANDOFF + status design

**What**: Rodar **Harness verify (Full)**; atualizar STATE Current Work / Handoff; marcar `design.md` Status = Approved; atualizar traceability HARN-* para Ready for Execute / Pending Execute.  
**Where**: shell + `_docs/specs/STATE.md`, `HANDOFF.md`, `features/ajuste-harness/{design,spec,tasks}.md`  
**Depends on**: T12  
**Reuses**: Bloco Harness verify  
**Requirement**: HARN-10, HARN-11

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`

**Done when**:

- [ ] Harness verify (Full) passa
- [ ] STATE/HANDOFF refletem “tasks approved / awaiting Execute OK”
- [ ] `tasks.md` Status → Approved (após OK do usuário nesta fase)
- [ ] Nenhum commit de produto backend/frontend nesta feature

**Tests**: none  
**Gate**: full (harness)  
**Commit**: `docs(harness): record verification gates and handoff after tasks`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5

Phase 1:  T1 ──→ T2
Phase 2:  T3 ──→ T4 ──→ T5
Phase 3:  T6 ──→ T7
Phase 4:  T8 ──→ T9 ──→ T10 ──→ T11
Phase 5:  T12 ──→ T13
```

**Batch packing (Execute):** 13 tasks → ~2 batches  
- Batch 1: Phase 1+2+3 = T1–T7 (7 tasks)  
- Batch 2: Phase 4+5 = T8–T13 (6 tasks)  

No Execute: oferecer sub-agentes (offer-then-confirm) porque > ~8 tasks.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: gitignore | 1 file | ✅ Granular |
| T2: git add núcleo | 1 operação índice | ✅ Granular |
| T3: lessons.py | 1 file | ✅ Granular |
| T4: SKILL.md TLC | 1 file | ✅ Granular |
| T5: references TLC | 1 pasta / 1 conceito path | ✅ Cohesive |
| T6: 5 banners FE | mesmo padrão / 5 arquivos | ✅ Cohesive |
| T7: STRUCTURE note | 1–3 docs | ✅ Cohesive |
| T8: delete AGENTS _docs | 1 file | ✅ Granular |
| T9: AGENTS.md update | 1 file | ✅ Granular |
| T10: cursorrules | 1 file | ✅ Granular |
| T11: windsurf+claude | 2 paths relacionados tooling | ✅ Cohesive |
| T12: ROADMAP | 1 file | ✅ Granular |
| T13: full gate + memory | verificação + docs estado | ✅ Cohesive |

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ----------------- | ------------- | ------ |
| T1 | None | (start) | ✅ |
| T2 | T1 | T1→T2 | ✅ |
| T3 | T2 | T2→T3 | ✅ |
| T4 | T3 | T3→T4 | ✅ |
| T5 | T4 | T4→T5 | ✅ |
| T6 | T5 | T5→T6 | ✅ |
| T7 | T6 | T6→T7 | ✅ |
| T8 | T7 | T7→T8 | ✅ |
| T9 | T8 | T8→T9 | ✅ |
| T10 | T9 | T9→T10 | ✅ |
| T11 | T10 | T10→T11 | ✅ |
| T12 | T11 | T11→T12 | ✅ |
| T13 | T12 | T12→T13 | ✅ |

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | gitignore | none | none | ✅ |
| T2 | git index | none | none | ✅ |
| T3 | TLC lessons.py | none | none | ✅ |
| T4 | TLC SKILL | none | none | ✅ |
| T5 | TLC references | none | none | ✅ |
| T6 | FE skills | none | none | ✅ |
| T7 | brownfield docs | none | none | ✅ |
| T8 | delete file | none | none | ✅ |
| T9 | AGENTS.md | none | none | ✅ |
| T10 | cursorrules | none | none | ✅ |
| T11 | tooling stubs | none | none | ✅ |
| T12 | ROADMAP | none | none | ✅ |
| T13 | verify + STATE | none | none | ✅ |

---

## Status

| Task | Status | Assignee |
| ---- | ------ | -------- |
| T1 | Done | Batch 1 |
| T2 | Done | Batch 1 |
| T3 | Done | Batch 1 |
| T4 | Done | Batch 1 |
| T5 | Done | Batch 1 |
| T6 | Done | Batch 1 |
| T7 | Done | Batch 1 |
| T8 | Done | Batch 2 |
| T9 | Done | Batch 2 |
| T10 | Done | Batch 2 |
| T11 | Done | Batch 2 |
| T12 | Done | Batch 2 |
| T13 | Done | Batch 2 |

---

## Tools question (obrigatório antes do Execute)

Para cada task, quais ferramentas usar?

**MCPs disponíveis neste ambiente:** `plugin-linear-linear`, `user-context7` (docs de libs).  
**Skills relevantes:** `tlc-spec-driven` (obrigatória no Execute); demais skills de produto **não** devem dirigir refactors FE nesta feature (TARGET).

**Proposta default:** MCP NONE em todas; Skill `tlc-spec-driven` apenas; commits atômicos por task conforme acima.

Confirme ou ajuste.

---

## Next

1. Usuário **aprova tasks** (e matriz/gates)  
2. Usuário confirma tools (ou aceita default)  
3. Usuário diz **aprovado para Execute**  
4. Execute: offer batch sub-agents (2 batches) → Verifier no fim
