# Modular ACL / Security Fix Validation

**Date**: 2026-07-26  
**Spec**: `_docs/specs/features/modular-acl-security-fix/spec.md`  
**Diff range**: uncommitted working tree (no feature commits) — surface under `beneficios.application`, `config/SecurityConfig`, `folha.application`/`folha.api`, matching tests; `frontend/src/services/api.ts` unchanged vs HEAD  
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 Empty-set short-circuit + tests | ✅ Done | Short-circuit before unscoped; MODACL-01–05 tests present |
| T2 Refresh permitAll + security test | ✅ Done | Matcher + `SecurityConfigAuthRefreshTest` |
| T3 Folha DELETE ACL | ✅ Done | `removerSeAutorizado` + controller `Authentication` |
| T4 Gate + FE static | ✅ Done | Full `mvn test` 94/94; ArchUnit 16/16; `api.ts` refresh sem Bearer |

---

## Spec-Anchored Acceptance Criteria

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| MODACL-01 restrito+empty listagem | Lista vazia; unscoped **nunca** chamado | `BeneficioMensalServiceTest.java:83–97` — `assertTrue(result.isEmpty())` + `verify(..., never()).findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(...)` (+ never In) | ✅ PASS |
| MODACL-02 restrito+empty resumo | Resumo vazio; agregação unscoped **nunca** | `BeneficioMensalServiceTest.java:101–113` — `assertTrue(result.isEmpty())` + `verify(..., never()).resumoPorCompetencia(...)` (+ never AndCentroCustoIds) | ✅ PASS |
| MODACL-03 acessoTotal listagem/resumo | Pode ler todos (unscoped) | `BeneficioMensalServiceTest.java:117–135` — `assertEquals(1, result.size())` + `verify(...).findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(...)`; resumo unscoped em `:241–256` via `resumoPorCompetencia(..., emptySet)` (mesmo branch pós short-circuit skip) | ✅ PASS |
| MODACL-04 restrito+centros não vazios | Só lançamentos dos centros (`In`) | `BeneficioMensalServiceTest.java:139–159` — `assertEquals(1, result.size())` + `verify(...InAndAtivoTrue(..., centros))` + `never()` unscoped | ✅ PASS |
| MODACL-05 cobertura deny empty≠unscoped | Fixture restrito+empty distinta de SEM_FUNCIONARIO; lista/resumo vazios + never unscoped | `BeneficioMensalServiceTest.java:83–97` e `:101–113` (comentários MODACL; `contextoRestrito(emptySet)` vs `contextoNegado(SEM_FUNCIONARIO)` em `:69–79`) | ✅ PASS |
| MODACL-06 matcher refresh permitAll | `HttpMethod.POST` `/auth/refresh` `permitAll()`, path sem `/api` duplicado | `SecurityConfig.java:33–34` — `.requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()` ao lado de `/auth/login` | ✅ PASS (inspeção) |
| MODACL-07 anon refresh sem Authorization | Security **não** responde 401 por ausência de auth (2xx ou 4xx de negócio) | `SecurityConfigAuthRefreshTest.java:48–66` — `status().is2xxSuccessful()` + `assertThat(...getStatus()).isNotEqualTo(401)` com body refresh e sem header Authorization | ✅ PASS |
| MODACL-08 security test + não regressão autenticadas | Anon refresh ok; rotas autenticadas / ADMIN não viram permitAll | `SecurityConfigAuthRefreshTest.java:48–66` + `:70–75` (`get("/auth/acesso")` → 401/403); `SecurityConfigTipoBeneficioTest` (gate: 2/2 verdes) | ✅ PASS |
| MODACL-09 FE fetch refresh sem Bearer | Contrato compatível; sem mudança FE obrigatória | `frontend/src/services/api.ts:85–90` — `fetch(..., { headers: { 'Content-Type': 'application/json' } })` sem `Authorization`; `git diff HEAD -- api.ts` vazio | ✅ PASS (estático) |
| MODACL-10 controller passa login | `Authentication` → service (não só `id`) | `FolhaPagamentoController.java:80–81` — `remover(@PathVariable Long id, Authentication authentication)` → `removerSeAutorizado(authentication.getName(), id)` | ✅ PASS (inspeção; matrix: thin controller) |
| MODACL-11 deny soft-delete fora do centro | Não soft-delete; responde não encontrado (false→404) | `FolhaPagamentoServiceTest.java:145–152` — `assertFalse(...)` + `verify(..., never()).softDelete(any())`; mapping HTTP `FolhaPagamentoController.java:81–83` (`false` → `notFound()`) | ✅ PASS |
| MODACL-12 allow soft-delete | Soft-delete + sucesso (true→204) | `FolhaPagamentoServiceTest.java:156–163` — `assertTrue(...)` + `verify(...).softDelete(3L)`; também acessoTotal `:167–173`; mapping `Controller.java:81–82` (`true` → `noContent()`) | ✅ PASS |
| MODACL-13 testes deny vs allow Folha | Cobertura unitária com mock `OrganogramaAcessoPort` | `FolhaPagamentoServiceTest.java:145–173` (deny / allow / acessoTotal) | ✅ PASS |

**Status**: ✅ All 13 ACs covered — 0 gaps; 0 spec-precision gaps flagged as blocking  
**Note**: MODACL-10/11/12 HTTP status codes are evidenced by thin-controller source mapping + service boolean contract (tasks matrix: no Folha DELETE MockMvc). Payload/conjunction on deny paths uses `never().softDelete` / `never()` unscoped, not call-count-only.

---

## Discrimination Sensor

Scratch mutations on live files with restore-after; baseline green before inject.

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| 1 | `BeneficioMensalService.java:46–48` / `:58–60` | Removed both `!acessoTotal && centrosVazios` short-circuits | ✅ Killed — `Failures: 2`; `NeverWantedButInvoked` on `findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue` (`BeneficioMensalServiceTest.java:94`) |
| 2 | `SecurityConfig.java:34` | Removed `.requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()` | ✅ Killed — status `403` vs expected 2xx (`SecurityConfigAuthRefreshTest.java:63`) |
| 3 | `FolhaPagamentoService.java:125` | Removed `.filter(folha -> aplicarFiltroAcesso(...))` from `removerSeAutorizado` | ✅ Killed — `expected: <false> but was: <true>` (`FolhaPagamentoServiceTest.java:151`) |

**Sensor depth**: lightweight (3 behavior-level mutants)  
**Result**: 3/3 killed — PASS ✅  
**Restore**: all three production files byte-identical to pre-mutation backups after each run.

---

## Interactive UAT Results

N/A — backend security/ACL fix; automated evidence sufficient per validate.md.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ |
| Surgical changes | ✅ |
| No scope creep | ✅ (no ArchUnit/ports redesign; FE untouched) |
| Matches patterns | ✅ (espelho `removerSeAutorizado` benefícios) |
| Spec-anchored outcome check | ✅ |
| Per-layer Coverage Expectation met | ✅ (per tasks matrix) |
| Every test maps to a spec requirement | ✅ (new tests tagged/aligned MODACL; pre-existing benefício CRUD retained) |
| Documented guidelines | ✅ `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, skill `spring-security` |

---

## Edge Cases

- [x] `acessoNegado` (SEM_FUNCIONARIO) → lista vazia + never unscoped — `BeneficioMensalServiceTest.java:69–79`
- [x] `centrosCustoIds` null tratado como vazio restrito — `centrosVazios` (`BeneficioMensalService.java:170–173`)
- [x] Refresh inválido = responsabilidade do application service — Security permitAll only; test mocks success path
- [x] DELETE folha id inexistente → false / never softDelete — `FolhaPagamentoServiceTest.java:177–183`
- [x] `acessoTotal=true` no delete → soft-delete — `FolhaPagamentoServiceTest.java:167–173`
- [x] Ports sibling preservados — `ModularArchitectureTest` 16/16 verde

---

## Gate Check

- **Gate command (targeted, verifier minimum)**:  
  `cd backend && mvn test -Dtest=BeneficioMensalServiceTest,SecurityConfigAuthRefreshTest,SecurityConfigTipoBeneficioTest,FolhaPagamentoServiceTest,ModularArchitectureTest`
- **Targeted result**: **44** passed, **0** failed, **0** skipped (16+2+2+8+16)
- **Full suite**: `cd backend && mvn test` → **94** passed, **0** failed, **0** skipped — BUILD SUCCESS
- **Test count after feature**: 94 (author claim reconfirmed)
- **Delta**: +feature tests vs pre-feature (new: restrito+empty×2, refresh security×2, Folha remover ACL×3–4) — no silent deletions observed in touched suites
- **Skipped tests**: none
- **Failures**: none
- **npm build**: not re-run by verifier (author claim; FE `api.ts` statically verified unchanged)

---

## Fix Plans

None — clean PASS.

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| MODACL-01 | Done (author) | ✅ Verified |
| MODACL-02 | Done (author) | ✅ Verified |
| MODACL-03 | Done (author) | ✅ Verified |
| MODACL-04 | Done (author) | ✅ Verified |
| MODACL-05 | Done (author) | ✅ Verified |
| MODACL-06 | Done (author) | ✅ Verified |
| MODACL-07 | Done (author) | ✅ Verified |
| MODACL-08 | Done (author) | ✅ Verified |
| MODACL-09 | Done (author) | ✅ Verified |
| MODACL-10 | Done (author) | ✅ Verified |
| MODACL-11 | Done (author) | ✅ Verified |
| MODACL-12 | Done (author) | ✅ Verified |
| MODACL-13 | Done (author) | ✅ Verified |

---

## Diff Surface (uncommitted)

| Path | Role |
| ---- | ---- |
| `beneficios/application/BeneficioMensalService.java` | Empty-set short-circuit |
| `beneficios/application/BeneficioMensalServiceTest.java` | MODACL-01–05 |
| `config/SecurityConfig.java` | Refresh permitAll |
| `config/SecurityConfigAuthRefreshTest.java` | MODACL-07–08 |
| `folha/application/FolhaPagamentoService.java` | `removerSeAutorizado` ACL |
| `folha/api/FolhaPagamentoController.java` | Pass `Authentication` |
| `folha/application/FolhaPagamentoServiceTest.java` | MODACL-11–13 |
| `frontend/src/services/api.ts` | Unchanged (MODACL-09) |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 13/13 ACs matched spec outcome | 0 spec-precision gaps  
**Sensor**: 3/3 mutations killed  
**Gate**: 44 targeted + 94 full suite passed  

**What works**: Benefício empty-set deny sem leak unscoped; refresh anon permitAll; Folha soft-delete ACL espelhando benefícios; ArchUnit sem regressão.

**Issues found**: none

**Next steps**: optional commit/PR when user requests; no fix→re-verify cycle.
