# Adequação da Análise de Projeto — R4 Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill: **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path. The skill is the source of truth for the full flow (per-task cycle, sub-agent delegation, adequacy review, Verifier, discrimination sensor).

**If the skill cannot be activated, STOP and tell the user — do not proceed without it.**

---

**Design**: `_docs/specs/features/adequacao-analise-projeto-r4/design.md`  
**Spec**: `_docs/specs/features/adequacao-analise-projeto-r4/spec.md`  
**Status**: Execute in progress — Batch 1 done (T1–T6 @ `9ee71bb`); Batch 2 next (T7–T15)  
**Branch base**: `main` @ `088a438` → `feat/adequacao-analise-projeto`

**Budget global R4**: ≤ **10** novos `it(`/`test(` (AAP4-17). Contagem acumulada: T5 (+1) · T6 (+0 assertion edits) · T8 (+1 e2e) · T11 (+0–2). **Usado: 1/10**. Parar e perguntar se o budget for estourar.

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec — confirm before Execute. Guidelines found: `AGENTS.md` (raiz), `backend/AGENTS.md`, `frontend/AGENTS.md`, `_docs/specs/TESTING.md` (stale — sync is T1), AD-004 (FE brownfield), AD-010 (ArchUnit), skills `spring-security`, `testing-a11y` (target — brownfield MSW/`page.route` only), R3 harness (`api.test.ts`, `AuthenticationServiceTest`, `ImportacaoFolhaAdpIntegrationTest`, gate scripts).

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
| ---------- | ------------------ | -------------------- | ---------------- | ----------- |
| Shared `apiBaseUrl` module | none | Build/typecheck; consumers verdes | `frontend/src/lib/apiBaseUrl.ts` | `cd frontend && npm test` |
| FE auth client (`api.ts`) | unit (Vitest + MSW) | ≥3 falhas refresh/401 com `response.status === 401` (não só `toBeDefined`); branches existentes | `frontend/src/services/api.test.ts` | `cd frontend && npm test -- api.test` |
| FE MSW handlers | none | Compila; `API_BASE_URL` re-export/import da fonte única | `frontend/src/test/handlers/authHandlers.ts` | `cd frontend && npm test` |
| FE Login page (opcional branch) | unit (Vitest + TL) | ≤1 caso novo se T9/T11 exigir; sem novo arquivo de page test | `frontend/src/pages/Login/Login.test.tsx` | `cd frontend && npm test -- Login.test` |
| FE Playwright login smoke | e2e | ≥1 spec: heading login + submit com `page.route` mock `POST */auth/login` shape `LoginResponse` | `frontend/e2e/*.spec.ts` | `cd frontend && npm run test:e2e` |
| Backend auth refresh domain | unit (Mockito) | `validarRefreshToken` → false ⇒ `RefreshTokenInvalidoException` (além de token ausente) | `backend/src/test/java/**/auth/**/AuthenticationServiceTest.java` | `cd backend && mvn test -Dtest=AuthenticationServiceTest` |
| Importação ADP integration | integration (Testcontainers) | Docker UP: exit 0 + ≥1 linha pré-rollback; Docker DOWN: skip + N/A documentado | `backend/src/test/java/**/importacao/**/ImportacaoFolhaAdpIntegrationTest.java` | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` |
| ArchUnit modular boundaries | unit | Zero violação AD-010 | `backend/src/test/java/**/arch/ModularArchitectureTest.java` | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| Gate scripts (sonar, jacoco, gate-r4) | none | Exit 0; flags documentadas | `diversos/scripts/*.sh` | `./diversos/scripts/sonar-analyze.sh` / `gate-r4-local.sh` |
| Docs (TESTING, AGENTS, CONCERNS, validation) | none | Review gate vs counts reais | `_docs/specs/*.md`, `*/AGENTS.md` | manual + gate counts |

## Gate Check Commands

> Generated from codebase — confirm before Execute.

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick FE | FE unit/MSW task | `cd frontend && npm test` |
| FE Focused | Single FE test file | `cd frontend && npm test -- <pattern>` |
| FE Coverage | Before Sonar checkpoint | `cd frontend && npm run test:coverage` |
| Quick BE | Backend focused unit | `cd backend && mvn test -Dtest=<ClassTest>` |
| ADP Integration | T10 | `cd backend && mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` |
| Arch | Backend code tasks | `cd backend && mvn test -Dtest=ModularArchitectureTest` |
| JaCoCo | Phase 3 gates | `bash diversos/scripts/check-jacoco-thresholds.sh` |
| Full BE | Phase 3 / Verifier | `cd backend && mvn test` (≥474 tests, 0 failures) |
| Full FE | Phase 3 / Verifier | `cd frontend && npm test` (≥184 cases; novos ≤10 R4) |
| Build FE | Playwright / config | `cd frontend && npm run lint && npm run build` |
| Sonar | T9 checkpoint + T13 final | `./diversos/scripts/sonar-analyze.sh` |
| E2E | T8 | `cd frontend && npm run test:e2e` |
| Gate local P3 | T14 | `./diversos/scripts/gate-r4-local.sh` (+ opcional `--docker` / `--e2e`) |

---

## Execution Plan

Phases run sequentially; tasks within a phase run in order.

**Batch sizing (Execute — offer sub-agents if user accepts):**

| Batch | Phases | Tasks | Count |
| ----- | ------ | ----- | ----- |
| **Batch 1** | Phase 1 | T1 → T6 | 6 |
| **Batch 2** | Phase 2 + Phase 3 | T7 → T13 | 7 |
| **Batch 3** (optional P3) | Phase 4 | T14 → T15 | 2 |

### Phase 1: Docs + hardening (P1)

```
T1 → T2 → T3 → T4 → T5 → T6
```

### Phase 2: Playwright + Sonar checkpoint (P1)

```
T7 → T8 → T9
```

### Phase 3: ADP live + gates + docs finais (P2)

```
T10 → T11 → T12 → T13
```

### Phase 4: Gate script + reset fallback (P3 opcional)

```
T14 → T15
```

---

## Task Breakdown

### T1: Sincronizar `_docs/specs/TESTING.md`

**What**: Reescrever `TESTING.md` para refletir o harness pós-R3 (Vitest 184+, MSW, JaCoCo/Sonar scripts, Testcontainers ADP Docker-gated, comandos gate; placeholder Playwright a completar em T7–T8).  
**Where**: `_docs/specs/TESTING.md`  
**Depends on**: None  
**Reuses**: Contagens baseline R3 (`088a438`); scripts em `diversos/scripts/`; padrões R3 validation  
**Requirement**: AAP4-09

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Documento lista Vitest **184+**, MSW harness, JaCoCo + `check-jacoco-thresholds.sh` + `sonar-analyze.sh`, Testcontainers ADP `@EnabledIf`
- [x] Removidas afirmações stale (“5 tests”, “sem Vitest”, “sem cobertura”)
- [x] Comandos de execução batem com `frontend/package.json` e `backend/pom.xml`

**Tests**: none  
**Gate**: manual review

**Commit**: `docs(r4): sync TESTING.md with post-R3 harness`

---

### T2: Atualizar `backend/AGENTS.md` §4 Testing

**What**: Corrigir §4 para mencionar Testcontainers, `ImportacaoFolhaAdpIntegrationTest`, `@EnabledIf` Docker, e contagem **≥ 474**.  
**Where**: `backend/AGENTS.md`  
**Depends on**: T1  
**Reuses**: Conteúdo real pós-R3; `ImportacaoFolhaAdpIntegrationTest.java`  
**Requirement**: AAP4-10

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] §4 não afirma mais “Testcontainers: Não configurado”
- [x] Menciona classe de integração ADP + skip Docker-gated
- [x] Contagem de testes backend **≥ 474**

**Tests**: none  
**Gate**: manual review

**Commit**: `docs(r4): update backend AGENTS testing section for Testcontainers`

---

### T3: Nota brownfield R3/R4 em `frontend/AGENTS.md`

**What**: Incluir nota brownfield: MSW isolado em testes HTTP (não global `setup.ts`); Playwright smoke R4; TARGET AD-004 unchanged.  
**Where**: `frontend/AGENTS.md`  
**Depends on**: T2  
**Reuses**: AD-004; `src/test/mswServer.ts` pattern  
**Requirement**: AAP4-11

**Tools**:

- MCP: NONE
- Skill: NONE (AD-004 — não migrar `src/pages/` → `src/features/`)

**Done when**:

- [x] Nota brownfield explícita (MSW isolado + Playwright R4)
- [x] TARGET AD-004 permanece; sem mudança estrutural de pastas
- [x] Script `test:e2e` referido como alvo R4 (implementação em T7)

**Tests**: none  
**Gate**: manual review

**Commit**: `docs(r4): add FE AGENTS brownfield note for MSW and Playwright`

---

### T4: Unificar `API_BASE_URL` em fonte única

**What**: Criar `getApiBaseUrl()` em `frontend/src/lib/apiBaseUrl.ts` (default `http://localhost:8083/api`); `api.ts` e `authHandlers.ts` (e re-exports) passam a usar essa fonte.  
**Where**: `frontend/src/lib/apiBaseUrl.ts` (criar); `frontend/src/services/api.ts`; `frontend/src/test/handlers/authHandlers.ts`  
**Depends on**: T3  
**Reuses**: Default atual em `api.ts` / `authHandlers.ts`; design Approach D1  
**Requirement**: AAP4-16

**Tools**:

- MCP: `context7` (Vite `import.meta.env` se necessário)
- Skill: NONE

**Done when**:

- [x] Uma única definição de base URL (lib); handlers re-exportam ou importam
- [x] `api.ts` não duplica literal divergente
- [x] Gate: `cd frontend && npm test` — ≥184 passam (0 falhas)
- [x] Test count: sem deleção silenciosa vs floor R3

**Tests**: none  
**Gate**: Quick FE

**Commit**: `fix(r4): share API base URL between axios client and MSW handlers`

---

### T5: Branch `validarRefreshToken` false em `AuthenticationServiceTest`

**What**: Adicionar **1** test case: token presente + `validarRefreshToken` → false ⇒ `RefreshTokenInvalidoException`.  
**Where**: `backend/src/test/java/.../auth/application/AuthenticationServiceTest.java`  
**Depends on**: T4  
**Reuses**: Padrão `refreshToken_inexistente_lancaMensagemInvalida`; `RefreshTokenInvalidoException`  
**Requirement**: AAP4-14, AAP4-17 (budget +1)

**Tools**:

- MCP: NONE
- Skill: `spring-security`

**Done when**:

- [x] Case cobre `validarRefreshToken == false` (não só `Optional.empty`)
- [x] Mensagem/exceção alinhada ao domínio existente
- [x] Gate: `cd backend && mvn test -Dtest=AuthenticationServiceTest` exit 0
- [x] Gate: `cd backend && mvn test -Dtest=ModularArchitectureTest` exit 0
- [x] Test count: +1 líquido; budget R4 acumulado ≤10

**Tests**: unit  
**Gate**: Quick BE + Arch

**Commit**: `test(r4): cover expired refresh token validation branch`

---

### T6: Fortalecer assertions 401 em `api.test.ts`

**What**: Em **≥ 3** casos de falha refresh/unauthorized que usam `rejects.toBeDefined()`, substituir/ampliar para assertir `response.status === 401` (shape axios). Preferir editar existentes (+0); no máximo +3 novos dentro do budget.  
**Where**: `frontend/src/services/api.test.ts`  
**Depends on**: T5  
**Reuses**: MSW handlers; `sampleLoginResponse`; padrão R3  
**Requirement**: AAP4-15, AAP4-17, AAP4-19 (branches em arquivo leak-period)

**Tools**:

- MCP: NONE
- Skill: `testing-a11y` (brownfield Vitest/MSW)

**Done when**:

- [x] ≥3 casos falha com assertion de status **401** (ou mensagem spec-defined)
- [x] Nenhum dos ≥3 permanece só com `rejects.toBeDefined()`
- [x] Gate: `cd frontend && npm test -- api.test` exit 0
- [x] Gate: `cd frontend && npm test` — ≥184; novos R4 acumulados ≤10

**Tests**: unit  
**Gate**: Quick FE

**Commit**: `test(r4): assert 401 status on auth client failure paths`

---

### T7: Adicionar Playwright + config + script `test:e2e`

**What**: Instalar `@playwright/test`; criar `frontend/playwright.config.ts` com `webServer` → `npm run build && npm run preview` (sem backend); script `test:e2e` em `package.json`.  
**Where**: `frontend/package.json`, `frontend/package-lock.json`, `frontend/playwright.config.ts`  
**Depends on**: T6  
**Reuses**: Design Approach B1; Vite preview  
**Requirement**: AAP4-06

**Tools**:

- MCP: `context7` (Playwright config)
- Skill: `testing-a11y` (E2E target pattern)

**Done when**:

- [x] `@playwright/test` em `devDependencies`
- [x] Script `test:e2e` presente
- [x] `playwright.config.ts` com `baseURL` + `webServer` preview (sem BE)
- [ ] Gate: `cd frontend && npm ci` exit 0; `npm test` ainda ≥184

**Tests**: none  
**Gate**: Build FE (deps) + Quick FE

**Commit**: `chore(r4): add Playwright and test:e2e script`

---

### T8: Spec E2E login smoke com `page.route`

**What**: Criar `frontend/e2e/login.spec.ts`: heading/título login visível; submit com mock `page.route('**/auth/login', …)` retornando shape de `sampleLoginResponse`.  
**Where**: `frontend/e2e/login.spec.ts`  
**Depends on**: T7  
**Reuses**: `sampleLoginResponse` de `authHandlers.ts`  
**Requirement**: AAP4-07, AAP4-08

**Tools**:

- MCP: `context7` (Playwright `page.route`)
- Skill: `testing-a11y`

**Done when**:

- [x] ≥1 spec passando com asserts (a) heading e (b) submit + mock login
- [x] Gate: `cd frontend && npm run test:e2e` exit 0 **ou** N/A documentado (browser não instalado) com comando `npx playwright install chromium` — P1 restante não bloqueado
- [x] Budget: +1 `test(`; acumulado R4 ≤10

**Tests**: e2e  
**Gate**: E2E

**Commit**: `test(r4): add Playwright login smoke with page.route mock`

---

### T9: Checkpoint Sonar — meta `new_coverage` ≥85%

**What**: Rodar `./diversos/scripts/sonar-analyze.sh`; registrar QG, `new_coverage`, `new_branch_coverage`, `new_violations`, agregado. Se `new_coverage` &lt;85%, flag explícita para T11 (branch tests) antes de considerar T15.  
**Where**: Evidência em handoff / nota interim (baseline final em T13 `validation.md`)  
**Depends on**: T8  
**Reuses**: `sonar-analyze.sh`; Sonar MCP `get_component_measures` / `get_project_quality_gate_status` se disponível  
**Requirement**: AAP4-01…04 (checkpoint), AAP4-18 (informativo)

**Tools**:

- MCP: `user-sonarqube` (measures / QG)
- Skill: NONE

**Done when**:

- [x] `sonar-analyze.sh` exit 0 (ou bloqueio documentado: Sonar DOWN)
- [x] Métricas registradas: QG, `new_coverage`, `new_branch_coverage`, `new_violations`, `coverage`
- [x] Decisão explícita: ≥85% → T11 pode skip código; &lt;85% → T11 obrigatório dentro AAP4-19; se budget esgotado → parar e perguntar (edge case spec)

**Tests**: none  
**Gate**: Sonar

**Commit**: `chore(r4): record Sonar checkpoint metrics after hardening`

---

### T10: Evidência live ADP integration (Docker-gated)

**What**: Pré-check `docker info`; se UP, `mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` exit 0; se DOWN, suite completa permanece verde (1 skip) e N/A documentado.  
**Where**: Evidência para T13; sem mudança de produção esperada  
**Depends on**: T9  
**Reuses**: `ImportacaoFolhaAdpIntegrationTest` + `@EnabledIf`  
**Requirement**: AAP4-12, AAP4-13

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [x] Comando Docker de verificação registrado
- [ ] UP → teste exit 0 (persistência ≥1 linha pré-rollback) — **N/A**: Testcontainers unavailable (skip 1)
- [x] DOWN → `mvn test` suite verde com skip; N/A motivo registrado
- [x] Falha Testcontainers com Docker UP é **bloqueante** (não mascarar)

**Tests**: none (evidência do teste de integração existente)  
**Gate**: ADP Integration

**Commit**: `test(r4): record ADP integration live or Docker N/A evidence`

---

### T11: Fechar gap `new_branch_coverage` / buffer (escopo AAP4-19)

**What**: Se T9 ≥85% **e** `new_branch_coverage` ≥70%: documentar skip (sem código). Senão: adicionar ≤2 casos **somente** em `api.test.ts`, `AuthenticationServiceTest`, ou **1** page test existente (`Login.test.tsx` / `FolhaPagamento.test.tsx`) — **sem** novos arquivos de page test.  
**Where**: Arquivos leak-period R3 conforme necessidade  
**Depends on**: T10  
**Reuses**: Estratégia Branch Coverage do design  
**Requirement**: AAP4-18, AAP4-19, AAP4-17

**Tools**:

- MCP: `user-sonarqube` (`get_file_coverage_details` / `search_files_by_coverage` se útil)
- Skill: `testing-a11y` / `spring-security` conforme arquivo

**Done when**:

- [x] Skip documentado **ou** ≤2 casos adicionados no escopo AAP4-19
- [x] Budget R4 acumulado ≤10
- [x] Gates dos arquivos tocados verdes
- [x] Se ainda &lt;85% após budget: **parar e perguntar** (SPEC_DEVIATION / B9) — não inventar page tests

**Tests**: unit (se houver código) / none (se skip)  
**Gate**: Quick FE e/ou Quick BE conforme touch

**Commit**: `test(r4): raise branch coverage within R4 leak-period scope` **ou** `docs(r4): skip branch push — metrics already met`

---

### T12: Gates finais BE + FE + JaCoCo + ArchUnit

**What**: Rodar suíte completa e limiares: `mvn test` ≥474 / 0 falhas; `npm test` ≥184; `check-jacoco-thresholds.sh` exit 0; `ModularArchitectureTest` OK.  
**Where**: N/A (gate only)  
**Depends on**: T11  
**Reuses**: Scripts R3  
**Requirement**: AAP4-20, AAP4-21, AAP4-22, AAP4-24

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] `cd backend && mvn test` — 0 falhas; count ≥474
- [ ] `bash diversos/scripts/check-jacoco-thresholds.sh` exit 0
- [ ] `cd frontend && npm test` — ≥184; novos R4 ≤10
- [ ] `ModularArchitectureTest` — zero violações AD-010

**Tests**: none  
**Gate**: Full BE + Full FE + JaCoCo + Arch

**Commit**: `chore(r4): confirm full BE/FE/JaCoCo/Arch gates green`

---

### T13: `validation.md` R4 + `CONCERNS.md` sync

**What**: Criar `_docs/specs/features/adequacao-analise-projeto-r4/validation.md` com baseline pós-R4 (commit, métricas Sonar, comando gate, ADP/Playwright PASS ou N/A). Atualizar `CONCERNS.md`: E2E Playwright; ADP integration; pointer TESTING.md.  
**Where**: `_docs/specs/features/adequacao-analise-projeto-r4/validation.md` (criar); `_docs/specs/CONCERNS.md`  
**Depends on**: T12  
**Reuses**: Template `adequacao-analise-projeto-r3/validation.md`  
**Requirement**: AAP4-05, AAP4-23, AAP4-01…04 (final), AAP4-08/13 N/A se aplicável

**Tools**:

- MCP: `user-sonarqube` (métricas finais)
- Skill: NONE

**Done when**:

- [ ] `validation.md` com baseline commit, `new_coverage`, `new_branch_coverage`, agregado, QG, comando reprodução
- [ ] Playwright e ADP: PASS ou N/A com motivo
- [ ] `CONCERNS.md` atualizado (E2E, ADP, TESTING pointer)
- [ ] Sonar final: QG OK; `new_violations` 0; metas internas registradas (85% / 70% branch) com ressalva se informativo

**Tests**: none  
**Gate**: Sonar (final) + manual review

**Commit**: `docs(r4): add validation baseline and sync CONCERNS`

---

### T14: Script `gate-r4-local.sh` (P3 opcional)

**What**: Criar `diversos/scripts/gate-r4-local.sh` executando `mvn test`, `npm test`, `check-jacoco-thresholds.sh`; flags `--docker` (ADP integration se Docker UP; warning+continue se DOWN) e opcional `--e2e` / `--sonar`. Documentar em `TESTING.md`.  
**Where**: `diversos/scripts/gate-r4-local.sh`; `_docs/specs/TESTING.md` (referência)  
**Depends on**: T13  
**Reuses**: Scripts existentes; design C6  
**Requirement**: AAP4-25, AAP4-26

**Tools**:

- MCP: NONE
- Skill: NONE

**Done when**:

- [ ] Script exit 0 quando todos os passos invocados passam
- [ ] `--docker`: UP inclui ADP test; DOWN warning + continua
- [ ] Referência em `TESTING.md`
- [ ] Gate: dry-run sem `--sonar` exit 0 (ou documentar pré-req)

**Tests**: none  
**Gate**: Gate local P3

**Commit**: `chore(r4): add gate-r4-local.sh for reproducible pre-merge checks`

---

### T15: Fallback reset Sonar `PREVIOUS_VERSION` (P3 — só se necessário)

**What**: **Somente se** após T11–T13 `new_coverage` &lt;85% com budget esgotado e usuário aprovou B9: reset baseline Sonar `PREVIOUS_VERSION` + evidência ops em `validation.md`. Se métricas OK: skip task com commit docs ou marcar cancelada.  
**Where**: Ops Sonar + `validation.md`  
**Depends on**: T14  
**Reuses**: Decisão B9 spec; Approach C3  
**Requirement**: B9 fallback (não AAP4-25)

**Tools**:

- MCP: `user-sonarqube`
- Skill: NONE

**Done when**:

- [ ] Skip se `new_coverage` ≥85% **ou** usuário não aprovou reset
- [ ] Se executado: evidência ops + métricas pós-reset em `validation.md`; QG OK
- [ ] Nunca reset sem evidência de scan pós-hardening

**Tests**: none  
**Gate**: Sonar

**Commit**: `chore(r4): Sonar baseline reset fallback` **ou** `docs(r4): skip Sonar reset — coverage meta met`

---

## Phase Execution Map

```
Phase 1 → Phase 2 → Phase 3 → Phase 4 (optional)

Phase 1:  T1 ──→ T2 ──→ T3 ──→ T4 ──→ T5 ──→ T6
Phase 2:  T7 ──→ T8 ──→ T9
Phase 3:  T10 ──→ T11 ──→ T12 ──→ T13
Phase 4:  T14 ──→ T15
```

Execution is strictly sequential — no intra-phase parallelism.

**How phase-based execution works:**

At Execute, pack phases into **task-budgeted batches** (~7 tasks/worker, whole phases). This feature → **3 batches** (6 + 7 + 2). Offer batch sub-agents; never auto-spawn. After last committed task, Verifier runs automatically (author ≠ verifier).

---

## Task Granularity Check

| Task | Scope | Status |
| ---- | ----- | ------ |
| T1: TESTING.md sync | 1 doc file | ✅ Granular |
| T2: backend AGENTS §4 | 1 section | ✅ Granular |
| T3: frontend AGENTS note | 1 note | ✅ Granular |
| T4: apiBaseUrl + wire | 1 module + 2 consumers | ⚠️ Cohesive (same concern AAP4-16) |
| T5: Auth refresh false branch | 1 test case | ✅ Granular |
| T6: api.test 401 assertions | 1 file, ≥3 cases | ⚠️ Cohesive (AAP4-15) |
| T7: Playwright deps+config | package + config | ⚠️ Cohesive (AAP4-06 infra) |
| T8: login.spec.ts | 1 e2e spec | ✅ Granular |
| T9: Sonar checkpoint | 1 ops gate | ✅ Granular |
| T10: ADP live evidence | 1 integration run | ✅ Granular |
| T11: Branch coverage gap | ≤2 cases / skip | ✅ Granular |
| T12: Full gates | gate-only | ✅ Granular |
| T13: validation + CONCERNS | 2 docs | ⚠️ Cohesive (close-out) |
| T14: gate-r4-local.sh | 1 script | ✅ Granular |
| T15: Sonar reset fallback | 1 ops path | ✅ Granular |

**Granularity check**: all ✅ or ⚠️ cohesive same-concern — no ❌ multi-feature bundles.

---

## Diagram-Definition Cross-Check

| Task | Depends On (task body) | Diagram Shows | Status |
| ---- | ---------------------- | ------------- | ------ |
| T1 | None | (start) | ✅ Match |
| T2 | T1 | T1 → T2 | ✅ Match |
| T3 | T2 | T2 → T3 | ✅ Match |
| T4 | T3 | T3 → T4 | ✅ Match |
| T5 | T4 | T4 → T5 | ✅ Match |
| T6 | T5 | T5 → T6 | ✅ Match |
| T7 | T6 | T6 → T7 (phase boundary) | ✅ Match |
| T8 | T7 | T7 → T8 | ✅ Match |
| T9 | T8 | T8 → T9 | ✅ Match |
| T10 | T9 | T9 → T10 | ✅ Match |
| T11 | T10 | T10 → T11 | ✅ Match |
| T12 | T11 | T11 → T12 | ✅ Match |
| T13 | T12 | T12 → T13 | ✅ Match |
| T14 | T13 | T13 → T14 | ✅ Match |
| T15 | T14 | T14 → T15 | ✅ Match |

No forward-phase dependencies. All arrows ↔ body fields agree.

---

## Test Co-location Validation

| Task | Code Layer Created/Modified | Matrix Requires | Task Says | Status |
| ---- | --------------------------- | --------------- | --------- | ------ |
| T1 | Docs | none | none | ✅ OK |
| T2 | Docs | none | none | ✅ OK |
| T3 | Docs | none | none | ✅ OK |
| T4 | Shared apiBaseUrl / handlers / api.ts | none | none | ✅ OK |
| T5 | Backend auth refresh domain | unit | unit | ✅ OK |
| T6 | FE auth client tests | unit | unit | ✅ OK |
| T7 | Playwright deps/config | none | none | ✅ OK |
| T8 | FE Playwright login smoke | e2e | e2e | ✅ OK |
| T9 | Gate scripts (run) | none | none | ✅ OK |
| T10 | ADP integration (evidence only) | none (existing IT) | none | ✅ OK |
| T11 | FE/BE unit (se código) | unit / none | unit (se código) / none (skip) | ✅ OK |
| T12 | Gate-only | none | none | ✅ OK |
| T13 | Docs | none | none | ✅ OK |
| T14 | Gate script | none | none | ✅ OK |
| T15 | Ops / docs | none | none | ✅ OK |

No ❌ VIOLATION. Tests co-located with the task that creates/modifies the layer; no “tested in another task” deferral.

---

## Requirement Traceability (tasks)

| Requirement | Tasks |
| ----------- | ----- |
| AAP4-01…04 | T9 (checkpoint), T13 (final) |
| AAP4-05 | T13 |
| AAP4-06 | T7 |
| AAP4-07 | T8 |
| AAP4-08 | T8, T13 |
| AAP4-09 | T1 |
| AAP4-10 | T2 |
| AAP4-11 | T3 |
| AAP4-12…13 | T10, T13 |
| AAP4-14 | T5 |
| AAP4-15 | T6 |
| AAP4-16 | T4 |
| AAP4-17 | T5, T6, T8, T11 (budget) |
| AAP4-18…19 | T9, T11 |
| AAP4-20…22, AAP4-24 | T12 |
| AAP4-23 | T13 |
| AAP4-25…26 | T14 |
| B9 fallback | T15 |

**Coverage:** 26 ACs + B9 mapped; 0 unmapped.
