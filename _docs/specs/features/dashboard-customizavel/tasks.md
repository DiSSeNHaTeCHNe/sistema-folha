# Dashboard Customizável Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/dashboard-customizavel/design.md`  
**Spec**: `_docs/specs/features/dashboard-customizavel/spec.md`  
**Status**: Draft — aguardando aprovação antes de Execute  
**Constraints**: AD-008, AD-010, AD-011, AD-013, AD-017 (JSONB)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` + AD-004, `.agents/skills/testing-a11y/SKILL.md`, `dashboard-customizavel/spec.md` (DASHC-01…44 ACs + edge cases).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Dashboard application (`DashboardAccessGuard`, `DashboardLayoutService`, `DashboardWidgetCatalogService`, `DashboardWidgetQueryService`) | unit (Mockito) | All branches; 1:1 to DASHC ACL/validation ACs; edge cases (widgetId inválido, max 30, ordem normalizada, widget removido do catálogo na leitura) | `backend/src/test/java/**/dashboard/application/*Test.java` | `cd backend && mvn test -Dtest=DashboardAccessGuardTest,DashboardLayoutServiceTest,DashboardWidgetCatalogServiceTest,DashboardWidgetQueryServiceTest` |
| Dashboard API (`DashboardLayoutController`, `DashboardWidgetController`) | WebMvcTest | All routes in scope: happy + ACL 403 + validation 400 + isolamento usuários; auth 403 sem token | `backend/src/test/java/**/dashboard/api/*WebMvcTest.java` | `cd backend && mvn test -Dtest=DashboardLayoutControllerWebMvcTest,DashboardWidgetControllerWebMvcTest` |
| Dashboard domain/infrastructure (entity JSONB, Flyway) | none | Round-trip JSONB exercitado em `DashboardLayoutServiceTest`; migration compile via full suite | `dashboard/domain/`, `db/migration/V1.29__*` | compile gate |
| Frontend hooks/components (`useDashboardLayout`, `DashboardGrid`, widgets) | unit (Vitest + Testing Library) | Queries by role/label; DASHC-07…13, DASHC-19…23 behaviors; keyboard reorder mock | `frontend/src/pages/MeuDashboard/**/*.test.tsx` | `cd frontend && npm test -- src/pages/MeuDashboard` |
| Frontend utils (`dashboardAccess.ts`) | unit | `podeAcessarMeuDashboard` — all ACL branches mirroring BE | `frontend/src/utils/dashboardAccess.test.ts` | `cd frontend && npm test -- dashboardAccess` |
| Frontend E2E (Meu Dashboard) | Playwright | DASHC-20: save layout, reload, persist; add + reorder smoke | `frontend/e2e/meu-dashboard.spec.ts` | `cd frontend && npm run test:e2e -- e2e/meu-dashboard.spec.ts` |
| Dashboard clássico (chip removal) | unit | DASHC-06: no KPI variation chips in DOM | `frontend/src/pages/Dashboard/Dashboard.test.tsx` | `cd frontend && npm test -- Dashboard.test` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After backend unit tasks (T3–T5, T17) | `cd backend && mvn test -Dtest=DashboardAccessGuardTest,DashboardLayoutServiceTest,DashboardWidgetCatalogServiceTest,DashboardWidgetQueryServiceTest` |
| Quick FE | After frontend unit tasks (T11–T15, T20) | `cd frontend && npm test -- src/pages/MeuDashboard src/utils/dashboardAccess` |
| Full | After WebMvcTest / E2E tasks (T6, T16, T18) | `cd backend && mvn test -Dtest=DashboardLayoutControllerWebMvcTest,DashboardWidgetControllerWebMvcTest && cd ../frontend && npm test -- src/pages/MeuDashboard && npm run test:e2e -- e2e/meu-dashboard.spec.ts` |
| Build | After migration/entity-only (T1–T2), FE shell (T7–T9), route wiring (T14) | `cd backend && mvn test -Dtest=ModularArchitectureTest && cd ../frontend && npm run build` |
| Release | After T24 (feature complete, pre-Verifier) | `cd backend && mvn test && cd ../frontend && npm run lint && npm test && npm run build` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Backend — Layout Foundation

```
T1 → T2 → T3 → T4 → T5 → T6
```

### Phase 2: Frontend — Widgets + Shell

```
T7 → T8 → T9 → T10
```

### Phase 3: Frontend — Grid + Edit Mode

```
T11 → T12 → T13 → T14
```

### Phase 4: QA — Fase 1 Gate

```
T15 → T16
```

### Phase 5: Backend — Widget Data (Fase 2)

```
T17 → T18 → T19
```

### Phase 6: Frontend — Parametrizável (Fase 2)

```
T20 → T21 → T22 → T23 → T24
```

**Batch packing (Execute):** 24 tasks → **4 batches** (~6–8 tasks each, whole phases):

| Batch | Phases | Tasks | Worker |
| ----- | ------ | ----- | ------ |
| 1 | Phase 1 | T1–T6 | Backend worker |
| 2 | Phases 2–3 | T7–T14 | Frontend worker |
| 3 | Phases 4–5 | T15–T19 | QA + Backend worker |
| 4 | Phase 6 | T20–T24 | Frontend worker |

Batches run **sequentially**. Offer sub-agents at Execute start; default inline if user declines.

---

## Task Breakdown

### T1: Flyway migration `V1.29__create_dashboard_layout.sql`

**What**: Criar tabela `dashboard_layout` com JSONB `widgets`, índice único `usuario_id`, FK cascade.  
**Where**: `backend/src/main/resources/db/migration/V1.29__create_dashboard_layout.sql`  
**Depends on**: None  
**Reuses**: Padrão migrations existentes (`V1.28__create_relatorio.sql`)  
**Requirements**: DASHC-19 (foundation), AD-017

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`

**Done when**:
- [x] Migration idempotente (`IF NOT EXISTS`)
- [x] Colunas `data_criacao` / `data_atualizacao` (não `data_alteracao`)
- [x] `mvn flyway:migrate` ou suite completa compila sem erro de schema

**Tests**: none  
**Gate**: build (`cd backend && mvn test -Dtest=ModularArchitectureTest`)  
**Commit**: `feat(dashboard): add V1.29 dashboard_layout migration`

---

### T2: Entity `DashboardLayout` + repository JSONB

**What**: Entity JPA com `@JdbcTypeCode(SqlTypes.JSON)` para `widgets`; `DashboardLayoutRepository`; record `WidgetInstancePayload`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/dashboard/domain/DashboardLayout.java`
- `backend/src/main/java/br/com/techne/sistemafolha/dashboard/domain/WidgetInstancePayload.java`
- `backend/src/main/java/br/com/techne/sistemafolha/dashboard/infrastructure/DashboardLayoutRepository.java`

**Depends on**: T1  
**Reuses**: Padrão `@PrePersist`/`@PreUpdate` de `Funcionario.java`  
**Requirements**: AD-017

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Entity mapeia JSONB sem dependência externa (Hypersistence)
- [x] Repository `findByUsuarioId`, `deleteByUsuarioId`
- [x] Compilação verde

**Tests**: none (JSON round-trip exercitado em T5)  
**Gate**: build  
**Commit**: `feat(dashboard): add DashboardLayout entity and repository with JSONB`

---

### T3: `DashboardAccessGuard` + exceção 403 + refactor ACL

**What**: Extrair `deveNegarAcesso` para `DashboardAccessGuard`; criar `DashboardAcessoNegadoException`; handler 403 no `GlobalExceptionHandler`; refatorar `DashboardService` e `DashboardConsultaAdapter` para delegar.  
**Where**:
- `backend/.../dashboard/application/DashboardAccessGuard.java`
- `backend/.../dashboard/domain/DashboardAcessoNegadoException.java`
- `backend/.../exception/GlobalExceptionHandler.java` (modify)
- `backend/.../dashboard/application/DashboardService.java` (modify)
- `backend/.../dashboard/application/DashboardConsultaAdapter.java` (modify)

**Depends on**: T2  
**Reuses**: Lógica existente `deveNegarAcesso`; padrão `RelatorioAcessoNegadoException`  
**Requirements**: DASHC-03, DASHC-25, DASHC-41 (foundation)

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [x] `assertEscopo(login)` lança 403 quando sem vínculo/nó/CC vazio
- [x] `DashboardService.getStats` comportamento clássico **inalterado** (200 + zeros)
- [x] Testes existentes de dashboard continuam verdes
- [x] Gate quick: `mvn test -Dtest=DashboardAccessGuardTest,DashboardServiceTest`

**Tests**: unit (`DashboardAccessGuardTest` — branches ACL espelhando spec)  
**Gate**: quick  
**Commit**: `feat(dashboard): centralize ACL in DashboardAccessGuard with 403 for custom dashboard`

---

### T4: Catálogo de widgets server-side

**What**: Enum/registry `WidgetCatalog` (12 entradas) + `DashboardWidgetCatalogService` com filtro ACL.  
**Where**:
- `backend/.../dashboard/domain/WidgetCatalog.java`
- `backend/.../dashboard/application/DashboardWidgetCatalogService.java`
- `backend/.../dashboard/api/WidgetCatalogItemDTO.java`

**Depends on**: T3  
**Reuses**: `OrganogramaAcessoPort` via guard  
**Requirements**: DASHC-14, DASHC-15, DASHC-42 (catalog filter)

**Done when**:
- [x] 12 `widgetId`s conforme design § Widget catalog
- [x] `grafico-funcionarios-por-cargo` presente no catálogo, ausente do default factory (T5)
- [x] `listarParaUsuario` filtra widgets conforme escopo
- [x] Gate quick: `mvn test -Dtest=DashboardWidgetCatalogServiceTest`

**Tests**: unit (ACL total vs scoped; widget permitido/negado)  
**Gate**: quick  
**Commit**: `feat(dashboard): add server-side widget catalog with ACL filter`

---

### T5: `DashboardLayoutService` — default, save, reset

**What**: Service com factory de layout padrão (11 widgets, ordem/colSpan/rowSpan do design), PUT atômico, normalização de ordem, validação widgetId/max 30, reset.  
**Where**: `backend/.../dashboard/application/DashboardLayoutService.java`  
**Depends on**: T4  
**Reuses**: `DashboardAccessGuard`, `DashboardWidgetCatalogService`, `DashboardLayoutRepository`  
**Requirements**: DASHC-01, DASHC-19…27

**Done when**:
- [x] Primeiro `obterOuCriarPadrao` persiste 11 widgets na ordem especificada
- [x] PUT rejeita `widgetId` inválido, `colSpan`>12, >30 widgets (400 via exceção/validation)
- [x] `usuarioId` nunca lido do DTO
- [x] Leitura ignora `widgetId` desconhecido no array salvo (DASHC-27)
- [x] JSONB round-trip: save/load preserva lista `widgets` intacta
- [x] Gate quick: `mvn test -Dtest=DashboardLayoutServiceTest`

**Tests**: unit (default factory, validation, isolation, unknown widgetId on read, JSON list integrity)  
**Gate**: quick  
**Commit**: `feat(dashboard): add DashboardLayoutService with default layout and validation`

---

### T6: `DashboardLayoutController` + WebMvcTest

**What**: REST endpoints layout + catálogo; DTOs com Bean Validation; testes MockMvc ACL/validation/isolamento.  
**Where**:
- `backend/.../dashboard/api/DashboardLayoutController.java`
- `backend/.../dashboard/api/DashboardLayoutDTO.java`, `WidgetInstanceDTO.java`
- `backend/src/test/java/.../dashboard/api/DashboardLayoutControllerWebMvcTest.java`

**Depends on**: T5  
**Reuses**: Padrão `DashboardControllerWebMvcTest`, `RelatorioFolhaControllerWebMvcTest` (403 ACL)  
**Requirements**: DASHC-03, DASHC-19…27, DASHC-26

**Done when**:
- [x] `GET/PUT/DELETE /dashboard/layout`, `GET /dashboard/widgets/catalog` expostos
- [x] Sem auth → 403; sem escopo → 403; com escopo → 200
- [x] PUT payload inválido → 400 `ErrorResponse`
- [x] Dois usuários: layout de A invisível para B
- [x] Gate full (backend slice): `mvn test -Dtest=DashboardLayoutControllerWebMvcTest`

**Tests**: WebMvcTest  
**Gate**: full (backend slice)  
**Commit**: `feat(dashboard): expose layout and catalog REST endpoints with ACL tests`

---

### T7: API client frontend — layout + types

**What**: `dashboardLayoutService.ts` (get/put/delete layout, get catalog) + tipos `WidgetInstance`, `DashboardLayout`, `WidgetCatalogItem`.  
**Where**:
- `frontend/src/services/dashboardLayoutService.ts`
- `frontend/src/pages/MeuDashboard/types.ts`

**Depends on**: T6  
**Reuses**: `frontend/src/services/api.ts`, padrão `dashboardService.ts`  
**Requirements**: DASHC-14 (client foundation)

**Done when**:
- [x] Funções tipadas chamam `/dashboard/layout` e `/dashboard/widgets/catalog`
- [x] `npm run build` passa

**Tests**: none (HTTP coberto indiretamente em T11/T15 via MSW ou mock)  
**Gate**: build  
**Commit**: `feat(dashboard): add frontend layout API client and types`

---

### T8: Componentes de widget + registry

**What**: Extrair blocos visuais do Dashboard em componentes reutilizáveis + `registry.tsx` com 12 entradas.  
**Where**:
- `frontend/src/pages/MeuDashboard/widgets/KpiWidget.tsx`
- `frontend/src/pages/MeuDashboard/widgets/EvolucaoMensalWidget.tsx`
- `frontend/src/pages/MeuDashboard/widgets/DistribuicaoWidget.tsx`
- `frontend/src/pages/MeuDashboard/widgets/TopRubricasWidget.tsx`
- `frontend/src/pages/MeuDashboard/widgets/FuncionariosPorCargoWidget.tsx`
- `frontend/src/pages/MeuDashboard/widgets/registry.tsx`

**Depends on**: T7  
**Reuses**: Visual/ lógica de `Dashboard/index.tsx`, `formatMoneyDisplay`, `theme.palette.charts`  
**Requirements**: DASHC-01, DASHC-02 (render foundation)

**Done when**:
- [x] Registry exporta `WidgetDefinition` conforme design
- [x] KPI widgets **sem** chips de variação percentual
- [x] Widget por cargo renderiza `porCargo` quando presente em stats
- [x] Gate quick FE: testes unitários mínimos por widget (render com stats mock)

**Tests**: unit (1 test file por widget type ou `widgets/registry.test.tsx` cobrindo render de cada id)  
**Gate**: quick FE  
**Commit**: `feat(dashboard): extract dashboard widgets and registry for Meu Dashboard`

---

### T9: Página shell `MeuDashboard` + `WidgetFrame` (modo visualização)

**What**: Página carrega layout (GET) + stats (GET /dashboard/stats); renderiza grid estática com widgets via registry; `WidgetFrame` só título + conteúdo (sem edição).  
**Where**:
- `frontend/src/pages/MeuDashboard/index.tsx`
- `frontend/src/pages/MeuDashboard/WidgetFrame.tsx`

**Depends on**: T8  
**Reuses**: `useNotification` para erros de carga  
**Requirements**: DASHC-01, DASHC-02, DASHC-11

**Done when**:
- [x] Primeiro acesso exibe 11 widgets na ordem do layout padrão
- [x] Valores batem com stats monolítico (mesmo usuário/competência)
- [x] Fora do modo edição: sem handles/menus
- [x] Gate quick FE: `npm test -- MeuDashboard.test.tsx` (shell render)

**Tests**: unit  
**Gate**: quick FE  
**Commit**: `feat(dashboard): add Meu Dashboard read-only shell with default layout`

---

### T10: Remover chips de variação falsos do Dashboard clássico

**What**: Remover `<Chip label="+2.5% este mês">`, `+5.2%`, `Estável` de `Dashboard/index.tsx`; atualizar testes.  
**Where**: `frontend/src/pages/Dashboard/index.tsx`, `Dashboard.test.tsx`  
**Depends on**: None (paralelizável após T8 visual reference — sequencial na Phase 2)  
**Reuses**: —  
**Requirements**: DASHC-06

**Done when**:
- [x] Strings hardcoded ausentes do código (`grep` zero matches)
- [x] Teste asserta ausência de chip de variação nos KPIs
- [x] Gate: `npm test -- Dashboard.test`

**Tests**: unit  
**Gate**: quick FE  
**Commit**: `fix(dashboard): remove fake KPI variation chips from classic dashboard`

---

### T11: Hook `useDashboardLayout` + cache localStorage

**What**: Hook carrega layout+catálogo; draft em edição; PUT no Salvar; Cancelar restaura último salvo; cache otimista em `storage.ts`.  
**Where**:
- `frontend/src/pages/MeuDashboard/hooks/useDashboardLayout.ts`
- `frontend/src/pages/MeuDashboard/storage.ts`

**Depends on**: T9  
**Reuses**: Padrão `theme/storage.ts`  
**Requirements**: DASHC-19…23, DASHC-27 (FE ignore unknown)

**Done when**:
- [x] Salvar chama PUT e sai do modo edição em sucesso
- [x] Falha PUT mantém draft + notification error
- [x] Cancelar descarta draft
- [x] Gate quick FE: `npm test -- useDashboardLayout.test`

**Tests**: unit (MSW ou mock service)  
**Gate**: quick FE  
**Commit**: `feat(dashboard): add useDashboardLayout hook with explicit save and local cache`

---

### T12: `DashboardGrid` com dnd-kit sortable

**What**: Grid 12 colunas; DnD só em editMode; presets P/M/G/Full; keyboard reorder; responsivo md→span 12.  
**Where**: `frontend/src/pages/MeuDashboard/DashboardGrid.tsx`  
**Depends on**: T11  
**Reuses**: Sensors de `Organograma/index.tsx` (PointerSensor distance 8 + KeyboardSensor)  
**Requirements**: DASHC-07…09, DASHC-12

**Done when**:
- [x] Arrastar reordena widgets e atualiza `ordem`
- [x] Teclado reordena sem mouse (mock `@dnd-kit` ou integração)
- [x] Viewport estreita empilha sem mutar layout salvo
- [x] Gate quick FE: `npm test -- DashboardGrid.test`

**Tests**: unit  
**Gate**: quick FE  
**Commit**: `feat(dashboard): add sortable DashboardGrid with width presets`

---

### T13: `WidgetCatalogDrawer` + add/remove + empty state

**What**: Drawer lista catálogo do servidor; adicionar ao final; marcar já adicionados (Fase 1 sem duplicata); remover widget; estado vazio com CTA.  
**Where**:
- `frontend/src/pages/MeuDashboard/WidgetCatalogDrawer.tsx`
- Integração em `MeuDashboard/index.tsx`

**Depends on**: T12  
**Requirements**: DASHC-10, DASHC-13…18

**Done when**:
- [x] Adicionar insere widget com `colSpanPadrao` do catálogo
- [x] Widget presente marcado indisponível para duplicata (Fase 1)
- [x] Layout vazio mostra mensagem + ações (DASHC-13)
- [x] Limite 30 bloqueia add com mensagem

**Tests**: unit (drawer + add/remove flows)  
**Gate**: quick FE  
**Commit**: `feat(dashboard): add widget catalog drawer with add/remove flows`

---

### T14: Toolbar edição + rota + menu gate

**What**: Botões Salvar/Cancelar/Restaurar padrão; `beforeunload` quando dirty; rota `/meu-dashboard`; `DashboardCustomRoute`; item menu condicional; helper `podeAcessarMeuDashboard`.  
**Where**:
- `frontend/src/pages/MeuDashboard/index.tsx` (toolbar)
- `frontend/src/routes/DashboardCustomRoute.tsx`
- `frontend/src/utils/dashboardAccess.ts`
- `frontend/src/routes/index.tsx`, `frontend/src/components/Layout/index.tsx`

**Depends on**: T13  
**Requirements**: DASHC-03…05, DASHC-19…22

**Done when**:
- [x] Dois itens menu: "Dashboard" e "Meu Dashboard" (quando permitido)
- [x] Usuário sem escopo: menu oculto + rota redireciona/403
- [x] Restaurar padrão confirma e recarrega 11 widgets
- [x] Gate: `npm test -- dashboardAccess` + build

**Tests**: unit (`dashboardAccess.test.ts` + route test)  
**Gate**: build  
**Commit**: `feat(dashboard): wire Meu Dashboard route, menu gate, and edit toolbar`

---

### T15: Testes Vitest — cobertura Fase 1

**What**: Consolidar/ampliar testes unitários MeuDashboard: paridade render, edit flows, ACL helper, widget unknown id ignored.  
**Where**: `frontend/src/pages/MeuDashboard/**/*.test.tsx`, `dashboardAccess.test.ts`  
**Depends on**: T14  
**Requirements**: DASHC-01…18, DASHC-21…23, DASHC-27

**Done when**:
- [x] Cobertura dos ACs Fase 1 mapeados na matriz
- [x] Gate quick FE: `npm test -- src/pages/MeuDashboard src/utils/dashboardAccess`

**Tests**: unit (consolidação — não task separada de prod code)  
**Gate**: quick FE  
**Commit**: `test(dashboard): add Meu Dashboard unit test coverage for phase 1 ACs`

---

### T16: E2E Playwright — persistência de layout

**What**: Spec E2E: login mock → abrir Meu Dashboard → remover widget → salvar → reload → layout persistido (mock API ou preview com MSW routes).  
**Where**: `frontend/e2e/meu-dashboard.spec.ts`  
**Depends on**: T15  
**Reuses**: Padrão `e2e/login.spec.ts` (`page.route()`)  
**Requirements**: DASHC-20

**Done when**:
- [x] E2E passa localmente: `npm run test:e2e -- e2e/meu-dashboard.spec.ts`
- [x] Fluxo: edit → save → reload → widget removido permanece ausente

**Tests**: e2e  
**Gate**: full  
**Commit**: `test(dashboard): add Playwright E2E for layout persistence`

---

### T17: `DashboardWidgetQueryService` + params whitelist

**What**: Service Fase 2: resolve dado por `widgetId` + query params validados; ACL reaplicada; `semDados` quando competência vazia.  
**Where**:
- `backend/.../dashboard/application/DashboardWidgetQueryService.java`
- `backend/.../dashboard/api/WidgetQueryParams.java`, `WidgetDataDTO.java`

**Depends on**: T6  
**Reuses**: `DashboardStatsAggregator`, `DashboardAccessGuard`, `FolhaConsultaPort`  
**Requirements**: DASHC-28…31, DASHC-35…36, DASHC-40…43

**Done when**:
- [x] Params fora da whitelist → 400
- [x] CC/LN fora do escopo → negado ou filtrado
- [x] Competência sem folha → `semDados=true`
- [x] Gate quick: `mvn test -Dtest=DashboardWidgetQueryServiceTest`

**Tests**: unit (ACL scoped/total, whitelist, semDados, topN bounds)  
**Gate**: quick  
**Commit**: `feat(dashboard): add per-widget query service with ACL and param validation`

---

### T18: `DashboardWidgetController` + WebMvcTest ACL

**What**: `GET /dashboard/widgets/{widgetId}/data`; testes ACL por endpoint (scoped vs total vs denied).  
**Where**:
- `backend/.../dashboard/api/DashboardWidgetController.java`
- `backend/src/test/java/.../dashboard/api/DashboardWidgetControllerWebMvcTest.java`

**Depends on**: T17, T19  
**Reuses**: Padrão ACL tests de `FuncionarioAclWebMvcTest`  
**Requirements**: DASHC-41…44

**Done when**:
- [ ] Chamada direta a widget não no catálogo do usuário → negada
- [ ] Escopo restrito não retorna CC fora do conjunto
- [ ] `GET /dashboard/stats` inalterado (regressão zero)
- [ ] Gate full: `mvn test -Dtest=DashboardWidgetControllerWebMvcTest`

**Tests**: WebMvcTest  
**Gate**: full (backend)  
**Commit**: `feat(dashboard): expose per-widget data endpoint with ACL WebMvc tests`

---

### T19: Slices no `DashboardStatsAggregator` para widgets

**What**: Métodos focados (KPI individual, distribuição com topN, evolução N meses, top rubricas) reutilizados por `DashboardWidgetQueryService`.  
**Where**: `backend/.../dashboard/application/DashboardStatsAggregator.java`  
**Depends on**: T17 (pode ser feito em paralelo lógico — **sequencial**: T17 stubs first, T19 fills, T18 after both)  

**Reorder**: T19 before T18 — T18 depends on T17+T19. Update execution plan.

**Depends on**: T17  
**Requirements**: DASHC-32…34, DASHC-40

**Done when**:
- [ ] Aggregator expõe slices sem duplicar SQL das agregações existentes
- [ ] `getStats` monolítico continua usando os mesmos métodos internos
- [ ] Gate quick: tests aggregator + query service green

**Tests**: unit (aggregator slice tests ou via QueryServiceTest)  
**Gate**: quick  
**Commit**: `refactor(dashboard): add widget-focused slices to DashboardStatsAggregator`

---

### T20: react-query local + `useWidgetData`

**What**: `QueryClientProvider` scoped em `MeuDashboard`; `dashboardWidgetService.ts`; hook `useWidgetData(widgetId, config, competenciaGlobal)`.  
**Where**:
- `frontend/src/services/dashboardWidgetService.ts`
- `frontend/src/pages/MeuDashboard/hooks/useWidgetData.ts`
- `frontend/src/pages/MeuDashboard/index.tsx` (provider wrapper)

**Depends on**: T18  
**Reuses**: `@tanstack/react-query` 5.x (já instalado)  
**Requirements**: DASHC-40

**Done when**:
- [ ] Widget visível dispara fetch; widget ausente do layout não fetcha
- [ ] `staleTime` 5min por chave `[widgetId, instanceId, config, competencia]`
- [ ] Erro isolado no widget (error boundary ou state local)

**Tests**: unit (hook com QueryClientProvider test wrapper + MSW)  
**Gate**: quick FE  
**Commit**: `feat(dashboard): add per-widget data fetching with react-query`

---

### T21: Seletor global de competência (sessão)

**What**: `CompetenciaSelector` no topo; estado sessão (`useState` + optional `sessionStorage`); widgets sem override usam global.  
**Where**: `frontend/src/pages/MeuDashboard/CompetenciaSelector.tsx`  
**Depends on**: T20  
**Requirements**: DASHC-28, DASHC-31

**Done when**:
- [ ] Selecionar competência refetch widgets sem override
- [ ] Default = folha mais recente (comportamento atual)
- [ ] Competência global **não** incluída no PUT layout

**Tests**: unit  
**Gate**: quick FE  
**Commit**: `feat(dashboard): add session-scoped global competencia selector`

---

### T22: `WidgetConfigPanel` + persistência de config

**What**: Painel por widget: topN, dimensão, métrica, tipo visualização, competência fixa; validação FE; salva via PUT layout (`config` JSONB).  
**Where**: `frontend/src/pages/MeuDashboard/WidgetConfigPanel.tsx`, `WidgetFrame.tsx`  
**Depends on**: T21  
**Requirements**: DASHC-29, DASHC-32…35

**Done when**:
- [ ] Override competência persiste após reload
- [ ] topN=10 mostra 7º CC (DASHC-32 acceptance scenario)
- [ ] Config inválida bloqueada antes do save

**Tests**: unit  
**Gate**: quick FE  
**Commit**: `feat(dashboard): add per-widget configuration panel with persisted config`

---

### T23: Múltiplas instâncias do mesmo widget

**What**: Fase 2: catálogo permite adicionar mesmo `widgetId` com novo `instanceId`; grid renderiza instâncias independentes.  
**Where**: `WidgetCatalogDrawer.tsx`, `useDashboardLayout.ts`, `DashboardGrid.tsx`  
**Depends on**: T22  
**Requirements**: DASHC-37…39

**Done when**:
- [ ] Duas instâncias mesmo widget com configs distintas renderizam dados distintos
- [ ] Remover uma instância não afeta a outra

**Tests**: unit  
**Gate**: quick FE  
**Commit**: `feat(dashboard): support multiple widget instances with independent config`

---

### T24: Migrar widgets para fetch individual + estados vazios/erro

**What**: Registry/widgets usam `useWidgetData` em vez de `stats` prop; empty state `semDados`; error retry por widget; remover fetch monolítico `/stats` da página Meu Dashboard.  
**Where**: `frontend/src/pages/MeuDashboard/widgets/*`, `index.tsx`  
**Depends on**: T23  
**Requirements**: DASHC-30, DASHC-40, DASHC-44 (FE side)

**Done when**:
- [ ] Página Meu Dashboard não chama `getDashboardStats()` 
- [ ] Competência sem dados mostra empty state explícito (não zeros)
- [ ] Gate release: `cd backend && mvn test && cd ../frontend && npm run lint && npm test && npm run build`

**Tests**: unit (empty/error states)  
**Gate**: release  
**Commit**: `feat(dashboard): switch Meu Dashboard to per-widget data with isolated error states`

---

## Phase Execution Map

```
Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4 ──→ T5 ──→ T6
Phase 2:  T7 ──→ T8 ──→ T9 ──→ T10
Phase 3:  T11 ──→ T12 ──→ T13 ──→ T14
Phase 4:  T15 ──→ T16
Phase 5:  T17 ──→ T19 ──→ T18
Phase 6:  T20 ──→ T21 ──→ T22 ──→ T23 ──→ T24
```

Execution is strictly sequential within each phase. Phase 5 order: T19 before T18 (aggregator slices before controller integration tests).

---

## Requirement Traceability (Tasks → DASHC)

| Requirement | Task(s) |
| ----------- | ------- |
| DASHC-01…02 | T5, T8, T9, T15 |
| DASHC-03…05 | T3, T6, T14, T15 |
| DASHC-06 | T8, T10, T15 |
| DASHC-07…13 | T12, T13, T15 |
| DASHC-14…18 | T4, T6, T7, T13, T15 |
| DASHC-19…27 | T5, T6, T11, T14, T15, T16 |
| DASHC-28…31 | T17, T21, T24 |
| DASHC-32…36 | T17, T19, T22, T24 |
| DASHC-37…39 | T23 |
| DASHC-40…44 | T17, T18, T19, T20, T24 |

**Coverage:** 44/44 mapped ✅

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Flyway migration | 1 migration file | ✅ Granular |
| T2: Entity + repository | 1 domain + 1 infra | ✅ Granular |
| T3: AccessGuard + refactor | 1 service + exception + handler touch | ✅ Granular |
| T4: Widget catalog service | 1 registry + 1 service | ✅ Granular |
| T5: LayoutService | 1 service | ✅ Granular |
| T6: LayoutController + WebMvcTest | 1 controller + tests | ✅ Granular |
| T7: FE API client | 1 service + types | ✅ Granular |
| T8: Widget components + registry | 1 pasta widgets (cohesive) | ✅ Granular |
| T9: Page shell | 1 page + frame | ✅ Granular |
| T10: Remove fake chips | 1 file change | ✅ Granular |
| T11: useDashboardLayout hook | 1 hook + storage | ✅ Granular |
| T12: DashboardGrid DnD | 1 component | ✅ Granular |
| T13: Catalog drawer | 1 component | ✅ Granular |
| T14: Route + menu + toolbar | route + layout touch | ✅ Granular |
| T15: Vitest consolidation | tests only | ✅ Granular |
| T16: Playwright E2E | 1 spec file | ✅ Granular |
| T17: WidgetQueryService | 1 service | ✅ Granular |
| T18: WidgetController WebMvc | 1 controller + tests | ✅ Granular |
| T19: Aggregator slices | 1 class extend | ✅ Granular |
| T20: react-query hook | 1 hook + service | ✅ Granular |
| T21: CompetenciaSelector | 1 component | ✅ Granular |
| T22: WidgetConfigPanel | 1 component | ✅ Granular |
| T23: Multi-instance | 3 files cohesive | ✅ Granular |
| T24: Per-widget fetch migration | widgets + page | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ----------------- | ------------- | ------ |
| T1 | None | entry | ✅ |
| T2 | T1 | T1→T2 | ✅ |
| T3 | T2 | T2→T3 | ✅ |
| T4 | T3 | T3→T4 | ✅ |
| T5 | T4 | T4→T5 | ✅ |
| T6 | T5 | T5→T6 | ✅ |
| T7 | T6 | T6→T7 | ✅ |
| T8 | T7 | T7→T8 | ✅ |
| T9 | T8 | T8→T9 | ✅ |
| T10 | None* | parallel in phase | ✅ (*no backward dep) |
| T11 | T9 | T9→T11 | ✅ |
| T12 | T11 | T11→T12 | ✅ |
| T13 | T12 | T12→T13 | ✅ |
| T14 | T13 | T13→T14 | ✅ |
| T15 | T14 | T14→T15 | ✅ |
| T16 | T15 | T15→T16 | ✅ |
| T17 | T6 | T6→T17 | ✅ |
| T19 | T17 | T17→T19 | ✅ |
| T18 | T17, T19 | T19→T18 | ✅ |
| T20 | T18 | T18→T20 | ✅ |
| T21 | T20 | T20→T21 | ✅ |
| T22 | T21 | T21→T22 | ✅ |
| T23 | T22 | T22→T23 | ✅ |
| T24 | T23 | T23→T24 | ✅ |

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | Flyway | none | none | ✅ |
| T2 | Entity/repo | none | none | ✅ |
| T3 | AccessGuard service | unit | unit | ✅ |
| T4 | CatalogService | unit | unit | ✅ |
| T5 | LayoutService | unit | unit | ✅ |
| T6 | LayoutController | WebMvcTest | WebMvcTest | ✅ |
| T7 | FE service | none | none | ✅ |
| T8 | Widget components | unit | unit | ✅ |
| T9 | Page shell | unit | unit | ✅ |
| T10 | Dashboard classic | unit | unit | ✅ |
| T11 | Hook | unit | unit | ✅ |
| T12 | Grid | unit | unit | ✅ |
| T13 | Drawer | unit | unit | ✅ |
| T14 | Route/utils | unit | unit | ✅ |
| T15 | Tests consolidation | unit | unit | ✅ |
| T16 | E2E | e2e | e2e | ✅ |
| T17 | QueryService | unit | unit | ✅ |
| T18 | WidgetController | WebMvcTest | WebMvcTest | ✅ |
| T19 | Aggregator | unit | unit | ✅ |
| T20 | useWidgetData | unit | unit | ✅ |
| T21 | CompetenciaSelector | unit | unit | ✅ |
| T22 | ConfigPanel | unit | unit | ✅ |
| T23 | Multi-instance | unit | unit | ✅ |
| T24 | Widget migration | unit | unit | ✅ |

---

## MCPs & Skills (Execute — confirm with user)

| Task range | Recommended Skills | MCPs |
| ---------- | ------------------ | ---- |
| T1–T2 | `flyway-migration-writer`, `jpa-performance` | — |
| T3–T6 | `spring-boot-new-endpoint`, `spring-security`, `jpa-performance` | SonarQube (optional post-batch) |
| T7–T16 | `component-architecture`, `forms-validation`, `testing-a11y`, `routing-perf` | — |
| T17–T19 | `spring-boot-new-endpoint`, `jpa-performance`, `spring-security` | — |
| T20–T24 | `api-client`, `testing-a11y`, `component-architecture` | Context7 (react-query docs if needed) |

---

## Task Verification Standards

Every task: **one atomic commit**, gate must pass, test count must not silently decrease. Verifier runs automatically after T24 (author ≠ verifier).
