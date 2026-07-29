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

**Totalização bruto / líquido / custo (CLT)** — IN PROGRESS

- Spec TLC: `_docs/specs/features/folha-custo-clt/` (Execute Done; Verifier PASS)
- Fix1 Done: `_docs/specs/features/folha-custo-clt-fix1/` (processamento pós-import ADP)
- **Fix2 (Execute Done):** `_docs/specs/features/folha-custo-clt-fix2/` — Custo Techne por `porcentagem`; remove rateio ADP; paridade card↔aba/resumo
- **Fix3 (Specify draft):** `_docs/specs/features/folha-custo-clt-fix3/` — Fixa global (funcionário opcional); UX Rubricas Fixas + detalhe Bruto/Líquido/Custo padronizado

**Benefícios mensais** — PLANNED

- Tela `BeneficiosMensais` (stub frontend criado)
- Visão consolidada por competência alinhada ao fluxo de benefícios

**Integridade de cadastro** — IN PROGRESS

- Unicidade de CPF para funcionários ativos (migration `V1.11__funcionario_cpf_unique_ativo.sql`)
- Testes em `FuncionarioServiceTest`

**Controle de acesso hierárquico** — PLANNED

- Evolução de permissões por escopo organizacional (centro de custo / linha de negócio)
- Referência: `_docs/CONTROLE_ACESSO_HIERARQUICO.md`

### Harness / agents

**Ajuste do harness (P1)** — COMPLETE

- Versionar núcleo (`AGENTS.md`, `.agents/`, `_docs/specs/`, ponteiros Cursor/Claude)
- TLC paths → `_docs/specs/`; skills FE marcadas TARGET; Full harness gate PASS (commits do usuário pendentes)

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

**Qualidade ampliada** — PLANNED

- Testes unitários frontend
- Cobertura de integração para fluxos críticos de importação e totalização
- Gate de QA alinhado a `_docs/specs/TESTING.md` (após `map codebase`)

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
