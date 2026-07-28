# Quick Task 008 — Summary

**Status:** ✅ Complete  
**Date:** 2026-07-28

## Problem

Resumo scoped e dashboard agregavam linhas de folha normal e 13º no mesmo mês (mesmas datas), exibindo totais idênticos para usuários com escopo de organograma.

## Solution

Coluna `decimo_terceiro` em `folha_pagamento` + filtro em consultas, importação e agregações scoped.

## Changes

| Area | Change |
|------|--------|
| Flyway `V1.16` | `decimo_terceiro` + índice parcial |
| Importação | Persiste flag; substituição remove só linhas do mesmo tipo |
| `FolhaConsultaPort` | `findLinhasAtivasPorCompetencia(..., decimoTerceiro, centros)` |
| Resumo scoped | Agrega com flag do snapshot |
| Dashboard | Cards usam flag do resumo; evolução scoped usa `false` |
| API/FE | `GET /folha-pagamento?decimoTerceiro=` + "Ver funcionários" |

## Verification

```bash
cd backend && mvn test -Dtest=FolhaConsultaAdapterTest,FolhaImportacaoAdapterTest,ResumoFolhaPagamentoServiceTest,DashboardServiceTest,ImportacaoFolhaAdpServiceTest,FolhaPagamentoServiceTest
```

**Result:** 50 tests, 0 failures (2026-07-28)

## AC traceability

| ID | Status |
|----|--------|
| DT13-01 | ✅ |
| DT13-02 | ✅ |
| DT13-03 | ✅ |
| DT13-04 | ✅ |
| DT13-05 | ✅ |
| DT13-06 | ✅ |
| DT13-07 | ✅ |
