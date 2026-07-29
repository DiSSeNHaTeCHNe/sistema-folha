# Adequação da Análise de Projeto — R2 Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/adequacao-analise-projeto-r2/design.md`  
**Spec**: `_docs/specs/features/adequacao-analise-projeto-r2/spec.md`  
**Status**: Draft — Tasks 2026-07-29  
**Git:** branch `feat/adequacao-analise-projeto-r2` a partir de `main` @ `047e64d`; commits atômicos por task; sem push unless user asks.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `AGENTS.md` (raiz, `backend/AGENTS.md`, `frontend/AGENTS.md`), `_docs/specs/TESTING.md`, AD-004 (FE brownfield), AD-010 (ArchUnit), skills `spring-security`, `jpa-performance`, `testing-a11y` (target — apply brownfield patterns only).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Backend application services (folha, organograma, auth, config) | unit (Mockito) | 1:1 to spec AC; regressão pós-refactor tx; timing/JWT cases | `backend/src/test/java/**/**/*Test.java` | `cd backend && mvn test -Dtest=<Class>` |
| GlobalExceptionHandler | unit | Handler validação + existentes 404/400 | `backend/src/test/java/**/exception/*Test.java` | `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest` |
| Backend security (JWT filter, startup validator) | unit | Log redaction; blank/default secret fail paths | `backend/src/test/java/**/security/*Test.java`, `config/*Test.java` | `cd backend && mvn test -Dtest=JwtAuthenticationFilterTest,JwtSecretStartupValidatorTest` |
| ArchUnit modular boundaries | unit | Zero violação AD-010 | `backend/src/test/java/**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Frontend pages (Login, Folha, Organograma) | unit (Vitest + Testing Library) | ≥1 teste render/behavior por página; queries by role/label | `frontend/src/pages/**/*.test.tsx` | `cd frontend && npm test` |
| Frontend services (tokenService) | unit (Vitest) | Expiração, clear, localStorage round-trip | `frontend/src/services/*.test.ts` | `cd frontend && npm test` |
| FE test harness | none | Compila; consumido por page tests | `frontend/src/test/renderWithProviders.tsx` | `cd frontend && npm test` |
| Gate scripts (sonar-analyze, jacoco-thresholds) | none | Exit 0; FE lcov gerado | `diversos/scripts/*.sh` | `bash diversos/scripts/check-jacoco-thresholds.sh` |
| Spring YAML profiles (ddl-auto) | none | Build + unit suite verde | `backend/src/main/resources/application*.yml` | `cd backend && mvn test` |
| Sonar smell remediation (touch-only) | none | Re-scan smells ≤230; top-20 CR+MAJOR zero | touched source files | `./diversos/scripts/sonar-analyze.sh` |
| Docs (CONCERNS sync) | none | Review gate | `_docs/specs/CONCERNS.md` | manual |
| ADP integration (P3 optional) | integration | ≥1 linha persistida com rollback | `backend/src/test/java/**/importacao/**/*IntegrationTest.java` | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick BE | Backend task with focused unit test | `cd backend && mvn test -Dtest=<ClassTest>` |
| Arch | Every backend code task | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Quick FE | FE test task | `cd frontend && npm test` |
| FE Coverage | After FE test tasks / before Sonar | `cd frontend && npm run test:coverage` |
| JaCoCo | After T9+ / phase 1b end | `bash diversos/scripts/check-jacoco-thresholds.sh` |
| Full BE | Phase 2 end / before Verifier | `cd backend && mvn test` (≥359 tests, 0 failures) |
| Build FE | FE-only smoke | `cd frontend && npm run lint && npm run build` |
| Sonar | End phase 1b + final R2 | `./diversos/scripts/sonar-analyze.sh` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

**Batch sizing (Execute — offer sub-agents if user accepts):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| **Batch 1** | Phase 1a | T1 → T7 | 7 |
| **Batch 2** | Phase 1b | T8 → T11 | 4 |
| **Batch 3** | Phase 2 | T12 → T19 | 8 |
| **Batch 4** (optional) | Phase 3 | T20 | 1 |

### Phase 1a: Pipeline + FE coverage

```
T1 → T2 → T3 → T4 → T5 → T6 → T7
```

### Phase 1b: Backend coverage + Sonar hygiene

```
T8 → T9 → T10 → T11
```

### Phase 2: CONCERNS + gates

```
T12 → T13 → T14 → T15 → T16 → T17 → T18 → T19
```

### Phase 3: Integração opcional

```
T20
```

---

## Task Breakdown

### T1: sonar-analyze.sh — FE lcov antes do scanner

**What**: Inserir `npm run test:coverage` no pipeline Sonar antes do `docker run sonar-scanner`.  
**Where**: `diversos/scripts/sonar-analyze.sh`  
**Depends on**: None  
**Reuses**: `sonar-project.properties` (`frontend/coverage/lcov.info`)  
**Requirement**: AAP2-06

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Script executa `(cd frontend && npm run test:coverage)` após JaCoCo backend
- [x] Aviso stderr se `frontend/coverage/lcov.info` ausente
- [x] Gate: `./diversos/scripts/sonar-analyze.sh` exit 0 (Sonar local UP)

**Tests**: none  
**Gate**: Sonar (manual — requer Sonar UP)

**Commit**: `fix(r2): run vitest coverage before sonar scan`

---

### T2: FE test harness — renderWithProviders

**What**: Helper de render com `MemoryRouter` + mock `AuthContext` para page tests.  
**Where**: `frontend/src/test/renderWithProviders.tsx`  
**Depends on**: None  
**Reuses**: `frontend/src/test/setup.ts`, padrão Vitest R1 (`smoke.test.tsx`)  
**Requirement**: AAP2-08 (infra)

**Tools**:
- MCP: NONE
- Skill: `testing-a11y` (brownfield — by role queries)

**Done when**:
- [x] Export `renderWithProviders(ui, options?)` utilizável por page tests
- [x] Gate: `cd frontend && npm test` — testes existentes ainda passam (≥2)

**Tests**: none (infra consumida por T4–T6)  
**Gate**: Quick FE

**Commit**: `test(r2): add renderWithProviders harness`

---

### T3: tokenService unit tests

**What**: Testes Vitest para `TokenService` (expiração, clear, localStorage).  
**Where**: `frontend/src/services/tokenService.test.ts`  
**Depends on**: None  
**Reuses**: `frontend/src/services/tokenService.ts`  
**Requirement**: AAP2-08

**Tools**:
- MCP: NONE
- Skill: `testing-a11y` (brownfield)

**Done when**:
- [x] ≥4 test cases passando (`isTokenExpired`, `clearTokens`, set/get round-trip)
- [x] Gate: `cd frontend && npm test` — total ≥6 test cases (2 smoke + 4 novos)

**Tests**: unit  
**Gate**: Quick FE

**Commit**: `test(r2): cover TokenService expiration and storage`

---

### T4: Login page Vitest

**What**: Testes render/behavior da página Login (campos por role, submit).  
**Where**: `frontend/src/pages/Login/Login.test.tsx`  
**Depends on**: T2  
**Reuses**: `renderWithProviders`, mock `useAuth`  
**Requirement**: AAP2-09

**Tools**:
- MCP: NONE
- Skill: `testing-a11y`

**Done when**:
- [x] ≥3 test cases passando (heading/campos login+senha visíveis por role ou label)
- [x] Gate: `cd frontend && npm test` — total ≥9 test cases

**Tests**: unit  
**Gate**: Quick FE

**Commit**: `test(r2): add Login page vitest coverage`

---

### T5: FolhaPagamento page Vitest

**What**: Testes smoke render da página Folha com services mockados.  
**Where**: `frontend/src/pages/FolhaPagamento/FolhaPagamento.test.tsx`  
**Depends on**: T2  
**Reuses**: `vi.mock` em `folhaPagamentoService`, `resumoFolhaPagamentoService`  
**Requirement**: AAP2-09

**Tools**:
- MCP: NONE
- Skill: `testing-a11y`

**Done when**:
- [x] ≥3 test cases passando (render título/aba principal sem HTTP real)
- [x] Gate: `cd frontend && npm test` — total ≥12 test cases

**Tests**: unit  
**Gate**: Quick FE

**Commit**: `test(r2): add FolhaPagamento page vitest smoke`

---

### T6: Organograma page Vitest

**What**: Testes smoke render da página Organograma com services mockados.  
**Where**: `frontend/src/pages/Organograma/Organograma.test.tsx`  
**Depends on**: T2  
**Reuses**: `vi.mock` em `organogramaService`, `funcionarioService`, `centroCustoService`  
**Requirement**: AAP2-09, AAP2-08 (≥15 total)

**Tools**:
- MCP: NONE
- Skill: `testing-a11y`

**Done when**:
- [x] ≥3 test cases passando (heading ou toggle lista/gráfico visível)
- [x] Gate: `cd frontend && npm run test:coverage` — **≥15** test cases passando (AAP2-08)
- [x] `frontend/coverage/lcov.info` gerado

**Tests**: unit  
**Gate**: FE Coverage

**Commit**: `test(r2): add Organograma page vitest smoke`

---

### T7: GlobalExceptionHandler — MethodArgumentNotValidException

**What**: Handler Bean Validation + teste unitário.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/exception/GlobalExceptionHandler.java`
- `backend/src/test/java/br/com/techne/sistemafolha/exception/GlobalExceptionHandlerTest.java`

**Depends on**: None  
**Reuses**: `ErrorResponse`, padrão handlers existentes  
**Requirement**: AAP2-11

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `@ExceptionHandler(MethodArgumentNotValidException.class)` retorna 400 com campos concatenados
- [x] Teste `handleMethodArgumentNotValidException_retorna400` passa
- [x] Gate: `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: Quick BE + Arch

**Commit**: `fix(r2): add validation handler to GlobalExceptionHandler`

---

### T8: Backend tests — fechar gap JaCoCo global 75%

**What**: Estender/criar testes unitários backend onde JaCoCo report indicar gap até global ≥75% **antes** de subir o script threshold.  
**Where**: Classes identificadas via `backend/target/site/jacoco/jacoco.xml` pós-`mvn test` (prioridade: `folha.application`, `auth.application` abaixo da média)  
**Depends on**: T7  
**Reuses**: Padrão `@ExtendWith(MockitoExtension.class)` R1  
**Requirement**: AAP2-07

**Tools**:
- MCP: NONE
- Skill: `jpa-performance` (se tocar queries/repos mockados)

**Done when**:
- [x] JaCoCo global ≥75% medido manualmente no XML (ou script com threshold 75 temporário)
- [x] Gate: `cd backend && mvn test` — ≥359 tests, 0 failures
- [x] Gate: `cd backend && mvn test -Dtest=ModularArchitectureTest`

**Tests**: unit  
**Gate**: Full BE (partial) + Arch

**Commit**: `test(r2): expand backend unit tests for jacoco 75%`

---

### T9: JaCoCo script — global threshold 75%

**What**: Atualizar `check-jacoco-thresholds.sh` global 65% → 75%.  
**Where**: `diversos/scripts/check-jacoco-thresholds.sh`  
**Depends on**: T8  
**Reuses**: Script R1  
**Requirement**: AAP2-07

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `"global": 75.0` no script Python embebido
- [x] Floors R1 inalterados (organograma 50, security 40, importacao 75)
- [x] Gate: `bash diversos/scripts/check-jacoco-thresholds.sh` exit 0

**Tests**: none  
**Gate**: JaCoCo

**Commit**: `chore(r2): raise jacoco global threshold to 75%`

---

### T10: Sonar smell remediation — backend top-20

**What**: Corrigir smells CRITICAL+MAJOR (touch-only, ≤50 LOC/issue) nos arquivos backend do export top-20 **exceto** tx services (T12–T13).  
**Where**: Arquivos do export Sonar — ex.: `ImportacaoFolhaAdpService`, `OrganogramaService`, `FolhaPagamentoService`, `SecurityConfig`, etc.  
**Depends on**: T9  
**Reuses**: Matriz top-20 em `design.md`  
**Requirement**: AAP2-04, AAP2-05 (partial)

**Tools**:
- MCP: `user-sonarqube` (export issues, if available)
- Skill: NONE

**Done when**:
- [x] Zero issues CR+MAJOR `sinceLeakPeriod=true` nos arquivos backend top-20 (exceto tx — fixed in T12–T13)
- [x] Refactors >50 LOC registrados em CONCERNS follow-up, não nesta task
- [x] Gate: `cd backend && mvn test` + `ModularArchitectureTest`

**Tests**: none (smell-only touches)  
**Gate**: Full BE + Arch

**Commit**: `fix(r2): sonar smell cleanup backend top-20`

---

### T11: Sonar smell remediation — frontend + gate Fase 1

**What**: Smells FE top-20 (ex. S2245 se presente) + verificar métricas Sonar Fase 1.  
**Where**: `frontend/src/pages/FolhaPagamento/index.tsx`, `Organograma/index.tsx`, outros do export  
**Depends on**: T10, T6  
**Reuses**: Vitest já cobre páginas  
**Requirement**: AAP2-01, AAP2-02, AAP2-03, AAP2-04, AAP2-10 (partial)

**Tools**:
- MCP: `user-sonarqube`
- Skill: NONE

**Done when**:
- [x] `code_smells` ≤230 (ou delta ≥15% vs 270)
- [x] Zero CR+MAJOR top-20 frontend export
- [x] Gate: `./diversos/scripts/sonar-analyze.sh` exit 0
- [x] Evidência: `new_violations`=0; cobertura agregada ≥48% (ou documentar gap para validation)

**Tests**: none  
**Gate**: Sonar + FE Coverage

**Commit**: `fix(r2): sonar smell cleanup frontend and phase1 gate`

---

### T12: FolhaTotalizacaoService — tx self-invocation fix

**What**: Extract `calcularTotaisPorFuncionarioInterno` private sem `@Transactional`; eliminar S2229.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaTotalizacaoService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaTotalizacaoServiceTest.java`

**Depends on**: T11  
**Reuses**: Padrão design C1  
**Requirement**: AAP2-12

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Sem chamada `@Transactional` via `this` entre métodos públicos
- [x] `FolhaTotalizacaoServiceTest` verde — sem deleção de testes
- [x] Gate: `cd backend && mvn test -Dtest=FolhaTotalizacaoServiceTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: Quick BE + Arch

**Commit**: `fix(r2): remove transactional self-invocation in FolhaTotalizacaoService`

---

### T13: OrganogramaAcessoService — tx self-invocation fix

**What**: Extract `resolverContextoAcesso` private sem `@Transactional`; eliminar S2229.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/organograma/acesso/application/OrganogramaAcessoService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/organograma/acesso/application/OrganogramaAcessoServiceTest.java`

**Depends on**: T12  
**Reuses**: Padrão design C2  
**Requirement**: AAP2-13

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Port interface inalterada; ACL behavior preserved
- [x] `OrganogramaAcessoServiceTest` verde
- [x] Gate: `cd backend && mvn test -Dtest=OrganogramaAcessoServiceTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: Quick BE + Arch

**Commit**: `fix(r2): remove transactional self-invocation in OrganogramaAcessoService`

---

### T14: ddl-auto validate + application-dev.yml

**What**: Default `validate`; `update` só com profile `dev`.  
**Where**:
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml` (novo)

**Depends on**: T13  
**Reuses**: Flyway canônico (B6)  
**Requirement**: AAP2-14

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer` (consulta — sem migration nesta task)

**Done when**:
- [x] `application.yml`: `ddl-auto: validate`
- [x] `application-dev.yml`: `ddl-auto: update`
- [x] Gate: `cd backend && mvn test` — 0 failures

**Tests**: none  
**Gate**: Full BE

**Commit**: `fix(r2): ddl-auto validate by default, update in dev profile`

---

### T15: JwtSecretStartupValidator hardening

**What**: Fail em secret blank ou default em profile ≠ dev/test.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/config/JwtSecretStartupValidator.java`
- `backend/src/test/java/br/com/techne/sistemafolha/config/JwtSecretStartupValidatorTest.java`

**Depends on**: T14  
**Reuses**: `DEFAULT_JWT_SECRET` constant  
**Requirement**: AAP2-15

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [x] Blank secret → `IllegalStateException` (non-dev/test)
- [x] Default secret + profile staging/default (sem dev/test) → fail
- [x] dev/test + default → warn only
- [x] Gate: `cd backend && mvn test -Dtest=JwtSecretStartupValidatorTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: Quick BE + Arch

**Commit**: `fix(r2): harden JwtSecretStartupValidator for non-dev profiles`

---

### T16: AuthenticationService — timing-safe dummy BCrypt

**What**: Sempre invocar `passwordEncoder.matches` com dummy hash quando usuário inexistente.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/auth/application/AuthenticationService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/auth/application/AuthenticationServiceTest.java`

**Depends on**: T15  
**Reuses**: Mensagem genérica AAP-08 R1  
**Requirement**: AAP2-16

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [x] Login inexistente chama `matches(senha, DUMMY_BCRYPT_HASH)`
- [x] Teste verifica `verify(passwordEncoder).matches(any(), eq(DUMMY_BCRYPT_HASH))`
- [x] Mensagem genérica mantida
- [x] Gate: `cd backend && mvn test -Dtest=AuthenticationServiceTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: Quick BE + Arch

**Commit**: `fix(r2): constant-time login path with dummy bcrypt hash`

---

### T17: JwtAuthenticationFilter — log redaction

**What**: Remover log do valor do header Authorization.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/security/JwtAuthenticationFilter.java`
- `backend/src/test/java/br/com/techne/sistemafolha/security/JwtAuthenticationFilterTest.java`

**Depends on**: T16  
**Reuses**: JwtAuthenticationFilterTest R1  
**Requirement**: AAP2-17

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [x] Log debug não inclui `authHeader` value
- [x] Teste confirma filter chain continua; sem token literal no log (appender ou assert message)
- [x] Gate: `cd backend && mvn test -Dtest=JwtAuthenticationFilterTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: Quick BE + Arch

**Commit**: `fix(r2): redact authorization header from jwt filter logs`

---

### T18: CONCERNS.md sync — P1 items resolved

**What**: Marcar resolvidos: self-invocation tx, ddl-auto, JWT hardening, timing login.  
**Where**: `_docs/specs/CONCERNS.md`  
**Depends on**: T12–T17  
**Reuses**: Template sync R1  
**Requirement**: AAP2-20

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Tech Debt `ddl-auto` → Mitigated/Resolved
- [x] Security JWT → hardening complete note
- [x] Sonar follow-ups table: S2229 tx → Resolved
- [x] Timing login noted resolved

**Tests**: none  
**Gate**: manual review

**Commit**: `docs(r2): sync CONCERNS after P1 debt closure`

---

### T19: Full gate suite — regression zero

**What**: Rodar suite completa; confirmar AAP2-18/19/21; preparar evidências para Verifier.  
**Where**: repo-wide (no code unless gate failure)  
**Depends on**: T18  
**Reuses**: Todos os gates  
**Requirement**: AAP2-01, AAP2-02, AAP2-03, AAP2-18, AAP2-19, AAP2-21

**Tools**:
- MCP: `user-sonarqube` (QG API measures)
- Skill: NONE

**Done when**:
- [x] `cd backend && mvn test` — ≥359 tests, 0 failures
- [x] `bash diversos/scripts/check-jacoco-thresholds.sh` exit 0
- [x] `cd frontend && npm run test:coverage` — ≥15 tests
- [x] `./diversos/scripts/sonar-analyze.sh` exit 0; bugs=0; vulns CR+MAJOR=0
- [x] `ModularArchitectureTest` zero violações
- [x] QG OK **or** ≤2 exceções anotadas para Verifier escrever em `validation.md`

**Tests**: none (gate-only)  
**Gate**: Full BE + JaCoCo + FE Coverage + Sonar + Arch

**Commit**: `chore(r2): phase2 full gate evidence`

> **Note:** `validation.md` is written by the Verifier after T19, not in this task.

**Gate evidence (2026-07-29):** BE 411 tests 0 failures; JaCoCo global 76.8% PASS; FE 16 Vitest PASS; sonar-analyze exit 0.

---

### T20: ImportacaoFolhaAdp integration test (optional P3)

**What**: Um teste integração ADP com Testcontainers + rollback.  
**Where**:
- `backend/pom.xml` (testcontainers deps)
- `backend/src/test/java/.../importacao/application/ImportacaoFolhaAdpIntegrationTest.java`
- Reuse `backend/src/test/resources/importacao/folha-adp-minimal.txt`

**Depends on**: T19 (P1–P2 verdes)  
**Reuses**: `ImportacaoFolhaAdpServiceTest` asserts  
**Requirement**: AAP2-22

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [ ] Teste persiste ≥1 linha com `@Transactional` rollback **or** Testcontainers teardown
- [ ] Gate: `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest`
- [x] Se Docker indisponível: documentar N/A em validation (não bloqueia R2) — Testcontainers: `Could not find a valid Docker environment` (2026-07-29)

**Tests**: integration  
**Gate**: Quick BE (integration class)

**Commit**: `test(r2): add ImportacaoFolhaAdp integration test with testcontainers`

---

## Phase Execution Map

```
Phase 1a:  T1 ──→ T2 ──→ T3 ──→ T4 ──→ T5 ──→ T6 ──→ T7
Phase 1b:  T8 ──→ T9 ──→ T10 ──→ T11
Phase 2:   T12 ──→ T13 ──→ T14 ──→ T15 ──→ T16 ──→ T17 ──→ T18 ──→ T19
Phase 3:   T20 (optional)
```

Execution is strictly sequential — one task at a time, in order.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: sonar-analyze FE step | 1 script | ✅ Granular |
| T2: renderWithProviders | 1 helper file | ✅ Granular |
| T3: tokenService tests | 1 test file | ✅ Granular |
| T4: Login tests | 1 page test file | ✅ Granular |
| T5: FolhaPagamento tests | 1 page test file | ✅ Granular |
| T6: Organograma tests | 1 page test file | ✅ Granular |
| T7: GEH validation handler | 1 handler + 1 test | ✅ Granular |
| T8: Backend JaCoCo gap tests | multiple test files (cohesive goal) | ✅ OK — single metric target |
| T9: JaCoCo script threshold | 1 script | ✅ Granular |
| T10: Backend smell batch | multiple files, one Sonar goal | ✅ OK — R1 precedent |
| T11: Frontend smell + Sonar gate | FE files + verify | ✅ OK — phase gate |
| T12: FolhaTotalizacao tx | 1 service + regression test | ✅ Granular |
| T13: OrganogramaAcesso tx | 1 service + regression test | ✅ Granular |
| T14: ddl-auto profiles | 2 YAML files | ✅ Granular |
| T15: JWT validator | 1 class + tests | ✅ Granular |
| T16: Auth timing | 1 service + tests | ✅ Granular |
| T17: JWT filter logs | 1 filter + tests | ✅ Granular |
| T18: CONCERNS sync | 1 doc file | ✅ Granular |
| T19: Full gates | gate-only | ✅ Granular |
| T20: ADP integration | 1 integration test + pom | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ------------------- | ------------- | ------ |
| T1 | None | entry | ✅ Match |
| T2 | None | after T1 chain | ✅ Match (T2 independent; OK) |
| T3 | None | parallel to T2 | ✅ Match |
| T4 | T2 | T2→T4 | ✅ Match |
| T5 | T2 | T2→T5 | ✅ Match |
| T6 | T2 | T2→T6 | ✅ Match |
| T7 | None | after T6 | ✅ Match |
| T8 | T7 | T7→T8 | ✅ Match |
| T9 | T8 | T8→T9 | ✅ Match |
| T10 | T9 | T9→T10 | ✅ Match |
| T11 | T10, T6 | T10→T11 | ✅ Match |
| T12 | T11 | T11→T12 | ✅ Match |
| T13 | T12 | T12→T13 | ✅ Match |
| T14 | T13 | T13→T14 | ✅ Match |
| T15 | T14 | T14→T15 | ✅ Match |
| T16 | T15 | T15→T16 | ✅ Match |
| T17 | T16 | T16→T17 | ✅ Match |
| T18 | T12–T17 | T17→T18 | ✅ Match |
| T19 | T18 | T18→T19 | ✅ Match |
| T20 | T19 | T19→T20 | ✅ Match |

No backward dependencies. ✅

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | Gate script | none | none | ✅ OK |
| T2 | FE harness | none | none | ✅ OK |
| T3 | FE service | unit | unit | ✅ OK |
| T4 | FE Login page | unit | unit | ✅ OK |
| T5 | FE Folha page | unit | unit | ✅ OK |
| T6 | FE Organograma page | unit | unit | ✅ OK |
| T7 | GlobalExceptionHandler | unit | unit | ✅ OK |
| T8 | Backend services | unit | unit | ✅ OK |
| T9 | JaCoCo script | none | none | ✅ OK |
| T10 | Smell remediation BE | none | none | ✅ OK |
| T11 | Smell remediation FE | none | none | ✅ OK |
| T12 | FolhaTotalizacaoService | unit | unit | ✅ OK |
| T13 | OrganogramaAcessoService | unit | unit | ✅ OK |
| T14 | YAML config | none | none | ✅ OK |
| T15 | JwtSecretStartupValidator | unit | unit | ✅ OK |
| T16 | AuthenticationService | unit | unit | ✅ OK |
| T17 | JwtAuthenticationFilter | unit | unit | ✅ OK |
| T18 | CONCERNS doc | none | none | ✅ OK |
| T19 | Gates | none | none | ✅ OK |
| T20 | ADP integration | integration | integration | ✅ OK |

All tasks pass co-location validation. ✅

---

## MCPs & Skills (Execute prep)

Before Execute, confirm tools per batch:

| Tool | Use in R2 |
| ---- | --------- |
| **MCP SonarQube** (`user-sonarqube`) | T10, T11, T19 — export issues, QG measures |
| **Skill `testing-a11y`** | T2–T6 — Vitest by role/label |
| **Skill `spring-security`** | T15–T17 — JWT/auth hardening |
| **Skill `jpa-performance`** | T8, T12–T13, T20 — tx/service tests |
| **Skill `tlc-spec-driven`** | All tasks — Execute + Verifier |

Default: filesystem + shell; Sonar MCP optional if local API unavailable (use dashboard manual).
