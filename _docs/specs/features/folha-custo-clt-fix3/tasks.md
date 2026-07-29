# folha-custo-clt-fix3 — Rubrica fixa global + UX detalhe Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/folha-custo-clt-fix3/design.md`  
**Status**: Execute complete — T1–T12 done @ `69f4258` → Verifier  
**Approach**: A (loop processamento) + A1 (renderer FE unificado) + B1 (DTO `%` live)  
**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md`, AD-004 (FE Vitest = target, gate atual = lint/build), AD-007/008/010/011/012, `folha-custo-clt-fix3/spec.md` (FIX3 ACs + edge cases).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Cadastros service (fixa global CRUD) | unit (Mockito) | FIX3-02,03,07; edge valor null rubrica não calculada → 400; overlap global vs individual independente; `porcentagem` live no DTO | `backend/src/test/java/**/cadastros/application/FuncionarioRubricaFixaServiceTest.java` | `cd backend && mvn test -Dtest=FuncionarioRubricaFixaServiceTest` |
| Folha processamento (fixa global) | unit (Mockito) | FIX3-04,05,06,08; 2 CLT + global → ambos CUSTO_FIXO; individual vence global; dedup ADP WARN preservado; reprocesso idempotente | `backend/src/test/java/**/folha/application/FolhaProcessamentoServiceTest.java` | `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest` |
| Ficha detalhe consulta (`porcentagem` snapshot) | unit (Mockito) | FIX3-09,10,11; GROSS/NET `contribuicao` sem `%`; snapshot ADP; BENEFICIO `porcentagem` null; `%` null→100 no FE (contrato API) | `backend/src/test/java/**/folha/application/FolhaFichaConsultaServiceTest.java` | `cd backend && mvn test -Dtest=FolhaFichaConsultaServiceTest` |
| Repository / Flyway / entity / DTO record | none | Compile + coberto indiretamente via service tests | `db/migration/V1.24__*.sql`, `cadastros/infrastructure/*`, `cadastros/api/*` | `cd backend && mvn clean compile` |
| Controller 409 body | none | Propagação verificada manualmente ou via service+handler existente; gate compile | `cadastros/api/FuncionarioRubricaFixaController.java` | compile gate |
| Frontend Rubricas Fixas + Folha detalhe | none (AD-004) | FIX3-12…24 via lint/build; ACs manuais nos Independent Tests da spec | `frontend/src/pages/RubricasFixas/`, `frontend/src/pages/FolhaPagamento/` | `cd frontend && npm run lint && npm run build` |
| ArchUnit modular | unit | AD-010 inalterado — sem nova dependência cross-infra | `**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após task com unit tests backend (T4, T6, T7) | `cd backend && mvn test -Dtest=<ClassTest>` |
| Build | Após T1–T3, T5 (schema/repo/DTO/controller sem teste dedicado) | `cd backend && mvn clean compile` |
| FE | Após T9–T11 (frontend) | `cd frontend && npm run lint && npm run build` |
| Full | Após T12 (fechamento feature + Verifier) | `cd backend && mvn test && cd ../frontend && npm run lint && npm run build` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Schema + persistência global (3 tasks)

```
T1 → T2 → T3
```

### Phase 2: Cadastros API global (2 tasks)

```
T4 → T5
```

### Phase 3: Folha backend — processamento + consulta (2 tasks)

```
T6 → T7
```

### Phase 4: Frontend + fechamento (5 tasks)

```
T8 → T9 → T10 → T11 → T12
```

**Batch packing (~7 tasks/worker, whole phases):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | Phase 1 + Phase 2 + Phase 3 | T1–T7 | 7 |
| 2 | Phase 4 | T8–T12 | 5 |

→ **2 workers** sequenciais (offer-then-confirm no Execute). Batch 1 = backend completo; Batch 2 = frontend + docs + full gate.

---

## Task Breakdown

### T1: Flyway V1.24 — `funcionario_id` nullable (fixa global)

**What**: Migração torna `funcionario_id` nullable; índice parcial global; entity JPA `nullable = true`.  
**Where**:
- `backend/src/main/resources/db/migration/V1.24__funcionario_rubrica_fixa_global.sql`
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/domain/FuncionarioRubricaFixa.java`

**Depends on**: None  
**Reuses**: Design DDL V1.24; padrão `V1.19__funcionario_rubrica_fixa.sql`  
**Requirement**: FIX3-01

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`, `jpa-performance`

**Done when**:
- [x] `funcionario_id` nullable com COMMENT documentado
- [x] Índice parcial `idx_funcionario_rubrica_fixa_global_vigencia` criado
- [x] Entity `@JoinColumn(nullable = true)` em `funcionario`
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: build

**Commit**: `feat(cadastros): allow global rubrica fixa (nullable funcionario_id)`

---

### T2: Repository — LEFT JOIN + overlap global

**What**: Queries compatíveis com `funcionario_id` null; método `existsVigenciaSobrepostaGlobal`.  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/cadastros/infrastructure/FuncionarioRubricaFixaRepository.java`

**Depends on**: T1  
**Reuses**: Predicado overlap intervalo de `existsVigenciaSobreposta`  
**Requirement**: FIX3-07 (query support)

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] `findByFiltros` usa `LEFT JOIN FETCH f.funcionario`
- [x] `findVigentesNaCompetencia` usa `LEFT JOIN FETCH f.funcionario`
- [x] `existsVigenciaSobrepostaGlobal(rubricaId, vigenciaInicio, vigenciaFim, excludeId)` filtra `f.funcionario IS NULL AND f.ativo = true`
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none (validado via T4)  
**Gate**: build

**Commit**: `feat(cadastros): repository queries for global rubrica fixa`

---

### T3: DTO + exception + service CRUD global/individual

**What**: `funcionarioId` opcional; `porcentagem` read-only no DTO; service bifurca global vs individual; overlap global 409; `toDTO` null-safe.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/api/FuncionarioRubricaFixaDTO.java`
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/domain/FuncionarioRubricaFixaVigenciaConflictException.java`
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/application/FuncionarioRubricaFixaService.java`

**Depends on**: T2  
**Reuses**: `validarValor`, `validarVigencia`, `isRubricaCalculada`  
**Requirement**: FIX3-02, FIX3-03, FIX3-07; edge valor null (spec Edge Cases)

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `jpa-performance`

**Done when**:
- [x] POST/PUT sem `funcionarioId` persiste `funcionario = null`; DTO retorna `funcionarioNome` null
- [x] POST/PUT com `funcionarioId` mantém fluxo individual
- [x] Overlap global lança `FuncionarioRubricaFixaVigenciaConflictException.forGlobal()`
- [x] Overlap individual lança `.forIndividual()`
- [x] Individual e global **podem coexistir** mesma rubrica (sem 409 cruzado)
- [x] `toDTO` inclui `porcentagem` de `rubrica.porcentagem`
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none (validado via T4)  
**Gate**: build

**Commit**: `feat(cadastros): global rubrica fixa CRUD and DTO porcentagem`

---

### T4: Service unit tests — global CRUD + overlap

**What**: Testes Mockito cobrindo criar global, overlap global 409, individual inalterado, DTO `porcentagem`.  
**Where**: `backend/src/test/java/br/com/techne/sistemafolha/cadastros/application/FuncionarioRubricaFixaServiceTest.java`

**Depends on**: T3  
**Reuses**: Padrão Mockito existente no arquivo  
**Requirement**: FIX3-02, FIX3-03, FIX3-07; edge valor obrigatório

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `criar_global_semFuncionario_persisteFuncionarioNull` — FIX3-02
- [x] `criar_global_vigenciaSobreposta_lanca409Global` — FIX3-07
- [x] `criar_individual_comportamentoInalterado` — FIX3-03
- [x] `criar_global_semValor_rubricaNaoCalculada_lanca400` — edge case
- [x] `toDTO_incluiPorcentagemDaRubrica` — FIX3-14 (contrato API)
- [x] Gate: `cd backend && mvn test -Dtest=FuncionarioRubricaFixaServiceTest`
- [x] Test count: ≥4 novos testes (sem deleções silenciosas)

**Tests**: unit  
**Gate**: quick

**Commit**: `test(cadastros): global rubrica fixa service coverage`

---

### T5: Controller — propagar 409 com message body

**What**: Remover catch local de `FuncionarioRubricaFixaVigenciaConflictException` em POST/PUT; deixar `GlobalExceptionHandler` retornar `ErrorResponse.message` para FE (FIX3-16).  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/cadastros/api/FuncionarioRubricaFixaController.java`

**Depends on**: T3  
**Reuses**: `GlobalExceptionHandler.handleFuncionarioRubricaFixaVigenciaConflictException`  
**Requirement**: FIX3-16 (backend contract)

**Tools**:
- MCP: NONE
- Skill: `spring-security` (ADMIN-only inalterado)

**Done when**:
- [x] POST/PUT conflito vigência retorna HTTP 409 com JSON `{ message: "..." }` distinguível global vs individual
- [x] Demais erros (404, 400) inalterados
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: build

**Commit**: `fix(cadastros): expose vigencia conflict message on 409`

---

### T6: Processamento — aplicar fixas globais + testes

**What**: Particionar vigentes individual/global; aplicar globais após individuais com prioridade ADP > individual > global.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaProcessamentoService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaProcessamentoServiceTest.java`

**Depends on**: T1 (entity nullable — compile); T2 (query vigentes retorna globais)  
**Reuses**: `montarLinhaCustoFixo`, WARN dedup ADP  
**Requirement**: FIX3-04, FIX3-05, FIX3-06, FIX3-08

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] 2 CLT + 1 fixa global vigente → ambas fichas têm linha `CUSTO_FIXO` mesmo valor — FIX3-04
- [x] Funcionário com fixa individual + global mesma rubrica → só individual materializada — FIX3-05
- [x] Rubrica no ADP → fixa global ignorada com WARN — FIX3-06
- [x] Alterar cadastro global não altera ficha até reprocessar — FIX3-08 (teste: processar → assert; simula que consulta lê snapshot)
- [x] Gate: `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest`
- [x] Test count: ≥3 novos testes globais

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(folha): apply global rubrica fixa on processamento`

---

### T7: API detalhe — `porcentagem` snapshot + testes

**What**: Adicionar `porcentagem` a `FichaLinhaDetalheDTO`; popular snapshot em consulta; null para BENEFICIO.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/api/FichaLinhaDetalheDTO.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaFichaConsultaService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaFichaConsultaServiceTest.java`

**Depends on**: None (domínio folha independente de T6 para consulta; pode rodar após T5 em sequência de fase)  
**Reuses**: Testes fix2 GROSS/NET sem `%` em contribuição  
**Requirement**: FIX3-09, FIX3-10, FIX3-11

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`

**Done when**:
- [x] Response linha ficha inclui `porcentagem` snapshot (`BigDecimal`, pode ser null)
- [x] Linha BENEFICIO: `porcentagem` null — FIX3-10
- [x] GROSS/NET: `contribuicao` inalterada (sem `%`); `porcentagem` presente quando snapshot existe — FIX3-11
- [x] COMPANY_COST: `%` aplicado só em contribuição (fix2 preservado)
- [x] Gate: `cd backend && mvn test -Dtest=FolhaFichaConsultaServiceTest`
- [x] Test count: ≥3 novos/atualizados assertions em `porcentagem`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(folha): expose porcentagem snapshot on ficha linha detalhe`

---

### T8: FE types — rubrica fixa opcional + linha detalhe `%`

**What**: Tipos e payloads FE alinhados ao contrato API.  
**Where**:
- `frontend/src/services/funcionarioRubricaFixaService.ts`
- `frontend/src/services/folhaPagamentoService.ts`

**Depends on**: T7 (tipo `porcentagem` na API detalhe); T3 (DTO cadastros)  
**Reuses**: Padrão service existente  
**Requirement**: FIX3-13 (payload), FIX3-09 (tipo consumo)

**Tools**:
- MCP: NONE
- Skill: `api-client`

**Done when**:
- [x] `FuncionarioRubricaFixa.funcionarioId` optional/nullable; `porcentagem?: number | null`
- [x] `criar`/`atualizar` omitem `funcionarioId` quando vazio
- [x] `FichaLinhaDetalhe.porcentagem?: string | number | null`
- [x] Gate: `cd frontend && npm run typecheck` (ou `npm run build` se typecheck indisponível)

**Tests**: none (AD-004)  
**Gate**: build

**Commit**: `feat(frontend): types for global rubrica fixa and linha porcentagem`

---

### T9: Rubricas Fixas — formulário reordenado + funcionário opcional

**What**: Ordem campos spec; select funcionário opcional com helper; submit sem funcionarioId.  
**Where**: `frontend/src/pages/RubricasFixas/index.tsx`

**Depends on**: T8  
**Reuses**: React Hook Form + MUI Select existentes  
**Requirement**: FIX3-12, FIX3-13

**Tools**:
- MCP: NONE
- Skill: `forms-validation`, `component-architecture`

**Done when**:
- [x] Ordem: Rubrica → Valor → Vigência Início → Vigência Fim → Funcionário (opcional) → Comentário
- [x] Opção vazia com helper **“Todos os funcionários (mesmo valor)”**
- [x] Edição de registro global pré-seleciona opção vazia
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none (AD-004)  
**Gate**: FE

**Commit**: `feat(frontend): rubricas fixas form with optional global scope`

---

### T10: Rubricas Fixas — listagem Todos + Percentual + toast 409

**What**: Colunas Funcionário “Todos” e Percentual live; toast distingue 409 global vs individual.  
**Where**: `frontend/src/pages/RubricasFixas/index.tsx`

**Depends on**: T9, T5  
**Reuses**: `getApiErrorMessage`; `%` default 100 quando null  
**Requirement**: FIX3-14, FIX3-15, FIX3-16

**Tools**:
- MCP: NONE
- Skill: `forms-validation`

**Done when**:
- [x] Coluna Funcionário exibe **“Todos”** quando `funcionarioId`/`funcionarioNome` null — FIX3-15
- [x] Coluna Percentual exibe `porcentagem ?? 100` formatado — FIX3-14
- [x] Toast 409 usa `response.data.message` (global vs individual) — FIX3-16
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none (AD-004)  
**Gate**: FE

**Commit**: `feat(frontend): rubricas fixas listagem Todos and percentual`

---

### T11: Folha detalhe — renderer unificado Bruto/Líquido/Custo

**What**: `formatPercentual`, `renderDetalheAgrupado` para 3 abas; colunas, subtotais, total = card.  
**Where**: `frontend/src/pages/FolhaPagamento/index.tsx`

**Depends on**: T8, T7  
**Reuses**: `ORIGEM_LABELS`, `formatMoneyDisplay`, `TOTALIZADORES`  
**Requirement**: FIX3-17, FIX3-18, FIX3-19, FIX3-20, FIX3-21, FIX3-22, FIX3-23, FIX3-24

**Tools**:
- MCP: NONE
- Skill: `component-architecture`

**Done when**:
- [x] Bruto e Líquido agrupam por `origemLinha` como Custo — FIX3-17
- [x] Colunas: Rubrica \| Valor \| Percentual \| Contribuição — FIX3-18
- [x] Percentual: `138,63%` ou **—** (BENEFICIO / ausente); null→100% — FIX3-19
- [x] Subtotal por origem + Total aba — FIX3-20, FIX3-21
- [x] Total exibido = `salBruto` / `salLiquido` / `custoEmpresa` do card — FIX3-22…24
- [x] Empty state: mensagem + total R$ 0,00
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none (AD-004)  
**Gate**: FE

**Commit**: `feat(frontend): unified folha detalhe tabs with percentual and subtotals`

---

### T12: Docs cross-ref + full gate (pré-Verifier)

**What**: Emendar INT-2 em spec pai; atualizar traceability fix3; rodar full gate.  
**Where**:
- `_docs/specs/features/folha-custo-clt/spec.md` (nota fix3 global)
- `_docs/specs/features/folha-custo-clt-fix3/spec.md` (status requirements)
- `_docs/specs/STATE.md` (handoff)

**Depends on**: T1–T11  
**Reuses**: Cross-ref note na spec fix3  
**Requirement**: Spec cross-ref; Success Criteria gate

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Nota INT-2 fixa global adicionada em `folha-custo-clt/spec.md`
- [x] FIX3-01…24 marcados Done em traceability (ou Pending→Done conforme entrega)
- [x] Gate: `cd backend && mvn test && cd ../frontend && npm run lint && npm run build`
- [x] Handoff STATE.md atualizado → pronto para Verifier

**Tests**: none (full gate runs all)  
**Gate**: full

**Commit**: `docs(specs): folha-custo-clt-fix3 cross-ref and traceability`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 ──→ T2 ──→ T3
Phase 2:  T4 ──→ T5
Phase 3:  T6 ──→ T7
Phase 4:  T8 ──→ T9 ──→ T10 ──→ T11 ──→ T12
```

Execution is strictly sequential — one task at a time, in order.

**Batch workers (Execute):**

| Worker | Tasks | Scope |
| ------ | ----- | ----- |
| 1 | T1–T7 | Backend completo |
| 2 | T8–T12 | Frontend + docs + full gate → Verifier |

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Flyway + entity | 1 migration + 1 entity | ✅ Granular |
| T2: Repository queries | 1 repository file | ✅ Granular |
| T3: DTO + exception + service | 3 arquivos coesos (1 feature vertical cadastros) | ✅ OK (cohesive) |
| T4: Service tests | 1 test file | ✅ Granular |
| T5: Controller 409 body | 1 controller file | ✅ Granular |
| T6: Processamento + tests | 1 service + 1 test file | ✅ OK (cohesive) |
| T7: Consulta detalhe + tests | DTO + service + tests | ✅ OK (cohesive) |
| T8: FE types | 2 service files | ✅ OK (types only) |
| T9: Form Rubricas Fixas | 1 page (form section) | ✅ Granular |
| T10: Listagem Rubricas Fixas | 1 page (list section) | ✅ Granular |
| T11: Folha detalhe renderer | 1 page | ✅ Granular |
| T12: Docs + full gate | docs only | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | Entry | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T3 | T3 → T5 | ✅ Match |
| T6 | T1, T2 | Phase 3 after Phase 1 | ✅ Match |
| T7 | None (phase seq after T6) | T6 → T7 | ✅ Match |
| T8 | T7, T3 | Phase 4 after Phase 3 | ✅ Match |
| T9 | T8 | T8 → T9 | ✅ Match |
| T10 | T9, T5 | T9 → T10 | ✅ Match |
| T11 | T8, T7 | T8 → T11 | ✅ Match |
| T12 | T1–T11 | T11 → T12 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Flyway / entity | none | none | ✅ OK |
| T2 | Repository | none | none | ✅ OK |
| T3 | Cadastros service | unit | none (T4) | ✅ OK — tests in T4 |
| T4 | Cadastros service tests | unit | unit | ✅ OK |
| T5 | Controller | none | none | ✅ OK |
| T6 | Folha processamento service | unit | unit | ✅ OK |
| T7 | Ficha consulta service | unit | unit | ✅ OK |
| T8 | FE services/types | none | none | ✅ OK |
| T9 | FE page | none | none | ✅ OK |
| T10 | FE page | none | none | ✅ OK |
| T11 | FE page | none | none | ✅ OK |
| T12 | Docs | none | none (full gate) | ✅ OK |

---

## Requirement Traceability (Tasks)

| Requirement | Task(s) |
| ----------- | ------- |
| FIX3-01 | T1 |
| FIX3-02 | T3, T4 |
| FIX3-03 | T3, T4 |
| FIX3-04 | T6 |
| FIX3-05 | T6 |
| FIX3-06 | T6 |
| FIX3-07 | T3, T4 |
| FIX3-08 | T6 |
| FIX3-09 | T7, T8 |
| FIX3-10 | T7 |
| FIX3-11 | T7 |
| FIX3-12 | T9 |
| FIX3-13 | T8, T9 |
| FIX3-14 | T3, T10 |
| FIX3-15 | T10 |
| FIX3-16 | T5, T10 |
| FIX3-17 | T11 |
| FIX3-18 | T11 |
| FIX3-19 | T11 |
| FIX3-20 | T11 |
| FIX3-21 | T11 |
| FIX3-22 | T11 |
| FIX3-23 | T11 |
| FIX3-24 | T11 |

**Coverage:** 24/24 requirements mapped to tasks.

---

## MCPs & Skills (for Execute)

| Task | Suggested MCP | Suggested Skills |
| ---- | ------------- | ---------------- |
| T1 | NONE | `flyway-migration-writer`, `jpa-performance` |
| T2 | NONE | `jpa-performance` |
| T3 | NONE | `spring-boot-new-endpoint` |
| T4–T7 | NONE | — |
| T5 | NONE | `spring-security` (verify ADMIN unchanged) |
| T8 | NONE | `api-client` |
| T9–T11 | NONE | `forms-validation`, `component-architecture` |
| T12 | NONE | — |
| Verifier | NONE | `tlc-spec-driven` validate flow |

**Available MCPs (project):** Linear, Context7, SonarQube, MCP Docker  
**Available Skills:** ver `.agents/skills/` e `AGENTS.md`
