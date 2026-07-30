# Adequação da Análise de Projeto — R2 Validation

## Status atual
- Veredito: **PASS ✅** (closing verifier @ 2026-07-29)
- Spec: adequacao-analise-projeto-r2
- Branch/HEAD: `feat/adequacao-analise-projeto` @ `658be8a`
- Diff range: `0e767e3..658be8a` (R2 full; fix-cycles through fix-cycle-3)
- ACs: **20/21** executable PASS · **1** N/A (AAP2-22) · **1** QG exception (`new_coverage` **62.2%** < 80%, slot 1/2 per AAP2-01/AAP2-03)
- Gates: `mvn test` **464** 0 fail · JaCoCo global **81.7%** · Vitest **39** · ArchUnit **18** · `sonar-analyze.sh` exit **0** · QG **ERROR** (`new_coverage` only; `new_violations` **0**)
- Sensor: **3/3** auth/JWT mutations killed (worktree scratch @ final verify)

---

## Execução: adequacao-analise-projeto-r2 — 2026-07-29 — 3efa85e..471a3d6

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** `feat/adequacao-analise-projeto` @ `471a3d6`  
**Diff range:** `3efa85e^..471a3d6` (19 commits R2)

### Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1–T19 | ✅ Done | All marked done in `tasks.md`; T19 gate evidence recorded |
| T20 | ⏭️ N/A | Docker/Testcontainers unavailable — documented per edge case |

---

### Spec-Anchored Acceptance Criteria

| ID | Criterion (WHEN → THEN) | Spec-defined outcome | `file:line` + assertion | Result |
| -- | ----------------------- | -------------------- | ----------------------- | ------ |
| AAP2-01 | `./diversos/scripts/sonar-analyze.sh` completes | QG **OK** OR ≤2 exceptions in validation | Sonar API `@2026-07-29`: `project_status.status` = **ERROR** (`new_coverage` 60.8, `new_violations` 66); script exit 0 | ❌ GAP — QG not OK; 2 exceptions documented below (does not waive AAP2-02) |
| AAP2-02 | Sonar `new_violations` | **0** | Sonar API measure `new_violations` = **66** | ❌ GAP |
| AAP2-03 | Sonar `new_coverage` | **≥ 80%** OR single documented exception | Sonar API `new_coverage` = **60.8%** | ⚠️ Exception — see QG exceptions; below 80% threshold |
| AAP2-04 | Sonar `code_smells` | **≤ 230** (≥15% vs 270) | Sonar API `code_smells` = **189** (30% reduction) | ✅ PASS |
| AAP2-05 | CR+MAJOR top-20 export `sinceLeakPeriod=true` | **0** issues in top files | Sonar API issues search CR+MAJOR `sinceLeakPeriod=true`: **total 0** | ✅ PASS |
| AAP2-06 | `sonar-analyze.sh` runs | **`npm run test:coverage`** before Docker scanner | `diversos/scripts/sonar-analyze.sh:35-36` — `(cd frontend && npm run test:coverage)` | ✅ PASS |
| AAP2-07 | `check-jacoco-thresholds.sh` | global **≥ 75%**; floors R1 | Verifier run: global **76.8%**, organograma 66.6%, security 74.7%, importacao 76.5% — exit 0 | ✅ PASS |
| AAP2-08 | `npm run test:coverage` | **≥ 15** Vitest cases passing | Verifier run: **16 passed** (5 files) | ✅ PASS |
| AAP2-09 | FE pages Folha/Organograma/Login | **≥ 1 test each** | `Login.test.tsx:25-28` — `getByRole('heading', { name: 'Sistema de Folha' })`; `FolhaPagamento.test.tsx:31-34` — `getByRole('heading', { name: 'Folha de Pagamento' })`; `Organograma.test.tsx:28-31` — `getByRole('heading', { name: /Organograma/i })` | ✅ PASS |
| AAP2-10 | Sonar aggregate coverage | **≥ 48%** | Sonar API `coverage` = **44.7%** | ❌ GAP |
| AAP2-11 | GlobalExceptionHandler validation | test for `MethodArgumentNotValidException` | `GlobalExceptionHandlerTest.java:79-84` — `assertEquals(HttpStatus.BAD_REQUEST, …)` + message `"login: must not be blank; senha: size must be between 8 and 64"` | ✅ PASS |
| AAP2-12 | FolhaTotalizacaoService tx self-invocation | refactor; `FolhaTotalizacaoServiceTest` green | `FolhaTotalizacaoService.java:51-56` — private `calcularTotaisPorFuncionarioInterno` (no `@Transactional`); `FolhaTotalizacaoServiceTest` — 7 tests, 0 failures (gate) | ✅ PASS |
| AAP2-13 | OrganogramaAcessoService tx self-invocation | same pattern; test green | `OrganogramaAcessoService.java:77` — private `resolverContextoAcesso`; `OrganogramaAcessoServiceTest` — 8 tests, 0 failures (gate) | ✅ PASS |
| AAP2-14 | `application.yml` ddl-auto | **`validate`** default; **`update`** only `dev` | `application.yml:10` — `ddl-auto: validate`; `application-dev.yml:4` — `ddl-auto: update` | ✅ PASS |
| AAP2-15 | JWT blank/default secret | fail startup non-dev/test | `JwtSecretStartupValidatorTest.java:26-27,46-47,72-73` — `assertThrows(IllegalStateException.class, …)` prod/staging/blank; `72-73` blank | ✅ PASS |
| AAP2-16 | Login missing user timing-safe | `passwordEncoder.matches` vs dummy hash | `AuthenticationService.java:51-52` — `DUMMY_BCRYPT_HASH`; `AuthenticationServiceTest.java:78` — `verify(passwordEncoder).matches(SENHA, AuthenticationService.DUMMY_BCRYPT_HASH)` | ✅ PASS |
| AAP2-17 | JWT filter log redaction | no full Authorization value in log | `JwtAuthenticationFilterTest.java:73-76` — `noneMatch(…contains("eyJhbGciOiJIUzI1NiJ9"))` + `contains("Bearer esperado")` | ✅ PASS |
| AAP2-18 | `mvn test` | **0** failures; count **≥ 359** | Verifier run (outside sandbox): **411** tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS | ✅ PASS |
| AAP2-19 | Sonar bugs/vulns regression | bugs OPEN **0**; vulns CR+MAJOR **0** | Sonar API: `bugs`=0, `vulnerabilities`=0, OPEN CR+MAJOR total **0** | ✅ PASS |
| AAP2-20 | CONCERNS sync | mark tx/ddl/JWT/timing resolved | `_docs/specs/CONCERNS.md:25,64,164-166,172` — Resolved entries for R2 items | ✅ PASS |
| AAP2-21 | ArchUnit AD-010 | zero violations | `ModularArchitectureTest` — 18 tests, 0 failures (gate) | ✅ PASS |
| AAP2-22 | ADP integration (P3 optional) | persist ≥1 row with rollback OR N/A documented | T20 not implemented; `tasks.md:598` — Testcontainers `Could not find a valid Docker environment` | ⏭️ N/A |

**Spec-anchored summary:** 17/21 executable ACs PASS · 3 GAP · 1 exception (AAP2-03) · 1 N/A (AAP2-22)

#### QG exceptions (AAP2-01 / AAP2-03 — ≤2 allowed)

1. **`new_coverage` = 60.8% (< 80%)** — Baseline `PREVIOUS_VERSION` @ 2026-07-27; FE Vitest expanded (16 tests) but incremental leak-period coverage still below gate. Plan: R3 FE coverage (AD-004) or Sonar baseline reset on main post-merge.
2. **`new_violations` = 66** — Historical leak-period debt vs 2026-07-27 baseline; **not waived** (AAP2-02 remains FAIL). Requires baseline reconciliation or incremental smell/violation burn-down.

---

### Gate Check

| Gate | Command | Exit | Evidence |
| ---- | ------- | ---- | -------- |
| Full BE | `cd backend && mvn test` | 0 | 411 tests, 0 failures (+52 vs R1 baseline 359) |
| JaCoCo | `bash diversos/scripts/check-jacoco-thresholds.sh` | 0 | global 76.8% ≥ 75% |
| FE Coverage | `cd frontend && npm run test:coverage` | 0 | 16 tests passed; lcov generated |
| Sonar | `./diversos/scripts/sonar-analyze.sh` | 0 | Analysis uploaded; QG status ERROR (metrics above) |
| ArchUnit | `cd backend && mvn test -Dtest=ModularArchitectureTest` | 0 | 18 tests, 0 failures |

**Note:** `mvn test` fails in Cursor sandbox (Mockito `MockMaker` init — 337 errors). Gates require full permissions / local shell.

**Skipped tests:** none in committed suite. Stale `ImportacaoFolhaAdpIntegrationTest` surefire artifact from aborted T20 attempt — not in source tree; clean run excludes it.

---

### Discrimination Sensor (P0 auth/JWT — 3 mutations, scratch worktree)

| # | Mutation | File:line | Killed? |
| - | -------- | --------- | ------- |
| 1 | Disable blank-secret check `if (jwtSecret.isBlank())` → `if (false)` | `JwtSecretStartupValidator.java:32` | ✅ Killed — `JwtSecretStartupValidatorTest#validateJwtSecret_blankSecret_falhaStartup` FAIL |
| 2 | Missing-user login skips dummy hash (`""` instead of `DUMMY_BCRYPT_HASH`) | `AuthenticationService.java:51` | ✅ Killed — `AuthenticationServiceTest#authenticate_loginInexistente_lancaMensagemGenerica` FAIL (`verify(matches…DUMMY_BCRYPT_HASH)`) |
| 3 | Validation handler returns 500 instead of 400 | `GlobalExceptionHandler.java` (BAD_REQUEST→INTERNAL_SERVER_ERROR) | ✅ Killed — `GlobalExceptionHandlerTest#handleMethodArgumentNotValidException_retorna400` FAIL |

**Sensor depth:** P0 manual (3/3 killed) — **PASS ✅**

---

### Code Quality (spot-check)

| Principle | Status |
| --------- | ------ |
| Minimum scope / no product features | ✅ |
| Matches repo patterns | ✅ |
| Tests map to ACs (non-shallow auth/JWT) | ✅ |
| Documented guidelines (`TESTING.md`, skills) | ✅ |

---

### Edge Cases

- [x] QG fails on `new_coverage` only → exception path used (AAP2-03); aggregate still below 48% (AAP2-10 FAIL)
- [x] Testcontainers unavailable → AAP2-22 N/A documented
- [x] Tx refactor → existing service tests green (no SPEC_DEVIATION)

---

### Summary

**Overall:** ❌ Not Ready

**What works:** JaCoCo ≥75%; FE 16 Vitest; CONCERNS P1 closed; auth/JWT hardening with discriminating tests; smells 189 ≤230; bugs/vulns 0; ArchUnit green; `mvn test` 411 green.

**Blockers:**
1. **AAP2-02** — `new_violations` = 66 (requires Sonar baseline reconciliation or incremental fix)
2. **AAP2-10** — aggregate Sonar coverage 44.7% < 48%
3. **AAP2-01** — QG ERROR (partially documentable; does not override AAP2-02)

**Next steps:** Fix tasks for Sonar `new_violations` + aggregate coverage; optional T20 when Docker available; re-verify (max 3 iterations).

**Commit:** validation.md **not committed** (verifier read-only policy; user did not request commit).

---

## Execução: fix-cycle-1 — 2026-07-29 — 471a3d6..1156899

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** `feat/adequacao-analise-projeto` @ `1156899`  
**Diff range:** `471a3d6..1156899` (4 commits: RubricasFixas null-safe, auth log redaction, FE coverage expansion, validation docs)

### Fix-cycle delta (vs prior verify @ 471a3d6)

| Métrica | Antes | Depois | Δ |
| ------- | ----- | ------ | - |
| Sonar `coverage` (agregado) | 44.7% | **46.0%** | +1.3pp — ainda < 48% |
| Sonar `new_violations` | 66 | **67** | +1 — regressão |
| Sonar `new_coverage` | 60.8% | **61.4%** | +0.6pp |
| Vitest casos | 16 | **39** | +23 |
| `mvn test` | 411 | **415** | +4 |
| JaCoCo global | 76.8% | **76.7%** | ~flat |
| Code smells | 189 | **190** | +1 (≤230) |

### Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| Fix-cycle-1 | ✅ Done | 3 code commits + 1 docs commit in range |
| T20 | ⏭️ N/A | Unchanged — Testcontainers unavailable |

---

### Spec-Anchored Acceptance Criteria (re-check focus: AAP2-01, AAP2-02, AAP2-10)

| ID | Criterion (WHEN → THEN) | Spec-defined outcome | `file:line` + assertion | Result |
| -- | ----------------------- | -------------------- | ----------------------- | ------ |
| AAP2-01 | `./diversos/scripts/sonar-analyze.sh` completes | QG **OK** OR ≤2 exceptions in validation | Sonar API `@2026-07-29 post-scan`: `project_status.status` = **ERROR** (`new_coverage` 61.4, `new_violations` 67); `./diversos/scripts/sonar-analyze.sh` exit **0** | ❌ GAP — QG not OK; 2 exceptions documented below (does not waive AAP2-02) |
| AAP2-02 | Sonar `new_violations` | **0** | Sonar API QG condition `new_violations` = **67** | ❌ GAP — regressão +1 vs ciclo anterior (66) |
| AAP2-03 | Sonar `new_coverage` | **≥ 80%** OR single documented exception | Sonar API `new_coverage` = **61.4%** | ⚠️ Exception — see QG exceptions |
| AAP2-04 | Sonar `code_smells` | **≤ 230** | Sonar API `code_smells` = **190** | ✅ PASS |
| AAP2-05 | CR+MAJOR top-20 export `sinceLeakPeriod=true` | **0** issues in top files | Sonar API issues search CR+MAJOR `sinceLeakPeriod=true`: top-file filter unchanged from prior PASS | ✅ PASS |
| AAP2-06 | `sonar-analyze.sh` runs | **`npm run test:coverage`** before Docker scanner | `diversos/scripts/sonar-analyze.sh:35-36` — `(cd frontend && npm run test:coverage)` | ✅ PASS |
| AAP2-07 | `check-jacoco-thresholds.sh` | global **≥ 75%**; floors R1 | Verifier run: global **76.7%**, organograma 66.6%, security 74.7%, importacao 76.5% — exit 0 | ✅ PASS |
| AAP2-08 | `npm run test:coverage` | **≥ 15** Vitest cases passing | Verifier run: **39 passed** (14 files) | ✅ PASS |
| AAP2-09 | FE pages Folha/Organograma/Login | **≥ 1 test each** | `Login.test.tsx:28` — `getByRole('heading', { name: 'Sistema de Folha' })`; `FolhaPagamento.test.tsx:34` — `getByRole('heading', { name: 'Folha de Pagamento' })`; `Organograma.test.tsx:31` — `getByRole('heading', { name: /Organograma/i })` | ✅ PASS |
| AAP2-10 | Sonar aggregate coverage | **≥ 48%** | Sonar API `coverage` = **46.0%** (post fresh scan) | ❌ GAP — +1.3pp vs 44.7%, faltam **2.0pp** |
| AAP2-11 | GlobalExceptionHandler validation | test for `MethodArgumentNotValidException` | `GlobalExceptionHandlerTest.java:79-84` — `assertEquals(HttpStatus.BAD_REQUEST, …)` | ✅ PASS |
| AAP2-12 | FolhaTotalizacaoService tx self-invocation | refactor; test green | `FolhaTotalizacaoService.java:51-56`; gate 7 tests 0 failures | ✅ PASS |
| AAP2-13 | OrganogramaAcessoService tx self-invocation | same pattern; test green | `OrganogramaAcessoService.java:77`; gate 8 tests 0 failures | ✅ PASS |
| AAP2-14 | `application.yml` ddl-auto | **`validate`** default; **`update`** only `dev` | `application.yml:10`; `application-dev.yml:4` | ✅ PASS |
| AAP2-15 | JWT blank/default secret | fail startup non-dev/test | `JwtSecretStartupValidatorTest.java:26-27,46-47,72-73` | ✅ PASS |
| AAP2-16 | Login missing user timing-safe | dummy hash path | `AuthenticationServiceTest.java:78` — `verify(matches…DUMMY_BCRYPT_HASH)` | ✅ PASS |
| AAP2-17 | JWT filter log redaction | no full Authorization in log | `JwtAuthenticationFilterTest.java:73-76` | ✅ PASS |
| AAP2-18 | `mvn test` | **0** failures; count **≥ 359** | Verifier run: **415** tests, 0 failures, BUILD SUCCESS | ✅ PASS |
| AAP2-19 | Sonar bugs/vulns regression | bugs **0**; vulns CR+MAJOR **0** | Sonar API: `bugs`=0, `vulnerabilities`=0 | ✅ PASS |
| AAP2-20 | CONCERNS sync | mark tx/ddl/JWT/timing resolved | `_docs/specs/CONCERNS.md:25,64,164-166,172` | ✅ PASS |
| AAP2-21 | ArchUnit AD-010 | zero violations | `ModularArchitectureTest` — 18 tests, 0 failures | ✅ PASS |
| AAP2-22 | ADP integration (P3 optional) | persist ≥1 row OR N/A | T20 not implemented | ⏭️ N/A |

**Spec-anchored summary:** 17/21 executable ACs PASS · 3 GAP · 1 exception (AAP2-03) · 1 N/A (AAP2-22)

#### QG exceptions (AAP2-01 / AAP2-03 — ≤2 allowed)

1. **`new_coverage` = 61.4% (< 80%)** — Incremental leak-period coverage improved marginally (+0.6pp) after FE expansion (39 Vitest); still below gate. Plan: additional FE page tests or Sonar baseline reset post-merge on `main`.
2. **`new_violations` = 67** — Historical leak-period debt vs baseline 2026-07-27; **not waived** (AAP2-02 remains FAIL). Fix-cycle added FE tests (+23) but did not reduce violations; +1 regression vs prior verify.

---

### Gate Check

| Gate | Command | Exit | Evidence |
| ---- | ------- | ---- | -------- |
| Full BE | `cd backend && mvn test` | 0 | **415** tests, 0 failures (+4 vs prior) |
| JaCoCo | `bash diversos/scripts/check-jacoco-thresholds.sh` | 0 | global 76.7% ≥ 75% |
| FE Coverage | `cd frontend && npm run test:coverage` | 0 | **39** tests passed; lcov generated (statements 37%) |
| Sonar | `./diversos/scripts/sonar-analyze.sh` | 0 | Analysis uploaded @ 2026-07-29; QG status ERROR |
| ArchUnit | `cd backend && mvn test -Dtest=ModularArchitectureTest` | 0 | 18 tests, 0 failures |

---

### Discrimination Sensor

**Skipped (optional)** — scratch mutation blocked in verifier environment. Fix-cycle adds discriminating assertion: `RubricasFixas.test.tsx:45-50` — `getByRole('cell', { name: '100%' })` targets null `porcentagem` → 100% default (`index.tsx:55-57`). Prior cycle: 3/3 auth/JWT mutations killed.

---

### Summary

**Overall:** ❌ Not Ready

**What improved (fix-cycle-1):** FE Vitest 16→39; aggregate Sonar coverage 44.7→46.0%; auth log redaction tests (`RefreshTokenServiceTest.java:135-152`); RubricasFixas null-safe percent display.

**Blockers (ranked):**
1. **AAP2-02** — `new_violations` = **67** (requires baseline reconciliation or incremental burn-down; regressed +1)
2. **AAP2-10** — aggregate Sonar coverage **46.0%** < **48%** (gap 2.0pp; fix-cycle +1.3pp insufficient)
3. **AAP2-01** — QG ERROR (≤2 exceptions documented; does not override AAP2-02)

**Next steps:** fix-cycle-2 targeting remaining 2pp aggregate coverage + `new_violations` burn-down; re-verify (iteration 2/3).

**Commit:** validation.md **not committed** (verifier read-only policy; user did not request commit).

---

## Execução: fix-cycle-2 — 2026-07-29 — 1156899..f45f63b

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** `feat/adequacao-analise-projeto` @ `f45f63b`  
**Diff range:** `1156899..f45f63b` (1 commit: backend unit tests for aggregate Sonar coverage)

### Fix-cycle delta (vs prior verify @ 1156899 / fix-cycle-1)

| Métrica | Antes | Depois | Δ |
| ------- | ----- | ------ | - |
| Sonar `coverage` (agregado) | 46.0% | **48.0%** | +2.0pp — **AAP2-10 fechado** |
| Sonar `new_violations` | 67 | **67** | 0 — flat |
| Sonar `new_coverage` | 61.4% | **62.0%** | +0.6pp |
| Vitest casos | 39 | **39** | 0 |
| `mvn test` (suite commitada) | 415 | **464** | +49 |
| JaCoCo global | 76.7% | **81.6%** | +4.9pp |
| Code smells | 190 | **190** | 0 (≤230) |

### Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| Fix-cycle-2 | ✅ Done | 1 commit — Cargo/CentroCusto/LinhaNegocio + adapter/service test expansion |
| T20 | ⏭️ N/A | Unchanged — Testcontainers unavailable; stale `ImportacaoFolhaAdpIntegrationTest` surefire artifact excluded from count |

---

### Spec-Anchored Acceptance Criteria (re-check focus: AAP2-01, AAP2-02, AAP2-10)

| ID | Criterion (WHEN → THEN) | Spec-defined outcome | `file:line` + assertion | Result |
| -- | ----------------------- | -------------------- | ----------------------- | ------ |
| AAP2-01 | `./diversos/scripts/sonar-analyze.sh` completes | QG **OK** OR ≤2 exceptions in validation | Sonar API `@2026-07-29 post-scan`: `project_status.status` = **ERROR** (`new_coverage` 62.0, `new_violations` 67); prior scan exit 0 | ❌ GAP — QG not OK; 2 exceptions documented below (does not waive AAP2-02) |
| AAP2-02 | Sonar `new_violations` | **0** | Sonar API QG condition `new_violations` = **67** | ❌ GAP — unchanged vs fix-cycle-1 |
| AAP2-03 | Sonar `new_coverage` | **≥ 80%** OR single documented exception | Sonar API `new_coverage` = **62.0%** | ⚠️ Exception — see QG exceptions |
| AAP2-04 | Sonar `code_smells` | **≤ 230** | Sonar API `code_smells` = **190** | ✅ PASS |
| AAP2-05 | CR+MAJOR top-20 export `sinceLeakPeriod=true` | **0** issues in top files | Sonar API issues search CR+MAJOR `sinceLeakPeriod=true`: **total 0** | ✅ PASS |
| AAP2-06 | `sonar-analyze.sh` runs | **`npm run test:coverage`** before Docker scanner | `diversos/scripts/sonar-analyze.sh:35-36` | ✅ PASS |
| AAP2-07 | `check-jacoco-thresholds.sh` | global **≥ 75%**; floors R1 | Verifier run: global **81.6%**, organograma 66.6%, security 74.7%, importacao 76.5% — exit 0 | ✅ PASS |
| AAP2-08 | `npm run test:coverage` | **≥ 15** Vitest cases passing | Verifier run: **39 passed** (14 files) | ✅ PASS |
| AAP2-09 | FE pages Folha/Organograma/Login | **≥ 1 test each** | `Login.test.tsx:28`; `FolhaPagamento.test.tsx:34`; `Organograma.test.tsx:31` | ✅ PASS |
| AAP2-10 | Sonar aggregate coverage | **≥ 48%** | Sonar API `coverage` = **48.0%** (post fresh scan) | ✅ PASS — +2.0pp vs 46.0%; threshold met |
| AAP2-11 | GlobalExceptionHandler validation | test for `MethodArgumentNotValidException` | `GlobalExceptionHandlerTest.java:79-84` | ✅ PASS |
| AAP2-12 | FolhaTotalizacaoService tx self-invocation | refactor; test green | `FolhaTotalizacaoService.java:51-56`; gate 7 tests 0 failures | ✅ PASS |
| AAP2-13 | OrganogramaAcessoService tx self-invocation | same pattern; test green | `OrganogramaAcessoService.java:77`; gate 8 tests 0 failures | ✅ PASS |
| AAP2-14 | `application.yml` ddl-auto | **`validate`** default; **`update`** only `dev` | `application.yml:10`; `application-dev.yml:4` | ✅ PASS |
| AAP2-15 | JWT blank/default secret | fail startup non-dev/test | `JwtSecretStartupValidatorTest.java:26-27,46-47,72-73` | ✅ PASS |
| AAP2-16 | Login missing user timing-safe | dummy hash path | `AuthenticationServiceTest.java:78` | ✅ PASS |
| AAP2-17 | JWT filter log redaction | no full Authorization in log | `JwtAuthenticationFilterTest.java:73-76` | ✅ PASS |
| AAP2-18 | `mvn test` | **0** failures; count **≥ 359** | Verifier run: **464** tests, 0 failures/errors (committed suite); stale `ImportacaoFolhaAdpIntegrationTest` artifact excluded | ✅ PASS |
| AAP2-19 | Sonar bugs/vulns regression | bugs **0**; vulns CR+MAJOR **0** | Sonar API: `bugs`=0, `vulnerabilities`=0 | ✅ PASS |
| AAP2-20 | CONCERNS sync | mark tx/ddl/JWT/timing resolved | `_docs/specs/CONCERNS.md:25,64,164-166,172` | ✅ PASS |
| AAP2-21 | ArchUnit AD-010 | zero violations | `ModularArchitectureTest` — 18 tests, 0 failures | ✅ PASS |
| AAP2-22 | ADP integration (P3 optional) | persist ≥1 row OR N/A | T20 not implemented | ⏭️ N/A |

**Spec-anchored summary:** 18/21 executable ACs PASS · 2 GAP · 1 exception (AAP2-03) · 1 N/A (AAP2-22)

#### QG exceptions (AAP2-01 / AAP2-03 — ≤2 allowed)

1. **`new_coverage` = 62.0% (< 80%)** — Incremental leak-period coverage improved (+0.6pp) after backend test expansion; aggregate now ≥48% (AAP2-10 closed). Plan: additional FE page tests or Sonar baseline reset post-merge on `main`.
2. **`new_violations` = 67** — Historical leak-period debt vs baseline 2026-07-27; **not waived** (AAP2-02 remains FAIL). Fix-cycle-2 added 49 backend tests but did not reduce violations.

---

### Gate Check

| Gate | Command | Exit | Evidence |
| ---- | ------- | ---- | -------- |
| Full BE | `cd backend && mvn test` | 0 | **464** tests, 0 failures (+49 vs fix-cycle-1) |
| JaCoCo | `bash diversos/scripts/check-jacoco-thresholds.sh` | 0 | global **81.6%** ≥ 75% |
| FE Coverage | `cd frontend && npm run test:coverage` | 0 | **39** tests passed; lcov generated (statements 37%) |
| Sonar | Sonar API (post scan @ fix-cycle-2) | — | QG status **ERROR**; aggregate coverage **48.0%** |
| ArchUnit | `cd backend && mvn test -Dtest=ModularArchitectureTest` | 0 | 18 tests, 0 failures |

---

### Discrimination Sensor

**Skipped (optional)** — fix-cycle-2 adds service tests only; prior cycle: 3/3 auth/JWT mutations killed. New coverage evidence: `CargoServiceTest.java:33-139` — CRUD + not-found paths; `CentroCustoServiceTest.java:33-187` — list/create/update/delete + exceptions.

---

### Summary

**Overall:** ❌ Not Ready (partial)

**What improved (fix-cycle-2):** AAP2-10 closed (46.0→**48.0%**); backend +49 tests (Cargo, CentroCusto, LinhaNegocio, Rubrica, Funcionario, Dashboard, TipoBeneficio, FolhaTotalizacaoAdapter); JaCoCo global 76.7→**81.6%**.

**Blockers (ranked):**
1. **AAP2-02** — `new_violations` = **67** (baseline reconciliation or incremental burn-down required)
2. **AAP2-01** — QG **ERROR** (≤2 exceptions documented; does not override AAP2-02)

**Next steps:** fix-cycle-3 targeting `new_violations` burn-down only (aggregate coverage met); re-verify (iteration 3/3).

**Commit:** validation.md **not committed** (verifier read-only policy; user did not request commit).

---

## Execução: fix-cycle-3 — 2026-07-29 — f45f63b..HEAD

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** `feat/adequacao-analise-projeto` @ fix-cycle-3 post-scan  
**Diff range:** `f45f63b..HEAD` (fix-cycle-3 smell burn-down)

### Investigation (67 `new_violations`)

Sonar API `inNewCodePeriod=true&issueStatuses=OPEN` @ PREVIOUS_VERSION baseline 2026-07-27:

| Rule | Count | Category |
| ---- | ----- | -------- |
| java:S1128 | 28 | Unused/same-package imports |
| java:S8688 | 8 | `LocalDateTime.now()` without Clock/ZoneId |
| java:S1612 | 7 | Lambda → method reference |
| typescript:S1874 | 7 | Deprecated MUI props |
| java:S2629 | 5 | Logging preconditions evaluated eagerly |
| java:S8694 | 4 | Month int literals |
| Other (S135, S1172, S2147, S1133, S6541, S7760, S6759, S3776) | 8 | Touch-only refactors/suppressions |

**Conclusion:** Violations were **new-code-period smells** (not historical baseline debt outside leak period). Incremental touch-only fixes were sufficient; no baseline reset required.

### Fix-cycle delta (vs prior verify @ f45f63b / fix-cycle-2)

| Métrica | Antes | Depois | Δ |
| ------- | ----- | ------ | - |
| Sonar `new_violations` | 67 | **0** | **−67 — AAP2-02 fechado** |
| Sonar `new_code_smells` | 67 | **0** | −67 |
| Sonar `code_smells` (total) | 190 | **121** | −69 |
| Sonar `coverage` (agregado) | 48.0% | **48.0%** | 0 |
| Sonar `new_coverage` | 62.0% | **62.2%** | +0.2pp |
| Vitest casos | 39 | **39** | 0 |
| `mvn test` | 464 | **464** | 0 |
| JaCoCo global | 81.6% | **81.7%** | +0.1pp |

### Spec-Anchored Acceptance Criteria (re-check focus: AAP2-01, AAP2-02)

| ID | Criterion | Spec-defined outcome | Evidence | Result |
| -- | --------- | -------------------- | -------- | ------ |
| AAP2-01 | `./diversos/scripts/sonar-analyze.sh` completes | QG **OK** OR ≤2 exceptions | Sonar API post-scan: QG **ERROR** (`new_coverage` 62.2, `new_violations` **0**); script exit **0** | ⚠️ GAP — 1 exception (AAP2-03); `new_violations` OK |
| AAP2-02 | Sonar `new_violations` | **0** | Sonar API QG condition `new_violations` = **0** | ✅ PASS |
| AAP2-03 | Sonar `new_coverage` | **≥ 80%** OR documented exception | Sonar API `new_coverage` = **62.2%** | ⚠️ Exception — see QG exceptions |
| AAP2-10 | Sonar aggregate coverage | **≥ 48%** | Sonar API `coverage` = **48.0%** | ✅ PASS |

**Spec-anchored summary (blockers):** AAP2-02 ✅ · AAP2-10 ✅ · AAP2-01/AAP2-03 QG exception path (1 of 2 slots)

#### QG exceptions (AAP2-01 / AAP2-03 — ≤2 allowed)

1. **`new_coverage` = 62.2% (< 80%)** — Incremental leak-period coverage; aggregate ≥48% (AAP2-10 closed). Plan: additional FE page tests or Sonar baseline reset post-merge on `main`.

### Gate Check

| Gate | Command | Exit | Evidence |
| ---- | ------- | ---- | -------- |
| Full BE | `cd backend && mvn test` | 0 | **464** tests, 0 failures |
| JaCoCo | `bash diversos/scripts/check-jacoco-thresholds.sh` | 0 | global **81.7%** ≥ 75% |
| FE Coverage | `cd frontend && npm run test:coverage` | 0 | **39** tests passed |
| Sonar | `./diversos/scripts/sonar-analyze.sh` | 0 | QG **ERROR** (`new_coverage` only); `new_violations` **0** |
| ArchUnit | `cd backend && mvn test -Dtest=ModularArchitectureTest` | 0 | 18 tests, 0 failures |

### Summary

**Overall:** ⚠️ Partial Ready — **AAP2-02 closable**; remaining QG ERROR is `new_coverage` only (documented exception).

**Questões abertas:** None for AAP2-02. `new_coverage` gap remains for AAP2-01/AAP2-03 (exception documented, not a blocker for smell burn-down).

**Next steps:** Merge R2; optional R3 FE coverage or post-merge Sonar baseline reset for `new_coverage` gate.

---

## Execução: final verify (closing) — 2026-07-29 — 0e767e3..658be8a

**Verifier:** independent sub-agent (author ≠ verifier)  
**Branch:** `feat/adequacao-analise-projeto` @ `658be8a`  
**Diff range:** `0e767e3..658be8a` (R2 full; 21 commits incl. fix-cycle-3 + docs)

### Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1–T19 | ✅ Done | All marked done in `tasks.md` |
| T20 | ⏭️ N/A | Testcontainers unavailable — documented per edge case |

---

### Spec-Anchored Acceptance Criteria (AAP2-01 … AAP2-22)

| ID | Criterion (WHEN → THEN) | Spec-defined outcome | `file:line` + assertion | Result |
| -- | ----------------------- | -------------------- | ----------------------- | ------ |
| AAP2-01 | `./diversos/scripts/sonar-analyze.sh` completes | QG **OK** OR ≤2 exceptions in validation | Verifier run: script exit **0**; Sonar API post-scan: QG **ERROR** (`new_coverage` 62.2 only; `new_violations` 0) | ✅ PASS — 1 exception documented (slot 1/2; spec edge case: QG fails only on `new_coverage`, aggregate ≥48%) |
| AAP2-02 | Sonar `new_violations` | **0** | Sonar API QG condition `new_violations` = **0** | ✅ PASS |
| AAP2-03 | Sonar `new_coverage` | **≥ 80%** OR single documented exception | Sonar API `new_coverage` = **62.2%** | ✅ PASS — documented exception; plan R3 FE coverage or post-merge baseline reset |
| AAP2-04 | Sonar `code_smells` | **≤ 230** (≥15% vs 270) | Sonar API `code_smells` = **121** (55% reduction) | ✅ PASS |
| AAP2-05 | CR+MAJOR top-20 export `sinceLeakPeriod=true` | **0** issues in top files | Sonar API `inNewCodePeriod=true&severities=CRITICAL,MAJOR&issueStatuses=OPEN`: **total 0** | ✅ PASS |
| AAP2-06 | `sonar-analyze.sh` runs | **`npm run test:coverage`** before Docker scanner | `diversos/scripts/sonar-analyze.sh:35-36` — `(cd frontend && npm run test:coverage)` | ✅ PASS |
| AAP2-07 | `check-jacoco-thresholds.sh` | global **≥ 75%**; floors R1 | Verifier run: global **81.7%**, organograma 66.7%, security 74.7%, importacao 76.5% — exit 0 | ✅ PASS |
| AAP2-08 | `npm run test:coverage` | **≥ 15** Vitest cases passing | Verifier run: **39 passed** (14 files) | ✅ PASS |
| AAP2-09 | FE pages Folha/Organograma/Login | **≥ 1 test each** | `Login.test.tsx:28` — `getByRole('heading', { name: 'Sistema de Folha' })`; `FolhaPagamento.test.tsx:34` — `getByRole('heading', { name: 'Folha de Pagamento' })`; `Organograma.test.tsx:31` — `getByRole('heading', { name: /Organograma/i })` | ✅ PASS |
| AAP2-10 | Sonar aggregate coverage | **≥ 48%** | Sonar API `coverage` = **48.0%** (post fresh scan @ final verify) | ✅ PASS |
| AAP2-11 | GlobalExceptionHandler validation | test for `MethodArgumentNotValidException` | `GlobalExceptionHandlerTest.java:79-84` — `assertEquals(HttpStatus.BAD_REQUEST, …)` + message `"login: must not be blank; senha: size must be between 8 and 64"` | ✅ PASS |
| AAP2-12 | FolhaTotalizacaoService tx self-invocation | refactor; `FolhaTotalizacaoServiceTest` green | `FolhaTotalizacaoService.java:50-56` — private `calcularTotaisPorFuncionarioInterno` (no `@Transactional`); gate 7 tests 0 failures | ✅ PASS |
| AAP2-13 | OrganogramaAcessoService tx self-invocation | same pattern; test green | `OrganogramaAcessoService.java:78` — private `resolverContextoAcesso`; gate 8 tests 0 failures | ✅ PASS |
| AAP2-14 | `application.yml` ddl-auto | **`validate`** default; **`update`** only `dev` | `application.yml:10` — `ddl-auto: validate`; `application-dev.yml:4` — `ddl-auto: update` | ✅ PASS |
| AAP2-15 | JWT blank/default secret | fail startup non-dev/test | `JwtSecretStartupValidatorTest.java:26-27,46-47,72-73` — `assertThrows(IllegalStateException.class, …)` prod/staging/blank | ✅ PASS |
| AAP2-16 | Login missing user timing-safe | `passwordEncoder.matches` vs dummy hash | `AuthenticationService.java:51` — `DUMMY_BCRYPT_HASH`; `AuthenticationServiceTest.java:78` — `verify(passwordEncoder).matches(SENHA, AuthenticationService.DUMMY_BCRYPT_HASH)` | ✅ PASS |
| AAP2-17 | JWT filter log redaction | no full Authorization value in log | `JwtAuthenticationFilterTest.java:73-76` — `noneMatch(…contains("eyJhbGciOiJIUzI1NiJ9"))` + `contains("Bearer esperado")` | ✅ PASS |
| AAP2-18 | `mvn test` | **0** failures; count **≥ 359** | Verifier run: **464** tests, 0 failures/errors/skipped; BUILD SUCCESS | ✅ PASS |
| AAP2-19 | Sonar bugs/vulns regression | bugs OPEN **0**; vulns CR+MAJOR **0** | Sonar API: `bugs`=0, `vulnerabilities`=0; `inNewCodePeriod` CR+MAJOR OPEN **0** | ✅ PASS |
| AAP2-20 | CONCERNS sync | mark tx/ddl/JWT/timing resolved | `_docs/specs/CONCERNS.md:25,64,164-166,172` — Resolved entries for R2 items | ✅ PASS |
| AAP2-21 | ArchUnit AD-010 | zero violations | `ModularArchitectureTest` — 18 tests, 0 failures (gate) | ✅ PASS |
| AAP2-22 | ADP integration (P3 optional) | persist ≥1 row with rollback OR N/A documented | T20 not implemented; Testcontainers unavailable per `tasks.md` | ⏭️ N/A |

**Spec-anchored summary:** 20/21 executable ACs PASS · 0 GAP · 1 documented QG exception (AAP2-03) · 1 N/A (AAP2-22)

#### QG exception (AAP2-01 / AAP2-03 — 1 of ≤2 allowed)

1. **`new_coverage` = 62.2% (< 80%)** — Incremental leak-period coverage below Sonar gate; aggregate **48.0%** (AAP2-10 met). Plan: R3 FE coverage (AD-004) or Sonar baseline reset on `main` post-merge.

---

### Gate Check

| Gate | Command | Exit | Evidence |
| ---- | ------- | ---- | -------- |
| Full BE | `cd backend && mvn test` | 0 | **464** tests, 0 failures/errors/skipped |
| JaCoCo | `bash diversos/scripts/check-jacoco-thresholds.sh` | 0 | global **81.7%** ≥ 75%; domain floors PASS |
| FE Coverage | `cd frontend && npm run test:coverage` | 0 | **39** tests passed; lcov generated (statements 37%) |
| Sonar | `./diversos/scripts/sonar-analyze.sh` | 0 | Analysis uploaded @ 2026-07-29 final verify; QG **ERROR** (`new_coverage` only); `new_violations` **0** |
| ArchUnit | `cd backend && mvn test -Dtest=ModularArchitectureTest` | 0 | 18 tests, 0 failures |

**Test delta vs R1 baseline:** backend **359 → 464** (+105); Vitest **2 → 39** (+37). No silent deletions.

---

### Discrimination Sensor (P0 auth/JWT — 3 mutations, scratch worktree)

| # | Mutation | File:line | Killed? |
| - | -------- | --------- | ------- |
| 1 | Disable blank-secret check `if (jwtSecret.isBlank())` → `if (false && jwtSecret.isBlank())` | `JwtSecretStartupValidator.java:32` | ✅ Killed — `JwtSecretStartupValidatorTest#validateJwtSecret_blankSecret_falhaStartup` FAIL |
| 2 | Missing-user login skips dummy hash (`""` instead of `DUMMY_BCRYPT_HASH`) | `AuthenticationService.java:51` | ✅ Killed — `AuthenticationServiceTest#authenticate_loginInexistente_lancaMensagemGenerica` FAIL (`verify(matches…DUMMY_BCRYPT_HASH)`) |
| 3 | Validation handler returns 500 instead of 400 | `GlobalExceptionHandler.java:126-127` | ✅ Killed — `GlobalExceptionHandlerTest#handleMethodArgumentNotValidException_retorna400` FAIL (`expected 400 BAD_REQUEST but was 500`) |

**Sensor depth:** P0 manual (3/3 killed) — **PASS ✅**

---

### Code Quality (spot-check)

| Principle | Status |
| --------- | ------ |
| Minimum scope / no product features | ✅ |
| Matches repo patterns | ✅ |
| Tests map to ACs (non-shallow auth/JWT) | ✅ |
| Documented guidelines (`TESTING.md`, skills) | ✅ |

---

### Edge Cases

- [x] QG fails on `new_coverage` only → exception path used (AAP2-01/AAP2-03); aggregate ≥48% (AAP2-10 PASS)
- [x] Testcontainers unavailable → AAP2-22 N/A documented
- [x] Tx refactor → existing service tests green (no SPEC_DEVIATION)

---

### Summary

**Overall:** ✅ **Ready — PASS**

**What works:** Sonar `new_violations`=0; smells 121 ≤230; aggregate coverage 48.0%; JaCoCo 81.7%; Vitest 39; CONCERNS P1 closed; auth/JWT hardening with discriminating tests; bugs/vulns 0; ArchUnit green; full gate suite green.

**Open items (non-blocking):** `new_coverage` 62.2% — documented QG exception; optional R3 FE coverage or post-merge baseline reset. AAP2-22 N/A until Docker/Testcontainers available.

**Commit:** validation.md updated by closing verifier (user did not request git commit).
