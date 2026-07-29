# folha-custo-clt-fix2 — Custo Techne por % de rubrica Specification

**Parent:** `_docs/specs/features/folha-custo-clt/` (supersedes composição D4-CLT / FCLT-06 encargos rateados)  
**Related:** `folha-custo-clt-fix1` (processamento pós-import — **escopo distinto**, Done), `beneficios-mensais` (INT-1), legacy Custo Techne  
**Complexity:** Large  
**Spec status:** Draft 2026-07-29 — **Tasks draft** (`tasks.md` T1–T15)

> **Nota de nomenclatura:** `folha-custo-clt-fix1` já existe e trata **encadeamento import ADP → processamento**. Esta feature é **`fix2`** (composição auditável de custo empresa).

## Problem Statement

O `custoEmpresa` implementado em `folha-custo-clt` soma **encargos rateados** do rodapé ADP (~R$ 1.860/funcionário) **sem linha no detalhe**, quebrando paridade card ↔ aba Custo e divergindo do **Custo Techne legado**, onde encargos entram via **`porcentagem` no cadastro de rubricas** (ex. Salário Base 138,63%). O campo `Rubrica.porcentagem` existe e é editável na UI, mas o motor **ignora** `%` no cálculo de custo. Gestores veem totais “corretos” em maio (com benefícios) e “incompletos” ou “inventados” em meses sem benefícios ou com rateio invisível.

## Goals

- [ ] Compor `custoEmpresa` como **(ficha ADP + fixas + calculadas: valor × op_custo × %/100) + benefícios**, auditável na aba Custo
- [ ] **Remover** encargos rateados ADP da composição de `custoEmpresa` (global e scoped)
- [ ] **Preservar** bruto/líquido com **valor original** da rubrica × operador — **sem** `porcentagem`
- [ ] Aplicar `porcentagem` do cadastro no **custo** para **FOLHA_ADP**, **CUSTO_FIXO** e **CALCULADO**
- [ ] Snapshot `porcentagem` em `ficha_linha` no processamento (todas as origens materializadas)
- [ ] Paridade card ↔ soma aba Custo ↔ API (`custoEmpresa` = `custoFolha` + `custoBeneficios`)
- [ ] Paridade **resumo ↔ cards**: `totalBruto` / `totalLiquido` / `totalCustoEmpresa` = soma dos totais por funcionário visíveis (± arredondamento)
- [ ] Manter ACL dual-path e INT-1/INT-2 (benefícios consulta; fixas materializadas)

## Out of Scope

| Feature | Reason |
| --- | --- |
| Alterar fórmula bruto/líquido (sem `%`) | Pedido explícito; só **valor original** × operador |
| Rateio encargos ADP no custo empresa | Superseded por FIX2-CTX-01 |
| Rateio encargos por escopo do gestor | Já rejeitado (B1); permanece fora |
| Importação automática do sistema legado | P3 / integração futura |
| Inventário completo de códigos legado (1100, 1500…) | FIX2-13 seed documentado; não bloqueia motor |
| Processamento pós-import | `folha-custo-clt-fix1` Done |
| PJ, 13º regras novas, portal holerite | Etapa 2+ |

---

## Composição de custo — modelo revisado

### Semântica de **valor original**

Todo totalizador parte do **valor monetário original** persistido na linha:

| Origem do valor | Campo |
| --- | --- |
| Import ADP | `folha_pagamento.valor` → `ficha_linha.valor` |
| Rubrica fixa | `funcionario_rubrica_fixa.valor` → `ficha_linha.valor` |
| Calculado (férias etc.) | valor gerado no processamento → `ficha_linha.valor` |

**Bruto e líquido** usam **sempre** esse valor original — **nunca** `valor × porcentagem`.

### Tabela por origem

| Origem | Exemplos | Custo empresa | Bruto / Líquido |
| --- | --- | --- | --- |
| **FOLHA_ADP** | Salário, proventos importados | `valor × op_custo × (%/100)` | `valor × op_bruto / op_liquido` (**sem %**) |
| **CUSTO_FIXO** | Housing, RH, ajuda cadastrada | `valor × op_custo × (%/100)` | `valor × op_bruto / op_liquido` (**sem %**) |
| **CALCULADO** | Férias 2,5 | idem custo com `%` | idem bruto/líquido sem `%` |
| **BENEFICIO** | VR, AM planilha RH | valor integral na **consulta** | **não entra** |

```text
valorOriginal = ficha_linha.valor   // import, fixa ou calculado

contribuicaoCusto = valorOriginal × operador_custo × (porcentagem / 100)   // default % = 100

custoFolha      = Σ contribuicaoCusto  (FOLHA_ADP + CUSTO_FIXO + CALCULADO)
custoBeneficios = BeneficioConsultaPort
custoEmpresa    = custoFolha + custoBeneficios

bruto   = Σ (valorOriginal × operador_bruto)      // sem porcentagem
liquido = Σ (valorOriginal × operador_liquido)    // sem porcentagem

// Resumo da folha (scoped e global) — paridade FCLT-ACL-11 / FIX2-CTX-09
totalBruto        = Σ salBruto        dos funcionários visíveis na competência
totalLiquido      = Σ salLiquido      dos funcionários visíveis
totalCustoEmpresa = Σ custoEmpresa    dos funcionários visíveis
```

**Não usar** `resumo.totalPagamentos` (rodapé ADP) como `totalBruto` quando fichas ou linhas operador-based existirem.

Decisões: `context.md` FIX2-CTX-01…09.

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Supersedes D4-CLT rateio | Encargos ADP fora de `custoEmpresa` | FIX2-CTX-01; operador | y |
| % só no custo | Bruto/líquido: valor original × operador | FIX2-CTX-02 | y |
| Fixas com % no custo | Mesma fórmula ADP | FIX2-CTX-08 | y |
| Snapshot % na linha | Copiar no processamento | FIX2-CTX-03 | y |
| Paridade card = aba | Soma detalhe = `custoEmpresa` | FIX2-CTX-04 | y |
| Paridade resumo = Σ cards | Bruto/líquido/custo no resumo = soma dos funcionários | FIX2-CTX-09 | y |
| `encargosRateados` DTO | Sempre `0` na composição (deprecated) | Compat API 1 release | y |
| `total_encargos` snapshot | Persiste na import; não compõe custo | Informativo ADP | y |
| Valores % legado (138,63) | Migração P2 FIX2-13 | FIX2-CTX-06 | y |
| Arredondamento | HALF_UP 2 casas por contribuição e totais | Paridade FCLT-08 | y |

**Open questions:** none — ver `context.md`.

---

## User Stories

### P1: Motor — custo com porcentagem (folha + fixas) ⭐ MVP

**User Story**: Como sistema, quero aplicar `porcentagem` do cadastro de rubricas **somente** no cálculo de custo (folha ADP, rubricas fixas e calculadas), usando **valor original** da linha, sem alterar bruto/líquido.

**Why P1**: Base da correção; campo `porcentagem` já existe no cadastro.

**Acceptance Criteria**:

1. (FIX2-01) WHEN motor calcular contribuição de custo de uma linha de ficha THEN SHALL usar `valorOriginal × operador_custo × (porcentagem/100)` com `porcentagem` default **100** se null
2. (FIX2-02) WHEN motor calcular bruto ou líquido THEN SHALL usar **`valorOriginal × operador_bruto` / `valorOriginal × operador_liquido`** — **sem** multiplicar por `porcentagem` (valor = import/cadastro, não valor escalado)
3. (FIX2-03) WHEN `porcentagem = 138.63`, `operador_custo = 1`, `valorOriginal = 7258.43` THEN contribuição custo SHALL ser **`10062.36`** (HALF_UP 2 casas)
4. (FIX2-04) WHEN processamento materializar `ficha_linha` (ADP, **CUSTO_FIXO** ou CALCULADO) THEN SHALL copiar `porcentagem` da rubrica mestre para snapshot na linha (Flyway se coluna inexistente)
5. (FIX2-05) WHEN processamento recalcular `ficha_mensal` THEN `custo_folha` persistido SHALL usar fórmula FIX2-01
6. (FIX2-20) WHEN linha `origemLinha=CUSTO_FIXO` com valor cadastrado `688.00` e rubrica `porcentagem=100` THEN contribuição custo SHALL ser **`688.00`** e bruto/líquido SHALL usar **`688.00 × operador_*`** sem `%`
7. (FIX2-21) WHEN aba Bruto ou Líquido listar linha THEN coluna **Valor** SHALL exibir `valorOriginal` e **Contribuição** SHALL **não** aplicar `porcentagem`

**Independent Test**: Fixa Housing + ADP salário 138,63% → custo com %; bruto/líquido iguais ao baseline pré-fix2 (valores originais).

---

### P1: Remover encargos rateados de `custoEmpresa` ⭐ MVP

**User Story**: Como gestor, quero que custo empresa seja a soma auditável de rubricas + benefícios, sem parcela invisível de rateio ADP.

**Why P1**: Elimina ~R$ 1.860 “fantasma”; fecha expectativa operador.

**Acceptance Criteria**:

1. (FIX2-06) WHEN qualquer endpoint calcular `custoEmpresa` THEN SHALL usar `FolhaCustoEmpresaComposer.compor(custoFolha, ZERO, custoBeneficios)` — **encargosRateados não somados**
2. (FIX2-07) WHEN `GET /folha-pagamento/totais-funcionarios` THEN campo `encargosRateados` SHALL ser **`0.00`** (deprecated, não usado na composição)
3. (FIX2-08) WHEN resumo global ou scoped agregar `totalCustoEmpresa` THEN SHALL **excluir** rateio `EncargosRateioService` da composição
4. (FIX2-09) WHEN `GET /dashboard/stats` THEN `custoMensalFolha` SHALL refletir composição FIX2-06 (sem rateio)
5. (FIX2-10) WHEN testes de regressão rodarem THEN SHALL falhar se `EncargosRateioService` for invocado para compor `custoEmpresa` (sensor discrimination)

**Independent Test**: Thyago mai/2026 — `custoEmpresa` ≈ soma aba Custo (folha com % + benefícios), **não** + R$ 1.860 rateio.

---

### P1: Detalhe e API — contribuição com % ⭐ MVP

**User Story**: Como gestor, quero ver na aba Custo a mesma contribuição que compõe o card.

**Why P1**: Auditabilidade FCLT-ACL-15 revisada.

**Acceptance Criteria**:

1. (FIX2-11) WHEN `GET /fichas/{id}/linhas?totalizer=COMPANY_COST` THEN `contribuicao` SHALL ser `valorOriginal × operador_custo × (porcentagem/100)` para linhas de ficha (incl. **CUSTO_FIXO**); benefícios `contribuicao = valor`
2. (FIX2-12) WHEN somar contribuições da aba Custo + benefícios listados THEN total SHALL igualar `custoEmpresa` do card (± R$ 0,01)
3. (FIX2-13) WHEN aba Custo THEN SHALL **não** listar linha de encargos rateados (removido; scoped/global igual)

**Independent Test**: Abrir detalhe Thyago mai/2026 — soma contribuições = valor do card.

---

### P1: ACL — paridade resumo ↔ cards ⭐ MVP

**User Story**: Como gestor, quero que Bruto, Líquido e Custo Empresa no **resumo** sejam a **soma** dos mesmos totais dos cards de funcionários na mesma competência.

**Why P1**: Estende FCLT-ACL-11; elimina uso de `totalPagamentos` ADP como bruto no resumo.

**Acceptance Criteria**:

1. (FIX2-22) WHEN `GET /resumo-folha-pagamento*` para competência com fichas/linhas THEN `totalBruto` SHALL equal **Σ `salBruto`** de `/totais-funcionarios` para o mesmo usuário, competência e `decimoTerceiro` (± R$ 0,01 acumulado)
2. (FIX2-23) WHEN mesmo contexto THEN `totalLiquido` SHALL equal **Σ `salLiquido`** dos cards (± R$ 0,01)
3. (FIX2-14) WHEN caminho scoped THEN `totalCustoEmpresa` resumo = soma `custoEmpresa` cards visíveis (± arredondamento)
4. (FIX2-24) WHEN `acessoTotal` e fichas existirem THEN resumo global SHALL agregar totais por **soma de fichas/cards**, **não** mapear `totalPagamentos` snapshot ADP para `totalBruto`
5. (FIX2-15) WHEN caminho scoped ou global THEN bruto/líquido por funcionário SHALL usar **valor original × operador** **sem** `%` (sem regressão RSF-01)
6. (FIX2-16) WHEN `acessoTotal` THEN resumo global `totalCustoEmpresa` SHALL agregar nova composição **sem** rateio ADP

**Independent Test**: `FolhaAclParidadeResumoCardsTest` estendido — scoped **e** global (`acessoTotal`) — PASS para bruto, líquido e custo.

---

### P2: Migração de `porcentagem` no cadastro ⭐

**User Story**: Como RH, quero percentuais legados (ex. 138,63% no Salário Base) no cadastro para custo bater com sistema anterior.

**Acceptance Criteria**:

1. (FIX2-17) WHEN migração/seed FIX2-13 rodar THEN SHALL documentar mapeamento mínimo (ex. rubrica `0010` → `138.63`) em comentário Flyway ou `_docs/temp/` operacional
2. (FIX2-18) WHEN operador editar `porcentagem` em Rubricas THEN valor SHALL persistir e refletir no custo **após reprocessar** competência

**Independent Test**: Após seed + reprocesso mai/2026 — Salário Base contribui R$ 10.062,36 no custo.

---

### P2: Frontend — decomposição opcional no card

**User Story**: Como operador, quero ver no card (ou tooltip) custo folha vs benefícios além do total.

**Acceptance Criteria**:

1. (FIX2-19) WHEN card exibir custo THEN MAY exibir `salCustoFolha` e `salCustoBeneficios` retornados pela API (subtexto); `encargosRateados` **não** exibido

**Independent Test**: Card Thyago mostra total = folha + benefícios visíveis.

---

## Edge Cases

- WHEN `porcentagem = 0` THEN contribuição custo da linha = 0 (linha omitida na aba se op_custo ≠ 0 mas contribuição 0)
- WHEN `operador_custo = 0` THEN linha não entra no custo **mesmo com % > 0**
- WHEN benefícios ausentes na competência THEN `custoEmpresa = custoFolha` only (jun/2026 Thyago)
- WHEN rubrica fixa vigente THEN contribuição **custo** usa `valorOriginal × op_custo × (%/100)`; bruto/líquido usam `valorOriginal` sem `%` (origem `CUSTO_FIXO`)
- WHEN reprocesso após alterar % THEN totais atualizam; consulta **não** lê % live do cadastro
- WHEN competência sem ficha processada THEN resumo MAY usar fallback de linhas ADP com operadores; **nunca** usar `totalPagamentos` snapshot como `totalBruto` se linhas operador-based existirem
- WHEN `total_encargos` no snapshot ADP > 0 THEN permanece no resumo ADP como campo informativo **sem** impacto em `custoEmpresa`

---

## Implicit-requirement dimensions (Large)

| Dimension | Resolution |
| --- | --- |
| Auth boundaries | ACL inalterada; mesmos endpoints |
| Failure / partial-failure | Processamento @Transactional; rollback import+process (fix1) preservado |
| Idempotency | Reprocessar substitui ficha e snapshots de % |
| Concurrency | Serialização por competência (existente) |
| Observability | Log domínio `folha` ao recalcular com nova fórmula |
| Input validation | `porcentagem ≥ 0` no cadastro rubrica |
| External deps | Import ADP inalterado; `total_encargos` persistido mas não compõe custo |
| Data lifecycle | Snapshot % na linha; cadastro mestre exige reprocesso |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FIX2-01 | P1 motor % custo | T2 | Done |
| FIX2-02 | P1 bruto/líquido inalterados | T2 | Done |
| FIX2-03 | P1 caso 138,63% | T2 | Done |
| FIX2-04 | P1 snapshot % linha | T1,T3,T4 | Done |
| FIX2-05 | P1 custo_folha persistido | T3 | Done |
| FIX2-20 | P1 fixas % custo | T3 | Done |
| FIX2-21 | P1 detalhe bruto/líquido valor original | T7 | Done |
| FIX2-06 | P1 composer sem encargos | T5,T6 | Done |
| FIX2-07 | P1 DTO encargos 0 | T5 | Done |
| FIX2-08 | P1 resumo sem rateio | T6,T8 | Done |
| FIX2-09 | P1 dashboard | T10,T11 | Done |
| FIX2-10 | P1 sensor rateio | T5 | Done |
| FIX2-11 | P1 API detalhe contribuição | T7 | Done |
| FIX2-12 | P1 paridade aba = card | T7 | Done |
| FIX2-13 | P1 sem linha encargos | T7 | Done |
| FIX2-14 | P1 ACL paridade custo scoped | T9 | Done |
| FIX2-15 | P1 ACL bruto/líquido sem % | T9 | Done |
| FIX2-16 | P1 ACL resumo global custo | T8,T9 | Done |
| FIX2-22 | P1 resumo totalBruto = Σ cards | T9 | Done |
| FIX2-23 | P1 resumo totalLiquido = Σ cards | T9 | Done |
| FIX2-24 | P1 resumo não usa pagamentos ADP como bruto | T8 | Done |
| FIX2-17 | P2 seed % legado | T12 | Done |
| FIX2-18 | P2 cadastro + reprocesso | T3 | Done |
| FIX2-19 | P2 FE decomposição | T13 | Done |

**Coverage:** 24 requirements mapped → T1–T15 (Execute complete, pre-Verifier)

**Cross-ref:** Atualizar `folha-custo-clt/spec.md` composição e FCLT-06/FCLT-13 na Execute de `fix2` (nota superseded, não reabrir feature pai).

---

## Success Criteria

- [ ] Thyago mai/2026: `custoEmpresa` card = soma aba Custo (com % no salário após seed) + benefícios — **sem** + rateio ADP
- [ ] Bruto/líquido Thyago mai/jun inalterados vs baseline pré-fix2 (**valor original**, sem `%`)
- [ ] Rubrica fixa (ex. RH R$ 688) entra no custo com `%` do cadastro após processamento
- [ ] Teste discrimination mata reintrodução de rateio em `custoEmpresa`
- [ ] Gestor scoped **e** `acessoTotal`: `totalBruto` resumo = Σ `salBruto` dos cards (± R$ 0,01)
- [ ] `totalLiquido` resumo = Σ `salLiquido` dos cards
- [ ] Campo `porcentagem` no cadastro passa a impactar **somente** custo após reprocesso
