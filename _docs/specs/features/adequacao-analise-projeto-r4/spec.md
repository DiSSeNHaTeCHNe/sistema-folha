# Adequação da Análise de Projeto — R4 (Qualidade Sustentável) Specification

**Parent:** `_docs/specs/features/adequacao-analise-projeto-r3/spec.md` (merged @ `bcff5f9`)  
**Related:** `_docs/specs/CONCERNS.md`, `_docs/specs/TESTING.md`, `validation.md` R3 PASS @ `bcff5f9`, AD-004 (skills FE TARGET), AD-003 (harness versionado)  
**Complexity:** Large  
**Spec status:** Draft — Design 2026-07-29 (Specify confirmado)

## Problem Statement

A R3 fechou o **Quality Gate Sonar OK** (`new_coverage` **80.0%** exato, `new_violations` **0**, agregado **59.8%**), entregou MSW + **184** testes Vitest, refresh → **401**, Testcontainers ADP (código) e follow-ups CONCERNS (S2245, BeneficioMensal tx). Porém a entrega ficou **no limiar** do gate (zero margem), com **`new_branch_coverage` 62.5%** (gargalo futuro), **Playwright N/A** (AAP3-21), **integração ADP não executada live** no Verifier (Docker), **`_docs/specs/TESTING.md` e `backend/AGENTS.md` desatualizados** vs realidade pós-R3, e **sem CI remoto** — o próximo PR pode regredir QG sem detecção precoce.

A R4 muda o foco de **volume de cobertura** (R3) para **infraestrutura e confiança sustentável**: buffer de gate, E2E mínimo, docs canônicos sincronizados, hardening de testes de alto risco e evidência live da integração ADP quando Docker existir — **sem** nova maratona de page tests nem reset Sonar isolado sem valor.

## Goals

- [ ] Manter Sonar QG **OK** com **margem** (`new_coverage` ≥ **85%** meta interna; floor QG Sonar permanece 80%)
- [ ] Entregar **Playwright login smoke** (carryover AAP3-21)
- [ ] Sincronizar **`TESTING.md`**, **`backend/AGENTS.md`** e nota brownfield em **`frontend/AGENTS.md`** com baseline pós-R3
- [ ] **Evidência live** de `ImportacaoFolhaAdpIntegrationTest` quando Docker disponível; path N/A documentado quando ausente
- [ ] Hardening **cirúrgico** de testes auth/MSW (sem explosão de contagem Vitest)
- [ ] Registrar **baseline Sonar pós-merge R3** e procedimento de gate reproduzível em `validation.md` / script
- [ ] Atualizar `CONCERNS.md` e `validation.md` R4

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Nova maratona FE page tests só para Sonar (estilo fix-cycle-1 R3) | Retorno decrescente; R4 = sustentabilidade, não volume |
| Reset Sonar `PREVIOUS_VERSION` **sem** evidência de scan pós-merge + docs | Ops isolado; R4 exige scan documentado, não “reset mágico” |
| Adequação FE **completa** AD-004 (`src/features/`, TanStack Query, RFC 7807) | ROADMAP M3; R4 = docs + smoke E2E |
| Pipeline CI/CD remoto completo (GitHub Actions etc.) | ROADMAP M3 DevOps; R4 limita-se a script/gate local reproduzível |
| Zerar ~121 code smells ou subir agregado 60%→80% | Meta R4 = manter QG + buffer; agregado ≥ **59%** (regressão zero vs R3) |
| Backend Relatórios PDF / remoção `Beneficio` legado | Features de produto separadas |
| Refactor `ImportacaoFolhaAdpService` (CC 71) | Fora R4 |
| Feature `auth-api-keys` | Feature paralela (AD-013); não misturar escopo |
| Migrar `src/pages/` → `src/features/` | Refactor estrutural AD-004 completo |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| B1 — Baseline R4 | `main` @ `088a438` (pós-merge R3) como referência de métricas e docs | Último squash R3 + handoff STATE | y |
| B2 — Meta `new_coverage` | **≥ 85%** meta interna R4; QG Sonar continua exigindo **≥ 80%** | Margem ~5 pp vs R3 (80.0% exato); reduz risco de regressão no próximo PR | **y** |
| B3 — Playwright | **P1** — Playwright + **`page.route()`** mock de login (reusa `sampleLoginResponse` de `authHandlers.ts`); **sem** `webServer` backend | Custo baixo, estável em CI futuro, reutiliza shape R3; webServer exigiria BE+DB por E2E | **y** (agent) |
| B4 — ADP integration live | Gate **obrigatório quando Docker daemon UP**; skip documentado quando DOWN (mantém `@EnabledIf`) | Fecha gap Verifier R3; não bloqueia dev sem Docker | n |
| B5 — Hardening vs volume | ≤ **10** test cases **novos** no total R4 (BE+FE); prioridade qualidade de assertion | Evita replay fix-cycle-1; code-review R3 flagged weak assertions | y |
| B6 — Branch coverage | Elevar **`new_branch_coverage`** ≥ **70%** como meta interna (informativa; não condição QG Sonar atual) | R3: 62.5%; gargalo identificado pós-merge | y |
| B7 — Docs sync | `TESTING.md` + `backend/AGENTS.md` **obrigatórios**; `frontend/AGENTS.md` = nota AD-004 brownfield apenas | TESTING.md ainda descreve “5 tests, sem Vitest”; BE AGENTS diz “sem Testcontainers” | y |
| B8 — Branch git | `feat/adequacao-analise-projeto` a partir de `main` @ `088a438` | Convenção fix/r4 → branch base sem sufixo | y |
| B9 — Sonar baseline pós-merge | **Document-first:** scan + métricas em `validation.md` obrigatório; **reset `PREVIOUS_VERSION` somente P3 fallback** se hardening + ≤10 testes não atingirem 85% e QG regredir — task ops isolada, não default | Reset esconde dívida; hardening primeiro tem melhor custo-benefício | **y** (agent) |

**Open questions:** none — B2 confirmado pelo usuário; B3/B9 resolvidos pelo agente (2026-07-29).

### Implicit-requirement dimensions (Large)

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | N/A — R4 não adiciona endpoints |
| Failure / partial-failure | AAP4-08: ADP rollback; AAP4-04: Playwright skip se setup falhar |
| Idempotency / retry | N/A — testes/gates apenas |
| Auth boundaries | AAP4-07: teste service refresh expirado/revogado; MSW `API_BASE_URL` alinhado |
| Concurrency / ordering | N/A |
| Data lifecycle | N/A |
| Observability | N/A — sem novos logs |
| External-dependency failure | AAP4-08/09: Docker indisponível → N/A documentado; suite verde |
| State-transition integrity | N/A |

---

## User Stories

### P1: Gate sustentável — buffer e baseline pós-R3 ⭐ MVP

**User Story**: Como Tech Lead, quero margem acima do floor Sonar e baseline documentado pós-merge R3, para que o próximo feature não reabra dívida de `new_coverage` no limiar.

**Why P1**: R3 fechou em **80.0%** exato; code-review e Verifier flagged risco de regressão imediata.

**Acceptance Criteria**:

1. **(AAP4-01)** WHEN `./diversos/scripts/sonar-analyze.sh` completar em `main` pós-R4 THEN Quality Gate status SHALL ser **OK** (zero condições ERROR)
2. **(AAP4-02)** WHEN Sonar API consultar `new_coverage` THEN valor SHALL ser **≥ 85%** (meta interna R4)
3. **(AAP4-03)** WHEN Sonar API consultar `new_violations` THEN valor SHALL ser **0**
4. **(AAP4-04)** WHEN Sonar API consultar `coverage` (agregado) THEN valor SHALL ser **≥ 59%** (floor R3: 59.8%; regressão zero)
5. **(AAP4-05)** WHEN R4 fechar THEN `validation.md` SHALL registrar baseline pós-merge (`main` @ commit R4), métricas Sonar (`new_coverage`, `new_branch_coverage`, agregado) e comando de reprodução do gate

**Independent Test**: `sonar-analyze.sh` exit 0; Sonar API measures; `validation.md` baseline block.

---

### P1: Playwright login smoke ⭐ MVP

**User Story**: Como QA/mantenedor, quero **um** fluxo E2E de login executável via `npm run test:e2e`, como ponte para testes end-to-end futuros.

**Why P1**: AAP3-21 N/A na R3; CONCERNS lista E2E pendente.

**Acceptance Criteria**:

1. **(AAP4-06)** WHEN `frontend/package.json` for inspecionado pós-R4 THEN SHALL existir dependência Playwright (ou `@playwright/test`) e script **`test:e2e`**
2. **(AAP4-07)** WHEN `cd frontend && npm run test:e2e` rodar THEN SHALL existir **≥ 1** spec passando assertando: (a) heading/título da página login visível; (b) submit do formulário executado com **`page.route()`** mock de `POST */auth/login` retornando shape `LoginResponse` (reuso `sampleLoginResponse` de `authHandlers.ts`)
3. **(AAP4-08)** WHEN setup Playwright falhar por ambiente (browser não instalado) THEN `validation.md` SHALL documentar N/A com motivo; P1 restante não bloqueado se AAP4-01…05 PASS

**Independent Test**: `npm run test:e2e`; evidência em `validation.md`.

---

### P1: Docs canônicos sincronizados ⭐ MVP

**User Story**: Como agente/desenvolvedor, quero que `TESTING.md` e `AGENTS.md` por frente reflitam o harness real pós-R3, para não repetir decisões erradas (ex.: “sem Vitest”, “sem Testcontainers”).

**Why P1**: Code-review R3: `TESTING.md` stale; `backend/AGENTS.md` §4 incorreto; agents carregam esses arquivos na implementação.

**Acceptance Criteria**:

1. **(AAP4-09)** WHEN `_docs/specs/TESTING.md` for lido pós-R4 THEN SHALL documentar: Vitest **184+** cases, MSW harness, JaCoCo/Sonar scripts, Testcontainers ADP (Docker-gated), comandos gate atuais
2. **(AAP4-10)** WHEN `backend/AGENTS.md` §4 Testing for lido pós-R4 THEN SHALL mencionar Testcontainers, `ImportacaoFolhaAdpIntegrationTest`, `@EnabledIf` Docker, e contagem de testes **≥ 474**
3. **(AAP4-11)** WHEN `frontend/AGENTS.md` for lido pós-R4 THEN SHALL incluir nota **brownfield R3**: MSW isolado em testes HTTP (não global `setup.ts`); Playwright smoke R4; TARGET AD-004 unchanged

**Independent Test**: Diff docs vs `package.json`, `pom.xml`, `npm test` / `mvn test` counts.

---

### P2: Integração ADP — evidência live ⭐

**User Story**: Como mantenedor, quero evidência de que `ImportacaoFolhaAdpIntegrationTest` passa com Docker, complementando o skip path já verificado na R3.

**Why P2**: AAP3-11 N/A no Verifier; CONCERNS “Mitigated” mas não “Resolved”.

**Acceptance Criteria**:

1. **(AAP4-12)** WHEN Docker daemon estiver disponível e `mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` rodar THEN gate SHALL exit **0** e persistir **≥ 1** linha antes de rollback (`@Transactional`)
2. **(AAP4-13)** WHEN Docker indisponível THEN `mvn test` suite completa SHALL permanecer verde (1 skip); `validation.md` SHALL registrar N/A com comando de verificação Docker usado

**Independent Test**: `mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` com/sem Docker; evidência em validation.

---

### P2: Hardening auth + MSW (cirúrgico) ⭐

**User Story**: Como mantenedor, quero fechar gaps de teste de alto risco identificados no code-review R3, sem inflar a suíte com smokes redundantes.

**Why P2**: Gaps reais: refresh expirado no service; assertions fracas em `api.test.ts`; drift `API_BASE_URL` MSW vs `api.ts`.

**Acceptance Criteria**:

1. **(AAP4-14)** WHEN `AuthenticationServiceTest` rodar THEN SHALL existir test case com `validarRefreshToken` retornando **false** assertando **`RefreshTokenInvalidoException`** (não apenas token ausente)
2. **(AAP4-15)** WHEN testes de falha HTTP em `api.test.ts` rodarem THEN assertions SHALL incluir **`response.status === 401`** (ou mensagem spec-defined) — não **`rejects.toBeDefined()`** isolado — em **≥ 3** casos de falha refresh/unauthorized previamente fracos
3. **(AAP4-16)** WHEN handlers MSW e `api.ts` forem inspecionados THEN **`API_BASE_URL`** SHALL ter **fonte única** compartilhada (const ou env) entre client axios e `authHandlers.ts`
4. **(AAP4-17)** WHEN contagem de test cases novos na R4 for medida THEN total de **`it(`/`test(` adicionados** SHALL ser **≤ 10** (hardening only)

**Independent Test**: `mvn test -Dtest=AuthenticationServiceTest`; `npm test -- api.test`; grep `API_BASE_URL`.

---

### P2: Branch coverage — meta interna ⭐

**User Story**: Como Tech Lead, quero reduzir o risco de QG futuro elevando `new_branch_coverage`, focando branches já tocadas na R3 (auth + top FE pages), não novos arquivos.

**Why P2**: R3: `new_branch_coverage` **62.5%** vs linhas ~90%; Sonar compõe métricas com branches.

**Acceptance Criteria**:

1. **(AAP4-18)** WHEN Sonar API consultar `new_branch_coverage` pós-R4 THEN valor SHALL ser **≥ 70%** (meta interna; informativa se QG OK em AAP4-01)
2. **(AAP4-19)** WHEN hardening de branch coverage exigir novos testes THEN SHALL limitar-se a arquivos já no leak-period R3: `api.test.ts`, `AuthenticationServiceTest`, no máximo **1** page test existente (ex.: `Login.test.tsx` ou `FolhaPagamento.test.tsx`) — **sem** novos arquivos de page test

**Independent Test**: Sonar API `new_branch_coverage`; diff R4 file list.

---

### P2: Gates, regressão e CONCERNS ⭐

**User Story**: Como desenvolvedor, quero gates verdes e CONCERNS atualizado após R4.

**Acceptance Criteria**:

1. **(AAP4-20)** WHEN `cd backend && mvn test` THEN **0** falhas; contagem **≥ 474** (sem deleção silenciosa vs R3)
2. **(AAP4-21)** WHEN `bash diversos/scripts/check-jacoco-thresholds.sh` THEN exit **0**
3. **(AAP4-22)** WHEN `cd frontend && npm test` THEN **≥ 184** test cases passando (floor R3; novos ≤10)
4. **(AAP4-23)** WHEN R4 fechar THEN `_docs/specs/CONCERNS.md` SHALL atualizar: E2E Playwright (Resolved ou Mitigated); ADP integration (Resolved se Docker live PASS); Test Coverage / TESTING.md pointer
5. **(AAP4-24)** WHEN `ModularArchitectureTest` rodar THEN zero violações AD-010

**Independent Test**: Full gate suite; CONCERNS diff; ArchUnit.

---

### P3: Script gate unificado (local) — opcional

**User Story**: Como desenvolvedor, quero um script único que rode BE + FE + JaCoCo + Sonar (e opcionalmente Docker check + E2E), reproduzível antes de merge.

**Why P3**: Sem CI remoto; reduz dependência de memória do agente.

**Acceptance Criteria**:

1. **(AAP4-25)** WHEN `./diversos/scripts/gate-r4-local.sh` (ou extensão de script existente) rodar com flags documentadas THEN SHALL executar: `mvn test`, `npm test`, `check-jacoco-thresholds.sh`, e reportar exit code agregado **0** quando todos passarem
2. **(AAP4-26)** WHEN flag `--docker` for passada e Docker UP THEN script SHALL incluir `ImportacaoFolhaAdpIntegrationTest` no gate; quando DOWN SHALL emitir warning e continuar (exit 0 se restante verde)

**Independent Test**: Script exit codes; README ou `TESTING.md` referência.

---

## Edge Cases

- WHEN meta 85% `new_coverage` conflitar com ≤10 testes novos THEN priorizar **hardening + branch paths existentes** antes de expandir page tests; se impossível, registrar SPEC_DEVIATION e propor B9 (baseline reset) — **parar e perguntar**
- WHEN Playwright browsers não instalados THEN AAP4-08 N/A; documentar `npx playwright install`
- WHEN Docker UP mas Testcontainers falhar (porta, pull) THEN falha é **bloqueante** para AAP4-12; documentar erro em validation
- WHEN `new_coverage` ≥85% mas `new_branch_coverage` &lt;70% THEN R4 pode PASS com ressalva em validation (AAP4-18 informativo) se AAP4-01 OK
- WHEN docs sync contradizer AD-004 TARGET THEN nota brownfield explícita (AAP4-11); não migrar estrutura FE

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AAP4-01 | P1: Gate QG OK | Design | Pending |
| AAP4-02 | P1: new_coverage ≥85% | Design | Pending |
| AAP4-03 | P1: new_violations 0 | Design | Pending |
| AAP4-04 | P1: agregado ≥59% | Design | Pending |
| AAP4-05 | P1: validation baseline | Design | Pending |
| AAP4-06 | P1: Playwright deps | Design | Pending |
| AAP4-07 | P1: Playwright spec | Design | Pending |
| AAP4-08 | P1: Playwright N/A | Design | Pending |
| AAP4-09 | P1: TESTING.md | Design | Pending |
| AAP4-10 | P1: backend AGENTS | Design | Pending |
| AAP4-11 | P1: frontend AGENTS note | Design | Pending |
| AAP4-12 | P2: ADP live | Design | Pending |
| AAP4-13 | P2: ADP N/A path | Design | Pending |
| AAP4-14 | P2: Auth service test | Design | Pending |
| AAP4-15 | P2: api.test assertions | Design | Pending |
| AAP4-16 | P2: API_BASE_URL | Design | Pending |
| AAP4-17 | P2: test budget ≤10 | Design | Pending |
| AAP4-18 | P2: branch coverage 70% | Design | Pending |
| AAP4-19 | P2: branch scope limit | Design | Pending |
| AAP4-20 | P2: mvn test | Design | Pending |
| AAP4-21 | P2: JaCoCo | Design | Pending |
| AAP4-22 | P2: Vitest floor | Design | Pending |
| AAP4-23 | P2: CONCERNS | Design | Pending |
| AAP4-24 | P2: ArchUnit | Design | Pending |
| AAP4-25 | P3: gate script | Design | Pending |
| AAP4-26 | P3: gate docker flag | Design | Pending |

**Coverage:** 26 total, 26 mapped, 0 unmapped

---

## Success Criteria

- [ ] Sonar QG **OK**; `new_coverage` ≥ **85%**; `new_violations` = 0; agregado ≥ 59%
- [ ] Playwright login smoke **ou** N/A documentado
- [ ] `TESTING.md` + `backend/AGENTS.md` + nota FE alinhados ao harness real
- [ ] ADP integration live PASS **ou** N/A Docker documentado
- [ ] Auth/MSW hardening; ≤10 testes novos; `API_BASE_URL` unificado
- [ ] `validation.md` R4 + CONCERNS sync
- [ ] `mvn test` ≥474; Vitest ≥184; `./diversos/scripts/sonar-analyze.sh` exit 0
- [ ] Nenhuma breaking change HTTP/DTO pública

---

## Baseline pós-R3 (referência R4 — `main` @ `088a438`)

| Métrica | Valor pós-R3 (merge) |
| ------- | -------------------- |
| Sonar QG | **OK** |
| Sonar `new_coverage` | **80.0%** (limiar exato) |
| Sonar `new_branch_coverage` | **62.5%** |
| Sonar `new_violations` | **0** |
| Cobertura agregada Sonar | **59.8%** |
| Code smells | ~121 |
| Sonar bugs / vulns CR+MAJOR | **0** |
| JaCoCo global backend | **82.0%** |
| Testes backend | **474** (1 skip ADP) |
| Testes frontend (Vitest) | **184** |
| MSW | presente (`src/test/mswServer.ts`) |
| Playwright | **ausente** |
| ADP integration live | **não evidenciado** (Docker-gated skip) |
| `TESTING.md` | **stale** (pré-R1/R2 snapshot) |
| CI remoto | **ausente** |

**Metas R4 resumidas:** QG OK · `new_coverage` ≥85% · Playwright smoke · docs sync · ADP live · auth/MSW hardening · branch cov ≥70% informacional

---

## Próximos passos (TLC)

1. ~~**Specify**~~ — confirmado 2026-07-29
2. ~~**Design**~~ — `design.md`
3. ~~**Tasks**~~ — `tasks.md` (T1–T15; aguardando aprovação)
4. **Execute** — branch `feat/adequacao-analise-projeto` a partir de `main` @ `088a438`
