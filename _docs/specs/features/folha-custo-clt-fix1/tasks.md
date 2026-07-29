# folha-custo-clt-fix1 Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/folha-custo-clt-fix1/design.md`  
**Spec**: `_docs/specs/features/folha-custo-clt-fix1/spec.md`  
**Status**: Execute complete — Batch 1 (T1–T8)  
**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md`, AD-004 (FE skills = target; cobertura atual = lint/build), AD-010, `folha-custo-clt-fix1/spec.md` (FIX1-01…13 + edge cases).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Folha port adapter (`FolhaProcessamentoAdapter`) | unit (Mockito) | Delegação a `FolhaProcessamentoService`; `recalcularFerias` → `ProcessamentoOpcoes`; retorno `ProcessamentoResultadoDTO` | `backend/src/test/java/**/folha/application/FolhaProcessamentoAdapterTest.java` | `cd backend && mvn test -Dtest=FolhaProcessamentoAdapterTest` |
| Importação orquestrador (`ImportacaoFolhaAdpService`) | unit (Mockito) | FIX1-01, 03, 04, 05; edge: substituição confirmada encadeia; process port throw propaga; `recalcularFerias=false`; zero fichas OK | `backend/src/test/java/**/importacao/application/ImportacaoFolhaAdpServiceTest.java` | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpServiceTest` |
| Processamento motor (regressão) | unit (Mockito) | Zero regressão em materialização existente | `**/FolhaProcessamentoServiceTest.java` | `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest` |
| Importação controller / response DTO | none | FIX1-02/03 cobertos por service + inspeção manual do mapping; compile gate | `importacao/api/*` | compile via Quick/Full gate |
| Port interface / result record | none | Compile gate only | `folha/port/*`, `importacao/application/ImportacaoFolhaAdpResult.java` | `cd backend && mvn clean compile` |
| ArchUnit modular (AD-010) | unit | `importacao.application` zero foreign infra após injeção da port | `**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Security `/processar` ADMIN (regressão) | unit (`@WebMvcTest`) | FIX1-11 — 403 non-ADMIN inalterado | `**/SecurityConfigFolhaProcessamentoTest.java` | `cd backend && mvn test -Dtest=SecurityConfigFolhaProcessamentoTest` |
| Frontend pages/services | none (AD-004 gap) | FIX1-06…13 via lint + build; walkthrough manual `/importacao` | `frontend/src/pages/Importacao/**`, `frontend/src/services/**` | `cd frontend && npm run lint && npm run build` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após T1–T4 (backend) | `cd backend && mvn test -Dtest=FolhaProcessamentoAdapterTest,ImportacaoFolhaAdpServiceTest,FolhaProcessamentoServiceTest,ModularArchitectureTest` |
| Full | Após T8 (fechamento feature) | `cd backend && mvn test && cd ../frontend && npm run lint && npm run build` |
| Build | T1 (port interface only) | `cd backend && mvn clean compile` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Folha processamento port (2 tasks)

```
T1 → T2
```

### Phase 2: Backend encadeamento + API (2 tasks)

```
T3 → T4
```

### Phase 3: Frontend importação (3 tasks)

```
T5 → T6 → T7
```

### Phase 4: Fechamento (1 task)

```
T8
```

**Batch packing (~7 tasks/worker, whole phases):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | Phase 1–4 | T1–T8 | 8 |

→ **1 batch / execução inline** (≤ ~8 tasks). Sub-agents não necessários salvo pedido explícito.

---

## Task Breakdown

### T1: FolhaProcessamentoPort

**What**: Interface cross-domain para materializar ficha pós-import ADP.  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/folha/port/FolhaProcessamentoPort.java`  
**Depends on**: None  
**Reuses**: Assinatura alinhada a `FolhaProcessamentoService.processar`; padrão `FolhaImportacaoPort`  
**Requirement**: FIX1-01 (contrato)

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles` (port boundary)

**Done when**:
- [x] Interface com `processar(competenciaInicio, competenciaFim, decimoTerceiro, recalcularFerias)` retornando `ProcessamentoResultadoDTO`
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: Build

**Commit**: `feat(folha): add FolhaProcessamentoPort for post-import processing`

---

### T2: FolhaProcessamentoAdapter + unit test

**What**: Adapter `@Service` que delega a `FolhaProcessamentoService` com `@Transactional`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaProcessamentoAdapter.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaProcessamentoAdapterTest.java`

**Depends on**: T1  
**Reuses**: `FolhaImportacaoAdapter` (delegação + `@RequiredArgsConstructor`)  
**Requirement**: FIX1-01

**Tools**:
- MCP: NONE
- Skill: `jpa-performance` (`@Transactional` REQUIRED)

**Done when**:
- [x] Adapter implementa `FolhaProcessamentoPort`
- [x] `recalcularFerias` mapeia para `new ProcessamentoOpcoes(recalcularFerias)`
- [x] Teste verifica delegação com competência e flags corretas
- [x] Gate: `cd backend && mvn test -Dtest=FolhaProcessamentoAdapterTest`
- [x] Test count: ≥2 testes novos passando

**Tests**: unit  
**Gate**: Quick (adapter only)

**Commit**: `feat(folha): add FolhaProcessamentoAdapter delegating to processamento service`

---

### T3: ImportacaoFolhaAdpService — encadeamento + testes

**What**: Orquestrar import → process na mesma TX; record `ImportacaoFolhaAdpResult`; testes FIX1-01…05 e edge cases.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/importacao/application/ImportacaoFolhaAdpResult.java` (new)
- `backend/src/main/java/br/com/techne/sistemafolha/importacao/application/ImportacaoFolhaAdpService.java` (modify)
- `backend/src/test/java/br/com/techne/sistemafolha/importacao/application/ImportacaoFolhaAdpServiceTest.java` (modify)

**Depends on**: T2  
**Reuses**: Fluxo parse/persist existente; mocks de `FolhaImportacaoPort`  
**Requirement**: FIX1-01, FIX1-03, FIX1-04, FIX1-05; edge: substituição confirmada; zero fichas CLT

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`, `spring-boot-new-endpoint` (orquestração)

**Done when**:
- [x] Após `persistirImportacao`, invoca `folhaProcessamentoPort.processar(..., false)`
- [x] Retorno `ImportacaoFolhaAdpResult` com folhas + `ProcessamentoResultadoDTO`
- [x] Import falha pré-persist → `processar` **never** invoked (FIX1-04)
- [x] Process port throws → exceção propaga (FIX1-03 rollback via TX)
- [x] Substituir existente confirmado → encadeia process (edge spec)
- [x] Process retorna `totalFichas=0` → result ainda composto (edge spec)
- [x] Gate: `cd backend && mvn test -Dtest=ImportacaoFolhaAdpServiceTest`
- [x] Test count: ≥4 testes novos/atualizados passando (baseline 4 existentes preservados)

**Tests**: unit  
**Gate**: Quick

**Commit**: `feat(importacao): chain ADP import with ficha processing via port`

---

### T4: ImportacaoFolhaAdpResponseDTO + controller

**What**: Estender response HTTP com stats de processamento; controller mapeia `ImportacaoFolhaAdpResult` e mensagens FIX1-02/03.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/importacao/api/ImportacaoFolhaAdpResponseDTO.java` (modify)
- `backend/src/main/java/br/com/techne/sistemafolha/importacao/api/ImportacaoFolhaAdpController.java` (modify)

**Depends on**: T3  
**Reuses**: Factories `success`/`error` existentes; `FolhaDuplicadaException` → 409 inalterado  
**Requirement**: FIX1-02, FIX1-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Campos `fichasProcessadas`, `linhasProcessadas` no record (nullable em error/conflict)
- [x] `success=true` só quando import **e** process OK; mensagem composta (registros + fichas/linhas)
- [x] Falha processamento: `success=false`, mensagem prefixo *"Falha no processamento da ficha"*
- [x] Gate: `cd backend && mvn test -Dtest=ImportacaoFolhaAdpServiceTest,FolhaProcessamentoAdapterTest`
- [x] Compila sem erros

**Tests**: none (controller — service tests + compile)  
**Gate**: Quick

**Commit**: `feat(importacao): extend ADP import response with processing stats`

---

### T5: Frontend — tipos + feedback upload ADP

**What**: Estender `ImportacaoResponse`; toast/loading/mensagem de erro no fluxo encadeado.  
**Where**:
- `frontend/src/types/index.ts` (modify)
- `frontend/src/pages/Importacao/index.tsx` (modify — card Folha ADP)

**Depends on**: T4  
**Reuses**: `UploadState`, `toast`, timeout 5 min em `importacaoService`  
**Requirement**: FIX1-06, FIX1-07, FIX1-08

**Tools**:
- MCP: NONE
- Skill: `component-architecture`, `api-client` (tipos response)

**Done when**:
- [x] `ImportacaoResponse` inclui `fichasProcessadas?`, `linhasProcessadas?`
- [x] Loading ADP: texto *"Importando e processando ficha…"*
- [x] Sucesso: toast usa `response.message` (composto) — não só *"arquivo importado"*
- [x] Erro API: toast erro; `success` state não fica true (FIX1-07)
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: Build (frontend)

**Commit**: `feat(frontend): ADP import UI reflects chained processing`

---

### T6: Frontend — folhaPagamentoService.processarCompetencia

**What**: Cliente HTTP para `POST /folha-pagamento/processar`.  
**Where**: `frontend/src/services/folhaPagamentoService.ts` (modify)

**Depends on**: T5  
**Reuses**: Instância `api` axios; shape `ProcessamentoRequestDTO`  
**Requirement**: FIX1-10, FIX1-12

**Tools**:
- MCP: NONE
- Skill: `api-client`

**Done when**:
- [x] Método `processarCompetencia` POST com body `{ competenciaInicio, competenciaFim, decimoTerceiro, opcoes: { recalcularFerias } }`
- [x] Retorno tipado `{ totalFichas, totalLinhas, totalFuncionarios }`
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: Build (frontend)

**Commit**: `feat(frontend): add processarCompetencia to folhaPagamentoService`

---

### T7: Frontend — seção reprocesso manual

**What**: Card **"Processar ficha da competência"** abaixo do card Folha ADP em `/importacao`.  
**Where**: `frontend/src/pages/Importacao/index.tsx` (modify)

**Depends on**: T6  
**Reuses**: `MESES`, `gerarAnosDisponiveis`, `competenciaParams`, padrão selects benefícios  
**Requirement**: FIX1-09, FIX1-10, FIX1-11, FIX1-12, FIX1-13; edge: competência sem ADP informa totais

**Tools**:
- MCP: NONE
- Skill: `component-architecture`, `forms-validation`

**Done when**:
- [x] Seção visível com mês/ano, checkbox 13º, checkbox recalcular férias, botão Processar
- [x] Submit chama `folhaPagamentoService.processarCompetencia`
- [x] Sucesso: toast *"Ficha processada: X fichas, Y linhas"* (FIX1-13)
- [x] Erro 403: mensagem de permissão (FIX1-11)
- [x] `recalcularFerias=true` quando checkbox marcado (FIX1-12)
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: Build (frontend)

**Commit**: `feat(frontend): manual ficha processing section on import page`

---

### T8: Cross-ref FCLT-04 + gate final

**What**: Atualizar traceability em `folha-custo-clt/spec.md`; rodar gate full + regressões security/arch.  
**Where**:
- `_docs/specs/features/folha-custo-clt/spec.md` (modify — FCLT-04 note)
- `_docs/specs/features/folha-custo-clt-fix1/spec.md` (modify — traceability → tasks mapped)

**Depends on**: T7  
**Reuses**: FIX1-CTX-06  
**Requirement**: FIX1-01…13 (closure); FCLT-04 cross-ref

**Tools**:
- MCP: NONE
- Skill: `tlc-spec-driven` (Verifier na sequência automática)

**Done when**:
- [x] `folha-custo-clt/spec.md` anota que FCLT-04 refinado por `folha-custo-clt-fix1`
- [x] `folha-custo-clt-fix1/spec.md` traceability: todos FIX1-* → Done ou mapped
- [x] Gate Full passa: `cd backend && mvn test && cd ../frontend && npm run lint && npm run build`
- [x] Regressão: `SecurityConfigFolhaProcessamentoTest`, `ModularArchitectureTest`, `FolhaProcessamentoServiceTest`

**Tests**: none (docs + gate)  
**Gate**: Full

**Commit**: `docs(specs): map folha-custo-clt-fix1 tasks and FCLT-04 cross-ref`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 ──→ T2
Phase 2:  T3 ──→ T4
Phase 3:  T5 ──→ T6 ──→ T7
Phase 4:  T8
```

Execution is strictly sequential — one task at a time, in order.

---

## Requirement Traceability (Task Map)

| Requirement | Task(s) | Verify via |
| ----------- | ------- | ---------- |
| FIX1-01 | T2, T3 | `ImportacaoFolhaAdpServiceTest` — port invoked pós-persist, mesma competência, `recalcularFerias=false` |
| FIX1-02 | T4 | Response DTO + controller success message |
| FIX1-03 | T3, T4 | Service test port throws; controller error message |
| FIX1-04 | T3 | Service test — never process on import fail |
| FIX1-05 | T3 | Service test + `FolhaProcessamentoServiceTest` regressão |
| FIX1-06 | T5 | Manual / walkthrough toast composto |
| FIX1-07 | T5 | Manual / walkthrough erro sem success state |
| FIX1-08 | T5 | Manual / walkthrough loading text |
| FIX1-09 | T7 | Manual — seção visível |
| FIX1-10 | T6, T7 | Manual — POST processar |
| FIX1-11 | T7, T8 gate | `SecurityConfigFolhaProcessamentoTest` + UI 403 |
| FIX1-12 | T6, T7 | Manual — checkbox → request body |
| FIX1-13 | T7 | Manual — toast sucesso manual |

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: FolhaProcessamentoPort | 1 interface | ✅ Granular |
| T2: FolhaProcessamentoAdapter + test | 1 adapter + 1 test file | ✅ Granular |
| T3: Service encadeamento + tests | 1 service + 1 record + tests | ✅ Granular (coeso) |
| T4: Response DTO + controller | 2 arquivos API acoplados | ✅ OK (coeso — mesmo contrato HTTP) |
| T5: FE tipos + upload UX | 2 arquivos FE relacionados | ✅ OK (mesmo fluxo ADP) |
| T6: processarCompetencia | 1 service method | ✅ Granular |
| T7: Seção manual UI | 1 page section | ✅ Granular |
| T8: Docs + gate | traceability + full test | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | (start) | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | T7 | T7 → T8 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Port interface | none | none | ✅ OK |
| T2 | Folha adapter | unit | unit | ✅ OK |
| T3 | Import service + result | unit | unit | ✅ OK |
| T4 | Controller + DTO | none | none | ✅ OK |
| T5 | FE page/types | none | none | ✅ OK |
| T6 | FE service | none | none | ✅ OK |
| T7 | FE page | none | none | ✅ OK |
| T8 | Docs only | none | none | ✅ OK |

---

## Tools & Skills (Execute)

Before starting Execute, confirm tool usage per task:

| Task | Recommended Skills |
| ---- | ------------------ |
| T1–T2 | `jpa-performance`, `modular-design-principles` |
| T3–T4 | `jpa-performance`, `spring-boot-new-endpoint` |
| T5–T7 | `api-client`, `component-architecture` |
| T8 | `tlc-spec-driven` (Verifier automático pós-T8) |

**Available MCPs:** Context7 (Spring/JPA docs if needed), SonarQube (optional post-gate), Linear (issue tracking if applicable).
