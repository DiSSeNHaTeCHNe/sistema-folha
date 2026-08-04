# relatorios-executivos-fix1 Validation

## Status atual

| Campo | Valor |
| ----- | ----- |
| **Verdict** | FAIL (gate + sensor PASS; AC evidence gaps on FIX1-02 partial, FIX1-04, FIX1-22) |
| **Spec slug** | `relatorios-executivos-fix1` |
| **HEAD** | `2e1b812` (`fix1(relatorios): mapeamento BYTEA explícito relatorio_arquivo`) |
| **Commit range** | `2fc9f12..2e1b812` (6 fix1 commits) |
| **Open gaps** | FIX1-04 (P1 logs untested), FIX1-22 (P2 BYTEA mapping untested), FIX1-02 partial (`dataProcessamento` on ERRO not asserted) |

---

## Execution — relatorios-executivos-fix1 — 2026-08-03

**Slug:** relatorios-executivos-fix1  
**Date:** 2026-08-03  
**Commit range:** `2fc9f12..2e1b812`  
**Verifier:** independent sub-agent (author ≠ verifier)  
**Overall:** FAIL (ressalvas: build gate verde, sensor 5/5 killed)

### Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | Stale detector + tracker + config |
| T2 | ✅ Done | Worker terminal states |
| T3 | ✅ Done | Stale recovery service |
| T4 | ✅ Done | Service integration, DTOs, WebMvc 429 |
| T5 | ✅ Done | FE errors, stale retry, Vitest |
| T6 | ✅ Done | BYTEA mapping + persist test |

### Gate Check

| Metric | Result |
| ------ | ------ |
| **Command** | Full fix1 gate from `tasks.md` |
| **Backend** | 54 passed, 0 failed, 0 skipped |
| **Frontend** | 30 passed, 0 failed |
| **Build** | `npm run build` — SUCCESS |
| **Total** | **84 passed, 0 failed** |

### Spec-Anchored Acceptance Criteria

| AC | Criterion (WHEN → THEN) | Spec-defined outcome | `file:line` + assertion | Result |
| -- | ----------------------- | -------------------- | ----------------------- | ------ |
| FIX1-01 | Worker success → terminal PROCESSADO + blob | `status=PROCESSADO`, `dataProcessamento` set, row in `relatorio_arquivo` | `RelatorioGeracaoWorkerTest.java:112` — `assertEquals(RelatorioStatus.PROCESSADO, salvo.getStatus())`; `:114` — `assertTrue(salvo.getDataProcessamento() != null)`; `:117-119` — `verify(relatorioArquivoRepository).save(...)` + `assertEquals(pdf, ...getPdfBytes())` | ✅ PASS |
| FIX1-02 | Render/persist fail → ERRO + truncated msg + `dataProcessamento` | `status=ERRO`, generic msg ≤500 chars, `dataProcessamento` filled | `RelatorioGeracaoWorkerTest.java:134-135` — `assertEquals(ERRO)` + `assertEquals("Erro ao gerar relatório", ...)`; `:183-185` — `truncarErro` length 500 | ⚠️ PARTIAL — `dataProcessamento` on ERRO path not asserted |
| FIX1-03 | `findById` null / `ativo=false`+PENDENTE → no orphan PENDENTE | No save for missing id; inactive PENDENTE → ERRO | `RelatorioGeracaoWorkerTest.java:164` — `verify(relatorioRepository, never()).save(any())`; `:177-178` — `assertEquals(ERRO)` + `"Relatório indisponível"` | ✅ PASS |
| FIX1-04 | Worker start/finish structured logs | INFO at start; INFO/WARN/ERROR at finish with `relatorioId`, `login` | — | ❌ GAP — logs implemented in `RelatorioGeracaoWorker.java:72-98` but no test captures/asserts log output |
| FIX1-05 | PENDENTE without blob, age > timeout+grace → stale | Threshold 180s (60+120); null `data_criacao` → stale | `RelatorioStaleDetectorTest.java:38` — `assertEquals(Duration.ofSeconds(180), ...)`; `:44` — `assertTrue(detector.isStale(...))`; `:56` — null creation → stale; `:62` — blob → non-stale | ✅ PASS |
| FIX1-06 | Stale detected in listar/gerar → reenqueue once | `markAttempted` + worker enqueue on first detection | `RelatorioStaleRecoveryServiceTest.java:82-83` — `verify(recoveryTracker).markAttempted(10L)` + `verify(enqueueFn).accept(10L)`; `RelatorioGeracaoServiceTest.java:309` — `verify(staleRecoveryService).recuperarParaUsuario(1L)` | ✅ PASS |
| FIX1-07 | Second stale detection → ERRO exact message | `status=ERRO`, `erro="Tempo esgotado na geração"` | `RelatorioStaleRecoveryServiceTest.java:98-99` — `assertEquals(ERRO, ...)` + `assertEquals(ERRO_TEMPO_ESGOTADO, ...getErro())` | ✅ PASS |
| FIX1-08 | ERRO stale jobs excluded from 429 count | Stale / promoted ERRO not counted as active pending | `RelatorioStaleRecoveryServiceTest.java:143` — `assertEquals(1L, count)` (1 recent of 2); `:155` — `assertEquals(0L, count)` when only stale | ✅ PASS |
| FIX1-09 | <3 non-stale PENDENTE → POST not 429 | 200/202/PENDENTE allowed | `RelatorioGeracaoServiceTest.java:179-182` — `gerarFolha` returns DTO with `PENDENTE`, no `RelatorioGeracaoLimiteException`; `contarPendentesAtivos` mocked 0 | ✅ PASS |
| FIX1-10 | ≥3 non-stale PENDENTE → 429 + limit message | HTTP 429, message about simultaneous limit | `RelatorioGeracaoServiceTest.java:157-158` — `assertThrows(RelatorioGeracaoLimiteException.class, ...)`; `RelatorioFolhaControllerWebMvcTest.java:216-218` — `status().isTooManyRequests()` + `jsonPath("$.message").value("Limite de 3 gerações simultâneas por usuário atingido")` | ✅ PASS |
| FIX1-11 | POST with stale job → recovery before 429 | Recovery runs before limit check | `RelatorioGeracaoServiceTest.java:182` — `verify(staleRecoveryService).recuperarParaUsuario(1L)` before successful generate with `contarPendentesAtivos` 0 | ✅ PASS |
| FIX1-12 | Re-POST non-stale PENDENTE → reenqueue worker | Worker `processar` invoked again | `RelatorioGeracaoServiceTest.java:205` — `verify(relatorioGeracaoWorker).processar(10L)` | ✅ PASS |
| FIX1-13 | POST axios timeout ≥65000ms | `RELATORIO_GERACAO_TIMEOUT_MS >= 65000` | `Relatorios.test.tsx:308` — `expect(RELATORIO_GERACAO_TIMEOUT_MS).toBeGreaterThanOrEqual(65_000)` | ✅ PASS |
| FIX1-14 | API 429 → explicit limit toast | Not generic "Erro ao gerar relatório" | `Relatorios.test.tsx:270-273` — `toHaveBeenCalledWith('Limite de 3 gerações simultâneas por usuário atingido', 'error')` | ✅ PASS |
| FIX1-15 | API 403 → access denied toast | Explicit denied message | `Relatorios.test.tsx:286` — `toHaveBeenCalledWith('Acesso negado', 'error')` | ✅ PASS |
| FIX1-16 | Client timeout → timeout toast + polling hint | Message mentions tempo esgotado | `Relatorios.test.tsx:300-303` — `expect.stringContaining('Tempo esgotado na requisição')` | ✅ PASS |
| FIX1-17 | PENDENTE stale → enabled "Tentar novamente" | Retry button, not blocked "Gerando…" | `Relatorios.test.tsx:315-317` — `findByRole('button', { name: 'Tentar novamente Executivo de Folha' })` + click triggers `gerarRelatorioFolha` | ✅ PASS |
| FIX1-18 | PENDENTE non-stale → progress, disabled | Disabled progress indicator | `Relatorios.test.tsx:189` — `getByRole('button', { name: /aguardando processamento/i }).toBeDisabled()` | ✅ PASS |
| FIX1-19 | GET list scoped to authenticated user | Backend filters by `usuarioId` (no tenant-wide leak) | `RelatorioGeracaoServiceTest.java:310` — `verify(relatorioRepository).findByUsuarioIdAndTipoAndAtivoTrueOrderByAnoDescMesDesc(1L, RelatorioTipo.FOLHA)` | ✅ PASS |
| FIX1-20 | Card resolves report for logged-in user only | Status/actions from user's record for `(mes, ano)` | `RelatorioGeracaoServiceTest.java:305-310` — list scoped to user 1L; FE `Relatorios.test.tsx:164-168` — generate uses matched competencia from user's list | ✅ PASS |
| FIX1-21 | No user report for competencia → "Gerar" state | Generate button even if other users have reports | `Relatorios.test.tsx:336` — after picker change to Mar/2025, `findByRole('button', { name: 'Gerar Executivo de Folha' })` | ✅ PASS |
| FIX1-22 | dev profile: `pdfBytes` maps BYTEA without OID | `@JdbcTypeCode(VARBINARY)` + `columnDefinition=bytea`, no `@Lob` | — (implementation: `RelatorioArquivo.java:27-28`) | ❌ GAP — no automated assertion on entity mapping (compile-only) |
| FIX1-23 | Worker persist PDF → `relatorio_arquivo` insert succeeds | `save` with PDF bytes | `RelatorioGeracaoWorkerTest.java:117-119` — `verify(relatorioArquivoRepository).save(...)` + `assertEquals(pdf, ...getPdfBytes())` | ✅ PASS |

**AC summary:** 20 PASS, 2 GAP, 1 PARTIAL (21/23 with evidence; 2 without `file:line` assertion)

### Discrimination Sensor

| Mutation | File | Description | Killed? |
| -------- | ---- | ----------- | ------- |
| M1 | `RelatorioStaleDetector.java:35` | Flip `isAfter(limite)` → `isBefore(limite)` | ✅ Killed (`RelatorioStaleDetectorTest`) |
| M2 | `RelatorioGeracaoConstants.java:5` | Change `ERRO_TEMPO_ESGOTADO` text | ✅ Killed (`RelatorioStaleRecoveryServiceTest`) |
| M3 | `RelatorioGeracaoWorker.java` | Comment out `setStatus(PROCESSADO)` on success | ✅ Killed (`RelatorioGeracaoWorkerTest#processar_sucesso_...`) |
| M4 | `relatorioService.ts:4` | `RELATORIO_GERACAO_TIMEOUT_MS` 65000 → 10000 | ✅ Killed (`Relatorios.test.tsx` timeout constant test) |
| M5 | `RelatorioStaleRecoveryService.java` | Disable stale skip in `contarPendentesAtivos` | ✅ Killed (`contarPendentesAtivos_excluiStale`) |

**Sensor depth:** lightweight (5 behavior-level faults)  
**Result:** 5/5 killed — PASS ✅  
**Scratch state:** mutations applied in working tree, tests run, restored via `git checkout` (no persistent code changes)

### Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ |
| Matches existing patterns | ✅ |
| Spec-anchored outcome check | ⚠️ 2 AC gaps, 1 partial |
| Per-layer coverage (domain 1:1, routes happy+edge+error) | ✅ for stale/429/worker/FE |
| Documented guidelines | `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` |

### Edge Cases (spec)

| Edge case | Evidence |
| --------- | -------- |
| Upsert on re-generate (no duplicate tuple) | `RelatorioGeracaoServiceTest.java:219-237` — reuses existing record, sets PENDENTE |
| Stale → ERRO → download 409 | `RelatorioFolhaControllerWebMvcTest.java:106-112` — PENDENTE download → 409 |
| `data_criacao` null → stale immediate | `RelatorioStaleDetectorTest.java:56` |
| Stale promoted → slot freed (no 429) | `RelatorioGeracaoServiceTest.java:162-182` + `RelatorioStaleRecoveryServiceTest.java:146-155` |
| FE no poll when stale | `Relatorios.test.tsx:320-328` |

### Ranked Gaps

| # | AC | Severity | Fix needed |
| - | -- | -------- | ---------- |
| 1 | FIX1-04 | Major | Add worker test with log appender/list: assert INFO "Iniciando processamento" at start and INFO/WARN/ERROR at terminal paths |
| 2 | FIX1-22 | Minor (P2) | Add entity/reflection or `@DataJpaTest` asserting `@Lob` absent and BYTEA mapping on `RelatorioArquivo.pdfBytes` |
| 3 | FIX1-02 | Minor | Extend `processar_falhaRender_marcaErroTruncado` (and inativo ERRO test) to `assertNotNull(salvo.getDataProcessamento())` |

### Summary

**Gate:** 84/84 tests passed, build SUCCESS  
**Sensor:** 5/5 killed  
**What works:** Stale detection/recovery, 429 limit with stale exclusion, worker terminal states, FE timeout/errors/retry, user-scoped listing, PDF persist path  
**Blockers:** P1 observability AC (FIX1-04) lacks test evidence per evidence-or-zero  
**Next steps:** Add 3 targeted tests above → re-verify (iteration 1/3)
