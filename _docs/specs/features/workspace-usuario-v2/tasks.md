# Workspace do Usuário v2 — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/workspace-usuario-v2/design.md`  
**Spec**: `_docs/specs/features/workspace-usuario-v2/spec.md`  
**Baseline**: `_docs/specs/features/workspace-usuario/spec.md` (WKS-01…32 congelado — regressão = bug v1)  
**Status**: Draft — aguardando aprovação  
**Constraints**: AD-004 (brownfield `pages/Workspace/`), AD-008 (domínio `workspace.*` additive only), AD-011 (ACL), AD-013 (IA/MCP inalterado)  
**Pré-requisito Execute**: v1 `workspace-usuario` merged; suite WKS-* verde em `main`

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `frontend/AGENTS.md`, `backend/AGENTS.md`, `.agents/skills/testing-a11y/SKILL.md`, `.agents/skills/spring-security/SKILL.md`, `workspace-usuario-v2/spec.md` (WKS2-01…37 ACs + edge cases; **sem E2E novo** — spec Out of Scope).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Workspace v2 application (novos métodos `DatasetAuditService.listarHistoricoDataset`, `WidgetQueryService.preview`) | unit (Mockito) | Branches dos métodos novos; 1:1 a WKS2-18/19/28/26; edge ACL negado | `backend/src/test/java/**/workspace/application/*Test.java` | `cd backend && mvn test -Dtest='workspace.application.*Test'` |
| Workspace v2 API (4 endpoints finos: validate, preview, audit agregado, versions) | WebMvcTest | Happy + ACL 403 + validation 400 + dataset/template inexistente 404 | `backend/src/test/java/**/workspace/api/*WebMvcTest.java` | `cd backend && mvn test -Dtest='FormulaValidationControllerWebMvcTest,WidgetDefinitionControllerWebMvcTest,DatasetControllerWebMvcTest,TemplateControllerWebMvcTest'` |
| Workspace v2 domain/infrastructure (DTOs, entities) | none | — (build gate only) | `workspace/api/dto/`, `workspace/application/` | compile gate |
| Frontend shared (`workspaceTheme`, `workspaceLimits`, shell/chips/banners/quota) | unit (Vitest + Testing Library) | Render por role/label; tokens aplicados; limites espelham `WorkspaceLimits.java` | `frontend/src/pages/Workspace/**/*.test.tsx` | `cd frontend && npm test -- src/pages/Workspace` |
| Frontend hooks (`useUnsavedChangesGuard`) | unit | dirty=true bloqueia navigate; beforeunload registrado | `frontend/src/pages/Workspace/hooks/*.test.ts` | `cd frontend && npm test -- useUnsavedChangesGuard` |
| Frontend pages (hub, detail, dataset refactor, builder, publish, upgrade, history, IA) | unit (Vitest + Testing Library) | 1:1 a WKS2 ACs por tela; queries por role/label; edge cases spec (erro parcial hub, quota, empty states) | `frontend/src/pages/Workspace/**/*.test.tsx` | `cd frontend && npm test -- src/pages/Workspace` |
| Layout sub-nav | unit | WKS2-05…07: seção Meu Workspace + 3 sub-itens + highlight rota filha | `frontend/src/components/Layout/Layout.test.tsx` | `cd frontend && npm test -- Layout.test` |
| Routes (ordem estática vs `:workspaceId`) | unit | Rotas literais não capturadas como id; redirect 404 id inválido | `frontend/src/routes/WorkspaceRoute.test.tsx` | `cd frontend && npm test -- WorkspaceRoute` |
| Frontend service (`workspaceService` métodos v2) | unit | Chamadas corretas aos 4 endpoints; tipos | `frontend/src/services/workspaceService.test.ts` | `cd frontend && npm test -- workspaceService` |
| Regressão v1 workspace | unit (BE+FE) | Suite WKS-* permanece verde | patterns acima | release gate abaixo |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick BE | After T15–T18 (application + WebMvcTest) | `cd backend && mvn test -Dtest='workspace.application.*Test,FormulaValidationControllerWebMvcTest,WidgetDefinitionControllerWebMvcTest,DatasetControllerWebMvcTest,TemplateControllerWebMvcTest'` |
| Quick FE | After frontend unit tasks | `cd frontend && npm test -- src/pages/Workspace src/components/Layout/Layout.test.tsx src/routes/WorkspaceRoute.test.tsx src/services/workspaceService.test.ts src/utils/workspaceAccess.test.ts` |
| Build | After theme/limits-only tasks (T1, T6) | `cd frontend && npm run build` |
| Full API | After batch BE fino (T15–T18) | `cd backend && mvn test -Dtest='workspace.api.*WebMvcTest'` |
| Release | After each phase batch + T28 final | `cd backend && mvn test && cd ../frontend && npm run lint && npm test && npm run build` |
| Regressão v1 | Antes do Verifier final | `cd backend && mvn test -Dtest='workspace.application.*Test,workspace.api.*WebMvcTest' && cd ../frontend && npm test -- src/pages/Workspace` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Foundation — theme, shell, quotas

Tokens Techne brand e componentes compartilhados reutilizados por todas as páginas v2.

```
T1 → T2 → T3 → T4 → T5 → T6
```

### Phase 2: Navigation & guards

Sub-nav lateral e proteção de alterações não salvas.

```
T7 → T8
```

### Phase 3: Hub & workspace detail (P1)

Split hub vs. grid; badges e empty states.

```
T9 → T10 → T11 → T12
```

### Phase 4: Dataset editor UX (P1)

Layout mockup telas 02–03.

```
T13 → T14
```

### Phase 5: Backend fino (additive)

Quatro endpoints para validate, preview, audit agregado e versões de template.

```
T15 → T16 → T17 → T18
```

### Phase 6: P2 flows (FE)

Widget builder, catálogo, publicação, upgrade, histórico.

```
T19 → T20 → T21 → T22 → T23 → T24
```

### Phase 7: P3 — IA, visual polish, cleanup

Páginas IA, banners de erro, migração de testes v1.

```
T25 → T26 → T27 → T28
```

---

## Task Breakdown

### T1: workspaceTheme.ts — tokens Techne brand

**What**: Exportar paleta e variantes de chip/banner (`colors`, `chipVariants`, `bannerVariants`) espelhando `gen_mockups_workspace.py`.  
**Where**: `frontend/src/pages/Workspace/workspaceTheme.ts`  
**Depends on**: None  
**Reuses**: Cores do mockup PDF (`#20284E`, `#7836FC`, `#EFF2F7`, `#DCE2EE`)  
**Requirement**: WKS2-35

**Tools**:

- MCP: NONE
- Skill: `component-architecture`

**Done when**:

- [x] Constantes exportadas e tipadas (`as const`)
- [x] Variantes semânticas: info, ok, warn, danger, ai
- [x] Gate check passes: `cd frontend && npm run build`

**Tests**: none  
**Gate**: build

---

### T2: WorkspacePageShell component

**What**: Shell de página com header (título, subtítulo, actions) e fundo `#EFF2F7`.  
**Where**: `frontend/src/pages/Workspace/components/WorkspacePageShell.tsx`  
**Depends on**: T1  
**Reuses**: `workspaceTheme.ts`  
**Requirement**: WKS2-35

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [x] Renderiza `title`, `subtitle`, `actions`, `children`
- [x] Aplica tokens de T1 via `sx`
- [x] Gate check passes: `cd frontend && npm test -- WorkspacePageShell`
- [x] Test count: ≥3 tests pass (render, actions slot, a11y heading)

**Tests**: unit  
**Gate**: quick FE

---

### T3: StatusChip component

**What**: Chip semântico (info, ok, warn, danger, ai) para quotas, versões, origem, IA.  
**Where**: `frontend/src/pages/Workspace/components/StatusChip.tsx`  
**Depends on**: T1  
**Reuses**: `workspaceTheme.chipVariants`  
**Requirement**: WKS2-36

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [x] Variantes renderizam texto e cor corretos
- [x] Gate check passes: `cd frontend && npm test -- StatusChip`
- [x] Test count: ≥5 tests pass (uma por variante)

**Tests**: unit  
**Gate**: quick FE

---

### T4: InfoBanner component

**What**: Banner isolado para avisos/erros sem derrubar página inteira.  
**Where**: `frontend/src/pages/Workspace/components/InfoBanner.tsx`  
**Depends on**: T1  
**Reuses**: `workspaceTheme.bannerVariants`  
**Requirement**: WKS2-36, WKS2-37

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [x] Variantes info/warn/danger/ai com `role="alert"` ou `role="status"` conforme severidade
- [x] Gate check passes: `cd frontend && npm test -- InfoBanner`
- [x] Test count: ≥4 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T5: QuotaProgressBar component

**What**: Barra de progresso "N de M" com tooltip ao atingir limite.  
**Where**: `frontend/src/pages/Workspace/components/QuotaProgressBar.tsx`  
**Depends on**: T1  
**Reuses**: `StatusChip` para estado warn/danger opcional  
**Requirement**: WKS2-04, WKS2-14

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [x] Exibe label, contagem e barra proporcional
- [x] 100% desabilita visualmente com tooltip explicativo
- [x] Gate check passes: `cd frontend && npm test -- QuotaProgressBar`
- [x] Test count: ≥3 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T6: workspaceLimits.ts — espelho FE de WorkspaceLimits.java

**What**: Constantes de quota + teste espelho que documenta paridade com backend.  
**Where**: `frontend/src/pages/Workspace/workspaceLimits.ts`, `workspaceLimits.test.ts`  
**Depends on**: None  
**Reuses**: `backend/.../domain/WorkspaceLimits.java` (comentário sync)  
**Requirement**: WKS2-04, WKS2-14

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `WORKSPACE_LIMITS` exportado com todos os limites v1
- [x] Teste compara valores com documentação v1 / Java source
- [x] Gate check passes: `cd frontend && npm test -- workspaceLimits`

**Tests**: unit  
**Gate**: quick FE

---

### T7: Layout — seção Meu Workspace com sub-nav

**What**: Substituir item único "Workspace" por Collapse **Meu Workspace** com 3 sub-itens (padrão Cadastros).  
**Where**: `frontend/src/components/Layout/index.tsx`, `Layout.test.tsx`  
**Depends on**: None  
**Reuses**: Padrão Cadastros Collapse (`Layout/index.tsx:144–166`), `podeAcessarWorkspace`  
**Requirement**: WKS2-05, WKS2-06, WKS2-07

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [x] Sub-itens: Meus workspaces (`/workspace`), Meus dados (`/workspace/datasets`), Catálogo (`/workspace/templates`)
- [x] Highlight correto em rotas filhas (`/workspace/:id`, `/workspace/datasets/:id`, etc.)
- [x] Gate check passes: `cd frontend && npm test -- Layout.test`
- [x] Test count: Layout tests atualizados (≥2 asserts novos para sub-nav)

**Tests**: unit  
**Gate**: quick FE

---

### T8: useUnsavedChangesGuard hook

**What**: Hook `beforeunload` + `useBlocker` (React Router v7) para layout/dataset dirty.  
**Where**: `frontend/src/pages/Workspace/hooks/useUnsavedChangesGuard.ts`, `useUnsavedChangesGuard.test.ts`  
**Depends on**: None  
**Reuses**: Padrão React Router v7 blocker  
**Requirement**: WKS2-09

**Tools**:

- MCP: NONE
- Skill: `routing-perf`, `testing-a11y`

**Done when**:

- [x] `dirty=true` dispara confirm dialog ao navegar
- [x] `beforeunload` registrado quando dirty
- [x] Gate check passes: `cd frontend && npm test -- useUnsavedChangesGuard`
- [x] Test count: ≥3 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T9: WorkspaceHubPage

**What**: Página hub com cards de workspaces + tabela resumo de datasets + quotas.  
**Where**: `frontend/src/pages/Workspace/WorkspaceHubPage.tsx`, `WorkspaceHubPage.test.tsx`  
**Depends on**: T2, T4, T5, T6  
**Reuses**: `listWorkspaces`, `listDatasets`, `listWidgetDefinitions`, `createWorkspace`, `WorkspacePageShell`, `QuotaProgressBar`  
**Requirement**: WKS2-01, WKS2-02, WKS2-04

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`, `api-client`

**Done when**:

- [x] Cards com nome, widgets, última edição, ação Abrir e Novo workspace
- [x] Tabela datasets com colunas spec; erro parcial se datasets falham (workspaces OK)
- [x] Quota datasets visível antes de criar
- [x] Gate check passes: `cd frontend && npm test -- WorkspaceHubPage`
- [x] Test count: ≥6 tests pass (cards, tabela, quota, erro parcial, empty, navigate Abrir)

**Tests**: unit  
**Gate**: quick FE

---

### T10: WorkspaceDetailPage — grid + toolbar edição

**What**: Extrair grid/toolbar de `WorkspacePage` para página dedicada `/workspace/:workspaceId`.  
**Where**: `frontend/src/pages/Workspace/WorkspaceDetailPage.tsx`, `WorkspaceDetailPage.test.tsx`  
**Depends on**: T2, T8  
**Reuses**: `useWorkspaceLayout`, `WorkspaceGrid`, `useUnsavedChangesGuard`, `WorkspacePageShell`  
**Requirement**: WKS2-08, WKS2-09

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`, `routing-perf`

**Done when**:

- [x] Toolbar: nome, contagem widgets, Adicionar widget, Instalar template, toggle Editar/Salvar/Cancelar
- [x] Modo edição exibe chip "editando"; dirty guard ativo
- [x] 404/acesso negado para id inválido
- [x] Gate check passes: `cd frontend && npm test -- WorkspaceDetailPage`
- [x] Test count: ≥5 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T11: SourceBadge + empty state no detail

**What**: Badge DATASET/SISTEMA no widget frame; empty state orientativo no workspace vazio.  
**Where**: `frontend/src/pages/Workspace/components/SourceBadge.tsx`, alterações em `WorkspaceGrid.tsx` / `WorkspaceDetailPage.tsx`  
**Depends on**: T10, T3  
**Reuses**: `WidgetFrame`, metadado `fontes` de definição  
**Requirement**: WKS2-10, WKS2-11

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [x] Badge visível por origem DATASET vs SISTEMA
- [x] Empty state com atalhos adicionar widget / instalar template
- [x] Gate check passes: `cd frontend && npm test -- SourceBadge WorkspaceDetailPage`
- [x] Test count: ≥4 tests pass (badge types, empty state)

**Tests**: unit  
**Gate**: quick FE

---

### T12: Routes — split hub/detail + ordem estática

**What**: Registrar rotas v2; literais antes de `:workspaceId`; hub em `/workspace`, detail em `/workspace/:workspaceId`.  
**Where**: `frontend/src/routes/index.tsx`, `WorkspaceRoute.test.tsx`; deprecar corpo monolítico de `WorkspacePage.tsx` (re-export ou redirect)  
**Depends on**: T9, T10, T11  
**Reuses**: `WorkspaceRoute`, lazy imports existentes  
**Requirement**: WKS2-03

**Tools**:

- MCP: NONE
- Skill: `routing-perf`, `testing-a11y`

**Done when**:

- [x] `/workspace` → `WorkspaceHubPage`; `/workspace/:workspaceId` → `WorkspaceDetailPage`
- [x] `/workspace/datasets` não capturado como `:workspaceId`
- [x] Gate check passes: `cd frontend && npm test -- WorkspaceRoute`
- [x] Test count: route tests ≥3 pass

**Tests**: unit  
**Gate**: quick FE

**Commit**: `feat(workspace-v2): split hub and detail routes`

---

### T13: DatasetEditorPage — schema builder + FieldTypePanel

**What**: Painel de campos (Campo, Tipo, Obrigatório, Observação) + painel lateral de tipos + confirmação remoção campo.  
**Where**: `frontend/src/pages/Workspace/DatasetEditorPage.tsx`, `components/FieldTypePanel.tsx`, tests  
**Depends on**: T2, T3  
**Reuses**: Lógica schema existente; link "Histórico" placeholder até T24  
**Requirement**: WKS2-12, WKS2-13, WKS2-16

**Tools**:

- MCP: NONE
- Skill: `forms-validation`, `component-architecture`, `testing-a11y`

**Done when**:

- [x] Colunas schema conforme mockup; "+ Adicionar campo"
- [x] Painel lateral Texto/Número/Data/Moeda/Referência com texto explicativo Referência
- [x] Dialog confirmação ao remover campo com dados
- [x] Gate check passes: `cd frontend && npm test -- DatasetEditorPage`
- [x] Test count: ≥5 tests pass (schema, tipos, confirmação remoção)

**Tests**: unit  
**Gate**: quick FE

---

### T14: DatasetEditorPage — quotas + InlineCellError

**What**: Barras quota dataset/linhas; erro inline por célula inválida; componente `InlineCellError`.  
**Where**: `DatasetEditorPage.tsx`, `components/InlineCellError.tsx`, tests  
**Depends on**: T5, T6, T13  
**Reuses**: `QuotaProgressBar`, validação tipo existente v1  
**Requirement**: WKS2-14, WKS2-15, WKS2-37 (partial)

**Tools**:

- MCP: NONE
- Skill: `forms-validation`, `testing-a11y`

**Done when**:

- [x] Barras "N de M" datasets e linhas visíveis
- [x] Célula inválida destacada com mensagem inline (linha válida intacta)
- [x] Gate check passes: `cd frontend && npm test -- DatasetEditorPage InlineCellError`
- [x] Test count: ≥4 tests pass (quota display, inline error)

**Tests**: unit  
**Gate**: quick FE

---

### T15: POST /workspace/formulas/validate

**What**: Endpoint validação fórmula sem persistir; delega `FormulaEngine.validate`.  
**Where**: `backend/.../workspace/api/FormulaValidationController.java`, DTOs, `FormulaValidationControllerWebMvcTest.java`  
**Depends on**: None  
**Reuses**: `FormulaEngine`, `WidgetDefinitionService.buildAvailableFields`  
**Requirement**: WKS2-18

**Tools**:

- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `spring-security`, `jpa-performance`

**Done when**:

- [ ] `POST /workspace/formulas/validate` retorna `{ valid, errors[] }`
- [ ] ACL 403 fora de escopo; 400 fórmula inválida
- [ ] Gate check passes: `cd backend && mvn test -Dtest=FormulaValidationControllerWebMvcTest`
- [ ] Test count: WebMvcTest ≥4 pass

**Tests**: integration (WebMvcTest)  
**Gate**: quick BE

**Commit**: `feat(workspace): add formula validate endpoint`

---

### T16: POST /workspace/widget-definitions/preview

**What**: Preview widget sem persistir; `WidgetQueryService.preview(login, request)`.  
**Where**: `WidgetDefinitionController` (método preview), service, `WidgetDefinitionControllerWebMvcTest.java`  
**Depends on**: None  
**Reuses**: `WidgetQueryService`, `CreateWidgetDefinitionRequest`  
**Requirement**: WKS2-19

**Tools**:

- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `spring-security`

**Done when**:

- [ ] Preview retorna `WorkspaceWidgetDataDTO` formatado
- [ ] Nada persistido no banco após preview
- [ ] Gate check passes: `cd backend && mvn test -Dtest=WidgetDefinitionControllerWebMvcTest`
- [ ] Test count: preview tests ≥3 pass

**Tests**: integration (WebMvcTest)  
**Gate**: quick BE

---

### T17: GET /workspace/datasets/{id}/audit — timeline agregada

**What**: `DatasetAuditService.listarHistoricoDataset` + endpoint timeline.  
**Where**: `DatasetController`, `DatasetAuditService`, DTO, `DatasetControllerWebMvcTest.java`  
**Depends on**: None  
**Reuses**: Audit por linha existente  
**Requirement**: WKS2-28

**Tools**:

- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `jpa-performance`, `spring-security`

**Done when**:

- [ ] Lista cronológica CREATE/UPDATE/DELETE com autor e timestamp
- [ ] 404 dataset inexistente; 403 ACL
- [ ] Gate check passes: `cd backend && mvn test -Dtest=DatasetControllerWebMvcTest`
- [ ] Test count: audit agregado tests ≥3 pass

**Tests**: integration (WebMvcTest)  
**Gate**: quick BE

---

### T18: GET /workspace/templates/{id}/versions

**What**: Listagem versões template com `estruturaResumo` para diff FE.  
**Where**: `TemplateController`, DTO, repository query, `TemplateControllerWebMvcTest.java`  
**Depends on**: None  
**Reuses**: `WorkspaceTemplateVersionRepository`  
**Requirement**: WKS2-26

**Tools**:

- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `spring-security`

**Done when**:

- [ ] Retorna `{ versao, estruturaResumo, dataPublicacao }[]` ordenado desc
- [ ] Gate check passes: `cd backend && mvn test -Dtest=TemplateControllerWebMvcTest`
- [ ] Test count: versions tests ≥3 pass

**Tests**: integration (WebMvcTest)  
**Gate**: quick BE

---

### T19: workspaceService — métodos v2 + tipos FE

**What**: `validateFormula`, `previewWidgetDefinition`, `listDatasetAudit`, `listTemplateVersions` + tipos; regenerar OpenAPI se aplicável.  
**Where**: `frontend/src/services/workspaceService.ts`, `types.ts`, `workspaceService.test.ts`  
**Depends on**: T15, T16, T17, T18  
**Reuses**: Cliente HTTP existente, tipos v1  
**Requirement**: WKS2-18, WKS2-19, WKS2-28, WKS2-26

**Tools**:

- MCP: NONE
- Skill: `api-client`

**Done when**:

- [ ] Quatro métodos implementados com tipos corretos
- [ ] Gate check passes: `cd frontend && npm test -- workspaceService`
- [ ] Test count: ≥4 tests pass (um por método)

**Tests**: unit  
**Gate**: quick FE

---

### T20: WidgetBuilderPage — fluxo dedicado com preview

**What**: Página `/workspace/:workspaceId/widgets/novo` com tipo, fontes, fórmula, validate debounce, preview, persist.  
**Where**: `WidgetBuilderPage.tsx`, `WidgetBuilderPage.test.tsx`; rota em `routes/index.tsx`  
**Depends on**: T19, T2, T4  
**Reuses**: `FormulaEditor`, `DynamicKpiWidget`/`Table`/`Chart`, `validateFormula`, `previewWidgetDefinition`  
**Requirement**: WKS2-17, WKS2-18, WKS2-19, WKS2-20

**Tools**:

- MCP: NONE
- Skill: `forms-validation`, `component-architecture`, `testing-a11y`, `routing-perf`

**Done when**:

- [ ] Seleção KPI/Tabela/Gráfico; fontes filtradas ACL
- [ ] Erro fórmula inline antes submit; preview pt-BR moeda
- [ ] Confirm persiste definição + adiciona ao layout workspace
- [ ] Gate check passes: `cd frontend && npm test -- WidgetBuilderPage`
- [ ] Test count: ≥6 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T21: TemplateCatalogPage refactor

**What**: Cards catálogo com distinção nativo vs usuário, escopo, versão, indicador vN disponível.  
**Where**: `TemplateCatalogPage.tsx`, `TemplateCatalogPage.test.tsx`  
**Depends on**: T2, T3  
**Reuses**: `listTemplateCatalog`, `RelatorioCatalogCard` pattern  
**Requirement**: WKS2-21, WKS2-25 (indicator partial)

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [ ] Cards distintos nativo vs publicado; versão visível
- [ ] Link "Ver diferenças" quando upgrade disponível
- [ ] Gate check passes: `cd frontend && npm test -- TemplateCatalogPage`
- [ ] Test count: ≥4 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T22: TemplatePublishPage — checklist estrutura×dado

**What**: Página `/workspace/templates/publish?datasetId|widgetId` com colunas "será publicado" / "nunca será publicado".  
**Where**: `TemplatePublishPage.tsx`, test, rota  
**Depends on**: T2, T4  
**Reuses**: `publishDatasetTemplate`, `publishWidgetTemplate`  
**Requirement**: WKS2-22, WKS2-23, WKS2-24

**Tools**:

- MCP: NONE
- Skill: `forms-validation`, `testing-a11y`

**Done when**:

- [ ] Checklist explícito esquema/definições vs linhas/valores/histórico
- [ ] Bloqueio item não salvo; feedback sucesso/erro idempotente
- [ ] Gate check passes: `cd frontend && npm test -- TemplatePublishPage`
- [ ] Test count: ≥5 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T23: TemplateUpgradePage + compareTemplateStructures

**What**: Página diff versões + escolha Atualizar/Permanecer; helper diff textual MVP.  
**Where**: `TemplateUpgradePage.tsx`, `utils/compareTemplateStructures.ts`, tests, rota  
**Depends on**: T19, T3  
**Reuses**: `listTemplateVersions`, `upgradeTemplateInstallation`, `TemplateUpgradeBanner` patterns  
**Requirement**: WKS2-25, WKS2-26, WKS2-27

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [ ] Diff lista campos/widgets/fórmulas add/remove/alterados
- [ ] Ações Atualizar e Permanecer sem upgrade silencioso
- [ ] Gate check passes: `cd frontend && npm test -- TemplateUpgradePage compareTemplateStructures`
- [ ] Test count: ≥5 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T24: DatasetHistoryPage — timeline + drill-down

**What**: Página `/workspace/datasets/:id/historico` com timeline e drill-down por linha.  
**Where**: `DatasetHistoryPage.tsx`, test, rota; link em `DatasetEditorPage`  
**Depends on**: T19, T2  
**Reuses**: `listDatasetAudit`, `listDatasetRowAudit`  
**Requirement**: WKS2-28, WKS2-29, WKS2-30

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [ ] Timeline cronológica; drill-down linha
- [ ] Empty state explicativo quando sem alterações
- [ ] Gate check passes: `cd frontend && npm test -- DatasetHistoryPage`
- [ ] Test count: ≥5 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T25: WorkspaceAssistantPage — superfície IA

**What**: Página `/workspace/assistente` com gate permissão, proposta inline, expiração 72h.  
**Where**: `WorkspaceAssistantPage.tsx`, test, rota  
**Depends on**: T2, T4, T3  
**Reuses**: Conteúdo `ProposalReviewDialog`, APIs proposta v1  
**Requirement**: WKS2-31, WKS2-32, WKS2-34

**Tools**:

- MCP: NONE
- Skill: `spring-security` (ACL read-only), `testing-a11y`

**Done when**:

- [ ] Com `WORKSPACE_IA_CRIAR`: fluxo propor/aplicar/descartar inline
- [ ] Sem permissão: mensagem indisponibilidade
- [ ] TTL 72h exibido quando pendente
- [ ] Gate check passes: `cd frontend && npm test -- WorkspaceAssistantPage`
- [ ] Test count: ≥4 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T26: WorkspaceSuggestionsPage — Sugerir para mim

**What**: Página `/workspace/:workspaceId/sugestoes` com propostas revisáveis, nunca auto-aplicadas.  
**Where**: `WorkspaceSuggestionsPage.tsx`, test, rota  
**Depends on**: T25, T2  
**Reuses**: `createWorkspaceProposal('SUGESTAO')`, fluxo proposta v1  
**Requirement**: WKS2-33

**Tools**:

- MCP: NONE
- Skill: `testing-a11y`

**Done when**:

- [ ] Lista sugestões como cards revisáveis
- [ ] Nenhuma aplicação automática
- [ ] Gate check passes: `cd frontend && npm test -- WorkspaceSuggestionsPage`
- [ ] Test count: ≥3 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T27: WidgetErrorBanner + polish visual WKS2-35…37

**What**: Banner erro isolado no widget; aplicar tokens/chips consistentes nas páginas v2 restantes.  
**Where**: `components/WidgetErrorBanner.tsx`, passagem em `WorkspaceGrid`/`WidgetDataRenderer`, ajustes visuais páginas  
**Depends on**: T1, T3, T4, T11  
**Reuses**: `InfoBanner`, `Dynamic*Widget`  
**Requirement**: WKS2-35, WKS2-36, WKS2-37

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:

- [ ] Fórmula inválida: banner no widget, workspace carrega
- [ ] Paleta Techne consistente hub/detail/dataset/catálogo
- [ ] Gate check passes: `cd frontend && npm test -- WidgetErrorBanner WorkspaceGrid`
- [ ] Test count: ≥4 tests pass

**Tests**: unit  
**Gate**: quick FE

---

### T28: Test migration + deprecate WidgetBuilderDrawer

**What**: Split `WorkspacePage.test` → hub/detail; atualizar testes órfãos; marcar drawer deprecated; release gate.  
**Where**: `WorkspacePage.test.tsx`, `WidgetBuilderDrawer.tsx`, imports  
**Depends on**: T12, T20, T27  
**Reuses**: Testes v1 como baseline regressão  
**Requirement**: WKS2-01…37 (regressão), Success Criteria spec

**Tools**:

- MCP: NONE
- Skill: `testing-a11y`

**Done when**:

- [ ] `WorkspaceHubPage.test` + `WorkspaceDetailPage.test` cobrem cenários migrados
- [ ] `WidgetBuilderDrawer` não referenciado em fluxo principal (redirect ou removido)
- [ ] Gate check passes: **release gate** + regressão v1
- [ ] Test count: suite workspace FE ≥ baseline v1 (sem deleções silenciosas)

**Tests**: unit  
**Gate**: release

**Commit**: `chore(workspace-v2): migrate tests and deprecate widget drawer`

---

## Phase Execution Map

Visual representation of task ordering. Phases run in sequence; tasks within a phase run in order:

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7

Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4 ──→ T5 ──→ T6
Phase 2:  T7 ──→ T8
Phase 3:  T9 ──→ T10 ──→ T11 ──→ T12
Phase 4:  T13 ──→ T14
Phase 5:  T15 ──→ T16 ──→ T17 ──→ T18
Phase 6:  T19 ──→ T20 ──→ T21 ──→ T22 ──→ T23 ──→ T24
Phase 7:  T25 ──→ T26 ──→ T27 ──→ T28
```

**Batch packing (~7 tasks/worker):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | 1–2 | T1–T8 | 8 |
| 2 | 3–4 | T9–T14 | 6 |
| 3 | 5 + T19 | T15–T19 | 5 |
| 4 | 6 (partial) | T20–T24 | 5 |
| 5 | 7 | T25–T28 | 4 |

Execution is strictly sequential — no intra-phase parallelism.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: workspaceTheme.ts | 1 file / tokens | ✅ Granular |
| T2: WorkspacePageShell | 1 component | ✅ Granular |
| T3: StatusChip | 1 component | ✅ Granular |
| T4: InfoBanner | 1 component | ✅ Granular |
| T5: QuotaProgressBar | 1 component | ✅ Granular |
| T6: workspaceLimits.ts | 1 module + test | ✅ Granular |
| T7: Layout sub-nav | 1 integration point | ✅ Granular |
| T8: useUnsavedChangesGuard | 1 hook | ✅ Granular |
| T9: WorkspaceHubPage | 1 page | ✅ Granular |
| T10: WorkspaceDetailPage | 1 page | ✅ Granular |
| T11: SourceBadge + empty | 1 component + page tweak | ✅ Granular |
| T12: Routes split | 1 route module | ✅ Granular |
| T13: Dataset schema panel | 1 page area + panel | ✅ Granular |
| T14: Dataset quotas/errors | 1 page area + component | ✅ Granular |
| T15: formula validate | 1 endpoint | ✅ Granular |
| T16: widget preview | 1 endpoint | ✅ Granular |
| T17: dataset audit | 1 endpoint | ✅ Granular |
| T18: template versions | 1 endpoint | ✅ Granular |
| T19: workspaceService v2 | 1 service file | ✅ Granular |
| T20: WidgetBuilderPage | 1 page | ✅ Granular |
| T21: TemplateCatalogPage | 1 page refactor | ✅ Granular |
| T22: TemplatePublishPage | 1 page | ✅ Granular |
| T23: TemplateUpgradePage | 1 page + helper | ✅ Granular |
| T24: DatasetHistoryPage | 1 page | ✅ Granular |
| T25: WorkspaceAssistantPage | 1 page | ✅ Granular |
| T26: WorkspaceSuggestionsPage | 1 page | ✅ Granular |
| T27: WidgetErrorBanner + polish | 1 component + visual pass | ✅ Granular |
| T28: Test migration | 1 cleanup task | ✅ Granular |

**Granularity check**: ✅ All 28 tasks pass

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | Phase 1 start | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T1 | T1 → T3 | ✅ Match |
| T4 | T1 | T1 → T4 | ✅ Match |
| T5 | T1 | T1 → T5 | ✅ Match |
| T6 | None | T6 (parallel to T1 chain) | ✅ Match |
| T7 | None | Phase 2 start | ✅ Match |
| T8 | None | T7 → T8 | ✅ Match |
| T9 | T2,T4,T5,T6 | Phase 3 start | ✅ Match |
| T10 | T2,T8 | T9 → T10 | ✅ Match |
| T11 | T10,T3 | T10 → T11 | ✅ Match |
| T12 | T9,T10,T11 | T11 → T12 | ✅ Match |
| T13 | T2,T3 | Phase 4 start | ✅ Match |
| T14 | T5,T6,T13 | T13 → T14 | ✅ Match |
| T15 | None | Phase 5 start | ✅ Match |
| T16 | None | T15 → T16 | ✅ Match |
| T17 | None | T16 → T17 | ✅ Match |
| T18 | None | T17 → T18 | ✅ Match |
| T19 | T15–T18 | Phase 6 start | ✅ Match |
| T20 | T19,T2,T4 | T19 → T20 | ✅ Match |
| T21 | T2,T3 | T20 → T21 | ✅ Match |
| T22 | T2,T4 | T21 → T22 | ✅ Match |
| T23 | T19,T3 | T22 → T23 | ✅ Match |
| T24 | T19,T2 | T23 → T24 | ✅ Match |
| T25 | T2,T4,T3 | Phase 7 start | ✅ Match |
| T26 | T25,T2 | T25 → T26 | ✅ Match |
| T27 | T1,T3,T4,T11 | T26 → T27 | ✅ Match |
| T28 | T12,T20,T27 | T27 → T28 | ✅ Match |

**Cross-check**: ✅ No backward dependencies; diagram matches all `Depends on` fields

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1: workspaceTheme | Entity/config | none | none | ✅ OK |
| T2: WorkspacePageShell | FE shared component | unit | unit | ✅ OK |
| T3: StatusChip | FE shared component | unit | unit | ✅ OK |
| T4: InfoBanner | FE shared component | unit | unit | ✅ OK |
| T5: QuotaProgressBar | FE shared component | unit | unit | ✅ OK |
| T6: workspaceLimits | FE utils/constants | unit | unit | ✅ OK |
| T7: Layout sub-nav | Layout | unit | unit | ✅ OK |
| T8: useUnsavedChangesGuard | FE hook | unit | unit | ✅ OK |
| T9: WorkspaceHubPage | FE page | unit | unit | ✅ OK |
| T10: WorkspaceDetailPage | FE page | unit | unit | ✅ OK |
| T11: SourceBadge | FE component + grid | unit | unit | ✅ OK |
| T12: Routes | Routes | unit | unit | ✅ OK |
| T13: Dataset schema | FE page | unit | unit | ✅ OK |
| T14: Dataset quotas/errors | FE page + component | unit | unit | ✅ OK |
| T15: formula validate | API controller | WebMvcTest | integration | ✅ OK |
| T16: widget preview | API controller | WebMvcTest | integration | ✅ OK |
| T17: dataset audit | API controller + service | WebMvcTest + unit | integration | ✅ OK |
| T18: template versions | API controller | WebMvcTest | integration | ✅ OK |
| T19: workspaceService | FE service | unit | unit | ✅ OK |
| T20: WidgetBuilderPage | FE page | unit | unit | ✅ OK |
| T21: TemplateCatalogPage | FE page | unit | unit | ✅ OK |
| T22: TemplatePublishPage | FE page | unit | unit | ✅ OK |
| T23: TemplateUpgradePage | FE page + util | unit | unit | ✅ OK |
| T24: DatasetHistoryPage | FE page | unit | unit | ✅ OK |
| T25: WorkspaceAssistantPage | FE page | unit | unit | ✅ OK |
| T26: WorkspaceSuggestionsPage | FE page | unit | unit | ✅ OK |
| T27: WidgetErrorBanner | FE component | unit | unit | ✅ OK |
| T28: Test migration | Tests only | unit | unit | ✅ OK |

**Co-location check**: ✅ All tasks comply

---

## Requirement Traceability (WKS2 → Tasks)

| Requirement | Task(s) |
| ----------- | ------- |
| WKS2-01…04 | T5, T6, T9, T12 |
| WKS2-05…07 | T7, T12 |
| WKS2-08…11 | T8, T10, T11, T12, T20 (navigate to builder) |
| WKS2-12…16 | T13, T14 |
| WKS2-17…20 | T15, T16, T19, T20 |
| WKS2-21 | T21 |
| WKS2-22…24 | T22 |
| WKS2-25…27 | T18, T21, T23 |
| WKS2-28…30 | T17, T19, T24 |
| WKS2-31, 32, 34 | T25 |
| WKS2-33 | T26 |
| WKS2-35…37 | T1–T4, T27, T28 |

**Coverage:** 37/37 requirements mapped

---

## MCP & Skills (confirm before Execute)

For each task batch, preferred tools:

| Batch | MCPs | Skills |
| ----- | ---- | ------ |
| 1 (T1–T8) | NONE | `component-architecture`, `testing-a11y`, `routing-perf` |
| 2 (T9–T14) | NONE | `component-architecture`, `testing-a11y`, `forms-validation`, `api-client`, `routing-perf` |
| 3 (T15–T19) | `user-context7` (Spring WebMvcTest patterns if needed) | `spring-boot-new-endpoint`, `spring-security`, `jpa-performance`, `api-client` |
| 4 (T20–T24) | NONE | `forms-validation`, `component-architecture`, `testing-a11y`, `routing-perf` |
| 5 (T25–T28) | NONE | `testing-a11y`, `spring-security` (ACL IA), `component-architecture` |
