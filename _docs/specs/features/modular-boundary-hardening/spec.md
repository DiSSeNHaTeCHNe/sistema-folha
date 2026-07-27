# Modular Boundary Hardening — Specification

**Parent chain:** `modular-monolith` → `modular-monolith-fix` → `modular-acl-security-fix` (all Verifier **PASS**, uncommitted)  
**Source:** Decomposition roadmap Phase 2; AD-009 follow-up in `_docs/specs/STATE.md` Handoff/Todos  
**Complexity:** Large (ports cross-domain + ACL + ArchUnit + refactor de 2 god-paths)  
**Spec status:** Confirmed (Approach A Design drafted 2026-07-27)

## Problem Statement

A migração modular fechou isolamento application-layer na maioria dos domínios via ports (`BeneficioConsultaPort`, `OrganogramaAcessoPort`, lookup cadastros/auth), mas **`dashboard.application.DashboardService`** e **`importacao.application.ImportacaoFolhaAdpService`** ainda injetam repositórios JPA de `folha` e `cadastros` diretamente — violando AD-008 e deixando AD-009 como dívida documentada. ArchUnit **não** inclui regras `application → foreign infrastructure` para `dashboard..` nem `importacao..` (gap vs outros domínios). Em paralelo, **`GET /dashboard/stats`** retorna agregações **globais** sem `OrganogramaAcessoPort` — vazamento operacional para usuário restrito (P0), enquanto folha/benefícios já filtram por ACL.

Este follow-up **não reabre** migração de pacotes nem extração de microserviços — fecha fronteiras in-process: ports de consulta/escrita Folha, ACL no dashboard, refator dos dois offenders, ArchUnit completo, supersede AD-009.

## Goals

- [x] Eliminar reach-through persistence em `dashboard.application` e `importacao.application` (zero imports de `..infrastructure..` estrangeira)
- [x] Aplicar ACL no dashboard espelhando semântica MOD-09 (`OrganogramaAcessoPort` + empty-set restrito ⇒ stats vazios/zerados, nunca agregação global)
- [x] Introduzir ports públicas de folha (consulta + importação/escrita) com superfície sem entities JPA na port (primitivos/DTOs de contrato)
- [x] Estender ArchUnit para `dashboard..application..` e `importacao..application..`; remover dívida AD-009
- [x] Preservar comportamento funcional de importação ADP e stats do dashboard para usuário com `acessoTotal=true` (regressão zero nos gates existentes)

## Out of Scope

Explicitamente excluído. Documentado para prevent scope creep.

| Feature | Reason |
| ------- | ------ |
| Microserviços / multi-módulo Maven | AD-007 monólito in-process permanece |
| Evoluir **todas** ports de lookup para DTOs (cadastros/auth globais) | Phase 3 roadmap separada; este fix exige apenas ports **novas** de folha sem entity |
| Consolidar `ImportacaoBeneficioMensalController` em `importacao.api` | Follow-up organizacional; rotas HTTP estáveis neste fix |
| Split god-classes (`OrganogramaService`, LOC reduction) | Phase 4 refinement |
| Reabrir ACL empty-set benefício / refresh / folha delete | Já PASS em `modular-acl-security-fix` |
| Reabrir ArchUnit/ports do sibling `modular-monolith-fix` | Não regressão |
| Zerar ESLint brownfield | AD-004 advisory |
| Mudanças obrigatórias no FE além de compatibilidade com stats filtrados | FE mínimo; contrato REST `/dashboard/stats` preservado |
| Testcontainers / `@SpringBootTest` em massa | Manter unitário + MockMvc pontual se necessário |
| Performance N+1 import loops | Deferred concern STATE.md |

---

## Assumptions & Open Questions

Every ambiguity is resolved or recorded here — nothing is left silently unclear.

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Dashboard ACL = espelho folha/benefício | `getStats(String login)` via `Authentication.getName()` no controller; service resolve `UsuarioLookupPort` + `OrganogramaAcessoPort` | Padrão existente em `FolhaPagamentoController` | y |
| Empty-set restrito no dashboard | Stats zerados/vazios (mesma shape `DashboardStatsDTO`); **não** agregar folha/cadastros fora dos centros | MOD-09 / MODACL semantics | y |
| `acessoTotal=true` | Comportamento atual preservado (agregação global) | Flag explícita | y |
| SEM_FUNCIONARIO / SEM_NO | Stats zerados/vazios (deny implícito, sem 403 no GET stats) | Consistente com listagens vazias em folha restrita | y |
| Port consulta folha | `FolhaConsultaPort` em `folha.port` — agregações read-only usadas pelo dashboard (primitivos + DTOs em `folha.port` ou `dashboard.api`, **não** entities) | Modelo `BeneficioConsultaPort` | y |
| Port stats cadastros | Estender contrato existente ou `CadastrosConsultaPort`/`CadastrosStatsPort` — Design decide nome; spec exige **zero** `cadastros.infrastructure` em dashboard | AD-008 | n (Design) |
| Port escrita folha | `FolhaImportacaoPort` (ou nome equivalente) em `folha.port` — importação ADP delega persistência de folha/resumo; ownership de write em `folha.application` | State isolation princípio 8 | y |
| Lookup cadastros na importação | Usar/estender ports cadastros (`FuncionarioConsultaPort` + extensão rubrica/tipo se necessário) — **não** repos diretos | Sibling ports já existem parcialmente | n (Design) |
| Transação importação | `@Transactional` permanece no orquestrador importação **ou** move para folha via port — Design documenta ownership; rollback cross-domain SHALL ser documentado | Unscoped transaction P1 debt | n (Design) |
| Stats DTOs em `cadastros.api` | Permanecem neste fix (P2 mover para `dashboard.api` opcional); ports retornam shapes compatíveis | Menor blast radius | y |
| FE `/dashboard/stats` | Path e método inalterados; resposta filtrada server-side | Brownfield FE mínimo | y |
| ArchUnit AD-009 | Adicionar regras simétricas às de folha/benefícios; remover string/comentário allowlist; registrar AD-010 superseding em STATE | Fechamento explícito da dívida | y |
| Sequencing | Execute após sibling ACL fix (já PASS); não depende de commits do usuário para especificar | Working tree uncommitted OK | y |

**Open questions:** none blocking Specify — nome exato das ports cadastros estendidas e ownership transacional importação ficam **Agent's Discretion no Design** (defaults acima).

**Evidence anchors (codebase, 2026-07-27):**

| Issue | Location | Present? |
| ----- | -------- | -------- |
| Dashboard reach-through | `dashboard/application/DashboardService.java` L14–17, L41–45 — `FolhaPagamentoRepository`, `FuncionarioRepository`, `RubricaRepository`, `ResumoFolhaPagamentoRepository` | ✅ |
| Dashboard sem ACL | `dashboard/api/DashboardController.java` L21–25 — `getStats()` sem `Authentication` | ✅ |
| Import reach-through | `importacao/application/ImportacaoFolhaAdpService.java` L8–12, L44–48 — 5 repos folha+cadastros | ✅ |
| ArchUnit gap dashboard/import | `ModularArchitectureTest.java` — regras `*_application_must_not_access_foreign_infrastructure` **não** incluem `dashboard..` nem `importacao..` | ✅ |
| ACL pattern OK | `folha/application/FolhaPagamentoService.java` — `aplicarFiltroAcesso` + `OrganogramaAcessoPort` | ✅ (referência) |
| Port modelo OK | `beneficios/port/BeneficioConsultaPort.java` — primitivos, sem entity | ✅ (referência) |
| Dashboard já usa benefício port | `DashboardService` L43 — `BeneficioConsultaPort` | ✅ |

**Implicit-requirement dimensions (Large):**

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | Preservar validações existentes de importação ADP; ports SHALL NOT enfraquecer rejeição de arquivo inválido |
| Failure / partial-failure | Importação: falha na port folha SHALL propagar rollback documentado; dashboard: erro de port SHALL falhar request (sem stats parciais silenciosos cross-domain) |
| Idempotency / retry | Preservar semântica atual de substituição por competência na importação ADP (Design documenta) |
| Auth boundaries | Dashboard stats scoped por login; importação ADP permanece endpoint autenticado existente (sem mudança de matcher) |
| Concurrency / ordering | N/A — sem novo estado concorrente |
| Data lifecycle / expiry | N/A |
| Observability | Preservar/estender `DomainLogging` prefix `domain=` nos services refatorados |
| External-dependency failure | N/A |
| State-transition integrity | N/A |

---

## User Stories

### P1: ACL no dashboard ⭐ MVP

**User Story**: Como operador com acesso restrito ao organograma, quero que `GET /dashboard/stats` retorne apenas agregações dos centros de custo que posso acessar, para que totais globais de folha não vazem para perfis restritos.

**Why P1**: P0 segurança — único endpoint agregador cross-domain sem ACL; folha/benefícios já filtram.

**Acceptance Criteria**:

1. (MODBH-01) WHEN `GET /dashboard/stats` for invocado por usuário autenticado THEN o controller SHALL passar o login (`Authentication.getName()`) ao application service
2. (MODBH-02) WHEN usuário tiver `acessoTotal=false`, `temFuncionarioVinculado=true`, `temNoOrganograma=true` e `centrosCustoIds` **vazio** THEN o sistema SHALL retornar `DashboardStatsDTO` com totais numéricos zero e listas vazias (shape idêntico) e SHALL NOT incluir dados agregados de funcionários/centros fora do escopo
3. (MODBH-03) WHEN usuário tiver `motivoNegacao` SEM_FUNCIONARIO ou SEM_NO_ORGANOGRAMA THEN o sistema SHALL retornar stats zerados/vazios (mesma regra MODBH-02)
4. (MODBH-04) WHEN usuário tiver `acessoTotal=true` THEN o sistema SHALL preservar agregação global equivalente ao comportamento pré-refator (regressão zero em fixture de total access)
5. (MODBH-05) WHEN usuário restrito tiver `centrosCustoIds` não vazio THEN agregações SHALL considerar **somente** funcionários/lançamentos cujo centro de custo ∈ conjunto acessível (incluindo descendentes já resolvidos pela port)
6. (MODBH-06) WHEN testes unitários de `DashboardService` forem executados THEN SHALL existir cobertura restrito+empty, SEM_FUNCIONARIO, e total-access que falhariam se stats globais fossem retornados sem filtro

**Independent Test**: Mock `OrganogramaAcessoPort` restricted+empty → stats zerados; total-access → valores não filtrados vs baseline; verify agregadores unscoped não invocados sob restrito+empty.

---

### P1: FolhaConsultaPort — agregações read-only ⭐ MVP

**User Story**: Como mantenedor do monólito, quero uma port read-only de consultas/agregações de folha, para que consumidores cross-domain não dependam de `folha.infrastructure`.

**Why P1**: Pré-requisito para fechar dashboard reach-through com contrato explícito (princípio 5/8).

**Acceptance Criteria**:

1. (MODBH-07) WHEN o backend for inspecionado THEN SHALL existir interface pública em `folha.port` (ex.: `FolhaConsultaPort`) com métodos suficientes para `DashboardService` calcular stats **sem** importar `folha.infrastructure` ou `folha.domain` entities
2. (MODBH-08) WHEN a port for inspecionada THEN parâmetros e retornos SHALL usar tipos primitivos, `BigDecimal`, `LocalDate`, records/DTOs de contrato — **não** `@Entity` JPA
3. (MODBH-09) WHEN adapter for implementado THEN SHALL residir em `folha.application` e delegar a repositories **do domínio folha** apenas
4. (MODBH-10) WHEN testes unitários do adapter forem executados THEN SHALL cobrir ao menos happy path de competência recente e competência ausente (empty)

**Independent Test**: Adapter test verde; `DashboardService` compila sem imports `folha.infrastructure.*`.

---

### P1: Port cadastros read-only para dashboard ⭐ MVP

**User Story**: Como mantenedor, quero que o dashboard obtenha contagens/ dimensões de cadastros via port, para eliminar `cadastros.infrastructure` em `dashboard.application`.

**Why P1**: Segundo eixo do reach-through dashboard (funcionário/rubrica/stats por dimensão).

**Acceptance Criteria**:

1. (MODBH-11) WHEN `DashboardService` for compilado THEN zero imports de `cadastros.infrastructure..` SHALL permanecer
2. (MODBH-12) WHEN dados de cadastros forem necessários ao dashboard THEN consumo SHALL ocorrer via contrato em `cadastros.port` (interface existente estendida ou nova port stats — Design decide) com retornos sem `@Entity`
3. (MODBH-13) WHEN testes do consumidor dashboard forem executados THEN mocks SHALL usar a port, não repositories cadastros

**Independent Test**: `rg 'cadastros\.infrastructure' backend/src/main/java/**/dashboard/application` → zero matches.

---

### P1: Refatorar DashboardService — zero foreign infra ⭐ MVP

**User Story**: Como arquiteto, quero `DashboardService` dependendo apenas de ports (`BeneficioConsultaPort`, `FolhaConsultaPort`, cadastros port, `OrganogramaAcessoPort`, `UsuarioLookupPort`), para completar isolamento AD-008 no dashboard.

**Why P1**: Consolida MODBH-01…13; remove offender AD-009.

**Acceptance Criteria**:

1. (MODBH-14) WHEN `DashboardService` for inspecionado THEN SHALL NOT declarar campos de tipo `*Repository` de domínio folha ou cadastros
2. (MODBH-15) WHEN `DashboardService` for inspecionado THEN SHALL injetar `OrganogramaAcessoPort` e `UsuarioLookupPort` (ou equivalente auth lookup já existente)
3. (MODBH-16) WHEN `mvn test` incluir `DashboardServiceTest` THEN testes SHALL passar com mocks de ports (atualizar testes existentes que mockam repos estrangeiros)
4. (MODBH-17) WHEN `./diversos/scripts/check-modular-compliance.sh` for executado THEN gates mandatory SHALL permanecer verdes (build BE/FE)

**Independent Test**: Suite dashboard tests verde; grep zero foreign infra em dashboard.application.

---

### P1: FolhaImportacaoPort — escrita delegada ⭐ MVP

**User Story**: Como mantenedor, quero que importação ADP persista folha/resumo via port de comando do domínio folha, para que `importacao.application` não escreva em tabelas folha via repos estrangeiros.

**Why P1**: Maior violação state isolation (5 repos cross-domain, ~575 LOC).

**Acceptance Criteria**:

1. (MODBH-18) WHEN o backend for inspecionado THEN SHALL existir port de escrita/comando em `folha.port` (ex.: `FolhaImportacaoPort`) encapsulando persistência de lançamentos e resumo de competência usada pela importação ADP
2. (MODBH-19) WHEN a port for inspecionada THEN contrato SHALL usar DTOs/commands de integração — **não** expor entities JPA na superfície
3. (MODBH-20) WHEN adapter for implementado THEN SHALL residir em `folha.application`; ownership de regras de persistência folha SHALL ser documentado no Design (transação)
4. (MODBH-21) WHEN testes do adapter forem executados THEN SHALL cobrir persistência delegada (mock repos folha no teste do adapter, não no teste importação)

**Independent Test**: Adapter test verde; contrato command documentado.

---

### P1: Refatorar ImportacaoFolhaAdpService — zero foreign infra ⭐ MVP

**User Story**: Como mantenedor, quero que importação ADP faça parse/orquestração e delegue lookup cadastros + escrita folha a ports, para fechar reach-through em importacao.

**Why P1**: Segundo offender AD-009; bloqueia ArchUnit completo.

**Acceptance Criteria**:

1. (MODBH-22) WHEN `ImportacaoFolhaAdpService` for compilado THEN zero imports de `folha.infrastructure..` e `cadastros.infrastructure..` SHALL permanecer
2. (MODBH-23) WHEN lookup de funcionário/rubrica/tipo for necessário THEN SHALL usar ports cadastros (existentes ou estendidas no Design) — não repositories
3. (MODBH-24) WHEN persistência de folha/resumo ocorrer THEN SHALL invocar exclusivamente `FolhaImportacaoPort` (ou nome Design)
4. (MODBH-25) WHEN importação ADP válida for processada THEN resultado funcional SHALL ser equivalente ao baseline (mesmos campos de resposta HTTP/contrato existente — sem breaking change de API)
5. (MODBH-26) WHEN arquivo inválido for enviado THEN SHALL preservar rejeição/erro existente (sem relaxar validação)

**Independent Test**: `rg '\.infrastructure\.' backend/src/main/java/**/importacao/application` → zero foreign; testes importação existentes ou novos unitários verdes.

---

### P1: ArchUnit completo — supersede AD-009 ⭐ MVP

**User Story**: Como mantenedor, quero regras ArchUnit simétricas para dashboard e importacao application layers, para que regressões de reach-through sejam bloqueadas em CI.

**Why P1**: Fecha dívida AD-009; garante sustentabilidade do fix.

**Acceptance Criteria**:

1. (MODBH-27) WHEN `ModularArchitectureTest` for executado THEN SHALL existir regra `dashboard_application_must_not_access_foreign_infrastructure` proibindo `..dashboard..application..` → infra de outros domínios (same-domain OK se existir)
2. (MODBH-28) WHEN `ModularArchitectureTest` for executado THEN SHALL existir regra equivalente para `..importacao..application..`
3. (MODBH-29) WHEN `mvn test -Dtest=ModularArchitectureTest` for executado THEN SHALL exit 0 com **zero** violations
4. (MODBH-30) WHEN AD-009 for superseded THEN `_docs/specs/STATE.md` SHALL registrar AD-010 (ou amendment AD-009 status superseded) documentando allowlist removida

**Independent Test**: ArchUnit verde; comentário AD-009 allowlist ausente ou marcado superseded.

---

### P2: Higiene de contratos dashboard (Stats DTOs)

**User Story**: Como mantenedor, quero DTOs de agregação do dashboard no pacote correto, para reduzir acoplamento semântico cadastros → dashboard.

**Why P2**: Decomposition Pattern 2; não bloqueia MVP boundary hardening.

**Acceptance Criteria**:

1. (MODBH-31) WHEN stats DTOs usados exclusivamente pelo dashboard forem identificados THEN Design MAY mover de `cadastros.api.*StatsDTO` para `dashboard.api` (ou subpacote) sem breaking JSON (mesmos nomes de campos)
2. (MODBH-32) WHEN movidos THEN imports em `DashboardStatsDTO` SHALL apontar para novo pacote; FE OpenAPI/types regenerados **somente se** contrato JSON mudar (preferir pacote Java-only sem mudança wire)

**Independent Test**: Build verde; OpenAPI diff sem breaking fields se wire inalterado.

---

### P3: Gate estático complementar (opcional)

**User Story**: Como mantenedor, quero grep/gate no compliance script para dashboard/importacao foreign infra, para defesa em profundidade além ArchUnit.

**Why P3**: Nice-to-have; ArchUnit já cobre MODBH-27–29.

**Acceptance Criteria**:

1. (MODBH-33) WHEN `check-modular-compliance.sh` for estendido (opcional) THEN SHALL falhar se `dashboard.application` ou `importacao.application` importar `*.infrastructure.*` de outro domínio

**Independent Test**: Script exit 1 em violação artificial; exit 0 no tree limpo.

---

## Edge Cases

- WHEN não existir resumo de folha recente THEN dashboard SHALL retornar stats zerados/vazios (comportamento atual preservado), aplicando ACL antes de decidir empty global vs scoped
- WHEN usuário autenticado não existir em `UsuarioLookupPort` THEN SHALL falhar de forma segura (4xx/empty — Design alinha com folha; não retornar stats globais)
- WHEN importação ADP referenciar funcionário/rubrica inexistente THEN SHALL preservar erro agregado existente via ports (sem swallow)
- WHEN port folha consulta receber competência sem lançamentos THEN SHALL retornar zeros/empty collections (não null pointer em dashboard)
- WHEN `BeneficioConsultaPort` retornar zero benefícios THEN dashboard stats SHALL continuar coerentes (já usa port — não regressão)

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| MODBH-01 | P1: ACL dashboard | Design | Done |
| MODBH-02 | P1: ACL dashboard | Design | Done |
| MODBH-03 | P1: ACL dashboard | Design | Done |
| MODBH-04 | P1: ACL dashboard | Design | Done |
| MODBH-05 | P1: ACL dashboard | Design | Done |
| MODBH-06 | P1: ACL dashboard | Tasks | Done |
| MODBH-07 | P1: FolhaConsultaPort | Design | Done |
| MODBH-08 | P1: FolhaConsultaPort | Design | Done |
| MODBH-09 | P1: FolhaConsultaPort | Tasks | Done |
| MODBH-10 | P1: FolhaConsultaPort | Tasks | Done |
| MODBH-11 | P1: Cadastros port dashboard | Design | Done |
| MODBH-12 | P1: Cadastros port dashboard | Design | Done |
| MODBH-13 | P1: Cadastros port dashboard | Tasks | Done |
| MODBH-14 | P1: Refator DashboardService | Tasks | Done |
| MODBH-15 | P1: Refator DashboardService | Tasks | Done |
| MODBH-16 | P1: Refator DashboardService | Tasks | Done |
| MODBH-17 | P1: Refator DashboardService | Execute | Done |
| MODBH-18 | P1: FolhaImportacaoPort | Design | Done |
| MODBH-19 | P1: FolhaImportacaoPort | Design | Done |
| MODBH-20 | P1: FolhaImportacaoPort | Design | Done |
| MODBH-21 | P1: FolhaImportacaoPort | Tasks | Done |
| MODBH-22 | P1: Refator ImportacaoFolhaAdp | Tasks | Done |
| MODBH-23 | P1: Refator ImportacaoFolhaAdp | Tasks | Done |
| MODBH-24 | P1: Refator ImportacaoFolhaAdp | Tasks | Done |
| MODBH-25 | P1: Refator ImportacaoFolhaAdp | Execute | Done |
| MODBH-26 | P1: Refator ImportacaoFolhaAdp | Tasks | Done |
| MODBH-27 | P1: ArchUnit AD-009 | Tasks | Done |
| MODBH-28 | P1: ArchUnit AD-009 | Tasks | Done |
| MODBH-29 | P1: ArchUnit AD-009 | Execute | Done |
| MODBH-30 | P1: ArchUnit AD-009 | Execute | Done |
| MODBH-31 | P2: Stats DTOs | Design | Done |
| MODBH-32 | P2: Stats DTOs | Tasks | Done |
| MODBH-33 | P3: Compliance grep | Tasks | Deferred |

**Coverage:** 33 total, 33 mapped, 0 unmapped

---

## Success Criteria

- [x] `dashboard.application` e `importacao.application` com **zero** dependência de infraestrutura estrangeira (grep + ArchUnit)
- [x] `GET /dashboard/stats` respeita ACL MOD-09 (restrito+empty ⇒ stats zerados; total access preservado)
- [x] Ports folha (consulta + importação) publicadas em `folha.port` sem entities na superfície
- [x] `ModularArchitectureTest` verde incluindo regras dashboard/importacao; AD-009 superseded documentado
- [x] `mvn test` backend verde (suite ≥ baseline 94 testes); `npm run build` FE exit 0
- [ ] Independent Verifier PASS em `_docs/specs/features/modular-boundary-hardening/validation.md`
- [x] Sem regressão nos Verifiers PASS de `modular-monolith-fix` e `modular-acl-security-fix`
