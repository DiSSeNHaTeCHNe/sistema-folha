# ACL — Centro de Custo por Competência Context

**Gathered:** 2026-07-29  
**Spec:** `_docs/specs/features/acl-cc-competencia/spec.md`  
**Status:** Ready for design

---

## Feature boundary

Corrigir filtro ACL (organograma) em **folha** e **benefícios** para usar o centro de custo **da competência** (gravado na linha/lançamento), não o CC atual do cadastro do funcionário. Inclui resumo scoped, dashboard e endpoint por CC.

Fora: histórico de vínculos no cadastro, reprocessamento de dados legados, mudanças de JWT/organograma.

---

## User decisions (gray areas)

### A — Regra canônica do CC para ACL

**Escolha: A2 — COALESCE(linha/lançamento → funcionário)**

- Prioridade: `folha_pagamento.centro_custo_id` / `beneficio_mensal.centro_custo_id`
- Fallback: `funcionarios.centro_custo_id` quando a linha/lançamento não tiver CC
- Rationale: linha reflete a competência; fallback cobre edge cases sem bloquear leitura

### B — Benefícios no mesmo feature

**Escolha: B2 — Sim, mesmo critério**

- `BeneficioMensal` hoje **não** persiste CC na linha — spec exige coluna `centro_custo_id` + snapshot na criação
- ACL, queries de listagem/resumo e DTO alinhados com folha

### C — Endpoint `consultarPorCentroCusto`

**Escolha: C1 — Filtrar por CC da linha**

- `GET /folha-pagamento/centro-custo/{id}` retorna linhas onde `folha.centro_custo_id = {id}` no período
- Permissão organograma inalterada (`usuarioPodeAcessarCentroCusto`)

### D — Dados legados

**Escolha: não se preocupar — fase de desenvolvimento**

- Sem backfill, sem migração de dados históricos
- Fallback A2 suficiente para linhas/lançamentos sem CC explícito
- Novos lançamentos (import folha + create benefício) SHALL gravar CC na linha

### E — Path ficha processada (round 2, 2026-07-29)

**Escolha: E1 — Snapshot CC em `ficha_mensal` no processamento (Opção A)**

- Coluna `ficha_mensal.centro_custo_id` nullable + FK
- `FolhaProcessamentoService.montarFicha` congela CC efetivo das linhas ADP do grupo
- `FichaLinhaRepository` + `FolhaConsultaAdapter` path ficha usam `COALESCE(ficha, funcionário)`
- Reprocessamento atualiza snapshot (FCC-21)
- Sem backfill ficha legado — fallback funcionário (A10)

---

## Locked for design

| Decision | Value |
| -------- | ----- |
| Helper central | `centroCustoEfetivo` reutilizável (folha + benefício) |
| `acessoTotal` | Sem filtro por CC (AD-011) |
| Contrato HTTP | Sem breaking change de campos DTO |
| Benefício create | Snapshot `funcionario.centroCusto` em `beneficio_mensal.centro_custo_id` |
| Testes | Caso discriminatório: funcionário mudou CC após import/create |

---

## Deferred ideas

- Tabela de histórico de CC no cadastro do funcionário (feature separada)
- Backfill retroativo de `centro_custo_id` em benefícios existentes (se necessário pós-produção)
