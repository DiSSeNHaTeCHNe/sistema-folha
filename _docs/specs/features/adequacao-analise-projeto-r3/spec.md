# Adequação da Análise de Projeto — R3 (Profundidade FE + QG Sonar) Specification

**Parent:** `_docs/specs/features/adequacao-analise-projeto-r2/spec.md` (merged @ `cb6e04a`)  
**Related:** `_docs/specs/CONCERNS.md`, `_docs/specs/TESTING.md`, `validation.md` (R2 PASS @ `658be8a`), AD-004 (skills FE TARGET)  
**Complexity:** Large  
**Spec status:** Draft — Design 2026-07-29

## Problem Statement

A R2 fechou o **Plano C** com PASS: CONCERNS P1 resolvidos, JaCoCo **81.7%**, cobertura agregada Sonar **48%**, `new_violations` **0**, Vitest **39** testes. Porém o **Quality Gate Sonar permanece ERROR** na única condição **`new_coverage` 62.2%** (< 80%) — exceção documentada (slot 1/2). Os testes FE são majoritariamente **smoke render** (`vi.mock` de services); **`api.ts`** (refresh/401/fila) não tem cobertura; **T20/AAP2-22** (integração ADP) ficou N/A por Docker; e follow-ups Sonar/CONCERNS (**S2245**, tx `BeneficioMensalService`) permanecem abertos.

A R3 ataca **profundidade de cobertura incremental** (meta QG verde), **primeiro passo AD-004** (MSW + behavior tests no auth stack FE), **carryover integração ADP**, e **polish auth BE** (refresh inválido → 401), sem abrir features de produto.

## Goals

- [ ] Quality Gate Sonar **OK** após `./diversos/scripts/sonar-analyze.sh` (sem exceção em `new_coverage`)
- [ ] Sonar `new_coverage` ≥ **80%** (vs 62.2% pós-R2)
- [ ] Manter `new_violations` = **0** e cobertura agregada ≥ **48%** (regressão zero vs R2)
- [ ] MSW + testes de **behavior** para `api.ts` (refresh, fila 401, logout)
- [ ] Entregar **T20 carryover**: integração ADP com Testcontainers **ou** N/A documentado
- [ ] Fechar follow-ups Sonar/CONCERNS selecionados (S2245, tx `BeneficioMensalService`)
- [ ] Atualizar `CONCERNS.md` e `validation.md` R3

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Reset Sonar baseline **sem** código/testes | Ops isolado; R3 exige elevação real de `new_coverage` |
| Adequação FE **completa** AD-004 (migrar `src/features/`, TanStack Query, forms-validation skill) | Escopo ROADMAP M3; R3 = passo incremental brownfield |
| Migração estrutural `src/pages/` → `src/features/` | Refactor amplo; fora R3 |
| Backend Relatórios PDF | Feature de produto separada |
| Remoção modelo legado `Beneficio` | Feature separada |
| Zerar todos code smells (~121) | Meta = top follow-ups + manter ≤230 |
| Suite E2E Playwright completa | P3 opcional — 1 smoke se tempo; não suite |
| CI/CD remoto | M3 DevOps |
| Mudança semântica ACL / contratos HTTP públicos | Fix-only; refresh 401 é correção de status, não contrato novo |
| Refactor estrutural `ImportacaoFolhaAdpService` (CC 71) | Fora R3 |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| B1 — Baseline Sonar pós-R2 | Primeira análise em `main` @ `cb6e04a` como referência R3 | Merge squash R2; medir `new_*` após 1º scan pós-merge | y |
| B2 — Prioridade QG | **`new_coverage` ≥ 80%** é P1 bloqueante; QG OK é outcome | Fecha exceção R2 documentada | y |
| B3 — AD-004 incremental | Adicionar **MSW** + testes `api.ts` + 1 behavior Login; **não** reestruturar pastas FE | Skills TARGET parcial; brownfield `frontend/AGENTS.md` | y |
| B4 — MSW ausente hoje | R3 adiciona `msw` devDependency + `setupServer` em `src/test/` | `package.json` não lista MSW ainda; skill `testing-a11y` prescreve MSW | y |
| B5 — Vitest meta | ≥ **50** test cases passando (vs 39 R2) como proxy de profundidade | Mensurável; correlaciona com `new_coverage` | y |
| B6 — Refresh inválido | `AuthenticationService` lança exceção de domínio mapeada a **401** + mensagem genérica AAP-08 | Code-review R2: hoje 500 via handler genérico | y |
| B7 — T20 carryover | Mesmo critério R2: Testcontainers + rollback; N/A se Docker indisponível | AAP2-22 não entregue | y |
| B8 — Sonar touch-only | S2245 + BeneficioMensal tx: ≤50 LOC/issue; senão CONCERNS follow-up | Precedente R2 T10–T13 | y |
| B9 — Branch | `feat/adequacao-analise-projeto` a partir de `main` @ `cb6e04a` | Convenção fix/r3 → branch base sem sufixo | y |
| B10 — Playwright | **P3 opcional** — 1 spec login smoke se P1–P2 verdes | Playwright não configurado no `package.json` atual | y |

**Open questions:** none — escopo R3 derivado do backlog R2 + CONCERNS; confirmar com usuário na revisão da spec.

### Implicit-requirement dimensions (Large)

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | N/A — R3 não adiciona endpoints novos |
| Failure / partial-failure | AAP3-11: integração ADP rollback; AAP3-06: refresh fail → logout |
| Idempotency / retry | AAP3-06: fila `failedQueue` + flag `isRefreshing` testada |
| Auth boundaries | AAP3-09/10: refresh inválido → 401 genérico; AAP3-06: sem token vaza em retry |
| Concurrency / ordering | AAP3-06: requisições 401 concorrentes enfileiram até refresh único |
| Data lifecycle | N/A |
| Observability | N/A — R2 redaction mantida; sem novos logs sensíveis |
| External-dependency failure | AAP3-11: Testcontainers indisponível → N/A documentado |
| State-transition integrity | AAP3-13: tx BeneficioMensal sem self-invocation bypass |

---

## User Stories

### P1: Quality Gate Sonar — `new_coverage` verde ⭐ MVP

**User Story**: Como Tech Lead, quero o Quality Gate Sonar **OK** sem exceções, para encerrar a dívida de cobertura incremental aberta na R2.

**Why P1**: R2 deixou QG ERROR só em `new_coverage` 62.2%; exceção documentada apontava R3.

**Acceptance Criteria**:

1. **(AAP3-01)** WHEN `./diversos/scripts/sonar-analyze.sh` completar em `main` pós-R3 THEN Quality Gate status SHALL ser **OK** (zero condições ERROR)
2. **(AAP3-02)** WHEN Sonar API consultar `new_coverage` THEN valor SHALL ser **≥ 80%**
3. **(AAP3-03)** WHEN Sonar API consultar `new_violations` THEN valor SHALL ser **0** (regressão zero vs R2)
4. **(AAP3-04)** WHEN Sonar API consultar `coverage` (agregado) THEN valor SHALL ser **≥ 48%** (floor R2 mantido)

**Independent Test**: Script sonar-analyze exit 0; Sonar API QG + measures `new_coverage`, `new_violations`, `coverage`.

---

### P1: FE — MSW + testes de behavior no auth stack ⭐ MVP

**User Story**: Como mantenedor frontend, quero testes que exercitem o fluxo HTTP real (MSW) especialmente refresh/401, para elevar `new_coverage` e detectar regressões no auth client.

**Why P1**: FE é gargalo de `new_coverage`; R2 usou `vi.mock` — cobertura de linhas em `api.ts` permanece baixa.

**Acceptance Criteria**:

1. **(AAP3-05)** WHEN `cd frontend && npm test` rodar THEN projeto SHALL incluir harness MSW (`setupServer` ou equivalente) integrado ao Vitest setup existente
2. **(AAP3-06)** WHEN testes de `api.ts` rodarem THEN SHALL existir ≥ **4** test cases passando cobrindo: (a) 401 → refresh → retry com novo token; (b) refresh falha → `auth:logout` + tokens limpos; (c) segunda 401 durante refresh enfileira retry; (d) refresh endpoint 401 → logout
3. **(AAP3-07)** WHEN `Login.test.tsx` rodar THEN SHALL existir ≥ **1** test case de **submit com credenciais inválidas** assertando mensagem de erro visível (role/alert ou text) **sem** navegação para rota autenticada
4. **(AAP3-08)** WHEN `cd frontend && npm run test:coverage` rodar THEN total Vitest SHALL ser **≥ 50** test cases passando (vs 39 R2)

**Independent Test**: `npm run test:coverage`; grep `api.test.ts` ou equivalente; contagem Vitest; Sonar `new_coverage` measure.

---

### P2: Auth backend — refresh inválido retorna 401 ⭐

**User Story**: Como consumidor da API, quero que refresh token inválido/expirado retorne **401** com mensagem genérica, não 500, alinhado ao fluxo JWT documentado.

**Why P2**: Code-review R2; `IllegalStateException` cai em handler genérico → 500.

**Acceptance Criteria**:

1. **(AAP3-09)** WHEN `POST /auth/refresh` receber token inválido, expirado ou revogado THEN response status SHALL ser **401** e body SHALL conter mensagem genérica (mesmo texto de falha de login, per AAP-08 R1)
2. **(AAP3-10)** WHEN `AuthenticationServiceTest` ou teste MVC equivalente rodar THEN SHALL existir teste assertando status **401** (não 500) para refresh inválido

**Independent Test**: `MockMvc` ou `@WebMvcTest` no `AuthController`; unit test do service + handler mapping.

---

### P2: Integração ADP — carryover T20 ⭐

**User Story**: Como mantenedor, quero **um** teste de integração da importação ADP com DB real, complementando fixtures unitárias R1/R2.

**Why P2**: AAP2-22 N/A na R2; CONCERNS marca importação como frágil.

**Acceptance Criteria**:

1. **(AAP3-11)** WHEN `ImportacaoFolhaAdpIntegrationTest` rodar com Docker disponível THEN SHALL persistir **≥ 1** linha a partir de `folha-adp-minimal.txt` com rollback (`@Transactional` test ou Testcontainers teardown) e gate `mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest` exit 0
2. **(AAP3-12)** WHEN Docker/Testcontainers indisponível THEN `validation.md` SHALL documentar N/A; gate `mvn test` suite completa SHALL permanecer verde (teste `@EnabledIf` ou profile)

**Independent Test**: `mvn test -Dtest=ImportacaoFolhaAdpIntegrationTest`; evidência em validation.

---

### P2: Sonar / CONCERNS follow-ups selecionados

**User Story**: Como mantenedor, quero fechar follow-ups Sonar/CONCERNS explícitos adiados na R2 sem refactor estrutural.

**Acceptance Criteria**:

1. **(AAP3-13)** WHEN `FolhaPagamento/index.tsx` renderizar lista THEN React keys SHALL **not** usar `Math.random()` (S2245); teste ou grep SHALL confirmar key estável (ex.: `id` do item)
2. **(AAP3-14)** WHEN `BeneficioMensalService` executar métodos transacionais THEN SHALL aplicar padrão R2 (private non-@Transactional helper); `@SuppressWarnings(S6809)` removido ou substituído por refactor; `BeneficioMensalServiceTest` verde
3. **(AAP3-15)** WHEN issues Sonar CR+MAJOR `inNewCodePeriod=true` forem exportadas nos arquivos tocados THEN contagem SHALL ser **0** pós-R3

**Independent Test**: Sonar export; grep keys; `BeneficioMensalServiceTest`; `mvn test`.

---

### P2: Gates, documentação e regressão zero

**User Story**: Como desenvolvedor, quero gates verdes e CONCERNS sincronizado após R3.

**Acceptance Criteria**:

1. **(AAP3-16)** WHEN `cd backend && mvn test` THEN **0** falhas; contagem **≥ 464** (sem deleção silenciosa vs R2)
2. **(AAP3-17)** WHEN `bash diversos/scripts/check-jacoco-thresholds.sh` THEN exit **0** (global ≥ 75%, floors R1/R2)
3. **(AAP3-18)** WHEN `./diversos/scripts/sonar-analyze.sh` completar THEN bugs OPEN = **0** e vulns CR+MAJOR = **0**
4. **(AAP3-19)** WHEN R3 fechar THEN `_docs/specs/CONCERNS.md` SHALL marcar resolvidos: S2245, BeneficioMensal tx; atualizar seção Test Coverage FE
5. **(AAP3-20)** WHEN `ModularArchitectureTest` rodar THEN zero violações AD-010

**Independent Test**: Full gate suite; CONCERNS diff; ArchUnit.

---

### P3: E2E smoke opcional (se P1–P2 verdes)

**User Story**: Como QA, quero **um** fluxo Playwright de login como ponte para E2E futuro.

**Why P3**: CONCERNS: integração E2E pendente; escopo mínimo.

**Acceptance Criteria**:

1. **(AAP3-21)** WHEN Playwright estiver configurado e `npm run test:e2e` rodar THEN SHALL existir **≥ 1** spec passando: página login visível + submit (mock backend ou ambiente test)

**Independent Test**: `npm run test:e2e` (ou script documentado); skip N/A em validation se não configurado.

---

## Edge Cases

- WHEN `new_coverage` subir mas QG falhar em outra condição THEN SHALL corrigir antes de PASS (AAP3-01 bloqueante)
- WHEN MSW conflitar com `vi.mock` existente em page tests THEN SHALL isolar MSW aos testes de `api.ts`/integração HTTP; page tests podem manter mock de service (brownfield)
- WHEN refactor tx `BeneficioMensalService` quebrar benefício mensal THEN SHALL reverter e marcar SPEC_DEVIATION
- WHEN Testcontainers indisponível THEN AAP3-11/12 = N/A; não bloqueia R3 (mesmo critério R2)
- WHEN smell fix exigir refactor >50 LOC THEN registrar follow-up CONCERNS; não expandir task
- WHEN Playwright setup exceder budget R3 THEN AAP3-21 = N/A documentado; P3 não bloqueia

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AAP3-01 | P1: QG | Fase 1 | Mapped → C-sonar, gates |
| AAP3-02 | P1: QG | Fase 1 | Mapped → C-sonar |
| AAP3-03 | P1: QG | Fase 1 | Mapped → C-sonar |
| AAP3-04 | P1: QG | Fase 1 | Mapped → C-sonar |
| AAP3-05 | P1: FE MSW | Fase 1 | Mapped → C1 |
| AAP3-06 | P1: FE api.ts | Fase 1 | Mapped → C2 |
| AAP3-07 | P1: FE Login behavior | Fase 1 | Mapped → C3 |
| AAP3-08 | P1: FE count | Fase 1 | Mapped → C2,C3,C4 |
| AAP3-09 | P2: Auth 401 | Fase 2 | Mapped → C5 |
| AAP3-10 | P2: Auth test | Fase 2 | Mapped → C5 |
| AAP3-11 | P2: ADP integration | Fase 2 | Mapped → C6 |
| AAP3-12 | P2: ADP N/A path | Fase 2 | Mapped → C6 |
| AAP3-13 | P2: S2245 | Fase 2 | Mapped → C7 |
| AAP3-14 | P2: BeneficioMensal tx | Fase 2 | Mapped → C8 |
| AAP3-15 | P2: Sonar new-code | Fase 2 | Mapped → C9 |
| AAP3-16 | P2: Gate BE | Fase 2 | Mapped → C9 |
| AAP3-17 | P2: Gate JaCoCo | Fase 2 | Mapped → C9 |
| AAP3-18 | P2: Gate Sonar bugs | Fase 2 | Mapped → C9 |
| AAP3-19 | P2: CONCERNS sync | Fase 2 | Mapped → C9 |
| AAP3-20 | P2: ArchUnit | Fase 2 | Mapped → C9 |
| AAP3-21 | P3: Playwright | Fase 3 | Mapped → C10 |

**Coverage:** 21 total, 21 mapped, 0 unmapped

---

## Success Criteria

- [ ] Sonar QG **OK** (sem exceções em `validation.md`)
- [ ] `new_coverage` ≥ 80%; `new_violations` = 0; agregado ≥ 48%
- [ ] MSW + ≥4 testes `api.ts`; Login behavior test; Vitest ≥ 50
- [ ] Refresh inválido → HTTP 401
- [ ] Integração ADP entregue ou N/A documentado
- [ ] S2245 + BeneficioMensal tx resolvidos em CONCERNS
- [ ] `mvn test` verde; `./diversos/scripts/sonar-analyze.sh` exit 0
- [ ] Nenhuma breaking change HTTP/DTO pública

---

## Baseline pós-R2 (referência R3 — `main` @ `cb6e04a`)

| Métrica | Valor pós-R2 |
| ------- | ------------ |
| Sonar QG | ERROR (`new_coverage` only) |
| Sonar `new_coverage` | 62.2% |
| Sonar `new_violations` | 0 |
| Cobertura agregada Sonar | 48.0% |
| Code smells | 121 |
| Sonar bugs / vulns CR+MAJOR | 0 |
| JaCoCo global backend | 81.7% |
| Testes backend | 464 |
| Testes frontend (Vitest) | 39 |
| MSW no projeto | ausente |
| Playwright configurado | ausente |
| T20 / AAP2-22 | N/A (Docker) |

**Metas R3 resumidas:** QG OK · `new_coverage` ≥80% · MSW + api.ts tests · Vitest ≥50 · refresh 401 · ADP integration · S2245 + BeneficioMensal tx

---

## Próximos passos (TLC)

1. ~~**Specify**~~ — `spec.md`
2. ~~**Design**~~ — `design.md`
3. ~~**Tasks**~~ — `tasks.md`
4. **Execute** — branch `feat/adequacao-analise-projeto` a partir de `main` @ `cb6e04a`
