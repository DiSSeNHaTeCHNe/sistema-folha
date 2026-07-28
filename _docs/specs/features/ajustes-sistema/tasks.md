# Ajustes Sistema Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/ajustes-sistema/design.md`  
**Status**: Draft

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `AGENTS.md` (raiz), `backend/AGENTS.md`, `frontend/AGENTS.md`, `backend/src/test/java/**/BeneficioMensalServiceTest.java`, `backend/src/test/java/**/BeneficioMensalControllerWebMvcTest.java` — frontend: **no Vitest configured** (`frontend/package.json` has no `test` script).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Benefício application service | unit | All branches for `listarCompetenciasParaUsuario`; 1:1 BEN-06/BEN-07 ACs; ACL negado + centros vazios + acessoTotal + filtro ano/mês | `backend/src/test/java/**/beneficios/application/*Test.java` | `cd backend && mvn test -Dtest=BeneficioMensalServiceTest` |
| Benefício REST controller | e2e (WebMvcTest) | Happy path `GET /competencias`; delegação ao service com auth | `backend/src/test/java/**/beneficios/api/*WebMvcTest.java` | `cd backend && mvn test -Dtest=BeneficioMensalControllerWebMvcTest` |
| Benefício repository (JPQL) | none | Covered indirectly via service unit tests (mock repo) | — | build gate only |
| DTO / projection records | none | — | — | build gate only |
| Frontend (Layout, routes, pages, services) | none | Build gate only — no test runner in FE today (AD-004 target ≠ current) | — | `cd frontend && npm run build` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | After T1–T3 (backend unit + webmvc only) | `cd backend && mvn test -Dtest=BeneficioMensalServiceTest,BeneficioMensalControllerWebMvcTest` |
| Full | After any backend change | `cd backend && mvn test` |
| Build | After T4–T6 (frontend tasks) | `cd frontend && npm run build` |
| Release | After last task (T6) before Verifier | `cd backend && mvn test && cd ../frontend && npm run build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Backend — Competências & DTO extend

Foundation for Benefícios drill-down (BEN-06, BEN-07, BEN-03 data).

```
T1 → T2 → T3
```

### Phase 2: Frontend — Senha self-service

Independent of Phase 1 (SENHA-01…04).

```
T4
```

### Phase 3: Frontend — Menu Cadastros ADMIN

Independent of Phases 1–2 (MENU-01…03).

```
T5
```

### Phase 4: Frontend — Benefícios drill-down UX

Depends on Phase 1 API (BEN-01…05).

```
T6
```

**Batch packing:** 6 tasks total → **1 worker batch** (≤8). Execute inline, no sub-agent offer required.

---

## Task Breakdown

### T1: Repository + DTOs de competências de benefício

**What**: Criar `BeneficioMensalCompetenciaResumoDTO`, projection `BeneficioMensalCompetenciaProjection`, e queries JPQL agregadas por competência (com/sem filtro centro de custo) no repository.

**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/api/BeneficioMensalCompetenciaResumoDTO.java` (novo)
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/infrastructure/BeneficioMensalCompetenciaProjection.java` (novo)
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/infrastructure/BeneficioMensalRepository.java` (modify)

**Depends on**: None  
**Reuses**: Padrão dual-query de `resumoPorCompetencia` / `resumoPorCompetenciaAndCentroCustoIds`  
**Requirements**: BEN-06 (data layer)

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [ ] DTO record com `competenciaInicio`, `competenciaFim`, `totalFuncionarios`, `totalBeneficios`, `qtdLancamentos`
- [ ] Projection interface espelha campos agregados
- [ ] Duas queries `@Query`: sem filtro centro (acesso total) e com `centroCustoIds IN :ids`
- [ ] Queries filtram `ativo = true`, agrupam por par competência, ordenam `competenciaInicio DESC`
- [ ] `mvn compile` passa em `backend/`

**Tests**: none  
**Gate**: build (`cd backend && mvn compile -q`)

**Commit**: `feat(beneficios): add competencia aggregation repository and DTO`

---

### T2: Service listarCompetenciasParaUsuario + testes unitários

**What**: Implementar `listarCompetenciasParaUsuario(login, ano, mes)` com ACL (reuso `obterContextoAcesso`, `acessoNegado`, `centrosParaFiltro`) e helper `periodoDe(ano, mes)` espelhando Folha; adicionar testes unitários cobrindo ACs BEN-06/BEN-07 e edge cases ACL.

**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/application/BeneficioMensalService.java` (modify)
- `backend/src/test/java/br/com/techne/sistemafolha/beneficios/application/BeneficioMensalServiceTest.java` (modify)

**Depends on**: T1  
**Reuses**: Pipeline ACL de `listarPorCompetenciaParaUsuario`; `ResumoFolhaPagamentoService.periodoDe` como referência  
**Requirements**: BEN-06, BEN-07

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [ ] Método retorna `List<BeneficioMensalCompetenciaResumoDTO>` mapeado da projection
- [ ] ACL negado → lista vazia (sem chamar repo)
- [ ] Centros vazios (não acessoTotal) → lista vazia
- [ ] acessoTotal → query sem filtro centro
- [ ] Escopo parcial → query com centros do contexto
- [ ] Filtro ano obrigatório; mês opcional restringe ao mês
- [ ] Testes novos (mínimo 4): acesso negado, acessoTotal com dados, escopo parcial, filtro ano+mes
- [ ] Gate passa: `cd backend && mvn test -Dtest=BeneficioMensalServiceTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(beneficios): list competencias with ACL-scoped aggregation`

---

### T3: Extend BeneficioMensalDTO + endpoint GET /competencias + WebMvcTest

**What**: Adicionar `cargoDescricao`, `linhaNegocioId`, `linhaNegocioDescricao` ao DTO e `toDTO()`; garantir `@Transactional(readOnly = true)` nos métodos de listagem; expor `GET /beneficio-mensal/competencias`; testes WebMvcTest.

**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/api/BeneficioMensalDTO.java` (modify)
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/application/BeneficioMensalService.java` (modify — toDTO + transactional)
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/api/BeneficioMensalController.java` (modify)
- `backend/src/test/java/br/com/techne/sistemafolha/beneficios/api/BeneficioMensalControllerWebMvcTest.java` (modify)

**Depends on**: T2  
**Reuses**: Validação `@Min/@Max` de `ResumoFolhaPagamentoController`; padrão WebMvcTest existente  
**Requirements**: BEN-03 (data), BEN-06

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `jpa-performance`

**Done when**:
- [ ] DTO estendido; `toDTO()` popula cargo e linha de negócio do funcionário
- [ ] Endpoint `GET /competencias?ano=&mes=` delega a `listarCompetenciasParaUsuario`
- [ ] WebMvcTest: status 200 + verify service chamado com login e params
- [ ] Testes existentes de BeneficioMensalServiceTest continuam passando
- [ ] Gate passa: `cd backend && mvn test -Dtest=BeneficioMensalServiceTest,BeneficioMensalControllerWebMvcTest`

**Tests**: e2e (WebMvcTest) + regression unit  
**Gate**: quick

**Commit**: `feat(beneficios): expose competencias endpoint and enrich DTO for employee cards`

---

### T4: Troca de senha — service fix + AlterarSenhaDialog + Layout menu

**What**: Corrigir `usuarioService.alterarSenha` para POST com query params; criar `AlterarSenhaDialog` (RHF, 3 campos, validação min 6 + confirmação); adicionar item "Alterar senha" no menu AccountCircle do Layout.

**Where**:
- `frontend/src/services/usuarioService.ts` (modify)
- `frontend/src/components/AlterarSenhaDialog/index.tsx` (novo)
- `frontend/src/components/Layout/index.tsx` (modify)

**Depends on**: None  
**Reuses**: Validação de `Usuarios/index.tsx`; padrão Dialog MUI; `useNotification` para sucesso/erro  
**Requirements**: SENHA-01, SENHA-02, SENHA-03, SENHA-04

**Tools**:
- MCP: NONE
- Skill: `forms-validation` (referência — brownfield RHF manual conforme AD-004)

**Done when**:
- [ ] Service chama `POST /usuarios/{id}/alterar-senha?senhaAtual=&novaSenha=`
- [ ] Dialog abre via menu; campos senha atual, nova, confirmar (type password)
- [ ] Validação client: min 6 chars, confirmação igual — sem API se inválido
- [ ] Senha atual incorreta (400) → mensagem clara, dialog permanece aberto
- [ ] Sucesso → dialog fecha + notificação sucesso
- [ ] Sempre usa `user.id` do AuthContext
- [ ] Gate passa: `cd frontend && npm run build`

**Tests**: none  
**Gate**: build

**Commit**: `feat(auth): add self-service password change from account menu`

---

### T5: Menu Cadastros ADMIN — isAdmin, AdminRoute, Layout, Dashboard notification

**What**: Criar `permissions.ts` (`isAdmin`, `CADASTRO_ROUTES`); criar `AdminRoute` wrapper; envolver 8 rotas de cadastro; ocultar seção Cadastros no Layout para não-ADMIN; notificação no Dashboard após redirect por acesso negado.

**Where**:
- `frontend/src/utils/permissions.ts` (novo)
- `frontend/src/routes/AdminRoute.tsx` (novo)
- `frontend/src/routes/index.tsx` (modify)
- `frontend/src/components/Layout/index.tsx` (modify)
- `frontend/src/pages/Dashboard/index.tsx` (modify)

**Depends on**: None  
**Reuses**: Padrão `PrivateRoute`; `useNotification` + `Notification` component  
**Requirements**: MENU-01, MENU-02, MENU-03

**Tools**:
- MCP: NONE
- Skill: `component-architecture` (referência), `spring-security` (AD-011 semantics)

**Done when**:
- [ ] `isAdmin` retorna true só com `ADMIN` em permissoes (não `ACESSO_TOTAL`)
- [ ] Layout: seção Cadastros + divider anterior ocultos para não-ADMIN
- [ ] 8 rotas cadastro wrapped com `AdminRoute`: `/usuarios`, `/linhas-negocio`, `/centros-custo`, `/cargos`, `/rubricas`, `/tipos-beneficio`, `/organograma`, `/importacao`
- [ ] Não-ADMIN em rota cadastro → redirect `/dashboard` com `state.acessoNegado`
- [ ] Dashboard exibe snackbar "Acesso negado. Apenas administradores." e limpa state
- [ ] ADMIN vê menu e acessa rotas normalmente
- [ ] Gate passa: `cd frontend && npm run build`

**Tests**: none  
**Gate**: build

**Commit**: `feat(auth): restrict Cadastros menu and routes to ADMIN users`

---

### T6: Benefícios Mensais — drill-down UX (Resumo → Funcionários → Dialog)

**What**: Adicionar `listarCompetencias` ao service FE; refatorar `BeneficiosMensais/index.tsx` para fluxo 3 níveis espelhando Folha (filtros ano/mês, tabela competências, cards funcionários, dialog detalhe); remover UX expandível por tipo.

**Where**:
- `frontend/src/services/beneficioMensalService.ts` (modify)
- `frontend/src/types/index.ts` (modify — tipo `BeneficioMensalCompetenciaResumo` se necessário)
- `frontend/src/pages/BeneficiosMensais/index.tsx` (rewrite)

**Depends on**: T3  
**Reuses**: `FolhaPagamento/index.tsx` (state machine, filtros RHF, cards, dialog); `centroCustoService`, `linhaNegocioService`  
**Requirements**: BEN-01, BEN-02, BEN-03, BEN-04, BEN-05

**Tools**:
- MCP: NONE
- Skill: `component-architecture`, `api-client`

**Done when**:
- [ ] Tela inicial: tabela resumos por competência + filtros ano (select) e mês (opcional) + Filtrar/Limpar
- [ ] Colunas: competência, total funcionários, total R$, qtd lançamentos, botão Ver Funcionários
- [ ] Drill-down: cards com nome, cargo, CC, linha, total R$, botão Ver Benefícios
- [ ] Dialog: código, descrição tipo, valor, observação
- [ ] Voltar preserva filtros ano/mês
- [ ] Empty states conforme spec (sem dados / sem funcionários após filtro)
- [ ] Gate release passa: `cd backend && mvn test && cd ../frontend && npm run build`

**Tests**: none  
**Gate**: release

**Commit**: `feat(beneficios): align monthly benefits UX with payroll drill-down flow`

---

## Phase Execution Map

```
Phase 1:  T1 ──→ T2 ──→ T3
Phase 2:  T4                    (parallel-safe after spec; sequential in Execute)
Phase 3:  T5                    (parallel-safe; sequential in Execute)
Phase 4:  T6                    (depends T3)

Full order: T1 → T2 → T3 → T4 → T5 → T6
```

Execution is strictly sequential — one task at a time, one commit per task.

**Batch:** 6 tasks → single inline Execute batch (no sub-agent offer).

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: DTO + projection + repo queries | 3 arquivos, 1 conceito (agregação) | ✅ Granular |
| T2: Service method + unit tests | 1 service + tests | ✅ Granular |
| T3: DTO extend + controller + WebMvcTest | 1 endpoint + enrichments | ✅ Granular |
| T4: Senha dialog + service fix + Layout | 1 feature vertical FE | ✅ Granular |
| T5: ADMIN guard (utils + route + layout + dashboard) | 1 feature vertical FE | ✅ Granular |
| T6: Benefícios page refactor + service client | 1 page rewrite | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | Entry point Phase 1 | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | None | Independent Phase 2 | ✅ Match |
| T5 | None | Independent Phase 3 | ✅ Match |
| T6 | T3 | T3 → T6 (Phase 4) | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Repository + DTO | none | none | ✅ OK |
| T2 | Application service | unit | unit | ✅ OK |
| T3 | Controller + service toDTO | e2e + unit regression | e2e (WebMvcTest) + regression | ✅ OK |
| T4 | Frontend components/services | none | none | ✅ OK |
| T5 | Frontend routes/layout | none | none | ✅ OK |
| T6 | Frontend page/service | none | none | ✅ OK |

---

## Requirement Traceability (Tasks)

| Requirement ID | Task(s) |
| -------------- | ------- |
| MENU-01 | T5 |
| MENU-02 | T5 |
| MENU-03 | T5 |
| SENHA-01 | T4 |
| SENHA-02 | T4 |
| SENHA-03 | T4 |
| SENHA-04 | T4 |
| BEN-01 | T6 |
| BEN-02 | T6 |
| BEN-03 | T3, T6 |
| BEN-04 | T6 |
| BEN-05 | T6 |
| BEN-06 | T1, T2, T3, T6 |
| BEN-07 | T2 |

**Coverage:** 13 requirements → 6 tasks, all mapped ✅

---

## Tools & Skills (Execute)

Before starting Execute, confirm per task:

| Task | Recommended MCP | Recommended Skill |
| ---- | --------------- | ----------------- |
| T1–T3 | NONE | `jpa-performance`, `spring-boot-new-endpoint` |
| T4 | NONE | `forms-validation` (ref) |
| T5 | NONE | `spring-security` (AD-011), `component-architecture` (ref) |
| T6 | NONE | `api-client`, `component-architecture` (ref) |

**Available MCPs in project:** Linear, SonarQube, Context7, Docker  
**Note:** Context7 only if API/library uncertainty during Execute.
