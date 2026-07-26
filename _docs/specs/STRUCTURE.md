# Project Structure

**Root:** `/Volumes/SSD Externo/repo/sistema-folha`

## Directory Tree

```text
sistema-folha/
├── backend/                 # Spring Boot API
│   ├── pom.xml
│   └── src/
│       ├── main/java/.../sistemafolha/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── model/
│       │   ├── dto/
│       │   ├── exception/
│       │   └── security/
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/
│       └── test/java/.../service/
├── frontend/                # React + Vite SPA
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── pages/
│       ├── components/
│       ├── services/
│       ├── contexts/
│       ├── hooks/
│       ├── routes/
│       └── types/
├── diversos/                # Postman, scripts, DB helpers, bcrypt
├── _docs/                   # PRD/TDD/SDD + specs
│   └── specs/               # Brownfield + features (flat layout)
├── .agents/                 # Rules, skills, references
├── docker-compose.yml
├── Dockerfile
└── AGENTS.md
```

## Module Organization

### Backend API

**Purpose:** REST API de folha, cadastros, benefícios, organograma, importações, auth  
**Location:** `backend/src/main/java/br/com/techne/sistemafolha/`  
**Key files:** `SistemaFolhaApplication.java`, `config/SecurityConfig.java`, controllers/services por domínio

### Frontend SPA

**Purpose:** UI autenticada (CRUD, dashboard, organograma, importação)  
**Location:** `frontend/src/`  
**Key files:** `main.tsx`, `routes/index.tsx`, `contexts/AuthContext.tsx`, `services/api.ts`

### Specs & agents

**Purpose:** Governança multiagente e documentação canônica  
**Location:** `_docs/specs/`, `.agents/`  
**Key files:** `STACK.md`…`CONCERNS.md`, `features/`, `AGENTS.md`

### Diversos

**Purpose:** Artefatos operacionais fora do runtime  
**Location:** `diversos/`  
**Key files:** `postman/`, `scripts/test-api.sh`, `db/`, `bcrypt-generator/`

## Where Things Live

**Funcionários / cadastros:**

- UI: `frontend/src/pages/{Funcionarios,Cargos,CentrosCusto,LinhasNegocio,Rubricas}/`
- Business: `backend/.../service/*Service.java`
- Data: `backend/.../model/`, `repository/`, Flyway `db/migration/`

**Folha de pagamento:**

- UI: `frontend/src/pages/FolhaPagamento/`
- Business: `FolhaTotalizacaoService`, `ImportacaoFolhaAdpService`
- Data: `FolhaPagamento`, `ResumoFolhaPagamento`, itens

**Benefícios mensais:**

- UI: `frontend/src/pages/{BeneficiosMensais,TiposBeneficio}/`, `Importacao/`
- Business: `BeneficioMensalService`, `TipoBeneficioService`, `ImportacaoBeneficioMensalService`
- Data: `BeneficioMensal`, `TipoBeneficio` (V1.12/V1.13); legado `Beneficio`/`beneficios` ainda usado em totalização/dashboard

**Organograma / ACL:**

- UI: `frontend/src/pages/Organograma/`, `components/OrganogramaGrafico/`
- Business: `OrganogramaService`, `OrganogramaAcessoService`
- Data: `nos_organograma`, vínculos funcionário/centro

**Auth:**

- UI: `pages/Login/`, `contexts/AuthContext.tsx`, `services/tokenService.ts`
- Business: `security/*`, `AuthController`, `RefreshTokenService`
- Config: `SecurityConfig`, `jwt.*` em `application.yml`

## Current vs TARGET (frontend)

**Obrigação atual (brownfield):** este documento + o layout real em `frontend/src/` (`pages/`, `services/`, `components/`, etc.) e as convenções/testes em `CONVENTIONS.md` / `TESTING.md`.

**TARGET (não obrigatório ainda):** as skills `api-client`, `forms-validation`, `component-architecture`, `routing-perf` e `testing-a11y` descrevem o destino (ex.: `src/features/`). Só viram gate de PR quando o ROADMAP/AD liberar a adequação de código correspondente (feature futura). Até lá: referência de destino, não obrigação.

## Special Directories

**`_old/`:**  
**Purpose:** Snapshot legado de skills/specs — não é fonte canônica  
**Examples:** `_old/.specs/`, `_old/skills/`

**`target/` (raiz):**  
**Purpose:** Artefato Maven residual na raiz — preferir `backend/target/`

**`frontend/dist/`:**  
**Purpose:** Build estático servido pelo Nginx no Docker
