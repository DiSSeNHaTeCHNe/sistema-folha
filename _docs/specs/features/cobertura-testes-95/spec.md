# Cobertura de Testes 95% (Backend + Frontend) Specification

**Related:** `_docs/specs/TESTING.md`, `_docs/specs/CONCERNS.md`, `sonar-project.properties`, `diversos/scripts/check-jacoco-thresholds.sh`, série `adequacao-analise-projeto-r2/r3/r4` (convenção anterior: 75% linha JaCoCo, 80–85% Sonar `new_coverage` leak period)
**Complexity:** Complex
**Spec status:** Draft — Specify em andamento (2026-08-01)

## Problem Statement

O projeto hoje mira 75% de linha no backend (JaCoCo global) e 80–85% de `new_coverage` no leak period do Sonar — nunca uma meta de cobertura global e por branch. O usuário pediu explicitamente adequar **backend e frontend** a **95% de cobertura, tanto de linha quanto de branch**.

**Descoberta na fase Design (2026-08-01):** ~1.354 dos 1.734 branches faltando no backend eram `equals`/`hashCode`/getters **gerados pelo Lombok `@Data`** em 68 entidades — código não escrito à mão. Decisão AD-014 (Design): adicionar `backend/lombok.config` com `lombok.addLombokGeneratedAnnotation = true`, fazendo o JaCoCo/Sonar ignorarem métodos `@lombok.Generated`. A meta de 95% recai sobre **código escrito à mão** (services, controllers, regras de domínio). Isso reinterpreta A2 (ver Assumptions).

**Baseline real medida em 2026-08-01** (backend já com `lombok.config`):

| Stack    | Métrica    | Atual (real) | Meta | Gap     |
| -------- | ---------- | ------------ | ---- | ------- |
| Backend  | Linha      | 82.48%       | 95%  | ~12.5pp |
| Backend  | Branch     | 68.70%       | 95%  | ~26pp   |
| Frontend | Linha      | 68.06%       | 95%  | ~27pp   |
| Frontend | Branch     | 48.33%       | 95%  | ~47pp   |

Os gaps reais do backend concentram-se em **services (`*.application`)** e **controllers (`*.api`)** — lógica de negócio genuína (ex.: `folha/application` 99 branches, `beneficios/application` 70, `cadastros/application` 50, `importacao/application` 44; controllers como `cadastros/api` 82 linhas e `organograma/api` 64 linhas quase sem teste). No frontend, os gaps são lógica de página real (`pages/Funcionarios` 5.37% branch, `pages/Dashboard` 23.33% branch etc.) — não há código gerado análogo.

## Goals

- [ ] Backend: JaCoCo global — linha ≥ 95% E branch ≥ 95%, sem exclusões de pacote
- [ ] Frontend: Vitest/v8 — statements/linhas ≥ 95% E branch ≥ 95%, sem exclusões de arquivo
- [ ] Gate automatizado (script) que falha o build se qualquer uma das 4 métricas cair abaixo de 95% após esta feature
- [ ] Zero teste "fake" — nenhum teste desabilitado, `@Disabled`, `it.skip`, ou assert vazio só para inflar número
- [ ] `TESTING.md` atualizado com a nova baseline e os novos thresholds (substituindo os 75%/80–85% antigos)

## Out of Scope

| Feature                                                              | Reason                                                                                   |
| --------------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| Reduzir complexidade ciclomática dos services (refactor de `ImportacaoFolhaAdpService` etc.) | Testar o código existente, não redesenhá-lo — CC alta só torna a Fase 3 mais cara, não bloqueia |
| Mudar o Quality Gate do Sonar (`new_coverage` floor 80%)              | Sonar mede leak period; esta feature mede cobertura global — são metas paralelas, não substitutas |
| Novos endpoints/telas ou mudança de comportamento funcional            | Feature é puramente de teste; nenhum código de produção muda de comportamento             |
| Pipeline CI remoto (GitHub Actions) rodando o gate automaticamente     | ROADMAP M3 DevOps; esta feature entrega o script, não a integração CI                     |
| Testes E2E (Playwright) adicionais                                     | Fora do escopo de cobertura unit/integration medida por JaCoCo/Vitest                     |

---

## Assumptions & Open Questions

| Assumption / decision                                   | Chosen default                                                                                          | Rationale                                                                                                     | Confirmed? |
| --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- | ---------- |
| A1 — Escopo de stacks e métricas                         | Backend + Frontend, linha E branch, ambos a 95%                                                          | Confirmado pelo usuário                                                                                            | **y**      |
| A2 — Exclusões (revisado no Design)                       | **Código escrito à mão** conta a 95%. Métodos gerados pelo Lombok (`@lombok.Generated`) são ignorados via `backend/lombok.config` (AD-014). Nenhuma exclusão de classe/pacote de negócio. `*Application.java`, `main.tsx`, `theme.ts` continuam contando. | Usuário confirmou "sem exclusões" (A2 original); no Design ficou provado que ~1.354 branches eram Lombok gerado (não código). Usuário escolheu "Excluir código gerado" — gerado ≠ escrito. | **y** (revisado 2026-08-01) |
| A3 — Estrutura da feature                                | Uma feature Complex única com fases internas (não série r5/r6)                                            | Confirmado pelo usuário                                                                                            | **y**      |
| A4 — Nome da feature                                     | `cobertura-testes-95` (não continua a série `adequacao-analise-projeto-rN`)                               | Meta e mecanismo são diferentes da série (que mirava Sonar leak period, não cobertura global); nome próprio evita confusão de convenção | n (agente) |
| A5 — Branch de trabalho                                  | `feat/cobertura-testes-95` a partir de `main`                                                              | Convenção do projeto (branch por feature)                                                                          | n (agente) |
| A6 — Gate automatizado                                   | Script novo `diversos/scripts/check-coverage-95.sh` (linha + branch, ambas stacks) substituindo o antigo `check-jacoco-thresholds.sh` (75%) | Consolida os dois checks (BE+FE) e os dois thresholds antigos em um único gate coerente com a nova meta          | n (agente) |
| A7 — Testes triviais em DTOs/boilerplate                 | Aceitos quando necessários para fechar branch coverage (ex.: `equals`/`hashCode`/builders gerados)         | Decorre diretamente de A2 (sem exclusões) — se DTOs contam, precisam de teste                                     | n (agente) |
| A8 — Testcontainers/Docker-gated tests na métrica         | `ImportacaoFolhaAdpIntegrationTest` (Docker-gated) permanece opcional; sua cobertura NÃO é exigida quando Docker está indisponível — medição de baseline considera o cenário sem Docker | Consistente com o padrão já documentado em `TESTING.md`/R4 (`@EnabledIf`); exigir Docker obrigatório mudaria infraestrutura de CI, fora de escopo | n (agente) |

**Open questions:** none — todas resolvidas acima (A1–A3 pelo usuário; A4–A8 assumidas pelo agente com racional registrado).

### Implicit-requirement dimensions (Complex — cobertura completa)

| Dimension                                 | Resolution                                                                                                                   |
| ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| Input validation & bounds                  | N/A because esta feature não adiciona nem altera validação de input — apenas testa a validação já existente                    |
| Failure / partial-failure states           | COV-14: gate deve falhar de forma clara (mensagem + métrica abaixo do threshold) quando qualquer uma das 4 métricas ficar < 95% |
| Idempotency / retry / duplicate handling   | N/A because nenhuma operação nova de escrita é introduzida                                                                     |
| Auth boundaries & rate limits              | N/A because nenhum endpoint novo ou mudança de permissão é introduzida                                                         |
| Concurrency / ordering                     | N/A because testes seguem o modelo de paralelismo já documentado em `TESTING.md` (Mockito sem estado compartilhado, MSW isolado por arquivo) — nenhuma mudança de concorrência |
| Data lifecycle / expiry                    | N/A because nenhuma entidade ou TTL novo é introduzido                                                                          |
| Observability                              | COV-15: script de gate deve logar as 4 métricas (BE linha/branch, FE linha/branch) de forma legível para uso em CI futuro       |
| External-dependency failure                | Ver A8 — cobertura do path Docker-gated (Testcontainers) é N/A quando Docker está indisponível, documentado como tal no relatório de gate |
| State-transition integrity                 | N/A because nenhuma máquina de estados nova é introduzida                                                                       |

---

## User Stories

### P1: Backend — cobertura de linha a 95% ⭐ MVP

**User Story**: Como Tech Lead, quero que o backend atinja 95% de cobertura de linha (JaCoCo), para que a maior parte da lógica de negócio tenha ao menos um caminho feliz e os principais caminhos de erro exercitados por teste automatizado.

**Why P1**: É o menor gap (82.77% → 95%, ~12pp) — entrega valor rápido e valida a mecânica do gate antes de atacar branch coverage (o gap maior).

**Acceptance Criteria**:

1. WHEN `mvn test` roda na branch da feature THEN o relatório `target/site/jacoco/jacoco.xml` SHALL reportar `LINE` counter com `covered/(covered+missed) ≥ 0.95`
2. WHEN uma classe de produção em `backend/src/main/java` não tem nenhuma linha coberta THEN a Fase 1 SHALL adicionar teste unitário cobrindo seu caminho principal antes de prosseguir para as próximas fases
3. WHEN os novos testes são adicionados THEN nenhum teste existente (baseline: 474 casos) SHALL ser removido ou enfraquecido para atingir a meta

**Independent Test**: Rodar `cd backend && mvn test` e inspecionar `target/site/jacoco/jacoco.xml` — contador `LINE` ≥ 95%.

---

### P1: Frontend — cobertura de linha/statements a 95%

**User Story**: Como Tech Lead, quero que o frontend atinja 95% de cobertura de linha/statements (Vitest v8), para que páginas e serviços tenham seus fluxos principais e de erro exercitados por teste automatizado.

**Why P1**: Gap intermediário (68.06% → 95%, ~27pp); paralelo ao P1 backend — ambos formam a base "linha" antes de branch.

**Acceptance Criteria**:

1. WHEN `npm run test:coverage` roda na branch da feature THEN o resumo SHALL reportar `Lines ≥ 95%` e `Statements ≥ 95%` em "All files"
2. WHEN uma página ou service em `frontend/src` está abaixo de 95% de linha (ex.: `pages/Funcionarios` 35%, `pages/Organograma` 42.56%, `pages/Relatorios` 37.73%, `pages/Importacao` 35.67%, `pages/Usuarios` 44.66%, `pages/Dashboard` 68.18%, `pages/ApiKeys` 42.22%) THEN a Fase 2 SHALL adicionar/expandir testes até fechar o gap
3. WHEN os novos testes são adicionados THEN nenhum teste existente (baseline: 184 casos) SHALL ser removido ou enfraquecido para atingir a meta

**Independent Test**: Rodar `cd frontend && npm run test:coverage` e inspecionar o resumo — `Lines` e `Statements` ≥ 95% em "All files".

---

### P2: Backend — cobertura de branch a 95%

**User Story**: Como Tech Lead, quero que o backend atinja 95% de cobertura de branch, para que as combinações de condicionais (if/else, guards de ACL, validações de importação) sejam exercitadas, não só a linha "feliz".

**Why P2**: Maior gap do projeto inteiro (35.30% → 95%, ~60pp) — depende de P1 (linha) estar perto de fechado para não retrabalhar os mesmos métodos duas vezes.

**Acceptance Criteria**:

1. WHEN `mvn test` roda na branch da feature THEN `target/site/jacoco/jacoco.xml` SHALL reportar `BRANCH` counter com `covered/(covered+missed) ≥ 0.95`
2. WHEN um método com múltiplos branches (ex.: `OrganogramaAcessoService`, `ImportacaoFolhaAdpService`) tem branches não cobertos THEN a Fase 3 SHALL adicionar casos de teste parametrizados/dedicados cobrindo cada ramo (true/false, cada `case`, cada cláusula de exceção)
3. WHEN um branch é inatingível por design (ex.: guard defensivo que a linguagem/framework nunca aciona) THEN ele SHALL ser documentado como tal em `validation.md` com justificativa — não contornado silenciosamente

**Independent Test**: Rodar `cd backend && mvn test` e inspecionar `target/site/jacoco/jacoco.xml` — contador `BRANCH` ≥ 95%.

---

### P2: Frontend — cobertura de branch a 95%

**User Story**: Como Tech Lead, quero que o frontend atinja 95% de cobertura de branch, para que condicionais de UI (estados de erro, permissões, formulários) sejam exercitados por teste, não só o caminho feliz.

**Why P2**: Segundo maior gap (48.33% → 95%, ~47pp); paralelo ao P2 backend.

**Acceptance Criteria**:

1. WHEN `npm run test:coverage` roda na branch da feature THEN o resumo SHALL reportar `Branches ≥ 95%` em "All files"
2. WHEN uma página com branch coverage baixo (ex.: `pages/Funcionarios` 5.37%, `pages/Dashboard` 23.33%, `pages/Organograma` 32.23%) tem condicionais não exercitadas THEN a Fase 4 SHALL adicionar casos cobrindo cada branch (estados de loading/erro/vazio, permissões condicionais, validação de formulário)
3. WHEN os testes de branch são escritos THEN SHALL seguir o padrão já estabelecido (`Testing Library` por role/label, MSW por arquivo) — sem introduzir novo padrão de mock

**Independent Test**: Rodar `cd frontend && npm run test:coverage` e inspecionar o resumo — `Branches` ≥ 95% em "All files".

---

### P3: Gate automatizado de 95%

**User Story**: Como Tech Lead, quero um script único que valide as 4 métricas (BE linha/branch, FE linha/branch) e falhe com mensagem clara se qualquer uma cair abaixo de 95%, para que regressões futuras sejam detectadas antes do merge.

**Why P3**: Consolida o resultado das fases anteriores em um gate reproduzível — sem isso, a meta de 95% erode no próximo PR (mesmo risco que a série `adequacao-analise-projeto-rN` já documentou para o Sonar).

**Acceptance Criteria**:

1. WHEN `diversos/scripts/check-coverage-95.sh` roda após `mvn test` (backend) e `npm run test:coverage` (frontend) THEN SHALL ler `jacoco.xml` (LINE, BRANCH) e o resumo do Vitest (Lines, Branches) e comparar cada um a 95%
2. WHEN qualquer uma das 4 métricas está abaixo de 95% THEN o script SHALL sair com código de erro != 0 e imprimir qual(is) métrica(s) falhou(aram) e o valor medido
3. WHEN todas as 4 métricas estão ≥ 95% THEN o script SHALL sair com código 0 e imprimir um resumo das 4 métricas

**Independent Test**: Rodar `bash diversos/scripts/check-coverage-95.sh` após os relatórios gerados — exit code e mensagem condizem com os valores reais.

---

## Edge Cases

- WHEN uma classe é gerada/boilerplate pura (DTO só com getters/setters, `record`) THEN ainda assim precisa de teste (equals/hashCode/builder) para contar linha/branch, dado A2 (sem exclusões)
- WHEN `ImportacaoFolhaAdpIntegrationTest` (Docker-gated) está skipada por falta de Docker THEN sua cobertura NÃO conta contra o denominador da meta nesta rodada de medição (A8) — documentar no `validation.md` quando isso ocorrer
- WHEN um branch é matematicamente inatingível (ex.: `default` de switch exaustivo, guard de null que o compilador/Spring já garante) THEN documentar como tal em vez de forçar teste artificial (ver dimensão Observability / AC P2-BE-3)
- WHEN a meta de 95% force testes de baixo valor semântico (ex.: branch trivial de getter) THEN preferir consolidar em teste único por classe a multiplicar arquivos, mantendo rastreabilidade em `tasks.md`

---

## Requirement Traceability

| Requirement ID | Story                                   | Phase  | Status  |
| --------------- | ---------------------------------------- | ------ | ------- |
| COV-01          | P1: Backend linha 95%                    | Design | Pending |
| COV-02          | P1: Backend linha 95% (classes sem cobertura) | Design | Pending |
| COV-03          | P1: Backend linha 95% (não enfraquecer baseline) | Design | Pending |
| COV-04          | P1: Frontend linha 95%                   | Design | Pending |
| COV-05          | P1: Frontend linha 95% (páginas abaixo da meta) | Design | Pending |
| COV-06          | P1: Frontend linha 95% (não enfraquecer baseline) | Design | Pending |
| COV-07          | P2: Backend branch 95%                   | Design | Pending |
| COV-08          | P2: Backend branch 95% (métodos multi-branch) | Design | Pending |
| COV-09          | P2: Backend branch 95% (branch inatingível documentado) | Design | Pending |
| COV-10          | P2: Frontend branch 95%                  | Design | Pending |
| COV-11          | P2: Frontend branch 95% (páginas com branch baixo) | Design | Pending |
| COV-12          | P2: Frontend branch 95% (padrão de teste existente) | Design | Pending |
| COV-13          | P3: Gate automatizado                    | Design | Pending |
| COV-14          | P3: Gate — mensagem de falha clara       | Design | Pending |
| COV-15          | P3: Gate — log das 4 métricas            | Design | Pending |

**ID format:** `COV-NN`

**Status values:** Pending → In Design → In Tasks → Implementing → Verified

**Coverage:** 15 total, 0 mapped to tasks, 15 unmapped ⚠️ (Tasks phase ainda não executada)

---

## Success Criteria

- [ ] `mvn test` (backend): JaCoCo `LINE` ≥ 95% E `BRANCH` ≥ 95%
- [ ] `npm run test:coverage` (frontend): `Lines` ≥ 95% E `Branches` ≥ 95% em "All files"
- [ ] `bash diversos/scripts/check-coverage-95.sh` sai com código 0
- [ ] Nenhum teste da baseline (474 BE / 184 FE) foi removido ou desabilitado
- [ ] `TESTING.md` atualizado com a nova baseline e thresholds
