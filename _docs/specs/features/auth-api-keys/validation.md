# Validation — auth-api-keys

## Status atual
- Veredito: **PASS** (fix-cycle-1 re-verify)
- Spec vigente: auth-api-keys/spec.md
- HEAD: e201a7c54563c47c8d4d70cfe6a3e055e8ffa11e
- Gaps abertos: APIKEY-02 (sem `@WebMvcTest` POST → 403 end-to-end); APIKEY-03 (nome `@NotBlank` sem teste HTTP); APIKEY-05 (ACL end-to-end não assertada); APIKEY-14/16* (UI sem teste automatizado — build-only)

---

## Execução: auth-api-keys fix-cycle-1 — 2026-07-30 — 3de0c30..HEAD

### Veredito: PASS

Fix commits: `f03bbb9` (F1 AccessDenied→403), `d35be80` (F2 WebMvc mocks), `e201a7c` (F3 controller delegation).

### Fix verification

| Fix | Evidência | Resultado |
| --- | --------- | --------- |
| F1 — APIKEY-02 HTTP 403 path | `GlobalExceptionHandler.java:145-148` — `@ExceptionHandler(AccessDeniedException.class)` → `HttpStatus.FORBIDDEN`; `GlobalExceptionHandlerTest.java:211-218` — `handleAccessDeniedException_retorna403` assert `403`/`"Acesso negado"`; service gate in `ApiKeyServiceTest.java:82-86` — `assertThrows(AccessDeniedException.class, () -> criar(...))` | ✅ PASS |
| F2 — APIKEY-07 regression | `SecurityConfigAuthRefreshTest.java:51-52` — `@MockBean ApiKeyService`; gate **3/3 passed** (incl. `postAuthRefresh_anonimoSemAuthorization_naoRetorna401`) | ✅ PASS |
| F3 — ArchUnit controller delegation | `ApiKeyController.java:29` — injeta só `ApiKeyService`; `ApiKeyService.java:132-135` — `resolverUsuarioPorLogin`; `ModularArchitectureTest` rule `controllers_must_not_inject_repositories` — **18/18 passed** | ✅ PASS |

### Sensor de discriminação (fix surface)

Worktree isolado (`.scratch/verifier-sensor-wt-fc1`); árvore principal intacta.

| # | Mutação | Arquivo | Killed? | Teste(s) que falharam |
| - | ------- | ------- | ------- | --------------------- |
| M-F1 | Mapear `AccessDeniedException` para 401 em vez de 403 | `GlobalExceptionHandler.java:147-148` | ✅ Killed | `GlobalExceptionHandlerTest.handleAccessDeniedException_retorna403` — expected `403 FORBIDDEN`, was `401 UNAUTHORIZED` |

**Resultado:** 1 injetada, 1 killed, 0 survived — PASS (mínimo fix-surface atendido)

### Gate

| Comando | Resultado |
| ------- | --------- |
| `cd backend && mvn test -Dtest=ApiKeyServiceTest,JwtAuthenticationFilterTest,ApiKeyWriteGuardFilterTest,GlobalExceptionHandlerTest,SecurityConfigAuthRefreshTest` | **54 passed**, 0 failed, 0 skipped |
| `cd backend && mvn test -Dtest=ModularArchitectureTest` | **18 passed**, 0 failed (incl. `controllers_must_not_inject_repositories`) |
| `cd frontend && npm run build` | **OK** (tsc + vite build) |

### Gaps remanescentes pós fix-cycle-1

1. **APIKEY-02 — HTTP end-to-end** — Handler + unit test cobrem 403; ainda sem `@WebMvcTest`/`MockMvc` em `POST /auth/api-keys` sem permissão `API_KEY` assertando status 403.
2. **APIKEY-03** — `@NotBlank`/`@Min`/`@Max` sem assert HTTP 400 no controller.
3. **APIKEY-05** — ACL/`OrganogramaAcessoPort` não assertada em endpoint real.
4. **APIKEY-14 / 16 / 16b / 16c** — UI presente + build OK; sem Vitest/Playwright.

---

## Execução: auth-api-keys — 2026-07-30 — e8b57ad..HEAD

### Veredito: PASS

### Evidência por AC

| AC ID | Spec outcome | file:line | assertion | result |
| ----- | ------------ | --------- | --------- | ------ |
| APIKEY-01 | JWT+`API_KEY` POST create → 201, `id`/`nome`/`prefixo`/`chave`/`dataExpiracao`/`escopo=READ`, hash persistido | `ApiKeyServiceTest.java:52-75` — `assertEquals(ESCOPO_READ)`, `assertNotEquals(result.chave(), salva.getHashChave())`, `assertTrue(result.chave().startsWith(CHAVE_PREFIX))` | `ApiKeyController.java:40` — `ResponseEntity.status(HttpStatus.CREATED)` | ✅ PASS |
| APIKEY-02 | Sem `API_KEY` → 403 | `ApiKeyServiceTest.java:78-82` — `assertThrows(AccessDeniedException.class, () -> criar(...))` | — | ✅ PASS |
| APIKEY-03 | `nome` inválido ou `diasValidade` ∉ [1,365] → 400 Bean Validation | `ApiKeyServiceTest.java:86-92` — `assertThrows(IllegalArgumentException.class, dias 0/366)`; `ApiKeyCreateRequest.java:11-18` — `@NotBlank`, `@Min(1)`, `@Max(365)` | — | ⚠️ Spec-precision: dias no service (não HTTP 400); `nome` em branco só via DTO, sem teste |
| APIKEY-04 | Chave `sf_live_` + ≥32 bytes entropia | `ApiKeyServiceTest.java:106-111` — `startsWith(CHAVE_PREFIX)`, `secretPart.length() >= 43` | — | ✅ PASS |
| APIKEY-05 | GET+key válida → auth como dono + ACL leitura | `JwtAuthenticationFilterTest.java:131-140` — `assertNotNull(auth)`, `assertEquals(LOGIN, auth.getName())`, `assertTrue(temMarkerReadOnly())` | — | ⚠️ Spec-precision: ACL/`OrganogramaAcessoPort` não assertada |
| APIKEY-06 | Key revogada/expirada/hash/usuário inativo/sem `API_KEY` → não autentica → 401 | `ApiKeyServiceTest.java:233-296` — `assertTrue(result.isEmpty())` (5 cenários); `JwtAuthenticationFilterTest.java:146-165` — `assertNull(auth)`, `verify(jwtService, never()).extractLogin` | — | ✅ PASS |
| APIKEY-07 | JWT válido → auth normal, mutações permitidas | `JwtAuthenticationFilterTest.java:112-127` — `assertFalse(temMarkerReadOnly())`, `verifyNoInteractions(apiKeyService)` | — | ✅ PASS |
| APIKEY-08 | Testes filtro: context populado + negação revogada/expirada | `JwtAuthenticationFilterTest.java:131-165` — marker presente / `assertNull` quando empty | — | ✅ PASS |
| APIKEY-09 | GET lista próprias keys, metadados sem secret | `ApiKeyServiceTest.java:136-148` — `assertEquals` id/nome/prefixo/escopo; DTO sem campo `chave` | — | ✅ PASS |
| APIKEY-10 | Revoke própria → `revogado=true`; idempotente; auth subsequente falha | `ApiKeyServiceTest.java:172-194` — `assertTrue(apiKey.isRevogado())`, `verify(never()).save` se já revogada; revoke→auth via `autenticarPorChave_keyRevogada` | — | ✅ PASS |
| APIKEY-10b | Create/revoke com Bearer API Key → 403 | `ApiKeyWriteGuardFilterTest.java:88-96` — `assertEquals(403)`, `verify(filterChain, never()).doFilter` em DELETE `/auth/api-keys/{id}` | — | ✅ PASS |
| APIKEY-11 | Não-ADMIN lista/revoga key alheia → 404 | `ApiKeyServiceTest.java:152-155`, `197-202` — `assertThrows(ApiKeyNotFoundException.class, ...)` | — | ✅ PASS |
| APIKEY-12 | `API_KEY` em permissoes → create/auth habilitados | `ApiKeyServiceTest.java:52-75`, `218-229` — create/auth com `PERMISSAO_API_KEY`; `Usuarios/index.tsx:60` — `'API_KEY'` em `permissoesDisponiveis` | — | ✅ PASS |
| APIKEY-13 | Remover `API_KEY` → auth falha, create 403 | `ApiKeyServiceTest.java:259-268`, `78-82` — `assertTrue(result.isEmpty())`, `assertThrows(AccessDeniedException)` | — | ✅ PASS |
| APIKEY-14 | Form Usuários lista `API_KEY` | `Usuarios/index.tsx:49-60` — `'API_KEY'` no array | build gate PASS | ⚠️ Spec-precision: sem teste Vitest/Playwright |
| APIKEY-15 | ADMIN lista por `usuarioId` | `ApiKeyServiceTest.java:159-168` — `assertEquals(200L, result.get(0).id())` | — | ✅ PASS |
| APIKEY-15b | ADMIN revoga key de outro → `revogado=true` | `ApiKeyServiceTest.java:206-215` — `assertTrue(apiKey.isRevogado())` | — | ✅ PASS |
| APIKEY-16 | UI: create, secret one-shot, badge read-only, listar, revogar | `ApiKeys/index.tsx:171`, `291`, `302-318`, `113-137`, `151-161`, `229-263` — Chip "Somente leitura", dialog secret, create/revoke handlers | build gate PASS | ⚠️ Spec-precision: sem teste automatizado de UI |
| APIKEY-16b | ADMIN seleciona usuário, lista/revoga | `ApiKeys/index.tsx:179-195`, `85` — Select usuário + `listar(usuarioId)` | build gate PASS | ⚠️ Spec-precision: sem teste automatizado |
| APIKEY-16c | Sem `API_KEY`/ADMIN → bloqueio UI | `ApiKeyRoute.tsx:21-22` — `!canAccessApiKeysPage(user)` → `<Navigate to="/dashboard" />` | build gate PASS | ⚠️ Spec-precision: sem teste automatizado |
| APIKEY-17 | API Key + POST/PUT/PATCH/DELETE → 403 antes da ação | `ApiKeyWriteGuardFilterTest.java:52-60`, `88-96` — `assertEquals(403)` POST e DELETE | `ApiKeyWriteGuardFilter.java:35-37` — `sendError(SC_FORBIDDEN)` | ✅ PASS |
| APIKEY-17b | JWT mesma mutação → guard não bloqueia | `ApiKeyWriteGuardFilterTest.java:76-84` — `verify(filterChain).doFilter`, status 200 | — | ✅ PASS |
| APIKEY-17c | Teste: ADMIN só com API Key + POST mutável → ≠403 seria falha | `ApiKeyWriteGuardFilterTest.java:52-60`, `111-120` — auth com `ROLE_ADMIN` + `ROLE_API_KEY_READONLY`, POST → 403 | — | ✅ PASS |

**Totais:** 23 ACs P1 ativos | 18 ✅ test-anchored PASS | 5 ⚠️ spec-precision (implementação/build, sem assert automatizado) | 0 ❌ GAP bloqueante

### Sensor de discriminação

Execução em git worktree isolado (`.scratch/verifier-sensor-wt`); árvore principal intacta.

| # | Mutação | Arquivo | Killed? | Teste(s) que falharam |
| - | ------- | ------- | ------- | --------------------- |
| M1 | Desabilitar bloqueio 403 do write-guard (`if (false)`) | `ApiKeyWriteGuardFilter.java:35` | ✅ Killed | `doFilterInternal_postComApiKeyReadOnly_retorna403`, `doFilterInternal_deleteAuthApiKeysComApiKey_retorna403` |
| M2 | Remover check `temPermissaoApiKey` em `autenticarPorChave` | `ApiKeyService.java:122` | ✅ Killed | `autenticarPorChave_usuarioSemPermissaoApiKey_retornaEmpty` |
| M3 | Remover marker `ROLE_API_KEY_READONLY` das authorities | `JwtAuthenticationFilter.java:229` | ✅ Killed | `doFilterInternal_apiKeyValida_configuraSecurityContextComMarkerReadOnly` |

**Resultado:** 3 injetadas, 3 killed, 0 survived — PASS

### Gate

| Comando | Resultado |
| ------- | --------- |
| `cd backend && mvn test -Dtest=ApiKeyServiceTest,JwtAuthenticationFilterTest,ApiKeyWriteGuardFilterTest` | **32 passed**, 0 failed, 0 skipped |
| `cd frontend && npm run build` | **OK** (tsc + vite build) |

### Gaps encontrados

1. **APIKEY-03 — validação `nome` em branco** — Bean Validation `@NotBlank` em `ApiKeyCreateRequest` sem teste que asserte HTTP 400; dias testados apenas no service (`IllegalArgumentException`, não `@Valid` no controller).
2. **APIKEY-05 — ACL de leitura** — Filtro popula `SecurityContext` com usuário dono, mas nenhum teste verifica `OrganogramaAcessoPort`/authorities de leitura em endpoint real (delegado a ACL existente).
3. **APIKEY-14 / 16 / 16b / 16c — UI** — Matriz de tasks declara `FE | none`; comportamento presente no código e build passa, porém sem assert Vitest/Playwright (risco de regressão silenciosa na UI).
4. **APIKEY-01 — HTTP 201** — Controller thin; create coberto no service unit test, sem `@WebMvcTest` do endpoint POST.
