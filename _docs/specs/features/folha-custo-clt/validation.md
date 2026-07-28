# Folha CLT — Bruto, Líquido e Custo Empresa Validation

**Date**: 2026-07-28  
**Re-run (cycle 1)**: 2026-07-28 17:44 UTC-3 — commits `b78ba23`..`3d14347`  
**Re-run (cycle 2)**: 2026-07-28 17:49 UTC-3 — commits `df1358a`..`c09a298`  
**Spec**: `_docs/specs/features/folha-custo-clt/spec.md`  
**Diff range**: `c4ac1699a03f4acc20f7066eacda95d698c488b2..HEAD`  
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1–T26 | ✅ Done | All marked done in `tasks.md` |
| T27 | ✅ Done | Full gate green — backend 233/233; frontend lint 0 errors + build pass |

---

## Spec-Anchored Acceptance Criteria

Scope: 40 ACs (FCLT-17 P3 deferred). Evidence-or-zero: no `file:line` + assertion ⇒ gap.

### P1 — Operadores

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| FCLT-01 WHEN migration THEN operadores from tipo_rubrica | PROVENTO +1/+1/+1; DESCONTO 0/−1/0; INFORMATIVO 0/0/0 | `V1.17__rubrica_operadores.sql:6-28` — DDL/backfill (no test assertion) | ❌ GAP |
| FCLT-02 WHEN API cadastra/edita rubrica THEN persist operadores ∈ {−1,0,1} | Invalid → 400; valid persisted | `RubricaServiceTest.java:128-134` — `assertEquals((short)-1, captor.getValue().getOperadorLiquido())`; `:146` — `assertThrows(IllegalArgumentException.class, …)` | ✅ PASS |
| FCLT-03 WHEN UI Rubricas salvar THEN edit three operadores a11y | Three labeled fields persist via API | `Rubricas/index.tsx:352-411` — form fields (no automated assertion) | ⚠️ Spec-precision gap |

### P1 — Motor / ficha

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| FCLT-04 WHEN ADP import/process THEN ficha_mensal with bruto/liquido/custoFolha | Persisted totals per funcionário | `FolhaProcessamentoServiceTest.java:102-104` — `assertEquals(new BigDecimal("10000.00"), fichaFinal.getBruto())` etc. | ✅ PASS |
| FCLT-05 WHEN motor recalc THEN Σ valor×operador formulas | bruto/liquido/custoFolha from lines | `FolhaMotorCalculoTest.java:19-23` — provento+desconto totals; `FolhaProcessamentoServiceTest.java:102-104` | ✅ PASS |
| FCLT-06 WHEN consulta custoEmpresa THEN custoFolha+encargos+beneficios read-time | Composer adds three parts; benefits not in ficha | `FolhaCustoEmpresaComposerTest.java:13-19` — `assertEquals(new BigDecimal("9700.00"), resultado)`; `FolhaTotalizacaoServiceTest.java:62` — `assertEquals(new BigDecimal("8700.00"), total.custoEmpresa())` | ✅ PASS |
| FCLT-07 WHEN linha inserida THEN operadores snapshot + origemLinha | Copied from rubrica; origem FOLHA_ADP/CUSTO_FIXO/CALCULADO | `FolhaProcessamentoServiceTest.java:95-97` — operadores + `OrigemLinha.FOLHA_ADP`; `:139-142` — `CUSTO_FIXO`; `:214-217` — `CALCULADO` | ✅ PASS |
| FCLT-08 WHEN motor unit tests THEN PROVENTO+DESCONTO, custom, HALF_UP 2 | Cases above + rounding | `FolhaMotorCalculoTest.java:40-49` — `assertEquals(new BigDecimal("10.01"), totais.bruto())` | ✅ PASS |
| FCLT-INT-01 WHEN benefícios after folha THEN custoEmpresa updates without reimport | Benefits in consulta only | `FolhaTotalizacaoServiceTest.java:59-62` — `assertEquals(new BigDecimal("700.00"), total.salCustoBeneficios())`; `BeneficioConsultaAdapterTest.java:151-158` — batch map | ✅ PASS |

### P1 — ACL resumo

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| FCLT-ACL-01 scoped resumo three totals from scoped lines | totalBruto/Liquido/CustoEmpresa from CC-filtered lines | `ResumoFolhaPagamentoServiceTest.java:145-146` — scoped bruto/custo 8000 | ✅ PASS |
| FCLT-ACL-02 scoped encargos=0 | totalEncargos zero; custo without rateio | `ResumoFolhaPagamentoServiceTest.java:147` — `assertEquals(0, BigDecimal.ZERO.compareTo(dto.totalEncargos()))` | ✅ PASS |
| FCLT-ACL-03 acessoTotal global totals | Includes encargos + benefits | `ResumoFolhaPagamentoServiceTest.java:176-177` — bruto 60000, custoEmpresa 70000 | ✅ PASS |
| FCLT-ACL-04 snapshot no lines in scope → zeros + metadata | A2 zeros preserving id/dates | `ResumoFolhaPagamentoServiceTest.java:232-240` | ✅ PASS |
| FCLT-ACL-05 deny → empty list | RSF-03 | `ResumoFolhaPagamentoServiceTest.java:191` — `assertTrue(result.isEmpty())` | ✅ PASS |
| FCLT-ACL-06 scoped never reads global ficha totals | Discrimination sensor | `ResumoFolhaPagamentoServiceTest.java:400-401` — `verify(fichaMensalRepository, never()).findByCompetencia(…)` | ✅ PASS |

### P1 — ACL cards

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| FCLT-ACL-07 totais-funcionarios fields | bruto, liquido, custoFolha, custoBeneficios, custoEmpresa | `FolhaTotalizacaoServiceTest.java:59-62`; `FolhaPagamentoServiceTest.java:282-283` | ✅ PASS |
| FCLT-ACL-08 scoped lines CC filter; encargos=0 | Scoped encargosRateados zero | `FolhaTotalizacaoServiceTest.java:117` — `assertEquals(BigDecimal.ZERO.setScale(2), total.encargosRateados())` | ✅ PASS |
| FCLT-ACL-09 acessoTotal rateio D4-CLT | 800/200 on 8k/2k | `FolhaTotalizacaoServiceTest.java:140-143` | ✅ PASS |
| FCLT-ACL-10 FE cards use API not local aggregate | No reduce on money totals | `FolhaPagamento/index.tsx:202-206` — `consultarTotaisPorFuncionario` (no automated assertion) | ⚠️ Spec-precision gap |
| FCLT-ACL-11 paridade resumo ↔ cards | Sum cards = scoped resumo | `FolhaAclParidadeResumoCardsTest.java:103-152` — sum bruto/liquido/custoEmpresa = resumo scoped | ✅ PASS |

### P1 — ACL detalhe

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| FCLT-ACL-12 totalizer GROSS/NET/COMPANY_COST operador≠0 | contribuicao = valor×operador | `FolhaFichaConsultaServiceTest.java:85-87` — GROSS one line; `:106-108` — NET two lines | ✅ PASS |
| FCLT-ACL-13 out-of-scope ficha → 404 | FichaMensalNotFoundException | `FolhaFichaConsultaServiceTest.java:140-141` — `assertThrows(FichaMensalNotFoundException.class, …)` | ✅ PASS |
| FCLT-ACL-14 FE tabs keyboard a11y | Tabs with aria | `FolhaPagamento/index.tsx:753-773` — `role="tablist"`, `aria-controls` (no automated assertion) | ⚠️ Spec-precision gap |
| FCLT-ACL-15 aba Custo: ficha origens + BENEFICIO consulta; scoped no encargos | FOLHA_ADP, CUSTO_FIXO, BENEFICIO listed | `FolhaFichaConsultaServiceTest.java:128-131` | ✅ PASS |

### P1 — ACL dashboard

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| FCLT-ACL-16 scoped custoMensalFolha = custo empresa escopo ≠ liquido global | 5200 scoped vs 99999 snapshot liquido | `DashboardServiceTest.java:166-171` — `assertEquals(new BigDecimal("5200.00"), stats.custoMensalFolha())`; `:171` — evolução ≠ global liquido | ✅ PASS |
| FCLT-ACL-17 acessoTotal KPI global com encargos | custo includes rateio | `DashboardServiceTest.java:123` — `assertEquals(new BigDecimal("6500.00"), stats.custoMensalFolha())` | ✅ PASS |
| FCLT-ACL-18 evolução scoped custo empresa | Scoped points not global liquido | `DashboardServiceTest.java:169-171`; `:309` scoped decimoTerceiro separation | ✅ PASS |

### P1 — Frontend resumo/cards

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| FCLT-09 resumo columns Bruto/Líquido/Custo Empresa | Three columns in table | `FolhaPagamento/index.tsx:663-708` (no automated assertion) | ⚠️ Spec-precision gap |
| FCLT-10 cards three values | salBruto/salLiquido/custoEmpresa displayed | `FolhaPagamento/index.tsx:564-570` (no automated assertion) | ⚠️ Spec-precision gap |
| FCLT-11 detalhe abas totalizer | Tabs + API fetch | `FolhaPagamento/index.tsx:81-85`, `:262-277` (no automated assertion) | ⚠️ Spec-precision gap |
| FCLT-12 money as string decimal | No Number() for money calc | `formatMoneyDisplay` on money fields `:564-708`; `Number()` only for year/mes filter `:180-181` | ⚠️ Spec-precision gap |

### P2 — Rateio / regime / custos fixos / férias

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| FCLT-13 rateio 8k/2k → 800/200 ±R$0.01 | HALF_UP 2; sum tolerance | `EncargosRateioServiceTest.java:24-26` | ✅ PASS |
| FCLT-14 scoped rateio not executed | encargos=0 | `EncargosRateioServiceTest.java:30-35`; `FolhaLinhaAgregacaoTest.java:101` | ✅ PASS |
| FCLT-15 migration regime CLT seed | CLT active; funcionários → CLT | `FuncionarioConsultaAdapterTest.java:71-79` — `assertEquals("CLT", result.get().getRegimeTrabalho().getCodigo())`; `:79` — `assertTrue(…getAtivo())`; migration `V1.20__regime_trabalho.sql:9-26` | ✅ PASS |
| FCLT-16 férias 2,5 CALCULADO when recalcularFerias | Line cod 5000; valor 2500 on 12k sal | `FolhaProcessamentoServiceTest.java:214-217` — `assertEquals(new BigDecimal("2500.00"), linhaFerias.getValor())` | ✅ PASS |
| FCLT-18 schema funcionario_rubrica_fixa | Table columns per spec | `V1.19__funcionario_rubrica_fixa.sql:2-13` (no test assertion) | ❌ GAP |
| FCLT-19 CRUD rubrica fixa | Persist id + valor | `FuncionarioRubricaFixaServiceTest.java:74-80` | ✅ PASS |
| FCLT-20 overlap vigência → 409 | Conflict exception | `FuncionarioRubricaFixaServiceTest.java:111-112` — `assertThrows(FuncionarioRubricaFixaVigenciaConflictException.class, …)` | ✅ PASS |
| FCLT-21 UI Rubricas Fixas CRUD | CRUD with valor/vigência | `RubricasFixas/index.tsx:254` etc. (no automated assertion) | ⚠️ Spec-precision gap |
| FCLT-22 processar inject CUSTO_FIXO | origemLinha CUSTO_FIXO from cadastro | `FolhaProcessamentoServiceTest.java:138-142` | ✅ PASS |
| FCLT-23 dedup ADP vs fixo prefer ADP | Single ADP line when duplicate | `FolhaProcessamentoServiceTest.java:176-180` — `assertEquals(1, resultado.totalLinhas())` | ✅ PASS |
| FCLT-24 cadastro change stale until reprocess | Consulta unchanged until POST /processar | `FolhaProcessamentoServiceTest.java:226-274` — ficha 10500 unchanged after cadastro 800; 10800 after reprocess | ✅ PASS |
| FCLT-INT-02 scoped CUSTO_FIXO respects ACL by CC | Same filter as FOLHA_ADP | `FolhaConsultaAdapterTest.java:191-211` — scoped Set.of(100L) returns only CC 100 CUSTO_FIXO | ✅ PASS |
| FCLT-25 férias impact totals via operadores | bruto/custo include line | `FolhaProcessamentoServiceTest.java:222-223` — bruto 14500 | ✅ PASS |

**Status**: ⚠️ Cycle-2 — **32/40** with test assertions ✅; **8/40** ⚠️ impl-only (FE, AD-004 non-blocker); **2/40** ❌ schema/migration evidence (FCLT-01, FCLT-18 — tasks matrix: compile gate only)

---

## SPEC_DEVIATION

| Item | Notes |
| ---- | ----- |
| `GET /folha-pagamento/fichas/por-funcionario` | Not listed in spec API table; added in T24 to resolve `fichaId` before `GET …/linhas?totalizer=`. Supports FCLT-11 / ACL-12. Implementation: `FolhaFichaController.java:29-47`, FE `folhaPagamentoService.ts:85-98`. Documented deviation; behavior aligned with detalhe flow. |
| `CadastrosLookupPort.findRubricasFixasVigentesNaCompetencia` | Extended port for processamento + ArchUnit (T27 worker note). Supports FCLT-22. |

---

## Discrimination Sensor

**Sensor depth**: lightweight (5 mutations, scratch restore)

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| M1 | `FolhaMotorCalculo.java:50` | Ignore `operadorLiquido` in líquido sum | ✅ Killed (`FolhaMotorCalculoTest`) |
| M2 | `EncargosRateioService.java:52-53` | Wrong rateio denominator (+1) | ✅ Killed (`EncargosRateioServiceTest`) |
| M3 | `FolhaCustoEmpresaComposer.java` | Drop `custoBeneficios` from compose | ✅ Killed (`FolhaCustoEmpresaComposerTest`) |
| M4 | `FolhaLinhaAgregacao.java` | Scoped path encargos = 999 instead of 0 | ✅ Killed (`FolhaLinhaAgregacaoTest#agregarComBeneficiosEEncargos_scoped_…`) |
| M5 | `ResumoFolhaPagamentoService.java` | Scoped path calls `fichaMensalRepository.existsByCompetencia` | ✅ Killed (`ResumoFolhaPagamentoServiceTest#listarTodos_scoped_nuncaConsultaTotaisGlobaisFicha`) |

**Result**: 5/5 killed — ✅ PASS

---

## Cycle 2 Re-check (targeted gaps)

| Target | Expected | Evidence (re-run) | Result |
| ------ | -------- | ----------------- | ------ |
| FCLT-15 | Funcionário carregado com regime CLT ativo | `FuncionarioConsultaAdapterTest.java:71-79` — codigo `CLT`, `ativo=true` | ✅ PASS |
| Repo-wide FE lint | Full gate `npm run lint` exit 0 | `eslint .` — **0 errors**, 8 warnings (hook-deps brownfield) | ✅ PASS |
| Full gate | mvn test + lint + build | Backend 233/233; lint 0 errors; `tsc -b && vite build` success | ✅ PASS |

---

## Cycle 1 Re-check (targeted gaps)

| Target | Expected | Evidence (re-run) | Result |
| ------ | -------- | ----------------- | ------ |
| FCLT-ACL-11 | Sum cards = scoped resumo | `FolhaAclParidadeResumoCardsTest.java:150-152` — `compareTo` paridade bruto/líquido/custoEmpresa | ✅ PASS |
| FCLT-24 | Cadastro fixo stale until reprocess | `FolhaProcessamentoServiceTest.java:258-274` — 10500 unchanged; 10800 after reprocess | ✅ PASS |
| FCLT-INT-02 | Scoped CUSTO_FIXO ACL by CC | `FolhaConsultaAdapterTest.java:192-210` — `Set.of(100L)` + `verify(findByCompetenciaAndCentrosCustoIds)` | ✅ PASS |
| Security processar | ADMIN only | `SecurityConfigFolhaProcessamentoTest.java:47-74` — 403 USER/ACESSO_TOTAL | ✅ PASS |
| Security rubrica-fixa | ADMIN only | `SecurityConfigFuncionarioRubricaFixaTest.java:47-64` — 403 USER on GET/POST | ✅ PASS |
| FE lint (feature scope) | 0 errors on touched pages | `eslint` on Dashboard/FolhaPagamento/Rubricas/RubricasFixas/folhaPagamentoService — 0 errors, 1 warning | ✅ PASS |

---

## Gate Check

- **Gate command**: `cd backend && mvn test && cd ../frontend && npm run lint && npm run build`
- **Backend**: ✅ BUILD SUCCESS — **233** tests, **0** failed, **0** skipped (cycle 2 re-run 2026-07-28 17:49 UTC-3)
- **Frontend lint (repo-wide)**: ✅ **0 errors**, 8 warnings — hook-deps brownfield (`Rubricas`, `AuthContext`, `Organograma`, etc.)
- **Frontend build**: ✅ `tsc -b && vite build` success
- **Test files**: base `c4ac169` → **26**; HEAD → **36** (+10 test classes incl. cycle-1 security/paridade + cycle-2 FCLT-15)
- **ModularArchitectureTest**: ✅ 18 rules passed

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ |
| Matches existing patterns | ✅ |
| Spec-anchored backend tests non-shallow | ✅ |
| FE tests per AD-004 | ❌ Not added (known gap — **does not block PASS** per tasks matrix + AD-004) |
| Guidelines | `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` |

---

## Edge Cases (spot-check)

| Edge case | Result |
| --------- | ------ |
| Scoped competência in snapshot, no lines → zeros (A2) | ✅ `ResumoFolhaPagamentoServiceTest:232-240` |
| Operador 0 → line excluded from totalizer tab | ✅ `FolhaFichaConsultaServiceTest:85-87` |
| Dedup ADP vs fixo | ✅ `FolhaProcessamentoServiceTest:176-180` |
| Rateio centavos última parcela | ✅ `EncargosRateioServiceTest:48-57` |
| Custo fixo stale until reprocess | ✅ `FolhaProcessamentoServiceTest:226-274` |

---

## Security (cycle-1 fixes)

| Control | Expected | Evidence | Result |
| ------- | -------- | -------- | ------ |
| POST `/folha-pagamento/processar` ADMIN only | 403 USER/ACESSO_TOTAL; 2xx ADMIN | `SecurityConfig.java:39`; `SecurityConfigFolhaProcessamentoTest.java:47-74` | ✅ PASS |
| `/funcionario-rubrica-fixa/**` ADMIN only | 403 USER; 2xx ADMIN list/create | `SecurityConfig.java:43`; `SecurityConfigFuncionarioRubricaFixaTest.java:47-74` | ✅ PASS |

---

## Ranked Gaps

1. **FE ACs (FCLT-03, 09–12, 21, ACL-10, 14)** — implementation present, zero Vitest/Playwright assertions (AD-004; **non-blocker** per tasks matrix)
2. **Schema ACs (FCLT-01, FCLT-18)** — migration SQL only; compile gate passes; optional Flyway smoke not added (tasks matrix: none required)
3. ~~**Repo-wide lint**~~ — **closed cycle 2** (`df1358a` — 0 errors repo-wide)

---

## Requirement Traceability Update

| Requirement | Previous | Verified status |
| ----------- | -------- | --------------- |
| FCLT-15 | Done | ✅ Verified (cycle-2 `FuncionarioConsultaAdapterTest`) |
| FCLT-01, 18 | Done | ⚠️ Migration SQL only (compile gate; optional smoke) |
| FCLT-24, INT-02, ACL-11 | Done | ✅ Verified (cycle-1 fix) |
| FCLT-03, 09–12, 21, ACL-10, 14 | Done | ⚠️ Verified impl-only |
| All other in-scope FCLT / ACL | Done | ✅ Verified (test-backed) |
| FCLT-17 | Deferred | — |

---

## Summary

**Overall**: ✅ **PASS** (cycle 2 re-run)

**Cycle 2 targets**: FCLT-15 test assertion, repo-wide FE lint (`df1358a`), full gate — all ✅  
**Cycle 1 carry-over**: FCLT-ACL-11, FCLT-24, FCLT-INT-02, security fixes — still ✅  
**Spec-anchored check**: **32/40** ACs with test assertions; **8/40** ⚠️ impl-only (FE); **2/40** ❌ schema/migration evidence (non-blocking per tasks matrix)  
**Sensor**: 5 injected, 5 killed, 0 survived (unchanged from cycle 1)  
**Gate**: backend **233/233** pass; frontend **repo-wide lint pass** (0 errors); frontend build pass

**What works**: Backend motor, ACL dual-path, rateio, processamento (ADP + fixo + férias), paridade resumo↔cards, stale custo fixo, scoped CUSTO_FIXO ACL, ADMIN-only processar/rubrica-fixa, dashboard custo empresa, regime CLT load assertion.

**Remaining gaps (non-blocking)**: (1) FE Vitest/Playwright per AD-004 — impl-only, does **not** block PASS; (2) optional Flyway smoke for FCLT-01/18.

**Lessons**: not recorded (`lessons.py` requires `--feature/--signal/--source`; manual follow-up recommended).
