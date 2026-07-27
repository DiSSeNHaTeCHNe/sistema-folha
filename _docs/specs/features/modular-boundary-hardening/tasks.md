# Modular Boundary Hardening — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/modular-boundary-hardening/design.md`  
**Status**: Draft  
**Approach**: A (ports agregadoras + ACL dashboard + ArchUnit sem allowlist; P2 Stats DTOs; P3 defer)

**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` (AD-004), `modular-boundary-hardening/spec.md` (MODBH ACs), Design Approach A, AD-007/008/009.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Port adapters (`FolhaConsultaAdapter`, `FolhaImportacaoAdapter`, `CadastrosImportLookupAdapter`) | unit (Mockito) | Happy path + empty/ausente; 1:1 métodos da port; sem foreign-repo em consumidores | `backend/src/test/java/**/application/*AdapterTest.java` | `cd backend && mvn test` |
| `BeneficioConsultaAdapter` (extensão scoped) | unit (Mockito) | Novo método scoped: filtro por centros + regressão unscoped existente | `backend/src/test/java/**/beneficios/application/BeneficioConsultaAdapterTest.java` | `cd backend && mvn test` |
| `DashboardService` (ACL + ports) | unit (Mockito) | MODBH-02/03/04/05/06: restrito+empty, SEM_FUNCIONARIO, total-access, centros não-vazios; mocks só ports | `backend/src/test/java/**/dashboard/application/DashboardServiceTest.java` | `cd backend && mvn test` |
| `ImportacaoFolhaAdpService` (orquestração) | unit (Mockito) | Duplicidade sem confirmar → `FolhaDuplicadaException`; path feliz chama `FolhaImportacaoPort`; zero mocks de `*.infrastructure` | `backend/src/test/java/**/importacao/application/ImportacaoFolhaAdpServiceTest.java` | `cd backend && mvn test` |
| ArchUnit | unit (ArchUnit) | Regras dashboard+importacao application → foreign infra; suite verde; sem allowlist AD-009 | `backend/src/test/java/**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Port interfaces / command records / Stats DTO moves / STATE | none | — (build / docs) | — | `cd backend && mvn clean package -DskipTests` |
| Controllers (`DashboardController`, import response) | none (thin) | Compile + wire; ACL via service tests | — | build / suite |
| Frontend | none | Build mandatory; lint advisory (AD-004) | — | `cd frontend && npm run build` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após adapters/consumers/ArchUnit unit | `cd backend && mvn test` |
| Full | Fechamento com FE + compliance | `cd backend && mvn test && cd ../frontend && npm run build && cd .. && ./diversos/scripts/check-modular-compliance.sh` |
| Build | Interface-only / DTO move | `cd backend && mvn clean package -DskipTests` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: FolhaConsultaPort

```
T1 → T2
```

### Phase 2: CadastrosImportLookupPort

```
T3 → T4
```

### Phase 3: Benefício scoped

```
T5
```

### Phase 4: FolhaImportacaoPort

```
T6 → T7
```

### Phase 5: Dashboard (P2 DTOs + ACL + ports)

```
T8 → T9
```

### Phase 6: Importação ADP refactor

```
T10
```

### Phase 7: ArchUnit + AD-010

```
T11
```

### Phase 8: Gate final

```
T12
```

**Batch packing (Execute):** ~12 tasks → 2 batches (~7 budget):  
- Batch 1: Phases 1–4 (T1–T7)  
- Batch 2: Phases 5–8 (T8–T12)

---

## Task Breakdown

### T1: Criar `FolhaConsultaPort` + snapshots

**What**: Interface pública e records de contrato em `folha.port` (sem entities).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/port/FolhaConsultaPort.java`
- `.../folha/port/FolhaLinhaSnapshot.java`
- `.../folha/port/FolhaResumoSnapshot.java`
- `.../folha/port/FolhaEvolucaoSnapshot.java`

**Depends on**: None  
**Reuses**: Design §1; padrão `BeneficioConsultaPort`  
**Requirement**: MODBH-07, MODBH-08

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [ ] Métodos do Design §1 presentes (resumo recente, linhas por competência + `Set<Long>` centros, evolução 12m, exists resumo, exists CPF, exists funcionario+rubrica+período)
- [ ] Snapshots são `record` sem imports `*.domain` entity / `*.infrastructure`
- [ ] Compila: `cd backend && mvn clean package -DskipTests`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(folha): add FolhaConsultaPort and snapshots`

---

### T2: Implementar `FolhaConsultaAdapter` + testes

**What**: Adapter `@Service` delegando a repos folha; mapeia entities → snapshots.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/application/FolhaConsultaAdapter.java`
- `backend/src/test/java/br/com/techne/sistemafolha/folha/application/FolhaConsultaAdapterTest.java`

**Depends on**: T1  
**Reuses**: `FolhaPagamentoRepository`, `ResumoFolhaPagamentoRepository`; padrão `BeneficioConsultaAdapterTest`  
**Requirement**: MODBH-09, MODBH-10

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [ ] Adapter implementa todos os métodos da port
- [ ] Filtro por `centrosCustoIds` quando não-null; `null` = unscoped
- [ ] Testes: competência com resumo; competência ausente (empty); linhas filtradas por centro
- [ ] Gate: `cd backend && mvn test`
- [ ] Test count: ≥3 testes novos passam

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): implement FolhaConsultaAdapter`

---

### T3: Criar `CadastrosImportLookupPort` + refs

**What**: Contrato cadastros para import ADP + counts (DTOs, sem entity).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/port/CadastrosImportLookupPort.java`
- `.../cadastros/port/FuncionarioImportRef.java`
- `.../cadastros/port/RubricaImportRef.java`

**Depends on**: None (após T2 no plano sequencial)  
**Reuses**: Design §3  
**Requirement**: MODBH-12

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [ ] Métodos: `findFuncionarioByIdExterno`, `findOrCreateRubrica`, `countFuncionariosAtivos`, `countFuncionariosAtivosPorCentros`
- [ ] Refs sem `@Entity` / infrastructure
- [ ] Compila: build gate

**Tests**: none  
**Gate**: build  
**Commit**: `feat(cadastros): add CadastrosImportLookupPort`

---

### T4: Implementar `CadastrosImportLookupAdapter` + testes

**What**: Adapter sobre repos Funcionario/Rubrica/TipoRubrica.  
**Where**:
- `.../cadastros/application/CadastrosImportLookupAdapter.java`
- `.../cadastros/application/CadastrosImportLookupAdapterTest.java`

**Depends on**: T3  
**Reuses**: Lógica find-or-create rubrica de `ImportacaoFolhaAdpService`; `FuncionarioRepository.findByIdExterno`  
**Requirement**: MODBH-11, MODBH-12, MODBH-13

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [ ] Find por idExterno presente/empty
- [ ] findOrCreateRubrica: existing + create path
- [ ] Counts: ativos total + por centros
- [ ] Gate: `cd backend && mvn test`
- [ ] Test count: ≥4 testes novos passam

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(cadastros): implement CadastrosImportLookupAdapter`

---

### T5: Extender `BeneficioConsultaPort` com contagem scoped

**What**: Novo método de contagem por centros + implementação no adapter + testes.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/port/BeneficioConsultaPort.java` (modify)
- `.../beneficios/application/BeneficioConsultaAdapter.java` (modify)
- `.../beneficios/application/BeneficioConsultaAdapterTest.java` (modify)

**Depends on**: None (após T4 no plano)  
**Reuses**: Adapter existente; filtro por `funcionario.centroCusto.id`  
**Requirement**: MODBH-05 (suporte), Design §4

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Método `contarLancamentosAtivosNaCompetenciaPorCentros(inicio, fim, centros)` na port
- [ ] Adapter filtra corretamente; unscoped existente intacto
- [ ] Testes: scoped retorna subset; empty set → 0; regressão método unscoped
- [ ] Gate: `cd backend && mvn test`
- [ ] Test count: suite benefícios adapter verde (≥2 novos asserts/métodos)

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(beneficios): add scoped competencia count to BeneficioConsultaPort`

---

### T6: Criar `FolhaImportacaoPort` + command records

**What**: Contrato de escrita de importação ADP + commands.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/folha/port/FolhaImportacaoPort.java`
- `.../folha/port/FolhaImportacaoCommand.java`
- `.../folha/port/FolhaImportacaoLinhaCommand.java`
- `.../folha/port/FolhaImportacaoResumoCommand.java`

**Depends on**: None (após T5 no plano; usa `FolhaPagamentoDTO` existente)  
**Reuses**: Design §2; `folha.api.FolhaPagamentoDTO` como retorno  
**Requirement**: MODBH-18, MODBH-19

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [ ] `persistirImportacao(FolhaImportacaoCommand) → List<FolhaPagamentoDTO>`
- [ ] Commands sem entities
- [ ] Compila: build gate

**Tests**: none  
**Gate**: build  
**Commit**: `feat(folha): add FolhaImportacaoPort and commands`

---

### T7: Implementar `FolhaImportacaoAdapter` + testes

**What**: Adapter `@Transactional` — substituir competência + persistir linhas/resumo.  
**Where**:
- `.../folha/application/FolhaImportacaoAdapter.java`
- `.../folha/application/FolhaImportacaoAdapterTest.java`

**Depends on**: T6  
**Reuses**: Delete+save de `ImportacaoFolhaAdpService` L103–135, L271–294  
**Requirement**: MODBH-20, MODBH-21

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [ ] `@Transactional` no método de persistência
- [ ] `substituirExistente=true` remove folhas+resumos da competência antes de inserir
- [ ] Retorna DTOs mapeados (ids/descrições)
- [ ] Testes Mockito: persist sem substituir; persist com substituir (verify delete); resumo null não salva resumo
- [ ] Gate: `cd backend && mvn test`
- [ ] Test count: ≥3 testes novos passam

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): implement FolhaImportacaoAdapter`

---

### T8: Mover `*StatsDTO` para `dashboard.api` (P2)

**What**: Relocate stats records de `cadastros.api` → `dashboard.api`; atualizar imports.  
**Where**:
- Move/create: `LinhaNegocioStatsDTO`, `CentroCustoStatsDTO`, `CargoStatsDTO`, `RubricaStatsDTO` em `dashboard.api`
- Update: `DashboardStatsDTO`, `DashboardService`, testes que importam stats
- Remove stubs antigos em `cadastros.api` se sem outros consumidores

**Depends on**: None (antes de T9 no plano)  
**Reuses**: Mesmos campos record (wire JSON idêntico)  
**Requirement**: MODBH-31, MODBH-32

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] DTOs vivem em `dashboard.api`; `DashboardStatsDTO` importa de lá
- [ ] `rg 'cadastros\.api\.(LinhaNegocio|CentroCusto|Cargo|Rubrica)StatsDTO' backend/src` → zero (ou só legado documentado)
- [ ] Compila: build gate
- [ ] FE build não obrigatório nesta task (wire inalterado) — full gate em T12

**Tests**: none  
**Gate**: build  
**Commit**: `refactor(dashboard): move stats DTOs into dashboard.api`

---

### T9: Refatorar Dashboard — ACL + ports

**What**: Controller passa login; service usa só ports; testes ACL 1:1 MODBH-02…06.  
**Where**:
- `dashboard/api/DashboardController.java` (modify)
- `dashboard/application/DashboardService.java` (modify)
- `dashboard/application/DashboardServiceTest.java` (rewrite)

**Depends on**: T2, T4, T5, T8  
**Reuses**: Design §5; padrão `FolhaPagamentoService` ACL; `emptyStats()`  
**Requirement**: MODBH-01…06, MODBH-11, MODBH-14…16

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [ ] `getStats(Authentication)` → `getStats(login)`
- [ ] Injeta: `FolhaConsultaPort`, `CadastrosImportLookupPort`, `BeneficioConsultaPort`, `OrganogramaAcessoPort`, `UsuarioLookupPort` — **zero** `*Repository`
- [ ] Short-circuit deny/empty → stats zerados; verify ports de dados **não** chamadas
- [ ] Total-access preserva agregação; restrito+centros filtra linhas/benefícios
- [ ] Evolução: restrito → lista vazia; total-access → port evolução
- [ ] Login ausente → emptyStats
- [ ] Gate: `cd backend && mvn test`
- [ ] Test count: ≥4 testes ACL + regressão path com resumo (suite dashboard verde)

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(dashboard): enforce ACL and consume folha/cadastros ports`

---

### T10: Refatorar `ImportacaoFolhaAdpService` + response DTO

**What**: Orquestração via ports; response aceita `List<FolhaPagamentoDTO>`; testes unitários.  
**Where**:
- `importacao/application/ImportacaoFolhaAdpService.java` (modify)
- `importacao/api/ImportacaoFolhaAdpResponseDTO.java` (modify)
- `importacao/api/ImportacaoFolhaAdpController.java` (modify se assinatura mudar)
- `importacao/application/ImportacaoFolhaAdpServiceTest.java` (create)

**Depends on**: T2, T4, T7  
**Reuses**: Parse ADP existente; Design §6  
**Requirement**: MODBH-22…26

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Zero imports `folha.infrastructure` / `cadastros.infrastructure` no package importacao.application
- [ ] Lookup via `CadastrosImportLookupPort`; checks via `FolhaConsultaPort`; write via `FolhaImportacaoPort`
- [ ] `@Transactional` mantido no orquestrador
- [ ] `ResponseDTO.success(arquivo, size, List<FolhaPagamentoDTO>)` — JSON fields iguais
- [ ] Testes: duplicidade sem confirmar → `FolhaDuplicadaException` + port escrita never; happy path mínimo com mocks (verify `persistirImportacao`)
- [ ] Gate: `cd backend && mvn test`
- [ ] Test count: ≥2 testes novos passam

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(importacao): orchestrate ADP import via ports`

---

### T11: ArchUnit dashboard/importacao + AD-010

**What**: Regras ArchUnit simétricas; remover allowlist; registrar AD-010.  
**Where**:
- `backend/src/test/java/br/com/techne/sistemafolha/arch/ModularArchitectureTest.java`
- `_docs/specs/STATE.md` (AD-009 superseded; AD-010 append)

**Depends on**: T9, T10  
**Reuses**: Regras `folha_application_must_not_access_foreign_infrastructure`  
**Requirement**: MODBH-27…30

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Regras `dashboard_application_must_not_access_foreign_infrastructure` e `importacao_application_must_not_access_foreign_infrastructure`
- [ ] Constante/comentário allowlist AD-009 removido das mensagens `because`
- [ ] `mvn test -Dtest=ModularArchitectureTest` exit 0
- [ ] AD-010 em STATE; AD-009 status `superseded by AD-010`
- [ ] Gate: `cd backend && mvn test -Dtest=ModularArchitectureTest`

**Tests**: unit (ArchUnit)  
**Gate**: quick  
**Commit**: `test(arch): enforce dashboard/importacao isolation; supersede AD-009`

---

### T12: Gate conformidade final + handoff

**What**: Suite completa + FE build + compliance script; atualizar handoff STATE.  
**Where**: `_docs/specs/STATE.md` (Handoff); opcional nota em design Status=Executed  
**Depends on**: T11  
**Reuses**: `check-modular-compliance.sh`  
**Requirement**: MODBH-17, Success Criteria

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Full gate: `cd backend && mvn test && cd ../frontend && npm run build && cd .. && ./diversos/scripts/check-modular-compliance.sh` exit 0 (mandatory parts)
- [ ] `rg 'folha\.infrastructure|cadastros\.infrastructure' backend/src/main/java/**/dashboard/application` → zero
- [ ] `rg 'folha\.infrastructure|cadastros\.infrastructure' backend/src/main/java/**/importacao/application` → zero
- [ ] STATE Handoff: Feature C Execute done; AD-010 active; ready for Verifier
- [ ] Spec Success Criteria checkboxes atualizados onde aplicável

**Tests**: none (gate agregado)  
**Gate**: full  
**Commit**: `docs(modular-boundary-hardening): close Execute handoff`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8

Phase 1:  T1 ──→ T2
Phase 2:  T3 ──→ T4
Phase 3:  T5
Phase 4:  T6 ──→ T7
Phase 5:  T8 ──→ T9
Phase 6:  T10
Phase 7:  T11
Phase 8:  T12
```

Execution is strictly sequential — no intra-phase parallelism.

**Batches (Execute offer):**
1. **Batch 1** (T1–T7): ports folha consulta, cadastros import, benefício scoped, folha importação  
2. **Batch 2** (T8–T12): dashboard, importação consumer, ArchUnit/AD-010, gate final  

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: FolhaConsultaPort + snapshots | 1 port + records coesos | ✅ Granular |
| T2: FolhaConsultaAdapter + tests | 1 adapter + tests | ✅ Granular |
| T3: CadastrosImportLookupPort + refs | 1 port + records | ✅ Granular |
| T4: CadastrosImportLookupAdapter + tests | 1 adapter + tests | ✅ Granular |
| T5: BeneficioConsultaPort scoped | 1 método + adapter + tests | ✅ Granular |
| T6: FolhaImportacaoPort + commands | 1 port + commands | ✅ Granular |
| T7: FolhaImportacaoAdapter + tests | 1 adapter + tests | ✅ Granular |
| T8: Move Stats DTOs | 1 relocate coeso | ✅ Granular |
| T9: Dashboard ACL + ports | 1 service + controller + tests | ✅ Granular (mesmo feature slice) |
| T10: Importacao refactor + response | 1 orquestrador + DTO borda + tests | ✅ Granular |
| T11: ArchUnit + AD-010 | 1 enforce + decision log | ✅ Granular |
| T12: Final gate + handoff | 1 fechamento | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ----------------- | ------------- | ------ |
| T1 | None | (start) | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | None | (start Phase 2) | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | None | (start Phase 3) | ✅ Match |
| T6 | None | (start Phase 4) | ✅ Match |
| T7 | T6 | T6 → T7 | ✅ Match |
| T8 | None | (start Phase 5) | ✅ Match |
| T9 | T2, T4, T5, T8 | Phase 5 after 1–4; T8 → T9 | ✅ Match |
| T10 | T2, T4, T7 | Phase 6 after ports | ✅ Match |
| T11 | T9, T10 | Phase 7 after consumers | ✅ Match |
| T12 | T11 | T11 → T12 | ✅ Match |

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | Port interfaces | none | none | ✅ OK |
| T2 | FolhaConsultaAdapter | unit | unit | ✅ OK |
| T3 | Port interfaces | none | none | ✅ OK |
| T4 | CadastrosImportLookupAdapter | unit | unit | ✅ OK |
| T5 | BeneficioConsultaAdapter | unit | unit | ✅ OK |
| T6 | Port interfaces | none | none | ✅ OK |
| T7 | FolhaImportacaoAdapter | unit | unit | ✅ OK |
| T8 | DTO move | none | none | ✅ OK |
| T9 | DashboardService | unit | unit | ✅ OK |
| T10 | ImportacaoFolhaAdpService | unit | unit | ✅ OK |
| T11 | ArchUnit | unit (ArchUnit) | unit | ✅ OK |
| T12 | docs/gate | none | none | ✅ OK |

**MODBH-33 (P3):** deferred — sem task (Design).

---

## Requirement Traceability (tasks)

| Requirement | Task(s) |
| ----------- | ------- |
| MODBH-01…06 | T9 |
| MODBH-07…10 | T1, T2 |
| MODBH-11…13 | T3, T4, T9 |
| MODBH-14…17 | T9, T12 |
| MODBH-18…21 | T6, T7 |
| MODBH-22…26 | T10 |
| MODBH-27…30 | T11 |
| MODBH-31…32 | T8 |
| MODBH-33 | Deferred |

**Coverage:** 32 mapped + 1 deferred P3 = 33 total.
