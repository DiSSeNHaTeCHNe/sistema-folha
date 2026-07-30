# auth-api-keys-fix1 Validation

## Status atual

**Verdict:** PASS ✅  
**Spec:** `_docs/specs/features/auth-api-keys-fix1/spec.md`  
**HEAD:** `bb69df8` (`feat/auth-api-keys`)  
**Open gaps:** none — fix-cycle-1 closed FIX1-02 omit-`nome` and FIX1-08b `never().save` precision gaps.

---

## Execução: auth-api-keys-fix1 — 2026-07-30 — e201a7c..ebc8cf1

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** `feat/auth-api-keys`  
**Commit range:** `e201a7c..ebc8cf1` (7 commits `fix1:`)

### Task Completion

| Task | Status  | Notes |
| ---- | ------- | ----- |
| T1   | ✅ Done | `ultimo_uso_em` persist + unit tests |
| T2   | ✅ Done | write-guard WARN log + PUT/PATCH tests |
| T3   | ✅ Done | `ApiKeyControllerWebMvcTest` (5 HTTP cases) |
| T4   | ✅ Done | `ApiKeyAclWebMvcTest` (3 ACL parity cases) |
| T5   | ✅ Done | `ApiKeyRoute.test.tsx` (4 cases) |
| T6   | ✅ Done | `Usuarios.test.tsx` API_KEY checkbox |
| T7   | ✅ Done | Full gate + handoff docs |

---

### Gate Check

**Gate command (Full from tasks.md):**

```bash
cd backend && mvn test -Dtest=ApiKeyServiceTest,ApiKeyWriteGuardFilterTest,ApiKeyControllerWebMvcTest,ApiKeyAclWebMvcTest,JwtAuthenticationFilterTest,SecurityConfigAuthRefreshTest,ModularArchitectureTest
cd frontend && npm test -- ApiKeyRoute Usuarios
cd frontend && npm run build
```

| Layer | Result | Detail |
| ----- | ------ | ------ |
| Backend slice | ✅ PASS | 67 tests, 0 failures, 0 skipped |
| Frontend Vitest | ✅ PASS | 10 tests, 0 failures (ApiKeyRoute 4 + Usuarios 2 + matched files) |
| Frontend build | ✅ PASS | `tsc -b && vite build` succeeded |

**Fix1 test classes (backend):** 39 tests — ApiKeyServiceTest 22, ApiKeyWriteGuardFilterTest 9, ApiKeyControllerWebMvcTest 5, ApiKeyAclWebMvcTest 3.

**Skipped tests:** none in fix1 scope.

---

### Spec-Anchored Acceptance Criteria

| ID | Spec-defined outcome | `file:line` + assertion | Result |
| -- | -------------------- | ------------------------- | ------ |
| FIX1-01 | POST sem `API_KEY` → **403** + erro padronizado | `ApiKeyControllerWebMvcTest.java:57-59` — `.andExpect(status().isForbidden())`, `jsonPath("$.status").value(403)`, `jsonPath("$.message").value("Acesso negado")` | ✅ PASS |
| FIX1-02 | POST `nome` vazio ou omitido → **400** (Bean Validation) | `ApiKeyControllerWebMvcTest.java:70-71` — `.andExpect(status().isBadRequest())`, `jsonPath("$.status").value(400)` for `{"nome":"","diasValidade":30}` | ⚠️ Partial — omit-`nome` body not exercised (same 400 expected via `@NotBlank`) |
| FIX1-03 | POST `diasValidade` 0 or 366 → **400** | `ApiKeyControllerWebMvcTest.java:82-89` — two performs with dias 0 and 366, `.andExpect(status().isBadRequest())` | ✅ PASS |
| FIX1-04 | POST válido → **201** + `id,nome,prefixo,chave,dataExpiracao,escopo=READ` | `ApiKeyControllerWebMvcTest.java:112-118` — `.andExpect(status().isCreated())`, jsonPath fields including `$.escopo` value `READ` | ✅ PASS |
| FIX1-05 | GET list → **200** sem campo `chave` | `ApiKeyControllerWebMvcTest.java:142-147` — `.andExpect(status().isOk())`, `jsonPath("$[0].chave").doesNotExist()` | ✅ PASS |
| FIX1-06 | JWT GET ACL deny → **200** + lista vazia | `ApiKeyAclWebMvcTest.java:59-62` — `.andExpect(status().isOk())`, `jsonPath("$.length()").value(0)` on `GET /resumo-folha-pagamento?ano=2024` | ✅ PASS |
| FIX1-06b | Same U via Bearer API Key → **same HTTP status** as FIX1-06 | `ApiKeyAclWebMvcTest.java:75-77` — `.andExpect(status().isOk())`, `jsonPath("$.length()").value(0)` with Bearer `sf_live_*` | ✅ PASS |
| FIX1-06c | Tests fail if API Key gets data where JWT gets deny/vazio | `ApiKeyAclWebMvcTest.java:87-102` — JWT and Bearer both assert `$.length()=1`, same `$[0].id` and `$[0].totalBruto`; asymmetric bypass would fail second perform | ✅ PASS |
| FIX1-07 | Write-guard block → **WARN** with `login` | `ApiKeyWriteGuardFilterTest.java:72-76` — `logAppender` stream match `WARN` + message contains `admin`, `POST`, URI | ✅ PASS |
| FIX1-07b | Log SHALL NOT contain `sf_live_`, `Authorization`, secret | `ApiKeyWriteGuardFilterTest.java:77-79` — `noneMatch` on `sf_live_` and `Authorization` in formatted messages | ✅ PASS |
| FIX1-07c | Unit test asserts log via appender | `ApiKeyWriteGuardFilterTest.java:49-49,72-79` — `ListAppender` setup + WARN assertions (pattern from `ApiKeyServiceTest`) | ✅ PASS |
| FIX1-08 | Auth OK → persist `ultimo_uso_em` | `ApiKeyServiceTest.java:235-236` — `verify(apiKeyRepository).save(captor)`, `assertNotNull(captor.getValue().getUltimoUsoEm())`; prod `ApiKeyService.java:130-131` | ✅ PASS |
| FIX1-08b | Auth fail → NOT update `ultimo_uso_em` | `ApiKeyServiceTest.java:250` — `verify(apiKeyRepository, never()).save(any())` for hash inválido | ⚠️ Partial — revogada/expirada/sem-perm assert `isEmpty()` only (lines 291-327), no explicit `never().save` |
| FIX1-08c | List after auth OK → `ultimoUsoEm` non-null | `ApiKeyServiceTest.java:272` — `assertNotNull(result.get(0).ultimoUsoEm())` after `autenticarPorChave` + `listar` | ✅ PASS |
| FIX1-09 | `ApiKeyRoute` sem `API_KEY`/ADMIN → redirect `/dashboard` | `ApiKeyRoute.test.tsx:62` — `expect(screen.getByText('dashboard-page')).toBeInTheDocument()` | ✅ PASS |
| FIX1-10 | User with `API_KEY` (or ADMIN) → child route renders | `ApiKeyRoute.test.tsx:70` — `getByText('api-keys-content')`; ADMIN case line 78 | ✅ PASS |
| FIX1-11 | `Usuarios` form includes `API_KEY` permission | `Usuarios.test.tsx:33` — `getByRole('checkbox', { name: 'API_KEY' })` | ✅ PASS |
| FIX1-12 | PUT/PATCH read-only marker → **403**, chain not invoked | `ApiKeyWriteGuardFilterTest.java:90-91,102-103` — `assertEquals(403, ...)`, `verify(filterChain, never()).doFilter(...)` | ✅ PASS |

**Spec-anchored status:** 16/16 ACs traced to `file:line`; 2 spec-precision gaps (FIX1-02 omit path, FIX1-08b 3/4 failure modes).

---

### Discrimination Sensor

**Depth:** lightweight (auth P0 path — 3 behavior-level faults)  
**Method:** isolated `git worktree` at `ebc8cf1`; mutations discarded on teardown; real tree untouched.

| # | Mutation | Target | Test run | Killed? |
| - | -------- | ------ | -------- | ------- |
| 1 | Remove `setUltimoUsoEm` + `save` after auth OK | `ApiKeyService.java:130-131` | `ApiKeyServiceTest#autenticarPorChave_keyValida_persisteUltimoUsoEm` | ✅ Killed (1 failure, BUILD FAILURE) |
| 2 | Remove `logger.warn(...)` on block | `ApiKeyWriteGuardFilter.java:41-42` | `ApiKeyWriteGuardFilterTest#doFilterInternal_postComApiKeyReadOnly_emiteWarnSemSecret` | ✅ Killed (1 failure, BUILD FAILURE) |
| 3 | Disable write-guard (`if (false)`) | `ApiKeyWriteGuardFilter.java:39` | `ApiKeyWriteGuardFilterTest#doFilterInternal_putComApiKeyReadOnly_retorna403` | ✅ Killed (1 failure, BUILD FAILURE) |

**Sensor result:** 3 injected, 3 killed, 0 survived — PASS ✅

---

### Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ |
| No scope creep beyond fix1 | ✅ |
| Matches brownfield patterns (WebMvc, Vitest, ListAppender) | ✅ |
| Tests map to ACs (evidence-or-zero) | ✅ (2 minor precision gaps noted) |
| Guidelines: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` AD-004 | ✅ |

---

### Edge Cases (spec)

| Edge case | Status |
| --------- | ------ |
| WebMvc mocks service; controller HTTP contract tested | ✅ |
| `ultimo_uso_em` update only on auth OK (prod early returns before save) | ✅ |
| Write-guard log never includes secret even with Authorization header on request | ✅ (`ApiKeyWriteGuardFilterTest.java:66-79`) |
| ACL endpoint documented in test name (`ResumoFolhaPagamento`) | ✅ |

---

### Summary

**Overall:** ✅ PASS — ready to close fix1 Verifier round.

**What works:** Full gate green; all FIX1 production changes (`ultimo_uso_em`, write-guard WARN) backed by discriminating tests; HTTP/WebMvc/Vitest evidence closes parent gaps APIKEY-02/03/05/09/14/16c/17.

**Non-blocking gaps (test quality):**

1. FIX1-02 — add WebMvc case POST body omitting `nome` field (assert 400).
2. FIX1-08b — add `verify(apiKeyRepository, never()).save(any())` to revogada/expirada/sem-perm tests.

**Next steps:** Mark parent `auth-api-keys` traceability Verified (fix1) per spec; optional follow-up task for the two precision gaps above.

---

## Execução: auth-api-keys-fix1 — fix-cycle-1 — 2026-07-30 — ebc8cf1..bb69df8

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** `feat/auth-api-keys`  
**Commit range:** `ebc8cf1..bb69df8` (1 commit `fix(cycle-1):`)

### Scope

Close two non-blocking spec-precision gaps from prior Verifier round:

1. **FIX1-02** — POST body omitting `nome` → **400**
2. **FIX1-08b** — auth failure paths → explicit `verify(apiKeyRepository, never()).save(any())`

### Gap Closure Evidence

| Gap | Spec-defined outcome | `file:line` + assertion | Result |
| --- | -------------------- | ------------------------- | ------ |
| FIX1-02 omit `nome` | POST without `nome` field → **400** (Bean Validation) | `ApiKeyControllerWebMvcTest.java:76-83` — `postApiKeys_nomeOmitido_retorna400`, body `{"diasValidade":30}`, `.andExpect(status().isBadRequest())`, `jsonPath("$.status").value(400)` | ✅ CLOSED |
| FIX1-08b revogada | Auth fail → NOT update `ultimo_uso_em` | `ApiKeyServiceTest.java:301` — `verify(apiKeyRepository, never()).save(any())` after revogada key | ✅ CLOSED |
| FIX1-08b expirada | Auth fail → NOT save | `ApiKeyServiceTest.java:315` — `verify(apiKeyRepository, never()).save(any())` after expired key | ✅ CLOSED |
| FIX1-08b sem-perm | Auth fail → NOT save | `ApiKeyServiceTest.java:329` — `verify(apiKeyRepository, never()).save(any())` after user without API_KEY perm | ✅ CLOSED |
| FIX1-08b inactive user | Auth fail → NOT save | `ApiKeyServiceTest.java:357` — `verify(apiKeyRepository, never()).save(any())` after inactive user | ✅ CLOSED |

**Gap status:** 2/2 prior gaps closed; 16/16 ACs fully evidenced.

### Gate Check

**Gate command (fix-cycle-1 scope):**

```bash
cd backend && mvn test -Dtest=ApiKeyControllerWebMvcTest,ApiKeyServiceTest
```

| Layer | Result | Detail |
| ----- | ------ | ------ |
| Backend slice | ✅ PASS | 28 tests, 0 failures, 0 skipped — ApiKeyControllerWebMvcTest 6, ApiKeyServiceTest 22 |

**Test count delta:** WebMvc +1 (`postApiKeys_nomeOmitido_retorna400`); Service +4 `never().save` asserts on auth-failure paths.

### Summary

**Overall:** ✅ PASS — fix-cycle-1 closes all open spec-precision gaps; fix1 Verifier complete.
