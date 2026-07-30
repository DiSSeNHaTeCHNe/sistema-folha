# auth-api-keys-fix1 Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/auth-api-keys-fix1/design.md`  
**Spec**: `_docs/specs/features/auth-api-keys-fix1/spec.md`  
**Parent**: `_docs/specs/features/auth-api-keys/` (branch `feat/auth-api-keys`)  
**Status**: Execute complete 2026-07-30 — T1–T7 (`e201a7c..ebc8cf1`) + fix-cycle-1 (`bb69df8`); Verifier PASS  
**Commits**: prefixo `fix1:` (ex.: `fix1: persist ultimo_uso_em on API key auth`)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` (AD-004 brownfield Vitest), `auth-api-keys-fix1/spec.md` (FIX1-01…12), skill `spring-security`, skill `testing-a11y`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| `ApiKeyService.autenticarPorChave` | unit | FIX1-08/08b/08c: save `ultimoUsoEm` on OK; no save on fail; list DTO populated | `**/auth/application/ApiKeyServiceTest.java` | `cd backend && mvn test -Dtest=ApiKeyServiceTest` |
| `ApiKeyWriteGuardFilter` | unit | FIX1-07/07b/07c WARN log + no secret; FIX1-12 PUT/PATCH→403 | `**/security/ApiKeyWriteGuardFilterTest.java` | `cd backend && mvn test -Dtest=ApiKeyWriteGuardFilterTest` |
| `ApiKeyController` HTTP | WebMvc (`@WebMvcTest`) | FIX1-01…05: POST 403/400/201, GET 200 sem secret | `**/auth/api/ApiKeyControllerWebMvcTest.java` | `cd backend && mvn test -Dtest=ApiKeyControllerWebMvcTest` |
| ACL paridade JWT vs API Key | WebMvc | FIX1-06/06b/06c: `GET /resumo-folha-pagamento?ano=2024` mesmo status JWT vs Bearer | `**/auth/api/ApiKeyAclWebMvcTest.java` (ou `folha/api/`) | `cd backend && mvn test -Dtest=ApiKeyAclWebMvcTest` |
| `ApiKeyRoute` | Vitest unit | FIX1-09 redirect; FIX1-10 allow API_KEY/ADMIN | `frontend/src/routes/ApiKeyRoute.test.tsx` | `cd frontend && npm test -- ApiKeyRoute` |
| `Usuarios` perm chip | Vitest unit | FIX1-11: label/checkbox `API_KEY` visível no form | `frontend/src/pages/Usuarios/Usuarios.test.tsx` | `cd frontend && npm test -- Usuarios` |
| Regressão JWT filter | unit | Inalterado (parent APIKEY-07/08) | `**/JwtAuthenticationFilterTest.java` | incluído no Full gate |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | T1, T2 (service/filter) | `cd backend && mvn test -Dtest=ApiKeyServiceTest,ApiKeyWriteGuardFilterTest` |
| WebMvc | T3, T4 | `cd backend && mvn test -Dtest=ApiKeyControllerWebMvcTest,ApiKeyAclWebMvcTest` |
| FE | T5, T6 | `cd frontend && npm test -- ApiKeyRoute Usuarios` |
| Full | T7 fechamento | Quick + WebMvc + FE + `cd backend && mvn test -Dtest=JwtAuthenticationFilterTest,SecurityConfigAuthRefreshTest,ModularArchitectureTest` + `cd frontend && npm run build` |

---

## Execution Plan

### Phase 1: Produção + testes unitários backend

```
T1 → T2
```

### Phase 2: Evidência HTTP WebMvc

```
T3 → T4
```

### Phase 3: Vitest frontend

```
T5 → T6
```

### Phase 4: Gate + handoff

```
T7
```

**Batch packing (~7 tasks/worker, whole phases):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | Phase 1–4 | T1–T7 | 7 |

→ **1 batch / execução inline** (≤ ~8 tasks). Sub-agents opcionais; override do projeto pode despachar 1 worker com T1–T7.

---

## Task Breakdown

### T1: `ultimo_uso_em` em `autenticarPorChave` + unit tests

**What**: Persistir `ultimo_uso_em` após auth OK; não atualizar em falha; assert em list DTO.  
**Where**:
- `backend/src/main/java/.../auth/application/ApiKeyService.java`
- `backend/src/test/java/.../auth/application/ApiKeyServiceTest.java`

**Depends on**: None (MVP já em `feat/auth-api-keys`)  
**Reuses**: `ApiKeyServiceTest` (ArgumentCaptor, fixtures existentes)  
**Requirement**: FIX1-08, FIX1-08b, FIX1-08c

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [x] `@Transactional` em `autenticarPorChave`; `save` com `ultimoUsoEm` após validações OK
- [x] ≥2 testes novos: auth OK → captor `ultimoUsoEm != null`; falha (revogada/hash/sem perm) → sem save indevido
- [x] Gate Quick: `ApiKeyServiceTest` passa; contagem testes ≥ baseline + 2
**Commit**: `5ced28c`

**Tests**: unit  
**Gate**: Quick  
**Commit**: `fix1: persist ultimo_uso_em on successful API key auth`

---

### T2: Log write-guard + PUT/PATCH tests

**What**: WARN log em bloqueio 403 (login, method, uri — sem secret); casos PUT/PATCH no filter test.  
**Where**:
- `backend/src/main/java/.../security/ApiKeyWriteGuardFilter.java`
- `backend/src/test/java/.../security/ApiKeyWriteGuardFilterTest.java`

**Depends on**: None  
**Reuses**: `ListAppender` pattern de `ApiKeyServiceTest`; casos POST/DELETE existentes  
**Requirement**: FIX1-07, FIX1-07b, FIX1-07c, FIX1-12

**Tools**: MCP NONE; Skill `spring-security`

**Done when**:
- [x] Log WARN antes de `sendError(403)`; mensagem contém login; **não** contém `sf_live_` nem `Authorization`
- [x] Teste appender asserta WARN + ausência de secret
- [x] ≥2 testes PUT e PATCH → 403; `filterChain` never invoked
- [x] Gate Quick: `ApiKeyWriteGuardFilterTest` passa
**Commit**: `8f54935`

**Tests**: unit  
**Gate**: Quick  
**Commit**: `fix1: log API key write-guard blocks and cover PUT/PATCH`

---

### T3: `ApiKeyControllerWebMvcTest`

**What**: Suite WebMvc com 5 casos HTTP em `/auth/api-keys`.  
**Where**: `backend/src/test/java/.../auth/api/ApiKeyControllerWebMvcTest.java`  
**Depends on**: None (handler 403/400 já existem pós fix-cycle-1)  
**Reuses**: `SecurityConfigAuthRefreshTest` (`@WebMvcTest`, `@Import(SecurityConfig, GlobalExceptionHandler)`, mocks padrão)  
**Requirement**: FIX1-01, FIX1-02, FIX1-03, FIX1-04, FIX1-05

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [x] POST sem `API_KEY` → **403**
- [x] POST `nome` vazio → **400** (Bean Validation)
- [x] POST `diasValidade` 0 ou 366 → **400**
- [x] POST válido → **201** + jsonPath `escopo=READ`, campos create
- [x] GET list → **200**; body **sem** campo `chave`
- [x] Gate WebMvc: `mvn test -Dtest=ApiKeyControllerWebMvcTest` — ≥5 testes
**Commit**: `0784226`

**Tests**: WebMvc  
**Gate**: WebMvc  
**Commit**: `fix1: add ApiKeyController WebMvc HTTP contract tests`

---

### T4: `ApiKeyAclWebMvcTest` (paridade JWT vs Bearer)

**What**: Smoke ACL HTTP em `GET /resumo-folha-pagamento?ano=2024` — JWT vs Bearer API Key mesmo status/body.  
**Where**: `backend/src/test/java/.../folha/api/ApiKeyAclWebMvcTest.java` (ou `auth/api/` se preferir co-location auth)  
**Depends on**: None  
**Reuses**: `OrganogramaControllerWebMvcTest` mock set; design endpoint fixo  
**Requirement**: FIX1-06, FIX1-06b, FIX1-06c

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [x] JWT `@WithMockUser("gestor")` + stub service lista vazia → **200** + `[]`
- [x] Bearer `sf_live_*` + mock `ApiKeyService.autenticarPorChave` → mesmo **200** + `[]`
- [x] Caso discriminação: paridade simétrica quando stub retorna dados (JWT e API Key iguais)
- [x] Nomes de teste incluem `ResumoFolhaPagamento`
- [x] Gate WebMvc: `mvn test -Dtest=ApiKeyAclWebMvcTest` — ≥3 testes
**Commit**: `d6be86f`

**Tests**: WebMvc  
**Gate**: WebMvc  
**Commit**: `fix1: add JWT vs API key ACL HTTP parity test`

---

### T5: `ApiKeyRoute.test.tsx`

**What**: Vitest route guard — redirect sem perm; allow `API_KEY` e `ADMIN`; loading.  
**Where**: `frontend/src/routes/ApiKeyRoute.test.tsx`  
**Depends on**: None  
**Reuses**: `AdminRoute.test.tsx` (mock `useAuth`, `MemoryRouter`)  
**Requirement**: FIX1-09, FIX1-10

**Tools**: MCP NONE; Skill `testing-a11y`

**Done when**:
- [x] User sem `API_KEY`/ADMIN → redirect `/dashboard`
- [x] User com `API_KEY` → child renderizado
- [x] User `ADMIN` sem `API_KEY` → child renderizado
- [x] Loading → `progressbar`
- [x] Gate FE: `npm test -- ApiKeyRoute` — ≥4 testes
**Commit**: `7432576`

**Tests**: Vitest unit  
**Gate**: FE  
**Commit**: `fix1: add ApiKeyRoute vitest guard tests`

---

### T6: Assert `API_KEY` em `Usuarios.test.tsx`

**What**: Teste que abre form novo usuário e asserta opção `API_KEY` por role/label.  
**Where**: `frontend/src/pages/Usuarios/Usuarios.test.tsx`  
**Depends on**: None  
**Reuses**: `renderWithProviders`, mocks `usuarioService` existentes  
**Requirement**: FIX1-11

**Tools**: MCP NONE; Skill `testing-a11y`

**Done when**:
- [x] Após abrir "Novo usuário", permissão **API_KEY** visível (getByRole/label)
- [x] Gate FE: `npm test -- Usuarios` passa (baseline + 1 teste)
**Commit**: `c6b0cff`

**Tests**: Vitest unit  
**Gate**: FE  
**Commit**: `fix1: assert API_KEY permission in Usuarios vitest`

---

### T7: Full gate + handoff + traceability

**What**: Rodar Full gate; atualizar `auth-api-keys-fix1` status; append handoff em `STATE.md`; marcar parent traceability Verified onde FIX1 fechou.  
**Where**:
- `_docs/specs/STATE.md` (Handoff)
- `_docs/specs/features/auth-api-keys-fix1/spec.md` (Goals/Success Criteria)
- `_docs/specs/features/auth-api-keys/spec.md` (traceability FIX1 refs)

**Depends on**: T1–T6  
**Reuses**: Gate commands acima  
**Requirement**: Success criteria fix1

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [x] Full gate passa (backend slice + FE build)
- [x] Goals/Success Criteria fix1 marcados `[x]` onde aplicável
- [x] Handoff STATE atualizado para fix1 complete pending Verifier
**Commit**: `ebc8cf1`

**Tests**: none  
**Gate**: Full  
**Commit**: `fix1: mark gate and handoff for auth-api-keys-fix1`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 ──→ T2
Phase 2:  T3 ──→ T4
Phase 3:  T5 ──→ T6
Phase 4:  T7

Batch 1: T1–T7 (7 tasks, 1 worker)
```

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1 | 1 method cluster + tests | ✅ |
| T2 | 1 filter + test extensions | ✅ |
| T3 | 1 WebMvc class (5 casos) | ✅ |
| T4 | 1 WebMvc class (ACL smoke) | ✅ |
| T5 | 1 FE test file | ✅ |
| T6 | 1 assert em test existente | ✅ |
| T7 | gate/docs | ✅ |

---

## Diagram-Definition Cross-Check

| Task | Depends On | Diagram | Status |
| ---- | ---------- | ------- | ------ |
| T1 | None | start | ✅ |
| T2 | None | parallel T1 (phase order T1→T2) | ✅ |
| T3 | None | Phase 2 start | ✅ |
| T4 | None | T3→T4 | ✅ |
| T5 | None | Phase 3 start | ✅ |
| T6 | None | T5→T6 | ✅ |
| T7 | T1–T6 | →T7 | ✅ |

---

## Test Co-location Validation

| Task | Layer | Matrix | Task Says | Status |
| ---- | ----- | ------ | --------- | ------ |
| T1 | ApiKeyService | unit | unit | ✅ |
| T2 | WriteGuardFilter | unit | unit | ✅ |
| T3 | ApiKeyController | WebMvc | WebMvc | ✅ |
| T4 | ACL HTTP | WebMvc | WebMvc | ✅ |
| T5 | ApiKeyRoute | Vitest | Vitest | ✅ |
| T6 | Usuarios | Vitest | Vitest | ✅ |
| T7 | docs | none | none | ✅ |

---

## Requirement Traceability (tasks)

| ID | Tasks |
| -- | ----- |
| FIX1-01…05 | T3 |
| FIX1-06/06b/06c | T4 |
| FIX1-07/07b/07c, FIX1-12 | T2 |
| FIX1-08/08b/08c | T1 |
| FIX1-09/10 | T5 |
| FIX1-11 | T6 |
| Success criteria | T7 |

**Coverage:** 16 requirements → 7 tasks; 0 unmapped.

---

## Tools (before Execute)

**Proposta default:** MCP NONE; skills `spring-security` (T2), `testing-a11y` (T5/T6); commits `fix1:` conforme acima.

| Task | MCP | Skill |
| ---- | --- | ----- |
| T1 | NONE | NONE |
| T2 | NONE | spring-security |
| T3–T4 | NONE | NONE |
| T5–T6 | NONE | testing-a11y |
| T7 | NONE | NONE |
