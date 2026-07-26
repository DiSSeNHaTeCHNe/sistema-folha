# Ajuste do Harness — Specification

## Problem Statement

O harness de agentes foi introduzido depois do projeto brownfield já existir. Parte descreve a realidade do código; parte descreve um alvo aspiracional. Hoje o harness não é confiável para desenvolver e manter o produto: núcleo fora do Git, paths TLC vs `_docs/specs/` divergentes, memória (STATE) defasada, skills FE obrigando padrões inexistentes, e ponteiros de ferramentas inconsistentes. Sem um harness coerente e versionável, a fase seguinte (adequar o projeto) só multiplica ruído.

## Goals

- [ ] Núcleo do harness versionado no Git e utilizável após clone fresco
- [ ] Paths canônicos em `_docs/specs/` (HARN-03)
- [ ] Skills FE marcadas como target; brownfield documentado como fonte obrigatória até liberação no ROADMAP
- [ ] Ferramentas apontando para `AGENTS.md` **só na raiz**; `_docs/AGENTS.md` apagado (`_docs` = specs + temp transitório)
- [ ] Decisões desta feature revisadas e aprovadas pelo usuário **antes** de qualquer Execute

## Out of Scope

| Item | Reason |
| ---- | ------ |
| Adequação do código (backend/frontend) às skills | Feature separada — fase 2 após harness estável |
| Refatorar fluxo TLC (Verifier, auto-sizing, lessons rules) | Só patch de paths para `_docs/specs/` |
| Manter `AGENTS.md` sob `_docs/` | `_docs/` não hospeda governança permanente — só transitório (`temp`) ou specs |
| Conteúdo / migração de `_docs/temp/` | Material transitório — não considerar |
| Popular PRD/TDD/SDD com conteúdo de produto | Fora do P1; seguir AGENTS quando docs existirem |
| Configuração completa Antigravity “pronta para produção” | Uso eventual; só ponteiro/padrão agora |
| Mudar Linear key / frentes oficiais | Já definidos em `linear_issue_management.md` |

---

## Assumptions & Open Questions

| Assumption / decision | Chosen default | Rationale | Confirmed? |
| --------------------- | -------------- | --------- | ---------- |
| Git: versionar núcleo | `AGENTS.md` + `.agents/` + `_docs/specs/` | Escolha 1A | y |
| Skills FE | Target + banner “não aplicar até ROADMAP”; brownfield em CONVENTIONS/STRUCTURE/TESTING | Escolha 2B | y |
| TLC paths | Patch **cirúrgico** em TLC: `.specs/` → `_docs/specs/` (docs/memória/artefatos); sem mudar o fluxo da skill | Q1 revisado | y |
| Linear / PRD | Conforme `AGENTS.md` + rule Linear; sem inventar PRD vazio | “conforme solicitado” | y |
| AGENTS canônico | Só raiz; **apagar** `_docs/AGENTS.md`; `_docs` não guarda governança permanente | Q2 | y |
| `_docs/temp` | Fora de escopo e fora do Git | Pedido explícito | y |
| Symlink `.specs` | Não necessário se patch TLC cobrir | Consequência de Q1 | y |
| Execute só após OK | Nenhuma implementação até confirmação desta revisão | Pedido explícito | y |
| Forma exata `.gitignore` | `_docs/*` + `!_docs/specs/**`; track `.agents/`, AGENTS raiz, `.claude/`, `.cursor/rules/` | Design Approach A | y |

**Open questions:** none — Q1/Q2 resolvidos; detalhe de `.gitignore` fica para Design.

---

## User Stories

### P1: Harness versionável no Git ⭐ MVP

**User Story**: Como desenvolvedor/agente, quero clonar o repositório e já ter `AGENTS.md`, `.agents/` e `_docs/specs/` disponíveis, para que governança e specs não dependam de uma máquina local.

**Why P1**: Sem isso o harness não existe para o time/CI.

**Acceptance Criteria**:

1. WHEN um clone limpo do repositório for feito THEN o sistema SHALL incluir `AGENTS.md`, `.agents/rules/`, `.agents/skills/` (incl. TLC intacta), `.agents/references/specs-layout.md` e `_docs/specs/` (brownfield + features) versionados
2. WHEN o `.gitignore` for aplicado THEN `_docs/temp/`, `.env*`, secrets e layout deprecado `.specs/` (como árvore real de conteúdo, se aplicável) SHALL permanecer fora do versionamento
3. WHEN um agente ler `AGENTS.md` após o clone THEN SHALL encontrar ponteiros válidos para rules/skills/references sem depender de `_docs/temp/`

**Independent Test**: Clone em diretório limpo (ou `git check-ignore` / `git ls-files`) e verificar presença do núcleo e ausência de temp/secrets.

---

### P1: Paths TLC alinhados a `_docs/specs/` (patch cirúrgico)

**User Story**: Como agente TLC, quero que documentação, memória e artefatos usem `_docs/specs/`, com um ajuste mínimo na skill, para eliminar o cisma `.specs/` vs AD-001.

**Why P1**: Paths dúbios quebram STATE, features, validation e lessons.

**Acceptance Criteria**:

1. WHEN Specify/Design/Tasks/Validate/lessons gerarem ou lerem artefatos THEN SHALL usar `_docs/specs/` (incl. `features/`, `STATE.md`, `lessons.json` / `LESSONS.md` se aplicável)
2. WHEN o patch TLC for aplicado THEN SHALL alterar **apenas** referências de path/layout necessárias; SHALL NOT mudar Verifier, auto-sizing, contrato de tasks, nem regras de lessons além do path de store
3. WHEN `AGENTS.md` e `.agents/references/specs-layout.md` forem lidos THEN SHALL permanecer coerentes com o TLC após o patch (uma fonte canônica)
4. WHEN um agente procurar layout `.specs/` como canônico do **projeto** THEN SHALL NOT encontrá-lo documentado como destino oficial (pode restar menção histórica só se inevitável; preferir zero)

**Independent Test**: `rg -q '\.specs'` na skill TLC pós-patch = 0 hits de path canônico (ou só menções de migração documentadas); novo artefato cai em `_docs/specs/features/ajuste-harness/`.

---

### P1: Skills target vs brownfield explícitos

**User Story**: Como agente de frontend, quero saber se uma skill é obrigação atual ou alvo futuro, para não “corrigir” o brownfield contra o ROADMAP.

**Why P1**: Skills FE aspiracionais geram falso não-conformidade e PRs errados.

**Acceptance Criteria**:

1. WHEN um agente abrir skill FE target (`api-client`, `forms-validation`, `component-architecture`, `routing-perf`, `testing-a11y`) THEN SHALL encontrar aviso explícito de **target** e condição de aplicação (ROADMAP / AD)
2. WHEN a tarefa não estiver liberada no ROADMAP THEN o agente SHALL seguir `CONVENTIONS.md` / `STRUCTURE.md` / `TESTING.md` e o código existente como fonte obrigatória
3. WHEN `ROADMAP.md` ou STATE forem atualizados nesta feature THEN SHALL registrar que adequação de código às skills target é **feature posterior**, não deste MVP

**Independent Test**: Ler header das 5 skills e CONVENTIONS/STRUCTURE; confirmar linguagem target vs current.

---

### P1: Um AGENTS.md canônico (raiz) — sem governança permanente em `_docs/`

**User Story**: Como operador multi-ferramenta, quero um único `AGENTS.md` na raiz e `_docs/` livre de cópias de governança, porque nessa pasta esse tipo de arquivo é só transitório.

**Why P1**: Duplicata sob `_docs/` diverge e viola a regra de uso da pasta.

**Acceptance Criteria**:

1. WHEN governança for consultada THEN SHALL existir `AGENTS.md` **somente na raiz** como canônico
2. WHEN `_docs/AGENTS.md` for verificado após Execute THEN SHALL **não existir** (apagado)
3. WHEN novos arquivos de governança permanente forem propostos sob `_docs/` (exceto specs TLC em `_docs/specs/`) THEN SHALL ser rejeitados; transitórios vão para `_docs/temp/`
4. WHEN Claude for usado THEN `.claude/CLAUDE.md` SHALL apontar para o AGENTS da raiz
5. WHEN Cursor for usado THEN a config SHALL apontar para `AGENTS.md` + `.agents/` com paths reais (sem `_D2TLabs`)
6. WHEN Windsurf não tiver config real THEN stubs vazios SHALL ser removidos ou documentados como não suportados
7. WHEN Antigravity for preparado THEN SHALL apontar para a **raiz**, sem recriar `_docs/AGENTS.md`

**Independent Test**: `test ! -f _docs/AGENTS.md`; `test -f AGENTS.md`; Claude/Cursor resolvem a raiz.

---

### P1: Revisão obrigatória antes do Execute ⭐

**User Story**: Como dono do projeto, quero revisar e confirmar todas as decisões desta spec/context antes de qualquer commit de ajuste de harness.

**Why P1**: Pedido explícito do usuário.

**Acceptance Criteria**:

1. WHEN spec + context forem apresentados THEN Execute SHALL NOT iniciar até confirmação explícita do usuário (“aprovado” / equivalente)
2. WHEN open questions da seção Assumptions forem respondidas THEN spec SHALL ser atualizada (Confirmed? = y) antes do Design/Tasks
3. WHEN o usuário rejeitar um default THEN o default SHALL ser alterado no spec/context antes de Design

**Independent Test**: Nenhum PR/commit de `.gitignore`/skills banners até mensagem de aprovação.

---

### P2: Memória operacional mínima do harness

**User Story**: Como agente em sessão nova, quero STATE/HANDOFF refletindo o trabalho de ajuste do harness, para retomar sem reler o relatório ad hoc.

**Why P2**: Importante para manutenção; pode seguir logo após P1 Git/paths.

**Acceptance Criteria**:

1. WHEN o Design/Execute desta feature avançar THEN `STATE.md` SHALL receber ADs novos (versionamento, target skills, compat TLC) e Current Work atualizado
2. WHEN a sessão pausar THEN `HANDOFF.md` ou seção Handoff em STATE SHALL existir com próximo passo claro
3. WHEN `_docs/RELATORIO_CONFORMIDADE_HARNESS_2026-07-26.md` for referenciado THEN SHALL ser tratado como diagnóstico histórico, não como contrato vivo

**Independent Test**: Abrir STATE e ver ADs desta feature + handoff.

---

### P3: Checklist de “feature fechada” no AGENTS

**User Story**: Como QA/agente Verifier, quero um checklist curto no AGENTS apontando validation.md / gate de testes conforme TLC, para não marcar Verified sem evidência.

**Why P3**: Melhora adoção; TLC já define o contrato — só precisa eco no AGENTS.

**Acceptance Criteria**:

1. WHEN uma feature Large/Complex for encerrada THEN AGENTS (ou rule curta) SHALL exigir artefato de verificação em `_docs/specs/features/[feature]/validation.md` conforme fluxo TLC, **sem alterar** a skill TLC

**Independent Test**: Trecho em AGENTS/rule legível e apontando path canônico `_docs/specs/`.

---

## Edge Cases

- WHEN `_docs/` deixar de ser ignorado por completo THEN `_docs/temp/` SHALL continuar ignorado e SHALL NOT ser commitado
- WHEN alguém recriar `_docs/AGENTS.md` THEN o harness SHALL tratá-lo como erro de processo (governança só na raiz; temp só em `_docs/temp/`)
- WHEN skill FE target for invocada em tarefa não liberada THEN agente SHALL avisar e seguir brownfield em vez de refatorar para o alvo
- WHEN Antigravity ainda não estiver configurado THEN ausência de config Antigravity SHALL NOT bloquear P1
- WHEN o patch TLC deixar algum writer ainda em `.specs/` THEN SHALL ser corrigido no mesmo P1 (não deixar dois layouts vivos)

---

## Implicit-Requirement Dimensions

| Dimension | Resolution |
| --------- | ---------- |
| Input validation & bounds | N/A — docs/processo; bounds = o que entra/sai do Git |
| Failure / partial-failure | Clone sem núcleo = falha de governança; mitigado por versionar P1 |
| Idempotency / retry | Reaplicar `.gitignore` e banners deve ser idempotente |
| Auth boundaries & rate limits | N/A |
| Concurrency / ordering | Patch TLC unifica writers em `_docs/specs/` |
| Data lifecycle / expiry | temp não versionado; AGENTS sob `_docs` = transitório proibido como permanente; relatório = histórico |
| Observability | validation.md / STATE ADs como evidência de processo |
| External-dependency failure | N/A (ferramentas IDE); Antigravity eventual |
| State-transition integrity | Spec confirmed → Design → Tasks → Execute só após OK do usuário |

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| HARN-01 | P1: Harness versionável | Execute | Implemented (uncommitted) |
| HARN-02 | P1: Ignore temp/secrets | Execute | Implemented (uncommitted) |
| HARN-03 | P1: Paths `_docs/specs/` (patch TLC) | Execute | Implemented (uncommitted) |
| HARN-04 | P1: Patch TLC só paths (fluxo intacto) | Execute | Implemented (uncommitted) |
| HARN-05 | P1: Skills FE target banner | Execute | Implemented (uncommitted) |
| HARN-06 | P1: Brownfield obrigatório até ROADMAP | Execute | Implemented (uncommitted) |
| HARN-07 | P1: Apagar `_docs/AGENTS.md`; canônico na raiz | Execute | Implemented (uncommitted) |
| HARN-08 | P1: Cursor/Claude paths | Execute | Implemented (uncommitted) |
| HARN-09 | P1: Remover Windsurf stub | Execute | Implemented (uncommitted) |
| HARN-10 | P1: Gate revisão pré-Execute | — | Satisfied (Execute authorized) |
| HARN-11 | P2: STATE/HANDOFF ADs | Execute | Implemented (uncommitted) |
| HARN-12 | P3: Checklist Verified no AGENTS | Execute | Implemented (uncommitted) |

**Coverage:** 12 total; Execute applied; Verifier pending (no `validation.md` yet).

---

## Success Criteria

- [ ] Clone fresco contém harness utilizável (HARN-01..02)
- [ ] TLC aponta para `_docs/specs/` com patch só de paths (HARN-03..04)
- [ ] Skills FE com target explícito; brownfield como default (HARN-05..06)
- [ ] `_docs/AGENTS.md` removido; AGENTS só na raiz (HARN-07)
- [ ] Cursor/Claude coerentes (HARN-08)
- [ ] Nenhuma execução de ajuste antes da aprovação desta revisão (HARN-10)

---

## References

- Context: `_docs/specs/features/ajuste-harness/context.md`
- Diagnóstico: `_docs/RELATORIO_CONFORMIDADE_HARNESS_2026-07-26.md`
- Layout: `.agents/references/specs-layout.md`
- Governança: `AGENTS.md`, `.agents/rules/linear_issue_management.md`
