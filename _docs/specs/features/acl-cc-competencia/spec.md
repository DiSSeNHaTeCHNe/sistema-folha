# ACL — Centro de Custo por Competência Specification

**Parent / related:** `acl-scoped-folha-resumo`, `modular-acl-security-fix`, AD-007, AD-011  
**Complexity:** Medium  
**Spec status:** Amended 2026-07-29 — round 3 (FCC-26 ficha drill-down ACL)

## Problem Statement

O filtro de acesso pelo organograma em **folha** e **benefícios** usa o centro de custo **atual** do cadastro do funcionário (`funcionarios.centro_custo_id`), ignorando o CC **gravado na linha da competência** (`folha_pagamento.centro_custo_id`, preenchido na importação ADP).

Se um funcionário muda de CC entre meses, gestores com escopo parcial veem competências passadas no CC errado: quem deveria ver janeiro no CC-A deixa de ver, e quem não deveria ver passa a ver. Resumo scoped e dashboard herdam o mesmo critério incorreto via `FolhaConsultaAdapter`.

## Goals

- [x] ACL de folha e benefícios SHALL usar CC **da competência** (`COALESCE` linha → funcionário)
- [x] Gestor com escopo parcial SHALL ver apenas lançamentos cujo CC efetivo ∈ `centrosCustoIds`
- [x] Usuário com `acessoTotal=true` SHALL continuar sem filtro por CC
- [x] Resumo scoped, dashboard, stats por centro e endpoint por CC SHALL usar o mesmo critério
- [x] Benefícios SHALL persistir CC na linha no momento da criação (snapshot)

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Histórico de CC no cadastro do funcionário (tabela de vínculos) | Escopo maior; linha congela CC por competência |
| Alterar importação ADP (já grava CC na linha da folha) | Bug está na leitura/filtro |
| Backfill / migração de dados legados | Fase de desenvolvimento; D = não se preocupar |
| Mudança de JWT / organograma / roles | Contexto via `OrganogramaAcessoPort` inalterado |
| Frontend além do necessário | API devolve DTO já filtrado; contrato inalterado |
| Reprocessamento retroativo de benefícios existentes sem CC | Fallback A2; snapshot só em novos creates |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| A1 — Fonte do CC para ACL | `COALESCE(linha.centroCusto, funcionario.centroCusto)` | A2 confirmado | y |
| A2 — Benefícios no feature | Sim — coluna `centro_custo_id` + snapshot no create | B2 confirmado | y |
| A3 — Endpoint por CC | Filtrar por `folha.centro_custo_id` | C1 confirmado | y |
| A4 — Dados legados | Sem backfill; fallback A2 | Dev phase (D) | y |
| A5 — Linha/lançamento e funcionário sem CC | Negar acesso scoped | Paridade com deny atual | y |
| A6 — Re-importação folha | Linha nova recebe CC do cadastro na hora do import | Import já faz isso | y |
| A7 — `acessoTotal` | Sem filtro por CC | AD-011 | y |
| A8 — Benefício update | CC da linha **não** muda em edição (snapshot imutável por competência) | Paridade folha importada | y |
| A9 — Ficha processada | Snapshot `ficha_mensal.centro_custo_id` no processamento (CC efetivo das linhas ADP do grupo) | Opção A confirmada 2026-07-29 | y |
| A10 — Ficha legado | Sem backfill; fallback A1 via funcionário | Dev phase (D) | y |
| A11 — Reprocessamento ficha | Nova ficha SHALL receber CC snapshot na hora do processamento | Paridade A6 | y |

**Open questions:** none — round 2 fechada 2026-07-29 (`context.md` E).

---

## User Stories

### P1: ACL folha usa CC da competência ⭐ MVP

**User Story**: Como gestor com escopo parcial no organograma, quero que folhas de meses passados respeitem o centro de custo **daquele mês**, para que transferências entre CCs não distorçam o que vejo.

**Why P1**: Corrige vazamento/ocultação de dados históricos.

**Acceptance Criteria**:

1. **(FCC-01)** WHEN usuário **sem** `acessoTotal` consultar folha (`GET /folha-pagamento*`) THEN o sistema SHALL incluir apenas linhas ativas cujo CC efetivo (`COALESCE(folha.centro_custo_id, funcionario.centro_custo_id)`) ∈ `centrosCustoIds`
2. **(FCC-02)** WHEN funcionário esteve no CC-A em jan/2026 e no CC-B em fev/2026 (linhas importadas com CCs distintos) AND gestor tem escopo só CC-A THEN jan SHALL aparecer no escopo AND fev SHALL **not** aparecer
3. **(FCC-03)** WHEN gestor tem escopo só CC-B THEN fev SHALL aparecer AND jan SHALL **not** aparecer (inverso de FCC-02)
4. **(FCC-04)** WHEN usuário com `acessoTotal=true` consultar folha THEN o sistema SHALL retornar todas as linhas ativas (sem filtro por CC)
5. **(FCC-05)** WHEN linha não tiver CC na folha THEN o sistema SHALL usar `funcionario.centro_custo_id` para ACL (fallback A1)
6. **(FCC-06)** WHEN testes unitários de `FolhaPagamentoService` e `FolhaConsultaAdapter` forem executados THEN SHALL existir caso que **falharia** se ACL usasse só CC atual do funcionário (funcionário mudou de CC após import)

**Independent Test**: Simular duas competências com CCs diferentes na linha; funcionário com CC atual = B; gestor CC-A vê só jan.

---

### P1: Resumo scoped e dashboard alinhados ⭐ MVP

**User Story**: Como gestor com escopo parcial, quero que resumo da folha, evolução mensal e stats por centro usem o **mesmo critério** da listagem.

**Why P1**: RSF-01 de `acl-scoped-folha-resumo` exige “mesmo critério ACL”; hoje quebra após transferência.

**Acceptance Criteria**:

1. **(FCC-07)** WHEN usuário scoped consultar `GET /resumo-folha-pagamento*` THEN totais SHALL agregar apenas linhas cujo CC efetivo ∈ escopo (regra FCC-01)
2. **(FCC-08)** WHEN usuário scoped consultar `GET /dashboard/stats` THEN `evolucaoMensal`, stats por centro e por linha SHALL usar CC efetivo da linha (via `FolhaConsultaAdapter`), não CC atual do funcionário
3. **(FCC-09)** WHEN competência existir no snapshot global mas **nenhuma** linha no escopo (CC histórico) THEN resumo scoped SHALL retornar zeros (RSF-04 preservado)

**Independent Test**: Cenário FCC-02; resumo jan scoped CC-A > 0; fev scoped CC-A = zeros.

---

### P1: ACL benefícios com CC da competência ⭐ MVP

**User Story**: Como gestor com escopo parcial, quero que benefícios mensais respeitem o CC **da competência do lançamento**, não o CC atual do funcionário.

**Why P1**: B2 confirmado; paridade com folha.

**Acceptance Criteria**:

1. **(FCC-13)** WHEN um benefício mensal for criado THEN o sistema SHALL persistir `beneficio_mensal.centro_custo_id` = CC do funcionário **no momento da criação** (snapshot)
2. **(FCC-14)** WHEN usuário **sem** `acessoTotal` consultar benefícios (`GET /beneficios-mensais*`) THEN o sistema SHALL incluir apenas lançamentos cujo CC efetivo (`COALESCE(beneficio.centro_custo_id, funcionario.centro_custo_id)`) ∈ `centrosCustoIds`
3. **(FCC-15)** WHEN funcionário mudar de CC após criação do benefício THEN ACL scoped SHALL continuar usando CC snapshotado na linha (não o CC atual)
4. **(FCC-16)** WHEN testes de `BeneficioMensalService` e `BeneficioConsultaAdapter` forem executados THEN SHALL existir caso discriminatório equivalente a FCC-06

**Independent Test**: Criar benefício em CC-A; transferir funcionário para CC-B; gestor CC-A ainda vê; gestor CC-B não vê.

---

### P2: Endpoint por centro de custo consistente

**User Story**: Como consumidor da API, quero que consulta explícita por centro de custo retorne linhas **daquele CC na competência**.

**Acceptance Criteria**:

1. **(FCC-10)** WHEN `GET /folha-pagamento/centro-custo/{id}` for chamado com CC autorizado THEN SHALL retornar linhas onde `folha.centro_custo_id = {id}` no período (não `funcionario.centro_custo_id`)
2. **(FCC-11)** WHEN usuário não tiver permissão no CC THEN SHALL retornar lista vazia (comportamento atual)

**Independent Test**: Funcionário transferido; consulta CC antigo retorna linhas da competência antiga.

---

### P2: Exibição alinhada (sem mudar contrato API)

**Acceptance Criteria**:

1. **(FCC-12)** WHEN linha tiver `centro_custo_id` THEN DTO/snapshot SHALL expor esse CC; `FolhaConsultaAdapter.toLinhaSnapshot` SHALL usar CC efetivo da linha (não só funcionário)
2. **(FCC-17)** WHEN benefício tiver `centro_custo_id` THEN `BeneficioMensalDTO.centroCustoId` SHALL refletir CC da linha (snapshot), com fallback funcionário se null

---

### P1: ACL ficha processada (path materializado) ⭐ MVP

**User Story**: Como gestor com escopo parcial, quero que dashboard e resumo scoped usem o CC **congelado na ficha** quando a competência já foi processada, para paridade com folha ADP e benefícios.

**Why P1**: Gap code-review (b); path `linhasDeFicha` ainda filtrava por CC atual do funcionário — quebra FCC-07/FCC-08 pós-processamento.

**Acceptance Criteria**:

1. **(FCC-18)** WHEN competência for processada (`FolhaProcessamentoService.processar`) THEN `ficha_mensal.centro_custo_id` SHALL ser persistido = CC efetivo (`COALESCE` linha ADP → funcionário) do grupo processado
2. **(FCC-19)** WHEN usuário scoped consultar linhas via path ficha (`FolhaConsultaAdapter.linhasDeFicha`) THEN filtro SHALL usar CC efetivo `COALESCE(ficha_mensal.centro_custo_id, funcionario.centro_custo_id)` ∈ `centrosCustoIds`
3. **(FCC-20)** WHEN ficha tiver `centro_custo_id` THEN `toLinhaSnapshotFromFicha` SHALL expor esse CC (fallback funcionário se null — A10)
4. **(FCC-21)** WHEN competência for reprocessada THEN nova ficha SHALL receber CC snapshot atualizado (não preservar CC de ficha anterior)
5. **(FCC-22)** WHEN testes de `FolhaConsultaAdapter` cobrirem path ficha THEN SHALL existir caso discriminatório: ficha snapshot CC-A, funcionário atual CC-B → gestor A vê, gestor B não

**Independent Test**: Processar competência com linha ADP CC-A; transferir funcionário para CC-B; consulta scoped via adapter (ficha exists) — gestor CC-A vê, CC-B não.

---

### P2: Hygiene e performance (code-review)

**Acceptance Criteria**:

1. **(FCC-23)** WHEN `BeneficioConsultaAdapter.contarLancamentosAtivosNaCompetenciaPorCentros` for chamado THEN SHALL usar query SQL com `COALESCE(bm.centro_custo_id, funcionario.centro_custo_id)` (não full fetch + filter in-memory)
2. **(FCC-24)** WHEN código referenciar query scoped de benefícios por CC THEN nome do método repository SHALL refletir CC efetivo (não `FuncionarioCentroCustoIdIn`)
3. **(FCC-25)** WHEN `findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue` não tiver callers THEN SHALL ser removido de `FolhaPagamentoRepository`

---

### P1: ACL drill-down ficha (detalhe por totalizador) ⭐ MVP

**User Story**: Como gestor com escopo parcial, quero que o detalhe da ficha (`listarLinhasPorTotalizador`, `buscarFichaIdPorFuncionario`) use o **mesmo CC efetivo** da listagem/resumo, para não vazar ou ocultar drill-down após transferência de CC.

**Why P1**: Code-review round 2 — `FolhaFichaConsultaService.podeAcessarFicha` ainda usava só `funcionario.centroCusto`.

**Acceptance Criteria**:

1. **(FCC-26)** WHEN gestor **sem** `acessoTotal` acessar drill-down ficha THEN ACL SHALL usar CC efetivo `COALESCE(ficha_mensal.centro_custo_id, funcionario.centro_custo_id)` ∈ `centrosCustoIds` (paridade FCC-19)
2. **(FCC-27)** WHEN ficha snapshot CC-A AND funcionário atual CC-B THEN gestor CC-A SHALL acessar AND gestor CC-B SHALL receber `FichaMensalNotFoundException` (404)
3. **(FCC-28)** WHEN testes de `FolhaFichaConsultaService` forem executados THEN SHALL existir caso discriminatório FCC-27 que **falharia** se ACL usasse só CC atual do funcionário

**Independent Test**: Ficha com `centroCusto` snapshot A, funcionário CC B; gestor A lista linhas; gestor B 404.

---

## Edge Cases

- WHEN linha/lançamento e funcionário sem CC THEN SHALL negar acesso scoped
- WHEN funcionário inativo com folha/benefício histórico THEN ACL scoped SHALL usar CC efetivo da linha
- WHEN `centrosCustoIds` vazio e `acessoTotal=false` THEN SHALL negar (sem regressão AD-007)
- WHEN re-import substituir competência de folha THEN linhas novas SHALL refletir CC do cadastro na hora do import
- WHEN competência processada (ficha) THEN ACL scoped usa CC snapshot da ficha (FCC-19)
- WHEN ficha legado sem `centro_custo_id` THEN fallback funcionário (A10)
- WHEN reprocessar competência THEN CC snapshot da ficha atualizado (FCC-21)

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| FCC-01 | P1: ACL folha | Tasks | T1, T2 |
| FCC-02 | P1: ACL folha | Tasks | T1, T2 |
| FCC-03 | P1: ACL folha | Tasks | T1, T2 |
| FCC-04 | P1: ACL folha | Tasks | T2 |
| FCC-05 | P1: ACL folha | Tasks | T1, T2 |
| FCC-06 | P1: ACL folha | Tasks | T2 |
| FCC-07 | P1: Resumo/dashboard | Tasks | T2 |
| FCC-08 | P1: Resumo/dashboard | Tasks | T2 |
| FCC-09 | P1: Resumo/dashboard | Tasks | T2 |
| FCC-10 | P2: Endpoint CC | Tasks | T3 |
| FCC-11 | P2: Endpoint CC | Tasks | T3 |
| FCC-12 | P2: Exibição folha | Tasks | T2 |
| FCC-13 | P1: ACL benefícios | Tasks | T4, T6 |
| FCC-14 | P1: ACL benefícios | Tasks | T5, T6, T7 |
| FCC-15 | P1: ACL benefícios | Tasks | T6 |
| FCC-16 | P1: ACL benefícios | Tasks | T7 |
| FCC-17 | P2: Exibição benefícios | Tasks | T6 |
| FCC-18 | P1: ACL ficha | Tasks | T8 |
| FCC-19 | P1: ACL ficha | Tasks | T9 |
| FCC-20 | P1: ACL ficha | Tasks | T9 |
| FCC-21 | P1: ACL ficha | Tasks | T8 |
| FCC-22 | P1: ACL ficha | Tasks | T9 |
| FCC-23 | P2: Hygiene/perf | Tasks | T10 |
| FCC-24 | P2: Hygiene/perf | Tasks | T11 |
| FCC-25 | P2: Hygiene/perf | Tasks | T11 |
| FCC-26 | P1: Ficha drill-down | Tasks | T12 |
| FCC-27 | P1: Ficha drill-down | Tasks | T12 |
| FCC-28 | P1: Ficha drill-down | Tasks | T12 |

**Coverage:** 28 total, 28 mapped to tasks

---

## Success Criteria

- [x] Gestor CC-A não vê folha/benefício de competência em que funcionário estava no CC-B (mesmo com cadastro atual = B)
- [x] Resumo scoped, dashboard e listagem batem para o mesmo escopo após transferência
- [x] Testes FCC-06 e FCC-16 matam mutação “voltar a usar funcionario.centroCusto no ACL”
- [x] Zero breaking change de contrato HTTP / DTO

- [x] Competências **processadas** (path ficha) respeitam CC snapshot para ACL scoped
- [x] Drill-down ficha (`FolhaFichaConsultaService`) usa CC efetivo da ficha (paridade listagem)

---

## Questões abertas (pós round 2)

| Item | Classificação | Descrição |
| ---- | ------------- | --------- |
| `FolhaFichaConsultaService.podeAcessarFicha` | **Resolvido round 3** — FCC-26…28 | |
| Teste FCC-17 fallback null | **(c) deferido** | |
| Teste `CentroCustoNotFoundException` | **(c) deferido** | |
| Docs `CONHECIMENTO_CONSOLIDADO` stale | **(c)** | Referência a método removido |
