# Adequação da Análise de Projeto Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/adequacao-analise-projeto/design.md`  
**Spec**: `_docs/specs/features/adequacao-analise-projeto/spec.md`  
**Status**: Execute complete — Verifier **PASS** @ `d22a374` (fix-cycle-2)

**Git:** branch `feat/adequacao-analise-projeto`; commits atômicos por task; sem push.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `AGENTS.md`, AD-010 (ArchUnit), skills `spring-security`, `jpa-performance`, `testing-a11y` (P3 FE).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Backend services (benefícios, organograma, importação, auth) | unit (Mockito) | 1:1 to spec ACs; every listed edge case | `backend/src/test/java/**/**/*Test.java` | `cd backend && mvn test -Dtest=<Class>` |
| Backend controllers (upload ADP) | unit (MockMvc) | Happy + edge (null filename → 400) | `backend/src/test/java/**/api/*WebMvcTest.java` | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpControllerWebMvcTest` |
| Backend security (JWT filter, JwtService, startup validator) | unit | Header ausente/inválido/válido; prod default secret fail | `backend/src/test/java/**/security/*Test.java`, `config/*Test.java` | `cd backend && mvn test -Dtest=JwtAuthenticationFilterTest` |
| GlobalExceptionHandler | unit | ≥3 handlers (404, 400, validation) | `backend/src/test/java/**/exception/*Test.java` | `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest` |
| ArchUnit regression | unit | Zero violação AD-010 | `backend/src/test/java/**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Frontend component (OrganogramaGrafico) | none (lint/build) | Build gate; Vitest smoke in T16 | `frontend/src/components/OrganogramaGrafico/` | `cd frontend && npm run lint && npm run build` |
| JaCoCo gate script | none | Script exit 0/1 | `diversos/scripts/check-jacoco-thresholds.sh` | `bash diversos/scripts/check-jacoco-thresholds.sh` |
| Docs (INTEGRATIONS, CONCERNS) | none | Review gate | `_docs/specs/*.md` | manual |
| Frontend Vitest (P3) | unit | ≥1 smoke test pass | `frontend/src/**/*.test.ts` | `cd frontend && npm test` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Backend task with focused unit/MockMvc tests | `cd backend && mvn test -Dtest=<ClassTest>` |
| Full | Phase completion or multi-class backend | `cd backend && mvn test` |
| Build | FE-only or docs-only; phase 3 FE | `cd frontend && npm run lint && npm run build` |
| Arch | Every backend code task | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| JaCoCo | T12+ | `bash diversos/scripts/check-jacoco-thresholds.sh` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

**Batch sizing (delegated):**
- **Batch 1:** Phase 1 — T1…T7 (7 tasks)
- **Batch 2:** Phase 2 — T8…T13 (6 tasks)
- **Batch 3:** Phase 3 — T14…T16 (3 tasks)

### Phase 1: P1 — Reliability + Security

```
T1 → T2 → T3 → T4 → T5 → T6 → T7
```

### Phase 2: P2 — Coverage + Gate

```
T8 → T9 → T10 → T11 → T12 → T13
```

### Phase 3: P3 — Hygiene + FE baseline

```
T14 → T15 → T16
```

---

## Task Breakdown

### T1: BeneficioMensalService transactional fix

**What**: Adicionar `@Transactional` em `criarParaUsuario` e `removerSeAutorizado`; testes provam persistência em fluxo autorizado.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/application/BeneficioMensalService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/beneficios/application/BeneficioMensalServiceTest.java`

**Depends on**: None  
**Reuses**: Padrão `@Transactional` existente em `criar`/`remover`  
**Requirement**: AAP-01

**Done when**:
- [x] `@Transactional` em `criarParaUsuario` e `removerSeAutorizado` (read-write)
- [x] Teste verifica save/softDelete em fluxo autorizado
- [x] Gate: `cd backend && mvn test -Dtest=BeneficioMensalServiceTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: quick + Arch

**Commit:** `0dde345`

---

### T2: ImportacaoFolhaAdpController null-safe upload

**What**: Guard null/blank `getOriginalFilename()` → 400 antes de `.toLowerCase()`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/importacao/api/ImportacaoFolhaAdpController.java`
- `backend/src/test/java/br/com/techne/sistemafolha/importacao/api/ImportacaoFolhaAdpControllerWebMvcTest.java`

**Depends on**: None  
**Reuses**: `ImportacaoFolhaAdpResponseDTO.error`  
**Requirement**: AAP-02

**Done when**:
- [x] Multipart sem filename → `status().isBadRequest()`
- [x] Mensagem clara no corpo (não 500/NPE)
- [x] Gate: `cd backend && mvn test -Dtest=ImportacaoFolhaAdpControllerWebMvcTest,ModularArchitectureTest`

**Tests**: unit (MockMvc)  
**Gate**: quick + Arch

**Commit:** `2446142`

---

### T3: OrganogramaService construirArvore NPE guard

**What**: Skip node quando `toDTOCompleto` retorna null ou `dto.id()` null.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/organograma/application/OrganogramaService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/organograma/application/OrganogramaServiceTest.java` (novo)

**Depends on**: None  
**Reuses**: Guard em `toDTO`  
**Requirement**: AAP-03

**Done when**:
- [x] Guard `if (dto == null || dto.id() == null) continue;` em `construirArvore`
- [x] Teste unitário cobre path null-safe (mock repo)
- [x] Gate: `cd backend && mvn test -Dtest=OrganogramaServiceTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: quick + Arch

**Commit:** `48bdb6a`

---

### T4: OrganogramaGrafico conditional fix

**What**: Remover ternário redundante `borderColor: isExpanded ? 'primary.main' : 'primary.main'`.  
**Where**: `frontend/src/components/OrganogramaGrafico/index.tsx`

**Depends on**: None  
**Reuses**: MUI theme tokens  
**Requirement**: AAP-04

**Done when**:
- [x] Expressão distinta ou ternário removido (Sonar S3923 cleared)
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: build

**Commit:** `07224c7`

---

### T5: SecurityConfig audit + CSRF documentation

**What**: Auditar matchers vs rotas reais; documentar CSRF disabled em INTEGRATIONS.md.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/config/SecurityConfig.java` (só se correção necessária)
- `_docs/specs/INTEGRATIONS.md`
- Testes regressão existentes em `backend/src/test/java/br/com/techne/sistemafolha/config/SecurityConfig*Test.java`

**Depends on**: None  
**Reuses**: `diversos/scripts/check-modular-compliance.sh`  
**Requirement**: AAP-06, AAP-07

**Done when**:
- [x] Matchers obsoletos removidos/corrigidos se encontrados
- [x] Seção Security em INTEGRATIONS.md: JWT stateless, CSRF disabled rationale
- [x] Gate: `cd backend && mvn test -Dtest=SecurityConfigTipoBeneficioTest,SecurityConfigAuthRefreshTest,ModularArchitectureTest`

**Tests**: unit (regressão existente)  
**Gate**: quick + Arch

**Commit:** `df2ff03`

---

### T6: AuthenticationService anti-enumeration + JwtSecretStartupValidator

**What**: Unificar mensagens login inválido; fail-fast JWT default em prod.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/auth/application/AuthenticationService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/config/JwtSecretStartupValidator.java` (novo)
- `backend/src/test/java/br/com/techne/sistemafolha/auth/application/AuthenticationServiceTest.java` (novo ou estender)
- `backend/src/test/java/br/com/techne/sistemafolha/config/JwtSecretStartupValidatorTest.java` (novo)

**Depends on**: None  
**Reuses**: Catch final já unificado em AuthenticationService  
**Requirement**: AAP-08, AAP-09

**Done when**:
- [x] Login inexistente e senha errada → mesma mensagem genérica
- [x] Profile prod + default secret → `IllegalStateException` no startup
- [x] Dev profile → WARN once, app sobe
- [x] Gate: `cd backend && mvn test -Dtest=AuthenticationServiceTest,JwtSecretStartupValidatorTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: quick + Arch

**Commit:** `12021f3`

---

### T7: OrganogramaController debug removal

**What**: Substituir `System.out/err` e `printStackTrace` por Logger SLF4J.  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/organograma/api/OrganogramaController.java`

**Depends on**: None  
**Reuses**: Padrão Logger do projeto  
**Requirement**: AAP-10 (partial S4507)

**Done when**:
- [x] Zero System.out/err/printStackTrace no controller
- [x] Gate: `cd backend && mvn test -Dtest=ModularArchitectureTest`

**Tests**: none  
**Gate**: Arch

**Commit:** `ca7427e`

---

### T8: OrganogramaServiceTest + OrganogramaAcessoServiceTest expansion

**What**: Testes unitários elevando cobertura organograma ≥50%.  
**Where**:
- `backend/src/test/java/br/com/techne/sistemafolha/organograma/application/OrganogramaServiceTest.java`
- `backend/src/test/java/br/com/techne/sistemafolha/organograma/acesso/application/OrganogramaAcessoServiceTest.java`

**Depends on**: T3  
**Reuses**: Fixtures OrganogramaAcessoServiceTest  
**Requirement**: AAP-11

**Done when**:
- [x] Cenários: construirArvore 2 níveis, cadastrar, validarCicloHierarquico, obterOrganogramaAtivo
- [x] Expandir ACL scoped CC parcial
- [x] Gate: `cd backend && mvn test -Dtest=OrganogramaServiceTest,OrganogramaAcessoServiceTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: quick + Arch

**Commit:** `19ede27` (+ extra `7828ec1` coverage for JaCoCo gate)

---

### T9: JwtAuthenticationFilterTest + JwtServiceTest

**What**: Testes security elevando cobertura ≥40%.  
**Where**:
- `backend/src/test/java/br/com/techne/sistemafolha/security/JwtAuthenticationFilterTest.java` (novo)
- `backend/src/test/java/br/com/techne/sistemafolha/security/JwtServiceTest.java` (novo)

**Depends on**: T6  
**Reuses**: `SecurityConfigAuthRefreshTest` token patterns  
**Requirement**: AAP-12

**Done when**:
- [x] Filter: sem header, inválido, válido → SecurityContext
- [x] JwtService: generate/validate round-trip
- [x] Gate: `cd backend && mvn test -Dtest=JwtAuthenticationFilterTest,JwtServiceTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: quick + Arch

**Commit:** `3773d33`

---

### T10: ImportacaoFolhaAdpServiceTest

**What**: Fixture mínima + happy path + 1 falha validação.  
**Where**:
- `backend/src/test/resources/importacao/folha-adp-minimal.txt`
- `backend/src/test/java/br/com/techne/sistemafolha/importacao/application/ImportacaoFolhaAdpServiceTest.java` (novo)

**Depends on**: T2  
**Reuses**: `ImportacaoBeneficioMensalServiceTest` structure  
**Requirement**: AAP-13, AAP-14

**Done when**:
- [x] Happy path parse + assert save count
- [x] Arquivo vazio/layout inválido → exceção esperada
- [x] Gate: `cd backend && mvn test -Dtest=ImportacaoFolhaAdpServiceTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: quick + Arch

**Commit:** `c2d195c`

---

### T11: GlobalExceptionHandlerTest

**What**: Teste unitário direto ≥3 handlers.  
**Where**: `backend/src/test/java/br/com/techne/sistemafolha/exception/GlobalExceptionHandlerTest.java` (novo)

**Depends on**: None  
**Reuses**: Padrão unitário @RestControllerAdvice  
**Requirement**: AAP-15

**Done when**:
- [x] FuncionarioNotFoundException → 404
- [x] IllegalArgumentException → 400
- [x] BeneficioMensalNotFoundException → 404 (ou handler validação)
- [x] Gate: `cd backend && mvn test -Dtest=GlobalExceptionHandlerTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: quick + Arch

**Commit:** `a887d02`

---

### T12: check-jacoco-thresholds.sh

**What**: Script parse jacoco.xml com limiares AAP-11…16.  
**Where**: `diversos/scripts/check-jacoco-thresholds.sh`

**Depends on**: T8, T9, T10, T11  
**Reuses**: Algoritmo análise chat (Python inline)  
**Requirement**: AAP-16, AAP-17

**Done when**:
- [x] Fail se organograma <50%, security <40%, importacao <75%, global <65%
- [x] Exit 0 após `cd backend && mvn test` + script
- [x] Gate: `bash diversos/scripts/check-jacoco-thresholds.sh`

**Tests**: none  
**Gate**: JaCoCo

**Commit:** `bc37db3`

---

### T13: Sonar gate verification + CONCERNS.md sync

**What**: Rodar sonar-analyze; atualizar CONCERNS; documentar exceções QG se necessário.  
**Where**:
- `_docs/specs/CONCERNS.md`
- `_docs/specs/features/adequacao-analise-projeto/validation.md` (notas preliminares se QG ERROR)

**Depends on**: T12  
**Reuses**: `./diversos/scripts/sonar-analyze.sh`  
**Requirement**: AAP-05, AAP-10, AAP-17, AAP-18, AAP-19

**Done when**:
- [x] Sonar bugs OPEN = 0; vulns CRITICAL+MAJOR = 0 (API ou dashboard) — **partial: 4 vulns remain, documented AAP-18**
- [x] CONCERNS.md: itens resolvidos marcados; pendentes datados
- [x] Gate: `cd backend && mvn test` + `./diversos/scripts/sonar-analyze.sh` (se Sonar disponível)

**Tests**: none  
**Gate**: full

**Commit:** `c78f140`

---

### T14: JwtAuthenticationFilter logger rename

**What**: Renomear field `logger` → `log` (Sonar S1149).  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/security/JwtAuthenticationFilter.java`

**Depends on**: T9  
**Reuses**: SLF4J convention  
**Requirement**: AAP-21

**Done when**:
- [x] Field renomeado; compila
- [x] Gate: `cd backend && mvn test -Dtest=JwtAuthenticationFilterTest,ModularArchitectureTest`

**Tests**: unit (regressão T9)  
**Gate**: quick + Arch

**Commit:** `b2035aa`

---

### T15: Touch-only Sonar hygiene + transactional follow-up

**What**: Zero blocker/critical nos arquivos tocados P1/P2; AAP-22 follow-up em CONCERNS se services não tocados.  
**Where**: Arquivos alterados em P1/P2; `_docs/specs/CONCERNS.md` se follow-up

**Depends on**: T13  
**Reuses**: Sonar issues search nos paths tocados  
**Requirement**: AAP-20, AAP-22

**Done when**:
- [x] Sonar blocker/critical = 0 nos paths tocados
- [x] `@Transactional` via `this` em services não alterados → entrada CONCERNS follow-up
- [x] Gate: `cd backend && mvn test`

**Tests**: none  
**Gate**: full

**Commit:** `6bcdfd4`

---

### T16: Vitest baseline + optional lcov

**What**: Setup Vitest + ≥1 smoke test; lcov para Sonar se viável.  
**Where**:
- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/src/**/*.test.ts`
- `sonar-project.properties` (se AAP-24)

**Depends on**: T4  
**Reuses**: Vite 6 / TS 5.8 versions  
**Requirement**: AAP-23, AAP-24

**Done when**:
- [x] `cd frontend && npm test` exit 0
- [x] ≥1 teste passando
- [x] lcov configurado OU N/A registrado em validation notes
- [x] Gate: `cd frontend && npm test && npm run lint && npm run build`

**Tests**: unit  
**Gate**: build

**Commit:** `1e138a3`

---

## Phase Execution Map

```
Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4 ──→ T5 ──→ T6 ──→ T7
Phase 2:  T8 ──→ T9 ──→ T10 ──→ T11 ──→ T12 ──→ T13
Phase 3:  T14 ──→ T15 ──→ T16
```

**Batch dispatch:**
- Batch 1: Phase 1 (T1–T7)
- Batch 2: Phase 2 (T8–T13)
- Batch 3: Phase 3 (T14–T16)

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1–T16 | 1 deliverable each | ✅ Granular |

## Diagram-Definition Cross-Check

| Task | Depends On | Diagram Shows | Status |
| ---- | ---------- | ------------- | ------ |
| T1–T7 | per body | Phase 1 chain | ✅ |
| T8 | T3 | after T3 | ✅ |
| T9 | T6 | after T6 | ✅ |
| T10 | T2 | after T2 | ✅ |
| T12 | T8–T11 | after coverage tasks | ✅ |
| T13 | T12 | after T12 | ✅ |
| T14 | T9 | after T9 | ✅ |
| T15 | T13 | after T13 | ✅ |
| T16 | T4 | after T4 | ✅ |

## Test Co-location Validation

| Task | Layer | Matrix Requires | Task Says | Status |
| ---- | ----- | --------------- | --------- | ------ |
| T1–T3,T6,T8–T11 | service/controller | unit | unit | ✅ |
| T4,T7,T12,T13,T15 | FE/docs/script | none/build | none/build | ✅ |
| T16 | FE | unit | unit | ✅ |
