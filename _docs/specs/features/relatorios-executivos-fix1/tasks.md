# relatorios-executivos-fix1 — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/relatorios-executivos-fix1/design.md`  
**Spec**: `_docs/specs/features/relatorios-executivos-fix1/spec.md`  
**Branch**: `feat/relatorios-executivos` · **Commit prefix**: `fix1:`  
**Status**: ✅ Execute complete — T1–T6 done · Verifier PASS (fix cycle 1) · HEAD `7f13eeb`

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md`, `relatorios-executivos-fix1/spec.md` (FIX1 ACs + edge cases).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Stale detector / recovery tracker | unit (Mockito) | FIX1-05; threshold `timeout+grace`; `data_criacao` null → stale; blob present → non-stale | `backend/src/test/java/**/relatorios/application/RelatorioStaleDetectorTest.java`, `RelatorioRecoveryTrackerTest.java` | `cd backend && mvn test -Dtest=RelatorioStaleDetectorTest,RelatorioRecoveryTrackerTest` |
| Stale recovery service | unit (Mockito) | FIX1-06…08; reenqueue once; 2ª detecção → ERRO `"Tempo esgotado na geração"`; contagem exclui stale | `backend/src/test/java/**/relatorios/application/RelatorioStaleRecoveryServiceTest.java` | `cd backend && mvn test -Dtest=RelatorioStaleRecoveryServiceTest` |
| Relatorio geracao worker | unit (Mockito) | FIX1-01…04; sucesso/ERRO terminal; id inexistente; `ativo=false`+PENDENTE; `finally` safety-net | `backend/src/test/java/**/relatorios/application/RelatorioGeracaoWorkerTest.java` | `cd backend && mvn test -Dtest=RelatorioGeracaoWorkerTest` |
| Relatorio geracao service | unit (Mockito) | FIX1-09…12; 3 stale ≠ 429; 3 active = 429; recovery antes do limite; re-POST reenfileira; listagem por `usuarioId` | `backend/src/test/java/**/relatorios/application/RelatorioGeracaoServiceTest.java` | `cd backend && mvn test -Dtest=RelatorioGeracaoServiceTest` |
| Controllers REST | WebMvc (MockMvc) | FIX1-10; POST 429 mensagem limite; GET lista scoped (mock service) | `backend/src/test/java/**/relatorios/api/RelatorioFolhaControllerWebMvcTest.java`, `RelatorioBeneficioControllerWebMvcTest.java` | `cd backend && mvn test -Dtest=RelatorioFolhaControllerWebMvcTest,RelatorioBeneficioControllerWebMvcTest` |
| RelatorioArquivo entity | unit (Mockito) | FIX1-22…23; persist PDF bytes sem falha de mapping | `backend/src/test/java/**/relatorios/application/RelatorioGeracaoWorkerTest.java` (persist path) | `cd backend && mvn test -Dtest=RelatorioGeracaoWorkerTest` |
| Config / properties / yml | none | Compile gate | `RelatorioGeracaoProperties.java`, `application.yml` | `cd backend && mvn clean compile` |
| Frontend hub / card / service | unit (Vitest) | FIX1-13…18, FIX1-21; 429/403/timeout toasts; stale retry; multi-user card scope; POST timeout ≥65s | `frontend/src/pages/Relatorios/**/*.test.tsx`, `frontend/src/services/relatorioService.test.ts` (se criado) | `cd frontend && npm run test -- src/pages/Relatorios` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick BE | Após task backend com unit tests | `cd backend && mvn test -Dtest=<ClassTest>` |
| Quick FE | Após task frontend | `cd frontend && npm run test -- src/pages/Relatorios` |
| Full fix1 | Após T6 + Verifier | `cd backend && mvn test -Dtest=RelatorioStaleDetectorTest,RelatorioRecoveryTrackerTest,RelatorioStaleRecoveryServiceTest,RelatorioGeracaoWorkerTest,RelatorioGeracaoServiceTest,RelatorioFolhaControllerWebMvcTest,RelatorioBeneficioControllerWebMvcTest && cd ../frontend && npm run test -- src/pages/Relatorios && npm run build` |
| Build | Config/properties-only slices | `cd backend && mvn clean compile` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Stale detection foundation (1 task)

```
T1
```

### Phase 2: Worker terminal integrity (1 task)

```
T2
```

### Phase 3: Stale recovery orchestration (1 task)

```
T3
```

### Phase 4: Service, DTOs e API (1 task)

```
T4
```

### Phase 5: Frontend errors, stale retry e testes (1 task)

```
T5
```

### Phase 6: BYTEA mapping P2 (1 task)

```
T6
```

**Batch packing (~7 tasks/worker, whole phases):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | Phase 1–6 | T1–T6 | 6 |

→ **1 batch** — Execute inline (≤ ~8 tasks). Verifier automático após T6.

---

## Task Breakdown

### T1: Stale detector, recovery tracker e config

**What**: Criar `RelatorioStaleDetector` (regra FIX1-05), `RelatorioRecoveryTracker` (in-memory attempted flag), adicionar `staleGraceSegundos` em `RelatorioGeracaoProperties` + `application.yml`; constante compartilhada `ERRO_TEMPO_ESGOTADO = "Tempo esgotado na geração"`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioStaleDetector.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioRecoveryTracker.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioGeracaoProperties.java`
- `backend/src/main/resources/application.yml`

**Depends on**: None  
**Reuses**: `RelatorioGeracaoProperties`, `Clock` injetável (padrão Spring)  
**Requirement**: FIX1-05

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `isStale()` retorna true para PENDENTE sem blob quando idade > `timeoutSegundos + staleGraceSegundos`
- [x] `data_criacao == null` tratado como stale imediato (edge case spec)
- [x] Tracker: `markAttempted` / `hasAttempted` / `clear` thread-safe
- [x] Gate: `cd backend && mvn test -Dtest=RelatorioStaleDetectorTest,RelatorioRecoveryTrackerTest`
- [x] Test count: ≥6 novos testes passam

**Tests**: unit  
**Gate**: quick BE

**Commit**: `fix1(relatorios): stale detector, recovery tracker e config grace`

---

### T2: Worker — estados terminais obrigatórios

**What**: Corrigir `RelatorioGeracaoWorker.processar`: logs INFO início/fim; `ativo=false` + PENDENTE → ERRO; `finally` safety-net se ainda PENDENTE; integrar `RelatorioRecoveryTracker.clear` em sucesso/ERRO; testes FIX1-01…04.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioGeracaoWorker.java`
- `backend/src/test/java/br/com/techne/sistemafolha/relatorios/application/RelatorioGeracaoWorkerTest.java`

**Depends on**: T1  
**Reuses**: `marcarErro`, `truncarErro`, testes existentes de sucesso/falha/tamanho  
**Requirement**: FIX1-01, FIX1-02, FIX1-03, FIX1-04

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Sucesso → PROCESSADO + blob + `dataProcessamento` (FIX1-01)
- [x] Falha render/persist → ERRO + mensagem ≤500 chars (FIX1-02)
- [x] `findById` null → sem save, WARN only (FIX1-03)
- [x] `ativo=false` + PENDENTE → ERRO (FIX1-03 variant)
- [x] Gate: `cd backend && mvn test -Dtest=RelatorioGeracaoWorkerTest`
- [x] Test count: ≥4 testes worker passam (incl. ≥2 novos)

**Tests**: unit  
**Gate**: quick BE

**Commit**: `fix1(relatorios): worker estados terminais e safety-net`

---

### T3: Stale recovery service e repository sweep

**What**: Adicionar `RelatorioRepository.findByUsuarioIdAndStatusAndAtivoTrue`; criar `RelatorioStaleRecoveryService` com `recuperarParaUsuario`, `recuperarRelatorio`, `contarPendentesAtivos`; wiring de reenqueue via `Consumer<Long>` (afterCommit, extraído de `RelatorioGeracaoService` ou `@Configuration` bean).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/infrastructure/RelatorioRepository.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioStaleRecoveryService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioGeracaoService.java` (extrair enqueue apenas — sem hooks listar/gerar ainda)
- `backend/src/test/java/br/com/techne/sistemafolha/relatorios/application/RelatorioStaleRecoveryServiceTest.java`

**Depends on**: T1, T2  
**Reuses**: `RelatorioStaleDetector`, `RelatorioRecoveryTracker`, `TransactionTemplate`, padrão `afterCommit` existente  
**Requirement**: FIX1-06, FIX1-07, FIX1-08

**Tools**:
- MCP: NONE
- Skill: `jpa-performance` (query por usuarioId+status)

**Done when**:
- [x] 1ª detecção stale → reenqueue worker + `markAttempted` (FIX1-06)
- [x] 2ª detecção stale → ERRO `"Tempo esgotado na geração"` (FIX1-07)
- [x] `contarPendentesAtivos` exclui stale / pós-promoção ERRO (FIX1-08)
- [x] Gate: `cd backend && mvn test -Dtest=RelatorioStaleRecoveryServiceTest`
- [x] Test count: ≥5 testes recovery passam

**Tests**: unit  
**Gate**: quick BE

**Commit**: `fix1(relatorios): stale recovery service lazy reenqueue`

---

### T4: Service integration, DTOs e WebMvc 429

**What**: Integrar recovery em `listarFolha`/`listarBeneficio`/`iniciarGeracao`; trocar listagem para `findByUsuarioIdAndTipo...`; substituir contagem 429 por `contarPendentesAtivos`; DTOs + `to*Dto` com `dataCriacao` e `stale`; atualizar WebMvc tests e construtores DTO nos testes existentes.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioGeracaoService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/api/RelatorioFolhaDTO.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/api/RelatorioBeneficioDTO.java`
- `backend/src/test/java/br/com/techne/sistemafolha/relatorios/application/RelatorioGeracaoServiceTest.java`
- `backend/src/test/java/br/com/techne/sistemafolha/relatorios/api/RelatorioFolhaControllerWebMvcTest.java`
- `backend/src/test/java/br/com/techne/sistemafolha/relatorios/api/RelatorioBeneficioControllerWebMvcTest.java`

**Depends on**: T3  
**Reuses**: `RelatorioStaleRecoveryService`, queries existentes por `usuarioId`  
**Requirement**: FIX1-09, FIX1-10, FIX1-11, FIX1-12, FIX1-19, FIX1-20, FIX1-21

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint` (DTO contract)

**Done when**:
- [x] 3 jobs stale → POST retorna 200/202, **not** 429 (FIX1-09, FIX1-11)
- [x] 3 jobs PENDENTE non-stale → 429 com mensagem limite (FIX1-10)
- [x] Re-POST tupla PENDENTE non-stale reenfileira worker (FIX1-12)
- [x] GET lista retorna só relatórios do usuário autenticado; DTO inclui `dataCriacao`, `stale` (FIX1-19…21)
- [x] Gate: `cd backend && mvn test -Dtest=RelatorioGeracaoServiceTest,RelatorioFolhaControllerWebMvcTest,RelatorioBeneficioControllerWebMvcTest`
- [x] Test count: suite relatorios backend verde (≥4 novos cenários)

**Tests**: unit + WebMvc  
**Gate**: quick BE (classes acima)

**Commit**: `fix1(relatorios): recovery hooks, DTO stale e limite 429 corrigido`

---

### T5: Frontend — erros acionáveis, stale retry e Vitest

**What**: `resolveRelatorioApiError` em `relatorioService.ts`; tipos `dataCriacao`/`stale`; `RelatorioCatalogCard` prop `stale` (retry vs progress); `Relatorios/index.tsx` remove early-return cego, toasts 429/403/timeout, polling só para `PENDENTE && !stale`; testes Vitest.  
**Where**:
- `frontend/src/services/relatorioService.ts`
- `frontend/src/pages/Relatorios/RelatorioCatalogCard.tsx`
- `frontend/src/pages/Relatorios/index.tsx`
- `frontend/src/pages/Relatorios/Relatorios.test.tsx`

**Depends on**: T4  
**Reuses**: `useNotification`, `RelatorioCatalogCard` retry existente para ERRO, timeout 65s já em POST  
**Requirement**: FIX1-13, FIX1-14, FIX1-15, FIX1-16, FIX1-17, FIX1-18, FIX1-21

**Tools**:
- MCP: NONE
- Skill: `testing-a11y`, `forms-validation` (RFC 7807 error shape — read `response.status`)

**Done when**:
- [x] POST usa timeout ≥65000ms (assert em teste ou mock config) (FIX1-13)
- [x] Toast distingue 429, 403, timeout cliente (FIX1-14…16)
- [x] Card `PENDENTE && stale` → botão "Tentar novamente" habilitado (FIX1-17)
- [x] Card `PENDENTE && !stale` → progress desabilitado (FIX1-18)
- [x] Lista multi-usuário mock: card mostra status do usuário logado only (FIX1-21)
- [x] Gate: `cd frontend && npm run test -- src/pages/Relatorios`
- [x] Test count: ≥5 novos cenários Vitest passam

**Tests**: unit (Vitest)  
**Gate**: quick FE

**Commit**: `fix1(relatorios): FE erros HTTP, stale retry e card scoped`

---

### T6: RelatorioArquivo BYTEA mapping P2

**What**: Remover `@Lob` de `RelatorioArquivo.pdfBytes`; mapear BYTEA explícito (`columnDefinition` ou `@JdbcTypeCode`); estender teste worker para assert persist após sucesso (FIX1-22…23).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/domain/RelatorioArquivo.java`
- `backend/src/test/java/br/com/techne/sistemafolha/relatorios/application/RelatorioGeracaoWorkerTest.java`

**Depends on**: T2  
**Reuses**: Flyway `V1.28` schema BYTEA; teste `processar_sucesso_persistePdfEAtualizaStatusProcessado`  
**Requirement**: FIX1-22, FIX1-23

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Entity sem `@Lob`; mapping compatível PostgreSQL BYTEA + dev `ddl-auto: update`
- [x] Worker test confirma `relatorioArquivoRepository.save` com bytes PDF (FIX1-23)
- [x] Gate full fix1: comando **Full fix1** da tabela Gate Check Commands
- [x] Backend + frontend suites relatorios verdes

**Tests**: unit  
**Gate**: full fix1

**Commit**: `fix1(relatorios): mapeamento BYTEA explícito relatorio_arquivo`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6

Phase 1:  T1
Phase 2:  T2
Phase 3:  T3
Phase 4:  T4
Phase 5:  T5
Phase 6:  T6
```

Execution is strictly sequential — 1 batch inline (6 tasks).

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Stale detector + tracker + config | 3 classes + yml + tests | ✅ Granular (coeso: foundation) |
| T2: Worker terminal paths | 1 class + tests | ✅ Granular |
| T3: Stale recovery service | 1 service + repo method + enqueue extract | ✅ Granular |
| T4: Service + DTOs + WebMvc | Service layer + API contract + tests | ✅ Granular (1 integration slice) |
| T5: FE errors + card + Vitest | 3 FE files + tests | ✅ Granular (1 UI slice) |
| T6: BYTEA entity mapping | 1 entity + test extend | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | T1 (Phase 1 root) | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T1, T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T2 | T2 → T6 (parallel tail after T5 in sequence) | ✅ Match — T6 runs after T5 in phase order; depends T2 only (BYTEA independent of FE) |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Stale detector / tracker | unit | unit | ✅ OK |
| T2 | Worker | unit | unit | ✅ OK |
| T3 | Recovery service | unit | unit | ✅ OK |
| T4 | Service + controllers | unit + WebMvc | unit + WebMvc | ✅ OK |
| T5 | Frontend hub / service | unit (Vitest) | unit (Vitest) | ✅ OK |
| T6 | Entity + worker persist | unit | unit | ✅ OK |

---

## Requirement Traceability (Tasks)

| Requirement | Task(s) |
| ----------- | ------- |
| FIX1-01…04 | T2 |
| FIX1-05 | T1 |
| FIX1-06…08 | T3 |
| FIX1-09…12 | T4 |
| FIX1-13…18 | T5 |
| FIX1-19…21 | T4, T5 |
| FIX1-22…23 | T6 |

**Coverage:** 23 requirements → 6 tasks → 1 Execute batch

---

## Tools Confirmation (before Execute)

For each task, confirm which tools to use:

| Task | Recommended MCPs | Recommended Skills |
| ---- | ---------------- | ------------------ |
| T1 | NONE | NONE |
| T2 | NONE | NONE |
| T3 | NONE | `jpa-performance` |
| T4 | NONE | `spring-boot-new-endpoint` |
| T5 | NONE | `testing-a11y` |
| T6 | NONE | `jpa-performance` |

**Available MCPs (project):** Linear, Context7, SonarQube, sistema-folha API, Docker  
**Available Skills (project):** `tlc-spec-driven`, `testing-a11y`, `spring-boot-new-endpoint`, `jpa-performance`, `spring-security`, `api-client`, …

Reply with adjustments or **approve tasks** to start Execute (`fix1:` commits on `feat/relatorios-executivos`).
