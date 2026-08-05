# Workspace do Usuário — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/workspace-usuario/design.md`  
**Spec**: `_docs/specs/features/workspace-usuario/spec.md`  
**Status**: P1 complete — Batch 5 done (T1–T30); ready for P2 Batch 6 (T31–T39)

### Batch 5 complete (2026-08-05)
| Task | Commit | Gate |
| ---- | ------ | ---- |
| T29 | `e4cacb4` | E2E workspace ✅ |
| T30 | `40b8561` | release ✅ |

### Batch 4 complete (2026-08-04)
| Task | Commit | Gate |
| ---- | ------ | ---- |
| T20 | `9798844` | quick FE ✅ |
| T21 | `1216226` | quick FE ✅ |
| T22 | `5bdc0ad` | quick FE ✅ |
| T23 | `a2f58b5` | quick FE ✅ |
| T24 | `37994f2` | quick FE ✅ |
| T25 | `97201e2` | quick FE ✅ |
| T26 | `ff47c30` | quick FE ✅ |
| T27 | `0d2d4a5` | quick FE ✅ |
| T28 | `4635036` | quick FE ✅ |

### Batch 3 complete (2026-08-04)
| Task | Commit | Gate |
| ---- | ------ | ---- |
| T12 | `e8cf19d` | LayoutValidator ✅ |
| T13 | `fb4b9a8` | quick BE ✅ |
| T14 | `76ae8d0` | full API ✅ |
| T15 | `967d51b` | quick BE + ArchUnit ✅ |
| T16 | `0c7b826` | full API ✅ |
| T17 | `bd3b767` | quick BE ✅ |
| T18 | `98c40f4` | full API ✅ |
| T19 | `2571d91` | ArchUnit ✅ |

### Batch 2 complete (2026-08-04)
| Task | Commit | Gate |
| ---- | ------ | ---- |
| T7 | `43e196e` | quick BE ✅ |
| T8 | `7ff5f16` | quick BE ✅ |
| T9 | `f4af70d` | DatasetControllerWebMvcTest ✅ |
| T10 | `a7ec5fc` | quick BE ✅ |
| T11 | `84079fb` | full API ✅ |

### Batch 1 complete (2026-08-04)
| Task | Commit | Gate |
| ---- | ------ | ---- |
| T1 | `47639c4` | ModularArchitectureTest ✅ |
| T2 | `7fa3509` | WorkspaceAccessGuardTest ✅ |
| T3 | `a7bac50` | quick BE ✅ |
| T4 | `ab8b360` | quick BE ✅ |
| T5 | `cba8fea`/`e9e7a32` | quick BE ✅ (duplicate parse commit — noted) |
| T6 | `237c45f` | quick BE ✅ |

### Batch 1 complete (T1–T6)
| Task | Commit | Status |
| ---- | ------ | ------ |
| T1 | `47639c4` | ✅ |
| T2 | `7fa3509` | ✅ |
| T3 | `a7bac50` | ✅ |
| T4 | `ab8b360` | ✅ |
| T5 | `cba8fea` | ✅ |
| T6 | `237c45f` | ✅ |  
**Constraints**: AD-008, AD-010, AD-011, AD-013, AD-017, AD-018 (AST whitelist)  
**Pré-requisito Execute**: Nível 1 (`dashboard-customizavel`) merged em `main`; gate em T1 verifica `DashboardLayout` estável.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md`, `.agents/skills/testing-a11y/SKILL.md`, `.agents/skills/jpa-performance/SKILL.md`, `.agents/skills/spring-security/SKILL.md`, `workspace-usuario/spec.md` (WKS-01…32 ACs + edge cases).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Workspace application (`WorkspaceAccessGuard`, `DatasetService`, `DatasetRowService`, `FormulaEngine`, `WidgetDefinitionService`, `WorkspaceService`, `WidgetQueryService`, `TemplatePublishService`, `TemplateInstallService`, `DatasetAuditService`, `WorkspaceProposalService`, `OrcamentoTemplateInstaller`) | unit (Mockito) | All branches; 1:1 to WKS ACs; all spec edge cases; FormulaEngine mutation-safe (whitelist rejects SpEL-like input) | `backend/src/test/java/**/workspace/application/*Test.java` | `cd backend && mvn test -Dtest='workspace.application.*Test'` |
| Workspace API (all `*Controller`) | WebMvcTest | All routes: happy + ACL 403 + validation 400/409 + quota + permission WKS-25; API Key write guard on proposal paths | `backend/src/test/java/**/workspace/api/*WebMvcTest.java` | `cd backend && mvn test -Dtest='workspace.api.*WebMvcTest'` |
| Workspace domain/infrastructure (entities JSONB, Flyway, adapters) | none / unit for validators | JSONB round-trip in service tests; `DatasetRowValidator` all field types; migration via full suite | `workspace/domain/`, `db/migration/V1.3*` | compile gate |
| Security (`ApiKeyWriteGuardFilter` extension) | unit + WebMvcTest | JWT full write; API Key readonly blocked; API Key workspace only `/workspace/proposals/**` | `backend/src/test/java/**/security/*Test.java`, `workspace/api/ProposalControllerWebMvcTest.java` | `cd backend && mvn test -Dtest=ApiKeyWriteGuardFilterTest,ProposalControllerWebMvcTest` |
| ArchUnit workspace boundaries | unit | New domain obeys AD-010; no foreign infrastructure | `backend/src/test/java/**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Frontend hooks/pages (`pages/Workspace/**`) | unit (Vitest + Testing Library) | By role/label; WKS-02/04 UI feedback; workspace switcher WKS-11; formula editor errors; dynamic widget render | `frontend/src/pages/Workspace/**/*.test.tsx` | `cd frontend && npm test -- src/pages/Workspace` |
| Frontend utils (`workspaceAccess.ts`) | unit | ACL mirror BE — 403 gate | `frontend/src/utils/workspaceAccess.test.ts` | `cd frontend && npm test -- workspaceAccess` |
| Frontend E2E (Workspace P1) | Playwright | Create dataset → row → widget → workspace layout → orçamento install smoke | `frontend/e2e/workspace.spec.ts` | `cd frontend && npm run test:e2e -- e2e/workspace.spec.ts` |
| MCP whitelist | shell script | New tools in `api-to-mcp.yml` match OpenAPI operationIds | `diversos/openapi/validate-mcp-whitelist.sh` | `bash diversos/openapi/validate-mcp-whitelist.sh` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick BE | After application unit tasks | `cd backend && mvn test -Dtest='workspace.application.*Test'` |
| Quick FE | After frontend unit tasks | `cd frontend && npm test -- src/pages/Workspace src/utils/workspaceAccess` |
| Full API | After WebMvcTest tasks | `cd backend && mvn test -Dtest='workspace.api.*WebMvcTest'` |
| Security | After T43–T44 (P3) | `cd backend && mvn test -Dtest=ApiKeyWriteGuardFilterTest,ProposalControllerWebMvcTest,ModularArchitectureTest` |
| Build | After migration/entity-only tasks | `cd backend && mvn test -Dtest=ModularArchitectureTest && cd ../frontend && npm run build` |
| Release | After T30, T40, T48 (phase gates) | `cd backend && mvn test && cd ../frontend && npm run lint && npm test && npm run build` |
| MCP | After T46 | `bash diversos/openapi/validate-mcp-whitelist.sh` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Backend — Domain Foundation (P1)

```
T1 → T2 → T3 → T4
```

### Phase 2: Backend — Formula Engine (P1)

```
T5 → T6
```

### Phase 3: Backend — Dataset API (P1)

```
T7 → T8 → T9
```

### Phase 4: Backend — Widget Definition API (P1)

```
T10 → T11
```

### Phase 5: Backend — Workspace + Query (P1)

```
T12 → T13 → T14 → T15 → T16
```

### Phase 6: Backend — Orçamento Template + ArchUnit (P1)

```
T17 → T18 → T19
```

### Phase 7: Frontend — Shell + Dataset Editor (P1)

```
T20 → T21 → T22 → T23 → T24
```

### Phase 8: Frontend — Widgets + Grid (P1)

```
T25 → T26 → T27 → T28
```

### Phase 9: P1 — QA Gate

```
T29 → T30
```

### Phase 10: Backend — Templates + Audit (P2)

```
T31 → T32 → T33 → T34 → T35 → T36
```

### Phase 11: Frontend — Marketplace (P2)

```
T37 → T38
```

### Phase 12: P2 — QA Gate

```
T39
```

### Phase 13: Backend — IA Proposals + Security (P3)

```
T40 → T41 → T42 → T43 → T44
```

### Phase 14: MCP + Frontend IA (P3)

```
T45 → T46 → T47
```

### Phase 15: P3 — QA Gate

```
T48
```

**Batch packing (Execute):** 48 tasks → **7 batches** (~6–7 tasks, whole phases):

| Batch | Phases | Tasks | Focus |
| ----- | ------ | ----- | ----- |
| 1 | 1–2 | T1–T6 | BE foundation + FormulaEngine |
| 2 | 3–4 | T7–T11 | Dataset + WidgetDefinition API |
| 3 | 5–6 | T12–T19 | Workspace/Query + Orçamento + ArchUnit |
| 4 | 7–8 | T20–T28 | FE shell, dataset, widgets, grid |
| 5 | 9 | T29–T30 | P1 QA + release gate |
| 6 | 10–12 | T31–T39 | P2 marketplace + audit |
| 7 | 13–15 | T40–T48 | P3 IA/MCP + security |

Batches run **sequentially**. Offer sub-agents at Execute start; default inline if user declines.

---

## Requirement Traceability (Tasks → WKS)

| Task(s) | WKS |
| ------- | --- |
| T7–T9 | WKS-01, WKS-02, WKS-03, WKS-04 |
| T5–T6, T10–T11 | WKS-05, WKS-06, WKS-07 |
| T15–T16, T26–T27 | WKS-08, WKS-09 |
| T13–T14, T22–T23, T28 | WKS-10, WKS-11, WKS-12 |
| T17–T18, T29 | WKS-13, WKS-14 |
| T32–T36, T37–T38 | WKS-15…WKS-21 |
| T34, T36 | WKS-22, WKS-23 |
| T40–T47 | WKS-24…WKS-32 |
| T2, T9, T11, T14, T16 (403 cases) | Edge: sem organograma |
| T6, T10 (invalid formula) | Edge: fórmula inválida, campo removido |
| T32 (hash) | Edge: publish idempotente |
| T43–T44 | Edge: IA sem permissão; API Key write scope |

**Coverage:** 32/32 WKS mapped.

---

## Task Breakdown

### T1: Flyway V1.30 + JPA entities (workspace foundation)

**What**: Migration `V1.30__workspace_foundation.sql` + entities `Workspace`, `WorkspaceDataset`, `WorkspaceDatasetRow`, `WorkspaceWidgetDefinition` with JSONB mappings (AD-017).
**Where**: `backend/src/main/resources/db/migration/`, `backend/.../workspace/domain/`, `backend/.../workspace/infrastructure/*Repository.java`
**Depends on**: Nível 1 merged (`dashboard_layout` exists)
**Reuses**: `DashboardLayout.java` JSONB pattern, `V1.29` migration style
**Requirement**: WKS-01 (persistence foundation)

**Tools**:
- Skill: `flyway-migration-writer`, `jpa-performance`

**Done when**:
- [ ] Tables match design.md schema (workspace, workspace_dataset, workspace_dataset_row, workspace_widget_definition)
- [ ] `@JdbcTypeCode(SqlTypes.JSON)` on schema, valores, fontes, config, widgets
- [ ] Repositories compile; `mvn test -Dtest=ModularArchitectureTest` passes

**Tests**: none (entity/migration)
**Gate**: build

**Commit**: `feat(workspace): add V1.30 foundation entities and migration`

---

### T2: WorkspaceAccessGuard + WorkspaceAcessoNegadoException

**What**: ACL guard mirroring `DashboardAccessGuard`; 403 when sem funcionário/nó/CC.
**Where**: `workspace/application/WorkspaceAccessGuard.java`, `workspace/domain/WorkspaceAcessoNegadoException.java`, `GlobalExceptionHandler` mapping
**Depends on**: T1
**Reuses**: `DashboardAccessGuard.java`
**Requirement**: Edge — organograma 403; WKS-08 prep

**Tools**:
- Skill: `spring-security`

**Done when**:
- [ ] `resolve` / `assertEscopo` match dashboard deny rules
- [ ] `WorkspaceAccessGuardTest`: acessoTotal, negado sem nó, negado CC vazio — ≥6 tests
- [ ] Gate: `cd backend && mvn test -Dtest=WorkspaceAccessGuardTest`

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add WorkspaceAccessGuard with ACL parity`

---

### T3: DatasetQuotaPolicy + workspace constants

**What**: Central quota limits (20 datasets, 500 rows, 30 fields, 50 widgets, 10 workspaces, 30 widgets/workspace).
**Where**: `workspace/application/DatasetQuotaPolicy.java`, `workspace/domain/WorkspaceLimits.java`
**Depends on**: T1
**Reuses**: `DashboardLayoutService` MAX_WIDGETS pattern
**Requirement**: WKS-03

**Done when**:
- [ ] Constants match design.md open questions
- [ ] `DatasetQuotaPolicyTest`: at-limit blocks, under-limit allows — ≥8 branch tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add quota policy and limits`

---

### T4: DatasetRowValidator (typed field validation)

**What**: Domain validator for NUMERO, TEXTO, DATA, MOEDA, REFERENCIA against schema JSONB.
**Where**: `workspace/domain/DatasetRowValidator.java`
**Depends on**: T1
**Reuses**: Folha `BigDecimal` conventions
**Requirement**: WKS-02

**Done when**:
- [ ] Rejects type mismatch with field-level errors
- [ ] `DatasetRowValidatorTest`: one test per type + incompatible combo — ≥10 tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add DatasetRowValidator for typed rows`

---

### T5: FormulaEngine — parse and validate (whitelist)

**What**: Tokenizer + AST builder + `validate()` rejecting unknown functions/fields (AD-018).
**Where**: `workspace/domain/formula/*`, `workspace/application/FormulaEngine.java`
**Depends on**: T4
**Reuses**: None (greenfield)
**Requirement**: WKS-06, WKS-07

**Tools**:
- Skill: `spring-security` (review whitelist)

**Done when**:
- [ ] Whitelist: SOMA, MÉDIA, MÍN, MÁX, CONTAGEM, SE, arithmetic, comparators
- [ ] Rejects `Runtime`, `eval`, unknown identifiers
- [ ] `FormulaEngineValidateTest`: ≥15 cases (valid, invalid fn, invalid field, nested SE)
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add FormulaEngine parse and validate`

---

### T6: FormulaEngine — evaluate

**What**: `evaluate()` over `EvaluationContext` with BigDecimal semantics.
**Where**: `workspace/application/FormulaEngine.java` (extend)
**Depends on**: T5
**Requirement**: WKS-06, WKS-09 (numeric output)

**Done when**:
- [ ] Manual parity: `SOMA(a)*MÉDIA(b)` matches spec independent test
- [ ] `FormulaEngineEvaluateTest`: aggregations, SE branches, division-by-zero safe — ≥12 tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add FormulaEngine evaluate`

---

### T7: DatasetService (schema CRUD + WKS-04)

**What**: Create/update/delete dataset schema with owner scoping, quota, schema version increment.
**Where**: `workspace/application/DatasetService.java`, DTOs, `workspace/api/DatasetController.java` (skeleton if needed for wiring)
**Depends on**: T2, T3, T4
**Requirement**: WKS-01, WKS-04

**Tools**:
- Skill: `spring-boot-new-endpoint`, `jpa-performance`

**Done when**:
- [ ] WKS-04: remove field with data → 409 unless `confirmarRemocao=true`
- [ ] WKS-03: block at dataset quota
- [ ] `DatasetServiceTest`: ≥12 tests covering ACs + stale schema 409
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add DatasetService schema CRUD`

---

### T8: DatasetRowService

**What**: Row CRUD with validation, row quota, owner check.
**Where**: `workspace/application/DatasetRowService.java`
**Depends on**: T7, T4
**Requirement**: WKS-02, WKS-03

**Done when**:
- [ ] Invalid row → 400 with per-field errors
- [ ] Row quota enforced
- [ ] `DatasetRowServiceTest`: ≥10 tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add DatasetRowService`

---

### T9: DatasetController + WebMvcTest

**What**: REST `/workspace/datasets` + `/workspace/datasets/{id}/rows`; wire T7–T8.
**Where**: `workspace/api/DatasetController.java`, `DatasetControllerWebMvcTest.java`
**Depends on**: T7, T8
**Requirement**: WKS-01, WKS-02, WKS-03, WKS-04; Edge 403

**Done when**:
- [ ] CRUD routes: happy + 400 + 403 + 409
- [ ] User isolation (cannot read other's dataset)
- [ ] `DatasetControllerWebMvcTest`: ≥14 tests
- [ ] Gate: `cd backend && mvn test -Dtest=DatasetControllerWebMvcTest`

**Tests**: WebMvcTest (e2e layer)
**Gate**: full API

**Commit**: `feat(workspace): expose dataset REST API`

---

### T10: WidgetDefinitionService

**What**: CRUD user widget definitions (tipo, fontes, formula); marks `invalido` when formula breaks.
**Where**: `workspace/application/WidgetDefinitionService.java`
**Depends on**: T5, T6, T7
**Requirement**: WKS-05, WKS-06, WKS-07; Edge formula inválida após schema change

**Done when**:
- [ ] Validates formula on save via FormulaEngine
- [ ] `WidgetDefinitionServiceTest`: ≥10 tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add WidgetDefinitionService`

---

### T11: WidgetDefinitionController + WebMvcTest

**What**: REST `/workspace/widget-definitions`.
**Where**: `workspace/api/WidgetDefinitionController.java`, `WidgetDefinitionControllerWebMvcTest.java`
**Depends on**: T10
**Requirement**: WKS-05, WKS-06, WKS-07

**Done when**:
- [ ] Invalid formula → 400 pointing field
- [ ] WebMvcTest ≥10 tests
- [ ] Gate: full API

**Tests**: WebMvcTest
**Gate**: full API

**Commit**: `feat(workspace): expose widget definition REST API`

---

### T12: LayoutValidator (shared widget layout rules)

**What**: Extract or implement max 30 widgets, colSpan bounds, ordem normalization — shared with dashboard semantics.
**Where**: `dashboard/application/LayoutValidator.java` (extract) OR `workspace/application/WorkspaceLayoutValidator.java`
**Depends on**: T1
**Reuses**: `DashboardLayoutService` validation logic
**Requirement**: WKS-10 (layout validation)

**Done when**:
- [ ] Max 30 widgets enforced
- [ ] Unit tests ≥6; dashboard tests still pass if extracted
- [ ] Gate: `cd backend && mvn test -Dtest=DashboardLayoutServiceTest,WorkspaceLayoutValidatorTest`

**Tests**: unit
**Gate**: quick BE

**Commit**: `refactor(dashboard): extract LayoutValidator for workspace reuse`

---

### T13: WorkspaceService

**What**: Multi-workspace CRUD; layout JSONB save; delete workspace does NOT delete datasets.
**Where**: `workspace/application/WorkspaceService.java`
**Depends on**: T2, T12
**Requirement**: WKS-10, WKS-11, WKS-12

**Done when**:
- [ ] Unique (usuario_id, nome)
- [ ] Delete workspace preserves datasets
- [ ] `WorkspaceServiceTest`: ≥10 tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add WorkspaceService multi-workspace`

---

### T14: WorkspaceController + WebMvcTest

**What**: REST `/workspace/workspaces` + layout PUT.
**Where**: `workspace/api/WorkspaceController.java`, `WorkspaceControllerWebMvcTest.java`
**Depends on**: T13
**Requirement**: WKS-10, WKS-11, WKS-12; Edge 403

**Done when**:
- [ ] List/create/switch/delete + layout save
- [ ] WebMvcTest ≥12 tests
- [ ] Gate: full API

**Tests**: WebMvcTest
**Gate**: full API

**Commit**: `feat(workspace): expose workspace REST API`

---

### T15: OrcamentoConsultaPort + adapter

**What**: Port aggregating realizado from folha with CC rollup for template/query.
**Where**: `workspace/port/OrcamentoConsultaPort.java`, `workspace/infrastructure/OrcamentoConsultaAdapter.java`
**Depends on**: T2
**Reuses**: `FolhaConsultaPort`, `DashboardStatsAggregator` rollup patterns
**Requirement**: WKS-13 prep, WKS-08

**Tools**:
- Skill: `jpa-performance`

**Done when**:
- [ ] Scoped by AccessContextDTO
- [ ] `OrcamentoConsultaAdapterTest`: ≥6 tests with mocked ports
- [ ] `ModularArchitectureTest` passes (no foreign infra)
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE + build (ArchUnit)

**Commit**: `feat(workspace): add OrcamentoConsultaPort adapter`

---

### T16: WidgetQueryService + WidgetDataController WebMvcTest

**What**: Resolve widget data from datasets + system sources + formula; pt-BR money in DTO.
**Where**: `workspace/application/WidgetQueryService.java`, `workspace/api/WorkspaceWidgetDataController.java`, WebMvcTest
**Depends on**: T6, T8, T10, T15
**Requirement**: WKS-08, WKS-09

**Done when**:
- [ ] ACL on system sources; empty outside scope
- [ ] Monetary fields as string formatted pt-BR in response
- [ ] `WidgetQueryServiceTest` ≥10 + WebMvcTest ≥8
- [ ] Gate: full API

**Tests**: unit + WebMvcTest
**Gate**: full API

**Commit**: `feat(workspace): add WidgetQueryService and data endpoint`

---

### T17: OrcamentoTemplateInstaller

**What**: Native template: dataset schema + widget defs + layout entries per design inline table.
**Where**: `workspace/application/OrcamentoTemplateInstaller.java`
**Depends on**: T10, T13, T15
**Requirement**: WKS-13, WKS-14

**Done when**:
- [ ] Installs empty orçamento dataset + preconfigured widgets
- [ ] `OrcamentoTemplateInstallerTest`: ≥6 tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add native orçamento template installer`

---

### T18: Template install endpoint (native orçamento)

**What**: `POST /workspace/templates/orcamento-padrao/install`; native catalog entry WKS-14.
**Where**: `workspace/api/TemplateController.java` (minimal), WebMvcTest
**Depends on**: T17
**Requirement**: WKS-13, WKS-14

**Done when**:
- [ ] Install populates workspace with widgets; realizado from folha when data exists
- [ ] WebMvcTest ≥6 tests
- [ ] Gate: full API

**Tests**: WebMvcTest
**Gate**: full API

**Commit**: `feat(workspace): expose orçamento template install endpoint`

---

### T19: ArchUnit — workspace domain boundaries

**What**: Extend `ModularArchitectureTest` for `workspace.application` / `workspace.api` AD-010 rules.
**Where**: `backend/src/test/java/.../arch/ModularArchitectureTest.java`
**Depends on**: T1, T15
**Requirement**: AD-010 compliance

**Done when**:
- [ ] Rules forbid cross-domain infrastructure access
- [ ] Gate: `mvn test -Dtest=ModularArchitectureTest`

**Tests**: unit (ArchUnit)
**Gate**: build

**Commit**: `test(arch): add workspace modular boundary rules`

---

### T20: workspaceService.ts + TypeScript types

**What**: API client for datasets, rows, widgets, workspaces, widget data.
**Where**: `frontend/src/services/workspaceService.ts`, types aligned OpenAPI
**Depends on**: T9, T11, T14, T16 (API stable)
**Reuses**: `dashboardLayoutService.ts` pattern

**Tools**:
- Skill: `api-client`

**Done when**:
- [ ] All P1 endpoints wrapped; RFC 7807 errors
- [ ] `workspaceService.test.ts`: mock fetch ≥8 tests
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add workspace API client`

---

### T21: Route /workspace + ACL gate + menu

**What**: `WorkspaceRoute`, menu item below Meu Dashboard, `workspaceAccess.ts`.
**Where**: `frontend/src/routes/`, `frontend/src/utils/workspaceAccess.ts`, menu config
**Depends on**: T20
**Reuses**: `dashboardAccess.ts`, `DashboardCustomRoute` pattern
**Requirement**: Edge 403 UI

**Done when**:
- [ ] Denied users see access denied, not empty workspace
- [ ] `workspaceAccess.test.ts` ≥5 tests
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add route menu and ACL gate`

---

### T22: useWorkspaceLayout hook

**What**: CRUD workspaces, layout draft/save explicit, workspace switcher state.
**Where**: `frontend/src/pages/Workspace/hooks/useWorkspaceLayout.ts`
**Depends on**: T20
**Reuses**: `useDashboardLayout.ts`
**Requirement**: WKS-11

**Done when**:
- [ ] Switch preserves per-workspace layout
- [ ] Hook tests ≥8 (MSW or mocked service)
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add useWorkspaceLayout hook`

---

### T23: WorkspacePage + WorkspaceSwitcher

**What**: Shell page with workspace selector, toolbar, empty state.
**Where**: `frontend/src/pages/Workspace/WorkspacePage.tsx`, `WorkspaceSwitcher.tsx`
**Depends on**: T21, T22
**Requirement**: WKS-10, WKS-11

**Tools**:
- Skill: `component-architecture`, `testing-a11y`

**Done when**:
- [ ] Create/rename/delete workspace flows
- [ ] Tests by role ≥6
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add WorkspacePage shell and switcher`

---

### T24: DatasetEditorPage

**What**: Schema builder + editable row grid with type validation feedback.
**Where**: `frontend/src/pages/Workspace/DatasetEditorPage.tsx`
**Depends on**: T20
**Requirement**: WKS-01, WKS-02, WKS-04

**Tools**:
- Skill: `forms-validation`, `testing-a11y`

**Done when**:
- [ ] Add/remove fields; row validation errors per field
- [ ] Confirm dialog on destructive schema change
- [ ] Tests ≥10 by role/label
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add DatasetEditorPage`

---

### T25: WidgetBuilderDrawer + FormulaEditor

**What**: Create/edit widget definition: tipo, fontes, formula with async validation.
**Where**: `frontend/src/pages/Workspace/WidgetBuilderDrawer.tsx`, `FormulaEditor.tsx`
**Depends on**: T20, T24
**Requirement**: WKS-05, WKS-06, WKS-07

**Done when**:
- [ ] Shows server validation errors on formula
- [ ] Tests ≥8
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add widget builder and formula editor`

---

### T26: DynamicKpiWidget + DynamicTableWidget + DynamicChartWidget

**What**: Schema-driven renderers using API widget data payload.
**Where**: `frontend/src/pages/Workspace/widgets/Dynamic*.tsx`
**Depends on**: T20
**Reuses**: `KpiWidget`, `chartUtils`, `formatMoneyDisplay`
**Requirement**: WKS-05, WKS-09

**Done when**:
- [ ] KPI/table/line/bar render from mock payload
- [ ] Money displays pt-BR
- [ ] Tests ≥9 (3 per widget type)
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add dynamic widget renderers`

---

### T27: Workspace registry + WidgetDataRenderer

**What**: Extend registry with USER_* entries; resolves Nível 1 + user widgets.
**Where**: `frontend/src/pages/Workspace/widgets/registry.tsx`, `WidgetDataRenderer.tsx`
**Depends on**: T26
**Reuses**: `MeuDashboard/widgets/registry.tsx` imports
**Requirement**: WKS-05, design registry dual

**Done when**:
- [ ] Fixed catalog widgets still work when placed in workspace layout
- [ ] User widgets route to dynamic renderers
- [ ] Tests ≥6
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add dual widget registry and renderer`

---

### T28: Workspace grid + save flow

**What**: Integrate DashboardGrid pattern, dnd-kit, explicit save, invalid widget banner.
**Where**: `frontend/src/pages/Workspace/WorkspaceGrid.tsx`, wire into WorkspacePage
**Depends on**: T23, T27
**Reuses**: `DashboardGrid.tsx`, dnd-kit sensors from Organograma
**Requirement**: WKS-10; Edge widget fórmula inválida

**Done when**:
- [ ] Drag reorder + colSpan; save persists layout
- [ ] Broken formula shows banner, not page crash
- [ ] Tests ≥8
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add workspace grid with layout save`

---

### T29: Orçamento install UI + P1 E2E smoke

**What**: Button to install native orçamento template; Playwright smoke spec.
**Where**: `frontend/src/pages/Workspace/`, `frontend/e2e/workspace.spec.ts`
**Depends on**: T18, T28
**Requirement**: WKS-13, WKS-14; P1 independent tests from spec

**Tools**:
- Skill: `testing-a11y`

**Done when**:
- [ ] E2E: create dataset → row → widget → 2 workspaces → install orçamento
- [ ] `npm run test:e2e -- e2e/workspace.spec.ts` passes
- [ ] Gate: release (backend tests + e2e)

**Tests**: e2e
**Gate**: release

**Commit**: `test(workspace): add P1 e2e smoke and orçamento install UI`

---

### T30: P1 release gate + spec traceability update

**What**: Full suite green; update spec WKS-01…14 status Pending→Done in tasks notes (not spec until Verifier).
**Where**: `_docs/specs/features/workspace-usuario/tasks.md` (status section)
**Depends on**: T29
**Requirement**: P1 complete

**Done when**:
- [ ] `cd backend && mvn test && cd ../frontend && npm run lint && npm test && npm run build` passes
- [ ] P1 batch summary recorded in Handoff (STATE.md) when user commits

**Tests**: none (gate only)
**Gate**: release

**Commit**: `chore(workspace): P1 complete — release gate green`

---

### T31: Flyway V1.31 — templates + row audit tables

**What**: Migration for template marketplace + `workspace_dataset_row_audit`.
**Where**: `db/migration/V1.31__workspace_templates.sql`, entities + repos
**Depends on**: T30 (P1 merged/stable)
**Requirement**: WKS-15 prep, WKS-22 prep

**Tools**:
- Skill: `flyway-migration-writer`

**Done when**:
- [ ] Tables match design.md P2 schema
- [ ] Build gate passes

**Tests**: none
**Gate**: build

**Commit**: `feat(workspace): add V1.31 template and audit tables`

---

### T32: TemplatePublishService

**What**: Publish structure only; hierarchy visibility; hash idempotency; WKS-18 guard.
**Where**: `workspace/application/TemplatePublishService.java`
**Depends on**: T31, T2
**Requirement**: WKS-15, WKS-16, WKS-18, WKS-19 (version/hash)

**Done when**:
- [ ] Publish never includes row data
- [ ] Duplicate hash → no-op
- [ ] `TemplatePublishServiceTest`: ≥12 tests including hierarchy
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add TemplatePublishService`

---

### T33: TemplateInstallService + version upgrade

**What**: Install copy; track version; optional upgrade WKS-20/21 preserving compatible data.
**Where**: `workspace/application/TemplateInstallService.java`
**Depends on**: T32, T7, T10
**Requirement**: WKS-17, WKS-19, WKS-20, WKS-21

**Done when**:
- [ ] Installed copy independent
- [ ] Upgrade adds fields without wiping compatible rows
- [ ] `TemplateInstallServiceTest`: ≥10 tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add TemplateInstallService with versioning`

---

### T34: DatasetAuditService

**What**: Record CREATE/UPDATE/DELETE on rows; query history chronologically.
**Where**: `workspace/application/DatasetAuditService.java`
**Depends on**: T31, T8
**Requirement**: WKS-22, WKS-23

**Done when**:
- [ ] Two edits produce two audit entries with author/timestamp
- [ ] `DatasetAuditServiceTest`: ≥8 tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add dataset row audit trail`

---

### T35: TemplateController + audit REST + WebMvcTest

**What**: `/workspace/templates/publish`, catalog, install, upgrade; row history GET.
**Where**: `workspace/api/TemplateController.java`, audit endpoint on DatasetController, WebMvcTests
**Depends on**: T32, T33, T34
**Requirement**: WKS-15…WKS-23; Edge template deleted from catalog

**Done when**:
- [ ] Out-of-hierarchy user cannot see template (WKS-16)
- [ ] WebMvcTest ≥16 tests
- [ ] Gate: full API

**Tests**: WebMvcTest
**Gate**: full API

**Commit**: `feat(workspace): expose template marketplace and audit API`

---

### T36: Wire audit into DatasetRowService

**What**: Hook audit on row mutations (modify T8 integration).
**Where**: `DatasetRowService.java`
**Depends on**: T34, T8
**Requirement**: WKS-22

**Done when**:
- [ ] Every row mutation creates audit row
- [ ] Existing DatasetRowService tests updated + audit assertions
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): wire row audit into DatasetRowService`

---

### T37: TemplateCatalogPage (publish + install)

**What**: FE hub for browse/install/publish templates (Relatórios card pattern).
**Where**: `frontend/src/pages/Workspace/TemplateCatalogPage.tsx`
**Depends on**: T35, T20
**Reuses**: `pages/Relatorios/` layout
**Requirement**: WKS-15, WKS-17

**Tools**:
- Skill: `component-architecture`, `testing-a11y`

**Done when**:
- [ ] Publish flow explicit opt-in; install creates local copy
- [ ] Tests ≥8 by role
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add template catalog page`

---

### T38: Version upgrade indicator UI

**What**: Badge when newer template version available; optional upgrade action WKS-20.
**Where**: `frontend/src/pages/Workspace/components/TemplateUpgradeBanner.tsx`
**Depends on**: T37
**Requirement**: WKS-20, WKS-21

**Done when**:
- [ ] User not forced to upgrade
- [ ] Upgrade preserves data when compatible
- [ ] Tests ≥5
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add template version upgrade UI`

---

### T39: P2 release gate

**What**: Full suite + marketplace integration scenarios in WebMvcTest suite.
**Depends on**: T38
**Requirement**: P2 complete WKS-15…23

**Done when**:
- [ ] Release gate passes
- [ ] Hierarchy visibility scenario documented in test names

**Tests**: none (gate)
**Gate**: release

**Commit**: `chore(workspace): P2 complete — release gate green`

---

### T40: Flyway V1.32 + WORKSPACE_IA_CRIAR permission

**What**: `workspace_ia_proposal` table; permission constant; optional dev seed.
**Where**: `V1.32__workspace_proposals.sql`, permission docs in `OrganogramaAcessoService` or auth module
**Depends on**: T39
**Requirement**: WKS-24 prep; permission name from design

**Tools**:
- Skill: `flyway-migration-writer`, `spring-security`

**Done when**:
- [ ] Entity + repo compile
- [ ] Permission string `WORKSPACE_IA_CRIAR` documented
- [ ] Build gate passes

**Tests**: none
**Gate**: build

**Commit**: `feat(workspace): add V1.32 proposals and IA permission`

---

### T41: WorkspaceProposalService

**What**: Create pending proposal, confirm, discard, dedup, TTL 72h scheduled expiry, audit WKS-29.
**Where**: `workspace/application/WorkspaceProposalService.java`
**Depends on**: T40, T5, T32, T3
**Requirement**: WKS-24, WKS-26, WKS-27, WKS-29, WKS-30; Edge quota on apply

**Done when**:
- [ ] Confirm persists; create does not
- [ ] Similar template suggested before greenfield (WKS-27 simple name match)
- [ ] `WorkspaceProposalServiceTest`: ≥14 tests
- [ ] Gate passes

**Tests**: unit
**Gate**: quick BE

**Commit**: `feat(workspace): add WorkspaceProposalService`

---

### T42: WorkspaceConsultaPort + metadata GET endpoints

**What**: Read-only catalog metadata for MCP/UI; ACL-scoped WKS-28.
**Where**: `workspace/port/WorkspaceConsultaPort.java`, `workspace/api/WorkspaceMetadataController.java`
**Depends on**: T32, T2
**Requirement**: WKS-28

**Done when**:
- [ ] Returns only templates visible to user
- [ ] Unit + WebMvcTest ≥8
- [ ] Gate passes

**Tests**: unit + WebMvcTest
**Gate**: full API

**Commit**: `feat(workspace): add metadata endpoints for IA read path`

---

### T43: ApiKeyWriteGuardFilter workspace allowlist

**What**: Allow POST only on `/workspace/proposals/**` for API Key with `WORKSPACE_IA_CRIAR`; new role marker.
**Where**: `security/ApiKeyWriteGuardFilter.java`, `ApiKeySecurity.java`, `JwtAuthenticationFilter.java`
**Depends on**: T40
**Reuses**: AD-013 patterns
**Requirement**: WKS-25 path isolation; spec MCP write

**Tools**:
- Skill: `spring-security`

**Done when**:
- [ ] Readonly API Key blocked on POST /workspace/datasets
- [ ] Workspace-capable key allowed on proposals only
- [ ] `ApiKeyWriteGuardFilterTest`: ≥8 regression tests
- [ ] Gate: security

**Tests**: unit
**Gate**: security

**Commit**: `feat(security): allow API Key workspace proposal writes only`

---

### T44: ProposalController + WebMvcTest

**What**: REST proposals create/confirm/discard; permission WKS-25.
**Where**: `workspace/api/ProposalController.java`, `ProposalControllerWebMvcTest.java`
**Depends on**: T41, T43
**Requirement**: WKS-24, WKS-25, WKS-26

**Done when**:
- [ ] Without permission → 403, no proposal body
- [ ] WebMvcTest ≥12 including API Key paths
- [ ] Gate: security

**Tests**: WebMvcTest
**Gate**: security

**Commit**: `feat(workspace): expose proposal REST API`

---

### T45: MCP whitelist — workspace read + proposal tools

**What**: Extend `api-to-mcp.yml` + OpenAPI regen if needed; metadata GET + proposal POST operationIds.
**Where**: `diversos/openapi/api-to-mcp.yml`, `validate-mcp-whitelist.sh`
**Depends on**: T42, T44
**Reuses**: `mcp-agent-tools` patterns
**Requirement**: WKS-24 MCP path

**Done when**:
- [ ] `validate-mcp-whitelist.sh` passes
- [ ] Folha/benefícios tools unchanged readonly
- [ ] Gate: MCP script

**Tests**: shell validation
**Gate**: MCP

**Commit**: `feat(mcp): add workspace metadata and proposal tools`

---

### T46: ProposalReviewDialog + "Sugerir para mim"

**What**: UI to review/confirm/discard proposals; on-demand suggestion button WKS-31/32.
**Where**: `frontend/src/pages/Workspace/ProposalReviewDialog.tsx`, suggestion action on WorkspacePage
**Depends on**: T44, T20
**Requirement**: WKS-31, WKS-32

**Done when**:
- [ ] No background suggestions; only on button click
- [ ] Tests ≥8
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `feat(workspace): add proposal review and suggest UI`

---

### T47: P3 Frontend integration tests for IA flows

**What**: Vitest integration: suggest → review → confirm; permission denied state.
**Where**: `frontend/src/pages/Workspace/WorkspaceIa.test.tsx`
**Depends on**: T46
**Requirement**: WKS-24, WKS-25, WKS-31

**Done when**:
- [ ] MSW covers proposal lifecycle
- [ ] ≥6 tests
- [ ] Gate: quick FE

**Tests**: unit
**Gate**: quick FE

**Commit**: `test(workspace): add IA proposal UI integration tests`

---

### T48: P3 release gate + Verifier prep

**What**: Full release gate; feature ready for Verifier (author completes all tasks).
**Depends on**: T45, T47
**Requirement**: WKS-24…32 complete

**Done when**:
- [ ] `cd backend && mvn test && cd ../frontend && npm run lint && npm test && npm run build`
- [ ] Security gate: proposal + API Key tests green
- [ ] MCP validate script green
- [ ] Handoff updated for Verifier dispatch

**Tests**: none (gate)
**Gate**: release + security + MCP

**Commit**: `chore(workspace): P3 complete — ready for Verifier`

---

## Phase Execution Map

```
Phase 1:   T1 ──→ T2 ──→ T3 ──→ T4
Phase 2:   T5 ──→ T6
Phase 3:   T7 ──→ T8 ──→ T9
Phase 4:   T10 ──→ T11
Phase 5:   T12 ──→ T13 ──→ T14 ──→ T15 ──→ T16
Phase 6:   T17 ──→ T18 ──→ T19
Phase 7:   T20 ──→ T21 ──→ T22 ──→ T23 ──→ T24
Phase 8:   T25 ──→ T26 ──→ T27 ──→ T28
Phase 9:   T29 ──→ T30
Phase 10:  T31 ──→ T32 ──→ T33 ──→ T34 ──→ T35 ──→ T36
Phase 11:  T37 ──→ T38
Phase 12:  T39
Phase 13:  T40 ──→ T41 ──→ T42 ──→ T43 ──→ T44
Phase 14:  T45 ──→ T46 ──→ T47
Phase 15:  T48
```

Execution is strictly sequential — one task at a time, in order.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Migration + entities | 1 migration + domain package | ✅ Granular |
| T5: Formula validate | 1 engine concern | ✅ Granular |
| T6: Formula evaluate | 1 engine concern | ✅ Granular |
| T24: DatasetEditorPage | 1 page | ✅ Granular |
| T28: Grid + save | 1 integration surface | ✅ Granular |
| T41: ProposalService | 1 service | ✅ Granular |

All 48 tasks: ✅ Granular (1 endpoint, 1 service, 1 page, or 1 migration scope each).

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ----------------- | ------------- | ------ |
| T1 | Nível 1 merged | (external prereq) | ✅ |
| T2 | T1 | T1→T2 | ✅ |
| T3 | T1 | T1→T3 | ✅ |
| T4 | T1 | T1→T4 | ✅ |
| T5 | T4 | T4→T5 | ✅ |
| T6 | T5 | T5→T6 | ✅ |
| T7 | T2,T3,T4 | T2,T3,T4→T7 | ✅ |
| T8 | T7,T4 | T7→T8 | ✅ |
| T9 | T7,T8 | T7,T8→T9 | ✅ |
| T10 | T5,T6,T7 | T5,T6,T7→T10 | ✅ |
| T11 | T10 | T10→T11 | ✅ |
| T12 | T1 | T1→T12 | ✅ |
| T13 | T2,T12 | T2,T12→T13 | ✅ |
| T14 | T13 | T13→T14 | ✅ |
| T15 | T2 | T2→T15 | ✅ |
| T16 | T6,T8,T10,T15 | deps→T16 | ✅ |
| T17 | T10,T13,T15 | deps→T17 | ✅ |
| T18 | T17 | T17→T18 | ✅ |
| T19 | T1,T15 | T1,T15→T19 | ✅ |
| T20 | T9,T11,T14,T16 | BE APIs→T20 | ✅ |
| T21 | T20 | T20→T21 | ✅ |
| T22 | T20 | T20→T22 | ✅ |
| T23 | T21,T22 | T21,T22→T23 | ✅ |
| T24 | T20 | T20→T24 | ✅ |
| T25 | T20,T24 | T20,T24→T25 | ✅ |
| T26 | T20 | T20→T26 | ✅ |
| T27 | T26 | T26→T27 | ✅ |
| T28 | T23,T27 | T23,T27→T28 | ✅ |
| T29 | T18,T28 | T18,T28→T29 | ✅ |
| T30 | T29 | T29→T30 | ✅ |
| T31–T48 | per body | sequential phases | ✅ |

No backward dependencies. ✅

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | Entity/migration | none | none | ✅ |
| T2 | WorkspaceAccessGuard | unit | unit | ✅ |
| T3 | QuotaPolicy | unit | unit | ✅ |
| T4 | RowValidator | unit | unit | ✅ |
| T5–T6 | FormulaEngine | unit | unit | ✅ |
| T7–T8 | Services | unit | unit | ✅ |
| T9 | Controller | WebMvcTest | WebMvcTest | ✅ |
| T10 | Service | unit | unit | ✅ |
| T11 | Controller | WebMvcTest | WebMvcTest | ✅ |
| T12 | LayoutValidator | unit | unit | ✅ |
| T13 | Service | unit | unit | ✅ |
| T14 | Controller | WebMvcTest | WebMvcTest | ✅ |
| T15 | Adapter | unit | unit | ✅ |
| T16 | Service+Controller | WebMvcTest+unit | unit+WebMvcTest | ✅ |
| T17–T18 | Installer+Controller | unit+WebMvcTest | unit+WebMvcTest | ✅ |
| T19 | ArchUnit | unit | unit | ✅ |
| T20–T28 | FE components | unit | unit | ✅ |
| T29 | E2E | Playwright | e2e | ✅ |
| T30 | Gate only | none | none | ✅ |
| T31 | Migration | none | none | ✅ |
| T32–T36 | Services | unit | unit | ✅ |
| T35 | Controller | WebMvcTest | WebMvcTest | ✅ |
| T37–T38, T46–T47 | FE | unit | unit | ✅ |
| T43 | Security filter | unit | unit | ✅ |
| T44 | Controller | WebMvcTest | WebMvcTest | ✅ |
| T45 | MCP script | shell | shell | ✅ |

No ❌ violations.

---

## Recommended Tools (confirm before Execute)

Per task, default skills from repo (override per task if needed):

| Area | MCP | Skills |
| ---- | --- | ------ |
| Backend BE | project MCP (read-only folha for smoke) | `jpa-performance`, `flyway-migration-writer`, `spring-boot-new-endpoint`, `spring-security` |
| Frontend FE | — | `component-architecture`, `forms-validation`, `testing-a11y`, `api-client` |
| MCP P3 | `@sgaluza/api-to-mcp` via script | `mcp-agent-tools` design reference |
| Verifier | — | `tlc-spec-driven` validate flow |

**Question before Execute:** For each batch, confirm MCPs/skills above or specify overrides.

---

## Approval Checklist

- [ ] 48 tasks granularity approved
- [ ] Test Coverage Matrix + Gate Commands confirmed
- [ ] Batch plan (7 batches) accepted
- [ ] P1 can start only after Nível 1 merged
- [ ] Orçamento inline template (T17–T18) accepted without separate spec

**Status after approval:** set to `Approved — ready for Execute Batch 1 (T1–T6)`
