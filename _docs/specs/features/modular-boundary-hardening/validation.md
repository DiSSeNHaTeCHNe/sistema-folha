# Modular Boundary Hardening Validation

**Date**: 2026-07-27  
**Spec**: `_docs/specs/features/modular-boundary-hardening/spec.md`  
**Diff range**: uncommitted working tree (`main` ahead of `origin/main` by 3 commits + large uncommitted modular migration surface including this feature)  
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 FolhaConsultaPort + snapshots | ✅ Done | Port + records present |
| T2 FolhaConsultaAdapter + tests | ✅ Done | 4 adapter tests green |
| T3 CadastrosImportLookupPort | ✅ Done | Port + refs present |
| T4 CadastrosImportLookupAdapter | ✅ Done | Adapter tests green |
| T5 BeneficioConsultaPort scoped | ✅ Done | Scoped + empty-set + unscoped regressão |
| T6 FolhaImportacaoPort + commands | ✅ Done | Port + command records |
| T7 FolhaImportacaoAdapter + tests | ✅ Done | 3 adapter tests green |
| T8 Move Stats DTOs | ✅ Done | DTOs in `dashboard.api` |
| T9 Dashboard ACL + ports | ✅ Done | 6 service tests; controller passes login |
| T10 ImportacaoFolhaAdp refactor | ✅ Done | Ports only; 3 service tests |
| T11 ArchUnit + AD-010 | ✅ Done | Rules present; AD-009 superseded |
| T12 Gate final | ✅ Done | Full mandatory gate PASS |
| MODBH-33 (P3) | ⏭️ Deferred | Out of Execute scope |

---

## Spec-Anchored Acceptance Criteria

| Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| ------------------------- | -------------------- | ----------------------- | ------ |
| MODBH-01 Controller passa login | `Authentication.getName()` → `getStats(login)` | `DashboardController.java:23-24` — `dashboardService.getStats(authentication.getName())` | ✅ PASS |
| MODBH-02 Restrito+centros vazios → empty stats | Totais 0, listas vazias; sem agregação global | `DashboardServiceTest.java:70-74` — `assertEmptyStats(stats)` + `verify(..., never()).findResumoMaisRecente()` | ✅ PASS |
| MODBH-03 SEM_FUNCIONARIO / SEM_NO → empty | Stats zerados/vazios (mesma regra MODBH-02) | `DashboardServiceTest.java:86-88` — `assertEmptyStats` + never ports; shared branch `DashboardService.java:144-145` (`motivoNegacao() != null`) | ✅ PASS |
| MODBH-04 acessoTotal → agregação global | Baseline preservado (resumo/linhas unscoped) | `DashboardServiceTest.java:114-121` — `assertEquals(1L, totalFuncionarios)`, `custoMensalFolha=50000.00`, `findLinhas...(isNull())` | ✅ PASS |
| MODBH-05 Restrito+centros → só escopo | Agregações só dos centros acessíveis | `DashboardServiceTest.java:145-152` — `eq(centros)` nas linhas; `contar...PorCentros`; `never` unscoped benefício/evolução | ✅ PASS |
| MODBH-06 Cobertura ACL unitária | restrito+empty, SEM_FUNCIONARIO, total-access | `DashboardServiceTest.java:63,78,93` — três testes nomeados; falhariam sob stats globais (sensor M1/M3) | ✅ PASS |
| MODBH-07 FolhaConsultaPort existe | Interface pública em `folha.port` | `FolhaConsultaPort.java:8-23` — interface com métodos de agregação | ✅ PASS |
| MODBH-08 Port sem @Entity | Primitivos/records/DTOs | `FolhaResumoSnapshot.java:6-12`, `FolhaLinhaSnapshot.java:5-18` — records; `rg @Entity` em `folha/port` → zero | ✅ PASS |
| MODBH-09 Adapter em folha.application | Delega a repos folha | `FolhaConsultaAdapter.java:25-28` — `implements FolhaConsultaPort` + repos folha | ✅ PASS |
| MODBH-10 Adapter happy + empty | Competência recente + ausente | `FolhaConsultaAdapterTest.java:53-56` — `assertTrue(isPresent)`; `:65` — `assertTrue(isEmpty)` | ✅ PASS |
| MODBH-11 Zero cadastros.infra em dashboard.application | Zero imports | Verifier `rg` → zero matches; `DashboardService.java:43-47` — só ports | ✅ PASS |
| MODBH-12 Consumo via cadastros.port | Sem @Entity na superfície | `CadastrosImportLookupPort.java:6-14` — refs/counts; `DashboardService.java:71-73` — `countFuncionariosAtivos*` | ✅ PASS |
| MODBH-13 Mocks usam port | Não repositories cadastros | `DashboardServiceTest.java:47-48` — `@Mock CadastrosImportLookupPort` | ✅ PASS |
| MODBH-14 Sem *Repository folha/cadastros | Só ports | `DashboardService.java:43-47` — cinco ports; grep `Repository` → zero | ✅ PASS |
| MODBH-15 Injeta OrganogramaAcessoPort + UsuarioLookupPort | Presentes | `DashboardService.java:46-47` — fields; tests stub ambos | ✅ PASS |
| MODBH-16 DashboardServiceTest verde | Mocks de ports | Suite: 6/6; gate `mvn test` inclui classe | ✅ PASS |
| MODBH-17 Compliance mandatory verde | Script exit 0 (mandatory) | `./diversos/scripts/check-modular-compliance.sh` → **Mandatory checks: PASS** (exit 0); lint advisory FAIL (AD-004) | ✅ PASS |
| MODBH-18 FolhaImportacaoPort existe | Port escrita em `folha.port` | `FolhaImportacaoPort.java:7-9` — `persistirImportacao(command)` | ✅ PASS |
| MODBH-19 Contrato sem entities | Commands/DTOs | `FolhaImportacaoCommand.java:6-13`, `FolhaImportacaoLinhaCommand.java:7-16` — records Long/BigDecimal | ✅ PASS |
| MODBH-20 Adapter + TX ownership | `folha.application` + Design TX | `FolhaImportacaoAdapter.java:30,37-38` — `@Transactional`; `design.md` § ownership/join | ✅ PASS |
| MODBH-21 Adapter testes persistência | Mock repos no adapter | `FolhaImportacaoAdapterTest.java:108-111,140-141,158` — save/delete/never resumo | ✅ PASS |
| MODBH-22 Zero foreign infra importacao.application | Zero imports | Verifier `rg` → zero; `ImportacaoFolhaAdpService.java:42-44` — três ports | ✅ PASS |
| MODBH-23 Lookup via ports cadastros | Não repositories | `ImportacaoFolhaAdpService.java:42` — `CadastrosImportLookupPort`; test mocks port | ✅ PASS |
| MODBH-24 Persistência via FolhaImportacaoPort | Exclusivo | `ImportacaoFolhaAdpServiceTest.java:87` — `verify(folhaImportacaoPort).persistirImportacao(...)` | ✅ PASS |
| MODBH-25 Resultado funcional equivalente | Contrato HTTP estável | `ImportacaoFolhaAdpResponseDTO.java:5-16,18-31` — mesmos campos; service retorna `List<FolhaPagamentoDTO>` (`ImportacaoFolhaAdpServiceTest.java:85`) | ✅ PASS |
| MODBH-26 Arquivo inválido — rejeição preservada | Sem relaxar validação | `ImportacaoFolhaAdpService.java:226-231` — throws RuntimeException (CPF/funcionários); `ImportacaoFolhaAdpServiceTest.java:57-65` — duplicidade ainda rejeita; **sem teste dedicado “arquivo inválido” genérico** | ⚠️ Spec-precision gap |
| MODBH-27 ArchUnit dashboard rule | Regra nomeada presente | `ModularArchitectureTest.java:192-203` — `dashboard_application_must_not_access_foreign_infrastructure` | ✅ PASS |
| MODBH-28 ArchUnit importacao rule | Regra equivalente | `ModularArchitectureTest.java:206-217` — `importacao_application_must_not_access_foreign_infrastructure` | ✅ PASS |
| MODBH-29 ModularArchitectureTest exit 0 | Zero violations | `mvn test -Dtest=ModularArchitectureTest` → Tests run: **18**, Failures: **0**, exit **0** | ✅ PASS |
| MODBH-30 AD-009 superseded / AD-010 | STATE documenta | `STATE.md:82` — `superseded by AD-010`; `STATE.md:84-87` — AD-010 active | ✅ PASS |
| MODBH-31 Stats DTOs → dashboard.api | Pacote correto, JSON estável | `LinhaNegocioStatsDTO.java:1` etc. em `dashboard.api`; wire fields inalterados | ✅ PASS |
| MODBH-32 DashboardStatsDTO imports novo pacote | Imports `dashboard.api.*` | `DashboardStatsDTO.java:1,6-17` — same package; `rg cadastros.api.*StatsDTO` → zero | ✅ PASS |

**MODBH-33**: Deferred (P3) — not in scope for this verdict.

**Status**: ⚠️ Spec-precision gaps flagged (1 soft: MODBH-26) — no hard uncovered ACs among MODBH-01…32

---

## Discrimination Sensor

Scratch only: temp copy at `/tmp/modbh-sensor.*` (rsync backend). Real working tree never mutated (`REAL_TREE_CLEAN`).

| Mutation | File:line (scratch) | Description | Killed? |
| -------- | ------------------- | ----------- | ------- |
| 1 | `DashboardService.java:150` | ACL short-circuit: empty-centros deny → `return false` | ✅ Killed — `getStats_restritoComCentrosVazios_...` failed (`never().findResumoMaisRecente` violated) |
| 2 | `FolhaImportacaoAdapter.java:39` | Skip delete-before-insert: `if (false && substituirExistente)` | ✅ Killed — `persistirImportacao_comSubstituir_removeAntesDeInserir` failed (`deleteAll` not invoked) |
| 3 | `DashboardService.java:62` | Bypass deny emptyStats: `if (false && deveNegarAcesso)` | ✅ Killed — restrito+empty + SEM_FUNCIONARIO both failed (`findResumoMaisRecente` invoked) |

**Sensor depth**: lightweight (3 behavior-level faults)  
**Result**: 3/3 killed — PASS ✅

---

## Interactive UAT Results

N/A — backend/infrastructure feature; automated checks sufficient per validate.md.

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code | ✅ |
| Surgical changes | ✅ |
| No scope creep | ✅ (P3 deferred) |
| Matches patterns | ✅ (ports/adapters like benefícios) |
| Spec-anchored outcome check | ✅ (1 soft precision note MODBH-26) |
| Per-layer Coverage Expectation | ✅ (adapters + DashboardService + Importacao + ArchUnit) |
| Every test maps to AC / Done-when | ✅ |
| Documented guidelines: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4 | ✅ |

---

## Edge Cases

- [x] Sem resumo recente → stats via count cadastros / zeros (`getStats_acessoTotalSemResumo_usaCountCadastros`)
- [x] Login ausente → emptyStats (`getStats_loginAusente_retornaEmpty`)
- [x] Importação duplicidade sem confirmar → FolhaDuplicadaException + never persist
- [x] Port consulta competência ausente → Optional.empty
- [x] Benefício scoped empty set → 0 (`BeneficioConsultaAdapterTest`)
- [ ] Arquivo ADP “inválido” genérico — sem teste unitário dedicado (MODBH-26 soft gap)

---

## Gate Check

| Command | Exit | Notes |
| ------- | ---- | ----- |
| `cd backend && mvn test` | **0** | Tests run: **120**, Failures: 0, Errors: 0, Skipped: 0 |
| `cd frontend && npm run build` | **0** | tsc + vite build OK |
| `./diversos/scripts/check-modular-compliance.sh` | **0** | Mandatory checks: **PASS**; lint advisory FAIL (AD-004) |
| `rg 'folha\.infrastructure\|cadastros\.infrastructure' …/dashboard/application` | **1** (no matches) | Zero foreign infra ✅ |
| `rg 'folha\.infrastructure\|cadastros\.infrastructure' …/importacao/application` | **1** (no matches) | Zero foreign infra ✅ |
| `mvn test -Dtest=ModularArchitectureTest` | **0** | Tests run: **18**, Failures: 0 |

- **Gate command (Full)**: `mvn test && npm run build && check-modular-compliance.sh`
- **Result**: mandatory gate **passed** (120 backend tests; FE build; compliance mandatory PASS)
- **Test count after feature**: 120 (STATE baseline cited ≥94; delta positive)
- **Skipped tests**: none
- **Failures**: none (under unrestricted JVM; Mockito self-attach required — sandbox breaks MockMaker)

---

## Fix Plans (if issues found)

### Soft: MODBH-26 invalid-file assertion (optional)

- **Root cause**: Spec outcome “arquivo inválido” is underspecified; no unit test asserts a dedicated invalid ADP payload rejection beyond duplicidade/happy path.
- **Fix task** (optional, non-blocking): Add `ImportacaoFolhaAdpServiceTest` case for unreadable/malformed payload asserting existing exception message/type; or tighten spec wording to “duplicidade + funcionários inexistentes”.
- **Priority**: Minor / Spec-precision

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| MODBH-01…25 | Done | ✅ Verified |
| MODBH-26 | Done | ⚠️ Verified with spec-precision note |
| MODBH-27…32 | Done | ✅ Verified |
| MODBH-33 | Deferred | Deferred (unchanged) |

---

## Summary

**Overall**: ✅ Ready (soft precision note on MODBH-26 only)

**Spec-anchored check**: 31/32 ACs matched with hard evidence; 1/32 spec-precision gap (MODBH-26)  
**Sensor**: 3/3 mutations killed  
**Gate**: 120 passed, 0 failed; compliance mandatory PASS; ArchUnit 18/18

**What works**: Dashboard ACL empty-set/total-access/scoped; FolhaConsultaPort + FolhaImportacaoPort; Importacao ADP via ports; ArchUnit AD-010; Stats DTOs in `dashboard.api`; zero foreign infra in dashboard/importacao application.

**Issues found**: MODBH-26 lacks a dedicated invalid-file unit assertion (rejection paths still present in code).

**Next steps**: Optional strengthen MODBH-26 test; mark feature Verifier PASS; distill lessons only if treating soft gap as signal (none recorded — clean PASS with advisory note only).

---

# Fix Cycle 1 Re-verification

**Date**: 2026-07-27  
**Verifier**: independent sub-agent (author ≠ verifier)  
**Cycle**: fix → re-verify #1 (of max 3)  
**Scope**: FIX-1 / FIX-2 / FIX-3 only + discrimination sensor re-run + mandatory gates  
**Real tree**: read-only (mutations only under `/tmp/modbh-fix1-sensor.*`; restored; `NO_MUTATION_IN_REAL_TREE`)

---

## Fixes Under Review — Test Evidence

| Fix | Intent | Implementation | Test evidence (`file:line` + assertion) | Result |
| --- | ------ | -------------- | ---------------------------------------- | ------ |
| FIX-1 | `substituirExistente=true` skips `existsByFuncionarioIdAndRubricaIdAndPeriodo` so replacement still emits linhas | `ImportacaoFolhaAdpService.java:331-334` — `jaExiste = !substituirExistente && exists…` | `ImportacaoFolhaAdpServiceTest.java:115-141` — `importar_confirmarSubstituicao_naoPulaLinhasQuandoJaExistem`: `assertTrue(command.substituirExistente())`; `assertFalse(command.linhas().isEmpty())`; `verify(..., never()).existsByFuncionarioIdAndRubricaIdAndPeriodo(...)` | ✅ PASS |
| FIX-2 | Null optional FKs (cargo / centro / linha) must not call `EntityManager.getReference` | `FolhaImportacaoAdapter.java:74-82` — null-guarded `getReference` | `FolhaImportacaoAdapterTest.java:167-196` — `persistirImportacao_cargoECentroCustoNull_persisteSemGetReferenceOpcional`: `assertNull(salva.getCargo/CentroCusto/LinhaNegocio)`; `verify(entityManager, never()).getReference(eq(Cargo\|CentroCusto\|LinhaNegocio.class), isNull())` | ✅ PASS |
| FIX-3 | Empty-resumo dashboard path must count benefícios (competência do mês corrente), not leave total at 0 | `DashboardService.java:70-98` — fallback competência + `contarLancamentosAtivosNaCompetencia*` | `DashboardServiceTest.java:165-178` — `assertEquals(7L, stats.totalBeneficiosAtivos())` + verify unscoped count; `DashboardServiceTest.java:182-195` — `assertEquals(12L, stats.totalBeneficiosAtivos())` + `never` scoped | ✅ PASS |

---

## Discrimination Sensor (re-run, scratch only)

Scratch: `/tmp/modbh-fix1-sensor.xsgzDU` (rsync backend). Real working tree never mutated.

| Mutation | File:line (scratch) | Description | Killed? |
| -------- | ------------------- | ----------- | ------- |
| 1 (FIX-1 revert) | `ImportacaoFolhaAdpService.java:~332` | Always call `existsBy…` (drop `!substituirExistente` short-circuit) | ✅ Killed — `importar_confirmarSubstituicao_naoPulaLinhasQuandoJaExistem` failed (`never().existsBy…` violated; invoked with `[1,2,2024-10-01,2024-10-31]`) |
| 2 (FIX-2 revert) | `FolhaImportacaoAdapter.java:~74` | Unconditional `getReference` for cargo/centro/linha even when IDs null | ✅ Killed — `persistirImportacao_cargoECentroCustoNull_persisteSemGetReferenceOpcional` failed (`never().getReference(..., isNull())` violated) |
| 3 (FIX-3 revert) | `DashboardService.java:~75` | Empty-resumo path hardcodes `totalBeneficiosAtivos = 0L` | ✅ Killed — `getStats_acessoTotal_semResumo_incluiBeneficios` failed (`expected: <12> but was: <0>`); also `getStats_acessoTotalSemResumo_usaCountCadastros` (`expected: <7> but was: <0>`) |

**Sensor depth**: lightweight (3 behavior-level faults targeting fix cycle 1)  
**Result**: 3/3 killed — PASS ✅

---

## Gate Check (re-run)

| Command | Exit | Notes |
| ------- | ---- | ----- |
| `cd backend && mvn test` | **0** | Tests run: **123**, Failures: 0, Errors: 0, Skipped: 0 (`BUILD SUCCESS`) |
| `mvn test -Dtest=ModularArchitectureTest` | **0** | Tests run: **18**, Failures: 0 |
| `./diversos/scripts/check-modular-compliance.sh` | **0** | Mandatory checks: **PASS**; FE lint advisory FAIL (AD-004) |
| `rg 'folha\.infrastructure\|cadastros\.infrastructure' …/dashboard/application` | no matches | Zero foreign infra ✅ |
| `rg 'folha\.infrastructure\|cadastros\.infrastructure' …/importacao/application` | no matches | Zero foreign infra ✅ |

- **Test count delta vs initial validation**: 120 → **123** (+3 fix-cycle tests: substituir-skip-exists, null-FK adapter, empty-resumo benefícios)
- **Skipped**: none
- **Failures**: none

---

## Spec-precision carry-forward

- MODBH-26 (arquivo inválido genérico) remains a soft ⚠️ Spec-precision gap from the initial report — **unchanged**, non-blocking for this fix cycle.

---

## Fix Cycle 1 Summary

**Overall**: ✅ PASS — all three fixes evidenced; sensor 3/3 killed; gates green

**Spec-anchored (fixes)**: 3/3 FIX criteria with `file:line` evidence  
**Sensor**: 3/3 mutations killed  
**Gate**: 123 passed, 0 failed; ArchUnit 18/18; compliance mandatory PASS; rg foreign-infra zero

**Lessons**: none recorded (clean PASS; MODBH-26 soft note already carried from initial validation, no new hard signal)

**Next steps**: Feature ready at Verifier PASS for fix cycle 1; optional MODBH-26 strengthen remains advisory only.
