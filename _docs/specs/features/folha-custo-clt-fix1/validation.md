# folha-custo-clt-fix1 Validation

**Date**: 2026-07-28 (cycle 1 re-run)  
**Spec**: `_docs/specs/features/folha-custo-clt-fix1/spec.md`  
**Diff range**: `4b803c4..e061498` (T1–T8 + fix cycle 1: `c7e97aa`, `e061498`)  
**HEAD**: `e0614988a41809a549ff65576cb6318c9a5d2235`  
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status  | Notes |
| ---- | ------- | ----- |
| T1   | ✅ Done | `FolhaProcessamentoPort` interface |
| T2   | ✅ Done | `FolhaProcessamentoAdapter` + 2 unit tests |
| T3   | ✅ Done | Encadeamento import→process + 7 service tests |
| T4   | ✅ Done | `ImportacaoFolhaAdpResponseDTO` + controller mapping |
| T5   | ✅ Done | FE upload UX (toast/loading/error) |
| T6   | ✅ Done | `folhaPagamentoService.processarCompetencia` |
| T7   | ✅ Done | Manual reprocess section on `/importacao` |
| T8   | ✅ Done | FCLT-04 cross-ref + full gate |

---

## Spec-Anchored Acceptance Criteria

| Criterion | Spec-defined outcome | `file:line` + assertion expression | Result |
| --------- | -------------------- | ---------------------------------- | ------ |
| **FIX1-01** WHEN import ADP succeeds (incl. substituição confirmada) THEN invoke processamento same `competenciaInicio`/`competenciaFim`/`decimoTerceiro`, `recalcularFerias=false` | Port called post-persist with matching dates and `false` | `ImportacaoFolhaAdpServiceTest.java:111` — `verify(folhaProcessamentoPort).processar(COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false)`; `:129` — same after substituição; `FolhaProcessamentoAdapterTest.java:44-45` — `verify(folhaProcessamentoService).processar(..., new ProcessamentoOpcoes(false))` | ✅ PASS |
| **FIX1-02** WHEN import+process OK THEN API `success=true` and composite message (registros + `totalFichas`/`totalLinhas`) | HTTP 200, `success=true`, message mentions import + ficha counts | `ImportacaoFolhaAdpResponseDTOTest.java:29-37` — `assertTrue(response.success())`; `assertEquals("Importação concluída: 2 registros ADP; ficha processada: 3 fichas, 15 linhas", response.message())`; `assertEquals(3, response.fichasProcessadas())`; `assertEquals(15, response.linhasProcessadas())` | ✅ PASS |
| **FIX1-03** WHEN process fails after persist in same TX THEN `success=false`, rollback import, message about ficha failure | Exception propagates; API prefix *"Falha no processamento da ficha"*; TX rollback | `ImportacaoFolhaAdpServiceTest.java:176-183` — `assertThrows(FolhaProcessamentoFalhaException.class, ...)` + `verify(folhaImportacaoPort).persistirImportacao(...)`; `ImportacaoFolhaAdpControllerWebMvcTest.java:53-56` — `.andExpect(status().isInternalServerError())` + `jsonPath("$.success").value(false)` + `jsonPath("$.message").value("Falha no processamento da ficha: ...")`; rollback via `@Transactional` — not directly asserted | ✅ PASS (API + typed exception); ⚠️ rollback implicit only |
| **FIX1-04** WHEN import fails before process THEN process not invoked | `processar` never called on pre-persist failure | `ImportacaoFolhaAdpServiceTest.java:218-219` — `verify(folhaProcessamentoPort, never()).processar(...)`; `:77` — never on 409 without confirm | ✅ PASS |
| **FIX1-05** WHEN chained op completes THEN ≥1 `ficha_mensal` per CLT processed | Materialization produces fichas | `ImportacaoFolhaAdpServiceTest.java:102` — `assertEquals(processamento, result.processamento())` (port result only); `FolhaProcessamentoServiceTest.java:83-85` — `assertEquals(1, resultado.totalFichas())` (motor regression) | ⚠️ Spec-precision gap (no integration assert on `ficha_mensal` post-import) |
| **FIX1-06** WHEN chained success THEN UI toast mentions importação **e** processamento | Toast uses composite API message | — (AD-004: no Vitest; impl `Importacao/index.tsx:162` — `toast.success(response.message)`) | ❌ NOT covered |
| **FIX1-07** WHEN FIX1-03 error THEN UI error, no partial success | `success` stays false; error toast | — (AD-004; impl `Importacao/index.tsx:168-174`, `:194`) | ❌ NOT covered |
| **FIX1-08** WHEN chained in progress THEN loading text *"Importando e processando ficha…"* | Button/status composite text | — (AD-004; impl `Importacao/index.tsx:598`, `:705-707`) | ❌ NOT covered |
| **FIX1-09** WHEN admin on `/importacao` THEN section *"Processar ficha da competência"* with mês/ano, 13º, Processar | Section visible with controls | — (AD-004; impl `Importacao/index.tsx:612-691`) | ❌ NOT covered |
| **FIX1-10** WHEN Processar clicked THEN POST `/folha-pagamento/processar` and show totals/error | POST with competência; display totals | — (AD-004; impl `folhaPagamentoService.ts:153-158`, `Importacao/index.tsx:302-310`) | ❌ NOT covered |
| **FIX1-11** WHEN non-admin process THEN API 403 and UI permission message | 403 on `/processar`; FE permission toast | `SecurityConfigFolhaProcessamentoTest.java:48-58` — `.andExpect(status().isForbidden())` for `USER`; UI — no test (`Importacao/index.tsx:313-315`) | ✅ PASS (API); ❌ NOT covered (UI) |
| **FIX1-12** WHEN recalcular férias checked THEN `opcoes.recalcularFerias=true` | Request sends true when checkbox on | `FolhaProcessamentoAdapterTest.java:55-63` — `verify(..., new ProcessamentoOpcoes(true))`; FE wiring — no test | ⚠️ Spec-precision gap (adapter ✅; FE request body not asserted) |
| **FIX1-13** WHEN manual reprocess success THEN toast confirms competência | Toast *"Ficha processada: X fichas, Y linhas"* | — (AD-004; impl `Importacao/index.tsx:308-310`) | ❌ NOT covered |

**Status**: 6/13 ACs with automated assertion expressions; 2 spec-precision gaps; 6 NOT covered (FE AD-004); 0 backend GAPs (FIX1-02/03 closed in cycle 1)

---

## Edge Cases

| Edge case | Spec-defined outcome | Evidence | Result |
| --------- | -------------------- | -------- | ------ |
| Competência sem linhas CLT processáveis | `totalFichas=0`, import OK | `ImportacaoFolhaAdpServiceTest.java:199-200` — `assertEquals(0, result.processamento().totalFichas())` | ✅ PASS |
| Substituição confirmada encadeia process | Persist + process after confirm | `ImportacaoFolhaAdpServiceTest.java:128-129` — `verify(folhaProcessamentoPort).processar(...)` | ✅ PASS |
| Timeout 5 min (sync) | Client extended timeout | — (impl only; `importacaoService` timeout 300000) | ❌ NOT covered |
| Reprocesso manual sem import ADP | API returns totals; UI displays | — (impl only) | ❌ NOT covered |

---

## Discrimination Sensor

Scratch state: temp copy of `backend/` → inject behavior-level fault → run targeted tests → discard copy (working tree unchanged).

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| M1 | `ImportacaoFolhaAdpService.java:260-265` | Skip `folhaProcessamentoPort.processar`, return hardcoded `(0,0,0)` | ✅ Killed — `ImportacaoFolhaAdpServiceTest#importar_happyPath` verify fails |
| M2 | `ImportacaoFolhaAdpService.java:261` | Pass `recalcularFerias=true` instead of `false` | ✅ Killed — mock `eq(false)` + verify mismatch |
| M3 | `ImportacaoFolhaAdpService.java:236` | Comment out funcionário-not-found throw (process invoked on bad import) | ✅ Killed — `importar_funcionarioNaoEncontrado_nuncaChamaProcessamento` failure |
| M4 | `FolhaProcessamentoAdapter.java:29` | `ProcessamentoOpcoes(true)` always | ✅ Killed — `FolhaProcessamentoAdapterTest` verify error |
| M5 | `ImportacaoFolhaAdpResponseDTO.java:32` | `success=false` in success factory | ✅ Killed — `ImportacaoFolhaAdpResponseDTOTest#success_factorySetsSuccessTrueAndCompositeMessage` `assertTrue(response.success())` fails |

**Sensor depth**: lightweight (5 behavior-level faults)  
**Result**: 5/5 killed — ✅ PASS

---

## Gate Check

- **Gate command**: `cd backend && mvn test && cd ../frontend && npm run lint && npm run build`
- **Backend**: **242 passed**, 0 failed, 0 skipped (+4 vs cycle 0: `ImportacaoFolhaAdpResponseDTOTest` ×2, `ImportacaoFolhaAdpControllerWebMvcTest` ×2)
- **Frontend lint**: 0 errors, 8 warnings (pre-existing `react-hooks/exhaustive-deps`, unrelated files)
- **Frontend build**: success (`tsc -b && vite build`)
- **Feature test delta (cycle 1)**: +2 DTO unit tests; +2 WebMvcTest; service test updated for `FolhaProcessamentoFalhaException`
- **Regressions**: `FolhaProcessamentoServiceTest` (5), `ModularArchitectureTest` (18), `SecurityConfigFolhaProcessamentoTest` (3) — all green in full gate

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ |
| No scope creep | ✅ |
| Matches port/adapter + existing UX patterns | ✅ |
| Test Coverage Matrix (`tasks.md`) met for declared layers | ✅ (FE gap documented AD-004) |
| Guidelines: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` | ✅ |

---

## Requirement Traceability Update

| Requirement | Cycle 0 Status | Cycle 1 Status |
| ----------- | -------------- | -------------- |
| FIX1-01, FIX1-04 | ✅ Verified (automated) | ✅ Verified (automated) |
| FIX1-02 | ❌ Needs DTO/WebMvcTest | ✅ Verified (`ImportacaoFolhaAdpResponseDTOTest`) |
| FIX1-03 | ⚠️ Partial (service only) | ✅ Verified (typed exception + WebMvcTest HTTP 500) |
| FIX1-05, FIX1-12 | ⚠️ Partial | ⚠️ Partial (unchanged) |
| FIX1-11 | ✅ API verified | ✅ API verified |
| FIX1-06 … FIX1-10, FIX1-13 | ⚠️ Impl inspection only | ⚠️ Impl inspection only (AD-004) |

---

## Summary

**Overall**: ✅ PASS (gate green; FIX1-02/03 gaps closed; M5 killed; FE AD-004 gaps non-blocking)

**Spec-anchored check**: 6/13 ACs with assertion expressions; 2 spec-precision gaps; 6 NOT covered (FE AD-004); 0 backend GAPs  
**Sensor**: 5 injected, 5 killed, 0 survived  
**Gate**: 242 passed, 0 failed

**Cycle 1 fixes verified**:
- `c7e97aa` — `FolhaProcessamentoFalhaException` replaces substring heuristics; controller returns HTTP 500 + prefixed message; WebMvcTest asserts FIX1-03 contract
- `e061498` — `ImportacaoFolhaAdpResponseDTOTest` asserts `success=true`, composite message, and processing stats (kills M5)

**Remaining non-blocking gaps** (AD-004 / optional):
1. FIX1-05 integration — optional `@SpringBootTest` verifying `ficha_mensal` rows post-chain
2. FIX1-06…13 FE — Vitest/Playwright when AD-004 policy expands
3. FIX1-11 UI 403 — component test for permission toast
4. FIX1-03 rollback — implicit via `@Transactional`; no explicit TX rollback assertion

**Next steps**: None required for feature closure; optional FE tests in separate AD-004 follow-up.
