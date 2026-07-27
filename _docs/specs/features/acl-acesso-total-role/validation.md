# ACL — Role `ACESSO_TOTAL` Validation

**Date**: 2026-07-27  
**Spec**: `_docs/specs/features/acl-acesso-total-role/spec.md`  
**Diff range**: uncommitted feature surface on `main` @ `425883b` —  
`backend/.../OrganogramaAcessoService.java`,  
`backend/.../OrganogramaAcessoServiceTest.java`,  
`backend/.../FolhaPagamentoServiceTest.java` (+ ATOT-03/04 period cases — Fix cycle 1),  
`backend/.../AuthenticationServiceAcessoTest.java` (+ ATOT-10 mapping — Fix cycle 1),  
`backend/.../SecurityConfigTipoBeneficioTest.java` (+ ATOT-06 negative — Fix cycle 1),  
`backend/.../db/migration/V1.15__grant_acesso_total_to_admin.sql`,  
`frontend/src/pages/Usuarios/index.tsx`,  
`frontend/src/contexts/AuthContext.tsx`,  
`_docs/specs/features/acl-acesso-total-role/**`  
**Verifier**: independent sub-agent re-verify after Fix cycle 1 (author ≠ verifier)  
**Fix cycle**: 1 applied (tests only; no production code change)

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | Early-return + OrganogramaAcessoServiceTest |
| T2 | ✅ Done | V1.15 present, idempotent SQL |
| T3 | ✅ Done | `ACESSO_TOTAL` in picker + chip |
| T4 | ✅ Done | AuthContext order: `acessoTotal` before vínculo |
| T5 | ✅ Done | Full gate green; prior AC gaps closed by Fix cycle 1 tests |

---

## Spec-Anchored Acceptance Criteria

ATOT-11 Deferred — N/A (skipped).

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| ATOT-01: `ACESSO_TOTAL` → `acessoTotal=true` sem exigir funcionário/nó | `acessoTotal=true`; vínculo/nó não exigidos; `usuarioPodeAcessarCentroCusto` true | `OrganogramaAcessoServiceTest.java:74` — `assertTrue(contexto.acessoTotal())`; `:75` — `assertFalse(contexto.temFuncionarioVinculado())`; `:76` — `assertFalse(contexto.temNoOrganograma())`; `:82` — `assertTrue(service.usuarioPodeAcessarCentroCusto(USUARIO_ID, 999L))` | ✅ PASS |
| ATOT-02: sem `ACESSO_TOTAL` → regras deny atuais | sem funcionário → deny SEM_FUNCIONARIO; sem nó → SEM_NO; com nó → centros, `acessoTotal=false` | `OrganogramaAcessoServiceTest.java:61` — `assertFalse(contexto.acessoTotal())`; `:63` — `assertEquals(SEM_FUNCIONARIO, …)`; `:110` — `assertFalse(acessoTotal)` + `:112` SEM_NO; `:137` — `assertFalse(acessoTotal)` + `:138` `Set.of(100L)` | ✅ PASS |
| ATOT-03: `ACESSO_TOTAL` sem funcionário → período retorna linhas | `consultarPorPeriodo` não vazia por ACL (early-return shape) | `FolhaPagamentoServiceTest.java:67-80` — stub `contextoAcessoTotalEarlyReturn()` (`acessoTotal=true`, vínculo/nó false); `:79` — `assertEquals(1, result.size())`; `:80` — `assertEquals(10L, result.get(0).id())` | ✅ PASS |
| ATOT-04: sem total / sem vínculo → período lista vazia | deny → `isEmpty()` mesmo com linhas no repo | `FolhaPagamentoServiceTest.java:84-96` — stub `contextoNegado(SEM_FUNCIONARIO)` + repo returns row; `:96` — `assertTrue(result.isEmpty())` | ✅ PASS |
| ATOT-05: cobertura discriminante grant + deny | testes que falhariam se flag deixasse de setar | `OrganogramaAcessoServiceTest.java:68-83` (grant) + `:53-64` / `:86-96` (deny / ADMIN-only); Folha period + sensor A/B killed | ✅ PASS |
| ATOT-06: `hasRole("ADMIN")` intacto; `ACESSO_TOTAL` ≠ ADMIN | mutação tipo-beneficio exige ADMIN; só ACESSO_TOTAL → 403 | `SecurityConfigTipoBeneficioTest.java:50` — `status().isForbidden()` (USER); `:61` — `status().isForbidden()` (`@WithMockUser(roles="ACESSO_TOTAL")`); `:74` — `status().is2xxSuccessful()` (ADMIN); `SecurityConfig.java:43-45` — `hasRole("ADMIN")` only | ✅ PASS |
| ATOT-07: Flyway seed admin recebe `ACESSO_TOTAL` | INSERT for `login='admin'`, idempotent | `V1.15__grant_acesso_total_to_admin.sql:1-5` — `INSERT … SELECT … 'ACESSO_TOTAL' … WHERE u.login = 'admin' ON CONFLICT DO NOTHING` (review-only per design/tasks) | ✅ PASS |
| ATOT-08: só `ADMIN` sem `ACESSO_TOTAL` → `acessoTotal=false` | não setar total só por ADMIN | `OrganogramaAcessoServiceTest.java:92` — `assertFalse(contexto.acessoTotal())`; `:94` — `SEM_FUNCIONARIO` | ✅ PASS |
| ATOT-09: picker inclui `ACESSO_TOTAL` | string na lista de permissões do formulário | `Usuarios/index.tsx:51` — `'ACESSO_TOTAL'` in `permissoesDisponiveis`; `:220` chip `'error'` (AD-004: no Vitest; FE gate = `npm run build`) | ✅ PASS |
| ATOT-10: persist + `/auth/acesso` → `acessoTotal=true` (cliente honra) | API persiste strings; DTO mapeia flag; FE honra ordem | BE unit: `AuthenticationServiceAcessoTest.java:75` — `assertTrue(acesso.isAcessoTotal())`; `:76-77` vínculo/nó false; persist brownfield `UsuarioService.java:88,125` — `setPermissoes(dto.permissoes())`; FE review: `AuthContext.tsx:144` — `if (acessoUsuario.acessoTotal) return true` before vínculo (AD-004). Persist→HTTP integration not required per design. | ✅ PASS |

**Status**: ✅ All MVP ACs covered with evidence

**MVP AC score**: **10/10** matched with evidence (ATOT-11 N/A)

---

## Discrimination Sensor

Scratch only: workspace `.sensor-scratch-atot` (rsync backend, mutate Folha, `mvn test`, discard). Live tree never mutated (`if (contexto.acessoTotal())` intact at `:143`).

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| A | `FolhaPagamentoService.java:143` (scratch) | Flip `if (contexto.acessoTotal())` → `if (!contexto.acessoTotal())` | ✅ Killed — ATOT-03 `:79 expected: <1> but was: <0>`; ATOT-04 `:96 expected: <true> but was: <false>` (exit 1, 2 failures) |
| B | `FolhaPagamentoService.java:143-145` (scratch) | Remove early-return `acessoTotal` block | ✅ Killed — ATOT-03 `:79 expected: <1> but was: <0>` (exit 1, 1 failure) |

**Sensor depth**: lightweight (2 Folha mutants targeting Fix cycle 1 ACs)  
**Result**: 2/2 killed — PASS ✅

---

## Interactive UAT Results

Not performed (Verifier automation pass; user-facing UAT not requested in this verification turn).

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ |
| Surgical changes | ✅ Fix cycle 1 = tests only |
| No scope creep | ✅ |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ✅ |
| Per-layer Coverage Expectation met | ✅ Folha period + auth mapping + security negative |
| Every test maps to a spec requirement | ✅ |
| Documented guidelines followed | ✅ `_docs/specs/TESTING.md` / AD-004 / tasks matrix |

---

## Edge Cases

- [x] `ACESSO_TOTAL` + early-return: `acessoTotal=true`, centros ∅, vínculo factual — ATOT-01 test
- [x] Usuário inexistente / sem funcionário sem permissão → deny — existing tests
- [x] Match exato `ACESSO_TOTAL` (contains) — implied by fixture string; casing edge not separately tested (⚠️ minor, non-blocking)
- [x] JWT antigo — out of automated scope (spec: next `/auth/acesso`)

---

## Gate Check

- **Gate command**: `cd backend && mvn test` + `cd frontend && npm run build`
- **Backend**: Tests run: **129**, Failures: **0**, Errors: **0**, Skipped: **0** — exit **0** (BUILD SUCCESS)
- **Frontend**: `npm run build` — exit **0** (tsc + vite)
- **Test count before Fix cycle 1**: 125  
- **Test count after Fix cycle 1**: **129** (+4: Folha period ×2, Auth acessoTotal mapping ×1, Security ACESSO_TOTAL→403 ×1)
- **Delta**: +4 focused tests; no silent deletions observed
- **Skipped tests**: none
- **Failures**: none at gate

---

## Fix Plans

None — all prior gaps closed.

---

## Requirement Traceability Update

| Requirement | Previous Status (pre Fix 1) | New Status |
| ----------- | --------------------------- | ---------- |
| ATOT-01 | ✅ Verified | ✅ Verified |
| ATOT-02 | ✅ Verified | ✅ Verified |
| ATOT-03 | ❌ Needs Fix | ✅ Verified |
| ATOT-04 | ❌ Needs Fix | ✅ Verified |
| ATOT-05 | ✅ Verified | ✅ Verified |
| ATOT-06 | ✅ Verified (⚠️ negative incomplete) | ✅ Verified (negative closed) |
| ATOT-07 | ✅ Verified (review) | ✅ Verified (review) |
| ATOT-08 | ✅ Verified | ✅ Verified |
| ATOT-09 | ✅ Verified (AD-004 review + build) | ✅ Verified |
| ATOT-10 | ❌ Needs Fix | ✅ Verified (unit mapping + FE review) |
| ATOT-11 | Deferred | N/A |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 10/10 MVP ACs matched; 0 gaps  
**Sensor**: 2/2 Folha mutants killed  
**Gate**: 129 passed, 0 failed; FE build pass  
**Fix cycle**: 1 applied; gaps ATOT-03/04/10 (+ ATOT-06 negative) closed via tests only

**What works**: Port grant/deny/ADMIN≠total; Folha period happy+deny; `/auth/acesso` DTO mapping; seed SQL; FE picker + AuthContext order; Security ADMIN matchers + ACESSO_TOTAL-alone 403; full suite green; discrimination A/B.

**Issues found**: none

**Next steps**: none for verifier — feature Ready; lessons not recorded (clean PASS).
