# ACL — Role `ACESSO_TOTAL` Context

**Gathered:** 2026-07-27  
**Spec:** `_docs/specs/features/acl-acesso-total-role/spec.md`  
**Status:** Ready for design approval → Execute (Medium)

---

## Feature Boundary

Introduzir permissão `ACESSO_TOTAL` que seta `acessoTotal=true` no port ACL sem exigir funcionário/nó; manter `ADMIN` só para mutações; seed do admin + picker FE; **não** filtrar `ResumoFolhaPagamento` neste MVP.

---

## Implementation Decisions

### A — Nome da permissão

- String canônica: `ACESSO_TOTAL` (authority `ROLE_ACESSO_TOTAL`)
- Match exato na lista `usuario.permissoes` (mesmo padrão das demais)

### B — Seed

- Migração Flyway concede `ACESSO_TOTAL` ao usuário admin seed (`login = 'admin'`, tipicamente id=1)
- Idempotente (`ON CONFLICT DO NOTHING` ou equivalente seguro)

### C — Ajuste de `ADMIN`

- **Somar** `ACESSO_TOTAL` ao admin; **não** alterar o significado de `ADMIN`
- `ADMIN` sozinho **não** implica `acessoTotal=true`
- Matchers Spring `hasRole("ADMIN")` existentes permanecem intactos

### D — Resumo da folha

- **Não** aplicar ACL em `ResumoFolhaPagamentoService` neste MVP
- ATOT-11 fica Deferred (feature futura se a assimetria incomodar gestores restritos)
- Correção do sintoma = `ACESSO_TOTAL` faz `GET /folha-pagamento` (e demais consumidores da flag) liberar dados

### Agent's Discretion

- Ordem de early-return em `obterContextoAcesso` (checar `ACESSO_TOTAL` antes do fluxo de funcionário/nó)
- Valores de `temFuncionarioVinculado` / `temNoOrganograma` / `centrosCustoIds` quando `acessoTotal=true` sem vínculo — desde que consumidores consultem `acessoTotal` primeiro (padrão já existente)
- Cor do chip FE para `ACESSO_TOTAL` (sugestão: `error` ou `warning` para sinalizar privilégio alto)
- Número da próxima migration Flyway: `V1.15__...`

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma — A–D confirmadas pelo usuário (“seguir com essas opções”) em 2026-07-27.

---

## Specific References

- Bug report: resumo da folha ok; Ver funcionários vazio com usuário ADM
- Produto: permissão separada de `ADMIN`; concedível a qualquer usuário
- Rejeitado: funcionário fantasma “Admin” no organograma

---

## Deferred Ideas

- ACL no endpoint de resumo da folha (ATOT-11 / gray D)
- Redesign/enforcement do catálogo FE (`GESTOR`, `OPERADOR`, …)
- Hygiene privilege escalation `/usuarios`
