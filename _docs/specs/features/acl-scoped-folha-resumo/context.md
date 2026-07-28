# ACL — Resumo Folha scoped + Dashboard evolução Context

**Gathered:** 2026-07-27  
**Spec:** `_docs/specs/features/acl-scoped-folha-resumo/spec.md`  
**Status:** Ready for design (A2, B1, C1, D1 locked)

---

## Feature Boundary

Resumo da folha (`/resumo-folha-pagamento*`) devolve totais no escopo do usuário (ou snapshot se `acessoTotal`); competências sem linhas no escopo aparecem com zeros. Dashboard: evolução mensal também no escopo parcial. Benefícios: sem mudança de código neste feature.

---

## Implementation Decisions

### A — Competência sem linhas no escopo

- **A2:** Mostrar a competência na listagem com **zeros** (`totalEmpregados=0`, valores monetários 0, inclusive encargos)

### B — Total Encargos com escopo

- **B1:** `totalEncargos = 0` no caminho scoped  
- Demais totais a partir das linhas: pagamentos = soma PROVENTO, descontos = soma DESCONTO, líquido = pagamentos − descontos, empregados = distinct funcionarioId  
- Com `acessoTotal`: snapshot ADP intacto (incluindo encargos reais)

### C — Dashboard evolução

- **C1:** Incluir no **MVP (P1)** — evolução mensal recalculada no escopo parcial (não lista vazia)

### D — Benefícios

- **D1:** Fora de implementação; apenas regressão/gate se a suite for tocada indiretamente

### Agent's Discretion

- Manter shape `ResumoFolhaPagamentoDTO`; passar login no controller  
- Preservar `id` / `decimoTerceiro` / datas do snapshot da competência ao devolver linha zerada ou recalculada  
- Como montar a série de evolução scoped (iterar competências do snapshot vs meses calendário) — Design escolhe o mais simples alinhado ao port existente  
- Onde viver o helper de agregação (service folha vs reuso Dashboard) — Design

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma — A–D respondidas pelo usuário (A2, B1, C1, D1).

---

## Specific References

- Auditoria: resumo folha global vs listagem filtrada; Dashboard evolução vazia sem total  
- Parent ATOT-11 de `acl-acesso-total-role`

---

## Deferred Ideas

- Rateio de encargos por escopo  
- Resumos persistidos por centro de custo  
- Privilege escalation `/usuarios`
