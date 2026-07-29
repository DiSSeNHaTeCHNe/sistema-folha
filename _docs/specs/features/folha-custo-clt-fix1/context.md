# folha-custo-clt-fix1 — Processamento pós-import ADP (context)

**Parent:** `folha-custo-clt` (fecha P2 do design: hook pós-import)  
**Date:** 2026-07-28

---

## FIX1-CTX-01 — Onde fica o botão manual?

**Question:** Em qual tela o operador reprocessa uma competência?

**Options considered:**

| Opção | Prós | Contras |
| ----- | ---- | ------- |
| A — Cadastros → **Importação** | Mesmo fluxo operacional; competência já conhecida após upload | Não visível na tela Folha |
| B — Folha de Pagamento | Próximo do detalhe que exige ficha | Mistura consulta com operação admin |
| C — Ambas | Máxima visibilidade | Duplicação UI |

**Decision:** **A** — seção **“Processar ficha da competência”** na página `/importacao` (menu Cadastros), abaixo do card Folha ADP.

**Rationale:** Operador RH já importa ali; reprocessamento manual é exceção (custos fixos alterados, férias recalcular). Folha permanece somente consulta.

---

## FIX1-CTX-02 — Sucesso da importação vs falha do processamento

**Question:** Se ADP gravou mas processamento falhou, o que o usuário vê?

**Decision:** **Uma transação de orquestração** — `ImportacaoFolhaAdpService` chama processamento via port **no mesmo `@Transactional`** após `persistirImportacao`. Se processamento falhar, **rollback inclui importação**; API retorna **4xx/5xx**, UI **não** exibe toast de sucesso.

**Rationale:** Atende “só retornar como feito ao terminar os dois”; evita estado 5/2026 (3928 linhas ADP, 0 fichas).

**Trade-off:** Importações grandes reexecutam parse+persistência em retry — aceitável para volume atual.

---

## FIX1-CTX-03 — Chamada cross-domain (AD-010)

**Question:** Importação pode chamar `FolhaProcessamentoService` direto?

**Decision:** **Não** — novo **`FolhaProcessamentoPort`** em `folha.port` com adapter em `folha.application`; `importacao.application` injeta só a port.

**Rationale:** ArchUnit AD-010; espelha `FolhaImportacaoPort` existente.

---

## FIX1-CTX-04 — `recalcularFerias` no hook automático

**Decision:** **`false`** no pós-import automático (mesmo default de `ProcessamentoOpcoes`). Checkbox **opcional** apenas no botão manual.

**Rationale:** Férias calculadas são decisão explícita do operador; não surpreender na importação em massa.

---

## FIX1-CTX-05 — Payload de resposta unificado

**Decision:** Estender `ImportacaoFolhaAdpResponseDTO` com campos opcionais de processamento (`fichasProcessadas`, `linhasProcessadas`, `mensagem` composta). Manter `POST /folha-pagamento/processar` para reprocesso manual.

**Rationale:** UI de importação mostra resultado dos dois passos numa mensagem; reprocesso usa endpoint existente.

---

## FIX1-CTX-06 — Relação com FCLT-04

**Decision:** `folha-custo-clt-fix1` **refina** FCLT-04: “WHEN importação ADP concluir” passa a incluir processamento na mesma operação. Atualizar traceability em `folha-custo-clt/spec.md` na fase Execute de `folha-custo-clt-fix1` (não reabrir ARCH-1 Opção B — projeção continua materializada, só o **gatilho** muda).

---

## Deferred

- Processamento assíncrono / fila para competências muito grandes (P3).
- Botão duplicado em Folha de Pagamento (C acima).
