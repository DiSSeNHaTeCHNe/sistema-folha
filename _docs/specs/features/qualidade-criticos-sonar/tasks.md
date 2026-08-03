# Qualidade — 7 CRITICAL + S8688 Tasks

## Execution Protocol (MANDATORY — do not skip)

Implement these tasks with the `tlc-spec-driven` skill: activate it by name and follow its Execute flow and Critical Rules. The skill is the source of truth (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor). If the skill cannot be activated, STOP and tell the user.

**Design**: `_docs/specs/features/qualidade-criticos-sonar/design.md`
**Status**: Complete (14/14 tasks committed — incl. emenda B2 QUAL-12)

---

## Test Coverage Matrix

> Gerada de: `_docs/specs/TESTING.md`, `AGENTS.md`/`CLAUDE.md`, JaCoCo (AD-014, meta 95%), amostragem de `backend/src/test/java/**` e `frontend/src/**/*.test.tsx`. Guidelines encontradas: `TESTING.md`, `.claude/CLAUDE.md`, `AGENTS.md`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| --- | --- | --- | --- | --- |
| Serviço/adapter backend (application) | unit | Todos os branches; 1:1 aos ACs; comportamento preservado; +teste com `Clock.fixed` onde há tempo | `backend/src/test/java/**/*Test.java` | `cd backend && mvn test` |
| Entidade JPA / Config backend | none | build gate apenas (sem lógica testável nova) | — | `cd backend && mvn -q -DskipTests compile` |
| Componente/página React | unit (RTL) | Testes existentes permanecem verdes; comportamento/render idêntico | `frontend/src/**/*.test.tsx` | `cd frontend && npm run test:coverage` |

## Gate Check Commands

> Confirmar antes do Execute.

| Gate Level | When to Use | Command |
| --- | --- | --- |
| Quick | Tarefa com testes unitários backend | `cd backend && mvn test` (ou `-Dtest=ClasseTest`) |
| Quick (FE) | Tarefa com testes React | `cd frontend && npm run test:coverage` |
| Build | Config/entidade, ou fim de fase | `cd backend && mvn test` **e** `cd frontend && npm run build && npm run lint` |
| Sonar (final) | Confirmar ACs de gate (QUAL-11) | `./diversos/scripts/sonar-analyze.sh` → CRITICAL=0, S8688=0, QG=OK |

---

## Execution Plan

### Phase 1: Foundation
```
T1
```
### Phase 2: S8688 — tempo testável (backend)
```
T2 → T3 → T4 → T5 → T6 → T7
```
### Phase 3: 7 CRITICAL — refactors
```
T8 → T9 → T10 → T11
```
### Phase 4: Verificação de gate
```
T12
```
### Phase 5: QUAL-12 — validação auth com Clock injetado (emenda B2)
```
T13 → T14
```

---

## Task Breakdown

### T1: Criar TimeConfig com Clock bean
**Status**: ✅ Complete — `f961a7b`
**What**: expor `@Bean Clock clock()` = `Clock.systemDefaultZone()`.
**Where**: `backend/src/main/java/br/com/techne/sistemafolha/config/TimeConfig.java` (novo)
**Depends on**: None
**Reuses**: convenção `Clock.systemDefaultZone()` existente
**Requirement**: QUAL-07
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Bean `Clock` disponível no contexto Spring
- [x] `cd backend && mvn -q -DskipTests compile` passa
**Tests**: none · **Gate**: build

### T2: JwtService — injetar Clock
**Status**: ✅ Complete — `65e04c6`
**What**: injetar `Clock` e trocar `.now()` por `.now(clock)`.
**Where**: `security/JwtService.java` (+ teste)
**Depends on**: T1
**Requirement**: QUAL-07, QUAL-08
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Nenhum `.now()` sem arg em `JwtService`
- [x] Testes de `JwtService` verdes (comportamento idêntico)
- [x] Gate: `cd backend && mvn test -Dtest=JwtServiceTest`
**Tests**: unit · **Gate**: quick

### T3: RefreshTokenService — injetar Clock + teste com Clock.fixed
**Status**: ✅ Complete — `6fe0001`
**What**: injetar `Clock`; adicionar teste de expiração com `Clock.fixed` (antes/depois da expiração).
**Where**: `auth/application/RefreshTokenService.java` (+ `RefreshTokenServiceTest`)
**Depends on**: T1
**Requirement**: QUAL-07, QUAL-08, QUAL-10
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Nenhum `.now()` sem arg no serviço
- [x] Teste com `Clock.fixed`: token válido em instante T- e expirado em T+ (assert do resultado, não do mock)
- [x] Gate: `cd backend && mvn test -Dtest=RefreshTokenServiceTest`
**Tests**: unit · **Gate**: quick

### T4: AuthenticationService — injetar Clock
**Status**: ✅ Complete — `03c6ce2`
**What**: injetar `Clock`, trocar `.now()`.
**Where**: `auth/application/AuthenticationService.java` (+ teste)
**Depends on**: T1
**Requirement**: QUAL-07, QUAL-08
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Nenhum `.now()` sem arg no serviço
- [x] Testes de `AuthenticationService` verdes
- [x] Gate: `cd backend && mvn test -Dtest=AuthenticationServiceTest`
**Tests**: unit · **Gate**: quick

### T5: ApiKeyService — migrar band-aid → Clock injetado + teste Clock.fixed
**Status**: ✅ Complete — `a1c4c2d`
**What**: trocar `now(Clock.systemDefaultZone())` (quick-task 010) por `now(clock)` injetado; teste de expiração de API key com `Clock.fixed`.
**Where**: `auth/application/ApiKeyService.java` (+ `ApiKeyServiceTest`)
**Depends on**: T1
**Requirement**: QUAL-07, QUAL-08, QUAL-10
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] `ApiKeyService` usa `clock` injetado (sem `systemDefaultZone()` inline)
- [x] Teste com `Clock.fixed`: expiração determinística
- [x] Gate: `cd backend && mvn test -Dtest=ApiKeyServiceTest`
**Tests**: unit · **Gate**: quick

### T6: ImportacaoFolhaAdpService — injetar Clock + extrair constante S1192
**Status**: ✅ Complete — `276ab07`
**What**: injetar `Clock` (troca `.now()`); extrair literal "Filial 0065 TECHNE - EDUCACAO" para constante em `inicializarMapaEmpresas` (S1192).
**Where**: `importacao/application/ImportacaoFolhaAdpService.java` (+ testes existentes)
**Depends on**: T1
**Requirement**: QUAL-06, QUAL-07, QUAL-08
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Nenhum `.now()` sem arg; literal duplicado extraído
- [x] 61 testes de `ImportacaoFolhaAdpServiceTest` verdes (sem regressão)
- [x] Gate: `cd backend && mvn test -Dtest=ImportacaoFolhaAdpServiceTest`
**Tests**: unit · **Gate**: quick

### T7: Entidades JPA — zona explícita (Opção A)
**Status**: ✅ Complete — `b4979c3`
**What**: trocar `.now()` por `.now(Clock.systemDefaultZone())` nos `@PrePersist`/`@PreUpdate`.
**Where**: `auth/domain/RefreshToken.java`, `cadastros/domain/Funcionario.java`, `organograma/domain/{FuncionarioOrganograma,NoOrganograma,CentroCustoOrganograma}.java`, `beneficios/domain/{BeneficioMensal,TipoBeneficio}.java`
**Depends on**: T1
**Requirement**: QUAL-08, QUAL-09
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Nenhum `.now()` sem arg nas entidades (grep = 0 no backend após T2–T7)
- [x] Instante gravado idêntico (comportamento preservado)
- [x] Gate build: `cd backend && mvn test` (suíte completa verde — fim da fase 2)
**Tests**: none · **Gate**: build

### T8: RubricaService — extrair constante S1192
**Status**: ✅ Complete — `cd98635`
**What**: constante para "Rubrica não encontrada com ID: ".
**Where**: `cadastros/application/RubricaService.java` (+ testes existentes)
**Depends on**: None
**Requirement**: QUAL-05
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Literal extraído; mensagem de exceção byte-a-byte idêntica
- [x] Gate: `cd backend && mvn test -Dtest=RubricaServiceTest`
**Tests**: unit · **Gate**: quick

### T9: ImportacaoBeneficioMensalService.importar — reduzir CC (S3776)
**Status**: ✅ Complete — `6ec1703`
**What**: extrair sub-métodos (parse/validação/persistência) para CC ≤ 15, preservando comportamento.
**Where**: `beneficios/application/ImportacaoBeneficioMensalService.java` (+ testes existentes)
**Depends on**: None
**Requirement**: QUAL-01
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Complexidade cognitiva do método ≤ 15 (Sonar não flagga S3776)
- [x] Testes existentes de importação verdes (mesmo resultado)
- [x] Gate: `cd backend && mvn test -Dtest=ImportacaoBeneficioMensalServiceTest`
**Tests**: unit · **Gate**: quick

### T10: FE Organograma/index.tsx — S3776 + S2004
**Status**: ✅ Complete — `6a85ea9`
**What**: extrair função (l.408, CC≤15) e desaninhar callbacks (l.465,470, aninhamento≤4).
**Where**: `frontend/src/pages/Organograma/index.tsx` (+ testes existentes)
**Depends on**: None
**Requirement**: QUAL-02, QUAL-03
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Sonar não flagga S3776/S2004 nas linhas alvo
- [x] Testes RTL de Organograma verdes (render/comportamento idêntico)
- [x] Gate: `cd frontend && npm run test:coverage` + `npm run build`
**Tests**: unit · **Gate**: full

### T11: FE Usuarios/index.tsx — S2004
**Status**: ✅ Complete — `097c5ee`
**What**: desaninhar callback (l.621, aninhamento≤4).
**Where**: `frontend/src/pages/Usuarios/index.tsx` (+ testes existentes)
**Depends on**: None
**Requirement**: QUAL-04
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] Sonar não flagga S2004 na linha alvo
- [x] Testes RTL de Usuarios verdes
- [x] Gate: `cd frontend && npm run test:coverage` + `npm run build`
**Tests**: unit · **Gate**: full

### T12: Confirmação de gate (Sonar) — QUAL-11
**Status**: ✅ Complete — `c9b1317`
**What**: reanálise Sonar + validação dos ACs de gate.
**Where**: — (execução de script)
**Depends on**: T2–T11
**Requirement**: QUAL-11 (+ verificação QUAL-01..10)
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] `./diversos/scripts/sonar-analyze.sh` → **CRITICAL=0**, **S8688=0** (backend), **new_violations=0**, **QG=OK**
- [x] Cobertura ≥ 95% linha/branch (backend); suíte completa verde
**Tests**: none · **Gate**: build + Sonar
**Commit**: `refactor(quality): reduce cognitive complexity, extract constants, inject Clock`

### T13: RefreshTokenService — expiração via Clock injetado (QUAL-12) + testes Clock.fixed
**Status**: ✅ Complete — `0df5dcd`
**What**: em `validarRefreshToken`, checar expiração com `LocalDateTime.now(clock).isAfter(dataExpiracao)` e `revogado` inline — **não** delegar a `isValido()`. Reescrever teste `criarRefreshToken_comClockFixo_*`: instância A (clock antes do TTL) → `validarRefreshToken` true; instância B (clock após TTL, mesma entidade) → false — **sem** `setDataExpiracao`.
**Where**: `auth/application/RefreshTokenService.java`, `RefreshTokenServiceTest.java`
**Depends on**: T1–T3 (Clock bean + serviço)
**Requirement**: QUAL-12, QUAL-10
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] `validarRefreshToken` não chama `refreshToken.isValido()` / `isExpirado()`
- [x] Semântica `isAfter` preservada (válido no instante exato)
- [x] Teste avança `Clock.fixed` entre instâncias de serviço (sem mutar entidade)
- [x] Gate: `cd backend && mvn test -Dtest=RefreshTokenServiceTest`
**Tests**: unit · **Gate**: quick

### T14: ApiKeyService — expiração via Clock injetado (QUAL-12) + testes Clock.fixed
**Status**: ✅ Complete — `fe2f993`
**What**: em `autenticarPorChave`, checar expiração/revogação com `clock` injetado inline — **não** delegar a `isValida()`. Reescrever teste `criar_comClockFixo_*`: serviço clock antes → `autenticarPorChave` presente; serviço clock após TTL → empty — **sem** `setDataExpiracao`.
**Where**: `auth/application/ApiKeyService.java`, `ApiKeyServiceTest.java`
**Depends on**: T13 (padrão estabelecido; pode rodar em paralelo lógico mas sequencial na fase)
**Requirement**: QUAL-12, QUAL-10
**Tools**: MCP: NONE · Skill: NONE
**Done when**:
- [x] `autenticarPorChave` não chama `apiKey.isValida()` / `isExpirada()` para expiração
- [x] Teste avança `Clock.fixed` entre instâncias (sem mutar entidade)
- [x] Gate: `cd backend && mvn test -Dtest=ApiKeyServiceTest`
- [x] Gate build: `cd backend && mvn test` (fim fase 5)
**Tests**: unit · **Gate**: quick (T14 final: build)

---

## Phase Execution Map
```
Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5
Phase 1:  T1
Phase 2:  T2 → T3 → T4 → T5 → T6 → T7
Phase 3:  T8 → T9 → T10 → T11
Phase 4:  T12
Phase 5:  T13 → T14   (emenda B2 / QUAL-12)
```
Execução estritamente sequencial. Emenda B2: 2 tarefas → 1 worker.

---

## Validação pré-aprovação

### Check 1 — Granularidade
| Task | Escopo | Status |
| --- | --- | --- |
| T1 | 1 arquivo novo (bean) | ✅ Granular |
| T2 | 1 serviço | ✅ Granular |
| T3 | 1 serviço + teste | ✅ Granular |
| T4 | 1 serviço | ✅ Granular |
| T5 | 1 serviço + teste | ✅ Granular |
| T6 | 1 arquivo (2 edições cirúrgicas cohesas) | ✅ Granular |
| T7 | 7 entidades, 1 edição mecânica idêntica | ⚠️ OK (sweep coeso, mesma mudança trivial) |
| T8 | 1 serviço | ✅ Granular |
| T9 | 1 método | ✅ Granular |
| T10 | 1 arquivo FE | ✅ Granular |
| T11 | 1 arquivo FE | ✅ Granular |
| T12 | verificação | ✅ Granular |

### Check 2 — Diagrama × Definição
| Task | Depends on (corpo) | Diagrama | Status |
| --- | --- | --- | --- |
| T1 | None | início | ✅ |
| T2–T7 | T1 | T1 → fase 2 | ✅ |
| T8–T11 | None (fase 3, independentes entre si) | sequência fase 3 | ✅ |
| T12 | T2–T11 | fase 4 após fase 2/3 | ✅ |

*(T8–T11 são independentes; a seta na fase 3 indica ordem de execução sequencial, não dependência de dados — nenhuma depende de tarefa de fase posterior.)*

### Check 3 — Co-locação de testes
| Task | Camada | Matrix exige | Task diz | Status |
| --- | --- | --- | --- | --- |
| T1 | Config | none | none | ✅ |
| T2,T4,T6,T8,T9 | Serviço | unit | unit | ✅ |
| T3,T5 | Serviço (tempo) | unit (+Clock.fixed) | unit | ✅ |
| T7 | Entidade | none | none | ✅ |
| T10,T11 | Componente React | unit | unit | ✅ |
| T12 | Verificação | none | none | ✅ |

Todas as checagens passam.

---

## Requirement Coverage
| Requirement | Task(s) |
| --- | --- |
| QUAL-01 | T9 | 
| QUAL-02, QUAL-03 | T10 |
| QUAL-04 | T11 |
| QUAL-05 | T8 |
| QUAL-06 | T6 |
| QUAL-07 | T1–T6 |
| QUAL-08 | T2–T7 |
| QUAL-09 | T7 |
| QUAL-10 | T3, T5, T13, T14 |
| QUAL-11 | T12 |
| QUAL-12 | T13, T14 |

**12/12 requisitos mapeados a tarefas (QUAL-12 via T13–T14).**
