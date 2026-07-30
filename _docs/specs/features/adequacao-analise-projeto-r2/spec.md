# Adequação da Análise de Projeto — R2 (Plano C Híbrido) Specification

**Parent:** `_docs/specs/features/adequacao-analise-projeto/spec.md` (R1 merged @ `047e64d`)  
**Related:** `_docs/specs/CONCERNS.md`, `_docs/specs/TESTING.md`, `validation.md` (R1 PASS @ `d22a374`)  
**Complexity:** Large  
**Spec status:** Complete — Execute PASS @ `658be8a` (T1–T19; T20 N/A Docker)

## Problem Statement

A R1 (`adequacao-analise-projeto`) entregou **piso de confiabilidade**: Sonar bugs/vulns zerados, domínios críticos com JaCoCo acima dos limiares, Vitest baseline e gates locais reproduzíveis. Porém o **Quality Gate Sonar permanece ERROR** (`new_violations` = 126, `new_coverage` = 60.4% vs baseline 2026-07-27), **270 code smells** remanescentes, cobertura agregada **40.7%** (FE ainda mínimo), e itens **CONCERNS** P1 não endereçados (`@Transactional` via `this`, `ddl-auto: update`, hardening JWT incompleto).

A R2 aplica o **Plano C híbrido**: **Fase 1** ataca métricas Sonar incrementais e cobertura FE útil; **Fase 2** paga dívida técnica operacional de CONCERNS sem abrir features de produto (relatórios PDF, remoção Beneficio legado).

## Goals

- [x] Quality Gate Sonar **OK** após `./diversos/scripts/sonar-analyze.sh` **ou** ≤2 exceções documentadas em `validation.md` — 1 exceção (`new_coverage` 62.2%)
- [x] `new_violations` Sonar = **0** (vs baseline pós-merge R1)
- [x] Cobertura incremental Sonar `new_coverage` ≥ **80%** **ou** cobertura agregada ≥ **48%** com evidência FE lcov — agregado **48.0%**
- [x] Reduzir code smells Sonar em ≥ **15%** vs pós-R1 (270 → ≤ **230**) — **121**
- [x] JaCoCo global backend ≥ **75%** (R1: 71.5%) — **81.7%**
- [x] Fechar follow-ups CONCERNS P1: self-invocation transacional, `ddl-auto`, JWT hardening
- [x] Atualizar `_docs/specs/CONCERNS.md` e `validation.md` R2

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Resetar baseline Sonar no servidor sem código | Atalho ops; não reduz dívida real |
| Zerar todos os code smells (~270) | Escopo infinito; meta = −15% + top issues |
| Backend Relatórios PDF | Feature de produto separada |
| Remoção modelo legado `Beneficio` | Feature separada (dual model) |
| Adequação FE completa AD-004 (skills TARGET) | ROADMAP M3 deferred |
| Refactor estrutural `ImportacaoFolhaAdpService` (CC 71) | Fora R2; só smells tocados |
| Testcontainers em massa | 1 integração pontual ADP **opcional** P2; não suite completa |
| CI/CD remoto | M3 DevOps |
| Mudança semântica ACL / contratos HTTP | Fix-only |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| B1 — Baseline Sonar pós-R1 | Primeira análise em `main` @ `047e64d` redefine referência para `new_*` | Merge squash altera linha do tempo; medir após 1º scan em main | y |
| B2 — Plano C = Fase1 QG + Fase2 CONCERNS | P1 Sonar/coverage; P2 transação/schema/JWT | Híbrido acordado com usuário | y |
| B3 — QG OK prioritário | Tentar OK; se falhar só `new_coverage` histórico, 1 exceção permitida | Pragmático vs 126 violations | y |
| B4 — FE tests R2 | Vitest em páginas Folha, Organograma, Login + 1 service; não E2E Playwright | Eleva `new_coverage` sem AD-004 full | y |
| B5 — JaCoCo global ≥75% | Incremento +5pp vs R1; domínios R1 mantêm floors (50/40/75/65) | Meta incremental | y |
| B6 — `ddl-auto` | `validate` em profile default; `update` só se profile `dev` explícito | Flyway canônico | y |
| B7 — JWT hardening | Rejeitar secret blank; fail-fast se secret = default em qualquer profile ≠ dev/test | Estende AAP-09 R1 | y |
| B8 — Timing side-channel login | BCrypt dummy hash quando usuário inexistente | Code-review R1 finding | y |
| B9 — Sonar analyze pipeline | `sonar-analyze.sh` invoca `npm run test:coverage` antes do scanner | AAP2-09; FE lcov fresh | y |
| B10 — Branch | `feat/adequacao-analise-projeto-r2` a partir de `main` @ pós-merge | Convenção fix/r2 | y |

**Open questions:** none — Plano C híbrido confirmado pelo usuário.

### Implicit-requirement dimensions (Large)

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | N/A — R2 não adiciona endpoints |
| Failure / partial-failure | AAP2-10/11: transação correta provada por teste |
| Idempotency / retry | N/A |
| Auth boundaries | AAP2-13/14: JWT + login hardening |
| Concurrency / ordering | N/A |
| Data lifecycle | AAP2-12: ddl-auto validate |
| Observability | N/A — não logar Authorization header (follow-up se tocado) |
| External-dependency failure | N/A |
| State-transition integrity | AAP2-10/11: rollback transacional |

---

## User Stories

### P1: Quality Gate Sonar — violations e cobertura incremental ⭐ MVP

**User Story**: Como Tech Lead, quero o Quality Gate Sonar verde (ou exceções mínimas documentadas), para que merge futuro tenha gate objetivo.

**Why P1**: R1 deixou QG ERROR; principal bloqueio percebido pós-entrega.

**Acceptance Criteria**:

1. **(AAP2-01)** WHEN `./diversos/scripts/sonar-analyze.sh` completar em `main` pós-R2 THEN Quality Gate status SHALL ser **OK** OR `validation.md` SHALL listar ≤2 exceções com rationale
2. **(AAP2-02)** WHEN Sonar API consultar condição `new_violations` THEN valor SHALL ser **0**
3. **(AAP2-03)** WHEN Sonar API consultar `new_coverage` THEN valor SHALL ser **≥ 80%** OR exceção única documentada em validation com plano FE
4. **(AAP2-04)** WHEN Sonar API consultar `code_smells` THEN contagem SHALL ser **≤ 230** (≥15% redução vs 270 pós-R1)
5. **(AAP2-05)** WHEN issues Sonar forem exportadas (CRITICAL+MAJOR, `sinceLeakPeriod=true`) THEN zero issues SHALL permanecer nos **top 20 arquivos** por densidade (folha, importacao, auth, config)
6. **(AAP2-06)** WHEN `./diversos/scripts/sonar-analyze.sh` rodar THEN script SHALL executar `npm run test:coverage` antes do scanner Docker

**Independent Test**: Script sonar-analyze exit 0; API QG + measures; diff smells count.

---

### P1: Cobertura backend e frontend ⭐ MVP

**User Story**: Como mantenedor, quero cobertura incrementally alta em backend e FE nas telas críticas, para sustentar `new_coverage` e reduzir regressões.

**Why P1**: `new_coverage` 60.4% bloqueia QG; FE é o gargalo.

**Acceptance Criteria**:

1. **(AAP2-07)** WHEN `bash diversos/scripts/check-jacoco-thresholds.sh` rodar THEN global backend SHALL ser **≥ 75%** (floors R1 mantidos: organograma ≥50%, security ≥40%, importacao ≥75%)
2. **(AAP2-08)** WHEN `cd frontend && npm run test:coverage` rodar THEN SHALL existir **≥ 15** test cases passando (total Vitest)
3. **(AAP2-09)** WHEN cobertura FE for medida THEN arquivos `pages/FolhaPagamento`, `pages/Organograma`, `pages/Login` (ou equivalente auth) SHALL ter ≥1 teste cada (render ou behavior)
4. **(AAP2-10)** WHEN Sonar importar lcov THEN cobertura agregada Sonar SHALL ser **≥ 48%**
5. **(AAP2-11)** WHEN `GlobalExceptionHandler` for ampliado THEN SHALL existir teste para handler de validação (`MethodArgumentNotValidException` ou equivalente documentado)

**Independent Test**: JaCoCo script; Vitest count; Sonar coverage measure; test files grep.

---

### P2: Dívida CONCERNS — transação, schema, JWT ⭐ MVP operacional

**User Story**: Como responsável por ops, quero fechar dívidas P1 de CONCERNS que afetam integridade de dados e deploy seguro.

**Why P2**: Explicitamente adiadas na R1 (AAP-22, A8, code-review security).

**Acceptance Criteria**:

1. **(AAP2-12)** WHEN `FolhaTotalizacaoService` executar métodos transacionais via `this` THEN SHALL refatorar (extract private non-@Transactional helper ou self-injection pattern do repo) sem quebrar `FolhaTotalizacaoServiceTest`
2. **(AAP2-13)** WHEN `OrganogramaAcessoService` tiver self-invocation `@Transactional` THEN SHALL aplicar mesmo padrão; `OrganogramaAcessoServiceTest` verde
3. **(AAP2-14)** WHEN `application.yml` for auditado THEN `spring.jpa.hibernate.ddl-auto` SHALL ser **`validate`** no profile default; `update` permitido **somente** com profile `dev` ativo
4. **(AAP2-15)** WHEN `JWT_SECRET` estiver vazio ou igual ao default THEN `JwtSecretStartupValidator` SHALL falhar startup em qualquer profile exceto `dev`/`test`
5. **(AAP2-16)** WHEN login falhar por usuário inexistente THEN `AuthenticationService` SHALL invocar `passwordEncoder.matches` contra hash dummy (constant-time path); teste discrimina timing bypass
6. **(AAP2-17)** WHEN `JwtAuthenticationFilter` logar header inválido THEN log SHALL **not** incluir valor do token/header completo

**Independent Test**: Unit tests existentes + novos; startup validator tests; grep logs.

---

### P2: Gate, documentação e regressão zero

**User Story**: Como desenvolvedor, quero gates verdes e CONCERNS sincronizado após R2.

**Acceptance Criteria**:

1. **(AAP2-18)** WHEN `cd backend && mvn test` THEN **0** falhas (contagem ≥ 359, sem deleção silenciosa)
2. **(AAP2-19)** WHEN `./diversos/scripts/sonar-analyze.sh` completar THEN bugs OPEN = **0** e vulns CRITICAL+MAJOR = **0** (regressão R1)
3. **(AAP2-20)** WHEN R2 fechar THEN `_docs/specs/CONCERNS.md` SHALL marcar resolvidos: self-invocation tx, ddl-auto, JWT hardening, timing login
4. **(AAP2-21)** WHEN `ModularArchitectureTest` rodar THEN zero violações AD-010

**Independent Test**: Full gate suite; CONCERNS diff; ArchUnit.

---

### P3: Integração pontual importação (opcional se P1–P2 verdes)

**User Story**: Como mantenedor, quero **um** teste de integração da importação ADP com DB real (Testcontainers ou `@SpringBootTest` mínimo), para complementar fixtures unitárias.

**Why P3**: CONCERNS marca importação como frágil; R1 só unit.

**Acceptance Criteria**:

1. **(AAP2-22)** WHEN teste integração ADP rodar THEN SHALL persistir ≥1 linha em fixture mínima com rollback **ou** `@Transactional` test rollback; documentado em validation se skip por infra

**Independent Test**: `mvn test -Dtest=*Importacao*Integration*` ou tag equivalente.

---

## Edge Cases

- WHEN QG falhar **somente** em `new_coverage` após FE tests THEN AAP2-01 permite 1 exceção se agregado ≥48% e plano R3 documentado
- WHEN refactor tx quebrar totalização folha THEN SHALL reverter e marcar SPEC_DEVIATION
- WHEN Testcontainers indisponível no ambiente THEN AAP2-22 = N/A documentado; não bloqueia R2
- WHEN smell fix exigir refactor >50 LOC THEN registrar follow-up CONCERNS; não expandir task

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| AAP2-01 | P1: QG | Fase 1 | Mapped → C0,C10,C11 |
| AAP2-02 | P1: QG | Fase 1 | Mapped → C0,C10 |
| AAP2-03 | P1: QG | Fase 1 | Mapped → C0,C9 |
| AAP2-04 | P1: QG | Fase 1 | Mapped → C10 |
| AAP2-05 | P1: QG | Fase 1 | Mapped → C10 |
| AAP2-06 | P1: QG | Fase 1 | Mapped → C0 |
| AAP2-07 | P1: Coverage | Fase 1 | Mapped → C8 |
| AAP2-08 | P1: Coverage | Fase 1 | Mapped → C9 |
| AAP2-09 | P1: Coverage | Fase 1 | Mapped → C9 |
| AAP2-10 | P1: Coverage | Fase 1 | Mapped → C0,C9 |
| AAP2-11 | P1: Coverage | Fase 1 | Mapped → C7 |
| AAP2-12 | P2: CONCERNS | Fase 2 | Mapped → C1 |
| AAP2-13 | P2: CONCERNS | Fase 2 | Mapped → C2 |
| AAP2-14 | P2: CONCERNS | Fase 2 | Mapped → C3 |
| AAP2-15 | P2: CONCERNS | Fase 2 | Mapped → C4 |
| AAP2-16 | P2: CONCERNS | Fase 2 | Mapped → C5 |
| AAP2-17 | P2: CONCERNS | Fase 2 | Mapped → C6 |
| AAP2-18 | P2: Gate | Fase 2 | Mapped → gates |
| AAP2-19 | P2: Gate | Fase 2 | Mapped → C0,C10 |
| AAP2-20 | P2: Gate | Fase 2 | Mapped → C11 |
| AAP2-21 | P2: Gate | Fase 2 | Mapped → ArchUnit gate |
| AAP2-22 | P3: Integração | Fase 3 | Mapped → C12 (optional) |

**Coverage:** 22 total, 22 mapped, 0 unmapped

---

## Success Criteria

- [x] Sonar QG OK ou ≤2 exceções em `validation.md`
- [x] `new_violations` = 0; smells ≤ 230; bugs/vulns = 0
- [x] JaCoCo global ≥ 75%; Vitest ≥ 15 tests; Sonar coverage ≥ 48%
- [x] CONCERNS P1 items (tx self-invocation, ddl-auto, JWT) resolvidos
- [x] `mvn test` verde; `./diversos/scripts/sonar-analyze.sh` exit 0
- [x] Nenhuma breaking change HTTP/DTO

---

## Baseline pós-R1 (referência R2 — `main` @ `047e64d`)

| Métrica | Valor pós-R1 |
| ------- | ------------ |
| Sonar bugs | 0 |
| Sonar vulns CR+MAJOR | 0 |
| Code smells | 270 |
| Cobertura agregada Sonar | 40.7% |
| QG | ERROR (`new_violations` 126, `new_coverage` 60.4%) |
| JaCoCo global backend | 71.5% |
| Testes backend | 359 |
| Testes frontend | 2 |

**Metas R2 resumidas:** QG OK · smells ≤230 · coverage ≥48% · JaCoCo ≥75% · FE ≥15 tests · CONCERNS P1 closed.

---

## Próximos passos (TLC)

1. ~~**Design**~~ — `design.md`
2. ~~**Tasks**~~ — `tasks.md` (T1–T20; batches 1–3 + optional T20)
3. ~~**Execute**~~ — merged @ `feat/adequacao-analise-projeto` → `main` (validation PASS)
