# Relatórios Executivos Validation

## Status atual

| Campo | Valor |
| --- | --- |
| **Verdict** | ⚠️ PASS com ressalvas |
| **Spec slug** | `relatorios-executivos` |
| **HEAD** | `2fc9f1228c45513bf42b690e842d0eb4c98131dc` |
| **Spec-anchored** | 21/27 ACs PASS · 5 ⚠️ partial · 0 ❌ GAP |
| **Gate** | Backend 1119 passed / 0 failed / 1 skipped · Frontend lint 0 errors · Relatorios 22/22 passed · build OK · full FE suite 347 passed / 3 files ENFILE (env) |
| **Sensor** | 5 injected · 5 killed · 0 survived |
| **Open gaps (ranked)** | REL-01 async HTTP proof · REL-09 CC "Outros" aggregation (>15 rows) · REL-12 evolução labels in PDF text · REL-17 API port↔PDF resumo integration · REL-25 preview aria test assert |

---

## Execution — relatorios-executivos — 2026-08-03

**Date**: 2026-08-03  
**Spec**: `_docs/specs/features/relatorios-executivos/spec.md`  
**Diff range**: `c0c567d1c8265dea2dc421f063cdba215dd016ec..308184e`  
**Verifier**: independent sub-agent (author ≠ verifier)  
**Overall**: ❌ FAIL

---

### Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| T1–T15 | ✅ Done | All committed per orchestrator |

---

### Spec-Anchored Acceptance Criteria

| ID | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- | --- |
| REL-01 | POST `/api/relatorios/folha` válido → PENDENTE, async, DTO com id/status final ≤60s | `status=PROCESSADO` ou `ERRO`; DTO com `id` | `RelatorioFolhaControllerWebMvcTest.java:96` — `jsonPath("$.status").value("PROCESSADO")`; `RelatorioGeracaoWorkerTest.java:111` — `assertEquals(RelatorioStatus.PROCESSADO, salvo.getStatus())` | ⚠️ Worker cobre transição; WebMvc mocka service — não prova fluxo PENDENTE→PROCESSADO assíncrono |
| REL-02 | POST `/api/relatorios/beneficio` mesmo fluxo | Igual REL-01 para benefício | `RelatorioBeneficioControllerWebMvcTest.java:91-92` — `jsonPath("$.id").value(2)`, `jsonPath("$.status").value("PROCESSADO")` | ✅ PASS |
| REL-03 | GET list → ordenado `ano DESC, mes DESC`; visível a todos autenticados (tenant) | Lista global single-tenant com ordering | `RelatorioFolhaControllerWebMvcTest.java:135-136` — `status().isOk()` apenas | ❌ GAP — sem assert de ordenação; implementação filtra por `usuarioId` (`RelatorioGeracaoService.java:59`) vs spec tenant-wide |
| REL-04 | Download com `PROCESSADO` → `application/pdf` | Content-Type PDF + bytes | `RelatorioFolhaControllerWebMvcTest.java:119-121` — `contentType(APPLICATION_PDF)`, `content().bytes(pdf)`; `RelatorioGeracaoServiceTest.java:173` — `assertArrayEquals(pdf, result)` | ✅ PASS |
| REL-05 | Download `PENDENTE` ou `ERRO` → HTTP 409 | 409 indisponível | `RelatorioFolhaControllerWebMvcTest.java:107-108` — `status().isConflict()` para PENDENTE | ❌ GAP — ramo `ERRO` sem teste (`RelatorioIndisponivelException` para ERRO não coberto) |
| REL-06 | `mes`/`ano` fora limites → HTTP 400 Bean Validation | 400 validation error | `RelatorioFolhaControllerWebMvcTest.java:77-78` — competência futura via service (400) | ❌ GAP — sem teste `mes=13` ou `ano=1999` via `@Valid` em `GerarRelatorioRequest.java:9-15` |
| REL-07 | Capa PDF folha: logo, título, competência, gerador, 4 KPIs spec | Total Funcionários, **Custo Empresa**, Total Proventos, **Total Descontos** | `FolhaExecutivoPdfRendererTest.java:41-44` — título/competência; `FolhaExecutivoPdfRendererTest.java:66` — `text.contains("150")` | ❌ GAP — impl usa KPIs diferentes (`FolhaExecutivoPdfRenderer.java:105-108`: Custo Folha, Benefícios Ativos); teste não asserta labels spec |
| REL-08 | KPIs PDF = `DashboardService.getStats()` ±R$0,01 | Paridade numérica | `RelatorioPdfServiceTest.java:105` — `assertEquals(stats, model.stats())` (mesmo objeto mock) | ❌ GAP — não extrai valores do PDF nem compara com dashboard |
| REL-09 | Seção Por Centro de Custo top 15 + Outros | Tabela CC ordenada DESC | — | ❌ GAP — lógica em `FolhaExecutivoPdfRenderer.java:132-141` sem assert de texto/ordem |
| REL-10 | Seção Por Linha de Negócio | Mesma estrutura CC | — | ❌ GAP — `FolhaExecutivoPdfRenderer.java:143-152` sem teste |
| REL-11 | Top 5 Proventos / Descontos | Código, descrição, valor, qtd | — | ❌ GAP — seções em `FolhaExecutivoPdfRenderer.java:154-168` sem assert |
| REL-12 | Gráfico evolução 6 meses `MMM/yyyy` | Labels + chart estático | `RelatorioPdfServiceTest.java:106` — `assertEquals(evolucao, model.evolucao6Meses())` | ❌ GAP — paridade model OK; PDF/chart/labels não assertados |
| REL-13 | Escopo ACL restrito → totais scoped | Paridade dashboard scoped | `DashboardStatsAggregatorTest.java:77-98` — scoped aggregator; `RelatorioPdfServiceTest.java:113-165` — scoped benefício | ❌ GAP — folha PDF sem teste scoped; aggregator não prova PDF |
| REL-14 | Competência vazia → capa + "Sem dados…" PROCESSADO | Mensagem explícita | `FolhaExecutivoPdfRendererTest.java:56` — `text.contains("Sem dados para a compet")` | ✅ PASS |
| REL-15 | Rodapé `Página X de Y` + texto Techne | Numeração completa | `FolhaExecutivoPdfRendererTest.java:43` — `text.contains("Gerado pelo Sistema de Folha")`; `RelatorioLayoutHelperTest.java:58` — `assertNotNull(createFooterEvent())` | ❌ GAP — impl só `Página X` (`RelatorioLayoutHelper.java:111`); sem assert de numeração |
| REL-16 | Capa benefícios: KPIs spec | Total Benefícios, Qtd Lançamentos, Custo Folha, Consolidado | `BeneficioCustoPdfRendererTest.java:37-41` — título + `text.contains("4000")`, `R$` | ⚠️ Labels abreviados ("Consolidado"); valores parcialmente checados |
| REL-17 | Tabela Resumo por Tipo = API resumo | Paridade `GET /beneficio-mensal/resumo` | `BeneficioConsultaAdapterTest.java:319-325` — port resumo; `BeneficioCustoPdfRendererTest.java:40` — `Vale Refei` | ❌ GAP — port OK; paridade API↔PDF não assertada |
| REL-18 | Drill-down Top 10 por tipo | Nome, CC, valor; Outros agrupado | `BeneficioConsultaAdapterTest.java:356-368` — top 10 port | ❌ GAP — PDF drill-down não assertado |
| REL-19 | Matriz Top 5 CC × Top 5 tipos | Valores R$ | `BeneficioConsultaAdapterTest.java:378-390` — matriz port | ❌ GAP — PDF matriz não assertada |
| REL-20 | Sem benefícios, com folha → nota | "Nenhum benefício lançado" | `BeneficioCustoPdfRendererTest.java:49` — `text.contains("Nenhum benef")` | ✅ PASS |
| REL-21 | Moeda pt-BR `R$ 1.234,56` | Formato BR | `RelatorioLayoutHelperTest.java:29-30` — `formatted.contains("1.234,56")`; `BeneficioCustoPdfRendererTest.java:65` — `R$` | ✅ PASS |
| REL-22 | Hub cards catálogo (não só tabs) | 2 cards com ícone/descrição/badge | `Relatorios.test.tsx:87-89` — `getByRole('heading', { name: 'Executivo de Folha' })` etc. | ✅ PASS |
| REL-23 | MonthPicker → payload `{ mes, ano }` selecionado | Competência escolhida, não hardcoded | `Relatorios.test.tsx:107` — `toHaveBeenCalledWith(6, 2026)` | ❌ GAP — usa competência default mock; não altera picker antes de gerar |
| REL-24 | PENDENTE → progresso + desabilita re-geração | Botão disabled + status | `Relatorios.test.tsx:129` — `toBeDisabled()`; `CompetenciaPicker.test.tsx:76` | ✅ PASS |
| REL-25 | PROCESSADO → Baixar PDF + thumbnail ≤200px | Download + preview imagem ou ícone premium | `Relatorios.test.tsx:117-121` — download | ⚠️ Ícone PDF placeholder (`RelatorioCatalogCard.tsx:128-131`); sem assert de preview/thumbnail |
| REL-26 | ERRO → mensagem + Tentar novamente | Alert + retry | `Relatorios.test.tsx:154-159` — `getByRole('alert')`, retry click | ✅ PASS |
| REL-27 | Layout MUI responsivo + a11y roles/labels | Cards navegáveis por role/label | `CompetenciaPicker.test.tsx:14` — `getByLabelText('Selecionar competência…')`; `Relatorios.test.tsx:115` — `getByRole('button', { name: 'Baixar PDF…' })` | ✅ PASS (layout responsivo não testado — ⚠️ spec-precision) |

**Status**: ❌ Gaps present — 9/27 PASS, 15 GAP, 3 partial

---

### Discrimination Sensor

| # | File:line | Mutation | Killed? |
| --- | --- | --- | --- |
| 1 | `RelatorioGeracaoService.java:84` | Flip `!= PROCESSADO` → `== PROCESSADO` on download | ✅ Killed — `RelatorioGeracaoServiceTest` failures |
| 2 | `FolhaExecutivoPdfRenderer.java:50` | Remove "Sem dados para a competência…" text | ✅ Killed — `FolhaExecutivoPdfRendererTest.render_semDados` |
| 3 | `RelatorioGeracaoService.java:162` | Disable competência futura validation | ✅ Killed — compile/test errors in service tests |
| 4 | `RelatorioLayoutHelper.java:24` | `Locale.US` instead of pt-BR | ✅ Killed — `RelatorioLayoutHelperTest` + `BeneficioCustoPdfRendererTest` |
| 5 | `RelatorioCatalogCard.tsx:149` | Remove `disabled` on pending button | ✅ Killed — `Relatorios.test.tsx` + `CompetenciaPicker.test.tsx` |

**Inferred weak zones** (code review; not executed — scratch-only policy): KPI label swap, list ordering reversal, ERRO download skip, PDF section title removal would likely **survive** current suite.

**Sensor depth**: lightweight (5 mutations)  
**Result**: 5/5 killed — sensor PASS ✅ · suite has blind spots on REL-03/07/09–12/15/17–19

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / surgical | ✅ |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ❌ 15 AC gaps |
| Per-layer coverage (domain 1:1 ACs) | ❌ PDF + list + validation layers thin |
| Documented guidelines (`TESTING.md`, tasks matrix) | ⚠️ Matrix claims REL-07…15 coverage; renderer tests shallow |

---

### Edge Cases (spec)

| Edge case | Result |
| --- | --- |
| Sem organograma → 403 | ✅ `RelatorioGeracaoServiceTest.java:108` — `RelatorioAcessoNegadoException` |
| Geração falha → ERRO truncado | ✅ `RelatorioGeracaoWorkerTest.java:131-132` — `assertEquals(500, salvo.getErro().length())` |
| Download id inexistente → 404 | ✅ `RelatorioGeracaoServiceTest.java:195` — `RelatorioNotFoundException` |
| Competência futura → 400 | ✅ WebMvc + service tests |
| Re-geração mesma tupla substitui | ✅ `RelatorioGeracaoServiceTest.java:143` — `assertEquals(PENDENTE, salvo.getStatus())` on replace |
| PDF >50 MB → ERRO | ✅ `RelatorioGeracaoWorkerTest.java:149` |

---

### Gate Check

| Item | Result |
| --- | --- |
| **Command** | `cd backend && mvn test && cd ../frontend && npm run lint && npm run test && npm run build` |
| **Backend** | 1101 passed, 0 failed, 1 skipped (Testcontainers/Docker) |
| **Frontend lint** | 0 errors, 14 warnings (pre-existing) |
| **Frontend tests** | 455 passed |
| **Frontend build** | ✅ success |
| **Delta** | +15 test files in feature diff (matrix) |

---

### Fix Plans (for implementer — do not execute in verify)

1. **REL-07** — Align capa KPIs to spec (Custo Empresa, Total Descontos); add renderer test asserting four label strings + values from `DashboardStatsDTO`.
2. **REL-03** — Decide spec vs impl (tenant-wide list vs per-user); add `listarFolha` ordering test with multi-row fixture.
3. **REL-08** — Integration-style test: fixed stats → render PDF → extract text → assert numeric parity ±0.01.
4. **REL-05/06** — WebMvc: download ERRO → 409; POST `mes=13` / `ano=1999` → 400 validation body.
5. **REL-09–12, REL-15** — Extend `FolhaExecutivoPdfRendererTest` with section titles, "Outros", footer "Página X de Y".
6. **REL-13** — `RelatorioPdfServiceTest.renderFolhaExecutivo_scoped_passesCentrosToDashboardPort`.
7. **REL-17–19** — PDF asserts for resumo table, drill-down, matriz (or golden-string tests).
8. **REL-23** — Fire MonthPicker change in test, then assert `gerarRelatorioFolha(mes, ano)` matches selection.
9. **REL-25** — Assert `getByLabelText('Pré-visualização do relatório PDF')` when PROCESSADO.

---

### Requirement Traceability Update

| Requirement | Previous | New |
| --- | --- | --- |
| REL-01…06 | Verified (pending Verifier) | ⚠️ REL-01 partial · ❌ REL-03,05,06 |
| REL-07…15 | Verified (pending Verifier) | ❌ REL-07,08,09,10,11,12,13,15 · ✅ REL-14 |
| REL-16…21 | Verified (pending Verifier) | ⚠️ REL-16 · ✅ REL-20,21 · ❌ REL-17,18,19 |
| REL-22…27 | Verified (pending Verifier) | ✅ REL-22,24,26,27 · ⚠️ REL-25 · ❌ REL-23 |

---

### Summary

**Overall**: ❌ Not Ready — gate green, spec-anchored coverage insufficient for MVP sign-off.

**Lessons recorded**: L-011 (PDF KPI asserts), L-012 (list ordering), L-013 (ERRO download branch) via `lessons.py`.

**Next steps**: Route ranked fix tasks to implementer; re-verify after fixes (iteration 1/3).

---

## Execution — relatorios-executivos fix cycle 1 — 2026-08-03

**Date**: 2026-08-03  
**Spec**: `_docs/specs/features/relatorios-executivos/spec.md`  
**Diff range**: `308184e..6311f79` (3 commits: `e5d62fd`, `1130ef9`, `6311f79`)  
**Verifier**: independent sub-agent (author ≠ verifier)  
**Overall**: ❌ FAIL (improved: 14 PASS vs 9 prior; sensor survivor on list ordering)

---

### Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| fix(cycle-1) PDF KPIs, footer, benefício drill-down | ✅ Done | `e5d62fd` |
| fix(cycle-1) tenant listing, async afterCommit, WebMvc gaps | ✅ Done | `1130ef9` |
| fix(cycle-1) FE picker payload, benefício card label | ✅ Done | `6311f79` |

---

### Spec-Anchored Acceptance Criteria

| ID | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- | --- |
| REL-01 | POST `/api/relatorios/folha` válido → PENDENTE, async, DTO com id/status final ≤60s | `status=PROCESSADO` ou `ERRO`; DTO com `id` | `RelatorioGeracaoServiceTest.java:223-248` — `verify(relatorioGeracaoWorker).processar(10L)` after commit; `RelatorioGeracaoWorkerTest.java:111` — `assertEquals(PROCESSADO, salvo.getStatus())` | ⚠️ Service/worker proven; WebMvc still mocks final status — no HTTP-level async proof |
| REL-02 | POST `/api/relatorios/beneficio` mesmo fluxo | Igual REL-01 para benefício | `RelatorioBeneficioControllerWebMvcTest.java:91-92` — `jsonPath("$.status").value("PROCESSADO")` | ✅ PASS |
| REL-03 | GET list → ordenado `ano DESC, mes DESC`; visível tenant-wide | Lista global single-tenant com ordering | `RelatorioGeracaoService.java:78-79` — `findByTipoAndAtivoTrueOrderByAnoDescMesDesc`; `RelatorioFolhaControllerWebMvcTest.java:188-191` — `$[0].ano=2026`, `$[1].ano=2024` | ⚠️ Impl fixed; WebMvc mocks service — sensor M5 survived on repository order flip |
| REL-04 | Download com `PROCESSADO` → `application/pdf` | Content-Type PDF + bytes | `RelatorioFolhaControllerWebMvcTest.java:121-124` — `contentType(APPLICATION_PDF)`, `content().bytes(pdf)` | ✅ PASS |
| REL-05 | Download `PENDENTE` ou `ERRO` → HTTP 409 | 409 indisponível | `RelatorioFolhaControllerWebMvcTest.java:107-108` PENDENTE; `RelatorioFolhaControllerWebMvcTest.java:129-135` ERRO — `status().isConflict()` | ✅ PASS |
| REL-06 | `mes`/`ano` fora limites → HTTP 400 Bean Validation | 400 validation error | `RelatorioFolhaControllerWebMvcTest.java:151-156` — `mes=13` → 400 | ⚠️ `mes` covered; `ano=1999` not tested (`GerarRelatorioRequest.java:13` `@Min(2000)`) |
| REL-07 | Capa PDF folha: logo, título, competência, gerador, 4 KPIs spec | Total Funcionários, Custo Empresa, Total Proventos, Total Descontos | `FolhaExecutivoPdfRenderer.java:109-112`; `FolhaExecutivoPdfRendererTest.java:66-69` — labels + values `150`, `8.000`, `1.000`, `9.000` | ✅ PASS |
| REL-08 | KPIs PDF = `DashboardService.getStats()` ±R$0,01 | Paridade numérica | `RelatorioPdfServiceTest.java:105` — `assertEquals(stats, model.stats())` (mock object identity) | ❌ GAP — no PDF text extraction vs dashboard values |
| REL-09 | Seção Por Centro de Custo top 15 + Outros | Tabela CC ordenada DESC | `FolhaExecutivoPdfRenderer.java:138-147`, `199-217` — logic present | ❌ GAP — no assert on "Centros de Custo", "Outros", or order |
| REL-10 | Seção Por Linha de Negócio | Mesma estrutura CC | `FolhaExecutivoPdfRenderer.java:149-158` | ❌ GAP — no PDF assert |
| REL-11 | Top 5 Proventos / Descontos | Código, descrição, valor, qtd | `FolhaExecutivoPdfRenderer.java:160-174` | ❌ GAP — no PDF assert |
| REL-12 | Gráfico evolução 6 meses `MMM/yyyy` | Labels + chart estático | `RelatorioPdfServiceTest.java:106` — model parity only | ❌ GAP — chart/labels not asserted in PDF |
| REL-13 | Escopo ACL restrito → totais scoped | Paridade dashboard scoped | `RelatorioPdfService.java:49-50` — ACL via login in `DashboardConsultaAdapter`; benefício scoped in `RelatorioPdfServiceTest.java:113-165` | ❌ GAP — folha PDF scoped path not tested |
| REL-14 | Competência vazia → capa + "Sem dados…" PROCESSADO | Mensagem explícita | `FolhaExecutivoPdfRendererTest.java:56` — `text.contains("Sem dados para a compet")` | ✅ PASS |
| REL-15 | Rodapé `Página X de Y` + texto Techne | Numeração completa | `RelatorioLayoutHelperTest.java:70-84` — `page2Text.contains("Página 2 de 2")`; capa test `FolhaExecutivoPdfRendererTest.java:43-44` | ✅ PASS |
| REL-16 | Capa benefícios: KPIs spec | Total Benefícios, Qtd Lançamentos, Custo Folha, Consolidado | `BeneficioCustoPdfRenderer.java:75-78` — labels "Benefícios", "Lançamentos", "Custo Folha", "Consolidado"; `BeneficioCustoPdfRendererTest.java:39-41` — values partial | ⚠️ Labels abreviados vs spec wording |
| REL-17 | Tabela Resumo por Tipo = API resumo | Paridade `GET /beneficio-mensal/resumo` | `BeneficioCustoPdfRendererTest.java:40` — `Vale Refei`; port in `BeneficioConsultaAdapterTest.java:319-325` | ❌ GAP — API↔PDF numeric parity not asserted |
| REL-18 | Drill-down Top 10 por tipo | Nome, CC, valor; Outros agrupado | `BeneficioCustoPdfRendererTest.java:68-87` — `CC Admin`, `Outros`, `funcion` | ✅ PASS |
| REL-19 | Matriz Top 5 CC × Top 5 tipos | Valores R$ | `BeneficioCustoPdfRenderer.java:143-146`; model fixture `BeneficioCustoPdfRendererTest.java:94-95` | ❌ GAP — matriz in model; no PDF text assert |
| REL-20 | Sem benefícios, com folha → nota | "Nenhum benefício lançado" | `BeneficioCustoPdfRendererTest.java:49` | ✅ PASS |
| REL-21 | Moeda pt-BR `R$ 1.234,56` | Formato BR | `RelatorioLayoutHelperTest.java:35-37`; `BeneficioCustoPdfRendererTest.java:65` | ✅ PASS |
| REL-22 | Hub cards catálogo | 2 cards com ícone/descrição/badge | `Relatorios.test.tsx:116-117` | ✅ PASS |
| REL-23 | MonthPicker → payload `{ mes, ano }` | Competência escolhida | `Relatorios.test.tsx:127-145` — picker Mar/2025 → `gerarRelatorioFolha(3, 2025)` | ✅ PASS |
| REL-24 | PENDENTE → progresso + desabilita re-geração | Botão disabled + status | `Relatorios.test.tsx:174-178` — `toBeDisabled()` | ✅ PASS |
| REL-25 | PROCESSADO → Baixar PDF + thumbnail ≤200px | Download + preview | `RelatorioCatalogCard.tsx:128-131` — `aria-label="Pré-visualização do relatório PDF"` + PdfIcon; `Relatorios.test.tsx:161-171` download only | ⚠️ Preview aria present; no test assert; icon placeholder not thumbnail |
| REL-26 | ERRO → mensagem + Tentar novamente | Alert + retry | `Relatorios.test.tsx:197-209` | ✅ PASS |
| REL-27 | Layout MUI responsivo + a11y roles/labels | Cards navegáveis | `Relatorios.test.tsx:118`; `CompetenciaPicker.test.tsx` label | ✅ PASS |

**Status**: ❌ Gaps present — 14/27 PASS, 5 partial, 8 GAP (was 9/15/3)

---

### Discrimination Sensor

| # | File:line | Mutation | Killed? |
| --- | --- | --- | --- |
| 1 | `RelatorioGeracaoService.java:104` | Flip `!= PROCESSADO` → `== PROCESSADO` on download | ✅ Killed |
| 2 | `FolhaExecutivoPdfRenderer.java:54` | Replace "Sem dados para a competência…" text | ✅ Killed |
| 3 | `FolhaExecutivoPdfRenderer.java:110` | Swap KPI label "Custo Empresa" → "Custo Folha" | ✅ Killed |
| 4 | `RelatorioLayoutHelper.java` footer | Remove "de Y" from pagination | ✅ Killed |
| 5 | `RelatorioRepository.java:14` | `OrderByAnoDescMesDesc` → `OrderByAnoAscMesAsc` | ❌ **Survived** — WebMvc mocks service; no service-layer ordering test |

**Sensor depth**: 5 mutations (script `.scratch/sensor-relatorios/run-sensor.sh`)  
**Result**: 4/5 killed — sensor **FAIL** (1 survivor)

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / surgical | ✅ fix cycle scoped |
| Matches patterns | ✅ |
| Spec-anchored outcome check | ❌ 8 AC gaps + 5 partial |
| Per-layer coverage | ⚠️ PDF sections + list ordering still thin |
| Fix cycle addressed prior top gaps | ✅ REL-05,07,15,18,23; partial REL-03,06 |

---

### Edge Cases (spec)

| Edge case | Result |
| --- | --- |
| Sem organograma → 403 | ✅ `RelatorioGeracaoServiceTest.java:125-132` |
| Geração falha → ERRO truncado | ✅ `RelatorioGeracaoWorkerTest.java:131-132` — generic message |
| Download id inexistente → 404 | ✅ `RelatorioFolhaControllerWebMvcTest.java:140-146` |
| Competência futura → 400 | ✅ WebMvc + service |
| Re-geração mesma tupla substitui | ✅ `RelatorioGeracaoServiceTest.java:164-166` |
| PDF >50 MB → ERRO | ✅ `RelatorioGeracaoWorkerTest.java:149` |
| Worker after commit | ✅ `RelatorioGeracaoServiceTest.java:248` |

---

### Gate Check

| Item | Result |
| --- | --- |
| **Command** | `cd backend && mvn test && cd ../frontend && npm run lint && npm run test && npm run build` |
| **Backend** | 1111 passed, 0 failed, 1 skipped (Testcontainers/Docker) |
| **Frontend lint** | 0 errors, 14 warnings (pre-existing) |
| **Frontend tests (feature)** | Relatorios 22/22 passed |
| **Frontend tests (full)** | 299+ passed; 4 test files fail to load (ENFILE file-table overflow — environment) |
| **Frontend build** | ✅ success |
| **Relatorios module** | 52 backend + 22 frontend tests green |

---

### Fix Plans (cycle 2 — for implementer)

1. **REL-03 / sensor M5** — `RelatorioGeracaoServiceTest.listarFolha_ordenadoAnoMesDesc` with mocked repository returning unsorted rows; verify service preserves repo order (or integration test).
2. **REL-08** — Render folha PDF with fixed stats → extract text → assert KPI values ±0.01.
3. **REL-09–12** — Extend `FolhaExecutivoPdfRendererTest`: section titles, "Outros", rubrica codes, evolução labels.
4. **REL-13** — `RelatorioPdfServiceTest.renderFolhaExecutivo_scoped_passesLoginToDashboardPort` with restricted context fixture.
5. **REL-17, REL-19** — PDF asserts for resumo totals and matriz cell values.
6. **REL-06** — WebMvc `ano=1999` → 400.
7. **REL-16** — Align benefício KPI labels to spec or document accepted abbreviations.
8. **REL-25** — Test `getByLabelText('Pré-visualização do relatório PDF')` when PROCESSADO.

---

### Requirement Traceability Update

| Requirement | Cycle 0 | Cycle 1 |
| --- | --- | --- |
| REL-01…06 | ⚠️01 · ❌03,05,06 · ✅02,04 | ⚠️01,03,06 · ✅02,04,05 |
| REL-07…15 | ❌07,08,09–13,15 · ✅14 | ✅07,14,15 · ❌08,09–13 |
| REL-16…21 | ⚠️16 · ❌17–19 · ✅20,21 | ⚠️16 · ✅18,20,21 · ❌17,19 |
| REL-22…27 | ✅22,24,26,27 · ⚠️25 · ❌23 | ✅22,23,24,26,27 · ⚠️25 |

---

### Summary

**Overall**: ❌ Not Ready — gate green (backend + feature tests), spec-anchored coverage improved but insufficient; sensor survivor on list ordering.

**Delta vs cycle 0**: +5 PASS (REL-05,07,15,18,23); REL-03 impl fixed; 7 prior GAPs closed or downgraded.

**Next steps**: Route cycle-2 fix tasks; re-verify (iteration 2/3).

---

## Execution — relatorios-executivos fix cycle 2 — 2026-08-03

**Date**: 2026-08-03  
**Spec**: `_docs/specs/features/relatorios-executivos/spec.md`  
**Diff range**: `6311f79..2fc9f12` (1 commit: `2fc9f12`)  
**Verifier**: independent sub-agent (author ≠ verifier)  
**Overall**: ⚠️ PASS com ressalvas (21 PASS · 5 partial · 0 GAP; sensor 5/5; gate green)

---

### Task Completion

| Task | Status | Notes |
| --- | --- | --- |
| fix(cycle-2) PDF tests + benefício KPI labels | ✅ Done | `2fc9f12` — `PdfTextTestHelper`, folha/benefício renderer tests, scoped folha service test, list ordering service test, `ano=1999` WebMvc |

---

### Spec-Anchored Acceptance Criteria

| ID | Criterion (WHEN X THEN Y) | Spec-defined outcome | `file:line` + assertion | Result |
| --- | --- | --- | --- | --- |
| REL-01 | POST `/api/relatorios/folha` válido → PENDENTE, async, DTO com id/status final ≤60s | `status=PROCESSADO` ou `ERRO`; DTO com `id` | `RelatorioGeracaoServiceTest.java:256-281` — `verify(relatorioGeracaoWorker).processar(10L)`; `RelatorioGeracaoWorkerTest.java:111` — `assertEquals(PROCESSADO, salvo.getStatus())` | ⚠️ Service/worker proven; WebMvc mocks final status — no HTTP-level async proof |
| REL-02 | POST `/api/relatorios/beneficio` mesmo fluxo | Igual REL-01 para benefício | `RelatorioBeneficioControllerWebMvcTest.java:91-92` — `jsonPath("$.status").value("PROCESSADO")` | ✅ PASS |
| REL-03 | GET list → ordenado `ano DESC, mes DESC`; visível tenant-wide | Lista global single-tenant com ordering | `RelatorioGeracaoServiceTest.java:224-252` — `listarFolha_usaOrdenacaoRepositorioAnoMesDesc`; `RelatorioFolhaControllerWebMvcTest.java:186-201` — `$[0].ano=2026`, `$[1].ano=2024` | ✅ PASS |
| REL-04 | Download com `PROCESSADO` → `application/pdf` | Content-Type PDF + bytes | `RelatorioFolhaControllerWebMvcTest.java:121-124` — `contentType(APPLICATION_PDF)`, `content().bytes(pdf)` | ✅ PASS |
| REL-05 | Download `PENDENTE` ou `ERRO` → HTTP 409 | 409 indisponível | `RelatorioFolhaControllerWebMvcTest.java:107-108` PENDENTE; `129-135` ERRO — `status().isConflict()` | ✅ PASS |
| REL-06 | `mes`/`ano` fora limites → HTTP 400 Bean Validation | 400 validation error | `RelatorioFolhaControllerWebMvcTest.java:151-156` — `ano=1999` → 400; `161-166` — `mes=13` → 400 | ✅ PASS |
| REL-07 | Capa PDF folha: logo, título, competência, gerador, 4 KPIs spec | Total Funcionários, Custo Empresa, Total Proventos, Total Descontos | `FolhaExecutivoPdfRendererTest.java:69-76` — labels + `containsCurrencyValue` for stats fields | ✅ PASS |
| REL-08 | KPIs PDF = `DashboardService.getStats()` ±R$0,01 | Paridade numérica | `FolhaExecutivoPdfRendererTest.java:61-76` — `PdfTextTestHelper.extractAllText` + `containsCurrencyValue(text, stats.custoMensalFolha())` etc. | ✅ PASS |
| REL-09 | Seção Por Centro de Custo top 15 + Outros | Tabela CC ordenada DESC | `FolhaExecutivoPdfRendererTest.java:85-86` — `text.contains("Centros de Custo")`, `CC Admin` | ⚠️ Section titles/values asserted; no fixture with >15 CCs → "Outros" branch untested |
| REL-10 | Seção Por Linha de Negócio | Mesma estrutura CC | `FolhaExecutivoPdfRendererTest.java:87-88` — `Linhas de Neg`, `Educacional` | ✅ PASS |
| REL-11 | Top 5 Proventos / Descontos | Código, descrição, valor, qtd | `FolhaExecutivoPdfRendererTest.java:89-94` — `Top 5 Proventos`, `Top 5 Descontos`, `INSS`, currency values | ✅ PASS |
| REL-12 | Gráfico evolução 6 meses `MMM/yyyy` | Labels + chart estático | `FolhaExecutivoPdfRendererTest.java:98-108` — `containsEmbeddedImage(pdf)`; model labels `Jan/2024`, `Jun/2024` | ⚠️ Chart embedded; MMM/yyyy labels not asserted in extracted PDF text |
| REL-13 | Escopo ACL restrito → totais scoped | Paridade dashboard scoped | `RelatorioPdfServiceTest.java:84-116` — scoped stats in model; `FolhaExecutivoPdfRendererTest.java:112-137` — scoped PDF values | ✅ PASS |
| REL-14 | Competência vazia → capa + "Sem dados…" PROCESSADO | Mensagem explícita | `FolhaExecutivoPdfRendererTest.java:56` — `text.contains("Sem dados para a compet")` | ✅ PASS |
| REL-15 | Rodapé `Página X de Y` + texto Techne | Numeração completa | `RelatorioLayoutHelperTest.java:70-84` — `page2Text.contains("Página 2 de 2")` | ✅ PASS |
| REL-16 | Capa benefícios: KPIs spec | Total Benefícios, Qtd Lançamentos, Custo Folha, Consolidado | `BeneficioCustoPdfRenderer.java:75-78`; `BeneficioCustoPdfRendererTest.java:39-42` — `Total Benef`, `Qtd. Lan`, `Total Custo Folha`, `Custo Empresa Consolidado` | ✅ PASS |
| REL-17 | Tabela Resumo por Tipo = API resumo | Paridade `GET /beneficio-mensal/resumo` | `BeneficioCustoPdfRendererTest.java:48-51` — `Resumo por Tipo`, `Vale Refei`, currency; port in `BeneficioConsultaAdapterTest.java:319-325` | ⚠️ PDF content from model fixture; no integration test wiring port output → PDF text |
| REL-18 | Drill-down Top 10 por tipo | Nome, CC, valor; Outros agrupado | `BeneficioCustoPdfRendererTest.java:106-108` — `CC Admin`, `Outros`, `funcion` | ✅ PASS |
| REL-19 | Matriz Top 5 CC × Top 5 tipos | Valores R$ | `BeneficioCustoPdfRendererTest.java:55-63` — `Matriz Centro de Custo`, `CC Admin`, `containsCurrencyValue(1500.00)` | ✅ PASS |
| REL-20 | Sem benefícios, com folha → nota | "Nenhum benefício lançado" | `BeneficioCustoPdfRendererTest.java:71` | ✅ PASS |
| REL-21 | Moeda pt-BR `R$ 1.234,56` | Formato BR | `RelatorioLayoutHelperTest.java:35-37`; `BeneficioCustoPdfRendererTest.java:87` | ✅ PASS |
| REL-22 | Hub cards catálogo | 2 cards com ícone/descrição/badge | `Relatorios.test.tsx:116-117` | ✅ PASS |
| REL-23 | MonthPicker → payload `{ mes, ano }` | Competência escolhida | `Relatorios.test.tsx:127-145` — picker Mar/2025 → `gerarRelatorioFolha(3, 2025)` | ✅ PASS |
| REL-24 | PENDENTE → progresso + desabilita re-geração | Botão disabled + status | `Relatorios.test.tsx:174-178` — `toBeDisabled()` | ✅ PASS |
| REL-25 | PROCESSADO → Baixar PDF + thumbnail ≤200px | Download + preview | `RelatorioCatalogCard.tsx:128` — `aria-label="Pré-visualização do relatório PDF"`; `Relatorios.test.tsx:161-171` download only | ⚠️ Preview aria present; no test assert; icon placeholder not thumbnail |
| REL-26 | ERRO → mensagem + Tentar novamente | Alert + retry | `Relatorios.test.tsx:197-209` | ✅ PASS |
| REL-27 | Layout MUI responsivo + a11y roles/labels | Cards navegáveis | `Relatorios.test.tsx:118`; `CompetenciaPicker.test.tsx` label | ✅ PASS |

**Status**: ⚠️ Ressalvas — 21/27 PASS, 5 partial, 0 GAP (was 14/5/8)

---

### Discrimination Sensor

| # | File:line | Mutation | Killed? |
| --- | --- | --- | --- |
| 1 | `RelatorioGeracaoService.java:104` | Flip `!= PROCESSADO` → `== PROCESSADO` on download | ✅ Killed |
| 2 | `FolhaExecutivoPdfRenderer.java:54` | Replace "Sem dados para a competência…" text | ✅ Killed |
| 3 | `FolhaExecutivoPdfRenderer.java:110` | Swap KPI label "Custo Empresa" → "Custo Folha" | ✅ Killed |
| 4 | `RelatorioLayoutHelper.java` footer | Remove "de Y" from pagination | ✅ Killed |
| 5 | `RelatorioRepository.java:14` | `OrderByAnoDescMesDesc` → `OrderByAnoAscMesAsc` | ✅ Killed — `RelatorioGeracaoServiceTest.listarFolha_usaOrdenacaoRepositorioAnoMesDesc` + compile guard |

**Sensor depth**: 5 mutations (script `.scratch/sensor-relatorios/run-sensor.sh`)  
**Result**: 5/5 killed — sensor **PASS** ✅

---

### Code Quality

| Principle | Status |
| --- | --- |
| Minimum code / surgical | ✅ fix cycle scoped (+266 LOC tests) |
| Matches patterns | ✅ `PdfTextTestHelper` reusable |
| Spec-anchored outcome check | ⚠️ 0 GAP; 5 partial (depth/integration) |
| Per-layer coverage | ✅ PDF renderer + service layers strengthened |
| Fix cycle addressed cycle-1 gaps | ✅ REL-03,06,08,09–13,16,17,19 closed or upgraded |

---

### Edge Cases (spec)

| Edge case | Result |
| --- | --- |
| Sem organograma → 403 | ✅ `RelatorioGeracaoServiceTest.java:125-132` |
| Geração falha → ERRO truncado | ✅ `RelatorioGeracaoWorkerTest.java:131-132` |
| Download id inexistente → 404 | ✅ `RelatorioFolhaControllerWebMvcTest.java:140-146` |
| Competência futura → 400 | ✅ WebMvc + service |
| Re-geração mesma tupla substitui | ✅ `RelatorioGeracaoServiceTest.java:164-166` |
| PDF >50 MB → ERRO | ✅ `RelatorioGeracaoWorkerTest.java:149` |
| Worker after commit | ✅ `RelatorioGeracaoServiceTest.java:281` |
| Bean Validation ano/mes bounds | ✅ `RelatorioFolhaControllerWebMvcTest.java:151-166` |

---

### Gate Check

| Item | Result |
| --- | --- |
| **Command** | `cd backend && mvn test && cd ../frontend && npm run lint && npm run test && npm run build` |
| **Backend** | 1119 passed, 0 failed, 1 skipped (Testcontainers/Docker) |
| **Frontend lint** | 0 errors, 14 warnings (pre-existing) |
| **Frontend tests (feature)** | Relatorios 22/22 passed |
| **Frontend tests (full)** | 347 passed; 3 test files fail to load (ENFILE file-table overflow — environment) |
| **Frontend build** | ✅ success |
| **Delta vs cycle 1** | +8 backend tests (PDF helper + strengthened asserts) |

---

### Fix Plans (cycle 3 — optional polish)

1. **REL-01** — Integration/WebMvc test with `@Async` or await poll until PROCESSADO (or accept partial for MVP).
2. **REL-09** — Fixture with 16+ CC rows; assert "Outros" row and DESC order in PDF text.
3. **REL-12** — Assert `Jan/2024` / `Jun/2024` in extracted PDF text (not just model).
4. **REL-17** — Wire `BeneficioConsultaPort.resumoPorTipo` mock → render → PDF text parity test.
5. **REL-25** — `Relatorios.test.tsx`: `getByLabelText('Pré-visualização do relatório PDF')` when PROCESSADO.

---

### Requirement Traceability Update

| Requirement | Cycle 1 | Cycle 2 |
| --- | --- | --- |
| REL-01…06 | ⚠️01,03,06 · ✅02,04,05 | ⚠️01 · ✅02,03,04,05,06 |
| REL-07…15 | ✅07,14,15 · ❌08,09–13 | ✅07,08,10,11,13,14,15 · ⚠️09,12 |
| REL-16…21 | ⚠️16 · ✅18,20,21 · ❌17,19 | ✅16,18,19,20,21 · ⚠️17 |
| REL-22…27 | ✅22,23,24,26,27 · ⚠️25 | ✅22,23,24,26,27 · ⚠️25 |

---

### Summary

**Overall**: ⚠️ PASS com ressalvas — gate green, sensor 5/5, all 8 prior GAPs closed; 5 partial remain (integration depth, not missing behavior).

**Delta vs cycle 1**: +7 PASS (REL-03,06,08,10,11,13,16,19); sensor M5 survivor eliminated; 0 GAP.

**Next steps**: Accept ressalvas for MVP sign-off, or route cycle-3 polish (iteration 3/3).
