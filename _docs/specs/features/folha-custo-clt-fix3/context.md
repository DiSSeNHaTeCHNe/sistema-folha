# folha-custo-clt-fix3 Context

**Gathered:** 2026-07-29  
**Spec:** `_docs/specs/features/folha-custo-clt-fix3/spec.md`  
**Status:** Ready for design

---

## FIX3-CTX-01: Funcionário opcional → fixa global

**Decision:** Campo funcionário **opcional** no cadastro. `funcionario_id = null` = fixa **global**.

**Behavior:**
- Mesmo `valor`, `rubrica`, vigência aplicados a **cada** ficha CLT gerada no processamento da competência.
- Universo = funcionários CLT que **já entram no loop de processamento** (possuem linhas ADP na competência) — não cria ficha órfã.

**Rationale:** Pedido explícito do usuário; superset do modelo individual (INT-2).

---

## FIX3-CTX-02: Prioridade em conflito de rubrica

**Decision:** Para mesma `rubrica_id` na mesma ficha:

1. Linha **ADP** existente → ignora qualquer fixa (WARN log).
2. Senão: fixa **individual** vigente → usa individual.
3. Senão: fixa **global** vigente → aplica global.

**Rationale:** Exceção específica prevalece sobre regra geral; preserva FCLT-23.

---

## FIX3-CTX-03: Vigência e sobreposição global

**Decision:**
- Individual: sobreposição proibida para `(funcionario_id, rubrica_id, vigência)` — comportamento atual.
- Global: sobreposição proibida para `(rubrica_id, vigência)` onde `funcionario_id IS NULL`.
- Individual e global **podem coexistir** na mesma rubrica (prioridade CTX-02).

**Rationale:** Evita duas RH globais conflitantes; permite exceção por funcionário.

---

## FIX3-CTX-04: Percentual na listagem vs detalhe

| Superfície | Fonte do % | Motivo |
| --- | --- | --- |
| Listagem Rubricas Fixas | **Live** `rubricas.porcentagem` | Operador vê % atual do cadastro mestre (“dinâmico”) |
| Detalhe ficha (3 abas) | **Snapshot** `ficha_linha.porcentagem` | Auditável vs competência processada (fix2) |

**Rationale:** Pedido usuário listagem dinâmica; detalhe deve bater com motor da competência.

---

## FIX3-CTX-05: Coluna Percentual nas abas Bruto/Líquido

**Decision:** Exibir snapshot `%` entre Valor e Contribuição em **todas** as abas. Em Bruto/Líquido a contribuição **não** multiplica por `%` (fix2-02); coluna é **informativa**.

**Display:** `100%` quando null; `—` para BENEFICIO.

---

## FIX3-CTX-06: Agrupamento por origem (Bruto/Líquido)

**Decision:** Reutilizar `ORIGEM_LABELS` e layout da aba Custo para Bruto e Líquido:

- `FOLHA_ADP` → “Folha ADP”
- `CUSTO_FIXO` → “Custo Fixo”
- `CALCULADO` → “Calculado”
- `BENEFICIO` → “Benefício” (somente aba Custo)

Ordem sugerida dos grupos: FOLHA_ADP, CUSTO_FIXO, CALCULADO, BENEFICIO (Custo).

---

## FIX3-CTX-07: Subtotais e total da aba

**Decision:**
- **Subtotal por origem:** soma das `contribuicao` das linhas do grupo (HALF_UP 2 casas por linha, soma decimal).
- **Total da aba:** soma de todas as contribuições exibidas na aba.
- **Paridade card:** Total aba Bruto = `salBruto`; Líquido = `salLiquido`; Custo = `custoEmpresa` do `FuncionarioResumo` aberto no dialog (± R$ 0,01).

**Rationale:** Pedido usuário; reforça FIX2-12/22/23 no FE.

---

## FIX3-CTX-08: Formulário Rubricas Fixas — ordem

**Decision:** Ordem fixa dos campos:

1. Rubrica (required)  
2. Valor  
3. Vigência Início (required)  
4. Vigência Fim  
5. Funcionário (optional — “Todos os funcionários”)  
6. Comentário  

**Rationale:** Pedido explícito usuário.

---

## FIX3-CTX-09: Listagem — coluna Funcionário

**Decision:** `funcionario_id` null → exibir **“Todos”** (não vazio, não “-”).

---

## FIX3-CTX-10: API DTO listagem fixas

**Decision:** Estender `FuncionarioRubricaFixaDTO` com `porcentagem` (read-only, join rubrica) **ou** FE resolve via mapa de rubricas já carregado — preferência design: **campo no DTO** para listagem consistente sem N+1 no FE.

**Open for design:** adapter populates `porcentagem` from `Rubrica` on read.

---

## Deferred Ideas

- Fixa global filtrada por centro de custo / linha de negócio (escopo organograma).
- `%` override por vínculo fixo.
- Export CSV detalhe com subtotais.
