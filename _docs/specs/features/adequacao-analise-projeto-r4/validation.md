# Adequação da Análise de Projeto — R4 Validation

## Status atual
- Veredito: **PASS com ressalva** (merge aprovado 2026-07-30 — QG Sonar OK; meta interna AAP4-02 85% / AAP4-18 70% documentadas como ressalva do leak period R3)
- Spec: adequacao-analise-projeto-r4
- Branch/HEAD merge: `feat/adequacao-analise-projeto` → `main` (squash)
- Sonar QG: **OK** — `new_coverage` **80.0%**, `new_branch_coverage` **62.6%**, `new_violations` **0**, aggregate **59.8%**
- Gates: BE **475** · FE **189** · JaCoCo **81.9%** · ArchUnit **0** · Playwright **1/1** · `gate-r4-local.sh` **0**
- ADP: **N/A** (Testcontainers socket 400)
- Ressalvas aceitas no merge: AAP4-02, AAP4-18; sensor M2 residual; B9 não executado
- Gaps pós-merge (follow-up): B9 reset opcional; cobrir ~173 unidades no leak period se meta 85% voltar a ser P1

---

## Execução: adequacao-analise-projeto-r4 — 2026-07-29 — Batch 2 (T7–T15)

**Branch:** `feat/adequacao-analise-projeto`  
**Range:** T7 Playwright → T15 Sonar reset skip

### Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T7 | ✅ Done | `@playwright/test`, `playwright.config.ts`, `test:e2e` |
| T8 | ✅ Done | `e2e/login.spec.ts` — heading + submit + `page.route` mock |
| T9 | ✅ Done | Sonar checkpoint — ver `sonar-checkpoint.md` |
| T10 | ✅ Done | ADP N/A — ver `adp-evidence.md` |
| T11 | ✅ Done | +2 cases: `api.test.ts` 500 refresh; `Login.test.tsx` loading |
| T12 | ✅ Done | Full BE/FE/JaCoCo/Arch gates green |
| T13 | ✅ Done | This file + CONCERNS sync |
| T14 | ✅ Done | `gate-r4-local.sh` |
| T15 | ⏭️ Skip | `new_coverage` ≥80% QG OK; B9 reset not approved |

---

### Gate Check

| Gate | Command | Exit | Evidence |
| ---- | ------- | ---- | -------- |
| Full BE | `cd backend && mvn test` | 0 | 475 run, 0 fail, 1 skip (ADP `@EnabledIf`) |
| JaCoCo | `bash diversos/scripts/check-jacoco-thresholds.sh` | 0 | global **81.9%** ≥ 75%; importacao 76.5% |
| FE unit | `cd frontend && npm test` | 0 | **186** tests, 27 files |
| ArchUnit | `cd backend && mvn test -Dtest=ModularArchitectureTest` | 0 | AD-010 zero violations |
| Playwright | `cd frontend && npm run test:e2e` | 0 | 1 passed (login smoke) |
| Sonar analyze | `./diversos/scripts/sonar-analyze.sh` | 0* | T9 checkpoint SUCCESS; T13 re-run scanner warning (metrics unchanged) |
| Sonar QG | API `qualitygates/project_status` | OK | `new_coverage` 80.0, `new_violations` 0 |
| Gate local | `./diversos/scripts/gate-r4-local.sh` | 0 | dry-run without `--sonar` |

\* T13 final scan reported surefire parser warning; dashboard metrics unchanged from T9 checkpoint.

---

### Sonar baseline (T9 checkpoint + T13 final API)

| Metric | Value | Target |
| ------ | ----- | ------ |
| Quality Gate | OK | OK |
| `new_coverage` | 80.0% | ≥85% internal / ≥80% QG |
| `new_branch_coverage` | 62.6% | ≥70% informacional |
| `new_violations` | 0 | 0 |
| `coverage` (aggregate) | 59.8% | ≥59% (no regression) |

**Reprodução:** `./diversos/scripts/sonar-analyze.sh` (Sonar UP + `.sonar.env`)

---

### Playwright login smoke (AAP4-06…08)

**Status:** PASS  
**Spec:** `frontend/e2e/login.spec.ts`  
**Command:** `cd frontend && npm run test:e2e`  
**Prereq browsers:** `npx playwright install chromium`

---

### ADP integration live (AAP4-12…13)

**Status:** N/A  
**Reason:** `docker info` OK; Testcontainers `isDockerAvailable()` false → 1 skipped.  
**Evidence:** `_docs/specs/features/adequacao-analise-projeto-r4/adp-evidence.md`

---

### T15 — Sonar baseline reset (B9 fallback)

**Status:** SKIP  
**Reason:** QG OK @ 80%; meta interna 85% documented; user has not approved B9 reset.

---

### CONCERNS sync

| Item | R4 status |
| ---- | --------- |
| FE E2E Playwright | **Mitigated** — `test:e2e` + login smoke (mock route) |
| ADP Testcontainers | **Mitigated** — integration test exists; live N/A this run |
| TESTING harness | **Synced** — `_docs/specs/TESTING.md`; gate script T14 |
| Sonar 85% internal meta | **Open** — 80.0% @ leak period; B9 reset not executed |

---

_Append-only validation log — R4 batch 2 @ 2026-07-29_

---

## Execução: adequacao-analise-projeto-r4 — Verifier — 2026-07-30 — 088a438..5cbaba5

**Branch:** `feat/adequacao-analise-projeto` @ `5cbaba5`  
**Verifier:** independent sub-agent (author ≠ verifier)  
**Verdict:** **FAIL ❌**

### Gate Check (Verifier re-run)

| Gate | Command | Exit | Evidence |
| ---- | ------- | ---- | -------- |
| Full BE | `cd backend && mvn test` | 0 | 475 run, 0 fail, 1 skip (ADP `@EnabledIf`) |
| JaCoCo | `bash diversos/scripts/check-jacoco-thresholds.sh` | 0 | global **81.9%** ≥ 75% |
| FE unit | `cd frontend && npm test` | 0 | **186** tests, 27 files |
| ArchUnit | `cd backend && mvn test -Dtest=ModularArchitectureTest` | 0 | AD-010 zero violations |
| Playwright | `cd frontend && npm run test:e2e` | 0 | 1 passed (`e2e/login.spec.ts`) |
| Gate local | `./diversos/scripts/gate-r4-local.sh` | 0 | mvn + jacoco + npm |
| Gate `--docker` | `./diversos/scripts/gate-r4-local.sh --docker` | 0 | ADP skipped (Testcontainers 400); script continued |
| Sonar QG | MCP `get_project_quality_gate_status` (main) | OK | `new_coverage` 80.0, `new_violations` 0 |

### Spec-Anchored Acceptance Criteria

| ID | Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| -- | --------- | -------------------- | ----------------------- | ------ |
| AAP4-01 | Sonar QG OK | status **OK**, zero ERROR | Sonar API @ Verifier run — QG **OK**, `new_coverage` threshold 80 OK | ✅ PASS |
| AAP4-02 | `new_coverage` ≥85% | **≥ 85.0%** | Sonar API — actual **80.0%** (`sonar-checkpoint.md:12`) | ❌ **GAP** |
| AAP4-03 | `new_violations` 0 | **0** | Sonar API — **0** | ✅ PASS |
| AAP4-04 | aggregate ≥59% | **≥ 59%** | Sonar API — **59.8%** | ✅ PASS |
| AAP4-05 | validation baseline | commit + metrics + repro cmd | `validation.md:1-14` (Status atual); `./diversos/scripts/sonar-analyze.sh` | ✅ PASS |
| AAP4-06 | Playwright deps + script | `@playwright/test` + `test:e2e` | `frontend/package.json:13,38` — `"test:e2e": "playwright test"`, `@playwright/test` | ✅ PASS |
| AAP4-07 | E2E login smoke | heading visible + submit + `page.route` mock login | `frontend/e2e/login.spec.ts:42` — `getByRole('heading', { name: 'Sistema de Folha' })`; `:19-25` route mock; `:46` submit | ✅ PASS |
| AAP4-08 | Playwright N/A path | document if setup fails | Playwright **PASS** (not N/A) — browsers installed | ✅ PASS (N/A not triggered) |
| AAP4-09 | TESTING.md sync | Vitest 184+, MSW, JaCoCo/Sonar, Testcontainers, gate cmds | `_docs/specs/TESTING.md:12-14,72-87` — **stale** `:15` "Not yet installed", `:65-68` "Planned" vs Playwright live | ⚠️ **GAP** (partial stale) |
| AAP4-10 | backend AGENTS §4 | Testcontainers, ADP IT, ≥474 | `backend/AGENTS.md:116-125` — Testcontainers + `ImportacaoFolhaAdpIntegrationTest` + **≥ 474** | ✅ PASS |
| AAP4-11 | frontend AGENTS brownfield | MSW isolated, Playwright R4, AD-004 unchanged | `frontend/AGENTS.md:56-62` — brownfield note explicit | ✅ PASS |
| AAP4-12 | ADP live Docker UP | exit 0 + ≥1 line pre-rollback | `ImportacaoFolhaAdpIntegrationTest.java:82-91` — `assertTrue(linhasPersistidas >= 1)`; Verifier: **Skipped 1** (Testcontainers 400) | ⏭️ **N/A** |
| AAP4-13 | ADP Docker DOWN path | suite green + N/A documented | `mvn test` 475/0/1 skip; `adp-evidence.md:20-24` | ✅ PASS |
| AAP4-14 | refresh false branch | `validarRefreshToken` false → `RefreshTokenInvalidoException` | `AuthenticationServiceTest.java:170-175` — `thenReturn(false)` + `assertThrows(RefreshTokenInvalidoException.class, …)` | ✅ PASS |
| AAP4-15 | ≥3 auth failure assertions | `response.status === 401` or spec message (not `toBeDefined` alone) | `api.test.ts:127` — `rejects.toMatchObject({ response: { status: 401 } })`; `:391` same; `:76` — `rejects.toThrow('Falha ao renovar token')`; `:176` — `rejects.toThrow('Refresh token expirado')` | ✅ PASS (4 cases) |
| AAP4-16 | API_BASE_URL single source | shared const/env | `frontend/src/lib/apiBaseUrl.ts:4-8`; `api.ts:3,19`; `authHandlers.ts:3,5` | ✅ PASS |
| AAP4-17 | ≤10 new tests R4 | total added **≤ 10** | diff 088a438..HEAD: `@Test` +1, `it(` +3, Playwright `test(` +1 = **5/10** | ✅ PASS |
| AAP4-18 | `new_branch_coverage` ≥70% | **≥ 70%** | Sonar — actual **62.6%** (`sonar-checkpoint.md:13`) | ❌ **GAP** (informacional) |
| AAP4-19 | branch scope limit | leak-period files only | diff: `api.test.ts`, `AuthenticationServiceTest.java`, `Login.test.tsx` only | ✅ PASS |
| AAP4-20 | mvn test ≥474 | 0 failures | Verifier: **475** run, 0 fail, 1 skip | ✅ PASS |
| AAP4-21 | JaCoCo script exit 0 | exit **0** | `check-jacoco-thresholds.sh` exit 0, global 81.9% | ✅ PASS |
| AAP4-22 | Vitest ≥184 | ≥184 pass | Verifier: **186** passed | ✅ PASS |
| AAP4-23 | CONCERNS sync | E2E, ADP, TESTING pointer | `_docs/specs/CONCERNS.md:148-154,179` | ✅ PASS |
| AAP4-24 | ArchUnit AD-010 | zero violations | `ModularArchitectureTest` — 18 tests, 0 violations | ✅ PASS |
| AAP4-25 | gate-r4-local.sh | mvn + npm + jacoco, exit 0 | `diversos/scripts/gate-r4-local.sh` exit 0 | ✅ PASS |
| AAP4-26 | `--docker` flag | UP runs ADP; DOWN warn+continue | `gate-r4-local.sh:42-56`; Verifier `--docker`: exit 0, ADP skipped, continued | ✅ PASS |

**Spec-anchored summary:** 22/26 matched · 2 GAP (AAP4-02, AAP4-18) · 1 partial GAP (AAP4-09 stale) · 1 N/A (AAP4-12)

### Discrimination Sensor

| # | Mutation | File | Killed? |
| - | -------- | ---- | ------- |
| 1 | Flip `validarRefreshToken` false→true in expired refresh test | `AuthenticationServiceTest.java:170` | ✅ Killed (mvn exit 1) |
| 2 | Replace all `rejects.toMatchObject({ response: { status: 401 } })` → `rejects.toBeDefined()` | `api.test.ts:127,391` | ❌ **Survived** (19/19 pass) |
| 3 | Change default API base URL `8083` → `9999` | `apiBaseUrl.ts:1` | ❌ **Survived** (19/19 pass) |
| 4 | Change E2E heading expectation to wrong text | `e2e/login.spec.ts:42` | ✅ Killed (e2e exit 1) |

**Sensor depth:** lightweight (4 mutations)  
**Result:** 2/4 killed — **FAIL ❌** (MSW/api.test regressions on 401 assertion strength and apiBaseUrl drift not detected)

Mutations applied in scratch/temp copies only; production tree restored (`git diff` clean on mutated paths).

### Ranked Gaps

1. **AAP4-02** — `new_coverage` **80.0%** vs meta **≥85%** — Sonar QG floor OK but P1 AC FAIL — no test evidence can override metric
2. **Sensor M2** — Weakening 401 assertions to `toBeDefined()` survives full `api.test.ts` — strengthen status-401 checks on refresh failure paths (`api.test.ts`)
3. **Sensor M3** — `apiBaseUrl` default drift undetected — add test asserting MSW handler URL matches `getApiBaseUrl()` (`api.test.ts` or dedicated)
4. **AAP4-18** — `new_branch_coverage` **62.6%** vs **≥70%** (informacional; QG OK)
5. **AAP4-09** — `TESTING.md:15,65-68` still says Playwright "Not yet installed" / "Planned" — sync to live state post-T8
6. **AAP4-12** — ADP live N/A (Testcontainers socket 400 despite `docker info` UP) — environment blocker, documented

### Summary

**Overall:** ❌ Not Ready — QG OK and gates green, but P1 meta `new_coverage` ≥85% **not met**; discrimination sensor found weak MSW/apiBaseUrl tests.

**Next steps:** (1) Close AAP4-02 via targeted branch/line tests within remaining budget or user-approved B9 reset; (2) Fix sensor survivors M2/M3; (3) Sync TESTING.md Playwright section.

---

## Execução: adequacao-analise-projeto-r4 — Verifier re-verify-1 — 2026-07-30 — beccc74..c3baabe

**Branch:** `feat/adequacao-analise-projeto` @ `c3baabe`  
**Verifier:** independent sub-agent (author ≠ verifier)  
**Verdict:** **FAIL ❌**

### Gate Check (Verifier re-verify-1)

| Gate | Command | Exit | Evidence |
| ---- | ------- | ---- | -------- |
| Full BE | `cd backend && mvn test` | 0 | 475 run, 0 fail, 1 skip (ADP `@EnabledIf`) |
| JaCoCo | `bash diversos/scripts/check-jacoco-thresholds.sh` | 0 | global **81.9%** ≥ 75% |
| FE unit | `cd frontend && npm test` | 0 | **189** tests, 28 files |
| ArchUnit | `cd backend && mvn test -Dtest=ModularArchitectureTest` | 0 | AD-010 zero violations |
| Playwright | `cd frontend && npm run test:e2e` | 0 | 1 passed (`e2e/login.spec.ts`) |
| Gate local | `./diversos/scripts/gate-r4-local.sh` | 0 | mvn + jacoco + npm |
| Gate `--docker` | `./diversos/scripts/gate-r4-local.sh --docker` | 0 | ADP skipped (Testcontainers 400); script continued |
| Sonar QG | MCP `get_project_quality_gate_status` (main) | OK | `new_coverage` 80.0, `new_violations` 0 |

**Note:** fix-cycle-1 added tests (+3 Vitest) but Sonar rescan not re-baselined — metrics unchanged from T9 checkpoint (`sonar-checkpoint.md`).

### Spec-Anchored Acceptance Criteria

| ID | Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| -- | --------- | -------------------- | ----------------------- | ------ |
| AAP4-01 | Sonar QG OK | status **OK**, zero ERROR | Sonar API @ re-verify-1 — QG **OK**, `new_coverage` threshold 80 OK | ✅ PASS |
| AAP4-02 | `new_coverage` ≥85% | **≥ 85.0%** | Sonar API — actual **80.0%** (`sonar-checkpoint.md:12`) | ❌ **GAP** |
| AAP4-03 | `new_violations` 0 | **0** | Sonar API — **0** | ✅ PASS |
| AAP4-04 | aggregate ≥59% | **≥ 59%** | Sonar API — **59.8%** | ✅ PASS |
| AAP4-05 | validation baseline | commit + metrics + repro cmd | `validation.md:1-14`; `./diversos/scripts/sonar-analyze.sh` | ✅ PASS |
| AAP4-06 | Playwright deps + script | `@playwright/test` + `test:e2e` | `frontend/package.json:13,38` | ✅ PASS |
| AAP4-07 | E2E login smoke | heading + submit + `page.route` mock | `frontend/e2e/login.spec.ts:42,19-25,46` | ✅ PASS |
| AAP4-08 | Playwright N/A path | document if setup fails | Playwright **PASS** (not N/A) | ✅ PASS (N/A not triggered) |
| AAP4-09 | TESTING.md sync | Vitest 184+, MSW, JaCoCo/Sonar, Testcontainers, gate cmds | `_docs/specs/TESTING.md:12-15,65-68,87` — Playwright **live**, no stale "Not yet installed" | ✅ PASS |
| AAP4-10 | backend AGENTS §4 | Testcontainers, ADP IT, ≥474 | `backend/AGENTS.md:116-125` | ✅ PASS |
| AAP4-11 | frontend AGENTS brownfield | MSW isolated, Playwright R4, AD-004 unchanged | `frontend/AGENTS.md:56-62` | ✅ PASS |
| AAP4-12 | ADP live Docker UP | exit 0 + ≥1 line pre-rollback | Verifier: **Skipped 1** (Testcontainers 400); `adp-evidence.md:20-24` | ⏭️ **N/A** |
| AAP4-13 | ADP Docker DOWN path | suite green + N/A documented | `mvn test` 475/0/1 skip; `adp-evidence.md` | ✅ PASS |
| AAP4-14 | refresh false branch | `validarRefreshToken` false → `RefreshTokenInvalidoException` | `AuthenticationServiceTest.java:170-175` | ✅ PASS |
| AAP4-15 | ≥3 auth failure assertions | `response.status === 401` or spec message | `api.test.ts:76,130,182,342,378,412` — 2× status **401** + 4× `toThrow` spec messages | ✅ PASS |
| AAP4-16 | API_BASE_URL single source | shared const/env | `apiBaseUrl.ts:1-8`; `api.ts:3,19`; `authHandlers.ts:3,5` | ✅ PASS |
| AAP4-17 | ≤10 new tests R4 | total added **≤ 10** | diff 088a438..HEAD: `@Test` +1, `it(` +5, `test(` +1 = **7/10** | ✅ PASS |
| AAP4-18 | `new_branch_coverage` ≥70% | **≥ 70%** | Sonar — actual **62.6%** (`sonar-checkpoint.md:13`) | ❌ **GAP** (informacional) |
| AAP4-19 | branch scope limit | leak-period files only | diff: `api.test.ts`, `apiBaseUrl.test.ts`, `AuthenticationServiceTest.java`, `Login.test.tsx` | ✅ PASS |
| AAP4-20 | mvn test ≥474 | 0 failures | Verifier: **475** run, 0 fail, 1 skip | ✅ PASS |
| AAP4-21 | JaCoCo script exit 0 | exit **0** | `check-jacoco-thresholds.sh` exit 0, global 81.9% | ✅ PASS |
| AAP4-22 | Vitest ≥184 | ≥184 pass | Verifier: **189** passed | ✅ PASS |
| AAP4-23 | CONCERNS sync | E2E, ADP, TESTING pointer | `_docs/specs/CONCERNS.md:148-154,179` | ✅ PASS |
| AAP4-24 | ArchUnit AD-010 | zero violations | `ModularArchitectureTest` — 18 tests, 0 violations | ✅ PASS |
| AAP4-25 | gate-r4-local.sh | mvn + npm + jacoco, exit 0 | `diversos/scripts/gate-r4-local.sh` exit 0 | ✅ PASS |
| AAP4-26 | `--docker` flag | UP runs ADP; DOWN warn+continue | `gate-r4-local.sh:42-56`; Verifier `--docker`: exit 0, ADP skipped | ✅ PASS |

**Spec-anchored summary:** 23/26 matched · 2 GAP (AAP4-02, AAP4-18) · 1 N/A (AAP4-12)

### Discrimination Sensor (re-verify-1)

| # | Mutation | File | Killed? |
| - | -------- | ---- | ------- |
| 1 | Flip `validarRefreshToken` false→true in expired refresh test | `AuthenticationServiceTest.java:170` | ✅ Killed (mvn exit 1) |
| 2 | Replace `response?.status` 401 checks → `toBeDefined()` | `api.test.ts:130,412` | ❌ **Survived** (20/20 pass) |
| 3 | Change default API base URL `8083` → `9999` | `apiBaseUrl.ts:1` | ✅ Killed (`apiBaseUrl.test.ts` exit 1) |
| 4 | Change E2E heading expectation to wrong text | `e2e/login.spec.ts:42` | ✅ Killed (e2e exit 1) |

**Sensor depth:** lightweight (4 mutations)  
**Result:** 3/4 killed — **partial fix** (M3 closed via `apiBaseUrl.test.ts`; M2 still survives)

Mutations applied in scratch (`.scratch/verifier-sensor/`); production tree restored after each run.

### Ranked Gaps

1. **AAP4-02** — `new_coverage` **80.0%** vs meta **≥85%** — fix-cycle tests did not move Sonar metric (rescan not re-baselined)
2. **Sensor M2** — Weakening explicit 401 status assertions to `toBeDefined()` still survives full `api.test.ts`
3. **AAP4-18** — `new_branch_coverage` **62.6%** vs **≥70%** (informacional; QG OK)
4. **AAP4-12** — ADP live N/A (Testcontainers socket 400 despite `docker info` UP) — environment blocker, documented

### Summary

**Overall:** ❌ Not Ready — QG OK and all build gates green; fix-cycle-1 closed AAP4-09 and sensor M3, but P1 meta `new_coverage` ≥85% **not met** and sensor M2 still survives.
