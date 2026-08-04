# Roadmap

**Current Milestone:** M2 — Consolidação operacional da folha
**Status:** In Progress

---

## Milestone 1 — Plataforma core

**Goal:** Operação básica de folha e cadastros com autenticação, CRUDs, relatórios e organograma utilizável.
**Target:** Concluído (baseline v1.x em produção interna / homologação)

### Features

**Autenticação e usuários** — COMPLETE

- Login JWT, refresh token, proteção de rotas
- CRUD de usuários com soft delete e permissões

**Cadastros mestres** — COMPLETE

- Funcionários, cargos, centros de custo, linhas de negócio, rubricas
- Validações e soft delete em entidades principais

**Folha e benefícios** — COMPLETE

- CRUD de lançamentos de folha e benefícios
- Resumo de folha e relatórios PDF (folha e benefícios)

**Organograma** — COMPLETE

- Modo lista tradicional
- Modo gráfico (ReactFlow) com zoom/pan e edição preservada

**Dashboard e UX** — COMPLETE

- Dashboard com indicadores (Recharts)
- Interface Material-UI responsiva e componentes reutilizáveis

**Qualidade base** — COMPLETE

- Testes backend (unitários/integração), coleção Postman, Swagger

---

## Milestone 2 — Consolidação operacional da folha

**Goal:** Fechar o ciclo mensal com importação confiável, totalização por funcionário e telas operacionais que reduzam planilhas paralelas.
**Target:** Competência mensal operável com totais auditáveis e importação ADP estável

### Features

**Importação ADP / folha** — IN PROGRESS

- Importação de folha e benefícios via controllers dedicados
- Documentação e objetos de importação em `_docs/IMPORTACAO_*.md`
- Ajustes em `ImportacaoFolhaAdpService` e repositórios relacionados

**Totalização bruto / líquido / custo (CLT)** — COMPLETE

- Spec TLC: `_docs/specs/features/folha-custo-clt/` (Verifier PASS)
- Fix1–Fix3 Done: processamento pós-import, Custo Techne por `porcentagem`, rubrica fixa global

**Benefícios mensais** — COMPLETE

- Tela `BeneficiosMensais` operacional (resumo por competência + drill-down)
- Backend `beneficio_mensal` + importação; legado `Beneficio` removido (modular-monolith)

**Relatórios executivos PDF** — COMPLETE

- Domínio `relatorios.*` async (OpenPDF); hub FE `/relatorios` com polling/download (AD-015)

**Integridade de cadastro** — IN PROGRESS

- Unicidade de CPF para funcionários ativos (migration `V1.11__funcionario_cpf_unique_ativo.sql`)
- Testes em `FuncionarioServiceTest`

**Controle de acesso hierárquico** — COMPLETE (MVP)

- `OrganogramaAcessoService` + permissão `ACESSO_TOTAL` (AD-011); enforcement em folha, benefícios, dashboard, relatórios
- Evoluções futuras: admin UI granular, escopos finos — ver `_docs/CONTROLE_ACESSO_HIERARQUICO.md`

### Harness / agents

**Ajuste do harness (P1)** — COMPLETE

- Núcleo versionado (`AGENTS.md`, `.agents/`, `_docs/specs/`, ponteiros Cursor/Claude)
- TLC paths → `_docs/specs/`; modular monolith + ACL entregues em `main`

**API Keys (PAT integrações)** — COMPLETE

- AD-013: Bearer `sf_live_`, UI `/api-keys`, permissão `API_KEY`

**Temas visuais selecionáveis** — PLANNED (spec pronta)

- Feature `_docs/specs/features/temas-visuais/` — aguardando aprovação Execute

**Adequação código ↔ skills FE target** — DEFERRED

- Feature futura: alinhar frontend/código às skills TARGET (`api-client`, forms, component-architecture, routing-perf, testing-a11y)
- Não misturar com o P1 de harness acima

---

## Milestone 3 — Modernização e integrações

**Goal:** Preparar o sistema para novos meios de pagamento, maior cobertura de testes e deploy repetível.
**Target:** Pipeline CI/CD, Docker estável e módulo de pagamentos extensível

### Features

**Strategy pattern de pagamentos** — PLANNED

- Abstração para boletos, PIX e demais meios
- Referência: `_docs/STRATEGY_PATTERN_PAGAMENTOS.md`, `_docs/backlog-modernizacao-pagamentos.md`

**DevOps e ambientes** — PLANNED

- Docker / docker-compose refinados (Dockerfile e compose já presentes)
- Ambientes dev, staging e prod
- CI/CD automatizado

**Qualidade ampliada** — IN PROGRESS

- Testes unitários frontend (Vitest 436+ casos, gate AD-014 ≥95%)
- Gate canônico: `diversos/scripts/check-coverage-95.sh`
- Pendente: CI/CD remoto, E2E além de login smoke

**Performance e observabilidade** — PLANNED

- Otimização de queries e lazy loading onde necessário
- Logging estruturado e monitoramento básico

---

## Future Considerations

- Estudo de cálculo de ficha salarial / paridade com Janus (`_docs/ESTUDO_CALCULO_FICHA_SALARIAL_JANUS.md`)
- Transição estágio → efetivo automatizada
- Histórico de alterações por entidade
- Relatórios personalizados e exportações adicionais
- Cache e compressão para grandes volumes de competência
- Portal do colaborador e multi-empresa
