## Status atual
- **Veredito:** PASS
- **Spec vigente:** workspace-usuario-v2-fix2/spec.md
- **HEAD:** b4b2e7cf7e710f4a73ff8e2d2fef8c7c6024ed1d
- **Gaps abertos:** nenhum — 14/14 ACs com evidência; sensor 5/5 killed

---

## workspace-usuario-v2-fix2 — 2026-08-05 — 5d65deb..9f08dd4

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** feat/workspace-usuario-v2  
**Diff surface:** `frontend/src/routes/index.tsx`, `frontend/src/routes/index.test.tsx`, `frontend/src/test/renderWithDataRouter.tsx`, `frontend/src/pages/Workspace/WorkspaceDetailPage.integration.test.tsx`

### Veredito: FAIL

Implementação e gates de regressão estão verdes, mas a verificação evidence-or-zero não fecha 14/14: WKS2F2-04 carece de asserção de teste, WKS2F2-05 cobre só 2 das 5 rotas citadas na spec, e o sensor registrou mutante sobrevivente na camada de produção do router.

---

### Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | `createBrowserRouter` + `RouterProvider` em `routes/index.tsx`; build verde |
| T2 | ✅ Done | Smoke auth + workspace ACL em `index.test.tsx` |
| T3 | ✅ Done | `renderWithDataRouter.tsx` exportado e consumido |
| T4 | ✅ Done | Integração detail sem mock do guard |
| T5 | ✅ Done | Guard dirty/clean na integração |
| T6 | ✅ Done | Gate full 991 tests; workspace 205 tests |

---

### Evidência por AC (table)

| ID | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion expression | Result |
| --- | --- | --- | --- | --- |
| WKS2F2-01 | WHEN usuário clica **Abrir** no hub THEN navega para `/workspace/{id}` e renderiza heading | Heading do workspace visível; não tela branca | `WorkspaceDetailPage.integration.test.tsx:79` — `expect(await screen.findByRole('heading', { name: 'Planejamento', level: 1 })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-02 | WHEN `WorkspaceDetailPage` monta THEN console sem erro `useBlocker must be used within a data router` | Ausência do erro no `console.error` | `WorkspaceDetailPage.integration.test.tsx:80-82` — `expect(consoleError.mock.calls.some((call) => String(call[0]).includes('useBlocker must be used within a data router'))).toBe(false)` | ✅ PASS |
| WKS2F2-03 | WHEN workspace existe THEN toolbar **ou** empty state **Workspace vazio** | Botões **Adicionar widget** / **Editar layout** ou status vazio | `WorkspaceDetailPage.integration.test.tsx:90-91` — `expect(screen.getByRole('button', { name: 'Adicionar widget' })).toBeInTheDocument()`; `:100` — `expect(await screen.findByRole('status', { name: 'Workspace vazio' })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-04 | WHEN app inicia THEN root usa `RouterProvider` + `createBrowserRouter` — NOT `<BrowserRouter>` | Produção monta data router global | Impl: `routes/index.tsx:126-130`, `main.tsx:13` — sem asserção Vitest | ❌ GAP |
| WKS2F2-05 | WHEN usuário acessa rotas existentes THEN cada rota renderiza componente correto | Smoke em `/dashboard`, `/workspace`, `/workspace/datasets`, `/login`, `/usuarios` | `index.test.tsx:81` — `expect(await screen.findByRole('heading', { name: 'Dashboard Gerencial', level: 1 })).toBeInTheDocument()`; `:86` — `expect(await screen.findByRole('heading', { name: 'Meus workspaces', level: 1 })).toBeInTheDocument()`; `/login`, `/workspace/datasets`, `/usuarios` — sem teste | ⚠️ GAP parcial |
| WKS2F2-06 | WHEN não autenticado acessa rota privada THEN redirect `/login` | Tela de login visível | `index.test.tsx:92-93` — `renderRoutes('/dashboard', { user: null, isAuthenticated: false, acessoUsuario: null })`; `expect(await screen.findByRole('heading', { name: 'Sistema de Folha', level: 1 })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-07 | WHEN sem acesso workspace acessa `/workspace` THEN alerta acesso negado | Alert com texto de acesso negado; hub não renderiza | `index.test.tsx:102-105` — `expect(screen.getByRole('alert')).toHaveTextContent(/Acesso negado ao Workspace/i)`; `expect(screen.queryByRole('heading', { name: 'Meus workspaces' })).not.toBeInTheDocument()` | ✅ PASS |
| WKS2F2-08 | WHEN modo edição dirty THEN `beforeunload` listener registrado | `addEventListener('beforeunload', …)` | `useUnsavedChangesGuard.test.tsx:47` — `expect(addListener).toHaveBeenCalledWith('beforeunload', expect.any(Function))` | ✅ PASS |
| WKS2F2-09 | WHEN dirty e navegação in-app THEN `window.confirm` com mensagem padrão | `'Existem alterações não salvas. Deseja sair sem salvar?'` | `WorkspaceDetailPage.integration.test.tsx:130` — `expect(confirmSpy).toHaveBeenCalledWith('Existem alterações não salvas. Deseja sair sem salvar?')`; `useUnsavedChangesGuard.test.tsx:56` — mesma asserção | ✅ PASS |
| WKS2F2-10 | WHEN usuário cancela confirm THEN permanece na página de detalhe | Heading do workspace presente; hub destino ausente | `WorkspaceDetailPage.integration.test.tsx:139-140` — `expect(screen.getByRole('heading', { name: 'Planejamento', level: 1 })).toBeInTheDocument()`; `expect(screen.queryByRole('heading', { name: 'Hub de destino', level: 1 })).not.toBeInTheDocument()` | ✅ PASS |
| WKS2F2-11 | WHEN `dirty=false` THEN navegação sem confirm | `confirm` não chamado; destino renderizado | `WorkspaceDetailPage.integration.test.tsx:159-160` — `expect(confirmSpy).not.toHaveBeenCalled()`; `expect(await screen.findByRole('heading', { name: 'Hub de destino', level: 1 })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-12 | WHEN `npm test` THEN contagem ≥ baseline pré-fix2 (980) | ≥980 passed, 0 failed | Gate `@9f08dd4` — Vitest: `Tests 991 passed (991)` vs baseline 980 @ `5d65deb` | ✅ PASS |
| WKS2F2-13 | WHEN `npm run build` THEN completa sem erro TS | Exit 0, bundle emitido | Gate `@9f08dd4` — `tsc -b && vite build` exit 0 | ✅ PASS |
| WKS2F2-14 | WHEN `npm test -- src/pages/Workspace` THEN suite 100% | 205/205 pass | Gate `@9f08dd4` — `Test Files 32 passed (32); Tests 205 passed (205)` | ✅ PASS |

**Spec-anchored check:** 12/14 matched spec outcome; 1 GAP (WKS2F2-04); 1 partial GAP (WKS2F2-05 — 2/5 rotas listadas)

---

### Sensor de discriminação

| # | Mutation | File:line | Description | Killed? |
| --- | --- | --- | --- | --- |
| 1 | Confirm message | `useUnsavedChangesGuard.ts:4` | `DEFAULT_MESSAGE` → `'Mensagem errada'` | ✅ Killed — 5 tests failed (`useUnsavedChangesGuard` + `WorkspaceDetailPage.integration`) |
| 2 | Blocker polarity | `useUnsavedChangesGuard.ts:15` | `useBlocker(dirty)` → `useBlocker(!dirty)` | ✅ Killed — 3 integration tests failed (confirm/cancel/clean paths) |
| 3 | beforeunload | `useUnsavedChangesGuard.ts:25` | Comentou `window.addEventListener` | ✅ Killed — `useUnsavedChangesGuard.test.tsx:47` failed |
| 4 | Auth redirect | `routes/index.tsx:58` | `if (!user)` → `if (user)` | ✅ Killed — 4/4 `index.test.tsx` failed |
| 5 | Production router API | `routes/index.tsx:126` | `createBrowserRouter` → `createMemoryRouter` | ❌ **Survived** — `WorkspaceDetailPage.integration` 7/7 still pass (harness usa router próprio) |

**Sensor depth:** lightweight (5 mutations)  
**Result:** 4/5 killed — **FAIL** (survivor on WKS2F2-04 production path)

Todos os mutantes foram aplicados em scratch (`cp` + `sed` + restore); working tree restaurada ao HEAD.

---

### Gate Check

| Command | Result |
| --- | --- |
| `cd frontend && npm test` | ✅ 991 passed, 0 failed, 95 files, exit 0 (~186s) |
| `cd frontend && npm run build` | ✅ exit 0 (~11s) |
| `cd frontend && npm test -- src/pages/Workspace` | ✅ 205 passed, 32 files, exit 0 (~44s) |

- **Test count before feature:** 980 @ `5d65deb`
- **Test count after feature:** 991 @ `9f08dd4`
- **Delta:** +11 tests
- **Skipped:** 0
- **Failures:** none

---

### Edge Cases (spec)

| Edge case | Evidence | Result |
| --- | --- | --- |
| Id inválido (`NaN`) → **Workspace não encontrado** | Não coberto por teste fix2 | ⚠️ Out of fix2 gate |
| `getWorkspace` falha → erro + **Voltar ao hub** | Não coberto por teste fix2 | ⚠️ Out of fix2 gate |
| Confirm descarte → navegação completa | `WorkspaceDetailPage.integration.test.tsx:149` | ✅ PASS |
| Hot reload sem router duplicado | Manual/dev only | ⚠️ Not automated |

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / surgical diff | ✅ 4 files, +419/-52 |
| No scope creep | ✅ Routing + test harness only |
| Matches patterns | ✅ Vitest + RTL by role |
| Tests map to ACs | ⚠️ WKS2F2-04/05 gaps |
| Guidelines | `frontend/AGENTS.md`, `_docs/specs/TESTING.md`, `.agents/skills/testing-a11y/SKILL.md` |

---

### Gaps encontrados

1. **WKS2F2-04 — no automated assertion for production data router** — Implementation correct (`routes/index.tsx:126-130`, `main.tsx:13`), but evidence-or-zero requires test assertion; T1 explicitly omitted tests; production-router mutant survives.
2. **WKS2F2-05 — partial route smoke** — Spec lists 5 routes; tests assert only `/dashboard` and `/workspace`. Missing smoke for `/login`, `/workspace/datasets`, `/usuarios`.
3. **Discrimination sensor survivor** — Swapping `createBrowserRouter` → `createMemoryRouter` in production `RouterWithAuth` does not fail any test in fix2 scope.

**Recommended fix tasks (for implementer, not executed by Verifier):**

- Add unit/static test asserting `RouterWithAuth` / `routeObjects` wiring uses `createBrowserRouter` (or import smoke that fails under `<BrowserRouter>`).
- Extend `index.test.tsx` with smoke for `/login`, `/workspace/datasets`, `/usuarios` (mock admin auth for `/usuarios`).
- Optional: app-level integration test mounting `RouterWithAuth` under test to kill production-router mutants.

---

### Requirement Traceability (Verifier view)

| Requirement | Author status | Verifier status |
| --- | --- | --- |
| WKS2F2-01…03 | Done | ✅ Verified |
| WKS2F2-04 | Done | ❌ Needs Fix (no test evidence) |
| WKS2F2-05 | Done | ⚠️ Partial |
| WKS2F2-06…11 | Done | ✅ Verified |
| WKS2F2-12…14 | Done | ✅ Verified |

---

### Summary

**Overall:** ❌ Not Ready — gates green, spec-anchored evidence incomplete.

**What works:** Detail page under data router without `useBlocker` crash; guard confirm/cancel/clean flows; auth + workspace ACL smoke; full FE regression (+11 tests).

**Next steps:** Implement fix tasks for WKS2F2-04/05 and re-run Verifier (iteration 1/3).

---

## workspace-usuario-v2-fix2 — 2026-08-05 — fix cycle-1 — 9f08dd4..2ed851e

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** feat/workspace-usuario-v2  
**Diff surface (cycle-1):** `frontend/src/routes/index.test.tsx` (+86 lines)

### Veredito: PASS

Fix cycle-1 fecha os gaps WKS2F2-04/05 com asserções Vitest em `index.test.tsx`. Sensor de discriminação 5/5 killed (mutante de produção `createBrowserRouter` → `createMemoryRouter` agora falha 2 testes). Gates verdes @ `2ed851e`.

---

### Task Completion (cycle-1)

| Task | Status | Notes |
| ---- | ------ | ----- |
| Fix WKS2F2-04 | ✅ Done | Static + mount tests for `RouterWithAuth` / `createBrowserRouter(routeObjects)` |
| Fix WKS2F2-05 | ✅ Done | Smoke for `/login`, `/workspace/datasets`, `/usuarios` added |

---

### Evidência por AC (table)

| ID | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion expression | Result |
| --- | --- | --- | --- | --- |
| WKS2F2-01 | WHEN usuário clica **Abrir** no hub THEN navega para `/workspace/{id}` e renderiza heading | Heading do workspace visível; não tela branca | `WorkspaceDetailPage.integration.test.tsx:79` — `expect(await screen.findByRole('heading', { name: 'Planejamento', level: 1 })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-02 | WHEN `WorkspaceDetailPage` monta THEN console sem erro `useBlocker must be used within a data router` | Ausência do erro no `console.error` | `WorkspaceDetailPage.integration.test.tsx:80-82` — `expect(consoleError.mock.calls.some((call) => String(call[0]).includes('useBlocker must be used within a data router'))).toBe(false)` | ✅ PASS |
| WKS2F2-03 | WHEN workspace existe THEN toolbar **ou** empty state **Workspace vazio** | Botões **Adicionar widget** / **Editar layout** ou status vazio | `WorkspaceDetailPage.integration.test.tsx:90-91` — `expect(screen.getByRole('button', { name: 'Adicionar widget' })).toBeInTheDocument()`; `:100` — `expect(await screen.findByRole('status', { name: 'Workspace vazio' })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-04 | WHEN app inicia THEN root usa `RouterProvider` + `createBrowserRouter` — NOT `<BrowserRouter>` | Produção monta data router global | `index.test.tsx:113-117` — `expect(routesModuleSource).toMatch(/createBrowserRouter\s*\(\s*routeObjects\s*\)/)`; `:115-116` — `expect(routesModuleSource).not.toMatch(/<\s*BrowserRouter/)`; `:119-135` — `expect(createBrowserRouterMock).toHaveBeenCalledWith(routeObjects)`; impl `routes/index.tsx:126-130`, `main.tsx:13` | ✅ PASS |
| WKS2F2-05 | WHEN usuário acessa rotas existentes THEN cada rota renderiza componente correto | Smoke em `/dashboard`, `/workspace`, `/workspace/datasets`, `/login`, `/usuarios` | `index.test.tsx:147-150` — Dashboard; `:152-155` — Meus workspaces; `:157-160` — Sistema de Folha (login); `:162-165` — Datasets; `:167-172` — Manutenção de Usuários | ✅ PASS |
| WKS2F2-06 | WHEN não autenticado acessa rota privada THEN redirect `/login` | Tela de login visível | `index.test.tsx:176-178` — `renderRoutes('/dashboard', { user: null, ... })`; `expect(await screen.findByRole('heading', { name: 'Sistema de Folha', level: 1 })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-07 | WHEN sem acesso workspace acessa `/workspace` THEN alerta acesso negado | Alert com texto de acesso negado; hub não renderiza | `index.test.tsx:183-190` — `expect(screen.getByRole('alert')).toHaveTextContent(/Acesso negado ao Workspace/i)`; `expect(screen.queryByRole('heading', { name: 'Meus workspaces' })).not.toBeInTheDocument()` | ✅ PASS |
| WKS2F2-08 | WHEN modo edição dirty THEN `beforeunload` listener registrado | `addEventListener('beforeunload', …)` | `useUnsavedChangesGuard.test.tsx:47` — `expect(addListener).toHaveBeenCalledWith('beforeunload', expect.any(Function))` | ✅ PASS |
| WKS2F2-09 | WHEN dirty e navegação in-app THEN `window.confirm` com mensagem padrão | `'Existem alterações não salvas. Deseja sair sem salvar?'` | `WorkspaceDetailPage.integration.test.tsx:130` — `expect(confirmSpy).toHaveBeenCalledWith('Existem alterações não salvas. Deseja sair sem salvar?')`; `useUnsavedChangesGuard.test.tsx:56` — mesma asserção | ✅ PASS |
| WKS2F2-10 | WHEN usuário cancela confirm THEN permanece na página de detalhe | Heading do workspace presente; hub destino ausente | `WorkspaceDetailPage.integration.test.tsx:139-140` — `expect(screen.getByRole('heading', { name: 'Planejamento', level: 1 })).toBeInTheDocument()`; `expect(screen.queryByRole('heading', { name: 'Hub de destino', level: 1 })).not.toBeInTheDocument()` | ✅ PASS |
| WKS2F2-11 | WHEN `dirty=false` THEN navegação sem confirm | `confirm` não chamado; destino renderizado | `WorkspaceDetailPage.integration.test.tsx:159-160` — `expect(confirmSpy).not.toHaveBeenCalled()`; `expect(await screen.findByRole('heading', { name: 'Hub de destino', level: 1 })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-12 | WHEN `npm test` THEN contagem ≥ baseline pré-fix2 (980) | ≥980 passed, 0 failed | Gate `@2ed851e` — Vitest: `Tests 996 passed (996)` vs baseline 980 @ `5d65deb` | ✅ PASS |
| WKS2F2-13 | WHEN `npm run build` THEN completa sem erro TS | Exit 0, bundle emitido | Gate `@2ed851e` — `tsc -b && vite build` exit 0 (~11s) | ✅ PASS |
| WKS2F2-14 | WHEN `npm test -- src/pages/Workspace` THEN suite 100% | 205/205 pass | Gate `@2ed851e` — `Test Files 32 passed (32); Tests 205 passed (205)` | ✅ PASS |

**Spec-anchored check:** 14/14 matched spec outcome

---

### Sensor de discriminação

| # | Mutation | File:line | Description | Killed? |
| --- | --- | --- | --- | --- |
| 1 | Confirm message | `useUnsavedChangesGuard.ts:4` | `DEFAULT_MESSAGE` → `'Mensagem errada'` | ✅ Killed — 2 tests failed (guard + integration) |
| 2 | Blocker polarity | `useUnsavedChangesGuard.ts:15` | `useBlocker(dirty)` → `useBlocker(!dirty)` | ✅ Killed — 3 integration tests failed |
| 3 | beforeunload | `useUnsavedChangesGuard.ts:25` | Comentou `window.addEventListener` | ✅ Killed — `useUnsavedChangesGuard.test.tsx:47` failed |
| 4 | Auth redirect | `routes/index.tsx:58` | `if (!user)` → `if (user)` | ✅ Killed — 7/9 `index.test.tsx` failed |
| 5 | Production router API | `routes/index.tsx:126` | `createBrowserRouter` → `createMemoryRouter` | ✅ Killed — 2/9 `index.test.tsx` failed (static + mount WKS2F2-04) |

**Sensor depth:** lightweight (5 mutations)  
**Result:** 5/5 killed — **PASS**

Mutantes aplicados em scratch (`cp` + `sed` + restore); working tree restaurada ao HEAD.

---

### Gate Check

| Command | Result |
| --- | --- |
| `cd frontend && npm test` | ✅ 996 passed, 0 failed, 95 files, exit 0 (~142s) |
| `cd frontend && npm run build` | ✅ exit 0 (~11s) |
| `cd frontend && npm test -- src/pages/Workspace` | ✅ 205 passed, 32 files, exit 0 (~42s) |

- **Test count before feature:** 980 @ `5d65deb`
- **Test count after fix2 (pre cycle-1):** 991 @ `9f08dd4`
- **Test count after cycle-1:** 996 @ `2ed851e`
- **Delta (full fix2):** +16 tests
- **Delta (cycle-1):** +5 tests
- **Skipped:** 0
- **Failures:** none

---

### Requirement Traceability (Verifier view)

| Requirement | Prior Verifier | Cycle-1 Verifier |
| --- | --- | --- |
| WKS2F2-01…03 | ✅ Verified | ✅ Verified |
| WKS2F2-04 | ❌ Needs Fix | ✅ Verified |
| WKS2F2-05 | ⚠️ Partial | ✅ Verified |
| WKS2F2-06…11 | ✅ Verified | ✅ Verified |
| WKS2F2-12…14 | ✅ Verified | ✅ Verified |

---

### Summary

**Overall:** ✅ Ready — 14/14 ACs evidence-or-zero; gates green; sensor 5/5.

**Cycle-1 fix:** `index.test.tsx` adds WKS2F2-04 static + `RouterWithAuth` mount assertions and WKS2F2-05 smoke for login, datasets, usuarios.

**Gaps closed:** WKS2F2-04 test evidence; WKS2F2-05 full route coverage; production-router mutant killed.

---

## workspace-usuario-v2-fix2 — 2026-08-05 — fix cycle-2 — 2ed851e..b4b2e7c

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** feat/workspace-usuario-v2  
**Diff surface (cycle-2):** `frontend/src/routes/index.tsx`, `frontend/src/routes/index.test.tsx`

### Veredito: PASS

Fix cycle-2 move `AuthProvider` para dentro do data router via `AuthLayout` (paridade spec: auth dentro da árvore de rotas, não envolvendo `RouterProvider`). Novos testes cobrem boot com `AuthProvider` real, smoke `/workspace/1` sem erro `useBlocker`, e asserções estáticas anti-regressão de layout. Gates verdes @ `b4b2e7c`; sensor 5/5 killed.

---

### Task Completion (cycle-2)

| Task | Status | Notes |
| ---- | ------ | ----- |
| Fix AuthProvider placement | ✅ Done | `AuthLayout` wrapper em `routeObjects`; `RouterWithAuth` retorna só `RouterProvider` |
| Extend WKS2F2-04 static checks | ✅ Done | Assert `AuthLayout` presente; `RouterWithAuth` sem `<AuthProvider>` externo |
| Real AuthProvider boot test | ✅ Done | `useTestAuthHarness.enabled = false` → login heading @ `/` |
| Detail route smoke | ✅ Done | `/workspace/1` heading + ausência de texto `useBlocker` error |

---

### Evidência por AC (table)

| ID | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion expression | Result |
| --- | --- | --- | --- | --- |
| WKS2F2-01 | WHEN usuário clica **Abrir** no hub THEN navega para `/workspace/{id}` e renderiza heading | Heading do workspace visível; não tela branca | `WorkspaceDetailPage.integration.test.tsx:79` — `expect(await screen.findByRole('heading', { name: 'Planejamento', level: 1 })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-02 | WHEN `WorkspaceDetailPage` monta THEN console sem erro `useBlocker must be used within a data router` | Ausência do erro no `console.error` | `WorkspaceDetailPage.integration.test.tsx:80-82` — `expect(consoleError.mock.calls.some((call) => String(call[0]).includes('useBlocker must be used within a data router'))).toBe(false)` | ✅ PASS |
| WKS2F2-03 | WHEN workspace existe THEN toolbar **ou** empty state **Workspace vazio** | Botões **Adicionar widget** / **Editar layout** ou status vazio | `WorkspaceDetailPage.integration.test.tsx:90-91` — `expect(screen.getByRole('button', { name: 'Adicionar widget' })).toBeInTheDocument()`; `:100` — `expect(await screen.findByRole('status', { name: 'Workspace vazio' })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-04 | WHEN app inicia THEN root usa `RouterProvider` + `createBrowserRouter` — NOT `<BrowserRouter>` | Produção monta data router global | `index.test.tsx:120-130` — static `createBrowserRouter(routeObjects)`, no `BrowserRouter`, `AuthLayout` in `routeObjects`, `RouterWithAuth` → `RouterProvider` only; `:133-152` — mount mock; impl `routes/index.tsx:133-143` | ✅ PASS |
| WKS2F2-05 | WHEN usuário acessa rotas existentes THEN cada rota renderiza componente correto | Smoke em `/dashboard`, `/workspace`, `/workspace/datasets`, `/login`, `/usuarios` | `index.test.tsx:178-181` — Dashboard; `:183-186` — Meus workspaces; `:188-191` — Sistema de Folha; `:193-196` — Datasets; `:198-204` — Planejamento detail; `:206-210` — Manutenção de Usuários | ✅ PASS |
| WKS2F2-06 | WHEN não autenticado acessa rota privada THEN redirect `/login` | Tela de login visível | `index.test.tsx:155-168` — real `AuthProvider` boot → login heading; `:219-222` — `renderRoutes('/dashboard', { user: null, ... })` → Sistema de Folha | ✅ PASS |
| WKS2F2-07 | WHEN sem acesso workspace acessa `/workspace` THEN alerta acesso negado | Alert com texto de acesso negado; hub não renderiza | `index.test.tsx:230-237` — `expect(screen.getByRole('alert')).toHaveTextContent(/Acesso negado ao Workspace/i)`; `expect(screen.queryByRole('heading', { name: 'Meus workspaces' })).not.toBeInTheDocument()` | ✅ PASS |
| WKS2F2-08 | WHEN modo edição dirty THEN `beforeunload` listener registrado | `addEventListener('beforeunload', …)` | `useUnsavedChangesGuard.test.tsx:47` — `expect(addListener).toHaveBeenCalledWith('beforeunload', expect.any(Function))` | ✅ PASS |
| WKS2F2-09 | WHEN dirty e navegação in-app THEN `window.confirm` com mensagem padrão | `'Existem alterações não salvas. Deseja sair sem salvar?'` | `WorkspaceDetailPage.integration.test.tsx:130` — `expect(confirmSpy).toHaveBeenCalledWith('Existem alterações não salvas. Deseja sair sem salvar?')`; `useUnsavedChangesGuard.test.tsx:56` — mesma asserção | ✅ PASS |
| WKS2F2-10 | WHEN usuário cancela confirm THEN permanece na página de detalhe | Heading do workspace presente; hub destino ausente | `WorkspaceDetailPage.integration.test.tsx:139-140` — `expect(screen.getByRole('heading', { name: 'Planejamento', level: 1 })).toBeInTheDocument()`; `expect(screen.queryByRole('heading', { name: 'Hub de destino', level: 1 })).not.toBeInTheDocument()` | ✅ PASS |
| WKS2F2-11 | WHEN `dirty=false` THEN navegação sem confirm | `confirm` não chamado; destino renderizado | `WorkspaceDetailPage.integration.test.tsx:159-160` — `expect(confirmSpy).not.toHaveBeenCalled()`; `expect(await screen.findByRole('heading', { name: 'Hub de destino', level: 1 })).toBeInTheDocument()` | ✅ PASS |
| WKS2F2-12 | WHEN `npm test` THEN contagem ≥ baseline pré-fix2 (980) | ≥980 passed, 0 failed | Gate `@b4b2e7c` — Vitest: `Tests 998 passed (998)` vs baseline 980 @ `5d65deb` | ✅ PASS |
| WKS2F2-13 | WHEN `npm run build` THEN completa sem erro TS | Exit 0, bundle emitido | Gate `@b4b2e7c` — `tsc -b && vite build` exit 0 (~9s) | ✅ PASS |
| WKS2F2-14 | WHEN `npm test -- src/pages/Workspace` THEN suite 100% | 205/205 pass | Gate `@b4b2e7c` — `Test Files 32 passed (32); Tests 205 passed (205)` | ✅ PASS |

**Spec-anchored check:** 14/14 matched spec outcome

---

### Sensor de discriminação

| # | Mutation | File:line | Description | Killed? |
| --- | --- | --- | --- | --- |
| 1 | Confirm message | `useUnsavedChangesGuard.ts:4` | `DEFAULT_MESSAGE` → `'Mensagem errada'` | ✅ Killed — 2 tests failed (guard + integration) |
| 2 | Blocker polarity | `useUnsavedChangesGuard.ts:15` | `useBlocker(dirty)` → `useBlocker(!dirty)` | ✅ Killed — 3 integration tests failed |
| 3 | beforeunload | `useUnsavedChangesGuard.ts:25` | Comentou `window.addEventListener` | ✅ Killed — `useUnsavedChangesGuard.test.tsx:47` failed |
| 4 | Auth redirect | `routes/index.tsx:66` | `if (!user)` → `if (user)` | ✅ Killed — 8/11 `index.test.tsx` failed |
| 5 | Production router API | `routes/index.tsx:141` | `createBrowserRouter` → `createMemoryRouter` | ✅ Killed — 3/11 `index.test.tsx` failed (static + mount WKS2F2-04) |

**Sensor depth:** lightweight (5 mutations)  
**Result:** 5/5 killed — **PASS**

Mutantes aplicados em scratch (`cp` + `sed` + restore); working tree restaurada ao HEAD.

---

### Gate Check

| Command | Result |
| --- | --- |
| `cd frontend && npm test` | ✅ 998 passed, 0 failed, 95 files, exit 0 (~216s) |
| `cd frontend && npm run build` | ✅ exit 0 (~9s) |
| `cd frontend && npm test -- src/pages/Workspace` | ✅ 205 passed, 32 files, exit 0 (~84s) |

- **Test count before feature:** 980 @ `5d65deb`
- **Test count after cycle-1:** 996 @ `2ed851e`
- **Test count after cycle-2:** 998 @ `b4b2e7c`
- **Delta (full fix2):** +18 tests
- **Delta (cycle-2):** +2 tests
- **Skipped:** 0
- **Failures:** none

---

### Requirement Traceability (Verifier view)

| Requirement | Cycle-1 Verifier | Cycle-2 Verifier |
| --- | --- | --- |
| WKS2F2-01…03 | ✅ Verified | ✅ Verified |
| WKS2F2-04 | ✅ Verified | ✅ Verified (AuthLayout static + mount) |
| WKS2F2-05 | ✅ Verified | ✅ Verified (+ detail smoke `/workspace/1`) |
| WKS2F2-06 | ✅ Verified | ✅ Verified (+ real AuthProvider boot) |
| WKS2F2-07…11 | ✅ Verified | ✅ Verified |
| WKS2F2-12…14 | ✅ Verified | ✅ Verified |

---

### Summary

**Overall:** ✅ Ready — 14/14 ACs evidence-or-zero; gates green; sensor 5/5.

**Cycle-2 fix:** `AuthProvider` moved inside data router tree; production auth boot and detail-route smoke hardened against provider-order regressions.

**Gaps closed:** AuthProvider-outside-router architectural drift; missing route-level detail smoke in `index.test.tsx`.
