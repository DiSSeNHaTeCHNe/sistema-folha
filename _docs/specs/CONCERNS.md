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
- Current mitigation: Override via `${JWT_SECRET:...}`; **`JwtSecretStartupValidator` fail-fast em prod** (adequação P1 T6 / AAP-09).
- Recommendations: Exigir secret em produção; rotacionar; nunca commitár secrets reais.
- **Status:** Mitigated (startup validator); monitorar deploy env

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
- Files: `application.yml`, `db/migration/*.sql`
- Impact: Schemas não reproduzíveis entre ambientes.
- Fix approach: `validate`/`none` fora de dev; schema só via Flyway.
- **Status:** Open

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
- **Status:** Open; **test coverage added** (P2 T10, fixture `folha-adp-minimal.txt`)

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
- Test coverage: **`ImportacaoFolhaAdpServiceTest` + fixtures** (adequação P2 T10 / AAP-13).
- **Status:** Mitigated (unit tests); integração com DB real pendente

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

**Controllers / Security / Repositories:**

- What's not tested: Repositories SQL reais, integração end-to-end.
- Progress (adequação P2): JWT filter/service tests (T9), GlobalExceptionHandler (T11), importação ADP (T10), organograma service+ACL+controller smoke (T8/T8-ext).
- **Status:** Partially closed; Testcontainers follow-up

**Frontend:**

- What's not tested: Páginas, interceptors, organograma.
- **Status:** Open — AAP-23 Vitest baseline (Batch 3)

**Exception handler:**

- Progress: `GlobalExceptionHandlerTest` (T11 / AAP-15).
- Remaining: `MethodArgumentNotValidException` handler ausente.
- **Status:** Partially closed

## Sonar follow-ups (post P2 gate)

| Item | Severity | Location | Target |
| ---- | -------- | -------- | ------ |
| S5804 user enumeration | MAJOR | `AuthenticationService` | Batch 3 hygiene or Sonar accept |
| S4502 CSRF disabled | CRITICAL | `SecurityConfig` | Documented; accept for JWT API |
| S2245 pseudorandom | MAJOR | `FolhaPagamento/index.tsx` | Batch 3 FE |
| `@Transactional` via `this` | CRITICAL | `FolhaTotalizacaoService`, `OrganogramaAcessoService` | AAP-22 Batch 3 if not touched |

---

_Concerns audit: 2026-07-25_  
_Sync adequação P2: 2026-07-29 (JaCoCo gate pass; Sonar bugs=0; vulns CRITICAL+MAJOR=4 documented in validation.md)_
