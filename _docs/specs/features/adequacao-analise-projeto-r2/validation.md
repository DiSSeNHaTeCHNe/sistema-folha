# Adequação da Análise de Projeto — R2 Validation

## Status atual
- Veredito: FAIL
- Spec: adequacao-analise-projeto-r2
- HEAD: 471a3d6
- Gaps abertos: AAP2-02 (`new_violations`=66 ≠ 0); AAP2-10 (cobertura agregada Sonar 44.7% < 48%); AAP2-01/AAP2-03 (QG ERROR — `new_coverage` 60.8% < 80%, exceções documentadas abaixo); AAP2-22 N/A (T20 Docker/Testcontainers indisponível)

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
