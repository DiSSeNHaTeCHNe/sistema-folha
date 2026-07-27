# ACL — Role `ACESSO_TOTAL` Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/acl-acesso-total-role/design.md`  
**Status**: Done  
**Approach**: Early-return `ACESSO_TOTAL` no port + Flyway seed + FE picker + AuthContext order fix  
**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` + AD-004 (FE Vitest TARGET — obrigação atual = build/lint), `acl-acesso-total-role/spec.md` (ATOT ACs), AD-007/AD-011, skill `flyway-migration-writer` (T2).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Application ACL (`OrganogramaAcessoService`) | unit (Mockito) | ATOT-01/02/05/08: `ACESSO_TOTAL` sem funcionário → `acessoTotal=true` + `usuarioPodeAcessarCentroCusto` true; sem permissão → deny SEM_FUNCIONARIO; só `ADMIN` → `acessoTotal=false`; centros vazios com total OK | `backend/src/test/java/**/organograma/acesso/application/OrganogramaAcessoServiceTest.java` | `cd backend && mvn test -Dtest=OrganogramaAcessoServiceTest` |
| Application Folha (regressão flag) | unit (Mockito) | ATOT-03: `consultarPorPeriodo` com mock `acessoTotal=true` retorna linhas (já coberto por casos existentes — reforçar se DTO total sem funcionário mudar shape) | `backend/src/test/java/**/folha/application/FolhaPagamentoServiceTest.java` | `cd backend && mvn test -Dtest=FolhaPagamentoServiceTest` |
| SecurityConfig ADMIN | unit (MockMvc) | ATOT-06: sem alteração de matchers — regressão suite existente | `**/config/SecurityConfigTipoBeneficioTest.java` | `cd backend && mvn test -Dtest=SecurityConfigTipoBeneficioTest` |
| Flyway migration | none | Build/migrate review; SQL idempotente | `db/migration/V1.15__*.sql` | review + app boot / `mvn test` suite |
| FE Usuarios picker / AuthContext | none | AD-004 / TESTING.md: sem Vitest obrigatório; `npm run build` | `frontend/src/pages/Usuarios/index.tsx`, `frontend/src/contexts/AuthContext.tsx` | `cd frontend && npm run build` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após T1 (unit ACL) | `cd backend && mvn test -Dtest=OrganogramaAcessoServiceTest,FolhaPagamentoServiceTest,SecurityConfigTipoBeneficioTest` |
| Full | Após T3–T4 FE + fechamento | `cd backend && mvn test && cd ../frontend && npm run build` |
| Build | Migration / fechamento fase | `cd backend && mvn clean package -DskipTests` (opcional) + Full |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Port ACL + testes

```
T1
```

### Phase 2: Seed Flyway

```
T2
```

### Phase 3: Frontend

```
T3 → T4
```

### Phase 4: Gate + handoff

```
T5
```

**Batch packing:** 5 tasks → **1 batch** (≤ ~8) → Execute **inline** (sem sub-agents).

---

## Task Breakdown

### T1: Early-return `ACESSO_TOTAL` em `OrganogramaAcessoService` + testes

**What**: Em `obterContextoAcesso`, após load do usuário (não-null), se `permissoes` contém `ACESSO_TOTAL` (match exato, null-safe), retornar `AccessContextDTO` com `acessoTotal=true`, `motivoNegacao=null`, `centrosCustoIds` vazio, flags de vínculo conforme fato (`funcionario != null`), metadados de nó null no early-return; adicionar testes ATOT-01/05/08 (ATOT-02 já coberto pelo deny existente).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/organograma/acesso/application/OrganogramaAcessoService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/organograma/acesso/application/OrganogramaAcessoServiceTest.java`

**Depends on**: None  
**Reuses**: Design §OrganogramaAcessoService; helpers `negar` / fixtures do teste; constante `ACESSO_TOTAL`  
**Requirement**: ATOT-01, ATOT-02, ATOT-05, ATOT-08

**Tools**:
- MCP: NONE
- Skill: NONE (lógica ACL local)

**Done when**:
- [x] `ACESSO_TOTAL` sem `funcionario` → `acessoTotal=true`; `usuarioPodeAcessarCentroCusto(id)` true para id arbitrário
- [x] Só `ADMIN` (sem `ACESSO_TOTAL`) sem funcionário → `acessoTotal=false`, SEM_FUNCIONARIO
- [x] Sem permissão total → fluxos deny/nó existentes intactos
- [x] Fixture seta `permissoes` (nunca NPE em `getPermissoes()`)
- [x] Gate quick: `cd backend && mvn test -Dtest=OrganogramaAcessoServiceTest,FolhaPagamentoServiceTest,SecurityConfigTipoBeneficioTest`
- [x] Test count: ≥2 novos testes (+ regressões); sem silent deletions

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(acl): grant acessoTotal via ACESSO_TOTAL permission`

---

### T2: Flyway V1.15 — seed `ACESSO_TOTAL` para admin

**What**: Criar migration idempotente que insere `ACESSO_TOTAL` para `usuarios.login = 'admin'`.  
**Where**: `backend/src/main/resources/db/migration/V1.15__grant_acesso_total_to_admin.sql`

**Depends on**: T1  
**Reuses**: PK `usuario_permissoes` (V1.4); design SQL intent; skill flyway naming  
**Requirement**: ATOT-07

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer` (naming + idempotência)

**Done when**:
- [x] Arquivo `V1.15__grant_acesso_total_to_admin.sql` existe
- [x] `INSERT … SELECT … WHERE login = 'admin' ON CONFLICT DO NOTHING`
- [x] Sem hardcode frágil de id (preferir login)
- [x] Gate: arquivo presente + suite backend ainda verde (`mvn test` ou quick)

**Tests**: none  
**Gate**: quick  
**Commit**: `feat(acl): seed ACESSO_TOTAL for admin user`

---

### T3: FE — incluir `ACESSO_TOTAL` no picker de Usuários

**What**: Adicionar `ACESSO_TOTAL` a `permissoesDisponiveis` e cor de chip (`error`).  
**Where**: `frontend/src/pages/Usuarios/index.tsx`

**Depends on**: T2  
**Reuses**: Lista/checkbox e `getPermissaoColor` existentes  
**Requirement**: ATOT-09

**Tools**:
- MCP: NONE
- Skill: NONE (brownfield page; AD-004)

**Done when**:
- [x] `ACESSO_TOTAL` aparece no formulário de permissões
- [x] Chip color mapeado
- [x] Persistência continua via API existente (sem mudança de contrato)
- [x] Gate: `cd frontend && npm run build` (pode rodar em T5 se preferir um único build; T3 deve ao menos typecheck-safe)

**Tests**: none  
**Gate**: build (fechado em T5 Full; T3: arquivo alterado sem erro óbvio)  
**Commit**: `feat(acl): expose ACESSO_TOTAL on users permissions UI`

---

### T4: FE — `AuthContext.podeAcessarCentroCusto` honra `acessoTotal` primeiro

**What**: Reordenar checks: `acessoTotal` → true **antes** de exigir `temFuncionarioVinculado` / `temNoOrganograma`.  
**Where**: `frontend/src/contexts/AuthContext.tsx`

**Depends on**: T3  
**Reuses**: Design §AuthContext; `acessoUsuario` do `/auth/acesso`  
**Requirement**: ATOT-10 (cliente)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Ordem: null check → `acessoTotal` → vínculo → `centrosCustoIds.includes`
- [x] Comentário atualizado se necessário
- [x] Gate Full em T5

**Tests**: none  
**Gate**: build (via T5)  
**Commit**: `fix(acl): honor acessoTotal before organogram checks in AuthContext`

---

### T5: Gate Full + handoff + confirmação ATOT-06

**What**: Rodar suite backend completa + `npm run build`; confirmar `SecurityConfig` sem matcher novo para `ACESSO_TOTAL`; atualizar `STATE.md` handoff e marcar tasks Done.  
**Where**: `_docs/specs/STATE.md`, `_docs/specs/features/acl-acesso-total-role/tasks.md` (status)

**Depends on**: T4  
**Reuses**: Gate Full commands; `SecurityConfigTipoBeneficioTest`  
**Requirement**: ATOT-06, success criteria gate

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `cd backend && mvn test` exit 0
- [x] `cd frontend && npm run build` exit 0
- [x] `SecurityConfig` sem `hasRole("ACESSO_TOTAL")` / sem mudança indevida de ADMIN matchers
- [x] Handoff: Execute done → ready for Independent Verifier
- [x] Tasks status → Done (ou In Progress→Done)

**Tests**: none (orquestra gates existentes)  
**Gate**: full  
**Commit**: `docs(acl): handoff after ACESSO_TOTAL execute` (opcional / se user pedir commit)

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1
Phase 2:  T2
Phase 3:  T3 ──→ T4
Phase 4:  T5
```

Execution is strictly sequential. **1 batch (5 tasks)** → inline Execute.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: OrganogramaAcessoService + unit tests | 1 service + co-located tests | ✅ Granular |
| T2: Flyway V1.15 | 1 migration file | ✅ Granular |
| T3: Usuarios picker | 1 FE file (lista + chip) | ✅ Granular |
| T4: AuthContext order | 1 function | ✅ Granular |
| T5: Gate + handoff | orchestration / docs | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | (start) | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Application ACL OrganogramaAcessoService | unit | unit | ✅ OK |
| T2 | Flyway migration | none | none | ✅ OK |
| T3 | FE Usuarios | none | none | ✅ OK |
| T4 | FE AuthContext | none | none | ✅ OK |
| T5 | Docs / gate orchestration | none | none | ✅ OK |

Folha ATOT-03: coberto por testes existentes em `FolhaPagamentoServiceTest` (mock `acessoTotal`) — exercitado no gate quick de T1; sem task separada (evita deferral / duplicação).

---

## Requirement Traceability (tasks)

| Requirement ID | Task(s) |
| -------------- | ------- |
| ATOT-01 | T1 |
| ATOT-02 | T1 |
| ATOT-03 | T1 gate (FolhaPagamentoServiceTest existente) |
| ATOT-04 | T1 (deny path existente + sem ACESSO_TOTAL) |
| ATOT-05 | T1 |
| ATOT-06 | T5 (+ SecurityConfigTipoBeneficioTest no quick T1) |
| ATOT-07 | T2 |
| ATOT-08 | T1 |
| ATOT-09 | T3 |
| ATOT-10 | T4 (+ T1 BE para `/auth/acesso` via port) |
| ATOT-11 | Deferred — sem task |

---

## Tools question (before Execute)

Para cada task, quais ferramentas usar?

**MCPs disponíveis no ambiente:** Context7, SonarQube, Linear, Docker MCP (não necessários por default neste feature).  
**Skills relevantes:** `tlc-spec-driven` (obrigatória no Execute), `flyway-migration-writer` (T2), `spring-security` (consulta se tocar SecurityConfig — design diz **não** tocar).

Default proposto: **MCP NONE** em todas; Skill `flyway-migration-writer` só em T2; resto NONE.

Confirme tasks + matrix + tools para liberar Execute.
