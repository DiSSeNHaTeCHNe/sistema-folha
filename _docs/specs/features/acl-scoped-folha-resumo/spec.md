# ACL — Resumo Folha (scoped) + Dashboard evolução Specification

**Parent / related:** `acl-acesso-total-role` (ATOT-11), `modular-acl-security-fix`, AD-007, AD-011  
**Complexity:** Medium  
**Spec status:** Confirmed 2026-07-27 (gray areas A2, B1, C1, D1 — see `context.md`)

## Problem Statement

Listagens de **folha** e **benefícios** e a maior parte do **Dashboard** já respeitam o contexto de acesso (`ACESSO_TOTAL` ou centros do organograma). O **resumo da folha** (`GET /resumo-folha-pagamento*`) continua devolvendo totais **globais** da importação ADP — gestor com escopo parcial vê números da empresa inteira na tabela de competências, enquanto “Ver funcionários” mostra só o seu escopo. No Dashboard, a **evolução mensal** some para quem não tem acesso total (lista vazia) em vez de refletir o escopo.

## Goals

- [x] Resumo da folha (listagem de competências e totais) SHALL refletir apenas o escopo do usuário autenticado; competências sem linhas no escopo SHALL aparecer com zeros
- [x] Usuário com `acessoTotal=true` SHALL continuar vendo os totais do snapshot global da importação
- [x] Dashboard: evolução mensal SHALL existir para escopo parcial, com números do escopo
- [x] Benefícios: **sem mudança** de código neste feature (D1)

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Alterar importação ADP / persistência do snapshot global `ResumoFolhaPagamento` | Snapshot canônico para visão total |
| Redesign de schema com resumos por centro de custo | Agregação on-the-fly a partir das linhas |
| Privilege escalation `/usuarios` | Concern separado |
| Catálogo/enforcement de roles FE órfãs | Dívida pré-existente |
| Mudança de JWT / claims | Contexto via `OrganogramaAcessoPort` |
| Relatórios PDF/download de benefícios legados | Outro domínio |
| Rateio de encargos proporcionais ao escopo | Rejeitado (B1 = zero) |
| Alterar `BeneficioMensalService` / ACL benefícios | Já adequado (D1) |
| Reescrever FE Folha além do necessário se API devolver DTO filtrado | Preferir zero/mínimo FE |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Fonte dos totais com escopo | Recalcular a partir de linhas `FolhaPagamento` filtradas por centros (mesmo critério ACL da listagem) | Snapshot sem fatia por CC; padrão Dashboard | y |
| Competência sem linhas no escopo | **Mostrar com zeros** (A2) | Usuário escolheu A2 | y |
| `totalEncargos` com escopo | **`BigDecimal.ZERO`** (B1); pagamentos/descontos/líquido/empregados das linhas | Encargos só no rodapé ADP | y |
| Dashboard evolução | **P1 MVP** (C1) — série scoped | Usuário escolheu C1 | y |
| Benefícios | Sem implementação (D1); regressão só se necessário | Já filtra | y |
| Shape da API | Manter `ResumoFolhaPagamentoDTO`; controller passa login | Sem breaking de campos | y |
| `acessoTotal` | Usa snapshot persistido sem recalcular | Paridade com import | y |

**Open questions:** none — A–D confirmadas 2026-07-27 (`context.md`).

---

## User Stories

### P1: Resumo da folha scoped ⭐ MVP

**User Story**: Como gestor com escopo parcial no organograma, quero que a tabela de resumos da folha mostre totais apenas dos funcionários/centros que posso acessar, para que os números batam com “Ver funcionários”.

**Why P1**: Fecha o buraco ACL do resumo (ATOT-11).

**Acceptance Criteria**:

1. (RSF-01) WHEN usuário autenticado **sem** `acessoTotal` e com centros no contexto consultar `GET /resumo-folha-pagamento` (e variantes periodo/latest/competencia usadas pelo FE) THEN o sistema SHALL devolver competências com totais calculados **somente** a partir de linhas de folha ativas cujo centro de custo do funcionário ∈ `centrosCustoIds`: `totalEmpregados` = distinct funcionários; `totalPagamentos` = soma PROVENTO; `totalDescontos` = soma DESCONTO; `totalLiquido` = pagamentos − descontos; `totalEncargos` = `0`
2. (RSF-02) WHEN o usuário tiver `acessoTotal=true` THEN o sistema SHALL devolver os valores do snapshot `ResumoFolhaPagamento` persistido (incluindo encargos reais)
3. (RSF-03) WHEN usuário sem funcionário/nó e sem `ACESSO_TOTAL` consultar resumos THEN o sistema SHALL devolver lista vazia (deny)
4. (RSF-04) WHEN competência existir no snapshot mas **não** houver linhas no escopo do usuário THEN o sistema SHALL **ainda retornar** essa competência com totais zerados (`totalEmpregados=0`, valores monetários 0) preservando metadados da competência (`competenciaInicio`/`Fim`, `decimoTerceiro`, e id do snapshot quando aplicável)
5. (RSF-05) WHEN testes unitários do serviço de resumo forem executados THEN SHALL existir cobertura que falharia se o caminho scoped voltasse a espelhar o snapshot global

**Independent Test**: 2 centros; usuário só no A; snapshot global ≠ totais scoped; competência só com centro B → linha com zeros; `ACESSO_TOTAL` → snapshot.

---

### P1: Dashboard — evolução mensal no escopo ⭐ MVP

**User Story**: Como gestor com escopo parcial, quero ver a evolução mensal do dashboard com números do meu escopo, não um gráfico vazio.

**Why P1**: C1 confirmado.

**Acceptance Criteria**:

1. (RSF-06) WHEN usuário **sem** `acessoTotal` e com centros válidos chamar `GET /dashboard/stats` THEN `evolucaoMensal` SHALL conter pontos dos últimos N meses (mesma janela atual) com `totalLiquido` / `totalEmpregados` **recalculados** a partir de linhas filtradas pelo escopo — **não** a série global do snapshot e **não** lista vazia só por falta de `acessoTotal`
2. (RSF-07) WHEN usuário com `acessoTotal` THEN evolução SHALL permanecer baseada na fonte atual (`findEvolucaoUltimos12Meses` / snapshots)
3. (RSF-08) WHEN acesso negado THEN stats vazias (já existente) — sem regressão

**Independent Test**: Linhas em ≥2 competências no centro do usuário → evolução não vazia e ≠ globais.

---

### P2: Benefícios — regressão apenas

**User Story**: Como operador, quero que benefícios continuem filtrados.

**Why P2**: D1 — sem mudança de feature; smoke/gate.

**Acceptance Criteria**:

1. (RSF-09) WHEN a suite relevante de benefícios (listagem + resumo ACL) rodar no gate Full THEN SHALL permanecer verde sem alteração obrigatória de produção neste feature

**Independent Test:** Gate existente.

---

## Edge Cases

- WHEN `ACESSO_TOTAL` sem funcionário THEN resumo usa snapshot (RSF-02)
- WHEN centros do nó vazios THEN deny / lista vazia (AD-007)
- WHEN scoped com linhas THEN `totalEncargos` sempre 0 (B1); FE mostra R$ 0,00
- WHEN competência 13º THEN preservar `decimoTerceiro` do snapshot ao recalcular ou zerar
- WHEN id do resumo scoped THEN Design: preferir id do snapshot da competência

---

## Implicit-requirement dimensions (Medium)

| Dimension | Resolution |
| --------- | ---------- |
| Auth boundaries | Endpoints de resumo folha com auth + `OrganogramaAcessoPort`; sem novo matcher Spring |
| Failure / partial-failure | Sem linhas → competência com zeros (A2); encargos 0 (B1) |
| Observability | Logs de domínio existentes; opcional info ao recalcular |
| Input validation | Datas/competência como hoje |
| Remaining | N/A for this scope |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| RSF-01 | P1: Resumo scoped | Execute | ✅ Verified |
| RSF-02 | P1: Snapshot se total | Execute | ✅ Verified |
| RSF-03 | P1: Deny vazio | Execute | ✅ Verified |
| RSF-04 | P1: Zeros se sem linhas | Execute | ✅ Verified |
| RSF-05 | P1: Testes | Execute | ✅ Verified |
| RSF-06 | P1: Evolução scoped | Execute | ✅ Verified |
| RSF-07 | P1: Evolução total | Execute | ✅ Verified |
| RSF-08 | P1: Deny dashboard | Execute | ✅ Verified |
| RSF-09 | P2: Benefícios regressão | Execute | ✅ Verified |

**Coverage:** 9 MVP/P2 confirmed; Design/Tasks next (Medium — Design recomendado por agregação + 2 serviços)

---

## Success Criteria

- [x] Gestor parcial: totais do resumo (quando há linhas) batem com o escopo de “Ver funcionários”; competências sem linhas aparecem zeradas
- [x] `ACESSO_TOTAL`: totais iguais ao snapshot de importação
- [x] Dashboard: evolução mensal não vazia para escopo parcial e valores scoped
- [x] Benefícios sem regressão de ACL
- [x] Testes RSF-01…05 e RSF-06…08 verdes; sensor mata retorno de snapshot global no caminho scoped
