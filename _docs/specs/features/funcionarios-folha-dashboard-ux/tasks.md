# Funcionários, Folha e Dashboard — UX Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/funcionarios-folha-dashboard-ux/design.md`  
**Status**: Approved — Execute complete (2026-07-27)  
**Approach**: enum `status` funcionários; SQL `findUltimos12MesesRegulares`; param `ano`/`mes` resumo folha  
**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` + AD-004, `funcionarios-folha-dashboard-ux/spec.md` (FUNC/DASH/FOLH ACs).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Cadastros application (`FuncionarioService`) | unit (Mockito) | FUNC-02…05 + edge inativar 404; ATIVO/INATIVO/TODOS; combinação com nome; `remover` → ativo=false | `backend/src/test/java/**/cadastros/application/FuncionarioServiceTest.java` | `cd backend && mvn test -Dtest=FuncionarioServiceTest` |
| Cadastros API (`FuncionarioController`) | none | Wiring; coberto indiretamente via service tests | `cadastros/api/FuncionarioController.java` | compile via `mvn test` |
| Cadastros enum + repository | none | Query paths exercitados em `FuncionarioServiceTest` | `FuncionarioStatusFiltro.java`, `FuncionarioRepository.java` | compile gate |
| Folha application (`ResumoFolhaPagamentoService`) | unit (Mockito) | FOLH-01…03: default ano corrente; filtro mes; ano inválido; ACL deny → [] | `backend/src/test/java/**/folha/application/ResumoFolhaPagamentoServiceTest.java` | `cd backend && mvn test -Dtest=ResumoFolhaPagamentoServiceTest` |
| Folha repository + adapter (13º evolução) | unit via callers | DASH-01/02: resumo 13º excluído; regular dez permanece; scoped idem | `ResumoFolhaPagamentoRepository.java`, `FolhaConsultaAdapter.java`, `DashboardServiceTest.java` | `cd backend && mvn test -Dtest=DashboardServiceTest` |
| Dashboard application | unit (Mockito) | DASH-01/02: `evolucaoMensal` sem pontos de `decimoTerceiro=true` (total + scoped) | `backend/src/test/java/**/dashboard/application/DashboardServiceTest.java` | `cd backend && mvn test -Dtest=DashboardServiceTest` |
| Folha API (`ResumoFolhaPagamentoController`) | none | OpenAPI annotations; compile | `folha/api/ResumoFolhaPagamentoController.java` | compile gate |
| Frontend pages/services | none | AD-004; build + lint only | `frontend/src/pages/**`, `frontend/src/services/**` | `cd frontend && npm run build && npm run lint` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após T2, T4, T6 (unit backend) | `cd backend && mvn test -Dtest=FuncionarioServiceTest,ResumoFolhaPagamentoServiceTest,DashboardServiceTest` |
| Full | Após T8 (fechamento feature) | `cd backend && mvn test && cd ../frontend && npm run lint && npm run build` |
| Build | T1, T3, T5, T7 (sem testes novos BE) | `cd frontend && npm run build` ou `cd backend && mvn test -Dtest=ExistingTest` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Cadastros — Backend

```
T1 → T2
```

### Phase 2: Cadastros — Frontend

```
T3
```

### Phase 3: Folha — Backend

```
T4
```

### Phase 4: Folha — Frontend

```
T5
```

### Phase 5: Dashboard — Backend + Frontend

```
T6 → T7
```

### Phase 6: Gate final

```
T8
```

**Batch packing:** 8 tasks → **1 batch** (≤ ~8) → Execute **inline** (sem sub-agents), salvo pedido do usuário por workers.

---

## Task Breakdown

### T1: Enum `FuncionarioStatusFiltro` + repository JPQL

**What**: Criar enum `ATIVO|INATIVO|TODOS` e estender `findByFiltros` com `(:ativo IS NULL OR f.ativo = :ativo)`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/api/FuncionarioStatusFiltro.java` (novo)
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/infrastructure/FuncionarioRepository.java`

**Depends on**: None  
**Reuses**: JPQL existente em `findByFiltros`  
**Requirement**: FUNC-03, FUNC-04, FUNC-05 (foundation)

**Tools**:
- MCP: NONE
- Skill: `jpa-performance` (query parametrizada)

**Done when**:
- [ ] Enum com três valores documentados
- [ ] Query aceita `ativo` nullable (null = TODOS)
- [ ] Compilação backend verde: `cd backend && mvn test -Dtest=FuncionarioServiceTest` (testes existentes ainda passam)

**Tests**: none (repository — exercitado em T2)  
**Gate**: build  
**Commit**: `feat(cadastros): add FuncionarioStatusFiltro and ativo param to findByFiltros`

---

### T2: `FuncionarioService.listar` unificado + controller + testes

**What**: Unificar listagem com `status`; atualizar `FuncionarioController`; testes spec-anchored para filtro status e `remover`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/application/FuncionarioService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/api/FuncionarioController.java`
- `backend/src/test/java/br/com/techne/sistemafolha/cadastros/application/FuncionarioServiceTest.java`

**Depends on**: T1  
**Reuses**: `toDTO`, `remover()` existente, padrão Mockito do projeto  
**Requirement**: FUNC-02, FUNC-03, FUNC-04, FUNC-05

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint` (contrato REST)

**Done when**:
- [ ] `GET /funcionarios` aceita `status` (default ATIVO); remove bifurcação listar/listarComFiltros
- [ ] Service mapeia ATIVO→true, INATIVO→false, TODOS→null
- [ ] Testes novos (mínimo 5): default ATIVO; INATIVO só false; TODOS mixed; filtro nome+status; `remover` seta ativo=false; segundo `remover` → exceção
- [ ] Gate: `cd backend && mvn test -Dtest=FuncionarioServiceTest`
- [ ] Test count: ≥ testes anteriores + novos (sem deleções silenciosas)

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(cadastros): filter funcionarios by status ATIVO/INATIVO/TODOS`

---

### T3: Frontend Funcionários — filtro status + card inativo

**What**: Propagar `status` no service; Select Ativo/Inativo/Todos (default Ativo); card cinza + ícone/chip; ocultar Editar/Inativar em inativos; Limpar volta status para Ativo.  
**Where**:
- `frontend/src/services/funcionarioService.ts`
- `frontend/src/pages/Funcionarios/index.tsx`

**Depends on**: T2  
**Reuses**: Padrão MUI Select; fluxo DELETE/inativar existente  
**Requirement**: FUNC-01, FUNC-03, FUNC-05, FUNC-06, FUNC-07

**Tools**:
- MCP: NONE
- Skill: NONE (AD-004 — brownfield `pages/`)

**Done when**:
- [ ] Filtro status visível; default **Ativo**; Limpar reseta para Ativo
- [ ] Cards inativos: texto cinza, fundo atenuado, indicador acessível (`PersonOff` ou chip)
- [ ] Ações Editar/Inativar só em `ativo !== false`
- [ ] Gate: `cd frontend && npm run build && npm run lint`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(frontend): funcionarios status filter and inactive card styling`

---

### T4: Backend Folha — filtro `ano`/`mes` no resumo

**What**: `listarTodos(login, ano, mes)` deriva período e delega `consultarPorPeriodo`; controller com params + validação + OpenAPI; testes.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/ResumoFolhaPagamentoService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/api/ResumoFolhaPagamentoController.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/ResumoFolhaPagamentoServiceTest.java`

**Depends on**: None (domínio independente; fase após T3 por ordem de entrega)  
**Reuses**: `consultarPorPeriodo`, ACL existente, helper `periodoDe(ano, mes)`  
**Requirement**: FOLH-01, FOLH-02, FOLH-03, FOLH-04

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `jpa-performance`

**Done when**:
- [ ] `GET /resumo-folha-pagamento?ano=&mes=` documentado; ano default = corrente quando omitido
- [ ] `mes` opcional restringe ao mês; ano fora 2000–2100 → 400
- [ ] Testes novos (mínimo 4): default ano corrente; ano+mes; ano sem resumos → []; ACL deny → []
- [ ] Gate: `cd backend && mvn test -Dtest=ResumoFolhaPagamentoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): filter resumo folha by ano and optional mes`

---

### T5: Frontend Folha — ano obrigatório + fetch server-side

**What**: `listarPorAno(ano, mes?)` no service; FolhaPagamento com ano corrente default, Select ano, validação, sem `listarTodos` na carga inicial.  
**Where**:
- `frontend/src/services/resumoFolhaPagamentoService.ts`
- `frontend/src/pages/FolhaPagamento/index.tsx`

**Depends on**: T4  
**Reuses**: Padrão `BeneficiosMensais` (`gerarAnosDisponiveis`, default `getFullYear()`)  
**Requirement**: FOLH-01, FOLH-02, FOLH-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Tela abre com ano corrente selecionado e carrega resumos desse ano via API
- [ ] Submit com ano vazio bloqueado (validação)
- [ ] Limpar reseta ano corrente + recarrega
- [ ] Mês opcional restringe subconjunto (client passa `mes` quando preenchido)
- [ ] Gate: `cd frontend && npm run build && npm run lint`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(frontend): mandatory year filter on folha pagamento screen`

---

### T6: Excluir 13º da evolução — repository + adapter + testes dashboard

**What**: `findUltimos12MesesRegulares` exclui `decimoTerceiro=true`; adapter usa novo método; remover `findUltimos12Meses`; testes Dashboard total + scoped.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/infrastructure/ResumoFolhaPagamentoRepository.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaConsultaAdapter.java`
- `backend/src/test/java/br/com/techne/sistemafolha/dashboard/application/DashboardServiceTest.java`

**Depends on**: None (código independente; fase após Folha FE)  
**Reuses**: `FolhaConsultaPort`, mocks existentes em `DashboardServiceTest`  
**Requirement**: DASH-01, DASH-02

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Query filtra `(decimoTerceiro = false OR decimoTerceiro IS NULL)`
- [ ] Adapter chama método `Regulares`; método antigo removido se sem referências
- [ ] Testes novos (mínimo 2): evolução total ignora resumo 13º na mesma competência; evolução scoped idem
- [ ] Gate: `cd backend && mvn test -Dtest=DashboardServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `fix(dashboard): exclude decimo terceiro from folha evolution series`

---

### T7: Frontend Dashboard — remover mock de evolução

**What**: Remover array hardcoded quando `evolucaoMensal` vazio; exibir empty state no chart.  
**Where**: `frontend/src/pages/Dashboard/index.tsx`

**Depends on**: T6  
**Reuses**: Padrão empty state existente na página (se houver)  
**Requirement**: DASH-05 (spec AC evolução vazia)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Sem dados mock (linhas ~85–92 removidas)
- [ ] `evolucaoMensal` vazio → chart vazio ou mensagem orientativa (não valores fictícios)
- [ ] Gate: `cd frontend && npm run build && npm run lint`

**Tests**: none  
**Gate**: build  
**Commit**: `fix(frontend): remove hardcoded dashboard evolution mock data`

---

### T8: Gate final + handoff

**What**: Rodar suite completa; atualizar traceability em spec; preparar Verifier.  
**Where**: `_docs/specs/features/funcionarios-folha-dashboard-ux/spec.md` (status reqs), `STATE.md` (handoff)

**Depends on**: T3, T5, T7  
**Reuses**: Gate commands acima  
**Requirement**: Todos (FUNC/DASH/FOLH)

**Tools**:
- MCP: NONE
- Skill: `tlc-spec-driven` (Verifier automático pós último commit de código)

**Done when**:
- [ ] Full gate passa: `cd backend && mvn test && cd ../frontend && npm run lint && npm run build`
- [ ] Requirement IDs marcados Implementing/ready for Verifier
- [ ] Handoff STATE.md atualizado → Execute complete, Verifier next

**Tests**: full suite  
**Gate**: full  
**Commit**: `chore(funcionarios-folha-dashboard-ux): complete feature gate` (opcional — pode ser só doc se usuário pedir sem commit)

---

## Phase Execution Map

```
Phase 1:  T1 ──→ T2
Phase 2:  T3
Phase 3:  T4
Phase 4:  T5
Phase 5:  T6 ──→ T7
Phase 6:  T8
```

Execution is strictly sequential — one task at a time, in order.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Enum + repository JPQL | 1 enum + 1 query | ✅ Granular |
| T2: Service + controller + tests | 2 classes BE + 1 test (coeso: um endpoint) | ✅ Granular |
| T3: FE Funcionários | 2 arquivos FE mesma feature | ✅ Granular (coeso UI) |
| T4: BE Folha ano/mes | service + controller + tests | ✅ Granular |
| T5: FE Folha | 2 arquivos FE mesma feature | ✅ Granular |
| T6: 13º evolução BE | repo + adapter + tests | ✅ Granular |
| T7: Dashboard FE mock | 1 arquivo | ✅ Granular |
| T8: Gate final | docs + CI | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | Phase 1 start | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | Phase 2 after T2 | ✅ Match |
| T4 | None (phase order) | Phase 3 standalone | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | None (phase order) | Phase 5 start | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | T3, T5, T7 | Phase 6 after 2,4,5 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Repository + enum | none | none | ✅ OK |
| T2 | FuncionarioService + Controller | unit (service) | unit | ✅ OK |
| T3 | FE pages/services | none | none | ✅ OK |
| T4 | ResumoFolhaPagamentoService + Controller | unit (service) | unit | ✅ OK |
| T5 | FE pages/services | none | none | ✅ OK |
| T6 | Repository + adapter + DashboardService | unit (dashboard) | unit | ✅ OK |
| T7 | FE Dashboard page | none | none | ✅ OK |
| T8 | Docs only | full suite | full | ✅ OK |

---

## Requirement Traceability (Tasks)

| Req ID | Task(s) |
| ------ | ------- |
| FUNC-01 | T3 |
| FUNC-02 | T2, T3 |
| FUNC-03 | T1, T2, T3 |
| FUNC-04 | T1, T2, T3 |
| FUNC-05 | T1, T2, T3 |
| FUNC-06 | T3 |
| FUNC-07 | T3 |
| DASH-01 | T6 |
| DASH-02 | T6 |
| DASH-05 | T7 |
| FOLH-01 | T4, T5 |
| FOLH-02 | T4, T5 |
| FOLH-03 | T4, T5 |
| FOLH-04 | T4 |

**Coverage:** 13 requirements → 8 tasks, 0 unmapped

---

## Tools & Skills (Execute)

| Task | Recommended MCP | Recommended Skill |
| ---- | --------------- | ------------------- |
| T1 | NONE | `jpa-performance` |
| T2 | NONE | `spring-boot-new-endpoint` |
| T3 | NONE | NONE |
| T4 | NONE | `spring-boot-new-endpoint`, `jpa-performance` |
| T5 | NONE | NONE |
| T6 | NONE | NONE |
| T7 | NONE | NONE |
| T8 | NONE | `tlc-spec-driven` (Verifier) |

**Confirme antes do Execute:** MCPs disponíveis (context7, sonarqube) são opcionais; skills acima cobrem o escopo.
