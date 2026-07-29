# folha-custo-clt-fix2 — Custo Techne por % de rubrica Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/folha-custo-clt-fix2/design.md`  
**Status**: Execute complete — Verifier PASS @ a31bc15 (fix cycle 1)  
**Approach**: A — Motor único + snapshot `%` + remover rateio ADP (AD-012)  
**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md`, AD-004 (FE skills = target), AD-007/008/010/011/012, `folha-custo-clt-fix2/spec.md` (FIX2 ACs + edge cases).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Motor / composer (pure application) | unit | All branches; 1:1 FIX2-01…03; `%` null→100; `%`=0; bruto/líquido sem `%` (FIX2-02,15); HALF_UP | `backend/src/test/java/**/folha/application/FolhaMotorCalculoTest.java`, `FolhaCustoEmpresaComposerTest.java` | `cd backend && mvn test -Dtest=FolhaMotorCalculoTest,FolhaCustoEmpresaComposerTest` |
| Processamento (snapshot `%`) | unit (Mockito) | FIX2-04,05,20; snapshot ADP/CUSTO_FIXO/CALCULADO; `custo_folha` persistido; reprocesso após `%` cadastro (FIX2-18 edge) | `**/folha/application/FolhaProcessamentoServiceTest.java` | `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest` |
| Consulta port / snapshot | unit (Mockito) | `FolhaLinhaSnapshot.porcentagem` ficha vs fallback ADP; dual source inalterado | `**/FolhaConsultaAdapterTest.java` | `cd backend && mvn test -Dtest=FolhaConsultaAdapterTest` |
| Totalização / agregação / rateio removal | unit (Mockito) | FIX2-06,07,10; encargosRateados=0; discrimination sensor (mock verify rateio não invocado); agregação sem rateio | `**/FolhaTotalizacaoServiceTest.java`, `FolhaLinhaAgregacaoTest.java` | `cd backend && mvn test -Dtest=FolhaTotalizacaoServiceTest,FolhaLinhaAgregacaoTest` |
| Ficha detalhe consulta | unit (Mockito) | FIX2-11,12,13,21; COMPANY_COST com `%`; GROSS/NET sem `%`; sem linha encargos | `**/FolhaFichaConsultaServiceTest.java` | `cd backend && mvn test -Dtest=FolhaFichaConsultaServiceTest` |
| Resumo ACL paridade | unit (Mockito) | FIX2-08,14,16,22,23,24; scoped **e** global `acessoTotal`; Σ cards = resumo | `**/ResumoFolhaPagamentoServiceTest.java`, `FolhaAclParidadeResumoCardsTest.java` | `cd backend && mvn test -Dtest=ResumoFolhaPagamentoServiceTest,FolhaAclParidadeResumoCardsTest` |
| Dashboard KPI custo | unit (Mockito) | FIX2-09; composição sem rateio via port; paridade com totalização | `**/dashboard/application/DashboardServiceTest.java` | `cd backend && mvn test -Dtest=DashboardServiceTest` |
| FolhaTotalizacaoPort | unit (Mockito) | Port delega ao service; ArchUnit AD-010 dashboard só via port | `**/FolhaTotalizacaoPort*` ou adapter test | compile + DashboardServiceTest |
| Flyway migrations / entities | none | Idempotente; compile gate | `db/migration/V1.22__*.sql`, `V1.23__*.sql` | `cd backend && mvn clean compile` |
| Frontend card (P2) | none (AD-004 gap) | Build + lint; manual FIX2-19 subtexto | `frontend/src/pages/FolhaPagamento/` | `cd frontend && npm run lint && npm run build` |
| ArchUnit modular | unit | AD-010 — dashboard sem infra folha estrangeira | `**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após task com unit tests backend | `cd backend && mvn test -Dtest=<ClassTest>` (classe(s) da task) |
| Full | Após T15 (fechamento feature + Verifier) | `cd backend && mvn test && cd ../frontend && npm run lint && npm run build` |
| Build | Migrations / entities-only (T1, T12) | `cd backend && mvn clean compile` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Schema + motor + processamento (4 tasks)

```
T1 → T2 → T3 → T4
```

### Phase 2: Remover rateio da composição (2 tasks)

```
T5 → T6
```

### Phase 3: Detalhe ficha + API contribuição (1 task)

```
T7
```

### Phase 4: Resumo paridade Σ cards (2 tasks)

```
T8 → T9
```

### Phase 5: Dashboard via port (2 tasks)

```
T10 → T11
```

### Phase 6: P2 dados + FE + fechamento (4 tasks)

```
T12 → T13 → T14 → T15
```

**Batch packing (~7 tasks/worker, whole phases):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | Phase 1 + Phase 2 | T1–T6 | 6 |
| 2 | Phase 3 + Phase 4 | T7–T9 | 3 |
| 3 | Phase 5 + Phase 6 | T10–T15 | 6 |

→ **3 workers** sequenciais (offer-then-confirm no Execute). Feature cabe em 3 batches ≤7; inline no agente principal se usuário recusar sub-agents.

---

## Task Breakdown

### T1: Flyway V1.22 — snapshot `porcentagem` em `ficha_linha`

**What**: Migração `porcentagem NUMERIC(7,4)` nullable; sincronizar entity `FichaLinha`.  
**Where**:
- `backend/src/main/resources/db/migration/V1.22__ficha_linha_porcentagem.sql`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/domain/FichaLinha.java`

**Depends on**: None  
**Reuses**: Padrão V1.18 `ficha_linha`; `Rubrica.porcentagem` como origem semântica  
**Requirement**: FIX2-04 (schema)

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`, `jpa-performance`

**Done when**:
- [x] Coluna `porcentagem` nullable com COMMENT documentado
- [x] Entity JPA mapeia `precision=7, scale=4`
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(folha): add ficha_linha porcentagem snapshot V1.22`

---

### T2: Motor — custo com `%`; bruto/líquido inalterados

**What**: Estender `FolhaMotorCalculo.LinhaCalculoInput` com `porcentagem`; aplicar `%` só em `custoFolha` e `contribuicao(COMPANY_COST)`; helper `porcentagemEfetiva` (null→100).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaMotorCalculo.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaMotorCalculoTest.java`

**Depends on**: None (motor puro; T1 paralelo conceitual — T3 depende de T2)  
**Reuses**: HALF_UP scale 2 existente  
**Requirement**: FIX2-01, FIX2-02, FIX2-03; edge `%`=0, `%` null

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Caso âncora: 7258.43 × 1 × 138.63% = **10062.36**
- [x] Bruto/líquido ignoram `%` com `%`≠100 no input
- [x] `%`=0 → custo contribuição 0; bruto/líquido inalterados
- [x] Gate: `cd backend && mvn test -Dtest=FolhaMotorCalculoTest`
- [x] Test count: todos os testes da classe passam (sem deleções silenciosas)

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): apply rubrica porcentagem on company cost only`

---

### T3: Processamento — snapshot `%` e `custo_folha` persistido

**What**: Copiar `rubrica.getPorcentagem()` nos três `montarLinha*`; `toInput(FichaLinha)` inclui `%`; persistir `custo_folha` via motor T2.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaProcessamentoService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaProcessamentoServiceTest.java`

**Depends on**: T1, T2  
**Reuses**: Padrão snapshot operadores FCLT-07  
**Requirement**: FIX2-04, FIX2-05, FIX2-20; FIX2-18 (reprocesso reflete `%` cadastro)

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Linhas ADP, CUSTO_FIXO e CALCULADO persistem `porcentagem` snapshot
- [x] Fixa R$ 688, `%`=100 → `custo_folha` inclui 688; bruto usa valor original
- [x] Gate: `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): snapshot porcentagem on ficha_linha processing`

---

### T4: Port consulta — `FolhaLinhaSnapshot.porcentagem`

**What**: Adicionar campo `porcentagem` ao record; mapear em `FolhaConsultaAdapter` (ficha snapshot + fallback rubrica live); atualizar helpers de teste que instanciam `FolhaLinhaSnapshot`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/port/FolhaLinhaSnapshot.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaConsultaAdapter.java`
- Test helpers: `FolhaConsultaAdapterTest`, `FolhaLinhaAgregacaoTest`, `FolhaTotalizacaoServiceTest`, `FolhaAclParidadeResumoCardsTest`, `ResumoFolhaPagamentoServiceTest`, `FolhaPagamentoServiceTest`, `DashboardServiceTest`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaConsultaAdapterTest.java`

**Depends on**: T1  
**Reuses**: Dual source ficha/ADP existente  
**Requirement**: FIX2-04 (leitura); suporte FIX2-01 em callers downstream

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Snapshot ficha expõe `porcentagem` da linha; fallback ADP expõe `%` live da rubrica
- [x] Projeto compila; testes adapter passam
- [x] Gate: `cd backend && mvn test -Dtest=FolhaConsultaAdapterTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): propagate porcentagem in FolhaLinhaSnapshot`

---

### T5: Totalização — remover rateio de `custoEmpresa`

**What**: `FolhaTotalizacaoService` deixa de chamar `EncargosRateioService` para composição; `encargosRateados` DTO sempre `0.00`; `FolhaCustoEmpresaComposer.compor(folha, ZERO, benefícios)`; javadoc `@Deprecated` em uso composição de `EncargosRateioService`; teste discrimination FIX2-10.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaTotalizacaoService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/EncargosRateioService.java` (javadoc)
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaTotalizacaoServiceTest.java`

**Depends on**: T2, T4  
**Reuses**: `FolhaCustoEmpresaComposer`  
**Requirement**: FIX2-06, FIX2-07, FIX2-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `custoEmpresa` = custoFolha + custoBeneficios (sem encargos)
- [x] Teste verify/mock: `EncargosRateioService.ratearPorFuncionario` **não** invocado na composição
- [x] Gate: `cd backend && mvn test -Dtest=FolhaTotalizacaoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `fix(folha): remove encargos rateados from custoEmpresa composition`

---

### T6: Agregação resumo — encargos zerados na composição

**What**: `FolhaLinhaAgregacao.agregar` compõe `custoEmpresa` com encargos=0; callers passam map zerado ou agregador ignora encargos na soma final; atualizar `toInput` com `%`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaLinhaAgregacao.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaLinhaAgregacaoTest.java`

**Depends on**: T2, T4, T5  
**Reuses**: Motor + composer  
**Requirement**: FIX2-06, FIX2-08 (partial — agregador)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Agregação multi-funcionário não soma encargos em `totalCustoEmpresa`
- [x] Custo folha usa `%` quando presente no snapshot input
- [x] Gate: `cd backend && mvn test -Dtest=FolhaLinhaAgregacaoTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `fix(folha): FolhaLinhaAgregacao custo without encargos rateados`

---

### T7: Detalhe ficha — contribuição custo com `%`

**What**: `FolhaFichaConsultaService` passa `%` ao motor; aba Custo usa fórmula FIX2-01; abas Bruto/Líquido mantêm valor original sem `%`; confirmar ausência de linha encargos rateados.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaFichaConsultaService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaFichaConsultaServiceTest.java`

**Depends on**: T2, T3  
**Reuses**: `FichaLinhaDetalheDTO`; motor `contribuicao`  
**Requirement**: FIX2-11, FIX2-12, FIX2-13, FIX2-21

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] COMPANY_COST: contribuição = valor × op_custo × (%/100)
- [x] GROSS/NET: valor coluna = original; contribuição sem `%`
- [x] Nenhuma linha sintética de encargos rateados na aba Custo
- [x] Gate: `cd backend && mvn test -Dtest=FolhaFichaConsultaServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): ficha detail company cost with porcentagem`

---

### T8: Resumo — unificar global/scoped sem rateio

**What**: `ResumoFolhaPagamentoService`: remover rateio em `toDtoGlobalFromFicha` / `toDtoFromLinhas`; global com fichas delega a agregação por linhas + encargos=0; eliminar fallback `totalPagamentos`→`totalBruto` quando linhas operador-based existem; remover injeção/uso de `EncargosRateioService` na composição de custo.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/ResumoFolhaPagamentoService.java`

**Depends on**: T5, T6  
**Reuses**: `FolhaLinhaAgregacao`; `FolhaConsultaPort`  
**Requirement**: FIX2-08, FIX2-16, FIX2-24

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Global `acessoTotal` não chama `encargosRateioService.ratearPorFuncionario` para custo
- [x] Com linhas/ficha: `totalBruto` vem de agregação operador-based, não `totalPagamentos` ADP
- [x] Gate: `cd backend && mvn test -Dtest=ResumoFolhaPagamentoServiceTest`

**Tests**: unit (atualizar/estender `ResumoFolhaPagamentoServiceTest` na mesma task)  
**Gate**: quick  
**Commit**: `fix(folha): resumo totals without encargos rateados`

---

### T9: ACL paridade resumo ↔ cards (scoped + global)

**What**: Estender `FolhaAclParidadeResumoCardsTest` com cenário `acessoTotal`; garantir Σ `salBruto`/`salLiquido`/`custoEmpresa` cards = resumo; bruto/líquido sem `%` regressão.  
**Where**:
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaAclParidadeResumoCardsTest.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/ResumoFolhaPagamentoServiceTest.java` (ajustes se necessário)

**Depends on**: T8  
**Reuses**: Padrão FCLT-ACL-11 existente  
**Requirement**: FIX2-14, FIX2-15, FIX2-22, FIX2-23

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Teste scoped PASS (regressão)
- [x] Teste novo global `acessoTotal`: totalBruto/totalLiquido/totalCustoEmpresa = Σ cards
- [x] Gate: `cd backend && mvn test -Dtest=FolhaAclParidadeResumoCardsTest,ResumoFolhaPagamentoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `test(folha): ACL paridade resumo cards scoped and global`

---

### T10: `FolhaTotalizacaoPort` — contrato cross-domain

**What**: Criar `FolhaTotalizacaoPort` com `calcularTotalCustoEmpresa(...)`; adapter/implementação delegando a `FolhaTotalizacaoService`; registrar bean Spring.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/port/FolhaTotalizacaoPort.java`
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaTotalizacaoAdapter.java` (ou service implements port)

**Depends on**: T5  
**Reuses**: AD-010 port pattern (`FolhaConsultaPort`)  
**Requirement**: FIX2-09 (contrato)

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Port expõe agregação custo empresa sem rateio
- [x] Implementação reutiliza motor/composer de T5
- [x] Gate: `cd backend && mvn clean compile && mvn test -Dtest=ModularArchitectureTest`

**Tests**: none (validado via T11 + ArchUnit)  
**Gate**: build + ArchUnit  
**Commit**: `feat(folha): add FolhaTotalizacaoPort for dashboard KPI`

---

### T11: Dashboard — KPI via port sem rateio

**What**: `DashboardService` injeta `FolhaTotalizacaoPort`; substituir `calcularCustoEmpresa` inline + `ratearEncargos` local; atualizar testes.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/dashboard/application/DashboardService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/dashboard/application/DashboardServiceTest.java`

**Depends on**: T10  
**Reuses**: `FolhaConsultaPort` para linhas  
**Requirement**: FIX2-09

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] `custoMensalFolha` = composição FIX2-06 (sem rateio)
- [x] Método local `ratearEncargos` removido ou morto
- [x] Gate: `cd backend && mvn test -Dtest=DashboardServiceTest,ModularArchitectureTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `fix(dashboard): custoMensalFolha via FolhaTotalizacaoPort without rateio`

---

### T12: Flyway V1.23 — seed `%` legado Salário Base

**What**: Migração documentada `0010` → `138.63`; comentário operacional no SQL.  
**Where**:
- `backend/src/main/resources/db/migration/V1.23__seed_rubrica_porcentagem_legado.sql`

**Depends on**: T1  
**Reuses**: `rubricas.porcentagem` existente  
**Requirement**: FIX2-17

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`

**Done when**:
- [x] UPDATE idempotente com comentário mapeamento mínimo
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: build  
**Commit**: `chore(folha): seed rubrica 0010 porcentagem legado V1.23`

---

### T13: Frontend — subtexto custo folha vs benefícios (P2)

**What**: Card funcionário exibe `salCustoFolha` + `salCustoBeneficios` como subtexto; ocultar `encargosRateados`.  
**Where**:
- `frontend/src/pages/FolhaPagamento/index.tsx`
- `frontend/src/services/folhaPagamentoService.ts` (se tipagem/display)

**Depends on**: T5  
**Reuses**: Campos DTO já expostos pela API  
**Requirement**: FIX2-19

**Tools**:
- MCP: NONE
- Skill: `component-architecture` (target AD-004)

**Done when**:
- [x] Subtexto visível quando dados presentes; total = folha + benefícios
- [x] `encargosRateados` não renderizado
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(frontend): show custo folha vs beneficios on employee card`

---

### T14: Docs — supersede composição no spec pai

**What**: Nota em `folha-custo-clt/spec.md` indicando D4-CLT/FCLT-06 superseded por fix2 + AD-012; sem reabrir feature pai.  
**Where**:
- `_docs/specs/features/folha-custo-clt/spec.md`

**Depends on**: T5  
**Reuses**: Cross-ref da spec fix2  
**Requirement**: traceability note (spec §Cross-ref)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Seção superseded documentada com link para fix2
- [x] Nenhuma alteração de código

**Tests**: none  
**Gate**: n/a  
**Commit**: `docs(spec): folha-custo-clt composicao superseded by fix2`

---

### T15: Gate final feature

**What**: Executar suite completa backend + lint/build frontend; preparar handoff para Verifier automático.  
**Where**: n/a (gate only)

**Depends on**: T1–T14  
**Reuses**: Gate Check Commands Full  
**Requirement**: todos FIX2 P1 + P2

**Tools**:
- MCP: `user-sonarqube` (opcional pós-gate)
- Skill: NONE

**Done when**:
- [x] Gate: `cd backend && mvn test`
- [x] Gate: `cd frontend && npm run lint && npm run build`
- [x] `tasks.md` status → Execute complete (pré-Verifier)
- [x] Handoff `STATE.md` atualizado

**Tests**: full suite  
**Gate**: full  
**Commit**: n/a (gate task — ou commit docs STATE se alterado)

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6

Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4
Phase 2:  T5 ──→ T6
Phase 3:  T7
Phase 4:  T8 ──→ T9
Phase 5:  T10 ──→ T11
Phase 6:  T12 ──→ T13 ──→ T14 ──→ T15
```

Execution is strictly sequential — there is no intra-phase parallelism.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Flyway + entity | 1 migration + 1 entity | ✅ Granular |
| T2: Motor + tests | 1 class + 1 test class | ✅ Granular |
| T3: Processamento snapshot | 1 service + tests | ✅ Granular |
| T4: Snapshot port field | 1 record + adapter + test fixes | ✅ Granular (cohesive) |
| T5: Totalização rateio removal | 1 service + tests | ✅ Granular |
| T6: Agregação | 1 class + tests | ✅ Granular |
| T7: Ficha consulta | 1 service + tests | ✅ Granular |
| T8: Resumo service | 1 service + tests | ✅ Granular |
| T9: ACL paridade tests | test-only extension | ✅ Granular |
| T10: Port interface | 1 port + adapter | ✅ Granular |
| T11: Dashboard | 1 service + tests | ✅ Granular |
| T12: Seed migration | 1 migration | ✅ Granular |
| T13: FE card | 1 page (+ service types) | ✅ Granular |
| T14: Parent spec note | 1 doc file | ✅ Granular |
| T15: Full gate | verification only | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | T1 (start) | ✅ Match |
| T2 | None | T2 after T1 chain* | ⚠️ T2 parallel to T1 in body; diagram orders T1→T2 — OK (T2 não exige T1) |
| T3 | T1, T2 | T1→T2→T3 | ✅ Match |
| T4 | T1 | T1→T4 | ✅ Match |
| T5 | T2, T4 | T4→T5 | ✅ Match |
| T6 | T2, T4, T5 | T5→T6 | ✅ Match |
| T7 | T2, T3 | T3→T7 | ✅ Match |
| T8 | T5, T6 | T6→T8 | ✅ Match |
| T9 | T8 | T8→T9 | ✅ Match |
| T10 | T5 | T5→T10 | ✅ Match |
| T11 | T10 | T10→T11 | ✅ Match |
| T12 | T1 | T1→T12 (Phase 6) | ✅ Match |
| T13 | T5 | T5→T13 | ✅ Match |
| T14 | T5 | T5→T14 | ✅ Match |
| T15 | T1–T14 | T12→T13→T14→T15 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Flyway / entity | none | none | ✅ OK |
| T2 | Motor | unit | unit | ✅ OK |
| T3 | Processamento service | unit | unit | ✅ OK |
| T4 | Consulta port / adapter | unit | unit | ✅ OK |
| T5 | Totalização service | unit | unit | ✅ OK |
| T6 | Agregação | unit | unit | ✅ OK |
| T7 | Ficha consulta service | unit | unit | ✅ OK |
| T8 | Resumo service | unit | unit | ✅ OK |
| T9 | ACL tests only | unit | unit | ✅ OK |
| T10 | Port / adapter | none (+ ArchUnit) | none | ✅ OK |
| T11 | Dashboard service | unit | unit | ✅ OK |
| T12 | Flyway seed | none | none | ✅ OK |
| T13 | Frontend page | none | none | ✅ OK |
| T14 | Docs | none | none | ✅ OK |
| T15 | Gate | full suite | full | ✅ OK |

---

## Requirement Traceability (Tasks → FIX2)

| Task | Requirements |
| ---- | ------------ |
| T1 | FIX2-04 |
| T2 | FIX2-01, FIX2-02, FIX2-03 |
| T3 | FIX2-04, FIX2-05, FIX2-20, FIX2-18 |
| T4 | FIX2-04 (read path) |
| T5 | FIX2-06, FIX2-07, FIX2-10 |
| T6 | FIX2-06, FIX2-08 |
| T7 | FIX2-11, FIX2-12, FIX2-13, FIX2-21 |
| T8 | FIX2-08, FIX2-16, FIX2-24 |
| T9 | FIX2-14, FIX2-15, FIX2-22, FIX2-23 |
| T10 | FIX2-09 (contract) |
| T11 | FIX2-09 |
| T12 | FIX2-17 |
| T13 | FIX2-19 |
| T14 | cross-ref spec pai |
| T15 | all (gate) |

**Coverage:** 24 FIX2 requirements → 15 tasks (P1 complete T1–T11; P2 T12–T13; docs T14; gate T15)

---

## MCPs & Skills (confirm before Execute)

| Task | Recommended MCP | Recommended Skill |
| ---- | ----------------- | ----------------- |
| T1, T12 | NONE | `flyway-migration-writer`, `jpa-performance` |
| T2–T11 | NONE | `jpa-performance` onde JPA/queries |
| T10 | NONE | `modular-design-principles` |
| T13 | NONE | `component-architecture` (target) |
| T15 | `user-sonarqube` (opcional) | NONE |
| Verifier (auto pós-T15) | NONE | `tlc-spec-driven` validate flow |

Confirme antes do Execute se deseja sub-agents (**3 batches**: T1–T6, T7–T9, T10–T15) ou execução inline.
