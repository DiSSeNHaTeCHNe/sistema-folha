# ACL — Role `ACESSO_TOTAL` Specification

**Parent / related:** `modular-monolith` (MOD-09 ACL), `modular-acl-security-fix`, `modular-boundary-hardening`  
**Complexity:** Medium  
**Spec status:** Confirmed 2026-07-27 (gray areas A–D locked — see `context.md`)

## Problem Statement

Após a correção ACL “empty ≠ total access”, nenhum caminho de produção seta `acessoTotal=true`. Usuários com role `ADMIN` (ex.: seed `admin`) sem funcionário/nó no organograma veem o **resumo** da folha (`GET /resumo-folha-pagamento`, sem filtro de acesso), mas **Ver funcionários** retorna lista vazia (`GET /folha-pagamento`, filtrado por `aplicarFiltroAcesso`). O produto precisa de uma permissão explícita e concedível a qualquer usuário para visão global de dados, separada de `ADMIN` (mutações privilegiadas).

## Goals

- [ ] Introduzir permissão `ACESSO_TOTAL` que seta `acessoTotal=true` em `OrganogramaAcessoPort.obterContextoAcesso`, **sem** exigir funcionário vinculado nem nó no organograma
- [ ] Separar `ADMIN` (privilégio de configuração/mutação) de `ACESSO_TOTAL` (escopo global de dados)
- [ ] Garantir que usuário com `ACESSO_TOTAL` consiga o mesmo conjunto de dados na listagem de funcionários da folha que o resumo implica (totais da competência)
- [ ] Expor `ACESSO_TOTAL` na UI de manutenção de usuários para concessão a ADM e outros
- [ ] Seed/migração: usuário admin existente recebe `ACESSO_TOTAL` junto com o ajuste documentado de roles

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Redesign completo do catálogo de roles (`GESTOR`, `OPERADOR`, …) e enforcement Spring por cada uma | Dívida pré-existente; FE lista muitas roles que o BE quase não usa |
| Privilege escalation hygiene em `/usuarios` (só ADMIN gerencia usuários) | Deferred concern (CONCERNS) |
| ACL em `ResumoFolhaPagamentoService` (filtrar resumos por organograma) | Confirmado fora do MVP (gray D); ATOT-11 Deferred |
| Funcionário fantasma “Admin” + nó raiz | Explicitamente rejeitado na discussão de produto |
| Mudança de modelo JWT / claims além do que `/auth/acesso` já reflete via port | Sem redesign de token |
| Rate limits / auditoria formal de concessão | Observabilidade mínima (log existente) basta neste MVP |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Nome da permissão | String `ACESSO_TOTAL` → `ROLE_ACESSO_TOTAL` via `Usuario.getAuthorities()` | Alinha ao padrão existente `ROLE_` + permissão | y |
| `ADMIN` sozinho **não** implica `acessoTotal` | Só `ACESSO_TOTAL` seta a flag | Least privilege; conversa de produto | y |
| Concessão | Qualquer usuário pode receber `ACESSO_TOTAL` (via tela Usuários / API) | Conversa: “posso dar para outros?” → sim | y |
| Seed admin | Migração Flyway: `INSERT` `ACESSO_TOTAL` para o usuário admin seed (`login = 'admin'`) | Desbloqueia ADM sem gambiarra de funcionário | y |
| “Ajustar role admin” | Manter `ADMIN` para mutações; **adicionar** `ACESSO_TOTAL` ao admin; não remover `ADMIN` | ADMIN ≠ visão de folha | y |
| Resumo da folha | **Não** aplicar ACL no endpoint de resumo neste MVP | Sintoma corrigido via flag; filtrar resumo = Deferred ATOT-11 | y |
| Consumidores | Qualquer path que já consulta `contexto.acessoTotal()` passa a funcionar (Folha listagens/totais/delete, Benefício mensal, Dashboard) sem mudança por consumidor | Flag no port; um ponto de concessão | y |
| FE picker | Incluir `ACESSO_TOTAL` em `permissoesDisponiveis` + cor de chip | Necessário para conceder sem SQL | y |

**Open questions:** none — A–D confirmadas 2026-07-27 (`context.md`).

---

## User Stories

### P1: Permissão `ACESSO_TOTAL` seta `acessoTotal` ⭐ MVP

**User Story**: Como operador de sistema, quero uma permissão `ACESSO_TOTAL` que conceda visão global de dados de folha/benefícios/dashboard sem vínculo a organograma, para que o ADM (e outros autorizados) não dependam de funcionário fictício.

**Why P1**: Sem isso, `acessoTotal` nunca é `true` em produção; listagem de funcionários da folha fica vazia para admin.

**Acceptance Criteria**:

1. (ATOT-01) WHEN `OrganogramaAcessoPort.obterContextoAcesso(usuarioId)` for chamado para usuário ativo cuja lista `permissoes` contém `ACESSO_TOTAL` THEN o sistema SHALL retornar `acessoTotal=true` e SHALL NOT exigir `temFuncionarioVinculado` nem `temNoOrganograma` para esse resultado
2. (ATOT-02) WHEN o mesmo usuário **não** tiver `ACESSO_TOTAL` THEN o sistema SHALL preservar as regras atuais (sem funcionário → negar; sem nó → negar; com nó → centros do nó+descendentes, `acessoTotal=false`)
3. (ATOT-03) WHEN usuário com `ACESSO_TOTAL` e **sem** funcionário vinculado consultar `GET /folha-pagamento` com `dataInicio`/`dataFim` da competência do resumo THEN o sistema SHALL retornar as linhas ativas da competência (não lista vazia por ACL)
4. (ATOT-04) WHEN usuário **sem** `ACESSO_TOTAL`, sem funcionário ou sem nó, consultar `GET /folha-pagamento` no mesmo período THEN o sistema SHALL continuar retornando lista vazia (deny)
5. (ATOT-05) WHEN testes unitários de `OrganogramaAcessoService` (e/ou consumidor Folha) forem executados THEN SHALL existir cobertura que falharia se `ACESSO_TOTAL` deixasse de setar `acessoTotal=true` sem funcionário; e cobertura de regressão deny sem a permissão

**Independent Test**: Mock/fixture usuário só com `ACESSO_TOTAL`, sem `funcionario_id` → `acessoTotal=true`; `consultarPorPeriodo` retorna linhas; remover permissão → vazio.

---

### P1: Separar e ajustar `ADMIN` + seed ⭐ MVP

**User Story**: Como administrador, quero manter `ADMIN` para privilégios de mutação e receber `ACESSO_TOTAL` no usuário seed, para que login admin volte a ver funcionários da folha sem confundir as duas responsabilidades.

**Why P1**: Desbloqueio operacional do usuário ADM/admin após importação.

**Acceptance Criteria**:

1. (ATOT-06) WHEN regras Spring existentes usarem `hasRole("ADMIN")` (ex. mutação `tipo-beneficio`) THEN SHALL permanecerem atreladas a `ADMIN` — `ACESSO_TOTAL` **não** substitui `ADMIN` nesses matchers
2. (ATOT-07) WHEN a migração Flyway desta feature for aplicada THEN o usuário admin seed SHALL possuir permissão `ACESSO_TOTAL` (além das permissões admin já existentes)
3. (ATOT-08) WHEN usuário tiver apenas `ADMIN` **sem** `ACESSO_TOTAL` THEN `obterContextoAcesso` SHALL **não** setar `acessoTotal=true` só por ser ADMIN

**Independent Test**: Assert seed/migração; teste unitário ADMIN-only → `acessoTotal=false`; ADMIN+ACESSO_TOTAL → `true`.

---

### P1: UI Usuários concede `ACESSO_TOTAL` ⭐ MVP

**User Story**: Como ADMIN na tela de Usuários, quero marcar/desmarcar `ACESSO_TOTAL` em qualquer usuário, para conceder visão global sem SQL.

**Why P1**: Conversa — permissão deve ser atribuível a outros usuários.

**Acceptance Criteria**:

1. (ATOT-09) WHEN o formulário de usuário listar permissões disponíveis THEN SHALL incluir `ACESSO_TOTAL`
2. (ATOT-10) WHEN um usuário for salvo com `ACESSO_TOTAL` na lista de permissões THEN a API de usuários SHALL persistir a string e, após novo login/`/auth/acesso`, o cliente SHALL receber `acessoTotal=true`

**Independent Test**: Criar/editar usuário via API ou UI com a permissão; `GET /auth/acesso` reflete `acessoTotal=true`.

---

### P2: Alinhar resumo da folha à ACL — Deferred

**User Story**: Como gestor com escopo parcial, quero que a lista de resumos não mostre competências/totais globais além do meu escopo.

**Status:** Deferred (gray D confirmada — fora deste MVP).

**Acceptance Criteria (future):**

1. (ATOT-11) WHEN feature futura for aberta THEN `ResumoFolhaPagamento` listagens SHALL respeitar o mesmo contexto de acesso

**Independent Test:** N/A neste MVP.

---

## Edge Cases

- WHEN usuário tem `ACESSO_TOTAL` **e** funcionário+nó THEN SHALL `acessoTotal=true` (visão global prevalece; centros do nó podem permanecer preenchidos ou vazios — implementação livre desde que consumidores tratem `acessoTotal` primeiro)
- WHEN usuário inativo ou login inexistente THEN SHALL negar como hoje
- WHEN permissão for string com casing diferente (`acesso_total`) THEN SHALL **não** conceder (match exato `ACESSO_TOTAL`, como demais permissões)
- WHEN JWT antigo existir após concessão THEN novo contexto vale no próximo `obterContextoAcesso` / refresh de `/auth/acesso` (sem redesign de claims)

---

## Implicit-requirement dimensions (Medium)

| Dimension | Resolution |
| --------- | ---------- |
| Auth boundaries | `ACESSO_TOTAL` → `acessoTotal`; `ADMIN` permanece mutações; deny paths inalterados sem a permissão |
| Failure / partial-failure | Sem permissão → lista vazia (já existente); sem novo código de erro |
| Observability | Reusar log ACL existente; ao conceder total, log info (não inventar stack de métricas) |
| Input validation | String de permissão via lista FE/API existente |
| Remaining | N/A for this scope (no concurrency redesign, no external deps, no rate limits, no TTL) |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| ATOT-01 | P1: ACESSO_TOTAL → flag | Execute (implicit) | Confirmed |
| ATOT-02 | P1: regressão deny | Execute (implicit) | Confirmed |
| ATOT-03 | P1: folha período com total | Execute (implicit) | Confirmed |
| ATOT-04 | P1: folha período sem total | Execute (implicit) | Confirmed |
| ATOT-05 | P1: testes | Execute (implicit) | Confirmed |
| ATOT-06 | P1: ADMIN matchers intactos | Execute (implicit) | Confirmed |
| ATOT-07 | P1: seed migração | Execute (implicit) | Confirmed |
| ATOT-08 | P1: ADMIN ≠ acessoTotal | Execute (implicit) | Confirmed |
| ATOT-09 | P1: FE picker | Execute (implicit) | Confirmed |
| ATOT-10 | P1: persist + /auth/acesso | Execute (implicit) | Confirmed |
| ATOT-11 | P2: ACL resumo | — | Deferred |

**Coverage:** 10 MVP confirmed (Design/Tasks skipped — Medium); ATOT-11 Deferred; Execute maps ACs → testes

---

## Success Criteria

- [ ] Usuário admin seed com `ACESSO_TOTAL` vê funcionários após importação (mesmo período do resumo)
- [ ] Usuário só `ADMIN` sem `ACESSO_TOTAL` **não** ganha visão global
- [ ] Outro usuário qualquer pode receber `ACESSO_TOTAL` pela tela Usuários
- [ ] Testes ATOT-01…05 e ATOT-08 verdes; mutações ADMIN (ATOT-06) sem regressão
- [ ] `acessoTotal` continua **nunca** derivado de `Set` vazio / ausência de nó
