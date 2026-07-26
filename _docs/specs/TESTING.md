# Testing Infrastructure

**Analyzed:** 2026-07-25

## Test Frameworks

**Unit/Integration:** JUnit 5 + Mockito via `spring-boot-starter-test` (backend)  
**E2E:** Nenhum (Playwright/Cypress não presentes)  
**Coverage:** Sem JaCoCo/cobertura configurada no `pom.xml`  
**Frontend:** Sem Vitest/Jest/Testing Library — apenas `npm run lint`

## Test Organization

**Location:** `backend/src/test/java/br/com/techne/sistemafolha/service/`  
**Naming:** `*ServiceTest.java` espelhando o service  
**Structure:** Classes unitárias com `@ExtendWith(MockitoExtension.class)`, `@Mock` repositories, `@InjectMocks` service

**Classes atuais (5):**

- `FuncionarioServiceTest.java`
- `BeneficioMensalServiceTest.java`
- `TipoBeneficioServiceTest.java`
- `ImportacaoBeneficioMensalServiceTest.java`
- `FolhaTotalizacaoServiceTest.java`

## Testing Patterns

### Unit Tests

**Approach:** Isolar service; mockar repositórios; assertir regras com `assertThrows` e mensagens em PT  
**Location:** `backend/src/test/java/.../service/`  
**Dados:** Entities/DTOs construídos inline — sem `application-test.yml` nem fixtures SQL

### Integration Tests

**Approach:** Não estabelecido no repositório (`@SpringBootTest` / Testcontainers ausentes)

### E2E / API manual

**Approach:** Postman + shell scripts  
**Location:** `diversos/postman/`, `diversos/scripts/test-api.sh` (e scripts de importação)

## Test Execution

**Commands:**

| Ação | Comando |
|------|---------|
| Todos os testes backend | `cd backend && mvn test` |
| Classe | `cd backend && mvn test -Dtest=FuncionarioServiceTest` |
| Método | `cd backend && mvn test -Dtest=FuncionarioServiceTest#cadastrar_rejeita_cpf_ativo_duplicado` |
| Build sem testes | `cd backend && mvn clean package -DskipTests` |
| Lint frontend | `cd frontend && npm run lint` |
| Build frontend | `cd frontend && npm run build` |

**Configuration:** Sem perfil de teste dedicado; datasource de runtime não é usado pelos unit tests atuais (mocks).

## Coverage Targets

**Current:** Não mensurado automaticamente  
**Goals:** Não documentados no projeto  
**Enforcement:** Nenhum gate de coverage no CI local visível

## Test Coverage Matrix

| Code Layer | Required Test Type | Location Pattern | Run Command |
| ---------- | ------------------ | ---------------- | ----------- |
| Backend services (regras) | unit (Mockito) | `backend/src/test/java/**/service/*Test.java` | `cd backend && mvn test` |
| Backend controllers | none (gap) | — | — |
| Backend security/JWT | none (gap) | — | — |
| Backend repositories / SQL | none (gap) | — | — |
| Flyway migrations | none (manual/migrate) | `db/migration/V*.sql` | `cd backend && mvn flyway:migrate` |
| Frontend pages/services | none (gap) | — | `cd frontend && npm run lint` (só estático) |
| Importação ADP/XLSX | unit parcial (benefício mensal) | `ImportacaoBeneficioMensalServiceTest` | `mvn test -Dtest=ImportacaoBeneficioMensalServiceTest` |
| API E2E | manual Postman | `diversos/postman/` | scripts em `diversos/scripts/` |

## Parallelism Assessment

| Test Type | Parallel-Safe? | Isolation Model | Evidence |
| --------- | -------------- | --------------- | -------- |
| Backend unit (Mockito) | Yes | Sem DB compartilhado; mocks por instância de teste | `*ServiceTest.java` com `@ExtendWith(MockitoExtension.class)` |
| Frontend | N/A | Sem suite | — |
| Manual Postman | No | Mesmo Postgres de ambiente | scripts contra API real |

## Gate Check Commands

| Gate Level | When to Use | Command |
| ---------- | ----------- | ------- |
| Quick | Após task só de service | `cd backend && mvn test` |
| Full | Após mudanças de API + UI | `cd backend && mvn test` && `cd frontend && npm run lint && npm run build` |
| Build | Fechamento de fase / Docker | `cd backend && mvn clean package` && `cd frontend && npm run build` (ou `docker compose build`) |

> **TARGET:** a skill `testing-a11y` (Vitest/Playwright/MSW) não é obrigação até ROADMAP liberar adequação FE — ver `STRUCTURE.md` (Current vs TARGET).
