# modular-monolith-fix Validation

**Date**: 2026-07-26
**Spec**: `_docs/specs/features/modular-monolith-fix/spec.md`
**Diff range**: working tree uncommitted modular-monolith-fix
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status  | Notes |
| ---- | ------- | ----- |
| T1   | ✅ Done | `FuncionarioConsultaPort` |
| T2   | ✅ Done | Adapter + tests |
| T3   | ✅ Done | `CadastrosLookupPort` |
| T4   | ✅ Done | Adapter + tests |
| T5   | ✅ Done | `UsuarioLookupPort` |
| T6   | ✅ Done | Adapter + tests |
| T7   | ✅ Done | `BeneficioMensalService` refactor |
| T8   | ✅ Done | `ImportacaoBeneficioMensalService` refactor |
| T9   | ✅ Done | `FolhaPagamentoService` refactor |
| T10  | ✅ Done | `OrganogramaService` refactor + wiring test |
| T11  | ✅ Done | `OrganogramaAcessoService` refactor |
| T12  | ✅ Done | `UsuarioService` refactor + test |
| T13  | ✅ Done | ArchUnit application-layer rules (AD-009 allowlist) |
| T14  | ✅ Done | `AuthenticationServiceAcessoTest` |
| T15  | ✅ Done | Checklist AD-004 messaging |
| T16  | ✅ Done | Lint scoped analysis documented |
| T17  | ✅ Done | `BeneficioMensalControllerWebMvcTest` (partial MODFIX-16) |
| T18  | ✅ Done | Gate handoff in STATE.md |

---

## Spec-Anchored Acceptance Criteria

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| MODFIX-01: compliance script mandatory vs advisory lint + AD-004 | FE greps + `npm run build` mandatory; `npm run lint` advisory with AD-004 message | `diversos/scripts/check-modular-compliance.sh:183-193` — lint section title + AD-004 echo; `:208-228` — exit 0 when only `LINT_FAIL=1` and mandatory_ok=1 | ✅ PASS |
| MODFIX-02: Success Criteria declare modular FE ≠ full ESLint green | Explicit AD-004 statement in fix + parent specs | `modular-monolith-fix/spec.md:175` — Success Criteria text; `modular-monolith/spec.md:165,344,374` — lint advisory amendment | ✅ PASS |
| MODFIX-03: identify/fix only lint introduced by migration | 0 introduced or corrected; pre-existing untouched | `_docs/specs/STATE.md:93` — "0 introduced — 1 pre-existing … unchanged"; tasks T16 done-when satisfied | ✅ PASS (process evidence; no automated lint diff test) |
| MODFIX-04: `npm run build` exit 0 | Build succeeds | `_docs/specs/STATE.md:92` — FE build exit 0; `check-modular-compliance.sh:196-203` — mandatory build gate | ✅ PASS |
| MODFIX-05: ArchUnit rule blocks foreign infra in application layer (same-domain OK) | Rule fails cross-domain `..application..` → foreign `..infrastructure..` | `ModularArchitectureTest.java:128-196` — five per-domain `noClasses().that().resideInAnyPackage("..{domain}..application..")` rules with AD-009 `because` | ✅ PASS |
| MODFIX-06: zero `cadastros.infrastructure` in beneficios.application services | No foreign cadastros infra imports | Static grep `beneficios/**/application/**` → 0 matches; `BeneficioMensalService.java:14-18` imports ports only | ✅ PASS |
| MODFIX-07: zero `cadastros.infrastructure` in folha.application | No foreign cadastros infra | Static grep `folha/**/application/**` → 0 matches; `FolhaPagamentoService.java:13-14` — `CadastrosLookupPort`, `UsuarioLookupPort` | ✅ PASS |
| MODFIX-08: zero `cadastros.infrastructure` in organograma.application | No foreign cadastros infra | Static grep `organograma/**/application/**` → 0 matches; `OrganogramaService.java:7-8` — port imports | ✅ PASS |
| MODFIX-09: zero `cadastros.infrastructure` in auth.application (UsuarioService) | No foreign cadastros infra in UsuarioService | Static grep `auth/**/application/**` for `cadastros.infrastructure` → 0; `UsuarioService.java:8` — `FuncionarioConsultaPort` | ✅ PASS |
| MODFIX-10: consumers use `cadastros.port` / `auth.port`; adapters in same domain | Port contracts + `@Service` adapters | `FuncionarioConsultaPort.java:7-14`; `FuncionarioConsultaAdapter.java:13`; consumers per grep (BeneficioMensal, Folha, Organograma, Usuario) | ✅ PASS |
| MODFIX-11: all ArchUnit rules pass 0 violations | `ModularArchitectureTest` green | Gate run: 16 ArchUnit rule evaluations, 0 failures (`mvn test -Dtest=ModularArchitectureTest`, 2026-07-26) | ✅ PASS |
| MODFIX-12: SEM_FUNCIONARIO → DTO fields false/false/false, empty centros, motivo SEM_FUNCIONARIO | Exact ACL denial mapping | `AuthenticationServiceAcessoTest.java:75-80` — `assertFalse` ×3, `assertTrue(getCentrosCustoIds().isEmpty())`, `assertEquals(MotivoNegacaoAcesso.SEM_FUNCIONARIO, …)` | ⚠️ Spec-precision gap — uses `obterAcessoUsuario(id)` not literal `obterAcessoUsuarioPorLogin` nor MockMvc; mapping outcome matches spec |
| MODFIX-13: grant parcial → true/true/false, non-empty centrosCustoIds | Partial grant mapping with expected IDs | `AuthenticationServiceAcessoTest.java:105-113` — `assertTrue` ×2, `assertFalse(acessoTotal)`, `assertEquals(Set.of(100L, 200L), getCentrosCustoIds())` | ✅ PASS |
| MODFIX-14: new test passes without `@SpringBootTest` full context | Unit/Mockito or light MockMvc slice only | `AuthenticationServiceAcessoTest.java:29` — `@ExtendWith(MockitoExtension.class)`; no `@SpringBootTest` in file | ✅ PASS |
| MODFIX-15: MockMvc BeneficioMensal delegates to service, no repo, compatible status | HTTP 200 + verify service call | `BeneficioMensalControllerWebMvcTest.java:53` — `status().isOk()`; `:55-56` — `verify(beneficioMensalService).listarPorCompetenciaParaUsuario(...)`; no `@MockBean` repository | ✅ PASS |
| MODFIX-16: AuthController login/acesso delegation verifiable | MockMvc or equivalent verifies `AuthenticationService` delegation | — | ⏭️ Deferred (P2) — no `AuthController*WebMvcTest`; allowed per spec Success Criteria `:176` and design §8 |

**Status**: ✅ All P1 ACs covered — 1 spec-precision gap (MODFIX-12 entry point); MODFIX-16 explicitly deferred

---

## Discrimination Sensor

Mutations applied in isolated `/tmp` backend copy (production tree untouched); discarded after each run.

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| 1 | `AuthenticationService.java:141` | Flipped `.temFuncionarioVinculado(contexto.temFuncionarioVinculado())` → negated | ✅ Killed — `AuthenticationServiceAcessoTest`: 2 failures |
| 2 | `BeneficioMensalService.java:1` | Injected `import …cadastros.infrastructure.FuncionarioRepository` | ✅ Killed — `ModularArchitectureTest` build/test failure (exit ≠ 0) |
| 3 | `BeneficioMensalControllerWebMvcTest.java:53` | Changed `status().isOk()` → `status().isNotFound()` | ✅ Killed — 1 failure |

**Sensor depth**: lightweight (3 behavior-level faults)
**Result**: 3/3 killed — ✅ PASS

---

## Interactive UAT Results

Not performed — backend/infrastructure fix-only; automated gates sufficient per spec scope.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ Ports/adapters scoped to known offenders |
| Surgical changes | ✅ No product semantics changed |
| No scope creep | ✅ AD-009 allowlist documented for dashboard/importacao |
| Matches patterns | ✅ Mirrors `BeneficioConsultaPort` / existing ArchUnit style |
| Spec-anchored outcome check | ⚠️ MODFIX-12 entry-point literal vs tasks T14 |
| Per-layer coverage met | ✅ Domain via unit tests; ArchUnit for isolation; MockMvc P2 partial |
| Tests map to requirements | ✅ Matrix in tasks.md satisfied |
| Guidelines followed | ✅ `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4 (no `@SpringBootTest` for ACL unit) |

---

## Edge Cases

- [x] Port returns empty → consumers propagate domain exceptions (`UsuarioServiceTest.java:assertThrows(FuncionarioNotFoundException)`)
- [x] ArchUnit same-domain exception — `cadastros.application` → `cadastros.infrastructure` allowed; cross-domain blocked
- [x] Pre-existing lint in untouched files — not required scope (STATE.md:93)
- [x] AD-009 allowlist `dashboard.application` + `importacao.application` — documented debt, not FAIL
- [x] Spring wiring — full `mvn test` green (86 tests per STATE handoff)

---

## Gate Check

- **Gate command**: `cd backend && mvn test -Dtest=ModularArchitectureTest,AuthenticationServiceAcessoTest,BeneficioMensalControllerWebMvcTest`
- **Result**: 19 passed, 0 failed, 0 skipped (BUILD SUCCESS, 2026-07-26)
- **Full suite**: `mvn test` — 86 passed per `_docs/specs/STATE.md:91` (verified same session, BUILD SUCCESS)
- **Compliance script**: `./diversos/scripts/check-modular-compliance.sh` exit 0 per STATE.md:92 (T18 handoff)
- **Test count before feature (parent)**: 62 (modular-monolith validation)
- **Test count after feature**: 86
- **Delta**: +24 tests (adapters, ACL, ArchUnit rules, MockMvc, consumer wiring)
- **Skipped tests**: none
- **Failures**: none

**Static grep (MODFIX-06–09)**:

```text
rg 'cadastros\.infrastructure' backend/src/main/java/**/application/**
→ matches only cadastros.application.*, dashboard.application.*, importacao.application.* (AD-009 allowlist)
→ zero in beneficios/folha/organograma/auth non-cadastros application packages
```

---

## Fix Plans

None required for PASS. Optional follow-ups (non-blocking):

### Follow-up 1: MODFIX-16 AuthController MockMvc (P2)

- **What**: Add `@WebMvcTest(AuthController.class)` verifying login/acesso delegates to `AuthenticationService`
- **Priority**: Minor (explicitly deferrable)

### Follow-up 2: MODFIX-12 literal entry point

- **What**: Add SEM_FUNCIONARIO case via `obterAcessoUsuarioPorLogin` or MockMvc `GET /auth/acesso` for spec literal alignment
- **Priority**: Minor (mapping already proven via `obterAcessoUsuario`)

### Follow-up 3: AD-009 ports for dashboard/importacao

- **What**: Extend ports to remove allowlist debt
- **Priority**: Major (architectural debt — separate feature)

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| MODFIX-01 | In Tasks | ✅ Verified |
| MODFIX-02 | In Tasks | ✅ Verified |
| MODFIX-03 | In Tasks | ✅ Verified |
| MODFIX-04 | In Tasks | ✅ Verified |
| MODFIX-05 | In Tasks | ✅ Verified |
| MODFIX-06 | In Tasks | ✅ Verified |
| MODFIX-07 | In Tasks | ✅ Verified |
| MODFIX-08 | In Tasks | ✅ Verified |
| MODFIX-09 | In Tasks | ✅ Verified |
| MODFIX-10 | In Tasks | ✅ Verified |
| MODFIX-11 | In Tasks | ✅ Verified |
| MODFIX-12 | In Tasks | ⚠️ Verified (spec-precision gap on entry point) |
| MODFIX-13 | In Tasks | ✅ Verified |
| MODFIX-14 | In Tasks | ✅ Verified |
| MODFIX-15 | In Tasks | ✅ Verified |
| MODFIX-16 | In Tasks | ⏭️ Deferred (P2) |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 15/16 ACs matched spec outcome; 1 spec-precision gap (MODFIX-12); MODFIX-16 deferred per spec
**Sensor**: 3/3 mutations killed
**Gate**: 19/19 targeted + 86/86 full suite pass

**What works**:

- Application-layer foreign-infra isolation enforced (ArchUnit + port refactors)
- Five known offender services refactored to ports
- ACL DTO mapping proven with discriminating unit tests
- FE lint contract aligned (AD-004 advisory + parent spec amendment)
- P2 MockMvc smoke for BeneficioMensal delegation

**Issues found**: None blocking. MODFIX-16 Auth MockMvc and MODFIX-12 literal HTTP entry point are optional hardening.

**Next steps**: Re-Verifier parent `modular-monolith`; optional P2 MockMvc for AuthController; roadmap AD-009 dashboard/importacao ports.
