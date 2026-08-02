# Cobertura de Testes 95% Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/cobertura-testes-95/design.md`
**Status**: Draft

**Nota de tooling (padrão para todas as tasks):** MCP `context7` disponível para consultar API de JaCoCo/Vitest/Testing Library quando necessário; Skill `NONE`. Ferramentas nativas Bash/Read/Edit/Write. A decisão de sub-agentes por batch vs inline é da fase Execute (o volume ~22 tasks dispara a oferta de sub-agentes).

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `sonar-project.properties`, `diversos/scripts/check-jacoco-thresholds.sh`, AD-014 (STATE.md). Meta desta feature (AD-014/spec): linha ≥ 95% E branch ≥ 95% em código escrito à mão (Lombok gerado excluído via `@lombok.Generated`).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Backend service (`*.application`) | unit (Mockito) | Todos os branches escritos à mão ≥ 95%; 1:1 com ACs; todo edge case listado testado | `backend/src/test/java/**/application/*Test.java` | `cd backend && mvn test -Dtest=<Classe>` |
| Backend controller (`*.api`) | integration (WebMvc slice) | Toda rota em escopo: happy + edge + error (401/403/404/400) ≥ 95% linha/branch | `backend/src/test/java/**/api/*WebMvcTest.java` | `cd backend && mvn test -Dtest=<Classe>` |
| Backend domain (exceções/regras à mão) | unit | Branches de lógica à mão ≥ 95% (getters/equals Lombok excluídos) | `backend/src/test/java/**/domain/*Test.java` | `cd backend && mvn test -Dtest=<Classe>` |
| Backend ADP import | integration (Testcontainers, Docker-gated) | Mantido `@EnabledIf`; cobertura N/A quando Docker down (A8) | `importacao/application/ImportacaoFolhaAdpIntegrationTest.java` | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` |
| Backend entity/DTO (Lombok `@Data`) | none | Excluído via `@lombok.Generated` (AD-014) — build gate only | `*/domain/*.java`, `*/api/*DTO.java` | build gate |
| Frontend page (`pages/**`) | unit (Vitest + Testing Library + MSW) | Linha ≥ 95% E branch ≥ 95%: happy + loading/erro/vazio + permissões + validação de form | `frontend/src/pages/**/*.test.tsx` | `cd frontend && npm test -- <pattern>` |
| Frontend service/util/context/hook | unit (Vitest) | Linha ≥ 95% E branch ≥ 95%; caminhos de erro | `frontend/src/{services,utils,contexts,hooks}/**/*.test.ts(x)` | `cd frontend && npm test -- <pattern>` |
| Gate script | none (self-check) | Script detecta corretamente estado <95% (exit 1) e ≥95% (exit 0); imprime as 4 métricas | `diversos/scripts/check-coverage-95.sh` | `bash diversos/scripts/check-coverage-95.sh` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick BE | Após task de service/domain unit | `cd backend && mvn test -Dtest=<Classe(s)>` |
| Quick FE | Após task de página/service FE | `cd frontend && npm test -- <pattern>` |
| Coverage BE | Fechar fase backend | `cd backend && mvn test` → parse `target/site/jacoco/jacoco.xml` |
| Coverage FE | Fechar fase frontend | `cd frontend && npm run test:coverage` → `coverage/coverage-summary.json` |
| Full (95 gate) | Fim de fase / Verifier | `cd backend && mvn test && cd ../frontend && npm run test:coverage && cd .. && bash diversos/scripts/check-coverage-95.sh` |
| Build FE | Após config/entity-only | `cd frontend && npm run lint && npm run build` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 0: Infra (gate + config)

```
T1 → T2
```

### Phase 1: Backend services (`*.application`) → 95%

```
T3 → T4 → T5 → T6 → T7 → T8 → T9
```

### Phase 2: Backend controllers (`*.api`) + domínio residual → 95%

```
T10 → T11 → T12 → T13 → T14
```

### Phase 3: Frontend páginas + resíduos → 95% (linha + branch)

```
T15 → T16 → T17 → T18 → T19 → T20
```

### Phase 4: Fechamento e docs

```
T21 → T22
```

---

## Task Breakdown

### T1: Estabelecer `backend/lombok.config` e alinhar gate antigo

**What**: Consolidar `backend/lombok.config` (já criado no Design) como fonte de exclusão de código gerado e marcar o gate antigo de 75% como superseded.
**Where**: `backend/lombok.config` (verificar), `diversos/scripts/check-jacoco-thresholds.sh` (deprecar/apontar p/ novo), `sonar-project.properties` (comentar threshold antigo se houver)
**Depends on**: None
**Reuses**: Filtragem automática `@lombok.Generated` do JaCoCo 0.8.12
**Requirement**: COV-13 (infra da meta AD-014)

**Tools**: MCP: NONE • Skill: NONE

**Done when**:

- [x] `backend/lombok.config` contém `lombok.addLombokGeneratedAnnotation = true`
- [x] `cd backend && mvn clean test` recompila e JaCoCo reporta BRANCH ≈ 68.7% (baseline pós-config, confirmando exclusão do gerado)
- [x] Referência ao gate antigo (75%) marcada como superseded por AD-014
- [x] Test count backend: ≥ 474 (nenhum removido)

**Tests**: none (config — build gate)
**Gate**: build (`cd backend && mvn clean test`)

---

### T2: Criar gate `check-coverage-95.sh`

**What**: Script que lê JaCoCo XML (LINE, BRANCH) e Vitest `coverage-summary.json` (lines, branches), compara as 4 métricas a 95% e falha com mensagem clara.
**Where**: `diversos/scripts/check-coverage-95.sh`; `frontend/vite.config.ts` (adicionar reporter `json-summary`)
**Depends on**: T1
**Reuses**: Lógica de parse validada na medição de baseline; padrão dos scripts em `diversos/scripts/`
**Requirement**: COV-13, COV-14, COV-15

**Tools**: MCP: NONE • Skill: NONE

**Done when**:

- [x] Adicionado reporter `json-summary` ao Vitest; `npm run test:coverage` gera `frontend/coverage/coverage-summary.json`
- [x] Script imprime tabela das 4 métricas (BE linha/branch, FE linha/branch) — COV-15
- [x] Com baseline atual (< 95%), script sai com código != 0 e lista as métricas reprovadas com valor medido — COV-14
- [x] Com `--threshold 0`, script sai 0 (auto-teste do caminho de sucesso)
- [x] Falha clara se relatório ausente ("rode mvn test / npm run test:coverage antes")

**Tests**: none (script self-check por execução)
**Gate**: quick (`bash diversos/scripts/check-coverage-95.sh` + `bash ... --threshold 0`)

---

### T3: Cobrir `folha/application` — motor e cálculo → 95%

**What**: Expandir testes de branch/linha dos serviços de cálculo da folha.
**Where**: `backend/src/test/java/**/folha/application/` — `FolhaMotorCalculoTest`, `FolhaCustoEmpresaComposerTest`, `EncargosRateioServiceTest`, `FolhaLinhaAgregacaoTest`, `FolhaProcessamentoServiceTest`
**Depends on**: T2
**Reuses**: Padrão `*ServiceTest` (Mockito); mensagens PT existentes
**Requirement**: COV-01, COV-02, COV-03, COV-07, COV-08

**Tools**: MCP: `context7` (se precisar API) • Skill: NONE

**Done when**:

- [x] Branches escritos à mão dos 5 alvos cobertos (cada ramo de custo/encargo/agregação)
- [x] `mvn test -Dtest=FolhaMotorCalculoTest,FolhaCustoEmpresaComposerTest,EncargosRateioServiceTest,FolhaLinhaAgregacaoTest,FolhaProcessamentoServiceTest` passa
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick

---

### T4: Cobrir `folha/application` — totalização, consulta e resumo → 95%

**What**: Expandir testes dos serviços de totalização/consulta/resumo e adapters da folha.
**Where**: `FolhaTotalizacaoServiceTest`, `ResumoFolhaPagamentoServiceTest`, `FolhaFichaConsultaServiceTest`, `FolhaPagamentoServiceTest`, `FolhaConsultaAdapterTest`, `FolhaTotalizacaoAdapterTest`, `FolhaProcessamentoAdapterTest`, `FolhaImportacaoAdapterTest`
**Depends on**: T3
**Reuses**: Padrão `*ServiceTest`/`*AdapterTest`
**Requirement**: COV-01, COV-02, COV-03, COV-07, COV-08

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Branches à mão dos serviços/adapters de totalização/resumo cobertos
- [x] `mvn test -Dtest=FolhaTotalizacaoServiceTest,ResumoFolhaPagamentoServiceTest,FolhaFichaConsultaServiceTest,FolhaPagamentoServiceTest,FolhaConsultaAdapterTest,FolhaTotalizacaoAdapterTest,FolhaProcessamentoAdapterTest,FolhaImportacaoAdapterTest` passa
- [x] `folha/application` (jacoco) ≥ 95% linha e branch
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick

---

### T5: Cobrir `beneficios/application` → 95%

**What**: Expandir testes de branch/linha dos serviços de benefícios.
**Where**: `BeneficioMensalServiceTest`, `ImportacaoBeneficioMensalServiceTest`, `TipoBeneficioServiceTest`, `BeneficioConsultaAdapterTest`
**Depends on**: T4
**Reuses**: Padrão `*ServiceTest`
**Requirement**: COV-01, COV-02, COV-03, COV-07, COV-08

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Branches à mão dos 4 alvos cobertos (validações de importação, rejeição, tipos)
- [x] `mvn test -Dtest=BeneficioMensalServiceTest,ImportacaoBeneficioMensalServiceTest,TipoBeneficioServiceTest,BeneficioConsultaAdapterTest` passa
- [x] `beneficios/application` ≥ 95% linha e branch
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick

---

### T6: Cobrir `cadastros/application` → 95%

**What**: Expandir testes dos serviços de cadastros (cargo, centro custo, funcionário, rubrica fixa, linha negócio, rubrica) e adapters.
**Where**: `CargoServiceTest`, `CentroCustoServiceTest`, `FuncionarioServiceTest`, `FuncionarioRubricaFixaServiceTest`, `LinhaNegocioServiceTest`, `RubricaServiceTest`, `CadastrosImportLookupAdapterTest`, `CadastrosLookupAdapterTest`, `FuncionarioConsultaAdapterTest`
**Depends on**: T5
**Reuses**: Padrão `*ServiceTest`
**Requirement**: COV-01, COV-02, COV-03, COV-07, COV-08

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Branches à mão cobertos (vigência de rubrica fixa, conflitos, not-found)
- [x] `mvn test -Dtest=CargoServiceTest,CentroCustoServiceTest,FuncionarioServiceTest,FuncionarioRubricaFixaServiceTest,LinhaNegocioServiceTest,RubricaServiceTest,CadastrosImportLookupAdapterTest,CadastrosLookupAdapterTest,FuncionarioConsultaAdapterTest` passa
- [x] `cadastros/application` ≥ 95% linha e branch
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick

---

### T7: Cobrir `importacao/application` (ADP) → 95%

**What**: Expandir `ImportacaoFolhaAdpServiceTest` cobrindo os ramos de parsing/validação do serviço de alta complexidade (CC 71) sem refatorá-lo.
**Where**: `ImportacaoFolhaAdpServiceTest` (unit); `ImportacaoFolhaAdpIntegrationTest` mantido Docker-gated
**Depends on**: T6
**Reuses**: `@ParameterizedTest`/`@MethodSource` por ramo; fixtures XLSX existentes
**Requirement**: COV-01, COV-02, COV-03, COV-07, COV-08, COV-09 (branches inatingíveis documentados)

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Cada ramo de validação/erro de `ImportacaoFolhaAdpService` coberto por caso parametrizado
- [x] Branches comprovadamente inatingíveis documentados em `validation.md` (COV-09) — não contornados
- [x] `mvn test -Dtest=ImportacaoFolhaAdpServiceTest` passa
- [x] `importacao/application` ≥ 95% linha e branch (excluindo path Docker-gated, A8)
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: unit (+ integration Docker-gated inalterado)
**Gate**: quick

---

### T8: Cobrir `auth/application` → 95%

**What**: Expandir testes dos serviços de auth (autenticação, refresh, api key, cleanup, usuário, lookup).
**Where**: `AuthenticationServiceTest`, `AuthenticationServiceAcessoTest`, `RefreshTokenServiceTest`, `ApiKeyServiceTest`, `TokenCleanupServiceTest`, `UsuarioServiceTest`, `UsuarioLookupAdapterTest`
**Depends on**: T7
**Reuses**: Padrão `*ServiceTest`; casos de token expirado/revogado existentes
**Requirement**: COV-01, COV-02, COV-03, COV-07, COV-08

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Branches à mão cobertos (login inválido, refresh expirado/revogado, key expirada, permissões)
- [x] `mvn test -Dtest=AuthenticationServiceTest,AuthenticationServiceAcessoTest,RefreshTokenServiceTest,ApiKeyServiceTest,TokenCleanupServiceTest,UsuarioServiceTest,UsuarioLookupAdapterTest` passa
- [x] `auth/application` ≥ 95% linha e branch
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick

---

### T9: Cobrir `organograma/application` + `organograma/acesso` + `dashboard/application` → 95%

**What**: Expandir testes dos serviços de organograma, ACL de acesso e dashboard.
**Where**: `OrganogramaServiceTest`, `OrganogramaServicePortWiringTest`, `OrganogramaAcessoServiceTest`, `DashboardServiceTest`
**Depends on**: T8
**Reuses**: Padrão `*ServiceTest`; cenários ACL (SEM_FUNCIONARIO, SEM_NO, ACESSO_TOTAL) já presentes
**Requirement**: COV-01, COV-02, COV-03, COV-07, COV-08

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Branches à mão cobertos (mover/remover nó, soft delete, ramos de ACL, agregação dashboard)
- [x] `mvn test -Dtest=OrganogramaServiceTest,OrganogramaServicePortWiringTest,OrganogramaAcessoServiceTest,DashboardServiceTest` passa
- [x] `organograma/application`, `organograma/acesso/application`, `dashboard/application` ≥ 95% linha e branch
- [x] `cd backend && mvn test` → verificar tendência agregada de branch subindo
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: coverage BE (`cd backend && mvn test`)

---

### T10: Cobrir controllers `cadastros/api` → 95%

**What**: Criar/expandir WebMvc tests dos 6 controllers de cadastros (hoje ~32% linha, 82 linhas sem teste).
**Where**: `backend/src/test/java/**/cadastros/api/` — `CargoControllerWebMvcTest`, `CentroCustoControllerWebMvcTest`, `FuncionarioControllerWebMvcTest`, `FuncionarioRubricaFixaControllerWebMvcTest`, `LinhaNegocioControllerWebMvcTest`, `RubricaControllerWebMvcTest`
**Depends on**: T9
**Reuses**: Padrão `BeneficioMensalControllerWebMvcTest` / `FuncionarioAclWebMvcTest` (`@WebMvcTest` slice)
**Requirement**: COV-01, COV-02, COV-07

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Cada rota: happy path + 400 (validação) + 401/403 (auth/ACL) + 404 coberta
- [x] `mvn test -Dtest=CargoControllerWebMvcTest,CentroCustoControllerWebMvcTest,FuncionarioControllerWebMvcTest,FuncionarioRubricaFixaControllerWebMvcTest,LinhaNegocioControllerWebMvcTest,RubricaControllerWebMvcTest` passa
- [x] `cadastros/api` ≥ 95% linha e branch
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: integration (WebMvc)
**Gate**: quick

---

### T11: Cobrir controllers `folha/api` → 95%

**What**: Criar/expandir WebMvc tests dos controllers de folha (ficha, pagamento, processamento, resumo).
**Where**: `FolhaFichaControllerWebMvcTest`, `FolhaPagamentoControllerWebMvcTest`, `FolhaProcessamentoControllerWebMvcTest`, `ResumoFolhaPagamentoControllerWebMvcTest`
**Depends on**: T10
**Reuses**: Padrão `*WebMvcTest`; `ApiKeyAclWebMvcTest` para shape de ACL
**Requirement**: COV-01, COV-02, COV-07

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Cada rota: happy + edge + error (400/401/403/404) coberta
- [x] `mvn test -Dtest=FolhaFichaControllerWebMvcTest,FolhaPagamentoControllerWebMvcTest,FolhaProcessamentoControllerWebMvcTest,ResumoFolhaPagamentoControllerWebMvcTest` passa
- [x] `folha/api` ≥ 95% linha e branch
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: integration (WebMvc)
**Gate**: quick

---

### T12: Cobrir controllers `auth/api` → 95%

**What**: Criar/expandir WebMvc tests de `AuthController` e `UsuarioController` (login, refresh, logout, CRUD usuário).
**Where**: `AuthControllerWebMvcTest`, `UsuarioControllerWebMvcTest` (complementa `UsuarioAclWebMvcTest`)
**Depends on**: T11
**Reuses**: `ApiKeyControllerWebMvcTest`, `UsuarioAclWebMvcTest`
**Requirement**: COV-01, COV-02, COV-07

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Rotas auth: login ok/inválido, refresh ok/expirado, logout; usuário CRUD + ACL
- [x] `mvn test -Dtest=AuthControllerWebMvcTest,UsuarioControllerWebMvcTest,UsuarioAclWebMvcTest` passa
- [x] `auth/api` ≥ 95% linha e branch
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: integration (WebMvc)
**Gate**: quick

---

### T13: Cobrir controllers `beneficios/api` → 95%

**What**: Criar/expandir WebMvc tests de `TipoBeneficioController` e `ImportacaoBeneficioMensalController` (complementa `BeneficioMensalControllerWebMvcTest`).
**Where**: `TipoBeneficioControllerWebMvcTest`, `ImportacaoBeneficioMensalControllerWebMvcTest`
**Depends on**: T12
**Reuses**: `BeneficioMensalControllerWebMvcTest`
**Requirement**: COV-01, COV-02, COV-07

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Rotas: happy + validação + auth/ACL + importação rejeitada
- [x] `mvn test -Dtest=TipoBeneficioControllerWebMvcTest,ImportacaoBeneficioMensalControllerWebMvcTest,BeneficioMensalControllerWebMvcTest` passa
- [x] `beneficios/api` ≥ 95% linha e branch
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: integration (WebMvc)
**Gate**: quick

---

### T14: Cobrir `organograma/api`, `importacao/api`, `dashboard/api` + domínio residual → 95%

**What**: Aprofundar WebMvc de organograma (17% linha), importação ADP e dashboard controllers, e cobrir branches à mão remanescentes de domínio (exceções com lógica).
**Where**: `OrganogramaControllerWebMvcTest` (aprofundar), `ImportacaoFolhaAdpControllerWebMvcTest` (aprofundar), `DashboardControllerWebMvcTest`, testes de domínio residual (`**/domain/*Test.java`)
**Depends on**: T13
**Reuses**: `OrganogramaControllerWebMvcTest`, `ImportacaoFolhaAdpControllerWebMvcTest` existentes
**Requirement**: COV-01, COV-02, COV-07, COV-09

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Rotas organograma/importacao/dashboard: happy + edge + error cobertas
- [x] Branches à mão de domínio (organograma/domain 10 br) cobertos ou documentados como inatingíveis (COV-09)
- [x] `cd backend && mvn test` passa; JaCoCo BE ≥ 95% linha E branch (agregado)
- [x] `bash diversos/scripts/check-coverage-95.sh` reprova só nas métricas FE (BE verde)
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: integration (WebMvc) + unit (domain)
**Gate**: coverage BE + full gate parcial

---

### T15: Cobrir `pages/Funcionarios` → 95% (linha + branch)

**What**: Expandir `Funcionarios.test.tsx` (hoje 35% linha, 5.37% branch — pior do FE).
**Where**: `frontend/src/pages/Funcionarios/Funcionarios.test.tsx`
**Depends on**: T14
**Reuses**: MSW por arquivo, Testing Library, `testProviders.tsx`
**Requirement**: COV-04, COV-05, COV-06, COV-10, COV-11, COV-12

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Cenários: listagem, filtros, criação/edição, validação de form, estados loading/erro/vazio, permissões condicionais
- [x] `npm test -- Funcionarios` passa
- [x] `pages/Funcionarios` ≥ 95% linha e branch (via `npm run test:coverage`)
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick FE

---

### T16: Cobrir `pages/Importacao` → 95% (linha + branch)

**What**: Expandir `Importacao.test.tsx` (35.67% linha, 22.44% branch).
**Where**: `frontend/src/pages/Importacao/Importacao.test.tsx`
**Depends on**: T15
**Reuses**: MSW, Testing Library
**Requirement**: COV-04, COV-05, COV-06, COV-10, COV-11, COV-12

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [x] Cenários: upload, seleção competência, sucesso/erro/rejeição de importação, estados de progresso
- [x] `npm test -- Importacao` passa
- [x] `pages/Importacao` ≥ 95% linha e branch
- [x] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick FE

---

### T17: Cobrir `pages/Organograma` + `components/OrganogramaGrafico` → 95%

**What**: Expandir `Organograma.test.tsx` (42.56% linha) e `OrganogramaGrafico.test.tsx` (67.18% linha, 19.44% branch).
**Where**: `frontend/src/pages/Organograma/Organograma.test.tsx`, `frontend/src/components/OrganogramaGrafico/OrganogramaGrafico.test.tsx`
**Depends on**: T16
**Reuses**: MSW, Testing Library
**Requirement**: COV-04, COV-05, COV-06, COV-10, COV-11, COV-12

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [ ] Cenários: árvore, mover/associar nó, edição, render do gráfico, estados de erro/vazio
- [ ] `npm test -- Organograma OrganogramaGrafico` passa
- [ ] `pages/Organograma` e `components/OrganogramaGrafico` ≥ 95% linha e branch
- [ ] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick FE

---

### T18: Cobrir `pages/Usuarios` + `pages/ApiKeys` → 95%

**What**: Expandir `Usuarios.test.tsx` (44.66% linha) e `ApiKeys.test.tsx` (42.22% linha).
**Where**: `frontend/src/pages/Usuarios/Usuarios.test.tsx`, `frontend/src/pages/ApiKeys/ApiKeys.test.tsx`
**Depends on**: T17
**Reuses**: MSW, Testing Library
**Requirement**: COV-04, COV-05, COV-06, COV-10, COV-11, COV-12

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [ ] Cenários: CRUD usuário, permissões, criação/revogação de API key, validação, erros
- [ ] `npm test -- Usuarios ApiKeys` passa
- [ ] `pages/Usuarios` e `pages/ApiKeys` ≥ 95% linha e branch
- [ ] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick FE

---

### T19: Cobrir `pages/Relatorios` + `pages/Dashboard` → 95%

**What**: Expandir `Relatorios.test.tsx` (37.73% linha) e `Dashboard.test.tsx` (68.18% linha, 23.33% branch).
**Where**: `frontend/src/pages/Relatorios/Relatorios.test.tsx`, `frontend/src/pages/Dashboard/Dashboard.test.tsx`
**Depends on**: T18
**Reuses**: MSW, Testing Library
**Requirement**: COV-04, COV-05, COV-06, COV-10, COV-11, COV-12

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [ ] Cenários: geração de relatório, filtros, cards do dashboard, estados loading/erro/vazio, ramos de permissão
- [ ] `npm test -- Relatorios Dashboard` passa
- [ ] `pages/Relatorios` e `pages/Dashboard` ≥ 95% linha e branch
- [ ] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: quick FE

---

### T20: Cobrir resíduos FE (services, utils, contexts, hooks, routes) → 95%

**What**: Fechar arquivos FE não-página abaixo de 95%: `utils/permissions.ts` (83% linha), `contexts/AuthContext.tsx` (92% linha), `hooks/useNotification` (71% linha), `test/testProviders.tsx`, e branches residuais de services (`usuarioService` 50% branch, `rubricaService` 87.5% branch, `api.ts` linha 112).
**Where**: `frontend/src/utils/permissions.ts` test, `frontend/src/contexts/AuthContext.test.tsx`, `frontend/src/hooks/*`, `frontend/src/services/*.test.ts`
**Depends on**: T19
**Reuses**: Testes existentes de service/context
**Requirement**: COV-04, COV-05, COV-06, COV-10, COV-11, COV-12

**Tools**: MCP: `context7` • Skill: NONE

**Done when**:

- [ ] Branches/linhas residuais cobertos nos arquivos não-página
- [ ] `npm run test:coverage` → FE ≥ 95% linha E branch em "All files"
- [ ] Nenhum teste existente removido/enfraquecido

**Tests**: unit
**Gate**: coverage FE (`npm run test:coverage`)

---

### T21: Fechamento — gate completo das 4 métricas ≥ 95%

**What**: Rodar o gate completo, identificar e fechar qualquer resíduo remanescente até as 4 métricas passarem.
**Where**: qualquer arquivo de teste com gap residual (BE ou FE)
**Depends on**: T20
**Reuses**: gate `check-coverage-95.sh`
**Requirement**: COV-13, COV-14, COV-01, COV-04, COV-07, COV-10

**Tools**: MCP: NONE • Skill: NONE

**Done when**:

- [ ] `cd backend && mvn test` → JaCoCo LINE ≥ 95% E BRANCH ≥ 95%
- [ ] `cd frontend && npm run test:coverage` → Lines ≥ 95% E Branches ≥ 95%
- [ ] `bash diversos/scripts/check-coverage-95.sh` sai com código 0 e imprime as 4 métricas ≥ 95%
- [ ] Branches inatingíveis documentados em `validation.md` (COV-09)
- [ ] Test count: ≥ 474 BE / ≥ 184 FE (nenhum removido)

**Tests**: none (agregação)
**Gate**: full (95 gate)

---

### T22: Sincronizar docs (`TESTING.md`, `CONCERNS.md`)

**What**: Atualizar baseline, thresholds e matriz em `TESTING.md`; registrar fechamento em `CONCERNS.md`.
**Where**: `_docs/specs/TESTING.md`, `_docs/specs/CONCERNS.md`
**Depends on**: T21
**Reuses**: números finais do gate T21
**Requirement**: (success criteria do spec — docs sync)

**Tools**: MCP: NONE • Skill: NONE

**Done when**:

- [ ] `TESTING.md` "Coverage Targets" reflete 95% linha+branch (BE+FE) e AD-014 (Lombok excluído); remove/deprecia thresholds 75%/80–85% antigos como histórico
- [ ] `TESTING.md` cita `check-coverage-95.sh` como gate canônico
- [ ] `CONCERNS.md` "Test Coverage Gaps" atualizado (fechado)
- [ ] Contagens finais de teste registradas

**Tests**: none (docs)
**Gate**: build (nenhuma mudança de código)

**Commit**: `docs(coverage): sync TESTING/CONCERNS com meta 95% + AD-014`

---

## Phase Execution Map

```
Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4

Phase 0:  T1 ──→ T2
Phase 1:  T3 ──→ T4 ──→ T5 ──→ T6 ──→ T7 ──→ T8 ──→ T9
Phase 2:  T10 ──→ T11 ──→ T12 ──→ T13 ──→ T14
Phase 3:  T15 ──→ T16 ──→ T17 ──→ T18 ──→ T19 ──→ T20
Phase 4:  T21 ──→ T22
```

Execução estritamente sequencial. Total: **22 tasks** → packing em ~4 batches (~7 tasks/fases consecutivas) → **dispara oferta de sub-agentes** na fase Execute. Verifier independente roda após T22.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: lombok.config + deprecar gate antigo | 1 config + refs | ✅ Granular |
| T2: gate script | 1 script | ✅ Granular |
| T3–T9: cobrir 1 pacote `.application` cada | 1 pacote coeso de service tests | ✅ Granular (coeso) |
| T10–T14: cobrir 1 pacote `.api` cada | 1 pacote coeso de WebMvc tests | ✅ Granular (coeso) |
| T15–T16, T20: 1 arquivo/área FE | 1 arquivo de teste | ✅ Granular |
| T17–T19: 2 arquivos FE coesos cada | 2 arquivos relacionados | ⚠️ OK (coeso) |
| T21: fechamento gate | agregação | ✅ Granular (verificação) |
| T22: docs | 2 docs | ✅ Granular |

> Nota: tasks de cobertura são atômicas por **área coesa** (pacote/página) — o deliverable é "área X a 95%", verificável por gate. Folha foi dividida (T3/T4) por ser o maior pacote.

---

## Diagram-Definition Cross-Check

| Task | Depends On (body) | Diagram Shows | Status |
| ---- | ----------------- | ------------- | ------ |
| T1 | None | (início) | ✅ Match |
| T2 | T1 | T1→T2 | ✅ Match |
| T3 | T2 | T2→T3 | ✅ Match |
| T4 | T3 | T3→T4 | ✅ Match |
| T5 | T4 | T4→T5 | ✅ Match |
| T6 | T5 | T5→T6 | ✅ Match |
| T7 | T6 | T6→T7 | ✅ Match |
| T8 | T7 | T7→T8 | ✅ Match |
| T9 | T8 | T8→T9 | ✅ Match |
| T10 | T9 | T9→T10 | ✅ Match |
| T11 | T10 | T10→T11 | ✅ Match |
| T12 | T11 | T11→T12 | ✅ Match |
| T13 | T12 | T12→T13 | ✅ Match |
| T14 | T13 | T13→T14 | ✅ Match |
| T15 | T14 | T14→T15 | ✅ Match |
| T16 | T15 | T15→T16 | ✅ Match |
| T17 | T16 | T16→T17 | ✅ Match |
| T18 | T17 | T17→T18 | ✅ Match |
| T19 | T18 | T18→T19 | ✅ Match |
| T20 | T19 | T19→T20 | ✅ Match |
| T21 | T20 | T20→T21 | ✅ Match |
| T22 | T21 | T21→T22 | ✅ Match |

Cadeia estritamente linear; nenhuma dependência aponta para fase posterior. ✅

---

## Test Co-location Validation

| Task | Code Layer | Matrix Requires | Task Says | Status |
| ---- | ---------- | --------------- | --------- | ------ |
| T1 | config Lombok | none (build gate) | none | ✅ OK |
| T2 | gate script | none (self-check) | none | ✅ OK |
| T3–T9 | Backend service | unit | unit | ✅ OK |
| T7 | Backend service (ADP) | unit (+integração Docker-gated) | unit | ✅ OK |
| T10–T13 | Backend controller | integration (WebMvc) | integration | ✅ OK |
| T14 | Controller + domain | integration + unit | integration + unit | ✅ OK |
| T15–T20 | Frontend page/service/util | unit | unit | ✅ OK |
| T21 | agregação | none (verificação) | none | ✅ OK |
| T22 | docs | none | none | ✅ OK |

Todas as tasks que criam/modificam camada com tipo de teste exigido incluem os testes na própria task (co-locados). ✅ Nenhuma violação.

---

## Coverage (rastreabilidade)

15 requisitos (COV-01…15) — todos mapeados a tasks:

| Requisito | Tasks |
| --------- | ----- |
| COV-01/02/03 (BE linha) | T3–T14, T21 |
| COV-04/05/06 (FE linha) | T15–T20, T21 |
| COV-07/08 (BE branch) | T3–T14, T21 |
| COV-09 (branch inatingível documentado) | T7, T14, T21 |
| COV-10/11/12 (FE branch) | T15–T20, T21 |
| COV-13/14/15 (gate) | T1, T2, T21 |

0 requisitos não mapeados. ✅
