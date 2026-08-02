# mcp-agent-tools Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/mcp-agent-tools/design.md`  
**Spec**: `_docs/specs/features/mcp-agent-tools/spec.md`  
**Status**: Execute complete — awaiting Verifier  
**Branch sugerida**: `feat/mcp-agent-tools`  
**Commits**: prefixo `mcp:` (ex.: `mcp: add api-to-mcp whitelist config`)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `AGENTS.md` §7 (implementação + testes quando aplicável), `diversos/scripts/*.sh` (gate scripts bash), spec MCP-01…15.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| MCP bridge config (`api-to-mcp.yml`) | none | YAML válido; `readonly: true`; `only` com 12 IDs; sem secrets | `diversos/openapi/api-to-mcp.yml` | validado por T3 |
| MCP launcher script | none | `bash -n`; preserva carga env/PATH; `exec api-to-mcp` | `.cursor/scripts/mcp-sistema-folha.sh` | `bash -n .cursor/scripts/mcp-sistema-folha.sh` |
| Whitelist + policy validator | integration (script self-test) | MCP-14/15: IDs existem / faltantes falham; MCP-01/02/04/05/06: count 10–15, IDs obrigatórios, só GET, paths excluídos (organograma/import/auth-keys/dashboard) | `diversos/openapi/validate-mcp-whitelist.sh` | `bash diversos/openapi/validate-mcp-whitelist.sh --self-test` |
| Live API smoke (optional Docker) | integration (curl) | MCP-03/07: `obterInformacoesAcesso` 200 + `listarTodos` scoped quando API+key disponíveis; skip explícito se ausente | `diversos/openapi/smoke-mcp-api.sh` | `bash diversos/openapi/smoke-mcp-api.sh` |
| Operator docs | none | MCP-11…13: fluxo, folha vs cadastro, setup `api-to-mcp` | `diversos/openapi/README.md` | revisão manual |
| Backend / Frontend | regression (unchanged) | Nenhum código BE/FE alterado nesta feature | — | não obrigatório por task |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Syntax | T2 | `bash -n .cursor/scripts/mcp-sistema-folha.sh` |
| Validator self-test | T3 | `bash diversos/openapi/validate-mcp-whitelist.sh --self-test` |
| Validator (prod paths) | T3, T5 | `bash diversos/openapi/validate-mcp-whitelist.sh` |
| Live smoke (optional) | T5 | `bash diversos/openapi/smoke-mcp-api.sh` (skip OK se sem API/key) |
| **Full** | T5 fechamento | `bash -n .cursor/scripts/mcp-sistema-folha.sh && bash diversos/openapi/validate-mcp-whitelist.sh --self-test && bash diversos/openapi/validate-mcp-whitelist.sh` |

---

## Execution Plan

### Phase 1: Config + launcher

```
T1 → T2
```

### Phase 2: Validation scripts

```
T3
```

### Phase 3: Documentation

```
T4
```

### Phase 4: Gate + smoke evidence

```
T5
```

**Batch packing (~7 tasks/worker, whole phases):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | Phase 1–4 | T1–T5 | 5 |

→ **1 batch / execução inline** (≤ ~8 tasks). Verifier automático após T5 (não é task separada).

---

## Task Breakdown

### T1: Criar `api-to-mcp.yml` com whitelist curada

**What**: Adicionar config versionada do bridge com `readonly: true` e 12 `operationId` da spec.  
**Where**: `diversos/openapi/api-to-mcp.yml`  
**Depends on**: None  
**Reuses**: Curated tool set em `spec.md` / `design.md`  
**Requirements**: MCP-01, MCP-04, MCP-05, MCP-08

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Arquivo existe com `spec: sistema-folha-openapi.json` (sibling path)
- [x] `options.readonly: true`
- [x] `options.only` contém exatamente os 12 IDs: `obterInformacoesAcesso`, `listarTodos`, `consultarPorCompetencia`, `consultarPorPeriodo`, `listarMaisRecentes`, `consultarTotaisPorFuncionario`, `consultarPorPeriodo_1`, `buscarFichaPorFuncionario`, `listarCompetencias`, `resumoPorCompetencia`, `listar_2`, `listar`
- [x] **Nenhum** secret/API key no YAML
- [x] Gate: YAML parseável (`python3 -c "import yaml"` ou validação via T3 após T3)

**Tests**: none  
**Gate**: build (validado em T3)

**Commit**: `mcp: add api-to-mcp whitelist config`

---

### T2: Migrar launcher para `@sgaluza/api-to-mcp`

**What**: Trocar `exec` do bridge legado por `api-to-mcp rest --config` mantendo resolução de token e PATH.  
**Where**: `.cursor/scripts/mcp-sistema-folha.sh`  
**Depends on**: T1  
**Reuses**: Lógica atual de `mcp.env`, `OPENAPI_BEARER_TOKEN`, mensagens de erro  
**Requirements**: MCP-01, MCP-09, MCP-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `exec npx -y @sgaluza/api-to-mcp rest --config "${ROOT}/diversos/openapi/api-to-mcp.yml"`
- [x] `export OPENAPI_BEARER_TOKEN="${TOKEN}"` preservado antes do exec
- [x] Mensagem de erro quando key ausente **inalterada** em substância (MCP-10)
- [x] PATH asdf/homebrew preservado
- [x] Gate passes: `bash -n .cursor/scripts/mcp-sistema-folha.sh`

**Tests**: none  
**Gate**: Syntax (`bash -n`)

**Commit**: `mcp: switch launcher to api-to-mcp`

---

### T3: Validator whitelist + policy checks + self-test

**What**: Script que valida IDs da whitelist contra OpenAPI e política MCP (count, GET-only, exclusões). Incluir modo `--self-test` com caso positivo e negativo (MCP-15).  
**Where**: `diversos/openapi/validate-mcp-whitelist.sh` (+ helper Python inline ou `validate_mcp_whitelist.py` no mesmo dir se necessário)  
**Depends on**: T1  
**Reuses**: `diversos/openapi/sistema-folha-openapi.json`, `api-to-mcp.yml`  
**Requirements**: MCP-01, MCP-02, MCP-04, MCP-05, MCP-06, MCP-14, MCP-15

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `validate-mcp-whitelist.sh` (default) exit **0** contra yaml+spec atuais
- [x] Assert: `len(only)` entre **10 e 15** (spec: 12)
- [x] Assert: IDs obrigatórios presentes — `obterInformacoesAcesso`, `listarTodos`, `consultarTotaisPorFuncionario`, `listarCompetencias`
- [x] Assert: cada ID whitelisted mapeia para método **GET** na spec
- [x] Assert: nenhum path whitelisted começa com `/organograma`, `/importacao`, `/auth/api-keys`, `/dashboard` (MCP-06)
- [x] Assert: nenhum ID mutável conhecido na lista (`processar`, `cadastrar`, `importarFolhaAdp`, etc.)
- [x] `--self-test`: caso **negativo** — yaml temp com ID fictício → exit **≠ 0** + stderr lista ID faltante (MCP-15)
- [x] `--self-test`: caso **positivo** — exit **0**
- [x] Gate passes: `bash diversos/openapi/validate-mcp-whitelist.sh --self-test` (**≥ 2 assertions** no self-test)

**Tests**: integration (script self-test)  
**Gate**: Validator self-test

**Commit**: `mcp: add whitelist validator with self-test`

---

### T4: Atualizar README MCP (setup + roteamento agente)

**What**: Documentar `@sgaluza/api-to-mcp`, fluxo recomendado, distinção folha/cadastro/fora-MCP, instrução Refresh Cursor.  
**Where**: `diversos/openapi/README.md`  
**Depends on**: T1, T3  
**Reuses**: Tabela de roteamento do `design.md`  
**Requirements**: MCP-11, MCP-12, MCP-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Referência `@sgaluza/openapi-mcp-bridge` **substituída** por `@sgaluza/api-to-mcp`
- [x] Seção **fluxo recomendado** (escopo → resumo → totais → benefícios)
- [x] Tabela distingue **folha** vs **cadastro** (`listar_2`) vs **fora do MCP**
- [x] Nota obsoleta “funcionários sem ACL” **corrigida** (fix2)
- [x] Menciona `validate-mcp-whitelist.sh` pós-regen OpenAPI
- [x] Menciona Settings → MCP → Refresh após mudanças
- [x] operationIds batem com `api-to-mcp.yml`

**Tests**: none  
**Gate**: build (doc review)

**Commit**: `mcp: document agent tool routing in openapi README`

---

### T5: Smoke API live + fechamento de gate

**What**: Script curl opcional para MCP-03/07 quando Docker+key disponíveis; registrar evidência; rodar Full gate.  
**Where**: `diversos/openapi/smoke-mcp-api.sh`  
**Depends on**: T2, T3, T4  
**Reuses**: `.cursor/mcp.env`, baseline Humberto (10 emp 05/2026)  
**Requirements**: MCP-03, MCP-07

**Tools**:

- MCP: `project-0-sistema-folha-sistema-folha` (evidência adicional via CallMcpTool se servidor Ready)
- Skill: NONE

**Done when**:

- [x] `smoke-mcp-api.sh`: carrega key de `.cursor/mcp.env` ou env; **skip** com exit 0 + mensagem se API down ou key ausente
- [x] Quando API up: `GET /auth/acesso` → **200**; `GET /resumo-folha-pagamento?ano=2026&mes=5` → **200** com body parseável
- [x] Quando API up + key scoped: resposta folha SHALL ter cardinalidade **≤ global** (não 310 se scoped — evidência MCP-07 qualitativa)
- [x] Full gate passes:
  ```bash
  bash -n .cursor/scripts/mcp-sistema-folha.sh
  bash diversos/openapi/validate-mcp-whitelist.sh --self-test
  bash diversos/openapi/validate-mcp-whitelist.sh
  ```
- [ ] Atualizar `spec.md` traceability: MCP-03…15 → Implementing/Done conforme evidência
- [ ] **Verifier** dispara automaticamente pós-commit (gera `validation.md`)

**Tests**: integration (live smoke, skippable)  
**Gate**: Full

**Commit**: `mcp: add optional live API smoke for MCP agent tools`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 ──→ T2
Phase 2:  T3
Phase 3:  T4
Phase 4:  T5 ──→ [Verifier auto]
```

Execução estritamente sequencial — 1 batch inline (5 tasks).

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: api-to-mcp.yml | 1 config file | ✅ Granular |
| T2: launcher script | 1 script modify | ✅ Granular |
| T3: validator + self-test | 1 script (+ helper) | ✅ Granular |
| T4: README MCP section | 1 doc file | ✅ Granular |
| T5: smoke + gate closure | 1 script + evidence | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | (start) | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T1 | T1 → T3 (via Phase 2 after T2) | ✅ Match |
| T4 | T1, T3 | T3 → T4 | ✅ Match |
| T5 | T2, T3, T4 | T2,T3,T4 → T5 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1: api-to-mcp.yml | MCP config | none | none | ✅ OK |
| T2: launcher | Bash launcher | none | none | ✅ OK |
| T3: validator | Whitelist validator | integration | integration | ✅ OK |
| T4: README | Operator docs | none | none | ✅ OK |
| T5: smoke | Live API smoke | integration | integration | ✅ OK |

---

## Requirement Traceability (Tasks)

| Requirement ID | Task(s) | Status |
| -------------- | ------- | ------ |
| MCP-01 | T1, T2, T3 | Done (10c17cf, 0b0fd4d, 8702cbe) |
| MCP-02 | T3 | Done (8702cbe) |
| MCP-03 | T5 | Done (ee6c5c8) |
| MCP-04 | T1, T3 | Done (10c17cf, 8702cbe) |
| MCP-05 | T1, T3 | Done (10c17cf, 8702cbe) |
| MCP-06 | T3 | Done (8702cbe) |
| MCP-07 | T5 | Done (ee6c5c8) |
| MCP-08 | T1 | Done (10c17cf) |
| MCP-09 | T2 | Done (0b0fd4d) |
| MCP-10 | T2 | Done (0b0fd4d) |
| MCP-11 | T4 | Done (8143549) |
| MCP-12 | T4 | Done (8143549) |
| MCP-13 | T4 | Done (8143549) |
| MCP-14 | T3 | Done (8702cbe) |
| MCP-15 | T3 | Done (8702cbe) |

**Coverage:** 15/15 mapped ✅

---

## MCPs & Skills (confirm before Execute)

| Task | Recommended MCP | Recommended Skill |
| ---- | --------------- | ----------------- |
| T1 | NONE | NONE |
| T2 | NONE | NONE |
| T3 | NONE | NONE |
| T4 | NONE | NONE |
| T5 | Cursor MCP `sistema-folha` (evidência CallMcpTool) | NONE |

**Proposta default:** MCP NONE em T1–T4; MCP nativo só em T5 para evidência MCP-07; skills NONE; commits `mcp:` conforme acima; **1 batch inline**, sem sub-agents.

**Confirme antes do Execute:** aprovar tasks + ferramentas acima, ou indicar ajustes.
