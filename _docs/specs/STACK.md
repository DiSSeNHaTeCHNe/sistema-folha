# Tech Stack

**Analyzed:** 2026-07-25

## Core

- Framework: Spring Boot 3.2.3 (backend), React 19.1 + Vite 6.3 (frontend)
- Language: Java 17 (`pom.xml`), TypeScript ~5.8 (frontend)
- Runtime: JVM (embedded Tomcat, port 8083); Node 22 (Docker build) / Node 18+ local
- Package manager: Maven (backend, sem wrapper), npm (frontend)

## Frontend

- UI Framework: React 19.1, Material-UI (MUI) v7.2 + `@mui/icons-material`, `@mui/x-date-pickers` 8.5
- Styling: Emotion + MUI `sx` / `ThemeProvider` (`frontend/src/main.tsx`, `theme.ts`)
- State Management: React Context (`AuthContext`) + `useState`/`useEffect` por página
- Form Handling: React Hook Form 7.59 (+ `Controller` em formulários complexos)
- Routing: React Router DOM 7.6
- HTTP: Axios 1.10 com interceptors JWT refresh (`frontend/src/services/api.ts`)
- Charts / graphs: Recharts 3.0 (Dashboard), ReactFlow 11.11 (Organograma)
- Dates: date-fns 4.1
- DnD: `@dnd-kit/*` (Organograma)
- Toasts: react-toastify 11
- Money input: react-number-format 5
- Note: `@tanstack/react-query` 5.80 está no `package.json` mas **não é usado** em `src/`

## Backend

- API Style: REST JSON via Spring Web MVC (`@RestController`)
- Context path: `/api` (`application.yml`)
- Database: Spring Data JPA + Hibernate, PostgreSQL 15 (Docker), Flyway
- Authentication: Spring Security (stateless) + JWT jjwt 0.12.5 + refresh tokens em DB
- Validation: Jakarta Bean Validation
- API docs: springdoc-openapi 2.3.0 (Swagger UI `/swagger-ui.html`)
- Boilerplate: Lombok 1.18.30
- File parse: Apache POI 5.2.5 (importação XLSX de benefícios mensais)
- Scheduling: `@EnableScheduling` — `TokenCleanupService` (purge refresh tokens)

## Testing

- Unit (backend): JUnit 5 + Mockito (`spring-boot-starter-test`)
- Security test dep: `spring-security-test` (presente, pouco usado)
- Integration: sem suite `@SpringBootTest` / Testcontainers
- API manual: Postman (`diversos/postman/`), scripts (`diversos/scripts/`)
- Frontend unit/E2E: nenhum runner configurado (só `lint`)

## External Services

- Database: PostgreSQL (Docker porta host 5433; `application.yml` default aponta IP LAN)
- File import: layout ADP texto/CSV (folha); XLSX benefícios mensais — ambos in-process
- Sem clientes HTTP externos, S3, e-mail ou filas

## Development Tools

- Lint (frontend): ESLint 9 + typescript-eslint + Prettier
- Containerization: Dockerfile multi-stage + `docker-compose.yml` (api, frontend, postgres)
- DB migrations: Flyway `V1.0` … `V1.13` em `backend/src/main/resources/db/migration/`
- Utility: `diversos/bcrypt-generator/`
- Agent/spec: `.agents/`, `_docs/specs/`
