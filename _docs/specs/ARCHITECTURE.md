# Architecture

**Pattern:** Monolito full-stack em camadas (Spring Boot API + React SPA), deployável via Docker Compose

**Analyzed:** 2026-07-25

## High-Level Structure

```text
Browser (Vite/React :3000/5173)
    │  HTTPS/HTTP + Bearer JWT
    ▼
Nginx (prod) / Vite (dev)
    │  REST JSON → VITE_API_URL
    ▼
Spring Boot (:8083/api)
    │  SecurityFilterChain + JwtAuthenticationFilter
    ▼
Services (regras + mapeamento DTO)
    │
    ▼
Spring Data JPA → PostgreSQL 15
    ▲
Flyway migrations (V1.0–V1.13)
```

## Identified Patterns

### Layered REST (Controller → Service → Repository)

**Location:** `backend/src/main/java/br/com/techne/sistemafolha/`  
**Purpose:** Separar HTTP, regras e persistência  
**Implementation:** Controllers finos com `@Valid` + `ResponseEntity`; services com `@Transactional`; repositories JPA  
**Example:** `controller/FuncionarioController.java` → `service/FuncionarioService.java` → `repository/FuncionarioRepository.java`

### Manual DTO mapping (records)

**Location:** `dto/` + métodos `toDTO`/`fromDTO` nos services  
**Purpose:** Contrato API tipado sem MapStruct  
**Implementation:** Java `record` + Jakarta validation + `@Schema` OpenAPI  
**Example:** `dto/FuncionarioDTO.java`, `dto/BeneficioMensalDTO.java`

### Soft delete via flag `ativo`

**Location:** entities + repositories  
**Purpose:** Exclusão lógica sem apagar histórico  
**Implementation:** `UPDATE … SET ativo = false` (`softDelete`); listagens filtram `ativo = true`  
**Example:** `repository/FuncionarioRepository.java`

### JWT + refresh token DB-backed

**Location:** `security/`, `config/SecurityConfig.java`, `model/RefreshToken`  
**Purpose:** Auth stateless com rotação/revogação de refresh  
**Implementation:** Access JWT (claims `roles`); refresh opaco em tabela; cleanup agendado  
**Example:** `security/AuthenticationService.java`, `service/TokenCleanupService.java`

### Hierarchical access (organograma)

**Location:** `service/OrganogramaAcessoService.java`  
**Purpose:** Filtrar dados por centros de custo do nó do usuário e descendentes  
**Implementation:** Regras documentadas no Javadoc (sem funcionário → sem acesso; sem nó → acesso total)  
**Example:** consumo em controllers de folha / `GET /auth/acesso`

### Dual benefit domain (legado + mensal)

**Location:** `model/Beneficio` + `model/BeneficioMensal` / `TipoBeneficio`  
**Purpose:** Migrar benefícios de cadastro contínuo para lançamentos por competência  
**Implementation:** `FolhaTotalizacaoService` prefere `BeneficioMensal` se existir competência; senão fallback em `beneficios`  
**Example:** `service/FolhaTotalizacaoService.java` (linhas ~74–90)

### File-based imports

**Location:** `ImportacaoFolhaAdpService`, `ImportacaoBeneficioMensalService`  
**Purpose:** Carga em lote sem integração online  
**Implementation:** `MultipartFile` → parse texto/CSV ou XLSX (POI) → persistência transacional  
**Example:** `controller/ImportacaoBeneficioMensalController.java`

## Data Flow

### Authentication

1. `POST /api/auth/login` (público) → JWT + refresh  
2. Frontend grava tokens (`TokenService`) + user em `localStorage`  
3. Axios injeta `Authorization: Bearer`  
4. Em 401/403: fila + `POST /auth/refresh`; falha dispara `auth:logout`  
5. `PrivateRoute` redireciona para `/login` se sem usuário

### Folha + totalização com benefícios

1. Importação ADP ou CRUD de `FolhaPagamento`  
2. Consultas agregam linhas por funcionário/competência  
3. `FolhaTotalizacaoService` calcula bruto/líquido/custo e soma benefícios (mensal ou legado)  
4. Resumos em `resumo_folha_pagamento` (migrações V1.2+)

### Benefícios mensais

1. Admin cadastra `TipoBeneficio` (`/tipo-beneficio`, mutações `ADMIN`)  
2. Upload XLSX em `/importacao/beneficios-mensais` ou CRUD `/beneficio-mensal`  
3. UI lista/resumo em `BeneficiosMensais`; tipos em `TiposBeneficio`

### Organograma ACL

1. Usuário logado → `OrganogramaAcessoService` resolve nó e centros acessíveis  
2. Frontend guarda `acessoUsuario` e usa `podeAcessarCentroCusto`  
3. Backend aplica filtros em endpoints sensíveis à hierarquia

## Code Organization

**Approach:** Layer-based no backend; feature-based (pages + services) no frontend

**Structure:**

- Backend: um pacote plano por camada (sem módulos Maven multi-módulo)
- Frontend: `pages/<Feature>/index.tsx` + `services/<feature>Service.ts` + tipos em barrel `types/index.ts`

**Module boundaries:**

- Domínios acoplados via FKs JPA (funcionário ↔ cargo/centro/folha/benefício)
- Sem bounded contexts formais; organograma é o principal cross-cutting de autorização de dados
