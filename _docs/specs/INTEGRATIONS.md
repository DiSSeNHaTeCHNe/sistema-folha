# External Integrations

**Analyzed:** 2026-07-25

Não há integrações online com terceiros (sem SDKs de cloud, e-mail, filas ou HTTP clients de parceiros). O sistema integra-se a **arquivos**, **PostgreSQL** e **deploy Docker**.

## Database

**Service:** PostgreSQL 15 (`postgres:15-alpine` no Compose)  
**Purpose:** Persistência principal (folha, cadastros, auth, organograma, benefícios)  
**Implementation:** Spring Data JPA + Flyway (`backend/src/main/resources/db/migration/`)  
**Configuration:** `spring.datasource.*` em `application.yml`; Compose sobrescreve URL para `jdbc:postgresql://postgres:5432/sistema_folha`  
**Authentication:** user/password `postgres` (dev); porta host `5433`

## File Import — Folha ADP

**Service:** Layout de arquivo texto/CSV estilo ADP (não API ADP)  
**Purpose:** Carga de lançamentos de folha por competência  
**Implementation:** `ImportacaoFolhaAdpService` + `ImportacaoFolhaAdpController` (`/importacao/...`)  
**Configuration:** Mapeamentos de empresa/rubricas embutidos no service  
**Authentication:** Endpoint autenticado (JWT)

## File Import — Benefícios mensais (XLSX)

**Service:** Planilha Excel via Apache POI  
**Purpose:** Lançamentos `BeneficioMensal` por competência e tipo  
**Implementation:** `ImportacaoBeneficioMensalService` + `ImportacaoBeneficioMensalController` (`POST /importacao/beneficios-mensais`)  
**Configuration:** Multipart até 50MB (`application.yml`)  
**Authentication:** JWT

## API Surface (first-party)

### Security (Spring Security + JWT)

**Purpose:** Autenticação stateless e autorização por rota  
**Location:** `backend/src/main/java/br/com/techne/sistemafolha/config/SecurityConfig.java`, `security/*`  
**Authentication:** Bearer JWT no header `Authorization`; login/refresh públicos; demais rotas autenticadas  
**CSRF:** **Desabilitado** (`csrf.disable()`). A API é stateless — tokens via header Bearer, sem cookie de sessão. CSRF protege sessões baseadas em cookie; não se aplica a este modelo. Reavaliar se auth migrar para cookie HttpOnly.  
**Matchers:** Paths relativos ao `context-path` (`/api`); controllers mapeiam sem prefixo `/api` (ex.: `/beneficio-mensal`, `/tipo-beneficio`, `/importacao`). Rotas não listadas explicitamente caem em `anyRequest().authenticated()`.  
**Roles:** Mutações em `tipo-beneficio` e `funcionario-rubrica-fixa` exigem `ADMIN`; `POST /folha-pagamento/processar` exige `ADMIN`.  
**Audit (2026-07-29):** Matchers obsoletos (`/api/beneficios/**`) ausentes; alinhados com controllers modulares atuais.

### Auth API

**Purpose:** Login, refresh, logout, acesso organograma  
**Location:** `AuthController`, `security/*`  
**Authentication:** Login público; demais fluxos com token/refresh  
**Key endpoints:** `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/acesso`

### Domain REST

**Purpose:** CRUD e consultas dos domínios do produto  
**Location:** `controller/*`  
**Authentication:** `anyRequest().authenticated()`; mutações `tipo-beneficio` exigem `ADMIN`  
**Key resources:** `/funcionarios`, `/folha-pagamento`, `/beneficio-mensal`, `/tipo-beneficio`, `/organograma`, `/dashboard`, cadastros

### OpenAPI / Swagger

**Purpose:** Documentação interativa da API  
**Location:** springdoc (`/swagger-ui.html`, `/api-docs`)  
**Authentication:** Matchers públicos em `SecurityConfig`

## Webhooks

Nenhum handler de webhook encontrado.

## Background Jobs

**Queue system:** Nenhum (sem Rabbit/Kafka/SQS)  
**Location:** `TokenCleanupService` com `@Scheduled`  
**Jobs:** Purge periódico de refresh tokens expirados (a cada 6h)

## Frontend ↔ Backend

**Service:** Axios client  
**Purpose:** Único consumidor da API em runtime de produto  
**Implementation:** `frontend/src/services/api.ts` (`VITE_API_URL` ou `http://localhost:8083/api`)  
**Authentication:** Bearer access token + refresh queue

## Deploy packaging

**Service:** Docker multi-stage + Compose  
**Purpose:** Empacotar SPA (Nginx) + JAR + Postgres  
**Implementation:** `Dockerfile` (Node 22 build FE, Maven Temurin 21 build BE, runtime Temurin 21 / Nginx)  
**Configuration:** `docker-compose.yml` — serviços `api`, `frontend`, `postgres`
