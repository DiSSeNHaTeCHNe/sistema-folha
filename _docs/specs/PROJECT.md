# Sistema de Folha de Pagamento (Techne)

**Vision:** Plataforma web integrada para cadastro, processamento, importação e análise de folha de pagamento, benefícios e estrutura organizacional, com controle de acesso e relatórios operacionais.
**For:** Equipes de RH, financeiro e gestores da Techne Engenharia e Sistemas que precisam operar e auditar dados de pessoal e folha.
**Solves:** Dispersão de dados entre planilhas e sistemas legados (ex.: ADP), falta de visibilidade consolidada por funcionário/centro de custo e dificuldade de manter cadastros mestres (funcionários, rubricas, organograma) alinhados ao processamento mensal.

## Goals

- Manter cadastros mestres e lançamentos de folha/benefícios consistentes, com soft delete, validações e trilha de auditoria básica via API.
- Reduzir retrabalho na competência mensal com importação ADP, totalização por funcionário (bruto, líquido, custo) e telas operacionais unificadas.
- Entregar visão gerencial via dashboard, organograma interativo e relatórios PDF, com autenticação JWT e controle de permissões.
- Evoluir para arquitetura sustentável: testes automatizados, CI/CD, Docker e padrões claros para novas integrações de pagamento.

## Tech Stack

**Core:**

- Backend: Java 17, Spring Boot 3.2.3, Spring Security, Spring Data JPA
- Frontend: React 19, TypeScript, Vite 6, Material-UI v7
- Database: PostgreSQL, Flyway migrations
- Auth: JWT (jjwt 0.12.x) + refresh token
- API docs: springdoc-openapi / Swagger UI

**Key dependencies:**

- Axios + TanStack React Query (frontend data layer)
- ReactFlow (organograma em modo gráfico)
- Recharts (dashboard)
- Lombok, Bean Validation, Postman collections (QA manual)

Detalhamento brownfield: `_docs/specs/STACK.md` (gerado via `map codebase`).

## Scope

**Incluído (produto atual e evolução próxima):**

- Autenticação, usuários e permissões
- CRUD de funcionários, cargos, centros de custo, linhas de negócio, rubricas
- Folha de pagamento, benefícios, resumos e relatórios PDF
- Organograma (lista + visualização gráfica)
- Importação de folha/benefícios (ADP e fluxos relacionados)
- Dashboard operacional
- Totalização de folha por funcionário (bruto, líquido, custo)
- Benefícios mensais e regras de unicidade (ex.: CPF ativo)

**Fora de escopo (nesta fase):**

- Motor completo de cálculo legal de folha (INSS, IRRF, férias, rescisão) substituindo ADP/Janus
- Portal do colaborador (holerite self-service)
- Multi-tenant / multi-empresa
- Notificações em tempo real e histórico de alterações field-level
- Backup automatizado e observabilidade de produção (monitoring/APM)

## Constraints

- **Técnico:** Monorepo Spring Boot + SPA React; API REST JSON; PostgreSQL como fonte única de verdade operacional.
- **Integração:** Compatibilidade com layouts e objetos de importação ADP já documentados em `_docs/` (legado operacional).
- **Governança:** Specs e planejamento em `_docs/specs/`; backlog operacional no Linear com key única por produto.
- **Recursos:** Evolução incremental sobre base brownfield existente — priorizar consolidação e qualidade antes de features greenfield amplas.
