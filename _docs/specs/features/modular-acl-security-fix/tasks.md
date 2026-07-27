# Modular ACL / Security Fix — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/modular-acl-security-fix/design.md`  
**Status**: Execute done  
**Approach**: A (short-circuit empty-set + refresh permitAll + Folha remover ACL)

**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` (AD-004 — FE fora deste fix salvo verificação estática MODACL-09), `modular-acl-security-fix/spec.md` (MODACL ACs), AD-007/008/009, skill `spring-security`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Application ACL (`BeneficioMensalService` empty-set) | unit (Mockito) | MODACL-01–05: restrito+empty list+resumo vazios + never unscoped; total ainda unscoped; restrito+IDs filtra; distinto de SEM_FUNCIONARIO | `backend/src/test/java/**/beneficios/application/BeneficioMensalServiceTest.java` | `cd backend && mvn test` |
| SecurityConfig / Auth refresh | unit (MockMvc `@WebMvcTest`) | MODACL-07–08: anon POST `/auth/refresh` ≠ 401; regressão ADMIN `tipo-beneficio` permanece | `backend/src/test/java/**/config/*Refresh*Test.java` ou `**/auth/api/*Refresh*Test.java` | `cd backend && mvn test` |
| Application ACL Folha delete | unit (Mockito) | MODACL-11–13: deny nunca softDelete; allow softDelete; acessoTotal allow | `backend/src/test/java/**/folha/application/FolhaPagamentoServiceTest.java` | `cd backend && mvn test` |
| Controller Folha DELETE wiring | none (thin) | Compile + HTTP contract via service bool → 204/404; padrão já em Benefício | `folha/api/FolhaPagamentoController.java` | build / suite |
| ArchUnit regression | unit (ArchUnit) | Zero foreign-infra em application fora allowlist AD-009 | `**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| FE `api.ts` | none | Static: refresh fetch sem Bearer (MODACL-09) — não alterar | `frontend/src/services/api.ts` | grep / read |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após T1–T3 unit/MockMvc | `cd backend && mvn test` |
| Full | Fechamento com ArchUnit + FE contract check | `cd backend && mvn test && cd ../frontend && npm run build` |
| Build | Config-only / fechamento fase | `cd backend && mvn clean package` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Benefício empty-set

```
T1
```

### Phase 2: Auth refresh permitAll

```
T2
```

### Phase 3: Folha DELETE ACL

```
T3
```

### Phase 4: Gate conformidade

```
T4
```

---

## Task Breakdown

### T1: Short-circuit empty-set em `BeneficioMensalService` + testes

**What**: Em `listarPorCompetenciaParaUsuario` / `resumoPorCompetenciaParaUsuario`, após `acessoNegado`, retornar vazio quando `!acessoTotal` e centros null/empty — **antes** de `centrosParaFiltro` / query unscoped; adicionar testes MODACL-01–05.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/application/BeneficioMensalService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/beneficios/application/BeneficioMensalServiceTest.java`

**Depends on**: None  
**Reuses**: Design §1; `acessoNegado`; helpers de contexto no teste existente  
**Requirement**: MODACL-01, MODACL-02, MODACL-03, MODACL-04, MODACL-05

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Restrito + `centrosCustoIds` vazio → lista e resumo vazios; `verify(..., never())` em find/resumo **unscoped**
- [x] `acessoTotal=true` ainda usa query unscoped (casos existentes ou assert preservado)
- [x] Restrito + centros não vazios ainda filtra por `In`
- [x] Caso distinto de SEM_FUNCIONARIO já existente
- [x] Zero imports foreign infra novos
- [x] Gate: `cd backend && mvn test` (incl. `BeneficioMensalServiceTest`)
- [x] Test count: ≥2 novos testes (list + resumo restrito+empty) passam; sem silent deletions

**Tests**: unit  
**Gate**: quick  
**Commit**: `fix(beneficios): deny empty centro set without unscoped query`

---

### T2: `POST /auth/refresh` permitAll + teste de segurança

**What**: Adicionar matcher `HttpMethod.POST, "/auth/refresh"` com `permitAll()`; criar MockMvc security test anon ≠ 401; confirmar regressão ADMIN intacta.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/config/SecurityConfig.java`
- `backend/src/test/java/br/com/techne/sistemafolha/config/SecurityConfigAuthRefreshTest.java` (ou path equivalente sob `auth/api/`)

**Depends on**: None (após Phase 1 no plano)  
**Reuses**: `SecurityConfigTipoBeneficioTest`; Design §2; Context7 `permitAll`  
**Requirement**: MODACL-06, MODACL-07, MODACL-08 (MODACL-09 verificado em T4)

**Tools**:
- MCP: `user-context7` (opcional — Spring Security)
- Skill: `spring-security` (permitAll já autorizado no Design approve)

**Done when**:
- [x] Matcher presente ao lado de `/auth/login`, path **sem** `/api` duplicado
- [x] Anon `POST /auth/refresh` com body + `@MockBean AuthenticationService` → status **≠ 401** (preferir 200 com mock)
- [x] `SecurityConfigTipoBeneficioTest` continua verde (403 USER / 2xx ADMIN)
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥1 novo teste refresh; suite sem regressão security existente

**Tests**: unit (MockMvc)  
**Gate**: quick  
**Commit**: `fix(auth): permitAll POST /auth/refresh for SPA token renewal`

---

### T3: ACL no soft-delete de Folha (service + controller) + testes

**What**: Espelhar `removerSeAutorizado`: service recebe login, aplica `aplicarFiltroAcesso`; controller passa `Authentication`; deny → false/404, allow → softDelete/204.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaPagamentoService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/api/FolhaPagamentoController.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaPagamentoServiceTest.java`

**Depends on**: None (após Phase 2 no plano; independente de T1/T2 logicamente)  
**Reuses**: `BeneficioMensalService.removerSeAutorizado`; `BeneficioMensalController.remover`; Folha `aplicarFiltroAcesso`  
**Requirement**: MODACL-10, MODACL-11, MODACL-12, MODACL-13

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Controller `remover(Long id, Authentication authentication)` chama service com `authentication.getName()`
- [x] Deny (centro fora) → não chama `softDelete`; retorna false → 404
- [x] Allow / `acessoTotal` → soft-delete e true → 204
- [x] Ports existentes preservados (sem foreign infra)
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥2 testes deny/allow passam

**Tests**: unit  
**Gate**: quick  
**Commit**: `fix(folha): enforce ACL on soft-delete like beneficios`

---

### T4: Gate final + handoff + contrato FE

**What**: Rodar suite + ArchUnit; confirmar `api.ts` refresh sem Bearer inalterado (MODACL-09); atualizar Handoff STATE; marcar tasks done.  
**Where**: `_docs/specs/STATE.md` (Handoff only); verificação estática `frontend/src/services/api.ts`  
**Depends on**: T1, T2, T3  
**Reuses**: Design Success Criteria  
**Requirement**: MODACL-09; fecho MODACL-01–13

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `cd backend && mvn test` exit 0 (incl. ArchUnit)
- [x] `cd backend && mvn test -Dtest=ModularArchitectureTest` exit 0
- [x] `cd frontend && npm run build` exit 0 (regressão zero)
- [x] Grep/read: `api.ts` refresh `fetch` headers **sem** `Authorization` (MODACL-09)
- [x] Handoff: Execute T1–T4 done; ready for Verifier `modular-acl-security-fix`
- [x] Tasks.md checkboxes T1–T4 `[x]`

**Tests**: none  
**Gate**: full  
**Commit**: `chore: record modular-acl-security-fix compliance gate`

---

> **Nota:** `_docs/specs/features/modular-acl-security-fix/validation.md` é produzido pelo **Verifier** TLC após o último task — não é task do autor.

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1
Phase 2:  T2
Phase 3:  T3
Phase 4:  T4
```

**Batch packing (Execute):** 4 tasks → **1 batch** (≤ ~8) → Execute **inline** (sem offer de sub-agentes obrigatório).  
Opcional: usuário pode pedir max subagents mesmo assim.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1 | 1 service + tests (cohesive ACL fix) | ✅ Granular |
| T2 | SecurityConfig + 1 security test class | ✅ Granular |
| T3 | Service + thin controller + tests (espelho benefícios) | ✅ OK cohesive (2 files, one ACL surface) |
| T4 | Gate + handoff + static FE check | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ----------------- | ------------- | ------ |
| T1 | None | Phase1 start | ✅ |
| T2 | None (seq após P1) | Phase2 | ✅ |
| T3 | None (seq após P2) | Phase3 | ✅ |
| T4 | T1, T2, T3 | Phase4 after all | ✅ |

Nota: T1–T3 são independentes logicamente; plano sequencial reduz risco de merge conflicts no mesmo working tree. T4 depende dos três.

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | -------------- | --------- | ------ |
| T1 | Application ACL benefício | unit | unit | ✅ |
| T2 | SecurityConfig / MockMvc refresh | unit (MockMvc) | unit (MockMvc) | ✅ |
| T3 | Application ACL Folha (+ thin controller) | unit | unit | ✅ |
| T4 | Docs/gate / FE static | none | none | ✅ |

---

## Requirement Traceability (tasks)

| Requirement ID | Tasks |
| -------------- | ----- |
| MODACL-01 | T1 |
| MODACL-02 | T1 |
| MODACL-03 | T1 |
| MODACL-04 | T1 |
| MODACL-05 | T1 |
| MODACL-06 | T2 |
| MODACL-07 | T2 |
| MODACL-08 | T2 |
| MODACL-09 | T4 |
| MODACL-10 | T3 |
| MODACL-11 | T3 |
| MODACL-12 | T3 |
| MODACL-13 | T3 |

**Coverage:** 13 IDs mapped; 0 unmapped.

---

## Tools question (before Execute)

Para cada task, quais ferramentas usar?

**MCPs disponíveis:** `user-context7`, `plugin-linear-linear`  
**Skills relevantes:** `spring-security` (T2), `tlc-spec-driven`, `jpa-performance` (N/A salvo tocar queries)

Defaults já preenchidos por task. Confirme se quer alterar algum antes do Execute.
