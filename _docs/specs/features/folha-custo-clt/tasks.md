# Folha CLT — Bruto, Líquido e Custo Empresa Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/folha-custo-clt/design.md`  
**Status**: Draft — aguardando aprovação antes de Execute  
**Approach**: A — Motor único + port estendida (ARCH-1 Opção B, ACL dual-path, INT-1/INT-2)  
**User preference (project):** sem commits automáticos salvo pedido explícito — Execute pode pular `git commit` se o usuário disser “sem commit”.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md`, AD-004 (FE skills = target), AD-007/008/010/011, `folha-custo-clt/spec.md` (FCLT ACs + edge cases).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Motor / composer / rateio (pure application) | unit | All branches; 1:1 FCLT-05, FCLT-08, FCLT-13; arredondamento HALF_UP; última parcela encargos | `backend/src/test/java/**/folha/application/*Test.java` | `cd backend && mvn test -Dtest=FolhaMotorCalculoTest,EncargosRateioServiceTest` |
| Processamento / totalização / agregação / ficha consulta | unit (Mockito) | FCLT-04, 07, 16, 22, 23, ACL-07…13, INT-01/02; edge cases spec §Edge Cases aplicáveis ao layer | `**/folha/application/*ServiceTest.java`, `FolhaLinhaAgregacaoTest.java` | `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest,FolhaTotalizacaoServiceTest,FolhaLinhaAgregacaoTest,FolhaFichaConsultaServiceTest` |
| Resumo ACL | unit (Mockito) | FCLT-ACL-01…06 incl. discrimination sensor (never read global ficha totals scoped) | `**/ResumoFolhaPagamentoServiceTest.java` | `cd backend && mvn test -Dtest=ResumoFolhaPagamentoServiceTest` |
| Dashboard custo empresa | unit (Mockito) | FCLT-ACL-16…18; scoped ≠ liquido global; evolução scoped custo empresa | `**/dashboard/application/DashboardServiceTest.java` | `cd backend && mvn test -Dtest=DashboardServiceTest` |
| Port adapters (FolhaConsulta, BeneficioConsulta) | unit (Mockito) | Dual source ficha/fallback; batch benefit sums; CC filter | `**/FolhaConsultaAdapterTest.java`, `**/BeneficioConsultaAdapterTest.java` | `cd backend && mvn test -Dtest=FolhaConsultaAdapterTest,BeneficioConsultaAdapterTest` |
| Cadastros (Rubrica operadores, RubricaFixa) | unit (Mockito) | FCLT-02, 18, 20; operador validation; overlap 409 | `**/cadastros/application/*Test.java` | `cd backend && mvn test -Dtest=RubricaServiceTest,FuncionarioRubricaFixaServiceTest` |
| API controllers (Folha/Cadastros) | none | Auth wiring; compile + caller service tests | `folha/api/*`, `cadastros/api/*` | compile via Full gate |
| Flyway migrations / entities | none | `mvn flyway:migrate` ou compile; schema idempotente | `db/migration/V1.17__*.sql` … | `cd backend && mvn test` (compile) |
| Frontend pages/services | none (AD-004 gap) | Build + lint; manual walkthrough FCLT-09…12, ACL-10/11/14 | `frontend/src/pages/**` | `cd frontend && npm run lint && npm run build` |
| ArchUnit modular | unit | AD-010 — dashboard/importação sem infra estrangeira | `**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após tasks com unit tests backend | `cd backend && mvn test -Dtest=<ClassTest>` (classe(s) da task) |
| Full | Após T27 (fechamento feature) | `cd backend && mvn test && cd ../frontend && npm run lint && npm run build` |
| Build | Migrations / entities-only | `cd backend && mvn clean compile` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Schema + motor + processamento base (7 tasks)

```
T1 → T2 → T3 → T4 → T5 → T6 → T7
```

### Phase 2: Ports + ACL read path (6 tasks)

```
T8 → T9 → T10 → T11 → T12 → T13
```

### Phase 3: Detalhe ficha + dashboard (3 tasks)

```
T14 → T15 → T16
```

### Phase 4: Custos fixos + férias + regime (4 tasks)

```
T17 → T18 → T19 → T20
```

### Phase 5: Frontend (6 tasks)

```
T21 → T22 → T23 → T24 → T25 → T26
```

### Phase 6: Gate final (1 task)

```
T27
```

**Batch packing (~7 tasks/worker, whole phases):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | Phase 1 | T1–T7 | 7 |
| 2 | Phase 2 | T8–T13 | 6 |
| 3 | Phase 3 + 4 | T14–T20 | 7 |
| 4 | Phase 5 + 6 | T21–T27 | 7 |

→ **4 workers** sequenciais (offer-then-confirm no Execute). Inline se usuário recusar sub-agents.

---

## Task Breakdown

### T1: Flyway V1.17 — operadores de rubrica

**What**: Migração `operador_bruto/liquido/custo` em `rubricas` com backfill de `tipo_rubrica`; estender entity `Rubrica`.  
**Where**:
- `backend/src/main/resources/db/migration/V1.17__rubrica_operadores.sql`
- `backend/src/main/java/br/com/techne/sistemafolha/cadastros/domain/Rubrica.java`

**Depends on**: None  
**Reuses**: Seed `tipo_rubrica` (V1.1); lógica PROVENTO/DESCONTO de `FolhaTotalizacaoService.coeficientesDe`  
**Requirement**: FCLT-01

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`, `jpa-performance`

**Done when**:
- [x] Colunas SMALLINT NOT NULL com default derivado do tipo
- [x] Entity JPA sincronizada
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(cadastros): add rubrica operadores migration V1.17`

---

### T2: API cadastro rubrica — operadores

**What**: Estender DTOs + `RubricaService` + controller para persistir/validar `operadorBruto/Liquido/Custo ∈ {-1,0,1}`.  
**Where**:
- `cadastros/api/RubricaDTO.java` (ou records existentes)
- `cadastros/application/RubricaService.java`
- `cadastros/api/RubricaController.java`
- `backend/src/test/java/.../cadastros/application/RubricaServiceTest.java` (criar se ausente)

**Depends on**: T1  
**Reuses**: Padrão CRUD `RubricaService` existente  
**Requirement**: FCLT-02

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `spring-security`

**Done when**:
- [x] POST/PUT aceitam operadores; valores inválidos → 400
- [x] Testes unitários cobrem validação e persistência
- [x] Gate: `cd backend && mvn test -Dtest=RubricaServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(cadastros): rubrica operadores API`

---

### T3: Flyway V1.21 reconcile + V1.18 ficha_mensal/linha

**What**: Reconciliar schema `folha_pagamento`; criar tabelas `ficha_mensal`, `ficha_linha`, enum `origem_linha`, índices.  
**Where**:
- `V1.21__folha_pagamento_reconcile.sql`
- `V1.18__ficha_mensal_linha.sql`

**Depends on**: T1  
**Reuses**: Padrão migrations V1.2/V1.16  
**Requirement**: FCLT-04, FCLT-07

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`

**Done when**:
- [x] Migrations idempotentes e ordenadas V1.21 antes V1.18 se dependência de colunas
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(folha): ficha_mensal schema migrations V1.18/V1.21`

---

### T4: Entities + repositories ficha

**What**: `FichaMensal`, `FichaLinha`, enum `OrigemLinha`; repositories JPA com queries por competência.  
**Where**:
- `folha/domain/FichaMensal.java`, `FichaLinha.java`, `OrigemLinha.java`
- `folha/infrastructure/FichaMensalRepository.java`, `FichaLinhaRepository.java`

**Depends on**: T3  
**Reuses**: Padrão `FolhaPagamento` entity  
**Requirement**: FCLT-04, FCLT-07

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`, `flyway-migration-writer`

**Done when**:
- [x] Entities mapeiam schema T3
- [x] Repository: findByCompetencia, deleteByCompetencia (replace idempotente)
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(folha): FichaMensal and FichaLinha entities`

---

### T5: FolhaMotorCalculo + FolhaCustoEmpresaComposer

**What**: Helpers package-private com fórmulas bruto/líquido/custoFolha e composição custoEmpresa.  
**Where**:
- `folha/application/FolhaMotorCalculo.java`
- `folha/application/FolhaCustoEmpresaComposer.java`
- `FolhaMotorCalculoTest.java`, `FolhaCustoEmpresaComposerTest.java`

**Depends on**: None  
**Reuses**: Loop arredondamento de `FolhaTotalizacaoService`  
**Requirement**: FCLT-05, FCLT-06 (fórmula), FCLT-08 (partial)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] PROVENTO+DESCONTO; operador custom; HALF_UP 2 casas
- [x] Composer: custoFolha + encargos + benefícios
- [x] Gate: `cd backend && mvn test -Dtest=FolhaMotorCalculoTest,FolhaCustoEmpresaComposerTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): motor calculo and custo empresa composer`

---

### T6: FolhaProcessamentoService — ADP → ficha + POST /processar

**What**: Processamento idempotente: copiar `folha_pagamento` → `ficha_linha` (FOLHA_ADP, operadores snapshot); recalcular `ficha_mensal` totals; expor endpoint.  
**Where**:
- `folha/application/FolhaProcessamentoService.java`
- `folha/api/FolhaProcessamentoController.java`
- `folha/api/ProcessamentoRequestDTO.java`
- `FolhaProcessamentoServiceTest.java`

**Depends on**: T4, T5  
**Reuses**: Replace-by-competência de `FolhaImportacaoAdapter`  
**Requirement**: FCLT-04, FCLT-05, FCLT-07

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `spring-security`, `jpa-performance`

**Done when**:
- [x] POST `/folha-pagamento/processar` persiste ficha com bruto/liquido/custoFolha
- [x] Operadores copiados na linha; origemLinha=FOLHA_ADP
- [x] Testes: import simulado → processar → totais corretos
- [x] Gate: `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): processamento ADP to ficha`

---

### T7: FolhaTotalizacaoService — operadores + testes motor

**What**: Refatorar totalização para usar operadores de linha/rubrica (não `tipo_rubrica` string); estender testes FCLT-08.  
**Where**:
- `folha/application/FolhaTotalizacaoService.java`
- `FolhaTotalizacaoServiceTest.java`

**Depends on**: T5  
**Reuses**: `FolhaMotorCalculo`, `BeneficioConsultaPort`  
**Requirement**: FCLT-05, FCLT-08

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] DESCONTO impacta só líquido; PROVENTO impacta os três
- [x] Arredondamento e benefícios intactos
- [x] Gate: `cd backend && mvn test -Dtest=FolhaTotalizacaoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(folha): totalizacao uses rubrica operadores`

---

### T8: BeneficioConsultaPort — batch sums

**What**: Métodos batch `somarValorPorFuncionariosECompetencia` e `somarValorPorCompetenciaECentros`; adapter + testes.  
**Where**:
- `beneficios/port/BeneficioConsultaPort.java`
- `beneficios/application/BeneficioConsultaAdapter.java`
- `BeneficioConsultaAdapterTest.java`

**Depends on**: None  
**Reuses**: Queries existentes `BeneficioMensalRepository`  
**Requirement**: FCLT-06, FCLT-INT-01, ACL-5

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Batch evita N+1 no resumo/cards
- [x] Filtro CC na query (não in-memory)
- [x] Gate: `cd backend && mvn test -Dtest=BeneficioConsultaAdapterTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(beneficios): batch consulta port for custo empresa`

---

### T9: EncargosRateioService

**What**: Rateio proporcional ao bruto CLT; ajuste centavos última parcela; encargos=0 documentado para scoped callers.  
**Where**:
- `folha/application/EncargosRateioService.java`
- `EncargosRateioServiceTest.java`

**Depends on**: T5  
**Reuses**: `totalEncargos` de `ResumoFolhaPagamento`  
**Requirement**: FCLT-13, FCLT-14

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] 8k/2k bruto, 1k encargos → 800/200; soma ± R$0,01
- [x] Gate: `cd backend && mvn test -Dtest=EncargosRateioServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): encargos rateio D4-CLT`

---

### T10: FolhaConsultaPort — dual source + FolhaLinhaSnapshot extend
**Where**:
- `folha/port/FolhaConsultaPort.java`, `FolhaLinhaSnapshot.java`
- `folha/application/FolhaConsultaAdapter.java`
- `FolhaConsultaAdapterTest.java`

**Depends on**: T4, T6  
**Reuses**: `findLinhasAtivasPorCompetencia` existente  
**Requirement**: ARCH-1, FCLT-ACL-08 (foundation)

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Pós-processamento usa ficha; pré-processamento fallback ADP
- [x] `@EntityGraph` ou fetch join para evitar N+1
- [x] Gate: `cd backend && mvn test -Dtest=FolhaConsultaAdapterTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): consulta port dual source ficha/adp`

---

### T11: FolhaLinhaAgregacao — três totalizadores

**What**: Evoluir agregação para bruto/líquido/custoFolha/custoEmpresa com benefícios e encargos maps.  
**Where**:
- `folha/application/FolhaLinhaAgregacao.java`
- `FolhaLinhaAgregacaoTest.java`

**Depends on**: T5, T8  
**Reuses**: `FolhaMotorCalculo`, `FolhaCustoEmpresaComposer`  
**Requirement**: FCLT-ACL-01, FCLT-ACL-02, ACL-4

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Scoped: encargos sempre 0; custoEmpresa inclui benefícios
- [ ] Testes: mix operadores; lista vazia → zeros
- [ ] Gate: `cd backend && mvn test -Dtest=FolhaLinhaAgregacaoTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): agregacao three totalizers`

---

### T12: ResumoFolhaPagamentoService — ACL 3 totais + discrimination

**What**: Estender DTO resumo; global agrega ficha+encargos+benefícios; scoped recalcula linhas; FCLT-ACL-06 sensor.  
**Where**:
- `folha/api/ResumoFolhaPagamentoDTO.java`
- `folha/application/ResumoFolhaPagamentoService.java`
- `ResumoFolhaPagamentoServiceTest.java`

**Depends on**: T9, T10, T11  
**Reuses**: Dual-path RSF; A2 zeros; deny []  
**Requirement**: FCLT-ACL-01…06, FCLT-06

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [ ] Scoped: totalBruto/Liquido/CustoEmpresa; encargos=0
- [ ] Global: inclui encargos rateados + benefícios
- [ ] Teste verify: scoped **never** calls repository de totais globais ficha
- [ ] Gate: `cd backend && mvn test -Dtest=ResumoFolhaPagamentoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): resumo ACL three totalizers`

---

### T13: Totais por funcionário — API cards + INT-01

**What**: Refatorar `GET /totais-funcionarios` via port; DTO `custoEmpresa`/`encargosRateados`; param `decimoTerceiro`; composição benefícios pós-import.  
**Where**:
- `folha/api/FolhaTotaisFuncionarioDTO.java`
- `folha/application/FolhaPagamentoService.java`
- `folha/application/FolhaTotalizacaoService.java`
- `folha/api/FolhaPagamentoController.java`
- `FolhaPagamentoServiceTest.java`, `FolhaTotalizacaoServiceTest.java`

**Depends on**: T7, T8, T9, T10  
**Reuses**: `aplicarFiltroAcesso`  
**Requirement**: FCLT-ACL-07…09, FCLT-INT-01

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `spring-security`

**Done when**:
- [ ] Scoped: encargosRateados=0; custoEmpresa = custoFolha + benefícios
- [ ] Global: rateio encargos por funcionário
- [ ] Benefício lançado após folha reflete na consulta sem reimport
- [ ] Gate: `cd backend && mvn test -Dtest=FolhaPagamentoServiceTest,FolhaTotalizacaoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): totais funcionarios custo empresa ACL`

---

### T14: FolhaFichaConsultaService + API totalizer

**What**: `GET /fichas/{id}/linhas?totalizer=`; ACL out-of-scope 404; aba Custo inclui BENEFICIO na consulta.  
**Where**:
- `folha/application/FolhaFichaConsultaService.java`
- `folha/api/FolhaFichaController.java`
- `folha/api/FichaLinhaDetalheDTO.java`, `Totalizador.java`
- `FolhaFichaConsultaServiceTest.java`

**Depends on**: T10, T8  
**Reuses**: `FolhaMotorCalculo.contribuicao`; critério ACL `FolhaPagamentoService`  
**Requirement**: FCLT-ACL-12…13, FCLT-ACL-15

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `spring-security`

**Done when**:
- [ ] GROSS/NET/COMPANY_COST filtram operador ≠ 0
- [ ] COMPANY_COST lista ficha_linha por origem + benefícios (origem=BENEFICIO)
- [ ] Scoped: encargos não listados
- [ ] Gate: `cd backend && mvn test -Dtest=FolhaFichaConsultaServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): ficha linhas by totalizer ACL`

---

### T15: DashboardService — custo empresa KPI + evolução

**What**: `custoMensalFolha` = custo empresa (não líquido); scoped/global; evolução mensal scoped com custo empresa.  
**Where**:
- `dashboard/application/DashboardService.java`
- `dashboard/api/DashboardStatsDTO.java` (se necessário)
- `DashboardServiceTest.java`

**Depends on**: T10, T11, T12  
**Reuses**: `FolhaConsultaPort`, `BeneficioConsultaPort` (AD-010)  
**Requirement**: FCLT-ACL-16…18

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Scoped KPI ≠ liquido global snapshot
- [ ] Global inclui encargos
- [ ] Evolução scoped usa custo empresa por competência
- [ ] Gate: `cd backend && mvn test -Dtest=DashboardServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(dashboard): custo empresa KPI and evolution`

---

### T16: FolhaEvolucaoSnapshot — decimoTerceiro (se necessário)

**What**: Estender snapshot de evolução com flag `decimoTerceiro` se T15 exigir; atualizar adapter e testes dashboard.  
**Where**:
- `folha/port/FolhaEvolucaoSnapshot.java`
- `folha/application/FolhaConsultaAdapter.java`
- `DashboardServiceTest.java`

**Depends on**: T15  
**Reuses**: Padrão `acl-scoped-folha-resumo` T1  
**Requirement**: FCLT-ACL-18 (13º separado)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Evolução scoped não mistura folha regular e 13º
- [ ] Gate: `cd backend && mvn test -Dtest=DashboardServiceTest,FolhaConsultaAdapterTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(folha): evolucao snapshot decimo terceiro`

---

### T17: Flyway V1.19 + V1.20 — rubrica fixa + regime CLT

**What**: Tabela `funcionario_rubrica_fixa`; `regime_trabalho` seed CLT; FK funcionários.  
**Where**:
- `V1.19__funcionario_rubrica_fixa.sql`
- `V1.20__regime_trabalho.sql`
- Entities correspondentes em `cadastros/domain/`

**Depends on**: T1  
**Reuses**: Padrão soft-delete `ativo`  
**Requirement**: FCLT-18, FCLT-15

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`, `jpa-performance`

**Done when**:
- [ ] Schema + entities compilam
- [ ] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(cadastros): rubrica fixa and regime trabalho schema`

---

### T18: FuncionarioRubricaFixa — CRUD API

**What**: Service + controller CRUD; overlap vigência → 409; validação valor obrigatório se não calculada.  
**Where**:
- `cadastros/application/FuncionarioRubricaFixaService.java`
- `cadastros/api/FuncionarioRubricaFixaController.java`
- `FuncionarioRubricaFixaServiceTest.java`

**Depends on**: T17, T2  
**Reuses**: Padrão `BeneficioMensalService` CRUD  
**Requirement**: FCLT-19, FCLT-20

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `spring-security`

**Done when**:
- [ ] CRUD funcional; 409 em overlap; 400 sem valor
- [ ] Gate: `cd backend && mvn test -Dtest=FuncionarioRubricaFixaServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(cadastros): funcionario rubrica fixa CRUD`

---

### T19: Processamento — injeção CUSTO_FIXO + dedup

**What**: Estender `FolhaProcessamentoService` para injetar fixos vigentes; dedup ADP vs fixo (WARN log).  
**Where**:
- `folha/application/FolhaProcessamentoService.java`
- `FolhaProcessamentoServiceTest.java`

**Depends on**: T6, T18  
**Reuses**: `funcionario_rubrica_fixa` repository  
**Requirement**: FCLT-22, FCLT-23, FCLT-INT-02, FCLT-24

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [ ] Linha CUSTO_FIXO na ficha após processar
- [ ] Alteração cadastro não reflete até reprocessar
- [ ] ACL scoped filtra por CC do funcionário
- [ ] Gate: `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): inject custo fixo on processamento`

---

### T20: Processamento — férias proporcionais CALCULADO

**What**: Injetar rubrica férias fator 2,5 quando `opcoes.recalcularFerias=true`; origem CALCULADO.  
**Where**:
- `folha/application/FolhaProcessamentoService.java`
- `FolhaProcessamentoServiceTest.java`

**Depends on**: T19  
**Reuses**: Rubrica cod. semântico `5000` (configurável)  
**Requirement**: FCLT-16, FCLT-25

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Linha férias presente com origem CALCULADO
- [ ] Totais refletem operadores da rubrica
- [ ] Gate: `cd backend && mvn test -Dtest=FolhaProcessamentoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(folha): ferias proporcionais calculado`

---

### T21: FE — Rubricas operadores UI

**What**: Formulário rubrica exibe/edita três operadores com labels acessíveis.  
**Where**:
- `frontend/src/pages/Rubricas/` (ou equivalente)

**Depends on**: T2  
**Reuses**: Padrão formulários existentes  
**Requirement**: FCLT-03

**Tools**:
- MCP: NONE
- Skill: `forms-validation`, `component-architecture` (target/AD-004)

**Done when**:
- [ ] Campos operador visíveis e persistem via API
- [ ] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(frontend): rubrica operadores form`

---

### T22: FE — Resumo colunas Bruto/Líquido/Custo

**What**: Estender tipos e tabela resumo com três colunas; strings decimais para money.  
**Where**:
- `frontend/src/pages/FolhaPagamento/index.tsx`
- `frontend/src/services/resumoFolhaPagamentoService.ts`
- types OpenAPI/manual

**Depends on**: T12  
**Reuses**: Layout resumo existente  
**Requirement**: FCLT-09, FCLT-12 (partial)

**Tools**:
- MCP: NONE
- Skill: `api-client`

**Done when**:
- [ ] Colunas exibidas; sem `Number()` para cálculo
- [ ] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(frontend): resumo bruto liquido custo columns`

---

### T23: FE — Cards via `/totais-funcionarios`

**What**: Remover `reduce` local; consumir API totais; exibir bruto/líquido/custoEmpresa.  
**Where**:
- `frontend/src/services/folhaPagamentoService.ts`
- `frontend/src/pages/FolhaPagamento/index.tsx`

**Depends on**: T13, T22  
**Reuses**: FE-1 — zero agregação local  
**Requirement**: FCLT-10, FCLT-ACL-10, FCLT-ACL-11

**Tools**:
- MCP: NONE
- Skill: `api-client`

**Done when**:
- [ ] Cards usam API; sem reduce de linhas
- [ ] Três valores por funcionário
- [ ] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(frontend): cards from totais-funcionarios API`

---

### T24: FE — Detalhe abas totalizer

**What**: Tabs Bruto/Líquido/Custo acessíveis; fetch `fichas/{id}/linhas?totalizer=`; agrupar origem na aba Custo.  
**Where**:
- `frontend/src/pages/FolhaPagamento/index.tsx` (dialog detalhe)

**Depends on**: T14, T23  
**Reuses**: Padrão tabs existente no projeto  
**Requirement**: FCLT-11, FCLT-ACL-14, FCLT-ACL-15

**Tools**:
- MCP: NONE
- Skill: `component-architecture`, `testing-a11y` (target)

**Done when**:
- [ ] Abas keyboard-navigable; cada aba lista rubricas filtradas pela API
- [ ] Aba Custo distingue origens incl. BENEFICIO
- [ ] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(frontend): folha detail totalizer tabs`

---

### T25: FE — Rubricas Fixas CRUD

**What**: Nova rota/página CRUD `funcionario_rubrica_fixa` filtrável por funcionário/rubrica.  
**Where**:
- `frontend/src/pages/RubricasFixas/` (nova)
- router config

**Depends on**: T18  
**Reuses**: Padrão listagens cadastro  
**Requirement**: FCLT-21

**Tools**:
- MCP: NONE
- Skill: `routing-perf`, `forms-validation`, `component-architecture`

**Done when**:
- [ ] CRUD funcional; campos valor/vigência acessíveis
- [ ] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(frontend): rubricas fixas CRUD page`

---

### T26: FE — Dashboard label Custo Empresa

**What**: Renomear label KPI; exibir valor string da API.  
**Where**:
- `frontend/src/pages/Dashboard/index.tsx`

**Depends on**: T15  
**Reuses**: Card KPI existente  
**Requirement**: FCLT-ACL-16 (FE label)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Label "Custo Empresa"; valor formatado sem recalcular
- [ ] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: build  
**Commit**: `feat(frontend): dashboard custo empresa label`

---

### T27: Full gate + ArchUnit regression

**What**: Rodar suite completa backend + build FE; confirmar ArchUnit AD-010; preparar handoff para Verifier.  
**Where**: repo root  
**Depends on**: T21–T26  
**Reuses**: Gate commands acima  
**Requirement**: all FCLT (exceto FCLT-17 P3 deferred)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `cd backend && mvn test` — all green
- [ ] `cd frontend && npm run lint && npm run build` — success
- [ ] `ModularArchitectureTest` passa
- [ ] Atualizar traceability em `spec.md` (Execute → Done por task)
- [ ] Verifier automático pós-T27 (skill Execute)

**Tests**: unit (full suite)  
**Gate**: full  
**Commit**: `chore(folha-custo-clt): full gate pass`

---

## Phase Execution Map

```
Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4 ──→ T5 ──→ T6 ──→ T7
Phase 2:  T8 ──→ T9 ──→ T10 ──→ T11 ──→ T12 ──→ T13
Phase 3:  T14 ──→ T15 ──→ T16
Phase 4:  T17 ──→ T18 ──→ T19 ──→ T20
Phase 5:  T21 ──→ T22 ──→ T23 ──→ T24 ──→ T25 ──→ T26
Phase 6:  T27
```

Execution is strictly sequential — one task at a time, in order.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Migration operadores | 1 migration + entity | ✅ Granular |
| T2: API rubrica operadores | 1 endpoint layer + tests | ✅ Granular |
| T3: Migrations ficha | 2 migrations | ✅ Granular (cohesive schema) |
| T4: Entities ficha | domain + repos | ✅ Granular |
| T5: Motor + composer | 2 pure classes + tests | ✅ Granular |
| T6: Processamento ADP | 1 service + 1 endpoint | ✅ Granular |
| T7: Totalizacao refactor | 1 service + tests | ✅ Granular |
| T8: Beneficio batch port | 1 port extend + adapter | ✅ Granular |
| T9: Encargos rateio | 1 service + tests | ✅ Granular |
| T10: Consulta port dual | port + adapter | ✅ Granular |
| T11: Agregacao 3 totais | 1 helper + tests | ✅ Granular |
| T12: Resumo ACL | 1 service + DTO + tests | ✅ Granular |
| T13: Totais funcionarios | service + controller + DTO | ✅ Granular |
| T14: Ficha consulta API | 1 service + 1 controller | ✅ Granular |
| T15: Dashboard custo | 1 service modify | ✅ Granular |
| T16: Evolucao DT13 | port record extend | ✅ Granular |
| T17: Migrations fixa/regime | 2 migrations | ✅ Granular |
| T18: CRUD rubrica fixa | 1 service + controller | ✅ Granular |
| T19: Inject custo fixo | extend processamento | ✅ Granular |
| T20: Ferias calculado | extend processamento | ✅ Granular |
| T21–T26: FE screens | 1 screen/concern each | ✅ Granular |
| T27: Full gate | verification only | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ----------------- | ------------- | ------ |
| T1 | None | Phase 1 start | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T1 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | None | T4 → T5 | ⚠️ T5 independente de T4 no body; ordem de fase OK (motor antes processamento) |
| T6 | T4, T5 | T5 → T6 | ✅ Match |
| T7 | T5 | T6 → T7 | ✅ Match |
| T8 | None | Phase 2 start | ✅ Match |
| T9 | T5 | T8 → T9 | ✅ Match |
| T10 | T4, T6 | T9 → T10 | ✅ Match |
| T11 | T5, T8 | T10 → T11 | ✅ Match |
| T12 | T9, T10, T11 | T11 → T12 | ✅ Match |
| T13 | T7, T8, T9, T10 | T12 → T13 | ✅ Match |
| T14 | T10, T8 | Phase 3 T14 | ✅ Match |
| T15 | T10, T11, T12 | T14 → T15 | ✅ Match |
| T16 | T15 | T15 → T16 | ✅ Match |
| T17 | T1 | Phase 4 start | ✅ Match |
| T18 | T17, T2 | T17 → T18 | ✅ Match |
| T19 | T6, T18 | T18 → T19 | ✅ Match |
| T20 | T19 | T19 → T20 | ✅ Match |
| T21 | T2 | Phase 5 (FE após BE APIs) | ✅ Match |
| T22 | T12 | T21 → T22 | ✅ Match |
| T23 | T13, T22 | T22 → T23 | ✅ Match |
| T24 | T14, T23 | T23 → T24 | ✅ Match |
| T25 | T18 | T24 → T25 | ✅ Match |
| T26 | T15 | T25 → T26 | ✅ Match |
| T27 | T21–T26 | Phase 6 | ✅ Match |

> T5 `Depends on: None` — posicionado na fase após T4 por conveniência de gate; não viola dependências backward.

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | Flyway/entity | none | none | ✅ OK |
| T2 | Cadastros service | unit | unit | ✅ OK |
| T3 | Flyway | none | none | ✅ OK |
| T4 | Entity/repo | none | none | ✅ OK |
| T5 | Motor/composer | unit | unit | ✅ OK |
| T6 | Processamento service | unit | unit | ✅ OK |
| T7 | Totalizacao service | unit | unit | ✅ OK |
| T8 | Port adapter | unit | unit | ✅ OK |
| T9 | Rateio service | unit | unit | ✅ OK |
| T10 | Port adapter | unit | unit | ✅ OK |
| T11 | Agregacao helper | unit | unit | ✅ OK |
| T12 | Resumo service | unit | unit | ✅ OK |
| T13 | Folha service | unit | unit | ✅ OK |
| T14 | Ficha consulta service | unit | unit | ✅ OK |
| T15 | Dashboard service | unit | unit | ✅ OK |
| T16 | Port snapshot | unit | unit | ✅ OK |
| T17 | Flyway/entity | none | none | ✅ OK |
| T18 | Cadastros service | unit | unit | ✅ OK |
| T19 | Processamento service | unit | unit | ✅ OK |
| T20 | Processamento service | unit | unit | ✅ OK |
| T21–T26 | FE | none | none | ✅ OK |
| T27 | Full suite | unit | unit | ✅ OK |

---

## Requirement Traceability (Tasks)

| Requirement | Task(s) |
| ----------- | ------- |
| FCLT-01 | T1 |
| FCLT-02 | T2 |
| FCLT-03 | T21 |
| FCLT-04 | T3, T4, T6 |
| FCLT-05 | T5, T6, T7 |
| FCLT-06 | T5, T12, T13 |
| FCLT-07 | T3, T4, T6 |
| FCLT-08 | T5, T7 |
| FCLT-INT-01 | T8, T13 |
| FCLT-09 | T22 |
| FCLT-10 | T23 |
| FCLT-11 | T24 |
| FCLT-12 | T22, T23 |
| FCLT-13, FCLT-14 | T9 |
| FCLT-15 | T17 |
| FCLT-16, FCLT-25 | T20 |
| FCLT-17 | — (P3 deferred) |
| FCLT-18 | T17 |
| FCLT-19 | T18 |
| FCLT-20 | T18 |
| FCLT-21 | T25 |
| FCLT-22, FCLT-23, FCLT-24 | T19 |
| FCLT-INT-02 | T19 |
| FCLT-ACL-01…06 | T11, T12 |
| FCLT-ACL-07…09 | T13 |
| FCLT-ACL-10, FCLT-ACL-11 | T23 |
| FCLT-ACL-12…15 | T14, T24 |
| FCLT-ACL-16…18 | T15, T16, T26 |
