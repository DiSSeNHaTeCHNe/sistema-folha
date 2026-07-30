# Project State

_Persistent memory across sessions. Updated as decisions are made, blockers surface, and lessons are learned._

**Last Updated:** 2026-07-30  
**Current Work:** `adequacao-analise-projeto-r4` — squash merge to `main` (QG OK; meta interna 85% documentada como ressalva)

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

---

## Handoff

- **Feature (done)**: `adequacao-analise-projeto-r4` → squash merged to `main` @ `64acb93` (2026-07-30); QG OK @ `new_coverage` 80.0%; meta interna 85%/branch 70% = ressalva (leak period R3); Playwright PASS; ADP N/A Testcontainers
- **Feature (tasks draft)**: `auth-api-keys` → Design A approved; `tasks.md` T1–T15; aguarda Execute
- **Feature (done)**: `adequacao-analise-projeto-r3` → squash merged to `main` @ `bcff5f9`; Verifier PASS; Sonar QG OK @ 80.0%
- **Feature (done)**: `adequacao-analise-projeto` (R1) → `main` @ `047e64d`
- **Feature (done)**: `adequacao-analise-projeto-r2` → `main` @ `cb6e04a`
- **Feature (done)**: `acl-cc-competencia` → @ `7e0421d`; squash merge pendente
- **Feature (done)**: `organograma-linhas-hierarquia` → merged `main`
- **Decisions**: AD-001…AD-014 active
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
- [ ] Commits do usuário (`ajuste-harness` / `modular-monolith` / fix batch)
- [x] Feature `modular-boundary-hardening` — Execute T1–T12 done; AD-010 active; ready for Verifier
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

---

## Lessons Learned

### L-001: Relatórios frontend desconectado (2026-06-20)

**Context:** Brownfield mapping compared frontend services to backend controllers.  
**Problem:** `relatorioService.ts` calls six `/relatorios/*` endpoints with no backend implementation.  
**Solution:** Document in `CONCERNS.md`; prioritize backend relatórios or remove/disable UI until implemented.  
**Prevents:** Agents assuming PDF reports work because README or UI exists.

---

## Deferred Ideas

- Portal do colaborador (holerite self-service)
- Motor legal completo de folha substituindo ADP
- Multi-tenant
- Notificações em tempo real
- Adequação do código ao harness (fase 2) — ROADMAP Deferred
- Mover relatório de conformidade para `diversos/relatorios/` se precisar versionar

---

## Preferences

- Governança multiagente via Linear (key única por produto) e regras em `.agents/rules/`
- Spec-driven: Specify + Execute sempre; Design/Tasks conforme auto-sizing da skill TLC
- Harness: Approach A (patch in-place); TLC path → `_docs/specs/`; AGENTS só na raiz
- Sem commits automáticos nesta feature — usuário controla commits
