# workspace-usuario-v2-fix2 — Tasks

**Spec**: `_docs/specs/features/workspace-usuario-v2-fix2/spec.md`  
**Design**: `_docs/specs/features/workspace-usuario-v2-fix2/design.md`  
**Branch**: `feat/workspace-usuario-v2`  
**Commit prefix**: `fix2:`  
**Baseline tests (pre-fix2)**: 980 passed (93 files) @ `5d65deb`

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `frontend/AGENTS.md`, `_docs/specs/TESTING.md`, `.agents/skills/testing-a11y/SKILL.md`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Routes / router config | integration (Vitest + RouterProvider) | All routes in scope: happy + auth redirect + workspace ACL | `frontend/src/routes/**/*.test.tsx` | `cd frontend && npm test -- src/routes` |
| Page (WorkspaceDetail) integration | integration (Vitest + RouterProvider) | WKS2F2-01…03 + guard flow 09…11 without guard mock | `frontend/src/pages/Workspace/*.integration.test.tsx` | `cd frontend && npm test -- WorkspaceDetailPage.integration` |
| Hook (useUnsavedChangesGuard) | unit | 1:1 to WKS2F2-08…11; already covered | `frontend/src/pages/Workspace/hooks/*.test.tsx` | `cd frontend && npm test -- useUnsavedChangesGuard` |
| Test harness | unit compile | Helper exports usable | `frontend/src/test/renderWithDataRouter.tsx` | via integration tests |
| Regression | full suite + build | ≥980 tests; build clean | all FE | `cd frontend && npm test && npm run build` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | After T2, T3, T4, T5 (scoped tests) | `cd frontend && npm test -- <pattern>` |
| Build | After T1 (router migration) | `cd frontend && npm run build` |
| Full | After T6 (regression) | `cd frontend && npm test && npm run build` |

---

## Execution Plan

### Phase 1 — Data router migration (Foundation)

#### T1: Migrate global routing to createBrowserRouter + RouterProvider

- **Depends on**: —
- **Files**: `frontend/src/routes/index.tsx`
- **Requirement IDs**: WKS2F2-04
- **Done when**:
  - `RouterWithAuth` uses `createBrowserRouter` + `RouterProvider` — no `<BrowserRouter>`
  - `AuthProvider` wraps `RouterProvider` (order preserved)
  - All existing routes mapped to `RouteObject[]` with same paths and guard nesting
  - `npm run build` passes
- **Tests**: none (config layer — build gate)
- **Gate**: Build — `cd frontend && npm run build`
- **Commit**: `fix2(routing): migrate app to createBrowserRouter data router`

#### T2: Add route smoke tests with createMemoryRouter

- **Depends on**: T1
- **Files**: `frontend/src/routes/index.test.tsx`
- **Requirement IDs**: WKS2F2-05, WKS2F2-06, WKS2F2-07
- **Done when**:
  - Smoke: `/dashboard` renders dashboard content (mock auth user)
  - Smoke: `/workspace` renders hub heading or characteristic element
  - Unauthenticated private route redirects to `/login`
  - User without workspace access sees access denied alert on `/workspace`
  - `npm test -- src/routes/index.test` passes
- **Tests**: integration in same task
- **Gate**: Quick — `cd frontend && npm test -- src/routes/index.test`
- **Commit**: `fix2(routing): add data router smoke tests for auth and workspace ACL`

### Phase 2 — Integration test harness + detail page (Core)

#### T3: Add renderWithDataRouter test helper

- **Depends on**: T1
- **Files**: `frontend/src/test/renderWithDataRouter.tsx`
- **Requirement IDs**: (infra — supports WKS2F2-01…03)
- **Done when**:
  - Helper accepts `routes`, `initialEntries`, `authContext`, `temaId`
  - Uses `createMemoryRouter` + `RouterProvider` + `TestAuthProvider` + `ThemeProvider`
  - Exported and used by T4/T5
- **Tests**: covered by T4 consumer
- **Gate**: Quick — compile via T4 gate
- **Commit**: `fix2(test): add renderWithDataRouter harness for data router integration`

#### T4: WorkspaceDetailPage integration test without guard mock

- **Depends on**: T3
- **Files**: `frontend/src/pages/Workspace/WorkspaceDetailPage.integration.test.tsx`
- **Requirement IDs**: WKS2F2-01, WKS2F2-02, WKS2F2-03
- **Done when**:
  - Test renders detail under `RouterProvider` **without** mocking `useUnsavedChangesGuard`
  - Asserts workspace heading visible (not blank)
  - Asserts toolbar buttons OR empty state "Workspace vazio"
  - No console error about `useBlocker must be used within a data router` (spy or absence of throw)
  - `npm test -- WorkspaceDetailPage.integration` passes
- **Tests**: integration in same task
- **Gate**: Quick — `cd frontend && npm test -- WorkspaceDetailPage.integration`
- **Commit**: `fix2(workspace): add detail page integration test under data router`

#### T5: Guard navigation integration on WorkspaceDetailPage

- **Depends on**: T4
- **Files**: `frontend/src/pages/Workspace/WorkspaceDetailPage.integration.test.tsx` (extend)
- **Requirement IDs**: WKS2F2-09, WKS2F2-10, WKS2F2-11
- **Done when**:
  - Edit mode + dirty → navigate away → `window.confirm` called with default message
  - User cancels confirm → stays on detail page
  - User confirms → navigation completes
  - `dirty=false` navigation proceeds without confirm
  - WKS2F2-08 covered by existing `useUnsavedChangesGuard.test.tsx` (no duplicate required)
- **Tests**: integration in same task
- **Gate**: Quick — `cd frontend && npm test -- WorkspaceDetailPage.integration`
- **Commit**: `fix2(workspace): add unsaved changes guard integration on detail page`

### Phase 3 — Regression gate (Integration)

#### T6: Full frontend regression gate

- **Depends on**: T1–T5
- **Files**: `_docs/specs/features/workspace-usuario-v2-fix2/spec.md` (traceability update only)
- **Requirement IDs**: WKS2F2-12, WKS2F2-13, WKS2F2-14
- **Done when**:
  - `npm test` ≥ 980 tests, 0 failures
  - `npm run build` completes without TypeScript errors
  - `npm test -- src/pages/Workspace` 100% pass
  - Update requirement traceability in spec.md (Pending → Done)
- **Tests**: gate only
- **Gate**: Full — `cd frontend && npm test && npm run build && npm test -- src/pages/Workspace`
- **Commit**: `fix2(regression): confirm workspace suite and full FE gate green`

---

## Task Summary

| Task | Phase | Req IDs | Status |
| --- | --- | --- | --- |
| T1 | 1 | WKS2F2-04 | done |
| T2 | 1 | WKS2F2-05,06,07 | done |
| T3 | 2 | infra | done |
| T4 | 2 | WKS2F2-01,02,03 | done |
| T5 | 2 | WKS2F2-09,10,11 | done |
| T6 | 3 | WKS2F2-12,13,14 | done |

**Total tasks:** 6 — **1 worker batch** (phases 1–3)
