# Folha CLT — Bruto, Líquido e Custo Empresa — Context

**Feature:** `folha-custo-clt`  
**Source:** `_docs/folha-salarial/especificacao-etapa-1-clt.md`  
**Related:** `acl-scoped-folha-resumo` (RSF-01…08, B1), AD-007, AD-011  
**Captured:** 2026-07-28

---

## Gray areas resolved

### ACL-1: Fonte dos três totalizadores com escopo parcial

**Question:** Usuário scoped pode ler totais persistidos em `ficha_mensal` / snapshot global?

**Decision:** **Não.** Mesmo padrão de `acl-scoped-folha-resumo`: escopo parcial **recalcula on-the-fly** a partir de linhas filtradas por `centrosCustoIds`. Totais persistidos na ficha são canônicos **somente** para `acessoTotal=true`.

**Rationale:** Snapshot e ficha não têm fatia por centro de custo; expor totais globais quebraria paridade com “Ver funcionários”.

---

### ACL-2: Encargos patronais no escopo parcial

**Question:** Ratear encargos proporcionalmente ao escopo do gestor?

**Decision:** **Não** — manter **B1** de `acl-scoped-folha-resumo`: `totalEncargos = 0` e `totalEncargosRateados = 0` no caminho scoped.

**Rationale:** Encargos vêm do rodapé global ADP; rateio por escopo foi explicitamente rejeitado na feature ACL. Custo empresa scoped = custo_folha (linhas) + benefícios mensais (escopo) + **0 encargos**.

---

### ACL-3: Encargos patronais com acesso total

**Question:** Como alocar encargos por funcionário?

**Decision:** **Opção A (D4-CLT)** — rateio proporcional ao **bruto CLT** de cada funcionário na competência.

**Rationale:** Simples, determinístico; recomendação da spec etapa 1 §6.2. Só aplica quando `acessoTotal=true`.

---

### ACL-4: Paridade resumo ↔ cards

**Question:** Totais do resumo scoped devem bater com a soma dos cards da mesma competência?

**Decision:** **Sim.** Para o mesmo usuário, competência e flag `decimoTerceiro`, soma de `bruto`/`liquido`/`custoEmpresa` dos funcionários visíveis SHALL igualar os totais do resumo scoped (tolerância de arredondamento ±R$ 0,01 por funcionário acumulado).

**Rationale:** Fecha o buraco que motivou `acl-scoped-folha-resumo`; evita gestor ver resumo ≠ drill-down.

---

### INT-1: Benefícios mensais — composição na consulta

**Question:** Benefícios entram persistidos na `ficha_mensal` no pós-import ou são buscados na consulta?

**Decision:** **Buscar e compor na consulta** — via `BeneficioConsultaPort`, no mesmo request que calcula/exibe custo empresa. **Não** gravar `beneficio_mensal` como `ficha_linha` nem congelar `total_beneficios` na ficha como fonte canônica.

**Fórmula na consulta (por funcionário × competência):**

```text
custoBeneficios = BeneficioConsultaPort.somarValorPorFuncionarioECompetencia(...)
custoEmpresa    = custoFolha + encargosRateados + custoBeneficios
                  (encargosRateados = 0 no caminho scoped)
```

**O que persiste na ficha (pós-import):** `bruto`, `liquido`, `custo_folha` (+ `encargos_rateados` opcional para cache global). **`custo_empresa` exibido = sempre composto na leitura.**

**Rationale:** Benefícios são módulo e importação separados; composição na consulta reflete lançamentos atualizados sem reprocessar folha; alinha com ACL scoped (benefícios filtrados por CC na mesma camada de leitura).

**UI aba Custo:** rubricas de `ficha_linha` (operador_custo ≠ 0) **+** lista de `beneficio_mensal` obtida na consulta (não misturar com rubricas ADP).

---

### INT-2: Custos fixos Techne — cadastro + injeção no processamento

**Question:** Como tratar custos que não vêm de folha ADP nem de `beneficio_mensal` (ajuda de custo, housing, viagens, férias proporcionais)?

**Decision:** **Terceira origem**, distinta de INT-1:

1. **Cadastro** em `funcionario_rubrica_fixa` (funcionário × rubrica × valor × vigência).
2. **Injeção** no `POST /folha-pagamento/processar` como `ficha_linha` com `origemLinha=CUSTO_FIXO` (ou `CALCULADO` para férias 2,5 e regras automáticas P3).
3. **Persistido** na ficha — entra em `bruto`/`liquido`/`custo_folha` via operadores da rubrica.
4. **Consulta** lê linhas já materializadas; **não** busca cadastro de custo fixo ad hoc (unlike BENEFICIO).
5. **Alteração** no cadastro exige **reprocessar** a competência para refletir nos totais.

**Não confundir com:**

| | Custos fixos (INT-2) | Benefícios mensais (INT-1) |
| --- | --- | --- |
| Fonte | Cadastro RH / cálculo processamento | Planilha `beneficio_mensal` |
| Persistência na ficha | Sim (`ficha_linha`) | Não |
| Quando compõe custo | Materializado no processamento | Na consulta |
| Impacto bruto/líquido | Sim (operadores) | Não — só `custoEmpresa` |

**UI:** tela **Rubricas Fixas** (CRUD `funcionario_rubrica_fixa`). Aba detalhe rotula `origemLinha`.

**ACL:** linhas `CUSTO_FIXO` / `CALCULADO` seguem filtro por CC do funcionário — sem caminho especial.

**Rubricas calculadas avançadas** (dissídio, VR automático, housing condicional): P3 — mesma mecânica `origemLinha=CALCULADO`, sem valor no cadastro fixo.

---

### ACL-5: Benefícios no custo empresa scoped

**Question:** Benefícios entram no custo scoped?

**Decision:** **Sim**, somando apenas `beneficio_mensal` de funcionários cujo centro ∈ `centrosCustoIds`, **buscados na consulta** via `BeneficioConsultaPort` (mesma regra já existente em `BeneficioMensalService`).

**Rationale:** Paridade com folha; benefícios já filtram por organograma; composição na leitura garante escopo correto.

---

### ARCH-1: `folha_pagamento` vs `ficha_mensal` (D5-CLT)

**Question:** Qual fonte de verdade na transição?

**Decision:** **Opção B inicial** — `folha_pagamento` permanece fonte da importação ADP; `ficha_mensal`/`ficha_linha` é **projeção materializada pós-import** com totais persistidos. Consulta scoped continua podendo recalcular de linhas (`folha_pagamento` ou `ficha_linha`) sem ler totais persistidos.

**Rationale:** Menor big-bang; importação atual intacta; ficha prepara evolução futura.

**Follow-up:** Opção A (import grava direto em ficha) pode ser fase 2 desta feature se estável.

---

### ARCH-2: Operadores iniciais (D3-CLT)

**Decision:** Migração deriva operadores de `tipo_rubrica` (PROVENTO +1/+1/+1, DESCONTO 0/−1/0, INFORMATIVO 0/0/0); UI de cadastro permite override.

---

### FE-1: Cálculo no frontend

**Decision:** Frontend **não recalcula** bruto/líquido/custo — consome API (`/totais-funcionarios`, `/fichas`, resumo estendido). Remove agregação local atual em `FolhaPagamento/index.tsx`.

---

## Open questions

none — ACL-1…5, ARCH-1/2, FE-1, INT-1, INT-2 logged above.
