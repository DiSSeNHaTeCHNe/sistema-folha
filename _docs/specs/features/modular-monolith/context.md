# Modular Monolith — Context

**Gathered:** 2026-07-26  
**Spec:** `_docs/specs/features/modular-monolith/spec.md`  
**Status:** Ready for design (pending user confirm of spec)

---

## Feature Boundary

Refatoração estrutural do monolito **sistema-folha** para adequação às skills `modular-design-principles` e `modular-decomposition` — reorganização de pacotes por domínio, contratos explícitos cross-domain, controllers finos, SecurityConfig coerente e remoção de código morto/legado — **sem novas funcionalidades de produto**.

**Inclui:** backend (pacotes por domínio, ports in-process, remoção completa do benefício legado, correção do bug ACL organograma, alinhamento SecurityConfig); frontend mínimo (services alinhados, remoção de órfãos, pages sem chamada direta a `api`).

**Fora de escopo:** extração de microserviços; rewrite frontend conforme skills FE TARGET (AD-004); backend de Relatórios/PDF; strategy pattern de pagamentos; reabertura de CRUD/UX de benefícios já entregues; refatoração do harness (`ajuste-harness`).

---

## Implementation Decisions

### 1. Destino do `Beneficio` legado — Remoção completa

- **Decisão:** eliminar o modelo legado por completo — não renomear, não deprecar, não manter coexistência com flag por competência.
- Remover entity `Beneficio`, `BeneficioRepository`, fallback em `FolhaTotalizacaoService` e consumo legado em `DashboardService`.
- `BeneficioMensal` + `TipoBeneficio` passam a ser a **única** fonte de custo de benefícios na totalização e no dashboard.
- Frontend: remover `beneficioService.ts` e quaisquer imports/referências órfãs.
- Tabela `beneficios`: tratar via migration Flyway (drop ou arquivamento explícito conforme Design); dados históricos legados não migram automaticamente para mensal nesta feature — decisão de dados fica no Design (drop vs. migration de arquivo).
- Após remoção, o contrato Folha ↔ Benefícios usa **somente** a port de consulta mensal (ver área 4).

### 2. Organograma / Acesso — Submódulo de Organograma (2B)

- **Decisão:** `OrganogramaAcesso` vive como **submódulo** do domínio Organograma, não como shared kernel solto nem como filtro replicado por domínio.
- Outros domínios (Folha, Benefícios, Dashboard, etc.) consomem acesso **somente via contrato publicado** (port/interface), nunca injetando repositories ou services internos de Organograma diretamente.
- Controllers de outros domínios aplicam filtro delegando ao contrato; não duplicam regras de hierarquia/centros de custo.
- A correção do bug ACL (área 6) é implementada **dentro** deste submódulo e refletida no contrato.

### 3. Migração de pacotes — Incremental por domínio (3B)

- **Decisão:** migração **incremental**, um domínio por vez, com regras de dependência crescentes — não big bang, não congelar legado em pacote plano.
- **Ordem proposta (Design confirma):** Benefícios → Folha → Organograma (+ Acesso) → Cadastros → Importação → Auth/Security → demais.
- Código novo e código movido passam a residir em pacotes por domínio; camada técnica (web, persistence) permanece separada **dentro** de cada domínio ou como infra compartilhada mínima conforme Design.
- Enforcement de fronteiras: preferência por regras automatizadas (ArchUnit ou script equivalente) adicionadas progressivamente a cada domínio migrado — ferramenta exata fica em Agent's Discretion.
- Cada incremento deve manter build verde e testes existentes passando antes do próximo domínio.

### 4. Contrato Folha ↔ Benefícios — Port síncrona in-process (4A)

- **Decisão:** Folha consulta totais de benefícios via **port síncrona in-process** (ex.: `BeneficioConsultaPort` ou nome equivalente definido no Design).
- Após remoção do legado, a port expõe **apenas** dados de `BeneficioMensal` agregados por competência/funcionário/centro de custo conforme necessidade de `FolhaTotalizacaoService`.
- `FolhaTotalizacaoService` e `DashboardService` **não** acessam repositories de Benefícios diretamente — apenas a port.
- `BeneficioMensalController` **não** injeta repositories de outros domínios; controllers permanecem finos, delegando a services do próprio domínio.
- Sem read model separado, sem CQRS, sem eventos assíncronos nesta feature.

### 5. Escopo frontend — Mínimo apenas (5A)

- **Decisão:** frontend limitado ao necessário para não quebrar contratos API e alinhar fronteiras modulares na borda HTTP.
- **Must-have:** remover `beneficioService.ts` e referências; garantir que pages usem services de domínio (não `api`/`axios` direto); alinhar nomes/paths de services com rotas backend pós-refator; ajustar tipos em `types/index.ts` se contratos mudarem.
- **Fora desta feature:** promoção de skills FE TARGET (`api-client`, `forms-validation`, `component-architecture`, `routing-perf`, `testing-a11y`); React Query; Zod; reestruturação `src/features/`; testes Vitest/RTL; lazy routes — permanecem deferred (AD-004 / ROADMAP).
- Verificação BE + FE: ambos devem atender requisitos da spec — backend via ports/pacotes/testes; frontend via services alinhados e ausência de órfãos/reach-through na UI.

### 6. Bug ACL “sem nó = acesso total” — Corrigir nesta feature (6A, P1)

- **Decisão:** corrigir o comportamento incorreto documentado em `CONCERNS.md` e `OrganogramaAcessoService` — distinguir explicitamente **“usuário sem funcionário vinculado”** (sem acesso) de **“funcionário sem nó no organograma”** (não conceder acesso total por default).
- Comportamento pós-correção: regra de produto explícita no Javadoc/contrato da port; testes de integração cobrindo os cenários de borda (sem funcionário, sem nó, com nó e descendentes, admin).
- Não documentar o bug como comportamento intencional; não deferir para feature de segurança separada.

### Agent's Discretion

- Nomes exatos de pacotes Java por domínio (ex.: `br.com.techne.sistemafolha.beneficios`, `...folha`, `...organograma`).
- Nome exato da port Folha ↔ Benefícios e assinatura dos métodos.
- ArchUnit vs. script Gradle/Maven customizado para enforcement de dependências entre pacotes.
- Estrutura interna de subpastas por domínio (`api`, `domain`, `persistence` vs. variação alinhada a `CONVENTIONS.md`).
- Estratégia de migration Flyway para tabela `beneficios` (DROP vs. rename/archive) desde que remoção do legado no código seja completa.
- Ordem fina dentro de cada domínio na migração incremental (desde que respeite Benefícios → Folha → Organograma como sequência inicial).

### Declined / Undiscussed Gray Areas → Assumptions

Nenhuma das 6 áreas propostas na fase Discuss foi recusada ou deixada sem resposta. Todas foram resolvidas com decisões explícitas acima — **não há defaults agent-chosen pendentes** para registrar na spec.

---

## Specific References

- **Relatório consolidado de modular decomposition** (Patterns 1–5 + domain grouping) — inventário de componentes, acoplamento, duplicação de domínio e candidatos a bounded contexts; base para ordem de migração e fronteiras.
- **`_docs/specs/CONCERNS.md`** (2026-07-25) — dual model benefícios, reach-through em totalização/dashboard, SecurityConfig paths, bug ACL organograma, código morto FE.
- **`_docs/specs/ARCHITECTURE.md`** — monolito em camadas, ausência de bounded contexts formais, `OrganogramaAcessoService` como cross-cutting, dual benefit domain.
- **AD-004** (`STATE.md`) — skills FE são TARGET; obrigação atual = brownfield; esta feature faz apenas FE mínimo, não rewrite TARGET.
- **Features já entregues (não reabrir produto):** `beneficios-mensais`, `ajuste-beneficios-reorganizacao`, `alteracao-beneficios-mensais`.
- **Recomendação dos subagentes (aceita pelo usuário nas áreas 2–6):** `2B · 3B · 4A · 5A · 6A`; área 1 foi **mais restritiva** que as opções A/B/C originais — remoção total, não rename/deprecate/coexistência.

---

## Deferred Ideas

- **Rewrite frontend completo** conforme skills FE TARGET (`api-client`, forms Zod, `component-architecture`, `routing-perf`, `testing-a11y`) — feature separada pós-ROADMAP (AD-004).
- **Backend de Relatórios / PDF** — UI frontend existe sem API; implementação é feature própria, não boundary desta refatoração.
- **Extração de microserviços / Pattern 6** — fora de escopo; monolito modular in-process permanece.
- **Strategy pattern de pagamentos** — M3 / `_docs/STRATEGY_PATTERN_PAGAMENTOS.md`.
- **Shared kernel `acesso/` autônomo** (opção 2A não escolhida) — Acesso ficou submódulo de Organograma.
- **Big bang de pacotes** (opção 3A) e **congelamento de legado** (opção 3C) — rejeitados implicitamente pela escolha 3B.
- **Read model / CQRS / módulo consultas** (opções 4B/4C) — rejeitados pela escolha 4A.
