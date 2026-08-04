# auth-api-keys-fix2 Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/auth-api-keys-fix2/design.md`  
**Spec**: `_docs/specs/features/auth-api-keys-fix2/spec.md`  
**Context**: `_docs/specs/features/auth-api-keys-fix2/context.md`  
**Parent**: `_docs/specs/features/auth-api-keys/` (MVP + fix1 Verified)  
**Status**: Execute complete 2026-07-30 — Verifier pending  
**Branch sugerida**: `feat/auth-api-keys-fix2`  
**Commits**: prefixo `fix2:` (ex.: `fix2: ACL read path em FuncionarioService`)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` (AD-004), `auth-api-keys-fix2/spec.md` (FIX2-01…15), skills `spring-security`, `jpa-performance`, `testing-a11y`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| `FuncionarioService.*ParaUsuario` | unit | FIX2-01…03: scoped list/get, acessoTotal, CC query fora escopo → vazio; out-of-scope id → 404 | `**/cadastros/application/FuncionarioServiceTest.java` | `cd backend && mvn test -Dtest=FuncionarioServiceTest` |
| `FuncionarioController` GET + ACL parity | WebMvc | FIX2-04: JWT vs Bearer `sf_live_*` mesmo status/cardinalidade | `**/cadastros/api/FuncionarioAclWebMvcTest.java` | `cd backend && mvn test -Dtest=FuncionarioAclWebMvcTest` |
| `UsuarioService.*ParaUsuario` | unit | FIX2-05…07: scoped exclui sem funcionário; get 404; acessoTotal global | `**/auth/application/UsuarioServiceTest.java` | `cd backend && mvn test -Dtest=UsuarioServiceTest` |
| `UsuarioController` GET + ACL parity | WebMvc | FIX2-08: paridade JWT vs API Key | `**/auth/api/UsuarioAclWebMvcTest.java` | `cd backend && mvn test -Dtest=UsuarioAclWebMvcTest` |
| `GlobalExceptionHandler` + `BeneficioMensalController` GET | WebMvc + unit handler | FIX2-09/10: GET `/beneficio-mensal` sem params → **400** (JWT e Bearer) | `**/exception/GlobalExceptionHandlerTest.java`, `**/beneficios/api/BeneficioMensalControllerWebMvcTest.java` | `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest,BeneficioMensalControllerWebMvcTest` |
| `api.ts` interceptor | Vitest + MSW | FIX2-11…13: 401 refresh; 403 sem logout/sem clear tokens | `frontend/src/services/api.test.ts` | `cd frontend && npm test -- api.test` |
| `ApiKeys` + `permissions.ts` | Vitest | FIX2-14/15: ADMIN-only create disabled + Alert; API_KEY habilitado | `frontend/src/pages/ApiKeys/ApiKeys.test.tsx` | `cd frontend && npm test -- ApiKeys` |
| ArchUnit regressão | unit | AD-008: cadastros/auth não importam infra organograma | `**/arch/ModularArchitectureTest.java` | incluído no Full gate |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick — Funcionário | T1 | `cd backend && mvn test -Dtest=FuncionarioServiceTest` |
| WebMvc — Funcionário | T2 | `cd backend && mvn test -Dtest=FuncionarioAclWebMvcTest` |
| Quick — Usuário | T3 | `cd backend && mvn test -Dtest=UsuarioServiceTest` |
| WebMvc — Usuário | T4 | `cd backend && mvn test -Dtest=UsuarioAclWebMvcTest` |
| HTTP 400 benefício | T5 | `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest,BeneficioMensalControllerWebMvcTest` |
| FE interceptor | T6 | `cd frontend && npm test -- api.test` |
| FE ApiKeys UX | T7 | `cd frontend && npm test -- ApiKeys` |
| **Full** | T8 fechamento | `cd backend && mvn test -Dtest=FuncionarioServiceTest,FuncionarioAclWebMvcTest,UsuarioServiceTest,UsuarioAclWebMvcTest,GlobalExceptionHandlerTest,BeneficioMensalControllerWebMvcTest,ModularArchitectureTest` + `cd frontend && npm test -- api.test ApiKeys` + `cd frontend && npm run build` |

---

## Execution Plan

### Phase 1: ACL funcionários (BE)

```
T1 → T2
```

### Phase 2: ACL usuários (BE)

```
T3 → T4
```

### Phase 3: Benefício 400 (BE)

```
T5
```

### Phase 4: Interceptor axios (FE)

```
T6
```

### Phase 5: UX ApiKeys (FE)

```
T7
```

### Phase 6: Gate + handoff

```
T8
```

**Batch packing (~7 tasks/worker, whole phases):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | Phase 1–6 | T1–T8 | 8 |

→ **1 batch / execução inline** (≤ ~8 tasks). Sub-agents opcionais; offer só se escopo crescer.

---

## Task Breakdown

### T1: `FuncionarioService` ACL read path + unit tests ✅ `6f949b1`

**What**: Implementar `listarParaUsuario` e `buscarPorIdParaUsuario` com `OrganogramaAcessoPort` + `UsuarioLookupPort`; testes FIX2-01…03.  
**Where**:
- `backend/src/main/java/.../cadastros/application/FuncionarioService.java`
- `backend/src/test/java/.../cadastros/application/FuncionarioServiceTest.java`

**Depends on**: None  
**Reuses**: `BeneficioMensalService` (ACL helpers), `CentroCustoEfetivo`, `FuncionarioRepository.findByFiltros`  
**Requirement**: FIX2-01, FIX2-02, FIX2-03

**Tools**: MCP NONE; Skills `jpa-performance`, `spring-security`

**Done when**:
- [x] Scoped: lista só CC ∈ `centrosCustoIds`; sem CC → excluído; CC query fora escopo → `[]`
- [x] Scoped get out-of-scope → `FuncionarioNotFoundException`
- [x] `acessoTotal=true` → delega `listar`/`buscarPorId` atuais
- [x] Gate Quick: `FuncionarioServiceTest` verde; ≥4 casos novos ACL

**Tests**: unit  
**Gate**: Quick  
**Commit**: `fix2: ACL read path em FuncionarioService`

---

### T2: `FuncionarioController` GET wire + WebMvc paridade API Key ✅ `f35b141`

**What**: Controllers GET passam `Authentication.getName()`; `FuncionarioAclWebMvcTest` paridade JWT vs Bearer.  
**Where**:
- `backend/src/main/java/.../cadastros/api/FuncionarioController.java`
- `backend/src/test/java/.../cadastros/api/FuncionarioAclWebMvcTest.java`

**Depends on**: T1  
**Reuses**: `ApiKeyAclWebMvcTest` (setup SecurityConfig + Bearer mock)  
**Requirement**: FIX2-04

**Tools**: MCP NONE; Skill `spring-security`

**Done when**:
- [ ] `GET /funcionarios` e `GET /funcionarios/{id}` usam `*ParaUsuario`
- [ ] WebMvc: mesmo login JWT `@WithMockUser` vs Bearer `sf_live_*` → mesmo status e `$.length()` (ou 404)
- [ ] Mutações POST/PUT/DELETE **inalteradas**
- [ ] Gate WebMvc — Funcionário: passa

**Tests**: WebMvc  
**Gate**: WebMvc — Funcionário  
**Commit**: `fix2: wire FuncionarioController GET ACL + WebMvc parity`

---

### T3: `UsuarioService` ACL read path + unit tests ✅ `13b47ca`

**What**: `listarParaUsuario`, `buscarPorIdParaUsuario`, `buscarPorLoginParaUsuario`, `buscarPorFuncionarioParaUsuario`; excluir usuários sem funcionário no scoped (FIX2-CTX-01).  
**Where**:
- `backend/src/main/java/.../auth/application/UsuarioService.java`
- `backend/src/test/java/.../auth/application/UsuarioServiceTest.java`

**Depends on**: None (paralelo lógico a T1; sequência Execute após T2 por fase)  
**Reuses**: `OrganogramaAcessoPort`, `UsuarioRepository.findByFiltros`  
**Requirement**: FIX2-05, FIX2-06, FIX2-07

**Tools**: MCP NONE; Skills `jpa-performance`, `spring-security`

**Done when**:
- [ ] Scoped list: só usuários com funcionário CC no escopo; sem funcionário → excluído
- [ ] Scoped get/login/funcionario out-of-scope → `UsuarioNotFoundException`
- [ ] `acessoTotal=true` → comportamento global atual
- [ ] Gate Quick — Usuário: passa; ≥4 casos ACL novos

**Tests**: unit  
**Gate**: Quick — Usuário  
**Commit**: `fix2: ACL read path em UsuarioService`

---

### T4: `UsuarioController` GET wire + WebMvc paridade API Key ✅ `d51d2d0`

**What**: Todos `@GetMapping` em `UsuarioController` usam `*ParaUsuario`; WebMvc FIX2-08.  
**Where**:
- `backend/src/main/java/.../auth/api/UsuarioController.java`
- `backend/src/test/java/.../auth/api/UsuarioAclWebMvcTest.java`

**Depends on**: T3  
**Reuses**: Template `FuncionarioAclWebMvcTest` / `ApiKeyAclWebMvcTest`  
**Requirement**: FIX2-08

**Tools**: MCP NONE; Skill `spring-security`

**Done when**:
- [ ] GET list, `/{id}`, `/login/{login}`, `/funcionario/{funcionarioId}` com ACL
- [ ] Paridade JWT vs Bearer API Key (status + cardinalidade)
- [ ] Gate WebMvc — Usuário: passa

**Tests**: WebMvc  
**Gate**: WebMvc — Usuário  
**Commit**: `fix2: wire UsuarioController GET ACL + WebMvc parity`

---

### T5: `GlobalExceptionHandler` missing param + Beneficio WebMvc 400 ✅ `2d5acb8`

**What**: Handler `MissingServletRequestParameterException` → 400; teste GET `/beneficio-mensal` sem params.  
**Where**:
- `backend/src/main/java/.../exception/GlobalExceptionHandler.java`
- `backend/src/test/java/.../exception/GlobalExceptionHandlerTest.java`
- `backend/src/test/java/.../beneficios/api/BeneficioMensalControllerWebMvcTest.java` (novo)

**Depends on**: None (Phase 3 independente de FE)  
**Reuses**: `GlobalExceptionHandlerTest`, `ApiKeyControllerWebMvcTest` setup  
**Requirement**: FIX2-09, FIX2-10

**Tools**: MCP NONE; Skill NONE

**Done when**:
- [ ] `GET /beneficio-mensal` sem `competenciaInicio`/`competenciaFim` → **400** + body padronizado
- [ ] Mesmo cenário com Bearer API Key → **400**
- [ ] Handler unit test cobre missing param
- [ ] Gate HTTP 400 benefício: passa

**Tests**: unit + WebMvc  
**Gate**: HTTP 400 benefício  
**Commit**: `fix2: 400 for missing beneficio-mensal query params`

---

### T6: Interceptor axios — 403 não desloga + Vitest ✅ `bbb93d1`

**What**: `shouldRefreshToken` / `isUnauthorizedStatus` só **401**; testes FIX2-11…13.  
**Where**:
- `frontend/src/services/api.ts`
- `frontend/src/services/api.test.ts`

**Depends on**: None  
**Reuses**: MSW `createAuthMswServer`, padrão 401 retry existente  
**Requirement**: FIX2-11, FIX2-12, FIX2-13

**Tools**: MCP NONE; Skill `testing-a11y`

**Done when**:
- [ ] 403 em request autenticado: tokens permanecem; `auth:logout` **not** dispatched
- [ ] 401: refresh path preservado (regressão teste existente)
- [ ] Gate FE interceptor: passa; ≥2 casos novos (403 + regressão 401)

**Tests**: Vitest + MSW  
**Gate**: FE interceptor  
**Commit**: `fix2: axios interceptor refresh only on 401 not 403`

---

### T7: `canCreateApiKey` + ApiKeys UI disabled + Vitest ✅ `420454c`

**What**: `permissions.canCreateApiKey`; botão create disabled + Alert para ADMIN sem `API_KEY` (FIX2-CTX-02).  
**Where**:
- `frontend/src/utils/permissions.ts`
- `frontend/src/pages/ApiKeys/index.tsx`
- `frontend/src/pages/ApiKeys/ApiKeys.test.tsx` (novo)

**Depends on**: T6 (interceptor fix evita logout se alguém contornar UI — ordem recomendada)  
**Reuses**: `ApiKeyRoute.test.tsx` mock patterns  
**Requirement**: FIX2-14, FIX2-15

**Tools**: MCP NONE; Skills `component-architecture`, `testing-a11y`

**Done when**:
- [ ] ADMIN sem `API_KEY`: botão disabled + Alert warning visível
- [ ] User com `API_KEY`: botão enabled (sem regressão)
- [ ] Revogar admin continua habilitado
- [ ] Gate FE ApiKeys UX: passa

**Tests**: Vitest  
**Gate**: FE ApiKeys UX  
**Commit**: `fix2: disable API key create for ADMIN without API_KEY permission`

---

### T8: Full gate + handoff docs ✅ `f3254d3`

**What**: Rodar Full gate; atualizar traceability spec parent; append handoff `STATE.md`; preparar slot `validation.md`.  
**Where**:
- `_docs/specs/features/auth-api-keys-fix2/spec.md` (status traceability)
- `_docs/specs/features/auth-api-keys/spec.md` (APIKEY-05 → Verified fix2)
- `_docs/specs/STATE.md` (Handoff)

**Depends on**: T1–T7  
**Reuses**: Gate commands acima  
**Requirement**: Success Criteria (spec fix2)

**Tools**: MCP NONE; Skill `tlc-spec-driven` (Verifier dispatch)

**Done when**:
- [ ] Full gate verde (BE slice + FE + build)
- [ ] `ModularArchitectureTest` passa (sem import infra organograma em cadastros)
- [ ] Spec fix2 traceability → Implementing/Done; parent APIKEY-05 anotado
- [ ] Handoff STATE.md atualizado
- [ ] Verifier sub-agent dispatched (automático pós-T8)

**Tests**: Full gate (no new tests)  
**Gate**: Full  
**Commit**: `fix2: gate green and handoff docs for auth-api-keys-fix2`

---

## Phase Execution Map

```
Phase 1:  T1 ──→ T2
Phase 2:  T3 ──→ T4
Phase 3:  T5
Phase 4:  T6
Phase 5:  T7
Phase 6:  T8
```

Execução estritamente sequencial — 1 task por vez, 1 commit por task.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: FuncionarioService ACL + unit | 1 service + tests | ✅ Granular |
| T2: FuncionarioController + WebMvc | 1 controller + 1 test class | ✅ Granular |
| T3: UsuarioService ACL + unit | 1 service + tests | ✅ Granular |
| T4: UsuarioController + WebMvc | 1 controller + 1 test class | ✅ Granular |
| T5: Exception handler + Beneficio WebMvc | 1 handler + 1 WebMvc class | ✅ Granular |
| T6: api.ts interceptor + tests | 1 service file + tests | ✅ Granular |
| T7: ApiKeys UX + permissions | 2 FE files + 1 test | ✅ Granular |
| T8: Gate + docs | ops/docs only | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | Phase 1 start | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | None* | Phase 2 start | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | None | Phase 3 isolated | ✅ Match |
| T6 | None | Phase 4 isolated | ✅ Match |
| T7 | T6 | Phase 5 after T6 | ✅ Match |
| T8 | T1–T7 | Phase 6 after all | ✅ Match |

\*T3 sem dependência hard de T2; ordem de fase impõe T3 após T2 na Execute.

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | FuncionarioService | unit | unit | ✅ OK |
| T2 | FuncionarioController | WebMvc | WebMvc | ✅ OK |
| T3 | UsuarioService | unit | unit | ✅ OK |
| T4 | UsuarioController | WebMvc | WebMvc | ✅ OK |
| T5 | Handler + BeneficioController | unit + WebMvc | unit + WebMvc | ✅ OK |
| T6 | api.ts | Vitest | Vitest + MSW | ✅ OK |
| T7 | ApiKeys + permissions | Vitest | Vitest | ✅ OK |
| T8 | docs only | none (gate) | Full gate | ✅ OK |

---

## Tools & Skills (Execute)

| Task | Recommended Skills |
| ---- | ------------------ |
| T1–T4 | `spring-security`, `jpa-performance`, `spring-boot-new-endpoint` (read-only wire) |
| T5 | `spring-boot-new-endpoint` (exception handling pattern) |
| T6–T7 | `testing-a11y`, `component-architecture`, `api-client` |
| T8 | `tlc-spec-driven` (Verifier) |

**MCP sugerido:** nenhum obrigatório; SonarQube opcional pós-gate.

---

## Requirement Traceability (tasks → AC)

| Task | FIX2 IDs |
| ---- | -------- |
| T1 | FIX2-01, FIX2-02, FIX2-03 |
| T2 | FIX2-04 |
| T3 | FIX2-05, FIX2-06, FIX2-07 |
| T4 | FIX2-08 |
| T5 | FIX2-09, FIX2-10 |
| T6 | FIX2-11, FIX2-12, FIX2-13 |
| T7 | FIX2-14, FIX2-15 |
| T8 | Success Criteria + Verifier |

**Coverage:** 15/15 ACs mapeados; 8 tasks; 0 unmapped.
