# folha-custo-clt-fix3 — Rubrica fixa global + UX detalhe Specification

**Parent:** `_docs/specs/features/folha-custo-clt/` (INT-2 evolução)  
**Related:** `folha-custo-clt-fix2` (Done — `%` no custo, paridade card↔aba), `folha-custo-clt-fix1` (Done)  
**Complexity:** Large  
**Spec status:** Execute complete 2026-07-29 — T1–T12 done on `feat/folha-custo-clt` → pronto para Verifier

> **Nota de nomenclatura:** `fix3` estende INT-2 (funcionário opcional / fixa global) e melhora UX de Rubricas Fixas + detalhe Bruto/Líquido/Custo.

## Problem Statement

Rubricas fixas exigem hoje **funcionário obrigatório**, impedindo cadastrar custo Techne **igual para todos** (ex.: taxa uniforme na competência). A listagem não exibe **percentual** da rubrica mestre. No detalhe do funcionário, só a aba **Custo** agrupa por origem; Bruto/Líquido não seguem o mesmo padrão, faltam coluna **Percentual**, **subtotais por origem** e **totalizador da aba** alinhado ao card — dificultando auditoria pós-fix2.

## Goals

- [x] Permitir rubrica fixa **global** (`funcionario_id` null) aplicada a todos os CLT processados na competência, mantendo fixa **individual** por funcionário
- [x] Reordenar formulário Rubricas Fixas e exibir **Percentual** dinâmico (cadastro mestre) na listagem
- [x] Padronizar abas **Bruto / Líquido / Custo** no detalhe: agrupamento por origem, coluna Percentual, subtotal por origem, total da aba = valor do card
- [x] Preservar regras fix2: `%` só no custo; bruto/líquido com valor original; paridade card ↔ soma aba (± R$ 0,01)

## Out of Scope

| Feature | Reason |
| --- | --- |
| `%` por vínculo fixo (override por funcionário) | Permanece no cadastro mestre da rubrica |
| Fixa global para CLT **sem** linha ADP na competência | Universo = quem recebe ficha no processamento atual |
| Valores diferentes por funcionário na mesma fixa global | Global = **mesmo valor** para todos |
| Alterar fórmulas bruto/líquido/custo (fix2) | Escopo UX + INT-2 global apenas |
| Testes Vitest FE completos (AD-004) | Build + lint gate; testes manuais nos ACs |
| PJ, portal holerite, import legado | Etapa 2+ |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --- | --- | --- | --- |
| Funcionário opcional = fixa global | `funcionario_id` null → aplica a todo CLT com ficha na competência | Conversa + recomendação aprovada implicitamente no pedido | y |
| Prioridade conflito rubrica | Individual > global > skip se rubrica já no ADP | Exceção sobrescreve regra geral; dedup ADP preservado (FCLT-23) | y |
| Universo “todos” | Mesmo universo do processamento: CLT com linhas ADP na competência | Evita criar ficha só por fixa global | y |
| Percentual listagem fixas | **Live** de `rubricas.porcentagem` (cadastro mestre) | Pedido “dinâmico, valor atualizado” | y |
| Percentual detalhe ficha | **Snapshot** `ficha_linha.porcentagem` (null→100) | Auditável vs competência processada; alinha fix2 | y |
| Bruto/Líquido: coluna % | Exibir snapshot; contribuição **sem** `%` (fix2-02) | Coluna informativa, não recalcula contribuição | y |
| Totalizador aba | Bruto→`salBruto`, Líquido→`salLiquido`, Custo→`custoEmpresa` do card | Paridade explícita com card aberto | y |
| Vigência global | Uma fixa global ativa por `(rubrica_id, vigência)` — sem sobreposição | Evita duas RH globais conflitantes | y |
| Listagem: coluna Funcionário | Exibir **“Todos”** quando `funcionario_id` null | UX clara | y |

**Open questions:** none — ver `context.md` FIX3-CTX-01…10.

Decisões detalhadas: `_docs/specs/features/folha-custo-clt-fix3/context.md`.

---

## User Stories

### P1: Rubrica fixa global (backend) ⭐ MVP

**User Story**: Como operador de RH, quero cadastrar rubrica fixa **sem funcionário** para aplicar o **mesmo valor** a todos os CLT da competência processada, mantendo cadastros individuais quando necessário.

**Why P1**: Desbloqueia modelo “compor a folha” sem repetir N cadastros idênticos.

**Acceptance Criteria**:

1. (FIX3-01) WHEN migração Flyway rodar THEN `funcionario_rubrica_fixa.funcionario_id` SHALL ser **nullable** com FK opcional
2. (FIX3-02) WHEN POST/PUT rubrica fixa **sem** `funcionarioId` THEN API SHALL persistir `funcionario_id` null e retornar DTO com `funcionarioNome` null ou label acordado
3. (FIX3-03) WHEN POST/PUT rubrica fixa **com** `funcionarioId` THEN comportamento atual SHALL permanecer (fixa individual)
4. (FIX3-04) WHEN `POST /folha-pagamento/processar` THEN fixas globais vigentes SHALL ser injetadas como `CUSTO_FIXO` em **cada** ficha CLT da competência (mesmo valor e operadores da rubrica)
5. (FIX3-05) WHEN funcionário tem fixa **individual** e fixa **global** para mesma rubrica THEN individual SHALL prevalecer; global ignorada para esse par funcionário+rubrica
6. (FIX3-06) WHEN rubrica já presente no ADP do funcionário THEN fixa (global ou individual) SHALL ser ignorada — log WARN (regra FCLT-23 preservada)
7. (FIX3-07) WHEN duas fixas globais ativas com mesma rubrica e vigência sobreposta THEN API SHALL retornar HTTP **409**
8. (FIX3-08) WHEN fixa global alterada após processamento THEN totais SHALL atualizar somente após **reprocessar** competência

**Independent Test**: Cadastrar RH R$ 500 global → processar competência com 2 CLT → ambas fichas têm linha CUSTO_FIXO 500; cadastrar João RH R$ 688 individual → reprocessar → João 688, demais 500.

---

### P1: API detalhe — expor percentual snapshot ⭐ MVP

**User Story**: Como gestor, quero ver o **percentual** de cada linha no detalhe da ficha para auditar custo pós-fix2.

**Why P1**: FE não pode exibir coluna Percentual sem campo na API.

**Acceptance Criteria**:

1. (FIX3-09) WHEN `GET /folha-pagamento/fichas/{id}/linhas?totalizer=*` para linha de ficha THEN response SHALL incluir `porcentagem` (snapshot `ficha_linha`, null→100)
2. (FIX3-10) WHEN linha `origemLinha=BENEFICIO` (aba Custo) THEN `porcentagem` SHALL ser **null** ou omitido; FE exibe **—**
3. (FIX3-11) WHEN totalizador GROSS ou NET THEN `contribuicao` SHALL permanecer **sem** `%`; `porcentagem` apenas informativa

**Independent Test**: Salário 138,63% processado → aba Custo linha ADP com `porcentagem=138.63`; aba Bruto mesma linha com `porcentagem=138.63` e contribuição = valor original.

---

### P1: Tela Rubricas Fixas — cadastro e listagem ⭐ MVP

**User Story**: Como operador, quero cadastrar e listar rubricas fixas com ordem de campos clara, funcionário opcional e percentual visível.

**Why P1**: Entrega UX solicitada + suporte à fixa global.

**Acceptance Criteria**:

1. (FIX3-12) WHEN formulário Nova/Editar Rubrica Fixa THEN ordem dos campos SHALL ser: **Rubrica → Valor → Vigência Início → Vigência Fim → Funcionário (opcional) → Comentário**
2. (FIX3-13) WHEN Funcionário não selecionado THEN helper SHALL indicar **“Todos os funcionários (mesmo valor)”**; submit envia sem `funcionarioId`
3. (FIX3-14) WHEN listagem de Rubricas Fixas THEN coluna **Percentual** SHALL exibir `%` atual da rubrica mestre (`rubricas.porcentagem`, default **100%** se null) — **não** snapshot de ficha
4. (FIX3-15) WHEN listagem THEN coluna Funcionário SHALL exibir **“Todos”** para registros globais
5. (FIX3-16) WHEN erro 409 vigência THEN toast SHALL distinguir conflito global vs individual quando aplicável

**Independent Test**: Abrir Rubricas Fixas → criar fixa global → listagem mostra “Todos” + percentual da rubrica; editar ordem visual conforme spec.

---

### P1: Detalhe folha funcionário — abas padronizadas ⭐ MVP

**User Story**: Como gestor, quero nas abas Bruto, Líquido e Custo ver origens, percentual, subtotais e total igual ao card.

**Why P1**: Auditoria e paridade visual pós-fix2.

**Acceptance Criteria**:

1. (FIX3-17) WHEN aba **Bruto** ou **Líquido** THEN linhas SHALL agrupar por `origemLinha` com título conforme `ORIGEM_LABELS` (mesmo padrão aba Custo hoje)
2. (FIX3-18) WHEN qualquer aba (Bruto/Líquido/Custo) THEN colunas SHALL ser: **Rubrica | Valor | Percentual | Contribuição**
3. (FIX3-19) WHEN Percentual THEN exibir snapshot formatado (ex. `138,63%`) ou **—** para BENEFICIO / ausente
4. (FIX3-20) WHEN fim de cada grupo de origem THEN SHALL exibir **Subtotal** da soma das contribuições daquele grupo (± arredondamento HALF_UP por linha)
5. (FIX3-21) WHEN fim da aba THEN SHALL exibir **Total** = soma de todas as contribuições da aba
6. (FIX3-22) WHEN aba Bruto THEN Total SHALL equal `salBruto` do card do funcionário (± R$ 0,01)
7. (FIX3-23) WHEN aba Líquido THEN Total SHALL equal `salLiquido` do card (± R$ 0,01)
8. (FIX3-24) WHEN aba Custo THEN Total SHALL equal `custoEmpresa` do card (± R$ 0,01)

**Independent Test**: Abrir Thyago mai/2026 → alternar abas → subtotais por origem + total = card em cada aba.

---

## Edge Cases

- WHEN fixa global com `valor` null e rubrica não calculada THEN rejeitar no cadastro (400) — igual individual
- WHEN `%` null na rubrica mestre THEN listagem e detalhe tratam como **100%**
- WHEN aba sem linhas (operador 0 em todas) THEN mensagem empty state; Total = **0,00**
- WHEN scoped ACL oculta funcionário THEN fixa global só materializa em fichas visíveis no processamento (sem bypass ACL)
- WHEN reprocesso após cadastrar fixa global THEN novas linhas CUSTO_FIXO aparecem; consulta não lê cadastro live

---

## Implicit-requirement dimensions (Large)

| Dimension | Resolution |
| --- | --- |
| Input validation | Valor obrigatório (não calculada); vigência; sobreposição global vs individual |
| Failure / partial-failure | Processamento @Transactional; rollback preservado |
| Idempotency | Reprocessar substitui ficha; fixa global re-aplicada idempotentemente |
| Auth boundaries | CRUD fixa ADMIN; detalhe ficha ACL existente |
| Concurrency | Serialização por competência no processamento |
| Data lifecycle | Global: `funcionario_id` null persistido; histórico soft-delete |
| Observability | WARN dedup ADP/global/individual conforme fix2 |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| --- | --- | --- | --- |
| FIX3-01 | P1 global schema | T1 | Done |
| FIX3-02 | P1 API create global | T3,T4 | Done |
| FIX3-03 | P1 API create individual | T3,T4 | Done |
| FIX3-04 | P1 processamento global | T6 | Done |
| FIX3-05 | P1 prioridade individual | T6 | Done |
| FIX3-06 | P1 dedup ADP | T6 | Done |
| FIX3-07 | P1 vigência global 409 | T3,T4 | Done |
| FIX3-08 | P1 reprocesso | T6 | Done |
| FIX3-09 | P1 API porcentagem linha | T7,T8 | Done |
| FIX3-10 | P1 benefício sem % | T7 | Done |
| FIX3-11 | P1 bruto/líquido informativo | T7 | Done |
| FIX3-12 | P1 FE ordem form | T9 | Done |
| FIX3-13 | P1 FE funcionário opcional | T8,T9 | Done |
| FIX3-14 | P1 FE coluna % listagem | T10 | Done |
| FIX3-15 | P1 FE coluna Todos | T10 | Done |
| FIX3-16 | P1 FE toast 409 | T5,T10 | Done |
| FIX3-17 | P1 FE agrupamento bruto/líquido | T11 | Done |
| FIX3-18 | P1 FE colunas detalhe | T11 | Done |
| FIX3-19 | P1 FE formato % | T11 | Done |
| FIX3-20 | P1 FE subtotal origem | T11 | Done |
| FIX3-21 | P1 FE total aba | T11 | Done |
| FIX3-22 | P1 paridade total bruto | T11 | Done |
| FIX3-23 | P1 paridade total líquido | T11 | Done |
| FIX3-24 | P1 paridade total custo | T11 | Done |

**Coverage:** 24 requirements mapped → Tasks T1–T12 Execute complete @ `69f4258`

**Cross-ref:** INT-2 emendado em `folha-custo-clt/spec.md` (nota fix3 global).

---

## Success Criteria

- [x] Operador cadastra 1 fixa global RH R$ 500 → N funcionários processados recebem linha CUSTO_FIXO sem N cadastros
- [x] Exceção individual sobrescreve global na mesma rubrica
- [x] Listagem Rubricas Fixas exibe Percentual live e “Todos” para globais
- [x] Detalhe Bruto/Líquido/Custo: mesma estrutura visual; Total aba = card (± R$ 0,01)
- [x] Gate backend + FE lint/build verde; testes unitários backend para global + API `%`
