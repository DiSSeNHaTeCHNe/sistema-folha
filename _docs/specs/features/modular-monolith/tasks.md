# Monólito Modular — Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/modular-monolith/design.md`  
**Status**: Draft  
**Approach**: A (pacotes por domínio + ports in-process)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `_docs/specs/TESTING.md`, `backend/AGENTS.md` §4, `frontend/AGENTS.md` (AD-004: FE skills target ≠ obrigação), `spec.md` ACs (MockMvc Security mínimo; ArchUnit P2; unitários de ports/ACL).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Application services / port adapters | unit (Mockito) | All branches for ACs in scope; 1:1 to MOD ACs touched; every listed edge case for that service | `backend/src/test/java/**/{application,service,arch}/*Test.java` (espelhar pacote pós-move) | `cd backend && mvn test` |
| Port interfaces / DTOs / enums / entities | none | — (build gate only) | — | build / `mvn test` suite |
| Controllers | none | — (repo gap; smoke via service tests + compile) | — | — |
| Security matchers (MOD-13) | unit (MockMvc / `@WebMvcTest`) | POST `/tipo-beneficio` sem ADMIN → 403; com ADMIN → 2xx | `backend/src/test/java/**/config/*Security*Test.java` | `cd backend && mvn test -Dtest=*Security*` |
| ArchUnit rules (P2) | unit (ArchUnit) | Rules fail on forbidden import; pass on clean tree | `backend/src/test/java/**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Flyway migrations | none | Idempotent SQL; manual/`flyway:migrate` when DB available | `backend/src/main/resources/db/migration/V*.sql` | `cd backend && mvn flyway:migrate` (env) |
| Frontend pages/services/contexts | none | Lint + build only (AD-004; no Vitest yet) | — | `cd frontend && npm run lint && npm run build` |
| Docs / ARCHITECTURE / checklist | none | Artefato presente + checklist command exit 0 | `_docs/specs/**`, `diversos/scripts/check-modular-compliance.sh` | `./diversos/scripts/check-modular-compliance.sh` |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após tasks só de service/port/adapter (unit) | `cd backend && mvn test` |
| Full | Após tasks que tocam API + FE, ou Security MockMvc | `cd backend && mvn test && cd ../frontend && npm run lint && npm run build` |
| Build | Fechamento de fase / package moves / config-only | `cd backend && mvn clean package && cd ../frontend && npm run build` |

---

## Execution Plan

Phases are ordered and run sequentially — each phase completes before the next begins, and tasks within a phase execute in order.

### Phase 1: BeneficioConsultaPort + remoção legado

```
T1 → T2 → T3 → T4 → T5
```

### Phase 2: OrganogramaAcessoPort + correção ACL

```
T6 → T7 → T8 → T9 → T10
```

### Phase 3: Controllers finos

```
T11 → T12 → T13 → T14
```

### Phase 4: Pacote `beneficios.*`

```
T15 → T16 → T17
```

### Phase 5: SecurityConfig paths

```
T18
```

### Phase 6: Frontend mínimo 5A

```
T19 → T20 → T21 → T22
```

### Phase 7: ArchUnit base + migrar Folha

```
T23 → T24 → T25
```

### Phase 8: Migrar Cadastros + Organograma

```
T26 → T27
```

### Phase 9: Migrar restante + logging + ArchUnit cumulativo

```
T28 → T29 → T30
```

### Phase 10: Conformidade P3 (docs + checklist)

```
T31 → T32
```

---

## Task Breakdown

### T1: Criar interface `BeneficioConsultaPort`

**What**: Definir o contrato público cross-domain de consulta agregada de benefícios mensais (sem entities JPA).  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/beneficios/port/BeneficioConsultaPort.java` (criar pacote `beneficios/port` se necessário)  
**Depends on**: None  
**Reuses**: Assinaturas do design §2  
**Requirement**: MOD-02

**Tools**:
- MCP: `user-context7` (opcional — Spring interfaces)
- Skill: `modular-design-principles`

**Done when**:
- [x] Interface com: `somarValorPorFuncionarioECompetencia`, `contarLancamentosPorFuncionarioECompetencia`, `existeDadosMensaisNaCompetencia`, `contarLancamentosAtivosNaCompetencia`
- [x] Retornos `BigDecimal` / `int` / `boolean` / `long` — sem tipos JPA
- [x] Compila (`mvn test` suite existente ainda não referencia a port)

**Tests**: none  
**Gate**: build (`cd backend && mvn clean package -DskipTests`)  
**Commit**: `feat(beneficios): add BeneficioConsultaPort contract`

---

### T2: Implementar `BeneficioConsultaAdapter` + testes unitários

**What**: Adapter `@Service` que implementa a port lendo **somente** `BeneficioMensalRepository` (e tipos do domínio Benefícios).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/beneficios/application/BeneficioConsultaAdapter.java`
- `backend/src/test/java/br/com/techne/sistemafolha/beneficios/application/BeneficioConsultaAdapterTest.java`

**Depends on**: T1  
**Reuses**: Queries em `BeneficioMensalRepository`; padrão Mockito de `BeneficioMensalServiceTest`  
**Requirement**: MOD-02, MOD-03

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Adapter `@Service` implementa todos os métodos da port
- [x] Sem lançamentos → `ZERO` / `0` / `false` (sem exceção)
- [x] IDs/`LocalDate` nulos → `IllegalArgumentException`
- [x] Testes cobrem: soma parcial por funcionário; competência sem dados; null args; contagem dashboard
- [x] Gate check passa: `cd backend && mvn test`
- [x] Test count: ≥4 testes novos passam (no silent deletions da suite)

**Tests**: unit  
**Gate**: quick  
**Commit**: `feat(beneficios): implement BeneficioConsultaAdapter with unit tests`

---

### T3: Refatorar `FolhaTotalizacaoService` para consumir a port

**What**: Remover branch dual legado/mensal; injetar apenas `BeneficioConsultaPort`; atualizar testes para mockar a port (nunca `BeneficioRepository`).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/service/FolhaTotalizacaoService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/service/FolhaTotalizacaoServiceTest.java`

**Depends on**: T2  
**Reuses**: Assinatura pública `calcularTotaisPorFuncionario`; coeficientes de rubrica  
**Requirement**: MOD-02, MOD-03

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Zero injeção de `BeneficioRepository` / `BeneficioMensalRepository` neste service
- [x] Branch ~74–91 legado removido
- [x] Testes usam `@Mock BeneficioConsultaPort`; cenário “sem mensal → legado” **removido** ou substituído por “port retorna zero”
- [x] Edge: competência parcial / lista vazia / port zero — cobertos
- [x] Gate: `cd backend && mvn test`
- [x] Test count: suite `FolhaTotalizacaoServiceTest` passa (≥2 testes; nenhum mock de `BeneficioRepository`)

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(folha): totalizacao via BeneficioConsultaPort only`

---

### T4: Refatorar `DashboardService` para consumir a port

**What**: Substituir `BeneficioRepository` por `BeneficioConsultaPort`; criar testes unitários do dashboard.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/service/DashboardService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/service/DashboardServiceTest.java` (novo)

**Depends on**: T2  
**Reuses**: Agregações existentes por linha/centro/cargo  
**Requirement**: MOD-28

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Zero `BeneficioRepository` no `DashboardService`
- [x] Métricas de benefício via port (`contarLancamentosAtivosNaCompetencia` ou equivalente)
- [x] `DashboardServiceTest` com mock da port — happy + zero dados
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥2 testes novos em `DashboardServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(dashboard): benefits metrics via BeneficioConsultaPort`

---

### T5: Remover legado `Beneficio` + Flyway V1.14

**What**: Deletar entity/repository e qualquer referência restante; migration idempotente drop da tabela `beneficios` e índices.  
**Where**:
- DELETE `backend/src/main/java/br/com/techne/sistemafolha/model/Beneficio.java`
- DELETE `backend/src/main/java/br/com/techne/sistemafolha/repository/BeneficioRepository.java`
- CREATE `backend/src/main/resources/db/migration/V1.14__drop_beneficios_legado.sql`
- Grep limpeza de imports/refs em main+test

**Depends on**: T3, T4  
**Reuses**: Design §3 SQL  
**Requirement**: MOD-01

**Tools**:
- MCP: NONE
- Skill: `flyway-migration-writer`

**Done when**:
- [x] `grep -r BeneficioRepository backend/src` → zero
- [x] `grep -r model.Beneficio backend/src` → zero (exceto comentários históricos em docs fora de src)
- [x] Migration: `DROP INDEX IF EXISTS` ×3 + `DROP TABLE IF EXISTS beneficios`
- [x] Gate: `cd backend && mvn test`
- [x] Test count: suite completa passa (sem testes do legado)

**Tests**: none (migration + compile; suite existing = gate)  
**Gate**: quick  
**Commit**: `feat(beneficios): drop legacy Beneficio model and table V1.14`

---

### T6: Criar `MotivoNegacaoAcesso` + `AccessContextDTO`

**What**: Enum e record tipados do contrato ACL (sinais distintos).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/organograma/acesso/port/MotivoNegacaoAcesso.java`
- `backend/src/main/java/br/com/techne/sistemafolha/organograma/acesso/port/AccessContextDTO.java`

**Depends on**: None (pode iniciar após Phase 1; sequencial no plano)  
**Reuses**: Design §4 / Data Models  
**Requirement**: MOD-08, MOD-09

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Enum: `SEM_FUNCIONARIO`, `SEM_NO_ORGANOGRAMA`
- [x] Record com campos do design (`temFuncionarioVinculado`, `temNoOrganograma`, `acessoTotal`, `centrosCustoIds`, `motivoNegacao`, metadados de nó)
- [x] Compila

**Tests**: none  
**Gate**: build (`cd backend && mvn clean package -DskipTests`)  
**Commit**: `feat(organograma): add AccessContextDTO and MotivoNegacaoAcesso`

---

### T7: Criar interface `OrganogramaAcessoPort`

**What**: Contrato público ACL consumido por Folha/Benefícios/Dashboard/Auth.  
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/organograma/acesso/port/OrganogramaAcessoPort.java`  
**Depends on**: T6  
**Reuses**: Design §4  
**Requirement**: MOD-08

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Métodos: `obterCentrosCustoAcessiveis`, `usuarioPodeAcessarCentroCusto`, `obterContextoAcesso`
- [x] Compila

**Tests**: none  
**Gate**: build (`cd backend && mvn clean package -DskipTests`)  
**Commit**: `feat(organograma): add OrganogramaAcessoPort`

---

### T8: Refatorar `OrganogramaAcessoService` — ACL fix + implementa port

**What**: Mover/refatorar service para `organograma.acesso.application`, implementar port, corrigir conflação empty/`Optional`; testes dos 3 cenários.  
**Where**:
- MOVE/EDIT → `backend/src/main/java/br/com/techne/sistemafolha/organograma/acesso/application/OrganogramaAcessoService.java`
- CREATE `backend/src/test/java/br/com/techne/sistemafolha/organograma/acesso/application/OrganogramaAcessoServiceTest.java`
- Atualizar imports dos consumidores temporários se path mudar

**Depends on**: T7  
**Reuses**: `coletarCentrosCustoRecursivo`; repos organograma internos  
**Requirement**: MOD-08, MOD-09, MOD-10

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Sem funcionário → negar (`acessoTotal=false`, centros ∅, `motivoNegacao=SEM_FUNCIONARIO`, `usuarioPodeAcessarCentroCusto` false)
- [x] Com funcionário sem nó → negar (`SEM_NO_ORGANOGRAMA`)
- [x] Com nó → centros = nó + descendentes; `acessoTotal=false` (não derivado de empty)
- [x] Empty set **nunca** implica acesso total
- [x] Testes unitários cobrem os 3 cenários + `usuarioPodeAcessarCentroCusto`
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥3 testes ACL novos passam

**Tests**: unit  
**Gate**: quick  
**Commit**: `fix(organograma): deny ACL without employee or org node via port`

---

### T9: Evoluir `AcessoUsuarioDTO` + mapear em Auth

**What**: DTO HTTP tipado com sinais distintos; `AuthenticationService` / `AuthController` montam a partir de `OrganogramaAcessoPort.obterContextoAcesso`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/dto/AcessoUsuarioDTO.java` (ou move para `organograma/acesso/api/` / `auth/api/`)
- `backend/src/main/java/br/com/techne/sistemafolha/security/AuthenticationService.java` (ou path atual)
- Consumidores de `GET /auth/acesso` / `TokenDTO.acessoUsuario`

**Depends on**: T8  
**Reuses**: Campos existentes de nó; **breaking**: `centrosCustoIds` + flags; remover semântica “empty = total” do Javadoc  
**Requirement**: MOD-29

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Campos: `temFuncionarioVinculado`, `temNoOrganograma`, `acessoTotal`, `centrosCustoIds`, `motivoNegacao`, metadados
- [x] Javadoc antigo (“Set vazio = acesso total”) removido
- [x] Login/refresh/`GET /auth/acesso` usam port — sem `Map<String,Object>` ambíguo
- [x] Gate: `cd backend && mvn test`

**Tests**: unit (atualizar/criar testes do serviço de auth/acesso se existirem; senão cobrir via `OrganogramaAcessoServiceTest` + compile — preferir teste de mapeamento DTO no service de auth se houver lógica)  
**Gate**: quick  
**Commit**: `feat(auth): expose distinct ACL signals in AcessoUsuarioDTO`

---

### T10: Consumidores BE usam `OrganogramaAcessoPort` (não service concreto)

**What**: Folha, Benefícios Mensais, Dashboard (e demais filtros de centro) dependem só da port.  
**Where**: Services/controllers que hoje injetam `OrganogramaAcessoService` concreto  
**Depends on**: T8  
**Reuses**: Métodos da port  
**Requirement**: MOD-10

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Grep: consumidores fora de `organograma.acesso` não importam `OrganogramaAcessoService` concreto — só `OrganogramaAcessoPort`
- [x] Comportamento de filtro preservado para usuário com nó
- [x] Gate: `cd backend && mvn test`

**Tests**: unit (ajustar mocks nos testes existentes para a port)  
**Gate**: quick  
**Commit**: `refactor: consume OrganogramaAcessoPort across domains`

---

### T11: Extrair `FolhaPagamentoService` + controller fino

**What**: Mover queries/soft-delete/map DTO/filtro ACL do controller para service novo; controller sem `*Repository`.  
**Where**:
- CREATE `backend/src/main/java/br/com/techne/sistemafolha/service/FolhaPagamentoService.java` (P2 move para `folha.application`)
- EDIT `backend/src/main/java/br/com/techne/sistemafolha/controller/FolhaPagamentoController.java`
- CREATE `backend/src/test/java/br/com/techne/sistemafolha/service/FolhaPagamentoServiceTest.java` (mínimo: 1–2 caminhos chave + ACL filter mock)

**Depends on**: T10 (ACL via port disponível)  
**Reuses**: Corpo atual do controller  
**Requirement**: MOD-04, MOD-05, MOD-14

**Tools**:
- MCP: NONE
- Skill: `spring-boot-new-endpoint` (padrões de service; sem endpoint novo)

**Done when**:
- [x] Controller sem campos `*Repository`
- [x] Service concentra persistência + map DTO
- [x] Testes unitários do service passam
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥2 testes em `FolhaPagamentoServiceTest`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(folha): extract FolhaPagamentoService; thin controller`

---

### T12: Extrair `ResumoFolhaPagamentoService` + controller fino

**What**: Idem para resumo de folha.  
**Where**:
- CREATE `.../service/ResumoFolhaPagamentoService.java`
- EDIT `.../controller/ResumoFolhaPagamentoController.java`
- CREATE `.../service/ResumoFolhaPagamentoServiceTest.java`

**Depends on**: T11  
**Reuses**: Queries atuais do controller  
**Requirement**: MOD-04, MOD-05

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Controller sem `ResumoFolhaPagamentoRepository`
- [x] Testes unitários mínimos do service
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥1 teste novo

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(folha): extract ResumoFolhaPagamentoService; thin controller`

---

### T13: Thin `BeneficioMensalController`

**What**: Remover injeção de repositories; delegar resolução de usuário/CRUD ao `BeneficioMensalService` (expandir service se necessário).  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/controller/BeneficioMensalController.java`
- `backend/src/main/java/br/com/techne/sistemafolha/service/BeneficioMensalService.java`
- `backend/src/test/java/br/com/techne/sistemafolha/service/BeneficioMensalServiceTest.java` (atualizar)

**Depends on**: T10  
**Reuses**: Service existente  
**Requirement**: MOD-04, MOD-14

**Tools**:
- MCP: NONE
- Skill: `jpa-performance`

**Done when**:
- [x] Controller sem `*Repository`
- [x] Testes de `BeneficioMensalService` atualizados / passam
- [x] Gate: `cd backend && mvn test`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(beneficios): thin BeneficioMensalController`

---

### T14: Thin `AuthController`

**What**: Remover `UsuarioRepository` do controller; delegar a `UsuarioService` ou `AuthenticationService`.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/controller/AuthController.java`
- Service de auth/usuário correspondente

**Depends on**: T9  
**Reuses**: Fluxos login/refresh/acesso existentes  
**Requirement**: MOD-04, MOD-14

**Tools**:
- MCP: NONE
- Skill: `spring-security`

**Done when**:
- [x] AuthController sem `UsuarioRepository`
- [x] Contratos HTTP inalterados (exceto DTO ACL já evoluído em T9)
- [x] Gate: `cd backend && mvn test`

**Tests**: unit (se lógica nova no service; senão suite + compile)  
**Gate**: quick  
**Commit**: `refactor(auth): thin AuthController without repository injection`

---

### T15: Mover camada domain + infrastructure de Benefícios

**What**: Relocar entities, repositories e exceptions de Benefícios para `beneficios.domain` / `beneficios.infrastructure`.  
**Where**: Pacotes sob `br.com.techne.sistemafolha.beneficios.{domain,infrastructure}` (+ exceptions do domínio)  
**Depends on**: T5, T13  
**Reuses**: Classes mensais/tipo existentes (não legado)  
**Requirement**: MOD-06

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Entities/repos Benefícios fora dos pacotes planos `model/` / `repository/`
- [x] Imports atualizados; app compila
- [x] Gate: `cd backend && mvn test`

**Tests**: unit (atualizar packages dos testes que importam entities/repos)  
**Gate**: quick  
**Commit**: `refactor(beneficios): move domain and infrastructure packages`

---

### T16: Mover application Benefícios + atualizar testes

**What**: Relocar services/adapter para `beneficios.application`; espelhar pacotes de teste.  
**Where**: `beneficios/application/*`; testes `.../beneficios/application/*Test.java`  
**Depends on**: T15, T2  
**Reuses**: `BeneficioMensalService`, `TipoBeneficioService`, `ImportacaoBeneficioMensalService`, `BeneficioConsultaAdapter`  
**Requirement**: MOD-06, MOD-07

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] `grep -r "sistemafolha.service.BeneficioMensalService" backend` → zero
- [x] `mvn test -Dtest=*Beneficio*,*TipoBeneficio*,*ImportacaoBeneficioMensal*` exit 0
- [x] Gate: `cd backend && mvn test`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(beneficios): move application services to beneficios.application`

---

### T17: Mover API Benefícios (`*.api`) + DTOs do domínio

**What**: Controllers (e DTOs de benefícios) em `beneficios.api` / pacote DTO do domínio; rotas inalteradas.  
**Where**: `beneficios/api/*Controller`; DTOs de benefício/tipo/importação  
**Depends on**: T16  
**Reuses**: Mappings HTTP existentes  
**Requirement**: MOD-06, MOD-07

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Controllers Benefícios sob `beneficios.api`
- [x] Outros domínios não importam `beneficios.infrastructure`
- [x] Gate: `cd backend && mvn clean package` (build) + `mvn test`
- [x] Rotas `/beneficio-mensal`, `/tipo-beneficio`, `/importacao/beneficios-mensais` preservadas

**Tests**: unit (suite benefícios)  
**Gate**: build  
**Commit**: `refactor(beneficios): move controllers to beneficios.api`

---

### T18: Alinhar `SecurityConfig` + teste MockMvc mínimo

**What**: Matchers sem `/api` duplicado; remover `/api/beneficios/**`; ADMIN em `/tipo-beneficio` POST; teste 403 vs 2xx.  
**Where**:
- `backend/src/main/java/br/com/techne/sistemafolha/config/SecurityConfig.java`
- CREATE `backend/src/test/java/br/com/techne/sistemafolha/config/SecurityConfigTipoBeneficioTest.java`

**Depends on**: T17 (paths de tipo-benefício estáveis)  
**Reuses**: JWT filter; design §9  
**Requirement**: MOD-13

**Tools**:
- MCP: `user-context7` (Spring Security 6 matchers / MockMvc)
- Skill: `spring-security`

**Done when**:
- [x] Zero matcher `/api/beneficios/**`
- [x] Paths consistentes com `context-path` (sem prefixo `/api` duplicado)
- [x] Teste: POST `/tipo-beneficio` sem ADMIN → 403; com ADMIN → 2xx (ou 201/200 conforme controller)
- [x] Login/refresh/permitAll inalterados em comportamento
- [x] Gate: `cd backend && mvn test`
- [x] Test count: ≥2 casos no teste de security

**Tests**: unit (MockMvc)  
**Gate**: quick  
**Commit**: `fix(security): align SecurityConfig matchers; protect tipo-beneficio`

---

### T19: Remover órfãos FE (`beneficioService`, Example, App morto)

**What**: Deletar service legado e páginas/arquivos fora do grafo de `main.tsx`.  
**Where**:
- DELETE `frontend/src/services/beneficioService.ts`
- DELETE `frontend/src/pages/Example/` (se presente)
- Remover `App.tsx`/`App.css` se fora do grafo; limpar tipos `Beneficio` legado em `types/index.ts`

**Depends on**: T5 (contrato BE sem legado)  
**Reuses**: N/A  
**Requirement**: MOD-12, MOD-27

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Arquivos órfãos ausentes
- [x] Grep `beneficioService` no frontend → zero
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: full (FE lint+build; backend opcional se sem mudança) — use Full se monorepo gate: prefer `cd frontend && npm run lint && npm run build`  
**Commit**: `chore(frontend): remove legacy beneficioService and orphan pages`

---

### T20: `FolhaPagamento` page sem import direto de `api.ts`

**What**: Delegar HTTP a `folhaPagamentoService` (expandir service se faltar método).  
**Where**:
- `frontend/src/pages/FolhaPagamento/index.tsx`
- `frontend/src/services/folhaPagamentoService.ts`

**Depends on**: T19  
**Reuses**: Padrão de outros `*Service.ts`  
**Requirement**: MOD-11

**Tools**:
- MCP: NONE
- Skill: NONE (não usar `api-client` target — AD-004)

**Done when**:
- [x] Zero `import ... from '.../api'` em `FolhaPagamento/index.tsx`
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: full (FE)  
**Commit**: `refactor(frontend): FolhaPagamento uses folhaPagamentoService only`

---

### T21: `Funcionarios` page sem import direto de `api.ts`

**What**: Delegar a `funcionarioService`.  
**Where**:
- `frontend/src/pages/Funcionarios/index.tsx`
- `frontend/src/services/funcionarioService.ts`

**Depends on**: T19  
**Reuses**: `funcionarioService` existente  
**Requirement**: MOD-11

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Zero import `api` na page Funcionarios
- [x] Grep `from.*services/api` em `frontend/src/pages` → zero (mínimo Folha+Funcionarios; ideal todas as pages)
- [x] Gate: `cd frontend && npm run lint && npm run build`

**Tests**: none  
**Gate**: full (FE)  
**Commit**: `refactor(frontend): Funcionarios uses funcionarioService only`

---

### T22: AuthContext + types ACL alinhados ao DTO evoluído

**What**: Tipagem `AcessoUsuario` com sinais distintos; negar default; nunca tratar empty como total.  
**Where**:
- `frontend/src/types/index.ts`
- `frontend/src/contexts/AuthContext.tsx`
- Consumidores de `centrosCustoAcessiveis` / `acessoTotal`

**Depends on**: T9, T19  
**Reuses**: Design FE `AcessoUsuario`  
**Requirement**: MOD-12, MOD-29

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Type com `temFuncionarioVinculado`, `temNoOrganograma`, `centrosCustoIds`, `motivoNegacao`
- [x] `podeAcessarCentroCusto`: nega se `!temFuncionarioVinculado || !temNoOrganograma`; `acessoTotal` só com flag explícita
- [x] Sem fallback permissivo (`return true`) quando contexto inválido
- [x] Gate: `cd frontend && npm run lint && npm run build` (+ `cd backend && mvn test` se Full monorepo)

**Tests**: none  
**Gate**: full  
**Commit**: `fix(frontend): honor distinct ACL denial signals in AuthContext`

---

### T23: Adicionar ArchUnit + regras iniciais (Benefícios)

**What**: Dependência `archunit-junit5:1.4.2`; teste que falha se Folha/Dashboard importarem `beneficios.infrastructure` ou controller injetar repository fora de `*.api`.  
**Where**:
- `backend/pom.xml`
- CREATE `backend/src/test/java/br/com/techne/sistemafolha/arch/ModularArchitectureTest.java`

**Depends on**: T17  
**Reuses**: Design §11  
**Requirement**: MOD-15, MOD-16

**Tools**:
- MCP: `user-context7` (ArchUnit JUnit5)
- Skill: `modular-design-principles`

**Done when**:
- [x] Dependência no `pom.xml` compatível Java 17 / Boot 3.2
- [x] Regras Benefícios/ports ativas e verdes no tree atual
- [x] Gate: `cd backend && mvn test -Dtest=ModularArchitectureTest` e `mvn test`

**Tests**: unit (ArchUnit)  
**Gate**: quick  
**Commit**: `test(arch): add ArchUnit rules for beneficios boundaries`

---

### T24: Migrar domínio Folha para `folha.*`

**What**: Mover `FolhaPagamento*`, `FolhaTotalizacaoService`, `ResumoFolhaPagamento*`, services/controllers/repos/DTOs de folha para `folha.{api,application,domain,infrastructure}`.  
**Where**: `br.com.techne.sistemafolha.folha.*`  
**Depends on**: T11, T12, T23  
**Reuses**: Estrutura de camadas de Benefícios  
**Requirement**: MOD-17

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Classes de Folha fora dos pacotes planos
- [x] Consome Benefícios só via `beneficios.port`
- [x] Gate: `cd backend && mvn test`

**Tests**: unit (atualizar packages dos testes de folha/totalização)  
**Gate**: quick  
**Commit**: `refactor(folha): migrate package to folha.*`

---

### T25: Expandir ArchUnit — Folha ↔ Benefícios via ports only

**What**: Adicionar regras cumulativas pós-migração Folha (nunca relaxar anteriores).  
**Where**: `ModularArchitectureTest.java`  
**Depends on**: T24  
**Reuses**: T23  
**Requirement**: MOD-20

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Regra: `folha..` não importa `beneficios.infrastructure..`
- [x] Introduzir import proibido em scratch mental / teste temporário local opcional — build falha; código limpo passa
- [x] Gate: `cd backend && mvn test`

**Tests**: unit (ArchUnit)  
**Gate**: quick  
**Commit**: `test(arch): enforce folha-beneficios port-only dependency`

---

### T26: Migrar Cadastros para `cadastros.*`

**What**: Mover Funcionário, Cargo, Centro, Rubrica, Linha (e correlatos) para `cadastros.*` (subpacotes por agregado permitidos).  
**Where**: `br.com.techne.sistemafolha.cadastros.*`  
**Depends on**: T25  
**Reuses**: Padrão de move Benefícios/Folha  
**Requirement**: MOD-18

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Agregados de cadastro sob `cadastros.*`
- [x] Sem cross-import de `infrastructure` entre consumidores indevidos
- [x] Gate: `cd backend && mvn test`

**Tests**: unit (`FuncionarioServiceTest` etc. atualizados de pacote)  
**Gate**: quick  
**Commit**: `refactor(cadastros): migrate aggregates to cadastros.*`

---

### T27: Migrar Organograma (+ consolidar `acesso`) para `organograma.*`

**What**: Mover entidades nó/vínculo/services de organograma; consolidar submodule `organograma.acesso` já criado.  
**Where**: `br.com.techne.sistemafolha.organograma.*`  
**Depends on**: T26, T8  
**Reuses**: Port já pública  
**Requirement**: MOD-18, MOD-10

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Organograma sob `organograma.*`; `OrganogramaAcessoPort` permanece superfície pública
- [x] ArchUnit expandido se necessário (sem relaxar)
- [x] Gate: `cd backend && mvn test`

**Tests**: unit  
**Gate**: quick  
**Commit**: `refactor(organograma): migrate package and consolidate acesso submodule`

---

### T28: Migrar Importação + Auth + Dashboard packages

**What**: `importacao.*` (Folha ADP; benefícios mensais já em beneficios), `auth.*` / `security` mínimo, `dashboard.*`.  
**Where**: Pacotes conforme design Phase P2 levas 4–5  
**Depends on**: T27  
**Reuses**: Services existentes  
**Requirement**: MOD-19

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Classes restantes de importação/auth/dashboard fora do layout plano (salvo shared mínimo)
- [x] Rotas Swagger preservadas
- [x] Gate: `cd backend && mvn clean package` + `mvn test`

**Tests**: unit  
**Gate**: build  
**Commit**: `refactor: migrate importacao, auth, and dashboard packages`

---

### T29: ArchUnit cumulativo final (todos os domínios migrados)

**What**: Regras finais: nenhum domínio importa `infrastructure` alheia; controllers só em `..api..` sem `*Repository`.  
**Where**: `ModularArchitectureTest.java`  
**Depends on**: T28  
**Reuses**: T23–T25  
**Requirement**: MOD-15, MOD-16

**Tools**:
- MCP: NONE
- Skill: `modular-design-principles`

**Done when**:
- [x] Suite ArchUnit verde no main
- [x] Gate: `cd backend && mvn test`

**Tests**: unit (ArchUnit)  
**Gate**: quick  
**Commit**: `test(arch): finalize cumulative modular boundary rules`

---

### T30: Logging estruturado `domain=` nos módulos migrados

**What**: MDC ou prefixo key-value `domain=<nome>` em application services; WARN em negação ACL com `usuarioId` + `motivoNegacao` (sem PII extra).  
**Where**: Services application migrados; especialmente `OrganogramaAcessoService`  
**Depends on**: T27, T28  
**Reuses**: SLF4J/Logback existentes  
**Requirement**: MOD-21, MOD-22

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Logs de application layer incluem `domain=`
- [x] Negação ACL → WARN com campos exigidos
- [x] Níveis default inalterados
- [x] Gate: `cd backend && mvn test`

**Tests**: unit (opcional assert de interação com logger mock se já houver padrão; senão inspeção + suite)  
**Gate**: quick  
**Commit**: `feat(observability): add structured domain= logging`

---

### T31: Atualizar `_docs/specs/ARCHITECTURE.md`

**What**: Remover “Dual benefit domain”; documentar monólito modular in-process, ports, ordem 3B, diagrama de dependências permitidas.  
**Where**: `_docs/specs/ARCHITECTURE.md`  
**Depends on**: T29 (fronteiras finais conhecidas)  
**Reuses**: Design overview  
**Requirement**: MOD-26

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Seção dual model removida/substituída
- [x] Ports e pacotes `{dominio}.{camada}` documentados
- [x] Deploy in-process explícito

**Tests**: none  
**Gate**: build (doc-only — validação por revisão; sem comando de teste)  
**Commit**: `docs(architecture): document modular monolith and ports`

---

### T32: Checklist BE+FE reproduzível (MOD-24/25/30)

**What**: Script ou target documentado (Maven/npm/shell) que verifica ports, zero legado, ArchUnit, controllers sem repo, FE sem `api.ts` em pages, órfãos, ACL fields.  
**Where**: Preferir `diversos/scripts/check-modular-compliance.sh` (ou equivalente) + referência em design/tasks  
**Depends on**: T22, T29, T31  
**Reuses**: greps + `mvn test -Dtest=ModularArchitectureTest` + `npm run lint`  
**Requirement**: MOD-24, MOD-25, MOD-30

**Tools**:
- MCP: NONE
- Skill: NONE

**Done when**:
- [x] Comando único documentado exit 0 no tree pós-refactor
- [x] Checklist cobre itens BE + FE da spec Success Criteria
- [x] Gate: executar o checklist + `cd backend && mvn test && cd ../frontend && npm run lint && npm run build`

**Tests**: none (script de verificação)  
**Gate**: full  
**Commit**: `chore: add modular compliance checklist script`

---

> **Nota MOD-23:** `_docs/specs/features/modular-monolith/validation.md` é produzido pelo **Verifier** TLC após o último commit de Execute — não é task do autor.

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8 → Phase 9 → Phase 10

Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4 ──→ T5
Phase 2:  T6 ──→ T7 ──→ T8 ──→ T9 ──→ T10
Phase 3:  T11 ──→ T12 ──→ T13 ──→ T14
Phase 4:  T15 ──→ T16 ──→ T17
Phase 5:  T18
Phase 6:  T19 ──→ T20 ──→ T21 ──→ T22
Phase 7:  T23 ──→ T24 ──→ T25
Phase 8:  T26 ──→ T27
Phase 9:  T28 ──→ T29 ──→ T30
Phase 10: T31 ──→ T32
```

Execution is strictly sequential — no intra-phase parallelism.

**Suggested batch packing (~7 tasks / worker, whole phases):**

| Batch | Phases | Tasks |
| ----- | ------ | ----- |
| 1 | Phase 1 | T1–T5 (5) |
| 2 | Phase 2 | T6–T10 (5) |
| 3 | Phase 3 + 4 | T11–T17 (7) |
| 4 | Phase 5 + 6 | T18–T22 (5) |
| 5 | Phase 7 | T23–T25 (3) |
| 6 | Phase 8 + 9 | T26–T30 (5) |
| 7 | Phase 10 | T31–T32 (2) |

On Execute: offer batch sub-agents (32 tasks → multi-batch). Batches run sequentially. Verifier runs automatically after T32.

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: BeneficioConsultaPort | 1 interface | ✅ Granular |
| T2: BeneficioConsultaAdapter + tests | 1 adapter + tests | ✅ Granular |
| T3: FolhaTotalizacaoService → port | 1 service + tests | ✅ Granular |
| T4: DashboardService → port | 1 service + tests | ✅ Granular |
| T5: Drop legado + Flyway | remoção coesa + 1 migration | ✅ Granular |
| T6: MotivoNegacao + AccessContextDTO | 2 types coesos | ✅ OK cohesive |
| T7: OrganogramaAcessoPort | 1 interface | ✅ Granular |
| T8: OrganogramaAcessoService ACL | 1 service + tests | ✅ Granular |
| T9: AcessoUsuarioDTO + auth map | DTO + wiring auth | ✅ OK cohesive |
| T10: Wire port consumers | troca de dependência cross-cutting | ✅ OK cohesive |
| T11: FolhaPagamentoService extract | 1 service + thin controller | ✅ Granular |
| T12: ResumoFolhaPagamentoService | 1 service + thin controller | ✅ Granular |
| T13: Thin BeneficioMensalController | 1 controller + service expand | ✅ Granular |
| T14: Thin AuthController | 1 controller | ✅ Granular |
| T15–T17: Move Benefícios layers | 1 layer group each | ✅ Granular |
| T18: SecurityConfig + MockMvc | 1 config + test | ✅ Granular |
| T19: FE orphans | remoção coesa | ✅ Granular |
| T20–T21: FE pages | 1 page each | ✅ Granular |
| T22: AuthContext ACL | 1 context + types | ✅ Granular |
| T23–T25: ArchUnit + Folha | 1 concern each | ✅ Granular |
| T26–T28: Domain package moves | 1 leva each | ✅ OK (mechanical move) |
| T29: ArchUnit final | 1 test class update | ✅ Granular |
| T30: Logging domain= | observability pass | ✅ Granular |
| T31: ARCHITECTURE.md | 1 doc | ✅ Granular |
| T32: Checklist script | 1 script | ✅ Granular |

**Granularity check**: all ✅ — no task requires split before approval.

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | (start) | ✅ Match |
| T2 | T1 | T1→T2 | ✅ Match |
| T3 | T2 | T2→T3 | ✅ Match |
| T4 | T2 | T2→T4 (via T3→T4 chain; T4 after T3 in phase order) | ✅ Match* |
| T5 | T3, T4 | T3→T4→T5 | ✅ Match |
| T6 | None | Phase2 start | ✅ Match |
| T7 | T6 | T6→T7 | ✅ Match |
| T8 | T7 | T7→T8 | ✅ Match |
| T9 | T8 | T8→T9 | ✅ Match |
| T10 | T8 | T8→T9→T10 (T10 also after T9 in sequence) | ✅ Match* |
| T11 | T10 | Phase2→T11 | ✅ Match |
| T12 | T11 | T11→T12 | ✅ Match |
| T13 | T10 | After T12 in phase; dep T10 only | ✅ Match* |
| T14 | T9 | After T13; dep T9 | ✅ Match* |
| T15 | T5, T13 | Phase3/1 → T15 | ✅ Match |
| T16 | T15, T2 | T15→T16 | ✅ Match |
| T17 | T16 | T16→T17 | ✅ Match |
| T18 | T17 | T17→T18 | ✅ Match |
| T19 | T5 | Phase6 after P1 | ✅ Match |
| T20 | T19 | T19→T20 | ✅ Match |
| T21 | T19 | T20→T21 (both dep T19) | ✅ Match* |
| T22 | T9, T19 | After T21; deps T9+T19 | ✅ Match |
| T23 | T17 | After Phase4 | ✅ Match |
| T24 | T11, T12, T23 | T23→T24 | ✅ Match |
| T25 | T24 | T24→T25 | ✅ Match |
| T26 | T25 | T25→T26 | ✅ Match |
| T27 | T26, T8 | T26→T27 | ✅ Match |
| T28 | T27 | T27→T28 | ✅ Match |
| T29 | T28 | T28→T29 | ✅ Match |
| T30 | T27, T28 | T29→T30 (after T28) | ✅ Match |
| T31 | T29 | T29→T31 | ✅ Match |
| T32 | T22, T29, T31 | T31→T32 | ✅ Match |

\* Phase order enforces sequencing beyond the minimal `Depends on` set; no forward-phase dependencies.

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Port interface | none | none | ✅ OK |
| T2 | Port adapter (application) | unit | unit | ✅ OK |
| T3 | Application service | unit | unit | ✅ OK |
| T4 | Application service | unit | unit | ✅ OK |
| T5 | Entity delete + Flyway | none | none | ✅ OK |
| T6 | DTO/enum | none | none | ✅ OK |
| T7 | Port interface | none | none | ✅ OK |
| T8 | Application service (ACL) | unit | unit | ✅ OK |
| T9 | DTO + auth wiring | unit (service logic) | unit | ✅ OK |
| T10 | Application consumers | unit | unit | ✅ OK |
| T11 | Application service | unit | unit | ✅ OK |
| T12 | Application service | unit | unit | ✅ OK |
| T13 | Application service + controller | unit | unit | ✅ OK |
| T14 | Auth controller/service | unit | unit | ✅ OK |
| T15 | Domain/infra move | unit (update tests) | unit | ✅ OK |
| T16 | Application move | unit | unit | ✅ OK |
| T17 | Controllers move | unit (suite) | unit | ✅ OK |
| T18 | Security config | unit (MockMvc) | unit (MockMvc) | ✅ OK |
| T19 | FE orphans | none | none | ✅ OK |
| T20 | FE page/service | none | none | ✅ OK |
| T21 | FE page/service | none | none | ✅ OK |
| T22 | FE context/types | none | none | ✅ OK |
| T23 | ArchUnit | unit (ArchUnit) | unit | ✅ OK |
| T24 | Folha packages | unit | unit | ✅ OK |
| T25 | ArchUnit | unit | unit | ✅ OK |
| T26 | Cadastros packages | unit | unit | ✅ OK |
| T27 | Organograma packages | unit | unit | ✅ OK |
| T28 | Import/Auth/Dashboard | unit | unit | ✅ OK |
| T29 | ArchUnit | unit | unit | ✅ OK |
| T30 | Logging (application) | unit / suite | unit | ✅ OK |
| T31 | Docs | none | none | ✅ OK |
| T32 | Checklist script | none | none | ✅ OK |

**Co-location check**: all ✅ — no deferred tests.

---

## Requirement Traceability (Tasks)

| Req ID | Tasks |
| ------ | ----- |
| MOD-01 | T5 |
| MOD-02 | T1, T2, T3 |
| MOD-03 | T2, T3 |
| MOD-04 | T11, T12, T13, T14 |
| MOD-05 | T11, T12 |
| MOD-06 | T15, T16, T17 |
| MOD-07 | T16, T17 |
| MOD-08 | T6, T7, T8 |
| MOD-09 | T6, T8 |
| MOD-10 | T8, T10, T27 |
| MOD-11 | T20, T21 |
| MOD-12 | T19, T22 |
| MOD-13 | T18 |
| MOD-14 | T11, T13, T14 |
| MOD-15 | T23, T29 |
| MOD-16 | T23, T29 |
| MOD-17 | T24 |
| MOD-18 | T26, T27 |
| MOD-19 | T28 |
| MOD-20 | T25 |
| MOD-21 | T30 |
| MOD-22 | T30 |
| MOD-23 | Verifier (`validation.md`) — pós-Execute |
| MOD-24 | T32 |
| MOD-25 | T32 |
| MOD-26 | T31 |
| MOD-27 | T19 |
| MOD-28 | T4 |
| MOD-29 | T9, T22 |
| MOD-30 | T32 |

**Coverage:** 30 total, 29 mapped to tasks, 1 Verifier-owned (MOD-23) ✅
