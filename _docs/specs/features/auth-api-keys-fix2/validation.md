# auth-api-keys-fix2 Validation

## Status atual

**Verdict:** PASS ✅  
**Date:** 2026-07-30  
**Latest run:** fix(cycle-1) — `c46d5b6`  
**Spec-anchored check:** 15/15 ACs matched spec-defined outcome  
**Gate:** 130 passed, 0 failed (backend 106 + frontend 24)  
**Sensor:** 4 injected (3 baseline + 1 cycle-1), 4 killed, 0 survived  
**Spec-precision gaps:** 2 flagged (non-blocking)  
**Edge-case gaps:** 0 (cycle-1 closed empty `centrosCustoIds` + login WebMvc)  
**Lessons recorded:** none (clean PASS)

---

## Execução: auth-api-keys-fix2 — 2026-07-30 — 6f949b1..f3254d3

**Spec:** `_docs/specs/features/auth-api-keys-fix2/spec.md`  
**Diff range:** `6f949b1..f3254d3` (8 commits, T1–T8)  
**Branch:** `feat/auth-api-keys`  
**Verifier:** independent sub-agent (author ≠ verifier)

---

### Task Completion

| Task | Status  | Notes |
| ---- | ------- | ----- |
| T1   | ✅ Done | FuncionarioService ACL + unit tests |
| T2   | ✅ Done | FuncionarioController wire + WebMvc parity |
| T3   | ✅ Done | UsuarioService ACL + unit tests |
| T4   | ✅ Done | UsuarioController wire + WebMvc parity |
| T5   | ✅ Done | MissingServletRequestParameterException → 400 |
| T6   | ✅ Done | axios interceptor 401-only refresh |
| T7   | ✅ Done | ApiKeys UX disabled create for ADMIN-only |
| T8   | ✅ Done | Full gate green + handoff docs |

---

### Spec-Anchored Acceptance Criteria

| AC | Criterion (WHEN → THEN) | Spec-defined outcome | `file:line` + assertion | Result |
| -- | ----------------------- | -------------------- | ----------------------- | ------ |
| FIX2-01 | Scoped `GET /funcionarios` | **200**, list contains only active employees with `centroCusto.id ∈ centrosCustoIds`; no-CC excluded | `FuncionarioServiceTest.java:72` — `assertEquals(1, result.size())`; `:73` — `assertEquals(793L, result.get(0).centroCustoId())`; `:129` — `assertEquals(0, result.size())` (no CC) | ✅ PASS |
| FIX2-02 | Scoped `GET /funcionarios/{id}` out-of-scope | **404** Not Found | `FuncionarioServiceTest.java:98` — `assertThrows(FuncionarioNotFoundException.class, …)`; `FuncionarioAclWebMvcTest.java:110` — `status().isNotFound()` | ✅ PASS |
| FIX2-03 | `acessoTotal=true` `GET /funcionarios` | Global list behavior preserved | `FuncionarioServiceTest.java:113` — `assertEquals(1, result.size())` with `contextoAcessoTotal()` stub | ✅ PASS ⚠️ |
| FIX2-04 | Bearer API Key parity on funcionários GETs | Same HTTP status + cardinality as JWT | `FuncionarioAclWebMvcTest.java:88-89` — JWT `status().isOk()` + `jsonPath("$.length()").value(1)`; `:97-98` — Bearer same status + length | ✅ PASS |
| FIX2-05 | Scoped `GET /usuarios` | **200**, only users with linked funcionário CC in scope; no-funcionario excluded | `UsuarioServiceTest.java:81` — `assertEquals(1, result.size())`; `:82` — `assertEquals(FUNCIONARIO_ID, result.get(0).funcionarioId())` | ✅ PASS |
| FIX2-06 | Scoped `GET /usuarios/{id}` out-of-scope / no funcionário | **404** | `UsuarioServiceTest.java:108` — `assertThrows(UsuarioNotFoundException.class, …)`; `UsuarioAclWebMvcTest.java:106` — `status().isNotFound()`; `:132` — login path `assertThrows(UsuarioNotFoundException.class, …)` | ✅ PASS |
| FIX2-07 | `acessoTotal=true` `GET /usuarios` | Global list preserved | `UsuarioServiceTest.java:122` — `assertEquals(1, result.size())` with `contextoAcessoTotal()` | ✅ PASS ⚠️ |
| FIX2-08 | Bearer API Key parity on usuarios GETs | Same status + cardinality as JWT | `UsuarioAclWebMvcTest.java:84-85` — JWT ok + length 1; `:93-94` — Bearer ok + length 1 | ✅ PASS |
| FIX2-09 | `GET /beneficio-mensal` without required params | **400** standardized body, NOT 500 | `GlobalExceptionHandlerTest.java:218` — `assertEquals(HttpStatus.BAD_REQUEST, …)`; `:220` — `assertEquals(400, response.getBody().status())`; `BeneficioMensalControllerWebMvcTest.java:91` — `status().isBadRequest()`; `:92` — `jsonPath("$.status").value(400)` | ✅ PASS |
| FIX2-10 | Same invalid request with Bearer API Key | **400** | `BeneficioMensalControllerWebMvcTest.java:106` — `status().isBadRequest()`; `:107` — `jsonPath("$.status").value(400)` | ✅ PASS |
| FIX2-11 | HTTP **401** response | Interceptor tries refresh; logout on refresh failure | `api.test.ts:60` — `expect(refreshCallCount).toBe(1)`; `:76-80` — tokens cleared + `logoutListener.toHaveBeenCalledTimes(1)` on refresh fail | ✅ PASS |
| FIX2-12 | HTTP **403** response | NO refresh, NO logout; reject to caller | `api.test.ts:160` — `expect(refreshCallCount).toBe(0)`; `:161-162` — tokens preserved; `:163` — `expect(logoutListener).not.toHaveBeenCalled()` | ✅ PASS |
| FIX2-13 | Vitest simulates **403** on `POST /auth/api-keys` | Tokens remain; `auth:logout` NOT dispatched | `api.test.ts:188` — `expect(refreshCallCount).toBe(0)`; `:189` — `expect(TokenService.getToken()).toBe('old-access-token')`; `:190` — `expect(logoutListener).not.toHaveBeenCalled()` | ✅ PASS |
| FIX2-14 | User has ADMIN but not API_KEY | Create controls disabled + permission message | `ApiKeys.test.tsx:66` — `expect(…'nova api key'…).toBeDisabled()`; `:70` — `getByText('Conceda a permissão API_KEY…')` | ✅ PASS |
| FIX2-15 | User has API_KEY | Create remains enabled (no regression) | `ApiKeys.test.tsx:80` — `expect(…'nova api key'…).toBeEnabled()`; `:84-85` — warning absent | ✅ PASS |

**Status:** ✅ 15/15 ACs covered with spec-matching assertions

**Spec-precision gaps (non-blocking):**

| AC | Gap |
| -- | --- |
| FIX2-03 | Unit test asserts delegation returns mocked count (1), not multi-record global cardinality vs scoped deny |
| FIX2-07 | Same as FIX2-03 for usuarios |

---

### Discrimination Sensor

Scratch mutations applied and discarded (working tree verified clean post-sensor).

| # | File:line | Mutation | Tests run | Killed? |
| - | --------- | -------- | --------- | ------- |
| 1 | `FuncionarioService.java:92` | Bypass ACL filter: `.filter(f -> aplicarFiltroAcesso…)` → `.filter(f -> true)` | `FuncionarioServiceTest#listarParaUsuario_scoped_filtraPorCentrosCusto`, `#listarParaUsuario_semCentroCusto_excluiFuncionario` | ✅ Killed (BUILD FAILURE, 2 errors) |
| 2 | `GlobalExceptionHandler.java:146-153` | Removed `MissingServletRequestParameterException` handler | `GlobalExceptionHandlerTest#handleMissingServletRequestParameterException_retorna400`, `BeneficioMensalControllerWebMvcTest#listarPorCompetencia_semParams_retorna400` | ✅ Killed (compile failure — handler method missing) |
| 3 | `api.ts:43` | Treat 403 like 401: `status === 401 \|\| status === 403` | `npm test -- api.test -t "403"` | ✅ Killed (2 failed: refreshCallCount expected 0 got 1) |

**Sensor depth:** lightweight (3 behavior-level faults)  
**Result:** 3/3 killed — PASS ✅

---

### Edge Cases (from spec.md)

| Edge case | Covered? | Evidence |
| --------- | -------- | -------- |
| Scoped user with empty `centrosCustoIds` → **200** empty list (funcionários + usuarios) | ⚠️ Partial | Implementation: `FuncionarioService.java:79-80`, `UsuarioService.java:94` (`centrosVazios` → `List.of()`). **No dedicated unit test** with `contextoRestrito(Collections.emptySet())` |
| Query `centroCustoId` on `/funcionarios` outside scope → **200** empty | ✅ | `FuncionarioServiceTest.java:85` — `assertEquals(0, result.size())`; `:86` — `verify(funcionarioRepository, never()).findByFiltros(…)` |
| JWT and API Key same U in-scope → equivalent payloads (same IDs) | ✅ | `FuncionarioAclWebMvcTest.java:90-91` / `:99-100` — same `$[0].id` + `$[0].nome`; `UsuarioAclWebMvcTest.java:86-87` / `:95-96` — same `$[0].id` + `$[0].login` |
| Out-of-scope → **404** for both JWT and Bearer | ✅ | WebMvc 404 tests for JWT + Bearer on both controllers |
| Write-guard `POST /funcionarios` via API Key → **403** (regression) | ⏭️ N/A | Out of fix2 prod scope per spec; not in Full gate |
| `GET /usuarios/login/{login}` same ACL as FIX2-06 | ⚠️ Partial | Service: `UsuarioServiceTest.java:132` — `assertThrows(UsuarioNotFoundException.class, …)`. **No WebMvc test** for `/usuarios/login/{login}` |

---

### Gate Check

**Command (backend):**
```bash
cd backend && mvn test -Dtest=FuncionarioServiceTest,FuncionarioAclWebMvcTest,UsuarioServiceTest,UsuarioAclWebMvcTest,GlobalExceptionHandlerTest,BeneficioMensalControllerWebMvcTest,ModularArchitectureTest
```

**Command (frontend):**
```bash
cd frontend && npm test -- api.test ApiKeys
```

| Layer | Passed | Failed | Skipped |
| ----- | ------ | ------ | ------- |
| Backend (7 classes) | 101 | 0 | 0 |
| Frontend (2 files) | 24 | 0 | 0 |
| **Total** | **125** | **0** | **0** |

Per-class backend breakdown: FuncionarioServiceTest 18, FuncionarioAclWebMvcTest 5, UsuarioServiceTest 31, UsuarioAclWebMvcTest 5, BeneficioMensalControllerWebMvcTest 4, GlobalExceptionHandlerTest 20, ModularArchitectureTest 18.

---

### Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ |
| No scope creep | ✅ |
| Matches existing patterns (BeneficioMensal ACL, ApiKeyAclWebMvcTest template) | ✅ |
| Spec-anchored outcome check | ✅ 15/15 |
| Per-layer coverage expectation (tasks.md matrix) | ✅ |
| Guidelines: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` AD-004 | ✅ |

---

### Ranked Gaps (non-blocking — PASS with flags)

1. **Edge: empty `centrosCustoIds` → 200 []** — spec edge case; implementation present (`centrosVazios`) but no unit test in `FuncionarioServiceTest` / `UsuarioServiceTest` — severity: Minor
2. **Edge: `GET /usuarios/login/{login}` WebMvc 404** — service covered (`UsuarioServiceTest:132`); no controller-level WebMvc — severity: Minor
3. **Spec-precision: FIX2-03/07 global cardinality** — tests prove delegation path, not multi-record global vs scoped contrast — severity: Cosmetic

---

### Requirement Traceability Update

| Requirement | Previous | New |
| ----------- | -------- | --- |
| FIX2-01 … FIX2-15 | Done (Execute) | ✅ Verified |
| APIKEY-05 (parent) | Verified fix1 smoke folha | Verified fix2 cadastro (per T8 handoff) |

---

### Summary

**Overall:** ✅ Ready

**What works:**
- ACL read path on `GET /funcionarios` and `GET /usuarios` (scoped filter, 404 out-of-scope, acessoTotal bypass, API Key parity)
- `GET /beneficio-mensal` without params returns **400** (JWT + Bearer)
- axios interceptor refreshes only on **401**; **403** preserves session
- ApiKeys UI disables create for ADMIN without `API_KEY` permission

**Issues found:** None blocking. Two edge-case test gaps and two spec-precision notes above.

**Next steps:** Feature ready for merge. Optional follow-up: add unit tests for empty `centrosCustoIds` edge case.

**Lessons:** Clean PASS — no `scripts/lessons.py add` invoked.

---

## Execução: auth-api-keys-fix2 — fix(cycle-1) — 2026-07-30 — c46d5b6

**Spec:** `_docs/specs/features/auth-api-keys-fix2/spec.md`  
**Diff range:** `6f949b1..c46d5b6` (9 commits, T1–T8 + fix cycle 1)  
**Fix commit:** `c46d5b6` — edge-case tests for empty `centrosCustoIds` and login ACL WebMvc  
**Branch:** `feat/auth-api-keys`  
**Verifier:** independent sub-agent (author ≠ verifier)

---

### Fix Cycle 1 — Scope

| Gap (from prior run) | Addressed? | Evidence |
| -------------------- | ---------- | -------- |
| Empty `centrosCustoIds` → **200** `[]` (funcionários) | ✅ | `FuncionarioServiceTest.java:77-87` — `listarParaUsuario_scoped_centrosCustoIdsVazio_retornaListaVazia`; `assertEquals(0, result.size())`; `verify(funcionarioRepository, never()).findByFiltros(…)` |
| Empty `centrosCustoIds` → **200** `[]` (usuarios) | ✅ | `UsuarioServiceTest.java:86-95` — `listarParaUsuario_scoped_centrosCustoIdsVazio_retornaListaVazia`; `assertEquals(0, result.size())`; `verify(usuarioRepository, never()).findByFiltros(…)` |
| `GET /usuarios/login/{login}` WebMvc ACL (FIX2-06 parity) | ✅ | `UsuarioAclWebMvcTest.java:122-128` — JWT out-of-scope `status().isNotFound()`; `:131-138` — Bearer same; `:143-159` — JWT + Bearer parity on `$[0].id` + `$[0].login` |

---

### Spec-Anchored Acceptance Criteria

**Status:** ✅ 15/15 ACs — unchanged from baseline; edge-case evidence strengthened for FIX2-05/06 scope paths.

**Spec-precision gaps (non-blocking, unchanged):**

| AC | Gap |
| -- | --- |
| FIX2-03 | Unit test asserts delegation returns mocked count (1), not multi-record global cardinality vs scoped deny |
| FIX2-07 | Same as FIX2-03 for usuarios |

---

### Discrimination Sensor (cycle-1 add-on)

Scratch mutation applied and discarded (working tree verified clean post-sensor).

| # | File:line | Mutation | Tests run | Killed? |
| - | --------- | -------- | --------- | ------- |
| 4 | `FuncionarioService.java:79-81` | Bypass `centrosVazios` early return (commented guard) | `FuncionarioServiceTest#listarParaUsuario_scoped_centrosCustoIdsVazio_retornaListaVazia` | ✅ Killed (1 failure — `findByFiltros` invoked when `never()` expected) |

**Sensor depth:** lightweight (1 behavior-level fault on new test)  
**Cumulative:** 4/4 killed — PASS ✅

---

### Edge Cases (from spec.md) — post cycle-1

| Edge case | Covered? | Evidence |
| --------- | -------- | -------- |
| Scoped user with empty `centrosCustoIds` → **200** empty list (funcionários + usuarios) | ✅ | `FuncionarioServiceTest.java:77-87`; `UsuarioServiceTest.java:86-95`; impl `FuncionarioService.java:79-80`, `UsuarioService.java:94` |
| Query `centroCustoId` on `/funcionarios` outside scope → **200** empty | ✅ | `FuncionarioServiceTest.java:90` — unchanged from baseline |
| JWT and API Key same U in-scope → equivalent payloads (same IDs) | ✅ | Extended to login path: `UsuarioAclWebMvcTest.java:143-159` |
| Out-of-scope → **404** for both JWT and Bearer | ✅ | Login path added: `UsuarioAclWebMvcTest.java:122-138` |
| Write-guard `POST /funcionarios` via API Key → **403** (regression) | ⏭️ N/A | Out of fix2 prod scope per spec |
| `GET /usuarios/login/{login}` same ACL as FIX2-06 | ✅ | Service: `UsuarioServiceTest.java:132`; WebMvc: `UsuarioAclWebMvcTest.java:122-159` |

---

### Gate Check

**Command (backend):**
```bash
cd backend && mvn test -Dtest=FuncionarioServiceTest,FuncionarioAclWebMvcTest,UsuarioServiceTest,UsuarioAclWebMvcTest,GlobalExceptionHandlerTest,BeneficioMensalControllerWebMvcTest,ModularArchitectureTest
```

**Command (frontend):**
```bash
cd frontend && npm test -- api.test ApiKeys
```

| Layer | Passed | Failed | Skipped |
| ----- | ------ | ------ | ------- |
| Backend (7 classes) | 106 | 0 | 0 |
| Frontend (2 files) | 24 | 0 | 0 |
| **Total** | **130** | **0** | **0** |

Per-class backend breakdown: FuncionarioServiceTest **19** (+1), FuncionarioAclWebMvcTest 5, UsuarioServiceTest **32** (+1), UsuarioAclWebMvcTest **8** (+3), BeneficioMensalControllerWebMvcTest 4, GlobalExceptionHandlerTest 20, ModularArchitectureTest 18.

Delta vs baseline (`f3254d3`): **+5 tests** (2 unit empty-scope + 3 WebMvc login ACL).

---

### Ranked Gaps (non-blocking — PASS with flags)

1. **Spec-precision: FIX2-03/07 global cardinality** — tests prove delegation path, not multi-record global vs scoped contrast — severity: Cosmetic

---

### Summary

**Overall:** ✅ PASS — fix cycle 1 closed both edge-case gaps from prior run.

**What changed in c46d5b6:**
- Unit tests for empty `centrosCustoIds` on funcionários and usuarios list paths
- WebMvc tests for `GET /usuarios/login/{login}` — 404 JWT/Bearer out-of-scope + in-scope parity

**Issues found:** None blocking. Two spec-precision notes remain (cosmetic).

**Next steps:** Feature ready for merge.

**Lessons:** Clean PASS — no `scripts/lessons.py add` invoked.
