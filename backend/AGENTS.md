# Backend — AGENTS.md

Contexto operacional para agentes trabalhando na API Java deste repositório.  
Governança geral do monorepo: `../AGENTS.md`. Specs técnicas: `../_docs/specs/`.

---

## 1. Build & Test Commands

**Build tool:** Maven (sem wrapper — use `mvn` global). **Sempre execute a partir de `backend/`.**

| Ação | Comando |
|------|---------|
| Build completo | `mvn clean package -DskipTests` |
| Build + testes | `mvn clean install` |
| Todos os testes | `mvn test` |
| Cobertura JaCoCo | `mvn test` → `target/site/jacoco/index.html` (+ `jacoco.xml`) |
| Classe de teste | `mvn test -Dtest=FuncionarioServiceTest` |
| Método específico | `mvn test -Dtest=FuncionarioServiceTest#cadastrar_rejeita_cpf_ativo_duplicado` |
| Subir API local | `mvn spring-boot:run` |
| Migrações Flyway | `mvn flyway:migrate` |

**Lint/Checkstyle:** não configurado no `pom.xml`. Não invente plugins.

**JaCoCo:** `jacoco-maven-plugin` gera relatório na fase `test`. O Sonar local (`./diversos/scripts/sonar-up.sh`) importa `target/site/jacoco/jacoco.xml` via `./diversos/scripts/sonar-analyze.sh`.

**Perfis Maven:** nenhum (`-Pdev` / `-Pprod` não existem).

**Pré-requisitos locais:**
- Java **17** (versão declarada no `pom.xml`; Docker usa Temurin 21, mas o bytecode alvo é 17).
- PostgreSQL acessível. Via `docker compose` na raiz: host **`localhost:5433`**, DB `sistema_folha`, user/pass `postgres`.
- Ajuste `src/main/resources/application.yml` (`spring.datasource.url`) antes de rodar — o valor commitado aponta para IP fixo da rede interna.

**URLs em dev:** API em `http://localhost:8083/api`, Swagger em `http://localhost:8083/api/swagger-ui.html`.

---

## 2. Code Style & Conventions

| Item | Valor |
|------|-------|
| Java | 17 |
| Framework | Spring Boot **3.2.3** (Jakarta EE, não `javax.*`) |
| ORM | Spring Data JPA + Hibernate |
| Banco | PostgreSQL 15, migrações Flyway |
| Auth | Spring Security + JWT (jjwt 0.12) |
| Docs API | springdoc-openapi 2.3 |

**Estilo:**
- Pacote base: `br.com.techne.sistemafolha`.
- **Lombok:** `@RequiredArgsConstructor` + `private final` nos services/controllers; `@Data` nas entities.
- **DTOs:** `record` com validação Jakarta (`@NotBlank`, `@NotNull`) e `@Schema` do OpenAPI.
- **Entities:** classes JPA em `model/` com `@Entity`, `@Table`, relacionamentos `@ManyToOne`/`@OneToMany`.
- **Sem MapStruct** — conversão Entity↔DTO manual nos services (`toDTO`, `fromDTO`).
- **Injeção:** constructor injection via Lombok; evite `@Autowired` em campo.
- **Controllers:** finos — delegam para service, retornam `ResponseEntity<T>`, anotados com `@Tag`/`@Operation`.
- **Idioma:** mensagens de negócio e validação em **português**.

**Tratamento de erros:**
- Exceções de domínio em `exception/` (ex.: `FuncionarioNotFoundException`).
- Respostas padronizadas via `GlobalExceptionHandler` (`@RestControllerAdvice`) → `ErrorResponse`.
- Validações de regra de negócio podem usar `IllegalArgumentException` (mapeada para 400).
- Não engula exceptions; não retorne stack trace ao cliente.

**Transações:**
- Métodos de escrita em services: `@Transactional`.
- Leituras pesadas/agregações: `@Transactional(readOnly = true)`.

---

## 3. Architecture & Directory Structure

Monolito Spring Boot em camadas — **103 classes**, pacote único:

```text
backend/src/main/java/br/com/techne/sistemafolha/
├── SistemaFolhaApplication.java   # Entry point
├── config/                        # SecurityConfig, WebConfig, AuthenticationConfig
├── controller/                    # REST — @RequestMapping relativo (sem prefixo /api)
├── service/                       # Regras de negócio + mapeamento DTO
├── repository/                    # Spring Data JPA interfaces
├── model/                         # Entities JPA
├── dto/                           # Records de entrada/saída da API
├── exception/                     # Exceções + GlobalExceptionHandler
└── security/                      # JWT filter, JwtService, AuthenticationService

backend/src/main/resources/
├── application.yml                  # Config principal (porta, datasource, jwt, flyway)
└── db/migration/                    # Flyway: V1.0__..., V1.11__...
```

| Camada | Responsabilidade | Exemplo |
|--------|------------------|---------|
| `controller/` | HTTP, validação `@Valid`, status codes | `FuncionarioController` → `/funcionarios` |
| `service/` | Regras, orquestração, importações ADP | `ImportacaoFolhaAdpService`, `FolhaTotalizacaoService` |
| `repository/` | Queries JPA, `@Query` customizadas | `FuncionarioRepository` |
| `model/` | Persistência | `Funcionario`, `FolhaPagamento`, `Rubrica` |
| `dto/` | Contrato da API | `FuncionarioDTO` (record) |
| `security/` | Autenticação stateless JWT | `JwtAuthenticationFilter` |

**Context path:** `server.servlet.context-path: /api` — controllers mapeiam `/funcionarios`, URL final é `/api/funcionarios`.

**Domínios principais:** funcionários, rubricas, folha de pagamento, benefícios, organograma (acesso hierárquico), importação ADP, dashboard.

**Soft delete:** entidades usam flag `ativo`; exclusão lógica via `@Query UPDATE ... SET ativo = false`, não `repository.delete()`.

---

## 4. Testing Philosophy

| Ferramenta | Uso |
|------------|-----|
| JUnit 5 | Framework de testes |
| Mockito (`@ExtendWith(MockitoExtension.class)`) | Mock de repositories e ports |
| spring-boot-starter-test | `@SpringBootTest` em integração (ADP) |
| Testcontainers | PostgreSQL 15 para `ImportacaoFolhaAdpIntegrationTest` |
| JaCoCo | Cobertura na fase `test`; gate via `check-jacoco-thresholds.sh` |
| ArchUnit | `ModularArchitectureTest` — limites modulares AD-010 |

**Contagem atual:** **≥ 474** testes (`mvn test` — 0 falhas; 1 skip quando Docker ausente).

**Estratégia:** testes **unitários isolados** de services com mocks — padrão em `FuncionarioServiceTest` e `AuthenticationServiceTest`.  
Integração com Testcontainers reservada para fluxos críticos (importação ADP).

**Integração ADP:** `ImportacaoFolhaAdpIntegrationTest` usa `@Testcontainers`, `@Transactional` (rollback) e `@EnabledIf("isDockerAvailable")` — **skip automático** quando o daemon Docker não está disponível; a suíte completa permanece verde.

**Não** use `@SpringBootTest` para lógica de negócio simples isolável com mocks.

**Dados de teste:** sem `application-test.yml` para unit tests. Integração ADP usa perfil `test` + container dinâmico.  
**Testes de API manuais:** Postman em `../diversos/postman/` + script `../diversos/scripts/test-api.sh`.

Ao adicionar teste unitário: espelhe o pacote do código (`src/test/java/.../`).

---

## 5. Common Pitfalls

1. **Banco errado / conexão recusada.** Docker expõe Postgres na porta **5433** (não 5432). O `application.yml` pode ter IP de rede interna — troque para `jdbc:postgresql://localhost:5433/sistema_folha` em dev local. Flyway Maven plugin também aponta para `localhost:5433`.

2. **Schema só via Hibernate.** `ddl-auto: update` está ativo, mas **mudanças de schema devem ir em Flyway** (`src/main/resources/db/migration/V1.x__descricao.sql`). Próximo número disponível: verificar pasta e incrementar (atual: V1.11).

3. **Esquecer `@Transactional` em writes.** Importações (`ImportacaoFolhaAdpService`), cadastros e soft deletes falham silenciosamente ou estouram `LazyInitializationException` sem transação no service.

4. **Prefixo `/api` duplicado.** Controllers usam `/funcionarios`, não `/api/funcionarios`. Ao configurar `SecurityConfig`, verifique o padrão existente — o arquivo mistura paths com e sem prefixo `/api`.

5. **Exclusão física vs lógica.** Funcionários e outras entidades são desativados (`ativo=false`). Não chame `deleteById` salvo em código legado explícito.

6. **CPF único entre ativos.** Índice parcial `V1.11` — permitir reutilizar CPF de funcionário inativo, mas bloquear duplicata entre ativos (`existsByCpfAndAtivoTrue`).
