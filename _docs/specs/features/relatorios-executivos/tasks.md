# Relatórios Executivos — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/relatorios-executivos/design.md`  
**Spec**: `_docs/specs/features/relatorios-executivos/spec.md`  
**Branding**: `_docs/specs/features/relatorios-executivos/branding/` (copiar para runtime em T2)  
**Status**: Execute complete (2026-08-03) — T1–T15 done; Verifier PASS com ressalvas (cycle 2)  
**User preference (project):** sem commits automáticos salvo pedido explícito.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md`, AD-004/008/010/011/012/013/014, `relatorios-executivos/spec.md` (REL ACs + edge cases).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Dashboard aggregator / port | unit (Mockito) | REL-08, REL-13; paridade `getStats()` inalterado vs refactor; stats por competência scoped/global | `backend/src/test/java/**/dashboard/application/DashboardStatsAggregatorTest.java`, `DashboardServiceTest.java` | `cd backend && mvn test -Dtest=DashboardStatsAggregatorTest,DashboardServiceTest` |
| Beneficio consulta port (ext.) | unit (Mockito) | REL-17…19; resumo/drill-down/matriz; ACL centros null vs scoped | `backend/src/test/java/**/beneficios/application/BeneficioConsultaAdapterTest.java` | `cd backend && mvn test -Dtest=BeneficioConsultaAdapterTest` |
| PDF layout / charts | unit | `%PDF` magic bytes; PNG chart non-empty; branding fallback sem logo | `backend/src/test/java/**/relatorios/application/pdf/*Test.java` | `cd backend && mvn test -Dtest=RelatorioChartImageFactoryTest,RelatorioLayoutHelperTest` |
| PDF renderers | unit | REL-07…15, REL-16…21; strings chave no PDF; competência vazia; moeda pt-BR | `**/relatorios/application/pdf/FolhaExecutivoPdfRendererTest.java`, `BeneficioCustoPdfRendererTest.java` | `cd backend && mvn test -Dtest=FolhaExecutivoPdfRendererTest,BeneficioCustoPdfRendererTest` |
| Relatorio geracao / worker | unit (Mockito) | REL-01…06; status PENDENTE→PROCESSADO/ERRO; idempotência; 429 max jobs; download 409 | `**/relatorios/application/RelatorioGeracaoServiceTest.java`, `RelatorioGeracaoWorkerTest.java` | `cd backend && mvn test -Dtest=RelatorioGeracaoServiceTest,RelatorioGeracaoWorkerTest` |
| Relatorio PDF service | unit (Mockito) | Monta models via ports; delega renderers; sem infra estrangeira | `**/relatorios/application/RelatorioPdfServiceTest.java` | `cd backend && mvn test -Dtest=RelatorioPdfServiceTest` |
| Controllers REST | WebMvc (MockMvc) | REL-01…06; 401/403/400/404/409; download `%PDF`; POST competência futura | `**/relatorios/api/RelatorioFolhaControllerWebMvcTest.java`, `RelatorioBeneficioControllerWebMvcTest.java` | `cd backend && mvn test -Dtest=RelatorioFolhaControllerWebMvcTest,RelatorioBeneficioControllerWebMvcTest` |
| Flyway / entities / config | none | Compile gate | `db/migration/V1.28__*.sql`, entities, `application.yml` | `cd backend && mvn clean compile` |
| ArchUnit relatorios | unit | AD-010 — `relatorios.application` sem infra cross-domain | `**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Frontend hub / components | unit (Vitest) | REL-22…27; MonthPicker payload; estados PENDENTE/PROCESSADO/ERRO; a11y labels | `frontend/src/pages/Relatorios/**/*.test.tsx` | `cd frontend && npm run test -- src/pages/Relatorios` |
| Branding service | unit | REL-31 parcial; logo classpath; cores Techne `#7836FC`/`#3661FC` | `**/relatorios/application/RelatorioBrandingServiceTest.java` | `cd backend && mvn test -Dtest=RelatorioBrandingServiceTest` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após task com unit tests backend | `cd backend && mvn test -Dtest=<ClassTest>` |
| Quick FE | Após task frontend | `cd frontend && npm run test -- <path>` |
| Full | Após última task (T16) + Verifier | `cd backend && mvn test && cd ../frontend && npm run lint && npm run test && npm run build` |
| Build | Migrations / deps-only (T1, T2) | `cd backend && mvn clean compile` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: Schema, branding e ports de dados (4 tasks)

```
T1 → T2 → T3 → T4
```

### Phase 2: Motor PDF (layout, gráficos, renderers) (3 tasks)

```
T5 → T6 → T7
```

### Phase 3: Orquestração, API e arquitetura (5 tasks)

```
T8 → T9 → T10 → T11 → T12
```

### Phase 4: Frontend hub premium (3 tasks)

```
T13 → T14 → T15
```

**Batch packing (~7 tasks/worker, whole phases):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| 1 | Phase 1 + Phase 2 | T1–T7 | 7 |
| 2 | Phase 3 | T8–T12 | 5 |
| 3 | Phase 4 | T13–T15 | 3 |

→ **3 workers** sequenciais (offer-then-confirm no Execute). Total **15 tasks** MVP (REL-01…REL-27). P2/P3 (REL-28…33) fora deste arquivo.

---

## Task Breakdown

### T1: Flyway V1.28 + entities JPA + repositories

**What**: Criar tabelas `relatorio` e `relatorio_arquivo`; entities `Relatorio`, `RelatorioArquivo`; enums `RelatorioTipo`, `RelatorioStatus`; repositories Spring Data.  
**Where**:
- `backend/src/main/resources/db/migration/V1.28__create_relatorio.sql`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/domain/`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/infrastructure/`

**Depends on**: None  
**Reuses**: Padrão soft-delete `ativo`; FK `usuarios(id)`  
**Requirement**: REL-01 (persistência metadados)

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`, `jpa-performance`

**Done when**:
- [x] SQL idempotente conforme design § Data Models
- [x] Unique `(usuario_id, tipo, mes, ano)` enforced
- [x] Entities compilam com Lombok `@Data`
- [x] Gate: `cd backend && mvn clean compile`

**Tests**: none  
**Gate**: build

**Commit**: `feat(relatorios): schema V1.28 relatorio + relatorio_arquivo`

---

### T2: OpenPDF dependency + branding runtime + RelatorioBrandingService

**What**: Adicionar `openpdf:2.0.3`; copiar assets de `_docs/specs/features/relatorios-executivos/branding/logo-techne.png` → `backend/src/main/resources/branding/logo.png`; opcional `favicon.png` e monocromáticos; `@ConfigurationProperties` + `RelatorioBrandingService` com paleta Techne (`#7836FC`, `#3661FC`, `#273340`, `#f8fafc` per branding README).  
**Where**:
- `backend/pom.xml`
- `backend/src/main/resources/branding/` (runtime)
- `backend/src/main/resources/application.yml` (`relatorios.branding.*`, `relatorios.geracao.*`)
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioBrandingService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/BrandingTheme.java`

**Depends on**: T1  
**Reuses**: Assets em `branding/README.md`; fallback wordmark se logo ausente  
**Requirement**: REL-31 (parcial MVP), REL-07 (logo capa)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] OpenPDF 2.0.3 (não 2.1+) no pom
- [ ] `load()` retorna cores Techne do yml
- [ ] Logo carregado do classpath quando presente
- [ ] Unit: logo presente → bytes non-empty; logo ausente → fallback sem exceção
- [ ] Gate: `cd backend && mvn test -Dtest=RelatorioBrandingServiceTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(relatorios): OpenPDF 2.0.3 + branding Techne service`

---

### T3: DashboardStatsAggregator + DashboardConsultaPort

**What**: Extrair agregações de `DashboardService` para `DashboardStatsAggregator`; criar `DashboardConsultaPort` + adapter; adicionar `getStatsForCompetencia(login, inicio, fim, decimoTerceiro)` e `getEvolucaoMeses(..., 6)`; refatorar `getStats()` para delegar ao aggregator via resumo mais recente — **zero mudança de comportamento** do dashboard atual.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/dashboard/application/DashboardStatsAggregator.java`
- `backend/src/main/java/br/com/techne/sistemafolha/dashboard/port/DashboardConsultaPort.java`
- `backend/src/main/java/br/com/techne/sistemafolha/dashboard/application/DashboardConsultaAdapter.java`
- `backend/src/main/java/br/com/techne/sistemafolha/dashboard/application/DashboardService.java` (refactor)

**Depends on**: T1  
**Reuses**: Métodos privados atuais CC/LN/cargo/rubricas/evolução; ports Folha/Benefícios/Organograma  
**Requirement**: REL-08, REL-09, REL-10, REL-11, REL-12, REL-13

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [ ] `getStatsForCompetencia` retorna KPIs para competência passada (não só mais recente)
- [ ] Usuário scoped filtra centros (paridade ACL)
- [ ] `DashboardServiceTest` existentes passam sem alteração de asserts
- [ ] Novos testes: competência específica bate totais esperados; evolução 6 meses
- [ ] Gate: `cd backend && mvn test -Dtest=DashboardStatsAggregatorTest,DashboardServiceTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(dashboard): DashboardConsultaPort stats por competência`

---

### T4: Extensão BeneficioConsultaPort para relatório benefícios

**What**: Adicionar snapshots e métodos `resumoPorTipo`, `topFuncionariosPorTipo`, `matrizCentroCustoPorTipo` no port + adapter + queries repository.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/port/Beneficio*Snapshot.java` (novos records)
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/port/BeneficioConsultaPort.java`
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/application/BeneficioConsultaAdapter.java`
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/infrastructure/BeneficioMensalRepository.java`

**Depends on**: T1  
**Reuses**: Projeções `BeneficioMensalResumoProjection`; filtros por centros  
**Requirement**: REL-17, REL-18, REL-19

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [ ] Resumo por tipo paridade com lógica `BeneficioMensalService.resumoPorCompetencia`
- [ ] Top 10 funcionários por tipo ordenado valor DESC
- [ ] Matriz top 5 CC × top 5 tipos
- [ ] Filtro centros null = global; Set vazio = vazio
- [ ] Gate: `cd backend && mvn test -Dtest=BeneficioConsultaAdapterTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(beneficios): port consulta resumo/drill-down/matriz para relatórios`

---

### T5: RelatorioLayoutHelper + RelatorioChartImageFactory

**What**: Helpers PDF reutilizáveis (tabela zebrada, cabeçalho colorido, rodapé, KPI box) + factory Java2D line/bar charts → PNG bytes.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/pdf/RelatorioLayoutHelper.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/pdf/RelatorioChartImageFactory.java`

**Depends on**: T2  
**Reuses**: `BrandingTheme` de T2  
**Requirement**: REL-12 (gráfico), REL-15 (rodapé helper)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `lineChart` produz PNG > 0 bytes com ≥2 pontos
- [ ] `horizontalBarChart` respeita `maxBars`
- [ ] Layout helper formata moeda pt-BR
- [ ] Gate: `cd backend && mvn test -Dtest=RelatorioChartImageFactoryTest,RelatorioLayoutHelperTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(relatorios): PDF layout helper e chart factory Java2D`

---

### T6: FolhaExecutivoPdfRenderer

**What**: Renderer PDF executivo folha — capa KPI 4 cards, seções CC/LN (top 15 + Outros), top 5 proventos/descontos, gráfico evolução 6m, rodapé paginado, estado sem dados.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/pdf/FolhaExecutivoPdfRenderer.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioFolhaModel.java`

**Depends on**: T3, T5  
**Reuses**: `DashboardStatsDTO`, `EvolucaoMensalDTO`, layout/chart helpers  
**Requirement**: REL-07…REL-15

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] PDF bytes começam com `%PDF`
- [ ] Text extract (test): contém "Relatório Executivo de Folha", competência `MM/yyyy`, "Gerado pelo Sistema de Folha — Techne"
- [ ] Model `semDados=true` → capa + "Sem dados para a competência selecionada"
- [ ] KPI values no model refletidos no PDF (assert substring valores formatados)
- [ ] Gate: `cd backend && mvn test -Dtest=FolhaExecutivoPdfRendererTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(relatorios): PDF executivo de folha premium`

---

### T7: BeneficioCustoPdfRenderer

**What**: Renderer PDF custo benefício + folha — capa KPI, tabela resumo por tipo, drill-down top 10/tipo, matriz CC×tipo, notas sem benefícios/sem folha.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/pdf/BeneficioCustoPdfRenderer.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioBeneficioModel.java`

**Depends on**: T4, T5  
**Reuses**: Beneficio snapshots; layout helper  
**Requirement**: REL-16…REL-21

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] PDF contém "Relatório de Custo — Benefícios e Folha"
- [ ] Tabela tipos com código/descrição/total/qtd
- [ ] `semBeneficios=true` → nota "Nenhum benefício lançado"
- [ ] Valores `R$` formatados pt-BR
- [ ] Gate: `cd backend && mvn test -Dtest=BeneficioCustoPdfRendererTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(relatorios): PDF custo benefício + folha`

---

### T8: RelatorioPdfService

**What**: Monta `RelatorioFolhaModel` / `RelatorioBeneficioModel` consumindo `DashboardConsultaPort`, `BeneficioConsultaPort`, `FolhaTotalizacaoPort`, `OrganogramaAcessoPort`; delega renderers.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioPdfService.java`

**Depends on**: T3, T4, T6, T7  
**Reuses**: Ports only (AD-010)  
**Requirement**: REL-08, REL-16 (KPIs consolidados)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] `renderFolhaExecutivo(relatorioId context)` usa stats da competência do relatório
- [ ] `renderBeneficioCusto` calcula custo consolidado = folha + benefícios ACL
- [ ] Unit mocks ports — verifica delegação aos renderers
- [ ] Gate: `cd backend && mvn test -Dtest=RelatorioPdfServiceTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(relatorios): RelatorioPdfService agrega ports e renderiza`

---

### T9: RelatorioAsyncConfig + RelatorioGeracaoWorker

**What**: `@EnableAsync` config pool `relatorio-*`; worker `@Async processar(relatorioId)` chama `RelatorioPdfService`, persiste BYTEA, atualiza status/totais, captura erros ≤500 chars, valida ≤50 MB.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioAsyncConfig.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioGeracaoWorker.java`

**Depends on**: T1, T8  
**Reuses**: Repositories T1  
**Requirement**: REL-01 (processamento assíncrono), edge PDF >50MB

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] Sucesso → `status=PROCESSADO`, `dataProcessamento` set, blob salvo
- [ ] Falha render → `status=ERRO`, mensagem truncada
- [ ] PDF >50MB → ERRO com mensagem spec
- [ ] Gate: `cd backend && mvn test -Dtest=RelatorioGeracaoWorkerTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(relatorios): async worker geração PDF`

---

### T10: RelatorioGeracaoService + exceções domínio

**What**: Orquestração POST/list/download; validação competência futura; ACL 403; upsert idempotente; max 3 PENDENTE/usuário → 429; wait 60s no POST. Exceções: `RelatorioNotFoundException`, `RelatorioIndisponivelException`, `RelatorioGeracaoLimiteException`, `RelatorioAcessoNegadoException`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/application/RelatorioGeracaoService.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/domain/*Exception.java`
- `backend/src/main/java/br/com/techne/sistemafolha/exception/GlobalExceptionHandler.java` (handlers)

**Depends on**: T9  
**Reuses**: `OrganogramaAcessoPort`, `UsuarioLookupPort`  
**Requirement**: REL-01…06, edge cases spec

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`

**Done when**:
- [ ] Re-gerar mesma tupla substitui registro anterior
- [ ] Download PROCESSADO retorna bytes; PENDENTE/ERRO → exceção 409
- [ ] Competência futura → IllegalArgumentException/validation 400
- [ ] Gate: `cd backend && mvn test -Dtest=RelatorioGeracaoServiceTest`

**Tests**: unit  
**Gate**: quick

**Commit**: `feat(relatorios): RelatorioGeracaoService orquestração e ACL`

---

### T11: Controllers REST + DTOs + SecurityConfig

**What**: `RelatorioFolhaController` e `RelatorioBeneficioController` espelhando contrato `relatorioService.ts`; DTO records; registrar `/relatorios/**` authenticated em `SecurityConfig`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/api/RelatorioFolhaController.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/api/RelatorioBeneficioController.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/api/RelatorioFolhaDTO.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/api/RelatorioBeneficioDTO.java`
- `backend/src/main/java/br/com/techne/sistemafolha/relatorios/api/GerarRelatorioRequest.java`
- `backend/src/main/java/br/com/techne/sistemafolha/config/SecurityConfig.java`

**Depends on**: T10  
**Reuses**: Padrão `BeneficioMensalController`; `@Tag` OpenAPI  
**Requirement**: REL-01…06

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint`, `spring-security`

**Done when**:
- [ ] Endpoints compilam e delegam ao service
- [ ] Bean Validation em `{ mes, ano }`
- [ ] POST bloqueado para API Key (write guard existente AD-013)
- [ ] Gate: compile + prepara T12

**Tests**: none (WebMvc em T12)  
**Gate**: build

**Commit**: `feat(relatorios): REST controllers folha e benefício`

---

### T12: WebMvc tests + ArchUnit relatorios

**What**: `RelatorioFolhaControllerWebMvcTest` e `RelatorioBeneficioControllerWebMvcTest` (401, 400 futura, 409 download, 200 PDF magic); regra ArchUnit `relatorios_application_must_not_access_foreign_infrastructure`.  
**Where**:
- `backend/src/test/java/br/com/techne/sistemafolha/relatorios/api/RelatorioFolhaControllerWebMvcTest.java`
- `backend/src/test/java/br/com/techne/sistemafolha/relatorios/api/RelatorioBeneficioControllerWebMvcTest.java`
- `backend/src/test/java/br/com/techne/sistemafolha/arch/ModularArchitectureTest.java`

**Depends on**: T11  
**Reuses**: Padrão `BeneficioMensalControllerWebMvcTest`  
**Requirement**: REL-01…06, AD-010

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] MockMvc: POST gera → 200 com DTO; GET download PROCESSADO → `%PDF`
- [ ] GET download PENDENTE → 409
- [ ] Sem auth → 403
- [ ] ArchUnit passa com domínio relatorios
- [ ] Gate: `cd backend && mvn test -Dtest=RelatorioFolhaControllerWebMvcTest,RelatorioBeneficioControllerWebMvcTest,ModularArchitectureTest`

**Tests**: WebMvc + ArchUnit  
**Gate**: quick

**Commit**: `test(relatorios): WebMvc controllers e ArchUnit boundary`

---

### T13: Componentes FE — CompetenciaPicker, RelatorioCatalogCard, RelatorioStatusBadge

**What**: Extrair subcomponentes colocalizados em `pages/Relatorios/`; MonthPicker mês/ano; badge status semântico; card catálogo com ícone/descrição.  
**Where**:
- `frontend/src/pages/Relatorios/CompetenciaPicker.tsx`
- `frontend/src/pages/Relatorios/RelatorioCatalogCard.tsx`
- `frontend/src/pages/Relatorios/RelatorioStatusBadge.tsx`
- `frontend/src/pages/Relatorios/CompetenciaPicker.test.tsx` (etc.)

**Depends on**: T12 (API disponível para integração manual; tests mockam service)  
**Reuses**: MUI v7 DatePicker; Dashboard card spacing; `formatMoneyDisplay`  
**Requirement**: REL-22 (parcial), REL-23, REL-27

**Tools**:
- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:
- [ ] CompetenciaPicker emite `{ mes, ano }` ao selecionar
- [ ] Card tem `aria-label` nas ações
- [ ] Badge renderiza PENDENTE/PROCESSADO/ERRO
- [ ] Gate: `cd frontend && npm run test -- src/pages/Relatorios/CompetenciaPicker.test.tsx`

**Tests**: unit  
**Gate**: quick FE

**Commit**: `feat(relatorios): componentes hub catálogo e competência`

---

### T14: Hub Relatorios — redesign + polling

**What**: Reescrever `Relatorios/index.tsx` com 2 cards catálogo, seletor competência global, polling 2s quando PENDENTE, estados ERRO com retry, ícone PDF (REL-25 fallback).  
**Where**:
- `frontend/src/pages/Relatorios/index.tsx`
- `frontend/src/services/relatorioService.ts` (ajustes mínimos se necessário)

**Depends on**: T13  
**Reuses**: `relatorioService`; Notification hook  
**Requirement**: REL-22…REL-26

**Tools**:
- MCP: NONE
- Skill: `component-architecture`, `testing-a11y`

**Done when**:
- [ ] Gerar usa competência do picker (não hardcoded mês corrente)
- [ ] PENDENTE desabilita re-geração + polling
- [ ] PROCESSADO habilita download
- [ ] Gate: `cd frontend && npm run test -- src/pages/Relatorios/Relatorios.test.tsx`

**Tests**: unit  
**Gate**: quick FE

**Commit**: `feat(relatorios): hub premium com polling e download`

---

### T15: Fechamento — AD-015 STATE + full gate

**What**: Registrar **AD-015** em `_docs/specs/STATE.md` (OpenPDF server-side + `DashboardConsultaPort`); atualizar traceability spec REL-01…27 → Verified pending Verifier; rodar full gate.  
**Where**:
- `_docs/specs/STATE.md`
- `_docs/specs/features/relatorios-executivos/spec.md` (status IDs)

**Depends on**: T14  
**Reuses**: `memory.md` formato AD-NNN  
**Requirement**: AD-015 (design decision)

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [ ] AD-015 appended em STATE.md Decisions
- [ ] Handoff atualizado com feature `relatorios-executivos` Execute done pending Verifier
- [ ] Full gate PASS: `cd backend && mvn test && cd ../frontend && npm run lint && npm run test && npm run build`
- [ ] Test counts: backend ≥ baseline; frontend Relatorios tests pass

**Tests**: none (gate only)  
**Gate**: full

**Commit**: `docs(relatorios): AD-015 + handoff relatorios-executivos MVP`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4
Phase 2:  T5 ──→ T6 ──→ T7
Phase 3:  T8 ──→ T9 ──→ T10 ──→ T11 ──→ T12
Phase 4:  T13 ──→ T14 ──→ T15
```

Execution is strictly sequential — one task at a time, in order.

**Sub-agent batches (Execute):**

```
Batch 1: T1→T7  (Phase 1 + Phase 2)
Batch 2: T8→T12 (Phase 3)
Batch 3: T13→T15 (Phase 4)
```

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: Flyway + entities | 1 migration + 2 entities + repos | ✅ Granular |
| T2: Branding + OpenPDF | 1 dep + 1 service + assets | ✅ Granular |
| T3: Dashboard port | 1 aggregator + 1 port + refactor | ✅ Granular |
| T4: Beneficio port ext | 1 port extension + queries | ✅ Granular |
| T5: Layout + charts | 2 helpers | ✅ Cohesive |
| T6: Folha PDF renderer | 1 renderer | ✅ Granular |
| T7: Beneficio PDF renderer | 1 renderer | ✅ Granular |
| T8: PdfService | 1 orchestrator | ✅ Granular |
| T9: Async worker | config + worker | ✅ Cohesive |
| T10: GeracaoService | 1 service + exceptions | ✅ Granular |
| T11: Controllers | 2 controllers + DTOs + security | ⚠️ 2 controllers — OK (mesmo domínio/api) |
| T12: WebMvc + ArchUnit | tests + 1 rule | ✅ Granular |
| T13: FE components | 3 components + tests | ✅ Granular |
| T14: Hub page | 1 page rewrite | ✅ Granular |
| T15: Docs + full gate | STATE + gate | ✅ Granular |

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ----------------- | ------------- | ------ |
| T1 | None | (start) | ✅ |
| T2 | T1 | T1→T2 | ✅ |
| T3 | T1 | T1→T3 | ✅ |
| T4 | T1 | T1→T4 | ✅ |
| T5 | T2 | T2→T5 | ✅ |
| T6 | T3, T5 | T3,T5→T6 | ✅ |
| T7 | T4, T5 | T4,T5→T7 | ✅ |
| T8 | T3, T4, T6, T7 | T6,T7→T8 | ✅ |
| T9 | T1, T8 | T8→T9 | ✅ |
| T10 | T9 | T9→T10 | ✅ |
| T11 | T10 | T10→T11 | ✅ |
| T12 | T11 | T11→T12 | ✅ |
| T13 | T12 | T12→T13 | ✅ |
| T14 | T13 | T13→T14 | ✅ |
| T15 | T14 | T14→T15 | ✅ |

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | Flyway/entities | none | none | ✅ OK |
| T2 | Branding service | unit | unit | ✅ OK |
| T3 | Dashboard aggregator | unit | unit | ✅ OK |
| T4 | Beneficio port | unit | unit | ✅ OK |
| T5 | PDF helpers | unit | unit | ✅ OK |
| T6 | Folha renderer | unit | unit | ✅ OK |
| T7 | Beneficio renderer | unit | unit | ✅ OK |
| T8 | PdfService | unit | unit | ✅ OK |
| T9 | Worker | unit | unit | ✅ OK |
| T10 | GeracaoService | unit | unit | ✅ OK |
| T11 | Controllers | WebMvc | none → T12 | ✅ OK (T12 merges wiring + WebMvc) |
| T12 | Controllers + ArchUnit | WebMvc + unit | WebMvc + ArchUnit | ✅ OK |
| T13 | FE components | unit | unit | ✅ OK |
| T14 | FE page | unit | unit | ✅ OK |
| T15 | Docs | none | none | ✅ OK |

---

## Requirement Traceability (MVP tasks)

| Requirement | Task(s) |
| ----------- | ------- |
| REL-01…06 | T9, T10, T11, T12 |
| REL-07…15 | T3, T5, T6, T8 |
| REL-16…21 | T4, T7, T8 |
| REL-22…27 | T13, T14 |
| REL-28…33 | **Deferred** (P2/P3 — fora deste tasks.md) |

---

## MCPs & Skills (Execute)

| Task | MCPs | Skills |
| ---- | ---- | ------ |
| T1 | — | `flyway-migration-writer`, `jpa-performance` |
| T2 | — | — |
| T3 | — | `jpa-performance` |
| T4 | — | `jpa-performance` |
| T5–T8 | — | — |
| T9–T12 | — | `spring-boot-new-endpoint`, `spring-security` |
| T13–T14 | — | `component-architecture`, `testing-a11y` |
| T15 | — | — |

**Branding note:** T2 MUST copy `logo-techne.png` from `_docs/specs/features/relatorios-executivos/branding/` and apply colors from `branding/README.md` (`#7836FC` / `#3661FC`), superseding spec defaults `#1976d2` / `#dc004e`.

---

**Próximo passo:** Aprovar tasks → **Execute** (3 batches / offer sub-agents).
