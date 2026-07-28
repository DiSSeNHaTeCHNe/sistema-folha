# Ajustes — Listagens, Filtros e UX — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/ajustes-listagens/design.md`  
**Status**: Draft

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `backend/AGENTS.md` (JUnit 5 + Mockito unit tests for services), `frontend/AGENTS.md` (Vitest/Playwright target — not configured in `package.json`; build gate only for FE).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Service (Folha) | unit | AC FOLHA-ORD-*: ordenação `rubricaCodigo` ↑, desempate `id` ↑; lista vazia | `backend/src/test/java/**/folha/application/*Test.java` | `mvn test -Dtest=FolhaPagamentoServiceTest` |
| Service (Rubricas) | unit | AC RUB-*: patterns ILIKE, tri-state ATIVO/INATIVO/TODOS, trim vazio, delegação repo | `backend/src/test/java/**/cadastros/application/RubricaServiceTest.java` | `mvn test -Dtest=RubricaServiceTest` |
| Service (Usuários) | unit | AC USR-*: filtros nome/login/funcionarioId (AND), trim vazio, delegação repo ordenado | `backend/src/test/java/**/auth/application/UsuarioServiceTest.java` | `mvn test -Dtest=UsuarioServiceTest` |
| Controller / Repository | none | Coberto indiretamente via service unit tests | — | build gate only |
| FE pages / components | none | Gate build + typecheck; SENHA-VIS-* verificado na validation manual | `frontend/src/**` | `npm run build` (from `frontend/`) |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick (BE) | After T1, T2, T3 | `cd backend && mvn test -Dtest=FolhaPagamentoServiceTest` / `RubricaServiceTest` / `UsuarioServiceTest` |
| Build (FE) | After T4, T5, T6 | `cd frontend && npm run build` |
| Full | After all tasks (pre-Verifier) | `cd backend && mvn test` then `cd frontend && npm run build` |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

### Phase 1: Backend — ordenação e filtros

```
T1 → T2 → T3
```

### Phase 2: Frontend — UI e integração

```
T4 → T5 → T6
```

**Batch sizing:** 6 tasks total → **1 batch inline** (≤ ~8); no sub-agent offer required.

---

## Task Breakdown

### T1: Ordenar rubricas no detalhe Folha (BE)

**What**: Adicionar sort pós-ACL em `consultarPorFuncionario` por `rubricaCodigo` ↑, desempate `id` ↑.  
**Where**: `backend/src/main/java/.../folha/application/FolhaPagamentoService.java`, `backend/src/test/java/.../folha/application/FolhaPagamentoServiceTest.java`  
**Depends on**: None  
**Reuses**: Comparator pattern do design; testes ACL existentes em `FolhaPagamentoServiceTest`  
**Requirement**: FOLHA-ORD-01, FOLHA-ORD-02, FOLHA-ORD-03

**Tools**:

- MCP: NONE
- Skill: `jpa-performance` (sort pós-stream, sem N+1)

**Done when**:

- [ ] `consultarPorFuncionario` retorna linhas ordenadas por `rubricaCodigo` (case-insensitive), desempate `id`
- [ ] Teste novo: duas linhas com códigos `200` e `100` → resultado `[100, 200]`
- [ ] Teste novo: mesmo `rubricaCodigo`, ids diferentes → ordem por `id` crescente
- [ ] Gate: `cd backend && mvn test -Dtest=FolhaPagamentoServiceTest` — all pass
- [ ] Test count: baseline + ≥2 novos testes de ordenação (sem deleções silenciosas)

**Tests**: unit  
**Gate**: quick (BE)

**Commit**: `feat(folha): ordenar rubricas por codigo no detalhe do funcionario`

---

### T2: Filtros e ordenação de Rubricas (BE)

**What**: `RubricaStatusFiltro`, `findByFiltros` no repository, `listar(codigo, descricao, status)` no service/controller, testes unitários.  
**Where**:

- `backend/.../cadastros/api/RubricaStatusFiltro.java` *(novo)*
- `backend/.../cadastros/infrastructure/RubricaRepository.java`
- `backend/.../cadastros/application/RubricaService.java`
- `backend/.../cadastros/api/RubricaController.java`
- `backend/src/test/java/.../cadastros/application/RubricaServiceTest.java` *(novo)*

**Depends on**: None  
**Reuses**: `FuncionarioStatusFiltro`, `FuncionarioRepository.findByFiltros`, `FuncionarioService.resolverAtivo`  
**Requirement**: RUB-02…RUB-11

**Tools**:

- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `jpa-performance`

**Done when**:

- [ ] `GET /rubricas?codigo=&descricao=&status=` com default `status=ATIVO`
- [ ] Service normaliza: trim strings; vazio → pattern `null`; `%valor%` para ILIKE
- [ ] `resolverAtivo`: ATIVO→true, INATIVO→false, TODOS→null
- [ ] Repository JPQL com `ORDER BY r.codigo ASC`
- [ ] Testes: status ATIVO/INATIVO/TODOS; código `%ABC%`; descrição `%foo%`; trim ignora espaços
- [ ] Gate: `cd backend && mvn test -Dtest=RubricaServiceTest` — all pass
- [ ] Test count: ≥5 testes novos

**Tests**: unit  
**Gate**: quick (BE)

**Commit**: `feat(rubricas): filtros por codigo descricao status e ordenacao`

---

### T3: Filtros e ordem alfabética de Usuários (BE)

**What**: `findByFiltros` no repository, `listar(nome, login, funcionarioId)` no service/controller, testes de listagem.  
**Where**:

- `backend/.../auth/infrastructure/UsuarioRepository.java`
- `backend/.../auth/application/UsuarioService.java`
- `backend/.../auth/api/UsuarioController.java`
- `backend/src/test/java/.../auth/application/UsuarioServiceTest.java`

**Depends on**: None  
**Reuses**: `FuncionarioRepository.findByFiltros` (padrão ILIKE + ORDER BY)  
**Requirement**: USR-01…USR-08

**Tools**:

- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `jpa-performance`

**Done when**:

- [ ] `GET /usuarios?nome=&login=&funcionarioId=` substitui listagem cega de `listarTodos()`
- [ ] Apenas usuários `ativo=true` (comportamento preservado)
- [ ] Filtros combinados com AND; trim em nome/login
- [ ] `ORDER BY u.nome ASC, u.login ASC` na query
- [ ] Testes: filtro nome `%Maria%`; login `%adm%`; funcionarioId exato; critérios vazios ignorados
- [ ] Gate: `cd backend && mvn test -Dtest=UsuarioServiceTest` — all pass
- [ ] Test count: baseline + ≥4 testes de listagem

**Tests**: unit  
**Gate**: quick (BE)

**Commit**: `feat(usuarios): aplicar filtros e ordenacao alfabetica na listagem`

---

### T4: Card de filtros na tela Rubricas (FE)

**What**: Card Filtros (código, descrição, status), integração `rubricaService.listar(filtros)`, estado vazio.  
**Where**:

- `frontend/src/pages/Rubricas/index.tsx`
- `frontend/src/services/rubricaService.ts`

**Depends on**: T2  
**Reuses**: Layout de filtros de `Usuarios/index.tsx` / `Funcionarios/index.tsx`  
**Requirement**: RUB-01, RUB-09, RUB-10

**Tools**:

- MCP: NONE
- Skill: `forms-validation`, `component-architecture`

**Done when**:

- [ ] Card **Filtros** acima da tabela (padrão visual Usuários/Funcionários)
- [ ] Campos: Id/Código, Descrição, Status (Ativo/Inativo/Todos; default Ativo)
- [ ] Botões Filtrar e Limpar (reset + recarga default)
- [ ] Mensagem **"Nenhuma rubrica encontrada"** quando lista vazia
- [ ] `rubricaService.listar` envia query params alinhados ao BE
- [ ] Gate: `cd frontend && npm run build` — pass

**Tests**: none  
**Gate**: build (FE)

**Commit**: `feat(rubricas): adicionar filtros na listagem`

---

### T5: Olhinho no dialog Alterar senha (FE)

**What**: Toggle visibilidade independente em Nova senha e Confirmar; senha atual sem olho; reset ao abrir/fechar.  
**Where**: `frontend/src/components/AlterarSenhaDialog/index.tsx`  
**Depends on**: None  
**Reuses**: Padrão `Visibility`/`VisibilityOff` de `Usuarios/index.tsx`  
**Requirement**: SENHA-VIS-01…SENHA-VIS-06

**Tools**:

- MCP: NONE
- Skill: `component-architecture`, `forms-validation`

**Done when**:

- [ ] Nova senha e Confirmar nova senha com `IconButton` + `InputAdornment`
- [ ] Toggles independentes (`showNovaSenha`, `showConfirmarSenha`); default oculto
- [ ] Senha atual: `type="password"` fixo, sem ícone
- [ ] Reset toggles em `useEffect(open)` e `handleClose`
- [ ] Validação/API inalteradas
- [ ] Gate: `cd frontend && npm run build` — pass

**Tests**: none  
**Gate**: build (FE)

**Commit**: `feat(auth): toggle visibilidade em nova senha no dialog`

---

### T6: Verificar integração Usuários (FE smoke)

**What**: Confirmar que filtros existentes funcionam com BE corrigido; ajuste mínimo se necessário.  
**Where**: `frontend/src/pages/Usuarios/index.tsx` *(somente se necessário)*  
**Depends on**: T3  
**Reuses**: `usuarioService.listar(filtros)` existente  
**Requirement**: USR-06, USR-07

**Tools**:

- MCP: NONE
- Skill: `api-client`

**Done when**:

- [ ] Carga inicial lista usuários em ordem alfabética (via BE)
- [ ] Filtrar por nome, login e funcionário retorna subset correto
- [ ] Limpar restaura listagem completa
- [ ] Se FE precisar ajuste (ex.: param vazio), diff mínimo documentado no commit
- [ ] Gate: `cd frontend && npm run build` — pass

**Tests**: none  
**Gate**: build (FE)

**Commit**: `fix(usuarios): alinhar tela aos filtros da api` *(ou `chore(usuarios): validar filtros pos-be` se zero diff)*

---

## Phase Execution Map

```
Phase 1:  T1 ──→ T2 ──→ T3
Phase 2:  T4 ──→ T5 ──→ T6
              ↑ T2      ↑ T3
```

Execution strictly sequential — one task at a time, one commit per task.

**Single batch (6 tasks)** — Execute inline; Verifier runs automatically after T6 commit.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Sort Folha service + test | 1 service method + tests | ✅ Granular |
| T2: Rubricas BE stack + test | 1 endpoint coeso (enum/repo/service/controller) | ✅ Granular |
| T3: Usuários BE stack + test | 1 endpoint coeso | ✅ Granular |
| T4: Rubricas FE filtros | 1 page + 1 service | ✅ Granular |
| T5: AlterarSenhaDialog toggles | 1 component | ✅ Granular |
| T6: Usuários FE smoke | verificação + ajuste mínimo | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | T1 (início Phase 1) | ✅ Match |
| T2 | None | T2 após T1 | ✅ Match |
| T3 | None | T3 após T2 | ✅ Match |
| T4 | T2 | T4 após T3, nota ↑ T2 | ✅ Match |
| T5 | None | T5 após T4 | ✅ Match |
| T6 | T3 | T6 após T5, nota ↑ T3 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | Folha Service | unit | unit | ✅ OK |
| T2 | Rubrica Service | unit | unit | ✅ OK |
| T3 | Usuario Service | unit | unit | ✅ OK |
| T4 | FE page | none | none | ✅ OK |
| T5 | FE component | none | none | ✅ OK |
| T6 | FE page (smoke) | none | none | ✅ OK |

---

## Requirement → Task Map

| Requirement IDs | Task |
| --------------- | ---- |
| FOLHA-ORD-01…03 | T1 |
| RUB-01…11 | T2, T4 |
| USR-01…08 | T3, T6 |
| SENHA-VIS-01…06 | T5 |

**Coverage:** 28 requirements → 6 tasks ✅
