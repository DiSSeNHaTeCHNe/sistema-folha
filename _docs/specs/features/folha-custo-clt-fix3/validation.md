# folha-custo-clt-fix3 Validation

## Status atual
- **Veredito**: PASS ✅
- **Spec vigente**: folha-custo-clt-fix3/spec.md
- **HEAD**: d816f7a
- **Gaps abertos**: nenhum

---

## folha-custo-clt-fix3 — 2026-07-29 — a31bc15..d816f7a

**Verifier**: independent sub-agent (author ≠ verifier)  
**Diff range**: 12 commits (`fix3:` prefix) on `feat/folha-custo-clt`

### Veredito: PASS ✅

P1 backend (FIX3-01…11) com evidência em migração/código + testes unitários Mockito. P1 frontend (FIX3-12…24) verificado por inspeção de código + gate lint/build (AD-004). Gate completo verde. Sensor: 3/3 mutações mortas.

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | ✅ Done | V1.24 nullable + entity |
| T2 | ✅ Done | Repository global queries |
| T3 | ✅ Done | CRUD global/individual + DTO `%` |
| T4 | ✅ Done | Service unit tests |
| T5 | ✅ Done | Controller 409 → GlobalExceptionHandler |
| T6 | ✅ Done | Processamento global + tests |
| T7 | ✅ Done | API detalhe `porcentagem` + tests |
| T8 | ✅ Done | FE types |
| T9 | ✅ Done | Form reordenado + funcionário opcional |
| T10 | ✅ Done | Listagem Todos + Percentual + toast 409 |
| T11 | ✅ Done | Renderer unificado Bruto/Líquido/Custo |
| T12 | ✅ Done | Docs cross-ref + full gate |

---

## Spec-Anchored Acceptance Criteria

| Criterion | Spec-defined outcome | `file:line` + assertion / code evidence | Result |
| --------- | -------------------- | ----------------------------------------- | ------ |
| FIX3-01 WHEN migração THEN `funcionario_id` nullable + FK opcional | Column nullable; partial index global; entity `@JoinColumn(nullable=true)` | `V1.24__funcionario_rubrica_fixa_global.sql:2-10` — `ALTER COLUMN funcionario_id DROP NOT NULL`; `FuncionarioRubricaFixa.java:24-25` — `nullable = true` | ✅ PASS |
| FIX3-02 WHEN POST/PUT sem `funcionarioId` THEN persist null + DTO null nome | `funcionario_id` null; `funcionarioNome` null | `FuncionarioRubricaFixaServiceTest.java:98-107` — `assertNull(result.funcionarioId())`, `assertNull(captor.getValue().getFuncionario())` | ✅ PASS |
| FIX3-03 WHEN POST/PUT com `funcionarioId` THEN comportamento individual inalterado | Individual persiste vínculo e nome | `FuncionarioRubricaFixaServiceTest.java:147-155` — `assertEquals(FUNCIONARIO_ID, result.funcionarioId())`, `assertEquals("Funcionário Teste", result.funcionarioNome())` | ✅ PASS |
| FIX3-04 WHEN processar THEN fixas globais → `CUSTO_FIXO` em cada ficha CLT | 2 CLT + global → 2 linhas CUSTO_FIXO valor 500 | `FolhaProcessamentoServiceTest.java:464-475` — `assertEquals(2, linhasCustoFixo)`; `forEach(l -> assertEquals(new BigDecimal("500.00"), l.getValor()))` | ✅ PASS |
| FIX3-05 WHEN individual + global mesma rubrica THEN individual prevalece | João 688, demais 500 | `FolhaProcessamentoServiceTest.java:511-521` — `fixaFunc1` valor 688.00; `fixaFunc2` valor 500.00 | ✅ PASS |
| FIX3-06 WHEN rubrica no ADP THEN fixa ignorada (WARN) | Só linha ADP; fixa não materializada | `FolhaProcessamentoServiceTest.java:546-552` — `assertEquals(1, resultado.totalLinhas())`, `OrigemLinha.FOLHA_ADP`; WARN em `FolhaProcessamentoService.java:115-116` (não assertado) | ✅ PASS (⚠️ WARN não assertado) |
| FIX3-07 WHEN duas globais sobrepostas THEN HTTP 409 | Conflito global com mensagem distinta | `FuncionarioRubricaFixaServiceTest.java:120-125` — `assertThrows(...)` + mensagem global; `GlobalExceptionHandler.java:86-90` — `HttpStatus.CONFLICT` | ✅ PASS |
| FIX3-08 WHEN fixa global alterada pós-processamento THEN só após reprocessar | Ficha 10500 até reprocesso; 10700 após | `FolhaProcessamentoServiceTest.java:584-593` — `fichaPrimeiro.getCustoFolha()` 10500; `fichaReprocesso.getCustoFolha()` 10700 | ✅ PASS |
| FIX3-09 WHEN GET linhas detalhe THEN `porcentagem` snapshot na response | `porcentagem=138.63` nas 3 abas | `FolhaFichaConsultaServiceTest.java:228-233` — `assertEquals(new BigDecimal("138.63"), result.get(0).porcentagem())` (loop GROSS/NET/COMPANY_COST) | ✅ PASS |
| FIX3-10 WHEN BENEFICIO aba Custo THEN `porcentagem` null | null → FE exibe — | `FolhaFichaConsultaServiceTest.java:207-211` — `assertNull(beneficio.porcentagem())`; `FolhaFichaConsultaService.java:82-88` — `null` no DTO | ✅ PASS |
| FIX3-11 WHEN GROSS/NET THEN `contribuicao` sem `%`; `porcentagem` informativa | contrib=7258.43; porcentagem=138.63 | `FolhaFichaConsultaServiceTest.java:154-161` — `assertEquals(7258.43, gross.get(0).contribuicao())`, `assertEquals(138.63, gross.get(0).porcentagem())` | ✅ PASS |
| FIX3-12 WHEN form THEN ordem Rubrica→Valor→Vigências→Funcionário→Comentário | Ordem visual conforme spec | `RubricasFixas/index.tsx:298-386` — Rubrica (298), Valor (321), Vigência início (331), Vigência fim (341), Funcionário (351), Comentário (378) | ✅ PASS |
| FIX3-13 WHEN funcionário não selecionado THEN helper + submit sem `funcionarioId` | Label "Todos os funcionários (mesmo valor)"; payload omite id | `RubricasFixas/index.tsx:367,376` — MenuItem + FormHelperText; `funcionarioRubricaFixaService.ts:40-42` — só inclui `funcionarioId` se preenchido | ✅ PASS |
| FIX3-14 WHEN listagem THEN coluna Percentual live (`porcentagem ?? 100`) | Exibe % mestre, default 100 | `RubricasFixas/index.tsx:55-57,267` — `formatPercentualFixa(item.porcentagem)`; `FuncionarioRubricaFixaServiceTest.java:185` — DTO inclui `porcentagem` da rubrica | ✅ PASS |
| FIX3-15 WHEN listagem global THEN coluna Funcionário "Todos" | "Todos" quando id null | `RubricasFixas/index.tsx:60-64,262` — `formatFuncionarioFixa` retorna `'Todos'` | ✅ PASS |
| FIX3-16 WHEN erro 409 THEN toast distingue global vs individual | Toast usa `response.data.message` | `RubricasFixas/index.tsx:40-52,149` — `getApiErrorMessage`; backend `FuncionarioRubricaFixaVigenciaConflictException.java:9-16` mensagens distintas | ✅ PASS |
| FIX3-17 WHEN aba Bruto/Líquido THEN agrupar por `origemLinha` | Mesmo renderer que Custo; `ORIGEM_LABELS` | `FolhaPagamento/index.tsx:352-396` — `renderDetalheAgrupado` para GROSS/NET/COMPANY_COST; títulos `ORIGEM_LABELS[origem]` | ✅ PASS |
| FIX3-18 WHEN qualquer aba THEN colunas Rubrica\|Valor\|Percentual\|Contribuição | 4 colunas padronizadas | `FolhaPagamento/index.tsx:401-406` — TableHead com as 4 colunas | ✅ PASS |
| FIX3-19 WHEN Percentual THEN snapshot formatado ou — (BENEFICIO) | `138,63%` ou `—`; null→100% | `FolhaPagamento/index.tsx:107-122,415` — `formatPercentual`; BENEFICIO → `'—'` | ✅ PASS |
| FIX3-20 WHEN fim grupo origem THEN Subtotal soma contribuições | Subtotal por grupo | `FolhaPagamento/index.tsx:392,419-425` — `sumContribuicoes(linhasGrupo)` + row Subtotal | ✅ PASS |
| FIX3-21 WHEN fim aba THEN Total exibido | Total da aba renderizado | `FolhaPagamento/index.tsx:433-437` — `Total: {formatMoneyDisplay(cardTotal)}` | ✅ PASS |
| FIX3-22 WHEN aba Bruto THEN Total = `salBruto` card | Card bruto como total da aba | `FolhaPagamento/index.tsx:338-344,451` — `getCardTotal('GROSS')` → `funcionarioSelecionado.salBruto` passado ao renderer | ✅ PASS |
| FIX3-23 WHEN aba Líquido THEN Total = `salLiquido` card | Card líquido como total | `FolhaPagamento/index.tsx:345-346,451` — `getCardTotal('NET')` → `salLiquido` | ✅ PASS |
| FIX3-24 WHEN aba Custo THEN Total = `custoEmpresa` card | Card custo como total | `FolhaPagamento/index.tsx:347-348,451` — `getCardTotal('COMPANY_COST')` → `custoEmpresa` | ✅ PASS |

**Status**: ✅ 24/24 ACs covered (1 spec-precision note: FIX3-06 WARN não assertado em teste)

---

## Discrimination Sensor

Scratch: `git worktree` (detached HEAD d816f7a); mutações descartadas com remoção do worktree. Working tree principal intacto.

| Mutation | File:line | Description | Killed? |
| -------- | --------- | ----------- | ------- |
| M1 | `FolhaProcessamentoService.java:119` | Flip skip global quando individual existe (`contains` → `!contains`) | ✅ Killed — `FolhaProcessamentoServiceTest#processar_individualPrevaleceSobreGlobal_mesmaRubrica:520` `NoSuchElementException` (func2 sem CUSTO_FIXO) |
| M2 | `FuncionarioRubricaFixaService.java:145` | Desabilitar throw global 409 (`if (false) throw ...`) | ✅ Killed — `FuncionarioRubricaFixaServiceTest#criar_global_vigenciaSobreposta_lanca409Global:120` `AssertionFailedError: expected FuncionarioRubricaFixaVigenciaConflictException` |
| M3 | `FolhaFichaConsultaService.java:69` | Substituir `linha.getPorcentagem()` por `null` no DTO | ✅ Killed — `FolhaFichaConsultaServiceTest#listarLinhasPorTotalizador_expoePorcentagemSnapshotNasTresAbas:232` `expected: <138.63> but was: <null>` |

**Sensor depth**: lightweight (3 mutations)  
**Result**: 3/3 killed — PASS ✅

---

## Gate Check

- **Gate command**: `cd backend && mvn test && cd ../frontend && npm run lint && npm run build`
- **Backend**: **274** passed, **0** failed, **0** errors, **0** skipped
- **Frontend lint**: **PASS** — 0 errors, 8 warnings pré-existentes (`react-hooks/exhaustive-deps`)
- **Frontend build**: **PASS** — tsc + vite build OK
- **Test count before feature** (a31bc15): 264
- **Test count after feature** (d816f7a): 274
- **Delta**: +10 tests (scope: FuncionarioRubricaFixa +5, FolhaProcessamento +4, FolhaFichaConsulta +1)
- **ModularArchitectureTest**: 18/18 pass (AD-010 inalterado)

---

## Code Quality

| Principle | Status |
| --------- | ------ |
| Minimum code / surgical changes | ✅ |
| No scope creep | ✅ |
| Matches existing patterns | ✅ |
| Spec-anchored outcome check | ✅ (FIX3-06 WARN observability não assertada — minor) |
| Per-layer coverage (domain 1:1 ACs; FE AD-004 inspection) | ✅ |
| Every test maps to spec requirement | ✅ |
| Guidelines: `_docs/specs/TESTING.md`, AD-004, backend/frontend AGENTS.md | ✅ |

---

## Edge Cases (spec)

| Edge case | Result |
| --------- | ------ |
| Fixa global valor null + rubrica não calculada → 400 | ✅ `FuncionarioRubricaFixaServiceTest.java:159-165` |
| `%` null rubrica mestre → 100% listagem/detalhe | ✅ `RubricasFixas/index.tsx:56`; `FolhaPagamento/index.tsx:114-115` |
| Aba sem linhas → empty state + Total R$ 0,00 | ✅ `FolhaPagamento/index.tsx:373-384` |
| Scoped ACL — fixa global só em fichas visíveis | ⚠️ não testado nesta feature (herança processamento ACL existente) |
| Reprocesso após fixa global → novas linhas CUSTO_FIXO | ✅ `FolhaProcessamentoServiceTest.java:436-475`, `:556-593` |

---

## Summary

**Overall**: ✅ Ready

**Spec-anchored check**: 24/24 ACs matched (1 minor: FIX3-06 WARN não assertado)  
**Sensor**: 3/3 mutations killed  
**Gate**: 274 backend + FE lint/build passed

**What works**: Rubrica fixa global (CRUD + processamento + prioridade individual/ADP); API detalhe com `porcentagem` snapshot; UX Rubricas Fixas (form, listagem, 409 toast); abas Bruto/Líquido/Custo padronizadas com subtotais e total = card.

**Issues found**: nenhum bloqueante.

**Next steps**: nenhuma ação requerida — feature pronta para merge/UAT opcional.
