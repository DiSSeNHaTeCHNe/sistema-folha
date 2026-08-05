# Project State

_Persistent memory across sessions. Updated as decisions are made, blockers surface, and lessons are learned._

**Last Updated:** 2026-08-05  
**Current Work:** `workspace-usuario` — **P3 complete** (T1–T48); release + security + MCP gates green; ready for Verifier

---

## Decisions

### AD-001: Layout spec-driven em `_docs/specs/` (2026-06-20)

**Decision:** Todos os artefatos TLC spec-driven ficam no layout flat `_docs/specs/` (sem `.specs/` ou subpastas `project/` / `codebase/`).  
**Reason:** Alinhamento com `AGENTS.md` e `.agents/references/specs-layout.md`.  
**Trade-off:** Layout antigo `.specs/codebase/` abandonado; `.specs/` ignorado no git.  
**Impact:** Novas features usam `_docs/specs/features/[feature]/`; brownfield docs na raiz de `_docs/specs/`. Quick task 001 confirmou cleanup (2026-06-20).  
**Status:** active (reforçado por AD-005)

### AD-002: Layout físico frontend / backend / diversos (2026-06-20)

**Decision:** Código de produção em `frontend/` e `backend/`; auxiliares em `diversos/`.  
**Reason:** Separação clara para multiagente (frentes frontend/backend) e raiz enxuta.  
**Trade-off:** `src/` e `pom.xml` saíram da raiz; relatórios operacionais em `diversos/relatorios/`.  
**Impact:** Dockerfile, README, `.gitignore` e specs usam `frontend/` e `backend/` (quick task 003 renomeou de `front/`/`back/`).  
**Status:** active

### AD-003: Núcleo do harness versionado no Git (2026-07-26)

**Decision:** Versionar `AGENTS.md` (raiz), `.agents/`, `_docs/specs/`, `.claude/CLAUDE.md` e `.cursor/rules/`.  
**Reason:** Harness deve sobreviver a clone/CI; não pode ser só local.  
**Trade-off:** Diffs de governança no Git; prefs locais de IDE fora do tracking.  
**Scope:** `.gitignore`, onboarding de agentes.  
**Status:** active (applied in Execute `ajuste-harness`)

### AD-004: Skills FE = TARGET até liberação no ROADMAP (2026-07-26)

**Decision:** `api-client`, `forms-validation`, `component-architecture`, `routing-perf`, `testing-a11y` são target; obrigação atual = brownfield (`CONVENTIONS`/`STRUCTURE`/`TESTING` + código).  
**Reason:** Skills aspiracionais não devem forçar refactors antes da feature de adequação.  
**Trade-off:** Dualidade current/target até a fase 2.  
**Scope:** Frontend agents / PRs.  
**Status:** active (applied in Execute `ajuste-harness`)

### AD-005: TLC paths canônicos em `_docs/specs/` (2026-07-26)

**Decision:** Patch cirúrgico na skill `tlc-spec-driven` para docs/memória/artefatos/lessons usarem `_docs/specs/` (não `.specs/`). Fluxo TLC intacto.  
**Reason:** Eliminar cisma com AD-001 sem symlink.  
**Trade-off:** Diff de strings na skill upstream-local.  
**Scope:** `.agents/skills/tlc-spec-driven/**`.  
**Status:** active (applied in Execute `ajuste-harness`)

### AD-006: AGENTS só na raiz; `_docs` sem governança permanente (2026-07-26)

**Decision:** Canônico = `AGENTS.md` na raiz. Apagar `_docs/AGENTS.md`. Em `_docs/`, só `_docs/specs/` (processo) e `_docs/temp/` (transitório, não versionado).  
**Reason:** Duplicatas divergem; usuário definiu AGENTS sob `_docs` como transitório.  
**Trade-off:** Ferramentas devem apontar para a raiz.  
**Scope:** Governança / IDE pointers.  
**Status:** active (applied in Execute `ajuste-harness`)

### AD-007: Monólito modular in-process; remoção Beneficio legado (2026-07-26)

**Decision:** Adequação modular via pacotes por domínio in-process (sem microserviços). Remover domínio legado `Beneficio` por completo. OrganogramaAcesso como submódulo de Organograma (contrato). Migração incremental. Ports síncronas. FE mínimo. ACL: negar sem funcionário e sem nó.  
**Reason:** modular-design-principles + decomposition P0/P1; usuário confirmou remoção legado e refactor-only.  
**Trade-off:** Drop dados `beneficios` sem migração automática para mensal; breaking change em semântica ACL (fim de acesso total implícito).  
**Scope:** Feature `modular-monolith`; BE+FE mínimo.  
**Status:** active

### AD-008: Layout de pacote `{dominio}.{camada}` (2026-07-26)

**Decision:** Código backend em `br.com.techne.sistemafolha.{dominio}.{api|application|domain|infrastructure|port}` dentro de um único módulo Maven; comunicação cross-domain via packages `*.port` apenas.  
**Reason:** Approach A do Design `modular-monolith`; Spring Boot scan na raiz cobre subpacotes; alinha modular-decomposition sem multi-módulo.  
**Trade-off:** Moves grandes de package; período híbrido até P2 completar.  
**Scope:** Backend Java; futuras features devem colocar código novo no domínio correspondente.  
**Status:** active

### AD-009: ArchUnit application-layer + allowlist dashboard/importacao (2026-07-26)

**Decision:** `ModularArchitectureTest` SHALL incluir regras que proíbem `..application..` de depender de `..infrastructure..` de **outro** domínio (same-domain permitido). Até existirem ports Folha/Cadastros stats, `dashboard.application` e `importacao.application` ficam em **allowlist temporária** documentada no teste e no design `modular-monolith-fix` Approach A. Novas features não podem expandir a allowlist sem AD superseding.  
**Reason:** Fecha gap Verifier (application cross-infra) sem big-bang de ports de escrita/leitura Folha neste fix.  
**Trade-off:** Isolamento AD-008 incompleto nesses dois packages até follow-up.  
**Scope:** Backend ArchUnit; feature `modular-monolith-fix`; follow-up obrigatório para remover allowlist.  
**Status:** superseded by AD-010

### AD-010: ArchUnit dashboard/importacao sem allowlist (2026-07-27)

**Decision:** `ModularArchitectureTest` inclui regras simétricas `dashboard_application_must_not_access_foreign_infrastructure` e `importacao_application_must_not_access_foreign_infrastructure`. Allowlist AD-009 removida; consumidores usam apenas `*.port` (FolhaConsultaPort, FolhaImportacaoPort, CadastrosImportLookupPort, BeneficioConsultaPort, OrganogramaAcessoPort, UsuarioLookupPort).  
**Reason:** Fecha dívida AD-009 após ports agregadoras + ACL dashboard + refactor importação ADP (feature `modular-boundary-hardening`).  
**Trade-off:** Nenhum — isolamento AD-008 completo nos application packages cobertos.  
**Scope:** Backend ArchUnit; feature `modular-boundary-hardening` MODBH-27…30.  
**Status:** active

### AD-011: Permissão `ACESSO_TOTAL` ≠ `ADMIN` (2026-07-27)

**Decision:** Visão global de dados (`acessoTotal=true` no `OrganogramaAcessoPort`) exige permissão explícita `ACESSO_TOTAL`. Role `ADMIN` permanece só para mutações privilegiadas (`hasRole("ADMIN")`) e **não** implica `acessoTotal`. Seed admin recebe ambas. Concessão a qualquer usuário via `usuario_permissoes`.  
**Reason:** Least privilege; fecha gap pós MOD-09 onde `acessoTotal` nunca era setado em produção; evita funcionário fantasma no organograma.  
**Trade-off:** Resumo da folha continua unscoped neste MVP (ACL no resumo Deferred).  
**Scope:** ACL organograma + consumidores Folha/Benefícios/Dashboard; feature `acl-acesso-total-role`.  
**Status:** active

### AD-012: Custo Empresa = ficha × % + benefícios (sem rateio ADP) (2026-07-29)

**Decision:** Supersede D4-CLT na composição de `custoEmpresa`: custo usa `valorOriginal × operador_custo × porcentagem/100` (folha ADP + fixas + calculadas) + `custoBeneficios`; bruto/líquido usam **valor original** sem `%`. Feature: `folha-custo-clt-fix2`.  
**Reason:** Paridade card↔aba Custo; alinhamento Custo Techne legado; rateio rodapé ADP rejeitado.  
**Trade-off:** `total_encargos` snapshot ADP permanece informativo; migração de % legado em P2 (FIX2-17).  
**Scope:** Motor folha, totais, resumo, dashboard, detalhe; bruto/líquido inalterados (sem %).  
**Status:** active (spec draft fix2, refinado 2026-07-29)

### AD-013: API Key PAT — Bearer dual-path + permissão `API_KEY` (2026-07-29)

**Decision:** Credenciais de longa duração para integrações/agentes usam API Key (PAT) por `Usuario`: header `Authorization: Bearer` com prefixo `sf_live_`, convivendo com JWT no mesmo filtro; secret só hash (BCrypt); permissão explícita `API_KEY` para criar/usar; expiry obrigatória ≤365 dias; `ADMIN` pode revogar keys alheias; UI `/api-keys`. Domínio `auth.*` (AD-008). Servidor MCP fora desta feature.  
**Reason:** Feature `auth-api-keys` — Approach A; evita JWT de sessão em `mcp.json` e token compartilhado sem ACL.  
**Trade-off:** Filtro de auth sensível (mitigado com testes de regressão JWT); sem escopos granulares / rate limit no MVP.  
**Scope:** Backend auth/security + FE página ApiKeys + chip Usuários.  
**Status:** active

### AD-015: Relatórios PDF server-side + DashboardConsultaPort (2026-08-03)

**Decision:** Relatórios executivos PDF gerados no backend com **OpenPDF 2.0.3** (Java 17); agregados gerenciais cross-feature via **`DashboardConsultaPort`** — domínio `relatorios` não duplica lógica de dashboard. Geração assíncrona (`PENDENTE` → `PROCESSADO`/`ERRO`); blob PDF em PostgreSQL BYTEA; frontend hub com polling 2s.  
**Reason:** Feature `relatorios-executivos` — Approach A do design; ACL server-side; paridade KPIs REL-08; fecha L-001.  
**Trade-off:** Layout PDF verbose (OpenPDF programático); thumbnail MVP = ícone PDF (sem PDFBox page-1 render).  
**Scope:** Domínio `relatorios.{api,application,infrastructure}`; extensão `BeneficioConsultaPort`; FE `pages/Relatorios/` hub cards.  
**Status:** active

### AD-016: Tema Techne como padrão do frontend (2026-08-04)

**Decision:** `TEMA_PADRAO = 'techne'` no frontend; paleta alinhada a `relatorios.branding.primary-color` (`#7836FC`, AD-015); tipografia Poppins empacotada via `@fontsource/poppins`.  
**Reason:** Feature `temas-visuais` Fase 5 — coerência visual entre UI e PDFs executivos; identidade Techne como default para novos usuários.  
**Trade-off:** Preferência persiste só em `localStorage` (sem sync por usuário no backend neste MVP).  
**Scope:** `frontend/src/theme/**`, bootstrap `main.tsx`; cinco temas permanecem selecionáveis.  
**Status:** active

### AD-017: JSONB para preferências estruturadas por usuário (2026-08-04)

**Decision:** Layout de dashboard customizável persiste em `dashboard_layout.widgets JSONB` mapeado via `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6 nativo, sem dependência extra). Payload sempre lido/escrito inteiro; `versao_schema` permite normalização lazy.  
**Reason:** Feature `dashboard-customizavel` — cardinalidade baixa (1 row/usuário), evolução de `config` por widget na Fase 2 sem migration destrutiva; alternativa normalizada não traz benefício no caminho quente.  
**Trade-off:** Primeiro JSONB do projeto — exige teste de round-trip e validação server-side do array (não confiar só no PG).  
**Scope:** `dashboard.domain.DashboardLayout`, Flyway `V1.29`; futuras preferências estruturadas por usuário devem reutilizar este padrão antes de inventar outro.  
**Status:** active

### AD-018: Motor de expressão usuário = AST whitelist, sem scripting (2026-08-04)

**Decision:** Qualquer fórmula/expressão definida por usuário (workspace, templates publicados, propostas IA) SHALL usar parser + AST próprio com whitelist fixa de operadores e funções (`SOMA`, `MÉDIA`, `SE`, `MÍN`, `MÁX`, `CONTAGEM`, aritmética/comparação). **Proibido** SpEL, JSR-223, `eval`, ou bibliotecas genéricas sem sandbox comprovado. Avaliação sempre sobre campos tipados + `BigDecimal`.  
**Reason:** Feature `workspace-usuario` — fórmulas viajam com templates e rodam em nome de outros usuários; risco inaceitável de execução arbitrária.  
**Trade-off:** Implementação inicial maior que exp4j/SpEL; ganho em controle e nomenclatura pt-BR.  
**Scope:** Domínio `workspace.application.FormulaEngine`; futuras features com expressão usuário devem reutilizar ou estender este motor, não introduzir alternativa.  
**Status:** active (draft design `workspace-usuario`)

---

## Handoff

- **Branch:** `feat/workspace-usuario` (HEAD after Batch 7 / T48)
- **Feature (execute complete, not merged)**: `workspace-usuario` — P1+P2+P3 done (T1–T48); IA propor-e-confirmar end-to-end; WKS-24…32 implemented; dispatch Verifier
- **Feature (closed on main)**: `relatorios-executivos` + `relatorios-executivos-fix1` → mergeados (2026-08-03); AD-015 active; validation PASS / PASS com ressalvas
- **Feature (closed on main)**: `auth-api-keys` + fix1 + fix2 → mergeados; AD-013 active; PAT `sf_live_`, UI `/api-keys`
- **Quick task (done)**: `011-repo-limpo-atualizado` — sync governança, README, specs pendentes, higiene git
- **Branches locais obsoletas (merged)**: `feat/relatorios-executivos`, `feat/auth-api-keys`, `feat/mcp-agent-tools`, `feat/acl-cc-competencia`, `feat/folha-custo-clt`, `feat/organograma-linhas-hierarquia` — candidatas a delete
- **Branches locais ativas (não merged)**: `feat/cobertura-testes-95`, `feat/adequacao-analise-projeto`, `feat/qualidade-criticos-sonar`, `feat/temas-visuais`
- **Decisions**: AD-001…AD-016 active
---

## Blockers

_None currently._

---

## Todos

- [x] Inicializar projeto (`PROJECT.md`, `ROADMAP.md`, `STATE.md`)
- [x] Executar `map codebase` → brownfield docs
- [x] Specificar feature `ajuste-harness`
- [x] Aprovar Design `ajuste-harness` → Tasks → Execute (T1–T13 done, uncommitted)
- [x] Specificar feature `modular-monolith` (spec + context)
- [x] Design Approach A `modular-monolith`
- [x] Tasks `modular-monolith` (T1–T32 drafted)
- [x] Specificar + Design Approach A `modular-monolith` → Tasks → Execute T1–T32 (uncommitted; Verifier FAIL)
- [x] Design Approach A `modular-monolith-fix` (AD-009)
- [x] Aprovar tasks `modular-monolith-fix` → Execute → Verifier fix → re-Verifier pai
- [x] Specificar feature `modular-acl-security-fix` (ACL empty-set + refresh permitAll + Folha delete ACL)
- [x] Re-review spec `modular-acl-security-fix` vs tree pós sibling/parent PASS (2026-07-26)
- [x] Execute `modular-acl-security-fix` T1–T4 + Verifier PASS + code-review (uncommitted)
- [x] Commits do usuário (`ajuste-harness` / `modular-monolith` / fix batch) — conteúdo em `main`
- [x] Feature `modular-boundary-hardening` — Execute T1–T12 done; AD-010 active; ready for Verifier
- [x] Specificar feature `dashboard-customizavel` (spec + context, Fase 1+2, 44 requisitos) — 2026-08-04
- [x] Tasks `dashboard-customizavel` — 24 tasks, 4 batches, 44/44 DASHC mapped — 2026-08-04
- [ ] Execute `dashboard-customizavel` Batch 1 (T1–T6) — backend layout foundation
- [ ] **Débito técnico `temas-fidelidade-visual`** — 11 itens registrados em `_docs/specs/features/temas-fidelidade-visual/debito-tecnico.md` (2026-08-04). Nenhum quebra funcionalidade. Prioridade: DT-1 foco sem indicador visível (WCAG 2.4.7 AA, pré-existente) · DT-2 rota ativa não indicada no Drawer (`chrome.selecionado` é código morto) · DT-3 `MuiPickersPopper` sem borda no escuro (regressão da quick 014, fix de 1 linha) · DT-4 Alerts perdem cor semântica sob os tints (R-3 materializada, decisão de design)
- [ ] Deferred concerns (not this fix): `/usuarios` ADMIN privilege escalation; password logging hygiene; N+1 import loops
- [ ] Feature futura: adequação do código às skills FE target / gaps de segurança do relatório
- [x] Migrar ou descartar artefatos legados em `.specs/codebase/`

---

## Quick Tasks Completed

| #   | Description                         | Date       | Commit  | Status  |
| --- | ----------------------------------- | ---------- | ------- | ------- |
| 001 | Canonical specs cleanup (`.specs/`) | 2026-06-20 | pending | ✅ Done |
| 002 | Organizar pastas + diversos              | 2026-06-20 | pending | ✅ Done |
| 003 | Renomear front/back → frontend/backend   | 2026-06-20 | pending | ✅ Done |
| 004 | Fix TS build em Funcionarios/index.tsx     | 2026-06-20 | pending | ✅ Done |
| 005 | Importação no menu Cadastros                 | 2026-06-20 | —       | ✅ Done |
| 006 | Organograma no menu Cadastros                | 2026-06-20 | —       | ✅ Done |
| 007 | Inativar funcionário                         | 2026-07-27 | —       | ✅ Done |
| 008 | 13º na linha da folha                        | 2026-07-28 | —       | ✅ Done |
| 009 | Folha detalhe separar 13º                    | 2026-07-28 | —       | ✅ Done |
| 010 | Sonar gate new_violations                    | 2026-08-02 | pending | ✅ Done |
| 011 | Repo limpo e docs alinhados                  | 2026-08-03 | bf669dd | ✅ Done |
| 012 | Contraste dos temas: avatar de KPI + texto   | 2026-08-04 | d55132d, 649041a | ⚠️ Parcial (D-5 bloqueado: 4º arquivo) |
| 013 | D-5: `primary.main` como cor de texto (AA)   | 2026-08-04 | 641fb2e, 5a92326 | ✅ Done (teto de arquivos estendido pelo usuário) |
| 014 | Overlay de elevação off + varredura do fundo efetivo | 2026-08-04 | afea6f6, 95df7fe, 2640f2b, 2378986 | ✅ Done (teto estendido; `Alert`/R-3 fica como dívida) |

---

## Lessons Learned

### L-001: Relatórios frontend desconectado (2026-06-20) — **Resolved 2026-08-03**

**Context:** Brownfield mapping compared frontend services to backend controllers.  
**Problem:** `relatorioService.ts` calls `/relatorios/*` endpoints with no backend implementation.  
**Solution:** Feature `relatorios-executivos` + fix1 mergeados em `main`; domínio `relatorios.*` com PDF async (AD-015).  
**Prevents:** Agents assuming PDF reports work because README or UI exists — verificar controllers antes de marcar Open.

---

## Deferred Ideas

- Portal do colaborador (holerite self-service)
- Motor legal completo de folha substituindo ADP
- Multi-tenant
- Notificações em tempo real
- Adequação do código ao harness (fase 2) — ROADMAP Deferred
- Mover relatório de conformidade para `diversos/relatorios/` se precisar versionar
- **Orçamento e Planejamento de Custo de Pessoal** — spec draft criado 2026-08-04 (`_docs/specs/features/orcamento-custo-pessoal/spec.md`); orçado x realizado por centro de custo com consolidação hierárquica via organograma; ADP não cobre isso, é a motivação original da feature; torna-se o primeiro template de `workspace-usuario` (abaixo); aguardando priorização (Design pendente)
- **Workspace do Usuário — Dados, Widgets e Templates** — spec + **design Draft** 2026-08-04 (`_docs/specs/features/workspace-usuario/`); domínio `workspace.*` sibling (Approach A), motor AST whitelist (AD-018), template orçamento híbrido inline; aguardando aprovação do design → Tasks. Plataforma de **dois níveis**: Nível 1 = Dashboard Customizável (`estudo-dashboard-customizavel.md`, movido para dentro desta pasta; catálogo fixo de widgets, sem dataset próprio, para quem não quer complexidade — Fase 1/2 entregue antes) e Nível 2 = Workspace (esta spec; datasets próprios com esquema tipado, widgets com fórmula restrita, múltiplos workspaces, catálogo interno de templates escopado à hierarquia, para quem quer algo mais avançado), com o Nível 2 reaproveitando o registry de widgets e o modelo de layout do Nível 1 em vez de recriá-los; inclui camada de IA via MCP (propor-e-confirmar, permissão dedicada, sob demanda) construída sobre `mcp-agent-tools`; orçamento é o primeiro template nativo; `estudo-dashboard-query-builder.md` (Fase 3 do Nível 1, condicionada a gatilhos) também movido para a mesma pasta; Complex — Design pendente
- **Dashboard Customizável (Nível 1)** — spec + context criados 2026-08-04 em `_docs/specs/features/dashboard-customizavel/`, promovidos de `workspace-usuario/estudo-dashboard-customizavel.md`; escopo confirmado com o usuário = **Fase 1 + Fase 2** do estudo (layout/seleção de widgets persistido por usuário + widgets parametrizáveis por competência/topN/filtro), com Fase 3 (query builder) e templates por papel fora de escopo; 44 requisitos `DASHC-01…44`; Complex — Design pendente. Decisões-chave: layout padrão = paridade total com o dashboard atual; `/dashboard` clássico preservado com dois itens de menu simultâneos; acesso à tela nova gated por escopo de dados no organograma (sem permissão nova); salvamento explícito; Fase 1 expõe só largura. **Alerta para o Design:** o estudo assumiu convenções que não existem no código — não há JSONB algum no backend (AD-DC-03 seria o primeiro), a validação devolve 400 e não 422, não há `ToastContainer` montado, e `@dnd-kit/sortable` está instalado mas nunca foi importado
- **Preferência de tema por usuário no backend** — hoje a escolha de tema persiste só em `localStorage`; sync server-side permitiria mesma aparência em múltiplos dispositivos e política corporativa de tema padrão

---

## Preferences

- Governança multiagente via Linear (key única por produto) e regras em `.agents/rules/`
- Spec-driven: Specify + Execute sempre; Design/Tasks conforme auto-sizing da skill TLC
- Harness: Approach A (patch in-place); TLC path → `_docs/specs/`; AGENTS só na raiz
- Sem commits automáticos nesta feature — usuário controla commits
