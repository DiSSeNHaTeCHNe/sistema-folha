# Validation — Quick Task 008 (decimo_terceiro folha linha)

**Verdict:** ✅ PASS  
**Date:** 2026-07-28  
**Scope:** DT13-01 … DT13-07

## Gate

```bash
mvn test -Dtest=FolhaConsultaAdapterTest,FolhaImportacaoAdapterTest,ResumoFolhaPagamentoServiceTest,DashboardServiceTest,ImportacaoFolhaAdpServiceTest,FolhaPagamentoServiceTest
```

50 tests, 0 failures.

## Per-AC evidence

| AC | Evidence |
|----|----------|
| DT13-01 | `FolhaImportacaoAdapter.java` — `montarFolha` sets `decimoTerceiro`; `FolhaImportacaoAdapterTest.persistirImportacao_decimoTerceiro_gravaFlagNaLinha` |
| DT13-02 | `FolhaImportacaoAdapter.substituirCompetenciaExistente` uses `findByDataInicioAndDataFimAndDecimoTerceiro`; test `persistirImportacao_comSubstituir_removeAntesDeInserir` updated |
| DT13-03 | `ResumoFolhaPagamentoService.toDtoScoped` passes `Boolean.TRUE.equals(resumo.getDecimoTerceiro())`; test `listarTodos_scoped_mesComNormalE13_retornaTotaisDistintos` |
| DT13-04 | `DashboardService.getStats` passes `resumo.decimoTerceiro()` to line query |
| DT13-05 | `DashboardService.calcularEvolucaoMensalScoped` passes `false` |
| DT13-06 | `FolhaPagamentoController` + `FolhaPagamentoService.consultarPorPeriodo`; test `consultarPorPeriodo_comDecimoTerceiro_filtraPorTipo` |
| DT13-07 | Port/repository exists methods include `decimoTerceiro`; `ImportacaoFolhaAdpService.processarRubrica` passes flag |

## Discrimination sensor (manual)

| Fault injected (conceptual) | Killing test |
|-----------------------------|--------------|
| Omit `decimoTerceiro` filter in scoped resumo | `listarTodos_scoped_mesComNormalE13_retornaTotaisDistintos` |
| Omit flag on import persist | `persistirImportacao_decimoTerceiro_gravaFlagNaLinha` |
| Adapter returns all lines regardless of type | `findLinhasAtivasPorCompetencia_filtraPorDecimoTerceiro` |

## Notes

- No backfill (base sem dados reais, per user).
- `consultarPorCompetencia` resumo still ambiguous when two resumos share dates — out of scope (TASK.md).
