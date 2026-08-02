# Codebase Concerns

**Analysis Date:** 2026-07-25  
**Last sync (adequação P2):** 2026-07-29

## Known Bugs

**Relatórios UI without backend:**

- Symptoms: Página Relatórios falha em listar/gerar/download (API 404).
- Trigger: Navegar para `/relatorios` e acionar qualquer ação.
- Files: `frontend/src/services/relatorioService.ts`, `frontend/src/pages/Relatorios/index.tsx` — sem controller Java correspondente
- Workaround: Usar resumo de folha / exports existentes; evitar menu Relatórios até backend existir.
- Root cause: Frontend à frente do backend; README menciona PDF sem dependência PDF no `pom.xml`.
- **Status:** Open (fora escopo adequação P2)

## Security Considerations

**JWT secret default in config:**

- Risk: Chave de assinatura previsível se `JWT_SECRET` não for definido no deploy.
- Files: `backend/src/main/resources/application.yml` (`jwt.secret` com default longo)
- Current mitigation: Override via `${JWT_SECRET:...}`; **`JwtSecretStartupValidator` fail-fast em blank/default fora de profiles `dev`/`test`** (R2 T15 / AAP2-15).
- Recommendations: Exigir secret em produção; rotacionar; nunca commitár secrets reais.
- **Status:** Resolved (R2 hardening — blank + default blocked non-dev/test)

**SecurityConfig path prefix inconsistency:**

- Risk: Matchers explícitos com `/api/...` podem não alinhar com `context-path: /api`.
- Files: `backend/.../config/SecurityConfig.java`
- Current mitigation: Auditado em P1 T5; CSRF/JWT documentados em `_docs/specs/INTEGRATIONS.md`.
- Recommendations: Manter testes regressão `SecurityConfig*Test`.
- **Status:** Mitigated (audit + docs); Sonar S4502 (CSRF) permanece — aceito para API JWT stateless

**CSRF disabled:**

- Risk: Aceitável para API JWT stateless; perigoso se auth por cookie for introduzida.
- Files: `SecurityConfig.java` — `csrf.disable()`
- Recommendations: Manter stateless documentado; reavaliar se cookies de sessão forem usados.
- **Status:** Documented (INTEGRATIONS.md, AAP-07)

**Hardcoded CORS / credentials in repo:**

- Risk: Origins LAN embutidas; password `postgres` em YAML/Compose.
- Files: `config/WebConfig.java`, `application.yml`, `docker-compose.yml`
- Recommendations: Externalizar origins e secrets por ambiente.
- **Status:** Open — follow-up infra

**Organograma access edge cases:**

- Risk: Usuário sem `funcionario` → sem acesso; permissão `ACESSO_TOTAL` → acesso amplo.
- Files: `organograma/acesso/application/OrganogramaAcessoService.java`
- Current mitigation: Testes unitários expandidos (adequação P2 T8 / AAP-11).
- **Status:** Mitigated (unit coverage); integração E2E pendente

## Tech Debt

**Hibernate ddl-auto + Flyway:**

- Issue: `spring.jpa.hibernate.ddl-auto: update` junto com Flyway pode gerar drift fora das migrações.
- Files: `application.yml`, `application-dev.yml`, `db/migration/*.sql`
- Impact: Schemas não reproduzíveis entre ambientes.
- Fix approach: `validate` default; `update` somente com profile `dev` explícito (`SPRING_PROFILES_ACTIVE=dev`).
- **Status:** Resolved (R2 T14 / AAP2-14)

**Datasource LAN commitado:**

- Issue: JDBC default `192.168.68.110:5433`.
- Files: `application.yml`
- Impact: Falha local ou escrita acidental em DB compartilhado.
- Fix approach: Default `localhost:5433`; override por env no Compose.
- **Status:** Open

**Dual model de benefícios (legado + mensal):**

- Issue: Entity `Beneficio`, fallback em totalização/dashboard permanecem; frontend `beneficioService.ts` órfão.
- Files: `model/Beneficio.java`, `FolhaTotalizacaoService.java`, `frontend/src/services/beneficioService.ts`
- Impact: Dois caminhos de custo de benefício.
- Fix approach: Plano de migração quando `beneficio_mensal` for fonte única.
- **Status:** Open

**ADP import hardcoded mappings:**

- Issue: Códigos de empresa e rubricas ignoradas embutidos em Java.
- Files: `ImportacaoFolhaAdpService.java`
- Impact: Nova filial/rubrica exige redeploy.
- Fix approach: Tabelas de config ou YAML administrável.
- **Status:** Mitigated — unit tests + `ImportacaoFolhaAdpIntegrationTest` (Testcontainers, Docker-gated)

**Docker Java 21 vs pom Java 17:**

- Issue: Bytecode alvo 17; imagem build/runtime Temurin 21.
- Files: `pom.xml`, `Dockerfile`
- **Status:** Open

**Unused frontend dependency / dead code:**

- Issue: `@tanstack/react-query` instalado sem uso; órfãos em `frontend/src/`.
- **Status:** Open — P3 cleanup

**Axios typing shortcuts:**

- Issue: `@ts-ignore` e `any` nos interceptors.
- Files: `frontend/src/services/api.ts`
- **Status:** Open

## Fragile Areas

**Importação ADP em transação única:**

- Files: `ImportacaoFolhaAdpService.java`
- Why fragile: Arquivo grande + `@Transactional` amplo — timeout/rollback total.
- Safe modification: Mudar parse/mapeamento com fixtures.
- Test coverage: **`ImportacaoFolhaAdpServiceTest` + `ImportacaoFolhaAdpIntegrationTest`** (adequação R3 T11; R4 live N/A — Testcontainers skip).
- **Status:** Mitigated (unit + integration Docker-gated; live evidence N/A @ R4 T10)

**FolhaTotalizacaoService dual source:**

- Files: `FolhaTotalizacaoService.java`
- Why fragile: Presença de `BeneficioMensal` muda fonte para competência inteira.
- Test coverage: Unitário presente (`FolhaTotalizacaoServiceTest`).
- **Status:** Open — cenário importação parcial

**Security matchers + role ADMIN:**

- Files: `SecurityConfig.java`, controllers benefícios
- Why fragile: Paths com/sem `/api`.
- Safe modification: Validar com `MockMvc`/`SecurityConfig*Test`.
- **Status:** Mitigated (regressão P1 T5)

## Missing Critical Features

**Backend de Relatórios / PDF:**

- Problem: UI e service frontend existem sem API.
- **Status:** Open

## Test Coverage Gaps

**AD-014 gate (linha + branch ≥ 95%, BE + FE):**

- **Status:** **Closed** (2026-08-01, feature `cobertura-testes-95` T21)
- Gate: `bash diversos/scripts/check-coverage-95.sh` — BE LINE 96.57%, BE BRANCH 95.39%, FE Lines 97.38%, FE Branches 95.01%
- Contagens: **1044** testes BE (1 skip Docker-gated), **436** testes FE (32 arquivos)
- Branches inatingíveis documentados: `_docs/specs/features/cobertura-testes-95/validation.md` (COV-09)

**Controllers / Security / Repositories (integração):**

- What's not tested: Repositories SQL reais, integração end-to-end completa.
- Progress: WebMvc controllers, JWT filter, GlobalExceptionHandler, ADP import unit + Docker-gated integration.
- **Status:** Partially closed; Testcontainers follow-up for E2E DB paths

**Frontend (residual informacional):**

- Per-file branch gaps em algumas páginas (Rubricas, FolhaPagamento) permanecem abaixo de 100%; agregado FE ≥95% via gate AD-014.
- E2E: Playwright login smoke (`e2e/login.spec.ts`); ADP live path não exercido nesta run.
- **Status:** AD-014 meta closed; E2E smoke maintained

**Harness docs:**

- Canonical commands: `_docs/specs/TESTING.md`; gate 95%: `diversos/scripts/check-coverage-95.sh`

**Exception handler:**

- Progress: `GlobalExceptionHandlerTest` (T11 / AAP-15).
- Remaining: `MethodArgumentNotValidException` handler ausente.
- **Status:** Partially closed

## Sonar follow-ups (post P2 gate)

| Item | Severity | Location | Target | Status (P3 T15) |
| ---- | -------- | -------- | ------ | --------------- |
| S5804 user enumeration | MAJOR | `AuthenticationService` | Unified login/refresh failure paths | **Mitigated** (T15) |
| S4502 CSRF disabled | CRITICAL | `SecurityConfig` | `@SuppressWarnings` + INTEGRATIONS.md | **Suppressed** (T15) |
| S2245 pseudorandom | MAJOR | `FolhaPagamento/index.tsx` | Replace `Math.random` key | **Resolved** (R3 T12 — stable `id` keys + regression test) |
| `@Transactional` via `this` (Folha/Organograma) | CRITICAL | `FolhaTotalizacaoService`, `OrganogramaAcessoService` | Self-invocation bypasses proxy | **Resolved** (R2 T12–T13 / AAP2-12, AAP2-13) |
| `@Transactional` via `this` (BeneficioMensal) | CRITICAL | `BeneficioMensalService` | Extract tx helpers; remove S6809 suppress | **Resolved** (R3 T13) |
| Login timing side-channel | MAJOR | `AuthenticationService` | Dummy BCrypt on missing user | **Resolved** (R2 T16 / AAP2-16) |
| JWT filter logs Authorization | MAJOR | `JwtAuthenticationFilter` | Redact header value from debug logs | **Resolved** (R2 T17 / AAP2-17) |

---

_Concerns audit: 2026-07-25_  
_Sync adequação P2: 2026-07-29 (JaCoCo gate pass; Sonar bugs=0; vulns CRITICAL+MAJOR=4 documented in validation.md)_  
_Sync adequação R3: 2026-07-29 (Sonar QG OK; new_coverage 80.0%; MSW/api.ts FE coverage; S2245 + BeneficioMensal tx resolved; ADP integration mitigated)_  
_Sync adequação R4: 2026-07-29 (Playwright E2E smoke; 186 Vitest; gate-r4-local.sh; Sonar 80.0% QG OK — meta 85% informacional open; ver `_docs/specs/features/adequacao-analise-projeto-r4/validation.md`)_  
_Sync cobertura AD-014: 2026-08-01 (gate 95% BE+FE verde; 1044 BE / 436 FE; `check-coverage-95.sh` canônico — ver `_docs/specs/features/cobertura-testes-95/validation.md`)_
