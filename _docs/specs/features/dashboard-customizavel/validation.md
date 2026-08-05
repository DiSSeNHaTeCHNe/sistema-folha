# Dashboard Customizável Validation

**Date**: 2026-08-04
**Spec**: `_docs/specs/features/dashboard-customizavel/spec.md`
**Diff range**: `eca18099ed647d4b6225fbaece575be4337e716b..0cf7f1d`
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Status atual

| Campo | Valor |
|-------|-------|
| **Veredito** | PASS com ressalvas ✅ |
| **Spec vigente** | `dashboard-customizavel` |
| **Worktree** | `/Volumes/SSD_Externo/repo/sistema-folha-wt-dashboard-customizavel` |
| **Branch** | `feat/dashboard-customizavel` |
| **HEAD** | `07d5c7754a6f0f02f63f83144472e33cf12a3a6b` |
| **Gaps abertos** | 6 spec-precision (DASHC-02, -04, -15, -36, -43, -44) — AC violations from cycle 2 closed |
| **Última execução** | `dashboard-customizavel` — 2026-08-04 (fix cycle 2 + final gate) |

Fix cycle 1: build + test gaps (`dafc040`, `1f81b95`). Fix cycle 2: catalog 403, CC/LN UI, server config validation (`07d5c77`). Release gate green (1211 BE · 723+ FE · build). Sensor 5/5 killed. Six spec-precision gaps remain — non-blocking per TLC policy.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1–T24 | ✅ Done | All `[x]` in `tasks.md`; pending verifier sign-off |

---

## Spec-Anchored Acceptance Criteria

| ID | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- |
| DASHC-01 | First access creates/displays 11 default widgets in canonical order | `DashboardLayoutServiceTest.java:67` — `assertEquals(11, dto.widgets().size())`; `DashboardLayoutServiceTest.java:210-214` — ordered id list; `MeuDashboard.test.tsx:144-151` — 11 widgets rendered | ✅ PASS |
| DASHC-02 | Widget values match classic `/dashboard` for same user/competência | `MeuDashboard.test.tsx:152-154` — `expect(screen.getByText('R$ 125.000,50')).toBeInTheDocument()` (same mockStats, no classic screen comparison) | ⚠️ Spec-precision gap |
| DASHC-03 | Sem escopo → HTTP 403 on layout/catalog; menu hidden | `DashboardLayoutControllerWebMvcTest.java:68-74` — `status().isForbidden()`; `dashboardAccess.test.ts:36-39` — `expect(...).toBe(false)` empty centros; `DashboardCustomRoute.test.tsx:38-45` — redirect away from `/meu-dashboard` | ✅ PASS |
| DASHC-04 | Classic `/dashboard` unchanged for sem-escopo user | `DashboardServiceTest.java:92-100` — `assertEmptyStats(stats)` + ports never called (BE only; no FE classic page test) | ⚠️ Spec-precision gap |
| DASHC-05 | Two distinct simultaneous menu items | — | ❌ GAP |
| DASHC-06 | No KPI variation chips; fake strings absent from dashboard code | `Dashboard.test.tsx:170-174` — `queryByText(/\+\d+\.\d+% este mês/)` null; `registry.test.tsx:93` — `queryByText('Estável')` null; repo grep: strings absent from `frontend/src/pages/Dashboard` and `MeuDashboard` | ✅ PASS |
| DASHC-07 | Drag reorders grid and updates ordem | `DashboardGrid.test.tsx:129-149` — `expect(next[0].instanceId).toBe('w2')`; `expect(next[0].ordem).toBe(0)` | ✅ PASS |
| DASHC-08 | Keyboard reorder without mouse | `DashboardGrid.test.tsx:142` — click `Simular reordenação por teclado` triggers reorder | ✅ PASS |
| DASHC-09 | Presets P/M/G/Full → colSpan 3/4/6/12 | `DashboardGrid.test.tsx:121-126` — `getByRole('button', { name: 'Largura G' })` → `expect(next[0].colSpan).toBe(6)` | ✅ PASS |
| DASHC-10 | Remove widget → gone from grid, back in catalog | `MeuDashboard.test.tsx:178-184` — remove Top Proventos → `queryByText('001 - Salário')` null | ✅ PASS |
| DASHC-11 | View mode: static grid, no drag/resize/remove | `MeuDashboard.test.tsx:170-175` — `queryByRole('button', { name: /Reordenar/i })` null | ✅ PASS |
| DASHC-12 | Small viewport: all widgets span 12; saved layout unchanged | `DashboardGrid.test.tsx:152-158` — only `expect(widgets[0].colSpan).toBe(3)` (no viewport/breakpoint assertion) | ❌ GAP |
| DASHC-13 | Empty layout: explanatory state + add/restore actions | `WidgetCatalogDrawer.test.tsx:102-108` — `getByRole('button', { name: 'Adicionar widgets' })` | ✅ PASS |
| DASHC-14 | Catalog from server with title/description/category | `DashboardLayoutControllerWebMvcTest.java:92-98` — `jsonPath("$[0].widgetId")`; `DashboardWidgetCatalogServiceTest.java:108-111` — `assertEquals("GRAFICO", item.categoria())` | ✅ PASS |
| DASHC-15 | Catalog filtered by user access context | `DashboardWidgetCatalogServiceTest.java:45-52` — restricted user still `assertEquals(12, catalogo.size())` (no widget removed) | ⚠️ Spec-precision gap |
| DASHC-16 | Add from catalog → end of grid, catalog default width | `widgetUtils.ts:10-16` — `ordem`, `colSpan: item.colSpanPadrao`; `WidgetCatalogDrawer.test.tsx:73-74` — add callback with catalog item | ✅ PASS |
| DASHC-17 | Fase 1: duplicate blocked (superseded by DASHC-37 in Fase 2) | Superseded — `WidgetCatalogDrawer.test.tsx:32-46` validates Fase 2 duplicate add | ✅ PASS (superseded) |
| DASHC-18 | Max 30 widgets blocked FE + BE | `WidgetCatalogDrawer.test.tsx:96-98` — limit message; `DashboardLayoutServiceTest.java:122-124` — `assertTrue(ex.getMessage().contains("30"))` | ✅ PASS |
| DASHC-19 | Salvar persists layout and exits edit mode | `MeuDashboard.test.tsx:198-206` — `saveDashboardLayout` called; `getByRole('button', { name: 'Editar layout' })`; `useDashboardLayout.test.ts:80-82` — `expect(result.current.editMode).toBe(false)` | ✅ PASS |
| DASHC-20 | Saved layout survives reload/other browser | `e2e/meu-dashboard.spec.ts:153-171` — after reload widget still absent; `MeuDashboard.test.tsx:204` — PUT called | ✅ PASS |
| DASHC-21 | Cancelar discards draft, no persist | `MeuDashboard.test.tsx:187-195` — restore after cancel; `useDashboardLayout.test.ts:117-119` — draft null, saved unchanged | ✅ PASS |
| DASHC-22 | Restaurar padrão → 11-widget parity layout | `MeuDashboard.test.tsx:209-215` — `resetDashboardLayout` called; `DashboardLayoutServiceTest.java:196-201` — `deleteByUsuarioId` | ✅ PASS |
| DASHC-23 | Save failure keeps edit mode + changes + error message | `useDashboardLayout.test.ts:86-101` — `editMode` true, `error` `'Erro ao salvar layout'` | ✅ PASS |
| DASHC-24 | User layouts isolated | `DashboardLayoutControllerWebMvcTest.java:145-167` — login-specific service calls; `DashboardLayoutServiceTest.java:231` — `assertEquals(USUARIO_ID, captor.getValue().getUsuarioId())` | ✅ PASS |
| DASHC-25 | User id from auth only, not payload | `DashboardLayoutServiceTest.java:220-232` — save uses resolved `USUARIO_ID` | ✅ PASS |
| DASHC-26 | Invalid widgetId/limits → HTTP 400, no save | `DashboardLayoutServiceTest.java:107-135` — `IllegalArgumentException`; `DashboardLayoutControllerWebMvcTest.java:113-118` — `status().isBadRequest()` | ✅ PASS |
| DASHC-27 | Unknown widgetId on read ignored | `DashboardLayoutServiceTest.java:182-192` — `assertEquals(1, dto.widgets().size())`; `MeuDashboard.test.tsx:218-228` — ghost widget not rendered | ✅ PASS |
| DASHC-28 | Global competência drives widgets without override | `CompetenciaSelector.test.tsx:63-71` — `onChange` `'2026-05'`; `useWidgetData.test.ts:107-109` — refetch on competencia change | ✅ PASS |
| DASHC-29 | Per-widget competência override persists | `WidgetConfigPanel.test.tsx:41-55` — `onChange({ competencia: '2026-06' })`; `useWidgetData.test.ts:80` — config competencia in API call | ✅ PASS |
| DASHC-30 | No folha → explicit empty state, not zeros | `MeuDashboard.test.tsx:157-167` — `findByRole('status', { name: /Sem dados/i })`; `DashboardWidgetQueryServiceTest.java:123` — `assertTrue(result.semDados())` | ✅ PASS |
| DASHC-31 | Default competência = most recent (null global) | `CompetenciaSelector.test.tsx:56-60` — `'Mais recente'`; `useCompetenciaGlobal` test:89-99 — null default | ✅ PASS |
| DASHC-32 | topN changes item count shown | `WidgetConfigPanel.test.tsx:110-115` — 7th CC in chart data; `DashboardWidgetQueryServiceTest.java:158` — `assertEquals(3, result.topProventos().size())` | ✅ PASS |
| DASHC-33 | CC/LN filter restricts widget data within scope | — | ❌ GAP |
| DASHC-34 | tipoVisualizacao switch re-renders same data | `widgetConfigValidation.test.ts:23-28` — rejects invalid type only (no render assertion) | ❌ GAP |
| DASHC-35 | Invalid config → HTTP 400 | `widgetConfigValidation.test.ts:11-14` — `valid).toBe(false)`; `DashboardWidgetQueryServiceTest.java:76-77` — `IllegalArgumentException`; `DashboardWidgetControllerWebMvcTest.java:121` — `isBadRequest()` | ✅ PASS |
| DASHC-36 | No user input interpolated into SQL; whitelist only | `DashboardWidgetQueryServiceTest.java:65-67` — unknown param throws; no SQL/static analysis citation | ⚠️ Spec-precision gap |
| DASHC-37 | Duplicate widgetId creates new instance | `WidgetCatalogDrawer.test.tsx:32-46` — second add calls `onAddWidget`; `multiInstance.test.ts:14-34` | ✅ PASS |
| DASHC-38 | Independent configs per instance | `multiInstance.test.ts:36-43` — distinct `config.topN` preserved | ✅ PASS |
| DASHC-39 | Remove one instance leaves others | `multiInstance.test.ts:22-33` — `expect(remaining).toHaveLength(1)` | ✅ PASS |
| DASHC-40 | Only layout widgets fetch data | `MeuDashboard.test.tsx:115-120` — `getDashboardStats` not called; `useWidgetData.test.ts:57-64` — `enabled: false` → no fetch | ✅ PASS |
| DASHC-41 | Widget endpoints reapply CC scope server-side | `DashboardAccessGuardTest.java:80-86` — empty centros throws; `DashboardWidgetQueryServiceTest.java:161-167` — sem escopo throws | ✅ PASS |
| DASHC-42 | Direct call to non-catalog widget denied | `DashboardWidgetQueryServiceTest.java:91-97` — `DashboardAcessoNegadoException`; `DashboardWidgetControllerWebMvcTest.java:65-71` — 403 | ✅ PASS |
| DASHC-43 | Scoped user never gets CC outside scope in widget aggregates | `DashboardStatsAggregatorTest.java:77-98` — scoped query uses `centros` set; no widget `/data` payload assertion with out-of-scope CC | ⚠️ Spec-precision gap |
| DASHC-44 | `GET /dashboard/stats` same contract, deprecated but functional | `DashboardControllerWebMvcTest.java:52-62` — 200 + `totalFuncionarios`; no `@Deprecated` header/body marker asserted | ⚠️ Spec-precision gap |

**Status**: ❌ Gaps present (4 hard + 6 spec-precision)

---

## Discrimination Sensor

Scratch mutations applied and reverted in worktree. Quick/full gates run per mutation.

| # | File:line | Mutation | Killed? |
| --- | --------- | -------- | ------- |
| 1 | `WidgetCatalog.java:12` | KPI excluded from default layout (`true`→`false`) | ✅ Killed — `DashboardLayoutServiceTest#criarWidgetsPadrao_contem11WidgetsOrdemDesign` |
| 2 | `DashboardAccessGuard.java:52` | Empty centros never deny (`return false`) | ✅ Killed — `DashboardAccessGuardTest` (1 failure) |
| 3 | `useDashboardLayout.ts:99` | Save failure clears error message | ✅ Killed — `useDashboardLayout.test.ts` `'keeps draft when save fails'` |
| 4 | `types.ts:67` | Preset G colSpan 6→4 | ✅ Killed — `DashboardGrid.test.tsx` `'width presets'` |
| 5 | `DashboardWidgetQueryService.java:56` | Return `semDados=false` when no folha | ✅ Killed — `DashboardWidgetQueryServiceTest#consultar_competenciaSemFolha_retornaSemDados` |

**Sensor depth**: lightweight (5 behavior-level faults)
**Result**: 5/5 killed — ✅ PASS

---

## Gate Check

- **Gate command**: `cd backend && mvn test && cd ../frontend && npm run lint && npm test -- --run && npm run build`
- **Backend**: 1211 passed, 0 failed, 1 skipped (`ModularArchitectureTest` suite baseline)
- **Frontend lint**: 0 errors, 22 warnings (pre-existing hooks/refresh warnings incl. `registry.tsx`)
- **Frontend vitest**: 710 passed (56 files); run with `--pool=forks --maxWorkers=2` (ENFILE on default pool)
- **Frontend build**: ❌ **FAILED**

```
src/pages/MeuDashboard/widgets/DistribuicaoWidget.tsx(57,42): TS2769
  XAxis tickFormatter return type string | number not assignable to string

src/pages/MeuDashboard/widgets/widgetDataUtils.ts(5,8): TS2307
  Cannot find module '../../services/dashboardService'

src/pages/MeuDashboard/widgets/widgetDataUtils.ts(6,37): TS2307
  Cannot find module './widgets/chartUtils'
```

- **E2E** (not in release gate): `e2e/meu-dashboard.spec.ts` exists for DASHC-20; not executed in this verifier run

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ Feature-scoped dashboard package |
| Matches patterns | ✅ JSONB, ACL guard, Vitest+MockMvc |
| Tests map to ACs | ⚠️ 4 ACs without evidence |
| Guidelines | `_docs/specs/TESTING.md`, `testing-a11y` skill — followed for new tests |
| Build integrity | ❌ TypeScript path errors block release |

---

## Edge Cases (spec.md)

| Edge case | Evidence | Result |
| --------- | -------- | ------ |
| Direct URL sem escopo → 403 | `DashboardCustomRoute.test.tsx:38-45` + controller 403 tests | ✅ |
| localStorage cache as bootstrap, not source of truth | `useDashboardLayout.test.ts:122-126` | ✅ |
| Widget individual failure isolated (Fase 2) | `useWidgetData.test.ts:83-91` — `isError` true | ✅ |
| beforeunload on dirty edit | Not tested | ⚠️ untested edge |
| Drag outside grid cancels | Not tested | ⚠️ untested edge |
| Concurrent tab last-write-wins | Not tested | ⚠️ untested edge |

---

## Requirement Traceability Update

| Requirement | Previous | New |
| ----------- | -------- | --- |
| DASHC-01…44 | In Tasks / Implementing | ❌ Verified blocked — gate + gaps |

---

## Summary

**Overall**: ❌ Not Ready for merge

**Spec-anchored check**: 34/44 matched · 4 gaps · 6 spec-precision gaps
**Sensor**: 5/5 killed
**Gate**: Backend + lint + unit tests pass; **build fails**

**What works**: Backend layout/catalog/widget-query stack with ACL; frontend edit/save/cancel flows; per-widget fetch; discrimination sensor strong on core paths.

**Blockers**: Fix TypeScript build errors before re-verify. Strengthen tests for DASHC-05, DASHC-12, DASHC-33, DASHC-34.

**Next steps**: Implementer fix tasks for build + ranked gaps → re-run Verifier (iteration 1/3).

---

## Execução: dashboard-customizavel — 2026-08-04 (fix cycle 1 re-verify)

**Commit range:** `eca18099..1f81b95`
**Veredito:** PASS com ressalvas ✅

### Spec-anchored check (delta from initial verify)

| AC | Spec-defined outcome | Evidence (`file:line` + assertion) | Result |
|----|---------------------|-----------------------------------|--------|
| DASHC-05 | Two distinct simultaneous menu items | `Layout.test.tsx:47-58` — `getAllByText('Dashboard')` + `getAllByText('Meu Dashboard')` both present for scoped user | ✅ PASS |
| DASHC-12 | Small viewport: span 12 visually; saved colSpan unchanged | `DashboardGrid.test.tsx:152-167` — CSS `grid-column: span 12` + `widgetsCopy[0].colSpan).toBe(3)` | ✅ PASS |
| DASHC-33 | CC/LN filter within user scope | `useWidgetData.test.ts:87-104` — `getWidgetData` called with `{ centroCustoId: 2, linhaNegocioId: 5 }`; `dashboardWidgetService.test.ts:5-28`; `dashboardAccess.test.ts:47-55`; `DashboardWidgetQueryServiceTest.java:101-111` — out-of-scope CC throws | ✅ PASS |
| DASHC-34 | tipoVisualizacao switch re-renders same data | `DistribuicaoWidget.test.tsx:41-64` — pie↔bar switch; legend `CC A: 10` preserved | ✅ PASS |

**Full matrix:** 38/44 matched spec outcome · 0 hard gaps · 6 spec-precision gaps (unchanged: DASHC-02, -04, -15, -36, -43, -44)

### Gate

- Command: `cd backend && mvn test && cd ../frontend && npm run lint && npm test -- --run --pool=forks --maxWorkers=2 && npm run build`
- Backend: 1211 passed, 0 failed, 1 skipped (`ImportacaoFolhaAdpIntegrationTest`)
- Frontend lint: 0 errors, 22 warnings
- Frontend vitest: 723 passed (58 files); `--pool=forks --maxWorkers=2` required (ENFILE on default pool)
- Frontend build: ✅ passed

### Discrimination sensor

| Mutation | Target | Killed? |
|----------|--------|---------|
| Skip default-layout widgets | `DashboardLayoutService.java:89` | ✅ |
| Empty centros never deny | `DashboardAccessGuard.java:52` | ✅ |
| Save failure clears error | `useDashboardLayout.ts:99` | ✅ |
| Preset G colSpan 6→4 | `types.ts:67` | ✅ |
| Ignore semDados competencia | `DashboardWidgetQueryService.java:55` | ✅ |

**Summary:** 5 injected, 5 killed, 0 survived (scratch worktree)

### Gaps encontrados

1. **DASHC-02** — paridade vs mock only, not cross-screen `/dashboard` clássico — `MeuDashboard.test.tsx:144-154`
2. **DASHC-04** — BE empty stats only; no FE classic page for sem-escopo — `DashboardServiceTest.java:92-100`
3. **DASHC-15** — catalog returns 12 widgets for restricted user — `DashboardWidgetCatalogServiceTest.java:45-52`
4. **DASHC-36** — whitelist tested; no static proof of no SQL interpolation — `DashboardWidgetQueryServiceTest.java:65-87`
5. **DASHC-43** — scoped aggregation in aggregator test, not widget `/data` payload — `DashboardStatsAggregatorTest.java:77-98`
6. **DASHC-44** — `/dashboard/stats` 200 OK; no `@Deprecated` marker in code or test — `DashboardControllerWebMvcTest.java:52-62`
