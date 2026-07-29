# folha-custo-clt-fix2 — Custo Techne por % de rubrica (context)

**Parent:** `folha-custo-clt` (supersede D4-CLT / encargos rateados na composição de `custoEmpresa`)  
**Related:** `folha-custo-clt-fix1` (processamento pós-import — escopo distinto, Done)  
**Gathered:** 2026-07-29 (refinado — valor original bruto/líquido; % custo incl. fixas; resumo = Σ cards)  
**Status:** Ready for design

---

## FIX2-CTX-01 — Composição de `custoEmpresa` (substitui D4-CLT)

**Question:** Como compor custo empresa — rateio do rodapé ADP vs modelo legado Custo Techne?

**Options considered:**

| Opção | Descrição | Prós | Contras |
| ----- | --------- | ---- | ------- |
| A — Manter spec atual | `custoFolha + encargosRateados + benefícios` | Já implementado | Invisível no detalhe; card ≠ aba Custo; diverge do legado |
| B — **Custo Techne auditável** | `(ficha × operador_custo × porcentagem/100) + benefícios` | Paridade card↔aba; alinha legado (ex. 138,63% no salário); encargos embutidos na % | Exige migração de `porcentagem` no cadastro |
| C — Híbrido | % na ficha + rateio ADP | Soma encargos duas vezes | Rejeitado |

**Decision:** **Opção B.**

**Fórmula canônica (por funcionário × competência):**

```text
contribuicaoCusto(linha) = valor × operador_custo × (porcentagem / 100)

custoFolha     = Σ contribuicaoCusto das ficha_linha (FOLHA_ADP + CUSTO_FIXO + CALCULADO)
custoBeneficios = BeneficioConsultaPort (INT-1, inalterado)
custoEmpresa   = custoFolha + custoBeneficios
```

**Encargos rateados (`EncargosRateioService` / `total_encargos` snapshot):** **não** entram em `custoEmpresa`. Campo `encargosRateados` no DTO permanece **deprecated**, sempre `0` na composição (compatibilidade de contrato 1 release).

**Rationale:** Operador validou que custo = imagem da aba Custo (folha + benefícios + fixas) + % do cadastro; rateio ~R$ 1.860 “parece inventado” por não aparecer como rubrica. Legado usa **138,63%** no Salário Base em vez de rateio global.

---

## FIX2-CTX-02 — Valor original em bruto/líquido; `porcentagem` só no custo

**Question:** `Rubrica.porcentagem` impacta quais totalizadores? Qual “valor” entra em cada totalizador?

**Decision:**

| Totalizador | Base de cálculo | Fórmula | Altera nesta feature? |
| ----------- | ----------------- | ------- | --------------------- |
| **Bruto** | **Valor original** da linha (`ficha_linha.valor` importado/cadastrado) | `Σ valor × operador_bruto` | **Não** (sem `%`) |
| **Líquido** | **Valor original** da linha | `Σ valor × operador_liquido` | **Não** (sem `%`) |
| **Custo (ficha)** | Valor original × `%` do **cadastro da rubrica** | `Σ valor × operador_custo × (porcentagem/100)` | **Sim** |

**Valor original:** montante persistido na linha — import ADP (`folha_pagamento.valor`) ou valor cadastrado em `funcionario_rubrica_fixa.valor` — **nunca** `valor × porcentagem` para bruto/líquido.

**Default:** `porcentagem` null ou ausente na linha → tratar como **100** **somente** no cálculo de custo.

**Rationale:** Operador (2026-07-29): bruto e líquido usam valores originais das rubricas; no **custo**, todas as rubricas da ficha (ADP + fixas + calculadas) aplicam `valor + porcentagem` do cadastro — equivalente ao “Op.%” do Custo Techne legado.

---

## FIX2-CTX-03 — Snapshot de `porcentagem` em `ficha_linha`

**Question:** Consulta usa % live do cadastro ou snapshot na ficha?

**Decision:** **Snapshot no processamento** — ao materializar `ficha_linha`, copiar `porcentagem` da rubrica (mesmo padrão FCLT-07 para operadores). Recalcular `ficha_mensal.custo_folha` com a fórmula nova. Alteração de % no cadastro exige **reprocessar** competência (não consulta ad hoc).

**Rationale:** Paridade com operadores; auditoria histórica; alinha INT-2 (fixas materializadas no processamento).

**Escopo do snapshot:** `porcentagem` copiada da rubrica mestre para **todas** as origens materializadas — `FOLHA_ADP`, **`CUSTO_FIXO`** e **`CALCULADO`**.

---

## FIX2-CTX-08 — Rubricas fixas e `porcentagem` no custo

**Question:** Rubricas fixas (`funcionario_rubrica_fixa`) entram no custo com ou sem `%`?

**Decision:** **Com `%` do cadastro da rubrica**, mesma regra das linhas ADP:

```text
contribuicaoCusto (CUSTO_FIXO) = funcionario_rubrica_fixa.valor × operador_custo × (porcentagem_rubrica / 100)
```

- `valor` na linha = valor cadastrado na fixa (original).
- `porcentagem` = snapshot da `Rubrica` no processamento (ex. Housing 100%, Salário-base-like fixas conforme cadastro).
- Bruto/líquido de linhas `CUSTO_FIXO` continuam `valor × operador_*` **sem** `%`.

**Rationale:** Operador: “rubricas fixas, adicionar porcentagem para o cálculo” — apenas no eixo **custo**, não em bruto/líquido.

---

## FIX2-CTX-04 — Paridade card ↔ aba Custo ↔ API

**Question:** Soma das linhas visíveis deve fechar com `custoEmpresa`?

**Decision:** **Sim.** Para o mesmo funcionário/competência/`decimoTerceiro`:

```text
custoEmpresa (card/API) = Σ contribuicaoCusto (aba Custo, ficha) + Σ valor (aba Custo, BENEFICIO)
```

Tolerância ±R$ 0,01 por funcionário (arredondamento HALF_UP 2 casas).

**UI (P2):** Card pode continuar exibindo só `custoEmpresa`; detalhe e API expõem decomposição (`salCustoFolha`, `salCustoBeneficios`). Encargos rateados **não** aparecem em nenhuma aba.

---

## FIX2-CTX-05 — ACL e resumo (herança `folha-custo-clt`)

**Decision:**

- **Scoped:** recalcular de linhas filtradas; benefícios via port escopado; encargos fora de `custoEmpresa`.
- **Global (`acessoTotal`):** agregar por soma de fichas/cards — **sem** rateio ADP no custo.
- **Dashboard:** KPI custo mensal = mesma composição FIX2-01 (sem rateio).
- **`total_encargos` snapshot ADP:** informativo; **não** compõe `custoEmpresa`.

**Rationale:** Remove encargos rateados invisíveis; scoped/global divergem só por escopo.

---

## FIX2-CTX-09 — Resumo: Bruto/Líquido/Custo = soma dos funcionários

**Question:** O que exibir em `totalBruto` (e demais totalizadores) na tela **Resumo da Folha**?

**Options considered:**

| Opção | Descrição | Prós | Contras |
| ----- | --------- | ---- | ------- |
| A — Rodapé ADP | `totalBruto = totalPagamentos` do snapshot import | Já existe no fallback | ≠ soma dos cards; confunde gestor |
| B — **Σ cards** | `totalBruto = Σ salBruto` dos funcionários visíveis | Paridade resumo ↔ “Ver Funcionários”; auditável | Exige ficha/linhas processadas |
| C — Máximo dos dois | Maior valor | — | Sem sentido de negócio |

**Decision:** **Opção B** — para o mesmo usuário, competência e `decimoTerceiro`:

```text
totalBruto        = Σ salBruto        (cards / totais-funcionarios)
totalLiquido      = Σ salLiquido
totalCustoEmpresa = Σ custoEmpresa
```

Tolerância ±R$ 0,01 acumulada (HALF_UP 2 casas por funcionário).

**Implementação:** `ResumoFolhaPagamentoService` SHALL **não** mapear `resumo.totalPagamentos` → `totalBruto` quando existirem `ficha_mensal` ou linhas para recalcular com operadores. Estende **FCLT-ACL-11** para **`acessoTotal`** (hoje testado só scoped).

**Colunas legado ADP** (`totalPagamentos`, `totalDescontos`) permanecem no DTO como **informativas** da importação; **não** substituem os totalizadores operador-based.

**Rationale:** Operador (2026-07-29): “No resumo, Bruto deve ser a composição do bruto de todos os funcionários.”

---

## FIX2-CTX-06 — Migração de dados `porcentagem` (cadastro)

**Question:** Quem define 138,63% no Salário Base?

**Decision:** **P2 desta feature** — script/migração Flyway ou seed operacional documentado; **não** hardcode no motor. Spec fix2 entrega **mecanismo**; valores legado (ex. `0010` → 138,63) são tarefa de dados separada (FIX2-13) com evidência em ambiente de homologação.

**Rationale:** Separa regra de cálculo de carga de migração RH; evita assumir todos os códigos legado (1100, 1200, 1500…) sem inventário.

---

## FIX2-CTX-07 — Relação com `folha-custo-clt-fix1`

**Decision:** `fix1` = gatilho processamento pós-import (**Done**, escopo fechado). **`fix2`** = composição de custo (**esta feature**). Não reabrir fix1.

---

## Deferred

- Import automático de cadastro legado “Resumo Rubricas Custo Techne” (integração sistema antigo).
- Reintroduzir rateio ADP como linha informativa opcional no detalhe.
- Card FE com breakdown explícito (folha / benefícios) — P2 opcional FIX2-12.
