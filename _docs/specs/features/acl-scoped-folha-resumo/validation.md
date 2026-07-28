# acl-scoped-folha-resumo Validation

**Date**: 2026-07-27  
**Spec**: `_docs/specs/features/acl-scoped-folha-resumo/spec.md`  
**Diff range**: uncommitted working tree (sem commit) — paths: `folha/port/FolhaEvolucaoSnapshot`, `FolhaConsultaAdapter`, `FolhaLinhaAgregacao` (+Test), `ResumoFolhaPagamentoService` (+Test), `ResumoFolhaPagamentoController`, `DashboardService` (+Test), feature docs + `STATE.md`  
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 FolhaEvolucaoSnapshot.competenciaFim | ✅ Done | Record + adapter + callers |
| T2 FolhaLinhaAgregacao | ✅ Done | Helper + 3 unit tests |
| T3 ResumoService ACL RSF-01…05 | ✅ Done | 6 ACL scenarios + mapping regressões |
| T4 Controller Authentication | ✅ Done | 4 endpoints pass `getName()` (matrix: no unit) |
| T5 Dashboard evolução scoped | ✅ Done | RSF-06/07/08 |
| T6 Gate + handoff | ✅ Done | Quick re-run by Verifier; Beneficios smoke |

---

## Spec-Anchored Acceptance Criteria

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| RSF-01 scoped resumo totais from lines; encargos 0 | empregados=distinct; pagamentos=Σ PROVENTO; descontos=Σ DESCONTO; líquido=pag−desc; totalEncargos=0; ≠ snapshot | `ResumoFolhaPagamentoServiceTest.java:114` — `assertEquals(2, dto.totalEmpregados())`; `:115` — pagamentos `8000.00`; `:116` — descontos `700.00`; `:117` — líquido `7300.00`; `:118` — `BigDecimal.ZERO.compareTo(dto.totalEncargos())`; `:120-123` — `assertNotEquals` vs snapshot | ✅ PASS |
| RSF-02 acessoTotal → snapshot (encargos reais) | DTO = snapshot persistido; não chama linhas | `ResumoFolhaPagamentoServiceTest.java:140` — `assertEquals(new BigDecimal("10000.00"), dto.totalEncargos())`; `:139` empregados 100; `:141-143` pagamentos/descontos/líquido snapshot; `:144` — `verify(..., never()).findLinhasAtivasPorCompetencia` | ✅ PASS |
| RSF-03 deny → lista vazia | sem funcionário / centros vazios → `[]` | `ResumoFolhaPagamentoServiceTest.java:156` — `assertTrue(result.isEmpty())`; `:170` — `assertTrue(result.isEmpty())` | ✅ PASS |
| RSF-04 competência sem linhas no escopo → zeros + metadados | empregados=0; valores 0; id/competência/13º preservados | `ResumoFolhaPagamentoServiceTest.java:192-202` — id `7L`, empregados `0`, ZERO pagamentos/descontos/líquido/encargos, `competenciaInicio`/`Fim`, `decimoTerceiro()`, `dataImportacao`, `ativo()`; also `:226-229` competencia endpoint | ✅ PASS |
| RSF-05 testes falhariam se scoped espelhasse snapshot | assertions discrimination vs totais globais | `ResumoFolhaPagamentoServiceTest.java:120-123` — `assertNotEquals` empregados/pagamentos/líquido/encargos vs snapshot; sensor mutant1 killed | ✅ PASS |
| RSF-06 evolução scoped ≠ global e não vazia | pontos com líquido/empregados recalculados do escopo | `DashboardServiceTest.java:157` — `assertEquals(1, stats.evolucaoMensal().size())`; `:158` — valorTotal `4500.00`; `:159` — qty `1`; `:160-162` — `compareTo(evolucaoGlobal.totalLiquido()) != 0` e qty ≠ global | ✅ PASS |
| RSF-07 acessoTotal evolução via snapshot | valor/qty da série snapshot | `DashboardServiceTest.java:118-120` — size `1`, valorTotal `50000.00`, qty `1` (fonte `findEvolucaoUltimos12Meses`) | ✅ PASS |
| RSF-08 acesso negado → stats vazias (evolução vazia) | emptyStats incl. `evolucaoMensal` empty | `DashboardServiceTest.java:71` / `:87` — `assertEmptyStats(stats)`; `:264` — `assertTrue(stats.evolucaoMensal().isEmpty())` | ✅ PASS |
| RSF-09 benefícios regressão sem change produção | suite benefícios verde; sem diff produção | Gate: `BeneficioMensalServiceTest` 16 passed, 0 failed; `git diff` benefícios production = empty | ✅ PASS |

**Status**: ✅ All ACs covered (9/9)

---

## Discrimination Sensor

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| 1 | `ResumoFolhaPagamentoService.java:109` | Scoped `totalEncargos` = `resumo.getTotalEncargos()` em vez de `BigDecimal.ZERO` | ✅ Killed — `listarTodos_scoped_*` expected encargos compare `0` failed |
| 2 | `DashboardService.java:132-134` | Ramo parcial `evolucaoMensal = List.of()` de novo | ✅ Killed — size expected `1` was `0` |
| 3 | `ResumoFolhaPagamentoService.java:toDtoScoped` | Linhas vazias → `toDtoSnapshot` (totais não-zero) | ✅ Killed — empregados expected `0` was `100` |

**Sensor depth**: lightweight (3 behavior-level)  
**Scratch**: backup→mutate→test→restore; MD5 pós-restore idêntico ao pré-sensor  
**Result**: 3/3 killed — PASS ✅

---

## Interactive UAT Results

N/A — backend-only; automated checks sufficient per validate.md.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ |
| Surgical changes | ✅ |
| No scope creep | ✅ (benefícios sem produção) |
| Matches patterns | ✅ (ACL deny + port, AD-008/010) |
| Spec-anchored outcome check | ✅ |
| Per-layer Coverage Expectation | ✅ (domain 1:1 ACs; controller wiring per matrix none) |
| Every test maps to AC / edge / Done-when | ✅ |
| Documented guidelines: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4 | ✅ |

---

## Edge Cases

- [x] ACESSO_TOTAL sem funcionário → snapshot (RSF-02 path)
- [x] Centros vazios → deny / lista vazia (RSF-03)
- [x] Scoped com linhas → totalEncargos 0 (RSF-01)
- [x] Competência 13º → `decimoTerceiro` preservado (RSF-04)
- [x] Id do resumo scoped = id snapshot (RSF-04)

---

## Gate Check

- **Quick**: `cd backend && mvn test -Dtest=FolhaLinhaAgregacaoTest,ResumoFolhaPagamentoServiceTest,DashboardServiceTest`
  - **Result**: 19 passed, 0 failed, 0 skipped (FolhaLinhaAgregacao 3 + Resumo 8 + Dashboard 8)
- **RSF-09 smoke**: `mvn test -Dtest=BeneficioMensalServiceTest` → 16 passed, 0 failed
- **Full**: not re-run by Verifier (author claimed Full green at T6); Quick + Beneficios smoke sufficient for this verdict
- **Test count before feature** (approx): Resumo ~2 mapping tests; Dashboard sem evolução scoped
- **After**: + ACL Resumo scenarios + FolhaLinhaAgregacaoTest + Dashboard evolução scoped/zero — delta positive
- **Failures**: none on clean tree
- **Skipped**: none

---

## Fix Plans

_None — PASS._

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| RSF-01 | Confirmed / Implementing | ✅ Verified |
| RSF-02 | Confirmed / Implementing | ✅ Verified |
| RSF-03 | Confirmed / Implementing | ✅ Verified |
| RSF-04 | Confirmed / Implementing | ✅ Verified |
| RSF-05 | Confirmed / Implementing | ✅ Verified |
| RSF-06 | Confirmed / Implementing | ✅ Verified |
| RSF-07 | Confirmed / Implementing | ✅ Verified |
| RSF-08 | Confirmed / Implementing | ✅ Verified |
| RSF-09 | Confirmed / Implementing | ✅ Verified |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 9/9 ACs matched spec outcome | 0 spec-precision gaps  
**Sensor**: 3/3 mutations killed  
**Gate**: Quick 19 passed; BeneficioMensalServiceTest 16 passed  

**What works**: Resumo scoped aggregation + encargos 0 + A2 zeros; acessoTotal snapshot; deny empty; Dashboard evolução scoped; benefícios regressão intacta.

**Issues found**: none

**Next steps**: code-review / user commits (sem auto-commit)
