# Adequação da Análise de Projeto — R3 Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/adequacao-analise-projeto-r3/design.md`  
**Spec**: `_docs/specs/features/adequacao-analise-projeto-r3/spec.md`  
**Status**: Draft — Tasks 2026-07-29  
**Branch base**: `main` @ `cb6e04a` → `feat/adequacao-analise-projeto`

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `AGENTS.md` (raiz), `backend/AGENTS.md`, `frontend/AGENTS.md`, `_docs/specs/TESTING.md`, AD-004 (FE brownfield incremental), AD-010 (ArchUnit), skills `spring-security`, `testing-a11y` (target — brownfield MSW pattern only).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| FE auth client (`api.ts` interceptors) | unit (Vitest + MSW) | ≥4 cases AAP3-06 matrix: 401→refresh→retry; refresh fail→logout; queue during refresh; refresh endpoint 401; all listed edge cases | `frontend/src/services/api.test.ts` | `cd frontend && npm test -- api.test` |
| FE MSW harness | none | Compila; consumido por `api.test.ts` | `frontend/src/test/mswServer.ts`, `frontend/src/test/handlers/authHandlers.ts` | `cd frontend && npm test` |
| FE Login page behavior | unit (Vitest + Testing Library) | ≥1 case credenciais inválidas: alert visível, sem navigate | `frontend/src/pages/Login/Login.test.tsx` | `cd frontend && npm test -- Login.test` |
| FE services (tokenService) | unit (Vitest) | Já coberto R2; estender só se floor Vitest ≥50 exigir | `frontend/src/services/tokenService.test.ts` | `cd frontend && npm test` |
| FE pages (smoke existentes) | unit (Vitest) | Manter verdes; `vi.mock` brownfield OK | `frontend/src/pages/**/*.test.tsx` | `cd frontend && npm test` |
| Backend auth refresh domain | unit (Mockito) | Refresh inválido/expirado/revogado → `RefreshTokenInvalidoException`; não 500 | `backend/src/test/java/**/auth/**/*Test.java` | `cd backend && mvn test -Dtest=AuthenticationServiceTest` |
| Backend auth refresh HTTP | unit (`@WebMvcTest`) | `POST /auth/refresh` token inválido → **401** + mensagem genérica | `backend/src/test/java/**/config/SecurityConfigAuthRefreshTest.java` | `cd backend && mvn test -Dtest=SecurityConfigAuthRefreshTest` |
| GlobalExceptionHandler (refresh) | unit | Handler mapeia `RefreshTokenInvalidoException` → 401 | `backend/src/test/java/**/exception/GlobalExceptionHandlerTest.java` | `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest` |
| Importação ADP integration | integration (Testcontainers) | ≥1 linha persistida de `folha-adp-minimal.txt`; rollback; `@EnabledIf` Docker | `backend/src/test/java/**/importacao/**/*IntegrationTest.java` | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` |
| BeneficioMensalService tx | unit (Mockito) | Regressão pós-refactor tx; sem `@SuppressWarnings(S6809)` | `backend/src/test/java/**/beneficios/application/BeneficioMensalServiceTest.java` | `cd backend && mvn test -Dtest=BeneficioMensalServiceTest` |
| ArchUnit modular boundaries | unit | Zero violação AD-010 | `backend/src/test/java/**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| S2245 FolhaPagamento keys | unit | Grep/regressão: keys estáveis (`id`), sem `Math.random()` | `frontend/src/pages/FolhaPagamento/FolhaPagamento.test.tsx` | `cd frontend && npm test -- FolhaPagamento.test` |
| Testcontainers BOM / pom deps | none | Build + suite verde | `backend/pom.xml` | `cd backend && mvn test` |
| MSW npm dependency | none | `npm ci` + test suite verde | `frontend/package.json` | `cd frontend && npm ci && npm test` |
| Gate scripts (sonar, jacoco) | none | Exit 0; lcov gerado | `diversos/scripts/*.sh` | `./diversos/scripts/sonar-analyze.sh` |
| Docs (CONCERNS, validation) | none | Review gate | `_docs/specs/CONCERNS.md`, `validation.md` | manual |
| Playwright E2E (P3 optional) | e2e | ≥1 spec login smoke | `frontend/e2e/*.spec.ts` | `cd frontend && npm run test:e2e` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick FE | FE unit/MSW task | `cd frontend && npm test` |
| FE Focused | Single FE test file | `cd frontend && npm test -- <pattern>` |
| FE Coverage | After FE tasks / before Sonar checkpoint | `cd frontend && npm run test:coverage` |
| Quick BE | Backend focused unit test | `cd backend && mvn test -Dtest=<ClassTest>` |
| ADP Integration | After T11 | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` |
| Arch | Every backend code task | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| JaCoCo | Phase 2 gates | `bash diversos/scripts/check-jacoco-thresholds.sh` |
| Full BE | Phase 2 end / before Verifier | `cd backend && mvn test` (≥464 tests, 0 failures) |
| Build FE | FE-only smoke | `cd frontend && npm run lint && npm run build` |
| Sonar | T6 checkpoint + T14 final | `./diversos/scripts/sonar-analyze.sh` |
| E2E (P3) | T15 optional | `cd frontend && npm run test:e2e` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

**Batch sizing (Execute — offer sub-agents if user accepts):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| **Batch 1** | Phase 1 | T1 → T6 | 6 |
| **Batch 2** | Phase 2 | T7 → T14 | 8 |
| **Batch 3** (optional) | Phase 3 | T15 | 1 |

### Phase 1: FE depth + Sonar checkpoint (P1)

```
T1 → T2 → T3 → T4 → T5 → T6
```

### Phase 2: BE auth + integration + smells + gates (P2)

```
T7 → T8 → T9 → T10 → T11 → T12 → T13 → T14
```

### Phase 3: E2E smoke opcional (P3)

```
T15
```

---

## Task Breakdown

### T1: Adicionar MSW devDependency

**What**: Instalar `msw@^2` como devDependency e atualizar lockfile.  
**Where**: `frontend/package.json`, `frontend/package-lock.json`  
**Depends on**: None  
**Reuses**: Pin compatível Vitest 4 / Node 24 (`.nvmrc`)  
**Requirement**: AAP3-05 (infra)

**Tools**:

- MCP: NONE
- Skill: `testing-a11y` (MSW target pattern)

**Done when**:

- [x] `msw` listado em `devDependencies`
- [x] `npm ci` exit 0 em `frontend/`
- [x] Gate: `cd frontend && npm test` — 39 testes ainda passam (baseline R2)

**Tests**: none  
**Gate**: Quick FE

**Commit**: `chore(r3): add msw devDependency for auth client tests`

---

### T2: MSW test harness — mswServer + authHandlers

**What**: Factory `createAuthMswServer()` e handlers default para `/auth/refresh` e resource protegido.  
**Where**: `frontend/src/test/mswServer.ts`, `frontend/src/test/handlers/authHandlers.ts`  
**Depends on**: T1  
**Reuses**: `LoginResponse` shape em `frontend/src/types/index.ts`; skill `testing-a11y` MSW handlers  
**Requirement**: AAP3-05

**Tools**:

- MCP: `user-context7` (MSW 2.x API se necessário)
- Skill: `testing-a11y`

**Done when**:

- [x] `createAuthMswServer()` exportado; handlers retornam shape `LoginResponse`
- [x] Lifecycle documentado no arquivo (listen/reset/close)
- [x] Gate: `cd frontend && npm test` — suite existente verde (MSW não wired globalmente)

**Tests**: none (infra consumida por T3)  
**Gate**: Quick FE

**Commit**: `test(r3): add MSW auth harness for api client tests`

---

### T3: api.ts interceptor behavior tests (MSW)

**What**: Criar `api.test.ts` com ≥4 test cases cobrindo matriz AAP3-06; exportar helper `resetApiAuthState()` test-only se necessário para flaky module state.  
**Where**: `frontend/src/services/api.test.ts`; opcional `frontend/src/services/api.ts` (helper test-only)  
**Depends on**: T2  
**Reuses**: `createAuthMswServer`, `TokenService`, instância axios default de `api.ts`  
**Requirement**: AAP3-06

**Tools**:

- MCP: NONE
- Skill: `testing-a11y`

**Done when**:

- [x] Case (a): 401 → refresh OK → retry com novo `Authorization`
- [x] Case (b): refresh falha → tokens limpos + evento `auth:logout`
- [x] Case (c): 2 requests 401 paralelos → fila + refresh único → ambos completam
- [x] Case (d): POST `/auth/refresh` 401 → logout + tokens cleared
- [x] `onUnhandledRequest: 'error'` nos testes MSW
- [x] Gate: `cd frontend && npm test -- api.test` — ≥4 test cases passando
- [x] Gate: `cd frontend && npm test` — total ≥43 test cases

**Tests**: unit  
**Gate**: Quick FE

**Commit**: `test(r3): cover api.ts refresh queue and logout with MSW`

---

### T4: Login — submit credenciais inválidas

**What**: Adicionar test case de submit com `mockLogin.mockRejectedValue` assertando alert e ausência de navigate.  
**Where**: `frontend/src/pages/Login/Login.test.tsx`  
**Depends on**: None (reusa harness R2)  
**Reuses**: `renderWithProviders`, mocks existentes; mensagem exata `"Usuário ou senha inválidos"` (`Login/index.tsx:38`)  
**Requirement**: AAP3-07

**Tools**:

- MCP: NONE
- Skill: `testing-a11y`

**Done when**:

- [x] `screen.getByRole('alert')` com texto **"Usuário ou senha inválidos"**
- [x] `mockNavigate` **not** called
- [x] Gate: `cd frontend && npm test -- Login.test` — ≥4 test cases passando
- [x] Gate: `cd frontend && npm test` — total ≥44 test cases

**Tests**: unit  
**Gate**: Quick FE

**Commit**: `test(r3): assert Login shows error on invalid credentials`

---

### T5: Vitest floor — ≥50 test cases

**What**: Adicionar ≥6 test cases suplementares para atingir floor AAP3-08 (baseline 39 + T3/T4 = 44 → meta ≥50).  
**Where**: Prioridade `frontend/src/services/api.test.ts` (edge cases: 403 como unauthorized, refresh local expirado antes de HTTP, `logout()` limpa tokens); fallback `frontend/src/pages/*/*.test.tsx` se necessário  
**Depends on**: T3, T4  
**Reuses**: MSW harness T2; padrões Vitest R2  
**Requirement**: AAP3-08

**Tools**:

- MCP: NONE
- Skill: `testing-a11y`

**Done when**:

- [x] Gate: `cd frontend && npm run test:coverage` — **≥50** test cases passando
- [x] Nenhum teste R2 deletado silenciosamente
- [x] Gate: `cd frontend && npm run lint` exit 0

**Tests**: unit  
**Gate**: FE Coverage

**Commit**: `test(r3): reach vitest floor of 50 test cases`

---

### T6: Sonar checkpoint — Fase 1

**What**: Rodar `./diversos/scripts/sonar-analyze.sh`; registrar `new_coverage` em notas de task; se &lt;75% iterar FE tests antes de Fase 2.  
**Where**: N/A (gate operacional)  
**Depends on**: T5  
**Reuses**: `diversos/scripts/sonar-analyze.sh`, baseline R2 (`new_coverage` 62.2%)  
**Requirement**: AAP3-01, AAP3-02 (checkpoint), AAP3-03, AAP3-04 (parcial)

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `./diversos/scripts/sonar-analyze.sh` exit 0 (Sonar local UP)
- [x] `new_coverage` registrado em comentário/commit body ou nota em `validation.md` draft
- [x] Se `new_coverage` &lt;75%: adicionar testes FE e re-rodar antes de T7
- [x] `new_violations` = 0 no checkpoint

**Tests**: none  
**Gate**: Sonar

**Commit**: `chore(r3): sonar checkpoint after FE depth phase`

---

### T7: RefreshTokenInvalidoException (domain)

**What**: Criar exceção de domínio e substituir `IllegalStateException` em `AuthenticationService.refreshToken`.  
**Where**: `backend/.../auth/domain/RefreshTokenInvalidoException.java`, `backend/.../auth/application/AuthenticationService.java`  
**Depends on**: T6  
**Reuses**: Constante `MENSAGEM_REFRESH_INVALIDO` existente; padrão `*NotFoundException` em `auth/domain/`  
**Requirement**: AAP3-09 (domain layer)

**Tools**:

- MCP: NONE
- Skill: `spring-security`

**Done when**:

- [x] `refreshToken` lança `RefreshTokenInvalidoException` para token ausente/inválido/revogado
- [x] `AuthenticationServiceTest.refreshToken_inexistente_*` asserta `RefreshTokenInvalidoException` (não `IllegalStateException`)
- [x] Gate: `cd backend && mvn test -Dtest=AuthenticationServiceTest` exit 0
- [x] Gate: `cd backend && mvn test -Dtest=ModularArchitectureTest` exit 0

**Tests**: unit  
**Gate**: Quick BE + Arch

**Commit**: `fix(r3): throw RefreshTokenInvalidoException on invalid refresh`

---

### T8: GlobalExceptionHandler — refresh inválido → 401

**What**: Adicionar `@ExceptionHandler(RefreshTokenInvalidoException.class)` retornando 401 + `ErrorResponse` com mensagem da exceção.  
**Where**: `backend/.../exception/GlobalExceptionHandler.java`, `backend/.../exception/GlobalExceptionHandlerTest.java`  
**Depends on**: T7  
**Reuses**: Padrão handlers existentes; `ErrorResponse`  
**Requirement**: AAP3-09, AAP3-10 (handler layer)

**Tools**:

- MCP: NONE
- Skill: `spring-security`

**Done when**:

- [x] Handler retorna HTTP **401** (não 500 via `Exception.class`)
- [x] Teste em `GlobalExceptionHandlerTest` asserta status 401 + message
- [x] Gate: `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest` exit 0

**Tests**: unit  
**Gate**: Quick BE

**Commit**: `fix(r3): map RefreshTokenInvalidoException to HTTP 401`

---

### T9: WebMvcTest — POST /auth/refresh inválido → 401

**What**: Estender `SecurityConfigAuthRefreshTest` com caso refresh inválido retornando 401 (mock service lança `RefreshTokenInvalidoException`).  
**Where**: `backend/src/test/java/.../config/SecurityConfigAuthRefreshTest.java`  
**Depends on**: T8  
**Reuses**: `@WebMvcTest(controllers = AuthController.class)` existente  
**Requirement**: AAP3-09, AAP3-10

**Tools**:

- MCP: NONE
- Skill: `spring-security`

**Done when**:

- [x] `POST /auth/refresh` com token inválido → status **401** + body com mensagem genérica
- [x] Assert explícito: status **not** 500
- [x] Gate: `cd backend && mvn test -Dtest=SecurityConfigAuthRefreshTest` exit 0

**Tests**: unit (`@WebMvcTest`)  
**Gate**: Quick BE

**Commit**: `test(r3): assert invalid refresh returns 401 not 500`

---

### T10: Testcontainers — pom dependencies

**What**: Adicionar Testcontainers BOM + módulo `postgresql` + integração JUnit Jupiter ao `pom.xml`.  
**Where**: `backend/pom.xml`  
**Depends on**: T6  
**Reuses**: Versão alinhada Spring Boot 3.2.3 BOM  
**Requirement**: AAP3-11 (infra)

**Tools**:

- MCP: `user-context7` (Testcontainers Spring Boot 3 se necessário)
- Skill: NONE

**Done when**:

- [x] Dependências Testcontainers declaradas sem conflito de versão
- [x] Gate: `cd backend && mvn test` — suite completa verde (≥464 testes)
- [x] Gate: `cd backend && mvn test -Dtest=ModularArchitectureTest` exit 0

**Tests**: none  
**Gate**: Full BE + Arch

**Commit**: `chore(r3): add testcontainers dependencies for ADP integration`

---

### T11: ImportacaoFolhaAdpIntegrationTest

**What**: Teste de integração com `@SpringBootTest` + Testcontainers PostgreSQL; importar `folha-adp-minimal.txt`; assert ≥1 linha persistida; rollback via `@Transactional`; skip se Docker indisponível.  
**Where**: `backend/src/test/java/.../importacao/application/ImportacaoFolhaAdpIntegrationTest.java`  
**Depends on**: T10  
**Reuses**: `backend/src/test/resources/importacao/folha-adp-minimal.txt`, `ImportacaoFolhaAdpServiceTest` fixtures  
**Requirement**: AAP3-11, AAP3-12

**Tools**:

- MCP: NONE
- Skill: `jpa-performance`

**Done when**:

- [x] Com Docker: gate `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` exit 0
- [x] Sem Docker: teste skipped (`@EnabledIf` ou equivalente); `mvn test` full suite verde
- [x] ≥1 registro persistido assertado antes de rollback

**Tests**: integration  
**Gate**: ADP Integration (+ Full BE se Docker ausente)

**Commit**: `test(r3): add ImportacaoFolhaAdp integration test with Testcontainers`

---

### T12: S2245 — verify FolhaPagamento React keys

**What**: Verificar ausência de `Math.random()` em keys; adicionar teste regressão ou evidência grep; fix touch-only se encontrado.  
**Where**: `frontend/src/pages/FolhaPagamento/index.tsx` (fix se necessário), `frontend/src/pages/FolhaPagamento/FolhaPagamento.test.tsx`  
**Depends on**: T6  
**Reuses**: Keys estáveis R2 (`key={resumo.id}`)  
**Requirement**: AAP3-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] `grep -r 'Math.random' frontend/src/pages/FolhaPagamento/` retorna vazio
- [x] Teste regressão ou comentário de evidência no test file
- [x] Gate: `cd frontend && npm test -- FolhaPagamento.test` exit 0

**Tests**: unit  
**Gate**: Quick FE

**Commit**: `fix(r3): verify stable React keys in FolhaPagamento (S2245)`

---

### T13: BeneficioMensalService — tx helper extract

**What**: Remover `@SuppressWarnings("java:S6809")`; extrair helpers private non-`@Transactional` seguindo padrão R2 (`FolhaTotalizacaoService`).  
**Where**: `backend/.../beneficios/application/BeneficioMensalService.java`  
**Depends on**: T6  
**Reuses**: Template R2 T12/T13; `BeneficioMensalServiceTest`  
**Requirement**: AAP3-14

**Tools**:

- MCP: NONE
- Skill: `jpa-performance`

**Done when**:

- [x] Zero `@SuppressWarnings("java:S6809")` no arquivo
- [x] Gate: `cd backend && mvn test -Dtest=BeneficioMensalServiceTest` exit 0
- [x] Gate: `cd backend && mvn test -Dtest=ModularArchitectureTest` exit 0
- [x] Diff ≤50 LOC por smell (senão SPEC_DEVIATION + CONCERNS follow-up)

**Tests**: unit  
**Gate**: Quick BE + Arch

**Commit**: `refactor(r3): extract BeneficioMensal tx helpers without S6809 suppress`

---

### T14: Gates finais + CONCERNS + validation.md

**What**: Rodar suite completa de gates; sync `_docs/specs/CONCERNS.md`; criar/atualizar `validation.md` R3 com métricas Sonar finais.  
**Where**: `_docs/specs/CONCERNS.md`, `_docs/specs/features/adequacao-analise-projeto-r3/validation.md`  
**Depends on**: T7, T8, T9, T10, T11, T12, T13  
**Reuses**: Template sync R2 T18/T19; baseline pós-R2 da spec  
**Requirement**: AAP3-01…04, AAP3-15…20, AAP3-19

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `cd backend && mvn test` — 0 falhas, ≥464 testes
- [ ] `bash diversos/scripts/check-jacoco-thresholds.sh` exit 0
- [ ] `./diversos/scripts/sonar-analyze.sh` exit 0 — QG **OK**, `new_coverage` ≥80%, `new_violations` = 0, agregado ≥48%
- [ ] Bugs OPEN = 0; vulns CR+MAJOR = 0
- [ ] `CONCERNS.md` atualizado: S2245 Resolved, BeneficioMensal tx Resolved, FE coverage MSW/api.ts, ADP Mitigated/Resolved ou N/A
- [ ] `validation.md` R3 criado com evidências por AC

**Tests**: none  
**Gate**: Sonar + JaCoCo + Full BE

**Commit**: `docs(r3): sync CONCERNS and validation after gate pass`

---

### T15: Playwright login smoke (P3 opcional)

**What**: Configurar Playwright mínimo + 1 spec login (heading visível + submit).  
**Where**: `frontend/package.json`, `frontend/playwright.config.ts`, `frontend/e2e/login.spec.ts`  
**Depends on**: T14  
**Reuses**: Padrão TARGET em `frontend/AGENTS.md`  
**Requirement**: AAP3-21

**Tools**:

- MCP: NONE
- Skill: `testing-a11y`

**Done when**:

- [ ] `npm run test:e2e` exit 0 com ≥1 spec passando **ou**
- [ ] `validation.md` documenta N/A com motivo (budget/flake/setup)
- [ ] P3 não bloqueia PASS de T14

**Tests**: e2e  
**Gate**: E2E (ou N/A documentado)

**Commit**: `test(r3): add playwright login smoke spec`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 (optional)

Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4 ──→ T5 ──→ T6
Phase 2:  T7 ──→ T8 ──→ T9 ──→ T10 ──→ T11 ──→ T12 ──→ T13 ──→ T14
Phase 3:  T15
```

Execution is strictly sequential — no intra-phase parallelism.

**Sub-agent packing:** Batch 1 (T1–T6, 6 tasks) → Batch 2 (T7–T14, 8 tasks) → Batch 3 optional (T15). Offer sub-agents at Execute if user accepts.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: MSW devDependency | 1 package.json change | ✅ Granular |
| T2: MSW harness | 2 arquivos coesos (factory + handlers) | ✅ Granular |
| T3: api.test.ts | 1 test file + optional helper | ✅ Granular |
| T4: Login invalid test | 1 test case em 1 file | ✅ Granular |
| T5: Vitest floor | testes suplementares distribuídos | ✅ Granular |
| T6: Sonar checkpoint | gate operacional | ✅ Granular |
| T7: Domain exception | 1 exception + service edit | ✅ Granular |
| T8: GEH handler | 1 handler + test | ✅ Granular |
| T9: WebMvcTest 401 | 1 test class extend | ✅ Granular |
| T10: Testcontainers pom | 1 pom.xml | ✅ Granular |
| T11: ADP integration test | 1 integration test class | ✅ Granular |
| T12: S2245 verify | verify + optional 1 LOC fix | ✅ Granular |
| T13: BeneficioMensal tx | 1 service refactor | ✅ Granular |
| T14: Gates + docs | docs + gate run | ✅ Granular |
| T15: Playwright | config + 1 spec | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | None (start) | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | None | T3 → T4 (sequential in phase) | ✅ Match |
| T5 | T3, T4 | T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 | ✅ Match |
| T7 | T6 | T6 → T7 (phase boundary) | ✅ Match |
| T8 | T7 | T7 → T8 | ✅ Match |
| T9 | T8 | T8 → T9 | ✅ Match |
| T10 | T6 | T9 → T10 (sequential; T10 parallel-ready after T6) | ⚠️ Note |
| T11 | T10 | T10 → T11 | ✅ Match |
| T12 | T6 | T11 → T12 | ✅ Match |
| T13 | T6 | T12 → T13 | ✅ Match |
| T14 | T7–T13 | T13 → T14 | ✅ Match |
| T15 | T14 | T14 → T15 | ✅ Match |

**Note T10/T12/T13:** Depend on T6 (phase gate passed), not on auth chain T7–T9. Diagram shows sequential ordering within Phase 2; auth chain T7→T8→T9 completes before T10 starts. T12/T13 can run after T11 in sequence (no dependency on auth tasks).

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1: MSW dep | npm dependency | none | none | ✅ OK |
| T2: MSW harness | FE MSW harness | none | none | ✅ OK |
| T3: api.test.ts | FE auth client | unit (MSW) | unit | ✅ OK |
| T4: Login test | FE Login behavior | unit | unit | ✅ OK |
| T5: Vitest floor | FE auth client / pages | unit | unit | ✅ OK |
| T6: Sonar checkpoint | gate scripts | none | none | ✅ OK |
| T7: Domain exception | Backend auth domain | unit | unit | ✅ OK |
| T8: GEH handler | GlobalExceptionHandler | unit | unit | ✅ OK |
| T9: WebMvcTest | Backend auth HTTP | unit (@WebMvcTest) | unit | ✅ OK |
| T10: Testcontainers pom | pom deps | none | none | ✅ OK |
| T11: ADP integration | Importação ADP | integration | integration | ✅ OK |
| T12: S2245 | FE FolhaPagamento keys | unit | unit | ✅ OK |
| T13: BeneficioMensal tx | BeneficioMensalService | unit | unit | ✅ OK |
| T14: Gates/docs | docs | none | none | ✅ OK |
| T15: Playwright | E2E login | e2e | e2e | ✅ OK |

---

## Requirement Traceability (Task → AC)

| Task | Requirements |
| ---- | ------------ |
| T1, T2 | AAP3-05 |
| T3 | AAP3-06 |
| T4 | AAP3-07 |
| T5 | AAP3-08 |
| T6 | AAP3-01…04 (checkpoint) |
| T7, T8, T9 | AAP3-09, AAP3-10 |
| T10, T11 | AAP3-11, AAP3-12 |
| T12 | AAP3-13 |
| T13 | AAP3-14 |
| T14 | AAP3-01…04, AAP3-15…20, AAP3-19 |
| T15 | AAP3-21 |

**Coverage:** 21 ACs → 15 tasks, 0 unmapped

---

## Próximos passos (TLC)

1. ~~**Specify**~~ — `spec.md`
2. ~~**Design**~~ — `design.md`
3. ~~**Tasks**~~ — este documento (revisão usuário)
4. **Execute** — branch `feat/adequacao-analise-projeto` a partir de `main` @ `cb6e04a`
