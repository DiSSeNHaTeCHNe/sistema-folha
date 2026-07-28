# folha-custo-clt-fix1 Validation

**Date**: 2026-07-28  
**Spec**: `_docs/specs/features/folha-custo-clt-fix1/spec.md`  
**Diff range**: `7e0facd..07973e1` (T1–T8 on `feat/folha-custo-clt`)  
**Verifier**: independent sub-agent (author ≠ verifier)

---

## Task Completion

| Task | Status  | Notes |
| ---- | ------- | ----- |
| T1   | ✅ Done | FolhaProcessamentoPort |
| T2   | ✅ Done | Adapter + unit tests |
| T3   | ✅ Done | Encadeamento + service tests |
| T4   | ✅ Done | Response DTO + controller |
| T5   | ✅ Done | FE upload UX |
| T6   | ✅ Done | processarCompetencia client |
| T7   | ✅ Done | Manual reprocess section |
| T8   | ✅ Done | Docs + full gate |

---

## Spec-Anchored Acceptance Criteria

| Criterion | Spec-defined outcome | `file:line` + assertion / evidence | Result |
| --------- | -------------------- | ---------------------------------- | ------ |
| **FIX1-01** WHEN import ADP succeeds THEN invoke processamento same competência, `recalcularFerias=false` | Port called after persist with matching dates/decimoTerceiro and `false` flag | `ImportacaoFolhaAdpServiceTest.java:110` — `verify(folhaProcessamentoPort).processar(COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false)`; `FolhaProcessamentoAdapterTest.java:44-45` — `verify(folhaProcessamentoService).processar(..., new ProcessamentoOpcoes(false))` | ✅ PASS |
| **FIX1-02** WHEN import+process OK THEN API `success=true` and composite message (registros + fichas/linhas) | HTTP 200, `success=true`, message mentions import counts and `totalFichas`/`totalLinhas` | `ImportacaoFolhaAdpServiceTest.java:100-101` — `assertEquals(processamento, result.processamento())`; `ImportacaoFolhaAdpResponseDTO.java:51-59,63` — `success(true)` + message `"Importação concluída: %d registros ADP; ficha processada: %d fichas, %d linhas"` | ⚠️ Spec-precision gap (service + DTO impl; no WebMvcTest on HTTP body) |
| **FIX1-03** WHEN process fails after persist in same TX THEN `success=false`, rollback, message about ficha failure | Exception propagates; API error prefix *"Falha no processamento da ficha"*; TX rolls back | `ImportacaoFolhaAdpServiceTest.java:175-180` — `assertThrows(RuntimeException.class, ...)` + `assertEquals("Erro no processamento da ficha", ex.getMessage())`; `ImportacaoFolhaAdpController.java:133-139` — maps to `"Falha no processamento da ficha: " + message`; `@Transactional` on service (`ImportacaoFolhaAdpService.java:79`) | ⚠️ Spec-precision gap (rollback via TX convention; no HTTP assertion on `success=false`) |
| **FIX1-04** WHEN import fails before process THEN process not invoked | `processar` never called on pre-persist failure | `ImportacaoFolhaAdpServiceTest.java:218-219` — `verify(folhaProcessamentoPort, never()).processar(...)` when funcionário não encontrado | ✅ PASS |
| **FIX1-05** WHEN chained op completes THEN ≥1 `ficha_mensal` per CLT processed | Materialization produces fichas with lines | `FolhaProcessamentoServiceTest.java:83-85` — `assertEquals(1, resultado.totalFichas())`; `ImportacaoFolhaAdpServiceTest.java:100-101` — chained result includes non-null `processamento` | ✅ PASS |
| **FIX1-06** WHEN chained success THEN UI toast mentions importação **e** processamento | Toast uses API `response.message` (composite) | `Importacao/index.tsx:162` — `toast.success(response.message)`; backend message at `ImportacaoFolhaAdpResponseDTO.java:51-59` | ⚠️ Spec-precision gap (manual/lint gate — AD-004; no Vitest) |
| **FIX1-07** WHEN FIX1-03 error THEN UI error, no partial success | `success` state stays false; error toast | `Importacao/index.tsx:168-174` — `success: false` branch + `toast.error(response.message)`; catch block `194` — `success: false` | ⚠️ Spec-precision gap (manual/lint gate — AD-004) |
| **FIX1-08** WHEN chained in progress THEN loading text composite | Button/status shows *"Importando e processando ficha…"* | `Importacao/index.tsx:598` — button label; `705-707` — status panel text | ⚠️ Spec-precision gap (manual/lint gate — AD-004) |
| **FIX1-09** WHEN admin on `/importacao` THEN section *"Processar ficha da competência"* with mês/ano, 13º, Processar | Section visible with controls | `Importacao/index.tsx:612-691` — card title, month/year selects, 13º checkbox, Processar button | ⚠️ Spec-precision gap (manual/lint gate — AD-004) |
| **FIX1-10** WHEN Processar clicked THEN POST `/folha-pagamento/processar` and show totals/error | Service POST with competência; toast with totals | `folhaPagamentoService.ts:305-315` — `api.post('/folha-pagamento/processar', {...})`; `Importacao/index.tsx:302-310` — success toast with `totalFichas`/`totalLinhas` | ⚠️ Spec-precision gap (manual/lint gate — AD-004) |
| **FIX1-11** WHEN non-admin process THEN API 403 and UI permission message | 403 on `/processar`; FE permission toast | `SecurityConfigFolhaProcessamentoTest.java:48-58` — `.andExpect(status().isForbidden())` for `USER`; `Importacao/index.tsx:313-315` — 403 toast *"Apenas administradores..."* | ✅ PASS (API); ⚠️ Spec-precision gap (UI 403 — manual/lint) |
| **FIX1-12** WHEN recalcular férias checked THEN `opcoes.recalcularFerias=true` | Request body sends true when checkbox on | `folhaPagamentoService.ts:313` — `opcoes: { recalcularFerias: params.recalcularFerias }`; `Importacao/index.tsx:306` — passes `recalcularFerias` state | ⚠️ Spec-precision gap (manual/lint gate — AD-004) |
| **FIX1-13** WHEN manual reprocess success THEN toast confirms competência | Toast *"Ficha processada: X fichas, Y linhas"* | `Importacao/index.tsx:308-310` — `toast.success(\`Ficha processada: ${resultado.totalFichas} fichas, ${resultado.totalLinhas} linhas\`)` | ⚠️ Spec-precision gap (manual/lint gate — AD-004) |

**Status**: ✅ Backend ACs covered by automated tests; ⚠️ 10 spec-precision gaps (8 FE per AD-004 matrix + 2 HTTP DTO/controller without WebMvcTest). No FAIL on spec-defined outcomes in implementation.

---

## Edge Cases

| Edge case | Spec-defined outcome | Evidence | Result |
| --------- | -------------------- | -------- | ------ |
| Competência sem linhas CLT processáveis | `totalFichas=0`, `success=true` if import OK | `ImportacaoFolhaAdpServiceTest.java:186-201` — `assertEquals(0, result.processamento().totalFichas())` + non-null result | ✅ PASS |
| Timeout importação grande (5 min) | Client keeps extended timeout; sync op | `importacaoService.ts:15` — `timeout: 300000` | ⚠️ Spec-precision gap (impl only) |
| Reprocesso manual sem import ADP | API processa vazio/zero; UI shows totals | `folhaPagamentoService.ts:305-317`; `Importacao/index.tsx:308-310` (displays API totals) | ⚠️ Spec-precision gap (manual/lint) |
| Substituição confirmada (409→confirm) encadeia process | After confirm, persist + process same TX | `ImportacaoFolhaAdpServiceTest.java:114-128` — `verify(folhaProcessamentoPort).processar(...)` with `substituirExistente=true` | ✅ PASS |

---

## Discrimination Sensor

Scratch mutations applied via temp copy → run targeted tests → restore original file.

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| M1 | `ImportacaoFolhaAdpService.java:260` | Skip `folhaProcessamentoPort.processar`, return hardcoded `(0,0,0)` | ✅ Killed — `ImportacaoFolhaAdpServiceTest` fails verify on `processar` |
| M2 | `ImportacaoFolhaAdpService.java:261` | Pass `recalcularFerias=true` instead of `false` | ✅ Killed — `importar_happyPath_chamaPersistirImportacaoEProcessamento` verify fails |
| M3 | `FolhaProcessamentoAdapter.java:29` | Flip `ProcessamentoOpcoes(recalcularFerias)` → `ProcessamentoOpcoes(!recalcularFerias)` | ✅ Killed — `FolhaProcessamentoAdapterTest` verify fails |

**Sensor depth**: lightweight (3 behavior-level faults)  
**Result**: 3/3 killed — ✅ PASS

---

## Gate Check

- **Gate command**: `cd backend && mvn test && cd ../frontend && npm run lint && npm run build`
- **Backend**: 238 passed, 0 failed, 0 skipped
- **Frontend lint**: 0 errors, 8 warnings (pre-existing `react-hooks/exhaustive-deps` in unrelated files)
- **Frontend build**: success (`tsc -b && vite build`)
- **Feature test delta**: `ImportacaoFolhaAdpServiceTest` 4 → 7 (+3); new `FolhaProcessamentoAdapterTest` (+2); no tests deleted
- **Regressions exercised**: `SecurityConfigFolhaProcessamentoTest`, `ModularArchitectureTest`, `FolhaProcessamentoServiceTest` — all green in full gate

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ |
| No scope creep | ✅ |
| Matches existing patterns (port/adapter, AdminRoute, toast UX) | ✅ |
| Spec-anchored outcome check (backend tests match spec values) | ✅ |
| Per-layer coverage per Test Coverage Matrix (`tasks.md`) | ✅ (FE gap documented AD-004) |
| Guidelines: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` | ✅ |

---

## Requirement Traceability Update

| Requirement | Previous Status | New Status |
| ----------- | --------------- | ---------- |
| FIX1-01 … FIX1-05 | Done | ✅ Verified (automated) |
| FIX1-06 … FIX1-13 | Done | ✅ Verified (manual/lint gate + impl evidence) |
| Edge cases (4) | — | ✅ 2 automated + 2 manual/lint |

---

## Summary

**Overall**: ✅ PASS

**Spec-anchored check**: 13/13 ACs matched spec-defined outcomes in implementation; 10 spec-precision gaps (expected FE AD-004 + HTTP mapping without WebMvcTest)  
**Sensor**: 3/3 mutations killed  
**Gate**: 238 backend + lint/build passed, 0 failed

**What works**: ADP import chains ficha processing in one TX; service tests cover encadeamento, rollback propagation, zero-fichas, substitution; security regression on `/processar` ADMIN; FE implements composite UX and manual reprocess section per spec.

**Residual gaps (non-blocking)**: No automated Vitest/Playwright for FIX1-06…13; no WebMvcTest asserting FIX1-02/03 HTTP response bodies; rollback not asserted via integration test (relies on `@Transactional` + exception propagation).

**Next steps**: Optional follow-up — add `ImportacaoFolhaAdpControllerWebMvcTest` for FIX1-02/03 HTTP contract; add Vitest for Importacao page when AD-004 FE test policy expands.
