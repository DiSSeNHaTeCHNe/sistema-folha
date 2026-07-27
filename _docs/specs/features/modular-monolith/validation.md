# Monólito Modular Validation

**Date**: 2026-07-26  
**Spec**: `_docs/specs/features/modular-monolith/spec.md`  
**Diff range**: working tree uncommitted (modular-monolith + modular-monolith-fix)  
**Verifier**: independent sub-agent (author ≠ verifier) — **re-verify** after `modular-monolith-fix` PASS  
**Freshness**: re-derived from scratch against current tree; prior FAIL conclusions not copied blindly

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1–T32 | ✅ Done | All `Done when` checkboxes marked `[x]` in `tasks.md` |
| MOD-23 | ✅ Done | This report |

---

## Spec-Anchored Acceptance Criteria

### P1: Remover modelo dual e legado `Beneficio`

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| WHEN backend compiled THEN no `Beneficio.java` / `BeneficioRepository` / legacy queries | Zero legacy artifacts | `glob **/Beneficio.java` → 0; `rg \bBeneficioRepository\b backend/src/main` → only `TipoBeneficioRepository` (same domain) | ✅ PASS |
| WHEN `FolhaTotalizacaoService` totals THEN only via `BeneficioConsultaPort` | Port injection only | `folha/application/FolhaTotalizacaoService.java:3,31` — `BeneficioConsultaPort`; `rg BeneficioMensalRepository folha/` → 0 | ✅ PASS |
| WHEN `DashboardService` aggregates THEN via port, no `BeneficioRepository` | Port for benefit metrics | `dashboard/application/DashboardService.java:11,43` — `BeneficioConsultaPort`; no legacy `BeneficioRepository` in tree | ✅ PASS |
| WHEN Flyway applied THEN idempotent drop `beneficios` + indexes | `DROP INDEX IF EXISTS` ×3 + `DROP TABLE IF EXISTS` | `db/migration/V1.14__drop_beneficios_legado.sql:1-4` | ✅ PASS |
| WHEN FE build THEN no `beneficioService.ts` / legacy `Beneficio` types | Orphans absent | `rg beneficioService frontend/src` → 0; `types/index.ts` — `BeneficioMensal*` only | ✅ PASS |
| WHEN totalization/dashboard unit tests THEN no `BeneficioRepository` mock | Mocks use port | `rg BeneficioRepository backend/src/test` → 0 legacy name; only `TipoBeneficioRepository` in beneficios tests | ✅ PASS |

### P1: `BeneficioConsultaPort` in-process (somente mensal)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| Port exposes sum/count/exists; no JPA entities | 4 methods; primitive returns | `beneficios/port/BeneficioConsultaPort.java:8-16` | ✅ PASS |
| Folha/Dashboard depend only on port | No `BeneficioMensalRepository` in consumers | `FolhaTotalizacaoService.java:31`; `DashboardService.java:43` | ✅ PASS |
| Adapter reads only Benefícios infra | `BeneficioMensalRepository` only | `BeneficioConsultaAdapter.java:13-17,23-29` — `@Service implements BeneficioConsultaPort` | ✅ PASS |
| No monthly data → zero/count zero, no exception | `ZERO` / `0` / `false` | `BeneficioConsultaAdapterTest` (6 tests) green in gate | ✅ PASS |
| `@Service` in `beneficios`, wired by interface | Spring bean on adapter | `BeneficioConsultaAdapter.java:13-15` | ✅ PASS |

### P1: Controllers finos — sem injeção de repository

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| Controllers in CONCERNS have no `*Repository` fields | Zero repository injection | `rg Repository **/*Controller.java backend/src/main` → 0 matches | ✅ PASS |
| `BeneficioMensalController` delegates to service | Service-only dependency | `BeneficioMensalControllerWebMvcTest.java:53-56` — `status().isOk()` + `verify(beneficioMensalService).listarPorCompetenciaParaUsuario(...)` | ✅ PASS |
| Folha controllers delegate queries to services | Service layer owns persistence | Controllers under `folha.api.*`; services in `folha.application` | ✅ PASS |
| `AuthController` uses auth service, not repo | No `UsuarioRepository` in controller | `auth/api/AuthController.java:22,47` — `authenticationService.obterAcessoUsuarioPorLogin(...)` | ✅ PASS |
| `mvn test` passes; optional MockMvc confirms delegation | Suite green; MockMvc optional | Gate: 86 tests, 0 failures; `BeneficioMensalControllerWebMvcTest` added; no `AuthController*WebMvcTest` (optional per spec AC5) | ⚠️ Spec-precision gap (Auth MockMvc deferred — non-blocking) |

### P1: Pacotes por domínio — leva Benefícios

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| Benefícios under `beneficios.{api,application,domain,infrastructure,port}` | Layered package | e.g. `beneficios/api/BeneficioMensalController.java`, `beneficios/port/BeneficioConsultaPort.java` | ✅ PASS |
| Cross-domain imports use public packages | Folha imports `beneficios.port` not infra | `FolhaTotalizacaoService.java:3` — `beneficios.port.BeneficioConsultaPort` | ✅ PASS |
| Spring registers beans; routes preserved | Controllers at `/beneficio-mensal`, `/tipo-beneficio` | ArchUnit `controllers_must_reside_in_api_layer` green; gate BUILD SUCCESS | ✅ PASS |
| Zero `sistemafolha.service.BeneficioMensalService` | Class relocated | Package under `beneficios.application` | ✅ PASS |
| Domain tests pass | Benefícios test classes exit 0 | Gate: Beneficio* / TipoBeneficio* / Importacao* included in 86-test suite | ✅ PASS |

### P1: `OrganogramaAcessoPort` + correção ACL

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| Port exposes 3 methods + typed DTO with distinct signals | Fields per design | `organograma/acesso/port/OrganogramaAcessoPort.java`; `AcessoUsuarioDTO` builder fields | ✅ PASS |
| No funcionário → deny all | `temFuncionarioVinculado=false`, empty set, access false | `OrganogramaAcessoServiceTest.java:59-64` — `assertFalse` ×3, `assertEquals(SEM_FUNCIONARIO, ...)` | ✅ PASS |
| Funcionário sem nó → deny | `temNoOrganograma=false`, empty set, access false | `OrganogramaAcessoServiceTest.java:77-82` | ✅ PASS |
| Funcionário com nó → node + descendants, `acessoTotal=false` | Restricted set, not total | `OrganogramaAcessoServiceTest.java:104-107` — `assertEquals(Set.of(100L), ...)`, `assertFalse(contexto.acessoTotal())` | ✅ PASS |
| `acessoTotal=true` only via explicit flag, never from empty set | No empty-set total derivation | `OrganogramaAcessoService` `negar()` path; tests assert `acessoTotal=false` on deny | ✅ PASS |
| Folha/Benefícios/Dashboard use port not concrete ACL service | Port injection only outside organograma.acesso | Consumers use `OrganogramaAcessoPort`; ArchUnit `acl_consumers_must_not_access_organograma_internals` green | ✅ PASS |
| `GET /auth/acesso` JSON reflects distinct ACL signals | DTO maps all flags | `AuthController.java:43-47` delegates to `AuthenticationService.java:138-150`; `AuthenticationServiceAcessoTest.java:75-80` — SEM_FUNCIONARIO mapping; `:105-113` — grant parcial via `obterAcessoUsuarioPorLogin` | ⚠️ Spec-precision gap — service-level DTO proof, not HTTP MockMvc (mapping path proven; parent AC8 covers ACL scenarios separately) |
| ACL unit tests cover 3 denial/grant scenarios | 3 scenarios proven | `OrganogramaAcessoServiceTest.java:52-133` — 4 tests | ✅ PASS |

### P1: Alinhamento frontend mínimo (5A)

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| `pages/` do not import `api.ts` directly | Zero direct api imports | `rg "from.*services/api" frontend/src/pages` → 0 | ✅ PASS |
| Build: no orphans (`beneficioService`, Example, dead App) | Files absent / in graph | `rg beneficioService frontend` → 0; checklist FE modular PASS | ✅ PASS |
| Services follow domain naming | `*Service.ts` per aggregate | e.g. `folhaPagamentoService.ts`, `funcionarioService.ts` | ✅ PASS |
| `AuthContext` uses distinct ACL fields; no empty=total | Deny when invalid context | `AuthContext.tsx:145-146` — `if (!temFuncionarioVinculado \|\| !temNoOrganograma) return false` | ✅ PASS |
| `npm run build` exit 0; `npm run lint` **advisory** (AD-004) | Build mandatory; lint not hard gate | `npm run build` → exit 0; `npm run lint` → exit 1 (50 problems); checklist treats as advisory per amended AC5 | ✅ PASS |
| Legacy `Beneficio` types removed from `types/index.ts` | No orphan legacy type | `types/index.ts` — `BeneficioMensal` only | ✅ PASS |

### P1: `SecurityConfig` — alinhamento de paths

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| Matchers consistent with `context-path: /api` | Relative paths without `/api` prefix | `SecurityConfig.java:33-44` — `/auth/login`, `/tipo-beneficio` | ✅ PASS |
| Obsolete `/api/beneficios/**` removed | Matcher absent | `rg /api/beneficios SecurityConfig.java` → 0 | ✅ PASS |
| POST `/tipo-beneficio` requires ADMIN | `hasRole("ADMIN")` on POST | `SecurityConfig.java:42` | ✅ PASS |
| Security test: POST without ADMIN → 403; with ADMIN → 2xx | Status codes | `SecurityConfigTipoBeneficioTest.java:50` — `status().isForbidden()`; `:63` — `status().is2xxSuccessful()` | ✅ PASS |
| Login/refresh/permitAll unchanged | Public login preserved | `SecurityConfig.java:33` — `POST /auth/login` permitAll | ✅ PASS |

### P2: ArchUnit — enforcement crescente

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| ArchUnit fails on (a) domain X → infra Y; (b) controller repo injection; (c) Folha/Dashboard→Benefícios infra | Rules catch forbidden imports | `ModularArchitectureTest.java:128-196` — five per-domain `..application..` rules; gate: **16 rules**, 0 violations | ✅ PASS (AD-009 allowlist debt — see below) |
| Folha migrated → Folha↔Benefícios ports only | `folha..` must not import `beneficios.infrastructure..` | `ModularArchitectureTest.java:32-37,143-154` | ✅ PASS |
| Cadastros/Organograma rules expand without relaxing prior | Cumulative rules present | `ModularArchitectureTest.java:47-93,110-124,157-182` | ✅ PASS |
| `archunit-junit5` compatible Java 17 / Boot 3.2 | Version 1.4.2 | `pom.xml` — `archunit-junit5:1.4.2`; gate green | ✅ PASS |

**AD-009 documented debt (not parent FAIL):**

| Location | Foreign infra import | ArchUnit coverage |
| -------- | -------------------- | ----------------- |
| `dashboard/application/DashboardService.java:15-16` | `cadastros.infrastructure.FuncionarioRepository`, `RubricaRepository` | Explicit allowlist in `ModularArchitectureTest.java:125-126` `because` (AD-009) — no application rule for `dashboard..` yet |
| `importacao/application/ImportacaoFolhaAdpService.java:9-12` | `cadastros.infrastructure.*` | Same AD-009 deferred Approach A |

**P1 offenders refactored (fix closed prior hard gap):**

| Domain | Evidence |
| ------ | -------- |
| `beneficios.application` | `BeneficioMensalService.java:18` — `FuncionarioConsultaPort`; grep `cadastros.infrastructure` in `beneficios/**/application/**` → 0 |
| `folha.application` | `FolhaPagamentoService.java:13-14` — `CadastrosLookupPort`, `UsuarioLookupPort`; grep → 0 foreign cadastros infra |
| `organograma.application` | Port imports only; grep → 0 |
| `auth.application` | `UsuarioService` uses `FuncionarioConsultaPort`; grep → 0 |

### P2: Migrar domínios Folha e demais

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| Folha classes in `folha.*` layered packages | Package migration complete | e.g. `folha/application/FolhaTotalizacaoService.java`, `folha/api/FolhaPagamentoController.java` | ✅ PASS |
| Organograma + `acesso` submodule in `organograma.*` | Consolidated ACL | `organograma/acesso/application/OrganogramaAcessoService.java` | ✅ PASS |
| Cadastros in `cadastros.*` | Aggregates migrated | e.g. `cadastros/application/FuncionarioService.java` | ✅ PASS |
| All Swagger routes remain (compile/package gate) | No startup regression | `mvn test` BUILD SUCCESS (86 tests) | ✅ PASS |
| ArchUnit P2 active, no violations in main | Suite green | `ModularArchitectureTest` — 16 rules, 0 violations | ✅ PASS |

### P2: Logging estruturado por domínio

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| Application services log `domain=<nome>` | MDC/prefix on migrated modules | `DomainLogging.prefix(DOMAIN)` across auth, folha, beneficios, organograma, cadastros, importacao, dashboard | ✅ PASS |
| ACL denial → WARN with `domain=organograma`, `usuarioId`, `motivoNegacao` | Structured deny log | Gate log output: `ACL negado usuarioId=10 motivoNegacao=SEM_FUNCIONARIO` | ✅ PASS |
| Logback config retrocompatible | Default levels unchanged | No logback config changes required; tests pass | ✅ PASS |

### P3: Checklist + ARCHITECTURE.md

| Criterion | Spec-defined outcome | Evidence | Result |
| --------- | -------------------- | -------- | ------ |
| `validation.md` exists (Verifier) | This file | `_docs/specs/features/modular-monolith/validation.md` | ✅ PASS |
| Checklist BE+FE reproducible | Script exit 0 mandatory | `./diversos/scripts/check-modular-compliance.sh` — **Mandatory checks: PASS**; lint advisory FAIL | ✅ PASS |
| `ARCHITECTURE.md` updated | Dual model removed; ports documented | Ports + modular packages; no “Dual benefit domain” section | ✅ PASS |

**Status**: ✅ All ACs covered — **2 ⚠️ spec-precision gaps** (optional Auth MockMvc; service-level vs HTTP for `/auth/acesso`); **AD-009 allowlist debt** documented, not blocking

---

## Discrimination Sensor

Mutations applied on **temp copies** only (`cp -R backend` → `/tmp/modular-monolith-sensor-*`); production tree untouched; scratch discarded after each run.

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| 1 | `AuthenticationService.java:141` | Flip `.temFuncionarioVinculado(contexto.temFuncionarioVinculado())` → negated | ✅ Killed — `AuthenticationServiceAcessoTest` BUILD FAILURE (2 errors) |
| 2 | `BeneficioMensalService.java:18` | Inject `import …cadastros.infrastructure.FuncionarioRepository` | ✅ Killed — `ModularArchitectureTest` BUILD FAILURE |
| 3 | `SecurityConfig.java:42` | Replace `hasRole("ADMIN")` with `permitAll()` on POST `/tipo-beneficio` | ✅ Killed — `SecurityConfigTipoBeneficioTest` BUILD FAILURE |

**Sensor depth**: lightweight (3 targeted mutations — ACL DTO mapping, ArchUnit application isolation, security)  
**Result**: 3/3 killed — ✅ PASS

---

## Interactive UAT Results

Not performed — backend/infrastructure refactor; automated gates sufficient per spec scope.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ Refactor-only; fix feature scoped to ports + ArchUnit |
| No scope creep | ✅ |
| Matches existing patterns | ✅ Lombok, Mockito unit tests, layered packages |
| Spec-anchored outcome check | ✅ Hard gaps closed; 2 non-blocking spec-precision gaps |
| Per-layer coverage (domain 1:1 ACs; routes) | ✅ Strong BE unit + ArchUnit; optional Auth MockMvc deferred |
| Every test maps to spec requirement | ✅ Test matrix in tasks.md aligned |
| Documented guidelines | ✅ `TESTING.md`, `backend/AGENTS.md` §4, checklist script, AD-004/AD-009 |

---

## Edge Cases

| Edge case | Result |
| --------- | ------ |
| Partial `beneficio_mensal` per competência → sum per employee, zero otherwise | ✅ `FolhaTotalizacaoServiceTest` green |
| Flyway drop idempotent (`IF EXISTS`) | ✅ `V1.14__drop_beneficios_legado.sql` |
| Admin global future → explicit flag only | ✅ `negar()` never sets `acessoTotal=true` from empty set |
| Descendant centers when node has none | ✅ Recursive collection in `OrganogramaAcessoService` |
| Port null competência/list → neutral, no NPE | ✅ Adapter validation + Folha empty-list handling |
| FE 403/ACL → empty state | ⚠️ No E2E; `AuthContext` deny logic verified statically |
| Controller refactor → DTO compatible | ✅ Compile + existing service tests |
| Package break → fail-fast startup | ✅ `mvn test` / ArchUnit controllers-in-api |
| Legacy-only `beneficios` data → zero post-drop | ✅ By design (MOD-01) |

---

## Gate Check

- **Gate command**: `cd backend && mvn test` (tasks.md Quick); Build gate `mvn clean package` + `npm run build`; checklist `./diversos/scripts/check-modular-compliance.sh`
- **Backend result**: BUILD SUCCESS — **86 tests**, 0 failed, 0 skipped (includes **16** ArchUnit rule evaluations)
- **Targeted gate**: `mvn test -Dtest=ModularArchitectureTest,AuthenticationServiceAcessoTest` — **18 passed**, 0 failed
- **Frontend build**: SUCCESS (`tsc -b && vite build`) exit 0
- **Frontend lint**: exit 1 — 50 problems; **advisory** per amended AC5 + AD-004 + checklist
- **Checklist**: `./diversos/scripts/check-modular-compliance.sh` — **Mandatory checks: PASS**; lint advisory FAIL (expected)
- **Test count before feature (baseline)**: 62 (prior parent validation, pre-fix)
- **Test count after (parent + fix combined)**: 86
- **Delta**: +24 tests (ports/adapters, ACL DTO, ArchUnit application rules, MockMvc smoke)
- **Failures**: None on mandatory gates

---

## Fix closure vs remaining debt

### Closed by `modular-monolith-fix` (verified in parent re-check)

| Prior hard gap | Closure evidence |
| -------------- | ---------------- |
| P2 AC1(a) ArchUnit application-layer foreign infra | `ModularArchitectureTest.java:128-196`; offenders refactored to ports; gate 16/16 rules green |
| P1 ACL AC7 `/auth/acesso` DTO mapping | `AuthenticationServiceAcessoTest.java:75-80,105-113` — exact field assertions |
| FE lint hard FAIL | Parent AC5 amended (lint advisory); checklist AD-004 messaging |
| Optional controller MockMvc (partial) | `BeneficioMensalControllerWebMvcTest.java:53-56` strengthens P1 Controllers AC |

### Remaining optional / documented debt (non-blocking)

| Item | Status |
| ---- | ------ |
| AD-009 allowlist `dashboard.application` + `importacao.application` | Documented debt — follow-up ports required; not parent FAIL |
| `AuthController*WebMvcTest` (MODFIX-16) | Deferred P2 optional |
| `/auth/acesso` HTTP MockMvc | Spec-precision gap — service-level proof sufficient for parent PASS |
| FE lint 50 problems | Advisory per AD-004 |

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| MOD-01–MOD-14, MOD-17–MOD-22, MOD-24–MOD-28, MOD-30 | Verified / In Tasks | ✅ Verified |
| MOD-15, MOD-16 | ❌ Needs Fix (application cross-infra) | ✅ Verified (AD-009 allowlist documented) |
| MOD-23 | Pending (Verifier) | ✅ Verified (this report) |
| MOD-29 | ⚠️ Partial (static only) | ✅ Verified (service-level DTO test; HTTP optional) |
| MOD-11 | ✅ Verified (lint advisory) | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: All P1+P2+P3 ACs matched with evidence; **2 ⚠️ spec-precision gaps** (non-blocking); prior **2 hard gaps closed** by fix feature  
**Sensor**: 3/3 mutations killed  
**Gate**: 86/86 backend passed; FE build passed; checklist mandatory PASS; lint advisory (expected)

**What works**: Legacy `Beneficio` removed; ports wired; ACL deny scenarios proven; application-layer isolation enforced for P1 offenders; FE modular greps + build green; SecurityConfig ADMIN enforced; discrimination sensor strong; ARCHITECTURE.md updated.

**Issues found**: None blocking. AD-009 dashboard/importacao ports and optional Auth MockMvc are follow-ups.

**Next steps**: User commits; optional `modular-acl-security-fix`; roadmap to remove AD-009 allowlist.
