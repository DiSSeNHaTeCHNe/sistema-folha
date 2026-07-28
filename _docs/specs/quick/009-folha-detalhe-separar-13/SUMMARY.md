# Quick Task 009 — Summary

**Status:** ✅ Complete  
**Date:** 2026-07-28

## Problem

Detalhe "Ver Rubricas" podia misturar linhas de 13º e folha regular do mesmo mês, pois filtrava apenas por funcionário/período no estado local, sem respeitar `decimoTerceiro` do resumo selecionado.

## Solution

- `GET /folha-pagamento/funcionario/{id}` passa a aceitar `decimoTerceiro` e filtrar no repositório.
- DTO de linha inclui `decimoTerceiro`.
- Frontend busca rubricas do detalhe via API com o flag do resumo ativo (default `false`).
- Título do dialog indica "Folha regular" vs "13º salário".

## Changes

| Area | Change |
|------|--------|
| Backend API | `FolhaPagamentoController.consultarPorFuncionario` + param `decimoTerceiro` |
| Backend service | `FolhaPagamentoService.consultarPorFuncionario` filtra por competência + tipo |
| Repository | `findByFuncionarioIdAndCompetenciaAndDecimoTerceiroAndAtivoTrue` |
| DTO | `FolhaPagamentoDTO.decimoTerceiro` |
| Frontend | `handleDetalharRubricas` usa `buscarPorFuncionario` com flag do resumo |
| Frontend | `buscarPorPeriodo` envia `decimoTerceiro ?? false` explicitamente |

## Verification

```bash
cd backend && mvn test -Dtest=FolhaPagamentoServiceTest,ImportacaoFolhaAdpServiceTest,FolhaImportacaoAdapterTest
cd frontend && npm run typecheck
```

**Result:** 14 backend tests, 0 failures; typecheck OK (2026-07-28)

## AC traceability

| ID | Status |
|----|--------|
| DT13-DET-01 | ✅ |
| DT13-DET-02 | ✅ |
| DT13-DET-03 | ✅ |

## Manual check

Folha → jun/2026 regular → Ver Rubricas (Thyago): 15 linhas, sem 0508.  
Folha → jun/2026 13º → Ver Rubricas (Thyago): 1 linha (0508).
