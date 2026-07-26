# Codebase Concerns

**Analysis Date:** 2026-07-25

## Known Bugs

**Relatórios UI without backend:**

- Symptoms: Página Relatórios falha em listar/gerar/download (API 404).
- Trigger: Navegar para `/relatorios` e acionar qualquer ação.
- Files: `frontend/src/services/relatorioService.ts`, `frontend/src/pages/Relatorios/index.tsx` — sem controller Java correspondente
- Workaround: Usar resumo de folha / exports existentes; evitar menu Relatórios até backend existir.
- Root cause: Frontend à frente do backend; README menciona PDF sem dependência PDF no `pom.xml`.

## Security Considerations

**JWT secret default in config:**

- Risk: Chave de assinatura previsível se `JWT_SECRET` não for definido no deploy.
- Files: `backend/src/main/resources/application.yml` (`jwt.secret` com default longo)
- Current mitigation: Override via `${JWT_SECRET:...}`.
- Recommendations: Exigir secret em produção; rotacionar; nunca commitár secrets reais.

**SecurityConfig path prefix inconsistency:**

- Risk: Matchers explícitos com `/api/...` podem não alinhar com `context-path: /api` (controllers sem prefixo). Matcher legado `/api/beneficios/**` não corresponde mais às rotas `/beneficio-mensal` e `/tipo-beneficio`.
- Files: `backend/.../config/SecurityConfig.java`
- Current mitigation: `anyRequest().authenticated()` ainda protege rotas novas; regras `ADMIN` em tipo-benefício usam paths com `/api`.
- Recommendations: Auditar matchers contra `context-path`; remover `/api/beneficios/**` obsoleto; testes de segurança por endpoint.

**CSRF disabled:**

- Risk: Aceitável para API JWT stateless; perigoso se auth por cookie for introduzida.
- Files: `SecurityConfig.java` — `csrf.disable()`
- Recommendations: Manter stateless documentado; reavaliar se cookies de sessão forem usados.

**Hardcoded CORS / credentials in repo:**

- Risk: Origins LAN embutidas; password `postgres` em YAML/Compose.
- Files: `config/WebConfig.java`, `application.yml`, `docker-compose.yml`
- Recommendations: Externalizar origins e secrets por ambiente.

**Organograma access edge cases:**

- Risk: Usuário sem `funcionario` → sem acesso; funcionário sem nó → **acesso total** (regra 4).
- Files: `service/OrganogramaAcessoService.java`
- Recommendations: Decisão de produto explícita + testes de integração por regra.

## Tech Debt

**Hibernate ddl-auto + Flyway:**

- Issue: `spring.jpa.hibernate.ddl-auto: update` junto com Flyway pode gerar drift fora das migrações.
- Files: `application.yml`, `db/migration/*.sql`
- Impact: Schemas não reproduzíveis entre ambientes.
- Fix approach: `validate`/`none` fora de dev; schema só via Flyway.

**Datasource LAN commitado:**

- Issue: JDBC default `192.168.68.110:5433`.
- Files: `application.yml`
- Impact: Falha local ou escrita acidental em DB compartilhado.
- Fix approach: Default `localhost:5433`; override por env no Compose.

**Dual model de benefícios (legado + mensal):**

- Issue: CRUD/API legado `/beneficios` removido, mas entity `Beneficio`, `BeneficioRepository`, tabela `beneficios` e fallback em totalização/dashboard permanecem; frontend `beneficioService.ts` órfão.
- Files: `model/Beneficio.java`, `repository/BeneficioRepository.java`, `FolhaTotalizacaoService.java`, `DashboardService.java`, `frontend/src/services/beneficioService.ts`
- Impact: Dois caminhos de custo de benefício; risco de dados inconsistentes se competência mensal e legado coexistirem.
- Fix approach: Plano de migração de dados → retirar fallback e código morto quando `beneficio_mensal` for fonte única.

**ADP import hardcoded mappings:**

- Issue: Códigos de empresa e rubricas ignoradas embutidos em Java.
- Files: `ImportacaoFolhaAdpService.java`
- Impact: Nova filial/rubrica exige redeploy.
- Fix approach: Tabelas de config ou YAML administrável.

**Docker Java 21 vs pom Java 17:**

- Issue: Bytecode alvo 17; imagem build/runtime Temurin 21.
- Files: `pom.xml`, `Dockerfile`
- Impact: Confusão de versão para contribuidores.
- Fix approach: Alinhar 17 ou 21 de ponta a ponta.

**Unused frontend dependency / dead code:**

- Issue: `@tanstack/react-query` instalado sem uso; `beneficioService.ts`, `pages/Example`, `App.tsx`/`App.css` fora do entry `main.tsx`.
- Files: `frontend/package.json`, `frontend/src/services/beneficioService.ts`, `frontend/src/main.tsx`
- Impact: Ruído e falsa expectativa de padrões.
- Fix approach: Remover ou adotar Query; limpar órfãos.

**Axios typing shortcuts:**

- Issue: `@ts-ignore` e `any` nos interceptors.
- Files: `frontend/src/services/api.ts`
- Fix approach: Instância Axios tipada.

## Fragile Areas

**Importação ADP em transação única:**

- Files: `ImportacaoFolhaAdpService.java`
- Why fragile: Arquivo grande + `@Transactional` amplo — timeout/rollback total.
- Common failures: Upload > limites, layout divergente, mapeamento de empresa ausente.
- Safe modification: Mudar parse/mapeamento com fixtures; evitar side effects fora da transação.
- Test coverage: Sem unit test dedicado à importação ADP (há testes para benefício mensal).

**FolhaTotalizacaoService dual source:**

- Files: `FolhaTotalizacaoService.java`
- Why fragile: Presença de qualquer `BeneficioMensal` ativo na competência muda a fonte para todos os funcionários daquele período (via `existsByCompetencia...`).
- Common failures: Competência parcialmente importada mascara legado.
- Safe modification: Cobrir com testes de ambos os ramos (já parcialmente em `FolhaTotalizacaoServiceTest`).
- Test coverage: Unitário presente; falta cenário de “parcialidade” de importação.

**Security matchers + role ADMIN:**

- Files: `SecurityConfig.java`, `TipoBeneficioController.java`
- Why fragile: Paths com/sem `/api` e mudança recente de domínio benefícios.
- Safe modification: Validar com testes `MockMvc`/`@SpringBootTest` + `spring-security-test`.

## Missing Critical Features

**Backend de Relatórios / PDF:**

- Problem: UI e service frontend existem sem API.
- Current workaround: Outras telas de resumo.
- Blocks: Entrega de relatórios oficiais pelo menu Relatórios.
- Implementation complexity: Média–alta (agregações + geração PDF/export).

## Test Coverage Gaps

**Controllers / Security / Repositories:**

- What's not tested: Camada HTTP, JWT filter, queries SQL reais, importação ADP.
- Risk: Regressões de auth/path e SQL só aparecem em runtime.
- Priority: High (security + importação), Medium (CRUD controllers)
- Difficulty: Precisa Testcontainers ou DB de teste + `@SpringBootTest`.

**Frontend:**

- What's not tested: Páginas, interceptors de auth, organograma.
- Risk: Quebras de UX/ACL sem detecção no CI.
- Priority: Medium
- Difficulty: Requer setup Vitest/RTL do zero.

**Exception handler incompleto:**

- What's not tested / uncovered: Algumas exceções de domínio fora do `GlobalExceptionHandler`; sem handler explícito para `MethodArgumentNotValidException`.
- Risk: Respostas 500 genéricas ou tratamento inconsistente.
- Priority: Medium
- Files: `exception/GlobalExceptionHandler.java`

---

_Concerns audit: 2026-07-25_  
_Update as issues are fixed or new ones discovered_
