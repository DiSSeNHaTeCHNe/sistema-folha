# Auth — API Keys Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/auth-api-keys/design.md`  
**Status**: Executed — T1–T16 complete on `feat/auth-api-keys` (2026-07-30)  
**Approach**: A — Bearer dual-path + write-guard READ-ONLY (AD-014)  
**User preference (project):** sem commits automáticos salvo pedido explícito.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` + AD-004, skill `spring-security`, skill `flyway-migration-writer`, `auth-api-keys/spec.md` (APIKEY ACs incl. 17*), AD-008/AD-014.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Application (`ApiKeyService`) | unit | APIKEY-01..04, 06, 09..11, 13, 15/15b; edges expiry/idempotência/default 365 | `**/auth/application/ApiKeyServiceTest.java` | `cd backend && mvn test -Dtest=ApiKeyServiceTest` |
| Security Bearer filter | unit | APIKEY-05/07/08 + marker READONLY presente quando API Key | `**/security/JwtAuthenticationFilterTest.java` | `cd backend && mvn test -Dtest=JwtAuthenticationFilterTest` |
| Security write-guard | unit | APIKEY-17/17b/17c: POST+API Key→403; GET+API Key passa; POST+JWT não bloqueado pelo guard | `**/security/ApiKeyWriteGuardFilterTest.java` | `cd backend && mvn test -Dtest=ApiKeyWriteGuardFilterTest` |
| API Controller | none | Thin; ACs no service | `ApiKeyController.java` | build |
| Entity / Flyway / DTOs | none | Inclui coluna/campo `escopo=READ` | `V1.27__*.sql` | build |
| FE | none | Badge somente leitura; `npm run build` | `pages/ApiKeys/**` | `cd frontend && npm run build` |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Service/filter/guard | `cd backend && mvn test -Dtest=ApiKeyServiceTest,JwtAuthenticationFilterTest,ApiKeyWriteGuardFilterTest` |
| Full | FE + fechamento | Quick + `cd frontend && npm run build` |
| Build | Migration/entity/DTO | `cd backend && mvn -q -DskipTests compile` |

---

## Execution Plan

### Phase 1: Persistence foundation

```
T1 → T2 → T3
```

### Phase 2: Application service

```
T4 → T5 → T6 → T7
```

### Phase 3: HTTP + Bearer + write-guard

```
T8 → T9 → T10
```

### Phase 4: Frontend

```
T11 → T12 → T13 → T14 → T15
```

### Phase 5: Full gate + handoff

```
T16
```

**Batch packing:** 16 tasks → ~3 batches  
- Batch 1: T1–T7 (7)  
- Batch 2: T8–T14 (7)  
- Batch 3: T15–T16 (2)  

→ Offer sub-agents at Execute.

---

## Task Breakdown

### T1: Flyway `api_keys` (+ escopo)

**What**: Migration `V1.27__create_api_keys.sql` com colunas do design incluindo `escopo VARCHAR(16) NOT NULL DEFAULT 'READ'`.  
**Where**: `backend/src/main/resources/db/migration/V1.27__create_api_keys.sql`  
**Depends on**: None  
**Reuses**: `V1.5__create_refresh_tokens.sql`  
**Requirement**: APIKEY-01

**Tools**: MCP NONE; Skill `flyway-migration-writer`

**Done when**:
- [ ] Tabela completa + `escopo` default `READ` + índices
- [ ] Gate build: `cd backend && mvn -q -DskipTests compile`

**Tests**: none | **Gate**: build  
**Commit**: `feat(auth): add api_keys flyway migration`

---

### T2: Entity `ApiKey` + `ApiKeyNotFoundException`

**What**: Entity com `escopo`, helpers `isValida`/`isExpirada`/`isRevogado` e exceção 404.  
**Where**: `auth/domain/ApiKey.java`, `ApiKeyNotFoundException.java`  
**Depends on**: T1  
**Reuses**: `RefreshToken`  
**Requirement**: APIKEY-01, APIKEY-11

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] Entity mapeia `escopo`; lifecycle helpers
- [ ] Gate build compile

**Tests**: none | **Gate**: build  
**Commit**: `feat(auth): add ApiKey entity and not-found exception`

---

### T3: `ApiKeyRepository`

**What**: Finders `findByPrefixoAndRevogadoFalse`, `findByUsuarioIdOrderByDataCriacaoDesc`.  
**Where**: `auth/infrastructure/ApiKeyRepository.java`  
**Depends on**: T2  
**Requirement**: APIKEY-01, APIKEY-05, APIKEY-09

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] Métodos do design presentes
- [ ] Gate build compile

**Tests**: none | **Gate**: build  
**Commit**: `feat(auth): add ApiKeyRepository`

---

### T4: DTOs de API Key

**What**: Request/response com `escopo` na resposta created/list; request **sem** campo escopo editável.  
**Where**: `auth/api/*ApiKey*DTO*.java`  
**Depends on**: T2  
**Requirement**: APIKEY-01, APIKEY-03, APIKEY-09

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] Validação nome/dias; created/list expõem `escopo`
- [ ] Gate build compile

**Tests**: none | **Gate**: build  
**Commit**: `feat(auth): add API key DTOs`

---

### T5: `ApiKeyService.criar` + unit tests

**What**: Create com gate `API_KEY`, default 365, `escopo=READ` fixo, BCrypt, one-shot `chave`.  
**Where**: `ApiKeyService.java` + `ApiKeyServiceTest.java`  
**Depends on**: T3, T4  
**Requirement**: APIKEY-01, APIKEY-02, APIKEY-03, APIKEY-04

**Tools**: MCP NONE; Skill `spring-security`

**Done when**:
- [ ] Persistido `escopo=READ`; `chave` com `sf_live_`; hash ≠ chave
- [ ] Sem `API_KEY` → 403; dias inválidos rejeitados
- [ ] Gate quick: `mvn test -Dtest=ApiKeyServiceTest`
- [ ] ≥4 testes create

**Tests**: unit | **Gate**: quick  
**Commit**: `feat(auth): create read-only API key with hashed secret`

---

### T6: `ApiKeyService` listar + revogar + unit tests

**What**: List próprias / ADMIN por `usuarioId`; revoke própria/ADMIN; 404 alheia; idempotência.  
**Where**: `ApiKeyService` + test  
**Depends on**: T5  
**Requirement**: APIKEY-09, APIKEY-10, APIKEY-11, APIKEY-15, APIKEY-15b

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] List sem secret; ADMIN cross-user; 404 alheia; revoke idempotente
- [ ] Gate quick ApiKeyServiceTest
- [ ] ≥5 testes list/revoke

**Tests**: unit | **Gate**: quick  
**Commit**: `feat(auth): list and revoke API keys with admin override`

---

### T7: `ApiKeyService.autenticarPorChave` + unit tests

**What**: Resolve raw key → `Optional<Usuario>` com checks validade/ativo/`API_KEY`.  
**Where**: `ApiKeyService` + test  
**Depends on**: T6  
**Requirement**: APIKEY-05 (service), APIKEY-06, APIKEY-13

**Tools**: MCP NONE; Skill `spring-security`

**Done when**:
- [ ] Válida → user; inválida/revogada/expirada/sem perm → empty; sem log de secret
- [ ] Gate quick ApiKeyServiceTest
- [ ] ≥4 testes resolve

**Tests**: unit | **Gate**: quick  
**Commit**: `feat(auth): resolve API key for authentication`

---

### T8: `ApiKeyController` + handler 404

**What**: POST/GET/DELETE `/auth/api-keys` + `ApiKeyNotFoundException` → 404.  
**Where**: `ApiKeyController.java`, `GlobalExceptionHandler.java`  
**Depends on**: T7  
**Requirement**: APIKEY-01..03, APIKEY-09, APIKEY-10, APIKEY-15

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] Endpoints + handler; resolve usuario do Authentication
- [ ] Gate build compile

**Tests**: none | **Gate**: build  
**Commit**: `feat(auth): expose /auth/api-keys endpoints`

---

### T9: Bearer dual-path + marker READONLY + testes

**What**: Estender filtro: `sf_live_*` → auth user + **marker read-only**; JWT inalterado; wiring SecurityConfig.  
**Where**: `JwtAuthenticationFilter.java`, `SecurityConfig.java`, `JwtAuthenticationFilterTest.java`  
**Depends on**: T7  
**Requirement**: APIKEY-05, APIKEY-06, APIKEY-07, APIKEY-08

**Tools**: MCP NONE; Skill `spring-security`

**Done when**:
- [ ] API Key válida popula context **com** marker READONLY
- [ ] `sf_live_` inválida não chama JWT parser
- [ ] JWT regressão OK
- [ ] Gate: `mvn test -Dtest=ApiKeyServiceTest,JwtAuthenticationFilterTest`
- [ ] ≥3 novos casos filter

**Tests**: unit | **Gate**: quick  
**Commit**: `feat(security): authenticate API keys with read-only marker`

---

### T10: `ApiKeyWriteGuardFilter` + testes

**What**: Filtro que retorna 403 em POST/PUT/PATCH/DELETE quando marker API Key presente; GET passa; JWT mutável não bloqueado.  
**Where**: `ApiKeyWriteGuardFilter.java`, `ApiKeyWriteGuardFilterTest.java`, wire em `SecurityConfig`  
**Depends on**: T9  
**Requirement**: APIKEY-17, APIKEY-17b, APIKEY-17c, APIKEY-10b

**Tools**: MCP NONE; Skill `spring-security` — não afrouxar matchers

**Done when**:
- [ ] POST+API Key → 403; GET+API Key → chain segue
- [ ] POST+JWT (sem marker) → guard não bloqueia
- [ ] DELETE `/auth/api-keys/{id}` com API Key → 403
- [ ] Gate quick com `ApiKeyWriteGuardFilterTest` incluído
- [ ] ≥3 testes guard (17/17b/17c)

**Tests**: unit | **Gate**: quick  
**Commit**: `feat(security): block mutations for API key authentication`

---

### T11: Helper + `ApiKeyRoute`

**What**: `canAccessApiKeysPage` + guard de rota.  
**Where**: `permissions.ts`, `ApiKeyRoute.tsx`  
**Depends on**: T8  
**Requirement**: APIKEY-16c

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] Gate `API_KEY` \|\| ADMIN; senão redirect
- [ ] `cd frontend && npm run build`

**Tests**: none | **Gate**: full  
**Commit**: `feat(frontend): add ApiKeyRoute permission gate`

---

### T12: `apiKeyService` frontend

**What**: Cliente create/list/revoke tipado (`escopo` na resposta).  
**Where**: `services/apiKeyService.ts`  
**Depends on**: T11  
**Requirement**: APIKEY-16, APIKEY-15

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] Tipos alinhados; build FE passa

**Tests**: none | **Gate**: full  
**Commit**: `feat(frontend): add apiKeyService client`

---

### T13: Página `ApiKeys` (+ badge somente leitura)

**What**: UI create/list/copy/revoke; badge **Somente leitura**; ADMIN seletor usuário.  
**Where**: `pages/ApiKeys/index.tsx`  
**Depends on**: T12  
**Requirement**: APIKEY-16, APIKEY-16b

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] Secret one-shot; badge read-only visível; admin cross-user
- [ ] Build FE passa

**Tests**: none | **Gate**: full  
**Commit**: `feat(frontend): add API Keys management page`

---

### T14: Wire rota `/api-keys` + menu

**What**: Rota fora de `AdminRoute`; menu condicional.  
**Where**: `routes/index.tsx`, `Layout/index.tsx`  
**Depends on**: T13  
**Requirement**: APIKEY-16, APIKEY-16c

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] Rota + menu; build FE passa

**Tests**: none | **Gate**: full  
**Commit**: `feat(frontend): route and menu for API Keys`

---

### T15: Chip `API_KEY` em Usuários

**What**: Incluir `API_KEY` em `permissoesDisponiveis`.  
**Where**: `pages/Usuarios/index.tsx`  
**Depends on**: T14  
**Requirement**: APIKEY-12, APIKEY-14

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] Chip disponível; build FE passa

**Tests**: none | **Gate**: full  
**Commit**: `feat(frontend): allow granting API_KEY permission`

---

### T16: Full gate + handoff

**What**: Full gate; atualizar STATE handoff; status design/tasks.  
**Where**: `_docs/specs/STATE.md`, status em design/tasks  
**Depends on**: T10, T15  
**Requirement**: Success criteria

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] `mvn test -Dtest=ApiKeyServiceTest,JwtAuthenticationFilterTest,ApiKeyWriteGuardFilterTest` + `npm run build` passam
- [ ] Handoff atualizado

**Tests**: none | **Gate**: full  
**Commit**: `docs(auth-api-keys): mark execute gate and handoff`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5

Phase 1:  T1 ──→ T2 ──→ T3
Phase 2:  T4 ──→ T5 ──→ T6 ──→ T7
Phase 3:  T8 ──→ T9 ──→ T10
Phase 4:  T11 ──→ T12 ──→ T13 ──→ T14 ──→ T15
Phase 5:  T16

Batches: [T1–T7] → [T8–T14] → [T15–T16]
```

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1–T4 | 1 file/conceito | ✅ |
| T5–T7 | method cluster + tests | ✅ |
| T8 | controller + handler | ✅ OK |
| T9 | filter + marker + tests | ✅ |
| T10 | write-guard + tests | ✅ |
| T11–T15 | FE atomic | ✅ |
| T16 | gate/docs | ✅ |

---

## Diagram-Definition Cross-Check

| Task | Depends On | Diagram | Status |
| ---- | ---------- | ------- | ------ |
| T1 | None | start | ✅ |
| T2 | T1 | T1→T2 | ✅ |
| T3 | T2 | T2→T3 | ✅ |
| T4 | T2 | after T3 in order | ✅ |
| T5 | T3, T4 | T4→T5 | ✅ |
| T6 | T5 | T5→T6 | ✅ |
| T7 | T6 | T6→T7 | ✅ |
| T8 | T7 | T7→T8 | ✅ |
| T9 | T7 | T8→T9 (dep T7) | ✅ |
| T10 | T9 | T9→T10 | ✅ |
| T11 | T8 | T10→T11 (phase order; dep T8) | ✅ |
| T12 | T11 | T11→T12 | ✅ |
| T13 | T12 | T12→T13 | ✅ |
| T14 | T13 | T13→T14 | ✅ |
| T15 | T14 | T14→T15 | ✅ |
| T16 | T10, T15 | →T16 | ✅ |

---

## Test Co-location Validation

| Task | Layer | Matrix | Task Says | Status |
| ---- | ----- | ------ | --------- | ------ |
| T1–T4, T8 | entity/flyway/DTO/controller | none | none | ✅ |
| T5–T7 | ApiKeyService | unit | unit | ✅ |
| T9 | Bearer filter | unit | unit | ✅ |
| T10 | Write-guard | unit | unit | ✅ |
| T11–T16 | FE/docs | none | none | ✅ |

---

## Requirement Traceability (tasks)

| ID | Tasks |
| -- | ----- |
| APIKEY-01..04 | T1, T4, T5, T8 |
| APIKEY-05..08 | T7, T9 |
| APIKEY-09..11, 15* | T6, T8 |
| APIKEY-10b, 17* | T10 |
| APIKEY-12..14 | T15 |
| APIKEY-16* | T11–T14 |
| APIKEY-18/19 | Deferred |

---

## Tools (before Execute)

**Proposta default:** MCP NONE; skills `flyway-migration-writer` (T1), `spring-security` (T5/T7/T9/T10); commits só se pedido.
