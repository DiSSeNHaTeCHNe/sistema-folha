# Sistema de Controle de Folha de Pagamento

Sistema integrado de folha de pagamento para a Techne: cadastros, importação ADP, processamento mensal, benefícios, organograma com ACL hierárquico, dashboard e relatórios executivos PDF.

**Documentação canônica (spec-driven):** `_docs/specs/` — `PROJECT.md`, `ROADMAP.md`, `STATE.md`, `TESTING.md`.

## Stack

| Camada | Tecnologias |
|--------|-------------|
| **Backend** | Java 17, Spring Boot 3.2, Spring Security (JWT + API Keys), JPA, PostgreSQL, Flyway, OpenPDF, JaCoCo |
| **Frontend** | React 19, TypeScript, Material-UI v7, Vite 6, Vitest, MSW, Playwright (smoke) |
| **Infra local** | Docker Compose (PostgreSQL `:5433`), scripts em `diversos/scripts/` |

## Funcionalidades principais

- **Autenticação:** login JWT, refresh token, API Keys (`sf_live_`) para integrações/MCP
- **Cadastros:** funcionários, cargos, centros de custo, linhas de negócio, rubricas, rubricas fixas
- **Folha:** importação ADP, processamento mensal, fichas, totalização bruto/líquido/custo CLT
- **Benefícios mensais:** resumo por competência, importação, tipos de benefício
- **Organograma:** modo lista + gráfico (ReactFlow), ACL por centro de custo
- **Dashboard:** KPIs agregados com Recharts
- **Relatórios:** PDF executivo de folha e custo de benefícios (geração async, download)
- **Importação:** folha ADP e benefícios mensais via upload

## Estrutura do projeto

```text
sistema-folha/
├── frontend/                 # SPA React (rotas em src/routes/)
├── backend/                  # API Spring Boot — monólito modular por domínio
│   └── src/main/java/.../
│       ├── auth/             # JWT, usuários, API keys
│       ├── cadastros/        # Mestres
│       ├── folha/            # Processamento e motor CLT
│       ├── beneficios/       # Benefícios mensais
│       ├── organograma/      # Árvore + ACL
│       ├── relatorios/       # PDF async
│       ├── importacao/       # ADP
│       └── dashboard/        # Stats agregados
├── diversos/                 # Scripts, Postman, OpenAPI export
├── _docs/specs/              # Specs TLC, roadmap, estado do projeto
├── docker-compose.yml
└── Dockerfile
```

## Como executar

### Pré-requisitos

- JDK 17, Maven 3.8+, Node.js 18+, Docker (opcional, para Postgres e Testcontainers)

### Banco (Docker)

```bash
docker compose up -d
# Postgres em localhost:5433 — user/pass postgres, DB sistema_folha
```

Ajuste `backend/src/main/resources/application.yml` se necessário (`spring.datasource.url`).

### Backend

```bash
cd backend
mvn flyway:migrate
mvn spring-boot:run
# API: http://localhost:8083/api
# Swagger: http://localhost:8083/api/swagger-ui.html
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# SPA: http://localhost:5173 (porta padrão Vite)
```

## Testes e qualidade

| Gate | Comando |
|------|---------|
| Backend | `cd backend && mvn test` |
| Frontend | `cd frontend && npm test` |
| Cobertura ≥95% (canônico) | `bash diversos/scripts/check-coverage-95.sh` |
| E2E smoke (login) | `cd frontend && npm run test:e2e` |
| Sonar local | `bash diversos/scripts/sonar-analyze.sh` (requer `.sonar.env`) |

Detalhes: `_docs/specs/TESTING.md`.

### Postman

```text
diversos/postman/sistema-folha.postman_collection.json
diversos/postman/sistema-folha.postman_environment.json
./diversos/scripts/test-api.sh
```

## Próximos passos (roadmap)

Ver `_docs/specs/ROADMAP.md`. Destaques:

- **M2 em curso:** refinamento importação ADP, integridade CPF
- **Próxima feature UX:** temas visuais selecionáveis (`_docs/specs/features/temas-visuais/`)
- **M3:** CI/CD remoto, observabilidade, strategy pattern de pagamentos

## Changelog (recente)

### 2026-08 — Relatórios executivos + API Keys

- PDF async folha/benefício (OpenPDF, AD-015)
- API Keys PAT para integrações
- Gate cobertura 95% BE+FE (AD-014)

### 2025-10 — Organograma gráfico

- Modo ReactFlow com zoom/pan; toggle lista/gráfico
