# Quick Task 009 — Detalhe de rubricas sem misturar 13º e folha regular

**Date:** 2026-07-28  
**Scope:** Small (Quick-Fix)  
**Related:** Quick 008 (decimo_terceiro folha linha)

## Problem

No detalhamento "Ver Rubricas", linhas de 13º e folha regular do mesmo mês podem aparecer juntas quando a consulta por funcionário não filtra por `decimoTerceiro`.

## Requirements

| ID | WHEN | THEN |
| --- | --- | --- |
| **DT13-DET-01** | Usuário abre detalhe de rubricas a partir de um resumo | SHALL exibir somente linhas com o mesmo `decimoTerceiro` do resumo selecionado |
| **DT13-DET-02** | `GET /folha-pagamento/funcionario/{id}` | SHALL aceitar `decimoTerceiro` e filtrar linhas ativas do período |
| **DT13-DET-03** | Resposta da API de linha de folha | SHALL incluir `decimoTerceiro` no DTO |

## Out of Scope

- Reclassificar rubricas de base FGTS do 13º (1301/1331) importadas na folha regular
- Alterar totais agregados ou importação ADP

## Verification

```bash
cd backend && mvn test -Dtest=FolhaPagamentoServiceTest
```

Manual: Folha → resumo jun/2026 regular → Ver Rubricas (Thyago) → sem linha 0508; resumo 13º → só linha 0508.
