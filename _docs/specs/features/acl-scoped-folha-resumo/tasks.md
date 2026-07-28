# ACL — Resumo Folha scoped + Dashboard evolução Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/acl-scoped-folha-resumo/design.md`  
**Status**: Done — Execute T1–T6 complete 2026-07-27 (uncommitted; ready for Independent Verifier)  
**Approach**: ResumoService ACL + FolhaConsultaPort + FolhaLinhaAgregacao; Dashboard evolução scoped; extend FolhaEvolucaoSnapshot.competenciaFim; FE/Benefícios sem change  
**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` + AD-004, `acl-scoped-folha-resumo/spec.md` (RSF ACs), AD-007/AD-008/AD-010/AD-011, design.md.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Port record + adapter mapper (`FolhaEvolucaoSnapshot`) | unit (Mockito) se adapter testável; senão compile + callers | Mapper preenche `competenciaFim`; callers/tests atualizados | `folha/port/FolhaEvolucaoSnapshot.java`, `folha/application/FolhaConsultaAdapter.java`, `**/DashboardServiceTest.java` | `cd backend && mvn test -Dtest=DashboardServiceTest,FolhaConsultaAdapterTest` (AdapterTest se existir) |
| Helper agregação (`FolhaLinhaAgregacao`) | unit | Distinct empregados; Σ PROVENTO/DESCONTO; líquido; lista vazia → zeros | `folha/application/FolhaLinhaAgregacao*.java` + test co-located | `cd backend && mvn test -Dtest=FolhaLinhaAgregacaoTest` |
| Application Resumo ACL | unit (Mockito) | RSF-01…05: scoped ≠ snapshot; total Encargos 0; acessoTotal = snapshot; deny []; sem linhas → zeros + metadados | `**/folha/application/ResumoFolhaPagamentoServiceTest.java` | `cd backend && mvn test -Dtest=ResumoFolhaPagamentoServiceTest` |
| API Resumo controller | none | Auth wiring; review + compile | `folha/api/ResumoFolhaPagamentoController.java` | compile via `mvn test` / package |
| Application Dashboard evolução | unit (Mockito) | RSF-06: scoped evolução não vazia e ≠ globais; RSF-07: acessoTotal intacto; RSF-08 deny intacto | `**/dashboard/application/DashboardServiceTest.java` | `cd backend && mvn test -Dtest=DashboardServiceTest` |
| Benefícios | none (D1) | Regressão no Full gate | `BeneficioMensalServiceTest` | `cd backend && mvn test -Dtest=BeneficioMensalServiceTest` (via Full) |
| FE | none | AD-004; sem change esperado | — | `cd frontend && npm run build` (Full) |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após T1–T5 unit | `cd backend && mvn test -Dtest=FolhaLinhaAgregacaoTest,ResumoFolhaPagamentoServiceTest,DashboardServiceTest` |
| Full | Fechamento T6 | `cd backend && mvn test && cd ../frontend && npm run build` |
| Build | Opcional | `cd backend && mvn clean package -DskipTests` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Port + agregação

```
T1 → T2
```

### Phase 2: Resumo ACL

```
T3 → T4
```

### Phase 3: Dashboard evolução

```
T5
```

### Phase 4: Gate + handoff

```
T6
```

**Batch packing:** 6 tasks → **1 batch** (≤ ~8) → Execute **inline** (sem sub-agents), salvo pedido do usuário por workers.

---

## Task Breakdown

### T1: Estender `FolhaEvolucaoSnapshot` com `competenciaFim`

**What**: Adicionar `LocalDate competenciaFim` ao record; atualizar `FolhaConsultaAdapter.toEvolucaoSnapshot` a partir da entity; corrigir todos os call sites / testes que constroem o record.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/port/FolhaEvolucaoSnapshot.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaConsultaAdapter.java`
- Call sites de teste (ex. `DashboardServiceTest`)

**Depends on**: None  
**Reuses**: Entity `ResumoFolhaPagamento.getCompetenciaFim()`  
**Requirement**: RSF-06 (pré-requisito de janela correta)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Record inclui `competenciaFim`
- [x] Adapter mapeia fim da entity
- [x] Compilação + testes que usam o record verdes
- [x] Gate: `cd backend && mvn test -Dtest=DashboardServiceTest` (e AdapterTest se existir)

**Tests**: unit (atualizar fixtures existentes)  
**Gate**: quick  
**Commit**: `refactor(folha): add competenciaFim to FolhaEvolucaoSnapshot`

---

### T2: Helper `FolhaLinhaAgregacao`

**What**: Criar util package-private (ou package-visible) que, dado `List<FolhaLinhaSnapshot>`, retorna empregados distinct, Σ PROVENTO, Σ DESCONTO, líquido (= pagamentos − descontos).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaLinhaAgregacao.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaLinhaAgregacaoTest.java`

**Depends on**: T1 (opcional ordering; tecnicamente independente — **Depends on: None** se T1 não necessário; keep **T1** only for phase order — actually T2 does not need T1. Set Depends on: None for true independence, but phase says T1→T2 for sequential. Design: T2 depends on None; phase order still T1 then T2.)

**Depends on**: None  
**Reuses**: Critério string `"PROVENTO"` / `"DESCONTO"` do Dashboard  
**Requirement**: RSF-01, RSF-05 (foundation)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Helper calcula os 4 totais
- [x] Lista vazia → todos zero
- [x] Testes unitários cobrem happy + empty + mix de tipos
- [x] Gate: `cd backend && mvn test -Dtest=FolhaLinhaAgregacaoTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): add FolhaLinhaAgregacao for scoped totais`

---

### T3: `ResumoFolhaPagamentoService` com ACL + testes RSF-01…05

**What**: Injetar `OrganogramaAcessoPort`, `UsuarioLookupPort`, `FolhaConsultaPort`; métodos `*(String login, …)`; deny → vazio; `acessoTotal` → snapshot; scoped → `toDtoScoped` (encargos 0, zeros se sem linhas, metadados preservados). Atualizar `ResumoFolhaPagamentoServiceTest`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/ResumoFolhaPagamentoService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/ResumoFolhaPagamentoServiceTest.java`

**Depends on**: T2  
**Reuses**: `FolhaLinhaAgregacao`; padrão deny Benefícios/Dashboard; `findLinhasAtivasPorCompetencia`  
**Requirement**: RSF-01, RSF-02, RSF-03, RSF-04, RSF-05

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Assinaturas com login
- [x] RSF-01…05 cobertos com `file:line` assertions (totais scoped ≠ snapshot global; encargos 0; deny []; zeros + id; total snapshot se acessoTotal)
- [x] Gate: `cd backend && mvn test -Dtest=ResumoFolhaPagamentoServiceTest,FolhaLinhaAgregacaoTest`
- [x] Test count: ≥4 novos cenários ACL (+ regressões mapeamento)

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): scope resumo folha totais by organograma ACL`

---

### T4: Controller resumo passa `Authentication`

**What**: Em todos os endpoints de `ResumoFolhaPagamentoController`, receber `Authentication` e chamar service com `authentication.getName()`.  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/folha/api/ResumoFolhaPagamentoController.java`

**Depends on**: T3  
**Reuses**: `FolhaPagamentoController` / `BeneficioMensalController`  
**Requirement**: RSF-01…04 (wiring)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Todos os 4 endpoints passam login
- [x] Compila com T3
- [x] Gate: `cd backend && mvn test -Dtest=ResumoFolhaPagamentoServiceTest`

**Tests**: none (matrix: controller none)  
**Gate**: quick  
**Commit**: `feat(folha): pass authenticated user to resumo endpoints`

---

### T5: Dashboard evolução mensal scoped

**What**: Substituir `evolucaoMensal = List.of()` no ramo parcial por `calcularEvolucaoMensalScoped(centros)`; usar competências de `findEvolucaoUltimos12Meses` + linhas `findLinhasAtivasPorCompetencia(inicio, fim, centros)` + helpers provento/desconto; acessoTotal inalterado. Atualizar `DashboardServiceTest` (hoje espera evolução vazia no scoped — inverter para RSF-06).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/dashboard/application/DashboardService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/dashboard/application/DashboardServiceTest.java`

**Depends on**: T1  
**Reuses**: `calcularTotalProventos/Descontos`; port Folha  
**Requirement**: RSF-06, RSF-07, RSF-08

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Scoped: evolução não vazia quando há competências + linhas no escopo; valores ≠ totais globais do snapshot
- [x] acessoTotal: comportamento atual preservado
- [x] Deny/emptyStats: evolução vazia (sem regressão)
- [x] Gate: `cd backend && mvn test -Dtest=DashboardServiceTest`
- [x] Nota: teste que assertava `evolucaoMensal().isEmpty()` no scoped **atualizado** (não deleted silently)

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(dashboard): compute scoped evolucaoMensal from folha lines`

---

### T6: Gate Full + handoff + regressão benefícios

**What**: Rodar Full gate; confirmar nenhum diff de produção em benefícios; marcar tasks Done; atualizar `STATE.md` Handoff (Execute done → Verifier).  
**Where**: `_docs/specs/STATE.md`, `_docs/specs/features/acl-scoped-folha-resumo/tasks.md`

**Depends on**: T4, T5  
**Reuses**: Gate Full commands  
**Requirement**: RSF-09, success criteria gate

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `cd backend && mvn test` exit 0
- [x] `cd frontend && npm run build` exit 0
- [x] `BeneficioMensalServiceTest` (via suite) verde sem change obrigatória
- [x] Handoff: ready for Independent Verifier
- [x] Tasks status → Done

**Tests**: none (orquestra gates)  
**Gate**: full  
**Commit**: `docs(acl): handoff after scoped resumo execute` (opcional / se user pedir commit)

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 ──→ T2
Phase 2:  T3 ──→ T4
Phase 3:  T5
Phase 4:  T6
```

Nota: T5 depende de T1 (não de T4). T6 depende de T4 e T5. Ordem de execução recomendada: **T1 → T2 → T3 → T4 → T5 → T6** (T5 pode rodar após T1; na prática após T4 para um único worker linear).

Execution is strictly sequential. **1 batch (6 tasks)** → inline Execute.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: FolhaEvolucaoSnapshot + adapter | 1 record + mapper | ✅ Granular |
| T2: FolhaLinhaAgregacao + tests | 1 helper | ✅ Granular |
| T3: ResumoService + tests | 1 service + co-located tests | ✅ Granular |
| T4: Controller auth wiring | 1 controller | ✅ Granular |
| T5: Dashboard evolução + tests | 1 service method + tests | ✅ Granular |
| T6: Gate + handoff | orchestration | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | (start) | ✅ Match |
| T2 | None | após T1 na fase (ordem, sem dep hard) | ✅ Match (fase ordena; dep body None OK) |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T1 | T1 → T5 (paralelo lógico à fase 2) | ✅ Match |
| T6 | T4, T5 | T4/T5 → T6 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Port + adapter + test fixtures | unit / update callers | unit | ✅ OK |
| T2 | Helper agregação | unit | unit | ✅ OK |
| T3 | Resumo application | unit | unit | ✅ OK |
| T4 | Controller | none | none | ✅ OK |
| T5 | Dashboard application | unit | unit | ✅ OK |
| T6 | Docs / gate | none | none | ✅ OK |

---

## Requirement Traceability (tasks)

| Requirement ID | Task(s) |
| -------------- | ------- |
| RSF-01 | T3, T4 |
| RSF-02 | T3 |
| RSF-03 | T3 |
| RSF-04 | T3 |
| RSF-05 | T2, T3 |
| RSF-06 | T1, T5 |
| RSF-07 | T5 |
| RSF-08 | T5 |
| RSF-09 | T6 |

---

## Tools question (before Execute)

Para cada task, quais ferramentas usar?

**MCPs disponíveis:** Context7, SonarQube, Linear, Docker MCP (não necessários por default).  
**Skills relevantes:** `tlc-spec-driven` (obrigatória no Execute); `jpa-performance` (consulta se otimizar queries N×competência — design aceita N queries); `spring-security` (só se tocar SecurityConfig — design diz **não**).

Default proposto: **MCP NONE** em todas; Skill NONE além do protocolo TLC.

Confirme tasks + matrix + tools (+ se Design fica **Approved**) para liberar Execute.
