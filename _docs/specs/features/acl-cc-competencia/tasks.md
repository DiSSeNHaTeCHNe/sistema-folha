# ACL — Centro de Custo por Competência Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/acl-cc-competencia/design.md`  
**Spec**: `_docs/specs/features/acl-cc-competencia/spec.md`  
**Status**: Execute round 3 complete — T12 @ `7e0421d`; Verifier PASS 28/28 ACs

**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `acl-cc-competencia/spec.md` (FCC ACs), AD-007/008/010/011, skills `jpa-performance`, `spring-security`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Shared helper (`CentroCustoEfetivo`) | unit (JUnit 5) | All branches: null/null, linha only, fallback funcionário, `pertenceAoEscopo` true/false/null id/empty set | `backend/src/test/java/**/shared/access/CentroCustoEfetivoTest.java` | `cd backend && mvn test -Dtest=CentroCustoEfetivoTest` |
| Folha application ACL + port | unit (Mockito) | FCC-01…06, 07…09, 12: discriminatório linha CC ≠ funcionário CC atual; acessoTotal bypass; fallback linha null | `backend/src/test/java/**/folha/application/FolhaPagamentoServiceTest.java`, `FolhaConsultaAdapterTest.java`, `FolhaTotalizacaoServiceTest.java` | `cd backend && mvn test -Dtest=FolhaPagamentoServiceTest,FolhaConsultaAdapterTest,FolhaTotalizacaoServiceTest` |
| Folha repository + endpoint CC | unit (Mockito) | FCC-10, 11: query usa `f.centroCusto`; deny sem permissão organograma | `backend/src/test/java/**/folha/application/FolhaPagamentoServiceTest.java` | `cd backend && mvn test -Dtest=FolhaPagamentoServiceTest` |
| Benefício entity + Flyway | none | Build/migration gate only | `V1.25__*.sql`, `BeneficioMensal.java` | `cd backend && mvn test` (compile) |
| Benefício repository JPQL | unit (Mockito via service) | FCC-14: queries scoped usam COALESCE — verificado via service tests + adapter | `backend/src/test/java/**/beneficios/application/BeneficioMensalServiceTest.java` | `cd backend && mvn test -Dtest=BeneficioMensalServiceTest` |
| Benefício application ACL + create | unit (Mockito) | FCC-13…15, 17: snapshot no create; read ACL usa linha; create gate usa funcionário atual; DTO CC efetivo | `backend/src/test/java/**/beneficios/application/BeneficioMensalServiceTest.java` | `cd backend && mvn test -Dtest=BeneficioMensalServiceTest` |
| Benefício consulta port | unit (Mockito) | FCC-16: discriminatório equivalente FCC-06; contagem scoped por CC efetivo | `backend/src/test/java/**/beneficios/application/BeneficioConsultaAdapterTest.java` | `cd backend && mvn test -Dtest=BeneficioConsultaAdapterTest` |
| Controllers / FE | none | Thin controllers; zero FE change | — | — |
| ArchUnit regression | unit | Zero violação AD-010 pós helper em `shared` | `**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após T1 ou task folha/benefício isolada | `cd backend && mvn test -Dtest=<ClassTest>` |
| Full | Após T7 (fechamento feature) | `cd backend && mvn test` |
| Build | Após T4 (migration + entity) | `cd backend && mvn clean package` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

**Batch sizing:** 7 tasks → **1 batch**, Execute inline (sem sub-agents).

### Phase 1: Foundation

```
T1
```

### Phase 2: Folha — ACL + port + endpoint

```
T2 → T3
```

### Phase 3: Benefícios — schema + queries + ACL

```
T4 → T5 → T6 → T7
```

---

## Task Breakdown

### T1: Helper `CentroCustoEfetivo` + unit test

**What**: Criar utilitário puro com `idOf(linhaId, funcionarioId)` e `pertenceAoEscopo(effectiveId, centros)`; testes cobrindo null, fallback e membership.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/shared/access/CentroCustoEfetivo.java`
- `backend/src/test/java/br/com/techne/sistemafolha/shared/access/CentroCustoEfetivoTest.java`

**Depends on**: None  
**Reuses**: Padrão `shared/logging/DomainLogging` (final class, static methods)  
**Requirement**: Base para FCC-01, FCC-05, FCC-14

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `idOf` retorna linha quando presente; fallback funcionário; null se ambos null
- [x] `pertenceAoEscopo` false para id null, set null/empty, ou id fora do set
- [x] Gate: `cd backend && mvn test -Dtest=CentroCustoEfetivoTest` — all pass

**Commit:** `89f62ac`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(acl-cc): add CentroCustoEfetivo helper for scoped ACL`

---

### T2: Folha ACL in-memory + port snapshots + totalização

**What**: Refatorar `FolhaPagamentoService.aplicarFiltroAcesso`, `FolhaConsultaAdapter.pertenceAosCentros` e `toLinhaSnapshot`, `FolhaTotalizacaoService` CC display — todos via `CentroCustoEfetivo`; adicionar teste discriminatório FCC-06 (linha CC-A, funcionário CC-B atual → gestor A vê, B não).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaPagamentoService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaConsultaAdapter.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaTotalizacaoService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaPagamentoServiceTest.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaConsultaAdapterTest.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaTotalizacaoServiceTest.java` (se aplicável)

**Depends on**: T1  
**Reuses**: `OrganogramaAcessoPort`, `FolhaConsultaPort` contract inalterado  
**Requirement**: FCC-01, FCC-02, FCC-03, FCC-04, FCC-05, FCC-06, FCC-07, FCC-08, FCC-09, FCC-12

**Tools**:
- MCP: NONE
- Skill: `jpa-performance` (leitura — sem N+1 extra)

**Done when**:
- [x] ACL folha usa CC efetivo da linha (não só funcionário)
- [x] `toLinhaSnapshot` expõe CC da linha quando presente
- [x] Teste FCC-06 falha se reverter para `funcionario.getCentroCusto()`
- [x] Gate: `cd backend && mvn test -Dtest=FolhaPagamentoServiceTest,FolhaConsultaAdapterTest,FolhaTotalizacaoServiceTest`

**Commit:** `d5ee995`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(acl-cc): folha ACL and snapshots use effective cost center`

---

### T3: Folha endpoint/repository por CC da linha

**What**: Trocar `consultarPorCentroCusto` para query `f.centroCusto` no período; adicionar/ajustar `FolhaPagamentoRepository.findByCentroCustoAndDataInicioBetweenAndAtivoTrue`; testes FCC-10/FCC-11.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/infrastructure/FolhaPagamentoRepository.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaPagamentoService.java` (método `consultarPorCentroCusto`)
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaPagamentoServiceTest.java`

**Depends on**: T2  
**Reuses**: `usuarioPodeAcessarCentroCusto` inalterado  
**Requirement**: FCC-10, FCC-11

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Repository mock recebe chamada por CC da **linha**, não funcionário
- [x] Sem permissão organograma → lista vazia (regressão)
- [x] Gate: `cd backend && mvn test -Dtest=FolhaPagamentoServiceTest`

**Commit:** `f910f4c`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(acl-cc): query folha by line cost center in period`

---

### T4: Migration V1.25 + entity `BeneficioMensal.centroCusto`

**What**: Flyway `V1.25__beneficio_mensal_centro_custo.sql` (coluna nullable + FK + índice); `@ManyToOne CentroCusto centroCusto` nullable em `BeneficioMensal`.  
**Where**:
- `backend/src/main/resources/db/migration/V1.25__beneficio_mensal_centro_custo.sql`
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/domain/BeneficioMensal.java`

**Depends on**: None (paralelo conceitual a Phase 2, mas sequencial após T3 por plano)  
**Reuses**: Skill `flyway-migration-writer` conventions (V1.24 → V1.25)  
**Requirement**: FCC-13 (persistência)

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`

**Done when**:
- [x] Migration idempotente (`IF NOT EXISTS`)
- [x] Entity compila com campo nullable
- [x] Gate: `cd backend && mvn clean package` (compile + test suite existente verde)

**Commit:** `3244c73`

**Tests**: none  
**Gate**: build

**Commit**: `feat(acl-cc): add centro_custo_id to beneficio_mensal`

---

### T5: Benefício repository — queries scoped com COALESCE

**What**: Atualizar JPQL em `BeneficioMensalRepository` — substituir `bm.funcionario.centroCusto.id IN :ids` por `COALESCE(bm.centroCusto.id, bm.funcionario.centroCusto.id) IN :centroCustoIds` em todos os métodos scoped; renomear método derived se necessário.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/infrastructure/BeneficioMensalRepository.java`

**Depends on**: T4  
**Reuses**: Queries existentes como template  
**Requirement**: FCC-14

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] 4 queries scoped atualizadas (list, resumo, competencias, sumValor)
- [x] Compilação OK
- [x] Gate: `cd backend && mvn test` (suite completa — sem testes novos nesta task, compile gate)

**Commit:** `352aadd`

**Tests**: none (queries verificadas em T6/T7 via service/adapter tests)  
**Gate**: build

**Commit**: `feat(acl-cc): beneficio repository scoped queries use effective CC`

---

### T6: Benefício service — snapshot create + ACL read + DTO

**What**: Em `criar`, setar `beneficio.centroCusto` do funcionário; refatorar `aplicarFiltroAcesso(BeneficioMensal)` e `toDTO` para CC efetivo; manter gate create em `aplicarFiltroAcesso(Funcionario)`; testes FCC-13…15, 17.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/application/BeneficioMensalService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/beneficios/application/BeneficioMensalServiceTest.java`

**Depends on**: T1, T4, T5  
**Reuses**: `centrosParaFiltro`, deny pattern existente  
**Requirement**: FCC-13, FCC-14, FCC-15, FCC-17

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [x] Create persiste `centro_custo_id` snapshot
- [x] Read ACL usa CC da linha após transferência de funcionário
- [x] DTO `centroCustoId` reflete linha snapshotada
- [x] Gate: `cd backend && mvn test -Dtest=BeneficioMensalServiceTest`

**Commit:** `4e54f56`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(acl-cc): beneficio service ACL and snapshot on create`

---

### T7: Benefício consulta port + teste discriminatório FCC-16

**What**: Corrigir `BeneficioConsultaAdapter.contarLancamentosAtivosNaCompetenciaPorCentros` (e paths relacionados) para CC efetivo; teste FCC-16 equivalente a FCC-06.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/application/BeneficioConsultaAdapter.java`
- `backend/src/test/java/br/com/techne/sistemafolha/beneficios/application/BeneficioConsultaAdapterTest.java`

**Depends on**: T1, T6  
**Reuses**: Repository queries de T5  
**Requirement**: FCC-16, FCC-14 (port)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Contagem scoped usa CC efetivo (linha ≠ funcionário atual)
- [x] Teste discriminatório FCC-16 presente
- [x] Gate: `cd backend && mvn test` (full suite)
- [x] ArchUnit: `cd backend && mvn test -Dtest=ModularArchitectureTest`

**Commit:** `f72297f`

**Tests**: unit  
**Gate**: full

**Commit**: `feat(acl-cc): beneficio consulta port uses effective cost center`

**Post-T7:** Verifier automático → `validation.md`

---

## Fix cycle 1 (2026-07-29)

**Commit:** `e65a395` — `fix(cycle-1): add ACL test coverage for FCC-02 FCC-03 FCC-14`

**Tests added:**
- `FolhaPagamentoServiceTest.consultarPorPeriodo_duasCompetenciasCcDistintos_gestorVeSoCompetenciaDoEscopo_fcc02_fcc03`
- `FolhaConsultaAdapterTest.findLinhasAtivasPorCompetencia_duasCompetenciasCcDistintos_gestorVeSoCompetenciaDoEscopo_fcc02_fcc03`
- `BeneficioMensalServiceTest.listarPorCompetenciaParaUsuario_snapshotCcDiferenteDoFuncionarioAtual_filtraPorCcDaLinha_fcc14`

**Verifier re-run:** PASS ✅ (17/17 ACs, 296 tests)

---

## Round 2 — Execution Plan

**Batch sizing:** 4 tasks → **1 batch**.

### Phase 4: Ficha — snapshot + ACL path

```
T8 → T9
```

### Phase 5: Hygiene + perf

```
T10 → T11
```

---

### T8: Migration V1.26 + entity `FichaMensal.centroCusto` + snapshot no processamento

**What**: Flyway `V1.26__ficha_mensal_centro_custo.sql`; `@ManyToOne CentroCusto centroCusto` nullable em `FichaMensal`; `FolhaProcessamentoService.montarFicha` seta CC efetivo (COALESCE linha ADP → funcionário) do grupo; teste processamento persiste snapshot.

**Where**:
- `backend/src/main/resources/db/migration/V1.26__ficha_mensal_centro_custo.sql`
- `backend/src/main/java/.../folha/domain/FichaMensal.java`
- `backend/src/main/java/.../folha/application/FolhaProcessamentoService.java`
- `backend/src/test/java/.../folha/application/FolhaProcessamentoServiceTest.java`

**Depends on**: T1 (CentroCustoEfetivo — já committed)  
**Requirement**: FCC-18, FCC-21

**Done when**:
- [x] Migration idempotente; entity compila
- [x] `montarFicha` persiste `centro_custo_id` from effective CC of grupo
- [x] Reprocess test confirms snapshot updated
- [x] Gate: `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest`

**Commit:** `0573799`

---

### T9: Ficha ACL path — repository + adapter + teste FCC-22

**What**: `FichaLinhaRepository` filter COALESCE(ficha, funcionário); `toLinhaSnapshotFromFicha` uses ficha CC; discriminating test path ficha (snapshot CC-A, func CC-B).

**Where**:
- `backend/src/main/java/.../folha/infrastructure/FichaLinhaRepository.java`
- `backend/src/main/java/.../folha/application/FolhaConsultaAdapter.java`
- `backend/src/test/java/.../folha/application/FolhaConsultaAdapterTest.java`

**Depends on**: T8  
**Requirement**: FCC-19, FCC-20, FCC-22 (extends FCC-07/08)

**Done when**:
- [x] Scoped ficha path filters by effective CC
- [x] Snapshot exposes ficha CC when present
- [x] Test FCC-22 kills mutation reverting to funcionario-only filter
- [x] Gate: `cd backend && mvn test -Dtest=FolhaConsultaAdapterTest,FolhaProcessamentoServiceTest`

**Commit:** `3cef3ec`

---

### T10: Benefício COUNT scoped em SQL (FCC-23)

**What**: Add `countByCompetenciaECentros` to `BeneficioMensalRepository`; refactor `BeneficioConsultaAdapter.contarLancamentosAtivosNaCompetenciaPorCentros` to use it; test verifies no full-list fetch.

**Where**:
- `backend/src/main/java/.../beneficios/infrastructure/BeneficioMensalRepository.java`
- `backend/src/main/java/.../beneficios/application/BeneficioConsultaAdapter.java`
- `backend/src/test/java/.../beneficios/application/BeneficioConsultaAdapterTest.java`

**Depends on**: T7  
**Requirement**: FCC-23

**Done when**:
- [x] Count uses SQL COALESCE predicate
- [x] Test mocks verify `countByCompetenciaECentros`, never unscoped full fetch for scoped count
- [x] Gate: `cd backend && mvn test -Dtest=BeneficioConsultaAdapterTest`

**Commit:** `96811e9`

---

### T11: Repository hygiene — rename benefício + remove folha orphan (FCC-24, FCC-25)

**What**: Rename `findByCompetenciaInicioAndCompetenciaFimAndFuncionarioCentroCustoIdInAndAtivoTrue` → `findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue`; update service + tests; remove `findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue` from `FolhaPagamentoRepository`.

**Where**:
- `backend/src/main/java/.../beneficios/infrastructure/BeneficioMensalRepository.java`
- `backend/src/main/java/.../beneficios/application/BeneficioMensalService.java`
- `backend/src/test/java/.../beneficios/application/BeneficioMensalServiceTest.java`
- `backend/src/main/java/.../folha/infrastructure/FolhaPagamentoRepository.java`
- `backend/src/test/java/.../folha/application/FolhaPagamentoServiceTest.java`

**Depends on**: T10  
**Requirement**: FCC-24, FCC-25

**Done when**:
- [x] All references updated; compiles
- [x] Orphan folha method removed
- [x] Gate: `cd backend && mvn test`

**Commit:** `12804f9`

**Post-T11:** Verifier automático round 2

---

## Requirement Traceability (Tasks) — Round 2

| Requirement ID | Task(s) |
| -------------- | ------- |
| FCC-18, FCC-21 | T8 |
| FCC-19, FCC-20, FCC-22 | T9 |
| FCC-23 | T10 |
| FCC-24, FCC-25 | T11 |

**Coverage:** 8 new requirements → 4 tasks, 1 batch

---

## Round 3 — Execution Plan

**Batch sizing:** 1 task → **1 batch**.

### Phase 6: Ficha drill-down ACL

```
T12
```

---

### T12: `FolhaFichaConsultaService` ACL + repository fetch + testes FCC-26…28

**What**: Refatorar `podeAcessarFicha` via `CentroCustoEfetivo`; fetch `ficha.centroCusto` no repository; teste discriminatório snapshot CC-A / func CC-B; teste `buscarFichaIdPorFuncionario` paridade.

**Where**:
- `backend/src/main/java/.../folha/application/FolhaFichaConsultaService.java`
- `backend/src/main/java/.../folha/infrastructure/FichaMensalRepository.java`
- `backend/src/test/java/.../folha/application/FolhaFichaConsultaServiceTest.java`

**Depends on**: T8, T9  
**Requirement**: FCC-26, FCC-27, FCC-28

**Done when**:
- [x] `podeAcessarFicha` usa CC efetivo ficha→funcionário
- [x] Gestor CC-A acessa ficha snapshot A / func B; gestor CC-B 404
- [x] `buscarFichaIdPorFuncionario` mesma regra
- [x] Gate: full suite pass

**Commit:** `7e0421d`

**Post-T12:** Verifier round 3

---

## Requirement Traceability (Tasks) — Round 3

| Requirement ID | Task(s) |
| -------------- | ------- |
| FCC-26, FCC-27, FCC-28 | T12 |

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3

Phase 1:  T1
Phase 2:  T2 ──→ T3
Phase 3:  T4 ──→ T5 ──→ T6 ──→ T7
```

Execution is strictly sequential — 1 batch, inline, no sub-agents.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: CentroCustoEfetivo helper | 1 class + 1 test class | ✅ Granular |
| T2: Folha ACL + port + totalização | 3 services + tests (cohesive ACL slice) | ✅ Granular |
| T3: Folha repository endpoint CC | 1 repository method + 1 service method + tests | ✅ Granular |
| T4: Migration + entity | 2 files schema | ✅ Granular |
| T5: Benefício repository JPQL | 1 repository file | ✅ Granular |
| T6: Benefício service ACL | 1 service + tests | ✅ Granular |
| T7: Benefício consulta adapter | 1 adapter + tests | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | Phase 1 root | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | None (phase 3 start) | T4 after T3 phase boundary | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T1, T4, T5 | T5 → T6 (T1/T4 implicit prerequisites) | ✅ Match |
| T7 | T1, T6 | T6 → T7 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Shared helper | unit | unit | ✅ OK |
| T2 | Folha application | unit | unit | ✅ OK |
| T3 | Folha application + repository | unit | unit | ✅ OK |
| T4 | Entity + Flyway | none | none | ✅ OK |
| T5 | Repository JPQL | verified via T6/T7 | none | ✅ OK (repository tested through service layer per project pattern) |
| T6 | Benefício application | unit | unit | ✅ OK |
| T7 | Benefício port adapter | unit | unit | ✅ OK |

---

## Requirement Traceability (Tasks)

| Requirement ID | Task(s) |
| -------------- | ------- |
| FCC-01…06 | T1, T2 |
| FCC-07…09 | T2 |
| FCC-10, 11 | T3 |
| FCC-12 | T2 |
| FCC-13 | T4, T6 |
| FCC-14 | T5, T6, T7 |
| FCC-15 | T6 |
| FCC-16 | T7 |
| FCC-17 | T6 |

**Coverage:** 17 requirements → 7 tasks, 1 batch

---

## Tools & Skills (Execute)

| Task | Recommended Skills |
| ---- | ------------------ |
| T1 | — |
| T2–T3 | `jpa-performance`, `spring-security` |
| T4 | `flyway-migration-writer` |
| T5–T7 | `jpa-performance`, `spring-security` |

**MCPs:** none required (codebase-local).

**Sub-agents:** not offered — 7 tasks ≤ 8, single batch inline.
