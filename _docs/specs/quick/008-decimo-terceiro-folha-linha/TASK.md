# Quick Task 008 — Separar folha normal e 13º nas linhas

**Date:** 2026-07-28  
**Scope:** Small (Quick-Fix)  
**Related:** `acl-scoped-folha-resumo`, bug resumo scoped + dashboard misturando normal/13º

## Problem

Meses com folha regular e 13º compartilham as mesmas datas em `folha_pagamento`. Resumo scoped e dashboard agregam todas as linhas do período — totais iguais nos dois resumos.

## Requirements

| ID | WHEN | THEN |
| --- | --- | --- |
| **DT13-01** | Importação ADP (normal ou 13º) | Cada linha persistida SHALL ter `decimo_terceiro` alinhado ao comando de importação |
| **DT13-02** | Substituição de competência na importação | SHALL remover apenas linhas do **mesmo** tipo (normal vs 13º) |
| **DT13-03** | Resumo scoped (`toDtoScoped`) | SHALL agregar linhas filtradas por `decimoTerceiro` do snapshot |
| **DT13-04** | Dashboard cards (competência mais recente) | SHALL buscar linhas com `decimoTerceiro` do resumo mais recente |
| **DT13-05** | Dashboard evolução scoped | SHALL buscar linhas com `decimoTerceiro = false` (só regular) |
| **DT13-06** | `GET /folha-pagamento?dataInicio&dataFim` | SHALL aceitar `decimoTerceiro` e filtrar linhas |
| **DT13-07** | Duplicidade na importação (CPF/rubrica) | SHALL considerar `decimoTerceiro` no mesmo período |

## Out of Scope

- Backfill de dados legados (base sem dados reais)
- FK `resumo_folha_pagamento_id` nas linhas
- Alterar `consultarPorCompetencia` do resumo (ambíguo com dois resumos — follow-up)

## Verification

```bash
cd backend && mvn test -Dtest=FolhaConsultaAdapterTest,FolhaImportacaoAdapterTest,ResumoFolhaPagamentoServiceTest,DashboardServiceTest
```
